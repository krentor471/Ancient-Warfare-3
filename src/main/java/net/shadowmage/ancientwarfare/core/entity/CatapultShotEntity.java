package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.item.CatapultAmmoType;
import net.shadowmage.ancientwarfare.core.tile.GateProxyBlockEntity;

public class CatapultShotEntity extends BlockGraceSiegeProjectileEntity {
   private static final EntityDataAccessor<Integer> AMMO_TYPE = SynchedEntityData.defineId(CatapultShotEntity.class, EntityDataSerializers.INT);

   public CatapultShotEntity(EntityType<? extends CatapultShotEntity> type, Level level) {
      super(type, level);
   }

   public CatapultShotEntity(Level level, LivingEntity shooter, CatapultAmmoType ammoType) {
      super(AWCoreRegistryAccess.entityType(AWCoreIds.CATAPULT_SHOT), shooter, level);
      this.setAmmoType(ammoType);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(AMMO_TYPE, CatapultAmmoType.STONE_10.ordinal());
   }

   public CatapultAmmoType getAmmoType() {
      int index = (Integer)this.entityData.get(AMMO_TYPE);
      CatapultAmmoType[] values = CatapultAmmoType.values();
      return values[Math.max(0, Math.min(values.length - 1, index))];
   }

   public void setAmmoType(CatapultAmmoType ammoType) {
      this.entityData.set(AMMO_TYPE, ammoType.ordinal());
   }

   protected Item getDefaultItem() {
      return AWCoreRegistryAccess.item(AWCoreIds.AMMO_STONE_SHOT_10);
   }

   protected boolean canHitEntity(Entity entity) {
      Entity owner = this.getOwner();
      return entity != owner && (owner == null || entity != owner.getVehicle()) ? super.canHitEntity(entity) : false;
   }

   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide) {
         Entity target = result.getEntity();
         CatapultAmmoType ammoType = this.getAmmoType();
         if (ammoType.damage() > 0.0F) {
            target.hurt(this.damageSources().thrown(this, this.getOwner()), ammoType.damage());
         }

         if (ammoType.flaming()) {
            target.setSecondsOnFire(ammoType.spreadsNapalm() ? 8 : 4);
         }

         this.applyImpactEffects(this.blockPosition(), result);
         this.discard();
      }
   }

   protected void onHitBlock(BlockHitResult result) {
      if (!this.ignoresBlockHit(result)) {
         super.onHitBlock(result);
         if (!this.level().isClientSide) {
            GateProxyBlockEntity.damageGate(this.level(), result.getBlockPos(), this.damageSources().thrown(this, this.getOwner()), this.getAmmoType().damage());
            this.applyImpactEffects(result.getBlockPos().relative(result.getDirection()), result);
            this.discard();
         }
      }
   }

   private void applyImpactEffects(BlockPos center, HitResult result) {
      CatapultAmmoType ammoType = this.getAmmoType();
      if (ammoType.explosive()) {
         this.level().explode(this.getOwner(), this.getX(), this.getY(), this.getZ(), ammoType.explosionPower(), ExplosionInteraction.TNT);
      } else {
         if (ammoType.ignitesArea()) {
            this.placeFire(center, ammoType.areaTier());
         } else if (ammoType.spreadsNapalm()) {
            this.placeFire(center, ammoType.areaTier() + 1);
         } else if (ammoType.breaksBlocks()) {
            this.breakSoftBlocks(center, ammoType.areaTier());
         } else if (ammoType.spawnsSubmunitionsOnLaunch() || ammoType.spawnsSubmunitionsOnImpact()) {
            this.scatterImpact(center, ammoType.submunitionCount());
         }
      }
   }

   private void placeFire(BlockPos center, int radius) {
      int clampedRadius = Math.max(1, Math.min(5, radius));

      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-clampedRadius, -1, -clampedRadius), center.offset(clampedRadius, 1, clampedRadius))) {
         BlockPos firePos = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
         if (this.level().getBlockState(firePos).isAir() && this.level().getBlockState(firePos.below()).isSolidRender(this.level(), firePos.below())) {
            this.level().setBlock(firePos, Blocks.FIRE.defaultBlockState(), 11);
         }
      }
   }

   private void breakSoftBlocks(BlockPos center, int radius) {
      int clampedRadius = Math.max(1, Math.min(3, radius));

      for (BlockPos pos : BlockPos.betweenClosed(
         center.offset(-clampedRadius, -clampedRadius, -clampedRadius), center.offset(clampedRadius, clampedRadius, clampedRadius)
      )) {
         BlockPos target = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
         BlockState state = this.level().getBlockState(target);
         if (!state.isAir() && state.getDestroySpeed(this.level(), target) >= 0.0F && state.getDestroySpeed(this.level(), target) <= 2.0F) {
            if (this.level() instanceof net.minecraft.server.level.ServerLevel server) {
               net.shadowmage.ancientwarfare.core.faction.FactionRepairs.record(server, target, java.util.List.of(target));
            }

            this.level().destroyBlock(target, true, this);
         }
      }
   }

   private void scatterImpact(BlockPos center, int count) {
      int radius = Math.max(1, Math.min(4, count / 8));

      for (int i = 0; i < Math.min(12, count); i++) {
         BlockPos pos = center.offset(this.random.nextInt(radius * 2 + 1) - radius, 0, this.random.nextInt(radius * 2 + 1) - radius);
         Entity owner = this.getOwner();

         for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(1.25))) {
            if (living != owner) {
               living.hurt(this.damageSources().thrown(this, owner), 3.0F);
            }
         }
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putString("AmmoType", this.getAmmoType().name());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.contains("AmmoType")) {
         try {
            this.setAmmoType(CatapultAmmoType.valueOf(tag.getString("AmmoType")));
         } catch (IllegalArgumentException var3) {
            this.setAmmoType(CatapultAmmoType.STONE_10);
         }
      }
   }
}
