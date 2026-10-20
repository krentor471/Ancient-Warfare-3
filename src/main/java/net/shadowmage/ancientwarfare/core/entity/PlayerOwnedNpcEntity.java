package net.shadowmage.ancientwarfare.core.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.common.inventory.ItemHandlerAccess;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.npc.NpcOwnerAccess;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargeting;
import net.shadowmage.ancientwarfare.core.common.npc.UpkeepOrderData;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireHippocampusCompat;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireHippogryphCompat;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;
import net.shadowmage.ancientwarfare.core.tile.TownHallBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.TownHallDeathData;

public abstract class PlayerOwnedNpcEntity extends NpcBaseEntity implements NpcOwnerAccess {
   private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.m_135353_(PlayerOwnedNpcEntity.class, EntityDataSerializers.f_135041_);
   private static final EntityDataAccessor<String> CURRENT_COMMAND = SynchedEntityData.m_135353_(PlayerOwnedNpcEntity.class, EntityDataSerializers.f_135030_);
   private static final EntityDataAccessor<BlockPos> COMMAND_TARGET = SynchedEntityData.m_135353_(PlayerOwnedNpcEntity.class, EntityDataSerializers.f_135038_);
   private static final EntityDataAccessor<Boolean> HOME_SET = SynchedEntityData.m_135353_(PlayerOwnedNpcEntity.class, EntityDataSerializers.f_135035_);
   private static final EntityDataAccessor<BlockPos> HOME_POS = SynchedEntityData.m_135353_(PlayerOwnedNpcEntity.class, EntityDataSerializers.f_135038_);
   private static final EntityDataAccessor<Optional<UUID>> FOLLOW_TARGET = SynchedEntityData.m_135353_(
      PlayerOwnedNpcEntity.class, EntityDataSerializers.f_135041_
   );
   private static final EntityDataAccessor<ItemStack> ORDERS_STACK = SynchedEntityData.m_135353_(PlayerOwnedNpcEntity.class, EntityDataSerializers.f_135033_);
   private static final EntityDataAccessor<ItemStack> UPKEEP_STACK = SynchedEntityData.m_135353_(PlayerOwnedNpcEntity.class, EntityDataSerializers.f_135033_);
   private static final EntityDataAccessor<ItemStack> FOOD_STACK = SynchedEntityData.m_135353_(PlayerOwnedNpcEntity.class, EntityDataSerializers.f_135033_);
   private static final EntityDataAccessor<Integer> FOOD_REMAINING = SynchedEntityData.m_135353_(PlayerOwnedNpcEntity.class, EntityDataSerializers.f_135028_);
   private static final EntityDataAccessor<Boolean> GETTING_FOOD = SynchedEntityData.m_135353_(PlayerOwnedNpcEntity.class, EntityDataSerializers.f_135035_);
   private static final EntityDataAccessor<Boolean> TOWN_HALL_SET = SynchedEntityData.m_135353_(PlayerOwnedNpcEntity.class, EntityDataSerializers.f_135035_);
   private static final EntityDataAccessor<BlockPos> TOWN_HALL_POS = SynchedEntityData.m_135353_(PlayerOwnedNpcEntity.class, EntityDataSerializers.f_135038_);
   private static final EntityDataAccessor<Integer> DRAGON_RIDER_MOUNT = SynchedEntityData.m_135353_(
      PlayerOwnedNpcEntity.class, EntityDataSerializers.f_135028_
   );
   private static final String TAG_OWNER = "Owner";
   private static final String TAG_CURRENT_COMMAND = "CurrentCommand";
   private static final String TAG_COMMAND_TARGET = "CommandTarget";
   private static final String TAG_HOME_SET = "HomeSet";
   private static final String TAG_HOME_POS = "HomePos";
   private static final String TAG_FOLLOW_TARGET = "FollowTarget";
   private static final String TAG_ORDERS_STACK = "OrdersStack";
   private static final String TAG_UPKEEP_STACK = "UpkeepStack";
   private static final String TAG_FOOD_STACK = "FoodStack";
   private static final String TAG_TOWN_HALL_SET = "TownHallSet";
   private static final String TAG_TOWN_HALL_POS = "TownHallPos";
   private static final String TAG_DRAGON_RIDER_MOUNT = "DragonRiderMount";
   private static final String TAG_FOOD_REMAINING = "FoodRemaining";
   private static final String TAG_ATTACK_COMMAND_TARGET = "AttackCommandTarget";
   private static final int FOOD_ACTION_RANGE = 3;
   private static final int FOOD_SUPPLY_RETRY_TICKS = 200;
   private static final double FOLLOW_COMBAT_RECALL_DISTANCE = 24.0;
   private static final double FOLLOW_COMBAT_RESUME_DISTANCE = 10.0;
   private final Map<UUID, Integer> guiViewers = new HashMap<>();
   private UUID combatGroupCommanderId;
   private Vec3 combatGroupOffset = Vec3.f_82478_;
   private long combatGroupRefreshTime = Long.MIN_VALUE;
   private UUID dragonRiderMountUuid;
   private UUID attackCommandTargetUuid;
   private int foodSupplyRetryTicks;
   private boolean followCombatRecallActive;

   protected PlayerOwnedNpcEntity(EntityType<? extends PlayerOwnedNpcEntity> entityType, Level level) {
      super(entityType, level);
      this.f_21345_.m_25352_(3, new FoodSupplyGoal(this));
      this.f_21345_.m_25352_(3, new FollowPlayerGoal(this, 3.0F, 6.0F));
   }

