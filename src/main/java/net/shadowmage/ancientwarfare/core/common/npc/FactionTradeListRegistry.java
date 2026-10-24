package net.shadowmage.ancientwarfare.core.common.npc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.item.CoinItem;

public final class FactionTradeListRegistry {
   private static final String RESOURCE_PATH = "/data/ancientwarfare/npc/faction_trade_lists.json";
   public static final int MAX_VISIBLE_TRADES = 128;
   private static final int PLAYER_INVENTORY_SLOTS = 36;
   private static final int TRADE_GRID_SIZE = 9;
   private static final String COIN_ID = "ancientwarfare:coin";
   private static JsonObject root;

   private FactionTradeListRegistry() {
   }

   public static List<FactionTradeListRegistry.FactionTradeEntry> tradesFor(String faction) {
      List<FactionTradeListRegistry.FactionTradeEntry> trades = new ArrayList<>();
      JsonArray factionLists = array(object(root(), "faction_defaults"), faction);
      if (!factionLists.isEmpty()) {
         collectTrades(factionLists, trades);
      }

      if (trades.isEmpty()) {
         collectTrades(array(root(), "defaults"), trades);
      }

      return copyTrades(trades.stream().limit(128L).toList());
   }

   public static boolean performTrade(List<FactionTradeListRegistry.FactionTradeEntry> trades, int index, Player player) {
      return performTradeWithResult(trades, index, player) == FactionTradeListRegistry.TradeResult.SUCCESS;
   }

   public static FactionTradeListRegistry.TradeResult performTradeWithResult(List<FactionTradeListRegistry.FactionTradeEntry> trades, int index, Player player) {
      if (index >= 0 && index < trades.size()) {
         FactionTradeListRegistry.FactionTradeEntry trade = trades.get(index);
         FactionTradeListRegistry.TradeResult result = transactionResult(player, trade);
         if (result != FactionTradeListRegistry.TradeResult.SUCCESS) {
            return result;
         } else {
            removeInputs(player, trade.inputs());
            addOutputs(player, trade.outputs());
            player.getInventory().setChanged();
            return FactionTradeListRegistry.TradeResult.SUCCESS;
         }
      } else {
         return FactionTradeListRegistry.TradeResult.INVALID_TRADE_INDEX;
      }
   }

   public static boolean canTrade(List<FactionTradeListRegistry.FactionTradeEntry> trades, int index, Player player) {
      return index >= 0 && index < trades.size() && transactionResult(player, trades.get(index)) == FactionTradeListRegistry.TradeResult.SUCCESS;
   }

   public static List<FactionTradeListRegistry.FactionTradeEntry> readLegacyTradeList(CompoundTag rootTag) {
      if (!rootTag.contains("tradeList", 10)) {
         return List.of();
      } else {
         CompoundTag tradeListTag = rootTag.getCompound("tradeList");
         if (!tradeListTag.contains("tradeList", 9)) {
            return List.of();
         } else {
            List<FactionTradeListRegistry.FactionTradeEntry> trades = new ArrayList<>();
            ListTag entries = tradeListTag.getList("tradeList", 10);

            for (int i = 0; i < entries.size() && trades.size() < 128; i++) {
               CompoundTag entry = entries.getCompound(i);
               List<ItemStack> inputs = readLegacyStacks(entry, "inputItems");
               List<ItemStack> outputs = readLegacyStacks(entry, "outputItems");
               if (containsStack(inputs) && containsStack(outputs)) {
                  trades.add(new FactionTradeListRegistry.FactionTradeEntry(inputs, outputs));
               }
            }

            return trades;
         }
      }
   }

   public static List<FactionTradeListRegistry.FactionTradeEntry> readSavedTrades(CompoundTag rootTag, String key) {
      if (!rootTag.contains(key, 9)) {
         return List.of();
      } else {
         List<FactionTradeListRegistry.FactionTradeEntry> trades = new ArrayList<>();
         ListTag entries = rootTag.getList(key, 10);

         for (int i = 0; i < entries.size() && trades.size() < 128; i++) {
            CompoundTag entry = entries.getCompound(i);
            List<ItemStack> inputs = readSavedStacks(entry, "inputs");
            List<ItemStack> outputs = readSavedStacks(entry, "outputs");
            if (containsStack(inputs) && containsStack(outputs)) {
               trades.add(new FactionTradeListRegistry.FactionTradeEntry(inputs, outputs));
            }
         }

         return trades;
      }
   }

