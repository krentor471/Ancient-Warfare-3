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
   private static final EntityDataAccessor<Integer> SWORDSMAN_COMBO = SynchedEntityData.defineId(CombatNpcEntity.class, EntityDataSerializers.INT);
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
   private ItemStack legacySpellContainerStack = ItemStack.EMPTY;

   protected void dropCustomDeathLoot(DamageSource source, int looting, boolean killedByPlayer) {
      if (AWCorePlatform.gameplayConfig().playerSoldierEquipmentDropChance() < 0.0) {
         super.dropCustomDeathLoot(source, looting, killedByPlayer);
      }
   }

   protected void dropEquipment() {
      double chance = AWCorePlatform.gameplayConfig().playerSoldierEquipmentDropChance();
      if (chance < 0.0) {
         super.dropEquipment();
      } else if (!this.level().isClientSide && this.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
         for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = this.getItemBySlot(slot);
            if (!stack.isEmpty()
               && !EnchantmentHelper.hasVanishingCurse(stack)
               && chance > 0.0
               && (chance >= 1.0 || this.random.nextDouble() < chance)
               && this.spawnAtLocation(stack.copy()) != null) {
               this.setItemSlot(slot, ItemStack.EMPTY);
            }
         }
      }
   }

   protected PathNavigation createNavigation(Level level) {
      AmphibiousPathNavigation navigation = new AmphibiousPathNavigation(this, level);
      navigation.setCanFloat(true);
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
      return createMobAttributes()
         .add(Attributes.MAX_HEALTH, 24.0)
         .add(Attributes.MOVEMENT_SPEED, 0.27)
         .add(Attributes.ATTACK_DAMAGE, 2.0)
         .add(Attributes.ATTACK_SPEED, 4.0)
         .add(Attributes.FOLLOW_RANGE, 32.0);
   }

   @Override
   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(SWORDSMAN_COMBO, 0);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(1, new CombatNpcEntity.MoveToCommandGoal(this, 1.0, 2.0));
      this.goalSelector.addGoal(1, new CombatNpcEntity.ShieldDefenseGoal(this));
      this.goalSelector.addGoal(0, new CombatNpcEntity.SwordsmanProjectileGuardGoal(this));
      this.goalSelector.addGoal(2, new CombatNpcEntity.CombatRangedAttackGoal(this));
      this.goalSelector.addGoal(2, new CombatNpcEntity.CombatMeleeAttackGoal(this, this.playerSprintNavigationSpeed(), true));
      this.goalSelector.addGoal(3, new CombatNpcEntity.CommanderAuraGoal(this));
      this.goalSelector.addGoal(3, new CombatNpcEntity.MedicHealGoal(this));
      this.goalSelector.addGoal(3, new CombatGroupFollowGoal(this));
      this.goalSelector.addGoal(4, new CombatNpcEntity.PatrolOrderGoal(this, 1.0));
      this.goalSelector.addGoal(5, new CombatNpcEntity.ReturnHomeGoal(this, 1.0));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, new CombatNpcEntity.PlayerSoldierHurtByTargetGoal(this));
      this.targetSelector
         .addGoal(
            2,
            new VerticalRangeTargetGoal<>(
               this,
               FactionNpcEntity.class,
               2,
               false,
               false,
               target -> this.isHostileFactionNpc(target) && this.canAcquireCombatTarget(target),
               () -> this.isRangedCombatant() ? 64.0 : 4.0
            )
         );
      this.targetSelector
         .addGoal(
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
   public void setTarget(LivingEntity target) {
      if (target != null && this.isFollowCombatRecallActive()) {
         super.setTarget(null);
      } else if (target != null && !DragonRiderController.canAcquireTarget(this, target)) {
         super.setTarget(null);
      } else {
         super.setTarget(target);
      }
   }

   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (this.tryAcceptFood(player, hand)) {
         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else if (player.getItemInHand(hand).getItem() instanceof CommandBatonItem) {
         if (!this.level().isClientSide && this.hasCommandPermission(player)) {
            CommandBatonItem.toggleNpcSelection(player.getItemInHand(hand), player, this);
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else {
         this.openNpcInventory(player);
         return InteractionResult.sidedSuccess(this.level().isClientSide);
      }
   }

   public void openNpcInventory(Player player) {
      if (!this.level().isClientSide && this.hasCommandPermission(player)) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider((containerId, inventory, openedPlayer) -> new NpcInventoryMenu(containerId, inventory, this), this.getDisplayName()),
            buffer -> buffer.writeInt(this.getId())
         );
      }
   }

   @Override
   public String getNpcType() {
      return "combat";
   }

   @Override
   public String getNpcSubtype() {
      return this.isSpellcasterCombatant() ? "mage" : CombatProfessionResolver.subtypeFor(this.getMainHandItem(), this.getOffhandItem());
   }

   @Override
   public Component getTypeName() {
      Component baseName;
      if (!this.isLogicalDragonRider() && (this.getVehicle() == null || !IceAndFireDragonCompat.isDragon(this.getVehicle()))) {
         if (this.getVehicle() != null && IceAndFireHippocampusCompat.isHippocampus(this.getVehicle())) {
            baseName = Component.translatable("entity.ancientwarfare.combat.hippocampus_rider");
         } else {
            baseName = Component.translatable("entity.ancientwarfare.combat." + this.getNpcSubtype());
         }
      } else {
         baseName = Component.translatable("entity.ancientwarfare.combat.dragon_rider");
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
      return !this.isSpellcasterCombatant() && NpcEquipmentResolver.isRangedWeapon(this.getMainHandItem());
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
         AttributeInstance followRange = this.getAttribute(Attributes.FOLLOW_RANGE);
         if (followRange != null) {
            followRange.setBaseValue(Math.max(followRange.getBaseValue(), 48.0));
         }
      }
   }

   public boolean isSwordsman() {
      return "swordsman".equals(this.getNpcSubtype());
   }

   public int getSwordsmanComboStep() {
      return (Integer)this.entityData.get(SWORDSMAN_COMBO);
   }

   @Override
   public boolean doHurtTarget(Entity target) {
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
         this.swing(InteractionHand.MAIN_HAND, true);
         boolean hit = super.doHurtTarget(target);
         if (hit && target instanceof LivingEntity livingTarget) {
            IceAndFireEquipmentCompat.applySupportedMeleeHit(this.getMainHandItem(), livingTarget, this);
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
         if (target instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
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
      double distance = Math.sqrt(this.distanceToSqr(target));
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

      int selected = candidates.get(this.getRandom().nextInt(candidates.size()));
      if (candidates.size() > 1 && selected == this.swordsmanPreviousSummonedSwordArt && this.getRandom().nextFloat() < 0.7F) {
         List<Integer> alternatives = candidates.stream().filter(candidate -> candidate != this.swordsmanPreviousSummonedSwordArt).toList();
         if (!alternatives.isEmpty()) {
            selected = alternatives.get(this.getRandom().nextInt(alternatives.size()));
         }
      }

      return selected;
   }

   private CombatNpcEntity.SwordsmanTechnique chooseSwordsmanTechnique(LivingEntity target) {
      double distance = Math.sqrt(this.distanceToSqr(target));
      double heightDifference = target.getY() - this.getY();
      List<CombatNpcEntity.SwordsmanTechnique> candidates = new ArrayList<>();
      int heldArt = SlashBladeCompat.getHeldSlashArt(this);
      if (this.shouldUseSwordsmanSlashArt(target, heldArt, distance)) {
         this.swordsmanChargeType = this.chooseSwordsmanChargeType(distance);
         this.swordsmanChargeTicks = this.chargeTicksForType(this.swordsmanChargeType);
         return CombatNpcEntity.SwordsmanTechnique.SLASH_ART;
      } else {
         if (!this.onGround() && !this.isPassenger()) {
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
            } else if (!this.isPassenger()) {
               candidates.add(CombatNpcEntity.SwordsmanTechnique.UPPER_CHAIN);
            }

            if (!this.isPassenger() && distance >= 2.0) {
               candidates.add(CombatNpcEntity.SwordsmanTechnique.RAPID_SLASH);
            }
         } else {
            if (!this.isPassenger()) {
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

         CombatNpcEntity.SwordsmanTechnique selected = candidates.get(this.getRandom().nextInt(candidates.size()));
         if (candidates.size() > 1 && selected == this.swordsmanPreviousTechnique && this.getRandom().nextFloat() < 0.65F) {
            List<CombatNpcEntity.SwordsmanTechnique> alternatives = candidates.stream()
               .filter(candidate -> candidate != this.swordsmanPreviousTechnique)
               .toList();
            if (!alternatives.isEmpty()) {
               selected = alternatives.get(this.getRandom().nextInt(alternatives.size()));
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
      if (!this.onGround()
         || this.isPassenger()
         || this.swordsmanSlashArtCooldown > 0
         || this.swordsmanSlashArtCharge < 4
         || heldArt == 0
         || !this.isChargedArtSuitableAtDistance(heldArt, distance)
         || target.getHealth() <= Math.max(8.0F, target.getMaxHealth() * 0.2F)) {
         return false;
      } else if (this.swordsmanSlashArtCharge >= 7) {
         return true;
      } else {
         float chance = switch (this.swordsmanSlashArtCharge) {
            case 4 -> 0.22F;
            case 5 -> 0.38F;
            default -> 0.6F;
         };
         return this.getRandom().nextFloat() < chance;
      }
   }

   private int chooseSwordsmanChargeType(double distance) {
      List<Integer> candidates = new ArrayList<>();
      candidates.add(1);
      candidates.add(2);
      if (SlashBladeCompat.canUseChargedSlashArt(this, 3) && distance >= 5.0 && this.getRandom().nextFloat() < 0.22F) {
         candidates.add(3);
      }

      if (candidates.size() > 1) {
         candidates.removeIf(type -> type == this.swordsmanLastChargeType);
      }

      int selected = candidates.get(this.getRandom().nextInt(candidates.size()));
      if (candidates.size() > 1 && selected == this.swordsmanPreviousChargeType && this.getRandom().nextFloat() < 0.7F) {
         List<Integer> alternatives = candidates.stream().filter(type -> type != this.swordsmanPreviousChargeType).toList();
         if (!alternatives.isEmpty()) {
            selected = alternatives.get(this.getRandom().nextInt(alternatives.size()));
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
      if (!this.isPassenger() && this.onGround() && target != null && target.isAlive()) {
         if (this.distanceToSqr(target) > 20.25) {
            return false;
         } else {
            AABB launchSpace = this.getBoundingBox().inflate(0.35, 0.0, 0.35).expandTowards(0.0, 3.75, 0.0);
            return this.level().noCollision(this, launchSpace);
         }
      } else {
         return false;
      }
   }

   private boolean performSwordsmanTechniqueHit(int move, float damageRatio, int knockback) {
      if (this.swordsmanComboTarget != null
         && !(this.distanceToSqr(this.swordsmanComboTarget) > 27.5625)
         && SlashBladeCompat.performTechniqueHit(this, this.swordsmanComboTarget, move, damageRatio, knockback)) {
         this.swordsmanComboStep = move;
         this.swordsmanPoseTicks = 10;
         this.entityData.set(SWORDSMAN_COMBO, move);
         this.swing(InteractionHand.MAIN_HAND, true);
         return true;
      } else {
         return false;
      }
   }

   private void setSwordsmanTechniquePose(int move) {
      if (SlashBladeCompat.setTechniqueState(this, move)) {
         this.swordsmanComboStep = move;
         this.swordsmanPoseTicks = 10;
         this.entityData.set(SWORDSMAN_COMBO, move);
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
      if (this.swordsmanComboTarget != null && !this.isPassenger()) {
         this.getNavigation().stop();
         this.getLookControl().setLookAt(this.swordsmanComboTarget, 90.0F, 90.0F);
         this.faceSwordsmanTarget();
         if (!this.swordsmanChargedArtReleased) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.05, 1.0, 0.05));
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
               this.dashSwordsmanTowardsTarget(this.distanceToSqr(this.swordsmanComboTarget));
            } else {
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.25, 1.0, 0.25));
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
         Vec3 delta = this.swordsmanComboTarget.position().subtract(this.position());
         if (!(delta.horizontalDistanceSqr() < 1.0E-6)) {
            float yaw = (float)Math.toDegrees(Math.atan2(-delta.x, delta.z));
            this.setYRot(yaw);
            this.setYBodyRot(yaw);
            this.setYHeadRot(yaw);
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
      } else if (this.swordsmanComboTarget != null && !this.isPassenger()) {
         double distanceSqr = this.distanceToSqr(this.swordsmanComboTarget);
         if (distanceSqr > 18.0625) {
            this.dashSwordsmanTowardsTarget(distanceSqr);
            if (tick % 7 == 1) {
               this.setSwordsmanTechniquePose(9);
            }

            return true;
         } else {
            this.getNavigation().stop();
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.35, 1.0, 0.35));
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
      if (this.swordsmanComboTarget != null && !this.isPassenger()) {
         this.getNavigation().stop();
         Vec3 direction = this.horizontalDirectionToSwordsmanTarget();
         if (!(direction.lengthSqr() < 1.0E-4)) {
            double distance = Math.sqrt(distanceSqr);
            double speed = distance > 16.0 ? 1.1 : (distance > 8.0 ? 0.92 : 0.72);
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(direction.x * speed, motion.y, direction.z * speed);
            float yaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
            this.setYRot(yaw);
            this.setYBodyRot(yaw);
            this.setYHeadRot(yaw);
            this.setSprinting(true);
            this.hasImpulse = true;
            if (this.horizontalCollision && this.onGround()) {
               this.getJumpControl().jump();
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
            this.setDeltaMovement(this.getDeltaMovement().x, 0.6, this.getDeltaMovement().z);
            this.setOnGround(false);
            this.hasImpulse = true;
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
      if (!this.isPassenger()) {
         this.getNavigation().stop();
         Vec3 motion = this.getDeltaMovement();
         Vec3 towardTarget = this.horizontalDirectionToSwordsmanTarget();
         this.setDeltaMovement(towardTarget.x * 0.22, Math.max(0.72, motion.y), towardTarget.z * 0.22);
         this.setOnGround(false);
         this.hasImpulse = true;
      }
   }

   private void hoverSwordsmanNearTarget() {
      if (this.swordsmanComboTarget != null && !this.isPassenger()) {
         this.getNavigation().stop();
         Vec3 delta = this.swordsmanComboTarget.position().add(0.0, 1.0, 0.0).subtract(this.position());
         double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
         double x = Mth.clamp(delta.x * 0.1, -0.24, 0.24);
         double z = Mth.clamp(delta.z * 0.1, -0.24, 0.24);
         double y = Mth.clamp(delta.y * 0.08, -0.08, 0.16);
         if (horizontalDistance < 1.2) {
            x *= 0.35;
            z *= 0.35;
         }

         this.setDeltaMovement(x, y, z);
         this.fallDistance = 0.0F;
         this.setOnGround(false);
         this.hasImpulse = true;
      }
   }

   private boolean shouldBeginSwordsmanAerialCleave(int tick, int maximumAirTicks) {
      if (this.swordsmanAerialCleaveActive || this.isPassenger()) {
         return false;
      } else if (tick < maximumAirTicks && this.swordsmanComboTarget != null) {
         double horizontalDistanceSqr = this.horizontalDistanceSqr(this.swordsmanComboTarget);
         double targetBelow = this.getY() - this.swordsmanComboTarget.getY();
         return tick >= 16 && (targetBelow > 2.6 || horizontalDistanceSqr > 30.25 || !this.swordsmanComboTarget.isAlive());
      } else {
         return true;
      }
   }

   private void beginSwordsmanAerialCleave() {
      this.swordsmanAerialCleaveActive = true;
      this.swordsmanAerialCleaveTicks = 0;
      this.setSwordsmanTechniquePose(18);
      if (!this.isPassenger()) {
         this.getNavigation().stop();
         Vec3 motion = this.getDeltaMovement();
         this.setDeltaMovement(motion.x * 0.35, Math.min(-0.35, motion.y), motion.z * 0.35);
         this.setOnGround(false);
         this.hasImpulse = true;
      }
   }

   private boolean tickSwordsmanAerialCleave() {
      if (this.isPassenger()) {
         return false;
      } else {
         this.swordsmanAerialCleaveTicks++;
         this.fallDistance = 0.0F;
         Vec3 towardTarget = this.horizontalDirectionToSwordsmanTarget();
         Vec3 motion = this.getDeltaMovement();
         double downwardSpeed = Math.max(-1.65, motion.y - 0.24);
         this.setDeltaMovement(towardTarget.x * 0.18, downwardSpeed, towardTarget.z * 0.18);
         this.hasImpulse = true;
         if (this.swordsmanAerialCleaveTicks % 4 == 0
            && this.swordsmanComboTarget != null
            && this.distanceToSqr(this.swordsmanComboTarget) <= 22.5625
            && !this.performSwordsmanTechniqueHit(18, 0.44F, 3)) {
            return false;
         } else {
            if (this.onGround()) {
               if (this.swordsmanComboTarget != null && this.distanceToSqr(this.swordsmanComboTarget) <= 27.5625) {
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
         return Vec3.ZERO;
      } else {
         Vec3 delta = this.swordsmanComboTarget.position().subtract(this.position());
         Vec3 horizontal = new Vec3(delta.x, 0.0, delta.z);
         return horizontal.lengthSqr() < 1.0E-4 ? Vec3.ZERO : horizontal.normalize();
      }
   }

   private double horizontalDistanceSqr(LivingEntity target) {
      double x = target.getX() - this.getX();
      double z = target.getZ() - this.getZ();
      return x * x + z * z;
   }

   private void driveSwordsmanTowardsTarget(double speed, double stopDistance) {
      if (this.swordsmanComboTarget != null && !this.isPassenger()) {
         if (this.distanceToSqr(this.swordsmanComboTarget) > stopDistance * stopDistance) {
            this.getNavigation().moveTo(this.swordsmanComboTarget, speed);
         } else {
            this.getNavigation().stop();
         }
      }
   }

   private void tickSwordsmanCombo() {
      if (!this.level().isClientSide) {
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
                  if (!this.swordsmanTechnique.aerial && !this.swordsmanAerialCleaveActive || this.isPassenger()) {
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
                  this.getLookControl().setLookAt(target, 60.0F, 60.0F);
               }

               if (this.swordsmanTechnique.aerial
                  || this.swordsmanTechnique == CombatNpcEntity.SwordsmanTechnique.RAPID_SLASH
                  || this.swordsmanTechnique == CombatNpcEntity.SwordsmanTechnique.SLASH_ART
                  || this.swordsmanTechnique == CombatNpcEntity.SwordsmanTechnique.UPPER_CHAIN && this.swordsmanComboTicks >= 5) {
                  if ((this.swordsmanTechnique.aerial || this.swordsmanTechnique == CombatNpcEntity.SwordsmanTechnique.UPPER_CHAIN) && !this.isPassenger()) {
                     this.getNavigation().stop();
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
         && this.swordsmanComboTarget.isAlive()
         && this.swordsmanComboTarget.level() == this.level()
         && this.distanceToSqr(this.swordsmanComboTarget) <= 1600.0;
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
      this.setSprinting(false);
      if (!this.level().isClientSide && (Integer)this.entityData.get(SWORDSMAN_COMBO) != 0) {
         this.entityData.set(SWORDSMAN_COMBO, 0);
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

   public void performRangedAttack(LivingEntity target, float velocity) {
      if (target != null && target.isAlive()) {
         this.recordCombatAttack();
         if (NpcEquipmentResolver.isCrossbow(this.getMainHandItem())) {
            this.performCrossbowAttack(target);
         } else {
            ItemStack projectileStack = this.getProjectile(this.getMainHandItem());
            AbstractArrow arrow = ProjectileUtil.getMobArrow(this, projectileStack, velocity);
            this.positionDragonRiderArrow(arrow, target);
            float projectileSpeed = this.calculateProjectileVelocity(target);
            Vec3 start = new Vec3(this.getX(), arrow.getY(), this.getZ());
            NpcRangedAttackHelper.BallisticShot shot = NpcRangedAttackHelper.findBallisticShot(this, target, start, (double)projectileSpeed, 0.05)
               .orElseGet(() -> this.fallbackRangedShot(target, start, projectileSpeed));
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
         this.positionDragonRiderArrow(arrow, target);
         NpcRangedAttackHelper.configureCrossbowArrow(this, arrow, crossbow);
         arrow.shoot(shotDirection.x, shotDirection.y, shotDirection.z, 3.15F, (float)(8 - this.level().getDifficulty().getId() * 2));
         this.level().addFreshEntity(arrow);
      }

      this.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
   }

   private NpcRangedAttackHelper.BallisticShot fallbackRangedShot(LivingEntity target, Vec3 start, float speed) {
      Vec3 aim = NpcRangedAttackHelper.predictTargetPosition(this, target, (double)speed);
      double horizontal = Math.sqrt(Math.pow(aim.x - start.x, 2.0) + Math.pow(aim.z - start.z, 2.0));
      return new NpcRangedAttackHelper.BallisticShot(aim.subtract(start).add(0.0, horizontal * 0.2, 0.0).normalize(), speed);
   }

   private boolean hasBallisticRangedShot(LivingEntity target) {
      float speed = NpcEquipmentResolver.isCrossbow(this.getMainHandItem()) ? 3.15F : this.calculateProjectileVelocity(target);
      return NpcRangedAttackHelper.findBallisticShot(this, target, this.getEyePosition(), (double)speed, 0.05).isPresent();
   }

   private boolean canAcquireCombatTarget(LivingEntity target) {
      return !DragonRiderController.canAcquireTarget(this, target) ? false : !this.isRangedCombatant() || this.hasBallisticRangedShot(target);
   }

   private void positionDragonRiderArrow(AbstractArrow arrow, LivingEntity target) {
      if (this.isLogicalDragonRider()) {
         Vec3 direction = this.targetAimDirection(target);
         Vec3 origin = this.position().add(0.0, (double)this.getBbHeight() * 0.55, 0.0).add(direction.scale(1.35));
         arrow.setPos(origin.x, origin.y, origin.z);
      }
   }

   private Vec3 targetAimDirection(LivingEntity target) {
      Vec3 aim = NpcRangedAttackHelper.predictTargetPosition(this, target, 3.15);
      return aim.subtract(this.position().add(0.0, (double)this.getBbHeight() * 0.55, 0.0)).normalize();
   }

   private float calculateProjectileVelocity(LivingEntity target) {
      double dx = target.getX() - this.getX();
      double dz = target.getZ() - this.getZ();
      double distance = Math.sqrt(dx * dx + dz * dz);
      double height = target.getY(0.35) - this.getY(0.9);
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
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (!this.legacySpellContainerStack.isEmpty()) {
         tag.put("SpellbookStack", this.legacySpellContainerStack.save(new CompoundTag()));
      }

      tag.putInt("PatrolIndex", this.patrolIndex);
      tag.putBoolean("PatrolAtPoint", this.patrolAtPoint);
      tag.putInt("PatrolTicksAtPoint", this.patrolTicksAtPoint);
      tag.putInt("SwordsmanSlashArtCooldown", this.swordsmanSlashArtCooldown);
      tag.putInt("SwordsmanSlashArtCharge", this.swordsmanSlashArtCharge);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.legacySpellContainerStack = tag.contains("SpellbookStack", 10) ? ItemStack.of(tag.getCompound("SpellbookStack")) : ItemStack.EMPTY;
      this.patrolIndex = tag.getInt("PatrolIndex");
      this.patrolAtPoint = tag.getBoolean("PatrolAtPoint");
      this.patrolTicksAtPoint = tag.getInt("PatrolTicksAtPoint");
      this.swordsmanSlashArtCooldown = tag.contains("SwordsmanSlashArtCooldown") ? Math.max(0, tag.getInt("SwordsmanSlashArtCooldown")) : 200;
      this.swordsmanSlashArtCharge = tag.contains("SwordsmanSlashArtCharge") ? Mth.clamp(tag.getInt("SwordsmanSlashArtCharge"), 0, 7) : 0;
   }

   @Override
   protected boolean shouldSkipIdleMountedStop() {
      return this.patrolDrivingMount;
   }

   @Override
   protected boolean usesCustomMountedMovement() {
      return this.getVehicle() instanceof Mob;
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (!this.level().isClientSide && !this.legacySpellContainerStack.isEmpty()) {
         this.migrateLegacySpellContainer();
      }

      this.ensureRangedFollowRange();
      SlashBladeCompat.preserveNpcBladeDurability(this);
      this.tickSwordsmanCombo();
      this.tickSwordsmanPose();
      SlashBladeCompat.preserveNpcBladeDurability(this);
      if (!this.level().isClientSide && this.getCurrentCommand() == NpcCommandType.ATTACK && (this.getTarget() == null || !this.getTarget().isAlive())) {
         this.clearCommand();
      }

      if (this.isLogicalDragonRider() || this.getVehicle() != null && IceAndFireDragonCompat.isDragon(this.getVehicle())) {
         DragonRiderController.tick(this);
      }

      if (this.getVehicle() != null && IceAndFireHippogryphCompat.isHippogryph(this.getVehicle())) {
         HippogryphRiderController.tick(this);
      }

      if (this.getVehicle() != null && IceAndFireHippocampusCompat.isHippocampus(this.getVehicle())) {
         HippocampusRiderController.tick(this);
      }

      if (this.isCommander()) {
         CombatGroupController.tick(this);
      }

      this.tickOwnedCavalryCharge();
      this.tickOwnedMountedArcherOrbit();
   }

   private void migrateLegacySpellContainer() {
      if (!this.legacySpellContainerStack.isEmpty()) {
         if (this.getMainHandItem().isEmpty()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, this.legacySpellContainerStack);
            this.legacySpellContainerStack = ItemStack.EMPTY;
         } else if (this.getOffhandItem().isEmpty()) {
            this.setItemSlot(EquipmentSlot.OFFHAND, this.legacySpellContainerStack);
            this.legacySpellContainerStack = ItemStack.EMPTY;
         } else if (!this.level().isClientSide) {
            this.spawnAtLocation(this.legacySpellContainerStack);
            this.legacySpellContainerStack = ItemStack.EMPTY;
         }
      }
   }

   private void tickSwordsmanPose() {
      if (!this.level().isClientSide) {
         if (!this.isSwordsman()) {
            this.cancelSwordsmanCombo();
         } else {
            if (this.swordsmanPoseTicks > 0 && --this.swordsmanPoseTicks == 0) {
               this.entityData.set(SWORDSMAN_COMBO, 0);
            }
         }
      }
   }

   private void tickOwnedCavalryCharge() {
      if (this.getVehicle() == null || !IceAndFireHippocampusCompat.isHippocampus(this.getVehicle()) && !IceAndFireHippogryphCompat.isHippogryph(this.getVehicle())) {
         if (!this.isRangedCombatant()) {
            if (this.getVehicle() instanceof Mob mount) {
               if (!NpcWaterEscape.isRecoveryActive(this)) {
                  LivingEntity target = this.getTarget();
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

                              this.getLookControl().setLookAt(target, 30.0F, 30.0F);
                              if (this.mountPassDestination != null) {
                                 Vec3 destination = this.mountPassDestination;
                                 this.mountPassTicks--;
                                 this.driveMountedCharge(mount, destination);
                                 if (this.mountPassDestination == null) {
                                    this.mountAttackedThisCharge = false;
                                 } else {
                                    if (this.mountPassTicks <= 0 || mount.position().distanceToSqr(destination) < 3.0) {
                                       this.mountPassDestination = null;
                                       this.mountAttackedThisCharge = false;
                                    }
                                 }
                              } else {
                                 double attackDistance = Math.max(3.2, NpcEquipmentResolver.meleeAttackReach(this));
                                 if (mount.distanceToSqr(target) > attackDistance * attackDistance) {
                                    this.mountAttackedThisCharge = false;
                                    this.driveMountedCharge(mount, target.position());
                                 } else {
                                    if (!this.mountAttackedThisCharge && this.mountAttackCooldown <= 0) {
                                       this.swing(InteractionHand.MAIN_HAND);
                                       this.doHurtTarget(target);
                                       this.mountAttackedThisCharge = true;
                                       this.mountAttackCooldown = NpcEquipmentResolver.meleeAttackInterval(this);
                                       Vec3 direction = target.position().subtract(mount.position());
                                       Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
                                       horizontal = horizontal.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : horizontal.normalize();
                                       this.mountPassDestination = target.position().add(horizontal.scale(10.0));
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
      if (this.getVehicle() == null || !IceAndFireHippocampusCompat.isHippocampus(this.getVehicle()) && !IceAndFireHippogryphCompat.isHippogryph(this.getVehicle())) {
         if (!this.isRangedCombatant() || !(this.getVehicle() instanceof Mob mount)) {
            this.mountedArcherTarget = null;
         } else if (NpcWaterEscape.isRecoveryActive(this)) {
            this.mountedArcherTarget = null;
         } else {
            LivingEntity target = this.getTarget();
            if (this.getCurrentCommand() == NpcCommandType.MOVE && this.updateMountedMoveMovement()) {
               this.mountedArcherTarget = null;
            } else if (this.isFollowCombatRecallActive() && this.updateMountedFollowMovement()) {
               this.mountedArcherTarget = null;
            } else if (this.validMountedTarget(target) || !this.shouldFollowCombatGroupFormation()) {
               if (!this.validMountedTarget(target) && this.updateMountedFollowMovement()) {
                  this.mountedArcherTarget = null;
               } else if (!this.validMountedTarget(target)) {
                  this.mountedArcherTarget = null;
                  mount.getNavigation().stop();
                  mount.setDeltaMovement(mount.getDeltaMovement().multiply(0.45, 1.0, 0.45));
               } else {
                  if (this.mountedArcherTarget != target) {
                     this.mountedArcherTarget = target;
                     this.mountedArcherClockwise = this.getRandom().nextBoolean();
                  }

                  Vec3 offset = mount.position().subtract(target.position());
                  Vec3 radial = new Vec3(offset.x, 0.0, offset.z);
                  double distance = radial.length();
                  if (distance < 0.001) {
                     radial = new Vec3(1.0, 0.0, 0.0);
                     distance = 1.0;
                  } else {
                     radial = radial.scale(1.0 / distance);
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

                  AttributeInstance movement = mount.getAttribute(Attributes.MOVEMENT_SPEED);
                  if (movement != null) {
                     movement.setBaseValue(Math.max(movement.getBaseValue(), 0.45));
                  }

                  Vec3 current = mount.getDeltaMovement();
                  mount.setDeltaMovement(direction.x * 0.45, current.y, direction.z * 0.45);
                  mount.getNavigation().stop();
                  this.faceMountedDirection(mount, direction);
                  this.faceRiderAtTarget(target);
               }
            }
         }
      }
   }

   private Vec3 mountedOrbitDirection(Vec3 radial, double correction, boolean clockwise) {
      Vec3 tangent = clockwise ? new Vec3(-radial.z, 0.0, radial.x) : new Vec3(radial.z, 0.0, -radial.x);
      return tangent.add(radial.scale(correction)).normalize();
   }

   private void faceMountedDirection(Mob mount, Vec3 direction) {
      float yaw = (float)(Math.atan2(direction.z, direction.x) * 180.0F / (float)Math.PI) - 90.0F;
      mount.setYRot(yaw);
      mount.setYBodyRot(yaw);
      mount.setYHeadRot(yaw);
   }

   private void faceRiderAtTarget(LivingEntity target) {
      Vec3 direction = target.position().subtract(this.position());
      Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
      if (!(horizontal.lengthSqr() < 1.0E-4)) {
         float yaw = (float)(Math.atan2(horizontal.z, horizontal.x) * 180.0F / (float)Math.PI) - 90.0F;
         this.setYRot(yaw);
         this.setYBodyRot(yaw);
         this.setYHeadRot(yaw);
      }
   }

   private boolean validMountedTarget(LivingEntity target) {
      return NpcTargeting.canHarm(this, target);
   }

   private void driveMountedCharge(Mob mount, Vec3 destination) {
      Vec3 delta = destination.subtract(mount.position());
      Vec3 horizontal = new Vec3(delta.x, 0.0, delta.z);
      if (!(horizontal.lengthSqr() < 1.0E-4)) {
         Vec3 direction = horizontal.normalize();
         if (!NpcMovementSafety.canMoveMountedSafely(mount, direction)) {
            this.stopMountedCharge(mount);
            this.mountPassDestination = null;
         } else {
            AttributeInstance movement = mount.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movement != null) {
               movement.setBaseValue(Math.max(movement.getBaseValue(), 0.45));
            }

            Vec3 current = mount.getDeltaMovement();
            mount.setDeltaMovement(direction.x * 0.45, current.y, direction.z * 0.45);
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

   private void stopMountedCharge(Mob mount) {
      mount.getNavigation().stop();
      mount.setDeltaMovement(mount.getDeltaMovement().multiply(0.45, 1.0, 0.45));
   }

   boolean hasActivePatrolDeployment() {
      return this.getCurrentCommand() == NpcCommandType.NONE
         && this.getTarget() == null
         && AWCoreRegistryAccess.is(this.getOrdersStack(), AWCoreIds.COMBAT_ORDER)
         && CombatOrderData.matchesDimension(this.getOrdersStack(), this.level())
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

      public boolean canUse() {
         return this.npc.canUseMeleeCombatGoal() && super.canUse();
      }

      public boolean canContinueToUse() {
         return this.npc.canUseMeleeCombatGoal() && (this.npc.isSwordsmanAerialRecovery() || super.canContinueToUse());
      }

      public void tick() {
         if (this.weaponCooldown > 0) {
            this.weaponCooldown--;
         }

         super.tick();
         LivingEntity target = this.npc.getTarget();
         if (target != null && !this.npc.isSwordsman()) {
            double holdDistance = NpcEquipmentResolver.meleeHoldDistance(this.npc);
            if (this.npc.distanceToSqr(target) <= holdDistance * holdDistance && this.npc.hasMeleeLineOfSight(target)) {
               this.npc.getNavigation().stop();
               this.npc.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
         }
      }

      protected void checkAndPerformAttack(LivingEntity target, double distanceSqr) {
         if (!this.npc.isSwordsmanComboActive()) {
            double attackReachSqr = this.npc.isSwordsman() ? 1024.0 : this.getAttackReachSqr(target);
            if (distanceSqr <= attackReachSqr && this.weaponCooldown <= 0 && this.npc.hasMeleeLineOfSight(target)) {
               boolean attacked = this.npc.doHurtTarget(target);
               if (attacked) {
                  this.weaponCooldown = this.npc.isSwordsman()
                     ? this.npc.getSwordsmanTechniqueCooldownTicks()
                     : NpcEquipmentResolver.meleeAttackInterval(this.npc);
               }
            }
         }
      }

      protected double getAttackReachSqr(LivingEntity target) {
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
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         this.target = this.npc.getTarget();
         this.npc.ensureRangedFollowRange();
         boolean usable = !this.npc.isLogicalDragonRider() && this.npc.isRangedCombatant() && this.validTarget();
         if (usable) {
            this.resetBallisticCheck();
         }

         return usable;
      }

      public boolean canContinueToUse() {
         this.npc.ensureRangedFollowRange();
         return !this.npc.isLogicalDragonRider() && this.npc.isRangedCombatant() && this.npc.getTarget() == this.target && this.validTarget();
      }

      public void start() {
         this.resetBallisticCheck();
      }

      public void tick() {
         this.target = this.npc.getTarget();
         if (!this.validTarget()) {
            this.stopUsingWeapon();
         } else {
            this.npc.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
            double distanceSqr = this.npc.distanceToSqr(this.target);
            if (this.cooldown > 0) {
               this.cooldown--;
            }

            if (this.npc.isUsingItem()) {
               if (this.shouldRetreat(distanceSqr)) {
                  this.continueRetreating();
               } else {
                  this.clearRetreat();
                  this.npc.getNavigation().stop();
               }

               int requiredDrawTicks = this.npc.getMainHandItem().getItem() instanceof CrossbowItem
                  ? NpcRangedAttackHelper.crossbowChargeDuration(this.npc.getMainHandItem())
                  : 20;
               if (++this.drawTicks >= requiredDrawTicks) {
                  this.npc.performRangedAttack(this.target, 1.0F);
                  this.stopUsingWeapon();
                  this.drawTicks = 0;
                  this.cooldown = 30;
               }
            } else if (!this.canShootTarget()) {
               this.stopUsingWeapon();
               this.clearRetreat();
               this.drawTicks = 0;
               if (distanceSqr <= 144.0) {
                  this.npc.getNavigation().moveTo(this.target, this.npc.playerSprintNavigationSpeed());
               } else {
                  this.npc.getNavigation().stop();
               }
            } else if (this.cooldown > 0 && this.shouldRetreat(distanceSqr)) {
               this.stopUsingWeapon();
               this.drawTicks = 0;
               this.continueRetreating();
            } else {
               this.clearRetreat();
               this.npc.getNavigation().stop();
               if (this.cooldown <= 0) {
                  this.npc.startUsingItem(InteractionHand.MAIN_HAND);
                  this.drawTicks = 0;
               }
            }
         }
      }

      public void stop() {
         this.stopUsingWeapon();
         this.npc.getNavigation().stop();
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
         if (this.npc.isUsingItem()) {
            this.npc.stopUsingItem();
         }
      }

      private void continueRetreating() {
         if (this.retreatRepathTicks > 0) {
            this.retreatRepathTicks--;
         }

         boolean reachedPoint = this.retreatPosition != null && this.npc.position().distanceToSqr(this.retreatPosition) < 2.25;
         if (this.retreatPosition == null || this.retreatRepathTicks <= 0 || reachedPoint || this.npc.getNavigation().isDone()) {
            Vec3 candidate = DefaultRandomPos.getPosAway(this.npc, 10, 4, this.target.position());
            if (candidate == null) {
               Vec3 away = this.npc.position().subtract(this.target.position());
               if (away.horizontalDistanceSqr() < 1.0E-4) {
                  return;
               }

               candidate = this.npc.position().add(away.normalize().scale(7.0));
            }

            this.retreatPosition = candidate;
            this.retreatRepathTicks = 12;
            this.npc.getNavigation().moveTo(candidate.x, candidate.y, candidate.z, this.npc.playerSprintNavigationSpeed());
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

      public boolean canUse() {
         if (!this.commander.isCommander()) {
            return false;
         } else if (this.cooldown > 0) {
            this.cooldown--;
            return false;
         } else {
            return true;
         }
      }

      public void start() {
         this.cooldown = 40;
         double range = 32.0;
         AttributeInstance followRange = this.commander.getAttribute(Attributes.FOLLOW_RANGE);
         if (followRange != null) {
            range = followRange.getValue();
         }

         AABB box = this.commander.getBoundingBox().inflate(range, range / 2.0, range);

         for (PlayerOwnedNpcEntity ally : this.commander.level().getEntitiesOfClass(PlayerOwnedNpcEntity.class, box, this::canBoost)) {
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0, true, false));
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, true, false));
         }
      }

      private boolean canBoost(PlayerOwnedNpcEntity ally) {
         return ally != null
            && ally.isAlive()
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
         this.setFlags(EnumSet.of(Flag.LOOK));
      }

      public boolean canUse() {
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

      public boolean canContinueToUse() {
         return this.medic.isMedic()
            && !this.medic.isGettingFood()
            && this.target != null
            && this.target.isAlive()
            && this.target.getHealth() < this.target.getMaxHealth()
            && this.medic.distanceToSqr(this.target) <= 64.0;
      }

      public void start() {
         this.cooldown = 40;
      }

      public void tick() {
         if (this.target != null) {
            this.medic.getLookControl().setLookAt(this.target);
            if (this.medic.distanceToSqr(this.target) > 36.0) {
               this.medic.getNavigation().moveTo(this.target, this.medic.playerWalkNavigationSpeed());
            } else {
               this.medic.getNavigation().stop();
               this.target.heal(2.0F);
               this.medic.swing(InteractionHand.MAIN_HAND);
               this.cooldown = 40;
               this.target = null;
            }
         }
      }

      public void stop() {
         this.target = null;
      }

      private LivingEntity findWoundedAlly() {
         AABB box = this.medic.getBoundingBox().inflate(8.0);
         return this.medic
            .level()
            .getEntitiesOfClass(LivingEntity.class, box, this::canHeal)
            .stream()
            .min((first, second) -> Double.compare(this.medic.distanceToSqr(first), this.medic.distanceToSqr(second)))
            .orElse(null);
      }

      private boolean canHeal(LivingEntity entity) {
         if (entity == null || !entity.isAlive() || entity == this.medic || entity.getHealth() >= entity.getMaxHealth()) {
            return false;
         } else if (entity instanceof Player player) {
            return this.medic.getOwner().map(player.getUUID()::equals).orElse(false);
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
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (this.npc.getCurrentCommand() != NpcCommandType.MOVE) {
            return false;
         } else {
            this.target = this.npc.getCommandTarget();
            return !BlockPos.ZERO.equals(this.target) && this.distanceToTargetSq() > this.stopDistanceSq;
         }
      }

      public boolean canContinueToUse() {
         return this.npc.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() > this.stopDistanceSq;
      }

      public void tick() {
         this.npc.getLookControl().setLookAt((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5);
         this.npc
            .getNavigation()
            .moveTo(
               (double)this.target.getX() + 0.5,
               (double)this.target.getY(),
               (double)this.target.getZ() + 0.5,
               this.npc.playerSprintNavigationSpeed()
            );
      }

      public void stop() {
         if (this.npc.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() <= this.stopDistanceSq) {
            this.npc.clearCommand();
         }

         this.target = null;
         this.npc.getNavigation().stop();
      }

      private double distanceToTargetSq() {
         return this.npc.distanceToSqr((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5);
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
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (this.npc.getCurrentCommand() == NpcCommandType.NONE && this.npc.getTarget() == null) {
            if (AWCoreRegistryAccess.is(this.npc.getOrdersStack(), AWCoreIds.COMBAT_ORDER)
               && CombatOrderData.matchesDimension(this.npc.getOrdersStack(), this.npc.level())) {
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

      public boolean canContinueToUse() {
         return this.canUse();
      }

      public void tick() {
         BlockPos pos = this.entries.get(this.npc.patrolIndex);
         double distance = this.npc.distanceToSqr((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5);
         if (this.npc.patrolAtPoint) {
            this.npc.getNavigation().stop();
            this.npc.patrolTicksAtPoint++;
            if (this.npc.patrolTicksAtPoint > 50) {
               this.moveToNextPoint();
            }
         } else {
            if (distance > 4.0) {
               this.npc.getLookControl().setLookAt((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5);
               if (this.npc.getVehicle() instanceof Mob mount) {
                  this.npc.patrolDrivingMount = true;
                  mount.getNavigation().moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, this.speedModifier);
               } else {
                  this.npc.patrolDrivingMount = false;
                  this.npc
                     .getNavigation()
                     .moveTo((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, this.npc.playerWalkNavigationSpeed());
               }
            } else {
               this.npc.patrolDrivingMount = false;
               this.npc.patrolAtPoint = true;
               this.npc.patrolTicksAtPoint = 0;
            }
         }
      }

      public void stop() {
         this.npc.patrolTicksAtPoint = 0;
         this.npc.patrolDrivingMount = false;
         this.npc.getNavigation().stop();
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

      public boolean canUse() {
         LivingEntity attacker = this.npc.getLastHurtByMob();
         return NpcTargeting.canHarm(this.npc, attacker) && super.canUse();
      }

      public void start() {
         super.start();
         LivingEntity attacker = this.npc.getLastHurtByMob();
         if (NpcTargeting.canHarm(this.npc, attacker)) {
            this.npc
               .level()
               .getEntitiesOfClass(
                  CombatNpcEntity.class,
                  this.npc.getBoundingBox().inflate(64.0, 16.0, 64.0),
                  ally -> ally != this.npc && ally.isAlive() && ally.getOwner().equals(this.npc.getOwner()) && NpcTargeting.canHarm(ally, attacker)
               )
               .forEach(ally -> {
                  ally.setLastHurtByMob(attacker);
                  ally.setTarget(attacker);
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
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (!this.canReturnHome()) {
            return false;
         } else {
            this.home = this.npc.getHomePos();
            return this.distanceToHomeSq() > 9.0;
         }
      }

      public boolean canContinueToUse() {
         return this.home != null && this.canReturnHome() && this.distanceToHomeSq() > 9.0;
      }

      public void start() {
         this.repathTicks = 0;
      }

      public void tick() {
         this.npc.getLookControl().setLookAt((double)this.home.getX() + 0.5, (double)this.home.getY(), (double)this.home.getZ() + 0.5);
         if (this.repathTicks-- <= 0 || this.npc.getNavigation().isDone()) {
            this.npc
               .getNavigation()
               .moveTo((double)this.home.getX() + 0.5, (double)this.home.getY(), (double)this.home.getZ() + 0.5, this.speedModifier);
            this.repathTicks = 10;
         }
      }

      public void stop() {
         this.repathTicks = 0;
         this.home = null;
         this.npc.getNavigation().stop();
      }

      private boolean canReturnHome() {
         if (this.npc.hasHome()
            && !this.npc.isPassenger()
            && !this.npc.isGuiInteractionPaused()
            && !this.npc.isGettingFood()
            && this.npc.getCurrentCommand() == NpcCommandType.NONE
            && !this.npc.shouldFollowCombatGroupFormation()) {
            LivingEntity target = this.npc.getTarget();
            return target != null && target.isAlive() ? false : !this.hasActivePatrolOrder();
         } else {
            return false;
         }
      }

      private boolean hasActivePatrolOrder() {
         return AWCoreRegistryAccess.is(this.npc.getOrdersStack(), AWCoreIds.COMBAT_ORDER)
            && CombatOrderData.matchesDimension(this.npc.getOrdersStack(), this.npc.level())
            && !CombatOrderData.getEntries(this.npc.getOrdersStack()).isEmpty();
      }

      private double distanceToHomeSq() {
         return this.npc.distanceToSqr((double)this.home.getX() + 0.5, (double)this.home.getY(), (double)this.home.getZ() + 0.5);
      }
   }

   private static class ShieldDefenseGoal extends Goal {
      private static final int SHIELD_LINGER_TICKS = 20;
      private final CombatNpcEntity npc;
      private int lingerTicks;

      private ShieldDefenseGoal(CombatNpcEntity npc) {
         this.npc = npc;
      }

      public boolean canUse() {
         return "soldier".equals(this.npc.getNpcSubtype()) && !this.npc.isPassenger() && NpcShielding.shouldRaiseShield(this.npc, this.npc.getTarget());
      }

      public boolean canContinueToUse() {
         if (this.canUse()) {
            this.lingerTicks = 20;
            return true;
         } else {
            return this.lingerTicks-- > 0;
         }
      }

      public void start() {
         this.lingerTicks = 20;
         this.npc.setShielding(true);
      }

      public void tick() {
         LivingEntity target = this.npc.getTarget();
         if (target != null) {
            this.npc.getLookControl().setLookAt(target);
         }

         if (NpcShielding.shouldRaiseShield(this.npc, this.npc.getTarget())) {
            this.lingerTicks = 20;
         }

         this.npc.setShielding(this.lingerTicks > 0);
      }

      public void stop() {
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
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (this.npc.isSwordsman() && SlashBladeCompat.canUseThornsGuard(this.npc)) {
            this.threat = NpcShielding.findIncomingProjectile(this.npc);
            return this.threat != null;
         } else {
            return false;
         }
      }

      public boolean canContinueToUse() {
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

      public void start() {
         this.guardLingerTicks = 4;
         this.npc.cancelSwordsmanCombo();
         this.npc.getNavigation().stop();
         SlashBladeCompat.setThornsGuardInput(this.npc, true);
      }

      public void tick() {
         this.npc.getNavigation().stop();
         Vec3 motion = this.npc.getDeltaMovement();
         this.npc.setDeltaMovement(0.0, motion.y, 0.0);
         if (this.threat != null && this.threat.isAlive()) {
            this.npc.getLookControl().setLookAt(this.threat.getX(), this.threat.getY(), this.threat.getZ(), 90.0F, 90.0F);
            Vec3 direction = this.threat.position().subtract(this.npc.position());
            if (direction.horizontalDistanceSqr() > 1.0E-5) {
               float yaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
               this.npc.setYRot(yaw);
               this.npc.setYBodyRot(yaw);
               this.npc.setYHeadRot(yaw);
            }
         }

         SlashBladeCompat.setThornsGuardInput(this.npc, true);
      }

      public void stop() {
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
