package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

final class LegacyGateParts {
   private LegacyGateParts() {
   }

   static ModelPart basic() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition part1 = root.addOrReplaceChild(
         "part1",
         CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -16.0F, -8.0F, 2.0F, 16.0F, 16.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition top1 = root.addOrReplaceChild(
         "top1",
         CubeListBuilder.create().texOffs(41, 26).addBox(4.0F, 0.0F, -8.0F, 2.0F, 6.0F, 16.0F),
         PartPose.offsetAndRotation(0.0F, -16.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition top4 = top1.addOrReplaceChild(
         "top4",
         CubeListBuilder.create().texOffs(39, 0).addBox(1.0F, 0.0F, -8.0F, 3.0F, 8.0F, 16.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition top3 = top1.addOrReplaceChild(
         "top3",
         CubeListBuilder.create().texOffs(39, 0).addBox(0.0F, 0.0F, -8.0F, 3.0F, 8.0F, 16.0F),
         PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition top2 = top1.addOrReplaceChild(
         "top2",
         CubeListBuilder.create().texOffs(41, 26).addBox(0.0F, 0.0F, -8.0F, 2.0F, 6.0F, 16.0F),
         PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition barsTop = root.addOrReplaceChild(
         "barsTop",
         CubeListBuilder.create().texOffs(0, 75).addBox(-1.0F, -13.0F, -8.0F, 2.0F, 2.0F, 16.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition barsLeft1 = barsTop.addOrReplaceChild(
         "barsLeft1",
         CubeListBuilder.create().texOffs(0, 52).addBox(-1.0F, -16.0F, -5.0F, 2.0F, 3.0F, 2.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition barsLeft3 = barsTop.addOrReplaceChild(
         "barsLeft3",
         CubeListBuilder.create().texOffs(0, 58).addBox(-1.0F, -11.0F, -5.0F, 2.0F, 6.0F, 2.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition barsLeft5 = barsTop.addOrReplaceChild(
         "barsLeft5",
         CubeListBuilder.create().texOffs(0, 67).addBox(-1.0F, -3.0F, -5.0F, 2.0F, 3.0F, 2.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition barsRight1 = barsTop.addOrReplaceChild(
         "barsRight1",
         CubeListBuilder.create().texOffs(27, 52).addBox(-1.0F, -16.0F, 3.0F, 2.0F, 3.0F, 2.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition barsRight3 = barsTop.addOrReplaceChild(
         "barsRight3",
         CubeListBuilder.create().texOffs(27, 58).addBox(-1.0F, -11.0F, 3.0F, 2.0F, 6.0F, 2.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition barsRight5 = barsTop.addOrReplaceChild(
         "barsRight5",
         CubeListBuilder.create().texOffs(27, 67).addBox(-1.0F, -3.0F, 3.0F, 2.0F, 3.0F, 2.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition barsBottom = barsTop.addOrReplaceChild(
         "barsBottom",
         CubeListBuilder.create().texOffs(0, 33).addBox(-1.0F, -5.0F, -8.0F, 2.0F, 2.0F, 16.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition cornerTop1 = root.addOrReplaceChild(
         "cornerTop1",
         CubeListBuilder.create().texOffs(39, 49).addBox(1.0F, 0.0F, 0.0F, 3.0F, 8.0F, 8.0F),
         PartPose.offsetAndRotation(0.0F, -16.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition cornerTop2 = cornerTop1.addOrReplaceChild(
         "cornerTop2",
         CubeListBuilder.create().texOffs(39, 66).addBox(0.0F, 0.0F, 0.0F, 2.0F, 6.0F, 10.0F),
         PartPose.offsetAndRotation(4.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition cornerSide2 = cornerTop1.addOrReplaceChild(
         "cornerSide2",
         CubeListBuilder.create().texOffs(62, 83).addBox(0.0F, 0.0F, 0.0F, 2.0F, 16.0F, 6.0F),
         PartPose.offsetAndRotation(4.0F, 0.0F, -8.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition cornerSide1 = cornerTop1.addOrReplaceChild(
         "cornerSide1",
         CubeListBuilder.create().texOffs(39, 83).addBox(0.0F, 0.0F, 0.0F, 3.0F, 16.0F, 8.0F),
         PartPose.offsetAndRotation(1.0F, 0.0F, -8.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition cornerTop3 = cornerTop1.addOrReplaceChild(
         "cornerTop3",
         CubeListBuilder.create().texOffs(62, 49).addBox(0.0F, 0.0F, 0.0F, 3.0F, 8.0F, 8.0F),
         PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition cornerTop4 = cornerTop1.addOrReplaceChild(
         "cornerTop4",
         CubeListBuilder.create().texOffs(64, 66).addBox(0.0F, 0.0F, 0.0F, 2.0F, 6.0F, 10.0F),
         PartPose.offsetAndRotation(-6.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition cornerSide3 = cornerTop1.addOrReplaceChild(
         "cornerSide3",
         CubeListBuilder.create().texOffs(39, 108).addBox(0.0F, 0.0F, 0.0F, 3.0F, 16.0F, 8.0F),
         PartPose.offsetAndRotation(-4.0F, 0.0F, -8.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition cornerSide4 = cornerTop1.addOrReplaceChild(
         "cornerSide4",
         CubeListBuilder.create().texOffs(62, 108).addBox(0.0F, 0.0F, 0.0F, 2.0F, 16.0F, 6.0F),
         PartPose.offsetAndRotation(-6.0F, 0.0F, -8.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition side1 = root.addOrReplaceChild(
         "side1",
         CubeListBuilder.create().texOffs(39, 83).addBox(1.0F, 0.0F, -8.0F, 3.0F, 16.0F, 8.0F),
         PartPose.offsetAndRotation(0.0F, -16.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition side2 = side1.addOrReplaceChild(
         "side2",
         CubeListBuilder.create().texOffs(62, 83).addBox(0.0F, 0.0F, -8.0F, 2.0F, 16.0F, 6.0F),
         PartPose.offsetAndRotation(4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition side3 = side1.addOrReplaceChild(
         "side3",
         CubeListBuilder.create().texOffs(39, 108).addBox(0.0F, 0.0F, -8.0F, 3.0F, 16.0F, 8.0F),
         PartPose.offsetAndRotation(-4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition side4 = side1.addOrReplaceChild(
         "side4",
         CubeListBuilder.create().texOffs(62, 108).addBox(0.0F, 0.0F, -8.0F, 2.0F, 16.0F, 6.0F),
         PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(mesh, 256, 256).bakeRoot();
   }

   static ModelPart bridge() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition part1 = root.addOrReplaceChild(
         "part1",
         CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgePiece = root.addOrReplaceChild(
         "bridgePiece",
         CubeListBuilder.create().texOffs(0, 33).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 8.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeSide1 = root.addOrReplaceChild(
         "bridgeSide1",
         CubeListBuilder.create().texOffs(0, 58).addBox(-2.0F, -8.0F, 0.0F, 10.0F, 16.0F, 3.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeSide4 = root.addOrReplaceChild(
         "bridgeSide4",
         CubeListBuilder.create().texOffs(27, 58).addBox(-8.0F, -8.0F, 3.0F, 6.0F, 16.0F, 3.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeCorner1 = root.addOrReplaceChild(
         "bridgeCorner1",
         CubeListBuilder.create().texOffs(49, 34).addBox(-8.0F, -8.0F, 0.0F, 6.0F, 9.0F, 3.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeCorner2 = root.addOrReplaceChild(
         "bridgeCorner2",
         CubeListBuilder.create().texOffs(49, 47).addBox(-8.0F, -8.0F, 3.0F, 10.0F, 6.0F, 3.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeTop1 = root.addOrReplaceChild(
         "bridgeTop1",
         CubeListBuilder.create().texOffs(49, 58).addBox(-8.0F, -8.0F, 0.0F, 16.0F, 9.0F, 3.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeTop2 = root.addOrReplaceChild(
         "bridgeTop2",
         CubeListBuilder.create().texOffs(49, 72).addBox(-8.0F, -8.0F, 3.0F, 16.0F, 6.0F, 3.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeSide3 = root.addOrReplaceChild(
         "bridgeSide3",
         CubeListBuilder.create().texOffs(0, 58).addBox(-8.0F, -8.0F, 0.0F, 10.0F, 16.0F, 3.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeSide2 = root.addOrReplaceChild(
         "bridgeSide2",
         CubeListBuilder.create().texOffs(27, 58).addBox(2.0F, -8.0F, 3.0F, 6.0F, 16.0F, 3.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeCorner4 = root.addOrReplaceChild(
         "bridgeCorner4",
         CubeListBuilder.create().texOffs(49, 47).addBox(-2.0F, -8.0F, 3.0F, 10.0F, 6.0F, 3.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeCorner3 = root.addOrReplaceChild(
         "bridgeCorner3",
         CubeListBuilder.create().texOffs(49, 34).addBox(2.0F, -8.0F, 0.0F, 6.0F, 9.0F, 3.0F),
         PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      return LayerDefinition.create(mesh, 256, 256).bakeRoot();
   }
}
