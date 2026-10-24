package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.item.TrebuchetFixedItem;

public class TrebuchetFixedEntity extends TrebuchetMobileEntity {
   private static final Component MENU_TITLE = Component.translatable("container.ancientwarfare.trebuchet_fixed");

   public TrebuchetFixedEntity(EntityType<? extends TrebuchetFixedEntity> type, Level level) {
      super(type, level);
   }

   @Override
   public void setVehicleInput(float forward, float turn) {
      if (Math.abs(forward) > 1.0E-4F) {
         this.adjustLaunchVelocity(forward * 0.01F);
      }

      this.inputForward = 0.0F;
      this.inputTurn = 0.0F;
   }

   @Override
   protected void updateVehicleMotion() {
      this.setDeltaMovement(Vec3.ZERO);
   }

   @Override
   protected Component getMenuTitle() {
      return MENU_TITLE;
   }

   @Override
   protected Vec3 getPassengerOffset() {
      return new Vec3(0.0, 0.35, 1.425);
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
      ItemStack stack = new ItemStack(TrebuchetFixedItem.itemForMaterialLevel(this.getMaterialLevel()));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.getOrCreateTag().put("TrebuchetFixedData", tag);
      return stack;
   }

   @Override
   public double muzzleHeight() {
      return 7.84375;
   }
}
