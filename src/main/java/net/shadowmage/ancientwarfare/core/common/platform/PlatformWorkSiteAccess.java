package net.shadowmage.ancientwarfare.core.common.platform;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkSite;

@FunctionalInterface
public interface PlatformWorkSiteAccess {
   Optional<WorkSite> findCompatibleWorkSite(Level var1, BlockPos var2);
}
