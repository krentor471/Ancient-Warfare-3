package net.shadowmage.ancientwarfare.core.research;

import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.research.ResearchResource;

public record ResearchGoal(String id, String displayName, int time, Set<String> dependencies, List<ResearchResource> resourceRequirements) {
   public Component translatedName() {
      return Component.translatable("research.ancientwarfare." + this.id);
   }

   public boolean canResearch(Set<String> completedResearch) {
      return completedResearch.containsAll(this.dependencies) && !completedResearch.contains(this.id);
   }

   public List<ItemStack> resources() {
      return this.resourceRequirements.stream().map(ResearchResource::toStack).filter(stack -> !stack.isEmpty()).toList();
   }
}
