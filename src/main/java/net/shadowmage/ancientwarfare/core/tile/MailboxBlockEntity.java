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
   private final NonNullList<ItemStack> send = NonNullList.m_122780_(18, ItemStack.f_41583_);
   private final NonNullList<ItemStack> received = NonNullList.m_122780_(18, ItemStack.f_41583_);
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
      this.m_6596_();
   }

   public void setTargetName(String target) {
      this.target = target == null ? "" : target.trim();
      this.m_6596_();
   }

   public void setPrivateBox(boolean privateBox) {
      this.privateBox = privateBox;
      this.name = "";
      this.target = "";
      this.m_6596_();
   }

   public void setAutoExport(boolean autoExport) {
      this.autoExport = autoExport;
      this.m_6596_();
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
      this.m_6596_();
   }

   public String getOwnerNamePublic() {
      return this.getOwnerName();
   }

   public ItemStack insertReceived(ItemStack stack) {
      ItemStack remaining = stack;

      for (int slot = 0; slot < 18 && !remaining.m_41619_(); slot++) {
         remaining = this.insert(this.send, slot, remaining, false);
      }

      this.m_6596_();
      return remaining;
   }

   public Container sendContainer() {
      return new MailboxBlockEntity.MailboxContainer(this.send);
   }

   public Container receivedContainer() {
      return new MailboxBlockEntity.MailboxContainer(this.received);
   }

   public boolean stillValid(Player player) {
      return this.f_58857_ != null
         && this.f_58857_.m_7702_(this.f_58858_) == this
         && player.m_20275_((double)this.f_58858_.m_123341_() + 0.5, (double)this.f_58858_.m_123342_() + 0.5, (double)this.f_58858_.m_123343_() + 0.5) < 64.0;
   }

   public void sendNow() {
      if (this.f_58857_ instanceof ServerLevel serverLevel && this.target != null && !this.target.isBlank()) {
         for (int slot = 0; slot < 18; slot++) {
            ItemStack stack = (ItemStack)this.received.get(slot);
            if (!stack.m_41619_()) {
               ItemStack remaining = MailboxData.get(serverLevel).deliver(this, this.target, stack.m_41777_());
               int moved = stack.m_41613_() - remaining.m_41613_();
               if (moved > 0) {
                  this.extract(this.received, slot, moved, false);
               }
            }
         }

         return;
      }
   }

   public Component m_5446_() {
      return Component.m_237115_("container.ancientwarfare.mailbox");
   }

   public AbstractContainerMenu m_7208_(int containerId, Inventory inventory, Player player) {
      return new MailboxMenu(containerId, inventory, this);
   }

   public CompoundTag m_5995_() {
      CompoundTag tag = new CompoundTag();
      this.m_183515_(tag);
      return tag;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.m_195640_(this);
   }

   public void handleUpdateTag(CompoundTag tag) {
      this.m_142466_(tag);
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      tag.m_128365_("Send", saveItems(this.send));
      tag.m_128365_("Received", saveItems(this.received));
      tag.m_128359_("Name", this.name);
      tag.m_128359_("Target", this.target);
      tag.m_128379_("Private", this.privateBox);
      tag.m_128379_("Auto", this.autoExport);
      tag.m_128359_("Owner", this.getOwnerName());
      CompoundTag sideTag = new CompoundTag();
      this.sides.forEach((side, access) -> sideTag.m_128359_(side.name(), access.name()));
      tag.m_128365_("Sides", sideTag);
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      if (tag.m_128441_("Send")) {
         ContainerHelper.m_18980_(tag.m_128469_("Send"), this.send);
      }

      if (tag.m_128441_("Received")) {
         ContainerHelper.m_18980_(tag.m_128469_("Received"), this.received);
      }

      this.name = tag.m_128461_("Name");
      this.target = tag.m_128461_("Target");
      this.privateBox = tag.m_128471_("Private");
      this.autoExport = tag.m_128471_("Auto");
      this.setOwnerName(tag.m_128461_("Owner"));
      if (tag.m_128441_("Sides")) {
         CompoundTag sideTag = tag.m_128469_("Sides");

         for (WorksiteRelativeSide side : WorksiteRelativeSide.values()) {
            if (sideTag.m_128441_(side.name())) {
               try {
                  this.sides.put(side, WorksiteInventoryAccess.valueOf(sideTag.m_128461_(side.name())));
               } catch (IllegalArgumentException var8) {
                  this.sides.put(side, WorksiteInventoryAccess.NONE);
               }
            }
         }
      }
   }

   private static CompoundTag saveItems(NonNullList<ItemStack> items) {
      CompoundTag tag = new CompoundTag();
      ContainerHelper.m_18973_(tag, items);
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
      this.m_6596_();
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
         Direction front = (Direction)this.m_58900_().m_61143_(MailboxBlock.FACING);
         if (side == front) {
            return WorksiteRelativeSide.FRONT;
         } else if (side == front.m_122424_()) {
            return WorksiteRelativeSide.BACK;
         } else {
            return side == front.m_122428_() ? WorksiteRelativeSide.RIGHT : WorksiteRelativeSide.LEFT;
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
      if (valid(slot, items) && !stack.m_41619_()) {
         ItemStack existing = (ItemStack)items.get(slot);
         if (!existing.m_41619_() && !ItemStack.m_150942_(existing, stack)) {
            return stack;
         } else {
            int limit = Math.min(64, stack.m_41741_());
            int moved = Math.min(stack.m_41613_(), existing.m_41619_() ? limit : limit - existing.m_41613_());
            if (moved <= 0) {
               return stack;
            } else {
               if (!simulate) {
                  if (existing.m_41619_()) {
                     items.set(slot, stack.m_255036_(moved));
                  } else {
                     existing.m_41769_(moved);
                  }

                  this.m_6596_();
               }

               ItemStack remaining = stack.m_41777_();
               remaining.m_41774_(moved);
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
         if (existing.m_41619_()) {
            return ItemStack.f_41583_;
         } else {
            ItemStack result = existing.m_255036_(Math.min(amount, existing.m_41613_()));
            if (!simulate) {
               existing.m_41774_(result.m_41613_());
               if (existing.m_41619_()) {
                  items.set(slot, ItemStack.f_41583_);
               }

               this.m_6596_();
            }

            return result;
         }
      } else {
         return ItemStack.f_41583_;
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

      public int m_6643_() {
         return this.items.size();
      }

      public boolean m_7983_() {
         return this.items.stream().allMatch(ItemStack::m_41619_);
      }

      public ItemStack m_8020_(int slot) {
         return MailboxBlockEntity.valid(slot, this.items) ? (ItemStack)this.items.get(slot) : ItemStack.f_41583_;
      }

      public ItemStack m_7407_(int slot, int amount) {
         return MailboxBlockEntity.this.extract(this.items, slot, amount, false);
      }

      public ItemStack m_8016_(int slot) {
         if (!MailboxBlockEntity.valid(slot, this.items)) {
            return ItemStack.f_41583_;
         } else {
            ItemStack result = (ItemStack)this.items.get(slot);
            this.items.set(slot, ItemStack.f_41583_);
            return result;
         }
      }

      public void m_6836_(int slot, ItemStack stack) {
         if (MailboxBlockEntity.valid(slot, this.items)) {
            this.items.set(slot, stack);
            this.m_6596_();
         }
      }

      public void m_6596_() {
         MailboxBlockEntity.this.m_6596_();
      }

      public boolean m_6542_(Player player) {
         return MailboxBlockEntity.this.stillValid(player);
      }

      public void m_6211_() {
         this.items.replaceAll(ignored -> ItemStack.f_41583_);
         this.m_6596_();
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
         return MailboxBlockEntity.valid(slot, items) ? (ItemStack)items.get(slot) : ItemStack.f_41583_;
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
            : ItemStack.f_41583_;
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
