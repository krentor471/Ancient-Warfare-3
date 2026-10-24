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
      this.imageWidth = 178;
      this.imageHeight = 202;
      this.inventoryLabelX = 8;
      this.inventoryLabelY = 10000;
   }

   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
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
      graphics.fill(this.leftPos + 75, this.topPos + 8, this.leftPos + 170, this.topPos + 20, -16777216);
      this.drawTexturedButton(graphics, 75, 20, 95, 12, ((NpcInventoryMenu)this.menu).getSkinButtonLabel(), this.pressedButton == -100);
      Component followButtonLabel = Component.translatable(
         ((NpcInventoryMenu)this.menu).isFollowing() ? "guistrings.npc.stop_follow" : "guistrings.npc.togglefollow"
      );
      this.drawTexturedButton(graphics, 84, 36, 75, 12, followButtonLabel, this.pressedButton == 0);
      Component homeButtonLabel = Component.translatable(((NpcInventoryMenu)this.menu).hasHome() ? "guistrings.npc.clear_home" : "guistrings.npc.set_home");
      this.drawTexturedButton(graphics, 84, 48, 75, 12, homeButtonLabel, this.pressedButton == 1);
      int nextButtonY = 60;
      if (((NpcInventoryMenu)this.menu).canOpenTradeOrderGui()) {
         this.drawTexturedButton(graphics, 84, nextButtonY, 75, 12, Component.translatable("guistrings.npc.trade_order.edit"), this.pressedButton == 6);
         nextButtonY += 12;
      }

      if (((NpcInventoryMenu)this.menu).isBard()) {
         this.drawTexturedButton(graphics, 84, nextButtonY, 75, 12, Component.translatable("guistrings.npc.advanced_gui"), this.pressedButton == 5);
         nextButtonY += 12;
      }

      if (((NpcInventoryMenu)this.menu).canRepack(this.minecraft.player)) {
         this.drawTexturedButton(graphics, 84, nextButtonY, 75, 12, Component.translatable("guistrings.npc.repack"), this.pressedButton == 3);
         nextButtonY += 12;
      }

      if (((NpcInventoryMenu)this.menu).canOpenCreativeGui(this.minecraft.player)) {
         this.drawTexturedButton(graphics, 84, nextButtonY, 75, 12, Component.translatable("guistrings.npc.creative_gui"), this.pressedButton == 4);
      }
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(this.font, Component.translatable("guistrings.npc.npc_name"), 48, 9, 16777215, false);
      this.drawFittedString(graphics, ((NpcInventoryMenu)this.menu).getNpcDisplayName(), 77, 9, 91);
      graphics.drawString(this.font, Component.translatable("guistrings.npc.npc_texture"), 48, 21, 16777215, false);
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.renderTooltip(graphics, mouseX, mouseY);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         int localX = (int)mouseX - this.leftPos;
         int localY = (int)mouseY - this.topPos;
         int clicked = this.getButtonAt(localX, localY);
         if (clicked == -100) {
            this.pressedButton = clicked;
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.minecraft
               .setScreen(
                  new NpcSkinSelectionScreen(
                     this,
                     ((NpcInventoryMenu)this.menu).getNpcEntityId(),
                     ((NpcInventoryMenu)this.menu).getSkinName(),
                     ((NpcInventoryMenu)this.menu).isAlexModel()
                  )
               );
            return true;
         }

         if (clicked >= 0) {
            this.pressedButton = clicked;
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (this.minecraft.gameMode != null) {
               this.minecraft.gameMode.handleInventoryButtonClick(((NpcInventoryMenu)this.menu).containerId, clicked);
            }

            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.pressedButton = -1;
      return super.mouseReleased(mouseX, mouseY, button);
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
         if (((NpcInventoryMenu)this.menu).canOpenTradeOrderGui()) {
            if (this.inside(x, y, 84, nextButtonY, 75, 12)) {
               return 6;
            }

            nextButtonY += 12;
         }

         if (((NpcInventoryMenu)this.menu).isBard()) {
            if (this.inside(x, y, 84, nextButtonY, 75, 12)) {
               return 5;
            }

            nextButtonY += 12;
         }

         if (((NpcInventoryMenu)this.menu).canRepack(this.minecraft.player)) {
            if (this.inside(x, y, 84, nextButtonY, 75, 12)) {
               return 3;
            }

            nextButtonY += 12;
         }

         return ((NpcInventoryMenu)this.menu).canOpenCreativeGui(this.minecraft.player) && this.inside(x, y, 84, nextButtonY, 75, 12) ? 4 : -1;
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
      ItemStack toolOrWeapon = new ItemStack(((NpcInventoryMenu)this.menu).isCombatNpc() ? Items.IRON_SWORD : Items.IRON_PICKAXE);
      this.drawHintItem(graphics, toolOrWeapon, 26, 8);
      this.drawHintItem(graphics, new ItemStack(AWCoreRegistryAccess.item(AWCoreIds.WOODEN_SHIELD)), 26, 26);
      this.drawHintItem(graphics, new ItemStack(Items.IRON_HELMET), 26, 44);
      this.drawHintItem(graphics, new ItemStack(Items.IRON_CHESTPLATE), 26, 62);
      this.drawHintItem(graphics, new ItemStack(Items.IRON_LEGGINGS), 26, 80);
      this.drawHintItem(graphics, new ItemStack(Items.IRON_BOOTS), 26, 98);
      this.drawHintItem(graphics, new ItemStack(AWCoreRegistryAccess.item(AWCoreIds.ROUTING_ORDER)), 64, 44);
      this.drawHintItem(graphics, new ItemStack(AWCoreRegistryAccess.item(AWCoreIds.UPKEEP_ORDER)), 64, 62);
      this.drawHintItem(graphics, new ItemStack(Items.BREAD), 64, 80);
   }

   private void drawHintItem(GuiGraphics graphics, ItemStack stack, int x, int y) {
      graphics.renderItem(stack, this.leftPos + x, this.topPos + y);
   }

   private void drawDecorativeSlot(GuiGraphics graphics, int x, int y) {
      graphics.blit(WIDGETS, this.leftPos + x - 1, this.topPos + y - 1, 152, 120, 18, 18);
   }

   private void drawTexturedButton(GuiGraphics graphics, int x, int y, int width, int height, Component label, boolean pressed) {
      this.drawNineSlice(graphics, x, y, width, height, 0, pressed ? 80 : 40, 256, 40);
      int textWidth = this.font.width(label);
      if (textWidth <= width - 6) {
         graphics.drawString(this.font, label, this.leftPos + x + (width - textWidth) / 2, this.topPos + y + (height - 8) / 2, 16777215, false);
      } else {
         float scale = (float)(width - 6) / (float)textWidth;
         graphics.pose().pushPose();
         graphics.pose().translate((float)(this.leftPos + x + 3), (float)(this.topPos + y + 4), 0.0F);
         graphics.pose().scale(scale, scale, 1.0F);
         graphics.drawString(this.font, label, 0, 0, 16777215, false);
         graphics.pose().popPose();
      }
   }

   private void drawFittedString(GuiGraphics graphics, Component text, int x, int y, int maxWidth) {
      int textWidth = this.font.width(text);
      if (textWidth <= maxWidth) {
         graphics.drawString(this.font, text, x, y, 16777215, false);
      } else {
         float scale = (float)maxWidth / (float)textWidth;
         graphics.pose().pushPose();
         graphics.pose().translate((float)x, (float)(y + 1), 0.0F);
         graphics.pose().scale(scale, scale, 1.0F);
         graphics.drawString(this.font, text, 0, 0, 16777215, false);
         graphics.pose().popPose();
      }
   }

   private void drawSmallLabel(GuiGraphics graphics, Component text, int x, int y, int maxWidth) {
      int textWidth = this.font.width(text);
      float scale = textWidth <= maxWidth ? 0.65F : Math.min(0.65F, (float)maxWidth / (float)textWidth);
      graphics.pose().pushPose();
      graphics.pose().translate((float)x, (float)y, 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(this.font, text, 0, 0, -1513240, false);
      graphics.pose().popPose();
   }

   private void drawNineSlice(GuiGraphics graphics, int x, int y, int width, int height, int u, int v, int texWidth, int texHeight) {
      int halfWidth = width / 2;
      int halfHeight = height / 2;
      graphics.blit(WIDGETS, this.leftPos + x, this.topPos + y, u, v, halfWidth, halfHeight);
      graphics.blit(WIDGETS, this.leftPos + x + halfWidth, this.topPos + y, u + texWidth - (width - halfWidth), v, width - halfWidth, halfHeight);
      graphics.blit(WIDGETS, this.leftPos + x, this.topPos + y + halfHeight, u, v + texHeight - (height - halfHeight), halfWidth, height - halfHeight);
      graphics.blit(
         WIDGETS,
         this.leftPos + x + halfWidth,
         this.topPos + y + halfHeight,
         u + texWidth - (width - halfWidth),
         v + texHeight - (height - halfHeight),
         width - halfWidth,
         height - halfHeight
      );
   }
}
