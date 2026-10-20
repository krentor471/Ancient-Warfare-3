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
import net.shadowmage.ancientwarfare.core.item.CannonAmmoItem;
import net.shadowmage.ancientwarfare.core.item.CannonAmmoType;
import net.shadowmage.ancientwarfare.core.item.CannonTurretStandItem;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.object.PlayState;

public class CannonTurretStandEntity extends ChestPushcartEntity implements SiegeWeaponControl {
   public static final int STORAGE_SLOT_COUNT = 27;
   private static final Component MENU_TITLE = Component.m_237115_("container.ancientwarfare.cannon_turret_stand");
   private static final Component FIXED_MENU_TITLE = Component.m_237115_("container.ancientwarfare.cannon_stand_fixed");
   private static final float MIN_PITCH = -12.0F;
   private static final float MAX_PITCH = 27.0F;
   private static final float YAW_SPEED = 2.0F;
   private static final float PITCH_SPEED = 1.0F;
   private static final float AI_YAW_SPEED = 3.0F;
   private static final float AI_PITCH_SPEED = 1.5F;
   private static final int FIRE_COOLDOWN_TICKS = 50;
   private static final int FIRE_ANIMATION_LENGTH_TICKS = 12;
   private static final byte FIRE_ANIMATION_EVENT = 61;
   private static final RawAnimation FIRE = RawAnimation.begin().thenPlay("animation.ancientwarfare.cannon_turret_stand.fire_recoil");
   private static final EntityDataAccessor<Float> TURRET_PITCH = SynchedEntityData.m_135353_(CannonTurretStandEntity.class, EntityDataSerializers.f_135029_);
   private static final EntityDataAccessor<Float> TURRET_YAW = SynchedEntityData.m_135353_(CannonTurretStandEntity.class, EntityDataSerializers.f_135029_);
   private static final EntityDataAccessor<Integer> FIRE_ANIMATION_TICKS = SynchedEntityData.m_135353_(
      CannonTurretStandEntity.class, EntityDataSerializers.f_135028_
   );
   private static final EntityDataAccessor<Boolean> FIXED_TURRET = SynchedEntityData.m_135353_(CannonTurretStandEntity.class, EntityDataSerializers.f_135035_);
   private int fireCooldown;
   private int previousFireAnimationTicks;

   public CannonTurretStandEntity(EntityType<? extends CannonTurretStandEntity> type, Level level) {
      super(type, level);
      this.m_20242_(true);
   }

   @Override
   protected void m_8097_() {
      super.m_8097_();
      this.f_19804_.m_135372_(TURRET_PITCH, 0.0F);
      this.f_19804_.m_135372_(TURRET_YAW, 0.0F);
      this.f_19804_.m_135372_(FIRE_ANIMATION_TICKS, 0);
      this.f_19804_.m_135372_(FIXED_TURRET, false);
   }

   @Override
   public int m_6643_() {
      return 27;
   }

   @Override
   protected float maxHealthForLevel(int level) {
      return VehicleDurability.attackHealth(level);
   }

   @Override
   protected float armorForLevel(int level) {
      return VehicleDurability.attackArmor(level);
   }

   public float getTurretPitch() {
      return (Float)this.f_19804_.m_135370_(TURRET_PITCH);
   }

   public float getTurretYaw() {
      return (Float)this.f_19804_.m_135370_(TURRET_YAW);
   }

   public int getFireAnimationTicks() {
      return (Integer)this.f_19804_.m_135370_(FIRE_ANIMATION_TICKS);
   }

   public boolean hasFixedTurret() {
      return (Boolean)this.f_19804_.m_135370_(FIXED_TURRET);
   }

   public void setFixedTurret(boolean fixedTurret) {
      this.f_19804_.m_135381_(FIXED_TURRET, fixedTurret);
      if (fixedTurret) {
         this.f_19804_.m_135381_(TURRET_YAW, 0.0F);
      }
   }

