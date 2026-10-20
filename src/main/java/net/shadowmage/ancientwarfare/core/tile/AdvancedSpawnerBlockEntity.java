package net.shadowmage.ancientwarfare.core.tile;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcDefaultsRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.structure.AdvancedSpawnerConfig;
import net.shadowmage.ancientwarfare.core.compat.LegacyIceAndFireStructureCompat;
import net.shadowmage.ancientwarfare.core.entity.FactionNpcEntity;
import net.shadowmage.ancientwarfare.core.menu.AdvancedSpawnerMenu;
import net.shadowmage.ancientwarfare.core.structure.AdvancedSpawnerWorldData;
import net.shadowmage.ancientwarfare.core.structure.LargeIslandWorldGenerator;
import org.jetbrains.annotations.Nullable;

public class AdvancedSpawnerBlockEntity extends BlockEntity implements MenuProvider {
   private static final String SETTINGS_TAG = "spawnerSettings";
   private static final String ENTITY_ID_TAG = "entityId";
   private static final String SPAWN_GROUPS_TAG = "spawnGroups";
   private static final String SETTINGS_LIST_TAG = "settingsList";
   private static final String ENTITY_NBT_TAG = "entityNbt";
   private static final String MIN_DELAY_TAG = "minDelay";
   private static final String MAX_DELAY_TAG = "maxDelay";
   private static final String SPAWN_DELAY_TAG = "spawnDelay";
   private static final String PLAYER_RANGE_TAG = "playerRange";
   private static final String MOB_RANGE_TAG = "mobRange";
   private static final String SPAWN_RANGE_TAG = "spawnRange";
   private static final String MAX_NEARBY_TAG = "maxNearbyMonsters";
   private static final String MIN_TO_SPAWN_TAG = "minToSpawn";
   private static final String MAX_TO_SPAWN_TAG = "maxToSpawn";
   private static final String CUSTOM_TAG = "customTag";
   private static final String FACTION_NAME_TAG = "factionName";
   private static final String LIGHT_SENSITIVE_TAG = "lightSensitive";
   private static final String REDSTONE_SENSITIVE_TAG = "redstoneSensitive";
   private static final String REDSTONE_MODE_TAG = "redstoneMode";
   private static final String TRANSPARENT_TAG = "transparent";
   private static final String DEBUG_MODE_TAG = "debugMode";
   private static final String SPAWN_Y_OFFSET_TAG = "spawnYOffset";
   private static final Map<String, ResourceLocation> LEGACY_ENTITY_IDS = new HashMap<>();
   private CompoundTag spawnerSettings = new CompoundTag();
   private ResourceLocation entityId;
   private String factionName = "";
   private String entityNbt = "";
   private CompoundTag customEntityTag = new CompoundTag();
   private int minDelay = 200;
   private int spawnDelay = 200;
   private int playerRange = 16;
   private int mobRange = 4;
   private int spawnRange;
   private int maxNearby = 8;
   private int minToSpawn = 1;
   private int maxToSpawn = 1;
   private int spawnYOffset;
   private boolean lightSensitive;
   private boolean redstoneSensitive;
   private boolean redstoneMode = true;
   private boolean transparent;
   private boolean debugMode;
   private boolean hasConfiguredSpawn;

