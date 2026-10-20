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
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.f_54117_;
   public static final BooleanProperty OPEN = BooleanProperty.m_61465_("open");
   public static final BooleanProperty POWERED = BooleanProperty.m_61465_("powered");
   private static final VoxelShape CLOSED_NORTH_SOUTH = Block.m_49796_(0.0, 0.0, 7.0, 16.0, 32.0, 9.0);
   private static final VoxelShape CLOSED_EAST_WEST = Block.m_49796_(7.0, 0.0, 0.0, 9.0, 32.0, 16.0);
   private final boolean metal;

   public StaticGateBlock(Properties properties, boolean metal) {
      super(properties);
      this.metal = metal;
      this.m_49959_(
         (BlockState)((BlockState)((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(FACING, Direction.NORTH)).m_61124_(OPEN, false))
            .m_61124_(POWERED, false)
      );
   }

   public BlockState m_5573_(BlockPlaceContext context) {
      boolean powered = context.m_43725_().m_276867_(context.m_8083_());
      return (BlockState)((BlockState)((BlockState)this.m_49966_().m_61124_(FACING, context.m_8125_().m_122424_())).m_61124_(OPEN, powered))
         .m_61124_(POWERED, powered);
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if ((Boolean)state.m_61143_(POWERED)) {
         return InteractionResult.m_19078_(level.f_46443_);
      } else {
         boolean open = !(Boolean)state.m_61143_(OPEN);
         level.m_7731_(pos, (BlockState)state.m_61124_(OPEN, open), 10);
         this.playGateSound(level, pos, open);
         return InteractionResult.m_19078_(level.f_46443_);
      }
   }

   public void m_6861_(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      if (!level.f_46443_) {
         boolean powered = level.m_276867_(pos);
         if (powered != (Boolean)state.m_61143_(POWERED)) {
            boolean wasOpen = (Boolean)state.m_61143_(OPEN);
            level.m_7731_(pos, (BlockState)((BlockState)state.m_61124_(POWERED, powered)).m_61124_(OPEN, powered), 2);
            if (wasOpen != powered) {
               this.playGateSound(level, pos, powered);
            }
         }
      }
   }

   private void playGateSound(Level level, BlockPos pos, boolean open) {
      level.m_5594_(
         null,
         pos,
         this.metal ? (open ? SoundEvents.f_12056_ : SoundEvents.f_12055_) : (open ? SoundEvents.f_12627_ : SoundEvents.f_12626_),
         SoundSource.BLOCKS,
         0.8F,
         open ? 1.0F : 0.8F
      );
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return state.m_61143_(OPEN) ? Shapes.m_83144_() : this.closedShape(state);
   }

   public VoxelShape m_5939_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return state.m_61143_(OPEN) ? Shapes.m_83040_() : this.closedShape(state);
   }

   private VoxelShape closedShape(BlockState state) {
      Direction facing = (Direction)state.m_61143_(FACING);
      return facing.m_122434_() == Axis.X ? CLOSED_EAST_WEST : CLOSED_NORTH_SOUTH;
   }

   public BlockState m_6843_(BlockState state, Rotation rotation) {
      return (BlockState)state.m_61124_(FACING, rotation.m_55954_((Direction)state.m_61143_(FACING)));
   }

   public BlockState m_6943_(BlockState state, Mirror mirror) {
      return state.m_60717_(mirror.m_54846_((Direction)state.m_61143_(FACING)));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{FACING, OPEN, POWERED});
   }
}
