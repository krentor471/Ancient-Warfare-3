package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;

/**
 * Bandit lairs grow. A small lair only waylays caravans; a lair that has grown (its tier) and
 * gathered enough men (its garrison) sends raiders against trading posts and taverns, then guard
 * posts too, and at the top villages. Raiders do not take what they attack: they kill the
 * defenders, carry off gold and drive off some of the people, then go home.
 */
public final class FactionBandits {
   public static final int MAX_LEVEL = 4;
   /** Bandits a lair needs for each level above the first. */
   private static final int MEN_PER_LEVEL = 10;
   private static final double RAID_CHANCE = 0.3;
   private static final int RAID_RANGE = 700;
   private static final double RAID_SHARE = 0.6;
   private static final int MIN_RAIDERS = 5;
   private static final int LOOT_PER_LEVEL = 40;
   /** Lairs and the march their raiders are on; not saved, a restart only lets a lair send another. */
   private static final Map<Integer, Integer> RAIDING = new HashMap<>();
   /** Campaign id to the level its lair had when the raid set out. */
   private static final Map<Integer, Integer> RAID_LEVEL = new HashMap<>();
   private static final Set<String> VILLAGE_KINDS = Set.of(FactionTowns.VILLAGE, "town", "structure", "captured");

   private FactionBandits() {
   }

   /** 1: waylays caravans; 2: raids trading posts and taverns; 3: guard posts too; 4: villages too. */
   public static int level(FactionWorldData.Settlement lair) {
      return Math.max(1, Math.min(MAX_LEVEL, Math.min(1 + lair.tier(), 1 + lair.garrison() / MEN_PER_LEVEL)));
   }

   public static void simulateDay(ServerLevel level, FactionWorldData data) {
      RandomSource random = level.getRandom();

      for (FactionWorldData.Settlement lair : new java.util.ArrayList<>(data.settlements())) {
         if (!FactionDiplomacy.isOutlaw(lair.faction()) || level(lair) < 2 || raiding(data, lair) || random.nextDouble() >= RAID_CHANCE) {
            continue;
         }

         FactionWorldData.Settlement target = target(data, lair);
         int force = (int)Math.floor(lair.garrison() * RAID_SHARE);
         if (target == null || force < MIN_RAIDERS) {
            continue;
         }

         int lairLevel = level(lair);
         FactionWorldData.Campaign raid = FactionCampaigns.launchFrom(level, data, lair, target, force);
         if (raid != null) {
            RAIDING.put(lair.id(), raid.id());
            RAID_LEVEL.put(raid.id(), lairLevel);
            AncientWarfareCore.LOG.info("Bandit lair #{} (level {}) raids {} {} #{}", lair.id(), lairLevel, target.faction(), target.kind(), target.id());
         }
      }
   }

   private static boolean raiding(FactionWorldData data, FactionWorldData.Settlement lair) {
      Integer id = RAIDING.get(lair.id());
      if (id == null) {
         return false;
      }

      for (FactionWorldData.Campaign campaign : data.campaigns()) {
         if (campaign.id() == id) {
            return true;
         }
      }

      RAIDING.remove(lair.id());
      return false;
   }

   /** The nearest settlement in reach that a lair of this level dares to raid. */
   private static FactionWorldData.Settlement target(FactionWorldData data, FactionWorldData.Settlement lair) {
      int lairLevel = level(lair);
      FactionWorldData.Settlement best = null;
      double bestDistance = (double)RAID_RANGE * RAID_RANGE;

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         double distance = settlement.horizontalDistanceSquared(lair.center().getX(), lair.center().getZ());
         if (distance < bestDistance && !FactionDiplomacy.isOutlaw(settlement.faction()) && dares(lairLevel, settlement) && !underAttack(data, settlement)) {
            best = settlement;
            bestDistance = distance;
         }
      }

      return best;
   }

   private static boolean dares(int lairLevel, FactionWorldData.Settlement settlement) {
      String kind = settlement.kind();
      return FactionTrade.TRADE_POST.equals(kind)
         || lairLevel >= 3 && FactionTrade.GUARD_POST.equals(kind)
         || lairLevel >= 4 && VILLAGE_KINDS.contains(kind);
   }

   private static boolean underAttack(FactionWorldData data, FactionWorldData.Settlement settlement) {
      for (FactionWorldData.Campaign campaign : data.campaigns()) {
         if (campaign.targetSettlement() == settlement.id()) {
            return true;
         }
      }

      return false;
   }

   /** Raiders who beat the defenders carry off gold and drive off a third of the people. */
   static void plunder(ServerLevel level, FactionWorldData data, String outlaw, FactionWorldData.Settlement target, int lairLevel) {
      String owner = target.faction();
      int loot = Math.min(data.treasury(owner), LOOT_PER_LEVEL * lairLevel + target.population / 2);
      data.setTreasury(owner, data.treasury(owner) - loot);
      data.setTreasury(outlaw, data.treasury(outlaw) + loot);
      target.population -= target.population / 3;
      target.garrison = 0;
      data.setDirty();
      FactionNews.tell(
         level,
         Component.translatable(
            "news.ancientwarfare.plundered",
            FactionNews.factionName(outlaw),
            FactionNews.factionName(owner),
            target.center().getX(),
            target.center().getZ(),
            loot
         )
      );
   }

   /** The level the lair had when the raid set out, or 2 when it is not known. */
   static int raidLevel(int campaignId, FactionWorldData data) {
      return RAID_LEVEL.getOrDefault(campaignId, 2);
   }
}
