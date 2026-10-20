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
      return BuiltInRegistries.f_257033_.m_7804_(id) ? (Item)BuiltInRegistries.f_257033_.m_7745_(id) : Items.f_41852_;
   }

   public static Block block(ResourceLocation id) {
      return BuiltInRegistries.f_256975_.m_7804_(id) ? (Block)BuiltInRegistries.f_256975_.m_7745_(id) : Blocks.f_50016_;
   }

   public static <T extends AbstractContainerMenu> MenuType<T> menu(ResourceLocation id) {
      return (MenuType<T>)BuiltInRegistries.f_256818_.m_7745_(id);
   }

   public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(ResourceLocation id) {
      return (BlockEntityType<T>)BuiltInRegistries.f_257049_.m_7745_(id);
   }

   public static <T extends Entity> EntityType<T> entityType(ResourceLocation id) {
      return (EntityType<T>)BuiltInRegistries.f_256780_.m_7745_(id);
   }

   public static ItemStack stack(ResourceLocation id) {
      return stack(id, 1);
   }

   public static ItemStack stack(ResourceLocation id, int count) {
      Item item = item(id);
      return item == Items.f_41852_ ? ItemStack.f_41583_ : new ItemStack(item, count);
   }

   public static boolean is(ItemStack stack, ResourceLocation id) {
      return !stack.m_41619_() && BuiltInRegistries.f_257033_.m_7981_(stack.m_41720_()).equals(id);
   }

   public static <T extends RecipeType<?>> T recipeType(ResourceLocation id) {
      return (T)BuiltInRegistries.f_256990_.m_7745_(id);
   }

   public static <T extends RecipeSerializer<?>> T recipeSerializer(ResourceLocation id) {
      return (T)BuiltInRegistries.f_256769_.m_7745_(id);
   }
}