   public static List<FactionTradeListRegistry.FactionTradeEntry> repairSavedGoldenAppleTrades(List<FactionTradeListRegistry.FactionTradeEntry> trades) {
      List<FactionTradeListRegistry.FactionTradeEntry> repaired = new ArrayList<>(copyTrades(trades));

      for (int i = 0; i < repaired.size(); i++) {
         FactionTradeListRegistry.GoldenAppleTrade candidate = goldenAppleTrade(repaired.get(i));
         if (candidate != null) {
            int lowestPrice = candidate.coinCount();
            int highestPrice = candidate.coinCount();

            for (FactionTradeListRegistry.FactionTradeEntry otherTrade : repaired) {
               FactionTradeListRegistry.GoldenAppleTrade other = goldenAppleTrade(otherTrade);
               if (other != null && candidate.sameProductAndCurrency(other)) {
                  lowestPrice = Math.min(lowestPrice, other.coinCount());
                  highestPrice = Math.max(highestPrice, other.coinCount());
               }
            }

            if (lowestPrice < highestPrice && candidate.coinCount() == highestPrice) {
               List<ItemStack> outputs = new ArrayList<>(repaired.get(i).outputs());
               outputs.set(candidate.outputSlot(), new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, candidate.outputCount()));
               repaired.set(i, new FactionTradeListRegistry.FactionTradeEntry(repaired.get(i).inputs(), outputs));
            }
         }
      }

