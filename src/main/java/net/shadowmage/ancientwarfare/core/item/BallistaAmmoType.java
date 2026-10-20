package net.shadowmage.ancientwarfare.core.item;

public enum BallistaAmmoType {
   WOOD("ammo_ballista_bolt", 18.0F, 2.0F, false, false, true),
   IRON("ammo_ballista_bolt_iron", 30.0F, 2.0F, false, false, true),
   FLAME("ammo_ballista_bolt_flame", 16.0F, 2.2F, true, false, true),
   EXPLOSIVE("ammo_ballista_bolt_explosive", 15.0F, 2.6F, true, true, false);

   private final String id;
   private final float damage;
   private final float weight;
   private final boolean flaming;
   private final boolean explosive;
   private final boolean persistent;

   private BallistaAmmoType(String id, float damage, float weight, boolean flaming, boolean explosive, boolean persistent) {
      this.id = id;
      this.damage = damage;
      this.weight = weight;
      this.flaming = flaming;
      this.explosive = explosive;
      this.persistent = persistent;
   }

   public String id() {
      return this.id;
   }

   public float damage() {
      return this.damage;
   }

   public float weight() {
      return this.weight;
   }

   public boolean flaming() {
      return this.flaming;
   }

   public boolean explosive() {
      return this.explosive;
   }

   public boolean persistent() {
      return this.persistent;
   }
}
