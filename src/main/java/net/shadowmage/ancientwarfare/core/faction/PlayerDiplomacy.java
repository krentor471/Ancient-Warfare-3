package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;

/**
 * The player's diplomacy with AI factions. A faction must be met first (by coming near one of its
 * settlements or talking to its people). Treaties are negotiated with the faction's leader in
 * person; a faction accepts depending on its standing with the player. War can be declared from
 * anywhere.
 */
public final class PlayerDiplomacy {
   public static final int PEACE_STANDING = -60;
   public static final int PEACE_RESTORED_STANDING = -20;
   public static final int PACT_STANDING = 30;
   public static final int ALLIANCE_STANDING = 60;
   public static final int TRADE_STANDING = 0;
   /** A gift to the ruler: ten gold. */
   public static final int GIFT_EMERALDS = 100;
   public static final int GIFT_STANDING = 10;
   public static final int MAX_GIFT_STANDING = 100;
   public static final int WAR_STANDING = -30;
   /** Extra penalty for declaring war while a pact or alliance stands. */
   public static final int BETRAYAL_STANDING = -40;
   private static final double LEADER_RANGE = 8.0;
   public static final double DISCOVERY_RANGE = 64.0;
   /** In an entry's pending mask: a spy is on the way, beside the bits of the intrigues under way. */
   public static final int SPY_PENDING = 1 << 30;

   private PlayerDiplomacy() {
   }

   public enum Action {
      PEACE,
      PACT,
      ALLIANCE,
      GIFT,
      WAR,
      /** Signs a trade agreement, or ends the one in force. */
      TRADE,
      /** Sends a spy to learn where the faction's armies are bound. */
      SPY,
      /** Enters one of the player's soldiers against the faction's champion. */
      TOURNAMENT,
      /** Brings the faction a load of what its stores lack most. */
      SUPPLY,
      /** Offers to make peace between the faction and an enemy. */
      MEDIATE,
      /** A gift to one of the court: the heir, the marshal, the treasurer or the priest. */
      COURT_HEIR,
      COURT_MARSHAL,
      COURT_TREASURER,
      COURT_PRIEST,
      /** Secret work by the player's agents, one action for each kind. */
      STEAL,
      BRIBE,
      ARSON,
      RUMOUR,
      FORGERY;

      public boolean court() {
         return this.ordinal() >= COURT_HEIR.ordinal() && this.ordinal() <= COURT_PRIEST.ordinal();
      }

      public boolean intrigue() {
         return this.ordinal() >= STEAL.ordinal();
      }

      public FactionIntrigue.Op op() {
         return this.intrigue() ? FactionIntrigue.Op.values()[this.ordinal() - STEAL.ordinal()] : null;
      }

      public static Action byId(int id) {
         return id >= 0 && id < values().length ? values()[id] : null;
      }
   }

   public static void discover(ServerPlayer player, String faction) {
      if (!FactionDiplomacy.isOutlaw(faction) && FactionStandingData.get(player.serverLevel()).discover(player.getUUID(), faction)) {
         player.sendSystemMessage(Component.translatable("diplomacy.ancientwarfare.discovered", FactionNews.factionName(faction)));
      }
   }

   /** Opens the diplomacy screen, with focus selected if the player knows it. */
   public static void open(ServerPlayer player, String focus) {
      AWCoreNetwork.sendDiplomacyData(player, focus, true, entries(player));
   }

   /** The research an action needs, or null: alliances want diplomacy, trade agreements trade, spies espionage. */
   public static String research(Action action) {
      return switch (action) {
         case ALLIANCE -> net.shadowmage.ancientwarfare.core.research.PlayerResearch.DIPLOMACY;
         case TRADE -> "trade";
         case SPY, STEAL, BRIBE, ARSON, RUMOUR, FORGERY -> net.shadowmage.ancientwarfare.core.research.PlayerResearch.ESPIONAGE;
         default -> null;
      };
   }

   public static void act(ServerPlayer player, String faction, Action action) {
      FactionStandingData standings = FactionStandingData.get(player.serverLevel());
      if (!standings.knownFactions(player.getUUID()).contains(faction) || action == null) {
         return;
      }

      String research = research(action);
      if (research != null && !net.shadowmage.ancientwarfare.core.research.PlayerResearch.require(player, research)) {
         return;
      }

      boolean ending = action == Action.WAR || action == Action.SPY || action.intrigue() || action == Action.TRADE && standings.hasTrade(player.getUUID(), faction);
      if (!ending && !leaderNear(player, faction)) {
         player.sendSystemMessage(Component.translatable("diplomacy.ancientwarfare.need_leader", FactionNews.factionName(faction)));
      } else {
         player.sendSystemMessage(perform(player, standings, faction, action));
      }

      AWCoreNetwork.sendDiplomacyData(player, faction, false, entries(player));
   }

