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
      this.controllerPos = tag.m_128441_("WarehouseController") ? BlockPos.m_122022_(tag.m_128454_("WarehouseController")) : null;
      this.checkedRevision = Long.MIN_VALUE;
      this.nextDiscovery = 0L;
   }

   public void save(CompoundTag tag) {
      if (this.controllerPos != null) {
         tag.m_128356_("WarehouseController", this.controllerPos.m_121878_());
      }
   }

   public WarehouseControlBlockEntity findController(BlockEntity member) {
      Level level = member.m_58904_();
      if (level == null) {
         return null;
      } else {
         if (this.controllerPos != null) {
            if (!level.m_46805_(this.controllerPos)) {
               return null;
            }

            WarehouseControlBlockEntity bound = WarehouseNetwork.loadedController(level, this.controllerPos);
            if (bound != null && WarehouseNetwork.accepts(bound, member)) {
               return bound;
            }

            if (level.f_46443_) {
               return null;
            }

            this.controllerPos = null;
            this.checkedRevision = Long.MIN_VALUE;
            member.m_6596_();
            WarehouseNetwork.changed(level);
         }

         if (level.f_46443_) {
            return null;
         } else {
            long revision = WarehouseNetwork.revision(level);
            if (this.checkedRevision == revision && level.m_46467_() < this.nextDiscovery) {
               return null;
            } else {
               this.checkedRevision = revision;
               this.nextDiscovery = level.m_46467_() + 100L;
               List<WarehouseControlBlockEntity> candidates = WarehouseNetwork.controllersFor(member);
               if (candidates.isEmpty()) {
                  return null;
               } else {
                  this.bind(member, candidates.get(0).m_58899_());
                  return candidates.get(0);
               }
            }
         }
      }
   }

   public boolean bind(BlockEntity member, BlockPos pos) {
      WarehouseControlBlockEntity control = WarehouseNetwork.loadedController(member.m_58904_(), pos);
      if (control == null || !WarehouseNetwork.accepts(control, member)) {
         return false;
      } else if (pos.equals(this.controllerPos)) {
         return true;
      } else {
         this.controllerPos = pos.m_7949_();
         member.m_6596_();
         WarehouseNetwork.changed(member.m_58904_());
         if (member.m_58904_() != null) {
            member.m_58904_().m_7260_(member.m_58899_(), member.m_58900_(), member.m_58900_(), 2);
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
            if (candidates.get(i).m_58899_().equals(this.controllerPos)) {
               current = i;
            }
         }

         return this.bind(member, candidates.get((current + 1) % candidates.size()).m_58899_());
      }
   }

   public int getConnectionStatus(BlockEntity member) {
      if (this.findController(member) != null) {
         return 1;
      } else {
         return this.controllerPos != null && member.m_58904_() != null && !member.m_58904_().m_46805_(this.controllerPos) ? 2 : 0;
      }
   }
}
