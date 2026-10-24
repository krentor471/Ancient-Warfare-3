package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;

/**
 * Keeps countries in one piece. Wars move single provinces back and forth, which would leave the
 * map a patchwork: land nobody of its owner lives on drifts to the neighbour that surrounds it, and
 * a peace hands over the settlements each side holds deep inside the other's land.
 */
public final class FactionBorders {
   /** Of the eight provinces around an empty one, this many of one neighbour's take it over... */
   private static final int SURROUNDED = 5;
   /** ...as long as its owner holds no more than this many of them. */
   private static final int CUT_OFF = 2;
   /** Wild land this surrounded by one country becomes part of it. */
   private static final int WILD_SURROUNDED = 6;
   /** Of the four provinces beside a settlement's, at least this many of the other side's make it an enclave. */
   private static final int ENCLAVE_SIDES = 3;
   private static final int[][] AROUND = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};
   private static final int[][] BESIDE = {{0, -1}, {-1, 0}, {1, 0}, {0, 1}};

   private FactionBorders() {
   }

   /** Empty land that is cut off from its owner's country passes to the country around it. */
   public static void simulateDay(ServerLevel level, FactionWorldData data) {
      Map<Long, String> owners = data.provinceOwners();
      Set<Long> settled = new HashSet<>();
      data.settlements().forEach(settlement -> {
         if (settlement.faction().equals(owners.get(settlement.province()))) {
            settled.add(settlement.province());
         }
      });
      Map<Long, String> changes = new HashMap<>();
      owners.forEach((province, owner) -> {
         if (settled.contains(province) || PlayerCountry.isLand(owner)) {
            return;
         }

         Map<String, Integer> around = count(owners, province, AROUND);
         int own = owner.isEmpty() ? 0 : around.getOrDefault(owner, 0);
         for (Map.Entry<String, Integer> entry : around.entrySet()) {
            String neighbour = entry.getKey();
            if (neighbour.isEmpty() || neighbour.equals(owner) || FactionDiplomacy.isOutlaw(neighbour) || PlayerCountry.isLand(neighbour)) {
               continue;
            }

            boolean takes = owner.isEmpty() ? entry.getValue() >= WILD_SURROUNDED : entry.getValue() >= SURROUNDED && own <= CUT_OFF;
            if (takes) {
               changes.put(province, neighbour);
               break;
            }
         }
      });
      changes.forEach(data::setProvinceOwner);
      if (!changes.isEmpty()) {
         data.setDirty();
      }
   }

   /** At peace, each side gives the other the settlements it holds inside the other's land. */
   public static void settlePeace(ServerLevel level, FactionWorldData data, String faction, String other) {
      cede(level, data, faction, other);
      cede(level, data, other, faction);
   }

   private static void cede(ServerLevel level, FactionWorldData data, String from, String to) {
      Map<Long, String> owners = data.provinceOwners();
      List<FactionWorldData.Settlement> enclaves = new ArrayList<>();

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.faction().equals(from) && !FactionTowns.CAPITAL.equals(settlement.kind())) {
            Map<String, Integer> beside = count(owners, settlement.province(), BESIDE);
            if (beside.getOrDefault(from, 0) == 0 && beside.getOrDefault(to, 0) >= ENCLAVE_SIDES) {
               enclaves.add(settlement);
            }
         }
      }

      for (FactionWorldData.Settlement settlement : enclaves) {
         settlement.capturedFrom = "";
         settlement.loyalty = 60;
         settlement.setFaction(to);
         data.setProvinceOwner(settlement.province(), to);
      }

      if (!enclaves.isEmpty()) {
         data.setDirty();
         FactionNews.ceded(level, from, to, enclaves.size());
      }
   }

   private static Map<String, Integer> count(Map<Long, String> owners, long province, int[][] offsets) {
      int cellX = ChunkPos.getX(province);
      int cellZ = ChunkPos.getZ(province);
      Map<String, Integer> counts = new HashMap<>();

      for (int[] offset : offsets) {
         String owner = owners.get(ChunkPos.asLong(cellX + offset[0], cellZ + offset[1]));
         if (owner != null) {
            counts.merge(owner, 1, Integer::sum);
         }
      }

      return counts;
   }
}
