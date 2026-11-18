package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;

/**
 * Disasters the player sees and can help with. A house catches fire in a town the player is in:
 * smoke and flames pour from its roof and the townsfolk cry out; water poured on it (a water
 * bucket emptied beside it) puts it out, and the town is grateful; left to burn, the house is lost.
 * In a town with the plague the sick cough in the streets with a green pallor; a player who gives
 * a sick townsman milk cures him and is thanked.
 */
public final class TownDisasters {
   private static final long CHECK_TICKS = 100L;
   private static final double FIRE_CHANCE = 0.006;
   private static final long BURN_TICKS = 20L * 150;
   private static final int SAVED_STANDING = 6;
   private static final int CURED_STANDING = 3;
   private static final Map<Integer, Fire> FIRES = new HashMap<>();

   private static final class Fire {
      final int town;
      final FactionWorldData.Hall hall;
      final long since;
      int water = -1;

      Fire(int town, FactionWorldData.Hall hall, long since) {
         this.town = town;
         this.hall = hall;
         this.since = since;
      }
   }

   private TownDisasters() {
   }

   static void tick(ServerLevel level, FactionWorldData data) {
      long now = level.getGameTime();
      for (Fire fire : FIRES.values()) {
         burn(level, fire);
      }

      if (now % CHECK_TICKS != 0L) {
         return;
      }

      RandomSource random = level.getRandom();
      Iterator<Fire> iterator = FIRES.values().iterator();
      while (iterator.hasNext()) {
         Fire fire = iterator.next();
         FactionWorldData.Settlement town = data.settlements().stream().filter(s -> s.id() == fire.town).findFirst().orElse(null);
         if (town == null) {
            iterator.remove();
         } else if (fire.water < 0) {
            // Wells and troughs already there do not count: only water poured since.
            fire.water = water(level, fire.hall);
         } else if (water(level, fire.hall) > fire.water) {
            iterator.remove();
            saved(level, town, fire);
         } else if (now - fire.since > BURN_TICKS) {
            iterator.remove();
            lost(level, data, town, fire);
         }
      }

      for (ServerPlayer player : level.players()) {
         FactionWorldData.Settlement town = FactionCrime.settlementAt(level, player.blockPosition());
         if (town == null || !FactionTownLife.isTown(town) || FIRES.containsKey(town.id()) || town.halls().isEmpty()) {
            continue;
         }

         double chance = FIRE_CHANCE * (FactionSeasons.season(level) == FactionSeasons.Season.SUMMER ? 2.0 : 1.0) * (level.isRaining() ? 0.2 : 1.0);
         if (random.nextDouble() < chance) {
            FactionWorldData.Hall hall = town.halls().get(random.nextInt(town.halls().size()));
            FIRES.put(town.id(), new Fire(town.id(), hall, now));
            tell(level, town, Component.translatable("disaster.ancientwarfare.fire", FactionNews.named(town)).withStyle(ChatFormatting.RED));
            shout(level, hall, "disaster.ancientwarfare.fire_cry");
         }

         if (town.plague() > 0) {
            sick(level, player);
         }
      }
   }

