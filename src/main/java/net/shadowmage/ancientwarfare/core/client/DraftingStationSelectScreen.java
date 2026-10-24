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
      super(Component.translatable("gui.ancientwarfare.drafting.select"));
      this.parent = parent;
      this.candidate = parent.getMenu().templateCount() == 0 ? -1 : parent.selectedIndex();
      this.showingBuiltInTemplates = parent.getMenu().isBuiltInTemplate(this.candidate);
   }

   protected void init() {
      this.left = (this.width - 400) / 2;
      this.top = (this.height - 240) / 2;
      String query = this.search == null ? "" : this.search.getValue();
      this.search = new EditBox(this.font, this.left + 10, this.top + 34, 220, 10, Component.translatable("guistrings.automation.search"));
      this.search.setBordered(false);
      this.search.setMaxLength(128);
      this.search.setValue(query);
      this.search.setResponder(value -> this.listScroll = 0);
      this.addRenderableWidget(this.search);
      this.playerTemplates = (LegacyTexturedButton)this.addRenderableWidget(
         new LegacyTexturedButton(
            this.left + 8, this.top + 48, 112, 12, Component.translatable("gui.ancientwarfare.drafting.player_templates"), button -> this.setCategory(false)
         )
      );
      this.builtInTemplates = (LegacyTexturedButton)this.addRenderableWidget(
         new LegacyTexturedButton(
            this.left + 120, this.top + 48, 112, 12, Component.translatable("gui.ancientwarfare.drafting.builtin_templates"), button -> this.setCategory(true)
         )
      );
      this.addRenderableWidget(
         new LegacyTexturedButton(this.left + 277, this.top + 8, 55, 12, Component.translatable("gui.ancientwarfare.drafting.back"), button -> this.onClose())
      );
      this.done = (LegacyTexturedButton)this.addRenderableWidget(
         new LegacyTexturedButton(
            this.left + 337, this.top + 8, 55, 12, Component.translatable("gui.ancientwarfare.drafting.done"), button -> this.confirmSelection()
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
      this.playerTemplates.active = this.showingBuiltInTemplates;
      this.builtInTemplates.active = !this.showingBuiltInTemplates;
      this.done.active = this.candidate >= 0 && this.candidate < this.menu().templateCount();
   }

   private List<Integer> filteredIndices() {
      String query = this.search == null ? "" : this.search.getValue().toLowerCase(Locale.ROOT);
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
         : Component.translatable("gui.ancientwarfare.drafting.none").getString();
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      LegacyGuiStyle.background(graphics, this.left, this.top, 400, 240);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.drafting.selected"), this.left + 8, this.top + 8, -1, false);
      graphics.drawString(this.font, DraftingStationScreen.fittedText(this.font, this.candidateName(), 240), this.left + 8, this.top + 20, -1, false);
      graphics.fill(this.left + 8, this.top + 32, this.left + 232, this.top + 46, -1644826);
      graphics.fill(this.left + 9, this.top + 33, this.left + 231, this.top + 45, -16777216);
      graphics.drawString(
         this.font,
         DraftingStationScreen.fittedText(this.font, Component.translatable("gui.ancientwarfare.drafting.materials").getString(), 128),
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
      graphics.flush();
      graphics.enableScissor(this.left + 0 + 3, this.top + 64 + 3, right, this.top + 64 + 176 - 3);
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
         graphics.drawString(this.font, DraftingStationScreen.fittedText(this.font, names.get(index), rowWidth - 4), x + 2, y + 2, -1, false);
      }

      if (indices.isEmpty()) {
         graphics.drawString(
            this.font,
            DraftingStationScreen.fittedText(this.font, Component.translatable("gui.ancientwarfare.drafting.category_empty").getString(), 228),
            this.left + 8,
            this.top + 64 + 8,
            -1,
            false
         );
      }

      graphics.flush();
      graphics.disableScissor();
      ItemStack hoveredMaterial = DraftingStationScreen.renderMaterialPanel(
         graphics, this.font, this.menu(), this.candidate, this.left + 256, this.top + 40, 144, 200, this.materialScroll, mouseX, mouseY
      );
      super.render(graphics, mouseX, mouseY, partialTick);
      if (!hoveredMaterial.isEmpty()) {
         graphics.renderTooltip(this.font, hoveredMaterial, mouseX, mouseY);
      } else if (hoveredIndex >= 0 && this.font.width(names.get(hoveredIndex)) > 229) {
         graphics.renderTooltip(this.font, Component.literal(names.get(hoveredIndex)), mouseX, mouseY);
      } else if (mouseX >= this.left + 8
         && mouseX < this.left + 248
         && mouseY >= this.top + 20
         && mouseY < this.top + 29
         && this.font.width(this.candidateName()) > 240) {
         graphics.renderTooltip(this.font, Component.literal(this.candidateName()), mouseX, mouseY);
      }
   }

   public boolean mouseClicked(double x, double y, int button) {
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
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
         } else {
            return super.mouseClicked(x, y, button);
         }
      }
   }

   public boolean mouseScrolled(double x, double y, double delta) {
      if (this.insideList(x, y) || DraftingStationScreen.insideScrollbar(x, y, this.left + 0, this.top + 64, 256, 176)) {
         this.listScroll = this.listScroll - (int)Math.signum(delta) * 12 * 3;
         this.clampScroll(this.filteredIndices().size());
         return true;
      } else if (!DraftingStationScreen.insideMaterialViewport(x, y, this.left + 256, this.top + 40, 144, 200)
         && !DraftingStationScreen.insideScrollbar(x, y, this.left + 256, this.top + 40, 144, 200)) {
         return super.mouseScrolled(x, y, delta);
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

   public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
      if (button == 0 && this.draggingPanel >= 0) {
         this.scrollToMouse(y);
         return true;
      } else {
         return super.mouseDragged(x, y, button, dragX, dragY);
      }
   }

   public boolean mouseReleased(double x, double y, int button) {
      if (button == 0) {
         this.draggingPanel = -1;
      }

      return super.mouseReleased(x, y, button);
   }

   private void confirmSelection() {
      if (this.candidate >= 0 && this.candidate < this.menu().templateCount()) {
         this.parent.chooseTemplate(this.candidate);
         this.onClose();
      }
   }

   public void tick() {
      this.search.tick();
      if (this.minecraft.player == null || this.minecraft.player.containerMenu != this.menu()) {
         this.minecraft.setScreen(null);
      } else if (!this.minecraft.player.isAlive() || this.minecraft.player.isRemoved()) {
         this.minecraft.player.closeContainer();
      }
   }

   public void onClose() {
      if (this.minecraft.player != null && this.minecraft.player.containerMenu == this.menu()) {
         this.returningToParent = true;
         this.minecraft.setScreen(this.parent);
      } else {
         this.minecraft.setScreen(null);
      }
   }

   public void removed() {
      if (!this.returningToParent) {
         this.parent.selectorRemoved();
      }

      super.removed();
   }

   public boolean isPauseScreen() {
      return false;
   }
}
