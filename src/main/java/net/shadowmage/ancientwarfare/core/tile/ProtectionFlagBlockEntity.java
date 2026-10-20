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
      return new AABB(this.worldPosition, this.worldPosition.offset(1, 3, 1));
   }

   public boolean claim(Player player) {
      if (this.owner == null && this.level instanceof ServerLevel serverLevel) {
         if (this.hasTerritory()) {
            List<FactionNpcEntity> enemies = serverLevel.getEntitiesOfClass(
               FactionNpcEntity.class, this.territoryBounds(), npc -> npc.isAlive() && npc.isHostileTowards(player)
            );
            if (!enemies.isEmpty()) {
               for (LivingEntity enemy : enemies) {
                  enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 2400, 0, false, false, true));
               }

               player.displayClientMessage(Component.translatable("message.ancientwarfare.protection_flag.enemies", new Object[]{enemies.size()}), true);
               return false;
            }
         }

         this.setOwner(player);
         this.claimed = true;
         if (this.hasTerritory()) {
            TerritoryProtectionData.get(serverLevel).claim(this.worldPosition, this.owner, this.ownerName);
         }

         this.syncChanged();
         return true;
      } else {
         return false;
      }
   }

   public void configureTerritory(BlockPos min, BlockPos max) {
      this.territoryMin = min.immutable();
      this.territoryMax = max.immutable();
      this.claimed = false;
      this.owner = null;
      this.ownerName = "";
      if (this.level instanceof ServerLevel serverLevel) {
         TerritoryProtectionData.get(serverLevel).register(this.worldPosition, this.territoryMin, this.territoryMax);
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
      return new AABB(this.territoryMin, this.territoryMax.offset(1, 1, 1));
   }

   public void setOwner(Player player) {
      this.owner = player.getUUID();
      this.ownerName = player.getGameProfile().getName();
      this.syncChanged();
   }

   public boolean shouldProtectAgainst(Player player) {
      return this.owner != null && !this.owner.equals(player.getUUID());
   }

   public void loadFromItem(ItemStack stack) {
      CompoundTag tag = stack.getTag();
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

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      this.writeFlagData(tag);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.readFlagData(tag);
   }

   public CompoundTag getUpdateTag() {
      CompoundTag tag = super.getUpdateTag();
      this.writeFlagData(tag);
      return tag;
   }

   public void handleUpdateTag(CompoundTag tag) {
      this.readFlagData(tag);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   private void writeFlagData(CompoundTag tag) {
      tag.putString("FlagName", this.flagName());
      tag.putInt("TopColor", this.topColor);
      tag.putInt("BottomColor", this.bottomColor);
      tag.putBoolean("Claimed", this.claimed);
      if (this.hasTerritory()) {
         tag.putLong("TerritoryMin", this.territoryMin.asLong());
         tag.putLong("TerritoryMax", this.territoryMax.asLong());
      }

      if (this.owner != null) {
         tag.putUUID("Owner", this.owner);
         tag.putString("OwnerName", this.ownerName);
      }
   }

   private void readFlagData(CompoundTag tag) {
      if (tag.contains("FlagName")) {
         this.flagName = tag.getString("FlagName");
      } else if (tag.contains("name")) {
         this.flagName = tag.getString("name");
      }

      if (tag.contains("TopColor")) {
         this.topColor = tag.getInt("TopColor");
      } else if (tag.contains("topColor")) {
         this.topColor = tag.getInt("topColor");
      }

      if (tag.contains("BottomColor")) {
         this.bottomColor = tag.getInt("BottomColor");
      } else if (tag.contains("bottomColor")) {
         this.bottomColor = tag.getInt("bottomColor");
      }

      this.claimed = tag.getBoolean("Claimed");
      this.territoryMin = tag.contains("TerritoryMin") ? BlockPos.of(tag.getLong("TerritoryMin")) : null;
      this.territoryMax = tag.contains("TerritoryMax") ? BlockPos.of(tag.getLong("TerritoryMax")) : null;
      if (tag.hasUUID("Owner")) {
         this.owner = tag.getUUID("Owner");
         this.ownerName = tag.getString("OwnerName");
      }
   }

   private void syncChanged() {
      this.setChanged();
      if (this.level != null) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
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
