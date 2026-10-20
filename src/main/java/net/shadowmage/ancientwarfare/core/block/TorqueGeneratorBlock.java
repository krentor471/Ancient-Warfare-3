package net.shadowmage.ancientwarfare.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.shadowmage.ancientwarfare.core.common.torque.TorqueGeneratorKind;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueStats;
import net.shadowmage.ancientwarfare.core.tile.TorqueGeneratorBlockEntity;
import net.shadowmage.ancientwarfare.core.tile.WindmillBladeBlockEntity;
import org.jetbrains.annotations.Nullable;

public class TorqueGeneratorBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   private static final VoxelShape SHAPE = box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   private final TorqueGeneratorKind kind;

   public TorqueGeneratorBlock(Properties properties, TorqueGeneratorKind kind) {
      super(properties);
      this.kind = kind;
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   public TorqueGeneratorKind getKind() {
      return this.kind;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new TorqueGeneratorBlockEntity(pos, state);
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (level.getBlockEntity(pos) instanceof TorqueGeneratorBlockEntity generator) {
         ItemStack held = player.getItemInHand(hand);
         if (!level.isClientSide && this.kind == TorqueGeneratorKind.STIRLING && generator.tryInsertFuel(player, hand, held)) {
            return InteractionResult.CONSUME;
         }

         if (held.isEmpty()) {
            if (!level.isClientSide) {
               if (this.kind == TorqueGeneratorKind.WINDMILL) {
                  WindmillBladeBlockEntity.ToggleResult result = generator.toggleWindmillAssembly();
                  if (result == WindmillBladeBlockEntity.ToggleResult.STARTED) {
                     player.displayClientMessage(Component.literal("风车已组装并开始运行"), true);
                  } else if (result == WindmillBladeBlockEntity.ToggleResult.STOPPED) {
                     player.displayClientMessage(Component.literal("风车已停止并恢复为未组装状态"), true);
                  } else {
                     player.displayClientMessage(Component.literal("风车叶片摆放不正确，需要完整的 3x3、5x5 或 7x7 方阵"), true);
                  }

                  return InteractionResult.CONSUME;
               }

               TorqueStats stats = generator.getStats();
               player.displayClientMessage(
                  Component.translatable(
                     generator.getStatusTranslationKey(),
                     new Object[]{String.format("%.2f", stats.stored()), String.format("%.2f", stats.output()), generator.getStatusDetail()}
                  ),
                  true
               );
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
         }
      }

      return InteractionResult.PASS;
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : (tickerLevel, pos, tickerState, blockEntity) -> {
         if (blockEntity instanceof TorqueGeneratorBlockEntity generator) {
            TorqueGeneratorBlockEntity.serverTick(tickerLevel, pos, tickerState, generator);
         }
      };
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }
}
