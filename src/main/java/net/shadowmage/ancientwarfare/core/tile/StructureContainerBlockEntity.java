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
import net.shadowmage.ancientwarfare.core.compat.IceAndFireStructureLootCompat;
import net.shadowmage.ancientwarfare.core.compat.SlashBladeStructureLootCompat;
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
   private static final String ICE_AND_FIRE_THEME_TAG = "IceAndFireLootTheme";
   private static final String ICE_AND_FIRE_TIER_TAG = "IceAndFireLootTier";
   private static final String ICE_AND_FIRE_SEED_TAG = "IceAndFireLootSeed";
   private static final String SLASH_BLADE_PENDING_TAG = "SlashBladeLootPending";
   private static final String SLASH_BLADE_SEED_TAG = "SlashBladeLootSeed";
   private int size;
   private NonNullList<ItemStack> items;
   @Nullable
   private ResourceLocation lootTable;
   private CompoundTag legacyLootSettings = new CompoundTag();
   private long lootSeed;
   private int lootRolls = 1;
   private String iceAndFireLootTheme = "";
   private int iceAndFireLootTier;
   private long iceAndFireLootSeed;
   private boolean slashBladeLootPending;
   private long slashBladeLootSeed;
   private int openCount;
   private float lidProgress;
   private float previousLidProgress;

   public StructureContainerBlockEntity(BlockPos pos, BlockState state) {
      this(pos, state, containerSize(state));
   }

   public StructureContainerBlockEntity(BlockPos pos, BlockState state, int size) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.STRUCTURE_CONTAINER), pos, state);
      this.size = size;
      this.items = NonNullList.m_122780_(size, ItemStack.f_41583_);
   }

   public static int containerSize(BlockState state) {
      if (state.m_60713_(AWCoreRegistryAccess.block(AWCoreIds.URN))) {
         return 27;
      } else {
         return !state.m_60713_(AWCoreRegistryAccess.block(AWCoreIds.WOODEN_COFFIN)) && !state.m_60713_(AWCoreRegistryAccess.block(AWCoreIds.STONE_COFFIN))
            ? 27
            : 27;
      }
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      this.ensureCorrectSize();
      tag.m_128405_("Size", this.size);
      if (this.lootTable != null) {
         tag.m_128359_("LootTable", this.lootTable.toString());
         tag.m_128356_("LootTableSeed", this.lootSeed);
         tag.m_128405_("LootRolls", this.lootRolls);
      }

      if (!this.legacyLootSettings.m_128456_()) {
         tag.m_128365_("LegacyLootSettings", this.legacyLootSettings.m_6426_());
      }

      if (!this.iceAndFireLootTheme.isBlank()) {
         tag.m_128359_("IceAndFireLootTheme", this.iceAndFireLootTheme);
         tag.m_128405_("IceAndFireLootTier", this.iceAndFireLootTier);
         tag.m_128356_("IceAndFireLootSeed", this.iceAndFireLootSeed);
      }

      if (this.slashBladeLootPending) {
         tag.m_128379_("SlashBladeLootPending", true);
         tag.m_128356_("SlashBladeLootSeed", this.slashBladeLootSeed);
      }

      ContainerHelper.m_18973_(tag, this.items);
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.ensureCorrectSize();
      this.lootTable = ResourceLocation.m_135820_(tag.m_128461_("LootTable"));
      this.legacyLootSettings = tag.m_128441_("LegacyLootSettings") ? tag.m_128469_("LegacyLootSettings").m_6426_() : new CompoundTag();
      this.lootSeed = tag.m_128454_("LootTableSeed");
      this.lootRolls = Math.max(1, tag.m_128451_("LootRolls"));
      this.iceAndFireLootTheme = tag.m_128461_("IceAndFireLootTheme");
      this.iceAndFireLootTier = tag.m_128451_("IceAndFireLootTier");
      this.iceAndFireLootSeed = tag.m_128454_("IceAndFireLootSeed");
      this.slashBladeLootPending = tag.m_128471_("SlashBladeLootPending");
      this.slashBladeLootSeed = tag.m_128454_("SlashBladeLootSeed");
      ContainerHelper.m_18980_(tag, this.items);
      if (this.f_58857_ instanceof ServerLevel) {
         AncientWarfareCore.LOG
            .debug(
               "Structure container loaded: pos={}, block={}, type={}, pendingTable={}, rolls={}, items={}",
               new Object[]{
                  this.f_58858_, this.m_58900_().m_60734_(), this.getClass().getSimpleName(), this.lootTable, this.lootRolls, this.countNonEmptyItems()
               }
            );
         AncientWarfareCore.LOG
            .debug(
               "Structure container legacy settings loaded: pos={}, present={}, keys={}",
               new Object[]{this.f_58858_, !this.legacyLootSettings.m_128456_(), this.legacyLootSettings.m_128431_().size()}
            );
      }
   }

   public int m_6643_() {
      this.ensureCorrectSize();
      return this.items.size();
   }

   public boolean m_7983_() {
      this.ensureCorrectSize();
      return this.items.stream().allMatch(ItemStack::m_41619_);
   }

   public ItemStack m_8020_(int slot) {
      this.ensureCorrectSize();
      return slot >= 0 && slot < this.items.size() ? (ItemStack)this.items.get(slot) : ItemStack.f_41583_;
   }

   public ItemStack m_7407_(int slot, int amount) {
      this.ensureCorrectSize();
      ItemStack removed = slot >= 0 && slot < this.items.size() ? ContainerHelper.m_18969_(this.items, slot, amount) : ItemStack.f_41583_;
      if (!removed.m_41619_()) {
         this.syncChanged();
      }

      return removed;
   }

   public ItemStack m_8016_(int slot) {
      this.ensureCorrectSize();
      ItemStack removed = slot >= 0 && slot < this.items.size() ? ContainerHelper.m_18966_(this.items, slot) : ItemStack.f_41583_;
      if (!removed.m_41619_()) {
         this.syncChanged();
      }

      return removed;
   }

   public void m_6836_(int slot, ItemStack stack) {
      this.ensureCorrectSize();
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

   public void m_5856_(Player player) {
      if (!player.m_5833_() && this.isCoffin()) {
         this.openCount++;
         if (this.f_58857_ != null) {
            this.f_58857_.m_7696_(this.f_58858_, this.m_58900_().m_60734_(), 1, this.openCount);
         }
      }
   }

   public void m_5785_(Player player) {
      if (!player.m_5833_() && this.isCoffin()) {
         this.openCount = Math.max(0, this.openCount - 1);
         if (this.f_58857_ != null) {
            this.f_58857_.m_7696_(this.f_58858_, this.m_58900_().m_60734_(), 1, this.openCount);
         }
      }
   }

   public boolean m_7531_(int id, int value) {
      if (id == 1) {
         this.openCount = Math.max(0, value);
         return true;
      } else {
         return super.m_7531_(id, value);
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
      return this.m_58900_().m_60713_(AWCoreRegistryAccess.block(AWCoreIds.WOODEN_COFFIN))
         || this.m_58900_().m_60713_(AWCoreRegistryAccess.block(AWCoreIds.STONE_COFFIN));
   }

   public void m_6211_() {
      this.ensureCorrectSize();
      this.items.clear();
      this.syncChanged();
   }

   public Component m_5446_() {
      return Component.m_237115_(this.m_58900_().m_60734_().m_7705_());
   }

   @Nullable
   public AbstractContainerMenu m_7208_(int containerId, Inventory playerInventory, Player player) {
      this.unpackLoot(player);
      this.ensureCorrectSize();
      return new StructureContainerMenu(containerId, playerInventory, this.f_58858_);
   }

   public void configureStructureLoot(ResourceLocation table, int rolls, long seed) {
      this.lootTable = table;
      this.lootRolls = Math.max(1, rolls);
      this.lootSeed = seed;
      this.m_6596_();
      AncientWarfareCore.LOG
         .debug(
            "Structure container loot configured: pos={}, block={}, table={}, rolls={}",
            new Object[]{this.f_58858_, this.m_58900_().m_60734_(), table, this.lootRolls}
         );
   }

   public void storeLegacyLootSettings(CompoundTag settings) {
      if (settings != null && !settings.m_128456_()) {
         this.legacyLootSettings = settings.m_6426_();
         this.m_6596_();
      }
   }

   public boolean hasLegacyLootSettings() {
      return !this.legacyLootSettings.m_128456_();
   }

   public void configureIceAndFireLoot(IceAndFireStructureLootCompat.Settings settings, long seed) {
      if (settings != null) {
         this.iceAndFireLootTheme = settings.theme();
         this.iceAndFireLootTier = settings.tier();
         this.iceAndFireLootSeed = seed;
         this.m_6596_();
      }
   }

   public void configureSlashBladeLoot(long seed) {
      this.slashBladeLootPending = true;
      this.slashBladeLootSeed = seed;
      this.m_6596_();
   }

   public void unpackLootForDestruction() {
      this.unpackLoot(null);
   }

   private void unpackLoot(@Nullable Player player) {
      if (this.f_58857_ instanceof ServerLevel serverLevel && (this.lootTable != null || !this.iceAndFireLootTheme.isBlank() || this.slashBladeLootPending)) {
         AncientWarfareCore.LOG
            .debug(
               "Structure container loot generation start: pos={}, block={}, table={}, rolls={}, itemsBefore={}, player={}",
               new Object[]{
                  this.f_58858_,
                  this.m_58900_().m_60734_(),
                  this.lootTable,
                  this.lootRolls,
                  this.countNonEmptyItems(),
                  player == null ? "null" : player.m_36316_().getName()
               }
            );
         List<ItemStack> generated = new ArrayList<>();
         if (this.lootTable != null) {
            ResourceLocation tableId = StructureLootTierResolver.resolveStoredTable(this.lootTable);
            this.lootTable = null;
            LootTable table = serverLevel.m_7654_().m_278653_().m_278676_(tableId);
            Builder paramsBuilder = new Builder(serverLevel).m_287286_(LootContextParams.f_81460_, Vec3.m_82512_(this.f_58858_));
            if (player != null) {
               paramsBuilder.m_287239_(player.m_36336_());
            }

            LootParams params = paramsBuilder.m_287235_(LootContextParamSets.f_81411_);
            Set<String> seenResearch = new HashSet<>();
            int effectiveRolls = AWCorePlatform.gameplayConfig().effectiveLootRolls(tableId, this.lootRolls, this.lootSeed);

            for (int roll = 0; roll < effectiveRolls; roll++) {
               long rollSeed = this.lootSeed == 0L ? serverLevel.f_46441_.m_188505_() : this.lootSeed + (long)roll;
               List<ItemStack> loot = LootRewardHelper.getLootWithoutDuplicateResearch(table, params, rollSeed, seenResearch);
               generated.addAll(
                  LootRewardHelper.scaleItemCounts(loot, AWCorePlatform.gameplayConfig().lootItemCountMultiplier(), rollSeed ^ 4850189118611868748L)
               );
            }
         }

         if (!this.iceAndFireLootTheme.isBlank() && AWCorePlatform.gameplayConfig().iceAndFireBonusLoot()) {
            List<ItemStack> bonus = IceAndFireStructureLootCompat.roll(serverLevel, this.iceAndFireLootTheme, this.iceAndFireLootTier, this.iceAndFireLootSeed);
            generated.addAll(
               LootRewardHelper.scaleItemCounts(
                  bonus, AWCorePlatform.gameplayConfig().lootItemCountMultiplier(), this.iceAndFireLootSeed ^ 5279139334996641353L
               )
            );
         }

         this.iceAndFireLootTheme = "";
         this.iceAndFireLootTier = 0;
         this.iceAndFireLootSeed = 0L;
         if (this.slashBladeLootPending && AWCorePlatform.gameplayConfig().slashBladeBonusLoot()) {
            List<ItemStack> bonus = SlashBladeStructureLootCompat.roll(serverLevel, this.slashBladeLootSeed);
            generated.addAll(
               LootRewardHelper.scaleItemCounts(
                  bonus, AWCorePlatform.gameplayConfig().lootItemCountMultiplier(), this.slashBladeLootSeed ^ 6002244229328424001L
               )
            );
         }

         this.slashBladeLootPending = false;
         this.slashBladeLootSeed = 0L;
         List<Integer> slots = new ArrayList<>();

         for (int slot = 0; slot < this.items.size(); slot++) {
            if (((ItemStack)this.items.get(slot)).m_41619_()) {
               slots.add(slot);
            }
         }

         Collections.shuffle(slots, new Random(this.lootSeed == 0L ? serverLevel.f_46441_.m_188505_() : this.lootSeed));

         for (ItemStack stack : generated) {
            int remaining = stack.m_41613_();
            int maxStackSize = stack.m_41741_();

            while (remaining > 0 && !slots.isEmpty()) {
               ItemStack split = stack.m_41777_();
               split.m_41764_(Math.min(remaining, maxStackSize));
               this.items.set(slots.remove(slots.size() - 1), split);
               remaining -= split.m_41613_();
            }
         }

         this.syncChanged();
         AncientWarfareCore.LOG
            .debug(
               "Structure container loot generation end: pos={}, block={}, itemsAfter={}, pendingTable={}",
               new Object[]{this.f_58858_, this.m_58900_().m_60734_(), this.countNonEmptyItems(), this.lootTable}
            );
         return;
      }
   }

   private int countNonEmptyItems() {
      int count = 0;

      for (ItemStack stack : this.items) {
         if (!stack.m_41619_()) {
            count++;
         }
      }

      return count;
   }

   private void ensureCorrectSize() {
      int expectedSize = containerSize(this.m_58900_());
      if (expectedSize == this.items.size()) {
         this.size = expectedSize;
      } else {
         NonNullList<ItemStack> resized = NonNullList.m_122780_(expectedSize, ItemStack.f_41583_);

         for (int slot = 0; slot < Math.min(this.items.size(), resized.size()); slot++) {
            resized.set(slot, (ItemStack)this.items.get(slot));
         }

         this.items = resized;
         this.size = expectedSize;
      }
   }

   private void syncChanged() {
      this.m_6596_();
      if (this.f_58857_ != null) {
         this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 3);
      }
   }
}
