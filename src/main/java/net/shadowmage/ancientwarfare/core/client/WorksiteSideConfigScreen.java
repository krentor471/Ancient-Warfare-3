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
   protected void init() {
      super.init();
      this.registerLayoutRect("side column", 8, 18, 58, 84, false);
      this.registerLayoutRect("direction column", 74, 18, 48, 84, false);
      int row = 18;

      for (WorksiteRelativeSide side : WorksiteRelativeSide.values()) {
         this.legacyButton("side access " + side.name(), 128, row, 55, 12, this.accessText(side), button -> {
            ((WorksiteSideConfigMenu)this.menu).previewButton(side.ordinal());
            this.minecraft.gameMode.handleInventoryButtonClick(((WorksiteSideConfigMenu)this.menu).containerId, side.ordinal());
            button.setMessage(this.accessText(side));
         });
         row += 14;
      }
   }

   private Component accessText(WorksiteRelativeSide side) {
      return Component.translatable(((WorksiteSideConfigMenu)this.menu).getSideAccess(side).translationKey());
   }

   private Component directionText(WorksiteRelativeSide side) {
      BlockState state = this.minecraft.level.getBlockState(((WorksiteSideConfigMenu)this.menu).getWorksitePos());
      Direction front = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? (Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;

      Direction direction = switch (side) {
         case TOP -> Direction.UP;
         case BOTTOM -> Direction.DOWN;
         case FRONT -> front;
         case BACK -> front.getOpposite();
         case LEFT -> front.getClockWise();
         case RIGHT -> front.getCounterClockWise();
      };
      return Component.translatable("guistrings.inventory.direction." + direction.getName());
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      this.drawLabel(graphics, Component.translatable("guistrings.automation.block_side"), 8, 6, 58);
      this.drawLabel(graphics, Component.translatable("guistrings.automation.direction"), 74, 6, 48);
      this.drawLabel(graphics, Component.translatable("guistrings.automation.inventory_accessed"), 128, 6, 55);
      int row = 18;

      for (WorksiteRelativeSide side : WorksiteRelativeSide.values()) {
         this.drawLabel(graphics, Component.translatable(side.translationKey()), 8, row + 2, 58);
         this.drawLabel(graphics, this.directionText(side), 74, row + 2, 48);
         row += 14;
      }
   }

   private void drawLabel(GuiGraphics graphics, Component label, int x, int y, int width) {
      graphics.pose().pushPose();
      graphics.pose().translate((float)x, (float)y, 0.0F);
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.font.width(label)));
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(this.font, label, 0, 0, -1, false);
      graphics.pose().popPose();
   }
}
