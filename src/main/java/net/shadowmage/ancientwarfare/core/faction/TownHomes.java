package net.shadowmage.ancientwarfare.core.faction;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;

/**
 * A house of one's own in a town. The village head sells a player a house, which the town builds
 * at its edge; the player can have it furnished (a bed, chests, a table, shelves, carpets, a
 * lantern) and hire a servant, who keeps the house and has a meal ready once a day.
 */
public final class TownHomes {
   private static final String HOMES = "AWHomes";
   private static final String SERVANT = "AWServant";
   private static final String MEAL_DAY = "AWMealDay";
   public static final int HOUSE_PRICE = 200;
   /** Standing with the realm a buyer needs. */
   public static final int HOUSE_STANDING = 15;
   public static final int FURNISH_PRICE = 40;
   public static final int SERVANT_PRICE = 30;

   private TownHomes() {
   }

   /** The player's house in this town: {minX, minY, minZ, maxX, maxY, maxZ}, or null. */
   static int[] home(ServerPlayer player, FactionWorldData.Settlement town) {
      CompoundTag homes = player.getPersistentData().getCompound(HOMES);
      String key = String.valueOf(town.id());
      return homes.contains(key) ? homes.getIntArray(key) : null;
   }

   /** The head's offer: buy a house here, or see to the one the player has. */
   static void offer(ServerPlayer player, FactionNpcEntity head, FactionWorldData.Settlement town, java.util.function.BiFunction<String, String, Component> button) {
      int[] home = home(player, town);
      net.minecraft.network.chat.MutableComponent line = Component.literal("  ");
      if (home == null) {
         line.append(button.apply("home_buy", "head.ancientwarfare.home.buy"));
      } else {
         line.append(button.apply("home_furnish", "head.ancientwarfare.home.furnish")).append("  ").append(button.apply("home_servant", "head.ancientwarfare.home.servant"));
      }

      line.append("  ").append(button.apply("chronicle", "head.ancientwarfare.chronicle"));
      player.sendSystemMessage(line);
   }

   static boolean act(ServerPlayer player, FactionNpcEntity npc, FactionWorldData.Settlement town, String action) {
      ServerLevel level = player.serverLevel();
      switch (action) {
         case "home_buy" -> {
            if (home(player, town) != null) {
               return true;
            }

            if (FactionStandingData.get(level).getStanding(player.getUUID(), town.faction()) < HOUSE_STANDING) {
               FactionRumours.say(player, npc.getDisplayName(), Component.translatable("head.ancientwarfare.home.not_trusted", HOUSE_STANDING));
               return true;
            }

            if (!Coins.take(player, HOUSE_PRICE)) {
               player.sendSystemMessage(Component.translatable("traveller.ancientwarfare.no_silver").withStyle(ChatFormatting.RED));
               return true;
            }

            FactionWorldData data = FactionWorldData.get(level);
            FactionWorldData.Hall hall = FactionGrowth.buildHome(level, data, town);
            if (hall == null) {
               Coins.give(player, HOUSE_PRICE);
               FactionRumours.say(player, npc.getDisplayName(), Component.translatable("head.ancientwarfare.home.no_room"));
               return true;
            }

            CompoundTag homes = player.getPersistentData().getCompound(HOMES);
            homes.putIntArray(String.valueOf(town.id()), new int[]{hall.min().getX(), hall.min().getY(), hall.min().getZ(), hall.max().getX(), hall.max().getY(), hall.max().getZ()});
            player.getPersistentData().put(HOMES, homes);
            town.wealth += HOUSE_PRICE / 2;
            TownChronicle.write(level, town.id(), "house", player.getName().getString());
            BlockPos door = hall.min();
            player.sendSystemMessage(Component.translatable("head.ancientwarfare.home.bought", FactionNews.named(town), door.getX(), door.getY(), door.getZ()).withStyle(ChatFormatting.GREEN));
            return true;
         }
         case "home_furnish" -> {
            int[] home = home(player, town);
            if (home == null) {
               return true;
            }

            if (!Coins.take(player, FURNISH_PRICE)) {
               player.sendSystemMessage(Component.translatable("traveller.ancientwarfare.no_silver").withStyle(ChatFormatting.RED));
               return true;
            }

            furnish(level, home);
            player.sendSystemMessage(Component.translatable("head.ancientwarfare.home.furnished").withStyle(ChatFormatting.GREEN));
            return true;
         }
         case "home_servant" -> {
            int[] home = home(player, town);
            if (home == null) {
               return true;
            }

            AABB box = new AABB(home[0], home[1], home[2], home[3] + 1, home[4] + 1, home[5] + 1);
            if (!level.getEntitiesOfClass(FactionNpcEntity.class, box.inflate(4), servant -> player.getStringUUID().equals(servant.getPersistentData().getString(SERVANT))).isEmpty()) {
               FactionRumours.say(player, npc.getDisplayName(), Component.translatable("head.ancientwarfare.home.has_servant"));
               return true;
            }

            if (!Coins.take(player, SERVANT_PRICE)) {
               player.sendSystemMessage(Component.translatable("traveller.ancientwarfare.no_silver").withStyle(ChatFormatting.RED));
               return true;
            }

            FactionNpcEntity servant = AWCoreEntityTypes.FACTION_CIVILIAN_FEMALE_NPC.get().create(level);
            if (servant == null) {
               return true;
            }

            BlockPos inside = new BlockPos((home[0] + home[3]) / 2, home[1] + 1, (home[2] + home[5]) / 2);
            servant.moveTo(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5, 0.0F, 0.0F);
            servant.finalizeSpawn(level, level.getCurrentDifficultyAt(inside), MobSpawnType.EVENT, null, null);
            servant.setFactionName(town.faction());
            servant.setPersistenceRequired();
            servant.getPersistentData().putString(SERVANT, player.getStringUUID());
            servant.setCustomName(Component.translatable("head.ancientwarfare.home.servant_name", player.getName()));
            servant.restrictTo(inside, 4);
            level.addFreshEntity(servant);
            player.sendSystemMessage(Component.translatable("head.ancientwarfare.home.servant_hired").withStyle(ChatFormatting.GREEN));
            return true;
         }
         default -> {
            return false;
         }
      }
   }

