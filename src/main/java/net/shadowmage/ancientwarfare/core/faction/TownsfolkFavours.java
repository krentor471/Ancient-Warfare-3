package net.shadowmage.ancientwarfare.core.faction;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.WorkerNpcEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;

/**
 * What a player and the townsfolk do for each other besides passing the news. A townsperson may
 * have a small trouble and ask for a hand: firewood, bread, a few eggs since the hen went missing;
 * brought, it is paid for in silver and goodwill. A small gift (flowers, bread, something sweet)
 * warms them, and those a player has helped or given to remember his name and greet him by it.
 * A player on good terms with the town may hire a townsperson to work in his own village. Beggars
 * at the town ask for a coin, and the town thinks the better of whoever gives; children chatter.
 */
public final class TownsfolkFavours {
   public static final String BEGGAR_TAG = "AWBeggar";
   private static final String REQUEST = "AWRequest";
   private static final String FRIENDS = "AWFriends";
   private static final String GIFT_DAY = "AWGiftDay";
   private static final String ALMS_DAY = "AWAlmsDay";
   private static final int MAX_FRIENDS = 8;
   private static final double REQUEST_CHANCE = 0.35;
   private static final int REQUEST_STANDING = 2;
   private static final int GIFT_STANDING = 1;
   private static final int ALMS_STANDING = 1;
   private static final int ALMS_ORDER = 2;
   private static final int HIRE_PRICE = 25;
   /** Standing with the town's country below which nobody there will work for the player. */
   private static final int HIRE_STANDING = 10;
   private static final int CHILD_LINES = 8;
   private static final int BEGGAR_LINES = 4;
   private static final String HERO = "AWHero";
   private static final int HERO_RANGE = 48;
   private static final int HERO_KILLS = 3;
   private static final int HERO_DAYS = 7;
   private static final int HERO_STANDING = 5;
   private static final double HERO_PRICES = 0.8;
   private static final int HERO_LINES = 4;

   private record Need(String key, String item, int count, int reward) {
   }

   private static final Need[] NEEDS = {
      new Need("firewood", "#minecraft:logs", 16, 6),
      new Need("bread", "minecraft:bread", 4, 5),
      new Need("eggs", "minecraft:egg", 6, 4),
      new Need("wool", "#minecraft:wool", 3, 5),
      new Need("torches", "minecraft:torch", 8, 4),
      new Need("fish", "minecraft:cod", 4, 5),
      new Need("honey", "minecraft:honey_bottle", 1, 6),
      new Need("carrots", "minecraft:carrot", 8, 4),
      new Need("string", "minecraft:string", 4, 4),
      new Need("milk", "minecraft:milk_bucket", 1, 5)
   };

   private TownsfolkFavours() {
   }

   static boolean matchesItem(ItemStack stack, String item) {
      return matches(stack, item);
   }

   private static boolean matches(ItemStack stack, String item) {
      if (item.startsWith("#")) {
         return stack.is(TagKey.create(net.minecraft.core.registries.Registries.ITEM, new ResourceLocation(item.substring(1))));
      }

      return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(item);
   }

   private static boolean gift(ItemStack stack) {
      return stack.is(ItemTags.SMALL_FLOWERS)
         || stack.is(Items.BREAD)
         || stack.is(Items.CAKE)
         || stack.is(Items.COOKIE)
         || stack.is(Items.PUMPKIN_PIE)
         || stack.is(Items.HONEY_BOTTLE)
         || stack.is(Items.SWEET_BERRIES);
   }

   private static Need need(FactionNpcEntity npc) {
      String key = npc.getPersistentData().getString(REQUEST);
      for (Need need : NEEDS) {
         if (need.key.equals(key)) {
            return need;
         }
      }

      return null;
   }

   /** Whether a townsperson takes what the player holds: what he asked for, a gift, or a coin for a beggar. */
   public static boolean takesItem(FactionNpcEntity npc, ItemStack stack) {
      Need need = need(npc);
      return need != null && matches(stack, need.item) || gift(stack);
   }

   public static boolean isBeggar(FactionNpcEntity npc) {
      return npc.getTags().contains(BEGGAR_TAG);
   }