   private static void burn(ServerLevel level, Fire fire) {
      if (level.getGameTime() % 4L != 0L) {
         return;
      }

      RandomSource random = level.getRandom();
      BlockPos min = fire.hall.min();
      BlockPos max = fire.hall.max();
      double x = min.getX() + random.nextDouble() * (max.getX() - min.getX() + 1);
      double z = min.getZ() + random.nextDouble() * (max.getZ() - min.getZ() + 1);
      double y = max.getY() + 0.5;
      level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 3, 0.4, 0.4, 0.4, 0.02);
      level.sendParticles(ParticleTypes.FLAME, x, y - 0.5, z, 4, 0.5, 0.3, 0.5, 0.01);
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 1.0, z, 1, 0.2, 0.2, 0.2, 0.01);
      if (random.nextInt(3) == 0) {
         level.playSound(null, x, y, z, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.5F, 0.8F);
      }
   }

   /** Water poured beside or on the burning house. */
   private static int water(ServerLevel level, FactionWorldData.Hall hall) {
      int count = 0;
      for (BlockPos pos : BlockPos.betweenClosed(hall.min().offset(-2, -1, -2), hall.max().offset(2, 2, 2))) {
         if (level.getBlockState(pos).is(Blocks.WATER)) {
            count++;
         }
      }

      return count;
   }

   private static void saved(ServerLevel level, FactionWorldData.Settlement town, Fire fire) {
      AABB box = new AABB(fire.hall.min(), fire.hall.max()).inflate(12);
      FactionStandingData standings = FactionStandingData.get(level);
      for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
         standings.adjustStanding(player.getUUID(), town.faction(), SAVED_STANDING);
         Coins.give(player, 10);
      }

      town.order = Math.min(100, town.order + 3);
      // The water stays where it was poured: the player cleans up his own puddles.
      tell(level, town, Component.translatable("disaster.ancientwarfare.fire_saved", FactionNews.named(town)).withStyle(ChatFormatting.GREEN));
   }

   private static void lost(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement town, Fire fire) {
      town.order = Math.max(0, town.order - 6);
      town.wealth = Math.max(0, town.wealth - 40);
      // The house is a blackened shell: its roof and walls of wood turn to charcoal-dark blocks.
      for (BlockPos pos : BlockPos.betweenClosed(fire.hall.min(), fire.hall.max())) {
         var state = level.getBlockState(pos);
         if (state.is(net.minecraft.tags.BlockTags.PLANKS) || state.is(net.minecraft.tags.BlockTags.WOODEN_STAIRS) || state.is(net.minecraft.tags.BlockTags.WOODEN_SLABS)) {
            if (level.getRandom().nextInt(3) == 0) {
               level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            } else {
               level.setBlockAndUpdate(pos, Blocks.DARK_OAK_PLANKS.defaultBlockState());
            }
         } else if (state.is(net.minecraft.tags.BlockTags.WOOL) || state.is(net.minecraft.tags.BlockTags.BEDS) || state.is(Blocks.HAY_BLOCK)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
         }
      }

      data.setDirty();
      tell(level, town, Component.translatable("disaster.ancientwarfare.fire_lost", FactionNews.named(town)).withStyle(ChatFormatting.DARK_RED));
   }

   /** In a plague town the sick cough and look green; milk cures one. */
   private static void sick(ServerLevel level, ServerPlayer player) {
      RandomSource random = level.getRandom();
      List<FactionNpcEntity> folk = level.getEntitiesOfClass(FactionNpcEntity.class, player.getBoundingBox().inflate(24), npc -> FactionSimulation.isCivilian(npc) && npc.getId() % 3 == 0);
      for (FactionNpcEntity npc : folk) {
         level.sendParticles(ParticleTypes.ENTITY_EFFECT, npc.getX(), npc.getY() + 1.2, npc.getZ(), 4, 0.3, 0.4, 0.3, 0.0);
         if (random.nextInt(4) == 0) {
            level.playSound(null, npc.blockPosition(), SoundEvents.PANDA_SNEEZE, SoundSource.NEUTRAL, 0.6F, 0.6F);
         }

         npc.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0, false, false));
      }
   }

   /** A player holding milk who talks to a sick townsman cures him. True if it was a cure. */
   public static boolean cure(ServerPlayer player, FactionNpcEntity npc) {
      ItemStack held = player.getMainHandItem();
      if (!held.is(Items.MILK_BUCKET) || npc.getId() % 3 != 0) {
         return false;
      }

      FactionWorldData.Settlement town = FactionCrime.settlementAt(player.serverLevel(), npc.blockPosition());
      if (town == null || town.plague() <= 0) {
         return false;
      }

      if (!player.isCreative()) {
         player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
      }

      npc.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
      town.health = Math.min(100, town.health + 2);
      FactionStandingData.get(player.serverLevel()).adjustStanding(player.getUUID(), town.faction(), CURED_STANDING);
      player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, npc.getX(), npc.getY() + 1.5, npc.getZ(), 10, 0.4, 0.4, 0.4, 0.0);
      FactionRumours.say(player, npc.getDisplayName(), Component.translatable("disaster.ancientwarfare.cured"));
      return true;
   }

   private static void tell(ServerLevel level, FactionWorldData.Settlement town, Component message) {
      for (ServerPlayer player : level.players()) {
         if (town.horizontalDistanceSquared(player.getX(), player.getZ()) < 160.0 * 160.0) {
            player.sendSystemMessage(message);
         }
      }
   }

   private static void shout(ServerLevel level, FactionWorldData.Hall hall, String key) {
      BlockPos center = new BlockPos((hall.min().getX() + hall.max().getX()) / 2, hall.min().getY(), (hall.min().getZ() + hall.max().getZ()) / 2);
      for (FactionNpcEntity npc : level.getEntitiesOfClass(FactionNpcEntity.class, new AABB(center).inflate(32), FactionSimulation::isCivilian)) {
         npc.getNavigation().moveTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 1.2);
      }

      level.playSound(null, center, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 1.0F);
   }
}
