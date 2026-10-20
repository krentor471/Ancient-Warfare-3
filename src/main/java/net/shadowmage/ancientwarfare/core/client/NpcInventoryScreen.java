package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.menu.NpcInventoryMenu;

public class NpcInventoryScreen extends AbstractContainerScreen<NpcInventoryMenu> {
   private static final ResourceLocation BACKGROUND = new ResourceLocation("ancientwarfare", "textures/gui/guibackgroundlarge.png");
   private static final ResourceLocation WIDGETS = new ResourceLocation("ancientwarfare", "textures/gui/guibuttons1.png");
   private static final int TEXT_COLOR = 16777215;
   private static final int MUTED_TEXT_COLOR = -1513240;
   private static final int PLAYER_INV_TOP = 124;
   private static final int BUTTON_X = 84;
   private static final int BUTTON_WIDTH = 75;
   private static final int SKIN_BUTTON_X = 75;
   private static final int SKIN_BUTTON_WIDTH = 95;
   private static final int BUTTON_HEIGHT = 12;
   private static final int BUTTON_SKIN = -100;
   private int pressedButton = -1;

   public NpcInventoryScreen(NpcInventoryMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.f_97726_ = 178;
      this.f_97727_ = 202;
      this.f_97730_ = 8;
      this.f_97731_ = 10000;
   }

   protected void m_7286_(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.m_280218_(BACKGROUND, this.f_97735_, this.f_97736_, 0, 0, this.f_97726_, this.f_97727_);
      this.drawDecorativeSlot(graphics, 8, 8);
      this.drawDecorativeSlot(graphics, 8, 26);
      this.drawDecorativeSlot(graphics, 8, 44);
      this.drawDecorativeSlot(graphics, 8, 62);
      this.drawDecorativeSlot(graphics, 8, 80);
      this.drawDecorativeSlot(graphics, 8, 98);
      this.drawDecorativeSlot(graphics, 44, 44);
      this.drawDecorativeSlot(graphics, 44, 62);
      this.drawDecorativeSlot(graphics, 44, 80);
      this.drawLegacySlotHints(graphics);
      this.drawPlayerInventorySlots(graphics);
      graphics.m_280509_(this.f_97735_ + 75, this.f_97736_ + 8, this.f_97735_ + 170, this.f_97736_ + 20, -16777216);
      this.drawTexturedButton(graphics, 75, 20, 95, 12, ((NpcInventoryMenu)this.f_97732_).getSkinButtonLabel(), this.pressedButton == -100);
      Component followButtonLabel = Component.m_237115_(
         ((NpcInventoryMenu)this.f_97732_).isFollowing() ? "guistrings.npc.stop_follow" : "guistrings.npc.togglefollow"
      );
      this.drawTexturedButton(graphics, 84, 36, 75, 12, followButtonLabel, this.pressedButton == 0);
      Component homeButtonLabel = Component.m_237115_(((NpcInventoryMenu)this.f_97732_).hasHome() ? "guistrings.npc.clear_home" : "guistrings.npc.set_home");
      this.drawTexturedButton(graphics, 84, 48, 75, 12, homeButtonLabel, this.pressedButton == 1);
      int nextButtonY = 60;
      if (((NpcInventoryMenu)this.f_97732_).canOpenTradeOrderGui()) {
         this.drawTexturedButton(graphics, 84, nextButtonY, 75, 12, Component.m_237115_("guistrings.npc.trade_order.edit"), this.pressedButton == 6);
         nextButtonY += 12;
      }

      if (((NpcInventoryMenu)this.f_97732_).isBard()) {
         this.drawTexturedButton(graphics, 84, nextButtonY, 75, 12, Component.m_237115_("guistrings.npc.advanced_gui"), this.pressedButton == 5);
         nextButtonY += 12;
      }

      if (((NpcInventoryMenu)this.f_97732_).canRepack(this.f_96541_.f_91074_)) {
         this.drawTexturedButton(graphics, 84, nextButtonY, 75, 12, Component.m_237115_("guistrings.npc.repack"), this.pressedButton == 3);
         nextButtonY += 12;
      }

      if (((NpcInventoryMenu)this.f_97732_).canOpenCreativeGui(this.f_96541_.f_91074_)) {
         this.drawTexturedButton(graphics, 84, nextButtonY, 75, 12, Component.m_237115_("guistrings.npc.creative_gui"), this.pressedButton == 4);
      }
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.npc.npc_name"), 48, 9, 16777215, false);
      this.drawFittedString(graphics, ((NpcInventoryMenu)this.f_97732_).getNpcDisplayName(), 77, 9, 91);
      graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.npc.npc_texture"), 48, 21, 16777215, false);
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(graphics);
      super.m_88315_(graphics, mouseX, mouseY, partialTick);
      this.m_280072_(graphics, mouseX, mouseY);
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (button == 0) {
         int localX = (int)mouseX - this.f_97735_;
         int localY = (int)mouseY - this.f_97736_;
         int clicked = this.getButtonAt(localX, localY);
         if (clicked == -100) {
            this.pressedButton = clicked;
            this.f_96541_.m_91106_().m_120367_(SimpleSoundInstance.m_263171_(SoundEvents.f_12490_, 1.0F));
            this.f_96541_
               .m_91152_(
                  new NpcSkinSelectionScreen(
                     this,
                     ((NpcInventoryMenu)this.f_97732_).getNpcEntityId(),
                     ((NpcInventoryMenu)this.f_97732_).getSkinName(),
                     ((NpcInventoryMenu)this.f_97732_).isAlexModel()
                  )
               );
            return true;
         }

         if (clicked >= 0) {
            this.pressedButton = clicked;
            this.f_96541_.m_91106_().m_120367_(SimpleSoundInstance.m_263171_(SoundEvents.f_12490_, 1.0F));
            if (this.f_96541_.f_91072_ != null) {
               this.f_96541_.f_91072_.m_105208_(((NpcInventoryMenu)this.f_97732_).f_38840_, clicked);
            }

            return true;
         }
      }

      return super.m_6375_(mouseX, mouseY, button);
   }

