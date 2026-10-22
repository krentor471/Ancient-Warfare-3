package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;
import net.shadowmage.ancientwarfare.core.structure.FactionTowns;

/**
 * Days that bring a town to life. Each town keeps a market day once a week: extra traders, some
 * from friendly countries, crowd its middle from morning to evening and prices drop a little. A
 * town celebrates a victory of its country or a rich harvest with a festival: a day of music,
 * people in the square, fireworks after dark and cheaper goods. The crowds appear only while a
 * player is near, and leave when the day is over.
 */
public final class FactionTownLife {
   public static final String TAG_PREFIX = "aw_fair_";
   private static final int TICK_INTERVAL = 20;
   private static final int WEEK_DAYS = 7;
   private static final long MARKET_OPENS = 1000L;
   private static final long MARKET_CLOSES = 11000L;
   private static final double CROWD_RANGE = 96.0;
   private static final double LEAVE_RANGE = 160.0;
   private static final int MARKET_TRADERS = 3;
   private static final int VISITING_TRADERS = 2;
   private static final int FESTIVAL_PEOPLE = 5;
   private static final int SPREAD = 10;
   private static final double MARKET_PRICES = 0.9;
   private static final double FESTIVAL_PRICES = 0.8;
   private static final long FESTIVAL_TICKS = FactionSimulation.DAY_TICKS;
   private static final double HARVEST_FESTIVAL_CHANCE = 0.08;
   private static final int FIREWORK_TICKS = 60;
   private static final int MUSIC_TICKS = 6;
   private static final SoundEvent[] INSTRUMENTS = {
      SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundEvents.NOTE_BLOCK_GUITAR.value()
   };
   /** A merry tune in note-block pitches, played over and over. */
   private static final int[] TUNE = {0, 4, 7, 12, 7, 4, 5, 9, 12, 9, 5, 2, 7, 11, 14, 11, 7, 4, 0, -1};

   /** Festivals under way, by settlement: when they end and why they are held. */
   private static final Map<Integer, Festival> FESTIVALS = new HashMap<>();
   /** Crowds standing in towns now, by settlement. */
   private static final Map<Integer, Crowd> CROWDS = new HashMap<>();
   /** Who has been told of the day's event in a town, so it is said once. */
   private static final Set<String> TOLD = new HashSet<>();

   private record Festival(long ends, String reason, String reasonArg) {
   }

   private FactionTownLife() {
   }

   private enum Event {
      MARKET,
      FESTIVAL;
   }