   private static Component perform(ServerPlayer player, FactionStandingData standings, String faction, Action action) {
      Component name = FactionNews.factionName(faction);
      RelationState treaty = standings.getTreaty(player.getUUID(), faction);
      int standing = standings.getStanding(player.getUUID(), faction);
      // The court's good word, or ill, moves what the ruler asks.
      int temper = FactionRulers.trait(player.serverLevel(), faction).treatyStanding - FactionCourt.treatyBonus(player.serverLevel(), faction, player.getUUID());
      if (action.court()) {
         return FactionCourt.gift(player, faction, action.ordinal() - Action.COURT_HEIR.ordinal());
      } else if (action.intrigue()) {
         return FactionIntrigue.start(player, faction, action.op());
      }

      switch (action) {
         case PEACE -> {
            if (treaty != RelationState.WAR) {
               return Component.translatable("diplomacy.ancientwarfare.not_at_war", name);
            } else if (standing < PEACE_STANDING + temper) {
               return Component.translatable("diplomacy.ancientwarfare.refused", name, standing, PEACE_STANDING + temper);
            }

            standings.setTreaty(player.getUUID(), faction, RelationState.NEUTRAL);
            standings.setStanding(player.getUUID(), faction, Math.max(standing, PEACE_RESTORED_STANDING));
            return Component.translatable("diplomacy.ancientwarfare.peace", name);
         }
         case PACT -> {
            if (treaty != RelationState.NEUTRAL) {
               return Component.translatable("diplomacy.ancientwarfare.pact_needs_neutral", name);
            } else if (standing < PACT_STANDING + temper) {
               return Component.translatable("diplomacy.ancientwarfare.refused", name, standing, PACT_STANDING + temper);
            }

            standings.setTreaty(player.getUUID(), faction, RelationState.PACT);
            return Component.translatable("diplomacy.ancientwarfare.pact", name);
         }
         case ALLIANCE -> {
            if (treaty != RelationState.PACT) {
               return Component.translatable("diplomacy.ancientwarfare.alliance_needs_pact", name);
            } else if (standing < ALLIANCE_STANDING + temper) {
               return Component.translatable("diplomacy.ancientwarfare.refused", name, standing, ALLIANCE_STANDING + temper);
            }

            standings.setTreaty(player.getUUID(), faction, RelationState.ALLIANCE);
            return Component.translatable("diplomacy.ancientwarfare.alliance", name);
         }
         case TRADE -> {
            if (standings.hasTrade(player.getUUID(), faction)) {
               standings.setTrade(player.getUUID(), faction, false);
               return Component.translatable("diplomacy.ancientwarfare.trade_ended", name);
            } else if (treaty == RelationState.WAR) {
               return Component.translatable("diplomacy.ancientwarfare.trade_at_war", name);
            } else if (standing < TRADE_STANDING) {
               return Component.translatable("diplomacy.ancientwarfare.refused", name, standing, TRADE_STANDING);
            }

            standings.setTrade(player.getUUID(), faction, true);
            return Component.translatable("diplomacy.ancientwarfare.trade", name);
         }
         case SPY -> {
            return FactionSpies.send(player, faction);
         }
         case TOURNAMENT -> {
            return FactionTournaments.enter(player, faction);
         }
         case SUPPLY -> {
            return FactionCourt.supply(player, faction);
         }
         case MEDIATE -> {
            return FactionCourt.mediate(player, faction);
         }
         case GIFT -> {
            if (FactionCourt.recent(player.serverLevel(), player.getUUID(), faction, "gift") >= FactionCourt.TIRED) {
               return Component.translatable("diplomacy.ancientwarfare.gift_tired", name);
            } else if (!takeEmeralds(player, GIFT_EMERALDS)) {
               return Component.translatable("diplomacy.ancientwarfare.gift_missing", GIFT_EMERALDS);
            }

            // A greedy ruler is swayed more by a gift.
            int gift = FactionRulers.trait(player.serverLevel(), faction) == FactionRulers.Trait.GREEDY ? GIFT_STANDING * 3 / 2 : GIFT_STANDING;
            // A treasury in want of gold takes it the more gladly; gift after gift is worth less and less.
            gift += FactionCourt.onGift(player, faction, gift);
            gift = FactionCourt.weigh(player.serverLevel(), player.getUUID(), faction, "gift", gift);

            int raised = Math.max(standing, Math.min(MAX_GIFT_STANDING, standing + gift));
            standings.setStanding(player.getUUID(), faction, raised);
            return Component.translatable("diplomacy.ancientwarfare.gift", name, raised);
         }
         default -> {
            if (treaty == RelationState.WAR) {
               return Component.translatable("diplomacy.ancientwarfare.already_at_war", name);
            }

            boolean betrayal = treaty == RelationState.PACT || treaty == RelationState.ALLIANCE;
            standings.setTreaty(player.getUUID(), faction, RelationState.WAR);
            standings.setStanding(player.getUUID(), faction, Math.min(standing, 0) + WAR_STANDING + (betrayal ? BETRAYAL_STANDING : 0));
            CountryDiplomacy.warDeclaredBy(player, faction);
            FactionCourt.warDeclared(player.serverLevel(), player.getUUID(), faction);
            return Component.translatable(betrayal ? "diplomacy.ancientwarfare.betrayal" : "diplomacy.ancientwarfare.war", name);
         }
      }
   }

