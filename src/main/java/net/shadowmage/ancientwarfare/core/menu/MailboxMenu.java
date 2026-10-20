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
         this.m_38897_(new Slot(b.sendContainer(), i, 8 + i % 9 * 18, 20 + i / 9 * 18) {
            public boolean m_5857_(ItemStack stack) {
               return false;
            }
         });
      }

      for (int i = 0; i < 18; i++) {
         this.m_38897_(new Slot(b.receivedContainer(), i, 8 + i % 9 * 18, 68 + i / 9 * 18));
      }

      for (int r = 0; r < 3; r++) {
         for (int c = 0; c < 9; c++) {
            this.m_38897_(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 116 + r * 18));
         }
      }

      for (int c = 0; c < 9; c++) {
         this.m_38897_(new Slot(inv, c, 8 + c * 18, 174));
      }
   }

   public boolean m_6366_(Player p, int id) {
      if (id == 0 && !p.m_9236_().f_46443_) {
         AWCorePlatform.openMenu(
            p,
            new SimpleMenuProvider(
               (cid, inv, pl) -> new WorksiteSideConfigMenu(cid, inv, this.box.m_58899_()),
               Component.m_237115_("container.ancientwarfare.worksite_side_config")
            ),
            buf -> buf.m_130064_(this.box.m_58899_())
         );
         return true;
      } else {
         return super.m_6366_(p, id);
      }
   }

   public boolean m_6875_(Player p) {
      return this.box.stillValid(p);
   }

   public ItemStack m_7648_(Player p, int i) {
      if (i >= 0 && i < this.f_38839_.size() && ((Slot)this.f_38839_.get(i)).m_6657_()) {
         ItemStack original = ((Slot)this.f_38839_.get(i)).m_7993_().m_41777_();
         ItemStack st = ((Slot)this.f_38839_.get(i)).m_7993_();
         if (i < 36) {
            if (!this.m_38903_(st, 36, this.f_38839_.size(), true)) {
               return ItemStack.f_41583_;
            }
         } else if (!this.m_38903_(st, 0, 36, false)) {
            return ItemStack.f_41583_;
         }

         ((Slot)this.f_38839_.get(i)).m_6654_();
         return original;
      } else {
         return ItemStack.f_41583_;
      }
   }
}
