package net.shadowmage.ancientwarfare.core.research;

import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.research.ResearchResource;

/**
 * One technology. Besides the time and the resources it takes, a technology past the first steps
 * of the tree costs science: points the scribes gather at a research station, kept in the book.
 */
public record ResearchGoal(String id, String displayName, int time, Set<String> dependencies, List<ResearchResource> resourceRequirements, int science) {
   public Component translatedName() {
      return Component.translatable("research.ancientwarfare." + this.id);
   }

   public boolean canResearch(Set<String> completedResearch) {
      return completedResearch.containsAll(this.dependencies) && !completedResearch.contains(this.id);
   }

   ResearchGoal withScience(int science) {
      return new ResearchGoal(this.id, this.displayName, this.time, this.dependencies, this.resourceRequirements, science);
   }

   public List<ItemStack> resources() {
      return this.resourceRequirements.stream().map(ResearchResource::toStack).filter(stack -> !stack.isEmpty()).toList();
   }
}
