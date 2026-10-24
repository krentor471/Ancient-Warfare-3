package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.entity.FactionBardNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;

/**
 * The barman of a tavern. Every standing tavern near a player has one; talking to him lists what
 * he serves in chat, and clicking an offer pays for it in coins: a mug of ale, a hot meal, the
 * gossip of the road (a barman hears of buried treasure more often than most), a bed for the night,
 * or a round for the whole house, which the tavern's faction remembers kindly once a day.
 */
public final class FactionTaverns {
   public static final String BARMAN_TAG = "AWBarman";
   public static final String BARD_TAG = "AWTavernBard";
   private static final String ROUND_DAY_TAG = "AWBarmanRoundDay";
   private static final double SERVE_RANGE = 8.0;
   private static final long TALK_COOLDOWN = 40L;
   private static final long DAY_TICKS = 24000L;
   private static final double TREASURE_CHANCE = 0.1;
   private static final int ROUND_STANDING = 3;
   private static final int GREETINGS = 6;
   private static final Map<UUID, Long> LAST_TALK = new HashMap<>();

   public enum Offer {
      ALE(1),
      MEAL(2),
      RUMOUR(1),
      ROOM(4),
      ROUND(6),
      DICE(3),
      ARM(2);

      final int price;

      Offer(int price) {
         this.price = price;
      }

      String key() {
         return this.name().toLowerCase(Locale.ROOT);
      }
   }

   private FactionTaverns() {
   }

   public static boolean isBarman(Entity entity) {
      return entity.getPersistentData().getBoolean(BARMAN_TAG);
   }

   /** Called with the mercenary check: every tavern near a player gets its barman and its bard, a town's tavern hall too. */
   static void tick(ServerLevel level, FactionWorldData data) {
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!FactionMercenaries.playerNear(level, settlement) || !level.isLoaded(settlement.min()) || !level.isLoaded(settlement.max())) {
            continue;
         }

         if (WorldMap.isTavern(settlement)) {
            staff(level, settlement.faction(), settlement.id(), settlement.min(), settlement.max());
         }

