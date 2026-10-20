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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.shadowmage.ancientwarfare.core.block.TorqueGeneratorBlock;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueGeneratorKind;
import net.shadowmage.ancientwarfare.core.item.RestoredAutomationBlockItem;

public final class ForgeRestoredAutomationBlockItem extends RestoredAutomationBlockItem {
   public ForgeRestoredAutomationBlockItem(Block block, Properties properties) {
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
                  BlockState state = ForgeRestoredAutomationBlockItem.this.getBlock().defaultBlockState();
                  if (ForgeRestoredAutomationBlockItem.this.getBlock() instanceof EntityBlock entityBlock) {
                     BlockEntity blockEntity = entityBlock.newBlockEntity(BlockPos.ZERO, state);
                     if (blockEntity != null) {
                        poseStack.pushPose();
                        if (ForgeRestoredAutomationBlockItem.this.getBlock() instanceof TorqueGeneratorBlock generator
                           && generator.getKind() == TorqueGeneratorKind.WATERWHEEL) {
                           poseStack.translate(0.225, 0.205, 0.225);
                           poseStack.scale(0.55F, 0.55F, 0.55F);
                        }

                        Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(blockEntity, poseStack, buffers, packedLight, packedOverlay);
                        poseStack.popPose();
                     }
                  }
               }
            };

            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
               return this.renderer;
            }
         }
      );
   }
}
