package net.shadowmage.ancientwarfare.core.forge;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.entity.PlayerOwnedNpcEntity;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/**
 * The command view: a top-down camera with a free cursor. Left button selects (click or drag a box), right button
 * orders the selection to the ground or against an enemy. It drives the held baton, so orders and squads are the same
 * as by hand.
 */
public class CommandViewScreen extends Screen {
   private static final double PICK_RANGE = 256.0;
   private static final int DRAG_THRESHOLD = 4;
   private static final int EDGE_SCROLL_MARGIN = 3;
   private static final int BOX_FILL = 0x3366CCFF;
   private static final int BOX_BORDER = 0xCC66CCFF;
   private static final int TEXT_COLOR = 0xFFFFFF;
   private static final int MAX_STROKE_POINTS = 96;
   /** Cursor travel in GUI pixels between recorded points of a stroke. */
   private static final double STROKE_STEP = 8.0;
   private final KeyMapping toggleKey;
   private double dragStartX;
   private double dragStartY;
   private double mouseX;
   private double mouseY;
   private boolean dragging;
   private double frontStartX;
   private double frontStartY;
   private boolean drawingFront;
   private boolean mouseSeen;
   private double scroll;
   /** Cursor positions of the right-button stroke, and the ground block under each. */
   private final List<double[]> stroke = new ArrayList<>();
   private final List<BlockPos> strokeBlocks = new ArrayList<>();
   /** What is being drawn right now, for the world renderer to show where the selection would stand. */
   private static List<BlockPos> previewStroke = List.of();
   private static BlockPos previewBarrageCenter;
   private static int previewBarrageRadius;

   CommandViewScreen(KeyMapping toggleKey) {
      super(Component.translatable("guistrings.npc.command_view"));
      this.toggleKey = toggleKey;
   }

   @Override
   protected void init() {
      if (!CommandViewCamera.isActive()) {
         CommandViewCamera.start(this.minecraft);
      }
   }

   @Override
   public void removed() {
      CommandViewCamera.stop(this.minecraft);
      clearPreview();
   }

   private static void clearPreview() {
      previewStroke = List.of();
      previewBarrageCenter = null;
   }

   /** Ground blocks of the line being drawn, empty when nothing is. */
   static List<BlockPos> previewStroke() {
      return previewStroke;
   }

   /** Centre of the barrage circle being dragged out, or null. */
   static BlockPos previewBarrageCenter() {
      return previewBarrageCenter;
   }

