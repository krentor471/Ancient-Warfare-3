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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.shadowmage.ancientwarfare.core.block.ProtectionFlagBlock;
import net.shadowmage.ancientwarfare.core.item.ProtectionFlagBlockItem;
import net.shadowmage.ancientwarfare.core.tile.ProtectionFlagBlockEntity;

public final class ForgeProtectionFlagBlockItem extends ProtectionFlagBlockItem {
   public ForgeProtectionFlagBlockItem(Block block, Properties properties) {
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
                  ProtectionFlagBlockEntity flag = new ProtectionFlagBlockEntity(
                     BlockPos.ZERO, (BlockState)ForgeProtectionFlagBlockItem.this.getBlock().defaultBlockState().setValue(ProtectionFlagBlock.ROTATION, 0)
                  );
                  flag.loadFromItem(stack);
                  Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(flag, poseStack, buffers, packedLight, packedOverlay);
               }
            };

            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
               return this.renderer;
            }
         }
      );
   }
}
