package net.shadowmage.ancientwarfare.core.client;

import java.util.function.IntConsumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

abstract class LegacyWorksiteBoundsScreen<T extends AbstractContainerMenu> extends LegacyContainerScreen<T> {
   private static final String[] BUTTON_KEYS = new String[]{"north", "south", "west", "east", "xsize_minus", "xsize_plus", "zsize_minus", "zsize_plus"};
   private static final int MAP_X = 40;
   private static final int MAP_Y = 44;
   private static final int MAP_SIZE = 176;
   private final Supplier<BlockPos> boundsMin;
   private final Supplier<BlockPos> boundsMax;
   private final Supplier<BlockPos> worksitePos;
   private final IntConsumer previewButton;

   protected LegacyWorksiteBoundsScreen(
      T menu,
      Inventory inventory,
      Component title,
      Supplier<BlockPos> boundsMin,
      Supplier<BlockPos> boundsMax,
      Supplier<BlockPos> worksitePos,
      IntConsumer previewButton
   ) {
      super(menu, inventory, title, 256, 240);
      this.boundsMin = boundsMin;
      this.boundsMax = boundsMax;
      this.worksitePos = worksitePos;
      this.previewButton = previewButton;
   }

   @Override
   protected void m_7856_() {
      super.m_7856_();

      for (int id = 0; id < BUTTON_KEYS.length; id++) {
         int buttonId = id;
         this.legacyButton(
            "bounds adjustment " + id, 48 + id % 4 * 40, 12 + id / 4 * 12, 40, 12, Component.m_237115_("guistrings.automation." + BUTTON_KEYS[id]), button -> {
               this.previewButton.accept(buttonId);
               this.f_96541_.f_91072_.m_105208_(this.f_97732_.f_38840_, buttonId);
            }
         );
      }

      this.registerLayoutRect("bounds preview", 40, 44, 176, 176, true);
      this.registerLayoutRect("bounds size", 8, 226, 240, 9, true);
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      BlockPos min = this.boundsMin.get();
      BlockPos max = this.boundsMax.get();
      BlockPos worksite = this.worksitePos.get();
      int minX = Math.min(min.m_123341_(), worksite.m_123341_());
      int minZ = Math.min(min.m_123343_(), worksite.m_123343_());
      int maxX = Math.max(max.m_123341_(), worksite.m_123341_());
      int maxZ = Math.max(max.m_123343_(), worksite.m_123343_());
      long columns = Math.max(1L, (long)maxX - (long)minX + 1L);
      long rows = Math.max(1L, (long)maxZ - (long)minZ + 1L);
      double scale = Math.min(16.0, 174.0 / Math.max((double)columns + 2.0, (double)rows + 2.0));
      double originX = (double)(this.f_97735_ + 40) + (176.0 - (double)columns * scale) / 2.0;
      double originY = (double)(this.f_97736_ + 44) + (176.0 - (double)rows * scale) / 2.0;
      graphics.m_280262_();
      graphics.m_280588_(this.f_97735_ + 40, this.f_97736_ + 44, this.f_97735_ + 40 + 176, this.f_97736_ + 44 + 176);
      int x1 = pixel(originX + (double)((long)min.m_123341_() - (long)minX) * scale);
      int y1 = pixel(originY + (double)((long)min.m_123343_() - (long)minZ) * scale);
      int x2 = pixel(originX + (double)((long)max.m_123341_() - (long)minX + 1L) * scale);
      int y2 = pixel(originY + (double)((long)max.m_123343_() - (long)minZ + 1L) * scale);
      graphics.m_280509_(x1, y1, Math.max(x1 + 1, x2), Math.max(y1 + 1, y2), -16777216);
      if (scale >= 3.0) {
         for (long x = (long)min.m_123341_(); x <= (long)max.m_123341_(); x++) {
            int gridX = pixel(originX + (double)(x - (long)minX) * scale);
            graphics.m_280509_(gridX, y1, gridX + 1, y2, -10461088);
         }

         for (long z = (long)min.m_123343_(); z <= (long)max.m_123343_(); z++) {
            int gridY = pixel(originY + (double)(z - (long)minZ) * scale);
            graphics.m_280509_(x1, gridY, x2, gridY + 1, -10461088);
         }
      }

      double worksiteX = originX + (double)((long)worksite.m_123341_() - (long)minX) * scale;
      double worksiteY = originY + (double)((long)worksite.m_123343_() - (long)minZ) * scale;
      graphics.m_280509_(
         pixel(worksiteX),
         pixel(worksiteY),
         Math.max(pixel(worksiteX) + 1, pixel(worksiteX + scale)),
         Math.max(pixel(worksiteY) + 1, pixel(worksiteY + scale)),
         -16776961
      );
      graphics.m_280262_();
      graphics.m_280618_();
   }

   private static int pixel(double coordinate) {
      return (int)Math.round(coordinate);
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      BlockPos min = this.boundsMin.get();
      BlockPos max = this.boundsMax.get();
      Component label = Component.m_237110_(
         "guistrings.automation.bounds_size",
         new Object[]{(long)max.m_123341_() - (long)min.m_123341_() + 1L, (long)max.m_123343_() - (long)min.m_123343_() + 1L}
      );
      graphics.m_280168_().m_85836_();
      graphics.m_280168_().m_252880_(8.0F, 226.0F, 0.0F);
      float scale = Math.min(1.0F, 240.0F / (float)Math.max(1, this.f_96547_.m_92852_(label)));
      graphics.m_280168_().m_85841_(scale, scale, 1.0F);
      graphics.m_280614_(this.f_96547_, label, 0, 0, -1, false);
      graphics.m_280168_().m_85849_();
   }
}
