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
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   private static final VoxelShape SHAPE = box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   private final BiFunction<BlockPos, BlockState, BlockEntity> blockEntityFactory;

   public WarehouseBlock(Properties properties, BiFunction<BlockPos, BlockState, BlockEntity> blockEntityFactory) {
      super(properties);
      this.blockEntityFactory = blockEntityFactory;
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return this.blockEntityFactory.apply(pos, state);
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!level.isClientSide && level.getBlockEntity(pos) instanceof MenuProvider provider) {
         AWCorePlatform.openBlockMenu(player, provider, pos);
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!level.isClientSide && !state.is(newState.getBlock())) {
         BlockEntity blockEntity = level.getBlockEntity(pos);
         if (blockEntity instanceof WarehouseStorageBlockEntity storage) {
            for (WarehouseEntry entry : storage.takeAllEntries()) {
               long remaining = entry.count();

               while (remaining > 0L) {
                  int count = (int)Math.min(remaining, (long)entry.stack().getMaxStackSize());
                  Containers.dropItemStack(
                     level, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, entry.stack().copyWithCount(count)
                  );
                  remaining -= (long)count;
               }
            }
         } else if (blockEntity instanceof Container container) {
            if (blockEntity instanceof WarehouseInterfaceBlockEntity warehouseInterface) {
               for (ItemStack stack : warehouseInterface.takePendingReturns()) {
                  Containers.dropItemStack(level, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, stack);
               }
            }

            Containers.dropContents(level, pos, container);
         }
      }

      super.onRemove(state, level, pos, newState, movedByPiston);
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      if (level.isClientSide) {
         return null;
      } else {
         BlockEntityTicker<T> controlTicker = createTickerHelper(
            type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.WAREHOUSE_CONTROL), WarehouseControlBlockEntity::serverTick
         );
         if (controlTicker != null) {
            return controlTicker;
         } else {
            BlockEntityTicker<T> interfaceTicker = createTickerHelper(
               type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.WAREHOUSE_INTERFACE), WarehouseInterfaceBlockEntity::serverTick
            );
            if (interfaceTicker != null) {
               return interfaceTicker;
            } else {
               BlockEntityTicker<T> viewerTicker = createTickerHelper(
                  type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.WAREHOUSE_STOCK_VIEWER), WarehouseStockViewerBlockEntity::serverTick
               );
               if (viewerTicker != null) {
                  return viewerTicker;
               } else {
                  BlockEntityTicker<T> craftingTicker = createTickerHelper(
                     type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.WAREHOUSE_CRAFTING), WarehouseCraftingStationBlockEntity::serverTick
                  );
                  if (craftingTicker != null) {
                     return craftingTicker;
                  } else {
                     BlockEntityTicker<T> autoCraftingTicker = createTickerHelper(
                        type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.AUTO_CRAFTING), AutoCraftingBlockEntity::serverTick
                     );
                     return autoCraftingTicker != null
                        ? autoCraftingTicker
                        : createTickerHelper(type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.WAREHOUSE_STOCK_LINKER), (tickLevel, tickPos, tickState, linker) -> {
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
