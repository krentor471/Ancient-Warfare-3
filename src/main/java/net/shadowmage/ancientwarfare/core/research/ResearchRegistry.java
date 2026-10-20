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
      return stack(BuiltInRegistries.f_257033_.m_7981_(item).toString(), count);
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
      register(goal("agriculture", 200, deps(), res(stack(Items.f_42398_, 24), stack(Items.f_42401_, 3), stack(Items.f_42405_, 4), stack(Items.f_42000_, 4))));
      register(goal("invention", 200, deps(), res(stack(Items.f_42398_, 12), stack(Items.f_42401_, 3), stack(Items.f_42413_, 4), stack(Items.f_42000_, 4))));
      register(goal("leadership", 200, deps(), res(stack(Items.f_42516_, 10), stack(Items.f_42398_, 24), stack(Items.f_42401_, 3), stack(Items.f_42000_, 4))));
      register(
         goal(
            "animal_husbandry",
            200,
            deps("agriculture"),
            res(stack(Items.f_42405_, 12), stack(Items.f_42454_, 4), stack(Items.f_42401_, 12), stack(Items.f_42000_, 4))
         )
      );
      register(
         goal(
            "engineering",
            200,
            deps("invention"),
            res(stack(Items.f_42516_, 20), stack(Items.f_42398_, 12), stack(Items.f_42413_, 4), stack(Items.f_42000_, 4))
         )
      );
      register(
         goal(
            "farming",
            200,
            deps("invention", "agriculture"),
            res(stack(Items.f_42405_, 12), stack(Items.f_42398_, 24), stack(Items.f_42401_, 3), stack(Items.f_42000_, 4))
         )
      );
      register(
         goal(
            "fishing", 200, deps("agriculture"), res(stack(Items.f_42526_, 5), stack(Items.f_42401_, 24), stack(Items.f_42398_, 12), stack(Items.f_42000_, 4))
         )
      );
      register(
         goal("command", 200, deps("leadership"), res(stack(Items.f_42468_, 1), stack(Items.f_42469_, 1), stack(Items.f_42470_, 1), stack(Items.f_42471_, 1)))
      );
      register(
         goal(
            "conscription",
            200,
            deps("leadership"),
            res(stack(Items.f_42516_, 12), stack(Items.f_42417_, 4), stack(Items.f_42383_, 1), stack(Items.f_42436_, 1))
         )
      );
      register(
         goal(
            "construction",
            200,
            deps("engineering"),
            res(stack(Items.f_42385_, 1), stack(Items.f_42401_, 4), stack(Items.f_42398_, 12), stack(Items.f_42416_, 4))
         )
      );
      register(
         goal(
            "mathematics",
            200,
            deps("engineering"),
            res(stack(Items.f_42517_, 3), stack(Items.f_42451_, 5), stack(Items.f_41859_, 1), stack(Items.f_42413_, 4))
         )
      );
      register(
         goal("mining", 200, deps("engineering"), res(stack(Items.f_42385_, 1), stack(Items.f_42520_, 1), stack(Items.f_42594_, 16), stack(Items.f_42000_, 8)))
      );
      register(
         goal(
            "the_wheel", 200, deps("engineering"), res(stack(Items.f_42155_, 1), stack(Items.f_42162_, 1), stack(Items.f_41905_, 4), stack(Items.f_42647_, 4))
         )
      );
      register(
         goal("chemistry", 200, deps("mining"), res(stack(Items.f_42451_, 32), stack(Items.f_42534_, 16), stack(Items.f_42417_, 2), stack(Items.f_42048_, 8)))
      );
      register(
         goal(
            "combustion",
            200,
            deps("mining", "the_wheel"),
            res(stack(Items.f_42413_, 10), stack(Items.f_42414_, 8), stack(Items.f_42416_, 5), stack(Items.f_42048_, 10))
         )
      );
      register(
         goal(
            "tactics",
            200,
            deps("conscription", "command"),
            res(stack(Items.f_42516_, 8), stack(Items.f_42413_, 4), stack(Items.f_42517_, 1), stack(Items.f_42401_, 4))
         )
      );
      register(
         goal(
            "theory_of_gravity",
            200,
            deps("the_wheel", "construction", "mathematics"),
            res(stack(Items.f_42416_, 8), stack(Items.f_42401_, 8), stack(Items.f_42517_, 1), stack(Items.f_42410_, 1))
         )
      );
      register(
         goal(
            "trade",
            200,
            deps("fishing", "command"),
            res(stack(Items.f_42417_, 8), stack(Items.f_42415_, 3), stack(Items.f_42616_, 1), stack(Items.f_42517_, 4))
         )
      );
      register(
         goal(
            "ballistics",
            200,
            deps("theory_of_gravity"),
            res(stack(Items.f_42401_, 6), stack(Items.f_42498_, 6), stack(Items.f_42516_, 12), stack(Items.f_42000_, 6))
         )
      );
      register(
         goal(
            "explosives",
            200,
            deps("combustion", "chemistry"),
            res(stack(Items.f_42413_, 12), stack(Items.f_42414_, 8), stack(Items.f_41830_, 10), stack(Items.f_42048_, 10))
         )
      );
      register(
         goal(
            "machinery",
            200,
            deps("theory_of_gravity"),
            res(stack("ancientwarfare:iron_gear", 3), stack(Items.f_42416_, 8), stack("ancientwarfare:iron_bearings", 2), stack(Items.f_42749_, 8))
         )
      );
      register(
         goal(
            "navigation",
            200,
            deps("theory_of_gravity", "seafaring"),
            res(stack(Items.f_42522_, 1), stack(Items.f_42676_, 1), stack(Items.f_42516_, 12), stack(Items.f_42526_, 4))
         )
      );
      register(
         goal(
            "refining", 200, deps("chemistry"), res(stack(Items.f_42416_, 12), stack(Items.f_42516_, 10), stack(Items.f_42417_, 2), stack(Items.f_42413_, 12))
         )
      );
      register(
         goal(
            "rocketry",
            200,
            deps("gunpowder", "ballistics"),
            res(stack(Items.f_42403_, 12), stack(Items.f_42416_, 6), stack(Items.f_42398_, 18), stack(Items.f_42516_, 12), stack(Items.f_42000_, 6))
         )
      );
      register(
         goal(
            "seafaring",
            200,
            deps("the_wheel", "mathematics", "trade"),
            res(stack(Items.f_42647_, 12), stack(Items.f_42416_, 8), tag("minecraft:wool", 10), stack(Items.f_42401_, 12))
         )
      );
      register(
         goal(
            "siege_warfare",
            200,
            deps("tactics", "the_wheel"),
            res(stack("ancientwarfare:treated_wood", 8), stack("ancientwarfare:mobility_unit", 1), stack(Items.f_42401_, 12), stack(Items.f_42516_, 6))
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
            res(stack("ancientwarfare:explosive_charge", 4), stack("ancientwarfare:iron_casing", 4), stack(Items.f_42413_, 12), stack(Items.f_42516_, 8))
         )
      );
      register(
         goal(
            "mass_production",
            200,
            deps("refining", "machinery"),
            res(stack("ancientwarfare:steel_ingot", 12), stack("ancientwarfare:steel_ingot", 6), stack(Items.f_42749_, 3), stack(Items.f_42416_, 3))
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
               stack(Items.f_42516_, 8)
            )
         )
      );
   }
}
