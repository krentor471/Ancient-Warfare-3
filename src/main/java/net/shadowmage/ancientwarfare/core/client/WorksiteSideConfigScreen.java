package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteRelativeSide;
import net.shadowmage.ancientwarfare.core.menu.WorksiteSideConfigMenu;

public class WorksiteSideConfigScreen extends LegacyContainerScreen<WorksiteSideConfigMenu> {
   private static final int TEXT_COLOR = -1;

   public WorksiteSideConfigScreen(WorksiteSideConfigMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, 191, 106);
   }

   @Override
   protected void m_7856_() {
      super.m_7856_();
      this.registerLayoutRect("side column", 8, 18, 58, 84, false);
      this.registerLayoutRect("direction column", 74, 18, 48, 84, false);
      int row = 18;

      for (WorksiteRelativeSide side : WorksiteRelativeSide.values()) {
         this.legacyButton("side access " + side.name(), 128, row, 55, 12, this.accessText(side), button -> {
            ((WorksiteSideConfigMenu)this.f_97732_).previewButton(side.ordinal());
            this.f_96541_.f_91072_.m_105208_(((WorksiteSideConfigMenu)this.f_97732_).f_38840_, side.ordinal());
            button.m_93666_(this.accessText(side));
         });
         row += 14;
      }
   }

   private Component accessText(WorksiteRelativeSide side) {
      return Component.m_237115_(((WorksiteSideConfigMenu)this.f_97732_).getSideAccess(side).translationKey());
   }

   private Component directionText(WorksiteRelativeSide side) {
      BlockState state = this.f_96541_.f_91073_.m_8055_(((WorksiteSideConfigMenu)this.f_97732_).getWorksitePos());
      Direction front = state.m_61138_(BlockStateProperties.f_61374_) ? (Direction)state.m_61143_(BlockStateProperties.f_61374_) : Direction.NORTH;

      Direction direction = switch (side) {
         case TOP -> Direction.UP;
         case BOTTOM -> Direction.DOWN;
         case FRONT -> front;
         case BACK -> front.m_122424_();
         case LEFT -> front.m_122427_();
         case RIGHT -> front.m_122428_();
      };
      return Component.m_237115_("guistrings.inventory.direction." + direction.m_122433_());
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      this.drawLabel(graphics, Component.m_237115_("guistrings.automation.block_side"), 8, 6, 58);
      this.drawLabel(graphics, Component.m_237115_("guistrings.automation.direction"), 74, 6, 48);
      this.drawLabel(graphics, Component.m_237115_("guistrings.automation.inventory_accessed"), 128, 6, 55);
      int row = 18;

      for (WorksiteRelativeSide side : WorksiteRelativeSide.values()) {
         this.drawLabel(graphics, Component.m_237115_(side.translationKey()), 8, row + 2, 58);
         this.drawLabel(graphics, this.directionText(side), 74, row + 2, 48);
         row += 14;
      }
   }

   private void drawLabel(GuiGraphics graphics, Component label, int x, int y, int width) {
      graphics.m_280168_().m_85836_();
      graphics.m_280168_().m_252880_((float)x, (float)y, 0.0F);
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.f_96547_.m_92852_(label)));
      graphics.m_280168_().m_85841_(scale, scale, 1.0F);
      graphics.m_280614_(this.f_96547_, label, 0, 0, -1, false);
      graphics.m_280168_().m_85849_();
   }
}
