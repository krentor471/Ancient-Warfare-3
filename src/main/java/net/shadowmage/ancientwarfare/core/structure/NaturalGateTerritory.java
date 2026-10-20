package net.shadowmage.ancientwarfare.core.structure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.entity.GateEntity;

public final class NaturalGateTerritory {
   private static final String RESOURCE_ROOT = "/assets/ancientwarfare/template/";
   private static final int MAX_METADATA_LINES = 512;
   private static final int MAX_METADATA_CHARS = 65536;
   private static final int MAX_METADATA_CACHE = 256;
   private static final long RECORD_CACHE_TICKS = 20L;
   private static final Map<GateEntity, NaturalGateTerritory.Attribution> ATTRIBUTIONS = new WeakHashMap<>();
   private static final Map<String, NaturalGateTerritory.Metadata> METADATA = new LinkedHashMap<String, NaturalGateTerritory.Metadata>(32, 0.75F, true) {
      @Override
      protected boolean removeEldestEntry(Entry<String, NaturalGateTerritory.Metadata> eldest) {
         return this.size() > 256;
      }
   };

   private NaturalGateTerritory() {
   }

   public static UUID claimedOwner(ServerLevel level, GateEntity gate) {
      return TerritoryProtectionData.get(level).claimedOwner(gate.minPos(), gate.maxPos());
   }

   public static boolean hasClaimedTerritory(ServerLevel level, GateEntity gate) {
      return TerritoryProtectionData.get(level).hasClaimedTerritory(gate.minPos(), gate.maxPos());
   }

   public static String faction(ServerLevel level, GateEntity gate) {
      if (hasClaimedTerritory(level, gate)) {
         return "";
      } else {
         List<AwsWorldGenerationData.Entry> structures = AwsWorldGenerationData.get(level).entries();
         List<AwtWorldGenerationData.TownTask> towns = AwtWorldGenerationData.get(level).towns();
         long now = level.m_46467_();
         NaturalGateTerritory.Attribution cached = ATTRIBUTIONS.get(gate);
         if (cached != null && cached.valid(level, gate, structures.size(), towns.size(), now)) {
            return cached.faction();
         } else {
            BlockPos min = gate.minPos();
            BlockPos max = gate.maxPos();
            List<NaturalGateTerritory.TownState> townStates = new ArrayList<>();
            String townFaction = "";
            boolean townConflict = false;

            for (AwtWorldGenerationData.TownTask town : towns) {
               NaturalGateTerritory.Metadata metadata = metadata(town.path);
               if (containsTown(town, metadata.chunks(), min, max)) {
                  boolean failed = town.phase == AwtWorldGenerationData.Phase.FAILED;
                  townStates.add(new NaturalGateTerritory.TownState(new WeakReference<>(town), failed));
                  if (!failed && !metadata.faction().isEmpty()) {
                     if (!townFaction.isEmpty() && !townFaction.equals(metadata.faction())) {
                        townConflict = true;
                     }

                     townFaction = metadata.faction();
                  }
               }
            }

            String result = townConflict ? "" : townFaction;
            if (townFaction.isEmpty() && !townConflict) {
               for (AwsWorldGenerationData.Entry structure : structures) {
                  if (contains(structure, min, max)) {
                     String candidate = metadata(structure.template()).faction();
                     if (!candidate.isEmpty()) {
                        if (!result.isEmpty() && !result.equals(candidate)) {
                           result = "";
                           break;
                        }

                        result = candidate;
                     }
                  }
               }
            }

            ATTRIBUTIONS.put(
               gate,
               new NaturalGateTerritory.Attribution(new WeakReference<>(level), min, max, structures.size(), towns.size(), now, result, List.copyOf(townStates))
            );
            return result;
         }
      }
   }

   private static boolean contains(AwsWorldGenerationData.Entry entry, BlockPos min, BlockPos max) {
      return entry.minX() <= min.m_123341_()
         && entry.minY() <= min.m_123342_()
         && entry.minZ() <= min.m_123343_()
         && entry.maxX() >= max.m_123341_()
         && entry.maxY() >= max.m_123342_()
         && entry.maxZ() >= max.m_123343_();
   }

