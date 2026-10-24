package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.shadowmage.ancientwarfare.core.entity.GateEntity;
import org.joml.AxisAngle4f;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class GateEntityRenderer extends EntityRenderer<GateEntity> {
   private static final ResourceLocation WOOD_SPRITE = new ResourceLocation("ancientwarfare", "block/gate/wood_gate");
   private static final ResourceLocation METAL_SPRITE = new ResourceLocation("minecraft", "block/iron_block");
   private static final float PANEL_THICKNESS = 0.1875F;
   private static final float FRAME_THICKNESS = 0.375F;
   private static final float STRIP_THICKNESS = 0.125F;
   private static final float IRON_POST_THICKNESS = 0.3125F;
   private static final float IRON_RAIL_THICKNESS = 0.25F;
   private static final float IRON_BAR_THICKNESS = 0.1875F;
   private static final float DRAWBRIDGE_THICKNESS = 0.25F;
   private static final float TOP_HIDE_MARGIN = 0.475F;
   private static final ThreadLocal<Integer> CURRENT_OVERLAY = ThreadLocal.withInitial(() -> OverlayTexture.NO_OVERLAY);

   public GateEntityRenderer(Context context) {
      super(context);
      this.shadowRadius = 0.0F;
   }

   public void render(GateEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      TextureAtlasSprite woodSprite = (TextureAtlasSprite)Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WOOD_SPRITE);
      TextureAtlasSprite metalSprite = (TextureAtlasSprite)Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(METAL_SPRITE);
      VertexConsumer consumer = bufferSource.getBuffer(RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS));
      int width = entity.widthBlocks();
      int height = entity.heightBlocks();
      float openProgress = entity.openProgress(partialTick);
      poseStack.pushPose();
      CURRENT_OVERLAY.set(
         entity.hurtTime() > 0 ? OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(true)) : OverlayTexture.NO_OVERLAY
      );

      try {
         renderGate(entity, consumer, woodSprite, metalSprite, poseStack, width, height, openProgress, packedLight);
      } finally {
         CURRENT_OVERLAY.remove();
      }

      poseStack.popPose();
      super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
   }

   public ResourceLocation getTextureLocation(GateEntity entity) {
      return InventoryMenu.BLOCK_ATLAS;
   }

   private static void renderGate(
      GateEntity entity,
      VertexConsumer consumer,
      TextureAtlasSprite wood,
      TextureAtlasSprite metal,
      PoseStack poseStack,
      int width,
      int height,
      float yOffset,
      int packedLight
   ) {
      float halfWidth = (float)width * 0.5F;
      if (entity.openingStyle() == GateEntity.OpeningStyle.DRAWBRIDGE) {
         renderDrawbridge(entity, consumer, wood, metal, poseStack, width, height, halfWidth, yOffset, packedLight);
      } else if (entity.openingStyle() == GateEntity.OpeningStyle.SINGLE_SLIDING || entity.openingStyle() == GateEntity.OpeningStyle.DOUBLE_SLIDING) {
         renderSlidingGate(entity, consumer, wood, metal, poseStack, width, height, halfWidth, yOffset, packedLight);
      } else if (entity.material() == GateEntity.Material.IRON) {
         if (entity.isXAxisGate()) {
            renderIronXAxisGate(consumer, metal, poseStack, width, height, halfWidth, yOffset, packedLight);
         } else {
            renderIronZAxisGate(consumer, metal, poseStack, width, height, halfWidth, yOffset, packedLight);
         }
      } else {
         if (entity.isXAxisGate()) {
            renderXAxisGate(consumer, wood, metal, poseStack, width, height, halfWidth, yOffset, packedLight);
         } else {
            renderZAxisGate(consumer, wood, metal, poseStack, width, height, halfWidth, yOffset, packedLight);
         }
      }
   }

   private static void renderSlidingGate(
      GateEntity entity,
      VertexConsumer consumer,
      TextureAtlasSprite wood,
      TextureAtlasSprite metal,
      PoseStack poseStack,
      int width,
      int height,
      float halfWidth,
      float slideOffset,
      int packedLight
   ) {
      if (entity.material() == GateEntity.Material.IRON) {
         if (entity.isXAxisGate()) {
            renderIronSlidingXAxisGate(entity, consumer, metal, poseStack, width, height, halfWidth, slideOffset, packedLight);
         } else {
            renderIronSlidingZAxisGate(entity, consumer, metal, poseStack, width, height, halfWidth, slideOffset, packedLight);
         }
      } else if (entity.isXAxisGate()) {
         renderWoodSlidingXAxisGate(entity, consumer, wood, metal, poseStack, width, height, halfWidth, slideOffset, packedLight);
      } else {
         renderWoodSlidingZAxisGate(entity, consumer, wood, metal, poseStack, width, height, halfWidth, slideOffset, packedLight);
      }
   }

   private static void renderDrawbridge(
      GateEntity entity,
      VertexConsumer consumer,
      TextureAtlasSprite wood,
      TextureAtlasSprite metal,
      PoseStack poseStack,
      int width,
      int height,
      float halfWidth,
      float angleDegrees,
      int packedLight
   ) {
      if (entity.isXAxisGate()) {
         renderWoodXAxisFrame(consumer, wood, poseStack, height, halfWidth, packedLight);
         renderCuboid(poseStack, consumer, metal, -halfWidth - 0.25F, 0.1875F, -0.3125F, halfWidth + 0.25F, 0.4375F, 0.3125F, packedLight);
         poseStack.pushPose();
         float sign = entity.facing().getStepZ() >= 0 ? -1.0F : 1.0F;
         poseStack.mulPose(new Quaternionf(new AxisAngle4f((float)Math.toRadians((double)(angleDegrees * sign)), 1.0F, 0.0F, 0.0F)));
         renderDrawbridgeXAxisDeck(consumer, wood, metal, poseStack, height, halfWidth, packedLight);
         poseStack.popPose();
      } else {
         renderWoodZAxisFrame(consumer, wood, poseStack, height, halfWidth, packedLight);
         renderCuboid(poseStack, consumer, metal, -0.3125F, 0.1875F, -halfWidth - 0.25F, 0.3125F, 0.4375F, halfWidth + 0.25F, packedLight);
         poseStack.pushPose();
         float sign = entity.facing().getStepX() >= 0 ? 1.0F : -1.0F;
         poseStack.mulPose(new Quaternionf(new AxisAngle4f((float)Math.toRadians((double)(angleDegrees * sign)), 0.0F, 0.0F, 1.0F)));
         renderDrawbridgeZAxisDeck(consumer, wood, metal, poseStack, height, halfWidth, packedLight);
         poseStack.popPose();
      }
   }

   private static void renderDrawbridgeXAxisDeck(
      VertexConsumer consumer, TextureAtlasSprite wood, TextureAtlasSprite metal, PoseStack poseStack, int height, float halfWidth, int packedLight
   ) {
      renderCuboid(poseStack, consumer, wood, -halfWidth + 0.04F, 0.35F, -0.25F, halfWidth - 0.04F, (float)height, 0.25F, packedLight);
      renderCuboid(poseStack, consumer, wood, -halfWidth - 0.125F, 0.35F, -0.375F, -halfWidth + 0.125F, (float)height, 0.375F, packedLight);
      renderCuboid(poseStack, consumer, wood, halfWidth - 0.125F, 0.35F, -0.375F, halfWidth + 0.125F, (float)height, 0.375F, packedLight);
      renderCuboid(poseStack, consumer, metal, -halfWidth, (float)height * 0.52F, -0.3125F, halfWidth, (float)height * 0.52F + 0.25F, 0.3125F, packedLight);
      renderCuboid(poseStack, consumer, metal, -halfWidth, (float)height - 0.3125F, -0.375F, halfWidth, (float)height, 0.375F, packedLight);
   }

   private static void renderDrawbridgeZAxisDeck(
      VertexConsumer consumer, TextureAtlasSprite wood, TextureAtlasSprite metal, PoseStack poseStack, int height, float halfWidth, int packedLight
   ) {
      renderCuboid(poseStack, consumer, wood, -0.25F, 0.35F, -halfWidth + 0.04F, 0.25F, (float)height, halfWidth - 0.04F, packedLight);
      renderCuboid(poseStack, consumer, wood, -0.375F, 0.35F, -halfWidth - 0.125F, 0.375F, (float)height, -halfWidth + 0.125F, packedLight);
      renderCuboid(poseStack, consumer, wood, -0.375F, 0.35F, halfWidth - 0.125F, 0.375F, (float)height, halfWidth + 0.125F, packedLight);
      renderCuboid(poseStack, consumer, metal, -0.3125F, (float)height * 0.52F, -halfWidth, 0.3125F, (float)height * 0.52F + 0.25F, halfWidth, packedLight);
      renderCuboid(poseStack, consumer, metal, -0.375F, (float)height - 0.3125F, -halfWidth, 0.375F, (float)height, halfWidth, packedLight);
   }

   private static void renderXAxisGate(
      VertexConsumer consumer,
      TextureAtlasSprite wood,
      TextureAtlasSprite metal,
      PoseStack poseStack,
      int width,
      int height,
      float halfWidth,
      float yOffset,
      int packedLight
   ) {
      renderCuboid(poseStack, consumer, wood, -halfWidth - 0.375F, 0.0F, -0.375F, -halfWidth, (float)height + 0.25F, 0.375F, packedLight);
      renderCuboid(poseStack, consumer, wood, halfWidth, 0.0F, -0.375F, halfWidth + 0.375F, (float)height + 0.25F, 0.375F, packedLight);
      renderCuboid(poseStack, consumer, wood, -halfWidth - 0.375F, (float)height, -0.375F, halfWidth + 0.375F, (float)height + 0.375F, 0.375F, packedLight);
      renderCuboid(poseStack, consumer, wood, -halfWidth, 0.0F, -0.1875F, halfWidth, 0.25F, 0.1875F, packedLight);

      for (int x = 0; x < width; x++) {
         float minX = -halfWidth + (float)x;
         float maxX = minX + 1.0F;

         for (int y = 0; y < height; y++) {
            renderMovingCuboid(
               poseStack,
               consumer,
               wood,
               minX + 0.03F,
               (float)y + yOffset + 0.03F,
               -0.1875F,
               maxX - 0.03F,
               (float)y + yOffset + 0.97F,
               0.1875F,
               height,
               packedLight
            );
         }
      }

      renderMovingCuboid(
         poseStack,
         consumer,
         wood,
         -halfWidth + (float)width / 3.0F - 0.125F,
         yOffset,
         -0.25F,
         -halfWidth + (float)width / 3.0F + 0.125F,
         yOffset + (float)height,
         0.25F,
         height,
         packedLight
      );
      renderMovingCuboid(
         poseStack,
         consumer,
         wood,
         halfWidth - (float)width / 3.0F - 0.125F,
         yOffset,
         -0.25F,
         halfWidth - (float)width / 3.0F + 0.125F,
         yOffset + (float)height,
         0.25F,
         height,
         packedLight
      );
      renderMovingCuboid(
         poseStack,
         consumer,
         wood,
         -halfWidth,
         yOffset + (float)height * 0.55F,
         -0.25F,
         halfWidth,
         yOffset + (float)height * 0.55F + 0.18F,
         0.25F,
         height,
         packedLight
      );
      renderMovingCuboid(
         poseStack,
         consumer,
         wood,
         -halfWidth,
         yOffset + (float)height * 0.82F,
         -0.25F,
         halfWidth,
         yOffset + (float)height * 0.82F + 0.18F,
         0.25F,
         height,
         packedLight
      );
      renderMovingCuboid(
         poseStack,
         consumer,
         metal,
         -halfWidth - 0.04F,
         yOffset + (float)height * 0.32F,
         -0.24F,
         -halfWidth + 0.16F,
         yOffset + (float)height * 0.32F + 0.16F,
         0.24F,
         height,
         packedLight
      );
      renderMovingCuboid(
         poseStack,
         consumer,
         metal,
         halfWidth - 0.16F,
         yOffset + (float)height * 0.32F,
         -0.24F,
         halfWidth + 0.04F,
         yOffset + (float)height * 0.32F + 0.16F,
         0.24F,
         height,
         packedLight
      );
   }

   private static void renderZAxisGate(
      VertexConsumer consumer,
      TextureAtlasSprite wood,
      TextureAtlasSprite metal,
      PoseStack poseStack,
      int width,
      int height,
      float halfWidth,
      float yOffset,
      int packedLight
   ) {
      renderCuboid(poseStack, consumer, wood, -0.375F, 0.0F, -halfWidth - 0.375F, 0.375F, (float)height + 0.25F, -halfWidth, packedLight);
      renderCuboid(poseStack, consumer, wood, -0.375F, 0.0F, halfWidth, 0.375F, (float)height + 0.25F, halfWidth + 0.375F, packedLight);
      renderCuboid(poseStack, consumer, wood, -0.375F, (float)height, -halfWidth - 0.375F, 0.375F, (float)height + 0.375F, halfWidth + 0.375F, packedLight);
      renderCuboid(poseStack, consumer, wood, -0.1875F, 0.0F, -halfWidth, 0.1875F, 0.25F, halfWidth, packedLight);

      for (int z = 0; z < width; z++) {
         float minZ = -halfWidth + (float)z;
         float maxZ = minZ + 1.0F;

         for (int y = 0; y < height; y++) {
            renderMovingCuboid(
               poseStack,
               consumer,
               wood,
               -0.1875F,
               (float)y + yOffset + 0.03F,
               minZ + 0.03F,
               0.1875F,
               (float)y + yOffset + 0.97F,
               maxZ - 0.03F,
               height,
               packedLight
            );
         }
      }

      renderMovingCuboid(
         poseStack,
         consumer,
         wood,
         -0.25F,
         yOffset,
         -halfWidth + (float)width / 3.0F - 0.125F,
         0.25F,
         yOffset + (float)height,
         -halfWidth + (float)width / 3.0F + 0.125F,
         height,
         packedLight
      );
      renderMovingCuboid(
         poseStack,
         consumer,
         wood,
         -0.25F,
         yOffset,
         halfWidth - (float)width / 3.0F - 0.125F,
         0.25F,
         yOffset + (float)height,
         halfWidth - (float)width / 3.0F + 0.125F,
         height,
         packedLight
      );
      renderMovingCuboid(
         poseStack,
         consumer,
         wood,
         -0.25F,
         yOffset + (float)height * 0.55F,
         -halfWidth,
         0.25F,
         yOffset + (float)height * 0.55F + 0.18F,
         halfWidth,
         height,
         packedLight
      );
      renderMovingCuboid(
         poseStack,
         consumer,
         wood,
         -0.25F,
         yOffset + (float)height * 0.82F,
         -halfWidth,
         0.25F,
         yOffset + (float)height * 0.82F + 0.18F,
         halfWidth,
         height,
         packedLight
      );
      renderMovingCuboid(
         poseStack,
         consumer,
         metal,
         -0.24F,
         yOffset + (float)height * 0.32F,
         -halfWidth - 0.04F,
         0.24F,
         yOffset + (float)height * 0.32F + 0.16F,
         -halfWidth + 0.16F,
         height,
         packedLight
      );
      renderMovingCuboid(
         poseStack,
         consumer,
         metal,
         -0.24F,
         yOffset + (float)height * 0.32F,
         halfWidth - 0.16F,
         0.24F,
         yOffset + (float)height * 0.32F + 0.16F,
         halfWidth + 0.04F,
         height,
         packedLight
      );
   }

   private static void renderWoodSlidingXAxisGate(
      GateEntity entity,
      VertexConsumer consumer,
      TextureAtlasSprite wood,
      TextureAtlasSprite metal,
      PoseStack poseStack,
      int width,
      int height,
      float halfWidth,
      float slideOffset,
      int packedLight
   ) {
      renderWoodXAxisFrame(consumer, wood, poseStack, height, halfWidth, packedLight);
      if (entity.openingStyle() == GateEntity.OpeningStyle.DOUBLE_SLIDING) {
         renderWoodXAxisLeaf(consumer, wood, metal, poseStack, -halfWidth, 0.0F, height, -slideOffset, packedLight);
         renderWoodXAxisLeaf(consumer, wood, metal, poseStack, 0.0F, halfWidth, height, slideOffset, packedLight);
      } else {
         renderWoodXAxisLeaf(consumer, wood, metal, poseStack, -halfWidth, halfWidth, height, slideOffset, packedLight);
      }
   }

   private static void renderWoodSlidingZAxisGate(
      GateEntity entity,
      VertexConsumer consumer,
      TextureAtlasSprite wood,
      TextureAtlasSprite metal,
      PoseStack poseStack,
      int width,
      int height,
      float halfWidth,
      float slideOffset,
      int packedLight
   ) {
      renderWoodZAxisFrame(consumer, wood, poseStack, height, halfWidth, packedLight);
      if (entity.openingStyle() == GateEntity.OpeningStyle.DOUBLE_SLIDING) {
         renderWoodZAxisLeaf(consumer, wood, metal, poseStack, -halfWidth, 0.0F, height, -slideOffset, packedLight);
         renderWoodZAxisLeaf(consumer, wood, metal, poseStack, 0.0F, halfWidth, height, slideOffset, packedLight);
      } else {
         renderWoodZAxisLeaf(consumer, wood, metal, poseStack, -halfWidth, halfWidth, height, slideOffset, packedLight);
      }
   }

   private static void renderWoodXAxisFrame(VertexConsumer consumer, TextureAtlasSprite wood, PoseStack poseStack, int height, float halfWidth, int packedLight) {
      renderCuboid(poseStack, consumer, wood, -halfWidth - 0.375F, 0.0F, -0.375F, -halfWidth, (float)height + 0.25F, 0.375F, packedLight);
      renderCuboid(poseStack, consumer, wood, halfWidth, 0.0F, -0.375F, halfWidth + 0.375F, (float)height + 0.25F, 0.375F, packedLight);
      renderCuboid(poseStack, consumer, wood, -halfWidth - 0.375F, (float)height, -0.375F, halfWidth + 0.375F, (float)height + 0.375F, 0.375F, packedLight);
      renderCuboid(poseStack, consumer, wood, -halfWidth, 0.0F, -0.1875F, halfWidth, 0.25F, 0.1875F, packedLight);
   }

   private static void renderWoodZAxisFrame(VertexConsumer consumer, TextureAtlasSprite wood, PoseStack poseStack, int height, float halfWidth, int packedLight) {
      renderCuboid(poseStack, consumer, wood, -0.375F, 0.0F, -halfWidth - 0.375F, 0.375F, (float)height + 0.25F, -halfWidth, packedLight);
      renderCuboid(poseStack, consumer, wood, -0.375F, 0.0F, halfWidth, 0.375F, (float)height + 0.25F, halfWidth + 0.375F, packedLight);
      renderCuboid(poseStack, consumer, wood, -0.375F, (float)height, -halfWidth - 0.375F, 0.375F, (float)height + 0.375F, halfWidth + 0.375F, packedLight);
      renderCuboid(poseStack, consumer, wood, -0.1875F, 0.0F, -halfWidth, 0.1875F, 0.25F, halfWidth, packedLight);
   }

   private static void renderWoodXAxisLeaf(
      VertexConsumer consumer,
      TextureAtlasSprite wood,
      TextureAtlasSprite metal,
      PoseStack poseStack,
      float minX,
      float maxX,
      int height,
      float xOffset,
      int packedLight
   ) {
      for (float x = minX; x < maxX; x++) {
         float nextX = Math.min(x + 1.0F, maxX);

         for (int y = 0; y < height; y++) {
            renderCuboid(
               poseStack, consumer, wood, x + xOffset + 0.03F, (float)y + 0.03F, -0.1875F, nextX + xOffset - 0.03F, (float)y + 0.97F, 0.1875F, packedLight
            );
         }
      }

      float width = maxX - minX;
      renderCuboid(poseStack, consumer, wood, minX + xOffset, (float)height * 0.55F, -0.25F, maxX + xOffset, (float)height * 0.55F + 0.18F, 0.25F, packedLight);
      renderCuboid(poseStack, consumer, wood, minX + xOffset, (float)height * 0.82F, -0.25F, maxX + xOffset, (float)height * 0.82F + 0.18F, 0.25F, packedLight);
      renderCuboid(
         poseStack,
         consumer,
         wood,
         minX + xOffset + width * 0.33F - 0.125F,
         0.0F,
         -0.25F,
         minX + xOffset + width * 0.33F + 0.125F,
         (float)height,
         0.25F,
         packedLight
      );
      renderCuboid(
         poseStack,
         consumer,
         metal,
         minX + xOffset + 0.08F,
         (float)height * 0.32F,
         -0.24F,
         minX + xOffset + 0.28F,
         (float)height * 0.32F + 0.16F,
         0.24F,
         packedLight
      );
   }

   private static void renderWoodZAxisLeaf(
      VertexConsumer consumer,
      TextureAtlasSprite wood,
      TextureAtlasSprite metal,
      PoseStack poseStack,
      float minZ,
      float maxZ,
      int height,
      float zOffset,
      int packedLight
   ) {
      for (float z = minZ; z < maxZ; z++) {
         float nextZ = Math.min(z + 1.0F, maxZ);

         for (int y = 0; y < height; y++) {
            renderCuboid(
               poseStack, consumer, wood, -0.1875F, (float)y + 0.03F, z + zOffset + 0.03F, 0.1875F, (float)y + 0.97F, nextZ + zOffset - 0.03F, packedLight
            );
         }
      }

      float width = maxZ - minZ;
      renderCuboid(poseStack, consumer, wood, -0.25F, (float)height * 0.55F, minZ + zOffset, 0.25F, (float)height * 0.55F + 0.18F, maxZ + zOffset, packedLight);
      renderCuboid(poseStack, consumer, wood, -0.25F, (float)height * 0.82F, minZ + zOffset, 0.25F, (float)height * 0.82F + 0.18F, maxZ + zOffset, packedLight);
      renderCuboid(
         poseStack,
         consumer,
         wood,
         -0.25F,
         0.0F,
         minZ + zOffset + width * 0.33F - 0.125F,
         0.25F,
         (float)height,
         minZ + zOffset + width * 0.33F + 0.125F,
         packedLight
      );
      renderCuboid(
         poseStack,
         consumer,
         metal,
         -0.24F,
         (float)height * 0.32F,
         minZ + zOffset + 0.08F,
         0.24F,
         (float)height * 0.32F + 0.16F,
         minZ + zOffset + 0.28F,
         packedLight
      );
   }

   private static void renderIronXAxisGate(
      VertexConsumer consumer, TextureAtlasSprite metal, PoseStack poseStack, int width, int height, float halfWidth, float yOffset, int packedLight
   ) {
      renderCuboid(poseStack, consumer, metal, -halfWidth - 0.3125F, 0.0F, -0.25F, -halfWidth, (float)height + 0.25F, 0.25F, packedLight);
      renderCuboid(poseStack, consumer, metal, halfWidth, 0.0F, -0.25F, halfWidth + 0.3125F, (float)height + 0.25F, 0.25F, packedLight);
      renderCuboid(
         poseStack, consumer, metal, -halfWidth - 0.3125F, (float)height - 0.0625F, -0.3125F, halfWidth + 0.3125F, (float)height + 0.375F, 0.3125F, packedLight
      );
      renderMovingCuboid(
         poseStack, consumer, metal, -halfWidth, yOffset + 0.125F, -0.21875F, halfWidth, yOffset + 0.25F + 0.125F, 0.21875F, height, packedLight
      );
      renderMovingCuboid(
         poseStack,
         consumer,
         metal,
         -halfWidth,
         yOffset + (float)height * 0.48F,
         -0.21875F,
         halfWidth,
         yOffset + (float)height * 0.48F + 0.25F,
         0.21875F,
         height,
         packedLight
      );
      renderMovingCuboid(
         poseStack,
         consumer,
         metal,
         -halfWidth,
         yOffset + (float)height * 0.8F,
         -0.21875F,
         halfWidth,
         yOffset + (float)height * 0.8F + 0.25F,
         0.21875F,
         height,
         packedLight
      );

      for (int i = 0; i < width * 2; i++) {
         float x = -halfWidth + ((float)i + 0.5F) * 0.5F;
         renderMovingCuboid(
            poseStack, consumer, metal, x - 0.09375F, yOffset + 0.25F, -0.15625F, x + 0.09375F, yOffset + (float)height, 0.15625F, height, packedLight
         );
      }
   }

   private static void renderIronZAxisGate(
      VertexConsumer consumer, TextureAtlasSprite metal, PoseStack poseStack, int width, int height, float halfWidth, float yOffset, int packedLight
   ) {
      renderCuboid(poseStack, consumer, metal, -0.25F, 0.0F, -halfWidth - 0.3125F, 0.25F, (float)height + 0.25F, -halfWidth, packedLight);
      renderCuboid(poseStack, consumer, metal, -0.25F, 0.0F, halfWidth, 0.25F, (float)height + 0.25F, halfWidth + 0.3125F, packedLight);
      renderCuboid(
         poseStack, consumer, metal, -0.3125F, (float)height - 0.0625F, -halfWidth - 0.3125F, 0.3125F, (float)height + 0.375F, halfWidth + 0.3125F, packedLight
      );
      renderMovingCuboid(
         poseStack, consumer, metal, -0.21875F, yOffset + 0.125F, -halfWidth, 0.21875F, yOffset + 0.25F + 0.125F, halfWidth, height, packedLight
      );
      renderMovingCuboid(
         poseStack,
         consumer,
         metal,
         -0.21875F,
         yOffset + (float)height * 0.48F,
         -halfWidth,
         0.21875F,
         yOffset + (float)height * 0.48F + 0.25F,
         halfWidth,
         height,
         packedLight
      );
      renderMovingCuboid(
         poseStack,
         consumer,
         metal,
         -0.21875F,
         yOffset + (float)height * 0.8F,
         -halfWidth,
         0.21875F,
         yOffset + (float)height * 0.8F + 0.25F,
         halfWidth,
         height,
         packedLight
      );

      for (int i = 0; i < width * 2; i++) {
         float z = -halfWidth + ((float)i + 0.5F) * 0.5F;
         renderMovingCuboid(
            poseStack, consumer, metal, -0.15625F, yOffset + 0.25F, z - 0.09375F, 0.15625F, yOffset + (float)height, z + 0.09375F, height, packedLight
         );
      }
   }

   private static void renderIronSlidingXAxisGate(
      GateEntity entity,
      VertexConsumer consumer,
      TextureAtlasSprite metal,
      PoseStack poseStack,
      int width,
      int height,
      float halfWidth,
      float slideOffset,
      int packedLight
   ) {
      renderIronXAxisFrame(consumer, metal, poseStack, height, halfWidth, packedLight);
      if (entity.openingStyle() == GateEntity.OpeningStyle.DOUBLE_SLIDING) {
         renderIronXAxisLeaf(consumer, metal, poseStack, -halfWidth, 0.0F, height, -slideOffset, packedLight);
         renderIronXAxisLeaf(consumer, metal, poseStack, 0.0F, halfWidth, height, slideOffset, packedLight);
      } else {
         renderIronXAxisLeaf(consumer, metal, poseStack, -halfWidth, halfWidth, height, slideOffset, packedLight);
      }
   }

   private static void renderIronSlidingZAxisGate(
      GateEntity entity,
      VertexConsumer consumer,
      TextureAtlasSprite metal,
      PoseStack poseStack,
      int width,
      int height,
      float halfWidth,
      float slideOffset,
      int packedLight
   ) {
      renderIronZAxisFrame(consumer, metal, poseStack, height, halfWidth, packedLight);
      if (entity.openingStyle() == GateEntity.OpeningStyle.DOUBLE_SLIDING) {
         renderIronZAxisLeaf(consumer, metal, poseStack, -halfWidth, 0.0F, height, -slideOffset, packedLight);
         renderIronZAxisLeaf(consumer, metal, poseStack, 0.0F, halfWidth, height, slideOffset, packedLight);
      } else {
         renderIronZAxisLeaf(consumer, metal, poseStack, -halfWidth, halfWidth, height, slideOffset, packedLight);
      }
   }

   private static void renderIronXAxisFrame(
      VertexConsumer consumer, TextureAtlasSprite metal, PoseStack poseStack, int height, float halfWidth, int packedLight
   ) {
      renderCuboid(poseStack, consumer, metal, -halfWidth - 0.3125F, 0.0F, -0.25F, -halfWidth, (float)height + 0.25F, 0.25F, packedLight);
      renderCuboid(poseStack, consumer, metal, halfWidth, 0.0F, -0.25F, halfWidth + 0.3125F, (float)height + 0.25F, 0.25F, packedLight);
      renderCuboid(
         poseStack, consumer, metal, -halfWidth - 0.3125F, (float)height - 0.0625F, -0.3125F, halfWidth + 0.3125F, (float)height + 0.375F, 0.3125F, packedLight
      );
   }

   private static void renderIronZAxisFrame(
      VertexConsumer consumer, TextureAtlasSprite metal, PoseStack poseStack, int height, float halfWidth, int packedLight
   ) {
      renderCuboid(poseStack, consumer, metal, -0.25F, 0.0F, -halfWidth - 0.3125F, 0.25F, (float)height + 0.25F, -halfWidth, packedLight);
      renderCuboid(poseStack, consumer, metal, -0.25F, 0.0F, halfWidth, 0.25F, (float)height + 0.25F, halfWidth + 0.3125F, packedLight);
      renderCuboid(
         poseStack, consumer, metal, -0.3125F, (float)height - 0.0625F, -halfWidth - 0.3125F, 0.3125F, (float)height + 0.375F, halfWidth + 0.3125F, packedLight
      );
   }

   private static void renderIronXAxisLeaf(
      VertexConsumer consumer, TextureAtlasSprite metal, PoseStack poseStack, float minX, float maxX, int height, float xOffset, int packedLight
   ) {
      renderCuboid(poseStack, consumer, metal, minX + xOffset, 0.125F, -0.21875F, maxX + xOffset, 0.375F, 0.21875F, packedLight);
      renderCuboid(
         poseStack, consumer, metal, minX + xOffset, (float)height * 0.48F, -0.21875F, maxX + xOffset, (float)height * 0.48F + 0.25F, 0.21875F, packedLight
      );
      renderCuboid(
         poseStack, consumer, metal, minX + xOffset, (float)height * 0.8F, -0.21875F, maxX + xOffset, (float)height * 0.8F + 0.25F, 0.21875F, packedLight
      );
      int bars = Math.max(1, Math.round((maxX - minX) * 2.0F));

      for (int i = 0; i < bars; i++) {
         float x = minX + ((float)i + 0.5F) * ((maxX - minX) / (float)bars) + xOffset;
         renderCuboid(poseStack, consumer, metal, x - 0.09375F, 0.25F, -0.15625F, x + 0.09375F, (float)height, 0.15625F, packedLight);
      }
   }

   private static void renderIronZAxisLeaf(
      VertexConsumer consumer, TextureAtlasSprite metal, PoseStack poseStack, float minZ, float maxZ, int height, float zOffset, int packedLight
   ) {
      renderCuboid(poseStack, consumer, metal, -0.21875F, 0.125F, minZ + zOffset, 0.21875F, 0.375F, maxZ + zOffset, packedLight);
      renderCuboid(
         poseStack, consumer, metal, -0.21875F, (float)height * 0.48F, minZ + zOffset, 0.21875F, (float)height * 0.48F + 0.25F, maxZ + zOffset, packedLight
      );
      renderCuboid(
         poseStack, consumer, metal, -0.21875F, (float)height * 0.8F, minZ + zOffset, 0.21875F, (float)height * 0.8F + 0.25F, maxZ + zOffset, packedLight
      );
      int bars = Math.max(1, Math.round((maxZ - minZ) * 2.0F));

      for (int i = 0; i < bars; i++) {
         float z = minZ + ((float)i + 0.5F) * ((maxZ - minZ) / (float)bars) + zOffset;
         renderCuboid(poseStack, consumer, metal, -0.15625F, 0.25F, z - 0.09375F, 0.15625F, (float)height, z + 0.09375F, packedLight);
      }
   }

   private static void renderMovingCuboid(
      PoseStack poseStack,
      VertexConsumer consumer,
      TextureAtlasSprite sprite,
      float minX,
      float minY,
      float minZ,
      float maxX,
      float maxY,
      float maxZ,
      int gateHeight,
      int packedLight
   ) {
      float visibleTop = (float)gateHeight - 0.475F;
      if (!(minY > visibleTop)) {
         renderCuboid(poseStack, consumer, sprite, minX, minY, minZ, maxX, Math.min(maxY, visibleTop), maxZ, packedLight);
      }
   }

   private static void renderCuboid(
      PoseStack poseStack,
      VertexConsumer consumer,
      TextureAtlasSprite sprite,
      float minX,
      float minY,
      float minZ,
      float maxX,
      float maxY,
      float maxZ,
      int packedLight
   ) {
      Pose pose = poseStack.last();
      quad(pose, consumer, sprite, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0.0F, 0.0F, 1.0F, packedLight);
      quad(pose, consumer, sprite, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, 0.0F, 0.0F, -1.0F, packedLight);
      quad(pose, consumer, sprite, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1.0F, 0.0F, 0.0F, packedLight);
      quad(pose, consumer, sprite, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, 1.0F, 0.0F, 0.0F, packedLight);
      quad(pose, consumer, sprite, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, 0.0F, 1.0F, 0.0F, packedLight);
      quad(pose, consumer, sprite, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0.0F, -1.0F, 0.0F, packedLight);
   }

   private static void quad(
      Pose pose,
      VertexConsumer consumer,
      TextureAtlasSprite sprite,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float x4,
      float y4,
      float z4,
      float normalX,
      float normalY,
      float normalZ,
      int packedLight
   ) {
      vertex(pose.pose(), pose.normal(), consumer, sprite, x1, y1, z1, 0.0F, 0.0F, normalX, normalY, normalZ, packedLight);
      vertex(pose.pose(), pose.normal(), consumer, sprite, x2, y2, z2, 1.0F, 0.0F, normalX, normalY, normalZ, packedLight);
      vertex(pose.pose(), pose.normal(), consumer, sprite, x3, y3, z3, 1.0F, 1.0F, normalX, normalY, normalZ, packedLight);
      vertex(pose.pose(), pose.normal(), consumer, sprite, x4, y4, z4, 0.0F, 1.0F, normalX, normalY, normalZ, packedLight);
   }

   private static void vertex(
      Matrix4f matrix,
      Matrix3f normalMatrix,
      VertexConsumer consumer,
      TextureAtlasSprite sprite,
      float x,
      float y,
      float z,
      float u,
      float v,
      float normalX,
      float normalY,
      float normalZ,
      int packedLight
   ) {
      consumer.vertex(matrix, x, y, z)
         .color(255, 255, 255, 255)
         .uv(sprite.getU((double)(u * 16.0F)), sprite.getV((double)(v * 16.0F)))
         .overlayCoords(CURRENT_OVERLAY.get())
         .uv2(packedLight)
         .normal(normalMatrix, normalX, normalY, normalZ)
         .endVertex();
   }
}
