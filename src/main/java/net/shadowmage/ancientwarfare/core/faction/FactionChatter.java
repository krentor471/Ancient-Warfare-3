package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;

/**
 * Townsfolk talk. Now and then one of a faction's people near a player says something to them, and
 * what they say follows what is around them: the time of day and the weather, the season, how their
 * town is doing (hunger, sickness, a market or a feast, a war, new houses), what they think of the
 * player, and what the player is doing (wounded, sword drawn, sneaking, on horseback, ruling a
 * country, working for the town's board). Two neighbours standing together trade a few words.
 * People answer what the player does too: they shout when someone breaks their things or attacks
 * one of them, and thank a player who helped.
 */
public final class FactionChatter {
   private static final int TICK_INTERVAL = 20;
   private static final double TALK_RANGE = 10.0;
   private static final double HEAR_RANGE = 16.0;
   private static final int MIN_GAP = 400;
   private static final int GAP_SPREAD = 500;
   private static final long SPEAKER_REST = 1200L;
   private static final long REPLY_DELAY = 50L;
   private static final long SHOUT_REST = 100L;
   private static final double DIALOG_CHANCE = 0.25;
   private static final int DIALOGS = 10;
   private static final int FRIEND_STANDING = 30;
   private static final Map<String, Integer> COUNTS = Map.ofEntries(
      Map.entry("greet.friend", 6),
      Map.entry("greet.neutral", 6),
      Map.entry("greet.hostile", 6),
      Map.entry("soldier", 6),
      Map.entry("trader", 5),
      Map.entry("priest", 4),
      Map.entry("bard", 4),
      Map.entry("idle", 12),
      Map.entry("morning", 4),
      Map.entry("evening", 4),
      Map.entry("night", 5),
      Map.entry("rain", 4),
      Map.entry("thunder", 2),
      Map.entry("winter", 4),
      Map.entry("spring", 3),
      Map.entry("summer", 3),
      Map.entry("autumn", 4),
      Map.entry("famine", 4),
      Map.entry("plague", 3),
      Map.entry("market", 4),
      Map.entry("festival", 4),
      Map.entry("war", 4),
      Map.entry("grown", 3),
      Map.entry("wounded", 3),
      Map.entry("weapon", 3),
      Map.entry("sneak", 3),
      Map.entry("riding", 2),
      Map.entry("country", 3),
      Map.entry("helper", 3),
      Map.entry("break", 4),
      Map.entry("attack", 4),
      Map.entry("thanks", 2)
   );
   /** When each player next hears someone, and when each speaker may speak again; not saved. */
   private static final Map<UUID, Long> NEXT_FOR_PLAYER = new HashMap<>();
   private static final Map<UUID, Long> SPEAKER_RESTS = new HashMap<>();
   private static final Map<String, Long> SHOUTS = new HashMap<>();
   private static final List<Reply> REPLIES = new ArrayList<>();

   private record Reply(UUID speaker, int dialog, long at) {
   }

   private record Topic(String category, Object arg, int weight) {
   }

   private FactionChatter() {
   }

   public static void tick(ServerLevel level, FactionWorldData data) {
      long now = level.getGameTime();
      answer(level, now);
      if (now % TICK_INTERVAL != 0L) {
         return;
      }

      RandomSource random = level.getRandom();
      for (ServerPlayer player : level.players()) {
         if (player.isSpectator()) {
            continue;
         }

         Long next = NEXT_FOR_PLAYER.get(player.getUUID());
         if (next == null) {
            NEXT_FOR_PLAYER.put(player.getUUID(), now + MIN_GAP / 2 + random.nextInt(GAP_SPREAD));
            continue;
         }

         if (now >= next && talkTo(level, data, player, now, random)) {
            NEXT_FOR_PLAYER.put(player.getUUID(), now + MIN_GAP + random.nextInt(GAP_SPREAD));
         }
      }

      if (now % 6000L == 0L) {
         SPEAKER_RESTS.values().removeIf(until -> until < now);
         SHOUTS.values().removeIf(until -> until < now);
      }
   }

   /** People who may chat: not marching, trading on the road, fleeing with a purse or fighting. */
   private static boolean mayTalk(FactionNpcEntity npc, long now) {
      Long rest = SPEAKER_RESTS.get(npc.getUUID());
      return npc.getTarget() == null && (rest == null || rest <= now) && mayShout(npc);
   }

   /** A cry needs no rest and comes even from someone already fighting. */
   private static boolean mayShout(FactionNpcEntity npc) {
      return npc.isAlive()
         && !npc.isSleeping()
         && FactionArmies.armyId(npc) == 0
         && FactionCaravans.caravanId(npc) == 0
         && !FactionTownLife.isThief(npc)
         && !FactionDiplomacy.isOutlaw(npc.getFactionName());
   }

   private static List<FactionNpcEntity> near(ServerLevel level, Entity around, double range, long now) {
      return level.getEntitiesOfClass(FactionNpcEntity.class, around.getBoundingBox().inflate(range), npc -> mayTalk(npc, now));
   }

