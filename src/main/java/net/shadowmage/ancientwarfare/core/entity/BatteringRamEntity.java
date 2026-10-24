package net.shadowmage.ancientwarfare.core.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.shadowmage.ancientwarfare.core.block.GateProxyBlock;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.faction.FactionDiplomacy;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.npc.NpcOwnerAccess;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargeting;
import net.shadowmage.ancientwarfare.core.common.siege.SiegeWeaponControl;
import net.shadowmage.ancientwarfare.core.item.BatteringRamItem;
import net.shadowmage.ancientwarfare.core.structure.NaturalGateTerritory;
import net.shadowmage.ancientwarfare.core.tile.GateProxyBlockEntity;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.object.PlayState;

public class BatteringRamEntity extends ChestPushcartEntity implements SiegeWeaponControl {
   private static final Component MENU_TITLE = Component.translatable("container.ancientwarfare.battering_ram");
   private static final RawAnimation WHEEL_ROLL = RawAnimation.begin().thenLoop("animation.ancientwarfare.legacy.battering_ram.wheel_rotations");
   private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.ancientwarfare.legacy.battering_ram.firing_cycle");
   private static final EntityDataAccessor<Integer> ATTACK_ANIMATION_TICKS = SynchedEntityData.defineId(
      BatteringRamEntity.class, EntityDataSerializers.INT
   );
   private static final int ATTACK_COOLDOWN_TICKS = 30;
   private static final int ATTACK_ANIMATION_LENGTH_TICKS = 16;
   private static final float GATE_DAMAGE_MULTIPLIER = 10.0F;
   private int attackCooldown;
   private int previousAttackAnimationTicks;
   private int npcDriveTicks;
   private int npcCombatTicks;
   private GateEntity npcGateTarget;
   private LivingEntity npcGateEnemy;

   public BatteringRamEntity(EntityType<? extends BatteringRamEntity> type, Level level) {
      super(type, level);
   }

