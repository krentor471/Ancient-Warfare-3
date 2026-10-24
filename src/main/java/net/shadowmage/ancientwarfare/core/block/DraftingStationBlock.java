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
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new DraftingStationBlockEntity(pos, state);
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : createTickerHelper(type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.DRAFTING_STATION), DraftingStationBlockEntity::serverTick);
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof DraftingStationBlockEntity station) {
         List<DraftingStationMenu.TemplateSummary> templates = DraftingStationMenu.serverTemplateSummaries(serverPlayer.serverLevel());
         AWCorePlatform.openMenu(
            serverPlayer,
            station,
            buffer -> {
               buffer.writeBlockPos(pos);
               buffer.writeVarInt(templates.size());
               templates.forEach(template -> {
                  buffer.writeUtf(template.id(), 256);
                  buffer.writeUtf(template.label(), 128);
                  buffer.writeVarInt(template.materials().size());
                  template.materials().forEach(resource -> {
                     buffer.writeVarInt(BuiltInRegistries.ITEM.getId((Item)BuiltInRegistries.ITEM.get(resource.itemId())));
                     buffer.writeVarInt(resource.count());
                  });
               });
               buffer.writeUtf(station.selectedTemplate(), 256);
               buffer.writeVarInt(templates.size());
               templates.forEach(
                  template -> {
                     buffer.writeVarInt(template.materials().size());
                     template.materials()
                        .forEach(material -> buffer.writeVarInt(template.id().equals(station.selectedTemplate()) ? station.suppliedCount(material.itemId()) : 0));
                  }
               );
            }
         );
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean piston) {
      if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof DraftingStationBlockEntity station) {
         Containers.dropContents(level, pos, station);
      }

      super.onRemove(state, level, pos, next, piston);
   }
}
