package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class ShipCaptainNpcEntity extends ModernSoldierNpcEntity {
   public ShipCaptainNpcEntity(EntityType<? extends ShipCaptainNpcEntity> entityType, Level level) {
      super(entityType, level);
      this.setNpcType("ship_captain");
      this.setNpcSubtype("ship_captain");
   }

   @Override
   public String getNpcType() {
      return "ship_captain";
   }

   @Override
   public String getNpcSubtype() {
      return "ship_captain";
   }

   @Override
   public Component m_5677_() {
      return Component.m_237115_("entity.ancientwarfare.ship_captain");
   }
}
