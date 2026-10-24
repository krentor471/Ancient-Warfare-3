package net.shadowmage.ancientwarfare.core.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcDefaultsRegistry;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.structure.AdvancedSpawnerConfig;
import net.shadowmage.ancientwarfare.core.menu.AdvancedSpawnerMenu;

public class AdvancedSpawnerScreen extends AbstractContainerScreen<AdvancedSpawnerMenu> {
   private static final int TEXT_COLOR = -14671840;
   private static final int MUTED_COLOR = -11842741;
   private static final int INPUT_TEXT_COLOR = -1;
   private static final int PANEL = -1515310;
   private static final int PANEL_DARK = -3687255;
   private static final int ROW_HOVER = -1054500;
   private static final int ROW_SELECTED = -2758457;
   private static final int DROPDOWN_ROWS = 8;
   private final List<ResourceLocation> entityOptions = new ArrayList<>();
   private final List<String> factionOptions = new ArrayList<>();
   private EditBox entitySearch;
   private EditBox entityNbt;
   private EditBox minToSpawn;
   private EditBox maxToSpawn;
   private EditBox spawnDelay;
   private EditBox playerRange;
   private EditBox mobRange;
   private EditBox spawnRange;
   private EditBox maxNearby;
   private EditBox spawnYOffset;
   private ResourceLocation selectedEntity;
   private String selectedFaction;
   private boolean entityDropdownOpen;
   private boolean factionDropdownOpen;
   private int entityScroll;
   private int factionScroll;
   private boolean lightSensitive;
   private boolean redstoneSensitive;
   private boolean redstoneMode;
   private boolean transparent;
   private boolean debugMode;
   private Button entityButton;
   private Button factionButton;
   private Button lightButton;
   private Button redstoneButton;
   private Button redstoneModeButton;
   private Button transparentButton;
   private Button debugButton;

   public AdvancedSpawnerScreen(AdvancedSpawnerMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.imageWidth = 348;
      this.imageHeight = 236;
      this.inventoryLabelY = 10000;
      this.buildEntityOptions();
      this.buildFactionOptions();
   }

