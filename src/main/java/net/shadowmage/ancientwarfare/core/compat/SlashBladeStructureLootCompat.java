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
         data.m_128379_("AWSlashBladeLootPending", true);
         data.m_128356_("AWSlashBladeLootSeed", seed);
         chest.m_6596_();
      }
   }

   public static boolean hasPendingLoot(ChestBlockEntity chest) {
      return AWCorePlatform.blockEntityData(chest).m_128471_("AWSlashBladeLootPending");
   }

   public static List<ItemStack> consumeChestLoot(ChestBlockEntity chest) {
      CompoundTag data = AWCorePlatform.blockEntityData(chest);
      if (!data.m_128471_("AWSlashBladeLootPending")) {
         return List.of();
      } else {
         long seed = data.m_128454_("AWSlashBladeLootSeed");
         data.m_128473_("AWSlashBladeLootPending");
         data.m_128473_("AWSlashBladeLootSeed");
         chest.m_6596_();
         return chest.m_58904_() instanceof ServerLevel serverLevel ? roll(serverLevel, seed) : List.of();
      }
   }

   public static List<ItemStack> roll(ServerLevel level, long seed) {
      if (!loaded()) {
         return List.of();
      } else {
         RandomSource random = RandomSource.m_216335_(seed == 0L ? level.f_46441_.m_188505_() : seed);
         Item tiny = item("proudsoul_tiny");
         Item fragment = item("proudsoul");
         Item rewardItem = random.m_188501_() < 0.7F ? tiny : fragment;
         if (rewardItem == null) {
            rewardItem = tiny != null ? tiny : fragment;
         }

         if (rewardItem == null) {
            return List.of();
         } else {
            List<Enchantment> available = new ArrayList<>();

            for (String path : ENCHANTMENTS) {
               ResourceLocation id = new ResourceLocation("minecraft", path);
               if (BuiltInRegistries.f_256876_.m_7804_(id)) {
                  available.add((Enchantment)BuiltInRegistries.f_256876_.m_7745_(id));
               }
            }

            if (available.isEmpty()) {
               return List.of();
            } else {
               ItemStack reward = new ItemStack(rewardItem);
               reward.m_41663_(available.get(random.m_188503_(available.size())), 1);
               return List.of(reward);
            }
         }
      }
   }

   private static Item item(String path) {
      ResourceLocation id = new ResourceLocation("slashblade", path);
      return BuiltInRegistries.f_257033_.m_7804_(id) ? (Item)BuiltInRegistries.f_257033_.m_7745_(id) : null;
   }

   private static boolean loaded() {
      return AWCorePlatform.isModLoaded("slashblade");
   }
}
