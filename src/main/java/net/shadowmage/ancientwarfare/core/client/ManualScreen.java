package net.shadowmage.ancientwarfare.core.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.client.manual.ManualPaginator;
import net.shadowmage.ancientwarfare.core.menu.ManualMenu;

public class ManualScreen extends AbstractContainerScreen<ManualMenu> {
   private static final ResourceLocation MANUAL_TEXTURE = new ResourceLocation("ancientwarfare", "textures/gui/manual.png");
   private static final String INITIAL_CATEGORY = "table_of_contents";
   private static final int BOOK_WIDTH = 412;
   private static final int BOOK_HEIGHT = 254;
   private static final int PAGE_WIDTH = 206;
   private static final int PAGE_TEXT_WIDTH = 174;
   private static final int PAGE_TEXT_HEIGHT = 202;
   private static final int LINE_HEIGHT = 10;
   private static final int ITEMS_PER_ROW = 8;
   private final List<ManualScreen.LinkArea> links = new ArrayList<>();
   private final Deque<ManualScreen.Location> history = new ArrayDeque<>();
   private ManualPaginator.Book<ManualScreen.ManualElement> book = ManualPaginator.paginate(List.of(), 202);
   private String currentCategory = "table_of_contents";
   private int pageIndex;
   private float bookScale = 1.0F;
   private float bookLeft;
   private float bookTop;
   private ItemStack hoveredItem = ItemStack.EMPTY;

   public ManualScreen(ManualMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.imageWidth = 412;
      this.imageHeight = 254;
   }

