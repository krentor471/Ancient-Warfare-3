package net.shadowmage.ancientwarfare.core.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.npc.NpcEquipmentResolver;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.entity.BardNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.PlayerOwnedNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.TraderNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.WorkerNpcEntity;
import net.shadowmage.ancientwarfare.core.item.NpcSpawnerItem;

public class NpcInventoryMenu extends AbstractContainerMenu {
   public static final int BUTTON_TOGGLE_FOLLOW = 0;
   public static final int BUTTON_SET_HOME = 1;
   public static final int BUTTON_CLEAR_HOME = 2;
   public static final int BUTTON_REPACK = 3;
   public static final int BUTTON_CREATIVE_GUI = 4;
   public static final int BUTTON_ADVANCED_GUI = 5;
   public static final int BUTTON_TRADE_ORDER_GUI = 6;
   private static final int EQUIPMENT_SLOTS = 6;
   private static final int ORDER_SLOTS = 3;
   private static final int NPC_SLOTS = 9;
   private static final int PLAYER_INV_TOP = 124;
   private final PlayerOwnedNpcEntity npc;
   private boolean guiLockHeld;

   public NpcInventoryMenu(int containerId, Inventory playerInventory, int entityId) {
      this(containerId, playerInventory, findNpc(playerInventory.player, entityId));
   }

