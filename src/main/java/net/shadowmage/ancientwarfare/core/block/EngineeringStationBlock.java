package net.shadowmage.ancientwarfare.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.tile.EngineeringStationBlockEntity;
import org.jetbrains.annotations.Nullable;

public class EngineeringStationBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.f_54117_;
   private static final VoxelShape TABLE_SHAPE = Shapes.m_83124_(
      m_49796_(0.0, 13.0, 0.0, 16.0, 16.0, 16.0),
      new VoxelShape[]{
         m_49796_(1.0, 0.0, 1.0, 4.0, 13.0, 4.0),
         m_49796_(12.0, 0.0, 1.0, 15.0, 13.0, 4.0),
         m_49796_(1.0, 0.0, 12.0, 4.0, 13.0, 15.0),
         m_49796_(12.0, 0.0, 12.0, 15.0, 13.0, 15.0),
         m_49796_(1.0, 8.0, 1.0, 15.0, 13.0, 15.0)
      }
   );

   public EngineeringStationBlock(Properties properties) {
      super(properties);
      this.m_49959_((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(FACING, Direction.NORTH));
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new EngineeringStationBlockEntity(pos, state);
   }

   @Nullable
   public BlockState m_5573_(BlockPlaceContext context) {
      return (BlockState)this.m_49966_().m_61124_(FACING, context.m_8125_().m_122424_());
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{FACING});
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!level.f_46443_ && level.m_7702_(pos) instanceof EngineeringStationBlockEntity engineeringStation) {
         AWCorePlatform.openBlockMenu(player, engineeringStation, pos);
      }

      return InteractionResult.m_19078_(level.f_46443_);
   }

   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!state.m_60713_(newState.m_60734_()) && level.m_7702_(pos) instanceof EngineeringStationBlockEntity engineeringStation) {
         for (ItemStack stack : engineeringStation.getDrops()) {
            Containers.m_18992_(level, (double)pos.m_123341_(), (double)pos.m_123342_(), (double)pos.m_123343_(), stack);
         }
      }

      super.m_6810_(state, level, pos, newState, movedByPiston);
   }

   public RenderShape m_7514_(BlockState state) {
      return RenderShape.MODEL;
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return TABLE_SHAPE;
   }

   public int m_7753_(BlockState state, BlockGetter level, BlockPos pos) {
      return 0;
   }
}
