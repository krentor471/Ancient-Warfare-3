package net.shadowmage.ancientwarfare.core.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * What a faction's bards play: old tunes, free of anyone's rights, set for note-block instruments,
 * and each nation's own sound. Courtly folk play the harp and flute, the north flute over a bass,
 * the faithful bells, and orcs and wild men beat drums under a growling bass.
 *
 * A melody is written as notes with their length in beats of the tune, like "A4:2", "-:1" for a
 * rest; a drum line is one mark per beat, repeated under the melody: X a big drum, x a small one,
 * . nothing.
 */
public final class BardTunes {
   /** Lowest and highest notes a note-block instrument plays: F#3 to F#5. */
   private static final int LOWEST = 54;
   private static final int HIGHEST = 78;

   public record Tune(String name, int ticksPerBeat, SoundEvent instrument, int[] notes, SoundEvent bass, int[] bassNotes, String drums) {
      public int length() {
         return Math.max(this.notes.length, this.bassNotes.length);
      }
   }

   private static final List<Tune> COURT = List.of(
      tune(
         "Greensleeves",
         3,
         SoundEvents.NOTE_BLOCK_HARP.value(),
         "A4:2 C5:4 D5:2 E5:3 F5:1 E5:2 D5:4 B4:2 G4:3 A4:1 B4:2 C5:4 A4:2 A4:3 G#4:1 A4:2 B4:4 G#4:2 E4:4 A4:2 "
            + "C5:4 D5:2 E5:3 F5:1 E5:2 D5:4 B4:2 G4:3 A4:1 B4:2 C5:3 B4:1 A4:2 G#4:3 F#4:1 G#4:2 A4:6 "
            + "G5:6 G5:3 F#5:1 E5:2 D5:4 B4:2 G4:3 A4:1 B4:2 C5:4 A4:2 A4:3 G#4:1 A4:2 B4:4 G#4:2 E4:6 "
            + "G5:6 G5:3 F#5:1 E5:2 D5:4 B4:2 G4:3 A4:1 B4:2 C5:3 B4:1 A4:2 G#4:3 F#4:1 G#4:2 A4:6",
         SoundEvents.NOTE_BLOCK_GUITAR.value(),
         "A3:6 G3:6 C4:6 G3:6 A3:6 E3:6 E3:6 A3:6 A3:6 G3:6 C4:6 G3:6 F3:6 E3:6 A3:6 -:6 "
            + "C4:6 C4:6 G3:6 G3:6 A3:6 E3:6 E3:6 E3:6 C4:6 C4:6 G3:6 G3:6 F3:6 E3:6 A3:6",
         ""
      ),
      tune(
         "Estampie",
         2,
         SoundEvents.NOTE_BLOCK_HARP.value(),
         "D5:2 C5:1 D5:1 A4:2 F4:2 G4:1 A4:1 Bb4:1 A4:1 G4:2 F4:2 E4:2 F4:1 G4:1 A4:4 "
            + "D5:2 C5:1 D5:1 A4:2 F4:2 G4:1 A4:1 Bb4:1 G4:1 A4:2 E4:2 D4:4 -:2 "
            + "F4:1 G4:1 A4:2 A4:2 C5:2 Bb4:1 A4:1 G4:2 G4:2 Bb4:2 A4:1 G4:1 F4:2 E4:2 D4:4",
         SoundEvents.NOTE_BLOCK_GUITAR.value(),
         "D3:4 A3:4 D3:4 A3:4 G3:4 D3:4 A3:4 D3:4 D3:4 A3:4 G3:4 A3:4 D3:4 -:4 F3:4 C4:4 G3:4 C4:4 A3:4 D3:4",
         "X.x.X.x."
      ),
      tune(
         "Drunken Sailor",
         2,
         SoundEvents.NOTE_BLOCK_FLUTE.value(),
         "A4:1 A4:1 A4:1 A4:1 A4:1 A4:1 A4:1 D4:2 F4:1 A4:2 G4:1 G4:1 G4:1 G4:1 G4:1 G4:1 G4:1 C4:2 E4:1 G4:2 "
            + "A4:1 A4:1 A4:1 A4:1 A4:1 A4:1 A4:1 B4:1 C5:1 D5:1 C5:1 A4:1 G4:1 E4:2 D4:2 D4:4",
         SoundEvents.NOTE_BLOCK_BASS.value(),
         "D3:4 D3:4 C3:4 C3:4 D3:4 D3:4 C3:4 D3:4",
         "X.x.X.x."
      )
   );

   private static final List<Tune> NORTH = List.of(
      tune(
         "Drunken Sailor",
         2,
         SoundEvents.NOTE_BLOCK_FLUTE.value(),
         "A4:1 A4:1 A4:1 A4:1 A4:1 A4:1 A4:1 D4:2 F4:1 A4:2 G4:1 G4:1 G4:1 G4:1 G4:1 G4:1 G4:1 C4:2 E4:1 G4:2 "
            + "A4:1 A4:1 A4:1 A4:1 A4:1 A4:1 A4:1 B4:1 C5:1 D5:1 C5:1 A4:1 G4:1 E4:2 D4:2 D4:4",
         SoundEvents.NOTE_BLOCK_BASS.value(),
         "D3:4 D3:4 C3:4 C3:4 D3:4 D3:4 C3:4 D3:4",
         "X..xX..x"
      ),
      tune(
         "Song of the Fjord",
         3,
         SoundEvents.NOTE_BLOCK_FLUTE.value(),
         "E4:2 G4:1 A4:3 B4:2 A4:1 G4:3 E4:2 D4:1 E4:6 G4:2 A4:1 B4:3 D5:2 B4:1 A4:3 G4:2 A4:1 E4:6 "
            + "B4:2 D5:1 E5:3 D5:2 B4:1 A4:3 G4:2 A4:1 B4:3 A4:2 G4:1 E4:2 D4:1 E4:6",
         SoundEvents.NOTE_BLOCK_BASS.value(),
         "E3:6 E3:6 D3:6 E3:6 G3:6 D3:6 E3:6 E3:6 E3:6 D3:6 G3:6 E3:6",
         "X.....x....."
      )
   );

