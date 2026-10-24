package net.shadowmage.ancientwarfare.core.forge.item;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.shadowmage.ancientwarfare.core.item.RestoredCoffinBlockItem;
import net.shadowmage.ancientwarfare.core.tile.StructureContainerBlockEntity;

public final class ForgeRestoredCoffinBlockItem extends RestoredCoffinBlockItem {
   public ForgeRestoredCoffinBlockItem(Block block, Properties properties) {
      super(block, properties);
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(
         new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new BlockEntityWithoutLevelRenderer(
               Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()
            ) {
               public void renderByItem(
                  ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay
               ) {
                  StructureContainerBlockEntity coffin = new StructureContainerBlockEntity(
                     BlockPos.ZERO, ForgeRestoredCoffinBlockItem.this.getBlock().defaultBlockState()
                  );
                  Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(coffin, poseStack, buffers, packedLight, packedOverlay);
               }
            };

            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
               return this.renderer;
            }
         }
      );
   }
}
