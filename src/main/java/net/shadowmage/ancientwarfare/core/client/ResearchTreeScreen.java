package net.shadowmage.ancientwarfare.core.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.shadowmage.ancientwarfare.core.common.engineering.EngineeringRecipe;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.research.ResearchBookData;
import net.shadowmage.ancientwarfare.core.menu.ResearchStationMenu;
import net.shadowmage.ancientwarfare.core.research.ResearchGoal;
import net.shadowmage.ancientwarfare.core.research.ResearchRegistry;

/**
 * The whole tree of research as a map, the way the Civilization games draw it: each step of the
 * tree a column, every technology a card joined by lines to what it needs. Cards are coloured by
 * what the book knows (learned, being studied, queued, ready, out of reach); hovering one shows
 * its cost and what it opens, clicking one that is ready puts it in the queue (or takes it out).
 * Drag or scroll to look around.
 */
public final class ResearchTreeScreen extends AbstractContainerScreen<ResearchStationMenu> {
   private static final int NODE_WIDTH = 126;
   private static final int NODE_HEIGHT = 30;
   private static final int COLUMN = 162;
   private static final int ROW = 40;
   private static final int MARGIN = 20;
   private static final int HEADER = 34;
   private static final int LEARNED = 0xFF3C8C3C;
   private static final int CURRENT = 0xFFD8A020;
   private static final int QUEUED = 0xFF4070C8;
   private static final int READY = 0xFFE8E8E8;
   private static final int LOCKED = 0xFF5A5A5A;
   private final Map<String, int[]> positions = new HashMap<>();
   private final List<ResearchGoal> goals = new ArrayList<>();
   private int treeWidth;
   private int treeHeight;
   private double panX;
   private double panY;
   private boolean dragging;
   private double dragDistance;

   public ResearchTreeScreen(ResearchStationMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.titleLabelX = 10000;
      this.inventoryLabelY = 10000;
      this.layout();
   }

   /** Columns by depth; inside a column, each card sits near the cards it comes from, so lines cross little. */
   private void layout() {
      Map<Integer, List<ResearchGoal>> columns = new HashMap<>();
      int maxDepth = 0;
      for (ResearchGoal goal : ResearchRegistry.all()) {
         int depth = ResearchRegistry.depth(goal.id());
         columns.computeIfAbsent(depth, ignored -> new ArrayList<>()).add(goal);
         maxDepth = Math.max(maxDepth, depth);
         this.goals.add(goal);
      }

      Map<String, Double> rows = new HashMap<>();
      int maxRows = 0;
      for (int depth = 0; depth <= maxDepth; depth++) {
         List<ResearchGoal> column = columns.getOrDefault(depth, List.of());
         List<ResearchGoal> ordered = new ArrayList<>(column);
         ordered.sort(Comparator.comparingDouble(goal -> goal.dependencies().stream().mapToDouble(dep -> rows.getOrDefault(dep, 0.0)).average().orElse(0.0)));
         for (int row = 0; row < ordered.size(); row++) {
            rows.put(ordered.get(row).id(), (double)row);
            this.positions.put(ordered.get(row).id(), new int[]{MARGIN + depth * COLUMN, MARGIN + 18 + row * ROW});
         }

         maxRows = Math.max(maxRows, ordered.size());
      }

      this.treeWidth = MARGIN * 2 + maxDepth * COLUMN + NODE_WIDTH;
      this.treeHeight = MARGIN * 2 + 18 + maxRows * ROW;
   }

