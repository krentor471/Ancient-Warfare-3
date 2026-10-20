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
   private static final EntityDataAccessor<Integer> AMMO_TYPE = SynchedEntityData.m_135353_(BallistaBoltEntity.class, EntityDataSerializers.f_135028_);

   public BallistaBoltEntity(EntityType<? extends BallistaBoltEntity> type, Level level) {
      super(type, level);
   }

   public BallistaBoltEntity(Level level, LivingEntity shooter, BallistaAmmoType ammoType) {
      super(AWCoreRegistryAccess.entityType(AWCoreIds.BALLISTA_BOLT), shooter, level);
      this.setAmmoType(ammoType);
   }

   protected void m_8097_() {
      super.m_8097_();
      this.f_19804_.m_135372_(AMMO_TYPE, BallistaAmmoType.WOOD.ordinal());
   }

   public BallistaAmmoType getAmmoType() {
      int index = (Integer)this.f_19804_.m_135370_(AMMO_TYPE);
      BallistaAmmoType[] values = BallistaAmmoType.values();
      return values[Math.max(0, Math.min(values.length - 1, index))];
   }

   public void setAmmoType(BallistaAmmoType ammoType) {
      this.f_19804_.m_135381_(AMMO_TYPE, ammoType.ordinal());
   }

   protected Item m_7881_() {
      return switch (this.getAmmoType()) {
         case IRON -> AWCoreRegistryAccess.item(AWCoreIds.AMMO_BALLISTA_BOLT_IRON);
         case FLAME -> AWCoreRegistryAccess.item(AWCoreIds.AMMO_BALLISTA_BOLT_FLAME);
         case EXPLOSIVE -> AWCoreRegistryAccess.item(AWCoreIds.AMMO_BALLISTA_BOLT_EXPLOSIVE);
         case WOOD -> AWCoreRegistryAccess.item(AWCoreIds.AMMO_BALLISTA_BOLT);
      };
   }

   protected void m_5790_(EntityHitResult result) {
      super.m_5790_(result);
      if (!this.m_9236_().f_46443_) {
         Entity target = result.m_82443_();
         Entity owner = this.m_19749_();
         BallistaAmmoType ammoType = this.getAmmoType();
         target.m_6469_(this.m_269291_().m_269390_(this, owner), ammoType.damage());
         if (ammoType.flaming()) {
            target.m_20254_(ammoType.explosive() ? 3 : 4);
         }

         if (ammoType.explosive()) {
            this.m_9236_().m_254849_(owner, this.m_20185_(), this.m_20186_(), this.m_20189_(), 1.2F, ExplosionInteraction.TNT);
         }

         this.m_146870_();
      }
   }

   protected void m_8060_(BlockHitResult result) {
      if (!this.ignoresBlockHit(result)) {
         super.m_8060_(result);
         if (!this.m_9236_().f_46443_) {
            BallistaAmmoType ammoType = this.getAmmoType();
            if (ammoType.explosive()) {
               BlockPos pos = result.m_82425_().m_121945_(result.m_82434_());
               this.m_9236_()
                  .m_254849_(
                     this.m_19749_(),
                     (double)pos.m_123341_() + 0.5,
                     (double)pos.m_123342_() + 0.5,
                     (double)pos.m_123343_() + 0.5,
                     0.8F,
                     ExplosionInteraction.TNT
                  );
            } else if (ammoType.flaming()) {
               BlockPos pos = result.m_82425_().m_121945_(result.m_82434_());
               if (this.m_9236_().m_8055_(pos).m_60795_()) {
                  this.m_9236_().m_7731_(pos, Blocks.f_50083_.m_49966_(), 11);
               }
            }

            GateProxyBlockEntity.damageGate(this.m_9236_(), result.m_82425_(), this.m_269291_().m_269390_(this, this.m_19749_()), ammoType.damage());
            this.m_146870_();
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
            this.setAmmoType(BallistaAmmoType.valueOf(tag.m_128461_("AmmoType")));
         } catch (IllegalArgumentException var3) {
            this.setAmmoType(BallistaAmmoType.WOOD);
         }
      }
   }
}
