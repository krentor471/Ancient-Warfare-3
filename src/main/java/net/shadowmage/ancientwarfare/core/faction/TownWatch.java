package net.shadowmage.ancientwarfare.core.faction;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;

/**
 * The night watch, the jail and the court. A theft someone saw makes the player wanted in that
 * realm, with a fine to pay. At night watchmen with lanterns walk the streets of towns where a
 * player is; a watchman who catches a wanted player takes him to the town's cell. There the court
 * sits: pay the fine and walk free, or serve the time. A prisoner who breaks out is wanted twice
 * over.
 */
public final class TownWatch {
   public static final String WATCH_TAG = "AWWatch";
   private static final String WANTED = "AWWanted";
   private static final String JAIL = "AWJail";
   private static final int WATCHMEN = 2;
   private static final int NEAR = 64;
   private static final double CATCH = 6.0;
   private static final long SENTENCE_TICKS = 2400L;
   private static final int ESCAPE_STANDING = -5;
   private static final long CHECK_TICKS = 40L;

   private TownWatch() {
   }

   /** The witness saw a theft: the player is wanted with a fine. */
   public static void wanted(ServerPlayer player, String faction, int fine) {
      CompoundTag wanted = player.getPersistentData().getCompound(WANTED);
      wanted.putInt(faction, wanted.getInt(faction) + fine);
      player.getPersistentData().put(WANTED, wanted);
      player.sendSystemMessage(Component.translatable("watch.ancientwarfare.wanted", FactionNews.factionName(faction), wanted.getInt(faction)).withStyle(ChatFormatting.RED));
   }

