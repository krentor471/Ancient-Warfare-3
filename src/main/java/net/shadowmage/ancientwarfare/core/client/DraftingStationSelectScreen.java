package net.shadowmage.ancientwarfare.core.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.menu.DraftingStationMenu;

public class DraftingStationSelectScreen extends Screen {
   private static final int GUI_WIDTH = 400;
   private static final int GUI_HEIGHT = 240;
   private static final int LIST_X = 0;
   private static final int LIST_Y = 64;
   private static final int LIST_WIDTH = 256;
   private static final int LIST_HEIGHT = 176;
   private static final int MATERIAL_X = 256;
   private static final int MATERIAL_Y = 40;
   private static final int MATERIAL_WIDTH = 144;
   private static final int MATERIAL_HEIGHT = 200;
   private static final int ROW_HEIGHT = 12;
   private static final int PADDING = 3;
   private static final int SCROLLBAR_WIDTH = 12;
   private final DraftingStationScreen parent;
   private EditBox search;
   private LegacyTexturedButton done;
   private LegacyTexturedButton playerTemplates;
   private LegacyTexturedButton builtInTemplates;
   private int left;
   private int top;
   private int candidate;
   private int listScroll;
   private int materialScroll;
   private int draggingPanel = -1;
   private boolean showingBuiltInTemplates;
   private boolean returningToParent;

   public DraftingStationSelectScreen(DraftingStationScreen parent) {
      super(Component.m_237115_("gui.ancientwarfare.drafting.select"));
      this.parent = parent;
      this.candidate = parent.getMenu().templateCount() == 0 ? -1 : parent.selectedIndex();
      this.showingBuiltInTemplates = parent.getMenu().isBuiltInTemplate(this.candidate);
   }

   protected void m_7856_() {
      this.left = (this.f_96543_ - 400) / 2;
      this.top = (this.f_96544_ - 240) / 2;
      String query = this.search == null ? "" : this.search.m_94155_();
      this.search = new EditBox(this.f_96547_, this.left + 10, this.top + 34, 220, 10, Component.m_237115_("guistrings.automation.search"));
      this.search.m_94182_(false);
      this.search.m_94199_(128);
      this.search.m_94144_(query);
      this.search.m_94151_(value -> this.listScroll = 0);
      this.m_142416_(this.search);
      this.playerTemplates = (LegacyTexturedButton)this.m_142416_(
         new LegacyTexturedButton(
            this.left + 8, this.top + 48, 112, 12, Component.m_237115_("gui.ancientwarfare.drafting.player_templates"), button -> this.setCategory(false)
         )
      );
      this.builtInTemplates = (LegacyTexturedButton)this.m_142416_(
         new LegacyTexturedButton(
            this.left + 120, this.top + 48, 112, 12, Component.m_237115_("gui.ancientwarfare.drafting.builtin_templates"), button -> this.setCategory(true)
         )
      );
      this.m_142416_(
         new LegacyTexturedButton(this.left + 277, this.top + 8, 55, 12, Component.m_237115_("gui.ancientwarfare.drafting.back"), button -> this.m_7379_())
      );
      this.done = (LegacyTexturedButton)this.m_142416_(
         new LegacyTexturedButton(
            this.left + 337, this.top + 8, 55, 12, Component.m_237115_("gui.ancientwarfare.drafting.done"), button -> this.confirmSelection()
         )
      );
      this.updateButtons();
      this.draggingPanel = -1;
   }

   private DraftingStationMenu menu() {
      return this.parent.getMenu();
   }

   private void setCategory(boolean builtIn) {
      this.showingBuiltInTemplates = builtIn;
      this.listScroll = 0;
      this.updateButtons();
   }

   private void updateButtons() {
      this.playerTemplates.f_93623_ = this.showingBuiltInTemplates;
      this.builtInTemplates.f_93623_ = !this.showingBuiltInTemplates;
      this.done.f_93623_ = this.candidate >= 0 && this.candidate < this.menu().templateCount();
   }

   private List<Integer> filteredIndices() {
      String query = this.search == null ? "" : this.search.m_94155_().toLowerCase(Locale.ROOT);
      List<String> names = this.menu().templateNames();
      List<Integer> result = new ArrayList<>();

      for (int i = 0; i < names.size(); i++) {
         if (this.menu().isBuiltInTemplate(i) == this.showingBuiltInTemplates && names.get(i).toLowerCase(Locale.ROOT).contains(query)) {
            result.add(i);
         }
      }

      result.sort(Comparator.comparing(names::get, String.CASE_INSENSITIVE_ORDER));
      return result;
   }

   private static int listContentHeight(int count) {
      return 10 + count * 12;
   }

   private static int maxListScroll(int count) {
      return Math.max(0, listContentHeight(count) - 176 + 6);
   }

