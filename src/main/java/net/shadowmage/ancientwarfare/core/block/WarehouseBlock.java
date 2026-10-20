package net.shadowmage.ancientwarfare.core.block;

import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.warehouse.WarehouseEntry;
import net.shadowmage.ancientwarfare.core.tile.AutoCraftingBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseControlBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseCraftingStationBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseInterfaceBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseStockLinkerBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseStockViewerBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WarehouseStorageBlockEntity;
import org.jetbrains.annotations.Nullable;

public class WarehouseBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.f_54117_;
   private static final VoxelShape SHAPE = m_49796_(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   private final BiFunction<BlockPos, BlockState, BlockEntity> blockEntityFactory;

   public WarehouseBlock(Properties properties, BiFunction<BlockPos, BlockState, BlockEntity> blockEntityFactory) {
      super(properties);
      this.blockEntityFactory = blockEntityFactory;
      this.m_49959_((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(FACING, Direction.NORTH));
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return this.blockEntityFactory.apply(pos, state);
   }

   @Nullable
   public BlockState m_5573_(BlockPlaceContext context) {
      return (BlockState)this.m_49966_().m_61124_(FACING, context.m_8125_().m_122424_());
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{FACING});
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!level.f_46443_ && level.m_7702_(pos) instanceof MenuProvider provider) {
         AWCorePlatform.openBlockMenu(player, provider, pos);
      }

      return InteractionResult.m_19078_(level.f_46443_);
   }

   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!level.f_46443_ && !state.m_60713_(newState.m_60734_())) {
         BlockEntity blockEntity = level.m_7702_(pos);
         if (blockEntity instanceof WarehouseStorageBlockEntity storage) {
            for (WarehouseEntry entry : storage.takeAllEntries()) {
               long remaining = entry.count();

               while (remaining > 0L) {
                  int count = (int)Math.min(remaining, (long)entry.stack().m_41741_());
                  Containers.m_18992_(
                     level, (double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5, entry.stack().m_255036_(count)
                  );
                  remaining -= (long)count;
               }
            }
         } else if (blockEntity instanceof Container container) {
            if (blockEntity instanceof WarehouseInterfaceBlockEntity warehouseInterface) {
               for (ItemStack stack : warehouseInterface.takePendingReturns()) {
                  Containers.m_18992_(level, (double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5, stack);
               }
            }

            Containers.m_19002_(level, pos, container);
         }
      }

      super.m_6810_(state, level, pos, newState, movedByPiston);
   }

   public RenderShape m_7514_(BlockState state) {
      return RenderShape.MODEL;
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      if (level.f_46443_) {
         return null;
      } else {
         BlockEntityTicker<T> controlTicker = m_152132_(
            type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.WAREHOUSE_CONTROL), WarehouseControlBlockEntity::serverTick
         );
         if (controlTicker != null) {
            return controlTicker;
         } else {
            BlockEntityTicker<T> interfaceTicker = m_152132_(
               type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.WAREHOUSE_INTERFACE), WarehouseInterfaceBlockEntity::serverTick
            );
            if (interfaceTicker != null) {
               return interfaceTicker;
            } else {
               BlockEntityTicker<T> viewerTicker = m_152132_(
                  type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.WAREHOUSE_STOCK_VIEWER), WarehouseStockViewerBlockEntity::serverTick
               );
               if (viewerTicker != null) {
                  return viewerTicker;
               } else {
                  BlockEntityTicker<T> craftingTicker = m_152132_(
                     type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.WAREHOUSE_CRAFTING), WarehouseCraftingStationBlockEntity::serverTick
                  );
                  if (craftingTicker != null) {
                     return craftingTicker;
                  } else {
                     BlockEntityTicker<T> autoCraftingTicker = m_152132_(
                        type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.AUTO_CRAFTING), AutoCraftingBlockEntity::serverTick
                     );
                     return autoCraftingTicker != null
                        ? autoCraftingTicker
                        : m_152132_(type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.WAREHOUSE_STOCK_LINKER), (tickLevel, tickPos, tickState, linker) -> {
                           if (linker instanceof WarehouseStockLinkerBlockEntity stockLinker) {
                              WarehouseStockViewerBlockEntity.serverTick(tickLevel, tickPos, tickState, stockLinker);
                           }
                        });
                  }
               }
            }
         }
      }
   }
}
