package net.shadowmage.ancientwarfare.core.common.npc;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;

public final class CombatProfessionResolver {
   private static final String SLASH_BLADE_MOD_ID = "slashblade";
   private static final String SLASH_BLADE_ITEM_CLASS = "mods.flammpfeil.slashblade.item.ItemSlashBlade";

   private CombatProfessionResolver() {
   }

   public static String subtypeFor(ItemStack mainHand, ItemStack offHand) {
      return professionFor(mainHand, offHand).subtype();
   }

   public static CombatProfessionResolver.Profession professionFor(ItemStack mainHand, ItemStack offHand) {
      if (isSlashBlade(mainHand)) {
         return CombatProfessionResolver.Profession.SWORDSMAN;
      } else if (isRangedWeapon(mainHand)) {
         return CombatProfessionResolver.Profession.ARCHER;
      } else if (isCommandBaton(mainHand)) {
         return CombatProfessionResolver.Profession.COMMANDER;
      } else if (isMedicTool(mainHand)) {
         return CombatProfessionResolver.Profession.MEDIC;
      } else {
         return isEngineerTool(mainHand) ? CombatProfessionResolver.Profession.ENGINEER : CombatProfessionResolver.Profession.SOLDIER;
      }
   }

   private static boolean isRangedWeapon(ItemStack stack) {
      return NpcEquipmentResolver.isRangedWeapon(stack);
   }

   private static boolean isSlashBlade(ItemStack stack) {
      return stack != null
         && !stack.m_41619_()
         && AWCorePlatform.isModLoaded("slashblade")
         && CombatProfessionResolver.SlashBladeItemClassHolder.TYPE != null
         && CombatProfessionResolver.SlashBladeItemClassHolder.TYPE.isInstance(stack.m_41720_());
   }

   private static boolean isCommandBaton(ItemStack stack) {
      return hasToolRole(stack, NpcProfessionTool.ToolRole.COMMANDER);
   }

   private static boolean isMedicTool(ItemStack stack) {
      return stack != null && stack.m_150930_(Items.f_42410_);
   }

   private static boolean isEngineerTool(ItemStack stack) {
      return hasToolRole(stack, NpcProfessionTool.ToolRole.CRAFTSMAN);
   }

   private static boolean hasToolRole(ItemStack stack, NpcProfessionTool.ToolRole role) {
      return stack != null && stack.m_41720_() instanceof NpcProfessionTool tool && tool.npcToolRole() == role;
   }

   public static enum Profession {
      SOLDIER("soldier"),
      SWORDSMAN("swordsman"),
      ARCHER("archer"),
      COMMANDER("commander"),
      MEDIC("medic"),
      ENGINEER("engineer");

      private final String subtype;

      private Profession(String subtype) {
         this.subtype = subtype;
      }

      public String subtype() {
         return this.subtype;
      }
   }

   private static final class SlashBladeItemClassHolder {
      private static final Class<?> TYPE = load();

      private static Class<?> load() {
         try {
            return Class.forName("mods.flammpfeil.slashblade.item.ItemSlashBlade");
         } catch (LinkageError | ClassNotFoundException var1) {
            return null;
         }
      }
   }
}
