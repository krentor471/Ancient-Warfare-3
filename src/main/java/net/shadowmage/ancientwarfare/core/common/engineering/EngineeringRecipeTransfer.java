package net.shadowmage.ancientwarfare.core.common.engineering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.research.ResearchBookData;

public final class EngineeringRecipeTransfer {
   private EngineeringRecipeTransfer() {
   }

   public static boolean fill(
      Container target, int targetStart, int bookSlot, Inventory playerInventory, EngineeringRecipe recipe, int[] sourceStarts, int[] sourceCounts
   ) {
      if (recipe != null && recipe.ingredients().length == 9 && researchUnlocked(target.getItem(bookSlot), recipe)) {
         if (sourceStarts.length != sourceCounts.length) {
            return false;
         } else {
            List<EngineeringRecipeTransfer.Source> sources = new ArrayList<>();

            for (int group = 0; group < sourceStarts.length; group++) {
               for (int i = 0; i < sourceCounts[group]; i++) {
                  int slot = sourceStarts[group] + i;
                  sources.add(new EngineeringRecipeTransfer.Source(target, slot, target.getItem(slot).copy()));
               }
            }

            ItemStack[] playerStacks = new ItemStack[36];

            for (int i = 0; i < playerStacks.length; i++) {
               playerStacks[i] = playerInventory.getItem(i).copy();
            }

            ItemStack[] targets = new ItemStack[9];

            for (int i = 0; i < targets.length; i++) {
               targets[i] = target.getItem(targetStart + i).copy();
            }

            EngineeringIngredient[] arranged = arrangeForTransfer(recipe, targets, sources, playerStacks);
            if (arranged == null) {
               return false;
            } else {
               for (int i = 0; i < 9; i++) {
                  EngineeringIngredient ingredient = arranged[i];
                  ItemStack existing = targets[i];
                  if (ingredient.isEmpty()) {
                     if (!existing.isEmpty() && !insert(sources, playerStacks, existing)) {
                        return false;
                     }

                     targets[i] = ItemStack.EMPTY;
                  } else if (ingredient.test(existing)) {
                     if (existing.getCount() > ingredient.count()) {
                        ItemStack extra = existing.copyWithCount(existing.getCount() - ingredient.count());
                        if (!insert(sources, playerStacks, extra)) {
                           return false;
                        }

                        targets[i].setCount(ingredient.count());
                     }
                  } else {
                     if (!existing.isEmpty() && !insert(sources, playerStacks, existing)) {
                        return false;
                     }

                     ItemStack picked = take(ingredient, sources, playerStacks);
                     if (picked.isEmpty()) {
                        return false;
                     }

                     targets[i] = picked;
                  }
               }

               for (EngineeringRecipeTransfer.Source source : sources) {
                  source.container.setItem(source.slot, source.stack);
               }

               for (int ix = 0; ix < 36; ix++) {
                  playerInventory.setItem(ix, playerStacks[ix]);
               }

               playerInventory.setChanged();

               for (int ix = 0; ix < 9; ix++) {
                  target.setItem(targetStart + ix, targets[ix]);
               }

               target.setChanged();
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private static EngineeringIngredient[] arrangeForTransfer(
      EngineeringRecipe recipe, ItemStack[] targets, List<EngineeringRecipeTransfer.Source> sources, ItemStack[] playerStacks
   ) {
      if (!recipe.shapeless()) {
         return recipe.ingredients();
      } else {
         List<EngineeringIngredient> required = Arrays.stream(recipe.ingredients()).filter(ingredientx -> !ingredientx.isEmpty()).toList();
         EngineeringIngredient[] arranged = new EngineeringIngredient[9];
         Arrays.fill(arranged, EngineeringIngredient.empty());
         boolean[] usedIngredients = new boolean[required.size()];

         for (int slot = 0; slot < targets.length; slot++) {
            if (!targets[slot].isEmpty()) {
               for (int ingredient = 0; ingredient < required.size(); ingredient++) {
                  if (!usedIngredients[ingredient] && required.get(ingredient).test(targets[slot])) {
                     arranged[slot] = required.get(ingredient);
                     usedIngredients[ingredient] = true;
                     break;
                  }
               }
            }
         }

         for (int ingredientx = 0; ingredientx < required.size(); ingredientx++) {
            if (!usedIngredients[ingredientx]) {
               EngineeringIngredient value = required.get(ingredientx);
               if (!available(value, sources, playerStacks)) {
                  return null;
               }

               int empty = firstEmpty(arranged);
               if (empty < 0) {
                  return null;
               }

               arranged[empty] = value;
            }
         }

         return arranged;
      }
   }

   private static boolean available(EngineeringIngredient ingredient, List<EngineeringRecipeTransfer.Source> sources, ItemStack[] playerStacks) {
      for (EngineeringRecipeTransfer.Source source : sources) {
         if (ingredient.test(source.stack)) {
            return true;
         }
      }

      for (ItemStack stack : playerStacks) {
         if (ingredient.test(stack)) {
            return true;
         }
      }

      return false;
   }

   private static int firstEmpty(EngineeringIngredient[] ingredients) {
      for (int index = 0; index < ingredients.length; index++) {
         if (ingredients[index].isEmpty()) {
            return index;
         }
      }

      return -1;
   }

   private static boolean researchUnlocked(ItemStack book, EngineeringRecipe recipe) {
      return recipe.requiredResearch() == null
         || !book.isEmpty() && EngineeringRecipeLogic.hasCompletedResearch(ResearchBookData.getCompletedResearch(book), recipe.requiredResearch());
   }

   private static ItemStack take(EngineeringIngredient ingredient, List<EngineeringRecipeTransfer.Source> sources, ItemStack[] playerStacks) {
      for (EngineeringRecipeTransfer.Source source : sources) {
         if (!source.stack.isEmpty() && ingredient.test(source.stack)) {
            ItemStack picked = source.stack.copyWithCount(ingredient.count());
            source.stack.shrink(ingredient.count());
            return picked;
         }
      }

      for (int i = 0; i < playerStacks.length; i++) {
         if (!playerStacks[i].isEmpty() && ingredient.test(playerStacks[i])) {
            ItemStack picked = playerStacks[i].copyWithCount(ingredient.count());
            playerStacks[i].shrink(ingredient.count());
            return picked;
         }
      }

      return ItemStack.EMPTY;
   }

   private static boolean insert(List<EngineeringRecipeTransfer.Source> sources, ItemStack[] inventory, ItemStack stack) {
      ItemStack remaining = stack.copy();

      for (EngineeringRecipeTransfer.Source source : sources) {
         ItemStack current = source.stack;
         if (!current.isEmpty() && ItemStack.isSameItemSameTags(current, remaining)) {
            int moved = Math.min(remaining.getCount(), current.getMaxStackSize() - current.getCount());
            current.grow(moved);
            remaining.shrink(moved);
            if (remaining.isEmpty()) {
               return true;
            }
         }
      }

      for (EngineeringRecipeTransfer.Source sourcex : sources) {
         if (sourcex.stack.isEmpty()) {
            int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            sourcex.stack = remaining.copyWithCount(moved);
            remaining.shrink(moved);
            if (remaining.isEmpty()) {
               return true;
            }
         }
      }

      for (ItemStack current : inventory) {
         if (!current.isEmpty() && ItemStack.isSameItemSameTags(current, remaining)) {
            int moved = Math.min(remaining.getCount(), current.getMaxStackSize() - current.getCount());
            current.grow(moved);
            remaining.shrink(moved);
            if (remaining.isEmpty()) {
               return true;
            }
         }
      }

      for (int i = 0; i < inventory.length; i++) {
         if (inventory[i].isEmpty()) {
            int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            inventory[i] = remaining.copyWithCount(moved);
            remaining.shrink(moved);
            if (remaining.isEmpty()) {
               return true;
            }
         }
      }

      return false;
   }

   private static final class Source {
      private final Container container;
      private final int slot;
      private ItemStack stack;

      private Source(Container container, int slot, ItemStack stack) {
         this.container = container;
         this.slot = slot;
         this.stack = stack;
      }
   }
}
