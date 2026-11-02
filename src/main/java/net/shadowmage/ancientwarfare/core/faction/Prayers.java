package net.shadowmage.ancientwarfare.core.faction;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;

/**
 * A player's own faith. Praying in a town that keeps a shrine or temple makes the player one of its
 * faithful; once a day the prayer brings the faith's blessing, and the realms of that faith think a
 * little better of a fellow believer. Turning to another faith costs standing with the old one's realms.
 */
public final class Prayers {
   public static final int PRICE = 2;
   private static final String FAITH_TAG = "AWFaith";
   private static final String DAY_TAG = "AWPrayedDay";
   private static final int BLESSING_TICKS = 20 * 60 * 10;
   private static final int APOSTASY_STANDING = -6;

   private Prayers() {
   }

   /** The player's faith, or -1 before their first prayer. */
   public static int faith(ServerPlayer player) {
      return player.getPersistentData().contains(FAITH_TAG) ? player.getPersistentData().getInt(FAITH_TAG) : -1;
   }

   static void pray(ServerPlayer player, FactionNpcEntity head, FactionWorldData.Settlement town, Component speaker) {
      ServerLevel level = player.serverLevel();
      if (town == null || town.buildings().stream().noneMatch(b -> b.contains("shrine") || b.contains("temple") || b.contains("monastery"))) {
         FactionRumours.say(player, speaker, Component.translatable("faith.ancientwarfare.pray.no_shrine"));
         return;
      }

      long day = level.getDayTime() / FactionSimulation.DAY_TICKS;
      if (player.getPersistentData().getLong(DAY_TAG) == day + 1) {
         FactionRumours.say(player, speaker, Component.translatable("faith.ancientwarfare.pray.done"));
         return;
      }

      if (!Coins.take(player, PRICE)) {
         player.sendSystemMessage(Component.translatable("traveller.ancientwarfare.no_silver").withStyle(ChatFormatting.RED));
         return;
      }

      int faith = FaithSpread.faith(level, town);
      int old = faith(player);
      FactionStandingData standings = FactionStandingData.get(level);
      UUID id = player.getUUID();
      if (old != faith) {
         if (old >= 0) {
            for (String faction : standings.knownFactions(id)) {
               if (FactionFaith.faith(level, faction) == old) {
                  standings.adjustStanding(id, faction, APOSTASY_STANDING);
               }
            }

            player.sendSystemMessage(Component.translatable("faith.ancientwarfare.pray.converted", FactionFaith.name(old), FactionFaith.name(faith)).withStyle(ChatFormatting.LIGHT_PURPLE));
         } else {
            player.sendSystemMessage(Component.translatable("faith.ancientwarfare.pray.joined", FactionFaith.name(faith)).withStyle(ChatFormatting.LIGHT_PURPLE));
         }

         player.getPersistentData().putInt(FAITH_TAG, faith);
      }

      player.getPersistentData().putLong(DAY_TAG, day + 1);
      bless(player, faith);
      standings.adjustStanding(id, head.getFactionName(), FactionFaith.faith(level, head.getFactionName()) == faith ? 2 : 1);
      level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.5, player.getZ(), 40, 0.6, 0.8, 0.6, 0.6);
      level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.4F);
      player.sendSystemMessage(Component.translatable("faith.ancientwarfare.pray.blessing." + FactionFaith.KEYS[faith]).withStyle(ChatFormatting.GOLD));
   }

   /** Each faith's gift to its faithful. */
   private static void bless(ServerPlayer player, int faith) {
      switch (faith) {
         case FactionFaith.SUN -> {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 60, 0));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 20, 0));
         }
         case FactionFaith.OLD_GODS -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, BLESSING_TICKS, 0));
         case FactionFaith.ANCESTORS -> player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, BLESSING_TICKS, 0));
         default -> {
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, BLESSING_TICKS, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, BLESSING_TICKS, 0));
         }
      }
   }
}
