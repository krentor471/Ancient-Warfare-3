package net.shadowmage.ancientwarfare.core.common.npc;

import net.minecraft.resources.ResourceLocation;

public record NpcSubtype(String id, ResourceLocation equipment, NpcWorkType workType) {
}
