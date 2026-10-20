package net.shadowmage.ancientwarfare.core.forge;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.common.ForgeMod;
import net.shadowmage.ancientwarfare.core.common.platform.PlatformAttributeAccess;

public final class ForgeAttributeAccess implements PlatformAttributeAccess {
   @Override
   public Attribute entityReachAttribute() {
      return (Attribute)ForgeMod.ENTITY_REACH.get();
   }
}
