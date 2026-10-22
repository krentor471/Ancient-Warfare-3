package net.shadowmage.ancientwarfare.core.entity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.npc.NpcWorkType;
import net.shadowmage.ancientwarfare.core.construction.ConstructionJobs;

/**
 * A worker with a pickaxe, a shovel or bare hands takes on the building work its owner marked out
 * nearby: it fetches blocks from the job's chest, walks up to each spot in turn and digs it out or
 * sets the block, and carries what it dug back to the chest.
 */
final class BuilderGoal extends Goal {
   private static final int JOB_RANGE = 48;
   private static final int SEARCH_TICKS = 40;
   private static final int STUCK_TICKS = 80;
   private static final int REJECT_TICKS = 600;
   private static final int SHORT_REJECT_TICKS = 40;
   private static final int CARRY = 64;
   private static final double REACH_SQ = 4.5 * 4.5;
   private static final double WORK_REACH_SQ = 5.2 * 5.2;
   private static final int FILL_REACH_DOWN = 8;
   /** Within this many blocks paths to single spots are found; further off the worker first walks closer. */
   private static final double NEAR = 12.0;
   private final WorkerNpcEntity worker;
   private final Map<Long, Integer> rejectedUntil = new HashMap<>();
   /** Blocks taken from the chest to set. */
   private ItemStack materials = ItemStack.EMPTY;
   /** What the worker dug and carries to the chest. */
   private final List<ItemStack> carried = new ArrayList<>();
   private int jobId;
   private BlockPos target;
   private BlockPos stand;
   private BlockPos walkingTo;
   private int nextSearch;
   private int progress;
   private int stuck;
   private double lastDistance;

