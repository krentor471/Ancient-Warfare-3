package net.shadowmage.ancientwarfare.core.common.worksite;

import net.shadowmage.ancientwarfare.core.common.npc.NpcWorkType;

public interface WorkSite {
   boolean hasWork();

   default void onWorkerTick(WorkerAccess worker) {
   }

   void addEnergyFromWorker(WorkerAccess var1);

   NpcWorkType getWorkType();
}
