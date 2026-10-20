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
   private static final EntityDataAccessor<String> FACTION_NAME = SynchedEntityData.m_135353_(FactionNpcEntity.class, EntityDataSerializers.f_135030_);
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
   protected void m_8097_() {
      super.m_8097_();
      this.f_19804_.m_135372_(FACTION_NAME, "brigand");
   }

   @Override
   public void m_7380_(CompoundTag tag) {
      super.m_7380_(tag);
      tag.m_128359_("FactionName", this.getFactionName());
   }

   @Override
   public void m_7378_(CompoundTag tag) {
      super.m_7378_(tag);
      if (tag.m_128441_("FactionName")) {
         this.setFactionName(tag.m_128461_("FactionName"));
      }

      this.applyFactionDefaults();
   }

   @Override
   public String getFactionName() {
      return (String)this.f_19804_.m_135370_(FACTION_NAME);
   }

   public void setFactionName(String factionName) {
      String normalized = factionName != null && !factionName.isBlank() ? factionName.trim() : "brigand";
      this.f_19804_.m_135381_(FACTION_NAME, normalized);
      this.setNpcType(normalized);
      this.applyFactionDefaults();
      LivingEntity currentTarget = this.m_5448_();
      if (currentTarget != null && this.m_7307_(currentTarget)) {
         this.m_6710_(null);
      }
   }

   @Override
   public String getNpcType() {
      return this.getFactionName();
   }

   @Override
   public Component m_5677_() {
      return this.applyCombatRankName(
         Component.m_237110_(
            "entity.ancientwarfare.faction.named",
            new Object[]{this.getFactionName(), Component.m_237115_("entity.ancientwarfare.faction." + this.getNpcSubtype())}
         )
      );
   }

   protected void addSharedGoals() {
      this.addSharedGoals(true);
   }

   protected void addSharedGoals(boolean allowRandomStroll) {
      if (!(this instanceof FactionSoldierNpcEntity)) {
         this.f_21345_.m_25352_(0, new FloatGoal(this));
      }

      if (allowRandomStroll) {
         this.f_21345_.m_25352_(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
      }

      this.f_21345_.m_25352_(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.f_21345_.m_25352_(8, new RandomLookAroundGoal(this));
   }

   public boolean isHostileTowards(Entity entity) {
      if (!NpcTargeting.canHarm(this, entity)) {
         return false;
      } else if (entity instanceof FactionNpcEntity factionNpc) {
         return this.m_7307_(factionNpc) ? false : FactionRegistry.isHostile(this.getFactionName(), factionNpc.getFactionName());
      } else if (!(entity instanceof Player player)) {
         return entity instanceof PlayerOwnedNpcEntity ownedNpc ? ownedNpc.getOwner().map(owner -> {
            if (FactionRegistry.get(this.getFactionName()).defaultStanding() < 0) {
               return true;
            } else {
               if (this.m_9236_() instanceof ServerLevel serverLevelx && FactionStandingData.get(serverLevelx).isHostile(owner, this.getFactionName())) {
                  return true;
               }

               return false;
            }
         }).orElse(false) : false;
      } else if (player.m_7500_()) {
         return false;
      } else if (FactionRegistry.get(this.getFactionName()).defaultStanding() < 0) {
         return true;
      } else {
         if (this.m_9236_() instanceof ServerLevel serverLevel && FactionStandingData.get(serverLevel).isHostile(player.m_20148_(), this.getFactionName())) {
            return true;
         }

         return false;
      }
   }

   public boolean m_7307_(Entity entity) {
      if (entity instanceof FactionNpcEntity factionNpc && FactionRegistry.isSameFaction(this.getFactionName(), factionNpc.getFactionName())) {
         return true;
      }

      return super.m_7307_(entity);
   }

   @Override
   public boolean m_6779_(LivingEntity target) {
      return NpcTargeting.canHarm(this, target) && super.m_6779_(target);
   }

   @Override
   public void m_6710_(LivingEntity target) {
      if (target != null && !NpcTargeting.canHarm(this, target)) {
         super.m_6710_(null);
      } else {
         LivingEntity current = this.m_5448_();
         if (target != null && current != null && current != target && this.isValidLockedTarget(current)) {
            int currentTier = this.targetPriorityTier(current);
            int incomingTier = this.targetPriorityTier(target);
            if (incomingTier >= currentTier) {
               return;
            }
         }

         super.m_6710_(target);
      }
   }

   @Override
   public boolean m_7327_(Entity target) {
      if (!NpcTargeting.canHarm(this, target)) {
         if (target == this.m_5448_()) {
            this.m_6710_(null);
         }

         return false;
      } else {
         return super.m_7327_(target);
      }
   }

   protected void addFactionCombatTargets() {
      this.addFactionCombatTargets(true);
   }

   protected void addFactionCombatTargets(boolean mustSee) {
      this.f_21346_.m_25352_(1, new FactionNpcEntity.FactionHurtByTargetGoal(this));
      this.f_21346_
         .m_25352_(
            2,
            new VerticalRangeTargetGoal<LivingEntity>(
               this, LivingEntity.class, 4, false, false, target -> this.isPlayerTierTarget(target) && this.canAcquireFactionTarget(target), 96.0
            )
         );
      this.f_21346_
         .m_25352_(
            4,
            new VerticalRangeTargetGoal(
               this, FactionNpcEntity.class, 4, mustSee, false, target -> this.isFactionTierTarget(target) && this.canAcquireFactionTarget(target), 96.0
            )
         );
      this.f_21346_
         .m_25352_(
            5,
            new VerticalRangeTargetGoal<LivingEntity>(
               this, LivingEntity.class, 4, mustSee, false, target -> this.isMonsterTierTarget(target) && this.canAcquireFactionTarget(target), 96.0
            )
         );
   }

   protected final boolean isPlayerTierTarget(LivingEntity target) {
      return target != null && target.m_6084_() && (target instanceof Player || target instanceof PlayerOwnedNpcEntity) && this.isHostileTowards(target);
   }

   protected final boolean isFactionTierTarget(LivingEntity target) {
      if (target instanceof FactionNpcEntity factionNpc && target.m_6084_() && this.isHostileTowards(target)) {
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
         return target == this.m_21188_() && NpcTargeting.canHarm(this, target) ? 3 : Integer.MAX_VALUE;
      }
   }

   private boolean isValidLockedTarget(LivingEntity target) {
      return NpcTargeting.canHarm(this, target) && this.targetPriorityTier(target) != Integer.MAX_VALUE;
   }

   protected boolean canAcquireFactionTarget(LivingEntity target) {
      return true;
   }

   @Override
   public boolean m_6469_(DamageSource source, float amount) {
      LivingEntity attacker = source.m_7639_() instanceof LivingEntity livingx ? livingx : (source.m_7640_() instanceof LivingEntity living ? living : null);
      if (attacker == null || !NpcTargeting.isProtectedAlly(this, attacker) && !this.m_7307_(attacker) && !attacker.m_7307_(this)) {
         boolean damaged = super.m_6469_(source, amount);
         if (!this.m_9236_().f_46443_ && NpcTargeting.canHarm(this, attacker)) {
            this.m_6703_(attacker);
            this.m_6710_(attacker);
            this.alertFactionCombatants(attacker);
         }

         return damaged;
      } else {
         return false;
      }
   }

   private void alertFactionCombatants(LivingEntity attacker) {
      this.m_9236_()
         .m_6443_(
            FactionNpcEntity.class,
            this.m_20191_().m_82377_(64.0, 16.0, 64.0),
            ally -> ally != this
                  && ally.m_6084_()
                  && ally.getFactionName().equalsIgnoreCase(this.getFactionName())
                  && isFactionCombatRole(ally)
                  && NpcTargeting.canHarm(ally, attacker)
         )
         .forEach(ally -> {
            ally.m_6703_(attacker);
            ally.m_6710_(attacker);
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

   public int m_213860_() {
      return this.f_20888_ != null && this.isHostileTowards(this.f_20888_) ? FactionNpcRole.byId(this.getNpcSubtype()).map(role -> {
         return switch (role) {
            case SOLDIER, ARCHER, CAVALRY, MOUNTED_ARCHER, LEADER, SIEGE_ENGINEER, SPELLCASTER -> 20;
            case SOLDIER_ELITE, ARCHER_ELITE, LEADER_ELITE -> 40;
            default -> 0;
         };
      }).orElse(0) : 0;
   }

   @Override
   public boolean m_6785_(double distanceToClosestPlayer) {
      return false;
   }

   public void m_8107_() {
      super.m_8107_();
      this.updateMountedMovement();
   }

   public double m_6049_() {
      return this.isMountedFactionRole() ? super.m_6049_() - 0.45 : super.m_6049_();
   }

   protected void updateMountedMovement() {
      if (!(this.m_20202_() instanceof Mob mount) || !this.isMountedFactionRole()) {
         return;
      }

      if (!NpcWaterEscape.isRecoveryActive(this)) {
         if (mount.m_21525_()) {
            mount.m_21557_(false);
         }

         mount.m_6710_(null);
         mount.m_21561_(false);
         if (!this.usesCustomMountedMovement()) {
            LivingEntity target = this.m_5448_();
            if (target != null && target.m_6084_()) {
               double distanceSqr = mount.m_20280_(target);
               double stopDistance = FactionNpcRole.MOUNTED_ARCHER.id().equals(this.getNpcSubtype()) ? 18.0 : NpcEquipmentResolver.meleeHoldDistance(this);
               if (distanceSqr > stopDistance * stopDistance) {
                  this.driveMountToward(mount, target);
               } else {
                  mount.m_21573_().m_26573_();
                  mount.m_20256_(mount.m_20184_().m_82542_(0.55, 1.0, 0.55));
                  this.faceMountedTarget(mount, target);
               }
            } else {
               mount.m_21573_().m_26573_();
               mount.m_20256_(mount.m_20184_().m_82542_(0.4, 1.0, 0.4));
            }
         }
      }
   }

   protected boolean usesCustomMountedMovement() {
      return false;
   }

   private void driveMountToward(Mob mount, LivingEntity target) {
      Vec3 delta = target.m_20182_().m_82546_(mount.m_20182_());
      Vec3 horizontal = new Vec3(delta.f_82479_, 0.0, delta.f_82481_);
      if (!(horizontal.m_82556_() < 1.0E-4)) {
         double speed = 0.22;
         AttributeInstance speedAttribute = mount.m_21051_(Attributes.f_22279_);
         if (speedAttribute != null) {
            speed = Math.max(speed, speedAttribute.m_22135_() * 0.65);
         }

         Vec3 direction = horizontal.m_82541_();
         if (!NpcMovementSafety.canMoveMountedSafely(mount, direction)) {
            mount.m_21573_().m_26573_();
            mount.m_20256_(mount.m_20184_().m_82542_(0.4, 1.0, 0.4));
         } else {
            Vec3 current = mount.m_20184_();
            mount.m_20334_(direction.f_82479_ * speed, current.f_82480_, direction.f_82481_ * speed);
            mount.m_21573_().m_26573_();
            this.faceDirection(mount, direction);
         }
      }
   }

   private void faceMountedTarget(Mob mount, LivingEntity target) {
      Vec3 delta = target.m_20182_().m_82546_(mount.m_20182_());
      Vec3 horizontal = new Vec3(delta.f_82479_, 0.0, delta.f_82481_);
      if (horizontal.m_82556_() >= 1.0E-4) {
         this.faceDirection(mount, horizontal.m_82541_());
      }
   }

   private void faceDirection(Mob mount, Vec3 direction) {
      float yaw = (float)(Math.atan2(direction.f_82481_, direction.f_82479_) * 180.0F / (float)Math.PI) - 90.0F;
      mount.m_146922_(yaw);
      mount.m_5618_(mount.m_146908_());
      mount.m_5616_(mount.m_146908_());
      this.m_146922_(yaw);
      this.m_5618_(yaw);
      this.m_5616_(yaw);
   }

   private boolean isMountedFactionRole() {
      return FactionNpcRole.CAVALRY.id().equals(this.getNpcSubtype()) || FactionNpcRole.MOUNTED_ARCHER.id().equals(this.getNpcSubtype());
   }

   protected void swingWeapon() {
      this.m_6674_(InteractionHand.MAIN_HAND);
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

      public boolean m_8036_() {
         LivingEntity attacker = this.npc.m_21188_();
         return NpcTargeting.canHarm(this.npc, attacker) && super.m_8036_();
      }

      public void m_8056_() {
         super.m_8056_();
         LivingEntity attacker = this.npc.m_21188_();
         if (NpcTargeting.canHarm(this.npc, attacker)) {
            this.npc.alertFactionCombatants(attacker);
         }
      }
   }
}
