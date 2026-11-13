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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.item.CannonAmmoType;
import net.shadowmage.ancientwarfare.core.tile.GateProxyBlockEntity;

public class CannonShotEntity extends BlockGraceSiegeProjectileEntity {
   private static final EntityDataAccessor<Integer> AMMO_TYPE = SynchedEntityData.defineId(CannonShotEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> PELLET = SynchedEntityData.defineId(CannonShotEntity.class, EntityDataSerializers.BOOLEAN);

   public CannonShotEntity(EntityType<? extends CannonShotEntity> type, Level level) {
      super(type, level);
   }

   public CannonShotEntity(Level level, LivingEntity shooter, CannonAmmoType ammoType, boolean pellet) {
      super(AWCoreRegistryAccess.entityType(AWCoreIds.CANNON_SHOT), shooter, level);
      this.setAmmoType(ammoType);
      this.setPellet(pellet);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(AMMO_TYPE, CannonAmmoType.IRON_10.ordinal());
      this.entityData.define(PELLET, false);
   }

   public CannonAmmoType getAmmoType() {
      int index = (Integer)this.entityData.get(AMMO_TYPE);
      CannonAmmoType[] values = CannonAmmoType.values();
      return values[Math.max(0, Math.min(values.length - 1, index))];
   }

   public void setAmmoType(CannonAmmoType ammoType) {
      this.entityData.set(AMMO_TYPE, ammoType.ordinal());
   }

   public boolean isPellet() {
      return (Boolean)this.entityData.get(PELLET);
   }

   public void setPellet(boolean pellet) {
      this.entityData.set(PELLET, pellet);
   }

   protected Item getDefaultItem() {
      return AWCoreRegistryAccess.item(AWCoreIds.AMMO_IRON_SHOT_10);
   }

   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide) {
         Entity target = result.getEntity();
         CannonAmmoType ammoType = this.getAmmoType();
         float damage = this.isPellet() ? Math.max(3.0F, ammoType.damage() * 0.35F) : ammoType.damage();
         if (damage > 0.0F) {
            target.hurt(this.damageSources().thrown(this, this.getOwner()), damage);
         }

         this.applyImpactEffects(this.blockPosition(), result);
         this.discard();
      }
   }

   protected void onHitBlock(BlockHitResult result) {
      if (!this.ignoresBlockHit(result)) {
         super.onHitBlock(result);
         if (!this.level().isClientSide) {
            float damage = this.isPellet() ? Math.max(3.0F, this.getAmmoType().damage() * 0.35F) : this.getAmmoType().damage();
            GateProxyBlockEntity.damageGate(this.level(), result.getBlockPos(), this.damageSources().thrown(this, this.getOwner()), damage);
            this.applyImpactEffects(result.getBlockPos().relative(result.getDirection()), result);
            this.discard();
         }
      }
   }

   private void applyImpactEffects(BlockPos center, HitResult result) {
      CannonAmmoType ammoType = this.getAmmoType();
      if (!this.isPellet() && ammoType.pelletsOnImpact()) {
         this.spawnImpactPellets(ammoType);
      } else if (!this.isPellet() && ammoType.breaksBlocks()) {
         this.breakSoftBlocks(center, ammoType.weight() >= 15 ? 2 : 1);
      }
   }

   private void spawnImpactPellets(CannonAmmoType ammoType) {
      LivingEntity shooter = this.getOwner() instanceof LivingEntity living ? living : null;
      int count = Math.min(32, Math.max(1, ammoType.pelletCount()));

      for (int i = 0; i < count; i++) {
         CannonShotEntity pellet = new CannonShotEntity(this.level(), shooter, ammoType, true);
         pellet.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
         double x = (this.random.nextDouble() - 0.5) * 0.9;
         double y = this.random.nextDouble() * 0.55;
         double z = (this.random.nextDouble() - 0.5) * 0.9;
         pellet.shoot(x, y, z, 2.0F, 0.45F);
         this.level().addFreshEntity(pellet);
      }
   }

   private void breakSoftBlocks(BlockPos center, int radius) {
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
         BlockPos target = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
         BlockState state = this.level().getBlockState(target);
         float destroySpeed = state.getDestroySpeed(this.level(), target);
         if (!state.isAir() && destroySpeed >= 0.0F && destroySpeed <= 3.0F) {
            if (this.level() instanceof net.minecraft.server.level.ServerLevel server) {
               net.shadowmage.ancientwarfare.core.faction.FactionRepairs.record(server, target, java.util.List.of(target));
            }

            this.level().destroyBlock(target, true, this);
         }
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putString("AmmoType", this.getAmmoType().name());
      tag.putBoolean("Pellet", this.isPellet());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.contains("AmmoType")) {
         try {
            this.setAmmoType(CannonAmmoType.valueOf(tag.getString("AmmoType")));
         } catch (IllegalArgumentException var3) {
            this.setAmmoType(CannonAmmoType.IRON_10);
         }
      }

      this.setPellet(tag.getBoolean("Pellet"));
   }
}
