package net.shadowmage.ancientwarfare.core.client;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.shadowmage.ancientwarfare.core.forge.AWCoreNetwork;

/**
 * Choosing the work for a marked box (dig it out, fill it in, raise a wall round it or lay a floor,
 * and how deep or high) and the list of the player's jobs with their progress.
 */
public final class ConstructionScreen extends Screen {
   private static final String[] KINDS = {"dig", "fill", "wall", "floor"};
   private static final int PANEL_WIDTH = 300;
   private static final int MAX_HEIGHT = 24;
   private static int kind;
   private static int height = 3;
   @Nullable
   private final BlockPos first;
   @Nullable
   private final BlockPos second;
   @Nullable
   private final Screen parent;
   private final List<Line> lines = new ArrayList<>();
   private int left;
   private int top;
   private int bottom;

   private record Line(Component text, int color, int x, int y) {
   }

   public ConstructionScreen(@Nullable BlockPos first, @Nullable BlockPos second) {
      this(first, second, null);
   }

   public ConstructionScreen(@Nullable BlockPos first, @Nullable BlockPos second, @Nullable Screen parent) {
      super(Component.translatable("item.ancientwarfare.construction_wand"));
      this.first = first;
      this.second = second;
      this.parent = parent;
   }

   @Nullable
   public Screen parent() {
      return this.parent;
   }

   /** Back to the building view it was opened from, if any. */
   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   @Override
   public void removed() {
      ConstructionClient.setPreview(null, null);
   }

   void jobsChanged() {
      this.rebuildWidgets();
   }

   private boolean places() {
      return kind != 0;
   }

   /** The box the job covers: digging and filling go down from the marked top, a wall goes up from the ground, a floor lies on it. */
   private BlockPos[] box() {
      int minX = Math.min(this.first.getX(), this.second.getX());
      int maxX = Math.max(this.first.getX(), this.second.getX());
      int minZ = Math.min(this.first.getZ(), this.second.getZ());
      int maxZ = Math.max(this.first.getZ(), this.second.getZ());
      int low = Math.min(this.first.getY(), this.second.getY());
      int high = Math.max(this.first.getY(), this.second.getY());
      return switch (kind) {
         case 0, 1 -> new BlockPos[]{new BlockPos(minX, Math.min(low, high - height + 1), minZ), new BlockPos(maxX, high, maxZ)};
         case 2 -> new BlockPos[]{new BlockPos(minX, low + 1, minZ), new BlockPos(maxX, low + height, maxZ)};
         default -> new BlockPos[]{new BlockPos(minX, low, minZ), new BlockPos(maxX, low, maxZ)};
      };
   }

   private int volume(BlockPos[] box) {
      int x = box[1].getX() - box[0].getX() + 1;
      int y = box[1].getY() - box[0].getY() + 1;
      int z = box[1].getZ() - box[0].getZ() + 1;
      if (kind == 2) {
         return (x <= 2 || z <= 2 ? x * z : 2 * (x + z) - 4) * y;
      }

      return x * y * z;
   }

