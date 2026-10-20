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
import net.shadowmage.ancientwarfare.core.entity.DragonRiderController;
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
      this(containerId, playerInventory, findNpc(playerInventory.f_35978_, entityId));
   }

   public NpcInventoryMenu(int containerId, Inventory playerInventory, PlayerOwnedNpcEntity npc) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.NPC_INVENTORY), containerId);
      this.npc = npc;
      if (npc != null && !playerInventory.f_35978_.m_9236_().f_46443_) {
         npc.beginGuiInteraction(playerInventory.f_35978_);
         this.guiLockHeld = npc.isGuiInteractionPaused();
      }

      this.m_38897_(new NpcInventoryMenu.EquipmentItemSlot(npc, EquipmentSlot.MAINHAND, 8, 8));
      this.m_38897_(new NpcInventoryMenu.EquipmentItemSlot(npc, EquipmentSlot.OFFHAND, 8, 26));
      this.m_38897_(new NpcInventoryMenu.ArmorEquipmentSlot(npc, EquipmentSlot.FEET, 8, 98));
      this.m_38897_(new NpcInventoryMenu.ArmorEquipmentSlot(npc, EquipmentSlot.LEGS, 8, 80));
      this.m_38897_(new NpcInventoryMenu.ArmorEquipmentSlot(npc, EquipmentSlot.CHEST, 8, 62));
      this.m_38897_(new NpcInventoryMenu.ArmorEquipmentSlot(npc, EquipmentSlot.HEAD, 8, 44));
      this.m_38897_(new NpcInventoryMenu.NpcStoredStackSlot(npc, NpcInventoryMenu.StoredStackType.ORDERS, 44, 44));
      this.m_38897_(new NpcInventoryMenu.NpcStoredStackSlot(npc, NpcInventoryMenu.StoredStackType.UPKEEP, 44, 62));
      this.m_38897_(new NpcInventoryMenu.NpcStoredStackSlot(npc, NpcInventoryMenu.StoredStackType.FOOD, 44, 80));
      this.addPlayerInventory(playerInventory);
   }

   public Component getNpcDisplayName() {
      return (Component)(this.npc == null ? Component.m_237115_("entity.ancientwarfare.worker_npc") : this.npc.m_5446_());
   }

   public int getNpcEntityId() {
      return this.npc == null ? -1 : this.npc.m_19879_();
   }

   public String getSkinName() {
      return this.npc == null ? "" : this.npc.getSkinName();
   }

   public Component getSkinButtonLabel() {
      String skinName = this.getSkinName();
      return skinName.isBlank() ? Component.m_237115_("guistrings.npc.default_skin") : Component.m_237113_(skinName);
   }

   public boolean canRepack(Player player) {
      return this.npc != null && this.npc.m_6084_() && player != null && this.npc.hasCommandPermission(player);
   }

   public boolean canOpenCreativeGui(Player player) {
      return player != null && player.m_7500_();
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

   public boolean m_6366_(Player player, int id) {
      if (this.npc == null || !this.npc.m_6084_() || !this.npc.hasCommandPermission(player)) {
         return false;
      } else if (id == 0) {
         this.npc.toggleFollow(player);
         return true;
      } else if (id == 1) {
         if (this.npc.hasHome()) {
            this.npc.clearHome();
            player.m_5661_(Component.m_237115_("guistrings.npc.home_cleared"), true);
         } else {
            this.npc.setHome(this.npc.m_20183_());
            player.m_5661_(Component.m_237115_("guistrings.npc.home_set"), true);
         }

         return true;
      } else if (id == 2) {
         this.npc.clearHome();
         player.m_5661_(Component.m_237115_("guistrings.npc.home_cleared"), true);
         return true;
      } else if (id == 3) {
         Entity storedMount = this.npc.m_20202_();
         Entity logicalMount = DragonRiderController.getLogicalMount(this.npc);
         ItemStack repackedNpc = NpcSpawnerItem.createFromNpc(this.npc);
         if (!player.m_150109_().m_36054_(repackedNpc)) {
            player.m_36176_(repackedNpc, false);
         }

         player.f_36096_ = player.f_36095_;
         this.npc.m_8127_();
         this.npc.m_146870_();
         if (storedMount instanceof Mob && storedMount.m_6084_()) {
            storedMount.m_146870_();
         }

         if (logicalMount instanceof Mob && logicalMount.m_6084_()) {
            logicalMount.m_146870_();
         }

         player.m_5661_(Component.m_237115_("guistrings.npc.repacked"), true);
         return true;
      } else if (id == 4) {
         player.m_5661_(Component.m_237115_("guistrings.npc.not_ported_yet"), true);
         return true;
      } else if (id == 5 && this.npc instanceof BardNpcEntity bard) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, openedPlayer) -> new BardMusicMenu(containerId, inventory, bard),
               Component.m_237115_("container.ancientwarfare.bard_music")
            ),
            buffer -> BardMusicMenu.writeTracks(buffer, bard.m_19879_(), bard.getBardTracks())
         );
         return true;
      } else if (id == 6 && this.npc instanceof TraderNpcEntity trader && AWCoreRegistryAccess.is(trader.getOrdersStack(), AWCoreIds.TRADE_ORDER)) {
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, openedPlayer) -> new TradeOrderMenu(containerId, inventory, InteractionHand.MAIN_HAND, trader.m_19879_()),
               Component.m_237115_("container.ancientwarfare.trade_order")
            ),
            buffer -> {
               buffer.m_130068_(InteractionHand.MAIN_HAND);
               buffer.writeInt(trader.m_19879_());
            }
         );
         return true;
      } else {
         return false;
      }
   }

   public boolean m_6875_(Player player) {
      return this.npc != null && this.npc.m_6084_() && this.npc.m_20280_(player) < 64.0;
   }

   public void m_6877_(Player player) {
      super.m_6877_(player);
      if (this.guiLockHeld && this.npc != null) {
         this.npc.endGuiInteraction(player);
         this.guiLockHeld = false;
      }
   }

   public ItemStack m_7648_(Player player, int index) {
      ItemStack result = ItemStack.f_41583_;
      Slot slot = (Slot)this.f_38839_.get(index);
      if (slot != null && slot.m_6657_()) {
         ItemStack stack = slot.m_7993_();
         result = stack.m_41777_();
         if (index < 9) {
            if (!this.m_38903_(stack, 9, this.f_38839_.size(), true)) {
               return ItemStack.f_41583_;
            }
         } else if (isOrdersStack(stack)) {
            if (!this.m_38903_(stack, 6, 7, false)) {
               return ItemStack.f_41583_;
            }
         } else if (isUpkeepOrderStack(stack)) {
            if (!this.m_38903_(stack, 7, 8, false)) {
               return ItemStack.f_41583_;
            }
         } else if (isFoodStack(stack)) {
            if (!this.m_38903_(stack, 8, 9, false)) {
               return ItemStack.f_41583_;
            }
         } else if (!this.moveItemStackToEquipment(stack)) {
            return ItemStack.f_41583_;
         }

         if (stack.m_41619_()) {
            slot.m_5852_(ItemStack.f_41583_);
         } else {
            slot.m_6654_();
         }
      }

      return result;
   }

   private boolean moveItemStackToEquipment(ItemStack stack) {
      EquipmentSlot resolvedSlot = NpcEquipmentResolver.equipmentSlot(stack);
      if (resolvedSlot.m_20743_() == Type.ARMOR) {
         int target = switch (resolvedSlot) {
            case FEET -> 2;
            case LEGS -> 3;
            case CHEST -> 4;
            case HEAD -> 5;
            default -> -1;
         };
         if (target >= 0 && this.m_38903_(stack, target, target + 1, false)) {
            return true;
         }
      }

      return this.m_38903_(stack, 0, 2, false);
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
      return !stack.m_41619_() && PlayerOwnedNpcEntity.foodValue(stack) > 0;
   }

   private void addPlayerInventory(Inventory playerInventory) {
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.m_38897_(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 124 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.m_38897_(new Slot(playerInventory, col, 8 + col * 18, 182));
      }
   }

   private static PlayerOwnedNpcEntity findNpc(Player player, int entityId) {
      return player.m_9236_().m_6815_(entityId) instanceof PlayerOwnedNpcEntity npc ? npc : null;
   }

   private static class ArmorEquipmentSlot extends NpcInventoryMenu.EquipmentItemSlot {
      private final EquipmentSlot armorSlot;

      private ArmorEquipmentSlot(PlayerOwnedNpcEntity npc, EquipmentSlot equipmentSlot, int x, int y) {
         super(npc, equipmentSlot, x, y);
         this.armorSlot = equipmentSlot;
      }

      @Override
      public boolean m_5857_(ItemStack stack) {
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

      public ItemStack m_7993_() {
         return this.npc == null ? ItemStack.f_41583_ : this.npc.m_6844_(this.equipmentSlot);
      }

      public void m_5852_(ItemStack stack) {
         if (this.npc != null) {
            this.npc.m_8061_(this.equipmentSlot, stack);
         }

         this.m_6654_();
      }

      public boolean m_6657_() {
         return !this.m_7993_().m_41619_();
      }

      public int m_6641_() {
         return 1;
      }

      public ItemStack m_6201_(int amount) {
         ItemStack current = this.m_7993_();
         if (!current.m_41619_() && amount > 0) {
            ItemStack removed = current.m_41620_(amount);
            if (current.m_41619_()) {
               this.m_5852_(ItemStack.f_41583_);
            } else {
               this.m_6654_();
            }

            return removed;
         } else {
            return ItemStack.f_41583_;
         }
      }

      public boolean m_5857_(ItemStack stack) {
         return !stack.m_41619_();
      }

      public boolean m_8010_(Player player) {
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

      public ItemStack m_7993_() {
         if (this.npc == null) {
            return ItemStack.f_41583_;
         } else {
            return switch (this.type) {
               case ORDERS -> this.npc.getOrdersStack();
               case UPKEEP -> this.npc.getUpkeepStack();
               case FOOD -> this.npc.getFoodStack();
            };
         }
      }

      public void m_5852_(ItemStack stack) {
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

         this.m_6654_();
      }

      public boolean m_6657_() {
         return !this.m_7993_().m_41619_();
      }

      public int m_6641_() {
         return this.type == NpcInventoryMenu.StoredStackType.FOOD ? 64 : 1;
      }

      public int m_5866_(ItemStack stack) {
         return this.type == NpcInventoryMenu.StoredStackType.FOOD ? Math.min(64, stack.m_41741_()) : 1;
      }

      public ItemStack m_6201_(int amount) {
         ItemStack current = this.m_7993_();
         if (!current.m_41619_() && amount > 0) {
            ItemStack removed = current.m_41620_(amount);
            if (current.m_41619_()) {
               this.m_5852_(ItemStack.f_41583_);
            } else {
               this.m_5852_(current);
            }

            return removed;
         } else {
            return ItemStack.f_41583_;
         }
      }

      public boolean m_5857_(ItemStack stack) {
         return switch (this.type) {
            case ORDERS -> NpcInventoryMenu.isOrdersStack(stack);
            case UPKEEP -> NpcInventoryMenu.isUpkeepOrderStack(stack);
            case FOOD -> PlayerOwnedNpcEntity.foodValue(stack) > 0;
         };
      }

      public boolean m_8010_(Player player) {
         return true;
      }
   }

   private static enum StoredStackType {
      ORDERS,
      UPKEEP,
      FOOD;
   }
}
