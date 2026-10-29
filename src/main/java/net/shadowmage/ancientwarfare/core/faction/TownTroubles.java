package net.shadowmage.ancientwarfare.core.faction;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Fires, floods and riots. A fire takes one of a town's buildings, which goes back to the head of
 * its plan and is raised again; a player nearby sees the roofs burn. Low towns by the water flood
 * in spring and lose their stores. A town whose order has fallen too far riots: its treasury is
 * plundered and something is burned, and afterwards it is quieter for a while.
 */
public final class TownTroubles {
   private static final double FIRE_CHANCE = 0.006;
   private static final double FLOOD_CHANCE = 0.03;
   private static final double RIOT_CHANCE = 0.15;
   private static final int RIOT_ORDER = 25;
   private static final int SEEN = 96;

   private TownTroubles() {
   }

   static void simulateDay(ServerLevel level, FactionWorldData data) {
      RandomSource random = level.getRandom();
      FactionSeasons.Season season = FactionSeasons.season(level);
      for (FactionWorldData.Settlement town : data.settlements()) {
         if (!town.built() || FactionDiplomacy.isOutlaw(town.faction()) || town.buildings().isEmpty()) {
            continue;
         }

         double fire = FIRE_CHANCE * (season == FactionSeasons.Season.SUMMER ? 2.0 : 1.0) / (town.buildings().stream().anyMatch(b -> b.contains("well")) ? 2.0 : 1.0);
         if (random.nextDouble() < fire) {
            String lost = burn(level, town, random);
            news(level, town, Component.translatable("trouble.ancientwarfare.fire", FactionNews.named(town)));
            TownChronicle.write(level, town.id(), "fire", "#building.ancientwarfare." + lost);
         } else if ((season == FactionSeasons.Season.SPRING || season == FactionSeasons.Season.AUTUMN) && town.center().getY() <= level.getSeaLevel() + 2 && random.nextDouble() < FLOOD_CHANCE) {
            town.food = Math.max(0, town.food - town.population() * 3);
            town.order = Math.max(0, town.order - 5);
            news(level, town, Component.translatable("trouble.ancientwarfare.flood", FactionNews.named(town)));
            TownChronicle.write(level, town.id(), "flood");
         } else if (town.order < RIOT_ORDER && random.nextDouble() < RIOT_CHANCE) {
            int plundered = town.wealth / 4;
            town.wealth -= plundered;
            town.garrison = Math.max(0, town.garrison - 1);
            burn(level, town, random);
            // The anger is spent for now.
            town.order = Math.min(100, town.order + 10);
            news(level, town, Component.translatable("trouble.ancientwarfare.riot", FactionNews.named(town), plundered));
            TownChronicle.write(level, town.id(), "riot", plundered);
         }
      }
   }

   /** One building burns: off the list of what stands, back to the front of the plan. */
   private static String burn(ServerLevel level, FactionWorldData.Settlement town, RandomSource random) {
      String lost = town.buildings().remove(random.nextInt(town.buildings().size()));
      town.plan().add(0, lost);
      if (!town.halls().isEmpty() && !level.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class, new net.minecraft.world.phys.AABB(town.center()).inflate(SEEN)).isEmpty()) {
         FactionWorldData.Hall hall = town.halls().get(random.nextInt(town.halls().size()));
         for (int i = 0; i < 8; i++) {
            int x = hall.min().getX() + random.nextInt(hall.max().getX() - hall.min().getX() + 1);
            int z = hall.min().getZ() + random.nextInt(hall.max().getZ() - hall.min().getZ() + 1);
            BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
            if (level.getBlockState(top).isAir() && level.getBlockState(top.below()).isFlammable(level, top.below(), net.minecraft.core.Direction.UP)) {
               level.setBlockAndUpdate(top, Blocks.FIRE.defaultBlockState());
            }
         }
      }

      return lost;
   }

   private static void news(ServerLevel level, FactionWorldData.Settlement town, Component message) {
      net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData standings = net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData.get(level);
      for (net.minecraft.server.level.ServerPlayer player : level.players()) {
         if (standings.knownFactions(player.getUUID()).contains(town.faction())) {
            player.sendSystemMessage(message.copy().withStyle(ChatFormatting.RED));
         }
      }
   }
}