   public void applyStandInput(float pitchInput, float yawInput) {
      if (Math.abs(pitchInput) > 1.0E-4F) {
         this.f_19804_.m_135381_(TURRET_PITCH, clamp(this.getTurretPitch() + pitchInput * 1.0F, -12.0F, 27.0F));
      }

      if (!this.hasFixedTurret() && Math.abs(yawInput) > 1.0E-4F) {
         this.f_19804_.m_135381_(TURRET_YAW, wrapDegrees(this.getTurretYaw() + yawInput * 2.0F));
      }
   }

   @Override
   public void m_8119_() {
      if (!this.isMobileCannon()) {
         this.m_20242_(true);
         this.m_20256_(Vec3.f_82478_);
      }

      double fixedX = this.m_20185_();
      double fixedY = this.m_20186_();
      double fixedZ = this.m_20189_();
      super.m_8119_();
      if (this.fireCooldown > 0) {
         this.fireCooldown--;
      }

      if (this.getFireAnimationTicks() > 0) {
         this.f_19804_.m_135381_(FIRE_ANIMATION_TICKS, this.getFireAnimationTicks() - 1);
      }

      if (!this.isMobileCannon()) {
         this.m_20256_(Vec3.f_82478_);
         this.m_7678_(fixedX, fixedY, fixedZ, this.m_146908_(), this.m_146909_());
      }
   }

   protected boolean isMobileCannon() {
      return false;
   }

   public boolean tryFire() {
      if (!this.m_9236_().f_46443_ && this.fireCooldown <= 0) {
         CannonTurretStandEntity.AmmoSlot ammoSlot = this.findLoadedAmmo();
         if (ammoSlot == null) {
            return false;
         } else {
            ItemStack stack = this.m_8020_(ammoSlot.slot());
            stack.m_41774_(1);
            this.m_6836_(ammoSlot.slot(), stack);
            this.m_6596_();
            this.fireAmmo(ammoSlot.ammoType());
            this.fireCooldown = 50;
            this.f_19804_.m_135381_(FIRE_ANIMATION_TICKS, 12);
            this.m_9236_().m_7605_(this, (byte)61);
            return true;
         }
      } else {
         return false;
      }
   }

