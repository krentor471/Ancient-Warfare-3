package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreBlocks;

/**
 * Quest boards in the squares of towns. Each day a town pins up a few tasks that grow out of how it
 * is doing: food when it goes hungry or winter comes, wood, stone and iron when it builds or
 * grows, a bounty on bandits or on the soldiers of a country it is at war with, a letter to carry
 * along the roads to a friendly town. A player takes up to three at a time, brings goods to the
 * board, and is paid in silver and goodwill; the town gets the food or stores, so helping a town
 * through a famine or a winter is the work itself.
 */
public final class FactionQuests {
   public static final int MAX_TAKEN = 3;
   private static final int OFFERS = 3;
   private static final int BOARD_RANGE = 128;
   private static final long QUEST_DAYS = 5L;
   private static final long BOARD_TICKS = 100L;
   private static final double BOARD_NEAR = 96.0;
   private static final int BOARD_REACH = 16;
   private static final int MIN_LETTER = 200;
   private static final int MAX_LETTER = 2000;
   public static final String LETTER_TAG = "AwLetter";
   /** The target of a bounty on outlaws of any kind. */
   private static final String BANDITS = "brigand";

   private FactionQuests() {
   }

   public enum Type {
      FOOD,
      MATERIALS,
      BOUNTY,
      LETTER
   }

   private record Goods(String item, int count, int worth) {
   }

   /** Food a town asks for: so many of an item, each worth so many rations. */
   private static final Goods[] FOOD = {
      new Goods("minecraft:bread", 16, 3), new Goods("minecraft:wheat", 32, 1), new Goods("minecraft:carrot", 32, 1), new Goods("minecraft:potato", 32, 1),
      new Goods("minecraft:cooked_beef", 12, 4), new Goods("minecraft:cooked_porkchop", 12, 4), new Goods("minecraft:baked_potato", 24, 2)
   };
   /** Building goods: so many of an item, adding so much to one of the faction's stores (wood, stone, iron). */
   private static final Goods[] MATERIALS = {
      new Goods("#minecraft:logs", 32, FactionResources.WOOD), new Goods("minecraft:cobblestone", 48, FactionResources.STONE), new Goods("minecraft:iron_ingot", 8, FactionResources.IRON)
   };
   private static final int[] MATERIAL_STORE = {10, 10, 4};

   public static final class Quest {
      final long id;
      final int town;
      final long day;
      final int index;
      final Type type;
      /** The item (or #tag) to bring, or the faction to hunt. */
      final String target;
      final int count;
      int progress;
      final int reward;
      final int standing;
      /** The town a letter goes to. */
      final int destination;
      long expires;
      boolean done;

      Quest(long id, int town, long day, int index, Type type, String target, int count, int reward, int standing, int destination) {
         this.id = id;
         this.town = town;
         this.day = day;
         this.index = index;
         this.type = type;
         this.target = target;
         this.count = count;
         this.reward = reward;
         this.standing = standing;
         this.destination = destination;
      }

      CompoundTag save() {
         CompoundTag tag = new CompoundTag();
         tag.putLong("Id", this.id);
         tag.putInt("Town", this.town);
         tag.putLong("Day", this.day);
         tag.putInt("Index", this.index);
         tag.putString("Type", this.type.name());
         tag.putString("Target", this.target);
         tag.putInt("Count", this.count);
         tag.putInt("Progress", this.progress);
         tag.putInt("Reward", this.reward);
         tag.putInt("Standing", this.standing);
         tag.putInt("Destination", this.destination);
         tag.putLong("Expires", this.expires);
         tag.putBoolean("Done", this.done);
         return tag;
      }

      static Quest load(CompoundTag tag) {
         Type type;
         try {
            type = Type.valueOf(tag.getString("Type"));
         } catch (IllegalArgumentException e) {
            type = Type.FOOD;
         }

         Quest quest = new Quest(
            tag.getLong("Id"),
            tag.getInt("Town"),
            tag.getLong("Day"),
            tag.getInt("Index"),
            type,
            tag.getString("Target"),
            tag.getInt("Count"),
            tag.getInt("Reward"),
            tag.getInt("Standing"),
            tag.getInt("Destination")
         );
         quest.progress = tag.getInt("Progress");
         quest.expires = tag.getLong("Expires");
         quest.done = tag.getBoolean("Done");
         return quest;
      }

