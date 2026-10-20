package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.block.TorqueGeneratorBlock;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueGeneratorKind;
import net.shadowmage.ancientwarfare.core.tile.TorqueGeneratorBlockEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class TorqueGeneratorGeoModel extends GeoModel<TorqueGeneratorBlockEntity> {
   private static final ResourceLocation WATERWHEEL_MODEL = AWCoreIds.id("geo/block/automation/waterwheel_generator.geo.json");
   private static final ResourceLocation WINDMILL_MODEL_3X3 = AWCoreIds.id("geo/block/automation/windmill_blade_3x3.geo.json");
   private static final ResourceLocation WINDMILL_MODEL_5X5 = AWCoreIds.id("geo/block/automation/windmill_blade_5x5.geo.json");
   private static final ResourceLocation WINDMILL_MODEL_7X7 = AWCoreIds.id("geo/block/automation/windmill_blade_7x7.geo.json");
   private static final ResourceLocation WINDMILL_STATIC_MODEL = AWCoreIds.id("geo/block/automation/windmill_generator_static.geo.json");
   private static final ResourceLocation STIRLING_MODEL = AWCoreIds.id("geo/block/automation/stirling_generator.geo.json");
   private static final ResourceLocation WATERWHEEL_TEXTURE = AWCoreIds.id("textures/block/automation/waterwheel_generator.png");
   private static final ResourceLocation WINDMILL_TEXTURE = AWCoreIds.id("textures/block/automation/windmill_generator.png");
   private static final ResourceLocation WINDMILL_RUNTIME_TEXTURE = AWCoreIds.id("textures/block/automation/windmill_running_atlas.png");
   private static final ResourceLocation STIRLING_TEXTURE = AWCoreIds.id("textures/block/automation/stirling_generator.png");
   private static final ResourceLocation ANIMATION = AWCoreIds.id("animations/block/automation/generator.animation.json");

   public ResourceLocation getModelResource(TorqueGeneratorBlockEntity animatable) {
      return switch (this.kind(animatable)) {
         case WINDMILL -> {
            if (animatable.hasValidWindmillStructureForRender()) {
               switch (animatable.countWindmillBladesForRender()) {
                  case 3:
                     yield WINDMILL_MODEL_3X3;
                  case 7:
                     yield WINDMILL_MODEL_7X7;
                  default:
                     yield WINDMILL_MODEL_5X5;
               }
            } else {
               yield WINDMILL_STATIC_MODEL;
            }
         }
         case WATERWHEEL -> WATERWHEEL_MODEL;
         case STIRLING -> STIRLING_MODEL;
      };
   }

   public ResourceLocation getTextureResource(TorqueGeneratorBlockEntity animatable) {
      if (this.kind(animatable) == TorqueGeneratorKind.WINDMILL) {
         return animatable.hasValidWindmillStructureForRender() ? WINDMILL_RUNTIME_TEXTURE : WINDMILL_TEXTURE;
      } else {
         return this.kind(animatable) == TorqueGeneratorKind.STIRLING ? STIRLING_TEXTURE : WATERWHEEL_TEXTURE;
      }
   }

   public ResourceLocation getAnimationResource(TorqueGeneratorBlockEntity animatable) {
      return ANIMATION;
   }

   public void handleAnimations(TorqueGeneratorBlockEntity animatable, long instanceId, AnimationState<TorqueGeneratorBlockEntity> animationState) {
      super.handleAnimations(animatable, instanceId, animationState);
      if (animatable.m_58904_() != null) {
         TorqueGeneratorKind kind = this.kind(animatable);
         float ticks = (float)animatable.m_58904_().m_46467_() + animationState.getPartialTick();
         if (kind == TorqueGeneratorKind.WATERWHEEL) {
            if (!animatable.hasValidWaterwheelStructureForRender()) {
               this.setRotZ("waterwheelSpindle", 0.0F);
               this.setRotZ("outputGear", 0.0F);
            }
         } else if (kind == TorqueGeneratorKind.WINDMILL) {
            CoreGeoBone rotor = this.getAnimationProcessor().getBone("rotatingAssembly");
            if (rotor != null) {
               boolean running = animatable.hasValidWindmillStructureForRender();
               rotor.setHidden(!running);
               rotor.setRotZ(running ? (float)Math.toRadians((double)(ticks * 7.5F)) : 0.0F);
            }
         }
      }
   }

   private void setRotZ(String name, float angle) {
      CoreGeoBone bone = this.getAnimationProcessor().getBone(name);
      if (bone != null) {
         bone.setRotZ(angle);
      }
   }

   private TorqueGeneratorKind kind(TorqueGeneratorBlockEntity animatable) {
      return animatable.m_58900_().m_60734_() instanceof TorqueGeneratorBlock block ? block.getKind() : TorqueGeneratorKind.WATERWHEEL;
   }
}
