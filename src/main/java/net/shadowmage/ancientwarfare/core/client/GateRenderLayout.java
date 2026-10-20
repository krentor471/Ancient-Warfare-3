package net.shadowmage.ancientwarfare.core.client;

import net.shadowmage.ancientwarfare.core.entity.GateEntity;

final class GateRenderLayout {
   private GateRenderLayout() {
   }

   static float panelMovement(GateEntity.OpeningStyle style, int width, int col, float progress) {
      return style == GateEntity.OpeningStyle.DOUBLE_SLIDING && (float)col < (float)width * 0.5F ? -progress : progress;
   }

   static boolean panelVisible(GateEntity.OpeningStyle style, int width, int height, int col, int row, float progress) {
      if (style == GateEntity.OpeningStyle.VERTICAL) {
         return (float)row + progress <= (float)height - 0.475F;
      } else {
         float movement = panelMovement(style, width, col, progress);
         return style == GateEntity.OpeningStyle.DOUBLE_SLIDING && (float)col < (float)width * 0.5F
            ? (float)col + movement > -0.5F
            : (float)col + movement < (float)width - (style == GateEntity.OpeningStyle.DOUBLE_SLIDING ? 0.475F : 0.5F);
      }
   }
}
