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
      super(new Properties().stacksTo(1));
   }

   public static ItemStack create(BardInstrumentType type) {
      ItemStack stack = new ItemStack(AWCoreRegistryAccess.item(AWCoreIds.BARD_INSTRUMENT));
      setType(stack, type);
      return stack;
   }

   public static BardInstrumentType getType(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag == null ? BardInstrumentType.LUTE : BardInstrumentType.byId(tag.getString("instrument"));
   }

   public static void setType(ItemStack stack, BardInstrumentType type) {
      stack.getOrCreateTag().putString("instrument", type.id());
   }

   public Component getName(ItemStack stack) {
      return Component.translatable("item.ancientwarfare.bard_instrument." + getType(stack).id());
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide) {
         level.playSound(null, player.blockPosition(), this.soundFor(getType(stack)), SoundSource.PLAYERS, 2.0F, 1.0F);
      }

      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
   }

   public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
      return true;
   }

   private SoundEvent soundFor(BardInstrumentType type) {
      return switch (type) {
         case LUTE -> (SoundEvent)SoundEvents.NOTE_BLOCK_BASS.value();
         case FLUTE -> (SoundEvent)SoundEvents.NOTE_BLOCK_FLUTE.value();
         case HARP -> (SoundEvent)SoundEvents.NOTE_BLOCK_HARP.value();
         case DRUM -> (SoundEvent)SoundEvents.NOTE_BLOCK_BASEDRUM.value();
      };
   }
}
