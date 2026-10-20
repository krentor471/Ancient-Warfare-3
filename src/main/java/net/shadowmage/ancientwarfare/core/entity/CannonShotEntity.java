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
   private static final EntityDataAccessor<Integer> AMMO_TYPE = SynchedEntityData.m_135353_(CannonShotEntity.class, EntityDataSerializers.f_135028_);
   private static final EntityDataAccessor<Boolean> PELLET = SynchedEntityData.m_135353_(CannonShotEntity.class, EntityDataSerializers.f_135035_);

   public CannonShotEntity(EntityType<? extends CannonShotEntity> type, Level level) {
      super(type, level);
   }

   public CannonShotEntity(Level level, LivingEntity shooter, CannonAmmoType ammoType, boolean pellet) {
      super(AWCoreRegistryAccess.entityType(AWCoreIds.CANNON_SHOT), shooter, level);
      this.setAmmoType(ammoType);
      this.setPellet(pellet);
   }

   protected void m_8097_() {
      super.m_8097_();
      this.f_19804_.m_135372_(AMMO_TYPE, CannonAmmoType.IRON_10.ordinal());
      this.f_19804_.m_135372_(PELLET, false);
   }

   public CannonAmmoType getAmmoType() {
      int index = (Integer)this.f_19804_.m_135370_(AMMO_TYPE);
      CannonAmmoType[] values = CannonAmmoType.values();
      return values[Math.max(0, Math.min(values.length - 1, index))];
   }

   public void setAmmoType(CannonAmmoType ammoType) {
      this.f_19804_.m_135381_(AMMO_TYPE, ammoType.ordinal());
   }

   public boolean isPellet() {
      return (Boolean)this.f_19804_.m_135370_(PELLET);
   }

   public void setPellet(boolean pellet) {
      this.f_19804_.m_135381_(PELLET, pellet);
   }

   protected Item m_7881_() {
      return AWCoreRegistryAccess.item(AWCoreIds.AMMO_IRON_SHOT_10);
   }

   protected void m_5790_(EntityHitResult result) {
      super.m_5790_(result);
      if (!this.m_9236_().f_46443_) {
         Entity target = result.m_82443_();
         CannonAmmoType ammoType = this.getAmmoType();
         float damage = this.isPellet() ? Math.max(3.0F, ammoType.damage() * 0.35F) : ammoType.damage();
         if (damage > 0.0F) {
            target.m_6469_(this.m_269291_().m_269390_(this, this.m_19749_()), damage);
         }

         this.applyImpactEffects(this.m_20183_(), result);
         this.m_146870_();
      }
   }

   protected void m_8060_(BlockHitResult result) {
      if (!this.ignoresBlockHit(result)) {
         super.m_8060_(result);
         if (!this.m_9236_().f_46443_) {
            float damage = this.isPellet() ? Math.max(3.0F, this.getAmmoType().damage() * 0.35F) : this.getAmmoType().damage();
            GateProxyBlockEntity.damageGate(this.m_9236_(), result.m_82425_(), this.m_269291_().m_269390_(this, this.m_19749_()), damage);
            this.applyImpactEffects(result.m_82425_().m_121945_(result.m_82434_()), result);
            this.m_146870_();
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
      LivingEntity shooter = this.m_19749_() instanceof LivingEntity living ? living : null;
      int count = Math.min(32, Math.max(1, ammoType.pelletCount()));

      for (int i = 0; i < count; i++) {
         CannonShotEntity pellet = new CannonShotEntity(this.m_9236_(), shooter, ammoType, true);
         pellet.m_7678_(this.m_20185_(), this.m_20186_(), this.m_20189_(), this.m_146908_(), this.m_146909_());
         double x = (this.f_19796_.m_188500_() - 0.5) * 0.9;
         double y = this.f_19796_.m_188500_() * 0.55;
         double z = (this.f_19796_.m_188500_() - 0.5) * 0.9;
         pellet.m_6686_(x, y, z, 2.0F, 0.45F);
         this.m_9236_().m_7967_(pellet);
      }
   }

   private void breakSoftBlocks(BlockPos center, int radius) {
      for (BlockPos pos : BlockPos.m_121940_(center.m_7918_(-radius, -radius, -radius), center.m_7918_(radius, radius, radius))) {
         BlockPos target = new BlockPos(pos.m_123341_(), pos.m_123342_(), pos.m_123343_());
         BlockState state = this.m_9236_().m_8055_(target);
         float destroySpeed = state.m_60800_(this.m_9236_(), target);
         if (!state.m_60795_() && destroySpeed >= 0.0F && destroySpeed <= 3.0F) {
            this.m_9236_().m_46953_(target, true, this);
         }
      }
   }

   @Override
   public void m_7380_(CompoundTag tag) {
      super.m_7380_(tag);
      tag.m_128359_("AmmoType", this.getAmmoType().name());
      tag.m_128379_("Pellet", this.isPellet());
   }

   @Override
   public void m_7378_(CompoundTag tag) {
      super.m_7378_(tag);
      if (tag.m_128441_("AmmoType")) {
         try {
            this.setAmmoType(CannonAmmoType.valueOf(tag.m_128461_("AmmoType")));
         } catch (IllegalArgumentException var3) {
            this.setAmmoType(CannonAmmoType.IRON_10);
         }
      }

      this.setPellet(tag.m_128471_("Pellet"));
   }
}
