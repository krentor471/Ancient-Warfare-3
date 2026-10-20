package net.shadowmage.ancientwarfare.core.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.tile.MailboxBlockEntity;

public class MailboxTargetScreen extends Screen {
   private static final int GUI_WIDTH = 256;
   private static final int GUI_HEIGHT = 240;
   private static final int LIST_X = 8;
   private static final int LIST_Y = 40;
   private static final int LIST_WIDTH = 240;
   private static final int LIST_HEIGHT = 192;
   private static final int PADDING = 3;
   private static final int ROW_HEIGHT = 12;
   private static final int SCROLLBAR_WIDTH = 12;
   private static final int VISIBLE_ROWS = 15;
   private static MailboxTargetScreen active;
   private final Screen parent;
   private final MailboxBlockEntity box;
   private List<String> entries = new ArrayList<>();
   private EditBox search;
   private int left;
   private int top;
   private int firstRow;
   private boolean draggingScrollbar;

   public MailboxTargetScreen(Screen parent, MailboxBlockEntity box) {
      super(Component.m_237115_("guistrings.automation.mailbox_target_select"));
      this.parent = parent;
      this.box = box;
   }

   public static void receive(List<String> entries) {
      if (active != null) {
         active.entries = new ArrayList<>(entries);
         active.clampScroll(active.filteredEntries().size());
      }
   }

   protected void m_7856_() {
      active = this;
      this.left = (this.f_96543_ - 256) / 2;
      this.top = (this.f_96544_ - 240) / 2;
      String query = this.search == null ? "" : this.search.m_94155_();
      this.search = new EditBox(this.f_96547_, this.left + 8, this.top + 8, 174, 16, Component.m_237115_("guistrings.automation.search"));
      this.search.m_94199_(128);
      this.search.m_94144_(query);
      this.search.m_94151_(value -> this.firstRow = 0);
      this.m_142416_(this.search);
      this.m_142416_(new LegacyTexturedButton(this.left + 193, this.top + 10, 55, 12, Component.m_237115_("guistrings.npc.back"), button -> this.m_7379_()));
      this.draggingScrollbar = false;
      AWCorePlatform.requestMailboxList(this.box.m_58899_());
   }

   private List<String> filteredEntries() {
      String query = this.search == null ? "" : this.search.m_94155_().toLowerCase(Locale.ROOT);
      return query.isEmpty() ? this.entries : this.entries.stream().filter(entry -> entry.toLowerCase(Locale.ROOT).contains(query)).toList();
   }

   private int maxScroll(int count) {
      return Math.max(0, count - 15);
   }

   private void clampScroll(int count) {
      this.firstRow = Math.max(0, Math.min(this.firstRow, this.maxScroll(count)));
   }

   private int thumbHeight(int count) {
      return Math.min(190, Math.max(10, (int)(2850L / (long)Math.max(15, count))));
   }

   private void scrollToMouse(double mouseY) {
      int count = this.filteredEntries().size();
      int thumb = this.thumbHeight(count);
      double fraction = (mouseY - (double)this.top - 40.0 - 1.0 - (double)thumb / 2.0) / (double)Math.max(1, 190 - thumb);
      this.firstRow = (int)Math.round(Math.max(0.0, Math.min(1.0, fraction)) * (double)this.maxScroll(count));
   }

   private boolean insideList(double x, double y) {
      return x >= (double)(this.left + 8 + 3)
         && x < (double)(this.left + 8 + 240 - 12)
         && y >= (double)(this.top + 40 + 3)
         && y < (double)(this.top + 40 + 192 - 3);
   }

   private boolean insideScrollbar(double x, double y) {
      return x >= (double)(this.left + 8 + 240 - 12) && x < (double)(this.left + 8 + 240) && y >= (double)(this.top + 40) && y < (double)(this.top + 40 + 192);
   }

