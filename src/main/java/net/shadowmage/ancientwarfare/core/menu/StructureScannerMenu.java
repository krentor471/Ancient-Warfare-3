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
         buffer.m_130135_(),
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
      ItemStack scanner = inventory.f_35978_.m_21205_().m_150930_(AWCoreRegistryAccess.item(AWCoreIds.STRUCTURE_SCANNER))
         ? inventory.f_35978_.m_21205_()
         : inventory.f_35978_.m_21206_();
      CompoundTag tag = scanner.m_41783_();
      return new StructureScannerMenu.ScannerState(optionalPos(tag, "FirstPos"), optionalPos(tag, "SecondPos"), optionalPos(tag, "BuildOrigin"));
   }

   private static BlockPos optionalPos(CompoundTag tag, String key) {
      return tag != null && tag.m_128441_(key) ? BlockPos.m_122022_(tag.m_128454_(key)) : null;
   }

   private static BlockPos readOptionalPos(FriendlyByteBuf buffer) {
      return buffer.readBoolean() ? buffer.m_130135_() : null;
   }

   private static List<String> readNames(FriendlyByteBuf buffer) {
      int count = Math.min(512, buffer.m_130242_());
      return Collections.nCopies(count, "");
   }

   public static void writeOptionalPos(FriendlyByteBuf buffer, BlockPos pos) {
      buffer.writeBoolean(pos != null);
      if (pos != null) {
         buffer.m_130064_(pos);
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

   public boolean m_6875_(Player player) {
      return m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.STRUCTURE_SCANNER_BLOCK));
   }

   public ItemStack m_7648_(Player player, int index) {
      return ItemStack.f_41583_;
   }

   public static record ScannerState(BlockPos first, BlockPos second, BlockPos origin) {
   }
}
