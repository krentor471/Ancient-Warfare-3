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
      return m_21552_().m_22268_(Attributes.f_22276_, 24.0).m_22268_(Attributes.f_22279_, 0.3).m_22268_(Attributes.f_22277_, 40.0);
   }

   @Override
   protected boolean usesCustomMountedMovement() {
      return true;
   }

   @Override
   public void m_8107_() {
      super.m_8107_();
      this.tickMountedOrbit();
   }

   private void tickMountedOrbit() {
      if (this.m_20202_() instanceof Mob mount) {
         if (!NpcWaterEscape.isRecoveryActive(this)) {
            LivingEntity target = this.m_5448_();
            if (target != null && target.m_6084_()) {
               if (this.orbitTarget != target) {
                  this.orbitTarget = target;
                  this.orbitClockwise = this.m_217043_().m_188499_();
               }

               Vec3 offset = mount.m_20182_().m_82546_(target.m_20182_());
               Vec3 radial = new Vec3(offset.f_82479_, 0.0, offset.f_82481_);
               double distance = radial.m_82553_();
               if (distance < 0.001) {
                  radial = new Vec3(1.0, 0.0, 0.0);
                  distance = 1.0;
               } else {
                  radial = radial.m_82490_(1.0 / distance);
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
                     mount.m_21573_().m_26573_();
                     mount.m_20256_(mount.m_20184_().m_82542_(0.35, 1.0, 0.35));
                     return;
                  }
               }

               AttributeInstance movement = mount.m_21051_(Attributes.f_22279_);
               if (movement != null) {
                  movement.m_22100_(Math.max(movement.m_22115_(), 0.45));
               }

               Vec3 current = mount.m_20184_();
               mount.m_20334_(direction.f_82479_ * 0.45, current.f_82480_, direction.f_82481_ * 0.45);
               mount.m_21573_().m_26573_();
               this.faceDirection(mount, direction);
               this.faceDirection(this, target.m_20182_().m_82546_(this.m_20182_()));
            } else {
               this.orbitTarget = null;
               mount.m_21573_().m_26573_();
               mount.m_20256_(mount.m_20184_().m_82542_(0.45, 1.0, 0.45));
            }
         }
      }
   }

   private Vec3 orbitDirection(Vec3 radial, double radialCorrection, boolean clockwise) {
      Vec3 tangent = clockwise ? new Vec3(-radial.f_82481_, 0.0, radial.f_82479_) : new Vec3(radial.f_82481_, 0.0, -radial.f_82479_);
      return tangent.m_82549_(radial.m_82490_(radialCorrection)).m_82541_();
   }

   private void faceDirection(Mob mount, Vec3 direction) {
      float yaw = (float)(Math.atan2(direction.f_82481_, direction.f_82479_) * 180.0F / (float)Math.PI) - 90.0F;
      mount.m_146922_(yaw);
      mount.m_5618_(yaw);
      mount.m_5616_(yaw);
   }

   private void faceDirection(FactionMountedArcherNpcEntity rider, Vec3 direction) {
      Vec3 horizontal = new Vec3(direction.f_82479_, 0.0, direction.f_82481_);
      if (!(horizontal.m_82556_() < 1.0E-4)) {
         float yaw = (float)(Math.atan2(horizontal.f_82481_, horizontal.f_82479_) * 180.0F / (float)Math.PI) - 90.0F;
         rider.m_146922_(yaw);
         rider.m_5618_(yaw);
         rider.m_5616_(yaw);
      }
   }
}
