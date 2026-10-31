package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;

/**
 * Town councils and guilds, in the flesh. A town hall gets a long table with chairs, and the
 * councillors sit at it through the day; a guild hall the same, with its masters. The council
 * makes demands of its lord now and then: lower the tax, raise a wall, drive out the foreigners,
 * hold a feast. A demand left unmet sours the town; a player who speaks with the councillors can
 * meet it for them and win the town's goodwill. The guild masters take a player into the guild
 * for a fee, and members buy cheaper from the town's craftsmen and are given guild contracts.
 */
public final class CouncilHalls extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_councils";
   public static final String COUNCIL_TAG = "AWCouncil";
   private static final String SEAT_TAG = "AWSeat";
   private static final String MEMBER = "AWGuilds";
   private static final int NEAR = 64;
   private static final int SEATS = 4;
   private static final long CHECK_TICKS = 100L;
   public static final int GUILD_FEE = 50;
   /** Days a demand stands before the town turns sour over it. */
   private static final int PATIENCE = 6;

   public enum Demand {
      TAX(30), WALL(60), EXPEL(0), FEAST(25), ROAD(40), GRAIN(30);

      /** What meeting it costs a player, in silver. */
      final int price;

      Demand(int price) {
         this.price = price;
      }

      String key() {
         return this.name().toLowerCase(java.util.Locale.ROOT);
      }
   }

   /** Each town's standing demand and the day it was made. */
   private final Map<Integer, long[]> demands = new HashMap<>();

   public static CouncilHalls get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(CouncilHalls::load, CouncilHalls::new, DATA_NAME);
   }

   private static boolean council(FactionWorldData.Hall hall) {
      return hall.kind().equals(TownBuildings.Kind.TOWN_HALL.key()) || hall.kind().equals(TownBuildings.Kind.GUILD.key());
   }

   /** Furnishes the halls near players and seats their people. */
   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % CHECK_TICKS != 0L || level.players().isEmpty()) {
         return;
      }

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (FactionDiplomacy.isOutlaw(settlement.faction()) || !level.hasNearbyAlivePlayer(settlement.center().getX(), settlement.center().getY(), settlement.center().getZ(), NEAR + 32)) {
            continue;
         }

         for (FactionWorldData.Hall hall : settlement.halls()) {
            if (council(hall) && level.isLoaded(hall.min()) && level.isLoaded(hall.max())) {
               List<BlockPos> chairs = furnish(level, hall);
               seat(level, settlement, hall, chairs);
            }
         }
      }
   }

   /** A table down the middle of the hall and chairs either side, where the floor is free; returns the chairs. */
   private static List<BlockPos> furnish(ServerLevel level, FactionWorldData.Hall hall) {
      int floor = hall.min().getY() + 1;
      int midX = (hall.min().getX() + hall.max().getX()) / 2;
      int midZ = (hall.min().getZ() + hall.max().getZ()) / 2;
      boolean alongX = hall.max().getX() - hall.min().getX() >= hall.max().getZ() - hall.min().getZ();
      List<BlockPos> chairs = new ArrayList<>();
      for (int i = -2; i <= 2; i++) {
         BlockPos table = alongX ? new BlockPos(midX + i, floor, midZ) : new BlockPos(midX, floor, midZ + i);
         BlockState there = level.getBlockState(table);
         if (there.isAir()) {
            level.setBlockAndUpdate(table, Blocks.DARK_OAK_FENCE.defaultBlockState());
            level.setBlockAndUpdate(table.above(), Blocks.BROWN_CARPET.defaultBlockState());
         }

         if (Math.abs(i) == 2) {
            continue;
         }

         for (int side = -1; side <= 1; side += 2) {
            BlockPos chair = alongX ? table.offset(0, 0, side) : table.offset(side, 0, 0);
            Direction facing = alongX ? (side < 0 ? Direction.NORTH : Direction.SOUTH) : (side < 0 ? Direction.WEST : Direction.EAST);
            BlockState seat = level.getBlockState(chair);
            if (seat.isAir() && level.getBlockState(chair.above()).isAir()) {
               level.setBlockAndUpdate(chair, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing));
               seat = level.getBlockState(chair);
            }

            // Only a chair at the council table: a template's own staircase is no seat.
            if (seat.is(Blocks.OAK_STAIRS) && seat.getValue(StairBlock.FACING) == facing && level.getBlockState(table).is(Blocks.DARK_OAK_FENCE)
               && !level.getBlockState(chair.below()).isAir()) {
               chairs.add(chair);
            }
         }
      }

      return chairs;
   }

   private static void seat(ServerLevel level, FactionWorldData.Settlement settlement, FactionWorldData.Hall hall, List<BlockPos> chairs) {
      AABB box = new AABB(hall.min(), hall.max().offset(1, 1, 1)).inflate(1.0);
      String key = hall.kind() + "@" + hall.min().asLong();
      List<FactionNpcEntity> seated = level.getEntitiesOfClass(FactionNpcEntity.class, box, npc -> key.equals(npc.getPersistentData().getString(COUNCIL_TAG)));
      long night = level.getDayTime() % FactionSimulation.DAY_TICKS;
      if (night >= 13000L) {
         // They go home for the night; empty chairs until morning.
         return;
      }

      RandomSource random = level.getRandom();
      for (int i = seated.size(); i < Math.min(SEATS, chairs.size()); i++) {
         BlockPos chair = chairs.get(i);
         EntityType<? extends FactionNpcEntity> type = random.nextBoolean() ? AWCoreEntityTypes.FACTION_CIVILIAN_MALE_NPC.get() : AWCoreEntityTypes.FACTION_CIVILIAN_FEMALE_NPC.get();
         FactionNpcEntity npc = type.create(level);
         ArmorStand stand = EntityType.ARMOR_STAND.create(level);
         if (npc == null || stand == null) {
            return;
         }

         stand.moveTo(chair.getX() + 0.5, chair.getY() + 0.3, chair.getZ() + 0.5, 0.0F, 0.0F);
         CompoundTag standTag = new CompoundTag();
         stand.addAdditionalSaveData(standTag);
         standTag.putBoolean("Marker", true);
         standTag.putBoolean("Invisible", true);
         standTag.putBoolean("NoGravity", true);
         standTag.putBoolean("Invulnerable", true);
         stand.readAdditionalSaveData(standTag);
         stand.addTag(SEAT_TAG);
         level.addFreshEntity(stand);
         npc.moveTo(chair.getX() + 0.5, chair.getY() + 0.3, chair.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
         npc.finalizeSpawn(level, level.getCurrentDifficultyAt(chair), MobSpawnType.EVENT, null, null);
         npc.setFactionName(settlement.faction());
         npc.setPersistenceRequired();
         npc.getPersistentData().putString(COUNCIL_TAG, key);
         level.addFreshEntity(npc);
         npc.startRiding(stand, true);
      }
   }

   /** A seat whose sitter has gone (killed, or left for the night) is taken down. */
   public static void tidy(ServerLevel level) {
      if (level.getGameTime() % (CHECK_TICKS * 6) != 0L) {
         return;
      }

      for (ServerPlayer player : level.players()) {
         for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, player.getBoundingBox().inflate(NEAR), stand -> stand.getTags().contains(SEAT_TAG))) {
            if (stand.getPassengers().isEmpty()) {
               stand.discard();
            } else if (level.getDayTime() % FactionSimulation.DAY_TICKS >= 13000L) {
               // Night: the councillors leave their chairs and go home to bed like everyone else.
               stand.getPassengers().forEach(passenger -> {
                  passenger.stopRiding();
                  passenger.discard();
               });
               stand.discard();
            }
         }
      }
   }

   static void simulateDay(ServerLevel level, FactionWorldData data) {
      CouncilHalls councils = get(level);
      RandomSource random = level.getRandom();
      long day = data.lastSimulatedDay();
      for (FactionWorldData.Settlement town : data.settlements()) {
         if (town.halls().stream().noneMatch(hall -> hall.kind().equals(TownBuildings.Kind.TOWN_HALL.key()))) {
            continue;
         }

         long[] demand = councils.demands.get(town.id());
         if (demand == null) {
            if (random.nextDouble() < 0.08) {
               Demand chosen = choose(town, random);
               councils.demands.put(town.id(), new long[]{chosen.ordinal(), day});
               councils.setDirty();
            }

            continue;
         }

         // The lord meets some demands himself; the rest fester.
         if (random.nextDouble() < 0.06) {
            councils.demands.remove(town.id());
            town.order = Math.min(100, town.order + 3);
            councils.setDirty();
         } else if (day - demand[1] > PATIENCE) {
            town.order = Math.max(0, town.order - 1);
         }
      }
   }

   private static Demand choose(FactionWorldData.Settlement town, RandomSource random) {
      if (town.famine() > 0 || town.food() < town.population() * 3) {
         return Demand.GRAIN;
      }

      if (TownBuildings.defence(town) < 3 && random.nextBoolean()) {
         return Demand.WALL;
      }

      Demand[] all = Demand.values();
      return all[random.nextInt(all.length)];
   }

   private static MutableComponent button(Component label, String command, ChatFormatting color) {
      return Component.literal("[").append(label).append("]")
         .withStyle(style -> style.withColor(color).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, label)));
   }

   private static FactionWorldData.Settlement townOf(ServerLevel level, FactionNpcEntity npc) {
      return FactionSimulation.settlementAt(FactionWorldData.get(level), npc.getFactionName(), npc.getX(), npc.getZ(), NEAR);
   }

   /** A player speaks with a councillor or guild master. */
   public static void talk(ServerPlayer player, FactionNpcEntity npc) {
      ServerLevel level = player.serverLevel();
      FactionWorldData.Settlement town = townOf(level, npc);
      if (town == null) {
         return;
      }

      String id = "/awtownsfolk " + npc.getId() + " ";
      if (npc.getPersistentData().getString(COUNCIL_TAG).startsWith(TownBuildings.Kind.GUILD.key())) {
         boolean member = member(player, town);
         FactionRumours.say(player, npc.getDisplayName(), Component.translatable(member ? "council.ancientwarfare.guild.member" : "council.ancientwarfare.guild.hello"));
         MutableComponent buttons = Component.literal("  ");
         if (!member) {
            buttons.append(button(Component.translatable("council.ancientwarfare.guild.join", GUILD_FEE), id + "guild_join", ChatFormatting.GOLD)).append("  ");
         } else {
            buttons.append(button(Component.translatable("council.ancientwarfare.guild.contract"), id + "errand", ChatFormatting.GOLD)).append("  ");
         }

         buttons.append(button(Component.translatable("council.ancientwarfare.guild.caravan", FactionCaravans.ESCORT_PAY), id + "caravan_hire", ChatFormatting.GREEN));

         player.sendSystemMessage(buttons);
         return;
      }

      long[] demand = get(level).demands.get(town.id());
      if (demand == null) {
         FactionRumours.say(player, npc.getDisplayName(), Component.translatable("council.ancientwarfare.content"));
         return;
      }

      Demand wish = Demand.values()[(int)demand[0]];
      FactionRumours.say(player, npc.getDisplayName(), Component.translatable("council.ancientwarfare.demand." + wish.key()));
      MutableComponent buttons = Component.literal("  ");
      if (wish.price > 0) {
         buttons.append(button(Component.translatable("council.ancientwarfare.meet", wish.price), id + "council_meet", ChatFormatting.GREEN)).append("  ");
      }

      buttons.append(button(Component.translatable("council.ancientwarfare.speak"), id + "council_speak", ChatFormatting.AQUA));
      player.sendSystemMessage(buttons);
   }

   /** Guild members' discount at the town's craftsmen. */
   public static boolean member(ServerPlayer player, FactionWorldData.Settlement town) {
      for (int id : player.getPersistentData().getIntArray(MEMBER)) {
         if (id == town.id()) {
            return true;
         }
      }

      return false;
   }

   /** The buttons under a councillor's or guild master's words. Returns false when the action is not theirs. */
   public static boolean act(ServerPlayer player, FactionNpcEntity npc, String action) {
      ServerLevel level = player.serverLevel();
      FactionWorldData.Settlement town = townOf(level, npc);
      if (town == null || npc.getPersistentData().getString(COUNCIL_TAG).isEmpty()) {
         return false;
      }

      CouncilHalls councils = get(level);
      FactionStandingData standings = FactionStandingData.get(level);
      switch (action) {
         case "caravan_hire" -> {
            FactionCaravans.hire(player, town);
            return true;
         }
         case "guild_join" -> {
            if (member(player, town)) {
               return true;
            }

            if (!Coins.take(player, GUILD_FEE)) {
               player.sendSystemMessage(Component.translatable("traveller.ancientwarfare.no_silver").withStyle(ChatFormatting.RED));
               return true;
            }

            int[] old = player.getPersistentData().getIntArray(MEMBER);
            int[] joined = java.util.Arrays.copyOf(old, old.length + 1);
            joined[old.length] = town.id();
            player.getPersistentData().putIntArray(MEMBER, joined);
            standings.adjustStanding(player.getUUID(), town.faction(), 3);
            town.wealth += GUILD_FEE / 2;
            FactionRumours.say(player, npc.getDisplayName(), Component.translatable("council.ancientwarfare.guild.joined"));
            return true;
         }
         case "council_meet", "council_speak" -> {
            long[] demand = councils.demands.get(town.id());
            if (demand == null) {
               return true;
            }

            Demand wish = Demand.values()[(int)demand[0]];
            if (action.equals("council_meet")) {
               if (!Coins.take(player, wish.price)) {
                  player.sendSystemMessage(Component.translatable("traveller.ancientwarfare.no_silver").withStyle(ChatFormatting.RED));
                  return true;
               }

               switch (wish) {
                  case WALL -> town.plan().add(0, TownBuildings.Kind.PALISADE.key());
                  case GRAIN -> town.food = Math.max(0, town.food) + 60;
                  case FEAST -> town.order = Math.min(100, town.order + 5);
                  case ROAD -> town.wealth += 20;
                  default -> {
                  }
               }

               councils.demands.remove(town.id());
               councils.setDirty();
               town.order = Math.min(100, town.order + 6);
               standings.adjustStanding(player.getUUID(), town.faction(), 5);
               FactionWorldData.get(level).setDirty();
               FactionRumours.say(player, npc.getDisplayName(), Component.translatable("council.ancientwarfare.met"));
            } else if (level.getRandom().nextDouble() < 0.3 + standings.getStanding(player.getUUID(), town.faction()) / 300.0) {
               // Talked round: the council lets the matter rest.
               councils.demands.remove(town.id());
               councils.setDirty();
               standings.adjustStanding(player.getUUID(), town.faction(), 1);
               FactionRumours.say(player, npc.getDisplayName(), Component.translatable("council.ancientwarfare.persuaded"));
            } else {
               FactionRumours.say(player, npc.getDisplayName(), Component.translatable("council.ancientwarfare.unmoved"));
            }

            return true;
         }
         default -> {
            return false;
         }
      }
   }

   private static CouncilHalls load(CompoundTag tag) {
      CouncilHalls councils = new CouncilHalls();
      CompoundTag demands = tag.getCompound("Demands");
      for (String key : demands.getAllKeys()) {
         long[] value = demands.getLongArray(key);
         if (value.length == 2 && value[0] >= 0 && value[0] < Demand.values().length) {
            councils.demands.put(Integer.parseInt(key), value);
         }
      }

      return councils;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      CompoundTag demands = new CompoundTag();
      this.demands.forEach((id, value) -> demands.putLongArray(String.valueOf(id), value));
      tag.put("Demands", demands);
      return tag;
   }
}
