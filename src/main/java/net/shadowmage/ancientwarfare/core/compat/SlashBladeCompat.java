package net.shadowmage.ancientwarfare.core.compat;

import java.lang.reflect.Method;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;

public final class SlashBladeCompat {
   private static final String MOD_ID = "slashblade";
   public static final int MOVE_A1 = 1;
   public static final int MOVE_A2 = 2;
   public static final int MOVE_A3 = 3;
   public static final int MOVE_A4 = 4;
   public static final int MOVE_A4_EX = 5;
   public static final int MOVE_A5_EX = 6;
   public static final int MOVE_COMBO_B = 7;
   public static final int MOVE_COMBO_C = 8;
   public static final int MOVE_RAPID_SLASH = 9;
   public static final int MOVE_UPPERSLASH = 10;
   public static final int MOVE_UPPERSLASH_JUMP = 11;
   public static final int MOVE_RISING_STAR = 12;
   public static final int MOVE_AERIAL_A1 = 13;
   public static final int MOVE_AERIAL_A2 = 14;
   public static final int MOVE_AERIAL_A3 = 15;
   public static final int MOVE_AERIAL_B3 = 16;
   public static final int MOVE_AERIAL_B4 = 17;
   public static final int MOVE_AERIAL_CLEAVE = 18;
   public static final int MOVE_CHARGE = 19;
   public static final int MOVE_SLASH_ART = 20;
   public static final int MOVE_SLASH_ART_THRUST = 21;
   public static final int MOVE_SLASH_ART_RANGED = 22;
   public static final int KNOCKBACK_NONE = 0;
   public static final int KNOCKBACK_TOSS = 1;
   public static final int KNOCKBACK_SMASH = 2;
   public static final int KNOCKBACK_METEOR = 3;
   public static final int SUMMONED_SWORD_BASIC = 1;
   public static final int SUMMONED_SWORD_SPIRAL = 2;
   public static final int SUMMONED_SWORD_STORM = 3;
   public static final int SUMMONED_SWORD_BLISTERING = 4;
   public static final int SUMMONED_SWORD_HEAVY_RAIN = 5;
   public static final int CHARGE_NORMAL = 1;
   public static final int CHARGE_JUST = 2;
   public static final int CHARGE_SUPER = 3;
   public static final int SLASH_ART_NONE = 0;
   public static final int SLASH_ART_JUDGEMENT_CUT = 1;
   public static final int SLASH_ART_SAKURA_END = 2;
   public static final int SLASH_ART_VOID_SLASH = 3;
   public static final int SLASH_ART_CIRCLE_SLASH = 4;
   public static final int SLASH_ART_DRIVE_VERTICAL = 5;
   public static final int SLASH_ART_DRIVE_HORIZONTAL = 6;
   public static final int SLASH_ART_WAVE_EDGE = 7;
   public static final int SLASH_ART_PIERCING = 8;
   public static final String NPC_SUMMONED_SWORD_TAG = "ancientwarfareNpcSummonedSword";
   public static final String NPC_SUMMONED_SWORD_TARGET_TAG = "ancientwarfareNpcSummonedSwordTarget";

   private SlashBladeCompat() {
   }

   public static boolean isLoaded() {
      return AWCorePlatform.isModLoaded("slashblade");
   }

   public static boolean isSlashBlade(ItemStack stack) {
      return isLoaded() && callBoolean(() -> invokeBoolean("isSlashBlade", new Class[]{ItemStack.class}, stack));
   }

   public static void preserveNpcBladeDurability(LivingEntity holder) {
      if (isLoaded()) {
         run(() -> invoke("preserveNpcBladeDurability", new Class[]{LivingEntity.class}, holder));
      }
   }

   public static boolean performGroundCombo(LivingEntity attacker, Entity target, int comboStep) {
      return isLoaded()
         && callBoolean(() -> invokeBoolean("performGroundCombo", new Class[]{LivingEntity.class, Entity.class, int.class}, attacker, target, comboStep));
   }

   public static boolean performTechniqueHit(LivingEntity attacker, Entity target, int move, float damageRatio, int knockback) {
      return isLoaded()
         && callBoolean(
            () -> invokeBoolean(
                  "performTechniqueHit",
                  new Class[]{LivingEntity.class, Entity.class, int.class, float.class, int.class},
                  attacker,
                  target,
                  move,
                  damageRatio,
                  knockback
               )
         );
   }

   public static boolean setTechniqueState(LivingEntity attacker, int move) {
      return isLoaded() && callBoolean(() -> invokeBoolean("setTechniqueState", new Class[]{LivingEntity.class, int.class}, attacker, move));
   }

   public static boolean isPowered(LivingEntity attacker) {
      return isLoaded() && callBoolean(() -> invokeBoolean("isPowered", new Class[]{LivingEntity.class}, attacker));
   }

   public static boolean canUseSummonedSwordArts(LivingEntity attacker) {
      return isLoaded() && callBoolean(() -> invokeBoolean("canUseSummonedSwordArts", new Class[]{LivingEntity.class}, attacker));
   }

