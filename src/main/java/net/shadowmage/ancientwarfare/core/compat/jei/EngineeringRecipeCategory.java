package net.shadowmage.ancientwarfare.core.compat.jei;

import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.shadowmage.ancientwarfare.core.common.engineering.EngineeringIngredient;
import net.shadowmage.ancientwarfare.core.common.engineering.EngineeringRecipe;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.research.ResearchGoal;
import net.shadowmage.ancientwarfare.core.research.ResearchRegistry;

public final class EngineeringRecipeCategory implements IRecipeCategory<EngineeringRecipe> {
   public static final RecipeType<EngineeringRecipe> TYPE = RecipeType.create("ancientwarfare", "engineering", EngineeringRecipe.class);
   private static final int WIDTH = 116;
   private static final int HEIGHT = 54;
   private final IDrawable icon;
   private final IDrawable slot;
   private final IDrawable arrow;

   public EngineeringRecipeCategory(IGuiHelper guiHelper) {
      this.icon = guiHelper.createDrawableItemStack(AWCoreRegistryAccess.stack(AWCoreIds.RESEARCH_BOOK));
      this.slot = guiHelper.getSlotDrawable();
      this.arrow = guiHelper.getRecipeArrow();
   }

   public RecipeType<EngineeringRecipe> getRecipeType() {
      return TYPE;
   }

   public Component getTitle() {
      return Component.translatable("jei.ancientwarfare.category.engineering");
   }

   public int getWidth() {
      return 116;
   }

   public int getHeight() {
      return 54;
   }

   public IDrawable getIcon() {
      return this.icon;
   }

   public void setRecipe(IRecipeLayoutBuilder builder, EngineeringRecipe recipe, IFocusGroup focuses) {
      EngineeringIngredient[] ingredients = recipe.ingredients();

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 3; col++) {
            EngineeringIngredient ingredient = ingredients[row * 3 + col];
            IRecipeSlotBuilder recipeSlot = builder.addInputSlot(col * 18 + 1, row * 18 + 1);
            if (!ingredient.isEmpty()) {
               recipeSlot.addItemStacks(List.of(ingredient.displayStacks()));
            }
         }
      }

      builder.addOutputSlot(95, 19).addItemStack(recipe.result().copy());
   }

   public void draw(EngineeringRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 3; col++) {
            this.slot.draw(graphics, col * 18, row * 18);
         }
      }

      this.slot.draw(graphics, 94, 18);
      this.arrow.draw(graphics, 61, (54 - this.arrow.getHeight()) / 2);
      String research = researchName(recipe.requiredResearch());
      graphics.drawString(Minecraft.getInstance().font, research, 60, 0, 4473924, false);
   }

   private static String researchName(String researchId) {
      if (researchId != null && !researchId.isBlank()) {
         ResearchGoal goal = ResearchRegistry.get(researchId);
         return goal == null ? researchId : goal.translatedName().getString();
      } else {
         return Component.translatable("jei.ancientwarfare.no_research").getString();
      }
   }
}
