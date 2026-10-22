package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;

/**
 * Envoys: now and then a country the player knows sends a messenger with an offer. He walks up to
 * the player and speaks in chat, with buttons to accept or decline. An offer can be a treaty
 * (peace, pact, alliance, trade), a paid task (fight the country's enemy, clear out bandits), or
 * a demand for tribute that is refused at a price. A task is done by killing the asked number of
 * the enemy's men in time; its reward is paid in emeralds and goodwill.
 */
public final class FactionEnvoys {
   private static final String ENVOY_TAG = "AWEnvoy";
   private static final String NEXT_TAG = "AWEnvoyNext";
   private static final String QUEST_TAG = "AWEnvoyQuest";
   private static final long CHECK_TICKS = 1200L;
   private static final long DAY_TICKS = 24000L;
   /** Least time between two envoys to one player, and the chance one sets out at each check after it. */
   private static final long ENVOY_INTERVAL = DAY_TICKS * 3 / 2;
   private static final float ENVOY_CHANCE = 0.12F;
   private static final long WALK_TICKS = 600L;
   private static final long ANSWER_TICKS = DAY_TICKS;
   private static final long WAIT_AFTER_TALK = 2400L;
   private static final long QUEST_TICKS = DAY_TICKS * 3;
   private static final double DELIVER_RANGE = 4.0;
   private static final int WAR_HELP_KILLS = 5;
   private static final int WAR_HELP_REWARD = 20;
   private static final int BANDIT_KILLS = 6;
   private static final int BANDIT_REWARD = 15;
   private static final int QUEST_STANDING = 15;
   private static final int TRIBUTE = 10;
   private static final int TRIBUTE_STANDING = 15;
   private static final int REFUSED_TRIBUTE_STANDING = -20;
   /** A country whose tribute is refused at this standing or lower goes to war. */
   private static final int TRIBUTE_WAR_STANDING = -50;
   private static final int MIN_TRIBUTE_SETTLEMENTS = 6;
   private static final double BANDIT_RANGE = 1000.0;

   public enum Kind {
      PEACE,
      PACT,
      ALLIANCE,
      TRADE,
      WAR_HELP,
      BANDITS,
      TRIBUTE
   }

   private static final class Offer {
      final int id;
      final UUID player;
      final String faction;
      final Kind kind;
      /** The enemy to fight, for WAR_HELP. */
      final String enemy;
      final long sent;
      FactionNpcEntity envoy;
      long delivered = -1L;

      Offer(int id, UUID player, String faction, Kind kind, String enemy, long sent) {
         this.id = id;
         this.player = player;
         this.faction = faction;
         this.kind = kind;
         this.enemy = enemy;
         this.sent = sent;
      }
   }

   private static final Map<UUID, Offer> OFFERS = new HashMap<>();
   /** Envoys now walking the world; any other envoy is left over from before a restart. */
   private static final java.util.Set<UUID> LIVE_ENVOYS = new java.util.HashSet<>();
   private static int nextId = 1;

   private FactionEnvoys() {
   }

   public static boolean isEnvoy(FactionNpcEntity npc) {
      return npc.getPersistentData().getBoolean(ENVOY_TAG);
   }

   /** An envoy from before a restart, whose offer is gone, should not come back into the world. */
   public static boolean isStaleEnvoy(FactionNpcEntity npc) {
      return isEnvoy(npc) && !LIVE_ENVOYS.contains(npc.getUUID());
   }

   public static void tick(ServerLevel level) {
      long now = level.getGameTime();
      if (now % 20L == 0L) {
         tickOffers(level, now);
      }

      if (now % CHECK_TICKS != 0L) {
         return;
      }

      for (ServerPlayer player : level.players()) {
         if (player.isSpectator() || OFFERS.containsKey(player.getUUID())) {
            continue;
         }

         CompoundTag tag = persisted(player);
         if (tag.getLong(NEXT_TAG) == 0L) {
            // The first envoy waits a while: the player has only just arrived.
            tag.putLong(NEXT_TAG, now + ENVOY_INTERVAL);
         } else if (now >= tag.getLong(NEXT_TAG) && level.getRandom().nextFloat() < ENVOY_CHANCE) {
            send(level, player, null);
         }

         checkQuestTime(player, now);
      }
   }