   private static boolean talkTo(ServerLevel level, FactionWorldData data, ServerPlayer player, long now, RandomSource random) {
      List<FactionNpcEntity> people = near(level, player, TALK_RANGE, now).stream().filter(npc -> npc.hasLineOfSight(player)).toList();
      if (people.isEmpty()) {
         return false;
      }

      if (random.nextDouble() < DIALOG_CHANCE && dialog(level, people, now, random)) {
         return true;
      }

      // Whoever the player is looking at answers first.
      FactionNpcEntity speaker = null;
      Vec3 look = player.getViewVector(1.0F);
      double best = 0.9;
      for (FactionNpcEntity npc : people) {
         Vec3 to = npc.getEyePosition().subtract(player.getEyePosition()).normalize();
         double dot = look.dot(to);
         if (dot > best) {
            best = dot;
            speaker = npc;
         }
      }

      if (speaker == null) {
         speaker = people.get(random.nextInt(people.size()));
      }

      Topic topic = pick(topics(level, data, player, speaker), random);
      if (topic == null) {
         return false;
      }

      say(level, speaker, line(topic.category, random, topic.arg), now);
      speaker.getLookControl().setLookAt(player, 30.0F, 30.0F);
      return true;
   }

   /** Two townsfolk side by side: the first asks, the other answers a moment later. */
   private static boolean dialog(ServerLevel level, List<FactionNpcEntity> people, long now, RandomSource random) {
      for (FactionNpcEntity first : people) {
         if (!civilian(first)) {
            continue;
         }

         for (FactionNpcEntity second : people) {
            if (second != first && civilian(second) && first.distanceToSqr(second) < 25.0 && first.getFactionName().equals(second.getFactionName())) {
               int dialog = random.nextInt(DIALOGS);
               say(level, first, Component.translatable("chatter.ancientwarfare.dialog." + dialog + ".a"), now);
               first.getLookControl().setLookAt(second, 30.0F, 30.0F);
               SPEAKER_RESTS.put(second.getUUID(), now + SPEAKER_REST);
               REPLIES.add(new Reply(second.getUUID(), dialog, now + REPLY_DELAY));
               return true;
            }
         }
      }

      return false;
   }

   private static void answer(ServerLevel level, long now) {
      for (Iterator<Reply> replies = REPLIES.iterator(); replies.hasNext(); ) {
         Reply reply = replies.next();
         if (reply.at > now) {
            continue;
         }

         replies.remove();
         if (level.getEntity(reply.speaker) instanceof FactionNpcEntity npc && npc.isAlive()) {
            say(level, npc, Component.translatable("chatter.ancientwarfare.dialog." + reply.dialog + ".b"), now);
         }
      }
   }

   private static boolean civilian(FactionNpcEntity npc) {
      FactionNpcRole role = FactionNpcRole.byId(npc.getNpcSubtype()).orElse(FactionNpcRole.SOLDIER);
      return role == FactionNpcRole.CIVILIAN_MALE || role == FactionNpcRole.CIVILIAN_FEMALE;
   }

   private static List<Topic> topics(ServerLevel level, FactionWorldData data, ServerPlayer player, FactionNpcEntity npc) {
      List<Topic> topics = new ArrayList<>();
      String faction = npc.getFactionName();
      FactionStandingData standings = FactionStandingData.get(level);
      if (standings.isHostile(player.getUUID(), faction)) {
         topics.add(new Topic("greet.hostile", null, 6));
         if (holdsWeapon(player)) {
            topics.add(new Topic("weapon", null, 2));
         }

         return topics;
      }

      topics.add(new Topic(standings.getStanding(player.getUUID(), faction) >= FRIEND_STANDING ? "greet.friend" : "greet.neutral", null, 2));
      FactionNpcRole role = FactionNpcRole.byId(npc.getNpcSubtype()).orElse(FactionNpcRole.SOLDIER);
      String roleTopic = switch (role) {
         case TRADER -> "trader";
         case PRIEST -> "priest";
         case BARD -> "bard";
         case CIVILIAN_MALE, CIVILIAN_FEMALE -> null;
         default -> "soldier";
      };
      if (roleTopic != null) {
         topics.add(new Topic(roleTopic, null, 3));
      }

      topics.add(new Topic("idle", null, 3));
      long time = level.getDayTime() % FactionSimulation.DAY_TICKS;
      if (time < 2500L) {
         topics.add(new Topic("morning", null, 2));
      } else if (time >= 11500L && time < 13000L) {
         topics.add(new Topic("evening", null, 2));
      } else if (time >= 13000L && time < 23000L) {
         topics.add(new Topic("night", null, 3));
      }

      if (level.isThundering()) {
         topics.add(new Topic("thunder", null, 4));
      } else if (level.isRaining() && level.isRainingAt(npc.blockPosition().above())) {
         topics.add(new Topic("rain", null, 3));
      }

      topics.add(new Topic(FactionSeasons.season(level).name().toLowerCase(java.util.Locale.ROOT), null, 2));
      FactionWorldData.Settlement town = FactionSimulation.settlementAt(data, faction, npc.getX(), npc.getZ(), 24);
      if (town != null) {
         if (town.famine() > 0) {
            topics.add(new Topic("famine", null, 5));
         }

         if (town.plague() > 0) {
            topics.add(new Topic("plague", null, 5));
         }

         String event = FactionTownLife.eventName(level, data, town);
         if (event != null) {
            topics.add(new Topic(event, null, 4));
         }

         if (town.grown() > 0) {
            topics.add(new Topic("grown", null, 1));
         }

         List<FactionQuests.Quest> quests = data.allQuests().get(player.getUUID());
         if (quests != null && quests.stream().anyMatch(quest -> !quest.done && quest.town == town.id())) {
            topics.add(new Topic("helper", null, 3));
         }
      }

      String enemy = enemy(level, data, faction);
      if (enemy != null) {
         topics.add(new Topic("war", FactionNews.factionName(enemy), 2));
      }

      if (player.getHealth() < player.getMaxHealth() * 0.4F) {
         topics.add(new Topic("wounded", null, 6));
      }

      if (holdsWeapon(player)) {
         topics.add(new Topic("weapon", null, 2));
      }

      if (player.isCrouching()) {
         topics.add(new Topic("sneak", null, 4));
      }

      if (player.isPassenger()) {
         topics.add(new Topic("riding", null, 3));
      }

      FactionWorldData.Country country = data.country(player.getUUID());
      if (country != null) {
         topics.add(new Topic("country", country.name, 2));
      }

      return topics;
   }

