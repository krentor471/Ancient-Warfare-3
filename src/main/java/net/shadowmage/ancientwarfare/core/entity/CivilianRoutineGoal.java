package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.shadowmage.ancientwarfare.core.faction.FactionCaravans;
import net.shadowmage.ancientwarfare.core.faction.FactionSimulation;
import net.shadowmage.ancientwarfare.core.faction.FactionTownLife;
import net.shadowmage.ancientwarfare.core.faction.FactionTravellers;
import net.shadowmage.ancientwarfare.core.faction.FactionWorldData;

/**
 * The day of a townsperson: work and wander by day, gather in the square in the evening, and at
 * night go to a free bed nearby and sleep in it until morning. Without a bed in reach they stay
 * put at night instead of roaming the dark. Travellers and fairgoers keep their own ways.
 */
public class CivilianRoutineGoal extends Goal {
   private static final long EVENING = 11500L;
   private static final long NIGHT = 13000L;
   private static final long MORNING = 23500L;
   private static final int BED_REACH = 20;
   private static final int BED_HEIGHT = 6;
   private static final int SQUARE_SPREAD = 6;
   private static final int RETHINK_TICKS = 40;
   private final FactionNpcEntity npc;
   private BlockPos bed;
   private boolean searchedTonight;
   private int searches;
   private final java.util.Set<BlockPos> unreachable = new java.util.HashSet<>();
   private int stuckTicks;
   private double lastDistance;
   /** Beds already chosen tonight, so a household spreads over the beds rather than all making for one. */
   private static final java.util.Map<BlockPos, java.util.UUID> CLAIMED = new java.util.HashMap<>();
   private static final int MAX_SEARCHES = 4;
   private BlockPos spot;
   private int rethink;

   public CivilianRoutineGoal(FactionNpcEntity npc) {
      this.npc = npc;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
   }

   private long timeOfDay() {
      return this.npc.level().getDayTime() % FactionSimulation.DAY_TICKS;
   }

   private boolean night() {
      long time = this.timeOfDay();
      return time >= NIGHT && time < MORNING;
   }

   private boolean evening() {
      long time = this.timeOfDay();
      return time >= EVENING && time < NIGHT;
   }

   private boolean keepsOwnWays() {
      return FactionTravellers.isTraveller(this.npc)
         || FactionTownLife.isFairgoer(this.npc)
         || FactionTownLife.isThief(this.npc)
         || FactionCaravans.caravanId(this.npc) != 0
         || this.npc.getTags().contains("AWBuilder")
         || this.npc.getTarget() != null
         || this.npc.getLastHurtByMob() != null;
   }

   @Override
   public boolean canUse() {
      if (!(this.npc.level() instanceof ServerLevel) || this.keepsOwnWays()) {
         return false;
      }

      return this.night() || this.evening() && this.square() != null;
   }

   @Override
   public boolean canContinueToUse() {
      return this.canUse();
   }

   @Override
   public void start() {
      this.rethink = 0;
      this.spot = null;
   }

   @Override
   public void stop() {
      this.wake();
      this.spot = null;
      this.searchedTonight = false;
      this.searches = 0;
      this.unreachable.clear();
      this.release();
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return false;
   }

   @Override
   public void tick() {
      if (this.npc.hurtTime > 0) {
         this.wake();
         return;
      }

      if (--this.rethink > 0) {
         return;
      }

      this.rethink = RETHINK_TICKS / 2;
      if (this.night()) {
         this.toBed();
      } else {
         this.wake();
         this.toSquare();
      }
   }

   private void release() {
      if (this.bed != null) {
         CLAIMED.remove(this.bed, this.npc.getUUID());
      }

      this.bed = null;
   }

