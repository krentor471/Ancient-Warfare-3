package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The builder's wand marks out building work for the player's workers: click one corner, then the
 * other, and choose what to do with the box. Clicking the air opens the list of jobs under way.
 * The marking lives on the client; only the finished job goes to the server.
 */
public class ConstructionWandItem extends Item {
   public ConstructionWandItem() {
      super(new Item.Properties().stacksTo(1));
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      if (context.getLevel().isClientSide && context.getPlayer() != null) {
         net.shadowmage.ancientwarfare.core.client.ConstructionClient.click(context.getClickedPos(), context.getPlayer().isShiftKeyDown());
      }

      return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      if (level.isClientSide) {
         net.shadowmage.ancientwarfare.core.client.ConstructionClient.clickAir(player.isShiftKeyDown());
      }

      return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
   }
}
