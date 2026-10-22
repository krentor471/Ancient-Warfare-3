package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.entity.PlayerOwnedNpcEntity;
import net.shadowmage.ancientwarfare.core.tile.TownHallBlockEntity;

/**
 * A player's own country. At one of their town halls, with enough people and coin, a player founds
 * it and names it; that hall is its capital. Every province where one of the player's villages
 * stands and no faction settlement is left becomes the country's land, held on the map as
 * "player:" and the founder's id. All villages pay tax into one treasury, the capital's town hall,
 * at one rate the player sets there: the higher it is, the lower order and loyalty, up to revolt.
 */
public final class PlayerCountry {
   public static final String LAND_PREFIX = "player:";
   public static final int FOUND_RESIDENTS = 10;
   /** In silver. */
   public static final int FOUND_COST = 50;
   public static final int MAX_NAME = 24;
   private static final int RESIDENT_RANGE = 64;
   private static final long CHECK_TICKS = 200L;
   private static final int OWN_VILLAGE_LOYALTY = 70;
   private static final int OWN_VILLAGE_ORDER = 60;
   /** In silver, for a militiaman's arms; there is no pay after that. */
   public static final int RECRUIT_COST = 5;
   private static final double GUARD_RANGE = 48.0;
   /** Militia a village raises by itself when attacked, and sends to a neighbour in need. */
   private static final int RAISED_AT_HOME = 4;
   private static final int SENT_BY_NEIGHBOUR = 2;
   private static final int HELPING_NEIGHBOURS = 2;
   private static final int HELP_RANGE = 800;

   private PlayerCountry() {
   }

   /** The province owner that marks the player's land. */
   public static String land(UUID owner) {
      return LAND_PREFIX + owner;
   }

   public static boolean isLand(String owner) {
      return owner != null && owner.startsWith(LAND_PREFIX);
   }

   /** The founder whose land this owner marks, or null for a faction or wild land. */
   public static UUID founder(String owner) {
      if (!isLand(owner)) {
         return null;
      }

      try {
         return UUID.fromString(owner.substring(LAND_PREFIX.length()));
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   /** The country name for land the player holds, for maps and lists; the raw owner otherwise. */
   public static String landName(FactionWorldData data, String owner) {
      UUID founder = founder(owner);
      FactionWorldData.Country country = founder == null ? null : data.country(founder);
      return country == null ? owner : country.name;
   }

   /** The owner's people living around the hall, or a taken village's own count when that is more. */
   public static int residents(ServerLevel level, FactionWorldData data, BlockPos hall, UUID owner) {
      int counted = level.isLoaded(hall)
         ? level.getEntitiesOfClass(
               PlayerOwnedNpcEntity.class,
               new AABB(hall).inflate(RESIDENT_RANGE, 32.0, RESIDENT_RANGE),
               npc -> npc.isAlive() && npc.getOwner().map(owner::equals).orElse(false)
            )
            .size()
         : 0;
      FactionWorldData.VillageState state = data.villageState(hall);
      return state == null ? counted : Math.max(counted, state.people);
   }

   /** The player asked, at the hall, to found a country with that name. */
   public static void found(ServerPlayer player, BlockPos hall, String rawName) {
      ServerLevel level = player.serverLevel();
      FactionWorldData data = FactionWorldData.get(level);
      UUID id = player.getUUID();
      String name = rawName.strip();
      if (name.length() > MAX_NAME) {
         name = name.substring(0, MAX_NAME).strip();
      }

      if (data.country(id) != null) {
         refuse(player, "country.ancientwarfare.already");
      } else if (!id.equals(data.villages().get(hall)) || player.distanceToSqr(hall.getX() + 0.5, hall.getY() + 0.5, hall.getZ() + 0.5) > 64.0) {
         refuse(player, "country.ancientwarfare.not_here");
      } else if (name.isEmpty()) {
         refuse(player, "country.ancientwarfare.no_name");
      } else if (nameTaken(data, name)) {
         refuse(player, "country.ancientwarfare.name_taken");
      } else if (residents(level, data, hall, id) < FOUND_RESIDENTS) {
         refuse(player, "country.ancientwarfare.few_people", residents(level, data, hall, id), FOUND_RESIDENTS);
      } else if (!Coins.take(player, FOUND_COST)) {
         refuse(player, "country.ancientwarfare.no_coins", FOUND_COST);
      } else {
         FactionWorldData.Country country = data.addCountry(id, name, hall, level.getGameTime());
         check(level, data, id, country);
         int provinces = data.provincesOf(land(id)).size();
         player.sendSystemMessage(Component.translatable("country.ancientwarfare.founded", name, provinces).withStyle(ChatFormatting.GREEN));
         FactionNews.tell(level, Component.translatable("news.ancientwarfare.country_founded", player.getDisplayName(), name));
         AncientWarfareCore.LOG.info("{} founds the country {} at {}", player.getScoreboardName(), name, hall);
      }
   }

   private static boolean nameTaken(FactionWorldData data, String name) {
      return data.countries().values().stream().anyMatch(country -> country.name.equalsIgnoreCase(name));
   }

   private static void refuse(ServerPlayer player, String key, Object... args) {
      player.sendSystemMessage(Component.translatable(key, args).withStyle(ChatFormatting.RED));
   }

   /** Now and then: each country keeps a capital, its villages and land up to date, and the treasury reaches the capital. */
   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % CHECK_TICKS != 0L || data.countries().isEmpty()) {
         return;
      }

      for (Map.Entry<UUID, FactionWorldData.Country> entry : Map.copyOf(data.countries()).entrySet()) {
         check(level, data, entry.getKey(), entry.getValue());
      }
   }