   public void m_7822_(byte eventId) {
      if (eventId == 61) {
         this.f_19804_.m_135381_(FIRE_ANIMATION_TICKS, 12);
         this.previousFireAnimationTicks = 0;
      } else {
         super.m_7822_(eventId);
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
      if (!SiegeWeaponNpcControl.canAttack(this, target, 4.0, 128.0)) {
         return false;
      } else {
         float worldYaw = SiegeWeaponNpcControl.worldYawTo(target, this);
         if (this.hasFixedTurret() && !this.isMobileCannon() && Math.abs(Mth.m_14177_(worldYaw - this.m_146908_())) > 4.0F) {
            return false;
         } else {
            float pitch = SiegeWeaponNpcControl.clearBallisticPitch(
               this.m_9236_(),
               this,
               target,
               this.muzzleHeight(),
               this.muzzleDistance(),
               (double)this.projectileSpeed(),
               (double)this.projectileGravity(),
               this.hasFixedTurret() && !this.isMobileCannon() ? this.m_146908_() : worldYaw,
               -12.0F,
               27.0F,
               6.0
            );
            return !Float.isNaN(pitch);
         }
      }
   }

   @Override
   public double minimumAttackRange() {
      return 4.0;
   }

   @Override
   public double maximumAttackRange() {
      return 128.0;
   }

   @Override
   public boolean isMobile() {
      return this.isMobileCannon();
   }

   @Override
   public void aimAt(PathfinderMob engineer, LivingEntity target) {
      float worldYaw = SiegeWeaponNpcControl.worldYawTo(target, this);
      if (this.isMobileCannon()) {
         float nextYaw = Mth.m_14148_(this.m_146908_(), worldYaw, 3.0F);
         SiegeWeaponNpcControl.faceVehicle(this, nextYaw);
         this.f_19804_.m_135381_(TURRET_YAW, 0.0F);
      } else if (this.hasFixedTurret()) {
         this.f_19804_.m_135381_(TURRET_YAW, 0.0F);
      } else {
         float targetYaw = SiegeWeaponNpcControl.localYaw(worldYaw, this);
         this.f_19804_.m_135381_(TURRET_YAW, wrapDegrees(Mth.m_14148_(this.getTurretYaw(), targetYaw, 3.0F)));
      }

      float clearPitch = SiegeWeaponNpcControl.clearBallisticPitch(
         this.m_9236_(),
         this,
         target,
         this.muzzleHeight(),
         this.muzzleDistance(),
         (double)this.projectileSpeed(),
         (double)this.projectileGravity(),
         worldYaw,
         -12.0F,
         27.0F,
         6.0
      );
      float targetPitch = Float.isNaN(clearPitch)
         ? clamp(
            SiegeWeaponNpcControl.ballisticPitchTo(target, this, this.muzzleHeight(), (double)this.projectileSpeed(), (double)this.projectileGravity()),
            -12.0F,
            27.0F
         )
         : clearPitch;
      this.f_19804_.m_135381_(TURRET_PITCH, Mth.m_14121_(this.getTurretPitch(), targetPitch, 1.5F));
   }

   @Override
   public boolean fireAt(PathfinderMob engineer, LivingEntity target) {
      SiegeWeaponNpcControl.supplyFactionAmmo(engineer, this, AWCoreRegistryAccess.stack(AWCoreIds.AMMO_IRON_SHOT_10));
      return this.canAttackTarget(target) && this.isAimedAt(target) && this.tryFire();
   }

   private boolean isAimedAt(LivingEntity target) {
      float worldYaw = SiegeWeaponNpcControl.worldYawTo(target, this);
      float yawError = !this.hasFixedTurret() && !this.isMobileCannon()
         ? Math.abs(Mth.m_14177_(SiegeWeaponNpcControl.localYaw(worldYaw, this) - this.getTurretYaw()))
         : Math.abs(Mth.m_14177_(worldYaw - this.m_146908_()));
      float targetPitch = SiegeWeaponNpcControl.clearBallisticPitch(
         this.m_9236_(),
         this,
         target,
         this.muzzleHeight(),
         this.muzzleDistance(),
         (double)this.projectileSpeed(),
         (double)this.projectileGravity(),
         worldYaw,
         -12.0F,
         27.0F,
         6.0
      );
      return Float.isNaN(targetPitch) ? false : yawError <= 4.0F && Math.abs(targetPitch - this.getTurretPitch()) <= 2.0F;
   }

   @Override
   public float projectileSpeed() {
      return 3.8F;
   }

   @Override
   public double muzzleHeight() {
      return this.isMobileCannon() ? 0.91 : 1.45;
   }

   private double muzzleDistance() {
      return this.isMobileCannon() ? 1.4 : 1.55;
   }

   private void fireAmmo(CannonAmmoType ammoType) {
      LivingEntity shooter = this.m_6688_();
      double yaw = Math.toRadians((double)(this.m_146908_() + this.getTurretYaw()));
      double pitch = Math.toRadians((double)this.getTurretPitch());
      double cosPitch = Math.cos(pitch);
      Vec3 direction = new Vec3(-Math.sin(yaw) * cosPitch, Math.sin(pitch), Math.cos(yaw) * cosPitch).m_82541_();
      Vec3 muzzle = this.m_20182_().m_82549_(direction.m_82490_(this.muzzleDistance())).m_82520_(0.0, this.muzzleHeight(), 0.0);
      if (ammoType.pelletsOnLaunch()) {
         this.spawnPellets(shooter, ammoType, muzzle, direction);
      } else {
         CannonShotEntity shot = new CannonShotEntity(this.m_9236_(), shooter, ammoType, false);
         shot.m_7678_(muzzle.f_82479_, muzzle.f_82480_, muzzle.f_82481_, this.m_146908_() + this.getTurretYaw(), this.getTurretPitch());
         shot.setBlockCollisionGraceOrigin(muzzle);
         shot.m_6686_(direction.f_82479_, direction.f_82480_, direction.f_82481_, 3.8F, 0.18F);
         this.m_9236_().m_7967_(shot);
      }
   }

   public void spawnPellets(LivingEntity shooter, CannonAmmoType ammoType, Vec3 muzzle, Vec3 direction) {
      int count = Math.min(32, Math.max(1, ammoType.pelletCount()));

      for (int i = 0; i < count; i++) {
         Vec3 spread = direction.m_82520_(
               (this.f_19796_.m_188500_() - 0.5) * 0.28, (this.f_19796_.m_188500_() - 0.5) * 0.18, (this.f_19796_.m_188500_() - 0.5) * 0.28
            )
            .m_82541_();
         CannonShotEntity pellet = new CannonShotEntity(this.m_9236_(), shooter, ammoType, true);
         pellet.m_7678_(muzzle.f_82479_, muzzle.f_82480_, muzzle.f_82481_, this.m_146908_() + this.getTurretYaw(), this.getTurretPitch());
         pellet.setBlockCollisionGraceOrigin(muzzle);
         pellet.m_6686_(spread.f_82479_, spread.f_82480_, spread.f_82481_, 3.2F, 0.35F);
         this.m_9236_().m_7967_(pellet);
      }
   }

   private CannonTurretStandEntity.AmmoSlot findLoadedAmmo() {
      for (int i = 0; i < this.m_6643_(); i++) {
         ItemStack stack = this.m_8020_(i);
         CannonAmmoType ammoType = CannonAmmoItem.ammoType(stack);
         if (ammoType != null) {
            return new CannonTurretStandEntity.AmmoSlot(i, ammoType);
         }
      }

      return null;
   }

   public boolean m_7013_(int slot, ItemStack stack) {
      return CannonAmmoItem.ammoType(stack) != null;
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController[]{new AnimationController(this, "fire", 0, this::fireAnimation)});
   }

