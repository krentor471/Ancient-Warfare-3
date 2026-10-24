package net.shadowmage.ancientwarfare.core.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.entity.GateEntity;

public class GateSpawnerItem extends Item {
   private static final String FIRST_POS = "FirstPos";
   private static final int MAX_WIDTH = 12;
   private static final int MAX_HEIGHT = 12;
   private final GateEntity.Material material;
   private final GateEntity.OpeningStyle openingStyle;

   public GateSpawnerItem() {
      this(GateEntity.Material.WOOD, GateEntity.OpeningStyle.VERTICAL);
   }

   public GateSpawnerItem(GateEntity.Material material) {
      this(material, GateEntity.OpeningStyle.VERTICAL);
   }

   public GateSpawnerItem(GateEntity.Material material, GateEntity.OpeningStyle openingStyle) {
      super(new Properties().stacksTo(1));
      this.material = material;
      this.openingStyle = openingStyle;
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      ItemStack stack = context.getItemInHand();
      Player player = context.getPlayer();
      if (player != null && !player.isSpectator() && !stack.isEmpty()) {
         BlockPos clicked = context.getClickedPos().relative(context.getClickedFace());
         CompoundTag tag = stack.getOrCreateTag();
         if (!tag.contains("FirstPos")) {
            if (!level.isClientSide && player != null) {
               tag.put("FirstPos", NbtUtils.writeBlockPos(clicked));
               player.displayClientMessage(
                  Component.translatable("message.ancientwarfare.gate.first_pos", new Object[]{clicked.getX(), clicked.getY(), clicked.getZ()}),
                  true
               );
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
         } else {
            BlockPos first = NbtUtils.readBlockPos(tag.getCompound("FirstPos"));
            tag.remove("FirstPos");
            if (!level.isClientSide && player != null) {
               if (!isValidPlane(first, clicked)) {
                  player.displayClientMessage(Component.translatable("message.ancientwarfare.gate.invalid_plane"), true);
                  return InteractionResult.FAIL;
               }

               BlockPos min = new BlockPos(
                  Math.min(first.getX(), clicked.getX()),
                  Math.min(first.getY(), clicked.getY()),
                  Math.min(first.getZ(), clicked.getZ())
               );
               BlockPos max = new BlockPos(
                  Math.max(first.getX(), clicked.getX()),
                  Math.max(first.getY(), clicked.getY()),
                  Math.max(first.getZ(), clicked.getZ())
               );
               if (!isValidSize(min, max)) {
                  player.displayClientMessage(Component.translatable("message.ancientwarfare.gate.invalid_size", new Object[]{12, 12}), true);
                  return InteractionResult.FAIL;
               }

               if (!canPlace(level, player, stack, min, max)) {
                  player.displayClientMessage(Component.translatable("message.ancientwarfare.gate.blocked"), true);
                  return InteractionResult.FAIL;
               }

               GateEntity gate = (GateEntity)AWCoreRegistryAccess.entityType(AWCoreIds.DYNAMIC_GATE).create(level);
               if (gate != null) {
                  gate.configure(first, clicked, context.getHorizontalDirection());
                  gate.setMaterial(this.material);
                  gate.setOpeningStyle(this.openingStyle);
                  gate.setOwner(player.getUUID());
                  if (tag.contains("GateHealth")) {
                     gate.restorePackedHealth(tag.getFloat("GateHealth"));
                  }

                  if (!level.addFreshEntity(gate)) {
                     return InteractionResult.FAIL;
                  }

                  gate.ensureProxyBlocks();
                  if (!player.getAbilities().instabuild) {
                     stack.shrink(1);
                  }

                  player.displayClientMessage(Component.translatable("message.ancientwarfare.gate.created"), true);
               }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
         }
      } else {
         return InteractionResult.FAIL;
      }
   }

   private static boolean isValidPlane(BlockPos first, BlockPos second) {
      return first.getX() == second.getX() ^ first.getZ() == second.getZ();
   }

   private static boolean isValidSize(BlockPos min, BlockPos max) {
      int width = Math.max(max.getX() - min.getX() + 1, max.getZ() - min.getZ() + 1);
      int height = max.getY() - min.getY() + 1;
      return width >= 1 && height >= 1 && width <= 12 && height <= 12;
   }

   private static boolean canPlace(Level level, Player player, ItemStack stack, BlockPos min, BlockPos max) {
      for (int x = min.getX(); x <= max.getX(); x++) {
         for (int y = min.getY(); y <= max.getY(); y++) {
            int z = min.getZ();

            while (z <= max.getZ()) {
               BlockPos pos = new BlockPos(x, y, z);
               if (level.hasChunkAt(pos) && level.mayInteract(player, pos) && player.mayUseItemAt(pos, Direction.UP, stack)) {
                  BlockState state = level.getBlockState(pos);
                  if (!state.is(AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY)) && (state.isAir() || state.canBeReplaced())) {
                     z++;
                     continue;
                  }

                  return false;
               }

               return false;
            }
         }
      }

      return true;
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable(this.tooltipKey()).withStyle(ChatFormatting.GRAY));
      tooltip.add(Component.translatable("tooltip.ancientwarfare.gate_spawner.ownership").withStyle(ChatFormatting.GRAY));
      if (stack.hasTag() && stack.getTag().contains("FirstPos")) {
         BlockPos first = NbtUtils.readBlockPos(stack.getTag().getCompound("FirstPos"));
         tooltip.add(
            Component.translatable("tooltip.ancientwarfare.gate_spawner.selected", new Object[]{first.getX(), first.getY(), first.getZ()})
               .withStyle(ChatFormatting.YELLOW)
         );
      }
   }

   private String tooltipKey() {
      String materialName = this.material == GateEntity.Material.IRON ? "iron" : "wood";

      return switch (this.openingStyle) {
         case SINGLE_SLIDING -> "tooltip.ancientwarfare.gate_spawner_single_" + materialName;
         case DOUBLE_SLIDING -> "tooltip.ancientwarfare.gate_spawner_double_" + materialName;
         case DRAWBRIDGE -> "tooltip.ancientwarfare.gate_spawner_drawbridge";
         default -> this.material == GateEntity.Material.IRON ? "tooltip.ancientwarfare.gate_spawner_iron" : "tooltip.ancientwarfare.gate_spawner";
      };
   }
}
