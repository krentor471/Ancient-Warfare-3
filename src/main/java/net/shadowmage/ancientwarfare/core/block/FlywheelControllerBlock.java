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
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.f_54117_;
   private static final VoxelShape SHAPE = m_49796_(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   private final TorqueTier tier;

   public FlywheelControllerBlock(Properties properties, TorqueTier tier) {
      super(properties);
      this.tier = tier;
      this.m_49959_((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(FACING, Direction.NORTH));
   }

   public TorqueTier getTier() {
      return this.tier;
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new FlywheelControllerBlockEntity(pos, state);
   }

   @Nullable
   public BlockState m_5573_(BlockPlaceContext context) {
      return (BlockState)this.m_49966_().m_61124_(FACING, context.m_8125_());
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{FACING});
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!player.m_21120_(hand).m_41619_()) {
         return InteractionResult.PASS;
      } else {
         if (!level.f_46443_ && level.m_7702_(pos) instanceof FlywheelControllerBlockEntity controller) {
            TorqueStats stats = controller.getStats();
            player.m_5661_(
               Component.m_237110_(
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

         return InteractionResult.m_19078_(level.f_46443_);
      }
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      return !level.f_46443_ && type == AWCoreRegistryAccess.blockEntityType(AWCoreIds.id("flywheel_controller"))
         ? (tickerLevel, pos, tickerState, blockEntity) -> FlywheelControllerBlockEntity.serverTick(
               tickerLevel, pos, tickerState, (FlywheelControllerBlockEntity)blockEntity
            )
         : null;
   }

   public RenderShape m_7514_(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }
}