   @Override
   protected void m_8097_() {
      super.m_8097_();
      this.f_19804_.m_135372_(OWNER, Optional.empty());
      this.f_19804_.m_135372_(CURRENT_COMMAND, NpcCommandType.NONE.name());
      this.f_19804_.m_135372_(COMMAND_TARGET, BlockPos.f_121853_);
      this.f_19804_.m_135372_(HOME_SET, false);
      this.f_19804_.m_135372_(HOME_POS, BlockPos.f_121853_);
      this.f_19804_.m_135372_(FOLLOW_TARGET, Optional.empty());
      this.f_19804_.m_135372_(ORDERS_STACK, ItemStack.f_41583_);
      this.f_19804_.m_135372_(UPKEEP_STACK, ItemStack.f_41583_);
      this.f_19804_.m_135372_(FOOD_STACK, ItemStack.f_41583_);
      this.f_19804_.m_135372_(FOOD_REMAINING, 0);
      this.f_19804_.m_135372_(GETTING_FOOD, false);
      this.f_19804_.m_135372_(TOWN_HALL_SET, false);
      this.f_19804_.m_135372_(TOWN_HALL_POS, BlockPos.f_121853_);
      this.f_19804_.m_135372_(DRAGON_RIDER_MOUNT, -1);
   }

   @Override
   public void m_7380_(CompoundTag tag) {
      super.m_7380_(tag);
      this.getOwner().ifPresent(uuid -> tag.m_128362_("Owner", uuid));
      tag.m_128359_("CurrentCommand", this.getCurrentCommand().name());
      tag.m_128356_("CommandTarget", this.getCommandTarget().m_121878_());
      tag.m_128379_("HomeSet", this.hasHome());
      tag.m_128356_("HomePos", this.getHomePos().m_121878_());
      if (this.attackCommandTargetUuid != null) {
         tag.m_128362_("AttackCommandTarget", this.attackCommandTargetUuid);
      }

      this.getFollowTarget().ifPresent(uuid -> tag.m_128362_("FollowTarget", uuid));
      if (this.dragonRiderMountUuid != null) {
         tag.m_128362_("DragonRiderMount", this.dragonRiderMountUuid);
      }

      if (!this.getOrdersStack().m_41619_()) {
         tag.m_128365_("OrdersStack", this.getOrdersStack().m_41739_(new CompoundTag()));
      }

      if (!this.getUpkeepStack().m_41619_()) {
         tag.m_128365_("UpkeepStack", this.getUpkeepStack().m_41739_(new CompoundTag()));
      }

      if (!this.getFoodStack().m_41619_()) {
         tag.m_128365_("FoodStack", this.getFoodStack().m_41739_(new CompoundTag()));
      }

      tag.m_128405_("FoodRemaining", this.getFoodRemaining());
      tag.m_128379_("TownHallSet", this.hasTownHall());
      tag.m_128356_("TownHallPos", this.getTownHallPos().m_121878_());
   }

   @Override
   public void m_7378_(CompoundTag tag) {
      super.m_7378_(tag);
      if (tag.m_128403_("Owner")) {
         this.setOwner(tag.m_128342_("Owner"));
      }

      this.dragonRiderMountUuid = tag.m_128403_("DragonRiderMount") ? tag.m_128342_("DragonRiderMount") : null;
      this.attackCommandTargetUuid = tag.m_128403_("AttackCommandTarget") ? tag.m_128342_("AttackCommandTarget") : null;
      if (tag.m_128441_("CurrentCommand")) {
         this.setCurrentCommand(parseCommand(tag.m_128461_("CurrentCommand")));
      }

      if (tag.m_128441_("CommandTarget")) {
         this.setCommandTarget(BlockPos.m_122022_(tag.m_128454_("CommandTarget")));
      }

      if (tag.m_128441_("HomeSet")) {
         this.f_19804_.m_135381_(HOME_SET, tag.m_128471_("HomeSet"));
      }

      if (tag.m_128441_("HomePos")) {
         this.f_19804_.m_135381_(HOME_POS, BlockPos.m_122022_(tag.m_128454_("HomePos")));
      }

      if (tag.m_128403_("FollowTarget")) {
         this.setFollowTarget(tag.m_128342_("FollowTarget"));
      }

      if (tag.m_128441_("OrdersStack")) {
         this.setOrdersStack(ItemStack.m_41712_(tag.m_128469_("OrdersStack")));
      }

      if (tag.m_128441_("UpkeepStack")) {
         this.setUpkeepStack(ItemStack.m_41712_(tag.m_128469_("UpkeepStack")));
      }

      if (tag.m_128441_("FoodStack")) {
         this.setFoodStack(ItemStack.m_41712_(tag.m_128469_("FoodStack")));
      }

      if (tag.m_128441_("FoodRemaining")) {
         this.setFoodRemaining(tag.m_128451_("FoodRemaining"));
      }

      if (tag.m_128441_("TownHallSet")) {
         this.f_19804_.m_135381_(TOWN_HALL_SET, tag.m_128471_("TownHallSet"));
      }

      if (tag.m_128441_("TownHallPos")) {
         this.f_19804_.m_135381_(TOWN_HALL_POS, BlockPos.m_122022_(tag.m_128454_("TownHallPos")));
      }
   }

   @Override
   public Optional<UUID> getOwner() {
      return (Optional<UUID>)this.f_19804_.m_135370_(OWNER);
   }

   public void setOwner(UUID owner) {
      this.f_19804_.m_135381_(OWNER, Optional.ofNullable(owner));
   }

   public boolean m_7307_(Entity entity) {
      return NpcTargeting.isProtectedAlly(this, entity) || super.m_7307_(entity);
   }

   public void beginGuiInteraction(Player player) {
      if (!this.m_9236_().f_46443_ && player != null && this.hasCommandPermission(player)) {
         if (this.guiViewers.isEmpty()) {
            this.freezeForGui();
         }

         this.guiViewers.merge(player.m_20148_(), 1, Integer::sum);
      }
   }

