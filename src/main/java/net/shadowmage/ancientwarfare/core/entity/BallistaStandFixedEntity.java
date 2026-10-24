package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.item.BallistaTurretStandItem;

public class BallistaStandFixedEntity extends BallistaTurretStandEntity {
   private static final Component MENU_TITLE = Component.translatable("container.ancientwarfare.ballista_stand_fixed");

   public BallistaStandFixedEntity(EntityType<? extends BallistaStandFixedEntity> type, Level level) {
      super(type, level);
   }

   @Override
   public void adjustTurret(float pitchDelta, float yawDelta) {
      super.adjustTurret(pitchDelta, 0.0F);
      this.setTurretYaw(0.0F);
   }

   @Override
   protected Component getMenuTitle() {
      return MENU_TITLE;
   }

   @Override
   protected ItemStack createPackedItem() {
      ItemStack stack = new ItemStack(BallistaTurretStandItem.itemForMaterialLevel(this.getMaterialLevel(), true));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.getOrCreateTag().put("BallistaTurretStandData", tag);
      return stack;
   }
}
