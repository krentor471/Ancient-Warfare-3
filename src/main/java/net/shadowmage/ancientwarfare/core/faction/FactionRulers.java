package net.shadowmage.ancientwarfare.core.faction;

import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.shadowmage.ancientwarfare.core.common.npc.FactionTradeListRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;

/**
 * Every faction has a ruler with a name and a trait that shapes its wars, its treaties and its
 * prices. The ruler is the leader who stands in the capital; when he falls, another takes his place,
 * often of a different mind.
 */
public final class FactionRulers {
   public enum Trait {
      /** Dear prices, a heavy tribute, but gifts sway him more. */
      GREEDY(1.0, 0, 1.0, 1.25, 1.5, 0, 1.0),
      /** Quick to go to war with factions and players, slow to make peace. */
      WARLIKE(2.0, 15, 0.5, 1.0, 1.0, 15, 1.5),
      /** Goes to war only when much stronger, makes peace early. */
      CAUTIOUS(0.4, -15, 2.0, 1.0, 1.0, -15, 0.5),
      /** Fair prices and a light tribute; treaties come easier. */
      GENEROUS(1.0, -5, 1.25, 0.85, 0.75, -10, 1.0);

      /** Times the usual chance of a war with another faction. */
      final double warChance;
      /** Added to how bad an opinion, or a player's standing, may get before he goes to war. */
      final int warTemper;
      final double peaceChance;
      /** Times the usual amount a trader asks. */
      final double prices;
      final double tribute;
      /** Added to the standing a player needs for a treaty. */
      final int treatyStanding;
      final double raidChance;

      Trait(double warChance, int warTemper, double peaceChance, double prices, double tribute, int treatyStanding, double raidChance) {
         this.warChance = warChance;
         this.warTemper = warTemper;
         this.peaceChance = peaceChance;
         this.prices = prices;
         this.tribute = tribute;
         this.treatyStanding = treatyStanding;
         this.raidChance = raidChance;
      }

      public String key() {
         return this.name().toLowerCase(Locale.ROOT);
      }
   }

   /** How many ruler names the lang files hold: ruler.ancientwarfare.name.0 and on. */
   public static final int NAMES = 32;
   private static final Trait[] TRAITS = Trait.values();

   private FactionRulers() {
   }

   /** The faction's ruler, crowned the first time anyone asks. */
   public static FactionWorldData.Ruler ruler(FactionWorldData data, String faction, RandomSource random) {
      FactionWorldData.Ruler ruler = data.ruler(faction);
      if (ruler == null) {
         long day = Math.max(0L, data.lastSimulatedDay());
         ruler = new FactionWorldData.Ruler(
            random.nextInt(NAMES), random.nextInt(TRAITS.length), day, FactionPeople.newLook(random), FactionPeople.bornAt(day, 25 + random.nextInt(26), random), false, FactionPeople.newCharacter(random)
         );
         data.setRuler(faction, ruler);
      }

      return ruler;
   }

   /** Who the ruler is, by name and day of birth: what outlives him does not carry over to his heir. */
   static String key(FactionWorldData.Ruler ruler) {
      return ruler.name() + ":" + ruler.born();
   }

   /** How the ruler's character weighs on one of his chances; see FactionPeople.Character. */
   static double character(FactionWorldData data, String faction, RandomSource random, java.util.function.ToDoubleFunction<FactionPeople.Character> effect) {
      return FactionPeople.factor(ruler(data, faction, random).character(), effect);
   }

   static double character(ServerLevel level, String faction, java.util.function.ToDoubleFunction<FactionPeople.Character> effect) {
      return character(FactionWorldData.get(level), faction, level.getRandom(), effect);
   }

   public static Trait trait(ServerLevel level, String faction) {
      return trait(ruler(FactionWorldData.get(level), faction, level.getRandom()));
   }

   static Trait trait(FactionWorldData data, String faction, RandomSource random) {
      return trait(ruler(data, faction, random));
   }

   private static Trait trait(FactionWorldData.Ruler ruler) {
      return TRAITS[Mth.clamp(ruler.trait(), 0, TRAITS.length - 1)];
   }

   public static Component name(int name) {
      return Component.translatable("ruler.ancientwarfare.name." + Math.floorMod(name, NAMES));
   }

   public static Component traitName(int trait) {
      return Component.translatable("ruler.ancientwarfare.trait." + TRAITS[Mth.clamp(trait, 0, TRAITS.length - 1)].key());
   }

   /** Marks the one leader in a capital who is the ruler; any other leaders there are his captains. */
   public static final String RULER_TAG = "aw_ruler";

   public static boolean isRuler(FactionNpcEntity npc) {
      return npc.getTags().contains(RULER_TAG);
   }

