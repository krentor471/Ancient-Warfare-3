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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;

/**
 * Mercenaries for hire in taverns. Every standing tavern near a player has a mercenary captain;
 * talking to him lists his offers in chat, and clicking one hires a band for emeralds. The band
 * follows its new master as the player's own soldiers until the contract runs out, then leaves.
 * A captain has only so many free swords, and new ones drift in day by day.
 */
public final class FactionMercenaries {
   public static final String CAPTAIN_TAG = "AWMercenaryCaptain";
   /** Game time a mercenary's contract ends, kept in its persistent data. */
   private static final String CONTRACT_TAG = "AWMercenaryUntil";
   private static final String STOCK_TAG = "AWMercenaryStock";
   private static final String STOCK_DAY_TAG = "AWMercenaryStockDay";
   private static final int CHECK_TICKS = 200;
   private static final double CAPTAIN_RANGE = 96.0;
   private static final double HIRE_RANGE = 8.0;
   private static final int MAX_STOCK = 10;
   private static final int STOCK_PER_DAY = 2;
   public static final int CONTRACT_DAYS = 5;
   private static final long DAY_TICKS = 24000L;
   private static final long TALK_COOLDOWN = 40L;
   private static final java.util.Map<java.util.UUID, Long> LAST_TALK = new java.util.HashMap<>();

   public enum Offer {
      SWORDS(3, 0, 12),
      BOWS(0, 3, 15),
      BAND(5, 2, 30);

      final int swords;
      final int bows;
      final int price;

      Offer(int swords, int bows, int price) {
         this.swords = swords;
         this.bows = bows;
         this.price = price;
      }

      int size() {
         return this.swords + this.bows;
      }
   }

   private FactionMercenaries() {
   }

