package net.shadowmage.ancientwarfare.core.common.warehouse;

public enum WarehouseSortType {
   NAME,
   COUNT;

   public WarehouseSortType next() {
      return values()[(this.ordinal() + 1) % values().length];
   }
}