   private static final List<Tune> FAITH = List.of(
      tune(
         "Dies Irae",
         4,
         SoundEvents.NOTE_BLOCK_BELL.value(),
         "F4:1 E4:1 F4:1 D4:1 E4:1 C4:1 D4:2 F4:1 F4:1 G4:1 F4:1 E4:1 D4:1 C4:1 E4:1 F4:1 E4:1 D4:2 "
            + "A4:1 A4:1 Bb4:1 A4:1 G4:1 F4:1 E4:1 F4:1 G4:1 F4:1 E4:1 D4:2",
         SoundEvents.NOTE_BLOCK_BASS.value(),
         "D3:4 D3:4 F3:4 D3:4 C3:4 D3:4 F3:4 C3:4 D3:4",
         ""
      ),
      tune(
         "Veni Emmanuel",
         3,
         SoundEvents.NOTE_BLOCK_FLUTE.value(),
         "E4:2 G4:2 B4:2 B4:2 B4:2 A4:2 C5:2 B4:2 A4:2 G4:4 -:2 A4:2 B4:2 G4:2 E4:2 G4:2 A4:2 F#4:2 E4:2 D4:2 E4:6",
         SoundEvents.NOTE_BLOCK_BELL.value(),
         "E4:6 G4:6 A4:6 G4:6 E4:6 D4:6 E4:6",
         ""
      )
   );

   private static final List<Tune> WILD = List.of(
      tune(
         "War Drums",
         2,
         SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(),
         "E3:4 -:2 E3:1 G3:1 E3:4 -:4 A3:4 -:2 G3:1 E3:1 D3:4 -:4 E3:4 -:2 E3:1 G3:1 B3:2 A3:2 G3:2 E3:6",
         SoundEvents.NOTE_BLOCK_BASS.value(),
         "E3:2 E3:2 E3:2 E3:2 E3:2 E3:2 E3:2 E3:2 A3:2 A3:2 A3:2 A3:2 D3:2 D3:2 D3:2 D3:2 E3:2 E3:2 E3:2 E3:2 G3:2 G3:2 E3:2 E3:2",
         "X.X.x.X.XX.x..x."
      ),
      tune(
         "Hunt of the Tusk",
         2,
         SoundEvents.NOTE_BLOCK_BASS.value(),
         "A3:2 C4:1 A3:1 D4:2 C4:1 A3:1 G3:2 A3:2 -:4 A3:2 C4:1 A3:1 E4:2 D4:1 C4:1 A3:4 -:4",
         SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(),
         "A3:8 G3:8 A3:8 A3:8",
         "X..xX.x.X..xXxx."
      )
   );

   private BardTunes() {
   }

   /** The tunes a faction's bards know. */
   public static List<Tune> of(String faction) {
      String name = faction == null ? "" : faction.toLowerCase(Locale.ROOT);
      if (name.contains("norska") || name.contains("viking") || name.contains("north")) {
         return NORTH;
      } else if (name.contains("witch") || name.contains("faith") || name.contains("church") || name.contains("templar")) {
         return FAITH;
      } else if (name.contains("nogg") || name.contains("orc") || name.contains("barbarian") || name.contains("buffloka") || name.contains("brigand") || name.contains("kong")) {
         return WILD;
      }

      return COURT;
   }

   private static Tune tune(String name, int ticksPerBeat, SoundEvent instrument, String melody, SoundEvent bass, String bassLine, String drums) {
      return new Tune(name, ticksPerBeat, instrument, beats(melody), bass, beats(bassLine), drums);
   }

   /** One entry per beat: the note struck on it, or -1 for none. */
   private static int[] beats(String line) {
      List<Integer> beats = new ArrayList<>();
      for (String token : line.trim().split("\\s+")) {
         if (token.isEmpty()) {
            continue;
         }

         String[] parts = token.split(":");
         int length = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
         beats.add(parts[0].equals("-") ? -1 : fit(midi(parts[0])));
         for (int i = 1; i < length; i++) {
            beats.add(-1);
         }
      }

      return beats.stream().mapToInt(Integer::intValue).toArray();
   }

   private static int midi(String note) {
      int semitone = switch (Character.toUpperCase(note.charAt(0))) {
         case 'C' -> 0;
         case 'D' -> 2;
         case 'E' -> 4;
         case 'F' -> 5;
         case 'G' -> 7;
         case 'A' -> 9;
         default -> 11;
      };
      int index = 1;
      if (note.charAt(index) == '#') {
         semitone++;
         index++;
      } else if (note.charAt(index) == 'b') {
         semitone--;
         index++;
      }

      return 12 * (Integer.parseInt(note.substring(index)) + 1) + semitone;
   }

   /** Notes outside the instrument's two octaves move by octaves into them. */
   private static int fit(int midi) {
      while (midi < LOWEST) {
         midi += 12;
      }

      while (midi > HIGHEST) {
         midi -= 12;
      }

      return midi;
   }

   /** The pitch a note block plays this note at. */
   public static float pitch(int midi) {
      return (float)Math.pow(2.0, (midi - 66) / 12.0);
   }
}
