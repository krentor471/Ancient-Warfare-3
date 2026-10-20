package net.shadowmage.ancientwarfare.core.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcDefaultsRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;
import net.shadowmage.ancientwarfare.core.common.npc.NpcEquipmentResolver;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireDragonCompat;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.DragonRiderController;
import net.shadowmage.ancientwarfare.core.entity.FactionArcherEliteNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionArcherNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionBardNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionCavalryNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionCivilianFemaleNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionCivilianMaleNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionLeaderEliteNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionLeaderNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionMountedArcherNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionPriestNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionSiegeEngineerNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionSoldierEliteNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionSoldierNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionSpellcasterNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionTraderNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.PlayerOwnedNpcEntity;

public class NpcSpawnerItem extends Item {
   private static final Map<String, Function<Level, ? extends PlayerOwnedNpcEntity>> EXTERNAL_FACTORIES = new ConcurrentHashMap<>();
   private static final Map<String, Float> EXTERNAL_ICON_VALUES = new ConcurrentHashMap<>();
   private static final String NPC_TYPE_TAG = "npcType";
   private static final String NPC_DATA_TAG = "npcData";
   private static final String FACTION_TAG = "faction";
   private static final String FACTION_ROLE_TAG = "factionRole";
   private static final String MOUNT_DATA_TAG = "mountData";
   private static final String MOUNT_TYPE_TAG = "mountType";
   private static final String MOUNT_LOGICAL_TAG = "logicalMount";
   public static final String FACTION_TYPE = "faction";

   public NpcSpawnerItem() {
      super(new Properties().m_41487_(16));
   }

   public static void registerExternalNpc(String id, Function<Level, ? extends PlayerOwnedNpcEntity> factory, float iconValue) {
      if (id != null && !id.isBlank() && factory != null) {
         EXTERNAL_FACTORIES.put(id, factory);
         EXTERNAL_ICON_VALUES.put(id, iconValue);
      }
   }

   public static float getIconValue(String npcType) {
      Map var10000 = EXTERNAL_ICON_VALUES;

      return var10000.getOrDefault(npcType, switch (npcType) {
         case "combat" -> 0.1F;
         case "courier" -> 0.2F;
         case "trader" -> 0.3F;
         case "bard" -> 0.4F;
         case "priest" -> 0.5F;
         case "siege_engineer" -> 1.2F;
         case "modern_soldier" -> 1.3F;
         case "ship_captain" -> 1.4F;
         default -> 0.0F;
      });
   }

   public static ItemStack create(String npcType, String ignoredNpcSubtype) {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      CompoundTag tag = stack.m_41784_();
      tag.m_128359_("npcType", npcType);
      return stack;
   }

   public static ItemStack createWorker() {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      stack.m_41784_().m_128359_("npcType", "worker");
      return stack;
   }

   public static ItemStack createCombat() {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      stack.m_41784_().m_128359_("npcType", "combat");
      return stack;
   }

   public static ItemStack createCourier() {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      stack.m_41784_().m_128359_("npcType", "courier");
      return stack;
   }

   public static ItemStack createTrader() {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      stack.m_41784_().m_128359_("npcType", "trader");
      return stack;
   }

   public static ItemStack createBard() {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      stack.m_41784_().m_128359_("npcType", "bard");
      return stack;
   }

   public static ItemStack createPriest() {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      stack.m_41784_().m_128359_("npcType", "priest");
      return stack;
   }

   public static ItemStack createSiegeEngineer() {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      stack.m_41784_().m_128359_("npcType", "siege_engineer");
      return stack;
   }

   public static ItemStack createModernSoldier() {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      stack.m_41784_().m_128359_("npcType", "modern_soldier");
      return stack;
   }

   public static ItemStack createShipCaptain() {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      stack.m_41784_().m_128359_("npcType", "ship_captain");
      return stack;
   }

