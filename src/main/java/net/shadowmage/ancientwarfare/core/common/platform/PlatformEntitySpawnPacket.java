package net.shadowmage.ancientwarfare.core.common.platform;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;

public interface PlatformEntitySpawnPacket {
   Packet<ClientGamePacketListener> create(Entity var1);
}
