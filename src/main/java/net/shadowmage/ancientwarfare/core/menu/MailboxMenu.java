package net.shadowmage.ancientwarfare.core.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.tile.MailboxBlockEntity;

public class MailboxMenu extends AbstractContainerMenu {
   public final MailboxBlockEntity box;
   public static final int SIDES_BUTTON = 0;

   public MailboxMenu(int id, Inventory inv, MailboxBlockEntity b) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.MAILBOX), id);
      this.box = b;

      for (int i = 0; i < 18; i++) {
         this.addSlot(new Slot(b.sendContainer(), i, 8 + i % 9 * 18, 20 + i / 9 * 18) {
            public boolean mayPlace(ItemStack stack) {
               return false;
            }
         });
      }

      for (int i = 0; i < 18; i++) {
         this.addSlot(new Slot(b.receivedContainer(), i, 8 + i % 9 * 18, 68 + i / 9 * 18));
      }

      for (int r = 0; r < 3; r++) {
         for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 116 + r * 18));
         }
      }

      for (int c = 0; c < 9; c++) {
         this.addSlot(new Slot(inv, c, 8 + c * 18, 174));
      }
   }

   public boolean clickMenuButton(Player p, int id) {
      if (id == 0 && !p.level().isClientSide) {
         AWCorePlatform.openMenu(
            p,
            new SimpleMenuProvider(
               (cid, inv, pl) -> new WorksiteSideConfigMenu(cid, inv, this.box.getBlockPos()),
               Component.translatable("container.ancientwarfare.worksite_side_config")
            ),
            buf -> buf.writeBlockPos(this.box.getBlockPos())
         );
         return true;
      } else {
         return super.clickMenuButton(p, id);
      }
   }

   public boolean stillValid(Player p) {
      return this.box.stillValid(p);
   }

   public ItemStack quickMoveStack(Player p, int i) {
      if (i >= 0 && i < this.slots.size() && ((Slot)this.slots.get(i)).hasItem()) {
         ItemStack original = ((Slot)this.slots.get(i)).getItem().copy();
         ItemStack st = ((Slot)this.slots.get(i)).getItem();
         if (i < 36) {
            if (!this.moveItemStackTo(st, 36, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(st, 0, 36, false)) {
            return ItemStack.EMPTY;
         }

         ((Slot)this.slots.get(i)).setChanged();
         return original;
      } else {
         return ItemStack.EMPTY;
      }
   }
}