   private static boolean holdsWeapon(Player player) {
      var item = player.getMainHandItem().getItem();
      return item instanceof SwordItem || item instanceof AxeItem || item instanceof BowItem;
   }

   private static String enemy(ServerLevel level, FactionWorldData data, String faction) {
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         String other = settlement.faction();
         if (!other.equals(faction) && !FactionDiplomacy.isOutlaw(other) && FactionDiplomacy.relation(level, faction, other).state() == RelationState.WAR) {
            return other;
         }
      }

      return null;
   }

   private static Topic pick(List<Topic> topics, RandomSource random) {
      int total = topics.stream().mapToInt(Topic::weight).sum();
      if (total <= 0) {
         return null;
      }

      int roll = random.nextInt(total);
      for (Topic topic : topics) {
         roll -= topic.weight;
         if (roll < 0) {
            return topic;
         }
      }

      return null;
   }

   private static Component line(String category, RandomSource random, Object arg) {
      String key = "chatter.ancientwarfare." + category + "." + random.nextInt(COUNTS.getOrDefault(category, 1));
      return arg == null ? Component.translatable(key) : Component.translatable(key, arg);
   }

   /** The speaker's words reach everyone close enough to hear them. */
   private static void say(ServerLevel level, FactionNpcEntity speaker, Component line, long now) {
      SPEAKER_RESTS.put(speaker.getUUID(), now + SPEAKER_REST);
      Component message = Component.translatable("chatter.ancientwarfare.say", speaker.getDisplayName().copy().withStyle(ChatFormatting.GOLD), line)
         .withStyle(ChatFormatting.GRAY);
      for (ServerPlayer player : level.players()) {
         if (player.distanceToSqr(speaker) <= HEAR_RANGE * HEAR_RANGE) {
            player.sendSystemMessage(message);
         }
      }
   }

   /** Someone nearby cries out at what the player did; one cry a little while per player and kind. */
   private static void shout(ServerLevel level, Player player, Entity near, String category, Entity except) {
      long now = level.getGameTime();
      String key = player.getUUID() + "/" + category;
      Long until = SHOUTS.get(key);
      if (until != null && until > now) {
         return;
      }

      for (FactionNpcEntity npc : level.getEntitiesOfClass(FactionNpcEntity.class, near.getBoundingBox().inflate(HEAR_RANGE), FactionChatter::mayShout)) {
         if (npc != except && (category.equals("thanks") || !FactionStandingData.get(level).isHostile(player.getUUID(), npc.getFactionName()) || civilian(npc))) {
            SHOUTS.put(key, now + SHOUT_REST);
            say(level, npc, line(category, level.getRandom(), null), now);
            npc.getLookControl().setLookAt(player, 30.0F, 30.0F);
            return;
         }
      }
   }

   /** A player broke a block inside a faction's settlement. */
   public static void onBreak(ServerLevel level, Player player, net.minecraft.core.BlockPos pos) {
      FactionWorldData data = FactionWorldData.get(level);
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.built() && settlement.contains(pos.getX(), pos.getZ(), 0)) {
            shout(level, player, player, "break", null);
            return;
         }
      }
   }

   /** A player struck one of a faction's people. */
   public static void onAttacked(ServerLevel level, Player player, FactionNpcEntity victim) {
      if (FactionArmies.armyId(victim) == 0 && !FactionTownLife.isThief(victim)) {
         shout(level, player, victim, "attack", victim);
      }
   }

   /** A player helped the town: someone there says thanks. */
   public static void thank(ServerLevel level, Player player) {
      shout(level, player, player, "thanks", null);
   }
}
