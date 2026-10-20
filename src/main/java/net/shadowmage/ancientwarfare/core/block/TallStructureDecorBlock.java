package net.shadowmage.ancientwarfare.core.block;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TallStructureDecorBlock extends HorizontalDirectionalBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.f_54117_;
   public static final EnumProperty<DoubleBlockHalf> HALF = EnumProperty.m_61587_("half", DoubleBlockHalf.class);
   private final Map<Direction, VoxelShape> lowerShapes;
   private final Map<Direction, VoxelShape> upperShapes;
   private final boolean renderUpper;

   public TallStructureDecorBlock(Properties properties, Map<Direction, VoxelShape> lowerShapes, Map<Direction, VoxelShape> upperShapes) {
      this(properties, lowerShapes, upperShapes, false);
   }

   public TallStructureDecorBlock(Properties properties, Map<Direction, VoxelShape> lowerShapes, Map<Direction, VoxelShape> upperShapes, boolean renderUpper) {
      super(properties);
      this.lowerShapes = lowerShapes;
      this.upperShapes = upperShapes;
      this.renderUpper = renderUpper;
      this.m_49959_((BlockState)((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(FACING, Direction.NORTH)).m_61124_(HALF, DoubleBlockHalf.LOWER));
   }

   public BlockState m_5573_(BlockPlaceContext context) {
      BlockPos pos = context.m_8083_();
      return pos.m_123342_() < context.m_43725_().m_151558_() - 1 && context.m_43725_().m_8055_(pos.m_7494_()).m_60629_(context)
         ? (BlockState)((BlockState)this.m_49966_().m_61124_(FACING, context.m_8125_().m_122424_())).m_61124_(HALF, DoubleBlockHalf.LOWER)
         : null;
   }

   public void m_6402_(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      level.m_7731_(pos.m_7494_(), (BlockState)state.m_61124_(HALF, DoubleBlockHalf.UPPER), 3);
   }

   public void m_5707_(Level level, BlockPos pos, BlockState state, Player player) {
      DoubleBlockHalf half = (DoubleBlockHalf)state.m_61143_(HALF);
      BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.m_7494_() : pos.m_7495_();
      BlockState otherState = level.m_8055_(otherPos);
      if (otherState.m_60713_(this) && otherState.m_61143_(HALF) != half) {
         level.m_46953_(otherPos, false, player);
      }

      super.m_5707_(level, pos, state, player);
   }

   public BlockState m_7417_(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      DoubleBlockHalf half = (DoubleBlockHalf)state.m_61143_(HALF);
      Direction neededDirection = half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
      return direction != neededDirection || neighborState.m_60713_(this) && neighborState.m_61143_(HALF) != half
         ? super.m_7417_(state, direction, neighborState, level, pos, neighborPos)
         : Blocks.f_50016_.m_49966_();
   }

   public RenderShape m_7514_(BlockState state) {
      return state.m_61143_(HALF) != DoubleBlockHalf.LOWER && !this.renderUpper ? RenderShape.INVISIBLE : RenderShape.MODEL;
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      Map<Direction, VoxelShape> shapes = state.m_61143_(HALF) == DoubleBlockHalf.LOWER ? this.lowerShapes : this.upperShapes;
      return shapes.getOrDefault(state.m_61143_(FACING), Shapes.m_83144_());
   }

   public VoxelShape m_5939_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.m_5940_(state, level, pos, context);
   }

   public BlockState m_6843_(BlockState state, Rotation rotation) {
      return (BlockState)state.m_61124_(FACING, rotation.m_55954_((Direction)state.m_61143_(FACING)));
   }

   public BlockState m_6943_(BlockState state, Mirror mirror) {
      return state.m_60717_(mirror.m_54846_((Direction)state.m_61143_(FACING)));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{FACING, HALF});
   }
}
