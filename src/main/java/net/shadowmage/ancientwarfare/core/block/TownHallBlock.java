package net.shadowmage.ancientwarfare.core.block;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.tile.TownHallBlockEntity;
import org.jetbrains.annotations.Nullable;

public class TownHallBlock extends BaseEntityBlock {
   private static final VoxelShape SHAPE = m_49796_(0.0, 0.0, 0.0, 16.0, 32.0, 16.0);

   public TownHallBlock(Properties properties) {
      super(properties);
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new TownHallBlockEntity(pos, state);
   }

   public void m_6402_(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      if (!level.f_46443_ && placer instanceof Player player && level.m_7702_(pos) instanceof TownHallBlockEntity townHall) {
         townHall.setOwner(player.m_20148_());
      }
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!level.f_46443_ && level.m_7702_(pos) instanceof MenuProvider provider) {
         AWCorePlatform.openBlockMenu(player, provider, pos);
      }

      return InteractionResult.m_19078_(level.f_46443_);
   }

   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      if (!state.m_60713_(newState.m_60734_()) && level.m_7702_(pos) instanceof Container container) {
         Containers.m_19002_(level, pos, container);
      }

      super.m_6810_(state, level, pos, newState, movedByPiston);
   }

   public RenderShape m_7514_(BlockState state) {
      return RenderShape.MODEL;
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      return level.f_46443_ ? null : m_152132_(type, AWCoreRegistryAccess.blockEntityType(AWCoreIds.TOWN_HALL), TownHallBlockEntity::serverTick);
   }
}