   public static ItemStack createFaction(String faction, FactionNpcRole role) {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      CompoundTag tag = stack.m_41784_();
      tag.m_128359_("npcType", "faction");
      tag.m_128359_("faction", faction);
      tag.m_128359_("factionRole", role.id());
      return stack;
   }

   public InteractionResult m_6225_(UseOnContext context) {
      Level level = context.m_43725_();
      if (level.f_46443_) {
         return InteractionResult.SUCCESS;
      } else {
         ItemStack stack = context.m_43722_();
         String npcType = getNpcType(stack);
         Mob npc = createNpcFromStack(level, stack);
         if (npc == null) {
            return InteractionResult.FAIL;
         } else {
            BlockPos clickedPos = context.m_8083_();
            BlockPos spawnPos = clickedPos.m_121945_(context.m_43719_());
            Player player = context.m_43723_();
            npc.m_7678_((double)spawnPos.m_123341_() + 0.5, (double)spawnPos.m_123342_(), (double)spawnPos.m_123343_() + 0.5, context.m_7074_(), 0.0F);
            if (npc instanceof PlayerOwnedNpcEntity ownedNpc) {
               ownedNpc.setNpcType(npcType);
               if (player != null && !hasStoredNpcData(stack)) {
                  ownedNpc.setOwner(player.m_20148_());
               }
            }

            level.m_7967_(npc);
            if (hasStoredMountData(stack)) {
               restoreStoredMount(level, npc, stack);
            } else if (npc instanceof FactionNpcEntity factionNpc) {
               spawnMount(level, factionNpc);
            }

            if (player == null || !player.m_150110_().f_35937_) {
               stack.m_41774_(1);
            }

            return InteractionResult.SUCCESS;
         }
      }
   }

   private static PlayerOwnedNpcEntity createNpc(Level level, String npcType) {
      Function<Level, ? extends PlayerOwnedNpcEntity> factory = EXTERNAL_FACTORIES.get(npcType);
      if (factory != null) {
         PlayerOwnedNpcEntity extended = factory.apply(level);
         if (extended != null) {
            return extended;
         }
      }

      if ("worker".equals(npcType)) {
         return (PlayerOwnedNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.WORKER_NPC).m_20615_(level);
      } else if ("combat".equals(npcType)) {
         return (PlayerOwnedNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.COMBAT_NPC).m_20615_(level);
      } else if ("courier".equals(npcType)) {
         return (PlayerOwnedNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.COURIER_NPC).m_20615_(level);
      } else if ("trader".equals(npcType)) {
         return (PlayerOwnedNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.TRADER_NPC).m_20615_(level);
      } else if ("bard".equals(npcType)) {
         return (PlayerOwnedNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.BARD_NPC).m_20615_(level);
      } else if ("priest".equals(npcType)) {
         return (PlayerOwnedNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.PRIEST_NPC).m_20615_(level);
      } else if ("siege_engineer".equals(npcType)) {
         return (PlayerOwnedNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.SIEGE_ENGINEER_NPC).m_20615_(level);
      } else if ("modern_soldier".equals(npcType)) {
         return (PlayerOwnedNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.MODERN_SOLDIER_NPC).m_20615_(level);
      } else {
         return "ship_captain".equals(npcType) ? (PlayerOwnedNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.SHIP_CAPTAIN_NPC).m_20615_(level) : null;
      }
   }

