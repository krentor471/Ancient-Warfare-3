package net.shadowmage.ancientwarfare.core.tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.menu.StructureContainerMenu;
import net.shadowmage.ancientwarfare.core.structure.LootRewardHelper;
import net.shadowmage.ancientwarfare.core.structure.StructureLootTierResolver;
import org.jetbrains.annotations.Nullable;

public class StructureContainerBlockEntity extends BlockEntity implements Container, MenuProvider {
   public static final int LOOT_BASKET_SIZE = 27;
   public static final int URN_SIZE = 27;
   public static final int COFFIN_SIZE = 27;
   public static final int LOOT_CHEST_SIZE = 27;
   private static final String SIZE_TAG = "Size";
   private static final String LOOT_TABLE_TAG = "LootTable";
   private static final String LOOT_SEED_TAG = "LootTableSeed";
   private static final String LOOT_ROLLS_TAG = "LootRolls";
   private static final String LEGACY_SETTINGS_TAG = "LegacyLootSettings";
   private int size;
   private NonNullList<ItemStack> items;
   @Nullable
   private ResourceLocation lootTable;
   private CompoundTag legacyLootSettings = new CompoundTag();
   private long lootSeed;
   private int lootRolls = 1;
   private int openCount;
   private float lidProgress;
   private float previousLidProgress;

   public StructureContainerBlockEntity(BlockPos pos, BlockState state) {
      this(pos, state, containerSize(state));
   }

