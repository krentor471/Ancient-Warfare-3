package net.shadowmage.ancientwarfare.core.forge;

import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.shadowmage.ancientwarfare.core.common.platform.PlatformConfigDirectory;
import net.shadowmage.ancientwarfare.core.common.platform.PlatformFuel;
import net.shadowmage.ancientwarfare.core.common.platform.PlatformModLoader;

public final class ForgePlatformAdapters {
   private ForgePlatformAdapters() {
   }

   public static PlatformModLoader modLoader() {
      return modId -> modId != null && ModList.get().isLoaded(modId);
   }

   public static PlatformConfigDirectory configDirectory() {
      return () -> FMLPaths.CONFIGDIR.get();
   }

   public static PlatformFuel fuel() {
      return stack -> ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
   }
}
