package net.shadowmage.ancientwarfare.core.common.worksite;

public enum WorksiteInventoryAccess {
   NONE("guistrings.automation.side_access.none"),
   OUTPUT("guistrings.automation.side_access.output"),
   INPUT("guistrings.automation.side_access.input"),
   SPECIAL("guistrings.automation.side_access.special"),
   UPGRADE("guistrings.automation.side_access.upgrade");

   private final String translationKey;

   private WorksiteInventoryAccess(String translationKey) {
      this.translationKey = translationKey;
   }

   public String translationKey() {
      return this.translationKey;
   }

   public WorksiteInventoryAccess next() {
      WorksiteInventoryAccess[] values = values();
      return values[(this.ordinal() + 1) % values.length];
   }
}