   public boolean m_6348_(double mouseX, double mouseY, int button) {
      this.pressedButton = -1;
      return super.m_6348_(mouseX, mouseY, button);
   }

   private int getButtonAt(int x, int y) {
      if (this.inside(x, y, 75, 20, 95, 12)) {
         return -100;
      } else if (this.inside(x, y, 84, 36, 75, 12)) {
         return 0;
      } else if (this.inside(x, y, 84, 48, 75, 12)) {
         return 1;
      } else {
         int nextButtonY = 60;
         if (((NpcInventoryMenu)this.f_97732_).canOpenTradeOrderGui()) {
            if (this.inside(x, y, 84, nextButtonY, 75, 12)) {
               return 6;
            }

            nextButtonY += 12;
         }

         if (((NpcInventoryMenu)this.f_97732_).isBard()) {
            if (this.inside(x, y, 84, nextButtonY, 75, 12)) {
               return 5;
            }

            nextButtonY += 12;
         }

         if (((NpcInventoryMenu)this.f_97732_).canRepack(this.f_96541_.f_91074_)) {
            if (this.inside(x, y, 84, nextButtonY, 75, 12)) {
               return 3;
            }

            nextButtonY += 12;
         }

         return ((NpcInventoryMenu)this.f_97732_).canOpenCreativeGui(this.f_96541_.f_91074_) && this.inside(x, y, 84, nextButtonY, 75, 12) ? 4 : -1;
      }
   }

   private boolean inside(int x, int y, int left, int top, int width, int height) {
      return x >= left && y >= top && x < left + width && y < top + height;
   }

