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
   public static final EnumProperty<CoinStackBlock.StackSize> SIZE = EnumProperty.create("size", CoinStackBlock.StackSize.class);
   private static final VoxelShape[] SHAPES = new VoxelShape[]{
      Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
      Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0),
      Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0),
      Block.box(0.0, 0.0, 0.0, 16.0, 9.0, 16.0),
      Block.box(0.0, 0.0, 0.0, 16.0, 13.0, 16.0),
      Block.box(0.0, 0.0, 0.0, 16.0, 13.0, 16.0),
      Block.box(0.0, 0.0, 0.0, 16.0, 15.0, 16.0),
      Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)
   };
   private final CoinMetal metal;

   public CoinStackBlock(Properties properties, CoinMetal metal) {
      super(properties);
      this.metal = metal;
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(SIZE, CoinStackBlock.StackSize.EIGHT));
   }

   public CoinMetal metal() {
      return this.metal;
   }

   public static int stackSize(ItemStack stack) {
      return stack.hasTag() ? Math.max(8, Math.min(64, stack.getTag().getInt("size"))) : 8;
   }

   public ItemStack stackItem(int size) {
      ItemStack stack = new ItemStack(this.asItem());
      stack.getOrCreateTag().putInt("size", Math.max(8, Math.min(64, size)));
      return stack;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{SIZE});
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(SIZE, CoinStackBlock.StackSize.byCoins(stackSize(context.getItemInHand())));
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SHAPES[((CoinStackBlock.StackSize)state.getValue(SIZE)).ordinal()];
   }

   public List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
      return List.of(CoinItem.create(this.metal, ((CoinStackBlock.StackSize)state.getValue(SIZE)).coins));
   }

   public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      ItemStack held = player.getItemInHand(hand);
      if (level.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         CoinStackBlock.StackSize size = (CoinStackBlock.StackSize)state.getValue(SIZE);
         boolean matchingCoin = held.is(AWCoreRegistryAccess.item(AWCoreIds.COIN)) && CoinItem.getMetal(held) == this.metal;
         if (matchingCoin) {
            if (held.getCount() >= 8 && size != CoinStackBlock.StackSize.SIXTY_FOUR) {
               level.setBlock(pos, (BlockState)state.setValue(SIZE, size.next()), 3);
               if (!player.getAbilities().instabuild) {
                  held.shrink(8);
               }
            }
         } else {
            ItemStack coins = CoinItem.create(this.metal, 8);
            if (!player.addItem(coins)) {
               player.drop(coins, false);
            }

            if (size == CoinStackBlock.StackSize.EIGHT) {
               level.removeBlock(pos, false);
            } else {
               level.setBlock(pos, (BlockState)state.setValue(SIZE, size.previous()), 3);
            }
         }

         level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.5F, 1.3F);
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

      public String getSerializedName() {
         return Integer.toString(this.coins);
      }
   }
}
