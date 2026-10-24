package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.BallistaBoltEntity;
import net.shadowmage.ancientwarfare.core.item.BallistaAmmoType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class BallistaBoltRenderer extends EntityRenderer<BallistaBoltEntity> {
   private static final ResourceLocation TEXTURE = AWCoreIds.id("textures/entity/projectile/ballista_bolt.png");

   public BallistaBoltRenderer(Context context) {
      super(context);
   }

   public void render(BallistaBoltEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      poseStack.pushPose();
      float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
      float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
      poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
      poseStack.scale(1.35F, 1.35F, 1.35F);
      VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
      BallistaBoltRenderer.Palette palette = BallistaBoltRenderer.Palette.forAmmo(entity.getAmmoType());
      renderCuboid(poseStack, consumer, -0.035F, -0.035F, -0.55F, 0.035F, 0.035F, 0.42F, palette.shaft(), packedLight);
      renderCuboid(poseStack, consumer, -0.07F, -0.07F, 0.38F, 0.07F, 0.07F, 0.6F, palette.head(), packedLight);
      renderCuboid(poseStack, consumer, -0.12F, -0.015F, -0.7F, 0.12F, 0.015F, -0.5F, palette.fletching(), packedLight);
      renderCuboid(poseStack, consumer, -0.015F, -0.12F, -0.7F, 0.015F, 0.12F, -0.5F, palette.fletching(), packedLight);
      poseStack.popPose();
      super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
   }

   public ResourceLocation getTextureLocation(BallistaBoltEntity entity) {
      return TEXTURE;
   }

   private static void renderCuboid(
      PoseStack poseStack, VertexConsumer consumer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int color, int packedLight
   ) {
      Pose pose = poseStack.last();
      quad(pose, consumer, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0.0F, 0.0F, 1.0F, color, packedLight);
      quad(pose, consumer, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, 0.0F, 0.0F, -1.0F, color, packedLight);
      quad(pose, consumer, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, 0.0F, 1.0F, 0.0F, color, packedLight);
      quad(pose, consumer, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0.0F, -1.0F, 0.0F, color, packedLight);
      quad(pose, consumer, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, 1.0F, 0.0F, 0.0F, color, packedLight);
      quad(pose, consumer, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1.0F, 0.0F, 0.0F, color, packedLight);
   }

   private static void quad(
      Pose pose,
      VertexConsumer consumer,
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
      int color,
      int packedLight
   ) {
      vertex(pose, consumer, x1, y1, z1, 0.0F, 0.0F, normalX, normalY, normalZ, color, packedLight);
      vertex(pose, consumer, x2, y2, z2, 1.0F, 0.0F, normalX, normalY, normalZ, color, packedLight);
      vertex(pose, consumer, x3, y3, z3, 1.0F, 1.0F, normalX, normalY, normalZ, color, packedLight);
      vertex(pose, consumer, x4, y4, z4, 0.0F, 1.0F, normalX, normalY, normalZ, color, packedLight);
   }

   private static void vertex(
      Pose pose, VertexConsumer consumer, float x, float y, float z, float u, float v, float normalX, float normalY, float normalZ, int color, int packedLight
   ) {
      Matrix4f matrix = pose.pose();
      Matrix3f normal = pose.normal();
      consumer.vertex(matrix, x, y, z)
         .color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 255)
         .uv(u, v)
         .overlayCoords(0)
         .uv2(packedLight)
         .normal(normal, normalX, normalY, normalZ)
         .endVertex();
   }

   private static record Palette(int head, int shaft, int fletching) {
      private static BallistaBoltRenderer.Palette forAmmo(BallistaAmmoType type) {
         return switch (type) {
            case IRON -> new BallistaBoltRenderer.Palette(14081248, 12963025, 15790320);
            case FLAME -> new BallistaBoltRenderer.Palette(14170914, 10183471, 15790320);
            case EXPLOSIVE -> new BallistaBoltRenderer.Palette(15764002, 10183471, 15790320);
            case WOOD -> new BallistaBoltRenderer.Palette(14081248, 10183471, 15790320);
         };
      }
   }
}
