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
      return new EngineeringIngredient(Ingredient.EMPTY, 1, null);
   }

   public boolean isEmpty() {
      return this.ingredient.isEmpty();
   }

   public boolean test(ItemStack stack) {
      return !stack.isEmpty()
         && stack.getCount() >= this.count
         && this.ingredient.test(stack)
         && (this.requiredTag == null || this.specialTagMatch(stack) || NbtUtils.compareNbt(this.requiredTag, stack.getTag(), true));
   }

   private boolean specialTagMatch(ItemStack stack) {
      if (this.requiredTag != null && this.requiredTag.contains("StoredEnchantments")) {
         ListTag required = this.requiredTag.getList("StoredEnchantments", 10);
         if (required.isEmpty()) {
            return false;
         } else {
            CompoundTag enchantment = required.getCompound(0);
            ResourceLocation id = ResourceLocation.tryParse(enchantment.getString("id"));
            if (id == null) {
               return false;
            } else {
               Enchantment requiredEnchantment = (Enchantment)BuiltInRegistries.ENCHANTMENT.get(id);
               return requiredEnchantment == null
                  ? false
                  : EnchantmentHelper.getEnchantments(stack).getOrDefault(requiredEnchantment, 0) >= enchantment.getShort("lvl");
            }
         }
      } else {
         return false;
      }
   }

   public ItemStack[] displayStacks() {
      ItemStack[] stacks = this.ingredient.getItems();
      ItemStack[] result = new ItemStack[stacks.length];

      for (int index = 0; index < stacks.length; index++) {
         result[index] = stacks[index].copyWithCount(this.count);
         if (this.requiredTag != null) {
            result[index].getOrCreateTag().merge(this.requiredTag.copy());
         }
      }

      return result;
   }

   public void toNetwork(FriendlyByteBuf buffer) {
      this.ingredient.toNetwork(buffer);
      buffer.writeVarInt(this.count);
      buffer.writeNbt(this.requiredTag);
   }

   public static EngineeringIngredient fromNetwork(FriendlyByteBuf buffer) {
      return new EngineeringIngredient(Ingredient.fromNetwork(buffer), buffer.readVarInt(), buffer.readNbt());
   }
}
