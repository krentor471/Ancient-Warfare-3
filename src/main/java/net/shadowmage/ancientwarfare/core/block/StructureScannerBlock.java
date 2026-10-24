package net.shadowmage.ancientwarfare.core.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.menu.StructureScannerMenu;
import net.shadowmage.ancientwarfare.core.structure.PlayerStructureTemplateData;

public class StructureScannerBlock extends Block {
   public StructureScannerBlock(Properties properties) {
      super(properties);
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
         MenuProvider provider = new SimpleMenuProvider((id, inventory, ignored) -> new StructureScannerMenu(id, inventory, pos), this.getName());
         AWCorePlatform.openMenu(
            serverPlayer,
            provider,
            buffer -> {
               buffer.writeBlockPos(pos);
               ItemStack scanner = serverPlayer.getMainHandItem().is(AWCoreRegistryAccess.item(AWCoreIds.STRUCTURE_SCANNER))
                  ? serverPlayer.getMainHandItem()
                  : serverPlayer.getOffhandItem();
               CompoundTag tag = scanner.getTag();
               StructureScannerMenu.writeOptionalPos(buffer, tag != null && tag.contains("FirstPos") ? BlockPos.of(tag.getLong("FirstPos")) : null);
               StructureScannerMenu.writeOptionalPos(buffer, tag != null && tag.contains("SecondPos") ? BlockPos.of(tag.getLong("SecondPos")) : null);
               StructureScannerMenu.writeOptionalPos(
                  buffer, tag != null && tag.contains("BuildOrigin") ? BlockPos.of(tag.getLong("BuildOrigin")) : null
               );
               List<PlayerStructureTemplateData.PlayerTemplate> templates = PlayerStructureTemplateData.get(serverPlayer.serverLevel()).templates();
               buffer.writeVarInt(Math.min(512, templates.size()));
            }
         );
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }
}
