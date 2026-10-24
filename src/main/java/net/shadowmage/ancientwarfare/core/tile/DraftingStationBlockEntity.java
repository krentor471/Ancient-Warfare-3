package net.shadowmage.ancientwarfare.core.tile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.menu.DraftingStationMenu;
import net.shadowmage.ancientwarfare.core.structure.AwsTemplatePlacer;
import net.shadowmage.ancientwarfare.core.structure.PlayerStructureTemplateData;
import org.jetbrains.annotations.Nullable;

public class DraftingStationBlockEntity extends BlockEntity implements Container, MenuProvider {
   private final NonNullList<ItemStack> items = NonNullList.withSize(28, ItemStack.EMPTY);
   private final Map<ResourceLocation, Integer> suppliedMaterials = new LinkedHashMap<>();
   private String selectedTemplate = "";
   private boolean started;

   public DraftingStationBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.id("drafting_station")), pos, state);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, DraftingStationBlockEntity station) {
      if (level instanceof ServerLevel serverLevel) {
         station.acceptInputMaterials(serverLevel);
         if (station.started) {
            station.consumeAndCraft();
         }
      }
   }

   public void selectTemplate(String name) {
      String next = name == null ? "" : name;
      if (!next.equals(this.selectedTemplate)) {
         this.selectedTemplate = next;
         this.suppliedMaterials.clear();
      }

      this.started = false;
      this.markChangedAndSync();
   }

   public String selectedTemplate() {
      return this.selectedTemplate;
   }

   public int suppliedCount(ResourceLocation itemId) {
      return this.suppliedMaterials.getOrDefault(itemId, 0);
   }

   public void start() {
      if (!this.selectedTemplate.isBlank()) {
         this.started = true;
         this.setChanged();
      }
   }

   private List<DraftingStationBlockEntity.ResourceEntry> resources(ServerLevel serverLevel) {
      PlayerStructureTemplateData.PlayerTemplate template = PlayerStructureTemplateData.get(serverLevel).template(this.selectedTemplate).orElse(null);
      if (template != null) {
         return template.resources().stream().map(r -> new DraftingStationBlockEntity.ResourceEntry(r.itemId(), r.count())).toList();
      } else {
         return this.selectedTemplate.startsWith("aws:")
            ? AwsTemplatePlacer.templateResources(this.selectedTemplate.substring(4))
               .stream()
               .map(r -> new DraftingStationBlockEntity.ResourceEntry(r.itemId(), r.count()))
               .toList()
            : List.of();
      }
   }

   private void acceptInputMaterials(ServerLevel serverLevel) {
      if (!this.selectedTemplate.isBlank() && ((ItemStack)this.items.get(27)).isEmpty()) {
         List<DraftingStationBlockEntity.ResourceEntry> required = this.resources(serverLevel);
         if (!required.isEmpty()) {
            boolean changed = false;

            for (int slot = 0; slot < 27; slot++) {
               ItemStack stack = (ItemStack)this.items.get(slot);
               if (!stack.isEmpty()) {
                  ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                  int maxRequired = required.stream().filter(r -> r.itemId().equals(id)).mapToInt(DraftingStationBlockEntity.ResourceEntry::count).sum();
                  int remaining = Math.max(0, maxRequired - this.suppliedMaterials.getOrDefault(id, 0));
                  if (remaining > 0) {
                     int accepted = Math.min(stack.getCount(), remaining);
                     this.suppliedMaterials.merge(id, accepted, Integer::sum);
                     stack.shrink(accepted);
                     changed = true;
                  }
               }
            }

            if (changed) {
               this.markChangedAndSync();
            }
         }
      }
   }

   private void consumeAndCraft() {
      if (this.level instanceof ServerLevel serverLevel && ((ItemStack)this.items.get(27)).isEmpty()) {
         List<DraftingStationBlockEntity.ResourceEntry> resources = this.resources(serverLevel);
         if (resources.isEmpty()) {
            this.started = false;
            return;
         }

         for (DraftingStationBlockEntity.ResourceEntry resource : resources) {
            if (this.suppliedMaterials.getOrDefault(resource.itemId(), 0) < resource.count()) {
               return;
            }
         }

         ItemStack output = new ItemStack(AWCoreRegistryAccess.block(AWCoreIds.STRUCTURE_BUILDER_TICKED));
         output.getOrCreateTag().putString("TemplateName", this.selectedTemplate);
         this.items.set(27, output);
         this.started = false;
         this.markChangedAndSync();
         return;
      }
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      ContainerHelper.saveAllItems(tag, this.items);
      tag.putString("TemplateName", this.selectedTemplate);
      tag.putBoolean("Started", this.started);
      ListTag supplied = new ListTag();
      this.suppliedMaterials.forEach((id, count) -> {
         CompoundTag entry = new CompoundTag();
         entry.putString("Item", id.toString());
         entry.putInt("Count", count);
         supplied.add(entry);
      });
      tag.put("SuppliedMaterials", supplied);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      ContainerHelper.loadAllItems(tag, this.items);
      this.selectedTemplate = tag.getString("TemplateName");
      this.started = tag.getBoolean("Started");
      this.suppliedMaterials.clear();
      ListTag supplied = tag.getList("SuppliedMaterials", 10);

      for (int i = 0; i < supplied.size(); i++) {
         CompoundTag entry = supplied.getCompound(i);
         ResourceLocation id = ResourceLocation.tryParse(entry.getString("Item"));
         if (id != null && entry.getInt("Count") > 0) {
            this.suppliedMaterials.put(id, entry.getInt("Count"));
         }
      }
   }

   public int getContainerSize() {
      return this.items.size();
   }

   public boolean isEmpty() {
      return this.items.stream().allMatch(ItemStack::isEmpty);
   }

   public ItemStack getItem(int slot) {
      return (ItemStack)this.items.get(slot);
   }

   public ItemStack removeItem(int slot, int count) {
      ItemStack result = ContainerHelper.removeItem(this.items, slot, count);
      if (!result.isEmpty()) {
         if (slot == 27 && ((ItemStack)this.items.get(slot)).isEmpty()) {
            this.suppliedMaterials.clear();
         }

         this.markChangedAndSync();
      }

      return result;
   }

   public ItemStack removeItemNoUpdate(int slot) {
      ItemStack result = ContainerHelper.takeItem(this.items, slot);
      if (!result.isEmpty()) {
         if (slot == 27) {
            this.suppliedMaterials.clear();
         }

         this.markChangedAndSync();
      }

      return result;
   }

   public void setItem(int slot, ItemStack stack) {
      this.items.set(slot, stack);
      if (stack.getCount() > this.getMaxStackSize()) {
         stack.setCount(this.getMaxStackSize());
      }

      this.markChangedAndSync();
   }

   public boolean stillValid(Player player) {
      return this.level != null && this.level.getBlockEntity(this.worldPosition) == this && player.distanceToSqr(this.worldPosition.getCenter()) <= 64.0;
   }

   public void clearContent() {
      this.items.clear();
      this.setChanged();
   }

   public Component getDisplayName() {
      return Component.translatable("block.ancientwarfare.drafting_station");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
      return new DraftingStationMenu(id, inventory, this.worldPosition);
   }

   private void markChangedAndSync() {
      this.setChanged();
      if (this.level != null && !this.level.isClientSide) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
      }
   }

   public CompoundTag getUpdateTag() {
      CompoundTag tag = super.getUpdateTag();
      this.saveAdditional(tag);
      return tag;
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
      if (packet.getTag() != null) {
         this.load(packet.getTag());
      }
   }

   private static record ResourceEntry(ResourceLocation itemId, int count) {
   }
}