   public static boolean isCaptain(Entity entity) {
      return entity.getPersistentData().getBoolean(CAPTAIN_TAG);
   }

   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % CHECK_TICKS != 0L || level.players().isEmpty()) {
         return;
      }

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (WorldMap.isTavern(settlement) && playerNear(level, settlement) && level.isLoaded(settlement.min()) && level.isLoaded(settlement.max())) {
            ensureCaptain(level, settlement);
         }
      }

      endContracts(level);
   }

   private static boolean playerNear(ServerLevel level, FactionWorldData.Settlement settlement) {
      BlockPos center = settlement.center();
      return level.players().stream().anyMatch(player -> player.distanceToSqr(center.getX(), player.getY(), center.getZ()) < CAPTAIN_RANGE * CAPTAIN_RANGE);
   }

   private static AABB area(FactionWorldData.Settlement settlement) {
      return new AABB(settlement.min(), settlement.max().offset(1, 1, 1)).inflate(24.0, 32.0, 24.0);
   }

   private static void ensureCaptain(ServerLevel level, FactionWorldData.Settlement settlement) {
      // Entities load after their chunks: a captain not yet loaded must not get a twin.
      BlockPos min = settlement.min();
      BlockPos max = settlement.max();
      for (int cx = (min.getX() >> 4) - 1; cx <= (max.getX() >> 4) + 1; cx++) {
         for (int cz = (min.getZ() >> 4) - 1; cz <= (max.getZ() >> 4) + 1; cz++) {
            if (!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.asLong(cx, cz))) {
               return;
            }
         }
      }

      if (!level.getEntitiesOfClass(FactionNpcEntity.class, area(settlement), npc -> npc.isAlive() && isCaptain(npc)).isEmpty()) {
         return;
      }

      FactionNpcEntity captain = (FactionNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_CIVILIAN_MALE_NPC).create(level);
      if (captain == null) {
         return;
      }

      BlockPos spot = standingSpot(level, settlement);
      int x = spot.getX();
      int z = spot.getZ();
      captain.moveTo(x + 0.5, spot.getY(), z + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
      captain.setFactionName(settlement.faction());
      captain.getPersistentData().putBoolean(CAPTAIN_TAG, true);
      captain.getPersistentData().putInt(STOCK_TAG, MAX_STOCK);
      captain.getPersistentData().putLong(STOCK_DAY_TAG, level.getGameTime() / DAY_TICKS);
      captain.setCustomName(Component.translatable("entity.ancientwarfare.mercenary_captain").withStyle(ChatFormatting.GOLD));
      captain.setCustomNameVisible(true);
      captain.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
      captain.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
      captain.setPersistenceRequired();
      level.addFreshEntity(captain);
      AncientWarfareCore.LOG.info("Mercenary captain came to tavern #{} at {}, {}", settlement.id(), x, z);
   }

   /** A floor inside the tavern with room to stand, or the ground beside it when none is found. */
   private static BlockPos standingSpot(ServerLevel level, FactionWorldData.Settlement settlement) {
      RandomSource random = level.getRandom();
      BlockPos min = settlement.min();
      BlockPos max = settlement.max();

      for (int attempt = 0; attempt < 40; attempt++) {
         int x = min.getX() + 1 + random.nextInt(Math.max(1, max.getX() - min.getX() - 1));
         int z = min.getZ() + 1 + random.nextInt(Math.max(1, max.getZ() - min.getZ() - 1));

         for (int y = min.getY(); y <= min.getY() + 4; y++) {
            BlockPos feet = new BlockPos(x, y, z);
            if (level.getBlockState(feet.below()).isSolidRender(level, feet.below())
               && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
               && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
               return feet;
            }
         }
      }

      int x = max.getX() + 2;
      int z = settlement.center().getZ();
      return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
   }

   /** Free swords the captain has now; new ones come in each day up to the most he keeps. */
   private static int stock(ServerLevel level, Entity captain) {
      CompoundTag tag = captain.getPersistentData();
      long day = level.getGameTime() / DAY_TICKS;
      long since = Math.max(0L, day - tag.getLong(STOCK_DAY_TAG));
      int stock = (int)Math.min(MAX_STOCK, tag.getInt(STOCK_TAG) + since * STOCK_PER_DAY);
      tag.putInt(STOCK_TAG, stock);
      tag.putLong(STOCK_DAY_TAG, day);
      return stock;
   }

   /** Captains do not sell swords to those their faction is hostile to: the band would turn on its own. */
   private static boolean atWar(ServerPlayer player, FactionNpcEntity captain) {
      return FactionStandingData.get(player.serverLevel()).isHostile(player.getUUID(), captain.getFactionName());
   }

   /** The captain was spoken to: his offers, each a line to click. */
   public static void talk(ServerPlayer player, FactionNpcEntity captain) {
      long now = player.serverLevel().getGameTime();
      Long last = LAST_TALK.put(player.getUUID(), now);
      if (last != null && now - last < TALK_COOLDOWN && now >= last) {
         // A held button talks every few ticks; one offer is enough.
         return;
      }

      Component faction = FactionNews.factionName(captain.getFactionName());
      if (atWar(player, captain)) {
         player.sendSystemMessage(Component.translatable("mercenary.ancientwarfare.enemy", faction));
         return;
      }

      int stock = stock(player.serverLevel(), captain);
      player.sendSystemMessage(Component.translatable("mercenary.ancientwarfare.greeting", stock, CONTRACT_DAYS).withStyle(ChatFormatting.GOLD));

      for (Offer offer : Offer.values()) {
         MutableComponent line = Component.literal("  [")
            .append(Component.translatable("mercenary.ancientwarfare.offer." + offer.name().toLowerCase(java.util.Locale.ROOT), offer.price))
            .append("]");
         if (offer.size() <= stock) {
            line.withStyle(
               style -> style.withColor(ChatFormatting.GREEN)
                  .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/awhire " + captain.getId() + " " + offer.name().toLowerCase(java.util.Locale.ROOT)))
                  .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("mercenary.ancientwarfare.click")))
            );
         } else {
            line.withStyle(ChatFormatting.DARK_GRAY);
         }

         player.sendSystemMessage(line);
      }
   }

   /** The player clicked an offer: pay, and the band falls in behind them. */
   public static void hire(ServerPlayer player, int captainId, Offer offer) {
      ServerLevel level = player.serverLevel();
      if (!(level.getEntity(captainId) instanceof FactionNpcEntity captain) || !isCaptain(captain) || !captain.isAlive()) {
         return;
      }

      if (captain.distanceToSqr(player) > HIRE_RANGE * HIRE_RANGE) {
         player.sendSystemMessage(Component.translatable("mercenary.ancientwarfare.too_far"));
         return;
      }

      if (atWar(player, captain)) {
         player.sendSystemMessage(Component.translatable("mercenary.ancientwarfare.enemy", FactionNews.factionName(captain.getFactionName())));
         return;
      }

      int stock = stock(level, captain);
      if (offer.size() > stock) {
         player.sendSystemMessage(Component.translatable("mercenary.ancientwarfare.none_left", stock));
         return;
      }

      if (!PlayerDiplomacy.takeEmeralds(player, offer.price)) {
         player.sendSystemMessage(Component.translatable("mercenary.ancientwarfare.no_money", offer.price));
         return;
      }

      captain.getPersistentData().putInt(STOCK_TAG, stock - offer.size());
      long until = level.getGameTime() + CONTRACT_DAYS * DAY_TICKS;
      int spawned = 0;

      for (int i = 0; i < offer.size(); i++) {
         if (spawnMercenary(level, player, captain, i < offer.swords, until)) {
            spawned++;
         }
      }

      player.sendSystemMessage(Component.translatable("mercenary.ancientwarfare.hired", spawned, CONTRACT_DAYS).withStyle(ChatFormatting.GOLD));
      AncientWarfareCore.LOG.info("{} hired {} mercenaries for {} emeralds", player.getScoreboardName(), spawned, offer.price);
   }

   private static boolean spawnMercenary(ServerLevel level, ServerPlayer player, FactionNpcEntity captain, boolean sword, long until) {
      if (!(AWCoreRegistryAccess.entityType(AWCoreIds.COMBAT_NPC).create(level) instanceof CombatNpcEntity mercenary)) {
         return false;
      }

      RandomSource random = level.getRandom();
      double x = captain.getX() + random.nextDouble() * 4.0 - 2.0;
      double z = captain.getZ() + random.nextDouble() * 4.0 - 2.0;
      int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)Math.floor(x), (int)Math.floor(z));
      mercenary.moveTo(x, y, z, random.nextFloat() * 360.0F, 0.0F);
      mercenary.setNpcType("combat");
      mercenary.setOwner(player.getUUID());
      if (sword) {
         mercenary.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
         mercenary.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
         mercenary.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
         mercenary.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.CHAINMAIL_CHESTPLATE));
         mercenary.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
      } else {
         mercenary.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
         mercenary.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
         mercenary.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
      }

      for (EquipmentSlot slot : EquipmentSlot.values()) {
         // Hired gear stays with the band: killing them is no way to arm yourself.
         mercenary.setDropChance(slot, 0.0F);
      }

      mercenary.getPersistentData().putLong(CONTRACT_TAG, until);
      mercenary.setPersistenceRequired();
      level.addFreshEntity(mercenary);
      mercenary.toggleFollow(player);
      return true;
   }

   /** Mercenaries whose contract ran out say farewell and go. */
   private static void endContracts(ServerLevel level) {
      long now = level.getGameTime();
      List<? extends CombatNpcEntity> ending = level.getEntities(
         EntityTypeTest.forClass(CombatNpcEntity.class),
         npc -> npc.isAlive() && npc.getPersistentData().contains(CONTRACT_TAG) && npc.getPersistentData().getLong(CONTRACT_TAG) <= now
      );

      java.util.Map<java.util.UUID, Integer> gone = new java.util.HashMap<>();

      for (CombatNpcEntity mercenary : ending) {
         mercenary.getOwner().ifPresent(owner -> gone.merge(owner, 1, Integer::sum));
         mercenary.discard();
      }

      gone.forEach((owner, count) -> {
         net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(owner);
         if (player != null) {
            player.sendSystemMessage(Component.translatable("mercenary.ancientwarfare.left", count));
         }
      });
   }
}
