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
   public static final BooleanProperty LEG_NORTHEAST = BooleanProperty.create("leg_northeast");
   public static final BooleanProperty LEG_SOUTHEAST = BooleanProperty.create("leg_southeast");
   public static final BooleanProperty LEG_SOUTHWEST = BooleanProperty.create("leg_southwest");
   public static final BooleanProperty LEG_NORTHWEST = BooleanProperty.create("leg_northwest");
   private static final VoxelShape SHAPE = StructureDecorBlock.shape(
      Block.box(0.0, 14.0, 0.0, 16.0, 16.0, 16.0),
      Block.box(0.0, 0.0, 0.0, 2.0, 14.0, 2.0),
      Block.box(14.0, 0.0, 0.0, 16.0, 14.0, 2.0),
      Block.box(14.0, 0.0, 14.0, 16.0, 14.0, 16.0),
      Block.box(0.0, 0.0, 14.0, 2.0, 14.0, 16.0)
   );

   public TableStructureDecorBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(LEG_NORTHEAST, true)).setValue(LEG_SOUTHEAST, true))
               .setValue(LEG_SOUTHWEST, true))
            .setValue(LEG_NORTHWEST, true)
      );
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      return this.connectedState(state, level, pos);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return this.connectedState(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
   }

   private BlockState connectedState(BlockState state, BlockGetter level, BlockPos pos) {
      boolean north = level.getBlockState(pos.north()).is(this);
      boolean east = level.getBlockState(pos.east()).is(this);
      boolean south = level.getBlockState(pos.south()).is(this);
      boolean west = level.getBlockState(pos.west()).is(this);
      return (BlockState)((BlockState)((BlockState)((BlockState)state.setValue(LEG_NORTHEAST, !north && !east)).setValue(LEG_SOUTHEAST, !south && !east))
            .setValue(LEG_SOUTHWEST, !south && !west))
         .setValue(LEG_NORTHWEST, !north && !west);
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.getShape(state, level, pos, context);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{LEG_NORTHEAST, LEG_SOUTHEAST, LEG_SOUTHWEST, LEG_NORTHWEST});
   }
}
