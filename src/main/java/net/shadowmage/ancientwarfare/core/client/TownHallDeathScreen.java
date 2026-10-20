package net.shadowmage.ancientwarfare.core.client;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.menu.TownHallMenu;
import net.shadowmage.ancientwarfare.core.tile.TownHallBlockEntity;

public class TownHallDeathScreen extends Screen {
   private static final String KEY = "gui.ancientwarfare.town_hall.";
   private static final int ROW_HEIGHT = 34;
   private static final int PADDING = 3;
   private static final int SCROLLBAR_WIDTH = 12;
   private static final DateTimeFormatter DEATH_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
   private final TownHallScreen parent;
   private EditBox search;
   private int left;
   private int top;
   private int guiWidth;
   private int guiHeight;
   private int listWidth;
   private int listScroll;
   private int detailScroll;
   private int draggingPanel = -1;
   private UUID selectedRecord;
   private boolean returningToParent;

   public TownHallDeathScreen(TownHallScreen parent) {
      super(Component.m_237115_("gui.ancientwarfare.town_hall.records"));
      this.parent = parent;
   }

   protected void m_7856_() {
      this.guiWidth = Math.min(440, Math.max(1, this.f_96543_ - 12));
      this.guiHeight = Math.min(290, Math.max(1, this.f_96544_ - 12));
      this.listWidth = Math.min(160, Math.max(108, this.guiWidth * 37 / 100));
      this.left = (this.f_96543_ - this.guiWidth) / 2;
      this.top = (this.f_96544_ - this.guiHeight) / 2;
      String query = this.search == null ? "" : this.search.m_94155_();
      this.search = new EditBox(
         this.f_96547_, this.left + 10, this.top + 36, this.listWidth - 20, 10, Component.m_237115_("gui.ancientwarfare.town_hall.search")
      );
      this.search.m_94182_(false);
      this.search.m_94199_(128);
      this.search.m_94144_(query);
      this.search.m_94151_(value -> this.listScroll = 0);
      this.m_142416_(this.search);
      this.m_142416_(
         new LegacyTexturedButton(
            this.left + this.guiWidth - 64, this.top + 8, 56, 14, Component.m_237115_("gui.ancientwarfare.drafting.back"), button -> this.m_7379_()
         )
      );
      this.draggingPanel = -1;
   }

   private TownHallMenu menu() {
      return this.parent.getMenu();
   }

   private int listTop() {
      return this.top + 56;
   }

   private int listHeight() {
      return this.guiHeight - 64;
   }

   private int detailLeft() {
      return this.left + this.listWidth + 4;
   }

   private int detailWidth() {
      return this.guiWidth - this.listWidth - 12;
   }

   private int detailHeight() {
      return this.guiHeight - 40;
   }

   private List<TownHallBlockEntity.NpcDeathEntry> filteredEntries() {
      String query = this.search == null ? "" : this.search.m_94155_().toLowerCase(Locale.ROOT).trim();
      List<TownHallBlockEntity.NpcDeathEntry> entries = new ArrayList<>(this.menu().getDeathNotices());
      Collections.reverse(entries);
      if (!query.isEmpty()) {
         entries.removeIf(entry -> !(name(entry) + " " + profession(entry) + " " + status(entry).getString()).toLowerCase(Locale.ROOT).contains(query));
      }

      return entries;
   }

   private TownHallBlockEntity.NpcDeathEntry selection(List<TownHallBlockEntity.NpcDeathEntry> entries) {
      for (TownHallBlockEntity.NpcDeathEntry entry : entries) {
         if (entry.recordId().equals(this.selectedRecord)) {
            return entry;
         }
      }

      if (entries.isEmpty()) {
         return null;
      } else {
         this.selectedRecord = entries.get(0).recordId();
         this.detailScroll = 0;
         return entries.get(0);
      }
   }

   private static Component status(TownHallBlockEntity.NpcDeathEntry entry) {
      String state = entry.resurrected()
         ? "resurrected"
         : (entry.beingResurrected() ? "resurrecting" : (entry.canResurrect() ? "awaiting_resurrection" : "recorded"));
      return Component.m_237115_("gui.ancientwarfare.town_hall." + state);
   }