   /** Sends an envoy now, of the given kind or any that fits; returns false when no country has anything to say. */
   public static boolean send(ServerLevel level, ServerPlayer player, Kind wanted) {
      List<Offer> candidates = candidates(level, player, wanted);
      if (candidates.isEmpty()) {
         return false;
      }

      Offer offer = candidates.get(level.getRandom().nextInt(candidates.size()));
      persisted(player).putLong(NEXT_TAG, level.getGameTime() + ENVOY_INTERVAL);
      offer.envoy = spawnEnvoy(level, player, offer.faction);
      OFFERS.put(player.getUUID(), offer);
      if (offer.envoy == null) {
         deliver(level, player, offer);
      } else {
         player.sendSystemMessage(Component.translatable("envoy.ancientwarfare.coming", FactionNews.factionName(offer.faction)).withStyle(ChatFormatting.GRAY));
      }

      AncientWarfareCore.LOG.info("{} sends an envoy to {}: {}{}", offer.faction, player.getScoreboardName(), offer.kind, offer.enemy.isEmpty() ? "" : " against " + offer.enemy);
      return true;
   }

   private static List<Offer> candidates(ServerLevel level, ServerPlayer player, Kind wanted) {
      FactionStandingData standings = FactionStandingData.get(level);
      FactionWorldData data = FactionWorldData.get(level);
      UUID id = player.getUUID();
      long now = level.getGameTime();
      boolean questFree = !persisted(player).contains(QUEST_TAG);
      List<Offer> offers = new ArrayList<>();

      for (String faction : new TreeSet<>(standings.knownFactions(id))) {
         int settlements = (int)data.settlements().stream().filter(settlement -> settlement.faction().equals(faction)).count();
         if (FactionDiplomacy.isOutlaw(faction) || settlements == 0) {
            continue;
         }

         RelationState treaty = standings.getTreaty(id, faction);
         int standing = standings.getStanding(id, faction);
         List<Kind> kinds = new ArrayList<>();
         if (treaty == RelationState.WAR) {
            if (standing >= PlayerDiplomacy.PEACE_STANDING) {
               kinds.add(Kind.PEACE);
            }
         } else {
            if (treaty == RelationState.NEUTRAL && standing >= PlayerDiplomacy.PACT_STANDING - 10) {
               kinds.add(Kind.PACT);
            }

            if (treaty == RelationState.PACT && standing >= PlayerDiplomacy.ALLIANCE_STANDING - 10) {
               kinds.add(Kind.ALLIANCE);
            }

            if (standing >= PlayerDiplomacy.TRADE_STANDING && !standings.hasTrade(id, faction)) {
               kinds.add(Kind.TRADE);
            }

            if (standing < 0 && settlements >= MIN_TRIBUTE_SETTLEMENTS) {
               kinds.add(Kind.TRIBUTE);
            }

            if (questFree && !standings.isHostile(id, faction) && banditsNear(data, faction)) {
               kinds.add(Kind.BANDITS);
            }
         }

         for (Kind kind : kinds) {
            if (wanted == null || wanted == kind) {
               offers.add(new Offer(nextId++, id, faction, kind, "", now));
            }
         }

         if (questFree && !standings.isHostile(id, faction) && (wanted == null || wanted == Kind.WAR_HELP)) {
            for (String enemy : new TreeSet<>(FactionRegistry.factionNames())) {
               RelationState withEnemy = standings.getTreaty(id, enemy);
               if (!enemy.equals(faction)
                  && !FactionDiplomacy.isOutlaw(enemy)
                  && withEnemy != RelationState.PACT
                  && withEnemy != RelationState.ALLIANCE
                  && FactionDiplomacy.relation(level, faction, enemy).state() == RelationState.WAR) {
                  offers.add(new Offer(nextId++, id, faction, Kind.WAR_HELP, enemy, now));
               }
            }
         }
      }

      return offers;
   }

