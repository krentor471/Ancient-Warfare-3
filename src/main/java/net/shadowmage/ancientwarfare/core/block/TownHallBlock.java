package net.shadowmage.ancientwarfare.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.faction.FactionArmies;
import net.shadowmage.ancientwarfare.core.tile.TownHallBlockEntity;
import org.jetbrains.annotations.Nullable;

public class TownHallBlock extends BaseEntityBlock {
   private static final VoxelShape SHAPE = box(0.0, 0.0, 0.0, 16.0, 32.0, 16.0);

   public TownHallBlock(Properties properties) {
      super(properties);
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new TownHallBlockEntity(pos, state);
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof TownHallBlockEntity townHall) {
         townHall.setOwner(player.getUUID());
         if (level instanceof ServerLevel server) {
            FactionArmies.onTownHallPlaced(server, pos, player.getUUID());
         }
      }
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!level.isClientSide && level.getBlockEntity(pos) instanceof MenuProvider provider) {
         AWCorePlatform.openBlockMenu(player, provider, pos);
      }

      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof Container container) {
         Containers.dropContents(level, pos, container);
      }

      if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
         FactionArmies.onTownHallRemoved(server, pos);
      }

      super.onRemove(state, level, pos, newState, movedByPiston);
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : createTickerHelper(type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.TOWN_HALL), TownHallBlockEntity::serverTick);
   }
}
