package net.shadowmage.ancientwarfare.core.common.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class AWCoreRegistryAccess {
   private AWCoreRegistryAccess() {
   }

   public static Item item(ResourceLocation id) {
      return BuiltInRegistries.ITEM.containsKey(id) ? (Item)BuiltInRegistries.ITEM.get(id) : Items.AIR;
   }

   public static Block block(ResourceLocation id) {
      return BuiltInRegistries.BLOCK.containsKey(id) ? (Block)BuiltInRegistries.BLOCK.get(id) : Blocks.AIR;
   }

   public static <T extends AbstractContainerMenu> MenuType<T> menu(ResourceLocation id) {
      return (MenuType<T>)BuiltInRegistries.MENU.get(id);
   }

   public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(ResourceLocation id) {
      return (BlockEntityType<T>)BuiltInRegistries.BLOCK_ENTITY_TYPE.get(id);
   }

   public static <T extends Entity> EntityType<T> entityType(ResourceLocation id) {
      return (EntityType<T>)BuiltInRegistries.ENTITY_TYPE.get(id);
   }

   public static ItemStack stack(ResourceLocation id) {
      return stack(id, 1);
   }

   public static ItemStack stack(ResourceLocation id, int count) {
      Item item = item(id);
      return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
   }

   public static boolean is(ItemStack stack, ResourceLocation id) {
      return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id);
   }

   public static <T extends RecipeType<?>> T recipeType(ResourceLocation id) {
      return (T)BuiltInRegistries.RECIPE_TYPE.get(id);
   }

   public static <T extends RecipeSerializer<?>> T recipeSerializer(ResourceLocation id) {
      return (T)BuiltInRegistries.RECIPE_SERIALIZER.get(id);
   }
}
