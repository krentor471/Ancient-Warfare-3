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
      this.m_21008_(InteractionHand.MAIN_HAND, new ItemStack(Items.f_42411_));
      this.applyFactionDefaults();
      this.ensureArcherFollowRange(FactionNpcRole.ARCHER);
   }

   protected FactionArcherNpcEntity(EntityType<? extends FactionArcherNpcEntity> entityType, Level level, FactionNpcRole role) {
      super(entityType, level, role);
      this.m_21008_(InteractionHand.MAIN_HAND, new ItemStack(Items.f_42411_));
      this.applyFactionDefaults();
      this.ensureArcherFollowRange(role);
   }

   @Override
   protected boolean supportsCombatPromotion() {
      return !this.getNpcSubtype().endsWith(".elite");
   }

   public static Builder createAttributes() {
      return m_21552_().m_22268_(Attributes.f_22276_, 20.0).m_22268_(Attributes.f_22279_, 0.27).m_22268_(Attributes.f_22277_, 32.0);
   }

   private void ensureArcherFollowRange(FactionNpcRole role) {
      AttributeInstance followRange = this.m_21051_(Attributes.f_22277_);
      if (followRange != null) {
         double minimum = role == FactionNpcRole.MOUNTED_ARCHER ? 64.0 : 72.0;
         followRange.m_22100_(Math.max(followRange.m_22115_(), minimum));
      }
   }

   protected void m_8099_() {
      this.addSharedGoals();
      this.f_21345_.m_25352_(2, new FactionArcherNpcEntity.ArcherRangedAttackGoal(this));
      this.addFactionCombatTargets(false);
   }

   @Override
   public void m_8107_() {
      this.ensureArcherFollowRange(this.getNpcSubtype().equals(FactionNpcRole.MOUNTED_ARCHER.id()) ? FactionNpcRole.MOUNTED_ARCHER : FactionNpcRole.ARCHER);
      super.m_8107_();
   }

   public void m_6504_(LivingEntity target, float velocity) {
      if (target != null && target.m_6084_()) {
         this.recordCombatAttack();
         if (this.m_21205_().m_41720_() instanceof CrossbowItem) {
            this.performCrossbowAttack(target);
         } else {
            ItemStack projectileStack = this.m_6298_(this.m_21205_());
            AbstractArrow arrow = ProjectileUtil.m_37300_(this, projectileStack, velocity);
            float projectileSpeed = this.calculateProjectileVelocity(target);
            Vec3 start = new Vec3(this.m_20185_(), arrow.m_20186_(), this.m_20189_());
            NpcRangedAttackHelper.BallisticShot shot = NpcRangedAttackHelper.findBallisticShot(this, target, start, (double)projectileSpeed, 0.05)
               .orElseGet(() -> this.fallbackShot(target, start, projectileSpeed));
            arrow.m_6686_(
               shot.direction().f_82479_,
               shot.direction().f_82480_,
               shot.direction().f_82481_,
               shot.speed(),
               (float)(14 - this.m_9236_().m_46791_().m_19028_() * 4)
            );
            NpcRangedAttackHelper.configureBowArrow(this, arrow, velocity);
            this.m_5496_(SoundEvents.f_12382_, 1.0F, 1.0F / (this.m_217043_().m_188501_() * 0.4F + 0.8F));
            this.m_9236_().m_7967_(arrow);
         }
      }
   }

   private void performCrossbowAttack(LivingEntity target) {
      ItemStack crossbow = this.m_21205_();
      ItemStack projectileStack = this.m_6298_(crossbow);
      if (projectileStack.m_41619_()) {
         projectileStack = new ItemStack(Items.f_42412_);
      }

      Vec3 aim = NpcRangedAttackHelper.predictTargetPosition(this, target, 3.15);
      double x = aim.f_82479_ - this.m_20185_();
      double y = aim.f_82480_ - this.m_20188_();
      double z = aim.f_82481_ - this.m_20189_();
      double horizontal = Math.sqrt(x * x + z * z);
      Vec3 direction = new Vec3(x, y + horizontal * 0.2, z).m_82541_();
      int projectileCount = NpcRangedAttackHelper.crossbowProjectileCount(crossbow);

      for (int shot = 0; shot < projectileCount; shot++) {
         double angle = projectileCount == 1 ? 0.0 : (double)(shot - 1) * Math.toRadians(10.0);
         double cos = Math.cos(angle);
         double sin = Math.sin(angle);
         Vec3 shotDirection = new Vec3(
            direction.f_82479_ * cos - direction.f_82481_ * sin, direction.f_82480_, direction.f_82479_ * sin + direction.f_82481_ * cos
         );
         AbstractArrow arrow = ProjectileUtil.m_37300_(this, projectileStack, 1.0F);
         NpcRangedAttackHelper.configureCrossbowArrow(this, arrow, crossbow);
         arrow.m_6686_(shotDirection.f_82479_, shotDirection.f_82480_, shotDirection.f_82481_, 3.15F, (float)(8 - this.m_9236_().m_46791_().m_19028_() * 2));
         this.m_9236_().m_7967_(arrow);
      }

      this.m_5496_(SoundEvents.f_11847_, 1.0F, 1.0F / (this.m_217043_().m_188501_() * 0.4F + 0.8F));
   }

   private NpcRangedAttackHelper.BallisticShot fallbackShot(LivingEntity target, Vec3 start, float speed) {
      Vec3 aim = NpcRangedAttackHelper.predictTargetPosition(this, target, (double)speed);
      double horizontal = Math.sqrt(Math.pow(aim.f_82479_ - start.f_82479_, 2.0) + Math.pow(aim.f_82481_ - start.f_82481_, 2.0));
      Vec3 direction = aim.m_82546_(start).m_82520_(0.0, horizontal * 0.2, 0.0).m_82541_();
      return new NpcRangedAttackHelper.BallisticShot(direction, speed);
   }

   private boolean hasBallisticShot(LivingEntity target) {
      float speed = this.m_21205_().m_41720_() instanceof CrossbowItem ? 3.15F : this.calculateProjectileVelocity(target);
      return NpcRangedAttackHelper.findBallisticShot(this, target, this.m_146892_(), (double)speed, 0.05).isPresent();
   }

   @Override
   protected boolean canAcquireFactionTarget(LivingEntity target) {
      return target != null && target.m_6084_();
   }

   @Override
   public boolean m_7327_(Entity target) {
      return false;
   }

   private float calculateProjectileVelocity(LivingEntity target) {
      return this.calculateProjectileVelocityFrom(this.m_20182_(), target);
   }

   private float calculateProjectileVelocityFrom(Vec3 origin, LivingEntity target) {
      double dx = target.m_20185_() - origin.f_82479_;
      double dz = target.m_20189_() - origin.f_82481_;
      double distance = Math.sqrt(dx * dx + dz * dz);
      double height = target.m_20227_(0.35) - (origin.f_82480_ + (double)this.m_20192_());
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
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         this.target = this.archer.m_5448_();
         return this.validTarget(this.target);
      }

      public boolean m_8045_() {
         return this.validTarget(this.target) && this.archer.m_5448_() == this.target;
      }

      public void m_8056_() {
         this.cooldown = 0;
         this.drawTicks = 0;
         this.retreatRepathTicks = 0;
         this.retreatPosition = null;
         this.ballisticCheckCooldown = 0;
         this.ballisticPath = false;
         this.firingPosition = null;
         this.firingPositionRepathTicks = this.archer.m_217043_().m_188503_(10);
      }

      public void m_8037_() {
         this.target = this.archer.m_5448_();
         if (!this.validTarget(this.target)) {
            this.stopUsingBow();
         } else {
            this.archer.m_21563_().m_24960_(this.target, 30.0F, 30.0F);
            double distanceSqr = this.archer.m_20280_(this.target);
            if (this.cooldown > 0) {
               this.cooldown--;
            }

            if (this.archer.m_6117_()) {
               if (this.shouldRetreat(distanceSqr)) {
                  this.continueRetreating();
               } else {
                  this.clearRetreat();
                  this.archer.m_21573_().m_26573_();
               }

               this.drawTicks++;
               int requiredDrawTicks = this.archer.m_21205_().m_41720_() instanceof CrossbowItem
                  ? NpcRangedAttackHelper.crossbowChargeDuration(this.archer.m_21205_())
                  : 20;
               if (this.drawTicks >= requiredDrawTicks) {
                  this.archer.m_6504_(this.target, 1.0F);
                  this.stopUsingBow();
                  this.cooldown = 30;
                  this.drawTicks = 0;
               }
            } else if (!this.canShootTarget()) {
               this.stopUsingBow();
               this.clearRetreat();
               if (distanceSqr <= 144.0) {
                  this.clearFiringPosition();
                  this.archer.m_21573_().m_5624_(this.target, this.archer.playerSprintNavigationSpeed());
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
               this.archer.m_21573_().m_26573_();
               if (this.cooldown <= 0) {
                  this.archer.m_6672_(InteractionHand.MAIN_HAND);
                  this.drawTicks = 0;
               }
            }
         }
      }

      public void m_8041_() {
         this.stopUsingBow();
         this.target = null;
         this.drawTicks = 0;
         this.clearRetreat();
         this.clearFiringPosition();
         this.archer.m_21573_().m_26573_();
      }

      private boolean validTarget(LivingEntity candidate) {
         return candidate != null
            && candidate.m_6084_()
            && (this.archer.m_21205_().m_41720_() instanceof BowItem || this.archer.m_21205_().m_41720_() instanceof CrossbowItem)
            && (!(candidate instanceof Player player) || !player.m_7500_());
      }

      private boolean canShootTarget() {
         if (this.ballisticCheckCooldown-- <= 0) {
            this.ballisticPath = this.archer.hasBallisticShot(this.target);
            this.ballisticCheckCooldown = 5;
         }

         return this.ballisticPath;
      }

      private void stopUsingBow() {
         if (this.archer.m_6117_()) {
            this.archer.m_5810_();
         }
      }

      private void continueRetreating() {
         if (this.retreatRepathTicks > 0) {
            this.retreatRepathTicks--;
         }

         boolean reachedPoint = this.retreatPosition != null && this.archer.m_20182_().m_82557_(this.retreatPosition) < 2.25;
         if (this.retreatPosition == null || this.retreatRepathTicks <= 0 || reachedPoint || this.archer.m_21573_().m_26571_()) {
            Vec3 candidate = DefaultRandomPos.m_148407_(this.archer, 10, 4, this.target.m_20182_());
            if (candidate == null) {
               Vec3 away = this.archer.m_20182_().m_82546_(this.target.m_20182_());
               if (away.m_165925_() < 1.0E-4) {
                  return;
               }

               candidate = this.archer.m_20182_().m_82549_(away.m_82541_().m_82490_(7.0));
            }

            this.retreatPosition = candidate;
            this.retreatRepathTicks = 12;
            this.archer.m_21573_().m_26519_(candidate.f_82479_, candidate.f_82480_, candidate.f_82481_, this.archer.playerSprintNavigationSpeed());
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

            boolean reached = this.firingPosition != null && this.archer.m_20182_().m_82557_(this.firingPosition) < 2.25;
            boolean pathFinished = this.firingPosition != null && this.archer.m_21573_().m_26571_();
            if (this.firingPositionRepathTicks <= 0 || reached || pathFinished) {
               this.firingPosition = this.findFiringPosition();
               this.firingPositionRepathTicks = 40 + this.archer.m_217043_().m_188503_(11);
            }

            if (this.firingPosition != null) {
               this.archer
                  .m_21573_()
                  .m_26519_(this.firingPosition.f_82479_, this.firingPosition.f_82480_, this.firingPosition.f_82481_, this.archer.playerWalkNavigationSpeed());
            } else {
               this.archer.m_21573_().m_26573_();
            }
         }
      }

      private Vec3 findFiringPosition() {
         Vec3 origin = this.archer.m_20182_();
         Vec3 toTarget = this.target.m_20182_().m_82546_(origin);
         Vec3 horizontal = new Vec3(toTarget.f_82479_, 0.0, toTarget.f_82481_);
         if (horizontal.m_82556_() < 0.001) {
            return null;
         } else {
            Vec3 forward = horizontal.m_82541_();
            Vec3 side = new Vec3(-forward.f_82481_, 0.0, forward.f_82479_);
            Vec3 best = null;
            double bestScore = Double.MAX_VALUE;

            for (int sample = 0; sample < 6; sample++) {
               double lateral = (this.archer.m_217043_().m_188500_() * 2.0 - 1.0) * 12.0;
               double forwardOffset = (this.archer.m_217043_().m_188500_() * 2.0 - 1.0) * 5.0;
               Vec3 guess = origin.m_82549_(side.m_82490_(lateral)).m_82549_(forward.m_82490_(forwardOffset));
               Vec3 candidate = DefaultRandomPos.m_148412_(this.archer, 12, 5, guess, Math.PI / 2);
               if (candidate != null && !(candidate.m_82557_(this.target.m_20182_()) < 100.0)) {
                  float speed = this.archer.m_21205_().m_41720_() instanceof CrossbowItem
                     ? 3.15F
                     : this.archer.calculateProjectileVelocityFrom(candidate, this.target);
                  Vec3 shotStart = candidate.m_82520_(0.0, (double)this.archer.m_20192_(), 0.0);
                  if (!NpcRangedAttackHelper.findBallisticShot(this.archer, this.target, shotStart, (double)speed, 0.05).isEmpty()) {
                     Path path = this.archer.m_21573_().m_7864_(BlockPos.m_274446_(candidate), 1);
                     if (path != null && path.m_77403_()) {
                        double score = candidate.m_82557_(origin) + Math.abs(lateral) * 0.5;
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
