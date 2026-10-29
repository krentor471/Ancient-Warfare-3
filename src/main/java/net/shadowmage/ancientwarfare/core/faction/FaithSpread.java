package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Faith town by town. Each settlement holds a creed, at first its realm's. Temples and shrines send
 * the creed out to the towns around; preachers, sent by zealous rulers or paid for by a player, work
 * on one town for days until it turns. A town of another creed than its realm's is restless, and a
 * realm most of whose towns have turned takes up the new faith itself. Every grown village wants a
 * shrine of its own.
 */
public final class FaithSpread extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_faith_spread";
   private static final int SPREAD_RANGE = 500;
   private static final double TEMPLE_SPREAD = 0.03;
   private static final double SHRINE_SPREAD = 0.01;
   /** Days a preacher works on a town before it may turn, and the chance each day after. */
   private static final int PREACH_DAYS = 4;
   private static final double PREACH_CHANCE = 0.35;
   public static final int PREACHER_PRICE = 40;
   private static final double REALM_TURNS = 0.6;
   private static final int SHRINE_POPULATION = 12;

   private final Map<Integer, Integer> faiths = new HashMap<>();
   /** Preachers at work: town id to {faith, days}. */
   private final Map<Integer, int[]> preachers = new HashMap<>();

   public static FaithSpread get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(FaithSpread::load, FaithSpread::new, DATA_NAME);
   }

   /** The creed a settlement holds. */
   public static int faith(ServerLevel level, FactionWorldData.Settlement settlement) {
      Integer own = get(level).faiths.get(settlement.id());
      return own != null ? own : FactionFaith.faith(level, settlement.faction());
   }

   private static boolean has(FactionWorldData.Settlement settlement, String word) {
      // Far from any player a town's plans stand for what it has raised: its priests are at work already.
      return settlement.buildings().stream().anyMatch(building -> building.contains(word)) || settlement.plan().stream().anyMatch(building -> building.contains(word));
   }

   /** A preacher sets to work on a town; returns false when one already does or the town already holds that creed. */
   public static boolean sendPreacher(ServerLevel level, FactionWorldData.Settlement town, int faith) {
      FaithSpread spread = get(level);
      if (faith(level, town) == faith || spread.preachers.containsKey(town.id())) {
         return false;
      }

      spread.preachers.put(town.id(), new int[]{faith, 0});
      spread.setDirty();
      return true;
   }

   private void turn(ServerLevel level, FactionWorldData.Settlement town, int faith) {
      if (faith == FactionFaith.faith(level, town.faction())) {
         this.faiths.remove(town.id());
      } else {
         this.faiths.put(town.id(), faith);
      }

      this.preachers.remove(town.id());
      this.setDirty();
      FactionNews.tell(level, Component.translatable("news.ancientwarfare.town_converted", FactionNews.named(town), FactionFaith.name(faith)).withStyle(ChatFormatting.LIGHT_PURPLE));
   }

   static void simulateDay(ServerLevel level, FactionWorldData data) {
      FaithSpread spread = get(level);
      RandomSource random = level.getRandom();
      List<FactionWorldData.Settlement> towns = new ArrayList<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!FactionDiplomacy.isOutlaw(settlement.faction()) && !FactionAI.isFort(settlement)) {
            towns.add(settlement);
         }
      }

      for (FactionWorldData.Settlement town : towns) {
         // A grown village without a holy place of its own builds a shrine first.
         if (town.population() >= SHRINE_POPULATION && !has(town, "shrine") && !has(town, "temple")) {
            town.plan().add(0, TownBuildings.Kind.SHRINE.key());
         }

         double reach = has(town, "temple") ? TEMPLE_SPREAD : has(town, "shrine") ? SHRINE_SPREAD : 0.0;
         if (reach > 0.0 && random.nextDouble() < reach * 4) {
            int faith = faith(level, town);
            for (FactionWorldData.Settlement other : towns) {
               if (other != town && faith(level, other) != faith
                  && other.horizontalDistanceSquared(town.center().getX(), town.center().getZ()) < (double)SPREAD_RANGE * SPREAD_RANGE
                  && random.nextDouble() < 0.25 / (1 + (has(other, "temple") ? 2 : 0))) {
                  spread.turn(level, other, faith);
                  break;
               }
            }
         }

         int faith = faith(level, town);
         if (faith != FactionFaith.faith(level, town.faction())
            && !FactionPeople.has(FactionRulers.ruler(data, town.faction(), random).character(), FactionPeople.Character.TOLERANT)) {
            // Another creed than the realm's: the town chafes under its lord.
            town.order = Math.max(0, town.order - 1);
         }
      }

      // Zealous and pious rulers send preachers to towns of other creeds on their borders.
      for (FactionWorldData.Settlement town : towns) {
         int ruler = FactionRulers.ruler(data, town.faction(), random).character();
         boolean zealous = FactionPeople.has(ruler, FactionPeople.Character.ZEALOUS);
         if ((zealous || FactionPeople.has(ruler, FactionPeople.Character.PIOUS)) && random.nextDouble() < (zealous ? 0.02 : 0.008)) {
            int faith = FactionFaith.faith(level, town.faction());
            for (FactionWorldData.Settlement other : towns) {
               if (faith(level, other) != faith && other.horizontalDistanceSquared(town.center().getX(), town.center().getZ()) < (double)SPREAD_RANGE * SPREAD_RANGE) {
                  sendPreacher(level, other, faith);
                  break;
               }
            }
         }
      }

      for (Map.Entry<Integer, int[]> entry : new ArrayList<>(spread.preachers.entrySet())) {
         FactionWorldData.Settlement town = data.settlement(entry.getKey());
         if (town == null) {
            spread.preachers.remove(entry.getKey());
            continue;
         }

         int[] work = entry.getValue();
         work[1]++;
         // A temple of the town's own creed stands against the preacher.
         double chance = PREACH_CHANCE / (has(town, "temple") ? 3.0 : has(town, "shrine") ? 1.5 : 1.0);
         if (work[1] >= PREACH_DAYS && random.nextDouble() < chance) {
            spread.turn(level, town, work[0]);
         } else if (work[1] > PREACH_DAYS * 4) {
            spread.preachers.remove(entry.getKey());
         }

         spread.setDirty();
      }

      // A realm most of whose towns hold another creed takes it up.
      Map<String, Map<Integer, Integer>> counts = new HashMap<>();
      Map<String, Integer> totals = new HashMap<>();
      for (FactionWorldData.Settlement town : towns) {
         counts.computeIfAbsent(town.faction(), key -> new HashMap<>()).merge(faith(level, town), 1, Integer::sum);
         totals.merge(town.faction(), 1, Integer::sum);
      }

      for (Map.Entry<String, Map<Integer, Integer>> entry : counts.entrySet()) {
         int own = FactionFaith.faith(level, entry.getKey());
         for (Map.Entry<Integer, Integer> held : entry.getValue().entrySet()) {
            if (held.getKey() != own && totals.get(entry.getKey()) >= 3 && held.getValue() >= totals.get(entry.getKey()) * REALM_TURNS) {
               FactionFaith.convert(level, entry.getKey(), held.getKey());
               break;
            }
         }
      }
   }

   private static FaithSpread load(CompoundTag tag) {
      FaithSpread spread = new FaithSpread();
      CompoundTag faiths = tag.getCompound("Faiths");
      for (String key : faiths.getAllKeys()) {
         spread.faiths.put(Integer.parseInt(key), faiths.getInt(key));
      }

      CompoundTag preachers = tag.getCompound("Preachers");
      for (String key : preachers.getAllKeys()) {
         spread.preachers.put(Integer.parseInt(key), preachers.getIntArray(key));
      }

      return spread;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      CompoundTag faiths = new CompoundTag();
      this.faiths.forEach((id, faith) -> faiths.putInt(String.valueOf(id), faith));
      tag.put("Faiths", faiths);
      CompoundTag preachers = new CompoundTag();
      this.preachers.forEach((id, work) -> preachers.putIntArray(String.valueOf(id), work));
      tag.put("Preachers", preachers);
      return tag;
   }
}
