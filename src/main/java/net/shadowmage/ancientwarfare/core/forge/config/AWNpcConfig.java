package net.shadowmage.ancientwarfare.core.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;

public final class AWNpcConfig {
   public static final ForgeConfigSpec SPEC;
   private static final BooleanValue CUSTOM_SOLDIER_DROPS;
   private static final DoubleValue SOLDIER_DROP_CHANCE;

   private AWNpcConfig() {
   }

   public static double playerSoldierEquipmentDropChance() {
      return CUSTOM_SOLDIER_DROPS.get() ? (Double)SOLDIER_DROP_CHANCE.get() : -1.0;
   }

   static {
      Builder builder = new Builder();
      builder.comment("NPC settings. In multiplayer the server's settings apply.").push("player_soldiers");
      CUSTOM_SOLDIER_DROPS = builder.comment(
            new String[]{
               "Override vanilla equipment drops for player-owned combat NPCs only.",
               "False preserves vanilla drops. Faction NPCs, workers and traders are unaffected."
            }
         )
         .define("customEquipmentDrops", false);
      SOLDIER_DROP_CHANCE = builder.comment(
            new String[]{
               "Independent drop probability for each equipped hand or armor stack when customEquipmentDrops is true.",
               "0 = none, 1 = all. No player kill required; Looting does not change this probability.",
               "Respects doMobLoot and Curse of Vanishing. Dropped equipment is not restored on resurrection.",
               "Food and orders are not affected. Armor rendered only as a faction skin is not an equipped item."
            }
         )
         .defineInRange("equipmentDropChance", 1.0, 0.0, 1.0);
      builder.pop();
      SPEC = builder.build();
   }
}
