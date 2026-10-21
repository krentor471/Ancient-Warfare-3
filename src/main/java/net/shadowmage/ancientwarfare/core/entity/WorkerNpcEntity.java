package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.npc.NpcWorkType;
import net.shadowmage.ancientwarfare.core.common.npc.WorkOrderData;
import net.shadowmage.ancientwarfare.core.common.npc.WorkerProfessionResolver;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkSite;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkSites;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkerAccess;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;
import net.shadowmage.ancientwarfare.core.menu.NpcInventoryMenu;

public class WorkerNpcEntity extends PlayerOwnedNpcEntity implements WorkerAccess {
   private static final EntityDataAccessor<String> WORK_STATE = SynchedEntityData.defineId(WorkerNpcEntity.class, EntityDataSerializers.STRING);
   private static final String DEFAULT_WORK_STATE = "idle";
   private static final String MOVE_TO_WORK_STATE = "moving_to_work";
   private static final String WORKING_STATE = "working";
   private static final String NO_WORK_SITE_STATE = "no_work_site";
   private static final String NO_WORK_STATE = "no_work";
   private static final String RETURNING_HOME_STATE = "returning_home";
   private static final String SHELTERING_STATE = "sheltering";
   private static final String TAG_WORK_STATE = "WorkState";
   private static final String TAG_WORK_ORDER_INDEX = "WorkOrderIndex";
   private static final String TAG_WORK_TICKS_AT_SITE = "WorkTicksAtSite";
   private int workOrderIndex;
   private int workTicksAtSite;
   private BlockPos sleepBed;

   public WorkerNpcEntity(EntityType<? extends WorkerNpcEntity> entityType, Level level) {
      super(entityType, level);
   }