   /** A town of the winner holds a festival for a victory. */
   public static void celebrateVictory(ServerLevel level, FactionWorldData data, String winner, String loser) {
      FactionWorldData.Settlement capital = null;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.faction().equals(winner) && settlement.built() && (capital == null || FactionTowns.CAPITAL.equals(settlement.kind()))) {
            capital = settlement;
         }
      }

      if (capital != null && !FactionDiplomacy.isOutlaw(winner)) {
         FESTIVALS.put(capital.id(), new Festival(level.getGameTime() + FESTIVAL_TICKS, "victory", loser));
         AncientWarfareCore.LOG.info("Settlement #{} of {} celebrates a victory over {}", capital.id(), winner, loser);
      }
   }

   /** Daily: towns with full stores and good order may hold a harvest festival. */
   public static void simulateDay(ServerLevel level, FactionWorldData data) {
      long now = level.getGameTime();
      FESTIVALS.values().removeIf(festival -> festival.ends <= now);
      TOLD.clear();
      RandomSource random = level.getRandom();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         int mouths = settlement.population() + settlement.garrison();
         if (isTown(settlement) && settlement.order() >= 60 && settlement.food() > mouths * 6 && random.nextDouble() < HARVEST_FESTIVAL_CHANCE) {
            FESTIVALS.putIfAbsent(settlement.id(), new Festival(now + FESTIVAL_TICKS, "harvest", ""));
         }
      }
   }

   private static boolean isTown(FactionWorldData.Settlement settlement) {
      return settlement.built() && !FactionDiplomacy.isOutlaw(settlement.faction()) && !FactionAI.isFort(settlement) && settlement.population() >= 8;
   }

   /** What a town has on today, if anything. */
   private static Event eventAt(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement) {
      if (!isTown(settlement) || underAttack(data, settlement) || settlement.plague() > 0) {
         return null;
      }

      Festival festival = FESTIVALS.get(settlement.id());
      if (festival != null && festival.ends > level.getGameTime()) {
         return Event.FESTIVAL;
      }

      long day = level.getDayTime() / FactionSimulation.DAY_TICKS;
      long time = level.getDayTime() % FactionSimulation.DAY_TICKS;
      return Math.floorMod(day, WEEK_DAYS) == Math.floorMod(settlement.id(), WEEK_DAYS) && time >= MARKET_OPENS && time < MARKET_CLOSES && settlement.famine() == 0
         ? Event.MARKET
         : null;
   }

   private static boolean underAttack(FactionWorldData data, FactionWorldData.Settlement settlement) {
      for (FactionWorldData.Campaign campaign : data.campaigns()) {
         if (campaign.targetSettlement() == settlement.id() && !campaign.relief() && campaign.hunt() == 0) {
            return true;
         }
      }

      return data.armies().stream().anyMatch(army -> army.targetSettlement() == settlement.id());
   }

   /** Prices of a trader standing in a town on its market day or festival. */
   public static double priceFactor(ServerLevel level, String faction, double x, double z) {
      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.Settlement settlement = FactionSimulation.settlementAt(data, faction, x, z, 24);
      Event event = settlement == null ? null : eventAt(level, data, settlement);
      return event == Event.FESTIVAL ? FESTIVAL_PRICES : event == Event.MARKET ? MARKET_PRICES : 1.0;
   }

   public static void tick(ServerLevel level, FactionWorldData data) {
      long now = level.getGameTime();
      for (Crowd crowd : CROWDS.values()) {
         if (crowd.event == Event.FESTIVAL) {
            crowd.celebrate(level, now);
         }
      }

      if (now % TICK_INTERVAL != 0L) {
         return;
      }

      Map<Integer, FactionWorldData.Settlement> byId = new HashMap<>();
      data.settlements().forEach(settlement -> byId.put(settlement.id(), settlement));
      Iterator<Crowd> iterator = CROWDS.values().iterator();
      while (iterator.hasNext()) {
         Crowd crowd = iterator.next();
         FactionWorldData.Settlement settlement = byId.get(crowd.settlement);
         if (settlement == null || eventAt(level, data, settlement) != crowd.event || !playerWithin(level, settlement, LEAVE_RANGE)) {
            crowd.discard(level);
            iterator.remove();
         }
      }

      for (ServerPlayer player : level.players()) {
         for (FactionWorldData.Settlement settlement : data.settlements()) {
            if (settlement.horizontalDistanceSquared(player.getX(), player.getZ()) > CROWD_RANGE * CROWD_RANGE) {
               continue;
            }

            Event event = eventAt(level, data, settlement);
            if (event == null) {
               continue;
            }

            if (!CROWDS.containsKey(settlement.id())) {
               Crowd crowd = new Crowd(settlement.id(), event, settlement.center());
               CROWDS.put(settlement.id(), crowd);
               crowd.gather(level, data, settlement);
            }

            if (TOLD.add(player.getUUID() + "/" + settlement.id() + "/" + event)) {
               tell(player, settlement, event);
            }
         }
      }
   }

   private static void tell(ServerPlayer player, FactionWorldData.Settlement settlement, Event event) {
      Component name = settlement.name().isEmpty() ? FactionNews.factionName(settlement.faction()) : settlement.nameComponent();
      if (event == Event.MARKET) {
         player.sendSystemMessage(Component.translatable("town.ancientwarfare.market", name, Math.round((1.0 - MARKET_PRICES) * 100)).withStyle(ChatFormatting.GOLD));
      } else {
         Festival festival = FESTIVALS.get(settlement.id());
         Component why = festival != null && "victory".equals(festival.reason)
            ? Component.translatable("town.ancientwarfare.festival.victory", FactionNews.factionName(festival.reasonArg))
            : Component.translatable("town.ancientwarfare.festival.harvest");
         player.sendSystemMessage(
            Component.translatable("town.ancientwarfare.festival", name, why, Math.round((1.0 - FESTIVAL_PRICES) * 100)).withStyle(ChatFormatting.GOLD)
         );
      }
   }

   private static boolean playerWithin(ServerLevel level, FactionWorldData.Settlement settlement, double range) {
      BlockPos center = settlement.center();
      return level.players().stream().anyMatch(player -> player.distanceToSqr(center.getX(), player.getY(), center.getZ()) < range * range);
   }

   public static boolean isFairgoer(Entity entity) {
      return entity.getTags().stream().anyMatch(tag -> tag.startsWith(TAG_PREFIX));
   }

   /** A fairgoer from an old save or a crowd that has gone. */
   public static boolean isStale(Entity entity) {
      for (String tag : entity.getTags()) {
         if (tag.startsWith(TAG_PREFIX)) {
            try {
               return !CROWDS.containsKey(Integer.parseInt(tag.substring(TAG_PREFIX.length())));
            } catch (NumberFormatException e) {
               return true;
            }
         }
      }

      return false;
   }

   /** For the debug command: the town holds a festival from now. */
   public static void startFestival(ServerLevel level, FactionWorldData.Settlement settlement) {
      FESTIVALS.put(settlement.id(), new Festival(level.getGameTime() + FESTIVAL_TICKS, "harvest", ""));
   }

   private static final class Crowd {
      private final int settlement;
      private final Event event;
      private final BlockPos center;
      private final List<UUID> people = new ArrayList<>();
      private int note;

      Crowd(int settlement, Event event, BlockPos center) {
         this.settlement = settlement;
         this.event = event;
         this.center = center;
      }

      void gather(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement) {
         RandomSource random = level.getRandom();
         String faction = settlement.faction();
         if (this.event == Event.MARKET) {
            for (int i = 0; i < MARKET_TRADERS; i++) {
               this.spawn(level, AWCoreEntityTypes.FACTION_TRADER_NPC.get(), faction, random);
            }

            List<String> friends = friendlyNeighbours(level, data, settlement);
            for (int i = 0; i < VISITING_TRADERS && !friends.isEmpty(); i++) {
               this.spawn(level, AWCoreEntityTypes.FACTION_TRADER_NPC.get(), friends.get(random.nextInt(friends.size())), random);
            }

            for (int i = 0; i < 3; i++) {
               this.spawn(level, civilian(random), faction, random);
            }
         } else {
            this.spawn(level, AWCoreEntityTypes.FACTION_BARD_NPC.get(), faction, random);
            this.spawn(level, AWCoreEntityTypes.FACTION_TRADER_NPC.get(), faction, random);
            for (int i = 0; i < FESTIVAL_PEOPLE; i++) {
               this.spawn(level, civilian(random), faction, random);
            }
         }

         AncientWarfareCore.LOG.debug("{} at settlement #{}: {} people", this.event, this.settlement, this.people.size());
      }

      private static EntityType<? extends FactionNpcEntity> civilian(RandomSource random) {
         return random.nextBoolean() ? AWCoreEntityTypes.FACTION_CIVILIAN_MALE_NPC.get() : AWCoreEntityTypes.FACTION_CIVILIAN_FEMALE_NPC.get();
      }

      /** Somewhere in the open near the middle of town, not on a roof. */
      private void spawn(ServerLevel level, EntityType<? extends FactionNpcEntity> type, String faction, RandomSource random) {
         for (int attempt = 0; attempt < 8; attempt++) {
            int x = this.center.getX() + random.nextInt(SPREAD * 2 + 1) - SPREAD;
            int z = this.center.getZ() + random.nextInt(SPREAD * 2 + 1) - SPREAD;
            if (!level.hasChunk(x >> 4, z >> 4)) {
               continue;
            }

            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockState below = level.getBlockState(new BlockPos(x, y - 1, z));
            if (Math.abs(y - this.center.getY()) > 6 || !below.getFluidState().isEmpty()) {
               continue;
            }

            FactionNpcEntity npc = type.create(level);
            if (npc == null) {
               return;
            }

            npc.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
            npc.addTag(TAG_PREFIX + this.settlement);
            npc.finalizeSpawn(level, level.getCurrentDifficultyAt(new BlockPos(x, y, z)), MobSpawnType.EVENT, null, null);
            npc.setFactionName(faction);
            npc.setPersistenceRequired();
            level.addFreshEntity(npc);
            this.people.add(npc.getUUID());
            return;
         }
      }

      /** Music all day and fireworks after dark. */
      void celebrate(ServerLevel level, long now) {
         if (!level.hasChunk(this.center.getX() >> 4, this.center.getZ() >> 4)) {
            return;
         }

         RandomSource random = level.getRandom();
         if (now % MUSIC_TICKS == 0L) {
            int pitch = TUNE[this.note++ % TUNE.length];
            if (pitch >= 0) {
               SoundEvent instrument = INSTRUMENTS[(this.note / TUNE.length) % INSTRUMENTS.length];
               level.playSound(null, this.center.above(2), instrument, SoundSource.RECORDS, 2.0F, (float)Math.pow(2.0, (pitch - 12) / 12.0));
               level.sendParticles(
                  ParticleTypes.NOTE, this.center.getX() + 0.5, this.center.getY() + 3.0, this.center.getZ() + 0.5, 1, 0.5, 0.2, 0.5, pitch / 24.0
               );
            }
         }

         long time = level.getDayTime() % FactionSimulation.DAY_TICKS;
         if (time > 13000L && time < 22000L && now % FIREWORK_TICKS == 0L && random.nextInt(3) > 0) {
            this.firework(level, random);
         }
      }

      private void firework(ServerLevel level, RandomSource random) {
         ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
         CompoundTag fireworks = rocket.getOrCreateTagElement("Fireworks");
         CompoundTag explosion = new CompoundTag();
         explosion.putByte("Type", (byte)random.nextInt(5));
         int[] colors = new int[1 + random.nextInt(3)];
         for (int i = 0; i < colors.length; i++) {
            colors[i] = DyeColor.values()[random.nextInt(DyeColor.values().length)].getFireworkColor();
         }

         explosion.putIntArray("Colors", colors);
         explosion.putBoolean("Trail", random.nextBoolean());
         ListTag explosions = new ListTag();
         explosions.add(explosion);
         fireworks.put("Explosions", explosions);
         fireworks.putByte("Flight", (byte)(1 + random.nextInt(2)));
         double x = this.center.getX() + 0.5 + random.nextInt(9) - 4;
         double z = this.center.getZ() + 0.5 + random.nextInt(9) - 4;
         int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int)Math.floor(x), (int)Math.floor(z));
         level.addFreshEntity(new FireworkRocketEntity(level, x, y + 1.0, z, rocket));
      }

      void discard(ServerLevel level) {
         for (UUID id : this.people) {
            Entity entity = level.getEntity(id);
            if (entity != null) {
               entity.discard();
            }
         }
      }
   }

   /** Countries at peace or better with the town's, whose traders come to its market. */
   private static List<String> friendlyNeighbours(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement settlement) {
      Set<String> near = new HashSet<>();
      for (FactionWorldData.Settlement other : data.settlements()) {
         String faction = other.faction();
         if (!faction.equals(settlement.faction())
            && !FactionDiplomacy.isOutlaw(faction)
            && other.horizontalDistanceSquared(settlement.center().getX(), settlement.center().getZ()) < 1500.0 * 1500.0
            && !FactionDiplomacy.isHostile(level, faction, settlement.faction())) {
            near.add(faction);
         }
      }

      return new ArrayList<>(near);
   }
}
