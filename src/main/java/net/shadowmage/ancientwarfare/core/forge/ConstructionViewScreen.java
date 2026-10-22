package net.shadowmage.ancientwarfare.core.forge;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.client.ConstructionClient;
import net.shadowmage.ancientwarfare.core.client.ConstructionScreen;
import net.shadowmage.ancientwarfare.core.item.ConstructionWandItem;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/**
 * The building view: the command view's top-down camera with the builder's wand. Drag a box over
 * the ground with the left button to mark an area and choose the work for it; the right button
 * opens the list of jobs.
 */
public class ConstructionViewScreen extends Screen {
   private static final double PICK_RANGE = 256.0;
   private static final int EDGE_SCROLL_MARGIN = 3;
   private final KeyMapping toggleKey;
   private BlockPos dragStart;
   private double mouseX;
   private double mouseY;
   private boolean mouseSeen;
   private double scroll;
   /** Set while the work window is open over this view, so the camera stays where it is. */
   private boolean keepCamera;

   ConstructionViewScreen(KeyMapping toggleKey) {
      super(Component.translatable("gui.ancientwarfare.construction.view"));
      this.toggleKey = toggleKey;
   }

   static boolean holdingWand(Minecraft minecraft) {
      return minecraft.player != null
         && (minecraft.player.getMainHandItem().getItem() instanceof ConstructionWandItem
            || minecraft.player.getOffhandItem().getItem() instanceof ConstructionWandItem);
   }

   static boolean isOpen(Minecraft minecraft) {
      return minecraft.screen instanceof ConstructionViewScreen
         || minecraft.screen instanceof ConstructionScreen screen && screen.parent() instanceof ConstructionViewScreen;
   }

   @Override
   protected void init() {
      this.keepCamera = false;
      if (!CommandViewCamera.isActive()) {
         CommandViewCamera.start(this.minecraft);
      }
   }

   @Override
   public void removed() {
      ConstructionClient.setPreview(null, null);
      if (!this.keepCamera) {
         CommandViewCamera.stop(this.minecraft);
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void tick() {
      if (this.minecraft.player == null || !this.minecraft.player.isAlive() || !holdingWand(this.minecraft)) {
         this.onClose();
         return;
      }

      long window = this.minecraft.getWindow().getWindow();
      double forward = axis(window, this.minecraft.options.keyUp, this.minecraft.options.keyDown);
      double strafe = axis(window, this.minecraft.options.keyRight, this.minecraft.options.keyLeft);
      if (this.dragStart == null && this.mouseSeen) {
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

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (this.dragStart == null) {
         // The block under the cursor is outlined so the player sees where a box would start.
         BlockPos hover = this.groundAt(mouseX, mouseY);
         ConstructionClient.setPreview(hover, hover);
      }

      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.construction.view.status", ConstructionClient.jobs().size()), 6, 6, 0xFFFFFF);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.construction.view.help1"), 6, this.height - 22, 0xFFFFFF);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.construction.view.help2"), 6, this.height - 11, 0xFFFFFF);
      super.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override
   public void mouseMoved(double mouseX, double mouseY) {
      this.mouseSeen = true;
      this.mouseX = mouseX;
      this.mouseY = mouseY;
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
         this.dragStart = this.groundAt(mouseX, mouseY);
         ConstructionClient.setPreview(this.dragStart, this.dragStart);
         return true;
      } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
         this.openWork(null, null);
         return true;
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.dragStart != null) {
         BlockPos end = this.groundAt(mouseX, mouseY);
         if (end != null) {
            ConstructionClient.setPreview(this.dragStart, end);
         }
      }

      return true;
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.dragStart != null) {
         BlockPos start = this.dragStart;
         BlockPos end = this.groundAt(mouseX, mouseY);
         this.dragStart = null;
         if (end != null) {
            this.openWork(start, end);
         }

         return true;
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }

   private void openWork(BlockPos first, BlockPos second) {
      this.keepCamera = true;
      this.minecraft.setScreen(new ConstructionScreen(first, second, this));
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
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   /** The ground block under the cursor, or null over the sky. */
   private BlockPos groundAt(double mouseX, double mouseY) {
      Camera camera = this.minecraft.gameRenderer.getMainCamera();
      Vec3 start = camera.getPosition();
      Vec3 end = start.add(this.ray(camera, mouseX, mouseY).scale(PICK_RANGE));
      BlockHitResult hit = this.minecraft.level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, this.minecraft.player));
      return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
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

   private static double axis(long window, KeyMapping positive, KeyMapping negative) {
      return isDown(window, positive.getKey().getValue()) - isDown(window, negative.getKey().getValue());
   }

   private static int isDown(long window, int key) {
      return key > 0 && InputConstants.isKeyDown(window, key) ? 1 : 0;
   }

   static void open(Minecraft minecraft, KeyMapping toggleKey) {
      minecraft.setScreen(new ConstructionViewScreen(toggleKey));
   }
}
