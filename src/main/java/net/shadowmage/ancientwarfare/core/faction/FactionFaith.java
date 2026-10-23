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
import net.minecraft.nbt.IntArrayTag;
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
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;

/**
 * The faiths of the factions. Each faction keeps one of four creeds, and each creed has a holy
 * site: a temple or a great town. A realm that holds its own holy site is calmer; one whose holy
 * site is in the hands of unbelievers grieves, its rulers cannot go there on pilgrimage, and it
 * is quick to holy war against the holder. Factions of one faith warm to each other.
 *
 * Once a year each faith keeps its feast: its towns are calmed and the faithful bring offerings,
 * players among them. In a restless realm with a lax or greedy priest, a heresy may take root,
 * leaning toward a neighbour's creed. A pious or cruel ruler persecutes it, which stirs the towns;
 * a tolerant one lets it be, and it may fade, or win the realm over to the new faith. A heresy
 * persecuted until half the realm holds to it ends in a war of faith.
 */
public final class FactionFaith extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_faction_faith";
   public static final int SUN = 0;
   public static final int OLD_GODS = 1;
   public static final int ANCESTORS = 2;
   public static final int FLAME = 3;
   public static final String[] KEYS = {"sun", "old_gods", "ancestors", "flame"};
   private static final Map<String, Integer> FOUNDING = Map.of(
      "empire", SUN, "vyncan", SUN, "norska", OLD_GODS, "barbarian", OLD_GODS, "nogg", ANCESTORS, "buffloka", ANCESTORS, "witchbane", FLAME
   );
   /** Each faith keeps its feast once a year, on this day of its own season. */
   private static final int FEAST_DAY = 2;
   private static final int FEAST_ORDER = 10;
   private static final int FEAST_CALM = -10;
   public static final int OFFERING_COST = 20;
   private static final int OFFERING_STANDING = 4;
   private static final double HERESY_CHANCE = 0.004;
   private static final int PERSECUTED_DISCONTENT = 15;
   private static final long HERESY_FADES = 16L;

   /** Factions whose faith changed from the one they were founded with. */
   private final Map<String, Integer> converted = new HashMap<>();
   /** The settlement id of each faith's holy site, -1 until chosen. */
   private final int[] holySites = {-1, -1, -1, -1};
   private final Map<String, Heresy> heresies = new HashMap<>();
   /** Players who have brought an offering at each faith's feast this year. */
   private final Map<Integer, Set<UUID>> offerings = new HashMap<>();
   private final Set<Integer> feasting = new HashSet<>();

   static final class Heresy {
      final String faction;
      /** The creed it leans to. */
      final int faith;
      final long day;
      final Set<Integer> towns = new HashSet<>();
      /** 0 undecided, 1 persecuted, 2 tolerated. */
      int answer;

      Heresy(String faction, int faith, long day) {
         this.faction = faction;
         this.faith = faith;
         this.day = day;
      }
   }

   public static FactionFaith get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(FactionFaith::load, FactionFaith::new, DATA_NAME);
   }

   public static int faith(ServerLevel level, String faction) {
      Integer converted = get(level).converted.get(faction);
      if (converted != null) {
         return converted;
      }

      Integer founding = FOUNDING.get(faction);
      return founding != null ? founding : Math.floorMod(faction.hashCode(), KEYS.length);
   }

   public static Component name(int faith) {
      return Component.translatable("faith.ancientwarfare." + KEYS[Mth.clamp(faith, 0, KEYS.length - 1)]);
   }

   /** The realm takes up another faith. */
   static void convert(ServerLevel level, String faction, int faith) {
      FactionFaith faiths = get(level);
      faiths.converted.put(faction, faith);
      faiths.heresies.remove(faction);
      faiths.setDirty();
      FactionNews.tell(level, Component.translatable("news.ancientwarfare.converted", FactionNews.factionName(faction), name(faith)).withStyle(ChatFormatting.LIGHT_PURPLE));
   }

   // ---------------------------------------------------------------- holy sites

   /** The faith's holy site, chosen the first time anyone asks: a temple of its faithful, or else a capital. */
   static FactionWorldData.Settlement holySite(ServerLevel level, FactionWorldData data, int faith) {
      FactionFaith faiths = get(level);
      FactionWorldData.Settlement site = faiths.holySites[faith] < 0 ? null : data.settlement(faiths.holySites[faith]);
      if (site != null) {
         return site;
      }

      FactionWorldData.Settlement best = null;
      int bestRank = Integer.MAX_VALUE;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (FactionDiplomacy.isOutlaw(settlement.faction()) || faith(level, settlement.faction()) != faith) {
            continue;
         }

         int rank = WorldMap.isTemple(settlement) ? 0 : FactionTowns.CAPITAL.equals(settlement.kind()) ? 1 : 2;
         if (rank < bestRank || rank == bestRank && best != null && settlement.id() < best.id()) {
            bestRank = rank;
            best = settlement;
         }
      }

      if (best != null) {
         faiths.holySites[faith] = best.id();
         faiths.setDirty();
      }

      return best;
   }

   /** Whether the faction holds the holy site of the faith, and is not of it. */
   private static boolean holdsAsInfidel(ServerLevel level, FactionWorldData data, String holder, int faith) {
      FactionWorldData.Settlement site = holySite(level, data, faith);
      return site != null && site.faction().equals(holder) && faith(level, holder) != faith;
   }

   /** Points of order a town's faith gives or takes: its holy site held or lost, heresy in it. */
   static int order(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement) {
      String faction = settlement.faction();
      if (FactionDiplomacy.isOutlaw(faction)) {
         return 0;
      }

      int faith = faith(level, faction);
      FactionWorldData.Settlement site = holySite(level, data, faith);
      int order = 0;
      if (site != null) {
         order += site.faction().equals(faction) ? 5 : faith(level, site.faction()) != faith ? -5 : 0;
      }

      Heresy heresy = get(level).heresies.get(faction);
      if (heresy != null && heresy.towns.contains(settlement.id())) {
         order -= heresy.answer == 1 ? 10 : 3;
      }

      return order;
   }

   /** Times the usual chance of war between two factions: holy war over a holy site, kinship in faith. */
   static double warFactor(ServerLevel level, String faction, String other) {
      if (FactionDiplomacy.isOutlaw(faction) || FactionDiplomacy.isOutlaw(other)) {
         return 1.0;
      }

      FactionWorldData data = FactionWorldData.get(level);
      int mine = faith(level, faction);
      int theirs = faith(level, other);
      if (holdsAsInfidel(level, data, other, mine) || holdsAsInfidel(level, data, faction, theirs)) {
         // A priest who preaches war fans it.
         return FactionCourt.priestTrait(level, faction) == FactionRulers.Trait.WARLIKE || FactionCourt.priestTrait(level, other) == FactionRulers.Trait.WARLIKE ? 3.0 : 2.5;
      }

      return mine == theirs ? 0.8 : 1.1;
   }

   /** Whether a war the faction declares on the other is a holy war, for the other holds its holy site. */
   static boolean holyWar(ServerLevel level, String faction, String other) {
      return !FactionDiplomacy.isOutlaw(other) && holdsAsInfidel(level, FactionWorldData.get(level), other, faith(level, faction));
   }

   /** Times the usual chance the ruler sets out on pilgrimage: not while unbelievers or enemies hold the holy site. */
   static double pilgrimFactor(ServerLevel level, String faction) {
      FactionWorldData data = FactionWorldData.get(level);
      int faith = faith(level, faction);
      FactionWorldData.Settlement site = holySite(level, data, faith);
      if (site == null) {
         return 1.0;
      } else if (site.faction().equals(faction)) {
         return 1.5;
      }

      boolean barred = faith(level, site.faction()) != faith || FactionDiplomacy.relation(level, faction, site.faction()).state() == RelationState.WAR;
      return barred ? 0.0 : 1.0;
   }

   // ---------------------------------------------------------------- days

   static void simulateDay(ServerLevel level, FactionWorldData data) {
      FactionFaith faiths = get(level);
      RandomSource random = level.getRandom();
      long day = Math.max(0L, data.lastSimulatedDay());
      Set<String> factions = new TreeSet<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!FactionDiplomacy.isOutlaw(settlement.faction())) {
            factions.add(settlement.faction());
         }
      }

      // Kinship in faith, and grievance against whoever holds one's holy site.
      for (String faction : factions) {
         for (String other : factions) {
            if (faction.compareTo(other) < 0 && faith(level, faction) == faith(level, other) && random.nextDouble() < 0.2) {
               FactionDiplomacy.adjustOpinion(level, faction, other, 1);
               FactionDiplomacy.adjustOpinion(level, other, faction, 1);
            }

            if (!faction.equals(other) && holdsAsInfidel(level, data, other, faith(level, faction)) && random.nextDouble() < 0.5) {
               FactionDiplomacy.adjustOpinion(level, faction, other, -1);
            }
         }
      }

      faiths.feasting.clear();
      for (int faith = 0; faith < KEYS.length; faith++) {
         if (Math.floorMod(day, FactionSeasons.SEASON_DAYS * 4L) == (long)faith * FactionSeasons.SEASON_DAYS + FEAST_DAY) {
            faiths.feast(level, data, faith, factions);
         }
      }

      for (String faction : factions) {
         if (!faiths.heresies.containsKey(faction)) {
            faiths.maybeHeresy(level, data, faction, day);
         }
      }

      for (Heresy heresy : new ArrayList<>(faiths.heresies.values())) {
         faiths.tend(level, data, heresy, day);
      }

      faiths.setDirty();
   }

   // ---------------------------------------------------------------- feasts

   private void feast(ServerLevel level, FactionWorldData data, int faith, Set<String> factions) {
      List<String> faithful = new ArrayList<>();
      for (String faction : factions) {
         if (faith(level, faction) == faith && !FactionRebellions.inCivilWar(level, faction)) {
            faithful.add(faction);
         }
      }

      if (faithful.isEmpty()) {
         return;
      }

      this.feasting.add(faith);
      this.offerings.put(faith, new HashSet<>());
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         String faction = settlement.faction();
         if (!faithful.contains(faction) || settlement.famine() > 0) {
            continue;
         }

         // A pious priest keeps the feast with more splendour.
         int bonus = FactionPeople.has(FactionCourt.character(level, faction, FactionCourt.Role.PRIEST), FactionPeople.Character.PIOUS) ? 5 : 0;
         settlement.order = Math.min(100, settlement.order + FEAST_ORDER + bonus);
         FactionRebellions.stir(level, settlement.id(), FEAST_CALM);
         data.setTreasury(faction, Math.max(0, data.treasury(faction) - 3));
      }

      data.setDirty();
      MutableComponent names = Component.empty();
      for (int i = 0; i < faithful.size(); i++) {
         names.append(i == 0 ? Component.empty() : Component.literal(", ")).append(FactionNews.factionName(faithful.get(i)));
      }

      AncientWarfareCore.LOG.info("Feast of faith {} kept by {}", KEYS[faith], faithful);
      FactionNews.tell(
         level, Component.translatable("news.ancientwarfare.feast", Component.translatable("faith.ancientwarfare.feast." + KEYS[faith]), name(faith), names).withStyle(ChatFormatting.LIGHT_PURPLE)
      );
      FactionStandingData standings = FactionStandingData.get(level);
      for (ServerPlayer player : level.players()) {
         if (faithful.stream().anyMatch(faction -> standings.knownFactions(player.getUUID()).contains(faction))) {
            player.sendSystemMessage(
               Component.translatable("faith.ancientwarfare.offer", OFFERING_COST)
                  .withStyle(ChatFormatting.GRAY)
                  .append(" ")
                  .append(button(Component.translatable("faith.ancientwarfare.offering"), "/awoffering " + KEYS[faith], ChatFormatting.LIGHT_PURPLE))
            );
         }
      }
   }

   /** A player brings an offering to a faith's feast: every faction of that faith that knows them thinks better of them. */
   public static void offer(ServerPlayer player, String key) {
      ServerLevel level = player.serverLevel();
      FactionFaith faiths = get(level);
      int faith = List.of(KEYS).indexOf(key);
      Set<UUID> given = faith < 0 ? null : faiths.offerings.get(faith);
      if (given == null || !faiths.feasting.contains(faith)) {
         player.sendSystemMessage(Component.translatable("faith.ancientwarfare.no_feast").withStyle(ChatFormatting.GRAY));
         return;
      } else if (given.contains(player.getUUID())) {
         player.sendSystemMessage(Component.translatable("faith.ancientwarfare.already").withStyle(ChatFormatting.GRAY));
         return;
      } else if (!PlayerDiplomacy.takeEmeralds(player, OFFERING_COST)) {
         player.sendSystemMessage(Component.translatable("diplomacy.ancientwarfare.gift_missing", OFFERING_COST).withStyle(ChatFormatting.RED));
         return;
      }

      given.add(player.getUUID());
      faiths.setDirty();
      FactionStandingData standings = FactionStandingData.get(level);
      FactionWorldData data = FactionWorldData.get(level);
      int count = 0;
      for (String faction : standings.knownFactions(player.getUUID())) {
         if (!FactionDiplomacy.isOutlaw(faction) && faith(level, faction) == faith) {
            boolean pious = FactionPeople.has(FactionRulers.ruler(data, faction, level.getRandom()).character(), FactionPeople.Character.PIOUS);
            standings.adjustStanding(player.getUUID(), faction, OFFERING_STANDING + (pious ? 2 : 0));
            count++;
         }
      }

      player.sendSystemMessage(Component.translatable("faith.ancientwarfare.offered", name(faith), count).withStyle(ChatFormatting.GREEN));
   }

   // ---------------------------------------------------------------- heresies

   /** In a restless realm with a lax or greedy priest, a town turns toward a neighbour's creed. */
   private void maybeHeresy(ServerLevel level, FactionWorldData data, String faction, long day) {
      RandomSource random = level.getRandom();
      List<FactionWorldData.Settlement> towns = towns(data, faction);
      if (towns.size() < 3) {
         return;
      }

      FactionWorldData.Settlement worst = null;
      int total = 0;
      for (FactionWorldData.Settlement town : towns) {
         int discontent = FactionRebellions.discontent(level, town.id());
         total += discontent;
         if (!FactionTowns.CAPITAL.equals(town.kind()) && (worst == null || discontent > FactionRebellions.discontent(level, worst.id()))) {
            worst = town;
         }
      }

      double chance = HERESY_CHANCE * (1.0 + total / (double)towns.size() / 15.0);
      int priest = FactionCourt.character(level, faction, FactionCourt.Role.PRIEST);
      chance *= FactionCourt.priestTrait(level, faction) == FactionRulers.Trait.GREEDY || FactionPeople.has(priest, FactionPeople.Character.DECEITFUL) ? 2.0 : 1.0;
      chance *= FactionPeople.has(priest, FactionPeople.Character.PIOUS) ? 0.5 : 1.0;
      chance *= FactionPeople.has(FactionRulers.ruler(data, faction, random).character(), FactionPeople.Character.PIOUS) ? 0.5 : 1.0;
      if (worst == null || random.nextDouble() >= chance) {
         return;
      }

      int creed = nearestOtherFaith(level, data, faction, worst);
      if (creed < 0) {
         return;
      }

      this.startHeresy(level, faction, worst, creed, day);
   }

   private void startHeresy(ServerLevel level, String faction, FactionWorldData.Settlement town, int creed, long day) {
      Heresy heresy = new Heresy(faction, creed, day);
      heresy.towns.add(town.id());
      this.heresies.put(faction, heresy);
      this.setDirty();
      AncientWarfareCore.LOG.info("Heresy in {} at #{} leaning to {}", faction, town.id(), KEYS[creed]);
      FactionNews.tell(level, Component.translatable("news.ancientwarfare.heresy", FactionNews.factionName(faction), FactionNews.named(town), name(creed)).withStyle(ChatFormatting.LIGHT_PURPLE));
   }

   /** The faith of the nearest settlement of another faith, or -1. */
   private static int nearestOtherFaith(ServerLevel level, FactionWorldData data, String faction, FactionWorldData.Settlement town) {
      int mine = faith(level, faction);
      int best = -1;
      double bestDistance = Double.MAX_VALUE;
      for (FactionWorldData.Settlement other : data.settlements()) {
         if (FactionDiplomacy.isOutlaw(other.faction()) || faith(level, other.faction()) == mine) {
            continue;
         }

         double distance = other.horizontalDistanceSquared(town.center().getX(), town.center().getZ());
         if (distance < bestDistance) {
            bestDistance = distance;
            best = faith(level, other.faction());
         }
      }

      return best;
   }

   private static List<FactionWorldData.Settlement> towns(FactionWorldData data, String faction) {
      List<FactionWorldData.Settlement> towns = new ArrayList<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.faction().equals(faction) && !FactionAI.isFort(settlement)) {
            towns.add(settlement);
         }
      }

      return towns;
   }

   /** A day of heresy: the ruler answers it, it spreads or is rooted out, and it may win the realm. */
   private void tend(ServerLevel level, FactionWorldData data, Heresy heresy, long day) {
      RandomSource random = level.getRandom();
      List<FactionWorldData.Settlement> towns = towns(data, heresy.faction);
      heresy.towns.removeIf(id -> towns.stream().noneMatch(town -> town.id() == id));
      if (towns.isEmpty() || faith(level, heresy.faction) == heresy.faith) {
         this.heresies.remove(heresy.faction);
         return;
      }

      Component faction = FactionNews.factionName(heresy.faction);
      if (heresy.answer == 0 && day > heresy.day) {
         FactionWorldData.Ruler ruler = FactionRulers.ruler(data, heresy.faction, random);
         boolean zealous = FactionPeople.has(ruler.character(), FactionPeople.Character.PIOUS)
            || FactionPeople.has(ruler.character(), FactionPeople.Character.CRUEL)
            || FactionPeople.has(ruler.character(), FactionPeople.Character.PARANOID)
            || FactionPeople.has(FactionCourt.character(level, heresy.faction, FactionCourt.Role.PRIEST), FactionPeople.Character.PIOUS);
         heresy.answer = zealous ? 1 : 2;
         FactionNews.tell(
            level, Component.translatable(zealous ? "news.ancientwarfare.heresy_persecuted" : "news.ancientwarfare.heresy_tolerated", faction, FactionRulers.name(ruler.name()))
         );
      }

      // It spreads to the town nearest one that holds to it.
      if (random.nextDouble() < (heresy.answer == 1 ? 0.2 : heresy.answer == 2 ? 0.35 : 0.3)) {
         FactionWorldData.Settlement next = null;
         double best = Double.MAX_VALUE;
         for (FactionWorldData.Settlement town : towns) {
            if (heresy.towns.contains(town.id())) {
               continue;
            }

            for (int id : heresy.towns) {
               FactionWorldData.Settlement held = data.settlement(id);
               double distance = held == null ? Double.MAX_VALUE : town.horizontalDistanceSquared(held.center().getX(), held.center().getZ());
               if (distance < best) {
                  best = distance;
                  next = town;
               }
            }
         }

         if (next != null) {
            heresy.towns.add(next.id());
         }
      }

      if (heresy.answer == 1 && !heresy.towns.isEmpty()) {
         // The persecution roots it out a town at a time and leaves bitterness behind.
         double purge = 0.5 + (FactionPeople.has(FactionCourt.character(level, heresy.faction, FactionCourt.Role.PRIEST), FactionPeople.Character.PIOUS) ? 0.2 : 0.0);
         if (random.nextDouble() < purge) {
            List<Integer> held = new ArrayList<>(heresy.towns);
            int id = held.get(random.nextInt(held.size()));
            heresy.towns.remove(id);
            FactionRebellions.stir(level, id, PERSECUTED_DISCONTENT);
         }
      }

      if (heresy.towns.isEmpty()) {
         this.heresies.remove(heresy.faction);
         FactionNews.tell(level, Component.translatable("news.ancientwarfare.heresy_ended", faction));
         return;
      }

      if (heresy.towns.size() >= 3 && heresy.towns.size() * 2 >= towns.size()) {
         this.heresies.remove(heresy.faction);
         if (heresy.answer == 1) {
            // Persecuted until half the realm holds to it: a war of faith.
            FactionCourt.Role role = FactionPeople.has(FactionRulers.ruler(data, heresy.faction, random).character(), FactionPeople.Character.PIOUS)
               ? FactionCourt.Role.HEIR
               : FactionCourt.Role.PRIEST;
            if (!FactionRebellions.religiousWar(level, data, heresy.faction, role, heresy.towns, heresy.faith)) {
               convert(level, heresy.faction, heresy.faith);
            }
         } else {
            convert(level, heresy.faction, heresy.faith);
         }

         return;
      }

      if (heresy.answer == 2 && day >= heresy.day + HERESY_FADES) {
         this.heresies.remove(heresy.faction);
         FactionNews.tell(level, Component.translatable("news.ancientwarfare.heresy_faded", faction));
      }
   }

   // ---------------------------------------------------------------- view

   /** The overview's lines: the faith and its holy site, and a heresy. */
   static List<Component> lines(ServerLevel level, String faction) {
      List<Component> lines = new ArrayList<>();
      if (FactionDiplomacy.isOutlaw(faction)) {
         return lines;
      }

      FactionWorldData data = FactionWorldData.get(level);
      int faith = faith(level, faction);
      FactionWorldData.Settlement site = holySite(level, data, faith);
      if (site == null) {
         lines.add(Component.translatable("faith.ancientwarfare.line", name(faith)).withStyle(ChatFormatting.LIGHT_PURPLE));
      } else if (site.faction().equals(faction)) {
         lines.add(Component.translatable("faith.ancientwarfare.line.own", name(faith), FactionNews.named(site)).withStyle(ChatFormatting.LIGHT_PURPLE));
      } else if (faith(level, site.faction()) == faith) {
         lines.add(Component.translatable("faith.ancientwarfare.line.kin", name(faith), FactionNews.named(site), FactionNews.factionName(site.faction())).withStyle(ChatFormatting.LIGHT_PURPLE));
      } else {
         lines.add(Component.translatable("faith.ancientwarfare.line.lost", name(faith), FactionNews.named(site), FactionNews.factionName(site.faction())).withStyle(ChatFormatting.RED));
      }

      Heresy heresy = get(level).heresies.get(faction);
      if (heresy != null) {
         String key = heresy.answer == 1 ? "faith.ancientwarfare.heresy_line.persecuted" : heresy.answer == 2 ? "faith.ancientwarfare.heresy_line.tolerated" : "faith.ancientwarfare.heresy_line";
         lines.add(Component.translatable(key, name(heresy.faith), heresy.towns.size()).withStyle(ChatFormatting.GOLD));
      }

      return lines;
   }

   /** For the world map: each faith's holy site, as [settlement id, faith]. */
   static List<int[]> holySites(ServerLevel level, FactionWorldData data) {
      List<int[]> sites = new ArrayList<>();
      for (int faith = 0; faith < KEYS.length; faith++) {
         FactionWorldData.Settlement site = holySite(level, data, faith);
         if (site != null) {
            sites.add(new int[]{site.id(), faith});
         }
      }

      return sites;
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

   /** Admin: "feast" keeps the faction's feast now, "heresy" starts one, "tend" runs a heresy day, "convert:N", "list". */
   public static String debug(ServerLevel level, String faction, String what) {
      FactionWorldData data = FactionWorldData.get(level);
      FactionFaith faiths = get(level);
      long day = Math.max(0L, data.lastSimulatedDay());
      if (what.equals("feast")) {
         Set<String> factions = new TreeSet<>();
         data.settlements().forEach(settlement -> factions.add(settlement.faction()));
         faiths.feast(level, data, faith(level, faction), factions);
         faiths.setDirty();
         return "Feast of " + KEYS[faith(level, faction)];
      } else if (what.equals("heresy")) {
         List<FactionWorldData.Settlement> towns = towns(data, faction);
         FactionWorldData.Settlement town = towns.stream().filter(t -> !FactionTowns.CAPITAL.equals(t.kind())).findFirst().orElse(null);
         int creed = town == null ? -1 : nearestOtherFaith(level, data, faction, town);
         if (creed < 0) {
            return "No heresy";
         }

         faiths.startHeresy(level, faction, town, creed, day);
         return "Heresy at #" + town.id() + " to " + KEYS[creed];
      } else if (what.startsWith("tend")) {
         Heresy heresy = faiths.heresies.get(faction);
         if (heresy == null) {
            return "No heresy";
         }

         if (what.equals("tend_half")) {
            // Spreads it over two thirds of the realm at once.
            List<FactionWorldData.Settlement> towns = towns(data, faction);
            for (int i = 0; i < towns.size() * 2 / 3 + 1; i++) {
               heresy.towns.add(towns.get(i).id());
            }
         }

         faiths.tend(level, data, heresy, day + 1);
         faiths.setDirty();
         Heresy now = faiths.heresies.get(faction);
         return now == null ? "Heresy over" : "Heresy in " + now.towns.size() + " towns, answer " + now.answer;
      } else if (what.startsWith("convert:")) {
         convert(level, faction, Mth.clamp(Integer.parseInt(what.substring(8)), 0, KEYS.length - 1));
         return "Converted";
      }

      StringBuilder out = new StringBuilder();
      for (int faith = 0; faith < KEYS.length; faith++) {
         FactionWorldData.Settlement site = holySite(level, data, faith);
         out.append(KEYS[faith]).append(": site ").append(site == null ? "none" : "#" + site.id() + " " + site.faction() + " " + site.name()).append("; ");
      }

      out.append(faction).append(" faith ").append(KEYS[faith(level, faction)]);
      Heresy heresy = faiths.heresies.get(faction);
      if (heresy != null) {
         out.append(", heresy to ").append(KEYS[heresy.faith]).append(" in ").append(heresy.towns.size()).append(" answer ").append(heresy.answer);
      }

      return out.toString();
   }

   // ---------------------------------------------------------------- saving

   private static FactionFaith load(CompoundTag tag) {
      FactionFaith data = new FactionFaith();
      CompoundTag converted = tag.getCompound("Converted");
      for (String key : converted.getAllKeys()) {
         data.converted.put(key, Mth.clamp(converted.getInt(key), 0, KEYS.length - 1));
      }

      int[] sites = tag.getIntArray("HolySites");
      for (int i = 0; i < Math.min(sites.length, data.holySites.length); i++) {
         data.holySites[i] = sites[i];
      }

      for (Tag element : tag.getList("Heresies", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         Heresy heresy = new Heresy(entry.getString("Faction"), Mth.clamp(entry.getInt("Faith"), 0, KEYS.length - 1), entry.getLong("Day"));
         heresy.answer = entry.getInt("Answer");
         for (int id : entry.getIntArray("Towns")) {
            heresy.towns.add(id);
         }

         data.heresies.put(heresy.faction, heresy);
      }

      CompoundTag offerings = tag.getCompound("Offerings");
      for (String key : offerings.getAllKeys()) {
         Set<UUID> players = new HashSet<>();
         for (Tag element : offerings.getList(key, Tag.TAG_STRING)) {
            try {
               players.add(UUID.fromString(element.getAsString()));
            } catch (IllegalArgumentException ignored) {
            }
         }

         try {
            data.offerings.put(Integer.parseInt(key), players);
         } catch (NumberFormatException ignored) {
         }
      }

      for (int faith : tag.getIntArray("Feasting")) {
         data.feasting.add(faith);
      }

      return data;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      CompoundTag converted = new CompoundTag();
      this.converted.forEach(converted::putInt);
      tag.put("Converted", converted);
      tag.put("HolySites", new IntArrayTag(this.holySites.clone()));
      ListTag heresies = new ListTag();
      for (Heresy heresy : this.heresies.values()) {
         CompoundTag entry = new CompoundTag();
         entry.putString("Faction", heresy.faction);
         entry.putInt("Faith", heresy.faith);
         entry.putLong("Day", heresy.day);
         entry.putInt("Answer", heresy.answer);
         entry.put("Towns", new IntArrayTag(heresy.towns.stream().mapToInt(Integer::intValue).toArray()));
         heresies.add(entry);
      }

      tag.put("Heresies", heresies);
      CompoundTag offerings = new CompoundTag();
      this.offerings.forEach((faith, players) -> {
         ListTag list = new ListTag();
         players.forEach(id -> list.add(StringTag.valueOf(id.toString())));
         offerings.put(Integer.toString(faith), list);
      });
      tag.put("Offerings", offerings);
      tag.put("Feasting", new IntArrayTag(this.feasting.stream().mapToInt(Integer::intValue).toArray()));
      return tag;
   }
}
