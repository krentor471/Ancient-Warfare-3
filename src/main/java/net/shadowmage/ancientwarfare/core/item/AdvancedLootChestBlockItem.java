package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.block.AdvancedLootChestBlock;

public class AdvancedLootChestBlockItem extends BlockItem {
   private static final ResourceLocation DEFAULT_LOOT = new ResourceLocation("ancientwarfare", "chests/structure_low");

   public AdvancedLootChestBlockItem(Block block, Properties properties) {
      super(block, properties);
   }

   protected BlockState m_5965_(BlockPlaceContext context) {
      return Blocks.f_50087_.m_5573_(context);
   }

   public InteractionResult m_40576_(BlockPlaceContext context) {
      InteractionResult result = super.m_40576_(context);
      AncientWarfareCore.LOG
         .debug(
            "Advanced loot chest item place: pos={}, result={}, placedBlock={}, placedEntity={}",
            new Object[]{
               context.m_8083_(),
               result,
               context.m_43725_().m_8055_(context.m_8083_()).m_60734_(),
               context.m_43725_().m_7702_(context.m_8083_()) == null ? "null" : context.m_43725_().m_7702_(context.m_8083_()).getClass().getSimpleName()
            }
         );
      if (result.m_19077_() && !context.m_43725_().f_46443_ && context.m_43725_().m_7702_(context.m_8083_()) instanceof ChestBlockEntity chest) {
         AdvancedLootChestBlock.configureLoot(chest, DEFAULT_LOOT, 1, context.m_43725_().f_46441_.m_188505_());
      }

      return result;
   }
}
