package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;

/**
 * Spies: for a fee a player sends a spy into a faction's lands. He comes back with a report of
 * the faction's armies and where they are bound, and for some days after the world map shows them.
 * Some are caught, which the faction does not forget.
 */
public final class FactionSpies {
   public static final int SPY_EMERALDS = 10;
   private static final long TRAVEL_TICKS = 3000L;
   private static final long INTEL_TICKS = FactionSimulation.DAY_TICKS * 3L;
   private static final double CAUGHT_CHANCE = 0.2;
   private static final int CAUGHT_STANDING = -15;

   private FactionSpies() {
   }

   /** The player pays and sends a spy off. */
   static Component send(ServerPlayer player, String faction) {
      ServerLevel level = player.serverLevel();
      FactionWorldData data = FactionWorldData.get(level);
      Component name = FactionNews.factionName(faction);
      for (FactionWorldData.Spy spy : data.spies()) {
         if (spy.player().equals(player.getUUID()) && spy.faction().equals(faction)) {
            return Component.translatable("spy.ancientwarfare.already", name);
         }
      }

      if (!PlayerDiplomacy.takeEmeralds(player, SPY_EMERALDS)) {
         return Component.translatable("spy.ancientwarfare.missing", SPY_EMERALDS);
      }

      data.spies().add(new FactionWorldData.Spy(player.getUUID(), faction, level.getGameTime() + TRAVEL_TICKS));
      data.setDirty();
      return Component.translatable("spy.ancientwarfare.sent", name);
   }

   /** Whether the player's spies keep them informed of the faction's armies just now. */
   public static boolean knows(ServerLevel level, FactionWorldData data, ServerPlayer player, String faction) {
      return data.intelUntil(player.getUUID(), faction) > level.getGameTime();
   }

   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % 20L != 0L || data.spies().isEmpty()) {
         return;
      }

      long now = level.getGameTime();
      for (FactionWorldData.Spy spy : new ArrayList<>(data.spies())) {
         if (now < spy.ready()) {
            continue;
         }

         data.spies().remove(spy);
         data.setDirty();
         Component name = FactionNews.factionName(spy.faction());
         double caught = CAUGHT_CHANCE * (FactionRulers.trait(data, spy.faction(), level.getRandom()) == FactionRulers.Trait.CAUTIOUS ? 2.0 : 1.0);
         if (level.getRandom().nextDouble() < caught) {
            FactionStandingData.get(level).adjustStanding(spy.player(), spy.faction(), CAUGHT_STANDING);
            FactionNews.tellPlayer(level, spy.player(), Component.translatable("spy.ancientwarfare.caught", name).withStyle(ChatFormatting.RED));
            continue;
         }

         data.setIntel(spy.player(), spy.faction(), now + INTEL_TICKS);
         ServerPlayer player = level.getServer().getPlayerList().getPlayer(spy.player());
         for (Component line : report(data, spy, level)) {
            if (player != null) {
               player.sendSystemMessage(line);
            }
         }
      }
   }

   private static List<Component> report(FactionWorldData data, FactionWorldData.Spy spy, ServerLevel level) {
      String faction = spy.faction();
      List<Component> lines = new ArrayList<>();
      FactionWorldData.Ruler ruler = FactionRulers.ruler(data, faction, level.getRandom());
      lines.add(
         Component.translatable(
               "spy.ancientwarfare.report",
               FactionNews.factionName(faction),
               FactionRulers.name(ruler.name()),
               FactionRulers.traitName(ruler.trait()),
               FactionAI.strengths(data).getOrDefault(faction, 0),
               data.treasury(faction)
            )
            .withStyle(ChatFormatting.AQUA)
      );

      for (FactionWorldData.Campaign campaign : data.campaigns()) {
         if (campaign.faction().equals(faction)) {
            FactionWorldData.Settlement target = settlement(data, campaign.targetSettlement());
            lines.add(
               target == null
                  ? Component.translatable("spy.ancientwarfare.march_lost", campaign.size())
                  : Component.translatable(
                     "spy.ancientwarfare.march",
                     campaign.size(),
                     FactionNews.factionName(target.faction()),
                     target.center().getX(),
                     target.center().getZ(),
                     FactionNews.named(target)
                  )
            );
         }
      }

      for (FactionWorldData.Army army : data.armies()) {
         if (!army.faction().equals(faction)) {
            continue;
         }

         if (spy.player().equals(army.villageOwner())) {
            lines.add(
               Component.translatable(army.siege() ? "spy.ancientwarfare.siege_you" : "spy.ancientwarfare.raid_you", army.size(), army.target().getX(), army.target().getZ())
                  .withStyle(ChatFormatting.RED)
            );
         } else if (army.isRaid()) {
            lines.add(Component.translatable("spy.ancientwarfare.raid_other", army.size(), army.target().getX(), army.target().getZ()));
         } else {
            FactionWorldData.Settlement target = settlement(data, army.targetSettlement());
            lines.add(
               Component.translatable(
                  "spy.ancientwarfare.assault", army.size(), FactionNews.factionName(army.defender()), army.target().getX(), army.target().getZ(), FactionNews.named(target)
               )
            );
         }
      }

      if (lines.size() == 1) {
         lines.add(Component.translatable("spy.ancientwarfare.quiet"));
      }

      lines.add(Component.translatable("spy.ancientwarfare.map").withStyle(ChatFormatting.GRAY));
      return lines;
   }

   private static FactionWorldData.Settlement settlement(FactionWorldData data, int id) {
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.id() == id) {
            return settlement;
         }
      }

      return null;
   }
}
