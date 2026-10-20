package net.shadowmage.ancientwarfare.core.entity;

public final class VehicleDurability {
   private static final float[] CARGO_HEALTH_BY_MATERIAL = new float[]{100.0F, 200.0F, 400.0F, 800.0F, 1200.0F};
   private static final float[] CARGO_ARMOR_BY_MATERIAL = new float[]{1.0F, 4.0F, 8.0F, 12.0F, 20.0F};
   private static final float[] ATTACK_HEALTH_BY_MATERIAL = new float[]{100.0F, 150.0F, 200.0F, 300.0F, 500.0F};
   private static final float[] ATTACK_ARMOR_BY_MATERIAL = new float[]{1.0F, 2.0F, 4.0F, 6.0F, 12.0F};

   private VehicleDurability() {
   }

   public static float cargoHealth(int level) {
      return CARGO_HEALTH_BY_MATERIAL[clamp(level, CARGO_HEALTH_BY_MATERIAL.length)];
   }

   public static float cargoArmor(int level) {
      return CARGO_ARMOR_BY_MATERIAL[clamp(level, CARGO_ARMOR_BY_MATERIAL.length)];
   }

   public static float attackHealth(int level) {
      return ATTACK_HEALTH_BY_MATERIAL[clamp(level, ATTACK_HEALTH_BY_MATERIAL.length)];
   }

   public static float attackArmor(int level) {
      return ATTACK_ARMOR_BY_MATERIAL[clamp(level, ATTACK_ARMOR_BY_MATERIAL.length)];
   }

   private static int clamp(int level, int length) {
      return Math.max(0, Math.min(length - 1, level));
   }
}
