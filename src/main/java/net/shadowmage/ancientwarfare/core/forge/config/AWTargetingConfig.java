package net.shadowmage.ancientwarfare.core.forge.config;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargetWhitelist;

public final class AWTargetingConfig {
   public static final ForgeConfigSpec SPEC;
   private static final ConfigValue<List<? extends String>> FRIENDLY_ENTITY_WHITELIST;

   private AWTargetingConfig() {
   }

   public static void onConfigChanged(ModConfigEvent event) {
      if (event.getConfig().getSpec() == SPEC) {
         refresh();
      }
   }

   public static void refresh() {
      NpcTargetWhitelist.replace((Collection<? extends String>)FRIENDLY_ENTITY_WHITELIST.get());
   }

   private static boolean validEntityId(Object value) {
      if (!(value instanceof String string)) {
         return false;
      } else {
         ResourceLocation id = ResourceLocation.m_135820_(string.trim().toLowerCase(Locale.ROOT));
         return id != null && !id.m_135827_().isBlank() && !id.m_135815_().isBlank();
      }
   }

   static {
      Builder builder = new Builder();
      builder.comment(new String[]{"Ancient Warfare NPC targeting settings.", "These settings are server-authoritative in multiplayer."}).push("targeting");
      FRIENDLY_ENTITY_WHITELIST = builder.comment(
            new String[]{
               "Entity ids that automatic NPC targeting must treat as friendly.",
               "Use full ids such as [\"minecraft:villager\", \"examplemod:peaceful_guard\"].",
               "This explicit friendly override takes priority over entity tags, including npc_hostile.",
               "Unlisted entities retain their npc_friendly/npc_neutral/npc_hostile tags.",
               "Otherwise, unknown mod creatures are hostile; vanilla creatures keep their monster/passive defaults.",
               "This controls automatic targeting only; explicit attack orders and self-defense are unchanged.",
               "Edit this file on the server for multiplayer; a client's whitelist does not override the server."
            }
         )
         .defineListAllowEmpty("friendlyEntityWhitelist", List.of(), AWTargetingConfig::validEntityId);
      builder.pop();
      SPEC = builder.build();
   }
}
