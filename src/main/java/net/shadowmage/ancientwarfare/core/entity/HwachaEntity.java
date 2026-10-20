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
   private static final Component MENU_TITLE = Component.m_237115_("container.ancientwarfare.hwacha");
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
   public void m_8119_() {
      super.m_8119_();
      if (this.fireCooldown > 0) {
         this.fireCooldown--;
      }

      if (this.npcBurstRestTicks > 0) {
         this.npcBurstRestTicks--;
      }
   }

   public boolean tryFire() {
      if (!this.m_9236_().f_46443_ && this.fireCooldown <= 0) {
         HwachaEntity.AmmoSlot ammoSlot = this.findLoadedAmmo();
         if (ammoSlot == null) {
            return false;
         } else {
            ItemStack stack = this.m_8020_(ammoSlot.slot());
            stack.m_41774_(1);
            this.m_6836_(ammoSlot.slot(), stack);
            this.m_6596_();
            LivingEntity shooter = this.m_6688_();
            HwachaRocketEntity rocket = new HwachaRocketEntity(this.m_9236_(), shooter, ammoSlot.ammoType());
            int rocketIndex = this.nextRocketIndex;
            this.nextRocketIndex = (this.nextRocketIndex + 1) % 36;
            int row = rocketIndex / 9;
            int column = rocketIndex % 9;
            float yawSpread = (this.f_19796_.m_188501_() - this.f_19796_.m_188501_()) * 4.5F;
            float pitchSpread = (this.f_19796_.m_188501_() - this.f_19796_.m_188501_()) * 2.0F;
            double yaw = Math.toRadians((double)(this.m_146908_() + yawSpread));
            double pitch = Math.toRadians((double)(39.0F + pitchSpread));
            double cosPitch = Math.cos(pitch);
            Vec3 direction = new Vec3(-Math.sin(yaw) * cosPitch, Math.sin(pitch), Math.cos(yaw) * cosPitch).m_82541_();
            Vec3 side = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw)).m_82541_();
            Vec3 muzzle = this.m_20182_()
               .m_82549_(direction.m_82490_(2.0))
               .m_82549_(side.m_82490_((double)(column - 4) * 0.12))
               .m_82520_(0.0, 1.15 + (double)row * 0.1, 0.0);
            rocket.m_7678_(muzzle.f_82479_, muzzle.f_82480_, muzzle.f_82481_, this.m_146908_() + yawSpread, -(39.0F + pitchSpread));
            rocket.setBlockCollisionGraceOrigin(muzzle);
            rocket.m_6686_(direction.f_82479_, direction.f_82480_, direction.f_82481_, 1.9F, 0.45F);
            this.m_9236_().m_7967_(rocket);
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
         && SiegeWeaponNpcControl.hasClearBallisticPath(this.m_9236_(), this, target, 1.15, 2.0, 1.9F, 0.045, this.m_146908_(), 39.0F, 6.0);
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
      for (int i = 0; i < this.m_6643_(); i++) {
         ItemStack stack = this.m_8020_(i);
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
      if (this.m_20184_().m_165925_() > 4.0E-4) {
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
   protected void m_19956_(Entity passenger, MoveFunction moveFunction) {
      if (this.m_20363_(passenger)) {
         Vec3 offset = this.getPassengerOffset().m_82524_(-this.m_146908_() * (float) (Math.PI / 180.0));
         moveFunction.m_20372_(passenger, this.m_20185_() + offset.f_82479_, this.m_20186_() + offset.f_82480_, this.m_20189_() + offset.f_82481_);
      }
   }

   @Override
   protected ItemStack createPackedItem() {
      ItemStack stack = new ItemStack(HwachaItem.itemForMaterialLevel(this.getMaterialLevel()));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.m_41784_().m_128365_("HwachaData", tag);
      return stack;
   }

   private static record AmmoSlot(int slot, HwachaAmmoType ammoType) {
   }
}