   public void endGuiInteraction(Player player) {
      if (!this.m_9236_().f_46443_ && player != null) {
         UUID playerId = player.m_20148_();
         Integer openMenus = this.guiViewers.get(playerId);
         if (openMenus != null) {
            if (openMenus > 1) {
               this.guiViewers.put(playerId, openMenus - 1);
            } else {
               this.guiViewers.remove(playerId);
            }

            if (this.guiViewers.isEmpty()) {
               this.m_21573_().m_26573_();
            }
         }
      }
   }

   public boolean isGuiInteractionPaused() {
      return !this.guiViewers.isEmpty();
   }

   @Override
   public void m_8119_() {
      if (this.m_5448_() != null && !NpcTargeting.canHarm(this, this.m_5448_())) {
         this.m_6710_(null);
         this.m_6703_(null);
      }

      this.updateFollowCombatRecall();
      this.enforceAttackCommandTarget();
      if (this.isGuiInteractionPaused()) {
         this.freezeForGui();
      }

      super.m_8119_();
      if (this.isGuiInteractionPaused()) {
         this.freezeForGui();
      }

      this.updateFoodSupply();
   }

   private void updateFollowCombatRecall() {
      if (!this.m_9236_().f_46443_ && this.getCurrentCommand() == NpcCommandType.FOLLOW) {
         Player player = this.getFollowTargetPlayer().orElse(null);
         if (player == null) {
            this.followCombatRecallActive = false;
         } else {
            Entity movingEntity = (Entity)(this.m_20202_() != null ? this.m_20201_() : this);
            double distanceSq = movingEntity.m_20280_(player);
            if (!this.followCombatRecallActive && distanceSq > 576.0) {
               this.followCombatRecallActive = true;
            } else if (this.followCombatRecallActive && distanceSq <= 100.0) {
               this.followCombatRecallActive = false;
            }

            if (this.followCombatRecallActive) {
               this.m_6710_(null);
               this.m_6703_(null);
            }
         }
      } else {
         this.followCombatRecallActive = false;
      }
   }

   public boolean isFollowCombatRecallActive() {
      return this.followCombatRecallActive;
   }

   private void freezeForGui() {
      this.m_21573_().m_26573_();
      this.m_20256_(Vec3.f_82478_);
      Entity vehicle = this.m_20202_();
      if (vehicle != null && vehicle.m_146895_() == this) {
         if (vehicle instanceof Mob mount) {
            mount.m_21573_().m_26573_();
         }

         vehicle.m_20256_(Vec3.f_82478_);
      }
   }

   public boolean hasCommandPermission(Player player) {
      return this.getOwner().map(owner -> owner.equals(player.m_20148_())).orElse(true);
   }

   public NpcCommandType getCurrentCommand() {
      return parseCommand((String)this.f_19804_.m_135370_(CURRENT_COMMAND));
   }

   public void setCurrentCommand(NpcCommandType command) {
      this.f_19804_.m_135381_(CURRENT_COMMAND, (command == null ? NpcCommandType.NONE : command).name());
   }

   public BlockPos getCommandTarget() {
      return (BlockPos)this.f_19804_.m_135370_(COMMAND_TARGET);
   }

   public void setCommandTarget(BlockPos target) {
      this.f_19804_.m_135381_(COMMAND_TARGET, target == null ? BlockPos.f_121853_ : target);
   }

   public void setMoveCommand(BlockPos target) {
      this.setFollowTarget(null);
      this.attackCommandTargetUuid = null;
      this.m_6710_(null);
      this.setCommandTarget(target);
      this.setCurrentCommand(NpcCommandType.MOVE);
   }

   public void setAttackCommand(LivingEntity target) {
      if (NpcTargeting.canHarm(this, target)) {
         this.setFollowTarget(null);
         this.attackCommandTargetUuid = target.m_20148_();
         this.m_6710_(target);
         this.setCurrentCommand(NpcCommandType.ATTACK);
      }
   }

   public void clearCommand() {
      if (this.getCurrentCommand() == NpcCommandType.FOLLOW) {
         this.setFollowTarget(null);
      }

      this.setCurrentCommand(NpcCommandType.NONE);
      this.attackCommandTargetUuid = null;
      this.setCommandTarget(BlockPos.f_121853_);
      this.m_6710_(null);
   }

   private void enforceAttackCommandTarget() {
      if (!this.m_9236_().f_46443_ && this.getCurrentCommand() == NpcCommandType.ATTACK) {
         if (this.attackCommandTargetUuid == null) {
            LivingEntity current = this.m_5448_();
            if (NpcTargeting.canHarm(this, current)) {
               this.attackCommandTargetUuid = current.m_20148_();
            }
         } else {
            if (this.m_9236_() instanceof ServerLevel serverLevel) {
               Entity entity = serverLevel.m_8791_(this.attackCommandTargetUuid);
               if (entity instanceof LivingEntity living && NpcTargeting.canHarm(this, living)) {
                  if (this.m_5448_() != living) {
                     this.m_6710_(living);
                  }

                  return;
               }

               if (entity != null) {
                  this.clearCommand();
               }
            }
         }
      } else {
         if (this.getCurrentCommand() != NpcCommandType.ATTACK) {
            this.attackCommandTargetUuid = null;
         }
      }
   }

   public void assignCombatGroupFormation(PlayerOwnedNpcEntity commander, double lateralOffset, double rearOffset) {
      if (commander != null) {
         this.combatGroupCommanderId = commander.m_20148_();
         this.combatGroupOffset = new Vec3(lateralOffset, 0.0, rearOffset);
         this.combatGroupRefreshTime = this.m_9236_().m_46467_();
      }
   }

   public boolean canJoinCombatGroup(PlayerOwnedNpcEntity commander) {
      if (commander != null && commander != this) {
         Optional<PlayerOwnedNpcEntity> currentCommander = this.getCombatGroupCommander();
         return currentCommander.isEmpty() || currentCommander.get() == commander;
      } else {
         return false;
      }
   }

