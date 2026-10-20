package net.shadowmage.ancientwarfare.core.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireStructureLootCompat;
import net.shadowmage.ancientwarfare.core.compat.SlashBladeStructureLootCompat;
import net.shadowmage.ancientwarfare.core.structure.LootRewardHelper;
import net.shadowmage.ancientwarfare.core.structure.StructureLootTierResolver;
import org.slf4j.Logger;

public class AdvancedLootChestBlock extends ChestBlock {
   private static final String TABLE_TAG = "AWStructureLootTable";
   private static final String ROLLS_TAG = "AWStructureLootRolls";
   private static final String SEED_TAG = "AWStructureLootSeed";
   private static final String LEGACY_SETTINGS_TAG = "AWLegacyLootSettings";
   private static final String LEGACY_LOOTR_CONVERTED_TAG = "AWLootrConverted";
   private static final String LEGACY_LOOTR_ID_TAG = "AWLootrId";

   public AdvancedLootChestBlock(Properties properties, Supplier<BlockEntityType<? extends ChestBlockEntity>> blockEntityType) {
      super(properties, blockEntityType);
   }

   public static void configureLoot(ChestBlockEntity chest, ResourceLocation table, int rolls, long seed) {
      configureLootData(chest, table, rolls, seed);
      SlashBladeStructureLootCompat.configure(chest, seed ^ 6002244229328424001L);
   }

   public static void configureBasicLoot(ChestBlockEntity chest, ResourceLocation table, long seed) {
      configureLootData(chest, table, 1, seed);
   }

   public static void storeLegacyLootSettings(ChestBlockEntity chest, CompoundTag settings) {
      if (settings != null && !settings.m_128456_()) {
         data(chest).m_128365_("AWLegacyLootSettings", settings.m_6426_());
         chest.m_6596_();
      }
   }

   public static boolean hasLegacyLootSettings(ChestBlockEntity chest) {
      return data(chest).m_128441_("AWLegacyLootSettings");
   }

   private static void configureLootData(ChestBlockEntity chest, ResourceLocation table, int rolls, long seed) {
      CompoundTag data = data(chest);
      data.m_128359_("AWStructureLootTable", table.toString());
      data.m_128405_("AWStructureLootRolls", Math.max(1, rolls));
      data.m_128356_("AWStructureLootSeed", seed);
      chest.m_6596_();
      if (chest.m_58904_() != null) {
         chest.m_58904_().m_7260_(chest.m_58899_(), chest.m_58900_(), chest.m_58900_(), 2);
      }

      AncientWarfareCore.LOG.debug("Configured structure chest loot at {}: {} x{}", new Object[]{chest.m_58899_(), table, rolls});
   }

   public static boolean hasConfiguredLoot(ChestBlockEntity chest) {
      return data(chest).m_128441_("AWStructureLootTable");
   }

   public static boolean generateConnectedLoot(Level level, BlockPos pos, Player player) {
      List<BlockPos> positions = connectedChestPositions(level, pos);
      boolean generated = false;
      long distributionSeed = connectedDistributionSeed(level, positions);

      for (BlockPos chestPos : positions) {
         BlockEntity var10 = level.m_7702_(chestPos);
         if (var10 instanceof ChestBlockEntity) {
            ChestBlockEntity chest = (ChestBlockEntity)var10;
            if (hasConfiguredLoot(chest) || IceAndFireStructureLootCompat.hasPendingLoot(chest) || SlashBladeStructureLootCompat.hasPendingLoot(chest)) {
               generateLoot(chest, player);
               generated = true;
            }
         }
      }

      if (generated && positions.size() > 1) {
         redistributeConnectedContents(level, positions, distributionSeed);
      }

      return generated;
   }

