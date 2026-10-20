package net.shadowmage.ancientwarfare.core.common.npc;

import java.util.Optional;
import java.util.UUID;

public interface NpcOwnerAccess {
   Optional<UUID> getOwner();
}
