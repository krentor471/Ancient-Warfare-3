package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;

/**
 * The world's own villages, free of every country. One becomes known when a player first walks
 * into it. A free village lies between the countries like a prize: a country with a settlement
 * near enough may take it in, and then it is that country's village like any other, its villagers
 * counted among its people and its golems in its garrison; outlaws camped near it raid it. A raid
 * a player sees comes as a band of outlaws going for the villagers and golems, and the village
 * pays whoever drives them off; one no player sees only costs the village some of its people.
 * A player who puts a town hall in a free village makes it his own.
 */
public final class FreeVillages extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_free_villages";
   private static final long TICK_INTERVAL = 40L;
   /** How far a country's settlement may lie from a free village and still reach out for it. */
   private static final int ANNEX_RANGE = 800;
   private static final int RAID_RANGE = 600;
   private static final double ANNEX_CHANCE = 0.06;
   private static final double RAID_CHANCE = 0.12;
   private static final long RAID_REST_DAYS = 5L;
   /** How long a threatened raid waits for a player to see it before it happens out of sight. */
   private static final long RAID_WAIT_DAYS = 2L;
   private static final double WATCH_RANGE = 96.0;
   private static final int RAIDERS_MIN = 3;
   private static final int RAIDERS_MAX = 6;
   private static final long RAID_TICKS = 3600L;
   private static final int RAID_REWARD = 15;
   /** The most villagers a raid costs a village before the raiders make off with their plunder. */
   private static final int RAID_TOLL = 2;
   private static final String RAID_TAG = "AWFreeVillageRaid";
   private final List<Village> villages = new ArrayList<>();
   private int nextId = 1;

   static final class Village {
      int id;
      BlockPos min;
      BlockPos max;
      /** The country that took it in, "player" for a player's, or empty while it is free. */
      String owner = "";
      int settlement = -1;
      int villagers;
      int golems;
      long lastRaidDay = -100L;
      /** The outlaws who threaten it, and the day they did, while a raid is coming. */
      String raider = "";
      long raidDay;
      /** The game time a seen raid began, or 0, and how many villagers there were then. */
      long raidStart;
      int raidVillagers;

      BlockPos center() {
         return new BlockPos((this.min.getX() + this.max.getX()) / 2, (this.min.getY() + this.max.getY()) / 2, (this.min.getZ() + this.max.getZ()) / 2);
      }

      boolean contains(int x, int z, int margin) {
         return x >= this.min.getX() - margin && x <= this.max.getX() + margin && z >= this.min.getZ() - margin && z <= this.max.getZ() + margin;
      }

      AABB area() {
         return new AABB(this.min, this.max.offset(1, 1, 1)).inflate(16.0, 16.0, 16.0);
      }

      String tag() {
         return RAID_TAG + this.id;
      }

      boolean free() {
         return this.owner.isEmpty();
      }
   }

   public static FreeVillages get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(FreeVillages::load, FreeVillages::new, DATA_NAME);
   }

   /** The free villages, for the map: x, z, villagers and golems of each. */
   public static List<int[]> free(ServerLevel level) {
      List<int[]> list = new ArrayList<>();
      for (Village village : get(level).villages) {
         if (village.free()) {
            BlockPos center = village.center();
            list.add(new int[]{center.getX(), center.getZ(), village.villagers, village.golems});
         }
      }

      return list;
   }

   static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % TICK_INTERVAL != 0L || level != level.getServer().overworld()) {
         return;
      }

      FreeVillages villages = get(level);
      for (ServerPlayer player : level.players()) {
         villages.discover(level, data, player);
      }

      for (Village village : villages.villages) {
         BlockPos center = village.center();
         if (!level.hasChunkAt(center) || level.getNearestPlayer(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, WATCH_RANGE, false) == null) {
            continue;
         }

         villages.census(level, village);
         if (village.free() && !village.raider.isEmpty()) {
            villages.raid(level, data, village);
         }
      }
   }

   /** A player standing in one of the world's villages makes it known. */
   private void discover(ServerLevel level, FactionWorldData data, ServerPlayer player) {
      BlockPos pos = player.blockPosition();
      for (Village village : this.villages) {
         if (village.contains(pos.getX(), pos.getZ(), 0)) {
            return;
         }
      }

      StructureStart start = level.structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE);
      if (!start.isValid()) {
         return;
      }

      BoundingBox box = start.getBoundingBox();
      Village village = new Village();
      village.min = new BlockPos(box.minX(), box.minY(), box.minZ());
      village.max = new BlockPos(box.maxX(), box.maxY(), box.maxZ());
      BlockPos center = village.center();
      for (Village other : this.villages) {
         if (other.contains(center.getX(), center.getZ(), 0)) {
            return;
         }
      }

      // A country already built there, or a player's village stands in it: it is not free.
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.contains(center.getX(), center.getZ(), 0)) {
            return;
         }
      }

      for (BlockPos hall : data.villages().keySet()) {
         if (village.contains(hall.getX(), hall.getZ(), 0)) {
            village.owner = "player";
         }
      }

      village.id = this.nextId++;
      this.villages.add(village);
      this.census(level, village);
      this.setDirty();
      AncientWarfareCore.LOG.info("Found free village #{} at {}, {}", village.id, center.getX(), center.getZ());
      if (village.free()) {
         player.sendSystemMessage(Component.translatable("news.ancientwarfare.free_village_found", center.getX(), center.getZ()).withStyle(net.minecraft.ChatFormatting.GREEN));
      }
   }

   /** Counts the villagers and golems a player can see. */
   private void census(ServerLevel level, Village village) {
      AABB area = village.area();
      int villagers = level.getEntitiesOfClass(AbstractVillager.class, area, LivingEntity::isAlive).size();
      int golems = level.getEntitiesOfClass(IronGolem.class, area, LivingEntity::isAlive).size();
      if (villagers != village.villagers || golems != village.golems) {
         village.villagers = villagers;
         village.golems = golems;
         this.setDirty();
      }
   }

   /** A raid a player sees: outlaws come at the edge of the village and go for its people. */
   private void raid(ServerLevel level, FactionWorldData data, Village village) {
      List<FactionNpcEntity> raiders = level.getEntitiesOfClass(FactionNpcEntity.class, village.area().inflate(48.0), npc -> npc.isAlive() && npc.getTags().contains(village.tag()));
      if (village.raidStart == 0L) {
         RandomSource random = level.getRandom();
         BlockPos center = village.center();
         double angle = random.nextDouble() * Math.PI * 2.0;
         double reach = Math.max(village.max.getX() - village.min.getX(), village.max.getZ() - village.min.getZ()) / 2.0 + 8.0;
         int cx = center.getX() + (int)Math.round(Math.cos(angle) * reach);
         int cz = center.getZ() + (int)Math.round(Math.sin(angle) * reach);
         int count = RAIDERS_MIN + random.nextInt(RAIDERS_MAX - RAIDERS_MIN + 1);
         for (int i = 0; i < count * 6 && raiders.size() < count; i++) {
            FactionNpcEntity npc = FactionSimulation.spawnSoldier(level, village.raider, cx + random.nextInt(9) - 4, cz + random.nextInt(9) - 4, village.tag(), 0.0);
            if (npc != null) {
               npc.setPersistenceRequired();
               raiders.add(npc);
            }
         }

         if (raiders.isEmpty()) {
            return;
         }

         village.raidStart = level.getGameTime();
         village.raidVillagers = level.getEntitiesOfClass(AbstractVillager.class, village.area(), LivingEntity::isAlive).size();
         this.setDirty();
         this.tellNear(level, village, Component.translatable("news.ancientwarfare.free_village_raid", FactionNews.factionName(village.raider), center.getX(), center.getZ()));
         return;
      }

      List<LivingEntity> prey = new ArrayList<>(level.getEntitiesOfClass(AbstractVillager.class, village.area(), LivingEntity::isAlive));
      prey.addAll(level.getEntitiesOfClass(IronGolem.class, village.area(), LivingEntity::isAlive));
      int villagers = (int)prey.stream().filter(entity -> entity instanceof AbstractVillager).count();
      // Outlaws come for plunder, not to empty the village: once they have done their worst they go.
      boolean over = raiders.isEmpty() || villagers == 0 || villagers <= village.raidVillagers - RAID_TOLL || level.getGameTime() - village.raidStart > RAID_TICKS;
      if (!over) {
         for (FactionNpcEntity raider : raiders) {
            if (raider.getTarget() == null || !raider.getTarget().isAlive()) {
               prey.stream().min(Comparator.comparingDouble(raider::distanceToSqr)).ifPresent(raider::setTarget);
            }
         }

         return;
      }

      BlockPos center = village.center();
      if (raiders.isEmpty()) {
         // Driven off: the villagers pay those who stood by them.
         for (ServerPlayer player : level.getPlayers(player -> village.area().inflate(32.0).contains(player.position()) && !player.isSpectator())) {
            Coins.give(player, RAID_REWARD);
         }

         this.tellNear(level, village, Component.translatable("news.ancientwarfare.free_village_saved", center.getX(), center.getZ(), RAID_REWARD));
      } else {
         raiders.forEach(npc -> npc.discard());
         this.tellNear(level, village, Component.translatable("news.ancientwarfare.free_village_plundered", FactionNews.factionName(village.raider), center.getX(), center.getZ()));
      }

      village.raider = "";
      village.raidStart = 0L;
      this.setDirty();
   }

   private void tellNear(ServerLevel level, Village village, Component message) {
      for (ServerPlayer player : level.getPlayers(player -> village.area().inflate(WATCH_RANGE).contains(player.position()))) {
         player.sendSystemMessage(message.copy().withStyle(net.minecraft.ChatFormatting.GOLD));
      }
   }

   /** A day between the countries: free villages are taken in or threatened, and lost ones are free again. */
   static void simulateDay(ServerLevel level, FactionWorldData data) {
      FreeVillages villages = get(level);
      long day = data.lastSimulatedDay();
      RandomSource random = level.getRandom();
      for (Village village : villages.villages) {
         if (village.settlement > 0 && data.settlement(village.settlement) == null) {
            // The country lost it: razed, or taken and given up.
            village.owner = "";
            village.settlement = -1;
            villages.setDirty();
         }

         if (!village.free()) {
            continue;
         }

         BlockPos center = village.center();
         if (!village.raider.isEmpty() && village.raidStart == 0L && day - village.raidDay >= RAID_WAIT_DAYS) {
            // Nobody saw it: the outlaws took what they wanted and some of the people went with it.
            village.villagers = Math.max(1, village.villagers - 1 - random.nextInt(2));
            village.raider = "";
            villages.setDirty();
            continue;
         }

         FactionWorldData.Settlement country = nearest(data, center, ANNEX_RANGE, false);
         FactionWorldData.Settlement outlaws = nearest(data, center, RAID_RANGE, true);
         if (outlaws != null && village.raider.isEmpty() && day - village.lastRaidDay >= RAID_REST_DAYS && random.nextDouble() < RAID_CHANCE) {
            village.raider = outlaws.faction();
            village.raidDay = day;
            village.lastRaidDay = day;
            villages.setDirty();
         } else if (country != null && village.raider.isEmpty() && random.nextDouble() < ANNEX_CHANCE) {
            villages.annex(level, data, village, country.faction());
         }
      }
   }

   /** The nearest built settlement of a country (or of outlaws) within range of a spot. */
   private static FactionWorldData.Settlement nearest(FactionWorldData data, BlockPos center, int range, boolean outlaws) {
      FactionWorldData.Settlement best = null;
      double bestDistance = (double)range * range;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (FactionDiplomacy.isOutlaw(settlement.faction()) != outlaws) {
            continue;
         }

         double distance = settlement.horizontalDistanceSquared(center.getX(), center.getZ());
         if (distance < bestDistance) {
            best = settlement;
            bestDistance = distance;
         }
      }

      return best;
   }

   /** A country takes a free village in: its villagers become its people, its golems its garrison. */
   private void annex(ServerLevel level, FactionWorldData data, Village village, String faction) {
      BlockPos center = village.center();
      FactionRegions.Province province = FactionRegions.provinceAt(level, center.getX(), center.getZ());
      FactionWorldData.Settlement settlement = data.addSettlement(
         faction,
         FactionTowns.VILLAGE,
         "minecraft:village",
         village.min,
         village.max,
         province.key(),
         level.getGameTime(),
         2 + village.golems * 2,
         Math.max(8, village.villagers * 4)
      );
      settlement.manned = true;
      village.owner = faction;
      village.settlement = settlement.id();
      this.setDirty();
      AncientWarfareCore.LOG.info("{} took in free village #{} as settlement #{}", faction, village.id, settlement.id());
      FactionNews.tell(level, Component.translatable("news.ancientwarfare.free_village_annexed", FactionNews.factionName(faction), center.getX(), center.getZ()));
   }

   private Village nearestFree(double x, double z) {
      Village best = null;
      double bestDistance = Double.MAX_VALUE;
      for (Village village : this.villages) {
         BlockPos center = village.center();
         double distance = (center.getX() - x) * (center.getX() - x) + (center.getZ() - z) * (center.getZ() - z);
         if (village.free() && distance < bestDistance) {
            best = village;
            bestDistance = distance;
         }
      }

      return best;
   }

   /** For operators: a faction takes in the free village nearest a spot at once. */
   public static boolean annexNearest(ServerLevel level, String faction, double x, double z) {
      FreeVillages villages = get(level);
      Village village = villages.nearestFree(x, z);
      if (village == null) {
         return false;
      }

      villages.annex(level, FactionWorldData.get(level), village, faction);
      return true;
   }

   /** For operators: a faction's band raids the free village nearest a spot once a player is by. */
   public static boolean raidNearest(ServerLevel level, String faction, double x, double z) {
      FreeVillages villages = get(level);
      Village village = villages.nearestFree(x, z);
      if (village == null) {
         return false;
      }

      village.raider = faction;
      village.raidDay = FactionWorldData.get(level).lastSimulatedDay();
      village.raidStart = 0L;
      villages.setDirty();
      return true;
   }

   /** A town hall put down in a free village makes it the player's. */
   public static void onTownHallPlaced(ServerLevel level, BlockPos pos) {
      FreeVillages villages = get(level);
      for (Village village : villages.villages) {
         if (village.free() && village.contains(pos.getX(), pos.getZ(), 0)) {
            village.owner = "player";
            village.raider = "";
            villages.setDirty();
         }
      }
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      tag.putInt("NextId", this.nextId);
      ListTag list = new ListTag();
      for (Village village : this.villages) {
         CompoundTag entry = new CompoundTag();
         entry.putInt("Id", village.id);
         entry.put("Min", NbtUtils.writeBlockPos(village.min));
         entry.put("Max", NbtUtils.writeBlockPos(village.max));
         entry.putString("Owner", village.owner);
         entry.putInt("Settlement", village.settlement);
         entry.putInt("Villagers", village.villagers);
         entry.putInt("Golems", village.golems);
         entry.putLong("LastRaidDay", village.lastRaidDay);
         entry.putString("Raider", village.raider);
         entry.putLong("RaidDay", village.raidDay);
         list.add(entry);
      }

      tag.put("Villages", list);
      return tag;
   }

   private static FreeVillages load(CompoundTag tag) {
      FreeVillages villages = new FreeVillages();
      villages.nextId = Math.max(1, tag.getInt("NextId"));
      for (Tag raw : tag.getList("Villages", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)raw;
         Village village = new Village();
         village.id = entry.getInt("Id");
         village.min = NbtUtils.readBlockPos(entry.getCompound("Min"));
         village.max = NbtUtils.readBlockPos(entry.getCompound("Max"));
         village.owner = entry.getString("Owner");
         village.settlement = entry.getInt("Settlement");
         village.villagers = entry.getInt("Villagers");
         village.golems = entry.getInt("Golems");
         village.lastRaidDay = entry.getLong("LastRaidDay");
         village.raider = entry.getString("Raider");
         village.raidDay = entry.getLong("RaidDay");
         villages.villages.add(village);
      }

      return villages;
   }
}
