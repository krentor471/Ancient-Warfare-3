package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.CoinMetal;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.item.CoinItem;

/**
 * What people say when a player stops to talk: the news of the land around, read from the world
 * as it is. An army on the march, a bandit lair, a war, a hungry or sick town, a settlement that
 * changed hands, a town over the hills, and now and then buried treasure that is really there.
 * Townsfolk, travellers on the roads and tavern keepers all draw on it.
 */
public final class FactionRumours {
   private static final int MARCH_RANGE = 2500;
   private static final int BANDIT_RANGE = 1500;
   private static final int HARDSHIP_RANGE = 2500;
   private static final int CAPTURED_RANGE = 2000;
   private static final int PLACE_MIN = 300;
   private static final int PLACE_RANGE = 1500;
   private static final double TREASURE_CHANCE = 0.08;
   private static final int TREASURE_MIN = 200;
   private static final int TREASURE_MAX = 600;
   private static final int MAX_TREASURES = 16;
   /** One treasure a player may hear of per this many ticks. */
   private static final long TREASURE_COOLDOWN = FactionSimulation.DAY_TICKS;
   private static final double TREASURE_BURY_RANGE = 64.0;
   private static final int TREASURE_DEPTH = 2;
   /** The same person does not repeat themselves to a player this soon. */
   private static final long TALK_COOLDOWN = 100L;
   private static final Map<UUID, Long> LAST_TREASURE = new HashMap<>();
   private static final Map<UUID, Long> LAST_TALK = new HashMap<>();

   private FactionRumours() {
   }

   /** A townsperson the player right-clicked: a greeting and the news, or a cold shoulder from an enemy. */
   public static void chat(ServerPlayer player, FactionNpcEntity npc) {
      ServerLevel level = player.serverLevel();
      Long last = LAST_TALK.get(npc.getUUID());
      if (last != null && level.getGameTime() - last < TALK_COOLDOWN) {
         return;
      }

      LAST_TALK.put(npc.getUUID(), level.getGameTime());
      if (LAST_TALK.size() > 512) {
         LAST_TALK.clear();
      }

      String faction = npc.getFactionName();
      Component speaker = npc.getDisplayName();
      if (FactionStandingData.get(level).isHostile(player.getUUID(), faction)) {
         say(player, speaker, Component.translatable("rumour.ancientwarfare.hostile"));
         return;
      }

      say(player, speaker, rumour(level, player, npc.getX(), npc.getZ(), npc.getRandom()));
   }

   static void say(ServerPlayer player, Component speaker, Component line) {
      player.sendSystemMessage(Component.literal("<").append(speaker).append("> ").append(line.copy().withStyle(ChatFormatting.GRAY)));
   }

   /** One piece of news about the land around x, z, or small talk when there is none. */
   public static Component rumour(ServerLevel level, ServerPlayer player, double x, double z, RandomSource random) {
      FactionWorldData data = FactionWorldData.get(level);
      List<Supplier<Component>> kinds = new ArrayList<>(List.of(
         () -> march(data, x, z, random),
         () -> bandits(data, x, z),
         () -> war(level, data, x, z, random),
         () -> hardship(data, x, z, random),
         () -> captured(data, x, z, random),
         () -> place(data, x, z, random)
      ));
      if (random.nextDouble() < TREASURE_CHANCE) {
         Component treasure = treasure(level, data, player, x, z, random);
         if (treasure != null) {
            return treasure;
         }
      }

      java.util.Collections.shuffle(kinds, new java.util.Random(random.nextLong()));
      for (Supplier<Component> kind : kinds) {
         Component news = kind.get();
         if (news != null) {
            return news;
         }
      }

      return Component.translatable("rumour.ancientwarfare.small_talk." + random.nextInt(6));
   }

   private static Component march(FactionWorldData data, double x, double z, RandomSource random) {
      List<FactionWorldData.Campaign> near = new ArrayList<>();
      for (FactionWorldData.Campaign campaign : data.campaigns()) {
         if (!campaign.relief() && campaign.hunt() == 0 && distanceSq(campaign.x(), campaign.z(), x, z) < (double)MARCH_RANGE * MARCH_RANGE) {
            near.add(campaign);
         }
      }

      if (near.isEmpty()) {
         return null;
      }

      FactionWorldData.Campaign campaign = near.get(random.nextInt(near.size()));
      FactionWorldData.Settlement target = settlement(data, campaign.targetSettlement());
      return Component.translatable(
         "rumour.ancientwarfare.march",
         FactionNews.factionName(campaign.faction()),
         campaign.size(),
         FactionNews.factionName(campaign.defender()),
         FactionNews.named(target),
         where(x, z, campaign.x(), campaign.z())
      );
   }

