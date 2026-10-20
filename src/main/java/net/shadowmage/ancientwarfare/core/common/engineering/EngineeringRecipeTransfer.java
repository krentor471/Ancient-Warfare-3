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
      if (recipe != null && recipe.ingredients().length == 9 && researchUnlocked(target.m_8020_(bookSlot), recipe)) {
         if (sourceStarts.length != sourceCounts.length) {
            return false;
         } else {
            List<EngineeringRecipeTransfer.Source> sources = new ArrayList<>();

            for (int group = 0; group < sourceStarts.length; group++) {
               for (int i = 0; i < sourceCounts[group]; i++) {
                  int slot = sourceStarts[group] + i;
                  sources.add(new EngineeringRecipeTransfer.Source(target, slot, target.m_8020_(slot).m_41777_()));
               }
            }

            ItemStack[] playerStacks = new ItemStack[36];

            for (int i = 0; i < playerStacks.length; i++) {
               playerStacks[i] = playerInventory.m_8020_(i).m_41777_();
            }

            ItemStack[] targets = new ItemStack[9];

            for (int i = 0; i < targets.length; i++) {
               targets[i] = target.m_8020_(targetStart + i).m_41777_();
            }

            EngineeringIngredient[] arranged = arrangeForTransfer(recipe, targets, sources, playerStacks);
            if (arranged == null) {
               return false;
            } else {
               for (int i = 0; i < 9; i++) {
                  EngineeringIngredient ingredient = arranged[i];
                  ItemStack existing = targets[i];
                  if (ingredient.isEmpty()) {
                     if (!existing.m_41619_() && !insert(sources, playerStacks, existing)) {
                        return false;
                     }

                     targets[i] = ItemStack.f_41583_;
                  } else if (ingredient.test(existing)) {
                     if (existing.m_41613_() > ingredient.count()) {
                        ItemStack extra = existing.m_255036_(existing.m_41613_() - ingredient.count());
                        if (!insert(sources, playerStacks, extra)) {
                           return false;
                        }

                        targets[i].m_41764_(ingredient.count());
                     }
                  } else {
                     if (!existing.m_41619_() && !insert(sources, playerStacks, existing)) {
                        return false;
                     }

                     ItemStack picked = take(ingredient, sources, playerStacks);
                     if (picked.m_41619_()) {
                        return false;
                     }

                     targets[i] = picked;
                  }
               }

               for (EngineeringRecipeTransfer.Source source : sources) {
                  source.container.m_6836_(source.slot, source.stack);
               }

               for (int ix = 0; ix < 36; ix++) {
                  playerInventory.m_6836_(ix, playerStacks[ix]);
               }

               playerInventory.m_6596_();

               for (int ix = 0; ix < 9; ix++) {
                  target.m_6836_(targetStart + ix, targets[ix]);
               }

               target.m_6596_();
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
            if (!targets[slot].m_41619_()) {
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
         || !book.m_41619_() && EngineeringRecipeLogic.hasCompletedResearch(ResearchBookData.getCompletedResearch(book), recipe.requiredResearch());
   }

   private static ItemStack take(EngineeringIngredient ingredient, List<EngineeringRecipeTransfer.Source> sources, ItemStack[] playerStacks) {
      for (EngineeringRecipeTransfer.Source source : sources) {
         if (!source.stack.m_41619_() && ingredient.test(source.stack)) {
            ItemStack picked = source.stack.m_255036_(ingredient.count());
            source.stack.m_41774_(ingredient.count());
            return picked;
         }
      }

      for (int i = 0; i < playerStacks.length; i++) {
         if (!playerStacks[i].m_41619_() && ingredient.test(playerStacks[i])) {
            ItemStack picked = playerStacks[i].m_255036_(ingredient.count());
            playerStacks[i].m_41774_(ingredient.count());
            return picked;
         }
      }

      return ItemStack.f_41583_;
   }

   private static boolean insert(List<EngineeringRecipeTransfer.Source> sources, ItemStack[] inventory, ItemStack stack) {
      ItemStack remaining = stack.m_41777_();

      for (EngineeringRecipeTransfer.Source source : sources) {
         ItemStack current = source.stack;
         if (!current.m_41619_() && ItemStack.m_150942_(current, remaining)) {
            int moved = Math.min(remaining.m_41613_(), current.m_41741_() - current.m_41613_());
            current.m_41769_(moved);
            remaining.m_41774_(moved);
            if (remaining.m_41619_()) {
               return true;
            }
         }
      }

      for (EngineeringRecipeTransfer.Source sourcex : sources) {
         if (sourcex.stack.m_41619_()) {
            int moved = Math.min(remaining.m_41613_(), remaining.m_41741_());
            sourcex.stack = remaining.m_255036_(moved);
            remaining.m_41774_(moved);
            if (remaining.m_41619_()) {
               return true;
            }
         }
      }

      for (ItemStack current : inventory) {
         if (!current.m_41619_() && ItemStack.m_150942_(current, remaining)) {
            int moved = Math.min(remaining.m_41613_(), current.m_41741_() - current.m_41613_());
            current.m_41769_(moved);
            remaining.m_41774_(moved);
            if (remaining.m_41619_()) {
               return true;
            }
         }
      }

      for (int i = 0; i < inventory.length; i++) {
         if (inventory[i].m_41619_()) {
            int moved = Math.min(remaining.m_41613_(), remaining.m_41741_());
            inventory[i] = remaining.m_255036_(moved);
            remaining.m_41774_(moved);
            if (remaining.m_41619_()) {
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
