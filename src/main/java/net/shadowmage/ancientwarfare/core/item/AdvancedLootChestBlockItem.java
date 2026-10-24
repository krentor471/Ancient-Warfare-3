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

   protected BlockState getPlacementState(BlockPlaceContext context) {
      return Blocks.CHEST.getStateForPlacement(context);
   }

   public InteractionResult place(BlockPlaceContext context) {
      InteractionResult result = super.place(context);
      AncientWarfareCore.LOG
         .debug(
            "Advanced loot chest item place: pos={}, result={}, placedBlock={}, placedEntity={}",
            new Object[]{
               context.getClickedPos(),
               result,
               context.getLevel().getBlockState(context.getClickedPos()).getBlock(),
               context.getLevel().getBlockEntity(context.getClickedPos()) == null ? "null" : context.getLevel().getBlockEntity(context.getClickedPos()).getClass().getSimpleName()
            }
         );
      if (result.consumesAction() && !context.getLevel().isClientSide && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof ChestBlockEntity chest) {
         AdvancedLootChestBlock.configureLoot(chest, DEFAULT_LOOT, 1, context.getLevel().random.nextLong());
      }

      return result;
   }
}