   private void clampScroll(int count) {
      this.listScroll = Math.max(0, Math.min(this.listScroll, maxListScroll(count)));
      this.materialScroll = Math.max(0, Math.min(this.materialScroll, DraftingStationScreen.maxMaterialScroll(this.menu(), this.candidate, 200)));
   }

   private boolean insideList(double x, double y) {
      return DraftingStationScreen.insideMaterialViewport(x, y, this.left + 0, this.top + 64, 256, 176);
   }

   private int templateAt(double x, double y, List<Integer> indices) {
      if (this.insideList(x, y) && !(x < (double)(this.left + 0 + 3 + 5)) && !(x >= (double)(this.left + 0 + 256 - 12 - 3))) {
         double contentY = y - (double)this.top - 64.0 - 3.0 - 5.0 + (double)this.listScroll;
         if (contentY < 0.0) {
            return -1;
         } else {
            int row = (int)contentY / 12;
            return row < indices.size() ? indices.get(row) : -1;
         }
      } else {
         return -1;
      }
   }

   private String candidateName() {
      return this.candidate >= 0 && this.candidate < this.menu().templateCount()
         ? this.menu().templateNames().get(this.candidate)
         : Component.m_237115_("gui.ancientwarfare.drafting.none").getString();
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(graphics);
      LegacyGuiStyle.background(graphics, this.left, this.top, 400, 240);
      graphics.m_280614_(this.f_96547_, Component.m_237115_("gui.ancientwarfare.drafting.selected"), this.left + 8, this.top + 8, -1, false);
      graphics.m_280056_(this.f_96547_, DraftingStationScreen.fittedText(this.f_96547_, this.candidateName(), 240), this.left + 8, this.top + 20, -1, false);
      graphics.m_280509_(this.left + 8, this.top + 32, this.left + 232, this.top + 46, -1644826);
      graphics.m_280509_(this.left + 9, this.top + 33, this.left + 231, this.top + 45, -16777216);
      graphics.m_280056_(
         this.f_96547_,
         DraftingStationScreen.fittedText(this.f_96547_, Component.m_237115_("gui.ancientwarfare.drafting.materials").getString(), 128),
         this.left + 256 + 8,
         this.top + 30,
         -1,
         false
      );
      List<Integer> indices = this.filteredIndices();
      this.clampScroll(indices.size());
      LegacyGuiStyle.background(graphics, this.left + 0, this.top + 64, 256, 176);
      DraftingStationScreen.drawScrollbar(
         graphics, this.left + 0, this.top + 64, 256, 176, listContentHeight(indices.size()), this.listScroll, maxListScroll(indices.size())
      );
      int right = this.left + 0 + 256 - 12;
      int hoveredIndex = this.templateAt((double)mouseX, (double)mouseY, indices);
      graphics.m_280262_();
      graphics.m_280588_(this.left + 0 + 3, this.top + 64 + 3, right, this.top + 64 + 176 - 3);
      int first = Math.max(0, (this.listScroll - 5) / 12);
      int last = Math.min(indices.size(), first + 14 + 3);
      List<String> names = this.menu().templateNames();

      for (int row = first; row < last; row++) {
         int index = indices.get(row);
         int x = this.left + 0 + 3 + 5;
         int y = this.top + 64 + 3 + 5 + row * 12 - this.listScroll;
         int rowWidth = 233;
         int stateY = index != hoveredIndex && index != this.candidate ? 40 : 80;
         LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, x, y, rowWidth, 12, 0, stateY, 256, 40);
         graphics.m_280056_(this.f_96547_, DraftingStationScreen.fittedText(this.f_96547_, names.get(index), rowWidth - 4), x + 2, y + 2, -1, false);
      }

      if (indices.isEmpty()) {
         graphics.m_280056_(
            this.f_96547_,
            DraftingStationScreen.fittedText(this.f_96547_, Component.m_237115_("gui.ancientwarfare.drafting.category_empty").getString(), 228),
            this.left + 8,
            this.top + 64 + 8,
            -1,
            false
         );
      }

