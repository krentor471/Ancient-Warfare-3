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
      BlockState state = blockEntity.m_58900_();
      if (state.m_60734_() instanceof TorqueTransportBlock block) {
         float speed = blockEntity.getVisualSpeed();
         if (!(speed <= 0.001F) && blockEntity.m_58904_() != null) {
            TextureAtlasSprite sprite = this.sprite(block.getTier());
            VertexConsumer consumer = bufferSource.m_6299_(RenderType.m_110463_());
            Direction facing = (Direction)state.m_61143_(TorqueTransportBlock.FACING);
            float angle = ((float)blockEntity.m_58904_().m_46467_() + partialTick) * speed * 34.0F % 360.0F;
            if (block.getKind() == TorqueTransportKind.SHAFT) {
               this.renderShaft(poseStack, consumer, sprite, facing, angle, packedLight);
            } else {
               this.renderBranches(poseStack, consumer, sprite, blockEntity, block, state, facing, angle, packedLight);
            }
         }
      }
   }

   private void renderShaft(PoseStack poseStack, VertexConsumer consumer, TextureAtlasSprite sprite, Direction facing, float angle, int packedLight) {
      poseStack.m_85836_();
      poseStack.m_85837_(0.5, 0.5, 0.5);
      this.rotateToDirection(poseStack, facing);
      poseStack.m_252781_(Axis.f_252403_.m_252977_(angle));
      this.renderCuboid(poseStack, consumer, sprite, -0.165F, -0.165F, -0.51F, 0.165F, 0.165F, 0.51F, packedLight);
      poseStack.m_85849_();
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
            float sideAngle = direction != facing && direction != facing.m_122424_() ? -angle : angle;
            poseStack.m_85836_();
            poseStack.m_85837_(0.5, 0.5, 0.5);
            this.rotateToDirection(poseStack, direction);
            poseStack.m_252781_(Axis.f_252403_.m_252977_(sideAngle));
            this.renderCuboid(poseStack, consumer, sprite, -0.165F, -0.165F, 0.0F, 0.165F, 0.165F, 0.5F, packedLight);
            poseStack.m_85849_();
         }
      }
   }

   private TextureAtlasSprite sprite(TorqueTier tier) {
      ResourceLocation texture = new ResourceLocation("ancientwarfare", "block/automation/torque_shaft_" + this.tierName(tier));
      return (TextureAtlasSprite)Minecraft.m_91087_().m_91258_(TextureAtlas.f_118259_).apply(texture);
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
            poseStack.m_252781_(Axis.f_252529_.m_252977_(180.0F));
         case UP:
         default:
            break;
         case NORTH:
            poseStack.m_252781_(Axis.f_252529_.m_252977_(90.0F));
            break;
         case SOUTH:
            poseStack.m_252781_(Axis.f_252529_.m_252977_(-90.0F));
            break;
         case WEST:
            poseStack.m_252781_(Axis.f_252436_.m_252977_(-90.0F));
            break;
         case EAST:
            poseStack.m_252781_(Axis.f_252436_.m_252977_(90.0F));
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
      Pose pose = poseStack.m_85850_();
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
}