   private static void check(ServerLevel level, FactionWorldData data, UUID owner, FactionWorldData.Country country) {
      if (!owner.equals(data.villages().get(country.capital)) && !moveCapital(level, data, owner, country)) {
         return;
      }

      for (BlockPos hall : data.villages().keySet()) {
         if (owner.equals(data.villages().get(hall))) {
            village(level, data, hall, owner);
         }
      }

      claimLand(level, data, owner);
      if (country.pending > 0 && level.isLoaded(country.capital) && level.getBlockEntity(country.capital) instanceof TownHallBlockEntity capital) {
         Coins.store(capital, country.pending);
         capital.setChanged();
         country.pending = 0;
         data.setDirty();
      }
   }

   /** One of the country's villages: its own get a state like taken ones, so tax, order and loyalty work the same. */
   private static void village(ServerLevel level, FactionWorldData data, BlockPos hall, UUID owner) {
      FactionWorldData.VillageState state = data.villageState(hall);
      if (state == null) {
         state = new FactionWorldData.VillageState("", residents(level, data, hall, owner), OWN_VILLAGE_LOYALTY, OWN_VILLAGE_ORDER);
         FactionWorldData.Country country = data.country(owner);
         FactionWorldData.VillageState capital = data.villageState(country.capital);
         state.taxLevel = capital == null ? 1 : capital.taxLevel;
         data.setVillageState(hall, state);
      } else if (state.from.isEmpty() && level.isLoaded(hall)) {
         state.people = residents(level, data, hall, owner);
      }

      if (state.from.isEmpty()) {
         data.setTax(hall, Mth.clamp(state.people / 4, 1, 16));
      }
   }

   /** The capital is gone: the nearest other village takes its place, or the country falls apart. */
   private static boolean moveCapital(ServerLevel level, FactionWorldData data, UUID owner, FactionWorldData.Country country) {
      BlockPos old = country.capital;
      BlockPos next = null;
      for (Map.Entry<BlockPos, UUID> village : data.villages().entrySet()) {
         if (owner.equals(village.getValue()) && (next == null || village.getKey().distSqr(old) < next.distSqr(old))) {
            next = village.getKey();
         }
      }

      if (next == null) {
         data.removeCountry(owner);
         claimLand(level, data, owner);
         FactionNews.tellPlayer(level, owner, Component.translatable("country.ancientwarfare.fallen", country.name).withStyle(ChatFormatting.RED));
         FactionNews.tell(level, Component.translatable("news.ancientwarfare.country_fallen", country.name));
         return false;
      }

      country.capital = next;
      data.setDirty();
      FactionNews.tellPlayer(
         level, owner, Component.translatable("country.ancientwarfare.capital_moved", country.name, next.getX(), next.getZ()).withStyle(ChatFormatting.YELLOW)
      );
      return true;
   }

