package net.shadowmage.ancientwarfare.core.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.common.npc.NpcProfessionTool;
import org.jetbrains.annotations.Nullable;

public class QuillItem extends Item implements NpcProfessionTool {
   private final Multimap<Attribute, AttributeModifier> defaultModifiers;

   public QuillItem(Tier tier) {
      super(new Properties().m_41503_(tier.m_6609_()));
      Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
      builder.put(Attributes.f_22281_, new AttributeModifier(f_41374_, "Tool modifier", 1.0 + (double)tier.m_6631_(), Operation.ADDITION));
      this.defaultModifiers = builder.build();
   }

   @Override
   public NpcProfessionTool.ToolRole npcToolRole() {
      return NpcProfessionTool.ToolRole.RESEARCHER;
   }

   public Multimap<Attribute, AttributeModifier> m_7167_(EquipmentSlot slot) {
      return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.m_7167_(slot);
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
      tooltip.add(Component.m_237115_("tooltip.ancientwarfare.quill.work_mode"));
      tooltip.add(Component.m_237115_("tooltip.ancientwarfare.quill.porting_note"));
   }
}
