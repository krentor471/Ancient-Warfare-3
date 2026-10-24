package net.shadowmage.ancientwarfare.core.forge.init;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreSoundIds;

public final class AWCoreSounds {
   public static final DeferredRegister<SoundEvent> REGISTER = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, "ancientwarfare");
   private static final Map<String, RegistryObject<SoundEvent>> NPC_VOICES = new HashMap<>();

   private AWCoreSounds() {
   }

   public static SoundEvent npcVoice(String name) {
      RegistryObject<SoundEvent> sound = NPC_VOICES.get(name);
      return sound != null && sound.isPresent() ? (SoundEvent)sound.get() : null;
   }

   public static boolean hasNpcVoice(String name) {
      return NPC_VOICES.containsKey(name);
   }

   private static void registerNpcVoice(String name) {
      NPC_VOICES.put(name, REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(AWCoreIds.id(name))));
   }

   static {
      AWCoreSoundIds.NPC_VOICES.forEach(AWCoreSounds::registerNpcVoice);
   }
}
