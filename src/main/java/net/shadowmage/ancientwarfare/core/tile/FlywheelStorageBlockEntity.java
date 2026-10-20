package net.shadowmage.ancientwarfare.core.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.block.FlywheelStorageBlock;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTier;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class FlywheelStorageBlockEntity extends BlockEntity implements GeoBlockEntity {
   private static final String STORED_TORQUE_TAG = "StoredTorque";
   private double storedTorque;
   private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

   public FlywheelStorageBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.id("flywheel_storage")), pos, state);
   }

   public TorqueTier getTier() {
      return this.m_58900_().m_60734_() instanceof FlywheelStorageBlock block ? block.getTier() : TorqueTier.LIGHT;
   }

   public double getStoredTorque() {
      return this.storedTorque;
   }

   public void setStoredTorque(double storedTorque) {
      this.storedTorque = Math.max(0.0, storedTorque);
      this.m_6596_();
   }

   public FlywheelStorageBlockEntity.StorageRenderState getStorageRenderState() {
      Level level = this.m_58904_();
      if (level == null) {
         return new FlywheelStorageBlockEntity.StorageRenderState(
            FlywheelStorageBlockEntity.StorageSize.SMALL, FlywheelStorageBlockEntity.StorageLayer.SINGLE, true
         );
      } else if (!this.isStorage(level, this.f_58858_)) {
         return new FlywheelStorageBlockEntity.StorageRenderState(
            FlywheelStorageBlockEntity.StorageSize.SMALL, FlywheelStorageBlockEntity.StorageLayer.SINGLE, false
         );
      } else {
         for (int centerX = this.f_58858_.m_123341_() - 1; centerX <= this.f_58858_.m_123341_() + 1; centerX++) {
            for (int centerZ = this.f_58858_.m_123343_() - 1; centerZ <= this.f_58858_.m_123343_() + 1; centerZ++) {
               BlockPos center = new BlockPos(centerX, this.f_58858_.m_123342_(), centerZ);
               if (this.isFullLargeLayer(level, center)) {
                  if (!center.equals(this.f_58858_)) {
                     return new FlywheelStorageBlockEntity.StorageRenderState(
                        FlywheelStorageBlockEntity.StorageSize.LARGE, FlywheelStorageBlockEntity.StorageLayer.MIDDLE, false
                     );
                  }

                  return new FlywheelStorageBlockEntity.StorageRenderState(
                     FlywheelStorageBlockEntity.StorageSize.LARGE,
                     this.layerState(this.isFullLargeLayer(level, center.m_7495_()), this.isFullLargeLayer(level, center.m_7494_())),
                     true
                  );
               }
            }
         }

         return new FlywheelStorageBlockEntity.StorageRenderState(
            FlywheelStorageBlockEntity.StorageSize.SMALL,
            this.layerState(this.isStorage(level, this.f_58858_.m_7495_()), this.isStorage(level, this.f_58858_.m_7494_())),
            true
         );
      }
   }

   public AABB getRenderBoundingBox() {
      FlywheelStorageBlockEntity.StorageRenderState state = this.getStorageRenderState();
      return state.size() == FlywheelStorageBlockEntity.StorageSize.LARGE && state.visible()
         ? new AABB(this.f_58858_).m_82377_(2.0, 1.0, 2.0)
         : new AABB(this.f_58858_);
   }

   private FlywheelStorageBlockEntity.StorageLayer layerState(boolean below, boolean above) {
      if (below && above) {
         return FlywheelStorageBlockEntity.StorageLayer.MIDDLE;
      } else if (above) {
         return FlywheelStorageBlockEntity.StorageLayer.BOTTOM;
      } else {
         return below ? FlywheelStorageBlockEntity.StorageLayer.TOP : FlywheelStorageBlockEntity.StorageLayer.SINGLE;
      }
   }

   private boolean isFullLargeLayer(Level level, BlockPos center) {
      for (int x = -1; x <= 1; x++) {
         for (int z = -1; z <= 1; z++) {
            if (!this.isStorage(level, center.m_7918_(x, 0, z))) {
               return false;
            }
         }
      }

      return true;
   }

   private boolean isStorage(Level level, BlockPos pos) {
      if (level.m_8055_(pos).m_60734_() instanceof FlywheelStorageBlock block && block.getTier() == this.getTier()) {
         return true;
      }

      return false;
   }

   public void registerControllers(ControllerRegistrar controllers) {
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.animationCache;
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      tag.m_128347_("StoredTorque", this.storedTorque);
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.storedTorque = Math.max(0.0, tag.m_128459_("StoredTorque"));
   }

   public static enum StorageLayer {
      SINGLE,
      BOTTOM,
      MIDDLE,
      TOP;
   }

   public static record StorageRenderState(FlywheelStorageBlockEntity.StorageSize size, FlywheelStorageBlockEntity.StorageLayer layer, boolean visible) {
   }

   public static enum StorageSize {
      SMALL,
      LARGE;
   }
}