   private static boolean banditsNear(FactionWorldData data, String faction) {
      for (FactionWorldData.Settlement lair : data.settlements()) {
         if (FactionDiplomacy.isOutlaw(lair.faction())) {
            BlockPos center = lair.center();
            for (FactionWorldData.Settlement settlement : data.settlements()) {
               if (settlement.faction().equals(faction) && settlement.horizontalDistanceSquared(center.getX(), center.getZ()) < BANDIT_RANGE * BANDIT_RANGE) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static FactionNpcEntity spawnEnvoy(ServerLevel level, ServerPlayer player, String faction) {
      RandomSource random = level.getRandom();

      for (int attempt = 0; attempt < 10; attempt++) {
         double angle = random.nextDouble() * Math.PI * 2.0;
         int x = (int)Math.floor(player.getX() + Math.cos(angle) * 18.0);
         int z = (int)Math.floor(player.getZ() + Math.sin(angle) * 18.0);
         if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
            continue;
         }

         int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
         BlockPos feet = new BlockPos(x, y, z);
         if (!level.getFluidState(feet.below()).isEmpty() || Math.abs(y - player.getY()) > 8.0) {
            continue;
         }

         if (!(AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_CIVILIAN_MALE_NPC).create(level) instanceof FactionNpcEntity envoy)) {
            return null;
         }

         envoy.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
         envoy.setFactionName(faction);
         envoy.getPersistentData().putBoolean(ENVOY_TAG, true);
         envoy.setCustomName(Component.translatable("envoy.ancientwarfare.name", FactionNews.factionName(faction)).withStyle(ChatFormatting.AQUA));
         envoy.setCustomNameVisible(true);
         envoy.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.PAPER));
         envoy.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
         // He comes in his country's colours.
         envoy.setItemSlot(EquipmentSlot.HEAD, FactionHeraldry.banner(faction));
         envoy.setDropChance(EquipmentSlot.HEAD, 0.0F);
         envoy.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
         envoy.setDropChance(EquipmentSlot.FEET, 0.0F);
         envoy.setPersistenceRequired();
         LIVE_ENVOYS.add(envoy.getUUID());
         level.addFreshEntity(envoy);
         return envoy;
      }

      return null;
   }

   private static void tickOffers(ServerLevel level, long now) {
      Iterator<Offer> iterator = OFFERS.values().iterator();

      while (iterator.hasNext()) {
         Offer offer = iterator.next();
         ServerPlayer player = level.getServer().getPlayerList().getPlayer(offer.player);
         if (offer.delivered < 0L) {
            if (player == null || player.level() != level || offer.envoy == null || !offer.envoy.isAlive()) {
               // The player left, or the envoy was killed on the way: the offer is lost.
               dismiss(offer);
               iterator.remove();
            } else if (offer.envoy.distanceTo(player) <= DELIVER_RANGE || now - offer.sent > WALK_TICKS) {
               deliver(level, player, offer);
            } else {
               offer.envoy.getNavigation().moveTo(player, 0.8);
            }
         } else if (now - offer.delivered > ANSWER_TICKS) {
            dismiss(offer);
            iterator.remove();
         } else if (offer.envoy != null && now - offer.delivered > WAIT_AFTER_TALK) {
            // Not answered for a while: the envoy leaves, the offer still stands for a day.
            dismiss(offer);
         } else if (offer.envoy != null && player != null && offer.envoy.isAlive()) {
            offer.envoy.getLookControl().setLookAt(player);
         }
      }
   }

