package net.shadowmage.ancientwarfare.core.entity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.common.npc.CombatOrderData;
import net.shadowmage.ancientwarfare.core.common.npc.CombatProfessionResolver;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.npc.NpcEquipmentResolver;
import net.shadowmage.ancientwarfare.core.common.npc.NpcShielding;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargeting;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireDragonCompat;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireEquipmentCompat;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireHippocampusCompat;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireHippogryphCompat;
import net.shadowmage.ancientwarfare.core.compat.SlashBladeCompat;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;
import net.shadowmage.ancientwarfare.core.menu.NpcInventoryMenu;

public class CombatNpcEntity extends PlayerOwnedNpcEntity implements RangedAttackMob {
   private static final String TAG_SPELLBOOK_STACK = "SpellbookStack";
   private static final double MIN_RANGED_FOLLOW_RANGE = 48.0;
   private static final double RANGED_VERTICAL_TARGET_RANGE = 64.0;
   private static final EntityDataAccessor<Integer> SWORDSMAN_COMBO = SynchedEntityData.m_135353_(CombatNpcEntity.class, EntityDataSerializers.f_135028_);
   private static final int SWORDSMAN_POSE_TICKS = 10;
   private static final double SWORDSMAN_SKILL_START_RANGE_SQR = 1024.0;
   private static final double SWORDSMAN_SKILL_CANCEL_RANGE_SQR = 1600.0;
   private static final int SWORDSMAN_SLASH_ART_INITIAL_DELAY_TICKS = 200;
   private static final int SWORDSMAN_SLASH_ART_COOLDOWN_TICKS = 600;
   private static final int SWORDSMAN_SLASH_ART_MIN_CHARGE = 4;
   private static final int SWORDSMAN_SLASH_ART_GUARANTEED_CHARGE = 7;
   private static final double MOUNT_CHARGE_SPEED = 0.45;
   private static final double MOUNT_ATTACK_DISTANCE = 3.2;
   private static final double MOUNT_PASS_DISTANCE = 10.0;
   private static final int MOUNT_PASS_TICKS = 28;
   private static final double MOUNTED_ARCHER_SPEED = 0.45;
   private static final double MOUNTED_ARCHER_RADIUS = 14.0;
   private static final double MOUNTED_ARCHER_MIN_RADIUS = 10.0;
   private static final double MOUNTED_ARCHER_MAX_RADIUS = 18.0;
   private int patrolIndex;
   private boolean patrolAtPoint;
   private int patrolTicksAtPoint;
   private boolean patrolDrivingMount;
   private Vec3 mountPassDestination;
   private int mountPassTicks;
   private int mountAttackCooldown;
   private boolean mountAttackedThisCharge;
   private boolean mountedArcherClockwise;
   private LivingEntity mountedArcherTarget;
   private int swordsmanComboStep;
   private int swordsmanPoseTicks;
   private LivingEntity swordsmanComboTarget;
   private int swordsmanComboTicks;
   private boolean swordsmanComboActive;
   private CombatNpcEntity.SwordsmanTechnique swordsmanTechnique = CombatNpcEntity.SwordsmanTechnique.NONE;
   private CombatNpcEntity.SwordsmanTechnique swordsmanLastTechnique = CombatNpcEntity.SwordsmanTechnique.NONE;
   private CombatNpcEntity.SwordsmanTechnique swordsmanPreviousTechnique = CombatNpcEntity.SwordsmanTechnique.NONE;
   private boolean swordsmanAerialCleaveActive;
   private int swordsmanAerialCleaveTicks;
   private boolean swordsmanTechniqueFinished;
   private int swordsmanRapidHitCooldown;
   private int swordsmanRapidHitCount;
   private int swordsmanTechniqueRecoveryTicks;
   private int swordsmanLastSummonedSwordArt;
   private int swordsmanPreviousSummonedSwordArt;
   private int swordsmanChargedArt = 0;
   private int swordsmanChargeType = 1;
   private int swordsmanChargeTicks;
   private boolean swordsmanChargedArtReleased;
   private int swordsmanLastChargeType;
   private int swordsmanPreviousChargeType;
   private int swordsmanSlashArtCooldown = 200;
   private int swordsmanSlashArtCharge;
   private ItemStack legacySpellContainerStack = ItemStack.f_41583_;

   protected void m_7472_(DamageSource source, int looting, boolean killedByPlayer) {
      if (AWCorePlatform.gameplayConfig().playerSoldierEquipmentDropChance() < 0.0) {
         super.m_7472_(source, looting, killedByPlayer);
      }
   }

