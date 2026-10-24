package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * A player's own coat of arms, drawn on a loom: the banner they last handed to one of their
 * soldiers. Their soldiers' plain shields bear it, and a soldier with the banner on his head is
 * their standard-bearer.
 */
public final class PlayerHeraldry extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_player_heraldry";
   /** Marks a shield painted with its owner's arms, so it is repainted when the arms change. */
   public static final String PAINTED_TAG = "AWArms";
   /** Each player's arms: the banner's base colour and its patterns. */
   private final Map<UUID, CompoundTag> arms = new HashMap<>();

   public static PlayerHeraldry get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(PlayerHeraldry::load, PlayerHeraldry::new, DATA_NAME);
   }

   public static boolean isBanner(ItemStack stack) {
      return stack.getItem() instanceof BannerItem;
   }

   /** The banner's design becomes the player's arms. */
   public void setArms(UUID owner, ItemStack banner) {
      CompoundTag design = new CompoundTag();
      design.putInt("Base", ((BannerItem)banner.getItem()).getColor().getId());
      CompoundTag entity = BlockItem.getBlockEntityData(banner);
      design.put("Patterns", entity != null ? entity.getList("Patterns", Tag.TAG_COMPOUND).copy() : new ListTag());
      this.arms.put(owner, design);
      this.setDirty();
   }

   public CompoundTag arms(UUID owner) {
      return this.arms.get(owner);
   }

   /**
    * Paints the owner's arms on a shield that has none of its own, or that bears older arms of
    * theirs. Returns true when it changed the shield.
    */
   public boolean paint(UUID owner, ItemStack shield) {
      CompoundTag design = this.arms.get(owner);
      if (design == null || !shield.is(Items.SHIELD)) {
         return false;
      }

      CompoundTag entity = BlockItem.getBlockEntityData(shield);
      boolean ours = entity == null || entity.getBoolean(PAINTED_TAG);
      if (!ours || entity != null && entity.getInt("Base") == design.getInt("Base") && entity.get("Patterns") != null && entity.get("Patterns").equals(design.get("Patterns"))) {
         return false;
      }

      CompoundTag painted = design.copy();
      painted.putBoolean(PAINTED_TAG, true);
      shield.getOrCreateTag().put("BlockEntityTag", painted);
      return true;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      CompoundTag all = new CompoundTag();
      this.arms.forEach((owner, design) -> all.put(owner.toString(), design.copy()));
      tag.put("Arms", all);
      return tag;
   }

   private static PlayerHeraldry load(CompoundTag tag) {
      PlayerHeraldry data = new PlayerHeraldry();
      CompoundTag all = tag.getCompound("Arms");
      for (String key : all.getAllKeys()) {
         try {
            data.arms.put(UUID.fromString(key), all.getCompound(key).copy());
         } catch (IllegalArgumentException ignored) {
         }
      }

      return data;
   }
}
