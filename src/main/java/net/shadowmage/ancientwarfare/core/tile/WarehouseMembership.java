package net.shadowmage.ancientwarfare.core.tile;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class WarehouseMembership {
   private BlockPos controllerPos;
   private long checkedRevision = Long.MIN_VALUE;
   private long nextDiscovery;

   public BlockPos getControllerPos() {
      return this.controllerPos;
   }

   public void load(CompoundTag tag) {
      this.controllerPos = tag.contains("WarehouseController") ? BlockPos.of(tag.getLong("WarehouseController")) : null;
      this.checkedRevision = Long.MIN_VALUE;
      this.nextDiscovery = 0L;
   }

   public void save(CompoundTag tag) {
      if (this.controllerPos != null) {
         tag.putLong("WarehouseController", this.controllerPos.asLong());
      }
   }

   public WarehouseControlBlockEntity findController(BlockEntity member) {
      Level level = member.getLevel();
      if (level == null) {
         return null;
      } else {
         if (this.controllerPos != null) {
            if (!level.hasChunkAt(this.controllerPos)) {
               return null;
            }

            WarehouseControlBlockEntity bound = WarehouseNetwork.loadedController(level, this.controllerPos);
            if (bound != null && WarehouseNetwork.accepts(bound, member)) {
               return bound;
            }

            if (level.isClientSide) {
               return null;
            }

            this.controllerPos = null;
            this.checkedRevision = Long.MIN_VALUE;
            member.setChanged();
            WarehouseNetwork.changed(level);
         }

         if (level.isClientSide) {
            return null;
         } else {
            long revision = WarehouseNetwork.revision(level);
            if (this.checkedRevision == revision && level.getGameTime() < this.nextDiscovery) {
               return null;
            } else {
               this.checkedRevision = revision;
               this.nextDiscovery = level.getGameTime() + 100L;
               List<WarehouseControlBlockEntity> candidates = WarehouseNetwork.controllersFor(member);
               if (candidates.isEmpty()) {
                  return null;
               } else {
                  this.bind(member, candidates.get(0).getBlockPos());
                  return candidates.get(0);
               }
            }
         }
      }
   }

   public boolean bind(BlockEntity member, BlockPos pos) {
      WarehouseControlBlockEntity control = WarehouseNetwork.loadedController(member.getLevel(), pos);
      if (control == null || !WarehouseNetwork.accepts(control, member)) {
         return false;
      } else if (pos.equals(this.controllerPos)) {
         return true;
      } else {
         this.controllerPos = pos.immutable();
         member.setChanged();
         WarehouseNetwork.changed(member.getLevel());
         if (member.getLevel() != null) {
            member.getLevel().sendBlockUpdated(member.getBlockPos(), member.getBlockState(), member.getBlockState(), 2);
         }

         return true;
      }
   }

   public boolean cycleController(BlockEntity member) {
      List<WarehouseControlBlockEntity> candidates = WarehouseNetwork.controllersFor(member);
      if (candidates.isEmpty()) {
         return false;
      } else {
         int current = -1;

         for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).getBlockPos().equals(this.controllerPos)) {
               current = i;
            }
         }

         return this.bind(member, candidates.get((current + 1) % candidates.size()).getBlockPos());
      }
   }

   public int getConnectionStatus(BlockEntity member) {
      if (this.findController(member) != null) {
         return 1;
      } else {
         return this.controllerPos != null && member.getLevel() != null && !member.getLevel().hasChunkAt(this.controllerPos) ? 2 : 0;
      }
   }
}
