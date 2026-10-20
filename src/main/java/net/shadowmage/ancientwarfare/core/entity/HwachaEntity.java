package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.shadowmage.ancientwarfare.core.item.HwachaAmmoItem;
import net.shadowmage.ancientwarfare.core.item.HwachaAmmoType;
import net.shadowmage.ancientwarfare.core.item.HwachaItem;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.object.PlayState;

public class HwachaEntity extends ChestPushcartEntity implements SiegeWeaponControl {
   private static final Component MENU_TITLE = Component.translatable("container.ancientwarfare.hwacha");
   private static final RawAnimation WHEEL_ROLL = RawAnimation.begin().thenLoop("animation.ancientwarfare.legacy.hwacha.wheel_rotations");
   private static final int FIRE_INTERVAL_TICKS = 5;
   private static final int RACK_COLUMNS = 9;
   private static final int RACK_ROCKETS = 36;
   private static final float LAUNCH_PITCH = 39.0F;
   private static final float ROCKET_SPEED = 1.9F;
   private int fireCooldown;
   private int nextRocketIndex;
   private int npcBurstRemaining;
   private int npcBurstRestTicks;

   public HwachaEntity(EntityType<? extends HwachaEntity> type, Level level) {
      super(type, level);
   }

   @Override
   protected Component getMenuTitle() {
      return MENU_TITLE;
   }

   @Override
   protected final float maxHealthForLevel(int level) {
      return VehicleDurability.attackHealth(level);
   }

   @Override
   protected final float armorForLevel(int level) {
      return VehicleDurability.attackArmor(level);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.fireCooldown > 0) {
         this.fireCooldown--;
      }

      if (this.npcBurstRestTicks > 0) {
         this.npcBurstRestTicks--;
      }
   }

   public boolean tryFire() {
      if (!this.level().isClientSide && this.fireCooldown <= 0) {
         HwachaEntity.AmmoSlot ammoSlot = this.findLoadedAmmo();
         if (ammoSlot == null) {
            return false;
         } else {
            ItemStack stack = this.getItem(ammoSlot.slot());
            stack.shrink(1);
            this.setItem(ammoSlot.slot(), stack);
            this.setChanged();
            LivingEntity shooter = this.getControllingPassenger();
            HwachaRocketEntity rocket = new HwachaRocketEntity(this.level(), shooter, ammoSlot.ammoType());
            int rocketIndex = this.nextRocketIndex;
            this.nextRocketIndex = (this.nextRocketIndex + 1) % 36;
            int row = rocketIndex / 9;
            int column = rocketIndex % 9;
            float yawSpread = (this.random.nextFloat() - this.random.nextFloat()) * 4.5F;
            float pitchSpread = (this.random.nextFloat() - this.random.nextFloat()) * 2.0F;
            double yaw = Math.toRadians((double)(this.getYRot() + yawSpread));
            double pitch = Math.toRadians((double)(39.0F + pitchSpread));
            double cosPitch = Math.cos(pitch);
            Vec3 direction = new Vec3(-Math.sin(yaw) * cosPitch, Math.sin(pitch), Math.cos(yaw) * cosPitch).normalize();
            Vec3 side = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw)).normalize();
            Vec3 muzzle = this.position()
               .add(direction.scale(2.0))
               .add(side.scale((double)(column - 4) * 0.12))
               .add(0.0, 1.15 + (double)row * 0.1, 0.0);
            rocket.moveTo(muzzle.x, muzzle.y, muzzle.z, this.getYRot() + yawSpread, -(39.0F + pitchSpread));
            rocket.setBlockCollisionGraceOrigin(muzzle);
            rocket.shoot(direction.x, direction.y, direction.z, 1.9F, 0.45F);
            this.level().addFreshEntity(rocket);
            this.fireCooldown = 5;
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
      return SiegeWeaponNpcControl.canAttack(this, target, 10.0, 80.0)
         && SiegeWeaponNpcControl.hasClearBallisticPath(this.level(), this, target, 1.15, 2.0, 1.9F, 0.045, this.getYRot(), 39.0F, 6.0);
   }

   @Override
   public double minimumAttackRange() {
      return 10.0;
   }

   @Override
   public double maximumAttackRange() {
      return 80.0;
   }

   @Override
   public boolean isMobile() {
      return true;
   }

   @Override
   public void aimAt(PathfinderMob engineer, LivingEntity target) {
      SiegeWeaponNpcControl.faceVehicle(this, SiegeWeaponNpcControl.worldYawTo(target, this));
   }

   @Override
   public float projectileSpeed() {
      return 1.9F;
   }

   @Override
   public float projectileGravity() {
      return 0.045F;
   }

   @Override
   public boolean fireAt(PathfinderMob engineer, LivingEntity target) {
      if (this.npcBurstRestTicks > 0) {
         return false;
      } else {
         SiegeWeaponNpcControl.supplyFactionAmmo(engineer, this, AWCoreRegistryAccess.stack(AWCoreIds.AMMO_HWACHA_ROCKET));
         if (this.npcBurstRemaining <= 0) {
            this.npcBurstRemaining = 16;
         }

         if (this.canAttackTarget(target) && this.tryFire()) {
            this.npcBurstRemaining--;
            if (this.npcBurstRemaining <= 0) {
               this.npcBurstRestTicks = 40;
            }

            return true;
         } else {
            return false;
         }
      }
   }

   private HwachaEntity.AmmoSlot findLoadedAmmo() {
      for (int i = 0; i < this.getContainerSize(); i++) {
         ItemStack stack = this.getItem(i);
         HwachaAmmoType ammoType = HwachaAmmoItem.ammoType(stack);
         if (ammoType != null) {
            return new HwachaEntity.AmmoSlot(i, ammoType);
         }
      }

      return null;
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController[]{new AnimationController(this, "wheel", 0, this::wheelAnimation)});
   }

   private PlayState wheelAnimation(AnimationState<HwachaEntity> state) {
      if (this.getDeltaMovement().horizontalDistanceSqr() > 4.0E-4) {
         state.setAnimation(WHEEL_ROLL);
         return PlayState.CONTINUE;
      } else {
         return PlayState.STOP;
      }
   }

   @Override
   protected Vec3 getPassengerOffset() {
      return new Vec3(0.0, 0.72, -1.45);
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
      ItemStack stack = new ItemStack(HwachaItem.itemForMaterialLevel(this.getMaterialLevel()));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.getOrCreateTag().put("HwachaData", tag);
      return stack;
   }

   private static record AmmoSlot(int slot, HwachaAmmoType ammoType) {
   }
}