      return repaired;
   }

   public static void writeSavedTrades(CompoundTag rootTag, String key, List<FactionTradeListRegistry.FactionTradeEntry> trades) {
      ListTag entries = new ListTag();
      trades.stream().limit(128L).forEach(trade -> {
         CompoundTag entry = new CompoundTag();
         writeSavedStacks(entry, "inputs", trade.inputs());
         writeSavedStacks(entry, "outputs", trade.outputs());
         entries.add(entry);
      });
      rootTag.put(key, entries);
   }

   public static void writeTrades(FriendlyByteBuf buffer, List<FactionTradeListRegistry.FactionTradeEntry> trades) {
      int count = Math.min(128, trades.size());
      buffer.writeVarInt(count);

      for (int i = 0; i < count; i++) {
         writeStacks(buffer, trades.get(i).inputs());
         writeStacks(buffer, trades.get(i).outputs());
      }
   }

   public static List<FactionTradeListRegistry.FactionTradeEntry> readTrades(FriendlyByteBuf buffer) {
      int count = Math.min(128, Math.max(0, buffer.readVarInt()));
      List<FactionTradeListRegistry.FactionTradeEntry> trades = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         List<ItemStack> inputs = readStacks(buffer);
         List<ItemStack> outputs = readStacks(buffer);
         if (containsStack(inputs) && containsStack(outputs)) {
            trades.add(new FactionTradeListRegistry.FactionTradeEntry(inputs, outputs));
         }
      }

      return trades;
   }

   public static List<FactionTradeListRegistry.FactionTradeEntry> copyTrades(List<FactionTradeListRegistry.FactionTradeEntry> trades) {
      return trades.stream().map(trade -> new FactionTradeListRegistry.FactionTradeEntry(copyStacks(trade.inputs()), copyStacks(trade.outputs()))).toList();
   }

   private static void collectTrades(JsonArray templates, List<FactionTradeListRegistry.FactionTradeEntry> trades) {
      for (JsonElement templateElement : templates) {
         if (templateElement.isJsonObject()) {
            for (JsonElement entryElement : array(templateElement.getAsJsonObject(), "trades")) {
               parseTrade(entryElement).ifPresent(trades::add);
            }
         }
      }
   }

   private static Optional<FactionTradeListRegistry.FactionTradeEntry> parseTrade(JsonElement element) {
      if (!element.isJsonObject()) {
         return Optional.empty();
      } else {
         JsonObject object = element.getAsJsonObject();
         List<ItemStack> inputs = parseStacks(array(object, "input"));
         List<ItemStack> outputs = parseStacks(array(object, "output"));
         return containsStack(inputs) && containsStack(outputs)
            ? Optional.of(new FactionTradeListRegistry.FactionTradeEntry(inputs, outputs))
            : Optional.empty();
      }
   }

   private static List<ItemStack> parseStacks(JsonArray array) {
      List<ItemStack> stacks = new ArrayList<>();

      for (JsonElement element : array) {
         if (!element.isJsonObject()) {
            return List.of();
         }

         ItemStack stack = parseStack(element.getAsJsonObject());
         if (stack.isEmpty()) {
            return List.of();
         }

         stacks.add(stack);
      }

      return stacks;
   }

   private static ItemStack parseStack(JsonObject object) {
      int data = getInt(object, "data").orElse(0);
      String rawName = getString(object, "name").orElse("");
      String legacyNbt = getString(object, "nbt").orElse("");
      if ("minecraft:spawn_egg".equalsIgnoreCase(rawName)) {
         rawName = spawnEggId(legacyNbt);
      }

      String resolvedName = rawName;
      ItemStack stack = createLegacyStack(rawName, data, Math.max(1, getInt(object, "count").orElse(1)));
      if (stack.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         Optional.of(legacyNbt).filter(nbt -> !nbt.isBlank()).ifPresent(nbt -> {
            try {
               applyCompatibleTag(stack, TagParser.parseTag(nbt));
            } catch (Exception var4x) {
               AncientWarfareCore.LOG.debug("Ignoring invalid legacy trade NBT for {}: {}", resolvedName, nbt);
            }
         });
         return stack;
      }
   }

   private static List<ItemStack> readLegacyStacks(CompoundTag trade, String key) {
      if (!trade.contains(key, 9)) {
         return List.of();
      } else {
         List<ItemStack> stacks = emptyGrid();
         ListTag list = trade.getList(key, 10);

         for (int i = 0; i < list.size(); i++) {
            CompoundTag oldStack = list.getCompound(i);
            String rawId = oldStack.getString("id");
            if ("minecraft:spawn_egg".equalsIgnoreCase(rawId) && oldStack.contains("tag", 10)) {
               CompoundTag itemTag = oldStack.getCompound("tag");
               if (itemTag.contains("EntityTag", 10)) {
                  String entity = itemTag.getCompound("EntityTag").getString("id");
                  if (!entity.isBlank()) {
                     rawId = entity + "_spawn_egg";
                  }
               }
            }

            int data = oldStack.contains("Damage", 99) ? oldStack.getInt("Damage") : 0;
            int count = oldStack.contains("Count", 99) ? oldStack.getInt("Count") : 1;
            ItemStack stack = createLegacyStack(rawId, data, Math.max(1, count));
            if (stack.isEmpty()) {
               return List.of();
            }

            if (oldStack.contains("tag", 10)) {
               applyCompatibleTag(stack, oldStack.getCompound("tag"));
            }

            int slot = oldStack.contains("slot", 99) ? oldStack.getInt("slot") : i;
            putInGrid(stacks, slot, stack);
         }

         return stacks;
      }
   }

   private static ItemStack createLegacyStack(String rawId, int data, int count) {
      ResourceLocation id = ResourceLocation.tryParse(normalizeItemId(rawId, data));
      if (id == null) {
         return ItemStack.EMPTY;
      } else {
         Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
         return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
      }
   }

   private static void applyCompatibleTag(ItemStack stack, CompoundTag source) {
      CompoundTag tag = source.copy();
      ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
      if ("ancientwarfare:coin".equals(itemId.toString())) {
         String metal = tag.getString("metal").toLowerCase(Locale.ROOT);
         tag = new CompoundTag();

         tag.putString("metal", switch (metal) {
            case "silver", "gold" -> metal;
            default -> "copper";
         });
      }

      if (!tag.isEmpty()) {
         stack.setTag(tag);
      }
   }

   private static FactionTradeListRegistry.TradeResult transactionResult(Player player, FactionTradeListRegistry.FactionTradeEntry trade) {
      List<ItemStack> simulation = copyPlayerInventory(player.getInventory());
      if (!consumeSimulated(simulation, trade.inputs())) {
         return FactionTradeListRegistry.TradeResult.MISSING_INPUTS;
      } else {
         return insertAllSimulated(simulation, trade.outputs())
            ? FactionTradeListRegistry.TradeResult.SUCCESS
            : FactionTradeListRegistry.TradeResult.INVENTORY_FULL;
      }
   }

   private static List<ItemStack> copyPlayerInventory(Inventory inventory) {
      List<ItemStack> simulation = new ArrayList<>(36);

      for (int slot = 0; slot < Math.min(36, inventory.getContainerSize()); slot++) {
         simulation.add(inventory.getItem(slot).copy());
      }

      while (simulation.size() < 36) {
         simulation.add(ItemStack.EMPTY);
      }

      return simulation;
   }

   private static boolean consumeSimulated(List<ItemStack> inventory, List<ItemStack> inputs) {
      for (ItemStack input : inputs) {
         if (!input.isEmpty()) {
            int remaining = input.getCount();

            for (ItemStack stack : inventory) {
               if (matchesTradeStack(stack, input)) {
                  int removed = Math.min(remaining, stack.getCount());
                  stack.shrink(removed);
                  remaining -= removed;
                  if (remaining <= 0) {
                     break;
                  }
               }
            }

            if (remaining > 0) {
               return false;
            }
         }
      }

      return true;
   }

   private static boolean insertAllSimulated(List<ItemStack> inventory, List<ItemStack> outputs) {
      for (ItemStack output : outputs) {
         if (!output.isEmpty() && !insertSimulated(inventory, output.copy())) {
            return false;
         }
      }

      return true;
   }

   private static boolean insertSimulated(List<ItemStack> inventory, ItemStack stack) {
      for (ItemStack existing : inventory) {
         if (matchesTradeStack(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
            int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
            existing.grow(moved);
            stack.shrink(moved);
            if (stack.isEmpty()) {
               return true;
            }
         }
      }

      for (int i = 0; i < inventory.size(); i++) {
         if (inventory.get(i).isEmpty()) {
            int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
            ItemStack inserted = stack.copy();
            inserted.setCount(moved);
            inventory.set(i, inserted);
            stack.shrink(moved);
            if (stack.isEmpty()) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean matchesTradeStack(ItemStack stack, ItemStack required) {
      if (!stack.isEmpty() && !required.isEmpty() && stack.is(required.getItem())) {
         ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(required.getItem());
         return "ancientwarfare:coin".equals(itemId.toString())
            ? CoinItem.getMetal(stack) == CoinItem.getMetal(required)
            : ItemStack.isSameItemSameTags(stack, required);
      } else {
         return false;
      }
   }

   private static void removeInputs(Player player, List<ItemStack> inputs) {
      for (ItemStack input : inputs) {
         if (!input.isEmpty()) {
            int remaining = input.getCount();

            for (int slot = 0; slot < Math.min(36, player.getInventory().getContainerSize()) && remaining > 0; slot++) {
               ItemStack stack = player.getInventory().getItem(slot);
               if (matchesTradeStack(stack, input)) {
                  int removed = Math.min(remaining, stack.getCount());
                  stack.shrink(removed);
                  remaining -= removed;
               }
            }
         }
      }
   }

   private static void addOutputs(Player player, List<ItemStack> outputs) {
      for (ItemStack output : outputs) {
         if (!output.isEmpty()) {
            player.getInventory().add(output.copy());
         }
      }
   }

   private static String normalizeItemId(String rawId, int data) {
      String id = rawId == null ? "" : rawId.trim().toLowerCase(Locale.ROOT);
      if (id.startsWith("ancientwarfarenpc:")) {
         id = "ancientwarfare:" + id.substring("ancientwarfarenpc:".length());
      } else if (id.startsWith("ancientwarfarestructure:")) {
         id = "ancientwarfare:" + id.substring("ancientwarfarestructure:".length());
      }
      return switch (id) {
         case "minecraft:golden_apple" -> data == 1 ? "minecraft:enchanted_golden_apple" : "minecraft:golden_apple";
         case "minecraft:log" -> {
            switch (data) {
               case 1:
                  yield "minecraft:spruce_log";
               case 2:
                  yield "minecraft:birch_log";
               case 3:
                  yield "minecraft:jungle_log";
               default:
                  yield "minecraft:oak_log";
            }
         }
         case "minecraft:log2" -> data == 1 ? "minecraft:dark_oak_log" : "minecraft:acacia_log";
         case "minecraft:planks" -> woodId(data, "_planks");
         case "minecraft:sapling" -> woodId(data, "_sapling");
         case "minecraft:leaves" -> woodId(Math.min(data & 3, 3), "_leaves");
         case "minecraft:leaves2" -> woodId(4 + Math.min(data & 1, 1), "_leaves");
         case "minecraft:fish" -> fishId(data, false);
         case "minecraft:cooked_fish" -> fishId(data, true);
         case "minecraft:bed" -> colorId(data, "_bed");
         case "minecraft:wool" -> colorId(data, "_wool");
         case "minecraft:stained_hardened_clay" -> colorId(data, "_terracotta");
         case "minecraft:hardened_clay" -> "minecraft:terracotta";
         case "minecraft:stained_glass" -> colorId(data, "_stained_glass");
         case "minecraft:stained_glass_pane" -> colorId(data, "_stained_glass_pane");
         case "minecraft:concrete" -> colorId(data, "_concrete");
         case "minecraft:concrete_powder" -> colorId(data, "_concrete_powder");
         case "minecraft:dye" -> dyeId(data);
         case "minecraft:red_flower" -> redFlowerId(data);
         case "minecraft:yellow_flower" -> "minecraft:dandelion";
         case "minecraft:double_plant" -> doublePlantId(data);
         case "minecraft:reeds" -> "minecraft:sugar_cane";
         case "minecraft:web" -> "minecraft:cobweb";
         case "minecraft:waterlily" -> "minecraft:lily_pad";
         case "minecraft:brick_block" -> "minecraft:bricks";
         case "minecraft:nether_brick" -> "minecraft:nether_bricks";
         case "minecraft:stonebrick" -> {
            switch (data) {
               case 1:
                  yield "minecraft:mossy_stone_bricks";
               case 2:
                  yield "minecraft:cracked_stone_bricks";
               case 3:
                  yield "minecraft:chiseled_stone_bricks";
               default:
                  yield "minecraft:stone_bricks";
            }
         }
         case "minecraft:stone" -> stoneId(data);
         case "minecraft:sandstone" -> {
            switch (data) {
               case 1:
                  yield "minecraft:chiseled_sandstone";
               case 2:
                  yield "minecraft:cut_sandstone";
               default:
                  yield "minecraft:sandstone";
            }
         }
         case "minecraft:skull" -> skullId(data);
         case "minecraft:wooden_axe" -> "minecraft:wooden_axe";
         case "minecraft:wooden_hoe" -> "minecraft:wooden_hoe";
         case "minecraft:wooden_pickaxe" -> "minecraft:wooden_pickaxe";
         case "minecraft:wooden_shovel" -> "minecraft:wooden_shovel";
         case "minecraft:wooden_door" -> "minecraft:oak_door";
         case "minecraft:boat" -> "minecraft:oak_boat";
         case "minecraft:speckled_melon" -> "minecraft:glistering_melon_slice";
         default -> id;
      };
   }

   private static String spawnEggId(String nbt) {
      try {
         CompoundTag tag = TagParser.parseTag(nbt);
         if (tag.contains("EntityTag", 10)) {
            String entity = tag.getCompound("EntityTag").getString("id");
            if (!entity.isBlank()) {
               return entity + "_spawn_egg";
            }
         }
      } catch (Exception var3) {
      }

      return "minecraft:pig_spawn_egg";
   }

   private static String woodId(int data, String suffix) {
      String wood = switch (data) {
         case 1 -> "spruce";
         case 2 -> "birch";
         case 3 -> "jungle";
         case 4 -> "acacia";
         case 5 -> "dark_oak";
         default -> "oak";
      };
      return "minecraft:" + wood + suffix;
   }

   private static String fishId(int data, boolean cooked) {
      if (cooked) {
         return data == 1 ? "minecraft:cooked_salmon" : "minecraft:cooked_cod";
      } else {
         return switch (data) {
            case 1 -> "minecraft:salmon";
            case 2 -> "minecraft:tropical_fish";
            case 3 -> "minecraft:pufferfish";
            default -> "minecraft:cod";
         };
      }
   }

   private static String colorId(int data, String suffix) {
      String color = switch (data) {
         case 1 -> "orange";
         case 2 -> "magenta";
         case 3 -> "light_blue";
         case 4 -> "yellow";
         case 5 -> "lime";
         case 6 -> "pink";
         case 7 -> "gray";
         case 8 -> "light_gray";
         case 9 -> "cyan";
         case 10 -> "purple";
         case 11 -> "blue";
         case 12 -> "brown";
         case 13 -> "green";
         case 14 -> "red";
         case 15 -> "black";
         default -> "white";
      };
      return "minecraft:" + color + suffix;
   }

   private static String dyeId(int data) {
      return switch (data) {
         case 0 -> "minecraft:black_dye";
         case 1 -> "minecraft:red_dye";
         case 2 -> "minecraft:green_dye";
         case 3 -> "minecraft:brown_dye";
         case 4 -> "minecraft:blue_dye";
         case 5 -> "minecraft:purple_dye";
         case 6 -> "minecraft:cyan_dye";
         case 7 -> "minecraft:light_gray_dye";
         case 8 -> "minecraft:gray_dye";
         case 9 -> "minecraft:pink_dye";
         case 10 -> "minecraft:lime_dye";
         case 11 -> "minecraft:yellow_dye";
         case 12 -> "minecraft:light_blue_dye";
         case 13 -> "minecraft:magenta_dye";
         case 14 -> "minecraft:orange_dye";
         default -> "minecraft:white_dye";
      };
   }

   private static String redFlowerId(int data) {
      return switch (data) {
         case 1 -> "minecraft:blue_orchid";
         case 2 -> "minecraft:allium";
         case 3 -> "minecraft:azure_bluet";
         case 4 -> "minecraft:red_tulip";
         case 5 -> "minecraft:orange_tulip";
         case 6 -> "minecraft:white_tulip";
         case 7 -> "minecraft:pink_tulip";
         case 8 -> "minecraft:oxeye_daisy";
         default -> "minecraft:poppy";
      };
   }

   private static String doublePlantId(int data) {
      return switch (data) {
         case 1 -> "minecraft:lilac";
         case 2 -> "minecraft:tall_grass";
         case 3 -> "minecraft:large_fern";
         case 4 -> "minecraft:rose_bush";
         case 5 -> "minecraft:peony";
         default -> "minecraft:sunflower";
      };
   }

   private static String stoneId(int data) {
      return switch (data) {
         case 1 -> "minecraft:granite";
         case 2 -> "minecraft:polished_granite";
         case 3 -> "minecraft:diorite";
         case 4 -> "minecraft:polished_diorite";
         case 5 -> "minecraft:andesite";
         case 6 -> "minecraft:polished_andesite";
         default -> "minecraft:stone";
      };
   }

   private static String skullId(int data) {
      return switch (data) {
         case 1 -> "minecraft:wither_skeleton_skull";
         case 2 -> "minecraft:zombie_head";
         case 3 -> "minecraft:player_head";
         case 4 -> "minecraft:creeper_head";
         case 5 -> "minecraft:dragon_head";
         default -> "minecraft:skeleton_skull";
      };
   }

   private static void writeSavedStacks(CompoundTag parent, String key, List<ItemStack> stacks) {
      ListTag list = new ListTag();

      for (int slot = 0; slot < Math.min(9, stacks.size()); slot++) {
         ItemStack stack = stacks.get(slot);
         if (!stack.isEmpty()) {
            CompoundTag stackTag = stack.save(new CompoundTag());
            stackTag.putInt("slot", slot);
            list.add(stackTag);
         }
      }

      parent.put(key, list);
   }

   private static List<ItemStack> readSavedStacks(CompoundTag parent, String key) {
      List<ItemStack> stacks = emptyGrid();
      ListTag list = parent.getList(key, 10);

      for (int i = 0; i < list.size(); i++) {
         CompoundTag stackTag = list.getCompound(i);
         putInGrid(stacks, stackTag.getInt("slot"), ItemStack.of(stackTag));
      }

      return stacks;
   }

   private static void writeStacks(FriendlyByteBuf buffer, List<ItemStack> stacks) {
      int count = Math.min(9, stacks.size());
      buffer.writeVarInt(count);

      for (int i = 0; i < count; i++) {
         buffer.writeItem(stacks.get(i));
      }
   }

   private static List<ItemStack> readStacks(FriendlyByteBuf buffer) {
      int count = Math.min(9, Math.max(0, buffer.readVarInt()));
      List<ItemStack> stacks = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         stacks.add(buffer.readItem());
      }

      return stacks;
   }

   private static List<ItemStack> emptyGrid() {
      List<ItemStack> stacks = new ArrayList<>(9);

      for (int i = 0; i < 9; i++) {
         stacks.add(ItemStack.EMPTY);
      }

      return stacks;
   }

   private static void putInGrid(List<ItemStack> grid, int preferredSlot, ItemStack stack) {
      if (!stack.isEmpty()) {
         if (preferredSlot >= 0 && preferredSlot < grid.size() && grid.get(preferredSlot).isEmpty()) {
            grid.set(preferredSlot, stack);
         } else {
            for (int i = 0; i < grid.size(); i++) {
               if (grid.get(i).isEmpty()) {
                  grid.set(i, stack);
                  return;
               }
            }
         }
      }
   }

   private static boolean containsStack(List<ItemStack> stacks) {
      return stacks.stream().anyMatch(stack -> !stack.isEmpty());
   }

   private static FactionTradeListRegistry.GoldenAppleTrade goldenAppleTrade(FactionTradeListRegistry.FactionTradeEntry trade) {
      int outputSlot = -1;
      int outputCount = 0;

      for (int slot = 0; slot < trade.outputs().size(); slot++) {
         ItemStack output = trade.outputs().get(slot);
         if (!output.isEmpty()) {
            if (outputSlot >= 0 || !output.is(Items.GOLDEN_APPLE)) {
               return null;
            }

            outputSlot = slot;
            outputCount = output.getCount();
         }
      }

      if (outputSlot < 0) {
         return null;
      } else {
         String coinMetal = null;
         int coinCount = 0;

         for (ItemStack input : trade.inputs()) {
            if (!input.isEmpty()) {
               ResourceLocation inputId = BuiltInRegistries.ITEM.getKey(input.getItem());
               if (!"ancientwarfare:coin".equals(inputId.toString())) {
                  return null;
               }

               String metal = input.hasTag() ? input.getTag().getString("metal").toLowerCase(Locale.ROOT) : "copper";
               if (coinMetal != null && !coinMetal.equals(metal)) {
                  return null;
               }

               coinMetal = metal;
               coinCount += input.getCount();
            }
         }

         return coinMetal != null && coinCount > 0 ? new FactionTradeListRegistry.GoldenAppleTrade(outputSlot, outputCount, coinMetal, coinCount) : null;
      }
   }

   private static List<ItemStack> copyStacks(List<ItemStack> source) {
      return source.stream().<ItemStack>map(ItemStack::copy).toList();
   }

   private static JsonArray array(JsonObject parent, String name) {
      JsonElement element = parent.get(name);
      return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
   }

   private static JsonObject object(JsonObject parent, String name) {
      JsonElement element = parent.get(name);
      return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
   }

   private static Optional<String> getString(JsonObject object, String name) {
      JsonElement element = object.get(name);
      return element != null && element.isJsonPrimitive() ? Optional.of(element.getAsString()) : Optional.empty();
   }

   private static Optional<Integer> getInt(JsonObject object, String name) {
      JsonElement element = object.get(name);
      return element != null && element.isJsonPrimitive() ? Optional.of(element.getAsInt()) : Optional.empty();
   }

   private static JsonObject root() {
      if (root == null) {
         try (InputStream stream = FactionTradeListRegistry.class.getResourceAsStream("/data/ancientwarfare/npc/faction_trade_lists.json")) {
            if (stream == null) {
               AncientWarfareCore.LOG.warn("Missing faction trade list resource: {}", "/data/ancientwarfare/npc/faction_trade_lists.json");
               root = new JsonObject();
            } else {
               root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            }
         } catch (Exception var5) {
            AncientWarfareCore.LOG.error("Failed to load faction trade lists", var5);
            root = new JsonObject();
         }
      }

      return root;
   }

   public static record FactionTradeEntry(List<ItemStack> inputs, List<ItemStack> outputs) {
   }

   private static record GoldenAppleTrade(int outputSlot, int outputCount, String coinMetal, int coinCount) {
      private boolean sameProductAndCurrency(FactionTradeListRegistry.GoldenAppleTrade other) {
         return this.outputCount == other.outputCount && this.coinMetal.equals(other.coinMetal);
      }
   }

   public static enum TradeResult {
      SUCCESS,
      INVALID_TRADE_INDEX,
      MISSING_INPUTS,
      INVENTORY_FULL;
   }
}
