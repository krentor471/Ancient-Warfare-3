package net.shadowmage.ancientwarfare.core.common.npc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

public final class FactionNpcDefaultsRegistry {
   private static final String RESOURCE_PATH = "/data/ancientwarfare/npc/faction_npc_defaults.json";
   private static final String FACTIONS_RESOURCE_PATH = "/data/ancientwarfare/npc/factions.json";
   private static final int DEFAULT_FACTION_COLOR = 8388736;
   private static final JsonObject EMPTY = new JsonObject();
   private static JsonObject root;
   private static JsonObject factionRoot;

   private FactionNpcDefaultsRegistry() {
   }

   public static List<String> factionNames() {
      JsonObject factions = factions();
      List<String> names = new ArrayList<>(factions.keySet());
      names.sort(Comparator.naturalOrder());
      return names;
   }

   public static int factionColor(String faction) {
      JsonObject factions = object(factionRoot(), "factions_by_name");
      JsonObject factionObject = object(factions, faction);
      JsonElement color = factionObject.get("color");
      if (color != null && color.isJsonPrimitive()) {
         try {
            return color.getAsJsonPrimitive().isNumber() ? color.getAsInt() & 16777215 : Integer.parseInt(color.getAsString().replace("#", ""), 16) & 16777215;
         } catch (NumberFormatException var5) {
            return 8388736;
         }
      } else {
         return 8388736;
      }
   }

   public static boolean isEnabled(String faction, FactionNpcRole role) {
      JsonObject defaults = defaults();
      JsonObject defaultSubtype = subtype(defaults, role);
      JsonObject factionObject = faction(faction);
      JsonObject factionSubtype = subtype(factionObject, role);
      return getBoolean(factionSubtype, "enabled")
         .or(() -> getBoolean(defaultSubtype, "enabled"))
         .or(() -> getBoolean(factionObject, "enabled"))
         .or(() -> getBoolean(defaults, "enabled"))
         .orElse(true);
   }

   public static EntityType<? extends Mob> mountType(String faction, FactionNpcRole role) {
      return mountType(additionalAttribute(faction, role, "horse_entity").orElse("net.minecraft.entity.passive.EntityHorse"));
   }

   public static void apply(Mob npc, String faction, FactionNpcRole role) {
      setAttribute(npc, faction, role, "generic.maxHealth", Attributes.MAX_HEALTH);
      setAttribute(npc, faction, role, "generic.movementSpeed", Attributes.MOVEMENT_SPEED);
      setAttribute(npc, faction, role, "generic.followRange", Attributes.FOLLOW_RANGE);
      setAttribute(npc, faction, role, "generic.attackDamage", Attributes.ATTACK_DAMAGE);
      setAttribute(npc, faction, role, "generic.armor", Attributes.ARMOR);
      setAttribute(npc, faction, role, "generic.armorToughness", Attributes.ARMOR_TOUGHNESS);
      npc.setHealth(npc.getMaxHealth());
      applyEquipment(npc, faction, role);
   }

