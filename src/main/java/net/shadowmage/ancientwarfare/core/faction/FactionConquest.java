package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.WorkerNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreBlocks;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;
import net.shadowmage.ancientwarfare.core.tile.TownHallBlockEntity;

/**
 * A player who stands in a hostile faction's settlement after killing its whole garrison decides its
 * fate: spare it and make it their own village that pays tax, take a ransom, plunder it, or raze it.
 */
public final class FactionConquest {
   public enum Choice {
      ANNEX,
      RANSOM,
      PLUNDER,
      RAZE;
   }

   /** What the menu shows: the settlement and what each choice brings. */
   public static record Offer(int settlement, String faction, Component name, int population, int tax, int ransom, int loot, int razeLoot) {
   }

   private static final long CHECK_TICKS = 40L;
   /** A settlement whose fate was decided is not offered again for this long. */
   private static final long DECIDED_TICKS = FactionSimulation.DAY_TICKS;
   private static final int CHOICE_MARGIN = 8;
   private static final int MAX_EMERALDS = 96;
   private static final int ANNEX_STANDING = -20;
   private static final int RANSOM_STANDING = -10;
   private static final int PLUNDER_STANDING = -30;
   private static final int RAZE_STANDING = -60;
   private static final int RAZE_OPINION = -5;
   private static final int MAX_WORKERS = 8;
   /** The settlement each player was last offered; cleared when they leave it, so coming back offers it again. */
   private static final Map<UUID, Integer> OFFERED = new HashMap<>();
   private static final Map<Integer, Long> DECIDED = new HashMap<>();

