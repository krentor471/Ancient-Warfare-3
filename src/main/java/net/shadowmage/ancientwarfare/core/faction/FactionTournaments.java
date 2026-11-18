package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;

/**
 * Tournaments in a capital: for an entry fee, one of the player's soldiers fights the faction's
 * champion until one of them yields. A win brings emeralds, standing with the faction and
 * experience for the soldier; nobody dies.
 */
public final class FactionTournaments {
   public static final String CHAMPION_TAG = "aw_champion";
   public static final int ENTRY_EMERALDS = 5;
   /** Enchantment levels a champion's prize weapon or armour is worked with. */
   private static final int PRIZE_ENCHANT_LEVEL = 20;
   private static final int WIN_STANDING = 10;
   private static final int LOSS_STANDING = 2;
   private static final int WIN_EXPERIENCE = 20;
   private static final double FIGHTER_RANGE = 16.0;
   private static final double CHAMPION_DISTANCE = 6.0;
   /** A fighter yields when a blow would bring him below this share of his health. */
   private static final float YIELD_HEALTH = 0.25F;
   private static final long DUEL_TICKS = 1800L;
   /** Bouts under way, by the champion's UUID; not saved, a champion left over from a restart goes. */
   private static final Map<UUID, Duel> DUELS = new HashMap<>();

   private static final class Duel {
      final UUID player;
      final String faction;
      final LivingEntity fighter;
      final FactionNpcEntity champion;
      final long started;

      Duel(UUID player, String faction, LivingEntity fighter, FactionNpcEntity champion, long started) {
         this.player = player;
         this.faction = faction;
         this.fighter = fighter;
         this.champion = champion;
         this.started = started;
      }
   }

   private FactionTournaments() {
   }

   /** The player enters a soldier of his near the leader in the lists against the champion. */
   static Component enter(ServerPlayer player, String faction) {
      ServerLevel level = player.serverLevel();
      Component name = FactionNews.factionName(faction);
      FactionWorldData.Settlement capital = FactionSimulation.settlementAt(FactionWorldData.get(level), faction, player.getX(), player.getZ(), 16);
      if (capital == null || !"capital".equals(capital.kind())) {
         return Component.translatable("tournament.ancientwarfare.no_capital", name);
      } else if (FactionStandingData.get(level).isHostile(player.getUUID(), faction)) {
         return Component.translatable("tournament.ancientwarfare.hostile", name);
      }

      for (Duel duel : DUELS.values()) {
         if (duel.player.equals(player.getUUID())) {
            return Component.translatable("tournament.ancientwarfare.busy");
         }
      }

      CombatNpcEntity fighter = level.getEntitiesOfClass(
            CombatNpcEntity.class,
            player.getBoundingBox().inflate(FIGHTER_RANGE),
            npc -> npc.isAlive() && npc.getOwner().map(player.getUUID()::equals).orElse(false) && !npc.isRouting() && npc.getVehicle() == null
         )
         .stream()
         .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
         .orElse(null);
      if (fighter == null) {
         return Component.translatable("tournament.ancientwarfare.no_fighter");
      } else if (!PlayerDiplomacy.takeEmeralds(player, ENTRY_EMERALDS)) {
         return Component.translatable("tournament.ancientwarfare.missing", ENTRY_EMERALDS);
      }

      FactionNpcEntity champion = AWCoreEntityTypes.FACTION_SOLDIER_ELITE_NPC.get().create(level);
      if (champion == null) {
         return Component.translatable("tournament.ancientwarfare.busy");
      }

      // The champion steps out facing the fighter, from the player's side.
      Vec3 away = fighter.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
      away = away.lengthSqr() < 0.01 ? player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize() : away.normalize();
      int x = Mth.floor(fighter.getX() + away.x * CHAMPION_DISTANCE);
      int z = Mth.floor(fighter.getZ() + away.z * CHAMPION_DISTANCE);
      int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      champion.moveTo(x + 0.5, y, z + 0.5, 0.0F, 0.0F);
      champion.finalizeSpawn(level, level.getCurrentDifficultyAt(new BlockPos(x, y, z)), MobSpawnType.EVENT, null, null);
      champion.setFactionName(faction);
      champion.addTag(CHAMPION_TAG);
      champion.setCustomName(Component.translatable("tournament.ancientwarfare.champion", name));
      // Entered before he joins the world, or he would be taken for a leftover champion.
      DUELS.put(champion.getUUID(), new Duel(player.getUUID(), faction, fighter, champion, level.getGameTime()));
      level.addFreshEntity(champion);
      fighter.setTarget(champion);
      champion.setTarget(fighter);
      return Component.translatable("tournament.ancientwarfare.begins", fighter.getDisplayName(), name).withStyle(ChatFormatting.GOLD);
   }

