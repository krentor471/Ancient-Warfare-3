package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.faction.FactionSimulation;
import net.shadowmage.ancientwarfare.core.faction.TownTrades;

/**
 * A townsperson at their trade by day. The farmer goes out to the fields and reaps ripe crops,
 * sowing again behind him; the woodcutter fells a tree at the edge of town and plants a sapling;
 * the smith stands at the anvil or furnace, the weaver at the loom, the alchemist at the brewing
 * stand, the scribe at the lectern, each working there a while before moving on.
 */
public class TradeWorkGoal extends Goal {
   private static final long EVENING = 11500L;
   private static final int REACH = 28;
   private static final int HEIGHT = 4;
   private static final int WORK_TICKS = 600;
   private static final int REST_TICKS = 400;

   private enum Work {
      FARM, WOOD, MINE, BENCH
   }

   private final FactionNpcEntity npc;
   private BlockPos target;
   private Work work;
   private int left;
   private int rest;
   private int swing;
   /** What the townsperson held before taking up the tool of the trade. */
   private net.minecraft.world.item.ItemStack held = net.minecraft.world.item.ItemStack.EMPTY;

   public TradeWorkGoal(FactionNpcEntity npc) {
      this.npc = npc;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
   }

   private static Work workOf(String trade) {
      return switch (trade) {
         case "farmer", "herbalist", "shepherd" -> Work.FARM;
         case "woodcutter", "carpenter", "charcoal_burner" -> Work.WOOD;
         case "beekeeper", "miller" -> Work.BENCH;
         case "miner", "mason" -> Work.MINE;
         case "" -> null;
         default -> Work.BENCH;
      };
   }

   /** The work blocks of each bench trade. */
   private static Predicate<BlockState> bench(String trade) {
      return switch (trade) {
         case "smith", "armourer", "weaponsmith", "goldsmith", "jeweller", "bellfounder" ->
            state -> state.is(BlockTags.ANVIL) || state.is(Blocks.SMITHING_TABLE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.FURNACE) || state.is(Blocks.GRINDSTONE);
         case "weaver", "tailor", "ropemaker", "dyer" -> state -> state.is(Blocks.LOOM) || state.is(Blocks.CAULDRON) || state.is(Blocks.WATER_CAULDRON);
         case "tanner", "cobbler" -> state -> state.is(Blocks.CAULDRON) || state.is(Blocks.WATER_CAULDRON) || state.is(Blocks.CRAFTING_TABLE);
         case "alchemist", "potion_maker", "physician", "brewer" -> state -> state.is(Blocks.BREWING_STAND) || state.is(Blocks.CAULDRON) || state.is(Blocks.BARREL);
         case "cartographer" -> state -> state.is(Blocks.CARTOGRAPHY_TABLE) || state.is(Blocks.LECTERN);
         case "scribe", "bookbinder" -> state -> state.is(Blocks.LECTERN) || state.is(Blocks.BOOKSHELF);
         case "fletcher", "bowyer", "hunter" -> state -> state.is(Blocks.FLETCHING_TABLE) || state.is(Blocks.TARGET);
         case "butcher", "innkeeper", "fisherman" -> state -> state.is(Blocks.SMOKER) || state.is(Blocks.BARREL) || state.is(Blocks.CAMPFIRE);
         case "baker" -> state -> state.is(Blocks.FURNACE) || state.is(Blocks.SMOKER) || state.is(Blocks.COMPOSTER);
         case "potter", "glassblower", "candlemaker" -> state -> state.is(Blocks.FURNACE) || state.is(Blocks.CRAFTING_TABLE);
         case "stablemaster" -> state -> state.is(Blocks.HAY_BLOCK);
         case "beekeeper" -> state -> state.is(Blocks.BEEHIVE) || state.is(Blocks.BEE_NEST);
         case "miller" -> state -> state.is(Blocks.GRINDSTONE) || state.is(Blocks.HAY_BLOCK);
         default -> state -> state.is(Blocks.CRAFTING_TABLE) || state.is(Blocks.BARREL);
      };
   }

   private boolean day() {
      return this.npc.level().getDayTime() % FactionSimulation.DAY_TICKS < EVENING;
   }

