package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;

/**
 * How rulers and their courts pass their days. A ruler rides out to hunt and bids the players he
 * knows: a guest who comes is liked the better, the more so for the beasts he takes, and may save
 * the ruler when the boar turns on him; not every hunt ends well. A pious ruler goes on pilgrimage
 * and leaves his court for some days, wars the less meanwhile and comes back, mostly, with thanks for
 * those who paid his way. And courtiers quarrel; a player may take a side, and the ruler takes one in
 * the end, and the loser may be sent from court.
 */
public final class FactionPastimes extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_faction_pastimes";
   private static final double HUNT_CHANCE = 0.03;
   private static final int HUNT_RANGE = 96;
   private static final int INVITE_STANDING = -20;
   private static final int HUNT_STANDING = 5;
   private static final int BEAST_STANDING = 3;
   private static final int MAX_BEAST_STANDING = 15;
   /** Who takes this many beasts or more, the most of all, is the hunt's hero. */
   private static final int HERO_BEASTS = 3;
   private static final double HERO_FRIEND_CHANCE = 0.4;
   private static final int SNUB_STANDING = -8;
   private static final double INJURY_CHANCE = 0.06;
   private static final double HUNT_DEATH_CHANCE = 0.15;
   private static final double RESCUE_CHANCE = 0.5;
   private static final int RESCUE_STANDING = 15;
   private static final double PILGRIM_CHANCE = 0.004;
   private static final double PIOUS_PILGRIM_CHANCE = 0.03;
   private static final int PILGRIM_DAYS = 3;
   public static final int PILGRIM_GIFT = 30;
   private static final int PILGRIM_GIFT_STANDING = 6;
   private static final int PILGRIM_THANKS_STANDING = 4;
   private static final double PILGRIM_FRIEND_CHANCE = 0.3;
   private static final double ROAD_DEATH_CHANCE = 0.04;
   private static final double ROBBED_CHANCE = 0.12;
   private static final int ROBBED_GOLD = 150;
   /** While the ruler is away his realm is slow to war. */
   private static final double AWAY_WAR = 0.3;
   private static final double QUARREL_CHANCE = 0.03;
   private static final int QUARREL_DAYS = 3;
   private static final int SIDE_FAVOR = 8;
   private static final int SIDE_RIGHT_STANDING = 5;
   private static final int SIDE_WRONG_STANDING = -3;
   private static final double BANISH_CHANCE = 0.5;
   private static final int CHECK_TICKS = 100;
   private static final String[] CAUSES = {"spoils", "seat", "insult", "money"};

   private static final class Hunt {
      final String faction;
      final long day;
      final Set<UUID> coming = new HashSet<>();
      final Map<UUID, Integer> came = new HashMap<>();

      Hunt(String faction, long day) {
         this.faction = faction;
         this.day = day;
      }
   }

   private static final class Pilgrimage {
      final String faction;
      final String ruler;
      final long back;
      final Set<UUID> donors = new HashSet<>();

      Pilgrimage(String faction, String ruler, long back) {
         this.faction = faction;
         this.ruler = ruler;
         this.back = back;
      }
   }

   private static final class Quarrel {
      final String faction;
      final FactionCourt.Role a;
      final FactionCourt.Role b;
      /** Whom the ruler leans to; the player may guess it from the ruler's character. */
      final FactionCourt.Role favourite;
      final String cause;
      final long end;
      final Map<UUID, FactionCourt.Role> sides = new HashMap<>();

      Quarrel(String faction, FactionCourt.Role a, FactionCourt.Role b, FactionCourt.Role favourite, String cause, long end) {
         this.faction = faction;
         this.a = a;
         this.b = b;
         this.favourite = favourite;
         this.cause = cause;
         this.end = end;
      }
   }

   private final Map<String, Hunt> hunts = new HashMap<>();
   private final Map<String, Pilgrimage> pilgrimages = new HashMap<>();
   private final Map<String, Quarrel> quarrels = new HashMap<>();

   public static FactionPastimes get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(FactionPastimes::load, FactionPastimes::new, DATA_NAME);
   }

   /** Whether the faction's ruler is away on pilgrimage. */
   public static boolean away(ServerLevel level, String faction) {
      Pilgrimage pilgrimage = get(level).pilgrimages.get(faction);
      return pilgrimage != null && pilgrimage.ruler.equals(rulerKey(level, faction));
   }

   static double warFactor(ServerLevel level, String faction, String other) {
      return away(level, faction) || away(level, other) ? AWAY_WAR : 1.0;
   }

   private static String rulerKey(ServerLevel level, String faction) {
      return FactionRulers.key(FactionRulers.ruler(FactionWorldData.get(level), faction, level.getRandom()));
   }

   private static void tellKnown(ServerLevel level, String faction, Component message) {
      FactionStandingData standings = FactionStandingData.get(level);
      for (ServerPlayer player : level.players()) {
         if (standings.knownFactions(player.getUUID()).contains(faction)) {
            player.sendSystemMessage(message);
         }
      }
   }

   private static MutableComponent button(Component label, String command, ChatFormatting color) {
      return Component.literal("[")
         .append(label)
         .append("]")
         .withStyle(
            style -> style.withColor(color)
               .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
               .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, label))
         );
   }

   /** The overview's lines: a hunt to come, the ruler away, a quarrel at court. */
   static List<Component> lines(ServerLevel level, UUID player, String faction) {
      FactionPastimes pastimes = get(level);
      List<Component> lines = new ArrayList<>();
      long day = Math.max(0L, FactionWorldData.get(level).lastSimulatedDay());
      Hunt hunt = pastimes.hunts.get(faction);
      if (hunt != null) {
         String key = hunt.came.containsKey(player) ? "hunt.ancientwarfare.line.came" : hunt.coming.contains(player) ? "hunt.ancientwarfare.line.coming" : "hunt.ancientwarfare.line.hunt";
         lines.add(Component.translatable(key, FactionFeasts.capitalName(level, faction)).withStyle(ChatFormatting.GOLD));
      }

      if (away(level, faction)) {
         Pilgrimage pilgrimage = pastimes.pilgrimages.get(faction);
         lines.add(Component.translatable("pilgrim.ancientwarfare.line", Math.max(1L, pilgrimage.back - day)).withStyle(ChatFormatting.AQUA));
      }

      Quarrel quarrel = pastimes.quarrels.get(faction);
      if (quarrel != null) {
         FactionCourt.Role side = quarrel.sides.get(player);
         lines.add(
            Component.translatable(
                  side == null ? "quarrel.ancientwarfare.line" : "quarrel.ancientwarfare.line_sided",
                  courtier(level, faction, quarrel.a),
                  courtier(level, faction, quarrel.b),
                  side == null ? Component.empty() : courtier(level, faction, side)
               )
               .withStyle(ChatFormatting.YELLOW)
         );
      }

      return lines;
   }

   /** "the treasurer Darius" */
   private static Component courtier(ServerLevel level, String faction, FactionCourt.Role role) {
      return Component.translatable("quarrel.ancientwarfare.courtier", Component.translatable("quarrel.ancientwarfare.role." + role.key()), FactionCourt.courtierName(level, faction, role));
   }

   // ---------------------------------------------------------------- days

   static void simulateDay(ServerLevel level, FactionWorldData data) {
      FactionPastimes pastimes = get(level);
      long day = Math.max(0L, data.lastSimulatedDay());
      RandomSource random = level.getRandom();
      Set<String> factions = new TreeSet<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!FactionDiplomacy.isOutlaw(settlement.faction())) {
            factions.add(settlement.faction());
         }
      }

      // Yesterday's hunts are over.
      pastimes.hunts.values().removeIf(hunt -> {
         if (day <= hunt.day) {
            return false;
         }

         pastimes.endHunt(level, hunt, day);
         return true;
      });
      // Pilgrims come home, or do not.
      pastimes.pilgrimages.values().removeIf(pilgrimage -> {
         if (!pilgrimage.ruler.equals(rulerKey(level, pilgrimage.faction))) {
            return true;
         } else if (day < pilgrimage.back) {
            return false;
         }

         pastimes.endPilgrimage(level, data, pilgrimage, day);
         return true;
      });
      // Quarrels the ruler now settles.
      pastimes.quarrels.values().removeIf(quarrel -> {
         if (day < quarrel.end) {
            return false;
         }

         pastimes.settle(level, quarrel, day);
         return true;
      });
      for (String faction : factions) {
         FactionWorldData.Ruler ruler = FactionRulers.ruler(data, faction, random);
         boolean home = !pastimes.pilgrimages.containsKey(faction);
         if (home && !ruler.ill() && !pastimes.hunts.containsKey(faction) && FactionFeasts.capital(data, faction) != null && random.nextDouble() < HUNT_CHANCE) {
            pastimes.proclaimHunt(level, data, faction, day + 1);
         } else if (home && !ruler.ill() && !pastimes.hunts.containsKey(faction)
            && random.nextDouble() < (FactionPeople.has(ruler.character(), FactionPeople.Character.PIOUS) ? PIOUS_PILGRIM_CHANCE : PILGRIM_CHANCE) * FactionFaith.pilgrimFactor(level, faction)) {
            pastimes.depart(level, data, faction, day);
         }

         if (!pastimes.quarrels.containsKey(faction) && random.nextDouble() < QUARREL_CHANCE * FactionPeople.factor(ruler.character(), c -> c.plot)) {
            pastimes.quarrel(level, faction, day);
         }
      }

      pastimes.setDirty();
   }

   // ---------------------------------------------------------------- hunts

   private void proclaimHunt(ServerLevel level, FactionWorldData data, String faction, long day) {
      this.hunts.put(faction, new Hunt(faction, day));
      FactionWorldData.Ruler ruler = FactionRulers.ruler(data, faction, level.getRandom());
      FactionStandingData standings = FactionStandingData.get(level);
      for (ServerPlayer player : level.players()) {
         UUID id = player.getUUID();
         if (!standings.knownFactions(id).contains(faction) || standings.getTreaty(id, faction) == RelationState.WAR || standings.getStanding(id, faction) < INVITE_STANDING) {
            continue;
         }

         player.sendSystemMessage(
            Component.translatable("hunt.ancientwarfare.invite", FactionRulers.name(ruler.name()), FactionNews.factionName(faction), FactionFeasts.capitalName(level, faction))
               .withStyle(ChatFormatting.GOLD)
         );
         player.sendSystemMessage(Component.literal("  ").append(FactionEnvoys.button("hunt.ancientwarfare.accept", "/awhunt " + faction, ChatFormatting.GREEN)));
      }

      this.setDirty();
   }

   public static void accept(ServerPlayer player, String faction) {
      FactionPastimes pastimes = get(player.serverLevel());
      Hunt hunt = pastimes.hunts.get(faction);
      if (hunt == null) {
         player.sendSystemMessage(Component.translatable("hunt.ancientwarfare.none").withStyle(ChatFormatting.GRAY));
         return;
      }

      hunt.coming.add(player.getUUID());
      pastimes.setDirty();
      player.sendSystemMessage(Component.translatable("hunt.ancientwarfare.accepted", FactionFeasts.capitalName(player.serverLevel(), faction), HUNT_RANGE).withStyle(ChatFormatting.GOLD));
   }

   /** The hunt the player rides in today, if he is near enough to its capital. */
   private Hunt huntHere(ServerLevel level, FactionWorldData data, ServerPlayer player) {
      long day = data.lastSimulatedDay();
      for (Hunt hunt : this.hunts.values()) {
         FactionWorldData.Settlement capital = FactionFeasts.capital(data, hunt.faction);
         if (hunt.day == day && capital != null && capital.horizontalDistanceSquared(player.getX(), player.getZ()) < (double)HUNT_RANGE * HUNT_RANGE
            && FactionStandingData.get(level).getTreaty(player.getUUID(), hunt.faction) != RelationState.WAR) {
            return hunt;
         }
      }

      return null;
   }

   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % CHECK_TICKS != 0L) {
         return;
      }

      FactionPastimes pastimes = get(level);
      if (pastimes.hunts.isEmpty()) {
         return;
      }

      for (ServerPlayer player : level.players()) {
         Hunt hunt = pastimes.huntHere(level, data, player);
         if (hunt != null && !hunt.came.containsKey(player.getUUID())) {
            hunt.came.put(player.getUUID(), 0);
            pastimes.setDirty();
            FactionWorldData.Ruler ruler = FactionRulers.ruler(data, hunt.faction, level.getRandom());
            player.sendSystemMessage(Component.translatable("hunt.ancientwarfare.welcome", FactionRulers.name(ruler.name())).withStyle(ChatFormatting.GOLD));
         }
      }
   }

   /** A beast a player killed: counted if he rides in a hunt today. */
   public static void onBeastKilled(ServerLevel level, ServerPlayer player) {
      FactionPastimes pastimes = get(level);
      if (pastimes.hunts.isEmpty()) {
         return;
      }

      Hunt hunt = pastimes.huntHere(level, FactionWorldData.get(level), player);
      if (hunt != null) {
         int beasts = hunt.came.getOrDefault(player.getUUID(), 0) + 1;
         hunt.came.put(player.getUUID(), beasts);
         pastimes.setDirty();
         player.displayClientMessage(Component.translatable("hunt.ancientwarfare.beast", beasts).withStyle(ChatFormatting.GOLD), true);
      }
   }

   private void endHunt(ServerLevel level, Hunt hunt, long day) {
      RandomSource random = level.getRandom();
      FactionStandingData standings = FactionStandingData.get(level);
      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.Ruler ruler = FactionRulers.ruler(data, hunt.faction, random);
      Component name = FactionRulers.name(ruler.name());
      for (UUID guest : hunt.coming) {
         if (!hunt.came.containsKey(guest)) {
            standings.adjustStanding(guest, hunt.faction, SNUB_STANDING);
            FactionNews.tellPlayer(level, guest, Component.translatable("hunt.ancientwarfare.snub", name).withStyle(ChatFormatting.RED));
         }
      }

      UUID hero = null;
      int best = HERO_BEASTS - 1;
      for (Map.Entry<UUID, Integer> guest : hunt.came.entrySet()) {
         int beasts = guest.getValue();
         standings.adjustStanding(guest.getKey(), hunt.faction, HUNT_STANDING + Math.min(MAX_BEAST_STANDING, beasts * BEAST_STANDING));
         FactionNews.tellPlayer(level, guest.getKey(), Component.translatable("hunt.ancientwarfare.over", name, beasts).withStyle(ChatFormatting.GOLD));
         if (beasts > best) {
            best = beasts;
            hero = guest.getKey();
         }
      }

      if (hero != null) {
         FactionNews.tellPlayer(level, hero, Component.translatable("hunt.ancientwarfare.hero", name).withStyle(ChatFormatting.GREEN));
         if (FactionFeasts.playerBond(level, hero, hunt.faction) != FactionFeasts.FRIEND && random.nextDouble() < HERO_FRIEND_CHANCE) {
            FactionFeasts.befriend(level, hero, hunt.faction, FactionFeasts.FRIEND);
            FactionNews.tellPlayer(level, hero, Component.translatable("hunt.ancientwarfare.friend", name).withStyle(ChatFormatting.GREEN));
         }
      }

      // The boar turns, the horse falls: a drunkard is the likelier to come to grief, a hale man the less.
      double injury = INJURY_CHANCE * (FactionPeople.has(ruler.character(), FactionPeople.Character.DRUNKARD) ? 2.0 : 1.0)
         * (FactionPeople.has(ruler.character(), FactionPeople.Character.HEALTHY) ? 0.5 : 1.0);
      if (random.nextDouble() >= injury) {
         return;
      }

      List<UUID> guests = new ArrayList<>(hunt.came.keySet());
      if (!guests.isEmpty() && random.nextDouble() < RESCUE_CHANCE) {
         UUID saviour = guests.get(random.nextInt(guests.size()));
         standings.adjustStanding(saviour, hunt.faction, RESCUE_STANDING);
         FactionFeasts.befriend(level, saviour, hunt.faction, FactionFeasts.FRIEND);
         FactionNews.tellPlayer(level, saviour, Component.translatable("hunt.ancientwarfare.rescue", name).withStyle(ChatFormatting.GREEN));
         return;
      }

      if (random.nextDouble() < HUNT_DEATH_CHANCE) {
         FactionRulers.die(level, hunt.faction, "hunt", day);
      } else {
         data.setRuler(hunt.faction, ruler.withIll(true));
         FactionNews.tell(level, Component.translatable("news.ancientwarfare.hunt_injury", FactionNews.factionName(hunt.faction), name));
      }
   }

   // ---------------------------------------------------------------- pilgrimages

   private void depart(ServerLevel level, FactionWorldData data, String faction, long day) {
      FactionWorldData.Ruler ruler = FactionRulers.ruler(data, faction, level.getRandom());
      Pilgrimage pilgrimage = new Pilgrimage(faction, FactionRulers.key(ruler), day + PILGRIM_DAYS + level.getRandom().nextInt(PILGRIM_DAYS));
      this.pilgrimages.put(faction, pilgrimage);
      FactionNews.tell(
         level,
         Component.translatable(
            "news.ancientwarfare.pilgrimage", FactionNews.factionName(faction), FactionRulers.name(ruler.name()), FactionCourt.courtierName(level, faction, FactionCourt.Role.HEIR)
         )
      );
      tellKnown(
         level,
         faction,
         Component.literal("  ").append(button(Component.translatable("pilgrim.ancientwarfare.give", PILGRIM_GIFT / 10), "/awpilgrim " + faction, ChatFormatting.GREEN))
      );
      this.setDirty();
   }

   /** The player pays toward the ruler's road. */
   public static void give(ServerPlayer player, String faction) {
      ServerLevel level = player.serverLevel();
      FactionPastimes pastimes = get(level);
      Pilgrimage pilgrimage = pastimes.pilgrimages.get(faction);
      if (pilgrimage == null || !away(level, faction)) {
         player.sendSystemMessage(Component.translatable("pilgrim.ancientwarfare.none").withStyle(ChatFormatting.GRAY));
         return;
      } else if (pilgrimage.donors.contains(player.getUUID())) {
         player.sendSystemMessage(Component.translatable("pilgrim.ancientwarfare.already").withStyle(ChatFormatting.GRAY));
         return;
      } else if (!PlayerDiplomacy.takeEmeralds(player, PILGRIM_GIFT)) {
         player.sendSystemMessage(Component.translatable("diplomacy.ancientwarfare.gift_missing", PILGRIM_GIFT).withStyle(ChatFormatting.RED));
         return;
      }

      pilgrimage.donors.add(player.getUUID());
      pastimes.setDirty();
      int standing = (int)Math.round(PILGRIM_GIFT_STANDING * FactionRulers.character(level, faction, c -> c.gift));
      FactionStandingData.get(level).adjustStanding(player.getUUID(), faction, standing);
      player.sendSystemMessage(Component.translatable("pilgrim.ancientwarfare.given", FactionNews.factionName(faction), standing).withStyle(ChatFormatting.GREEN));
   }

   private void endPilgrimage(ServerLevel level, FactionWorldData data, Pilgrimage pilgrimage, long day) {
      RandomSource random = level.getRandom();
      FactionWorldData.Ruler ruler = FactionRulers.ruler(data, pilgrimage.faction, random);
      Component name = FactionRulers.name(ruler.name());
      Component faction = FactionNews.factionName(pilgrimage.faction);
      double death = ROAD_DEATH_CHANCE * FactionPeople.factor(ruler.character(), c -> c.illness) * (FactionPeople.age(ruler.born(), day) >= 60 ? 2.0 : 1.0);
      if (random.nextDouble() < death) {
         FactionRulers.die(level, pilgrimage.faction, "road", day);
         return;
      }

      // Those who paid his way paid for guards too.
      if (random.nextDouble() < ROBBED_CHANCE * (pilgrimage.donors.isEmpty() ? 1.0 : 0.5)) {
         int lost = Math.min(ROBBED_GOLD, data.treasury(pilgrimage.faction));
         data.setTreasury(pilgrimage.faction, data.treasury(pilgrimage.faction) - lost);
         FactionNews.tell(level, Component.translatable("news.ancientwarfare.pilgrim_robbed", faction, name, lost / 10));
      } else {
         if (ruler.ill()) {
            data.setRuler(pilgrimage.faction, ruler.withIll(false));
         }

         FactionNews.tell(level, Component.translatable(ruler.ill() ? "news.ancientwarfare.pilgrim_healed" : "news.ancientwarfare.pilgrim_back", faction, name));
      }

      FactionStandingData standings = FactionStandingData.get(level);
      for (UUID donor : pilgrimage.donors) {
         standings.adjustStanding(donor, pilgrimage.faction, PILGRIM_THANKS_STANDING);
         FactionNews.tellPlayer(level, donor, Component.translatable("pilgrim.ancientwarfare.thanks", name).withStyle(ChatFormatting.GREEN));
         if (FactionFeasts.playerBond(level, donor, pilgrimage.faction) != FactionFeasts.FRIEND && random.nextDouble() < PILGRIM_FRIEND_CHANCE) {
            FactionFeasts.befriend(level, donor, pilgrimage.faction, FactionFeasts.FRIEND);
            FactionNews.tellPlayer(level, donor, Component.translatable("pilgrim.ancientwarfare.friend", name).withStyle(ChatFormatting.GREEN));
         }
      }
   }

   // ---------------------------------------------------------------- quarrels

   /** Two courtiers fall out, the likelier the worse their characters suit. */
   private void quarrel(ServerLevel level, String faction, long day) {
      RandomSource random = level.getRandom();
      FactionCourt.Courtier[] court = FactionCourt.court(level, faction);
      FactionCourt.Role[] roles = FactionCourt.Role.values();
      FactionCourt.Role a = null;
      FactionCourt.Role b = null;
      int worst = Integer.MAX_VALUE;
      for (int i = 0; i < roles.length; i++) {
         for (int j = i + 1; j < roles.length; j++) {
            int affinity = FactionFeasts.affinity(court[i].character, court[j].character) * 4 + random.nextInt(4);
            if (affinity < worst) {
               worst = affinity;
               a = roles[i];
               b = roles[j];
            }
         }
      }

      if (a == null) {
         return;
      }

      FactionCourt.Role favourite = favourite(FactionRulers.ruler(FactionWorldData.get(level), faction, random).character(), a, b, random);
      Quarrel quarrel = new Quarrel(faction, a, b, favourite, CAUSES[random.nextInt(CAUSES.length)], day + QUARREL_DAYS);
      this.quarrels.put(faction, quarrel);
      FactionStandingData standings = FactionStandingData.get(level);
      Component first = courtier(level, faction, a);
      Component second = courtier(level, faction, b);
      for (ServerPlayer player : level.players()) {
         UUID id = player.getUUID();
         if (!standings.knownFactions(id).contains(faction) || standings.getTreaty(id, faction) == RelationState.WAR) {
            continue;
         }

         player.sendSystemMessage(
            Component.translatable("quarrel.ancientwarfare.news", FactionNews.factionName(faction), first, second, Component.translatable("quarrel.ancientwarfare.cause." + quarrel.cause))
               .withStyle(ChatFormatting.YELLOW)
         );
         player.sendSystemMessage(
            Component.literal("  ")
               .append(button(Component.translatable("quarrel.ancientwarfare.back", FactionCourt.courtierName(level, faction, a)), "/awquarrel " + faction + " " + a.ordinal(), ChatFormatting.GREEN))
               .append("  ")
               .append(button(Component.translatable("quarrel.ancientwarfare.back", FactionCourt.courtierName(level, faction, b)), "/awquarrel " + faction + " " + b.ordinal(), ChatFormatting.GREEN))
         );
      }

      this.setDirty();
   }

   /** A pious ruler leans to his priest, a fierce or ambitious one to his marshal, an honest or content one to his treasurer, and any to his heir. */
   private static FactionCourt.Role favourite(int character, FactionCourt.Role a, FactionCourt.Role b, RandomSource random) {
      for (FactionCourt.Role role : new FactionCourt.Role[]{a, b}) {
         boolean leans = switch (role) {
            case PRIEST -> FactionPeople.has(character, FactionPeople.Character.PIOUS);
            case MARSHAL -> FactionPeople.has(character, FactionPeople.Character.AMBITIOUS) || FactionPeople.has(character, FactionPeople.Character.CRUEL);
            case TREASURER -> FactionPeople.has(character, FactionPeople.Character.HONEST) || FactionPeople.has(character, FactionPeople.Character.CONTENT);
            case HEIR -> true;
         };
         if (leans) {
            return role;
         }
      }

      return random.nextBoolean() ? a : b;
   }

   /** The player takes one courtier's part. */
   public static void side(ServerPlayer player, String faction, int index) {
      ServerLevel level = player.serverLevel();
      FactionPastimes pastimes = get(level);
      Quarrel quarrel = pastimes.quarrels.get(faction);
      FactionCourt.Role[] roles = FactionCourt.Role.values();
      if (quarrel == null || index < 0 || index >= roles.length || roles[index] != quarrel.a && roles[index] != quarrel.b) {
         player.sendSystemMessage(Component.translatable("quarrel.ancientwarfare.none").withStyle(ChatFormatting.GRAY));
         return;
      } else if (quarrel.sides.containsKey(player.getUUID())) {
         player.sendSystemMessage(Component.translatable("quarrel.ancientwarfare.already").withStyle(ChatFormatting.GRAY));
         return;
      }

      FactionCourt.Role chosen = roles[index];
      FactionCourt.Role other = chosen == quarrel.a ? quarrel.b : quarrel.a;
      quarrel.sides.put(player.getUUID(), chosen);
      pastimes.setDirty();
      FactionCourt.adjustFavor(level, faction, chosen, player.getUUID(), SIDE_FAVOR);
      FactionCourt.adjustFavor(level, faction, other, player.getUUID(), -SIDE_FAVOR);
      boolean right = chosen == quarrel.favourite;
      FactionStandingData.get(level).adjustStanding(player.getUUID(), faction, right ? SIDE_RIGHT_STANDING : SIDE_WRONG_STANDING);
      player.sendSystemMessage(
         Component.translatable(right ? "quarrel.ancientwarfare.sided_right" : "quarrel.ancientwarfare.sided_wrong", courtier(level, faction, chosen), courtier(level, faction, other))
            .withStyle(right ? ChatFormatting.GREEN : ChatFormatting.YELLOW)
      );
   }

   private void settle(ServerLevel level, Quarrel quarrel, long day) {
      FactionCourt.Role loser = quarrel.favourite == quarrel.a ? quarrel.b : quarrel.a;
      Component winner = courtier(level, quarrel.faction, quarrel.favourite);
      Component lost = courtier(level, quarrel.faction, loser);
      Component faction = FactionNews.factionName(quarrel.faction);
      // The heir is never sent away; anyone else may be.
      if (loser != FactionCourt.Role.HEIR && level.getRandom().nextDouble() < BANISH_CHANCE) {
         FactionCourt.replace(level, quarrel.faction, loser, day);
         tellKnown(level, quarrel.faction, Component.translatable("quarrel.ancientwarfare.banished", faction, winner, lost, courtier(level, quarrel.faction, loser)).withStyle(ChatFormatting.YELLOW));
      } else {
         tellKnown(level, quarrel.faction, Component.translatable("quarrel.ancientwarfare.settled", faction, winner, lost).withStyle(ChatFormatting.YELLOW));
      }
   }

   /** For testing: a hunt today, a pilgrimage, a quarrel, or tomorrow's end of all three. */
   public static String debug(ServerLevel level, String faction, String what) {
      FactionPastimes pastimes = get(level);
      FactionWorldData data = FactionWorldData.get(level);
      long day = Math.max(0L, data.lastSimulatedDay());
      switch (what) {
         case "hunt" -> {
            if (FactionFeasts.capital(data, faction) == null) {
               return "No capital";
            }

            pastimes.proclaimHunt(level, data, faction, day);
            return "Hunt at " + FactionFeasts.capital(data, faction).center().toShortString();
         }
         case "pilgrim" -> {
            pastimes.depart(level, data, faction, day);
            return "Away until day " + pastimes.pilgrimages.get(faction).back;
         }
         case "quarrel" -> {
            pastimes.quarrel(level, faction, day);
            Quarrel quarrel = pastimes.quarrels.get(faction);
            return quarrel == null ? "No quarrel" : quarrel.a + " vs " + quarrel.b + ", ruler favours " + quarrel.favourite;
         }
         case "end" -> {
            pastimes.hunts.values().removeIf(hunt -> {
               pastimes.endHunt(level, hunt, day + 1);
               return true;
            });
            pastimes.pilgrimages.values().removeIf(pilgrimage -> {
               pastimes.endPilgrimage(level, data, pilgrimage, day + 1);
               return true;
            });
            pastimes.quarrels.values().removeIf(quarrel -> {
               pastimes.settle(level, quarrel, day + 1);
               return true;
            });
            pastimes.setDirty();
            return "Ended";
         }
         default -> {
            return "hunt|pilgrim|quarrel|end";
         }
      }
   }

   // ---------------------------------------------------------------- saving

   private static FactionPastimes load(CompoundTag tag) {
      FactionPastimes data = new FactionPastimes();
      FactionCourt.Role[] roles = FactionCourt.Role.values();
      for (Tag element : tag.getList("Hunts", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         Hunt hunt = new Hunt(entry.getString("Faction"), entry.getLong("Day"));
         readIds(entry.getList("Coming", Tag.TAG_STRING), hunt.coming);
         CompoundTag came = entry.getCompound("Came");
         for (String id : came.getAllKeys()) {
            try {
               hunt.came.put(UUID.fromString(id), came.getInt(id));
            } catch (IllegalArgumentException ignored) {
            }
         }

         data.hunts.put(hunt.faction, hunt);
      }

      for (Tag element : tag.getList("Pilgrimages", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         Pilgrimage pilgrimage = new Pilgrimage(entry.getString("Faction"), entry.getString("Ruler"), entry.getLong("Back"));
         readIds(entry.getList("Donors", Tag.TAG_STRING), pilgrimage.donors);
         data.pilgrimages.put(pilgrimage.faction, pilgrimage);
      }

      for (Tag element : tag.getList("Quarrels", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         Quarrel quarrel = new Quarrel(
            entry.getString("Faction"),
            roles[Mth.clamp(entry.getInt("A"), 0, roles.length - 1)],
            roles[Mth.clamp(entry.getInt("B"), 0, roles.length - 1)],
            roles[Mth.clamp(entry.getInt("Favourite"), 0, roles.length - 1)],
            entry.getString("Cause"),
            entry.getLong("End")
         );
         CompoundTag sides = entry.getCompound("Sides");
         for (String id : sides.getAllKeys()) {
            try {
               quarrel.sides.put(UUID.fromString(id), roles[Mth.clamp(sides.getInt(id), 0, roles.length - 1)]);
            } catch (IllegalArgumentException ignored) {
            }
         }

         data.quarrels.put(quarrel.faction, quarrel);
      }

      return data;
   }

   private static void readIds(ListTag list, Set<UUID> into) {
      for (Tag element : list) {
         try {
            into.add(UUID.fromString(element.getAsString()));
         } catch (IllegalArgumentException ignored) {
         }
      }
   }

   private static ListTag writeIds(Set<UUID> ids) {
      ListTag list = new ListTag();
      ids.forEach(id -> list.add(StringTag.valueOf(id.toString())));
      return list;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag hunts = new ListTag();
      for (Hunt hunt : this.hunts.values()) {
         CompoundTag entry = new CompoundTag();
         entry.putString("Faction", hunt.faction);
         entry.putLong("Day", hunt.day);
         entry.put("Coming", writeIds(hunt.coming));
         CompoundTag came = new CompoundTag();
         hunt.came.forEach((id, beasts) -> came.putInt(id.toString(), beasts));
         entry.put("Came", came);
         hunts.add(entry);
      }

      tag.put("Hunts", hunts);
      ListTag pilgrimages = new ListTag();
      for (Pilgrimage pilgrimage : this.pilgrimages.values()) {
         CompoundTag entry = new CompoundTag();
         entry.putString("Faction", pilgrimage.faction);
         entry.putString("Ruler", pilgrimage.ruler);
         entry.putLong("Back", pilgrimage.back);
         entry.put("Donors", writeIds(pilgrimage.donors));
         pilgrimages.add(entry);
      }

      tag.put("Pilgrimages", pilgrimages);
      ListTag quarrels = new ListTag();
      for (Quarrel quarrel : this.quarrels.values()) {
         CompoundTag entry = new CompoundTag();
         entry.putString("Faction", quarrel.faction);
         entry.putInt("A", quarrel.a.ordinal());
         entry.putInt("B", quarrel.b.ordinal());
         entry.putInt("Favourite", quarrel.favourite.ordinal());
         entry.putString("Cause", quarrel.cause);
         entry.putLong("End", quarrel.end);
         CompoundTag sides = new CompoundTag();
         quarrel.sides.forEach((id, role) -> sides.putInt(id.toString(), role.ordinal()));
         entry.put("Sides", sides);
         quarrels.add(entry);
      }

      tag.put("Quarrels", quarrels);
      return tag;
   }
}