   /** Every second: the two keep at each other; a bout that drags on ends in a draw. */
   public static void tick(ServerLevel level) {
      if (DUELS.isEmpty() || level.getGameTime() % 20L != 0L) {
         return;
      }

      for (Duel duel : DUELS.values().toArray(Duel[]::new)) {
         if (!duel.fighter.isAlive() || !duel.champion.isAlive()) {
            end(level, duel, duel.fighter.isAlive() ? duel.fighter : null);
         } else if (level.getGameTime() - duel.started > DUEL_TICKS) {
            end(level, duel, null);
         } else {
            if (duel.fighter instanceof net.minecraft.world.entity.Mob mob) {
               mob.setTarget(duel.champion);
            }

            duel.champion.setTarget(duel.fighter);
         }
      }
   }

   /**
    * A blow in the lists: from anyone but the opponent it is turned aside, and one that would
    * bring a fighter low makes him yield. Returns true when the damage is to be cancelled.
    */
   public static boolean onDamage(LivingEntity victim, Entity attacker, float amount) {
      Duel duel = duelOf(victim);
      if (duel == null) {
         return false;
      }

      LivingEntity opponent = victim == duel.fighter ? duel.champion : duel.fighter;
      if (attacker != opponent) {
         return true;
      } else if (victim.getHealth() - amount <= victim.getMaxHealth() * YIELD_HEALTH) {
         end((ServerLevel)victim.level(), duel, opponent == duel.fighter ? duel.fighter : null);
         return true;
      }

      return false;
   }

   private static Duel duelOf(LivingEntity entity) {
      if (DUELS.isEmpty()) {
         return null;
      }

      for (Duel duel : DUELS.values()) {
         if (duel.fighter == entity || duel.champion == entity) {
            return duel;
         }
      }

      return null;
   }

   /**
    * The champion's prize: a fine weapon or piece of armour, worked with enchantments and named for
    * the faction's tournament, or a pouch of jewels.
    */
   static ItemStack prize(RandomSource random, String faction) {
      int roll = random.nextInt(10);
      if (roll >= 8) {
         return roll == 8 ? new ItemStack(Items.DIAMOND, 2 + random.nextInt(3)) : new ItemStack(Items.EMERALD, 4 + random.nextInt(5));
      }

      Item item = switch (roll) {
         case 0 -> Items.DIAMOND_SWORD;
         case 1 -> Items.IRON_SWORD;
         case 2 -> Items.IRON_AXE;
         case 3 -> Items.BOW;
         case 4 -> Items.IRON_CHESTPLATE;
         case 5 -> Items.IRON_HELMET;
         case 6 -> Items.DIAMOND_HELMET;
         default -> Items.GOLDEN_HELMET;
      };
      ItemStack stack = EnchantmentHelper.enchantItem(random, new ItemStack(item), PRIZE_ENCHANT_LEVEL, false);
      stack.setHoverName(Component.translatable("tournament.ancientwarfare.prize", stack.getItem().getDescription(), FactionNews.factionName(faction)).withStyle(ChatFormatting.GOLD));
      return stack;
   }