   /** The capital the leader stands in, or null when he is not a leader in his faction's capital. */
   private static FactionWorldData.Settlement capitalOf(ServerLevel level, FactionNpcEntity npc) {
      if (!PlayerDiplomacy.isLeader(npc)) {
         return null;
      }

      FactionWorldData.Settlement settlement = FactionSimulation.settlementAt(FactionWorldData.get(level), npc.getFactionName(), npc.getX(), npc.getZ(), 16);
      return settlement != null && FactionTowns.CAPITAL.equals(settlement.kind()) ? settlement : null;
   }

   private static List<FactionNpcEntity> otherLeaders(ServerLevel level, FactionWorldData.Settlement capital, FactionNpcEntity npc) {
      return level.getEntitiesOfClass(
         FactionNpcEntity.class,
         new AABB(capital.min(), capital.max().offset(1, 1, 1)).inflate(24.0, 32.0, 24.0),
         other -> other != npc && other.isAlive() && PlayerDiplomacy.isLeader(other) && other.getFactionName().equals(npc.getFactionName())
      );
   }

   /** A leader comes into a capital: if he is the ruler, he bears the ruler's present name. */
   public static void onLeaderJoin(ServerLevel level, FactionNpcEntity npc) {
      if (isRuler(npc)) {
         nameLeader(level, npc);
      }
   }

   /**
    * Exactly one of the leaders standing in a capital is the ruler: the first is crowned when none is,
    * and any other who still bears the title, as after a chunk loaded two at once, steps down.
    */
   static void settleThrone(ServerLevel level, List<FactionNpcEntity> leaders) {
      FactionNpcEntity ruler = leaders.stream().filter(FactionRulers::isRuler).findFirst().orElse(leaders.get(0));
      for (FactionNpcEntity leader : leaders) {
         if (leader == ruler) {
            if (!isRuler(leader)) {
               leader.addTag(RULER_TAG);
               nameLeader(level, leader);
            }
         } else if (isRuler(leader) || isTitled(leader)) {
            leader.removeTag(RULER_TAG);
            leader.setCustomName(null);
         }
      }
   }

   private static boolean isTitled(FactionNpcEntity npc) {
      return npc.getCustomName() != null
         && npc.getCustomName().getContents() instanceof TranslatableContents title
         && "ruler.ancientwarfare.title".equals(title.getKey());
   }

   /** The ruler fell: a successor takes the throne, never of the same name and seldom of the same mind. */
   public static void onLeaderDeath(ServerLevel level, FactionNpcEntity leader) {
      FactionWorldData data = FactionWorldData.get(level);
      String faction = leader.getFactionName();
      RandomSource random = level.getRandom();
      FactionWorldData.Ruler old = ruler(data, faction, random);
      // The heir of the court takes the throne, with their own mind and their own friends.
      FactionWorldData.Ruler successor = FactionCourt.succession(level, faction, Math.max(0L, data.lastSimulatedDay()));
      int name = successor.name();
      int trait = successor.trait();
      data.setRuler(faction, successor);
      FactionNews.tell(
         level, Component.translatable("news.ancientwarfare.ruler_fell", FactionNews.factionName(faction), name(old.name()), name(name), traitName(trait))
      );
      // A captain in the capital is crowned at once.
      FactionWorldData.Settlement capital = capitalOf(level, leader);
      if (capital != null) {
         otherLeaders(level, capital, leader).stream().findFirst().ifPresent(heir -> {
            heir.addTag(RULER_TAG);
            nameLeader(level, heir);
         });
      }
   }

