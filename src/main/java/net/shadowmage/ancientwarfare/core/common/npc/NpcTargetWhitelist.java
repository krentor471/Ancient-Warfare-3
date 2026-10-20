package net.shadowmage.ancientwarfare.core.common.npc;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public final class NpcTargetWhitelist {
   private static final AtomicReference<Set<ResourceLocation>> FRIENDLY = new AtomicReference<>(Set.of());

   private NpcTargetWhitelist() {
   }

   public static Set<ResourceLocation> snapshot() {
      return FRIENDLY.get();
   }

   public static void replace(Collection<? extends String> ids) {
      Set<ResourceLocation> parsed = new HashSet<>();
      if (ids != null) {
         for (String value : ids) {
            if (value != null) {
               ResourceLocation id = ResourceLocation.m_135820_(value.trim().toLowerCase(Locale.ROOT));
               if (id != null) {
                  parsed.add(id);
               }
            }
         }
      }

      FRIENDLY.set(Set.copyOf(parsed));
   }

   public static boolean contains(EntityType<?> type) {
      ResourceLocation id = EntityType.m_20613_(type);
      return id != null && FRIENDLY.get().contains(id);
   }
}
