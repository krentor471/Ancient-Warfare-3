package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.item.CannonMobileItem;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.object.PlayState;

public class CannonMobileEntity extends CannonTurretStandEntity {
   private static final Component MENU_TITLE = Component.m_237115_("container.ancientwarfare.cannon_mobile");
   private static final RawAnimation WHEEL_ROLL = RawAnimation.begin().thenLoop("animation.ancientwarfare.cannon_mobile.wheel_roll");

   public CannonMobileEntity(EntityType<? extends CannonMobileEntity> type, Level level) {
      super(type, level);
      this.m_20242_(false);
   }

   @Override
   protected boolean isMobileCannon() {
      return true;
   }

   @Override
   public void m_8119_() {
      this.m_20242_(false);
      super.m_8119_();
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController[]{new AnimationController(this, "wheel", 0, this::wheelAnimation)});
      super.registerControllers(controllers);
   }

   private PlayState wheelAnimation(AnimationState<CannonMobileEntity> state) {
      if (this.m_20184_().m_165925_() > 4.0E-4) {
         state.setAnimation(WHEEL_ROLL);
         return PlayState.CONTINUE;
      } else {
         return PlayState.STOP;
      }
   }

   @Override
   protected Component getMenuTitle() {
      return MENU_TITLE;
   }

   @Override
   protected Vec3 getPassengerOffset() {
      return new Vec3(0.0, 0.35, -2.45);
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
      ItemStack stack = new ItemStack(CannonMobileItem.itemForMaterialLevel(this.getMaterialLevel()));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.m_41784_().m_128365_("CannonMobileData", tag);
      return stack;
   }
}
