package net.shadowmage.ancientwarfare.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.tile.MailboxBlockEntity;

public class MailboxBlock extends BaseEntityBlock {
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

   public MailboxBlock(Properties p) {
      super(p);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH));
   }

   public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
      return new MailboxBlockEntity(p, s);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> b) {
      b.add(new Property[]{FACING});
   }

   public BlockState getStateForPlacement(BlockPlaceContext c) {
      return (BlockState)this.defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
   }

   public void setPlacedBy(Level l, BlockPos p, BlockState s, LivingEntity e, ItemStack st) {
      super.setPlacedBy(l, p, s, e, st);
      if (l.getBlockEntity(p) instanceof MailboxBlockEntity b && e instanceof Player pl) {
         b.setOwnerName(pl.getGameProfile().getName());
      }
   }

   public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
      if (!l.isClientSide && l.getBlockEntity(p) instanceof MailboxBlockEntity b) {
         AWCorePlatform.openBlockMenu(pl, b, p);
      }

      return InteractionResult.sidedSuccess(l.isClientSide);
   }

   public RenderShape getRenderShape(BlockState s) {
      return RenderShape.MODEL;
   }

   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
      return l.isClientSide ? null : createTickerHelper(t, AWCoreRegistryAccess.blockEntityType(AWCoreIds.MAILBOX), MailboxBlockEntity::serverTick);
   }

   public void onRemove(BlockState s, Level l, BlockPos p, BlockState n, boolean m) {
      if (!s.is(n.getBlock()) && l.getBlockEntity(p) instanceof MailboxBlockEntity b) {
         drop(l, p, b.sendContainer());
         drop(l, p, b.receivedContainer());
      }

      super.onRemove(s, l, p, n, m);
   }

   private static void drop(Level l, BlockPos p, Container c) {
      for (int i = 0; i < c.getContainerSize(); i++) {
         ItemStack s = c.getItem(i);
         if (!s.isEmpty()) {
            Containers.dropItemStack(l, (double)p.getX() + 0.5, (double)p.getY() + 0.5, (double)p.getZ() + 0.5, s);
         }
      }
   }
}