   @Override
   protected void init() {
      this.imageWidth = this.width;
      this.imageHeight = this.height;
      super.init();
      this.leftPos = 0;
      this.topPos = 0;
      this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.returnToStation()).bounds(this.width - 58, 4, 52, 14).build());
      this.clampPan();
   }

   private void clampPan() {
      int viewWidth = this.width;
      int viewHeight = this.height - HEADER - 12;
      this.panX = Math.max(Math.min(0, viewWidth - this.treeWidth), Math.min(0, this.panX));
      this.panY = Math.max(Math.min(0, viewHeight - this.treeHeight), Math.min(0, this.panY));
   }

   private enum State {
      LEARNED,
      CURRENT,
      QUEUED,
      READY,
      LOCKED
   }

   private State state(ResearchGoal goal) {
      ItemStack book = this.menu.book();
      Set<String> completed = ResearchBookData.getCompletedResearch(book);
      if (completed.contains(goal.id())) {
         return State.LEARNED;
      } else if (goal.id().equals(ResearchBookData.getCurrentResearch(book))) {
         return State.CURRENT;
      } else if (ResearchBookData.getQueuedResearch(book).contains(goal.id())) {
         return State.QUEUED;
      } else {
         return goal.canResearch(completed) ? State.READY : State.LOCKED;
      }
   }

   private static int color(State state) {
      return switch (state) {
         case LEARNED -> LEARNED;
         case CURRENT -> CURRENT;
         case QUEUED -> QUEUED;
         case READY -> READY;
         case LOCKED -> LOCKED;
      };
   }

   private int originX() {
      return (int)this.panX;
   }

   private int originY() {
      return HEADER + (int)this.panY;
   }

   @Override
   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
   }

   @Override
   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.fill(0, 0, this.width, this.height, 0xFF17130F);
      graphics.enableScissor(0, HEADER, this.width, this.height);
      int ox = this.originX();
      int oy = this.originY();
      int maxDepth = (this.treeWidth - MARGIN * 2 - NODE_WIDTH) / COLUMN;
      int kingdom = ResearchRegistry.depth(ResearchRegistry.ERA_KINGDOM);
      int empire = ResearchRegistry.depth(ResearchRegistry.ERA_EMPIRE);
      for (int depth = 0; depth <= maxDepth; depth++) {
         int x = ox + MARGIN + depth * COLUMN;
         // Each era is a band of columns, from the column after its own era card.
         int band = depth > empire ? 0x18C060FF : depth > kingdom ? 0x1060A0FF : 0;
         if (band != 0) {
            graphics.fill(x - 9, oy, x - 9 + COLUMN, oy + this.treeHeight, band);
         }

         if (depth == 0 || depth == kingdom + 1 || depth == empire + 1) {
            String era = depth == 0 ? "era_tribe" : depth == kingdom + 1 ? ResearchRegistry.ERA_KINGDOM : ResearchRegistry.ERA_EMPIRE;
            graphics.drawString(this.font, Component.translatable("research.ancientwarfare." + era).withStyle(ChatFormatting.BOLD), x, oy + 4, 0xFFE0C070, false);
         }

         graphics.fill(x - 10, oy, x - 9, oy + this.treeHeight, 0xFF2C251C);
         graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.research_tree.step", depth + 1), x, oy + MARGIN - 2, 0xFF8A7A60, false);
      }

      ItemStack book = this.menu.book();
      Set<String> completed = ResearchBookData.getCompletedResearch(book);
      for (ResearchGoal goal : this.goals) {
         int[] to = this.positions.get(goal.id());
         for (String dependency : goal.dependencies()) {
            int[] from = this.positions.get(dependency);
            if (from != null) {
               this.link(graphics, ox + from[0] + NODE_WIDTH, oy + from[1] + NODE_HEIGHT / 2, ox + to[0], oy + to[1] + NODE_HEIGHT / 2, completed.contains(dependency) ? 0xFF4FA04F : 0xFF6A5A48);
            }
         }
      }

      ResearchGoal hovered = null;
      for (ResearchGoal goal : this.goals) {
         int[] at = this.positions.get(goal.id());
         int x = ox + at[0];
         int y = oy + at[1];
         boolean over = mouseY >= HEADER && mouseX >= x && mouseX < x + NODE_WIDTH && mouseY >= y && mouseY < y + NODE_HEIGHT;
         if (over) {
            hovered = goal;
         }

         this.node(graphics, goal, x, y, over);
      }

      graphics.disableScissor();
      this.header(graphics);
      graphics.fill(0, this.height - 12, this.width, this.height, 0xC02A2219);
      graphics.drawString(this.font, Component.translatable("gui.ancientwarfare.research_tree.hint"), 8, this.height - 10, 0xFF8A7A60, false);
      // Only the buttons: the station's slots belong to its own screen.
      for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
         renderable.render(graphics, mouseX, mouseY, partialTick);
      }

      if (hovered != null) {
         graphics.renderComponentTooltip(this.font, this.tooltip(hovered), mouseX, mouseY);
      }
   }

   private void header(GuiGraphics graphics) {
      graphics.fill(0, 0, this.width, HEADER, 0xFF2A2219);
      graphics.fill(0, HEADER - 1, this.width, HEADER, 0xFF5A4A30);
      Component title = Component.translatable("gui.ancientwarfare.research_tree.title").withStyle(ChatFormatting.BOLD);
      graphics.drawString(this.font, title, 8, 6, 0xFFFFD860, false);
      graphics.drawString(
         this.font, Component.translatable("guistrings.research.science", ResearchBookData.getScience(this.menu.book())), 20 + this.font.width(title), 6, 0xFFA0E0FF, false
      );
      int x = 8;
      for (State state : State.values()) {
         graphics.fill(x, 20, x + 7, 27, color(state));
         Component label = Component.translatable("gui.ancientwarfare.research_tree." + state.name().toLowerCase(java.util.Locale.ROOT));
         graphics.drawString(this.font, label, x + 10, 19, 0xFFC8C0B0, false);
         x += 18 + this.font.width(label);
      }
   }

   /** A line from one card's right edge to the next card's left edge, turning once halfway. */
   private void link(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
      int mid = x2 - 12;
      graphics.fill(x1, y1, mid, y1 + 1, color);
      graphics.fill(mid, Math.min(y1, y2), mid + 1, Math.max(y1, y2) + 1, color);
      graphics.fill(mid, y2, x2, y2 + 1, color);
      graphics.fill(x2 - 3, y2 - 2, x2, y2 + 3, color);
   }

   private void node(GuiGraphics graphics, ResearchGoal goal, int x, int y, boolean over) {
      State state = this.state(goal);
      int border = color(state);
      if (goal.isEra()) {
         graphics.fill(x - 3, y - 3, x + NODE_WIDTH + 3, y + NODE_HEIGHT + 3, 0xFFE0B040);
      }

      graphics.fill(x - 1, y - 1, x + NODE_WIDTH + 1, y + NODE_HEIGHT + 1, over ? 0xFFFFFFFF : border);
      graphics.fill(x, y, x + NODE_WIDTH, y + NODE_HEIGHT, state == State.LOCKED ? 0xFF221D17 : 0xFF3A3023);
      graphics.fill(x, y, x + 3, y + NODE_HEIGHT, border);
      List<ItemStack> resources = goal.resources();
      if (!resources.isEmpty()) {
         graphics.renderFakeItem(resources.get(0), x + 5, y + 7);
      }

      int textColor = state == State.LOCKED ? 0xFF8A8070 : 0xFFFFFFFF;
      List<FormattedCharSequence> name = this.font.split(goal.translatedName(), NODE_WIDTH - 28);
      int line = name.size() > 1 ? y + 3 : y + 5;
      for (int index = 0; index < Math.min(2, name.size()); index++) {
         graphics.drawString(this.font, name.get(index), x + 24, line, textColor, false);
         line += 9;
      }

      int science = ResearchBookData.scienceCost(this.menu.book(), goal);
      Component cost = science > 0
         ? Component.literal(science + "✦").withStyle(ChatFormatting.AQUA)
         : Component.translatable("gui.ancientwarfare.research_tree.free").withStyle(ChatFormatting.GRAY);
      graphics.drawString(this.font, cost, x + 24, name.size() > 1 ? y + 21 : y + 17, 0xFFFFFFFF, false);
      if (state == State.CURRENT) {
         int time = Math.max(1, goal.time());
         int progress = Math.min(time, ResearchBookData.getCurrentProgress(this.menu.book()));
         graphics.fill(x + 3, y + NODE_HEIGHT - 2, x + 3 + (NODE_WIDTH - 3) * progress / time, y + NODE_HEIGHT, CURRENT);
      }
   }

   private List<Component> tooltip(ResearchGoal goal) {
      List<Component> lines = new ArrayList<>();
      State state = this.state(goal);
      lines.add(goal.translatedName().copy().withStyle(ChatFormatting.GOLD));
      lines.add(Component.translatable("gui.ancientwarfare.research_tree." + state.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GRAY));
      if (!goal.dependencies().isEmpty()) {
         MutableComponent needs = Component.translatable("gui.ancientwarfare.research_tree.needs").withStyle(ChatFormatting.YELLOW);
         boolean first = true;
         for (String dependency : goal.dependencies()) {
            ResearchGoal before = ResearchRegistry.get(dependency);
            needs.append(first ? " " : ", ").append(before == null ? Component.literal(dependency) : before.translatedName());
            first = false;
         }

         lines.add(needs);
      }

      int science = ResearchBookData.scienceCost(this.menu.book(), goal);
      if (science > 0) {
         lines.add(Component.translatable("gui.ancientwarfare.research_tree.science", science).withStyle(ChatFormatting.AQUA));
         float scale = ResearchBookData.getScienceScale(this.menu.book());
         if (scale > 1.0F) {
            lines.add(Component.translatable("gui.ancientwarfare.research_tree.scale", Math.round((scale - 1.0F) * 100.0F)).withStyle(ChatFormatting.DARK_AQUA));
         }
      }

      lines.add(Component.translatable("gui.ancientwarfare.research_tree.resources").withStyle(ChatFormatting.YELLOW));
      for (ItemStack stack : goal.resources()) {
         lines.add(Component.literal("  " + stack.getCount() + " × ").append(stack.getHoverName()).withStyle(ChatFormatting.WHITE));
      }

      String opensKey = "research.ancientwarfare." + goal.id() + ".opens";
      List<EngineeringRecipe> recipes = this.recipes(goal.id());
      if (I18n.exists(opensKey) || !recipes.isEmpty()) {
         lines.add(Component.translatable("gui.ancientwarfare.research_tree.opens").withStyle(ChatFormatting.GREEN));
         if (I18n.exists(opensKey)) {
            lines.add(Component.literal("  ").append(Component.translatable(opensKey)).withStyle(ChatFormatting.WHITE));
         }

         int shown = 0;
         for (EngineeringRecipe recipe : recipes) {
            if (shown++ == 6) {
               lines.add(Component.translatable("guistrings.research.more", recipes.size() - 6).withStyle(ChatFormatting.GRAY));
               break;
            }

            lines.add(Component.literal("  ").append(recipe.result().getHoverName()).withStyle(ChatFormatting.WHITE));
         }
      }

      if (state == State.READY) {
         lines.add(Component.translatable("gui.ancientwarfare.research_tree.click_queue").withStyle(ChatFormatting.DARK_GREEN));
      } else if (state == State.LOCKED) {
         lines.add(Component.translatable("gui.ancientwarfare.research_tree.click_road", this.road(goal).size()).withStyle(ChatFormatting.DARK_GREEN));
      } else if (state == State.QUEUED) {
         lines.add(Component.translatable("gui.ancientwarfare.research_tree.click_unqueue").withStyle(ChatFormatting.DARK_GREEN));
      }

      return lines;
   }

   private List<EngineeringRecipe> recipes(String research) {
      return this.minecraft == null || this.minecraft.level == null
         ? List.of()
         : this.minecraft
            .level
            .getRecipeManager()
            .getAllRecipesFor(AWCoreRegistryAccess.<RecipeType<EngineeringRecipe>>recipeType(AWCoreIds.id("engineering")))
            .stream()
            .filter(recipe -> research.equals(recipe.requiredResearch()))
            .toList();
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      // Only the buttons answer clicks: the container's own handling would treat the tree as slots.
      for (net.minecraft.client.gui.components.events.GuiEventListener child : this.children()) {
         if (child.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(child);
            return true;
         }
      }

      if (button == 0 && mouseY >= HEADER) {
         this.dragging = true;
         this.dragDistance = 0.0;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (this.dragging) {
         this.panX += dragX;
         this.panY += dragY;
         this.dragDistance += Math.abs(dragX) + Math.abs(dragY);
         this.clampPan();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (this.dragging && button == 0) {
         this.dragging = false;
         if (this.dragDistance < 4.0) {
            this.clickNode(mouseX, mouseY);
         }

         return true;
      } else {
         return false;
      }
   }

   private void clickNode(double mouseX, double mouseY) {
      int ox = this.originX();
      int oy = this.originY();
      int index = 0;
      for (ResearchGoal goal : this.goals) {
         int[] at = this.positions.get(goal.id());
         if (mouseX >= ox + at[0] && mouseX < ox + at[0] + NODE_WIDTH && mouseY >= oy + at[1] && mouseY < oy + at[1] + NODE_HEIGHT) {
            State state = this.state(goal);
            if (this.minecraft != null && this.minecraft.gameMode != null) {
               if (state == State.READY) {
                  this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
               } else if (state == State.LOCKED) {
                  // Queue the whole road to it, the earliest steps first.
                  for (ResearchGoal step : this.road(goal)) {
                     this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, this.goals.indexOf(step));
                  }
               } else if (state == State.QUEUED) {
                  int queued = ResearchBookData.getQueuedResearch(this.menu.book()).indexOf(goal.id());
                  this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 100 + queued);
               }
            }

            return;
         }

         index++;
      }
   }

   /** Everything not yet learned or queued on the way to the goal, the goal included, ordered so each step comes after what it needs. */
   private List<ResearchGoal> road(ResearchGoal goal) {
      ItemStack book = this.menu.book();
      Set<String> have = new java.util.HashSet<>(ResearchBookData.getCompletedResearch(book));
      have.addAll(ResearchBookData.getQueuedResearch(book));
      String current = ResearchBookData.getCurrentResearch(book);
      if (current != null) {
         have.add(current);
      }

      List<ResearchGoal> road = new ArrayList<>();
      this.collect(goal, have, road);
      road.sort(Comparator.comparingInt(step -> ResearchRegistry.depth(step.id())));
      return road;
   }

   private void collect(ResearchGoal goal, Set<String> have, List<ResearchGoal> road) {
      if (goal == null || have.contains(goal.id()) || road.contains(goal)) {
         return;
      }

      road.add(goal);
      for (String dependency : goal.dependencies()) {
         this.collect(ResearchRegistry.get(dependency), have, road);
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      if (hasShiftDown()) {
         this.panX += delta * 30.0;
      } else {
         this.panY += delta * 30.0;
      }

      this.clampPan();
      return true;
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode != 256 && !this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
         return super.keyPressed(keyCode, scanCode, modifiers);
      } else {
         this.returnToStation();
         return true;
      }
   }

   private void returnToStation() {
      this.minecraft.setScreen(new ResearchStationScreen(this.menu, this.minecraft.player.getInventory(), this.title));
   }
}