   public boolean isCombatGroupEligible() {
      return this instanceof CombatNpcEntity;
   }

   public boolean isSiegeEngineerProfession() {
      return false;
   }

   public boolean canReceiveAttackCommand() {
      return this instanceof CombatNpcEntity;
   }

   protected boolean hasActiveWeaponDeployment() {
      return false;
   }

   public Optional<PlayerOwnedNpcEntity> getCombatGroupCommander() {
      if (this.combatGroupCommanderId != null && !this.m_9236_().f_46443_ && this.m_9236_().m_46467_() - this.combatGroupRefreshTime <= 40L) {
         if (((ServerLevel)this.m_9236_()).m_8791_(this.combatGroupCommanderId) instanceof PlayerOwnedNpcEntity commander
            && commander.m_6084_()
            && this.getOwner().equals(commander.getOwner())
            && CommandBatonItem.isCombatGroupMode(commander.m_21205_())
            && commander.isCombatGroupEligible()
            && commander.isDragonRiderCombatGroupCommander() == this.isLogicalDragonRider()
            && commander.isHippogryphCombatGroupCommander() == this.isHippogryphRider()
            && commander.isHippocampusCombatGroupCommander() == this.isHippocampusRider()
            && CommandBatonItem.containsCombatGroupNpc(commander.m_21205_(), this.m_20148_())) {
            return Optional.of(commander);
         }

         return Optional.empty();
      } else {
         return Optional.empty();
      }
   }

   public Optional<Vec3> getCombatGroupFormationPosition() {
      return this.getCombatGroupCommander().map(commander -> {
         double yaw = Math.toRadians((double)commander.m_146908_());
         Vec3 right = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
         Vec3 rear = new Vec3(Math.sin(yaw), 0.0, -Math.cos(yaw));
         Vec3 anchor = commander.m_20182_();
         if (commander.isLogicalDragonRider()) {
            Entity mount = DragonRiderController.getLogicalMount(commander);
            if (mount != null) {
               anchor = mount.m_20182_();
            } else {
               anchor = anchor.m_82492_(0.0, 2.35, 0.0);
            }
         } else if (commander instanceof CombatNpcEntity && IceAndFireHippogryphCompat.isHippogryph(commander.m_20202_())) {
            anchor = commander.m_20202_().m_20182_();
         } else if (commander instanceof CombatNpcEntity && IceAndFireHippocampusCompat.isHippocampus(commander.m_20202_())) {
            anchor = commander.m_20202_().m_20182_();
         }

         return anchor.m_82549_(right.m_82490_(this.combatGroupOffset.f_82479_)).m_82549_(rear.m_82490_(this.combatGroupOffset.f_82481_));
      });
   }

   public boolean isCombatGroupCommander() {
      return this.isCombatGroupEligible() && CommandBatonItem.isCombatGroupMode(this.m_21205_());
   }

   public boolean isDragonRiderCombatGroupCommander() {
      return this instanceof CombatNpcEntity && this.isLogicalDragonRider() && this.isCombatGroupCommander();
   }

   public boolean isHippogryphRider() {
      return this instanceof CombatNpcEntity && IceAndFireHippogryphCompat.isHippogryph(this.m_20202_());
   }

   public boolean isHippogryphCombatGroupCommander() {
      return this.isHippogryphRider() && this.isCombatGroupCommander();
   }

   public boolean isHippocampusRider() {
      return this instanceof CombatNpcEntity && IceAndFireHippocampusCompat.isHippocampus(this.m_20202_());
   }

   public boolean isHippocampusCombatGroupCommander() {
      return this.isHippocampusRider() && this.isCombatGroupCommander();
   }

   public boolean shouldFollowCombatGroupFormation() {
      LivingEntity target = this.m_5448_();
      return this.getCurrentCommand() == NpcCommandType.NONE
         && !this.isGettingFood()
         && (target == null || !target.m_6084_())
         && this.getCombatGroupCommander().isPresent();
   }

   public boolean hasHome() {
      return (Boolean)this.f_19804_.m_135370_(HOME_SET);
   }

   public BlockPos getHomePos() {
      return (BlockPos)this.f_19804_.m_135370_(HOME_POS);
   }

   public void setHome(BlockPos pos) {
      this.f_19804_.m_135381_(HOME_SET, true);
      this.f_19804_.m_135381_(HOME_POS, pos == null ? this.m_20183_() : pos);
   }

   public void clearHome() {
      this.f_19804_.m_135381_(HOME_SET, false);
      this.f_19804_.m_135381_(HOME_POS, BlockPos.f_121853_);
   }

   public Optional<UUID> getFollowTarget() {
      return (Optional<UUID>)this.f_19804_.m_135370_(FOLLOW_TARGET);
   }

   public void setFollowTarget(UUID followTarget) {
      this.f_19804_.m_135381_(FOLLOW_TARGET, Optional.ofNullable(followTarget));
   }

   public Optional<Player> getFollowTargetPlayer() {
      return this.getFollowTarget().<Player>map(this.m_9236_()::m_46003_).filter(player -> player.m_6084_() && !player.m_5833_());
   }

   public void toggleFollow(Player player) {
      if (this.getCurrentCommand() == NpcCommandType.FOLLOW) {
         this.clearCommand();
         if (this instanceof WorkerNpcEntity worker) {
            worker.setWorkState("idle");
         }

         player.m_5661_(Component.m_237110_("guistrings.npc.follow_disabled", new Object[]{this.m_5446_()}), true);
      } else {
         this.clearCommand();
         this.setGettingFood(false);
         this.setFollowTarget(player.m_20148_());
         this.setCurrentCommand(NpcCommandType.FOLLOW);
         if (this instanceof WorkerNpcEntity worker) {
            worker.setWorkState("follow");
         }

         player.m_5661_(Component.m_237110_("guistrings.npc.follow_enabled", new Object[]{this.m_5446_()}), true);
      }
   }

