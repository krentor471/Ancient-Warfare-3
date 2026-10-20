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
   protected static final EntityDataAccessor<Float> TURRET_YAW = SynchedEntityData.m_135353_(CatapultMobileEntity.class, EntityDataSerializers.f_135029_);
   private static final EntityDataAccessor<Integer> FIRE_ANIMATION_TICKS = SynchedEntityData.m_135353_(
      CatapultMobileEntity.class, EntityDataSerializers.f_135028_
   );
   private static final EntityDataAccessor<Float> VELOCITY_BONUS = SynchedEntityData.m_135353_(CatapultMobileEntity.class, EntityDataSerializers.f_135029_);
   private static final EntityDataAccessor<Boolean> FIXED_TURRET = SynchedEntityData.m_135353_(CatapultMobileEntity.class, EntityDataSerializers.f_135035_);
   private static final Component MENU_TITLE = Component.m_237115_("container.ancientwarfare.catapult_mobile");
   private static final Component FIXED_MENU_TITLE = Component.m_237115_("container.ancientwarfare.catapult_mobile_fixed");
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
   protected void m_8097_() {
      super.m_8097_();
      this.f_19804_.m_135372_(TURRET_YAW, 0.0F);
      this.f_19804_.m_135372_(FIRE_ANIMATION_TICKS, 0);
      this.f_19804_.m_135372_(VELOCITY_BONUS, 0.0F);
      this.f_19804_.m_135372_(FIXED_TURRET, false);
   }

   public float getTurretYaw() {
      return (Float)this.f_19804_.m_135370_(TURRET_YAW);
   }

   public boolean hasFixedTurret() {
      return (Boolean)this.f_19804_.m_135370_(FIXED_TURRET);
   }

   public void setFixedTurret(boolean fixedTurret) {
      this.setFixedTurretRaw(fixedTurret);
   }

   protected void setFixedTurretRaw(boolean fixedTurret) {
      this.f_19804_.m_135381_(FIXED_TURRET, fixedTurret);
      if (fixedTurret) {
         this.f_19804_.m_135381_(TURRET_YAW, 0.0F);
      }

      this.m_6210_();
   }

   public void adjustTurret(float yawDelta) {
      this.f_19804_.m_135381_(TURRET_YAW, this.hasFixedTurret() ? 0.0F : this.clampTurretYaw(this.getTurretYaw() + yawDelta));
   }

   protected float clampTurretYaw(float yaw) {
      return clamp(yaw, -45.0F, 45.0F);
   }

   public float getVelocityBonus() {
      return (Float)this.f_19804_.m_135370_(VELOCITY_BONUS);
   }

   public int getVelocityBonusPercent() {
      return Math.round(this.getVelocityBonus() * 100.0F);
   }

   public void adjustLaunchVelocity(float bonusDelta) {
      this.f_19804_.m_135381_(VELOCITY_BONUS, clamp(this.getVelocityBonus() + bonusDelta, 0.0F, 0.3F));
   }

   public int getFireAnimationTicks() {
      return (Integer)this.f_19804_.m_135370_(FIRE_ANIMATION_TICKS);
   }

   protected float getLaunchYawDegrees() {
      return this.m_146908_() + this.getTurretYaw();
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   @Override
   public void m_8119_() {
      super.m_8119_();
      if (this.fireCooldown > 0) {
         this.fireCooldown--;
      }

      if (this.getFireAnimationTicks() > 0) {
         this.f_19804_.m_135381_(FIRE_ANIMATION_TICKS, this.getFireAnimationTicks() - 1);
      }
   }

   public boolean tryFire() {
      return this.tryFire(this.projectileSpeed(), 0.75F, this.getLaunchYawDegrees());
   }

   private boolean tryFire(float launchSpeed, float inaccuracy, float launchWorldYaw) {
      if (!this.m_9236_().f_46443_ && this.fireCooldown <= 0) {
         CatapultMobileEntity.AmmoSlot ammoSlot = this.findLoadedAmmo();
         if (ammoSlot == null) {
            return false;
         } else {
            ItemStack stack = this.m_8020_(ammoSlot.slot());
            stack.m_41774_(1);
            this.m_6836_(ammoSlot.slot(), stack);
            this.m_6596_();
            LivingEntity shooter = this.m_6688_();
            CatapultShotEntity shot = new CatapultShotEntity(this.m_9236_(), shooter, ammoSlot.ammoType());
            double yaw = Math.toRadians((double)launchWorldYaw);
            Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw)).m_82541_();
            Vec3 muzzle = this.m_20182_().m_82549_(forward.m_82490_(this.muzzleForwardOffset())).m_82520_(0.0, this.muzzleHeight(), 0.0);
            shot.m_7678_(muzzle.f_82479_, muzzle.f_82480_, muzzle.f_82481_, launchWorldYaw, -24.0F);
            shot.setBlockCollisionGraceOrigin(muzzle);
            shot.m_6686_(forward.f_82479_, 0.55, forward.f_82481_, launchSpeed, inaccuracy);
            this.m_9236_().m_7967_(shot);
            this.fireCooldown = 60;
            this.f_19804_.m_135381_(FIRE_ANIMATION_TICKS, 27);
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
         return fixedEmplacement && Math.abs(Mth.m_14177_(worldYaw - this.m_146908_())) > 4.0F
            ? false
            : this.findLaunchSolution(target, fixedEmplacement ? this.m_146908_() : worldYaw) != null;
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
         this.f_19804_.m_135381_(TURRET_YAW, 0.0F);
      } else if (!(this instanceof CatapultTurretStandEntity) && !(this instanceof TrebuchetTurretEntity)) {
         SiegeWeaponNpcControl.faceVehicle(this, launchWorldYaw);
         this.f_19804_.m_135381_(TURRET_YAW, 0.0F);
      } else {
         this.f_19804_.m_135381_(TURRET_YAW, SiegeWeaponNpcControl.localYaw(launchWorldYaw, this));
      }

      CatapultMobileEntity.LaunchSolution solution = this.findLaunchSolution(target, launchWorldYaw);
      if (solution != null) {
         this.f_19804_.m_135381_(VELOCITY_BONUS, clamp(solution.speed() / 1.65F - 1.0F, 0.0F, 0.3F));
      } else {
         double distance = Math.sqrt(this.m_20280_(target));
         this.f_19804_.m_135381_(VELOCITY_BONUS, distance > 72.0 ? 0.3F : 0.0F);
      }
   }

   private CatapultMobileEntity.LaunchSolution findLaunchSolution(LivingEntity target, float worldYaw) {
      if (target != null && target.m_6084_()) {
         double yaw = Math.toRadians((double)worldYaw);
         Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw)).m_82541_();
         Vec3 muzzle = this.m_20182_().m_82549_(forward.m_82490_(this.muzzleForwardOffset())).m_82520_(0.0, this.muzzleHeight(), 0.0);
         AABB targetBox = target.m_20191_().m_82400_(0.35);
         Vec3 toTarget = targetBox.m_82399_().m_82546_(muzzle);
         double forwardDistance = toTarget.f_82479_ * forward.f_82479_ + toTarget.f_82481_ * forward.f_82481_;
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
                  Vec3 current = muzzle.m_82549_(forward.m_82490_(horizontalSpeed * dragDistance)).m_82520_(0.0, verticalDistance, 0.0);
                  double previousDragDistance = dragDistance - dragPower / 0.99;
                  int previousTicks = flightTicks - 1;
                  double previousVerticalDistance = verticalSpeed * previousDragDistance
                     - (double)this.projectileGravity() / 0.010000000000000009 * ((double)previousTicks - previousDragDistance);
                  Vec3 previous = muzzle.m_82549_(forward.m_82490_(horizontalSpeed * previousDragDistance)).m_82520_(0.0, previousVerticalDistance, 0.0);
                  if (!targetBox.m_82371_(previous, current).isEmpty()
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
      Vec3 velocity = forward.m_82490_(horizontalSpeed).m_82520_(0.0, verticalSpeed, 0.0);
      double graceDistanceSqr = 36.0;

      for (int tick = 0; tick < flightTicks; tick++) {
         Vec3 next = position.m_82549_(velocity);
         Vec3 targetHit = (Vec3)targetBox.m_82371_(position, next).orElse(null);
         Vec3 clipStart = position;
         if (muzzle.m_82557_(position) < graceDistanceSqr) {
            if (muzzle.m_82557_(next) <= graceDistanceSqr) {
               clipStart = next;
            } else {
               clipStart = SiegeWeaponNpcControl.segmentSphereExit(position, next, muzzle, 6.0);
            }
         }

         BlockHitResult blockHit = clipStart.m_82557_(next) <= 1.0E-8
            ? BlockHitResult.m_82426_(next, Direction.UP, BlockPos.m_274446_(next))
            : this.m_9236_().m_45547_(new ClipContext(clipStart, next, Block.COLLIDER, Fluid.NONE, this));
         if (targetHit != null) {
            return blockHit.m_6662_() == Type.MISS || blockHit.m_82450_().m_82557_(position) >= targetHit.m_82557_(position);
         }

         if (blockHit.m_6662_() != Type.MISS) {
            return false;
         }

         position = next;
         velocity = velocity.m_82490_(0.99).m_82520_(0.0, (double)(-this.projectileGravity()), 0.0);
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
      for (int i = 0; i < this.m_6643_(); i++) {
         ItemStack stack = this.m_8020_(i);
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

   public boolean m_7013_(int slot, ItemStack stack) {
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
      if (this.m_20184_().m_165925_() > 4.0E-4) {
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
   protected void m_19956_(Entity passenger, MoveFunction moveFunction) {
      if (this.m_20363_(passenger)) {
         float riderYaw = this.m_146908_() + (this.hasFixedTurret() ? 0.0F : this.getTurretYaw());
         Vec3 offset = this.getPassengerOffset().m_82524_(-riderYaw * (float) (Math.PI / 180.0));
         moveFunction.m_20372_(passenger, this.m_20185_() + offset.f_82479_, this.m_20186_() + offset.f_82480_, this.m_20189_() + offset.f_82481_);
      }
   }

   @Override
   protected ItemStack createPackedItem() {
      ItemStack stack = new ItemStack(CatapultMobileItem.itemForMaterialLevel(this.getMaterialLevel(), this.hasFixedTurret()));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.m_41784_().m_128365_("CatapultMobileData", tag);
      return stack;
   }

   @Override
   public void loadCartData(CompoundTag tag) {
      super.loadCartData(tag);
      this.setFixedTurret(tag.m_128471_("FixedTurret"));
      this.f_19804_.m_135381_(TURRET_YAW, this.hasFixedTurret() ? 0.0F : tag.m_128457_("TurretYaw"));
      this.f_19804_.m_135381_(VELOCITY_BONUS, clamp(tag.m_128457_("VelocityBonus"), 0.0F, 0.3F));
   }

   @Override
   public void saveCartData(CompoundTag tag) {
      super.saveCartData(tag);
      tag.m_128350_("TurretYaw", this.getTurretYaw());
      tag.m_128350_("VelocityBonus", this.getVelocityBonus());
      tag.m_128379_("FixedTurret", this.hasFixedTurret());
   }

   private static record AmmoSlot(int slot, CatapultAmmoType ammoType) {
   }

   private static record LaunchSolution(float speed) {
   }
}
