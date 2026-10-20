package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcDefaultsRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.common.npc.NpcEquipmentResolver;
import net.shadowmage.ancientwarfare.core.common.npc.NpcFactionAccess;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargeting;

public abstract class FactionNpcEntity extends NpcBaseEntity implements NpcFactionAccess {
   private static final EntityDataAccessor<String> FACTION_NAME = SynchedEntityData.defineId(FactionNpcEntity.class, EntityDataSerializers.STRING);
   private static final String TAG_FACTION_NAME = "FactionName";
   private static final double COMBAT_VERTICAL_TARGET_RANGE = 96.0;
   private static final int TARGET_SCAN_INTERVAL = 4;
   private static final int PLAYER_TIER = 1;
   private static final int FACTION_TIER = 2;
   private static final int MONSTER_TIER = 3;

   protected FactionNpcEntity(EntityType<? extends FactionNpcEntity> entityType, Level level, FactionNpcRole role) {
      super(entityType, level);
      this.setFactionName("brigand");
      this.setNpcSubtype(role.id());
      this.applyFactionDefaults();
   }

   @Override
   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(FACTION_NAME, "brigand");
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putString("FactionName", this.getFactionName());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.contains("FactionName")) {
         this.setFactionName(tag.getString("FactionName"));
      }

      this.applyFactionDefaults();
   }

   @Override
   public String getFactionName() {
      return (String)this.entityData.get(FACTION_NAME);
   }

   public void setFactionName(String factionName) {
      String normalized = factionName != null && !factionName.isBlank() ? factionName.trim() : "brigand";
      this.entityData.set(FACTION_NAME, normalized);
      this.setNpcType(normalized);
      this.applyFactionDefaults();
      LivingEntity currentTarget = this.getTarget();
      if (currentTarget != null && this.isAlliedTo(currentTarget)) {
         this.setTarget(null);
      }
   }

   @Override
   public String getNpcType() {
      return this.getFactionName();
   }

   @Override
   public Component getTypeName() {
      return this.applyCombatRankName(
         Component.translatable(
            "entity.ancientwarfare.faction.named",
            new Object[]{this.getFactionName(), Component.translatable("entity.ancientwarfare.faction." + this.getNpcSubtype())}
         )
      );
   }

   protected void addSharedGoals() {
      this.addSharedGoals(true);
   }

   protected void addSharedGoals(boolean allowRandomStroll) {
      if (!(this instanceof FactionSoldierNpcEntity)) {
         this.goalSelector.addGoal(0, new FloatGoal(this));
      }

      if (allowRandomStroll) {
         this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
      }

      this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
   }

   public boolean isHostileTowards(Entity entity) {
      if (!NpcTargeting.canHarm(this, entity)) {
         return false;
      } else if (entity instanceof FactionNpcEntity factionNpc) {
         return this.isAlliedTo(factionNpc) ? false : FactionRegistry.isHostile(this.getFactionName(), factionNpc.getFactionName());
      } else if (!(entity instanceof Player player)) {
         return entity instanceof PlayerOwnedNpcEntity ownedNpc ? ownedNpc.getOwner().map(owner -> {
            if (FactionRegistry.get(this.getFactionName()).defaultStanding() < 0) {
               return true;
            } else {
               if (this.level() instanceof ServerLevel serverLevelx && FactionStandingData.get(serverLevelx).isHostile(owner, this.getFactionName())) {
                  return true;
               }

               return false;
            }
         }).orElse(false) : false;
      } else if (player.isCreative()) {
         return false;
      } else if (FactionRegistry.get(this.getFactionName()).defaultStanding() < 0) {
         return true;
      } else {
         if (this.level() instanceof ServerLevel serverLevel && FactionStandingData.get(serverLevel).isHostile(player.getUUID(), this.getFactionName())) {
            return true;
         }

         return false;
      }
   }

   public boolean isAlliedTo(Entity entity) {
      if (entity instanceof FactionNpcEntity factionNpc && FactionRegistry.isSameFaction(this.getFactionName(), factionNpc.getFactionName())) {
         return true;
      }

      return super.isAlliedTo(entity);
   }

   @Override
   public boolean canAttack(LivingEntity target) {
      return NpcTargeting.canHarm(this, target) && super.canAttack(target);
   }

   @Override
   public void setTarget(LivingEntity target) {
      if (target != null && !NpcTargeting.canHarm(this, target)) {
         super.setTarget(null);
      } else {
         LivingEntity current = this.getTarget();
         if (target != null && current != null && current != target && this.isValidLockedTarget(current)) {
            int currentTier = this.targetPriorityTier(current);
            int incomingTier = this.targetPriorityTier(target);
            if (incomingTier >= currentTier) {
               return;
            }
         }

         super.setTarget(target);
      }
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      if (!NpcTargeting.canHarm(this, target)) {
         if (target == this.getTarget()) {
            this.setTarget(null);
         }

         return false;
      } else {
         return super.doHurtTarget(target);
      }
   }

   protected void addFactionCombatTargets() {
      this.addFactionCombatTargets(true);
   }

   protected void addFactionCombatTargets(boolean mustSee) {
      this.targetSelector.addGoal(1, new FactionNpcEntity.FactionHurtByTargetGoal(this));
      this.targetSelector
         .addGoal(
            2,
            new VerticalRangeTargetGoal<LivingEntity>(
               this, LivingEntity.class, 4, false, false, target -> this.isPlayerTierTarget(target) && this.canAcquireFactionTarget(target), 96.0
            )
         );
      this.targetSelector
         .addGoal(
            4,
            new VerticalRangeTargetGoal(
               this, FactionNpcEntity.class, 4, mustSee, false, target -> this.isFactionTierTarget(target) && this.canAcquireFactionTarget(target), 96.0
            )
         );
      this.targetSelector
         .addGoal(
            5,
            new VerticalRangeTargetGoal<LivingEntity>(
               this, LivingEntity.class, 4, mustSee, false, target -> this.isMonsterTierTarget(target) && this.canAcquireFactionTarget(target), 96.0
            )
         );
   }

   protected final boolean isPlayerTierTarget(LivingEntity target) {
      return target != null && target.isAlive() && (target instanceof Player || target instanceof PlayerOwnedNpcEntity) && this.isHostileTowards(target);
   }

   protected final boolean isFactionTierTarget(LivingEntity target) {
      if (target instanceof FactionNpcEntity factionNpc && target.isAlive() && this.isHostileTowards(target)) {
         return true;
      }

      return false;
   }

   protected final boolean isMonsterTierTarget(LivingEntity target) {
      return NpcTargeting.isAutomaticHostile(this, target);
   }

   protected final int targetPriorityTier(LivingEntity target) {
      if (target instanceof Player || target instanceof PlayerOwnedNpcEntity) {
         return 1;
      } else if (target instanceof FactionNpcEntity) {
         return 2;
      } else if (NpcTargeting.isAutomaticHostile(this, target)) {
         return 3;
      } else {
         return target == this.getLastHurtByMob() && NpcTargeting.canHarm(this, target) ? 3 : Integer.MAX_VALUE;
      }
   }

   private boolean isValidLockedTarget(LivingEntity target) {
      return NpcTargeting.canHarm(this, target) && this.targetPriorityTier(target) != Integer.MAX_VALUE;
   }

   protected boolean canAcquireFactionTarget(LivingEntity target) {
      return true;
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      LivingEntity attacker = source.getEntity() instanceof LivingEntity livingx ? livingx : (source.getDirectEntity() instanceof LivingEntity living ? living : null);
      if (attacker == null || !NpcTargeting.isProtectedAlly(this, attacker) && !this.isAlliedTo(attacker) && !attacker.isAlliedTo(this)) {
         boolean damaged = super.hurt(source, amount);
         if (!this.level().isClientSide && NpcTargeting.canHarm(this, attacker)) {
            this.setLastHurtByMob(attacker);
            this.setTarget(attacker);
            this.alertFactionCombatants(attacker);
         }

         return damaged;
      } else {
         return false;
      }
   }

   private void alertFactionCombatants(LivingEntity attacker) {
      this.level()
         .getEntitiesOfClass(
            FactionNpcEntity.class,
            this.getBoundingBox().inflate(64.0, 16.0, 64.0),
            ally -> ally != this
                  && ally.isAlive()
                  && ally.getFactionName().equalsIgnoreCase(this.getFactionName())
                  && isFactionCombatRole(ally)
                  && NpcTargeting.canHarm(ally, attacker)
         )
         .forEach(ally -> {
            ally.setLastHurtByMob(attacker);
            ally.setTarget(attacker);
         });
   }

   protected static boolean isFactionCombatRole(FactionNpcEntity npc) {
      return FactionNpcRole.byId(npc.getNpcSubtype()).map(role -> {
         return switch (role) {
            case SOLDIER, ARCHER, CAVALRY, MOUNTED_ARCHER, LEADER, SIEGE_ENGINEER, SPELLCASTER, SOLDIER_ELITE, ARCHER_ELITE, LEADER_ELITE -> true;
            default -> false;
         };
      }).orElse(false);
   }

   public int getExperienceReward() {
      return this.lastHurtByPlayer != null && this.isHostileTowards(this.lastHurtByPlayer) ? FactionNpcRole.byId(this.getNpcSubtype()).map(role -> {
         return switch (role) {
            case SOLDIER, ARCHER, CAVALRY, MOUNTED_ARCHER, LEADER, SIEGE_ENGINEER, SPELLCASTER -> 20;
            case SOLDIER_ELITE, ARCHER_ELITE, LEADER_ELITE -> 40;
            default -> 0;
         };
      }).orElse(0) : 0;
   }

   @Override
   public boolean removeWhenFarAway(double distanceToClosestPlayer) {
      return false;
   }

   public void aiStep() {
      super.aiStep();
      this.updateMountedMovement();
   }

   public double getMyRidingOffset() {
      return this.isMountedFactionRole() ? super.getMyRidingOffset() - 0.45 : super.getMyRidingOffset();
   }

   protected void updateMountedMovement() {
      if (!(this.getVehicle() instanceof Mob mount) || !this.isMountedFactionRole()) {
         return;
      }

      if (!NpcWaterEscape.isRecoveryActive(this)) {
         if (mount.isNoAi()) {
            mount.setNoAi(false);
         }

         mount.setTarget(null);
         mount.setAggressive(false);
         if (!this.usesCustomMountedMovement()) {
            LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
               double distanceSqr = mount.distanceToSqr(target);
               double stopDistance = FactionNpcRole.MOUNTED_ARCHER.id().equals(this.getNpcSubtype()) ? 18.0 : NpcEquipmentResolver.meleeHoldDistance(this);
               if (distanceSqr > stopDistance * stopDistance) {
                  this.driveMountToward(mount, target);
               } else {
                  mount.getNavigation().stop();
                  mount.setDeltaMovement(mount.getDeltaMovement().multiply(0.55, 1.0, 0.55));
                  this.faceMountedTarget(mount, target);
               }
            } else {
               mount.getNavigation().stop();
               mount.setDeltaMovement(mount.getDeltaMovement().multiply(0.4, 1.0, 0.4));
            }
         }
      }
   }

   protected boolean usesCustomMountedMovement() {
      return false;
   }

   private void driveMountToward(Mob mount, LivingEntity target) {
      Vec3 delta = target.position().subtract(mount.position());
      Vec3 horizontal = new Vec3(delta.x, 0.0, delta.z);
      if (!(horizontal.lengthSqr() < 1.0E-4)) {
         double speed = 0.22;
         AttributeInstance speedAttribute = mount.getAttribute(Attributes.MOVEMENT_SPEED);
         if (speedAttribute != null) {
            speed = Math.max(speed, speedAttribute.getValue() * 0.65);
         }

         Vec3 direction = horizontal.normalize();
         if (!NpcMovementSafety.canMoveMountedSafely(mount, direction)) {
            mount.getNavigation().stop();
            mount.setDeltaMovement(mount.getDeltaMovement().multiply(0.4, 1.0, 0.4));
         } else {
            Vec3 current = mount.getDeltaMovement();
            mount.setDeltaMovement(direction.x * speed, current.y, direction.z * speed);
            mount.getNavigation().stop();
            this.faceDirection(mount, direction);
         }
      }
   }

   private void faceMountedTarget(Mob mount, LivingEntity target) {
      Vec3 delta = target.position().subtract(mount.position());
      Vec3 horizontal = new Vec3(delta.x, 0.0, delta.z);
      if (horizontal.lengthSqr() >= 1.0E-4) {
         this.faceDirection(mount, horizontal.normalize());
      }
   }

   private void faceDirection(Mob mount, Vec3 direction) {
      float yaw = (float)(Math.atan2(direction.z, direction.x) * 180.0F / (float)Math.PI) - 90.0F;
      mount.setYRot(yaw);
      mount.setYBodyRot(mount.getYRot());
      mount.setYHeadRot(mount.getYRot());
      this.setYRot(yaw);
      this.setYBodyRot(yaw);
      this.setYHeadRot(yaw);
   }

   private boolean isMountedFactionRole() {
      return FactionNpcRole.CAVALRY.id().equals(this.getNpcSubtype()) || FactionNpcRole.MOUNTED_ARCHER.id().equals(this.getNpcSubtype());
   }

   protected void swingWeapon() {
      this.swing(InteractionHand.MAIN_HAND);
   }

   protected void applyFactionDefaults() {
      FactionNpcDefaultsRegistry.apply(this, this.getFactionName(), FactionNpcRole.byId(this.getNpcSubtype()).orElse(FactionNpcRole.SOLDIER));
   }

   protected static final class FactionHurtByTargetGoal extends HurtByTargetGoal {
      private static final double FACTION_ALERT_RANGE = 64.0;
      private final FactionNpcEntity npc;

      protected FactionHurtByTargetGoal(FactionNpcEntity npc) {
         super(npc, new Class[0]);
         this.npc = npc;
      }

      public boolean canUse() {
         LivingEntity attacker = this.npc.getLastHurtByMob();
         return NpcTargeting.canHarm(this.npc, attacker) && super.canUse();
      }

      public void start() {
         super.start();
         LivingEntity attacker = this.npc.getLastHurtByMob();
         if (NpcTargeting.canHarm(this.npc, attacker)) {
            this.npc.alertFactionCombatants(attacker);
         }
      }
   }
}
