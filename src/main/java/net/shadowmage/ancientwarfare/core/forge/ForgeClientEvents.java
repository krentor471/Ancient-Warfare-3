package net.shadowmage.ancientwarfare.core.forge;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.shadowmage.ancientwarfare.core.block.CoinStackBlock;
import net.shadowmage.ancientwarfare.core.client.AWCoreClientEntityRenderers;
import net.shadowmage.ancientwarfare.core.client.AWCoreClientScreens;
import net.shadowmage.ancientwarfare.core.client.AdvancedSpawnerRenderer;
import net.shadowmage.ancientwarfare.core.client.BardMusicLibrary;
import net.shadowmage.ancientwarfare.core.client.DraftingBuildingPreview;
import net.shadowmage.ancientwarfare.core.client.DraftingStationScreen;
import net.shadowmage.ancientwarfare.core.client.FlywheelControllerGeoRenderer;
import net.shadowmage.ancientwarfare.core.client.FlywheelStorageRenderer;
import net.shadowmage.ancientwarfare.core.client.HandCrankedGeneratorGeoRenderer;
import net.shadowmage.ancientwarfare.core.client.LegacyCoffinModel;
import net.shadowmage.ancientwarfare.core.client.LegacyCoffinRenderer;
import net.shadowmage.ancientwarfare.core.client.ProtectionFlagRenderer;
import net.shadowmage.ancientwarfare.core.client.StructureScannerScreen;
import net.shadowmage.ancientwarfare.core.client.TorqueGeneratorGeoRenderer;
import net.shadowmage.ancientwarfare.core.client.TorqueTransportMixedRenderer;
import net.shadowmage.ancientwarfare.core.client.WindmillBladeGeoRenderer;
import net.shadowmage.ancientwarfare.core.common.npc.BardInstrumentType;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcDefaultsRegistry;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.entity.NpcBaseEntity;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreBlockEntities;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreBlocks;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreItems;
import net.shadowmage.ancientwarfare.core.forge.init.AWCoreMenuTypes;
import net.shadowmage.ancientwarfare.core.item.BardInstrumentItem;
import net.shadowmage.ancientwarfare.core.item.CoinItem;
import net.shadowmage.ancientwarfare.core.item.NpcSpawnerItem;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public final class ForgeClientEvents {
   private ForgeClientEvents() {
   }

   @SubscribeEvent
   public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
      event.registerReloadListener((ResourceManagerReloadListener)manager -> DraftingBuildingPreview.invalidateResources());
   }

   @SubscribeEvent
   public static void registerLayerDefinitions(RegisterLayerDefinitions event) {
      event.registerLayerDefinition(LegacyCoffinModel.WOOD_LAYER, LegacyCoffinModel::createWoodLayer);
      event.registerLayerDefinition(LegacyCoffinModel.STONE_LAYER, LegacyCoffinModel::createStoneLayer);
   }

   @SubscribeEvent
   public static void clientSetup(FMLClientSetupEvent event) {
      event.enqueueWork(
         () -> {
            AWCoreClientEntityRenderers.setRendererRegistrar(EntityRenderers::register);
            AWCoreClientScreens.registerScreens();
            MenuScreens.register((MenuType)AWCoreMenuTypes.STRUCTURE_SCANNER.get(), StructureScannerScreen::new);
            MenuScreens.register((MenuType)AWCoreMenuTypes.DRAFTING_STATION.get(), DraftingStationScreen::new);
            AWCoreClientEntityRenderers.registerRenderers();
            BlockEntityRenderers.register((BlockEntityType)AWCoreBlockEntities.ADVANCED_SPAWNER.get(), AdvancedSpawnerRenderer::new);
            BlockEntityRenderers.register((BlockEntityType)AWCoreBlockEntities.TORQUE_TRANSPORT.get(), TorqueTransportMixedRenderer::new);
            BlockEntityRenderers.register((BlockEntityType)AWCoreBlockEntities.FLYWHEEL_STORAGE.get(), FlywheelStorageRenderer::new);
            BlockEntityRenderers.register((BlockEntityType)AWCoreBlockEntities.FLYWHEEL_CONTROLLER.get(), context -> new FlywheelControllerGeoRenderer());
            BlockEntityRenderers.register((BlockEntityType)AWCoreBlockEntities.TORQUE_GENERATOR.get(), context -> new TorqueGeneratorGeoRenderer());
            BlockEntityRenderers.register((BlockEntityType)AWCoreBlockEntities.HAND_CRANKED_GENERATOR.get(), context -> new HandCrankedGeneratorGeoRenderer());
            BlockEntityRenderers.register((BlockEntityType)AWCoreBlockEntities.WINDMILL_BLADE.get(), context -> new WindmillBladeGeoRenderer());
            BlockEntityRenderers.register((BlockEntityType)AWCoreBlockEntities.PROTECTION_FLAG.get(), ProtectionFlagRenderer::new);
            BlockEntityRenderers.register((BlockEntityType)AWCoreBlockEntities.STRUCTURE_CONTAINER.get(), LegacyCoffinRenderer::new);
            BlockEntityRenderers.register((BlockEntityType)AWCoreBlockEntities.ADVANCED_LOOT_CHEST.get(), ChestRenderer::new);
            BardMusicLibrary.musicFolder();
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.TOWN_HALL.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.IRON_CAGE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.GIBBET.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.FIRE_PIT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.FIRE_PIT_LOG.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.FIRE_PIT_SQUARE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.BRAZIER_FLAME.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.BRAZIER_EMBER.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.STRETCHING_RACK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.ALTAR_SHORT_CLOTH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.ALTAR_LONG_CLOTH.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.ALTAR_CANDLE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.STAKE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.TOTEM_PART_WINGS.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.TOTEM_PART_IRMINSUL_SIDE.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.TOTEM_PART_ELEPHANT_BODY.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.TOTEM_PART_ELEPHANT_LEFT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.TOTEM_PART_ELEPHANT_RIGHT.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer((Block)AWCoreBlocks.ADVANCED_SPAWNER.get(), RenderType.cutout());
            registerShieldProperties();
            registerCoinStackProperties();
            ItemProperties.register((Item)AWCoreItems.BARD_INSTRUMENT.get(), AWCoreIds.id("instrument"), (stack, level, entity, seed) -> {
               BardInstrumentType type = BardInstrumentItem.getType(stack);

               return switch (type) {
                  case LUTE -> 0.0F;
                  case FLUTE -> 0.25F;
                  case HARP -> 0.5F;
                  case DRUM -> 0.75F;
               };
            });
            ItemProperties.register((Item)AWCoreItems.NPC_SPAWNER.get(), AWCoreIds.id("npc_type"), (stack, level, entity, seed) -> {
               String type = NpcSpawnerItem.getNpcType(stack);
               byte var6 = -1;
               switch (type.hashCode()) {
                  case -1091888612:
                     if (type.equals("faction")) {
                        var6 = 0;
                     }
                  default:
                     return switch (var6) {
                        case 0 -> NpcSpawnerItem.getFactionRoleIconValue(stack);
                        default -> NpcSpawnerItem.getIconValue(type);
                     };
               }
            });
            ItemProperties.register(
               (Item)AWCoreItems.NPC_SPAWNER.get(), AWCoreIds.id("npc_weapon"), (stack, level, entity, seed) -> NpcSpawnerItem.getWeaponIconValue(stack)
            );
         }
      );
   }

   private static void registerShieldProperties() {
      ResourceLocation blocking = new ResourceLocation("minecraft", "blocking");
      Item[] shields = new Item[]{
         (Item)AWCoreItems.WOODEN_SHIELD.get(),
         (Item)AWCoreItems.STONE_SHIELD.get(),
         (Item)AWCoreItems.IRON_SHIELD.get(),
         (Item)AWCoreItems.GOLD_SHIELD.get(),
         (Item)AWCoreItems.DIAMOND_SHIELD.get(),
         (Item)AWCoreItems.SHIELD_TRIBAL_1.get(),
         (Item)AWCoreItems.SHIELD_TRIBAL_2.get(),
         (Item)AWCoreItems.SHIELD_ROUND_1.get(),
         (Item)AWCoreItems.SHIELD_ROUND_2.get(),
         (Item)AWCoreItems.SHIELD_ROUND_3.get(),
         (Item)AWCoreItems.SHIELD_ROUND_4.get(),
         (Item)AWCoreItems.SHIELD_ROUND_5.get(),
         (Item)AWCoreItems.SHIELD_ROUND_6.get(),
         (Item)AWCoreItems.SHIELD_WITCHBANE_1.get(),
         (Item)AWCoreItems.SHIELD_WITCHBANE_2.get(),
         (Item)AWCoreItems.SHIELD_BUFFLOKA.get()
      };

      for (Item shield : shields) {
         ItemProperties.register(shield, blocking, (stack, level, entity, seed) -> {
            if (entity == null) {
               return 0.0F;
            } else if (entity.isUsingItem() && entity.getUseItem() == stack) {
               return 1.0F;
            } else {
               if (entity instanceof NpcBaseEntity npc && npc.isShielding() && npc.getOffhandItem() == stack) {
                  return 1.0F;
               }

               return 0.0F;
            }
         });
      }
   }

   private static void registerCoinStackProperties() {
      ResourceLocation sizeProperty = AWCoreIds.id("coin_stack_size");
      Item[] coinStacks = new Item[]{
         ((Block)AWCoreBlocks.COIN_STACK_COPPER.get()).asItem(),
         ((Block)AWCoreBlocks.COIN_STACK_SILVER.get()).asItem(),
         ((Block)AWCoreBlocks.COIN_STACK_GOLD.get()).asItem(),
         ((Block)AWCoreBlocks.COIN_STACK_ANCIENT.get()).asItem()
      };

      for (Item item : coinStacks) {
         ItemProperties.register(item, sizeProperty, (stack, level, entity, seed) -> (float)CoinStackBlock.stackSize(stack) / 64.0F);
      }
   }

   @SubscribeEvent
   public static void registerItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
      event.register((stack, tintIndex) -> tintIndex == 0 ? CoinItem.getMetal(stack).color() : 16777215, new ItemLike[]{(ItemLike)AWCoreItems.COIN.get()});
      event.register((stack, tintIndex) -> {
         if (!NpcSpawnerItem.isFactionSpawner(stack)) {
            return 16777215;
         } else if (tintIndex == 1) {
            return FactionNpcDefaultsRegistry.factionColor(NpcSpawnerItem.getFaction(stack));
         } else {
            return tintIndex == 2 ? 15685463 : 16777215;
         }
      }, new ItemLike[]{(ItemLike)AWCoreItems.NPC_SPAWNER.get()});
   }
}