   public ItemStack getOrdersStack() {
      return (ItemStack)this.f_19804_.m_135370_(ORDERS_STACK);
   }

   public void setOrdersStack(ItemStack stack) {
      this.f_19804_.m_135381_(ORDERS_STACK, stack == null ? ItemStack.f_41583_ : stack.m_41777_());
   }

   public ItemStack getUpkeepStack() {
      return (ItemStack)this.f_19804_.m_135370_(UPKEEP_STACK);
   }

   public void setUpkeepStack(ItemStack stack) {
      this.f_19804_.m_135381_(UPKEEP_STACK, stack == null ? ItemStack.f_41583_ : stack.m_41777_());
   }

   public ItemStack getFoodStack() {
      return (ItemStack)this.f_19804_.m_135370_(FOOD_STACK);
   }

   public void setFoodStack(ItemStack stack) {
      if (stack != null && !stack.m_41619_() && foodValue(stack) > 0) {
         ItemStack stored = stack.m_41777_();
         stored.m_41764_(Math.min(stored.m_41613_(), 64));
         this.f_19804_.m_135381_(FOOD_STACK, stored);
         if (!this.m_9236_().f_46443_) {
            this.stopGettingFood();
         }
      } else {
         this.f_19804_.m_135381_(FOOD_STACK, ItemStack.f_41583_);
      }
   }

   public int getFoodRemaining() {
      return (Integer)this.f_19804_.m_135370_(FOOD_REMAINING);
   }

   public boolean isGettingFood() {
      return (Boolean)this.f_19804_.m_135370_(GETTING_FOOD);
   }

   boolean isFoodSupplyBlockedByDeployment() {
      if (this.isGuiInteractionPaused() || this.m_20159_() || this.getCurrentCommand() != NpcCommandType.NONE) {
         return true;
      } else if (this.hasActiveWeaponDeployment()) {
         return true;
      } else {
         LivingEntity combatTarget = this.m_5448_();
         if (combatTarget != null && combatTarget.m_6084_()) {
            return true;
         } else if (!this.getCombatGroupCommander().isPresent()
            && (!this.isCombatGroupCommander() || CommandBatonItem.getCombatGroupNpcs(this.m_21205_()).isEmpty())) {
            if (this instanceof CombatNpcEntity combat && combat.hasActiveCombatDeployment()) {
               return true;
            }

            if (this instanceof WorkerNpcEntity worker && worker.shouldRestAtHome()) {
               return true;
            }

            if (this instanceof PriestNpcEntity priest && priest.hasActiveResurrectionDeployment()) {
               return true;
            }

            return false;
         } else {
            return true;
         }
      }
   }

   private void setGettingFood(boolean gettingFood) {
      this.f_19804_.m_135381_(GETTING_FOOD, gettingFood);
   }

   public void setFoodRemaining(int amount) {
      this.f_19804_.m_135381_(FOOD_REMAINING, Math.max(0, amount));
   }

   public static int foodValue(ItemStack stack) {
      if (stack == null || stack.m_41619_()) {
         return 0;
      } else if (stack.m_150930_(Items.f_42583_) || stack.m_150930_(Items.f_42675_) || stack.m_150930_(Items.f_42591_)) {
         return 0;
      } else if (stack.m_150930_(Items.f_42410_)) {
         return 3000;
      } else if (stack.m_150930_(Items.f_42400_)) {
         return 4500;
      } else {
         FoodProperties food = stack.m_41720_().m_41473_();
         if (food == null) {
            return 0;
         } else {
            int nutrition = food.m_38744_();
            return Math.max(0, (nutrition + nutrition * (int)(food.m_38745_() * 2.0F)) * 350);
         }
      }
   }

   protected boolean tryAcceptFood(Player player, InteractionHand hand) {
      ItemStack offered = player.m_21120_(hand);
      int value = foodValue(offered);
      UpkeepOrderData.UpkeepEntry entry = UpkeepOrderData.getEntry(this.getUpkeepStack()).orElse(null);
      int target = entry == null ? 6000 : entry.upkeepAmount();
      if (value > 0 && this.getFoodRemaining() < target) {
         if (!this.m_9236_().f_46443_ && !player.m_150110_().f_35937_) {
            offered.m_41774_(1);
         }

         this.setFoodRemaining(this.getFoodRemaining() + value);
         this.stopGettingFood();
         return true;
      } else {
         return false;
      }
   }

   public boolean withdrawFood(ItemHandlerAccess handler) {
      UpkeepOrderData.UpkeepEntry entry = UpkeepOrderData.getEntry(this.getUpkeepStack()).orElse(null);
      int target = entry == null ? 6000 : entry.upkeepAmount();
      int needed = target - this.getFoodRemaining();
      if (needed > 0 && handler != null) {
         ItemStack requestedFood = entry == null ? ItemStack.f_41583_ : UpkeepOrderData.getFoodTarget(this.getUpkeepStack());
         ItemStack storedFood = this.getFoodStack();
         if (!requestedFood.m_41619_() && !storedFood.m_41619_() && !ItemStack.m_150942_(requestedFood, storedFood)) {
            return false;
         } else {
            int gained = 0;

            for (int slot = 0; slot < handler.getSlots() && gained < needed; slot++) {
               ItemStack stack = handler.getStackInSlot(slot);
               if (foodValue(stack) > 0 && (requestedFood.m_41619_() || ItemStack.m_150942_(requestedFood, stack))) {
                  int maxCount = requestedFood.m_41619_() ? 1 : requestedFood.m_41613_();
                  ItemStack removed = handler.extractItem(slot, Math.min(maxCount, stack.m_41613_()), false);
                  if (!removed.m_41619_()) {
                     int value = foodValue(removed);
                     int consumeCount = Math.min(removed.m_41613_(), Math.max(1, (int)Math.ceil((double)needed / (double)Math.max(1, value))));
                     gained += value * consumeCount;
                     if (removed.m_41613_() > consumeCount) {
                        this.storeFood(removed.m_255036_(removed.m_41613_() - consumeCount));
                     }
                  }
               }
            }

            if (gained > 0) {
               this.setFoodRemaining(this.getFoodRemaining() + gained);
            }

            return gained > 0;
         }
      } else {
         return this.getFoodRemaining() >= target;
      }
   }

