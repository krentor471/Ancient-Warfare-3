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
      return (PlayerStructureTemplateData)level.m_7654_()
         .m_129783_()
         .m_8895_()
         .m_164861_(PlayerStructureTemplateData::load, PlayerStructureTemplateData::new, "ancientwarfare_player_structures");
   }

   public static PlayerStructureTemplateData load(CompoundTag tag) {
      PlayerStructureTemplateData data = new PlayerStructureTemplateData();
      ListTag list = tag.m_128437_("Templates", 10);

      for (int i = 0; i < list.size(); i++) {
         PlayerStructureTemplateData.PlayerTemplate template = PlayerStructureTemplateData.PlayerTemplate.load(list.m_128728_(i));
         if (!template.name().isBlank()) {
            data.templates.put(template.name(), template);
         }
      }

      return data;
   }

   public CompoundTag m_7176_(CompoundTag tag) {
      ListTag list = new ListTag();
      this.templates.values().forEach(template -> list.add(template.save()));
      tag.m_128365_("Templates", list);
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
            Math.min(first.m_123341_(), second.m_123341_()), Math.min(first.m_123342_(), second.m_123342_()), Math.min(first.m_123343_(), second.m_123343_())
         );
         BlockPos max = new BlockPos(
            Math.max(first.m_123341_(), second.m_123341_()), Math.max(first.m_123342_(), second.m_123342_()), Math.max(first.m_123343_(), second.m_123343_())
         );
         int sizeX = max.m_123341_() - min.m_123341_() + 1;
         int sizeY = max.m_123342_() - min.m_123342_() + 1;
         int sizeZ = max.m_123343_() - min.m_123343_() + 1;
         long volume = (long)sizeX * (long)sizeY * (long)sizeZ;
         if (sizeX <= 96 && sizeY <= 96 && sizeZ <= 96 && volume <= 884736L) {
            List<PlayerStructureTemplateData.TemplateBlock> blocks = new ArrayList<>((int)volume);
            Map<ResourceLocation, Integer> resources = new LinkedHashMap<>();

            for (BlockPos pos : BlockPos.m_121940_(min, max)) {
               BlockState state = level.m_8055_(pos);
               CompoundTag blockEntityTag = null;
               BlockEntity blockEntity = level.m_7702_(pos);
               if (blockEntity != null) {
                  blockEntityTag = blockEntity.m_187480_();
                  blockEntityTag.m_128473_("x");
                  blockEntityTag.m_128473_("y");
                  blockEntityTag.m_128473_("z");
               }

               blocks.add(new PlayerStructureTemplateData.TemplateBlock(pos.m_121996_(origin), state, blockEntityTag));
               if (!state.m_60795_() && state.m_60734_() != Blocks.f_50454_) {
                  Item item = state.m_60734_().m_5456_();
                  if (item != Items.f_41852_) {
                     resources.merge(BuiltInRegistries.f_257033_.m_7981_(item), 1, Integer::sum);
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
            this.m_77762_();
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
         this.m_77762_();
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
         tag.m_128359_("Name", this.name);
         tag.m_128405_("SizeX", this.sizeX);
         tag.m_128405_("SizeY", this.sizeY);
         tag.m_128405_("SizeZ", this.sizeZ);
         ListTag blockList = new ListTag();
         this.blocks.forEach(block -> blockList.add(block.save()));
         tag.m_128365_("Blocks", blockList);
         ListTag resourceList = new ListTag();
         this.resources.forEach(resource -> resourceList.add(resource.save()));
         tag.m_128365_("Resources", resourceList);
         return tag;
      }

      static PlayerStructureTemplateData.PlayerTemplate load(CompoundTag tag) {
         List<PlayerStructureTemplateData.TemplateBlock> blocks = new ArrayList<>();
         ListTag blockList = tag.m_128437_("Blocks", 10);

         for (int i = 0; i < blockList.size(); i++) {
            blocks.add(PlayerStructureTemplateData.TemplateBlock.load(blockList.m_128728_(i)));
         }

         List<PlayerStructureTemplateData.RequiredItem> resources = new ArrayList<>();
         ListTag resourceList = tag.m_128437_("Resources", 10);

         for (int i = 0; i < resourceList.size(); i++) {
            resources.add(PlayerStructureTemplateData.RequiredItem.load(resourceList.m_128728_(i)));
         }

         return new PlayerStructureTemplateData.PlayerTemplate(
            tag.m_128461_("Name"), tag.m_128451_("SizeX"), tag.m_128451_("SizeY"), tag.m_128451_("SizeZ"), blocks, resources
         );
      }
   }

   public static record RequiredItem(ResourceLocation itemId, int count) {
      public ItemStack displayStack() {
         Item item = (Item)BuiltInRegistries.f_257033_.m_7745_(this.itemId);
         return item == Items.f_41852_ ? ItemStack.f_41583_ : new ItemStack(item, Math.min(this.count, item.m_41459_()));
      }

      CompoundTag save() {
         CompoundTag tag = new CompoundTag();
         tag.m_128359_("Item", this.itemId.toString());
         tag.m_128405_("Count", this.count);
         return tag;
      }

      static PlayerStructureTemplateData.RequiredItem load(CompoundTag tag) {
         ResourceLocation id = ResourceLocation.m_135820_(tag.m_128461_("Item"));
         return new PlayerStructureTemplateData.RequiredItem(id == null ? new ResourceLocation("minecraft", "air") : id, Math.max(0, tag.m_128451_("Count")));
      }
   }

   public static record TemplateBlock(BlockPos relativePos, BlockState state, CompoundTag blockEntityTag) {
      CompoundTag save() {
         CompoundTag tag = new CompoundTag();
         tag.m_128405_("X", this.relativePos.m_123341_());
         tag.m_128405_("Y", this.relativePos.m_123342_());
         tag.m_128405_("Z", this.relativePos.m_123343_());
         BlockState.f_61039_.encodeStart(NbtOps.f_128958_, this.state).result().ifPresent(encoded -> tag.m_128365_("State", encoded));
         if (this.blockEntityTag != null) {
            tag.m_128365_("BlockEntity", this.blockEntityTag.m_6426_());
         }

         return tag;
      }

      static PlayerStructureTemplateData.TemplateBlock load(CompoundTag tag) {
         DataResult<BlockState> decoded = BlockState.f_61039_.parse(NbtOps.f_128958_, tag.m_128423_("State"));
         BlockState state = decoded.result().orElse(Blocks.f_50016_.m_49966_());
         CompoundTag blockEntity = tag.m_128425_("BlockEntity", 10) ? tag.m_128469_("BlockEntity") : null;
         return new PlayerStructureTemplateData.TemplateBlock(new BlockPos(tag.m_128451_("X"), tag.m_128451_("Y"), tag.m_128451_("Z")), state, blockEntity);
      }
   }
}
