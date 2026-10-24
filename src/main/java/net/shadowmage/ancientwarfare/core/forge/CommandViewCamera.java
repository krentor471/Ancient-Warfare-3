package net.shadowmage.ancientwarfare.core.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;

/**
 * Top-down camera for the command view. It is a client-only marker entity set as the camera entity: it looks down at a
 * focus point on the ground from a distance, and the player's own body stays where it was.
 */
final class CommandViewCamera {
   private static final float PITCH = 55.0F;
   private static final double MIN_DISTANCE = 8.0;
   private static final double MAX_DISTANCE = 64.0;
   private static final double PAN_SPEED = 0.035;
   private static final float TURN_SPEED = 4.0F;
   private static final double MAX_FOCUS_RANGE = CommandBatonItem.COMMAND_VIEW_RANGE - 16.0;
   private static Marker camera;
   private static double focusX;
   private static double focusY;
   private static double focusZ;
   private static float yaw;
   private static double distance = 28.0;

   private CommandViewCamera() {
   }

   static boolean isActive() {
      return camera != null;
   }

   static void start(Minecraft minecraft) {
      focusX = minecraft.player.getX();
      focusY = minecraft.player.getY();
      focusZ = minecraft.player.getZ();
      yaw = minecraft.player.getYRot();
      camera = new Marker(EntityType.MARKER, minecraft.level);
      place();
      camera.setOldPosAndRot();
      minecraft.setCameraEntity(camera);
   }

   static void stop(Minecraft minecraft) {
      if (camera != null) {
         camera = null;
         if (minecraft.player != null) {
            minecraft.setCameraEntity(minecraft.player);
         }
      }
   }

   /** One client tick of camera motion: {@code forward} and {@code strafe} pan, {@code turn} rotates, {@code zoom} moves in and out. */
   static void tick(Minecraft minecraft, double forward, double strafe, float turn, double zoom) {
      if (camera != null && minecraft.player != null) {
         camera.setOldPosAndRot();
         yaw += turn * TURN_SPEED;
         distance = Mth.clamp(distance * (1.0 - zoom * 0.12), MIN_DISTANCE, MAX_DISTANCE);
         double speed = distance * PAN_SPEED;
         double radians = Math.toRadians(yaw);
         double forwardX = -Math.sin(radians);
         double forwardZ = Math.cos(radians);
         focusX += (forwardX * forward - forwardZ * strafe) * speed;
         focusZ += (forwardZ * forward + forwardX * strafe) * speed;
         double dx = focusX - minecraft.player.getX();
         double dz = focusZ - minecraft.player.getZ();
         double range = Math.sqrt(dx * dx + dz * dz);
         if (range > MAX_FOCUS_RANGE) {
            focusX = minecraft.player.getX() + dx / range * MAX_FOCUS_RANGE;
            focusZ = minecraft.player.getZ() + dz / range * MAX_FOCUS_RANGE;
         }

         int ground = minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(focusX), Mth.floor(focusZ));
         if (ground > minecraft.level.getMinBuildHeight()) {
            focusY += (ground - focusY) * 0.2;
         }

         place();
      }
   }

   private static void place() {
      Vec3 look = Vec3.directionFromRotation(PITCH, yaw);
      camera.setPos(focusX - look.x * distance, focusY - look.y * distance, focusZ - look.z * distance);
      camera.setYRot(yaw);
      camera.setXRot(PITCH);
   }
}
