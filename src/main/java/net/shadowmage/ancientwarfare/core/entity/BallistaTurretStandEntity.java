package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.item.BallistaTurretStandItem;

public class BallistaTurretStandEntity extends BallistaMobileEntity {
   private static final Component MENU_TITLE = Component.translatable("container.ancientwarfare.ballista_turret_stand");
   private static final float MIN_PITCH = -15.0F;
   private static final float MAX_PITCH = 15.0F;
   private static final float MAX_YAW = 45.0F;
   private float inputPitch;
   private float inputYaw;

   public BallistaTurretStandEntity(EntityType<? extends BallistaTurretStandEntity> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   @Override
   public void setVehicleInput(float forward, float turn) {
      this.inputPitch = Math.max(-1.0F, Math.min(1.0F, forward));
      this.inputYaw = Math.max(-1.0F, Math.min(1.0F, turn));
   }

   @Override
   public void tick() {
      this.setNoGravity(true);
      double fixedX = this.getX();
      double fixedY = this.getY();
      double fixedZ = this.getZ();
      this.setDeltaMovement(Vec3.ZERO);
      super.tick();
      if (!this.level().isClientSide && (Math.abs(this.inputPitch) > 1.0E-4F || Math.abs(this.inputYaw) > 1.0E-4F)) {
         this.adjustTurret(this.inputPitch, this.inputYaw * 2.0F);
      }

      this.setDeltaMovement(Vec3.ZERO);
      this.moveTo(fixedX, fixedY, fixedZ, this.getYRot(), this.getXRot());
   }

   @Override
   public void adjustTurret(float pitchDelta, float yawDelta) {
      this.setTurretPitch(clamp(this.getTurretPitch() + pitchDelta, -15.0F, 15.0F));
      this.setTurretYaw(clamp(this.getTurretYaw() + yawDelta, -45.0F, 45.0F));
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   @Override
   protected Component getMenuTitle() {
      return MENU_TITLE;
   }

   @Override
   protected Vec3 getPassengerOffset() {
      return new Vec3(0.0, 0.35, -1.8);
   }

   @Override
   public double muzzleHeight() {
      return 1.125;
   }

   @Override
   protected double muzzleForwardOffset() {
      return 0.0;
   }

   @Override
   protected void positionRider(Entity passenger, MoveFunction moveFunction) {
      if (this.hasPassenger(passenger)) {
         Vec3 offset = this.getPassengerOffset().yRot(-(this.getYRot() + this.getTurretYaw()) * (float) (Math.PI / 180.0));
         moveFunction.accept(passenger, this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);
      }
   }

   @Override
   protected ItemStack createPackedItem() {
      ItemStack stack = new ItemStack(BallistaTurretStandItem.itemForMaterialLevel(this.getMaterialLevel()));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.getOrCreateTag().put("BallistaTurretStandData", tag);
      return stack;
   }
}
