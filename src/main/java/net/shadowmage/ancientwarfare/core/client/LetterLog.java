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
 * press that comes from an envoy, or comes unbidden (a request from the player's own town, a court,
 * a rival), is gathered with the lines that came with it into a letter, read and answered on the letters page.
 */
public final class LetterLog {
   public enum Kind {
      CHOICE
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

   /** When the player last spoke to someone or pressed a button: what answers that goes to the chat. */
   private static long promptedAt = Long.MIN_VALUE / 2;

   public static void prompted() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level != null) {
         promptedAt = minecraft.level.getGameTime();
      }
   }

   /**
    * The tick's lines. The village head's offers open on his own page; an envoy's word, or any offer
    * that comes unbidden (from the player's own towns, a court, a rival), becomes a letter; whatever
    * answers the player's own word or click stays in the chat.
    */
   public static void flush() {
      if (BATCH.isEmpty()) {
         return;
      }

      List<Component> lines = new ArrayList<>(BATCH);
      BATCH.clear();
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null) {
         lines.forEach(line -> minecraft.gui.getChat().addMessage(line));
         return;
      }

      Component panel = lines.stream().filter(line -> key(line).equals(PANEL)).findFirst().orElse(null);
      if (panel != null) {
         lines.remove(panel);
         LetterScreen.openPanel(((TranslatableContents)panel.getContents()).getArgs().length > 0 && ((TranslatableContents)panel.getContents()).getArgs()[0] instanceof Component name ? name : panel, lines);
         return;
      }

      boolean prompted = minecraft.level.getGameTime() - promptedAt < 40L;
      boolean envoy = lines.stream().anyMatch(line -> mentions(line, "envoy."));
      boolean choice = lines.stream().anyMatch(LetterLog::button);
      if (!choice || prompted && !envoy) {
         lines.forEach(line -> minecraft.gui.getChat().addMessage(line));
         return;
      }

      LETTERS.add(new Letter(Kind.CHOICE, lines, minecraft.level.getDayTime()));
      while (LETTERS.size() > KEPT) {
         LETTERS.remove(0);
      }

      noticeUntil = minecraft.level.getGameTime() + 200L;
      if (prompted && minecraft.screen == null) {
         // An envoy the player is talking to: the letter opens at once.
         LetterScreen.open(Kind.CHOICE);
      }
   }

   private static final String PANEL = "head.ancientwarfare.panel";

   private static String key(Component line) {
      return line.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "";
   }

   private static boolean button(Component message) {
      return message.visit((style, text) -> {
         ClickEvent click = style.getClickEvent();
         return click != null && click.getAction() == ClickEvent.Action.RUN_COMMAND && click.getValue().startsWith("/aw")
            ? java.util.Optional.of(Boolean.TRUE)
            : java.util.Optional.empty();
      }, Style.EMPTY).isPresent();
   }

   private static boolean mentions(Component message, String prefix) {
      if (message.getContents() instanceof TranslatableContents translatable) {
         if (translatable.getKey().startsWith(prefix)) {
            return true;
         }

         for (Object arg : translatable.getArgs()) {
            if (arg instanceof Component inner && mentions(inner, prefix)) {
               return true;
            }
         }
      }

      return message.getSiblings().stream().anyMatch(sibling -> mentions(sibling, prefix));
   }
}