   private static Component bandits(FactionWorldData data, double x, double z) {
      FactionWorldData.Settlement lair = nearest(data, x, z, BANDIT_RANGE, settlement -> FactionDiplomacy.isOutlaw(settlement.faction()) && settlement.built());
      return lair == null ? null : Component.translatable("rumour.ancientwarfare.bandits", where(x, z, lair.center().getX(), lair.center().getZ()));
   }

   private static Component war(ServerLevel level, FactionWorldData data, double x, double z, RandomSource random) {
      List<String> around = new ArrayList<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         String faction = settlement.faction();
         if (!FactionDiplomacy.isOutlaw(faction) && !around.contains(faction) && settlement.horizontalDistanceSquared(x, z) < (double)PLACE_RANGE * PLACE_RANGE) {
            around.add(faction);
         }
      }

      List<String[]> wars = new ArrayList<>();
      for (String faction : around) {
         for (String other : net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry.factionNames()) {
            if (faction.compareTo(other) < 0 && !FactionDiplomacy.isOutlaw(other) && FactionDiplomacy.relation(level, faction, other).state() == RelationState.WAR) {
               wars.add(new String[]{faction, other});
            }
         }
      }

      if (wars.isEmpty()) {
         return null;
      }

      String[] war = wars.get(random.nextInt(wars.size()));
      return Component.translatable("rumour.ancientwarfare.war", FactionNews.factionName(war[0]), FactionNews.factionName(war[1]));
   }

   private static Component hardship(FactionWorldData data, double x, double z, RandomSource random) {
      List<FactionWorldData.Settlement> near = new ArrayList<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if ((settlement.famine() > 0 || settlement.plague() > 0) && settlement.horizontalDistanceSquared(x, z) < (double)HARDSHIP_RANGE * HARDSHIP_RANGE) {
            near.add(settlement);
         }
      }

      if (near.isEmpty()) {
         return null;
      }

      FactionWorldData.Settlement settlement = near.get(random.nextInt(near.size()));
      return Component.translatable(
         settlement.plague() > 0 ? "rumour.ancientwarfare.plague" : "rumour.ancientwarfare.famine",
         FactionNews.named(settlement),
         FactionNews.factionName(settlement.faction()),
         where(x, z, settlement.center().getX(), settlement.center().getZ())
      );
   }

   private static Component captured(FactionWorldData data, double x, double z, RandomSource random) {
      List<FactionWorldData.Settlement> near = new ArrayList<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!settlement.capturedFrom().isEmpty() && settlement.built() && settlement.horizontalDistanceSquared(x, z) < (double)CAPTURED_RANGE * CAPTURED_RANGE) {
            near.add(settlement);
         }
      }

      if (near.isEmpty()) {
         return null;
      }

      FactionWorldData.Settlement settlement = near.get(random.nextInt(near.size()));
      return Component.translatable(
         "rumour.ancientwarfare.captured",
         FactionNews.named(settlement),
         FactionNews.factionName(settlement.capturedFrom()),
         FactionNews.factionName(settlement.faction()),
         where(x, z, settlement.center().getX(), settlement.center().getZ())
      );
   }

   private static Component place(FactionWorldData data, double x, double z, RandomSource random) {
      List<FactionWorldData.Settlement> near = new ArrayList<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         double distance = settlement.horizontalDistanceSquared(x, z);
         if (settlement.built()
            && !settlement.name().isEmpty()
            && !FactionDiplomacy.isOutlaw(settlement.faction())
            && distance > (double)PLACE_MIN * PLACE_MIN
            && distance < (double)PLACE_RANGE * PLACE_RANGE) {
            near.add(settlement);
         }
      }

      if (near.isEmpty()) {
         return null;
      }

      FactionWorldData.Settlement settlement = near.get(random.nextInt(near.size()));
      return Component.translatable(
         "rumour.ancientwarfare.place",
         FactionNews.named(settlement),
         FactionNews.factionName(settlement.faction()),
         where(x, z, settlement.center().getX(), settlement.center().getZ())
      );
   }

   /** A treasure buried somewhere around: put in the ground once a player comes near it. */
   private static Component treasure(ServerLevel level, FactionWorldData data, ServerPlayer player, double x, double z, RandomSource random) {
      Long last = player == null ? null : LAST_TREASURE.get(player.getUUID());
      if (player == null || last != null && level.getGameTime() - last < TREASURE_COOLDOWN || data.treasures().size() >= MAX_TREASURES) {
         return null;
      }

      double angle = random.nextDouble() * Math.PI * 2.0;
      int distance = TREASURE_MIN + random.nextInt(TREASURE_MAX - TREASURE_MIN);
      int tx = (int)Math.round(x + Math.cos(angle) * distance);
      int tz = (int)Math.round(z + Math.sin(angle) * distance);
      LAST_TREASURE.put(player.getUUID(), level.getGameTime());
      data.treasures().add(FactionRoads.pack(tx, tz));
      data.setDirty();
      AncientWarfareCore.LOG.info("Rumour of treasure told to {}: {}, {}", player.getScoreboardName(), tx, tz);
      return Component.translatable("rumour.ancientwarfare.treasure", tx, tz);
   }

   /** Treasures a player has come near are buried for real, a chest under the ground at the very spot. */
   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % 100L != 0L || data.treasures().isEmpty()) {
         return;
      }

      Iterator<Long> iterator = data.treasures().iterator();
      while (iterator.hasNext()) {
         long point = iterator.next();
         int x = FactionRoads.unpackX(point);
         int z = FactionRoads.unpackZ(point);
         boolean near = level.players().stream().anyMatch(player -> player.distanceToSqr(x, player.getY(), z) < TREASURE_BURY_RANGE * TREASURE_BURY_RANGE);
         if (!near || !level.hasChunk(x >> 4, z >> 4)) {
            continue;
         }

         bury(level, x, z);
         iterator.remove();
         data.setDirty();
      }
   }

   private static void bury(ServerLevel level, int x, int z) {
      int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1;
      BlockPos pos = new BlockPos(x, ground - TREASURE_DEPTH, z);
      level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 2);
      if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
         RandomSource random = level.getRandom();
         chest.setItem(0, CoinItem.create(CoinMetal.SILVER, 8 + random.nextInt(24)));
         chest.setItem(1, CoinItem.create(CoinMetal.GOLD, 1 + random.nextInt(3)));
         chest.setItem(2, new ItemStack(random.nextBoolean() ? Items.DIAMOND : Items.EMERALD, 1 + random.nextInt(4)));
         chest.setItem(3, new ItemStack(Items.GOLD_INGOT, 2 + random.nextInt(6)));
         if (random.nextInt(3) == 0) {
            chest.setItem(4, FactionTournaments.prize(random, ""));
         }
      }

      AncientWarfareCore.LOG.info("Buried treasure at {}", pos);
   }

   /** "to the north, some 600 blocks away" from x, z toward tx, tz. */
   static MutableComponent where(double x, double z, double tx, double tz) {
      double dx = tx - x;
      double dz = tz - z;
      int distance = (int)Math.round(Math.sqrt(dx * dx + dz * dz) / 50.0) * 50;
      if (distance < 100) {
         return Component.translatable("rumour.ancientwarfare.where.near");
      }

      // Eight ways round, north being -z.
      int octant = Math.floorMod((int)Math.round(Math.atan2(dx, -dz) / (Math.PI / 4.0)), 8);
      String[] ways = {"north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest"};
      return Component.translatable("rumour.ancientwarfare.where", Component.translatable("rumour.ancientwarfare.dir." + ways[octant]), distance);
   }

   private static FactionWorldData.Settlement settlement(FactionWorldData data, int id) {
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.id() == id) {
            return settlement;
         }
      }

      return null;
   }

   private static FactionWorldData.Settlement nearest(
      FactionWorldData data, double x, double z, int range, java.util.function.Predicate<FactionWorldData.Settlement> filter
   ) {
      FactionWorldData.Settlement nearest = null;
      double best = (double)range * range;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         double distance = settlement.horizontalDistanceSquared(x, z);
         if (distance < best && filter.test(settlement)) {
            nearest = settlement;
            best = distance;
         }
      }

      return nearest;
   }

   private static double distanceSq(double ax, double az, double bx, double bz) {
      return (ax - bx) * (ax - bx) + (az - bz) * (az - bz);
   }
}