   /** Takes a price in silver coins; false when the player has too little. */
   static boolean takeEmeralds(ServerPlayer player, int count) {
      return Coins.take(player, count);
   }

   public static boolean leaderNear(ServerPlayer player, String faction) {
      return !player.serverLevel()
         .getEntitiesOfClass(
            FactionNpcEntity.class, player.getBoundingBox().inflate(LEADER_RANGE), npc -> npc.isAlive() && isLeader(npc) && npc.getFactionName().equals(faction)
         )
         .isEmpty();
   }

   public static boolean isLeader(FactionNpcEntity npc) {
      return FactionNpcRole.LEADER.id().equals(npc.getNpcSubtype()) || FactionNpcRole.LEADER_ELITE.id().equals(npc.getNpcSubtype());
   }

   private static List<Entry> entries(ServerPlayer player) {
      FactionStandingData standings = FactionStandingData.get(player.serverLevel());
      FactionWorldData data = FactionWorldData.get(player.serverLevel());
      List<Entry> entries = new ArrayList<>();

      for (String faction : new TreeSet<>(standings.knownFactions(player.getUUID()))) {
         List<String> wars = new ArrayList<>();

         for (String other : new TreeSet<>(FactionRegistry.factionNames())) {
            if (!other.equals(faction)
               && !FactionDiplomacy.isOutlaw(other)
               && FactionDiplomacy.relation(player.serverLevel(), faction, other).state() == RelationState.WAR) {
               wars.add(other);
            }
         }

         int settlements = (int)data.settlements().stream().filter(settlement -> settlement.faction().equals(faction)).count();
         entries.add(
            new Entry(
               faction,
               standings.getStanding(player.getUUID(), faction),
               standings.getTreaty(player.getUUID(), faction),
               wars,
               settlements,
               leaderNear(player, faction),
               standings.hasTrade(player.getUUID(), faction),
               FactionTrade.playerRoutes(data, player.getUUID(), faction),
               FactionRulers.ruler(data, faction, player.getRandom()).name(),
               FactionRulers.ruler(data, faction, player.getRandom()).trait(),
               new int[]{data.treasury(faction), data.stores(faction)[FactionResources.WOOD], data.stores(faction)[FactionResources.STONE], data.stores(faction)[FactionResources.IRON]},
               FactionCourt.courtView(player.serverLevel(), faction, player.getUUID()),
               FactionCourt.goalLines(player, faction),
               FactionIntrigue.pendingMask(player.serverLevel(), player.getUUID(), faction)
                  | (data.spies().stream().anyMatch(spy -> spy.player().equals(player.getUUID()) && spy.faction().equals(faction)) ? SPY_PENDING : 0)
            )
         );
      }

      return entries;
   }

   public static record Entry(
      String faction, int standing, RelationState treaty, List<String> wars, int settlements, boolean leaderNear, boolean trade, int routes, int ruler, int trait, int[] stores,
      int[] court, List<Component> goals, int pending
   ) {
   }
}
