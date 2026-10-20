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
   public static final DirectionProperty FACING = DirectionalBlock.f_52588_;
   public static final BooleanProperty CONNECTION_DOWN = BooleanProperty.m_61465_("connection_down");
   public static final BooleanProperty CONNECTION_UP = BooleanProperty.m_61465_("connection_up");
   public static final BooleanProperty CONNECTION_NORTH = BooleanProperty.m_61465_("connection_north");
   public static final BooleanProperty CONNECTION_SOUTH = BooleanProperty.m_61465_("connection_south");
   public static final BooleanProperty CONNECTION_WEST = BooleanProperty.m_61465_("connection_west");
   public static final BooleanProperty CONNECTION_EAST = BooleanProperty.m_61465_("connection_east");
   private static final VoxelShape SHAFT_NORTH_SOUTH_SHAPE = m_49796_(4.0, 4.0, 0.0, 12.0, 12.0, 16.0);
   private static final VoxelShape SHAFT_EAST_WEST_SHAPE = m_49796_(0.0, 4.0, 4.0, 16.0, 12.0, 12.0);
   private static final VoxelShape SHAFT_VERTICAL_SHAPE = m_49796_(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
   private static final VoxelShape CENTER_SHAPE = m_49796_(5.5, 5.5, 5.5, 10.5, 10.5, 10.5);
   private static final VoxelShape[] BRANCH_SHAPES = new VoxelShape[]{
      m_49796_(4.0, 0.0, 4.0, 12.0, 6.0, 12.0),
      m_49796_(4.0, 10.0, 4.0, 12.0, 16.0, 12.0),
      m_49796_(4.0, 4.0, 0.0, 12.0, 12.0, 6.0),
      m_49796_(4.0, 4.0, 10.0, 12.0, 12.0, 16.0),
      m_49796_(0.0, 4.0, 4.0, 6.0, 12.0, 12.0),
      m_49796_(10.0, 4.0, 4.0, 16.0, 12.0, 12.0)
   };
   private final TorqueTransportKind kind;
   private final TorqueTier tier;

   public TorqueTransportBlock(Properties properties, TorqueTransportKind kind, TorqueTier tier) {
      super(properties);
      this.kind = kind;
      this.tier = tier;
      this.m_49959_(this.setDefaultConnections((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(FACING, Direction.NORTH)));
   }

   public TorqueTransportKind getKind() {
      return this.kind;
   }

   public TorqueTier getTier() {
      return this.tier;
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new TorqueTransportBlockEntity(pos, state);
   }

   @Nullable
   public BlockState m_5573_(BlockPlaceContext context) {
      if (this.kind == TorqueTransportKind.SHAFT) {
         return (BlockState)this.m_49966_().m_61124_(FACING, this.getBestShaftFacing(context));
      } else {
         Direction facing = context.m_8125_();
         if (context.m_43719_().m_122434_() == Axis.Y) {
            facing = context.m_43719_().m_122424_();
         }

         return this.updateConnections(context.m_43725_(), context.m_8083_(), (BlockState)this.m_49966_().m_61124_(FACING, facing));
      }
   }

   private Direction getBestShaftFacing(BlockPlaceContext context) {
      Level level = context.m_43725_();
      BlockPos pos = context.m_8083_();
      Direction fallback = context.m_43719_().m_122434_() == Axis.Y ? context.m_43719_().m_122424_() : context.m_43719_();
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
      boolean input = this.neighborCanOutput(level, pos.m_121945_(facing.m_122424_()), facing);
      boolean output = this.neighborCanInput(level, pos.m_121945_(facing), facing.m_122424_());
      if (input && output) {
         return 8;
      } else if (input) {
         return 4;
      } else {
         return output ? 2 : 0;
      }
   }

   private boolean neighborCanOutput(Level level, BlockPos pos, Direction side) {
      if (level.m_7702_(pos) instanceof TorqueAccess torque && torque.canOutputTorque(side)) {
         return true;
      }

      return false;
   }

   private boolean neighborCanInput(Level level, BlockPos pos, Direction side) {
      if (level.m_7702_(pos) instanceof TorqueAccess torque && torque.canInputTorque(side)) {
         return true;
      }

      return false;
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{FACING, CONNECTION_DOWN, CONNECTION_UP, CONNECTION_NORTH, CONNECTION_SOUTH, CONNECTION_WEST, CONNECTION_EAST});
   }

   public BlockState m_7417_(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      return super.m_7417_(state, direction, neighborState, level, pos, neighborPos);
   }

   private BlockState setDefaultConnections(BlockState state) {
      return (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)state.m_61124_(CONNECTION_DOWN, false))
                     .m_61124_(CONNECTION_UP, false))
                  .m_61124_(CONNECTION_NORTH, false))
               .m_61124_(CONNECTION_SOUTH, false))
            .m_61124_(CONNECTION_WEST, false))
         .m_61124_(CONNECTION_EAST, false);
   }

   public BlockState updateConnections(Level level, BlockPos pos, BlockState state) {
      if (this.kind == TorqueTransportKind.SHAFT) {
         return state;
      } else {
         BlockState updated = state;

         for (Direction direction : Direction.values()) {
            updated = (BlockState)updated.m_61124_(connectionProperty(direction), this.hasConnection(level, pos, state, direction));
         }

         return updated;
      }
   }

   public BlockState toggleConnection(BlockState state, Direction direction) {
      if (this.kind == TorqueTransportKind.SHAFT) {
         return state;
      } else {
         BooleanProperty property = connectionProperty(direction);
         return (BlockState)state.m_61124_(property, !(Boolean)state.m_61143_(property));
      }
   }

   public boolean isConnected(BlockState state, Direction direction) {
      return this.kind == TorqueTransportKind.SHAFT || (Boolean)state.m_61143_(connectionProperty(direction));
   }

   private boolean hasConnection(Level level, BlockPos pos, BlockState state, Direction direction) {
      if (!(level.m_7702_(pos.m_121945_(direction)) instanceof TorqueAccess torque)) {
         return false;
      } else {
         Direction neighborSide = direction.m_122424_();
         return this.canOutputFromState(state, direction) && torque.canInputTorque(neighborSide)
            || this.canInputFromState(state, direction) && torque.canOutputTorque(neighborSide);
      }
   }

   private boolean canOutputFromState(BlockState state, Direction direction) {
      Direction facing = (Direction)state.m_61143_(FACING);
      return this.kind == TorqueTransportKind.DISTRIBUTOR ? direction != facing.m_122424_() : direction == facing;
   }

   private boolean canInputFromState(BlockState state, Direction direction) {
      Direction facing = (Direction)state.m_61143_(FACING);
      return this.kind == TorqueTransportKind.DISTRIBUTOR ? direction == facing.m_122424_() : direction != facing;
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

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!player.m_21120_(hand).m_41619_()) {
         return InteractionResult.PASS;
      } else {
         if (!level.f_46443_ && level.m_7702_(pos) instanceof TorqueTransportBlockEntity torque) {
            if (this.kind == TorqueTransportKind.JUNCTION || this.kind == TorqueTransportKind.DISTRIBUTOR) {
               Direction clickedFace = hit.m_82434_();
               boolean output = this.kind == TorqueTransportKind.DISTRIBUTOR
                  ? clickedFace != ((Direction)state.m_61143_(FACING)).m_122424_()
                  : clickedFace == state.m_61143_(FACING);
               boolean connected = this.isConnected(state, clickedFace);
               player.m_5661_(
                  Component.m_237110_(
                     output ? "guistrings.automation.torque.face.output" : "guistrings.automation.torque.face.input",
                     new Object[]{
                        clickedFace.m_122433_(),
                        connected
                           ? Component.m_237115_("guistrings.automation.torque.face.connected")
                           : Component.m_237115_("guistrings.automation.torque.face.disconnected")
                     }
                  ),
                  true
               );
               return InteractionResult.m_19078_(false);
            }

            TorqueStats stats = torque.getStats();
            player.m_5661_(
               Component.m_237110_(
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

         return InteractionResult.m_19078_(level.f_46443_);
      }
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      return !level.f_46443_ && type == AWCoreRegistryAccess.blockEntityType(AWCoreIds.id("torque_transport"))
         ? (tickerLevel, pos, tickerState, blockEntity) -> TorqueTransportBlockEntity.serverTick(
               tickerLevel, pos, tickerState, (TorqueTransportBlockEntity)blockEntity
            )
         : null;
   }

   public RenderShape m_7514_(BlockState state) {
      return this.kind != TorqueTransportKind.SHAFT && this.kind != TorqueTransportKind.JUNCTION && this.kind != TorqueTransportKind.DISTRIBUTOR
         ? RenderShape.MODEL
         : RenderShape.ENTITYBLOCK_ANIMATED;
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return this.kind == TorqueTransportKind.SHAFT ? shaftShape((Direction)state.m_61143_(FACING)) : CENTER_SHAPE;
   }

   public VoxelShape m_6079_(BlockState state, BlockGetter level, BlockPos pos) {
      return this.kind == TorqueTransportKind.SHAFT ? this.m_5940_(state, level, pos, CollisionContext.m_82749_()) : Shapes.m_83144_();
   }

   private static VoxelShape shaftShape(Direction facing) {
      return switch (facing.m_122434_()) {
         case X -> SHAFT_EAST_WEST_SHAPE;
         case Y -> SHAFT_VERTICAL_SHAPE;
         case Z -> SHAFT_NORTH_SOUTH_SHAPE;
         default -> throw new IncompatibleClassChangeError();
      };
   }
}
