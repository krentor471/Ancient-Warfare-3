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
      if (projectile.getOwner() instanceof CombatNpcEntity swordsman
         && projectile.getPersistentData().getBoolean("ancientwarfareNpcSummonedSword")
         && event.getRayTraceResult() instanceof EntityHitResult hit) {
         ResourceLocation projectileType = BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType());
         if (projectileType != null
            && "slashblade".equals(projectileType.getNamespace())
            && hit.getEntity().getId() != projectile.getPersistentData().getInt("ancientwarfareNpcSummonedSwordTarget")) {
            event.setCanceled(true);
            return;
         }
      }

      if (projectile instanceof AbstractArrow && projectile.getOwner() instanceof NpcBaseEntity && !projectile.level().isClientSide()) {
         if (projectile.getOwner() instanceof PlayerOwnedNpcEntity rider
            && rider.isLogicalDragonRider()
            && event.getRayTraceResult() instanceof EntityHitResult hitx
            && IceAndFireDragonCompat.isDragon(hitx.getEntity())) {
            event.setCanceled(true);
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(0.92));
            return;
         }
      }
   }
}
