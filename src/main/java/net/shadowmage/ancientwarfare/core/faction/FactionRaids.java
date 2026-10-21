package net.shadowmage.ancientwarfare.core.faction;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;

/**
 * Raids between countries at war. Beside the main army, which marches to take a border
 * settlement, a country sends small bands against the enemy's weakly held places: villages,
 * farms, mines, trading posts. Raiders do not hold what they attack: they kill the defenders,
 * carry off gold, lead away some of the people as captives and go home.
 */
public final class FactionRaids {
   private static final double RAID_CHANCE = 0.4;
   /** How far from one of its own settlements a country looks for a place to raid. */
   private static final int RAID_RANGE = 1600;
   private static final int MIN_RAIDERS = 8;
   private static final int MAX_RAIDERS = 30;
   /** A country only raids while it keeps this many times the band at home. */
   private static final int RESERVE_FACTOR = 3;
   private static final int LOOT = 50;

   private FactionRaids() {
   }

   /** Perhaps sends one band against the enemy; returns how many soldiers set out. */
   static int raid(ServerLevel level, FactionWorldData data, RandomSource random, String attacker, String defender, int strength) {
      if (raiding(data, attacker, defender) || random.nextDouble() >= RAID_CHANCE) {
         return 0;
      }

      FactionWorldData.Settlement target = target(data, attacker, defender);
      if (target == null) {
         return 0;
      }

      int force = Math.max(MIN_RAIDERS, Math.min(MAX_RAIDERS, target.garrison() * 3 + 6));
      if (strength < force * RESERVE_FACTOR) {
         return 0;
      }

      FactionWorldData.Campaign campaign = FactionCampaigns.send(level, data, attacker, target, force);
      if (campaign == null) {
         return 0;
      }

      campaign.raid = true;
      data.setDirty();
      AncientWarfareCore.LOG.info("Faction {} sends {} raiders against {} {} #{}", attacker, campaign.size(), defender, target.kind(), target.id());
      return campaign.size();
   }

   private static boolean raiding(FactionWorldData data, String attacker, String defender) {
      for (FactionWorldData.Campaign campaign : data.campaigns()) {
         if (campaign.raid && campaign.faction().equals(attacker) && campaign.defender().equals(defender)) {
            return true;
         }
      }

      return false;
   }

   /** The enemy's raidable place nearest to any of the attacker's settlements, within reach. */
   private static FactionWorldData.Settlement target(FactionWorldData data, String attacker, String defender) {
      FactionWorldData.Settlement best = null;
      double bestDistance = (double)RAID_RANGE * RAID_RANGE;

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!settlement.faction().equals(defender) || !raidable(settlement) || underAttack(data, settlement)) {
            continue;
         }

         for (FactionWorldData.Settlement home : data.settlements()) {
            if (home.faction().equals(attacker)) {
               double distance = home.horizontalDistanceSquared(settlement.center().getX(), settlement.center().getZ());
               if (distance < bestDistance) {
                  best = settlement;
                  bestDistance = distance;
               }
            }
         }
      }

      return best;
   }

   /** Anything but forts and capitals, whose walls are for the main army. */
   private static boolean raidable(FactionWorldData.Settlement settlement) {
      return !FactionAI.isFort(settlement) && !FactionTowns.CAPITAL.equals(settlement.kind());
   }

   private static boolean underAttack(FactionWorldData data, FactionWorldData.Settlement settlement) {
      for (FactionWorldData.Campaign campaign : data.campaigns()) {
         if (campaign.targetSettlement() == settlement.id()) {
            return true;
         }
      }

      return false;
   }

   /** Raiders who beat the defenders take gold and lead a quarter of the people away; another tenth flee and are lost. */
   static void plunder(ServerLevel level, FactionWorldData data, String raider, FactionWorldData.Settlement target) {
      String owner = target.faction();
      int loot = Math.min(data.treasury(owner), LOOT + target.population / 2);
      data.setTreasury(owner, data.treasury(owner) - loot);
      data.setTreasury(raider, data.treasury(raider) + loot);
      int captives = target.population / 4;
      target.population -= captives + target.population / 10;
      target.garrison = 0;
      FactionWorldData.Settlement home = largest(data, raider);
      if (home != null) {
         home.population += captives;
      }

      FactionDiplomacy.adjustOpinion(level, raider, owner, -3);
      data.setDirty();
      AncientWarfareCore.LOG.info("Faction {} plundered {} #{}: {} gold, {} captives", raider, owner, target.id(), loot, captives);
      FactionNews.tell(
         level,
         Component.translatable(
            "news.ancientwarfare.raided",
            FactionNews.factionName(raider),
            FactionNews.factionName(owner),
            target.center().getX(),
            target.center().getZ(),
            loot,
            captives,
            FactionNews.named(target)
         )
      );
   }

   private static FactionWorldData.Settlement largest(FactionWorldData data, String faction) {
      FactionWorldData.Settlement largest = null;

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.faction().equals(faction) && (largest == null || settlement.population > largest.population)) {
            largest = settlement;
         }
      }

      return largest;
   }
}
