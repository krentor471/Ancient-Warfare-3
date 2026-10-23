package net.shadowmage.ancientwarfare.core.client;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.menu.ResearchStationMenu;
import net.shadowmage.ancientwarfare.core.research.ResearchGoal;
import net.shadowmage.ancientwarfare.core.research.ResearchRegistry;

public class ResearchQueueScreen extends AbstractContainerScreen<ResearchStationMenu> {
   private static final int PANEL_Y = 40;
   private static final int PANEL_WIDTH = 200;
   private static final int PANEL_HEIGHT = 200;
   private static final int SCROLLBAR_WIDTH = 12;
   private static final int ROW_HEIGHT = 12;
   private static final int TEXT_COLOR = -1;
   private int queueScroll;
   private int selectionScroll;
   private int draggingPanel = -1;

   public ResearchQueueScreen(ResearchStationMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.imageWidth = 400;
      this.imageHeight = 240;
      this.titleLabelX = 10000;
      this.inventoryLabelY = 10000;
   }

   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      LegacyGuiStyle.background(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
      this.drawPanel(graphics, 0, this.queueRowCount(), this.queueScroll, false);
      this.drawPanel(graphics, 200, ((ResearchStationMenu)this.menu).getLearnableResearch().size(), this.selectionScroll, true);
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(this.font, Component.translatable("guistrings.research.queued_research"), 8, 8, -1, false);
      graphics.drawString(this.font, Component.translatable("guistrings.research.learnable_research"), 248, 8, -1, false);
      this.drawQueuedRows(graphics);
      this.drawLearnableRows(graphics);
   }

