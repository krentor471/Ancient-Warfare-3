package net.shadowmage.ancientwarfare.core.common.npc;

public interface NpcProfessionTool {
   NpcProfessionTool.ToolRole npcToolRole();

   public static enum ToolRole {
      RESEARCHER,
      CRAFTSMAN,
      COMMANDER;
   }
}