   public static ItemStack stackForEquipmentId(String rawId) {
      String normalized = normalizeItemId(rawId);
      if (!normalized.isBlank() && !normalized.equals("minecraft:air")) {
         ResourceLocation id = ResourceLocation.tryParse(normalized);
         if (id == null) {
            return ItemStack.EMPTY;
         } else {
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
            return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   private static void setAttribute(Mob npc, String faction, FactionNpcRole role, String oldName, Attribute attribute) {
      attributeValue(faction, role, oldName).ifPresent(value -> {
         AttributeInstance instance = npc.getAttribute(attribute);
         if (instance != null) {
            instance.setBaseValue(value);
         }
      });
   }

   private static Optional<Double> attributeValue(String faction, FactionNpcRole role, String name) {
      JsonObject defaults = defaults();
      JsonObject defaultSubtype = subtype(defaults, role);
      JsonObject factionObject = faction(faction);
      JsonObject factionSubtype = subtype(factionObject, role);
      return getDouble(attributes(factionSubtype), name)
         .or(() -> getDouble(attributes(defaultSubtype), name))
         .or(() -> getDouble(attributes(factionObject), name))
         .or(() -> getDouble(attributes(defaults), name));
   }

   public static Optional<String> additionalAttribute(String faction, FactionNpcRole role, String name) {
      JsonObject defaults = defaults();
      JsonObject defaultSubtype = subtype(defaults, role);
      JsonObject factionObject = faction(faction);
      JsonObject factionSubtype = subtype(factionObject, role);
      return getString(additionalAttributes(factionSubtype), name)
         .or(() -> getString(additionalAttributes(defaultSubtype), name))
         .or(() -> getString(additionalAttributes(factionObject), name))
         .or(() -> getString(additionalAttributes(defaults), name));
   }

   public static Optional<ResourceLocation> lootTable(String faction, FactionNpcRole role) {
      JsonObject defaults = defaults();
      JsonObject defaultSubtype = subtype(defaults, role);
      JsonObject factionObject = faction(faction);
      JsonObject factionSubtype = subtype(factionObject, role);
      return getString(factionSubtype, "loot_table")
         .or(() -> getString(defaultSubtype, "loot_table"))
         .or(() -> getString(factionObject, "loot_table"))
         .or(() -> getString(defaults, "loot_table"))
         .<ResourceLocation>map(ResourceLocation::tryParse)
         .filter(Objects::nonNull);
   }

   private static EntityType<? extends Mob> mountType(String oldEntityName) {
      String normalized = oldEntityName == null ? "" : oldEntityName.trim();

      return switch (normalized) {
         case "net.minecraft.entity.passive.EntitySkeletonHorse" -> EntityType.SKELETON_HORSE;
         case "net.minecraft.entity.passive.EntityZombieHorse" -> EntityType.ZOMBIE_HORSE;
         case "net.minecraft.entity.passive.EntityLlama" -> EntityType.LLAMA;
         case "net.minecraft.entity.passive.EntityPig" -> EntityType.PIG;
         case "net.minecraft.entity.passive.EntityChicken" -> EntityType.CHICKEN;
         case "net.minecraft.entity.monster.EntitySpider" -> EntityType.SPIDER;
         case "net.minecraft.entity.monster.EntityPolarBear" -> EntityType.POLAR_BEAR;
         default -> EntityType.HORSE;
      };
   }

   private static void applyEquipment(Mob npc, String faction, FactionNpcRole role) {
      ItemStack mainhand = equipment(faction, role, "mainhand");
      ItemStack offhand = equipment(faction, role, "offhand");
      npc.setItemSlot(EquipmentSlot.MAINHAND, mainhand);
      npc.setItemSlot(EquipmentSlot.OFFHAND, offhand);
      if (!mainhand.isEmpty()) {
         npc.setItemInHand(InteractionHand.MAIN_HAND, mainhand);
      }

      if (!offhand.isEmpty()) {
         npc.setItemInHand(InteractionHand.OFF_HAND, offhand);
      }
   }

   private static ItemStack equipment(String faction, FactionNpcRole role, String slot) {
      JsonObject defaultEquipment = equipment(subtype(defaults(), role));
      JsonObject factionEquipment = equipment(subtype(faction(faction), role));
      String itemId = getString(factionEquipment, slot).or(() -> getString(defaultEquipment, slot)).orElse("minecraft:air");
      return stackForEquipmentId(itemId);
   }

   private static String normalizeItemId(String rawId) {
      String id = rawId == null ? "" : rawId.trim().toLowerCase(Locale.ROOT);
      if (id.equals("minecraft:skull")) {
         return "minecraft:skeleton_skull";
      } else {
         return id.startsWith("ancientwarfarenpc:") ? "ancientwarfare:" + id.substring("ancientwarfarenpc:".length()) : id;
      }
   }

   private static JsonObject defaults() {
      return object(root(), "defaults");
   }

   private static JsonObject factions() {
      return object(root(), "factions");
   }

   private static JsonObject faction(String faction) {
      return object(factions(), faction);
   }

   private static JsonObject subtype(JsonObject parent, FactionNpcRole role) {
      return object(object(parent, "npc_subtypes"), role.id());
   }

   private static JsonObject attributes(JsonObject object) {
      return object(object, "attributes");
   }

   private static JsonObject additionalAttributes(JsonObject object) {
      return object(object, "additional_attributes");
   }

   private static JsonObject equipment(JsonObject object) {
      return object(object, "equipment");
   }

   private static JsonObject object(JsonObject parent, String name) {
      JsonElement element = parent.get(name);
      return element != null && element.isJsonObject() ? element.getAsJsonObject() : EMPTY;
   }

   private static Optional<String> getString(JsonObject object, String name) {
      JsonElement element = object.get(name);
      return element != null && element.isJsonPrimitive() ? Optional.of(element.getAsString()) : Optional.empty();
   }

   private static Optional<Boolean> getBoolean(JsonObject object, String name) {
      JsonElement element = object.get(name);
      return element != null && element.isJsonPrimitive() ? Optional.of(element.getAsBoolean()) : Optional.empty();
   }

   private static Optional<Double> getDouble(JsonObject object, String name) {
      JsonElement element = object.get(name);
      return element != null && element.isJsonPrimitive() ? Optional.of(element.getAsDouble()) : Optional.empty();
   }

   private static JsonObject root() {
      if (root == null) {
         try (InputStream stream = FactionNpcDefaultsRegistry.class.getResourceAsStream("/data/ancientwarfare/npc/faction_npc_defaults.json")) {
            if (stream == null) {
               AncientWarfareCore.LOG.warn("Missing faction NPC defaults resource: {}", "/data/ancientwarfare/npc/faction_npc_defaults.json");
               root = new JsonObject();
            } else {
               root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            }
         } catch (Exception var5) {
            AncientWarfareCore.LOG.error("Failed to load faction NPC defaults", var5);
            root = new JsonObject();
         }
      }

      return root;
   }

   private static JsonObject factionRoot() {
      if (factionRoot == null) {
         try (InputStream stream = FactionNpcDefaultsRegistry.class.getResourceAsStream("/data/ancientwarfare/npc/factions.json")) {
            if (stream == null) {
               AncientWarfareCore.LOG.warn("Missing faction registry resource: {}", "/data/ancientwarfare/npc/factions.json");
               factionRoot = new JsonObject();
            } else {
               factionRoot = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
               buildFactionLookup();
            }
         } catch (Exception var5) {
            AncientWarfareCore.LOG.error("Failed to load faction registry", var5);
            factionRoot = new JsonObject();
         }
      }

      return factionRoot;
   }

   private static void buildFactionLookup() {
      JsonObject byName = new JsonObject();
      JsonElement factionsElement = factionRoot.get("factions");
      if (factionsElement != null && factionsElement.isJsonArray()) {
         for (JsonElement element : factionsElement.getAsJsonArray()) {
            if (element.isJsonObject()) {
               JsonObject faction = element.getAsJsonObject();
               getString(faction, "name").ifPresent(name -> byName.add(name, faction));
            }
         }
      }

      factionRoot.add("factions_by_name", byName);
   }
}
