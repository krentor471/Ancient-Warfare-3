package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;

/**
 * The daily decisions of AI factions toward each other. Neighbours (factions whose provinces touch)
 * grate on each other, common enemies bring factions together, and treaties warm relations. A
 * faction that dislikes a weaker neighbour may declare war; long wars wear factions down into
 * peace. Factions at war send armies to march on each other's border settlements (see
 * FactionCampaigns), which can change hands; the world's past is reckoned as instant battles.
 */
public final class FactionAI {
   /** No wars before this day, so the player has time to settle in. */
   private static final long FIRST_WAR_DAY = 4L;
   private static final int WAR_OPINION = -40;
   private static final double WAR_CHANCE = 0.3;
   private static final double STRENGTH_ADVANTAGE = 1.2;
   private static final long MIN_WAR_DAYS = 5L;
   private static final double PEACE_CHANCE_PER_DAY = 0.06;
   private static final int PEACE_OPINION = -20;
   private static final int PACT_OPINION = 40;
   private static final int ALLIANCE_OPINION = 70;
   private static final double TREATY_CHANCE = 0.2;
   private static final double ATTACK_SHARE = 0.4;
   /** Walls make a fort's garrison count for more in a battle fought out of sight. */
   private static final double FORT_DEFENCE = 1.5;
   private static final double CAPITAL_DEFENCE = 2.0;
   static final int MAX_ARMY = 100;
   /** Days a newly taken settlement cannot be attacked, so it is not traded back and forth daily. */
   private static final long HOLD_DAYS = 6L;
   /** Day each settlement was last taken; not saved, a restart only shortens the respite. */
   private static final Map<Integer, Long> TAKEN = new HashMap<>();

   private FactionAI() {
   }

   public static void simulateDay(ServerLevel level, FactionWorldData data, long day) {
      RandomSource random = level.getRandom();
      Map<String, Integer> strength = strengths(data);
      List<String> factions = new ArrayList<>(strength.keySet());
      factions.removeIf(faction -> !FactionRegistry.isKnown(faction) || FactionDiplomacy.isOutlaw(faction));
      factions.sort(String::compareTo);
      Map<String, Set<String>> neighbours = neighbours(data);

      for (int i = 0; i < factions.size(); i++) {
         for (int j = i + 1; j < factions.size(); j++) {
            String faction = factions.get(i);
            String other = factions.get(j);
            boolean bordering = neighbours.getOrDefault(faction, Set.of()).contains(other);
            driftOpinion(level, factions, faction, other, bordering);
            decide(level, data, day, random, strength, faction, other, bordering);
         }
      }

      for (int i = 0; i < factions.size(); i++) {
         for (int j = i + 1; j < factions.size(); j++) {
            String faction = factions.get(i);
            String other = factions.get(j);
            if (FactionDiplomacy.relation(level, faction, other).state() == RelationState.WAR
               && neighbours.getOrDefault(faction, Set.of()).contains(other)) {
               battle(level, data, random, strength, faction, other);
            }
         }
      }
   }

   /** How much stronger a ruler wants to be before he attacks. */
   private static double advantage(FactionRulers.Trait ruler) {
      return switch (ruler) {
         case WARLIKE -> 1.0;
         case CAUTIOUS -> 1.6;
         default -> STRENGTH_ADVANTAGE;
      };
   }

   private static void driftOpinion(ServerLevel level, List<String> factions, String faction, String other, boolean bordering) {
      RelationState state = FactionDiplomacy.relation(level, faction, other).state();
      int delta = 0;
      if (state == RelationState.PACT || state == RelationState.ALLIANCE) {
         delta += 1;
      } else if (bordering) {
         delta -= 1;
      }

      for (String third : factions) {
         if (!third.equals(faction)
            && !third.equals(other)
            && FactionDiplomacy.relation(level, faction, third).state() == RelationState.WAR
            && FactionDiplomacy.relation(level, other, third).state() == RelationState.WAR) {
            delta += 1;
            break;
         }
      }

      int before = FactionDiplomacy.relation(level, faction, other).opinion();
      int after = FactionDiplomacy.adjustOpinion(level, faction, other, delta).opinion();
      if (state == RelationState.NEUTRAL && bordering && before > WAR_OPINION && after <= WAR_OPINION) {
         FactionNews.tension(level, faction, other);
      }
   }

