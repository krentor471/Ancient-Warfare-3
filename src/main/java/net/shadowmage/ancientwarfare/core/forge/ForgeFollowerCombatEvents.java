package net.shadowmage.ancientwarfare.core.forge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargeting;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.PlayerOwnedNpcEntity;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.FORGE
)
public final class ForgeFollowerCombatEvents {
   private static final double FOLLOWER_ALERT_RANGE = 96.0;

   private ForgeFollowerCombatEvents() {
   }

   @SubscribeEvent
   public static void onPlayerAttack(AttackEntityEvent event) {
      Player player = event.getEntity();
      if (player.level() instanceof ServerLevel level
         && event.getTarget() instanceof LivingEntity target
         && !CommandBatonItem.isAttackMode(player.getMainHandItem())) {
         alertFollowingSoldiers(level, player, target);
         return;
      }
   }

   @SubscribeEvent
   public static void onPlayerAttacked(LivingAttackEvent event) {
      if (event.getEntity() instanceof Player player && player.level() instanceof ServerLevel level) {
         LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity livingx
            ? livingx
            : (event.getSource().getDirectEntity() instanceof LivingEntity living ? living : null);
         if (attacker != null) {
            alertFollowingSoldiers(level, player, attacker);
         }

         return;
      }
   }

   private static void alertFollowingSoldiers(ServerLevel level, Player player, LivingEntity target) {
      if (isValidTarget(player, target)) {
         level.getEntitiesOfClass(CombatNpcEntity.class, player.getBoundingBox().inflate(96.0), soldier -> isFollowing(soldier, player) && canEngage(soldier, target))
            .forEach(soldier -> {
               soldier.setLastHurtByMob(target);
               soldier.setTarget(target);
            });
      }
   }

   private static boolean isFollowing(PlayerOwnedNpcEntity soldier, Player player) {
      return soldier.isAlive()
         && soldier.getCurrentCommand() == NpcCommandType.FOLLOW
         && soldier.getFollowTarget().filter(player.getUUID()::equals).isPresent();
   }

   private static boolean isValidTarget(Player player, LivingEntity target) {
      return NpcTargeting.canHarm(player, target);
   }

   private static boolean canEngage(CombatNpcEntity soldier, LivingEntity target) {
      return NpcTargeting.canHarm(soldier, target);
   }
}
