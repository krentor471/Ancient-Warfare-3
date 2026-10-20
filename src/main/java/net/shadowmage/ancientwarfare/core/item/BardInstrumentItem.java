package net.shadowmage.ancientwarfare.core.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.BardInstrumentType;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;

public class BardInstrumentItem extends Item {
   private static final String INSTRUMENT_TAG = "instrument";

   public BardInstrumentItem() {
      super(new Properties().m_41487_(1));
   }

   public static ItemStack create(BardInstrumentType type) {
      ItemStack stack = new ItemStack(AWCoreRegistryAccess.item(AWCoreIds.BARD_INSTRUMENT));
      setType(stack, type);
      return stack;
   }

   public static BardInstrumentType getType(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      return tag == null ? BardInstrumentType.LUTE : BardInstrumentType.byId(tag.m_128461_("instrument"));
   }

   public static void setType(ItemStack stack, BardInstrumentType type) {
      stack.m_41784_().m_128359_("instrument", type.id());
   }

   public Component m_7626_(ItemStack stack) {
      return Component.m_237115_("item.ancientwarfare.bard_instrument." + getType(stack).id());
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.m_21120_(hand);
      if (!level.f_46443_) {
         level.m_5594_(null, player.m_20183_(), this.soundFor(getType(stack)), SoundSource.PLAYERS, 2.0F, 1.0F);
      }

      return InteractionResultHolder.m_19092_(stack, level.f_46443_);
   }

   public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
      return true;
   }

   private SoundEvent soundFor(BardInstrumentType type) {
      return switch (type) {
         case LUTE -> (SoundEvent)SoundEvents.f_12209_.m_203334_();
         case FLUTE -> (SoundEvent)SoundEvents.f_12212_.m_203334_();
         case HARP -> (SoundEvent)SoundEvents.f_12214_.m_203334_();
         case DRUM -> (SoundEvent)SoundEvents.f_12208_.m_203334_();
      };
   }
}