   private static void dismiss(Offer offer) {
      if (offer.envoy != null) {
         LIVE_ENVOYS.remove(offer.envoy.getUUID());
         if (offer.envoy.isAlive()) {
            offer.envoy.discard();
         }
      }

      offer.envoy = null;
   }

   private static void deliver(ServerLevel level, ServerPlayer player, Offer offer) {
      offer.delivered = level.getGameTime();
      String key = offer.kind.name().toLowerCase(Locale.ROOT);
      Component faction = FactionNews.factionName(offer.faction);
      Component speech = switch (offer.kind) {
         case WAR_HELP -> Component.translatable("envoy.ancientwarfare.offer." + key, faction, FactionNews.factionName(offer.enemy), WAR_HELP_KILLS, WAR_HELP_REWARD);
         case BANDITS -> Component.translatable("envoy.ancientwarfare.offer." + key, faction, BANDIT_KILLS, BANDIT_REWARD);
         case TRIBUTE -> Component.translatable("envoy.ancientwarfare.offer." + key, faction, TRIBUTE);
         default -> Component.translatable("envoy.ancientwarfare.offer." + key, faction);
      };
      player.sendSystemMessage(Component.translatable("envoy.ancientwarfare.says", faction).withStyle(ChatFormatting.AQUA).append(speech.copy().withStyle(ChatFormatting.WHITE)));
      player.sendSystemMessage(
         Component.literal("  ")
            .append(button("envoy.ancientwarfare.accept", "/awenvoy accept " + offer.id, ChatFormatting.GREEN))
            .append("  ")
            .append(button("envoy.ancientwarfare.decline", "/awenvoy decline " + offer.id, ChatFormatting.RED))
      );
   }

