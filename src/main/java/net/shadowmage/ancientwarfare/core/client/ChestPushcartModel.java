package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.shadowmage.ancientwarfare.core.entity.ChestPushcartEntity;

public class ChestPushcartModel extends EntityModel<ChestPushcartEntity> {
   public static final String LAYER_NAME = "chest_pushcart";
   private final ModelPart body;
   private final ModelPart crates;
   private final ModelPart hitch;
   private final ModelPart frontLeftWheel;
   private final ModelPart frontRightWheel;
   private final ModelPart rearLeftWheel;
   private final ModelPart rearRightWheel;

   public ChestPushcartModel(ModelPart root) {
      this.body = root.m_171324_("body");
      this.crates = root.m_171324_("crates");
      this.hitch = root.m_171324_("hitch");
      this.frontLeftWheel = root.m_171324_("front_left_wheel");
      this.frontRightWheel = root.m_171324_("front_right_wheel");
      this.rearLeftWheel = root.m_171324_("rear_left_wheel");
      this.rearRightWheel = root.m_171324_("rear_right_wheel");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.m_171576_();
      root.m_171599_(
         "body",
         CubeListBuilder.m_171558_()
            .m_171514_(0, 0)
            .m_171481_(-18.0F, 0.0F, -26.0F, 36.0F, 2.0F, 52.0F)
            .m_171514_(0, 0)
            .m_171481_(-18.0F, 2.0F, -26.0F, 2.0F, 10.0F, 52.0F)
            .m_171514_(0, 0)
            .m_171481_(16.0F, 2.0F, -26.0F, 2.0F, 10.0F, 52.0F)
            .m_171514_(0, 0)
            .m_171481_(-18.0F, 2.0F, -28.0F, 36.0F, 10.0F, 2.0F)
            .m_171514_(0, 0)
            .m_171481_(-18.0F, 2.0F, 26.0F, 36.0F, 10.0F, 2.0F)
            .m_171514_(0, 0)
            .m_171481_(-19.0F, 12.0F, -27.0F, 38.0F, 2.0F, 4.0F)
            .m_171514_(0, 0)
            .m_171481_(-19.0F, 12.0F, 23.0F, 38.0F, 2.0F, 4.0F)
            .m_171514_(0, 0)
            .m_171481_(-19.0F, 12.0F, -27.0F, 4.0F, 2.0F, 54.0F)
            .m_171514_(0, 0)
            .m_171481_(15.0F, 12.0F, -27.0F, 4.0F, 2.0F, 54.0F),
         PartPose.f_171404_
      );
      root.m_171599_(
         "crates",
         CubeListBuilder.m_171558_()
            .m_171514_(0, 0)
            .m_171481_(-15.0F, 4.0F, -23.0F, 9.0F, 12.0F, 22.0F)
            .m_171514_(0, 0)
            .m_171481_(-4.5F, 4.0F, -23.0F, 9.0F, 12.0F, 22.0F)
            .m_171514_(0, 0)
            .m_171481_(6.0F, 4.0F, -23.0F, 9.0F, 12.0F, 22.0F)
            .m_171514_(0, 0)
            .m_171481_(-15.0F, 4.0F, 1.0F, 9.0F, 12.0F, 22.0F)
            .m_171514_(0, 0)
            .m_171481_(-4.5F, 4.0F, 1.0F, 9.0F, 12.0F, 22.0F)
            .m_171514_(0, 0)
            .m_171481_(6.0F, 4.0F, 1.0F, 9.0F, 12.0F, 22.0F),
         PartPose.f_171404_
      );
      root.m_171599_(
         "hitch",
         CubeListBuilder.m_171558_()
            .m_171514_(0, 0)
            .m_171481_(-4.5F, -0.75F, -18.0F, 1.5F, 1.5F, 20.0F)
            .m_171514_(0, 0)
            .m_171481_(3.0F, -0.75F, -18.0F, 1.5F, 1.5F, 20.0F)
            .m_171514_(0, 0)
            .m_171481_(-5.5F, -0.75F, -20.0F, 11.0F, 1.5F, 2.0F),
         PartPose.m_171419_(0.0F, 6.75F, -26.0F)
      );
      addWheel(root, "front_left_wheel", -21.0F, -5.0F, -16.0F);
      addWheel(root, "front_right_wheel", 21.0F, -5.0F, -16.0F);
      addWheel(root, "rear_left_wheel", -21.0F, -5.0F, 16.0F);
      addWheel(root, "rear_right_wheel", 21.0F, -5.0F, 16.0F);
      return LayerDefinition.m_171565_(mesh, 64, 64);
   }

   private static void addWheel(PartDefinition root, String name, float x, float y, float z) {
      root.m_171599_(
         name,
         CubeListBuilder.m_171558_()
            .m_171514_(0, 0)
            .m_171481_(-2.5F, -5.5F, -5.5F, 5.0F, 11.0F, 11.0F)
            .m_171514_(0, 0)
            .m_171481_(-3.0F, -1.0F, -6.5F, 6.0F, 2.0F, 13.0F)
            .m_171514_(0, 0)
            .m_171481_(-3.0F, -6.5F, -1.0F, 6.0F, 13.0F, 2.0F),
         PartPose.m_171419_(x, y, z)
      );
   }

   public void setupAnim(ChestPushcartEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      float wheel = Mth.m_14179_(ageInTicks - (float)entity.f_19797_, entity.wheelRotationPrev, entity.wheelRotation) * (float) (Math.PI / 180.0);
      this.frontLeftWheel.f_104203_ = wheel;
      this.frontRightWheel.f_104203_ = wheel;
      this.rearLeftWheel.f_104203_ = wheel;
      this.rearRightWheel.f_104203_ = wheel;
      double speed = entity.m_20184_().m_165924_();
      this.hitch.f_104203_ = speed > 0.02 ? 0.0F : 0.43633232F;
   }

   public void renderBody(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
      this.body.m_104301_(poseStack, buffer, packedLight, packedOverlay);
      this.hitch.m_104301_(poseStack, buffer, packedLight, packedOverlay);
   }

   public void renderCrates(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
      this.crates.m_104301_(poseStack, buffer, packedLight, packedOverlay);
   }

   public void renderWheels(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
      this.frontLeftWheel.m_104301_(poseStack, buffer, packedLight, packedOverlay);
      this.frontRightWheel.m_104301_(poseStack, buffer, packedLight, packedOverlay);
      this.rearLeftWheel.m_104301_(poseStack, buffer, packedLight, packedOverlay);
      this.rearRightWheel.m_104301_(poseStack, buffer, packedLight, packedOverlay);
   }

   public void m_7695_(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      this.renderBody(poseStack, buffer, packedLight, packedOverlay);
      this.renderCrates(poseStack, buffer, packedLight, packedOverlay);
      this.renderWheels(poseStack, buffer, packedLight, packedOverlay);
   }
}
