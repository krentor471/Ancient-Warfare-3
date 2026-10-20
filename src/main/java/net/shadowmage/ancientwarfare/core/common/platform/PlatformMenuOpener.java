package net.shadowmage.ancientwarfare.core.common.platform;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;

public interface PlatformMenuOpener {
   void openMenu(Player var1, MenuProvider var2, Consumer<FriendlyByteBuf> var3);

   void openBlockMenu(Player var1, MenuProvider var2, BlockPos var3);
}
