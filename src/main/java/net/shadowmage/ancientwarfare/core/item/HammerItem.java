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
      super(new Properties().m_41503_(tier.m_6609_()));
      Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
      builder.put(Attributes.f_22281_, new AttributeModifier(f_41374_, "Tool modifier", 4.0 + (double)tier.m_6631_(), Operation.ADDITION));
      builder.put(Attributes.f_22283_, new AttributeModifier(f_41375_, "Tool modifier", -3.1, Operation.ADDITION));
      this.defaultModifiers = builder.build();
   }

   @Override
   public NpcProfessionTool.ToolRole npcToolRole() {
      return NpcProfessionTool.ToolRole.CRAFTSMAN;
   }

   public Multimap<Attribute, AttributeModifier> m_7167_(EquipmentSlot slot) {
      return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.m_7167_(slot);
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.m_21120_(hand);
      if (player.m_6144_()) {
         if (!level.f_46443_) {
            boolean workMode = HammerModeData.toggleWorkMode(stack);
            level.m_5594_(null, player.m_20183_(), (SoundEvent)SoundEvents.f_12490_.m_203334_(), SoundSource.PLAYERS, 0.3F, 0.6F);
            player.m_5661_(Component.m_237115_(workMode ? "tooltip.ancientwarfare.hammer.work_mode" : "tooltip.ancientwarfare.hammer.rotate_mode"), true);
         }

         return InteractionResultHolder.m_19092_(stack, level.f_46443_);
      } else {
         return InteractionResultHolder.m_19098_(stack);
      }
   }

   public InteractionResult m_6225_(UseOnContext context) {
      Level level = context.m_43725_();
      BlockPos pos = context.m_8083_();
      ItemStack stack = context.m_43722_();
      BlockState state = level.m_8055_(pos);
      if (state.m_60734_() instanceof TorqueTransportBlock torqueTransport
         && torqueTransport.getKind() != TorqueTransportKind.SHAFT
         && context.m_43723_() != null
         && context.m_43723_().m_6144_()) {
         if (!level.f_46443_) {
            BlockState toggled = torqueTransport.toggleConnection(state, context.m_43719_());
            level.m_7731_(pos, toggled, 3);
            level.m_5594_(null, pos, SoundEvents.f_12312_, SoundSource.BLOCKS, 0.2F, 0.6F + level.f_46441_.m_188501_() * 0.15F);
            if (context.m_43723_() != null) {
               stack.m_41622_(1, context.m_43723_(), player -> player.m_21190_(context.m_43724_()));
            }
         }

         return InteractionResult.m_19078_(level.f_46443_);
      }

      if (context.m_43723_() != null && context.m_43723_().m_6144_()) {
         return InteractionResult.PASS;
      } else if (!HammerModeData.isWorkMode(stack)) {
         BlockState rotated = this.rotateState(state);
         if (state.m_60734_() instanceof TorqueTransportBlock torqueTransport && torqueTransport.getKind() != TorqueTransportKind.SHAFT) {
            rotated = torqueTransport.updateConnections(level, pos, rotated);
         }

         if (rotated == state) {
            this.playBlockFeedback(level, pos, state);
            return InteractionResult.m_19078_(level.f_46443_);
         } else {
            if (!level.f_46443_) {
               level.m_7731_(pos, rotated, 3);
               level.m_5594_(null, pos, SoundEvents.f_12312_, SoundSource.BLOCKS, 0.2F, 0.6F + level.f_46441_.m_188501_() * 0.15F);
               if (context.m_43723_() != null) {
                  stack.m_41622_(1, context.m_43723_(), player -> player.m_21190_(context.m_43724_()));
               }
            }

            return InteractionResult.m_19078_(level.f_46443_);
         }
      } else {
         if (!level.f_46443_) {
            level.m_5594_(null, pos, SoundEvents.f_12311_, SoundSource.BLOCKS, 0.2F, 0.6F + level.f_46441_.m_188501_() * 0.15F);
            if (context.m_43723_() != null) {
               if (level.m_7702_(pos) instanceof SurvivalStructureBuilderBlockEntity builder && builder.addPlayerWork()) {
                  stack.m_41622_(1, context.m_43723_(), player -> player.m_21190_(context.m_43724_()));
                  context.m_43723_().m_21011_(context.m_43724_(), true);
                  context.m_43723_().m_5661_(builder.progressMessage(), true);
                  return InteractionResult.m_19078_(level.f_46443_);
               }

               context.m_43723_().m_5661_(Component.m_237115_("tooltip.ancientwarfare.hammer.work_mode_pending"), true);
            }
         }

         return InteractionResult.m_19078_(level.f_46443_);
      }
   }

   private void playBlockFeedback(Level level, BlockPos pos, BlockState state) {
      if (!level.f_46443_) {
         SoundType sound = state.m_60827_();
         level.m_5594_(null, pos, sound.m_56777_(), SoundSource.BLOCKS, sound.m_56773_() * 0.5F, sound.m_56774_() * 0.8F);
      }
   }

   private BlockState rotateState(BlockState state) {
      if (state.m_60734_() instanceof TorqueTransportBlock && state.m_61138_(TorqueTransportBlock.FACING)) {
         return (BlockState)state.m_61124_(
            TorqueTransportBlock.FACING, this.rotateTorqueTransportDirection((Direction)state.m_61143_(TorqueTransportBlock.FACING))
         );
      } else if (state.m_61138_(HorizontalDirectionalBlock.f_54117_)) {
         Direction facing = (Direction)state.m_61143_(HorizontalDirectionalBlock.f_54117_);
         return (BlockState)state.m_61124_(HorizontalDirectionalBlock.f_54117_, facing.m_122427_());
      } else if (state.m_61138_(BlockStateProperties.f_61372_)) {
         Direction facing = (Direction)state.m_61143_(BlockStateProperties.f_61372_);
         return (BlockState)state.m_61124_(BlockStateProperties.f_61372_, this.rotateDirection(facing));
      } else {
         for (Property<?> property : state.m_61147_()) {
            if (property instanceof DirectionProperty directionProperty && state.m_61138_(directionProperty)) {
               Direction facing = (Direction)state.m_61143_(directionProperty);
               return (BlockState)state.m_61124_(directionProperty, this.rotateDirection(facing));
            }
         }

         return state.m_60717_(Rotation.CLOCKWISE_90);
      }
   }

   private Direction rotateDirection(Direction direction) {
      return direction.m_122434_().m_122479_() ? direction.m_122427_() : direction;
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

   public boolean m_7579_(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      stack.m_41622_(1, attacker, entity -> entity.m_21166_(EquipmentSlot.MAINHAND));
      return true;
   }

   public boolean m_6813_(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
      if (!level.f_46443_ && state.m_60800_(level, pos) != 0.0F) {
         stack.m_41622_(2, entity, living -> living.m_21166_(EquipmentSlot.MAINHAND));
      }

      return true;
   }

   public boolean m_8120_(ItemStack stack) {
      return true;
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.m_237115_("tooltip.ancientwarfare.hammer.toggle_mode"));
      tooltip.add(
         Component.m_237115_(HammerModeData.isWorkMode(stack) ? "tooltip.ancientwarfare.hammer.work_mode" : "tooltip.ancientwarfare.hammer.rotate_mode")
      );
   }
}
