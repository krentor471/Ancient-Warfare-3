package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;

/**
 * What a settlement builds as it prospers, and the purse it builds from. Every settlement, seen or
 * not, keeps a purse: its people's trade and crafts fill it, its garrison's pay empties it. A
 * settlement at peace with money in hand decides on its next building by what it lacks most (room
 * for its people, food, faith, a wall) and pays for it; from then on the building counts, on paper,
 * for room, food, health and order. When a player is about, the building is put up for real; see
 * FactionGrowth. A village builds up to thirty buildings, a town forty, a city or capital fifty.
 */
public final class TownBuildings {
   /** Buildings decided on and paid for, not yet put up, a settlement may have waiting at once. */
   static final int MAX_WAITING = 4;
   private static final int VILLAGE_MAX = 30;
   private static final int TOWN_MAX = 40;
   private static final int CITY_MAX = 50;
   private static final int GARRISON_PAY = 4;
   private static final int MAX_WEALTH = 5000;

   public enum Group {
      HOMES,
      FOOD,
      CRAFT,
      TRADE,
      FAITH,
      LEARNING,
      LEISURE,
      DEFENCE,
      RULE
   }

   /**
    * A kind of building. words name a faction's own templates for it, vanilla the village piece it
    * may be taken from, piece the plain building put up when neither is to hand.
    */
   public enum Kind {
      HOUSE(Group.HOMES, 10, 0, 99, "", "small_house", FactionGrowth.Piece.HOUSE, 4, 0, 0, 0, 0, 0),
      COTTAGE(Group.HOMES, 16, 0, 99, "cottage", "small_house", FactionGrowth.Piece.HOUSE, 5, 0, 0, 0, 0, 0),
      FAMILY_HOUSE(Group.HOMES, 24, 1, 99, "house", "medium_house", FactionGrowth.Piece.HOUSE, 8, 0, 0, 0, 1, 0),
      MANOR(Group.HOMES, 60, 2, 3, "manor|villa|estate", "big_house", FactionGrowth.Piece.HALL, 10, 0, 0, 1, 2, 0),
      WELL(Group.HOMES, 8, 0, 3, "well", "", FactionGrowth.Piece.WELL, 0, 0, 1, 0, 0, 0),
      LAMP(Group.HOMES, 3, 0, 8, "", "", FactionGrowth.Piece.LAMP, 0, 0, 0, 1, 0, 0),
      SQUARE(Group.HOMES, 20, 1, 1, "plaza|square|fountain", "meeting_point", FactionGrowth.Piece.WELL, 0, 0, 0, 1, 1, 0),
      GARDEN(Group.FOOD, 6, 0, 6, "", "", FactionGrowth.Piece.GARDEN, 0, 2, 0, 0, 0, 0),
      FIELD(Group.FOOD, 10, 0, 6, "farm|field", "small_farm", FactionGrowth.Piece.FIELD, 0, 4, 0, 0, 0, 0),
      LARGE_FARM(Group.FOOD, 22, 1, 3, "", "large_farm", FactionGrowth.Piece.FIELD, 0, 8, 0, 0, 0, 0),
      ANIMAL_PEN(Group.FOOD, 14, 0, 3, "pen|pasture|corral", "animal_pen", FactionGrowth.Piece.PEN, 0, 4, 0, 0, 0, 0),
      BUTCHER(Group.FOOD, 24, 0, 1, "butcher", "butcher", FactionGrowth.Piece.HALL, 0, 5, 0, 0, 1, 0),
      FISHERY(Group.FOOD, 20, 0, 1, "fish", "fisher", FactionGrowth.Piece.HALL, 0, 5, 0, 0, 1, 0),
      MILL(Group.FOOD, 30, 1, 1, "mill", "", FactionGrowth.Piece.HALL, 0, 6, 0, 0, 1, 0),
      BAKERY(Group.FOOD, 26, 1, 1, "bakery|baker", "", FactionGrowth.Piece.HALL, 0, 5, 0, 1, 1, 0),
      BREWERY(Group.FOOD, 28, 1, 1, "brew", "", FactionGrowth.Piece.HALL, 0, 1, 0, 1, 2, 0),
      GRANARY(Group.FOOD, 30, 1, 1, "granary|barn", "", FactionGrowth.Piece.HALL, 0, 6, 0, 0, 0, 0),
      SMITHY(Group.CRAFT, 30, 0, 1, "smith|forge", "tool_smith", FactionGrowth.Piece.HALL, 0, 0, 0, 0, 2, 0),
      WEAPONSMITH(Group.CRAFT, 34, 1, 1, "weapon", "weaponsmith", FactionGrowth.Piece.HALL, 0, 0, 0, 0, 2, 1),
      ARMORER(Group.CRAFT, 34, 1, 1, "armor|armour", "armorer", FactionGrowth.Piece.HALL, 0, 0, 0, 0, 2, 1),
      FLETCHER(Group.CRAFT, 22, 0, 1, "fletch|bow", "fletcher", FactionGrowth.Piece.HALL, 0, 0, 0, 0, 1, 1),
      MASON(Group.CRAFT, 26, 0, 1, "mason|quarry", "mason", FactionGrowth.Piece.HALL, 0, 0, 0, 0, 2, 0),
      TANNERY(Group.CRAFT, 22, 0, 1, "tanner", "tannery", FactionGrowth.Piece.HALL, 0, 0, 0, 0, 2, 0),
      WEAVER(Group.CRAFT, 22, 0, 1, "weav|shepherd|wool", "shepherd", FactionGrowth.Piece.HALL, 0, 0, 0, 0, 2, 0),
      MINE(Group.CRAFT, 40, 0, 2, "", "", FactionGrowth.Piece.MINE, 0, 0, 0, 0, 2, 0),
      CARPENTER(Group.CRAFT, 24, 0, 1, "carpent|lumber|saw", "", FactionGrowth.Piece.HALL, 0, 0, 0, 0, 2, 0),
      WORKSHOP(Group.CRAFT, 30, 1, 1, "workshop", "", FactionGrowth.Piece.HALL, 0, 0, 0, 0, 3, 0),
      STALL(Group.TRADE, 8, 0, 6, "stall", "", FactionGrowth.Piece.STALL, 0, 0, 0, 0, 1, 0),
      MARKET(Group.TRADE, 40, 1, 1, "market", "", FactionGrowth.Piece.HALL, 0, 1, 0, 1, 4, 0),
      WAREHOUSE(Group.TRADE, 30, 1, 1, "warehouse|storage", "", FactionGrowth.Piece.HALL, 0, 2, 0, 0, 2, 0),
      CARAVANSERAI(Group.TRADE, 45, 2, 1, "caravan", "", FactionGrowth.Piece.HALL, 2, 0, 0, 0, 4, 0),
      GUILD(Group.TRADE, 50, 1, 1, "guild", "", FactionGrowth.Piece.HALL, 0, 0, 0, 1, 4, 0),
      SHRINE(Group.FAITH, 10, 0, 2, "shrine", "", FactionGrowth.Piece.SHRINE, 0, 0, 1, 1, 0, 0),
      TEMPLE(Group.FAITH, 45, 1, 1, "temple|church|chapel", "temple", FactionGrowth.Piece.HALL, 0, 0, 2, 2, 0, 0),
      MONASTERY(Group.FAITH, 70, 2, 1, "monast|abbey", "", FactionGrowth.Piece.HALL, 2, 1, 2, 2, 0, 0),
      LIBRARY(Group.LEARNING, 40, 1, 1, "library", "library", FactionGrowth.Piece.HALL, 0, 0, 0, 1, 1, 0),
      CARTOGRAPHER(Group.LEARNING, 30, 1, 1, "cartograph|map", "cartographer", FactionGrowth.Piece.HALL, 0, 0, 0, 0, 2, 0),
      ALCHEMIST(Group.LEARNING, 36, 1, 1, "alchem|potion|apothec", "", FactionGrowth.Piece.HALL, 0, 0, 2, 0, 2, 0),
      SCHOOL(Group.LEARNING, 40, 2, 1, "school|academy", "", FactionGrowth.Piece.HALL, 0, 0, 0, 1, 1, 0),
      TAVERN(Group.LEISURE, 30, 0, 1, "tavern|inn", "", FactionGrowth.Piece.HALL, 2, 0, 0, 1, 2, 0),
      BATHHOUSE(Group.LEISURE, 40, 2, 1, "bath", "", FactionGrowth.Piece.HALL, 0, 0, 3, 1, 1, 0),
      ARENA(Group.LEISURE, 70, 2, 1, "arena", "", FactionGrowth.Piece.HALL, 0, 0, 0, 2, 2, 1),
      PALISADE(Group.DEFENCE, 12, 0, 8, "", "", FactionGrowth.Piece.PALISADE, 0, 0, 0, 1, 0, 1),
      WATCH_POST(Group.DEFENCE, 14, 0, 4, "watch|lookout", "", FactionGrowth.Piece.WATCH_POST, 0, 0, 0, 1, 0, 2),
      TOWER(Group.DEFENCE, 30, 2, 4, "tower", "", FactionGrowth.Piece.TOWER, 0, 0, 0, 1, 0, 3),
      GATEHOUSE(Group.DEFENCE, 40, 1, 2, "gate", "", FactionGrowth.Piece.GATEHOUSE, 0, 0, 0, 1, 0, 3),
      BARRACKS(Group.DEFENCE, 45, 1, 1, "barrack", "", FactionGrowth.Piece.HALL, 4, 0, 0, 2, 0, 3),
      ARCHERY(Group.DEFENCE, 26, 1, 1, "archer|range", "", FactionGrowth.Piece.HALL, 0, 0, 0, 0, 0, 2),
      STABLES(Group.DEFENCE, 30, 0, 1, "stable", "stable", FactionGrowth.Piece.HALL, 0, 0, 0, 0, 1, 1),
      ARMORY(Group.DEFENCE, 40, 2, 1, "armory|armoury|arsenal", "", FactionGrowth.Piece.HALL, 0, 0, 0, 1, 0, 2),
      TOWN_HALL(Group.RULE, 60, 1, 1, "town_hall|council|townhall", "", FactionGrowth.Piece.HALL, 0, 0, 0, 3, 2, 0);

