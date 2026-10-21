package net.shadowmage.ancientwarfare.core.common.npc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Where a group stands when ordered somewhere: the slots of a formation on the ground, or a line the player drew. Shared
 * by the server, which hands out the slots, and the client, which shows them before the order is given.
 */
public final class FormationPlanner {
   /** How far a slot may shift sideways when its own block is a tree, water or a cliff. */
   private static final int MAX_SHIFT = 3;
   private static final int[] HEIGHT_STEPS = {0, 1, -1, 2, -2, 3, -3, -4, 4};
   /** Rows behind a drawn line stand this far apart. */
   private static final int PATH_DEPTH_SPACING = 2;

   private FormationPlanner() {
   }

   /**
    * A slot of a planned formation. {@code rank} counts rows back from the front; inside a rank slots come in
    * {@code order} from left to right, so the NPCs can be matched to them without crossing paths.
    */
   public static record Spot(BlockPos pos, float yaw, int rank, double order) {
   }

   /** Formation slots around {@code target}, front row first, every slot moved onto ground a soldier can stand on. */
   public static List<Spot> grid(Level level, BlockPos target, Direction facing, NpcFormation formation, int count, int columns) {
      Direction right = facing.getClockWise();
      float yaw = facing.toYRot();
      Set<BlockPos> taken = new HashSet<>();
      List<Spot> spots = new ArrayList<>(count);

      for (NpcFormation.Slot slot : formation.slots(count, columns)) {
         int x = target.getX() + right.getStepX() * slot.side() - facing.getStepX() * slot.depth();
         int z = target.getZ() + right.getStepZ() * slot.side() - facing.getStepZ() * slot.depth();
         spots.add(new Spot(ground(level, x, z, target.getY(), taken), yaw, slot.row(), slot.side()));
      }

      return spots;
   }

   /**
    * Slots along a line the player drew, as many abreast as fit at {@code spacing}, more rows behind when they do not.
    * An open line faces to the left of the drawing direction, as a drawn front does; a closed loop faces outwards.
    */
   public static List<Spot> alongPath(Level level, List<BlockPos> points, int count, int spacing) {
      Path path = Path.of(points);
      List<Spot> spots = new ArrayList<>(count);
      if (count <= 0 || path == null) {
         return spots;
      }

      int perRank = path.closed ? Math.max(1, (int)(path.length / spacing)) : (int)(path.length / spacing) + 1;
      Set<BlockPos> taken = new HashSet<>();
      int placed = 0;

      for (int rank = 0; placed < count; rank++) {
         int inRank = Math.min(perRank, count - placed);
         for (int i = 0; i < inRank; i++) {
            double t;
            if (path.closed) {
               t = path.length * i / inRank;
            } else {
               t = inRank == 1 ? path.length * 0.5 : path.length * i / (inRank - 1);
            }

            double[] point = path.at(t);
            double[] facing = path.facing(t);
            double x = point[0] - facing[0] * rank * PATH_DEPTH_SPACING;
            double z = point[1] - facing[1] * rank * PATH_DEPTH_SPACING;
            float yaw = (float)Math.toDegrees(Math.atan2(-facing[0], facing[1]));
            BlockPos pos = ground(level, (int)Math.floor(x), (int)Math.floor(z), path.y, taken);
            spots.add(new Spot(pos, yaw, rank, t));
         }

         placed += inRank;
      }

      return spots;
   }

   /** Distance along the drawn line to the point of it nearest to (x, z); orders NPCs inside a rank. */
   public static double pathOrder(List<BlockPos> points, double x, double z) {
      Path path = Path.of(points);
      return path == null ? 0.0 : path.nearest(x, z);
   }

   /** True when the drawn points lie close to the straight line between the first and the last. */
   public static boolean isStraight(List<BlockPos> points, double tolerance) {
      if (points.size() < 3) {
         return true;
      }

      BlockPos first = points.get(0);
      BlockPos last = points.get(points.size() - 1);
      double dx = last.getX() - first.getX();
      double dz = last.getZ() - first.getZ();
      double length = Math.sqrt(dx * dx + dz * dz);
      if (length < 1.0) {
         return false;
      }

      for (BlockPos point : points) {
         double off = Math.abs((point.getX() - first.getX()) * dz - (point.getZ() - first.getZ()) * dx) / length;
         if (off > tolerance) {
            return false;
         }
      }

      return true;
   }

