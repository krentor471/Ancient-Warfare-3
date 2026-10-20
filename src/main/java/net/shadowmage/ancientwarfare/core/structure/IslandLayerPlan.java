package net.shadowmage.ancientwarfare.core.structure;

import java.util.Arrays;
import java.util.function.IntPredicate;

public final class IslandLayerPlan {
   private static final int[] EMPTY = new int[0];
   private final int sizeX;
   private final int sizeY;
   private final int sizeZ;
   private final int horizontalCells;
   private final int minX;
   private final int minZ;
   private final int turns;
   private final int minChunkX;
   private final int minChunkZ;
   private final int chunkWidth;
   private final int chunkCount;
   private final int[][][] layers;
   private IslandLayerPlan.CellAccumulator[] preparingCells;
   private int layer;
   private int cellCursor;
   private int finalizeCursor;

   public IslandLayerPlan(int sizeX, int sizeY, int sizeZ, int minX, int minZ, int turns) {
      if (sizeX > 0 && sizeY > 0 && sizeZ > 0) {
         this.sizeX = sizeX;
         this.sizeY = sizeY;
         this.sizeZ = sizeZ;
         this.horizontalCells = Math.multiplyExact(sizeX, sizeZ);
         Math.multiplyExact(this.horizontalCells, sizeY);
         this.minX = minX;
         this.minZ = minZ;
         this.turns = Math.floorMod(turns, 4);
         int rotatedSizeX = (this.turns & 1) == 0 ? sizeX : sizeZ;
         int rotatedSizeZ = (this.turns & 1) == 0 ? sizeZ : sizeX;
         this.minChunkX = Math.floorDiv(minX, 16);
         this.minChunkZ = Math.floorDiv(minZ, 16);
         int maxChunkX = Math.floorDiv(Math.addExact(minX, rotatedSizeX - 1), 16);
         int maxChunkZ = Math.floorDiv(Math.addExact(minZ, rotatedSizeZ - 1), 16);
         this.chunkWidth = maxChunkX - this.minChunkX + 1;
         this.chunkCount = Math.multiplyExact(this.chunkWidth, maxChunkZ - this.minChunkZ + 1);
         this.layers = new int[sizeY][][];
      } else {
         throw new IllegalArgumentException("Island dimensions must be positive");
      }
   }

   public int sizeX() {
      return this.sizeX;
   }

   public int sizeY() {
      return this.sizeY;
   }

   public int sizeZ() {
      return this.sizeZ;
   }

   public int chunkCount() {
      return this.chunkCount;
   }

   public int minChunkX() {
      return this.minChunkX;
   }

   public int minChunkZ() {
      return this.minChunkZ;
   }

   public int chunkWidth() {
      return this.chunkWidth;
   }

   public boolean prepare(IslandWorkBudget budget, IntPredicate occupied) {
      while (this.layer < this.sizeY) {
         if (this.preparingCells == null) {
            this.preparingCells = new IslandLayerPlan.CellAccumulator[this.chunkCount];
         }

         for (; this.cellCursor < this.horizontalCells; this.cellCursor++) {
            if (!budget.tryScan()) {
               return false;
            }

            int localCell = this.cellCursor;
            if (occupied.test(this.layer * this.horizontalCells + localCell)) {
               int chunk = this.chunkFor(localCell);
               IslandLayerPlan.CellAccumulator accumulator = this.preparingCells[chunk];
               if (accumulator == null) {
                  accumulator = new IslandLayerPlan.CellAccumulator();
                  this.preparingCells[chunk] = accumulator;
               }

               accumulator.add(localCell);
            }
         }

         if (this.layers[this.layer] == null) {
            this.layers[this.layer] = new int[this.chunkCount][];
         }

         while (this.finalizeCursor < this.chunkCount) {
            if (!budget.tryScan()) {
               return false;
            }

            IslandLayerPlan.CellAccumulator accumulator = this.preparingCells[this.finalizeCursor];
            this.layers[this.layer][this.finalizeCursor] = accumulator == null ? EMPTY : accumulator.finish();
            this.preparingCells[this.finalizeCursor] = null;
            this.finalizeCursor++;
         }

         this.layer++;
         this.cellCursor = 0;
         this.finalizeCursor = 0;
         this.preparingCells = null;
      }

      return true;
   }

   public int[] cells(int layer, int chunk) {
      if (layer >= 0 && layer < this.sizeY && chunk >= 0 && chunk < this.chunkCount) {
         int[][] preparedLayer = this.layers[layer];
         if (preparedLayer != null && preparedLayer[chunk] != null) {
            return preparedLayer[chunk];
         } else {
            throw new IllegalStateException("Island layer/chunk has not been prepared: " + layer + "/" + chunk);
         }
      } else {
         throw new IndexOutOfBoundsException("Invalid island layer/chunk: " + layer + "/" + chunk);
      }
   }

   private int chunkFor(int localCell) {
      int localX = localCell % this.sizeX;
      int localZ = localCell / this.sizeX;
      int rotatedX;
      int rotatedZ;
      switch (this.turns) {
         case 1:
            rotatedX = this.sizeZ - 1 - localZ;
            rotatedZ = localX;
            break;
         case 2:
            rotatedX = this.sizeX - 1 - localX;
            rotatedZ = this.sizeZ - 1 - localZ;
            break;
         case 3:
            rotatedX = localZ;
            rotatedZ = this.sizeX - 1 - localX;
            break;
         default:
            rotatedX = localX;
            rotatedZ = localZ;
      }

      int chunkX = Math.floorDiv(this.minX + rotatedX, 16) - this.minChunkX;
      int chunkZ = Math.floorDiv(this.minZ + rotatedZ, 16) - this.minChunkZ;
      return chunkZ * this.chunkWidth + chunkX;
   }

   private static final class CellAccumulator {
      private int[] values = new int[8];
      private int size;

      void add(int cell) {
         if (this.size == 256) {
            throw new IllegalStateException("An island layer/chunk cannot contain more than 256 cells");
         } else {
            if (this.size == this.values.length) {
               this.values = Arrays.copyOf(this.values, Math.min(256, this.values.length * 2));
            }

            this.values[this.size++] = cell;
         }
      }

      int[] finish() {
         return Arrays.copyOf(this.values, this.size);
      }
   }
}
