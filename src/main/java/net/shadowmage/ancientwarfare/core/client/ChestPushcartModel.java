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
      this.body = root.getChild("body");
      this.crates = root.getChild("crates");
      this.hitch = root.getChild("hitch");
      this.frontLeftWheel = root.getChild("front_left_wheel");
      this.frontRightWheel = root.getChild("front_right_wheel");
      this.rearLeftWheel = root.getChild("rear_left_wheel");
      this.rearRightWheel = root.getChild("rear_right_wheel");
   }

   public static LayerDefinition createBodyLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild(
         "body",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-18.0F, 0.0F, -26.0F, 36.0F, 2.0F, 52.0F)
            .texOffs(0, 0)
            .addBox(-18.0F, 2.0F, -26.0F, 2.0F, 10.0F, 52.0F)
            .texOffs(0, 0)
            .addBox(16.0F, 2.0F, -26.0F, 2.0F, 10.0F, 52.0F)
            .texOffs(0, 0)
            .addBox(-18.0F, 2.0F, -28.0F, 36.0F, 10.0F, 2.0F)
            .texOffs(0, 0)
            .addBox(-18.0F, 2.0F, 26.0F, 36.0F, 10.0F, 2.0F)
            .texOffs(0, 0)
            .addBox(-19.0F, 12.0F, -27.0F, 38.0F, 2.0F, 4.0F)
            .texOffs(0, 0)
            .addBox(-19.0F, 12.0F, 23.0F, 38.0F, 2.0F, 4.0F)
            .texOffs(0, 0)
            .addBox(-19.0F, 12.0F, -27.0F, 4.0F, 2.0F, 54.0F)
            .texOffs(0, 0)
            .addBox(15.0F, 12.0F, -27.0F, 4.0F, 2.0F, 54.0F),
         PartPose.ZERO
      );
      root.addOrReplaceChild(
         "crates",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-15.0F, 4.0F, -23.0F, 9.0F, 12.0F, 22.0F)
            .texOffs(0, 0)
            .addBox(-4.5F, 4.0F, -23.0F, 9.0F, 12.0F, 22.0F)
            .texOffs(0, 0)
            .addBox(6.0F, 4.0F, -23.0F, 9.0F, 12.0F, 22.0F)
            .texOffs(0, 0)
            .addBox(-15.0F, 4.0F, 1.0F, 9.0F, 12.0F, 22.0F)
            .texOffs(0, 0)
            .addBox(-4.5F, 4.0F, 1.0F, 9.0F, 12.0F, 22.0F)
            .texOffs(0, 0)
            .addBox(6.0F, 4.0F, 1.0F, 9.0F, 12.0F, 22.0F),
         PartPose.ZERO
      );
      root.addOrReplaceChild(
         "hitch",
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-4.5F, -0.75F, -18.0F, 1.5F, 1.5F, 20.0F)
            .texOffs(0, 0)
            .addBox(3.0F, -0.75F, -18.0F, 1.5F, 1.5F, 20.0F)
            .texOffs(0, 0)
            .addBox(-5.5F, -0.75F, -20.0F, 11.0F, 1.5F, 2.0F),
         PartPose.offset(0.0F, 6.75F, -26.0F)
      );
      addWheel(root, "front_left_wheel", -21.0F, -5.0F, -16.0F);
      addWheel(root, "front_right_wheel", 21.0F, -5.0F, -16.0F);
      addWheel(root, "rear_left_wheel", -21.0F, -5.0F, 16.0F);
      addWheel(root, "rear_right_wheel", 21.0F, -5.0F, 16.0F);
      return LayerDefinition.create(mesh, 64, 64);
   }

   private static void addWheel(PartDefinition root, String name, float x, float y, float z) {
      root.addOrReplaceChild(
         name,
         CubeListBuilder.create()
            .texOffs(0, 0)
            .addBox(-2.5F, -5.5F, -5.5F, 5.0F, 11.0F, 11.0F)
            .texOffs(0, 0)
            .addBox(-3.0F, -1.0F, -6.5F, 6.0F, 2.0F, 13.0F)
            .texOffs(0, 0)
            .addBox(-3.0F, -6.5F, -1.0F, 6.0F, 13.0F, 2.0F),
         PartPose.offset(x, y, z)
      );
   }

   public void setupAnim(ChestPushcartEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      float wheel = Mth.lerp(ageInTicks - (float)entity.tickCount, entity.wheelRotationPrev, entity.wheelRotation) * (float) (Math.PI / 180.0);
      this.frontLeftWheel.xRot = wheel;
      this.frontRightWheel.xRot = wheel;
      this.rearLeftWheel.xRot = wheel;
      this.rearRightWheel.xRot = wheel;
      double speed = entity.getDeltaMovement().horizontalDistance();
      this.hitch.xRot = speed > 0.02 ? 0.0F : 0.43633232F;
   }

   public void renderBody(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
      this.body.render(poseStack, buffer, packedLight, packedOverlay);
      this.hitch.render(poseStack, buffer, packedLight, packedOverlay);
   }

   public void renderCrates(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
      this.crates.render(poseStack, buffer, packedLight, packedOverlay);
   }

   public void renderWheels(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
      this.frontLeftWheel.render(poseStack, buffer, packedLight, packedOverlay);
      this.frontRightWheel.render(poseStack, buffer, packedLight, packedOverlay);
      this.rearLeftWheel.render(poseStack, buffer, packedLight, packedOverlay);
      this.rearRightWheel.render(poseStack, buffer, packedLight, packedOverlay);
   }

   public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
      this.renderBody(poseStack, buffer, packedLight, packedOverlay);
      this.renderCrates(poseStack, buffer, packedLight, packedOverlay);
      this.renderWheels(poseStack, buffer, packedLight, packedOverlay);
   }
}