   protected void m_5907_() {
      double chance = AWCorePlatform.gameplayConfig().playerSoldierEquipmentDropChance();
      if (chance < 0.0) {
         super.m_5907_();
      } else if (!this.m_9236_().f_46443_ && this.m_9236_().m_46469_().m_46207_(GameRules.f_46135_)) {
         for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = this.m_6844_(slot);
            if (!stack.m_41619_()
               && !EnchantmentHelper.m_44924_(stack)
               && chance > 0.0
               && (chance >= 1.0 || this.f_19796_.m_188500_() < chance)
               && this.m_19983_(stack.m_41777_()) != null) {
               this.m_8061_(slot, ItemStack.f_41583_);
            }
         }
      }
   }

   protected PathNavigation m_6037_(Level level) {
      AmphibiousPathNavigation navigation = new AmphibiousPathNavigation(this, level);
      navigation.m_7008_(true);
      return navigation;
   }

   public CombatNpcEntity(EntityType<? extends CombatNpcEntity> entityType, Level level) {
      super(entityType, level);
      this.setNpcType("combat");
   }

   @Override
   protected boolean supportsCombatPromotion() {
      return true;
   }

   public static Builder createAttributes() {
      return m_21552_()
         .m_22268_(Attributes.f_22276_, 24.0)
         .m_22268_(Attributes.f_22279_, 0.27)
         .m_22268_(Attributes.f_22281_, 2.0)
         .m_22268_(Attributes.f_22283_, 4.0)
         .m_22268_(Attributes.f_22277_, 32.0);
   }

   @Override
   protected void m_8097_() {
      super.m_8097_();
      this.f_19804_.m_135372_(SWORDSMAN_COMBO, 0);
   }

   protected void m_8099_() {
      this.f_21345_.m_25352_(1, new CombatNpcEntity.MoveToCommandGoal(this, 1.0, 2.0));
      this.f_21345_.m_25352_(1, new CombatNpcEntity.ShieldDefenseGoal(this));
      this.f_21345_.m_25352_(0, new CombatNpcEntity.SwordsmanProjectileGuardGoal(this));
      this.f_21345_.m_25352_(2, new CombatNpcEntity.CombatRangedAttackGoal(this));
      this.f_21345_.m_25352_(2, new CombatNpcEntity.CombatMeleeAttackGoal(this, this.playerSprintNavigationSpeed(), true));
      this.f_21345_.m_25352_(3, new CombatNpcEntity.CommanderAuraGoal(this));
      this.f_21345_.m_25352_(3, new CombatNpcEntity.MedicHealGoal(this));
      this.f_21345_.m_25352_(3, new CombatGroupFollowGoal(this));
      this.f_21345_.m_25352_(4, new CombatNpcEntity.PatrolOrderGoal(this, 1.0));
      this.f_21345_.m_25352_(5, new CombatNpcEntity.ReturnHomeGoal(this, 1.0));
      this.f_21345_.m_25352_(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.f_21345_.m_25352_(7, new RandomLookAroundGoal(this));
      this.f_21346_.m_25352_(1, new CombatNpcEntity.PlayerSoldierHurtByTargetGoal(this));
      this.f_21346_
         .m_25352_(
            2,
            new VerticalRangeTargetGoal(
               this,
               FactionNpcEntity.class,
               2,
               false,
               false,
               target -> this.isHostileFactionNpc(target) && this.canAcquireCombatTarget(target),
               () -> this.isRangedCombatant() ? 64.0 : 4.0
            )
         );
      this.f_21346_
         .m_25352_(
            3,
            new VerticalRangeTargetGoal<LivingEntity>(
               this,
               LivingEntity.class,
               2,
               false,
               false,
               target -> this.canAttackTarget(target) && this.canAcquireCombatTarget(target),
               () -> this.isRangedCombatant() ? 64.0 : 4.0
            )
         );
   }

   @Override
   public void m_6710_(LivingEntity target) {
      if (target != null && this.isFollowCombatRecallActive()) {
         super.m_6710_(null);
      } else if (target != null && !DragonRiderController.canAcquireTarget(this, target)) {
         super.m_6710_(null);
      } else {
         super.m_6710_(target);
      }
   }

   protected InteractionResult m_6071_(Player player, InteractionHand hand) {
      if (this.tryAcceptFood(player, hand)) {
         return InteractionResult.m_19078_(this.m_9236_().f_46443_);
      } else if (player.m_21120_(hand).m_41720_() instanceof CommandBatonItem) {
         if (!this.m_9236_().f_46443_ && this.hasCommandPermission(player)) {
            CommandBatonItem.toggleNpcSelection(player.m_21120_(hand), player, this);
         }

         return InteractionResult.m_19078_(this.m_9236_().f_46443_);
      } else {
         this.openNpcInventory(player);
         return InteractionResult.m_19078_(this.m_9236_().f_46443_);
      }
   }

   public void openNpcInventory(Player player) {
      if (!this.m_9236_().f_46443_ && this.hasCommandPermission(player)) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider((containerId, inventory, openedPlayer) -> new NpcInventoryMenu(containerId, inventory, this), this.m_5446_()),
            buffer -> buffer.writeInt(this.m_19879_())
         );
      }
   }

   @Override
   public String getNpcType() {
      return "combat";
   }

   @Override
   public String getNpcSubtype() {
      return this.isSpellcasterCombatant() ? "mage" : CombatProfessionResolver.subtypeFor(this.m_21205_(), this.m_21206_());
   }

   @Override
   public Component m_5677_() {
      Component baseName;
      if (!this.isLogicalDragonRider() && (this.m_20202_() == null || !IceAndFireDragonCompat.isDragon(this.m_20202_()))) {
         if (this.m_20202_() != null && IceAndFireHippocampusCompat.isHippocampus(this.m_20202_())) {
            baseName = Component.m_237115_("entity.ancientwarfare.combat.hippocampus_rider");
         } else {
            baseName = Component.m_237115_("entity.ancientwarfare.combat." + this.getNpcSubtype());
         }
      } else {
         baseName = Component.m_237115_("entity.ancientwarfare.combat.dragon_rider");
      }

      return this.applyCombatRankName(baseName);
   }

   private boolean canAttackTarget(LivingEntity target) {
      return NpcTargeting.isAutomaticHostile(this, target);
   }

   private boolean isHostileFactionNpc(LivingEntity target) {
      if (target instanceof FactionNpcEntity factionNpc && NpcTargeting.canHarm(this, target) && factionNpc.isHostileTowards(this)) {
         return true;
      }

      return false;
   }

   private boolean isCommander() {
      return "commander".equals(this.getNpcSubtype());
   }

   private boolean isMedic() {
      return "medic".equals(this.getNpcSubtype());
   }

   private boolean isRangedCombatant() {
      return !this.isSpellcasterCombatant() && NpcEquipmentResolver.isRangedWeapon(this.m_21205_());
   }

   protected boolean isSpellcasterCombatant() {
      return false;
   }

   protected boolean canUseMeleeCombatGoal() {
      return !this.isSpellcasterCombatant() && !this.isRangedCombatant();
   }

   public boolean isSpellcastingAnimation() {
      return false;
   }

   private void ensureRangedFollowRange() {
      if (this.isRangedCombatant()) {
         AttributeInstance followRange = this.m_21051_(Attributes.f_22277_);
         if (followRange != null) {
            followRange.m_22100_(Math.max(followRange.m_22115_(), 48.0));
         }
      }
   }

   public boolean isSwordsman() {
      return "swordsman".equals(this.getNpcSubtype());
   }

   public int getSwordsmanComboStep() {
      return (Integer)this.f_19804_.m_135370_(SWORDSMAN_COMBO);
   }

   @Override
   public boolean m_7327_(Entity target) {
      if (this.isRangedCombatant()) {
         return false;
      } else if (this.isSwordsman()) {
         boolean started = this.startSwordsmanCombo(target);
         if (started) {
            this.recordCombatAttack();
         }

         return started;
      } else {
         this.triggerMeleeAttackAnimation();
         this.m_21011_(InteractionHand.MAIN_HAND, true);
         boolean hit = super.m_7327_(target);
         if (hit && target instanceof LivingEntity livingTarget) {
            IceAndFireEquipmentCompat.applySupportedMeleeHit(this.m_21205_(), livingTarget, this);
         }

         if (hit) {
            this.recordCombatAttack();
         }

         return hit;
      }
   }

   private boolean startSwordsmanCombo(Entity target) {
      if (this.swordsmanComboActive) {
         return true;
      } else if (this.swordsmanTechniqueRecoveryTicks > 0) {
         return false;
      } else {
         if (target instanceof LivingEntity livingTarget && livingTarget.m_6084_()) {
            this.swordsmanComboTarget = livingTarget;
            this.swordsmanComboTicks = 0;
            this.swordsmanComboActive = true;
            this.swordsmanAerialCleaveActive = false;
            this.swordsmanAerialCleaveTicks = 0;
            this.swordsmanTechniqueFinished = false;
            this.swordsmanRapidHitCooldown = 0;
            this.swordsmanRapidHitCount = 0;
            this.swordsmanChargedArt = 0;
            this.swordsmanChargeTicks = 0;
            this.swordsmanChargedArtReleased = false;
            this.swordsmanTechnique = this.chooseSwordsmanTechnique(livingTarget);
            if (this.swordsmanTechnique != CombatNpcEntity.SwordsmanTechnique.NONE && this.executeSwordsmanTechniqueTick(0)) {
               this.tryReleaseSwordsmanSummonedSword(livingTarget);
               this.swordsmanPreviousTechnique = this.swordsmanLastTechnique;
               this.swordsmanLastTechnique = this.swordsmanTechnique;
               return true;
            }

            this.cancelSwordsmanCombo();
            return false;
         }

         return false;
      }
   }

   private void tryReleaseSwordsmanSummonedSword(LivingEntity target) {
      if (SlashBladeCompat.canUseSummonedSwordArts(this)) {
         int art = this.chooseSwordsmanSummonedSwordArt(target);
         boolean released = SlashBladeCompat.performSummonedSwordArt(this, target, art);
         if (!released && art != 1) {
            art = 1;
            released = SlashBladeCompat.performSummonedSwordArt(this, target, art);
         }

         if (released) {
            this.swordsmanPreviousSummonedSwordArt = this.swordsmanLastSummonedSwordArt;
            this.swordsmanLastSummonedSwordArt = art;
         }
      }
   }

   private int chooseSwordsmanSummonedSwordArt(LivingEntity target) {
      double distance = Math.sqrt(this.m_20280_(target));
      List<Integer> candidates = new ArrayList<>();
      if (distance <= 5.5) {
         candidates.add(2);
         candidates.add(3);
         candidates.add(4);
         candidates.add(1);
      } else if (distance <= 14.0) {
         candidates.add(3);
         candidates.add(4);
         candidates.add(5);
         candidates.add(1);
      } else {
         candidates.add(4);
         candidates.add(5);
         candidates.add(1);
      }

      if (candidates.size() > 1) {
         candidates.removeIf(candidate -> candidate == this.swordsmanLastSummonedSwordArt);
      }

      int selected = candidates.get(this.m_217043_().m_188503_(candidates.size()));
      if (candidates.size() > 1 && selected == this.swordsmanPreviousSummonedSwordArt && this.m_217043_().m_188501_() < 0.7F) {
         List<Integer> alternatives = candidates.stream().filter(candidate -> candidate != this.swordsmanPreviousSummonedSwordArt).toList();
         if (!alternatives.isEmpty()) {
            selected = alternatives.get(this.m_217043_().m_188503_(alternatives.size()));
         }
      }

      return selected;
   }

   private CombatNpcEntity.SwordsmanTechnique chooseSwordsmanTechnique(LivingEntity target) {
      double distance = Math.sqrt(this.m_20280_(target));
      double heightDifference = target.m_20186_() - this.m_20186_();
      List<CombatNpcEntity.SwordsmanTechnique> candidates = new ArrayList<>();
      int heldArt = SlashBladeCompat.getHeldSlashArt(this);
      if (this.shouldUseSwordsmanSlashArt(target, heldArt, distance)) {
         this.swordsmanChargeType = this.chooseSwordsmanChargeType(distance);
         this.swordsmanChargeTicks = this.chargeTicksForType(this.swordsmanChargeType);
         return CombatNpcEntity.SwordsmanTechnique.SLASH_ART;
      } else {
         if (!this.m_20096_() && !this.m_20159_()) {
            if (heightDifference < -1.5 || distance > 5.0) {
               return CombatNpcEntity.SwordsmanTechnique.AERIAL_CLEAVE;
            }

            candidates.add(CombatNpcEntity.SwordsmanTechnique.AERIAL_COMBO_A);
            candidates.add(CombatNpcEntity.SwordsmanTechnique.AERIAL_COMBO_B);
         } else if (distance <= 3.6) {
            candidates.add(CombatNpcEntity.SwordsmanTechnique.COMBO_A);
            candidates.add(CombatNpcEntity.SwordsmanTechnique.COMBO_B);
            candidates.add(CombatNpcEntity.SwordsmanTechnique.COMBO_C);
            if (this.canLaunchSwordsmanAerialCombo(target)) {
               candidates.add(CombatNpcEntity.SwordsmanTechnique.AERIAL_COMBO_A);
               candidates.add(CombatNpcEntity.SwordsmanTechnique.AERIAL_COMBO_B);
               if (heightDifference > 1.25) {
                  candidates.add(CombatNpcEntity.SwordsmanTechnique.AERIAL_COMBO_A);
                  candidates.add(CombatNpcEntity.SwordsmanTechnique.AERIAL_COMBO_B);
               }
            } else if (!this.m_20159_()) {
               candidates.add(CombatNpcEntity.SwordsmanTechnique.UPPER_CHAIN);
            }

            if (!this.m_20159_() && distance >= 2.0) {
               candidates.add(CombatNpcEntity.SwordsmanTechnique.RAPID_SLASH);
            }
         } else {
            if (!this.m_20159_()) {
               candidates.add(CombatNpcEntity.SwordsmanTechnique.RAPID_SLASH);
            }

            if (this.canLaunchSwordsmanAerialCombo(target) && distance <= 4.5 && heightDifference > 1.25) {
               candidates.add(CombatNpcEntity.SwordsmanTechnique.AERIAL_COMBO_A);
               candidates.add(CombatNpcEntity.SwordsmanTechnique.AERIAL_COMBO_B);
            }
         }

         if (candidates.isEmpty()) {
            candidates.add(CombatNpcEntity.SwordsmanTechnique.COMBO_A);
         }

         if (candidates.size() > 1) {
            candidates.removeIf(candidate -> candidate == this.swordsmanLastTechnique);
         }

         CombatNpcEntity.SwordsmanTechnique selected = candidates.get(this.m_217043_().m_188503_(candidates.size()));
         if (candidates.size() > 1 && selected == this.swordsmanPreviousTechnique && this.m_217043_().m_188501_() < 0.65F) {
            List<CombatNpcEntity.SwordsmanTechnique> alternatives = candidates.stream()
               .filter(candidate -> candidate != this.swordsmanPreviousTechnique)
               .toList();
            if (!alternatives.isEmpty()) {
               selected = alternatives.get(this.m_217043_().m_188503_(alternatives.size()));
            }
         }

         return selected;
      }
   }

   private boolean isChargedArtSuitableAtDistance(int art, double distance) {
      return switch (art) {
         case 1 -> distance <= 28.0;
         case 2, 3, 4 -> distance <= 7.0;
         case 5, 6, 7 -> distance >= 4.0 && distance <= 32.0;
         case 8 -> distance >= 2.0 && distance <= 18.0;
         default -> false;
      };
   }

   private boolean shouldUseSwordsmanSlashArt(LivingEntity target, int heldArt, double distance) {
      if (!this.m_20096_()
         || this.m_20159_()
         || this.swordsmanSlashArtCooldown > 0
         || this.swordsmanSlashArtCharge < 4
         || heldArt == 0
         || !this.isChargedArtSuitableAtDistance(heldArt, distance)
         || target.m_21223_() <= Math.max(8.0F, target.m_21233_() * 0.2F)) {
         return false;
      } else if (this.swordsmanSlashArtCharge >= 7) {
         return true;
      } else {
         float chance = switch (this.swordsmanSlashArtCharge) {
            case 4 -> 0.22F;
            case 5 -> 0.38F;
            default -> 0.6F;
         };
         return this.m_217043_().m_188501_() < chance;
      }
   }

   private int chooseSwordsmanChargeType(double distance) {
      List<Integer> candidates = new ArrayList<>();
      candidates.add(1);
      candidates.add(2);
      if (SlashBladeCompat.canUseChargedSlashArt(this, 3) && distance >= 5.0 && this.m_217043_().m_188501_() < 0.22F) {
         candidates.add(3);
      }

      if (candidates.size() > 1) {
         candidates.removeIf(type -> type == this.swordsmanLastChargeType);
      }

      int selected = candidates.get(this.m_217043_().m_188503_(candidates.size()));
      if (candidates.size() > 1 && selected == this.swordsmanPreviousChargeType && this.m_217043_().m_188501_() < 0.7F) {
         List<Integer> alternatives = candidates.stream().filter(type -> type != this.swordsmanPreviousChargeType).toList();
         if (!alternatives.isEmpty()) {
            selected = alternatives.get(this.m_217043_().m_188503_(alternatives.size()));
         }
      }

      if (!SlashBladeCompat.canUseChargedSlashArt(this, selected)) {
         selected = 1;
      }

      return selected;
   }

   private int chargeTicksForType(int chargeType) {
      return switch (chargeType) {
         case 2 -> 10;
         case 3 -> 26;
         default -> 16;
      };
   }

   private boolean canLaunchSwordsmanAerialCombo(LivingEntity target) {
      if (!this.m_20159_() && this.m_20096_() && target != null && target.m_6084_()) {
         if (this.m_20280_(target) > 20.25) {
            return false;
         } else {
            AABB launchSpace = this.m_20191_().m_82377_(0.35, 0.0, 0.35).m_82363_(0.0, 3.75, 0.0);
            return this.m_9236_().m_45756_(this, launchSpace);
         }
      } else {
         return false;
      }
   }

   private boolean performSwordsmanTechniqueHit(int move, float damageRatio, int knockback) {
      if (this.swordsmanComboTarget != null
         && !(this.m_20280_(this.swordsmanComboTarget) > 27.5625)
         && SlashBladeCompat.performTechniqueHit(this, this.swordsmanComboTarget, move, damageRatio, knockback)) {
         this.swordsmanComboStep = move;
         this.swordsmanPoseTicks = 10;
         this.f_19804_.m_135381_(SWORDSMAN_COMBO, move);
         this.m_21011_(InteractionHand.MAIN_HAND, true);
         return true;
      } else {
         return false;
      }
   }

   private void setSwordsmanTechniquePose(int move) {
      if (SlashBladeCompat.setTechniqueState(this, move)) {
         this.swordsmanComboStep = move;
         this.swordsmanPoseTicks = 10;
         this.f_19804_.m_135381_(SWORDSMAN_COMBO, move);
      }
   }

   private boolean executeSwordsmanTechniqueTick(int tick) {
      return switch (this.swordsmanTechnique) {
         case COMBO_A -> this.executeComboATick(tick);
         case COMBO_B -> this.executeComboBTick(tick);
         case COMBO_C -> this.executeComboCTick(tick);
         case RAPID_SLASH -> this.executeRapidSlashTick(tick);
         case UPPER_CHAIN -> this.executeUpperChainTick(tick);
         case AERIAL_COMBO_A -> this.executeAerialComboATick(tick);
         case AERIAL_COMBO_B -> this.executeAerialComboBTick(tick);
         case AERIAL_CLEAVE -> this.executeAerialCleaveOnlyTick(tick);
         case SLASH_ART -> this.executeChargedSlashArtTick(tick);
         default -> false;
      };
   }

   private boolean executeChargedSlashArtTick(int tick) {
      if (this.swordsmanComboTarget != null && !this.m_20159_()) {
         this.m_21573_().m_26573_();
         this.m_21563_().m_24960_(this.swordsmanComboTarget, 90.0F, 90.0F);
         this.faceSwordsmanTarget();
         if (!this.swordsmanChargedArtReleased) {
            this.m_20256_(this.m_20184_().m_82542_(0.05, 1.0, 0.05));
            if (tick == 0 || tick % 4 == 0) {
               this.setSwordsmanTechniquePose(19);
               SlashBladeCompat.emitChargedSlashArtParticles(this, this.swordsmanChargeType);
            }

            if (tick < this.swordsmanChargeTicks) {
               return true;
            } else {
               this.swordsmanChargedArt = SlashBladeCompat.beginChargedSlashArt(this, this.swordsmanComboTarget, this.swordsmanChargeType);
               if (this.swordsmanChargedArt == 0) {
                  return false;
               } else {
                  this.swordsmanChargedArtReleased = true;
                  this.swordsmanSlashArtCooldown = 600;
                  this.swordsmanSlashArtCharge = 0;
                  this.swordsmanPreviousChargeType = this.swordsmanLastChargeType;
                  this.swordsmanLastChargeType = this.swordsmanChargeType;
                  this.setSwordsmanTechniquePose(SlashBladeCompat.getSlashArtPose(this.swordsmanChargedArt));
                  return true;
               }
            }
         } else {
            int executionTick = tick - this.swordsmanChargeTicks;
            if (SlashBladeCompat.isPiercingSlashArt(this.swordsmanChargedArt) && executionTick <= 7) {
               this.dashSwordsmanTowardsTarget(this.m_20280_(this.swordsmanComboTarget));
            } else {
               this.m_20256_(this.m_20184_().m_82542_(0.25, 1.0, 0.25));
            }

            if (executionTick % 5 == 0) {
               this.setSwordsmanTechniquePose(SlashBladeCompat.getSlashArtPose(this.swordsmanChargedArt));
            }

            if (!SlashBladeCompat.tickChargedSlashArt(this, this.swordsmanComboTarget, this.swordsmanChargedArt, this.swordsmanChargeType, executionTick)) {
               return false;
            } else {
               if (executionTick >= SlashBladeCompat.getChargedSlashArtExecutionTicks(this.swordsmanChargedArt, this.swordsmanChargeType)) {
                  this.swordsmanTechniqueFinished = true;
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }

   private void faceSwordsmanTarget() {
      if (this.swordsmanComboTarget != null) {
         Vec3 delta = this.swordsmanComboTarget.m_20182_().m_82546_(this.m_20182_());
         if (!(delta.m_165925_() < 1.0E-6)) {
            float yaw = (float)Math.toDegrees(Math.atan2(-delta.f_82479_, delta.f_82481_));
            this.m_146922_(yaw);
            this.m_5618_(yaw);
            this.m_5616_(yaw);
         }
      }
   }

   private boolean executeComboATick(int tick) {
      boolean powered = SlashBladeCompat.isPowered(this);

      return switch (tick) {
         case 0 -> this.performSwordsmanTechniqueHit(1, 0.44F, 0);
         case 6 -> this.performSwordsmanTechniqueHit(2, 0.44F, 0);
         case 13, 16 -> this.performSwordsmanTechniqueHit(3, 0.44F, 0);
         case 22, 24 -> this.performSwordsmanTechniqueHit(powered ? 5 : 4, powered ? 1.0F : 0.44F, 0);
         case 31, 33, 35 -> !powered || this.performSwordsmanTechniqueHit(6, 1.0F, 0);
         default -> true;
      };
   }

   private boolean executeComboBTick(int tick) {
      return switch (tick) {
         case 0 -> this.performSwordsmanTechniqueHit(1, 0.44F, 0);
         default -> true;
         case 6 -> this.performSwordsmanTechniqueHit(2, 0.44F, 0);
         case 13, 16 -> this.performSwordsmanTechniqueHit(3, 0.44F, 0);
         case 24, 26, 28, 30, 32, 34, 36, 38 -> this.performSwordsmanTechniqueHit(7, 0.244F, 0);
         case 44, 46 -> this.performSwordsmanTechniqueHit(7, 0.5F, 1);
      };
   }

   private boolean executeComboCTick(int tick) {
      return switch (tick) {
         case 0 -> this.performSwordsmanTechniqueHit(1, 0.44F, 0);
         case 6 -> this.performSwordsmanTechniqueHit(2, 0.44F, 0);
         case 18, 20 -> this.performSwordsmanTechniqueHit(8, 0.88F, 0);
         default -> true;
      };
   }

   private boolean executeRapidSlashTick(int tick) {
      if (tick == 0) {
         this.setSwordsmanTechniquePose(9);
         return true;
      } else if (this.swordsmanComboTarget != null && !this.m_20159_()) {
         double distanceSqr = this.m_20280_(this.swordsmanComboTarget);
         if (distanceSqr > 18.0625) {
            this.dashSwordsmanTowardsTarget(distanceSqr);
            if (tick % 7 == 1) {
               this.setSwordsmanTechniquePose(9);
            }

            return true;
         } else {
            this.m_21573_().m_26573_();
            this.m_20256_(this.m_20184_().m_82542_(0.35, 1.0, 0.35));
            if (this.swordsmanRapidHitCooldown > 0) {
               this.swordsmanRapidHitCooldown--;
            } else if (this.swordsmanRapidHitCount < 4) {
               if (!this.performSwordsmanTechniqueHit(9, 0.44F, 0)) {
                  return false;
               }

               this.swordsmanRapidHitCount++;
               this.swordsmanRapidHitCooldown = 2;
            }

            if (this.swordsmanRapidHitCount >= 4 && this.swordsmanRapidHitCooldown <= 0) {
               this.swordsmanTechniqueFinished = true;
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private void dashSwordsmanTowardsTarget(double distanceSqr) {
      if (this.swordsmanComboTarget != null && !this.m_20159_()) {
         this.m_21573_().m_26573_();
         Vec3 direction = this.horizontalDirectionToSwordsmanTarget();
         if (!(direction.m_82556_() < 1.0E-4)) {
            double distance = Math.sqrt(distanceSqr);
            double speed = distance > 16.0 ? 1.1 : (distance > 8.0 ? 0.92 : 0.72);
            Vec3 motion = this.m_20184_();
            this.m_20334_(direction.f_82479_ * speed, motion.f_82480_, direction.f_82481_ * speed);
            float yaw = (float)Math.toDegrees(Math.atan2(-direction.f_82479_, direction.f_82481_));
            this.m_146922_(yaw);
            this.m_5618_(yaw);
            this.m_5616_(yaw);
            this.m_6858_(true);
            this.f_19812_ = true;
            if (this.f_19862_ && this.m_20096_()) {
               this.m_21569_().m_24901_();
            }
         }
      }
   }

   private boolean executeUpperChainTick(int tick) {
      if (tick == 0) {
         return this.performSwordsmanTechniqueHit(10, 0.5F, 1);
      } else {
         if (tick == 5) {
            this.setSwordsmanTechniquePose(11);
            this.m_20334_(this.m_20184_().f_82479_, 0.6, this.m_20184_().f_82481_);
            this.m_6853_(false);
            this.f_19812_ = true;
         } else {
            if (tick == 8) {
               return this.performSwordsmanTechniqueHit(12, 0.6F, 1);
            }

            if (tick == 14) {
               return this.performSwordsmanTechniqueHit(12, 0.6F, 1);
            }
         }

         return true;
      }
   }

   private boolean executeAerialComboATick(int tick) {
      if (tick == 0) {
         if (!this.performSwordsmanTechniqueHit(10, 0.5F, 1)) {
            return false;
         } else {
            this.launchSwordsmanAerialCombo();
            this.setSwordsmanTechniquePose(11);
            return true;
         }
      } else {
         if (this.shouldBeginSwordsmanAerialCleave(tick, 34)) {
            this.beginSwordsmanAerialCleave();
         }

         if (this.swordsmanAerialCleaveActive) {
            return this.tickSwordsmanAerialCleave();
         } else {
            this.hoverSwordsmanNearTarget();

            return switch (tick) {
               case 5 -> this.performSwordsmanTechniqueHit(13, 0.28F, 0);
               case 11 -> this.performSwordsmanTechniqueHit(14, 0.28F, 0);
               case 18, 20 -> this.performSwordsmanTechniqueHit(15, 0.28F, 2);
               default -> true;
            };
         }
      }
   }

   private boolean executeAerialComboBTick(int tick) {
      if (tick == 0) {
         if (!this.performSwordsmanTechniqueHit(10, 0.5F, 1)) {
            return false;
         } else {
            this.launchSwordsmanAerialCombo();
            this.setSwordsmanTechniquePose(11);
            return true;
         }
      } else {
         if (this.shouldBeginSwordsmanAerialCleave(tick, 40)) {
            this.beginSwordsmanAerialCleave();
         }

         if (this.swordsmanAerialCleaveActive) {
            return this.tickSwordsmanAerialCleave();
         } else {
            this.hoverSwordsmanNearTarget();

            return switch (tick) {
               case 5 -> this.performSwordsmanTechniqueHit(13, 0.28F, 0);
               case 11 -> this.performSwordsmanTechniqueHit(14, 0.28F, 0);
               case 19, 24 -> this.performSwordsmanTechniqueHit(16, 0.34F, 1);
               case 31, 33 -> this.performSwordsmanTechniqueHit(17, 0.34F, 3);
               default -> true;
            };
         }
      }
   }

   private boolean executeAerialCleaveOnlyTick(int tick) {
      if (!this.swordsmanAerialCleaveActive) {
         this.beginSwordsmanAerialCleave();
      }

      return this.tickSwordsmanAerialCleave();
   }

   private void launchSwordsmanAerialCombo() {
      if (!this.m_20159_()) {
         this.m_21573_().m_26573_();
         Vec3 motion = this.m_20184_();
         Vec3 towardTarget = this.horizontalDirectionToSwordsmanTarget();
         this.m_20334_(towardTarget.f_82479_ * 0.22, Math.max(0.72, motion.f_82480_), towardTarget.f_82481_ * 0.22);
         this.m_6853_(false);
         this.f_19812_ = true;
      }
   }

   private void hoverSwordsmanNearTarget() {
      if (this.swordsmanComboTarget != null && !this.m_20159_()) {
         this.m_21573_().m_26573_();
         Vec3 delta = this.swordsmanComboTarget.m_20182_().m_82520_(0.0, 1.0, 0.0).m_82546_(this.m_20182_());
         double horizontalDistance = Math.sqrt(delta.f_82479_ * delta.f_82479_ + delta.f_82481_ * delta.f_82481_);
         double x = Mth.m_14008_(delta.f_82479_ * 0.1, -0.24, 0.24);
         double z = Mth.m_14008_(delta.f_82481_ * 0.1, -0.24, 0.24);
         double y = Mth.m_14008_(delta.f_82480_ * 0.08, -0.08, 0.16);
         if (horizontalDistance < 1.2) {
            x *= 0.35;
            z *= 0.35;
         }

         this.m_20334_(x, y, z);
         this.f_19789_ = 0.0F;
         this.m_6853_(false);
         this.f_19812_ = true;
      }
   }

   private boolean shouldBeginSwordsmanAerialCleave(int tick, int maximumAirTicks) {
      if (this.swordsmanAerialCleaveActive || this.m_20159_()) {
         return false;
      } else if (tick < maximumAirTicks && this.swordsmanComboTarget != null) {
         double horizontalDistanceSqr = this.horizontalDistanceSqr(this.swordsmanComboTarget);
         double targetBelow = this.m_20186_() - this.swordsmanComboTarget.m_20186_();
         return tick >= 16 && (targetBelow > 2.6 || horizontalDistanceSqr > 30.25 || !this.swordsmanComboTarget.m_6084_());
      } else {
         return true;
      }
   }

   private void beginSwordsmanAerialCleave() {
      this.swordsmanAerialCleaveActive = true;
      this.swordsmanAerialCleaveTicks = 0;
      this.setSwordsmanTechniquePose(18);
      if (!this.m_20159_()) {
         this.m_21573_().m_26573_();
         Vec3 motion = this.m_20184_();
         this.m_20334_(motion.f_82479_ * 0.35, Math.min(-0.35, motion.f_82480_), motion.f_82481_ * 0.35);
         this.m_6853_(false);
         this.f_19812_ = true;
      }
   }

   private boolean tickSwordsmanAerialCleave() {
      if (this.m_20159_()) {
         return false;
      } else {
         this.swordsmanAerialCleaveTicks++;
         this.f_19789_ = 0.0F;
         Vec3 towardTarget = this.horizontalDirectionToSwordsmanTarget();
         Vec3 motion = this.m_20184_();
         double downwardSpeed = Math.max(-1.65, motion.f_82480_ - 0.24);
         this.m_20334_(towardTarget.f_82479_ * 0.18, downwardSpeed, towardTarget.f_82481_ * 0.18);
         this.f_19812_ = true;
         if (this.swordsmanAerialCleaveTicks % 4 == 0
            && this.swordsmanComboTarget != null
            && this.m_20280_(this.swordsmanComboTarget) <= 22.5625
            && !this.performSwordsmanTechniqueHit(18, 0.44F, 3)) {
            return false;
         } else {
            if (this.m_20096_()) {
               if (this.swordsmanComboTarget != null && this.m_20280_(this.swordsmanComboTarget) <= 27.5625) {
                  this.performSwordsmanTechniqueHit(18, 0.44F, 3);
               }

               this.swordsmanTechniqueFinished = true;
            } else if (this.swordsmanAerialCleaveTicks >= 45) {
               this.swordsmanTechniqueFinished = true;
            }

            return true;
         }
      }
   }

   private Vec3 horizontalDirectionToSwordsmanTarget() {
      if (this.swordsmanComboTarget == null) {
         return Vec3.f_82478_;
      } else {
         Vec3 delta = this.swordsmanComboTarget.m_20182_().m_82546_(this.m_20182_());
         Vec3 horizontal = new Vec3(delta.f_82479_, 0.0, delta.f_82481_);
         return horizontal.m_82556_() < 1.0E-4 ? Vec3.f_82478_ : horizontal.m_82541_();
      }
   }

   private double horizontalDistanceSqr(LivingEntity target) {
      double x = target.m_20185_() - this.m_20185_();
      double z = target.m_20189_() - this.m_20189_();
      return x * x + z * z;
   }

   private void driveSwordsmanTowardsTarget(double speed, double stopDistance) {
      if (this.swordsmanComboTarget != null && !this.m_20159_()) {
         if (this.m_20280_(this.swordsmanComboTarget) > stopDistance * stopDistance) {
            this.m_21573_().m_5624_(this.swordsmanComboTarget, speed);
         } else {
            this.m_21573_().m_26573_();
         }
      }
   }

   private void tickSwordsmanCombo() {
      if (!this.m_9236_().f_46443_) {
         if (this.swordsmanTechniqueRecoveryTicks > 0) {
            this.swordsmanTechniqueRecoveryTicks--;
         }

         if (this.swordsmanSlashArtCooldown > 0) {
            this.swordsmanSlashArtCooldown--;
         }

         if (this.swordsmanComboActive) {
            if (!this.isSwordsman()) {
               this.cancelSwordsmanCombo();
            } else {
               if (!this.isValidSwordsmanComboTarget()) {
                  if (!this.swordsmanTechnique.aerial && !this.swordsmanAerialCleaveActive || this.m_20159_()) {
                     this.cancelSwordsmanCombo();
                     return;
                  }

                  this.swordsmanComboTarget = null;
                  this.swordsmanTechnique = CombatNpcEntity.SwordsmanTechnique.AERIAL_CLEAVE;
                  if (!this.swordsmanAerialCleaveActive) {
                     this.beginSwordsmanAerialCleave();
                  }
               }

               LivingEntity target = this.swordsmanComboTarget;
               if (target != null) {
                  this.m_21563_().m_24960_(target, 60.0F, 60.0F);
               }

               if (this.swordsmanTechnique.aerial
                  || this.swordsmanTechnique == CombatNpcEntity.SwordsmanTechnique.RAPID_SLASH
                  || this.swordsmanTechnique == CombatNpcEntity.SwordsmanTechnique.SLASH_ART
                  || this.swordsmanTechnique == CombatNpcEntity.SwordsmanTechnique.UPPER_CHAIN && this.swordsmanComboTicks >= 5) {
                  if ((this.swordsmanTechnique.aerial || this.swordsmanTechnique == CombatNpcEntity.SwordsmanTechnique.UPPER_CHAIN) && !this.m_20159_()) {
                     this.m_21573_().m_26573_();
                  }
               } else {
                  this.driveSwordsmanTowardsTarget(0.9, 2.75);
               }

               this.swordsmanComboTicks++;
               if (!this.executeSwordsmanTechniqueTick(this.swordsmanComboTicks)) {
                  this.cancelSwordsmanCombo();
               } else if (this.swordsmanTechniqueFinished || this.swordsmanComboTicks >= this.swordsmanTechnique.durationTicks) {
                  this.finishSwordsmanTechnique();
               }
            }
         }
      }
   }

   private boolean isValidSwordsmanComboTarget() {
      return this.swordsmanComboTarget != null
         && this.swordsmanComboTarget.m_6084_()
         && this.swordsmanComboTarget.m_9236_() == this.m_9236_()
         && this.m_20280_(this.swordsmanComboTarget) <= 1600.0;
   }

   private void cancelSwordsmanCombo() {
      this.swordsmanComboActive = false;
      this.swordsmanComboTarget = null;
      this.swordsmanComboTicks = 0;
      this.swordsmanComboStep = 0;
      this.swordsmanPoseTicks = 0;
      this.swordsmanTechnique = CombatNpcEntity.SwordsmanTechnique.NONE;
      this.swordsmanAerialCleaveActive = false;
      this.swordsmanAerialCleaveTicks = 0;
      this.swordsmanTechniqueFinished = false;
      this.swordsmanRapidHitCooldown = 0;
      this.swordsmanRapidHitCount = 0;
      this.swordsmanChargedArt = 0;
      this.swordsmanChargeTicks = 0;
      this.swordsmanChargedArtReleased = false;
      this.m_6858_(false);
      if (!this.m_9236_().f_46443_ && (Integer)this.f_19804_.m_135370_(SWORDSMAN_COMBO) != 0) {
         this.f_19804_.m_135381_(SWORDSMAN_COMBO, 0);
      }
   }

   private boolean isSwordsmanComboActive() {
      return this.swordsmanComboActive;
   }

   private int getSwordsmanTechniqueCooldownTicks() {
      return 4;
   }

   private void finishSwordsmanTechnique() {
      if (this.swordsmanTechnique != CombatNpcEntity.SwordsmanTechnique.SLASH_ART) {
         this.swordsmanSlashArtCharge = Math.min(7, this.swordsmanSlashArtCharge + 1);
      }

      this.swordsmanTechniqueRecoveryTicks = 8;
      this.cancelSwordsmanCombo();
   }

   private boolean isSwordsmanAerialRecovery() {
      return this.swordsmanComboActive && (this.swordsmanTechnique.aerial || this.swordsmanAerialCleaveActive);
   }

   public void m_6504_(LivingEntity target, float velocity) {
      if (target != null && target.m_6084_()) {
         this.recordCombatAttack();
         if (NpcEquipmentResolver.isCrossbow(this.m_21205_())) {
            this.performCrossbowAttack(target);
         } else {
            ItemStack projectileStack = this.m_6298_(this.m_21205_());
            AbstractArrow arrow = ProjectileUtil.m_37300_(this, projectileStack, velocity);
            this.positionDragonRiderArrow(arrow, target);
            float projectileSpeed = this.calculateProjectileVelocity(target);
            Vec3 start = new Vec3(this.m_20185_(), arrow.m_20186_(), this.m_20189_());
            NpcRangedAttackHelper.BallisticShot shot = NpcRangedAttackHelper.findBallisticShot(this, target, start, (double)projectileSpeed, 0.05)
               .orElseGet(() -> this.fallbackRangedShot(target, start, projectileSpeed));
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
         this.positionDragonRiderArrow(arrow, target);
         NpcRangedAttackHelper.configureCrossbowArrow(this, arrow, crossbow);
         arrow.m_6686_(shotDirection.f_82479_, shotDirection.f_82480_, shotDirection.f_82481_, 3.15F, (float)(8 - this.m_9236_().m_46791_().m_19028_() * 2));
         this.m_9236_().m_7967_(arrow);
      }

      this.m_5496_(SoundEvents.f_11847_, 1.0F, 1.0F / (this.m_217043_().m_188501_() * 0.4F + 0.8F));
   }

   private NpcRangedAttackHelper.BallisticShot fallbackRangedShot(LivingEntity target, Vec3 start, float speed) {
      Vec3 aim = NpcRangedAttackHelper.predictTargetPosition(this, target, (double)speed);
      double horizontal = Math.sqrt(Math.pow(aim.f_82479_ - start.f_82479_, 2.0) + Math.pow(aim.f_82481_ - start.f_82481_, 2.0));
      return new NpcRangedAttackHelper.BallisticShot(aim.m_82546_(start).m_82520_(0.0, horizontal * 0.2, 0.0).m_82541_(), speed);
   }

   private boolean hasBallisticRangedShot(LivingEntity target) {
      float speed = NpcEquipmentResolver.isCrossbow(this.m_21205_()) ? 3.15F : this.calculateProjectileVelocity(target);
      return NpcRangedAttackHelper.findBallisticShot(this, target, this.m_146892_(), (double)speed, 0.05).isPresent();
   }

   private boolean canAcquireCombatTarget(LivingEntity target) {
      return !DragonRiderController.canAcquireTarget(this, target) ? false : !this.isRangedCombatant() || this.hasBallisticRangedShot(target);
   }

   private void positionDragonRiderArrow(AbstractArrow arrow, LivingEntity target) {
      if (this.isLogicalDragonRider()) {
         Vec3 direction = this.targetAimDirection(target);
         Vec3 origin = this.m_20182_().m_82520_(0.0, (double)this.m_20206_() * 0.55, 0.0).m_82549_(direction.m_82490_(1.35));
         arrow.m_6034_(origin.f_82479_, origin.f_82480_, origin.f_82481_);
      }
   }

   private Vec3 targetAimDirection(LivingEntity target) {
      Vec3 aim = NpcRangedAttackHelper.predictTargetPosition(this, target, 3.15);
      return aim.m_82546_(this.m_20182_().m_82520_(0.0, (double)this.m_20206_() * 0.55, 0.0)).m_82541_();
   }

   private float calculateProjectileVelocity(LivingEntity target) {
      double dx = target.m_20185_() - this.m_20185_();
      double dz = target.m_20189_() - this.m_20189_();
      double distance = Math.sqrt(dx * dx + dz * dz);
      double height = target.m_20227_(0.35) - this.m_20227_(0.9);
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

   @Override
   public void m_7380_(CompoundTag tag) {
      super.m_7380_(tag);
      if (!this.legacySpellContainerStack.m_41619_()) {
         tag.m_128365_("SpellbookStack", this.legacySpellContainerStack.m_41739_(new CompoundTag()));
      }

      tag.m_128405_("PatrolIndex", this.patrolIndex);
      tag.m_128379_("PatrolAtPoint", this.patrolAtPoint);
      tag.m_128405_("PatrolTicksAtPoint", this.patrolTicksAtPoint);
      tag.m_128405_("SwordsmanSlashArtCooldown", this.swordsmanSlashArtCooldown);
      tag.m_128405_("SwordsmanSlashArtCharge", this.swordsmanSlashArtCharge);
   }

   @Override
   public void m_7378_(CompoundTag tag) {
      super.m_7378_(tag);
      this.legacySpellContainerStack = tag.m_128425_("SpellbookStack", 10) ? ItemStack.m_41712_(tag.m_128469_("SpellbookStack")) : ItemStack.f_41583_;
      this.patrolIndex = tag.m_128451_("PatrolIndex");
      this.patrolAtPoint = tag.m_128471_("PatrolAtPoint");
      this.patrolTicksAtPoint = tag.m_128451_("PatrolTicksAtPoint");
      this.swordsmanSlashArtCooldown = tag.m_128441_("SwordsmanSlashArtCooldown") ? Math.max(0, tag.m_128451_("SwordsmanSlashArtCooldown")) : 200;
      this.swordsmanSlashArtCharge = tag.m_128441_("SwordsmanSlashArtCharge") ? Mth.m_14045_(tag.m_128451_("SwordsmanSlashArtCharge"), 0, 7) : 0;
   }

   @Override
   protected boolean shouldSkipIdleMountedStop() {
      return this.patrolDrivingMount;
   }

   @Override
   protected boolean usesCustomMountedMovement() {
      return this.m_20202_() instanceof Mob;
   }

   @Override
   public void m_8107_() {
      super.m_8107_();
      if (!this.m_9236_().f_46443_ && !this.legacySpellContainerStack.m_41619_()) {
         this.migrateLegacySpellContainer();
      }

      this.ensureRangedFollowRange();
      SlashBladeCompat.preserveNpcBladeDurability(this);
      this.tickSwordsmanCombo();
      this.tickSwordsmanPose();
      SlashBladeCompat.preserveNpcBladeDurability(this);
      if (!this.m_9236_().f_46443_ && this.getCurrentCommand() == NpcCommandType.ATTACK && (this.m_5448_() == null || !this.m_5448_().m_6084_())) {
         this.clearCommand();
      }

      if (this.isLogicalDragonRider() || this.m_20202_() != null && IceAndFireDragonCompat.isDragon(this.m_20202_())) {
         DragonRiderController.tick(this);
      }

      if (this.m_20202_() != null && IceAndFireHippogryphCompat.isHippogryph(this.m_20202_())) {
         HippogryphRiderController.tick(this);
      }

      if (this.m_20202_() != null && IceAndFireHippocampusCompat.isHippocampus(this.m_20202_())) {
         HippocampusRiderController.tick(this);
      }

      if (this.isCommander()) {
         CombatGroupController.tick(this);
      }

      this.tickOwnedCavalryCharge();
      this.tickOwnedMountedArcherOrbit();
   }

   private void migrateLegacySpellContainer() {
      if (!this.legacySpellContainerStack.m_41619_()) {
         if (this.m_21205_().m_41619_()) {
            this.m_8061_(EquipmentSlot.MAINHAND, this.legacySpellContainerStack);
            this.legacySpellContainerStack = ItemStack.f_41583_;
         } else if (this.m_21206_().m_41619_()) {
            this.m_8061_(EquipmentSlot.OFFHAND, this.legacySpellContainerStack);
            this.legacySpellContainerStack = ItemStack.f_41583_;
         } else if (!this.m_9236_().f_46443_) {
            this.m_19983_(this.legacySpellContainerStack);
            this.legacySpellContainerStack = ItemStack.f_41583_;
         }
      }
   }

   private void tickSwordsmanPose() {
      if (!this.m_9236_().f_46443_) {
         if (!this.isSwordsman()) {
            this.cancelSwordsmanCombo();
         } else {
            if (this.swordsmanPoseTicks > 0 && --this.swordsmanPoseTicks == 0) {
               this.f_19804_.m_135381_(SWORDSMAN_COMBO, 0);
            }
         }
      }
   }

   private void tickOwnedCavalryCharge() {
      if (this.m_20202_() == null || !IceAndFireHippocampusCompat.isHippocampus(this.m_20202_()) && !IceAndFireHippogryphCompat.isHippogryph(this.m_20202_())) {
         if (!this.isRangedCombatant()) {
            if (this.m_20202_() instanceof Mob mount) {
               if (!NpcWaterEscape.isRecoveryActive(this)) {
                  LivingEntity target = this.m_5448_();
                  if (this.getCurrentCommand() != NpcCommandType.MOVE || !this.updateMountedMoveMovement()) {
                     if (this.isFollowCombatRecallActive() && this.updateMountedFollowMovement()) {
                        this.stopMountedCharge(mount);
                        this.mountPassDestination = null;
                        this.mountAttackedThisCharge = false;
                     } else if (this.validMountedTarget(target) || !this.shouldFollowCombatGroupFormation()) {
                        if (this.validMountedTarget(target) || !this.updateMountedFollowMovement()) {
                           if (this.usesCustomMountedMovement() && this.validMountedTarget(target)) {
                              if (this.mountAttackCooldown > 0) {
                                 this.mountAttackCooldown--;
                              }

                              this.m_21563_().m_24960_(target, 30.0F, 30.0F);
                              if (this.mountPassDestination != null) {
                                 Vec3 destination = this.mountPassDestination;
                                 this.mountPassTicks--;
                                 this.driveMountedCharge(mount, destination);
                                 if (this.mountPassDestination == null) {
                                    this.mountAttackedThisCharge = false;
                                 } else {
                                    if (this.mountPassTicks <= 0 || mount.m_20182_().m_82557_(destination) < 3.0) {
                                       this.mountPassDestination = null;
                                       this.mountAttackedThisCharge = false;
                                    }
                                 }
                              } else {
                                 double attackDistance = Math.max(3.2, NpcEquipmentResolver.meleeAttackReach(this));
                                 if (mount.m_20280_(target) > attackDistance * attackDistance) {
                                    this.mountAttackedThisCharge = false;
                                    this.driveMountedCharge(mount, target.m_20182_());
                                 } else {
                                    if (!this.mountAttackedThisCharge && this.mountAttackCooldown <= 0) {
                                       this.m_6674_(InteractionHand.MAIN_HAND);
                                       this.m_7327_(target);
                                       this.mountAttackedThisCharge = true;
                                       this.mountAttackCooldown = NpcEquipmentResolver.meleeAttackInterval(this);
                                       Vec3 direction = target.m_20182_().m_82546_(mount.m_20182_());
                                       Vec3 horizontal = new Vec3(direction.f_82479_, 0.0, direction.f_82481_);
                                       horizontal = horizontal.m_82556_() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : horizontal.m_82541_();
                                       this.mountPassDestination = target.m_20182_().m_82549_(horizontal.m_82490_(10.0));
                                       this.mountPassTicks = 28;
                                    }
                                 }
                              }
                           } else {
                              this.stopMountedCharge(mount);
                              this.mountPassDestination = null;
                              this.mountAttackedThisCharge = false;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void tickOwnedMountedArcherOrbit() {
      if (this.m_20202_() == null || !IceAndFireHippocampusCompat.isHippocampus(this.m_20202_()) && !IceAndFireHippogryphCompat.isHippogryph(this.m_20202_())) {
         if (!this.isRangedCombatant() || !(this.m_20202_() instanceof Mob mount)) {
            this.mountedArcherTarget = null;
         } else if (NpcWaterEscape.isRecoveryActive(this)) {
            this.mountedArcherTarget = null;
         } else {
            LivingEntity target = this.m_5448_();
            if (this.getCurrentCommand() == NpcCommandType.MOVE && this.updateMountedMoveMovement()) {
               this.mountedArcherTarget = null;
            } else if (this.isFollowCombatRecallActive() && this.updateMountedFollowMovement()) {
               this.mountedArcherTarget = null;
            } else if (this.validMountedTarget(target) || !this.shouldFollowCombatGroupFormation()) {
               if (!this.validMountedTarget(target) && this.updateMountedFollowMovement()) {
                  this.mountedArcherTarget = null;
               } else if (!this.validMountedTarget(target)) {
                  this.mountedArcherTarget = null;
                  mount.m_21573_().m_26573_();
                  mount.m_20256_(mount.m_20184_().m_82542_(0.45, 1.0, 0.45));
               } else {
                  if (this.mountedArcherTarget != target) {
                     this.mountedArcherTarget = target;
                     this.mountedArcherClockwise = this.m_217043_().m_188499_();
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

                  double correction = (14.0 - distance) * 0.12;
                  if (distance < 10.0) {
                     correction = 1.0;
                  }

                  if (distance > 18.0) {
                     correction = -1.0;
                  }

                  Vec3 direction = this.mountedOrbitDirection(radial, correction, this.mountedArcherClockwise);
                  if (!NpcMovementSafety.canContinueMountedOrbit(mount, direction)) {
                     this.mountedArcherClockwise = !this.mountedArcherClockwise;
                     direction = this.mountedOrbitDirection(radial, correction, this.mountedArcherClockwise);
                     if (!NpcMovementSafety.canContinueMountedOrbit(mount, direction)) {
                        this.stopMountedCharge(mount);
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
                  this.faceMountedDirection(mount, direction);
                  this.faceRiderAtTarget(target);
               }
            }
         }
      }
   }

   private Vec3 mountedOrbitDirection(Vec3 radial, double correction, boolean clockwise) {
      Vec3 tangent = clockwise ? new Vec3(-radial.f_82481_, 0.0, radial.f_82479_) : new Vec3(radial.f_82481_, 0.0, -radial.f_82479_);
      return tangent.m_82549_(radial.m_82490_(correction)).m_82541_();
   }

   private void faceMountedDirection(Mob mount, Vec3 direction) {
      float yaw = (float)(Math.atan2(direction.f_82481_, direction.f_82479_) * 180.0F / (float)Math.PI) - 90.0F;
      mount.m_146922_(yaw);
      mount.m_5618_(yaw);
      mount.m_5616_(yaw);
   }

   private void faceRiderAtTarget(LivingEntity target) {
      Vec3 direction = target.m_20182_().m_82546_(this.m_20182_());
      Vec3 horizontal = new Vec3(direction.f_82479_, 0.0, direction.f_82481_);
      if (!(horizontal.m_82556_() < 1.0E-4)) {
         float yaw = (float)(Math.atan2(horizontal.f_82481_, horizontal.f_82479_) * 180.0F / (float)Math.PI) - 90.0F;
         this.m_146922_(yaw);
         this.m_5618_(yaw);
         this.m_5616_(yaw);
      }
   }

   private boolean validMountedTarget(LivingEntity target) {
      return NpcTargeting.canHarm(this, target);
   }

   private void driveMountedCharge(Mob mount, Vec3 destination) {
      Vec3 delta = destination.m_82546_(mount.m_20182_());
      Vec3 horizontal = new Vec3(delta.f_82479_, 0.0, delta.f_82481_);
      if (!(horizontal.m_82556_() < 1.0E-4)) {
         Vec3 direction = horizontal.m_82541_();
         if (!NpcMovementSafety.canMoveMountedSafely(mount, direction)) {
            this.stopMountedCharge(mount);
            this.mountPassDestination = null;
         } else {
            AttributeInstance movement = mount.m_21051_(Attributes.f_22279_);
            if (movement != null) {
               movement.m_22100_(Math.max(movement.m_22115_(), 0.45));
            }

            Vec3 current = mount.m_20184_();
            mount.m_20334_(direction.f_82479_ * 0.45, current.f_82480_, direction.f_82481_ * 0.45);
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

   private void stopMountedCharge(Mob mount) {
      mount.m_21573_().m_26573_();
      mount.m_20256_(mount.m_20184_().m_82542_(0.45, 1.0, 0.45));
   }

   boolean hasActivePatrolDeployment() {
      return this.getCurrentCommand() == NpcCommandType.NONE
         && this.m_5448_() == null
         && AWCoreRegistryAccess.is(this.getOrdersStack(), AWCoreIds.COMBAT_ORDER)
         && CombatOrderData.matchesDimension(this.getOrdersStack(), this.m_9236_())
         && !CombatOrderData.getEntries(this.getOrdersStack()).isEmpty();
   }

   boolean hasActiveCombatDeployment() {
      return !this.hasActivePatrolDeployment() && !this.shouldFollowCombatGroupFormation()
         ? this.isSwordsman() && SlashBladeCompat.canUseThornsGuard(this) && NpcShielding.findIncomingProjectile(this) != null
         : true;
   }

   private static class CombatMeleeAttackGoal extends MeleeAttackGoal {
      private final CombatNpcEntity npc;
      private int weaponCooldown;

      private CombatMeleeAttackGoal(CombatNpcEntity npc, double speedModifier, boolean followTargetEvenIfNotSeen) {
         super(npc, speedModifier, followTargetEvenIfNotSeen);
         this.npc = npc;
      }

      public boolean m_8036_() {
         return this.npc.canUseMeleeCombatGoal() && super.m_8036_();
      }

      public boolean m_8045_() {
         return this.npc.canUseMeleeCombatGoal() && (this.npc.isSwordsmanAerialRecovery() || super.m_8045_());
      }

      public void m_8037_() {
         if (this.weaponCooldown > 0) {
            this.weaponCooldown--;
         }

         super.m_8037_();
         LivingEntity target = this.npc.m_5448_();
         if (target != null && !this.npc.isSwordsman()) {
            double holdDistance = NpcEquipmentResolver.meleeHoldDistance(this.npc);
            if (this.npc.m_20280_(target) <= holdDistance * holdDistance && this.npc.hasMeleeLineOfSight(target)) {
               this.npc.m_21573_().m_26573_();
               this.npc.m_21563_().m_24960_(target, 30.0F, 30.0F);
            }
         }
      }

      protected void m_6739_(LivingEntity target, double distanceSqr) {
         if (!this.npc.isSwordsmanComboActive()) {
            double attackReachSqr = this.npc.isSwordsman() ? 1024.0 : this.m_6639_(target);
            if (distanceSqr <= attackReachSqr && this.weaponCooldown <= 0 && this.npc.hasMeleeLineOfSight(target)) {
               boolean attacked = this.npc.m_7327_(target);
               if (attacked) {
                  this.weaponCooldown = this.npc.isSwordsman()
                     ? this.npc.getSwordsmanTechniqueCooldownTicks()
                     : NpcEquipmentResolver.meleeAttackInterval(this.npc);
               }
            }
         }
      }

      protected double m_6639_(LivingEntity target) {
         double reach = NpcEquipmentResolver.meleeAttackReach(this.npc);
         return reach * reach;
      }
   }

   private static class CombatRangedAttackGoal extends Goal {
      private static final double MAX_RANGE = 18.0;
      private static final double MIN_RANGE = 8.0;
      private static final double RETREAT_START_RANGE = 6.0;
      private static final double RETREAT_END_RANGE = 10.0;
      private static final double CLOSE_REPOSITION_RANGE = 12.0;
      private static final int DRAW_TICKS = 20;
      private static final int COOLDOWN_TICKS = 30;
      private final CombatNpcEntity npc;
      private LivingEntity target;
      private int drawTicks;
      private int cooldown;
      private int retreatRepathTicks;
      private Vec3 retreatPosition;
      private int ballisticCheckCooldown;
      private boolean ballisticPath;

      private CombatRangedAttackGoal(CombatNpcEntity npc) {
         this.npc = npc;
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         this.target = this.npc.m_5448_();
         this.npc.ensureRangedFollowRange();
         boolean usable = !this.npc.isLogicalDragonRider() && this.npc.isRangedCombatant() && this.validTarget();
         if (usable) {
            this.resetBallisticCheck();
         }

         return usable;
      }

      public boolean m_8045_() {
         this.npc.ensureRangedFollowRange();
         return !this.npc.isLogicalDragonRider() && this.npc.isRangedCombatant() && this.npc.m_5448_() == this.target && this.validTarget();
      }

      public void m_8056_() {
         this.resetBallisticCheck();
      }

      public void m_8037_() {
         this.target = this.npc.m_5448_();
         if (!this.validTarget()) {
            this.stopUsingWeapon();
         } else {
            this.npc.m_21563_().m_24960_(this.target, 30.0F, 30.0F);
            double distanceSqr = this.npc.m_20280_(this.target);
            if (this.cooldown > 0) {
               this.cooldown--;
            }

            if (this.npc.m_6117_()) {
               if (this.shouldRetreat(distanceSqr)) {
                  this.continueRetreating();
               } else {
                  this.clearRetreat();
                  this.npc.m_21573_().m_26573_();
               }

               int requiredDrawTicks = this.npc.m_21205_().m_41720_() instanceof CrossbowItem
                  ? NpcRangedAttackHelper.crossbowChargeDuration(this.npc.m_21205_())
                  : 20;
               if (++this.drawTicks >= requiredDrawTicks) {
                  this.npc.m_6504_(this.target, 1.0F);
                  this.stopUsingWeapon();
                  this.drawTicks = 0;
                  this.cooldown = 30;
               }
            } else if (!this.canShootTarget()) {
               this.stopUsingWeapon();
               this.clearRetreat();
               this.drawTicks = 0;
               if (distanceSqr <= 144.0) {
                  this.npc.m_21573_().m_5624_(this.target, this.npc.playerSprintNavigationSpeed());
               } else {
                  this.npc.m_21573_().m_26573_();
               }
            } else if (this.cooldown > 0 && this.shouldRetreat(distanceSqr)) {
               this.stopUsingWeapon();
               this.drawTicks = 0;
               this.continueRetreating();
            } else {
               this.clearRetreat();
               this.npc.m_21573_().m_26573_();
               if (this.cooldown <= 0) {
                  this.npc.m_6672_(InteractionHand.MAIN_HAND);
                  this.drawTicks = 0;
               }
            }
         }
      }

      public void m_8041_() {
         this.stopUsingWeapon();
         this.npc.m_21573_().m_26573_();
         this.target = null;
         this.drawTicks = 0;
         this.resetBallisticCheck();
         this.clearRetreat();
      }

      private boolean validTarget() {
         return NpcTargeting.canHarm(this.npc, this.target);
      }

      private boolean canShootTarget() {
         if (this.ballisticCheckCooldown-- <= 0) {
            this.ballisticPath = this.npc.hasBallisticRangedShot(this.target);
            this.ballisticCheckCooldown = 5;
         }

         return this.ballisticPath;
      }

      private void resetBallisticCheck() {
         this.ballisticCheckCooldown = 0;
         this.ballisticPath = false;
      }

      private void stopUsingWeapon() {
         if (this.npc.m_6117_()) {
            this.npc.m_5810_();
         }
      }

      private void continueRetreating() {
         if (this.retreatRepathTicks > 0) {
            this.retreatRepathTicks--;
         }

         boolean reachedPoint = this.retreatPosition != null && this.npc.m_20182_().m_82557_(this.retreatPosition) < 2.25;
         if (this.retreatPosition == null || this.retreatRepathTicks <= 0 || reachedPoint || this.npc.m_21573_().m_26571_()) {
            Vec3 candidate = DefaultRandomPos.m_148407_(this.npc, 10, 4, this.target.m_20182_());
            if (candidate == null) {
               Vec3 away = this.npc.m_20182_().m_82546_(this.target.m_20182_());
               if (away.m_165925_() < 1.0E-4) {
                  return;
               }

               candidate = this.npc.m_20182_().m_82549_(away.m_82541_().m_82490_(7.0));
            }

            this.retreatPosition = candidate;
            this.retreatRepathTicks = 12;
            this.npc.m_21573_().m_26519_(candidate.f_82479_, candidate.f_82480_, candidate.f_82481_, this.npc.playerSprintNavigationSpeed());
         }
      }

      private void clearRetreat() {
         this.retreatPosition = null;
         this.retreatRepathTicks = 0;
      }

      private boolean shouldRetreat(double distanceSqr) {
         double distance = Math.sqrt(distanceSqr);
         return this.retreatPosition != null ? distance < 10.0 : distance < 6.0;
      }
   }

   private static class CommanderAuraGoal extends Goal {
      private static final int INTERVAL_TICKS = 40;
      private static final int EFFECT_TICKS = 200;
      private final CombatNpcEntity commander;
      private int cooldown;

      private CommanderAuraGoal(CombatNpcEntity commander) {
         this.commander = commander;
      }

      public boolean m_8036_() {
         if (!this.commander.isCommander()) {
            return false;
         } else if (this.cooldown > 0) {
            this.cooldown--;
            return false;
         } else {
            return true;
         }
      }

      public void m_8056_() {
         this.cooldown = 40;
         double range = 32.0;
         AttributeInstance followRange = this.commander.m_21051_(Attributes.f_22277_);
         if (followRange != null) {
            range = followRange.m_22135_();
         }

         AABB box = this.commander.m_20191_().m_82377_(range, range / 2.0, range);

         for (PlayerOwnedNpcEntity ally : this.commander.m_9236_().m_6443_(PlayerOwnedNpcEntity.class, box, this::canBoost)) {
            ally.m_7292_(new MobEffectInstance(MobEffects.f_19600_, 200, 0, true, false));
            ally.m_7292_(new MobEffectInstance(MobEffects.f_19605_, 200, 0, true, false));
         }
      }

      private boolean canBoost(PlayerOwnedNpcEntity ally) {
         return ally != null
            && ally.m_6084_()
            && ally != this.commander
            && ally.getOwner().equals(this.commander.getOwner())
            && (!(ally instanceof CombatNpcEntity combat) || !combat.isCommander());
      }
   }

   private static class MedicHealGoal extends Goal {
      private static final int INTERVAL_TICKS = 40;
      private static final double SEARCH_RANGE = 8.0;
      private static final double HEAL_RANGE_SQ = 36.0;
      private final CombatNpcEntity medic;
      private int cooldown;
      private LivingEntity target;

      private MedicHealGoal(CombatNpcEntity medic) {
         this.medic = medic;
         this.m_7021_(EnumSet.of(Flag.LOOK));
      }

      public boolean m_8036_() {
         if (!this.medic.isMedic() || this.medic.isGettingFood()) {
            return false;
         } else if (this.cooldown > 0) {
            this.cooldown--;
            return false;
         } else {
            this.target = this.findWoundedAlly();
            return this.target != null;
         }
      }

      public boolean m_8045_() {
         return this.medic.isMedic()
            && !this.medic.isGettingFood()
            && this.target != null
            && this.target.m_6084_()
            && this.target.m_21223_() < this.target.m_21233_()
            && this.medic.m_20280_(this.target) <= 64.0;
      }

      public void m_8056_() {
         this.cooldown = 40;
      }

      public void m_8037_() {
         if (this.target != null) {
            this.medic.m_21563_().m_148051_(this.target);
            if (this.medic.m_20280_(this.target) > 36.0) {
               this.medic.m_21573_().m_5624_(this.target, this.medic.playerWalkNavigationSpeed());
            } else {
               this.medic.m_21573_().m_26573_();
               this.target.m_5634_(2.0F);
               this.medic.m_6674_(InteractionHand.MAIN_HAND);
               this.cooldown = 40;
               this.target = null;
            }
         }
      }

      public void m_8041_() {
         this.target = null;
      }

      private LivingEntity findWoundedAlly() {
         AABB box = this.medic.m_20191_().m_82400_(8.0);
         return this.medic
            .m_9236_()
            .m_6443_(LivingEntity.class, box, this::canHeal)
            .stream()
            .min((first, second) -> Double.compare(this.medic.m_20280_(first), this.medic.m_20280_(second)))
            .orElse(null);
      }

      private boolean canHeal(LivingEntity entity) {
         if (entity == null || !entity.m_6084_() || entity == this.medic || entity.m_21223_() >= entity.m_21233_()) {
            return false;
         } else if (entity instanceof Player player) {
            return this.medic.getOwner().map(player.m_20148_()::equals).orElse(false);
         } else {
            if (entity instanceof PlayerOwnedNpcEntity ally && ally.getOwner().isPresent() && ally.getOwner().equals(this.medic.getOwner())) {
               return true;
            }

            return false;
         }
      }
   }

   private static class MoveToCommandGoal extends Goal {
      private final CombatNpcEntity npc;
      private final double speedModifier;
      private final double stopDistanceSq;
      private BlockPos target;

      private MoveToCommandGoal(CombatNpcEntity npc, double speedModifier, double stopDistance) {
         this.npc = npc;
         this.speedModifier = speedModifier;
         this.stopDistanceSq = stopDistance * stopDistance;
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         if (this.npc.getCurrentCommand() != NpcCommandType.MOVE) {
            return false;
         } else {
            this.target = this.npc.getCommandTarget();
            return !BlockPos.f_121853_.equals(this.target) && this.distanceToTargetSq() > this.stopDistanceSq;
         }
      }

      public boolean m_8045_() {
         return this.npc.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() > this.stopDistanceSq;
      }

      public void m_8037_() {
         this.npc.m_21563_().m_24946_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
         this.npc
            .m_21573_()
            .m_26519_(
               (double)this.target.m_123341_() + 0.5,
               (double)this.target.m_123342_(),
               (double)this.target.m_123343_() + 0.5,
               this.npc.playerSprintNavigationSpeed()
            );
      }

      public void m_8041_() {
         if (this.npc.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() <= this.stopDistanceSq) {
            this.npc.clearCommand();
         }

         this.target = null;
         this.npc.m_21573_().m_26573_();
      }

      private double distanceToTargetSq() {
         return this.npc.m_20275_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
      }
   }

   private static class PatrolOrderGoal extends Goal {
      private static final int MAX_TICKS_AT_POINT = 50;
      private static final double STOP_DISTANCE_SQ = 4.0;
      private final CombatNpcEntity npc;
      private final double speedModifier;
      private List<BlockPos> entries = List.of();

      private PatrolOrderGoal(CombatNpcEntity npc, double speedModifier) {
         this.npc = npc;
         this.speedModifier = speedModifier;
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         if (this.npc.getCurrentCommand() == NpcCommandType.NONE && this.npc.m_5448_() == null) {
            if (AWCoreRegistryAccess.is(this.npc.getOrdersStack(), AWCoreIds.COMBAT_ORDER)
               && CombatOrderData.matchesDimension(this.npc.getOrdersStack(), this.npc.m_9236_())) {
               this.entries = CombatOrderData.getEntries(this.npc.getOrdersStack());
               if (this.entries.isEmpty()) {
                  return false;
               } else {
                  if (this.npc.patrolIndex >= this.entries.size()) {
                     this.npc.patrolIndex = 0;
                  }

                  return true;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }

      public boolean m_8045_() {
         return this.m_8036_();
      }

      public void m_8037_() {
         BlockPos pos = this.entries.get(this.npc.patrolIndex);
         double distance = this.npc.m_20275_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_(), (double)pos.m_123343_() + 0.5);
         if (this.npc.patrolAtPoint) {
            this.npc.m_21573_().m_26573_();
            this.npc.patrolTicksAtPoint++;
            if (this.npc.patrolTicksAtPoint > 50) {
               this.moveToNextPoint();
            }
         } else {
            if (distance > 4.0) {
               this.npc.m_21563_().m_24946_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_(), (double)pos.m_123343_() + 0.5);
               if (this.npc.m_20202_() instanceof Mob mount) {
                  this.npc.patrolDrivingMount = true;
                  mount.m_21573_().m_26519_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_(), (double)pos.m_123343_() + 0.5, this.speedModifier);
               } else {
                  this.npc.patrolDrivingMount = false;
                  this.npc
                     .m_21573_()
                     .m_26519_((double)pos.m_123341_() + 0.5, (double)pos.m_123342_(), (double)pos.m_123343_() + 0.5, this.npc.playerWalkNavigationSpeed());
               }
            } else {
               this.npc.patrolDrivingMount = false;
               this.npc.patrolAtPoint = true;
               this.npc.patrolTicksAtPoint = 0;
            }
         }
      }

      public void m_8041_() {
         this.npc.patrolTicksAtPoint = 0;
         this.npc.patrolDrivingMount = false;
         this.npc.m_21573_().m_26573_();
      }

      private void moveToNextPoint() {
         this.npc.patrolAtPoint = false;
         this.npc.patrolTicksAtPoint = 0;
         this.npc.patrolIndex++;
         if (this.npc.patrolIndex >= this.entries.size()) {
            this.npc.patrolIndex = 0;
         }
      }
   }

   private static final class PlayerSoldierHurtByTargetGoal extends HurtByTargetGoal {
      private static final double OWNER_ALERT_RANGE = 64.0;
      private final CombatNpcEntity npc;

      private PlayerSoldierHurtByTargetGoal(CombatNpcEntity npc) {
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
            this.npc
               .m_9236_()
               .m_6443_(
                  CombatNpcEntity.class,
                  this.npc.m_20191_().m_82377_(64.0, 16.0, 64.0),
                  ally -> ally != this.npc && ally.m_6084_() && ally.getOwner().equals(this.npc.getOwner()) && NpcTargeting.canHarm(ally, attacker)
               )
               .forEach(ally -> {
                  ally.m_6703_(attacker);
                  ally.m_6710_(attacker);
               });
         }
      }
   }

   private static class ReturnHomeGoal extends Goal {
      private static final double STOP_DISTANCE_SQ = 9.0;
      private final CombatNpcEntity npc;
      private final double speedModifier;
      private BlockPos home;
      private int repathTicks;

      private ReturnHomeGoal(CombatNpcEntity npc, double speedModifier) {
         this.npc = npc;
         this.speedModifier = speedModifier;
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         if (!this.canReturnHome()) {
            return false;
         } else {
            this.home = this.npc.getHomePos();
            return this.distanceToHomeSq() > 9.0;
         }
      }

      public boolean m_8045_() {
         return this.home != null && this.canReturnHome() && this.distanceToHomeSq() > 9.0;
      }

      public void m_8056_() {
         this.repathTicks = 0;
      }

      public void m_8037_() {
         this.npc.m_21563_().m_24946_((double)this.home.m_123341_() + 0.5, (double)this.home.m_123342_(), (double)this.home.m_123343_() + 0.5);
         if (this.repathTicks-- <= 0 || this.npc.m_21573_().m_26571_()) {
            this.npc
               .m_21573_()
               .m_26519_((double)this.home.m_123341_() + 0.5, (double)this.home.m_123342_(), (double)this.home.m_123343_() + 0.5, this.speedModifier);
            this.repathTicks = 10;
         }
      }

      public void m_8041_() {
         this.repathTicks = 0;
         this.home = null;
         this.npc.m_21573_().m_26573_();
      }

      private boolean canReturnHome() {
         if (this.npc.hasHome()
            && !this.npc.m_20159_()
            && !this.npc.isGuiInteractionPaused()
            && !this.npc.isGettingFood()
            && this.npc.getCurrentCommand() == NpcCommandType.NONE
            && !this.npc.shouldFollowCombatGroupFormation()) {
            LivingEntity target = this.npc.m_5448_();
            return target != null && target.m_6084_() ? false : !this.hasActivePatrolOrder();
         } else {
            return false;
         }
      }

      private boolean hasActivePatrolOrder() {
         return AWCoreRegistryAccess.is(this.npc.getOrdersStack(), AWCoreIds.COMBAT_ORDER)
            && CombatOrderData.matchesDimension(this.npc.getOrdersStack(), this.npc.m_9236_())
            && !CombatOrderData.getEntries(this.npc.getOrdersStack()).isEmpty();
      }

      private double distanceToHomeSq() {
         return this.npc.m_20275_((double)this.home.m_123341_() + 0.5, (double)this.home.m_123342_(), (double)this.home.m_123343_() + 0.5);
      }
   }

   private static class ShieldDefenseGoal extends Goal {
      private static final int SHIELD_LINGER_TICKS = 20;
      private final CombatNpcEntity npc;
      private int lingerTicks;

      private ShieldDefenseGoal(CombatNpcEntity npc) {
         this.npc = npc;
      }

      public boolean m_8036_() {
         return "soldier".equals(this.npc.getNpcSubtype()) && !this.npc.m_20159_() && NpcShielding.shouldRaiseShield(this.npc, this.npc.m_5448_());
      }

      public boolean m_8045_() {
         if (this.m_8036_()) {
            this.lingerTicks = 20;
            return true;
         } else {
            return this.lingerTicks-- > 0;
         }
      }

      public void m_8056_() {
         this.lingerTicks = 20;
         this.npc.setShielding(true);
      }

      public void m_8037_() {
         LivingEntity target = this.npc.m_5448_();
         if (target != null) {
            this.npc.m_21563_().m_148051_(target);
         }

         if (NpcShielding.shouldRaiseShield(this.npc, this.npc.m_5448_())) {
            this.lingerTicks = 20;
         }

         this.npc.setShielding(this.lingerTicks > 0);
      }

      public void m_8041_() {
         this.lingerTicks = 0;
         this.npc.setShielding(false);
      }
   }

   private static class SwordsmanProjectileGuardGoal extends Goal {
      private final CombatNpcEntity npc;
      private Projectile threat;
      private int guardLingerTicks;

      private SwordsmanProjectileGuardGoal(CombatNpcEntity npc) {
         this.npc = npc;
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         if (this.npc.isSwordsman() && SlashBladeCompat.canUseThornsGuard(this.npc)) {
            this.threat = NpcShielding.findIncomingProjectile(this.npc);
            return this.threat != null;
         } else {
            return false;
         }
      }

      public boolean m_8045_() {
         if (this.npc.isSwordsman() && SlashBladeCompat.canUseThornsGuard(this.npc)) {
            Projectile nextThreat = NpcShielding.findIncomingProjectile(this.npc);
            if (nextThreat != null) {
               this.threat = nextThreat;
               this.guardLingerTicks = 4;
               return true;
            } else {
               return this.guardLingerTicks-- > 0;
            }
         } else {
            return false;
         }
      }

      public void m_8056_() {
         this.guardLingerTicks = 4;
         this.npc.cancelSwordsmanCombo();
         this.npc.m_21573_().m_26573_();
         SlashBladeCompat.setThornsGuardInput(this.npc, true);
      }

      public void m_8037_() {
         this.npc.m_21573_().m_26573_();
         Vec3 motion = this.npc.m_20184_();
         this.npc.m_20334_(0.0, motion.f_82480_, 0.0);
         if (this.threat != null && this.threat.m_6084_()) {
            this.npc.m_21563_().m_24950_(this.threat.m_20185_(), this.threat.m_20186_(), this.threat.m_20189_(), 90.0F, 90.0F);
            Vec3 direction = this.threat.m_20182_().m_82546_(this.npc.m_20182_());
            if (direction.m_165925_() > 1.0E-5) {
               float yaw = (float)Math.toDegrees(Math.atan2(-direction.f_82479_, direction.f_82481_));
               this.npc.m_146922_(yaw);
               this.npc.m_5618_(yaw);
               this.npc.m_5616_(yaw);
            }
         }

         SlashBladeCompat.setThornsGuardInput(this.npc, true);
      }

      public void m_8041_() {
         this.threat = null;
         this.guardLingerTicks = 0;
         SlashBladeCompat.setThornsGuardInput(this.npc, false);
      }
   }

   private static enum SwordsmanTechnique {
      NONE(0, false),
      COMBO_A(42, false),
      COMBO_B(54, false),
      COMBO_C(30, false),
      RAPID_SLASH(80, false),
      UPPER_CHAIN(24, false),
      AERIAL_COMBO_A(82, true),
      AERIAL_COMBO_B(88, true),
      AERIAL_CLEAVE(50, true),
      SLASH_ART(64, false);

      private final int durationTicks;
      private final boolean aerial;

      private SwordsmanTechnique(int durationTicks, boolean aerial) {
         this.durationTicks = durationTicks;
         this.aerial = aerial;
      }
   }
}
