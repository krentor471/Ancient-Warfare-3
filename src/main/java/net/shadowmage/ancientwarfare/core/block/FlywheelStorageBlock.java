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
   private static final VoxelShape SHAPE = box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   private final TorqueTier tier;

   public FlywheelStorageBlock(Properties properties, TorqueTier tier) {
      super(properties);
      this.tier = tier;
   }

   public TorqueTier getTier() {
      return this.tier;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new FlywheelStorageBlockEntity(pos, state);
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      if (!player.getItemInHand(hand).isEmpty()) {
         return InteractionResult.PASS;
      } else {
         if (!level.isClientSide && level.getBlockEntity(pos) instanceof FlywheelStorageBlockEntity storage) {
            player.displayClientMessage(Component.translatable("guistrings.automation.flywheel.storage", new Object[]{String.format("%.2f", storage.getStoredTorque())}), true);
         }

         return InteractionResult.sidedSuccess(level.isClientSide);
      }
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPE;
   }
}
