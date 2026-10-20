package net.shadowmage.ancientwarfare.core.compat;

import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.TamableAnimal;

public final class IceAndFireHippogryphCompat {
   private static final String HIPPogryph = "com.github.alexthe666.iceandfire.entity.EntityHippogryph";
   private static Class<?> hippogryphClass;
   private static boolean searched;

   private IceAndFireHippogryphCompat() {
   }

   private static Class<?> type() {
      if (!searched) {
         searched = true;

         try {
            hippogryphClass = Class.forName("com.github.alexthe666.iceandfire.entity.EntityHippogryph");
         } catch (Throwable var1) {
            hippogryphClass = null;
         }
      }

      return hippogryphClass;
   }

   public static boolean isHippogryph(Entity entity) {
      Class<?> type = type();
      return type != null && entity != null && type.isInstance(entity);
   }

   public static boolean canBeMountedBy(Entity mount, UUID owner) {
      if (isHippogryph(mount) && owner != null) {
         Boolean tame = mount instanceof TamableAnimal tamable ? tamable.isTame() : invokeBoolean(mount, "isTame");
         Boolean saddled = mount instanceof Saddleable saddleable ? saddleable.isSaddled() : invokeBoolean(mount, "isSaddled");
         UUID mountOwner = null;
         if (mount instanceof TamableAnimal tamablex) {
            mountOwner = tamablex.getOwnerUUID();
         }

         if (mountOwner == null) {
            mountOwner = invokeUuid(mount, "getOwnerUUID");
         }

         return Boolean.TRUE.equals(tame) && Boolean.TRUE.equals(saddled) && owner.equals(mountOwner) && mount.getPassengers().isEmpty();
      } else {
         return false;
      }
   }

   public static void prepareForRider(Entity mount) {
      if (isHippogryph(mount)) {
         invoke(mount, "setFlying", boolean.class, false);
         invoke(mount, "setHovering", boolean.class, false);
         invoke(mount, "setControlState", byte.class, (byte)0);
      }
   }

   public static void setFlying(Entity mount, boolean value) {
      if (isHippogryph(mount)) {
         invoke(mount, "setFlying", boolean.class, value);
      }
   }

   public static void setHovering(Entity mount, boolean value) {
      if (isHippogryph(mount)) {
         invoke(mount, "setHovering", boolean.class, value);
      }
   }

   public static void setAttack(Entity mount, boolean value) {
      if (isHippogryph(mount)) {
         invoke(mount, "attack", boolean.class, value);
      }
   }

   public static void setControlState(Entity mount, byte value) {
      if (isHippogryph(mount)) {
         invoke(mount, "setControlState", byte.class, value);
      }
   }

   private static void invoke(Entity target, String name, Class<?> parameter, Object value) {
      try {
         Method method = target.getClass().getMethod(name, parameter);
         method.invoke(target, value);
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