   public AdvancedSpawnerBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.ADVANCED_SPAWNER), pos, state);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, AdvancedSpawnerBlockEntity spawner) {
      if (level instanceof ServerLevel serverLevel) {
         if (!LargeIslandWorldGenerator.isConstructingAt(serverLevel, pos)) {
            if (spawner.spawnDelay > 0) {
               spawner.spawnDelay--;
            } else if (spawner.hasConfiguredSpawn) {
               if (spawner.hasRequiredPlayer(serverLevel)
                  && !spawner.tooManyNearbyMobs(serverLevel)
                  && spawner.hasRequiredRedstone(serverLevel)
                  && spawner.hasRequiredLight(serverLevel)) {
                  if (spawner.spawnEntities(serverLevel)) {
                     serverLevel.removeBlock(pos, false);
                  }
               }
            }
         }
      }
   }

   public Component statusMessage() {
      return this.hasConfiguredSpawn ? Component.literal("One-shot spawner: " + this.entityId) : Component.literal("One-shot spawner: no spawn configured");
   }

   public Component getDisplayName() {
      return Component.translatable("block.ancientwarfare.advanced_spawner");
   }

   @Nullable
   public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
      return new AdvancedSpawnerMenu(containerId, inventory, this.worldPosition);
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      this.writeSettingsToTag(tag);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.readSettingsFromTag(tag);
   }

   private void readSettingsFromTag(CompoundTag tag) {
      if (tag.contains("spawnerSettings", 10)) {
         this.spawnerSettings = tag.getCompound("spawnerSettings").copy();
      } else {
         this.spawnerSettings = new CompoundTag();
      }

      this.applySettings(this.spawnerSettings);
   }

   private void writeSettingsToTag(CompoundTag tag) {
      if (!this.spawnerSettings.isEmpty()) {
         tag.put("spawnerSettings", this.spawnerSettings.copy());
      }
   }

   public CompoundTag getUpdateTag() {
      CompoundTag tag = super.getUpdateTag();
      this.writeSettingsToTag(tag);
      return tag;
   }

   public void handleUpdateTag(CompoundTag tag) {
      this.readSettingsFromTag(tag);
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   private void applySettings(CompoundTag settings) {
      this.hasConfiguredSpawn = false;
      this.entityId = null;
      this.factionName = "";
      this.entityNbt = "";
      this.customEntityTag = new CompoundTag();
      this.minDelay = this.nonNegativeOrDefault(settings, "minDelay", this.minDelay);
      this.spawnDelay = this.nonNegativeOrDefault(settings, "spawnDelay", this.spawnDelay);
      this.playerRange = this.nonNegativeOrDefault(settings, "playerRange", this.playerRange);
      this.mobRange = this.nonNegativeOrDefault(settings, "mobRange", this.mobRange);
      this.spawnRange = Math.max(0, settings.getInt("spawnRange"));
      this.maxNearby = this.nonNegativeOrDefault(settings, "maxNearbyMonsters", this.maxNearby);
      this.spawnYOffset = settings.getInt("spawnYOffset");
      this.lightSensitive = settings.getBoolean("lightSensitive");
      this.redstoneSensitive = settings.getBoolean("redstoneSensitive");
      this.redstoneMode = !settings.contains("redstoneMode") || settings.getBoolean("redstoneMode");
      this.transparent = settings.getBoolean("transparent");
      this.debugMode = settings.getBoolean("debugMode");
      this.readFirstSpawnSettings(settings).ifPresent(first -> {
         this.entityId = this.normalizeEntityId(first.getString("entityId"));
         this.minToSpawn = Math.max(1, this.nonNegativeOrDefault(first, "minToSpawn", this.minToSpawn));
         this.maxToSpawn = Math.max(this.minToSpawn, this.nonNegativeOrDefault(first, "maxToSpawn", this.maxToSpawn));
         this.hasConfiguredSpawn = this.entityId != null && BuiltInRegistries.ENTITY_TYPE.containsKey(this.entityId);
         this.entityNbt = first.getString("entityNbt");
         if (first.contains("customTag", 10)) {
            this.customEntityTag = first.getCompound("customTag").copy();
            this.factionName = this.customEntityTag.getString("factionName");
         }
      });
   }

   public AdvancedSpawnerConfig getConfig() {
      String id = this.entityId == null ? "" : this.entityId.toString();
      return new AdvancedSpawnerConfig(
         id,
         this.factionName,
         this.entityNbt,
         this.minToSpawn,
         this.maxToSpawn,
         this.spawnDelay,
         this.playerRange,
         this.mobRange,
         this.spawnRange,
         this.maxNearby,
         this.spawnYOffset,
         this.lightSensitive,
         this.redstoneSensitive,
         this.redstoneMode,
         this.transparent,
         this.debugMode
      );
   }

   public void applyConfig(AdvancedSpawnerConfig config) {
      CompoundTag settings = new CompoundTag();
      settings.putInt("minDelay", Math.max(0, config.spawnDelay()));
      settings.putInt("maxDelay", Math.max(0, config.spawnDelay()));
      settings.putInt("spawnDelay", Math.max(0, config.spawnDelay()));
      settings.putInt("playerRange", Math.max(0, config.playerRange()));
      settings.putInt("mobRange", Math.max(0, config.mobRange()));
      settings.putInt("spawnRange", 0);
      settings.putInt("maxNearbyMonsters", Math.max(0, config.maxNearby()));
      settings.putInt("spawnYOffset", config.spawnYOffset());
      settings.putBoolean("lightSensitive", config.lightSensitive());
      settings.putBoolean("redstoneSensitive", config.redstoneSensitive());
      settings.putBoolean("redstoneMode", config.redstoneMode());
      settings.putBoolean("transparent", config.transparent());
      settings.putBoolean("debugMode", config.debugMode());
      CompoundTag entitySettings = new CompoundTag();
      entitySettings.putString("entityId", config.entityId().trim());
      entitySettings.putString("entityNbt", config.entityNbt().trim());
      entitySettings.putInt("minToSpawn", Math.max(1, config.minToSpawn()));
      entitySettings.putInt("maxToSpawn", Math.max(Math.max(1, config.minToSpawn()), config.maxToSpawn()));
      CompoundTag customTag = new CompoundTag();
      if (!config.factionName().isBlank()) {
         customTag.putString("factionName", config.factionName().trim());
      }

      entitySettings.put("customTag", customTag);
      ListTag settingsList = new ListTag();
      settingsList.add(entitySettings);
      CompoundTag group = new CompoundTag();
      group.putInt("groupWeight", 1);
      group.put("settingsList", settingsList);
      ListTag groups = new ListTag();
      groups.add(group);
      settings.put("spawnGroups", groups);
      this.spawnerSettings = settings;
      this.applySettings(this.spawnerSettings);
      this.setChanged();
      if (this.level instanceof ServerLevel serverLevel) {
         serverLevel.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public void loadFromOldTemplateData(String data) {
      try {
         int jsonStart = data.indexOf("JSON:");
         String snbt = jsonStart >= 0 ? data.substring(jsonStart + "JSON:".length()) : data;
         CompoundTag root = TagParser.parseTag(snbt);
         if (!root.contains("teData", 10)) {
            return;
         }

         CompoundTag tileData = root.getCompound("teData");
         if (!tileData.contains("spawnerSettings", 10)) {
            return;
         }

         this.spawnerSettings = tileData.getCompound("spawnerSettings").copy();
         this.applySettings(this.spawnerSettings);
         this.setChanged();
         if (this.level instanceof ServerLevel serverLevel) {
            serverLevel.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
         }
      } catch (Exception var8) {
         AncientWarfareCore.LOG.warn("Unable to read legacy advanced spawner data at {}", this.worldPosition, var8);
      }
   }

   private Optional<CompoundTag> readFirstSpawnSettings(CompoundTag settings) {
      if (!settings.contains("spawnGroups", 9)) {
         return Optional.empty();
      } else {
         ListTag groups = settings.getList("spawnGroups", 10);
         if (groups.isEmpty()) {
            return Optional.empty();
         } else {
            CompoundTag group = groups.getCompound(0);
            if (!group.contains("settingsList", 9)) {
               return Optional.empty();
            } else {
               ListTag settingsList = group.getList("settingsList", 10);
               return settingsList.isEmpty() ? Optional.empty() : Optional.of(settingsList.getCompound(0));
            }
         }
      }
   }

   private int nonNegativeOrDefault(CompoundTag tag, String key, int fallback) {
      return tag.contains(key, 99) ? Math.max(0, tag.getInt(key)) : fallback;
   }

   private ResourceLocation normalizeEntityId(String rawId) {
      if (rawId != null && !rawId.isBlank()) {
         ResourceLocation mapped = LEGACY_ENTITY_IDS.get(rawId);
         if (mapped != null) {
            return mapped;
         } else {
            ResourceLocation id = ResourceLocation.tryParse(rawId);
            if (id == null) {
               return null;
            } else {
               ResourceLocation currentId = LegacyIceAndFireStructureCompat.remapEntity(id);
               return BuiltInRegistries.ENTITY_TYPE.containsKey(currentId) ? currentId : this.legacyFallback(id);
            }
         }
      } else {
         return null;
      }
   }

   private ResourceLocation legacyFallback(ResourceLocation oldId) {
      String path = oldId.getPath().toLowerCase(Locale.ROOT);
      if (containsAny(path, "wizard", "spellcaster", "summoner", "beholder", "lich", "necromancer", "illithid")) {
         return vanilla("evoker");
      } else if (containsAny(path, "goblin", "orc", "pirate", "brigand", "trollager", "inquisitor", "barakoana", "barakoaya")) {
         return vanilla("pillager");
      } else if (containsAny(path, "troll", "cyclops", "minotaur", "umberhulk", "wroughtnaut", "golem", "hydra", "manticore")) {
         return vanilla("ravager");
      } else if (containsAny(path, "skeleton", "bone", "dread_knight", "dread_thrall", "fossil")) {
         return vanilla("skeleton");
      } else if (containsAny(path, "ghoul", "zombie", "undead", "vampire", "mummy", "unburied", "undertaker", "scarecrow")) {
         return vanilla("husk");
      } else if (containsAny(path, "ghost", "wraith", "banshee", "specter", "spirit", "foglet")) {
         return vanilla("vex");
      } else if (containsAny(path, "spider", "scuttler", "cavefisher", "ant_hill")) {
         return vanilla("cave_spider");
      } else if (containsAny(path, "slime", "gelatinous")) {
         return vanilla("slime");
      } else if (containsAny(path, "mimic", "chest", "haunted_tool", "cursed_armor")) {
         return vanilla("shulker");
      } else if (containsAny(path, "dragon", "amphithere", "gryphon", "harpy", "phoenix")) {
         return vanilla("phantom");
      } else if (containsAny(path, "shark", "serpent", "guardian", "jellyfish", "nautilus", "eel", "ray", "orca", "mermaid", "siren", "selkie")) {
         return vanilla("guardian");
      } else if (containsAny(path, "fish", "cod", "piranha")) {
         return vanilla("salmon");
      } else if (containsAny(path, "crab", "horseshoecrab")) {
         return vanilla("turtle");
      } else if (containsAny(path, "croc", "alligator", "komodo", "naga", "snake", "anaconda", "rattlesnake", "lizard")) {
         return vanilla("zoglin");
      } else if (containsAny(path, "deer", "gazelle", "bison", "hippo", "goat")) {
         return vanilla("goat");
      } else if (containsAny(path, "wolf", "werewolf", "feralwolf")) {
         return vanilla("wolf");
      } else if (containsAny(path, "owl", "goose", "pheasant", "turkey", "seagull", "albatross", "roadrunner", "dodo", "parrot")) {
         return vanilla("parrot");
      } else if (containsAny(path, "rat")) {
         return vanilla("silverfish");
      } else if (containsAny(path, "pixie", "sprite", "fairy")) {
         return vanilla("allay");
      } else if (containsAny(path, "villager", "trader", "butler", "dwarf")) {
         return vanilla("villager");
      } else if (containsAny(path, "bear", "yeti", "frostmaw")) {
         return vanilla("polar_bear");
      } else {
         AncientWarfareCore.LOG.warn("No direct 1.20 entity for legacy spawner id {}; using zombie", oldId);
         return vanilla("zombie");
      }
   }

   private static boolean containsAny(String value, String... needles) {
      for (String needle : needles) {
         if (value.contains(needle)) {
            return true;
         }
      }

      return false;
   }

   private static ResourceLocation vanilla(String path) {
      return new ResourceLocation("minecraft", path);
   }

   private boolean hasRequiredPlayer(ServerLevel level) {
      double rangeSquared = (double)this.playerRange * (double)this.playerRange;
      boolean allowCreative = AdvancedSpawnerWorldData.get(level).creativePlayersActivateSpawners();
      return level.players()
         .stream()
         .anyMatch(
            player -> (allowCreative || !player.isCreative())
                  && !player.isSpectator()
                  && player.isAlive()
                  && player.distanceToSqr((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5)
                     <= rangeSquared
         );
   }

   private boolean tooManyNearbyMobs(ServerLevel level) {
      return this.maxNearby > 0 && this.mobRange > 0
         ? level.getEntitiesOfClass(Mob.class, new AABB(this.worldPosition).inflate((double)this.mobRange)).size() >= this.maxNearby
         : false;
   }

   private boolean hasRequiredRedstone(ServerLevel level) {
      if (!this.redstoneSensitive) {
         return true;
      } else {
         boolean powered = level.hasNeighborSignal(this.worldPosition);
         return this.redstoneMode == powered;
      }
   }

   private boolean hasRequiredLight(ServerLevel level) {
      return !this.lightSensitive || level.getMaxLocalRawBrightness(this.worldPosition) <= 7;
   }

   private boolean spawnEntities(ServerLevel level) {
      if (this.entityId != null && BuiltInRegistries.ENTITY_TYPE.containsKey(this.entityId)) {
         EntityType<?> type = (EntityType<?>)BuiltInRegistries.ENTITY_TYPE.get(this.entityId);
         int count = this.minToSpawn + level.random.nextInt(Math.max(1, this.maxToSpawn - this.minToSpawn + 1));
         boolean spawnedAny = false;

         for (int i = 0; i < count; i++) {
            Entity entity = type.create(level);
            if (entity != null) {
               BlockPos spawnPos = this.randomSpawnPos(level);
               entity.moveTo(
                  (double)spawnPos.getX() + 0.5,
                  (double)spawnPos.getY(),
                  (double)spawnPos.getZ() + 0.5,
                  level.random.nextFloat() * 360.0F,
                  0.0F
               );
               if (entity instanceof Mob mob) {
                  mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.SPAWNER, null, null);
               }

               this.applyEntityNbt(entity);
               entity.moveTo(
                  (double)spawnPos.getX() + 0.5, (double)spawnPos.getY(), (double)spawnPos.getZ() + 0.5, entity.getYRot(), entity.getXRot()
               );
               if (entity instanceof Mob) {
                  Mob mob = (Mob)entity;
                  if (!this.factionName.isBlank()) {
                     if (mob instanceof FactionNpcEntity factionNpc) {
                        factionNpc.setFactionName(this.factionName);
                     } else {
                        mob.addTag("ancientwarfare:faction=" + this.factionName);
                     }
                  }
               }

               boolean spawned = spawnEntityWithFactionMount(level, entity);
               spawnedAny |= spawned;
            }
         }

         return spawnedAny;
      } else {
         return false;
      }
   }

   private static boolean spawnEntityWithFactionMount(ServerLevel level, Entity entity) {
      if (entity instanceof FactionNpcEntity factionNpc) {
         Mob mount = createFactionMount(level, factionNpc);
         if (mount == null || !factionNpc.startRiding(mount, true)) {
            return level.addFreshEntity(entity);
         } else if (level.tryAddFreshEntityWithPassengers(mount)) {
            return true;
         } else {
            factionNpc.stopRiding();
            mount.discard();
            return level.addFreshEntity(entity);
         }
      } else {
         return level.addFreshEntity(entity);
      }
   }

   private static Mob createFactionMount(ServerLevel level, FactionNpcEntity npc) {
      FactionNpcRole role = FactionNpcRole.byId(npc.getNpcSubtype()).orElse(FactionNpcRole.SOLDIER);
      if (role != FactionNpcRole.CAVALRY && role != FactionNpcRole.MOUNTED_ARCHER) {
         return null;
      } else {
         Mob mount = (Mob)FactionNpcDefaultsRegistry.mountType(npc.getFactionName(), role).create(level);
         if (mount == null) {
            return null;
         } else {
            if (mount instanceof AbstractHorse horse) {
               horse.setTamed(true);
               horse.setTemper(100);
               horse.equipSaddle(SoundSource.NEUTRAL);
            }

            AttributeInstance speed = mount.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) {
               speed.setBaseValue(Math.max(speed.getBaseValue(), 0.32));
            }

            mount.setNoAi(false);
            mount.setPersistenceRequired();
            mount.addTag("ancientwarfare:faction=" + npc.getFactionName());
            mount.moveTo(npc.getX(), npc.getY(), npc.getZ(), npc.getYRot(), 0.0F);
            return mount;
         }
      }
   }

   private void applyEntityNbt(Entity entity) {
      if (!this.entityNbt.isBlank() || !this.customEntityTag.isEmpty()) {
         try {
            CompoundTag saved = new CompoundTag();
            entity.saveWithoutId(saved);
            if (!this.entityNbt.isBlank()) {
               saved.merge(TagParser.parseTag(this.entityNbt));
            }

            if (!this.customEntityTag.isEmpty()) {
               saved.merge(this.customEntityTag.copy());
            }

            normalizeLegacyCustomName(saved);
            entity.load(saved);
         } catch (Exception var3) {
            AncientWarfareCore.LOG.warn("Unable to apply legacy entity NBT for {} at {}", new Object[]{this.entityId, this.worldPosition, var3});
         }
      }
   }

   private static void normalizeLegacyCustomName(CompoundTag tag) {
      if (tag.contains("CustomName", 8)) {
         String rawName = tag.getString("CustomName");
         if (!rawName.isBlank() && !rawName.trim().startsWith("{") && !rawName.trim().startsWith("[")) {
            tag.putString("CustomName", Serializer.toJson(Component.literal(rawName)));
         }
      }
   }

   private BlockPos randomSpawnPos(ServerLevel level) {
      if (this.spawnRange <= 0) {
         return this.worldPosition.offset(0, this.spawnYOffset, 0);
      } else {
         int x = level.random.nextInt(this.spawnRange * 2 + 1) - this.spawnRange;
         int z = level.random.nextInt(this.spawnRange * 2 + 1) - this.spawnRange;
         return this.worldPosition.offset(x, this.spawnYOffset, z);
      }
   }

   static {
      LEGACY_ENTITY_IDS.put("ancientwarfarenpc:faction.soldier", AWCoreIds.FACTION_SOLDIER_NPC);
      LEGACY_ENTITY_IDS.put("ancientwarfarenpc:faction.soldier.elite", AWCoreIds.FACTION_SOLDIER_ELITE_NPC);
      LEGACY_ENTITY_IDS.put("ancientwarfarenpc:faction.archer", AWCoreIds.FACTION_ARCHER_NPC);
      LEGACY_ENTITY_IDS.put("ancientwarfarenpc:faction.archer.elite", AWCoreIds.FACTION_ARCHER_ELITE_NPC);
      LEGACY_ENTITY_IDS.put("ancientwarfarenpc:faction.cavalry", AWCoreIds.FACTION_CAVALRY_NPC);
      LEGACY_ENTITY_IDS.put("ancientwarfarenpc:faction.mounted_archer", AWCoreIds.FACTION_MOUNTED_ARCHER_NPC);
      LEGACY_ENTITY_IDS.put("ancientwarfarenpc:faction.leader", AWCoreIds.FACTION_LEADER_NPC);
      LEGACY_ENTITY_IDS.put("ancientwarfarenpc:faction.leader.elite", AWCoreIds.FACTION_LEADER_ELITE_NPC);
      LEGACY_ENTITY_IDS.put("ancientwarfarenpc:faction.siege.engineer", AWCoreIds.FACTION_SIEGE_ENGINEER_NPC);
      LEGACY_ENTITY_IDS.put("ancientwarfarenpc:faction.siege_engineer", AWCoreIds.FACTION_SIEGE_ENGINEER_NPC);
      LEGACY_ENTITY_IDS.put("ancientwarfarenpc:faction.civilian.male", AWCoreIds.FACTION_CIVILIAN_MALE_NPC);
      LEGACY_ENTITY_IDS.put("ancientwarfarenpc:faction.civilian.female", AWCoreIds.FACTION_CIVILIAN_FEMALE_NPC);
      LEGACY_ENTITY_IDS.put("ancientwarfarenpc:faction.priest", AWCoreIds.FACTION_PRIEST_NPC);
      LEGACY_ENTITY_IDS.put("ancientwarfarenpc:faction.spellcaster", AWCoreIds.FACTION_SPELLCASTER_NPC);
      LEGACY_ENTITY_IDS.put("ancientwarfarenpc:faction.bard", AWCoreIds.FACTION_BARD_NPC);
      LEGACY_ENTITY_IDS.put("ancientwarfarenpc:faction.trader", AWCoreIds.FACTION_TRADER_NPC);
      LEGACY_ENTITY_IDS.put("ancientwarfarenpc:aw_npc_worker", AWCoreIds.WORKER_NPC);
      LEGACY_ENTITY_IDS.put("minecraft:evocation_illager", vanilla("evoker"));
      LEGACY_ENTITY_IDS.put("minecraft:vindication_illager", vanilla("vindicator"));
      LEGACY_ENTITY_IDS.put("minecraft:illusion_illager", vanilla("illusioner"));
      LEGACY_ENTITY_IDS.put("minecraft:villager_golem", vanilla("iron_golem"));
      LEGACY_ENTITY_IDS.put("minecraft:zombie_pigman", vanilla("zombified_piglin"));
   }
}
