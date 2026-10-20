package net.shadowmage.ancientwarfare.core.common.worksite;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;

public final class WorkSites {
   private static final int BOUNDED_SEARCH_RADIUS = 16;

   private WorkSites() {
   }

   public static Optional<WorkSite> get(Level level, BlockPos pos) {
      if (level != null && pos != null) {
         if (level.m_7702_(pos) instanceof WorkSite workSite) {
            return Optional.of(workSite);
         } else {
            Optional<WorkSite> compatibilitySite = AWCorePlatform.findCompatibleWorkSite(level, pos);
            return compatibilitySite.isPresent() ? compatibilitySite : findBoundedWorkSite(level, pos);
         }
      } else {
         return Optional.empty();
      }
   }

   private static Optional<WorkSite> findBoundedWorkSite(Level level, BlockPos target) {
      BlockPos min = target.m_7918_(-16, -2, -16);
      BlockPos max = target.m_7918_(16, 2, 16);

      for (BlockPos pos : BlockPos.m_121940_(min, max)) {
         if (level.m_7702_(pos) instanceof BoundedWorkSite boundedWorkSite && boundedWorkSite.containsWorkTarget(target)) {
            return Optional.of(boundedWorkSite);
         }
      }

      return Optional.empty();
   }
}
