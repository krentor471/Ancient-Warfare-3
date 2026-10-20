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
      super(new Properties().m_41487_(1));
      this.material = material;
      this.openingStyle = openingStyle;
   }

   public InteractionResult m_6225_(UseOnContext context) {
      Level level = context.m_43725_();
      ItemStack stack = context.m_43722_();
      Player player = context.m_43723_();
      if (player != null && !player.m_5833_() && !stack.m_41619_()) {
         BlockPos clicked = context.m_8083_().m_121945_(context.m_43719_());
         CompoundTag tag = stack.m_41784_();
         if (!tag.m_128441_("FirstPos")) {
            if (!level.f_46443_ && player != null) {
               tag.m_128365_("FirstPos", NbtUtils.m_129224_(clicked));
               player.m_5661_(
                  Component.m_237110_("message.ancientwarfare.gate.first_pos", new Object[]{clicked.m_123341_(), clicked.m_123342_(), clicked.m_123343_()}),
                  true
               );
            }

            return InteractionResult.m_19078_(level.f_46443_);
         } else {
            BlockPos first = NbtUtils.m_129239_(tag.m_128469_("FirstPos"));
            tag.m_128473_("FirstPos");
            if (!level.f_46443_ && player != null) {
               if (!isValidPlane(first, clicked)) {
                  player.m_5661_(Component.m_237115_("message.ancientwarfare.gate.invalid_plane"), true);
                  return InteractionResult.FAIL;
               }

               BlockPos min = new BlockPos(
                  Math.min(first.m_123341_(), clicked.m_123341_()),
                  Math.min(first.m_123342_(), clicked.m_123342_()),
                  Math.min(first.m_123343_(), clicked.m_123343_())
               );
               BlockPos max = new BlockPos(
                  Math.max(first.m_123341_(), clicked.m_123341_()),
                  Math.max(first.m_123342_(), clicked.m_123342_()),
                  Math.max(first.m_123343_(), clicked.m_123343_())
               );
               if (!isValidSize(min, max)) {
                  player.m_5661_(Component.m_237110_("message.ancientwarfare.gate.invalid_size", new Object[]{12, 12}), true);
                  return InteractionResult.FAIL;
               }

               if (!canPlace(level, player, stack, min, max)) {
                  player.m_5661_(Component.m_237115_("message.ancientwarfare.gate.blocked"), true);
                  return InteractionResult.FAIL;
               }

               GateEntity gate = (GateEntity)AWCoreRegistryAccess.entityType(AWCoreIds.DYNAMIC_GATE).m_20615_(level);
               if (gate != null) {
                  gate.configure(first, clicked, context.m_8125_());
                  gate.setMaterial(this.material);
                  gate.setOpeningStyle(this.openingStyle);
                  gate.setOwner(player.m_20148_());
                  if (tag.m_128441_("GateHealth")) {
                     gate.restorePackedHealth(tag.m_128457_("GateHealth"));
                  }

                  if (!level.m_7967_(gate)) {
                     return InteractionResult.FAIL;
                  }

                  gate.ensureProxyBlocks();
                  if (!player.m_150110_().f_35937_) {
                     stack.m_41774_(1);
                  }

                  player.m_5661_(Component.m_237115_("message.ancientwarfare.gate.created"), true);
               }
            }

            return InteractionResult.m_19078_(level.f_46443_);
         }
      } else {
         return InteractionResult.FAIL;
      }
   }

   private static boolean isValidPlane(BlockPos first, BlockPos second) {
      return first.m_123341_() == second.m_123341_() ^ first.m_123343_() == second.m_123343_();
   }

   private static boolean isValidSize(BlockPos min, BlockPos max) {
      int width = Math.max(max.m_123341_() - min.m_123341_() + 1, max.m_123343_() - min.m_123343_() + 1);
      int height = max.m_123342_() - min.m_123342_() + 1;
      return width >= 1 && height >= 1 && width <= 12 && height <= 12;
   }

   private static boolean canPlace(Level level, Player player, ItemStack stack, BlockPos min, BlockPos max) {
      for (int x = min.m_123341_(); x <= max.m_123341_(); x++) {
         for (int y = min.m_123342_(); y <= max.m_123342_(); y++) {
            int z = min.m_123343_();

            while (z <= max.m_123343_()) {
               BlockPos pos = new BlockPos(x, y, z);
               if (level.m_46805_(pos) && level.m_7966_(player, pos) && player.m_36204_(pos, Direction.UP, stack)) {
                  BlockState state = level.m_8055_(pos);
                  if (!state.m_60713_(AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY)) && (state.m_60795_() || state.m_247087_())) {
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

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.m_237115_(this.tooltipKey()).m_130940_(ChatFormatting.GRAY));
      tooltip.add(Component.m_237115_("tooltip.ancientwarfare.gate_spawner.ownership").m_130940_(ChatFormatting.GRAY));
      if (stack.m_41782_() && stack.m_41783_().m_128441_("FirstPos")) {
         BlockPos first = NbtUtils.m_129239_(stack.m_41783_().m_128469_("FirstPos"));
         tooltip.add(
            Component.m_237110_("tooltip.ancientwarfare.gate_spawner.selected", new Object[]{first.m_123341_(), first.m_123342_(), first.m_123343_()})
               .m_130940_(ChatFormatting.YELLOW)
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
