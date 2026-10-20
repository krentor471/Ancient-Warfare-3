package net.shadowmage.ancientwarfare.core.common.npc;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;

public final class WorkerProfessionResolver {
   private WorkerProfessionResolver() {
   }

   public static NpcWorkType workTypeFor(ItemStack stack) {
      return professionFor(stack).workType();
   }

   public static String subtypeFor(ItemStack stack) {
      return professionFor(stack).subtype();
   }

   public static WorkerProfessionResolver.Profession professionFor(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return WorkerProfessionResolver.Profession.NONE;
      } else if (stack.getItem() instanceof PickaxeItem) {
         return WorkerProfessionResolver.Profession.MINER;
      } else if (stack.getItem() instanceof HoeItem) {
         return WorkerProfessionResolver.Profession.FARMER;
      } else if (stack.getItem() instanceof AxeItem) {
         return WorkerProfessionResolver.Profession.LUMBERJACK;
      } else if (hasToolRole(stack, NpcProfessionTool.ToolRole.RESEARCHER)) {
         return WorkerProfessionResolver.Profession.RESEARCHER;
      } else {
         return hasToolRole(stack, NpcProfessionTool.ToolRole.CRAFTSMAN)
            ? WorkerProfessionResolver.Profession.CRAFTSMAN
            : WorkerProfessionResolver.Profession.NONE;
      }
   }

   private static boolean hasToolRole(ItemStack stack, NpcProfessionTool.ToolRole role) {
      if (stack.getItem() instanceof NpcProfessionTool tool && tool.npcToolRole() == role) {
         return true;
      }

      return false;
   }

   public static enum Profession {
      NONE("worker", NpcWorkType.NONE),
      MINER("miner", NpcWorkType.MINING),
      FARMER("farmer", NpcWorkType.FARMING),
      LUMBERJACK("lumberjack", NpcWorkType.FORESTRY),
      RESEARCHER("researcher", NpcWorkType.RESEARCH),
      CRAFTSMAN("craftsman", NpcWorkType.CRAFTING);

      private final String subtype;
      private final NpcWorkType workType;

      private Profession(String subtype, NpcWorkType workType) {
         this.subtype = subtype;
         this.workType = workType;
      }

      public String subtype() {
         return this.subtype;
      }

      public NpcWorkType workType() {
         return this.workType;
      }
   }
}
