package net.shadowmage.ancientwarfare.core.common.research;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record ResearchResource(String itemId, int count) {
   private static final String TAG_PREFIX = "#";

   public boolean matches(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         ResourceLocation id = this.resourceId();
         if (id == null) {
            return false;
         } else {
            return this.isTag()
               ? stack.is(TagKey.create(Registries.ITEM, id))
               : BuiltInRegistries.ITEM.containsKey(id) && id.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
         }
      }
   }

   public ItemStack toStack() {
      ResourceLocation id = this.resourceId();
      if (id == null) {
         return ItemStack.EMPTY;
      } else if (this.isTag()) {
         return BuiltInRegistries.ITEM
            .getTag(TagKey.create(Registries.ITEM, id))
            .flatMap(items -> items.stream().findFirst())
            .map(holder -> new ItemStack((ItemLike)holder.value(), this.count))
            .orElse(ItemStack.EMPTY);
      } else if (!BuiltInRegistries.ITEM.containsKey(id)) {
         return ItemStack.EMPTY;
      } else {
         Item item = (Item)BuiltInRegistries.ITEM.get(id);
         return item == null ? ItemStack.EMPTY : new ItemStack(item, this.count);
      }
   }

   private boolean isTag() {
      return this.itemId.startsWith("#");
   }

   private ResourceLocation resourceId() {
      return ResourceLocation.tryParse(this.isTag() ? this.itemId.substring("#".length()) : this.itemId);
   }
}
