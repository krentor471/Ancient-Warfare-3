package net.shadowmage.ancientwarfare.core.item;

public enum HwachaAmmoType {
   STANDARD("ammo_hwacha_rocket", 5.0F, false, false, false),
   FLAME("ammo_hwacha_rocket_flame", 5.5F, true, false, false),
   EXPLOSIVE("ammo_hwacha_rocket_explosive", 7.0F, true, true, false),
   AIRBURST("ammo_hwacha_rocket_airburst", 0.0F, false, false, true);

   private final String id;
   private final float damage;
   private final boolean flaming;
   private final boolean explosive;
   private final boolean airburst;

   private HwachaAmmoType(String id, float damage, boolean flaming, boolean explosive, boolean airburst) {
      this.id = id;
      this.damage = damage;
      this.flaming = flaming;
      this.explosive = explosive;
      this.airburst = airburst;
   }

   public String id() {
      return this.id;
   }

   public float damage() {
      return this.damage;
   }

   public boolean flaming() {
      return this.flaming;
   }

   public boolean explosive() {
      return this.explosive;
   }

   public boolean airburst() {
      return this.airburst;
   }

   public String effectTranslationKey() {
      return "tooltip.ancientwarfare.hwacha_ammo." + switch (this) {
         case STANDARD -> "standard";
         case FLAME -> "flame";
         case EXPLOSIVE -> "explosive";
         case AIRBURST -> "airburst";
      };
   }
}
