package net.shadowmage.ancientwarfare.core.common.worksite;

import net.minecraft.core.BlockPos;

public final class WorksiteBoundsGeometry {
   private WorksiteBoundsGeometry() {
   }

   public static boolean isValid(BlockPos worksite, BlockPos min, BlockPos max, int maxWidth) {
      if (worksite != null && hasValidSize(min, max, maxWidth)) {
         long distanceX = distanceToInterval(worksite.getX(), min.getX(), max.getX());
         long distanceZ = distanceToInterval(worksite.getZ(), min.getZ(), max.getZ());
         return Math.max(distanceX, distanceZ) == 1L;
      } else {
         return false;
      }
   }

   public static boolean hasValidSize(BlockPos min, BlockPos max, int maxWidth) {
      if (min != null && max != null && maxWidth >= 1) {
         long widthX = (long)max.getX() - (long)min.getX() + 1L;
         long widthZ = (long)max.getZ() - (long)min.getZ() + 1L;
         return widthX >= 1L && widthZ >= 1L && widthX <= (long)maxWidth && widthZ <= (long)maxWidth;
      } else {
         return false;
      }
   }

   private static long distanceToInterval(int coordinate, int min, int max) {
      if (coordinate < min) {
         return (long)min - (long)coordinate;
      } else {
         return coordinate > max ? (long)coordinate - (long)max : 0L;
      }
   }

   public static WorksiteBoundsGeometry.Bounds adjust(
      BlockPos worksite, BlockPos min, BlockPos max, int maxWidth, int minXDelta, int maxXDelta, int minZDelta, int maxZDelta
   ) {
      if (min != null && max != null) {
         BlockPos candidateMin = offset(min, (long)minXDelta, (long)minZDelta);
         BlockPos candidateMax = offset(max, (long)maxXDelta, (long)maxZDelta);
         return isValid(worksite, candidateMin, candidateMax, maxWidth) ? new WorksiteBoundsGeometry.Bounds(candidateMin, candidateMax) : null;
      } else {
         return null;
      }
   }

   public static WorksiteBoundsGeometry.Bounds adjustButton(BlockPos worksite, BlockPos min, BlockPos max, int maxWidth, int buttonId) {
      if (buttonId >= 0 && buttonId <= 7) {
         boolean originallyValid = isValid(worksite, min, max, maxWidth);
         WorksiteBoundsGeometry.Bounds sized = clampSize(worksite, min, max, maxWidth);
         if (sized == null) {
            return null;
         } else {
            WorksiteBoundsGeometry.Bounds basis = adjacent(worksite, sized.min(), sized.max(), maxWidth);
            if (basis == null) {
               return null;
            } else {
               min = basis.min();
               max = basis.max();
               boolean changeMinX = min.getX() < worksite.getX();
               boolean changeMinZ = min.getZ() < worksite.getZ();

               WorksiteBoundsGeometry.Bounds result = switch (buttonId) {
                  case 0 -> adjust(worksite, min, max, maxWidth, 0, 0, -1, -1);
                  case 1 -> adjust(worksite, min, max, maxWidth, 0, 0, 1, 1);
                  case 2 -> adjust(worksite, min, max, maxWidth, -1, -1, 0, 0);
                  case 3 -> adjust(worksite, min, max, maxWidth, 1, 1, 0, 0);
                  case 4 -> adjust(worksite, min, max, maxWidth, changeMinX ? 1 : 0, changeMinX ? 0 : -1, 0, 0);
                  case 5 -> adjust(worksite, min, max, maxWidth, changeMinX ? -1 : 0, changeMinX ? 0 : 1, 0, 0);
                  case 6 -> adjust(worksite, min, max, maxWidth, 0, 0, changeMinZ ? 1 : 0, changeMinZ ? 0 : -1);
                  case 7 -> adjust(worksite, min, max, maxWidth, 0, 0, changeMinZ ? -1 : 0, changeMinZ ? 0 : 1);
                  default -> null;
               };
               return result == null && !originallyValid ? basis : result;
            }
         }
      } else {
         return null;
      }
   }

