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
      if (stack.m_41619_()) {
         return false;
      } else {
         ResourceLocation id = this.resourceId();
         if (id == null) {
            return false;
         } else {
            return this.isTag()
               ? stack.m_204117_(TagKey.m_203882_(Registries.f_256913_, id))
               : BuiltInRegistries.f_257033_.m_7804_(id) && id.equals(BuiltInRegistries.f_257033_.m_7981_(stack.m_41720_()));
         }
      }
   }

   public ItemStack toStack() {
      ResourceLocation id = this.resourceId();
      if (id == null) {
         return ItemStack.f_41583_;
      } else if (this.isTag()) {
         return BuiltInRegistries.f_257033_
            .m_203431_(TagKey.m_203882_(Registries.f_256913_, id))
            .flatMap(items -> items.m_203614_().findFirst())
            .map(holder -> new ItemStack((ItemLike)holder.m_203334_(), this.count))
            .orElse(ItemStack.f_41583_);
      } else if (!BuiltInRegistries.f_257033_.m_7804_(id)) {
         return ItemStack.f_41583_;
      } else {
         Item item = (Item)BuiltInRegistries.f_257033_.m_7745_(id);
         return item == null ? ItemStack.f_41583_ : new ItemStack(item, this.count);
      }
   }

   private boolean isTag() {
      return this.itemId.startsWith("#");
   }

   private ResourceLocation resourceId() {
      return ResourceLocation.m_135820_(this.isTag() ? this.itemId.substring("#".length()) : this.itemId);
   }
}