   private static void put(ServerLevel level, BlockPos pos, BlockState state) {
      if (level.getBlockState(pos).isAir() && !level.getBlockState(pos.below()).isAir()) {
         level.setBlockAndUpdate(pos, state);
      }
   }

   /** A bed, chests, a table, shelves, carpets and a lantern, wherever the floor is free. */
   private static void furnish(ServerLevel level, int[] home) {
      int y = home[1] + 1;
      int x0 = home[0] + 1;
      int z0 = home[2] + 1;
      int x1 = home[3] - 1;
      int z1 = home[5] - 1;
      BlockPos foot = new BlockPos(x0, y, z0 + 1);
      BlockPos head = foot.north();
      if (level.getBlockState(foot).isAir() && level.getBlockState(head).isAir()) {
         BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, net.minecraft.core.Direction.NORTH);
         level.setBlock(head, bed.setValue(BedBlock.PART, BedPart.HEAD), Block.UPDATE_ALL);
         level.setBlock(foot, bed.setValue(BedBlock.PART, BedPart.FOOT), Block.UPDATE_ALL);
      }

      put(level, new BlockPos(x1, y, z0), Blocks.CHEST.defaultBlockState());
      put(level, new BlockPos(x1 - 1, y, z0), Blocks.BARREL.defaultBlockState());
      put(level, new BlockPos(x1, y, z1), Blocks.CRAFTING_TABLE.defaultBlockState());
      put(level, new BlockPos(x1 - 1, y, z1), Blocks.FURNACE.defaultBlockState());
      put(level, new BlockPos(x0, y, z1), Blocks.BOOKSHELF.defaultBlockState());
      put(level, new BlockPos(x0 + 1, y, z1), Blocks.BOOKSHELF.defaultBlockState());
      int mx = (x0 + x1) / 2;
      int mz = (z0 + z1) / 2;
      put(level, new BlockPos(mx, y, mz), Blocks.SPRUCE_FENCE.defaultBlockState());
      put(level, new BlockPos(mx, y + 1, mz), Blocks.POTTED_POPPY.defaultBlockState());
      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            if (Math.abs(x - mx) + Math.abs(z - mz) == 1) {
               put(level, new BlockPos(x, y, z), Blocks.RED_CARPET.defaultBlockState());
            }
         }
      }
   }

   /** The servant has a meal ready once a day. Returns true when the townsperson was a servant. */
   public static boolean talk(ServerPlayer player, FactionNpcEntity npc) {
      String master = npc.getPersistentData().getString(SERVANT);
      if (master.isEmpty()) {
         return false;
      }

      if (!master.equals(player.getStringUUID())) {
         FactionRumours.say(player, npc.getDisplayName(), Component.translatable("head.ancientwarfare.home.servant_other"));
         return true;
      }

      long day = player.serverLevel().getDayTime() / FactionSimulation.DAY_TICKS + 1;
      if (npc.getPersistentData().getLong(MEAL_DAY) == day) {
         FactionRumours.say(player, npc.getDisplayName(), Component.translatable("head.ancientwarfare.home.servant_done"));
         return true;
      }

      npc.getPersistentData().putLong(MEAL_DAY, day);
      for (ItemStack meal : List.of(new ItemStack(Items.BREAD, 3), new ItemStack(Items.COOKED_BEEF, 2), new ItemStack(Items.APPLE, 2))) {
         player.getInventory().placeItemBackInInventory(meal);
      }

      FactionRumours.say(player, npc.getDisplayName(), Component.translatable("head.ancientwarfare.home.servant_meal"));
      return true;
   }
}
