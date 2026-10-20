package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;

public class FactionArcherNpcEntity extends FactionNpcEntity implements RangedAttackMob {
   private static final double MIN_ARCHER_FOLLOW_RANGE = 72.0;
   private static final double ARCHER_VERTICAL_TARGET_RANGE = 72.0;
   private static final int ATTACK_INTERVAL_TICKS = 30;
   private static final double MIN_ATTACK_RANGE = 8.0;
   private static final double RETREAT_START_RANGE = 6.0;
   private static final double RETREAT_END_RANGE = 10.0;
   private static final double CLOSE_REPOSITION_RANGE = 12.0;
   private static final double FIRING_POSITION_SEARCH_RADIUS = 12.0;
   private static final int FIRING_POSITION_REPATH_TICKS = 40;
   private static final int FIRING_POSITION_SAMPLES = 6;
   private static final int BOW_DRAW_TICKS = 20;

   public FactionArcherNpcEntity(EntityType<? extends FactionArcherNpcEntity> entityType, Level level) {
      super(entityType, level, FactionNpcRole.ARCHER);
      this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
      this.applyFactionDefaults();
      this.ensureArcherFollowRange(FactionNpcRole.ARCHER);
   }

   protected FactionArcherNpcEntity(EntityType<? extends FactionArcherNpcEntity> entityType, Level level, FactionNpcRole role) {
      super(entityType, level, role);
      this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
      this.applyFactionDefaults();
      this.ensureArcherFollowRange(role);
   }

   @Override
   protected boolean supportsCombatPromotion() {
      return !this.getNpcSubtype().endsWith(".elite");
   }

