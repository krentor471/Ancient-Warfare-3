package net.shadowmage.ancientwarfare.core.common.engineering;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.shadowmage.ancientwarfare.core.common.backpack.BackpackData;

public final class EngineeringRecipeSerializer implements RecipeSerializer<EngineeringRecipe> {
   public EngineeringRecipe fromJson(ResourceLocation id, JsonObject object) {
      boolean shapeless = GsonHelper.getAsBoolean(object, "shapeless", false);
      EngineeringIngredient[] ingredients = shapeless
         ? readShapeless(GsonHelper.getAsJsonArray(object, "ingredients"))
         : readPattern(GsonHelper.getAsJsonArray(object, "pattern"), readKey(GsonHelper.getAsJsonObject(object, "key", new JsonObject())));
      ItemStack result = readResult(GsonHelper.getAsJsonObject(object, "result"));
      String requiredResearch = GsonHelper.getAsString(object, "required_research", null);
      return new EngineeringRecipe(id, ingredients, shapeless, result, requiredResearch);
   }

   @Nullable
   public EngineeringRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
      EngineeringIngredient[] ingredients = new EngineeringIngredient[9];

      for (int slot = 0; slot < ingredients.length; slot++) {
         ingredients[slot] = EngineeringIngredient.fromNetwork(buffer);
      }

      boolean shapeless = buffer.readBoolean();
      ItemStack result = buffer.readItem();
      String requiredResearch = buffer.readBoolean() ? buffer.readUtf(128) : null;
      return new EngineeringRecipe(id, ingredients, shapeless, result, requiredResearch);
   }

   public void toNetwork(FriendlyByteBuf buffer, EngineeringRecipe recipe) {
      for (EngineeringIngredient ingredient : recipe.ingredients()) {
         ingredient.toNetwork(buffer);
      }

      buffer.writeBoolean(recipe.shapeless());
      buffer.writeItem(recipe.result());
      buffer.writeBoolean(recipe.requiredResearch() != null);
      if (recipe.requiredResearch() != null) {
         buffer.writeUtf(recipe.requiredResearch(), 128);
      }
   }

   static Map<Character, EngineeringIngredient> readKey(JsonObject keyObject) {
      Map<Character, EngineeringIngredient> key = new HashMap<>();

      for (Entry<String, JsonElement> entry : keyObject.entrySet()) {
         if (entry.getKey().length() != 1 || entry.getKey().charAt(0) == ' ') {
            throw new JsonSyntaxException("Engineering recipe keys must be one non-space character");
         }

         key.put(entry.getKey().charAt(0), readIngredient(entry.getValue()));
      }

      return key;
   }

   static EngineeringIngredient[] readPattern(JsonArray pattern, Map<Character, EngineeringIngredient> key) {
      if (pattern.size() != 3) {
         throw new JsonSyntaxException("Engineering recipe pattern must have exactly 3 rows");
      } else {
         EngineeringIngredient[] ingredients = new EngineeringIngredient[9];

         for (int row = 0; row < 3; row++) {
            String line = GsonHelper.convertToString(pattern.get(row), "pattern row");
            if (line.length() != 3) {
               throw new JsonSyntaxException("Engineering recipe pattern rows must be exactly 3 characters");
            }

            for (int col = 0; col < 3; col++) {
               char keyChar = line.charAt(col);
               EngineeringIngredient ingredient = keyChar == ' ' ? EngineeringIngredient.empty() : key.get(keyChar);
               if (ingredient == null) {
                  throw new JsonSyntaxException("Pattern references undefined key '" + keyChar + "'");
               }

               ingredients[row * 3 + col] = ingredient;
            }
         }

         return ingredients;
      }
   }

   static EngineeringIngredient[] readShapeless(JsonArray array) {
      if (array.size() > 9) {
         throw new JsonSyntaxException("Engineering recipes support at most 9 ingredients");
      } else {
         EngineeringIngredient[] ingredients = new EngineeringIngredient[9];
         Arrays.fill(ingredients, EngineeringIngredient.empty());

         for (int index = 0; index < array.size(); index++) {
            ingredients[index] = readIngredient(array.get(index));
         }

         return ingredients;
      }
   }

   static EngineeringIngredient readIngredient(JsonElement element) {
      if (element.isJsonObject() && element.getAsJsonObject().has("ingredient")) {
         JsonObject wrapper = element.getAsJsonObject();
         Ingredient ingredient = Ingredient.fromJson(wrapper.get("ingredient"));
         int count = GsonHelper.getAsInt(wrapper, "count", 1);
         CompoundTag tag = null;
         if (wrapper.has("nbt")) {
            try {
               JsonElement nbt = wrapper.get("nbt");
               tag = TagParser.parseTag(nbt.isJsonPrimitive() ? nbt.getAsString() : nbt.toString());
            } catch (Exception var6) {
               throw new JsonSyntaxException("Invalid engineering ingredient NBT", var6);
            }
         }

         return new EngineeringIngredient(ingredient, count, tag);
      } else {
         return new EngineeringIngredient(Ingredient.fromJson(element), 1, null);
      }
   }

   static ItemStack readResult(JsonObject resultObject) {
      ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(resultObject, "item"));
      if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
         throw new JsonSyntaxException("Unknown result item " + itemId);
      } else {
         Item item = (Item)BuiltInRegistries.ITEM.get(itemId);
         ItemStack result = new ItemStack(item, GsonHelper.getAsInt(resultObject, "count", 1));
         if (resultObject.has("backpack_level")) {
            BackpackData.setLevel(result, GsonHelper.getAsInt(resultObject, "backpack_level"));
         }

         if (resultObject.has("tag")) {
            try {
               JsonElement tag = resultObject.get("tag");
               result.setTag(TagParser.parseTag(tag.isJsonPrimitive() ? tag.getAsString() : tag.toString()));
            } catch (Exception var5) {
               throw new JsonSyntaxException("Invalid result tag", var5);
            }
         }

         return result;
      }
   }
}