   public static Builder createAttributes() {
      return createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 24.0);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new WorkerNpcEntity.SleepGoal(this, this.playerSprintNavigationSpeed()));
      this.goalSelector.addGoal(2, new WorkerNpcEntity.MoveToCommandGoal(this, 1.0, 2.0));
      this.goalSelector.addGoal(4, new WorkerNpcEntity.WorkOrderGoal(this, 1.0, 2.0));
      this.goalSelector.addGoal(5, new WorkerNpcEntity.NearbyWorkGoal(this, 1.0));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
   }

   @Override
   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(WORK_STATE, "idle");
   }

   @Override
   public void tick() {
      if (!this.level().isClientSide) {
         this.migrateLegacyOrderState();
         if (this.isNoAi() && !this.isGuiInteractionPaused()) {
            this.setNoAi(false);
         }

         if (this.isSleeping() && !this.shouldSleepNow()) {
            this.stopSleeping();
            this.sleepBed = null;
         }
      }

      super.tick();
   }

   private void migrateLegacyOrderState() {
      if (!AWCoreRegistryAccess.is(this.getOrdersStack(), AWCoreIds.WORK_ORDER)) {
         if (AWCoreRegistryAccess.is(this.getUpkeepStack(), AWCoreIds.WORK_ORDER)) {
            ItemStack legacyOrder = this.getUpkeepStack().copy();
            super.setOrdersStack(legacyOrder);
            super.setUpkeepStack(ItemStack.EMPTY);
            this.resetWorkOrderProgress();
            if (this.getCurrentCommand() != NpcCommandType.NONE) {
               this.clearCommand();
               this.setFollowTarget(null);
            }
         }
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putString("WorkState", this.getWorkState());
      tag.putInt("WorkOrderIndex", this.workOrderIndex);
      tag.putInt("WorkTicksAtSite", this.workTicksAtSite);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.getBoolean("NoAI")) {
         this.setNoAi(false);
      }

      this.setWorkState(tag.getString("WorkState"));
      this.workOrderIndex = tag.getInt("WorkOrderIndex");
      this.workTicksAtSite = tag.getInt("WorkTicksAtSite");
   }

   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (this.tryAcceptFood(player, hand)) {
         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else if (player.getItemInHand(hand).getItem() instanceof CommandBatonItem) {
         if (!this.level().isClientSide && this.hasCommandPermission(player)) {
            CommandBatonItem.clickNpc(player.getItemInHand(hand), player, this);
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else {
         if (!this.level().isClientSide && this.hasCommandPermission(player)) {
            AWCorePlatform.openMenu(
               player,
               new SimpleMenuProvider((containerId, inventory, openedPlayer) -> new NpcInventoryMenu(containerId, inventory, this), this.getDisplayName()),
               buffer -> buffer.writeInt(this.getId())
            );
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
      }
   }

   public String getWorkState() {
      return (String)this.entityData.get(WORK_STATE);
   }

   public boolean isWorkingAnimation() {
      String state = this.getWorkState();
      return "working".equals(state);
   }

   public void setWorkState(String workState) {
      this.entityData.set(WORK_STATE, workState != null && !workState.isBlank() ? workState : "idle");
   }

   @Override
   public void setOrdersStack(ItemStack stack) {
      super.setOrdersStack(stack);
      this.resetWorkOrderProgress();
      if (AWCoreRegistryAccess.is(stack, AWCoreIds.WORK_ORDER) && this.getCurrentCommand() != NpcCommandType.NONE) {
         this.clearCommand();
         this.setFollowTarget(null);
      }
   }

   @Override
   public void setUpkeepStack(ItemStack stack) {
      if (AWCoreRegistryAccess.is(stack, AWCoreIds.WORK_ORDER)) {
         super.setOrdersStack(stack);
         super.setUpkeepStack(ItemStack.EMPTY);
         this.resetWorkOrderProgress();
         if (this.getCurrentCommand() != NpcCommandType.NONE) {
            this.clearCommand();
            this.setFollowTarget(null);
         }
      } else {
         super.setUpkeepStack(stack);
      }
   }

   public void resetWorkOrderProgress() {
      this.workOrderIndex = 0;
      this.workTicksAtSite = 0;
   }

   private boolean shouldSleepNow() {
      if (!this.level().isClientSide && this.getCurrentCommand() == NpcCommandType.NONE && !this.isGuiInteractionPaused() && !this.isGettingFood()) {
         boolean nightShift = AWCoreRegistryAccess.is(this.getOrdersStack(), AWCoreIds.WORK_ORDER) && WorkOrderData.isNightShift(this.getOrdersStack());
         return nightShift ? !this.level().isNight() : this.level().isNight();
      } else {
         return false;
      }
   }

   private boolean shouldShelterAtHome() {
      return !this.level().isClientSide
         && this.hasHome()
         && this.level().isRaining()
         && this.getCurrentCommand() == NpcCommandType.NONE
         && !this.isGuiInteractionPaused()
         && !this.isGettingFood();
   }

   boolean shouldRestAtHome() {
      return this.shouldSleepNow() || this.shouldShelterAtHome();
   }

   private boolean bedReservedByAnother(BlockPos bed) {
      AABB search = new AABB(bed).inflate(24.0, 8.0, 24.0);

      for (WorkerNpcEntity other : this.level().getEntitiesOfClass(WorkerNpcEntity.class, search)) {
         if (other != this && bed.equals(other.sleepBed)) {
            return true;
         }

         if (other != this && other.isSleeping() && other.getSleepingPos().isPresent() && bed.equals(other.getSleepingPos().get())) {
            return true;
         }
      }

      return false;
   }

   private BlockPos findAvailableBed(BlockPos center) {
      MutableBlockPos cursor = new MutableBlockPos();

      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-16, -4, -16), center.offset(16, 4, 16))) {
         cursor.set(pos);
         BlockState state = this.level().getBlockState(cursor);
         if (state.getBlock() instanceof BedBlock && !this.bedReservedByAnother(cursor.immutable())) {
            return cursor.immutable();
         }
      }

      return null;
   }

   public NpcWorkType getWorkType() {
      return WorkerProfessionResolver.workTypeFor(this.getMainHandItem());
   }

   @Override
   public String getNpcSubtype() {
      return WorkerProfessionResolver.subtypeFor(this.getMainHandItem());
   }

   @Override
   public Component getTypeName() {
      return Component.translatable("entity.ancientwarfare.worker." + this.getNpcSubtype());
   }

   @Override
   public float getWorkEffectiveness(NpcWorkType type) {
      return this.canWorkAt(type) ? 1.0F : 0.0F;
   }

   @Override
   public boolean canWorkAt(NpcWorkType type) {
      NpcWorkType ownType = this.getWorkType();
      return type != null && type != NpcWorkType.NONE && type == ownType;
   }

   @Override
   public double getWorkRangeSq() {
      return 4.0;
   }

   private boolean hasAssignedWorkOrder() {
      return AWCoreRegistryAccess.is(this.getOrdersStack(), AWCoreIds.WORK_ORDER) && !WorkOrderData.getWorkEntries(this.getOrdersStack()).isEmpty();
   }

   private static class MoveToCommandGoal extends Goal {
      private final WorkerNpcEntity worker;
      private final double speedModifier;
      private final double stopDistanceSq;
      private BlockPos target;

      private MoveToCommandGoal(WorkerNpcEntity worker, double speedModifier, double stopDistance) {
         this.worker = worker;
         this.speedModifier = speedModifier;
         this.stopDistanceSq = stopDistance * stopDistance;
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (this.worker.getCurrentCommand() != NpcCommandType.MOVE) {
            return false;
         } else {
            this.target = this.worker.getCommandTarget();
            return !BlockPos.ZERO.equals(this.target) && this.distanceToTargetSq() > this.stopDistanceSq;
         }
      }

      public boolean canContinueToUse() {
         return this.worker.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() > this.stopDistanceSq;
      }

      public void tick() {
         this.worker.getLookControl().setLookAt((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5);
         this.worker
            .getNavigation()
            .moveTo(
               (double)this.target.getX() + 0.5,
               (double)this.target.getY(),
               (double)this.target.getZ() + 0.5,
               this.worker.playerSprintNavigationSpeed()
            );
      }

      public void stop() {
         if (this.worker.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() <= this.stopDistanceSq) {
            this.worker.completeMoveCommand();
            this.worker.setWorkState("idle");
         }

         this.target = null;
         this.worker.getNavigation().stop();
      }

      private double distanceToTargetSq() {
         return this.worker.distanceToSqr((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5);
      }
   }

   private static class NearbyWorkGoal extends Goal {
      private static final int SEARCH_RADIUS = 24;
      private static final int VERTICAL_SEARCH_RADIUS = 8;
      private static final int SEARCH_RETRY_TICKS = 40;
      private static final int UNREACHABLE_RETRY_TICKS = 200;
      private static final int DEFAULT_WORK_TICKS = 10;
      private static final int STUCK_TICKS = 60;
      private final WorkerNpcEntity worker;
      private final double speedModifier;
      private final Map<BlockPos, Integer> rejectedUntil = new HashMap<>();
      private BlockPos target;
      private BlockPos approach;
      private int nextSearchTick;
      private int repathCooldown;
      private int stuckTicks;
      private double lastDistanceSq = Double.MAX_VALUE;
      private boolean failed;

      private NearbyWorkGoal(WorkerNpcEntity worker, double speedModifier) {
         this.worker = worker;
         this.speedModifier = speedModifier;
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (this.canAutomaticallyWork() && this.worker.tickCount >= this.nextSearchTick && this.worker.onGround()) {
            this.pruneRejectedTargets();
            this.target = this.findNearestAvailableWorksite();
            if (this.target == null) {
               this.worker.setWorkState("no_work");
               this.nextSearchTick = this.worker.tickCount + 40;
               return false;
            } else {
               this.approach = WorksiteApproach.find(this.worker, this.target, this.worker.getWorkRangeSq());
               if (this.approach == null) {
                  // Nowhere to stand next to it that the worker can walk to: try the next site instead of pushing at walls.
                  this.rejectedUntil.put(this.target, this.worker.tickCount + 200);
                  this.worker.setWorkState("no_work_site");
                  this.nextSearchTick = this.worker.tickCount + 1;
                  this.target = null;
                  return false;
               }

               this.failed = false;
               this.repathCooldown = 0;
               this.stuckTicks = 0;
               this.lastDistanceSq = this.distanceToTargetSq();
               this.worker.workTicksAtSite = 0;
               return true;
            }
         } else {
            return false;
         }
      }

      public boolean canContinueToUse() {
         if (this.canAutomaticallyWork() && !this.failed && this.target != null) {
            WorkSite site = WorkSites.get(this.worker.level(), this.target).orElse(null);
            return site != null && this.worker.canWorkAt(site.getWorkType()) && site.hasWork();
         } else {
            return false;
         }
      }

      public void tick() {
         if (this.target != null) {
            this.worker.getLookControl().setLookAt((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5);
            double distanceSq = this.distanceToTargetSq();
            if (distanceSq > this.worker.getWorkRangeSq() && !WorksiteApproach.isAt(this.worker, this.approach)
               || WorksiteApproach.inDoorway(this.worker)) {
               this.worker.workTicksAtSite = 0;
               this.worker.setWorkState("moving_to_work");
               if (distanceSq < this.lastDistanceSq - 0.05) {
                  this.stuckTicks = 0;
                  this.lastDistanceSq = distanceSq;
               } else {
                  this.stuckTicks++;
               }

               if (this.repathCooldown-- <= 0) {
                  this.repathCooldown = 20;
                  boolean pathFound = this.worker
                     .getNavigation()
                     .moveTo(
                        (double)this.approach.getX() + 0.5,
                        (double)this.approach.getY(),
                        (double)this.approach.getZ() + 0.5,
                        this.worker.playerSprintNavigationSpeed()
                     );
                  if (!pathFound) {
                     this.rejectCurrentTarget();
                     return;
                  }
               }

               if (this.stuckTicks >= 60) {
                  this.rejectCurrentTarget();
               }
            } else {
               this.worker.getNavigation().stop();
               WorkSite site = WorkSites.get(this.worker.level(), this.target).orElse(null);
               if (site != null && this.worker.canWorkAt(site.getWorkType()) && site.hasWork()) {
                  this.worker.setWorkState("working");
                  this.worker.workTicksAtSite++;
                  site.onWorkerTick(this.worker);
                  if (this.worker.tickCount % 10 == 0) {
                     this.worker.swing(InteractionHand.MAIN_HAND);
                  }

                  if (this.worker.workTicksAtSite >= 10) {
                     site.addEnergyFromWorker(this.worker);
                     this.worker.workTicksAtSite = 0;
                  }
               } else {
                  this.failed = true;
               }
            }
         }
      }

      public void stop() {
         this.worker.getNavigation().stop();
         this.target = null;
         this.approach = null;
         this.failed = false;
         this.repathCooldown = 0;
         this.stuckTicks = 0;
         this.lastDistanceSq = Double.MAX_VALUE;
         this.worker.workTicksAtSite = 0;
         if (this.worker.getCurrentCommand() == NpcCommandType.NONE) {
            this.worker.setWorkState("idle");
         }
      }

      private boolean canAutomaticallyWork() {
         return !this.worker.level().isClientSide
            && this.worker.getCurrentCommand() == NpcCommandType.NONE
            && !this.worker.hasAssignedWorkOrder()
            && this.worker.getWorkType() != NpcWorkType.NONE
            && !this.worker.shouldRestAtHome()
            && !this.worker.isGuiInteractionPaused()
            && !this.worker.isGettingFood();
      }

      private BlockPos findNearestAvailableWorksite() {
         if (!(this.worker.level() instanceof ServerLevel serverLevel)) {
            return null;
         } else {
            BlockPos origin = this.worker.blockPosition();
            int minChunkX = Math.floorDiv(origin.getX() - 24, 16);
            int maxChunkX = Math.floorDiv(origin.getX() + 24, 16);
            int minChunkZ = Math.floorDiv(origin.getZ() - 24, 16);
            int maxChunkZ = Math.floorDiv(origin.getZ() + 24, 16);
            double bestDistanceSq = Double.MAX_VALUE;
            BlockPos best = null;

            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
               for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                  LevelChunk chunk = serverLevel.getChunkSource().getChunkNow(chunkX, chunkZ);
                  if (chunk != null) {
                     for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                        BlockPos pos = blockEntity.getBlockPos();
                        if (Math.abs(pos.getY() - origin.getY()) <= 8 && this.rejectedUntil.getOrDefault(pos, 0) <= this.worker.tickCount) {
                           double distanceSq = origin.distSqr(pos);
                           if (!(distanceSq > 576.0) && !(distanceSq >= bestDistanceSq)) {
                              WorkSite site = WorkSites.get(serverLevel, pos).orElse(null);
                              if (site != null && this.worker.canWorkAt(site.getWorkType()) && site.hasWork()) {
                                 best = pos.immutable();
                                 bestDistanceSq = distanceSq;
                              }
                           }
                        }
                     }
                  }
               }
            }

            return best;
         }
      }

      private void rejectCurrentTarget() {
         if (this.target != null) {
            this.rejectedUntil.put(this.target.immutable(), this.worker.tickCount + 200);
         }

         this.failed = true;
         this.nextSearchTick = this.worker.tickCount + 1;
         this.worker.setWorkState("no_work_site");
         this.worker.getNavigation().stop();
      }

      private void pruneRejectedTargets() {
         this.rejectedUntil.entrySet().removeIf(entry -> entry.getValue() <= this.worker.tickCount);
      }

      private double distanceToTargetSq() {
         return this.worker.distanceToSqr((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5);
      }
   }

   private static class SleepGoal extends Goal {
      private static final int BED_SEARCH_INTERVAL = 100;
      private final WorkerNpcEntity worker;
      private final double speed;
      private BlockPos restCenter;
      private BlockPos targetBed;
      private boolean reachedCenter;
      private int bedSearchCooldown;

      private SleepGoal(WorkerNpcEntity worker, double speed) {
         this.worker = worker;
         this.speed = speed;
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (this.worker.shouldRestAtHome() && !this.worker.isSleeping()) {
            this.restCenter = this.worker.hasHome() ? this.worker.getHomePos() : this.worker.blockPosition();
            this.reachedCenter = this.worker
                  .distanceToSqr((double)this.restCenter.getX() + 0.5, (double)this.restCenter.getY() + 0.5, (double)this.restCenter.getZ() + 0.5)
               <= 9.0;
            this.targetBed = null;
            this.bedSearchCooldown = 0;
            return true;
         } else {
            return false;
         }
      }

      public boolean canContinueToUse() {
         return this.worker.shouldRestAtHome();
      }

      public void start() {
         this.worker.setWorkState(this.worker.shouldShelterAtHome() ? "sheltering" : "returning_home");
      }

      public void tick() {
         if (!this.worker.isSleeping()) {
            if (!this.reachedCenter) {
               this.worker
                  .getLookControl()
                  .setLookAt((double)this.restCenter.getX() + 0.5, (double)this.restCenter.getY() + 0.5, (double)this.restCenter.getZ() + 0.5);
               this.worker
                  .getNavigation()
                  .moveTo(
                     (double)this.restCenter.getX() + 0.5,
                     (double)this.restCenter.getY() + 0.5,
                     (double)this.restCenter.getZ() + 0.5,
                     this.speed
                  );
               this.reachedCenter = this.worker
                     .distanceToSqr((double)this.restCenter.getX() + 0.5, (double)this.restCenter.getY() + 0.5, (double)this.restCenter.getZ() + 0.5)
                  <= 9.0;
            } else {
               this.worker.getNavigation().stop();
               if (!this.worker.shouldSleepNow()) {
                  this.worker.setWorkState("sheltering");
                  this.targetBed = null;
                  this.worker.sleepBed = null;
               } else {
                  if (this.targetBed == null) {
                     if (this.bedSearchCooldown-- > 0) {
                        return;
                     }

                     this.targetBed = this.worker.findAvailableBed(this.restCenter);
                     this.worker.sleepBed = this.targetBed;
                     if (this.targetBed == null) {
                        this.worker.setWorkState("sleeping_no_bed");
                        this.bedSearchCooldown = 100;
                        return;
                     }
                  }

                  double distance = this.worker
                     .distanceToSqr((double)this.targetBed.getX() + 0.5, (double)this.targetBed.getY() + 0.5, (double)this.targetBed.getZ() + 0.5);
                  if (distance > 4.0) {
                     this.worker
                        .getLookControl()
                        .setLookAt((double)this.targetBed.getX() + 0.5, (double)this.targetBed.getY() + 0.5, (double)this.targetBed.getZ() + 0.5);
                     this.worker
                        .getNavigation()
                        .moveTo(
                           (double)this.targetBed.getX() + 0.5,
                           (double)this.targetBed.getY() + 0.5,
                           (double)this.targetBed.getZ() + 0.5,
                           this.speed
                        );
                  } else {
                     if (this.worker.level().getBlockState(this.targetBed).getBlock() instanceof BedBlock) {
                        this.worker.startSleeping(this.targetBed);
                        this.worker.setWorkState("sleeping");
                     } else {
                        this.targetBed = null;
                        this.worker.sleepBed = null;
                     }
                  }
               }
            }
         }
      }

      public void stop() {
         if (this.worker.isSleeping()) {
            this.worker.stopSleeping();
         }

         this.worker.sleepBed = null;
         this.worker.getNavigation().stop();
         if (this.worker.getCurrentCommand() == NpcCommandType.NONE) {
            this.worker.setWorkState("idle");
         }

         this.targetBed = null;
         this.restCenter = null;
         this.bedSearchCooldown = 0;
      }
   }

   private static class WorkOrderGoal extends Goal {
      private static final int DEFAULT_WORK_TICKS = 10;
      private static final int INVALID_SITE_WAIT_TICKS = 40;
      private static final int REPATH_TICKS = 20;
      private static final int STUCK_TICKS = 60;
      private static final int MAX_FAILURES = 3;
      private final WorkerNpcEntity worker;
      private final double speedModifier;
      private BlockPos target;
      private BlockPos approach;
      private BlockPos approachFor;
      private int repathCooldown;
      private int stuckTicks;
      private int failures;
      private double lastDistanceSq = Double.MAX_VALUE;

      private WorkOrderGoal(WorkerNpcEntity worker, double speedModifier, double stopDistance) {
         this.worker = worker;
         this.speedModifier = speedModifier;
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (this.worker.getCurrentCommand() != NpcCommandType.NONE || !AWCoreRegistryAccess.is(this.worker.getOrdersStack(), AWCoreIds.WORK_ORDER)) {
            return false;
         } else if (!WorkOrderData.matchesDimension(this.worker.getOrdersStack(), this.worker.level())) {
            this.worker.setWorkState("no_work");
            return false;
         } else if (!this.shouldWorkThisShift()) {
            this.worker.setWorkState("idle");
            return false;
         } else {
            return this.selectTarget();
         }
      }

      public boolean canContinueToUse() {
         if (this.worker.getCurrentCommand() == NpcCommandType.NONE
            && AWCoreRegistryAccess.is(this.worker.getOrdersStack(), AWCoreIds.WORK_ORDER)
            && WorkOrderData.matchesDimension(this.worker.getOrdersStack(), this.worker.level())
            && this.shouldWorkThisShift()
            && this.selectTarget()) {
            WorkSite site = this.target == null ? null : WorkSites.get(this.worker.level(), this.target).orElse(null);
            return site != null && this.worker.canWorkAt(site.getWorkType()) && site.hasWork();
         } else {
            return false;
         }
      }

      public void tick() {
         if (this.target != null) {
            this.worker.getLookControl().setLookAt((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5);
            if (this.distanceToTargetSq() > this.worker.getWorkRangeSq()
                  && !(this.target.equals(this.approachFor) && WorksiteApproach.isAt(this.worker, this.approach))
               || WorksiteApproach.inDoorway(this.worker)) {
               this.walkToSite();
            } else {
               this.worker.getNavigation().stop();
               WorkSite site = WorkSites.get(this.worker.level(), this.target).orElse(null);
               if (site == null || !this.worker.canWorkAt(site.getWorkType())) {
                  this.worker.setWorkState("no_work_site");
                  this.worker.workTicksAtSite++;
                  if (this.worker.workTicksAtSite >= 40) {
                     this.worker.workTicksAtSite = 0;
                     this.advanceToNextSite();
                  }
               } else if (!site.hasWork()) {
                  this.worker.setWorkState("no_work");
                  this.worker.workTicksAtSite++;
               } else {
                  this.worker.setWorkState("working");
                  this.worker.workTicksAtSite++;
                  site.onWorkerTick(this.worker);
                  if (this.worker.tickCount % 10 == 0) {
                     this.worker.swing(InteractionHand.MAIN_HAND);
                  }

                  if (this.worker.workTicksAtSite >= this.getTicksForCurrentSite()) {
                     site.addEnergyFromWorker(this.worker);
                     this.worker.workTicksAtSite = 0;
                     this.advanceToNextSite();
                  }
               }
            }
         }
      }

      /**
       * Walks to a free spot next to the site, refreshing the path now and then. A site with no reachable spot, or one
       * the worker stops getting closer to, is skipped for the next one in the order instead of blocking it forever.
       */
      private void walkToSite() {
         if (!this.target.equals(this.approachFor)) {
            this.approachFor = this.target;
            this.approach = null;
            this.failures = 0;
            this.repathCooldown = 0;
         }

         if (this.approach == null) {
            // Look for a spot now and then; the pathfinder cannot plan while the worker is still falling or jumping.
            if (this.repathCooldown-- <= 0 && this.worker.onGround()) {
               this.approach = WorksiteApproach.find(this.worker, this.target, this.worker.getWorkRangeSq());
               this.stuckTicks = 0;
               this.lastDistanceSq = this.distanceToTargetSq();
               if (this.approach == null && ++this.failures >= MAX_FAILURES) {
                  this.skipSite();
                  return;
               }

               this.repathCooldown = this.approach == null ? REPATH_TICKS : 0;
            }

            if (this.approach == null) {
               this.worker.setWorkState("no_work_site");
               return;
            }
         }

         this.worker.workTicksAtSite = 0;
         this.worker.setWorkState("moving_to_work");
         double distanceSq = this.distanceToTargetSq();
         if (distanceSq < this.lastDistanceSq - 0.05) {
            this.stuckTicks = 0;
            this.lastDistanceSq = distanceSq;
         } else if (++this.stuckTicks >= STUCK_TICKS) {
            this.skipSite();
            return;
         }

         if (this.repathCooldown-- <= 0) {
            this.repathCooldown = REPATH_TICKS;
            boolean pathFound = this.worker
               .getNavigation()
               .moveTo((double)this.approach.getX() + 0.5, (double)this.approach.getY(), (double)this.approach.getZ() + 0.5, this.worker.playerSprintNavigationSpeed());
            if (pathFound) {
               this.failures = 0;
            } else if (++this.failures >= MAX_FAILURES) {
               this.skipSite();
            }
         }
      }

      private void skipSite() {
         this.worker.setWorkState("no_work_site");
         this.worker.workTicksAtSite = 0;
         this.worker.getNavigation().stop();
         this.approachFor = null;
         this.advanceToNextSite();
      }

      public void stop() {
         this.target = null;
         this.approachFor = null;
         this.approach = null;
         this.worker.getNavigation().stop();
         if (this.worker.getCurrentCommand() == NpcCommandType.NONE) {
            this.worker.setWorkState("idle");
         }
      }

      private boolean selectTarget() {
         List<WorkOrderData.WorkEntry> entries = WorkOrderData.getWorkEntries(this.worker.getOrdersStack());
         if (entries.isEmpty()) {
            this.worker.resetWorkOrderProgress();
            return false;
         } else {
            String currentDimension = this.worker.level().dimension().location().toString();
            if (entries.stream().anyMatch(entry -> !entry.dimension().isEmpty() && !entry.dimension().equals(currentDimension))) {
               this.worker.setWorkState("no_work");
               return false;
            } else {
               if (this.worker.workOrderIndex < 0 || this.worker.workOrderIndex >= entries.size()) {
                  this.worker.workOrderIndex = 0;
                  this.worker.workTicksAtSite = 0;
               }

               this.target = entries.get(this.worker.workOrderIndex).pos();
               return this.target != null;
            }
         }
      }

      private int getTicksForCurrentSite() {
         List<WorkOrderData.WorkEntry> entries = WorkOrderData.getWorkEntries(this.worker.getOrdersStack());
         if (this.worker.workOrderIndex >= 0 && this.worker.workOrderIndex < entries.size()) {
            int configuredTicks = entries.get(this.worker.workOrderIndex).workLength();
            return configuredTicks > 0 ? configuredTicks : 10;
         } else {
            return 10;
         }
      }

      private void advanceToNextSite() {
         int size = WorkOrderData.getWorkEntries(this.worker.getOrdersStack()).size();
         if (size <= 0) {
            this.worker.resetWorkOrderProgress();
         } else {
            this.worker.workOrderIndex = (this.worker.workOrderIndex + 1) % size;
            this.selectTarget();
         }
      }

      private boolean shouldWorkThisShift() {
         boolean nightShift = WorkOrderData.isNightShift(this.worker.getOrdersStack());
         return nightShift == this.worker.level().isNight();
      }

      private double distanceToTargetSq() {
         return this.worker.distanceToSqr((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5);
      }
   }
}