   public StructureContainerBlockEntity(BlockPos pos, BlockState state, int size) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.STRUCTURE_CONTAINER), pos, state);
      this.size = size;
      this.items = NonNullList.withSize(size, ItemStack.EMPTY);
   }

   public static int containerSize(BlockState state) {
      if (state.is(AWCoreRegistryAccess.block(AWCoreIds.URN))) {
         return 27;
      } else {
         return !state.is(AWCoreRegistryAccess.block(AWCoreIds.WOODEN_COFFIN)) && !state.is(AWCoreRegistryAccess.block(AWCoreIds.STONE_COFFIN))
            ? 27
            : 27;
      }
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      this.ensureCorrectSize();
      tag.putInt("Size", this.size);
      if (this.lootTable != null) {
         tag.putString("LootTable", this.lootTable.toString());
         tag.putLong("LootTableSeed", this.lootSeed);
         tag.putInt("LootRolls", this.lootRolls);
      }

      if (!this.legacyLootSettings.isEmpty()) {
         tag.put("LegacyLootSettings", this.legacyLootSettings.copy());
      }

      ContainerHelper.saveAllItems(tag, this.items);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.ensureCorrectSize();
      this.lootTable = ResourceLocation.tryParse(tag.getString("LootTable"));
      this.legacyLootSettings = tag.contains("LegacyLootSettings") ? tag.getCompound("LegacyLootSettings").copy() : new CompoundTag();
      this.lootSeed = tag.getLong("LootTableSeed");
      this.lootRolls = Math.max(1, tag.getInt("LootRolls"));
      ContainerHelper.loadAllItems(tag, this.items);
      if (this.level instanceof ServerLevel) {
         AncientWarfareCore.LOG
            .debug(
               "Structure container loaded: pos={}, block={}, type={}, pendingTable={}, rolls={}, items={}",
               new Object[]{
                  this.worldPosition, this.getBlockState().getBlock(), this.getClass().getSimpleName(), this.lootTable, this.lootRolls, this.countNonEmptyItems()
               }
            );
         AncientWarfareCore.LOG
            .debug(
               "Structure container legacy settings loaded: pos={}, present={}, keys={}",
               new Object[]{this.worldPosition, !this.legacyLootSettings.isEmpty(), this.legacyLootSettings.getAllKeys().size()}
            );
      }
   }

   public int getContainerSize() {
      this.ensureCorrectSize();
      return this.items.size();
   }

   public boolean isEmpty() {
      this.ensureCorrectSize();
      return this.items.stream().allMatch(ItemStack::isEmpty);
   }

   public ItemStack getItem(int slot) {
      this.ensureCorrectSize();
      return slot >= 0 && slot < this.items.size() ? (ItemStack)this.items.get(slot) : ItemStack.EMPTY;
   }

   public ItemStack removeItem(int slot, int amount) {
      this.ensureCorrectSize();
      ItemStack removed = slot >= 0 && slot < this.items.size() ? ContainerHelper.removeItem(this.items, slot, amount) : ItemStack.EMPTY;
      if (!removed.isEmpty()) {
         this.syncChanged();
      }

      return removed;
   }

   public ItemStack removeItemNoUpdate(int slot) {
      this.ensureCorrectSize();
      ItemStack removed = slot >= 0 && slot < this.items.size() ? ContainerHelper.takeItem(this.items, slot) : ItemStack.EMPTY;
      if (!removed.isEmpty()) {
         this.syncChanged();
      }

      return removed;
   }

   public void setItem(int slot, ItemStack stack) {
      this.ensureCorrectSize();
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

   public void startOpen(Player player) {
      if (!player.isSpectator() && this.isCoffin()) {
         this.openCount++;
         if (this.level != null) {
            this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, this.openCount);
         }
      }
   }

   public void stopOpen(Player player) {
      if (!player.isSpectator() && this.isCoffin()) {
         this.openCount = Math.max(0, this.openCount - 1);
         if (this.level != null) {
            this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, this.openCount);
         }
      }
   }

   public boolean triggerEvent(int id, int value) {
      if (id == 1) {
         this.openCount = Math.max(0, value);
         return true;
      } else {
         return super.triggerEvent(id, value);
      }
   }

   public void clientTickLid() {
      this.previousLidProgress = this.lidProgress;
      float target = this.openCount > 0 ? 1.0F : 0.0F;
      if (this.lidProgress < target) {
         this.lidProgress = Math.min(target, this.lidProgress + 0.1F);
      } else if (this.lidProgress > target) {
         this.lidProgress = Math.max(target, this.lidProgress - 0.1F);
      }
   }

   public float getLidProgress(float partialTick) {
      return this.previousLidProgress + (this.lidProgress - this.previousLidProgress) * partialTick;
   }

   private boolean isCoffin() {
      return this.getBlockState().is(AWCoreRegistryAccess.block(AWCoreIds.WOODEN_COFFIN))
         || this.getBlockState().is(AWCoreRegistryAccess.block(AWCoreIds.STONE_COFFIN));
   }

   public void clearContent() {
      this.ensureCorrectSize();
      this.items.clear();
      this.syncChanged();
   }

   public Component getDisplayName() {
      return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
   }

   @Nullable
   public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
      this.unpackLoot(player);
      this.ensureCorrectSize();
      return new StructureContainerMenu(containerId, playerInventory, this.worldPosition);
   }

   public void configureStructureLoot(ResourceLocation table, int rolls, long seed) {
      this.lootTable = table;
      this.lootRolls = Math.max(1, rolls);
      this.lootSeed = seed;
      this.setChanged();
      AncientWarfareCore.LOG
         .debug(
            "Structure container loot configured: pos={}, block={}, table={}, rolls={}",
            new Object[]{this.worldPosition, this.getBlockState().getBlock(), table, this.lootRolls}
         );
   }

   public void storeLegacyLootSettings(CompoundTag settings) {
      if (settings != null && !settings.isEmpty()) {
         this.legacyLootSettings = settings.copy();
         this.setChanged();
      }
   }

   public boolean hasLegacyLootSettings() {
      return !this.legacyLootSettings.isEmpty();
   }

   public void unpackLootForDestruction() {
      this.unpackLoot(null);
   }

   private void unpackLoot(@Nullable Player player) {
      if (this.level instanceof ServerLevel serverLevel && this.lootTable != null) {
         AncientWarfareCore.LOG
            .debug(
               "Structure container loot generation start: pos={}, block={}, table={}, rolls={}, itemsBefore={}, player={}",
               new Object[]{
                  this.worldPosition,
                  this.getBlockState().getBlock(),
                  this.lootTable,
                  this.lootRolls,
                  this.countNonEmptyItems(),
                  player == null ? "null" : player.getGameProfile().getName()
               }
            );
         List<ItemStack> generated = new ArrayList<>();
         if (this.lootTable != null) {
            ResourceLocation tableId = StructureLootTierResolver.resolveStoredTable(this.lootTable);
            this.lootTable = null;
            LootTable table = serverLevel.getServer().getLootData().getLootTable(tableId);
            Builder paramsBuilder = new Builder(serverLevel).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.worldPosition));
            if (player != null) {
               paramsBuilder.withLuck(player.getLuck());
            }

            LootParams params = paramsBuilder.create(LootContextParamSets.CHEST);
            Set<String> seenResearch = new HashSet<>();
            int effectiveRolls = AWCorePlatform.gameplayConfig().effectiveLootRolls(tableId, this.lootRolls, this.lootSeed);

            for (int roll = 0; roll < effectiveRolls; roll++) {
               long rollSeed = this.lootSeed == 0L ? serverLevel.random.nextLong() : this.lootSeed + (long)roll;
               List<ItemStack> loot = LootRewardHelper.getLootWithoutDuplicateResearch(table, params, rollSeed, seenResearch);
               generated.addAll(
                  LootRewardHelper.scaleItemCounts(loot, AWCorePlatform.gameplayConfig().lootItemCountMultiplier(), rollSeed ^ 4850189118611868748L)
               );
            }
         }

         List<Integer> slots = new ArrayList<>();

         for (int slot = 0; slot < this.items.size(); slot++) {
            if (((ItemStack)this.items.get(slot)).isEmpty()) {
               slots.add(slot);
            }
         }

         Collections.shuffle(slots, new Random(this.lootSeed == 0L ? serverLevel.random.nextLong() : this.lootSeed));

         for (ItemStack stack : generated) {
            int remaining = stack.getCount();
            int maxStackSize = stack.getMaxStackSize();

            while (remaining > 0 && !slots.isEmpty()) {
               ItemStack split = stack.copy();
               split.setCount(Math.min(remaining, maxStackSize));
               this.items.set(slots.remove(slots.size() - 1), split);
               remaining -= split.getCount();
            }
         }

         this.syncChanged();
         AncientWarfareCore.LOG
            .debug(
               "Structure container loot generation end: pos={}, block={}, itemsAfter={}, pendingTable={}",
               new Object[]{this.worldPosition, this.getBlockState().getBlock(), this.countNonEmptyItems(), this.lootTable}
            );
         return;
      }
   }

   private int countNonEmptyItems() {
      int count = 0;

      for (ItemStack stack : this.items) {
         if (!stack.isEmpty()) {
            count++;
         }
      }

      return count;
   }

   private void ensureCorrectSize() {
      int expectedSize = containerSize(this.getBlockState());
      if (expectedSize == this.items.size()) {
         this.size = expectedSize;
      } else {
         NonNullList<ItemStack> resized = NonNullList.withSize(expectedSize, ItemStack.EMPTY);

         for (int slot = 0; slot < Math.min(this.items.size(), resized.size()); slot++) {
            resized.set(slot, (ItemStack)this.items.get(slot));
         }

         this.items = resized;
         this.size = expectedSize;
      }
   }

   private void syncChanged() {
      this.setChanged();
      if (this.level != null) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }
}
