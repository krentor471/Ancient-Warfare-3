package net.shadowmage.ancientwarfare.core.item;

public enum CannonAmmoType {
   IRON_5("ammo_iron_shot_5", 5, CannonAmmoType.Kind.IRON, 10.0F, 0, false),
   IRON_10("ammo_iron_shot_10", 10, CannonAmmoType.Kind.IRON, 15.0F, 0, false),
   IRON_15("ammo_iron_shot_15", 15, CannonAmmoType.Kind.IRON, 30.0F, 0, false),
   IRON_25("ammo_iron_shot_25", 25, CannonAmmoType.Kind.IRON, 45.0F, 0, false),
   GRAPE_5("ammo_grape_shot_5", 5, CannonAmmoType.Kind.GRAPE, 0.0F, 5, true),
   GRAPE_10("ammo_grape_shot_10", 10, CannonAmmoType.Kind.GRAPE, 0.0F, 10, true),
   GRAPE_15("ammo_grape_shot_15", 15, CannonAmmoType.Kind.GRAPE, 0.0F, 15, true),
   GRAPE_25("ammo_grape_shot_25", 25, CannonAmmoType.Kind.GRAPE, 0.0F, 25, true),
   CANISTER_5("ammo_canister_shot_5", 5, CannonAmmoType.Kind.CANISTER, 8.0F, 5, false),
   CANISTER_10("ammo_canister_shot_10", 10, CannonAmmoType.Kind.CANISTER, 8.0F, 10, false),
   CANISTER_15("ammo_canister_shot_15", 15, CannonAmmoType.Kind.CANISTER, 8.0F, 15, false),
   CANISTER_25("ammo_canister_shot_25", 25, CannonAmmoType.Kind.CANISTER, 8.0F, 25, false);

   private final String id;
   private final int weight;
   private final CannonAmmoType.Kind kind;
   private final float damage;
   private final int pelletCount;
   private final boolean pelletsOnLaunch;

   private CannonAmmoType(String id, int weight, CannonAmmoType.Kind kind, float damage, int pelletCount, boolean pelletsOnLaunch) {
      this.id = id;
      this.weight = weight;
      this.kind = kind;
      this.damage = damage;
      this.pelletCount = pelletCount;
      this.pelletsOnLaunch = pelletsOnLaunch;
   }

   public String id() {
      return this.id;
   }

   public int weight() {
      return this.weight;
   }

   public CannonAmmoType.Kind kind() {
      return this.kind;
   }

   public float damage() {
      return this.damage;
   }

   public int pelletCount() {
      return this.pelletCount;
   }

   public boolean pelletsOnLaunch() {
      return this.pelletsOnLaunch;
   }

   public boolean pelletsOnImpact() {
      return this.kind == CannonAmmoType.Kind.CANISTER;
   }

   public boolean breaksBlocks() {
      return this.kind == CannonAmmoType.Kind.IRON;
   }

   public String effectTranslationKey() {
      return "tooltip.ancientwarfare.cannon_ammo." + switch (this.kind) {
         case IRON -> "iron";
         case GRAPE -> "grape";
         case CANISTER -> "canister";
      };
   }

   public static enum Kind {
      IRON,
      GRAPE,
      CANISTER;
   }
}
