package net.shadowmage.ancientwarfare.core.common.worksite;

import net.shadowmage.ancientwarfare.core.common.npc.NpcWorkType;

public interface WorkerAccess {
   float getWorkEffectiveness(NpcWorkType var1);

   boolean canWorkAt(NpcWorkType var1);

   double getWorkRangeSq();
}
