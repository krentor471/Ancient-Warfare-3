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
   private static final Component MENU_TITLE = Component.m_237115_("container.ancientwarfare.transport_boat");

   public TransportBoatEntity(EntityType<? extends TransportBoatEntity> type, Level level) {
      super(type, level);
      this.m_20242_(true);
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
   }

   @Override
   protected void updateVehicleMotion() {
      boolean inWater = this.m_20069_() || this.m_9236_().m_6425_(this.m_20183_()).m_205070_(FluidTags.f_13131_);
      LivingEntity rider = this.m_6688_();
      double yMotion = inWater ? 0.0 : -0.04;
      if (this.m_20160_() && rider != null && inWater) {
         if (Math.abs(this.inputTurn) > 1.0E-4F) {
            this.m_146922_(this.m_146908_() - this.inputTurn * 3.0F);
            this.m_5616_(this.m_146908_());
            this.m_5618_(this.m_146908_());
         }

         this.f_19859_ = this.m_146908_();
         if (Math.abs(this.inputForward) > 1.0E-4F) {
            double speed = this.inputForward > 0.0F ? 0.13 : 0.055;
            double yaw = Math.toRadians((double)this.m_146908_());
            double motion = (double)this.inputForward * speed;
            this.m_20334_(-Math.sin(yaw) * motion, yMotion, Math.cos(yaw) * motion);
            this.f_19812_ = true;
         } else {
            this.m_20256_(this.m_20184_().m_82542_(0.72, 0.0, 0.72));
         }
      } else {
         this.m_20256_(this.m_20184_().m_82542_(0.8, 1.0, 0.8).m_82520_(0.0, yMotion, 0.0));
      }

      this.m_6478_(MoverType.SELF, this.m_20184_());
      this.m_20256_(this.m_20184_().m_82542_(inWater ? 0.92 : 0.7, inWater ? 0.0 : 0.8, inWater ? 0.92 : 0.7));
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
   protected void m_19956_(Entity passenger, MoveFunction moveFunction) {
      if (this.m_20363_(passenger)) {
         Vec3 offset = this.getPassengerOffset().m_82524_(-this.m_146908_() * (float) (Math.PI / 180.0));
         moveFunction.m_20372_(passenger, this.m_20185_() + offset.f_82479_, this.m_20186_() + offset.f_82480_, this.m_20189_() + offset.f_82481_);
      }
   }

   @Override
   protected ItemStack createPackedItem() {
      ItemStack stack = new ItemStack(TransportBoatItem.itemForMaterialLevel(this.getMaterialLevel()));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.m_41784_().m_128365_("TransportBoatData", tag);
      return stack;
   }
}
