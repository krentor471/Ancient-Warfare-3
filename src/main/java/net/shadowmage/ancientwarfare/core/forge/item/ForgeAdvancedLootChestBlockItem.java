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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.shadowmage.ancientwarfare.core.item.AdvancedLootChestBlockItem;

public final class ForgeAdvancedLootChestBlockItem extends AdvancedLootChestBlockItem {
   public ForgeAdvancedLootChestBlockItem(Block block, Properties properties) {
      super(block, properties);
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(
         new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new BlockEntityWithoutLevelRenderer(
               Minecraft.m_91087_().m_167982_(), Minecraft.m_91087_().m_167973_()
            ) {
               public void m_108829_(
                  ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay
               ) {
                  ChestBlockEntity chest = new ChestBlockEntity(BlockPos.f_121853_, Blocks.f_50087_.m_49966_());
                  Minecraft.m_91087_().m_167982_().m_112272_(chest, poseStack, buffers, packedLight, packedOverlay);
               }
            };

            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
               return this.renderer;
            }
         }
      );
   }
}
