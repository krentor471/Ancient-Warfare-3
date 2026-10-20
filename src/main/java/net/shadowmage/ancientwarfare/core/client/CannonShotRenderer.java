package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.CannonShotEntity;
import net.shadowmage.ancientwarfare.core.item.CannonAmmoType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class CannonShotRenderer extends EntityRenderer<CannonShotEntity> {
   private static final ResourceLocation TEXTURE = AWCoreIds.id("textures/entity/projectile/ballista_bolt.png");

   public CannonShotRenderer(Context context) {
      super(context);
   }

   public void render(CannonShotEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      poseStack.m_85836_();
      float size = entity.isPellet() ? 0.09F : sizeForWeight(entity.getAmmoType().weight());
      int color = colorForKind(entity.getAmmoType().kind(), entity.isPellet());
      VertexConsumer consumer = bufferSource.m_6299_(RenderType.m_110458_(TEXTURE));
      renderCuboid(poseStack, consumer, -size, -size, -size, size, size, size, color, packedLight);
      poseStack.m_85849_();
      super.m_7392_(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
   }

   public ResourceLocation getTextureLocation(CannonShotEntity entity) {
      return TEXTURE;
   }

   private static float sizeForWeight(int weight) {
      return switch (weight) {
         case 10 -> 0.19F;
         case 15 -> 0.23F;
         case 25 -> 0.28F;
         default -> 0.15F;
      };
   }

   private static int colorForKind(CannonAmmoType.Kind kind, boolean pellet) {
      if (pellet) {
         return 10197915;
      } else {
         return switch (kind) {
            case IRON -> 13159888;
            case GRAPE -> 9080207;
            case CANISTER -> 12026429;
         };
      }
   }

   private static void renderCuboid(
      PoseStack poseStack, VertexConsumer consumer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int color, int packedLight
   ) {
      Pose pose = poseStack.m_85850_();
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
      Matrix4f matrix = pose.m_252922_();
      Matrix3f normal = pose.m_252943_();
      consumer.m_252986_(matrix, x, y, z)
         .m_6122_(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 255)
         .m_7421_(u, v)
         .m_86008_(0)
         .m_85969_(packedLight)
         .m_252939_(normal, normalX, normalY, normalZ)
         .m_5752_();
   }
}
