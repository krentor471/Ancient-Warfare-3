package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.block.ProtectionFlagBlock;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.tile.ProtectionFlagBlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class ProtectionFlagRenderer implements BlockEntityRenderer<ProtectionFlagBlockEntity> {
   private static final ResourceLocation FALLBACK_TEXTURE = AWCoreIds.id("textures/entity/structure/banner/empire.png");
   private static final Map<String, ResourceLocation> TEXTURE_CACHE = new ConcurrentHashMap<>();
   private static final float MODEL_SCALE = 0.041666668F;

   public ProtectionFlagRenderer(Context context) {
   }

   public void render(
      ProtectionFlagBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay
   ) {
      int rotation = (Integer)blockEntity.m_58900_().m_61143_(ProtectionFlagBlock.ROTATION);
      VertexConsumer consumer = bufferSource.m_6299_(RenderType.m_110458_(flagTexture(blockEntity.flagName())));
      poseStack.m_85836_();
      poseStack.m_85837_(0.5, 0.0, 0.5);
      poseStack.m_252781_(Axis.f_252436_.m_252977_((float)(-rotation) * 22.5F));
      poseStack.m_85841_(0.041666668F, 0.041666668F, 0.041666668F);
      box(poseStack, consumer, 44.0F, 0.0F, -1.0F, 0.0F, -1.0F, 1.0F, 42.0F, 1.0F, packedLight, packedOverlay);
      box(poseStack, consumer, 0.0F, 42.0F, -10.0F, 40.0F, -1.0F, 10.0F, 42.0F, 1.0F, packedLight, packedOverlay);
      box(poseStack, consumer, 0.0F, 0.0F, -10.0F, 0.0F, -2.0F, 10.0F, 40.0F, -1.0F, packedLight, packedOverlay);
      poseStack.m_85849_();
   }

   private static void box(
      PoseStack stack,
      VertexConsumer consumer,
      float u,
      float v,
      float minX,
      float minY,
      float minZ,
      float maxX,
      float maxY,
      float maxZ,
      int light,
      int overlay
   ) {
      float x = maxX - minX;
      float y = maxY - minY;
      float z = maxZ - minZ;
      ProtectionFlagRenderer.Uv north = uv(u + z, v + z, u + z + x, v + z + y);
      ProtectionFlagRenderer.Uv east = uv(u, v + z, u + z, v + z + y);
      ProtectionFlagRenderer.Uv south = uv(u + z + x + z, v + z, u + z + x + z + x, v + z + y);
      ProtectionFlagRenderer.Uv west = uv(u + z + x, v + z, u + z + x + z, v + z + y);
      ProtectionFlagRenderer.Uv up = uv(u + z + x, v + z, u + z, v);
      ProtectionFlagRenderer.Uv down = uv(u + z + x + x, v, u + z + x, v + z);
      Pose pose = stack.m_85850_();
      quad(pose, consumer, north, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0.0F, 0.0F, 1.0F, light, overlay);
      quad(pose, consumer, south, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, 0.0F, 0.0F, -1.0F, light, overlay);
      quad(pose, consumer, up, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, 0.0F, 1.0F, 0.0F, light, overlay);
      quad(pose, consumer, down, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0.0F, -1.0F, 0.0F, light, overlay);
      quad(pose, consumer, east, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, 1.0F, 0.0F, 0.0F, light, overlay);
      quad(pose, consumer, west, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1.0F, 0.0F, 0.0F, light, overlay);
   }

   private static ProtectionFlagRenderer.Uv uv(float u0, float v0, float u1, float v1) {
      return new ProtectionFlagRenderer.Uv(u0 / 64.0F, v0 / 64.0F, u1 / 64.0F, v1 / 64.0F);
   }

   private static void quad(
      Pose pose,
      VertexConsumer consumer,
      ProtectionFlagRenderer.Uv uv,
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
      float nx,
      float ny,
      float nz,
      int light,
      int overlay
   ) {
      vertex(pose, consumer, x1, y1, z1, uv.u0, uv.v0, nx, ny, nz, light, overlay);
      vertex(pose, consumer, x2, y2, z2, uv.u1, uv.v0, nx, ny, nz, light, overlay);
      vertex(pose, consumer, x3, y3, z3, uv.u1, uv.v1, nx, ny, nz, light, overlay);
      vertex(pose, consumer, x4, y4, z4, uv.u0, uv.v1, nx, ny, nz, light, overlay);
   }

   private static void vertex(
      Pose pose, VertexConsumer consumer, float x, float y, float z, float u, float v, float nx, float ny, float nz, int light, int overlay
   ) {
      Matrix4f matrix = pose.m_252922_();
      Matrix3f normal = pose.m_252943_();
      consumer.m_252986_(matrix, x, y, z).m_6122_(255, 255, 255, 255).m_7421_(u, v).m_86008_(overlay).m_85969_(light).m_252939_(normal, nx, ny, nz).m_5752_();
   }

   private static ResourceLocation flagTexture(String flagName) {
      if (flagName != null && !flagName.isBlank()) {
         String safeName = flagName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "");
         return !safeName.isBlank() && !"generic".equals(safeName) ? TEXTURE_CACHE.computeIfAbsent(safeName, name -> {
            ResourceLocation requested = AWCoreIds.id("textures/entity/structure/banner/" + name + ".png");
            return Minecraft.m_91087_().m_91098_().m_213713_(requested).isPresent() ? requested : FALLBACK_TEXTURE;
         }) : FALLBACK_TEXTURE;
      } else {
         return FALLBACK_TEXTURE;
      }
   }

   private static record Uv(float u0, float v0, float u1, float v1) {
   }
}