   private static void decide(
      ServerLevel level, FactionWorldData data, long day, RandomSource random, Map<String, Integer> strength, String faction, String other, boolean bordering
   ) {
      FactionDiplomacy.Relation relation = FactionDiplomacy.relation(level, faction, other);
      int mine = strength.getOrDefault(faction, 0);
      int theirs = strength.getOrDefault(other, 0);
      FactionRulers.Trait myRuler = FactionRulers.trait(data, faction, random);
      FactionRulers.Trait theirRuler = FactionRulers.trait(data, other, random);
      // The fiercer of the two rulers sets how soon it comes to war; each needs his own edge in strength.
      // Each court's marshal urges the war on or holds it back.
      double warChance = WAR_CHANCE * Math.max(myRuler.warChance, theirRuler.warChance)
         * Math.max(FactionCourt.warFactor(level, faction), FactionCourt.warFactor(level, other))
         * Math.max(FactionRulers.character(data, faction, random, c -> c.factionWar), FactionRulers.character(data, other, random, c -> c.factionWar))
         * FactionFeasts.warFactor(level, faction, other)
         * FactionPastimes.warFactor(level, faction, other)
         * FactionRebellions.warFactor(level, faction, other)
         * FactionFaith.warFactor(level, faction, other);
      int warOpinion = WAR_OPINION + Math.max(myRuler.warTemper, theirRuler.warTemper);
      switch (relation.state()) {
         case NEUTRAL -> {
            if (bordering && day >= FIRST_WAR_DAY && relation.opinion() <= warOpinion && random.nextDouble() < warChance) {
               if (mine >= theirs * advantage(myRuler)) {
                  declareWar(level, data, faction, other);
               } else if (theirs >= mine * advantage(theirRuler)) {
                  declareWar(level, data, other, faction);
               }
            } else if (relation.opinion() >= PACT_OPINION && random.nextDouble() < TREATY_CHANCE) {
               declareFriendship(level, faction, other, RelationState.PACT);
            }
         }
         case PACT -> {
            if (relation.opinion() >= ALLIANCE_OPINION && random.nextDouble() < TREATY_CHANCE) {
               declareFriendship(level, faction, other, RelationState.ALLIANCE);
            }
         }
         case WAR -> {
            long days = day - relation.since();
            boolean beaten = mine == 0 || theirs == 0;
            double peaceChance = PEACE_CHANCE_PER_DAY * Math.min(myRuler.peaceChance, theirRuler.peaceChance) * (days - MIN_WAR_DAYS + 1)
               * Math.min(FactionCourt.peaceFactor(level, faction), FactionCourt.peaceFactor(level, other));
            if (beaten || days >= MIN_WAR_DAYS && random.nextDouble() < peaceChance) {
               if (FactionDiplomacy.setState(level, faction, other, RelationState.NEUTRAL)) {
                  FactionBorders.settlePeace(level, data, faction, other);
                  if (beaten) {
                     FactionNews.relationChanged(level, faction, other, RelationState.NEUTRAL, "beaten", FactionNews.factionName(mine == 0 ? faction : other));
                  } else {
                     FactionNews.relationChanged(level, faction, other, RelationState.NEUTRAL, "tired", days);
                  }
               }

               FactionDiplomacy.adjustOpinion(level, faction, other, Math.max(0, PEACE_OPINION - relation.opinion()));
            }
         }
         default -> {
         }
      }
   }

