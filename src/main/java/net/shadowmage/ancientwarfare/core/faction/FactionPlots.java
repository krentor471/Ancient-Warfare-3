package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.research.PlayerResearch;

/**
 * Plots and secrets at foreign courts.
 *
 * A courtier who has grown fond of the player (by gifts or an agent's bribes) may come to them with a
 * plot to overthrow the ruler: the player pays for it, or warns the ruler, or stays out of it. A plot
 * that succeeds crowns the courtier, who owes the player the throne; one that is found out means war
 * and the distrust of every court.
 *
 * A spy at a court now and then digs up its ruler's secret: embezzlement, or a secret pact with an
 * enemy. The player may blackmail the ruler with it, which makes the next deal with them easier, or
 * make it public, which shakes the realm and may bring the ruler down.
 */
public final class FactionPlots extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_faction_plots";
   /** Favour a courtier must have for the player before plotting with them. */
   private static final int PLOT_FAVOR = 30;
   private static final double PLOT_CHANCE = 0.05;
   /** Silver the plot costs: swords, bribes and a quiet place to meet. */
   public static final int PLOT_COST = 500;
   private static final long PLOT_DAYS = 2L;
   private static final long OFFER_DAYS = 1L;
   private static final double PLOT_SUCCESS = 0.5;
   private static final double SECRET_SERVICE_PLOT = 0.15;
   private static final double CAUTIOUS_PLOT = 0.15;
   /** Of the plots that fail, this share is found out. */
   private static final double EXPOSED_SHARE = 0.6;
   private static final int GRATEFUL_STANDING = 50;
   private static final int GRATEFUL_RAISE = 30;
   private static final int EXPOSED_STANDING = -60;
   private static final int DISTRUST_STANDING = -10;
   private static final int WARNED_STANDING = 15;
   private static final int WARNED_FAVOR = -40;

   private static final double SECRET_CHANCE = 0.15;
   private static final long SECRET_DAYS = 20L;
   /** What blackmail is worth in a deal, in silver. */
   public static final int LEVERAGE = 300;
   private static final int BLACKMAIL_STANDING = -5;
   private static final int PUBLISHED_ORDER = 20;
   private static final double FALL_CHANCE = 0.4;
   private static final int PUBLISHED_STANDING = -15;
   private static final int BETRAYED_OPINION = -40;

   enum Secret {
      EMBEZZLEMENT,
      ENEMY_PACT;

      String key() {
         return this.name().toLowerCase(java.util.Locale.ROOT);
      }
   }

   private static final class Plot {
      UUID player;
      String faction;
      FactionCourt.Role role;
      long day;
      /** Paid for and under way, or still only offered. */
      boolean backed;
   }

   private static final class Dirt {
      Secret secret;
      String other = "";
      long found;
      boolean leverage;
   }

   private final List<Plot> plots = new ArrayList<>();
   /** Secrets each player holds, by faction. */
   private final Map<UUID, Map<String, Dirt>> secrets = new HashMap<>();
   private int nextId;
   private final Map<Integer, Plot> byId = new HashMap<>();

   public static FactionPlots get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(FactionPlots::load, FactionPlots::new, DATA_NAME);
   }

   static void simulateDay(ServerLevel level, FactionWorldData data) {
      FactionPlots plots = get(level);
      long day = data.lastSimulatedDay();
      RandomSource random = level.getRandom();
      for (Plot plot : new ArrayList<>(plots.plots)) {
         if (!plot.backed && day - plot.day > OFFER_DAYS) {
            plots.remove(plot);
         } else if (plot.backed && day - plot.day >= PLOT_DAYS) {
            plots.remove(plot);
            plots.strike(level, data, plot);
         }
      }

      FactionStandingData standings = FactionStandingData.get(level);
      for (ServerPlayer player : level.players()) {
         UUID id = player.getUUID();
         Map<String, Dirt> mine = plots.secrets.get(id);
         if (mine != null) {
            mine.values().removeIf(dirt -> day - dirt.found > SECRET_DAYS);
         }

         for (String faction : standings.knownFactions(id)) {
            if (FactionDiplomacy.isOutlaw(faction) || data.settlements().stream().noneMatch(settlement -> settlement.faction().equals(faction))) {
               continue;
            }

            if (plots.plots.stream().noneMatch(plot -> plot.player.equals(id)) && random.nextDouble() < PLOT_CHANCE) {
               FactionCourt.Role role = FactionCourt.plotter(level, faction, id, PLOT_FAVOR);
               if (role != null) {
                  plots.offer(level, player, faction, role, day);
               }
            }

            if (data.intelUntil(id, faction) > level.getGameTime() && (mine == null || !mine.containsKey(faction)) && random.nextDouble() < SECRET_CHANCE) {
               plots.find(level, player, faction, day);
               mine = plots.secrets.get(id);
            }
         }
      }

      plots.setDirty();
   }

   private void remove(Plot plot) {
      this.plots.remove(plot);
      this.byId.values().remove(plot);
   }

   private void offer(ServerLevel level, ServerPlayer player, String faction, FactionCourt.Role role, long day) {
      Plot plot = new Plot();
      plot.player = player.getUUID();
      plot.faction = faction;
      plot.role = role;
      plot.day = day;
      int id = ++this.nextId;
      this.plots.add(plot);
      this.byId.put(id, plot);
      this.setDirty();
      player.sendSystemMessage(
         Component.translatable(
               "plot.ancientwarfare.offer", FactionCourt.roleName(role), FactionCourt.courtierName(level, faction, role), FactionNews.factionName(faction), PLOT_COST / 10
            )
            .withStyle(ChatFormatting.GOLD)
      );
      player.sendSystemMessage(
         Component.literal("  ")
            .append(FactionEnvoys.button("plot.ancientwarfare.back", "/awplot back " + id, ChatFormatting.YELLOW))
            .append("  ")
            .append(FactionEnvoys.button("plot.ancientwarfare.warn", "/awplot warn " + id, ChatFormatting.AQUA))
            .append("  ")
            .append(FactionEnvoys.button("plot.ancientwarfare.ignore", "/awplot ignore " + id, ChatFormatting.GRAY))
      );
   }

   /** The player answers a courtier's plot: "back" it, "warn" the ruler, or "ignore" it. */
   public static void answer(ServerPlayer player, int id, String answer) {
      ServerLevel level = player.serverLevel();
      FactionPlots plots = get(level);
      Plot plot = plots.byId.get(id);
      if (plot == null || plot.backed || !plot.player.equals(player.getUUID())) {
         player.sendSystemMessage(Component.translatable("plot.ancientwarfare.gone").withStyle(ChatFormatting.GRAY));
         return;
      }

      Component name = FactionNews.factionName(plot.faction);
      switch (answer) {
         case "back" -> {
            if (!Coins.take(player, PLOT_COST)) {
               player.sendSystemMessage(Component.translatable("plot.ancientwarfare.no_money", PLOT_COST / 10).withStyle(ChatFormatting.RED));
               return;
            }

            plot.backed = true;
            plot.day = FactionWorldData.get(level).lastSimulatedDay();
            player.sendSystemMessage(Component.translatable("plot.ancientwarfare.backed", name, PLOT_DAYS).withStyle(ChatFormatting.YELLOW));
         }
         case "warn" -> {
            plots.remove(plot);
            FactionStandingData.get(level).adjustStanding(player.getUUID(), plot.faction, WARNED_STANDING);
            FactionCourt.adjustFavor(level, plot.faction, plot.role, player.getUUID(), WARNED_FAVOR);
            player.sendSystemMessage(Component.translatable("plot.ancientwarfare.warned", name, FactionCourt.roleName(plot.role)).withStyle(ChatFormatting.AQUA));
         }
         default -> {
            plots.remove(plot);
            player.sendSystemMessage(Component.translatable("plot.ancientwarfare.ignored").withStyle(ChatFormatting.GRAY));
         }
      }

      plots.setDirty();
   }

   private void strike(ServerLevel level, FactionWorldData data, Plot plot) {
      FactionStandingData standings = FactionStandingData.get(level);
      Component name = FactionNews.factionName(plot.faction);
      ServerPlayer player = level.getServer().getPlayerList().getPlayer(plot.player);
      Component who = player != null ? player.getDisplayName() : Component.literal("?");
      boolean secretService = PlayerResearch.has(level, plot.player, PlayerResearch.SECRET_SERVICE);
      double chance = PLOT_SUCCESS + (secretService ? SECRET_SERVICE_PLOT : 0.0)
         - (FactionRulers.trait(level, plot.faction) == FactionRulers.Trait.CAUTIOUS ? CAUTIOUS_PLOT : 0.0);
      RandomSource random = level.getRandom();
      if (random.nextDouble() < chance) {
         FactionWorldData.Ruler old = FactionRulers.ruler(data, plot.faction, random);
         FactionWorldData.Ruler usurper = FactionCourt.crown(level, plot.faction, plot.role, data.lastSimulatedDay());
         FactionRulers.enthrone(level, plot.faction, usurper);
         FactionNews.tell(
            level,
            Component.translatable("news.ancientwarfare.coup", name, FactionRulers.name(old.name()), FactionRulers.name(usurper.name()), FactionRulers.traitName(usurper.trait()))
         );
         int standing = standings.getStanding(plot.player, plot.faction);
         standings.setStanding(plot.player, plot.faction, Math.max(standing + GRATEFUL_RAISE, GRATEFUL_STANDING));
         if (standings.getTreaty(plot.player, plot.faction) == RelationState.WAR) {
            standings.setTreaty(plot.player, plot.faction, RelationState.NEUTRAL);
         }

         FactionNews.tellPlayer(
            level, plot.player, Component.translatable("plot.ancientwarfare.success", FactionRulers.name(usurper.name()), name).withStyle(ChatFormatting.GREEN)
         );
      } else if (random.nextDouble() < EXPOSED_SHARE * (secretService ? 0.5 : 1.0)) {
         standings.setTreaty(plot.player, plot.faction, RelationState.WAR);
         standings.setStanding(plot.player, plot.faction, Math.min(standings.getStanding(plot.player, plot.faction), 0) + EXPOSED_STANDING);
         for (String other : FactionRegistry.factionNames()) {
            if (!other.equals(plot.faction) && !FactionDiplomacy.isOutlaw(other)) {
               standings.adjustStanding(plot.player, other, DISTRUST_STANDING);
            }
         }

         FactionNews.tell(level, Component.translatable("news.ancientwarfare.plot_exposed", name, FactionCourt.roleName(plot.role), who));
         CountryDiplomacy.warDeclaredOn(level, plot.player, plot.faction);
      } else {
         FactionNews.tellPlayer(level, plot.player, Component.translatable("plot.ancientwarfare.failed", name).withStyle(ChatFormatting.GRAY));
      }
   }

   private void find(ServerLevel level, ServerPlayer player, String faction, long day) {
      Dirt dirt = new Dirt();
      dirt.found = day;
      List<String> enemies = new ArrayList<>();
      for (String other : FactionRegistry.factionNames()) {
         if (!other.equals(faction) && !FactionDiplomacy.isOutlaw(other) && FactionDiplomacy.canNegotiate(faction, other)
            && FactionDiplomacy.relation(level, faction, other).state() != RelationState.ALLIANCE) {
            // Someone else's ally makes the most damning partner.
            enemies.add(other);
         }
      }

      if (!enemies.isEmpty() && level.getRandom().nextBoolean()) {
         dirt.secret = Secret.ENEMY_PACT;
         dirt.other = enemies.get(level.getRandom().nextInt(enemies.size()));
      } else {
         dirt.secret = Secret.EMBEZZLEMENT;
      }

      this.secrets.computeIfAbsent(player.getUUID(), id -> new HashMap<>()).put(faction, dirt);
      this.setDirty();
      player.sendSystemMessage(Component.translatable("secret.ancientwarfare.found", FactionNews.factionName(faction), describe(dirt)).withStyle(ChatFormatting.GOLD));
      player.sendSystemMessage(buttons(faction));
   }

   private static Component describe(Dirt dirt) {
      return Component.translatable("secret.ancientwarfare." + dirt.secret.key(), FactionNews.factionName(dirt.other));
   }

   private static MutableComponent buttons(String faction) {
      return Component.literal("  ")
         .append(FactionEnvoys.button("secret.ancientwarfare.blackmail", "/awsecret blackmail " + faction, ChatFormatting.YELLOW))
         .append("  ")
         .append(FactionEnvoys.button("secret.ancientwarfare.publish", "/awsecret publish " + faction, ChatFormatting.RED));
   }

   /** Whether the player holds the ruler's secret over them for the next deal. */
   static boolean leverage(ServerLevel level, UUID player, String faction) {
      Dirt dirt = get(level).secrets.getOrDefault(player, Map.of()).get(faction);
      return dirt != null && dirt.leverage;
   }

   /** A deal was struck under blackmail: the secret is spent. */
   static void spendLeverage(ServerLevel level, UUID player, String faction) {
      Map<String, Dirt> mine = get(level).secrets.get(player);
      if (mine != null && mine.get(faction) != null && mine.get(faction).leverage) {
         mine.remove(faction);
         FactionStandingData.get(level).adjustStanding(player, faction, BLACKMAIL_STANDING);
         get(level).setDirty();
      }
   }

   /** The player uses a secret: "blackmail" holds it over the ruler, "publish" tells the world. */
   public static void use(ServerPlayer player, String faction, String how) {
      ServerLevel level = player.serverLevel();
      FactionPlots plots = get(level);
      Map<String, Dirt> mine = plots.secrets.get(player.getUUID());
      Dirt dirt = mine == null ? null : mine.get(faction);
      Component name = FactionNews.factionName(faction);
      if (dirt == null) {
         player.sendSystemMessage(Component.translatable("secret.ancientwarfare.none_on", name).withStyle(ChatFormatting.GRAY));
         return;
      }

      if ("blackmail".equals(how)) {
         dirt.leverage = true;
         plots.setDirty();
         player.sendSystemMessage(Component.translatable("secret.ancientwarfare.blackmailed", name, LEVERAGE / 10).withStyle(ChatFormatting.YELLOW));
         return;
      }

      mine.remove(faction);
      plots.setDirty();
      FactionWorldData data = FactionWorldData.get(level);
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.faction().equals(faction)) {
            settlement.order = Math.max(0, settlement.order - PUBLISHED_ORDER);
         }
      }

      data.setDirty();
      FactionNews.tell(level, Component.translatable("news.ancientwarfare.secret_published", name, describe(dirt), player.getDisplayName()));
      if (dirt.secret == Secret.ENEMY_PACT) {
         // The faction's other friends hear it was dealing behind their backs.
         for (String other : FactionRegistry.factionNames()) {
            if (!other.equals(faction) && !other.equals(dirt.other) && !FactionDiplomacy.isOutlaw(other)
               && FactionDiplomacy.relation(level, other, dirt.other).state() == RelationState.WAR) {
               FactionDiplomacy.adjustOpinion(level, other, faction, BETRAYED_OPINION);
            }
         }
      }

      FactionStandingData.get(level).adjustStanding(player.getUUID(), faction, PUBLISHED_STANDING);
      if (level.getRandom().nextDouble() < FALL_CHANCE) {
         FactionWorldData.Ruler old = FactionRulers.ruler(data, faction, level.getRandom());
         FactionWorldData.Ruler heir = FactionCourt.succession(level, faction, data.lastSimulatedDay());
         FactionRulers.enthrone(level, faction, heir);
         FactionNews.tell(
            level, Component.translatable("news.ancientwarfare.ruler_disgraced", name, FactionRulers.name(old.name()), FactionRulers.name(heir.name()), FactionRulers.traitName(heir.trait()))
         );
      }
   }

   /** Lists the secrets the player holds, with buttons. */
   public static void show(ServerPlayer player) {
      Map<String, Dirt> mine = get(player.serverLevel()).secrets.getOrDefault(player.getUUID(), Map.of());
      if (mine.isEmpty()) {
         player.sendSystemMessage(Component.translatable("secret.ancientwarfare.none").withStyle(ChatFormatting.GRAY));
         return;
      }

      mine.forEach((faction, dirt) -> {
         MutableComponent line = FactionNews.factionName(faction).copy().append(": ").append(describe(dirt));
         if (dirt.leverage) {
            line.append(" ").append(Component.translatable("secret.ancientwarfare.held").withStyle(ChatFormatting.YELLOW));
         }

         player.sendSystemMessage(line);
         player.sendSystemMessage(buttons(faction));
      });
   }

   /** A line for the diplomacy window about a secret or a plot at this court, or null. */
   static Component line(ServerLevel level, UUID player, String faction) {
      Dirt dirt = get(level).secrets.getOrDefault(player, Map.of()).get(faction);
      if (dirt != null) {
         return Component.translatable(dirt.leverage ? "secret.ancientwarfare.line.held" : "secret.ancientwarfare.line", describe(dirt)).withStyle(ChatFormatting.LIGHT_PURPLE);
      }

      for (Plot plot : get(level).plots) {
         if (plot.backed && plot.player.equals(player) && plot.faction.equals(faction)) {
            return Component.translatable("plot.ancientwarfare.line", FactionCourt.roleName(plot.role)).withStyle(ChatFormatting.LIGHT_PURPLE);
         }
      }

      return null;
   }

   /** Test hooks: a courtier offers a plot now, or a spy finds a secret now. */
   public static boolean forcePlot(ServerPlayer player, String faction) {
      ServerLevel level = player.serverLevel();
      FactionCourt.Role role = FactionCourt.plotter(level, faction, player.getUUID(), Integer.MIN_VALUE);
      if (role == null) {
         return false;
      }

      get(level).offer(level, player, faction, role, FactionWorldData.get(level).lastSimulatedDay());
      return true;
   }

   public static void forceSecret(ServerPlayer player, String faction) {
      ServerLevel level = player.serverLevel();
      get(level).find(level, player, faction, FactionWorldData.get(level).lastSimulatedDay());
   }

   private static FactionPlots load(CompoundTag tag) {
      FactionPlots plots = new FactionPlots();
      plots.nextId = tag.getInt("NextId");
      for (Tag element : tag.getList("Plots", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         try {
            Plot plot = new Plot();
            plot.player = entry.getUUID("Player");
            plot.faction = entry.getString("Faction");
            plot.role = FactionCourt.Role.valueOf(entry.getString("Role"));
            plot.day = entry.getLong("Day");
            plot.backed = entry.getBoolean("Backed");
            plots.plots.add(plot);
            plots.byId.put(entry.getInt("Id"), plot);
         } catch (IllegalArgumentException e) {
            AncientWarfareCore.LOG.warn("Dropping unknown plot {}", entry);
         }
      }

      for (Tag element : tag.getList("Secrets", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         try {
            Dirt dirt = new Dirt();
            dirt.secret = Secret.valueOf(entry.getString("Secret"));
            dirt.other = entry.getString("Other");
            dirt.found = entry.getLong("Found");
            dirt.leverage = entry.getBoolean("Leverage");
            plots.secrets.computeIfAbsent(entry.getUUID("Player"), id -> new HashMap<>()).put(entry.getString("Faction"), dirt);
         } catch (IllegalArgumentException e) {
            AncientWarfareCore.LOG.warn("Dropping unknown secret {}", entry);
         }
      }

      return plots;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      tag.putInt("NextId", this.nextId);
      ListTag list = new ListTag();
      this.byId.forEach((id, plot) -> {
         CompoundTag entry = new CompoundTag();
         entry.putInt("Id", id);
         entry.putUUID("Player", plot.player);
         entry.putString("Faction", plot.faction);
         entry.putString("Role", plot.role.name());
         entry.putLong("Day", plot.day);
         entry.putBoolean("Backed", plot.backed);
         list.add(entry);
      });
      tag.put("Plots", list);
      ListTag secretList = new ListTag();
      this.secrets.forEach((player, mine) -> mine.forEach((faction, dirt) -> {
         CompoundTag entry = new CompoundTag();
         entry.putUUID("Player", player);
         entry.putString("Faction", faction);
         entry.putString("Secret", dirt.secret.name());
         entry.putString("Other", dirt.other);
         entry.putLong("Found", dirt.found);
         entry.putBoolean("Leverage", dirt.leverage);
         secretList.add(entry);
      }));
      tag.put("Secrets", secretList);
      return tag;
   }
}
