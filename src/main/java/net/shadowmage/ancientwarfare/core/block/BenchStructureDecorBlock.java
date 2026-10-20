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
   public static final Property<Axis> AXIS = RotatedPillarBlock.f_55923_;
   public static final EnumProperty<BenchStructureDecorBlock.Legs> LEGS = EnumProperty.m_61587_("legs", BenchStructureDecorBlock.Legs.class);
   private static final VoxelShape X_SHAPE = Block.m_49796_(0.0, 0.0, 3.0, 16.0, 9.0, 13.0);
   private static final VoxelShape Z_SHAPE = Block.m_49796_(3.0, 0.0, 0.0, 13.0, 9.0, 16.0);

   public BenchStructureDecorBlock(Properties properties) {
      super(properties);
      this.m_49959_((BlockState)((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(AXIS, Axis.X)).m_61124_(LEGS, BenchStructureDecorBlock.Legs.BOTH));
   }

   public BlockState m_5573_(BlockPlaceContext context) {
      Axis axis = context.m_8125_().m_122434_() == Axis.X ? Axis.Z : Axis.X;
      return this.connectedState((BlockState)this.m_49966_().m_61124_(AXIS, axis), context.m_43725_(), context.m_8083_());
   }

   public BlockState m_7417_(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      Axis axis = (Axis)state.m_61143_(AXIS);
      return direction.m_122434_() != axis ? state : this.connectedState(state, level, pos);
   }

   private BlockState connectedState(BlockState state, BlockGetter level, BlockPos pos) {
      Axis axis = (Axis)state.m_61143_(AXIS);
      BenchStructureDecorBlock.Legs legs = BenchStructureDecorBlock.Legs.NONE;
      if (axis == Axis.X) {
         boolean west = this.isSameAxisBench(state, level, pos.m_122024_());
         boolean east = this.isSameAxisBench(state, level, pos.m_122029_());
         if (west && !east) {
            legs = BenchStructureDecorBlock.Legs.LEFT;
         } else if (!west && east) {
            legs = BenchStructureDecorBlock.Legs.RIGHT;
         } else if (!west) {
            legs = BenchStructureDecorBlock.Legs.BOTH;
         }
      } else {
         boolean south = this.isSameAxisBench(state, level, pos.m_122019_());
         boolean north = this.isSameAxisBench(state, level, pos.m_122012_());
         if (north && !south) {
            legs = BenchStructureDecorBlock.Legs.RIGHT;
         } else if (!north && south) {
            legs = BenchStructureDecorBlock.Legs.LEFT;
         } else if (!north) {
            legs = BenchStructureDecorBlock.Legs.BOTH;
         }
      }

      return (BlockState)state.m_61124_(LEGS, legs);
   }

   private boolean isSameAxisBench(BlockState thisState, BlockGetter level, BlockPos neighborPos) {
      BlockState neighborState = level.m_8055_(neighborPos);
      return neighborState.m_60713_(this) && thisState.m_61143_(AXIS) == neighborState.m_61143_(AXIS);
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return state.m_61143_(AXIS) == Axis.X ? X_SHAPE : Z_SHAPE;
   }

   public BlockState m_6843_(BlockState state, Rotation rotation) {
      if (rotation != Rotation.CLOCKWISE_90 && rotation != Rotation.COUNTERCLOCKWISE_90) {
         return state;
      } else {
         Axis axis = (Axis)state.m_61143_(AXIS);
         return (BlockState)state.m_61124_(AXIS, axis == Axis.X ? Axis.Z : Axis.X);
      }
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{AXIS, LEGS});
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

      public String m_7912_() {
         return this.name;
      }
   }
}
