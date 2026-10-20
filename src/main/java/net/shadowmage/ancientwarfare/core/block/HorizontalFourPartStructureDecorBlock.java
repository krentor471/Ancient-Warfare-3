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

public class HorizontalFourPartStructureDecorBlock extends HorizontalDirectionalBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.f_54117_;
   public static final IntegerProperty PART = IntegerProperty.m_61631_("part", 0, 3);

   public HorizontalFourPartStructureDecorBlock(Properties properties) {
      super(properties);
      this.m_49959_((BlockState)((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(FACING, Direction.NORTH)).m_61124_(PART, 0));
   }

   public BlockState m_5573_(BlockPlaceContext context) {
      Direction facing = context.m_8125_().m_122424_();
      Direction extension = facing.m_122424_();
      BlockPos pos = context.m_8083_();

      for (int offset = 1; offset < 4; offset++) {
         if (!context.m_43725_().m_8055_(pos.m_5484_(extension, offset)).m_60629_(context)) {
            return null;
         }
      }

      return (BlockState)((BlockState)this.m_49966_().m_61124_(FACING, facing)).m_61124_(PART, 0);
   }

   public void m_6402_(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      Direction extension = ((Direction)state.m_61143_(FACING)).m_122424_();

      for (int offset = 1; offset < 4; offset++) {
         level.m_7731_(pos.m_5484_(extension, offset), (BlockState)state.m_61124_(PART, offset), 3);
      }
   }

   public void m_5707_(Level level, BlockPos pos, BlockState state, Player player) {
      Direction extension = ((Direction)state.m_61143_(FACING)).m_122424_();
      BlockPos basePos = pos.m_5484_(extension.m_122424_(), (Integer)state.m_61143_(PART));

      for (int offset = 0; offset < 4; offset++) {
         BlockPos partPos = basePos.m_5484_(extension, offset);
         if (!partPos.equals(pos) && level.m_8055_(partPos).m_60713_(this)) {
            level.m_46953_(partPos, false, player);
         }
      }

      super.m_5707_(level, pos, state, player);
   }

   public BlockState m_7417_(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      Direction extension = ((Direction)state.m_61143_(FACING)).m_122424_();
      int part = (Integer)state.m_61143_(PART);
      if (part <= 0 || direction != extension.m_122424_() || neighborState.m_60713_(this) && (Integer)neighborState.m_61143_(PART) == part - 1) {
         return part >= 3 || direction != extension || neighborState.m_60713_(this) && neighborState.m_61143_(PART) == part + 1
            ? super.m_7417_(state, direction, neighborState, level, pos, neighborPos)
            : Blocks.f_50016_.m_49966_();
      } else {
         return Blocks.f_50016_.m_49966_();
      }
   }

   public RenderShape m_7514_(BlockState state) {
      return state.m_61143_(PART) != 0 && state.m_61143_(PART) != 2 ? RenderShape.INVISIBLE : RenderShape.MODEL;
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.m_83144_();
   }

   public BlockState m_6843_(BlockState state, Rotation rotation) {
      return (BlockState)state.m_61124_(FACING, rotation.m_55954_((Direction)state.m_61143_(FACING)));
   }

   public BlockState m_6943_(BlockState state, Mirror mirror) {
      return state.m_60717_(mirror.m_54846_((Direction)state.m_61143_(FACING)));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{FACING, PART});
   }
}
