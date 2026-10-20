package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;

public final class LegacyCoffinModel {
   public static final ModelLayerLocation WOOD_LAYER = new ModelLayerLocation(AWCoreIds.id("legacy_wooden_coffin"), "main");
   public static final ModelLayerLocation STONE_LAYER = new ModelLayerLocation(AWCoreIds.id("legacy_stone_coffin"), "main");
   private final ModelPart root;
   private final ModelPart lid;
   private final boolean wooden;

   public LegacyCoffinModel(ModelPart root, boolean wooden) {
      this.root = root;
      this.lid = root.m_171324_("lid");
      this.wooden = wooden;
   }

   public static LayerDefinition createWoodLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.m_171576_();
      root.m_171599_(
         "side_r2",
         CubeListBuilder.m_171558_().m_171514_(0, 35).m_171481_(0.0F, 0.0F, 0.0F, 1.0F, 9.0F, 26.0F),
         PartPose.m_171423_(-5.0F, 15.0F, -7.89F, 0.0F, -0.11519173F, 0.0F)
      );
      root.m_171599_(
         "bottom", CubeListBuilder.m_171558_().m_171514_(24, 47).m_171481_(0.0F, 0.0F, 0.0F, 16.0F, 1.0F, 32.0F), PartPose.m_171419_(-8.0F, 22.9F, -8.0F)
      );
      root.m_171599_(
         "side_l2",
         CubeListBuilder.m_171558_().m_171514_(0, 0).m_171481_(0.0F, 0.0F, 0.0F, 1.0F, 9.0F, 26.0F),
         PartPose.m_171423_(4.0F, 15.0F, -7.89F, 0.0F, 0.11519173F, 0.0F)
      );
      root.m_171599_(
         "side_bottom", CubeListBuilder.m_171558_().m_171514_(0, 10).m_171481_(0.0F, 0.0F, 0.0F, 10.0F, 9.0F, 1.0F), PartPose.m_171419_(-5.0F, 15.0F, -8.0F)
      );
      root.m_171599_(
         "side_top", CubeListBuilder.m_171558_().m_171514_(0, 0).m_171481_(0.0F, 0.0F, 0.0F, 10.0F, 9.0F, 1.0F), PartPose.m_171419_(-5.0F, 15.0F, 23.0F)
      );
      root.m_171599_(
         "side_r1",
         CubeListBuilder.m_171558_().m_171514_(28, 45).m_171481_(0.0F, 0.0F, 0.0F, 1.0F, 9.0F, 7.0F),
         PartPose.m_171423_(-7.96F, 15.0F, 17.75F, 0.0F, 0.43633232F, 0.0F)
      );
      root.m_171599_(
         "side_l1",
         CubeListBuilder.m_171558_().m_171514_(28, 10).m_171481_(0.0F, 0.0F, 0.0F, 1.0F, 9.0F, 7.0F),
         PartPose.m_171423_(7.05F, 15.0F, 17.3F, 0.0F, -0.43633232F, 0.0F)
      );
      PartDefinition lid = root.m_171599_(
         "lid", CubeListBuilder.m_171558_().m_171514_(24, 0).m_171481_(0.0F, -0.01F, 0.0F, 16.0F, 0.01F, 32.0F), PartPose.m_171419_(-8.0F, 14.1F, -8.0F)
      );
      lid.m_171599_(
         "side_r1",
         CubeListBuilder.m_171558_().m_171514_(118, 45).m_171481_(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 7.0F),
         PartPose.m_171423_(0.0F, -0.1F, 25.6F, 0.0F, 0.43633232F, 0.0F)
      );
      lid.m_171599_(
         "side_r2",
         CubeListBuilder.m_171558_().m_171514_(90, 27).m_171481_(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 26.0F),
         PartPose.m_171423_(3.0F, -0.1F, 0.0F, 0.0F, -0.11519173F, 0.0F)
      );
      lid.m_171599_(
         "side_l2",
         CubeListBuilder.m_171558_().m_171514_(90, 0).m_171481_(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 26.0F),
         PartPose.m_171423_(12.0F, -0.09F, 0.0F, 0.0F, 0.11519173F, 0.0F)
      );
      lid.m_171599_(
         "side_bottom", CubeListBuilder.m_171558_().m_171514_(90, 2).m_171481_(0.0F, 0.0F, 0.0F, 10.0F, 1.0F, 1.0F), PartPose.m_171419_(3.0F, -0.11F, 0.0F)
      );
      lid.m_171599_(
         "side_l1",
         CubeListBuilder.m_171558_().m_171514_(118, 18).m_171481_(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 7.0F),
         PartPose.m_171423_(15.1F, -0.1F, 25.2F, 0.0F, -0.43633232F, 0.0F)
      );
      lid.m_171599_(
         "side_top", CubeListBuilder.m_171558_().m_171514_(90, 0).m_171481_(0.0F, 0.0F, 0.0F, 10.0F, 1.0F, 1.0F), PartPose.m_171419_(3.0F, -0.11F, 31.0F)
      );
      return LayerDefinition.m_171565_(mesh, 144, 80);
   }

   public static LayerDefinition createStoneLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.m_171576_();
      PartDefinition base = root.m_171599_(
         "base", CubeListBuilder.m_171558_().m_171514_(-48, 0).m_171481_(-12.0F, -4.0F, -24.0F, 24.0F, 4.0F, 48.0F), PartPose.m_171419_(0.0F, 24.0F, 0.0F)
      );
      base.m_171599_("south", CubeListBuilder.m_171558_().m_171514_(0, 52).m_171481_(-12.0F, -12.0F, -24.0F, 24.0F, 12.0F, 4.0F), PartPose.f_171404_);
      base.m_171599_(
         "north",
         CubeListBuilder.m_171558_().m_171514_(0, 52).m_171481_(-12.0F, -12.0F, -24.0F, 24.0F, 12.0F, 4.0F),
         PartPose.m_171430_(0.0F, (float) -Math.PI, 0.0F)
      );
      base.m_171599_(
         "left",
         CubeListBuilder.m_171558_().m_171514_(0, 68).m_171481_(-24.0F, -12.0F, -12.0F, 48.0F, 12.0F, 4.0F),
         PartPose.m_171430_(0.0F, (float) (Math.PI / 2), 0.0F)
      );
      base.m_171599_(
         "right",
         CubeListBuilder.m_171558_().m_171514_(0, 68).m_171481_(-24.0F, -12.0F, -12.0F, 48.0F, 12.0F, 4.0F),
         PartPose.m_171430_(0.0F, (float) (-Math.PI / 2), 0.0F)
      );
      PartDefinition lid = root.m_171599_(
         "lid", CubeListBuilder.m_171558_().m_171514_(48, 0).m_171481_(-10.0F, -37.0F, -4.0F, 20.0F, 44.0F, 4.0F), PartPose.m_171419_(0.0F, 12.0F, -15.0F)
      );
      lid.m_171599_("inside", CubeListBuilder.m_171558_().m_171480_().m_171514_(2, 1).m_171481_(-9.5F, -36.5F, -3.5F, 19.0F, 43.0F, 3.0F), PartPose.f_171404_);
      return LayerDefinition.m_171565_(mesh, 128, 128);
   }

   public void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, float open) {
      if (this.wooden) {
         this.lid.f_104203_ = 0.0F;
         this.lid.f_104204_ = 0.0F;
         this.lid.f_104205_ = -open * (float) (Math.PI / 2);
      } else {
         this.lid.f_104203_ = (float) (-Math.PI / 2);
         this.lid.f_104204_ = -open * (float) (Math.PI / 2);
         this.lid.f_104205_ = 0.0F;
      }

      this.root.m_104301_(poseStack, consumer, light, overlay);
   }
}