   static MutableComponent button(String key, String command, ChatFormatting color) {
      return Component.literal("[")
         .append(Component.translatable(key))
         .append("]")
         .withStyle(
            style -> style.withColor(color)
               .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
               .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable(key)))
         );
   }

   /** The envoy was spoken to: he repeats his offer to the player he came for. */
   public static void talk(ServerPlayer player, FactionNpcEntity envoy) {
      Offer offer = OFFERS.get(player.getUUID());
      if (offer != null && offer.envoy == envoy) {
         deliver(player.serverLevel(), player, offer);
      }
   }

   /** The player clicked accept or decline on an offer. */
   public static void answer(ServerPlayer player, int offerId, boolean accepted) {
      Offer offer = OFFERS.get(player.getUUID());
      if (offer == null || offer.id != offerId || offer.delivered < 0L) {
         player.sendSystemMessage(Component.translatable("envoy.ancientwarfare.gone"));
         return;
      }

      OFFERS.remove(player.getUUID());
      dismiss(offer);
      ServerLevel level = player.serverLevel();
      FactionStandingData standings = FactionStandingData.get(level);
      UUID id = player.getUUID();
      Component faction = FactionNews.factionName(offer.faction);
      String key = offer.kind.name().toLowerCase(Locale.ROOT);
      if (!accepted) {
         if (offer.kind == Kind.TRIBUTE) {
            int standing = standings.adjustStanding(id, offer.faction, REFUSED_TRIBUTE_STANDING);
            if (standing <= TRIBUTE_WAR_STANDING && standings.getTreaty(id, offer.faction) != RelationState.WAR) {
               standings.setTreaty(id, offer.faction, RelationState.WAR);
               player.sendSystemMessage(Component.translatable("news.ancientwarfare.war_on_you", faction).withStyle(ChatFormatting.RED));
               CountryDiplomacy.warDeclaredOn(player.serverLevel(), id, offer.faction);
               return;
            }
         }

         player.sendSystemMessage(Component.translatable("envoy.ancientwarfare.declined." + (offer.kind == Kind.TRIBUTE ? "tribute" : "any"), faction));
         return;
      }

      switch (offer.kind) {
         case PEACE -> {
            standings.setTreaty(id, offer.faction, RelationState.NEUTRAL);
            standings.setStanding(id, offer.faction, Math.max(standings.getStanding(id, offer.faction), PlayerDiplomacy.PEACE_RESTORED_STANDING));
         }
         case PACT -> standings.setTreaty(id, offer.faction, RelationState.PACT);
         case ALLIANCE -> standings.setTreaty(id, offer.faction, RelationState.ALLIANCE);
         case TRADE -> standings.setTrade(id, offer.faction, true);
         case TRIBUTE -> {
            if (!PlayerDiplomacy.takeEmeralds(player, TRIBUTE)) {
               player.sendSystemMessage(Component.translatable("mercenary.ancientwarfare.no_money", TRIBUTE));
               OFFERS.put(id, offer);
               return;
            }

            standings.adjustStanding(id, offer.faction, TRIBUTE_STANDING);
         }
         case WAR_HELP, BANDITS -> {
            CompoundTag quest = new CompoundTag();
            quest.putString("Kind", offer.kind.name());
            quest.putString("Faction", offer.faction);
            quest.putString("Enemy", offer.enemy);
            quest.putInt("Needed", offer.kind == Kind.WAR_HELP ? WAR_HELP_KILLS : BANDIT_KILLS);
            quest.putInt("Reward", offer.kind == Kind.WAR_HELP ? WAR_HELP_REWARD : BANDIT_REWARD);
            quest.putLong("Until", level.getGameTime() + QUEST_TICKS);
            persisted(player).put(QUEST_TAG, quest);
         }
      }

      player.sendSystemMessage(Component.translatable("envoy.ancientwarfare.accepted." + key, faction).withStyle(ChatFormatting.GREEN));
   }

   /** A faction NPC died at the player's hand: it may count toward the task they took on. */
   public static void onKill(ServerPlayer player, FactionNpcEntity npc) {
      CompoundTag tag = persisted(player);
      if (!tag.contains(QUEST_TAG)) {
         return;
      }

      CompoundTag quest = tag.getCompound(QUEST_TAG);
      boolean counts = Kind.BANDITS.name().equals(quest.getString("Kind"))
         ? FactionDiplomacy.isOutlaw(npc.getFactionName())
         : npc.getFactionName().equals(quest.getString("Enemy"));
      if (!counts) {
         return;
      }

      int done = quest.getInt("Done") + 1;
      quest.putInt("Done", done);
      int needed = quest.getInt("Needed");
      if (done < needed) {
         player.displayClientMessage(Component.translatable("envoy.ancientwarfare.progress", done, needed), true);
         return;
      }

      tag.remove(QUEST_TAG);
      int reward = quest.getInt("Reward");
      String faction = quest.getString("Faction");
      Coins.give(player, reward);

      FactionStandingData.get(player.serverLevel()).adjustStanding(player.getUUID(), faction, QUEST_STANDING);
      player.sendSystemMessage(Component.translatable("envoy.ancientwarfare.done", FactionNews.factionName(faction), reward).withStyle(ChatFormatting.GOLD));
   }

   private static void checkQuestTime(ServerPlayer player, long now) {
      CompoundTag tag = persisted(player);
      if (tag.contains(QUEST_TAG) && tag.getCompound(QUEST_TAG).getLong("Until") <= now) {
         String faction = tag.getCompound(QUEST_TAG).getString("Faction");
         tag.remove(QUEST_TAG);
         FactionStandingData.get(player.serverLevel()).adjustStanding(player.getUUID(), faction, -5);
         player.sendSystemMessage(Component.translatable("envoy.ancientwarfare.failed", FactionNews.factionName(faction)).withStyle(ChatFormatting.GRAY));
      }
   }

   /** Player data that outlives death. */
   private static CompoundTag persisted(Player player) {
      CompoundTag root = player.getPersistentData();
      if (!root.contains(Player.PERSISTED_NBT_TAG)) {
         root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
      }

      return root.getCompound(Player.PERSISTED_NBT_TAG);
   }
}
