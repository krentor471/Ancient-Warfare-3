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
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

   public SurvivalStructureBuilderBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new SurvivalStructureBuilderBlockEntity(pos, state);
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide
         ? null
         : createTickerHelper(type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.STRUCTURE_BUILDER_TICKED), SurvivalStructureBuilderBlockEntity::serverTick);
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      if (!level.isClientSide && level.getBlockEntity(pos) instanceof SurvivalStructureBuilderBlockEntity builder) {
         builder.configure(stack, (Direction)state.getValue(FACING));
      }
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!(level.getBlockEntity(pos) instanceof SurvivalStructureBuilderBlockEntity builder)) {
         return InteractionResult.PASS;
      } else {
         ItemStack held = player.getItemInHand(hand);
         boolean workingHammer = held.getItem() instanceof HammerItem && HammerModeData.isWorkMode(held);
         if (!level.isClientSide) {
            if (workingHammer && builder.addPlayerWork()) {
               held.hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(hand));
               player.swing(hand, true);
            }

            player.displayClientMessage(builder.progressMessage(), true);
         }

         return InteractionResult.sidedSuccess(level.isClientSide);
      }
   }

   public List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
      BlockEntity blockEntity = (BlockEntity)params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
      return blockEntity instanceof SurvivalStructureBuilderBlockEntity builder ? List.of(builder.createProgressStack()) : List.of(new ItemStack(this));
   }

   public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof SurvivalStructureBuilderBlockEntity builder) {
         ItemStack saved = builder.createProgressStack();
         if (!saved.getOrCreateTag().getString("TemplateName").isBlank()) {
            level.addFreshEntity(new ItemEntity(level, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, saved));
         }
      }

      super.playerWillDestroy(level, pos, state, player);
   }
}
