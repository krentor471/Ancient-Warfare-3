package net.shadowmage.ancientwarfare.core.common.platform;

import net.minecraft.core.BlockPos;

public interface PlatformMailboxNetwork {
   void sendConfig(BlockPos var1, int var2, String var3, boolean var4);

   void requestList(BlockPos var1);
}