   private void storeFood(ItemStack food) {
      if (!food.m_41619_() && foodValue(food) > 0) {
         ItemStack current = this.getFoodStack();
         if (current.m_41619_()) {
            this.setFoodStack(food);
         } else if (ItemStack.m_150942_(current, food)) {
            current.m_41769_(Math.min(food.m_41613_(), 64 - current.m_41613_()));
            this.setFoodStack(current);
         }
      }
   }

   private void updateFoodSupply() {
      if (!this.m_9236_().f_46443_ && !this.m_5803_()) {
         if (this.foodSupplyRetryTicks > 0) {
            this.foodSupplyRetryTicks--;
         }

         if (this.getFoodRemaining() > 0) {
            this.setFoodRemaining(this.getFoodRemaining() - 1);
         }

         ItemStack carriedFood = this.getFoodStack();
         if (!carriedFood.m_41619_() && foodValue(carriedFood) > 0) {
            if (this.getFoodRemaining() <= 0) {
               this.consumeFoodStack();
            }

            this.stopGettingFood();
         } else if (this.isFoodSupplyBlockedByDeployment()) {
            this.stopGettingFood();
         } else if (this.getCurrentCommand() != NpcCommandType.NONE) {
            this.stopGettingFood();
         } else {
            LivingEntity combatTarget = this.m_5448_();
            if (combatTarget != null && combatTarget.m_6084_()) {
               this.stopGettingFood();
            } else if (this.getFoodRemaining() > 0) {
               this.stopGettingFood();
            } else {
               UpkeepOrderData.UpkeepEntry entry = UpkeepOrderData.getEntry(this.getUpkeepStack()).orElse(null);
               BlockPos supplyPos = entry == null ? (this.hasTownHall() ? this.getTownHallPos() : null) : entry.pos();
               if (supplyPos != null && this.isSameSupplyDimension(entry)) {
                  if (entry == null && this.m_9236_().m_46805_(supplyPos) && !(this.m_9236_().m_7702_(supplyPos) instanceof TownHallBlockEntity)) {
                     this.clearTownHall();
                     this.stopGettingFood();
                  } else {
                     if (this.foodSupplyRetryTicks <= 0) {
                        this.setGettingFood(true);
                     }

                     if (this.isGettingFood()) {
                        double distance = this.m_20275_(
                           (double)supplyPos.m_123341_() + 0.5, (double)supplyPos.m_123342_() + 0.5, (double)supplyPos.m_123343_() + 0.5
                        );
                        if (distance > 9.0) {
                           this.m_21573_()
                              .m_26519_(
                                 (double)supplyPos.m_123341_() + 0.5,
                                 (double)supplyPos.m_123342_() + 0.5,
                                 (double)supplyPos.m_123343_() + 0.5,
                                 this instanceof WorkerNpcEntity ? this.playerSprintNavigationSpeed() : this.playerWalkNavigationSpeed()
                              );
                           this.m_21563_()
                              .m_24946_((double)supplyPos.m_123341_() + 0.5, (double)supplyPos.m_123342_() + 0.5, (double)supplyPos.m_123343_() + 0.5);
                        } else {
                           BlockEntity blockEntity = this.m_9236_().m_7702_(supplyPos);
                           Direction side = entry == null ? Direction.DOWN : entry.side();
                           if (blockEntity instanceof TownHallBlockEntity townHall) {
                              ItemStack removed = townHall.extractFoodLike(1);
                              if (!removed.m_41619_()) {
                                 this.setFoodRemaining(this.getFoodRemaining() + foodValue(removed));
                                 if (this.getFoodRemaining() > 0) {
                                    this.stopGettingFood();
                                 }
                              } else {
                                 this.abandonFoodSupplyAttempt();
                              }
                           } else if (blockEntity != null) {
                              ItemHandlerAccess handler = AWCorePlatform.findItemHandler(blockEntity, side);
                              if (handler != null && this.withdrawFood(handler) && this.getFoodRemaining() > 0) {
                                 this.stopGettingFood();
                              } else {
                                 this.abandonFoodSupplyAttempt();
                              }
                           } else {
                              this.abandonFoodSupplyAttempt();
                           }
                        }
                     }
                  }
               } else {
                  this.stopGettingFood();
               }
            }
         }
      }
   }

   private void abandonFoodSupplyAttempt() {
      this.foodSupplyRetryTicks = 200;
      this.stopGettingFood();
   }

   private void stopGettingFood() {
      if (this.isGettingFood()) {
         this.setGettingFood(false);
         this.m_21573_().m_26573_();
      }
   }

   private boolean consumeFoodStack() {
      ItemStack food = this.getFoodStack();
      if (food.m_41619_()) {
         return false;
      } else {
         int value = foodValue(food);
         if (value <= 0) {
            this.setFoodStack(ItemStack.f_41583_);
            return false;
         } else {
            ItemStack remaining = food.m_41777_();
            remaining.m_41774_(1);
            this.setFoodStack(remaining);
            this.setFoodRemaining(value);
            return true;
         }
      }
   }

   private boolean isSameSupplyDimension(UpkeepOrderData.UpkeepEntry entry) {
      return entry == null || entry.dimension().isEmpty() || entry.dimension().equals(this.m_9236_().m_46472_().m_135782_().toString());
   }

