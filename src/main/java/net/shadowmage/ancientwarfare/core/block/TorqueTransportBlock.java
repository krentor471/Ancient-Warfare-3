package net.shadowmage.ancientwarfare.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueAccess;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueStats;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTier;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTransportKind;
import net.shadowmage.ancientwarfare.core.tile.TorqueTransportBlockEntity;
import org.jetbrains.annotations.Nullable;

public class TorqueTransportBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = DirectionalBlock.FACING;
   public static final BooleanProperty CONNECTION_DOWN = BooleanProperty.create("connection_down");
   public static final BooleanProperty CONNECTION_UP = BooleanProperty.create("connection_up");
   public static final BooleanProperty CONNECTION_NORTH = BooleanProperty.create("connection_north");
   public static final BooleanProperty CONNECTION_SOUTH = BooleanProperty.create("connection_south");
   public static final BooleanProperty CONNECTION_WEST = BooleanProperty.create("connection_west");
   public static final BooleanProperty CONNECTION_EAST = BooleanProperty.create("connection_east");
   private static final VoxelShape SHAFT_NORTH_SOUTH_SHAPE = box(4.0, 4.0, 0.0, 12.0, 12.0, 16.0);
   private static final VoxelShape SHAFT_EAST_WEST_SHAPE = box(0.0, 4.0, 4.0, 16.0, 12.0, 12.0);
   private static final VoxelShape SHAFT_VERTICAL_SHAPE = box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
   private static final VoxelShape CENTER_SHAPE = box(5.5, 5.5, 5.5, 10.5, 10.5, 10.5);
   private static final VoxelShape[] BRANCH_SHAPES = new VoxelShape[]{
      box(4.0, 0.0, 4.0, 12.0, 6.0, 12.0),
      box(4.0, 10.0, 4.0, 12.0, 16.0, 12.0),
      box(4.0, 4.0, 0.0, 12.0, 12.0, 6.0),
      box(4.0, 4.0, 10.0, 12.0, 12.0, 16.0),
      box(0.0, 4.0, 4.0, 6.0, 12.0, 12.0),
      box(10.0, 4.0, 4.0, 16.0, 12.0, 12.0)
   };
   private final TorqueTransportKind kind;
   private final TorqueTier tier;

   public TorqueTransportBlock(Properties properties, TorqueTransportKind kind, TorqueTier tier) {
      super(properties);
      this.kind = kind;
      this.tier = tier;
      this.registerDefaultState(this.setDefaultConnections((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)));
   }

   public TorqueTransportKind getKind() {
      return this.kind;
   }

   public TorqueTier getTier() {
      return this.tier;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new TorqueTransportBlockEntity(pos, state);
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      if (this.kind == TorqueTransportKind.SHAFT) {
         return (BlockState)this.defaultBlockState().setValue(FACING, this.getBestShaftFacing(context));
      } else {
         Direction facing = context.getHorizontalDirection();
         if (context.getClickedFace().getAxis() == Axis.Y) {
            facing = context.getClickedFace().getOpposite();
         }

         return this.updateConnections(context.getLevel(), context.getClickedPos(), (BlockState)this.defaultBlockState().setValue(FACING, facing));
      }
   }

   private Direction getBestShaftFacing(BlockPlaceContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      Direction fallback = context.getClickedFace().getAxis() == Axis.Y ? context.getClickedFace().getOpposite() : context.getClickedFace();
      Direction best = fallback;
      int bestScore = -1;

      for (Direction facing : Direction.values()) {
         int score = this.scoreShaftFacing(level, pos, facing);
         if (score > bestScore || score == bestScore && facing == fallback) {
            bestScore = score;
            best = facing;
         }
      }

      return bestScore > 0 ? best : fallback;
   }

   private int scoreShaftFacing(Level level, BlockPos pos, Direction facing) {
      boolean input = this.neighborCanOutput(level, pos.relative(facing.getOpposite()), facing);
      boolean output = this.neighborCanInput(level, pos.relative(facing), facing.getOpposite());
      if (input && output) {
         return 8;
      } else if (input) {
         return 4;
      } else {
         return output ? 2 : 0;
      }
   }

   private boolean neighborCanOutput(Level level, BlockPos pos, Direction side) {
      if (level.getBlockEntity(pos) instanceof TorqueAccess torque && torque.canOutputTorque(side)) {
         return true;
      }

      return false;
   }

   private boolean neighborCanInput(Level level, BlockPos pos, Direction side) {
      if (level.getBlockEntity(pos) instanceof TorqueAccess torque && torque.canInputTorque(side)) {
         return true;
      }

      return false;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, CONNECTION_DOWN, CONNECTION_UP, CONNECTION_NORTH, CONNECTION_SOUTH, CONNECTION_WEST, CONNECTION_EAST});
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
   }

   private BlockState setDefaultConnections(BlockState state) {
      return (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)state.setValue(CONNECTION_DOWN, false))
                     .setValue(CONNECTION_UP, false))
                  .setValue(CONNECTION_NORTH, false))
               .setValue(CONNECTION_SOUTH, false))
            .setValue(CONNECTION_WEST, false))
         .setValue(CONNECTION_EAST, false);
   }

   public BlockState updateConnections(Level level, BlockPos pos, BlockState state) {
      if (this.kind == TorqueTransportKind.SHAFT) {
         return state;
      } else {
         BlockState updated = state;

         for (Direction direction : Direction.values()) {
            updated = (BlockState)updated.setValue(connectionProperty(direction), this.hasConnection(level, pos, state, direction));
         }

         return updated;
      }
   }

   public BlockState toggleConnection(BlockState state, Direction direction) {
      if (this.kind == TorqueTransportKind.SHAFT) {
         return state;
      } else {
         BooleanProperty property = connectionProperty(direction);
         return (BlockState)state.setValue(property, !(Boolean)state.getValue(property));
      }
   }

   public boolean isConnected(BlockState state, Direction direction) {
      return this.kind == TorqueTransportKind.SHAFT || (Boolean)state.getValue(connectionProperty(direction));
   }

   private boolean hasConnection(Level level, BlockPos pos, BlockState state, Direction direction) {
      if (!(level.getBlockEntity(pos.relative(direction)) instanceof TorqueAccess torque)) {
         return false;
      } else {
         Direction neighborSide = direction.getOpposite();
         return this.canOutputFromState(state, direction) && torque.canInputTorque(neighborSide)
            || this.canInputFromState(state, direction) && torque.canOutputTorque(neighborSide);
      }
   }

   private boolean canOutputFromState(BlockState state, Direction direction) {
      Direction facing = (Direction)state.getValue(FACING);
      return this.kind == TorqueTransportKind.DISTRIBUTOR ? direction != facing.getOpposite() : direction == facing;
   }

   private boolean canInputFromState(BlockState state, Direction direction) {
      Direction facing = (Direction)state.getValue(FACING);
      return this.kind == TorqueTransportKind.DISTRIBUTOR ? direction == facing.getOpposite() : direction != facing;
   }

   public static BooleanProperty connectionProperty(Direction direction) {
      return switch (direction) {
         case DOWN -> CONNECTION_DOWN;
         case UP -> CONNECTION_UP;
         case NORTH -> CONNECTION_NORTH;
         case SOUTH -> CONNECTION_SOUTH;
         case WEST -> CONNECTION_WEST;
         case EAST -> CONNECTION_EAST;
         default -> throw new IncompatibleClassChangeError();
      };
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!player.getItemInHand(hand).isEmpty()) {
         return InteractionResult.PASS;
      } else {
         if (!level.isClientSide && level.getBlockEntity(pos) instanceof TorqueTransportBlockEntity torque) {
            if (this.kind == TorqueTransportKind.JUNCTION || this.kind == TorqueTransportKind.DISTRIBUTOR) {
               Direction clickedFace = hit.getDirection();
               boolean output = this.kind == TorqueTransportKind.DISTRIBUTOR
                  ? clickedFace != ((Direction)state.getValue(FACING)).getOpposite()
                  : clickedFace == state.getValue(FACING);
               boolean connected = this.isConnected(state, clickedFace);
               player.displayClientMessage(
                  Component.translatable(
                     output ? "guistrings.automation.torque.face.output" : "guistrings.automation.torque.face.input",
                     new Object[]{
                        clickedFace.getName(),
                        connected
                           ? Component.translatable("guistrings.automation.torque.face.connected")
                           : Component.translatable("guistrings.automation.torque.face.disconnected")
                     }
                  ),
                  true
               );
               return InteractionResult.sidedSuccess(false);
            }

            TorqueStats stats = torque.getStats();
            player.displayClientMessage(
               Component.translatable(
                  "guistrings.automation.torque.values",
                  new Object[]{
                     String.format("%.2f", stats.stored()),
                     String.format("%.2f", stats.input()),
                     String.format("%.2f", stats.output()),
                     String.format("%.2f", stats.loss())
                  }
               ),
               true
            );
         }

         return InteractionResult.sidedSuccess(level.isClientSide);
      }
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return !level.isClientSide && type == AWCoreRegistryAccess.blockEntityType(AWCoreIds.id("torque_transport"))
         ? (tickerLevel, pos, tickerState, blockEntity) -> TorqueTransportBlockEntity.serverTick(
               tickerLevel, pos, tickerState, (TorqueTransportBlockEntity)blockEntity
            )
         : null;
   }

   public RenderShape getRenderShape(BlockState state) {
      return this.kind != TorqueTransportKind.SHAFT && this.kind != TorqueTransportKind.JUNCTION && this.kind != TorqueTransportKind.DISTRIBUTOR
         ? RenderShape.MODEL
         : RenderShape.ENTITYBLOCK_ANIMATED;
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.kind == TorqueTransportKind.SHAFT ? shaftShape((Direction)state.getValue(FACING)) : CENTER_SHAPE;
   }

   public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
      return this.kind == TorqueTransportKind.SHAFT ? this.getShape(state, level, pos, CollisionContext.empty()) : Shapes.block();
   }

   private static VoxelShape shaftShape(Direction facing) {
      return switch (facing.getAxis()) {
         case X -> SHAFT_EAST_WEST_SHAPE;
         case Y -> SHAFT_VERTICAL_SHAPE;
         case Z -> SHAFT_NORTH_SOUTH_SHAPE;
         default -> throw new IncompatibleClassChangeError();
      };
   }
}
