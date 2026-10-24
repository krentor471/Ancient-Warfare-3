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
      this.lid = root.getChild("lid");
      this.wooden = wooden;
   }

   public static LayerDefinition createWoodLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "side_r2",
         CubeListBuilder.create().texOffs(0, 35).addBox(0.0F, 0.0F, 0.0F, 1.0F, 9.0F, 26.0F),
         PartPose.offsetAndRotation(-5.0F, 15.0F, -7.89F, 0.0F, -0.11519173F, 0.0F)
      );
      root.addOrReplaceChild(
         "bottom", CubeListBuilder.create().texOffs(24, 47).addBox(0.0F, 0.0F, 0.0F, 16.0F, 1.0F, 32.0F), PartPose.offset(-8.0F, 22.9F, -8.0F)
      );
      root.addOrReplaceChild(
         "side_l2",
         CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 1.0F, 9.0F, 26.0F),
         PartPose.offsetAndRotation(4.0F, 15.0F, -7.89F, 0.0F, 0.11519173F, 0.0F)
      );
      root.addOrReplaceChild(
         "side_bottom", CubeListBuilder.create().texOffs(0, 10).addBox(0.0F, 0.0F, 0.0F, 10.0F, 9.0F, 1.0F), PartPose.offset(-5.0F, 15.0F, -8.0F)
      );
      root.addOrReplaceChild(
         "side_top", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 10.0F, 9.0F, 1.0F), PartPose.offset(-5.0F, 15.0F, 23.0F)
      );
      root.addOrReplaceChild(
         "side_r1",
         CubeListBuilder.create().texOffs(28, 45).addBox(0.0F, 0.0F, 0.0F, 1.0F, 9.0F, 7.0F),
         PartPose.offsetAndRotation(-7.96F, 15.0F, 17.75F, 0.0F, 0.43633232F, 0.0F)
      );
      root.addOrReplaceChild(
         "side_l1",
         CubeListBuilder.create().texOffs(28, 10).addBox(0.0F, 0.0F, 0.0F, 1.0F, 9.0F, 7.0F),
         PartPose.offsetAndRotation(7.05F, 15.0F, 17.3F, 0.0F, -0.43633232F, 0.0F)
      );
      PartDefinition lid = root.addOrReplaceChild(
         "lid", CubeListBuilder.create().texOffs(24, 0).addBox(0.0F, -0.01F, 0.0F, 16.0F, 0.01F, 32.0F), PartPose.offset(-8.0F, 14.1F, -8.0F)
      );
      lid.addOrReplaceChild(
         "side_r1",
         CubeListBuilder.create().texOffs(118, 45).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 7.0F),
         PartPose.offsetAndRotation(0.0F, -0.1F, 25.6F, 0.0F, 0.43633232F, 0.0F)
      );
      lid.addOrReplaceChild(
         "side_r2",
         CubeListBuilder.create().texOffs(90, 27).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 26.0F),
         PartPose.offsetAndRotation(3.0F, -0.1F, 0.0F, 0.0F, -0.11519173F, 0.0F)
      );
      lid.addOrReplaceChild(
         "side_l2",
         CubeListBuilder.create().texOffs(90, 0).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 26.0F),
         PartPose.offsetAndRotation(12.0F, -0.09F, 0.0F, 0.0F, 0.11519173F, 0.0F)
      );
      lid.addOrReplaceChild(
         "side_bottom", CubeListBuilder.create().texOffs(90, 2).addBox(0.0F, 0.0F, 0.0F, 10.0F, 1.0F, 1.0F), PartPose.offset(3.0F, -0.11F, 0.0F)
      );
      lid.addOrReplaceChild(
         "side_l1",
         CubeListBuilder.create().texOffs(118, 18).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 7.0F),
         PartPose.offsetAndRotation(15.1F, -0.1F, 25.2F, 0.0F, -0.43633232F, 0.0F)
      );
      lid.addOrReplaceChild(
         "side_top", CubeListBuilder.create().texOffs(90, 0).addBox(0.0F, 0.0F, 0.0F, 10.0F, 1.0F, 1.0F), PartPose.offset(3.0F, -0.11F, 31.0F)
      );
      return LayerDefinition.create(mesh, 144, 80);
   }

   public static LayerDefinition createStoneLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition base = root.addOrReplaceChild(
         "base", CubeListBuilder.create().texOffs(-48, 0).addBox(-12.0F, -4.0F, -24.0F, 24.0F, 4.0F, 48.0F), PartPose.offset(0.0F, 24.0F, 0.0F)
      );
      base.addOrReplaceChild("south", CubeListBuilder.create().texOffs(0, 52).addBox(-12.0F, -12.0F, -24.0F, 24.0F, 12.0F, 4.0F), PartPose.ZERO);
      base.addOrReplaceChild(
         "north",
         CubeListBuilder.create().texOffs(0, 52).addBox(-12.0F, -12.0F, -24.0F, 24.0F, 12.0F, 4.0F),
         PartPose.rotation(0.0F, (float) -Math.PI, 0.0F)
      );
      base.addOrReplaceChild(
         "left",
         CubeListBuilder.create().texOffs(0, 68).addBox(-24.0F, -12.0F, -12.0F, 48.0F, 12.0F, 4.0F),
         PartPose.rotation(0.0F, (float) (Math.PI / 2), 0.0F)
      );
      base.addOrReplaceChild(
         "right",
         CubeListBuilder.create().texOffs(0, 68).addBox(-24.0F, -12.0F, -12.0F, 48.0F, 12.0F, 4.0F),
         PartPose.rotation(0.0F, (float) (-Math.PI / 2), 0.0F)
      );
      PartDefinition lid = root.addOrReplaceChild(
         "lid", CubeListBuilder.create().texOffs(48, 0).addBox(-10.0F, -37.0F, -4.0F, 20.0F, 44.0F, 4.0F), PartPose.offset(0.0F, 12.0F, -15.0F)
      );
      lid.addOrReplaceChild("inside", CubeListBuilder.create().mirror().texOffs(2, 1).addBox(-9.5F, -36.5F, -3.5F, 19.0F, 43.0F, 3.0F), PartPose.ZERO);
      return LayerDefinition.create(mesh, 128, 128);
   }

   public void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, float open) {
      if (this.wooden) {
         this.lid.xRot = 0.0F;
         this.lid.yRot = 0.0F;
         this.lid.zRot = -open * (float) (Math.PI / 2);
      } else {
         this.lid.xRot = (float) (-Math.PI / 2);
         this.lid.yRot = -open * (float) (Math.PI / 2);
         this.lid.zRot = 0.0F;
      }

      this.root.render(poseStack, consumer, light, overlay);
   }
}