   @Override
   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(ATTACK_ANIMATION_TICKS, 0);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.attackCooldown > 0) {
         this.attackCooldown--;
      }

      if (this.getAttackAnimationTicks() > 0) {
         this.entityData.set(ATTACK_ANIMATION_TICKS, this.getAttackAnimationTicks() - 1);
      }
   }

   public int getAttackAnimationTicks() {
      return (Integer)this.entityData.get(ATTACK_ANIMATION_TICKS);
   }

   @Override
   protected void updateVehicleMotion() {
      if (this.getControllingPassenger() instanceof NpcBaseEntity npc) {
         if (!this.isNpcOperationPaused(npc) && this.npcDriveTicks > 0) {
            this.npcDriveTicks--;
            if (this.npcCombatTicks > 0) {
               this.npcCombatTicks--;
            }
         } else {
            this.stopNpcControl();
         }
      }

      super.updateVehicleMotion();
   }

   private boolean isNpcOperationPaused(PathfinderMob engineer) {
      if (engineer instanceof PlayerOwnedNpcEntity owned
         && (owned.isGuiInteractionPaused() || owned.isGettingFood() || owned.isFollowCombatRecallActive() || owned.getCurrentCommand() == NpcCommandType.MOVE)
         )
       {
         return true;
      }

      return false;
   }

   void stopNpcControl() {
      this.setVehicleInput(0.0F, 0.0F);
      this.npcDriveTicks = 0;
      this.npcCombatTicks = 0;
   }

   boolean hasNpcCombatControl(PathfinderMob engineer) {
      return this.getControllingPassenger() == engineer && this.npcCombatTicks > 0 && !this.isNpcOperationPaused(engineer);
   }

   void setNpcFormationInput(PathfinderMob engineer, float forward, float turn) {
      if (this.getControllingPassenger() == engineer) {
         if (this.isNpcOperationPaused(engineer)) {
            this.stopNpcControl();
         } else if (!this.hasNpcCombatControl(engineer)) {
            this.setVehicleInput(forward, turn);
            this.npcDriveTicks = 2;
         }
      }
   }

   @Override
   public boolean canBeOperatedBy(PathfinderMob engineer) {
      return SiegeWeaponNpcControl.canOperate(this, engineer);
   }

   @Override
   public boolean beginOperation(PathfinderMob engineer) {
      if (!SiegeWeaponNpcControl.beginOperation(this, engineer)) {
         return false;
      } else {
         this.stopNpcControl();
         this.npcGateTarget = null;
         this.npcGateEnemy = null;
         return true;
      }
   }

   @Override
   public void stopOperation(PathfinderMob engineer) {
      if (this.getControllingPassenger() == engineer) {
         this.stopNpcControl();
         this.npcGateTarget = null;
         this.npcGateEnemy = null;
      }

      SiegeWeaponNpcControl.stopOperation(this, engineer);
   }

   @Override
   public boolean isMobile() {
      return true;
   }

   @Override
   public double minimumAttackRange() {
      return 0.0;
   }

   @Override
   public double maximumAttackRange() {
      return 3.5;
   }

   private AABB attackBox() {
      double yaw = Math.toRadians((double)this.getYRot());
      Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
      Vec3 center = this.position().add(forward.scale(1.75)).add(0.0, 0.9, 0.0);
      return new AABB(
         center.x - 1.15, center.y - 0.8, center.z - 1.15, center.x + 1.15, center.y + 0.8, center.z + 1.15
      );
   }

   private BlockHitResult obstacleTowards(LivingEntity target) {
      return this.obstacleTowards(new Vec3(target.getX(), target.getY(0.5), target.getZ()));
   }

   private BlockHitResult obstacleTowards(Vec3 destination) {
      return this.level().clip(new ClipContext(this.position().add(0.0, 0.9, 0.0), destination, Block.COLLIDER, Fluid.NONE, this));
   }

   private GateEntity gateAtHit(BlockHitResult hit) {
      if (this.level() instanceof ServerLevel server && hit.getType() == Type.BLOCK) {
         BlockState state = this.level().getBlockState(hit.getBlockPos());
         if (state.getBlock() instanceof GateProxyBlock && !(Boolean)state.getValue(GateProxyBlock.OPEN)) {
            if (!(this.level().getBlockEntity(hit.getBlockPos()) instanceof GateProxyBlockEntity proxy)) {
               return null;
            }

            if (proxy.getGate(server) instanceof GateEntity gate && gate.isClosedForSiege() && gate.ownsProxyPosition(hit.getBlockPos())) {
               return gate;
            }

            return null;
         }

         return null;
      }

      return null;
   }

   private GateEntity blockingGate(PathfinderMob engineer, LivingEntity target) {
      if (!NpcTargeting.canHarm(engineer, target)) {
         return null;
      } else {
         GateEntity gate = this.gateAtHit(this.obstacleTowards(target));
         return gate != null && this.canDamageGate(engineer, target, gate) ? gate : null;
      }
   }

   private boolean canDamageGate(PathfinderMob engineer, LivingEntity target, GateEntity gate) {
      if (this.level() instanceof ServerLevel server && NpcTargeting.canHarm(engineer, gate)) {
         UUID owner = gate.getOwnerUUID();
         if (owner == null) {
            owner = NaturalGateTerritory.claimedOwner(server, gate);
            if (owner == null && NaturalGateTerritory.hasClaimedTerritory(server, gate)) {
               return false;
            }
         }

         if (owner != null) {
            if (engineer instanceof NpcOwnerAccess owned && owned.getOwner().filter(owner::equals).isPresent()) {
               return false;
            }

            Player ownerPlayer = this.level().getPlayerByUUID(owner);
            if (ownerPlayer != null && !NpcTargeting.canHarm(engineer, ownerPlayer)) {
               return false;
            }

            if (target == null
               || !NpcTargeting.canHarm(engineer, target)
               || !owner.equals(target.getUUID()) && (!(target instanceof NpcOwnerAccess owned) || !owned.getOwner().filter(owner::equals).isPresent())) {
               if (engineer instanceof FactionNpcEntity faction
                  && FactionStandingData.get(server).isHostile(owner, faction.getFactionName())) {
                  return true;
               }

               return false;
            }

            return true;
         }

         String gateFaction = NaturalGateTerritory.faction(server, gate);
         if (!gateFaction.isEmpty()) {
            if (engineer instanceof FactionNpcEntity faction) {
               return FactionDiplomacy.isHostile(server, faction.getFactionName(), gateFaction);
            }

            if (engineer instanceof NpcOwnerAccess owned
               && owned.getOwner()
                  .map(playerOwner -> FactionStandingData.get(server).isHostile(playerOwner, gateFaction))
                  .orElse(false)) {
               return true;
            }

            return false;
         }

         return target != null && NpcTargeting.canHarm(engineer, target) && this.gateAtHit(this.obstacleTowards(target)) == gate;
      }

      return false;
   }

   private Vec3 gateAimPoint(GateEntity gate) {
      AABB bounds = gate.getBoundingBox();
      return new Vec3(
         Math.max(bounds.minX + 0.05, Math.min(bounds.maxX - 0.05, this.getX())),
         Math.max(bounds.minY + 0.05, Math.min(bounds.maxY - 0.05, this.getY() + 0.9)),
         Math.max(bounds.minZ + 0.05, Math.min(bounds.maxZ - 0.05, this.getZ()))
      );
   }

   private boolean hasClearGateApproach(GateEntity gate) {
      return this.gateAtHit(this.obstacleTowards(this.gateAimPoint(gate))) == gate;
   }

   private boolean isValidGateTarget(NpcBaseEntity engineer, GateEntity gate, LivingEntity enemy, Predicate<LivingEntity> targetFilter) {
      if (gate != null && gate.isAlive() && gate.level() == this.level() && gate.isClosedForSiege() && !this.isNpcOperationPaused(engineer)) {
         Vec3 delta = this.gateAimPoint(gate).subtract(this.position());
         if (delta.x * delta.x + delta.y * delta.y + delta.z * delta.z > 9216.0) {
            return false;
         } else {
            return enemy == null
                  || enemy.level() == this.level()
                     && NpcTargeting.canHarm(engineer, enemy)
                     && targetFilter.test(enemy)
                     && !(engineer.distanceToSqr(enemy) > 9216.0)
               ? this.canDamageGate(engineer, enemy, gate) && this.hasClearGateApproach(gate)
               : false;
         }
      } else {
         return false;
      }
   }

   GateEntity chooseGateTarget(NpcBaseEntity engineer, Predicate<LivingEntity> targetFilter) {
      if (this.getControllingPassenger() == engineer && !this.isNpcOperationPaused(engineer)) {
         AABB search = this.getBoundingBox().inflate(96.0);
         if (this.isValidGateTarget(engineer, this.npcGateTarget, null, targetFilter)) {
            this.npcGateEnemy = null;
            return this.npcGateTarget;
         } else if (this.isValidGateTarget(engineer, this.npcGateTarget, this.npcGateEnemy, targetFilter)) {
            return this.npcGateTarget;
         } else {
            List<LivingEntity> enemies = this.level()
               .getEntitiesOfClass(
                  LivingEntity.class,
                  search,
                  enemyx -> NpcTargeting.canHarm(engineer, enemyx) && targetFilter.test(enemyx) && engineer.distanceToSqr(enemyx) <= 9216.0
               );
            List<GateEntity> gates = this.level().getEntitiesOfClass(GateEntity.class, search, Entity::isAlive).stream().sorted(Comparator.comparingDouble(gatex -> {
               Vec3 delta = this.gateAimPoint(gatex).subtract(this.position());
               return delta.x * delta.x + delta.y * delta.y + delta.z * delta.z;
            })).toList();
            if (this.npcGateTarget != null && gates.contains(this.npcGateTarget)) {
               ArrayList<GateEntity> ordered = new ArrayList<>(gates);
               ordered.remove(this.npcGateTarget);
               ordered.add(0, this.npcGateTarget);
               gates = ordered;
            }

            for (GateEntity gate : gates) {
               if (this.isValidGateTarget(engineer, gate, null, targetFilter)) {
                  this.npcGateTarget = gate;
                  this.npcGateEnemy = null;
                  return gate;
               }

               for (LivingEntity enemy : enemies) {
                  if (this.isValidGateTarget(engineer, gate, enemy, targetFilter)) {
                     this.npcGateTarget = gate;
                     this.npcGateEnemy = enemy;
                     return gate;
                  }
               }
            }

            this.npcGateTarget = null;
            this.npcGateEnemy = null;
            return null;
         }
      } else {
         this.npcGateTarget = null;
         this.npcGateEnemy = null;
         if (this.getControllingPassenger() == engineer) {
            this.stopNpcControl();
         }

         return null;
      }
   }

   void aimAtGate(PathfinderMob engineer, GateEntity gate) {
      if (this.getControllingPassenger() == engineer) {
         if (engineer instanceof NpcBaseEntity npc && gate == this.npcGateTarget && this.isValidGateTarget(npc, gate, this.npcGateEnemy, target -> true)) {
            this.aimTowards(this.gateAimPoint(gate), gate.getBoundingBox());
            return;
         }

         this.stopNpcControl();
      }
   }

   boolean fireAtGate(PathfinderMob engineer, GateEntity gate) {
      if (this.getControllingPassenger() != engineer) {
         return false;
      } else if (this.isNpcOperationPaused(engineer)) {
         this.stopNpcControl();
         return false;
      } else {
         if (engineer instanceof NpcBaseEntity npc
            && gate == this.npcGateTarget
            && this.isValidGateTarget(npc, gate, this.npcGateEnemy, target -> true)
            && this.attackBox().intersects(gate.getBoundingBox())) {
            return this.attack(engineer, this.npcGateEnemy, gate);
         }

         return false;
      }
   }

   @Override
   public boolean canAttackTarget(LivingEntity target) {
      if (this.getControllingPassenger() instanceof PathfinderMob engineer && !this.isNpcOperationPaused(engineer) && NpcTargeting.canHarm(engineer, target)) {
         GateEntity gate = this.blockingGate(engineer, target);
         return gate != null
            ? this.attackBox().intersects(gate.getBoundingBox())
            : this.obstacleTowards(target).getType() == Type.MISS && this.attackBox().intersects(target.getBoundingBox());
      }

      return false;
   }

   @Override
   public void aimAt(PathfinderMob engineer, LivingEntity target) {
      if (this.getControllingPassenger() == engineer && !this.isNpcOperationPaused(engineer) && NpcTargeting.canHarm(engineer, target)) {
         GateEntity gate = this.blockingGate(engineer, target);
         Vec3 aim = gate == null ? target.position() : this.obstacleTowards(target).getLocation();
         this.aimTowards(aim, gate != null ? gate.getBoundingBox() : target.getBoundingBox());
      } else {
         if (this.getControllingPassenger() == engineer) {
            this.stopNpcControl();
         }
      }
   }

   private void aimTowards(Vec3 aim, AABB targetBounds) {
      Vec3 delta = aim.subtract(this.position());
      if (delta.horizontalDistanceSqr() > 1.0E-6) {
         SiegeWeaponNpcControl.faceVehicle(this, (float)Math.toDegrees(Math.atan2(-delta.x, delta.z)));
      }

      boolean inReach = this.attackBox().intersects(targetBounds);
      float forward = inReach ? 0.0F : (delta.horizontalDistanceSqr() < 2.25 ? -1.0F : 1.0F);
      this.setVehicleInput(forward, 0.0F);
      this.npcDriveTicks = 2;
      this.npcCombatTicks = 2;
   }

   @Override
   public boolean fireAt(PathfinderMob engineer, LivingEntity target) {
      if (this.getControllingPassenger() != engineer) {
         return false;
      } else if (this.isNpcOperationPaused(engineer)) {
         this.stopNpcControl();
         return false;
      } else {
         return !this.canAttackTarget(target) ? false : this.attack(engineer, target, this.blockingGate(engineer, target));
      }
   }

   public boolean tryAttack() {
      return this.attack(this.getControllingPassenger(), null, null);
   }

   private boolean attack(LivingEntity rider, LivingEntity npcTarget, GateEntity npcGate) {
      if (!this.level().isClientSide && this.attackCooldown <= 0) {
         AABB attackBox = this.attackBox();
         boolean manualAttack = npcTarget == null && npcGate == null;
         DamageSource source = rider != null ? this.damageSources().mobAttack(rider) : this.damageSources().generic();
         float damage = attackDamageForLevel(this.getMaterialLevel());
         List<LivingEntity> targets = this.level()
            .getEntitiesOfClass(
               LivingEntity.class,
               attackBox,
               target -> target != rider
                     && !this.getPassengers().contains(target)
                     && target.isAlive()
                     && (manualAttack || npcGate == null && target == npcTarget && NpcTargeting.canHarm(rider, target))
            );
         boolean hit = false;

         for (LivingEntity target : targets) {
            hit |= target.hurt(source, damage);
         }

         for (GateEntity gate : this.level().getEntitiesOfClass(GateEntity.class, attackBox, Entity::isAlive)) {
            if (manualAttack || gate == npcGate && rider instanceof PathfinderMob engineer && this.canDamageGate(engineer, npcTarget, gate)) {
               hit |= gate.hurt(source, damage * 10.0F);
            }
         }

         this.attackCooldown = 30;
         this.entityData.set(ATTACK_ANIMATION_TICKS, 16);
         return hit;
      } else {
         return false;
      }
   }

   private static float attackDamageForLevel(int level) {
      return switch (Math.max(0, Math.min(4, level))) {
         case 1 -> 14.0F;
         case 2 -> 18.0F;
         case 3 -> 24.0F;
         case 4 -> 32.0F;
         default -> 10.0F;
      };
   }

   @Override
   protected float maxHealthForLevel(int level) {
      return VehicleDurability.attackHealth(level);
   }

   @Override
   protected float armorForLevel(int level) {
      return VehicleDurability.attackArmor(level);
   }

   @Override
   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController[]{new AnimationController(this, "wheel", 0, this::wheelAnimation)});
      controllers.add(new AnimationController[]{new AnimationController(this, "attack", 0, this::attackAnimation)});
   }

   private PlayState wheelAnimation(AnimationState<BatteringRamEntity> state) {
      if (this.getDeltaMovement().horizontalDistanceSqr() > 4.0E-4) {
         state.setAnimation(WHEEL_ROLL);
         return PlayState.CONTINUE;
      } else {
         return PlayState.STOP;
      }
   }

   private PlayState attackAnimation(AnimationState<BatteringRamEntity> state) {
      int ticks = this.getAttackAnimationTicks();
      if (ticks > 0) {
         if (this.previousAttackAnimationTicks <= 0) {
            state.getController().forceAnimationReset();
         }

         this.previousAttackAnimationTicks = ticks;
         state.setAnimation(ATTACK);
         return PlayState.CONTINUE;
      } else {
         this.previousAttackAnimationTicks = 0;
         return PlayState.STOP;
      }
   }

   @Override
   protected Component getMenuTitle() {
      return MENU_TITLE;
   }

   @Override
   protected Vec3 getPassengerOffset() {
      return new Vec3(0.0, 0.65, 1.8);
   }

   @Override
   protected void positionRider(Entity passenger, MoveFunction moveFunction) {
      if (this.hasPassenger(passenger)) {
         Vec3 offset = this.getPassengerOffset().yRot(-this.getYRot() * (float) (Math.PI / 180.0));
         moveFunction.accept(passenger, this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);
      }
   }

   @Override
   protected ItemStack createPackedItem() {
      ItemStack stack = new ItemStack(BatteringRamItem.itemForMaterialLevel(this.getMaterialLevel()));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.getOrCreateTag().put("BatteringRamData", tag);
      return stack;
   }
}
