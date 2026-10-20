package net.shadowmage.ancientwarfare.core.common.npc;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CombatProfessionResolver {
   private CombatProfessionResolver() {
   }

   public static String subtypeFor(ItemStack mainHand, ItemStack offHand) {
      return professionFor(mainHand, offHand).subtype();
   }

   public static CombatProfessionResolver.Profession professionFor(ItemStack mainHand, ItemStack offHand) {
      if (isRangedWeapon(mainHand)) {
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

   private static boolean isCommandBaton(ItemStack stack) {
      return hasToolRole(stack, NpcProfessionTool.ToolRole.COMMANDER);
   }

   private static boolean isMedicTool(ItemStack stack) {
      return stack != null && stack.is(Items.APPLE);
   }

   private static boolean isEngineerTool(ItemStack stack) {
      return hasToolRole(stack, NpcProfessionTool.ToolRole.CRAFTSMAN);
   }

   private static boolean hasToolRole(ItemStack stack, NpcProfessionTool.ToolRole role) {
      return stack != null && stack.getItem() instanceof NpcProfessionTool tool && tool.npcToolRole() == role;
   }

   public static enum Profession {
      SOLDIER("soldier"),
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
}
