package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.entity.GateEntity;

public final class LegacyDynamicGateRenderer extends EntityRenderer<GateEntity> {
   private static final ResourceLocation WOOD = texture("gate_wood_1.png");
   private static final ResourceLocation IRON = texture("gate_iron_1.png");
   private static final ResourceLocation BRIDGE = texture("gate_bridge_wood_1.png");
   private final ModelPart basic = LegacyGateParts.basic();
   private final ModelPart bridge = LegacyGateParts.bridge();

   public LegacyDynamicGateRenderer(Context context) {
      super(context);
      this.f_114477_ = 0.0F;
   }

   private static ResourceLocation texture(String name) {
      return new ResourceLocation("ancientwarfare", "textures/block/structure/gate/" + name);
   }

   public ResourceLocation getTextureLocation(GateEntity entity) {
      return entity.openingStyle() == GateEntity.OpeningStyle.DRAWBRIDGE ? BRIDGE : (entity.material() == GateEntity.Material.IRON ? IRON : WOOD);
   }

   public boolean shouldRender(GateEntity gate, Frustum frustum, double x, double y, double z) {
      if (gate.openingStyle() != GateEntity.OpeningStyle.DRAWBRIDGE) {
         return super.m_5523_(gate, frustum, x, y, z);
      } else if (!gate.m_6000_(x, y, z)) {
         return false;
      } else {
         BlockPos min = gate.minPos();
         BlockPos max = gate.maxPos();
         BlockPos end = max.m_5484_(gate.facing().m_122424_(), gate.heightBlocks());
         AABB bounds = new AABB(
            (double)Math.min(min.m_123341_(), end.m_123341_()),
            (double)min.m_123342_(),
            (double)Math.min(min.m_123343_(), end.m_123343_()),
            (double)(Math.max(max.m_123341_(), end.m_123341_()) + 1),
            (double)(max.m_123342_() + 1),
            (double)(Math.max(max.m_123343_(), end.m_123343_()) + 1)
         );
         return frustum.m_113029_(bounds.m_82400_(0.5));
      }
   }

   public void render(GateEntity entity, float yaw, float partialTick, PoseStack poses, MultiBufferSource buffers, int light) {
      VertexConsumer vertices = buffers.m_6299_(RenderType.m_110458_(this.getTextureLocation(entity)));
      int overlay = entity.hurtTime() > 0 ? OverlayTexture.m_118093_(OverlayTexture.m_118088_(0.0F), OverlayTexture.m_118096_(true)) : OverlayTexture.f_118083_;
      poses.m_85836_();
      if (entity.openingStyle() == GateEntity.OpeningStyle.DRAWBRIDGE && !entity.isXAxisGate()) {
         poses.m_252781_(Axis.f_252436_.m_252977_(90.0F));
      }

      poses.m_85841_(-1.0F, -1.0F, 1.0F);
      if (entity.openingStyle() == GateEntity.OpeningStyle.DRAWBRIDGE) {
         this.renderBridge(entity, poses, vertices, entity.openProgress(partialTick), light, overlay);
      } else {
         this.renderGate(entity, poses, vertices, entity.openProgress(partialTick), light, overlay);
      }

      poses.m_85849_();
      super.m_7392_(entity, yaw, partialTick, poses, buffers, light);
   }

   private void renderGate(GateEntity gate, PoseStack poses, VertexConsumer vertices, float progress, int light, int overlay) {
      int width = gate.widthBlocks();
      int height = gate.heightBlocks();
      boolean alongX = gate.isXAxisGate();
      float rotation = alongX ? 90.0F : 0.0F;
      String panel = gate.material() == GateEntity.Material.IRON ? "barsTop" : "part1";

      for (int row = 0; row < height; row++) {
         for (int col = 0; col < width; col++) {
            poses.m_85836_();
            float offset = (float)col - (float)(width - 1) * 0.5F;
            poses.m_252880_(alongX ? offset : 0.0F, (float)(-row), alongX ? 0.0F : offset);
            if (row == height - 1) {
               part(
                  this.basic,
                  col != 0 && col != width - 1 ? "top1" : "cornerTop1",
                  rotation + (float)(col == width - 1 ? 180 : 0),
                  poses,
                  vertices,
                  light,
                  overlay
               );
            } else if (col == 0 || col == width - 1) {
               part(this.basic, "side1", rotation + (float)(col == width - 1 ? 180 : 0), poses, vertices, light, overlay);
            }

            float movement = GateRenderLayout.panelMovement(gate.openingStyle(), width, col, progress);
            if (GateRenderLayout.panelVisible(gate.openingStyle(), width, height, col, row, progress)) {
               if (gate.openingStyle() == GateEntity.OpeningStyle.VERTICAL) {
                  poses.m_252880_(0.0F, -movement, 0.0F);
               } else {
                  poses.m_252880_(alongX ? movement : 0.0F, 0.0F, alongX ? 0.0F : movement);
               }

               part(this.basic, panel, rotation, poses, vertices, light, overlay);
            }

            poses.m_85849_();
         }
      }
   }

   private void renderBridge(GateEntity gate, PoseStack poses, VertexConsumer vertices, float angle, int light, int overlay) {
      int width = gate.widthBlocks();
      int height = gate.heightBlocks();
      int facingStep = gate.isXAxisGate() ? gate.facing().m_122431_() : gate.facing().m_122429_();
      poses.m_85837_(0.0, -0.5, 0.0);
      poses.m_252781_(Axis.f_252529_.m_252977_(facingStep >= 0 ? angle : -angle));
      if (facingStep < 0) {
         poses.m_252781_(Axis.f_252436_.m_252977_(180.0F));
      }

      for (int row = 0; row < height; row++) {
         for (int col = 0; col < width; col++) {
            poses.m_85836_();
            poses.m_252880_((float)col - (float)(width - 1) * 0.5F, (float)(-row), 0.0F);
            if (row == 0) {
               part(this.bridge, "part1", 180.0F, poses, vertices, light, overlay);
            }

            if (row == height - 1) {
               if (col == 0) {
                  this.parts(poses, vertices, light, overlay, "bridgeCorner1", "bridgeCorner2", "bridgeSide1", "bridgeSide2");
               } else if (col == width - 1) {
                  this.parts(poses, vertices, light, overlay, "bridgeCorner3", "bridgeCorner4", "bridgeSide3", "bridgeSide4");
               } else {
                  this.parts(poses, vertices, light, overlay, "bridgeTop1", "bridgeTop2");
               }
            } else if (row > 0 && col == 0) {
               this.parts(poses, vertices, light, overlay, "bridgeSide1", "bridgeSide2");
            } else if (row > 0 && col == width - 1) {
               this.parts(poses, vertices, light, overlay, "bridgeSide3", "bridgeSide4");
            }

            if (row > 0) {
               part(this.bridge, "bridgePiece", 180.0F, poses, vertices, light, overlay);
            }

            poses.m_85849_();
         }
      }
   }

   private void parts(PoseStack poses, VertexConsumer vertices, int light, int overlay, String... names) {
      for (String name : names) {
         part(this.bridge, name, 180.0F, poses, vertices, light, overlay);
      }
   }

   private static void part(ModelPart model, String name, float rotation, PoseStack poses, VertexConsumer vertices, int light, int overlay) {
      ModelPart part = model.m_171324_(name);
      part.f_104204_ = (float)Math.toRadians((double)rotation);
      part.m_104301_(poses, vertices, light, overlay);
   }
}
