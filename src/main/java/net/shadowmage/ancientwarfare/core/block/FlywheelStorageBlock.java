package net.shadowmage.ancientwarfare.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTier;
import net.shadowmage.ancientwarfare.core.tile.FlywheelStorageBlockEntity;
import org.jetbrains.annotations.Nullable;

public class FlywheelStorageBlock extends BaseEntityBlock {
   private static final VoxelShape SHAPE = m_49796_(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   private final TorqueTier tier;

   public FlywheelStorageBlock(Properties properties, TorqueTier tier) {
      super(properties);
      this.tier = tier;
   }

   public TorqueTier getTier() {
      return this.tier;
   }

   @Nullable
   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new FlywheelStorageBlockEntity(pos, state);
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!player.m_21120_(hand).m_41619_()) {
         return InteractionResult.PASS;
      } else {
         if (!level.f_46443_ && level.m_7702_(pos) instanceof FlywheelStorageBlockEntity storage) {
            player.m_5661_(Component.m_237110_("guistrings.automation.flywheel.storage", new Object[]{String.format("%.2f", storage.getStoredTorque())}), true);
         }

         return InteractionResult.m_19078_(level.f_46443_);
      }
   }

   public RenderShape m_7514_(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }
}
