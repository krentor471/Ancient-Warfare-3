package net.shadowmage.ancientwarfare.core.entity;

import java.util.Locale;
import net.minecraft.sounds.SoundEvent;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcDefaultsRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;

final class NpcVoiceSounds {
   private NpcVoiceSounds() {
   }

   static SoundEvent hurt(NpcBaseEntity npc) {
      return event(npc, "hurt");
   }

   static SoundEvent death(NpcBaseEntity npc) {
      SoundEvent sound = event(npc, "death");
      return sound == null && npc instanceof FactionNpcEntity && "barbarian".equals(profile(npc)) ? AWCorePlatform.npcVoice("human_death") : sound;
   }

   static void playFactionAttack(FactionNpcEntity npc) {
      SoundEvent sound = event(npc, "attack");
      if (sound != null) {
         float pitch = 1.2F / (npc.m_217043_().m_188501_() * 0.2F + 0.9F);
         npc.m_5496_(sound, 1.0F, pitch);
      }
   }

   private static SoundEvent event(NpcBaseEntity npc, String action) {
      String profile = profile(npc);
      return !profile.isBlank() && !"none".equals(profile) ? AWCorePlatform.npcVoice(profile + "_" + action) : null;
   }

   private static String profile(NpcBaseEntity npc) {
      if (npc instanceof FactionNpcEntity factionNpc) {
         FactionNpcRole role = FactionNpcRole.byId(npc.getNpcSubtype()).orElse(FactionNpcRole.SOLDIER);
         String configured = FactionNpcDefaultsRegistry.additionalAttribute(factionNpc.getFactionName(), role, "entity_sound").orElse("none");
         return normalize(configured);
      } else if (npc instanceof PlayerOwnedNpcEntity) {
         return npc.isAlexModel() ? "human_female" : "human";
      } else {
         return "none";
      }
   }

   private static String normalize(String raw) {
      String value = raw == null ? "none" : raw.trim().toLowerCase(Locale.ROOT);
      return "coven_puppetd".equals(value) ? "coven_puppet" : value;
   }
}
