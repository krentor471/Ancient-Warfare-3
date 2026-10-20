package net.shadowmage.ancientwarfare.core.forge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireDragonCompat;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.NpcBaseEntity;
import net.shadowmage.ancientwarfare.core.entity.PlayerOwnedNpcEntity;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.FORGE
)
public final class ForgeNpcProjectileEvents {
   private ForgeNpcProjectileEvents() {
   }

   @SubscribeEvent
   public static void onNpcArrowImpact(ProjectileImpactEvent event) {
      Projectile projectile = event.getProjectile();
      if (projectile.m_19749_() instanceof CombatNpcEntity swordsman
         && projectile.getPersistentData().m_128471_("ancientwarfareNpcSummonedSword")
         && event.getRayTraceResult() instanceof EntityHitResult hit) {
         ResourceLocation projectileType = BuiltInRegistries.f_256780_.m_7981_(projectile.m_6095_());
         if (projectileType != null
            && "slashblade".equals(projectileType.m_135827_())
            && hit.m_82443_().m_19879_() != projectile.getPersistentData().m_128451_("ancientwarfareNpcSummonedSwordTarget")) {
            event.setCanceled(true);
            return;
         }
      }

      if (projectile instanceof AbstractArrow && projectile.m_19749_() instanceof NpcBaseEntity && !projectile.m_9236_().m_5776_()) {
         if (projectile.m_19749_() instanceof PlayerOwnedNpcEntity rider
            && rider.isLogicalDragonRider()
            && event.getRayTraceResult() instanceof EntityHitResult hitx
            && IceAndFireDragonCompat.isDragon(hitx.m_82443_())) {
            event.setCanceled(true);
            projectile.m_20256_(projectile.m_20184_().m_82490_(0.92));
            return;
         }
      }
   }
}
