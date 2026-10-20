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
      super(Component.translatable("gui.ancientwarfare.town_hall.records"));
      this.parent = parent;
   }

   protected void init() {
      this.guiWidth = Math.min(440, Math.max(1, this.width - 12));
      this.guiHeight = Math.min(290, Math.max(1, this.height - 12));
      this.listWidth = Math.min(160, Math.max(108, this.guiWidth * 37 / 100));
      this.left = (this.width - this.guiWidth) / 2;
      this.top = (this.height - this.guiHeight) / 2;
      String query = this.search == null ? "" : this.search.getValue();
      this.search = new EditBox(
         this.font, this.left + 10, this.top + 36, this.listWidth - 20, 10, Component.translatable("gui.ancientwarfare.town_hall.search")
      );
      this.search.setBordered(false);
      this.search.setMaxLength(128);
      this.search.setValue(query);
      this.search.setResponder(value -> this.listScroll = 0);
      this.addRenderableWidget(this.search);
      this.addRenderableWidget(
         new LegacyTexturedButton(
            this.left + this.guiWidth - 64, this.top + 8, 56, 14, Component.translatable("gui.ancientwarfare.drafting.back"), button -> this.onClose()
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
      String query = this.search == null ? "" : this.search.getValue().toLowerCase(Locale.ROOT).trim();
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
      return Component.translatable("gui.ancientwarfare.town_hall." + state);
   }

   private static String name(TownHallBlockEntity.NpcDeathEntry entry) {
      return entry.npcName().isBlank() ? profession(entry) : entry.npcName();
   }

   private static String profession(TownHallBlockEntity.NpcDeathEntry entry) {
      String subtype = entry.details().getString("NpcSubtype");
      String key = "entity.ancientwarfare." + entry.npcType();
      if (!subtype.isBlank() && I18n.exists(key + "." + subtype)) {
         key = key + "." + subtype;
      } else if (!I18n.exists(key) && I18n.exists(key + "_npc")) {
         key = key + "_npc";
      }

      return I18n.exists(key) ? Component.translatable(key).getString() : entry.npcType();
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

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      LegacyGuiStyle.background(graphics, this.left, this.top, this.guiWidth, this.guiHeight);
      String header = Component.translatable("gui.ancientwarfare.town_hall.deaths_total", new Object[]{this.menu().getDeathNotices().size()}).getString();
      graphics.drawString(this.font, this.fitted(header, this.guiWidth - 80), this.left + 8, this.top + 10, -1, false);
      graphics.fill(this.left + 8, this.top + 32, this.left + this.listWidth - 8, this.top + 50, -1644826);
      graphics.fill(this.left + 9, this.top + 33, this.left + this.listWidth - 9, this.top + 49, -16777216);
      if (this.search.getValue().isEmpty() && !this.search.isFocused()) {
         graphics.drawString(
            this.font,
            this.fitted(Component.translatable("gui.ancientwarfare.town_hall.search").getString(), this.listWidth - 20),
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
      super.render(graphics, mouseX, mouseY, partialTick);
      if (!hoveredItem.isEmpty()) {
         graphics.renderTooltip(this.font, hoveredItem, mouseX, mouseY);
      } else {
         int hovered = this.rowAt((double)mouseX, (double)mouseY, entries.size());
         if (hovered >= 0 && this.font.width(name(entries.get(hovered))) > this.listWidth - 12 - 22) {
            graphics.renderTooltip(this.font, Component.literal(name(entries.get(hovered))), mouseX, mouseY);
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
      graphics.flush();
      graphics.enableScissor(panelLeft + 3, this.listTop() + 3, right, this.listTop() + this.listHeight() - 3);
      int first = Math.max(0, (this.listScroll - 5) / 34);
      int last = Math.min(entries.size(), first + this.listHeight() / 34 + 3);

      for (int row = first; row < last; row++) {
         TownHallBlockEntity.NpcDeathEntry entry = entries.get(row);
         int x = panelLeft + 3 + 2;
         int y = this.listTop() + 3 + 5 + row * 34 - this.listScroll;
         int rowWidth = right - x - 2;
         int stateY = !entry.recordId().equals(this.selectedRecord) && row != hovered ? 40 : 80;
         LegacyGuiStyle.quarteredTexture(graphics, LegacyGuiStyle.WIDGETS, x, y, rowWidth, 34, 0, stateY, 256, 40);
         graphics.drawString(this.font, this.fitted(name(entry), rowWidth - 6), x + 3, y + 3, -1, false);
         graphics.drawString(this.font, this.fitted(profession(entry), rowWidth - 6), x + 3, y + 13, -3355444, false);
         graphics.drawString(
            this.font, this.fitted(status(entry).getString(), rowWidth - 6), x + 3, y + 23, entry.resurrected() ? -4792678 : -1454950, false
         );
      }

      if (entries.isEmpty()) {
         Component empty = Component.translatable("gui.ancientwarfare.town_hall." + (this.menu().getDeathNotices().isEmpty() ? "empty" : "no_results"));
         int y = this.listTop() + 8;

         for (FormattedCharSequence line : this.font.split(empty, panelWidth - 12 - 14)) {
            graphics.drawString(this.font, line, panelLeft + 7, y, -3355444, false);
            y += 11;
         }
      }

      graphics.flush();
      graphics.disableScissor();
   }

   private ItemStack renderDetails(GuiGraphics graphics, List<TownHallDeathScreen.DetailRow> rows, int mouseX, int mouseY) {
      int panelLeft = this.detailLeft();
      int panelTop = this.top + 32;
      int panelWidth = this.detailWidth();
      LegacyGuiStyle.background(graphics, panelLeft, panelTop, panelWidth, this.detailHeight());
      DraftingStationScreen.drawScrollbar(
         graphics, panelLeft, panelTop, panelWidth, this.detailHeight(), detailContentHeight(rows), this.detailScroll, this.maxDetailScroll(rows)
      );
      graphics.flush();
      graphics.enableScissor(panelLeft + 3, panelTop + 3, panelLeft + panelWidth - 12, panelTop + this.detailHeight() - 3);
      int x = panelLeft + 7;
      int y = panelTop + 8 - this.detailScroll;
      ItemStack hovered = ItemStack.EMPTY;

      for (TownHallDeathScreen.DetailRow row : rows) {
         if (y + row.height() > panelTop + 3 && y < panelTop + this.detailHeight() - 3) {
            if (row.stack() != null) {
               LegacyGuiStyle.slot(graphics, panelLeft, panelTop, 7, y - panelTop + 1);
               if (!row.stack().isEmpty()) {
                  graphics.renderItem(row.stack(), x, y + 1);
                  graphics.renderItemDecorations(this.font, row.stack(), x, y + 1);
                  if (insidePanel((double)mouseX, (double)mouseY, panelLeft, panelTop, panelWidth, this.detailHeight())
                     && mouseX >= x
                     && mouseX < x + 16
                     && mouseY >= y + 1
                     && mouseY < y + 17) {
                     hovered = row.stack();
                  }
               }

               graphics.drawString(this.font, this.fitted(row.label().getString(), panelWidth - 12 - 38), x + 22, y + 1, -3355444, false);
               String itemName = row.stack().isEmpty()
                  ? Component.translatable("gui.ancientwarfare.town_hall.none").getString()
                  : row.stack().getHoverName().getString();
               graphics.drawString(this.font, this.fitted(itemName, panelWidth - 12 - 38), x + 22, y + 10, -1, false);
            } else {
               graphics.drawString(this.font, row.text(), x, y, row.color(), false);
            }
         }

         y += row.height();
      }

      graphics.flush();
      graphics.disableScissor();
      return hovered;
   }

   private List<TownHallDeathScreen.DetailRow> detailRows(TownHallBlockEntity.NpcDeathEntry entry) {
      List<TownHallDeathScreen.DetailRow> rows = new ArrayList<>();
      if (entry == null) {
         this.addText(rows, Component.translatable("gui.ancientwarfare.town_hall.select_record"), -3355444);
         return rows;
      } else {
         CompoundTag data = entry.details();
         this.addText(rows, Component.literal(name(entry)), -1);
         this.addField(rows, "profession", profession(entry));
         this.addField(rows, "status", status(entry).getString());
         String owner = data.getString("OwnerName");
         if (owner.isBlank() && data.hasUUID("Owner")) {
            owner = data.getUUID("Owner").toString();
         }

         this.addField(rows, "owner", owner.isBlank() ? unknown() : owner);
         this.addField(rows, "death_cause", entry.deathCause());
         Component deathMessage = Component.literal(data.getString("DeathMessage"));
         if (!data.getString("DeathMessageJson").isBlank()) {
            try {
               Component localized = Serializer.fromJson(data.getString("DeathMessageJson"));
               if (localized != null) {
                  deathMessage = localized;
               }
            } catch (RuntimeException var11) {
            }
         }

         if (!deathMessage.getString().isBlank()) {
            this.addText(rows, deathMessage, -1454950);
         }

         if (!data.getString("KillerName").isBlank()) {
            this.addField(rows, "killer", data.getString("KillerName"));
         }

         long time = data.getLong("DeathTimeMillis");
         this.addField(rows, "death_time", time > 0L ? DEATH_DATE.format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())) : unknown());
         if (data.contains("DeathGameTime", 99) && data.getLong("DeathGameTime") >= 0L) {
            this.addField(rows, "game_time", Long.toString(data.getLong("DeathGameTime")));
         }

         this.addField(rows, "dimension", data.getString("Dimension").isBlank() ? unknown() : data.getString("Dimension"));
         this.addField(rows, "position", coordinates(entry.pos()));
         if (data.hasUUID("NpcUUID")) {
            this.addField(rows, "npc_uuid", data.getUUID("NpcUUID").toString());
         }

         if (data.contains("MaxHealth", 99)) {
            this.addField(rows, "max_health", number((double)data.getFloat("MaxHealth")));
         }

         if (data.contains("AttackDamage", 99)) {
            this.addField(rows, "attack_damage", number(data.getDouble("AttackDamage")));
         }

         if (data.contains("CombatRank", 99)) {
            this.addField(rows, "rank", Integer.toString(data.getInt("CombatRank")));
         }

         if (!data.getString("Command").isBlank()) {
            String commandKey = "gui.ancientwarfare.town_hall.command." + data.getString("Command").toLowerCase(Locale.ROOT);
            this.addField(rows, "command", I18n.exists(commandKey) ? Component.translatable(commandKey).getString() : data.getString("Command"));
         }

         if (data.contains("CommandTarget", 99)) {
            this.addField(rows, "command_target", coordinates(BlockPos.of(data.getLong("CommandTarget"))));
         }

         if (data.contains("HomeSet")) {
            this.addField(
               rows,
               "home",
               data.getBoolean("HomeSet")
                  ? coordinates(BlockPos.of(data.getLong("HomePos")))
                  : Component.translatable("gui.ancientwarfare.town_hall.none").getString()
            );
         }

         if (data.contains("TownHallPos", 99)) {
            this.addField(rows, "town_hall", coordinates(BlockPos.of(data.getLong("TownHallPos"))));
         }

         if (!data.getString("MountType").isBlank()) {
            this.addField(rows, "mount", data.getString("MountType"));
         }

         ListTag equipment = data.getList("Equipment", 10);
         if (!equipment.isEmpty()) {
            this.addText(rows, Component.translatable("gui.ancientwarfare.town_hall.equipment_at_death"), -1454950);

            for (int i = 0; i < equipment.size(); i++) {
               CompoundTag equipped = equipment.getCompound(i);
               rows.add(
                  TownHallDeathScreen.DetailRow.item(
                     Component.translatable("gui.ancientwarfare.town_hall.slot." + equipped.getString("Slot")), ItemStack.of(equipped.getCompound("Stack"))
                  )
               );
            }
         }

         for (String supply : List.of("Food", "Orders", "Upkeep")) {
            if (data.contains(supply, 10)) {
               rows.add(
                  TownHallDeathScreen.DetailRow.item(
                     Component.translatable("gui.ancientwarfare.town_hall." + supply.toLowerCase(Locale.ROOT)), ItemStack.of(data.getCompound(supply))
                  )
               );
            }
         }

         return rows;
      }
   }

   private void addField(List<TownHallDeathScreen.DetailRow> rows, String key, String value) {
      this.addText(rows, Component.translatable("gui.ancientwarfare.town_hall." + key, new Object[]{value}), -2236963);
   }

   private void addText(List<TownHallDeathScreen.DetailRow> rows, Component text, int color) {
      for (FormattedCharSequence line : this.font.split(text, Math.max(24, this.detailWidth() - 12 - 14))) {
         rows.add(new TownHallDeathScreen.DetailRow(line, null, null, 11, color));
      }
   }

   private String fitted(String text, int width) {
      return DraftingStationScreen.fittedText(this.font, text, Math.max(0, width));
   }

   private static String coordinates(BlockPos pos) {
      return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
   }

   private static String number(double value) {
      return String.format(Locale.ROOT, "%.1f", value);
   }

   private static String unknown() {
      return Component.translatable("gui.ancientwarfare.town_hall.unknown").getString();
   }

   public boolean mouseClicked(double x, double y, int button) {
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
            return super.mouseClicked(x, y, button);
         }
      }
   }

   public boolean mouseScrolled(double x, double y, double delta) {
      List<TownHallBlockEntity.NpcDeathEntry> entries = this.filteredEntries();
      List<TownHallDeathScreen.DetailRow> rows = this.detailRows(this.selection(entries));
      if (insidePanel(x, y, this.left + 8, this.listTop(), this.listWidth - 8, this.listHeight())
         || DraftingStationScreen.insideScrollbar(x, y, this.left + 8, this.listTop(), this.listWidth - 8, this.listHeight())) {
         this.listScroll = this.listScroll - (int)Math.signum(delta) * 34 * 2;
         this.clampScroll(entries.size(), rows);
         return true;
      } else if (!insidePanel(x, y, this.detailLeft(), this.top + 32, this.detailWidth(), this.detailHeight())
         && !DraftingStationScreen.insideScrollbar(x, y, this.detailLeft(), this.top + 32, this.detailWidth(), this.detailHeight())) {
         return super.mouseScrolled(x, y, delta);
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

   public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
      if (button == 0 && this.draggingPanel >= 0) {
         List<TownHallBlockEntity.NpcDeathEntry> entries = this.filteredEntries();
         this.scrollToMouse(y, entries.size(), this.detailRows(this.selection(entries)));
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

   public void tick() {
      this.search.tick();
      if (this.minecraft.player == null || this.minecraft.player.containerMenu != this.menu()) {
         this.minecraft.setScreen(null);
      } else if (!this.minecraft.player.isAlive() || this.minecraft.player.isRemoved() || !this.menu().stillValid(this.minecraft.player)) {
         this.minecraft.player.closeContainer();
      }
   }

   public void onClose() {
      if (this.minecraft.player != null && this.minecraft.player.containerMenu == this.menu() && this.menu().stillValid(this.minecraft.player)) {
         this.returningToParent = true;
         this.minecraft.setScreen(this.parent);
      } else {
         this.minecraft.setScreen(null);
      }
   }

   public void removed() {
      if (!this.returningToParent) {
         this.parent.deathRecordsRemoved();
      }

      super.removed();
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static record DetailRow(FormattedCharSequence text, Component label, ItemStack stack, int height, int color) {
      private static TownHallDeathScreen.DetailRow item(Component label, ItemStack stack) {
         return new TownHallDeathScreen.DetailRow(null, label, stack, 22, -1);
      }
   }
}