   @Override
   protected void init() {
      this.lines.clear();
      int width = Math.min(PANEL_WIDTH, this.width - 24);
      this.left = (this.width - width) / 2;
      this.top = 24;
      int x = this.left + 8;
      int y = this.top + 6;
      if (this.first != null && this.second != null) {
         int sizeX = Math.abs(this.first.getX() - this.second.getX()) + 1;
         int sizeZ = Math.abs(this.first.getZ() - this.second.getZ()) + 1;
         this.line(Component.translatable("gui.ancientwarfare.construction.area", sizeX, sizeZ), 0xFFFFFF, x, y);
         y += 14;
         int buttonWidth = (width - 16 - 9) / 4;
         for (int index = 0; index < KINDS.length; index++) {
            int chosen = index;
            Button button = Button.builder(Component.translatable("construction.ancientwarfare.kind." + KINDS[index]), b -> {
               kind = chosen;
               this.rebuildWidgets();
            }).bounds(x + index * (buttonWidth + 3), y, buttonWidth, 20).build();
            button.active = kind != index;
            this.addRenderableWidget(button);
         }

         y += 26;
         if (kind != 3) {
            this.line(Component.translatable(kind == 2 ? "gui.ancientwarfare.construction.height" : "gui.ancientwarfare.construction.depth"), 0xE0E0E0, x, y + 6);
            this.addRenderableWidget(Button.builder(Component.literal("-"), b -> {
               height = Math.max(1, height - 1);
               this.rebuildWidgets();
            }).bounds(x + 110, y, 20, 20).build());
            this.line(Component.literal(String.valueOf(height)), 0xFFFFFF, x + 138, y + 6);
            this.addRenderableWidget(Button.builder(Component.literal("+"), b -> {
               height = Math.min(MAX_HEIGHT, height + 1);
               this.rebuildWidgets();
            }).bounds(x + 156, y, 20, 20).build());
            y += 26;
         }

         boolean ready = true;
         if (this.places()) {
            ItemStack held = Minecraft.getInstance().player.getOffhandItem();
            if (held.getItem() instanceof BlockItem item && !(item.getBlock() instanceof EntityBlock)) {
               this.line(Component.translatable("gui.ancientwarfare.construction.block", held.getHoverName()), 0xE0E0E0, x, y);
            } else {
               this.line(Component.translatable("gui.ancientwarfare.construction.need_block").withStyle(ChatFormatting.RED), 0xFF6060, x, y);
               ready = false;
            }

            y += 12;
            this.line(Component.translatable("gui.ancientwarfare.construction.supply"), 0xA0A0A0, x, y);
            y += 12;
         } else {
            this.line(Component.translatable("gui.ancientwarfare.construction.drops"), 0xA0A0A0, x, y);
            y += 12;
         }

         BlockPos[] box = this.box();
         ConstructionClient.setPreview(box[0], box[1]);
         this.line(Component.translatable("gui.ancientwarfare.construction.blocks", this.volume(box)), 0xE0E0E0, x, y + 6);
         Button start = Button.builder(Component.translatable("gui.ancientwarfare.construction.start"), b -> {
            AWCoreNetwork.sendConstructionCreate(box[0], box[1], kind);
            this.onClose();
         }).bounds(this.left + width - 8 - 100, y, 100, 20).build();
         start.active = ready;
         this.addRenderableWidget(start);
         y += 30;
      }

      List<ConstructionClient.JobView> jobs = ConstructionClient.jobs();
      this.line(Component.translatable("gui.ancientwarfare.construction.jobs", jobs.size()), 0xFFD860, x, y);
      y += 14;
      if (jobs.isEmpty()) {
         this.line(Component.translatable("gui.ancientwarfare.construction.no_jobs"), 0xA0A0A0, x, y);
         y += 14;
      }

      for (ConstructionClient.JobView job : jobs) {
         int percent = job.total() <= 0 ? 0 : job.done() * 100 / job.total();
         Component kindName = Component.translatable("construction.ancientwarfare.kind." + KINDS[Math.floorMod(job.kind(), KINDS.length)]);
         this.line(Component.translatable("gui.ancientwarfare.construction.job", job.id(), kindName, job.done(), percent), 0xFFFFFF, x, y);
         this.line(
            Component.translatable("gui.ancientwarfare.construction.job_where", job.min().getX(), job.min().getY(), job.min().getZ(), job.block()), 0xA0A0A0, x, y + 11
         );
         this.addRenderableWidget(
            Button.builder(Component.translatable("gui.ancientwarfare.construction.cancel"), b -> AWCoreNetwork.sendConstructionCancel(job.id()))
               .bounds(this.left + width - 8 - 70, y + 1, 70, 20)
               .build()
         );
         y += 26;
      }

      this.bottom = y + 2;
   }

   private void line(Component text, int color, int x, int y) {
      this.lines.add(new Line(text, color, x, y));
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      // Over the building view the world stays in sight, so the box being chosen shows on the ground.
      if (this.parent == null) {
         this.renderBackground(graphics);
      }

      int width = Math.min(PANEL_WIDTH, this.width - 24);
      graphics.fill(this.left - 2, this.top - 2, this.left + width + 2, this.bottom + 2, 0xFF5A4A30);
      graphics.fill(this.left, this.top, this.left + width, this.bottom, 0xE0201A14);
      graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
      for (Line line : this.lines) {
         graphics.drawString(this.font, line.text(), line.x(), line.y(), line.color(), false);
      }

      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
