package net.shadowmage.ancientwarfare.core.item;

public enum CatapultAmmoType {
   STONE_10("ammo_stone_shot_10", 10, CatapultAmmoType.Kind.STONE, 10.0F, false, false, false, 0),
   STONE_15("ammo_stone_shot_15", 15, CatapultAmmoType.Kind.STONE, 15.0F, false, false, false, 0),
   STONE_30("ammo_stone_shot_30", 30, CatapultAmmoType.Kind.STONE, 30.0F, false, false, false, 0),
   STONE_45("ammo_stone_shot_45", 45, CatapultAmmoType.Kind.STONE, 45.0F, false, false, false, 0),
   FLAME_10("ammo_flame_shot_10", 10, CatapultAmmoType.Kind.FLAME, 8.0F, true, false, false, 0),
   FLAME_15("ammo_flame_shot_15", 15, CatapultAmmoType.Kind.FLAME, 8.0F, true, false, false, 0),
   FLAME_30("ammo_flame_shot_30", 30, CatapultAmmoType.Kind.FLAME, 8.0F, true, false, false, 0),
   FLAME_45("ammo_flame_shot_45", 45, CatapultAmmoType.Kind.FLAME, 8.0F, true, false, false, 0),
   PEBBLE_10("ammo_pebble_shot_10", 10, CatapultAmmoType.Kind.PEBBLE, 0.0F, false, false, false, 10),
   PEBBLE_15("ammo_pebble_shot_15", 15, CatapultAmmoType.Kind.PEBBLE, 0.0F, false, false, false, 15),
   PEBBLE_30("ammo_pebble_shot_30", 30, CatapultAmmoType.Kind.PEBBLE, 0.0F, false, false, false, 30),
   PEBBLE_45("ammo_pebble_shot_45", 45, CatapultAmmoType.Kind.PEBBLE, 0.0F, false, false, false, 45),
   CLUSTER_10("ammo_cluster_shot_10", 10, CatapultAmmoType.Kind.CLUSTER, 5.0F, false, false, false, 10),
   CLUSTER_15("ammo_cluster_shot_15", 15, CatapultAmmoType.Kind.CLUSTER, 5.0F, false, false, false, 15),
   CLUSTER_30("ammo_cluster_shot_30", 30, CatapultAmmoType.Kind.CLUSTER, 5.0F, false, false, false, 30),
   CLUSTER_45("ammo_cluster_shot_45", 45, CatapultAmmoType.Kind.CLUSTER, 5.0F, false, false, false, 45),
   EXPLOSIVE_10("ammo_explosive_shot_10", 10, CatapultAmmoType.Kind.EXPLOSIVE, 10.0F, false, true, false, 0),
   EXPLOSIVE_15("ammo_explosive_shot_15", 15, CatapultAmmoType.Kind.EXPLOSIVE, 15.0F, false, true, false, 0),
   EXPLOSIVE_30("ammo_explosive_shot_30", 30, CatapultAmmoType.Kind.EXPLOSIVE, 30.0F, false, true, false, 0),
   EXPLOSIVE_45("ammo_explosive_shot_45", 45, CatapultAmmoType.Kind.EXPLOSIVE, 45.0F, false, true, false, 0),
   HIGH_EXPLOSIVE_10("ammo_explosive_shot_10_big", 10, CatapultAmmoType.Kind.HIGH_EXPLOSIVE, 10.0F, false, true, true, 0),
   HIGH_EXPLOSIVE_15("ammo_explosive_shot_15_big", 15, CatapultAmmoType.Kind.HIGH_EXPLOSIVE, 15.0F, false, true, true, 0),
   HIGH_EXPLOSIVE_30("ammo_explosive_shot_30_big", 30, CatapultAmmoType.Kind.HIGH_EXPLOSIVE, 30.0F, false, true, true, 0),
   HIGH_EXPLOSIVE_45("ammo_explosive_shot_45_big", 45, CatapultAmmoType.Kind.HIGH_EXPLOSIVE, 45.0F, false, true, true, 0),
   NAPALM_10("ammo_napalm_shot_10", 10, CatapultAmmoType.Kind.NAPALM, 10.0F, true, false, false, 0),
   NAPALM_15("ammo_napalm_shot_15", 15, CatapultAmmoType.Kind.NAPALM, 15.0F, true, false, false, 0),
   NAPALM_30("ammo_napalm_shot_30", 30, CatapultAmmoType.Kind.NAPALM, 30.0F, true, false, false, 0),
   NAPALM_45("ammo_napalm_shot_45", 45, CatapultAmmoType.Kind.NAPALM, 45.0F, true, false, false, 0);

   private final String id;
   private final int weight;
   private final CatapultAmmoType.Kind kind;
   private final float damage;
   private final boolean flaming;
   private final boolean explosive;
   private final boolean highExplosive;
   private final int submunitionCount;

   private CatapultAmmoType(
      String id, int weight, CatapultAmmoType.Kind kind, float damage, boolean flaming, boolean explosive, boolean highExplosive, int submunitionCount
   ) {
      this.id = id;
      this.weight = weight;
      this.kind = kind;
      this.damage = damage;
      this.flaming = flaming;
      this.explosive = explosive;
      this.highExplosive = highExplosive;
      this.submunitionCount = submunitionCount;
   }

   public String id() {
      return this.id;
   }

   public int weight() {
      return this.weight;
   }

   public CatapultAmmoType.Kind kind() {
      return this.kind;
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

   public boolean highExplosive() {
      return this.highExplosive;
   }

   public int submunitionCount() {
      return this.submunitionCount;
   }

   public boolean breaksBlocks() {
      return this.kind == CatapultAmmoType.Kind.STONE;
   }

   public boolean ignitesArea() {
      return this.kind == CatapultAmmoType.Kind.FLAME;
   }

   public boolean spreadsNapalm() {
      return this.kind == CatapultAmmoType.Kind.NAPALM;
   }

   public boolean spawnsSubmunitionsOnLaunch() {
      return this.kind == CatapultAmmoType.Kind.PEBBLE;
   }

   public boolean spawnsSubmunitionsOnImpact() {
      return this.kind == CatapultAmmoType.Kind.CLUSTER;
   }

   public float explosionPower() {
      if (!this.explosive) {
         return 0.0F;
      } else {
         float maxPower = this.highExplosive ? 7.0F : 3.5F;
         return maxPower * (float)this.weight / 45.0F;
      }
   }

   public int areaTier() {
      return this.weight >= 45 ? 4 : (this.weight >= 30 ? 3 : (this.weight >= 15 ? 2 : 1));
   }

   public String effectTranslationKey() {
      return "tooltip.ancientwarfare.catapult_ammo." + switch (this.kind) {
         case STONE -> "stone";
         case FLAME -> "flame";
         case PEBBLE -> "pebble";
         case CLUSTER -> "cluster";
         case EXPLOSIVE -> "explosive";
         case HIGH_EXPLOSIVE -> "high_explosive";
         case NAPALM -> "napalm";
      };
   }

   public static enum Kind {
      STONE,
      FLAME,
      PEBBLE,
      CLUSTER,
      EXPLOSIVE,
      HIGH_EXPLOSIVE,
      NAPALM;
   }
}
