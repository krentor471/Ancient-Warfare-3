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

   public void m_8119_() {
      if (this.m_9236_().f_46443_ && this.blockGraceOrigin == null) {
         this.setBlockCollisionGraceOrigin(this.m_20182_());
      }

      super.m_8119_();
   }

   protected final boolean ignoresBlockHit(BlockHitResult result) {
      return this.blockGraceOrigin != null && result.m_82450_().m_82557_(this.blockGraceOrigin) <= this.blockGraceDistance * this.blockGraceDistance;
   }

   public void m_7380_(CompoundTag tag) {
      super.m_7380_(tag);
      if (this.blockGraceOrigin != null && this.blockGraceDistance > 0.0) {
         tag.m_128347_("BlockGraceOriginX", this.blockGraceOrigin.f_82479_);
         tag.m_128347_("BlockGraceOriginY", this.blockGraceOrigin.f_82480_);
         tag.m_128347_("BlockGraceOriginZ", this.blockGraceOrigin.f_82481_);
         tag.m_128347_("BlockGraceDistance", this.blockGraceDistance);
      }
   }

   public void m_7378_(CompoundTag tag) {
      super.m_7378_(tag);
      if (tag.m_128441_("BlockGraceDistance")) {
         this.blockGraceOrigin = new Vec3(tag.m_128459_("BlockGraceOriginX"), tag.m_128459_("BlockGraceOriginY"), tag.m_128459_("BlockGraceOriginZ"));
         this.blockGraceDistance = Math.max(0.0, tag.m_128459_("BlockGraceDistance"));
      }
   }
}