   public static boolean restoreFormerLootrChest(Level level, BlockPos pos) {
      if (level.m_7702_(pos) instanceof ChestBlockEntity clicked && data(clicked).m_128471_("AWLootrConverted")) {
         List<BlockPos> positions = connectedChestPositions(level, pos);
         List<AdvancedLootChestBlock.RestoredChest> saved = new ArrayList<>();

         for (BlockPos chestPos : positions) {
            BlockState state = level.m_8055_(chestPos);
            if (level.m_7702_(chestPos) instanceof ChestBlockEntity chest && data(chest).m_128471_("AWLootrConverted")) {
               List<ItemStack> items = new ArrayList<>();

               for (int slot = 0; slot < chest.m_6643_(); slot++) {
                  items.add(chest.m_8020_(slot).m_41777_());
               }

               CompoundTag persistentData = data(chest).m_6426_();
               persistentData.m_128473_("AWLootrConverted");
               persistentData.m_128473_("AWLootrId");
               BlockState replacement = (BlockState)((BlockState)((BlockState)AWCoreRegistryAccess.block(AWCoreIds.ADVANCED_LOOT_CHEST)
                        .m_49966_()
                        .m_61124_(f_51478_, (Direction)state.m_61143_(f_51478_)))
                     .m_61124_(f_51479_, (ChestType)state.m_61143_(f_51479_)))
                  .m_61124_(f_51480_, (Boolean)state.m_61143_(f_51480_));
               saved.add(new AdvancedLootChestBlock.RestoredChest(chestPos, replacement, items, persistentData));
            }
         }

         for (AdvancedLootChestBlock.RestoredChest chest : saved) {
            level.m_7731_(chest.pos(), chest.state(), 3);
         }

         for (AdvancedLootChestBlock.RestoredChest chest : saved) {
            BlockEntity var18 = level.m_7702_(chest.pos());
            if (var18 instanceof ChestBlockEntity) {
               ChestBlockEntity restored = (ChestBlockEntity)var18;

               for (int slot = 0; slot < Math.min(restored.m_6643_(), chest.items().size()); slot++) {
                  restored.m_6836_(slot, chest.items().get(slot));
               }

               data(restored).m_128391_(chest.persistentData());
               restored.m_6596_();
            }
         }

         return !saved.isEmpty();
      }

      return false;
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (level.f_46443_) {
         return InteractionResult.SUCCESS;
      } else {
         Logger var10000;
         Object[] var10002;
         boolean var10005;
         label33: {
            var10000 = AncientWarfareCore.LOG;
            var10002 = new Object[]{
               pos, state.m_60734_(), level.m_7702_(pos) == null ? "null" : level.m_7702_(pos).getClass().getSimpleName(), null, null, null, null, null
            };
            if (level.m_7702_(pos) instanceof ChestBlockEntity chest && hasConfiguredLoot(chest)) {
               var10005 = true;
               break label33;
            }

            var10005 = false;
         }

         var10002[3] = var10005;
         var10002[4] = level.m_7702_(pos) instanceof ChestBlockEntity chest ? data(chest).m_128461_("AWStructureLootTable") : "";
         var10002[5] = level.m_7702_(pos) instanceof ChestBlockEntity chestx ? data(chestx).m_128451_("AWStructureLootRolls") : 0;
         var10002[6] = player.m_36316_().getName();
         var10002[7] = player.m_150110_().f_35937_;
         var10000.debug("Advanced loot chest use: pos={}, stateBlock={}, blockEntity={}, configured={}, table={}, rolls={}, player={}, creative={}", var10002);
         this.convertConnectedChest(level, pos, player);
         BlockState converted = level.m_8055_(pos);
         return converted.m_60713_(Blocks.f_50087_) ? converted.m_60664_(level, player, hand, hit) : InteractionResult.CONSUME;
      }
   }

   private void convertConnectedChest(Level level, BlockPos pos, Player player) {
      List<BlockPos> positions = connectedChestPositions(level, pos);
      long distributionSeed = connectedDistributionSeed(level, positions);
      boolean generated = false;
      List<AdvancedLootChestBlock.SavedChest> saved = new ArrayList<>();

      for (BlockPos chestPos : positions) {
         BlockState chestState = level.m_8055_(chestPos);
         if (level.m_7702_(chestPos) instanceof ChestBlockEntity chest) {
            boolean pending = hasConfiguredLoot(chest)
               || IceAndFireStructureLootCompat.hasPendingLoot(chest)
               || SlashBladeStructureLootCompat.hasPendingLoot(chest);
            generateLoot(chest, player);
            generated |= pending;
         }
      }

      if (generated && positions.size() > 1) {
         redistributeConnectedContents(level, positions, distributionSeed);
      }

      for (BlockPos chestPosx : positions) {
         BlockState chestState = level.m_8055_(chestPosx);
         if (level.m_7702_(chestPosx) instanceof ChestBlockEntity chest) {
            saved.add(new AdvancedLootChestBlock.SavedChest(chestPosx, vanillaState(chestState), chest.m_187482_()));
         }
      }

      for (AdvancedLootChestBlock.SavedChest chest : saved) {
         level.m_7731_(chest.pos(), chest.state(), 3);
      }

      for (AdvancedLootChestBlock.SavedChest chest : saved) {
         if (level.m_7702_(chest.pos()) instanceof ChestBlockEntity vanillaChest) {
            vanillaChest.m_142466_(chest.data());
            vanillaChest.m_6596_();
         }
      }
   }