   private void drawPanel(GuiGraphics graphics, int panelX, int count, int scroll, boolean add) {
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.BACKGROUND, this.leftPos + panelX, this.topPos + 40, 200, 200, 0, 0, 256, 240);
      int contentHeight = contentHeight(count);
      if (contentHeight > 200) {
         this.drawScrollbar(graphics, panelX + 200 - 12, contentHeight, scroll);
      }
   }

   private void drawQueuedRows(GuiGraphics graphics) {
      List<String> queued = ((ResearchStationMenu)this.menu).getQueuedResearch();
      int row = 0;
      graphics.enableScissor(this.leftPos, this.topPos + 40, this.leftPos + 200, this.topPos + this.imageHeight);
      String active = ((ResearchStationMenu)this.menu).getActiveResearchName();
      if (!active.isEmpty()) {
         int y = 48 - this.queueScroll;
         graphics.drawString(
            this.font,
            Component.literal(active).append(" (").append(Component.translatable("guistrings.research.current_goal")).append(")"),
            8,
            y + 1,
            -1,
            false
         );
         row++;
      }

      for (int index = 0; index < queued.size(); index++) {
         int y = 48 + row * 12 - this.queueScroll;
         ResearchGoal goal = ((ResearchStationMenu)this.menu).getQueuedGoal(index);
         graphics.drawString(this.font, (Component)(goal == null ? Component.literal(queued.get(index)) : withScience(goal)), 8, y + 1, -1, false);
         LegacyGuiStyle.buttonWithText(graphics, this.font, 0, 0, 168, y, 12, 12, Component.literal("-"), -1, -1);
         row++;
      }

      graphics.disableScissor();
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      this.renderBg(graphics, partialTick, mouseX, mouseY);
      graphics.pose().pushPose();
      graphics.pose().translate((float)this.leftPos, (float)this.topPos, 0.0F);
      this.renderLabels(graphics, mouseX, mouseY);
      graphics.pose().popPose();
   }

   /** The technology's name, with the science it costs when it costs any. */
   static Component withScience(ResearchGoal goal) {
      return goal.science() <= 0
         ? goal.translatedName()
         : goal.translatedName().copy().append(Component.literal(" " + goal.science() + "\u2726").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
   }

   private void drawLearnableRows(GuiGraphics graphics) {
      List<ResearchGoal> goals = ((ResearchStationMenu)this.menu).getLearnableResearch();
      graphics.enableScissor(this.leftPos + 200, this.topPos + 40, this.leftPos + this.imageWidth, this.topPos + this.imageHeight);

      for (int index = 0; index < goals.size(); index++) {
         int y = 48 + index * 12 - this.selectionScroll;
         graphics.drawString(this.font, withScience(goals.get(index)), 208, y + 1, -1, false);
         LegacyGuiStyle.buttonWithText(graphics, this.font, 0, 0, 368, y, 12, 12, Component.literal("+"), -1, -1);
      }

      graphics.disableScissor();
   }

   private void drawScrollbar(GuiGraphics graphics, int x, int contentHeight, int scroll) {
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.leftPos + x, this.topPos + 40, 12, 200, 80, 120, 40, 128);
      int view = 198;
      int handleHeight = Math.max(10, view * view / contentHeight);
      int max = contentHeight - 200;
      int handleY = this.topPos + 40 + 1 + (view - handleHeight) * scroll / max;
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.leftPos + x + 1, handleY, 10, handleHeight, 120, 120, 32, 128);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && !(mouseY < (double)(this.topPos + 40)) && !(mouseY >= (double)(this.topPos + this.imageHeight))) {
         int localX = (int)mouseX - this.leftPos;
         int panel = localX < 200 ? 0 : 1;
         int count = panel == 0 ? this.queueRowCount() : ((ResearchStationMenu)this.menu).getLearnableResearch().size();
         if (contentHeight(count) > 200 && localX >= (panel + 1) * 200 - 12) {
            this.draggingPanel = panel;
            this.scrollToMouse(panel, mouseY);
            return true;
         } else {
            int scroll = panel == 0 ? this.queueScroll : this.selectionScroll;
            int row = ((int)mouseY - this.topPos - 40 - 8 + scroll) / 12;
            if (row >= 0 && row < count) {
               int activeRows = ((ResearchStationMenu)this.menu).getActiveResearchName().isEmpty() ? 0 : 1;
               int index = panel == 0 ? row - activeRows : row;
               if (index < 0) {
                  return false;
               } else {
                  int buttonX = panel == 0 ? 168 : 368;
                  int rowY = 48 + row * 12 - scroll;
                  if (localX >= buttonX
                     && localX < buttonX + 12
                     && !(mouseY < (double)(this.topPos + rowY))
                     && !(mouseY >= (double)(this.topPos + rowY + 12))) {
                     if (this.minecraft.gameMode != null) {
                        if (panel == 0) {
                           this.minecraft.gameMode.handleInventoryButtonClick(((ResearchStationMenu)this.menu).containerId, 100 + index);
                        } else {
                           int registryIndex = registryIndex(((ResearchStationMenu)this.menu).getLearnableResearch().get(index));
                           if (registryIndex >= 0) {
                              this.minecraft.gameMode.handleInventoryButtonClick(((ResearchStationMenu)this.menu).containerId, registryIndex);
                           }
                        }
                     }

                     return true;
                  } else {
                     return false;
                  }
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      if (!(mouseY < (double)(this.topPos + 40)) && !(mouseY >= (double)(this.topPos + this.imageHeight))) {
         int panel = mouseX < (double)(this.leftPos + 200) ? 0 : 1;
         int count = panel == 0 ? this.queueRowCount() : ((ResearchStationMenu)this.menu).getLearnableResearch().size();
         if (contentHeight(count) <= 200) {
            return false;
         } else {
            int amount = (int)Math.signum(delta) * 12;
            if (panel == 0) {
               this.queueScroll = clampScroll(this.queueScroll - amount, count);
            } else {
               this.selectionScroll = clampScroll(this.selectionScroll - amount, count);
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.draggingPanel >= 0) {
         this.scrollToMouse(this.draggingPanel, mouseY);
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.draggingPanel = -1;
      return false;
   }

   private void scrollToMouse(int panel, double mouseY) {
      int count = panel == 0 ? this.queueRowCount() : ((ResearchStationMenu)this.menu).getLearnableResearch().size();
      int content = contentHeight(count);
      int max = Math.max(0, content - 200);
      int view = 198;
      int handle = Math.max(10, view * view / content);
      double position = mouseY - (double)this.topPos - 40.0 - 1.0 - (double)handle / 2.0;
      int value = (int)Math.round(Math.max(0.0, Math.min(1.0, position / Math.max(1.0, (double)(view - handle)))) * (double)max);
      if (panel == 0) {
         this.queueScroll = value;
      } else {
         this.selectionScroll = value;
      }
   }

   private static int contentHeight(int count) {
      return 16 + count * 12;
   }

   private static int clampScroll(int value, int count) {
      return Math.max(0, Math.min(Math.max(0, contentHeight(count) - 200), value));
   }

   private static int registryIndex(ResearchGoal goal) {
      int index = 0;

      for (ResearchGoal candidate : ResearchRegistry.all()) {
         if (candidate.id().equals(goal.id())) {
            return index;
         }

         index++;
      }

      return -1;
   }

   private int queueRowCount() {
      return ((ResearchStationMenu)this.menu).getQueuedResearch().size() + (((ResearchStationMenu)this.menu).getActiveResearchName().isEmpty() ? 0 : 1);
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public void onClose() {
      this.returnToStation();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 256 && !this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
         return super.keyPressed(keyCode, scanCode, modifiers);
      } else {
         this.returnToStation();
         return true;
      }
   }

   private void returnToStation() {
      this.minecraft.setScreen(new ResearchStationScreen((ResearchStationMenu)this.menu, this.minecraft.player.getInventory(), this.title));
   }
}
