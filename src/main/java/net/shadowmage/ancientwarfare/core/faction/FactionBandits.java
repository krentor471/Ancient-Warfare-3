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
 *
 * Countries hunt them in turn: a lair on a country's land, or close to its settlements, draws a
 * punitive march. If the hunters beat the bandits the lair is scattered: its men are dead, half
 * its people flee, it falls back a tier and the hunters take a share of the bandits' gold. A lair
 * with nothing left to fall back from is gone for good.
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
   /** A daily chance of a hunt against a lair, plus this much for each of its levels. */
   private static final double HUNT_CHANCE = 0.1;
   private static final double HUNT_CHANCE_PER_LEVEL = 0.05;
   /** A lair off any country's land is hunted by the country with a settlement this near. */
   private static final int HUNT_RANGE = 500;
   private static final int MIN_HUNTERS = 10;
   private static final int MAX_HUNTERS = 50;
   /** A country only sends hunters while it keeps at least as many soldiers at home. */
   private static final int HUNT_RESERVE_FACTOR = 2;
   private static final int BOUNTY_PER_LEVEL = 60;
   private static final int SCATTERED_POPULATION = 10;
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
         FactionWorldData.Campaign raid = FactionFleets.pirateRaid(level, data, lair, target, force);
         if (raid == null) {
            raid = FactionCampaigns.launchFrom(level, data, lair, target, force);
         }

         if (raid != null) {
            RAIDING.put(lair.id(), raid.id());
            RAID_LEVEL.put(raid.id(), lairLevel);
            AncientWarfareCore.LOG.info("Bandit lair #{} (level {}) raids {} {} #{}", lair.id(), lairLevel, target.faction(), target.kind(), target.id());
         }
      }

      hunt(level, data, random);
   }

   /** Countries send hunters against the lairs on or near their land, one hunt per country at a time. */
   private static void hunt(ServerLevel level, FactionWorldData data, RandomSource random) {
      for (FactionWorldData.Settlement lair : new java.util.ArrayList<>(data.settlements())) {
         if (!FactionDiplomacy.isOutlaw(lair.faction()) || underAttack(data, lair) || random.nextDouble() >= HUNT_CHANCE + HUNT_CHANCE_PER_LEVEL * level(lair)) {
            continue;
         }

         String hunter = hunter(data, lair);
         if (hunter == null || FactionCampaigns.marchingAgainst(data, hunter, lair.faction())) {
            continue;
         }

         int force = Math.max(MIN_HUNTERS, Math.min(MAX_HUNTERS, lair.garrison() * 2 + 6));
         int soldiers = 0;
         for (FactionWorldData.Settlement settlement : data.settlements()) {
            if (settlement.faction().equals(hunter)) {
               soldiers += settlement.garrison();
            }
         }

         if (soldiers < force * HUNT_RESERVE_FACTOR) {
            continue;
         }

         if (FactionCampaigns.launch(level, data, hunter, lair, force)) {
            AncientWarfareCore.LOG.info("Faction {} hunts bandit lair #{} (level {}) with {} soldiers", hunter, lair.id(), level(lair), force);
         }
      }
   }

   /** The country whose land the lair sits on, or failing that the one with the nearest settlement in reach. */
   private static String hunter(FactionWorldData data, FactionWorldData.Settlement lair) {
      String owner = data.provinceOwner(lair.province());
      if (owner != null && !owner.isEmpty() && !FactionDiplomacy.isOutlaw(owner) && hasSettlement(data, owner)) {
         return owner;
      }

      String nearest = null;
      double bestDistance = (double)HUNT_RANGE * HUNT_RANGE;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         double distance = settlement.horizontalDistanceSquared(lair.center().getX(), lair.center().getZ());
         if (distance < bestDistance && !FactionDiplomacy.isOutlaw(settlement.faction())) {
            nearest = settlement.faction();
            bestDistance = distance;
         }
      }

      return nearest;
   }

   private static boolean hasSettlement(FactionWorldData data, String faction) {
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.faction().equals(faction)) {
            return true;
         }
      }

      return false;
   }

   /** Hunters who beat a lair's bandits scatter it; a lair with nothing left to lose is gone. */
   static void scatter(ServerLevel level, FactionWorldData data, String hunter, FactionWorldData.Settlement lair) {
      String outlaw = lair.faction();
      int bounty = Math.min(data.treasury(outlaw), BOUNTY_PER_LEVEL * level(lair) + lair.population / 2);
      data.setTreasury(outlaw, data.treasury(outlaw) - bounty);
      data.setTreasury(hunter, data.treasury(hunter) + bounty);
      boolean gone = lair.tier <= 0 && lair.population <= SCATTERED_POPULATION * 2;
      lair.garrison = 0;
      lair.population /= 2;
      lair.tier = Math.max(0, lair.tier - 1);
      if (gone) {
         data.removeSettlement(lair);
      }

      data.setDirty();
      AncientWarfareCore.LOG.info("Faction {} scattered bandit lair #{}{}", hunter, lair.id(), gone ? " for good" : "");
      FactionNews.tell(
         level,
         Component.translatable(
            gone ? "news.ancientwarfare.lair_destroyed" : "news.ancientwarfare.lair_scattered",
            FactionNews.factionName(hunter),
            lair.center().getX(),
            lair.center().getZ(),
            bounty,
            FactionNews.named(lair)
         )
      );
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
            loot,
            FactionNews.named(target)
         )
      );
   }

   /** The level the lair had when the raid set out, or 2 when it is not known. */
   static int raidLevel(int campaignId, FactionWorldData data) {
      return RAID_LEVEL.getOrDefault(campaignId, 2);
   }
}
