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
    * A person's character: two of these, never a pair of opposites. Each weighs on what the ruler does,
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
      DRUNKARD(1.0, 1.0, 1.0, 1.0, 1.3, 1.0, 1.5, 1.0, 0.9, false);

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

      Character opposite() {
         return switch (this) {
            case AMBITIOUS -> CONTENT;
            case CONTENT -> AMBITIOUS;
            case CRUEL -> KIND;
            case KIND -> CRUEL;
            case HONEST -> DECEITFUL;
            case DECEITFUL -> HONEST;
            case HEALTHY -> SICKLY;
            case SICKLY -> HEALTHY;
            default -> this;
         };
      }
   }

   private static final Character[] CHARACTERS = Character.values();

   private FactionPeople() {
   }

   /** Two traits of character, drawn at random, never opposites. */
   public static int newCharacter(RandomSource random) {
      Character first = CHARACTERS[random.nextInt(CHARACTERS.length)];
      Character second;
      do {
         second = CHARACTERS[random.nextInt(CHARACTERS.length)];
      } while (second == first || second == first.opposite());

      return first.bit() | second.bit();
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
