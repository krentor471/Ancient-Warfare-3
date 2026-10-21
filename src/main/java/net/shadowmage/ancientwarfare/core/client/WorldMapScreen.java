package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.shadowmage.ancientwarfare.core.faction.FactionNames;
import net.shadowmage.ancientwarfare.core.faction.WorldMap;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;

/**
 * The faction world around its centre: sea and land by terrain, countries in their colours with
 * their names, settlements, capitals, roads and armies on the march, a scale bar and a legend of the
 * signs. Drag to move,
 * scroll to zoom, click to teleport. The view is kept between openings.
 */
public class WorldMapScreen extends Screen {
   private static final ResourceLocation TEXTURE = new ResourceLocation("ancientwarfare", "dynamic/world_map");
   private static final int MARGIN = 24;
   private static final int BUTTONS_END = MARGIN + 164;
   private static final int LEGEND_HEIGHT = 36;
   private static final int WILD_COLOR = 0xFF3A3A32;
   private static final int ROAD_COLOR = 0xFF7A5A32;
   private static final int SEA_COLOR = 0xFF34597E;
   /** Wild land by terrain, indexed by WorldMap.TERRAIN_*: plain, water, deep water, forest, mountain, desert, snow. */
   private static final int[] TERRAIN_COLORS = {0xFFB8B07A, 0xFF4F7FA8, 0xFF34597E, 0xFF7E9A5A, 0xFF9C9488, 0xFFD9C78E, 0xFFE6ECEE};
   /** How much of a faction's colour shows over its land; the rest is the terrain beneath. */
   private static final double FACTION_TINT = 0.75;
   /** Wild land is greyed toward this, so the countries stand out from it. */
   private static final int WILD_GREY = 0xFF8C8A7C;
   private static final double WILD_GREYING = 0.45;
   /** Map kinds shown in the legend of signs, in order. */
   private static final WorldMap.Kind[] LEGEND_KINDS = {
      WorldMap.Kind.CAPITAL, WorldMap.Kind.VILLAGE, WorldMap.Kind.FORT, WorldMap.Kind.FARM, WorldMap.Kind.MINE, WorldMap.Kind.TEMPLE,
      WorldMap.Kind.TAVERN, WorldMap.Kind.TRADE_POST, WorldMap.Kind.GUARD_POST, WorldMap.Kind.OUTLAW, WorldMap.Kind.ARMY
   };
   /** Lengths the scale bar may show, in blocks. */
   private static final int[] SCALE_STEPS = {50, 100, 250, 500, 1000, 2000, 5000};
   private static final int SCALE_BAR_WIDTH = 90;
   private static final int MARCH_COLOR = 0xFFE03030;
   private static final double MIN_SCALE = 0.02;
   private static final double MAX_SCALE = 2.0;
   private static final double HOVER_RANGE = 6.0;
   /** Map scales (pixels per block) from which settlement names show, and their text size. */
   private static final double CAPITAL_NAMES_SCALE = 0.03;
   private static final double TOWN_NAMES_SCALE = 0.12;
   private static final double ALL_NAMES_SCALE = 0.3;
   private static final float NAME_SIZE = 0.75F;
   /** The last view, so reopening the map shows the same place; NaN until first opened. */
   private static double savedX = Double.NaN;
   private static double savedZ;
   private static double savedScale;
   private final WorldMap.Data data;
   private DynamicTexture texture;
   /** Middle of each faction's land in world blocks, and how many cells it holds, by faction index. */
   private double[] labelX;
   private double[] labelZ;
   private int[] landCells;
   /** World point at the middle of the map view, and screen pixels per block. */
   private double viewX;
   private double viewZ;
   private double scale;
   private boolean dragged;

   private WorldMapScreen(WorldMap.Data data) {
      super(Component.translatable("gui.ancientwarfare.world_map.title"));
      this.data = data;
      this.viewX = Double.isNaN(savedX) ? 0.0 : savedX;
      this.viewZ = Double.isNaN(savedX) ? 0.0 : savedZ;
   }

   public static void open(WorldMap.Data data) {
      Minecraft.getInstance().setScreen(new WorldMapScreen(data));
   }

