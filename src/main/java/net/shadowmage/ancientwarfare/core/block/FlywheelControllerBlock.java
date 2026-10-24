package net.shadowmage.ancientwarfare.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueStats;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTier;
import net.shadowmage.ancientwarfare.core.tile.FlywheelControllerBlockEntity;
import org.jetbrains.annotations.Nullable;

public class FlywheelControllerBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   private static final VoxelShape SHAPE = box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   private final TorqueTier tier;

   public FlywheelControllerBlock(Properties properties, TorqueTier tier) {
      super(properties);
      this.tier = tier;
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   public TorqueTier getTier() {
      return this.tier;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new FlywheelControllerBlockEntity(pos, state);
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!player.getItemInHand(hand).isEmpty()) {
         return InteractionResult.PASS;
      } else {
         if (!level.isClientSide && level.getBlockEntity(pos) instanceof FlywheelControllerBlockEntity controller) {
            TorqueStats stats = controller.getStats();
            player.displayClientMessage(
               Component.translatable(
                  "guistrings.automation.flywheel.values",
                  new Object[]{
                     String.format("%.2f", stats.stored()),
                     String.format("%.2f", controller.getStorageStored()),
                     String.format("%.2f", controller.getStorageCapacity()),
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
      return !level.isClientSide && type == AWCoreRegistryAccess.blockEntityType(AWCoreIds.id("flywheel_controller"))
         ? (tickerLevel, pos, tickerState, blockEntity) -> FlywheelControllerBlockEntity.serverTick(
               tickerLevel, pos, tickerState, (FlywheelControllerBlockEntity)blockEntity
            )
         : null;
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }
}
