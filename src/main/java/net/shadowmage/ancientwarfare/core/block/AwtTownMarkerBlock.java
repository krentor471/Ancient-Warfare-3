package net.shadowmage.ancientwarfare.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.shadowmage.ancientwarfare.core.structure.AwtTownBlockItem;
import net.shadowmage.ancientwarfare.core.structure.AwtTownPlacer;
import org.jetbrains.annotations.Nullable;

public class AwtTownMarkerBlock extends Block {
   public AwtTownMarkerBlock(Properties properties) {
      super(properties);
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(level, pos, state, placer, stack);
      if (!level.isClientSide) {
         String template = AwtTownBlockItem.template(stack);
         Direction facing = placer == null ? Direction.NORTH : placer.getDirection();
         if (!AwtTownPlacer.place(level, pos, template, facing)) {
            level.removeBlock(pos, false);
         }
      }
   }
}
