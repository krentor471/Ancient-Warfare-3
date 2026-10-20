package net.shadowmage.ancientwarfare.core.tile;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.structure.TerritoryProtectionData;

public class ProtectionFlagBlockEntity extends BlockEntity {
   private static final String FLAG_NAME_TAG = "FlagName";
   private static final String LEGACY_NAME_TAG = "name";
   private static final String OWNER_TAG = "Owner";
   private static final String OWNER_NAME_TAG = "OwnerName";
   private static final String TOP_COLOR_TAG = "TopColor";
   private static final String BOTTOM_COLOR_TAG = "BottomColor";
   private static final String LEGACY_TOP_COLOR_TAG = "topColor";
   private static final String LEGACY_BOTTOM_COLOR_TAG = "bottomColor";
   private static final String CLAIMED_TAG = "Claimed";
   private static final String TERRITORY_MIN_TAG = "TerritoryMin";
   private static final String TERRITORY_MAX_TAG = "TerritoryMax";
   private String flagName = "generic";
   private UUID owner;
   private String ownerName = "";
   private int topColor = -1;
   private int bottomColor = -1;
   private boolean claimed;
   private BlockPos territoryMin;
   private BlockPos territoryMax;

   public ProtectionFlagBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.PROTECTION_FLAG), pos, state);
   }

   public Optional<UUID> owner() {
      return Optional.ofNullable(this.owner);
   }

   public String ownerName() {
      return this.ownerName.isBlank() ? "unknown" : this.ownerName;
   }

   public String flagName() {
      return this.flagName.isBlank() ? "generic" : this.flagName;
   }

   public AABB getRenderBoundingBox() {
      return new AABB(this.f_58858_, this.f_58858_.m_7918_(1, 3, 1));
   }

   public boolean claim(Player player) {
      if (this.owner == null && this.f_58857_ instanceof ServerLevel serverLevel) {
         if (this.hasTerritory()) {
            List<FactionNpcEntity> enemies = serverLevel.m_6443_(
               FactionNpcEntity.class, this.territoryBounds(), npc -> npc.m_6084_() && npc.isHostileTowards(player)
            );
            if (!enemies.isEmpty()) {
               for (LivingEntity enemy : enemies) {
                  enemy.m_7292_(new MobEffectInstance(MobEffects.f_19619_, 2400, 0, false, false, true));
               }

               player.m_5661_(Component.m_237110_("message.ancientwarfare.protection_flag.enemies", new Object[]{enemies.size()}), true);
               return false;
            }
         }

         this.setOwner(player);
         this.claimed = true;
         if (this.hasTerritory()) {
            TerritoryProtectionData.get(serverLevel).claim(this.f_58858_, this.owner, this.ownerName);
         }

         this.syncChanged();
         return true;
      } else {
         return false;
      }
   }

   public void configureTerritory(BlockPos min, BlockPos max) {
      this.territoryMin = min.m_7949_();
      this.territoryMax = max.m_7949_();
      this.claimed = false;
      this.owner = null;
      this.ownerName = "";
      if (this.f_58857_ instanceof ServerLevel serverLevel) {
         TerritoryProtectionData.get(serverLevel).register(this.f_58858_, this.territoryMin, this.territoryMax);
      }

      this.syncChanged();
   }

   public boolean hasTerritory() {
      return this.territoryMin != null && this.territoryMax != null;
   }

   public boolean isClaimed() {
      return this.claimed;
   }

   public Optional<GameProfile> ownerProfile() {
      return this.owner == null ? Optional.empty() : Optional.of(new GameProfile(this.owner, this.ownerName));
   }

   private AABB territoryBounds() {
      return new AABB(this.territoryMin, this.territoryMax.m_7918_(1, 1, 1));
   }

   public void setOwner(Player player) {
      this.owner = player.m_20148_();
      this.ownerName = player.m_36316_().getName();
      this.syncChanged();
   }

   public boolean shouldProtectAgainst(Player player) {
      return this.owner != null && !this.owner.equals(player.m_20148_());
   }

   public void loadFromItem(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      if (tag != null) {
         this.readFlagData(tag);
         this.syncChanged();
      }
   }

   public void loadFromOldTemplateData(String data) {
      String teData = extract(data, "teData:{", "}");
      if (!teData.isBlank()) {
         String name = extractString(teData, "name");
         if (!name.isBlank()) {
            this.flagName = name;
         }

         this.topColor = extractInt(teData, "topColor", this.topColor);
         this.bottomColor = extractInt(teData, "bottomColor", this.bottomColor);
         this.syncChanged();
      }
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      this.writeFlagData(tag);
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.readFlagData(tag);
   }

   public CompoundTag m_5995_() {
      CompoundTag tag = super.m_5995_();
      this.writeFlagData(tag);
      return tag;
   }

   public void handleUpdateTag(CompoundTag tag) {
      this.readFlagData(tag);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.m_195640_(this);
   }

   private void writeFlagData(CompoundTag tag) {
      tag.m_128359_("FlagName", this.flagName());
      tag.m_128405_("TopColor", this.topColor);
      tag.m_128405_("BottomColor", this.bottomColor);
      tag.m_128379_("Claimed", this.claimed);
      if (this.hasTerritory()) {
         tag.m_128356_("TerritoryMin", this.territoryMin.m_121878_());
         tag.m_128356_("TerritoryMax", this.territoryMax.m_121878_());
      }

      if (this.owner != null) {
         tag.m_128362_("Owner", this.owner);
         tag.m_128359_("OwnerName", this.ownerName);
      }
   }

   private void readFlagData(CompoundTag tag) {
      if (tag.m_128441_("FlagName")) {
         this.flagName = tag.m_128461_("FlagName");
      } else if (tag.m_128441_("name")) {
         this.flagName = tag.m_128461_("name");
      }

      if (tag.m_128441_("TopColor")) {
         this.topColor = tag.m_128451_("TopColor");
      } else if (tag.m_128441_("topColor")) {
         this.topColor = tag.m_128451_("topColor");
      }

      if (tag.m_128441_("BottomColor")) {
         this.bottomColor = tag.m_128451_("BottomColor");
      } else if (tag.m_128441_("bottomColor")) {
         this.bottomColor = tag.m_128451_("bottomColor");
      }

      this.claimed = tag.m_128471_("Claimed");
      this.territoryMin = tag.m_128441_("TerritoryMin") ? BlockPos.m_122022_(tag.m_128454_("TerritoryMin")) : null;
      this.territoryMax = tag.m_128441_("TerritoryMax") ? BlockPos.m_122022_(tag.m_128454_("TerritoryMax")) : null;
      if (tag.m_128403_("Owner")) {
         this.owner = tag.m_128342_("Owner");
         this.ownerName = tag.m_128461_("OwnerName");
      }
   }

   private void syncChanged() {
      this.m_6596_();
      if (this.f_58857_ != null) {
         this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 3);
      }
   }

   private static String extractString(String text, String key) {
      return extract(text, key + ":\"", "\"");
   }

   private static int extractInt(String text, String key, int fallback) {
      String value = extract(text, key + ":", ",");
      if (value.isBlank()) {
         value = extract(text, key + ":", "}");
      }

      if (value.isBlank()) {
         return fallback;
      } else {
         try {
            return Integer.parseInt(value.trim());
         } catch (NumberFormatException var5) {
            return fallback;
         }
      }
   }

   private static String extract(String text, String start, String end) {
      int startIndex = text.indexOf(start);
      if (startIndex < 0) {
         return "";
      } else {
         startIndex += start.length();
         int endIndex = text.indexOf(end, startIndex);
         return endIndex < 0 ? text.substring(startIndex) : text.substring(startIndex, endIndex);
      }
   }
}
