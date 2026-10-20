package net.shadowmage.ancientwarfare.core.forge;

import net.minecraftforge.eventbus.api.IEventBus;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreBlockEntities;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreBlocks;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreCreativeTabs;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreItems;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreMenuTypes;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreRecipeSerializers;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreRecipeTypes;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreSounds;

public final class ForgeCoreRegistries {
   private ForgeCoreRegistries() {
   }

   public static void register(IEventBus modBus) {
      AWCoreItems.REGISTER.register(modBus);
      AWCoreBlocks.REGISTER.register(modBus);
      AWCoreBlockEntities.REGISTER.register(modBus);
      AWCoreEntityTypes.REGISTER.register(modBus);
      AWCoreCreativeTabs.REGISTER.register(modBus);
      AWCoreMenuTypes.REGISTER.register(modBus);
      AWCoreRecipeSerializers.REGISTER.register(modBus);
      AWCoreRecipeTypes.REGISTER.register(modBus);
      AWCoreSounds.REGISTER.register(modBus);
   }
}