   public static WorksiteBoundsGeometry.Bounds clampSize(BlockPos worksite, BlockPos min, BlockPos max, int maxWidth) {
      if (worksite != null && min != null && max != null && maxWidth >= 1) {
         long minX = (long)min.getX();
         long maxX = (long)max.getX();
         long minZ = (long)min.getZ();
         long maxZ = (long)max.getZ();
         long widthX = maxX - minX + 1L;
         long widthZ = maxZ - minZ + 1L;
         if (widthX >= 1L && widthZ >= 1L) {
            if (widthX > (long)maxWidth) {
               if (minX < (long)worksite.getX()) {
                  minX = maxX - (long)maxWidth + 1L;
               } else {
                  maxX = minX + (long)maxWidth - 1L;
               }
            }

            if (widthZ > (long)maxWidth) {
               if (minZ < (long)worksite.getZ()) {
                  minZ = maxZ - (long)maxWidth + 1L;
               } else {
                  maxZ = minZ + (long)maxWidth - 1L;
               }
            }

            BlockPos candidateMin = position(minX, min.getY(), minZ);
            BlockPos candidateMax = position(maxX, max.getY(), maxZ);
            return candidateMin != null && candidateMax != null ? new WorksiteBoundsGeometry.Bounds(candidateMin, candidateMax) : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static WorksiteBoundsGeometry.Bounds adjacent(BlockPos worksite, BlockPos min, BlockPos max, int maxWidth) {
      if (worksite == null || !hasValidSize(min, max, maxWidth)) {
         return null;
      } else if (isValid(worksite, min, max, maxWidth)) {
         return new WorksiteBoundsGeometry.Bounds(min, max);
      } else {
         long deltaX = towardInterval(worksite.getX(), min.getX(), max.getX());
         long deltaZ = towardInterval(worksite.getZ(), min.getZ(), max.getZ());
         if (deltaX == 0L && deltaZ == 0L) {
            long west = (long)worksite.getX() - 1L - (long)max.getX();
            long east = (long)worksite.getX() + 1L - (long)min.getX();
            long north = (long)worksite.getZ() - 1L - (long)max.getZ();
            long south = (long)worksite.getZ() + 1L - (long)min.getZ();
            deltaX = Math.abs(west) <= Math.abs(east) ? west : east;
            long preferredZ = Math.abs(north) <= Math.abs(south) ? north : south;
            if (Math.abs(preferredZ) < Math.abs(deltaX)) {
               deltaX = 0L;
               deltaZ = preferredZ;
            }
         }

         BlockPos candidateMin = offset(min, deltaX, deltaZ);
         BlockPos candidateMax = offset(max, deltaX, deltaZ);
         return isValid(worksite, candidateMin, candidateMax, maxWidth) ? new WorksiteBoundsGeometry.Bounds(candidateMin, candidateMax) : null;
      }
   }

   private static long towardInterval(int coordinate, int min, int max) {
      if (coordinate < min) {
         return (long)coordinate + 1L - (long)min;
      } else {
         return coordinate > max ? (long)coordinate - 1L - (long)max : 0L;
      }
   }

   private static BlockPos offset(BlockPos pos, long deltaX, long deltaZ) {
      return position((long)pos.getX() + deltaX, pos.getY(), (long)pos.getZ() + deltaZ);
   }

   private static BlockPos position(long x, int y, long z) {
      return x >= -2147483648L && x <= 2147483647L && z >= -2147483648L && z <= 2147483647L ? new BlockPos((int)x, y, (int)z) : null;
   }

   public static record Bounds(BlockPos min, BlockPos max) {
      public Bounds(BlockPos min, BlockPos max) {
         min = min.immutable();
         max = max.immutable();
         this.min = min;
         this.max = max;
      }
   }
}
