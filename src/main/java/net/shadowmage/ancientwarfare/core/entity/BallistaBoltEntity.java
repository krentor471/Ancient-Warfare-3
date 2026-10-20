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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.item.BallistaAmmoType;
import net.shadowmage.ancientwarfare.core.tile.GateProxyBlockEntity;

public class BallistaBoltEntity extends BlockGraceSiegeProjectileEntity {
   private static final EntityDataAccessor<Integer> AMMO_TYPE = SynchedEntityData.defineId(BallistaBoltEntity.class, EntityDataSerializers.INT);

   public BallistaBoltEntity(EntityType<? extends BallistaBoltEntity> type, Level level) {
      super(type, level);
   }

   public BallistaBoltEntity(Level level, LivingEntity shooter, BallistaAmmoType ammoType) {
      super(AWCoreRegistryAccess.entityType(AWCoreIds.BALLISTA_BOLT), shooter, level);
      this.setAmmoType(ammoType);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(AMMO_TYPE, BallistaAmmoType.WOOD.ordinal());
   }

   public BallistaAmmoType getAmmoType() {
      int index = (Integer)this.entityData.get(AMMO_TYPE);
      BallistaAmmoType[] values = BallistaAmmoType.values();
      return values[Math.max(0, Math.min(values.length - 1, index))];
   }

   public void setAmmoType(BallistaAmmoType ammoType) {
      this.entityData.set(AMMO_TYPE, ammoType.ordinal());
   }

   protected Item getDefaultItem() {
      return switch (this.getAmmoType()) {
         case IRON -> AWCoreRegistryAccess.item(AWCoreIds.AMMO_BALLISTA_BOLT_IRON);
         case FLAME -> AWCoreRegistryAccess.item(AWCoreIds.AMMO_BALLISTA_BOLT_FLAME);
         case EXPLOSIVE -> AWCoreRegistryAccess.item(AWCoreIds.AMMO_BALLISTA_BOLT_EXPLOSIVE);
         case WOOD -> AWCoreRegistryAccess.item(AWCoreIds.AMMO_BALLISTA_BOLT);
      };
   }

   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide) {
         Entity target = result.getEntity();
         Entity owner = this.getOwner();
         BallistaAmmoType ammoType = this.getAmmoType();
         target.hurt(this.damageSources().thrown(this, owner), ammoType.damage());
         if (ammoType.flaming()) {
            target.setSecondsOnFire(ammoType.explosive() ? 3 : 4);
         }

         if (ammoType.explosive()) {
            this.level().explode(owner, this.getX(), this.getY(), this.getZ(), 1.2F, ExplosionInteraction.TNT);
         }

         this.discard();
      }
   }

   protected void onHitBlock(BlockHitResult result) {
      if (!this.ignoresBlockHit(result)) {
         super.onHitBlock(result);
         if (!this.level().isClientSide) {
            BallistaAmmoType ammoType = this.getAmmoType();
            if (ammoType.explosive()) {
               BlockPos pos = result.getBlockPos().relative(result.getDirection());
               this.level()
                  .explode(
                     this.getOwner(),
                     (double)pos.getX() + 0.5,
                     (double)pos.getY() + 0.5,
                     (double)pos.getZ() + 0.5,
                     0.8F,
                     ExplosionInteraction.TNT
                  );
            } else if (ammoType.flaming()) {
               BlockPos pos = result.getBlockPos().relative(result.getDirection());
               if (this.level().getBlockState(pos).isAir()) {
                  this.level().setBlock(pos, Blocks.FIRE.defaultBlockState(), 11);
               }
            }

            GateProxyBlockEntity.damageGate(this.level(), result.getBlockPos(), this.damageSources().thrown(this, this.getOwner()), ammoType.damage());
            this.discard();
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
            this.setAmmoType(BallistaAmmoType.valueOf(tag.getString("AmmoType")));
         } catch (IllegalArgumentException var3) {
            this.setAmmoType(BallistaAmmoType.WOOD);
         }
      }
   }
}
