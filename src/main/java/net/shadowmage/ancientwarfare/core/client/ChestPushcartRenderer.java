package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.ChestPushcartEntity;

public class ChestPushcartRenderer extends EntityRenderer<ChestPushcartEntity> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(AWCoreIds.id("chest_pushcart"), "main");
   private static final ResourceLocation[] BODY_TEXTURES = new ResourceLocation[]{
      AWCoreIds.id("textures/entity/vehicle/chest_pushcart/aw5_oak_planks_light.png"),
      AWCoreIds.id("textures/entity/vehicle/chest_pushcart/variants/chest_pushcart_body_iron_band_1.png"),
      AWCoreIds.id("textures/entity/vehicle/chest_pushcart/variants/chest_pushcart_body_iron_band_2.png"),
      AWCoreIds.id("textures/entity/vehicle/chest_pushcart/variants/chest_pushcart_body_iron_body.png"),
      AWCoreIds.id("textures/entity/vehicle/chest_pushcart/variants/chest_pushcart_body_steel_body.png")
   };
   private static final ResourceLocation CRATE_TEXTURE = AWCoreIds.id("textures/entity/vehicle/chest_pushcart/aw4_chest_crate.png");
   private static final ResourceLocation WHEEL_TEXTURE = AWCoreIds.id("textures/entity/vehicle/chest_pushcart/aw4_dark_wheel_wood.png");
   private final ChestPushcartModel model;

   public ChestPushcartRenderer(Context context) {
      super(context);
      this.shadowRadius = 0.8F;
      this.model = new ChestPushcartModel(context.bakeLayer(LAYER));
   }

   public void render(ChestPushcartEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      poseStack.pushPose();
      poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.lerp(partialTick, entity.yRotO, entity.getYRot())));
      poseStack.translate(0.0, 0.0, 0.0);
      poseStack.scale(1.0F, 1.0F, 1.0F);
      this.model.setupAnim(entity, 0.0F, 0.0F, (float)entity.tickCount + partialTick, 0.0F, 0.0F);
      this.model.renderBody(poseStack, bufferSource.getBuffer(this.model.renderType(this.getTextureLocation(entity))), packedLight, OverlayTexture.NO_OVERLAY);
      this.model.renderCrates(poseStack, bufferSource.getBuffer(this.model.renderType(CRATE_TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY);
      this.model.renderWheels(poseStack, bufferSource.getBuffer(this.model.renderType(WHEEL_TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY);
      poseStack.popPose();
      super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
   }

   public ResourceLocation getTextureLocation(ChestPushcartEntity entity) {
      return BODY_TEXTURES[entity.getMaterialLevel()];
   }
}
