package net.shadowmage.ancientwarfare.core.tile;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.shadowmage.ancientwarfare.core.block.MailboxBlock;
import net.shadowmage.ancientwarfare.core.common.inventory.ItemHandlerAccess;
import net.shadowmage.ancientwarfare.core.common.inventory.PlatformItemHandlerProvider;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.SideConfigurableWorksite;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteInventoryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteRelativeSide;
import net.shadowmage.ancientwarfare.core.menu.MailboxMenu;

public class MailboxBlockEntity extends BlockEntity implements MenuProvider, SideConfigurableWorksite, PlatformItemHandlerProvider {
   public static final int SEND_SLOTS = 18;
   public static final int RECEIVE_SLOTS = 18;
   private final NonNullList<ItemStack> send = NonNullList.withSize(18, ItemStack.EMPTY);
   private final NonNullList<ItemStack> received = NonNullList.withSize(18, ItemStack.EMPTY);
   private final EnumMap<WorksiteRelativeSide, WorksiteInventoryAccess> sides = new EnumMap<>(WorksiteRelativeSide.class);
   private String name = "";
   private String target = "";
   private boolean privateBox;
   private boolean autoExport;
   private String ownerName = "";

   public MailboxBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.MAILBOX), pos, state);

      for (WorksiteRelativeSide side : WorksiteRelativeSide.values()) {
         this.sides.put(side, WorksiteInventoryAccess.NONE);
      }

      this.sides.put(WorksiteRelativeSide.TOP, WorksiteInventoryAccess.INPUT);
      this.sides.put(WorksiteRelativeSide.BOTTOM, WorksiteInventoryAccess.OUTPUT);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, MailboxBlockEntity mailbox) {
      if (level instanceof ServerLevel serverLevel) {
         MailboxData data = MailboxData.get(serverLevel);
         data.register(mailbox);
         if (mailbox.target != null && !mailbox.target.isBlank()) {
            mailbox.sendNow();
         }
      }
   }

   public String getMailboxName() {
      return this.name;
   }

   public String getTargetName() {
      return this.target;
   }

   public boolean isPrivateBox() {
      return this.privateBox;
   }

   public boolean isAutoExport() {
      return this.autoExport;
   }

   public void setMailboxName(String name) {
      this.name = name == null ? "" : name.trim();
      this.setChanged();
   }

   public void setTargetName(String target) {
      this.target = target == null ? "" : target.trim();
      this.setChanged();
   }

   public void setPrivateBox(boolean privateBox) {
      this.privateBox = privateBox;
      this.name = "";
      this.target = "";
      this.setChanged();
   }

   public void setAutoExport(boolean autoExport) {
      this.autoExport = autoExport;
      this.setChanged();
   }

   public String addressKey() {
      return this.name.isBlank() ? null : this.keyFor(this.name);
   }

   public String keyFor(String mailboxName) {
      return (this.privateBox ? this.ownerKey() + ":" : "public:") + mailboxName;
   }

   private String ownerKey() {
      return this.getOwnerName();
   }

   private String getOwnerName() {
      return this.ownerName;
   }

   public void setOwnerName(String ownerName) {
      this.ownerName = ownerName == null ? "" : ownerName;
      this.setChanged();
   }

   public String getOwnerNamePublic() {
      return this.getOwnerName();
   }

   public ItemStack insertReceived(ItemStack stack) {
      ItemStack remaining = stack;

      for (int slot = 0; slot < 18 && !remaining.isEmpty(); slot++) {
         remaining = this.insert(this.send, slot, remaining, false);
      }

      this.setChanged();
      return remaining;
   }

   public Container sendContainer() {
      return new MailboxBlockEntity.MailboxContainer(this.send);
   }

   public Container receivedContainer() {
      return new MailboxBlockEntity.MailboxContainer(this.received);
   }

   public boolean stillValid(Player player) {
      return this.level != null
         && this.level.getBlockEntity(this.worldPosition) == this
         && player.distanceToSqr((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5) < 64.0;
   }

   public void sendNow() {
      if (this.level instanceof ServerLevel serverLevel && this.target != null && !this.target.isBlank()) {
         for (int slot = 0; slot < 18; slot++) {
            ItemStack stack = (ItemStack)this.received.get(slot);
            if (!stack.isEmpty()) {
               ItemStack remaining = MailboxData.get(serverLevel).deliver(this, this.target, stack.copy());
               int moved = stack.getCount() - remaining.getCount();
               if (moved > 0) {
                  this.extract(this.received, slot, moved, false);
               }
            }
         }

         return;
      }
   }

   public Component getDisplayName() {
      return Component.translatable("container.ancientwarfare.mailbox");
   }

   public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
      return new MailboxMenu(containerId, inventory, this);
   }

   public CompoundTag getUpdateTag() {
      CompoundTag tag = new CompoundTag();
      this.saveAdditional(tag);
      return tag;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public void handleUpdateTag(CompoundTag tag) {
      this.load(tag);
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.put("Send", saveItems(this.send));
      tag.put("Received", saveItems(this.received));
      tag.putString("Name", this.name);
      tag.putString("Target", this.target);
      tag.putBoolean("Private", this.privateBox);
      tag.putBoolean("Auto", this.autoExport);
      tag.putString("Owner", this.getOwnerName());
      CompoundTag sideTag = new CompoundTag();
      this.sides.forEach((side, access) -> sideTag.putString(side.name(), access.name()));
      tag.put("Sides", sideTag);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      if (tag.contains("Send")) {
         ContainerHelper.loadAllItems(tag.getCompound("Send"), this.send);
      }

      if (tag.contains("Received")) {
         ContainerHelper.loadAllItems(tag.getCompound("Received"), this.received);
      }

      this.name = tag.getString("Name");
      this.target = tag.getString("Target");
      this.privateBox = tag.getBoolean("Private");
      this.autoExport = tag.getBoolean("Auto");
      this.setOwnerName(tag.getString("Owner"));
      if (tag.contains("Sides")) {
         CompoundTag sideTag = tag.getCompound("Sides");

         for (WorksiteRelativeSide side : WorksiteRelativeSide.values()) {
            if (sideTag.contains(side.name())) {
               try {
                  this.sides.put(side, WorksiteInventoryAccess.valueOf(sideTag.getString(side.name())));
               } catch (IllegalArgumentException var8) {
                  this.sides.put(side, WorksiteInventoryAccess.NONE);
               }
            }
         }
      }
   }

   private static CompoundTag saveItems(NonNullList<ItemStack> items) {
      CompoundTag tag = new CompoundTag();
      ContainerHelper.saveAllItems(tag, items);
      return tag;
   }

   @Override
   public WorksiteInventoryAccess getSideAccess(WorksiteRelativeSide side) {
      return this.sides.getOrDefault(side, WorksiteInventoryAccess.NONE);
   }

   @Override
   public void setSideAccess(WorksiteRelativeSide side, WorksiteInventoryAccess access) {
      if (access != WorksiteInventoryAccess.INPUT && access != WorksiteInventoryAccess.OUTPUT && access != WorksiteInventoryAccess.NONE) {
         access = WorksiteInventoryAccess.NONE;
      }

      this.sides.put(side, access);
      this.setChanged();
   }

   @Override
   public Map<WorksiteRelativeSide, WorksiteInventoryAccess> getSideAccessMap() {
      return Map.copyOf(this.sides);
   }

   private WorksiteRelativeSide relativeSide(Direction side) {
      if (side == Direction.UP) {
         return WorksiteRelativeSide.TOP;
      } else if (side == Direction.DOWN) {
         return WorksiteRelativeSide.BOTTOM;
      } else {
         Direction front = (Direction)this.getBlockState().getValue(MailboxBlock.FACING);
         if (side == front) {
            return WorksiteRelativeSide.FRONT;
         } else if (side == front.getOpposite()) {
            return WorksiteRelativeSide.BACK;
         } else {
            return side == front.getCounterClockWise() ? WorksiteRelativeSide.RIGHT : WorksiteRelativeSide.LEFT;
         }
      }
   }

   public WorksiteInventoryAccess getInventoryAccess(Direction side) {
      return side == null ? WorksiteInventoryAccess.NONE : this.getSideAccess(this.relativeSide(side));
   }

   @Nullable
   @Override
   public ItemHandlerAccess getItemHandler(@Nullable Direction side) {
      return side == null ? null : new MailboxBlockEntity.SidedItemHandler(side);
   }

   private ItemStack insert(NonNullList<ItemStack> items, int slot, ItemStack stack, boolean simulate) {
      if (valid(slot, items) && !stack.isEmpty()) {
         ItemStack existing = (ItemStack)items.get(slot);
         if (!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing, stack)) {
            return stack;
         } else {
            int limit = Math.min(64, stack.getMaxStackSize());
            int moved = Math.min(stack.getCount(), existing.isEmpty() ? limit : limit - existing.getCount());
            if (moved <= 0) {
               return stack;
            } else {
               if (!simulate) {
                  if (existing.isEmpty()) {
                     items.set(slot, stack.copyWithCount(moved));
                  } else {
                     existing.grow(moved);
                  }

                  this.setChanged();
               }

               ItemStack remaining = stack.copy();
               remaining.shrink(moved);
               return remaining;
            }
         }
      } else {
         return stack;
      }
   }

   private ItemStack extract(NonNullList<ItemStack> items, int slot, int amount, boolean simulate) {
      if (valid(slot, items) && amount > 0) {
         ItemStack existing = (ItemStack)items.get(slot);
         if (existing.isEmpty()) {
            return ItemStack.EMPTY;
         } else {
            ItemStack result = existing.copyWithCount(Math.min(amount, existing.getCount()));
            if (!simulate) {
               existing.shrink(result.getCount());
               if (existing.isEmpty()) {
                  items.set(slot, ItemStack.EMPTY);
               }

               this.setChanged();
            }

            return result;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   private static boolean valid(int slot, NonNullList<ItemStack> items) {
      return slot >= 0 && slot < items.size();
   }

   private final class MailboxContainer implements Container {
      private final NonNullList<ItemStack> items;

      private MailboxContainer(NonNullList<ItemStack> items) {
         this.items = items;
      }

      public int getContainerSize() {
         return this.items.size();
      }

      public boolean isEmpty() {
         return this.items.stream().allMatch(ItemStack::isEmpty);
      }

      public ItemStack getItem(int slot) {
         return MailboxBlockEntity.valid(slot, this.items) ? (ItemStack)this.items.get(slot) : ItemStack.EMPTY;
      }

      public ItemStack removeItem(int slot, int amount) {
         return MailboxBlockEntity.this.extract(this.items, slot, amount, false);
      }

      public ItemStack removeItemNoUpdate(int slot) {
         if (!MailboxBlockEntity.valid(slot, this.items)) {
            return ItemStack.EMPTY;
         } else {
            ItemStack result = (ItemStack)this.items.get(slot);
            this.items.set(slot, ItemStack.EMPTY);
            return result;
         }
      }

      public void setItem(int slot, ItemStack stack) {
         if (MailboxBlockEntity.valid(slot, this.items)) {
            this.items.set(slot, stack);
            this.setChanged();
         }
      }

      public void setChanged() {
         MailboxBlockEntity.this.setChanged();
      }

      public boolean stillValid(Player player) {
         return MailboxBlockEntity.this.stillValid(player);
      }

      public void clearContent() {
         this.items.replaceAll(ignored -> ItemStack.EMPTY);
         this.setChanged();
      }
   }

   private final class SidedItemHandler implements ItemHandlerAccess {
      private final Direction side;

      private SidedItemHandler(Direction side) {
         this.side = side;
      }

      private NonNullList<ItemStack> items() {
         return MailboxBlockEntity.this.getSideAccess(MailboxBlockEntity.this.relativeSide(this.side)) == WorksiteInventoryAccess.OUTPUT
            ? MailboxBlockEntity.this.send
            : MailboxBlockEntity.this.received;
      }

      @Override
      public int getSlots() {
         return 18;
      }

      @Override
      public ItemStack getStackInSlot(int slot) {
         NonNullList<ItemStack> items = this.items();
         return MailboxBlockEntity.valid(slot, items) ? (ItemStack)items.get(slot) : ItemStack.EMPTY;
      }

      @Override
      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         return MailboxBlockEntity.this.getSideAccess(MailboxBlockEntity.this.relativeSide(this.side)) == WorksiteInventoryAccess.INPUT
            ? MailboxBlockEntity.this.insert(MailboxBlockEntity.this.received, slot, stack, simulate)
            : stack;
      }

      @Override
      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         return MailboxBlockEntity.this.getSideAccess(MailboxBlockEntity.this.relativeSide(this.side)) == WorksiteInventoryAccess.OUTPUT
            ? MailboxBlockEntity.this.extract(MailboxBlockEntity.this.send, slot, amount, simulate)
            : ItemStack.EMPTY;
      }

      @Override
      public int getSlotLimit(int slot) {
         return 64;
      }

      @Override
      public boolean isItemValid(int slot, ItemStack stack) {
         return MailboxBlockEntity.valid(slot, MailboxBlockEntity.this.received)
            && MailboxBlockEntity.this.getSideAccess(MailboxBlockEntity.this.relativeSide(this.side)) == WorksiteInventoryAccess.INPUT;
      }
   }
}
