package net.shadowmage.ancientwarfare.core.common.engineering;

import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public record EngineeringIngredient(Ingredient ingredient, int count, @Nullable CompoundTag requiredTag) {
   public EngineeringIngredient(Ingredient ingredient, int count, @Nullable CompoundTag requiredTag) {
      if (count < 1) {
         throw new IllegalArgumentException("Engineering ingredient count must be positive");
      } else {
         this.ingredient = ingredient;
         this.count = count;
         this.requiredTag = requiredTag;
      }
   }

   public static EngineeringIngredient empty() {
      return new EngineeringIngredient(Ingredient.f_43901_, 1, null);
   }

   public boolean isEmpty() {
      return this.ingredient.m_43947_();
   }

   public boolean test(ItemStack stack) {
      return !stack.m_41619_()
         && stack.m_41613_() >= this.count
         && this.ingredient.test(stack)
         && (this.requiredTag == null || this.specialTagMatch(stack) || NbtUtils.m_129235_(this.requiredTag, stack.m_41783_(), true));
   }

   private boolean specialTagMatch(ItemStack stack) {
      if (this.requiredTag != null && this.requiredTag.m_128441_("StoredEnchantments")) {
         ListTag required = this.requiredTag.m_128437_("StoredEnchantments", 10);
         if (required.isEmpty()) {
            return false;
         } else {
            CompoundTag enchantment = required.m_128728_(0);
            ResourceLocation id = ResourceLocation.m_135820_(enchantment.m_128461_("id"));
            if (id == null) {
               return false;
            } else {
               Enchantment requiredEnchantment = (Enchantment)BuiltInRegistries.f_256876_.m_7745_(id);
               return requiredEnchantment == null
                  ? false
                  : EnchantmentHelper.m_44831_(stack).getOrDefault(requiredEnchantment, 0) >= enchantment.m_128448_("lvl");
            }
         }
      } else {
         return false;
      }
   }

   public ItemStack[] displayStacks() {
      ItemStack[] stacks = this.ingredient.m_43908_();
      ItemStack[] result = new ItemStack[stacks.length];

      for (int index = 0; index < stacks.length; index++) {
         result[index] = stacks[index].m_255036_(this.count);
         if (this.requiredTag != null) {
            result[index].m_41784_().m_128391_(this.requiredTag.m_6426_());
         }
      }

      return result;
   }

   public void toNetwork(FriendlyByteBuf buffer) {
      this.ingredient.m_43923_(buffer);
      buffer.m_130130_(this.count);
      buffer.m_130079_(this.requiredTag);
   }

   public static EngineeringIngredient fromNetwork(FriendlyByteBuf buffer) {
      return new EngineeringIngredient(Ingredient.m_43940_(buffer), buffer.m_130242_(), buffer.m_130260_());
   }
}