      boolean sameOffer(Quest other) {
         return this.town == other.town && this.day == other.day && this.index == other.index;
      }
   }

   /** What the board screen shows of one offer or taken quest. */
   public record Line(long id, Component text, Component detail, int state) {
      public static final int OPEN = 0;
      public static final int TAKEN = 1;
      public static final int DONE = 2;
      public static final int ACTIVE = 3;
      public static final int ACTIVE_HERE = 4;
   }

   private static long today(ServerLevel level) {
      return Math.max(0L, FactionWorldData.get(level).lastSimulatedDay());
   }

   /** The town a board at this spot serves: the nearest town within reach. */
   private static FactionWorldData.Settlement townAt(FactionWorldData data, BlockPos pos) {
      FactionWorldData.Settlement best = null;
      double bestDistance = (double)BOARD_RANGE * BOARD_RANGE;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         double distance = settlement.horizontalDistanceSquared(pos.getX(), pos.getZ());
         if (FactionTownLife.isTown(settlement) && distance < bestDistance) {
            best = settlement;
            bestDistance = distance;
         }
      }

      return best;
   }

   private static FactionWorldData.Settlement byId(FactionWorldData data, int id) {
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (settlement.id() == id) {
            return settlement;
         }
      }

      return null;
   }

   static Component townName(FactionWorldData.Settlement settlement) {
      return settlement.name().isEmpty() ? FactionNews.factionName(settlement.faction()) : settlement.nameComponent();
   }

   /** Today's tasks on a town's board: the same for everyone, new each day. */
   private static List<Quest> offers(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement town, long day) {
      RandomSource random = RandomSource.create(town.id() * 341873128712L + day * 132897987541L + level.getSeed());
      List<Type> kinds = new ArrayList<>();
      List<Integer> weights = new ArrayList<>();
      int mouths = town.population() + town.garrison();
      boolean hungry = town.famine() > 0 || town.food() < mouths * 3 || FactionSeasons.season(level) == FactionSeasons.Season.WINTER;
      kinds.add(Type.FOOD);
      weights.add(hungry ? 4 : 1);
      kinds.add(Type.MATERIALS);
      weights.add(town.growth > 0 ? 3 : 2);
      kinds.add(Type.BOUNTY);
      weights.add(enemy(level, data, town, random) != null ? 3 : 2);
      if (letterTo(level, data, town, RandomSource.create(random.nextLong())) != null) {
         kinds.add(Type.LETTER);
         weights.add(2);
      }

      List<Quest> offers = new ArrayList<>();
      for (int index = 0; index < OFFERS && !kinds.isEmpty(); index++) {
         int total = weights.stream().mapToInt(Integer::intValue).sum();
         int roll = random.nextInt(total);
         int pick = 0;
         while (roll >= weights.get(pick)) {
            roll -= weights.get(pick);
            pick++;
         }

         Type type = kinds.remove(pick);
         weights.remove(pick);
         Quest quest = make(level, data, town, day, index, type, random, hungry);
         if (quest != null) {
            offers.add(quest);
         }
      }

      return offers;
   }

   private static Quest make(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement town, long day, int index, Type type, RandomSource random, boolean hungry) {
      long id = random.nextLong();
      switch (type) {
         case FOOD -> {
            Goods goods = FOOD[random.nextInt(FOOD.length)];
            int reward = 6 + random.nextInt(5) + (hungry ? 4 : 0);
            return new Quest(id, town.id(), day, index, type, goods.item, goods.count, reward, hungry ? 6 : 3, 0);
         }
         case MATERIALS -> {
            Goods goods = MATERIALS[random.nextInt(MATERIALS.length)];
            return new Quest(id, town.id(), day, index, type, goods.item, goods.count, 8 + random.nextInt(7), 3, 0);
         }
         case BOUNTY -> {
            String enemy = enemy(level, data, town, random);
            int count = 5 + random.nextInt(4);
            return new Quest(id, town.id(), day, index, type, enemy == null ? BANDITS : enemy, count, 10 + count * 2, enemy == null ? 4 : 6, 0);
         }
         default -> {
            FactionWorldData.Settlement to = letterTo(level, data, town, RandomSource.create(random.nextLong()));
            if (to == null) {
               return null;
            }

            int distance = (int)Math.sqrt(to.horizontalDistanceSquared(town.center().getX(), town.center().getZ()));
            return new Quest(id, town.id(), day, index, type, "", 1, 4 + distance / 100, 3, to.id());
         }
      }
   }

   /** A country the town's faction is at war with, if any. */
   private static String enemy(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement town, RandomSource random) {
      List<String> enemies = data.settlements()
         .stream()
         .map(FactionWorldData.Settlement::faction)
         .distinct()
         .filter(other -> !other.equals(town.faction()) && !FactionDiplomacy.isOutlaw(other))
         .filter(other -> FactionDiplomacy.relation(level, town.faction(), other).state() == RelationState.WAR)
         .sorted()
         .toList();
      return enemies.isEmpty() ? null : enemies.get(random.nextInt(enemies.size()));
   }

   /** A town of the same or a friendly country, not too near and not too far, for a letter. */
   private static FactionWorldData.Settlement letterTo(ServerLevel level, FactionWorldData data, FactionWorldData.Settlement town, RandomSource random) {
      List<FactionWorldData.Settlement> towns = new ArrayList<>();
      for (FactionWorldData.Settlement other : data.settlements()) {
         if (other == town || !FactionTownLife.isTown(other)) {
            continue;
         }

         double distance = other.horizontalDistanceSquared(town.center().getX(), town.center().getZ());
         if (distance < (double)MIN_LETTER * MIN_LETTER || distance > (double)MAX_LETTER * MAX_LETTER) {
            continue;
         }

         RelationState state = other.faction().equals(town.faction()) ? RelationState.ALLIANCE : FactionDiplomacy.relation(level, town.faction(), other.faction()).state();
         if (state == RelationState.ALLIANCE || state == RelationState.PACT) {
            towns.add(other);
         }
      }

      towns.sort((a, b) -> Integer.compare(a.id(), b.id()));
      return towns.isEmpty() ? null : towns.get(random.nextInt(towns.size()));
   }

   private static Component itemName(String target) {
      if (target.startsWith("#")) {
         return Component.translatable("quest.ancientwarfare.item." + target.substring(target.indexOf(':') + 1));
      }

      Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(target));
      return item.getDescription();
   }

   private static boolean matches(ItemStack stack, String target) {
      if (stack.isEmpty() || stack.hasTag() && stack.getTag().contains(LETTER_TAG)) {
         return false;
      }

      if (target.equals("#minecraft:logs")) {
         return stack.is(ItemTags.LOGS);
      }

      return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(target);
   }

   private static Component describe(ServerLevel level, FactionWorldData data, Quest quest) {
      FactionWorldData.Settlement town = byId(data, quest.town);
      Component townName = town == null ? Component.literal("?") : townName(town);
      return switch (quest.type) {
         case FOOD -> Component.translatable("quest.ancientwarfare.food", quest.count, itemName(quest.target), townName);
         case MATERIALS -> Component.translatable("quest.ancientwarfare.materials", quest.count, itemName(quest.target), townName);
         case BOUNTY -> FactionDiplomacy.isOutlaw(quest.target)
            ? Component.translatable("quest.ancientwarfare.bounty.bandits", quest.count, townName)
            : Component.translatable("quest.ancientwarfare.bounty", quest.count, FactionNews.factionName(quest.target), townName);
         case LETTER -> {
            FactionWorldData.Settlement to = byId(data, quest.destination);
            if (to == null || town == null) {
               yield Component.translatable("quest.ancientwarfare.letter.lost");
            }

            BlockPos from = town.center();
            BlockPos at = to.center();
            yield Component.translatable(
               "quest.ancientwarfare.letter", townName(to), FactionNews.factionName(to.faction()), FactionRumours.where(from.getX(), from.getZ(), at.getX(), at.getZ())
            );
         }
      };
   }

   private static Component rewardLine(Quest quest) {
      return Component.translatable("quest.ancientwarfare.reward", quest.reward, quest.standing);
   }

   private static Component progressLine(ServerLevel level, Quest quest) {
      long left = Math.max(0L, quest.expires - today(level));
      return switch (quest.type) {
         case FOOD, MATERIALS, BOUNTY -> Component.translatable("quest.ancientwarfare.progress", quest.progress, quest.count, left);
         case LETTER -> Component.translatable("quest.ancientwarfare.progress.letter", left);
      };
   }

   /** A player used a board: show its town's tasks and the player's own. */
   public static void open(ServerPlayer player, BlockPos board) {
      ServerLevel level = player.serverLevel();
      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.Settlement town = townAt(data, board);
      List<Line> offers = new ArrayList<>();
      Component title;
      Component note = Component.empty();
      if (town == null) {
         title = Component.translatable("gui.ancientwarfare.quest_board.title.none");
         note = Component.translatable("gui.ancientwarfare.quest_board.no_town");
      } else {
         title = Component.translatable("gui.ancientwarfare.quest_board.title", townName(town), FactionNews.factionName(town.faction()));
         if (FactionStandingData.get(level).isHostile(player.getUUID(), town.faction())) {
            note = Component.translatable("gui.ancientwarfare.quest_board.hostile");
         } else {
            List<Quest> mine = data.quests(player.getUUID());
            for (Quest offer : offers(level, data, town, today(level))) {
               Quest taken = mine.stream().filter(offer::sameOffer).findFirst().orElse(null);
               int state = taken == null ? Line.OPEN : taken.done ? Line.DONE : Line.TAKEN;
               offers.add(new Line(offer.index, describe(level, data, offer), rewardLine(offer), state));
            }
         }
      }

      List<Line> active = new ArrayList<>();
      for (Quest quest : data.quests(player.getUUID())) {
         if (!quest.done) {
            boolean here = town != null && deliverableAt(quest, town);
            active.add(new Line(quest.id, describe(level, data, quest), progressLine(level, quest), here ? Line.ACTIVE_HERE : Line.ACTIVE));
         }
      }

      AWCoreNetwork.sendQuestBoard(player, board, title, note, offers, active);
   }

   private static boolean deliverableAt(Quest quest, FactionWorldData.Settlement town) {
      return switch (quest.type) {
         case FOOD, MATERIALS -> quest.town == town.id();
         case BOUNTY -> quest.town == town.id() && quest.progress >= quest.count;
         case LETTER -> quest.destination == town.id();
      };
   }

   private static boolean nearBoard(ServerPlayer player, BlockPos board) {
      return player.distanceToSqr(board.getX() + 0.5, board.getY() + 0.5, board.getZ() + 0.5) <= 64.0
         && player.level().getBlockState(board).is(AWCoreBlocks.QUEST_BOARD.get());
   }

   /** The player takes one of the board's tasks. */
   public static void take(ServerPlayer player, BlockPos board, int index) {
      ServerLevel level = player.serverLevel();
      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.Settlement town = townAt(data, board);
      if (!nearBoard(player, board) || town == null || FactionStandingData.get(level).isHostile(player.getUUID(), town.faction())) {
         return;
      }

      List<Quest> mine = data.quests(player.getUUID());
      Quest offer = offers(level, data, town, today(level)).stream().filter(quest -> quest.index == index).findFirst().orElse(null);
      if (offer == null || mine.stream().anyMatch(offer::sameOffer)) {
         return;
      }

      if (mine.stream().filter(quest -> !quest.done).count() >= MAX_TAKEN) {
         player.sendSystemMessage(Component.translatable("quest.ancientwarfare.too_many", MAX_TAKEN).withStyle(ChatFormatting.RED));
         open(player, board);
         return;
      }

      offer.expires = today(level) + QUEST_DAYS;
      mine.add(offer);
      data.setDirty();
      if (offer.type == Type.LETTER) {
         FactionWorldData.Settlement to = byId(data, offer.destination);
         ItemStack letter = new ItemStack(Items.PAPER);
         letter.getOrCreateTag().putLong(LETTER_TAG, offer.id);
         letter.setHoverName(Component.translatable("item.ancientwarfare.quest_letter", to == null ? Component.literal("?") : townName(to)));
         player.getInventory().placeItemBackInInventory(letter);
      }

      player.sendSystemMessage(Component.translatable("quest.ancientwarfare.taken", describe(level, data, offer)).withStyle(ChatFormatting.YELLOW));
      open(player, board);
   }

   /** The player brings what a task asks for to the board. */
   public static void turnIn(ServerPlayer player, BlockPos board, long id) {
      ServerLevel level = player.serverLevel();
      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.Settlement town = townAt(data, board);
      Quest quest = data.quests(player.getUUID()).stream().filter(q -> q.id == id && !q.done).findFirst().orElse(null);
      if (!nearBoard(player, board) || town == null || quest == null || !deliverableAt(quest, town)) {
         return;
      }

      switch (quest.type) {
         case FOOD, MATERIALS -> {
            int brought = takeItems(player, quest.target, quest.count - quest.progress);
            if (brought == 0) {
               player.sendSystemMessage(Component.translatable("quest.ancientwarfare.nothing", itemName(quest.target)).withStyle(ChatFormatting.RED));
            }

            quest.progress += brought;
         }
         case LETTER -> {
            if (takeLetter(player, quest.id)) {
               quest.progress = quest.count;
            } else {
               player.sendSystemMessage(Component.translatable("quest.ancientwarfare.no_letter").withStyle(ChatFormatting.RED));
            }
         }
         default -> {
         }
      }

      data.setDirty();
      if (quest.progress >= quest.count) {
         complete(level, data, player, quest);
      }

      open(player, board);
   }

   private static int takeItems(ServerPlayer player, String target, int wanted) {
      int taken = 0;
      for (int slot = 0; slot < player.getInventory().getContainerSize() && taken < wanted; slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (matches(stack, target)) {
            int used = Math.min(stack.getCount(), wanted - taken);
            stack.shrink(used);
            taken += used;
         }
      }

      return taken;
   }

   private static boolean takeLetter(ServerPlayer player, long id) {
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (!stack.isEmpty() && stack.hasTag() && stack.getTag().getLong(LETTER_TAG) == id && stack.getTag().contains(LETTER_TAG)) {
            stack.shrink(1);
            return true;
         }
      }

      return false;
   }

   private static void complete(ServerLevel level, FactionWorldData data, ServerPlayer player, Quest quest) {
      quest.done = true;
      FactionWorldData.Settlement town = byId(data, quest.town);
      if (town != null) {
         if (quest.type == Type.FOOD) {
            int worth = 1;
            for (Goods goods : FOOD) {
               if (goods.item.equals(quest.target)) {
                  worth = goods.worth;
               }
            }

            town.food = Math.max(0, town.food) + quest.count * worth;
            if (town.food >= (town.population() + town.garrison()) * 2) {
               town.famine = 0;
            }
         } else if (quest.type == Type.MATERIALS) {
            for (int i = 0; i < MATERIALS.length; i++) {
               if (MATERIALS[i].item.equals(quest.target)) {
                  data.stores(town.faction())[MATERIALS[i].worth] += MATERIAL_STORE[i];
               }
            }
         }

         FactionStandingData.get(level).adjustStanding(player.getUUID(), town.faction(), quest.standing);
      }

      Coins.give(player, quest.reward);
      FactionChatter.thank(level, player);
      data.setDirty();
      player.sendSystemMessage(Component.translatable("quest.ancientwarfare.done", describe(level, data, quest), quest.reward).withStyle(ChatFormatting.GREEN));
      AncientWarfareCore.LOG.info("{} finished a {} quest for settlement #{}", player.getScoreboardName(), quest.type, quest.town);
   }

   /** The player gives up a task. */
   public static void abandon(ServerPlayer player, BlockPos board, long id) {
      FactionWorldData data = FactionWorldData.get(player.serverLevel());
      if (data.quests(player.getUUID()).removeIf(quest -> quest.id == id && !quest.done)) {
         takeLetter(player, id);
         data.setDirty();
      }

      if (nearBoard(player, board)) {
         open(player, board);
      }
   }

   /** A bounty counts the enemies a player kills. */
   public static void onNpcKilled(ServerLevel level, FactionNpcEntity npc, Player killer) {
      if (!(killer instanceof ServerPlayer player)) {
         return;
      }

      FactionWorldData data = FactionWorldData.get(level);
      List<Quest> mine = data.allQuests().get(player.getUUID());
      if (mine == null) {
         return;
      }

      String faction = npc.getFactionName();
      for (Quest quest : mine) {
         boolean hit = FactionDiplomacy.isOutlaw(quest.target) ? FactionDiplomacy.isOutlaw(faction) : quest.target.equals(faction);
         if (quest.type == Type.BOUNTY && !quest.done && quest.progress < quest.count && hit) {
            quest.progress++;
            data.setDirty();
            FactionWorldData.Settlement town = byId(data, quest.town);
            Component name = town == null ? Component.literal("?") : townName(town);
            player.displayClientMessage(
               quest.progress >= quest.count
                  ? Component.translatable("quest.ancientwarfare.bounty.met", name).withStyle(ChatFormatting.GREEN)
                  : Component.translatable("quest.ancientwarfare.bounty.progress", quest.progress, quest.count).withStyle(ChatFormatting.YELLOW),
               quest.progress < quest.count
            );
            return;
         }
      }
   }

   /** Daily: finished tasks are cleared away and those not done in time are lost. */
   public static void simulateDay(ServerLevel level, FactionWorldData data) {
      long today = today(level);
      for (Map.Entry<UUID, List<Quest>> entry : data.allQuests().entrySet()) {
         Iterator<Quest> quests = entry.getValue().iterator();
         while (quests.hasNext()) {
            Quest quest = quests.next();
            if (quest.done && quest.day < today) {
               quests.remove();
            } else if (!quest.done && quest.expires < today) {
               quests.remove();
               FactionNews.tellPlayer(level, entry.getKey(), Component.translatable("quest.ancientwarfare.failed", describe(level, data, quest)).withStyle(ChatFormatting.RED));
            }
         }
      }

      data.allQuests().values().removeIf(List::isEmpty);
      data.setDirty();
   }

   /** Every town a player comes near gets a board in its square, if it has none standing. */
   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % BOARD_TICKS != 0L || level.players().isEmpty()) {
         return;
      }

      for (FactionWorldData.Settlement town : data.settlements()) {
         if (!FactionTownLife.isTown(town) || !playerNear(level, town)) {
            continue;
         }

         if (town.board != null) {
            if (!level.isLoaded(town.board) || level.getBlockState(town.board).is(AWCoreBlocks.QUEST_BOARD.get())) {
               continue;
            }
         }

         BlockPos spot = boardSpot(level, town);
         if (spot != null) {
            Direction facing = facing(spot, town.center());
            level.setBlock(spot, AWCoreBlocks.QUEST_BOARD.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing), 3);
            town.board = spot;
            data.setDirty();
            AncientWarfareCore.LOG.info("Settlement #{} puts up a quest board at {}", town.id(), spot);
         }
      }
   }

   private static boolean playerNear(ServerLevel level, FactionWorldData.Settlement town) {
      for (ServerPlayer player : level.players()) {
         if (town.horizontalDistanceSquared(player.getX(), player.getZ()) <= BOARD_NEAR * BOARD_NEAR) {
            return true;
         }
      }

      return false;
   }

   private static Direction facing(BlockPos spot, BlockPos center) {
      int dx = center.getX() - spot.getX();
      int dz = center.getZ() - spot.getZ();
      if (dx == 0 && dz == 0) {
         return Direction.SOUTH;
      }

      return Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
   }

   /** Open ground near the middle of the town, on a street or the square rather than a roof. */
   private static BlockPos boardSpot(ServerLevel level, FactionWorldData.Settlement town) {
      BlockPos center = town.center();
      for (int radius = 2; radius <= BOARD_REACH; radius++) {
         for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
               if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                  continue;
               }

               int x = center.getX() + dx;
               int z = center.getZ() + dz;
               BlockPos column = new BlockPos(x, center.getY(), z);
               if (!level.isLoaded(column)) {
                  return null;
               }

               BlockPos ground = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
               if (ground.getY() >= town.min().getY() - 3 && ground.getY() <= town.max().getY() && street(level.getBlockState(ground)) && free(level, ground.above()) && free(level, ground.above(2))) {
                  return ground.above();
               }
            }
         }
      }

      return null;
   }

   private static boolean street(BlockState state) {
      return state.is(BlockTags.DIRT)
         || state.is(BlockTags.SAND)
         || state.is(net.minecraft.world.level.block.Blocks.DIRT_PATH)
         || state.is(net.minecraft.world.level.block.Blocks.GRAVEL)
         || state.is(net.minecraft.world.level.block.Blocks.COBBLESTONE)
         || state.is(net.minecraft.world.level.block.Blocks.STONE_BRICKS)
         || state.is(net.minecraft.world.level.block.Blocks.STONE);
   }

   private static boolean free(ServerLevel level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty();
   }
}