   public boolean hasTownHall() {
      return (Boolean)this.f_19804_.m_135370_(TOWN_HALL_SET);
   }

   public BlockPos getTownHallPos() {
      return (BlockPos)this.f_19804_.m_135370_(TOWN_HALL_POS);
   }

   public void handleTownHallBroadcast(BlockPos pos) {
      if (pos != null) {
         if (!this.hasTownHall() || this.closerThanCurrentTownHall(pos)) {
            this.f_19804_.m_135381_(TOWN_HALL_SET, true);
            this.f_19804_.m_135381_(TOWN_HALL_POS, pos.m_7949_());
         }
      }
   }

   public void clearTownHall() {
      this.f_19804_.m_135381_(TOWN_HALL_SET, false);
      this.f_19804_.m_135381_(TOWN_HALL_POS, BlockPos.f_121853_);
   }

   public int getDragonRiderMountId() {
      return (Integer)this.f_19804_.m_135370_(DRAGON_RIDER_MOUNT);
   }

   public void setDragonRiderMountId(int id) {
      this.f_19804_.m_135381_(DRAGON_RIDER_MOUNT, id);
   }

   public UUID getDragonRiderMountUuid() {
      return this.dragonRiderMountUuid;
   }

   public void setDragonRiderMount(Entity mount) {
      this.dragonRiderMountUuid = mount == null ? null : mount.m_20148_();
      this.setDragonRiderMountId(mount == null ? -1 : mount.m_19879_());
   }

   public void clearDragonRiderMount() {
      this.dragonRiderMountUuid = null;
      this.setDragonRiderMountId(-1);
   }

   public boolean isLogicalDragonRider() {
      return this.getDragonRiderMountId() >= 0 || this.dragonRiderMountUuid != null;
   }

   public void m_8107_() {
      if (this.isGuiInteractionPaused()) {
         this.freezeForGui();
      } else {
         super.m_8107_();
         this.updateOwnedMountedMovement();
      }
   }

   public double m_6049_() {
      return this.m_20202_() instanceof Mob ? super.m_6049_() - 0.45 : super.m_6049_();
   }

   public void m_6667_(DamageSource damageSource) {
      if (!this.f_20890_) {
         TownHallBlockEntity townHall = TownHallBlockEntity.findForDeath(this);
         boolean deferred = townHall == null
            && this.hasTownHall()
            && this.m_9236_() instanceof ServerLevel server
            && server.m_7726_().m_7131_(this.getTownHallPos().m_123341_() >> 4, this.getTownHallPos().m_123343_() >> 4) == null;
         CompoundTag details = townHall == null && !deferred ? null : TownHallBlockEntity.captureDeathDetails(this, damageSource);
         if (townHall != null) {
            details.m_128356_("TownHallPos", townHall.m_58899_().m_121878_());
         }

         super.m_6667_(damageSource);
         if (this.f_20890_) {
            this.recordTownHallDeath(townHall, damageSource, details);
            if (deferred && this.m_9236_() instanceof ServerLevel serverx) {
               TownHallDeathData.get(serverx).enqueue(this.getTownHallPos(), TownHallBlockEntity.createDeathEntry(this, damageSource, details));
            }
         }
      }
   }

