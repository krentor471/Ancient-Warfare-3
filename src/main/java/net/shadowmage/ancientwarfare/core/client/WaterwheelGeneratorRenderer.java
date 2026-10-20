package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.shadowmage.ancientwarfare.core.block.TorqueGeneratorBlock;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueGeneratorKind;
import net.shadowmage.ancientwarfare.core.tile.TorqueGeneratorBlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class WaterwheelGeneratorRenderer implements BlockEntityRenderer<TorqueGeneratorBlockEntity> {
   private static final WaterwheelGeneratorRenderer.UvRegion WOOD_UV = new WaterwheelGeneratorRenderer.UvRegion(0.0F, 0.0F, 16.0F, 16.0F);
   private static final WaterwheelGeneratorRenderer.UvRegion SAIL_UV = new WaterwheelGeneratorRenderer.UvRegion(0.0F, 0.0F, 16.0F, 16.0F);

   public WaterwheelGeneratorRenderer(Context context) {
   }

   public void render(
      TorqueGeneratorBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay
   ) {
      if (blockEntity.m_58904_() != null && blockEntity.m_58900_().m_60734_() instanceof TorqueGeneratorBlock block) {
         TorqueGeneratorKind kind = block.getKind();
         if (kind == TorqueGeneratorKind.WATERWHEEL || kind == TorqueGeneratorKind.WINDMILL) {
            TextureAtlasSprite texture = this.sprite();
            VertexConsumer consumer = bufferSource.m_6299_(RenderType.m_110463_());
            Direction facing = (Direction)blockEntity.m_58900_().m_61143_(TorqueGeneratorBlock.FACING);
            if (kind == TorqueGeneratorKind.WINDMILL) {
               this.renderWindmill(blockEntity, consumer, poseStack, facing, packedLight, partialTick);
            } else {
               float angle = blockEntity.hasValidWaterwheelStructureForRender()
                  ? ((float)blockEntity.m_58904_().m_46467_() + partialTick) * 5.0F % 360.0F
                  : 0.0F;
               poseStack.m_85836_();
               poseStack.m_85837_(0.5, 0.5, 0.5);
               poseStack.m_252781_(Axis.f_252436_.m_252977_(this.yawFor(facing)));
               this.renderBase(consumer, poseStack, texture, packedLight);
               poseStack.m_85836_();
               poseStack.m_252781_(Axis.f_252403_.m_252977_(angle));
               this.renderAxle(consumer, poseStack, texture, packedLight);
               poseStack.m_85836_();
               poseStack.m_85837_(0.0, 0.0, -0.82);
               this.renderWheel(consumer, poseStack, texture, packedLight);
               poseStack.m_85849_();
               poseStack.m_85836_();
               poseStack.m_85837_(0.0, 0.0, 0.34);
               this.renderGear(consumer, poseStack, texture, packedLight);
               poseStack.m_85849_();
               poseStack.m_85849_();
               poseStack.m_85849_();
            }
         }
      }
   }

   private void renderWindmill(
      TorqueGeneratorBlockEntity blockEntity, VertexConsumer consumer, PoseStack poseStack, Direction facing, int packedLight, float partialTick
   ) {
      TextureAtlasSprite frameTexture = this.windmillGeneratorSprite();
      TextureAtlasSprite bladeTexture = this.windmillBladeSprite();
      int bladeCount = blockEntity.countWindmillBladesForRender();
      boolean valid = bladeCount >= 8;
      float angle = valid ? ((float)blockEntity.m_58904_().m_46467_() + partialTick) * 7.5F % 360.0F : 0.0F;
      float bladeScale = Mth.m_184631_((float)bladeCount, 8.0F, 48.0F, 0.72F, 1.42F);
      poseStack.m_85836_();
      poseStack.m_85837_(0.5, 0.5, 0.5);
      poseStack.m_252781_(Axis.f_252436_.m_252977_(this.yawFor(facing)));
      this.renderWindmillBlock(consumer, poseStack, frameTexture, packedLight);
      if (valid) {
         poseStack.m_85836_();
         poseStack.m_85837_(0.0, 0.0, -0.58);
         poseStack.m_252781_(Axis.f_252403_.m_252977_(angle));
         this.renderWindmillRotor(consumer, poseStack, frameTexture, bladeTexture, packedLight, bladeScale);
         poseStack.m_85849_();
      }

      poseStack.m_85849_();
   }

   private void renderWindmillBlock(VertexConsumer consumer, PoseStack poseStack, TextureAtlasSprite texture, int packedLight) {
      this.renderCuboid(poseStack, consumer, texture, -0.425F, -0.7875F, -0.3F, 0.425F, -0.6875F, 0.45F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, -0.45F, -0.6875F, -0.2375F, -0.34375F, -0.5625F, 0.48125F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, 0.34375F, -0.6875F, -0.2375F, 0.45F, -0.5625F, 0.48125F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, -0.2375F, -0.58125F, 0.2375F, 0.2375F, -0.05F, 0.5F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, -0.28125F, -0.05625F, 0.2125F, 0.28125F, 0.04375F, 0.525F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, -0.28125F, -0.625F, 0.2125F, 0.28125F, -0.53125F, 0.525F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, -0.375F, -0.575F, -0.2F, -0.25625F, 0.125F, -0.0625F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, 0.25625F, -0.575F, -0.2F, 0.375F, 0.125F, -0.0625F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, -0.4F, 0.09375F, -0.225F, 0.4F, 0.2375F, -0.0375F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, -0.3875F, -0.5375F, -0.21875F, 0.3875F, -0.425F, -0.04375F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, -0.2125F, -0.5125F, -0.7125F, 0.2125F, -0.0625F, -0.3125F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, -0.1375F, -0.4375F, -0.76875F, 0.1375F, -0.1375F, -0.7F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, -0.1F, -0.4F, -0.84375F, 0.1F, -0.175F, -0.74375F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, -0.14375F, -0.5125F, 0.4875F, 0.14375F, -0.1875F, 0.75F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, -0.08125F, -0.4375F, 0.73125F, 0.08125F, -0.2625F, 1.03125F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, -0.175F, 0.05F, 0.1125F, 0.175F, 0.15F, 0.40625F, packedLight);
      this.renderRotatedCuboidZ(
         consumer, poseStack, texture, -0.325F, -0.5375F, -0.125F, -0.25F, 0.0125F, 0.35F, -16.0F, -0.2875F, -0.2625F, 0.1125F, packedLight
      );
      this.renderRotatedCuboidZ(consumer, poseStack, texture, 0.25F, -0.5375F, -0.125F, 0.325F, 0.0125F, 0.35F, 16.0F, 0.2875F, -0.2625F, 0.1125F, packedLight);
   }

   private void renderWindmillRotor(
      VertexConsumer consumer, PoseStack poseStack, TextureAtlasSprite frameTexture, TextureAtlasSprite bladeTexture, int packedLight, float bladeScale
   ) {
      this.renderCuboid(poseStack, consumer, frameTexture, -0.12F, -0.12F, -0.08F, 0.12F, 0.12F, 0.08F, packedLight);

      for (int i = 0; i < 4; i++) {
         poseStack.m_85836_();
         poseStack.m_252781_(Axis.f_252403_.m_252977_((float)i * 90.0F));
         this.renderCuboid(poseStack, consumer, frameTexture, -0.04F, 0.08F, -0.035F, 0.04F, 1.55F * bladeScale, 0.035F, packedLight);
         this.renderCuboid(poseStack, consumer, bladeTexture, 0.06F, 0.36F * bladeScale, -0.025F, 0.38F, 1.48F * bladeScale, 0.025F, packedLight);
         poseStack.m_85849_();
      }
   }

   private void renderRotatedCuboidZ(
      VertexConsumer consumer,
      PoseStack poseStack,
      TextureAtlasSprite texture,
      float minX,
      float minY,
      float minZ,
      float maxX,
      float maxY,
      float maxZ,
      float angle,
      float originX,
      float originY,
      float originZ,
      int packedLight
   ) {
      poseStack.m_85836_();
      poseStack.m_252880_(originX, originY, originZ);
      poseStack.m_252781_(Axis.f_252403_.m_252977_(angle));
      poseStack.m_252880_(-originX, -originY, -originZ);
      this.renderCuboid(poseStack, consumer, texture, minX, minY, minZ, maxX, maxY, maxZ, packedLight);
      poseStack.m_85849_();
   }

   private void renderBase(VertexConsumer consumer, PoseStack poseStack, TextureAtlasSprite texture, int packedLight) {
      this.renderCuboid(poseStack, consumer, texture, -0.5F, -0.5F, -0.5F, 0.5F, -0.4375F, 0.5F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, -0.1875F, -0.4375F, -0.42F, 0.1875F, 0.1875F, -0.3F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, -0.42F, -0.4375F, -0.42F, -0.3F, 0.4375F, -0.3F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, 0.3F, -0.4375F, -0.42F, 0.42F, 0.4375F, -0.3F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, -0.42F, 0.375F, -0.42F, 0.42F, 0.5F, -0.3F, packedLight);
   }

   private void renderAxle(VertexConsumer consumer, PoseStack poseStack, TextureAtlasSprite texture, int packedLight) {
      this.renderCuboid(poseStack, consumer, texture, -0.08F, -0.08F, -1.28F, 0.08F, 0.08F, 0.54F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, -0.13F, -0.13F, -0.92F, 0.13F, 0.13F, -0.72F, packedLight);
      this.renderCuboid(poseStack, consumer, texture, -0.12F, -0.12F, 0.22F, 0.12F, 0.12F, 0.46F, packedLight);
   }

   private void renderWheel(VertexConsumer consumer, PoseStack poseStack, TextureAtlasSprite texture, int packedLight) {
      this.renderCuboid(poseStack, consumer, texture, -0.08F, -0.08F, -0.1F, 0.08F, 0.08F, 0.1F, packedLight);

      for (int i = 0; i < 8; i++) {
         poseStack.m_85836_();
         poseStack.m_252781_(Axis.f_252403_.m_252977_((float)i * 45.0F));
         this.renderCuboid(poseStack, consumer, texture, -0.045F, 0.08F, -0.08F, 0.045F, 0.95F, 0.08F, packedLight);
         this.renderCuboid(poseStack, consumer, texture, -0.24F, 0.82F, -0.11F, 0.24F, 1.04F, 0.11F, packedLight);
         poseStack.m_85849_();
      }

      this.renderRing(consumer, poseStack, texture, 0.82F, packedLight);
      this.renderRing(consumer, poseStack, texture, 1.04F, packedLight);
   }

   private void renderGear(VertexConsumer consumer, PoseStack poseStack, TextureAtlasSprite texture, int packedLight) {
      this.renderCuboid(poseStack, consumer, texture, -0.11F, -0.11F, -0.08F, 0.11F, 0.11F, 0.08F, packedLight);
      this.renderRing(consumer, poseStack, texture, 0.34F, packedLight);
      this.renderRing(consumer, poseStack, texture, 0.48F, packedLight);

      for (int i = 0; i < 12; i++) {
         poseStack.m_85836_();
         poseStack.m_252781_(Axis.f_252403_.m_252977_((float)i * 30.0F));
         this.renderCuboid(poseStack, consumer, texture, -0.07F, 0.45F, -0.1F, 0.07F, 0.62F, 0.1F, packedLight);
         this.renderCuboid(poseStack, consumer, texture, -0.035F, 0.11F, -0.065F, 0.035F, 0.43F, 0.065F, packedLight);
         poseStack.m_85849_();
      }
   }

   private void renderRing(VertexConsumer consumer, PoseStack poseStack, TextureAtlasSprite texture, float radius, int packedLight) {
      for (int i = 0; i < 16; i++) {
         poseStack.m_85836_();
         poseStack.m_252781_(Axis.f_252403_.m_252977_((float)i * 22.5F));
         this.renderCuboid(poseStack, consumer, texture, -0.11F, radius - 0.035F, -0.09F, 0.11F, radius + 0.035F, 0.09F, packedLight);
         poseStack.m_85849_();
      }
   }

   private float yawFor(Direction facing) {
      return switch (facing) {
         case SOUTH -> 180.0F;
         case WEST -> 270.0F;
         case EAST -> 90.0F;
         default -> 0.0F;
      };
   }

   private TextureAtlasSprite sprite() {
      ResourceLocation texture = new ResourceLocation("ancientwarfare", "block/automation/waterwheel_generator");
      return (TextureAtlasSprite)Minecraft.m_91087_().m_91258_(TextureAtlas.f_118259_).apply(texture);
   }

   private TextureAtlasSprite windmillGeneratorSprite() {
      ResourceLocation texture = new ResourceLocation("ancientwarfare", "block/automation/windmill_generator");
      return (TextureAtlasSprite)Minecraft.m_91087_().m_91258_(TextureAtlas.f_118259_).apply(texture);
   }

   private TextureAtlasSprite windmillBladeSprite() {
      ResourceLocation texture = new ResourceLocation("ancientwarfare", "block/automation/windmill_blade");
      return (TextureAtlasSprite)Minecraft.m_91087_().m_91258_(TextureAtlas.f_118259_).apply(texture);
   }

   private void renderCuboid(
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
      Pose pose = poseStack.m_85850_();
      int light = packedLight == 0 ? 15728880 : packedLight;
      this.quad(pose, consumer, sprite, WOOD_UV, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0.0F, 0.0F, 1.0F, light);
      this.quad(pose, consumer, sprite, WOOD_UV, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, 0.0F, 0.0F, -1.0F, light);
      this.quad(pose, consumer, sprite, WOOD_UV, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, 0.0F, 1.0F, 0.0F, light);
      this.quad(pose, consumer, sprite, WOOD_UV, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0.0F, -1.0F, 0.0F, light);
      this.quad(pose, consumer, sprite, WOOD_UV, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, 1.0F, 0.0F, 0.0F, light);
      this.quad(pose, consumer, sprite, WOOD_UV, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1.0F, 0.0F, 0.0F, light);
   }

   private void quad(
      Pose pose,
      VertexConsumer consumer,
      TextureAtlasSprite sprite,
      WaterwheelGeneratorRenderer.UvRegion uv,
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
      this.vertex(pose, consumer, sprite, x1, y1, z1, uv.u0, uv.v0, normalX, normalY, normalZ, packedLight);
      this.vertex(pose, consumer, sprite, x2, y2, z2, uv.u1, uv.v0, normalX, normalY, normalZ, packedLight);
      this.vertex(pose, consumer, sprite, x3, y3, z3, uv.u1, uv.v1, normalX, normalY, normalZ, packedLight);
      this.vertex(pose, consumer, sprite, x4, y4, z4, uv.u0, uv.v1, normalX, normalY, normalZ, packedLight);
   }

   private void vertex(
      Pose pose,
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
      Matrix4f matrix = pose.m_252922_();
      Matrix3f normal = pose.m_252943_();
      consumer.m_252986_(matrix, x, y, z)
         .m_6122_(255, 255, 255, 255)
         .m_7421_(sprite.m_118367_((double)u), sprite.m_118393_((double)v))
         .m_85969_(packedLight)
         .m_252939_(normal, normalX, normalY, normalZ)
         .m_5752_();
   }

   public int m_142163_() {
      return Mth.m_14167_(96.0F);
   }

   private static record UvRegion(float u0, float v0, float u1, float v1) {
   }
}
