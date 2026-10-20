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

   public void m_6402_(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      super.m_6402_(level, pos, state, placer, stack);
      if (!level.f_46443_) {
         String template;
         Direction facing;
         boolean var10000;
         label25: {
            template = AwsTemplateBlockItem.template(stack);
            facing = placer == null ? Direction.NORTH : placer.m_6350_();
            if (placer instanceof Player player && player.m_150110_().f_35937_) {
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
            level.m_7471_(pos, false);
         }
      }
   }
}
