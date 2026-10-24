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
import net.shadowmage.ancientwarfare.core.faction.FactionColors;
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
   private static final int BUTTONS_END = MARGIN + 248;
   private static final int LEGEND_HEIGHT = 24;
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
   /** Texture pixels per map cell, and how ragged the coasts are drawn. */
   private static final int DETAIL = 4;
   private static final double COAST_ROUGHNESS = 0.35;
   private static final int SHALLOW_COLOR = 0xFF8CC4DE;
   private static final int COAST_COLOR = 0xFF3E3A2C;
   private static final int RIVER_COLOR = 0xFF4A86BE;
   /** Map kinds shown in the legend of signs, in order. */
   private static final WorldMap.Kind[] LEGEND_KINDS = {
      WorldMap.Kind.CAPITAL, WorldMap.Kind.VILLAGE, WorldMap.Kind.FORT, WorldMap.Kind.FARM, WorldMap.Kind.MINE, WorldMap.Kind.TEMPLE,
      WorldMap.Kind.TAVERN, WorldMap.Kind.TRADE_POST, WorldMap.Kind.GUARD_POST, WorldMap.Kind.OUTLAW, WorldMap.Kind.ARMY, WorldMap.Kind.FLEET, WorldMap.Kind.SHRINE,
      WorldMap.Kind.FREE_VILLAGE
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
   /** Whether the key to the signs and the land is open; kept between openings. */
   private static boolean showKey = true;
   /** Rivers in the key, which are not a kind of land of a cell. */
   private static final byte KEY_RIVER = 7;
   /** The kinds of land in the key, in order. */
   private static final byte[] KEY_TERRAIN = {
      WorldMap.TERRAIN_WATER, WorldMap.TERRAIN_DEEP_WATER, KEY_RIVER, WorldMap.TERRAIN_LAND, WorldMap.TERRAIN_FOREST, WorldMap.TERRAIN_MOUNTAIN, WorldMap.TERRAIN_DESERT, WorldMap.TERRAIN_SNOW
   };
   private static final String[] TERRAIN_NAMES = {"plain", "water", "deep_water", "forest", "mountain", "desert", "snow"};
   private final WorldMap.Data data;
   /** The river mask, WorldMap.SIZE * WorldMap.RIVER_DETAIL across, or empty while surveying. */
   private final boolean[] rivers;
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
      this.rivers = WorldMap.rivers(data.rivers());
      this.viewX = Double.isNaN(savedX) ? 0.0 : savedX;
      this.viewZ = Double.isNaN(savedX) ? 0.0 : savedZ;
   }

   public static void open(WorldMap.Data data) {
      Minecraft.getInstance().setScreen(new WorldMapScreen(data));
   }

   @Override
   protected void init() {
      if (this.texture == null) {
         this.findCountryMiddles();
         this.texture = new DynamicTexture(this.data.terrain().length == 0 ? this.ownersImage() : this.terrainImage());
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
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.ancientwarfare.world_map.key"), button -> showKey = !showKey).bounds(MARGIN + 168, 2, 80, 18).build()
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

   /** While the terrain is still being surveyed: owners only, one pixel a cell. */
   private NativeImage ownersImage() {
      NativeImage image = new NativeImage(WorldMap.SIZE, WorldMap.SIZE, false);

      for (int row = 0; row < WorldMap.SIZE; row++) {
         for (int column = 0; column < WorldMap.SIZE; column++) {
            int owner = this.owner(row, column);
            int color = owner == 0 ? WILD_COLOR : factionColor(this.data.factions().get(owner));
            image.setPixelRGBA(column, row, abgr(this.ownerChanges(owner, row, column) ? shade(color, 0.6) : color));
         }
      }

      return image;
   }

   /**
    * The terrain at DETAIL pixels a cell. Sea and land are blended between cell middles and
    * roughened a little, so coasts run as shores rather than steps; the coast gets a dark line and
    * shallows a lighter blue. Land is its terrain's colour, tinted with the owner's, with a light
    * pattern for forest, mountains, desert and snow, and darker along a border with another owner.
    */
   private NativeImage terrainImage() {
      int size = WorldMap.SIZE * DETAIL;
      boolean[] water = new boolean[size * size];
      double[] wet = new double[size * size];
      double[] deep = new double[size * size];

      for (int py = 0; py < size; py++) {
         for (int px = 0; px < size; px++) {
            double u = (px + 0.5) / DETAIL - 0.5;
            double v = (py + 0.5) / DETAIL - 0.5;
            int c = Mth.floor(u);
            int r = Mth.floor(v);
            double fx = u - c;
            double fy = v - r;
            double w = this.blend(r, c, fx, fy, false) + (noise(px, py) - 0.5) * COAST_ROUGHNESS;
            wet[py * size + px] = w;
            deep[py * size + px] = this.blend(r, c, fx, fy, true);
            water[py * size + px] = w > 0.5;
         }
      }

      NativeImage image = new NativeImage(size, size, false);

      for (int py = 0; py < size; py++) {
         for (int px = 0; px < size; px++) {
            int index = py * size + px;
            int color;
            if (water[index]) {
               color = deep[index] > 0.5 ? TERRAIN_COLORS[WorldMap.TERRAIN_DEEP_WATER] : TERRAIN_COLORS[WorldMap.TERRAIN_WATER];
               if (wet[index] < 0.62) {
                  color = mix(SHALLOW_COLOR, color, 0.7);
               }
            } else {
               int[] cell = this.landCell(px, py);
               int terrain = this.terrain(cell[0], cell[1]);
               int owner = this.owner(cell[0], cell[1]);
               int base = pattern(TERRAIN_COLORS[terrain], terrain, px, py);
               color = owner == 0
                  ? mix(WILD_GREY, base, WILD_GREYING)
                  : mix(shade(factionColor(this.data.factions().get(owner)), 1.15), base, FACTION_TINT);
               if (this.river(px, py)) {
                  color = RIVER_COLOR;
               } else if (touchesWater(water, size, px, py)) {
                  color = COAST_COLOR;
               } else if (owner != 0 && this.borderAt(owner, px, py, water, size)) {
                  color = shade(color, 0.45);
               }
            }

            image.setPixelRGBA(px, py, abgr(color));
         }
      }

      return image;
   }

   /** How much water (or deep water) there is at a point between four cell middles, 0 to 1. */
   private double blend(int row, int column, double fx, double fy, boolean deepOnly) {
      double top = this.wetness(row, column, deepOnly) * (1.0 - fx) + this.wetness(row, column + 1, deepOnly) * fx;
      double bottom = this.wetness(row + 1, column, deepOnly) * (1.0 - fx) + this.wetness(row + 1, column + 1, deepOnly) * fx;
      return top * (1.0 - fy) + bottom * fy;
   }

   private double wetness(int row, int column, boolean deepOnly) {
      int terrain = this.terrain(Mth.clamp(row, 0, WorldMap.SIZE - 1), Mth.clamp(column, 0, WorldMap.SIZE - 1));
      return deepOnly ? (terrain == WorldMap.TERRAIN_DEEP_WATER ? 1.0 : 0.0) : (isWater(terrain) ? 1.0 : 0.0);
   }

   /** The cell a land pixel takes its terrain and owner from: its own, or the nearest dry one around it. */
   private int[] landCell(int px, int py) {
      int row = py / DETAIL;
      int column = px / DETAIL;
      if (!isWater(this.terrain(row, column))) {
         return new int[]{row, column};
      }

      int[] best = {row, column};
      double bestDistance = Double.MAX_VALUE;
      for (int dr = -1; dr <= 1; dr++) {
         for (int dc = -1; dc <= 1; dc++) {
            int r = row + dr;
            int c = column + dc;
            if (r < 0 || c < 0 || r >= WorldMap.SIZE || c >= WorldMap.SIZE || isWater(this.terrain(r, c))) {
               continue;
            }

            double dx = (c + 0.5) * DETAIL - px;
            double dy = (r + 0.5) * DETAIL - py;
            if (dx * dx + dy * dy < bestDistance) {
               bestDistance = dx * dx + dy * dy;
               best = new int[]{r, c};
            }
         }
      }

      return best;
   }

   /** Whether a river runs through a texture pixel; the river mask has the texture's own detail. */
   private boolean river(int px, int py) {
      int size = WorldMap.SIZE * WorldMap.RIVER_DETAIL;
      return this.rivers.length == size * size && px < size && py < size && this.rivers[py * size + px];
   }

   private static boolean touchesWater(boolean[] water, int size, int px, int py) {
      return px > 0 && water[py * size + px - 1]
         || px + 1 < size && water[py * size + px + 1]
         || py > 0 && water[(py - 1) * size + px]
         || py + 1 < size && water[(py + 1) * size + px];
   }

   /** Whether a pixel at the edge of its cell faces land of another owner. */
   private boolean borderAt(int owner, int px, int py, boolean[] water, int size) {
      for (int[] offset : new int[][]{{0, 1}, {1, 0}, {0, -1}, {-1, 0}}) {
         int x = px + offset[1];
         int y = py + offset[0];
         if (x < 0 || y < 0 || x >= size || y >= size || water[y * size + x]) {
            continue;
         }

         int[] cell = this.landCell(x, y);
         if (this.owner(cell[0], cell[1]) != owner) {
            return true;
         }
      }

      return false;
   }

   /** A light texture so the kinds of land read apart even under a country's colour. */
   private static int pattern(int color, int terrain, int px, int py) {
      double n = noise(px, py);
      return switch (terrain) {
         case WorldMap.TERRAIN_FOREST -> n < 0.3 ? shade(color, 0.78) : color;
         case WorldMap.TERRAIN_MOUNTAIN -> (px + py) % 4 == 0 ? shade(color, 0.75) : n < 0.2 ? shade(color, 1.15) : color;
         case WorldMap.TERRAIN_DESERT -> n < 0.12 ? shade(color, 0.9) : color;
         case WorldMap.TERRAIN_SNOW -> n < 0.15 ? shade(color, 0.93) : color;
         default -> n < 0.08 ? shade(color, 0.94) : color;
      };
   }

   /** A steady pseudo-random number in [0, 1) for a pixel. */
   private static double noise(int x, int y) {
      int h = x * 374761393 + y * 668265263;
      h = (h ^ h >>> 13) * 1274126177;
      return ((h ^ h >>> 16) & 0xFFFF) / 65536.0;
   }

   private int owner(int row, int column) {
      return this.data.owners()[row * WorldMap.SIZE + column] & 0xFF;
   }

   private boolean ownerChanges(int owner, int row, int column) {
      for (int[] offset : new int[][]{{0, 1}, {1, 0}, {0, -1}, {-1, 0}}) {
         int r = row + offset[0];
         int c = column + offset[1];
         if (r >= 0 && c >= 0 && r < WorldMap.SIZE && c < WorldMap.SIZE && this.owner(r, c) != owner) {
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
      int pixels = this.texture.getPixels() == null ? WorldMap.SIZE : this.texture.getPixels().getWidth();
      graphics.blit(TEXTURE, mapX, mapZ, (int)Math.round(size), (int)Math.round(size), 0.0F, 0.0F, pixels, pixels, pixels, pixels);

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
         // An arrowhead the way the player faces, white on a black rim, above the signs and names.
         graphics.pose().pushPose();
         graphics.pose().translate(0.0F, 0.0F, 150.0F);
         this.drawPlayerArrow(graphics, px, pz, Math.toRadians(minecraft.player.getYRot()));
         graphics.pose().popPose();
      }

      this.renderScaleBar(graphics, right - SCALE_BAR_WIDTH - 8, bottom - 14);
      this.renderNorth(graphics, right - 12, top + 6);
      if (showKey) {
         // Above the names on the map, which are drawn a little in front of the land.
         graphics.pose().pushPose();
         graphics.pose().translate(0.0F, 0.0F, 200.0F);
         this.renderKey(graphics, left + 4, top + 4, bottom - 4);
         graphics.pose().popPose();
      }
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

   /** How the settlement lives: its people, stores, health, order and loyalty, and any trouble. */
   private static void addLife(List<Component> lines, int[] life) {
      if (life.length <= WorldMap.LIFE_FLAGS) {
         return;
      }

      int flags = life[WorldMap.LIFE_FLAGS];
      if (life.length > WorldMap.LIFE_RANK && life[WorldMap.LIFE_RANK] >= 0) {
         Component rank = net.shadowmage.ancientwarfare.core.faction.FactionPorts.rankName(life[WorldMap.LIFE_RANK]);
         lines.add(((flags & WorldMap.FLAG_PORT) != 0 ? Component.translatable("gui.ancientwarfare.world_map.rank_port", rank) : rank.copy()).withStyle(net.minecraft.ChatFormatting.AQUA));
      }

      lines.add(Component.translatable("gui.ancientwarfare.world_map.people", life[WorldMap.LIFE_PEOPLE], life[WorldMap.LIFE_GARRISON]));
      if (life[WorldMap.LIFE_FOOD_DAYS] >= 0) {
         lines.add(Component.translatable("gui.ancientwarfare.world_map.stores", life[WorldMap.LIFE_FOOD_DAYS], life[WorldMap.LIFE_HEALTH]));
      }

      lines.add(Component.translatable("gui.ancientwarfare.world_map.mood", life[WorldMap.LIFE_ORDER], life[WorldMap.LIFE_LOYALTY]));
      if ((flags & WorldMap.FLAG_HUNGRY) != 0) {
         lines.add(Component.translatable("gui.ancientwarfare.world_map.hungry").withStyle(net.minecraft.ChatFormatting.RED));
      }

      if ((flags & WorldMap.FLAG_FAMINE) != 0) {
         lines.add(Component.translatable("gui.ancientwarfare.world_map.famine").withStyle(net.minecraft.ChatFormatting.GOLD));
      }

      if ((flags & WorldMap.FLAG_PLAGUE) != 0) {
         lines.add(Component.translatable("gui.ancientwarfare.world_map.plague").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
      }

      if ((flags & WorldMap.FLAG_TAKEN) != 0 && life[WorldMap.LIFE_LOYALTY] < 40) {
         lines.add(Component.translatable("gui.ancientwarfare.world_map.unrest").withStyle(net.minecraft.ChatFormatting.YELLOW));
      }

      addPrices(lines, life);
   }

   /** What is dear and what is cheap at the town's market. */
   private static void addPrices(List<Component> lines, int[] life) {
      int kinds = net.shadowmage.ancientwarfare.core.faction.FactionResources.KINDS;
      if (life.length < WorldMap.LIFE_PRICES + kinds) {
         return;
      }

      net.minecraft.network.chat.MutableComponent dear = Component.empty();
      net.minecraft.network.chat.MutableComponent cheap = Component.empty();
      boolean anyDear = false;
      boolean anyCheap = false;
      for (int kind = 0; kind < kinds; kind++) {
         double price = life[WorldMap.LIFE_PRICES + kind] / 100.0;
         Component name = net.shadowmage.ancientwarfare.core.faction.TownMarkets.goodName(kind);
         if (price >= net.shadowmage.ancientwarfare.core.faction.TownMarkets.DEAR) {
            dear.append(anyDear ? ", " : "").append(name);
            anyDear = true;
         } else if (price <= net.shadowmage.ancientwarfare.core.faction.TownMarkets.CHEAP) {
            cheap.append(anyCheap ? ", " : "").append(name);
            anyCheap = true;
         }
      }

      if (anyDear) {
         lines.add(Component.translatable("market.ancientwarfare.dear", dear).withStyle(net.minecraft.ChatFormatting.RED));
      }

      if (anyCheap) {
         lines.add(Component.translatable("market.ancientwarfare.cheap", cheap).withStyle(net.minecraft.ChatFormatting.GREEN));
      }
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
            lines.add(WorldMap.ownerName(faction));
         }

         addLife(lines, hovered.life());
         if (hovered.label().isEmpty()) {
            // A holy site says all it has to say by its name.
         } else if (hovered.life().length == 0 || hovered.kind() == WorldMap.Kind.PLAYER_VILLAGE) {
            lines.add(Component.literal(hovered.label()));
         } else if (this.data.canTeleport()) {
            // The settlement's number and template, for the operator.
            lines.add(Component.literal(hovered.label()).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
         }

         x = hovered.x();
         z = hovered.z();
      } else {
         String owner = this.ownerAt(x, z);
         lines.add(owner.isEmpty() ? Component.translatable("gui.ancientwarfare.world_map.wild") : WorldMap.ownerName(owner));
         int terrain = this.terrainAt(x, z);
         int riverX = Math.floorDiv(x - this.data.originX(), WorldMap.STEP / WorldMap.RIVER_DETAIL);
         int riverZ = Math.floorDiv(z - this.data.originZ(), WorldMap.STEP / WorldMap.RIVER_DETAIL);
         if (riverX >= 0 && riverZ >= 0 && this.river(riverX, riverZ) && terrain >= 0 && !isWater(terrain)) {
            lines.add(Component.translatable("gui.ancientwarfare.world_map.terrain.river").withStyle(net.minecraft.ChatFormatting.GRAY));
         } else if (terrain >= 0) {
            lines.add(Component.translatable("gui.ancientwarfare.world_map.terrain." + TERRAIN_NAMES[terrain]).withStyle(net.minecraft.ChatFormatting.GRAY));
         }
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
         Component name = WorldMap.ownerName(faction);
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
         case FLEET -> {
            // A boat: a hull in the faction's colour under a white sail, outlined in red.
            graphics.fill(x - 6, y, x + 7, y + 6, 0xFF000000);
            graphics.fill(x - 5, y + 1, x + 6, y + 5, 0xFFE03030);
            graphics.fill(x - 4, y + 2, x + 5, y + 4, color);
            graphics.fill(x, y - 6, x + 1, y + 2, 0xFF000000);
            graphics.fill(x + 1, y - 6, x + 6, y, 0xFF000000);
            graphics.fill(x + 1, y - 5, x + 5, y - 1, 0xFFF0F0F0);
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
         case SHRINE -> {
            // A golden star: a cross with a heart in the holder's colour.
            graphics.fill(x - 1, y - 5, x + 2, y + 6, outline);
            graphics.fill(x - 5, y - 1, x + 6, y + 2, outline);
            graphics.fill(x, y - 4, x + 1, y + 5, 0xFFF0C040);
            graphics.fill(x - 4, y, x + 5, y + 1, 0xFFF0C040);
            graphics.fill(x - 2, y - 2, x + 3, y + 3, 0xFFF0C040);
            graphics.fill(x - 1, y - 1, x + 2, y + 2, color);
         }
         case HOMESTEAD -> {
            graphics.fill(x - 2, y - 2, x + 3, y + 3, outline);
            graphics.fill(x - 1, y - 1, x + 2, y + 2, color);
         }
         case PLAYER_VILLAGE -> {
            graphics.fill(x - 4, y - 4, x + 5, y + 5, outline);
            graphics.fill(x - 3, y - 3, x + 4, y + 4, 0xFF40A0FF);
         }
         case FREE_VILLAGE -> {
            // A plain village square in green, belonging to nobody.
            graphics.fill(x - 3, y - 3, x + 4, y + 4, outline);
            graphics.fill(x - 2, y - 2, x + 3, y + 3, 0xFF60D060);
            graphics.fill(x, y, x + 1, y + 1, 0xFFFFFFFF);
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
   /** A broad arrowhead with a notched tail, pointing along the yaw, drawn a pixel at a time. */
   private void drawPlayerArrow(GuiGraphics graphics, int x, int y, double yaw) {
      double forwardX = -Math.sin(yaw);
      double forwardY = Math.cos(yaw);
      // The tip, the two barbs and the notch, in the arrow's own frame (ahead, aside).
      double[][] shape = {{11.0, 0.0}, {-8.0, 8.0}, {-3.0, 0.0}, {-8.0, -8.0}};
      double[][] rim = new double[4][];
      double[][] body = new double[4][];
      for (int i = 0; i < 4; i++) {
         rim[i] = place(x, y, forwardX, forwardY, shape[i][0] * 1.25 + 0.5, shape[i][1] * 1.25);
         body[i] = place(x, y, forwardX, forwardY, shape[i][0], shape[i][1]);
      }

      for (int dy = -15; dy <= 15; dy++) {
         for (int dx = -15; dx <= 15; dx++) {
            double cx = x + dx + 0.5;
            double cy = y + dy + 0.5;
            if (inside(body, cx, cy)) {
               graphics.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, 0xFFFFFFFF);
            } else if (inside(rim, cx, cy)) {
               graphics.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, 0xFF000000);
            }
         }
      }

      graphics.fill(x - 1, y - 1, x + 1, y + 1, 0xFFD02020);
   }

   private static double[] place(int x, int y, double forwardX, double forwardY, double ahead, double aside) {
      return new double[]{x + forwardX * ahead - forwardY * aside, y + forwardY * ahead + forwardX * aside};
   }

   /** Whether a point lies inside a polygon, by counting the edges a ray from it crosses. */
   private static boolean inside(double[][] polygon, double x, double y) {
      boolean in = false;
      for (int i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
         double[] a = polygon[i];
         double[] b = polygon[j];
         if ((a[1] > y) != (b[1] > y) && x < (b[0] - a[0]) * (y - a[1]) / (b[1] - a[1]) + a[0]) {
            in = !in;
         }
      }

      return in;
   }

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

         Component name = WorldMap.ownerName(this.data.factions().get(i));
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

   /** The kind of land at a world point, or -1 off the map or while it is being surveyed. */
   private int terrainAt(int x, int z) {
      int column = Math.floorDiv(x - this.data.originX(), WorldMap.STEP);
      int row = Math.floorDiv(z - this.data.originZ(), WorldMap.STEP);
      if (this.data.terrain().length == 0 || column < 0 || row < 0 || column >= WorldMap.SIZE || row >= WorldMap.SIZE) {
         return -1;
      }

      return this.terrain(row, column);
   }

   /** The key: the kinds of land, then the signs, in columns down the map's left edge. */
   private void renderKey(GuiGraphics graphics, int x, int top, int bottom) {
      // Each entry is a land colour (Integer) or a sign (WorldMap.Kind).
      List<Object> entries = new ArrayList<>();
      List<Component> names = new ArrayList<>();
      if (this.data.terrain().length > 0) {
         for (byte kind : KEY_TERRAIN) {
            entries.add(kind == KEY_RIVER ? RIVER_COLOR : pattern(TERRAIN_COLORS[kind], kind, 1, 1));
            names.add(Component.translatable("gui.ancientwarfare.world_map.terrain." + (kind == KEY_RIVER ? "river" : TERRAIN_NAMES[kind])));
         }
      }

      for (WorldMap.Kind kind : LEGEND_KINDS) {
         entries.add(kind);
         names.add(Component.translatable("gui.ancientwarfare.world_map.kind." + kind.name().toLowerCase(java.util.Locale.ROOT)));
      }

      int lineHeight = 13;
      int rows = Math.max(1, (bottom - top - 6) / lineHeight);
      int columns = (names.size() + rows - 1) / rows;
      rows = (names.size() + columns - 1) / columns;
      int columnWidth = 0;
      for (Component name : names) {
         columnWidth = Math.max(columnWidth, 17 + this.font.width(name) + 8);
      }

      graphics.fill(x, top, x + columnWidth * columns + 2, top + rows * lineHeight + 6, 0xEE181410);
      for (int i = 0; i < names.size(); i++) {
         int cx = x + 4 + (i / rows) * columnWidth;
         int cy = top + 4 + (i % rows) * lineHeight;
         if (entries.get(i) instanceof Integer color) {
            graphics.fill(cx + 1, cy, cx + 12, cy + 10, 0xFF000000);
            graphics.fill(cx + 2, cy + 1, cx + 11, cy + 9, color);
         } else if (entries.get(i) instanceof WorldMap.Kind kind) {
            this.drawIcon(graphics, kind, cx + 6, cy + 5, 0xFFD8D8D8, 0xFF000000);
         }

         graphics.drawString(this.font, names.get(i), cx + 17, cy + 1, 0xFFE0E0E0);
      }
   }

   /** A small north arrow in the map's corner. */
   private void renderNorth(GuiGraphics graphics, int x, int y) {
      graphics.fill(x - 6, y - 4, x + 7, y + 20, 0x90000000);
      for (int i = 0; i < 5; i++) {
         graphics.fill(x - i, y + i, x + i + 1, y + i + 1, 0xFFFFFFFF);
      }

      graphics.fill(x, y + 5, x + 1, y + 9, 0xFFFFFFFF);
      graphics.drawCenteredString(this.font, Component.translatable("gui.ancientwarfare.world_map.north"), x + 1, y + 10, 0xFFFFFFFF);
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

   /** A steady colour per faction, the same in every world. */
   public static int factionColor(String faction) {
      return FactionColors.color(faction);
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
