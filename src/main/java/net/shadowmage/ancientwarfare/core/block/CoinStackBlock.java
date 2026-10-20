package net.shadowmage.ancientwarfare.core.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shadowmage.ancientwarfare.core.common.npc.CoinMetal;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.item.CoinItem;

public class CoinStackBlock extends Block {
   public static final EnumProperty<CoinStackBlock.StackSize> SIZE = EnumProperty.m_61587_("size", CoinStackBlock.StackSize.class);
   private static final VoxelShape[] SHAPES = new VoxelShape[]{
      Block.m_49796_(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
      Block.m_49796_(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),
      Block.m_49796_(0.0, 0.0, 0.0, 16.0, 6.0, 16.0),
      Block.m_49796_(0.0, 0.0, 0.0, 16.0, 9.0, 16.0),
      Block.m_49796_(0.0, 0.0, 0.0, 16.0, 13.0, 16.0),
      Block.m_49796_(0.0, 0.0, 0.0, 16.0, 13.0, 16.0),
      Block.m_49796_(0.0, 0.0, 0.0, 16.0, 15.0, 16.0),
      Block.m_49796_(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)
   };
   private final CoinMetal metal;

   public CoinStackBlock(Properties properties, CoinMetal metal) {
      super(properties);
      this.metal = metal;
      this.m_49959_((BlockState)((BlockState)this.f_49792_.m_61090_()).m_61124_(SIZE, CoinStackBlock.StackSize.EIGHT));
   }

   public CoinMetal metal() {
      return this.metal;
   }

   public static int stackSize(ItemStack stack) {
      return stack.m_41782_() ? Math.max(8, Math.min(64, stack.m_41783_().m_128451_("size"))) : 8;
   }

   public ItemStack stackItem(int size) {
      ItemStack stack = new ItemStack(this.m_5456_());
      stack.m_41784_().m_128405_("size", Math.max(8, Math.min(64, size)));
      return stack;
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{SIZE});
   }

   public BlockState m_5573_(BlockPlaceContext context) {
      return (BlockState)this.m_49966_().m_61124_(SIZE, CoinStackBlock.StackSize.byCoins(stackSize(context.m_43722_())));
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPES[((CoinStackBlock.StackSize)state.m_61143_(SIZE)).ordinal()];
   }

   public List<ItemStack> m_49635_(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
      return List.of(CoinItem.create(this.metal, ((CoinStackBlock.StackSize)state.m_61143_(SIZE)).coins));
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      ItemStack held = player.m_21120_(hand);
      if (level.f_46443_) {
         return InteractionResult.SUCCESS;
      } else {
         CoinStackBlock.StackSize size = (CoinStackBlock.StackSize)state.m_61143_(SIZE);
         boolean matchingCoin = held.m_150930_(AWCoreRegistryAccess.item(AWCoreIds.COIN)) && CoinItem.getMetal(held) == this.metal;
         if (matchingCoin) {
            if (held.m_41613_() >= 8 && size != CoinStackBlock.StackSize.SIXTY_FOUR) {
               level.m_7731_(pos, (BlockState)state.m_61124_(SIZE, size.next()), 3);
               if (!player.m_150110_().f_35937_) {
                  held.m_41774_(8);
               }
            }
         } else {
            ItemStack coins = CoinItem.create(this.metal, 8);
            if (!player.m_36356_(coins)) {
               player.m_36176_(coins, false);
            }

            if (size == CoinStackBlock.StackSize.EIGHT) {
               level.m_7471_(pos, false);
            } else {
               level.m_7731_(pos, (BlockState)state.m_61124_(SIZE, size.previous()), 3);
            }
         }

         level.m_5594_(null, pos, SoundEvents.f_11745_, SoundSource.BLOCKS, 0.5F, 1.3F);
         return InteractionResult.CONSUME;
      }
   }

   public static enum StackSize implements StringRepresentable {
      EIGHT(8),
      SIXTEEN(16),
      TWENTY_FOUR(24),
      THIRTY_TWO(32),
      FORTY(40),
      FORTY_EIGHT(48),
      FIFTY_SIX(56),
      SIXTY_FOUR(64);

      private final int coins;

      private StackSize(int coins) {
         this.coins = coins;
      }

      public int coins() {
         return this.coins;
      }

      public CoinStackBlock.StackSize next() {
         return values()[Math.min(values().length - 1, this.ordinal() + 1)];
      }

      public CoinStackBlock.StackSize previous() {
         return values()[Math.max(0, this.ordinal() - 1)];
      }

      public static CoinStackBlock.StackSize byCoins(int coins) {
         int normalized = Math.max(8, Math.min(64, (coins + 7) / 8 * 8));

         for (CoinStackBlock.StackSize size : values()) {
            if (size.coins == normalized) {
               return size;
            }
         }

         return EIGHT;
      }

      public String m_7912_() {
         return Integer.toString(this.coins);
      }
   }
}
