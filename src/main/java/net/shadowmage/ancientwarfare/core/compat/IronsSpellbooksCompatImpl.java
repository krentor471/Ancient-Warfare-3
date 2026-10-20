package net.shadowmage.ancientwarfare.core.compat;

import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionSpellcasterNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.IronsCombatNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.IronsFactionSpellcasterNpcEntity;

final class IronsSpellbooksCompatImpl {
   private IronsSpellbooksCompatImpl() {
   }

   static CombatNpcEntity createCombatNpc(EntityType<? extends CombatNpcEntity> entityType, Level level) {
      return new IronsCombatNpcEntity(entityType, level);
   }

   static FactionSpellcasterNpcEntity createFactionSpellcaster(EntityType<? extends FactionSpellcasterNpcEntity> entityType, Level level) {
      return new IronsFactionSpellcasterNpcEntity(entityType, level);
   }

   static boolean isSpellContainer(ItemStack stack) {
      return ISpellContainer.isSpellContainer(stack) && ISpellContainer.get(stack) != null && !ISpellContainer.get(stack).isEmpty();
   }
}