   @Override
   public boolean canUse() {
      if (!(this.npc.level() instanceof ServerLevel level) || !this.day() || this.npc.isBaby() || this.npc.isPassenger() || this.npc.getTarget() != null || this.npc.getLastHurtByMob() != null) {
         return false;
      }

      if (this.rest > 0) {
         this.rest -= 2;
         return false;
      }

      if (this.npc.getRandom().nextInt(4) != 0) {
         return false;
      }

      String trade = TownTrades.tradeOf(level, this.npc);
      this.work = workOf(trade);
      if (this.work == null) {
         return false;
      }

      // Most of the day at the trade's own building, when the town has one: the mill, the hives, the sawmill.
      BlockPos place = this.npc.getRandom().nextInt(3) > 0 ? TownTrades.workplace(level, this.npc, trade) : null;
      this.target = place != null ? this.near(level, place, trade) : this.find(level, trade);
      if (this.target == null) {
         this.rest = REST_TICKS;
      }

      return this.target != null;
   }

   /** What there is to work on at the trade's building: a crop, a log, rock, a bench; else the spot itself. */
   private BlockPos near(ServerLevel level, BlockPos place, String trade) {
      BlockPos found = this.find(level, trade, place, 6);
      if (found != null) {
         return found;
      }

      // Nothing to strike there: the townsperson works at the building with the bench routine.
      this.work = Work.BENCH;
      return place;
   }

   private BlockPos find(ServerLevel level, String trade) {
      return this.find(level, trade, this.npc.blockPosition(), REACH);
   }

   private BlockPos find(ServerLevel level, String trade, BlockPos home, int reach) {
      Predicate<BlockState> wanted = switch (this.work) {
         case FARM -> state -> state.getBlock() instanceof CropBlock;
         case WOOD -> state -> state.is(BlockTags.LOGS);
         case MINE -> state -> state.is(Blocks.STONE) || state.is(BlockTags.IRON_ORES) || state.is(BlockTags.COAL_ORES);
         case BENCH -> bench(trade);
      };
      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
      // A sparse sweep: every other column, so a search stays cheap in a crowded town.
      for (int dx = -reach; dx <= reach; dx += this.work == Work.BENCH ? 1 : 2) {
         for (int dz = -reach; dz <= reach; dz += this.work == Work.BENCH ? 1 : 2) {
            for (int dy = -HEIGHT; dy <= HEIGHT; dy++) {
               pos.set(home.getX() + dx, home.getY() + dy, home.getZ() + dz);
               if (!level.isLoaded(pos)) {
                  continue;
               }

               BlockState state = level.getBlockState(pos);
               if (!wanted.test(state) || this.work == Work.WOOD && !this.tree(level, pos) || this.work == Work.MINE && !level.getBlockState(pos.above()).isAir()) {
                  continue;
               }

               double distance = pos.distSqr(home) + this.npc.getRandom().nextInt(16);
               if (distance < bestDistance) {
                  bestDistance = distance;
                  best = pos.immutable();
               }
            }
         }
      }

      return best;
   }

   /** The foot of a wild tree: a log on soil with natural leaves above, not a house beam. */
   private boolean tree(ServerLevel level, BlockPos log) {
      if (!level.getBlockState(log.below()).is(BlockTags.DIRT)) {
         return false;
      }

      for (int up = 1; up < 10; up++) {
         BlockState state = level.getBlockState(log.above(up));
         if (state.getBlock() instanceof LeavesBlock) {
            return !state.getValue(LeavesBlock.PERSISTENT);
         }

         if (!state.is(BlockTags.LOGS)) {
            return false;
         }
      }

      return false;
   }

   @Override
   public void start() {
      this.left = WORK_TICKS;
      this.swing = 0;
      this.held = this.npc.getMainHandItem().copy();
      this.npc.setItemInHand(InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(switch (this.work) {
         case FARM -> net.minecraft.world.item.Items.IRON_HOE;
         case WOOD -> net.minecraft.world.item.Items.IRON_AXE;
         case MINE -> net.minecraft.world.item.Items.IRON_PICKAXE;
         case BENCH -> net.minecraft.world.item.Items.STONE_AXE;
      }));
      this.npc.getNavigation().moveTo(this.target.getX() + 0.5, this.target.getY(), this.target.getZ() + 0.5, 0.6);
   }

   @Override
   public boolean canContinueToUse() {
      return this.target != null && this.left > 0 && this.day() && this.npc.getTarget() == null && this.npc.getLastHurtByMob() == null;
   }

