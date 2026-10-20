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
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.f_54117_;
   private final BiFunction<BlockPos, BlockState, BlockEntity> blockEntityFactory;
   private final VoxelShape shape;

   public StructureContainerBlock(Properties properties, VoxelShape shape, BiFunction<BlockPos, BlockState, BlockEntity> blockEntityFactory) {
      super(properties);
      this.shape = shape;
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
      if (!state.m_60713_(newState.m_60734_()) && level.m_7702_(pos) instanceof Container container) {
         if (container instanceof StructureContainerBlockEntity structureContainer) {
            structureContainer.unpackLootForDestruction();
            Containers.m_19002_(level, pos, container);
         } else {
            Containers.m_19002_(level, pos, container);
         }
      }

      super.m_6810_(state, level, pos, newState, movedByPiston);
   }

   public RenderShape m_7514_(BlockState state) {
      return !state.m_60713_(AWCoreRegistryAccess.block(AWCoreIds.WOODEN_COFFIN)) && !state.m_60713_(AWCoreRegistryAccess.block(AWCoreIds.STONE_COFFIN))
         ? RenderShape.MODEL
         : RenderShape.INVISIBLE;
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      return level.f_46443_
            && (state.m_60713_(AWCoreRegistryAccess.block(AWCoreIds.WOODEN_COFFIN)) || state.m_60713_(AWCoreRegistryAccess.block(AWCoreIds.STONE_COFFIN)))
         ? (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof StructureContainerBlockEntity container) {
               container.clientTickLid();
            }
         }
         : null;
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.shape;
   }
}
