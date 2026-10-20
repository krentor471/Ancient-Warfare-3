package net.shadowmage.ancientwarfare.core.common.worksite;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.item.WorksiteUpgradeItem;

public enum WorksiteUpgrade {
   SIZE_MEDIUM(),
   SIZE_LARGE(SIZE_MEDIUM),
   QUARRY_MEDIUM(),
   QUARRY_LARGE(QUARRY_MEDIUM),
   ENCHANTED_TOOLS_1(),
   ENCHANTED_TOOLS_2(ENCHANTED_TOOLS_1),
   TOOL_QUALITY_1(),
   TOOL_QUALITY_2(TOOL_QUALITY_1),
   TOOL_QUALITY_3(TOOL_QUALITY_1, TOOL_QUALITY_2),
   BASIC_CHUNK_LOADER(),
   QUARRY_CHUNK_LOADER();

   private final Set<WorksiteUpgrade> overrides;

   private WorksiteUpgrade(WorksiteUpgrade... overrides) {
      this.overrides = Arrays.stream(overrides).collect(Collectors.toUnmodifiableSet());
   }

   public boolean overrides(WorksiteUpgrade upgrade) {
      return this.overrides.contains(upgrade);
   }

   public boolean conflictsWith(WorksiteUpgrade upgrade) {
      return this == upgrade || this.overrides(upgrade) || upgrade.overrides(this);
   }

   public String id() {
      return this.name().toLowerCase(Locale.ROOT);
   }

   public static WorksiteUpgrade fromStack(ItemStack stack) {
      return stack.m_41720_() instanceof WorksiteUpgradeItem upgradeItem ? upgradeItem.getUpgrade() : null;
   }
}
