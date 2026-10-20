package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Item.Properties;

public class NpcShieldItem extends ShieldItem {
   private final Tier repairTier;

   public NpcShieldItem(Tier repairTier, int durability) {
      super(new Properties().m_41503_(durability));
      this.repairTier = repairTier;
   }

   public boolean m_6832_(ItemStack stack, ItemStack repairCandidate) {
      return this.repairTier.m_6282_().test(repairCandidate) || super.m_6832_(stack, repairCandidate);
   }
}
