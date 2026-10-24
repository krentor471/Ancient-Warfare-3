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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.item.HwachaAmmoType;

public class HwachaRocketEntity extends BlockGraceSiegeProjectileEntity {
   private static final EntityDataAccessor<Integer> AMMO_TYPE = SynchedEntityData.defineId(HwachaRocketEntity.class, EntityDataSerializers.INT);
   private static final int BURN_TICKS = 24;
   private int lifeTicks;

   public HwachaRocketEntity(EntityType<? extends HwachaRocketEntity> type, Level level) {
      super(type, level);
   }

   public HwachaRocketEntity(Level level, LivingEntity shooter, HwachaAmmoType ammoType) {
      super(AWCoreRegistryAccess.entityType(AWCoreIds.HWACHA_ROCKET), shooter, level);
      this.setAmmoType(ammoType);
   }

   protected boolean canHitEntity(Entity entity) {
      Entity owner = this.getOwner();
      if (entity == owner) {
         return false;
      } else {
         return owner == null || entity != owner.getVehicle() && !entity.isPassengerOfSameVehicle(owner) ? super.canHitEntity(entity) : false;
      }
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(AMMO_TYPE, HwachaAmmoType.STANDARD.ordinal());
   }

   public HwachaAmmoType getAmmoType() {
      int index = (Integer)this.entityData.get(AMMO_TYPE);
      HwachaAmmoType[] values = HwachaAmmoType.values();
      return values[Math.max(0, Math.min(values.length - 1, index))];
   }

   public void setAmmoType(HwachaAmmoType ammoType) {
      this.entityData.set(AMMO_TYPE, ammoType.ordinal());
   }

   protected Item getDefaultItem() {
      return switch (this.getAmmoType()) {
         case FLAME -> AWCoreRegistryAccess.item(AWCoreIds.AMMO_HWACHA_ROCKET_FLAME);
         case EXPLOSIVE -> AWCoreRegistryAccess.item(AWCoreIds.AMMO_HWACHA_ROCKET_EXPLOSIVE);
         case AIRBURST -> AWCoreRegistryAccess.item(AWCoreIds.AMMO_HWACHA_ROCKET_AIRBURST);
         case STANDARD -> AWCoreRegistryAccess.item(AWCoreIds.AMMO_HWACHA_ROCKET);
      };
   }

   @Override
   public void tick() {
      super.tick();
      this.lifeTicks++;
      if (!this.level().isClientSide && this.lifeTicks > 160) {
         this.discard();
      } else {
         if (this.lifeTicks <= 24) {
            this.setDeltaMovement(this.getDeltaMovement().scale(1.002));
         }
      }
   }

   protected float getGravity() {
      return 0.045F;
   }

   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      if (!this.level().isClientSide) {
         Entity target = result.getEntity();
         Entity owner = this.getOwner();
         HwachaAmmoType ammoType = this.getAmmoType();
         if (ammoType.damage() > 0.0F) {
            target.hurt(this.damageSources().thrown(this, owner), ammoType.damage());
         }

         if (ammoType.flaming()) {
            target.setSecondsOnFire(ammoType.explosive() ? 3 : 4);
         }

         this.applyImpactEffect(ammoType, this.getX(), this.getY(), this.getZ(), target.blockPosition());
         this.discard();
      }
   }

   protected void onHitBlock(BlockHitResult result) {
      if (!this.ignoresBlockHit(result)) {
         super.onHitBlock(result);
         if (!this.level().isClientSide) {
            HwachaAmmoType ammoType = this.getAmmoType();
            BlockPos effectPos = result.getBlockPos().relative(result.getDirection());
            this.applyImpactEffect(ammoType, this.getX(), this.getY(), this.getZ(), effectPos);
            this.discard();
         }
      }
   }

   private void applyImpactEffect(HwachaAmmoType ammoType, double x, double y, double z, BlockPos effectPos) {
      if (ammoType == HwachaAmmoType.STANDARD) {
         this.level().levelEvent(2000, effectPos, 0);
      } else if (ammoType == HwachaAmmoType.FLAME) {
         this.igniteArea(effectPos, 1);
      } else if (ammoType == HwachaAmmoType.EXPLOSIVE) {
         this.igniteArea(effectPos, 0);
         this.level().explode(this.getOwner(), x, y, z, 1.35F, ExplosionInteraction.TNT);
      } else {
         if (ammoType == HwachaAmmoType.AIRBURST) {
            this.damageAirburst(x, y, z);

            for (int i = 0; i < 10; i++) {
               double angle = (Math.PI / 4) * (double)i;
               double px = x + Math.cos(angle) * 1.6;
               double pz = z + Math.sin(angle) * 1.6;
               this.level().explode(this.getOwner(), px, y + 0.25, pz, 0.25F, ExplosionInteraction.NONE);
            }
         }
      }
   }

   private void igniteArea(BlockPos center, int radius) {
      for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
         if (this.level().getBlockState(pos).isAir() && Blocks.FIRE.defaultBlockState().canSurvive(this.level(), pos)) {
            this.level().setBlock(pos, Blocks.FIRE.defaultBlockState(), 11);
         }
      }
   }

   private void damageAirburst(double x, double y, double z) {
      AABB area = new AABB(x - 3.5, y - 3.0, z - 3.5, x + 3.5, y + 3.0, z + 3.5);

      for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, area, entity -> entity != this.getOwner())) {
         target.hurt(this.damageSources().thrown(this, this.getOwner()), 4.0F);
      }
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("LifeTicks", this.lifeTicks);
      tag.putString("AmmoType", this.getAmmoType().name());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.lifeTicks = tag.getInt("LifeTicks");
      if (tag.contains("AmmoType")) {
         try {
            this.setAmmoType(HwachaAmmoType.valueOf(tag.getString("AmmoType")));
         } catch (IllegalArgumentException var3) {
            this.setAmmoType(HwachaAmmoType.STANDARD);
         }
      }
   }
}
