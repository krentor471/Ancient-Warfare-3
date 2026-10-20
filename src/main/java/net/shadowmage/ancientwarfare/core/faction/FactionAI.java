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

/**
 * The daily decisions of AI factions toward each other. Neighbours (factions whose provinces touch)
 * grate on each other, common enemies bring factions together, and treaties warm relations. A
 * faction that dislikes a weaker neighbour may declare war; long wars wear factions down into
 * peace. Wars between neighbours are fought out of sight as daily battles over border settlements,
 * which can change hands.
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
   private static final double CAPTURE_PLAYER_RANGE = 192.0;

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

      FactionDiplomacy.adjustOpinion(level, faction, other, delta);
   }

   private static void decide(
      ServerLevel level, FactionWorldData data, long day, RandomSource random, Map<String, Integer> strength, String faction, String other, boolean bordering
   ) {
      FactionDiplomacy.Relation relation = FactionDiplomacy.relation(level, faction, other);
      int mine = strength.getOrDefault(faction, 0);
      int theirs = strength.getOrDefault(other, 0);
      switch (relation.state()) {
         case NEUTRAL -> {
            if (bordering && day >= FIRST_WAR_DAY && relation.opinion() <= WAR_OPINION && random.nextDouble() < WAR_CHANCE) {
               if (mine >= theirs * STRENGTH_ADVANTAGE) {
                  declare(level, faction, other, RelationState.WAR);
               } else if (theirs >= mine * STRENGTH_ADVANTAGE) {
                  declare(level, other, faction, RelationState.WAR);
               }
            } else if (relation.opinion() >= PACT_OPINION && random.nextDouble() < TREATY_CHANCE) {
               declare(level, faction, other, RelationState.PACT);
            }
         }
         case PACT -> {
            if (relation.opinion() >= ALLIANCE_OPINION && random.nextDouble() < TREATY_CHANCE) {
               declare(level, faction, other, RelationState.ALLIANCE);
            }
         }
         case WAR -> {
            long days = day - relation.since();
            boolean beaten = mine == 0 || theirs == 0;
            if (beaten || days >= MIN_WAR_DAYS && random.nextDouble() < PEACE_CHANCE_PER_DAY * (days - MIN_WAR_DAYS + 1)) {
               declare(level, faction, other, RelationState.NEUTRAL);
               FactionDiplomacy.adjustOpinion(level, faction, other, Math.max(0, PEACE_OPINION - relation.opinion()));
            }
         }
         default -> {
         }
      }
   }

   private static void declare(ServerLevel level, String faction, String other, RelationState state) {
      if (FactionDiplomacy.setState(level, faction, other, state)) {
         FactionNews.relationChanged(level, faction, other, state);
      }
   }

   /** One day of fighting: the stronger side presses a border settlement of the weaker. */
   private static void battle(ServerLevel level, FactionWorldData data, RandomSource random, Map<String, Integer> strength, String faction, String other) {
      int mine = strength.getOrDefault(faction, 0);
      int theirs = strength.getOrDefault(other, 0);
      if (mine + theirs <= 0) {
         return;
      }

      boolean firstAttacks = random.nextInt(mine + theirs) < mine;
      String attacker = firstAttacks ? faction : other;
      String defender = firstAttacks ? other : faction;
      FactionWorldData.Settlement target = borderSettlement(data, attacker, defender);
      if (target == null) {
         return;
      }

      int force = (int)Math.ceil(strength.getOrDefault(attacker, 0) * ATTACK_SHARE);
      if (level.hasNearbyAlivePlayer(target.center().getX(), target.center().getY(), target.center().getZ(), CAPTURE_PLAYER_RANGE)) {
         // A player is watching: the battle is fought by real soldiers instead.
         FactionArmies.assault(level, data, attacker, target, force);
         return;
      }

      int defence = target.garrison;
      int attackerLosses = Math.min(force, (int)Math.round(defence * (0.3 + random.nextDouble() * 0.4)));
      int defenderLosses = Math.min(defence, (int)Math.round(force * (0.3 + random.nextDouble() * 0.4)));
      takeLosses(data, attacker, attackerLosses);
      target.garrison -= defenderLosses;
      strength.merge(attacker, -attackerLosses, Integer::sum);
      strength.merge(defender, -defenderLosses, Integer::sum);
      FactionDiplomacy.adjustOpinion(level, attacker, defender, -2);
      if (target.garrison <= 0 && force - attackerLosses > 0) {
         int garrison = Math.max(1, (force - attackerLosses) / 2);
         takeLosses(data, attacker, garrison);
         capture(level, data, target, attacker, garrison);
      }

      data.setDirty();
   }

   /** The settlement passes to the attacker, with the given garrison, together with its province. */
   static void capture(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement target, String attacker, int garrison) {
      String defender = target.faction();
      target.capturedFrom = defender;
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

   /** The defender's settlement in a province touching the attacker's land, the weakest first. */
   private static FactionWorldData.Settlement borderSettlement(FactionWorldData data, String attacker, String defender) {
      Set<Long> attackerLand = new HashSet<>(data.provincesOf(attacker));
      FactionWorldData.Settlement best = null;

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.faction().equals(defender) && touches(settlement.province(), attackerLand) && (best == null || settlement.garrison < best.garrison)) {
            best = settlement;
         }
      }

      return best;
   }

   private static boolean touches(long province, Set<Long> land) {
      int cellX = ChunkPos.getX(province);
      int cellZ = ChunkPos.getZ(province);

      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            if (land.contains(ChunkPos.asLong(cellX + dx, cellZ + dz))) {
               return true;
            }
         }
      }

      return false;
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
         if (!owner.isEmpty()) {
            int cellX = ChunkPos.getX(province);
            int cellZ = ChunkPos.getZ(province);

            for (int dx = -1; dx <= 1; dx++) {
               for (int dz = -1; dz <= 1; dz++) {
                  String other = owners.get(ChunkPos.asLong(cellX + dx, cellZ + dz));
                  if (other != null && !other.isEmpty() && !other.equals(owner)) {
                     neighbours.computeIfAbsent(owner, ignored -> new HashSet<>()).add(other);
                  }
               }
            }
         }
      });
      return neighbours;
   }
}