   private static FactionNpcEntity createFactionNpc(Level level, FactionNpcRole role) {
      return (FactionNpcEntity)(switch (role) {
         case SOLDIER -> (FactionSoldierNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_SOLDIER_NPC).m_20615_(level);
         case SOLDIER_ELITE -> (FactionSoldierEliteNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_SOLDIER_ELITE_NPC).m_20615_(level);
         case ARCHER -> (FactionArcherNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_ARCHER_NPC).m_20615_(level);
         case ARCHER_ELITE -> (FactionArcherEliteNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_ARCHER_ELITE_NPC).m_20615_(level);
         case CAVALRY -> (FactionCavalryNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_CAVALRY_NPC).m_20615_(level);
         case MOUNTED_ARCHER -> (FactionMountedArcherNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_MOUNTED_ARCHER_NPC).m_20615_(level);
         case LEADER -> (FactionLeaderNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_LEADER_NPC).m_20615_(level);
         case LEADER_ELITE -> (FactionLeaderEliteNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_LEADER_ELITE_NPC).m_20615_(level);
         case SIEGE_ENGINEER -> (FactionSiegeEngineerNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_SIEGE_ENGINEER_NPC).m_20615_(level);
         case CIVILIAN_MALE -> (FactionCivilianMaleNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_CIVILIAN_MALE_NPC).m_20615_(level);
         case CIVILIAN_FEMALE -> (FactionCivilianFemaleNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_CIVILIAN_FEMALE_NPC).m_20615_(level);
         case PRIEST -> (FactionPriestNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_PRIEST_NPC).m_20615_(level);
         case SPELLCASTER -> (FactionSpellcasterNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_SPELLCASTER_NPC).m_20615_(level);
         case BARD -> (FactionBardNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_BARD_NPC).m_20615_(level);
         case TRADER -> (FactionTraderNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_TRADER_NPC).m_20615_(level);
      });
   }

   public Component m_7626_(ItemStack stack) {
      Component storedName = getStoredNpcDisplayName(stack);
      if (storedName != null) {
         return storedName;
      } else {
         return isFactionSpawner(stack)
            ? Component.m_237110_("item.ancientwarfare.npc_spawner.faction", new Object[]{getFaction(stack), getTranslatedFactionRole(stack)})
            : Component.m_237115_("item.ancientwarfare.npc_spawner." + getNpcType(stack));
      }
   }

   @Nullable
   public static Component getStoredNpcDisplayName(ItemStack stack) {
      CompoundTag root = stack.m_41783_();
      if (root != null && root.m_128425_("npcData", 10)) {
         CompoundTag npcData = root.m_128469_("npcData");
         if ("combat".equals(npcData.m_128461_("NpcType"))) {
            String mountType = storedMountType(stack);
            if (isDragonMountType(mountType)) {
               return Component.m_237115_("entity.ancientwarfare.combat.dragon_rider");
            }

            if (isHippocampusMountType(mountType)) {
               return Component.m_237115_("entity.ancientwarfare.combat.hippocampus_rider");
            }
         }

         String npcType = npcData.m_128461_("NpcType");
         String subtype = npcData.m_128461_("NpcSubtype");
         if ("modern_soldier".equals(npcType) || "ship_captain".equals(npcType)) {
            return Component.m_237115_("entity.ancientwarfare." + npcType);
         } else {
            return !npcType.isBlank() && !subtype.isBlank() ? Component.m_237115_("entity.ancientwarfare." + npcType + "." + subtype) : null;
         }
      } else {
         return null;
      }
   }

   public static float getWeaponIconValue(ItemStack stack) {
      CompoundTag root = stack.m_41783_();
      if (root != null && root.m_128425_("npcData", 10)) {
         CompoundTag npcData = root.m_128469_("npcData");
         if (!"combat".equals(npcData.m_128461_("NpcType"))) {
            return 0.0F;
         } else {
            ListTag hands = npcData.m_128437_("HandItems", 10);
            if (hands.isEmpty()) {
               return 0.0F;
            } else {
               return NpcEquipmentResolver.isRangedWeapon(ItemStack.m_41712_(hands.m_128728_(0))) ? 1.0F : 0.0F;
            }
         }
      } else {
         return 0.0F;
      }
   }

   private static String storedMountType(ItemStack stack) {
      CompoundTag root = stack.m_41783_();
      return root != null && root.m_128425_("mountData", 10) ? root.m_128469_("mountData").m_128461_("mountType").toLowerCase(Locale.ROOT) : "";
   }