   protected void init() {
      super.init();
      AdvancedSpawnerConfig config = ((AdvancedSpawnerMenu)this.menu).getConfig();
      this.selectedEntity = ResourceLocation.tryParse(config.entityId());
      this.selectedFaction = config.factionName().isBlank() ? "brigand" : config.factionName();
      this.lightSensitive = config.lightSensitive();
      this.redstoneSensitive = config.redstoneSensitive();
      this.redstoneMode = config.redstoneMode();
      this.transparent = config.transparent();
      this.debugMode = config.debugMode();
      this.entitySearch = this.addTextBox(this.leftPos + 12, this.topPos + 28, 154, this.selectedEntity == null ? "" : this.selectedEntity.toString(), 128);
      this.entityButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("gui.ancientwarfare.advanced_spawner.select_entity"), button -> {
         this.entityDropdownOpen = !this.entityDropdownOpen;
         this.factionDropdownOpen = false;
      }).bounds(this.leftPos + 172, this.topPos + 27, 92, 18).build());
      this.factionButton = (Button)this.addRenderableWidget(Button.builder(Component.empty(), button -> {
         this.factionDropdownOpen = !this.factionDropdownOpen;
         this.entityDropdownOpen = false;
      }).bounds(this.leftPos + 12, this.topPos + 55, 154, 18).build());
      this.entityNbt = this.addTextBox(this.leftPos + 12, this.topPos + 202, 322, config.entityNbt(), 2048);
      this.minToSpawn = this.addNumberBox(this.leftPos + 214, this.topPos + 58, 34, config.minToSpawn(), 3);
      this.maxToSpawn = this.addNumberBox(this.leftPos + 278, this.topPos + 58, 34, config.maxToSpawn(), 3);
      this.spawnDelay = this.addNumberBox(this.leftPos + 214, this.topPos + 82, 34, config.spawnDelay(), 5);
      this.playerRange = this.addNumberBox(this.leftPos + 214, this.topPos + 106, 34, config.playerRange(), 3);
      this.mobRange = this.addNumberBox(this.leftPos + 278, this.topPos + 106, 34, config.mobRange(), 3);
      this.spawnRange = this.addNumberBox(this.leftPos + 214, this.topPos + 130, 34, config.spawnRange(), 3);
      this.maxNearby = this.addNumberBox(this.leftPos + 278, this.topPos + 130, 34, config.maxNearby(), 3);
      this.spawnYOffset = this.addSignedNumberBox(this.leftPos + 214, this.topPos + 154, 34, config.spawnYOffset(), 4);
      this.lightButton = this.addToggle(
         this.leftPos + 12, this.topPos + 82, 80, "gui.ancientwarfare.advanced_spawner.light", this.lightSensitive, button -> {
            this.lightSensitive = !this.lightSensitive;
            this.refreshToggleLabels();
         }
      );
      this.redstoneButton = this.addToggle(
         this.leftPos + 98, this.topPos + 82, 96, "gui.ancientwarfare.advanced_spawner.redstone", this.redstoneSensitive, button -> {
            this.redstoneSensitive = !this.redstoneSensitive;
            this.refreshToggleLabels();
         }
      );
      this.redstoneModeButton = this.addToggle(
         this.leftPos + 12, this.topPos + 106, 122, "gui.ancientwarfare.advanced_spawner.mode", this.redstoneMode, button -> {
            this.redstoneMode = !this.redstoneMode;
            this.refreshToggleLabels();
         }
      );
      this.transparentButton = this.addToggle(
         this.leftPos + 12, this.topPos + 130, 80, "gui.ancientwarfare.advanced_spawner.transparent", this.transparent, button -> {
            this.transparent = !this.transparent;
            this.refreshToggleLabels();
         }
      );
      this.debugButton = this.addToggle(this.leftPos + 98, this.topPos + 130, 96, "gui.ancientwarfare.advanced_spawner.debug", this.debugMode, button -> {
         this.debugMode = !this.debugMode;
         this.refreshToggleLabels();
      });
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.ancientwarfare.advanced_spawner.save"), button -> this.save())
            .bounds(this.leftPos + 214, this.topPos + 176, 58, 18)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.ancientwarfare.advanced_spawner.close"), button -> this.onClose())
            .bounds(this.leftPos + 278, this.topPos + 176, 58, 18)
            .build()
      );
      this.refreshToggleLabels();
   }

   private void buildEntityOptions() {
      this.entityOptions.clear();
      BuiltInRegistries.ENTITY_TYPE
         .entrySet()
         .stream()
         .filter(entry -> ((EntityType)entry.getValue()).canSerialize())
         .map(entry -> ((ResourceKey)entry.getKey()).location())
         .sorted(Comparator.comparing(ResourceLocation::toString))
         .forEach(this.entityOptions::add);
   }

   private void buildFactionOptions() {
      this.factionOptions.clear();
      this.factionOptions.addAll(FactionNpcDefaultsRegistry.factionNames());
      if (!this.factionOptions.contains("brigand")) {
         this.factionOptions.add("brigand");
      }

      this.factionOptions.sort(Comparator.naturalOrder());
   }

   private EditBox addTextBox(int x, int y, int width, String value, int maxLength) {
      EditBox box = new EditBox(this.font, x, y, width, 16, Component.empty());
      box.setMaxLength(maxLength);
      box.setTextColor(-1);
      box.setTextColorUneditable(-4210753);
      box.setValue(value);
      return (EditBox)this.addRenderableWidget(box);
   }

   private EditBox addNumberBox(int x, int y, int width, int value, int maxLength) {
      EditBox box = this.addTextBox(x, y, width, Integer.toString(value), maxLength);
      box.setFilter(text -> text.isEmpty() || text.matches("[0-9]+"));
      return box;
   }

   private EditBox addSignedNumberBox(int x, int y, int width, int value, int maxLength) {
      EditBox box = this.addTextBox(x, y, width, Integer.toString(value), maxLength);
      box.setFilter(text -> text.isEmpty() || text.matches("-?[0-9]+"));
      return box;
   }

   private Button addToggle(int x, int y, int width, String labelKey, boolean value, OnPress onPress) {
      return (Button)this.addRenderableWidget(Button.builder(this.toggleLabel(labelKey, value), onPress).bounds(x, y, width, 18).build());
   }

   private Component toggleLabel(String labelKey, boolean value) {
      return Component.translatable(
         "gui.ancientwarfare.advanced_spawner.toggle", new Object[]{Component.translatable(labelKey), Component.translatable(value ? "options.on" : "options.off")}
      );
   }

   private void refreshToggleLabels() {
      this.factionButton.setMessage(Component.translatable("gui.ancientwarfare.advanced_spawner.faction_value", new Object[]{this.selectedFaction}));
      this.lightButton.setMessage(this.toggleLabel("gui.ancientwarfare.advanced_spawner.light", this.lightSensitive));
      this.redstoneButton.setMessage(this.toggleLabel("gui.ancientwarfare.advanced_spawner.redstone", this.redstoneSensitive));
      this.redstoneModeButton
         .setMessage(
            Component.translatable(
               "gui.ancientwarfare.advanced_spawner.mode_value",
               new Object[]{
                  Component.translatable(this.redstoneMode ? "gui.ancientwarfare.advanced_spawner.powered" : "gui.ancientwarfare.advanced_spawner.unpowered")
               }
            )
         );
      this.transparentButton.setMessage(this.toggleLabel("gui.ancientwarfare.advanced_spawner.transparent", this.transparent));
      this.debugButton.setMessage(this.toggleLabel("gui.ancientwarfare.advanced_spawner.debug", this.debugMode));
   }

   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -1515310);
      graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 20, -3687255);
      graphics.drawString(this.font, this.title, this.leftPos + 10, this.topPos + 7, -14671840, false);
      this.drawLabel(graphics, "gui.ancientwarfare.advanced_spawner.entity_search", 12, 18);
      this.drawLabel(graphics, "gui.ancientwarfare.advanced_spawner.faction", 12, 47);
      this.drawLabel(graphics, "gui.ancientwarfare.advanced_spawner.entity_nbt", 12, 193);
      this.drawLabel(graphics, "gui.ancientwarfare.advanced_spawner.count", 214, 49);
      this.drawLabel(graphics, "gui.ancientwarfare.advanced_spawner.to", 254, 62);
      this.drawLabel(graphics, "gui.ancientwarfare.advanced_spawner.delay", 214, 73);
      this.drawLabel(graphics, "gui.ancientwarfare.advanced_spawner.player", 214, 97);
      this.drawLabel(graphics, "gui.ancientwarfare.advanced_spawner.mob", 254, 97);
      this.drawLabel(graphics, "gui.ancientwarfare.advanced_spawner.spawn", 214, 121);
      this.drawLabel(graphics, "gui.ancientwarfare.advanced_spawner.nearby", 254, 121);
      this.drawLabel(graphics, "gui.ancientwarfare.advanced_spawner.y_offset", 214, 145);
   }

   private void renderDropdowns(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.pose().pushPose();
      graphics.pose().translate(0.0F, 0.0F, 400.0F);
      this.renderEntityDropdown(graphics, mouseX, mouseY);
      this.renderFactionDropdown(graphics, mouseX, mouseY);
      graphics.pose().popPose();
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
   }

   private void drawLabel(GuiGraphics graphics, String key, int x, int y) {
      graphics.drawString(this.font, Component.translatable(key), this.leftPos + x, this.topPos + y, -14671840, false);
   }

   private void renderEntityDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
      if (this.entityDropdownOpen) {
         List<ResourceLocation> filtered = this.filteredEntities();
         int x = this.leftPos + 12;
         int y = this.topPos + 48;
         int width = 252;
         this.renderDropdown(graphics, x, y, width, filtered.size());

         for (int row = 0; row < 8; row++) {
            int index = this.entityScroll + row;
            if (index >= filtered.size()) {
               break;
            }

            ResourceLocation id = filtered.get(index);
            this.renderDropdownRow(graphics, x, y, width, row, id.toString(), id.equals(this.selectedEntity), mouseX, mouseY);
         }
      }
   }

   private void renderFactionDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
      if (this.factionDropdownOpen) {
         int x = this.leftPos + 12;
         int y = this.topPos + 75;
         int width = 154;
         this.renderDropdown(graphics, x, y, width, this.factionOptions.size());

         for (int row = 0; row < 8; row++) {
            int index = this.factionScroll + row;
            if (index >= this.factionOptions.size()) {
               break;
            }

            String faction = this.factionOptions.get(index);
            this.renderDropdownRow(graphics, x, y, width, row, faction, faction.equals(this.selectedFaction), mouseX, mouseY);
         }
      }
   }

   private void renderDropdown(GuiGraphics graphics, int x, int y, int width, int size) {
      int rows = Math.min(8, size);
      graphics.fill(x - 1, y - 1, x + width + 1, y + rows * 14 + 1, -12963029);
      graphics.fill(x, y, x + width, y + rows * 14, -462104);
   }

   private void renderDropdownRow(GuiGraphics graphics, int x, int y, int width, int row, String text, boolean selected, int mouseX, int mouseY) {
      int rowY = y + row * 14;
      boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= rowY && mouseY < rowY + 14;
      if (selected || hovered) {
         graphics.fill(x, rowY, x + width, rowY + 14, selected ? -2758457 : -1054500);
      }

      graphics.drawString(this.font, Component.literal(this.font.plainSubstrByWidth(text, width - 8)), x + 4, rowY + 3, -14671840, false);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (this.entityDropdownOpen && this.selectEntity(mouseX, mouseY)) {
         return true;
      } else {
         return this.factionDropdownOpen && this.selectFaction(mouseX, mouseY) ? true : super.mouseClicked(mouseX, mouseY, button);
      }
   }

   private boolean selectEntity(double mouseX, double mouseY) {
      List<ResourceLocation> filtered = this.filteredEntities();
      int x = this.leftPos + 12;
      int y = this.topPos + 48;
      int width = 252;
      int row = this.dropdownRow(mouseX, mouseY, x, y, width, filtered.size());
      if (row < 0) {
         return false;
      } else {
         this.selectedEntity = filtered.get(this.entityScroll + row);
         this.entitySearch.setValue(this.selectedEntity.toString());
         this.entityDropdownOpen = false;
         return true;
      }
   }

   private boolean selectFaction(double mouseX, double mouseY) {
      int x = this.leftPos + 12;
      int y = this.topPos + 75;
      int width = 154;
      int row = this.dropdownRow(mouseX, mouseY, x, y, width, this.factionOptions.size());
      if (row < 0) {
         return false;
      } else {
         this.selectedFaction = this.factionOptions.get(this.factionScroll + row);
         this.factionDropdownOpen = false;
         this.refreshToggleLabels();
         return true;
      }
   }

   private int dropdownRow(double mouseX, double mouseY, int x, int y, int width, int size) {
      int rows = Math.min(8, size);
      return !(mouseX < (double)x) && !(mouseX >= (double)(x + width)) && !(mouseY < (double)y) && !(mouseY >= (double)(y + rows * 14))
         ? Math.min(rows - 1, (int)((mouseY - (double)y) / 14.0))
         : -1;
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      if (this.entityDropdownOpen) {
         this.entityScroll = this.scroll(this.entityScroll, this.filteredEntities().size(), delta);
         return true;
      } else if (this.factionDropdownOpen) {
         this.factionScroll = this.scroll(this.factionScroll, this.factionOptions.size(), delta);
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, delta);
      }
   }

   private int scroll(int value, int size, double delta) {
      int max = Math.max(0, size - 8);
      return Math.max(0, Math.min(max, value - (int)Math.signum(delta)));
   }

   private List<ResourceLocation> filteredEntities() {
      String filter = this.entitySearch == null ? "" : this.entitySearch.getValue().toLowerCase(Locale.ROOT);
      return this.entityOptions.stream().filter(id -> filter.isBlank() || id.toString().toLowerCase(Locale.ROOT).contains(filter)).toList();
   }

   private void save() {
      ResourceLocation entity = this.selectedEntity;
      ResourceLocation typed = ResourceLocation.tryParse(this.entitySearch.getValue());
      if (typed != null && this.entityOptions.contains(typed)) {
         entity = typed;
      }

      AdvancedSpawnerConfig config = new AdvancedSpawnerConfig(
         entity == null ? "" : entity.toString(),
         this.selectedFaction,
         this.entityNbt.getValue(),
         this.number(this.minToSpawn, 1),
         this.number(this.maxToSpawn, 1),
         this.number(this.spawnDelay, 0),
         this.number(this.playerRange, 0),
         this.number(this.mobRange, 0),
         this.number(this.spawnRange, 0),
         this.number(this.maxNearby, 0),
         this.signedNumber(this.spawnYOffset, 0),
         this.lightSensitive,
         this.redstoneSensitive,
         this.redstoneMode,
         this.transparent,
         this.debugMode
      );
      AWCorePlatform.setAdvancedSpawnerConfig(((AdvancedSpawnerMenu)this.menu).getPos(), config);
      this.onClose();
   }

   private int number(EditBox box, int fallback) {
      try {
         return Math.max(0, Integer.parseInt(box.getValue()));
      } catch (NumberFormatException var4) {
         return fallback;
      }
   }

   private int signedNumber(EditBox box, int fallback) {
      try {
         return Integer.parseInt(box.getValue());
      } catch (NumberFormatException var4) {
         return fallback;
      }
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.renderDropdowns(graphics, mouseX, mouseY);
      this.renderTooltip(graphics, mouseX, mouseY);
   }
}