   @Override
   public void stop() {
      if (this.target != null && this.npc.level() instanceof ServerLevel level) {
         level.destroyBlockProgress(this.npc.getId(), this.target, -1);
      }

      this.target = null;
      this.rest = REST_TICKS;
      this.npc.getNavigation().stop();
      this.npc.setItemInHand(InteractionHand.MAIN_HAND, this.held);
      this.held = net.minecraft.world.item.ItemStack.EMPTY;
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }

   @Override
   public void tick() {
      if (this.target == null) {
         // The work is done; the goal ends on its next check.
         return;
      }

      this.left--;
      this.npc.getLookControl().setLookAt(this.target.getX() + 0.5, this.target.getY() + 0.5, this.target.getZ() + 0.5);
      if (this.npc.distanceToSqr(this.target.getX() + 0.5, this.target.getY(), this.target.getZ() + 0.5) > 6.25) {
         if (this.npc.getNavigation().isDone()) {
            this.npc.getNavigation().moveTo(this.target.getX() + 0.5, this.target.getY(), this.target.getZ() + 0.5, 0.6);
         }

         return;
      }

      this.npc.getNavigation().stop();
      if (++this.swing % 15 != 0) {
         return;
      }

      this.npc.swing(InteractionHand.MAIN_HAND);
      if (!(this.npc.level() instanceof ServerLevel level)) {
         return;
      }

      // Each blow shows: chips and cracks on the log or the rock, sparks at the anvil, earth turned in the field.
      BlockState struck = level.getBlockState(this.target);
      if (this.work == Work.WOOD || this.work == Work.MINE) {
         level.destroyBlockProgress(this.npc.getId(), this.target, Math.min(9, this.swing / 10));
         level.playSound(null, this.target, struck.getSoundType().getHitSound(), net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 0.9F);
      } else if (this.work == Work.FARM) {
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, this.target.getX() + 0.5, this.target.getY() + 0.5, this.target.getZ() + 0.5, 3, 0.3, 0.2, 0.3, 0.0);
      } else {
         level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, this.target.getX() + 0.5, this.target.getY() + 1.0, this.target.getZ() + 0.5, 4, 0.2, 0.1, 0.2, 0.05);
         level.playSound(null, this.target, struck.getSoundType().getHitSound(), net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.2F);
      }

      if (this.swing < 90) {
         return;
      }

      if (this.work == Work.MINE) {
         // The stone is cut and carted off to the town; the rock face stays for the next day.
         level.destroyBlockProgress(this.npc.getId(), this.target, -1);
         level.levelEvent(2001, this.target, Block.getId(struck));
         this.swing = 0;
         return;
      }

      BlockState state = level.getBlockState(this.target);
      switch (this.work) {
         case FARM -> {
            if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
               // Reaped and sown again; the harvest goes to the town's stores, not the ground.
               level.levelEvent(2001, this.target, Block.getId(state));
               level.setBlock(this.target, crop.getStateForAge(0), Block.UPDATE_ALL);
            } else if (state.getBlock() instanceof CropBlock crop) {
               // Not ripe yet: weeded and watered, it grows a little.
               level.setBlock(this.target, crop.getStateForAge(Math.min(crop.getMaxAge(), crop.getAge(state) + 1)), Block.UPDATE_ALL);
            }

            this.target = null;
         }
         case WOOD -> {
            if (state.is(BlockTags.LOGS)) {
               Block sapling = sapling(state);
               BlockPos log = this.target;
               while (level.getBlockState(log).is(BlockTags.LOGS)) {
                  level.destroyBlock(log, false);
                  log = log.above();
               }

               level.setBlock(this.target, sapling.defaultBlockState(), Block.UPDATE_ALL);
               level.destroyBlockProgress(this.npc.getId(), this.target, -1);
            }

            this.target = null;
         }
         default -> {
            // At the bench or the rock face: work on until the time is up.
         }
      }
   }

   private static Block sapling(BlockState log) {
      if (log.is(BlockTags.SPRUCE_LOGS)) {
         return Blocks.SPRUCE_SAPLING;
      }

      if (log.is(BlockTags.BIRCH_LOGS)) {
         return Blocks.BIRCH_SAPLING;
      }

      if (log.is(BlockTags.JUNGLE_LOGS)) {
         return Blocks.JUNGLE_SAPLING;
      }

      if (log.is(BlockTags.ACACIA_LOGS)) {
         return Blocks.ACACIA_SAPLING;
      }

      if (log.is(BlockTags.DARK_OAK_LOGS)) {
         return Blocks.DARK_OAK_SAPLING;
      }

      return Blocks.OAK_SAPLING;
   }
}
