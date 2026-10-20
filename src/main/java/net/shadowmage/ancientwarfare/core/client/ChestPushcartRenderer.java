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
      this.f_114477_ = 0.8F;
      this.model = new ChestPushcartModel(context.m_174023_(LAYER));
   }

   public void render(ChestPushcartEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
      poseStack.m_85836_();
      poseStack.m_252781_(Axis.f_252436_.m_252977_(180.0F - Mth.m_14179_(partialTick, entity.f_19859_, entity.m_146908_())));
      poseStack.m_85837_(0.0, 0.0, 0.0);
      poseStack.m_85841_(1.0F, 1.0F, 1.0F);
      this.model.setupAnim(entity, 0.0F, 0.0F, (float)entity.f_19797_ + partialTick, 0.0F, 0.0F);
      this.model.renderBody(poseStack, bufferSource.m_6299_(this.model.m_103119_(this.getTextureLocation(entity))), packedLight, OverlayTexture.f_118083_);
      this.model.renderCrates(poseStack, bufferSource.m_6299_(this.model.m_103119_(CRATE_TEXTURE)), packedLight, OverlayTexture.f_118083_);
      this.model.renderWheels(poseStack, bufferSource.m_6299_(this.model.m_103119_(WHEEL_TEXTURE)), packedLight, OverlayTexture.f_118083_);
      poseStack.m_85849_();
      super.m_7392_(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
   }

   public ResourceLocation getTextureLocation(ChestPushcartEntity entity) {
      return BODY_TEXTURES[entity.getMaterialLevel()];
   }
}