   private static String name(TownHallBlockEntity.NpcDeathEntry entry) {
      return entry.npcName().isBlank() ? profession(entry) : entry.npcName();
   }

   private static String profession(TownHallBlockEntity.NpcDeathEntry entry) {
      String subtype = entry.details().m_128461_("NpcSubtype");
      String key = "entity.ancientwarfare." + entry.npcType();
      if (!subtype.isBlank() && I18n.m_118936_(key + "." + subtype)) {
         key = key + "." + subtype;
      } else if (!I18n.m_118936_(key) && I18n.m_118936_(key + "_npc")) {
         key = key + "_npc";
      }

      return I18n.m_118936_(key) ? Component.m_237115_(key).getString() : entry.npcType();
   }

   private static int listContentHeight(int count) {
      return 10 + count * 34;
   }

   private int maxListScroll(int count) {
      return Math.max(0, listContentHeight(count) - this.listHeight() + 6);
   }

   private int maxDetailScroll(List<TownHallDeathScreen.DetailRow> rows) {
      return Math.max(0, detailContentHeight(rows) - this.detailHeight() + 6);
   }

   private static int detailContentHeight(List<TownHallDeathScreen.DetailRow> rows) {
      return 10 + rows.stream().mapToInt(TownHallDeathScreen.DetailRow::height).sum();
   }

   private void clampScroll(int count, List<TownHallDeathScreen.DetailRow> rows) {
      this.listScroll = Math.max(0, Math.min(this.listScroll, this.maxListScroll(count)));
      this.detailScroll = Math.max(0, Math.min(this.detailScroll, this.maxDetailScroll(rows)));
   }

   private int rowAt(double x, double y, int count) {
      if (!insidePanel(x, y, this.left + 8, this.listTop(), this.listWidth - 8, this.listHeight())) {
         return -1;
      } else {
         double contentY = y - (double)this.listTop() - 3.0 - 5.0 + (double)this.listScroll;
         if (contentY < 0.0) {
            return -1;
         } else {
            int row = (int)contentY / 34;
            return row < count ? row : -1;
         }
      }
   }

   private static boolean insidePanel(double x, double y, int left, int top, int width, int height) {
      return x >= (double)(left + 3) && x < (double)(left + width - 12) && y >= (double)(top + 3) && y < (double)(top + height - 3);
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(graphics);
      LegacyGuiStyle.background(graphics, this.left, this.top, this.guiWidth, this.guiHeight);
      String header = Component.m_237110_("gui.ancientwarfare.town_hall.deaths_total", new Object[]{this.menu().getDeathNotices().size()}).getString();
      graphics.m_280056_(this.f_96547_, this.fitted(header, this.guiWidth - 80), this.left + 8, this.top + 10, -1, false);
      graphics.m_280509_(this.left + 8, this.top + 32, this.left + this.listWidth - 8, this.top + 50, -1644826);
      graphics.m_280509_(this.left + 9, this.top + 33, this.left + this.listWidth - 9, this.top + 49, -16777216);
      if (this.search.m_94155_().isEmpty() && !this.search.m_93696_()) {
         graphics.m_280056_(
            this.f_96547_,
            this.fitted(Component.m_237115_("gui.ancientwarfare.town_hall.search").getString(), this.listWidth - 20),
            this.left + 10,
            this.top + 36,
            -7829368,
            false
         );
      }

      List<TownHallBlockEntity.NpcDeathEntry> entries = this.filteredEntries();
      TownHallBlockEntity.NpcDeathEntry selected = this.selection(entries);
      List<TownHallDeathScreen.DetailRow> details = this.detailRows(selected);
      this.clampScroll(entries.size(), details);
      this.renderList(graphics, entries, mouseX, mouseY);
      ItemStack hoveredItem = this.renderDetails(graphics, details, mouseX, mouseY);
      super.m_88315_(graphics, mouseX, mouseY, partialTick);
      if (!hoveredItem.m_41619_()) {
         graphics.m_280153_(this.f_96547_, hoveredItem, mouseX, mouseY);
      } else {
         int hovered = this.rowAt((double)mouseX, (double)mouseY, entries.size());
         if (hovered >= 0 && this.f_96547_.m_92895_(name(entries.get(hovered))) > this.listWidth - 12 - 22) {
            graphics.m_280557_(this.f_96547_, Component.m_237113_(name(entries.get(hovered))), mouseX, mouseY);
         }
      }
   }

