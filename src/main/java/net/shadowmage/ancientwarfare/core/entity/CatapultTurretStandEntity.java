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
   private static final Component MENU_TITLE = Component.m_237115_("container.ancientwarfare.catapult_turret_stand");
   private static final Component FIXED_MENU_TITLE = Component.m_237115_("container.ancientwarfare.catapult_stand_fixed");
   private static final float YAW_SPEED = 2.0F;
   private static final float VELOCITY_STEP = 0.01F;

   public CatapultTurretStandEntity(EntityType<? extends CatapultTurretStandEntity> type, Level level) {
      super(type, level);
      this.m_20242_(true);
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
   public void m_8119_() {
      this.m_20242_(true);
      double fixedX = this.m_20185_();
      double fixedY = this.m_20186_();
      double fixedZ = this.m_20189_();
      this.m_20256_(Vec3.f_82478_);
      super.m_8119_();
      this.m_20256_(Vec3.f_82478_);
      this.m_7678_(fixedX, fixedY, fixedZ, this.m_146908_(), this.m_146909_());
   }

   @Override
   protected Component getMenuTitle() {
      return this.hasFixedTurret() ? FIXED_MENU_TITLE : MENU_TITLE;
   }

   @Override
   protected Vec3 getPassengerOffset() {
      return this.hasFixedTurret() ? new Vec3(0.0, 0.0, 1.2) : new Vec3(0.0, 0.55, 1.2);
   }

   public EntityDimensions m_6972_(Pose pose) {
      return this.hasFixedTurret() ? EntityDimensions.m_20395_(2.0F, 1.7F) : super.m_6972_(pose);
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
      ItemStack stack = new ItemStack(CatapultTurretStandItem.itemForMaterialLevel(this.getMaterialLevel(), this.hasFixedTurret()));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.m_41784_().m_128365_("CatapultTurretStandData", tag);
      return stack;
   }

   @Override
   public double muzzleHeight() {
      return this.hasFixedTurret() ? 0.1875 : 0.4;
   }
}