   private void toBed() {
      if (this.npc.isSleeping()) {
         return;
      }

      if (this.bed == null || !freeBed(this.npc.level().getBlockState(this.bed))) {
         this.release();
         if (!this.searchedTonight || this.searches < MAX_SEARCHES) {
            this.searchedTonight = true;
            this.searches++;
            this.bed = this.findBed();
            if (this.bed != null) {
               CLAIMED.put(this.bed, this.npc.getUUID());
               this.stuckTicks = 0;
               this.lastDistance = Double.MAX_VALUE;
            }
         }
      }

      if (this.bed == null) {
         // Nowhere to sleep: wait out the night by the square rather than crowd a doorway, and the town builds another house.
         if (this.searches >= MAX_SEARCHES && this.npc.level() instanceof ServerLevel level) {
            net.shadowmage.ancientwarfare.core.faction.FactionGrowth.homeless(level, this.npc);
         }

         this.toSquare();
         return;
      }

      double distance = this.npc.distanceToSqr(this.bed.getX() + 0.5, this.bed.getY() + 0.5, this.bed.getZ() + 0.5);
      if (distance < 4.0) {
         this.npc.getNavigation().stop();
         this.npc.startSleeping(this.bed);
         return;
      }

      // No nearer for a while: the way in is blocked or the bed out of reach; another bed is sought.
      if (distance < this.lastDistance - 0.5) {
         this.lastDistance = distance;
         this.stuckTicks = 0;
      } else if ((this.stuckTicks += RETHINK_TICKS / 2) > RETHINK_TICKS * 4) {
         this.unreachable.add(this.bed);
         this.release();
         return;
      }

      if (this.npc.getNavigation().isDone()) {
         this.npc.getNavigation().moveTo(this.bed.getX() + 0.5, this.bed.getY(), this.bed.getZ() + 0.5, 0.8);
      }
   }

   private void toSquare() {
      BlockPos square = this.square();
      if (square == null) {
         return;
      }

      if (this.spot == null) {
         this.spot = square.offset(
            this.npc.getRandom().nextInt(SQUARE_SPREAD * 2 + 1) - SQUARE_SPREAD, 0, this.npc.getRandom().nextInt(SQUARE_SPREAD * 2 + 1) - SQUARE_SPREAD
         );
      }

      if (this.npc.distanceToSqr(this.spot.getX() + 0.5, this.npc.getY(), this.spot.getZ() + 0.5) > 4.0 && this.npc.getNavigation().isDone()) {
         this.npc.getNavigation().moveTo(this.spot.getX() + 0.5, this.spot.getY(), this.spot.getZ() + 0.5, 0.7);
      }
   }

   private void wake() {
      if (this.npc.isSleeping()) {
         this.npc.stopSleeping();
      }
   }

   /** The middle of the town this person lives in, or null away from any. */
   private BlockPos square() {
      if (!(this.npc.level() instanceof ServerLevel level)) {
         return null;
      }

      FactionWorldData.Settlement settlement = FactionSimulation.settlementAt(FactionWorldData.get(level), this.npc.getFactionName(), this.npc.getX(), this.npc.getZ(), 24);
      return settlement == null || !settlement.built() ? null : settlement.center();
   }

   private static boolean freeBed(BlockState state) {
      return state.is(BlockTags.BEDS) && state.getValue(BedBlock.PART) == BedPart.HEAD && !state.getValue(BedBlock.OCCUPIED);
   }

   private BlockPos findBed() {
      BlockPos origin = this.npc.blockPosition();
      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;
      BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
      for (int dy = -BED_HEIGHT; dy <= BED_HEIGHT; dy++) {
         for (int dx = -BED_REACH; dx <= BED_REACH; dx++) {
            for (int dz = -BED_REACH; dz <= BED_REACH; dz++) {
               pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
               if (!this.npc.level().hasChunkAt(pos)) {
                  continue;
               }

               BlockState state = this.npc.level().getBlockState(pos);
               double distance = dx * dx + dz * dz + dy * dy * 4;
               java.util.UUID claimant = CLAIMED.get(pos);
               if (distance < bestDistance && freeBed(state) && !this.unreachable.contains(pos) && (claimant == null || claimant.equals(this.npc.getUUID()) || !(this.npc.level() instanceof ServerLevel level) || level.getEntity(claimant) == null)) {
                  best = pos.immutable();
                  bestDistance = distance;
               }
            }
         }
      }

      return best;
   }
}
