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
      return TagKey.m_203882_(Registries.f_256939_, new ResourceLocation("ancientwarfare", name));
   }

   public static NpcTargeting.Disposition disposition(LivingEntity target) {
      if (NpcTargetWhitelist.contains(target.m_6095_())) {
         return NpcTargeting.Disposition.FRIENDLY;
      } else if (target.m_6095_().m_204039_(FRIENDLY)) {
         return NpcTargeting.Disposition.FRIENDLY;
      } else if (target.m_6095_().m_204039_(NEUTRAL)) {
         return NpcTargeting.Disposition.NEUTRAL;
      } else if (target.m_6095_().m_204039_(HOSTILE)) {
         return NpcTargeting.Disposition.HOSTILE;
      } else if ("minecraft".equals(EntityType.m_20613_(target.m_6095_()).m_135827_())) {
         return target instanceof Monster ? NpcTargeting.Disposition.HOSTILE : NpcTargeting.Disposition.NEUTRAL;
      } else {
         return NpcTargeting.Disposition.HOSTILE;
      }
   }

   public static boolean isAutomaticHostile(LivingEntity observer, LivingEntity target) {
      return canHarm(observer, target)
         && !(target instanceof Player)
         && !(target instanceof NpcOwnerAccess)
         && !(target instanceof NpcFactionAccess)
         && !(target instanceof ArmorStand)
         && !isOwnedOrTamed(target)
         && disposition(target) == NpcTargeting.Disposition.HOSTILE;
   }

   public static boolean canHarm(LivingEntity observer, Entity target) {
      return observer != null
         && target != null
         && target.m_6084_()
         && (!(target instanceof Player player) || !player.m_7500_() && !player.m_5833_())
         && !isProtectedAlly(observer, target)
         && !observer.m_7307_(target)
         && !target.m_7307_(observer);
   }

   public static boolean isProtectedAlly(Entity observer, Entity target) {
      if (observer == null || target == null) {
         return false;
      } else if (observer != target && !observer.m_20365_(target)) {
         if (observer.m_5647_() != null && observer.m_5647_().m_83536_(target.m_5647_())) {
            return true;
         } else {
            UUID observerOwner = ownerId(observer);
            UUID targetOwner = ownerId(target);
            return observerOwner != null && (observerOwner.equals(target.m_20148_()) || observerOwner.equals(targetOwner))
               || targetOwner != null && targetOwner.equals(observer.m_20148_());
         }
      } else {
         return true;
      }
   }

   private static boolean isOwnedOrTamed(Entity entity) {
      return entity instanceof TamableAnimal animal && animal.m_21824_() || entity instanceof OwnableEntity && ownerId(entity) != null;
   }

   private static UUID ownerId(Entity entity) {
      if (entity instanceof Player player) {
         return player.m_20148_();
      } else if (entity instanceof NpcOwnerAccess npc) {
         return npc.getOwner().orElse(null);
      } else {
         if (entity instanceof TamableAnimal animal && !animal.m_21824_()) {
            return null;
         }

         return entity instanceof OwnableEntity ownable ? ownable.m_21805_() : null;
      }
   }

   public static enum Disposition {
      FRIENDLY,
      NEUTRAL,
      HOSTILE;
   }
}
