package net.shadowmage.ancientwarfare.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StaticGateBlock extends HorizontalDirectionalBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   public static final BooleanProperty OPEN = BooleanProperty.create("open");
   public static final BooleanProperty POWERED = BooleanProperty.create("powered");
   private static final VoxelShape CLOSED_NORTH_SOUTH = Block.box(0.0, 0.0, 7.0, 16.0, 32.0, 9.0);
   private static final VoxelShape CLOSED_EAST_WEST = Block.box(7.0, 0.0, 0.0, 9.0, 32.0, 16.0);
   private final boolean metal;

   public StaticGateBlock(Properties properties, boolean metal) {
      super(properties);
      this.metal = metal;
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(OPEN, false))
            .setValue(POWERED, false)
      );
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
      return (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())).setValue(OPEN, powered))
         .setValue(POWERED, powered);
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if ((Boolean)state.getValue(POWERED)) {
         return InteractionResult.sidedSuccess(level.isClientSide);
      } else {
         boolean open = !(Boolean)state.getValue(OPEN);
         level.setBlock(pos, (BlockState)state.setValue(OPEN, open), 10);
         this.playGateSound(level, pos, open);
         return InteractionResult.sidedSuccess(level.isClientSide);
      }
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      if (!level.isClientSide) {
         boolean powered = level.hasNeighborSignal(pos);
         if (powered != (Boolean)state.getValue(POWERED)) {
            boolean wasOpen = (Boolean)state.getValue(OPEN);
            level.setBlock(pos, (BlockState)((BlockState)state.setValue(POWERED, powered)).setValue(OPEN, powered), 2);
            if (wasOpen != powered) {
               this.playGateSound(level, pos, powered);
            }
         }
      }
   }

   private void playGateSound(Level level, BlockPos pos, boolean open) {
      level.playSound(
         null,
         pos,
         this.metal ? (open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE) : (open ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE),
         SoundSource.BLOCKS,
         0.8F,
         open ? 1.0F : 0.8F
      );
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return state.getValue(OPEN) ? Shapes.block() : this.closedShape(state);
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return state.getValue(OPEN) ? Shapes.empty() : this.closedShape(state);
   }

   private VoxelShape closedShape(BlockState state) {
      Direction facing = (Direction)state.getValue(FACING);
      return facing.getAxis() == Axis.X ? CLOSED_EAST_WEST : CLOSED_NORTH_SOUTH;
   }

   public BlockState rotate(BlockState state, Rotation rotation) {
      return (BlockState)state.setValue(FACING, rotation.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirror) {
      return state.rotate(mirror.getRotation((Direction)state.getValue(FACING)));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, OPEN, POWERED});
   }
}
