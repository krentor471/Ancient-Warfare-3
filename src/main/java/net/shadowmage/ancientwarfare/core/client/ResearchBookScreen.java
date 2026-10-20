package net.shadowmage.ancientwarfare.core.client;

import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.engineering.EngineeringRecipe;
import net.shadowmage.ancientwarfare.core.common.engineering.EngineeringRecipeRegistry;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.item.ResearchBookItem;
import net.shadowmage.ancientwarfare.core.menu.ResearchBookMenu;
import net.shadowmage.ancientwarfare.core.research.ResearchGoal;
import net.shadowmage.ancientwarfare.core.research.ResearchRegistry;

public class ResearchBookScreen extends AbstractContainerScreen<ResearchBookMenu> {
   private static final ResourceLocation BACKGROUND = new ResourceLocation("ancientwarfare", "textures/gui/guibackgroundlarge.png");
   private static final ResourceLocation WIDGETS = new ResourceLocation("ancientwarfare", "textures/gui/guibuttons1.png");
   private static final int TEXT_COLOR = 16777215;
   private static final int LEFT_PANEL_X = 0;
   private static final int RIGHT_PANEL_X = 200;
   private static final int PANEL_Y = 40;
   private static final int PANEL_WIDTH = 200;
   private static final int PANEL_HEIGHT = 200;
   private int selectedIndex = 0;
   private int scrollOffset = 0;
   private int pressedButton = -1;