      public final Group group;
      final int cost;
      /** The least rank (0 village, 1 town, 2 city) a settlement needs to build it. */
      final int rank;
      /** The most of it one settlement builds. */
      final int most;
      final String[] words;
      final String vanilla;
      final FactionGrowth.Piece piece;
      final int room;
      /** Rations a day it adds to the settlement's stores. */
      final int food;
      final int health;
      final int order;
      /** Silver a day it brings into the settlement's purse. */
      final int income;
      final int defence;

      Kind(Group group, int cost, int rank, int most, String words, String vanilla, FactionGrowth.Piece piece, int room, int food, int health, int order, int income, int defence) {
         this.group = group;
         this.cost = cost;
         this.rank = rank;
         this.most = most;
         this.words = words.isEmpty() ? new String[0] : words.split("\\|");
         this.vanilla = vanilla;
         this.piece = piece;
         this.room = room;
         this.food = food;
         this.health = health;
         this.order = order;
         this.income = income;
         this.defence = defence;
      }

      public String key() {
         return this.name().toLowerCase(Locale.ROOT);
      }

      public Component title() {
         return Component.translatable("building.ancientwarfare." + this.key());
      }

      boolean names(String name) {
         String lower = name.toLowerCase(Locale.ROOT);
         for (String word : this.words) {
            if (lower.contains(word)) {
               return true;
            }
         }

         return false;
      }

