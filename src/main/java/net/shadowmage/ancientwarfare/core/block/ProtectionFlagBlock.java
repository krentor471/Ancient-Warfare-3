package net.shadowmage.ancientwarfare.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shadowmage.ancientwarfare.core.tile.ProtectionFlagBlockEntity;
import org.jetbrains.annotations.Nullable;

public class ProtectionFlagBlock extends BaseEntityBlock {
   public static final IntegerProperty ROTATION = IntegerProperty.m_61631_("rotation", 0, 15);
   private static final VoxelShape SHAPE = m_49796_(5.0, 0.0, 5.0, 11.0, 16.0, 11.0);

   public ProtectionFlagBlock(Properties properties) {
      super(properties);
      this.m_49959_((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(ROTATION, 0));
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new ProtectionFlagBlockEntity(pos, state);
   }

   public void m_6402_(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      if (!level.f_46443_ && level.m_7702_(pos) instanceof ProtectionFlagBlockEntity flag) {
         flag.loadFromItem(stack);
      }
   }

   public BlockState m_5573_(BlockPlaceContext context) {
      int rotation = Mth.m_14107_((double)((180.0F + context.m_7074_()) * 16.0F / 360.0F) + 0.5) & 15;
      return (BlockState)this.m_49966_().m_61124_(ROTATION, rotation);
   }

   public RenderShape m_7514_(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   public BlockState m_6843_(BlockState state, Rotation rotation) {
      return (BlockState)state.m_61124_(ROTATION, rotation.m_55949_((Integer)state.m_61143_(ROTATION), 16));
   }

   public BlockState m_6943_(BlockState state, Mirror mirror) {
      return this.m_6843_(state, mirror.m_54846_(Direction.m_122407_((Integer)state.m_61143_(ROTATION) / 4)));
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   public VoxelShape m_5939_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.m_83040_();
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{ROTATION});
   }
}
