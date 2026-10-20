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
      super(new Properties().stacksTo(16));
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
      CompoundTag tag = stack.getOrCreateTag();
      tag.putString("npcType", npcType);
      return stack;
   }

   public static ItemStack createWorker() {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      stack.getOrCreateTag().putString("npcType", "worker");
      return stack;
   }

   public static ItemStack createCombat() {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      stack.getOrCreateTag().putString("npcType", "combat");
      return stack;
   }

   public static ItemStack createCourier() {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      stack.getOrCreateTag().putString("npcType", "courier");
      return stack;
   }

   public static ItemStack createTrader() {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      stack.getOrCreateTag().putString("npcType", "trader");
      return stack;
   }

   public static ItemStack createBard() {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      stack.getOrCreateTag().putString("npcType", "bard");
      return stack;
   }

   public static ItemStack createPriest() {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      stack.getOrCreateTag().putString("npcType", "priest");
      return stack;
   }

   public static ItemStack createSiegeEngineer() {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      stack.getOrCreateTag().putString("npcType", "siege_engineer");
      return stack;
   }

   public static ItemStack createModernSoldier() {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      stack.getOrCreateTag().putString("npcType", "modern_soldier");
      return stack;
   }

   public static ItemStack createShipCaptain() {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      stack.getOrCreateTag().putString("npcType", "ship_captain");
      return stack;
   }

   public static ItemStack createFaction(String faction, FactionNpcRole role) {
      ItemStack stack = AWCoreRegistryAccess.stack(AWCoreIds.NPC_SPAWNER);
      CompoundTag tag = stack.getOrCreateTag();
      tag.putString("npcType", "faction");
      tag.putString("faction", faction);
      tag.putString("factionRole", role.id());
      return stack;
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      if (level.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         ItemStack stack = context.getItemInHand();
         String npcType = getNpcType(stack);
         Mob npc = createNpcFromStack(level, stack);
         if (npc == null) {
            return InteractionResult.FAIL;
         } else {
            BlockPos clickedPos = context.getClickedPos();
            BlockPos spawnPos = clickedPos.relative(context.getClickedFace());
            Player player = context.getPlayer();
            npc.moveTo((double)spawnPos.getX() + 0.5, (double)spawnPos.getY(), (double)spawnPos.getZ() + 0.5, context.getRotation(), 0.0F);
            if (npc instanceof PlayerOwnedNpcEntity ownedNpc) {
               ownedNpc.setNpcType(npcType);
               if (player != null && !hasStoredNpcData(stack)) {
                  ownedNpc.setOwner(player.getUUID());
               }
            }

            level.addFreshEntity(npc);
            if (hasStoredMountData(stack)) {
               restoreStoredMount(level, npc, stack);
            } else if (npc instanceof FactionNpcEntity factionNpc) {
               spawnMount(level, factionNpc);
            }

            if (player == null || !player.getAbilities().instabuild) {
               stack.shrink(1);
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
         return (PlayerOwnedNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.WORKER_NPC).create(level);
      } else if ("combat".equals(npcType)) {
         return (PlayerOwnedNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.COMBAT_NPC).create(level);
      } else if ("courier".equals(npcType)) {
         return (PlayerOwnedNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.COURIER_NPC).create(level);
      } else if ("trader".equals(npcType)) {
         return (PlayerOwnedNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.TRADER_NPC).create(level);
      } else if ("bard".equals(npcType)) {
         return (PlayerOwnedNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.BARD_NPC).create(level);
      } else if ("priest".equals(npcType)) {
         return (PlayerOwnedNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.PRIEST_NPC).create(level);
      } else if ("siege_engineer".equals(npcType)) {
         return (PlayerOwnedNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.SIEGE_ENGINEER_NPC).create(level);
      } else if ("modern_soldier".equals(npcType)) {
         return (PlayerOwnedNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.MODERN_SOLDIER_NPC).create(level);
      } else {
         return "ship_captain".equals(npcType) ? (PlayerOwnedNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.SHIP_CAPTAIN_NPC).create(level) : null;
      }
   }

   private static FactionNpcEntity createFactionNpc(Level level, FactionNpcRole role) {
      return (FactionNpcEntity)(switch (role) {
         case SOLDIER -> (FactionSoldierNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_SOLDIER_NPC).create(level);
         case SOLDIER_ELITE -> (FactionSoldierEliteNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_SOLDIER_ELITE_NPC).create(level);
         case ARCHER -> (FactionArcherNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_ARCHER_NPC).create(level);
         case ARCHER_ELITE -> (FactionArcherEliteNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_ARCHER_ELITE_NPC).create(level);
         case CAVALRY -> (FactionCavalryNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_CAVALRY_NPC).create(level);
         case MOUNTED_ARCHER -> (FactionMountedArcherNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_MOUNTED_ARCHER_NPC).create(level);
         case LEADER -> (FactionLeaderNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_LEADER_NPC).create(level);
         case LEADER_ELITE -> (FactionLeaderEliteNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_LEADER_ELITE_NPC).create(level);
         case SIEGE_ENGINEER -> (FactionSiegeEngineerNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_SIEGE_ENGINEER_NPC).create(level);
         case CIVILIAN_MALE -> (FactionCivilianMaleNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_CIVILIAN_MALE_NPC).create(level);
         case CIVILIAN_FEMALE -> (FactionCivilianFemaleNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_CIVILIAN_FEMALE_NPC).create(level);
         case PRIEST -> (FactionPriestNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_PRIEST_NPC).create(level);
         case SPELLCASTER -> (FactionSpellcasterNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_SPELLCASTER_NPC).create(level);
         case BARD -> (FactionBardNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_BARD_NPC).create(level);
         case TRADER -> (FactionTraderNpcEntity)AWCoreRegistryAccess.entityType(AWCoreIds.FACTION_TRADER_NPC).create(level);
      });
   }

   public Component getName(ItemStack stack) {
      Component storedName = getStoredNpcDisplayName(stack);
      if (storedName != null) {
         return storedName;
      } else {
         return isFactionSpawner(stack)
            ? Component.translatable("item.ancientwarfare.npc_spawner.faction", new Object[]{getFaction(stack), getTranslatedFactionRole(stack)})
            : Component.translatable("item.ancientwarfare.npc_spawner." + getNpcType(stack));
      }
   }

   @Nullable
   public static Component getStoredNpcDisplayName(ItemStack stack) {
      CompoundTag root = stack.getTag();
      if (root != null && root.contains("npcData", 10)) {
         CompoundTag npcData = root.getCompound("npcData");
         if ("combat".equals(npcData.getString("NpcType"))) {
            String mountType = storedMountType(stack);
            if (isDragonMountType(mountType)) {
               return Component.translatable("entity.ancientwarfare.combat.dragon_rider");
            }

            if (isHippocampusMountType(mountType)) {
               return Component.translatable("entity.ancientwarfare.combat.hippocampus_rider");
            }
         }

         String npcType = npcData.getString("NpcType");
         String subtype = npcData.getString("NpcSubtype");
         if ("modern_soldier".equals(npcType) || "ship_captain".equals(npcType)) {
            return Component.translatable("entity.ancientwarfare." + npcType);
         } else {
            return !npcType.isBlank() && !subtype.isBlank() ? Component.translatable("entity.ancientwarfare." + npcType + "." + subtype) : null;
         }
      } else {
         return null;
      }
   }

   public static float getWeaponIconValue(ItemStack stack) {
      CompoundTag root = stack.getTag();
      if (root != null && root.contains("npcData", 10)) {
         CompoundTag npcData = root.getCompound("npcData");
         if (!"combat".equals(npcData.getString("NpcType"))) {
            return 0.0F;
         } else {
            ListTag hands = npcData.getList("HandItems", 10);
            if (hands.isEmpty()) {
               return 0.0F;
            } else {
               return NpcEquipmentResolver.isRangedWeapon(ItemStack.of(hands.getCompound(0))) ? 1.0F : 0.0F;
            }
         }
      } else {
         return 0.0F;
      }
   }

   private static String storedMountType(ItemStack stack) {
      CompoundTag root = stack.getTag();
      return root != null && root.contains("mountData", 10) ? root.getCompound("mountData").getString("mountType").toLowerCase(Locale.ROOT) : "";
   }

   private static boolean isDragonMountType(String type) {
      return type.contains(":dragon_") || type.endsWith(":dragon") || type.contains("iceandfire:dragon");
   }

   private static boolean isHippocampusMountType(String type) {
      return type.contains("hippocampus");
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.ancientwarfare.npc_spawner.type", new Object[]{getNpcType(stack)}).withStyle(ChatFormatting.GRAY));
      if (isFactionSpawner(stack)) {
         tooltip.add(Component.translatable("tooltip.ancientwarfare.npc_spawner.faction", new Object[]{getFaction(stack)}).withStyle(ChatFormatting.GRAY));
         tooltip.add(
            Component.translatable("tooltip.ancientwarfare.npc_spawner.role", new Object[]{getTranslatedFactionRole(stack)}).withStyle(ChatFormatting.GRAY)
         );
      }

      tooltip.add(Component.translatable("tooltip.ancientwarfare.npc_spawner.place").withStyle(ChatFormatting.DARK_GRAY));
      if ("combat".equals(getNpcType(stack))) {
         tooltip.add(Component.translatable("tooltip.ancientwarfare.npc_spawner.combat_weapon").withStyle(ChatFormatting.DARK_GRAY));
      } else if ("courier".equals(getNpcType(stack))) {
         tooltip.add(Component.translatable("tooltip.ancientwarfare.npc_spawner.courier_order").withStyle(ChatFormatting.DARK_GRAY));
      } else if ("trader".equals(getNpcType(stack))) {
         tooltip.add(Component.translatable("tooltip.ancientwarfare.npc_spawner.trader_order").withStyle(ChatFormatting.DARK_GRAY));
      } else if ("bard".equals(getNpcType(stack))) {
         tooltip.add(Component.translatable("tooltip.ancientwarfare.npc_spawner.bard_instrument").withStyle(ChatFormatting.DARK_GRAY));
      } else if ("priest".equals(getNpcType(stack))) {
         tooltip.add(Component.translatable("tooltip.ancientwarfare.npc_spawner.priest").withStyle(ChatFormatting.DARK_GRAY));
      } else if ("siege_engineer".equals(getNpcType(stack))) {
         tooltip.add(Component.translatable("tooltip.ancientwarfare.npc_spawner.siege_engineer").withStyle(ChatFormatting.DARK_GRAY));
      } else if ("modern_soldier".equals(getNpcType(stack))) {
         tooltip.add(Component.translatable("tooltip.ancientwarfare.npc_spawner.modern_soldier").withStyle(ChatFormatting.DARK_GRAY));
      } else if ("ship_captain".equals(getNpcType(stack))) {
         tooltip.add(Component.translatable("tooltip.ancientwarfare.npc_spawner.ship_captain").withStyle(ChatFormatting.DARK_GRAY));
      } else {
         tooltip.add(Component.translatable("tooltip.ancientwarfare.npc_spawner.worker_tool").withStyle(ChatFormatting.DARK_GRAY));
      }
   }

   private static Component getTranslatedFactionRole(ItemStack stack) {
      return Component.translatable("entity.ancientwarfare.faction." + getFactionRole(stack));
   }

   public static String getNpcType(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag != null && tag.contains("npcType") ? tag.getString("npcType") : "worker";
   }

   public static boolean isFactionSpawner(ItemStack stack) {
      return "faction".equals(getNpcType(stack));
   }

   public static String getFaction(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag != null && tag.contains("faction") ? tag.getString("faction") : "brigand";
   }

   public static String getFactionRole(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag != null && tag.contains("factionRole") ? tag.getString("factionRole") : FactionNpcRole.SOLDIER.id();
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
      npc.saveWithoutId(npcData);
      npcData.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(npc.getType()).toString());
      sanitizeStoredNpcData(npcData);
      stack.getOrCreateTag().put("npcData", npcData);
      if (DragonRiderController.getLogicalMount(npc) instanceof Mob mount) {
         CompoundTag mountData = new CompoundTag();
         mount.saveWithoutId(mountData);
         sanitizeStoredMountData(mountData);
         mountData.putString("mountType", BuiltInRegistries.ENTITY_TYPE.getKey(mount.getType()).toString());
         mountData.putBoolean("logicalMount", true);
         stack.getOrCreateTag().put("mountData", mountData);
      } else if (npc.getVehicle() instanceof Mob mount) {
         CompoundTag mountData = new CompoundTag();
         mount.saveWithoutId(mountData);
         sanitizeStoredMountData(mountData);
         mountData.putString("mountType", BuiltInRegistries.ENTITY_TYPE.getKey(mount.getType()).toString());
         stack.getOrCreateTag().put("mountData", mountData);
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
      CompoundTag root = stack.getTag();
      if (root != null && root.contains("npcData", 10)) {
         ResourceLocation id = ResourceLocation.tryParse(root.getCompound("npcData").getString("id"));
         if (id == null) {
            return null;
         } else {
            Entity entity = BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(type -> type.create(level)).orElse(null);
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
         Mob mount = (Mob)FactionNpcDefaultsRegistry.mountType(npc.getFactionName(), role).create(level);
         if (mount != null) {
            if (mount instanceof AbstractHorse horse) {
               horse.setTamed(true);
               horse.setTemper(100);
               horse.equipSaddle(SoundSource.NEUTRAL);
               AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
               if (speed != null) {
                  speed.setBaseValue(Math.max(speed.getBaseValue(), 0.32));
               }
            }

            mount.setPersistenceRequired();
            mount.moveTo(npc.getX(), npc.getY(), npc.getZ(), npc.getYRot(), 0.0F);
            level.addFreshEntity(mount);
            npc.startRiding(mount, true);
         }
      }
   }

   private static boolean hasStoredNpcData(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag != null && tag.contains("npcData", 10);
   }

   private static void loadStoredNpcData(PlayerOwnedNpcEntity npc, ItemStack stack) {
      CompoundTag tag = stack.getTag();
      if (tag != null && tag.contains("npcData", 10)) {
         CompoundTag npcData = tag.getCompound("npcData").copy();
         sanitizeStoredNpcData(npcData);
         npc.load(npcData);
      }
   }

   private static boolean hasStoredMountData(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag != null && tag.contains("mountData", 10);
   }

   private static void restoreStoredMount(Level level, Mob npc, ItemStack stack) {
      CompoundTag root = stack.getTag();
      if (root != null && root.contains("mountData", 10)) {
         CompoundTag mountData = root.getCompound("mountData").copy();
         String typeId = mountData.getString("mountType");
         if (!typeId.isBlank()) {
            Entity mount = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.tryParse(typeId)).map(type -> type.create(level)).orElse(null);
            if (mount instanceof Mob mobMount) {
               mountData.remove("mountType");
               boolean logicalMount = mountData.getBoolean("logicalMount");
               mountData.remove("logicalMount");
               mobMount.load(mountData);
               mobMount.setPersistenceRequired();
               mobMount.moveTo(npc.getX(), npc.getY(), npc.getZ(), npc.getYRot(), 0.0F);
               level.addFreshEntity(mobMount);
               if (logicalMount && npc instanceof CombatNpcEntity combatNpc && IceAndFireDragonCompat.isDragon(mobMount)) {
                  DragonRiderController.attach(combatNpc, mobMount);
                  return;
               }

               npc.startRiding(mobMount, true);
            }
         }
      }
   }

   private static void sanitizeStoredMountData(CompoundTag mountData) {
      mountData.remove("Pos");
      mountData.remove("Motion");
      mountData.remove("Rotation");
      mountData.remove("UUID");
      mountData.remove("Passengers");
      mountData.remove("Leash");
   }

   private static void sanitizeStoredNpcData(CompoundTag npcData) {
      npcData.remove("Pos");
      npcData.remove("Motion");
      npcData.remove("Rotation");
      npcData.remove("UUID");
      npcData.remove("Passengers");
      npcData.remove("Leash");
   }
}
