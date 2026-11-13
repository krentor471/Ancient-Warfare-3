package net.shadowmage.ancientwarfare.core.faction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;

/**
 * Plague and hunger are counted on paper; this makes them seen. When a loaded town loses people to
 * sickness or famine, as many of the townsfolk walking its streets die in proportion.
 */
public final class TownPopulation {
   private static final int CHECK_TICKS = 400;
   private static final int PLAYER_RANGE = 160;
   /** Population last seen per settlement; not saved, so a restart only starts counting afresh. */
   private static final Map<Integer, Integer> LAST = new HashMap<>();

   private TownPopulation() {
   }

   public static void tick(ServerLevel level, FactionWorldData data) {
      if (level.getGameTime() % CHECK_TICKS != 0L || level.players().isEmpty()) {
         return;
      }

      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!settlement.built() || FactionAI.isFort(settlement)) {
            continue;
         }

         int now = settlement.population();
         Integer before = LAST.put(settlement.id(), now);
         boolean dying = settlement.plague() > 0 || settlement.famine > 0 || settlement.health() < 30;
         if (before == null || now >= before || !dying) {
            continue;
         }

         if (!level.hasNearbyAlivePlayer(settlement.center().getX(), settlement.center().getY(), settlement.center().getZ(), PLAYER_RANGE)) {
            continue;
         }

         AABB area = new AABB(settlement.min(), settlement.max().offset(1, 1, 1)).inflate(8.0, 32.0, 8.0);
         List<FactionNpcEntity> folk = level.getEntitiesOfClass(
            FactionNpcEntity.class, area, npc -> npc.isAlive() && npc.getFactionName().equals(settlement.faction()) && FactionSimulation.isCivilian(npc)
         );
         if (folk.isEmpty()) {
            continue;
         }

         double share = (double)(before - now) * folk.size() / Math.max(1, before);
         int deaths = (int)share + (level.getRandom().nextDouble() < share - (int)share ? 1 : 0);
         // Never the last few: someone is always left to bury the dead.
         deaths = Math.min(deaths, folk.size() - 2);
         java.util.Collections.shuffle(folk, new java.util.Random(level.getRandom().nextLong()));
         for (int i = 0; i < deaths; i++) {
            FactionNpcEntity npc = folk.get(i);
            level.sendParticles(ParticleTypes.SOUL, npc.getX(), npc.getY() + 1.0, npc.getZ(), 6, 0.3, 0.5, 0.3, 0.01);
            level.playSound(null, npc.blockPosition(), SoundEvents.VILLAGER_DEATH, SoundSource.NEUTRAL, 0.8F, 0.9F);
            npc.discard();
         }
      }
   }
}
