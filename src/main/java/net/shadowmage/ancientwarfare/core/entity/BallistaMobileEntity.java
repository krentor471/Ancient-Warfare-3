package net.shadowmage.ancientwarfare.core.entity;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.siege.SiegeWeaponControl;
import net.shadowmage.ancientwarfare.core.item.BallistaAmmoItem;
import net.shadowmage.ancientwarfare.core.item.BallistaAmmoType;
import net.shadowmage.ancientwarfare.core.item.BallistaMobileItem;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;

public class BallistaMobileEntity extends ChestPushcartEntity implements SiegeWeaponControl {
   public static final int STORAGE_SLOT_COUNT = 27;
   private static final float MAX_TURRET_PITCH = 15.0F;
   private static final float MIN_TURRET_PITCH = -15.0F;
   private static final EntityDataAccessor<Float> TURRET_PITCH = SynchedEntityData.defineId(BallistaMobileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> TURRET_YAW = SynchedEntityData.defineId(BallistaMobileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> FIRE_ANIMATION = SynchedEntityData.defineId(BallistaMobileEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> CRANK_ANGLE = SynchedEntityData.defineId(BallistaMobileEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> FIXED_TURRET = SynchedEntityData.defineId(BallistaMobileEntity.class, EntityDataSerializers.BOOLEAN);
   private static final Component TURRET_MENU_TITLE = Component.translatable("container.ancientwarfare.ballista_mobile");
   private static final Component FIXED_MENU_TITLE = Component.translatable("container.ancientwarfare.ballista_mobile_fixed");
   private static final int FIRE_COOLDOWN_TICKS = 30;
   public static final int RELEASE_ANIMATION_TICKS = 5;
   public static final int RELOAD_ANIMATION_TICKS = 25;
   public static final int FIRE_ANIMATION_TICKS = 30;
   private static final double MUZZLE_FORWARD_OFFSET = 1.0;
   private static final double MUZZLE_HEIGHT = 1.2;
   protected int fireCooldown;

   public BallistaMobileEntity(EntityType<? extends BallistaMobileEntity> type, Level level) {
      super(type, level);
   }

   @Override
   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(TURRET_PITCH, 0.0F);
      this.entityData.define(TURRET_YAW, 0.0F);
      this.entityData.define(FIRE_ANIMATION, 0);
      this.entityData.define(CRANK_ANGLE, 0.0F);
      this.entityData.define(FIXED_TURRET, false);
   }

   @Override
   public int getContainerSize() {
      return 27;
   }

   public float getTurretPitch() {
      return (Float)this.entityData.get(TURRET_PITCH);
   }

   public float getTurretYaw() {
      return (Float)this.entityData.get(TURRET_YAW);
   }

   public boolean hasFixedTurret() {
      return (Boolean)this.entityData.get(FIXED_TURRET);
   }

   public int getFireAnimationTicks() {
      return (Integer)this.entityData.get(FIRE_ANIMATION);
   }

   public float getCrankAngle() {
      return (Float)this.entityData.get(CRANK_ANGLE);
   }

   public void setFixedTurret(boolean fixedTurret) {
      this.entityData.set(FIXED_TURRET, fixedTurret);
      if (fixedTurret) {
         this.entityData.set(TURRET_YAW, 0.0F);
      }
   }

   public void adjustTurret(float pitchDelta, float yawDelta) {
      this.setTurretPitch(clamp(this.getTurretPitch() + pitchDelta, -15.0F, 15.0F));
      this.setTurretYaw(this.hasFixedTurret() ? 0.0F : Mth.wrapDegrees(this.getTurretYaw() + yawDelta));
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   protected void setTurretPitch(float pitch) {
      this.entityData.set(TURRET_PITCH, pitch);
   }

   protected void setTurretYaw(float yaw) {
      this.entityData.set(TURRET_YAW, yaw);
   }

   @Override
   protected final float maxHealthForLevel(int level) {
      return ballistaMaxHealthForLevel(level);
   }

   @Override
   protected final float armorForLevel(int level) {
      return ballistaArmorForLevel(level);
   }

   public static float ballistaMaxHealthForLevel(int level) {
      return VehicleDurability.attackHealth(level);
   }

   public static float ballistaArmorForLevel(int level) {
      return VehicleDurability.attackArmor(level);
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         if (this.fireCooldown > 0) {
            this.fireCooldown--;
         }

         int animationTicks = this.getFireAnimationTicks();
         if (animationTicks > 0) {
            if (animationTicks <= 25) {
               this.entityData.set(CRANK_ANGLE, this.getCrankAngle() - 4.0F);
            }

            this.entityData.set(FIRE_ANIMATION, animationTicks - 1);
         }
      }
   }

   public boolean tryFire() {
      if (!this.level().isClientSide && this.fireCooldown <= 0) {
         BallistaMobileEntity.AmmoSlot ammoSlot = this.findLoadedAmmo();
         if (ammoSlot == null) {
            return false;
         } else {
            ItemStack stack = this.getItem(ammoSlot.slot());
            stack.shrink(1);
            this.setItem(ammoSlot.slot(), stack);
            this.setChanged();
            LivingEntity shooter = this.getControllingPassenger();
            BallistaBoltEntity bolt = new BallistaBoltEntity(this.level(), shooter, ammoSlot.ammoType());
            double yaw = Math.toRadians((double)(this.getYRot() + this.getTurretYaw()));
            double pitch = Math.toRadians((double)this.getTurretPitch());
            double cosPitch = Math.cos(pitch);
            Vec3 direction = new Vec3(-Math.sin(yaw) * cosPitch, Math.sin(pitch), Math.cos(yaw) * cosPitch).normalize();
            Vec3 muzzle = this.position().add(direction.scale(this.muzzleForwardOffset())).add(0.0, this.muzzleHeight(), 0.0);
            bolt.moveTo(muzzle.x, muzzle.y, muzzle.z, this.getYRot() + this.getTurretYaw(), this.getTurretPitch());
            bolt.setBlockCollisionGraceOrigin(muzzle);
            bolt.shoot(direction.x, direction.y, direction.z, 3.4F, 0.35F);
            this.level().addFreshEntity(bolt);
            this.fireCooldown = 30;
            this.entityData.set(FIRE_ANIMATION, 30);
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
      if (!SiegeWeaponNpcControl.canAttack(this, target, 3.0, 96.0)) {
         return false;
      } else {
         float worldYaw = SiegeWeaponNpcControl.worldYawTo(target, this);
         if (this instanceof BallistaStandFixedEntity && Math.abs(Mth.wrapDegrees(worldYaw - this.getYRot())) > 4.0F) {
            return false;
         } else {
            float pitch = SiegeWeaponNpcControl.clearBallisticPitch(
               this.level(),
               this,
               target,
               this.muzzleHeight(),
               this.muzzleForwardOffset(),
               (double)this.projectileSpeed(),
               (double)this.projectileGravity(),
               this instanceof BallistaStandFixedEntity ? this.getYRot() : worldYaw,
               -15.0F,
               15.0F,
               6.0
            );
            return !Float.isNaN(pitch);
         }
      }
   }

   @Override
   public double minimumAttackRange() {
      return 3.0;
   }

   @Override
   public double maximumAttackRange() {
      return 96.0;
   }

   @Override
   public boolean isMobile() {
      return !(this instanceof BallistaTurretStandEntity);
   }

   @Override
   public void aimAt(PathfinderMob engineer, LivingEntity target) {
      float worldYaw = SiegeWeaponNpcControl.worldYawTo(target, this);
      if (this instanceof BallistaStandFixedEntity) {
         this.setTurretYaw(0.0F);
      } else if (this instanceof BallistaTurretStandEntity) {
         this.setTurretYaw(SiegeWeaponNpcControl.localYaw(worldYaw, this));
      } else {
         SiegeWeaponNpcControl.faceVehicle(this, worldYaw);
         this.setTurretYaw(0.0F);
      }

      float pitch = SiegeWeaponNpcControl.clearBallisticPitch(
         this.level(),
         this,
         target,
         this.muzzleHeight(),
         this.muzzleForwardOffset(),
         (double)this.projectileSpeed(),
         (double)this.projectileGravity(),
         this instanceof BallistaStandFixedEntity ? this.getYRot() : worldYaw,
         -15.0F,
         15.0F,
         6.0
      );
      this.setTurretPitch(
         Float.isNaN(pitch)
            ? clamp(
               SiegeWeaponNpcControl.ballisticPitchTo(target, this, this.muzzleHeight(), (double)this.projectileSpeed(), (double)this.projectileGravity()),
               -15.0F,
               15.0F
            )
            : pitch
      );
   }

   @Override
   public float projectileSpeed() {
      return 3.4F;
   }

   @Override
   public double muzzleHeight() {
      return 1.2;
   }

   protected double muzzleForwardOffset() {
      return 1.0;
   }

   @Override
   public boolean fireAt(PathfinderMob engineer, LivingEntity target) {
      SiegeWeaponNpcControl.supplyFactionAmmo(engineer, this, AWCoreRegistryAccess.stack(AWCoreIds.AMMO_BALLISTA_BOLT));
      return this.canAttackTarget(target) && this.tryFire();
   }

   private BallistaMobileEntity.AmmoSlot findLoadedAmmo() {
      for (int i = 0; i < this.getContainerSize(); i++) {
         ItemStack stack = this.getItem(i);
         BallistaAmmoType ammoType = BallistaAmmoItem.ammoType(stack);
         if (ammoType != null) {
            return new BallistaMobileEntity.AmmoSlot(i, ammoType);
         }
      }

      return null;
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
   }

   @Override
   protected Component getMenuTitle() {
      return this.hasFixedTurret() ? FIXED_MENU_TITLE : TURRET_MENU_TITLE;
   }

   @Override
   protected Vec3 getPassengerOffset() {
      return new Vec3(0.0, 0.55, -1.2);
   }

   @Override
   protected void positionRider(Entity passenger, MoveFunction moveFunction) {
      if (this.hasPassenger(passenger)) {
         Vec3 offset = this.getPassengerOffset().yRot(-this.getYRot() * (float) (Math.PI / 180.0));
         moveFunction.accept(passenger, this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);
      }
   }

   @Override
   protected ItemStack createPackedItem() {
      ItemStack stack = new ItemStack(BallistaMobileItem.itemForMaterialLevel(this.getMaterialLevel(), this.hasFixedTurret()));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.getOrCreateTag().put("BallistaMobileData", tag);
      return stack;
   }

   @Override
   public void loadCartData(CompoundTag tag) {
      super.loadCartData(tag);
      this.setFixedTurret(tag.getBoolean("FixedTurret"));
      this.entityData.set(CRANK_ANGLE, tag.getFloat("BallistaCrankAngle"));
   }

   @Override
   public void saveCartData(CompoundTag tag) {
      super.saveCartData(tag);
      tag.putBoolean("FixedTurret", this.hasFixedTurret());
      tag.putFloat("BallistaCrankAngle", this.getCrankAngle());
   }

   private static record AmmoSlot(int slot, BallistaAmmoType ammoType) {
   }
}
