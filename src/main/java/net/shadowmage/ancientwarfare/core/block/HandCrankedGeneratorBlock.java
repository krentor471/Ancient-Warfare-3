package net.shadowmage.ancientwarfare.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.shadowmage.ancientwarfare.core.tile.HandCrankedGeneratorBlockEntity;
import org.jetbrains.annotations.Nullable;

public class HandCrankedGeneratorBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.f_54117_;
   private static final VoxelShape SHAPE = m_49796_(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);

   public HandCrankedGeneratorBlock(Properties properties) {
      super(properties);
      this.m_49959_((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(FACING, Direction.NORTH));
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new HandCrankedGeneratorBlockEntity(pos, state);
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
         if (!level.f_46443_ && level.m_7702_(pos) instanceof HandCrankedGeneratorBlockEntity generator) {
            double accepted = generator.crankByPlayer();
            TorqueStats stats = generator.getStats();
            level.m_5594_(null, pos, SoundEvents.f_12629_, SoundSource.BLOCKS, 0.25F, 0.75F + level.f_46441_.m_188501_() * 0.2F);
            player.m_5661_(
               Component.m_237110_(
                  "guistrings.automation.torque.cranked",
                  new Object[]{String.format("%.2f", accepted), String.format("%.2f", stats.stored()), String.format("%.2f", stats.output())}
               ),
               true
            );
         }

         return InteractionResult.m_19078_(level.f_46443_);
      }
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      return !level.f_46443_ && type == AWCoreRegistryAccess.blockEntityType(AWCoreIds.id("hand_cranked_generator"))
         ? (tickerLevel, pos, tickerState, blockEntity) -> HandCrankedGeneratorBlockEntity.serverTick(
               tickerLevel, pos, tickerState, (HandCrankedGeneratorBlockEntity)blockEntity
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