   BuilderGoal(WorkerNpcEntity worker) {
      this.worker = worker;
      this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   private boolean able() {
      if (this.worker.level().isClientSide || this.worker.getOwner().isEmpty()) {
         return false;
      }

      NpcWorkType type = this.worker.getWorkType();
      return (type == NpcWorkType.MINING || type == NpcWorkType.NONE)
         && this.worker.getCurrentCommand() == NpcCommandType.NONE
         && !this.worker.hasAssignedWorkOrder()
         && !this.worker.shouldRestAtHome()
         && !this.worker.isGuiInteractionPaused()
         && !this.worker.isGettingFood();
   }

   private ConstructionJobs jobs() {
      return ConstructionJobs.get((ServerLevel)this.worker.level());
   }

   private UUID owner() {
      return this.worker.getOwner().orElseThrow();
   }

   @Override
   public boolean canUse() {
      if (this.worker.tickCount < this.nextSearch || !this.able()) {
         return false;
      }

      this.nextSearch = this.worker.tickCount + SEARCH_TICKS;
      if (this.carriedCount() > 0) {
         return true;
      }

      return this.pickJob() != null;
   }

   @Override
   public boolean canContinueToUse() {
      return this.able() && (this.jobs().job(this.jobId) != null || !this.carried.isEmpty() || !this.materials.isEmpty());
   }

   @Override
   public void start() {
      this.target = null;
      this.walkingTo = null;
   }

   @Override
   public void stop() {
      this.releaseTarget();
      this.worker.getNavigation().stop();
      this.walkingTo = null;
      this.worker.setWorkState("idle");
      this.nextSearch = this.worker.tickCount + SEARCH_TICKS;
   }

   private ConstructionJobs.Job pickJob() {
      List<ConstructionJobs.Job> near = this.jobs().near(this.owner(), this.worker.getX(), this.worker.getY(), this.worker.getZ(), JOB_RANGE);
      ConstructionJobs.Job current = this.jobs().job(this.jobId);
      if (current != null && near.remove(current)) {
         near.add(0, current);
      }

      for (ConstructionJobs.Job job : near) {
         if (job.kind().places() && !this.hasMaterials(job) && !this.chestHas(job)) {
            continue;
         }

         this.jobId = job.id();
         return job;
      }

      return null;
   }

   @Override
   public void tick() {
      ConstructionJobs.Job job = this.jobs().job(this.jobId);
      if (job != null && job.supply() != null) {
         this.lastSupply = job.supply();
      }

      if (job == null || this.carriedCount() >= CARRY) {
         this.deliver(job);
         return;
      }

      if (!job.kind().places() && !this.materials.isEmpty()) {
         // Blocks left over from building work go back to the chest with what is dug.
         this.carry(this.materials);
         this.materials = ItemStack.EMPTY;
      }

      if (job.kind().places() && !this.hasMaterials(job)) {
         this.releaseTarget();
         this.fetch(job);
         return;
      }

      if (this.target == null && job.distanceSq(this.worker.getX(), this.worker.getY(), this.worker.getZ()) > NEAR * NEAR) {
         // Paths only reach so far: first get close to the work, then pick spots in it.
         this.worker.setWorkState("moving_to_work");
         BlockPos center = new BlockPos(
            (job.min().getX() + job.max().getX()) / 2, job.max().getY() + 1, (job.min().getZ() + job.max().getZ()) / 2
         );
         if (!this.walkFar(center)) {
            this.jobId = 0;
            this.nextSearch = this.worker.tickCount + REJECT_TICKS;
         }

         return;
      }

      if (this.target == null || !ConstructionJobs.needsWork(this.worker.level(), job, this.target)) {
         this.releaseTarget();
         if (!this.chooseTarget(job)) {
            if (!this.carried.isEmpty()) {
               this.deliver(job);
            } else {
               // Nothing this worker can reach now: try other work a while, then come back.
               this.jobId = 0;
               this.nextSearch = this.worker.tickCount + SEARCH_TICKS * 3;
            }

            return;
         }
      }

      this.worker.getLookControl().setLookAt(this.target.getX() + 0.5, this.target.getY() + 0.5, this.target.getZ() + 0.5);
      if (!this.inReach(job, this.target)) {
         this.worker.setWorkState("moving_to_work");
         if (!this.walk(this.stand)) {
            this.rejectTarget();
         }

         return;
      }

      this.worker.getNavigation().stop();
      this.walkingTo = null;
      this.worker.setWorkState("working");
      if (this.worker.tickCount % 8 == 0) {
         this.worker.swing(InteractionHand.MAIN_HAND);
      }

      if (job.kind() == ConstructionJobs.Kind.DIG) {
         this.dig(job);
      } else {
         this.place(job);
      }
   }

   private boolean chooseTarget(ConstructionJobs.Job job) {
      Set<Long> skip = new HashSet<>();
      int now = this.worker.tickCount;
      this.rejectedUntil.values().removeIf(until -> until < now);
      skip.addAll(this.rejectedUntil.keySet());
      for (int tries = 0; tries < 6; tries++) {
         BlockPos pos = this.jobs().take(this.worker.level(), job, this.worker.getUUID(), skip);
         if (pos == null) {
            return false;
         }

         BlockPos spot = this.findStand(job, pos);
         if (spot != null) {
            this.target = pos;
            this.stand = spot;
            this.progress = 0;
            this.stuck = 0;
            this.lastDistance = Double.MAX_VALUE;
            this.walkingTo = null;
            return true;
         }

         this.jobs().release(pos, this.worker.getUUID());
         skip.add(pos.asLong());
         this.rejectedUntil.put(pos.asLong(), now + REJECT_TICKS);
      }

      return false;
   }

   /** A place to stand within reach of the spot that the worker can walk to and that is not part of the work. */
   private BlockPos findStand(ConstructionJobs.Job job, BlockPos pos) {
      if (this.inReachFrom(job, this.worker.blockPosition(), pos) && !this.worker.blockPosition().equals(pos) && this.worker.onGround()) {
         return this.worker.blockPosition();
      }

      List<BlockPos> spots = new ArrayList<>();
      int above = job.kind() == ConstructionJobs.Kind.FILL ? FILL_REACH_DOWN : 4;
      for (int dy = -4; dy <= above; dy++) {
         for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
               BlockPos spot = pos.offset(dx, dy, dz);
               if (this.inReachFrom(job, spot, pos) && !this.blocksWork(job, spot) && WorksiteApproach.canStandAt(this.worker.level(), spot)) {
                  spots.add(spot);
               }
            }
         }
      }

      spots.sort((a, b) -> Double.compare(this.score(a, pos), this.score(b, pos)));
      int checks = 0;
      for (BlockPos spot : spots) {
         if (checks++ >= 5) {
            break;
         }

         var path = this.worker.getNavigation().createPath(spot, 0);
         if (path != null && path.canReach()) {
            return spot;
         }
      }

