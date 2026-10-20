package net.shadowmage.ancientwarfare.core.structure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class AdvancedSpawnerWorldData extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_advanced_spawner_settings";
   private boolean creativePlayersActivateSpawners;

   public static AdvancedSpawnerWorldData get(ServerLevel level) {
      return (AdvancedSpawnerWorldData)level.m_7654_()
         .m_129783_()
         .m_8895_()
         .m_164861_(AdvancedSpawnerWorldData::load, AdvancedSpawnerWorldData::new, "ancientwarfare_advanced_spawner_settings");
   }

   public boolean creativePlayersActivateSpawners() {
      return this.creativePlayersActivateSpawners;
   }

   public void setCreativePlayersActivateSpawners(boolean enabled) {
      if (this.creativePlayersActivateSpawners != enabled) {
         this.creativePlayersActivateSpawners = enabled;
         this.m_77762_();
      }
   }

   public CompoundTag m_7176_(CompoundTag tag) {
      tag.m_128379_("CreativePlayersActivateSpawners", this.creativePlayersActivateSpawners);
      return tag;
   }

   private static AdvancedSpawnerWorldData load(CompoundTag tag) {
      AdvancedSpawnerWorldData data = new AdvancedSpawnerWorldData();
      data.creativePlayersActivateSpawners = tag.m_128471_("CreativePlayersActivateSpawners");
      return data;
   }
}
