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
import net.shadowmage.ancientwarfare.core.tile.StructureContainerBlockEntity;
import org.jetbrains.annotations.Nullable;

public class StructureContainerBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   private final BiFunction<BlockPos, BlockState, BlockEntity> blockEntityFactory;
   private final VoxelShape shape;

   public StructureContainerBlock(Properties properties, VoxelShape shape, BiFunction<BlockPos, BlockState, BlockEntity> blockEntityFactory) {
      super(properties);
      this.shape = shape;
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
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Container container) {
         if (container instanceof StructureContainerBlockEntity structureContainer) {
            structureContainer.unpackLootForDestruction();
            Containers.dropContents(level, pos, container);
         } else {
            Containers.dropContents(level, pos, container);
         }
      }

      super.onRemove(state, level, pos, newState, movedByPiston);
   }

   public RenderShape getRenderShape(BlockState state) {
      return !state.is(AWCoreRegistryAccess.block(AWCoreIds.WOODEN_COFFIN)) && !state.is(AWCoreRegistryAccess.block(AWCoreIds.STONE_COFFIN))
         ? RenderShape.MODEL
         : RenderShape.INVISIBLE;
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide
            && (state.is(AWCoreRegistryAccess.block(AWCoreIds.WOODEN_COFFIN)) || state.is(AWCoreRegistryAccess.block(AWCoreIds.STONE_COFFIN)))
         ? (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof StructureContainerBlockEntity container) {
               container.clientTickLid();
            }
         }
         : null;
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.shape;
   }
}
