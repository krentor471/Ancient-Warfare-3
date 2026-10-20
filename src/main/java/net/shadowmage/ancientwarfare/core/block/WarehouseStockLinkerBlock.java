package net.shadowmage.ancientwarfare.core.block;

import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.shadowmage.ancientwarfare.core.tile.WarehouseStockLinkerBlockEntity;

public class WarehouseStockLinkerBlock extends WarehouseBlock {
   public static final BooleanProperty LIT = BooleanProperty.create("lit");

   public WarehouseStockLinkerBlock(Properties properties, BiFunction<BlockPos, BlockState, BlockEntity> blockEntityFactory) {
      super(properties, blockEntityFactory);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(LIT, false));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, LIT});
   }

   public boolean isSignalSource(BlockState state) {
      return true;
   }

   public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      if ((Boolean)state.getValue(LIT)) {
         return 15;
      } else {
         if (level.getBlockEntity(pos) instanceof WarehouseStockLinkerBlockEntity linker && linker.isActive()) {
            return 15;
         }

         return 0;
      }
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      super.neighborChanged(state, level, pos, block, fromPos, moving);
      level.updateNeighborsAt(pos, this);
   }
}