   private FactionConquest() {
   }

   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % CHECK_TICKS != 0L) {
         return;
      }

      for (ServerPlayer player : level.players()) {
         FactionWorldData.Settlement settlement = conquered(level, data, player, 0);
         if (settlement == null) {
            OFFERED.remove(player.getUUID());
         } else if (!Integer.valueOf(settlement.id()).equals(OFFERED.get(player.getUUID()))) {
            OFFERED.put(player.getUUID(), settlement.id());
            AWCoreNetwork.sendConquestOffer(player, offer(data, settlement));
         }
      }
   }

   /** The hostile, emptied settlement the player stands in, or null. */
   private static FactionWorldData.Settlement conquered(ServerLevel level, FactionWorldData data, ServerPlayer player, int margin) {
      if (player.isSpectator() || !player.isAlive()) {
         return null;
      }

      FactionStandingData standings = FactionStandingData.get(level);
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!settlement.contains(player.getX(), player.getZ(), margin)) {
            continue;
         }

         String faction = settlement.faction();
         boolean hostile = standings.getTreaty(player.getUUID(), faction) == RelationState.WAR || standings.getStanding(player.getUUID(), faction) < 0;
         Long decided = DECIDED.get(settlement.id());
         if (settlement.manned()
            && settlement.garrison() == 0
            && hostile
            && !FactionDiplomacy.isOutlaw(faction)
            && (decided == null || level.getGameTime() - decided > DECIDED_TICKS)
            && !besieged(data, settlement)
            && FactionSimulation.liveSoldiers(level, settlement) == 0) {
            return settlement;
         }
      }

      return null;
   }

   /** A faction army storming it takes it by its own rules. */
   private static boolean besieged(FactionWorldData data, FactionWorldData.Settlement settlement) {
      return data.armies().stream().anyMatch(army -> army.spawned() && army.targetSettlement() == settlement.id());
   }

   private static Offer offer(FactionWorldData data, FactionWorldData.Settlement settlement) {
      int treasury = Math.max(0, data.treasury(settlement.faction()));
      int population = settlement.population();
      return new Offer(
         settlement.id(),
         settlement.faction(),
         settlement.name().isEmpty() ? Component.empty() : settlement.nameComponent(),
         population,
         tax(settlement),
         Math.min(MAX_EMERALDS, Math.min(treasury, 8 + population / 3)),
         Math.min(MAX_EMERALDS, Math.min(treasury, 16 + population / 2)),
         Math.min(MAX_EMERALDS, Math.min(treasury, 24 + population))
      );
   }

   private static int tax(FactionWorldData.Settlement settlement) {
      return Mth.clamp(settlement.population() / 4, 2, 16);
   }

   /** The player chose in the menu. */
   public static void choose(ServerPlayer player, int settlementId, Choice choice) {
      ServerLevel level = player.serverLevel();
      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.Settlement settlement = conquered(level, data, player, CHOICE_MARGIN);
      if (settlement == null || settlement.id() != settlementId) {
         player.sendSystemMessage(Component.translatable("conquest.ancientwarfare.gone"));
         return;
      }

      Offer offer = offer(data, settlement);
      String faction = settlement.faction();
      Component factionName = FactionNews.factionName(faction);
      FactionStandingData standings = FactionStandingData.get(level);
      UUID id = player.getUUID();
      DECIDED.put(settlement.id(), level.getGameTime());
      switch (choice) {
         case ANNEX -> {
            BlockPos hall = annex(level, data, player, settlement, offer.tax());
            standings.adjustStanding(id, faction, ANNEX_STANDING);
            player.sendSystemMessage(
               Component.translatable("conquest.ancientwarfare.annexed", factionName, offer.tax(), hall.getX(), hall.getY(), hall.getZ()).withStyle(ChatFormatting.GREEN)
            );
            FactionNews.tell(
               level, Component.translatable("news.ancientwarfare.conquest_annexed", player.getDisplayName(), factionName, FactionNews.named(settlement))
            );
         }
         case RANSOM -> {
            pay(player, data, faction, offer.ransom());
            standings.adjustStanding(id, faction, RANSOM_STANDING);
            player.sendSystemMessage(Component.translatable("conquest.ancientwarfare.ransomed", factionName, offer.ransom()).withStyle(ChatFormatting.GOLD));
         }
         case PLUNDER -> {
            pay(player, data, faction, offer.loot());
            settlement.population -= settlement.population / 3;
            settlement.food = 0;
            standings.adjustStanding(id, faction, PLUNDER_STANDING);
            player.sendSystemMessage(Component.translatable("conquest.ancientwarfare.plundered", factionName, offer.loot()).withStyle(ChatFormatting.GOLD));
            FactionNews.tell(
               level, Component.translatable("news.ancientwarfare.conquest_plundered", player.getDisplayName(), factionName, FactionNews.named(settlement))
            );
         }
         case RAZE -> {
            pay(player, data, faction, offer.razeLoot());
            civilians(level, settlement).forEach(FactionNpcEntity::discard);
            data.removeSettlement(settlement);
            standings.adjustStanding(id, faction, RAZE_STANDING);
            for (String other : FactionRegistry.factionNames()) {
               // Everyone hears of it.
               if (!other.equals(faction)) {
                  standings.adjustStanding(id, other, RAZE_OPINION);
               }
            }

            player.sendSystemMessage(Component.translatable("conquest.ancientwarfare.razed", factionName, offer.razeLoot()).withStyle(ChatFormatting.RED));
            FactionNews.tell(
               level, Component.translatable("news.ancientwarfare.conquest_razed", player.getDisplayName(), factionName, FactionNews.named(settlement))
            );
         }
      }

      data.setDirty();
   }

   /** The faction's treasury pays, in emeralds into the player's pockets. */
   private static void pay(ServerPlayer player, FactionWorldData data, String faction, int emeralds) {
      data.setTreasury(faction, Math.max(0, data.treasury(faction) - emeralds));
      give(player, emeralds);
   }

   private static void give(ServerPlayer player, int emeralds) {
      Coins.give(player, emeralds);
   }

   /**
    * The settlement becomes the player's village: a town hall stands at its middle, its people go to
    * work for the new owner, and it pays a daily tax into the hall.
    */
   private static BlockPos annex(ServerLevel level, FactionWorldData data, ServerPlayer player, FactionWorldData.Settlement settlement, int tax) {
      List<FactionNpcEntity> people = civilians(level, settlement);
      data.removeSettlement(settlement);
      BlockPos center = settlement.center();
      if (Math.abs(player.getBlockX() - center.getX()) <= 1 && Math.abs(player.getBlockZ() - center.getZ()) <= 1) {
         // Not on the player's head.
         center = center.offset(3, 0, 0);
      }

      BlockPos hall = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center);
      level.setBlockAndUpdate(hall, AWCoreBlocks.TOWN_HALL.get().defaultBlockState());
      if (level.getBlockEntity(hall) instanceof TownHallBlockEntity townHall) {
         townHall.setOwner(player.getUUID());
      }

      data.setVillage(hall, player.getUUID());
      data.setTax(hall, tax);
      data.setVillageState(hall, new FactionWorldData.VillageState(settlement.faction(), settlement.population(), 40, 45));
      int workers = 0;
      for (FactionNpcEntity person : people) {
         WorkerNpcEntity worker = workers < MAX_WORKERS ? AWCoreEntityTypes.WORKER_NPC.get().create(level) : null;
         if (worker != null) {
            worker.moveTo(person.getX(), person.getY(), person.getZ(), person.getYRot(), 0.0F);
            worker.finalizeSpawn(level, level.getCurrentDifficultyAt(person.blockPosition()), MobSpawnType.EVENT, null, null);
            worker.setOwner(player.getUUID());
            worker.setPersistenceRequired();
            level.addFreshEntity(worker);
            workers++;
         }

         person.discard();
      }

      return hall;
   }

   private static List<FactionNpcEntity> civilians(ServerLevel level, FactionWorldData.Settlement settlement) {
      AABB area = new AABB(settlement.min(), settlement.max().offset(1, 1, 1)).inflate(8.0, 32.0, 8.0);
      return level.getEntitiesOfClass(
         FactionNpcEntity.class, area, npc -> npc.isAlive() && npc.getFactionName().equals(settlement.faction()) && FactionSimulation.isCivilian(npc)
      );
   }

   /** Daily: villages taken from factions pay their tax into the town hall, or a country's into its treasury. */
   public static void simulateDay(ServerLevel level, FactionWorldData data) {
      for (Map.Entry<BlockPos, UUID> village : data.villages().entrySet()) {
         int tax = FactionLife.villageTax(data, village.getKey());
         if (tax > 0 && !PlayerCountry.collect(data, village.getKey(), village.getValue(), tax) && level.getBlockEntity(village.getKey()) instanceof TownHallBlockEntity hall) {
            Coins.store(hall, tax);
            hall.setChanged();
         }
      }
   }
}
