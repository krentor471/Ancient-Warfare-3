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
               Minecraft.m_91087_().m_167982_(), Minecraft.m_91087_().m_167973_()
            ) {
               public void m_108829_(
                  ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay
               ) {
                  ProtectionFlagBlockEntity flag = new ProtectionFlagBlockEntity(
                     BlockPos.f_121853_, (BlockState)ForgeProtectionFlagBlockItem.this.m_40614_().m_49966_().m_61124_(ProtectionFlagBlock.ROTATION, 0)
                  );
                  flag.loadFromItem(stack);
                  Minecraft.m_91087_().m_167982_().m_112272_(flag, poseStack, buffers, packedLight, packedOverlay);
               }
            };

            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
               return this.renderer;
            }
         }
      );
   }
}
