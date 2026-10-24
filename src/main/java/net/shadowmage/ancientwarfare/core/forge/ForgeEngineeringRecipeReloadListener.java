package net.shadowmage.ancientwarfare.core.forge;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.engineering.EngineeringRecipe;
import net.shadowmage.ancientwarfare.core.common.engineering.EngineeringRecipeRegistry;
import net.shadowmage.ancientwarfare.core.common.engineering.EngineeringRecipeSerializer;
import net.shadowmage.ancientwarfare.core.item.BackpackItem;

@EventBusSubscriber(
   modid = "ancientwarfare"
)
public final class ForgeEngineeringRecipeReloadListener {
   private ForgeEngineeringRecipeReloadListener() {
   }

   @SubscribeEvent
   public static void addReloadListeners(AddReloadListenerEvent event) {
      event.addListener(new ForgeEngineeringRecipeReloadListener.Listener());
   }

   private static final class Listener extends SimpleJsonResourceReloadListener {
      private static final Gson GSON = new Gson();

      private Listener() {
         super(GSON, "engineering_recipes");
      }

      protected void apply(Map<ResourceLocation, JsonElement> json, ResourceManager manager, ProfilerFiller profiler) {
         List<EngineeringRecipe> recipes = new ArrayList<>();

         for (Entry<ResourceLocation, JsonElement> entry : json.entrySet()) {
            try {
               recipes.add(this.readRecipe(entry.getKey(), GsonHelper.convertToJsonObject(entry.getValue(), "engineering recipe")));
            } catch (RuntimeException var8) {
               AncientWarfareCore.LOG.error("Failed to load engineering recipe {}", entry.getKey(), var8);
            }
         }

         EngineeringRecipeRegistry.setRecipes(recipes);
         AncientWarfareCore.LOG.info("Loaded {} Ancient Warfare engineering recipes", recipes.size());
      }

      private EngineeringRecipe readRecipe(ResourceLocation id, JsonObject object) {
         return new EngineeringRecipeSerializer().fromJson(id, object);
      }

      private Map<Character, Ingredient> readKey(JsonObject keyObject) {
         Map<Character, Ingredient> key = new HashMap<>();

         for (Entry<String, JsonElement> entry : keyObject.entrySet()) {
            if (entry.getKey().length() != 1 || entry.getKey().charAt(0) == ' ') {
               throw new JsonSyntaxException("Engineering recipe keys must be one non-space character");
            }

            key.put(entry.getKey().charAt(0), Ingredient.fromJson(entry.getValue()));
         }

         return key;
      }

      private Ingredient[] readPattern(JsonArray pattern, Map<Character, Ingredient> key) {
         if (pattern.size() != 3) {
            throw new JsonSyntaxException("Engineering recipe pattern must have exactly 3 rows");
         } else {
            Ingredient[] ingredients = new Ingredient[9];

            for (int row = 0; row < 3; row++) {
               String line = GsonHelper.convertToString(pattern.get(row), "pattern row");
               if (line.length() != 3) {
                  throw new JsonSyntaxException("Engineering recipe pattern rows must be exactly 3 characters");
               }

               for (int col = 0; col < 3; col++) {
                  char keyChar = line.charAt(col);
                  if (keyChar == ' ') {
                     ingredients[row * 3 + col] = Ingredient.EMPTY;
                  } else {
                     Ingredient ingredient = key.get(keyChar);
                     if (ingredient == null) {
                        throw new JsonSyntaxException("Pattern references undefined key '" + keyChar + "'");
                     }

                     ingredients[row * 3 + col] = ingredient;
                  }
               }
            }

            return ingredients;
         }
      }

      private ItemStack readResult(JsonObject resultObject) {
         ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(resultObject, "item"));
         Item item = (Item)BuiltInRegistries.ITEM.get(itemId);
         if (item == null) {
            throw new JsonSyntaxException("Unknown result item " + itemId);
         } else {
            ItemStack result = new ItemStack(item, GsonHelper.getAsInt(resultObject, "count", 1));
            if (resultObject.has("backpack_level")) {
               BackpackItem.setLevel(result, GsonHelper.getAsInt(resultObject, "backpack_level"));
            }

            if (resultObject.has("tag")) {
               try {
                  result.setTag(TagParser.parseTag(GSON.toJson(resultObject.get("tag"))));
               } catch (Exception var6) {
                  throw new JsonSyntaxException("Invalid result tag", var6);
               }
            }

            return result;
         }
      }
   }
}
