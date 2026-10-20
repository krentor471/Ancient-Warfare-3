package net.shadowmage.ancientwarfare.core.client;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.entity.NpcBaseEntity;

public class NpcSkinSelectionScreen extends Screen {
   private static final int PANEL_WIDTH = 352;
   private static final int PANEL_HEIGHT = 196;
   private static final int PREVIEW_X = 16;
   private static final int PREVIEW_Y = 42;
   private static final int PREVIEW_WIDTH = 130;
   private static final int PREVIEW_HEIGHT = 112;
   private static final int LIST_X = 162;
   private static final int LIST_Y = 56;
   private static final int LIST_WIDTH = 172;
   private static final int LIST_HEIGHT = 108;
   private static final int ROW_HEIGHT = 12;
   private static final int SCROLLBAR_WIDTH = 8;
   private static final int TEXT_COLOR = 16777215;
   private static final int MUTED_TEXT_COLOR = -4342339;
   private final NpcInventoryScreen parent;
   private final int npcEntityId;
   private final String initialSkinName;
   private boolean alexModel;
   private EditBox skinNameBox;
   private int scrollOffset;
   private boolean draggingScrollbar;

   public NpcSkinSelectionScreen(NpcInventoryScreen parent, int npcEntityId, String initialSkinName, boolean alexModel) {
      super(Component.m_237115_("container.ancientwarfare.npc_skin"));
      this.parent = parent;
      this.npcEntityId = npcEntityId;
      this.initialSkinName = initialSkinName == null ? "" : initialSkinName;
      this.alexModel = alexModel;
   }

   protected void m_7856_() {
      int left = (this.f_96543_ - 352) / 2;
      int top = (this.f_96544_ - 196) / 2;
      this.skinNameBox = new EditBox(this.f_96547_, left + 162, top + 31, 172, 18, Component.m_237115_("guistrings.npc.skin_search"));
      this.skinNameBox.m_94199_(128);
      this.skinNameBox.m_94144_(this.initialSkinName);
      this.m_142416_(this.skinNameBox);
      this.m_264313_(this.skinNameBox);
      this.m_142416_(
         Button.m_253074_(Component.m_237115_("guistrings.npc.skin_custom"), button -> this.setSearchPrefix("custom"))
            .m_252987_(left + 16, top + 160, 58, 20)
            .m_253136_()
      );
      this.m_142416_(Button.m_253074_(this.alexButtonLabel(), button -> {
         this.alexModel = !this.alexModel;
         button.m_93666_(this.alexButtonLabel());
      }).m_252987_(left + 80, top + 160, 58, 20).m_253136_());
      this.m_142416_(
         Button.m_253074_(Component.m_237115_("guistrings.npc.skin_apply"), button -> this.applySkin(this.skinNameBox.m_94155_()))
            .m_252987_(left + 162, top + 169, 52, 20)
            .m_253136_()
      );
      this.m_142416_(
         Button.m_253074_(Component.m_237115_("guistrings.npc.skin_default"), button -> this.applySkin(""))
            .m_252987_(left + 220, top + 169, 52, 20)
            .m_253136_()
      );
      this.m_142416_(
         Button.m_253074_(Component.m_237115_("guistrings.npc.back"), button -> this.f_96541_.m_91152_(this.parent))
            .m_252987_(left + 278, top + 169, 52, 20)
            .m_253136_()
      );
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(graphics);
      int left = (this.f_96543_ - 352) / 2;
      int top = (this.f_96544_ - 196) / 2;
      List<String> matches = this.filteredSkins();
      this.clampScroll(matches);
      this.drawPanel(graphics, left, top);
      graphics.m_280614_(this.f_96547_, this.f_96539_, left + 10, top + 10, 16777215, false);
      graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.npc.skin_preview"), left + 16, top + 28, 16777215, false);
      graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.npc.skin_search"), left + 162, top + 21, 16777215, false);
      this.drawPreview(graphics, left, top, this.previewSkin(matches), mouseX, mouseY);
      this.drawSkinList(graphics, left, top, matches, mouseX, mouseY);
      super.m_88315_(graphics, mouseX, mouseY, partialTick);
      if (!this.skinNameBox.m_94155_().isBlank() && !NpcSkinRegistry.hasSkin(this.skinNameBox.m_94155_()) && matches.isEmpty()) {
         graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.npc.skin_unknown"), left + 162, top + 44, -34953, false);
      }
   }

