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
      PartDefinition root = mesh.m_171576_();
      PartDefinition part1 = root.m_171599_(
         "part1",
         CubeListBuilder.m_171558_().m_171514_(0, 0).m_171481_(-1.0F, -16.0F, -8.0F, 2.0F, 16.0F, 16.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition top1 = root.m_171599_(
         "top1",
         CubeListBuilder.m_171558_().m_171514_(41, 26).m_171481_(4.0F, 0.0F, -8.0F, 2.0F, 6.0F, 16.0F),
         PartPose.m_171423_(0.0F, -16.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition top4 = top1.m_171599_(
         "top4",
         CubeListBuilder.m_171558_().m_171514_(39, 0).m_171481_(1.0F, 0.0F, -8.0F, 3.0F, 8.0F, 16.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition top3 = top1.m_171599_(
         "top3",
         CubeListBuilder.m_171558_().m_171514_(39, 0).m_171481_(0.0F, 0.0F, -8.0F, 3.0F, 8.0F, 16.0F),
         PartPose.m_171423_(-4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition top2 = top1.m_171599_(
         "top2",
         CubeListBuilder.m_171558_().m_171514_(41, 26).m_171481_(0.0F, 0.0F, -8.0F, 2.0F, 6.0F, 16.0F),
         PartPose.m_171423_(-6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition barsTop = root.m_171599_(
         "barsTop",
         CubeListBuilder.m_171558_().m_171514_(0, 75).m_171481_(-1.0F, -13.0F, -8.0F, 2.0F, 2.0F, 16.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition barsLeft1 = barsTop.m_171599_(
         "barsLeft1",
         CubeListBuilder.m_171558_().m_171514_(0, 52).m_171481_(-1.0F, -16.0F, -5.0F, 2.0F, 3.0F, 2.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition barsLeft3 = barsTop.m_171599_(
         "barsLeft3",
         CubeListBuilder.m_171558_().m_171514_(0, 58).m_171481_(-1.0F, -11.0F, -5.0F, 2.0F, 6.0F, 2.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition barsLeft5 = barsTop.m_171599_(
         "barsLeft5",
         CubeListBuilder.m_171558_().m_171514_(0, 67).m_171481_(-1.0F, -3.0F, -5.0F, 2.0F, 3.0F, 2.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition barsRight1 = barsTop.m_171599_(
         "barsRight1",
         CubeListBuilder.m_171558_().m_171514_(27, 52).m_171481_(-1.0F, -16.0F, 3.0F, 2.0F, 3.0F, 2.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition barsRight3 = barsTop.m_171599_(
         "barsRight3",
         CubeListBuilder.m_171558_().m_171514_(27, 58).m_171481_(-1.0F, -11.0F, 3.0F, 2.0F, 6.0F, 2.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition barsRight5 = barsTop.m_171599_(
         "barsRight5",
         CubeListBuilder.m_171558_().m_171514_(27, 67).m_171481_(-1.0F, -3.0F, 3.0F, 2.0F, 3.0F, 2.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition barsBottom = barsTop.m_171599_(
         "barsBottom",
         CubeListBuilder.m_171558_().m_171514_(0, 33).m_171481_(-1.0F, -5.0F, -8.0F, 2.0F, 2.0F, 16.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition cornerTop1 = root.m_171599_(
         "cornerTop1",
         CubeListBuilder.m_171558_().m_171514_(39, 49).m_171481_(1.0F, 0.0F, 0.0F, 3.0F, 8.0F, 8.0F),
         PartPose.m_171423_(0.0F, -16.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition cornerTop2 = cornerTop1.m_171599_(
         "cornerTop2",
         CubeListBuilder.m_171558_().m_171514_(39, 66).m_171481_(0.0F, 0.0F, 0.0F, 2.0F, 6.0F, 10.0F),
         PartPose.m_171423_(4.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition cornerSide2 = cornerTop1.m_171599_(
         "cornerSide2",
         CubeListBuilder.m_171558_().m_171514_(62, 83).m_171481_(0.0F, 0.0F, 0.0F, 2.0F, 16.0F, 6.0F),
         PartPose.m_171423_(4.0F, 0.0F, -8.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition cornerSide1 = cornerTop1.m_171599_(
         "cornerSide1",
         CubeListBuilder.m_171558_().m_171514_(39, 83).m_171481_(0.0F, 0.0F, 0.0F, 3.0F, 16.0F, 8.0F),
         PartPose.m_171423_(1.0F, 0.0F, -8.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition cornerTop3 = cornerTop1.m_171599_(
         "cornerTop3",
         CubeListBuilder.m_171558_().m_171514_(62, 49).m_171481_(0.0F, 0.0F, 0.0F, 3.0F, 8.0F, 8.0F),
         PartPose.m_171423_(-4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition cornerTop4 = cornerTop1.m_171599_(
         "cornerTop4",
         CubeListBuilder.m_171558_().m_171514_(64, 66).m_171481_(0.0F, 0.0F, 0.0F, 2.0F, 6.0F, 10.0F),
         PartPose.m_171423_(-6.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition cornerSide3 = cornerTop1.m_171599_(
         "cornerSide3",
         CubeListBuilder.m_171558_().m_171514_(39, 108).m_171481_(0.0F, 0.0F, 0.0F, 3.0F, 16.0F, 8.0F),
         PartPose.m_171423_(-4.0F, 0.0F, -8.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition cornerSide4 = cornerTop1.m_171599_(
         "cornerSide4",
         CubeListBuilder.m_171558_().m_171514_(62, 108).m_171481_(0.0F, 0.0F, 0.0F, 2.0F, 16.0F, 6.0F),
         PartPose.m_171423_(-6.0F, 0.0F, -8.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition side1 = root.m_171599_(
         "side1",
         CubeListBuilder.m_171558_().m_171514_(39, 83).m_171481_(1.0F, 0.0F, -8.0F, 3.0F, 16.0F, 8.0F),
         PartPose.m_171423_(0.0F, -16.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition side2 = side1.m_171599_(
         "side2",
         CubeListBuilder.m_171558_().m_171514_(62, 83).m_171481_(0.0F, 0.0F, -8.0F, 2.0F, 16.0F, 6.0F),
         PartPose.m_171423_(4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition side3 = side1.m_171599_(
         "side3",
         CubeListBuilder.m_171558_().m_171514_(39, 108).m_171481_(0.0F, 0.0F, -8.0F, 3.0F, 16.0F, 8.0F),
         PartPose.m_171423_(-4.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition side4 = side1.m_171599_(
         "side4",
         CubeListBuilder.m_171558_().m_171514_(62, 108).m_171481_(0.0F, 0.0F, -8.0F, 2.0F, 16.0F, 6.0F),
         PartPose.m_171423_(-6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      return LayerDefinition.m_171565_(mesh, 256, 256).m_171564_();
   }

   static ModelPart bridge() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.m_171576_();
      PartDefinition part1 = root.m_171599_(
         "part1",
         CubeListBuilder.m_171558_().m_171514_(0, 0).m_171481_(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgePiece = root.m_171599_(
         "bridgePiece",
         CubeListBuilder.m_171558_().m_171514_(0, 33).m_171481_(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 8.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeSide1 = root.m_171599_(
         "bridgeSide1",
         CubeListBuilder.m_171558_().m_171514_(0, 58).m_171481_(-2.0F, -8.0F, 0.0F, 10.0F, 16.0F, 3.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeSide4 = root.m_171599_(
         "bridgeSide4",
         CubeListBuilder.m_171558_().m_171514_(27, 58).m_171481_(-8.0F, -8.0F, 3.0F, 6.0F, 16.0F, 3.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeCorner1 = root.m_171599_(
         "bridgeCorner1",
         CubeListBuilder.m_171558_().m_171514_(49, 34).m_171481_(-8.0F, -8.0F, 0.0F, 6.0F, 9.0F, 3.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeCorner2 = root.m_171599_(
         "bridgeCorner2",
         CubeListBuilder.m_171558_().m_171514_(49, 47).m_171481_(-8.0F, -8.0F, 3.0F, 10.0F, 6.0F, 3.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeTop1 = root.m_171599_(
         "bridgeTop1",
         CubeListBuilder.m_171558_().m_171514_(49, 58).m_171481_(-8.0F, -8.0F, 0.0F, 16.0F, 9.0F, 3.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeTop2 = root.m_171599_(
         "bridgeTop2",
         CubeListBuilder.m_171558_().m_171514_(49, 72).m_171481_(-8.0F, -8.0F, 3.0F, 16.0F, 6.0F, 3.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeSide3 = root.m_171599_(
         "bridgeSide3",
         CubeListBuilder.m_171558_().m_171514_(0, 58).m_171481_(-8.0F, -8.0F, 0.0F, 10.0F, 16.0F, 3.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeSide2 = root.m_171599_(
         "bridgeSide2",
         CubeListBuilder.m_171558_().m_171514_(27, 58).m_171481_(2.0F, -8.0F, 3.0F, 6.0F, 16.0F, 3.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeCorner4 = root.m_171599_(
         "bridgeCorner4",
         CubeListBuilder.m_171558_().m_171514_(49, 47).m_171481_(-2.0F, -8.0F, 3.0F, 10.0F, 6.0F, 3.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      PartDefinition bridgeCorner3 = root.m_171599_(
         "bridgeCorner3",
         CubeListBuilder.m_171558_().m_171514_(49, 34).m_171481_(2.0F, -8.0F, 0.0F, 6.0F, 9.0F, 3.0F),
         PartPose.m_171423_(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
      );
      return LayerDefinition.m_171565_(mesh, 256, 256).m_171564_();
   }
}
