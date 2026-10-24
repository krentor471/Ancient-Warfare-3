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
   }

   public static void configureBasicLoot(ChestBlockEntity chest, ResourceLocation table, long seed) {
      configureLootData(chest, table, 1, seed);
   }

   public static void storeLegacyLootSettings(ChestBlockEntity chest, CompoundTag settings) {
      if (settings != null && !settings.isEmpty()) {
         data(chest).put("AWLegacyLootSettings", settings.copy());
         chest.setChanged();
      }
   }

   public static boolean hasLegacyLootSettings(ChestBlockEntity chest) {
      return data(chest).contains("AWLegacyLootSettings");
   }

   private static void configureLootData(ChestBlockEntity chest, ResourceLocation table, int rolls, long seed) {
      CompoundTag data = data(chest);
      data.putString("AWStructureLootTable", table.toString());
      data.putInt("AWStructureLootRolls", Math.max(1, rolls));
      data.putLong("AWStructureLootSeed", seed);
      chest.setChanged();
      if (chest.getLevel() != null) {
         chest.getLevel().sendBlockUpdated(chest.getBlockPos(), chest.getBlockState(), chest.getBlockState(), 2);
      }

      AncientWarfareCore.LOG.debug("Configured structure chest loot at {}: {} x{}", new Object[]{chest.getBlockPos(), table, rolls});
   }

   public static boolean hasConfiguredLoot(ChestBlockEntity chest) {
      return data(chest).contains("AWStructureLootTable");
   }

   public static boolean generateConnectedLoot(Level level, BlockPos pos, Player player) {
      List<BlockPos> positions = connectedChestPositions(level, pos);
      boolean generated = false;
      long distributionSeed = connectedDistributionSeed(level, positions);

      for (BlockPos chestPos : positions) {
         BlockEntity var10 = level.getBlockEntity(chestPos);
         if (var10 instanceof ChestBlockEntity) {
            ChestBlockEntity chest = (ChestBlockEntity)var10;
            if (hasConfiguredLoot(chest)) {
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
      if (level.getBlockEntity(pos) instanceof ChestBlockEntity clicked && data(clicked).getBoolean("AWLootrConverted")) {
         List<BlockPos> positions = connectedChestPositions(level, pos);
         List<AdvancedLootChestBlock.RestoredChest> saved = new ArrayList<>();

         for (BlockPos chestPos : positions) {
            BlockState state = level.getBlockState(chestPos);
            if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest && data(chest).getBoolean("AWLootrConverted")) {
               List<ItemStack> items = new ArrayList<>();

               for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                  items.add(chest.getItem(slot).copy());
               }

               CompoundTag persistentData = data(chest).copy();
               persistentData.remove("AWLootrConverted");
               persistentData.remove("AWLootrId");
               BlockState replacement = (BlockState)((BlockState)((BlockState)AWCoreRegistryAccess.block(AWCoreIds.ADVANCED_LOOT_CHEST)
                        .defaultBlockState()
                        .setValue(FACING, (Direction)state.getValue(FACING)))
                     .setValue(TYPE, (ChestType)state.getValue(TYPE)))
                  .setValue(WATERLOGGED, (Boolean)state.getValue(WATERLOGGED));
               saved.add(new AdvancedLootChestBlock.RestoredChest(chestPos, replacement, items, persistentData));
            }
         }

         for (AdvancedLootChestBlock.RestoredChest chest : saved) {
            level.setBlock(chest.pos(), chest.state(), 3);
         }

         for (AdvancedLootChestBlock.RestoredChest chest : saved) {
            BlockEntity var18 = level.getBlockEntity(chest.pos());
            if (var18 instanceof ChestBlockEntity) {
               ChestBlockEntity restored = (ChestBlockEntity)var18;

               for (int slot = 0; slot < Math.min(restored.getContainerSize(), chest.items().size()); slot++) {
                  restored.setItem(slot, chest.items().get(slot));
               }

               data(restored).merge(chest.persistentData());
               restored.setChanged();
            }
         }

         return !saved.isEmpty();
      }

      return false;
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (level.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         Logger var10000;
         Object[] var10002;
         boolean var10005;
         label33: {
            var10000 = AncientWarfareCore.LOG;
            var10002 = new Object[]{
               pos, state.getBlock(), level.getBlockEntity(pos) == null ? "null" : level.getBlockEntity(pos).getClass().getSimpleName(), null, null, null, null, null
            };
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest && hasConfiguredLoot(chest)) {
               var10005 = true;
               break label33;
            }

            var10005 = false;
         }

         var10002[3] = var10005;
         var10002[4] = level.getBlockEntity(pos) instanceof ChestBlockEntity chest ? data(chest).getString("AWStructureLootTable") : "";
         var10002[5] = level.getBlockEntity(pos) instanceof ChestBlockEntity chestx ? data(chestx).getInt("AWStructureLootRolls") : 0;
         var10002[6] = player.getGameProfile().getName();
         var10002[7] = player.getAbilities().instabuild;
         var10000.debug("Advanced loot chest use: pos={}, stateBlock={}, blockEntity={}, configured={}, table={}, rolls={}, player={}, creative={}", var10002);
         this.convertConnectedChest(level, pos, player);
         BlockState converted = level.getBlockState(pos);
         return converted.is(Blocks.CHEST) ? converted.use(level, player, hand, hit) : InteractionResult.CONSUME;
      }
   }

   private void convertConnectedChest(Level level, BlockPos pos, Player player) {
      List<BlockPos> positions = connectedChestPositions(level, pos);
      long distributionSeed = connectedDistributionSeed(level, positions);
      boolean generated = false;
      List<AdvancedLootChestBlock.SavedChest> saved = new ArrayList<>();

      for (BlockPos chestPos : positions) {
         BlockState chestState = level.getBlockState(chestPos);
         if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            boolean pending = hasConfiguredLoot(chest);
            generateLoot(chest, player);
            generated |= pending;
         }
      }

      if (generated && positions.size() > 1) {
         redistributeConnectedContents(level, positions, distributionSeed);
      }

      for (BlockPos chestPosx : positions) {
         BlockState chestState = level.getBlockState(chestPosx);
         if (level.getBlockEntity(chestPosx) instanceof ChestBlockEntity chest) {
            saved.add(new AdvancedLootChestBlock.SavedChest(chestPosx, vanillaState(chestState), chest.saveWithoutMetadata()));
         }
      }

      for (AdvancedLootChestBlock.SavedChest chest : saved) {
         level.setBlock(chest.pos(), chest.state(), 3);
      }

      for (AdvancedLootChestBlock.SavedChest chest : saved) {
         if (level.getBlockEntity(chest.pos()) instanceof ChestBlockEntity vanillaChest) {
            vanillaChest.load(chest.data());
            vanillaChest.setChanged();
         }
      }
   }

   private static List<BlockPos> connectedChestPositions(Level level, BlockPos pos) {
      List<BlockPos> positions = new ArrayList<>();
      positions.add(pos);
      BlockState state = level.getBlockState(pos);
      if (state.getBlock() instanceof ChestBlock && state.hasProperty(TYPE) && state.getValue(TYPE) != ChestType.SINGLE) {
         BlockPos neighbor = pos.relative(ChestBlock.getConnectedDirection(state));
         BlockState neighborState = level.getBlockState(neighbor);
         if (neighborState.getBlock() instanceof ChestBlock
            && neighborState.hasProperty(TYPE)
            && neighborState.getValue(TYPE) != ChestType.SINGLE
            && neighborState.getValue(FACING) == state.getValue(FACING)) {
            positions.add(neighbor);
         }

         return positions;
      } else {
         return positions;
      }
   }

   private static BlockState vanillaState(BlockState state) {
      return (BlockState)((BlockState)((BlockState)Blocks.CHEST.defaultBlockState().setValue(FACING, (Direction)state.getValue(FACING)))
            .setValue(TYPE, (ChestType)state.getValue(TYPE)))
         .setValue(WATERLOGGED, (Boolean)state.getValue(WATERLOGGED));
   }

   private static long connectedDistributionSeed(Level level, List<BlockPos> positions) {
      long seed = 4922246660975051592L;

      for (BlockPos chestPos : positions.stream().sorted().toList()) {
         long part = chestPos.asLong();
         if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest && data(chest).contains("AWStructureLootSeed")) {
            part ^= data(chest).getLong("AWStructureLootSeed");
         }

         seed = Long.rotateLeft(seed ^ part, 17) * -7046029254386353131L;
      }

      return seed;
   }

   private static void redistributeConnectedContents(Level level, List<BlockPos> positions, long seed) {
      List<ItemStack> contents = new ArrayList<>();
      List<AdvancedLootChestBlock.ChestSlot> slots = new ArrayList<>();

      for (BlockPos chestPos : positions) {
         BlockEntity slot = level.getBlockEntity(chestPos);
         if (slot instanceof ChestBlockEntity) {
            ChestBlockEntity chest = (ChestBlockEntity)slot;

            for (int slotx = 0; slotx < chest.getContainerSize(); slotx++) {
               ItemStack stack = chest.getItem(slotx);
               if (!stack.isEmpty()) {
                  contents.add(stack.copy());
               }

               slots.add(new AdvancedLootChestBlock.ChestSlot(chest, slotx));
            }

            chest.clearContent();
         }
      }

      Collections.shuffle(slots, new Random(seed ^ 6001140263786661170L));

      for (int index = 0; index < contents.size() && index < slots.size(); index++) {
         AdvancedLootChestBlock.ChestSlot target = slots.get(index);
         target.chest().setItem(target.slot(), contents.get(index));
      }

      for (BlockPos chestPosx : positions) {
         if (level.getBlockEntity(chestPosx) instanceof ChestBlockEntity chest) {
            chest.setChanged();
         }
      }
   }

   public static void generateLoot(ChestBlockEntity chest, Player player) {
      CompoundTag data = data(chest);
      String tableName = data.getString("AWStructureLootTable");
      ResourceLocation tableId = ResourceLocation.tryParse(tableName);
      if (chest.getLevel() instanceof ServerLevel serverLevel) {
         AncientWarfareCore.LOG
            .debug(
               "Advanced loot generation start: pos={}, blockEntity={}, table={}, rolls={}, itemsBefore={}, player={}",
               new Object[]{
                  chest.getBlockPos(),
                  chest.getClass().getSimpleName(),
                  tableName,
                  data.getInt("AWStructureLootRolls"),
                  countNonEmpty(chest),
                  player == null ? "null" : player.getGameProfile().getName()
               }
            );
         ResourceLocation generatedTable = null;
         long configuredSeed = data.getLong("AWStructureLootSeed");
         if (tableId != null) {
            long seed = configuredSeed;
            generatedTable = StructureLootTierResolver.resolveStoredTable(tableId);
            int rolls = AWCorePlatform.gameplayConfig().effectiveLootRolls(generatedTable, Math.max(1, data.getInt("AWStructureLootRolls")), configuredSeed);
            LootTable table = serverLevel.getServer().getLootData().getLootTable(generatedTable);
            LootParams params = new Builder(serverLevel)
               .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(chest.getBlockPos()))
               .withLuck(player.getLuck())
               .create(LootContextParamSets.CHEST);
            Set<String> seenResearch = new HashSet<>();

            for (int roll = 0; roll < rolls; roll++) {
               long rollSeed = seed == 0L ? serverLevel.random.nextLong() : seed + (long)roll;
               List<ItemStack> loot = LootRewardHelper.getLootWithoutDuplicateResearch(table, params, rollSeed, seenResearch);

               for (ItemStack stack : LootRewardHelper.scaleItemCounts(
                  loot, AWCorePlatform.gameplayConfig().lootItemCountMultiplier(), rollSeed ^ 4850189118611868748L
               )) {
                  insertIntoEmptySlot(chest, stack);
               }
            }

            data.remove("AWStructureLootTable");
            data.remove("AWStructureLootRolls");
            data.remove("AWStructureLootSeed");
         }

         chest.setChanged();
         AncientWarfareCore.LOG
            .debug(
               "Advanced loot generation end: pos={}, generatedTable={}, itemsAfter={}, pendingMetadata={}",
               new Object[]{chest.getBlockPos(), generatedTable, countNonEmpty(chest), hasConfiguredLoot(chest)}
            );
         if (generatedTable != null) {
            AncientWarfareCore.LOG.info("Generated structure chest loot at {} from {}", chest.getBlockPos(), generatedTable);
         }
      }
   }

   private static void insertIntoEmptySlot(ChestBlockEntity chest, ItemStack stack) {
      int remaining = stack.getCount();
      int maxStackSize = stack.getMaxStackSize();

      for (int slot = 0; slot < chest.getContainerSize() && remaining > 0; slot++) {
         if (chest.getItem(slot).isEmpty()) {
            ItemStack split = stack.copy();
            split.setCount(Math.min(remaining, maxStackSize));
            chest.setItem(slot, split);
            remaining -= split.getCount();
         }
      }
   }

   private static int countNonEmpty(ChestBlockEntity chest) {
      int count = 0;

      for (int slot = 0; slot < chest.getContainerSize(); slot++) {
         if (!chest.getItem(slot).isEmpty()) {
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