   public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 257 && keyCode != 335) {
         boolean handled = super.m_7933_(keyCode, scanCode, modifiers);
         this.clampScroll(this.filteredSkins());
         return handled;
      } else {
         this.applySkin(this.skinNameBox.m_94155_());
         return true;
      }
   }

   public boolean m_5534_(char codePoint, int modifiers) {
      boolean handled = super.m_5534_(codePoint, modifiers);
      this.scrollOffset = 0;
      return handled;
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      int left = (this.f_96543_ - 352) / 2;
      int top = (this.f_96544_ - 196) / 2;
      if (button == 0 && this.isInside(mouseX, mouseY, this.scrollbarX(left), this.scrollbarY(top), 8, this.scrollbarHeight())) {
         this.draggingScrollbar = true;
         List<String> matches = this.filteredSkins();
         this.updateScrollFromMouse(mouseY, matches);
         return true;
      } else {
         if (this.isInside(mouseX, mouseY, left + 162, top + 56, 162, 108)) {
            List<String> matches = this.filteredSkins();
            int index = this.scrollOffset + ((int)mouseY - (top + 56)) / 12;
            if (index >= 0 && index < matches.size()) {
               this.skinNameBox.m_94144_(matches.get(index));
               this.scrollOffset = Math.min(this.scrollOffset, Math.max(0, matches.size() - this.visibleRows()));
               return true;
            }
         }

         return super.m_6375_(mouseX, mouseY, button);
      }
   }

   public boolean m_7979_(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (button == 0 && this.draggingScrollbar) {
         this.updateScrollFromMouse(mouseY, this.filteredSkins());
         return true;
      } else {
         return super.m_7979_(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean m_6348_(double mouseX, double mouseY, int button) {
      if (button == 0 && this.draggingScrollbar) {
         this.draggingScrollbar = false;
         return true;
      } else {
         return super.m_6348_(mouseX, mouseY, button);
      }
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      int left = (this.f_96543_ - 352) / 2;
      int top = (this.f_96544_ - 196) / 2;
      if (this.isInside(mouseX, mouseY, left + 162, top + 56, 172, 108)) {
         List<String> matches = this.filteredSkins();
         this.scrollOffset = this.scrollOffset - (int)Math.signum(delta);
         this.clampScroll(matches);
         return true;
      } else {
         return super.m_6050_(mouseX, mouseY, delta);
      }
   }

   public boolean m_7043_() {
      return false;
   }

   private void drawPanel(GuiGraphics graphics, int left, int top) {
      graphics.m_280509_(left, top, left + 352, top + 196, -585162977);
      graphics.m_280509_(left + 1, top + 1, left + 352 - 1, top + 196 - 1, -582268085);
      graphics.m_280509_(left + 3, top + 3, left + 352 - 3, top + 196 - 3, -584373461);
      graphics.m_280509_(left + 16, top + 42, left + 16 + 130, top + 42 + 112, -1441656302);
      graphics.m_280509_(left + 162, top + 56, left + 162 + 172, top + 56 + 108, -1441656302);
   }

   private void drawSkinList(GuiGraphics graphics, int left, int top, List<String> matches, int mouseX, int mouseY) {
      int rows = this.visibleRows();
      String selected = this.skinNameBox.m_94155_().trim();
      if (matches.isEmpty()) {
         graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.npc.skin_no_matches"), left + 162 + 5, top + 56 + 5, -4342339, false);
      } else {
         int listRight = left + 162 + 172;

         for (int row = 0; row < rows; row++) {
            int index = this.scrollOffset + row;
            if (index >= matches.size()) {
               break;
            }

            String skinName = matches.get(index);
            int rowX = left + 162 + 2;
            int rowY = top + 56 + row * 12;
            boolean hovered = this.isInside((double)mouseX, (double)mouseY, rowX, rowY, 163, 12);
            boolean active = skinName.equals(selected);
            if (hovered || active) {
               graphics.m_280509_(rowX, rowY, listRight - 8, rowY + 12, active ? -1436584883 : 1717531725);
            }

            graphics.m_280056_(this.f_96547_, this.f_96547_.m_92834_(skinName, 155), rowX + 2, rowY + 2, active ? -1638480 : 16777215, false);
         }

         this.drawScrollbar(graphics, left, top, matches.size());
      }
   }

   private void drawScrollbar(GuiGraphics graphics, int left, int top, int totalRows) {
      int trackX = this.scrollbarX(left);
      int trackY = this.scrollbarY(top);
      int trackHeight = this.scrollbarHeight();
      graphics.m_280509_(trackX, trackY, trackX + 4, trackY + trackHeight, -13092808);
      int maxScroll = Math.max(0, totalRows - this.visibleRows());
      int thumbHeight = maxScroll == 0 ? trackHeight : Math.max(14, trackHeight * this.visibleRows() / totalRows);
      int thumbY = maxScroll == 0 ? trackY : trackY + (trackHeight - thumbHeight) * this.scrollOffset / maxScroll;
      graphics.m_280509_(trackX, thumbY, trackX + 4, thumbY + thumbHeight, -4802890);
   }

   private void drawPreview(GuiGraphics graphics, int left, int top, String skinName, int mouseX, int mouseY) {
      int centerX = left + 16 + 65;
      int centerY = top + 42 + 112 - 18;
      if (this.f_96541_.f_91073_ != null && this.f_96541_.f_91073_.m_6815_(this.npcEntityId) instanceof NpcBaseEntity npc) {
         String oldSkin = npc.getSkinName();
         boolean oldAlex = npc.isAlexModel();
         npc.setSkinName(skinName == null ? "" : skinName);
         npc.setAlexModel(this.alexModel);

         try {
            InventoryScreen.m_274545_(graphics, centerX, centerY, 28, (float)(centerX - mouseX), (float)(mouseY - centerY), npc);
         } finally {
            npc.setSkinName(oldSkin);
            npc.setAlexModel(oldAlex);
         }

         graphics.m_280056_(this.f_96547_, this.f_96547_.m_92834_(skinName == null ? "" : skinName, 120), left + 16 + 5, top + 42 + 112 - 12, 16777215, false);
      } else {
         graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.npc.skin_preview_empty"), left + 16 + 12, top + 42 + 48, -4342339, false);
      }
   }

   private List<String> filteredSkins() {
      String query = this.skinNameBox == null ? "" : this.skinNameBox.m_94155_().trim().toLowerCase(Locale.ROOT);
      List<String> names = NpcSkinRegistry.skinNames();
      return query.isEmpty() ? names : names.stream().filter(name -> name.toLowerCase(Locale.ROOT).contains(query)).toList();
   }

   private String previewSkin(List<String> matches) {
      String selected = this.skinNameBox.m_94155_().trim();
      if (NpcSkinRegistry.hasSkin(selected)) {
         return selected;
      } else {
         return matches.isEmpty() ? "" : matches.get(0);
      }
   }

   private int visibleRows() {
      return 9;
   }

   private void clampScroll(List<String> matches) {
      int maxScroll = Math.max(0, matches.size() - this.visibleRows());
      if (this.scrollOffset < 0) {
         this.scrollOffset = 0;
      } else if (this.scrollOffset > maxScroll) {
         this.scrollOffset = maxScroll;
      }
   }

   private boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
      return mouseX >= (double)x && mouseY >= (double)y && mouseX < (double)(x + width) && mouseY < (double)(y + height);
   }

   private void applySkin(String skinName) {
      AWCorePlatform.setNpcSkin(this.npcEntityId, skinName, this.alexModel);
      this.f_96541_.m_91152_(this.parent);
   }

   private void setSearchPrefix(String prefix) {
      this.skinNameBox.m_94144_(prefix);
      this.scrollOffset = 0;
   }

   private Component alexButtonLabel() {
      return Component.m_237115_(this.alexModel ? "guistrings.npc.skin_alex_on" : "guistrings.npc.skin_alex_off");
   }

   private int scrollbarX(int left) {
      return left + 162 + 172 - 8;
   }

   private int scrollbarY(int top) {
      return top + 56 + 2;
   }

   private int scrollbarHeight() {
      return 104;
   }

   private void updateScrollFromMouse(double mouseY, List<String> matches) {
      int maxScroll = Math.max(0, matches.size() - this.visibleRows());
      if (maxScroll == 0) {
         this.scrollOffset = 0;
      } else {
         int top = (this.f_96544_ - 196) / 2;
         int trackY = this.scrollbarY(top);
         int trackHeight = this.scrollbarHeight();
         int thumbHeight = Math.max(14, trackHeight * this.visibleRows() / matches.size());
         double ratio = (mouseY - (double)trackY - (double)thumbHeight / 2.0) / (double)Math.max(1, trackHeight - thumbHeight);
         this.scrollOffset = (int)Math.round(Math.max(0.0, Math.min(1.0, ratio)) * (double)maxScroll);
      }
   }
}