   public NpcInventoryMenu(int containerId, Inventory playerInventory, PlayerOwnedNpcEntity npc) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.NPC_INVENTORY), containerId);
      this.npc = npc;
      if (npc != null && !playerInventory.player.level().isClientSide) {
         npc.beginGuiInteraction(playerInventory.player);
         this.guiLockHeld = npc.isGuiInteractionPaused();
      }

      this.addSlot(new NpcInventoryMenu.EquipmentItemSlot(npc, EquipmentSlot.MAINHAND, 8, 8));
      this.addSlot(new NpcInventoryMenu.EquipmentItemSlot(npc, EquipmentSlot.OFFHAND, 8, 26));
      this.addSlot(new NpcInventoryMenu.ArmorEquipmentSlot(npc, EquipmentSlot.FEET, 8, 98));
      this.addSlot(new NpcInventoryMenu.ArmorEquipmentSlot(npc, EquipmentSlot.LEGS, 8, 80));
      this.addSlot(new NpcInventoryMenu.ArmorEquipmentSlot(npc, EquipmentSlot.CHEST, 8, 62));
      this.addSlot(new NpcInventoryMenu.ArmorEquipmentSlot(npc, EquipmentSlot.HEAD, 8, 44));
      this.addSlot(new NpcInventoryMenu.NpcStoredStackSlot(npc, NpcInventoryMenu.StoredStackType.ORDERS, 44, 44));
      this.addSlot(new NpcInventoryMenu.NpcStoredStackSlot(npc, NpcInventoryMenu.StoredStackType.UPKEEP, 44, 62));
      this.addSlot(new NpcInventoryMenu.NpcStoredStackSlot(npc, NpcInventoryMenu.StoredStackType.FOOD, 44, 80));
      this.addPlayerInventory(playerInventory);
   }

   public Component getNpcDisplayName() {
      return (Component)(this.npc == null ? Component.translatable("entity.ancientwarfare.worker_npc") : this.npc.getDisplayName());
   }

   public int getNpcEntityId() {
      return this.npc == null ? -1 : this.npc.getId();
   }

   public String getSkinName() {
      return this.npc == null ? "" : this.npc.getSkinName();
   }

   public Component getSkinButtonLabel() {
      String skinName = this.getSkinName();
      return skinName.isBlank() ? Component.translatable("guistrings.npc.default_skin") : Component.literal(skinName);
   }

   public boolean canRepack(Player player) {
      return this.npc != null && this.npc.isAlive() && player != null && this.npc.hasCommandPermission(player);
   }

   public boolean canOpenCreativeGui(Player player) {
      return player != null && player.isCreative();
   }

   public boolean isAlexModel() {
      return this.npc != null && this.npc.isAlexModel();
   }

   public boolean isFollowing() {
      return this.npc != null && this.npc.getCurrentCommand() == NpcCommandType.FOLLOW;
   }

   public boolean hasHome() {
      return this.npc != null && this.npc.hasHome();
   }

   public String getWorkState() {
      return this.npc instanceof WorkerNpcEntity worker ? worker.getWorkState() : "";
   }

   public boolean isBard() {
      return this.npc instanceof BardNpcEntity;
   }

   public boolean isCombatNpc() {
      return this.npc != null && this.npc.canReceiveAttackCommand();
   }

   public boolean canOpenTradeOrderGui() {
      return this.npc instanceof TraderNpcEntity && AWCoreRegistryAccess.is(this.npc.getOrdersStack(), AWCoreIds.TRADE_ORDER);
   }

   public boolean clickMenuButton(Player player, int id) {
      if (this.npc == null || !this.npc.isAlive() || !this.npc.hasCommandPermission(player)) {
         return false;
      } else if (id == 0) {
         this.npc.toggleFollow(player);
         return true;
      } else if (id == 1) {
         if (this.npc.hasHome()) {
            this.npc.clearHome();
            player.displayClientMessage(Component.translatable("guistrings.npc.home_cleared"), true);
         } else {
            this.npc.setHome(this.npc.blockPosition());
            player.displayClientMessage(Component.translatable("guistrings.npc.home_set"), true);
         }

         return true;
      } else if (id == 2) {
         this.npc.clearHome();
         player.displayClientMessage(Component.translatable("guistrings.npc.home_cleared"), true);
         return true;
      } else if (id == 3) {
         Entity storedMount = this.npc.getVehicle();
         ItemStack repackedNpc = NpcSpawnerItem.createFromNpc(this.npc);
         if (!player.getInventory().add(repackedNpc)) {
            player.drop(repackedNpc, false);
         }

         player.containerMenu = player.inventoryMenu;
         this.npc.stopRiding();
         this.npc.discard();
         if (storedMount instanceof Mob && storedMount.isAlive()) {
            storedMount.discard();
         }

         player.displayClientMessage(Component.translatable("guistrings.npc.repacked"), true);
         return true;
      } else if (id == 4) {
         player.displayClientMessage(Component.translatable("guistrings.npc.not_ported_yet"), true);
         return true;
      } else if (id == 5 && this.npc instanceof BardNpcEntity bard) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, openedPlayer) -> new BardMusicMenu(containerId, inventory, bard),
               Component.translatable("container.ancientwarfare.bard_music")
            ),
            buffer -> BardMusicMenu.writeTracks(buffer, bard.getId(), bard.getBardTracks())
         );
         return true;
      } else if (id == 6 && this.npc instanceof TraderNpcEntity trader && AWCoreRegistryAccess.is(trader.getOrdersStack(), AWCoreIds.TRADE_ORDER)) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, openedPlayer) -> new TradeOrderMenu(containerId, inventory, InteractionHand.MAIN_HAND, trader.getId()),
               Component.translatable("container.ancientwarfare.trade_order")
            ),
            buffer -> {
               buffer.writeEnum(InteractionHand.MAIN_HAND);
               buffer.writeInt(trader.getId());
            }
         );
         return true;
      } else {
         return false;
      }
   }

   public boolean stillValid(Player player) {
      return this.npc != null && this.npc.isAlive() && this.npc.distanceToSqr(player) < 64.0;
   }

   public void removed(Player player) {
      super.removed(player);
      if (this.guiLockHeld && this.npc != null) {
         this.npc.endGuiInteraction(player);
         this.guiLockHeld = false;
      }
   }

   public ItemStack quickMoveStack(Player player, int index) {
      ItemStack result = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(index);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         result = stack.copy();
         if (index < 9) {
            if (!this.moveItemStackTo(stack, 9, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (isOrdersStack(stack)) {
            if (!this.moveItemStackTo(stack, 6, 7, false)) {
               return ItemStack.EMPTY;
            }
         } else if (isUpkeepOrderStack(stack)) {
            if (!this.moveItemStackTo(stack, 7, 8, false)) {
               return ItemStack.EMPTY;
            }
         } else if (isFoodStack(stack)) {
            if (!this.moveItemStackTo(stack, 8, 9, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackToEquipment(stack)) {
            return ItemStack.EMPTY;
         }

         if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }
      }

      return result;
   }

   private boolean moveItemStackToEquipment(ItemStack stack) {
      EquipmentSlot resolvedSlot = NpcEquipmentResolver.equipmentSlot(stack);
      if (resolvedSlot.getType() == Type.ARMOR) {
         int target = switch (resolvedSlot) {
            case FEET -> 2;
            case LEGS -> 3;
            case CHEST -> 4;
            case HEAD -> 5;
            default -> -1;
         };
         if (target >= 0 && this.moveItemStackTo(stack, target, target + 1, false)) {
            return true;
         }
      }

      return this.moveItemStackTo(stack, 0, 2, false);
   }

   private static boolean isOrdersStack(ItemStack stack) {
      return AWCoreRegistryAccess.is(stack, AWCoreIds.WORK_ORDER)
         || AWCoreRegistryAccess.is(stack, AWCoreIds.ROUTING_ORDER)
         || AWCoreRegistryAccess.is(stack, AWCoreIds.TRADE_ORDER)
         || AWCoreRegistryAccess.is(stack, AWCoreIds.COMBAT_ORDER);
   }

   private static boolean isUpkeepOrderStack(ItemStack stack) {
      return AWCoreRegistryAccess.is(stack, AWCoreIds.UPKEEP_ORDER);
   }

   private static boolean isFoodStack(ItemStack stack) {
      return !stack.isEmpty() && PlayerOwnedNpcEntity.foodValue(stack) > 0;
   }

   private void addPlayerInventory(Inventory playerInventory) {
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 124 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 182));
      }
   }

   private static PlayerOwnedNpcEntity findNpc(Player player, int entityId) {
      return player.level().getEntity(entityId) instanceof PlayerOwnedNpcEntity npc ? npc : null;
   }

   private static class ArmorEquipmentSlot extends NpcInventoryMenu.EquipmentItemSlot {
      private final EquipmentSlot armorSlot;

      private ArmorEquipmentSlot(PlayerOwnedNpcEntity npc, EquipmentSlot equipmentSlot, int x, int y) {
         super(npc, equipmentSlot, x, y);
         this.armorSlot = equipmentSlot;
      }

      @Override
      public boolean mayPlace(ItemStack stack) {
         return NpcEquipmentResolver.equipmentSlot(stack) == this.armorSlot;
      }
   }

   private static class EquipmentItemSlot extends Slot {
      private static final Container EMPTY_CONTAINER = new SimpleContainer(0);
      private final PlayerOwnedNpcEntity npc;
      private final EquipmentSlot equipmentSlot;

      private EquipmentItemSlot(PlayerOwnedNpcEntity npc, EquipmentSlot equipmentSlot, int x, int y) {
         super(EMPTY_CONTAINER, 0, x, y);
         this.npc = npc;
         this.equipmentSlot = equipmentSlot;
      }

      public ItemStack getItem() {
         return this.npc == null ? ItemStack.EMPTY : this.npc.getItemBySlot(this.equipmentSlot);
      }

      public void set(ItemStack stack) {
         if (this.npc != null) {
            this.npc.setItemSlot(this.equipmentSlot, stack);
         }

         this.setChanged();
      }

      public boolean hasItem() {
         return !this.getItem().isEmpty();
      }

      public int getMaxStackSize() {
         return 1;
      }

      public ItemStack remove(int amount) {
         ItemStack current = this.getItem();
         if (!current.isEmpty() && amount > 0) {
            ItemStack removed = current.split(amount);
            if (current.isEmpty()) {
               this.set(ItemStack.EMPTY);
            } else {
               this.setChanged();
            }

            return removed;
         } else {
            return ItemStack.EMPTY;
         }
      }

      public boolean mayPlace(ItemStack stack) {
         return !stack.isEmpty();
      }

      public boolean mayPickup(Player player) {
         return true;
      }
   }

   private static class NpcStoredStackSlot extends Slot {
      private static final Container EMPTY_CONTAINER = new SimpleContainer(0);
      private final PlayerOwnedNpcEntity npc;
      private final NpcInventoryMenu.StoredStackType type;

      private NpcStoredStackSlot(PlayerOwnedNpcEntity npc, NpcInventoryMenu.StoredStackType type, int x, int y) {
         super(EMPTY_CONTAINER, 0, x, y);
         this.npc = npc;
         this.type = type;
      }

      public ItemStack getItem() {
         if (this.npc == null) {
            return ItemStack.EMPTY;
         } else {
            return switch (this.type) {
               case ORDERS -> this.npc.getOrdersStack();
               case UPKEEP -> this.npc.getUpkeepStack();
               case FOOD -> this.npc.getFoodStack();
            };
         }
      }

      public void set(ItemStack stack) {
         if (this.npc != null) {
            switch (this.type) {
               case ORDERS:
                  this.npc.setOrdersStack(stack);
                  break;
               case UPKEEP:
                  this.npc.setUpkeepStack(stack);
                  break;
               case FOOD:
                  this.npc.setFoodStack(stack);
            }
         }

         this.setChanged();
      }

      public boolean hasItem() {
         return !this.getItem().isEmpty();
      }

      public int getMaxStackSize() {
         return this.type == NpcInventoryMenu.StoredStackType.FOOD ? 64 : 1;
      }

      public int getMaxStackSize(ItemStack stack) {
         return this.type == NpcInventoryMenu.StoredStackType.FOOD ? Math.min(64, stack.getMaxStackSize()) : 1;
      }

      public ItemStack remove(int amount) {
         ItemStack current = this.getItem();
         if (!current.isEmpty() && amount > 0) {
            ItemStack removed = current.split(amount);
            if (current.isEmpty()) {
               this.set(ItemStack.EMPTY);
            } else {
               this.set(current);
            }

            return removed;
         } else {
            return ItemStack.EMPTY;
         }
      }

      public boolean mayPlace(ItemStack stack) {
         return switch (this.type) {
            case ORDERS -> NpcInventoryMenu.isOrdersStack(stack);
            case UPKEEP -> NpcInventoryMenu.isUpkeepOrderStack(stack);
            case FOOD -> PlayerOwnedNpcEntity.foodValue(stack) > 0;
         };
      }

      public boolean mayPickup(Player player) {
         return true;
      }
   }

   private static enum StoredStackType {
      ORDERS,
      UPKEEP,
      FOOD;
   }
}