   /** The spot nearest to (x, z) at about height {@code refY} where a soldier can stand and that no one else holds. */
   public static BlockPos ground(Level level, int x, int z, int refY, Set<BlockPos> taken) {
      for (int radius = 0; radius <= MAX_SHIFT; radius++) {
         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) == radius) {
                  BlockPos spot = standable(level, x + dx, z + dz, refY);
                  if (spot != null && taken.add(spot)) {
                     return spot;
                  }
               }
            }
         }
      }

      BlockPos fallback = new BlockPos(x, refY, z);
      taken.add(fallback);
      return fallback;
   }

   private static BlockPos standable(Level level, int x, int z, int refY) {
      for (int dy : HEIGHT_STEPS) {
         BlockPos feet = new BlockPos(x, refY + dy, z);
         if (canStand(level, feet)) {
            return feet;
         }
      }

      return null;
   }

   /** Solid ground that is not a tree, two free blocks above it and no water or lava. */
   public static boolean canStand(Level level, BlockPos feet) {
      BlockPos below = feet.below();
      BlockState ground = level.getBlockState(below);
      if (ground.is(BlockTags.LEAVES) || ground.is(BlockTags.LOGS)) {
         return false;
      } else {
         VoxelShape shape = ground.getCollisionShape(level, below);
         return !shape.isEmpty() && shape.max(Axis.Y) >= 0.5 && isOpen(level, feet) && isOpen(level, feet.above());
      }
   }

   private static boolean isOpen(Level level, BlockPos pos) {
      return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
   }

   /** The drawn line as segments in block-centre coordinates. */
   private static final class Path {
      private final double[] xs;
      private final double[] zs;
      private final double[] starts;
      private final double length;
      private final boolean closed;
      private final int y;
      private final double centerX;
      private final double centerZ;

      private Path(double[] xs, double[] zs, boolean closed, int y) {
         this.xs = xs;
         this.zs = zs;
         this.closed = closed;
         this.y = y;
         this.starts = new double[xs.length];
         double total = 0.0;
         double sumX = 0.0;
         double sumZ = 0.0;

         for (int i = 0; i < xs.length; i++) {
            this.starts[i] = total;
            if (i + 1 < xs.length) {
               total += Math.hypot(xs[i + 1] - xs[i], zs[i + 1] - zs[i]);
            }

            sumX += xs[i];
            sumZ += zs[i];
         }

         this.length = total;
         this.centerX = sumX / xs.length;
         this.centerZ = sumZ / xs.length;
      }

      private static Path of(List<BlockPos> points) {
         List<BlockPos> clean = new ArrayList<>();
         for (BlockPos point : points) {
            if (clean.isEmpty() || !clean.get(clean.size() - 1).equals(point)) {
               clean.add(point);
            }
         }

         if (clean.size() < 2) {
            return null;
         }

         double drawn = 0.0;
         for (int i = 1; i < clean.size(); i++) {
            drawn += Math.sqrt(clean.get(i).distSqr(clean.get(i - 1)));
         }

         BlockPos first = clean.get(0);
         BlockPos last = clean.get(clean.size() - 1);
         double gap = Math.hypot(last.getX() - first.getX(), last.getZ() - first.getZ());
         // A loop: the line comes back near where it began. It is closed so the ring has no gap.
         boolean closed = clean.size() >= 4 && drawn >= 8.0 && gap <= Math.max(3.0, drawn * 0.15);
         if (closed && !last.equals(first)) {
            clean.add(first);
         }

         double[] xs = new double[clean.size()];
         double[] zs = new double[clean.size()];
         long ySum = 0L;
         for (int i = 0; i < clean.size(); i++) {
            xs[i] = clean.get(i).getX() + 0.5;
            zs[i] = clean.get(i).getZ() + 0.5;
            ySum += clean.get(i).getY();
         }

         Path path = new Path(xs, zs, closed, (int)Math.round((double)ySum / clean.size()));
         return path.length < 1.0 ? null : path;
      }

      private int segment(double t) {
         for (int i = this.xs.length - 2; i >= 0; i--) {
            if (t >= this.starts[i]) {
               return i;
            }
         }

         return 0;
      }

      private double[] at(double t) {
         int i = this.segment(t);
         double segmentLength = this.starts[i + 1] - this.starts[i];
         double f = segmentLength <= 1.0E-6 ? 0.0 : Math.min(1.0, (t - this.starts[i]) / segmentLength);
         return new double[]{this.xs[i] + (this.xs[i + 1] - this.xs[i]) * f, this.zs[i] + (this.zs[i + 1] - this.zs[i]) * f};
      }

      /** Unit vector the slot at {@code t} faces: left of the drawing direction, or away from the middle of a loop. */
      private double[] facing(double t) {
         // Average the direction over a few blocks so a wobbly hand does not turn single soldiers sideways.
         double[] a = this.at(Math.max(0.0, t - 2.0));
         double[] b = this.at(Math.min(this.length, t + 2.0));
         double dx = b[0] - a[0];
         double dz = b[1] - a[1];
         double norm = Math.hypot(dx, dz);
         if (norm < 1.0E-6) {
            return new double[]{0.0, 1.0};
         }

         // Same side as a drawn front of the grid formation: the drawing direction is the formation's right hand.
         double fx = dz / norm;
         double fz = -dx / norm;
         if (this.closed) {
            double[] p = this.at(t);
            if ((p[0] - this.centerX) * fx + (p[1] - this.centerZ) * fz < 0.0) {
               fx = -fx;
               fz = -fz;
            }
         }

         return new double[]{fx, fz};
      }

      private double nearest(double x, double z) {
         double best = Double.MAX_VALUE;
         double bestT = 0.0;

         for (int i = 0; i + 1 < this.xs.length; i++) {
            double sx = this.xs[i + 1] - this.xs[i];
            double sz = this.zs[i + 1] - this.zs[i];
            double lengthSq = sx * sx + sz * sz;
            double f = lengthSq <= 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, ((x - this.xs[i]) * sx + (z - this.zs[i]) * sz) / lengthSq));
            double px = this.xs[i] + sx * f;
            double pz = this.zs[i] + sz * f;
            double distance = (px - x) * (px - x) + (pz - z) * (pz - z);
            if (distance < best) {
               best = distance;
               bestT = this.starts[i] + Math.sqrt(lengthSq) * f;
            }
         }

         return bestT;
      }
   }
}
