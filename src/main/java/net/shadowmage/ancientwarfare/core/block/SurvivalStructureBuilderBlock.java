package net.shadowmage.ancientwarfare.core.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.shadowmage.ancientwarfare.core.common.hammer.HammerModeData;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.item.HammerItem;
import net.shadowmage.ancientwarfare.core.tile.SurvivalStructureBuilderBlockEntity;
import org.jetbrains.annotations.Nullable;

public class SurvivalStructureBuilderBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = BlockStateProperties.f_61374_;

   public SurvivalStructureBuilderBlock(Properties properties) {
      super(properties);
      this.m_49959_((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(FACING, Direction.NORTH));
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new SurvivalStructureBuilderBlockEntity(pos, state);
   }

   public RenderShape m_7514_(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   public BlockState m_5573_(BlockPlaceContext context) {
      return (BlockState)this.m_49966_().m_61124_(FACING, context.m_8125_());
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{FACING});
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      return level.f_46443_
         ? null
         : m_152132_(type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.STRUCTURE_BUILDER_TICKED), SurvivalStructureBuilderBlockEntity::serverTick);
   }

   public void m_6402_(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      if (!level.f_46443_ && level.m_7702_(pos) instanceof SurvivalStructureBuilderBlockEntity builder) {
         builder.configure(stack, (Direction)state.m_61143_(FACING));
      }
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!(level.m_7702_(pos) instanceof SurvivalStructureBuilderBlockEntity builder)) {
         return InteractionResult.PASS;
      } else {
         ItemStack held = player.m_21120_(hand);
         boolean workingHammer = held.m_41720_() instanceof HammerItem && HammerModeData.isWorkMode(held);
         if (!level.f_46443_) {
            if (workingHammer && builder.addPlayerWork()) {
               held.m_41622_(1, player, entity -> entity.m_21190_(hand));
               player.m_21011_(hand, true);
            }

            player.m_5661_(builder.progressMessage(), true);
         }

         return InteractionResult.m_19078_(level.f_46443_);
      }
   }

   public List<ItemStack> m_49635_(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
      BlockEntity blockEntity = (BlockEntity)params.m_287159_(LootContextParams.f_81462_);
      return blockEntity instanceof SurvivalStructureBuilderBlockEntity builder ? List.of(builder.createProgressStack()) : List.of(new ItemStack(this));
   }

   public void m_5707_(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.f_46443_ && player.m_7500_() && level.m_7702_(pos) instanceof SurvivalStructureBuilderBlockEntity builder) {
         ItemStack saved = builder.createProgressStack();
         if (!saved.m_41784_().m_128461_("TemplateName").isBlank()) {
            level.m_7967_(new ItemEntity(level, (double)pos.m_123341_() + 0.5, (double)pos.m_123342_() + 0.5, (double)pos.m_123343_() + 0.5, saved));
         }
      }

      super.m_5707_(level, pos, state, player);
   }
}
