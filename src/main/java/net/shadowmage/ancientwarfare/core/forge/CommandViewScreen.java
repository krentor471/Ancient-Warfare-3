package net.shadowmage.ancientwarfare.core.forge;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Camera;
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
   private final KeyMapping toggleKey;
   private double dragStartX;
   private double dragStartY;
   private double mouseX;
   private double mouseY;
   private boolean dragging;
   private boolean mouseSeen;
   private double scroll;

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
      graphics.drawString(this.font, Component.translatable("guistrings.npc.command_view.help1"), 6, this.height - 33, TEXT_COLOR);
      graphics.drawString(this.font, Component.translatable("guistrings.npc.command_view.help2"), 6, this.height - 22, TEXT_COLOR);
      graphics.drawString(this.font, Component.translatable("guistrings.npc.command_view.help3"), 6, this.height - 11, TEXT_COLOR);
      super.render(graphics, mouseX, mouseY, partialTick);
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
         this.order(mouseX, mouseY);
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
      } else if (keyCode == GLFW.GLFW_KEY_G) {
         AWCoreNetwork.sendBatonFormation();
         return true;
      } else if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
         // Ctrl+number saves the selection as a squad; the number alone (or Alt+number) selects that squad.
         AWCoreNetwork.sendBatonSquad(keyCode - GLFW.GLFW_KEY_0, hasControlDown());
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   private void order(double mouseX, double mouseY) {
      HitResult hit = this.pick(mouseX, mouseY);
      boolean queued = hasShiftDown();
      if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target && !this.isOwn(target)) {
         AWCoreNetwork.sendCommandViewOrder(CommandBatonItem.ViewOrder.ATTACK, target.blockPosition(), target.getId(), false);
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

   /** The block or entity under the cursor, looking from the camera along the cursor's ray. */
   private HitResult pick(double mouseX, double mouseY) {
      Camera camera = this.minecraft.gameRenderer.getMainCamera();
      Vec3 start = camera.getPosition();
      Vec3 end = start.add(this.ray(camera, mouseX, mouseY).scale(PICK_RANGE));
      BlockHitResult blockHit = this.minecraft.level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this.minecraft.player));
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
      minecraft.setScreen(new CommandViewScreen(toggleKey));
   }
}
