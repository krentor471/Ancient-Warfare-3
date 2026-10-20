package net.shadowmage.ancientwarfare.core.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.item.ResearchBookItem;

public class ResearchBookMenu extends AbstractContainerMenu {
   private final Inventory playerInventory;

   public ResearchBookMenu(int containerId, Inventory playerInventory) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.RESEARCH_BOOK), containerId);
      this.playerInventory = playerInventory;
   }

   public ItemStack getResearchBook() {
      ItemStack mainHand = this.playerInventory.f_35978_.m_21205_();
      if (AWCoreRegistryAccess.is(mainHand, AWCoreIds.RESEARCH_BOOK)) {
         return mainHand;
      } else {
         ItemStack offHand = this.playerInventory.f_35978_.m_21206_();
         return AWCoreRegistryAccess.is(offHand, AWCoreIds.RESEARCH_BOOK) ? offHand : ItemStack.f_41583_;
      }
   }

   public String getResearcherName() {
      String name = ResearchBookItem.getResearcherName(this.getResearchBook());
      return name == null ? "" : name;
   }

   public ItemStack m_7648_(Player player, int index) {
      return ItemStack.f_41583_;
   }

   public boolean m_6875_(Player player) {
      return !this.getResearchBook().m_41619_();
   }
}
