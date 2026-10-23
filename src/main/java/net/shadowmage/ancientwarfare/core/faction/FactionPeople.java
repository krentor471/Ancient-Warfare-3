package net.shadowmage.ancientwarfare.core.faction;

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

   private FactionPeople() {
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
