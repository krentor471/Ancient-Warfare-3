package net.shadowmage.ancientwarfare.core.structure;

import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

public final class PlayerStructureTemplateData extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_player_structures";
   private static final int MAX_AXIS = 96;
   private static final int MAX_BLOCKS = 884736;
   private final Map<String, PlayerStructureTemplateData.PlayerTemplate> templates = new LinkedHashMap<>();

   public static PlayerStructureTemplateData get(ServerLevel level) {
      return (PlayerStructureTemplateData)level.getServer()
         .overworld()
         .getDataStorage()
         .computeIfAbsent(PlayerStructureTemplateData::load, PlayerStructureTemplateData::new, "ancientwarfare_player_structures");
   }

   public static PlayerStructureTemplateData load(CompoundTag tag) {
      PlayerStructureTemplateData data = new PlayerStructureTemplateData();
      ListTag list = tag.getList("Templates", 10);

      for (int i = 0; i < list.size(); i++) {
         PlayerStructureTemplateData.PlayerTemplate template = PlayerStructureTemplateData.PlayerTemplate.load(list.getCompound(i));
         if (!template.name().isBlank()) {
            data.templates.put(template.name(), template);
         }
      }

      return data;
   }

   public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();
      this.templates.values().forEach(template -> list.add(template.save()));
      tag.put("Templates", list);
      return tag;
   }

   public List<PlayerStructureTemplateData.PlayerTemplate> templates() {
      return this.templates.values().stream().sorted(Comparator.comparing(PlayerStructureTemplateData.PlayerTemplate::name)).toList();
   }

   public Optional<PlayerStructureTemplateData.PlayerTemplate> template(String name) {
      return Optional.ofNullable(this.templates.get(name));
   }

   public PlayerStructureTemplateData.CaptureResult capture(ServerLevel level, String requestedName, BlockPos first, BlockPos second, BlockPos origin) {
      String name = sanitizeName(requestedName);
      if (name.isBlank()) {
         return new PlayerStructureTemplateData.CaptureResult(false, "message.ancientwarfare.structure_scan.invalid_name", 0);
      } else {
         BlockPos min = new BlockPos(
            Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ())
         );
         BlockPos max = new BlockPos(
            Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ())
         );
         int sizeX = max.getX() - min.getX() + 1;
         int sizeY = max.getY() - min.getY() + 1;
         int sizeZ = max.getZ() - min.getZ() + 1;
         long volume = (long)sizeX * (long)sizeY * (long)sizeZ;
         if (sizeX <= 96 && sizeY <= 96 && sizeZ <= 96 && volume <= 884736L) {
            List<PlayerStructureTemplateData.TemplateBlock> blocks = new ArrayList<>((int)volume);
            Map<ResourceLocation, Integer> resources = new LinkedHashMap<>();

            for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
               BlockState state = level.getBlockState(pos);
               CompoundTag blockEntityTag = null;
               BlockEntity blockEntity = level.getBlockEntity(pos);
               if (blockEntity != null) {
                  blockEntityTag = blockEntity.saveWithFullMetadata();
                  blockEntityTag.remove("x");
                  blockEntityTag.remove("y");
                  blockEntityTag.remove("z");
               }

               blocks.add(new PlayerStructureTemplateData.TemplateBlock(pos.subtract(origin), state, blockEntityTag));
               if (!state.isAir() && state.getBlock() != Blocks.STRUCTURE_VOID) {
                  Item item = state.getBlock().asItem();
                  if (item != Items.AIR) {
                     resources.merge(BuiltInRegistries.ITEM.getKey(item), 1, Integer::sum);
                  }
               }
            }

            boolean replaced = this.templates.containsKey(name);
            PlayerStructureTemplateData.PlayerTemplate template = new PlayerStructureTemplateData.PlayerTemplate(
               name,
               sizeX,
               sizeY,
               sizeZ,
               blocks,
               resources.entrySet().stream().map(entry -> new PlayerStructureTemplateData.RequiredItem(entry.getKey(), entry.getValue())).toList()
            );
            this.templates.put(name, template);
            this.setDirty();
            return new PlayerStructureTemplateData.CaptureResult(
               true, replaced ? "message.ancientwarfare.structure_scan.replaced" : "message.ancientwarfare.structure_scan.saved", blocks.size()
            );
         } else {
            return new PlayerStructureTemplateData.CaptureResult(false, "message.ancientwarfare.structure_scan.too_large", (int)Math.min(2147483647L, volume));
         }
      }
   }

   public boolean remove(String requestedName) {
      String name = sanitizeName(requestedName);
      if (!name.isBlank() && this.templates.remove(name) != null) {
         this.setDirty();
         return true;
      } else {
         return false;
      }
   }

   private static String sanitizeName(String requestedName) {
      String value = requestedName == null ? "" : requestedName.trim();
      value = value.replaceAll("[^\\p{L}\\p{N}_ .-]", "_");
      return value.length() > 48 ? value.substring(0, 48) : value;
   }

   public static record CaptureResult(boolean success, String messageKey, int blockCount) {
   }

   public static record PlayerTemplate(
      String name,
      int sizeX,
      int sizeY,
      int sizeZ,
      List<PlayerStructureTemplateData.TemplateBlock> blocks,
      List<PlayerStructureTemplateData.RequiredItem> resources
   ) {
      CompoundTag save() {
         CompoundTag tag = new CompoundTag();
         tag.putString("Name", this.name);
         tag.putInt("SizeX", this.sizeX);
         tag.putInt("SizeY", this.sizeY);
         tag.putInt("SizeZ", this.sizeZ);
         ListTag blockList = new ListTag();
         this.blocks.forEach(block -> blockList.add(block.save()));
         tag.put("Blocks", blockList);
         ListTag resourceList = new ListTag();
         this.resources.forEach(resource -> resourceList.add(resource.save()));
         tag.put("Resources", resourceList);
         return tag;
      }

      static PlayerStructureTemplateData.PlayerTemplate load(CompoundTag tag) {
         List<PlayerStructureTemplateData.TemplateBlock> blocks = new ArrayList<>();
         ListTag blockList = tag.getList("Blocks", 10);

         for (int i = 0; i < blockList.size(); i++) {
            blocks.add(PlayerStructureTemplateData.TemplateBlock.load(blockList.getCompound(i)));
         }

         List<PlayerStructureTemplateData.RequiredItem> resources = new ArrayList<>();
         ListTag resourceList = tag.getList("Resources", 10);

         for (int i = 0; i < resourceList.size(); i++) {
            resources.add(PlayerStructureTemplateData.RequiredItem.load(resourceList.getCompound(i)));
         }

         return new PlayerStructureTemplateData.PlayerTemplate(
            tag.getString("Name"), tag.getInt("SizeX"), tag.getInt("SizeY"), tag.getInt("SizeZ"), blocks, resources
         );
      }
   }

   public static record RequiredItem(ResourceLocation itemId, int count) {
      public ItemStack displayStack() {
         Item item = (Item)BuiltInRegistries.ITEM.get(this.itemId);
         return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, Math.min(this.count, item.getMaxStackSize()));
      }

      CompoundTag save() {
         CompoundTag tag = new CompoundTag();
         tag.putString("Item", this.itemId.toString());
         tag.putInt("Count", this.count);
         return tag;
      }

      static PlayerStructureTemplateData.RequiredItem load(CompoundTag tag) {
         ResourceLocation id = ResourceLocation.tryParse(tag.getString("Item"));
         return new PlayerStructureTemplateData.RequiredItem(id == null ? new ResourceLocation("minecraft", "air") : id, Math.max(0, tag.getInt("Count")));
      }
   }

   public static record TemplateBlock(BlockPos relativePos, BlockState state, CompoundTag blockEntityTag) {
      CompoundTag save() {
         CompoundTag tag = new CompoundTag();
         tag.putInt("X", this.relativePos.getX());
         tag.putInt("Y", this.relativePos.getY());
         tag.putInt("Z", this.relativePos.getZ());
         BlockState.CODEC.encodeStart(NbtOps.INSTANCE, this.state).result().ifPresent(encoded -> tag.put("State", encoded));
         if (this.blockEntityTag != null) {
            tag.put("BlockEntity", this.blockEntityTag.copy());
         }

         return tag;
      }

      static PlayerStructureTemplateData.TemplateBlock load(CompoundTag tag) {
         DataResult<BlockState> decoded = BlockState.CODEC.parse(NbtOps.INSTANCE, tag.get("State"));
         BlockState state = decoded.result().orElse(Blocks.AIR.defaultBlockState());
         CompoundTag blockEntity = tag.contains("BlockEntity", 10) ? tag.getCompound("BlockEntity") : null;
         return new PlayerStructureTemplateData.TemplateBlock(new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")), state, blockEntity);
      }
   }
}
