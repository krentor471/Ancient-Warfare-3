package net.shadowmage.ancientwarfare.core.faction;

import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.shadowmage.ancientwarfare.core.common.npc.FactionTradeListRegistry;
import net.minecraft.world.item.ItemStack;
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
         ruler = new FactionWorldData.Ruler(random.nextInt(NAMES), random.nextInt(TRAITS.length), Math.max(0L, data.lastSimulatedDay()));
         data.setRuler(faction, ruler);
      }

      return ruler;
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

   /** A leader in his faction's capital: the ruler himself. */
   public static boolean isRuler(ServerLevel level, FactionNpcEntity npc) {
      if (!PlayerDiplomacy.isLeader(npc)) {
         return false;
      }

      FactionWorldData.Settlement settlement = FactionSimulation.settlementAt(FactionWorldData.get(level), npc.getFactionName(), npc.getX(), npc.getZ(), 16);
      return settlement != null && FactionTowns.CAPITAL.equals(settlement.kind());
   }

   /** The ruler fell: a successor takes the throne, never of the same name and seldom of the same mind. */
   public static void onLeaderDeath(ServerLevel level, FactionNpcEntity leader) {
      FactionWorldData data = FactionWorldData.get(level);
      String faction = leader.getFactionName();
      RandomSource random = level.getRandom();
      FactionWorldData.Ruler old = ruler(data, faction, random);
      int name = (old.name() + 1 + random.nextInt(NAMES - 1)) % NAMES;
      int trait = random.nextInt(4) == 0 ? old.trait() : (old.trait() + 1 + random.nextInt(TRAITS.length - 1)) % TRAITS.length;
      data.setRuler(faction, new FactionWorldData.Ruler(name, trait, Math.max(0L, data.lastSimulatedDay())));
      FactionNews.tell(
         level, Component.translatable("news.ancientwarfare.ruler_fell", FactionNews.factionName(faction), name(old.name()), name(name), traitName(trait))
      );
   }

   /** The leader standing in the capital bears the ruler's name. */
   public static void nameLeader(ServerLevel level, FactionNpcEntity leader) {
      FactionWorldData.Ruler ruler = ruler(FactionWorldData.get(level), leader.getFactionName(), level.getRandom());
      leader.setCustomName(
         Component.translatable(
            "ruler.ancientwarfare.title", name(ruler.name()), Component.translatable("ruler.ancientwarfare.epithet." + TRAITS[Mth.clamp(ruler.trait(), 0, TRAITS.length - 1)].key())
         )
      );
   }

   /** A trader of a greedy ruler asks more, of a generous one less. */
   public static List<FactionTradeListRegistry.FactionTradeEntry> price(ServerLevel level, String faction, List<FactionTradeListRegistry.FactionTradeEntry> trades) {
      double factor = trait(level, faction).prices;
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
