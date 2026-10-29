package net.shadowmage.ancientwarfare.core.faction;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;

/**
 * The towns' year. Prices follow the harvest and the wars: grain is cheap after the autumn
 * harvest and dear in winter and famine, iron and leather dear while the realm is at war. Once a
 * season the bigger towns hold a fair, with crowds and cheap goods. A town with a tavern or a manor
 * now and then holds a feast, and whoever comes eats well and is the better liked. Capitals and
 * towns with an arena hold a tournament on a fixed day of the season: a herald stands in the
 * square, and a player may step into the lists himself or send a soldier against the champion.
 */
public final class TownCalendar {
   public static final String HERALD_TAG = "AWHerald";
   private static final int FAIR_DAY = 2;
   private static final int TOURNAMENT_DAY = 4;
   private static final int FAIR_POPULATION = 30;
   private static final double FEAST_CHANCE = 0.03;
   private static final long CHECK_TICKS = 100L;
   private static final int NEAR = 48;

   private TownCalendar() {
   }

   private static boolean has(FactionWorldData.Settlement town, String word) {
      return town.buildings().stream().anyMatch(building -> building.contains(word));
   }

   private static boolean jousts(FactionWorldData.Settlement town) {
      return net.shadowmage.ancientwarfare.core.structure.FactionTowns.CAPITAL.equals(town.kind()) || has(town, "arena");
   }

   static void simulateDay(ServerLevel level, FactionWorldData data) {
      RandomSource random = level.getRandom();
      long day = Math.max(0L, data.lastSimulatedDay());
      int dayOfSeason = (int)Math.floorMod(day, FactionSeasons.SEASON_DAYS);
      FactionSeasons.Season season = FactionSeasons.season(level);
      TownMarkets markets = TownMarkets.get(level);
      for (FactionWorldData.Settlement town : data.settlements()) {
         if (!FactionTownLife.isTown(town)) {
            continue;
         }

         // The harvest and the wars move prices: goods come into the market or are drawn out of it.
         if (season == FactionSeasons.Season.AUTUMN) {
            markets.change(town.id(), FactionResources.GRAIN, 20.0);
         } else if (season == FactionSeasons.Season.WINTER || town.famine() > 0) {
            markets.change(town.id(), FactionResources.GRAIN, -20.0);
         }

         if (atWar(level, data, town.faction())) {
            markets.change(town.id(), FactionResources.IRON, -10.0);
            markets.change(town.id(), FactionResources.LEATHER, -10.0);
         }

         if (dayOfSeason == FAIR_DAY && town.population() >= FAIR_POPULATION) {
            FactionTownLife.holdFestival(level, town.id(), "fair");
            markets.change(town.id(), FactionResources.GRAIN, 30.0);
            news(level, town, "fair");
         } else if (dayOfSeason == TOURNAMENT_DAY && jousts(town)) {
            FactionTownLife.holdFestival(level, town.id(), "tournament");
            news(level, town, "tournament");
         } else if ((has(town, "tavern") || has(town, "manor")) && town.food() > (town.population() + town.garrison()) * 4 && random.nextDouble() < FEAST_CHANCE) {
            FactionTownLife.holdFestival(level, town.id(), "feast");
            town.food -= town.population();
            town.order = Math.min(100, town.order + 4);
            news(level, town, "feast");
         }
      }
   }

   private static boolean atWar(ServerLevel level, FactionWorldData data, String faction) {
      return data.settlements().stream().map(FactionWorldData.Settlement::faction).distinct()
         .anyMatch(other -> !other.equals(faction) && FactionDiplomacy.relation(level, faction, other).state() == RelationState.WAR);
   }

   /** Players who know the town's realm hear of its fair, tournament or feast. */
   private static void news(ServerLevel level, FactionWorldData.Settlement town, String what) {
      TownChronicle.write(level, town.id(), what);
      FactionStandingData standings = FactionStandingData.get(level);
      for (ServerPlayer player : level.players()) {
         if (standings.knownFactions(player.getUUID()).contains(town.faction()) && !standings.isHostile(player.getUUID(), town.faction())) {
            BlockPos at = town.center();
            player.sendSystemMessage(Component.translatable("calendar.ancientwarfare." + what, FactionNews.named(town), at.getX(), at.getZ()).withStyle(ChatFormatting.GOLD));
         }
      }
   }

