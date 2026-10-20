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
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.block.HandCrankedGeneratorBlock;
import net.shadowmage.ancientwarfare.core.tile.HandCrankedGeneratorBlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class HandCrankedGeneratorRenderer implements BlockEntityRenderer<HandCrankedGeneratorBlockEntity> {
   private static final HandCrankedGeneratorRenderer.UvRegion ALL_UV = new HandCrankedGeneratorRenderer.UvRegion(0.0F, 0.0F, 16.0F, 16.0F);

   public HandCrankedGeneratorRenderer(Context context) {
   }

   public void render(
      HandCrankedGeneratorBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay
   ) {
      if (blockEntity.m_58904_() != null) {
         BlockState state = blockEntity.m_58900_();
         Direction facing = state.m_61138_(HandCrankedGeneratorBlock.FACING) ? (Direction)state.m_61143_(HandCrankedGeneratorBlock.FACING) : Direction.NORTH;
         TextureAtlasSprite texture = this.sprite();
         VertexConsumer consumer = bufferSource.m_6299_(RenderType.m_110463_());
         float angle = blockEntity.getCrankActiveTicks() > 0 ? ((float)blockEntity.m_58904_().m_46467_() + partialTick) * 30.0F % 360.0F : 0.0F;
         poseStack.m_85836_();
         poseStack.m_85837_(0.5, 0.5, 0.5);
         poseStack.m_252781_(Axis.f_252436_.m_252977_(this.yawFor(facing)));
         poseStack.m_85836_();
         poseStack.m_85837_(0.0, 0.4375, 0.25);
         poseStack.m_252781_(Axis.f_252436_.m_252977_(angle));
         poseStack.m_85837_(0.0, -0.4375, -0.25);
         this.renderTopWheel(poseStack, consumer, texture, packedLight);
         poseStack.m_85849_();
         poseStack.m_85836_();
         poseStack.m_85837_(0.0, 0.0, -0.453125);
         poseStack.m_252781_(Axis.f_252403_.m_252977_(-angle));
         poseStack.m_85837_(0.0, 0.0, 0.453125);
         this.renderFrontGear(poseStack, consumer, texture, packedLight);
         poseStack.m_85849_();
         poseStack.m_85849_();
      }
   }

   private void renderTopWheel(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite texture, int packedLight) {
      this.renderModelCuboid(poseStack, consumer, texture, 5.0F, 14.5F, 8.5F, 11.0F, 15.5F, 14.5F, packedLight);
      this.renderModelCuboid(poseStack, consumer, texture, 11.0F, 14.5F, 9.5F, 12.0F, 15.5F, 13.5F, packedLight);
      this.renderModelCuboid(poseStack, consumer, texture, 4.0F, 14.5F, 9.5F, 5.0F, 15.5F, 13.5F, packedLight);
      this.renderModelCuboid(poseStack, consumer, texture, 6.0F, 14.5F, 14.5F, 10.0F, 15.5F, 15.5F, packedLight);
      this.renderModelCuboid(poseStack, consumer, texture, 6.0F, 14.5F, 7.5F, 10.0F, 15.5F, 8.5F, packedLight);
      this.renderModelCuboid(poseStack, consumer, texture, 10.0F, 15.5F, 9.5F, 11.0F, 16.5F, 10.5F, packedLight);
   }

   private void renderFrontGear(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite texture, int packedLight) {
      this.renderModelCuboid(poseStack, consumer, texture, 6.0F, 6.0F, 0.0F, 10.0F, 10.0F, 1.0F, packedLight);
      this.renderModelCuboid(poseStack, consumer, texture, 7.0F, 10.0F, 0.0F, 9.0F, 12.0F, 1.0F, packedLight);
      this.renderModelCuboid(poseStack, consumer, texture, 7.0F, 4.0F, 0.0F, 9.0F, 6.0F, 1.0F, packedLight);
      this.renderModelCuboid(poseStack, consumer, texture, 4.0F, 7.0F, 0.0F, 6.0F, 9.0F, 1.0F, packedLight);
      this.renderModelCuboid(poseStack, consumer, texture, 10.0F, 7.0F, 0.0F, 12.0F, 9.0F, 1.0F, packedLight);
      this.renderModelCuboid(poseStack, consumer, texture, 10.0F, 10.0F, 0.0F, 11.0F, 11.0F, 1.0F, packedLight);
      this.renderModelCuboid(poseStack, consumer, texture, 5.0F, 10.0F, 0.0F, 6.0F, 11.0F, 1.0F, packedLight);
      this.renderModelCuboid(poseStack, consumer, texture, 5.0F, 5.0F, 0.0F, 6.0F, 6.0F, 1.0F, packedLight);
      this.renderModelCuboid(poseStack, consumer, texture, 10.0F, 5.0F, 0.0F, 11.0F, 6.0F, 1.0F, packedLight);
   }

   private void renderModelCuboid(
      PoseStack poseStack,
      VertexConsumer consumer,
      TextureAtlasSprite texture,
      float minX,
      float minY,
      float minZ,
      float maxX,
      float maxY,
      float maxZ,
      int packedLight
   ) {
      this.renderCuboid(
         poseStack,
         consumer,
         texture,
         minX / 16.0F - 0.5F,
         minY / 16.0F - 0.5F,
         minZ / 16.0F - 0.5F,
         maxX / 16.0F - 0.5F,
         maxY / 16.0F - 0.5F,
         maxZ / 16.0F - 0.5F,
         packedLight
      );
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
      ResourceLocation texture = new ResourceLocation("ancientwarfare", "block/automation/hand_cranked_generator");
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
      this.quad(pose, consumer, sprite, ALL_UV, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0.0F, 0.0F, 1.0F, light);
      this.quad(pose, consumer, sprite, ALL_UV, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, 0.0F, 0.0F, -1.0F, light);
      this.quad(pose, consumer, sprite, ALL_UV, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, 0.0F, 1.0F, 0.0F, light);
      this.quad(pose, consumer, sprite, ALL_UV, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0.0F, -1.0F, 0.0F, light);
      this.quad(pose, consumer, sprite, ALL_UV, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, 1.0F, 0.0F, 0.0F, light);
      this.quad(pose, consumer, sprite, ALL_UV, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1.0F, 0.0F, 0.0F, light);
   }

   private void quad(
      Pose pose,
      VertexConsumer consumer,
      TextureAtlasSprite sprite,
      HandCrankedGeneratorRenderer.UvRegion uv,
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
      return Mth.m_14167_(64.0F);
   }

   private static record UvRegion(float u0, float v0, float u1, float v1) {
   }
}
