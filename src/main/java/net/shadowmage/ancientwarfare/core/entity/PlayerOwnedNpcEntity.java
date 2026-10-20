package net.shadowmage.ancientwarfare.core.entity;

import java.util.ArrayDeque;
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
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;
import net.shadowmage.ancientwarfare.core.tile.TownHallBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.TownHallDeathData;

public abstract class PlayerOwnedNpcEntity extends NpcBaseEntity implements NpcOwnerAccess {
   private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(PlayerOwnedNpcEntity.class, EntityDataSerializers.OPTIONAL_UUID);
   private static final EntityDataAccessor<String> CURRENT_COMMAND = SynchedEntityData.defineId(PlayerOwnedNpcEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<BlockPos> COMMAND_TARGET = SynchedEntityData.defineId(PlayerOwnedNpcEntity.class, EntityDataSerializers.BLOCK_POS);
   private static final EntityDataAccessor<Boolean> HOME_SET = SynchedEntityData.defineId(PlayerOwnedNpcEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<BlockPos> HOME_POS = SynchedEntityData.defineId(PlayerOwnedNpcEntity.class, EntityDataSerializers.BLOCK_POS);
   private static final EntityDataAccessor<Optional<UUID>> FOLLOW_TARGET = SynchedEntityData.defineId(
      PlayerOwnedNpcEntity.class, EntityDataSerializers.OPTIONAL_UUID
   );
   private static final EntityDataAccessor<ItemStack> ORDERS_STACK = SynchedEntityData.defineId(PlayerOwnedNpcEntity.class, EntityDataSerializers.ITEM_STACK);
   private static final EntityDataAccessor<ItemStack> UPKEEP_STACK = SynchedEntityData.defineId(PlayerOwnedNpcEntity.class, EntityDataSerializers.ITEM_STACK);
   private static final EntityDataAccessor<ItemStack> FOOD_STACK = SynchedEntityData.defineId(PlayerOwnedNpcEntity.class, EntityDataSerializers.ITEM_STACK);
   private static final EntityDataAccessor<Integer> FOOD_REMAINING = SynchedEntityData.defineId(PlayerOwnedNpcEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> GETTING_FOOD = SynchedEntityData.defineId(PlayerOwnedNpcEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> TOWN_HALL_SET = SynchedEntityData.defineId(PlayerOwnedNpcEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<BlockPos> TOWN_HALL_POS = SynchedEntityData.defineId(PlayerOwnedNpcEntity.class, EntityDataSerializers.BLOCK_POS);
   private static final String TAG_OWNER = "Owner";
   // Mirrors OWNER: ally checks read it for every nearby NPC, and synched data reads take a lock.
   private Optional<UUID> owner;
   private static final String TAG_CURRENT_COMMAND = "CurrentCommand";
   private static final int MAX_QUEUED_MOVES = 16;
   private static final double HOLD_LEASH_SQ = 400.0;
   private static final String TAG_COMMAND_TARGET = "CommandTarget";
   private static final String TAG_HOME_SET = "HomeSet";
   private static final String TAG_HOME_POS = "HomePos";
   private static final String TAG_FOLLOW_TARGET = "FollowTarget";
   private static final String TAG_ORDERS_STACK = "OrdersStack";
   private static final String TAG_UPKEEP_STACK = "UpkeepStack";
   private static final String TAG_FOOD_STACK = "FoodStack";
   private static final String TAG_TOWN_HALL_SET = "TownHallSet";
   private static final String TAG_TOWN_HALL_POS = "TownHallPos";
   private static final String TAG_FOOD_REMAINING = "FoodRemaining";
   private static final String TAG_ATTACK_COMMAND_TARGET = "AttackCommandTarget";
   private static final int FOOD_ACTION_RANGE = 3;
   private static final int FOOD_SUPPLY_RETRY_TICKS = 200;
   private static final double FOLLOW_COMBAT_RECALL_DISTANCE = 24.0;
   private static final double FOLLOW_COMBAT_RESUME_DISTANCE = 10.0;
   private final Map<UUID, Integer> guiViewers = new HashMap<>();
   private UUID combatGroupCommanderId;
   private Vec3 combatGroupOffset = Vec3.ZERO;
   private long combatGroupRefreshTime = Long.MIN_VALUE;
   private UUID attackCommandTargetUuid;
   private final ArrayDeque<PlayerOwnedNpcEntity.Waypoint> queuedMoves = new ArrayDeque<>();
   private float holdYaw = Float.NaN;
   private int foodSupplyRetryTicks;
   private boolean followCombatRecallActive;

   protected PlayerOwnedNpcEntity(EntityType<? extends PlayerOwnedNpcEntity> entityType, Level level) {
      super(entityType, level);
      this.goalSelector.addGoal(3, new FoodSupplyGoal(this));
      this.goalSelector.addGoal(3, new FollowPlayerGoal(this, 3.0F, 6.0F));
   }

   @Override
   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(OWNER, Optional.empty());
      this.entityData.define(CURRENT_COMMAND, NpcCommandType.NONE.name());
      this.entityData.define(COMMAND_TARGET, BlockPos.ZERO);
      this.entityData.define(HOME_SET, false);
      this.entityData.define(HOME_POS, BlockPos.ZERO);
      this.entityData.define(FOLLOW_TARGET, Optional.empty());
      this.entityData.define(ORDERS_STACK, ItemStack.EMPTY);
      this.entityData.define(UPKEEP_STACK, ItemStack.EMPTY);
      this.entityData.define(FOOD_STACK, ItemStack.EMPTY);
      this.entityData.define(FOOD_REMAINING, 0);
      this.entityData.define(GETTING_FOOD, false);
      this.entityData.define(TOWN_HALL_SET, false);
      this.entityData.define(TOWN_HALL_POS, BlockPos.ZERO);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      this.getOwner().ifPresent(uuid -> tag.putUUID("Owner", uuid));
      tag.putString("CurrentCommand", this.getCurrentCommand().name());
      tag.putLong("CommandTarget", this.getCommandTarget().asLong());
      tag.putBoolean("HomeSet", this.hasHome());
      tag.putLong("HomePos", this.getHomePos().asLong());
      if (this.attackCommandTargetUuid != null) {
         tag.putUUID("AttackCommandTarget", this.attackCommandTargetUuid);
      }

      this.getFollowTarget().ifPresent(uuid -> tag.putUUID("FollowTarget", uuid));
      if (!this.queuedMoves.isEmpty()) {
         tag.putLongArray("QueuedMoves", this.queuedMoves.stream().mapToLong(waypoint -> waypoint.pos().asLong()).toArray());
         tag.putIntArray("QueuedYaws", this.queuedMoves.stream().mapToInt(waypoint -> Float.floatToIntBits(waypoint.yaw())).toArray());
      }

      if (!Float.isNaN(this.holdYaw)) {
         tag.putFloat("HoldYaw", this.holdYaw);
      }

      if (!this.getOrdersStack().isEmpty()) {
         tag.put("OrdersStack", this.getOrdersStack().save(new CompoundTag()));
      }

      if (!this.getUpkeepStack().isEmpty()) {
         tag.put("UpkeepStack", this.getUpkeepStack().save(new CompoundTag()));
      }

      if (!this.getFoodStack().isEmpty()) {
         tag.put("FoodStack", this.getFoodStack().save(new CompoundTag()));
      }

      tag.putInt("FoodRemaining", this.getFoodRemaining());
      tag.putBoolean("TownHallSet", this.hasTownHall());
      tag.putLong("TownHallPos", this.getTownHallPos().asLong());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.hasUUID("Owner")) {
         this.setOwner(tag.getUUID("Owner"));
      }

      this.attackCommandTargetUuid = tag.hasUUID("AttackCommandTarget") ? tag.getUUID("AttackCommandTarget") : null;
      if (tag.contains("CurrentCommand")) {
         this.setCurrentCommand(parseCommand(tag.getString("CurrentCommand")));
      }

      if (tag.contains("CommandTarget")) {
         this.setCommandTarget(BlockPos.of(tag.getLong("CommandTarget")));
      }

      this.queuedMoves.clear();
      long[] queuedPositions = tag.getLongArray("QueuedMoves");
      int[] queuedYaws = tag.getIntArray("QueuedYaws");
      for (int i = 0; i < queuedPositions.length; i++) {
         float yaw = i < queuedYaws.length ? Float.intBitsToFloat(queuedYaws[i]) : Float.NaN;
         this.queuedMoves.add(new PlayerOwnedNpcEntity.Waypoint(BlockPos.of(queuedPositions[i]), yaw));
      }

      this.holdYaw = tag.contains("HoldYaw") ? tag.getFloat("HoldYaw") : Float.NaN;

      if (tag.contains("HomeSet")) {
         this.entityData.set(HOME_SET, tag.getBoolean("HomeSet"));
      }

      if (tag.contains("HomePos")) {
         this.entityData.set(HOME_POS, BlockPos.of(tag.getLong("HomePos")));
      }

      if (tag.hasUUID("FollowTarget")) {
         this.setFollowTarget(tag.getUUID("FollowTarget"));
      }

      if (tag.contains("OrdersStack")) {
         this.setOrdersStack(ItemStack.of(tag.getCompound("OrdersStack")));
      }

      if (tag.contains("UpkeepStack")) {
         this.setUpkeepStack(ItemStack.of(tag.getCompound("UpkeepStack")));
      }

      if (tag.contains("FoodStack")) {
         this.setFoodStack(ItemStack.of(tag.getCompound("FoodStack")));
      }

      if (tag.contains("FoodRemaining")) {
         this.setFoodRemaining(tag.getInt("FoodRemaining"));
      }

      if (tag.contains("TownHallSet")) {
         this.entityData.set(TOWN_HALL_SET, tag.getBoolean("TownHallSet"));
      }

      if (tag.contains("TownHallPos")) {
         this.entityData.set(TOWN_HALL_POS, BlockPos.of(tag.getLong("TownHallPos")));
      }
   }

   @Override
   public Optional<UUID> getOwner() {
      Optional<UUID> cached = this.owner;
      return cached != null ? cached : (Optional<UUID>)this.entityData.get(OWNER);
   }

   public void setOwner(UUID owner) {
      Optional<UUID> value = Optional.ofNullable(owner);
      this.entityData.set(OWNER, value);
      this.owner = value;
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (OWNER.equals(key)) {
         this.owner = (Optional<UUID>)this.entityData.get(OWNER);
      }
   }

   public boolean isAlliedTo(Entity entity) {
      return NpcTargeting.isProtectedAlly(this, entity) || super.isAlliedTo(entity);
   }

   public void beginGuiInteraction(Player player) {
      if (!this.level().isClientSide && player != null && this.hasCommandPermission(player)) {
         if (this.guiViewers.isEmpty()) {
            this.freezeForGui();
         }

         this.guiViewers.merge(player.getUUID(), 1, Integer::sum);
      }
   }

   public void endGuiInteraction(Player player) {
      if (!this.level().isClientSide && player != null) {
         UUID playerId = player.getUUID();
         Integer openMenus = this.guiViewers.get(playerId);
         if (openMenus != null) {
            if (openMenus > 1) {
               this.guiViewers.put(playerId, openMenus - 1);
            } else {
               this.guiViewers.remove(playerId);
            }

            if (this.guiViewers.isEmpty()) {
               this.getNavigation().stop();
            }
         }
      }
   }

   public boolean isGuiInteractionPaused() {
      return !this.guiViewers.isEmpty();
   }

   @Override
   public void tick() {
      if (this.getTarget() != null && !NpcTargeting.canHarm(this, this.getTarget())) {
         this.setTarget(null);
         this.setLastHurtByMob(null);
      }

      this.updateFollowCombatRecall();
      this.enforceAttackCommandTarget();
      this.enforceHoldLeash();
      if (this.isGuiInteractionPaused()) {
         this.freezeForGui();
      }

      super.tick();
      if (this.isGuiInteractionPaused()) {
         this.freezeForGui();
      }

      this.updateFoodSupply();
   }

   private void updateFollowCombatRecall() {
      if (!this.level().isClientSide && this.getCurrentCommand() == NpcCommandType.FOLLOW) {
         Player player = this.getFollowTargetPlayer().orElse(null);
         if (player == null) {
            this.followCombatRecallActive = false;
         } else {
            Entity movingEntity = (Entity)(this.getVehicle() != null ? this.getRootVehicle() : this);
            double distanceSq = movingEntity.distanceToSqr(player);
            if (!this.followCombatRecallActive && distanceSq > 576.0) {
               this.followCombatRecallActive = true;
            } else if (this.followCombatRecallActive && distanceSq <= 100.0) {
               this.followCombatRecallActive = false;
            }

            if (this.followCombatRecallActive) {
               this.setTarget(null);
               this.setLastHurtByMob(null);
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
      this.getNavigation().stop();
      this.setDeltaMovement(Vec3.ZERO);
      Entity vehicle = this.getVehicle();
      if (vehicle != null && vehicle.getFirstPassenger() == this) {
         if (vehicle instanceof Mob mount) {
            mount.getNavigation().stop();
         }

         vehicle.setDeltaMovement(Vec3.ZERO);
      }
   }

   public boolean hasCommandPermission(Player player) {
      return this.getOwner().map(owner -> owner.equals(player.getUUID())).orElse(true);
   }

   public NpcCommandType getCurrentCommand() {
      return parseCommand((String)this.entityData.get(CURRENT_COMMAND));
   }

   public void setCurrentCommand(NpcCommandType command) {
      this.entityData.set(CURRENT_COMMAND, (command == null ? NpcCommandType.NONE : command).name());
   }

   public BlockPos getCommandTarget() {
      return (BlockPos)this.entityData.get(COMMAND_TARGET);
   }

   public void setCommandTarget(BlockPos target) {
      this.entityData.set(COMMAND_TARGET, target == null ? BlockPos.ZERO : target);
   }

   public void setMoveCommand(BlockPos target) {
      this.setMoveCommand(target, Float.NaN);
   }

   /** Moves to the target; {@code yaw} is the direction to face once there, or NaN for none. */
   public void setMoveCommand(BlockPos target, float yaw) {
      this.queuedMoves.clear();
      this.startMoveCommand(target, yaw);
   }

   /** Adds a move to the end of the order queue, or starts it at once when the NPC is not already moving. */
   public void queueMoveCommand(BlockPos target, float yaw) {
      if (this.getCurrentCommand() != NpcCommandType.MOVE) {
         this.setMoveCommand(target, yaw);
      } else if (this.queuedMoves.size() < MAX_QUEUED_MOVES) {
         this.queuedMoves.add(new PlayerOwnedNpcEntity.Waypoint(target, yaw));
      }
   }

   /** Called when the NPC reaches its move target: starts the next queued move, holds the spot, or goes back to its duties. */
   public void completeMoveCommand() {
      PlayerOwnedNpcEntity.Waypoint next = this.queuedMoves.poll();
      if (next != null) {
         this.startMoveCommand(next.pos(), next.yaw());
      } else if (this.holdsPositionAfterMove()) {
         this.setHoldCommand(this.getCommandTarget());
      } else {
         this.clearCommand();
      }
   }

   protected boolean holdsPositionAfterMove() {
      return false;
   }

   /** Keeps the NPC at the position: it fights enemies that come close and walks back afterwards. */
   public void setHoldCommand(BlockPos pos, float yaw) {
      this.holdYaw = yaw;
      this.setHoldCommand(pos);
   }

   public void setHoldCommand(BlockPos pos) {
      this.queuedMoves.clear();
      this.setFollowTarget(null);
      this.attackCommandTargetUuid = null;
      this.setCommandTarget(pos);
      this.setCurrentCommand(NpcCommandType.GUARD);
   }

   public boolean isHoldingPosition() {
      return this.getCurrentCommand() == NpcCommandType.GUARD && !BlockPos.ZERO.equals(this.getCommandTarget());
   }

   private void enforceHoldLeash() {
      if (!this.level().isClientSide && this.isHoldingPosition()) {
         LivingEntity target = this.getTarget();
         BlockPos hold = this.getCommandTarget();
         double leashSq = this.holdLeashSq();
         if (target != null && target.distanceToSqr((double)hold.getX() + 0.5, (double)hold.getY(), (double)hold.getZ() + 0.5) > leashSq) {
            this.setTarget(null);
         }
      }
   }

   /** How far from its post a holding NPC may follow a target before letting it go. */
   protected double holdLeashSq() {
      return HOLD_LEASH_SQ;
   }

   /** Direction a holding NPC faces when idle, or NaN when it may look around. */
   public float getHoldYaw() {
      return this.holdYaw;
   }

   private void startMoveCommand(BlockPos target, float yaw) {
      this.holdYaw = yaw;
      this.setFollowTarget(null);
      this.attackCommandTargetUuid = null;
      this.setTarget(null);
      this.setCommandTarget(target);
      this.setCurrentCommand(NpcCommandType.MOVE);
   }

   public void setAttackCommand(LivingEntity target) {
      if (NpcTargeting.canHarm(this, target)) {
         this.setFollowTarget(null);
         this.attackCommandTargetUuid = target.getUUID();
         // The command goes first: a passive soldier only takes a target it was ordered to attack.
         this.setCurrentCommand(NpcCommandType.ATTACK);
         this.setTarget(target);
      }
   }

   public void clearCommand() {
      this.queuedMoves.clear();
      this.holdYaw = Float.NaN;
      if (this.getCurrentCommand() == NpcCommandType.FOLLOW) {
         this.setFollowTarget(null);
      }

      this.setCurrentCommand(NpcCommandType.NONE);
      this.attackCommandTargetUuid = null;
      this.setCommandTarget(BlockPos.ZERO);
      this.setTarget(null);
   }

   private void enforceAttackCommandTarget() {
      if (!this.level().isClientSide && this.getCurrentCommand() == NpcCommandType.ATTACK) {
         if (this.attackCommandTargetUuid == null) {
            LivingEntity current = this.getTarget();
            if (NpcTargeting.canHarm(this, current)) {
               this.attackCommandTargetUuid = current.getUUID();
            }
         } else {
            if (this.level() instanceof ServerLevel serverLevel) {
               Entity entity = serverLevel.getEntity(this.attackCommandTargetUuid);
               if (entity instanceof LivingEntity living && NpcTargeting.canHarm(this, living)) {
                  if (this.getTarget() != living) {
                     this.setTarget(living);
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
         this.combatGroupCommanderId = commander.getUUID();
         this.combatGroupOffset = new Vec3(lateralOffset, 0.0, rearOffset);
         this.combatGroupRefreshTime = this.level().getGameTime();
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
      if (this.combatGroupCommanderId != null && !this.level().isClientSide && this.level().getGameTime() - this.combatGroupRefreshTime <= 40L) {
         if (((ServerLevel)this.level()).getEntity(this.combatGroupCommanderId) instanceof PlayerOwnedNpcEntity commander
            && commander.isAlive()
            && this.getOwner().equals(commander.getOwner())
            && CommandBatonItem.isCombatGroupMode(commander.getMainHandItem())
            && commander.isCombatGroupEligible()
            && CommandBatonItem.containsCombatGroupNpc(commander.getMainHandItem(), this.getUUID())) {
            return Optional.of(commander);
         }

         return Optional.empty();
      } else {
         return Optional.empty();
      }
   }

   public Optional<Vec3> getCombatGroupFormationPosition() {
      return this.getCombatGroupCommander().map(commander -> {
         double yaw = Math.toRadians((double)commander.getYRot());
         Vec3 right = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
         Vec3 rear = new Vec3(Math.sin(yaw), 0.0, -Math.cos(yaw));
         Vec3 anchor = commander.position();
         return anchor.add(right.scale(this.combatGroupOffset.x)).add(rear.scale(this.combatGroupOffset.z));
      });
   }

   public boolean isCombatGroupCommander() {
      return this.isCombatGroupEligible() && CommandBatonItem.isCombatGroupMode(this.getMainHandItem());
   }

   public boolean shouldFollowCombatGroupFormation() {
      LivingEntity target = this.getTarget();
      return this.getCurrentCommand() == NpcCommandType.NONE
         && !this.isGettingFood()
         && (target == null || !target.isAlive())
         && this.getCombatGroupCommander().isPresent();
   }

   public boolean hasHome() {
      return (Boolean)this.entityData.get(HOME_SET);
   }

   public BlockPos getHomePos() {
      return (BlockPos)this.entityData.get(HOME_POS);
   }

   public void setHome(BlockPos pos) {
      this.entityData.set(HOME_SET, true);
      this.entityData.set(HOME_POS, pos == null ? this.blockPosition() : pos);
   }

   public void clearHome() {
      this.entityData.set(HOME_SET, false);
      this.entityData.set(HOME_POS, BlockPos.ZERO);
   }

   public Optional<UUID> getFollowTarget() {
      return (Optional<UUID>)this.entityData.get(FOLLOW_TARGET);
   }

   public void setFollowTarget(UUID followTarget) {
      this.entityData.set(FOLLOW_TARGET, Optional.ofNullable(followTarget));
   }

   public Optional<Player> getFollowTargetPlayer() {
      return this.getFollowTarget().<Player>map(this.level()::getPlayerByUUID).filter(player -> player.isAlive() && !player.isSpectator());
   }

   public void toggleFollow(Player player) {
      if (this.getCurrentCommand() == NpcCommandType.FOLLOW) {
         this.clearCommand();
         if (this instanceof WorkerNpcEntity worker) {
            worker.setWorkState("idle");
         }

         player.displayClientMessage(Component.translatable("guistrings.npc.follow_disabled", new Object[]{this.getDisplayName()}), true);
      } else {
         this.clearCommand();
         this.setGettingFood(false);
         this.setFollowTarget(player.getUUID());
         this.setCurrentCommand(NpcCommandType.FOLLOW);
         if (this instanceof WorkerNpcEntity worker) {
            worker.setWorkState("follow");
         }

         player.displayClientMessage(Component.translatable("guistrings.npc.follow_enabled", new Object[]{this.getDisplayName()}), true);
      }
   }

   public ItemStack getOrdersStack() {
      return (ItemStack)this.entityData.get(ORDERS_STACK);
   }

   public void setOrdersStack(ItemStack stack) {
      this.entityData.set(ORDERS_STACK, stack == null ? ItemStack.EMPTY : stack.copy());
   }

   public ItemStack getUpkeepStack() {
      return (ItemStack)this.entityData.get(UPKEEP_STACK);
   }

   public void setUpkeepStack(ItemStack stack) {
      this.entityData.set(UPKEEP_STACK, stack == null ? ItemStack.EMPTY : stack.copy());
   }

   public ItemStack getFoodStack() {
      return (ItemStack)this.entityData.get(FOOD_STACK);
   }

   public void setFoodStack(ItemStack stack) {
      if (stack != null && !stack.isEmpty() && foodValue(stack) > 0) {
         ItemStack stored = stack.copy();
         stored.setCount(Math.min(stored.getCount(), 64));
         this.entityData.set(FOOD_STACK, stored);
         if (!this.level().isClientSide) {
            this.stopGettingFood();
         }
      } else {
         this.entityData.set(FOOD_STACK, ItemStack.EMPTY);
      }
   }

   public int getFoodRemaining() {
      return (Integer)this.entityData.get(FOOD_REMAINING);
   }

   public boolean isGettingFood() {
      return (Boolean)this.entityData.get(GETTING_FOOD);
   }

   boolean isFoodSupplyBlockedByDeployment() {
      if (this.isGuiInteractionPaused() || this.isPassenger() || this.getCurrentCommand() != NpcCommandType.NONE) {
         return true;
      } else if (this.hasActiveWeaponDeployment()) {
         return true;
      } else {
         LivingEntity combatTarget = this.getTarget();
         if (combatTarget != null && combatTarget.isAlive()) {
            return true;
         } else if (!this.getCombatGroupCommander().isPresent()
            && (!this.isCombatGroupCommander() || CommandBatonItem.getCombatGroupNpcs(this.getMainHandItem()).isEmpty())) {
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
      this.entityData.set(GETTING_FOOD, gettingFood);
   }

   public void setFoodRemaining(int amount) {
      this.entityData.set(FOOD_REMAINING, Math.max(0, amount));
   }

   public static int foodValue(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return 0;
      } else if (stack.is(Items.ROTTEN_FLESH) || stack.is(Items.POISONOUS_POTATO) || stack.is(Items.SPIDER_EYE)) {
         return 0;
      } else if (stack.is(Items.APPLE)) {
         return 3000;
      } else if (stack.is(Items.MUSHROOM_STEW)) {
         return 4500;
      } else {
         FoodProperties food = stack.getItem().getFoodProperties();
         if (food == null) {
            return 0;
         } else {
            int nutrition = food.getNutrition();
            return Math.max(0, (nutrition + nutrition * (int)(food.getSaturationModifier() * 2.0F)) * 350);
         }
      }
   }

   protected boolean tryAcceptFood(Player player, InteractionHand hand) {
      ItemStack offered = player.getItemInHand(hand);
      int value = foodValue(offered);
      UpkeepOrderData.UpkeepEntry entry = UpkeepOrderData.getEntry(this.getUpkeepStack()).orElse(null);
      int target = entry == null ? 6000 : entry.upkeepAmount();
      if (value > 0 && this.getFoodRemaining() < target) {
         if (!this.level().isClientSide && !player.getAbilities().instabuild) {
            offered.shrink(1);
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
         ItemStack requestedFood = entry == null ? ItemStack.EMPTY : UpkeepOrderData.getFoodTarget(this.getUpkeepStack());
         ItemStack storedFood = this.getFoodStack();
         if (!requestedFood.isEmpty() && !storedFood.isEmpty() && !ItemStack.isSameItemSameTags(requestedFood, storedFood)) {
            return false;
         } else {
            int gained = 0;

            for (int slot = 0; slot < handler.getSlots() && gained < needed; slot++) {
               ItemStack stack = handler.getStackInSlot(slot);
               if (foodValue(stack) > 0 && (requestedFood.isEmpty() || ItemStack.isSameItemSameTags(requestedFood, stack))) {
                  int maxCount = requestedFood.isEmpty() ? 1 : requestedFood.getCount();
                  ItemStack removed = handler.extractItem(slot, Math.min(maxCount, stack.getCount()), false);
                  if (!removed.isEmpty()) {
                     int value = foodValue(removed);
                     int consumeCount = Math.min(removed.getCount(), Math.max(1, (int)Math.ceil((double)needed / (double)Math.max(1, value))));
                     gained += value * consumeCount;
                     if (removed.getCount() > consumeCount) {
                        this.storeFood(removed.copyWithCount(removed.getCount() - consumeCount));
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
      if (!food.isEmpty() && foodValue(food) > 0) {
         ItemStack current = this.getFoodStack();
         if (current.isEmpty()) {
            this.setFoodStack(food);
         } else if (ItemStack.isSameItemSameTags(current, food)) {
            current.grow(Math.min(food.getCount(), 64 - current.getCount()));
            this.setFoodStack(current);
         }
      }
   }

   private void updateFoodSupply() {
      if (!this.level().isClientSide && !this.isSleeping()) {
         if (this.foodSupplyRetryTicks > 0) {
            this.foodSupplyRetryTicks--;
         }

         if (this.getFoodRemaining() > 0) {
            this.setFoodRemaining(this.getFoodRemaining() - 1);
         }

         ItemStack carriedFood = this.getFoodStack();
         if (!carriedFood.isEmpty() && foodValue(carriedFood) > 0) {
            if (this.getFoodRemaining() <= 0) {
               this.consumeFoodStack();
            }

            this.stopGettingFood();
         } else if (this.isFoodSupplyBlockedByDeployment()) {
            this.stopGettingFood();
         } else if (this.getCurrentCommand() != NpcCommandType.NONE) {
            this.stopGettingFood();
         } else {
            LivingEntity combatTarget = this.getTarget();
            if (combatTarget != null && combatTarget.isAlive()) {
               this.stopGettingFood();
            } else if (this.getFoodRemaining() > 0) {
               this.stopGettingFood();
            } else {
               UpkeepOrderData.UpkeepEntry entry = UpkeepOrderData.getEntry(this.getUpkeepStack()).orElse(null);
               BlockPos supplyPos = entry == null ? (this.hasTownHall() ? this.getTownHallPos() : null) : entry.pos();
               if (supplyPos != null && this.isSameSupplyDimension(entry)) {
                  if (entry == null && this.level().hasChunkAt(supplyPos) && !(this.level().getBlockEntity(supplyPos) instanceof TownHallBlockEntity)) {
                     this.clearTownHall();
                     this.stopGettingFood();
                  } else {
                     if (this.foodSupplyRetryTicks <= 0) {
                        this.setGettingFood(true);
                     }

                     if (this.isGettingFood()) {
                        double distance = this.distanceToSqr(
                           (double)supplyPos.getX() + 0.5, (double)supplyPos.getY() + 0.5, (double)supplyPos.getZ() + 0.5
                        );
                        if (distance > 9.0) {
                           this.getNavigation()
                              .moveTo(
                                 (double)supplyPos.getX() + 0.5,
                                 (double)supplyPos.getY() + 0.5,
                                 (double)supplyPos.getZ() + 0.5,
                                 this instanceof WorkerNpcEntity ? this.playerSprintNavigationSpeed() : this.playerWalkNavigationSpeed()
                              );
                           this.getLookControl()
                              .setLookAt((double)supplyPos.getX() + 0.5, (double)supplyPos.getY() + 0.5, (double)supplyPos.getZ() + 0.5);
                        } else {
                           BlockEntity blockEntity = this.level().getBlockEntity(supplyPos);
                           Direction side = entry == null ? Direction.DOWN : entry.side();
                           if (blockEntity instanceof TownHallBlockEntity townHall) {
                              ItemStack removed = townHall.extractFoodLike(1);
                              if (!removed.isEmpty()) {
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
         this.getNavigation().stop();
      }
   }

   private boolean consumeFoodStack() {
      ItemStack food = this.getFoodStack();
      if (food.isEmpty()) {
         return false;
      } else {
         int value = foodValue(food);
         if (value <= 0) {
            this.setFoodStack(ItemStack.EMPTY);
            return false;
         } else {
            ItemStack remaining = food.copy();
            remaining.shrink(1);
            this.setFoodStack(remaining);
            this.setFoodRemaining(value);
            return true;
         }
      }
   }

   private boolean isSameSupplyDimension(UpkeepOrderData.UpkeepEntry entry) {
      return entry == null || entry.dimension().isEmpty() || entry.dimension().equals(this.level().dimension().location().toString());
   }

   public boolean hasTownHall() {
      return (Boolean)this.entityData.get(TOWN_HALL_SET);
   }

   public BlockPos getTownHallPos() {
      return (BlockPos)this.entityData.get(TOWN_HALL_POS);
   }

   public void handleTownHallBroadcast(BlockPos pos) {
      if (pos != null) {
         if (!this.hasTownHall() || this.closerThanCurrentTownHall(pos)) {
            this.entityData.set(TOWN_HALL_SET, true);
            this.entityData.set(TOWN_HALL_POS, pos.immutable());
         }
      }
   }

   public void clearTownHall() {
      this.entityData.set(TOWN_HALL_SET, false);
      this.entityData.set(TOWN_HALL_POS, BlockPos.ZERO);
   }

   public void aiStep() {
      if (this.isGuiInteractionPaused()) {
         this.freezeForGui();
      } else {
         super.aiStep();
         this.updateOwnedMountedMovement();
      }
   }

   public double getMyRidingOffset() {
      return this.getVehicle() instanceof Mob ? super.getMyRidingOffset() - 0.45 : super.getMyRidingOffset();
   }

   public void die(DamageSource damageSource) {
      if (!this.dead) {
         TownHallBlockEntity townHall = TownHallBlockEntity.findForDeath(this);
         boolean deferred = townHall == null
            && this.hasTownHall()
            && this.level() instanceof ServerLevel server
            && server.getChunkSource().getChunkNow(this.getTownHallPos().getX() >> 4, this.getTownHallPos().getZ() >> 4) == null;
         CompoundTag details = townHall == null && !deferred ? null : TownHallBlockEntity.captureDeathDetails(this, damageSource);
         if (townHall != null) {
            details.putLong("TownHallPos", townHall.getBlockPos().asLong());
         }

         super.die(damageSource);
         if (this.dead) {
            this.recordTownHallDeath(townHall, damageSource, details);
            if (deferred && this.level() instanceof ServerLevel serverx) {
               TownHallDeathData.get(serverx).enqueue(this.getTownHallPos(), TownHallBlockEntity.createDeathEntry(this, damageSource, details));
            }
         }
      }
   }

   private boolean closerThanCurrentTownHall(BlockPos pos) {
      BlockPos current = this.getTownHallPos();
      return BlockPos.ZERO.equals(current)
         ? true
         : this.distanceToSqr((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5)
            < this.distanceToSqr((double)current.getX() + 0.5, (double)current.getY() + 0.5, (double)current.getZ() + 0.5);
   }

   private void recordTownHallDeath(TownHallBlockEntity townHall, DamageSource damageSource, CompoundTag details) {
      if (!this.level().isClientSide && townHall != null && !townHall.isRemoved() && details != null) {
         townHall.recordNpcDeath(this, damageSource, details);
      }
   }

   private void updateOwnedMountedMovement() {
      Entity vehicle = this.getVehicle();
      if (this.shouldFollowCombatGroupFormation()) {
         Optional<Vec3> formationPosition = this.getCombatGroupFormationPosition();
         if (formationPosition.isPresent()) {
            Vec3 destination = formationPosition.get();
            if (vehicle instanceof ChestPushcartEntity cart) {
               this.driveCombatGroupCart(cart, destination);
               return;
            }

            label77: {
               if (vehicle instanceof Mob mount && mount.position().distanceToSqr(destination) > 2.25) {
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
               LivingEntity target = this.getTarget();
               if (target != null && target.isAlive()) {
                  double stopDistance = "archer".equals(this.getNpcSubtype()) ? 12.0 : 2.5;
                  if (mount.distanceToSqr(target) > stopDistance * stopDistance) {
                     this.driveMountToward(mount, target.position());
                  } else {
                     this.stopMount(mount, 0.55);
                     this.faceMountedTarget(mount, target.position());
                  }
               } else {
                  if (this.getCurrentCommand() == NpcCommandType.MOVE) {
                     BlockPos targetPos = this.getCommandTarget();
                     if (!BlockPos.ZERO.equals(targetPos)) {
                        Vec3 destinationx = Vec3.atBottomCenterOf(targetPos);
                        if (mount.distanceToSqr(destinationx) > 4.0) {
                           this.driveMountToward(mount, destinationx);
                        } else {
                           this.stopMount(mount, 0.4);
                           this.completeMoveCommand();
                        }

                        return;
                     }
                  }

                  if (this.updateMountedHoldMovement()) {
                     return;
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

   protected boolean updateMountedFollowMovement() {
      if (this.getCurrentCommand() == NpcCommandType.FOLLOW && !this.isGuiInteractionPaused()) {
         LivingEntity combatTarget = this.getTarget();
         if (!this.followCombatRecallActive && combatTarget != null && combatTarget.isAlive()) {
            return false;
         } else {
            Entity vehicle = this.getVehicle();
            if (vehicle instanceof Mob mount) {
               Player player = this.getFollowTargetPlayer().orElse(null);
               if (player == null) {
                  this.stopMount(mount, 0.4);
                  return true;
               }

               double distanceSq = mount.distanceToSqr(player);
               if (distanceSq > 100.0) {
                  this.driveMountToward(mount, player.position());
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
         LivingEntity combatTarget = this.getTarget();
         if (combatTarget != null && combatTarget.isAlive()) {
            return false;
         } else {
            Entity vehicle = this.getVehicle();
            if (vehicle instanceof Mob mount) {
               BlockPos target = this.getCommandTarget();
               if (BlockPos.ZERO.equals(target)) {
                  this.clearCommand();
                  this.stopMount(mount, 0.4);
                  return true;
               }

               Vec3 destination = Vec3.atBottomCenterOf(target);
               if (mount.distanceToSqr(destination) > 4.0) {
                  this.driveMountToward(mount, destination);
               } else {
                  this.stopMount(mount, 0.4);
                  this.completeMoveCommand();
               }

               return true;
            }

            return false;
         }
      } else {
         return false;
      }
   }

   /** Rides back to the held position once there is nothing to fight. */
   protected boolean updateMountedHoldMovement() {
      if (this.isHoldingPosition() && !this.isGuiInteractionPaused()) {
         LivingEntity combatTarget = this.getTarget();
         if (combatTarget != null && combatTarget.isAlive()) {
            return false;
         } else if (this.getVehicle() instanceof Mob mount) {
            Vec3 destination = Vec3.atBottomCenterOf(this.getCommandTarget());
            if (mount.distanceToSqr(destination) > 9.0) {
               this.driveMountToward(mount, destination);
            } else {
               this.stopMount(mount, 0.4);
            }

            return true;
         } else {
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

      Vec3 delta = destination.subtract(cart.position());
      if (delta.horizontalDistanceSqr() <= 2.25) {
         if (cart instanceof BatteringRamEntity ram) {
            ram.setNpcFormationInput(this, 0.0F, 0.0F);
         } else {
            cart.setVehicleInput(0.0F, 0.0F);
         }
      } else {
         float desiredYaw = (float)Math.toDegrees(Math.atan2(-delta.x, delta.z));
         float yawError = Mth.wrapDegrees(desiredYaw - cart.getYRot());
         float turn = Mth.clamp(-yawError / 28.0F, -1.0F, 1.0F);
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
      Vec3 delta = destination.subtract(mount.position());
      Vec3 horizontal = new Vec3(delta.x, 0.0, delta.z);
      if (!(horizontal.lengthSqr() < 1.0E-4)) {
         double speed = 0.22;
         AttributeInstance speedAttribute = mount.getAttribute(Attributes.MOVEMENT_SPEED);
         if (speedAttribute != null) {
            speed = Math.max(speed, speedAttribute.getValue() * 0.65);
         }

         Vec3 direction = horizontal.normalize();
         Vec3 current = mount.getDeltaMovement();
         mount.setDeltaMovement(direction.x * speed, current.y, direction.z * speed);
         mount.getNavigation().stop();
         this.faceDirection(mount, direction);
      }
   }

   private void stopMount(Mob mount, double damping) {
      mount.getNavigation().stop();
      mount.setDeltaMovement(mount.getDeltaMovement().multiply(damping, 1.0, damping));
   }

   private void faceMountedTarget(Mob mount, Vec3 destination) {
      Vec3 delta = destination.subtract(mount.position());
      Vec3 horizontal = new Vec3(delta.x, 0.0, delta.z);
      if (horizontal.lengthSqr() >= 1.0E-4) {
         this.faceDirection(mount, horizontal.normalize());
      }
   }

   private void faceDirection(Mob mount, Vec3 direction) {
      float yaw = (float)(Math.atan2(direction.z, direction.x) * 180.0F / (float)Math.PI) - 90.0F;
      mount.setYRot(yaw);
      mount.setYBodyRot(yaw);
      mount.setYHeadRot(yaw);
      this.setYRot(yaw);
      this.setYBodyRot(yaw);
      this.setYHeadRot(yaw);
   }

   private static NpcCommandType parseCommand(String name) {
      try {
         return NpcCommandType.valueOf(name);
      } catch (IllegalArgumentException var2) {
         return NpcCommandType.NONE;
      }
   }

   private static record Waypoint(BlockPos pos, float yaw) {
   }
}
