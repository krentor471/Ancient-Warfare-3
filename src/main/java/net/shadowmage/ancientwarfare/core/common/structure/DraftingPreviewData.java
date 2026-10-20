package net.shadowmage.ancientwarfare.core.common.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public record DraftingPreviewData(int sizeX, int sizeY, int sizeZ, List<DraftingPreviewData.Cell> cells, boolean simplified) {
   public static final int MAX_CELLS = 32768;
   public static final int MAX_SOURCE_CELLS = 1048576;
   public static final int MAX_AXIS = 65535;
   public static final int CELLS_PER_CHUNK = 1024;
   public static final int MAX_CHUNKS = 32;
   public static final int MAX_PACKET_BYTES = 30000;
   public static final int MAX_TOTAL_BYTES = 1000000;

   public DraftingPreviewData(int sizeX, int sizeY, int sizeZ, List<DraftingPreviewData.Cell> cells, boolean simplified) {
      if (sizeX >= 1 && sizeY >= 1 && sizeZ >= 1 && sizeX <= 65535 && sizeY <= 65535 && sizeZ <= 65535 && cells != null && cells.size() <= 32768) {
         cells = List.copyOf(cells);

         for (DraftingPreviewData.Cell cell : cells) {
            if (cell == null
               || cell.state() == null
               || cell.pos() == null
               || cell.pos().m_123341_() < 0
               || cell.pos().m_123341_() >= sizeX
               || cell.pos().m_123342_() < 0
               || cell.pos().m_123342_() >= sizeY
               || cell.pos().m_123343_() < 0
               || cell.pos().m_123343_() >= sizeZ) {
               throw new IllegalArgumentException("Invalid drafting preview cell");
            }
         }

         this.sizeX = sizeX;
         this.sizeY = sizeY;
         this.sizeZ = sizeZ;
         this.cells = cells;
         this.simplified = simplified;
      } else {
         throw new IllegalArgumentException("Invalid drafting preview bounds");
      }
   }

   public static DraftingPreviewData empty() {
      return new DraftingPreviewData(1, 1, 1, List.of(), false);
   }

   public static DraftingPreviewData fromCells(List<DraftingPreviewData.Cell> source) {
      if (source != null && !source.isEmpty()) {
         if (source.size() > 1048576) {
            throw new IllegalArgumentException("Drafting preview source too large");
         } else {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;

            for (DraftingPreviewData.Cell cell : source) {
               if (visible(cell)) {
                  minX = Math.min(minX, cell.pos().m_123341_());
                  maxX = Math.max(maxX, cell.pos().m_123341_());
                  minY = Math.min(minY, cell.pos().m_123342_());
                  maxY = Math.max(maxY, cell.pos().m_123342_());
                  minZ = Math.min(minZ, cell.pos().m_123343_());
                  maxZ = Math.max(maxZ, cell.pos().m_123343_());
               }
            }

            if (minX == Integer.MAX_VALUE) {
               return empty();
            } else {
               int sizeX = boundedSize(minX, maxX);
               int sizeY = boundedSize(minY, maxY);
               int sizeZ = boundedSize(minZ, maxZ);
               Map<Long, DraftingPreviewData.Cell> occupied = new LinkedHashMap<>();

               for (DraftingPreviewData.Cell cellx : source) {
                  if (visible(cellx)) {
                     BlockPos pos = new BlockPos(cellx.pos().m_123341_() - minX, cellx.pos().m_123342_() - minY, cellx.pos().m_123343_() - minZ);
                     occupied.put(positionKey(pos), new DraftingPreviewData.Cell(pos, cellx.state()));
                  }
               }

               Map<BlockState, Boolean> opaque = new HashMap<>();
               List<DraftingPreviewData.Cell> surface = new ArrayList<>();

               for (DraftingPreviewData.Cell cellxx : occupied.values()) {
                  if (!opaqueCube(cellxx.state(), opaque) || !surrounded(cellxx.pos(), occupied, opaque)) {
                     surface.add(cellxx);
                  }
               }

               boolean simplified = surface.size() > 32768;
               if (simplified) {
                  int step = 2;

                  while (true) {
                     Map<Long, DraftingPreviewData.Cell> buckets = new LinkedHashMap<>();

                     for (DraftingPreviewData.Cell cellxxx : surface) {
                        BlockPos bucket = new BlockPos(cellxxx.pos().m_123341_() / step, cellxxx.pos().m_123342_() / step, cellxxx.pos().m_123343_() / step);
                        long key = positionKey(bucket);
                        DraftingPreviewData.Cell current = buckets.get(key);
                        if (current == null || cellxxx.pos().m_123342_() > current.pos().m_123342_()) {
                           buckets.put(key, cellxxx);
                        }
                     }

                     if (buckets.size() <= 32768) {
                        surface = new ArrayList<>(buckets.values());
                        break;
                     }

                     step *= 2;
                  }
               }

               return new DraftingPreviewData(sizeX, sizeY, sizeZ, surface, simplified);
            }
         }
      } else {
         return empty();
      }
   }

   private static int boundedSize(int min, int max) {
      long size = (long)max - (long)min + 1L;
      if (size >= 1L && size <= 65535L) {
         return (int)size;
      } else {
         throw new IllegalArgumentException("Drafting preview axis too large");
      }
   }

   private static boolean visible(DraftingPreviewData.Cell cell) {
      return cell != null && !cell.state().m_60795_() && !cell.state().m_60713_(Blocks.f_50454_);
   }

   private static boolean opaqueCube(BlockState state, Map<BlockState, Boolean> cache) {
      return cache.computeIfAbsent(
         state, candidate -> candidate.m_60815_() && Block.m_49916_(candidate.m_60768_(EmptyBlockGetter.INSTANCE, BlockPos.f_121853_))
      );
   }

   private static boolean surrounded(BlockPos pos, Map<Long, DraftingPreviewData.Cell> occupied, Map<BlockState, Boolean> opaque) {
      Direction[] var3 = Direction.values();
      int var4 = var3.length;
      int var5 = 0;

      while (var5 < var4) {
         Direction direction = var3[var5];
         BlockPos neighbor = pos.m_121945_(direction);
         if (neighbor.m_123341_() >= 0 && neighbor.m_123342_() >= 0 && neighbor.m_123343_() >= 0) {
            DraftingPreviewData.Cell cell = occupied.get(positionKey(neighbor));
            if (cell != null && opaqueCube(cell.state(), opaque)) {
               var5++;
               continue;
            }

            return false;
         }

         return false;
      }

      return true;
   }

   private static long positionKey(BlockPos pos) {
      return (long)pos.m_123341_() << 32 | (long)pos.m_123342_() << 16 | (long)pos.m_123343_();
   }

   public int chunkCount() {
      return Math.max(1, (this.cells.size() + 1024 - 1) / 1024);
   }

   public DraftingPreviewData chunk(int index) {
      if (index >= 0 && index < this.chunkCount()) {
         int start = index * 1024;
         return new DraftingPreviewData(
            this.sizeX, this.sizeY, this.sizeZ, this.cells.subList(start, Math.min(this.cells.size(), start + 1024)), this.simplified
         );
      } else {
         throw new IllegalArgumentException("Invalid preview chunk");
      }
   }

   public static void writeChunk(FriendlyByteBuf buffer, DraftingPreviewData data) {
      if (data.cells().size() > 1024) {
         throw new IllegalArgumentException("Preview packet too large");
      } else {
         buffer.writeShort(data.sizeX());
         buffer.writeShort(data.sizeY());
         buffer.writeShort(data.sizeZ());
         buffer.writeBoolean(data.simplified());
         buffer.m_130130_(data.cells().size());

         for (DraftingPreviewData.Cell cell : data.cells()) {
            buffer.writeShort(cell.pos().m_123341_());
            buffer.writeShort(cell.pos().m_123342_());
            buffer.writeShort(cell.pos().m_123343_());
            buffer.m_130130_(Block.f_49791_.m_7447_(cell.state()));
         }
      }
   }

   public static DraftingPreviewData readChunk(FriendlyByteBuf buffer) {
      if (buffer.readableBytes() >= 30000) {
         throw new IllegalArgumentException("Preview packet too large");
      } else {
         int sizeX = buffer.readUnsignedShort();
         int sizeY = buffer.readUnsignedShort();
         int sizeZ = buffer.readUnsignedShort();
         boolean simplified = buffer.readBoolean();
         int count = buffer.m_130242_();
         if (count >= 0 && count <= 1024) {
            List<DraftingPreviewData.Cell> cells = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
               BlockPos pos = new BlockPos(buffer.readUnsignedShort(), buffer.readUnsignedShort(), buffer.readUnsignedShort());
               int stateId = buffer.m_130242_();
               BlockState state = stateId < 0 ? null : (BlockState)Block.f_49791_.m_7942_(stateId);
               if (state == null) {
                  throw new IllegalArgumentException("Invalid preview block state");
               }

               cells.add(new DraftingPreviewData.Cell(pos, state));
            }

            if (buffer.isReadable()) {
               throw new IllegalArgumentException("Unexpected preview packet data");
            } else {
               return new DraftingPreviewData(sizeX, sizeY, sizeZ, cells, simplified);
            }
         } else {
            throw new IllegalArgumentException("Invalid preview cell count");
         }
      }
   }

   public static void requireChunkMetadata(int index, int count) {
      if (count < 1 || count > 32 || index < 0 || index >= count) {
         throw new IllegalArgumentException("Invalid drafting preview chunk metadata");
      }
   }

   public static record Cell(BlockPos pos, BlockState state) {
      public Cell(BlockPos pos, BlockState state) {
         if (pos != null && state != null) {
            pos = pos.m_7949_();
            this.pos = pos;
            this.state = state;
         } else {
            throw new IllegalArgumentException("Missing preview cell");
         }
      }
   }
}
