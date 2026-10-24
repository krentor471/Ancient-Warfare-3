package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.item.TransportBoatItem;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;

public class TransportBoatEntity extends ChestPushcartEntity {
   private static final Component MENU_TITLE = Component.translatable("container.ancientwarfare.transport_boat");

   public TransportBoatEntity(EntityType<? extends TransportBoatEntity> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
   }

   @Override
   protected void updateVehicleMotion() {
      boolean inWater = this.isInWater() || this.level().getFluidState(this.blockPosition()).is(FluidTags.WATER);
      LivingEntity rider = this.getControllingPassenger();
      double yMotion = inWater ? 0.0 : -0.04;
      if (this.isVehicle() && rider != null && inWater) {
         if (Math.abs(this.inputTurn) > 1.0E-4F) {
            this.setYRot(this.getYRot() - this.inputTurn * 3.0F);
            this.setYHeadRot(this.getYRot());
            this.setYBodyRot(this.getYRot());
         }

         this.yRotO = this.getYRot();
         if (Math.abs(this.inputForward) > 1.0E-4F) {
            double speed = this.inputForward > 0.0F ? 0.13 : 0.055;
            double yaw = Math.toRadians((double)this.getYRot());
            double motion = (double)this.inputForward * speed;
            this.setDeltaMovement(-Math.sin(yaw) * motion, yMotion, Math.cos(yaw) * motion);
            this.hasImpulse = true;
         } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.72, 0.0, 0.72));
         }
      } else {
         this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, 1.0, 0.8).add(0.0, yMotion, 0.0));
      }

      this.move(MoverType.SELF, this.getDeltaMovement());
      this.setDeltaMovement(this.getDeltaMovement().multiply(inWater ? 0.92 : 0.7, inWater ? 0.0 : 0.8, inWater ? 0.92 : 0.7));
   }

   @Override
   protected Component getMenuTitle() {
      return MENU_TITLE;
   }

   @Override
   protected Vec3 getPassengerOffset() {
      return new Vec3(0.0, 0.55, 1.4);
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
      ItemStack stack = new ItemStack(TransportBoatItem.itemForMaterialLevel(this.getMaterialLevel()));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.getOrCreateTag().put("TransportBoatData", tag);
      return stack;
   }
}
