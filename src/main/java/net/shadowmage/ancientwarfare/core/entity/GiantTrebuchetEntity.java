package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.item.CatapultAmmoType;
import net.shadowmage.ancientwarfare.core.item.GiantTrebuchetItem;

public class GiantTrebuchetEntity extends TrebuchetFixedEntity {
   private static final Component MENU_TITLE = Component.m_237115_("container.ancientwarfare.giant_trebuchet");

   public GiantTrebuchetEntity(EntityType<? extends GiantTrebuchetEntity> type, Level level) {
      super(type, level);
   }

   @Override
   protected boolean canFireAmmo(CatapultAmmoType ammoType) {
      return ammoType.weight() <= 45;
   }

   @Override
   protected Component getMenuTitle() {
      return MENU_TITLE;
   }

   @Override
   protected Vec3 getPassengerOffset() {
      return new Vec3(0.0, 0.35, 3.5625);
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
      ItemStack stack = new ItemStack(GiantTrebuchetItem.itemForMaterialLevel(this.getMaterialLevel()));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.m_41784_().m_128365_("GiantTrebuchetData", tag);
      return stack;
   }

   @Override
   public double muzzleHeight() {
      return 19.609375;
   }
}
