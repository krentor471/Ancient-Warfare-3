package net.shadowmage.ancientwarfare.core.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.entity.GateEntity;
import net.shadowmage.ancientwarfare.core.tile.GateProxyBlockEntity;

public class GateProxyBlock extends BaseEntityBlock {
   public static final BooleanProperty OPEN = BooleanProperty.m_61465_("open");
   public static final BooleanProperty BRIDGE = BooleanProperty.m_61465_("bridge");
   private static final VoxelShape BRIDGE_SHAPE = m_49796_(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);

   public GateProxyBlock(Properties properties) {
      super(properties);
      this.m_49959_((BlockState)((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(OPEN, false)).m_61124_(BRIDGE, false));
   }

   public RenderShape m_7514_(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return state.m_61143_(BRIDGE) ? BRIDGE_SHAPE : Shapes.m_83144_();
   }

   public VoxelShape m_5939_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return shapeFor(state);
   }

   private static VoxelShape shapeFor(BlockState state) {
      if ((Boolean)state.m_61143_(OPEN)) {
         return Shapes.m_83040_();
      } else {
         return state.m_61143_(BRIDGE) ? BRIDGE_SHAPE : Shapes.m_83144_();
      }
   }

   public void m_6256_(BlockState state, Level level, BlockPos pos, Player player) {
      super.m_6256_(state, level, pos, player);
      if (!level.f_46443_ && level.m_7702_(pos) instanceof GateProxyBlockEntity proxy && proxy.getGate((ServerLevel)level) instanceof GateEntity gate) {
         player.m_5706_(gate);
      }
   }

   public boolean m_7420_(BlockState state, BlockGetter level, BlockPos pos) {
      return true;
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      return !level.f_46443_ && level.m_7702_(pos) instanceof GateProxyBlockEntity proxy && proxy.getGate((ServerLevel)level) instanceof GateEntity gate
         ? gate.m_6096_(player, hand)
         : InteractionResult.m_19078_(level.f_46443_);
   }

   public void m_6861_(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      super.m_6861_(state, level, pos, block, fromPos, moving);
      if (!level.f_46443_ && level.m_7702_(pos) instanceof GateProxyBlockEntity proxy && proxy.getGate((ServerLevel)level) instanceof GateEntity gate) {
         gate.refreshRedstonePower();
      }
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new GateProxyBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      return level.f_46443_ ? null : m_152132_(type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.GATE_PROXY), GateProxyBlockEntity::serverTick);
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{OPEN, BRIDGE});
   }
}