   private void renderList(GuiGraphics graphics, List<TownHallBlockEntity.NpcDeathEntry> entries, int mouseX, int mouseY) {
      int panelLeft = this.left + 8;
      int panelWidth = this.listWidth - 8;
      LegacyGuiStyle.background(graphics, panelLeft, this.listTop(), panelWidth, this.listHeight());
      DraftingStationScreen.drawScrollbar(
         graphics,
         panelLeft,
         this.listTop(),
         panelWidth,
         this.listHeight(),
         listContentHeight(entries.size()),
         this.listScroll,
         this.maxListScroll(entries.size())
      );
      int right = panelLeft + panelWidth - 12;
      int hovered = this.rowAt((double)mouseX, (double)mouseY, entries.size());
      graphics.m_280262_();
      graphics.m_280588_(panelLeft + 3, this.listTop() + 3, right, this.listTop() + this.listHeight() - 3);
      int first = Math.max(0, (this.listScroll - 5) / 34);
      int last = Math.min(entries.size(), first + this.listHeight() / 34 + 3);

      for (int row = first; row < last; row++) {
         TownHallBlockEntity.NpcDeathEntry entry = entries.get(row);
         int x = panelLeft + 3 + 2;
         int y = this.listTop() + 3 + 5 + row * 34 - this.listScroll;
         int rowWidth = right - x - 2;
         int stateY = !entry.recordId().equals(this.selectedRecord) && row != hovered ? 40 : 80;
         LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, x, y, rowWidth, 34, 0, stateY, 256, 40);
         graphics.m_280056_(this.f_96547_, this.fitted(name(entry), rowWidth - 6), x + 3, y + 3, -1, false);
         graphics.m_280056_(this.f_96547_, this.fitted(profession(entry), rowWidth - 6), x + 3, y + 13, -3355444, false);
         graphics.m_280056_(
            this.f_96547_, this.fitted(status(entry).getString(), rowWidth - 6), x + 3, y + 23, entry.resurrected() ? -4792678 : -1454950, false
         );
      }

      if (entries.isEmpty()) {
         Component empty = Component.m_237115_("gui.ancientwarfare.town_hall." + (this.menu().getDeathNotices().isEmpty() ? "empty" : "no_results"));
         int y = this.listTop() + 8;

         for (FormattedCharSequence line : this.f_96547_.m_92923_(empty, panelWidth - 12 - 14)) {
            graphics.m_280649_(this.f_96547_, line, panelLeft + 7, y, -3355444, false);
            y += 11;
         }
      }

