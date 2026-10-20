package net.shadowmage.ancientwarfare.core.common.worksite;

import java.util.Map;

public interface SideConfigurableWorksite {
   WorksiteInventoryAccess getSideAccess(WorksiteRelativeSide var1);

   void setSideAccess(WorksiteRelativeSide var1, WorksiteInventoryAccess var2);

   Map<WorksiteRelativeSide, WorksiteInventoryAccess> getSideAccessMap();
}
