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
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final EnumProperty<DoubleBlockHalf> HALF = EnumProperty.create("half", DoubleBlockHalf.class);
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
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(HALF, DoubleBlockHalf.LOWER));
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockPos pos = context.getClickedPos();
      return pos.getY() < context.getLevel().getMaxBuildHeight() - 1 && context.getLevel().getBlockState(pos.above()).canBeReplaced(context)
         ? (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())).setValue(HALF, DoubleBlockHalf.LOWER)
         : null;
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      level.setBlock(pos.above(), (BlockState)state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
   }

   public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      DoubleBlockHalf half = (DoubleBlockHalf)state.getValue(HALF);
      BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
      BlockState otherState = level.getBlockState(otherPos);
      if (otherState.is(this) && otherState.getValue(HALF) != half) {
         level.destroyBlock(otherPos, false, player);
      }

      super.playerWillDestroy(level, pos, state, player);
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      DoubleBlockHalf half = (DoubleBlockHalf)state.getValue(HALF);
      Direction neededDirection = half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
      return direction != neededDirection || neighborState.is(this) && neighborState.getValue(HALF) != half
         ? super.updateShape(state, direction, neighborState, level, pos, neighborPos)
         : Blocks.AIR.defaultBlockState();
   }

   public RenderShape getRenderShape(BlockState state) {
      return state.getValue(HALF) != DoubleBlockHalf.LOWER && !this.renderUpper ? RenderShape.INVISIBLE : RenderShape.MODEL;
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      Map<Direction, VoxelShape> shapes = state.getValue(HALF) == DoubleBlockHalf.LOWER ? this.lowerShapes : this.upperShapes;
      return shapes.getOrDefault(state.getValue(FACING), Shapes.block());
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.getShape(state, level, pos, context);
   }

   public BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirror) {
      return state.rotate(mirror.getRotation((Direction)state.getValue(FACING)));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, HALF});
   }
}
