package net.shadowmage.ancientwarfare.core.item;

import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;

public class ProtectionFlagBlockItem extends BlockItem {
   public ProtectionFlagBlockItem(Block block, Properties properties) {
      super(block, properties);
   }

   public static ItemStack create(Block block, String factionName) {
      ItemStack stack = new ItemStack(block);
      String faction = normalize(factionName);
      CompoundTag tag = stack.getOrCreateTag();
      tag.putString("FlagName", faction);
      tag.putString("name", faction);
      return stack;
   }

   public Component getName(ItemStack stack) {
      String faction = factionName(stack);
      return (Component)(!faction.isBlank() && !"generic".equals(faction)
         ? Component.literal(displayFactionName(faction) + " ").append(Component.translatable("block.ancientwarfare.protection_flag"))
         : super.getName(stack));
   }

   private static String factionName(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag == null ? "generic" : normalize(tag.contains("FlagName") ? tag.getString("FlagName") : tag.getString("name"));
   }

   private static String displayFactionName(String faction) {
      StringBuilder result = new StringBuilder(faction.length());
      boolean upper = true;

      for (int i = 0; i < faction.length(); i++) {
         char character = faction.charAt(i);
         if (character != '_' && character != '-' && character != '.') {
            result.append(upper ? Character.toUpperCase(character) : character);
            upper = false;
         } else {
            result.append(' ');
            upper = true;
         }
      }

      return result.toString();
   }

   private static String normalize(String value) {
      return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
   }
}
