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
   private static final EntityDataAccessor<String> WORK_STATE = SynchedEntityData.m_135353_(WorkerNpcEntity.class, EntityDataSerializers.f_135030_);
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
      return m_21552_().m_22268_(Attributes.f_22276_, 20.0).m_22268_(Attributes.f_22279_, 0.25).m_22268_(Attributes.f_22277_, 24.0);
   }

   protected void m_8099_() {
      this.f_21345_.m_25352_(0, new FloatGoal(this));
      this.f_21345_.m_25352_(1, new WorkerNpcEntity.SleepGoal(this, this.playerSprintNavigationSpeed()));
      this.f_21345_.m_25352_(2, new WorkerNpcEntity.MoveToCommandGoal(this, 1.0, 2.0));
      this.f_21345_.m_25352_(4, new WorkerNpcEntity.WorkOrderGoal(this, 1.0, 2.0));
      this.f_21345_.m_25352_(5, new WorkerNpcEntity.NearbyWorkGoal(this, 1.0));
      this.f_21345_.m_25352_(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.f_21345_.m_25352_(7, new RandomLookAroundGoal(this));
   }

   @Override
   protected void m_8097_() {
      super.m_8097_();
      this.f_19804_.m_135372_(WORK_STATE, "idle");
   }

   @Override
   public void m_8119_() {
      if (!this.m_9236_().f_46443_) {
         this.migrateLegacyOrderState();
         if (this.m_21525_() && !this.isGuiInteractionPaused()) {
            this.m_21557_(false);
         }

         if (this.m_5803_() && !this.shouldSleepNow()) {
            this.m_5796_();
            this.sleepBed = null;
         }
      }

      super.m_8119_();
   }

   private void migrateLegacyOrderState() {
      if (!AWCoreRegistryAccess.is(this.getOrdersStack(), AWCoreIds.WORK_ORDER)) {
         if (AWCoreRegistryAccess.is(this.getUpkeepStack(), AWCoreIds.WORK_ORDER)) {
            ItemStack legacyOrder = this.getUpkeepStack().m_41777_();
            super.setOrdersStack(legacyOrder);
            super.setUpkeepStack(ItemStack.f_41583_);
            this.resetWorkOrderProgress();
            if (this.getCurrentCommand() != NpcCommandType.NONE) {
               this.clearCommand();
               this.setFollowTarget(null);
            }
         }
      }
   }

   @Override
   public void m_7380_(CompoundTag tag) {
      super.m_7380_(tag);
      tag.m_128359_("WorkState", this.getWorkState());
      tag.m_128405_("WorkOrderIndex", this.workOrderIndex);
      tag.m_128405_("WorkTicksAtSite", this.workTicksAtSite);
   }

   @Override
   public void m_7378_(CompoundTag tag) {
      super.m_7378_(tag);
      if (tag.m_128471_("NoAI")) {
         this.m_21557_(false);
      }

      this.setWorkState(tag.m_128461_("WorkState"));
      this.workOrderIndex = tag.m_128451_("WorkOrderIndex");
      this.workTicksAtSite = tag.m_128451_("WorkTicksAtSite");
   }

   protected InteractionResult m_6071_(Player player, InteractionHand hand) {
      if (this.tryAcceptFood(player, hand)) {
         return InteractionResult.m_19078_(this.m_9236_().f_46443_);
      } else if (player.m_21120_(hand).m_41720_() instanceof CommandBatonItem) {
         if (!this.m_9236_().f_46443_ && this.hasCommandPermission(player)) {
            CommandBatonItem.toggleNpcSelection(player.m_21120_(hand), player, this);
         }

         return InteractionResult.m_19078_(this.m_9236_().f_46443_);
      } else {
         if (!this.m_9236_().f_46443_ && this.hasCommandPermission(player)) {
            AWCorePlatform.openMenu(
               player,
               new SimpleMenuProvider((containerId, inventory, openedPlayer) -> new NpcInventoryMenu(containerId, inventory, this), this.m_5446_()),
               buffer -> buffer.writeInt(this.m_19879_())
            );
         }

         return InteractionResult.m_19078_(this.m_9236_().f_46443_);
      }
   }

   public String getWorkState() {
      return (String)this.f_19804_.m_135370_(WORK_STATE);
   }

   public boolean isWorkingAnimation() {
      String state = this.getWorkState();
      return "working".equals(state);
   }

   public void setWorkState(String workState) {
      this.f_19804_.m_135381_(WORK_STATE, workState != null && !workState.isBlank() ? workState : "idle");
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
         super.setUpkeepStack(ItemStack.f_41583_);
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
      if (!this.m_9236_().f_46443_ && this.getCurrentCommand() == NpcCommandType.NONE && !this.isGuiInteractionPaused() && !this.isGettingFood()) {
         boolean nightShift = AWCoreRegistryAccess.is(this.getOrdersStack(), AWCoreIds.WORK_ORDER) && WorkOrderData.isNightShift(this.getOrdersStack());
         return nightShift ? !this.m_9236_().m_46462_() : this.m_9236_().m_46462_();
      } else {
         return false;
      }
   }

   private boolean shouldShelterAtHome() {
      return !this.m_9236_().f_46443_
         && this.hasHome()
         && this.m_9236_().m_46471_()
         && this.getCurrentCommand() == NpcCommandType.NONE
         && !this.isGuiInteractionPaused()
         && !this.isGettingFood();
   }

   boolean shouldRestAtHome() {
      return this.shouldSleepNow() || this.shouldShelterAtHome();
   }

   private boolean bedReservedByAnother(BlockPos bed) {
      AABB search = new AABB(bed).m_82377_(24.0, 8.0, 24.0);

      for (WorkerNpcEntity other : this.m_9236_().m_45976_(WorkerNpcEntity.class, search)) {
         if (other != this && bed.equals(other.sleepBed)) {
            return true;
         }

         if (other != this && other.m_5803_() && other.m_21257_().isPresent() && bed.equals(other.m_21257_().get())) {
            return true;
         }
      }

      return false;
   }

   private BlockPos findAvailableBed(BlockPos center) {
      MutableBlockPos cursor = new MutableBlockPos();

      for (BlockPos pos : BlockPos.m_121940_(center.m_7918_(-16, -4, -16), center.m_7918_(16, 4, 16))) {
         cursor.m_122190_(pos);
         BlockState state = this.m_9236_().m_8055_(cursor);
         if (state.m_60734_() instanceof BedBlock && !this.bedReservedByAnother(cursor.m_7949_())) {
            return cursor.m_7949_();
         }
      }

      return null;
   }

   public NpcWorkType getWorkType() {
      return WorkerProfessionResolver.workTypeFor(this.m_21205_());
   }

   @Override
   public String getNpcSubtype() {
      return WorkerProfessionResolver.subtypeFor(this.m_21205_());
   }

   @Override
   public Component m_5677_() {
      return Component.m_237115_("entity.ancientwarfare.worker." + this.getNpcSubtype());
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
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         if (this.worker.getCurrentCommand() != NpcCommandType.MOVE) {
            return false;
         } else {
            this.target = this.worker.getCommandTarget();
            return !BlockPos.f_121853_.equals(this.target) && this.distanceToTargetSq() > this.stopDistanceSq;
         }
      }

      public boolean m_8045_() {
         return this.worker.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() > this.stopDistanceSq;
      }

      public void m_8037_() {
         this.worker.m_21563_().m_24946_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
         this.worker
            .m_21573_()
            .m_26519_(
               (double)this.target.m_123341_() + 0.5,
               (double)this.target.m_123342_(),
               (double)this.target.m_123343_() + 0.5,
               this.worker.playerSprintNavigationSpeed()
            );
      }

      public void m_8041_() {
         if (this.worker.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() <= this.stopDistanceSq) {
            this.worker.clearCommand();
            this.worker.setWorkState("idle");
         }

         this.target = null;
         this.worker.m_21573_().m_26573_();
      }

      private double distanceToTargetSq() {
         return this.worker.m_20275_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
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
      private int nextSearchTick;
      private int repathCooldown;
      private int stuckTicks;
      private double lastDistanceSq = Double.MAX_VALUE;
      private boolean failed;

      private NearbyWorkGoal(WorkerNpcEntity worker, double speedModifier) {
         this.worker = worker;
         this.speedModifier = speedModifier;
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         if (this.canAutomaticallyWork() && this.worker.f_19797_ >= this.nextSearchTick) {
            this.pruneRejectedTargets();
            this.target = this.findNearestAvailableWorksite();
            if (this.target == null) {
               this.worker.setWorkState("no_work");
               this.nextSearchTick = this.worker.f_19797_ + 40;
               return false;
            } else {
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

      public boolean m_8045_() {
         if (this.canAutomaticallyWork() && !this.failed && this.target != null) {
            WorkSite site = WorkSites.get(this.worker.m_9236_(), this.target).orElse(null);
            return site != null && this.worker.canWorkAt(site.getWorkType()) && site.hasWork();
         } else {
            return false;
         }
      }

      public void m_8037_() {
         if (this.target != null) {
            this.worker.m_21563_().m_24946_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
            double distanceSq = this.distanceToTargetSq();
            if (distanceSq > this.worker.getWorkRangeSq()) {
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
                     .m_21573_()
                     .m_26519_(
                        (double)this.target.m_123341_() + 0.5,
                        (double)this.target.m_123342_(),
                        (double)this.target.m_123343_() + 0.5,
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
               this.worker.m_21573_().m_26573_();
               WorkSite site = WorkSites.get(this.worker.m_9236_(), this.target).orElse(null);
               if (site != null && this.worker.canWorkAt(site.getWorkType()) && site.hasWork()) {
                  this.worker.setWorkState("working");
                  this.worker.workTicksAtSite++;
                  site.onWorkerTick(this.worker);
                  if (this.worker.f_19797_ % 10 == 0) {
                     this.worker.m_6674_(InteractionHand.MAIN_HAND);
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

      public void m_8041_() {
         this.worker.m_21573_().m_26573_();
         this.target = null;
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
         return !this.worker.m_9236_().f_46443_
            && this.worker.getCurrentCommand() == NpcCommandType.NONE
            && !this.worker.hasAssignedWorkOrder()
            && this.worker.getWorkType() != NpcWorkType.NONE
            && !this.worker.shouldRestAtHome()
            && !this.worker.isGuiInteractionPaused()
            && !this.worker.isGettingFood();
      }

      private BlockPos findNearestAvailableWorksite() {
         if (!(this.worker.m_9236_() instanceof ServerLevel serverLevel)) {
            return null;
         } else {
            BlockPos origin = this.worker.m_20183_();
            int minChunkX = Math.floorDiv(origin.m_123341_() - 24, 16);
            int maxChunkX = Math.floorDiv(origin.m_123341_() + 24, 16);
            int minChunkZ = Math.floorDiv(origin.m_123343_() - 24, 16);
            int maxChunkZ = Math.floorDiv(origin.m_123343_() + 24, 16);
            double bestDistanceSq = Double.MAX_VALUE;
            BlockPos best = null;

            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
               for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                  LevelChunk chunk = serverLevel.m_7726_().m_7131_(chunkX, chunkZ);
                  if (chunk != null) {
                     for (BlockEntity blockEntity : chunk.m_62954_().values()) {
                        BlockPos pos = blockEntity.m_58899_();
                        if (Math.abs(pos.m_123342_() - origin.m_123342_()) <= 8 && this.rejectedUntil.getOrDefault(pos, 0) <= this.worker.f_19797_) {
                           double distanceSq = origin.m_123331_(pos);
                           if (!(distanceSq > 576.0) && !(distanceSq >= bestDistanceSq)) {
                              WorkSite site = WorkSites.get(serverLevel, pos).orElse(null);
                              if (site != null && this.worker.canWorkAt(site.getWorkType()) && site.hasWork()) {
                                 best = pos.m_7949_();
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
            this.rejectedUntil.put(this.target.m_7949_(), this.worker.f_19797_ + 200);
         }

         this.failed = true;
         this.nextSearchTick = this.worker.f_19797_ + 1;
         this.worker.setWorkState("no_work_site");
         this.worker.m_21573_().m_26573_();
      }

      private void pruneRejectedTargets() {
         this.rejectedUntil.entrySet().removeIf(entry -> entry.getValue() <= this.worker.f_19797_);
      }

      private double distanceToTargetSq() {
         return this.worker.m_20275_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
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
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         if (this.worker.shouldRestAtHome() && !this.worker.m_5803_()) {
            this.restCenter = this.worker.hasHome() ? this.worker.getHomePos() : this.worker.m_20183_();
            this.reachedCenter = this.worker
                  .m_20275_((double)this.restCenter.m_123341_() + 0.5, (double)this.restCenter.m_123342_() + 0.5, (double)this.restCenter.m_123343_() + 0.5)
               <= 9.0;
            this.targetBed = null;
            this.bedSearchCooldown = 0;
            return true;
         } else {
            return false;
         }
      }

      public boolean m_8045_() {
         return this.worker.shouldRestAtHome();
      }

      public void m_8056_() {
         this.worker.setWorkState(this.worker.shouldShelterAtHome() ? "sheltering" : "returning_home");
      }

      public void m_8037_() {
         if (!this.worker.m_5803_()) {
            if (!this.reachedCenter) {
               this.worker
                  .m_21563_()
                  .m_24946_((double)this.restCenter.m_123341_() + 0.5, (double)this.restCenter.m_123342_() + 0.5, (double)this.restCenter.m_123343_() + 0.5);
               this.worker
                  .m_21573_()
                  .m_26519_(
                     (double)this.restCenter.m_123341_() + 0.5,
                     (double)this.restCenter.m_123342_() + 0.5,
                     (double)this.restCenter.m_123343_() + 0.5,
                     this.speed
                  );
               this.reachedCenter = this.worker
                     .m_20275_((double)this.restCenter.m_123341_() + 0.5, (double)this.restCenter.m_123342_() + 0.5, (double)this.restCenter.m_123343_() + 0.5)
                  <= 9.0;
            } else {
               this.worker.m_21573_().m_26573_();
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
                     .m_20275_((double)this.targetBed.m_123341_() + 0.5, (double)this.targetBed.m_123342_() + 0.5, (double)this.targetBed.m_123343_() + 0.5);
                  if (distance > 4.0) {
                     this.worker
                        .m_21563_()
                        .m_24946_((double)this.targetBed.m_123341_() + 0.5, (double)this.targetBed.m_123342_() + 0.5, (double)this.targetBed.m_123343_() + 0.5);
                     this.worker
                        .m_21573_()
                        .m_26519_(
                           (double)this.targetBed.m_123341_() + 0.5,
                           (double)this.targetBed.m_123342_() + 0.5,
                           (double)this.targetBed.m_123343_() + 0.5,
                           this.speed
                        );
                  } else {
                     if (this.worker.m_9236_().m_8055_(this.targetBed).m_60734_() instanceof BedBlock) {
                        this.worker.m_5802_(this.targetBed);
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

      public void m_8041_() {
         if (this.worker.m_5803_()) {
            this.worker.m_5796_();
         }

         this.worker.sleepBed = null;
         this.worker.m_21573_().m_26573_();
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
      private final WorkerNpcEntity worker;
      private final double speedModifier;
      private BlockPos target;

      private WorkOrderGoal(WorkerNpcEntity worker, double speedModifier, double stopDistance) {
         this.worker = worker;
         this.speedModifier = speedModifier;
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         if (this.worker.getCurrentCommand() != NpcCommandType.NONE || !AWCoreRegistryAccess.is(this.worker.getOrdersStack(), AWCoreIds.WORK_ORDER)) {
            return false;
         } else if (!WorkOrderData.matchesDimension(this.worker.getOrdersStack(), this.worker.m_9236_())) {
            this.worker.setWorkState("no_work");
            return false;
         } else if (!this.shouldWorkThisShift()) {
            this.worker.setWorkState("idle");
            return false;
         } else {
            return this.selectTarget();
         }
      }

      public boolean m_8045_() {
         if (this.worker.getCurrentCommand() == NpcCommandType.NONE
            && AWCoreRegistryAccess.is(this.worker.getOrdersStack(), AWCoreIds.WORK_ORDER)
            && WorkOrderData.matchesDimension(this.worker.getOrdersStack(), this.worker.m_9236_())
            && this.shouldWorkThisShift()
            && this.selectTarget()) {
            WorkSite site = this.target == null ? null : WorkSites.get(this.worker.m_9236_(), this.target).orElse(null);
            return site != null && this.worker.canWorkAt(site.getWorkType()) && site.hasWork();
         } else {
            return false;
         }
      }

      public void m_8037_() {
         if (this.target != null) {
            this.worker.m_21563_().m_24946_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
            if (this.distanceToTargetSq() > this.worker.getWorkRangeSq()) {
               this.worker.workTicksAtSite = 0;
               this.worker.setWorkState("moving_to_work");
               this.worker
                  .m_21573_()
                  .m_26519_(
                     (double)this.target.m_123341_() + 0.5,
                     (double)this.target.m_123342_(),
                     (double)this.target.m_123343_() + 0.5,
                     this.worker.playerSprintNavigationSpeed()
                  );
            } else {
               this.worker.m_21573_().m_26573_();
               WorkSite site = WorkSites.get(this.worker.m_9236_(), this.target).orElse(null);
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
                  if (this.worker.f_19797_ % 10 == 0) {
                     this.worker.m_6674_(InteractionHand.MAIN_HAND);
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

      public void m_8041_() {
         this.target = null;
         this.worker.m_21573_().m_26573_();
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
            String currentDimension = this.worker.m_9236_().m_46472_().m_135782_().toString();
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
         return nightShift == this.worker.m_9236_().m_46462_();
      }

      private double distanceToTargetSq() {
         return this.worker.m_20275_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
      }
   }
}
