package net.shadowmage.ancientwarfare.core.entity;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.npc.NpcShielding;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargeting;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public abstract class NpcBaseEntity extends PathfinderMob {
   public static final double PLAYER_WALK_SPEED = 0.1;
   public static final double PLAYER_SPRINT_SPEED = 0.13;
   private double movementSpeedBeforeShield = -1.0;
   private static final EntityDataAccessor<String> NPC_TYPE = SynchedEntityData.m_135353_(NpcBaseEntity.class, EntityDataSerializers.f_135030_);
   private static final EntityDataAccessor<String> NPC_SUBTYPE = SynchedEntityData.m_135353_(NpcBaseEntity.class, EntityDataSerializers.f_135030_);
   private static final EntityDataAccessor<String> SKIN_NAME = SynchedEntityData.m_135353_(NpcBaseEntity.class, EntityDataSerializers.f_135030_);
   private static final EntityDataAccessor<Boolean> ALEX_MODEL = SynchedEntityData.m_135353_(NpcBaseEntity.class, EntityDataSerializers.f_135035_);
   private static final EntityDataAccessor<Boolean> SHIELDING = SynchedEntityData.m_135353_(NpcBaseEntity.class, EntityDataSerializers.f_135035_);
   private static final EntityDataAccessor<Integer> MELEE_ATTACK_ANIMATION = SynchedEntityData.m_135353_(NpcBaseEntity.class, EntityDataSerializers.f_135028_);
   private static final EntityDataAccessor<Integer> STATUS_ICONS = SynchedEntityData.m_135353_(NpcBaseEntity.class, EntityDataSerializers.f_135028_);
   private static final EntityDataAccessor<Integer> COMBAT_ATTACK_COUNT = SynchedEntityData.m_135353_(NpcBaseEntity.class, EntityDataSerializers.f_135028_);
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

   protected NpcBaseEntity(EntityType<? extends NpcBaseEntity> entityType, Level level) {
      super(entityType, level);
      this.m_21530_();
      this.m_21573_().m_26575_().m_77351_(true);
      this.m_21573_().m_26575_().m_77355_(true);
      this.f_21345_.m_25352_(1, new NpcOpenDoorGoal(this));
   }

   public boolean m_6785_(double distanceToClosestPlayer) {
      return false;
   }

   public int m_6056_() {
      return 1;
   }

   public void m_6478_(MoverType moverType, Vec3 movement) {
      if (moverType == MoverType.SELF && !NpcMovementSafety.canMoveSafely(this, movement)) {
         this.m_21573_().m_26573_();
         movement = new Vec3(0.0, movement.f_82480_, 0.0);
      }

      super.m_6478_(moverType, movement);
   }

   protected void m_8097_() {
      super.m_8097_();
      this.f_19804_.m_135372_(NPC_TYPE, "worker");
      this.f_19804_.m_135372_(NPC_SUBTYPE, "miner");
      this.f_19804_.m_135372_(SKIN_NAME, "");
      this.f_19804_.m_135372_(ALEX_MODEL, false);
      this.f_19804_.m_135372_(SHIELDING, false);
      this.f_19804_.m_135372_(MELEE_ATTACK_ANIMATION, 0);
      this.f_19804_.m_135372_(STATUS_ICONS, 0);
      this.f_19804_.m_135372_(COMBAT_ATTACK_COUNT, 0);
   }

   public void m_8119_() {
      this.updateShieldMovementSpeed();
      if (!this.m_9236_().f_46443_) {
         if (this.m_20202_() instanceof Mob mount) {
            mount.m_21530_();
         }

         NpcSuffocationEscape.tryEscape(this);
         int meleeAnimation = this.getMeleeAttackAnimationTicks();
         if (meleeAnimation > 0) {
            this.f_19804_.m_135381_(MELEE_ATTACK_ANIMATION, meleeAnimation - 1);
         }
      }

      super.m_8119_();
      if (!this.m_9236_().f_46443_) {
         NpcWaterEscape.tick(this);
         int flags = this.calculateStatusIconFlags();
         if (flags != this.getStatusIconFlags()) {
            this.f_19804_.m_135381_(STATUS_ICONS, flags);
         }
      }
   }

   private void updateShieldMovementSpeed() {
      if (!this.m_9236_().f_46443_) {
         AttributeInstance movement = this.m_21051_(Attributes.f_22279_);
         if (movement != null) {
            if (this.isShielding() && this.movementSpeedBeforeShield < 0.0) {
               this.movementSpeedBeforeShield = movement.m_22115_();
               movement.m_22100_(this.movementSpeedBeforeShield * 0.5);
            } else if (!this.isShielding() && this.movementSpeedBeforeShield >= 0.0) {
               movement.m_22100_(this.movementSpeedBeforeShield);
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
      return (Integer)this.f_19804_.m_135370_(STATUS_ICONS);
   }

   protected int calculateStatusIconFlags() {
      int flags = 0;
      if (this.m_5448_() != null) {
         flags |= 1;
      }

      if (this.m_5803_()) {
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

         if (owned.getFoodRemaining() <= 0 && !owned.getUpkeepStack().m_41619_()) {
            flags |= 4;
         }

         if (owned instanceof WorkerNpcEntity worker) {
            String state = worker.getWorkState();
            if ("working".equals(state) || "moving_to_work".equals(state)) {
               flags |= 16;
            }

            if ("sleeping".equals(state) || "sleeping_no_bed".equals(state)) {
               flags |= 4096;
               if (owned.hasHome() && !worker.m_5803_()) {
                  flags |= 8;
               }
            }
         }

         if (owned instanceof CombatNpcEntity combat
            && command == NpcCommandType.NONE
            && combat.m_5448_() == null
            && AWCoreRegistryAccess.is(combat.getOrdersStack(), AWCoreIds.COMBAT_ORDER)) {
            flags |= 32;
         }

         return flags;
      }
   }

   public void m_7380_(CompoundTag tag) {
      super.m_7380_(tag);
      tag.m_128359_("NpcType", this.getNpcType());
      tag.m_128359_("NpcSubtype", this.getNpcSubtype());
      if (!this.getSkinName().isBlank()) {
         tag.m_128359_("SkinName", this.getSkinName());
      }

      if (this.isAlexModel()) {
         tag.m_128379_("AlexModel", true);
      }

      if (this.getCombatAttackCount() > 0) {
         tag.m_128405_("CombatAttackCount", this.getCombatAttackCount());
      }
   }

   public void m_7378_(CompoundTag tag) {
      super.m_7378_(tag);
      this.m_21530_();
      if (tag.m_128441_("NpcType")) {
         this.setNpcType(tag.m_128461_("NpcType"));
      }

      if (tag.m_128441_("NpcSubtype")) {
         this.setNpcSubtype(tag.m_128461_("NpcSubtype"));
      }

      if (tag.m_128441_("SkinName")) {
         this.setSkinName(tag.m_128461_("SkinName"));
      }

      this.setAlexModel(tag.m_128471_("AlexModel"));
      if (tag.m_128441_("CombatAttackCount")) {
         this.f_19804_.m_135381_(COMBAT_ATTACK_COUNT, Math.max(0, Math.min(100, tag.m_128451_("CombatAttackCount"))));
      }

      this.applyCombatRankAttributes();
   }

   public String getNpcType() {
      return (String)this.f_19804_.m_135370_(NPC_TYPE);
   }

   public void setNpcType(String npcType) {
      this.f_19804_.m_135381_(NPC_TYPE, npcType != null && !npcType.isBlank() ? npcType : "worker");
   }

   public String getNpcSubtype() {
      return (String)this.f_19804_.m_135370_(NPC_SUBTYPE);
   }

   public void setNpcSubtype(String npcSubtype) {
      this.f_19804_.m_135381_(NPC_SUBTYPE, npcSubtype != null && !npcSubtype.isBlank() ? npcSubtype : "miner");
   }

   public String getSkinName() {
      return (String)this.f_19804_.m_135370_(SKIN_NAME);
   }

   public void setSkinName(String skinName) {
      this.f_19804_.m_135381_(SKIN_NAME, skinName == null ? "" : skinName.trim());
   }

   public boolean isAlexModel() {
      return (Boolean)this.f_19804_.m_135370_(ALEX_MODEL);
   }

   public void setAlexModel(boolean alexModel) {
      this.f_19804_.m_135381_(ALEX_MODEL, alexModel);
   }

   public int getCombatAttackCount() {
      return (Integer)this.f_19804_.m_135370_(COMBAT_ATTACK_COUNT);
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
      if (!this.m_9236_().f_46443_ && this.supportsCombatPromotion()) {
         int previousRank = this.getCombatRank();
         int attacks = Math.min(100, this.getCombatAttackCount() + 1);
         this.f_19804_.m_135381_(COMBAT_ATTACK_COUNT, attacks);
         if (this.getCombatRank() != previousRank) {
            this.applyCombatRankAttributes();
         }
      }
   }

   protected final void applyCombatRankAttributes() {
      if (!this.m_9236_().f_46443_ && this.supportsCombatPromotion()) {
         int rank = this.getCombatRank();
         AttributeInstance health = this.m_21051_(Attributes.f_22276_);
         AttributeInstance damage = this.m_21051_(Attributes.f_22281_);
         if (health != null && damage != null) {
            float healthRatio = this.m_21233_() > 0.0F ? this.m_21223_() / this.m_21233_() : 1.0F;
            health.m_22120_(COMBAT_RANK_HEALTH_MODIFIER);
            damage.m_22120_(COMBAT_RANK_DAMAGE_MODIFIER);
            if (rank > 0) {
               double multiplierBonus = rank == 1 ? 1.0 : 3.0;
               health.m_22125_(
                  new AttributeModifier(COMBAT_RANK_HEALTH_MODIFIER, "Ancient Warfare combat rank health", multiplierBonus, Operation.MULTIPLY_BASE)
               );
               damage.m_22125_(
                  new AttributeModifier(COMBAT_RANK_DAMAGE_MODIFIER, "Ancient Warfare combat rank damage", multiplierBonus, Operation.MULTIPLY_BASE)
               );
            }

            this.m_21153_(Math.min(this.m_21233_(), Math.max(0.0F, this.m_21233_() * healthRatio)));
         }
      }
   }

   protected final Component applyCombatRankName(Component baseName) {
      return (Component)(switch (this.getCombatRank()) {
         case 1 -> Component.m_237110_("entity.ancientwarfare.combat.rank.elite", new Object[]{baseName});
         case 2 -> Component.m_237110_("entity.ancientwarfare.combat.rank.ace", new Object[]{baseName});
         default -> baseName;
      });
   }

   public boolean isShielding() {
      return (Boolean)this.f_19804_.m_135370_(SHIELDING);
   }

   public void setShielding(boolean shielding) {
      boolean shouldShield = shielding && NpcShielding.hasShield(this);
      this.f_19804_.m_135381_(SHIELDING, shouldShield);
      if (!this.m_9236_().f_46443_) {
         if (shouldShield) {
            if (!this.m_6117_() || this.m_7655_() != InteractionHand.OFF_HAND || !NpcShielding.isShield(this.m_21211_())) {
               this.m_6672_(InteractionHand.OFF_HAND);
            }
         } else if (this.m_6117_() && this.m_7655_() == InteractionHand.OFF_HAND && NpcShielding.isShield(this.m_21211_())) {
            this.m_5810_();
         }
      }
   }

   public int getMeleeAttackAnimationTicks() {
      return (Integer)this.f_19804_.m_135370_(MELEE_ATTACK_ANIMATION);
   }

   protected void triggerMeleeAttackAnimation() {
      if (!this.m_9236_().f_46443_) {
         this.f_19804_.m_135381_(MELEE_ATTACK_ANIMATION, 12);
      }
   }

   public boolean m_6469_(DamageSource source, float amount) {
      LivingEntity responsibleAttacker = source.m_7639_() instanceof LivingEntity livingx
         ? livingx
         : (source.m_7640_() instanceof LivingEntity living ? living : null);
      if (this.isShielding() && source.m_7640_() != null && !bypassesNpcShield(source)) {
         Entity directAttacker = source.m_7640_();
         Vec3 sourcePosition = directAttacker.m_20182_();
         if (NpcShielding.isBlockedFromFront(this, sourcePosition)) {
            if (responsibleAttacker != null) {
               this.m_6703_(responsibleAttacker);
            }

            Vec3 away = directAttacker.m_20182_().m_82546_(this.m_20182_());
            Vec3 horizontal = new Vec3(away.f_82479_, 0.0, away.f_82481_);
            if (horizontal.m_82556_() > 1.0E-4) {
               horizontal = horizontal.m_82541_();
               Vec3 current = directAttacker.m_20184_();
               double rebound = directAttacker instanceof Projectile ? 0.55 : 0.35;
               directAttacker.m_20334_(horizontal.f_82479_ * rebound, Math.max(current.f_82480_, 0.18), horizontal.f_82481_ * rebound);
               directAttacker.f_19864_ = true;
            }

            this.m_5496_(SoundEvents.f_12346_, 1.0F, 0.9F + this.f_19796_.m_188501_() * 0.2F);
            return false;
         }
      }

      return super.m_6469_(source, amount);
   }

   public boolean m_21275_(DamageSource source) {
      return !bypassesNpcShield(source) && super.m_21275_(source);
   }

   private static boolean bypassesNpcShield(DamageSource source) {
      Entity direct = source.m_7640_();
      if (direct instanceof BlockGraceSiegeProjectileEntity) {
         return true;
      } else {
         if (direct instanceof AbstractArrow arrow && arrow.m_36796_() > 0) {
            return true;
         }

         return false;
      }
   }

   public boolean m_6779_(LivingEntity target) {
      return NpcTargeting.canHarm(this, target) && super.m_6779_(target);
   }

   public void m_6710_(LivingEntity target) {
      super.m_6710_(target != null && !NpcTargeting.canHarm(this, target) ? null : target);
   }

   public boolean m_7327_(Entity target) {
      if (NpcTargeting.canHarm(this, target) && this.hasMeleeLineOfSight(target)) {
         boolean hit = super.m_7327_(target);
         if (hit && this instanceof FactionNpcEntity factionNpc && this.f_19796_.m_188501_() < 0.2F) {
            NpcVoiceSounds.playFactionAttack(factionNpc);
         }

         return hit;
      } else {
         return false;
      }
   }

   public final boolean hasMeleeLineOfSight(Entity target) {
      if (target != null && target.m_6084_()) {
         AABB ownBox = this.m_20191_();
         AABB targetBox = target.m_20191_();
         if (ownBox.m_82400_(0.05).m_82381_(targetBox)) {
            return true;
         } else {
            Vec3 start = this.m_20299_(1.0F);
            double centerX = (targetBox.f_82288_ + targetBox.f_82291_) * 0.5;
            double centerZ = (targetBox.f_82290_ + targetBox.f_82293_) * 0.5;
            double height = Math.max(0.1, targetBox.f_82292_ - targetBox.f_82289_);
            Vec3[] samples = new Vec3[]{
               target instanceof LivingEntity living ? living.m_20299_(1.0F) : new Vec3(centerX, targetBox.f_82289_ + height * 0.75, centerZ),
               new Vec3(centerX, targetBox.f_82289_ + height * 0.5, centerZ),
               new Vec3(centerX, targetBox.f_82289_ + height * 0.8, centerZ)
            };

            for (Vec3 end : samples) {
               if (this.m_9236_().m_45547_(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, this)).m_6662_() == Type.MISS) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   protected SoundEvent m_7975_(DamageSource source) {
      return NpcVoiceSounds.hurt(this);
   }

   protected SoundEvent m_5592_() {
      return NpcVoiceSounds.death(this);
   }

   public Component m_5677_() {
      return Component.m_237115_("entity.ancientwarfare." + this.getNpcType() + "." + this.getNpcSubtype());
   }
}
