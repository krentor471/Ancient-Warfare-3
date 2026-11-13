package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;

/**
 * What a town happening does in the world, not only on paper: a fire really burns on a roof until
 * the player helps put it out, wolves really come down on the town at night.
 */
final class TownHappenings {
   /** Fires lit by a happening, per player, so a helping hand can put them out. */
   private static final Map<UUID, List<BlockPos>> FIRES = new HashMap<>();
   /** Wolves sent by a happening, per player. */
   private static final Map<UUID, List<UUID>> WOLVES = new HashMap<>();

   private TownHappenings() {
   }

   static void begin(ServerPlayer player, FactionWorldData.Settlement town, String key) {
      ServerLevel level = player.serverLevel();
      RandomSource random = level.getRandom();
      if (key.equals("fire")) {
         List<BlockPos> lit = new ArrayList<>();
         BlockPos origin = player.blockPosition();
         for (int attempt = 0; attempt < 200 && lit.size() < 4; attempt++) {
            int x = origin.getX() + random.nextInt(41) - 20;
            int z = origin.getZ() + random.nextInt(41) - 20;
            if (!town.contains(x, z, 0) || Math.abs(x - origin.getX()) < 6 && Math.abs(z - origin.getZ()) < 6) {
               continue;
            }

            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            // Only on a roof of something that burns: thatch, planks, logs, wool.
            if (level.isEmptyBlock(top) && (level.getBlockState(top.below()).is(BlockTags.PLANKS) || level.getBlockState(top.below()).is(BlockTags.LOGS)
               || level.getBlockState(top.below()).is(Blocks.HAY_BLOCK) || level.getBlockState(top.below()).is(BlockTags.WOOL)
               || level.getBlockState(top.below()).is(BlockTags.WOODEN_STAIRS) || level.getBlockState(top.below()).is(BlockTags.WOODEN_SLABS))) {
               level.setBlockAndUpdate(top, Blocks.FIRE.defaultBlockState());
               lit.add(top);
            }
         }

         FIRES.put(player.getUUID(), lit);
      } else if (key.equals("wolves")) {
         List<UUID> pack = new ArrayList<>();
         double angle = random.nextDouble() * Math.PI * 2.0;
         int x = (int)(player.getX() + Math.cos(angle) * 22.0);
         int z = (int)(player.getZ() + Math.sin(angle) * 22.0);
         BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
         List<FactionNpcEntity> prey = level.getEntitiesOfClass(FactionNpcEntity.class, player.getBoundingBox().inflate(32.0),
            npc -> npc.isAlive() && FactionSimulation.isCivilian(npc));
         for (int i = 0; i < 3; i++) {
            Wolf wolf = EntityType.WOLF.spawn(level, ground.offset(i, 0, 0), MobSpawnType.EVENT);
            if (wolf != null) {
               if (!prey.isEmpty()) {
                  wolf.setTarget(prey.get(random.nextInt(prey.size())));
               }

               wolf.setRemainingPersistentAngerTime(1200);
               pack.add(wolf.getUUID());
            }
         }

         WOLVES.put(player.getUUID(), pack);
      }
   }

   static void resolve(ServerPlayer player, FactionWorldData.Settlement town, String key, String choice, boolean ok) {
      ServerLevel level = player.serverLevel();
      lasting(player, town, key + "." + choice, ok);
      if (key.equals("fire")) {
         List<BlockPos> lit = FIRES.remove(player.getUUID());
         if (lit != null && choice.equals("help") && ok) {
            // The bucket chain the player joined puts the roofs out.
            for (BlockPos pos : lit) {
               if (level.getBlockState(pos).is(Blocks.FIRE)) {
                  level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
               }
            }
         }
      } else if (key.equals("wolves")) {
         List<UUID> pack = WOLVES.remove(player.getUUID());
         if (pack != null && choice.equals("hunt")) {
            for (UUID id : pack) {
               if (level.getEntity(id) instanceof Wolf wolf && wolf.isAlive()) {
                  // The pack turns on the hunter.
                  wolf.setTarget(player);
                  wolf.setPersistentAngerTarget(player.getUUID());
                  wolf.setRemainingPersistentAngerTime(1200);
               }
            }
         }
      }
   }

   /**
    * What stays with the town after the player has chosen: bread given feeds people, a cure shortens
    * the plague, a sponsored apprentice opens a stall, a bribe taken makes the player wanted.
    */
   private static void lasting(ServerPlayer player, FactionWorldData.Settlement town, String what, boolean ok) {
      switch (what) {
         case "hungry_family.give_bread" -> town.food += 4;
         case "baker.sell_wheat" -> town.food += 16;
         case "cold_night.firewood" -> town.health = Math.min(100, town.health + 2);
         case "sick_child.milk", "sick_child.pay_healer" -> {
            if (town.plague > 0) {
               town.plague--;
            }
         }
         case "apprentice.sponsor" -> town.plan().add(0, TownBuildings.Kind.STALL.key());
         case "pilgrim.alms" -> {
            if (town.plan().stream().noneMatch(TownBuildings.Kind.SHRINE.key()::equals)) {
               town.plan().add(TownBuildings.Kind.SHRINE.key());
            }
         }
         case "guard_bribe.take_cut" -> TownWatch.wanted(player, town.faction(), 20);
         case "fire.flee" -> town.plan().add(0, TownBuildings.Kind.GRANARY.key());
         case "fire.help" -> {
            if (!ok) {
               town.plan().add(0, TownBuildings.Kind.GRANARY.key());
            }
         }
         case "thief.pardon" -> town.order = Math.max(0, town.order - 1);
         case "brawl.break_up" -> {
            if (!ok) {
               // The brawl spills over: the tavern's door and benches get smashed.
               town.wealth = Math.max(0, town.wealth - 5);
            }
         }
         case "bully.intervene" -> {
            if (!ok) {
               TownWatch.wanted(player, town.faction(), 5);
            }
         }
         default -> {
            return;
         }
      }

      FactionWorldData.get(player.serverLevel()).setDirty();
   }

   static void reset() {
      FIRES.clear();
      WOLVES.clear();
   }
}
