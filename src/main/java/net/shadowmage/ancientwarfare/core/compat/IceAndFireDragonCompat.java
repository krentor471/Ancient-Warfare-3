package net.shadowmage.ancientwarfare.core.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

public final class IceAndFireDragonCompat {
   private static final String DRAGON_BASE = "com.github.alexthe666.iceandfire.entity.EntityDragonBase";
   private static Class<?> dragonClass;
   private static boolean searched;

   private IceAndFireDragonCompat() {
   }

   private static Class<?> dragonClass() {
      if (!searched) {
         searched = true;

         try {
            dragonClass = Class.forName("com.github.alexthe666.iceandfire.entity.EntityDragonBase");
         } catch (Throwable var1) {
            dragonClass = null;
         }
      }

      return dragonClass;
   }

   public static boolean isDragon(Entity entity) {
      Class<?> type = dragonClass();
      return type != null && entity != null && type.isInstance(entity);
   }

   public static boolean canBeMountedBy(Entity dragon, UUID owner) {
      if (isDragon(dragon) && owner != null) {
         long playerPassengers = dragon.getPassengers().stream().filter(passenger -> passenger instanceof Player).count();
         if (playerPassengers > 0L) {
            AncientWarfareCore.LOG.info("Dragon mount rejected: dragon has {} real player passenger(s), passengers={}", playerPassengers, dragon.getPassengers());
            return false;
         } else {
            Boolean tame = dragon instanceof TamableAnimal tamable ? tamable.isTame() : invokeBoolean(dragon, "isTame");
            Integer stage = invokeInt(dragon, "getDragonStage");
            UUID dragonOwner = ownerUuid(dragon);
            boolean valid = Boolean.TRUE.equals(tame) && stage != null && stage >= 3 && owner.equals(dragonOwner);
            if (!valid) {
               AncientWarfareCore.LOG
                  .info(
                     "Dragon mount rejected: tame={}, stage={}, dragonOwner={}, commandOwner={}, passengers={}, class={}",
                     new Object[]{tame, stage, dragonOwner, owner, dragon.getPassengers().size(), dragon.getClass().getName()}
                  );
            }

            return valid;
         }
      } else {
         return false;
      }
   }

   public static void detachRealPassenger(Entity rider) {
      if (rider != null && rider.isPassenger()) {
         rider.stopRiding();
      }
   }

   public static void prepareForRider(Entity dragon) {
      if (isDragon(dragon)) {
         for (Entity passenger : new ArrayList(dragon.getPassengers())) {
            if (!(passenger instanceof Player)) {
               passenger.stopRiding();
            }
         }

         invoke(dragon, "setTarget", new Class[]{LivingEntity.class}, new Object[]{null});
         invoke(dragon, "setFlying", new Class[]{boolean.class}, new Object[]{false});
         invoke(dragon, "setHovering", new Class[]{boolean.class}, new Object[]{false});
         invoke(dragon, "setTackling", new Class[]{boolean.class}, new Object[]{false});
         clearAnimation(dragon);
      }
   }

   public static void clearNativeTarget(Entity dragon) {
      if (isDragon(dragon)) {
         invoke(dragon, "setTarget", new Class[]{LivingEntity.class}, new Object[]{null});
      }
   }

   public static void setFlightTarget(Entity dragon, Vec3 target) {
      if (isDragon(dragon) && target != null) {
         try {
            Object manager = dragon.getClass().getField("flightManager").get(dragon);
            Method method = manager.getClass().getMethod("setFlightTarget", Vec3.class);
            method.invoke(manager, target);
         } catch (Throwable var4) {
         }
      }
   }

   public static void setFlying(Entity dragon, boolean flying) {
      if (isDragon(dragon)) {
         invoke(dragon, "setFlying", new Class[]{boolean.class}, new Object[]{flying});
      }
   }

   public static void setHovering(Entity dragon, boolean hovering) {
      if (isDragon(dragon)) {
         invoke(dragon, "setHovering", new Class[]{boolean.class}, new Object[]{hovering});
      }
   }

   public static void setTackling(Entity dragon, boolean tackling) {
      if (isDragon(dragon)) {
         invoke(dragon, "setTackling", new Class[]{boolean.class}, new Object[]{tackling});
      }
   }

   public static void breakNearbyBlocks(Entity dragon) {
      if (isDragon(dragon)) {
         invoke(dragon, "breakBlocks", new Class[]{boolean.class}, new Object[]{true});
      }
   }

   public static void setBreathingFire(Entity dragon, boolean breathing, LivingEntity target) {
      if (isDragon(dragon)) {
         invoke(dragon, "setBreathingFire", new Class[]{boolean.class}, new Object[]{breathing});
      }
   }

