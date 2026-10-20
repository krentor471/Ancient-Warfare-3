package net.shadowmage.ancientwarfare.core.common.npc;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;

public final class NpcDefinitionRegistry {
   public static final String WORKER = "worker";
   public static final String COMBAT = "combat";
   public static final String COURIER = "courier";
   public static final String TRADER = "trader";
   public static final String BARD = "bard";
   public static final String PRIEST = "priest";
   public static final String SIEGE_ENGINEER = "siege_engineer";
   public static final String MODERN_SOLDIER = "modern_soldier";
   public static final String SHIP_CAPTAIN = "ship_captain";
   public static final String MINER = "miner";
   public static final String FARMER = "farmer";
   public static final String LUMBERJACK = "lumberjack";
   public static final String RESEARCHER = "researcher";
   public static final String CRAFTSMAN = "craftsman";
   public static final String SOLDIER = "soldier";
   public static final String SWORDSMAN = "swordsman";
   public static final String ARCHER = "archer";
   public static final String COMMANDER = "commander";
   public static final String MEDIC = "medic";
   public static final String ENGINEER = "engineer";
   public static final String MAGE = "mage";
   private static final NpcDefinition WORKER_DEFINITION = new NpcDefinition(
      "worker",
      List.of(
         new NpcSubtype("miner", NpcDefinitionRegistry.BuiltInItemIds.IRON_PICKAXE, NpcWorkType.MINING),
         new NpcSubtype("farmer", NpcDefinitionRegistry.BuiltInItemIds.IRON_HOE, NpcWorkType.FARMING),
         new NpcSubtype("lumberjack", NpcDefinitionRegistry.BuiltInItemIds.IRON_AXE, NpcWorkType.FORESTRY),
         new NpcSubtype("researcher", AWCoreIds.IRON_QUILL, NpcWorkType.RESEARCH),
         new NpcSubtype("craftsman", AWCoreIds.IRON_HAMMER, NpcWorkType.CRAFTING)
      )
   );
   private static final NpcDefinition COMBAT_DEFINITION = new NpcDefinition(
      "combat",
      List.of(
         new NpcSubtype("soldier", NpcDefinitionRegistry.BuiltInItemIds.IRON_SWORD, NpcWorkType.NONE),
         new NpcSubtype("swordsman", new ResourceLocation("slashblade", "slashblade"), NpcWorkType.NONE),
         new NpcSubtype("archer", NpcDefinitionRegistry.BuiltInItemIds.BOW, NpcWorkType.NONE),
         new NpcSubtype("commander", AWCoreIds.IRON_COMMAND_BATON, NpcWorkType.NONE),
         new NpcSubtype("medic", NpcDefinitionRegistry.BuiltInItemIds.APPLE, NpcWorkType.NONE),
         new NpcSubtype("engineer", AWCoreIds.IRON_HAMMER, NpcWorkType.NONE)
      )
   );
   private static final NpcDefinition COURIER_DEFINITION = new NpcDefinition("courier", List.of());
   private static final NpcDefinition TRADER_DEFINITION = new NpcDefinition("trader", List.of());
   private static final NpcDefinition BARD_DEFINITION = new NpcDefinition("bard", List.of());
   private static final NpcDefinition PRIEST_DEFINITION = new NpcDefinition("priest", List.of());
   private static final NpcDefinition SIEGE_ENGINEER_DEFINITION = new NpcDefinition("siege_engineer", List.of());
   private static final NpcDefinition MODERN_SOLDIER_DEFINITION = new NpcDefinition("modern_soldier", List.of());
   private static final NpcDefinition SHIP_CAPTAIN_DEFINITION = new NpcDefinition("ship_captain", List.of());
   private static final List<NpcDefinition> DEFINITIONS = List.of(
      WORKER_DEFINITION,
      COMBAT_DEFINITION,
      COURIER_DEFINITION,
      TRADER_DEFINITION,
      BARD_DEFINITION,
      PRIEST_DEFINITION,
      SIEGE_ENGINEER_DEFINITION,
      MODERN_SOLDIER_DEFINITION,
      SHIP_CAPTAIN_DEFINITION
   );

   private NpcDefinitionRegistry() {
   }

   public static List<NpcDefinition> all() {
      return DEFINITIONS;
   }

   public static Optional<NpcDefinition> get(String id) {
      return DEFINITIONS.stream().filter(definition -> definition.id().equals(id)).findFirst();
   }

   public static Optional<NpcSubtype> getSubtype(String npcType, String npcSubtype) {
      return get(npcType).flatMap(definition -> definition.subtypes().stream().filter(subtype -> subtype.id().equals(npcSubtype)).findFirst());
   }

   private static final class BuiltInItemIds {
      private static final ResourceLocation IRON_PICKAXE = resourceId(Items.IRON_PICKAXE);
      private static final ResourceLocation IRON_HOE = resourceId(Items.IRON_HOE);
      private static final ResourceLocation IRON_AXE = resourceId(Items.IRON_AXE);
      private static final ResourceLocation IRON_SWORD = resourceId(Items.IRON_SWORD);
      private static final ResourceLocation BOW = resourceId(Items.BOW);
      private static final ResourceLocation APPLE = resourceId(Items.APPLE);

      private static ResourceLocation resourceId(Item item) {
         return BuiltInRegistries.ITEM.getKey(item);
      }
   }
}