   private static boolean isDragonMountType(String type) {
      return type.contains(":dragon_") || type.endsWith(":dragon") || type.contains("iceandfire:dragon");
   }

   private static boolean isHippocampusMountType(String type) {
      return type.contains("hippocampus");
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.m_237110_("tooltip.ancientwarfare.npc_spawner.type", new Object[]{getNpcType(stack)}).m_130940_(ChatFormatting.GRAY));
      if (isFactionSpawner(stack)) {
         tooltip.add(Component.m_237110_("tooltip.ancientwarfare.npc_spawner.faction", new Object[]{getFaction(stack)}).m_130940_(ChatFormatting.GRAY));
         tooltip.add(
            Component.m_237110_("tooltip.ancientwarfare.npc_spawner.role", new Object[]{getTranslatedFactionRole(stack)}).m_130940_(ChatFormatting.GRAY)
         );
      }

      tooltip.add(Component.m_237115_("tooltip.ancientwarfare.npc_spawner.place").m_130940_(ChatFormatting.DARK_GRAY));
      if ("combat".equals(getNpcType(stack))) {
         tooltip.add(Component.m_237115_("tooltip.ancientwarfare.npc_spawner.combat_weapon").m_130940_(ChatFormatting.DARK_GRAY));
      } else if ("courier".equals(getNpcType(stack))) {
         tooltip.add(Component.m_237115_("tooltip.ancientwarfare.npc_spawner.courier_order").m_130940_(ChatFormatting.DARK_GRAY));
      } else if ("trader".equals(getNpcType(stack))) {
         tooltip.add(Component.m_237115_("tooltip.ancientwarfare.npc_spawner.trader_order").m_130940_(ChatFormatting.DARK_GRAY));
      } else if ("bard".equals(getNpcType(stack))) {
         tooltip.add(Component.m_237115_("tooltip.ancientwarfare.npc_spawner.bard_instrument").m_130940_(ChatFormatting.DARK_GRAY));
      } else if ("priest".equals(getNpcType(stack))) {
         tooltip.add(Component.m_237115_("tooltip.ancientwarfare.npc_spawner.priest").m_130940_(ChatFormatting.DARK_GRAY));
      } else if ("siege_engineer".equals(getNpcType(stack))) {
         tooltip.add(Component.m_237115_("tooltip.ancientwarfare.npc_spawner.siege_engineer").m_130940_(ChatFormatting.DARK_GRAY));
      } else if ("modern_soldier".equals(getNpcType(stack))) {
         tooltip.add(Component.m_237115_("tooltip.ancientwarfare.npc_spawner.modern_soldier").m_130940_(ChatFormatting.DARK_GRAY));
      } else if ("ship_captain".equals(getNpcType(stack))) {
         tooltip.add(Component.m_237115_("tooltip.ancientwarfare.npc_spawner.ship_captain").m_130940_(ChatFormatting.DARK_GRAY));
      } else {
         tooltip.add(Component.m_237115_("tooltip.ancientwarfare.npc_spawner.worker_tool").m_130940_(ChatFormatting.DARK_GRAY));
      }
   }

   private static Component getTranslatedFactionRole(ItemStack stack) {
      return Component.m_237115_("entity.ancientwarfare.faction." + getFactionRole(stack));
   }

   public static String getNpcType(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      return tag != null && tag.m_128441_("npcType") ? tag.m_128461_("npcType") : "worker";
   }

   public static boolean isFactionSpawner(ItemStack stack) {
      return "faction".equals(getNpcType(stack));
   }

   public static String getFaction(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      return tag != null && tag.m_128441_("faction") ? tag.m_128461_("faction") : "brigand";
   }

   public static String getFactionRole(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      return tag != null && tag.m_128441_("factionRole") ? tag.m_128461_("factionRole") : FactionNpcRole.SOLDIER.id();
   }

   public static float getFactionRoleIconValue(ItemStack stack) {
      return FactionNpcRole.byId(getFactionRole(stack)).map(role -> {
         return switch (role) {
            case SOLDIER, SOLDIER_ELITE -> 0.6F;
            case ARCHER, ARCHER_ELITE -> 0.7F;
            case CAVALRY -> 0.6F;
            case MOUNTED_ARCHER -> 0.7F;
            case LEADER, LEADER_ELITE -> 0.8F;
            case SIEGE_ENGINEER -> 1.2F;
            case CIVILIAN_MALE, CIVILIAN_FEMALE -> 0.9F;
            case PRIEST, SPELLCASTER -> 1.0F;
            case BARD -> 1.1F;
            case TRADER -> 0.3F;
         };
      }).orElse(0.6F);
   }

   public static ItemStack createFromNpc(PlayerOwnedNpcEntity npc) {
      ItemStack stack = create(npc.getNpcType(), npc.getNpcSubtype());
      CompoundTag npcData = new CompoundTag();
      npc.m_20240_(npcData);
      npcData.m_128359_("id", BuiltInRegistries.f_256780_.m_7981_(npc.m_6095_()).toString());
      sanitizeStoredNpcData(npcData);
      stack.m_41784_().m_128365_("npcData", npcData);
      if (DragonRiderController.getLogicalMount(npc) instanceof Mob mount) {
         CompoundTag mountData = new CompoundTag();
         mount.m_20240_(mountData);
         sanitizeStoredMountData(mountData);
         mountData.m_128359_("mountType", BuiltInRegistries.f_256780_.m_7981_(mount.m_6095_()).toString());
         mountData.m_128379_("logicalMount", true);
         stack.m_41784_().m_128365_("mountData", mountData);
      } else if (npc.m_20202_() instanceof Mob mount) {
         CompoundTag mountData = new CompoundTag();
         mount.m_20240_(mountData);
         sanitizeStoredMountData(mountData);
         mountData.m_128359_("mountType", BuiltInRegistries.f_256780_.m_7981_(mount.m_6095_()).toString());
         stack.m_41784_().m_128365_("mountData", mountData);
      }

      return stack;
   }

   public static Mob createNpcFromStack(Level level, ItemStack stack) {
      if (isFactionSpawner(stack)) {
         FactionNpcRole role = FactionNpcRole.byId(getFactionRole(stack)).orElse(FactionNpcRole.SOLDIER);
         FactionNpcEntity npc = createFactionNpc(level, role);
         if (npc == null) {
            return null;
         } else {
            npc.setNpcSubtype(role.id());
            npc.setFactionName(getFaction(stack));
            return npc;
         }
      } else {
         PlayerOwnedNpcEntity npc = createStoredNpc(level, stack);
         if (npc == null) {
            npc = createNpc(level, getNpcType(stack));
         }

         if (npc == null) {
            return null;
         } else {
            loadStoredNpcData(npc, stack);
            npc.setNpcType(getNpcType(stack));
            return npc;
         }
      }
   }

   @Nullable
   private static PlayerOwnedNpcEntity createStoredNpc(Level level, ItemStack stack) {
      CompoundTag root = stack.m_41783_();
      if (root != null && root.m_128425_("npcData", 10)) {
         ResourceLocation id = ResourceLocation.m_135820_(root.m_128469_("npcData").m_128461_("id"));
         if (id == null) {
            return null;
         } else {
            Entity entity = BuiltInRegistries.f_256780_.m_6612_(id).map(type -> type.m_20615_(level)).orElse(null);
            if (entity instanceof PlayerOwnedNpcEntity npc && npc.getNpcType().equals(getNpcType(stack))) {
               return npc;
            }

            return null;
         }
      } else {
         return null;
      }
   }

   public static List<ItemStack> createFactionCreativeStacks() {
      List<ItemStack> stacks = new ArrayList<>();

      for (String faction : FactionNpcDefaultsRegistry.factionNames()) {
         for (FactionNpcRole role : FactionNpcRole.CREATIVE_ROLES) {
            if (FactionNpcDefaultsRegistry.isEnabled(faction, role)) {
               stacks.add(createFaction(faction, role));
            }
         }
      }

      return stacks;
   }

   private static void spawnMount(Level level, FactionNpcEntity npc) {
      FactionNpcRole role = FactionNpcRole.byId(npc.getNpcSubtype()).orElse(FactionNpcRole.SOLDIER);
      if (role == FactionNpcRole.CAVALRY || role == FactionNpcRole.MOUNTED_ARCHER) {
         Mob mount = (Mob)FactionNpcDefaultsRegistry.mountType(npc.getFactionName(), role).m_20615_(level);
         if (mount != null) {
            if (mount instanceof AbstractHorse horse) {
               horse.m_30651_(true);
               horse.m_30649_(100);
               horse.m_5853_(SoundSource.NEUTRAL);
               AttributeInstance speed = horse.m_21051_(Attributes.f_22279_);
               if (speed != null) {
                  speed.m_22100_(Math.max(speed.m_22115_(), 0.32));
               }
            }

            mount.m_21530_();
            mount.m_7678_(npc.m_20185_(), npc.m_20186_(), npc.m_20189_(), npc.m_146908_(), 0.0F);
            level.m_7967_(mount);
            npc.m_7998_(mount, true);
         }
      }
   }

   private static boolean hasStoredNpcData(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      return tag != null && tag.m_128425_("npcData", 10);
   }

   private static void loadStoredNpcData(PlayerOwnedNpcEntity npc, ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      if (tag != null && tag.m_128425_("npcData", 10)) {
         CompoundTag npcData = tag.m_128469_("npcData").m_6426_();
         sanitizeStoredNpcData(npcData);
         npc.m_20258_(npcData);
      }
   }

   private static boolean hasStoredMountData(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      return tag != null && tag.m_128425_("mountData", 10);
   }

   private static void restoreStoredMount(Level level, Mob npc, ItemStack stack) {
      CompoundTag root = stack.m_41783_();
      if (root != null && root.m_128425_("mountData", 10)) {
         CompoundTag mountData = root.m_128469_("mountData").m_6426_();
         String typeId = mountData.m_128461_("mountType");
         if (!typeId.isBlank()) {
            Entity mount = BuiltInRegistries.f_256780_.m_6612_(ResourceLocation.m_135820_(typeId)).map(type -> type.m_20615_(level)).orElse(null);
            if (mount instanceof Mob mobMount) {
               mountData.m_128473_("mountType");
               boolean logicalMount = mountData.m_128471_("logicalMount");
               mountData.m_128473_("logicalMount");
               mobMount.m_20258_(mountData);
               mobMount.m_21530_();
               mobMount.m_7678_(npc.m_20185_(), npc.m_20186_(), npc.m_20189_(), npc.m_146908_(), 0.0F);
               level.m_7967_(mobMount);
               if (logicalMount && npc instanceof CombatNpcEntity combatNpc && IceAndFireDragonCompat.isDragon(mobMount)) {
                  DragonRiderController.attach(combatNpc, mobMount);
                  return;
               }

               npc.m_7998_(mobMount, true);
            }
         }
      }
   }

   private static void sanitizeStoredMountData(CompoundTag mountData) {
      mountData.m_128473_("Pos");
      mountData.m_128473_("Motion");
      mountData.m_128473_("Rotation");
      mountData.m_128473_("UUID");
      mountData.m_128473_("Passengers");
      mountData.m_128473_("Leash");
   }

   private static void sanitizeStoredNpcData(CompoundTag npcData) {
      npcData.m_128473_("Pos");
      npcData.m_128473_("Motion");
      npcData.m_128473_("Rotation");
      npcData.m_128473_("UUID");
      npcData.m_128473_("Passengers");
      npcData.m_128473_("Leash");
   }
}