   private static List<BlockPos> connectedChestPositions(Level level, BlockPos pos) {
      List<BlockPos> positions = new ArrayList<>();
      positions.add(pos);
      BlockState state = level.m_8055_(pos);
      if (state.m_60734_() instanceof ChestBlock && state.m_61138_(f_51479_) && state.m_61143_(f_51479_) != ChestType.SINGLE) {
         BlockPos neighbor = pos.m_121945_(ChestBlock.m_51584_(state));
         BlockState neighborState = level.m_8055_(neighbor);
         if (neighborState.m_60734_() instanceof ChestBlock
            && neighborState.m_61138_(f_51479_)
            && neighborState.m_61143_(f_51479_) != ChestType.SINGLE
            && neighborState.m_61143_(f_51478_) == state.m_61143_(f_51478_)) {
            positions.add(neighbor);
         }

         return positions;
      } else {
         return positions;
      }
   }

   private static BlockState vanillaState(BlockState state) {
      return (BlockState)((BlockState)((BlockState)Blocks.f_50087_.m_49966_().m_61124_(f_51478_, (Direction)state.m_61143_(f_51478_)))
            .m_61124_(f_51479_, (ChestType)state.m_61143_(f_51479_)))
         .m_61124_(f_51480_, (Boolean)state.m_61143_(f_51480_));
   }

   private static long connectedDistributionSeed(Level level, List<BlockPos> positions) {
      long seed = 4922246660975051592L;

      for (BlockPos chestPos : positions.stream().sorted().toList()) {
         long part = chestPos.m_121878_();
         if (level.m_7702_(chestPos) instanceof ChestBlockEntity chest && data(chest).m_128441_("AWStructureLootSeed")) {
            part ^= data(chest).m_128454_("AWStructureLootSeed");
         }

         seed = Long.rotateLeft(seed ^ part, 17) * -7046029254386353131L;
      }

      return seed;
   }

   private static void redistributeConnectedContents(Level level, List<BlockPos> positions, long seed) {
      List<ItemStack> contents = new ArrayList<>();
      List<AdvancedLootChestBlock.ChestSlot> slots = new ArrayList<>();

      for (BlockPos chestPos : positions) {
         BlockEntity slot = level.m_7702_(chestPos);
         if (slot instanceof ChestBlockEntity) {
            ChestBlockEntity chest = (ChestBlockEntity)slot;

            for (int slotx = 0; slotx < chest.m_6643_(); slotx++) {
               ItemStack stack = chest.m_8020_(slotx);
               if (!stack.m_41619_()) {
                  contents.add(stack.m_41777_());
               }

               slots.add(new AdvancedLootChestBlock.ChestSlot(chest, slotx));
            }

            chest.m_6211_();
         }
      }

      Collections.shuffle(slots, new Random(seed ^ 6001140263786661170L));

      for (int index = 0; index < contents.size() && index < slots.size(); index++) {
         AdvancedLootChestBlock.ChestSlot target = slots.get(index);
         target.chest().m_6836_(target.slot(), contents.get(index));
      }

      for (BlockPos chestPosx : positions) {
         if (level.m_7702_(chestPosx) instanceof ChestBlockEntity chest) {
            chest.m_6596_();
         }
      }
   }

