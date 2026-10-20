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
   public static final int GIFT_EMERALDS = 5;
   public static final int GIFT_STANDING = 10;
   public static final int MAX_GIFT_STANDING = 100;
   public static final int WAR_STANDING = -30;
   /** Extra penalty for declaring war while a pact or alliance stands. */
   public static final int BETRAYAL_STANDING = -40;
   private static final double LEADER_RANGE = 8.0;
   public static final double DISCOVERY_RANGE = 64.0;

   private PlayerDiplomacy() {
   }

   public enum Action {
      PEACE,
      PACT,
      ALLIANCE,
      GIFT,
      WAR;

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

   public static void act(ServerPlayer player, String faction, Action action) {
      FactionStandingData standings = FactionStandingData.get(player.serverLevel());
      if (!standings.knownFactions(player.getUUID()).contains(faction) || action == null) {
         return;
      }

      if (action != Action.WAR && !leaderNear(player, faction)) {
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
      switch (action) {
         case PEACE -> {
            if (treaty != RelationState.WAR) {
               return Component.translatable("diplomacy.ancientwarfare.not_at_war", name);
            } else if (standing < PEACE_STANDING) {
               return Component.translatable("diplomacy.ancientwarfare.refused", name, standing, PEACE_STANDING);
            }

            standings.setTreaty(player.getUUID(), faction, RelationState.NEUTRAL);
            standings.setStanding(player.getUUID(), faction, Math.max(standing, PEACE_RESTORED_STANDING));
            return Component.translatable("diplomacy.ancientwarfare.peace", name);
         }
         case PACT -> {
            if (treaty != RelationState.NEUTRAL) {
               return Component.translatable("diplomacy.ancientwarfare.pact_needs_neutral", name);
            } else if (standing < PACT_STANDING) {
               return Component.translatable("diplomacy.ancientwarfare.refused", name, standing, PACT_STANDING);
            }

            standings.setTreaty(player.getUUID(), faction, RelationState.PACT);
            return Component.translatable("diplomacy.ancientwarfare.pact", name);
         }
         case ALLIANCE -> {
            if (treaty != RelationState.PACT) {
               return Component.translatable("diplomacy.ancientwarfare.alliance_needs_pact", name);
            } else if (standing < ALLIANCE_STANDING) {
               return Component.translatable("diplomacy.ancientwarfare.refused", name, standing, ALLIANCE_STANDING);
            }

            standings.setTreaty(player.getUUID(), faction, RelationState.ALLIANCE);
            return Component.translatable("diplomacy.ancientwarfare.alliance", name);
         }
         case GIFT -> {
            if (!takeEmeralds(player, GIFT_EMERALDS)) {
               return Component.translatable("diplomacy.ancientwarfare.gift_missing", GIFT_EMERALDS);
            }

            int raised = Math.max(standing, Math.min(MAX_GIFT_STANDING, standing + GIFT_STANDING));
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
            return Component.translatable(betrayal ? "diplomacy.ancientwarfare.betrayal" : "diplomacy.ancientwarfare.war", name);
         }
      }
   }

   private static boolean takeEmeralds(ServerPlayer player, int count) {
      if (player.isCreative()) {
         return true;
      } else if (player.getInventory().countItem(Items.EMERALD) < count) {
         return false;
      }

      int remaining = count;

      for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (stack.is(Items.EMERALD)) {
            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;
         }
      }

      return true;
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
               leaderNear(player, faction)
            )
         );
      }

      return entries;
   }

   public static record Entry(String faction, int standing, RelationState treaty, List<String> wars, int settlements, boolean leaderNear) {
   }
}
