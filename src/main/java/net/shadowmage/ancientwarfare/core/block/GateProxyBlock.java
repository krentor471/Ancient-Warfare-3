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
   public static final BooleanProperty OPEN = BooleanProperty.create("open");
   public static final BooleanProperty BRIDGE = BooleanProperty.create("bridge");
   private static final VoxelShape BRIDGE_SHAPE = box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);

   public GateProxyBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(OPEN, false)).setValue(BRIDGE, false));
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.INVISIBLE;
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return state.getValue(BRIDGE) ? BRIDGE_SHAPE : Shapes.block();
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return shapeFor(state);
   }

   private static VoxelShape shapeFor(BlockState state) {
      if ((Boolean)state.getValue(OPEN)) {
         return Shapes.empty();
      } else {
         return state.getValue(BRIDGE) ? BRIDGE_SHAPE : Shapes.block();
      }
   }

   public void attack(BlockState state, Level level, BlockPos pos, Player player) {
      super.attack(state, level, pos, player);
      if (!level.isClientSide && level.getBlockEntity(pos) instanceof GateProxyBlockEntity proxy && proxy.getGate((ServerLevel)level) instanceof GateEntity gate) {
         player.attack(gate);
      }
   }

   public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
      return true;
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      return !level.isClientSide && level.getBlockEntity(pos) instanceof GateProxyBlockEntity proxy && proxy.getGate((ServerLevel)level) instanceof GateEntity gate
         ? gate.interact(player, hand)
         : InteractionResult.sidedSuccess(level.isClientSide);
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      super.neighborChanged(state, level, pos, block, fromPos, moving);
      if (!level.isClientSide && level.getBlockEntity(pos) instanceof GateProxyBlockEntity proxy && proxy.getGate((ServerLevel)level) instanceof GateEntity gate) {
         gate.refreshRedstonePower();
      }
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new GateProxyBlockEntity(pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : createTickerHelper(type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.GATE_PROXY), GateProxyBlockEntity::serverTick);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{OPEN, BRIDGE});
   }
}
