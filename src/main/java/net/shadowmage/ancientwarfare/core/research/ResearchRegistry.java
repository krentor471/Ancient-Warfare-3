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

   private static ResearchGoal goal(String id, int time, Set<String> dependencies, List<ResearchResource> resources) {
      return new ResearchGoal(id, title(id), time, dependencies, resources);
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
   }
}
