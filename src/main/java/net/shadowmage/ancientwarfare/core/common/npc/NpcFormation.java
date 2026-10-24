package net.shadowmage.ancientwarfare.core.common.npc;

import java.util.ArrayList;
import java.util.List;

/**
 * Shapes a group takes when sent to a point. Slots are block offsets relative to the clicked point:
 * {@code side} runs left to right across the front, {@code depth} counts rows back from the front row.
 */
public enum NpcFormation {
   SQUARE("square", 2, 2),
   LINE("line", 2, 2),
   SHIELD_WALL("shield_wall", 1, 1),
   COLUMN("column", 2, 2),
   WEDGE("wedge", 2, 2);

   private static final int COLUMN_WIDTH = 3;
   private static final int LINE_RANK_SIZE = 12;
   private static final int SHIELD_WALL_RANK_SIZE = 16;
   private static final int MAX_RANKS = 4;
   private final String name;
   private final int sideSpacing;
   private final int depthSpacing;

   NpcFormation(String name, int sideSpacing, int depthSpacing) {
      this.name = name;
      this.sideSpacing = sideSpacing;
      this.depthSpacing = depthSpacing;
   }

   public String serializedName() {
      return this.name;
   }

   public String translationKey() {
      return "guistrings.npc.formation." + this.name;
   }

   public NpcFormation next() {
      NpcFormation[] values = values();
      return values[(this.ordinal() + 1) % values.length];
   }

   public static NpcFormation byName(String name) {
      for (NpcFormation formation : values()) {
         if (formation.name.equals(name)) {
            return formation;
         }
      }

      return SQUARE;
   }

   public int sideSpacing() {
      return this.sideSpacing;
   }

   public List<Slot> slots(int count) {
      return this.slots(count, 0);
   }

   /**
    * Slots for {@code count} NPCs, front row first, each row left to right. {@code columns} above zero sets the width of
    * the front for the square, line and shield wall, as when the player draws the front; the column and wedge keep
    * their shape.
    */
   public List<Slot> slots(int count, int columns) {
      List<Slot> slots = new ArrayList<>(count);
      if (count <= 0) {
         return slots;
      } else if (this == WEDGE) {
         // Row r of the wedge holds 2r + 1 NPCs, the tip in front.
         for (int row = 0; slots.size() < count; row++) {
            int width = Math.min(2 * row + 1, count - slots.size());
            this.addRow(slots, row, width);
         }

         return slots;
      } else {
         int width = columns > 0 && this != COLUMN ? Math.min(columns, count) : switch (this) {
            case SQUARE -> (int)Math.ceil(Math.sqrt(count));
            case LINE -> ceilDiv(count, Math.min(MAX_RANKS, ceilDiv(count, LINE_RANK_SIZE)));
            case SHIELD_WALL -> ceilDiv(count, Math.min(MAX_RANKS, ceilDiv(count, SHIELD_WALL_RANK_SIZE)));
            case COLUMN -> Math.min(count, COLUMN_WIDTH);
            case WEDGE -> throw new IllegalStateException();
         };

         for (int row = 0; slots.size() < count; row++) {
            this.addRow(slots, row, Math.min(width, count - slots.size()));
         }

         return slots;
      }
   }

   private void addRow(List<Slot> slots, int row, int width) {
      // Offsets are whole blocks; an even row is shifted half a gap to the right so it stays on the grid.
      int left = -(width - 1) * this.sideSpacing / 2;

      for (int i = 0; i < width; i++) {
         slots.add(new Slot(left + i * this.sideSpacing, row * this.depthSpacing, row));
      }
   }

   private static int ceilDiv(int a, int b) {
      return (a + b - 1) / b;
   }

   public static record Slot(int side, int depth, int row) {
   }
}
