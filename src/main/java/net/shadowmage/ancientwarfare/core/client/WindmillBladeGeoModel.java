package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.tile.WindmillBladeBlockEntity;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class WindmillBladeGeoModel extends GeoModel<WindmillBladeBlockEntity> {
   private static final ResourceLocation CUBE_MODEL = AWCoreIds.id("geo/block/automation/windmill_blade_cube.geo.json");
   private static final ResourceLocation WINDMILL_MODEL_3X3 = AWCoreIds.id("geo/block/automation/windmill_blade_3x3.geo.json");
   private static final ResourceLocation WINDMILL_MODEL_5X5 = AWCoreIds.id("geo/block/automation/windmill_blade_5x5.geo.json");
   private static final ResourceLocation WINDMILL_MODEL_7X7 = AWCoreIds.id("geo/block/automation/windmill_blade_7x7.geo.json");
   private static final ResourceLocation EMPTY_MODEL = AWCoreIds.id("geo/block/automation/empty_generator.geo.json");
   private static final ResourceLocation CUBE_TEXTURE = AWCoreIds.id("textures/block/automation/windmill_blade_cube.png");
   private static final ResourceLocation WINDMILL_TEXTURE = AWCoreIds.id("textures/block/automation/windmill_running_atlas.png");
   private static final ResourceLocation ANIMATION = AWCoreIds.id("animations/block/automation/generator.animation.json");

   public ResourceLocation getModelResource(WindmillBladeBlockEntity blade) {
      if (!blade.isControl()) {
         return EMPTY_MODEL;
      } else {
         return switch (blade.getWindmillSize()) {
            case 3 -> WINDMILL_MODEL_3X3;
            case 7 -> WINDMILL_MODEL_7X7;
            default -> WINDMILL_MODEL_5X5;
         };
      }
   }

   public ResourceLocation getTextureResource(WindmillBladeBlockEntity blade) {
      return blade.isControl() ? WINDMILL_TEXTURE : CUBE_TEXTURE;
   }

   public ResourceLocation getAnimationResource(WindmillBladeBlockEntity blade) {
      return ANIMATION;
   }

   public void handleAnimations(WindmillBladeBlockEntity blade, long instanceId, AnimationState<WindmillBladeBlockEntity> animationState) {
      super.handleAnimations(blade, instanceId, animationState);
      CoreGeoBone rotor = this.getAnimationProcessor().getBone("rotatingAssembly");
      if (rotor != null) {
         float angle = blade.isFormed() && blade.isControl() ? (float)Math.toRadians((double)blade.getRotation(animationState.getPartialTick())) : 0.0F;
         rotor.setRotZ(angle);
      }
   }
}
