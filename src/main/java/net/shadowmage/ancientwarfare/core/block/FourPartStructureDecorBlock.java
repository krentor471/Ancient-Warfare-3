package net.shadowmage.ancientwarfare.core.block;

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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FourPartStructureDecorBlock extends HorizontalDirectionalBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);

   public FourPartStructureDecorBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(PART, 0));
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockPos pos = context.getClickedPos();
      if (pos.getY() >= context.getLevel().getMaxBuildHeight() - 3) {
         return null;
      } else {
         for (int offset = 1; offset < 4; offset++) {
            if (!context.getLevel().getBlockState(pos.above(offset)).canBeReplaced(context)) {
               return null;
            }
         }

         return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())).setValue(PART, 0);
      }
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      for (int offset = 1; offset < 4; offset++) {
         level.setBlock(pos.above(offset), (BlockState)state.setValue(PART, offset), 3);
      }
   }

   public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      BlockPos basePos = pos.below((Integer)state.getValue(PART));

      for (int offset = 0; offset < 4; offset++) {
         BlockPos partPos = basePos.above(offset);
         if (!partPos.equals(pos) && level.getBlockState(partPos).is(this)) {
            level.destroyBlock(partPos, false, player);
         }
      }

      super.playerWillDestroy(level, pos, state, player);
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      int part = (Integer)state.getValue(PART);
      if (part <= 0 || direction != Direction.DOWN || neighborState.is(this) && (Integer)neighborState.getValue(PART) == part - 1) {
         return part >= 3 || direction != Direction.UP || neighborState.is(this) && neighborState.getValue(PART) == part + 1
            ? super.updateShape(state, direction, neighborState, level, pos, neighborPos)
            : Blocks.AIR.defaultBlockState();
      } else {
         return Blocks.AIR.defaultBlockState();
      }
   }

   public RenderShape getRenderShape(BlockState state) {
      return state.getValue(PART) != 0 && state.getValue(PART) != 2 ? RenderShape.INVISIBLE : RenderShape.MODEL;
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.block();
   }

   public BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirror) {
      return state.rotate(mirror.getRotation((Direction)state.getValue(FACING)));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, PART});
   }
}
