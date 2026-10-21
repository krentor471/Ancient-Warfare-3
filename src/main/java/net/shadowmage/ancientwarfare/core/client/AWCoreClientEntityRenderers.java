package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.entity.BardNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.CourierNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionArcherEliteNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionArcherNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionBardNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionCavalryNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionCivilianFemaleNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionCivilianMaleNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionLeaderEliteNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionLeaderNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionMountedArcherNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionPriestNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionSiegeEngineerNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionSoldierEliteNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionSoldierNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionTraderNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.NpcBaseEntity;
import net.shadowmage.ancientwarfare.core.entity.PriestNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.SiegeEngineerNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.TraderNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.WorkerNpcEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class AWCoreClientEntityRenderers {
   private static AWCoreClientEntityRenderers.RendererRegistrar rendererRegistrar;

   private AWCoreClientEntityRenderers() {
   }

   public static void setRendererRegistrar(AWCoreClientEntityRenderers.RendererRegistrar registrar) {
      rendererRegistrar = registrar;
   }

   private static <T extends Entity> void register(EntityType<? extends T> entityType, EntityRendererProvider<T> provider) {
      if (rendererRegistrar == null) {
         throw new IllegalStateException("Entity renderer registrar has not been configured by the active loader");
      } else {
         rendererRegistrar.register(entityType, provider);
      }
   }

   public static void registerRenderers() {
      register(AWCoreRegistryAccess.entityType(AWCoreIds.id("chest_pushcart")), ChestPushcartGeoRenderer::new);
      AWCoreClientEntityRenderers.EntityRenderers.register(AWCoreRegistryAccess.entityType(AWCoreIds.id("ballista_mobile")), BallistaMobileGeoRenderer::new);
      AWCoreClientEntityRenderers.EntityRenderers.register(AWCoreRegistryAccess.entityType(AWCoreIds.id("ballista_boat")), BallistaBoatGeoRenderer::new);
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.id("ballista_turret_stand")), BallistaTurretStandGeoRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.id("ballista_stand_fixed")), BallistaTurretStandGeoRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(AWCoreRegistryAccess.entityType(AWCoreIds.id("ballista_bolt")), BallistaBoltRenderer::new);
      AWCoreClientEntityRenderers.EntityRenderers.register(AWCoreRegistryAccess.entityType(AWCoreIds.id("trebuchet_mobile")), TrebuchetMobileGeoRenderer::new);
      AWCoreClientEntityRenderers.EntityRenderers.register(AWCoreRegistryAccess.entityType(AWCoreIds.id("trebuchet_fixed")), TrebuchetFixedGeoRenderer::new);
      AWCoreClientEntityRenderers.EntityRenderers.register(AWCoreRegistryAccess.entityType(AWCoreIds.id("trebuchet_turret")), TrebuchetTurretGeoRenderer::new);
      AWCoreClientEntityRenderers.EntityRenderers.register(AWCoreRegistryAccess.entityType(AWCoreIds.id("giant_trebuchet")), GiantTrebuchetGeoRenderer::new);
      AWCoreClientEntityRenderers.EntityRenderers.register(AWCoreRegistryAccess.entityType(AWCoreIds.id("catapult_mobile")), CatapultMobileGeoRenderer::new);
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.id("catapult_turret_stand")), CatapultTurretStandGeoRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(AWCoreRegistryAccess.entityType(AWCoreIds.id("catapult_boat")), CatapultBoatGeoRenderer::new);
      AWCoreClientEntityRenderers.EntityRenderers.register(AWCoreRegistryAccess.entityType(AWCoreIds.id("transport_boat")), TransportBoatGeoRenderer::new);
      AWCoreClientEntityRenderers.EntityRenderers.register(AWCoreRegistryAccess.entityType(AWCoreIds.id("catapult_shot")), CatapultShotRenderer::new);
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.id("cannon_turret_stand")), CannonTurretStandGeoRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(AWCoreRegistryAccess.entityType(AWCoreIds.id("cannon_mobile")), CannonMobileGeoRenderer::new);
      AWCoreClientEntityRenderers.EntityRenderers.register(AWCoreRegistryAccess.entityType(AWCoreIds.id("cannon_shot")), CannonShotRenderer::new);
      AWCoreClientEntityRenderers.EntityRenderers.register(AWCoreRegistryAccess.entityType(AWCoreIds.id("battering_ram")), BatteringRamGeoRenderer::new);
      AWCoreClientEntityRenderers.EntityRenderers.register(AWCoreRegistryAccess.entityType(AWCoreIds.id("hwacha")), HwachaGeoRenderer::new);
      AWCoreClientEntityRenderers.EntityRenderers.register(AWCoreRegistryAccess.entityType(AWCoreIds.id("hwacha_rocket")), HwachaRocketRenderer::new);
      AWCoreClientEntityRenderers.EntityRenderers.register(AWCoreRegistryAccess.entityType(AWCoreIds.DYNAMIC_GATE), LegacyDynamicGateRenderer::new);
      registerNpcRenderers();
   }

   public static void registerNpcRenderers() {
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.WORKER_NPC), AWCoreClientEntityRenderers.WorkerRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.COMBAT_NPC), AWCoreClientEntityRenderers.CombatRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.COURIER_NPC), AWCoreClientEntityRenderers.CourierRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.TRADER_NPC), AWCoreClientEntityRenderers.TraderRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(AWCoreRegistryAccess.entityType(AWCoreIds.BARD_NPC), AWCoreClientEntityRenderers.BardRenderer::new);
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.PRIEST_NPC), AWCoreClientEntityRenderers.PriestRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.SIEGE_ENGINEER_NPC), AWCoreClientEntityRenderers.SiegeEngineerRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_SOLDIER_NPC), AWCoreClientEntityRenderers.FactionSoldierRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_SOLDIER_ELITE_NPC), AWCoreClientEntityRenderers.FactionSoldierEliteRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_ARCHER_NPC), AWCoreClientEntityRenderers.FactionArcherRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_ARCHER_ELITE_NPC), AWCoreClientEntityRenderers.FactionArcherEliteRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_CAVALRY_NPC), AWCoreClientEntityRenderers.FactionCavalryRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_MOUNTED_ARCHER_NPC), AWCoreClientEntityRenderers.FactionMountedArcherRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_LEADER_NPC), AWCoreClientEntityRenderers.FactionLeaderRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_LEADER_ELITE_NPC), AWCoreClientEntityRenderers.FactionLeaderEliteRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_SIEGE_ENGINEER_NPC), AWCoreClientEntityRenderers.FactionSiegeEngineerRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_CIVILIAN_MALE_NPC), AWCoreClientEntityRenderers.FactionCivilianMaleRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_CIVILIAN_FEMALE_NPC), AWCoreClientEntityRenderers.FactionCivilianFemaleRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_PRIEST_NPC), AWCoreClientEntityRenderers.FactionPriestRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_BARD_NPC), AWCoreClientEntityRenderers.FactionBardRenderer::new
      );
      AWCoreClientEntityRenderers.EntityRenderers.register(
         AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_TRADER_NPC), AWCoreClientEntityRenderers.FactionTraderRenderer::new
      );
   }

   private static class BardRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<BardNpcEntity> {
      private BardRenderer(Context context) {
         super(context, new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM)));
      }

      public ResourceLocation getTextureLocation(BardNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private abstract static class BaseNpcRenderer<T extends NpcBaseEntity> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
      private static final double MORALE_BAR_RANGE_SQ = 32.0 * 32.0;
      private final HumanoidModel<T> wideModel;
      private final HumanoidModel<T> slimModel;

      private BaseNpcRenderer(Context context, HumanoidModel<T> wideModel, HumanoidModel<T> slimModel) {
         super(context, wideModel, 0.5F);
         this.wideModel = wideModel;
         this.slimModel = slimModel;
         this.addLayer(new ItemInHandLayer(this, context.getItemInHandRenderer()));
         this.addLayer(
            new HumanoidArmorLayer(
               this,
               new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
               new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
               context.getModelManager()
            )
         );
      }

      public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
         this.model = entity.isAlexModel() ? this.slimModel : this.wideModel;
         super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
         this.renderStatusIcons(entity, poseStack, bufferSource, packedLight);
      }

      private void renderStatusIcons(T entity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
         if (this.entityRenderDispatcher.camera != null
            && !(this.entityRenderDispatcher.distanceToSqr(entity) > 4096.0)
            && Minecraft.getInstance().player != null
            && Minecraft.getInstance().player.hasLineOfSight(entity)) {
            List<ResourceLocation> icons = this.statusIcons(entity.getStatusIconFlags());
            boolean routing = (entity.getStatusIconFlags() & NpcBaseEntity.ICON_ROUTING) != 0;
            boolean moraleBar = entity.showsMorale()
               && (routing || entity.getShownMorale() < 100)
               && this.entityRenderDispatcher.distanceToSqr(entity) <= MORALE_BAR_RANGE_SQ;
            if (!icons.isEmpty() || moraleBar) {
               float scale = 0.02666667F;
               float startX = -((float)icons.size() * 10.0F / 2.0F) * scale;
               float y = entity.getBbHeight() + 0.5F;
               poseStack.pushPose();
               poseStack.translate(0.0, (double)y, 0.0);
               poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
               poseStack.scale(-scale, -scale, scale);

               for (int i = 0; i < icons.size(); i++) {
                  ResourceLocation icon = icons.get(i);
                  VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityTranslucent(icon));
                  float x = startX / scale + (float)i * 20.0F;
                  this.drawIcon(poseStack, consumer, x, -16.0F, packedLight);
               }

               if (moraleBar) {
                  this.drawMoraleBar(poseStack, bufferSource, entity.getShownMorale(), routing, entity.tickCount, icons.isEmpty() ? -6.0F : -22.0F);
               }

               poseStack.popPose();
            }
         }
      }

      /**
       * Morale under the icons: green when steady, yellow when shaken, red when about to break. A routing soldier's bar
       * blinks red, so a fleeing man stands out from far off.
       */
      private void drawMoraleBar(PoseStack poseStack, MultiBufferSource bufferSource, int morale, boolean routing, int tickCount, float y) {
         VertexConsumer consumer = bufferSource.getBuffer(RenderType.textBackground());
         Matrix4f matrix = poseStack.last().pose();
         float half = 16.0F;
         float fill = -half + 2.0F * half * Math.max(0, Math.min(100, morale)) / 100.0F;
         int light = 15728880;
         int frame = routing && tickCount / 5 % 2 == 0 ? 0xFFFF2020 : 0xFF000000;
         this.quad(consumer, matrix, -half - 1.0F, y - 1.0F, half + 1.0F, y + 5.0F, frame, light, 0.0F);
         this.quad(consumer, matrix, -half, y, half, y + 4.0F, 0xFF404040, light, -0.01F);
         int color = morale > 60 ? 0xFF30D030 : (morale > 35 ? 0xFFE8C020 : 0xFFE02020);
         if (fill > -half) {
            this.quad(consumer, matrix, -half, y, fill, y + 4.0F, color, light, -0.02F);
         }
      }

      private void quad(VertexConsumer consumer, Matrix4f matrix, float x0, float y0, float x1, float y1, int argb, int light, float z) {
         int a = argb >>> 24 & 0xFF;
         int r = argb >> 16 & 0xFF;
         int g = argb >> 8 & 0xFF;
         int b = argb & 0xFF;
         consumer.vertex(matrix, x0, y1, z).color(r, g, b, a).uv2(light).endVertex();
         consumer.vertex(matrix, x1, y1, z).color(r, g, b, a).uv2(light).endVertex();
         consumer.vertex(matrix, x1, y0, z).color(r, g, b, a).uv2(light).endVertex();
         consumer.vertex(matrix, x0, y0, z).color(r, g, b, a).uv2(light).endVertex();
      }

      private List<ResourceLocation> statusIcons(int flags) {
         List<ResourceLocation> icons = new ArrayList<>();
         this.addIcon(icons, flags, 1, "task_attack");
         this.addIcon(icons, flags, 2, "task_upkeep");
         this.addIcon(icons, flags, 4, "task_upkeep2");
         this.addIcon(icons, flags, 8, "task_home");
         this.addIcon(icons, flags, 16, "task_work");
         this.addIcon(icons, flags, 32, "task_patrol");
         this.addIcon(icons, flags, 64, "task_guard");
         this.addIcon(icons, flags, 128, "task_follow");
         this.addIcon(icons, flags, 256, "task_wander");
         this.addIcon(icons, flags, 512, "task_move");
         this.addIcon(icons, flags, 1024, "task_alarm");
         this.addIcon(icons, flags, 2048, "task_flee");
         this.addIcon(icons, flags, 4096, "task_sleep");
         this.addIcon(icons, flags, 8192, "task_rain");
         this.addIcon(icons, flags, NpcBaseEntity.ICON_RELOADING, "task_reload");
         this.addIcon(icons, flags, NpcBaseEntity.ICON_AIMING, "task_aim");
         this.addIcon(icons, flags, NpcBaseEntity.ICON_SHOOTING, "task_shoot");
         return icons;
      }

      private void addIcon(List<ResourceLocation> icons, int flags, int flag, String name) {
         if ((flags & flag) != 0) {
            icons.add(this.aiIcon(name));
         }
      }

      private ResourceLocation aiIcon(String name) {
         return AWCoreIds.id("textures/entity/npc/ai/" + name + ".png");
      }

      private void drawIcon(PoseStack poseStack, VertexConsumer consumer, float x, float y, int packedLight) {
         Pose pose = poseStack.last();
         Matrix4f matrix = pose.pose();
         Matrix3f normal = pose.normal();
         float left = x - 8.0F;
         float right = x + 8.0F;
         float bottom = y + 16.0F;
         this.iconVertex(consumer, matrix, normal, left, bottom, 0.0F, 1.0F, 1.0F, packedLight);
         this.iconVertex(consumer, matrix, normal, right, bottom, 1.0F, 1.0F, 1.0F, packedLight);
         this.iconVertex(consumer, matrix, normal, right, y, 1.0F, 0.0F, 1.0F, packedLight);
         this.iconVertex(consumer, matrix, normal, left, y, 0.0F, 0.0F, 1.0F, packedLight);
      }

      private void iconVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float u, float v, float normalZ, int packedLight) {
         consumer.vertex(matrix, x, y, 0.0F)
            .color(255, 255, 255, 255)
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(packedLight)
            .normal(normal, 0.0F, 0.0F, normalZ)
            .endVertex();
      }
   }

   private static class CombatRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<CombatNpcEntity> {
      private CombatRenderer(Context context) {
         super(
            context,
            new AWCoreClientEntityRenderers.ShieldingHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
            new AWCoreClientEntityRenderers.ShieldingHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM))
         );
      }

      public ResourceLocation getTextureLocation(CombatNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private static class CourierRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<CourierNpcEntity> {
      private CourierRenderer(Context context) {
         super(context, new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM)));
      }

      public ResourceLocation getTextureLocation(CourierNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private static final class EntityRenderers {
      private static <T extends Entity> void register(EntityType<? extends T> entityType, EntityRendererProvider<T> provider) {
         AWCoreClientEntityRenderers.register(entityType, provider);
      }
   }

   private static class FactionArcherEliteRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<FactionArcherEliteNpcEntity> {
      private FactionArcherEliteRenderer(Context context) {
         super(context, new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM)));
      }

      public ResourceLocation getTextureLocation(FactionArcherEliteNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private static class FactionArcherRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<FactionArcherNpcEntity> {
      private FactionArcherRenderer(Context context) {
         super(context, new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM)));
      }

      public ResourceLocation getTextureLocation(FactionArcherNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private static class FactionBardRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<FactionBardNpcEntity> {
      private FactionBardRenderer(Context context) {
         super(context, new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM)));
      }

      public ResourceLocation getTextureLocation(FactionBardNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private static class FactionCavalryRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<FactionCavalryNpcEntity> {
      private FactionCavalryRenderer(Context context) {
         super(
            context,
            new AWCoreClientEntityRenderers.ShieldingHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
            new AWCoreClientEntityRenderers.ShieldingHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM))
         );
      }

      public ResourceLocation getTextureLocation(FactionCavalryNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private static class FactionCivilianFemaleRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<FactionCivilianFemaleNpcEntity> {
      private FactionCivilianFemaleRenderer(Context context) {
         super(context, new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM)));
      }

      public ResourceLocation getTextureLocation(FactionCivilianFemaleNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private static class FactionCivilianMaleRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<FactionCivilianMaleNpcEntity> {
      private FactionCivilianMaleRenderer(Context context) {
         super(context, new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM)));
      }

      public ResourceLocation getTextureLocation(FactionCivilianMaleNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private static class FactionLeaderEliteRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<FactionLeaderEliteNpcEntity> {
      private FactionLeaderEliteRenderer(Context context) {
         super(
            context,
            new AWCoreClientEntityRenderers.ShieldingHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
            new AWCoreClientEntityRenderers.ShieldingHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM))
         );
      }

      public ResourceLocation getTextureLocation(FactionLeaderEliteNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private static class FactionLeaderRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<FactionLeaderNpcEntity> {
      private FactionLeaderRenderer(Context context) {
         super(
            context,
            new AWCoreClientEntityRenderers.ShieldingHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
            new AWCoreClientEntityRenderers.ShieldingHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM))
         );
      }

      public ResourceLocation getTextureLocation(FactionLeaderNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private static class FactionMountedArcherRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<FactionMountedArcherNpcEntity> {
      private FactionMountedArcherRenderer(Context context) {
         super(context, new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM)));
      }

      public ResourceLocation getTextureLocation(FactionMountedArcherNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private static class FactionPriestRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<FactionPriestNpcEntity> {
      private FactionPriestRenderer(Context context) {
         super(context, new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM)));
      }

      public ResourceLocation getTextureLocation(FactionPriestNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private static class FactionSiegeEngineerRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<FactionSiegeEngineerNpcEntity> {
      private FactionSiegeEngineerRenderer(Context context) {
         super(context, new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM)));
      }

      public ResourceLocation getTextureLocation(FactionSiegeEngineerNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private static class FactionSoldierEliteRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<FactionSoldierEliteNpcEntity> {
      private FactionSoldierEliteRenderer(Context context) {
         super(
            context,
            new AWCoreClientEntityRenderers.ShieldingHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
            new AWCoreClientEntityRenderers.ShieldingHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM))
         );
      }

      public ResourceLocation getTextureLocation(FactionSoldierEliteNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private static class FactionSoldierRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<FactionSoldierNpcEntity> {
      private FactionSoldierRenderer(Context context) {
         super(
            context,
            new AWCoreClientEntityRenderers.ShieldingHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)),
            new AWCoreClientEntityRenderers.ShieldingHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM))
         );
      }

      public ResourceLocation getTextureLocation(FactionSoldierNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private static class FactionTraderRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<FactionTraderNpcEntity> {
      private FactionTraderRenderer(Context context) {
         super(context, new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM)));
      }

      public ResourceLocation getTextureLocation(FactionTraderNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private static class PriestRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<PriestNpcEntity> {
      private PriestRenderer(Context context) {
         super(context, new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM)));
      }

      public ResourceLocation getTextureLocation(PriestNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   @FunctionalInterface
   public interface RendererRegistrar {
      <T extends Entity> void register(EntityType<? extends T> var1, EntityRendererProvider<T> var2);
   }

   private static class ShieldingHumanoidModel<T extends NpcBaseEntity> extends NpcHumanoidModel<T> {
      private ShieldingHumanoidModel(ModelPart root) {
         super(root);
      }

      @Override
      public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
         super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
      }
   }

   private static class SiegeEngineerRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<SiegeEngineerNpcEntity> {
      private SiegeEngineerRenderer(Context context) {
         super(context, new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM)));
      }

      public ResourceLocation getTextureLocation(SiegeEngineerNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private static class TraderRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<TraderNpcEntity> {
      private TraderRenderer(Context context) {
         super(context, new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM)));
      }

      public ResourceLocation getTextureLocation(TraderNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }

   private static class WorkerRenderer extends AWCoreClientEntityRenderers.BaseNpcRenderer<WorkerNpcEntity> {
      private WorkerRenderer(Context context) {
         super(context, new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), new NpcHumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM)));
      }

      public ResourceLocation getTextureLocation(WorkerNpcEntity entity) {
         return NpcSkinRegistry.textureFor(entity);
      }
   }
}
