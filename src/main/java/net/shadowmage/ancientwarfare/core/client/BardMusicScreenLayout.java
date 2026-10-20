package net.shadowmage.ancientwarfare.core.client;

import java.util.List;

public final class BardMusicScreenLayout {
   public static final int WIDTH = 320;
   public static final int HEIGHT = 240;
   public static final int VISIBLE_TRACKS = 2;
   public static final int TRACK_HEIGHT = 80;
   public static final int MAX_TRACKS = 32;
   public static final int FILE_ROWS = 6;
   public static final int FILE_ROW_HEIGHT = 12;
   public static final BardMusicScreenLayout.Rect LIST = new BardMusicScreenLayout.Rect(8, 24, 304, 164);
   public static final BardMusicScreenLayout.Rect ADD = new BardMusicScreenLayout.Rect(10, 204, 82, 12);
   public static final BardMusicScreenLayout.Rect SAVE = new BardMusicScreenLayout.Rect(178, 204, 62, 12);
   public static final BardMusicScreenLayout.Rect BACK = new BardMusicScreenLayout.Rect(244, 204, 66, 12);

   private BardMusicScreenLayout() {
   }

   public static int trackY(int row) {
      return LIST.y() + row * 80;
   }

   public static List<BardMusicScreenLayout.Rect> fields(int row) {
      int y = trackY(row);
      return List.of(
         new BardMusicScreenLayout.Rect(42, y + 16, 145, 12),
         new BardMusicScreenLayout.Rect(42, y + 40, 145, 12),
         new BardMusicScreenLayout.Rect(220, y + 16, 35, 12),
         new BardMusicScreenLayout.Rect(220, y + 36, 35, 12),
         new BardMusicScreenLayout.Rect(220, y + 56, 35, 12),
         new BardMusicScreenLayout.Rect(264, y + 16, 40, 12)
      );
   }

   public static BardMusicScreenLayout.Rect remove(int row) {
      return new BardMusicScreenLayout.Rect(264, trackY(row) + 40, 18, 12);
   }

   public static BardMusicScreenLayout.Rect fileButton(int row) {
      return new BardMusicScreenLayout.Rect(284, trackY(row) + 40, 20, 12);
   }

   public static BardMusicScreenLayout.Rect popup(int row, int count) {
      BardMusicScreenLayout.Rect anchor = fileButton(row);
      int height = Math.min(6, Math.max(1, count)) * 12 + 4;
      int below = anchor.y() + anchor.height() + 2;
      int y = below + height <= LIST.y() + LIST.height() ? below : anchor.y() - height - 2;
      y = Math.max(LIST.y(), Math.min(y, LIST.y() + LIST.height() - height));
      return new BardMusicScreenLayout.Rect(42, y, 198, height);
   }

   public static int maxFileScroll(int count) {
      return Math.max(0, count - 6);
   }

   public static int fileChoice(BardMusicScreenLayout.Rect popup, double x, double y, int count, int offset) {
      if (popup.contains(x, y) && !(x < (double)(popup.x() + 2)) && !(x >= (double)(popup.x() + popup.width() - 8)) && !(y < (double)(popup.y() + 2))) {
         int visibleRow = (int)(y - (double)popup.y() - 2.0) / 12;
         int choice = Math.max(0, Math.min(offset, maxFileScroll(count))) + visibleRow;
         return visibleRow < 6 && choice < count ? choice : -1;
      } else {
         return -1;
      }
   }

   public static record Rect(int x, int y, int width, int height) {
      public boolean contains(double px, double py) {
         return px >= (double)this.x && py >= (double)this.y && px < (double)(this.x + this.width) && py < (double)(this.y + this.height);
      }
   }
}
