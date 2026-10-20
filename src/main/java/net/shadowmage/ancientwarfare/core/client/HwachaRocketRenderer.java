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
import net.shadowmage.ancientwarfare.core.entity.HwachaRocketEntity;
import net.shadowmage.ancientwarfare.core.item.HwachaAmmoType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class HwachaRocketRenderer extends EntityRenderer<HwachaRocketEntity> {
   private static final ResourceLocation TEXTURE = AWCoreIds.id("textures/entity/projectile/ballista_bolt.png");

   public HwachaRocketRenderer(Context context) {
      super(context);
   }

   public void render(HwachaRocketEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      poseStack.pushPose();
      float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
      float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
      poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
      poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
      poseStack.scale(1.25F, 1.25F, 1.25F);
      VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
      HwachaRocketRenderer.Palette palette = HwachaRocketRenderer.Palette.forAmmo(entity.getAmmoType());
      renderCuboid(poseStack, consumer, -0.045F, -0.045F, -0.62F, 0.045F, 0.045F, 0.1F, palette.stick(), packedLight);
      renderCuboid(poseStack, consumer, -0.075F, -0.075F, -0.1F, 0.075F, 0.075F, 0.3F, palette.body(), packedLight);
      renderCuboid(poseStack, consumer, -0.09F, -0.09F, 0.24F, 0.09F, 0.09F, 0.46F, palette.head(), packedLight);
      renderCuboid(poseStack, consumer, -0.13F, -0.018F, -0.72F, 0.13F, 0.018F, -0.5F, palette.fin(), packedLight);
      renderCuboid(poseStack, consumer, -0.018F, -0.13F, -0.72F, 0.018F, 0.13F, -0.5F, palette.fin(), packedLight);
      renderCuboid(poseStack, consumer, -0.06F, -0.06F, -0.76F, 0.06F, 0.06F, -0.64F, 2236962, packedLight);
      poseStack.popPose();
      super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
   }

   public ResourceLocation getTextureLocation(HwachaRocketEntity entity) {
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

   private static record Palette(int head, int body, int stick, int fin) {
      private static HwachaRocketRenderer.Palette forAmmo(HwachaAmmoType type) {
         return switch (type) {
            case FLAME -> new HwachaRocketRenderer.Palette(14170914, 10839604, 9394733, 15261904);
            case EXPLOSIVE -> new HwachaRocketRenderer.Palette(14846498, 10839604, 9394733, 15261904);
            case AIRBURST -> new HwachaRocketRenderer.Palette(14215408, 10839604, 9394733, 15261904);
            case STANDARD -> new HwachaRocketRenderer.Palette(14081248, 10839604, 9394733, 15261904);
         };
      }
   }
}