   @Override
   protected void init() {
      if (this.texture == null) {
         NativeImage image = new NativeImage(WorldMap.SIZE, WorldMap.SIZE, false);

         for (int row = 0; row < WorldMap.SIZE; row++) {
            for (int column = 0; column < WorldMap.SIZE; column++) {
               int owner = this.data.owners()[row * WorldMap.SIZE + column] & 0xFF;
               image.setPixelRGBA(column, row, abgr(this.landColor(owner, row, column)));
            }
         }

         this.findCountryMiddles();
         this.texture = new DynamicTexture(image);
         this.minecraft.getTextureManager().register(TEXTURE, this.texture);
         this.scale = Double.isNaN(savedX) ? Math.min(this.mapWidth(), this.mapHeight()) / (double)(WorldMap.SIZE * WorldMap.STEP) : savedScale;
      }

      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.ancientwarfare.world_map.center_player"), button -> this.centerOnPlayer()).bounds(MARGIN, 2, 90, 18).build()
      );
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.ancientwarfare.chronicle.button"), button -> AWCoreNetwork.sendChronicleOpen())
            .bounds(MARGIN + 94, 2, 70, 18)
            .build()
      );
   }

   private void centerOnPlayer() {
      if (this.minecraft.player != null) {
         this.viewX = this.minecraft.player.getX();
         this.viewZ = this.minecraft.player.getZ();
      }
   }

   @Override
   public void removed() {
      savedX = this.viewX;
      savedZ = this.viewZ;
      savedScale = this.scale;
      this.minecraft.getTextureManager().release(TEXTURE);
      this.texture = null;
   }

   /**
    * The terrain beneath, tinted with the owner's colour; water stays water. Land along a border
    * with another owner is darker, and so is the coast, so countries and shores read at a glance.
    */
   private int landColor(int owner, int row, int column) {
      if (this.data.terrain().length == 0) {
         // The terrain is still being surveyed: owners only, as before.
         int color = owner == 0 ? WILD_COLOR : factionColor(this.data.factions().get(owner));
         return this.ownerChanges(owner, row, column) ? shade(color, 0.6) : color;
      }

      int terrain = this.terrain(row, column);
      if (isWater(terrain)) {
         return TERRAIN_COLORS[terrain];
      }

      int color = owner == 0
         ? mix(WILD_GREY, TERRAIN_COLORS[terrain], WILD_GREYING)
         : mix(shade(factionColor(this.data.factions().get(owner)), 1.15), TERRAIN_COLORS[terrain], FACTION_TINT);
      if (owner != 0 && this.ownerChanges(owner, row, column)) {
         return shade(color, 0.45);
      }

      return this.nearWater(row, column) ? shade(color, 0.8) : color;
   }

   private boolean ownerChanges(int owner, int row, int column) {
      for (int[] offset : new int[][]{{0, 1}, {1, 0}, {0, -1}, {-1, 0}}) {
         int r = row + offset[0];
         int c = column + offset[1];
         if (r >= 0 && c >= 0 && r < WorldMap.SIZE && c < WorldMap.SIZE && (this.data.owners()[r * WorldMap.SIZE + c] & 0xFF) != owner
            && !isWater(this.terrain(r, c))) {
            return true;
         }
      }

      return false;
   }

   private boolean nearWater(int row, int column) {
      for (int[] offset : new int[][]{{0, 1}, {1, 0}, {0, -1}, {-1, 0}}) {
         int r = row + offset[0];
         int c = column + offset[1];
         if (r >= 0 && c >= 0 && r < WorldMap.SIZE && c < WorldMap.SIZE && isWater(this.terrain(r, c))) {
            return true;
         }
      }

      return false;
   }

   private int terrain(int row, int column) {
      return this.data.terrain().length == 0 ? WorldMap.TERRAIN_LAND : this.data.terrain()[row * WorldMap.SIZE + column];
   }

   private static boolean isWater(int terrain) {
      return terrain == WorldMap.TERRAIN_WATER || terrain == WorldMap.TERRAIN_DEEP_WATER;
   }

   /** Where to write each country's name: the middle of its dry land. */
   private void findCountryMiddles() {
      int count = this.data.factions().size();
      this.labelX = new double[count];
      this.labelZ = new double[count];
      this.landCells = new int[count];

      for (int row = 0; row < WorldMap.SIZE; row++) {
         for (int column = 0; column < WorldMap.SIZE; column++) {
            int owner = this.data.owners()[row * WorldMap.SIZE + column] & 0xFF;
            if (owner != 0 && owner < count && !isWater(this.terrain(row, column))) {
               this.labelX[owner] += column;
               this.labelZ[owner] += row;
               this.landCells[owner]++;
            }
         }
      }

      for (int i = 1; i < count; i++) {
         if (this.landCells[i] > 0) {
            this.labelX[i] = this.data.originX() + (this.labelX[i] / this.landCells[i] + 0.5) * WorldMap.STEP;
            this.labelZ[i] = this.data.originZ() + (this.labelZ[i] / this.landCells[i] + 0.5) * WorldMap.STEP;
         }
      }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      int left = MARGIN;
      int top = MARGIN;
      int right = left + this.mapWidth();
      int bottom = top + this.mapHeight();
      graphics.fill(left - 2, top - 2, right + 2, bottom + 2, 0xFF2A2018);
      graphics.fill(left - 1, top - 1, right + 1, bottom + 1, 0xFF8A7650);
      graphics.enableScissor(left, top, right, bottom);
      graphics.fill(left, top, right, bottom, this.data.terrain().length == 0 ? 0xFF000000 : SEA_COLOR);
      double size = WorldMap.SIZE * WorldMap.STEP * this.scale;
      int mapX = (int)Math.round(this.screenX(this.data.originX()));
      int mapZ = (int)Math.round(this.screenY(this.data.originZ()));
      graphics.blit(TEXTURE, mapX, mapZ, (int)Math.round(size), (int)Math.round(size), 0.0F, 0.0F, WorldMap.SIZE, WorldMap.SIZE, WorldMap.SIZE, WorldMap.SIZE);

      // Roads thicken as the map zooms in.
      int roadWidth = this.scale >= 0.25 ? 2 : 1;
      for (int[] road : this.data.roads()) {
         for (int i = 0; i + 3 < road.length; i += 2) {
            this.line(graphics, this.screenX(road[i]), this.screenY(road[i + 1]), this.screenX(road[i + 2]), this.screenY(road[i + 3]), ROAD_COLOR, roadWidth);
         }
      }

      this.renderCountryNames(graphics);

      for (int[] march : this.data.marches()) {
         this.line(graphics, this.screenX(march[0]), this.screenY(march[1]), this.screenX(march[2]), this.screenY(march[3]), MARCH_COLOR, 1);
      }

      WorldMap.Marker hovered = this.markerAt(mouseX, mouseY);

      for (WorldMap.Marker marker : this.data.markers()) {
         this.drawMarker(graphics, marker, marker == hovered);
      }

      this.renderSettlementNames(graphics);

      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null) {
         int px = (int)Math.round(this.screenX(minecraft.player.getX()));
         int pz = (int)Math.round(this.screenY(minecraft.player.getZ()));
         // A dot with a short line the way the player faces.
         double yaw = Math.toRadians(minecraft.player.getYRot());
         this.line(graphics, px, pz, px - Math.sin(yaw) * 9, pz + Math.cos(yaw) * 9, 0xFF000000, 2);
         graphics.fill(px - 3, pz - 3, px + 4, pz + 4, 0xFF000000);
         graphics.fill(px - 2, pz - 2, px + 3, pz + 3, 0xFFFFFFFF);
      }

      this.renderScaleBar(graphics, right - SCALE_BAR_WIDTH - 8, bottom - 14);
      graphics.disableScissor();
      // The title follows the buttons; the hint takes the rest of the line when it fits.
      int titleX = Math.max(this.width / 2 - this.font.width(this.title) / 2, BUTTONS_END + 8);
      graphics.drawString(this.font, this.title, titleX, 8, -1);
      Component hint = Component.translatable("gui.ancientwarfare.world_map.hint");
      int hintX = this.width - MARGIN - this.font.width(hint);
      if (hintX > titleX + this.font.width(this.title) + 12) {
         graphics.drawString(this.font, hint, hintX, 8, 0xFFA0A0A0);
      }
      this.renderLegend(graphics, left, bottom + 4);
      if (mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom) {
         this.renderTooltip(graphics, mouseX, mouseY, hovered);
      }

      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY, WorldMap.Marker hovered) {
      int x = (int)Math.floor(this.worldX(mouseX));
      int z = (int)Math.floor(this.worldZ(mouseY));
      List<Component> lines = new ArrayList<>();
      if (hovered != null) {
         if (!hovered.name().isEmpty()) {
            lines.add(FactionNames.component(hovered.name()).withStyle(net.minecraft.ChatFormatting.GOLD));
         }

         lines.add(Component.translatable("gui.ancientwarfare.world_map.kind." + hovered.kind().name().toLowerCase(java.util.Locale.ROOT)));
         String faction = this.data.factions().get(hovered.faction());
         if (!faction.isEmpty()) {
            lines.add(Component.translatable("faction.ancientwarfare." + faction));
         }

         lines.add(Component.literal(hovered.label()));
         x = hovered.x();
         z = hovered.z();
      } else {
         String owner = this.ownerAt(x, z);
         lines.add(owner.isEmpty() ? Component.translatable("gui.ancientwarfare.world_map.wild") : Component.translatable("faction.ancientwarfare." + owner));
      }

      lines.add(Component.literal(x + ", " + z));
      if (this.data.canTeleport()) {
         lines.add(Component.translatable("gui.ancientwarfare.world_map.click_to_teleport"));
      }

      graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
   }

   private void renderLegend(GuiGraphics graphics, int x, int y) {
      int cursor = x;

      for (int i = 1; i < this.data.factions().size(); i++) {
         String faction = this.data.factions().get(i);
         Component name = Component.translatable("faction.ancientwarfare." + faction);
         int width = 12 + this.font.width(name) + 10;
         if (cursor + width > this.width - MARGIN) {
            if (y > this.height - MARGIN) {
               break;
            }

            cursor = x;
            y += 11;
         }

         graphics.fill(cursor, y + 1, cursor + 8, y + 9, factionColor(faction));
         graphics.drawString(this.font, name, cursor + 11, y + 1, -1);
         cursor += width;
      }

      // The signs, on their own line below the countries.
      cursor = x;
      y += 12;

      for (WorldMap.Kind kind : LEGEND_KINDS) {
         Component name = Component.translatable("gui.ancientwarfare.world_map.kind." + kind.name().toLowerCase(java.util.Locale.ROOT));
         int width = 11 + this.font.width(name) + 5;
         if (cursor + width > this.width - MARGIN) {
            break;
         }

         this.drawIcon(graphics, kind, cursor + 4, y + 5, 0xFFB0B0B0, 0xFF000000);
         graphics.drawString(this.font, name, cursor + 11, y + 1, 0xFFD0D0D0);
         cursor += width;
      }

   }

   private void drawMarker(GuiGraphics graphics, WorldMap.Marker marker, boolean hovered) {
      int x = (int)Math.round(this.screenX(marker.x()));
      int y = (int)Math.round(this.screenY(marker.z()));
      String faction = this.data.factions().get(marker.faction());
      int color = faction.isEmpty() ? 0xFFFFFFFF : shade(factionColor(faction), 1.35);
      this.drawIcon(graphics, marker.kind(), x, y, color, hovered ? 0xFFFFFFFF : 0xFF000000);
   }

   /** One map sign, centred on x, y, in the owner's colour where the sign has one. */
   private void drawIcon(GuiGraphics graphics, WorldMap.Kind kind, int x, int y, int color, int outline) {
      switch (kind) {
         case CAPITAL -> {
            // A gold-rimmed square with a gold crown of three points on top.
            graphics.fill(x - 5, y - 4, x + 6, y + 6, outline);
            graphics.fill(x - 4, y - 3, x + 5, y + 5, 0xFFFFD040);
            graphics.fill(x - 2, y - 1, x + 3, y + 3, color);
            graphics.fill(x - 5, y - 7, x - 3, y - 4, 0xFFFFD040);
            graphics.fill(x - 1, y - 8, x + 2, y - 4, 0xFFFFD040);
            graphics.fill(x + 4, y - 7, x + 6, y - 4, 0xFFFFD040);
         }
         case FORT -> {
            graphics.fill(x - 4, y - 4, x + 5, y + 5, outline);
            graphics.fill(x - 3, y - 3, x + 4, y + 4, 0xFF505050);
            graphics.fill(x - 1, y - 1, x + 2, y + 2, color);
         }
         case TRADE_POST -> {
            graphics.fill(x - 2, y - 2, x + 3, y + 3, outline);
            graphics.fill(x - 1, y - 1, x + 2, y + 2, 0xFFFFD040);
         }
         case GUARD_POST -> {
            graphics.fill(x - 2, y - 2, x + 3, y + 3, outline);
            graphics.fill(x - 1, y - 1, x + 2, y + 2, 0xFFB0B0B0);
         }
         case ARMY -> {
            // A banner: a pole and a flag in the faction's colour, outlined in red.
            graphics.fill(x - 4, y - 6, x + 5, y + 3, 0xFFE03030);
            graphics.fill(x - 3, y - 5, x + 4, y + 2, color);
            graphics.fill(x - 4, y + 3, x - 2, y + 7, 0xFF000000);
         }
         case TAVERN -> {
            graphics.fill(x - 2, y - 2, x + 3, y + 3, outline);
            graphics.fill(x - 1, y - 1, x + 2, y + 2, 0xFFE07020);
         }
         case FARM -> {
            graphics.fill(x - 3, y - 3, x + 4, y + 4, outline);
            graphics.fill(x - 2, y - 2, x + 3, y + 3, 0xFFC8D040);
            graphics.fill(x, y, x + 1, y + 1, color);
         }
         case MINE -> {
            graphics.fill(x - 3, y - 3, x + 4, y + 4, outline);
            graphics.fill(x - 2, y - 2, x + 3, y + 3, 0xFF6A5030);
            graphics.fill(x, y, x + 1, y + 1, color);
         }
         case TEMPLE -> {
            graphics.fill(x - 3, y - 3, x + 4, y + 4, outline);
            graphics.fill(x - 2, y - 2, x + 3, y + 3, 0xFFF0F0F0);
            graphics.fill(x, y, x + 1, y + 1, color);
         }
         case HOMESTEAD -> {
            graphics.fill(x - 2, y - 2, x + 3, y + 3, outline);
            graphics.fill(x - 1, y - 1, x + 2, y + 2, color);
         }
         case PLAYER_VILLAGE -> {
            graphics.fill(x - 4, y - 4, x + 5, y + 5, outline);
            graphics.fill(x - 3, y - 3, x + 4, y + 4, 0xFF40A0FF);
         }
         case OUTLAW -> {
            graphics.fill(x - 3, y - 3, x + 4, y + 4, outline);
            graphics.fill(x - 2, y - 2, x + 3, y + 3, 0xFFD02020);
         }
         default -> {
            graphics.fill(x - 3, y - 3, x + 4, y + 4, outline);
            graphics.fill(x - 2, y - 2, x + 3, y + 3, color);
         }
      }
   }

   /** A line drawn as small squares of the given width; roads are short segments, so this stays cheap. */
   private void line(GuiGraphics graphics, double x0, double y0, double x1, double y1, int color, int width) {
      int steps = (int)Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));

      for (int i = 0; i <= steps; i++) {
         double t = steps == 0 ? 0.0 : (double)i / steps;
         int x = (int)Math.round(x0 + (x1 - x0) * t);
         int y = (int)Math.round(y0 + (y1 - y0) * t);
         graphics.fill(x, y, x + width, y + width, color);
      }
   }

   /** Names under the signs: capitals from afar, forts and villages closer in, the rest only up close. */
   private void renderSettlementNames(GuiGraphics graphics) {
      for (WorldMap.Marker marker : this.data.markers()) {
         if (marker.name().isEmpty()) {
            continue;
         }

         double needed = switch (marker.kind()) {
            case CAPITAL -> CAPITAL_NAMES_SCALE;
            case FORT, VILLAGE -> TOWN_NAMES_SCALE;
            default -> ALL_NAMES_SCALE;
         };
         if (this.scale < needed) {
            continue;
         }

         Component name = FactionNames.component(marker.name());
         int x = (int)Math.round(this.screenX(marker.x()));
         int y = (int)Math.round(this.screenY(marker.z()));
         graphics.pose().pushPose();
         graphics.pose().translate(x, y + 7, 0);
         graphics.pose().scale(NAME_SIZE, NAME_SIZE, 1.0F);
         graphics.drawCenteredString(this.font, name, 0, 0, marker.kind() == WorldMap.Kind.CAPITAL ? 0xFFFFE080 : 0xFFF0F0F0);
         graphics.pose().popPose();
      }
   }

   /** Each country's name over the middle of its land, once the country is big enough on screen to hold it. */
   private void renderCountryNames(GuiGraphics graphics) {
      for (int i = 1; i < this.data.factions().size(); i++) {
         if (this.landCells[i] == 0) {
            continue;
         }

         Component name = Component.translatable("faction.ancientwarfare." + this.data.factions().get(i));
         double across = Math.sqrt(this.landCells[i]) * WorldMap.STEP * this.scale;
         if (across >= this.font.width(name) * 0.8) {
            graphics.drawCenteredString(this.font, name, (int)Math.round(this.screenX(this.labelX[i])), (int)Math.round(this.screenY(this.labelZ[i])) - 18, 0xFFFFF4D0);
         }
      }
   }

   /** A bar of a round number of blocks, about SCALE_BAR_WIDTH pixels long. */
   private void renderScaleBar(GuiGraphics graphics, int x, int y) {
      int blocks = SCALE_STEPS[0];
      for (int step : SCALE_STEPS) {
         if (step * this.scale <= SCALE_BAR_WIDTH) {
            blocks = step;
         }
      }

      int length = Math.max(1, (int)Math.round(blocks * this.scale));
      graphics.fill(x - 3, y - 11, x + length + 3, y + 6, 0x90000000);
      graphics.fill(x, y, x + length, y + 3, 0xFFFFFFFF);
      graphics.fill(x, y - 2, x + 1, y + 3, 0xFFFFFFFF);
      graphics.fill(x + length - 1, y - 2, x + length, y + 3, 0xFFFFFFFF);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.world_map.scale", blocks), x, y - 10, 0xFFFFFFFF);
   }

   private WorldMap.Marker markerAt(double mouseX, double mouseY) {
      WorldMap.Marker best = null;
      double bestDistance = HOVER_RANGE * HOVER_RANGE;

      for (WorldMap.Marker marker : this.data.markers()) {
         double dx = this.screenX(marker.x()) - mouseX;
         double dy = this.screenY(marker.z()) - mouseY;
         double distance = dx * dx + dy * dy;
         if (distance < bestDistance) {
            bestDistance = distance;
            best = marker;
         }
      }

      return best;
   }

   private String ownerAt(int x, int z) {
      int column = Math.floorDiv(x - this.data.originX(), WorldMap.STEP);
      int row = Math.floorDiv(z - this.data.originZ(), WorldMap.STEP);
      if (column < 0 || row < 0 || column >= WorldMap.SIZE || row >= WorldMap.SIZE) {
         return "";
      }

      return this.data.factions().get(this.data.owners()[row * WorldMap.SIZE + column] & 0xFF);
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.dragged = false;
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      this.dragged = true;
      this.viewX -= dragX / this.scale;
      this.viewZ -= dragY / this.scale;
      return true;
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      boolean inside = mouseX >= MARGIN && mouseX < MARGIN + this.mapWidth() && mouseY >= MARGIN && mouseY < MARGIN + this.mapHeight();
      if (button == 0 && !this.dragged && inside && this.data.canTeleport()) {
         WorldMap.Marker marker = this.markerAt(mouseX, mouseY);
         int x = marker != null ? marker.x() : (int)Math.floor(this.worldX(mouseX));
         int z = marker != null ? marker.z() : (int)Math.floor(this.worldZ(mouseY));
         AWCoreNetwork.sendWorldMapTeleport(x, z);
         this.onClose();
         return true;
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      // Zoom around the cursor: the world point under it stays put.
      double worldX = this.worldX(mouseX);
      double worldZ = this.worldZ(mouseY);
      this.scale = Mth.clamp(this.scale * (delta > 0 ? 1.25 : 0.8), MIN_SCALE, MAX_SCALE);
      this.viewX = worldX - (mouseX - this.centerX()) / this.scale;
      this.viewZ = worldZ - (mouseY - this.centerY()) / this.scale;
      return true;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private int mapWidth() {
      return this.width - MARGIN * 2;
   }

   private int mapHeight() {
      return this.height - MARGIN * 2 - LEGEND_HEIGHT;
   }

   private double centerX() {
      return MARGIN + this.mapWidth() / 2.0;
   }

   private double centerY() {
      return MARGIN + this.mapHeight() / 2.0;
   }

   private double screenX(double worldX) {
      return this.centerX() + (worldX - this.viewX) * this.scale;
   }

   private double screenY(double worldZ) {
      return this.centerY() + (worldZ - this.viewZ) * this.scale;
   }

   private double worldX(double screenX) {
      return this.viewX + (screenX - this.centerX()) / this.scale;
   }

   private double worldZ(double screenY) {
      return this.viewZ + (screenY - this.centerY()) / this.scale;
   }

   /** Hand-picked colours, so neighbours never look alike; factions not listed get one from their name. */
   private static final Map<String, Integer> COLORS = Map.ofEntries(
      Map.entry("empire", 0xFFC03A3A),
      Map.entry("nogg", 0xFF3A6FD0),
      Map.entry("norska", 0xFF7FB8E8),
      Map.entry("sarkonid", 0xFFE0A030),
      Map.entry("zimba", 0xFF8A5A2A),
      Map.entry("barbarian", 0xFF5FA03A),
      Map.entry("buffloka", 0xFFD8D050),
      Map.entry("witchbane", 0xFF9050C0),
      Map.entry("vyncan", 0xFF2E8A6A),
      Map.entry("kong", 0xFF4A4A9A),
      Map.entry("sealsker", 0xFF50B0B0),
      Map.entry("guild", 0xFFE070B0),
      Map.entry("amazon", 0xFFA0D070),
      Map.entry("reiksgard", 0xFFB07050),
      Map.entry("shakayana", 0xFFF08040),
      Map.entry("smingol", 0xFF708090),
      Map.entry("xoltec", 0xFFD04080),
      Map.entry("zamurai", 0xFFF0F0F0)
   );

   /** A steady colour per faction, the same in every world. */
   public static int factionColor(String faction) {
      Integer fixed = COLORS.get(faction);
      if (fixed != null) {
         return fixed;
      }

      int mixed = faction.hashCode() * 0x9E3779B9;
      float hue = ((mixed >>> 8) & 0xFFFF) / 65536.0F;
      return 0xFF000000 | Mth.hsvToRgb(hue, 0.6F, 0.8F);
   }

   /** a over b, by share of a. */
   private static int mix(int a, int b, double share) {
      int r = (int)(((a >> 16) & 0xFF) * share + ((b >> 16) & 0xFF) * (1.0 - share));
      int g = (int)(((a >> 8) & 0xFF) * share + ((b >> 8) & 0xFF) * (1.0 - share));
      int bl = (int)((a & 0xFF) * share + (b & 0xFF) * (1.0 - share));
      return 0xFF000000 | r << 16 | g << 8 | bl;
   }

   private static int shade(int argb, double factor) {
      int r = Mth.clamp((int)(((argb >> 16) & 0xFF) * factor), 0, 255);
      int g = Mth.clamp((int)(((argb >> 8) & 0xFF) * factor), 0, 255);
      int b = Mth.clamp((int)((argb & 0xFF) * factor), 0, 255);
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   /** NativeImage stores pixels as ABGR. */
   private static int abgr(int argb) {
      return argb & 0xFF00FF00 | (argb & 0xFF) << 16 | (argb >> 16) & 0xFF;
   }
}
