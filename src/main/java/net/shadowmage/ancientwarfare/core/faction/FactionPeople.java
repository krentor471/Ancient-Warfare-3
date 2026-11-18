package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/**
 * Rulers and courtiers grow old. A year passes every YEAR_DAYS days; each person has a day of birth
 * and a look, a number the diplomacy screen draws their portrait from.
 */
public final class FactionPeople {
   /** Days in a year of the realm. */
   public static final int YEAR_DAYS = 2;
   /** No one lives past this. */
   private static final int OLDEST = 95;

   /**
    * A person's character: three of these, never two that contradict each other. Each weighs on what the ruler does,
    * times the usual chance: of war with factions and with players, of raids, of how a gift moves
    * him, of falling ill and dying of age, of his courtiers plotting, of his secrets being found, and of
    * his traders' prices.
    */
   public enum Character {
      AMBITIOUS(1.4, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, true),
      CONTENT(0.6, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, true),
      CRUEL(1.0, 1.5, 1.5, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, false),
      KIND(1.0, 0.6, 0.5, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, true),
      PIOUS(1.0, 1.0, 1.0, 1.3, 1.0, 1.0, 1.0, 1.0, 1.0, true),
      HONEST(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.5, 1.0, true),
      DECEITFUL(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 1.0, false),
      PARANOID(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.4, 1.0, 1.0, false),
      HEALTHY(1.0, 1.0, 1.0, 1.0, 0.5, 0.6, 1.0, 1.0, 1.0, true),
      SICKLY(1.0, 1.0, 1.0, 1.0, 2.0, 1.3, 1.0, 1.0, 1.0, false),
      DRUNKARD(1.0, 1.0, 1.0, 1.0, 1.3, 1.0, 1.5, 1.0, 0.9, false),
      BRAVE(1.15, 1.1, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, true),
      CRAVEN(0.7, 0.7, 0.8, 1.0, 1.0, 1.0, 1.2, 1.0, 1.0, false),
      DILIGENT(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.8, 1.0, 0.95, true),
      LAZY(0.85, 1.0, 1.0, 1.0, 1.0, 1.0, 1.2, 1.0, 1.05, false),
      PATIENT(0.8, 0.8, 0.9, 1.0, 1.0, 1.0, 0.9, 1.0, 1.0, true),
      WRATHFUL(1.2, 1.4, 1.2, 0.85, 1.0, 1.0, 1.0, 1.0, 1.0, false),
      HUMBLE(1.0, 1.0, 1.0, 1.2, 1.0, 1.0, 0.8, 1.0, 1.0, true),
      ARROGANT(1.1, 1.2, 1.0, 0.7, 1.0, 1.0, 1.2, 1.0, 1.05, false),
      TEMPERATE(1.0, 1.0, 1.0, 1.0, 0.8, 0.85, 1.0, 1.0, 1.0, true),
      GLUTTONOUS(1.0, 1.0, 1.0, 1.1, 1.3, 1.2, 1.0, 1.0, 1.0, false),
      JUST(1.0, 0.9, 0.7, 1.0, 1.0, 1.0, 0.9, 0.8, 1.0, true),
      ARBITRARY(1.0, 1.1, 1.3, 1.0, 1.0, 1.0, 1.3, 1.0, 1.1, false),
      CYNICAL(1.0, 1.0, 1.0, 0.8, 1.0, 1.0, 1.0, 1.2, 1.0, false),
      GREGARIOUS(1.0, 1.0, 1.0, 1.3, 1.0, 1.0, 1.0, 1.2, 1.0, true),
      SHY(1.0, 1.0, 1.0, 0.8, 1.0, 1.0, 1.0, 0.8, 1.0, false),
      TRUSTING(1.0, 0.9, 1.0, 1.1, 1.0, 1.0, 1.5, 1.2, 1.0, true),
      FORGIVING(0.9, 0.7, 0.9, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, true),
      VENGEFUL(1.2, 1.4, 1.1, 0.9, 1.0, 1.0, 1.0, 1.0, 1.0, false),
      // Traits of faith: the zealot hates other creeds, the tolerant shrugs, the lustful sins.
      ZEALOUS(1.1, 1.1, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, true),
      TOLERANT(0.9, 0.9, 1.0, 1.1, 1.0, 1.0, 1.0, 1.0, 1.0, true),
      LUSTFUL(1.0, 1.0, 1.0, 1.0, 1.1, 1.0, 1.1, 1.5, 1.0, false);

      final double factionWar;
      final double playerWar;
      final double raid;
      final double gift;
      final double illness;
      final double oldAge;
      final double plot;
      final double secret;
      final double prices;
      /** Whether the screen shows it as a virtue, in green, or a vice, in red. */
      public final boolean virtue;

      Character(double factionWar, double playerWar, double raid, double gift, double illness, double oldAge, double plot, double secret, double prices, boolean virtue) {
         this.factionWar = factionWar;
         this.playerWar = playerWar;
         this.raid = raid;
         this.gift = gift;
         this.illness = illness;
         this.oldAge = oldAge;
         this.plot = plot;
         this.secret = secret;
         this.prices = prices;
         this.virtue = virtue;
      }

      public int bit() {
         return 1 << this.ordinal();
      }

      public Component title() {
         return Component.translatable("character.ancientwarfare." + this.name().toLowerCase(java.util.Locale.ROOT));
      }

      public Component description() {
         return Component.translatable("character.ancientwarfare." + this.name().toLowerCase(java.util.Locale.ROOT) + ".tip");
      }

      /** Whether a person could not be both this and that. */
      boolean clashes(Character other) {
         return other == this || clash(this, other) || clash(other, this);
      }

