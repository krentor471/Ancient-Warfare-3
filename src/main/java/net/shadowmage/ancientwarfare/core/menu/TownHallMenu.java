package net.shadowmage.ancientwarfare.core.menu;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.server.level.ServerLevel;
import net.shadowmage.ancientwarfare.core.faction.FactionLife;
import net.shadowmage.ancientwarfare.core.faction.FactionWorldData;
import net.shadowmage.ancientwarfare.core.faction.PlayerCountry;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.tile.TownHallBlockEntity;

public class TownHallMenu extends AbstractContainerMenu {
   private static final int TOWN_HALL_SLOTS = 27;
   private final Container container;
   private final BlockPos pos;
   /**
    * For a village with a state (taken, or in a country): whether it has one, loyalty, order, tax level,
    * guards, silver a day; then its country standing (see VillageData), residents, the country's
    * provinces and its income on the last tax day.
    */
   private final ContainerData village;

   public TownHallMenu(int containerId, Inventory playerInventory, BlockPos pos) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.TOWN_HALL_MENU), containerId);
      this.pos = pos;
      Level level = playerInventory.player.level();
      if (level.getBlockEntity(pos) instanceof TownHallBlockEntity townHall) {
         this.container = townHall;
      } else {
         this.container = new SimpleContainer(27);
      }

      this.village = level instanceof ServerLevel serverLevel ? new VillageData(serverLevel, pos) : new SimpleContainerData(VillageData.COUNT);
      this.addDataSlots(this.village);
      this.container.startOpen(playerInventory.player);
      this.addSlotGrid(this.container, 0, 8, 22, 9, 3);
      this.addPlayerInventory(playerInventory, 8, 92);
   }

   public ContainerData village() {
      return this.village;
   }

   public BlockPos pos() {
      return this.pos;
   }

   /** Buttons 0 to 2 set the tax of a taken village, for its owner only. */
   @Override
   public boolean clickMenuButton(Player player, int id) {
      if (id < 0 || id > 2 || !(player.level() instanceof ServerLevel level)) {
         return false;
      }

      FactionWorldData data = FactionWorldData.get(level);
      FactionWorldData.VillageState state = data.villageState(this.pos);
      if (state == null || !player.getUUID().equals(data.villages().get(this.pos))) {
         return false;
      }

      state.taxLevel = id;
      if (data.country(player.getUUID()) != null) {
         PlayerCountry.setTaxLevel(data, player.getUUID(), id);
      }

      data.setDirty();
      return true;
   }

   private static final class VillageData implements ContainerData {
      static final int COUNT = 10;
      /** Index 6: the hall's owner has no country and may found one here. */
      static final int MAY_FOUND = 1;
      static final int CAPITAL = 2;
      static final int COUNTRY_VILLAGE = 3;
      private static final long RESIDENTS_TICKS = 20L;
      private final ServerLevel level;
      private final BlockPos pos;
      private int residents;
      private long residentsCounted = Long.MIN_VALUE / 2;

      VillageData(ServerLevel level, BlockPos pos) {
         this.level = level;
         this.pos = pos;
      }

      @Override
      public int get(int index) {
         FactionWorldData data = FactionWorldData.get(this.level);
         if (index >= 6) {
            return this.country(data, index);
         }

         FactionWorldData.VillageState state = data.villageState(this.pos);
         if (state == null) {
            return 0;
         }

         return switch (index) {
            case 0 -> 1;
            case 1 -> state.loyalty;
            case 2 -> state.order;
            case 3 -> state.taxLevel;
            case 4 -> state.guards;
            case 5 -> FactionLife.villageTax(data, this.pos);
            default -> 0;
         };
      }

      private int country(FactionWorldData data, int index) {
         java.util.UUID owner = data.villages().get(this.pos);
         if (owner == null) {
            return 0;
         }

         FactionWorldData.Country country = data.country(owner);
         return switch (index) {
            case 6 -> country == null ? MAY_FOUND : this.pos.equals(country.capital) ? CAPITAL : COUNTRY_VILLAGE;
            case 7 -> {
               if (this.level.getGameTime() - this.residentsCounted >= RESIDENTS_TICKS) {
                  this.residents = PlayerCountry.residents(this.level, data, this.pos, owner);
                  this.residentsCounted = this.level.getGameTime();
               }

               yield this.residents;
            }
            case 8 -> country == null ? 0 : data.provincesOf(PlayerCountry.land(owner)).size();
            case 9 -> country == null ? 0 : country.income;
            default -> 0;
         };
      }

      @Override
      public void set(int index, int value) {
      }

      @Override
      public int getCount() {
         return COUNT;
      }
   }

   public List<TownHallBlockEntity.NpcDeathEntry> getDeathNotices() {
      return this.container instanceof TownHallBlockEntity townHall ? townHall.getDeathNotices() : List.of();
   }

   private void addSlotGrid(Container container, int firstSlot, int x, int y, int columns, int rows) {
      for (int row = 0; row < rows; row++) {
         for (int col = 0; col < columns; col++) {
            int index = firstSlot + row * columns + col;
            if (index < container.getContainerSize()) {
               this.addSlot(new Slot(container, index, x + col * 18, y + row * 18));
            }
         }
      }
   }

   private void addPlayerInventory(Inventory playerInventory, int x, int y) {
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col + row * 9 + 9, x + col * 18, y + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.addSlot(new Slot(playerInventory, col, x + col * 18, y + 58));
      }
   }

   public boolean stillValid(Player player) {
      return stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.TOWN_HALL));
   }

   public ItemStack quickMoveStack(Player player, int index) {
      if (index >= 0 && index < this.slots.size()) {
         Slot slot = (Slot)this.slots.get(index);
         if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            if (index < 27) {
               if (!this.moveItemStackTo(stack, 27, this.slots.size(), true)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(stack, 0, 27, false)) {
               return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
               slot.setByPlayer(ItemStack.EMPTY);
            } else {
               slot.setChanged();
            }

            if (stack.getCount() == original.getCount()) {
               return ItemStack.EMPTY;
            } else {
               slot.onTake(player, stack);
               this.broadcastChanges();
               return original;
            }
         } else {
            return ItemStack.EMPTY;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   public void removed(Player player) {
      super.removed(player);
      this.container.stopOpen(player);
   }
}