   public static void generateLoot(ChestBlockEntity chest, Player player) {
      CompoundTag data = data(chest);
      String tableName = data.m_128461_("AWStructureLootTable");
      ResourceLocation tableId = ResourceLocation.m_135820_(tableName);
      if (chest.m_58904_() instanceof ServerLevel serverLevel) {
         AncientWarfareCore.LOG
            .debug(
               "Advanced loot generation start: pos={}, blockEntity={}, table={}, rolls={}, itemsBefore={}, player={}",
               new Object[]{
                  chest.m_58899_(),
                  chest.getClass().getSimpleName(),
                  tableName,
                  data.m_128451_("AWStructureLootRolls"),
                  countNonEmpty(chest),
                  player == null ? "null" : player.m_36316_().getName()
               }
            );
         ResourceLocation generatedTable = null;
         long configuredSeed = data.m_128454_("AWStructureLootSeed");
         if (tableId != null) {
            long seed = configuredSeed;
            generatedTable = StructureLootTierResolver.resolveStoredTable(tableId);
            int rolls = AWCorePlatform.gameplayConfig().effectiveLootRolls(generatedTable, Math.max(1, data.m_128451_("AWStructureLootRolls")), configuredSeed);
            LootTable table = serverLevel.m_7654_().m_278653_().m_278676_(generatedTable);
            LootParams params = new Builder(serverLevel)
               .m_287286_(LootContextParams.f_81460_, Vec3.m_82512_(chest.m_58899_()))
               .m_287239_(player.m_36336_())
               .m_287235_(LootContextParamSets.f_81411_);
            Set<String> seenResearch = new HashSet<>();

            for (int roll = 0; roll < rolls; roll++) {
               long rollSeed = seed == 0L ? serverLevel.f_46441_.m_188505_() : seed + (long)roll;
               List<ItemStack> loot = LootRewardHelper.getLootWithoutDuplicateResearch(table, params, rollSeed, seenResearch);

               for (ItemStack stack : LootRewardHelper.scaleItemCounts(
                  loot, AWCorePlatform.gameplayConfig().lootItemCountMultiplier(), rollSeed ^ 4850189118611868748L
               )) {
                  insertIntoEmptySlot(chest, stack);
               }
            }

            data.m_128473_("AWStructureLootTable");
            data.m_128473_("AWStructureLootRolls");
            data.m_128473_("AWStructureLootSeed");
         }

         List<ItemStack> iceAndFireLoot = IceAndFireStructureLootCompat.consumeChestLoot(chest);
         if (AWCorePlatform.gameplayConfig().iceAndFireBonusLoot()) {
            for (ItemStack stack : LootRewardHelper.scaleItemCounts(
               iceAndFireLoot, AWCorePlatform.gameplayConfig().lootItemCountMultiplier(), configuredSeed ^ 5279139334996641353L
            )) {
               insertIntoEmptySlot(chest, stack);
            }
         }

         List<ItemStack> slashBladeLoot = SlashBladeStructureLootCompat.consumeChestLoot(chest);
         if (AWCorePlatform.gameplayConfig().slashBladeBonusLoot()) {
            for (ItemStack stack : LootRewardHelper.scaleItemCounts(
               slashBladeLoot, AWCorePlatform.gameplayConfig().lootItemCountMultiplier(), configuredSeed ^ 6002244229328424001L
            )) {
               insertIntoEmptySlot(chest, stack);
            }
         }

         chest.m_6596_();
         AncientWarfareCore.LOG
            .debug(
               "Advanced loot generation end: pos={}, generatedTable={}, itemsAfter={}, pendingMetadata={}",
               new Object[]{chest.m_58899_(), generatedTable, countNonEmpty(chest), hasConfiguredLoot(chest)}
            );
         if (generatedTable != null) {
            AncientWarfareCore.LOG.info("Generated structure chest loot at {} from {}", chest.m_58899_(), generatedTable);
         }
      }
   }

   private static void insertIntoEmptySlot(ChestBlockEntity chest, ItemStack stack) {
      int remaining = stack.m_41613_();
      int maxStackSize = stack.m_41741_();

      for (int slot = 0; slot < chest.m_6643_() && remaining > 0; slot++) {
         if (chest.m_8020_(slot).m_41619_()) {
            ItemStack split = stack.m_41777_();
            split.m_41764_(Math.min(remaining, maxStackSize));
            chest.m_6836_(slot, split);
            remaining -= split.m_41613_();
         }
      }
   }

   private static int countNonEmpty(ChestBlockEntity chest) {
      int count = 0;

      for (int slot = 0; slot < chest.m_6643_(); slot++) {
         if (!chest.m_8020_(slot).m_41619_()) {
            count++;
         }
      }

      return count;
   }

   private static CompoundTag data(ChestBlockEntity chest) {
      return AWCorePlatform.blockEntityData(chest);
   }

   private static record ChestSlot(ChestBlockEntity chest, int slot) {
   }

   private static record RestoredChest(BlockPos pos, BlockState state, List<ItemStack> items, CompoundTag persistentData) {
   }

   private static record SavedChest(BlockPos pos, BlockState state, CompoundTag data) {
   }
}
