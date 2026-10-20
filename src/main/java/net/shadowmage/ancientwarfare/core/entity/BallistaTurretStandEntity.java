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
   private static final Component MENU_TITLE = Component.m_237115_("container.ancientwarfare.ballista_turret_stand");
   private static final float MIN_PITCH = -15.0F;
   private static final float MAX_PITCH = 15.0F;
   private static final float MAX_YAW = 45.0F;
   private float inputPitch;
   private float inputYaw;

   public BallistaTurretStandEntity(EntityType<? extends BallistaTurretStandEntity> type, Level level) {
      super(type, level);
      this.m_20242_(true);
   }

   @Override
   public void setVehicleInput(float forward, float turn) {
      this.inputPitch = Math.max(-1.0F, Math.min(1.0F, forward));
      this.inputYaw = Math.max(-1.0F, Math.min(1.0F, turn));
   }

   @Override
   public void m_8119_() {
      this.m_20242_(true);
      double fixedX = this.m_20185_();
      double fixedY = this.m_20186_();
      double fixedZ = this.m_20189_();
      this.m_20256_(Vec3.f_82478_);
      super.m_8119_();
      if (!this.m_9236_().f_46443_ && (Math.abs(this.inputPitch) > 1.0E-4F || Math.abs(this.inputYaw) > 1.0E-4F)) {
         this.adjustTurret(this.inputPitch, this.inputYaw * 2.0F);
      }

      this.m_20256_(Vec3.f_82478_);
      this.m_7678_(fixedX, fixedY, fixedZ, this.m_146908_(), this.m_146909_());
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
   protected void m_19956_(Entity passenger, MoveFunction moveFunction) {
      if (this.m_20363_(passenger)) {
         Vec3 offset = this.getPassengerOffset().m_82524_(-(this.m_146908_() + this.getTurretYaw()) * (float) (Math.PI / 180.0));
         moveFunction.m_20372_(passenger, this.m_20185_() + offset.f_82479_, this.m_20186_() + offset.f_82480_, this.m_20189_() + offset.f_82481_);
      }
   }

   @Override
   protected ItemStack createPackedItem() {
      ItemStack stack = new ItemStack(BallistaTurretStandItem.itemForMaterialLevel(this.getMaterialLevel()));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.m_41784_().m_128365_("BallistaTurretStandData", tag);
      return stack;
   }
}
