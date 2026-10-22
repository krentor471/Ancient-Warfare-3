package net.shadowmage.ancientwarfare.core.forge;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargeting;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.NpcBaseEntity;
import net.shadowmage.ancientwarfare.core.entity.PlayerOwnedNpcEntity;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.FORGE
)
public final class ForgeFollowerCombatEvents {
   private static final double FOLLOWER_ALERT_RANGE = 96.0;
   /** Soldiers this close to a comrade or their lord under attack join the fight on their own. */
   private static final double ALLY_ALERT_RANGE = 16.0;

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

   /**
    * Someone hits one of the player's men or the player: idle soldiers of the same lord nearby turn on the attacker,
    * instead of watching their comrade die.
    */
   /** Soldiers count their kills: of men, monsters and beasts that fought back. */
   @SubscribeEvent
   public static void onKill(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
      LivingEntity victim = event.getEntity();
      if (!victim.level().isClientSide
         && event.getSource().getEntity() instanceof net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity soldier
         && (victim instanceof net.minecraft.world.entity.monster.Enemy || victim instanceof Player || victim instanceof net.shadowmage.ancientwarfare.core.entity.NpcBaseEntity)) {
         soldier.onKill(victim);
      }
   }

   @SubscribeEvent
   public static void onAllyHurt(LivingHurtEvent event) {
      LivingEntity victim = event.getEntity();
      if (victim.level() instanceof ServerLevel level && event.getSource().getEntity() instanceof LivingEntity attacker && attacker != victim) {
         UUID lord = victim instanceof Player player ? player.getUUID() : (victim instanceof PlayerOwnedNpcEntity owned ? owned.getOwner().orElse(null) : null);
         if (lord != null) {
            level.getEntitiesOfClass(
                  CombatNpcEntity.class,
                  victim.getBoundingBox().inflate(ALLY_ALERT_RANGE, 6.0, ALLY_ALERT_RANGE),
                  soldier -> soldier != victim
                     && soldier.isAlive()
                     && !soldier.isRouting()
                     && soldier.getOwner().filter(lord::equals).isPresent()
                     && (soldier.getTarget() == null || !soldier.getTarget().isAlive())
                     && canEngage(soldier, attacker)
               )
               .forEach(soldier -> soldier.setTarget(attacker));
         }
      }
   }

   /**
    * Arrows of NPC archers fly through their own side instead of hitting a comrade in the back; the target and everyone
    * else is hit as usual.
    */
   @SubscribeEvent
   public static void onArrowImpact(ProjectileImpactEvent event) {
      if (event.getProjectile() instanceof AbstractArrow arrow
         && arrow.getOwner() instanceof NpcBaseEntity shooter
         && event.getRayTraceResult() instanceof EntityHitResult hit) {
         Entity struck = hit.getEntity();
         if (struck != shooter.getTarget() && (NpcTargeting.isProtectedAlly(shooter, struck) || shooter.isAlliedTo(struck) || struck.isAlliedTo(shooter))) {
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
         }
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
