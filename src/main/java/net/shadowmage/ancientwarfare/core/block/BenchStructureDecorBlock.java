package net.shadowmage.ancientwarfare.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BenchStructureDecorBlock extends Block {
   public static final Property<Axis> AXIS = RotatedPillarBlock.AXIS;
   public static final EnumProperty<BenchStructureDecorBlock.Legs> LEGS = EnumProperty.create("legs", BenchStructureDecorBlock.Legs.class);
   private static final VoxelShape X_SHAPE = Block.box(0.0, 0.0, 3.0, 16.0, 9.0, 13.0);
   private static final VoxelShape Z_SHAPE = Block.box(3.0, 0.0, 0.0, 13.0, 9.0, 16.0);

   public BenchStructureDecorBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(AXIS, Axis.X)).setValue(LEGS, BenchStructureDecorBlock.Legs.BOTH));
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Axis axis = context.getHorizontalDirection().getAxis() == Axis.X ? Axis.Z : Axis.X;
      return this.connectedState((BlockState)this.defaultBlockState().setValue(AXIS, axis), context.getLevel(), context.getClickedPos());
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      Axis axis = (Axis)state.getValue(AXIS);
      return direction.getAxis() != axis ? state : this.connectedState(state, level, pos);
   }

   private BlockState connectedState(BlockState state, BlockGetter level, BlockPos pos) {
      Axis axis = (Axis)state.getValue(AXIS);
      BenchStructureDecorBlock.Legs legs = BenchStructureDecorBlock.Legs.NONE;
      if (axis == Axis.X) {
         boolean west = this.isSameAxisBench(state, level, pos.west());
         boolean east = this.isSameAxisBench(state, level, pos.east());
         if (west && !east) {
            legs = BenchStructureDecorBlock.Legs.LEFT;
         } else if (!west && east) {
            legs = BenchStructureDecorBlock.Legs.RIGHT;
         } else if (!west) {
            legs = BenchStructureDecorBlock.Legs.BOTH;
         }
      } else {
         boolean south = this.isSameAxisBench(state, level, pos.south());
         boolean north = this.isSameAxisBench(state, level, pos.north());
         if (north && !south) {
            legs = BenchStructureDecorBlock.Legs.RIGHT;
         } else if (!north && south) {
            legs = BenchStructureDecorBlock.Legs.LEFT;
         } else if (!north) {
            legs = BenchStructureDecorBlock.Legs.BOTH;
         }
      }

      return (BlockState)state.setValue(LEGS, legs);
   }

   private boolean isSameAxisBench(BlockState thisState, BlockGetter level, BlockPos neighborPos) {
      BlockState neighborState = level.getBlockState(neighborPos);
      return neighborState.is(this) && thisState.getValue(AXIS) == neighborState.getValue(AXIS);
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return state.getValue(AXIS) == Axis.X ? X_SHAPE : Z_SHAPE;
   }

   public BlockState rotate(BlockState state, Rotation rotation) {
      if (rotation != Rotation.CLOCKWISE_90 && rotation != Rotation.COUNTERCLOCKWISE_90) {
         return state;
      } else {
         Axis axis = (Axis)state.getValue(AXIS);
         return (BlockState)state.setValue(AXIS, axis == Axis.X ? Axis.Z : Axis.X);
      }
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{AXIS, LEGS});
   }

   public static enum Legs implements StringRepresentable {
      NONE("none"),
      LEFT("left"),
      RIGHT("right"),
      BOTH("both");

      private final String name;

      private Legs(String name) {
         this.name = name;
      }

      public String getSerializedName() {
         return this.name;
      }
   }
}
