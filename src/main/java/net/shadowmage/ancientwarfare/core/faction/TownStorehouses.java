package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;

/**
 * The realm's stores are real goods: a town's warehouse holds a barrel for each of them (a granary
 * one of grain), filled with what the realm has in store. What a player takes out comes off the
 * realm's stores (and, taken from anyone but friends, sours them on the player); what a player puts
 * in is the realm's to use.
 */
public final class TownStorehouses {
   private static final long CHECK_TICKS = 100L;
   private static final int NEAR = 40;
   private static final int BARREL_SLOTS = 27;
   /** Taken without leave: this many items cost one point of standing. */
   private static final int THEFT_PER_STANDING = 16;
   private static final Item[] GOODS = {Items.OAK_LOG, Items.COBBLESTONE, Items.IRON_INGOT, Items.WHEAT, Items.LEATHER, Items.WHITE_WOOL, Items.COAL, Items.COPPER_INGOT};
   /** What each barrel was last filled with; not saved, so a restart only refills it afresh. */
   private static final Map<Long, Integer> FILLED = new HashMap<>();

   private TownStorehouses() {
   }

   static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % CHECK_TICKS != 0L || level.players().isEmpty()) {
         return;
      }

      for (FactionWorldData.Settlement town : data.settlements()) {
         if (FactionDiplomacy.isOutlaw(town.faction())) {
            continue;
         }

         for (FactionWorldData.Hall hall : town.halls()) {
            boolean warehouse = hall.kind().equals(TownBuildings.Kind.WAREHOUSE.key());
            boolean granary = hall.kind().equals(TownBuildings.Kind.GRANARY.key());
            if ((warehouse || granary) && level.hasChunk(hall.min().getX() >> 4, hall.min().getZ() >> 4) && level.hasChunk(hall.max().getX() >> 4, hall.max().getZ() >> 4)
               && level.hasNearbyAlivePlayer(hall.min().getX(), hall.min().getY(), hall.min().getZ(), NEAR)) {
               keep(level, data, town, hall, granary);
            }
         }
      }
   }

   private static void keep(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement town, FactionWorldData.Hall hall, boolean granary) {
      int[] store = data.stores(town.faction());
      int width = Math.max(1, hall.max().getX() - hall.min().getX() - 1);
      for (int kind = 0; kind < GOODS.length; kind++) {
         if (granary && kind != FactionResources.GRAIN) {
            continue;
         }

         int slot = granary ? 0 : kind;
         BlockPos at = floor(level, hall, hall.min().getX() + 1 + slot % width, hall.min().getZ() + 1);
         if (at == null) {
            continue;
         }

         if (!(level.getBlockEntity(at) instanceof BarrelBlockEntity)) {
            if (!level.getBlockState(at).isAir()) {
               continue;
            }

            level.setBlockAndUpdate(at, Blocks.BARREL.defaultBlockState());
            FILLED.remove(at.asLong());
         }

         if (!(level.getBlockEntity(at) instanceof BarrelBlockEntity barrel)) {
            continue;
         }

         Item item = GOODS[kind];
         int counted = 0;
         for (int i = 0; i < barrel.getContainerSize(); i++) {
            ItemStack stack = barrel.getItem(i);
            if (stack.is(item)) {
               counted += stack.getCount();
            }
         }

         Integer last = FILLED.get(at.asLong());
         if (last != null && counted != last) {
            int change = counted - last;
            store[kind] = Math.max(0, store[kind] + change);
            data.setDirty();
            if (change < 0) {
               stolen(level, town, at, -change, kind);
            }
         }

         int wanted = Math.min(store[kind], BARREL_SLOTS * item.getMaxStackSize());
         barrel.clearContent();
         int left = wanted;
         for (int i = 0; i < barrel.getContainerSize() && left > 0; i++) {
            int count = Math.min(left, item.getMaxStackSize());
            barrel.setItem(i, new ItemStack(item, count));
            left -= count;
         }

         barrel.setCustomName(Component.translatable("storehouse.ancientwarfare.barrel", Component.translatable("trade.ancientwarfare.good." + FactionResources.NAMES[kind]), store[kind]));
         barrel.setChanged();
         FILLED.put(at.asLong(), wanted);
      }
   }

   /** The first free spot on the floor inside the hall at x, z (a barrel already there counts), or null. */
   private static BlockPos floor(ServerLevel level, FactionWorldData.Hall hall, int x, int z) {
      for (int y = Math.max(level.getMinBuildHeight() + 1, hall.min().getY()); y < hall.max().getY(); y++) {
         BlockPos at = new BlockPos(x, y, z);
         if (level.getBlockEntity(at) instanceof BarrelBlockEntity) {
            return at;
         }

         if (level.getBlockState(at).isAir() && level.getBlockState(at.above()).isAir() && !level.getBlockState(at.below()).getCollisionShape(level, at.below()).isEmpty()) {
            return at;
         }
      }

      return null;
   }

   /** Goods gone from a barrel: the nearest player took them; friends may, anyone else is a thief. */
   private static void stolen(ServerLevel level, FactionWorldData.Settlement town, BlockPos at, int amount, int kind) {
      ServerPlayer taker = (ServerPlayer)level.getNearestPlayer(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 12.0, false);
      if (taker == null) {
         return;
      }

      FactionStandingData standings = FactionStandingData.get(level);
      RelationState treaty = standings.getTreaty(taker.getUUID(), town.faction());
      if (treaty == RelationState.ALLIANCE) {
         return;
      }

      int loss = Math.max(1, amount / THEFT_PER_STANDING);
      standings.adjustStanding(taker.getUUID(), town.faction(), -loss);
      taker.sendSystemMessage(Component.translatable("storehouse.ancientwarfare.theft", FactionNews.factionName(town.faction()), loss).withStyle(ChatFormatting.RED));
   }
}
