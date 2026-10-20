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
import net.shadowmage.ancientwarfare.core.block.TorqueTransportBlock;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTier;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTransportKind;
import net.shadowmage.ancientwarfare.core.tile.TorqueTransportBlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class TorqueTransportRenderer implements BlockEntityRenderer<TorqueTransportBlockEntity> {
   private static final float SHAFT_RADIUS = 0.165F;
   private static final float BRANCH_LENGTH = 0.5F;
   private static final float SIDE_U0 = 0.0F;
   private static final float SIDE_V0 = 1.25F;
   private static final float SIDE_U1 = 5.0F;
   private static final float SIDE_V1 = 2.5F;
   private static final float CAP_U0 = 1.25F;
   private static final float CAP_V0 = 0.0F;
   private static final float CAP_U1 = 2.5F;
   private static final float CAP_V1 = 1.25F;

   public TorqueTransportRenderer(Context context) {
   }

   public void render(
      TorqueTransportBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay
   ) {
      BlockState state = blockEntity.getBlockState();
      if (state.getBlock() instanceof TorqueTransportBlock block) {
         float speed = blockEntity.getVisualSpeed();
         if (!(speed <= 0.001F) && blockEntity.getLevel() != null) {
            TextureAtlasSprite sprite = this.sprite(block.getTier());
            VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());
            Direction facing = (Direction)state.getValue(TorqueTransportBlock.FACING);
            float angle = ((float)blockEntity.getLevel().getGameTime() + partialTick) * speed * 34.0F % 360.0F;
            if (block.getKind() == TorqueTransportKind.SHAFT) {
               this.renderShaft(poseStack, consumer, sprite, facing, angle, packedLight);
            } else {
               this.renderBranches(poseStack, consumer, sprite, blockEntity, block, state, facing, angle, packedLight);
            }
         }
      }
   }

   private void renderShaft(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite, Direction facing, float angle, int packedLight) {
      poseStack.pushPose();
      poseStack.translate(0.5, 0.5, 0.5);
      this.rotateToDirection(poseStack, facing);
      poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
      this.renderCuboid(poseStack, consumer, sprite, -0.165F, -0.165F, -0.51F, 0.165F, 0.165F, 0.51F, packedLight);
      poseStack.popPose();
   }

   private void renderBranches(
      PoseStack poseStack,
      VertexConsumer consumer,
      TextureAtlasSprite sprite,
      TorqueTransportBlockEntity blockEntity,
      TorqueTransportBlock block,
      BlockState state,
      Direction facing,
      float angle,
      int packedLight
   ) {
      for (Direction direction : Direction.values()) {
         if (block.isConnected(state, direction) && blockEntity.isSideVisuallyActive(direction)) {
            float sideAngle = direction != facing && direction != facing.getOpposite() ? -angle : angle;
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            this.rotateToDirection(poseStack, direction);
            poseStack.mulPose(Axis.ZP.rotationDegrees(sideAngle));
            this.renderCuboid(poseStack, consumer, sprite, -0.165F, -0.165F, 0.0F, 0.165F, 0.165F, 0.5F, packedLight);
            poseStack.popPose();
         }
      }
   }

   private TextureAtlasSprite sprite(TorqueTier tier) {
      ResourceLocation texture = new ResourceLocation("ancientwarfare", "block/automation/torque_shaft_" + this.tierName(tier));
      return (TextureAtlasSprite)Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
   }

   private String tierName(TorqueTier tier) {
      return switch (tier) {
         case LIGHT -> "light";
         case MEDIUM -> "medium";
         case HEAVY -> "heavy";
      };
   }

   private void rotateToDirection(PoseStack poseStack, Direction direction) {
      switch (direction) {
         case DOWN:
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
         case UP:
         default:
            break;
         case NORTH:
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            break;
         case SOUTH:
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            break;
         case WEST:
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            break;
         case EAST:
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
      }
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
      Pose pose = poseStack.last();
      int light = packedLight == 0 ? 15728880 : packedLight;
      this.quad(
         pose, consumer, sprite, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, 0.0F, 0.0F, 1.0F, 1.25F, 0.0F, 2.5F, 1.25F, light
      );
      this.quad(
         pose, consumer, sprite, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, 0.0F, 0.0F, -1.0F, 1.25F, 0.0F, 2.5F, 1.25F, light
      );
      this.quad(
         pose, consumer, sprite, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, 0.0F, 1.0F, 0.0F, 0.0F, 1.25F, 5.0F, 2.5F, light
      );
      this.quad(
         pose, consumer, sprite, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, 0.0F, -1.0F, 0.0F, 0.0F, 1.25F, 5.0F, 2.5F, light
      );
      this.quad(
         pose, consumer, sprite, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, 1.0F, 0.0F, 0.0F, 0.0F, 1.25F, 5.0F, 2.5F, light
      );
      this.quad(
         pose, consumer, sprite, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, -1.0F, 0.0F, 0.0F, 0.0F, 1.25F, 5.0F, 2.5F, light
      );
   }

   private void quad(
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
      float u0,
      float v0,
      float u1,
      float v1,
      int packedLight
   ) {
      this.vertex(pose, consumer, sprite, x1, y1, z1, u0, v0, normalX, normalY, normalZ, packedLight);
      this.vertex(pose, consumer, sprite, x2, y2, z2, u1, v0, normalX, normalY, normalZ, packedLight);
      this.vertex(pose, consumer, sprite, x3, y3, z3, u1, v1, normalX, normalY, normalZ, packedLight);
      this.vertex(pose, consumer, sprite, x4, y4, z4, u0, v1, normalX, normalY, normalZ, packedLight);
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
      Matrix4f matrix = pose.pose();
      Matrix3f normal = pose.normal();
      consumer.vertex(matrix, x, y, z)
         .color(255, 255, 255, 255)
         .uv(sprite.getU((double)u), sprite.getV((double)v))
         .uv2(packedLight)
         .normal(normal, normalX, normalY, normalZ)
         .endVertex();
   }

   public int getViewDistance() {
      return Mth.ceil(64.0F);
   }
}