   public static Builder createAttributes() {
      return createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.FOLLOW_RANGE, 32.0);
   }

   private void ensureArcherFollowRange(FactionNpcRole role) {
      AttributeInstance followRange = this.getAttribute(Attributes.FOLLOW_RANGE);
      if (followRange != null) {
         double minimum = role == FactionNpcRole.MOUNTED_ARCHER ? 64.0 : 72.0;
         followRange.setBaseValue(Math.max(followRange.getBaseValue(), minimum));
      }
   }

   @Override
   protected boolean usesMorale() {
      return true;
   }

   protected void registerGoals() {
      this.addSharedGoals();
      this.goalSelector.addGoal(2, new FactionArcherNpcEntity.ArcherRangedAttackGoal(this));
      this.addFactionCombatTargets(false);
   }

   @Override
   public void aiStep() {
      this.ensureArcherFollowRange(this.getNpcSubtype().equals(FactionNpcRole.MOUNTED_ARCHER.id()) ? FactionNpcRole.MOUNTED_ARCHER : FactionNpcRole.ARCHER);
      super.aiStep();
   }

   public void performRangedAttack(LivingEntity target, float velocity) {
      if (target != null && target.isAlive()) {
         this.recordCombatAttack();
         if (this.getMainHandItem().getItem() instanceof CrossbowItem) {
            this.performCrossbowAttack(target);
         } else {
            ItemStack projectileStack = this.getProjectile(this.getMainHandItem());
            AbstractArrow arrow = ProjectileUtil.getMobArrow(this, projectileStack, velocity);
            float projectileSpeed = this.calculateProjectileVelocity(target);
            Vec3 start = new Vec3(this.getX(), arrow.getY(), this.getZ());
            NpcRangedAttackHelper.BallisticShot shot = NpcRangedAttackHelper.findBallisticShot(this, target, start, (double)projectileSpeed, 0.05)
               .orElseGet(() -> this.fallbackShot(target, start, projectileSpeed));
            arrow.shoot(
               shot.direction().x,
               shot.direction().y,
               shot.direction().z,
               shot.speed(),
               (float)(14 - this.level().getDifficulty().getId() * 4)
            );
            NpcRangedAttackHelper.configureBowArrow(this, arrow, velocity);
            this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            this.level().addFreshEntity(arrow);
         }
      }
   }

   private void performCrossbowAttack(LivingEntity target) {
      ItemStack crossbow = this.getMainHandItem();
      ItemStack projectileStack = this.getProjectile(crossbow);
      if (projectileStack.isEmpty()) {
         projectileStack = new ItemStack(Items.ARROW);
      }

      Vec3 aim = NpcRangedAttackHelper.predictTargetPosition(this, target, 3.15);
      double x = aim.x - this.getX();
      double y = aim.y - this.getEyeY();
      double z = aim.z - this.getZ();
      double horizontal = Math.sqrt(x * x + z * z);
      Vec3 direction = new Vec3(x, y + horizontal * 0.2, z).normalize();
      int projectileCount = NpcRangedAttackHelper.crossbowProjectileCount(crossbow);

      for (int shot = 0; shot < projectileCount; shot++) {
         double angle = projectileCount == 1 ? 0.0 : (double)(shot - 1) * Math.toRadians(10.0);
         double cos = Math.cos(angle);
         double sin = Math.sin(angle);
         Vec3 shotDirection = new Vec3(
            direction.x * cos - direction.z * sin, direction.y, direction.x * sin + direction.z * cos
         );
         AbstractArrow arrow = ProjectileUtil.getMobArrow(this, projectileStack, 1.0F);
         NpcRangedAttackHelper.configureCrossbowArrow(this, arrow, crossbow);
         arrow.shoot(shotDirection.x, shotDirection.y, shotDirection.z, 3.15F, (float)(8 - this.level().getDifficulty().getId() * 2));
         this.level().addFreshEntity(arrow);
      }

      this.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
   }

   private NpcRangedAttackHelper.BallisticShot fallbackShot(LivingEntity target, Vec3 start, float speed) {
      Vec3 aim = NpcRangedAttackHelper.predictTargetPosition(this, target, (double)speed);
      double horizontal = Math.sqrt(Math.pow(aim.x - start.x, 2.0) + Math.pow(aim.z - start.z, 2.0));
      Vec3 direction = aim.subtract(start).add(0.0, horizontal * 0.2, 0.0).normalize();
      return new NpcRangedAttackHelper.BallisticShot(direction, speed);
   }

   private boolean hasBallisticShot(LivingEntity target) {
      float speed = this.getMainHandItem().getItem() instanceof CrossbowItem ? 3.15F : this.calculateProjectileVelocity(target);
      return NpcRangedAttackHelper.findBallisticShot(this, target, this.getEyePosition(), (double)speed, 0.05).isPresent();
   }

   @Override
   protected boolean canAcquireFactionTarget(LivingEntity target) {
      return target != null && target.isAlive();
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      return false;
   }

   private float calculateProjectileVelocity(LivingEntity target) {
      return this.calculateProjectileVelocityFrom(this.position(), target);
   }

   private float calculateProjectileVelocityFrom(Vec3 origin, LivingEntity target) {
      double dx = target.getX() - origin.x;
      double dz = target.getZ() - origin.z;
      double distance = Math.sqrt(dx * dx + dz * dz);
      double height = target.getY(0.35) - (origin.y + (double)this.getEyeHeight());
      double gravity = 0.05;
      double angle = Math.toRadians(35.0);
      double cos = Math.cos(angle);
      double denominator = 2.0 * cos * cos * (distance * Math.tan(angle) - height);
      if (denominator <= 0.001) {
         return 3.0F;
      } else {
         double speed = Math.sqrt(gravity * distance * distance / denominator);
         return (float)Math.max(1.6, Math.min(4.0, speed));
      }
   }

   private static final class ArcherRangedAttackGoal extends Goal {
      private final FactionArcherNpcEntity archer;
      private int cooldown;
      private int drawTicks;
      private int retreatRepathTicks;
      private LivingEntity target;
      private Vec3 retreatPosition;
      private int ballisticCheckCooldown;
      private boolean ballisticPath;
      private Vec3 firingPosition;
      private int firingPositionRepathTicks;

      private ArcherRangedAttackGoal(FactionArcherNpcEntity archer) {
         this.archer = archer;
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         this.target = this.archer.getTarget();
         return this.validTarget(this.target);
      }

      public boolean canContinueToUse() {
         return this.validTarget(this.target) && this.archer.getTarget() == this.target;
      }

      public void start() {
         this.cooldown = 0;
         this.drawTicks = 0;
         this.retreatRepathTicks = 0;
         this.retreatPosition = null;
         this.ballisticCheckCooldown = 0;
         this.ballisticPath = false;
         this.firingPosition = null;
         this.firingPositionRepathTicks = this.archer.getRandom().nextInt(10);
      }

      public void tick() {
         this.target = this.archer.getTarget();
         if (!this.validTarget(this.target)) {
            this.stopUsingBow();
         } else {
            this.archer.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
            double distanceSqr = this.archer.distanceToSqr(this.target);
            if (this.cooldown > 0) {
               this.cooldown--;
            }

            if (this.archer.isUsingItem()) {
               if (this.shouldRetreat(distanceSqr)) {
                  this.continueRetreating();
               } else {
                  this.clearRetreat();
                  this.archer.getNavigation().stop();
               }

               this.drawTicks++;
               int requiredDrawTicks = this.archer.getMainHandItem().getItem() instanceof CrossbowItem
                  ? NpcRangedAttackHelper.crossbowChargeDuration(this.archer.getMainHandItem())
                  : 20;
               if (this.drawTicks >= requiredDrawTicks) {
                  this.archer.performRangedAttack(this.target, 1.0F);
                  this.stopUsingBow();
                  this.cooldown = NpcVolley.COOLDOWN_TICKS;
                  this.drawTicks = 0;
               }
            } else if (!this.canShootTarget()) {
               this.stopUsingBow();
               this.clearRetreat();
               if (distanceSqr <= 144.0) {
                  this.clearFiringPosition();
                  this.archer.getNavigation().moveTo(this.target, this.archer.playerSprintNavigationSpeed());
               } else {
                  this.moveToFiringPosition();
               }

               this.drawTicks = 0;
            } else if (this.cooldown > 0 && this.shouldRetreat(distanceSqr)) {
               this.stopUsingBow();
               this.continueRetreating();
               this.drawTicks = 0;
            } else {
               this.clearRetreat();
               this.clearFiringPosition();
               this.archer.getNavigation().stop();
               if (this.cooldown <= 0 && NpcVolley.canStartDraw(this.archer)) {
                  this.archer.startUsingItem(InteractionHand.MAIN_HAND);
                  this.drawTicks = 0;
               }
            }
         }
      }

      public void stop() {
         this.stopUsingBow();
         this.target = null;
         this.drawTicks = 0;
         this.clearRetreat();
         this.clearFiringPosition();
         this.archer.getNavigation().stop();
      }

      private boolean validTarget(LivingEntity candidate) {
         return candidate != null
            && candidate.isAlive()
            && (this.archer.getMainHandItem().getItem() instanceof BowItem || this.archer.getMainHandItem().getItem() instanceof CrossbowItem)
            && (!(candidate instanceof Player player) || !player.isCreative());
      }

      private boolean canShootTarget() {
         if (this.ballisticCheckCooldown-- <= 0) {
            this.ballisticPath = this.archer.hasBallisticShot(this.target);
            this.ballisticCheckCooldown = 5;
         }

         return this.ballisticPath;
      }

      private void stopUsingBow() {
         if (this.archer.isUsingItem()) {
            this.archer.stopUsingItem();
         }
      }

      private void continueRetreating() {
         if (this.retreatRepathTicks > 0) {
            this.retreatRepathTicks--;
         }

         boolean reachedPoint = this.retreatPosition != null && this.archer.position().distanceToSqr(this.retreatPosition) < 2.25;
         if (this.retreatPosition == null || this.retreatRepathTicks <= 0 || reachedPoint || this.archer.getNavigation().isDone()) {
            Vec3 candidate = DefaultRandomPos.getPosAway(this.archer, 10, 4, this.target.position());
            if (candidate == null) {
               Vec3 away = this.archer.position().subtract(this.target.position());
               if (away.horizontalDistanceSqr() < 1.0E-4) {
                  return;
               }

               candidate = this.archer.position().add(away.normalize().scale(7.0));
            }

            this.retreatPosition = candidate;
            this.retreatRepathTicks = 12;
            this.archer.getNavigation().moveTo(candidate.x, candidate.y, candidate.z, this.archer.playerSprintNavigationSpeed());
         }
      }

      private boolean shouldRetreat(double distanceSqr) {
         double distance = Math.sqrt(distanceSqr);
         return this.retreatPosition != null ? distance < 10.0 : distance < 6.0;
      }

      private void clearRetreat() {
         this.retreatPosition = null;
         this.retreatRepathTicks = 0;
      }

      private void moveToFiringPosition() {
         if (!(this.archer instanceof FactionMountedArcherNpcEntity)) {
            if (this.firingPositionRepathTicks > 0) {
               this.firingPositionRepathTicks--;
            }

            boolean reached = this.firingPosition != null && this.archer.position().distanceToSqr(this.firingPosition) < 2.25;
            boolean pathFinished = this.firingPosition != null && this.archer.getNavigation().isDone();
            if (this.firingPositionRepathTicks <= 0 || reached || pathFinished) {
               this.firingPosition = this.findFiringPosition();
               this.firingPositionRepathTicks = 40 + this.archer.getRandom().nextInt(11);
            }

            if (this.firingPosition != null) {
               this.archer
                  .getNavigation()
                  .moveTo(this.firingPosition.x, this.firingPosition.y, this.firingPosition.z, this.archer.playerWalkNavigationSpeed());
            } else {
               this.archer.getNavigation().stop();
            }
         }
      }

      private Vec3 findFiringPosition() {
         Vec3 origin = this.archer.position();
         Vec3 toTarget = this.target.position().subtract(origin);
         Vec3 horizontal = new Vec3(toTarget.x, 0.0, toTarget.z);
         if (horizontal.lengthSqr() < 0.001) {
            return null;
         } else {
            Vec3 forward = horizontal.normalize();
            Vec3 side = new Vec3(-forward.z, 0.0, forward.x);
            Vec3 best = null;
            double bestScore = Double.MAX_VALUE;

            for (int sample = 0; sample < 6; sample++) {
               double lateral = (this.archer.getRandom().nextDouble() * 2.0 - 1.0) * 12.0;
               double forwardOffset = (this.archer.getRandom().nextDouble() * 2.0 - 1.0) * 5.0;
               Vec3 guess = origin.add(side.scale(lateral)).add(forward.scale(forwardOffset));
               Vec3 candidate = DefaultRandomPos.getPosTowards(this.archer, 12, 5, guess, Math.PI / 2);
               if (candidate != null && !(candidate.distanceToSqr(this.target.position()) < 100.0)) {
                  float speed = this.archer.getMainHandItem().getItem() instanceof CrossbowItem
                     ? 3.15F
                     : this.archer.calculateProjectileVelocityFrom(candidate, this.target);
                  Vec3 shotStart = candidate.add(0.0, (double)this.archer.getEyeHeight(), 0.0);
                  if (!NpcRangedAttackHelper.findBallisticShot(this.archer, this.target, shotStart, (double)speed, 0.05).isEmpty()) {
                     Path path = this.archer.getNavigation().createPath(BlockPos.containing(candidate), 1);
                     if (path != null && path.canReach()) {
                        double score = candidate.distanceToSqr(origin) + Math.abs(lateral) * 0.5;
                        if (score < bestScore) {
                           best = candidate;
                           bestScore = score;
                        }
                     }
                  }
               }
            }

            return best;
         }
      }

      private void clearFiringPosition() {
         this.firingPosition = null;
         this.firingPositionRepathTicks = 0;
      }
   }
}
