package net.shadowmage.ancientwarfare.core.common.inventory;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;

public interface PlatformItemHandlerProvider {
   @Nullable
   ItemHandlerAccess getItemHandler(@Nullable Direction var1);
}
