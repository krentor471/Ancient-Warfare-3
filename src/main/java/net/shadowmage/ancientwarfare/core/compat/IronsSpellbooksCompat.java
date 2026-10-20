package net.shadowmage.ancientwarfare.core.compat;

import java.lang.reflect.Method;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionSpellcasterNpcEntity;

public final class IronsSpellbooksCompat {
   private static final String MOD_ID = "irons_spellbooks";

   private IronsSpellbooksCompat() {
   }

   public static boolean isLoaded() {
      return AWCorePlatform.isModLoaded("irons_spellbooks");
   }

   public static CombatNpcEntity createCombatNpc(EntityType<? extends CombatNpcEntity> entityType, Level level) {
      if (isLoaded()) {
         try {
            return (CombatNpcEntity)invoke("createCombatNpc", new Class[]{EntityType.class, Level.class}, entityType, level);
         } catch (LinkageError | ReflectiveOperationException var3) {
            AncientWarfareCore.LOG.error("Iron's Spells is present but its NPC compatibility API could not be linked", var3);
         }
      }

      return new CombatNpcEntity(entityType, level);
   }

   public static FactionSpellcasterNpcEntity createFactionSpellcaster(EntityType<? extends FactionSpellcasterNpcEntity> entityType, Level level) {
      if (isLoaded()) {
         try {
            return (FactionSpellcasterNpcEntity)invoke("createFactionSpellcaster", new Class[]{EntityType.class, Level.class}, entityType, level);
         } catch (LinkageError | ReflectiveOperationException var3) {
            AncientWarfareCore.LOG.error("Iron's Spells is present but its faction spellcaster compatibility API could not be linked", var3);
         }
      }

      return new FactionSpellcasterNpcEntity(entityType, level);
   }

   public static boolean isSpellContainer(ItemStack stack) {
      if (isLoaded() && stack != null && !stack.m_41619_()) {
         try {
            return Boolean.TRUE.equals(invoke("isSpellContainer", new Class[]{ItemStack.class}, stack));
         } catch (LinkageError | ReflectiveOperationException var2) {
            AncientWarfareCore.LOG.error("Failed to inspect an Iron's Spells item", var2);
            return false;
         }
      } else {
         return false;
      }
   }

   private static Object invoke(String name, Class<?>[] parameterTypes, Object... arguments) throws ReflectiveOperationException {
      Class<?> implementation = Class.forName("net.shadowmage.ancientwarfare.core.compat.IronsSpellbooksCompatImpl");
      Method method = implementation.getDeclaredMethod(name, parameterTypes);
      method.setAccessible(true);
      return method.invoke(null, arguments);
   }
}