   static int previewBarrageRadius() {
      return previewBarrageRadius;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void tick() {
      ItemStack baton = heldBaton(this.minecraft);
      if (this.minecraft.player == null || !this.minecraft.player.isAlive() || !CommandBatonItem.isAdvanced(baton)) {
         this.onClose();
      } else {
         long window = this.minecraft.getWindow().getWindow();
         double forward = axis(window, this.minecraft.options.keyUp, this.minecraft.options.keyDown);
         double strafe = axis(window, this.minecraft.options.keyRight, this.minecraft.options.keyLeft);
         if (!this.dragging && this.mouseSeen) {
            // Pan by holding the cursor at the screen edge, as in strategy games.
            if (this.mouseX <= EDGE_SCROLL_MARGIN) {
               strafe = -1.0;
            } else if (this.mouseX >= this.width - 1 - EDGE_SCROLL_MARGIN) {
               strafe = 1.0;
            }

            if (this.mouseY <= EDGE_SCROLL_MARGIN) {
               forward = 1.0;
            } else if (this.mouseY >= this.height - 1 - EDGE_SCROLL_MARGIN) {
               forward = -1.0;
            }
         }

         float turn = (float)(isDown(window, GLFW.GLFW_KEY_E) - isDown(window, GLFW.GLFW_KEY_Q));
         CommandViewCamera.tick(this.minecraft, forward, strafe, turn, this.scroll);
         this.scroll = 0.0;
      }
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (this.drawingFront) {
         // Dotted line along the stroke being drawn; with Alt a straight line from the barrage centre.
         if (hasAltDown()) {
            this.dottedLine(graphics, this.frontStartX, this.frontStartY, mouseX, mouseY);
         } else {
            double[] previous = null;
            for (double[] point : this.stroke) {
               if (previous != null) {
                  this.dottedLine(graphics, previous[0], previous[1], point[0], point[1]);
               }

               previous = point;
            }

            if (previous != null) {
               this.dottedLine(graphics, previous[0], previous[1], mouseX, mouseY);
            }
         }
      }

      if (this.dragging) {
         int x0 = (int)Math.min(this.dragStartX, mouseX);
         int y0 = (int)Math.min(this.dragStartY, mouseY);
         int x1 = (int)Math.max(this.dragStartX, mouseX);
         int y1 = (int)Math.max(this.dragStartY, mouseY);
         graphics.fill(x0, y0, x1, y1, BOX_FILL);
         graphics.renderOutline(x0, y0, x1 - x0, y1 - y0, BOX_BORDER);
      }

      ItemStack baton = heldBaton(this.minecraft);
      graphics.drawString(
         this.font,
         Component.translatable(
            "guistrings.npc.command_view.status",
            new Object[]{CommandBatonItem.selectionCount(baton), Component.translatable(CommandBatonItem.getFormation(baton).translationKey())}
         ),
         6,
         6,
         TEXT_COLOR
      );
      graphics.drawString(this.font, Component.translatable("guistrings.npc.command_view.help5", new Object[]{CommandBatonItem.getBarrageRadius(baton)}), 6, this.height - 55, TEXT_COLOR);
      graphics.drawString(this.font, Component.translatable("guistrings.npc.command_view.help1"), 6, this.height - 44, TEXT_COLOR);
      graphics.drawString(this.font, Component.translatable("guistrings.npc.command_view.help4"), 6, this.height - 33, TEXT_COLOR);
      graphics.drawString(this.font, Component.translatable("guistrings.npc.command_view.help2"), 6, this.height - 22, TEXT_COLOR);
      graphics.drawString(this.font, Component.translatable("guistrings.npc.command_view.help3"), 6, this.height - 11, TEXT_COLOR);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   private void dottedLine(GuiGraphics graphics, double x0, double y0, double x1, double y1) {
      double dx = x1 - x0;
      double dy = y1 - y0;
      int steps = (int)(Math.sqrt(dx * dx + dy * dy) / 3.0);

      for (int i = 0; i <= steps; i++) {
         int x = (int)(x0 + dx * i / Math.max(1, steps));
         int y = (int)(y0 + dy * i / Math.max(1, steps));
         graphics.fill(x - 1, y - 1, x + 1, y + 1, BOX_BORDER);
      }
   }

   private void addStrokePoint(double mouseX, double mouseY) {
      if (this.stroke.size() < MAX_STROKE_POINTS) {
         this.stroke.add(new double[]{mouseX, mouseY});
         BlockHitResult hit = this.pickBlock(mouseX, mouseY);
         if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos block = hit.getBlockPos().relative(hit.getDirection());
            if (this.strokeBlocks.isEmpty() || !this.strokeBlocks.get(this.strokeBlocks.size() - 1).equals(block)) {
               this.strokeBlocks.add(block);
            }
         }
      }
   }

   private void updateBarragePreview(double mouseX, double mouseY) {
      BlockHitResult center = this.pickBlock(this.frontStartX, this.frontStartY);
      BlockHitResult edge = this.pickBlock(mouseX, mouseY);
      if (center.getType() == HitResult.Type.BLOCK && edge.getType() == HitResult.Type.BLOCK) {
         previewBarrageCenter = center.getBlockPos().relative(center.getDirection());
         BlockPos edgePos = edge.getBlockPos();
         previewBarrageRadius = Math.max(
            1, Math.min(CommandBatonItem.MAX_BARRAGE_RADIUS, (int)Math.round(Math.hypot(edgePos.getX() - previewBarrageCenter.getX(), edgePos.getZ() - previewBarrageCenter.getZ())))
         );
      }
   }

   @Override
   public void mouseMoved(double mouseX, double mouseY) {
      // Edge scrolling starts only once the cursor has really moved: the first frame may report it in a corner.
      this.mouseSeen = true;
      this.mouseX = mouseX;
      this.mouseY = mouseY;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
         this.dragStartX = mouseX;
         this.dragStartY = mouseY;
         this.dragging = false;
         return true;
      } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
         // The order goes on release: a click orders as usual, a drag draws the front of the formation.
         this.frontStartX = mouseX;
         this.frontStartY = mouseY;
         this.drawingFront = false;
         this.stroke.clear();
         this.strokeBlocks.clear();
         this.addStrokePoint(mouseX, mouseY);
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && Math.abs(mouseX - this.dragStartX) + Math.abs(mouseY - this.dragStartY) > DRAG_THRESHOLD) {
         this.dragging = true;
      } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && Math.abs(mouseX - this.frontStartX) + Math.abs(mouseY - this.frontStartY) > DRAG_THRESHOLD) {
         this.drawingFront = true;
         if (hasAltDown()) {
            previewStroke = List.of();
            this.updateBarragePreview(mouseX, mouseY);
         } else {
            double[] last = this.stroke.get(this.stroke.size() - 1);
            if (Math.abs(mouseX - last[0]) + Math.abs(mouseY - last[1]) >= STROKE_STEP) {
               this.addStrokePoint(mouseX, mouseY);
            }

            previewBarrageCenter = null;
            previewStroke = List.copyOf(this.strokeBlocks);
         }
      }

      return true;
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
         boolean add = hasShiftDown();
         if (this.dragging) {
            this.dragging = false;
            this.selectInBox(Math.min(this.dragStartX, mouseX), Math.min(this.dragStartY, mouseY), Math.max(this.dragStartX, mouseX), Math.max(this.dragStartY, mouseY), add);
         } else {
            this.selectUnderCursor(mouseX, mouseY, add);
         }

         return true;
      } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
         if (this.drawingFront && hasAltDown()) {
            this.drawingFront = false;
            this.orderBarrage(this.frontStartX, this.frontStartY, mouseX, mouseY);
         } else if (this.drawingFront) {
            this.drawingFront = false;
            this.addStrokePoint(mouseX, mouseY);
            this.orderStroke();
         } else if (hasControlDown()) {
            this.orderFace(this.frontStartX, this.frontStartY);
         } else {
            this.order(this.frontStartX, this.frontStartY);
         }

         clearPreview();
         return true;
      } else {
         return super.mouseReleased(mouseX, mouseY, button);
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      this.scroll += delta;
      return true;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.toggleKey.matches(keyCode, scanCode)) {
         this.onClose();
         return true;
      } else if (keyCode == GLFW.GLFW_KEY_H) {
         AWCoreNetwork.sendCommandViewOrder(CommandBatonItem.ViewOrder.HOLD, this.minecraft.player.blockPosition(), -1, false);
         return true;
      } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
         AWCoreNetwork.sendCommandViewOrder(CommandBatonItem.ViewOrder.CLEAR, this.minecraft.player.blockPosition(), -1, false);
         return true;
      } else if (ForgeClientInputEvents.BATON_BARRAGE_RADIUS.matches(keyCode, scanCode)) {
         AWCoreNetwork.sendBatonBarrageRadius();
         return true;
      } else if (keyCode == GLFW.GLFW_KEY_G) {
         AWCoreNetwork.sendBatonFormation();
         return true;
      } else if (keyCode == GLFW.GLFW_KEY_V) {
         AWCoreNetwork.sendBatonStance();
         return true;
      } else if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
         // Ctrl+number saves the selection as a squad; the number alone (or Alt+number) selects that squad.
         AWCoreNetwork.sendBatonSquad(keyCode - GLFW.GLFW_KEY_0, hasControlDown());
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   /** A straight stroke is the front of the formation; a curve or a loop spreads the selection along it. */
   private void orderStroke() {
      if (this.strokeBlocks.size() >= 2) {
         AWCoreNetwork.sendCommandViewShape(new ArrayList<>(this.strokeBlocks), hasShiftDown());
      } else {
         this.order(this.frontStartX, this.frontStartY);
      }
   }

   private void orderBarrage(double startX, double startY, double endX, double endY) {
      BlockHitResult center = this.pickBlock(startX, startY);
      BlockHitResult edge = this.pickBlock(endX, endY);
      if (center.getType() == HitResult.Type.BLOCK && edge.getType() == HitResult.Type.BLOCK) {
         AWCoreNetwork.sendCommandViewBarrage(center.getBlockPos().relative(center.getDirection()), edge.getBlockPos());
      }
   }

   private void orderFace(double mouseX, double mouseY) {
      BlockHitResult hit = this.pickBlock(mouseX, mouseY);
      if (hit.getType() == HitResult.Type.BLOCK) {
         AWCoreNetwork.sendCommandViewFace(hit.getBlockPos());
      }
   }

   private void order(double mouseX, double mouseY) {
      HitResult hit = this.pick(mouseX, mouseY);
      boolean queued = hasShiftDown();
      if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target && !this.isOwn(target)) {
         AWCoreNetwork.sendCommandViewOrder(CommandBatonItem.ViewOrder.ATTACK, target.blockPosition(), target.getId(), false);
      } else if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK && hasAltDown()) {
         AWCoreNetwork.sendCommandViewOrder(CommandBatonItem.ViewOrder.BARRAGE, blockHit.getBlockPos().relative(blockHit.getDirection()), -1, false);
      } else if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK) {
         AWCoreNetwork.sendCommandViewOrder(CommandBatonItem.ViewOrder.MOVE, blockHit.getBlockPos().relative(blockHit.getDirection()), -1, queued);
      } else if (hit instanceof EntityHitResult entityHit) {
         AWCoreNetwork.sendCommandViewOrder(CommandBatonItem.ViewOrder.MOVE, entityHit.getEntity().blockPosition(), -1, queued);
      }
   }

   private void selectUnderCursor(double mouseX, double mouseY, boolean add) {
      HitResult hit = this.pick(mouseX, mouseY);
      if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof PlayerOwnedNpcEntity npc && this.isOwn(npc)) {
         AWCoreNetwork.sendCommandViewSelect(new int[]{npc.getId()}, add);
      } else if (!add) {
         AWCoreNetwork.sendCommandViewSelect(new int[0], false);
      }
   }

   private void selectInBox(double x0, double y0, double x1, double y1, boolean add) {
      Camera camera = this.minecraft.gameRenderer.getMainCamera();
      List<Integer> ids = new ArrayList<>();
      AABB search = this.minecraft.player.getBoundingBox().inflate(CommandBatonItem.COMMAND_VIEW_RANGE);

      for (PlayerOwnedNpcEntity npc : this.minecraft.level.getEntitiesOfClass(PlayerOwnedNpcEntity.class, search, npc -> npc.isAlive() && this.isOwn(npc))) {
         double[] screen = this.project(camera, npc.position().add(0.0, npc.getBbHeight() * 0.5, 0.0));
         if (screen != null && screen[0] >= x0 && screen[0] <= x1 && screen[1] >= y0 && screen[1] <= y1) {
            ids.add(npc.getId());
         }
      }

      AWCoreNetwork.sendCommandViewSelect(ids.stream().mapToInt(Integer::intValue).toArray(), add);
   }

   private boolean isOwn(LivingEntity entity) {
      if (entity instanceof PlayerOwnedNpcEntity npc) {
         UUID self = this.minecraft.player.getUUID();
         return npc.getOwner().map(self::equals).orElse(false);
      } else {
         return false;
      }
   }

   /** The block under the cursor, ignoring entities in the way. */
   private BlockHitResult pickBlock(double mouseX, double mouseY) {
      Camera camera = this.minecraft.gameRenderer.getMainCamera();
      Vec3 start = camera.getPosition();
      Vec3 end = start.add(this.ray(camera, mouseX, mouseY).scale(PICK_RANGE));
      return this.minecraft.level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this.minecraft.player));
   }

   /** The block or entity under the cursor, looking from the camera along the cursor's ray. */
   private HitResult pick(double mouseX, double mouseY) {
      Camera camera = this.minecraft.gameRenderer.getMainCamera();
      Vec3 start = camera.getPosition();
      Vec3 end = start.add(this.ray(camera, mouseX, mouseY).scale(PICK_RANGE));
      BlockHitResult blockHit = this.pickBlock(mouseX, mouseY);
      Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
      EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
         this.minecraft.level,
         this.minecraft.player,
         start,
         entityEnd,
         new AABB(start, entityEnd).inflate(1.0),
         entity -> entity instanceof LivingEntity && entity.isPickable() && !entity.isSpectator()
      );
      return entityHit != null ? entityHit : blockHit;
   }

   private Vec3 ray(Camera camera, double mouseX, double mouseY) {
      double tan = Math.tan(Math.toRadians(this.minecraft.options.fov().get()) / 2.0);
      double aspect = (double)this.minecraft.getWindow().getWidth() / (double)this.minecraft.getWindow().getHeight();
      double ndcX = mouseX / this.width * 2.0 - 1.0;
      double ndcY = 1.0 - mouseY / this.height * 2.0;
      Vector3f look = camera.getLookVector();
      Vector3f up = camera.getUpVector();
      Vector3f left = camera.getLeftVector();
      return new Vec3(
            look.x() - left.x() * ndcX * tan * aspect + up.x() * ndcY * tan,
            look.y() - left.y() * ndcX * tan * aspect + up.y() * ndcY * tan,
            look.z() - left.z() * ndcX * tan * aspect + up.z() * ndcY * tan
         )
         .normalize();
   }

   /** Screen position of a world point in GUI coordinates, or null when it is behind the camera. */
   private double[] project(Camera camera, Vec3 point) {
      Vec3 relative = point.subtract(camera.getPosition());
      Vector3f look = camera.getLookVector();
      Vector3f up = camera.getUpVector();
      Vector3f left = camera.getLeftVector();
      double depth = relative.x * look.x() + relative.y * look.y() + relative.z * look.z();
      if (depth <= 0.1) {
         return null;
      } else {
         double tan = Math.tan(Math.toRadians(this.minecraft.options.fov().get()) / 2.0);
         double aspect = (double)this.minecraft.getWindow().getWidth() / (double)this.minecraft.getWindow().getHeight();
         double right = -(relative.x * left.x() + relative.y * left.y() + relative.z * left.z());
         double upward = relative.x * up.x() + relative.y * up.y() + relative.z * up.z();
         double ndcX = right / (depth * tan * aspect);
         double ndcY = upward / (depth * tan);
         return new double[]{(ndcX + 1.0) / 2.0 * this.width, (1.0 - ndcY) / 2.0 * this.height};
      }
   }

   private static double axis(long window, KeyMapping positive, KeyMapping negative) {
      return isDown(window, positive.getKey().getValue()) - isDown(window, negative.getKey().getValue());
   }

   private static int isDown(long window, int key) {
      return key > 0 && InputConstants.isKeyDown(window, key) ? 1 : 0;
   }

   static ItemStack heldBaton(Minecraft minecraft) {
      if (minecraft.player == null) {
         return ItemStack.EMPTY;
      } else {
         ItemStack main = minecraft.player.getMainHandItem();
         return main.getItem() instanceof CommandBatonItem ? main : minecraft.player.getOffhandItem();
      }
   }

   static boolean isOpen(Minecraft minecraft) {
      return minecraft.screen instanceof CommandViewScreen;
   }

   static void open(Minecraft minecraft, KeyMapping toggleKey) {
      if (!net.shadowmage.ancientwarfare.core.client.ClientResearch.require(net.shadowmage.ancientwarfare.core.research.PlayerResearch.FIELD_COMMAND)) {
         return;
      }

      minecraft.setScreen(new CommandViewScreen(toggleKey));
   }
}