      static Kind byKey(String key) {
         for (Kind kind : values()) {
            if (kind.key().equals(key)) {
               return kind;
            }
         }

         return null;
      }
   }

   private TownBuildings() {
   }

   /** The most buildings a settlement of its rank and kind builds in all. */
   static int most(FactionWorldData.Settlement settlement) {
      if (FactionTowns.CAPITAL.equals(settlement.kind()) || settlement.tier() >= FactionPorts.CITY) {
         return CITY_MAX;
      }

      return settlement.tier() >= 1 || "town".equals(settlement.kind()) ? TOWN_MAX : VILLAGE_MAX;
   }

   private static int rank(FactionWorldData.Settlement settlement) {
      return FactionTowns.CAPITAL.equals(settlement.kind()) ? FactionPorts.CITY : Math.max(settlement.tier(), "town".equals(settlement.kind()) ? 1 : 0);
   }

   /** Every building the settlement has, standing or on paper. */
   static List<Kind> all(FactionWorldData.Settlement settlement) {
      List<Kind> kinds = new ArrayList<>();
      for (String key : settlement.buildings()) {
         Kind kind = Kind.byKey(key);
         if (kind != null) {
            kinds.add(kind);
         }
      }

      for (String key : settlement.plan()) {
         Kind kind = Kind.byKey(key);
         if (kind != null) {
            kinds.add(kind);
         }
      }

      return kinds;
   }

   private static int count(List<Kind> kinds, Kind kind) {
      int count = 0;
      for (Kind other : kinds) {
         if (other == kind) {
            count++;
         }
      }

      return count;
   }

   /** Room for people its buildings, standing or on paper, give a settlement besides those it was built with. */
   static int room(FactionWorldData.Settlement settlement) {
      int room = 0;
      for (Kind kind : all(settlement)) {
         room += kind.room;
      }

      // Pieces from before buildings had kinds were each a house's worth.
      return room + Math.max(0, settlement.grown() - settlement.buildings().size()) * FactionGrowth.ROOM_PER_PIECE;
   }

   public static int defence(FactionWorldData.Settlement settlement) {
      int defence = 0;
      for (Kind kind : all(settlement)) {
         defence += kind.defence;
      }

      return defence;
   }

   /** All its buildings, standing or on paper, counted. */
   public static int total(FactionWorldData.Settlement settlement) {
      return Math.max(settlement.grown(), settlement.buildings().size()) + settlement.plan().size();
   }