   private PlayState fireAnimation(AnimationState<CannonTurretStandEntity> state) {
      int ticks = this.getFireAnimationTicks();
      if (ticks > 0) {
         if (this.previousFireAnimationTicks <= 0) {
            state.getController().forceAnimationReset();
         }

         this.previousFireAnimationTicks = ticks;
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
      return this.hasFixedTurret() ? new Vec3(0.0, 0.78, -1.1) : new Vec3(0.0, 0.55, -1.25);
   }

   @Override
   protected void m_19956_(Entity passenger, MoveFunction moveFunction) {
      if (this.m_20363_(passenger)) {
         Vec3 offset = this.getPassengerOffset().m_82524_(-(this.m_146908_() + this.getTurretYaw()) * (float) (Math.PI / 180.0));
         moveFunction.m_20372_(passenger, this.m_20185_() + offset.f_82479_, this.m_20186_() + offset.f_82480_, this.m_20189_() + offset.f_82481_);
      }
   }

   @Override
   protected ItemStack createPackedItem() {
      ItemStack stack = new ItemStack(CannonTurretStandItem.itemForMaterialLevel(this.getMaterialLevel(), this.hasFixedTurret()));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.m_41784_().m_128365_("CannonTurretStandData", tag);
      return stack;
   }

   @Override
   public void loadCartData(CompoundTag tag) {
      super.loadCartData(tag);
      this.f_19804_.m_135381_(TURRET_PITCH, clamp(tag.m_128457_("TurretPitch"), -12.0F, 27.0F));
      this.setFixedTurret(tag.m_128471_("FixedTurret"));
      this.f_19804_.m_135381_(TURRET_YAW, this.hasFixedTurret() ? 0.0F : wrapDegrees(tag.m_128457_("TurretYaw")));
   }

   @Override
   public void saveCartData(CompoundTag tag) {
      super.saveCartData(tag);
      tag.m_128350_("TurretPitch", this.getTurretPitch());
      tag.m_128350_("TurretYaw", this.getTurretYaw());
      tag.m_128379_("FixedTurret", this.hasFixedTurret());
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private static float wrapDegrees(float yaw) {
      float wrapped = yaw % 360.0F;
      return wrapped < 0.0F ? wrapped + 360.0F : wrapped;
   }

   private static record AmmoSlot(int slot, CannonAmmoType ammoType) {
   }
}
