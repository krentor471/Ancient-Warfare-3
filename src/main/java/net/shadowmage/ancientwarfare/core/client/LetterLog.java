package net.shadowmage.ancientwarfare.core.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Offers and choices the world puts to the player, kept out of the chat. A message with buttons to
 * press (an envoy's demand, a council's question, a trader's wares) and every word about caravans
 * is gathered with the lines that came with it into a letter, read and answered on the letters page.
 */
public final class LetterLog {
   public enum Kind {
      CHOICE,
      CARAVAN
   }

   public static final class Letter {
      public final Kind kind;
      public final List<Component> lines;
      public final long time;
      public boolean read;

      Letter(Kind kind, List<Component> lines, long time) {
         this.kind = kind;
         this.lines = lines;
         this.time = time;
      }
   }

   private static final int KEPT = 60;
   private static final List<Letter> LETTERS = new ArrayList<>();
   private static final List<Component> BATCH = new ArrayList<>();
   private static long noticeUntil;

   private LetterLog() {
   }

   public static List<Letter> letters() {
      return LETTERS;
   }

   public static int unread() {
      return (int)LETTERS.stream().filter(letter -> !letter.read).count();
   }

   public static boolean noticeShowing() {
      return Minecraft.getInstance().level != null && Minecraft.getInstance().level.getGameTime() < noticeUntil && unread() > 0;
   }

   /** A system line from the server, held back until the end of the tick with the rest of its lines. */
   public static void hold(Component message) {
      BATCH.add(message);
   }

   /** The tick's lines: a letter if any of them has a button or speaks of caravans, otherwise into the chat as usual. */
   public static void flush() {
      if (BATCH.isEmpty()) {
         return;
      }

      List<Component> lines = new ArrayList<>(BATCH);
      BATCH.clear();
      Minecraft minecraft = Minecraft.getInstance();
      boolean caravan = lines.stream().anyMatch(LetterLog::caravan);
      boolean choice = lines.stream().anyMatch(LetterLog::button);
      if (!caravan && !choice || minecraft.level == null) {
         lines.forEach(line -> minecraft.gui.getChat().addMessage(line));
         return;
      }

      LETTERS.add(new Letter(caravan ? Kind.CARAVAN : Kind.CHOICE, lines, minecraft.level.getDayTime()));
      while (LETTERS.size() > KEPT) {
         LETTERS.remove(0);
      }

      noticeUntil = minecraft.level.getGameTime() + 200L;
      if (choice && !caravan && minecraft.screen == null && minecraft.hitResult instanceof net.minecraft.world.phys.EntityHitResult) {
         // The player is talking to someone who waits on an answer: the page opens at once.
         LetterScreen.open(Kind.CHOICE);
      }
   }

   private static boolean button(Component message) {
      return message.visit((style, text) -> {
         ClickEvent click = style.getClickEvent();
         return click != null && click.getAction() == ClickEvent.Action.RUN_COMMAND && click.getValue().startsWith("/aw")
            ? java.util.Optional.of(Boolean.TRUE)
            : java.util.Optional.empty();
      }, Style.EMPTY).isPresent();
   }

   private static boolean caravan(Component message) {
      return mentions(message);
   }

   private static boolean mentions(Component message) {
      if (message.getContents() instanceof TranslatableContents translatable) {
         if (translatable.getKey().contains("caravan")) {
            return true;
         }

         for (Object arg : translatable.getArgs()) {
            if (arg instanceof Component inner && mentions(inner)) {
               return true;
            }
         }
      }

      return message.getSiblings().stream().anyMatch(LetterLog::mentions);
   }
}