   /**
    * The country holds each province with one of its villages where no faction settlement stands;
    * provinces it held without a village any more go wild. A player without a country holds none.
    */
   private static void claimLand(ServerLevel level, FactionWorldData data, UUID owner) {
      String land = land(owner);
      Set<Long> wanted = new HashSet<>();
      if (data.country(owner) != null) {
         for (Map.Entry<BlockPos, UUID> village : data.villages().entrySet()) {
            if (owner.equals(village.getValue())) {
               wanted.add(FactionRegions.provinceAt(level, village.getKey().getX(), village.getKey().getZ()).key());
            }
         }
      }

      for (long province : wanted) {
         String current = data.provinceOwner(province);
         // Another country keeps what it holds; a faction keeps a province while it still lives there.
         if (!land.equals(current) && !isLand(current) && !factionSettles(data, province, current)) {
            data.setProvinceOwner(province, land);
         }
      }

      for (long province : data.provincesOf(land)) {
         if (!wanted.contains(province)) {
            data.setProvinceOwner(province, "");
         }
      }
   }

   /** Whether the faction holding the province still has a settlement there. */
   private static boolean factionSettles(FactionWorldData data, long province, String faction) {
      if (faction == null || faction.isEmpty()) {
         return false;
      }

      return data.settlements().stream().anyMatch(settlement -> settlement.province() == province && settlement.faction().equals(faction));
   }

   /** Soldiers a village can keep from its own people: a third of them, at least two. */
   public static int militiaCap(int people) {
      return Math.max(2, people / 3);
   }

   private static int guards(ServerLevel level, BlockPos hall, UUID owner) {
      return level.getEntitiesOfClass(
            net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity.class,
            new AABB(hall).inflate(GUARD_RANGE),
            npc -> npc.isAlive() && npc.getOwner().map(owner::equals).orElse(false)
         )
         .size();
   }

   /** At a hall of the player's country: one of the villagers takes up arms, for the price of them. */
   public static void recruit(ServerPlayer player, BlockPos hall) {
      ServerLevel level = player.serverLevel();
      FactionWorldData data = FactionWorldData.get(level);
      UUID owner = player.getUUID();
      FactionWorldData.VillageState state = data.villageState(hall);
      if (data.country(owner) == null || state == null || !owner.equals(data.villages().get(hall))) {
         return;
      }

      int guards = guards(level, hall, owner);
      int cap = militiaCap(state.people);
      if (guards >= cap) {
         refuse(player, "country.ancientwarfare.militia_full", guards, cap);
         return;
      }

      if (!Coins.take(player, RECRUIT_COST)) {
         refuse(player, "country.ancientwarfare.no_coins", RECRUIT_COST);
         return;
      }

      if (militia(level, hall, owner, hall, 4) != null) {
         if (!state.from.isEmpty()) {
            state.people = Math.max(1, state.people - 1);
         }

         state.guards = guards + 1;
         data.setDirty();
         player.sendSystemMessage(Component.translatable("country.ancientwarfare.recruited", guards + 1, cap).withStyle(ChatFormatting.GREEN));
      }
   }

