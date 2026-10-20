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
   private static final EntityDataAccessor<Integer> AMMO_TYPE = SynchedEntityData.m_135353_(HwachaRocketEntity.class, EntityDataSerializers.f_135028_);
   private static final int BURN_TICKS = 24;
   private int lifeTicks;

   public HwachaRocketEntity(EntityType<? extends HwachaRocketEntity> type, Level level) {
      super(type, level);
   }

   public HwachaRocketEntity(Level level, LivingEntity shooter, HwachaAmmoType ammoType) {
      super(AWCoreRegistryAccess.entityType(AWCoreIds.HWACHA_ROCKET), shooter, level);
      this.setAmmoType(ammoType);
   }

   protected boolean m_5603_(Entity entity) {
      Entity owner = this.m_19749_();
      if (entity == owner) {
         return false;
      } else {
         return owner == null || entity != owner.m_20202_() && !entity.m_20365_(owner) ? super.m_5603_(entity) : false;
      }
   }

   protected void m_8097_() {
      super.m_8097_();
      this.f_19804_.m_135372_(AMMO_TYPE, HwachaAmmoType.STANDARD.ordinal());
   }

   public HwachaAmmoType getAmmoType() {
      int index = (Integer)this.f_19804_.m_135370_(AMMO_TYPE);
      HwachaAmmoType[] values = HwachaAmmoType.values();
      return values[Math.max(0, Math.min(values.length - 1, index))];
   }

   public void setAmmoType(HwachaAmmoType ammoType) {
      this.f_19804_.m_135381_(AMMO_TYPE, ammoType.ordinal());
   }

   protected Item m_7881_() {
      return switch (this.getAmmoType()) {
         case FLAME -> AWCoreRegistryAccess.item(AWCoreIds.AMMO_HWACHA_ROCKET_FLAME);
         case EXPLOSIVE -> AWCoreRegistryAccess.item(AWCoreIds.AMMO_HWACHA_ROCKET_EXPLOSIVE);
         case AIRBURST -> AWCoreRegistryAccess.item(AWCoreIds.AMMO_HWACHA_ROCKET_AIRBURST);
         case STANDARD -> AWCoreRegistryAccess.item(AWCoreIds.AMMO_HWACHA_ROCKET);
      };
   }

   @Override
   public void m_8119_() {
      super.m_8119_();
      this.lifeTicks++;
      if (!this.m_9236_().f_46443_ && this.lifeTicks > 160) {
         this.m_146870_();
      } else {
         if (this.lifeTicks <= 24) {
            this.m_20256_(this.m_20184_().m_82490_(1.002));
         }
      }
   }

   protected float m_7139_() {
      return 0.045F;
   }

   protected void m_5790_(EntityHitResult result) {
      super.m_5790_(result);
      if (!this.m_9236_().f_46443_) {
         Entity target = result.m_82443_();
         Entity owner = this.m_19749_();
         HwachaAmmoType ammoType = this.getAmmoType();
         if (ammoType.damage() > 0.0F) {
            target.m_6469_(this.m_269291_().m_269390_(this, owner), ammoType.damage());
         }

         if (ammoType.flaming()) {
            target.m_20254_(ammoType.explosive() ? 3 : 4);
         }

         this.applyImpactEffect(ammoType, this.m_20185_(), this.m_20186_(), this.m_20189_(), target.m_20183_());
         this.m_146870_();
      }
   }

   protected void m_8060_(BlockHitResult result) {
      if (!this.ignoresBlockHit(result)) {
         super.m_8060_(result);
         if (!this.m_9236_().f_46443_) {
            HwachaAmmoType ammoType = this.getAmmoType();
            BlockPos effectPos = result.m_82425_().m_121945_(result.m_82434_());
            this.applyImpactEffect(ammoType, this.m_20185_(), this.m_20186_(), this.m_20189_(), effectPos);
            this.m_146870_();
         }
      }
   }

   private void applyImpactEffect(HwachaAmmoType ammoType, double x, double y, double z, BlockPos effectPos) {
      if (ammoType == HwachaAmmoType.STANDARD) {
         this.m_9236_().m_46796_(2000, effectPos, 0);
      } else if (ammoType == HwachaAmmoType.FLAME) {
         this.igniteArea(effectPos, 1);
      } else if (ammoType == HwachaAmmoType.EXPLOSIVE) {
         this.igniteArea(effectPos, 0);
         this.m_9236_().m_254849_(this.m_19749_(), x, y, z, 1.35F, ExplosionInteraction.TNT);
      } else {
         if (ammoType == HwachaAmmoType.AIRBURST) {
            this.damageAirburst(x, y, z);

            for (int i = 0; i < 10; i++) {
               double angle = (Math.PI / 4) * (double)i;
               double px = x + Math.cos(angle) * 1.6;
               double pz = z + Math.sin(angle) * 1.6;
               this.m_9236_().m_254849_(this.m_19749_(), px, y + 0.25, pz, 0.25F, ExplosionInteraction.NONE);
            }
         }
      }
   }

   private void igniteArea(BlockPos center, int radius) {
      for (BlockPos pos : BlockPos.m_121940_(center.m_7918_(-radius, -1, -radius), center.m_7918_(radius, 1, radius))) {
         if (this.m_9236_().m_8055_(pos).m_60795_() && Blocks.f_50083_.m_49966_().m_60710_(this.m_9236_(), pos)) {
            this.m_9236_().m_7731_(pos, Blocks.f_50083_.m_49966_(), 11);
         }
      }
   }

   private void damageAirburst(double x, double y, double z) {
      AABB area = new AABB(x - 3.5, y - 3.0, z - 3.5, x + 3.5, y + 3.0, z + 3.5);

      for (LivingEntity target : this.m_9236_().m_6443_(LivingEntity.class, area, entity -> entity != this.m_19749_())) {
         target.m_6469_(this.m_269291_().m_269390_(this, this.m_19749_()), 4.0F);
      }
   }

   @Override
   public void m_7380_(CompoundTag tag) {
      super.m_7380_(tag);
      tag.m_128405_("LifeTicks", this.lifeTicks);
      tag.m_128359_("AmmoType", this.getAmmoType().name());
   }

   @Override
   public void m_7378_(CompoundTag tag) {
      super.m_7378_(tag);
      this.lifeTicks = tag.m_128451_("LifeTicks");
      if (tag.m_128441_("AmmoType")) {
         try {
            this.setAmmoType(HwachaAmmoType.valueOf(tag.m_128461_("AmmoType")));
         } catch (IllegalArgumentException var3) {
            this.setAmmoType(HwachaAmmoType.STANDARD);
         }
      }
   }
}
