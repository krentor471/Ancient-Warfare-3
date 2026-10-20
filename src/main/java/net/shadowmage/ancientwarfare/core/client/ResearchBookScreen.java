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
      this.f_97726_ = 400;
      this.f_97727_ = 240;
      this.f_97728_ = 8;
      this.f_97729_ = 8;
   }

   protected void m_7286_(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      this.drawBackground(graphics);
      this.drawPanelSeparator(graphics);
      this.drawResearchList(graphics);
      this.drawDetails(graphics);
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.research.status.completed"), 8, 12, 16777215, false);
      String researcherName = ((ResearchBookMenu)this.f_97732_).getResearcherName();
      graphics.m_280614_(
         this.f_96547_,
         researcherName.isEmpty() ? Component.m_237115_("guistrings.research.no_researcher") : Component.m_237113_(researcherName),
         220,
         12,
         16777215,
         false
      );
   }

   public void m_88315_(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(graphics);
      super.m_88315_(graphics, mouseX, mouseY, partialTick);
      this.m_280072_(graphics, mouseX, mouseY);
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (button == 0) {
         int localX = (int)mouseX - this.f_97735_;
         int localY = (int)mouseY - this.f_97736_;
         int listIndex = this.getGoalButtonAt(localX, localY);
         if (listIndex >= 0) {
            this.selectedIndex = listIndex;
            this.pressedButton = listIndex;
            this.playClick();
            return true;
         }
      }

      return super.m_6375_(mouseX, mouseY, button);
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      int localX = (int)mouseX - this.f_97735_;
      int localY = (int)mouseY - this.f_97736_;
      if (this.inside(localX, localY, 0, 40, 200, 200)) {
         List<ResearchGoal> goals = this.completedGoals();
         int maxOffset = Math.max(0, goals.size() - 13);
         if (maxOffset > 0 && delta != 0.0) {
            this.scrollOffset = (int)Math.max(0.0, Math.min((double)maxOffset, (double)this.scrollOffset - Math.signum(delta)));
            this.selectedIndex = Math.max(0, Math.min(this.selectedIndex, goals.size() - 1));
            return true;
         }
      }

      return super.m_6050_(mouseX, mouseY, delta);
   }

   public boolean m_6348_(double mouseX, double mouseY, int button) {
      this.pressedButton = -1;
      return super.m_6348_(mouseX, mouseY, button);
   }

   private void drawBackground(GuiGraphics graphics) {
      this.drawNineSlice(graphics, BACKGROUND, 0, 0, this.f_97726_, this.f_97727_, 0, 0, 256, 240);
   }

   private void drawPanelSeparator(GuiGraphics graphics) {
      graphics.m_280509_(this.f_97735_, this.f_97736_ + 40 - 2, this.f_97735_ + this.f_97726_, this.f_97736_ + 40, -16777216);
      graphics.m_280509_(this.f_97735_ + 200 - 1, this.f_97736_ + 40, this.f_97735_ + 200 + 1, this.f_97736_ + this.f_97727_, -16777216);
   }

   private void drawResearchList(GuiGraphics graphics) {
      List<ResearchGoal> goals = this.completedGoals();
      if (goals.isEmpty()) {
         this.drawTexturedButton(graphics, 8, 48, 160, 12, Component.m_237115_("guistrings.research.no_completed"), false);
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
         this.drawTexturedButton(graphics, 208, 48, 160, 12, Component.m_237115_("guistrings.research.no_completed"), false);
      } else {
         ResearchGoal goal = goals.get(Math.max(0, Math.min(this.selectedIndex, goals.size() - 1)));
         ItemStack book = ((ResearchBookMenu)this.f_97732_).getResearchBook();
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
         graphics.m_280614_(
            this.f_96547_,
            Component.m_237110_("guistrings.research.status", new Object[]{Component.m_237115_("guistrings.research.status." + statusKey)}),
            this.f_97735_ + x,
            this.f_97736_ + y,
            16777215,
            false
         );
         y += 14;
         if (currentGoal) {
            graphics.m_280614_(
               this.f_96547_,
               Component.m_237110_("guistrings.research.progress_value", new Object[]{ResearchBookItem.getCurrentProgress(book), goal.time()}),
               this.f_97735_ + x,
               this.f_97736_ + y,
               16777215,
               false
            );
            y += 14;
         }

         if (!goal.dependencies().isEmpty()) {
            graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.research.requires_header"), this.f_97735_ + x, this.f_97736_ + y, 16777215, false);
            y += 12;

            for (String dep : goal.dependencies()) {
               ResearchGoal dependency = ResearchRegistry.get(dep);
               graphics.m_280614_(
                  this.f_96547_,
                  (Component)(dependency == null ? Component.m_237113_(dep) : dependency.translatedName()),
                  this.f_97735_ + x + 8,
                  this.f_97736_ + y,
                  16777215,
                  false
               );
               y += 12;
            }
         }

         graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.research.resources_header"), this.f_97735_ + x, this.f_97736_ + y, 16777215, false);
         y += 14;
         List<ItemStack> resources = goal.resources();

         for (int i = 0; i < resources.size() && i < 9; i++) {
            int slotX = x + i % 3 * 18;
            int slotY = y + i / 3 * 18;
            this.drawDecorativeSlot(graphics, slotX, slotY);
            graphics.m_280480_(resources.get(i), this.f_97735_ + slotX, this.f_97736_ + slotY);
            graphics.m_280370_(this.f_96547_, resources.get(i), this.f_97735_ + slotX, this.f_97736_ + slotY);
         }

         y += (Math.min(resources.size(), 9) + 2) / 3 * 18 + 10;
         this.drawUnlocks(graphics, goal, x, y);
      }
   }

   private void drawUnlocks(GuiGraphics graphics, ResearchGoal goal, int x, int y) {
      List<EngineeringRecipe> unlocks = this.unlocksForResearch(goal.id());
      if (!unlocks.isEmpty() && y <= 188) {
         graphics.m_280614_(this.f_96547_, Component.m_237115_("guistrings.research.unlocks"), this.f_97735_ + x, this.f_97736_ + y, 16777215, false);
         y += 14;
         int max = Math.min(unlocks.size(), 8);

         for (int i = 0; i < max; i++) {
            ItemStack result = unlocks.get(i).result();
            int slotX = x + i % 4 * 18;
            int slotY = y + i / 4 * 18;
            this.drawDecorativeSlot(graphics, slotX, slotY);
            graphics.m_280480_(result, this.f_97735_ + slotX, this.f_97736_ + slotY);
            graphics.m_280370_(this.f_96547_, result, this.f_97735_ + slotX, this.f_97736_ + slotY);
         }

         if (unlocks.size() > max) {
            graphics.m_280614_(
               this.f_96547_,
               Component.m_237110_("guistrings.research.more", new Object[]{unlocks.size() - max}),
               this.f_97735_ + x + 78,
               this.f_97736_ + y + 22,
               16777215,
               false
            );
         }
      }
   }

   private List<EngineeringRecipe> unlocksForResearch(String researchId) {
      return this.f_96541_ != null && this.f_96541_.f_91073_ != null
         ? this.f_96541_
            .f_91073_
            .m_7465_()
            .m_44013_(AWCoreRegistryAccess.recipeType(AWCoreIds.id("engineering")))
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
      ItemStack book = ((ResearchBookMenu)this.f_97732_).getResearchBook();
      if (book.m_41619_()) {
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
      this.f_96541_.m_91106_().m_120367_(SimpleSoundInstance.m_263171_(SoundEvents.f_12490_, 1.0F));
   }

   private void drawTexturedButton(GuiGraphics graphics, int x, int y, int width, int height, Component label, boolean pressed) {
      this.drawNineSlice(graphics, WIDGETS, x, y, width, height, 0, pressed ? 80 : 40, 256, 40);
      int textWidth = this.f_96547_.m_92852_(label);
      graphics.m_280614_(this.f_96547_, label, this.f_97735_ + x + (width - textWidth) / 2, this.f_97736_ + y + (height - 8) / 2, 16777215, false);
   }

   private void drawDecorativeSlot(GuiGraphics graphics, int x, int y) {
      graphics.m_280218_(WIDGETS, this.f_97735_ + x - 1, this.f_97736_ + y - 1, 152, 120, 18, 18);
   }

   private void drawNineSlice(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int u, int v, int texWidth, int texHeight) {
      int halfWidth = width / 2;
      int halfHeight = height / 2;
      graphics.m_280218_(texture, this.f_97735_ + x, this.f_97736_ + y, u, v, halfWidth, halfHeight);
      graphics.m_280218_(texture, this.f_97735_ + x + halfWidth, this.f_97736_ + y, u + texWidth - (width - halfWidth), v, width - halfWidth, halfHeight);
      graphics.m_280218_(texture, this.f_97735_ + x, this.f_97736_ + y + halfHeight, u, v + texHeight - (height - halfHeight), halfWidth, height - halfHeight);
      graphics.m_280218_(
         texture,
         this.f_97735_ + x + halfWidth,
         this.f_97736_ + y + halfHeight,
         u + texWidth - (width - halfWidth),
         v + texHeight - (height - halfHeight),
         width - halfWidth,
         height - halfHeight
      );
   }
}
