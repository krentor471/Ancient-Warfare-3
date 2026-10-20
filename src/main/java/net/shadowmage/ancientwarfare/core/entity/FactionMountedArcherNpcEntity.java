package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;

public class FactionMountedArcherNpcEntity extends FactionArcherNpcEntity {
   private static final double ORBIT_SPEED = 0.45;
   private static final double ORBIT_RADIUS = 14.0;
   private static final double ORBIT_MIN_RADIUS = 10.0;
   private static final double ORBIT_MAX_RADIUS = 18.0;
   private boolean orbitClockwise;
   private LivingEntity orbitTarget;

   public FactionMountedArcherNpcEntity(EntityType<? extends FactionArcherNpcEntity> entityType, Level level) {
      super(entityType, level, FactionNpcRole.MOUNTED_ARCHER);
   }

   public static Builder createAttributes() {
      return createMobAttributes().add(Attributes.MAX_HEALTH, 24.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 40.0);
   }

   @Override
   protected boolean usesCustomMountedMovement() {
      return true;
   }

   @Override
   public void aiStep() {
      super.aiStep();
      this.tickMountedOrbit();
   }

   private void tickMountedOrbit() {
      if (this.getVehicle() instanceof Mob mount) {
         if (!NpcWaterEscape.isRecoveryActive(this)) {
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
               if (this.orbitTarget != target) {
                  this.orbitTarget = target;
                  this.orbitClockwise = this.getRandom().nextBoolean();
               }

               Vec3 offset = mount.position().subtract(target.position());
               Vec3 radial = new Vec3(offset.x, 0.0, offset.z);
               double distance = radial.length();
               if (distance < 0.001) {
                  radial = new Vec3(1.0, 0.0, 0.0);
                  distance = 1.0;
               } else {
                  radial = radial.scale(1.0 / distance);
               }

               double radialCorrection = 0.0;
               if (distance < 10.0) {
                  radialCorrection = 1.0;
               } else if (distance > 18.0) {
                  radialCorrection = -1.0;
               } else {
                  radialCorrection = (14.0 - distance) * 0.12;
               }

               Vec3 direction = this.orbitDirection(radial, radialCorrection, this.orbitClockwise);
               if (!NpcMovementSafety.canContinueMountedOrbit(mount, direction)) {
                  this.orbitClockwise = !this.orbitClockwise;
                  direction = this.orbitDirection(radial, radialCorrection, this.orbitClockwise);
                  if (!NpcMovementSafety.canContinueMountedOrbit(mount, direction)) {
                     mount.getNavigation().stop();
                     mount.setDeltaMovement(mount.getDeltaMovement().multiply(0.35, 1.0, 0.35));
                     return;
                  }
               }

               AttributeInstance movement = mount.getAttribute(Attributes.MOVEMENT_SPEED);
               if (movement != null) {
                  movement.setBaseValue(Math.max(movement.getBaseValue(), 0.45));
               }

               Vec3 current = mount.getDeltaMovement();
               mount.setDeltaMovement(direction.x * 0.45, current.y, direction.z * 0.45);
               mount.getNavigation().stop();
               this.faceDirection(mount, direction);
               this.faceDirection(this, target.position().subtract(this.position()));
            } else {
               this.orbitTarget = null;
               mount.getNavigation().stop();
               mount.setDeltaMovement(mount.getDeltaMovement().multiply(0.45, 1.0, 0.45));
            }
         }
      }
   }

   private Vec3 orbitDirection(Vec3 radial, double radialCorrection, boolean clockwise) {
      Vec3 tangent = clockwise ? new Vec3(-radial.z, 0.0, radial.x) : new Vec3(radial.z, 0.0, -radial.x);
      return tangent.add(radial.scale(radialCorrection)).normalize();
   }

   private void faceDirection(Mob mount, Vec3 direction) {
      float yaw = (float)(Math.atan2(direction.z, direction.x) * 180.0F / (float)Math.PI) - 90.0F;
      mount.setYRot(yaw);
      mount.setYBodyRot(yaw);
      mount.setYHeadRot(yaw);
   }

   private void faceDirection(FactionMountedArcherNpcEntity rider, Vec3 direction) {
      Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
      if (!(horizontal.lengthSqr() < 1.0E-4)) {
         float yaw = (float)(Math.atan2(horizontal.z, horizontal.x) * 180.0F / (float)Math.PI) - 90.0F;
         rider.setYRot(yaw);
         rider.setYBodyRot(yaw);
         rider.setYHeadRot(yaw);
      }
   }
}
