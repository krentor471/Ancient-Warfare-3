package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.siege.SiegeWeaponControl;
import net.shadowmage.ancientwarfare.core.item.CatapultAmmoItem;
import net.shadowmage.ancientwarfare.core.item.CatapultAmmoType;
import net.shadowmage.ancientwarfare.core.item.CatapultMobileItem;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.object.PlayState;

public class CatapultMobileEntity extends ChestPushcartEntity implements SiegeWeaponControl {
   private static final float MAX_TURRET_YAW = 45.0F;
   protected static final EntityDataAccessor<Float> TURRET_YAW = SynchedEntityData.defineId(CatapultMobileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> FIRE_ANIMATION_TICKS = SynchedEntityData.defineId(
      CatapultMobileEntity.class, EntityDataSerializers.INT
   );
   private static final EntityDataAccessor<Float> VELOCITY_BONUS = SynchedEntityData.defineId(CatapultMobileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> FIXED_TURRET = SynchedEntityData.defineId(CatapultMobileEntity.class, EntityDataSerializers.BOOLEAN);
   private static final Component MENU_TITLE = Component.translatable("container.ancientwarfare.catapult_mobile");
   private static final Component FIXED_MENU_TITLE = Component.translatable("container.ancientwarfare.catapult_mobile_fixed");
   private static final RawAnimation WHEEL_ROLL = RawAnimation.begin().thenLoop("animation.catapult_wheels_roll");
   private static final RawAnimation FIRE = RawAnimation.begin().thenPlay("animation.catapult_fire2");
   private static final int FIRE_COOLDOWN_TICKS = 60;
   public static final int FIRE_ANIMATION_LENGTH_TICKS = 27;
   public static final int RELEASE_ANIMATION_TICKS = 5;
   public static final int RELOAD_ANIMATION_TICKS = 22;
   private static final float MAX_VELOCITY_BONUS = 0.3F;
   private static final float BASE_LAUNCH_SPEED = 1.65F;
   private static final double LAUNCH_VERTICAL_COMPONENT = 0.55;
   private static final double PROJECTILE_DRAG = 0.99;
   private static final float AI_MIN_LAUNCH_SPEED = 0.35F;
   private static final int AIM_MAX_FLIGHT_TICKS = 200;
   private int fireCooldown;
   private int previousFireAnimationTicks;

   public CatapultMobileEntity(EntityType<? extends CatapultMobileEntity> type, Level level) {
      super(type, level);
   }

   @Override
   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(TURRET_YAW, 0.0F);
      this.entityData.define(FIRE_ANIMATION_TICKS, 0);
      this.entityData.define(VELOCITY_BONUS, 0.0F);
      this.entityData.define(FIXED_TURRET, false);
   }

   public float getTurretYaw() {
      return (Float)this.entityData.get(TURRET_YAW);
   }

   public boolean hasFixedTurret() {
      return (Boolean)this.entityData.get(FIXED_TURRET);
   }

   public void setFixedTurret(boolean fixedTurret) {
      this.setFixedTurretRaw(fixedTurret);
   }

   protected void setFixedTurretRaw(boolean fixedTurret) {
      this.entityData.set(FIXED_TURRET, fixedTurret);
      if (fixedTurret) {
         this.entityData.set(TURRET_YAW, 0.0F);
      }

      this.refreshDimensions();
   }

   public void adjustTurret(float yawDelta) {
      this.entityData.set(TURRET_YAW, this.hasFixedTurret() ? 0.0F : this.clampTurretYaw(this.getTurretYaw() + yawDelta));
   }

   protected float clampTurretYaw(float yaw) {
      return clamp(yaw, -45.0F, 45.0F);
   }

   public float getVelocityBonus() {
      return (Float)this.entityData.get(VELOCITY_BONUS);
   }

   public int getVelocityBonusPercent() {
      return Math.round(this.getVelocityBonus() * 100.0F);
   }

   public void adjustLaunchVelocity(float bonusDelta) {
      this.entityData.set(VELOCITY_BONUS, clamp(this.getVelocityBonus() + bonusDelta, 0.0F, 0.3F));
   }

   public int getFireAnimationTicks() {
      return (Integer)this.entityData.get(FIRE_ANIMATION_TICKS);
   }

   protected float getLaunchYawDegrees() {
      return this.getYRot() + this.getTurretYaw();
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   @Override
   public void tick() {
      super.tick();
      if (this.fireCooldown > 0) {
         this.fireCooldown--;
      }

      if (this.getFireAnimationTicks() > 0) {
         this.entityData.set(FIRE_ANIMATION_TICKS, this.getFireAnimationTicks() - 1);
      }
   }

   public boolean tryFire() {
      return this.tryFire(this.projectileSpeed(), 0.75F, this.getLaunchYawDegrees());
   }

   private boolean tryFire(float launchSpeed, float inaccuracy, float launchWorldYaw) {
      if (!this.level().isClientSide && this.fireCooldown <= 0) {
         CatapultMobileEntity.AmmoSlot ammoSlot = this.findLoadedAmmo();
         if (ammoSlot == null) {
            return false;
         } else {
            ItemStack stack = this.getItem(ammoSlot.slot());
            stack.shrink(1);
            this.setItem(ammoSlot.slot(), stack);
            this.setChanged();
            LivingEntity shooter = this.getControllingPassenger();
            CatapultShotEntity shot = new CatapultShotEntity(this.level(), shooter, ammoSlot.ammoType());
            double yaw = Math.toRadians((double)launchWorldYaw);
            Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw)).normalize();
            Vec3 muzzle = this.position().add(forward.scale(this.muzzleForwardOffset())).add(0.0, this.muzzleHeight(), 0.0);
            shot.moveTo(muzzle.x, muzzle.y, muzzle.z, launchWorldYaw, -24.0F);
            shot.setBlockCollisionGraceOrigin(muzzle);
            shot.shoot(forward.x, 0.55, forward.z, launchSpeed, inaccuracy);
            this.level().addFreshEntity(shot);
            this.fireCooldown = 60;
            this.entityData.set(FIRE_ANIMATION_TICKS, 27);
            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean canBeOperatedBy(PathfinderMob engineer) {
      return SiegeWeaponNpcControl.canOperate(this, engineer);
   }

   @Override
   public boolean beginOperation(PathfinderMob engineer) {
      return SiegeWeaponNpcControl.beginOperation(this, engineer);
   }

   @Override
   public void stopOperation(PathfinderMob engineer) {
      SiegeWeaponNpcControl.stopOperation(this, engineer);
   }

   @Override
   public boolean canAttackTarget(LivingEntity target) {
      if (!SiegeWeaponNpcControl.canAttack(this, target, 8.0, 112.0)) {
         return false;
      } else {
         float worldYaw = SiegeWeaponNpcControl.worldYawTo(target, this);
         boolean fixedEmplacement = this.isFixedEmplacement();
         return fixedEmplacement && Math.abs(Mth.wrapDegrees(worldYaw - this.getYRot())) > 4.0F
            ? false
            : this.findLaunchSolution(target, fixedEmplacement ? this.getYRot() : worldYaw) != null;
      }
   }

   @Override
   public double minimumAttackRange() {
      return 8.0;
   }

   @Override
   public double maximumAttackRange() {
      return 112.0;
   }

   @Override
   public boolean isMobile() {
      return !(this instanceof CatapultTurretStandEntity) && !(this instanceof TrebuchetFixedEntity);
   }

   private boolean isFixedEmplacement() {
      return this instanceof TrebuchetFixedEntity && !(this instanceof TrebuchetTurretEntity)
         || this instanceof CatapultTurretStandEntity && this.hasFixedTurret();
   }

   @Override
   public void aimAt(PathfinderMob engineer, LivingEntity target) {
      float launchWorldYaw = SiegeWeaponNpcControl.worldYawTo(target, this);
      if (this.isFixedEmplacement()) {
         this.entityData.set(TURRET_YAW, 0.0F);
      } else if (!(this instanceof CatapultTurretStandEntity) && !(this instanceof TrebuchetTurretEntity)) {
         SiegeWeaponNpcControl.faceVehicle(this, launchWorldYaw);
         this.entityData.set(TURRET_YAW, 0.0F);
      } else {
         this.entityData.set(TURRET_YAW, SiegeWeaponNpcControl.localYaw(launchWorldYaw, this));
      }

      CatapultMobileEntity.LaunchSolution solution = this.findLaunchSolution(target, launchWorldYaw);
      if (solution != null) {
         this.entityData.set(VELOCITY_BONUS, clamp(solution.speed() / 1.65F - 1.0F, 0.0F, 0.3F));
      } else {
         double distance = Math.sqrt(this.distanceToSqr(target));
         this.entityData.set(VELOCITY_BONUS, distance > 72.0 ? 0.3F : 0.0F);
      }
   }

   private CatapultMobileEntity.LaunchSolution findLaunchSolution(LivingEntity target, float worldYaw) {
      if (target != null && target.isAlive()) {
         double yaw = Math.toRadians((double)worldYaw);
         Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw)).normalize();
         Vec3 muzzle = this.position().add(forward.scale(this.muzzleForwardOffset())).add(0.0, this.muzzleHeight(), 0.0);
         AABB targetBox = target.getBoundingBox().inflate(0.35);
         Vec3 toTarget = targetBox.getCenter().subtract(muzzle);
         double forwardDistance = toTarget.x * forward.x + toTarget.z * forward.z;
         if (forwardDistance <= 0.0) {
            return null;
         } else {
            double launchVectorLength = Math.sqrt(1.3025);
            float maximumSpeed = 2.145F;
            double dragPower = 1.0;
            double dragDistance = 0.0;

            for (int flightTicks = 1; flightTicks <= 200; flightTicks++) {
               dragDistance += dragPower;
               dragPower *= 0.99;
               double horizontalSpeed = forwardDistance / dragDistance;
               float speed = (float)(horizontalSpeed * launchVectorLength);
               if (!(speed < 0.35F) && !(speed > maximumSpeed)) {
                  double verticalSpeed = (double)speed * 0.55 / launchVectorLength;
                  double verticalDistance = verticalSpeed * dragDistance
                     - (double)this.projectileGravity() / 0.010000000000000009 * ((double)flightTicks - dragDistance);
                  Vec3 current = muzzle.add(forward.scale(horizontalSpeed * dragDistance)).add(0.0, verticalDistance, 0.0);
                  double previousDragDistance = dragDistance - dragPower / 0.99;
                  int previousTicks = flightTicks - 1;
                  double previousVerticalDistance = verticalSpeed * previousDragDistance
                     - (double)this.projectileGravity() / 0.010000000000000009 * ((double)previousTicks - previousDragDistance);
                  Vec3 previous = muzzle.add(forward.scale(horizontalSpeed * previousDragDistance)).add(0.0, previousVerticalDistance, 0.0);
                  if (!targetBox.clip(previous, current).isEmpty()
                     && this.hasClearLaunchPath(muzzle, forward, horizontalSpeed, verticalSpeed, flightTicks, targetBox)) {
                     return new CatapultMobileEntity.LaunchSolution(speed);
                  }
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   private boolean hasClearLaunchPath(Vec3 muzzle, Vec3 forward, double horizontalSpeed, double verticalSpeed, int flightTicks, AABB targetBox) {
      Vec3 position = muzzle;
      Vec3 velocity = forward.scale(horizontalSpeed).add(0.0, verticalSpeed, 0.0);
      double graceDistanceSqr = 36.0;

      for (int tick = 0; tick < flightTicks; tick++) {
         Vec3 next = position.add(velocity);
         Vec3 targetHit = (Vec3)targetBox.clip(position, next).orElse(null);
         Vec3 clipStart = position;
         if (muzzle.distanceToSqr(position) < graceDistanceSqr) {
            if (muzzle.distanceToSqr(next) <= graceDistanceSqr) {
               clipStart = next;
            } else {
               clipStart = SiegeWeaponNpcControl.segmentSphereExit(position, next, muzzle, 6.0);
            }
         }

         BlockHitResult blockHit = clipStart.distanceToSqr(next) <= 1.0E-8
            ? BlockHitResult.miss(next, Direction.UP, BlockPos.containing(next))
            : this.level().clip(new ClipContext(clipStart, next, Block.COLLIDER, Fluid.NONE, this));
         if (targetHit != null) {
            return blockHit.getType() == Type.MISS || blockHit.getLocation().distanceToSqr(position) >= targetHit.distanceToSqr(position);
         }

         if (blockHit.getType() != Type.MISS) {
            return false;
         }

         position = next;
         velocity = velocity.scale(0.99).add(0.0, (double)(-this.projectileGravity()), 0.0);
      }

      return false;
   }

   @Override
   public float projectileSpeed() {
      return 1.65F * (1.0F + this.getVelocityBonus());
   }

   @Override
   public double muzzleHeight() {
      return 0.9375;
   }

   protected double muzzleForwardOffset() {
      return -2.0;
   }

   @Override
   public boolean fireAt(PathfinderMob engineer, LivingEntity target) {
      SiegeWeaponNpcControl.supplyFactionAmmo(engineer, this, AWCoreRegistryAccess.stack(AWCoreIds.AMMO_STONE_SHOT_10));
      if (!this.canAttackTarget(target)) {
         return false;
      } else {
         float targetWorldYaw = SiegeWeaponNpcControl.worldYawTo(target, this);
         this.aimAt(engineer, target);
         CatapultMobileEntity.LaunchSolution solution = this.findLaunchSolution(target, targetWorldYaw);
         return solution != null && this.tryFire(solution.speed(), 0.05F, targetWorldYaw);
      }
   }

   private CatapultMobileEntity.AmmoSlot findLoadedAmmo() {
      for (int i = 0; i < this.getContainerSize(); i++) {
         ItemStack stack = this.getItem(i);
         CatapultAmmoType ammoType = CatapultAmmoItem.ammoType(stack);
         if (ammoType != null && this.canFireAmmo(ammoType)) {
            return new CatapultMobileEntity.AmmoSlot(i, ammoType);
         }
      }

      return null;
   }

   protected boolean canFireAmmo(CatapultAmmoType ammoType) {
      return ammoType.weight() == 10 || ammoType.weight() == 15;
   }

   public boolean canPlaceItem(int slot, ItemStack stack) {
      CatapultAmmoType ammoType = CatapultAmmoItem.ammoType(stack);
      return ammoType == null || this.canFireAmmo(ammoType);
   }

   @Override
   protected float maxHealthForLevel(int level) {
      return VehicleDurability.attackHealth(level);
   }

   @Override
   protected float armorForLevel(int level) {
      return VehicleDurability.attackArmor(level);
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
   }

   private PlayState wheelAnimation(AnimationState<CatapultMobileEntity> state) {
      if (this.getDeltaMovement().horizontalDistanceSqr() > 4.0E-4) {
         state.setAnimation(WHEEL_ROLL);
         return PlayState.CONTINUE;
      } else {
         return PlayState.STOP;
      }
   }

   private PlayState fireAnimation(AnimationState<CatapultMobileEntity> state) {
      int fireAnimationTicks = this.getFireAnimationTicks();
      if (fireAnimationTicks > 0) {
         if (this.previousFireAnimationTicks <= 0) {
            state.getController().forceAnimationReset();
         }

         this.previousFireAnimationTicks = fireAnimationTicks;
         state.setAnimation(FIRE);
         return PlayState.CONTINUE;
      } else {
         this.previousFireAnimationTicks = 0;
         return PlayState.STOP;
      }
   }

   @Override
   protected Component getMenuTitle() {
      return this.hasFixedTurret() ? FIXED_MENU_TITLE : MENU_TITLE;
   }

   @Override
   protected Vec3 getPassengerOffset() {
      return this.hasFixedTurret() ? new Vec3(0.0, 0.55, 1.2) : new Vec3(0.0, 0.65, 0.8);
   }

   @Override
   protected void positionRider(Entity passenger, MoveFunction moveFunction) {
      if (this.hasPassenger(passenger)) {
         float riderYaw = this.getYRot() + (this.hasFixedTurret() ? 0.0F : this.getTurretYaw());
         Vec3 offset = this.getPassengerOffset().yRot(-riderYaw * (float) (Math.PI / 180.0));
         moveFunction.accept(passenger, this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);
      }
   }

   @Override
   protected ItemStack createPackedItem() {
      ItemStack stack = new ItemStack(CatapultMobileItem.itemForMaterialLevel(this.getMaterialLevel(), this.hasFixedTurret()));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.getOrCreateTag().put("CatapultMobileData", tag);
      return stack;
   }

   @Override
   public void loadCartData(CompoundTag tag) {
      super.loadCartData(tag);
      this.setFixedTurret(tag.getBoolean("FixedTurret"));
      this.entityData.set(TURRET_YAW, this.hasFixedTurret() ? 0.0F : tag.getFloat("TurretYaw"));
      this.entityData.set(VELOCITY_BONUS, clamp(tag.getFloat("VelocityBonus"), 0.0F, 0.3F));
   }

   @Override
   public void saveCartData(CompoundTag tag) {
      super.saveCartData(tag);
      tag.putFloat("TurretYaw", this.getTurretYaw());
      tag.putFloat("VelocityBonus", this.getVelocityBonus());
      tag.putBoolean("FixedTurret", this.hasFixedTurret());
   }

   private static record AmmoSlot(int slot, CatapultAmmoType ammoType) {
   }

   private static record LaunchSolution(float speed) {
   }
}