   /** The player right-clicked a townsperson, with what he holds. */
   public static void interact(ServerPlayer player, FactionNpcEntity npc, ItemStack held) {
      ServerLevel level = player.serverLevel();
      Component speaker = npc.getDisplayName();
      if (FactionStandingData.get(level).isHostile(player.getUUID(), npc.getFactionName())) {
         FactionRumours.chat(player, npc);
         return;
      }

      Need need = need(npc);
      if (need != null && !held.isEmpty() && matches(held, need.item)) {
         deliver(player, npc, need);
         return;
      }

      if (!held.isEmpty() && gift(held)) {
         takeGift(player, npc, held);
         return;
      }

      if (isBeggar(npc)) {
         alms(player, npc);
         return;
      }

      if (npc.isBaby()) {
         FactionRumours.say(player, speaker, Component.translatable("townsfolk.ancientwarfare.child." + npc.getRandom().nextInt(CHILD_LINES)));
         return;
      }

      if (isHero(player, npc.getFactionName())) {
         FactionRumours.say(player, speaker, Component.translatable("townsfolk.ancientwarfare.hero." + npc.getRandom().nextInt(HERO_LINES), player.getName()));
      } else if (isFriend(npc, player)) {
         FactionRumours.say(player, speaker, Component.translatable("townsfolk.ancientwarfare.hello_friend", player.getName()));
      }

      if (need != null) {
         FactionRumours.say(player, speaker, request(need));
      } else if (npc.getRandom().nextDouble() < REQUEST_CHANCE) {
         Need fresh = NEEDS[npc.getRandom().nextInt(NEEDS.length)];
         npc.getPersistentData().putString(REQUEST, fresh.key);
         FactionRumours.say(player, speaker, request(fresh));
      } else {
         FactionRumours.chat(player, npc);
      }

      TownTrades.offer(player, npc);
      if (FactionStandingData.get(level).getStanding(player.getUUID(), npc.getFactionName()) >= HIRE_STANDING) {
         player.sendSystemMessage(
            Component.literal("  [")
               .append(Component.translatable("townsfolk.ancientwarfare.hire", HIRE_PRICE))
               .append("]")
               .withStyle(
                  style -> style.withColor(ChatFormatting.GREEN)
                     .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/awtownsfolk " + npc.getId() + " hire"))
                     .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("townsfolk.ancientwarfare.hire.hint")))
               )
         );
      }
   }

   private static Component request(Need need) {
      return Component.translatable("townsfolk.ancientwarfare.need." + need.key, need.count, need.reward);
   }

   private static void deliver(ServerPlayer player, FactionNpcEntity npc, Need need) {
      int have = 0;
      for (ItemStack stack : player.getInventory().items) {
         if (matches(stack, need.item)) {
            have += stack.getCount();
         }
      }

      if (have < need.count) {
         FactionRumours.say(player, npc.getDisplayName(), Component.translatable("townsfolk.ancientwarfare.not_enough", need.count));
         return;
      }

      int left = need.count;
      for (ItemStack stack : player.getInventory().items) {
         if (left > 0 && matches(stack, need.item)) {
            int taken = Math.min(left, stack.getCount());
            if (stack.is(Items.MILK_BUCKET)) {
               // The bucket comes back empty.
               player.getInventory().add(new ItemStack(Items.BUCKET));
            }

            stack.shrink(taken);
            left -= taken;
         }
      }

      npc.getPersistentData().remove(REQUEST);
      Coins.give(player, need.reward);
      FactionStandingData.get(player.serverLevel()).adjustStanding(player.getUUID(), npc.getFactionName(), REQUEST_STANDING);
      befriend(npc, player);
      FactionRumours.say(player, npc.getDisplayName(), Component.translatable("townsfolk.ancientwarfare.thanks", need.reward));
   }

   private static void takeGift(ServerPlayer player, FactionNpcEntity npc, ItemStack held) {
      ServerLevel level = player.serverLevel();
      long day = level.getDayTime() / FactionSimulation.DAY_TICKS;
      CompoundTag data = npc.getPersistentData();
      String key = GIFT_DAY + player.getStringUUID();
      if (data.getLong(key) == day + 1) {
         FactionRumours.say(player, npc.getDisplayName(), Component.translatable("townsfolk.ancientwarfare.gift.again"));
         return;
      }

      held.shrink(1);
      data.putLong(key, day + 1);
      FactionStandingData.get(level).adjustStanding(player.getUUID(), npc.getFactionName(), GIFT_STANDING);
      befriend(npc, player);
      FactionRumours.say(player, npc.getDisplayName(), Component.translatable("townsfolk.ancientwarfare.gift." + npc.getRandom().nextInt(3), player.getName()));
   }

   /** A coin for a beggar: once a day the town notices. */
   private static void alms(ServerPlayer player, FactionNpcEntity beggar) {
      ServerLevel level = player.serverLevel();
      long day = level.getDayTime() / FactionSimulation.DAY_TICKS;
      CompoundTag data = beggar.getPersistentData();
      if (data.getLong(ALMS_DAY) == day + 1 || !Coins.take(player, 1)) {
         FactionRumours.say(player, beggar.getDisplayName(), Component.translatable("townsfolk.ancientwarfare.beggar." + beggar.getRandom().nextInt(BEGGAR_LINES)));
         return;
      }

      data.putLong(ALMS_DAY, day + 1);
      FactionStandingData.get(level).adjustStanding(player.getUUID(), beggar.getFactionName(), ALMS_STANDING);
      FactionWorldData worldData = FactionWorldData.get(level);
      FactionWorldData.Settlement town = FactionSimulation.settlementAt(worldData, beggar.getFactionName(), beggar.getX(), beggar.getZ(), 48);
      if (town != null) {
         town.order = Math.min(100, town.order + ALMS_ORDER);
         worldData.setDirty();
      }

      FactionRumours.say(player, beggar.getDisplayName(), Component.translatable("townsfolk.ancientwarfare.alms"));
   }

   /** The player clicked to hire a townsperson: paid, they leave the town and work for him. */
   public static void hire(ServerPlayer player, int npcId) {
      ServerLevel level = player.serverLevel();
      if (!(level.getEntity(npcId) instanceof FactionNpcEntity npc) || !npc.isAlive() || npc.isBaby() || isBeggar(npc) || npc.distanceToSqr(player) > 64.0) {
         player.sendSystemMessage(Component.translatable("traveller.ancientwarfare.gone"));
         return;
      }

      if (FactionStandingData.get(level).getStanding(player.getUUID(), npc.getFactionName()) < HIRE_STANDING) {
         FactionRumours.say(player, npc.getDisplayName(), Component.translatable("townsfolk.ancientwarfare.hire.refused"));
         return;
      }

      if (!Coins.take(player, HIRE_PRICE)) {
         player.sendSystemMessage(Component.translatable("traveller.ancientwarfare.no_silver"));
         return;
      }

      WorkerNpcEntity worker = AWCoreEntityTypes.WORKER_NPC.get().create(level);
      if (worker == null) {
         Coins.give(player, HIRE_PRICE);
         return;
      }

      worker.moveTo(npc.getX(), npc.getY(), npc.getZ(), npc.getYRot(), 0.0F);
      worker.finalizeSpawn(level, level.getCurrentDifficultyAt(npc.blockPosition()), MobSpawnType.EVENT, null, null);
      worker.setOwner(player.getUUID());
      worker.setPersistenceRequired();
      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.Settlement town = FactionSimulation.settlementAt(data, npc.getFactionName(), npc.getX(), npc.getZ(), 48);
      if (town != null && town.population > 1) {
         town.population--;
         data.setDirty();
      }

      FactionRumours.say(player, npc.getDisplayName(), Component.translatable("townsfolk.ancientwarfare.hired"));
      npc.discard();
      level.addFreshEntity(worker);
   }

   private static boolean isFriend(FactionNpcEntity npc, ServerPlayer player) {
      for (Tag name : npc.getPersistentData().getList(FRIENDS, Tag.TAG_STRING)) {
         if (name.getAsString().equals(player.getStringUUID())) {
            return true;
         }
      }

      return false;
   }

   private static void befriend(FactionNpcEntity npc, ServerPlayer player) {
      if (isFriend(npc, player)) {
         return;
      }

      ListTag friends = npc.getPersistentData().getList(FRIENDS, Tag.TAG_STRING);
      if (friends.size() >= MAX_FRIENDS) {
         friends.remove(0);
      }

      friends.add(StringTag.valueOf(player.getStringUUID()));
      npc.getPersistentData().put(FRIENDS, friends);
   }

   /**
    * A player killed one of an army besieging a town of a faction he is not at war with: enough of them and the town
    * calls him its hero for a week, greets him so and sells to him cheaper.
    */
   public static void onKill(ServerLevel level, ServerPlayer player, FactionNpcEntity dead) {
      FactionWorldData data = FactionWorldData.get(level);
      for (FactionWorldData.Settlement town : data.settlements()) {
         if (town.faction().equals(dead.getFactionName())
            || !town.contains((int)dead.getX(), (int)dead.getZ(), HERO_RANGE)
            || !FactionTownLife.underAttack(data, town)
            || FactionStandingData.get(level).isHostile(player.getUUID(), town.faction())) {
            continue;
         }

         CompoundTag hero = player.getPersistentData().getCompound(HERO);
         long day = level.getDayTime() / FactionSimulation.DAY_TICKS;
         String kills = town.faction() + ".kills";
         String until = town.faction() + ".until";
         int count = hero.getLong(until) > day && hero.getInt(kills) >= HERO_KILLS ? HERO_KILLS : hero.getInt(kills) + 1;
         hero.putInt(kills, count);
         if (count == HERO_KILLS && hero.getLong(until) <= day) {
            hero.putLong(until, day + HERO_DAYS);
            FactionStandingData.get(level).adjustStanding(player.getUUID(), town.faction(), HERO_STANDING);
            player.sendSystemMessage(
               Component.translatable("townsfolk.ancientwarfare.hero.named", FactionNews.factionName(town.faction()), HERO_DAYS).withStyle(ChatFormatting.GOLD)
            );
         }

         player.getPersistentData().put(HERO, hero);
         return;
      }
   }

   public static boolean isHero(net.minecraft.world.entity.player.Player player, String faction) {
      CompoundTag hero = player.getPersistentData().getCompound(HERO);
      if (hero.getLong(faction + ".until") <= player.level().getDayTime() / FactionSimulation.DAY_TICKS) {
         if (hero.getInt(faction + ".kills") >= HERO_KILLS) {
            // The week is over: the next siege starts the count again.
            hero.putInt(faction + ".kills", 0);
         }

         return false;
      }

      return true;
   }

   /** What a faction's traders ask of a player, as a share of the usual. */
   public static double heroPrices(net.minecraft.world.entity.player.Player player, String faction) {
      return isHero(player, faction) ? HERO_PRICES : 1.0;
   }
}
