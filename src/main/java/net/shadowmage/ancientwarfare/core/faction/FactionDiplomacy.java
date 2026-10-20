package net.shadowmage.ancientwarfare.core.faction;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;

/**
 * Relations between AI factions: an opinion from -100 to 100 and a state (war, neutral, pact,
 * alliance). Every faction starts neutral; opinion starts lower for factions the faction file marks
 * as old enemies. Outlaws are at war with everyone and cannot make peace.
 */
public final class FactionDiplomacy {
   public static final int MIN_OPINION = -100;
   public static final int MAX_OPINION = 100;
   private static final int RIVAL_OPINION = -30;
   private static final int FRIENDLY_OPINION = 20;
   /** Opinion lost when one faction's NPC kills another's. */
   public static final int KILL_OPINION = -1;

   private FactionDiplomacy() {
   }

   /** Whether NPCs of these two factions fight on sight. Falls back to the faction file off the server. */
   public static boolean isHostile(Level level, String faction, String other) {
      if (FactionRegistry.isSameFaction(faction, other)) {
         return false;
      } else if (!(level instanceof ServerLevel serverLevel) || !FactionRegistry.isKnown(faction) || !FactionRegistry.isKnown(other)) {
         return FactionRegistry.isHostile(faction, other);
      } else {
         return relation(serverLevel, faction, other).state() == RelationState.WAR;
      }
   }

   public static Relation relation(ServerLevel level, String faction, String other) {
      Relation stored = FactionWorldData.get(level).relation(faction, other);
      return stored != null ? stored : initialRelation(faction, other);
   }

   public static boolean isOutlaw(String faction) {
      return FactionRegions.OUTLAWS.contains(faction);
   }

   /** Changes the state; returns false when outlaws are involved or nothing changed. */
   public static boolean setState(ServerLevel level, String faction, String other, RelationState state) {
      if (!canNegotiate(faction, other)) {
         return false;
      }

      Relation current = relation(level, faction, other);
      if (current.state() == state) {
         return false;
      }

      FactionWorldData.get(level).setRelation(faction, other, new Relation(current.opinion(), state, level.getGameTime()));
      return true;
   }

   public static Relation adjustOpinion(ServerLevel level, String faction, String other, int delta) {
      Relation current = relation(level, faction, other);
      if (!canNegotiate(faction, other) || delta == 0) {
         return current;
      }

      int opinion = Math.max(MIN_OPINION, Math.min(MAX_OPINION, current.opinion() + delta));
      Relation updated = new Relation(opinion, current.state(), current.since());
      FactionWorldData.get(level).setRelation(faction, other, updated);
      return updated;
   }

   public static boolean canNegotiate(String faction, String other) {
      return FactionRegistry.isKnown(faction)
         && FactionRegistry.isKnown(other)
         && !FactionRegistry.isSameFaction(faction, other)
         && !isOutlaw(faction)
         && !isOutlaw(other);
   }

   private static Relation initialRelation(String faction, String other) {
      if (isOutlaw(faction) || isOutlaw(other)) {
         return new Relation(MIN_OPINION, RelationState.WAR, 0L);
      }

      boolean rivals = FactionRegistry.isHostile(faction, other) || FactionRegistry.isHostile(other, faction);
      return new Relation(rivals ? RIVAL_OPINION : FRIENDLY_OPINION, RelationState.NEUTRAL, 0L);
   }

   public static record Relation(int opinion, RelationState state, long since) {
   }
}
