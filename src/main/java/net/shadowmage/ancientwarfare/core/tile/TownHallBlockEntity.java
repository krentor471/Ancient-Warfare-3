package net.shadowmage.ancientwarfare.core.tile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.entity.PlayerOwnedNpcEntity;
import net.shadowmage.ancientwarfare.core.item.NpcSpawnerItem;
import net.shadowmage.ancientwarfare.core.menu.TownHallMenu;
import org.jetbrains.annotations.Nullable;

public class TownHallBlockEntity extends BlockEntity implements Container, MenuProvider {
   public static final int INVENTORY_SIZE = 27;
   private static final int BROADCAST_RANGE = 128;
   private static final int BROADCAST_HALF_HEIGHT = 64;
   private static final int BROADCAST_INTERVAL_TICKS = 200;
   static final int MAX_DEATH_NOTICES = 64;
   private static final String OWNER_TAG = "Owner";
   private static final String DEATH_NOTICES_TAG = "DeathNotices";
   private static final String BROADCAST_DELAY_TAG = "BroadcastDelay";
   private final NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
   private final List<TownHallBlockEntity.NpcDeathEntry> deathNotices = new ArrayList<>();
   private UUID owner;
   private int broadcastDelay;
   private boolean pendingDeathsImported;

   public TownHallBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.TOWN_HALL), pos, state);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, TownHallBlockEntity townHall) {
      if (!townHall.pendingDeathsImported && level instanceof ServerLevel server) {
         townHall.pendingDeathsImported = true;

         for (TownHallBlockEntity.NpcDeathEntry entry : TownHallDeathData.get(server).take(pos, townHall.owner)) {
            townHall.addDeathEntry(entry);
         }
      }

      if (--townHall.broadcastDelay <= 0) {
         townHall.broadcastDelay = 200;
         townHall.broadcast(level, pos);
      }
   }

   public void setOwner(UUID owner) {
      this.owner = owner;
      this.pendingDeathsImported = false;
      this.syncChanged();
   }

   public Optional<UUID> getOwner() {
      return Optional.ofNullable(this.owner);
   }

   public List<TownHallBlockEntity.NpcDeathEntry> getDeathNotices() {
      return List.copyOf(this.deathNotices);
   }

   public boolean acceptsNpc(PlayerOwnedNpcEntity npc) {
      return this.owner == null || npc.getOwner().map(this.owner::equals).orElse(false);
   }

   @Nullable
   public static TownHallBlockEntity findForDeath(PlayerOwnedNpcEntity npc) {
      if (npc.level() instanceof ServerLevel server) {
         if (npc.hasTownHall()) {
            BlockPos cached = npc.getTownHallPos();
            LevelChunk chunk = server.getChunkSource().getChunkNow(cached.getX() >> 4, cached.getZ() >> 4);
            if (chunk != null && chunk.getBlockEntity(cached) instanceof TownHallBlockEntity hall && !hall.isRemoved() && hall.acceptsNpc(npc)) {
               return hall;
            }
         }

         BlockPos origin = npc.blockPosition();
         TownHallBlockEntity nearest = null;
         double nearestDistance = Double.MAX_VALUE;

         for (BlockEntity blockEntity : WarehouseNetwork.loadedEntities(server, origin.offset(-128, -64, -128), origin.offset(128, 64, 128))) {
            if (blockEntity instanceof TownHallBlockEntity) {
               TownHallBlockEntity hall = (TownHallBlockEntity)blockEntity;
               if (hall.acceptsNpc(npc)) {
                  double distance = npc.distanceToSqr(hall.worldPosition.getCenter());
                  if (distance < nearestDistance) {
                     nearest = hall;
                     nearestDistance = distance;
                  }
               }
            }
         }

         return nearest;
      } else {
         return null;
      }
   }

   public void clearDeathNotices() {
      this.deathNotices.clear();
      this.syncChanged();
   }

   public void recordNpcDeath(PlayerOwnedNpcEntity npc, DamageSource source) {
      this.recordNpcDeath(npc, source, captureDeathDetails(npc, source));
   }

   public void recordNpcDeath(PlayerOwnedNpcEntity npc, DamageSource source, CompoundTag details) {
      if (this.level != null && !this.level.isClientSide && this.acceptsNpc(npc)) {
         this.addDeathEntry(createDeathEntry(npc, source, details));
      }
   }

   public static TownHallBlockEntity.NpcDeathEntry createDeathEntry(PlayerOwnedNpcEntity npc, DamageSource source, CompoundTag details) {
      ItemStack spawnerStack = NpcSpawnerItem.createFromNpc(npc);
      return new TownHallBlockEntity.NpcDeathEntry(
         spawnerStack,
         details.getString("NpcType").isBlank() ? npc.getNpcType() : details.getString("NpcType"),
         details.getString("NpcName").isBlank() ? npc.getDisplayName().getString() : details.getString("NpcName"),
         source == null ? "unknown" : source.getMsgId(),
         npc.blockPosition(),
         false,
         true,
         false,
         UUID.randomUUID(),
         details.copy()
      );
   }

   private void addDeathEntry(TownHallBlockEntity.NpcDeathEntry entry) {
      if (!this.deathNotices.stream().anyMatch(existing -> existing.recordId().equals(entry.recordId()))) {
         this.deathNotices.add(entry);
         this.deathNotices.sort(Comparator.comparingLong(record -> record.details().getLong("DeathTimeMillis")));

         while (this.deathNotices.size() > 64) {
            this.deathNotices.remove(0);
         }

         this.syncChanged();
      }
   }

   public static CompoundTag captureDeathDetails(PlayerOwnedNpcEntity npc, DamageSource source) {
      CompoundTag details = new CompoundTag();
      details.putString("NpcType", npc.getNpcType());
      details.putString("NpcName", npc.getDisplayName().getString());
      details.putUUID("NpcUUID", npc.getUUID());
      npc.getOwner().ifPresent(id -> {
         details.putUUID("Owner", id);
         if (npc.level() instanceof ServerLevel server) {
            server.getServer().getProfileCache().get(id).ifPresent(profile -> details.putString("OwnerName", profile.getName()));
         }
      });
      details.putString("NpcSubtype", npc.getNpcSubtype());
      details.putString("Dimension", npc.level().dimension().location().toString());
      details.putLong("DeathTimeMillis", System.currentTimeMillis());
      details.putLong("DeathGameTime", npc.level().getGameTime());
      details.putString("DeathMessage", source == null ? "" : source.getLocalizedDeathMessage(npc).getString());
      if (source != null) {
         details.putString("DeathMessageJson", Serializer.toJson(source.getLocalizedDeathMessage(npc)));
      }

      Entity killer = source == null ? null : source.getEntity();
      if (killer != null) {
         details.putString("KillerName", killer.getDisplayName().getString());
      }

      details.putFloat("MaxHealth", npc.getMaxHealth());
      AttributeInstance attack = npc.getAttribute(Attributes.ATTACK_DAMAGE);
      details.putDouble("AttackDamage", attack == null ? 0.0 : attack.getValue());
      details.putInt("CombatRank", npc.getCombatRank());
      details.putString("Command", npc.getCurrentCommand().name());
      details.putLong("CommandTarget", npc.getCommandTarget().asLong());
      details.putBoolean("HomeSet", npc.hasHome());
      details.putLong("HomePos", npc.getHomePos().asLong());
      if (npc.hasTownHall()) {
         details.putLong("TownHallPos", npc.getTownHallPos().asLong());
      }

      Entity mount = npc.getVehicle();

      if (mount != null) {
         details.putString("MountType", mount.getType().getDescription().getString());
      }

      ListTag equipment = new ListTag();

      for (EquipmentSlot slot : EquipmentSlot.values()) {
         ItemStack stack = npc.getItemBySlot(slot);
         if (!stack.isEmpty()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Slot", slot.getName());
            entry.put("Stack", stack.save(new CompoundTag()));
            equipment.add(entry);
         }
      }

      details.put("Equipment", equipment);
      if (!npc.getFoodStack().isEmpty()) {
         details.put("Food", npc.getFoodStack().save(new CompoundTag()));
      }

      if (!npc.getOrdersStack().isEmpty()) {
         details.put("Orders", npc.getOrdersStack().save(new CompoundTag()));
      }

      if (!npc.getUpkeepStack().isEmpty()) {
         details.put("Upkeep", npc.getUpkeepStack().save(new CompoundTag()));
      }

      return details;
   }

   public Optional<TownHallBlockEntity.NpcDeathEntry> claimResurrection() {
      for (int i = 0; i < this.deathNotices.size(); i++) {
         TownHallBlockEntity.NpcDeathEntry entry = this.deathNotices.get(i);
         if (entry.canResurrect() && !entry.spawnerStack().isEmpty() && !entry.resurrected() && !entry.beingResurrected()) {
            TownHallBlockEntity.NpcDeathEntry claimed = entry.withBeingResurrected(true);
            this.deathNotices.set(i, claimed);
            this.syncChanged();
            return Optional.of(claimed);
         }
      }

      return Optional.empty();
   }

   public void releaseResurrection(TownHallBlockEntity.NpcDeathEntry claimedEntry) {
      this.replaceDeathEntry(claimedEntry, claimedEntry.withBeingResurrected(false));
   }

   public void completeResurrection(TownHallBlockEntity.NpcDeathEntry claimedEntry, boolean resurrected) {
      this.replaceDeathEntry(claimedEntry, claimedEntry.withResurrected(resurrected).withBeingResurrected(false));
   }

   private void replaceDeathEntry(TownHallBlockEntity.NpcDeathEntry oldEntry, TownHallBlockEntity.NpcDeathEntry newEntry) {
      for (int i = 0; i < this.deathNotices.size(); i++) {
         TownHallBlockEntity.NpcDeathEntry current = this.deathNotices.get(i);
         if (current.sameRecord(oldEntry) && current.beingResurrected() && !current.resurrected()) {
            this.deathNotices.set(i, newEntry);
            this.syncChanged();
            return;
         }
      }
   }

   public ItemStack extractFoodLike(int amount) {
      if (amount <= 0) {
         return ItemStack.EMPTY;
      } else {
         for (int slot = 0; slot < this.items.size(); slot++) {
            ItemStack stack = (ItemStack)this.items.get(slot);
            if (this.isFoodLike(stack)) {
               ItemStack removed = stack.split(Math.min(amount, stack.getCount()));
               if (stack.isEmpty()) {
                  this.items.set(slot, ItemStack.EMPTY);
               }

               this.syncChanged();
               return removed;
            }
         }

         return ItemStack.EMPTY;
      }
   }

   private void broadcast(Level level, BlockPos pos) {
      AABB area = new AABB(
         (double)(pos.getX() - 128),
         (double)(pos.getY() - 64),
         (double)(pos.getZ() - 128),
         (double)(pos.getX() + 128 + 1),
         (double)(pos.getY() + 64 + 1),
         (double)(pos.getZ() + 128 + 1)
      );

      for (PlayerOwnedNpcEntity npc : level.getEntitiesOfClass(PlayerOwnedNpcEntity.class, area)) {
         if (npc.isAlive() && this.acceptsNpc(npc)) {
            npc.handleTownHallBroadcast(pos);
         }
      }
   }

   private boolean isFoodLike(ItemStack stack) {
      return !stack.isEmpty() && (stack.isEdible() || AWCoreRegistryAccess.is(stack, AWCoreIds.FOOD_BUNDLE));
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      if (this.owner != null) {
         tag.putUUID("Owner", this.owner);
      }

      tag.putInt("BroadcastDelay", this.broadcastDelay);
      ContainerHelper.saveAllItems(tag, this.items);
      ListTag deathList = new ListTag();

      for (TownHallBlockEntity.NpcDeathEntry entry : this.deathNotices) {
         deathList.add(entry.save());
      }

      tag.put("DeathNotices", deathList);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
      this.broadcastDelay = tag.getInt("BroadcastDelay");
      this.pendingDeathsImported = false;
      ContainerHelper.loadAllItems(tag, this.items);
      this.deathNotices.clear();
      ListTag deathList = tag.getList("DeathNotices", 10);

      for (int i = 0; i < deathList.size(); i++) {
         this.deathNotices.add(TownHallBlockEntity.NpcDeathEntry.load(deathList.getCompound(i)).withBeingResurrected(false));
      }
   }

   public CompoundTag getUpdateTag() {
      CompoundTag tag = new CompoundTag();
      if (this.owner != null) {
         tag.putUUID("Owner", this.owner);
      }

      ListTag deathList = new ListTag();

      for (TownHallBlockEntity.NpcDeathEntry entry : this.deathNotices) {
         deathList.add(entry.saveForClient());
      }

      tag.put("DeathNotices", deathList);
      return tag;
   }

   public void handleUpdateTag(CompoundTag tag) {
      this.owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
      this.deathNotices.clear();
      ListTag deathList = tag.getList("DeathNotices", 10);

      for (int i = 0; i < deathList.size(); i++) {
         this.deathNotices.add(TownHallBlockEntity.NpcDeathEntry.load(deathList.getCompound(i)));
      }
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
      CompoundTag tag = packet.getTag();
      if (tag != null) {
         this.handleUpdateTag(tag);
      }
   }

   public int getContainerSize() {
      return this.items.size();
   }

   public boolean isEmpty() {
      return this.items.stream().allMatch(ItemStack::isEmpty);
   }

   public ItemStack getItem(int slot) {
      return slot >= 0 && slot < this.items.size() ? (ItemStack)this.items.get(slot) : ItemStack.EMPTY;
   }

   public ItemStack removeItem(int slot, int amount) {
      ItemStack removed = slot >= 0 && slot < this.items.size() ? ContainerHelper.removeItem(this.items, slot, amount) : ItemStack.EMPTY;
      if (!removed.isEmpty()) {
         this.syncChanged();
      }

      return removed;
   }

   public ItemStack removeItemNoUpdate(int slot) {
      return slot >= 0 && slot < this.items.size() ? ContainerHelper.takeItem(this.items, slot) : ItemStack.EMPTY;
   }

   public void setItem(int slot, ItemStack stack) {
      if (slot >= 0 && slot < this.items.size()) {
         this.items.set(slot, stack);
         if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
         }

         this.syncChanged();
      }
   }

   public boolean stillValid(Player player) {
      return this.level != null
         && this.level.getBlockEntity(this.worldPosition) == this
         && player.distanceToSqr((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5) <= 64.0;
   }

   public void clearContent() {
      this.items.clear();
      this.syncChanged();
   }

   public Component getDisplayName() {
      return Component.translatable("container.ancientwarfare.town_hall");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
      return new TownHallMenu(containerId, playerInventory, this.worldPosition);
   }

   private void syncChanged() {
      this.setChanged();
      if (this.level != null) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public static record NpcDeathEntry(
      ItemStack spawnerStack,
      String npcType,
      String npcName,
      String deathCause,
      BlockPos pos,
      boolean resurrected,
      boolean canResurrect,
      boolean beingResurrected,
      UUID recordId,
      CompoundTag details
   ) {
      private static final String SPAWNER_STACK_TAG = "SpawnerStack";
      private static final String NPC_TYPE_TAG = "NpcType";
      private static final String NPC_NAME_TAG = "NpcName";
      private static final String DEATH_CAUSE_TAG = "DeathCause";
      private static final String POS_TAG = "Pos";
      private static final String RESURRECTED_TAG = "Resurrected";
      private static final String CAN_RESURRECT_TAG = "CanResurrect";
      private static final String BEING_RESURRECTED_TAG = "BeingResurrected";

      public NpcDeathEntry(
         ItemStack spawnerStack,
         String npcType,
         String npcName,
         String deathCause,
         BlockPos pos,
         boolean resurrected,
         boolean canResurrect,
         boolean beingResurrected
      ) {
         this(spawnerStack, npcType, npcName, deathCause, pos, resurrected, canResurrect, beingResurrected, UUID.randomUUID(), new CompoundTag());
      }

      public CompoundTag save() {
         CompoundTag tag = this.saveForClient();
         tag.put("SpawnerStack", this.spawnerStack.save(new CompoundTag()));
         return tag;
      }

      public CompoundTag saveForClient() {
         CompoundTag tag = new CompoundTag();
         tag.putString("NpcType", this.npcType);
         tag.putString("NpcName", this.npcName);
         tag.putString("DeathCause", this.deathCause);
         tag.putLong("Pos", this.pos.asLong());
         tag.putBoolean("Resurrected", this.resurrected);
         tag.putBoolean("CanResurrect", this.canResurrect);
         tag.putBoolean("BeingResurrected", this.beingResurrected);
         tag.putUUID("RecordId", this.recordId);
         tag.put("Details", this.details.copy());
         return tag;
      }

      public static TownHallBlockEntity.NpcDeathEntry load(CompoundTag tag) {
         return new TownHallBlockEntity.NpcDeathEntry(
            ItemStack.of(tag.getCompound("SpawnerStack")),
            tag.getString("NpcType"),
            tag.getString("NpcName"),
            tag.getString("DeathCause"),
            tag.contains("Pos") ? BlockPos.of(tag.getLong("Pos")) : BlockPos.ZERO,
            tag.getBoolean("Resurrected"),
            tag.getBoolean("CanResurrect"),
            tag.getBoolean("BeingResurrected"),
            tag.hasUUID("RecordId") ? tag.getUUID("RecordId") : UUID.randomUUID(),
            tag.contains("Details", 10) ? tag.getCompound("Details").copy() : legacyDetails(tag)
         );
      }

      private static CompoundTag legacyDetails(CompoundTag tag) {
         CompoundTag details = new CompoundTag();
         CompoundTag npcData = tag.getCompound("SpawnerStack").getCompound("tag").getCompound("npcData");

         for (String key : List.of("Owner", "NpcSubtype", "MaxHealth", "HomeSet", "HomePos", "TownHallPos")) {
            if (npcData.contains(key)) {
               details.put(key, npcData.get(key).copy());
            }
         }

         if (npcData.hasUUID("UUID")) {
            details.putUUID("NpcUUID", npcData.getUUID("UUID"));
         }

         details.putLong("DeathTimeMillis", 0L);
         details.putLong("DeathGameTime", -1L);
         ListTag equipment = new ListTag();

         for (Type type : Type.values()) {
            ListTag items = npcData.getList(type == Type.HAND ? "HandItems" : "ArmorItems", 10);

            for (EquipmentSlot slot : EquipmentSlot.values()) {
               if (slot.getType() == type && slot.getIndex() < items.size()) {
                  CompoundTag stack = items.getCompound(slot.getIndex());
                  if (stack.getByte("Count") > 0) {
                     CompoundTag entry = new CompoundTag();
                     entry.putString("Slot", slot.getName());
                     entry.put("Stack", stack.copy());
                     equipment.add(entry);
                  }
               }
            }
         }

         details.put("Equipment", equipment);
         return details;
      }

      public TownHallBlockEntity.NpcDeathEntry withResurrected(boolean value) {
         return new TownHallBlockEntity.NpcDeathEntry(
            this.spawnerStack.copy(),
            this.npcType,
            this.npcName,
            this.deathCause,
            this.pos,
            value,
            this.canResurrect,
            this.beingResurrected,
            this.recordId,
            this.details.copy()
         );
      }

      public TownHallBlockEntity.NpcDeathEntry withBeingResurrected(boolean value) {
         return new TownHallBlockEntity.NpcDeathEntry(
            this.spawnerStack.copy(),
            this.npcType,
            this.npcName,
            this.deathCause,
            this.pos,
            this.resurrected,
            this.canResurrect,
            value,
            this.recordId,
            this.details.copy()
         );
      }

      private boolean sameRecord(TownHallBlockEntity.NpcDeathEntry other) {
         return other != null && this.recordId.equals(other.recordId);
      }
   }
}
