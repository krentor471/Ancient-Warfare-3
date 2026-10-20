package net.shadowmage.ancientwarfare.core.tile;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.entity.GateEntity;
import net.shadowmage.ancientwarfare.core.structure.LargeIslandWorldGenerator;

public class GateProxyBlockEntity extends BlockEntity {
   private UUID gateUuid;
   private int orphanTicks;

   public GateProxyBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.id("gate_proxy")), pos, state);
   }

   public void setGateUuid(UUID gateUuid) {
      this.gateUuid = gateUuid;
      this.m_6596_();
   }

   public Entity getGate(ServerLevel level) {
      return this.gateUuid == null ? null : level.m_8791_(this.gateUuid);
   }

   public boolean isGate(UUID uuid) {
      return uuid != null && uuid.equals(this.gateUuid);
   }

   public static boolean damageGate(Level level, BlockPos pos, DamageSource source, float amount) {
      if (!(level instanceof ServerLevel server) || !(level.m_7702_(pos) instanceof GateProxyBlockEntity proxy)) {
         return false;
      }

      if (proxy.getGate(server) instanceof GateEntity gate && gate.ownsProxyPosition(pos) && gate.m_6469_(source, amount)) {
         return true;
      }

      return false;
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, GateProxyBlockEntity proxy) {
      if (level instanceof ServerLevel serverLevel && LargeIslandWorldGenerator.isConstructingAt(serverLevel, pos)) {
         return;
      }

      if (level instanceof ServerLevel serverLevel) {
         Entity entity = proxy.getGate(serverLevel);
         if (entity instanceof GateEntity && entity.m_6084_()) {
            proxy.orphanTicks = 0;
         } else {
            GateEntity nearbyGate = findOwningGate(serverLevel, pos);
            if (nearbyGate != null) {
               proxy.setGateUuid(nearbyGate.m_20148_());
               proxy.orphanTicks = 0;
            } else {
               proxy.orphanTicks++;
               if (proxy.orphanTicks > 100 && level.m_8055_(pos).m_60713_(AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY))) {
                  level.m_7471_(pos, false);
               }
            }
         }
      }
   }

   private static GateEntity findOwningGate(ServerLevel level, BlockPos pos) {
      AABB searchArea = new AABB(pos).m_82400_(32.0);

      for (GateEntity gate : level.m_6443_(GateEntity.class, searchArea, Entity::m_6084_)) {
         if (isInsideGate(gate, pos)) {
            return gate;
         }
      }

      return null;
   }

   private static boolean isInsideGate(GateEntity gate, BlockPos pos) {
      return gate.ownsProxyPosition(pos);
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      if (this.gateUuid != null) {
         tag.m_128362_("Gate", this.gateUuid);
      }
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.gateUuid = tag.m_128403_("Gate") ? tag.m_128342_("Gate") : null;
   }
}
