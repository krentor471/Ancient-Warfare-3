package net.shadowmage.ancientwarfare.core.forge;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkHooks;
import net.shadowmage.ancientwarfare.core.common.platform.PlatformEntitySpawnPacket;

public final class ForgeEntitySpawnPacket implements PlatformEntitySpawnPacket {
   @Override
   public Packet<ClientGamePacketListener> create(Entity entity) {
      return NetworkHooks.getEntitySpawningPacket(entity);
   }
}