   private static boolean containsTown(AwtWorldGenerationData.TownTask town, int chunks, BlockPos min, BlockPos max) {
      if (chunks <= 0) {
         return false;
      } else {
         long size = (long)chunks * 16L;
         long minX = (long)town.x - size / 2L;
         long minZ = (long)town.z - size / 2L;
         return minX <= (long)min.m_123341_()
            && minZ <= (long)min.m_123343_()
            && minX + size - 1L >= (long)max.m_123341_()
            && minZ + size - 1L >= (long)max.m_123343_();
      }
   }

   private static NaturalGateTerritory.Metadata metadata(String path) {
      NaturalGateTerritory.Metadata cached = METADATA.get(path);
      if (cached != null) {
         return cached;
      } else {
         NaturalGateTerritory.Metadata loaded = readMetadata(path);
         METADATA.put(path, loaded);
         return loaded;
      }
   }

   private static NaturalGateTerritory.Metadata readMetadata(String path) {
      if (path != null && !path.isBlank() && !path.contains("..") && !path.startsWith("/") && path.indexOf(92) < 0) {
         boolean town = path.toLowerCase(Locale.ROOT).endsWith(".awt");
         String faction = "";
         int minSize = 3;
         int maxSize = 9;
         int chars = 0;
         boolean ended = false;

         try (InputStream stream = NaturalGateTerritory.class.getResourceAsStream("/assets/ancientwarfare/template/" + path)) {
            if (stream == null) {
               return NaturalGateTerritory.Metadata.EMPTY;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
               for (int count = 0; count < 512; count++) {
                  String line = reader.readLine();
                  if (line == null) {
                     break;
                  }

                  chars += line.length();
                  if (chars > 65536) {
                     return NaturalGateTerritory.Metadata.EMPTY;
                  }

                  line = line.trim();
                  if (town ? line.equalsIgnoreCase(":endheader") : line.startsWith("#### LAYERS")) {
                     ended = true;
                     break;
                  }

                  if (!line.isEmpty() && !line.startsWith("#")) {
                     int split = line.indexOf(61);
                     if (split >= 0) {
                        String key = line.substring(0, split).trim().toLowerCase(Locale.ROOT);
                        String value = line.substring(split + 1).trim();
                        if ("territoryname".equals(key)) {
                           faction = value.toLowerCase(Locale.ROOT);
                        } else if (town && "minsize".equals(key)) {
                           minSize = Integer.parseInt(value);
                        } else if (town && "maxsize".equals(key)) {
                           maxSize = Integer.parseInt(value);
                        }
                     }
                  }
               }
            }
         } catch (IllegalArgumentException | IOException var18) {
            return NaturalGateTerritory.Metadata.EMPTY;
         }

         if (!ended) {
            return NaturalGateTerritory.Metadata.EMPTY;
         } else {
            if (!FactionRegistry.factionNames().contains(faction)) {
               faction = "";
            }

            return new NaturalGateTerritory.Metadata(faction, town ? Math.max(0, Math.max(minSize, maxSize)) : 0);
         }
      } else {
         return NaturalGateTerritory.Metadata.EMPTY;
      }
   }

   private static record Attribution(
      WeakReference<ServerLevel> level,
      BlockPos min,
      BlockPos max,
      int structures,
      int towns,
      long checkedAt,
      String faction,
      List<NaturalGateTerritory.TownState> townStates
   ) {
      boolean valid(ServerLevel currentLevel, GateEntity gate, int structureCount, int townCount, long now) {
         return this.level.get() == currentLevel
            && this.structures == structureCount
            && this.towns == townCount
            && this.min.equals(gate.minPos())
            && this.max.equals(gate.maxPos())
            && now >= this.checkedAt
            && now - this.checkedAt < 20L
            && this.townStates.stream().allMatch(NaturalGateTerritory.TownState::valid);
      }
   }

   private static record Metadata(String faction, int chunks) {
      private static final NaturalGateTerritory.Metadata EMPTY = new NaturalGateTerritory.Metadata("", 0);
   }

   private static record TownState(WeakReference<AwtWorldGenerationData.TownTask> task, boolean failed) {
      boolean valid() {
         AwtWorldGenerationData.TownTask current = this.task.get();
         return current != null && current.phase == AwtWorldGenerationData.Phase.FAILED == this.failed;
      }
   }
}
