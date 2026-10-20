package net.shadowmage.ancientwarfare.core.forge;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.InputEvent.Key;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;

@EventBusSubscriber(
   modid = "ancientwarfare",
   value = {Dist.CLIENT}
)
public final class ForgeClientInputEvents {
   public static final KeyMapping BALLISTA_LEFT = new KeyMapping("key.ancientwarfare.ballista_left", Type.KEYSYM, 90, "key.categories.ancientwarfare");
   public static final KeyMapping BALLISTA_RIGHT = new KeyMapping("key.ancientwarfare.ballista_right", Type.KEYSYM, 67, "key.categories.ancientwarfare");
   public static final KeyMapping BALLISTA_UP = new KeyMapping("key.ancientwarfare.ballista_up", Type.KEYSYM, 82, "key.categories.ancientwarfare");
   public static final KeyMapping BALLISTA_DOWN = new KeyMapping("key.ancientwarfare.ballista_down", Type.KEYSYM, 70, "key.categories.ancientwarfare");
   private static final KeyMapping ADD_WORK_ORDER_POINT = new KeyMapping(
      "key.ancientwarfare.add_work_order_point", Type.KEYSYM, 90, "key.categories.ancientwarfare"
   );
   private static final KeyMapping BATON_CLEAR_SELECTIONS = batonKey("key.ancientwarfare.baton_clear_selections", 88);

   private ForgeClientInputEvents() {
   }

   /**
    * Ctrl+1..9 saves the baton selection as a squad, Alt+1..9 selects that squad. Runs before vanilla handles the
    * hotbar keys, so the held baton stays in hand.
    */
   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      Minecraft minecraft = Minecraft.getInstance();
      if (event.phase == Phase.START && minecraft.player != null && minecraft.screen == null) {
         boolean holdingBaton = isHoldingBaton(minecraft.player.getMainHandItem()) || isHoldingBaton(minecraft.player.getOffhandItem());
         boolean assign = Screen.hasControlDown();
         if (holdingBaton && (assign || Screen.hasAltDown())) {
            KeyMapping[] hotbar = minecraft.options.keyHotbarSlots;

            for (int i = 0; i < hotbar.length; i++) {
               while (hotbar[i].consumeClick()) {
                  AWCoreNetwork.sendBatonSquad(i + 1, assign);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onKeyInput(Key event) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null && minecraft.screen == null) {
         while (ADD_WORK_ORDER_POINT.consumeClick()) {
            if (isHoldingOrder(minecraft.player.getMainHandItem()) || isHoldingOrder(minecraft.player.getOffhandItem())) {
               HitResult var3 = minecraft.hitResult;
               if (var3 instanceof BlockHitResult) {
                  BlockHitResult blockHit = (BlockHitResult)var3;
                  if (blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                     AWCorePlatform.addWorkOrderPoint(blockHit.getBlockPos(), blockHit.getDirection());
                  }
               }
            }
         }

         consumeBatonKey(minecraft, BATON_CLEAR_SELECTIONS);
      }
   }

   private static boolean isHoldingOrder(ItemStack stack) {
      return AWCoreRegistryAccess.is(stack, AWCoreIds.WORK_ORDER)
         || AWCoreRegistryAccess.is(stack, AWCoreIds.ROUTING_ORDER)
         || AWCoreRegistryAccess.is(stack, AWCoreIds.UPKEEP_ORDER)
         || AWCoreRegistryAccess.is(stack, AWCoreIds.COMBAT_ORDER);
   }

   private static KeyMapping batonKey(String translationKey, int key) {
      return new KeyMapping(translationKey, Type.KEYSYM, key, "key.categories.ancientwarfare");
   }

   private static void consumeBatonKey(Minecraft minecraft, KeyMapping mapping) {
      while (mapping.consumeClick()) {
         if (isHoldingBaton(minecraft.player.getMainHandItem()) || isHoldingBaton(minecraft.player.getOffhandItem())) {
            AWCorePlatform.clearCommandBatonSelections();
         }
      }
   }

   private static boolean isHoldingBaton(ItemStack stack) {
      return stack != null && stack.getItem() instanceof CommandBatonItem;
   }

   @EventBusSubscriber(
      modid = "ancientwarfare",
      bus = Bus.MOD,
      value = {Dist.CLIENT}
   )
   public static final class ModBus {
      private ModBus() {
      }

      @SubscribeEvent
      public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
         event.register(ForgeClientInputEvents.ADD_WORK_ORDER_POINT);
         event.register(ForgeClientInputEvents.BALLISTA_LEFT);
         event.register(ForgeClientInputEvents.BALLISTA_RIGHT);
         event.register(ForgeClientInputEvents.BALLISTA_UP);
         event.register(ForgeClientInputEvents.BALLISTA_DOWN);
         event.register(ForgeClientInputEvents.BATON_CLEAR_SELECTIONS);
      }
   }
}
