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
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.f_54117_;

   public MailboxBlock(Properties p) {
      super(p);
      this.m_49959_((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(FACING, Direction.NORTH));
   }

   public BlockEntity m_142194_(BlockPos p, BlockState s) {
      return new MailboxBlockEntity(p, s);
   }

   protected void m_7926_(Builder<Block, BlockState> b) {
      b.m_61104_(new Property[]{FACING});
   }

   public BlockState m_5573_(BlockPlaceContext c) {
      return (BlockState)this.m_49966_().m_61124_(FACING, c.m_8125_().m_122424_());
   }

   public void m_6402_(Level l, BlockPos p, BlockState s, LivingEntity e, ItemStack st) {
      super.m_6402_(l, p, s, e, st);
      if (l.m_7702_(p) instanceof MailboxBlockEntity b && e instanceof Player pl) {
         b.setOwnerName(pl.m_36316_().getName());
      }
   }

   public InteractionResult m_6227_(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
      if (!l.f_46443_ && l.m_7702_(p) instanceof MailboxBlockEntity b) {
         AWCorePlatform.openBlockMenu(pl, b, p);
      }

      return InteractionResult.m_19078_(l.f_46443_);
   }

   public RenderShape m_7514_(BlockState s) {
      return RenderShape.MODEL;
   }

   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level l, BlockState s, BlockEntityType<T> t) {
      return l.f_46443_ ? null : m_152132_(t, AWCoreRegistryAccess.blockEntityType(AWCoreIds.MAILBOX), MailboxBlockEntity::serverTick);
   }

   public void m_6810_(BlockState s, Level l, BlockPos p, BlockState n, boolean m) {
      if (!s.m_60713_(n.m_60734_()) && l.m_7702_(p) instanceof MailboxBlockEntity b) {
         drop(l, p, b.sendContainer());
         drop(l, p, b.receivedContainer());
      }

      super.m_6810_(s, l, p, n, m);
   }

   private static void drop(Level l, BlockPos p, Container c) {
      for (int i = 0; i < c.m_6643_(); i++) {
         ItemStack s = c.m_8020_(i);
         if (!s.m_41619_()) {
            Containers.m_18992_(l, (double)p.m_123341_() + 0.5, (double)p.m_123342_() + 0.5, (double)p.m_123343_() + 0.5, s);
         }
      }
   }
}
