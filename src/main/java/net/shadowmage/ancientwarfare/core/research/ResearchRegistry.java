package net.shadowmage.ancientwarfare.core.research;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.shadowmage.ancientwarfare.core.common.research.ResearchResource;

public final class ResearchRegistry {
   private static final Map<String, ResearchGoal> GOALS = new LinkedHashMap<>();

   private ResearchRegistry() {
   }

   /** Science a technology costs by how deep in the tree it lies; the first two steps cost none. */
   private static final int[] SCIENCE_BY_DEPTH = {0, 0, 300, 500, 800, 1200, 1600, 2000};
   private static final int AUTO_SCIENCE = -1;

   private static ResearchGoal goal(String id, int time, Set<String> dependencies, List<ResearchResource> resources) {
      return goal(id, time, dependencies, resources, AUTO_SCIENCE);
   }

   private static ResearchGoal goal(String id, int time, Set<String> dependencies, List<ResearchResource> resources, int science) {
      return new ResearchGoal(id, title(id), time, dependencies, resources, science);
   }

   private static int depth(String id, Map<String, Integer> depths) {
      Integer known = depths.get(id);
      if (known != null) {
         return known;
      }

      depths.put(id, 0);
      ResearchGoal goal = GOALS.get(id);
      int depth = 0;
      if (goal != null) {
         for (String dependency : goal.dependencies()) {
            depth = Math.max(depth, depth(dependency, depths) + 1);
         }
      }

      depths.put(id, depth);
      return depth;
   }

   /** Gives every technology without a set price the science its depth in the tree calls for. */
   private static final Map<String, Integer> DEPTHS = new java.util.HashMap<>();
   /** Eras are technologies too: a big step everything past it waits on. */
   public static final String ERA_PREFIX = "era_";
   public static final String ERA_KINGDOM = "era_kingdom";
   public static final String ERA_EMPIRE = "era_empire";

   /**
    * Opens an era: the era needs the named technologies, and every technology that was as deep in
    * the tree as the era now is waits on the era as well.
    */
   private static void era(String id, int science, Set<String> needs, List<ResearchResource> resources) {
      int depth = 0;
      for (String need : needs) {
         depth = Math.max(depth, depth(need) + 1);
      }

      List<String> waiting = new java.util.ArrayList<>();
      for (ResearchGoal goal : GOALS.values()) {
         if (depth(goal.id()) == depth) {
            waiting.add(goal.id());
         }
      }

      register(goal(id, 400, needs, resources, science));
      for (String other : waiting) {
         GOALS.put(other, GOALS.get(other).withDependency(id));
      }

      DEPTHS.clear();
   }

   /** The era a technology belongs to, by the last era on the way to it: null before the first. */
   public static String eraOf(String id) {
      ResearchGoal empire = GOALS.get(ERA_EMPIRE);
      ResearchGoal kingdom = GOALS.get(ERA_KINGDOM);
      int depth = depth(id);
      if (empire != null && depth >= depth(ERA_EMPIRE)) {
         return ERA_EMPIRE;
      } else {
         return kingdom != null && depth >= depth(ERA_KINGDOM) ? ERA_KINGDOM : null;
      }
   }

   /** How many steps of research lie before this one: 0 for the roots of the tree. */
   public static int depth(String id) {
      return depth(id, DEPTHS);
   }

   private static void priceScience() {
      Map<String, Integer> depths = DEPTHS;
      for (Map.Entry<String, ResearchGoal> entry : GOALS.entrySet()) {
         if (entry.getValue().science() == AUTO_SCIENCE) {
            int depth = depth(entry.getKey(), depths);
            entry.setValue(entry.getValue().withScience(SCIENCE_BY_DEPTH[Math.min(depth, SCIENCE_BY_DEPTH.length - 1)]));
         }
      }
   }

   private static Set<String> deps(String... ids) {
      return Set.of(ids);
   }

   private static List<ResearchResource> res(ResearchResource... stacks) {
      return List.of(stacks);
   }

   private static ResearchResource stack(Item item, int count) {
      return stack(BuiltInRegistries.ITEM.getKey(item).toString(), count);
   }

   private static ResearchResource stack(String itemId, int count) {
      return new ResearchResource(itemId, count);
   }

