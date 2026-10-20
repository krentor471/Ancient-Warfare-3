package net.shadowmage.ancientwarfare.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.shadowmage.ancientwarfare.core.structure.AwsTemplateBlockItem;
import net.shadowmage.ancientwarfare.core.structure.AwsTemplatePlacer;
import org.jetbrains.annotations.Nullable;

public class UndeadCemeterySkullMarkerBlock extends Block {
   public UndeadCemeterySkullMarkerBlock(Properties properties) {
      super(properties);
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(level, pos, state, placer, stack);
      if (!level.isClientSide) {
         String template;
         Direction facing;
         boolean var10000;
         label25: {
            template = AwsTemplateBlockItem.template(stack);
            facing = placer == null ? Direction.NORTH : placer.getDirection();
            if (placer instanceof Player player && player.getAbilities().instabuild) {
               var10000 = true;
               break label25;
            }

            var10000 = false;
         }

         boolean creativePlacement = var10000;
         boolean placed = creativePlacement
            ? AwsTemplatePlacer.place(level, pos, template, facing)
            : AwsTemplatePlacer.placePlayerBuilt(level, pos, template, facing);
         if (!placed) {
            level.removeBlock(pos, false);
         }
      }
   }
}