   /** A militiaman of the owner's, spawned near a spot and set to guard the village. */
   private static net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity militia(ServerLevel level, BlockPos near, UUID owner, BlockPos post, int spread) {
      var random = level.getRandom();
      int x = near.getX() + random.nextInt(spread * 2 + 1) - spread;
      int z = near.getZ() + random.nextInt(spread * 2 + 1) - spread;
      BlockPos at = new BlockPos(x, 0, z);
      if (!level.isLoaded(at)) {
         return null;
      }

      int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity npc = net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess
         .<net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity>entityType(net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds.COMBAT_NPC)
         .create(level);
      if (npc == null) {
         return null;
      }

      npc.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
      npc.setNpcType("combat");
      npc.setOwner(owner);
      npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STONE_SWORD));
      npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_HELMET));
      npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_CHESTPLATE));
      npc.setCustomName(Component.translatable("country.ancientwarfare.militiaman"));
      level.addFreshEntity(npc);
      npc.setHome(post);
      if (post.equals(near)) {
         npc.setHoldCommand(npc.blockPosition());
      } else {
         npc.setMoveCommand(post);
      }

      return npc;
   }

   /**
    * Raiders reached one of the country's villages: its people take up arms, and the nearest other
    * villages of the country send theirs to help. A village outside a country stands alone.
    */
   public static void callToArms(ServerLevel level, FactionWorldData data, BlockPos hall, UUID owner) {
      FactionWorldData.VillageState state = data.villageState(hall);
      if (owner == null || data.country(owner) == null || state == null) {
         return;
      }

      int raised = 0;
      int room = Math.min(RAISED_AT_HOME, militiaCap(state.people) - guards(level, hall, owner));
      for (int i = 0; i < room; i++) {
         if (militia(level, hall, owner, hall, 6) != null) {
            raised++;
         }
      }

      List<Map.Entry<BlockPos, UUID>> neighbours = new java.util.ArrayList<>();
      for (Map.Entry<BlockPos, UUID> village : data.villages().entrySet()) {
         if (owner.equals(village.getValue()) && !village.getKey().equals(hall) && village.getKey().distSqr(hall) <= (double)HELP_RANGE * HELP_RANGE) {
            neighbours.add(village);
         }
      }

      neighbours.sort(java.util.Comparator.comparingDouble(village -> village.getKey().distSqr(hall)));
      int helpers = 0;
      for (Map.Entry<BlockPos, UUID> village : neighbours.subList(0, Math.min(HELPING_NEIGHBOURS, neighbours.size()))) {
         // They come along the road from their village, already near enough to be seen.
         double dx = village.getKey().getX() - hall.getX();
         double dz = village.getKey().getZ() - hall.getZ();
         double length = Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
         BlockPos from = hall.offset((int)(dx / length * 28), 0, (int)(dz / length * 28));
         for (int i = 0; i < SENT_BY_NEIGHBOUR; i++) {
            if (militia(level, from, owner, hall, 3) != null) {
               helpers++;
            }
         }
      }

      if (raised > 0 || helpers > 0) {
         FactionNews.tellPlayer(
            level, owner, Component.translatable("country.ancientwarfare.call_to_arms", hall.getX(), hall.getZ(), raised, helpers).withStyle(ChatFormatting.GOLD)
         );
      }
   }

   /** Tax day: every village of a country pays into its treasury instead of its own hall. Returns false for villages outside a country. */
   public static boolean collect(FactionWorldData data, BlockPos hall, UUID owner, int tax) {
      FactionWorldData.Country country = data.country(owner);
      if (country == null) {
         return false;
      }

      country.pending += tax;
      country.income += tax;
      data.setDirty();
      return true;
   }

   /** Before tax day: the day's income starts again from nothing. */
   public static void startTaxDay(FactionWorldData data) {
      data.countries().values().forEach(country -> country.income = 0);
   }

   /** The rate the player set at a hall applies to every village of their country. */
   public static void setTaxLevel(FactionWorldData data, UUID owner, int taxLevel) {
      for (Map.Entry<BlockPos, UUID> village : data.villages().entrySet()) {
         FactionWorldData.VillageState state = owner.equals(village.getValue()) ? data.villageState(village.getKey()) : null;
         if (state != null) {
            state.taxLevel = taxLevel;
         }
      }

      data.setDirty();
   }
}
