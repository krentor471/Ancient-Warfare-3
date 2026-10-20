package net.shadowmage.ancientwarfare.core.forge;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkHooks;
import net.shadowmage.ancientwarfare.core.common.platform.PlatformMenuOpener;

public class ForgeMenuOpener implements PlatformMenuOpener {
   @Override
   public void openMenu(Player player, MenuProvider provider, Consumer<FriendlyByteBuf> extraDataWriter) {
      if (player instanceof ServerPlayer serverPlayer) {
         NetworkHooks.openScreen(serverPlayer, provider, extraDataWriter);
      }
   }

   @Override
   public void openBlockMenu(Player player, MenuProvider provider, BlockPos pos) {
      if (player instanceof ServerPlayer serverPlayer) {
         NetworkHooks.openScreen(serverPlayer, provider, pos);
      }
   }
}