      graphics.m_280262_();
      graphics.m_280618_();
   }

   private ItemStack renderDetails(GuiGraphics graphics, List<TownHallDeathScreen.DetailRow> rows, int mouseX, int mouseY) {
      int panelLeft = this.detailLeft();
      int panelTop = this.top + 32;
      int panelWidth = this.detailWidth();
      LegacyGuiStyle.background(graphics, panelLeft, panelTop, panelWidth, this.detailHeight());
      DraftingStationScreen.drawScrollbar(
         graphics, panelLeft, panelTop, panelWidth, this.detailHeight(), detailContentHeight(rows), this.detailScroll, this.maxDetailScroll(rows)
      );
      graphics.m_280262_();
      graphics.m_280588_(panelLeft + 3, panelTop + 3, panelLeft + panelWidth - 12, panelTop + this.detailHeight() - 3);
      int x = panelLeft + 7;
      int y = panelTop + 8 - this.detailScroll;
      ItemStack hovered = ItemStack.f_41583_;

      for (TownHallDeathScreen.DetailRow row : rows) {
         if (y + row.height() > panelTop + 3 && y < panelTop + this.detailHeight() - 3) {
            if (row.stack() != null) {
               LegacyGuiStyle.slot(graphics, panelLeft, panelTop, 7, y - panelTop + 1);
               if (!row.stack().m_41619_()) {
                  graphics.m_280480_(row.stack(), x, y + 1);
                  graphics.m_280370_(this.f_96547_, row.stack(), x, y + 1);
                  if (insidePanel((double)mouseX, (double)mouseY, panelLeft, panelTop, panelWidth, this.detailHeight())
                     && mouseX >= x
                     && mouseX < x + 16
                     && mouseY >= y + 1
                     && mouseY < y + 17) {
                     hovered = row.stack();
                  }
               }

               graphics.m_280056_(this.f_96547_, this.fitted(row.label().getString(), panelWidth - 12 - 38), x + 22, y + 1, -3355444, false);
               String itemName = row.stack().m_41619_()
                  ? Component.m_237115_("gui.ancientwarfare.town_hall.none").getString()
                  : row.stack().m_41786_().getString();
               graphics.m_280056_(this.f_96547_, this.fitted(itemName, panelWidth - 12 - 38), x + 22, y + 10, -1, false);
            } else {
               graphics.m_280649_(this.f_96547_, row.text(), x, y, row.color(), false);
            }
         }

         y += row.height();
      }

      graphics.m_280262_();
      graphics.m_280618_();
      return hovered;
   }

   private List<TownHallDeathScreen.DetailRow> detailRows(TownHallBlockEntity.NpcDeathEntry entry) {
      List<TownHallDeathScreen.DetailRow> rows = new ArrayList<>();
      if (entry == null) {
         this.addText(rows, Component.m_237115_("gui.ancientwarfare.town_hall.select_record"), -3355444);
         return rows;
      } else {
         CompoundTag data = entry.details();
         this.addText(rows, Component.m_237113_(name(entry)), -1);
         this.addField(rows, "profession", profession(entry));
         this.addField(rows, "status", status(entry).getString());
         String owner = data.m_128461_("OwnerName");
         if (owner.isBlank() && data.m_128403_("Owner")) {
            owner = data.m_128342_("Owner").toString();
         }

         this.addField(rows, "owner", owner.isBlank() ? unknown() : owner);
         this.addField(rows, "death_cause", entry.deathCause());
         Component deathMessage = Component.m_237113_(data.m_128461_("DeathMessage"));
         if (!data.m_128461_("DeathMessageJson").isBlank()) {
            try {
               Component localized = Serializer.m_130701_(data.m_128461_("DeathMessageJson"));
               if (localized != null) {
                  deathMessage = localized;
               }
            } catch (RuntimeException var11) {
            }
         }

         if (!deathMessage.getString().isBlank()) {
            this.addText(rows, deathMessage, -1454950);
         }

         if (!data.m_128461_("KillerName").isBlank()) {
            this.addField(rows, "killer", data.m_128461_("KillerName"));
         }

         long time = data.m_128454_("DeathTimeMillis");
         this.addField(rows, "death_time", time > 0L ? DEATH_DATE.format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())) : unknown());
         if (data.m_128425_("DeathGameTime", 99) && data.m_128454_("DeathGameTime") >= 0L) {
            this.addField(rows, "game_time", Long.toString(data.m_128454_("DeathGameTime")));
         }

         this.addField(rows, "dimension", data.m_128461_("Dimension").isBlank() ? unknown() : data.m_128461_("Dimension"));
         this.addField(rows, "position", coordinates(entry.pos()));
         if (data.m_128403_("NpcUUID")) {
            this.addField(rows, "npc_uuid", data.m_128342_("NpcUUID").toString());
         }

         if (data.m_128425_("MaxHealth", 99)) {
            this.addField(rows, "max_health", number((double)data.m_128457_("MaxHealth")));
         }

         if (data.m_128425_("AttackDamage", 99)) {
            this.addField(rows, "attack_damage", number(data.m_128459_("AttackDamage")));
         }

         if (data.m_128425_("CombatRank", 99)) {
            this.addField(rows, "rank", Integer.toString(data.m_128451_("CombatRank")));
         }

         if (!data.m_128461_("Command").isBlank()) {
            String commandKey = "gui.ancientwarfare.town_hall.command." + data.m_128461_("Command").toLowerCase(Locale.ROOT);
            this.addField(rows, "command", I18n.m_118936_(commandKey) ? Component.m_237115_(commandKey).getString() : data.m_128461_("Command"));
         }

         if (data.m_128425_("CommandTarget", 99)) {
            this.addField(rows, "command_target", coordinates(BlockPos.m_122022_(data.m_128454_("CommandTarget"))));
         }

         if (data.m_128441_("HomeSet")) {
            this.addField(
               rows,
               "home",
               data.m_128471_("HomeSet")
                  ? coordinates(BlockPos.m_122022_(data.m_128454_("HomePos")))
                  : Component.m_237115_("gui.ancientwarfare.town_hall.none").getString()
            );
         }

         if (data.m_128425_("TownHallPos", 99)) {
            this.addField(rows, "town_hall", coordinates(BlockPos.m_122022_(data.m_128454_("TownHallPos"))));
         }

         if (!data.m_128461_("MountType").isBlank()) {
            this.addField(rows, "mount", data.m_128461_("MountType"));
         }

         ListTag equipment = data.m_128437_("Equipment", 10);
         if (!equipment.isEmpty()) {
            this.addText(rows, Component.m_237115_("gui.ancientwarfare.town_hall.equipment_at_death"), -1454950);

            for (int i = 0; i < equipment.size(); i++) {
               CompoundTag equipped = equipment.m_128728_(i);
               rows.add(
                  TownHallDeathScreen.DetailRow.item(
                     Component.m_237115_("gui.ancientwarfare.town_hall.slot." + equipped.m_128461_("Slot")), ItemStack.m_41712_(equipped.m_128469_("Stack"))
                  )
               );
            }
         }

         for (String supply : List.of("Food", "Orders", "Upkeep")) {
            if (data.m_128425_(supply, 10)) {
               rows.add(
                  TownHallDeathScreen.DetailRow.item(
                     Component.m_237115_("gui.ancientwarfare.town_hall." + supply.toLowerCase(Locale.ROOT)), ItemStack.m_41712_(data.m_128469_(supply))
                  )
               );
            }
         }

         return rows;
      }
   }

   private void addField(List<TownHallDeathScreen.DetailRow> rows, String key, String value) {
      this.addText(rows, Component.m_237110_("gui.ancientwarfare.town_hall." + key, new Object[]{value}), -2236963);
   }

   private void addText(List<TownHallDeathScreen.DetailRow> rows, Component text, int color) {
      for (FormattedCharSequence line : this.f_96547_.m_92923_(text, Math.max(24, this.detailWidth() - 12 - 14))) {
         rows.add(new TownHallDeathScreen.DetailRow(line, null, null, 11, color));
      }
   }

   private String fitted(String text, int width) {
      return DraftingStationScreen.fittedText(this.f_96547_, text, Math.max(0, width));
   }

   private static String coordinates(BlockPos pos) {
      return pos.m_123341_() + ", " + pos.m_123342_() + ", " + pos.m_123343_();
   }

   private static String number(double value) {
      return String.format(Locale.ROOT, "%.1f", value);
   }

   private static String unknown() {
      return Component.m_237115_("gui.ancientwarfare.town_hall.unknown").getString();
   }

   public boolean m_6375_(double x, double y, int button) {
      List<TownHallBlockEntity.NpcDeathEntry> entries = this.filteredEntries();
      List<TownHallDeathScreen.DetailRow> rows = this.detailRows(this.selection(entries));
      this.clampScroll(entries.size(), rows);
      if (button == 0
         && DraftingStationScreen.insideScrollbar(x, y, this.left + 8, this.listTop(), this.listWidth - 8, this.listHeight())
         && this.maxListScroll(entries.size()) > 0) {
         this.draggingPanel = 0;
         this.scrollToMouse(y, entries.size(), rows);
         return true;
      } else if (button == 0
         && DraftingStationScreen.insideScrollbar(x, y, this.detailLeft(), this.top + 32, this.detailWidth(), this.detailHeight())
         && this.maxDetailScroll(rows) > 0) {
         this.draggingPanel = 1;
         this.scrollToMouse(y, entries.size(), rows);
         return true;
      } else {
         int row = this.rowAt(x, y, entries.size());
         if (button == 0 && row >= 0) {
            this.selectedRecord = entries.get(row).recordId();
            this.detailScroll = 0;
            return true;
         } else {
            return super.m_6375_(x, y, button);
         }
      }
   }

   public boolean m_6050_(double x, double y, double delta) {
      List<TownHallBlockEntity.NpcDeathEntry> entries = this.filteredEntries();
      List<TownHallDeathScreen.DetailRow> rows = this.detailRows(this.selection(entries));
      if (insidePanel(x, y, this.left + 8, this.listTop(), this.listWidth - 8, this.listHeight())
         || DraftingStationScreen.insideScrollbar(x, y, this.left + 8, this.listTop(), this.listWidth - 8, this.listHeight())) {
         this.listScroll = this.listScroll - (int)Math.signum(delta) * 34 * 2;
         this.clampScroll(entries.size(), rows);
         return true;
      } else if (!insidePanel(x, y, this.detailLeft(), this.top + 32, this.detailWidth(), this.detailHeight())
         && !DraftingStationScreen.insideScrollbar(x, y, this.detailLeft(), this.top + 32, this.detailWidth(), this.detailHeight())) {
         return super.m_6050_(x, y, delta);
      } else {
         this.detailScroll = this.detailScroll - (int)Math.signum(delta) * 33;
         this.clampScroll(entries.size(), rows);
         return true;
      }
   }

   private void scrollToMouse(double y, int count, List<TownHallDeathScreen.DetailRow> rows) {
      if (this.draggingPanel == 0) {
         this.listScroll = DraftingStationScreen.scrollOffsetAtMouse(y, this.listTop(), this.listHeight(), listContentHeight(count), this.maxListScroll(count));
      } else if (this.draggingPanel == 1) {
         this.detailScroll = DraftingStationScreen.scrollOffsetAtMouse(
            y, this.top + 32, this.detailHeight(), detailContentHeight(rows), this.maxDetailScroll(rows)
         );
      }
   }

   public boolean m_7979_(double x, double y, int button, double dragX, double dragY) {
      if (button == 0 && this.draggingPanel >= 0) {
         List<TownHallBlockEntity.NpcDeathEntry> entries = this.filteredEntries();
         this.scrollToMouse(y, entries.size(), this.detailRows(this.selection(entries)));
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

   public void m_86600_() {
      this.search.m_94120_();
      if (this.f_96541_.f_91074_ == null || this.f_96541_.f_91074_.f_36096_ != this.menu()) {
         this.f_96541_.m_91152_(null);
      } else if (!this.f_96541_.f_91074_.m_6084_() || this.f_96541_.f_91074_.m_213877_() || !this.menu().m_6875_(this.f_96541_.f_91074_)) {
         this.f_96541_.f_91074_.m_6915_();
      }
   }

   public void m_7379_() {
      if (this.f_96541_.f_91074_ != null && this.f_96541_.f_91074_.f_36096_ == this.menu() && this.menu().m_6875_(this.f_96541_.f_91074_)) {
         this.returningToParent = true;
         this.f_96541_.m_91152_(this.parent);
      } else {
         this.f_96541_.m_91152_(null);
      }
   }

   public void m_7861_() {
      if (!this.returningToParent) {
         this.parent.deathRecordsRemoved();
      }

      super.m_7861_();
   }

   public boolean m_7043_() {
      return false;
   }

   private static record DetailRow(FormattedCharSequence text, Component label, ItemStack stack, int height, int color) {
      private static TownHallDeathScreen.DetailRow item(Component label, ItemStack stack) {
         return new TownHallDeathScreen.DetailRow(null, label, stack, 22, -1);
      }
   }
}
