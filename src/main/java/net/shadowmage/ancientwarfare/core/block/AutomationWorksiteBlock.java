package net.shadowmage.ancientwarfare.core.block;

import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.shadowmage.ancientwarfare.core.tile.BasicWorksiteBlockEntity;
import org.jetbrains.annotations.Nullable;

public class AutomationWorksiteBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.f_54117_;
   private static final VoxelShape SHAPE = m_49796_(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   private final BiFunction<BlockPos, BlockState, BlockEntity> blockEntityFactory;

   public AutomationWorksiteBlock(Properties properties, BiFunction<BlockPos, BlockState, BlockEntity> blockEntityFactory) {
      super(properties);
      this.blockEntityFactory = blockEntityFactory;
      this.m_49959_((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(FACING, Direction.NORTH));
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return this.blockEntityFactory.apply(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      return level.f_46443_ ? null : (tickerLevel, pos, tickerState, blockEntity) -> {
         if (blockEntity instanceof BasicWorksiteBlockEntity worksite) {
            BasicWorksiteBlockEntity.tickStoredEnergy(tickerLevel, pos, tickerState, worksite);
         }
      };
   }

   @Nullable
   public BlockState m_5573_(BlockPlaceContext context) {
      return (BlockState)this.m_49966_().m_61124_(FACING, context.m_8125_().m_122424_());
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{FACING});
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!level.f_46443_ && level.m_7702_(pos) instanceof BasicWorksiteBlockEntity worksite) {
         if (worksite instanceof MenuProvider provider) {
            AWCorePlatform.openBlockMenu(player, provider, pos);
         } else {
            player.m_5661_(
               Component.m_237110_(
                  "guistrings.automation.worksite_status",
                  new Object[]{Component.m_237115_(worksite.getDescriptionId()), worksite.getStoredEnergy(), worksite.getMaxEnergy()}
               ),
               true
            );
         }
      }

      return InteractionResult.m_19078_(level.f_46443_);
   }

   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!state.m_60713_(newState.m_60734_()) && level.m_7702_(pos) instanceof BasicWorksiteBlockEntity worksite) {
         worksite.releaseChunkLoading();
         if (worksite instanceof Container container) {
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
}
