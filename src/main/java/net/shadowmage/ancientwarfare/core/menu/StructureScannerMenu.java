package net.shadowmage.ancientwarfare.core.menu;

import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public class StructureScannerMenu extends AbstractContainerMenu {
   private final BlockPos pos;
   private final BlockPos firstPos;
   private final BlockPos secondPos;
   private final BlockPos buildOrigin;
   private final List<String> templateNames;

   public StructureScannerMenu(int id, Inventory inventory, BlockPos pos) {
      this(id, inventory, pos, scannerState(inventory), List.of());
   }

   public StructureScannerMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
      this(
         id,
         inventory,
         buffer.readBlockPos(),
         new StructureScannerMenu.ScannerState(readOptionalPos(buffer), readOptionalPos(buffer), readOptionalPos(buffer)),
         readNames(buffer)
      );
   }

   private StructureScannerMenu(int id, Inventory inventory, BlockPos pos, StructureScannerMenu.ScannerState state, List<String> names) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.STRUCTURE_SCANNER), id);
      this.pos = pos;
      this.firstPos = state.first();
      this.secondPos = state.second();
      this.buildOrigin = state.origin();
      this.templateNames = names;
   }

   private static StructureScannerMenu.ScannerState scannerState(Inventory inventory) {
      ItemStack scanner = inventory.player.getMainHandItem().is(AWCoreRegistryAccess.item(AWCoreIds.STRUCTURE_SCANNER))
         ? inventory.player.getMainHandItem()
         : inventory.player.getOffhandItem();
      CompoundTag tag = scanner.getTag();
      return new StructureScannerMenu.ScannerState(optionalPos(tag, "FirstPos"), optionalPos(tag, "SecondPos"), optionalPos(tag, "BuildOrigin"));
   }

   private static BlockPos optionalPos(CompoundTag tag, String key) {
      return tag != null && tag.contains(key) ? BlockPos.of(tag.getLong(key)) : null;
   }

   private static BlockPos readOptionalPos(FriendlyByteBuf buffer) {
      return buffer.readBoolean() ? buffer.readBlockPos() : null;
   }

   private static List<String> readNames(FriendlyByteBuf buffer) {
      int count = Math.min(512, buffer.readVarInt());
      return Collections.nCopies(count, "");
   }

   public static void writeOptionalPos(FriendlyByteBuf buffer, BlockPos pos) {
      buffer.writeBoolean(pos != null);
      if (pos != null) {
         buffer.writeBlockPos(pos);
      }
   }

   public BlockPos pos() {
      return this.pos;
   }

   public BlockPos firstPos() {
      return this.firstPos;
   }

   public BlockPos secondPos() {
      return this.secondPos;
   }

   public BlockPos buildOrigin() {
      return this.buildOrigin;
   }

   public List<String> templateNames() {
      return this.templateNames;
   }

   public boolean stillValid(Player player) {
      return stillValid(ContainerLevelAccess.create(player.level(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.STRUCTURE_SCANNER_BLOCK));
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }

   public static record ScannerState(BlockPos first, BlockPos second, BlockPos origin) {
   }
}