   private boolean closerThanCurrentTownHall(BlockPos pos) {
      BlockPos current = this.getTownHallPos();
      return BlockPos.f_121853_.equals(current)
         ? true
         : this.m_20275_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5)
            < this.m_20275_((double)current.m_123341_() + 0.5, (double)current.m_123342_() + 0.5, (double)current.m_123343_() + 0.5);
   }

   private void recordTownHallDeath(TownHallBlockEntity townHall, DamageSource damageSource, CompoundTag details) {
      if (!this.m_9236_().f_46443_ && townHall != null && !townHall.m_58901_() && details != null) {
         townHall.recordNpcDeath(this, damageSource, details);
      }
   }

   private void updateOwnedMountedMovement() {
      Entity vehicle = this.m_20202_();
      if (vehicle != null && IceAndFireHippocampusCompat.isHippocampus(vehicle)) {
         this.m_21573_().m_26573_();
      } else {
         if (this.shouldFollowCombatGroupFormation()) {
            Optional<Vec3> formationPosition = this.getCombatGroupFormationPosition();
            if (formationPosition.isPresent()) {
               Vec3 destination = formationPosition.get();
               if (vehicle instanceof ChestPushcartEntity cart) {
                  this.driveCombatGroupCart(cart, destination);
                  return;
               }

               label77: {
                  if (vehicle instanceof Mob mount && mount.m_20182_().m_82557_(destination) > 2.25) {
                     this.driveMountToward(mount, destination);
                     break label77;
                  }

                  if (vehicle instanceof Mob mount) {
                     this.stopMount(mount, 0.5);
                  }
               }

               if (vehicle instanceof Mob) {
                  return;
               }
            }
         }

         if (vehicle instanceof Mob mount) {
            if (!this.usesCustomMountedMovement()) {
               if (!this.followCombatRecallActive || !this.updateMountedFollowMovement()) {
                  LivingEntity target = this.m_5448_();
                  if (target != null && target.m_6084_()) {
                     double stopDistance = "archer".equals(this.getNpcSubtype()) ? 12.0 : 2.5;
                     if (mount.m_20280_(target) > stopDistance * stopDistance) {
                        this.driveMountToward(mount, target.m_20182_());
                     } else {
                        this.stopMount(mount, 0.55);
                        this.faceMountedTarget(mount, target.m_20182_());
                     }
                  } else {
                     if (this.getCurrentCommand() == NpcCommandType.MOVE) {
                        BlockPos targetPos = this.getCommandTarget();
                        if (!BlockPos.f_121853_.equals(targetPos)) {
                           Vec3 destinationx = Vec3.m_82539_(targetPos);
                           if (mount.m_20238_(destinationx) > 4.0) {
                              this.driveMountToward(mount, destinationx);
                           } else {
                              this.stopMount(mount, 0.4);
                              this.clearCommand();
                           }

                           return;
                        }
                     }

                     if (!this.updateMountedFollowMovement()) {
                        if (!this.shouldSkipIdleMountedStop()) {
                           this.stopMount(mount, 0.4);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   protected boolean updateMountedFollowMovement() {
      if (this.getCurrentCommand() == NpcCommandType.FOLLOW && !this.isGuiInteractionPaused()) {
         LivingEntity combatTarget = this.m_5448_();
         if (!this.followCombatRecallActive && combatTarget != null && combatTarget.m_6084_()) {
            return false;
         } else {
            Entity vehicle = this.m_20202_();
            if (vehicle instanceof Mob mount && !IceAndFireHippogryphCompat.isHippogryph(vehicle) && !IceAndFireHippocampusCompat.isHippocampus(vehicle)) {
               Player player = this.getFollowTargetPlayer().orElse(null);
               if (player == null) {
                  this.stopMount(mount, 0.4);
                  return true;
               }

               double distanceSq = mount.m_20280_(player);
               if (distanceSq > 100.0) {
                  this.driveMountToward(mount, player.m_20182_());
               } else if (distanceSq <= 9.0) {
                  this.stopMount(mount, 0.5);
               }

               return true;
            }

            return false;
         }
      } else {
         return false;
      }
   }

   protected boolean updateMountedMoveMovement() {
      if (this.getCurrentCommand() == NpcCommandType.MOVE && !this.isGuiInteractionPaused()) {
         LivingEntity combatTarget = this.m_5448_();
         if (combatTarget != null && combatTarget.m_6084_()) {
            return false;
         } else {
            Entity vehicle = this.m_20202_();
            if (vehicle instanceof Mob mount && !IceAndFireHippogryphCompat.isHippogryph(vehicle) && !IceAndFireHippocampusCompat.isHippocampus(vehicle)) {
               BlockPos target = this.getCommandTarget();
               if (BlockPos.f_121853_.equals(target)) {
                  this.clearCommand();
                  this.stopMount(mount, 0.4);
                  return true;
               }

               Vec3 destination = Vec3.m_82539_(target);
               if (mount.m_20238_(destination) > 4.0) {
                  this.driveMountToward(mount, destination);
               } else {
                  this.stopMount(mount, 0.4);
                  this.clearCommand();
               }

               return true;
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private void driveCombatGroupCart(ChestPushcartEntity cart, Vec3 destination) {
      if (cart instanceof BatteringRamEntity ram && ram.hasNpcCombatControl(this)) {
         return;
      }

      Vec3 delta = destination.m_82546_(cart.m_20182_());
      if (delta.m_165925_() <= 2.25) {
         if (cart instanceof BatteringRamEntity ram) {
            ram.setNpcFormationInput(this, 0.0F, 0.0F);
         } else {
            cart.setVehicleInput(0.0F, 0.0F);
         }
      } else {
         float desiredYaw = (float)Math.toDegrees(Math.atan2(-delta.f_82479_, delta.f_82481_));
         float yawError = Mth.m_14177_(desiredYaw - cart.m_146908_());
         float turn = Mth.m_14036_(-yawError / 28.0F, -1.0F, 1.0F);
         float forward = Math.abs(yawError) > 75.0F ? 0.2F : 1.0F;
         if (cart instanceof BatteringRamEntity ram) {
            ram.setNpcFormationInput(this, forward, turn);
         } else {
            cart.setVehicleInput(forward, turn);
         }
      }
   }

   protected boolean shouldSkipIdleMountedStop() {
      return false;
   }

   protected boolean usesCustomMountedMovement() {
      return false;
   }

   private void driveMountToward(Mob mount, Vec3 destination) {
      Vec3 delta = destination.m_82546_(mount.m_20182_());
      Vec3 horizontal = new Vec3(delta.f_82479_, 0.0, delta.f_82481_);
      if (!(horizontal.m_82556_() < 1.0E-4)) {
         double speed = 0.22;
         AttributeInstance speedAttribute = mount.m_21051_(Attributes.f_22279_);
         if (speedAttribute != null) {
            speed = Math.max(speed, speedAttribute.m_22135_() * 0.65);
         }

         Vec3 direction = horizontal.m_82541_();
         Vec3 current = mount.m_20184_();
         mount.m_20334_(direction.f_82479_ * speed, current.f_82480_, direction.f_82481_ * speed);
         mount.m_21573_().m_26573_();
         this.faceDirection(mount, direction);
      }
   }

   private void stopMount(Mob mount, double damping) {
      mount.m_21573_().m_26573_();
      mount.m_20256_(mount.m_20184_().m_82542_(damping, 1.0, damping));
   }

   private void faceMountedTarget(Mob mount, Vec3 destination) {
      Vec3 delta = destination.m_82546_(mount.m_20182_());
      Vec3 horizontal = new Vec3(delta.f_82479_, 0.0, delta.f_82481_);
      if (horizontal.m_82556_() >= 1.0E-4) {
         this.faceDirection(mount, horizontal.m_82541_());
      }
   }

   private void faceDirection(Mob mount, Vec3 direction) {
      float yaw = (float)(Math.atan2(direction.f_82481_, direction.f_82479_) * 180.0F / (float)Math.PI) - 90.0F;
      mount.m_146922_(yaw);
      mount.m_5618_(yaw);
      mount.m_5616_(yaw);
      this.m_146922_(yaw);
      this.m_5618_(yaw);
      this.m_5616_(yaw);
   }

   private static NpcCommandType parseCommand(String name) {
      try {
         return NpcCommandType.valueOf(name);
      } catch (IllegalArgumentException var2) {
         return NpcCommandType.NONE;
      }
   }
}
