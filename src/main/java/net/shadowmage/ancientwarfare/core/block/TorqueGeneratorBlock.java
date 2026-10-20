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
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.f_54117_;
   private static final VoxelShape SHAPE = m_49796_(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   private final TorqueGeneratorKind kind;

   public TorqueGeneratorBlock(Properties properties, TorqueGeneratorKind kind) {
      super(properties);
      this.kind = kind;
      this.m_49959_((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(FACING, Direction.NORTH));
   }

   public TorqueGeneratorKind getKind() {
      return this.kind;
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new TorqueGeneratorBlockEntity(pos, state);
   }

   @Nullable
   public BlockState m_5573_(BlockPlaceContext context) {
      return (BlockState)this.m_49966_().m_61124_(FACING, context.m_8125_());
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{FACING});
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (level.m_7702_(pos) instanceof TorqueGeneratorBlockEntity generator) {
         ItemStack held = player.m_21120_(hand);
         if (!level.f_46443_ && this.kind == TorqueGeneratorKind.STIRLING && generator.tryInsertFuel(player, hand, held)) {
            return InteractionResult.CONSUME;
         }

         if (held.m_41619_()) {
            if (!level.f_46443_) {
               if (this.kind == TorqueGeneratorKind.WINDMILL) {
                  WindmillBladeBlockEntity.ToggleResult result = generator.toggleWindmillAssembly();
                  if (result == WindmillBladeBlockEntity.ToggleResult.STARTED) {
                     player.m_5661_(Component.m_237113_("风车已组装并开始运行"), true);
                  } else if (result == WindmillBladeBlockEntity.ToggleResult.STOPPED) {
                     player.m_5661_(Component.m_237113_("风车已停止并恢复为未组装状态"), true);
                  } else {
                     player.m_5661_(Component.m_237113_("风车叶片摆放不正确，需要完整的 3x3、5x5 或 7x7 方阵"), true);
                  }

                  return InteractionResult.CONSUME;
               }

               TorqueStats stats = generator.getStats();
               player.m_5661_(
                  Component.m_237110_(
                     generator.getStatusTranslationKey(),
                     new Object[]{String.format("%.2f", stats.stored()), String.format("%.2f", stats.output()), generator.getStatusDetail()}
                  ),
                  true
               );
            }

            return InteractionResult.m_19078_(level.f_46443_);
         }
      }

      return InteractionResult.PASS;
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      return level.f_46443_ ? null : (tickerLevel, pos, tickerState, blockEntity) -> {
         if (blockEntity instanceof TorqueGeneratorBlockEntity generator) {
            TorqueGeneratorBlockEntity.serverTick(tickerLevel, pos, tickerState, generator);
         }
      };
   }

   public RenderShape m_7514_(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }
}