      private static boolean clash(Character one, Character two) {
         return switch (one) {
            case AMBITIOUS -> two == CONTENT || two == LAZY;
            case CRUEL -> two == KIND || two == FORGIVING || two == JUST;
            case HONEST -> two == DECEITFUL;
            case HEALTHY -> two == SICKLY || two == DRUNKARD || two == GLUTTONOUS;
            case PIOUS -> two == CYNICAL || two == LUSTFUL;
            case ZEALOUS -> two == TOLERANT || two == CYNICAL;
            case PARANOID -> two == TRUSTING || two == GREGARIOUS;
            case DRUNKARD -> two == TEMPERATE;
            case BRAVE -> two == CRAVEN;
            case DILIGENT -> two == LAZY;
            case PATIENT -> two == WRATHFUL;
            case HUMBLE -> two == ARROGANT;
            case TEMPERATE -> two == GLUTTONOUS;
            case JUST -> two == ARBITRARY;
            case GREGARIOUS -> two == SHY;
            case FORGIVING -> two == VENGEFUL || two == WRATHFUL;
            case KIND -> two == WRATHFUL || two == VENGEFUL;
            default -> false;
         };
      }

      /** Whether a ruler of this disposition could not be of this character: a warlike coward, a generous miser. */
      boolean clashes(FactionRulers.Trait trait) {
         return switch (trait) {
            case WARLIKE -> this == CONTENT || this == CRAVEN || this == KIND || this == FORGIVING;
            case CAUTIOUS -> this == WRATHFUL || this == BRAVE || this == ARROGANT || this == TRUSTING;
            case GREEDY -> this == CONTENT || this == HUMBLE || this == KIND;
            case GENEROUS -> this == CRUEL || this == ARBITRARY || this == GLUTTONOUS;
         };
      }
   }

   private static final Character[] CHARACTERS = Character.values();

   private FactionPeople() {
   }

   private static final int TRAITS_EACH = 3;

   /** Traits of character drawn at random, none contradicting another. */
   public static int newCharacter(RandomSource random) {
      return newCharacter(random, null);
   }

   /** Traits of character for a ruler or courtier of this disposition: none contradicting it either. */
   public static int newCharacter(RandomSource random, FactionRulers.Trait disposition) {
      List<Character> chosen = new ArrayList<>();
      for (int tries = 0; chosen.size() < TRAITS_EACH && tries < 100; tries++) {
         Character next = CHARACTERS[random.nextInt(CHARACTERS.length)];
         if ((disposition == null || !next.clashes(disposition)) && chosen.stream().noneMatch(next::clashes)) {
            chosen.add(next);
         }
      }

      int character = 0;
      for (Character trait : chosen) {
         character |= trait.bit();
      }

      return character;
   }

   /** A character saved before contradictions were weeded out, with any trait that contradicts an earlier one or the disposition dropped. */
   public static int consistent(int character, FactionRulers.Trait disposition) {
      int kept = 0;
      List<Character> chosen = new ArrayList<>();
      for (Character trait : CHARACTERS) {
         if ((character & trait.bit()) != 0
            && (disposition == null || !trait.clashes(disposition))
            && chosen.stream().noneMatch(trait::clashes)) {
            chosen.add(trait);
            kept |= trait.bit();
         }
      }

      return kept;
   }

   /** The character of someone from a world saved before it was kept, the same each time it is read. */
   static int oldCharacter(int look) {
      return newCharacter(RandomSource.create(look));
   }

   public static List<Character> traits(int character) {
      List<Character> traits = new ArrayList<>();
      for (Character trait : CHARACTERS) {
         if ((character & trait.bit()) != 0) {
            traits.add(trait);
         }
      }

      return traits;
   }

   public static boolean has(int character, Character trait) {
      return (character & trait.bit()) != 0;
   }

   /** The product of one effect over all of a person's traits. */
   static double factor(int character, ToDoubleFunction<Character> effect) {
      double factor = 1.0;
      for (Character trait : traits(character)) {
         factor *= effect.applyAsDouble(trait);
      }

      return factor;
   }

   public static int age(long born, long day) {
      return born == Long.MIN_VALUE ? 0 : (int)Math.max(0L, (day - born) / YEAR_DAYS);
   }

   public static int years(long since, long day) {
      return (int)Math.max(0L, (day - since) / YEAR_DAYS);
   }

   public static int newLook(RandomSource random) {
      return random.nextInt() & Integer.MAX_VALUE;
   }

   /** The day of birth of someone who is age years old on this day. */
   public static long bornAt(long day, int age, RandomSource random) {
      return day - (long)age * YEAR_DAYS - random.nextInt(YEAR_DAYS);
   }

   /** Looks for someone from a world saved before looks were kept, the same each time it is read. */
   static int oldLook(String key, int name) {
      return (key.hashCode() * 31 + name * 7919) & Integer.MAX_VALUE;
   }

   /** Born so as to be between thirty and fifty on this day. */
   static long oldBorn(int look, long day) {
      return day - (30L + look % 21) * YEAR_DAYS;
   }

   /** The chance in a day that someone of this age dies of old age alone. */
   static double oldAgeDeath(int age) {
      if (age >= OLDEST) {
         return 1.0;
      }

      return age < 60 ? 0.0 : (age - 58) * 0.0012;
   }

   /** The chance in a day that someone of this age falls ill. */
   static double illness(int age) {
      return 0.004 + Math.max(0, age - 40) * 0.0005;
   }

   /** The chance in a day that someone ill of this age dies of it. */
   static double illnessDeath(int age) {
      return 0.02 + Math.max(0, age - 45) * 0.002;
   }

   /** The chance in a day that someone ill of this age gets well. */
   static double recovery(int age) {
      return Math.max(0.05, 0.2 - Math.max(0, age - 30) * 0.002);
   }
}
