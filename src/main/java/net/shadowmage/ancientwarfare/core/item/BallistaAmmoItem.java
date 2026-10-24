package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;

public class BallistaAmmoItem extends Item {
   private final BallistaAmmoType ammoType;

   public BallistaAmmoItem(BallistaAmmoType ammoType) {
      super(new Properties().stacksTo(16));
      this.ammoType = ammoType;
   }

   public BallistaAmmoType ammoType() {
      return this.ammoType;
   }

   public static BallistaAmmoType ammoType(ItemStack stack) {
      return stack.getItem() instanceof BallistaAmmoItem ammo ? ammo.ammoType() : null;
   }
}
