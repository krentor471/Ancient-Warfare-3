package net.shadowmage.ancientwarfare.core.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.menu.DraftingStationMenu;
import net.shadowmage.ancientwarfare.core.tile.DraftingStationBlockEntity;
import org.jetbrains.annotations.Nullable;

public class DraftingStationBlock extends BaseEntityBlock {
   public DraftingStationBlock(Properties properties) {
      super(properties);
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new DraftingStationBlockEntity(pos, state);
   }

   public RenderShape m_7514_(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      return level.f_46443_ ? null : m_152132_(type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.DRAFTING_STATION), DraftingStationBlockEntity::serverTick);
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!level.f_46443_ && player instanceof ServerPlayer serverPlayer && level.m_7702_(pos) instanceof DraftingStationBlockEntity station) {
         List<DraftingStationMenu.TemplateSummary> templates = DraftingStationMenu.serverTemplateSummaries(serverPlayer.m_284548_());
         AWCorePlatform.openMenu(
            serverPlayer,
            station,
            buffer -> {
               buffer.m_130064_(pos);
               buffer.m_130130_(templates.size());
               templates.forEach(template -> {
                  buffer.m_130072_(template.id(), 256);
                  buffer.m_130072_(template.label(), 128);
                  buffer.m_130130_(template.materials().size());
                  template.materials().forEach(resource -> {
                     buffer.m_130130_(BuiltInRegistries.f_257033_.m_7447_((Item)BuiltInRegistries.f_257033_.m_7745_(resource.itemId())));
                     buffer.m_130130_(resource.count());
                  });
               });
               buffer.m_130072_(station.selectedTemplate(), 256);
               buffer.m_130130_(templates.size());
               templates.forEach(
                  template -> {
                     buffer.m_130130_(template.materials().size());
                     template.materials()
                        .forEach(material -> buffer.m_130130_(template.id().equals(station.selectedTemplate()) ? station.suppliedCount(material.itemId()) : 0));
                  }
               );
            }
         );
      }

      return InteractionResult.m_19078_(level.f_46443_);
   }

   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState next, boolean piston) {
      if (!state.m_60713_(next.m_60734_()) && level.m_7702_(pos) instanceof DraftingStationBlockEntity station) {
         Containers.m_19002_(level, pos, station);
      }

      super.m_6810_(state, level, pos, next, piston);
   }
}
