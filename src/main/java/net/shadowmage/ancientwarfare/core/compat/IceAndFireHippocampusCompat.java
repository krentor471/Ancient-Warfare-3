package net.shadowmage.ancientwarfare.core.compat;

import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

public final class IceAndFireHippocampusCompat {
   private static final String CLASS_NAME = "com.github.alexthe666.iceandfire.entity.EntityHippocampus";
   private static Class<?> hippocampusClass;
   private static boolean searched;

   private IceAndFireHippocampusCompat() {
   }

   private static Class<?> type() {
      if (!searched) {
         searched = true;

         try {
            hippocampusClass = Class.forName("com.github.alexthe666.iceandfire.entity.EntityHippocampus");
         } catch (Throwable var1) {
            hippocampusClass = null;
         }
      }

      return hippocampusClass;
   }

   public static boolean isHippocampus(Entity entity) {
      Class<?> type = type();
      return type != null && entity != null && type.isInstance(entity);
   }

   public static boolean canBeMountedBy(Entity mount, UUID owner) {
      if (isHippocampus(mount) && owner != null) {
         Boolean tame = mount instanceof TamableAnimal tamable ? tamable.isTame() : invokeBoolean(mount, "isTame");
         Boolean saddled = mount instanceof Saddleable saddleable ? saddleable.isSaddled() : invokeBoolean(mount, "isSaddled");
         UUID mountOwner = mount instanceof TamableAnimal tamablex ? tamablex.getOwnerUUID() : invokeUuid(mount, "getOwnerUUID");
         boolean valid = Boolean.TRUE.equals(tame) && Boolean.TRUE.equals(saddled) && owner.equals(mountOwner) && mount.getPassengers().isEmpty();
         if (!valid) {
            AncientWarfareCore.LOG
               .info(
                  "Hippocampus mount rejected: tame={}, saddled={}, mountOwner={}, commandOwner={}, passengers={}",
                  new Object[]{tame, saddled, mountOwner, owner, mount.getPassengers()}
               );
         }

         return valid;
      } else {
         return false;
      }
   }

   public static void setControlState(Entity mount, byte state) {
      if (isHippocampus(mount)) {
         invoke(mount, "setControlState", byte.class, state);
      }
   }

   public static void setAttack(Entity mount, boolean value) {
      if (isHippocampus(mount)) {
         invoke(mount, "attack", boolean.class, value);
      }
   }

   public static void prepareForRider(Entity mount) {
      if (isHippocampus(mount)) {
         if (mount instanceof Mob mob) {
            mob.setNoAi(true);
            mob.getNavigation().stop();
            mob.setTarget(null);
         } else {
            invoke(mount, "setNoAi", boolean.class, true);
            invoke(mount, "setTarget", LivingEntity.class, null);
         }

         mount.setDeltaMovement(Vec3.ZERO);
         mount.setNoGravity(true);
         setControlState(mount, (byte)0);
         setAttack(mount, false);
      }
   }

   public static void restoreAfterRider(Entity mount) {
      if (isHippocampus(mount)) {
         if (mount instanceof Mob mob) {
            mob.setNoAi(false);
            mob.getNavigation().stop();
         } else {
            invoke(mount, "setNoAi", boolean.class, false);
         }

         mount.setNoGravity(false);
         mount.setDeltaMovement(Vec3.ZERO);
      }
   }

   private static void invoke(Entity target, String name, Class<?> parameter, Object value) {
      try {
         target.getClass().getMethod(name, parameter).invoke(target, value);
      } catch (Throwable var5) {
      }
   }

   private static Boolean invokeBoolean(Entity target, String name) {
      try {
         return (Boolean)target.getClass().getMethod(name).invoke(target);
      } catch (Throwable var3) {
         return null;
      }
   }

   private static UUID invokeUuid(Entity target, String name) {
      try {
         return target.getClass().getMethod(name).invoke(target) instanceof UUID uuid ? uuid : null;
      } catch (Throwable var4) {
         return null;
      }
   }
}
