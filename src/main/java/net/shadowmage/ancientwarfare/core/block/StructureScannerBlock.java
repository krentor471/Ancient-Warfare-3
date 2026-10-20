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

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!level.f_46443_ && player instanceof ServerPlayer serverPlayer) {
         MenuProvider provider = new SimpleMenuProvider((id, inventory, ignored) -> new StructureScannerMenu(id, inventory, pos), this.m_49954_());
         AWCorePlatform.openMenu(
            serverPlayer,
            provider,
            buffer -> {
               buffer.m_130064_(pos);
               ItemStack scanner = serverPlayer.m_21205_().m_150930_(AWCoreRegistryAccess.item(AWCoreIds.STRUCTURE_SCANNER))
                  ? serverPlayer.m_21205_()
                  : serverPlayer.m_21206_();
               CompoundTag tag = scanner.m_41783_();
               StructureScannerMenu.writeOptionalPos(buffer, tag != null && tag.m_128441_("FirstPos") ? BlockPos.m_122022_(tag.m_128454_("FirstPos")) : null);
               StructureScannerMenu.writeOptionalPos(buffer, tag != null && tag.m_128441_("SecondPos") ? BlockPos.m_122022_(tag.m_128454_("SecondPos")) : null);
               StructureScannerMenu.writeOptionalPos(
                  buffer, tag != null && tag.m_128441_("BuildOrigin") ? BlockPos.m_122022_(tag.m_128454_("BuildOrigin")) : null
               );
               List<PlayerStructureTemplateData.PlayerTemplate> templates = PlayerStructureTemplateData.get(serverPlayer.m_284548_()).templates();
               buffer.m_130130_(Math.min(512, templates.size()));
            }
         );
      }

      return InteractionResult.m_19078_(level.f_46443_);
   }
}