   /** A new ruler takes the throne by other means than death: the ruler NPCs about take their name. */
   static void enthrone(ServerLevel level, String faction, FactionWorldData.Ruler ruler) {
      FactionWorldData.get(level).setRuler(faction, ruler);
      for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
         if (entity instanceof FactionNpcEntity npc && faction.equals(npc.getFactionName()) && isRuler(npc)) {
            nameLeader(level, npc);
         }
      }
   }

   /** The ruler bears his name and epithet. */
   private static void nameLeader(ServerLevel level, FactionNpcEntity leader) {
      FactionWorldData.Ruler ruler = ruler(FactionWorldData.get(level), leader.getFactionName(), level.getRandom());
      leader.setCustomName(
         Component.translatable(
            "ruler.ancientwarfare.title", name(ruler.name()), Component.translatable("ruler.ancientwarfare.epithet." + TRAITS[Mth.clamp(ruler.trait(), 0, TRAITS.length - 1)].key())
         )
      );
   }

   /** For the diplomacy screen: the ruler's looks, age, years on the throne and whether he is ill. */
   public static int[] person(FactionWorldData data, String faction, RandomSource random) {
      FactionWorldData.Ruler ruler = ruler(data, faction, random);
      long day = Math.max(0L, data.lastSimulatedDay());
      int age = FactionPeople.age(ruler.born(), day);
      // A ruler from a world read before ages were kept may have reigned longer than he has lived.
      int reign = Math.min(FactionPeople.years(ruler.since(), day), Math.max(0, age - 16));
      return new int[]{ruler.look(), age, reign, ruler.ill() ? 1 : 0, ruler.character()};
   }

   /** Daily: rulers fall ill and get well, and the old or the sick die and their heir takes the throne. */
   static void simulateDay(ServerLevel level, FactionWorldData data) {
      long day = Math.max(0L, data.lastSimulatedDay());
      RandomSource random = level.getRandom();
      java.util.Set<String> factions = new java.util.TreeSet<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!FactionDiplomacy.isOutlaw(settlement.faction())) {
            factions.add(settlement.faction());
         }
      }

      for (String faction : factions) {
         FactionWorldData.Ruler ruler = ruler(data, faction, random);
         int age = FactionPeople.age(ruler.born(), day);
         if (ruler.ill()) {
            if (random.nextDouble() < FactionPeople.illnessDeath(age) * FactionPeople.factor(ruler.character(), c -> c.oldAge)) {
               die(level, faction, "illness", day);
            } else if (random.nextDouble() < FactionPeople.recovery(age)) {
               data.setRuler(faction, ruler.withIll(false));
               FactionNews.tell(level, Component.translatable("news.ancientwarfare.ruler_recovered", FactionNews.factionName(faction), name(ruler.name())));
            }
         } else if (random.nextDouble() < FactionPeople.oldAgeDeath(age) * FactionPeople.factor(ruler.character(), c -> c.oldAge)) {
            die(level, faction, "old_age", day);
         } else if (random.nextDouble() < FactionPeople.illness(age) * FactionPeople.factor(ruler.character(), c -> c.illness)) {
            data.setRuler(faction, ruler.withIll(true));
            FactionNews.tell(level, Component.translatable("news.ancientwarfare.ruler_ill", FactionNews.factionName(faction), name(ruler.name()), age));
         }
      }
   }

   /** For testing: set the ruler's age, make him ill, let him die, or run a day of his life. */
   public static String debugLife(ServerLevel level, String faction, String what, int value) {
      FactionWorldData data = FactionWorldData.get(level);
      long day = Math.max(0L, data.lastSimulatedDay());
      FactionWorldData.Ruler ruler = ruler(data, faction, level.getRandom());
      switch (what) {
         case "age" -> data.setRuler(faction, new FactionWorldData.Ruler(ruler.name(), ruler.trait(), ruler.since(), ruler.look(), day - (long)value * FactionPeople.YEAR_DAYS, ruler.ill(), ruler.character()));
         case "ill" -> data.setRuler(faction, ruler.withIll(true));
         case "die" -> die(level, faction, value == 1 ? "illness" : "old_age", day);
         case "day" -> {
            for (int i = 0; i < Math.max(1, value); i++) {
               simulateDay(level, data);
            }
         }
         default -> {
            return "age|ill|die|day";
         }
      }

      data.setDirty();
      FactionWorldData.Ruler now = ruler(data, faction, level.getRandom());
      return name(now.name()).getString() + " age " + FactionPeople.age(now.born(), day) + (now.ill() ? " ill" : "");
   }

   /** The ruler dies in his bed: the heir is crowned and the ruler NPCs take his name. */
   static void die(ServerLevel level, String faction, String cause, long day) {
      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.Ruler old = ruler(data, faction, level.getRandom());
      FactionWorldData.Ruler successor = FactionCourt.succession(level, faction, day);
      enthrone(level, faction, successor);
      FactionNews.tell(
         level,
         Component.translatable(
            "news.ancientwarfare.ruler_died",
            FactionNews.factionName(faction),
            name(old.name()),
            FactionPeople.age(old.born(), day),
            Component.translatable("news.ancientwarfare.death." + cause),
            name(successor.name()),
            traitName(successor.trait())
         )
      );
   }

   /** A trader of a greedy ruler asks more, of a generous one less. */
   public static List<FactionTradeListRegistry.FactionTradeEntry> price(ServerLevel level, String faction, List<FactionTradeListRegistry.FactionTradeEntry> trades) {
      return price(level, faction, trades, 1.0);
   }

   /** As above, times a factor of the moment, such as a market day. */
   public static List<FactionTradeListRegistry.FactionTradeEntry> price(
      ServerLevel level, String faction, List<FactionTradeListRegistry.FactionTradeEntry> trades, double extra
   ) {
      double factor = trait(level, faction).prices * character(level, faction, c -> c.prices) * FactionCourt.priceFactor(level, faction) * extra;
      if (factor == 1.0) {
         return trades;
      }

      return trades.stream().map(trade -> new FactionTradeListRegistry.FactionTradeEntry(trade.inputs().stream().map(stack -> {
         ItemStack copy = stack.copy();
         if (copy.getMaxStackSize() > 1) {
            copy.setCount(Mth.clamp((int)Math.round(copy.getCount() * factor), 1, copy.getMaxStackSize()));
         }

         return copy;
      }).toList(), trade.outputs())).toList();
   }
}