   /** A herald at a tournament, guests at a feast. */
   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % CHECK_TICKS != 0L || level.players().isEmpty()) {
         return;
      }

      for (FactionWorldData.Settlement town : data.settlements()) {
         String reason = FactionTownLife.festivalReason(level, town.id());
         if (reason == null) {
            continue;
         }

         BlockPos center = town.center();
         List<ServerPlayer> near = level.getEntitiesOfClass(ServerPlayer.class, new net.minecraft.world.phys.AABB(center).inflate(NEAR));
         if (near.isEmpty()) {
            continue;
         }

         if ("tournament".equals(reason)) {
            herald(level, town);
         } else if ("feast".equals(reason)) {
            for (ServerPlayer player : near) {
               if (!player.hasEffect(MobEffects.SATURATION) && !FactionStandingData.get(level).isHostile(player.getUUID(), town.faction())
                  && !player.getPersistentData().getBoolean("AWFeast" + town.id() + "_" + level.getDayTime() / FactionSimulation.DAY_TICKS)) {
                  player.getPersistentData().putBoolean("AWFeast" + town.id() + "_" + level.getDayTime() / FactionSimulation.DAY_TICKS, true);
                  player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 100, 1));
                  player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0));
                  FactionStandingData.get(level).adjustStanding(player.getUUID(), town.faction(), 2);
                  player.sendSystemMessage(Component.translatable("calendar.ancientwarfare.feast_guest", FactionNews.named(town)).withStyle(ChatFormatting.GREEN));
               }
            }
         }
      }

      // Heralds go home when the tournament is over.
      for (ServerPlayer player : level.players()) {
         for (FactionNpcEntity herald : level.getEntitiesOfClass(FactionNpcEntity.class, player.getBoundingBox().inflate(96), npc -> npc.getTags().contains(HERALD_TAG))) {
            FactionWorldData.Settlement town = FactionSimulation.settlementAt(data, herald.getFactionName(), herald.getX(), herald.getZ(), NEAR);
            if (town == null || !"tournament".equals(FactionTownLife.festivalReason(level, town.id()))) {
               herald.discard();
            }
         }
      }
   }

   private static void herald(ServerLevel level, FactionWorldData.Settlement town) {
      BlockPos center = town.center();
      if (!level.getEntitiesOfClass(FactionNpcEntity.class, new net.minecraft.world.phys.AABB(center).inflate(NEAR), npc -> npc.getTags().contains(HERALD_TAG)).isEmpty()) {
         return;
      }

      FactionNpcEntity herald = AWCoreEntityTypes.FACTION_CIVILIAN_MALE_NPC.get().create(level);
      if (herald == null) {
         return;
      }

      int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX(), center.getZ());
      herald.moveTo(center.getX() + 0.5, y, center.getZ() + 0.5, 0.0F, 0.0F);
      herald.finalizeSpawn(level, level.getCurrentDifficultyAt(center), MobSpawnType.EVENT, null, null);
      herald.setFactionName(town.faction());
      herald.addTag(HERALD_TAG);
      herald.setCustomName(Component.translatable("calendar.ancientwarfare.herald"));
      herald.setPersistenceRequired();
      level.addFreshEntity(herald);
   }

   private static MutableComponent button(String key, String command, ChatFormatting color) {
      Component label = Component.translatable(key);
      return Component.literal("[").append(label).append("]")
         .withStyle(style -> style.withColor(color).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, label)));
   }

   /** The herald calls for challengers. */
   public static void talk(ServerPlayer player, FactionNpcEntity herald) {
      FactionRumours.say(player, herald.getDisplayName(), Component.translatable("calendar.ancientwarfare.herald.call", FactionNews.factionName(herald.getFactionName())));
      String id = "/awtownsfolk " + herald.getId() + " ";
      player.sendSystemMessage(Component.literal("  ")
         .append(button("calendar.ancientwarfare.herald.self", id + "joust_self", ChatFormatting.GOLD))
         .append("  ")
         .append(button("calendar.ancientwarfare.herald.soldier", id + "joust_soldier", ChatFormatting.YELLOW)));
   }

   /** A click under the herald's call. Returns false when the action is not his. */
   public static boolean act(ServerPlayer player, FactionNpcEntity npc, String action) {
      if (!npc.getTags().contains(HERALD_TAG) || !action.startsWith("joust_")) {
         return false;
      }

      if (FactionStandingData.get(player.serverLevel()).isHostile(player.getUUID(), npc.getFactionName())) {
         return true;
      }

      player.sendSystemMessage(FactionTournaments.challenge(player, npc.getFactionName(), npc, action.equals("joust_self")));
      return true;
   }
}