   private String fittedEntry(String entry) {
      int availableWidth = 222;
      return this.f_96547_.m_92895_(entry) <= availableWidth ? entry : this.f_96547_.m_92834_(entry, availableWidth - this.f_96547_.m_92895_("...")) + "...";
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(graphics);
      LegacyGuiStyle.background(graphics, this.left, this.top, 256, 240);
      graphics.m_280614_(this.f_96547_, this.f_96539_, this.left + 8, this.top + 27, -1, false);
      List<String> filtered = this.filteredEntries();
      this.clampScroll(filtered.size());
      LegacyGuiStyle.background(graphics, this.left + 8, this.top + 40, 240, 192);
      int trackX = this.left + 8 + 240 - 12;
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, trackX, this.top + 40, 12, 192, 80, 120, 40, 128);
      int thumb = this.thumbHeight(filtered.size());
      int thumbY = this.top + 40 + 1 + (int)((long)(190 - thumb) * (long)this.firstRow / (long)Math.max(1, this.maxScroll(filtered.size())));
      LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, trackX + 1, thumbY, 10, thumb, 120, 120, 32, 128);
      graphics.m_280262_();
      graphics.m_280588_(this.left + 8 + 3, this.top + 40 + 3, trackX, this.top + 40 + 192 - 3);
      String tooltip = null;

      for (int row = 0; row < 15 && this.firstRow + row < filtered.size(); row++) {
         String entry = filtered.get(this.firstRow + row);
         int y = this.top + 40 + 3 + row * 12;
         boolean hovered = this.insideList((double)mouseX, (double)mouseY) && mouseY >= y && mouseY < y + 12;
         if (hovered) {
            graphics.m_280509_(this.left + 8 + 3, y, trackX, y + 12, 1080057952);
            if (this.f_96547_.m_92895_(entry) > 222) {
               tooltip = entry;
            }
         }

         graphics.m_280056_(this.f_96547_, this.fittedEntry(entry), this.left + 8 + 3, y + 2, hovered ? -6250336 : -1, false);
      }

      graphics.m_280262_();
      graphics.m_280618_();
      super.m_88315_(graphics, mouseX, mouseY, partialTick);
      if (tooltip != null) {
         graphics.m_280557_(this.f_96547_, Component.m_237113_(tooltip), mouseX, mouseY);
      }
   }

   public boolean m_6375_(double x, double y, int button) {
      List<String> filtered = this.filteredEntries();
      this.clampScroll(filtered.size());
      if (button == 0 && this.insideScrollbar(x, y) && this.maxScroll(filtered.size()) > 0) {
         this.draggingScrollbar = true;
         this.scrollToMouse(y);
         return true;
      } else {
         if (button == 0 && this.insideList(x, y)) {
            int row = (int)(y - (double)this.top - 40.0 - 3.0) / 12;
            if (row < 15 && this.firstRow + row < filtered.size()) {
               String entry = filtered.get(this.firstRow + row);
               int separator = entry.lastIndexOf(" @ ");
               String name = separator < 0 ? entry : entry.substring(0, separator);
               this.box.setTargetName(name);
               AWCorePlatform.sendMailboxConfig(this.box.m_58899_(), 1, name, false);
               this.m_7379_();
               return true;
            }
         }

         return super.m_6375_(x, y, button);
      }
   }

   public boolean m_6050_(double x, double y, double delta) {
      if (!this.insideList(x, y) && !this.insideScrollbar(x, y)) {
         return super.m_6050_(x, y, delta);
      } else {
         this.firstRow = this.firstRow - (int)Math.signum(delta) * 3;
         this.clampScroll(this.filteredEntries().size());
         return true;
      }
   }

   public boolean m_7979_(double x, double y, int button, double dragX, double dragY) {
      if (button == 0 && this.draggingScrollbar) {
         this.scrollToMouse(y);
         return true;
      } else {
         return super.m_7979_(x, y, button, dragX, dragY);
      }
   }

   public boolean m_6348_(double x, double y, int button) {
      if (button == 0) {
         this.draggingScrollbar = false;
      }

      return super.m_6348_(x, y, button);
   }

   public void m_86600_() {
      this.search.m_94120_();
   }

   public void m_7861_() {
      if (active == this) {
         active = null;
      }

      super.m_7861_();
   }

   public void m_7379_() {
      if (active == this) {
         active = null;
      }

      this.f_96541_.m_91152_(this.parent);
   }

   public boolean m_7043_() {
      return false;
   }
}
