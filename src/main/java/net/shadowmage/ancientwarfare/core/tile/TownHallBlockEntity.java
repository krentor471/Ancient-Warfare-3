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
import net.shadowmage.ancientwarfare.core.entity.DragonRiderController;
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
   private final NonNullList<ItemStack> items = NonNullList.m_122780_(27, ItemStack.f_41583_);
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
      if (npc.m_9236_() instanceof ServerLevel server) {
         if (npc.hasTownHall()) {
            BlockPos cached = npc.getTownHallPos();
            LevelChunk chunk = server.m_7726_().m_7131_(cached.m_123341_() >> 4, cached.m_123343_() >> 4);
            if (chunk != null && chunk.m_7702_(cached) instanceof TownHallBlockEntity hall && !hall.m_58901_() && hall.acceptsNpc(npc)) {
               return hall;
            }
         }

         BlockPos origin = npc.m_20183_();
         TownHallBlockEntity nearest = null;
         double nearestDistance = Double.MAX_VALUE;

         for (BlockEntity blockEntity : WarehouseNetwork.loadedEntities(server, origin.m_7918_(-128, -64, -128), origin.m_7918_(128, 64, 128))) {
            if (blockEntity instanceof TownHallBlockEntity) {
               TownHallBlockEntity hall = (TownHallBlockEntity)blockEntity;
               if (hall.acceptsNpc(npc)) {
                  double distance = npc.m_20238_(hall.f_58858_.m_252807_());
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
      if (this.f_58857_ != null && !this.f_58857_.f_46443_ && this.acceptsNpc(npc)) {
         this.addDeathEntry(createDeathEntry(npc, source, details));
      }
   }

   public static TownHallBlockEntity.NpcDeathEntry createDeathEntry(PlayerOwnedNpcEntity npc, DamageSource source, CompoundTag details) {
      ItemStack spawnerStack = NpcSpawnerItem.createFromNpc(npc);
      return new TownHallBlockEntity.NpcDeathEntry(
         spawnerStack,
         details.m_128461_("NpcType").isBlank() ? npc.getNpcType() : details.m_128461_("NpcType"),
         details.m_128461_("NpcName").isBlank() ? npc.m_5446_().getString() : details.m_128461_("NpcName"),
         source == null ? "unknown" : source.m_19385_(),
         npc.m_20183_(),
         false,
         true,
         false,
         UUID.randomUUID(),
         details.m_6426_()
      );
   }

   private void addDeathEntry(TownHallBlockEntity.NpcDeathEntry entry) {
      if (!this.deathNotices.stream().anyMatch(existing -> existing.recordId().equals(entry.recordId()))) {
         this.deathNotices.add(entry);
         this.deathNotices.sort(Comparator.comparingLong(record -> record.details().m_128454_("DeathTimeMillis")));

         while (this.deathNotices.size() > 64) {
            this.deathNotices.remove(0);
         }

         this.syncChanged();
      }
   }

   public static CompoundTag captureDeathDetails(PlayerOwnedNpcEntity npc, DamageSource source) {
      CompoundTag details = new CompoundTag();
      details.m_128359_("NpcType", npc.getNpcType());
      details.m_128359_("NpcName", npc.m_5446_().getString());
      details.m_128362_("NpcUUID", npc.m_20148_());
      npc.getOwner().ifPresent(id -> {
         details.m_128362_("Owner", id);
         if (npc.m_9236_() instanceof ServerLevel server) {
            server.m_7654_().m_129927_().m_11002_(id).ifPresent(profile -> details.m_128359_("OwnerName", profile.getName()));
         }
      });
      details.m_128359_("NpcSubtype", npc.getNpcSubtype());
      details.m_128359_("Dimension", npc.m_9236_().m_46472_().m_135782_().toString());
      details.m_128356_("DeathTimeMillis", System.currentTimeMillis());
      details.m_128356_("DeathGameTime", npc.m_9236_().m_46467_());
      details.m_128359_("DeathMessage", source == null ? "" : source.m_6157_(npc).getString());
      if (source != null) {
         details.m_128359_("DeathMessageJson", Serializer.m_130703_(source.m_6157_(npc)));
      }

      Entity killer = source == null ? null : source.m_7639_();
      if (killer != null) {
         details.m_128359_("KillerName", killer.m_5446_().getString());
      }

      details.m_128350_("MaxHealth", npc.m_21233_());
      AttributeInstance attack = npc.m_21051_(Attributes.f_22281_);
      details.m_128347_("AttackDamage", attack == null ? 0.0 : attack.m_22135_());
      details.m_128405_("CombatRank", npc.getCombatRank());
      details.m_128359_("Command", npc.getCurrentCommand().name());
      details.m_128356_("CommandTarget", npc.getCommandTarget().m_121878_());
      details.m_128379_("HomeSet", npc.hasHome());
      details.m_128356_("HomePos", npc.getHomePos().m_121878_());
      if (npc.hasTownHall()) {
         details.m_128356_("TownHallPos", npc.getTownHallPos().m_121878_());
      }

      Entity mount = DragonRiderController.getLogicalMount(npc);
      if (mount == null) {
         mount = npc.m_20202_();
      }

      if (mount != null) {
         details.m_128359_("MountType", mount.m_6095_().m_20676_().getString());
      }

      ListTag equipment = new ListTag();

      for (EquipmentSlot slot : EquipmentSlot.values()) {
         ItemStack stack = npc.m_6844_(slot);
         if (!stack.m_41619_()) {
            CompoundTag entry = new CompoundTag();
            entry.m_128359_("Slot", slot.m_20751_());
            entry.m_128365_("Stack", stack.m_41739_(new CompoundTag()));
            equipment.add(entry);
         }
      }

      details.m_128365_("Equipment", equipment);
      if (!npc.getFoodStack().m_41619_()) {
         details.m_128365_("Food", npc.getFoodStack().m_41739_(new CompoundTag()));
      }

      if (!npc.getOrdersStack().m_41619_()) {
         details.m_128365_("Orders", npc.getOrdersStack().m_41739_(new CompoundTag()));
      }

      if (!npc.getUpkeepStack().m_41619_()) {
         details.m_128365_("Upkeep", npc.getUpkeepStack().m_41739_(new CompoundTag()));
      }

      return details;
   }

   public Optional<TownHallBlockEntity.NpcDeathEntry> claimResurrection() {
      for (int i = 0; i < this.deathNotices.size(); i++) {
         TownHallBlockEntity.NpcDeathEntry entry = this.deathNotices.get(i);
         if (entry.canResurrect() && !entry.spawnerStack().m_41619_() && !entry.resurrected() && !entry.beingResurrected()) {
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
         return ItemStack.f_41583_;
      } else {
         for (int slot = 0; slot < this.items.size(); slot++) {
            ItemStack stack = (ItemStack)this.items.get(slot);
            if (this.isFoodLike(stack)) {
               ItemStack removed = stack.m_41620_(Math.min(amount, stack.m_41613_()));
               if (stack.m_41619_()) {
                  this.items.set(slot, ItemStack.f_41583_);
               }

               this.syncChanged();
               return removed;
            }
         }

         return ItemStack.f_41583_;
      }
   }

   private void broadcast(Level level, BlockPos pos) {
      AABB area = new AABB(
         (double)(pos.m_123341_() - 128),
         (double)(pos.m_123342_() - 64),
         (double)(pos.m_123343_() - 128),
         (double)(pos.m_123341_() + 128 + 1),
         (double)(pos.m_123342_() + 64 + 1),
         (double)(pos.m_123343_() + 128 + 1)
      );

      for (PlayerOwnedNpcEntity npc : level.m_45976_(PlayerOwnedNpcEntity.class, area)) {
         if (npc.m_6084_() && this.acceptsNpc(npc)) {
            npc.handleTownHallBroadcast(pos);
         }
      }
   }

   private boolean isFoodLike(ItemStack stack) {
      return !stack.m_41619_() && (stack.m_41614_() || AWCoreRegistryAccess.is(stack, AWCoreIds.FOOD_BUNDLE));
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      if (this.owner != null) {
         tag.m_128362_("Owner", this.owner);
      }

      tag.m_128405_("BroadcastDelay", this.broadcastDelay);
      ContainerHelper.m_18973_(tag, this.items);
      ListTag deathList = new ListTag();

      for (TownHallBlockEntity.NpcDeathEntry entry : this.deathNotices) {
         deathList.add(entry.save());
      }

      tag.m_128365_("DeathNotices", deathList);
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.owner = tag.m_128403_("Owner") ? tag.m_128342_("Owner") : null;
      this.broadcastDelay = tag.m_128451_("BroadcastDelay");
      this.pendingDeathsImported = false;
      ContainerHelper.m_18980_(tag, this.items);
      this.deathNotices.clear();
      ListTag deathList = tag.m_128437_("DeathNotices", 10);

      for (int i = 0; i < deathList.size(); i++) {
         this.deathNotices.add(TownHallBlockEntity.NpcDeathEntry.load(deathList.m_128728_(i)).withBeingResurrected(false));
      }
   }

   public CompoundTag m_5995_() {
      CompoundTag tag = new CompoundTag();
      if (this.owner != null) {
         tag.m_128362_("Owner", this.owner);
      }

      ListTag deathList = new ListTag();

      for (TownHallBlockEntity.NpcDeathEntry entry : this.deathNotices) {
         deathList.add(entry.saveForClient());
      }

      tag.m_128365_("DeathNotices", deathList);
      return tag;
   }

   public void handleUpdateTag(CompoundTag tag) {
      this.owner = tag.m_128403_("Owner") ? tag.m_128342_("Owner") : null;
      this.deathNotices.clear();
      ListTag deathList = tag.m_128437_("DeathNotices", 10);

      for (int i = 0; i < deathList.size(); i++) {
         this.deathNotices.add(TownHallBlockEntity.NpcDeathEntry.load(deathList.m_128728_(i)));
      }
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.m_195640_(this);
   }

   public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
      CompoundTag tag = packet.m_131708_();
      if (tag != null) {
         this.handleUpdateTag(tag);
      }
   }

   public int m_6643_() {
      return this.items.size();
   }

   public boolean m_7983_() {
      return this.items.stream().allMatch(ItemStack::m_41619_);
   }

   public ItemStack m_8020_(int slot) {
      return slot >= 0 && slot < this.items.size() ? (ItemStack)this.items.get(slot) : ItemStack.f_41583_;
   }

   public ItemStack m_7407_(int slot, int amount) {
      ItemStack removed = slot >= 0 && slot < this.items.size() ? ContainerHelper.m_18969_(this.items, slot, amount) : ItemStack.f_41583_;
      if (!removed.m_41619_()) {
         this.syncChanged();
      }

      return removed;
   }

   public ItemStack m_8016_(int slot) {
      return slot >= 0 && slot < this.items.size() ? ContainerHelper.m_18966_(this.items, slot) : ItemStack.f_41583_;
   }

   public void m_6836_(int slot, ItemStack stack) {
      if (slot >= 0 && slot < this.items.size()) {
         this.items.set(slot, stack);
         if (!stack.m_41619_() && stack.m_41613_() > this.m_6893_()) {
            stack.m_41764_(this.m_6893_());
         }

         this.syncChanged();
      }
   }

   public boolean m_6542_(Player player) {
      return this.f_58857_ != null
         && this.f_58857_.m_7702_(this.f_58858_) == this
         && player.m_20275_((double)this.f_58858_.m_123341_() + 0.5, (double)this.f_58858_.m_123342_() + 0.5, (double)this.f_58858_.m_123343_() + 0.5) <= 64.0;
   }

   public void m_6211_() {
      this.items.clear();
      this.syncChanged();
   }

   public Component m_5446_() {
      return Component.m_237115_("container.ancientwarfare.town_hall");
   }

   @Nullable
   public AbstractContainerMenu m_7208_(int containerId, Inventory playerInventory, Player player) {
      return new TownHallMenu(containerId, playerInventory, this.f_58858_);
   }

   private void syncChanged() {
      this.m_6596_();
      if (this.f_58857_ != null) {
         this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 3);
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
         tag.m_128365_("SpawnerStack", this.spawnerStack.m_41739_(new CompoundTag()));
         return tag;
      }

      public CompoundTag saveForClient() {
         CompoundTag tag = new CompoundTag();
         tag.m_128359_("NpcType", this.npcType);
         tag.m_128359_("NpcName", this.npcName);
         tag.m_128359_("DeathCause", this.deathCause);
         tag.m_128356_("Pos", this.pos.m_121878_());
         tag.m_128379_("Resurrected", this.resurrected);
         tag.m_128379_("CanResurrect", this.canResurrect);
         tag.m_128379_("BeingResurrected", this.beingResurrected);
         tag.m_128362_("RecordId", this.recordId);
         tag.m_128365_("Details", this.details.m_6426_());
         return tag;
      }

      public static TownHallBlockEntity.NpcDeathEntry load(CompoundTag tag) {
         return new TownHallBlockEntity.NpcDeathEntry(
            ItemStack.m_41712_(tag.m_128469_("SpawnerStack")),
            tag.m_128461_("NpcType"),
            tag.m_128461_("NpcName"),
            tag.m_128461_("DeathCause"),
            tag.m_128441_("Pos") ? BlockPos.m_122022_(tag.m_128454_("Pos")) : BlockPos.f_121853_,
            tag.m_128471_("Resurrected"),
            tag.m_128471_("CanResurrect"),
            tag.m_128471_("BeingResurrected"),
            tag.m_128403_("RecordId") ? tag.m_128342_("RecordId") : UUID.randomUUID(),
            tag.m_128425_("Details", 10) ? tag.m_128469_("Details").m_6426_() : legacyDetails(tag)
         );
      }

      private static CompoundTag legacyDetails(CompoundTag tag) {
         CompoundTag details = new CompoundTag();
         CompoundTag npcData = tag.m_128469_("SpawnerStack").m_128469_("tag").m_128469_("npcData");

         for (String key : List.of("Owner", "NpcSubtype", "MaxHealth", "HomeSet", "HomePos", "TownHallPos")) {
            if (npcData.m_128441_(key)) {
               details.m_128365_(key, npcData.m_128423_(key).m_6426_());
            }
         }

         if (npcData.m_128403_("UUID")) {
            details.m_128362_("NpcUUID", npcData.m_128342_("UUID"));
         }

         details.m_128356_("DeathTimeMillis", 0L);
         details.m_128356_("DeathGameTime", -1L);
         ListTag equipment = new ListTag();

         for (Type type : Type.values()) {
            ListTag items = npcData.m_128437_(type == Type.HAND ? "HandItems" : "ArmorItems", 10);

            for (EquipmentSlot slot : EquipmentSlot.values()) {
               if (slot.m_20743_() == type && slot.m_20749_() < items.size()) {
                  CompoundTag stack = items.m_128728_(slot.m_20749_());
                  if (stack.m_128445_("Count") > 0) {
                     CompoundTag entry = new CompoundTag();
                     entry.m_128359_("Slot", slot.m_20751_());
                     entry.m_128365_("Stack", stack.m_6426_());
                     equipment.add(entry);
                  }
               }
            }
         }

         details.m_128365_("Equipment", equipment);
         return details;
      }

      public TownHallBlockEntity.NpcDeathEntry withResurrected(boolean value) {
         return new TownHallBlockEntity.NpcDeathEntry(
            this.spawnerStack.m_41777_(),
            this.npcType,
            this.npcName,
            this.deathCause,
            this.pos,
            value,
            this.canResurrect,
            this.beingResurrected,
            this.recordId,
            this.details.m_6426_()
         );
      }

      public TownHallBlockEntity.NpcDeathEntry withBeingResurrected(boolean value) {
         return new TownHallBlockEntity.NpcDeathEntry(
            this.spawnerStack.m_41777_(),
            this.npcType,
            this.npcName,
            this.deathCause,
            this.pos,
            this.resurrected,
            this.canResurrect,
            value,
            this.recordId,
            this.details.m_6426_()
         );
      }

      private boolean sameRecord(TownHallBlockEntity.NpcDeathEntry other) {
         return other != null && this.recordId.equals(other.recordId);
      }
   }
}
