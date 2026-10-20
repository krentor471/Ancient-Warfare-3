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
      this.f_97726_ = 400;
      this.f_97727_ = 240;
      this.f_97728_ = 10000;
      this.f_97731_ = 10000;
   }

   protected void m_7286_(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      LegacyGuiStyle.background(graphics, this.f_97735_, this.f_97736_, this.f_97726_, this.f_97727_);
      this.drawPanel(graphics, 0, this.queueRowCount(), this.queueScroll, false);
      this.drawPanel(graphics, 200, ((ResearchStationMenu)this.f_97732_).getLearnableResearch().size(), this.selectionScroll, true);
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.research.queued_research"), 8, 8, -1, false);
      graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.research.learnable_research"), 248, 8, -1, false);
      this.drawQueuedRows(graphics);
      this.drawLearnableRows(graphics);
   }

   private void drawPanel(GuiGraphics graphics, int panelX, int count, int scroll, boolean add) {
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.BACKGROUND, this.f_97735_ + panelX, this.f_97736_ + 40, 200, 200, 0, 0, 256, 240);
      int contentHeight = contentHeight(count);
      if (contentHeight > 200) {
         this.drawScrollbar(graphics, panelX + 200 - 12, contentHeight, scroll);
      }
   }

   private void drawQueuedRows(GuiGraphics graphics) {
      List<String> queued = ((ResearchStationMenu)this.f_97732_).getQueuedResearch();
      int row = 0;
      graphics.m_280588_(this.f_97735_, this.f_97736_ + 40, this.f_97735_ + 200, this.f_97736_ + this.f_97727_);
      String active = ((ResearchStationMenu)this.f_97732_).getActiveResearchName();
      if (!active.isEmpty()) {
         int y = 48 - this.queueScroll;
         graphics.m_280614_(
            this.f_96547_,
            Component.m_237113_(active).m_130946_(" (").m_7220_(Component.m_237115_("guistrings.research.current_goal")).m_130946_(")"),
            8,
            y + 1,
            -1,
            false
         );
         row++;
      }

      for (int index = 0; index < queued.size(); index++) {
         int y = 48 + row * 12 - this.queueScroll;
         ResearchGoal goal = ((ResearchStationMenu)this.f_97732_).getQueuedGoal(index);
         graphics.m_280614_(this.f_96547_, (Component)(goal == null ? Component.m_237113_(queued.get(index)) : goal.translatedName()), 8, y + 1, -1, false);
         LegacyGuiStyle.buttonWithText(graphics, this.f_96547_, 0, 0, 168, y, 12, 12, Component.m_237113_("-"), -1, -1);
         row++;
      }

      graphics.m_280618_();
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(graphics);
      this.m_7286_(graphics, partialTick, mouseX, mouseY);
      graphics.m_280168_().m_85836_();
      graphics.m_280168_().m_252880_((float)this.f_97735_, (float)this.f_97736_, 0.0F);
      this.m_280003_(graphics, mouseX, mouseY);
      graphics.m_280168_().m_85849_();
   }

   private void drawLearnableRows(GuiGraphics graphics) {
      List<ResearchGoal> goals = ((ResearchStationMenu)this.f_97732_).getLearnableResearch();
      graphics.m_280588_(this.f_97735_ + 200, this.f_97736_ + 40, this.f_97735_ + this.f_97726_, this.f_97736_ + this.f_97727_);

      for (int index = 0; index < goals.size(); index++) {
         int y = 48 + index * 12 - this.selectionScroll;
         graphics.m_280614_(this.f_96547_, goals.get(index).translatedName(), 208, y + 1, -1, false);
         LegacyGuiStyle.buttonWithText(graphics, this.f_96547_, 0, 0, 368, y, 12, 12, Component.m_237113_("+"), -1, -1);
      }

      graphics.m_280618_();
   }

   private void drawScrollbar(GuiGraphics graphics, int x, int contentHeight, int scroll) {
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.f_97735_ + x, this.f_97736_ + 40, 12, 200, 80, 120, 40, 128);
      int view = 198;
      int handleHeight = Math.max(10, view * view / contentHeight);
      int max = contentHeight - 200;
      int handleY = this.f_97736_ + 40 + 1 + (view - handleHeight) * scroll / max;
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, this.f_97735_ + x + 1, handleY, 10, handleHeight, 120, 120, 32, 128);
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (button == 0 && !(mouseY < (double)(this.f_97736_ + 40)) && !(mouseY >= (double)(this.f_97736_ + this.f_97727_))) {
         int localX = (int)mouseX - this.f_97735_;
         int panel = localX < 200 ? 0 : 1;
         int count = panel == 0 ? this.queueRowCount() : ((ResearchStationMenu)this.f_97732_).getLearnableResearch().size();
         if (contentHeight(count) > 200 && localX >= (panel + 1) * 200 - 12) {
            this.draggingPanel = panel;
            this.scrollToMouse(panel, mouseY);
            return true;
         } else {
            int scroll = panel == 0 ? this.queueScroll : this.selectionScroll;
            int row = ((int)mouseY - this.f_97736_ - 40 - 8 + scroll) / 12;
            if (row >= 0 && row < count) {
               int activeRows = ((ResearchStationMenu)this.f_97732_).getActiveResearchName().isEmpty() ? 0 : 1;
               int index = panel == 0 ? row - activeRows : row;
               if (index < 0) {
                  return false;
               } else {
                  int buttonX = panel == 0 ? 168 : 368;
                  int rowY = 48 + row * 12 - scroll;
                  if (localX >= buttonX
                     && localX < buttonX + 12
                     && !(mouseY < (double)(this.f_97736_ + rowY))
                     && !(mouseY >= (double)(this.f_97736_ + rowY + 12))) {
                     if (this.f_96541_.f_91072_ != null) {
                        if (panel == 0) {
                           this.f_96541_.f_91072_.m_105208_(((ResearchStationMenu)this.f_97732_).f_38840_, 100 + index);
                        } else {
                           int registryIndex = registryIndex(((ResearchStationMenu)this.f_97732_).getLearnableResearch().get(index));
                           if (registryIndex >= 0) {
                              this.f_96541_.f_91072_.m_105208_(((ResearchStationMenu)this.f_97732_).f_38840_, registryIndex);
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

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      if (!(mouseY < (double)(this.f_97736_ + 40)) && !(mouseY >= (double)(this.f_97736_ + this.f_97727_))) {
         int panel = mouseX < (double)(this.f_97735_ + 200) ? 0 : 1;
         int count = panel == 0 ? this.queueRowCount() : ((ResearchStationMenu)this.f_97732_).getLearnableResearch().size();
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

   public boolean m_7979_(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.draggingPanel >= 0) {
         this.scrollToMouse(this.draggingPanel, mouseY);
         return true;
      } else {
         return false;
      }
   }

   public boolean m_6348_(double mouseX, double mouseY, int button) {
      this.draggingPanel = -1;
      return false;
   }

   private void scrollToMouse(int panel, double mouseY) {
      int count = panel == 0 ? this.queueRowCount() : ((ResearchStationMenu)this.f_97732_).getLearnableResearch().size();
      int content = contentHeight(count);
      int max = Math.max(0, content - 200);
      int view = 198;
      int handle = Math.max(10, view * view / content);
      double position = mouseY - (double)this.f_97736_ - 40.0 - 1.0 - (double)handle / 2.0;
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
      return ((ResearchStationMenu)this.f_97732_).getQueuedResearch().size() + (((ResearchStationMenu)this.f_97732_).getActiveResearchName().isEmpty() ? 0 : 1);
   }

   public boolean m_6913_() {
      return false;
   }

   public void m_7379_() {
      this.returnToStation();
   }

   public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 256 && !this.f_96541_.f_91066_.f_92092_.m_90832_(keyCode, scanCode)) {
         return super.m_7933_(keyCode, scanCode, modifiers);
      } else {
         this.returnToStation();
         return true;
      }
   }

   private void returnToStation() {
      this.f_96541_.m_91152_(new ResearchStationScreen((ResearchStationMenu)this.f_97732_, this.f_96541_.f_91074_.m_150109_(), this.f_96539_));
   }
}