   /**
    * One day of every settlement's purse and buildings: income in, the garrison paid, the buildings'
    * yield of food, health and order, and, for one at peace with money enough, the next building.
    */
   static void simulateDay(ServerLevel level, FactionWorldData data) {
      RandomSource random = level.getRandom();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (FactionDiplomacy.isOutlaw(settlement.faction()) || FactionAI.isFort(settlement)) {
            continue;
         }

         List<Kind> kinds = all(settlement);
         int income = settlement.population() / 5 + rank(settlement) * 2;
         int food = 0;
         int health = 0;
         int order = 0;
         for (Kind kind : kinds) {
            income += kind.income;
            food += kind.food;
            health += kind.health;
            order += kind.order;
         }

         settlement.wealth = Mth.clamp(settlement.wealth + income - settlement.garrison() / GARRISON_PAY, 0, MAX_WEALTH);
         yieldGoods(data.stores(settlement.faction()), kinds);
         if (settlement.food >= 0) {
            settlement.food += food;
         }

         // The buildings hold health and order up toward a level they set; they lift, they do not cap.
         if (settlement.health < 60 + Math.min(40, health * 5) && random.nextInt(4) < Math.min(3, 1 + health / 3)) {
            settlement.health = Math.min(100, settlement.health + 1);
         }

         if (settlement.order < 60 + Math.min(40, order * 3) && random.nextInt(4) < Math.min(3, 1 + order / 4)) {
            settlement.order = Math.min(100, settlement.order + 1);
         }

         if (settlement.plan().size() < MAX_WAITING && total(settlement) < most(settlement) && peaceful(data, settlement)) {
            Kind next = choose(settlement, kinds, random);
            if (next != null && settlement.wealth >= next.cost) {
               settlement.wealth -= next.cost;
               settlement.plan().add(next.key());
            }
         }
      }

      data.setDirty();
   }

   /** What the settlement's mines and workshops send to the faction's stores each day. */
   private static void yieldGoods(int[] stores, List<Kind> kinds) {
      for (Kind kind : kinds) {
         switch (kind) {
            case MINE -> {
               stores[FactionResources.STONE] += 3;
               stores[FactionResources.IRON] += 2;
               stores[FactionResources.COAL] += 2;
               stores[FactionResources.COPPER] += 1;
            }
            case MASON -> stores[FactionResources.STONE] += 2;
            case CARPENTER -> stores[FactionResources.WOOD] += 2;
            case TANNERY -> stores[FactionResources.LEATHER] += 1;
            case WEAVER -> stores[FactionResources.WOOL] += 1;
            case FIELD, LARGE_FARM, MILL, GRANARY -> stores[FactionResources.GRAIN] += 1;
            default -> {
            }
         }
      }
   }

   private static boolean peaceful(FactionWorldData data, FactionWorldData.Settlement settlement) {
      return settlement.famine() == 0 && settlement.plague() == 0 && settlement.order() >= 35 && !FactionTownLife.underAttack(data, settlement);
   }

   /** The building the settlement needs most, weighed by what it lacks; null if it may build nothing more. */
   static Kind choose(FactionWorldData.Settlement settlement, List<Kind> kinds, RandomSource random) {
      int rank = rank(settlement);
      int mouths = Math.max(1, settlement.population() + settlement.garrison());
      boolean crowded = settlement.population() * 4 >= FactionSimulation.capacity(settlement) * 3;
      boolean hungry = settlement.food() >= 0 && settlement.food() / mouths < 6 || settlement.famine() > 0;
      boolean sick = settlement.health() < 70;
      boolean restless = settlement.order() < 60;
      int defence = 0;
      for (Kind kind : kinds) {
         defence += kind.defence;
      }

      double total = 0.0;
      double[] weights = new double[Kind.values().length];
      for (Kind kind : Kind.values()) {
         if (kind.rank > rank || count(kinds, kind) >= kind.most) {
            continue;
         }

         double weight = switch (kind.group) {
            case HOMES -> crowded ? 6.0 : 1.0;
            case FOOD -> hungry ? 6.0 : 1.5;
            case FAITH -> sick || restless ? 3.0 : 1.0;
            case DEFENCE -> defence < 2 + rank * 3 ? 3.0 : 0.8;
            case LEISURE, RULE -> restless ? 3.0 : 1.0;
            default -> 1.2;
         };
         // What the settlement has none of yet comes before more of what it has.
         weight *= count(kinds, kind) == 0 ? 2.0 : 1.0;
         weights[kind.ordinal()] = weight;
         total += weight;
      }

      if (total <= 0.0) {
         return null;
      }

      double pick = random.nextDouble() * total;
      for (Kind kind : Kind.values()) {
         pick -= weights[kind.ordinal()];
         if (weights[kind.ordinal()] > 0.0 && pick < 0.0) {
            return kind;
         }
      }

      return null;
   }
}