      graphics.m_280262_();
      graphics.m_280618_();
      ItemStack hoveredMaterial = DraftingStationScreen.renderMaterialPanel(
         graphics, this.f_96547_, this.menu(), this.candidate, this.left + 256, this.top + 40, 144, 200, this.materialScroll, mouseX, mouseY
      );
      super.m_88315_(graphics, mouseX, mouseY, partialTick);
      if (!hoveredMaterial.m_41619_()) {
         graphics.m_280153_(this.f_96547_, hoveredMaterial, mouseX, mouseY);
      } else if (hoveredIndex >= 0 && this.f_96547_.m_92895_(names.get(hoveredIndex)) > 229) {
         graphics.m_280557_(this.f_96547_, Component.m_237113_(names.get(hoveredIndex)), mouseX, mouseY);
      } else if (mouseX >= this.left + 8
         && mouseX < this.left + 248
         && mouseY >= this.top + 20
         && mouseY < this.top + 29
         && this.f_96547_.m_92895_(this.candidateName()) > 240) {
         graphics.m_280557_(this.f_96547_, Component.m_237113_(this.candidateName()), mouseX, mouseY);
      }
   }

   public boolean m_6375_(double x, double y, int button) {
      List<Integer> indices = this.filteredIndices();
      this.clampScroll(indices.size());
      if (button == 0 && DraftingStationScreen.insideScrollbar(x, y, this.left + 0, this.top + 64, 256, 176) && maxListScroll(indices.size()) > 0) {
         this.draggingPanel = 0;
         this.scrollToMouse(y);
         return true;
      } else if (button == 0
         && DraftingStationScreen.insideScrollbar(x, y, this.left + 256, this.top + 40, 144, 200)
         && DraftingStationScreen.maxMaterialScroll(this.menu(), this.candidate, 200) > 0) {
         this.draggingPanel = 1;
         this.scrollToMouse(y);
         return true;
      } else {
         int index = this.templateAt(x, y, indices);
         if (button == 0 && index >= 0) {
            this.candidate = index;
            this.materialScroll = 0;
            this.updateButtons();
            this.f_96541_.m_91106_().m_120367_(SimpleSoundInstance.m_263171_(SoundEvents.f_12490_, 1.0F));
            return true;
         } else {
            return super.m_6375_(x, y, button);
         }
      }
   }

   public boolean m_6050_(double x, double y, double delta) {
      if (this.insideList(x, y) || DraftingStationScreen.insideScrollbar(x, y, this.left + 0, this.top + 64, 256, 176)) {
         this.listScroll = this.listScroll - (int)Math.signum(delta) * 12 * 3;
         this.clampScroll(this.filteredIndices().size());
         return true;
      } else if (!DraftingStationScreen.insideMaterialViewport(x, y, this.left + 256, this.top + 40, 144, 200)
         && !DraftingStationScreen.insideScrollbar(x, y, this.left + 256, this.top + 40, 144, 200)) {
         return super.m_6050_(x, y, delta);
      } else {
         this.materialScroll = this.materialScroll - (int)Math.signum(delta) * 18;
         this.clampScroll(this.filteredIndices().size());
         return true;
      }
   }

   private void scrollToMouse(double y) {
      if (this.draggingPanel == 0) {
         int count = this.filteredIndices().size();
         this.listScroll = DraftingStationScreen.scrollOffsetAtMouse(y, this.top + 64, 176, listContentHeight(count), maxListScroll(count));
      } else if (this.draggingPanel == 1) {
         this.materialScroll = DraftingStationScreen.scrollOffsetAtMouse(
            y,
            this.top + 40,
            200,
            DraftingStationScreen.materialContentHeight(this.menu(), this.candidate),
            DraftingStationScreen.maxMaterialScroll(this.menu(), this.candidate, 200)
         );
      }
   }

   public boolean m_7979_(double x, double y, int button, double dragX, double dragY) {
      if (button == 0 && this.draggingPanel >= 0) {
         this.scrollToMouse(y);
         return true;
      } else {
         return super.m_7979_(x, y, button, dragX, dragY);
      }
   }

   public boolean m_6348_(double x, double y, int button) {
      if (button == 0) {
         this.draggingPanel = -1;
      }

      return super.m_6348_(x, y, button);
   }

   private void confirmSelection() {
      if (this.candidate >= 0 && this.candidate < this.menu().templateCount()) {
         this.parent.chooseTemplate(this.candidate);
         this.m_7379_();
      }
   }

   public void m_86600_() {
      this.search.m_94120_();
      if (this.f_96541_.f_91074_ == null || this.f_96541_.f_91074_.f_36096_ != this.menu()) {
         this.f_96541_.m_91152_(null);
      } else if (!this.f_96541_.f_91074_.m_6084_() || this.f_96541_.f_91074_.m_213877_()) {
         this.f_96541_.f_91074_.m_6915_();
      }
   }

   public void m_7379_() {
      if (this.f_96541_.f_91074_ != null && this.f_96541_.f_91074_.f_36096_ == this.menu()) {
         this.returningToParent = true;
         this.f_96541_.m_91152_(this.parent);
      } else {
         this.f_96541_.m_91152_(null);
      }
   }

   public void m_7861_() {
      if (!this.returningToParent) {
         this.parent.selectorRemoved();
      }

      super.m_7861_();
   }

   public boolean m_7043_() {
      return false;
   }
}
