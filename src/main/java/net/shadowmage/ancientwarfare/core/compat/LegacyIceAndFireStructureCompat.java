package net.shadowmage.ancientwarfare.core.compat;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class LegacyIceAndFireStructureCompat {
   private static final String MOD_ID = "iceandfire";
   private static final Map<String, String> ENTITY_ALIASES = Map.of(
      "firedragon", "fire_dragon", "icedragon", "ice_dragon", "if_hydra", "hydra", "if_troll", "troll", "if_mob_skull", "mob_skull"
   );
   private static final Map<String, String> BLOCK_ALIASES = Map.of("goldpile", "gold_pile");
   private static final Map<String, String> VANILLA_BLOCK_FALLBACKS = Map.ofEntries(
      Map.entry("ash", "minecraft:gray_concrete_powder"),
      Map.entry("chared_cobblestone", "minecraft:blackstone"),
      Map.entry("chared_dirt", "minecraft:coarse_dirt"),
      Map.entry("chared_grass", "minecraft:podzol"),
      Map.entry("chared_gravel", "minecraft:gravel"),
      Map.entry("chared_stone", "minecraft:basalt"),
      Map.entry("dragon_bone_block", "minecraft:bone_block"),
      Map.entry("dragonforge_ice_brick", "minecraft:packed_ice"),
      Map.entry("dragonforge_ice_input", "minecraft:blue_ice"),
      Map.entry("dread_spawner", "minecraft:chiseled_deepslate"),
      Map.entry("dread_stone", "minecraft:deepslate"),
      Map.entry("dread_stone_bricks", "minecraft:deepslate_bricks"),
      Map.entry("dread_stone_bricks_chiseled", "minecraft:chiseled_deepslate"),
      Map.entry("dread_stone_bricks_cracked", "minecraft:cracked_deepslate_bricks"),
      Map.entry("dread_stone_bricks_mossy", "minecraft:mossy_stone_bricks"),
      Map.entry("dread_stone_face", "minecraft:chiseled_deepslate"),
      Map.entry("dread_stone_slab", "minecraft:deepslate_brick_slab"),
      Map.entry("dread_stone_stairs", "minecraft:deepslate_brick_stairs"),
      Map.entry("dread_stone_tile", "minecraft:deepslate_tiles"),
      Map.entry("dread_torch", "minecraft:soul_torch"),
      Map.entry("dreadwood_planks", "minecraft:dark_oak_planks"),
      Map.entry("dreadwood_planks_lock", "minecraft:chiseled_deepslate"),
      Map.entry("frozen_stone", "minecraft:packed_ice"),
      Map.entry("goldpile", "minecraft:gold_block"),
      Map.entry("nest", "minecraft:hay_block"),
      Map.entry("silver_ore", "minecraft:iron_ore")
   );

   private LegacyIceAndFireStructureCompat() {
   }

   public static ResourceLocation remapEntity(ResourceLocation legacyId) {
      if (legacyId != null && "iceandfire".equals(legacyId.m_135827_())) {
         String path = ENTITY_ALIASES.getOrDefault(legacyId.m_135815_(), legacyId.m_135815_());
         return new ResourceLocation("iceandfire", path);
      } else {
         return legacyId;
      }
   }

   public static String remapBlock(String legacyName) {
      ResourceLocation legacyId = ResourceLocation.m_135820_(legacyName);
      if (legacyId != null && "iceandfire".equals(legacyId.m_135827_())) {
         String aliasPath = BLOCK_ALIASES.getOrDefault(legacyId.m_135815_(), legacyId.m_135815_());
         ResourceLocation iceAndFireId = new ResourceLocation("iceandfire", aliasPath);
         return BuiltInRegistries.f_256975_.m_7804_(iceAndFireId)
            ? iceAndFireId.toString()
            : VANILLA_BLOCK_FALLBACKS.getOrDefault(legacyId.m_135815_(), "minecraft:air");
      } else {
         return legacyName;
      }
   }
}
