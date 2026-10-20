package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.item.CatapultAmmoType;
import net.shadowmage.ancientwarfare.core.item.TrebuchetMobileItem;

public class TrebuchetMobileEntity extends CatapultMobileEntity {
   private static final Component MENU_TITLE = Component.m_237115_("container.ancientwarfare.trebuchet_mobile");

   public TrebuchetMobileEntity(EntityType<? extends TrebuchetMobileEntity> type, Level level) {
      super(type, level);
      this.setFixedTurret(true);
   }

   @Override
   protected boolean canFireAmmo(CatapultAmmoType ammoType) {
      return ammoType.weight() <= 30;
   }

   @Override
   public void setFixedTurret(boolean fixedTurret) {
      super.setFixedTurret(true);
   }

   @Override
   protected Component getMenuTitle() {
      return MENU_TITLE;
   }

   @Override
   protected Vec3 getPassengerOffset() {
      return new Vec3(0.0, 0.65, 1.275);
   }

   @Override
   public double muzzleHeight() {
      return 8.59375;
   }

   @Override
   protected double muzzleForwardOffset() {
      return 0.0;
   }

   @Override
   protected ItemStack createPackedItem() {
      ItemStack stack = new ItemStack(TrebuchetMobileItem.itemForMaterialLevel(this.getMaterialLevel()));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.m_41784_().m_128365_("TrebuchetMobileData", tag);
      return stack;
   }
}