   public static boolean canUseThornsGuard(LivingEntity attacker) {
      return isLoaded() && callBoolean(() -> invokeBoolean("canUseThornsGuard", new Class[]{LivingEntity.class}, attacker));
   }

   public static void setThornsGuardInput(LivingEntity attacker, boolean guarding) {
      if (isLoaded()) {
         run(() -> invoke("setThornsGuardInput", new Class[]{LivingEntity.class, boolean.class}, attacker, guarding));
      }
   }

   public static int getHeldSlashArt(LivingEntity attacker) {
      return isLoaded() ? callInt(() -> invokeInt("getHeldSlashArt", new Class[]{LivingEntity.class}, attacker), 0) : 0;
   }

   public static boolean canUseChargedSlashArt(LivingEntity attacker, int chargeType) {
      return isLoaded() && callBoolean(() -> invokeBoolean("canUseChargedSlashArt", new Class[]{LivingEntity.class, int.class}, attacker, chargeType));
   }

   public static int beginChargedSlashArt(LivingEntity attacker, LivingEntity target, int chargeType) {
      return isLoaded()
         ? callInt(() -> invokeInt("beginChargedSlashArt", new Class[]{LivingEntity.class, LivingEntity.class, int.class}, attacker, target, chargeType), 0)
         : 0;
   }

   public static boolean tickChargedSlashArt(LivingEntity attacker, LivingEntity target, int art, int chargeType, int executionTick) {
      return isLoaded()
         && callBoolean(
            () -> invokeBoolean(
                  "tickChargedSlashArt",
                  new Class[]{LivingEntity.class, LivingEntity.class, int.class, int.class, int.class},
                  attacker,
                  target,
                  art,
                  chargeType,
                  executionTick
               )
         );
   }

   public static int getChargedSlashArtExecutionTicks(int art, int chargeType) {
      return isLoaded() ? callInt(() -> invokeInt("getChargedSlashArtExecutionTicks", new Class[]{int.class, int.class}, art, chargeType), 0) : 0;
   }

   public static int getSlashArtPose(int art) {
      return isLoaded() ? callInt(() -> invokeInt("getSlashArtPose", new Class[]{int.class}, art), 20) : 20;
   }

   public static boolean isPiercingSlashArt(int art) {
      return isLoaded() && callBoolean(() -> invokeBoolean("isPiercingSlashArt", new Class[]{int.class}, art));
   }

   public static void emitChargedSlashArtParticles(LivingEntity attacker, int chargeType) {
      if (isLoaded()) {
         run(() -> invoke("emitChargedSlashArtParticles", new Class[]{LivingEntity.class, int.class}, attacker, chargeType));
      }
   }

   public static boolean performSummonedSwordArt(LivingEntity attacker, LivingEntity target, int art) {
      return isLoaded()
         && callBoolean(() -> invokeBoolean("performSummonedSwordArt", new Class[]{LivingEntity.class, LivingEntity.class, int.class}, attacker, target, art));
   }

   private static boolean callBoolean(SlashBladeCompat.BooleanCall call) {
      try {
         return call.get();
      } catch (LinkageError | ReflectiveOperationException var2) {
         logLinkageFailure(var2);
         return false;
      }
   }

   private static int callInt(SlashBladeCompat.IntCall call, int fallback) {
      try {
         return call.get();
      } catch (LinkageError | ReflectiveOperationException var3) {
         logLinkageFailure(var3);
         return fallback;
      }
   }

   private static void run(SlashBladeCompat.VoidCall call) {
      try {
         call.run();
      } catch (LinkageError | ReflectiveOperationException var2) {
         logLinkageFailure(var2);
      }
   }

   private static void logLinkageFailure(Throwable error) {
      AncientWarfareCore.LOG.error("SlashBlade is present but its compatibility API could not be linked", error);
   }

   private static Object invoke(String name, Class<?>[] parameterTypes, Object... arguments) throws ReflectiveOperationException {
      Class<?> implementation = Class.forName("net.shadowmage.ancientwarfare.core.compat.SlashBladeCompatImpl");
      Method method = implementation.getDeclaredMethod(name, parameterTypes);
      method.setAccessible(true);
      return method.invoke(null, arguments);
   }

   private static boolean invokeBoolean(String name, Class<?>[] parameterTypes, Object... arguments) throws ReflectiveOperationException {
      return Boolean.TRUE.equals(invoke(name, parameterTypes, arguments));
   }

   private static int invokeInt(String name, Class<?>[] parameterTypes, Object... arguments) throws ReflectiveOperationException {
      return invoke(name, parameterTypes, arguments) instanceof Number number ? number.intValue() : 0;
   }

   @FunctionalInterface
   private interface BooleanCall {
      boolean get() throws ReflectiveOperationException;
   }

   @FunctionalInterface
   private interface IntCall {
      int get() throws ReflectiveOperationException;
   }

   @FunctionalInterface
   private interface VoidCall {
      void run() throws ReflectiveOperationException;
   }
}
