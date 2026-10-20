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
      super(entityType, level, FactionNpcRole.CAVALRY, new ItemStack(Items.IRON_SWORD));
   }

   public static Builder createAttributes() {
      return createMobAttributes()
         .add(Attributes.MAX_HEALTH, 28.0)
         .add(Attributes.MOVEMENT_SPEED, 0.3)
         .add(Attributes.ATTACK_DAMAGE, 4.0)
         .add(Attributes.ATTACK_SPEED, 4.0)
         .add(Attributes.FOLLOW_RANGE, 35.0);
   }

   @Override
   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(2, new FactionSoldierNpcEntity.FactionMeleeAttackGoal(this, this.playerSprintNavigationSpeed(), true));
      this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.addFactionCombatTargets();
   }

   @Override
   protected boolean usesCustomMountedMovement() {
      return true;
   }

   @Override
   public void aiStep() {
      super.aiStep();
      this.tickCharge();
   }

   private void tickCharge() {
      if (this.getVehicle() instanceof Mob mount) {
         if (!NpcWaterEscape.isRecoveryActive(this)) {
            LivingEntity target = this.getTarget();
            if (!this.validChargeTarget(target)) {
               this.stopChargeMovement(mount);
               this.passDestination = null;
               this.hasAttackedThisCharge = false;
            } else {
               if (this.attackCooldown > 0) {
                  this.attackCooldown--;
               }

               this.getLookControl().setLookAt(target, 30.0F, 30.0F);
               if (this.passDestination != null) {
                  Vec3 destination = this.passDestination;
                  this.passTicks--;
                  this.driveToward(mount, destination);
                  if (this.passDestination == null) {
                     this.hasAttackedThisCharge = false;
                  } else {
                     if (this.passTicks <= 0 || mount.position().distanceToSqr(destination) < 3.0) {
                        this.passDestination = null;
                        this.hasAttackedThisCharge = false;
                     }
                  }
               } else {
                  double attackDistance = Math.max(3.2, NpcEquipmentResolver.meleeAttackReach(this));
                  if (mount.distanceToSqr(target) > attackDistance * attackDistance) {
                     this.hasAttackedThisCharge = false;
                     this.driveToward(mount, target.position());
                  } else {
                     if (!this.hasAttackedThisCharge && this.attackCooldown <= 0) {
                        this.swingWeapon();
                        this.doHurtTarget(target);
                        this.hasAttackedThisCharge = true;
                        this.attackCooldown = NpcEquipmentResolver.meleeAttackInterval(this);
                        Vec3 direction = target.position().subtract(mount.position());
                        Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
                        horizontal = horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
                        this.passDestination = target.position().add(horizontal.scale(7.0));
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
      mount.getNavigation().stop();
      mount.setDeltaMovement(mount.getDeltaMovement().multiply(0.45, 1.0, 0.45));
   }

   private void driveToward(Mob mount, Vec3 destination) {
      Vec3 delta = destination.subtract(mount.position());
      Vec3 horizontal = new Vec3(delta.x, 0.0, delta.z);
      if (!(horizontal.lengthSqr() < 1.0E-4)) {
         Vec3 direction = horizontal.normalize();
         if (!NpcMovementSafety.canMoveMountedSafely(mount, direction)) {
            this.passDestination = null;
            this.stopChargeMovement(mount);
         } else {
            Vec3 current = mount.getDeltaMovement();
            AttributeInstance movement = mount.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movement != null) {
               movement.setBaseValue(Math.max(movement.getBaseValue(), 0.36));
            }

            mount.setDeltaMovement(direction.x * 0.36, current.y, direction.z * 0.36);
            mount.getNavigation().stop();
            float yaw = (float)(Math.atan2(direction.z, direction.x) * 180.0F / (float)Math.PI) - 90.0F;
            mount.setYRot(yaw);
            mount.setYBodyRot(yaw);
            mount.setYHeadRot(yaw);
            this.setYRot(yaw);
            this.setYBodyRot(yaw);
            this.setYHeadRot(yaw);
         }
      }
   }
}
