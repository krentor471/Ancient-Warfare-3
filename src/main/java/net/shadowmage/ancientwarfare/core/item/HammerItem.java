package net.shadowmage.ancientwarfare.core.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.shadowmage.ancientwarfare.core.block.TorqueTransportBlock;
import net.shadowmage.ancientwarfare.core.common.hammer.HammerModeData;
import net.shadowmage.ancientwarfare.core.common.npc.NpcProfessionTool;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTransportKind;
import net.shadowmage.ancientwarfare.core.tile.SurvivalStructureBuilderBlockEntity;
import org.jetbrains.annotations.Nullable;

public class HammerItem extends Item implements NpcProfessionTool {
   private final Multimap<Attribute, AttributeModifier> defaultModifiers;

   public HammerItem(Tier tier) {
      super(new Properties().durability(tier.getUses()));
      Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
      builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Tool modifier", 4.0 + (double)tier.getAttackDamageBonus(), Operation.ADDITION));
      builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Tool modifier", -3.1, Operation.ADDITION));
      this.defaultModifiers = builder.build();
   }

   @Override
   public NpcProfessionTool.ToolRole npcToolRole() {
      return NpcProfessionTool.ToolRole.CRAFTSMAN;
   }

   public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
      return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (player.isShiftKeyDown()) {
         if (!level.isClientSide) {
            boolean workMode = HammerModeData.toggleWorkMode(stack);
            level.playSound(null, player.blockPosition(), (SoundEvent)SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.3F, 0.6F);
            player.displayClientMessage(Component.translatable(workMode ? "tooltip.ancientwarfare.hammer.work_mode" : "tooltip.ancientwarfare.hammer.rotate_mode"), true);
         }

         return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
      } else {
         return InteractionResultHolder.pass(stack);
      }
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      ItemStack stack = context.getItemInHand();
      BlockState state = level.getBlockState(pos);
      if (state.getBlock() instanceof TorqueTransportBlock torqueTransport
         && torqueTransport.getKind() != TorqueTransportKind.SHAFT
         && context.getPlayer() != null
         && context.getPlayer().isShiftKeyDown()) {
         if (!level.isClientSide) {
            BlockState toggled = torqueTransport.toggleConnection(state, context.getClickedFace());
            level.setBlock(pos, toggled, 3);
            level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.2F, 0.6F + level.random.nextFloat() * 0.15F);
            if (context.getPlayer() != null) {
               stack.hurtAndBreak(1, context.getPlayer(), player -> player.broadcastBreakEvent(context.getHand()));
            }
         }

         return InteractionResult.sidedSuccess(level.isClientSide);
      }

      if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
         return InteractionResult.PASS;
      } else if (!HammerModeData.isWorkMode(stack)) {
         BlockState rotated = this.rotateState(state);
         if (state.getBlock() instanceof TorqueTransportBlock torqueTransport && torqueTransport.getKind() != TorqueTransportKind.SHAFT) {
            rotated = torqueTransport.updateConnections(level, pos, rotated);
         }

         if (rotated == state) {
            this.playBlockFeedback(level, pos, state);
            return InteractionResult.sidedSuccess(level.isClientSide);
         } else {
            if (!level.isClientSide) {
               level.setBlock(pos, rotated, 3);
               level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.2F, 0.6F + level.random.nextFloat() * 0.15F);
               if (context.getPlayer() != null) {
                  stack.hurtAndBreak(1, context.getPlayer(), player -> player.broadcastBreakEvent(context.getHand()));
               }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
         }
      } else {
         if (!level.isClientSide) {
            level.playSound(null, pos, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.2F, 0.6F + level.random.nextFloat() * 0.15F);
            if (context.getPlayer() != null) {
               if (level.getBlockEntity(pos) instanceof SurvivalStructureBuilderBlockEntity builder && builder.addPlayerWork()) {
                  stack.hurtAndBreak(1, context.getPlayer(), player -> player.broadcastBreakEvent(context.getHand()));
                  context.getPlayer().swing(context.getHand(), true);
                  context.getPlayer().displayClientMessage(builder.progressMessage(), true);
                  return InteractionResult.sidedSuccess(level.isClientSide);
               }

               context.getPlayer().displayClientMessage(Component.translatable("tooltip.ancientwarfare.hammer.work_mode_pending"), true);
            }
         }

         return InteractionResult.sidedSuccess(level.isClientSide);
      }
   }

   private void playBlockFeedback(Level level, BlockPos pos, BlockState state) {
      if (!level.isClientSide) {
         SoundType sound = state.getSoundType();
         level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, sound.getVolume() * 0.5F, sound.getPitch() * 0.8F);
      }
   }

   private BlockState rotateState(BlockState state) {
      if (state.getBlock() instanceof TorqueTransportBlock && state.hasProperty(TorqueTransportBlock.FACING)) {
         return (BlockState)state.setValue(
            TorqueTransportBlock.FACING, this.rotateTorqueTransportDirection((Direction)state.getValue(TorqueTransportBlock.FACING))
         );
      } else if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
         Direction facing = (Direction)state.getValue(HorizontalDirectionalBlock.FACING);
         return (BlockState)state.setValue(HorizontalDirectionalBlock.FACING, facing.getClockWise());
      } else if (state.hasProperty(BlockStateProperties.FACING)) {
         Direction facing = (Direction)state.getValue(BlockStateProperties.FACING);
         return (BlockState)state.setValue(BlockStateProperties.FACING, this.rotateDirection(facing));
      } else {
         for (Property<?> property : state.getProperties()) {
            if (property instanceof DirectionProperty directionProperty && state.hasProperty(directionProperty)) {
               Direction facing = (Direction)state.getValue(directionProperty);
               return (BlockState)state.setValue(directionProperty, this.rotateDirection(facing));
            }
         }

         return state.rotate(Rotation.CLOCKWISE_90);
      }
   }

   private Direction rotateDirection(Direction direction) {
      return direction.getAxis().isHorizontal() ? direction.getClockWise() : direction;
   }

   private Direction rotateTorqueTransportDirection(Direction direction) {
      return switch (direction) {
         case NORTH -> Direction.EAST;
         case EAST -> Direction.SOUTH;
         case SOUTH -> Direction.WEST;
         case WEST -> Direction.UP;
         case UP -> Direction.DOWN;
         case DOWN -> Direction.NORTH;
         default -> throw new IncompatibleClassChangeError();
      };
   }

   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      stack.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
      return true;
   }

   public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
      if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F) {
         stack.hurtAndBreak(2, entity, living -> living.broadcastBreakEvent(EquipmentSlot.MAINHAND));
      }

      return true;
   }

   public boolean isEnchantable(ItemStack stack) {
      return true;
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.ancientwarfare.hammer.toggle_mode"));
      tooltip.add(
         Component.translatable(HammerModeData.isWorkMode(stack) ? "tooltip.ancientwarfare.hammer.work_mode" : "tooltip.ancientwarfare.hammer.rotate_mode")
      );
   }
}