   private static ResearchResource tag(String tagId, int count) {
      return new ResearchResource("#" + tagId, count);
   }

   private static String title(String id) {
      StringBuilder builder = new StringBuilder();

      for (String part : id.split("_")) {
         if (!part.isEmpty()) {
            if (builder.length() > 0) {
               builder.append(' ');
            }

            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
         }
      }

      return builder.toString();
   }

   public static void register(ResearchGoal goal) {
      if (goal != null && goal.id() != null && !goal.id().isBlank()) {
         GOALS.putIfAbsent(goal.id(), goal);
      }
   }

   public static ResearchGoal get(String id) {
      return GOALS.get(id);
   }

   public static Collection<ResearchGoal> all() {
      return GOALS.values();
   }

   public static ResearchGoal getByIndex(int index) {
      return index >= 0 && index < GOALS.size() ? GOALS.values().stream().skip((long)index).findFirst().orElse(null) : null;
   }

   static {
      register(goal("agriculture", 200, deps(), res(stack(Items.STICK, 24), stack(Items.STRING, 3), stack(Items.WHEAT, 4), stack(Items.TORCH, 4))));
      register(goal("invention", 200, deps(), res(stack(Items.STICK, 12), stack(Items.STRING, 3), stack(Items.COAL, 4), stack(Items.TORCH, 4))));
      register(goal("leadership", 200, deps(), res(stack(Items.PAPER, 10), stack(Items.STICK, 24), stack(Items.STRING, 3), stack(Items.TORCH, 4))));
      register(
         goal(
            "animal_husbandry",
            200,
            deps("agriculture"),
            res(stack(Items.WHEAT, 12), stack(Items.LEATHER, 4), stack(Items.STRING, 12), stack(Items.TORCH, 4))
         )
      );
      register(
         goal(
            "engineering",
            200,
            deps("invention"),
            res(stack(Items.PAPER, 20), stack(Items.STICK, 12), stack(Items.COAL, 4), stack(Items.TORCH, 4))
         )
      );
      register(
         goal(
            "farming",
            200,
            deps("invention", "agriculture"),
            res(stack(Items.WHEAT, 12), stack(Items.STICK, 24), stack(Items.STRING, 3), stack(Items.TORCH, 4))
         )
      );
      register(
         goal(
            "fishing", 200, deps("agriculture"), res(stack(Items.COD, 5), stack(Items.STRING, 24), stack(Items.STICK, 12), stack(Items.TORCH, 4))
         )
      );
      register(
         goal("command", 200, deps("leadership"), res(stack(Items.IRON_HELMET, 1), stack(Items.IRON_CHESTPLATE, 1), stack(Items.IRON_LEGGINGS, 1), stack(Items.IRON_BOOTS, 1)))
      );
      register(
         goal(
            "conscription",
            200,
            deps("leadership"),
            res(stack(Items.PAPER, 12), stack(Items.GOLD_INGOT, 4), stack(Items.IRON_SWORD, 1), stack(Items.GOLDEN_APPLE, 1))
         )
      );
      register(
         goal(
            "construction",
            200,
            deps("engineering"),
            res(stack(Items.IRON_PICKAXE, 1), stack(Items.STRING, 4), stack(Items.STICK, 12), stack(Items.IRON_INGOT, 4))
         )
      );
      register(
         goal(
            "mathematics",
            200,
            deps("engineering"),
            res(stack(Items.BOOK, 3), stack(Items.REDSTONE, 5), stack(Items.NOTE_BLOCK, 1), stack(Items.COAL, 4))
         )
      );
      register(
         goal("mining", 200, deps("engineering"), res(stack(Items.IRON_PICKAXE, 1), stack(Items.FURNACE_MINECART, 1), stack(Items.COBBLESTONE, 16), stack(Items.TORCH, 8)))
      );
      register(
         goal(
            "the_wheel", 200, deps("engineering"), res(stack(Items.HOPPER, 1), stack(Items.DROPPER, 1), stack(Items.STONE, 4), stack(Items.OAK_PLANKS, 4))
         )
      );
      register(
         goal("chemistry", 200, deps("mining"), res(stack(Items.REDSTONE, 32), stack(Items.LAPIS_LAZULI, 16), stack(Items.GOLD_INGOT, 2), stack(Items.NETHERRACK, 8)))
      );
      register(
         goal(
            "combustion",
            200,
            deps("mining", "the_wheel"),
            res(stack(Items.COAL, 10), stack(Items.CHARCOAL, 8), stack(Items.IRON_INGOT, 5), stack(Items.NETHERRACK, 10))
         )
      );
      register(
         goal(
            "tactics",
            200,
            deps("conscription", "command"),
            res(stack(Items.PAPER, 8), stack(Items.COAL, 4), stack(Items.BOOK, 1), stack(Items.STRING, 4))
         )
      );
      register(
         goal(
            "theory_of_gravity",
            200,
            deps("the_wheel", "construction", "mathematics"),
            res(stack(Items.IRON_INGOT, 8), stack(Items.STRING, 8), stack(Items.BOOK, 1), stack(Items.APPLE, 1))
         )
      );
      register(
         goal(
            "trade",
            200,
            deps("fishing", "command"),
            res(stack(Items.GOLD_INGOT, 8), stack(Items.DIAMOND, 3), stack(Items.EMERALD, 1), stack(Items.BOOK, 4))
         )
      );
      register(
         goal(
            "ballistics",
            200,
            deps("theory_of_gravity"),
            res(stack(Items.STRING, 6), stack(Items.BLACK_DYE, 6), stack(Items.PAPER, 12), stack(Items.TORCH, 6))
         )
      );
      register(
         goal(
            "explosives",
            200,
            deps("combustion", "chemistry"),
            res(stack(Items.COAL, 12), stack(Items.CHARCOAL, 8), stack(Items.SAND, 10), stack(Items.NETHERRACK, 10))
         )
      );
      register(
         goal(
            "machinery",
            200,
            deps("theory_of_gravity"),
            res(stack("ancientwarfare:iron_gear", 3), stack(Items.IRON_INGOT, 8), stack("ancientwarfare:iron_bearings", 2), stack(Items.IRON_NUGGET, 8))
         )
      );
      register(
         goal(
            "navigation",
            200,
            deps("theory_of_gravity", "seafaring"),
            res(stack(Items.COMPASS, 1), stack(Items.MAP, 1), stack(Items.PAPER, 12), stack(Items.COD, 4))
         )
      );
      register(
         goal(
            "refining", 200, deps("chemistry"), res(stack(Items.IRON_INGOT, 12), stack(Items.PAPER, 10), stack(Items.GOLD_INGOT, 2), stack(Items.COAL, 12))
         )
      );
      register(
         goal(
            "rocketry",
            200,
            deps("gunpowder", "ballistics"),
            res(stack(Items.GUNPOWDER, 12), stack(Items.IRON_INGOT, 6), stack(Items.STICK, 18), stack(Items.PAPER, 12), stack(Items.TORCH, 6))
         )
      );
      register(
         goal(
            "seafaring",
            200,
            deps("the_wheel", "mathematics", "trade"),
            res(stack(Items.OAK_PLANKS, 12), stack(Items.IRON_INGOT, 8), tag("minecraft:wool", 10), stack(Items.STRING, 12))
         )
      );
      register(
         goal(
            "siege_warfare",
            200,
            deps("tactics", "the_wheel"),
            res(stack("ancientwarfare:treated_wood", 8), stack("ancientwarfare:mobility_unit", 1), stack(Items.STRING, 12), stack(Items.PAPER, 6))
         )
      );
      register(
         goal(
            "advanced_siege_warfare",
            200,
            deps("siege_warfare", "machinery", "ballistics"),
            res(
               stack("ancientwarfare:torsion_unit", 2),
               stack("ancientwarfare:turret_components", 1),
               stack("ancientwarfare:iron_core_wood", 8),
               stack("ancientwarfare:tempered_iron", 6)
            )
         )
      );
      register(
         goal(
            "gunpowder",
            200,
            deps("explosives", "refining"),
            res(stack("ancientwarfare:explosive_charge", 4), stack("ancientwarfare:iron_casing", 4), stack(Items.COAL, 12), stack(Items.PAPER, 8))
         )
      );
      register(
         goal(
            "mass_production",
            200,
            deps("refining", "machinery"),
            res(stack("ancientwarfare:steel_ingot", 12), stack("ancientwarfare:steel_ingot", 6), stack(Items.IRON_NUGGET, 3), stack(Items.IRON_INGOT, 3))
         )
      );
      register(
         goal(
            "naval_warfare",
            200,
            deps("seafaring", "navigation", "siege_warfare"),
            res(
               stack("ancientwarfare:treated_wood", 12),
               stack("ancientwarfare:mobility_unit", 2),
               stack("ancientwarfare:turret_components", 1),
               stack(Items.PAPER, 8)
            )
         )
      );
      // The age of states: planning works, roads, commanding from above, treaties and spies.
      register(
         goal(
            PlayerResearch.SURVEYING,
            200,
            deps("construction", "mathematics"),
            res(stack(Items.PAPER, 12), stack(Items.STRING, 16), stack(Items.STICK, 16), stack(Items.COMPASS, 1))
         )
      );
      register(
         goal(
            PlayerResearch.ARCHITECTURE,
            200,
            deps(PlayerResearch.SURVEYING, "the_wheel"),
            res(stack(Items.BOOK, 4), stack(Items.STONE_BRICKS, 32), stack(Items.OAK_PLANKS, 32), stack(Items.IRON_INGOT, 6))
         )
      );
      register(
         goal(
            PlayerResearch.ROAD_BUILDING,
            200,
            deps(PlayerResearch.SURVEYING, "the_wheel"),
            res(stack(Items.GRAVEL, 32), stack(Items.COBBLESTONE, 32), stack(Items.IRON_SHOVEL, 1), stack(Items.STICK, 12)),
            600
         )
      );
      register(
         goal(
            PlayerResearch.FIELD_COMMAND,
            200,
            deps("tactics"),
            res(stack(Items.MAP, 1), stack(Items.PAPER, 16), stack(Items.BLACK_DYE, 4), stack(Items.GOLD_INGOT, 2))
         )
      );
      register(
         goal(
            PlayerResearch.DIPLOMACY,
            200,
            deps("leadership", "trade"),
            res(stack(Items.WRITABLE_BOOK, 1), stack(Items.PAPER, 16), stack(Items.GOLD_INGOT, 4), stack(Items.RED_DYE, 4))
         )
      );
      register(
         goal(
            PlayerResearch.ESPIONAGE,
            200,
            deps(PlayerResearch.DIPLOMACY, "tactics"),
            res(stack(Items.PAPER, 12), stack(Items.INK_SAC, 8), stack(Items.LEATHER, 6), stack(Items.MAP, 1))
         )
      );
      // Branches: economy, roads, maps, war, the secret service, faith and culture.
      register(goal(PlayerResearch.CROP_ROTATION, 200, deps("farming", "animal_husbandry"), res(stack(Items.WHEAT_SEEDS, 32), stack(Items.BONE_MEAL, 16), stack(Items.IRON_HOE, 1), stack(Items.WHEAT, 32))));
      register(goal(PlayerResearch.GRANARIES, 200, deps(PlayerResearch.CROP_ROTATION, "construction"), res(stack(Items.OAK_PLANKS, 48), stack(Items.HAY_BLOCK, 8), stack(Items.CHEST, 2), stack(Items.IRON_INGOT, 4))));
      register(goal(PlayerResearch.MARKETS, 200, deps("trade", PlayerResearch.CROP_ROTATION), res(stack(Items.GOLD_INGOT, 6), stack(Items.EMERALD, 2), stack(Items.CHEST, 2), stack(Items.PAPER, 8))));
      register(goal(PlayerResearch.COURIERS, 200, deps(PlayerResearch.ROAD_BUILDING, "animal_husbandry"), res(stack(Items.SADDLE, 1), stack(Items.LEATHER, 8), stack(Items.PAPER, 8), stack(Items.GOLD_INGOT, 2))));
      register(goal(PlayerResearch.CARTOGRAPHY, 200, deps(PlayerResearch.SURVEYING, "navigation"), res(stack(Items.MAP, 4), stack(Items.COMPASS, 2), stack(Items.PAPER, 24), stack(Items.INK_SAC, 8))));
      register(goal(PlayerResearch.DRILL, 200, deps("tactics", "conscription"), res(stack(Items.IRON_SWORD, 2), stack(Items.SHIELD, 2), stack(Items.PAPER, 8), stack(Items.LEATHER, 4))));
      register(goal(PlayerResearch.FIELD_CAMPS, 200, deps(PlayerResearch.DRILL, "construction"), res(stack(Items.CAMPFIRE, 2), tag("minecraft:wool", 12), stack(Items.LEATHER, 8), stack(Items.TORCH, 8))));
      register(goal(PlayerResearch.GARRISONS, 200, deps(PlayerResearch.DRILL, PlayerResearch.GRANARIES), res(stack(Items.IRON_CHESTPLATE, 1), stack(Items.SHIELD, 4), stack(Items.BREAD, 16), stack(Items.STONE_BRICKS, 32))));
      register(goal(PlayerResearch.CIPHERS, 200, deps(PlayerResearch.ESPIONAGE, "mathematics"), res(stack(Items.BOOK, 4), stack(Items.INK_SAC, 8), stack(Items.PAPER, 16), stack(Items.REDSTONE, 8))));
      register(goal(PlayerResearch.SECRET_SERVICE, 200, deps(PlayerResearch.CIPHERS), res(stack(Items.GOLD_INGOT, 12), stack(Items.ENDER_PEARL, 2), stack(Items.PAPER, 16), stack(Items.BLACK_DYE, 8))));
      register(goal(PlayerResearch.TEMPLES, 200, deps("leadership", "construction"), res(stack(Items.STONE_BRICKS, 32), stack(Items.GOLD_INGOT, 4), stack(Items.CANDLE, 4), stack(Items.BOOK, 2))));
      register(goal(PlayerResearch.FESTIVALS, 200, deps(PlayerResearch.TEMPLES, PlayerResearch.CROP_ROTATION), res(stack(Items.CAKE, 2), stack(Items.WHITE_BANNER, 2), stack(Items.FIREWORK_ROCKET, 8), stack(Items.WHEAT, 32))));
      register(goal(PlayerResearch.CHRONICLES, 200, deps(PlayerResearch.TEMPLES, "tactics"), res(stack(Items.BOOK, 6), stack(Items.INK_SAC, 8), stack(Items.FEATHER, 4), stack(Items.PAPER, 16))));
      register(goal(PlayerResearch.MISSIONARIES, 200, deps(PlayerResearch.TEMPLES, PlayerResearch.DIPLOMACY), res(stack(Items.BOOK, 4), stack(Items.BREAD, 16), stack(Items.GOLD_INGOT, 4), stack(Items.CANDLE, 4))));
      register(goal(PlayerResearch.BRIDGES, 200, deps(PlayerResearch.ROAD_BUILDING, "construction"), res(stack(Items.OAK_PLANKS, 48), stack(Items.STONE_BRICKS, 32), stack(Items.STRING, 16), stack(Items.IRON_INGOT, 4))));
      register(goal(PlayerResearch.POST_STATIONS, 200, deps(PlayerResearch.COURIERS, "trade"), res(stack(Items.WRITABLE_BOOK, 2), stack(Items.PAPER, 24), stack(Items.BELL, 1), stack(Items.LEATHER, 8))));
      register(goal(PlayerResearch.SEA_CHARTS, 200, deps(PlayerResearch.CARTOGRAPHY, "seafaring"), res(stack(Items.MAP, 6), stack(Items.COMPASS, 2), stack(Items.CLOCK, 1), stack(Items.INK_SAC, 8))));
      register(goal(PlayerResearch.LANDING, 200, deps("naval_warfare", PlayerResearch.FIELD_COMMAND), res(stack("ancientwarfare:treated_wood", 16), stack(Items.LEAD, 4), stack(Items.IRON_SWORD, 2), stack(Items.PAPER, 8))));
      priceScience();
      era(
         ERA_KINGDOM,
         1000,
         deps("construction", "mathematics", "trade", "tactics"),
         res(stack(Items.GOLD_INGOT, 8), stack(Items.BOOK, 6), stack(Items.WHITE_BANNER, 1), stack(Items.IRON_INGOT, 16))
      );
      era(
         ERA_EMPIRE,
         2500,
         deps("machinery", "refining", "navigation", PlayerResearch.ESPIONAGE),
         res(stack(Items.GOLD_BLOCK, 2), stack(Items.BOOK, 12), stack(Items.EMERALD, 4), stack(Items.DIAMOND, 2))
      );
   }
}