   protected void init() {
      super.init();
      this.bookScale = Math.min(1.0F, Math.min(((float)this.width - 12.0F) / 412.0F, ((float)this.height - 12.0F) / 254.0F));
      this.bookScale = Math.max(0.1F, this.bookScale);
      this.bookLeft = ((float)this.width - 412.0F * this.bookScale) / 2.0F;
      this.bookTop = ((float)this.height - 254.0F * this.bookScale) / 2.0F;
      int savedPage = this.pageIndex;
      this.loadCategory(this.currentCategory);
      this.pageIndex = this.clampPage(savedPage);
   }

   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      graphics.blit(MANUAL_TEXTURE, 0, 0, 0.0F, 0.0F, 412, 254, 512, 512);
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      this.links.clear();
      this.hoveredItem = ItemStack.EMPTY;
      double localX = (double)(((float)mouseX - this.bookLeft) / this.bookScale);
      double localY = (double)(((float)mouseY - this.bookTop) / this.bookScale);
      graphics.pose().pushPose();
      graphics.pose().translate(this.bookLeft, this.bookTop, 0.0F);
      graphics.pose().scale(this.bookScale, this.bookScale, 1.0F);
      this.renderBg(graphics, partialTick, mouseX, mouseY);
      this.renderPage(graphics, this.pageIndex, 22, 18, localX, localY);
      this.renderPage(graphics, this.pageIndex + 1, 228, 18, localX, localY);
      this.renderFooter(graphics, localX, localY);
      graphics.pose().popPose();
      if (!this.hoveredItem.isEmpty()) {
         graphics.renderTooltip(this.font, this.hoveredItem, mouseX, mouseY);
      }
   }

   private void renderFooter(GuiGraphics graphics, double mouseX, double mouseY) {
      graphics.drawCenteredString(
         this.font,
         Component.translatable("gui.ancientwarfare.manual.pages", new Object[]{this.pageIndex + 1, this.pageIndex + 2, this.book.pages().size()}),
         272,
         232,
         6710886
      );
      this.drawControl(graphics, Component.literal("<"), 355, this.canTurn(-2), mouseX, mouseY);
      this.drawControl(graphics, Component.literal(">"), 389, this.canTurn(2), mouseX, mouseY);
      if (!this.currentCategory.equals("table_of_contents")) {
         this.drawControl(graphics, Component.translatable("gui.ancientwarfare.manual.contents"), 22, true, mouseX, mouseY);
         this.drawControl(graphics, Component.translatable("gui.ancientwarfare.manual.chapter"), 90, this.pageIndex > 0, mouseX, mouseY);
      }

      this.drawControl(graphics, Component.translatable("gui.ancientwarfare.manual.back"), 158, !this.history.isEmpty(), mouseX, mouseY);
   }

   private void drawControl(GuiGraphics graphics, Component label, int x, boolean enabled, double mouseX, double mouseY) {
      int color = !enabled
         ? 11184810
         : (contains(mouseX, mouseY, (double)(x - 3), 228.0, (double)(this.font.width(label) + 6), 18.0) ? 10112793 : 4473924);
      graphics.drawString(this.font, label, x, 232, color, false);
   }

   private void renderPage(GuiGraphics graphics, int index, int x, int y, double mouseX, double mouseY) {
      for (ManualPaginator.Placed<ManualScreen.ManualElement> placed : this.book.pages().get(index)) {
         int cursorY = y + placed.y();
         Object i = placed.content();
         if (i instanceof ManualScreen.TextLine) {
            ManualScreen.TextLine text = (ManualScreen.TextLine)i;
            int color = text.link() == null ? 1118481 : 6308643;
            int lineWidth = this.font.width(text.line());
            if (text.link() != null) {
               ManualScreen.LinkArea area = new ManualScreen.LinkArea(x, cursorY, lineWidth, 10, text.link());
               this.links.add(area);
               if (area.contains(mouseX, mouseY)) {
                  color = 10704668;
               }
            }

            graphics.drawString(this.font, text.line(), x, cursorY, color, false);
         } else if (placed.content() instanceof ManualScreen.ItemRow row) {
            for (int ix = 0; ix < row.stacks().size(); ix++) {
               ItemStack stack = row.stacks().get(ix);
               int itemX = x + ix * 21;
               graphics.renderItem(stack, itemX, cursorY);
               if (contains(mouseX, mouseY, (double)itemX, (double)cursorY, 16.0, 16.0)) {
                  this.hoveredItem = stack;
               }
            }
         }
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      double x = (mouseX - (double)this.bookLeft) / (double)this.bookScale;
      double y = (mouseY - (double)this.bookTop) / (double)this.bookScale;
      if (button == 0) {
         if (y >= 228.0 && y < 247.0) {
            if (x >= 346.0 && x < 377.0 && this.turnPage(-2)) {
               return true;
            }

            if (x >= 380.0 && x < 408.0 && this.turnPage(2)) {
               return true;
            }

            if (!this.currentCategory.equals("table_of_contents")) {
               if (x >= 19.0 && x < 85.0) {
                  this.navigate(new ManualScreen.Target("table_of_contents", ""));
                  return true;
               }

               if (x >= 87.0 && x < 153.0 && this.pageIndex > 0) {
                  this.remember();
                  this.setPage(0);
                  return true;
               }
            }

            if (x >= 155.0 && x < 218.0 && this.goBack()) {
               return true;
            }
         }

         for (ManualScreen.LinkArea link : List.copyOf(this.links)) {
            if (link.contains(x, y)) {
               this.navigate(link.target());
               return true;
            }
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
      return !contains(mouseX, mouseY, (double)this.bookLeft, (double)this.bookTop, (double)(412.0F * this.bookScale), (double)(254.0F * this.bookScale));
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
      if (delta != 0.0
         && contains(mouseX, mouseY, (double)this.bookLeft, (double)this.bookTop, (double)(412.0F * this.bookScale), (double)(254.0F * this.bookScale))) {
         this.turnPage(delta > 0.0 ? -2 : 2);
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, delta);
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 262 || keyCode == 267) {
         this.turnPage(2);
         return true;
      } else if (keyCode == 263 || keyCode == 266) {
         this.turnPage(-2);
         return true;
      } else if (keyCode == 268) {
         this.navigate(new ManualScreen.Target("table_of_contents", ""));
         return true;
      } else {
         return keyCode == 259 && this.goBack() ? true : super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   private boolean canTurn(int delta) {
      return this.pageIndex + delta >= 0 && this.pageIndex + delta < this.book.pages().size();
   }

   private boolean turnPage(int delta) {
      if (!this.canTurn(delta)) {
         return false;
      } else {
         this.setPage(this.pageIndex + delta);
         return true;
      }
   }

   private int clampPage(int index) {
      return Math.max(0, Math.min(index / 2 * 2, this.book.pages().size() - 2));
   }

   private void setPage(int index) {
      this.pageIndex = this.clampPage(index);
      this.links.clear();
   }

   private void remember() {
      ManualScreen.Location location = new ManualScreen.Location(this.currentCategory, this.pageIndex);
      if (!location.equals(this.history.peek())) {
         this.history.push(location);
      }

      while (this.history.size() > 64) {
         this.history.removeLast();
      }
   }

   private void navigate(ManualScreen.Target target) {
      this.remember();
      if (!this.currentCategory.equals(target.category())) {
         this.loadCategory(target.category());
      }

      this.setPage(target.anchor().isEmpty() ? 0 : this.book.anchors().getOrDefault(target.anchor(), 0));
   }

   private boolean goBack() {
      if (this.history.isEmpty()) {
         return false;
      } else {
         ManualScreen.Location location = this.history.pop();
         if (!this.currentCategory.equals(location.category())) {
            this.loadCategory(location.category());
         }

         this.setPage(location.page());
         return true;
      }
   }

   private void loadCategory(String category) {
      this.currentCategory = category;
      this.book = ManualPaginator.paginate(this.readElements(category), 202);
      this.setPage(0);
   }

   private Optional<Resource> findResource(String language, String category) {
      ResourceLocation location = ResourceLocation.tryParse("ancientwarfare:registry/manual/" + language + "/" + category + ".json");
      return location == null ? Optional.empty() : Minecraft.getInstance().getResourceManager().getResource(location);
   }

   private List<ManualPaginator.Element<ManualScreen.ManualElement>> readElements(String category) {
      String language = Minecraft.getInstance().getLanguageManager().getSelected().toLowerCase(Locale.ROOT);
      List<String> candidates = new ArrayList<>();
      candidates.add(language);
      if (language.startsWith("zh_") && !language.equals("zh_cn")) {
         candidates.add("zh_cn");
      }

      if (!language.equals("en_us")) {
         candidates.add("en_us");
      }

      for (String candidate : candidates) {
         Optional<Resource> resource = this.findResource(candidate, category);
         if (!resource.isEmpty()) {
            try {
               Object var24;
               try (BufferedReader reader = resource.get().openAsReader()) {
                  JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                  List<ManualPaginator.Element<ManualScreen.ManualElement>> elements = new ArrayList<>();

                  for (JsonElement element : GsonHelper.getAsJsonArray(root, "content", new JsonArray())) {
                     JsonObject object = element.getAsJsonObject();
                     String type = GsonHelper.getAsString(object, "content_type", "text");
                     switch (type) {
                        case "toc_list":
                           for (JsonElement entry : GsonHelper.getAsJsonArray(object, "items", new JsonArray())) {
                              JsonObject item = entry.getAsJsonObject();
                              ManualScreen.Target target = new ManualScreen.Target(
                                 GsonHelper.getAsString(item, "category_link", category), GsonHelper.getAsString(item, "anchor", "")
                              );
                              this.addText(elements, Component.literal(GsonHelper.getAsString(item, "text", "")), target, 5, false, null);
                           }
                           break;
                        case "heading":
                           this.addText(
                              elements,
                              Component.literal(GsonHelper.getAsString(object, "text", "")).withStyle(style -> style.withBold(true)),
                              null,
                              5,
                              true,
                              GsonHelper.getAsString(object, "anchor", "")
                           );
                           break;
                        case "text":
                           this.addText(elements, Component.literal(GsonHelper.getAsString(object, "text", "")), null, 4, false, null);
                           break;
                        case "item":
                           this.addItems(elements, object);
                     }
                  }

                  var24 = elements;
               }

               return (List<ManualPaginator.Element<ManualScreen.ManualElement>>)var24;
            } catch (Exception var22) {
               AncientWarfareCore.LOG.warn("Failed to read manual category {} in {}; trying fallback", new Object[]{category, candidate, var22});
            }
         }
      }

      List<ManualPaginator.Element<ManualScreen.ManualElement>> error = new ArrayList<>();
      this.addText(error, Component.translatable("gui.ancientwarfare.manual.missing", new Object[]{category}), null, 0, false, null);
      return error;
   }

   private void addText(
      List<ManualPaginator.Element<ManualScreen.ManualElement>> elements, Component text, ManualScreen.Target link, int gap, boolean heading, String anchor
   ) {
      List<FormattedCharSequence> lines = this.font.split(text, 174);

      for (int i = 0; i < lines.size(); i++) {
         elements.add(
            new ManualPaginator.Element<>(new ManualScreen.TextLine(lines.get(i), link), 10, i == lines.size() - 1 ? gap : 0, heading, i == 0 ? anchor : null)
         );
      }
   }

   private void addItems(List<ManualPaginator.Element<ManualScreen.ManualElement>> elements, JsonObject object) {
      List<ItemStack> stacks = new ArrayList<>();
      if (object.has("name")) {
         ItemStack stack = this.readItem(object);
         if (!stack.isEmpty()) {
            stacks.add(stack);
         }
      }

      for (JsonElement item : GsonHelper.getAsJsonArray(object, "items", new JsonArray())) {
         ItemStack stack = this.readItem(item.getAsJsonObject());
         if (!stack.isEmpty()) {
            stacks.add(stack);
         }
      }

      for (int i = 0; i < stacks.size(); i += 8) {
         elements.add(
            new ManualPaginator.Element<>(new ManualScreen.ItemRow(List.copyOf(stacks.subList(i, Math.min(stacks.size(), i + 8)))), 16, 5, false, null)
         );
      }
   }

   private ItemStack readItem(JsonObject object) {
      String name = GsonHelper.getAsString(object, "name", "");
      ResourceLocation id = ResourceLocation.tryParse(name);
      if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
         ItemStack stack = new ItemStack((ItemLike)BuiltInRegistries.ITEM.get(id));
         if (object.has("tag")) {
            try {
               JsonElement tag = object.get("tag");
               stack.setTag(TagParser.parseTag(tag.isJsonObject() ? tag.toString() : tag.getAsString()));
            } catch (Exception var6) {
               AncientWarfareCore.LOG.warn("Invalid manual item data for {}", name, var6);
            }
         }

         return stack;
      } else {
         return ItemStack.EMPTY;
      }
   }

   private static boolean contains(double mouseX, double mouseY, double x, double y, double width, double height) {
      return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
   }

   private static record ItemRow(List<ItemStack> stacks) implements ManualScreen.ManualElement {
   }

   private static record LinkArea(int x, int y, int width, int height, ManualScreen.Target target) {
      private boolean contains(double mouseX, double mouseY) {
         return ManualScreen.contains(mouseX, mouseY, (double)this.x, (double)this.y, (double)this.width, (double)this.height);
      }
   }

   private static record Location(String category, int page) {
   }

   private sealed interface ManualElement permits ManualScreen.TextLine, ManualScreen.ItemRow {
   }

   private static record Target(String category, String anchor) {
   }

   private static record TextLine(FormattedCharSequence line, ManualScreen.Target link) implements ManualScreen.ManualElement {
   }
}
