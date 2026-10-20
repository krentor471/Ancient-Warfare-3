package net.shadowmage.ancientwarfare.core.compat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;

public final class SlashBladeStructureLootCompat {
   private static final String MOD_ID = "slashblade";
   private static final String PENDING_TAG = "AWSlashBladeLootPending";
   private static final String SEED_TAG = "AWSlashBladeLootSeed";
   private static final List<String> ENCHANTMENTS = List.of(
      "sharpness",
      "smite",
      "bane_of_arthropods",
      "knockback",
      "fire_aspect",
      "looting",
      "sweeping",
      "unbreaking",
      "power",
      "thorns",
      "soul_speed",
      "feather_falling",
      "fire_protection"
   );

   private SlashBladeStructureLootCompat() {
   }

   public static void configure(ChestBlockEntity chest, long seed) {
      if (loaded()) {
         CompoundTag data = AWCorePlatform.blockEntityData(chest);
         data.putBoolean("AWSlashBladeLootPending", true);
         data.putLong("AWSlashBladeLootSeed", seed);
         chest.setChanged();
      }
   }

   public static boolean hasPendingLoot(ChestBlockEntity chest) {
      return AWCorePlatform.blockEntityData(chest).getBoolean("AWSlashBladeLootPending");
   }

   public static List<ItemStack> consumeChestLoot(ChestBlockEntity chest) {
      CompoundTag data = AWCorePlatform.blockEntityData(chest);
      if (!data.getBoolean("AWSlashBladeLootPending")) {
         return List.of();
      } else {
         long seed = data.getLong("AWSlashBladeLootSeed");
         data.remove("AWSlashBladeLootPending");
         data.remove("AWSlashBladeLootSeed");
         chest.setChanged();
         return chest.getLevel() instanceof ServerLevel serverLevel ? roll(serverLevel, seed) : List.of();
      }
   }

   public static List<ItemStack> roll(ServerLevel level, long seed) {
      if (!loaded()) {
         return List.of();
      } else {
         RandomSource random = RandomSource.create(seed == 0L ? level.random.nextLong() : seed);
         Item tiny = item("proudsoul_tiny");
         Item fragment = item("proudsoul");
         Item rewardItem = random.nextFloat() < 0.7F ? tiny : fragment;
         if (rewardItem == null) {
            rewardItem = tiny != null ? tiny : fragment;
         }

         if (rewardItem == null) {
            return List.of();
         } else {
            List<Enchantment> available = new ArrayList<>();

            for (String path : ENCHANTMENTS) {
               ResourceLocation id = new ResourceLocation("minecraft", path);
               if (BuiltInRegistries.ENCHANTMENT.containsKey(id)) {
                  available.add((Enchantment)BuiltInRegistries.ENCHANTMENT.get(id));
               }
            }

            if (available.isEmpty()) {
               return List.of();
            } else {
               ItemStack reward = new ItemStack(rewardItem);
               reward.enchant(available.get(random.nextInt(available.size())), 1);
               return List.of(reward);
            }
         }
      }
   }

   private static Item item(String path) {
      ResourceLocation id = new ResourceLocation("slashblade", path);
      return BuiltInRegistries.ITEM.containsKey(id) ? (Item)BuiltInRegistries.ITEM.get(id) : null;
   }

   private static boolean loaded() {
      return AWCorePlatform.isModLoaded("slashblade");
   }
}
