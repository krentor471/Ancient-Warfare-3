package net.shadowmage.ancientwarfare.core.menu;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import net.minecraft.world.inventory.DataSlot;

public final class WarehouseDataSlots {
   private WarehouseDataSlots() {
   }

   public static DataSlot[] longValue(final LongSupplier getter, final LongConsumer setter) {
      DataSlot[] slots = new DataSlot[4];

      for (int i = 0; i < slots.length; i++) {
         final int shift = i * 16;
         slots[i] = new DataSlot() {
            public int m_6501_() {
               return (int)(getter.getAsLong() >>> shift) & 65535;
            }

            public void m_6422_(int value) {
               long mask = 65535L << shift;
               setter.accept(getter.getAsLong() & ~mask | (long)(value & 65535) << shift);
            }
         };
      }

      return slots;
   }

   public static DataSlot[] intValue(final IntSupplier getter, final IntConsumer setter) {
      DataSlot[] slots = new DataSlot[2];

      for (int i = 0; i < slots.length; i++) {
         final int shift = i * 16;
         slots[i] = new DataSlot() {
            public int m_6501_() {
               return getter.getAsInt() >>> shift & 65535;
            }

            public void m_6422_(int value) {
               int mask = 65535 << shift;
               setter.accept(getter.getAsInt() & ~mask | (value & 65535) << shift);
            }
         };
      }

      return slots;
   }
}
