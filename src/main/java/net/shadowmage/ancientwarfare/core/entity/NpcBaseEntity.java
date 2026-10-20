package net.shadowmage.ancientwarfare.core.entity;

import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCombatRoles;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.npc.NpcFactionAccess;
import net.shadowmage.ancientwarfare.core.common.npc.NpcOwnerAccess;
import net.shadowmage.ancientwarfare.core.common.npc.NpcShielding;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargeting;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public abstract class NpcBaseEntity extends PathfinderMob {
   public static final double PLAYER_WALK_SPEED = 0.1;
   public static final double PLAYER_SPRINT_SPEED = 0.13;
   private double movementSpeedBeforeShield = -1.0;
   private static final EntityDataAccessor<String> NPC_TYPE = SynchedEntityData.defineId(NpcBaseEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<String> NPC_SUBTYPE = SynchedEntityData.defineId(NpcBaseEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<String> SKIN_NAME = SynchedEntityData.defineId(NpcBaseEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<Boolean> ALEX_MODEL = SynchedEntityData.defineId(NpcBaseEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> SHIELDING = SynchedEntityData.defineId(NpcBaseEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> MELEE_ATTACK_ANIMATION = SynchedEntityData.defineId(NpcBaseEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> STATUS_ICONS = SynchedEntityData.defineId(NpcBaseEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> COMBAT_ATTACK_COUNT = SynchedEntityData.defineId(NpcBaseEntity.class, EntityDataSerializers.INT);
   public static final int STATUS_ATTACK = 1;
   public static final int STATUS_UPKEEP = 2;
   public static final int STATUS_UPKEEP_2 = 4;
   public static final int STATUS_HOME = 8;
   public static final int STATUS_WORK = 16;
   public static final int STATUS_PATROL = 32;
   public static final int STATUS_GUARD = 64;
   public static final int STATUS_FOLLOW = 128;
   public static final int STATUS_WANDER = 256;
   public static final int STATUS_MOVE = 512;
   public static final int STATUS_ALARM = 1024;
   public static final int STATUS_FLEE = 2048;
   public static final int STATUS_SLEEP = 4096;
   public static final int STATUS_RAIN = 8192;
   private static final String DEFAULT_NPC_TYPE = "worker";
   private static final String DEFAULT_NPC_SUBTYPE = "miner";
   private static final String TAG_NPC_TYPE = "NpcType";
   private static final String TAG_NPC_SUBTYPE = "NpcSubtype";
   private static final String TAG_SKIN_NAME = "SkinName";
   private static final String TAG_ALEX_MODEL = "AlexModel";
   private static final String TAG_COMBAT_ATTACK_COUNT = "CombatAttackCount";
   private static final UUID COMBAT_RANK_HEALTH_MODIFIER = UUID.fromString("f3a7c5e1-0b5c-4c2c-9e7c-7a1f1d7d6a01");
   private static final UUID COMBAT_RANK_DAMAGE_MODIFIER = UUID.fromString("b4d8e2a6-6e2a-4e91-a5f2-8c6e9b9d2c02");

   private static final float MAX_MORALE = 100.0F;
   private static final float ROUT_THRESHOLD = 20.0F;
   private static final float RALLY_MORALE = 50.0F;
   private static final int ROUT_TICKS = 160;
   private static final float MORALE_PER_DAMAGE = 1.5F;
   private static final float FLANK_MORALE_FACTOR = 2.0F;
   private static final double FLANK_DOT = -0.3;
   private static final float ALLY_DEATH_MORALE = 10.0F;
   private static final double MORALE_RADIUS = 12.0;
   private static final float LEADER_MORALE_RESISTANCE = 0.5F;
   private static final float IDLE_MORALE_REGEN = 0.05F;
   private static final float COMBAT_MORALE_REGEN = 0.01F;
   private static final float LEADER_REGEN_FACTOR = 3.0F;
   private float morale = MAX_MORALE;
   private int routTicks;
   private Vec3 routThreat;
   private boolean leaderNearby;

   protected NpcBaseEntity(EntityType<? extends NpcBaseEntity> entityType, Level level) {
      super(entityType, level);
      this.setPersistenceRequired();
      this.getNavigation().getNodeEvaluator().setCanPassDoors(true);
      this.getNavigation().getNodeEvaluator().setCanOpenDoors(true);
      this.goalSelector.addGoal(1, new NpcOpenDoorGoal(this));
      this.goalSelector.addGoal(0, new NpcRoutGoal(this));
   }

   @Override
   protected PathNavigation createNavigation(Level level) {
      return new NpcGroundNavigation(this, level);
   }

   public boolean removeWhenFarAway(double distanceToClosestPlayer) {
      return false;
   }

   public int getMaxFallDistance() {
      return 1;
   }

   public void move(MoverType moverType, Vec3 movement) {
      if (moverType == MoverType.SELF && !NpcMovementSafety.canMoveSafely(this, movement)) {
         this.getNavigation().stop();
         movement = new Vec3(0.0, movement.y, 0.0);
      }

      super.move(moverType, movement);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(NPC_TYPE, "worker");
      this.entityData.define(NPC_SUBTYPE, "miner");
      this.entityData.define(SKIN_NAME, "");
      this.entityData.define(ALEX_MODEL, false);
      this.entityData.define(SHIELDING, false);
      this.entityData.define(MELEE_ATTACK_ANIMATION, 0);
      this.entityData.define(STATUS_ICONS, 0);
      this.entityData.define(COMBAT_ATTACK_COUNT, 0);
   }

   public void tick() {
      this.updateShieldMovementSpeed();
      if (!this.level().isClientSide) {
         if (this.getVehicle() instanceof Mob mount) {
            mount.setPersistenceRequired();
         }

         NpcSuffocationEscape.tryEscape(this);
         this.tickMorale();
         int meleeAnimation = this.getMeleeAttackAnimationTicks();
         if (meleeAnimation > 0) {
            this.entityData.set(MELEE_ATTACK_ANIMATION, meleeAnimation - 1);
         }
      }

      super.tick();
      if (!this.level().isClientSide) {
         NpcWaterEscape.tick(this);
         int flags = this.calculateStatusIconFlags();
         if (flags != this.getStatusIconFlags()) {
            this.entityData.set(STATUS_ICONS, flags);
         }
      }
   }

   private void updateShieldMovementSpeed() {
      if (!this.level().isClientSide) {
         AttributeInstance movement = this.getAttribute(Attributes.MOVEMENT_SPEED);
         if (movement != null) {
            if (this.isShielding() && this.movementSpeedBeforeShield < 0.0) {
               this.movementSpeedBeforeShield = movement.getBaseValue();
               movement.setBaseValue(this.movementSpeedBeforeShield * 0.5);
            } else if (!this.isShielding() && this.movementSpeedBeforeShield >= 0.0) {
               movement.setBaseValue(this.movementSpeedBeforeShield);
               this.movementSpeedBeforeShield = -1.0;
            }
         }
      }
   }

   public final double playerWalkNavigationSpeed() {
      return 1.0;
   }

   public final double playerSprintNavigationSpeed() {
      return 1.3;
   }

   public int getStatusIconFlags() {
      return (Integer)this.entityData.get(STATUS_ICONS);
   }

   protected int calculateStatusIconFlags() {
      int flags = 0;
      if (this.getTarget() != null) {
         flags |= 1;
      }

      if (this.isSleeping()) {
         flags |= 4096;
      }

      if (!(this instanceof PlayerOwnedNpcEntity owned)) {
         return flags;
      } else {
         NpcCommandType command = owned.getCurrentCommand();
         if (command == NpcCommandType.ATTACK) {
            flags |= 1;
         }

         if (command == NpcCommandType.GUARD) {
            flags |= 64;
         }

         if (command == NpcCommandType.FOLLOW) {
            flags |= 128;
         }

         if (command == NpcCommandType.MOVE) {
            flags |= 512;
         }

         if (owned.isGettingFood()) {
            flags |= 2;
         }

         if (owned.getFoodRemaining() <= 0 && !owned.getUpkeepStack().isEmpty()) {
            flags |= 4;
         }

         if (owned instanceof WorkerNpcEntity worker) {
            String state = worker.getWorkState();
            if ("working".equals(state) || "moving_to_work".equals(state)) {
               flags |= 16;
            }

            if ("sleeping".equals(state) || "sleeping_no_bed".equals(state)) {
               flags |= 4096;
               if (owned.hasHome() && !worker.isSleeping()) {
                  flags |= 8;
               }
            }
         }

         if (owned instanceof CombatNpcEntity combat
            && command == NpcCommandType.NONE
            && combat.getTarget() == null
            && AWCoreRegistryAccess.is(combat.getOrdersStack(), AWCoreIds.COMBAT_ORDER)) {
            flags |= 32;
         }

         return flags;
      }
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putString("NpcType", this.getNpcType());
      tag.putString("NpcSubtype", this.getNpcSubtype());
      if (!this.getSkinName().isBlank()) {
         tag.putString("SkinName", this.getSkinName());
      }

      if (this.isAlexModel()) {
         tag.putBoolean("AlexModel", true);
      }

      if (this.getCombatAttackCount() > 0) {
         tag.putInt("CombatAttackCount", this.getCombatAttackCount());
      }
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.setPersistenceRequired();
      if (tag.contains("NpcType")) {
         this.setNpcType(tag.getString("NpcType"));
      }

      if (tag.contains("NpcSubtype")) {
         this.setNpcSubtype(tag.getString("NpcSubtype"));
      }

      if (tag.contains("SkinName")) {
         this.setSkinName(tag.getString("SkinName"));
      }

      this.setAlexModel(tag.getBoolean("AlexModel"));
      if (tag.contains("CombatAttackCount")) {
         this.entityData.set(COMBAT_ATTACK_COUNT, Math.max(0, Math.min(100, tag.getInt("CombatAttackCount"))));
      }

      this.applyCombatRankAttributes();
   }

   public String getNpcType() {
      return (String)this.entityData.get(NPC_TYPE);
   }

   public void setNpcType(String npcType) {
      this.entityData.set(NPC_TYPE, npcType != null && !npcType.isBlank() ? npcType : "worker");
   }

   public String getNpcSubtype() {
      return (String)this.entityData.get(NPC_SUBTYPE);
   }

   public void setNpcSubtype(String npcSubtype) {
      this.entityData.set(NPC_SUBTYPE, npcSubtype != null && !npcSubtype.isBlank() ? npcSubtype : "miner");
   }

   public String getSkinName() {
      return (String)this.entityData.get(SKIN_NAME);
   }

   public void setSkinName(String skinName) {
      this.entityData.set(SKIN_NAME, skinName == null ? "" : skinName.trim());
   }

   public boolean isAlexModel() {
      return (Boolean)this.entityData.get(ALEX_MODEL);
   }

   public void setAlexModel(boolean alexModel) {
      this.entityData.set(ALEX_MODEL, alexModel);
   }

   public int getCombatAttackCount() {
      return (Integer)this.entityData.get(COMBAT_ATTACK_COUNT);
   }

   public int getCombatRank() {
      int attacks = this.getCombatAttackCount();
      return attacks >= 100 ? 2 : (attacks >= 50 ? 1 : 0);
   }

   public boolean isEliteCombatRank() {
      return this.getCombatRank() >= 1;
   }

   public boolean isAceCombatRank() {
      return this.getCombatRank() >= 2;
   }

   protected boolean supportsCombatPromotion() {
      return false;
   }

   protected final void recordCombatAttack() {
      if (!this.level().isClientSide && this.supportsCombatPromotion()) {
         int previousRank = this.getCombatRank();
         int attacks = Math.min(100, this.getCombatAttackCount() + 1);
         this.entityData.set(COMBAT_ATTACK_COUNT, attacks);
         if (this.getCombatRank() != previousRank) {
            this.applyCombatRankAttributes();
         }
      }
   }

   protected final void applyCombatRankAttributes() {
      if (!this.level().isClientSide && this.supportsCombatPromotion()) {
         int rank = this.getCombatRank();
         AttributeInstance health = this.getAttribute(Attributes.MAX_HEALTH);
         AttributeInstance damage = this.getAttribute(Attributes.ATTACK_DAMAGE);
         if (health != null && damage != null) {
            float healthRatio = this.getMaxHealth() > 0.0F ? this.getHealth() / this.getMaxHealth() : 1.0F;
            health.removeModifier(COMBAT_RANK_HEALTH_MODIFIER);
            damage.removeModifier(COMBAT_RANK_DAMAGE_MODIFIER);
            if (rank > 0) {
               double multiplierBonus = rank == 1 ? 1.0 : 3.0;
               health.addPermanentModifier(
                  new AttributeModifier(COMBAT_RANK_HEALTH_MODIFIER, "Ancient Warfare combat rank health", multiplierBonus, Operation.MULTIPLY_BASE)
               );
               damage.addPermanentModifier(
                  new AttributeModifier(COMBAT_RANK_DAMAGE_MODIFIER, "Ancient Warfare combat rank damage", multiplierBonus, Operation.MULTIPLY_BASE)
               );
            }

            this.setHealth(Math.min(this.getMaxHealth(), Math.max(0.0F, this.getMaxHealth() * healthRatio)));
         }
      }
   }

   protected final Component applyCombatRankName(Component baseName) {
      return (Component)(switch (this.getCombatRank()) {
         case 1 -> Component.translatable("entity.ancientwarfare.combat.rank.elite", new Object[]{baseName});
         case 2 -> Component.translatable("entity.ancientwarfare.combat.rank.ace", new Object[]{baseName});
         default -> baseName;
      });
   }

   public boolean isShielding() {
      return (Boolean)this.entityData.get(SHIELDING);
   }

   public void setShielding(boolean shielding) {
      boolean shouldShield = shielding && NpcShielding.hasShield(this);
      this.entityData.set(SHIELDING, shouldShield);
      if (!this.level().isClientSide) {
         if (shouldShield) {
            if (!this.isUsingItem() || this.getUsedItemHand() != InteractionHand.OFF_HAND || !NpcShielding.isShield(this.getUseItem())) {
               this.startUsingItem(InteractionHand.OFF_HAND);
            }
         } else if (this.isUsingItem() && this.getUsedItemHand() == InteractionHand.OFF_HAND && NpcShielding.isShield(this.getUseItem())) {
            this.stopUsingItem();
         }
      }
   }

   public int getMeleeAttackAnimationTicks() {
      return (Integer)this.entityData.get(MELEE_ATTACK_ANIMATION);
   }

   protected void triggerMeleeAttackAnimation() {
      if (!this.level().isClientSide) {
         this.entityData.set(MELEE_ATTACK_ANIMATION, 12);
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      LivingEntity responsibleAttacker = source.getEntity() instanceof LivingEntity livingx
         ? livingx
         : (source.getDirectEntity() instanceof LivingEntity living ? living : null);
      if (this.isShielding() && source.getDirectEntity() != null && !bypassesNpcShield(source)) {
         Entity directAttacker = source.getDirectEntity();
         Vec3 sourcePosition = directAttacker.position();
         if (NpcShielding.isBlockedFromFront(this, sourcePosition)) {
            if (responsibleAttacker != null) {
               this.setLastHurtByMob(responsibleAttacker);
            }

            Vec3 away = directAttacker.position().subtract(this.position());
            Vec3 horizontal = new Vec3(away.x, 0.0, away.z);
            if (horizontal.lengthSqr() > 1.0E-4) {
               horizontal = horizontal.normalize();
               Vec3 current = directAttacker.getDeltaMovement();
               double rebound = directAttacker instanceof Projectile ? 0.55 : 0.35;
               directAttacker.setDeltaMovement(horizontal.x * rebound, Math.max(current.y, 0.18), horizontal.z * rebound);
               directAttacker.hurtMarked = true;
            }

            this.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
            return false;
         }
      }

      float scaled = amount * NpcCombatRoles.damageMultiplier(source, this);
      boolean hurt = super.hurt(source, scaled);
      if (hurt && !this.level().isClientSide) {
         this.loseMorale(scaled * MORALE_PER_DAMAGE * (isHitFromBehind(this, source) ? FLANK_MORALE_FACTOR : 1.0F), source.getEntity());
      }

      return hurt;
   }

   @Override
   public void die(DamageSource damageSource) {
      if (!this.level().isClientSide && this.usesMorale()) {
         // Seeing a comrade fall shakes everyone close by.
         for (NpcBaseEntity ally : this.level().getEntitiesOfClass(NpcBaseEntity.class, this.getBoundingBox().inflate(MORALE_RADIUS), other -> other != this && other.isAlive() && isMoraleAlly(this, other))) {
            ally.loseMorale(ALLY_DEATH_MORALE, damageSource.getEntity());
         }
      }

      super.die(damageSource);
   }

   /** Soldiers keep morale; it drops with wounds, flank hits and fallen comrades, and at the bottom they rout. */
   protected boolean usesMorale() {
      return false;
   }

   /** Leaders steady the men around them and break less easily themselves. */
   protected boolean isMoraleLeader() {
      return false;
   }

   public float getMorale() {
      return this.morale;
   }

   public boolean isRouting() {
      return this.routTicks > 0;
   }

   Vec3 getRoutThreat() {
      return this.routThreat == null ? this.position() : this.routThreat;
   }

   public void loseMorale(float amount, Entity source) {
      if (this.usesMorale() && !this.isRouting()) {
         this.morale -= amount * (this.isMoraleLeader() ? LEADER_MORALE_RESISTANCE : 1.0F);
         if (this.morale <= ROUT_THRESHOLD) {
            LivingEntity target = this.getTarget();
            this.routThreat = source != null ? source.position() : (target != null ? target.position() : null);
            this.routTicks = ROUT_TICKS;
            this.setTarget(null);
         }
      }
   }

   private void tickMorale() {
      if (this.usesMorale()) {
         if (this.routTicks > 0) {
            this.routTicks--;
            if (this.routTicks % 20 == 0 && this.level() instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getEyeY() + 0.3, this.getZ(), 6, 0.2, 0.1, 0.2, 0.0);
            }

            if (this.routTicks == 0) {
               this.morale = RALLY_MORALE;
               this.routThreat = null;
            }
         } else if (this.morale < MAX_MORALE) {
            if (this.tickCount % 40 == 0) {
               this.leaderNearby = !this.isMoraleLeader()
                  && !this.level().getEntitiesOfClass(NpcBaseEntity.class, this.getBoundingBox().inflate(MORALE_RADIUS), other -> other.isMoraleLeader() && other.isAlive() && isMoraleAlly(this, other)).isEmpty();
            }

            float regen = this.getTarget() == null ? IDLE_MORALE_REGEN : COMBAT_MORALE_REGEN;
            this.morale = Math.min(MAX_MORALE, this.morale + regen * (this.leaderNearby ? LEADER_REGEN_FACTOR : 1.0F));
         }
      }
   }

   private static boolean isMoraleAlly(NpcBaseEntity self, NpcBaseEntity other) {
      if (self instanceof NpcOwnerAccess selfOwned && other instanceof NpcOwnerAccess otherOwned && selfOwned.getOwner().isPresent()) {
         return selfOwned.getOwner().equals(otherOwned.getOwner());
      } else if (self instanceof NpcFactionAccess selfFaction && other instanceof NpcFactionAccess otherFaction) {
         return selfFaction.getFactionName().equals(otherFaction.getFactionName());
      } else {
         return false;
      }
   }

   private static boolean isHitFromBehind(LivingEntity victim, DamageSource source) {
      Entity attacker = source.getDirectEntity();
      if (attacker == null) {
         return false;
      } else {
         Vec3 toAttacker = attacker.position().subtract(victim.position()).multiply(1.0, 0.0, 1.0);
         Vec3 facing = Vec3.directionFromRotation(0.0F, victim.getYRot());
         return toAttacker.lengthSqr() > 1.0E-4 && toAttacker.normalize().dot(facing) < FLANK_DOT;
      }
   }

   public boolean isDamageSourceBlocked(DamageSource source) {
      return !bypassesNpcShield(source) && super.isDamageSourceBlocked(source);
   }

   private static boolean bypassesNpcShield(DamageSource source) {
      Entity direct = source.getDirectEntity();
      if (direct instanceof BlockGraceSiegeProjectileEntity) {
         return true;
      } else {
         if (direct instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) {
            return true;
         }

         return false;
      }
   }

   public boolean canAttack(LivingEntity target) {
      return NpcTargeting.canHarm(this, target) && super.canAttack(target);
   }

   public void setTarget(LivingEntity target) {
      super.setTarget(target != null && (this.isRouting() || !NpcTargeting.canHarm(this, target)) ? null : target);
   }

   public boolean doHurtTarget(Entity target) {
      if (NpcTargeting.canHarm(this, target) && this.hasMeleeLineOfSight(target)) {
         boolean hit = super.doHurtTarget(target);
         if (hit && this instanceof FactionNpcEntity factionNpc && this.random.nextFloat() < 0.2F) {
            NpcVoiceSounds.playFactionAttack(factionNpc);
         }

         return hit;
      } else {
         return false;
      }
   }

   public final boolean hasMeleeLineOfSight(Entity target) {
      if (target != null && target.isAlive()) {
         AABB ownBox = this.getBoundingBox();
         AABB targetBox = target.getBoundingBox();
         if (ownBox.inflate(0.05).intersects(targetBox)) {
            return true;
         } else {
            Vec3 start = this.getEyePosition(1.0F);
            double centerX = (targetBox.minX + targetBox.maxX) * 0.5;
            double centerZ = (targetBox.minZ + targetBox.maxZ) * 0.5;
            double height = Math.max(0.1, targetBox.maxY - targetBox.minY);
            Vec3[] samples = new Vec3[]{
               target instanceof LivingEntity living ? living.getEyePosition(1.0F) : new Vec3(centerX, targetBox.minY + height * 0.75, centerZ),
               new Vec3(centerX, targetBox.minY + height * 0.5, centerZ),
               new Vec3(centerX, targetBox.minY + height * 0.8, centerZ)
            };

            for (Vec3 end : samples) {
               if (this.level().clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, this)).getType() == Type.MISS) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return NpcVoiceSounds.hurt(this);
   }

   protected SoundEvent getDeathSound() {
      return NpcVoiceSounds.death(this);
   }

   public Component getTypeName() {
      return Component.translatable("entity.ancientwarfare." + this.getNpcType() + "." + this.getNpcSubtype());
   }
}
