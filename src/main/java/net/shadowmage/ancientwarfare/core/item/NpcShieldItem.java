package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Item.Properties;

public class NpcShieldItem extends ShieldItem {
   private final Tier repairTier;

   public NpcShieldItem(Tier repairTier, int durability) {
      super(new Properties().durability(durability));
      this.repairTier = repairTier;
   }

   public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
      return this.repairTier.getRepairIngredient().test(repairCandidate) || super.isValidRepairItem(stack, repairCandidate);
   }
}