   public static void startBreathingFire(Entity dragon) {
      if (isDragon(dragon)) {
         try {
            Field animation = dragon.getClass().getField("ANIMATION_FIRECHARGE");
            Object fireCharge = animation.get(null);
            if (fireCharge != null) {
               invoke(dragon, "setAnimation", new Class[]{fireCharge.getClass()}, new Object[]{fireCharge});
            }
         } catch (Throwable var3) {
         }
      }
   }

   public static void stimulateFireAt(Entity dragon, Vec3 point) {
      if (isDragon(dragon) && point != null) {
         try {
            Method method = dragon.getClass().getMethod("stimulateFire", double.class, double.class, double.class, int.class);
            method.invoke(dragon, point.x, point.y, point.z, 1);
         } catch (Throwable var3) {
         }
      }
   }

   public static Vec3 riderPosition(Entity dragon) {
      if (!isDragon(dragon)) {
         return dragon.position();
      } else {
         int stage = Math.max(1, Math.min(5, Objects.requireNonNullElse(invokeInt(dragon, "getDragonStage"), 3)));

         return dragon.position().add(0.0, switch (stage) {
            case 1 -> 0.9;
            case 2 -> 1.45;
            default -> 2.35;
            case 4 -> 4.85;
            case 5 -> 8.55;
         }, 0.0);
      }
   }

   public static void bite(Entity dragon, LivingEntity target) {
      if (isDragon(dragon) && target != null) {
         invoke(dragon, "doHurtTarget", new Class[]{Entity.class}, new Object[]{target});

         try {
            double damage = dragon.getClass().getMethod("getAttribute", Attribute.class).invoke(dragon, Attributes.ATTACK_DAMAGE) instanceof AttributeInstance instance
               ? instance.getValue()
               : 1.0;
            if (dragon instanceof LivingEntity livingDragon) {
               target.hurt(target.level().damageSources().mobAttack(livingDragon), (float)damage);
            }
         } catch (Throwable var6) {
         }
      }
   }

   public static void clearAnimation(Entity dragon) {
      if (isDragon(dragon)) {
         clearDragonAnimation(dragon);
      }
   }

   public static void stopNativeAI(Entity dragon) {
      if (isDragon(dragon)) {
         try {
            Object goalSelector = fieldValue(dragon, "goalSelector");
            Object targetSelector = fieldValue(dragon, "targetSelector");
            if (goalSelector == null || targetSelector == null) {
               return;
            }

            Method removeAll = goalSelector.getClass().getMethod("removeAllGoals", Predicate.class);
            removeAll.invoke(goalSelector, (Predicate<Object>)goal -> true);
            removeAll.invoke(targetSelector, (Predicate<Object>)goal -> true);
         } catch (Throwable var4) {
         }
      }
   }

   private static void clearDragonAnimation(Entity dragon) {
      try {
         Class<?> animated = Class.forName("com.github.alexthe666.citadel.animation.IAnimatedEntity");
         Object none = animated.getField("NO_ANIMATION").get(null);
         Method set = dragon.getClass().getMethod("setAnimation", none.getClass());
         set.invoke(dragon, none);
      } catch (Throwable var4) {
      }
   }

   private static Object fieldValue(Object target, String name) throws ReflectiveOperationException {
      for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
         try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
         } catch (NoSuchFieldException var4) {
         }
      }

      return null;
   }

   private static void invoke(Object target, String name, Class<?>[] types, Object[] args) {
      try {
         target.getClass().getMethod(name, types).invoke(target, args);
      } catch (Throwable var5) {
      }
   }

   private static Boolean invokeBoolean(Object target, String name) {
      try {
         return (Boolean)target.getClass().getMethod(name).invoke(target);
      } catch (Throwable var3) {
         return null;
      }
   }

   private static Integer invokeInt(Object target, String name) {
      try {
         return (Integer)target.getClass().getMethod(name).invoke(target);
      } catch (Throwable var3) {
         return null;
      }
   }

   private static UUID invokeUuid(Object target, String name) {
      try {
         return (UUID)target.getClass().getMethod(name).invoke(target);
      } catch (Throwable var3) {
         return null;
      }
   }

   private static UUID ownerUuid(Entity dragon) {
      if (dragon instanceof OwnableEntity ownable) {
         UUID direct = ownable.getOwnerUUID();
         if (direct != null) {
            return direct;
         }

         Entity owner = ownable.getOwner();
         if (owner != null) {
            return owner.getUUID();
         }
      }

      UUID directx = invokeUuid(dragon, "getOwnerUUID");
      if (directx != null) {
         return directx;
      } else {
         try {
            if (dragon.getClass().getMethod("getOwner").invoke(dragon) instanceof LivingEntity living) {
               return living.getUUID();
            }
         } catch (Throwable var4) {
         }

         return null;
      }
   }
}
