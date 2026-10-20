package net.shadowmage.ancientwarfare.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TableStructureDecorBlock extends Block {
   public static final BooleanProperty LEG_NORTHEAST = BooleanProperty.m_61465_("leg_northeast");
   public static final BooleanProperty LEG_SOUTHEAST = BooleanProperty.m_61465_("leg_southeast");
   public static final BooleanProperty LEG_SOUTHWEST = BooleanProperty.m_61465_("leg_southwest");
   public static final BooleanProperty LEG_NORTHWEST = BooleanProperty.m_61465_("leg_northwest");
   private static final VoxelShape SHAPE = StructureDecorBlock.shape(
      Block.m_49796_(0.0, 14.0, 0.0, 16.0, 16.0, 16.0),
      Block.m_49796_(0.0, 0.0, 0.0, 2.0, 14.0, 2.0),
      Block.m_49796_(14.0, 0.0, 0.0, 16.0, 14.0, 2.0),
      Block.m_49796_(14.0, 0.0, 14.0, 16.0, 14.0, 16.0),
      Block.m_49796_(0.0, 0.0, 14.0, 2.0, 14.0, 16.0)
   );

   public TableStructureDecorBlock(Properties properties) {
      super(properties);
      this.m_49959_(
         (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(LEG_NORTHEAST, true)).m_61124_(LEG_SOUTHEAST, true))
               .m_61124_(LEG_SOUTHWEST, true))
            .m_61124_(LEG_NORTHWEST, true)
      );
   }

   public BlockState m_7417_(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      return this.connectedState(state, level, pos);
   }

   public BlockState m_5573_(BlockPlaceContext context) {
      return this.connectedState(this.m_49966_(), context.m_43725_(), context.m_8083_());
   }

   private BlockState connectedState(BlockState state, BlockGetter level, BlockPos pos) {
      boolean north = level.m_8055_(pos.m_122012_()).m_60713_(this);
      boolean east = level.m_8055_(pos.m_122029_()).m_60713_(this);
      boolean south = level.m_8055_(pos.m_122019_()).m_60713_(this);
      boolean west = level.m_8055_(pos.m_122024_()).m_60713_(this);
      return (BlockState)((BlockState)((BlockState)((BlockState)state.m_61124_(LEG_NORTHEAST, !north && !east)).m_61124_(LEG_SOUTHEAST, !south && !east))
            .m_61124_(LEG_SOUTHWEST, !south && !west))
         .m_61124_(LEG_NORTHWEST, !north && !west);
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public VoxelShape m_5939_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.m_5940_(state, level, pos, context);
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{LEG_NORTHEAST, LEG_SOUTHEAST, LEG_SOUTHWEST, LEG_NORTHWEST});
   }
}