   /** The bout is over: winner is the player's fighter, or null when he lost or it was a draw. */
   private static void end(ServerLevel level, Duel duel, LivingEntity winner) {
      DUELS.remove(duel.champion.getUUID());
      duel.champion.discard();
      if (duel.fighter instanceof net.minecraft.world.entity.Mob mob) {
         mob.setTarget(null);
      }
      Component faction = FactionNews.factionName(duel.faction);
      ServerPlayer player = level.getServer().getPlayerList().getPlayer(duel.player);
      FactionStandingData standings = FactionStandingData.get(level);
      boolean draw = winner == null && duel.fighter.isAlive() && level.getGameTime() - duel.started > DUEL_TICKS;
      if (winner != null) {
         standings.adjustStanding(duel.player, duel.faction, WIN_STANDING);
         if (winner instanceof CombatNpcEntity soldier) {
            soldier.onTournamentWin(WIN_EXPERIENCE);
         }
         if (player != null) {
            ItemStack prize = prize(level.getRandom(), duel.faction);
            Component prizeName = prize.getHoverName().copy().append(prize.getCount() > 1 ? " x" + prize.getCount() : "");
            player.getInventory().placeItemBackInInventory(prize);
            player.sendSystemMessage(Component.translatable("tournament.ancientwarfare.won", winner.getDisplayName(), faction, prizeName).withStyle(ChatFormatting.GREEN));
         }

         FactionNews.tell(level, Component.translatable("news.ancientwarfare.tournament_won", winner.getDisplayName(), faction));
      } else if (draw) {
         if (player != null) {
            player.sendSystemMessage(Component.translatable("tournament.ancientwarfare.draw", faction));
         }
      } else {
         standings.adjustStanding(duel.player, duel.faction, LOSS_STANDING);
         if (player != null) {
            player.sendSystemMessage(Component.translatable("tournament.ancientwarfare.lost", duel.fighter.getDisplayName(), faction).withStyle(ChatFormatting.RED));
         }
      }
   }

   /**
    * At a town's tournament: the player steps into the lists himself, or sends the nearest of his
    * soldiers, against the town's champion, by the herald.
    */
   static Component challenge(ServerPlayer player, String faction, FactionNpcEntity herald, boolean self) {
      ServerLevel level = player.serverLevel();
      for (Duel duel : DUELS.values()) {
         if (duel.player.equals(player.getUUID())) {
            return Component.translatable("tournament.ancientwarfare.busy");
         }
      }

      LivingEntity fighter = self ? player : level.getEntitiesOfClass(
            CombatNpcEntity.class, player.getBoundingBox().inflate(FIGHTER_RANGE),
            npc -> npc.isAlive() && npc.getOwner().map(player.getUUID()::equals).orElse(false) && !npc.isRouting() && npc.getVehicle() == null
         )
         .stream()
         .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
         .orElse(null);
      if (fighter == null) {
         return Component.translatable("tournament.ancientwarfare.no_fighter");
      }

      FactionNpcEntity champion = AWCoreEntityTypes.FACTION_SOLDIER_ELITE_NPC.get().create(level);
      if (champion == null) {
         return Component.translatable("tournament.ancientwarfare.busy");
      }

      Vec3 away = herald.position().subtract(fighter.position()).multiply(1.0, 0.0, 1.0);
      away = away.lengthSqr() < 0.01 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
      int x = Mth.floor(fighter.getX() + away.x * CHAMPION_DISTANCE);
      int z = Mth.floor(fighter.getZ() + away.z * CHAMPION_DISTANCE);
      int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      champion.moveTo(x + 0.5, y, z + 0.5, 0.0F, 0.0F);
      champion.finalizeSpawn(level, level.getCurrentDifficultyAt(new BlockPos(x, y, z)), MobSpawnType.EVENT, null, null);
      champion.setFactionName(faction);
      champion.addTag(CHAMPION_TAG);
      champion.setCustomName(Component.translatable("tournament.ancientwarfare.champion", FactionNews.factionName(faction)));
      DUELS.put(champion.getUUID(), new Duel(player.getUUID(), faction, fighter, champion, level.getGameTime()));
      level.addFreshEntity(champion);
      if (fighter instanceof net.minecraft.world.entity.Mob mob) {
         mob.setTarget(champion);
      }

      champion.setTarget(fighter);
      return Component.translatable("tournament.ancientwarfare.begins", fighter.getDisplayName(), FactionNews.factionName(faction)).withStyle(ChatFormatting.GOLD);
   }

   /** A champion with no bout to fight, as after a restart, does not come back. */
   public static boolean isStaleChampion(FactionNpcEntity npc) {
      return npc.getTags().contains(CHAMPION_TAG) && !DUELS.containsKey(npc.getUUID());
   }
}