   public ResearchBookScreen(ResearchBookMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.imageWidth = 400;
      this.imageHeight = 240;
      this.titleLabelX = 8;
      this.titleLabelY = 8;
   }

   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      this.drawBackground(graphics);
      this.drawPanelSeparator(graphics);
      this.drawResearchList(graphics);
      this.drawDetails(graphics);
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.drawString(this.font, Component.translatable("guistrings.research.status.completed"), 8, 12, 16777215, false);
      String researcherName = ((ResearchBookMenu)this.menu).getResearcherName();
      graphics.drawString(
         this.font,
         researcherName.isEmpty() ? Component.translatable("guistrings.research.no_researcher") : Component.literal(researcherName),
         220,
         12,
         16777215,
         false
      );
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.renderTooltip(graphics, mouseX, mouseY);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0) {
         int localX = (int)mouseX - this.leftPos;
         int localY = (int)mouseY - this.topPos;
         int listIndex = this.getGoalButtonAt(localX, localY);
         if (listIndex >= 0) {
            this.selectedIndex = listIndex;
            this.pressedButton = listIndex;
            this.playClick();
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      int localX = (int)mouseX - this.leftPos;
      int localY = (int)mouseY - this.topPos;
      if (this.inside(localX, localY, 0, 40, 200, 200)) {
         List<ResearchGoal> goals = this.completedGoals();
         int maxOffset = Math.max(0, goals.size() - 13);
         if (maxOffset > 0 && delta != 0.0) {
            this.scrollOffset = (int)Math.max(0.0, Math.min((double)maxOffset, (double)this.scrollOffset - Math.signum(delta)));
            this.selectedIndex = Math.max(0, Math.min(this.selectedIndex, goals.size() - 1));
            return true;
         }
      }

      return super.mouseScrolled(mouseX, mouseY, delta);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.pressedButton = -1;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   private void drawBackground(GuiGraphics graphics) {
      this.drawNineSlice(graphics, BACKGROUND, 0, 0, this.imageWidth, this.imageHeight, 0, 0, 256, 240);
   }

   private void drawPanelSeparator(GuiGraphics graphics) {
      graphics.fill(this.leftPos, this.topPos + 40 - 2, this.leftPos + this.imageWidth, this.topPos + 40, -16777216);
      graphics.fill(this.leftPos + 200 - 1, this.topPos + 40, this.leftPos + 200 + 1, this.topPos + this.imageHeight, -16777216);
   }

   private void drawResearchList(GuiGraphics graphics) {
      List<ResearchGoal> goals = this.completedGoals();
      if (goals.isEmpty()) {
         this.drawTexturedButton(graphics, 8, 48, 160, 12, Component.translatable("guistrings.research.no_completed"), false);
      } else {
         this.selectedIndex = Math.max(0, Math.min(this.selectedIndex, goals.size() - 1));
         int visible = Math.min(goals.size() - this.scrollOffset, 13);

         for (int i = 0; i < visible; i++) {
            ResearchGoal goal = goals.get(i + this.scrollOffset);
            int y = 48 + i * 14;
            boolean selected = i + this.scrollOffset == this.selectedIndex;
            this.drawTexturedButton(graphics, 8, y, 160, 12, goal.translatedName(), selected || this.pressedButton == i + this.scrollOffset);
         }
      }
   }

   private void drawDetails(GuiGraphics graphics) {
      List<ResearchGoal> goals = this.completedGoals();
      if (goals.isEmpty()) {
         this.drawTexturedButton(graphics, 208, 48, 160, 12, Component.translatable("guistrings.research.no_completed"), false);
      } else {
         ResearchGoal goal = goals.get(Math.max(0, Math.min(this.selectedIndex, goals.size() - 1)));
         ItemStack book = ((ResearchBookMenu)this.menu).getResearchBook();
         boolean completed = ResearchBookItem.getCompletedResearch(book).contains(goal.id());
         boolean queued = ResearchBookItem.getQueuedResearch(book).contains(goal.id());
         String current = ResearchBookItem.getCurrentResearch(book);
         boolean currentGoal = goal.id().equals(current);
         int x = 208;
         int y = 48;
         this.drawTexturedButton(graphics, x, y, 160, 12, goal.translatedName(), true);
         y += 24;
         String statusKey = completed
            ? "completed"
            : (currentGoal ? "in_progress" : (queued ? "queued" : (goal.canResearch(ResearchBookItem.getCompletedResearch(book)) ? "available" : "locked")));
         graphics.drawString(
            this.font,
            Component.translatable("guistrings.research.status", new Object[]{Component.translatable("guistrings.research.status." + statusKey)}),
            this.leftPos + x,
            this.topPos + y,
            16777215,
            false
         );
         y += 14;
         if (currentGoal) {
            graphics.drawString(
               this.font,
               Component.translatable("guistrings.research.progress_value", new Object[]{ResearchBookItem.getCurrentProgress(book), goal.time()}),
               this.leftPos + x,
               this.topPos + y,
               16777215,
               false
            );
            y += 14;
         }

         if (!goal.dependencies().isEmpty()) {
            graphics.drawString(this.font, Component.translatable("guistrings.research.requires_header"), this.leftPos + x, this.topPos + y, 16777215, false);
            y += 12;

            for (String dep : goal.dependencies()) {
               ResearchGoal dependency = ResearchRegistry.get(dep);
               graphics.drawString(
                  this.font,
                  (Component)(dependency == null ? Component.literal(dep) : dependency.translatedName()),
                  this.leftPos + x + 8,
                  this.topPos + y,
                  16777215,
                  false
               );
               y += 12;
            }
         }

         graphics.drawString(this.font, Component.translatable("guistrings.research.resources_header"), this.leftPos + x, this.topPos + y, 16777215, false);
         y += 14;
         List<ItemStack> resources = goal.resources();

         for (int i = 0; i < resources.size() && i < 9; i++) {
            int slotX = x + i % 3 * 18;
            int slotY = y + i / 3 * 18;
            this.drawDecorativeSlot(graphics, slotX, slotY);
            graphics.renderItem(resources.get(i), this.leftPos + slotX, this.topPos + slotY);
            graphics.renderItemDecorations(this.font, resources.get(i), this.leftPos + slotX, this.topPos + slotY);
         }

         y += (Math.min(resources.size(), 9) + 2) / 3 * 18 + 10;
         this.drawUnlocks(graphics, goal, x, y);
      }
   }

   private void drawUnlocks(GuiGraphics graphics, ResearchGoal goal, int x, int y) {
      List<EngineeringRecipe> unlocks = this.unlocksForResearch(goal.id());
      if (!unlocks.isEmpty() && y <= 188) {
         graphics.drawString(this.font, Component.translatable("guistrings.research.unlocks"), this.leftPos + x, this.topPos + y, 16777215, false);
         y += 14;
         int max = Math.min(unlocks.size(), 8);

         for (int i = 0; i < max; i++) {
            ItemStack result = unlocks.get(i).result();
            int slotX = x + i % 4 * 18;
            int slotY = y + i / 4 * 18;
            this.drawDecorativeSlot(graphics, slotX, slotY);
            graphics.renderItem(result, this.leftPos + slotX, this.topPos + slotY);
            graphics.renderItemDecorations(this.font, result, this.leftPos + slotX, this.topPos + slotY);
         }

         if (unlocks.size() > max) {
            graphics.drawString(
               this.font,
               Component.translatable("guistrings.research.more", new Object[]{unlocks.size() - max}),
               this.leftPos + x + 78,
               this.topPos + y + 22,
               16777215,
               false
            );
         }
      }
   }

   private List<EngineeringRecipe> unlocksForResearch(String researchId) {
      return this.minecraft != null && this.minecraft.level != null
         ? this.minecraft
            .level
            .getRecipeManager()
            .getAllRecipesFor(AWCoreRegistryAccess.recipeType(AWCoreIds.id("engineering")))
            .stream()
            .filter(recipe -> researchId.equals(recipe.requiredResearch()))
            .toList()
         : EngineeringRecipeRegistry.recipesForResearch(researchId);
   }

   private int getGoalButtonAt(int x, int y) {
      List<ResearchGoal> goals = this.completedGoals();

      for (int i = 0; i < goals.size() - this.scrollOffset && i < 13; i++) {
         if (this.inside(x, y, 8, 48 + i * 14, 160, 12)) {
            return i + this.scrollOffset;
         }
      }

      return -1;
   }

   private List<ResearchGoal> completedGoals() {
      ItemStack book = ((ResearchBookMenu)this.menu).getResearchBook();
      if (book.isEmpty()) {
         return List.of();
      } else {
         Set<String> completed = ResearchBookItem.getCompletedResearch(book);
         return ResearchRegistry.all().stream().filter(goal -> completed.contains(goal.id())).toList();
      }
   }

   private boolean inside(int x, int y, int left, int top, int width, int height) {
      return x >= left && y >= top && x < left + width && y < top + height;
   }

   private void playClick() {
      this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   private void drawTexturedButton(GuiGraphics graphics, int x, int y, int width, int height, Component label, boolean pressed) {
      this.drawNineSlice(graphics, WIDGETS, x, y, width, height, 0, pressed ? 80 : 40, 256, 40);
      int textWidth = this.font.width(label);
      graphics.drawString(this.font, label, this.leftPos + x + (width - textWidth) / 2, this.topPos + y + (height - 8) / 2, 16777215, false);
   }

   private void drawDecorativeSlot(GuiGraphics graphics, int x, int y) {
      graphics.blit(WIDGETS, this.leftPos + x - 1, this.topPos + y - 1, 152, 120, 18, 18);
   }

   private void drawNineSlice(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int u, int v, int texWidth, int texHeight) {
      int halfWidth = width / 2;
      int halfHeight = height / 2;
      graphics.blit(texture, this.leftPos + x, this.topPos + y, u, v, halfWidth, halfHeight);
      graphics.blit(texture, this.leftPos + x + halfWidth, this.topPos + y, u + texWidth - (width - halfWidth), v, width - halfWidth, halfHeight);
      graphics.blit(texture, this.leftPos + x, this.topPos + y + halfHeight, u, v + texHeight - (height - halfHeight), halfWidth, height - halfHeight);
      graphics.blit(
         texture,
         this.leftPos + x + halfWidth,
         this.topPos + y + halfHeight,
         u + texWidth - (width - halfWidth),
         v + texHeight - (height - halfHeight),
         width - halfWidth,
         height - halfHeight
      );
   }
}
