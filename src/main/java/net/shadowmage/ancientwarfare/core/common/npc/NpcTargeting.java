package net.shadowmage.ancientwarfare.core.common.npc;

import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

public final class NpcTargeting {
   public static final TagKey<EntityType<?>> HOSTILE = tag("npc_hostile");
   public static final TagKey<EntityType<?>> NEUTRAL = tag("npc_neutral");
   public static final TagKey<EntityType<?>> FRIENDLY = tag("npc_friendly");

   private NpcTargeting() {
   }

   private static TagKey<EntityType<?>> tag(String name) {
      return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("ancientwarfare", name));
   }

   public static NpcTargeting.Disposition disposition(LivingEntity target) {
      if (NpcTargetWhitelist.contains(target.getType())) {
         return NpcTargeting.Disposition.FRIENDLY;
      } else if (target.getType().is(FRIENDLY)) {
         return NpcTargeting.Disposition.FRIENDLY;
      } else if (target.getType().is(NEUTRAL)) {
         return NpcTargeting.Disposition.NEUTRAL;
      } else if (target.getType().is(HOSTILE)) {
         return NpcTargeting.Disposition.HOSTILE;
      } else if ("minecraft".equals(EntityType.getKey(target.getType()).getNamespace())) {
         return target instanceof Monster ? NpcTargeting.Disposition.HOSTILE : NpcTargeting.Disposition.NEUTRAL;
      } else {
         return NpcTargeting.Disposition.HOSTILE;
      }
   }

   public static boolean isAutomaticHostile(LivingEntity observer, LivingEntity target) {
      return !(target instanceof Player)
         && !(target instanceof NpcOwnerAccess)
         && !(target instanceof NpcFactionAccess)
         && !(target instanceof ArmorStand)
         && !isOwnedOrTamed(target)
         && disposition(target) == NpcTargeting.Disposition.HOSTILE
         && canHarm(observer, target);
   }

   public static boolean canHarm(LivingEntity observer, Entity target) {
      return observer != null
         && target != null
         && target.isAlive()
         && (!(target instanceof Player player) || !player.isCreative() && !player.isSpectator())
         && !isProtectedAlly(observer, target)
         && !observer.isAlliedTo(target)
         && !target.isAlliedTo(observer);
   }

   public static boolean isProtectedAlly(Entity observer, Entity target) {
      if (observer == null || target == null) {
         return false;
      } else if (observer != target && !observer.isPassengerOfSameVehicle(target)) {
         if (observer.getTeam() != null && observer.getTeam().isAlliedTo(target.getTeam())) {
            return true;
         } else {
            UUID observerOwner = ownerId(observer);
            UUID targetOwner = ownerId(target);
            return observerOwner != null && (observerOwner.equals(target.getUUID()) || observerOwner.equals(targetOwner))
               || targetOwner != null && targetOwner.equals(observer.getUUID());
         }
      } else {
         return true;
      }
   }

   private static boolean isOwnedOrTamed(Entity entity) {
      return entity instanceof TamableAnimal animal && animal.isTame() || entity instanceof OwnableEntity && ownerId(entity) != null;
   }

   private static UUID ownerId(Entity entity) {
      if (entity instanceof Player player) {
         return player.getUUID();
      } else if (entity instanceof NpcOwnerAccess npc) {
         return npc.getOwner().orElse(null);
      } else {
         if (entity instanceof TamableAnimal animal && !animal.isTame()) {
            return null;
         }

         return entity instanceof OwnableEntity ownable ? ownable.getOwnerUUID() : null;
      }
   }

   public static enum Disposition {
      FRIENDLY,
      NEUTRAL,
      HOSTILE;
   }
}
