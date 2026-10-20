package net.shadowmage.ancientwarfare.core.menu;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.npc.CombatOrderData;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public class CombatOrderMenu extends AbstractContainerMenu {
   private static final int ENTRY_BUTTON_BASE = 10;
   private final Player player;
   private final InteractionHand hand;

   public CombatOrderMenu(int containerId, Inventory playerInventory, InteractionHand hand) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.COMBAT_ORDER_MENU), containerId);
      this.player = playerInventory.f_35978_;
      this.hand = hand;
   }

   public ItemStack getCombatOrderStack() {
      return this.player.m_21120_(this.hand);
   }

   public List<BlockPos> getEntries() {
      return CombatOrderData.getEntries(this.getCombatOrderStack());
   }

   public boolean m_6366_(Player player, int id) {
      ItemStack stack = this.getCombatOrderStack();
      if (!AWCoreRegistryAccess.is(stack, AWCoreIds.COMBAT_ORDER)) {
         return false;
      } else {
         return id >= 10 ? CombatOrderData.removeEntry(stack, id - 10) : false;
      }
   }

   public boolean m_6875_(Player player) {
      return AWCoreRegistryAccess.is(player.m_21120_(this.hand), AWCoreIds.COMBAT_ORDER);
   }

   public static int removeButton(int index) {
      return 10 + index;
   }

   public ItemStack m_7648_(Player player, int index) {
      return ItemStack.f_41583_;
   }
}