      return null;
   }

   private double score(BlockPos spot, BlockPos pos) {
      return Math.sqrt(this.worker.distanceToSqr(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5)) + 1.5 * spot.distSqr(pos);
   }

   /** Standing where the job still has to set a block, or right on top of a block still to dig, gets in the way. */
   private boolean blocksWork(ConstructionJobs.Job job, BlockPos spot) {
      // Filling a hole means standing in it and rising with the fill; a wall's line is kept clear.
      if (job.kind() != ConstructionJobs.Kind.WALL) {
         return false;
      }

      for (BlockPos body : new BlockPos[]{spot, spot.above()}) {
         if (body.getX() >= job.min().getX() && body.getX() <= job.max().getX()
            && body.getZ() >= job.min().getZ() && body.getZ() <= job.max().getZ()
            && body.getY() >= job.min().getY() && body.getY() <= job.max().getY()
            && ConstructionJobs.needsWork(this.worker.level(), job, body)) {
            return true;
         }
      }

      return false;
   }

   private boolean inReachFrom(ConstructionJobs.Job job, BlockPos spot, BlockPos pos) {
      return this.reaches(job, spot.getX() + 0.5, spot.getY() + 1.6, spot.getZ() + 0.5, pos, REACH_SQ);
   }

   private boolean inReach(ConstructionJobs.Job job, BlockPos pos) {
      // A little more room than when the spot was picked: the worker never stands right in the middle of its block.
      return this.reaches(job, this.worker.getX(), this.worker.getEyeY(), this.worker.getZ(), pos, WORK_REACH_SQ)
         && this.worker.onGround()
         && !WorksiteApproach.inDoorway(this.worker);
   }

   /** Within arm's reach; earth for a fill is tipped in from the edge, so any depth below counts as long as it is near across. */
   private boolean reaches(ConstructionJobs.Job job, double x, double eyeY, double z, BlockPos pos, double reachSq) {
      double dx = x - (pos.getX() + 0.5);
      double dy = eyeY - (pos.getY() + 0.5);
      double dz = z - (pos.getZ() + 0.5);
      if (job.kind() == ConstructionJobs.Kind.FILL && dy > 0.0 && dy <= FILL_REACH_DOWN + 2) {
         dy = Math.min(dy, 1.5);
      }

      return dx * dx + dy * dy + dz * dz <= reachSq;
   }

   /** Walks toward a spot; false once the worker has made no headway for a while. */
   private boolean walk(BlockPos spot) {
      double distance = this.worker.distanceToSqr(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
      if (!spot.equals(this.walkingTo)) {
         this.walkingTo = spot;
         this.lastDistance = distance;
         this.stuck = 0;
         if (!this.worker.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, this.worker.playerSprintNavigationSpeed())) {
            return false;
         }
      } else if (this.worker.getNavigation().isDone() && this.worker.tickCount % 10 == 0) {
         // The path ended short of the spot: try again, but this counts toward giving up on it.
         this.worker.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, this.worker.playerSprintNavigationSpeed());
      }

      if (distance < this.lastDistance - 0.05) {
         this.lastDistance = distance;
         this.stuck = 0;
      } else if (++this.stuck >= STUCK_TICKS) {
         this.walkingTo = null;
         return false;
      }

      return true;
   }

   private void dig(ConstructionJobs.Job job) {
      ServerLevel level = (ServerLevel)this.worker.level();
      BlockState state = level.getBlockState(this.target);
      float hardness = state.getDestroySpeed(level, this.target);
      ItemStack tool = this.worker.getMainHandItem();
      int needed = 10 + (int)(hardness * 15.0F);
      if (tool.getItem() instanceof PickaxeItem && state.is(BlockTags.MINEABLE_WITH_PICKAXE)
         || tool.getItem() instanceof ShovelItem && state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
         needed = 6 + (int)(hardness * 6.0F);
      }

      this.progress++;
      level.destroyBlockProgress(this.worker.getId(), this.target, Math.min(9, this.progress * 10 / Math.max(1, needed)));
      if (this.worker.tickCount % 5 == 0) {
         SoundType sound = state.getSoundType(level, this.target, this.worker);
         level.playSound(null, this.target, sound.getHitSound(), SoundSource.NEUTRAL, sound.getVolume() * 0.25F, sound.getPitch() * 0.5F);
      }

      if (this.progress < needed) {
         return;
      }

      level.destroyBlockProgress(this.worker.getId(), this.target, -1);
      List<ItemStack> drops = Block.getDrops(state, level, this.target, level.getBlockEntity(this.target), this.worker, tool);
      level.destroyBlock(this.target, false, this.worker);
      if (job.supply() == null) {
         drops.forEach(drop -> Block.popResource(level, this.target, drop));
      } else {
         drops.forEach(this::carry);
      }

      this.jobs().worked(job);
      this.releaseTarget();
   }

   private void place(ConstructionJobs.Job job) {
      ServerLevel level = (ServerLevel)this.worker.level();
      BlockState current = level.getBlockState(this.target);
      int needed = current.canBeReplaced() ? 8 : 10 + (int)(current.getDestroySpeed(level, this.target) * 10.0F);
      if (++this.progress < needed) {
         return;
      }

      AABB space = new AABB(this.target);
      if (!level.getEntities((Entity)null, space, entity -> !(entity instanceof ItemEntity) && entity.isAlive() && entity.blocksBuilding).isEmpty()) {
         // Someone stands there, maybe the builder itself: another spot first, this one again soon.
         this.rejectTarget(SHORT_REJECT_TICKS);
         return;
      }

      if (!current.isAir()) {
         Block.getDrops(current, level, this.target, level.getBlockEntity(this.target), this.worker, this.worker.getMainHandItem()).forEach(this::carry);
         level.levelEvent(2001, this.target, Block.getId(current));
      }

      level.setBlock(this.target, job.state(), Block.UPDATE_ALL);
      SoundType sound = job.state().getSoundType(level, this.target, this.worker);
      level.playSound(null, this.target, sound.getPlaceSound(), SoundSource.NEUTRAL, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
      this.materials.shrink(1);
      this.jobs().worked(job);
      this.releaseTarget();
   }

   private boolean hasMaterials(ConstructionJobs.Job job) {
      return !this.materials.isEmpty() && this.materials.is(job.state().getBlock().asItem());
   }

   private Container chest(ConstructionJobs.Job job) {
      if (job == null || job.supply() == null || !this.worker.level().isLoaded(job.supply())) {
         return null;
      }

      return this.worker.level().getBlockEntity(job.supply()) instanceof Container container ? container : null;
   }

   private boolean chestHas(ConstructionJobs.Job job) {
      Container chest = this.chest(job);
      if (chest == null) {
         return false;
      }

      Item item = job.state().getBlock().asItem();
      for (int slot = 0; slot < chest.getContainerSize(); slot++) {
         if (chest.getItem(slot).is(item)) {
            return true;
         }
      }

      this.jobs().outOfBlocks(this.worker.level(), job, true);
      return false;
   }

   /** To the chest for the job's blocks; blocks of another job go back first. */
   private void fetch(ConstructionJobs.Job job) {
      Container chest = this.chest(job);
      if (chest == null) {
         this.jobId = 0;
         return;
      }

      if (!this.atChest(job.supply())) {
         this.worker.setWorkState("moving_to_work");
         if (!this.walkToChest(job.supply())) {
            this.jobId = 0;
            this.nextSearch = this.worker.tickCount + REJECT_TICKS;
         }

         return;
      }

      this.worker.getNavigation().stop();
      this.worker.swing(InteractionHand.MAIN_HAND);
      if (!this.materials.isEmpty()) {
         this.carry(this.materials);
         this.materials = ItemStack.EMPTY;
      }

      this.unload(chest);
      Item item = job.state().getBlock().asItem();
      for (int slot = 0; slot < chest.getContainerSize() && this.materials.getCount() < CARRY; slot++) {
         ItemStack stack = chest.getItem(slot);
         if (stack.is(item)) {
            int take = Math.min(stack.getCount(), CARRY - this.materials.getCount());
            if (this.materials.isEmpty()) {
               this.materials = stack.copyWithCount(take);
            } else {
               this.materials.grow(take);
            }

            stack.shrink(take);
            chest.setChanged();
         }
      }

      if (this.materials.isEmpty()) {
         this.jobs().outOfBlocks(this.worker.level(), job, true);
         this.jobId = 0;
         this.nextSearch = this.worker.tickCount + REJECT_TICKS;
      } else {
         this.jobs().outOfBlocks(this.worker.level(), job, false);
      }
   }

   /** Carries what was dug to the job's chest; with no job left, to the nearest chest the worker can find. */
   private void deliver(ConstructionJobs.Job job) {
      this.releaseTarget();
      if (this.carried.isEmpty() && this.materials.isEmpty()) {
         this.jobId = 0;
         return;
      }

      BlockPos supply = job != null ? job.supply() : null;
      if (supply == null) {
         supply = this.lastSupply;
      }

      if (supply == null || !(this.worker.level().getBlockEntity(supply) instanceof Container chest)) {
         // Nowhere to take it: leave it on the ground rather than lose it.
         this.carried.forEach(stack -> Block.popResource(this.worker.level(), this.worker.blockPosition(), stack));
         this.carried.clear();
         if (job == null && !this.materials.isEmpty()) {
            Block.popResource(this.worker.level(), this.worker.blockPosition(), this.materials);
            this.materials = ItemStack.EMPTY;
         }

         return;
      }

      if (!this.atChest(supply)) {
         this.worker.setWorkState("moving_to_work");
         if (!this.walkToChest(supply)) {
            this.lastSupply = null;
         }

         return;
      }

      this.worker.getNavigation().stop();
      this.worker.swing(InteractionHand.MAIN_HAND);
      if (job == null && !this.materials.isEmpty()) {
         this.carry(this.materials);
         this.materials = ItemStack.EMPTY;
      }

      this.unload(chest);
   }

   private BlockPos lastSupply;

   private void unload(Container chest) {
      List<ItemStack> left = new ArrayList<>();
      for (ItemStack stack : this.carried) {
         for (int slot = 0; slot < chest.getContainerSize() && !stack.isEmpty(); slot++) {
            ItemStack inSlot = chest.getItem(slot);
            if (inSlot.isEmpty()) {
               chest.setItem(slot, stack.copy());
               stack.setCount(0);
            } else if (ItemStack.isSameItemSameTags(inSlot, stack) && inSlot.getCount() < inSlot.getMaxStackSize()) {
               int move = Math.min(stack.getCount(), inSlot.getMaxStackSize() - inSlot.getCount());
               inSlot.grow(move);
               stack.shrink(move);
            }
         }

         if (!stack.isEmpty()) {
            left.add(stack);
         }
      }

      chest.setChanged();
      this.carried.clear();
      // A full chest: the rest goes on the ground beside it.
      if (this.worker.level() instanceof ServerLevel level) {
         left.forEach(stack -> Block.popResource(level, this.worker.blockPosition(), stack));
      }
   }

   private boolean atChest(BlockPos chest) {
      return this.worker.distanceToSqr(chest.getX() + 0.5, chest.getY() + 0.5, chest.getZ() + 0.5) <= 9.0;
   }

   /** Heads toward a far spot a stretch at a time; false when no way on can be found. */
   private boolean walkFar(BlockPos goal) {
      if (this.worker.getNavigation().isDone() || this.worker.tickCount % 40 == 0) {
         Vec3 toward = DefaultRandomPos.getPosTowards(this.worker, 12, 6, Vec3.atBottomCenterOf(goal), Math.PI / 2.0);
         if (toward == null) {
            toward = Vec3.atBottomCenterOf(goal);
         }

         if (!this.worker.getNavigation().moveTo(toward.x, toward.y, toward.z, this.worker.playerSprintNavigationSpeed())) {
            return ++this.stuck < STUCK_TICKS / 10;
         }
      }

      this.stuck = 0;
      this.walkingTo = null;
      return true;
   }

   private boolean walkToChest(BlockPos chest) {
      this.lastSupply = chest;
      if (this.worker.distanceToSqr(chest.getX() + 0.5, chest.getY(), chest.getZ() + 0.5) > NEAR * NEAR) {
         return this.walkFar(chest);
      }

      BlockPos spot = chest.equals(this.chestSpotFor) ? this.chestSpot : null;
      if (spot == null) {
         spot = WorksiteApproach.find(this.worker, chest, 6.0);
         this.chestSpotFor = chest;
         this.chestSpot = spot;
         if (spot == null) {
            return false;
         }
      }

      if (!this.walk(spot)) {
         this.chestSpotFor = null;
         return false;
      }

      return true;
   }

   private BlockPos chestSpotFor;
   private BlockPos chestSpot;

   private void carry(ItemStack stack) {
      if (stack.isEmpty()) {
         return;
      }

      for (ItemStack held : this.carried) {
         if (ItemStack.isSameItemSameTags(held, stack) && held.getCount() < held.getMaxStackSize()) {
            int move = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());
            held.grow(move);
            stack.shrink(move);
            if (stack.isEmpty()) {
               return;
            }
         }
      }

      this.carried.add(stack.copy());
      stack.setCount(0);
   }

   private int carriedCount() {
      return this.carried.stream().mapToInt(ItemStack::getCount).sum();
   }

   private void releaseTarget() {
      if (this.target != null) {
         if (this.worker.level() instanceof ServerLevel level) {
            level.destroyBlockProgress(this.worker.getId(), this.target, -1);
            this.jobs().release(this.target, this.worker.getUUID());
         }

         this.target = null;
      }

      this.progress = 0;
   }

   private void rejectTarget() {
      this.rejectTarget(REJECT_TICKS);
   }

   private void rejectTarget(int ticks) {
      if (this.target != null) {
         this.rejectedUntil.put(this.target.asLong(), this.worker.tickCount + ticks);
      }

      this.releaseTarget();
      this.walkingTo = null;
   }
}
