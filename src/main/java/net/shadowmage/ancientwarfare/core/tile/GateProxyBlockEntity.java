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
      this.setChanged();
   }

   public Entity getGate(ServerLevel level) {
      return this.gateUuid == null ? null : level.getEntity(this.gateUuid);
   }

   public boolean isGate(UUID uuid) {
      return uuid != null && uuid.equals(this.gateUuid);
   }

   public static boolean damageGate(Level level, BlockPos pos, DamageSource source, float amount) {
      if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof GateProxyBlockEntity proxy)) {
         return false;
      }

      if (proxy.getGate(server) instanceof GateEntity gate && gate.ownsProxyPosition(pos) && gate.hurt(source, amount)) {
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
         if (entity instanceof GateEntity && entity.isAlive()) {
            proxy.orphanTicks = 0;
         } else {
            GateEntity nearbyGate = findOwningGate(serverLevel, pos);
            if (nearbyGate != null) {
               proxy.setGateUuid(nearbyGate.getUUID());
               proxy.orphanTicks = 0;
            } else {
               proxy.orphanTicks++;
               if (proxy.orphanTicks > 100 && level.getBlockState(pos).is(AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY))) {
                  level.removeBlock(pos, false);
               }
            }
         }
      }
   }

   private static GateEntity findOwningGate(ServerLevel level, BlockPos pos) {
      AABB searchArea = new AABB(pos).inflate(32.0);

      for (GateEntity gate : level.getEntitiesOfClass(GateEntity.class, searchArea, Entity::isAlive)) {
         if (isInsideGate(gate, pos)) {
            return gate;
         }
      }

      return null;
   }

   private static boolean isInsideGate(GateEntity gate, BlockPos pos) {
      return gate.ownsProxyPosition(pos);
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      if (this.gateUuid != null) {
         tag.putUUID("Gate", this.gateUuid);
      }
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.gateUuid = tag.hasUUID("Gate") ? tag.getUUID("Gate") : null;
   }
}
