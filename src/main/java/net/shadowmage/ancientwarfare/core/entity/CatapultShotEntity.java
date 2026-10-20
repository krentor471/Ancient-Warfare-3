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
   private static final EntityDataAccessor<Integer> AMMO_TYPE = SynchedEntityData.m_135353_(CatapultShotEntity.class, EntityDataSerializers.f_135028_);

   public CatapultShotEntity(EntityType<? extends CatapultShotEntity> type, Level level) {
      super(type, level);
   }

   public CatapultShotEntity(Level level, LivingEntity shooter, CatapultAmmoType ammoType) {
      super(AWCoreRegistryAccess.entityType(AWCoreIds.CATAPULT_SHOT), shooter, level);
      this.setAmmoType(ammoType);
   }

   protected void m_8097_() {
      super.m_8097_();
      this.f_19804_.m_135372_(AMMO_TYPE, CatapultAmmoType.STONE_10.ordinal());
   }

   public CatapultAmmoType getAmmoType() {
      int index = (Integer)this.f_19804_.m_135370_(AMMO_TYPE);
      CatapultAmmoType[] values = CatapultAmmoType.values();
      return values[Math.max(0, Math.min(values.length - 1, index))];
   }

   public void setAmmoType(CatapultAmmoType ammoType) {
      this.f_19804_.m_135381_(AMMO_TYPE, ammoType.ordinal());
   }

   protected Item m_7881_() {
      return AWCoreRegistryAccess.item(AWCoreIds.AMMO_STONE_SHOT_10);
   }

   protected boolean m_5603_(Entity entity) {
      Entity owner = this.m_19749_();
      return entity != owner && (owner == null || entity != owner.m_20202_()) ? super.m_5603_(entity) : false;
   }

   protected void m_5790_(EntityHitResult result) {
      super.m_5790_(result);
      if (!this.m_9236_().f_46443_) {
         Entity target = result.m_82443_();
         CatapultAmmoType ammoType = this.getAmmoType();
         if (ammoType.damage() > 0.0F) {
            target.m_6469_(this.m_269291_().m_269390_(this, this.m_19749_()), ammoType.damage());
         }

         if (ammoType.flaming()) {
            target.m_20254_(ammoType.spreadsNapalm() ? 8 : 4);
         }

         this.applyImpactEffects(this.m_20183_(), result);
         this.m_146870_();
      }
   }

   protected void m_8060_(BlockHitResult result) {
      if (!this.ignoresBlockHit(result)) {
         super.m_8060_(result);
         if (!this.m_9236_().f_46443_) {
            GateProxyBlockEntity.damageGate(this.m_9236_(), result.m_82425_(), this.m_269291_().m_269390_(this, this.m_19749_()), this.getAmmoType().damage());
            this.applyImpactEffects(result.m_82425_().m_121945_(result.m_82434_()), result);
            this.m_146870_();
         }
      }
   }

   private void applyImpactEffects(BlockPos center, HitResult result) {
      CatapultAmmoType ammoType = this.getAmmoType();
      if (ammoType.explosive()) {
         this.m_9236_().m_254849_(this.m_19749_(), this.m_20185_(), this.m_20186_(), this.m_20189_(), ammoType.explosionPower(), ExplosionInteraction.TNT);
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

      for (BlockPos pos : BlockPos.m_121940_(center.m_7918_(-clampedRadius, -1, -clampedRadius), center.m_7918_(clampedRadius, 1, clampedRadius))) {
         BlockPos firePos = new BlockPos(pos.m_123341_(), pos.m_123342_(), pos.m_123343_());
         if (this.m_9236_().m_8055_(firePos).m_60795_() && this.m_9236_().m_8055_(firePos.m_7495_()).m_60804_(this.m_9236_(), firePos.m_7495_())) {
            this.m_9236_().m_7731_(firePos, Blocks.f_50083_.m_49966_(), 11);
         }
      }
   }

   private void breakSoftBlocks(BlockPos center, int radius) {
      int clampedRadius = Math.max(1, Math.min(3, radius));

      for (BlockPos pos : BlockPos.m_121940_(
         center.m_7918_(-clampedRadius, -clampedRadius, -clampedRadius), center.m_7918_(clampedRadius, clampedRadius, clampedRadius)
      )) {
         BlockPos target = new BlockPos(pos.m_123341_(), pos.m_123342_(), pos.m_123343_());
         BlockState state = this.m_9236_().m_8055_(target);
         if (!state.m_60795_() && state.m_60800_(this.m_9236_(), target) >= 0.0F && state.m_60800_(this.m_9236_(), target) <= 2.0F) {
            this.m_9236_().m_46953_(target, true, this);
         }
      }
   }

   private void scatterImpact(BlockPos center, int count) {
      int radius = Math.max(1, Math.min(4, count / 8));

      for (int i = 0; i < Math.min(12, count); i++) {
         BlockPos pos = center.m_7918_(this.f_19796_.m_188503_(radius * 2 + 1) - radius, 0, this.f_19796_.m_188503_(radius * 2 + 1) - radius);
         Entity owner = this.m_19749_();

         for (LivingEntity living : this.m_9236_().m_45976_(LivingEntity.class, new AABB(pos).m_82400_(1.25))) {
            if (living != owner) {
               living.m_6469_(this.m_269291_().m_269390_(this, owner), 3.0F);
            }
         }
      }
   }

   @Override
   public void m_7380_(CompoundTag tag) {
      super.m_7380_(tag);
      tag.m_128359_("AmmoType", this.getAmmoType().name());
   }

   @Override
   public void m_7378_(CompoundTag tag) {
      super.m_7378_(tag);
      if (tag.m_128441_("AmmoType")) {
         try {
            this.setAmmoType(CatapultAmmoType.valueOf(tag.m_128461_("AmmoType")));
         } catch (IllegalArgumentException var3) {
            this.setAmmoType(CatapultAmmoType.STONE_10);
         }
      }
   }
}
