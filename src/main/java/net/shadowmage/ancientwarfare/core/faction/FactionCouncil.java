package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;

/**
 * The council of factions. At the start of each season the rulers' envoys gather in one faction's
 * capital and vote on one matter: peace between two warring factions, a common war on the outlaws,
 * naming a too-ambitious player the common enemy, or raising or lowering the tolls on trade. Each
 * faction's vote weighs by its size. A player who comes to the capital while the council sits has
 * a vote of their own, and the factions that think well of them may follow their lead.
 */
public final class FactionCouncil extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_faction_council";
   /** How close to the host capital's centre a player must stand to take part. */
   public static final int HALL_RANGE = 64;
   private static final long SITTING_DAYS = 1L;
   private static final int FOLLOW_STANDING = 40;
   private static final double FOLLOW_CHANCE = 0.6;
   private static final int SIDED_STANDING = 3;
   private static final double TOLLS_UP = 1.15;
   private static final double TOLLS_DOWN = 0.9;
   /** A player at war with this many factions, or this threatening, may be put to the vote as the common enemy. */
   private static final int ENEMY_WARS = 2;

   public enum Matter {
      PEACE,
      OUTLAWS,
      ENEMY,
      TOLLS_UP,
      TOLLS_DOWN;

      public String key() {
         return this.name().toLowerCase(java.util.Locale.ROOT);
      }
   }

   /** The sitting council, or null between councils. */
   private String host;
   private BlockPos hall;
   private Matter matter;
   /** The two factions at war, for PEACE. */
   private String first = "";
   private String second = "";
   /** The player named, for ENEMY. */
   private UUID enemy;
   private long closes;
   private final Map<UUID, Boolean> playerVotes = new HashMap<>();
   private double tolls = 1.0;

   public static FactionCouncil get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(FactionCouncil::load, FactionCouncil::new, DATA_NAME);
   }

   /** What the council decided on tolls: traders' prices are multiplied by it until the next council. */
   public static double tolls(ServerLevel level) {
      return get(level).tolls;
   }

   static void simulateDay(ServerLevel level, FactionWorldData data) {
      FactionCouncil council = get(level);
      long day = data.lastSimulatedDay();
      if (council.host != null && day >= council.closes) {
         council.resolve(level, data);
      }

      if (council.host == null && Math.floorMod(day, (long)FactionSeasons.SEASON_DAYS) == 0L && !FactionHistory.running()) {
         council.convene(level, data, null);
      }
   }

   /** Gathers the council in the capital of a faction, chosen by size, with a matter to decide. */
   private boolean convene(ServerLevel level, FactionWorldData data, Matter forced) {
      RandomSource random = level.getRandom();
      List<FactionWorldData.Settlement> capitals = new ArrayList<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if ("capital".equals(settlement.kind()) && !FactionDiplomacy.isOutlaw(settlement.faction())) {
            capitals.add(settlement);
         }
      }

      if (capitals.isEmpty()) {
         return false;
      }

      FactionWorldData.Settlement capital = capitals.get(random.nextInt(capitals.size()));
      List<Runnable> choices = new ArrayList<>();
      List<String[]> wars = new ArrayList<>();
      List<String> factions = members();
      for (int i = 0; i < factions.size(); i++) {
         for (int j = i + 1; j < factions.size(); j++) {
            if (FactionDiplomacy.relation(level, factions.get(i), factions.get(j)).state() == RelationState.WAR) {
               wars.add(new String[]{factions.get(i), factions.get(j)});
            }
         }
      }

      if (!wars.isEmpty() && (forced == null || forced == Matter.PEACE)) {
         String[] war = wars.get(random.nextInt(wars.size()));
         choices.add(() -> {
            this.matter = Matter.PEACE;
            this.first = war[0];
            this.second = war[1];
         });
      }

      if (forced == null || forced == Matter.OUTLAWS) {
         choices.add(() -> this.matter = Matter.OUTLAWS);
      }

      if (forced == null || forced == Matter.TOLLS_UP || forced == Matter.TOLLS_DOWN) {
         choices.add(() -> this.matter = forced != null ? forced : this.tolls > 1.0 || random.nextBoolean() ? Matter.TOLLS_DOWN : Matter.TOLLS_UP);
      }

      UUID feared = forced == null || forced == Matter.ENEMY ? this.mostFeared(level, data) : null;
      if (feared != null) {
         // A dangerous player is the first thing on everyone's mind.
         choices.clear();
         choices.add(() -> {
            this.matter = Matter.ENEMY;
            this.enemy = feared;
         });
      }

      if (choices.isEmpty()) {
         return false;
      }

      this.first = "";
      this.second = "";
      this.enemy = null;
      choices.get(random.nextInt(choices.size())).run();
      this.host = capital.faction();
      this.hall = capital.center();
      this.closes = data.lastSimulatedDay() + SITTING_DAYS;
      this.playerVotes.clear();
      this.setDirty();
      FactionNews.tell(
         level, Component.translatable("news.ancientwarfare.council_called", FactionNews.factionName(this.host), this.hall.getX(), this.hall.getZ(), this.question(level))
      );
      for (ServerPlayer player : level.players()) {
         player.sendSystemMessage(Component.translatable("council.ancientwarfare.invite", HALL_RANGE).withStyle(ChatFormatting.GRAY));
      }

      return true;
   }

   /** The player most feared: at war with several factions, or growing fast enough to frighten them. */
   private UUID mostFeared(ServerLevel level, FactionWorldData data) {
      FactionStandingData standings = FactionStandingData.get(level);
      UUID feared = null;
      int worst = 0;
      Set<UUID> players = new LinkedHashSet<>(data.villages().values());
      level.players().forEach(player -> players.add(player.getUUID()));
      for (UUID player : players) {
         int wars = (int)members().stream().filter(faction -> standings.getTreaty(player, faction) == RelationState.WAR).count();
         int threat = FactionCoalitions.threat(level, data, player);
         int score = wars * 4 + threat;
         if ((wars >= ENEMY_WARS || threat >= FactionCoalitions.THREAT_LIMIT) && score > worst) {
            feared = player;
            worst = score;
         }
      }

      return feared;
   }

   private static List<String> members() {
      List<String> members = new ArrayList<>();
      for (String faction : FactionRegistry.factionNames()) {
         if (!FactionDiplomacy.isOutlaw(faction)) {
            members.add(faction);
         }
      }

      return members;
   }

   private Component question(ServerLevel level) {
      return switch (this.matter) {
         case PEACE -> Component.translatable("council.ancientwarfare.matter.peace", FactionNews.factionName(this.first), FactionNews.factionName(this.second));
         case ENEMY -> Component.translatable("council.ancientwarfare.matter.enemy", playerName(level, this.enemy));
         default -> Component.translatable("council.ancientwarfare.matter." + this.matter.key());
      };
   }

   private static Component playerName(ServerLevel level, UUID player) {
      ServerPlayer online = player == null ? null : level.getServer().getPlayerList().getPlayer(player);
      if (online != null) {
         return online.getDisplayName();
      }

      return Component.literal(player == null ? "?" : level.getServer().getProfileCache().get(player).map(profile -> profile.getName()).orElse("?"));
   }

   /** How a faction votes, left to itself. */
   private boolean vote(ServerLevel level, FactionWorldData data, String faction, RandomSource random) {
      FactionStandingData standings = FactionStandingData.get(level);
      FactionRulers.Trait trait = FactionRulers.trait(level, faction);
      return switch (this.matter) {
         case PEACE -> {
            if (faction.equals(this.first) || faction.equals(this.second)) {
               // The warring sides themselves: the weary want it, the warlike do not.
               yield random.nextDouble() < 0.5 / Math.max(0.5, FactionCourt.warFactor(level, faction)) && trait != FactionRulers.Trait.WARLIKE;
            }

            RelationState withFirst = FactionDiplomacy.relation(level, faction, this.first).state();
            RelationState withSecond = FactionDiplomacy.relation(level, faction, this.second).state();
            if (withFirst == RelationState.WAR || withSecond == RelationState.WAR) {
               // At war with one side itself: it wants it kept busy.
               yield false;
            }

            yield random.nextDouble() < (trait == FactionRulers.Trait.WARLIKE ? 0.4 : 0.7);
         }
         case OUTLAWS -> random.nextDouble() < 0.85;
         case ENEMY -> {
            RelationState treaty = standings.getTreaty(this.enemy, faction);
            if (treaty == RelationState.ALLIANCE || treaty == RelationState.PACT) {
               yield false;
            }

            yield treaty == RelationState.WAR || standings.getStanding(this.enemy, faction) < 0 || random.nextDouble() < 0.3;
         }
         case TOLLS_UP -> random.nextDouble() < (trait == FactionRulers.Trait.GREEDY ? 0.8 : data.treasury(faction) < 1000 ? 0.6 : 0.4);
         case TOLLS_DOWN -> random.nextDouble() < (trait == FactionRulers.Trait.GREEDY ? 0.25 : 0.55);
      };
   }

   /** A faction's voice: one, and one more for every ten settlements. */
   private static int weight(FactionWorldData data, String faction) {
      return 1 + (int)data.settlements().stream().filter(settlement -> settlement.faction().equals(faction)).count() / 10;
   }

   private static int playerWeight(FactionWorldData data, UUID player) {
      return 1 + (int)data.villages().values().stream().filter(player::equals).count() / 2;
   }

   /** The player casts their vote; they must be in the hall. */
   public static void cast(ServerPlayer player, boolean yes) {
      ServerLevel level = player.serverLevel();
      FactionCouncil council = get(level);
      if (council.host == null) {
         player.sendSystemMessage(Component.translatable("council.ancientwarfare.none").withStyle(ChatFormatting.GRAY));
         return;
      } else if (player.blockPosition().distSqr(council.hall.atY(player.getBlockY())) > (double)HALL_RANGE * HALL_RANGE) {
         player.sendSystemMessage(
            Component.translatable("council.ancientwarfare.too_far", FactionNews.factionName(council.host), council.hall.getX(), council.hall.getZ()).withStyle(ChatFormatting.RED)
         );
         return;
      } else if (council.matter == Matter.ENEMY && player.getUUID().equals(council.enemy)) {
         player.sendSystemMessage(Component.translatable("council.ancientwarfare.no_say").withStyle(ChatFormatting.RED));
         return;
      }

      council.playerVotes.put(player.getUUID(), yes);
      council.setDirty();
      player.sendSystemMessage(Component.translatable(yes ? "council.ancientwarfare.voted_yes" : "council.ancientwarfare.voted_no").withStyle(ChatFormatting.GREEN));
   }

   private void resolve(ServerLevel level, FactionWorldData data) {
      RandomSource random = level.getRandom();
      FactionStandingData standings = FactionStandingData.get(level);
      int yes = 0;
      int no = 0;
      Set<String> ayes = new LinkedHashSet<>();
      Set<String> nays = new LinkedHashSet<>();
      for (String faction : members()) {
         if (data.settlements().stream().noneMatch(settlement -> settlement.faction().equals(faction))
            || this.matter == Matter.ENEMY && this.enemy != null && !standings.knownFactions(this.enemy).contains(faction)) {
            continue;
         }

         boolean vote = this.vote(level, data, faction, random);
         // A faction fond of a player who spoke may follow them.
         for (Map.Entry<UUID, Boolean> spoken : this.playerVotes.entrySet()) {
            if (standings.getStanding(spoken.getKey(), faction) >= FOLLOW_STANDING && random.nextDouble() < FOLLOW_CHANCE) {
               vote = spoken.getValue();
            }
         }

         if (vote) {
            yes += weight(data, faction);
            ayes.add(faction);
         } else {
            no += weight(data, faction);
            nays.add(faction);
         }
      }

      for (Map.Entry<UUID, Boolean> spoken : this.playerVotes.entrySet()) {
         if (spoken.getValue()) {
            yes += playerWeight(data, spoken.getKey());
         } else {
            no += playerWeight(data, spoken.getKey());
         }

         // The factions on the player's side think better of them; those against, worse.
         for (String faction : spoken.getValue() ? ayes : nays) {
            standings.adjustStanding(spoken.getKey(), faction, SIDED_STANDING);
         }

         for (String faction : spoken.getValue() ? nays : ayes) {
            standings.adjustStanding(spoken.getKey(), faction, -SIDED_STANDING);
         }
      }

      boolean passed = yes > no;
      Component question = this.question(level);
      FactionNews.tell(
         level, Component.translatable(passed ? "news.ancientwarfare.council_passed" : "news.ancientwarfare.council_failed", question, yes, no, FactionNews.factionName(this.host))
      );
      if (passed) {
         this.carryOut(level, data, ayes);
      }

      this.host = null;
      this.hall = null;
      this.playerVotes.clear();
      this.setDirty();
   }

   private void carryOut(ServerLevel level, FactionWorldData data, Set<String> ayes) {
      switch (this.matter) {
         case PEACE -> {
            if (FactionDiplomacy.setState(level, this.first, this.second, RelationState.NEUTRAL)) {
               FactionNews.relationChanged(level, this.first, this.second, RelationState.NEUTRAL, "council");
            }
         }
         case OUTLAWS -> {
            int beaten = 0;
            for (FactionWorldData.Settlement settlement : data.settlements()) {
               if (FactionDiplomacy.isOutlaw(settlement.faction())) {
                  beaten += settlement.garrison / 2;
                  settlement.garrison -= settlement.garrison / 2;
               }
            }

            data.setDirty();
            FactionNews.tell(level, Component.translatable("news.ancientwarfare.council_outlaws", beaten));
         }
         case ENEMY -> {
            FactionStandingData standings = FactionStandingData.get(level);
            Set<String> members = new LinkedHashSet<>();
            for (String faction : ayes) {
               RelationState treaty = standings.getTreaty(this.enemy, faction);
               if (treaty != RelationState.ALLIANCE && treaty != RelationState.PACT) {
                  members.add(faction);
               }
            }

            if (members.size() >= 2) {
               FactionCoalitions.proclaim(level, this.enemy, members);
            }
         }
         case TOLLS_UP -> this.tolls = TOLLS_UP;
         case TOLLS_DOWN -> this.tolls = TOLLS_DOWN;
      }
   }

   /** Tells the player about the sitting council, with vote buttons. */
   public static void show(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      FactionCouncil council = get(level);
      if (council.host == null) {
         long day = FactionWorldData.get(level).lastSimulatedDay();
         long next = FactionSeasons.SEASON_DAYS - Math.floorMod(day, (long)FactionSeasons.SEASON_DAYS);
         player.sendSystemMessage(Component.translatable("council.ancientwarfare.next", next, Math.round(council.tolls * 100)).withStyle(ChatFormatting.GRAY));
         return;
      }

      player.sendSystemMessage(
         Component.translatable("council.ancientwarfare.sitting", FactionNews.factionName(council.host), council.hall.getX(), council.hall.getZ(), council.question(level))
            .withStyle(ChatFormatting.GOLD)
      );
      Boolean mine = council.playerVotes.get(player.getUUID());
      MutableComponent line = Component.literal("  ")
         .append(FactionEnvoys.button("council.ancientwarfare.yes", "/awcouncil yes", ChatFormatting.GREEN))
         .append("  ")
         .append(FactionEnvoys.button("council.ancientwarfare.no", "/awcouncil no", ChatFormatting.RED));
      if (mine != null) {
         line.append("  ").append(Component.translatable(mine ? "council.ancientwarfare.voted_yes" : "council.ancientwarfare.voted_no").withStyle(ChatFormatting.GRAY));
      }

      player.sendSystemMessage(line);
      player.sendSystemMessage(Component.translatable("council.ancientwarfare.hint", HALL_RANGE, playerWeight(FactionWorldData.get(level), player.getUUID())).withStyle(ChatFormatting.GRAY));
   }

   /** Test hooks: call a council now, or close the sitting one. */
   public static boolean forceConvene(ServerLevel level, String matter) {
      FactionCouncil council = get(level);
      council.host = null;
      Matter forced = null;
      for (Matter each : Matter.values()) {
         if (each.key().equals(matter)) {
            forced = each;
         }
      }

      return council.convene(level, FactionWorldData.get(level), forced);
   }

   public static void forceResolve(ServerLevel level) {
      FactionCouncil council = get(level);
      if (council.host != null) {
         council.resolve(level, FactionWorldData.get(level));
      }
   }

   public static BlockPos hall(ServerLevel level) {
      return get(level).hall;
   }

   private static FactionCouncil load(CompoundTag tag) {
      FactionCouncil council = new FactionCouncil();
      council.tolls = tag.contains("Tolls") ? Mth.clamp(tag.getDouble("Tolls"), 0.5, 2.0) : 1.0;
      if (tag.contains("Host")) {
         try {
            council.host = tag.getString("Host");
            council.hall = BlockPos.of(tag.getLong("Hall"));
            council.matter = Matter.valueOf(tag.getString("Matter"));
            council.first = tag.getString("First");
            council.second = tag.getString("Second");
            council.enemy = tag.hasUUID("Enemy") ? tag.getUUID("Enemy") : null;
            council.closes = tag.getLong("Closes");
            for (Tag element : tag.getList("Votes", Tag.TAG_COMPOUND)) {
               CompoundTag vote = (CompoundTag)element;
               council.playerVotes.put(vote.getUUID("Player"), vote.getBoolean("Yes"));
            }
         } catch (IllegalArgumentException e) {
            council.host = null;
         }
      }

      return council;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      tag.putDouble("Tolls", this.tolls);
      if (this.host != null) {
         tag.putString("Host", this.host);
         tag.putLong("Hall", this.hall.asLong());
         tag.putString("Matter", this.matter.name());
         tag.putString("First", this.first);
         tag.putString("Second", this.second);
         if (this.enemy != null) {
            tag.putUUID("Enemy", this.enemy);
         }

         tag.putLong("Closes", this.closes);
         net.minecraft.nbt.ListTag votes = new net.minecraft.nbt.ListTag();
         this.playerVotes.forEach((player, yes) -> {
            CompoundTag vote = new CompoundTag();
            vote.putUUID("Player", player);
            vote.putBoolean("Yes", yes);
            votes.add(vote);
         });
         tag.put("Votes", votes);
      }

      return tag;
   }
}
