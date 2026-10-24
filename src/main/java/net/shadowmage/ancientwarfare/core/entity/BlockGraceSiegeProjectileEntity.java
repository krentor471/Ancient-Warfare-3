package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

abstract class BlockGraceSiegeProjectileEntity extends ThrowableItemProjectile {
   static final double FRIENDLY_WALL_GRACE_DISTANCE = 6.0;
   private Vec3 blockGraceOrigin;
   private double blockGraceDistance;

   protected BlockGraceSiegeProjectileEntity(EntityType<? extends BlockGraceSiegeProjectileEntity> type, Level level) {
      super(type, level);
   }

   protected BlockGraceSiegeProjectileEntity(EntityType<? extends BlockGraceSiegeProjectileEntity> type, LivingEntity shooter, Level level) {
      super(type, shooter, level);
   }

   final void setBlockCollisionGraceOrigin(Vec3 origin) {
      this.blockGraceOrigin = origin;
      this.blockGraceDistance = 6.0;
   }

   public void tick() {
      if (this.level().isClientSide && this.blockGraceOrigin == null) {
         this.setBlockCollisionGraceOrigin(this.position());
      }

      super.tick();
   }

   protected final boolean ignoresBlockHit(BlockHitResult result) {
      return this.blockGraceOrigin != null && result.getLocation().distanceToSqr(this.blockGraceOrigin) <= this.blockGraceDistance * this.blockGraceDistance;
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.blockGraceOrigin != null && this.blockGraceDistance > 0.0) {
         tag.putDouble("BlockGraceOriginX", this.blockGraceOrigin.x);
         tag.putDouble("BlockGraceOriginY", this.blockGraceOrigin.y);
         tag.putDouble("BlockGraceOriginZ", this.blockGraceOrigin.z);
         tag.putDouble("BlockGraceDistance", this.blockGraceDistance);
      }
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      if (tag.contains("BlockGraceDistance")) {
         this.blockGraceOrigin = new Vec3(tag.getDouble("BlockGraceOriginX"), tag.getDouble("BlockGraceOriginY"), tag.getDouble("BlockGraceOriginZ"));
         this.blockGraceDistance = Math.max(0.0, tag.getDouble("BlockGraceDistance"));
      }
   }
}
