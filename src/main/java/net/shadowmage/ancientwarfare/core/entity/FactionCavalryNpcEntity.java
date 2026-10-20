package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;
import net.shadowmage.ancientwarfare.core.common.npc.NpcEquipmentResolver;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargeting;

public class FactionCavalryNpcEntity extends FactionSoldierNpcEntity {
   private static final double CHARGE_SPEED = 0.36;
   private static final double ATTACK_DISTANCE = 3.2;
   private static final double PASS_DISTANCE = 7.0;
   private static final int PASS_TICKS = 24;
   private Vec3 passDestination;
   private int passTicks;
   private int attackCooldown;
   private boolean hasAttackedThisCharge;

   public FactionCavalryNpcEntity(EntityType<? extends FactionSoldierNpcEntity> entityType, Level level) {
      super(entityType, level, FactionNpcRole.CAVALRY, new ItemStack(Items.f_42383_));
   }

   public static Builder createAttributes() {
      return m_21552_()
         .m_22268_(Attributes.f_22276_, 28.0)
         .m_22268_(Attributes.f_22279_, 0.3)
         .m_22268_(Attributes.f_22281_, 4.0)
         .m_22268_(Attributes.f_22283_, 4.0)
         .m_22268_(Attributes.f_22277_, 35.0);
   }

   @Override
   protected void m_8099_() {
      this.f_21345_.m_25352_(0, new FloatGoal(this));
      this.f_21345_.m_25352_(2, new FactionSoldierNpcEntity.FactionMeleeAttackGoal(this, this.playerSprintNavigationSpeed(), true));
      this.f_21345_.m_25352_(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.f_21345_.m_25352_(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.f_21345_.m_25352_(7, new RandomLookAroundGoal(this));
      this.addFactionCombatTargets();
   }

   @Override
   protected boolean usesCustomMountedMovement() {
      return true;
   }

   @Override
   public void m_8107_() {
      super.m_8107_();
      this.tickCharge();
   }

   private void tickCharge() {
      if (this.m_20202_() instanceof Mob mount) {
         if (!NpcWaterEscape.isRecoveryActive(this)) {
            LivingEntity target = this.m_5448_();
            if (!this.validChargeTarget(target)) {
               this.stopChargeMovement(mount);
               this.passDestination = null;
               this.hasAttackedThisCharge = false;
            } else {
               if (this.attackCooldown > 0) {
                  this.attackCooldown--;
               }

               this.m_21563_().m_24960_(target, 30.0F, 30.0F);
               if (this.passDestination != null) {
                  Vec3 destination = this.passDestination;
                  this.passTicks--;
                  this.driveToward(mount, destination);
                  if (this.passDestination == null) {
                     this.hasAttackedThisCharge = false;
                  } else {
                     if (this.passTicks <= 0 || mount.m_20182_().m_82557_(destination) < 3.0) {
                        this.passDestination = null;
                        this.hasAttackedThisCharge = false;
                     }
                  }
               } else {
                  double attackDistance = Math.max(3.2, NpcEquipmentResolver.meleeAttackReach(this));
                  if (mount.m_20280_(target) > attackDistance * attackDistance) {
                     this.hasAttackedThisCharge = false;
                     this.driveToward(mount, target.m_20182_());
                  } else {
                     if (!this.hasAttackedThisCharge && this.attackCooldown <= 0) {
                        this.swingWeapon();
                        this.m_7327_(target);
                        this.hasAttackedThisCharge = true;
                        this.attackCooldown = NpcEquipmentResolver.meleeAttackInterval(this);
                        Vec3 direction = target.m_20182_().m_82546_(mount.m_20182_());
                        Vec3 horizontal = new Vec3(direction.f_82479_, 0.0, direction.f_82481_);
                        horizontal = horizontal.m_82556_() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : horizontal.m_82541_();
                        this.passDestination = target.m_20182_().m_82549_(horizontal.m_82490_(7.0));
                        this.passTicks = 24;
                     }
                  }
               }
            }
         }
      }
   }

   private boolean validChargeTarget(LivingEntity target) {
      return NpcTargeting.canHarm(this, target);
   }

   private void stopChargeMovement(Mob mount) {
      mount.m_21573_().m_26573_();
      mount.m_20256_(mount.m_20184_().m_82542_(0.45, 1.0, 0.45));
   }

   private void driveToward(Mob mount, Vec3 destination) {
      Vec3 delta = destination.m_82546_(mount.m_20182_());
      Vec3 horizontal = new Vec3(delta.f_82479_, 0.0, delta.f_82481_);
      if (!(horizontal.m_82556_() < 1.0E-4)) {
         Vec3 direction = horizontal.m_82541_();
         if (!NpcMovementSafety.canMoveMountedSafely(mount, direction)) {
            this.passDestination = null;
            this.stopChargeMovement(mount);
         } else {
            Vec3 current = mount.m_20184_();
            AttributeInstance movement = mount.m_21051_(Attributes.f_22279_);
            if (movement != null) {
               movement.m_22100_(Math.max(movement.m_22115_(), 0.36));
            }

            mount.m_20334_(direction.f_82479_ * 0.36, current.f_82480_, direction.f_82481_ * 0.36);
            mount.m_21573_().m_26573_();
            float yaw = (float)(Math.atan2(direction.f_82481_, direction.f_82479_) * 180.0F / (float)Math.PI) - 90.0F;
            mount.m_146922_(yaw);
            mount.m_5618_(yaw);
            mount.m_5616_(yaw);
            this.m_146922_(yaw);
            this.m_5618_(yaw);
            this.m_5616_(yaw);
         }
      }
   }
}