   private void drawPlayerInventorySlots(GuiGraphics graphics) {
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.drawDecorativeSlot(graphics, 8 + col * 18, 124 + row * 18);
         }
      }

      for (int col = 0; col < 9; col++) {
         this.drawDecorativeSlot(graphics, 8 + col * 18, 182);
      }
   }

   private void drawLegacySlotHints(GuiGraphics graphics) {
      ItemStack toolOrWeapon = new ItemStack(((NpcInventoryMenu)this.f_97732_).isCombatNpc() ? Items.f_42383_ : Items.f_42385_);
      this.drawHintItem(graphics, toolOrWeapon, 26, 8);
      this.drawHintItem(graphics, new ItemStack(AWCoreRegistryAccess.item(AWCoreIds.WOODEN_SHIELD)), 26, 26);
      this.drawHintItem(graphics, new ItemStack(Items.f_42468_), 26, 44);
      this.drawHintItem(graphics, new ItemStack(Items.f_42469_), 26, 62);
      this.drawHintItem(graphics, new ItemStack(Items.f_42470_), 26, 80);
      this.drawHintItem(graphics, new ItemStack(Items.f_42471_), 26, 98);
      this.drawHintItem(graphics, new ItemStack(AWCoreRegistryAccess.item(AWCoreIds.ROUTING_ORDER)), 64, 44);
      this.drawHintItem(graphics, new ItemStack(AWCoreRegistryAccess.item(AWCoreIds.UPKEEP_ORDER)), 64, 62);
      this.drawHintItem(graphics, new ItemStack(Items.f_42406_), 64, 80);
   }

   private void drawHintItem(GuiGraphics graphics, ItemStack stack, int x, int y) {
      graphics.m_280480_(stack, this.f_97735_ + x, this.f_97736_ + y);
   }

   private void drawDecorativeSlot(GuiGraphics graphics, int x, int y) {
      graphics.m_280218_(WIDGETS, this.f_97735_ + x - 1, this.f_97736_ + y - 1, 152, 120, 18, 18);
   }

   private void drawTexturedButton(GuiGraphics graphics, int x, int y, int width, int height, Component label, boolean pressed) {
      this.drawNineSlice(graphics, x, y, width, height, 0, pressed ? 80 : 40, 256, 40);
      int textWidth = this.f_96547_.m_92852_(label);
      if (textWidth <= width - 6) {
         graphics.m_280614_(this.f_96547_, label, this.f_97735_ + x + (width - textWidth) / 2, this.f_97736_ + y + (height - 8) / 2, 16777215, false);
      } else {
         float scale = (float)(width - 6) / (float)textWidth;
         graphics.m_280168_().m_85836_();
         graphics.m_280168_().m_252880_((float)(this.f_97735_ + x + 3), (float)(this.f_97736_ + y + 4), 0.0F);
         graphics.m_280168_().m_85841_(scale, scale, 1.0F);
         graphics.m_280614_(this.f_96547_, label, 0, 0, 16777215, false);
         graphics.m_280168_().m_85849_();
      }
   }

   private void drawFittedString(GuiGraphics graphics, Component text, int x, int y, int maxWidth) {
      int textWidth = this.f_96547_.m_92852_(text);
      if (textWidth <= maxWidth) {
         graphics.m_280614_(this.f_96547_, text, x, y, 16777215, false);
      } else {
         float scale = (float)maxWidth / (float)textWidth;
         graphics.m_280168_().m_85836_();
         graphics.m_280168_().m_252880_((float)x, (float)(y + 1), 0.0F);
         graphics.m_280168_().m_85841_(scale, scale, 1.0F);
         graphics.m_280614_(this.f_96547_, text, 0, 0, 16777215, false);
         graphics.m_280168_().m_85849_();
      }
   }

   private void drawSmallLabel(GuiGraphics graphics, Component text, int x, int y, int maxWidth) {
      int textWidth = this.f_96547_.m_92852_(text);
      float scale = textWidth <= maxWidth ? 0.65F : Math.min(0.65F, (float)maxWidth / (float)textWidth);
      graphics.m_280168_().m_85836_();
      graphics.m_280168_().m_252880_((float)x, (float)y, 0.0F);
      graphics.m_280168_().m_85841_(scale, scale, 1.0F);
      graphics.m_280614_(this.f_96547_, text, 0, 0, -1513240, false);
      graphics.m_280168_().m_85849_();
   }

   private void drawNineSlice(GuiGraphics graphics, int x, int y, int width, int height, int u, int v, int texWidth, int texHeight) {
      int halfWidth = width / 2;
      int halfHeight = height / 2;
      graphics.m_280218_(WIDGETS, this.f_97735_ + x, this.f_97736_ + y, u, v, halfWidth, halfHeight);
      graphics.m_280218_(WIDGETS, this.f_97735_ + x + halfWidth, this.f_97736_ + y, u + texWidth - (width - halfWidth), v, width - halfWidth, halfHeight);
      graphics.m_280218_(WIDGETS, this.f_97735_ + x, this.f_97736_ + y + halfHeight, u, v + texHeight - (height - halfHeight), halfWidth, height - halfHeight);
      graphics.m_280218_(
         WIDGETS,
         this.f_97735_ + x + halfWidth,
         this.f_97736_ + y + halfHeight,
         u + texWidth - (width - halfWidth),
         v + texHeight - (height - halfHeight),
         width - halfWidth,
         height - halfHeight
      );
   }
}