         for (FactionWorldData.Hall hall : settlement.halls()) {
            if (hall.kind().equals(FactionGrowth.HallKind.TAVERN.key()) && level.isLoaded(hall.min()) && level.isLoaded(hall.max())) {
               staff(level, settlement.faction(), settlement.id(), hall.min(), hall.max());
            }
         }
      }
   }

   private static void staff(ServerLevel level, String faction, int id, BlockPos min, BlockPos max) {
      // Entities load after their chunks: a barman not yet loaded must not get a twin.
      for (int cx = (min.getX() >> 4) - 1; cx <= (max.getX() >> 4) + 1; cx++) {
         for (int cz = (min.getZ() >> 4) - 1; cz <= (max.getZ() >> 4) + 1; cz++) {
            if (!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(cx, cz))) {
               return;
            }
         }
      }

      net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(min, max.offset(1, 1, 1)).inflate(8.0, 8.0, 8.0);
      if (level.getEntitiesOfClass(FactionNpcEntity.class, area, npc -> npc.isAlive() && isBarman(npc)).isEmpty()) {
         FactionNpcEntity barman = (FactionNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_CIVILIAN_MALE_NPC).create(level);
         if (barman != null) {
            BlockPos spot = standingSpot(level, min, max);
            barman.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
            barman.setFactionName(faction);
            barman.getPersistentData().putBoolean(BARMAN_TAG, true);
            barman.setCustomName(Component.translatable("entity.ancientwarfare.barman").withStyle(ChatFormatting.GOLD));
            barman.setCustomNameVisible(true);
            barman.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.HONEY_BOTTLE));
            barman.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            barman.setPersistenceRequired();
            level.addFreshEntity(barman);
            AncientWarfareCore.LOG.info("A barman took over tavern of #{} at {}, {}", id, spot.getX(), spot.getZ());
         }
      }

      // A tavern keeps a bard, who plays when there is anyone to hear.
      if (level.getEntitiesOfClass(FactionBardNpcEntity.class, area, npc -> npc.isAlive() && npc.getPersistentData().getBoolean(BARD_TAG)).isEmpty()) {
         FactionBardNpcEntity bard = AWCoreEntityTypes.FACTION_BARD_NPC.get().create(level);
         if (bard != null) {
            BlockPos spot = standingSpot(level, min, max);
            bard.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
            bard.setFactionName(faction);
            bard.getPersistentData().putBoolean(BARD_TAG, true);
            bard.setHome(spot);
            bard.setPersistenceRequired();
            level.addFreshEntity(bard);
         }
      }
   }

   /** Somewhere to stand inside the building: a floor with room for a body above it. */
   private static BlockPos standingSpot(ServerLevel level, BlockPos min, BlockPos max) {
      net.minecraft.util.RandomSource random = level.getRandom();
      for (int attempt = 0; attempt < 60; attempt++) {
         int x = min.getX() + 1 + random.nextInt(Math.max(1, max.getX() - min.getX() - 1));
         int z = min.getZ() + 1 + random.nextInt(Math.max(1, max.getZ() - min.getZ() - 1));
         for (int y = min.getY(); y <= Math.min(max.getY(), min.getY() + 8); y++) {
            BlockPos feet = new BlockPos(x, y, z);
            if (level.getBlockState(feet.below()).isSolidRender(level, feet.below())
               && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
               && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
               && !level.canSeeSky(feet)) {
               return feet;
            }
         }
      }

      int x = max.getX() + 2;
      int z = (min.getZ() + max.getZ()) / 2;
      return new BlockPos(x, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
   }

   private static boolean hostile(ServerPlayer player, FactionNpcEntity barman) {
      return FactionStandingData.get(player.serverLevel()).isHostile(player.getUUID(), barman.getFactionName());
   }

   /** The barman was spoken to: a greeting and what he serves, each a line to click. */
   public static void talk(ServerPlayer player, FactionNpcEntity barman) {
      long now = player.serverLevel().getGameTime();
      Long last = LAST_TALK.put(player.getUUID(), now);
      if (last != null && now - last < TALK_COOLDOWN && now >= last) {
         return;
      }

      Component speaker = barman.getDisplayName();
      if (hostile(player, barman)) {
         FactionRumours.say(player, speaker, Component.translatable("tavern.ancientwarfare.enemy", FactionNews.factionName(barman.getFactionName())));
         return;
      }

      FactionRumours.say(player, speaker, Component.translatable("tavern.ancientwarfare.greeting." + barman.getRandom().nextInt(GREETINGS)));
      for (Offer offer : Offer.values()) {
         MutableComponent line = Component.literal("  [")
            .append(Component.translatable("tavern.ancientwarfare.offer." + offer.key(), offer.price))
            .append("]")
            .withStyle(
               style -> style.withColor(ChatFormatting.GREEN)
                  .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/awtavern " + barman.getId() + " " + offer.key()))
                  .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("tavern.ancientwarfare.offer." + offer.key() + ".hint")))
            );
         player.sendSystemMessage(line);
      }
   }

   /** The player clicked an offer: pay, and be served. */
   public static void serve(ServerPlayer player, int barmanId, Offer offer) {
      ServerLevel level = player.serverLevel();
      if (!(level.getEntity(barmanId) instanceof FactionNpcEntity barman) || !isBarman(barman) || !barman.isAlive()) {
         return;
      }

      Component speaker = barman.getDisplayName();
      if (barman.distanceToSqr(player) > SERVE_RANGE * SERVE_RANGE) {
         player.sendSystemMessage(Component.translatable("tavern.ancientwarfare.too_far"));
         return;
      }

      if (hostile(player, barman)) {
         FactionRumours.say(player, speaker, Component.translatable("tavern.ancientwarfare.enemy", FactionNews.factionName(barman.getFactionName())));
         return;
      }

      long day = level.getDayTime() / DAY_TICKS;
      if (offer == Offer.ROUND && barman.getPersistentData().getLong(ROUND_DAY_TAG) == day + 1) {
         FactionRumours.say(player, speaker, Component.translatable("tavern.ancientwarfare.round_again"));
         return;
      }

      if (offer == Offer.ROOM && !level.isNight()) {
         FactionRumours.say(player, speaker, Component.translatable("tavern.ancientwarfare.room_day"));
         return;
      }

      if (!Coins.take(player, offer.price)) {
         FactionRumours.say(player, speaker, Component.translatable("tavern.ancientwarfare.no_money", offer.price));
         return;
      }

      barman.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
      switch (offer) {
         case ALE -> {
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.getFoodData().eat(2, 0.2F);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 120, 0));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 6, 0));
            FactionRumours.say(player, speaker, Component.translatable("tavern.ancientwarfare.served.ale"));
         }
         case MEAL -> {
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.getFoodData().eat(20, 1.0F);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 10, 0));
            FactionRumours.say(player, speaker, Component.translatable("tavern.ancientwarfare.served.meal"));
         }
         case RUMOUR -> FactionRumours.say(player, speaker, FactionRumours.rumour(level, player, barman.getX(), barman.getZ(), barman.getRandom(), TREASURE_CHANCE));
         case ROOM -> {
            player.setHealth(player.getMaxHealth());
            player.removeEffect(MobEffects.CONFUSION);
            if (level.getServer().getPlayerList().getPlayerCount() == 1) {
               // The only one awake in the world: the night passes as in a bed.
               level.setDayTime((level.getDayTime() / DAY_TICKS + 1) * DAY_TICKS);
            }

            FactionCrime.rentRoom(player, barman.blockPosition());
            FactionRumours.say(player, speaker, Component.translatable("tavern.ancientwarfare.served.room"));
         }
         case DICE -> dice(player, barman, speaker);
         case ARM -> armWrestle(player, barman, speaker);
         case ROUND -> {
            barman.getPersistentData().putLong(ROUND_DAY_TAG, day + 1);
            int standing = FactionStandingData.get(level).adjustStanding(player.getUUID(), barman.getFactionName(), ROUND_STANDING);
            level.playSound(null, barman.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6F, 1.4F);
            FactionRumours.say(
               player, speaker, Component.translatable("tavern.ancientwarfare.served.round", FactionNews.factionName(barman.getFactionName()), standing)
            );
         }
      }
   }

   /** Two dice each against a regular at the bar: the higher pair takes both stakes, a tie gives the stake back. */
   private static void dice(ServerPlayer player, FactionNpcEntity barman, Component speaker) {
      net.minecraft.util.RandomSource random = barman.getRandom();
      int mine = 2 + random.nextInt(6) + random.nextInt(6);
      int theirs = 2 + random.nextInt(6) + random.nextInt(6);
      player.serverLevel().playSound(null, barman.blockPosition(), SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 0.8F, 1.6F);
      String result;
      if (mine > theirs) {
         Coins.give(player, Offer.DICE.price * 2);
         result = "won";
      } else if (mine == theirs) {
         Coins.give(player, Offer.DICE.price);
         result = "tie";
      } else {
         result = "lost";
      }

      FactionRumours.say(player, speaker, Component.translatable("tavern.ancientwarfare.dice." + result, mine, theirs, Offer.DICE.price * 2));
   }

   /**
    * Arm wrestling with the strongest arm in the house: a fair match for a player with nothing on him, better with
    * strength in him and worse when he is hurt or hungry.
    */
   private static void armWrestle(ServerPlayer player, FactionNpcEntity barman, Component speaker) {
      double chance = 0.45;
      if (player.hasEffect(MobEffects.DAMAGE_BOOST)) {
         chance += 0.25 * (player.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() + 1);
      }

      if (player.hasEffect(MobEffects.WEAKNESS)) {
         chance -= 0.3;
      }

      chance -= 0.2 * (1.0 - player.getHealth() / player.getMaxHealth());
      if (player.getFoodData().getFoodLevel() < 8) {
         chance -= 0.1;
      }

      boolean won = barman.getRandom().nextDouble() < Math.max(0.05, Math.min(0.9, chance));
      player.causeFoodExhaustion(4.0F);
      if (won) {
         Coins.give(player, Offer.ARM.price * 2);
         player.serverLevel().playSound(null, barman.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.NEUTRAL, 0.8F, 1.0F);
      }

      FactionRumours.say(
         player, speaker, Component.translatable("tavern.ancientwarfare.arm." + (won ? "won" : "lost") + "." + barman.getRandom().nextInt(2), Offer.ARM.price * 2)
      );
   }
}
