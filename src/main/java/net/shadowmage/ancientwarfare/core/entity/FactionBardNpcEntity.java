package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;

/**
 * A faction's bard. When anyone is near to listen, he stops where he is and plays his nation's
 * tunes (see BardTunes), one after another with a breath between; a bard kept by a tavern goes
 * back to his corner of it between songs.
 */
public class FactionBardNpcEntity extends FactionNpcEntity {
   private static final double AUDIENCE_RANGE = 16.0;
   private static final int PAUSE_TICKS = 100;
   private static final double HOME_RANGE = 4.0;
   private BlockPos home;
   private BardTunes.Tune tune;
   private int beat;
   private int ticks;
   private int pause;

   public FactionBardNpcEntity(EntityType<? extends FactionBardNpcEntity> entityType, Level level) {
      super(entityType, level, FactionNpcRole.BARD);
   }

   public static Builder createAttributes() {
      return createMobAttributes().add(Attributes.MAX_HEALTH, 18.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 20.0);
   }

   protected void registerGoals() {
      this.addSharedGoals();
      this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
      this.goalSelector.addGoal(2, new PerformGoal());
   }

   public void setHome(BlockPos home) {
      this.home = home;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (this.home != null) {
         tag.putLong("BardHome", this.home.asLong());
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.home = tag.contains("BardHome") ? BlockPos.of(tag.getLong("BardHome")) : null;
   }

   private Player listener() {
      return this.level().getNearestPlayer(this, AUDIENCE_RANGE);
   }

   /** One tick of the song: the notes that fall on this beat, and the next tune once one ends. */
   private void play() {
      if (this.tune == null) {
         if (this.pause > 0) {
            this.pause--;
            return;
         }

         List<BardTunes.Tune> tunes = BardTunes.of(this.getFactionName());
         this.tune = tunes.get(this.random.nextInt(tunes.size()));
         this.beat = 0;
         this.ticks = 0;
      }

      if (this.ticks++ % this.tune.ticksPerBeat() != 0) {
         return;
      }

      if (this.beat >= this.tune.length()) {
         this.tune = null;
         this.pause = PAUSE_TICKS;
         return;
      }

      strike(this.tune.instrument(), this.tune.notes(), 1.0F);
      strike(this.tune.bass(), this.tune.bassNotes(), 0.7F);
      String drums = this.tune.drums();
      if (!drums.isEmpty()) {
         char mark = drums.charAt(this.beat % drums.length());
         if (mark == 'X') {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.RECORDS, 0.9F, 1.0F);
         } else if (mark == 'x') {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.RECORDS, 0.6F, 1.0F);
         }
      }

      this.beat++;
   }

   private void strike(net.minecraft.sounds.SoundEvent instrument, int[] notes, float volume) {
      if (this.beat >= notes.length || notes[this.beat] < 0) {
         return;
      }

      int note = notes[this.beat];
      this.level().playSound(null, this.getX(), this.getY(), this.getZ(), instrument, SoundSource.RECORDS, volume, BardTunes.pitch(note));
      if (volume >= 1.0F && this.level() instanceof ServerLevel server) {
         // A note particle takes its colour from its speed's x, a 24th of the way round per semitone.
         server.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + 2.2, this.getZ(), 0, (note - 54) / 24.0, 0.0, 0.0, 1.0);
      }
   }

   /** Stands and plays while someone listens; between songs a tavern's bard goes back to his corner. */
   private final class PerformGoal extends Goal {
      private PerformGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      }

      @Override
      public boolean canUse() {
         FactionBardNpcEntity bard = FactionBardNpcEntity.this;
         return bard.getTarget() == null && !bard.isInWater() && bard.listener() != null;
      }

      @Override
      public boolean canContinueToUse() {
         return this.canUse();
      }

      @Override
      public boolean requiresUpdateEveryTick() {
         return true;
      }

      @Override
      public void stop() {
         FactionBardNpcEntity.this.tune = null;
      }

      @Override
      public void tick() {
         FactionBardNpcEntity bard = FactionBardNpcEntity.this;
         Player listener = bard.listener();
         if (listener != null) {
            bard.getLookControl().setLookAt(listener, 30.0F, 30.0F);
         }

         if (bard.tune == null && bard.home != null && bard.home.distToCenterSqr(bard.position()) > HOME_RANGE * HOME_RANGE) {
            if (bard.getNavigation().isDone()) {
               bard.getNavigation().moveTo(bard.home.getX() + 0.5, bard.home.getY(), bard.home.getZ() + 0.5, 0.8);
            }

            return;
         }

         bard.getNavigation().stop();
         bard.play();
      }
   }
}
