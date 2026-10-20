package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.item.CatapultTurretStandItem;

public class CatapultTurretStandEntity extends CatapultMobileEntity {
   private static final Component MENU_TITLE = Component.translatable("container.ancientwarfare.catapult_turret_stand");
   private static final Component FIXED_MENU_TITLE = Component.translatable("container.ancientwarfare.catapult_stand_fixed");
   private static final float YAW_SPEED = 2.0F;
   private static final float VELOCITY_STEP = 0.01F;

   public CatapultTurretStandEntity(EntityType<? extends CatapultTurretStandEntity> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   public void applyStandInput(float velocityInput, float yawInput) {
      if (!this.hasFixedTurret() && Math.abs(yawInput) > 1.0E-4F) {
         this.adjustTurret(yawInput * 2.0F);
      }

      if (Math.abs(velocityInput) > 1.0E-4F) {
         this.adjustLaunchVelocity(velocityInput * 0.01F);
      }
   }

   @Override
   protected float clampTurretYaw(float yaw) {
      float wrapped = yaw % 360.0F;
      return wrapped < 0.0F ? wrapped + 360.0F : wrapped;
   }

   @Override
   public void tick() {
      this.setNoGravity(true);
      double fixedX = this.getX();
      double fixedY = this.getY();
      double fixedZ = this.getZ();
      this.setDeltaMovement(Vec3.ZERO);
      super.tick();
      this.setDeltaMovement(Vec3.ZERO);
      this.moveTo(fixedX, fixedY, fixedZ, this.getYRot(), this.getXRot());
   }

   @Override
   protected Component getMenuTitle() {
      return this.hasFixedTurret() ? FIXED_MENU_TITLE : MENU_TITLE;
   }

   @Override
   protected Vec3 getPassengerOffset() {
      return this.hasFixedTurret() ? new Vec3(0.0, 0.0, 1.2) : new Vec3(0.0, 0.55, 1.2);
   }

   public EntityDimensions getDimensions(Pose pose) {
      return this.hasFixedTurret() ? EntityDimensions.scalable(2.0F, 1.7F) : super.getDimensions(pose);
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
      ItemStack stack = new ItemStack(CatapultTurretStandItem.itemForMaterialLevel(this.getMaterialLevel(), this.hasFixedTurret()));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.getOrCreateTag().put("CatapultTurretStandData", tag);
      return stack;
   }

   @Override
   public double muzzleHeight() {
      return this.hasFixedTurret() ? 0.1875 : 0.4;
   }
}
