package net.shadowmage.ancientwarfare.core.common.worksite;

public enum WorksiteRelativeSide {
   TOP("guistrings.automation.side.top"),
   BOTTOM("guistrings.automation.side.bottom"),
   FRONT("guistrings.automation.side.front"),
   BACK("guistrings.automation.side.back"),
   LEFT("guistrings.automation.side.left"),
   RIGHT("guistrings.automation.side.right");

   private final String translationKey;

   private WorksiteRelativeSide(String translationKey) {
      this.translationKey = translationKey;
   }

   public String translationKey() {
      return this.translationKey;
   }
}
