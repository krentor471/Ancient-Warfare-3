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
   private final NonNullList<ItemStack> items = NonNullList.m_122780_(28, ItemStack.f_41583_);
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
         this.m_6596_();
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
      if (!this.selectedTemplate.isBlank() && ((ItemStack)this.items.get(27)).m_41619_()) {
         List<DraftingStationBlockEntity.ResourceEntry> required = this.resources(serverLevel);
         if (!required.isEmpty()) {
            boolean changed = false;

            for (int slot = 0; slot < 27; slot++) {
               ItemStack stack = (ItemStack)this.items.get(slot);
               if (!stack.m_41619_()) {
                  ResourceLocation id = BuiltInRegistries.f_257033_.m_7981_(stack.m_41720_());
                  int maxRequired = required.stream().filter(r -> r.itemId().equals(id)).mapToInt(DraftingStationBlockEntity.ResourceEntry::count).sum();
                  int remaining = Math.max(0, maxRequired - this.suppliedMaterials.getOrDefault(id, 0));
                  if (remaining > 0) {
                     int accepted = Math.min(stack.m_41613_(), remaining);
                     this.suppliedMaterials.merge(id, accepted, Integer::sum);
                     stack.m_41774_(accepted);
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
      if (this.f_58857_ instanceof ServerLevel serverLevel && ((ItemStack)this.items.get(27)).m_41619_()) {
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
         output.m_41784_().m_128359_("TemplateName", this.selectedTemplate);
         this.items.set(27, output);
         this.started = false;
         this.markChangedAndSync();
         return;
      }
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      ContainerHelper.m_18973_(tag, this.items);
      tag.m_128359_("TemplateName", this.selectedTemplate);
      tag.m_128379_("Started", this.started);
      ListTag supplied = new ListTag();
      this.suppliedMaterials.forEach((id, count) -> {
         CompoundTag entry = new CompoundTag();
         entry.m_128359_("Item", id.toString());
         entry.m_128405_("Count", count);
         supplied.add(entry);
      });
      tag.m_128365_("SuppliedMaterials", supplied);
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      ContainerHelper.m_18980_(tag, this.items);
      this.selectedTemplate = tag.m_128461_("TemplateName");
      this.started = tag.m_128471_("Started");
      this.suppliedMaterials.clear();
      ListTag supplied = tag.m_128437_("SuppliedMaterials", 10);

      for (int i = 0; i < supplied.size(); i++) {
         CompoundTag entry = supplied.m_128728_(i);
         ResourceLocation id = ResourceLocation.m_135820_(entry.m_128461_("Item"));
         if (id != null && entry.m_128451_("Count") > 0) {
            this.suppliedMaterials.put(id, entry.m_128451_("Count"));
         }
      }
   }

   public int m_6643_() {
      return this.items.size();
   }

   public boolean m_7983_() {
      return this.items.stream().allMatch(ItemStack::m_41619_);
   }

   public ItemStack m_8020_(int slot) {
      return (ItemStack)this.items.get(slot);
   }

   public ItemStack m_7407_(int slot, int count) {
      ItemStack result = ContainerHelper.m_18969_(this.items, slot, count);
      if (!result.m_41619_()) {
         if (slot == 27 && ((ItemStack)this.items.get(slot)).m_41619_()) {
            this.suppliedMaterials.clear();
         }

         this.markChangedAndSync();
      }

      return result;
   }

   public ItemStack m_8016_(int slot) {
      ItemStack result = ContainerHelper.m_18966_(this.items, slot);
      if (!result.m_41619_()) {
         if (slot == 27) {
            this.suppliedMaterials.clear();
         }

         this.markChangedAndSync();
      }

      return result;
   }

   public void m_6836_(int slot, ItemStack stack) {
      this.items.set(slot, stack);
      if (stack.m_41613_() > this.m_6893_()) {
         stack.m_41764_(this.m_6893_());
      }

      this.markChangedAndSync();
   }

   public boolean m_6542_(Player player) {
      return this.f_58857_ != null && this.f_58857_.m_7702_(this.f_58858_) == this && player.m_20238_(this.f_58858_.m_252807_()) <= 64.0;
   }

   public void m_6211_() {
      this.items.clear();
      this.m_6596_();
   }

   public Component m_5446_() {
      return Component.m_237115_("block.ancientwarfare.drafting_station");
   }

   @Nullable
   public AbstractContainerMenu m_7208_(int id, Inventory inventory, Player player) {
      return new DraftingStationMenu(id, inventory, this.f_58858_);
   }

   private void markChangedAndSync() {
      this.m_6596_();
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 2);
      }
   }

   public CompoundTag m_5995_() {
      CompoundTag tag = super.m_5995_();
      this.m_183515_(tag);
      return tag;
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.m_195640_(this);
   }

   public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
      if (packet.m_131708_() != null) {
         this.m_142466_(packet.m_131708_());
      }
   }

   private static record ResourceEntry(ResourceLocation itemId, int count) {
   }
}
