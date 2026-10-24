package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

/**
 * Towns mend what war breaks. Blocks a blast or a siege shot knocks out of a settlement are
 * remembered, and once no shot has fallen there for a while its people put them back, a few a
 * second from the ground up, wherever nothing else has since taken their place.
 */
public final class FactionRepairs extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_repairs";
   /** How long a town waits after the last blast before it starts mending. */
   private static final long QUIET_TICKS = 2400L;
   private static final long TICK_INTERVAL = 20L;
   private static final int BLOCKS_PER_TICK = 6;
   /** A blast this far outside a settlement's edge still counts as falling on it. */
   private static final int MARGIN = 8;
   /** The most blocks a settlement remembers; a town razed further is past mending by hand. */
   private static final int MAX_REMEMBERED = 20000;
   private final Map<Integer, Damage> damage = new HashMap<>();

   private static final class Damage {
      final LinkedHashMap<Long, BlockState> blocks = new LinkedHashMap<>();
      long lastBlast;
   }

   /** Blocks about to be broken by a blast or a shot: those inside a settlement are remembered. */
   public static void record(ServerLevel level, BlockPos center, List<BlockPos> positions) {
      if (positions.isEmpty() || level != level.getServer().overworld()) {
         return;
      }

      FactionWorldData.Settlement settlement = settlementAt(FactionWorldData.get(level), center);
      if (settlement == null) {
         return;
      }

      FactionRepairs repairs = get(level);
      Damage damage = repairs.damage.computeIfAbsent(settlement.id(), id -> new Damage());
      damage.lastBlast = level.getGameTime();
      for (BlockPos pos : positions) {
         BlockState state = level.getBlockState(pos);
         if (state.isAir() || !state.getFluidState().isEmpty() || state.is(Blocks.FIRE) || state.hasBlockEntity() || damage.blocks.size() >= MAX_REMEMBERED) {
            continue;
         }

         // The first state is the one to restore: a block knocked out twice keeps its original.
         damage.blocks.putIfAbsent(pos.asLong(), state);
      }

      repairs.setDirty();
   }

   private static FactionWorldData.Settlement settlementAt(FactionWorldData data, BlockPos pos) {
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.built() && settlement.contains(pos.getX(), pos.getZ(), MARGIN)) {
            return settlement;
         }
      }

      return null;
   }

   static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % TICK_INTERVAL != 0L) {
         return;
      }

      FactionRepairs repairs = get(level);
      if (repairs.damage.isEmpty()) {
         return;
      }

      Iterator<Map.Entry<Integer, Damage>> entries = repairs.damage.entrySet().iterator();
      while (entries.hasNext()) {
         Map.Entry<Integer, Damage> entry = entries.next();
         Damage damage = entry.getValue();
         if (damage.blocks.isEmpty() || data.settlement(entry.getKey()) == null) {
            entries.remove();
            repairs.setDirty();
            continue;
         }

         if (level.getGameTime() - damage.lastBlast < QUIET_TICKS) {
            continue;
         }

         mend(level, damage);
         repairs.setDirty();
         if (damage.blocks.isEmpty()) {
            AncientWarfareCore.LOG.info("Settlement #{} has mended its war damage", entry.getKey());
            entries.remove();
         }
      }
   }

   /** Puts back a few blocks, the lowest first so that walls stand before their roofs. */
   private static void mend(ServerLevel level, Damage damage) {
      List<Map.Entry<Long, BlockState>> lowest = new ArrayList<>(damage.blocks.entrySet());
      lowest.sort(Comparator.comparingInt(entry -> BlockPos.getY(entry.getKey())));
      int mended = 0;
      for (Map.Entry<Long, BlockState> entry : lowest) {
         if (mended >= BLOCKS_PER_TICK) {
            break;
         }

         BlockPos pos = BlockPos.of(entry.getKey());
         if (!level.isLoaded(pos)) {
            continue;
         }

         BlockState now = level.getBlockState(pos);
         damage.blocks.remove(entry.getKey());
         // Somebody built here since, or the hole filled with something solid: leave it be.
         if (!now.isAir() && !now.is(Blocks.FIRE) && !now.canBeReplaced()) {
            continue;
         }

         BlockState state = entry.getValue();
         if (!state.canSurvive(level, pos)) {
            // Its support is not back yet; try again on a later round.
            damage.blocks.put(entry.getKey(), state);
            continue;
         }

         level.setBlock(pos, state, 3);
         if (mended == 0) {
            level.playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.6F, 0.9F);
         }

         mended++;
      }

      if (mended == 0 && !damage.blocks.isEmpty()) {
         // Nothing could be placed this round (unloaded, or everything left waits for support):
         // drop what can never stand so the queue does not wait on it for ever.
         damage.blocks.entrySet().removeIf(entry -> level.isLoaded(BlockPos.of(entry.getKey())) && !entry.getValue().canSurvive(level, BlockPos.of(entry.getKey())));
      }
   }

   public static FactionRepairs get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(FactionRepairs::load, FactionRepairs::new, DATA_NAME);
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();
      for (Map.Entry<Integer, Damage> entry : this.damage.entrySet()) {
         CompoundTag value = new CompoundTag();
         value.putInt("Settlement", entry.getKey());
         value.putLong("LastBlast", entry.getValue().lastBlast);
         ListTag blocks = new ListTag();
         for (Map.Entry<Long, BlockState> block : entry.getValue().blocks.entrySet()) {
            CompoundTag one = new CompoundTag();
            one.putLong("Pos", block.getKey());
            one.put("State", NbtUtils.writeBlockState(block.getValue()));
            blocks.add(one);
         }

         value.put("Blocks", blocks);
         list.add(value);
      }

      tag.put("Damage", list);
      return tag;
   }

   private static FactionRepairs load(CompoundTag tag) {
      FactionRepairs repairs = new FactionRepairs();
      for (Tag raw : tag.getList("Damage", Tag.TAG_COMPOUND)) {
         CompoundTag value = (CompoundTag)raw;
         Damage damage = new Damage();
         damage.lastBlast = value.getLong("LastBlast");
         for (Tag rawBlock : value.getList("Blocks", Tag.TAG_COMPOUND)) {
            CompoundTag one = (CompoundTag)rawBlock;
            damage.blocks.put(
               one.getLong("Pos"), NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(), one.getCompound("State"))
            );
         }

         repairs.damage.put(value.getInt("Settlement"), damage);
      }

      return repairs;
   }
}