   private static boolean night(ServerLevel level) {
      long time = level.getDayTime() % FactionSimulation.DAY_TICKS;
      return time > 13000L && time < 23000L;
   }

   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % CHECK_TICKS != 0L) {
         return;
      }

      for (ServerPlayer player : level.players()) {
         CompoundTag jail = player.getPersistentData().getCompound(JAIL);
         if (!jail.isEmpty()) {
            prisoner(level, player, jail);
            continue;
         }

         BlockPos at = player.blockPosition();
         FactionWorldData.Settlement town = FactionCrime.settlementAt(level, at);
         List<FactionNpcEntity> watch = level.getEntitiesOfClass(FactionNpcEntity.class, player.getBoundingBox().inflate(NEAR), npc -> npc.getTags().contains(WATCH_TAG));
         if (!night(level) || town == null || !FactionTownLife.isTown(town)) {
            continue;
         }

         if (watch.size() < WATCHMEN) {
            watchman(level, town, at);
         }

         int fine = player.getPersistentData().getCompound(WANTED).getInt(town.faction());
         if (fine > 0 && !player.isCreative()) {
            for (FactionNpcEntity man : watch) {
               if (man.getFactionName().equals(town.faction()) && man.distanceTo(player) < CATCH && man.hasLineOfSight(player)) {
                  arrest(level, player, town, man, fine);
                  break;
               }
            }
         }
      }

      // At dawn the watch goes home.
      if (!night(level)) {
         for (ServerPlayer player : level.players()) {
            level.getEntitiesOfClass(FactionNpcEntity.class, player.getBoundingBox().inflate(NEAR * 2), npc -> npc.getTags().contains(WATCH_TAG)).forEach(FactionNpcEntity::discard);
         }
      }
   }

   private static void watchman(ServerLevel level, FactionWorldData.Settlement town, BlockPos near) {
      FactionNpcEntity man = AWCoreEntityTypes.FACTION_SOLDIER_NPC.get().create(level);
      if (man == null) {
         return;
      }

      int x = near.getX() + level.getRandom().nextInt(41) - 20;
      int z = near.getZ() + level.getRandom().nextInt(41) - 20;
      BlockPos at = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
      if (!town.contains(x, z, 0) || at.distSqr(near) < 100) {
         return;
      }

      man.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
      man.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
      man.setFactionName(town.faction());
      man.addTag(WATCH_TAG);
      man.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.LANTERN));
      man.setCustomName(Component.translatable("watch.ancientwarfare.watchman"));
      man.restrictTo(town.center(), 48);
      level.addFreshEntity(man);
   }

   /** The cell beside the town square: stone, bars and a roof; returns the floor inside. */
   private static BlockPos cell(ServerLevel level, FactionWorldData.Settlement town) {
      int x = town.center().getX() + 6;
      int z = town.center().getZ() + 6;
      int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      BlockPos inside = new BlockPos(x, y, z);
      for (int dx = -1; dx <= 1; dx++) {
         for (int dz = -1; dz <= 1; dz++) {
            level.setBlockAndUpdate(inside.offset(dx, -1, dz), Blocks.STONE_BRICKS.defaultBlockState());
            level.setBlockAndUpdate(inside.offset(dx, 3, dz), Blocks.STONE_BRICKS.defaultBlockState());
            for (int dy = 0; dy < 3; dy++) {
               level.setBlockAndUpdate(inside.offset(dx, dy, dz), dx == 0 && dz == 0 ? Blocks.AIR.defaultBlockState()
                  : dy == 1 ? Blocks.IRON_BARS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState());
            }
         }
      }

      level.setBlockAndUpdate(inside.above(2), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
      return inside;
   }

   private static void arrest(ServerLevel level, ServerPlayer player, FactionWorldData.Settlement town, FactionNpcEntity man, int fine) {
      BlockPos inside = cell(level, town);
      player.stopRiding();
      player.teleportTo(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);
      CompoundTag jail = new CompoundTag();
      jail.putIntArray("Pos", new int[]{inside.getX(), inside.getY(), inside.getZ()});
      jail.putString("Faction", town.faction());
      jail.putInt("Town", town.id());
      jail.putInt("Fine", fine);
      jail.putLong("Release", level.getGameTime() + SENTENCE_TICKS);
      player.getPersistentData().put(JAIL, jail);
      FactionRumours.say(player, man.getDisplayName(), Component.translatable("watch.ancientwarfare.arrest"));
      TownChronicle.write(level, town.id(), "arrest", player.getName().getString());
      court(player, fine);
   }

   private static MutableComponent button(String key, String command, ChatFormatting color, Object... args) {
      Component label = Component.translatable(key, args);
      return Component.literal("[").append(label).append("]")
         .withStyle(style -> style.withColor(color).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, label)));
   }

   private static void court(ServerPlayer player, int fine) {
      player.sendSystemMessage(Component.translatable("watch.ancientwarfare.court", fine, SENTENCE_TICKS / 1200).withStyle(ChatFormatting.GOLD));
      player.sendSystemMessage(Component.literal("  ")
         .append(button("watch.ancientwarfare.pay", "/awtownsfolk 0 court_pay", ChatFormatting.GREEN, fine))
         .append("  ")
         .append(button("watch.ancientwarfare.serve", "/awtownsfolk 0 court_serve", ChatFormatting.GRAY)));
   }

   private static void release(ServerPlayer player, CompoundTag jail) {
      int[] pos = jail.getIntArray("Pos");
      player.getPersistentData().remove(JAIL);
      CompoundTag wanted = player.getPersistentData().getCompound(WANTED);
      wanted.remove(jail.getString("Faction"));
      player.getPersistentData().put(WANTED, wanted);
      if (pos.length == 3) {
         player.teleportTo(pos[0] + 0.5, pos[1], pos[2] - 2.5);
      }

      player.sendSystemMessage(Component.translatable("watch.ancientwarfare.free").withStyle(ChatFormatting.GREEN));
   }

   private static void prisoner(ServerLevel level, ServerPlayer player, CompoundTag jail) {
      int[] pos = jail.getIntArray("Pos");
      if (pos.length != 3) {
         player.getPersistentData().remove(JAIL);
         return;
      }

      if (level.getGameTime() >= jail.getLong("Release")) {
         release(player, jail);
      } else if (player.distanceToSqr(pos[0] + 0.5, pos[1], pos[2] + 0.5) > 9.0) {
         // He broke out.
         player.getPersistentData().remove(JAIL);
         String faction = jail.getString("Faction");
         net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData.get(level).adjustStanding(player.getUUID(), faction, ESCAPE_STANDING);
         wanted(player, faction, jail.getInt("Fine"));
         TownChronicle.write(level, jail.getInt("Town"), "escape", player.getName().getString());
      }
   }

   /** A click under the court's sentence. */
   public static void act(ServerPlayer player, String action) {
      CompoundTag jail = player.getPersistentData().getCompound(JAIL);
      if (jail.isEmpty()) {
         return;
      }

      if (action.equals("court_pay")) {
         if (!Coins.take(player, jail.getInt("Fine"))) {
            player.sendSystemMessage(Component.translatable("traveller.ancientwarfare.no_silver").withStyle(ChatFormatting.RED));
            return;
         }

         FactionWorldData.Settlement town = FactionWorldData.get(player.serverLevel()).settlement(jail.getInt("Town"));
         if (town != null) {
            town.wealth += jail.getInt("Fine");
         }

         release(player, jail);
      } else {
         player.sendSystemMessage(Component.translatable("watch.ancientwarfare.serving", Math.max(1L, (jail.getLong("Release") - player.serverLevel().getGameTime()) / 20L)).withStyle(ChatFormatting.GRAY));
      }
   }
}