   /**
    * War, with the cause the news gives: to win back settlements the enemy took from it, to stand
    * by an ally the enemy is fighting, or else over the land along their border, where friction
    * soured them.
    */
   private static void declareWar(ServerLevel level, FactionWorldData data, String faction, String other) {
      if (!FactionDiplomacy.setState(level, faction, other, RelationState.WAR)) {
         return;
      }

      if (FactionFaith.holyWar(level, faction, other)) {
         FactionNews.relationChanged(level, faction, other, RelationState.WAR, "holy", FactionFaith.name(FactionFaith.faith(level, faction)));
         return;
      }

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.faction().equals(other) && settlement.capturedFrom.equals(faction)) {
            FactionNews.relationChanged(level, faction, other, RelationState.WAR, "revenge");
            return;
         }
      }

      for (String ally : FactionRegistry.factionNames()) {
         RelationState withAlly = ally.equals(faction) || ally.equals(other) ? null : FactionDiplomacy.relation(level, faction, ally).state();
         if ((withAlly == RelationState.ALLIANCE || withAlly == RelationState.PACT)
            && FactionDiplomacy.relation(level, ally, other).state() == RelationState.WAR) {
            FactionNews.relationChanged(level, faction, other, RelationState.WAR, "ally", FactionNews.factionName(ally));
            return;
         }
      }

      FactionNews.relationChanged(level, faction, other, RelationState.WAR, "border");
   }

   /** A pact or alliance, against an enemy both are fighting, or out of long good relations. */
   private static void declareFriendship(ServerLevel level, String faction, String other, RelationState state) {
      if (!FactionDiplomacy.setState(level, faction, other, state)) {
         return;
      }

      for (String enemy : FactionRegistry.factionNames()) {
         if (!enemy.equals(faction)
            && !enemy.equals(other)
            && !FactionDiplomacy.isOutlaw(enemy)
            && FactionDiplomacy.relation(level, faction, enemy).state() == RelationState.WAR
            && FactionDiplomacy.relation(level, other, enemy).state() == RelationState.WAR) {
            FactionNews.relationChanged(level, faction, other, state, "enemy", FactionNews.factionName(enemy));
            return;
         }
      }

      FactionNews.relationChanged(level, faction, other, state, "friends");
   }

   private static void battle(ServerLevel level, FactionWorldData data, RandomSource random, Map<String, Integer> strength, String faction, String other) {
      int mine = strength.getOrDefault(faction, 0);
      int theirs = strength.getOrDefault(other, 0);
      if (mine + theirs <= 0) {
         return;
      }

      boolean firstAttacks = random.nextInt(mine + theirs) < mine;
      String attacker = firstAttacks ? faction : other;
      String defender = firstAttacks ? other : faction;
      FactionWorldData.Settlement target = borderSettlement(data, attacker, defender, data.lastSimulatedDay());
      if (target == null) {
         return;
      }

      // However large the country, one campaign can only feed and march so many soldiers.
      int force = Math.min(MAX_ARMY, (int)Math.ceil(strength.getOrDefault(attacker, 0) * ATTACK_SHARE));
      if (!FactionHistory.running()) {
         // An army sets out and must march there; one at a time against each enemy.
         // Against another faction a host is mustered first, under the marshal; outlaws just go.
         boolean outlaws = FactionDiplomacy.isOutlaw(attacker) || FactionDiplomacy.isOutlaw(defender);
         if (!FactionCampaigns.marchingAgainst(data, attacker, defender) && !FactionMusters.mustering(level, attacker, defender)) {
            boolean went = outlaws ? FactionCampaigns.launch(level, data, attacker, target, force) : FactionMusters.muster(level, data, attacker, target, force);
            if (went) {
               strength.merge(attacker, -force, Integer::sum);
            }
         }

         // Beside the main army, small bands go looting behind the enemy's border.
         int raiders = FactionRaids.raid(level, data, random, attacker, defender, strength.getOrDefault(attacker, 0));
         strength.merge(attacker, -raiders, Integer::sum);

         return;
      }

      // The world's past is reckoned at once.
      int taken = takeLosses(data, attacker, force);
      int survivors = siege(level, data, random, attacker, target, taken, true);
      returnSoldiers(data, attacker, survivors);
      strength.merge(attacker, -(taken - survivors), Integer::sum);
   }

   /**
    * An army of force soldiers, already away from home, besieges the target out of sight. Both
    * sides lose men; if the garrison falls, half the survivors hold the settlement. Returns how many
    * of the army go home.
    */
   static int siege(ServerLevel level, FactionWorldData data, RandomSource random, String attacker, FactionWorldData.Settlement target, int force, boolean takes) {
      return siege(level, data, random, attacker, target, force, takes, 1.0);
   }

   /** As siege, for an army that hits might times as hard as its numbers. */
   static int siege(ServerLevel level, FactionWorldData data, RandomSource random, String attacker, FactionWorldData.Settlement target, int force, boolean takes, double might) {
      String defender = target.faction();
      int defence = (int)Math.round(target.garrison * walls(target));
      int attackerLosses = Math.min(force, (int)Math.round(defence * (0.3 + random.nextDouble() * 0.4) / might));
      int defenderLosses = Math.min(target.garrison, (int)Math.round(force * might * (0.3 + random.nextDouble() * 0.4)));
      target.garrison -= defenderLosses;
      FactionDiplomacy.adjustOpinion(level, attacker, defender, -2);
      int survivors = force - attackerLosses;
      data.setDirty();
      if (takes && target.garrison <= 0 && survivors > 0) {
         int garrison = Math.max(1, survivors / 2);
         capture(level, data, target, attacker, garrison);
         return survivors - garrison;
      }

      return survivors;
   }

   /** Times the garrison's worth its walls make it. */
   static double walls(FactionWorldData.Settlement target) {
      return isFort(target) ? FORT_DEFENCE : FactionTowns.CAPITAL.equals(target.kind()) ? CAPITAL_DEFENCE : 1.0;
   }

   /** The settlement passes to the attacker, with the given garrison, together with its province. */
   static void capture(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement target, String attacker, int garrison) {
      String defender = target.faction();
      target.capturedFrom = defender;
      target.loyalty = 30;
      target.order = 40;
      TAKEN.put(target.id(), data.lastSimulatedDay());
      target.setFaction(attacker);
      target.garrison = garrison;
      target.manned = true;
      data.setProvinceOwner(target.province(), attacker);
      data.setDirty();
      FactionNews.captured(level, attacker, defender, target);
   }

   /** Soldiers coming home join the faction's largest garrison. */
   static void returnSoldiers(FactionWorldData data, String faction, int soldiers) {
      FactionWorldData.Settlement largest = null;

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.faction().equals(faction) && (largest == null || settlement.garrison > largest.garrison)) {
            largest = settlement;
         }
      }

      if (largest != null && soldiers > 0) {
         largest.garrison += soldiers;
         data.setDirty();
      }
   }

   /** Takes soldiers from the faction's garrisons, largest first, as an army leaving home. Returns how many were taken. */
   static int takeLosses(FactionWorldData data, String faction, int losses) {
      int taken = 0;

      while (losses > 0) {
         FactionWorldData.Settlement largest = null;

         for (FactionWorldData.Settlement settlement : data.settlements()) {
            if (settlement.faction().equals(faction) && settlement.garrison > 0 && (largest == null || settlement.garrison > largest.garrison)) {
               largest = settlement;
            }
         }

         if (largest == null) {
            return taken;
         }

         largest.garrison--;
         losses--;
         taken++;
      }

      return taken;
   }

   /**
    * The defender's settlement in a province touching the attacker's land: the one most surrounded
    * by it, so the front moves as one line instead of cutting holes, then the weakest. While
    * the defender holds a fort on that border, the attacker must take the fort first.
    */
   static FactionWorldData.Settlement borderSettlement(FactionWorldData data, String attacker, String defender, long day) {
      Set<Long> attackerLand = new HashSet<>(data.provincesOf(attacker));
      FactionWorldData.Settlement best = null;

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         int front = front(settlement.province(), attackerLand);
         if (settlement.faction().equals(defender) && front > 0 && day - TAKEN.getOrDefault(settlement.id(), Long.MIN_VALUE / 2) >= HOLD_DAYS) {
            int bestFront = best == null ? 0 : front(best.province(), attackerLand);
            boolean better = best == null
               || isFort(settlement) && !isFort(best)
               || isFort(settlement) == isFort(best) && (front > bestFront || front == bestFront && settlement.garrison < best.garrison);
            if (better) {
               best = settlement;
            }
         }
      }

      return best;
   }

   static boolean isFort(FactionWorldData.Settlement settlement) {
      return "fort".equals(settlement.kind());
   }

   /** How many of the eight provinces around this one are in land. */
   private static int front(long province, Set<Long> land) {
      int cellX = ChunkPos.getX(province);
      int cellZ = ChunkPos.getZ(province);
      int count = 0;

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            if ((dx != 0 || dz != 0) && land.contains(ChunkPos.asLong(cellX + dx, cellZ + dz))) {
               count++;
            }
         }
      }

      return count;
   }

   static Map<String, Integer> strengths(FactionWorldData data) {
      Map<String, Integer> strength = new HashMap<>();

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         strength.merge(settlement.faction(), settlement.garrison, Integer::sum);
      }

      return strength;
   }

   /** Factions whose decided provinces touch. */
   private static Map<String, Set<String>> neighbours(FactionWorldData data) {
      Map<Long, String> owners = data.provinceOwners();
      Map<String, Set<String>> neighbours = new HashMap<>();
      owners.forEach((province, owner) -> {
         if (!owner.isEmpty() && !PlayerCountry.isLand(owner)) {
            int cellX = ChunkPos.getX(province);
            int cellZ = ChunkPos.getZ(province);

            for (int dx = -1; dx <= 1; dx++) {
               for (int dz = -1; dz <= 1; dz++) {
                  String other = owners.get(ChunkPos.asLong(cellX + dx, cellZ + dz));
                  if (other != null && !other.isEmpty() && !other.equals(owner) && !PlayerCountry.isLand(other)) {
                     neighbours.computeIfAbsent(owner, ignored -> new HashSet<>()).add(other);
                  }
               }
            }
         }
      });
      return neighbours;
   }
}
