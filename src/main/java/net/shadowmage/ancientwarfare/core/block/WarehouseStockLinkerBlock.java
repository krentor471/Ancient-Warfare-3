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
   public static final BooleanProperty LIT = BooleanProperty.m_61465_("lit");

   public WarehouseStockLinkerBlock(Properties properties, BiFunction<BlockPos, BlockState, BlockEntity> blockEntityFactory) {
      super(properties, blockEntityFactory);
      this.m_49959_((BlockState)this.m_49966_().m_61124_(LIT, false));
   }

   @Override
   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{FACING, LIT});
   }

   public boolean m_7899_(BlockState state) {
      return true;
   }

   public int m_6378_(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      if ((Boolean)state.m_61143_(LIT)) {
         return 15;
      } else {
         if (level.m_7702_(pos) instanceof WarehouseStockLinkerBlockEntity linker && linker.isActive()) {
            return 15;
         }

         return 0;
      }
   }

   public void m_6861_(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      super.m_6861_(state, level, pos, block, fromPos, moving);
      level.m_46672_(pos, this);
   }
}
