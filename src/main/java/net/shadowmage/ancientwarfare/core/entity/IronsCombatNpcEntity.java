package net.shadowmage.ancientwarfare.core.entity;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.attribute.IMagicAttribute;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.item.UpgradeData;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellSlot;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.capabilities.magic.TelekinesisData;
import io.redspace.ironsspellbooks.compat.Curios;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.spells.wall_of_fire.WallOfFireEntity;
import io.redspace.ironsspellbooks.spells.CastingMobAimingData;
import io.redspace.ironsspellbooks.util.UpgradeUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class IronsCombatNpcEntity extends CombatNpcEntity implements IMagicEntity {
   private static final String TAG_IRONS_COOLDOWNS = "IronsSpellCooldowns";
   private static final int GLOBAL_CAST_DELAY = 12;
   private static final double SPELL_RANGE = 32.0;
   private static final double CLOSE_MELEE_FALLBACK_RANGE = 4.0;
   private static final int FAILED_CAST_MELEE_TICKS = 40;
   private static final Set<String> SUPPORTED_SPELLS = Set.of(
      "magic_missile",
      "magic_arrow",
      "firebolt",
      "fireball",
      "fire_arrow",
      "icicle",
      "snowball",
      "ray_of_frost",
      "lightning_bolt",
      "lightning_lance",
      "ball_lightning",
      "poison_arrow",
      "guiding_bolt",
      "wither_skull",
      "acid_orb",
      "eldritch_blast",
      "blood_needles",
      "blood_slash",
      "arcane_shackle",
      "fang_strike",
      "shield",
      "gust",
      "evasion",
      "oakskin",
      "fortify",
      "abyssal_shroud",
      "ice_tomb",
      "heal",
      "greater_heal",
      "cleanse",
      "charge",
      "invisibility",
      "slow",
      "root",
      "blight",
      "chain_lightning",
      "blessing_of_life",
      "haste",
      "healing_circle",
      "arrow_volley",
      "fang_swirl",
      "firecracker",
      "magma_bomb",
      "flaming_strike",
      "sunbeam",
      "divine_smite",
      "cone_of_cold",
      "frostwave",
      "ice_spikes",
      "blizzard",
      "electrocute",
      "shockwave",
      "poison_breath",
      "poison_splash",
      "firefly_swarm",
      "earthquake",
      "stomp",
      "sculk_tentacles",
      "sonic_boom",
      "acupuncture",
      "devour",
      "ray_of_siphoning",
      "fire_breath",
      "dragon_breath",
      "blaze_storm",
      "shadow_slash",
      "gravity_fissure",
      "starfall",
      "black_hole",
      "chain_creeper",
      "lob_creeper",
      "fang_ward",
      "scorch",
      "heat_surge",
      "wisp",
      "thunderstorm",
      "volt_strike",
      "cloud_of_regeneration",
      "counterspell",
      "echoing_strikes",
      "flaming_barrage",
      "frostbite",
      "ice_block",
      "raise_dead",
      "raise_hell",
      "scapegoat",
      "soulfire_ray",
      "spider_aspect",
      "summon_polar_bear",
      "summon_swords",
      "summon_vex",
      "telekinesis",
      "throw",
      "heartstop",
      "sacrifice",
      "wall_of_fire"
   );
   private static final Set<String> SELF_HEALS = Set.of("heal", "greater_heal");
   private static final Set<String> ALLY_HEALS = Set.of("blessing_of_life", "healing_circle", "cloud_of_regeneration");
   private static final Set<String> ALLY_BUFFS = Set.of("haste", "fortify", "cleanse");
   private static final Set<String> DEFENSE = Set.of(
      "shield",
      "gust",
      "evasion",
      "oakskin",
      "abyssal_shroud",
      "ice_tomb",
      "charge",
      "invisibility",
      "echoing_strikes",
      "frostbite",
      "spider_aspect",
      "scapegoat",
      "heartstop"
   );
   private static final Set<String> CONTROL = Set.of("slow", "root", "blight", "arcane_shackle");
   private static final Set<String> CLOSE_RANGE = Set.of(
      "blood_slash",
      "gust",
      "fang_strike",
      "flaming_strike",
      "divine_smite",
      "frostwave",
      "shockwave",
      "stomp",
      "shadow_slash",
      "devour",
      "fang_ward",
      "heat_surge",
      "volt_strike"
   );
   private static final Set<String> LONG_RANGE = Set.of(
      "magic_arrow",
      "lightning_lance",
      "guiding_bolt",
      "poison_arrow",
      "lightning_bolt",
      "arrow_volley",
      "sunbeam",
      "sonic_boom",
      "firecracker",
      "lob_creeper",
      "wisp",
      "soulfire_ray",
      "throw",
      "ice_block"
   );
   private static final Set<String> AREA_ATTACKS = Set.of(
      "arrow_volley",
      "fang_swirl",
      "sunbeam",
      "blizzard",
      "poison_splash",
      "earthquake",
      "sculk_tentacles",
      "shockwave",
      "frostwave",
      "starfall",
      "black_hole",
      "chain_creeper",
      "scorch",
      "heat_surge",
      "fang_ward",
      "raise_hell",
      "wall_of_fire",
      "sacrifice"
   );
   private static final Set<String> CONTINUOUS_AIM_SPELLS = Set.of("ray_of_siphoning");
   private static final Set<String> TARGET_DATA_SPELLS = Set.of(
      "slow",
      "root",
      "blight",
      "chain_lightning",
      "blessing_of_life",
      "haste",
      "healing_circle",
      "arrow_volley",
      "fang_swirl",
      "sunbeam",
      "ice_spikes",
      "blizzard",
      "poison_splash",
      "firefly_swarm",
      "earthquake",
      "sculk_tentacles",
      "acupuncture",
      "devour",
      "chain_creeper",
      "wisp",
      "ice_block",
      "sacrifice"
   );
   private static final Set<String> SUMMON_SPELLS = Set.of("raise_dead", "summon_polar_bear", "summon_swords", "summon_vex");
   private static final Set<String> NPC_REPEAT_SPELLS = Set.of("eldritch_blast", "flaming_barrage", "raise_hell");
   private final MagicData magicData = new MagicData(true);
   private final List<IronsCombatNpcEntity.SpellChoice> spellPool = new ArrayList<>();
   private final Map<String, Integer> npcCooldowns = new HashMap<>();
   @Nullable
   private SpellData castingSpell;
   @Nullable
   private LivingEntity castingTarget;
   private int globalCastDelay;
   private String lastSpellId = "";
   private String previousSpellId = "";
   private boolean usedSingleAttack;
   @Nullable
   private SpellData repeatSpell;
   @Nullable
   private LivingEntity repeatTarget;
   private int repeatCastsRemaining;
   private int repeatCastDelay;
   private int forcedMeleeFallbackTicks;
   private ItemStack cachedMainHand = ItemStack.EMPTY;
   private ItemStack cachedOffHand = ItemStack.EMPTY;
   private final Set<IronsCombatNpcEntity.AppliedMagicModifier> appliedHandMagicModifiers = new HashSet<>();

   public IronsCombatNpcEntity(EntityType<? extends CombatNpcEntity> entityType, Level level) {
      super(entityType, level);
      this.magicData.setSyncedData(new SyncedSpellData(this));
   }

   @Override
   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(2, new IronsCombatNpcEntity.SpellbookCastingGoal());
   }

   @Override
   protected boolean isSpellcasterCombatant() {
      return !this.spellPool.isEmpty();
   }

   private void rebuildSpellPool() {
      this.spellPool.clear();
      Map<String, IronsCombatNpcEntity.SpellChoice> spellsById = new HashMap<>();
      this.collectSpells(this.getMainHandItem(), SpellSelectionManager.MAINHAND, spellsById);
      this.collectSpells(this.getOffhandItem(), SpellSelectionManager.OFFHAND, spellsById);
      this.spellPool.addAll(spellsById.values());
      if (this.spellPool.isEmpty() && this.isCasting()) {
         this.cancelCast();
      }
   }

   private void collectSpells(ItemStack stack, String equipmentSlot, Map<String, IronsCombatNpcEntity.SpellChoice> spellsById) {
      if (!stack.isEmpty() && ISpellContainer.isSpellContainer(stack)) {
         ISpellContainer container = ISpellContainer.get(stack);
         if (container != null) {
            for (SpellSlot slot : container.getActiveSpells()) {
               AbstractSpell spell = slot.getSpell();
               String id = spell.getSpellName().toLowerCase(Locale.ROOT);
               if (spell.isEnabled() && SUPPORTED_SPELLS.contains(id)) {
                  IronsCombatNpcEntity.SpellChoice choice = new IronsCombatNpcEntity.SpellChoice(spell, Math.max(1, slot.getLevel()), equipmentSlot);
                  spellsById.merge(
                     id, choice, (current, replacement) -> (IronsCombatNpcEntity.SpellChoice)(replacement.level() > current.level() ? replacement : current)
                  );
               }
            }
         }
      }
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
      this.npcCooldowns.replaceAll((id, ticks) -> Math.max(0, ticks - 1));
      this.npcCooldowns.values().removeIf(ticks -> ticks <= 0);
      if (this.globalCastDelay > 0) {
         this.globalCastDelay--;
      }

      if (this.forcedMeleeFallbackTicks > 0) {
         this.forcedMeleeFallbackTicks--;
      }

      this.refreshSpellPoolFromHands();
      this.tickActiveCast();
      this.tickRepeatCast();
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (this.level().isClientSide) {
         this.refreshSpellPoolFromHands();
      }
   }

   private void refreshSpellPoolFromHands() {
      ItemStack mainHand = this.getMainHandItem();
      ItemStack offHand = this.getOffhandItem();
      if (!ItemStack.isSameItemSameTags(mainHand, this.cachedMainHand) || !ItemStack.isSameItemSameTags(offHand, this.cachedOffHand)) {
         this.cachedMainHand = mainHand.copy();
         this.cachedOffHand = offHand.copy();
         this.rebuildSpellPool();
         if (!this.level().isClientSide) {
            this.refreshHandMagicAttributes(mainHand, offHand);
         }
      }
   }

   private void refreshHandMagicAttributes(ItemStack mainHand, ItemStack offHand) {
      this.clearHandMagicAttributes();
      this.applyHeldSpellbookAttributes(mainHand, 0, "mainhand_spellbook");
      this.applyHeldSpellbookAttributes(offHand, 1, "offhand_spellbook");
      Multimap<Attribute, AttributeModifier> nativeOffhand = offHand.getAttributeModifiers(EquipmentSlot.OFFHAND);
      boolean hasNativeOffhandMagic = nativeOffhand.keySet().stream().anyMatch(attribute -> attribute instanceof IMagicAttribute);
      if (!hasNativeOffhandMagic) {
         this.applyMagicAttributes(offHand.getAttributeModifiers(EquipmentSlot.MAINHAND), "offhand_item");
      }
   }

   private void applyHeldSpellbookAttributes(ItemStack stack, int handIndex, String source) {
      if (!stack.isEmpty() && ISpellContainer.isSpellContainer(stack) && stack.getItem() instanceof ICurioItem curioItem) {
         HashMultimap var10 = HashMultimap.create();
         UUID curioUuid = magicModifierUuid(source, null, null);
         SlotContext context = new SlotContext(Curios.SPELLBOOK_SLOT, this, handIndex, false, true);

         try {
            var10.putAll(curioItem.getAttributeModifiers(context, curioUuid, stack));
         } catch (RuntimeException var9) {
         }

         UpgradeData upgradeData = UpgradeData.getUpgradeData(stack);
         if (upgradeData != UpgradeData.NONE && Curios.SPELLBOOK_SLOT.equals(upgradeData.getUpgradedSlot())) {
            UpgradeUtils.handleAttributeEvent(var10, upgradeData, var10::put, var10::remove, Optional.of(curioUuid));
         }

         this.applyMagicAttributes(var10, source);
      }
   }

   private void applyMagicAttributes(Multimap<Attribute, AttributeModifier> modifiers, String source) {
      modifiers.forEach((attribute, modifier) -> {
         if (attribute instanceof IMagicAttribute) {
            AttributeInstance instance = this.getAttribute(attribute);
            if (instance != null) {
               UUID uuid = magicModifierUuid(source, attribute, modifier);
               instance.removeModifier(uuid);
               instance.addTransientModifier(new AttributeModifier(uuid, "Ancient Warfare held magic equipment bonus", modifier.getAmount(), modifier.getOperation()));
               this.appliedHandMagicModifiers.add(new IronsCombatNpcEntity.AppliedMagicModifier(attribute, uuid));
            }
         }
      });
   }

   private void clearHandMagicAttributes() {
      for (IronsCombatNpcEntity.AppliedMagicModifier applied : this.appliedHandMagicModifiers) {
         AttributeInstance instance = this.getAttribute(applied.attribute());
         if (instance != null) {
            instance.removeModifier(applied.uuid());
         }
      }

      this.appliedHandMagicModifiers.clear();
   }

   private static UUID magicModifierUuid(String source, @Nullable Attribute attribute, @Nullable AttributeModifier modifier) {
      String attributeId = attribute == null ? "none" : String.valueOf(BuiltInRegistries.ATTRIBUTE.getKey(attribute));
      String originalId = modifier == null ? "none" : modifier.getId().toString();
      String key = "ancientwarfare:irons_hand_bonus/" + source + "/" + attributeId + "/" + originalId;
      return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
   }

   private void tickActiveCast() {
      if (this.castingSpell != null) {
         AbstractSpell spell = this.castingSpell.getSpell();
         LivingEntity target = this.castingTarget;
         if (target != null && target.isAlive()) {
            this.faceTargetImmediately(target);
         }

         if (target == null || target.isAlive() && !spell.shouldAIStopCasting(this.castingSpell.getLevel(), this, target)) {
            this.magicData.handleCastDuration();
            if (this.magicData.isCasting()) {
               spell.onServerCastTick(this.level(), this.castingSpell.getLevel(), this, this.magicData);
            }

            if (this.magicData.getCastDurationRemaining() <= 0) {
               if (spell.getCastType() == CastType.LONG || spell.getCastType() == CastType.INSTANT) {
                  this.executeSpellCast(spell, this.castingSpell.getLevel(), target, false);
               }

               this.finishCast(false);
            } else if (spell.getCastType() == CastType.CONTINUOUS && (this.magicData.getCastDurationRemaining() + 1) % 10 == 0) {
               this.executeSpellCast(spell, this.castingSpell.getLevel(), target, false);
            }
         } else {
            this.cancelCast();
         }
      }
   }

   private void executeSpellCast(AbstractSpell spell, int spellLevel, @Nullable LivingEntity target, boolean isRepeat) {
      if ("wall_of_fire".equals(spell.getSpellName()) && target != null) {
         this.castNpcWallOfFire(spell, spellLevel, target);
      } else {
         spell.onCast(this.level(), spellLevel, this, CastSource.MOB, this.magicData);
      }

      if (!isRepeat && NPC_REPEAT_SPELLS.contains(spell.getSpellName())) {
         String var6 = spell.getSpellName();

         int totalCasts = switch (var6) {
            case "flaming_barrage" -> 5;
            case "eldritch_blast" -> Math.max(3, 2 + spellLevel);
            case "raise_hell" -> Math.max(1, spellLevel);
            default -> 1;
         };
         if (totalCasts > 1) {
            this.repeatSpell = new SpellData(spell, spellLevel);
            this.repeatTarget = target;
            this.repeatCastsRemaining = totalCasts - 1;
            this.repeatCastDelay = this.repeatDelayFor(spell.getSpellName());
         }
      }
   }

   private void castNpcWallOfFire(AbstractSpell spell, int spellLevel, LivingEntity target) {
      Vec3 forward = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
      if (forward.lengthSqr() < 0.01) {
         forward = this.getLookAngle().multiply(1.0, 0.0, 1.0);
      }

      forward = forward.normalize();
      Vec3 sideways = new Vec3(-forward.z, 0.0, forward.x);
      double halfLength = Math.min(12.0, 5.0 + (double)spellLevel * 1.5);
      Vec3 center = target.position().subtract(forward.scale(2.0));
      List<Vec3> anchors = List.of(center.add(sideways.scale(halfLength)), center.subtract(sideways.scale(halfLength)));
      WallOfFireEntity wall = new WallOfFireEntity(this.level(), this, anchors, spell.getSpellPower(spellLevel, this));
      wall.setPos(center);
      this.level().addFreshEntity(wall);
   }

   private void tickRepeatCast() {
      if (this.repeatSpell == null || this.repeatCastsRemaining <= 0) {
         this.clearRepeatCast();
      } else if (!this.isCasting()) {
         if (this.repeatTarget != null && this.repeatTarget.isAlive()) {
            this.faceTargetImmediately(this.repeatTarget);
            this.getNavigation().stop();
            if (this.repeatCastDelay-- <= 0) {
               this.executeSpellCast(this.repeatSpell.getSpell(), this.repeatSpell.getLevel(), this.repeatTarget, true);
               this.repeatCastsRemaining--;
               this.repeatCastDelay = this.repeatDelayFor(this.repeatSpell.getSpell().getSpellName());
               if (this.repeatCastsRemaining <= 0) {
                  this.clearRepeatCast();
               }
            }
         } else {
            this.clearRepeatCast();
         }
      }
   }

   private int repeatDelayFor(String spellId) {
      return "raise_hell".equals(spellId) ? 12 : 5;
   }

   private void clearRepeatCast() {
      this.repeatSpell = null;
      this.repeatTarget = null;
      this.repeatCastsRemaining = 0;
      this.repeatCastDelay = 0;
   }

   private void beginCast(IronsCombatNpcEntity.SpellChoice choice, @Nullable LivingEntity target) {
      if (choice != null && !this.isCasting() && !this.level().isClientSide) {
         AbstractSpell spell = choice.spell();
         this.castingTarget = target;
         if (target != null) {
            this.faceTargetImmediately(target);
         }

         if (TARGET_DATA_SPELLS.contains(spell.getSpellName()) && target != null) {
            this.magicData.setAdditionalCastData(new TargetEntityCastData(target));
         } else if ("telekinesis".equals(spell.getSpellName()) && target != null) {
            this.magicData.setAdditionalCastData(new TelekinesisData(this.distanceTo(target), target, 6));
         } else if (CONTINUOUS_AIM_SPELLS.contains(spell.getSpellName()) && target != null) {
            CastingMobAimingData aimData = new CastingMobAimingData();
            aimData.updateAim(target, 1.0F);
            this.magicData.setAdditionalCastData(aimData);
         } else if ("starfall".equals(spell.getSpellName()) && target != null) {
            this.faceTargetImmediately(target);
         }

         boolean hasExplicitTarget = (TARGET_DATA_SPELLS.contains(spell.getSpellName()) || "telekinesis".equals(spell.getSpellName())) && target != null;
         if (!hasExplicitTarget && !spell.checkPreCastConditions(this.level(), choice.level(), this, this.magicData)) {
            this.magicData.resetAdditionalCastData();
            this.castingTarget = null;
            this.globalCastDelay = 5;
            this.forcedMeleeFallbackTicks = 40;
         } else {
            if ("telekinesis".equals(spell.getSpellName()) && target != null) {
               this.magicData.setAdditionalCastData(new TelekinesisData(this.distanceTo(target), target, 6));
            } else if (TARGET_DATA_SPELLS.contains(spell.getSpellName()) && target != null) {
               this.magicData.setAdditionalCastData(new TargetEntityCastData(target));
            }

            this.castingSpell = new SpellData(spell, choice.level());
            this.forcedMeleeFallbackTicks = 0;
            this.magicData.initiateCast(spell, choice.level(), spell.getEffectiveCastTime(choice.level(), this), CastSource.MOB, choice.equipmentSlot());
            spell.onServerPreCast(this.level(), choice.level(), this, this.magicData);
            this.swing(InteractionHand.MAIN_HAND, true);
         }
      }
   }

   private void finishCast(boolean cancelled) {
      if (this.castingSpell == null) {
         this.magicData.resetCastingState();
      } else {
         AbstractSpell spell = this.castingSpell.getSpell();
         int level = this.castingSpell.getLevel();
         spell.onServerCastComplete(this.level(), level, this, this.magicData, cancelled);
         if (!cancelled) {
            int cooldown = Math.max(20, Utils.applyCooldownReduction(spell.getSpellCooldown(), this));
            this.npcCooldowns.put(spell.getSpellName(), cooldown);
            this.previousSpellId = this.lastSpellId;
            this.lastSpellId = spell.getSpellName();
            this.globalCastDelay = 12;
         } else {
            this.globalCastDelay = 5;
         }

         this.castingSpell = null;
         this.castingTarget = null;
      }
   }

   private IronsCombatNpcEntity.SpellSelection chooseSpell(LivingEntity enemy) {
      LivingEntity woundedAlly = this.findWoundedAlly();
      double distance = enemy == null ? Double.MAX_VALUE : (double)this.distanceTo(enemy);
      int nearbyEnemies = enemy == null ? 0 : this.level().getEntitiesOfClass(LivingEntity.class, enemy.getBoundingBox().inflate(5.0), this::isValidEnemy).size();
      List<IronsCombatNpcEntity.ScoredSpell> scored = new ArrayList<>();

      for (IronsCombatNpcEntity.SpellChoice choice : this.spellPool) {
         String id = choice.spell().getSpellName();
         if (!this.npcCooldowns.containsKey(id) && this.repeatSpell == null) {
            LivingEntity target = this.chooseTargetForSpell(id, enemy, woundedAlly);
            int score = this.scoreSpell(id, target, enemy, distance, nearbyEnemies);
            if (score > 0) {
               if (id.equals(this.lastSpellId)) {
                  score -= 22;
               }

               if (id.equals(this.lastSpellId) && id.equals(this.previousSpellId)) {
                  score -= 80;
               }

               score += this.random.nextInt(16);
               if (score > 0) {
                  scored.add(new IronsCombatNpcEntity.ScoredSpell(choice, target, score));
               }
            }
         }
      }

      if (scored.isEmpty()) {
         return null;
      } else {
         scored.sort(Comparator.comparingInt(IronsCombatNpcEntity.ScoredSpell::score).reversed());
         int candidates = Math.min(3, scored.size());
         int total = 0;

         for (int i = 0; i < candidates; i++) {
            total += Math.max(1, scored.get(i).score());
         }

         int roll = this.random.nextInt(Math.max(1, total));

         for (int i = 0; i < candidates; i++) {
            IronsCombatNpcEntity.ScoredSpell entry = scored.get(i);
            roll -= Math.max(1, entry.score());
            if (roll < 0) {
               return new IronsCombatNpcEntity.SpellSelection(entry.choice(), entry.target());
            }
         }

         IronsCombatNpcEntity.ScoredSpell first = scored.get(0);
         return new IronsCombatNpcEntity.SpellSelection(first.choice(), first.target());
      }
   }

   @Override
   protected boolean canUseMeleeCombatGoal() {
      LivingEntity enemy = this.getTarget();
      if (enemy != null && enemy.isAlive() && !this.isCasting() && this.repeatSpell == null && this.hasMeleeFallbackWeapon()) {
         double distanceSqr = this.distanceToSqr(enemy);
         if (this.forcedMeleeFallbackTicks > 0) {
            return true;
         } else {
            boolean hasReadySpell = this.globalCastDelay <= 0 && this.hasUsableSpell(enemy);
            return hasReadySpell ? false : this.globalCastDelay > 0 || distanceSqr <= 16.0 || !hasReadySpell;
         }
      } else {
         return false;
      }
   }

   private boolean hasMeleeFallbackWeapon() {
      ItemStack weapon = this.getMainHandItem();
      return !weapon.isEmpty() && weapon.getAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE);
   }

   private boolean hasUsableSpell(LivingEntity enemy) {
      if (enemy != null && enemy.isAlive() && this.repeatSpell == null && this.getSensing().hasLineOfSight(enemy)) {
         LivingEntity woundedAlly = this.findWoundedAlly();
         double distance = (double)this.distanceTo(enemy);
         int nearbyEnemies = this.level().getEntitiesOfClass(LivingEntity.class, enemy.getBoundingBox().inflate(5.0), this::isValidEnemy).size();

         for (IronsCombatNpcEntity.SpellChoice choice : this.spellPool) {
            String id = choice.spell().getSpellName();
            if (!this.npcCooldowns.containsKey(id)) {
               LivingEntity target = this.chooseTargetForSpell(id, enemy, woundedAlly);
               if (this.scoreSpell(id, target, enemy, distance, nearbyEnemies) > 0) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean hasReadyCloseRangeSpell() {
      return this.spellPool
         .stream()
         .anyMatch(
            choice -> {
               String id = choice.spell().getSpellName();
               return !this.npcCooldowns.containsKey(id)
                  && (
                     CLOSE_RANGE.contains(id)
                        || "fire_breath".equals(id)
                        || "dragon_breath".equals(id)
                        || "poison_breath".equals(id)
                        || "cone_of_cold".equals(id)
                        || "electrocute".equals(id)
                        || "ray_of_siphoning".equals(id)
                  );
            }
         );
   }

   private int scoreSpell(String id, @Nullable LivingEntity target, LivingEntity enemy, double distance, int nearbyEnemies) {
      float healthRatio = this.getHealth() / this.getMaxHealth();
      if (SELF_HEALS.contains(id)) {
         return healthRatio < 0.75F ? 35 + (int)((1.0F - healthRatio) * 100.0F) : 0;
      } else if (ALLY_HEALS.contains(id)) {
         if ("cloud_of_regeneration".equals(id)) {
            LivingEntity wounded = this.findWoundedAlly();
            if (wounded != null && !(this.distanceToSqr(wounded) > 25.0)) {
               float missing = 1.0F - wounded.getHealth() / wounded.getMaxHealth();
               return 50 + (int)(missing * 100.0F) + this.countWoundedNearbyAllies() * 12;
            } else {
               return 0;
            }
         } else if (target == null) {
            return 0;
         } else {
            float missing = 1.0F - target.getHealth() / target.getMaxHealth();
            return missing > 0.2F ? 35 + (int)(missing * 90.0F) : 0;
         }
      } else if (ALLY_BUFFS.contains(id)) {
         if ("fortify".equals(id)) {
            return this.countNearbyAllies() >= 2 ? 42 : 15;
         } else if ("cleanse".equals(id)) {
            return this.hasHarmfulEffectNearby() ? 75 : 0;
         } else {
            return target != null ? 30 : 0;
         }
      } else if ("counterspell".equals(id)) {
         return this.isActivelyCasting(enemy) ? 110 : 0;
      } else if (SUMMON_SPELLS.contains(id)) {
         return !SummonManager.getSummons(this).isEmpty() ? 0 : 48 + nearbyEnemies * 9;
      } else if ("sacrifice".equals(id)) {
         if (target instanceof IMagicSummon && enemy != null) {
            return target.distanceToSqr(enemy) <= 36.0 ? 62 + nearbyEnemies * 12 : 0;
         } else {
            return 0;
         }
      } else if (enemy == null || target == null || !this.getSensing().hasLineOfSight(enemy)) {
         return 0;
      } else if (DEFENSE.contains(id)) {
         int score = healthRatio < 0.55F ? 55 : 8;
         if (distance < 6.0) {
            score += 35;
         }

         return score;
      } else {
         int score = 42;
         if (CLOSE_RANGE.contains(id)) {
            if (distance > 9.0) {
               return 0;
            }

            score += (int)((9.0 - distance) * 5.0);
         } else if (LONG_RANGE.contains(id)) {
            if (distance < 5.0) {
               score -= 30;
            }

            if (distance > 14.0) {
               score += 25;
            }
         } else {
            if (distance > 32.0) {
               return 0;
            }

            if (distance >= 7.0 && distance <= 22.0) {
               score += 18;
            }
         }

         if (CONTROL.contains(id) && distance < 12.0) {
            score += 28;
         }

         if ("chain_lightning".equals(id)) {
            score += Math.max(0, nearbyEnemies - 1) * 18;
         }

         if (AREA_ATTACKS.contains(id)) {
            score += Math.max(0, nearbyEnemies - 1) * 15;
            if (nearbyEnemies <= 1) {
               score -= 12;
            }
         }

         if ("arrow_volley".equals(id) && distance < 10.0) {
            return 0;
         } else if ((
               "fire_breath".equals(id)
                  || "dragon_breath".equals(id)
                  || "poison_breath".equals(id)
                  || "cone_of_cold".equals(id)
                  || "electrocute".equals(id)
                  || "ray_of_siphoning".equals(id)
            )
            && distance > 12.0) {
            return 0;
         } else if ("ice_spikes".equals(id) && distance > 24.0) {
            return 0;
         } else {
            if ("starfall".equals(id) && nearbyEnemies < 2) {
               score -= 25;
            }

            if ("black_hole".equals(id) && distance < 7.0) {
               return 0;
            } else {
               if ("scorch".equals(id) && distance < 5.0) {
                  score -= 20;
               }

               if ("wisp".equals(id) && distance > 32.0) {
                  return 0;
               } else if (!"telekinesis".equals(id) || !(distance < 7.0) && !(distance > 24.0)) {
                  if ("raise_hell".equals(id) && distance > 8.0) {
                     return 0;
                  } else {
                     return !"wall_of_fire".equals(id) || !(distance < 5.0) && !(distance > 24.0) ? score : 0;
                  }
               } else {
                  return 0;
               }
            }
         }
      }
   }

   @Nullable
   private LivingEntity chooseTargetForSpell(String id, LivingEntity enemy, @Nullable LivingEntity woundedAlly) {
      if (SELF_HEALS.contains(id) || "cleanse".equals(id) || "fortify".equals(id) || DEFENSE.contains(id)) {
         return this;
      } else if (SUMMON_SPELLS.contains(id)) {
         return this;
      } else if ("sacrifice".equals(id)) {
         return this.findOwnedSummonNear(enemy);
      } else if ("cloud_of_regeneration".equals(id)) {
         return this;
      } else if (!ALLY_HEALS.contains(id) && !"haste".equals(id)) {
         return enemy;
      } else {
         return woundedAlly != null ? woundedAlly : this.findNearestAlly();
      }
   }

   @Nullable
   private LivingEntity findWoundedAlly() {
      return this.nearbyAllies()
         .stream()
         .filter(ally -> ally.getHealth() < ally.getMaxHealth() * 0.8F)
         .min(Comparator.comparingDouble(ally -> (double)(ally.getHealth() / ally.getMaxHealth())))
         .orElse(null);
   }

   @Nullable
   private LivingEntity findNearestAlly() {
      return this.nearbyAllies().stream().filter(ally -> ally != this).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(this);
   }

   private List<LivingEntity> nearbyAllies() {
      AABB area = this.getBoundingBox().inflate(16.0, 8.0, 16.0);
      return this.level().getEntitiesOfClass(LivingEntity.class, area, this::isFriendlyEntity);
   }

   private int countNearbyAllies() {
      return this.nearbyAllies().size();
   }

   private int countWoundedNearbyAllies() {
      return (int)this.nearbyAllies().stream().filter(ally -> ally.getHealth() < ally.getMaxHealth() * 0.8F).count();
   }

   @Nullable
   private LivingEntity findOwnedSummonNear(@Nullable LivingEntity enemy) {
      if (this.level() instanceof ServerLevel serverLevel && enemy != null) {
         return SummonManager.getSummons(this)
            .stream()
            .<Entity>map(serverLevel::getEntity)
            .filter(LivingEntity.class::isInstance)
            .map(LivingEntity.class::cast)
            .filter(Entity::isAlive)
            .filter(summon -> summon.distanceToSqr(enemy) <= 64.0)
            .min(Comparator.comparingDouble(summon -> summon.distanceToSqr(enemy)))
            .orElse(null);
      }

      return null;
   }

   private boolean hasHarmfulEffectNearby() {
      return this.nearbyAllies().stream().anyMatch(ally -> ally.getActiveEffects().stream().anyMatch(effect -> !effect.getEffect().isBeneficial()));
   }

   private boolean isActivelyCasting(@Nullable LivingEntity entity) {
      if (entity instanceof IMagicEntity caster) {
         return caster.isCasting();
      } else {
         return entity instanceof Player ? MagicData.getPlayerMagicData(entity).isCasting() : false;
      }
   }

   private boolean isFriendlyEntity(LivingEntity entity) {
      if (entity == this) {
         return true;
      } else if (entity instanceof Player player) {
         return this.getOwner().map(player.getUUID()::equals).orElse(false);
      } else if (!(entity instanceof PlayerOwnedNpcEntity owned)) {
         return entity instanceof IMagicSummon summon ? summon.getSummoner() == this : this.isAlliedTo(entity);
      } else {
         return this.getOwner().isPresent() && this.getOwner().equals(owned.getOwner());
      }
   }

   @Override
   public boolean isAlliedTo(Entity entity) {
      if (entity instanceof Player player && this.getOwner().map(player.getUUID()::equals).orElse(false)) {
         return true;
      }

      if (entity instanceof PlayerOwnedNpcEntity owned && this.getOwner().isPresent() && this.getOwner().equals(owned.getOwner())) {
         return true;
      }

      if (entity instanceof IMagicSummon summon && summon.getSummoner() == this) {
         return true;
      }

      return super.isAlliedTo(entity);
   }

   private boolean isValidEnemy(LivingEntity entity) {
      return entity != null && entity.isAlive() && !this.isFriendlyEntity(entity) && (!(entity instanceof Player player) || !player.isCreative());
   }

   private void faceTargetImmediately(LivingEntity target) {
      Vec3 delta = target.getEyePosition().subtract(this.getEyePosition());
      double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
      float yaw = (float)(Mth.atan2(delta.z, delta.x) * 180.0F / (float)Math.PI) - 90.0F;
      float pitch = (float)(-(Mth.atan2(delta.y, horizontal) * 180.0F / (float)Math.PI));
      this.setYRot(yaw);
      this.yHeadRot = yaw;
      this.yBodyRot = yaw;
      this.setXRot(pitch);
      this.getLookControl().setLookAt(target, 360.0F, 360.0F);
   }

   public MagicData getMagicData() {
      return this.magicData;
   }

   public void setSyncedSpellData(SyncedSpellData syncedSpellData) {
      if (this.level().isClientSide) {
         boolean wasCasting = this.magicData.isCasting();
         this.magicData.setSyncedData(syncedSpellData);
         this.castingSpell = syncedSpellData.isCasting()
            ? new SpellData(SpellRegistry.getSpell(syncedSpellData.getCastingSpellId()), syncedSpellData.getCastingSpellLevel())
            : null;
         if (!wasCasting && this.castingSpell != null) {
            InteractionHand castingHand = SpellSelectionManager.OFFHAND.equals(syncedSpellData.getCastingEquipmentSlot())
               ? InteractionHand.OFF_HAND
               : InteractionHand.MAIN_HAND;
            this.castingSpell.getSpell().onClientPreCast(this.level(), this.castingSpell.getLevel(), this, castingHand, this.magicData);
         }
      }
   }

   public boolean isCasting() {
      return this.magicData.isCasting();
   }

   @Override
   public boolean isSpellcastingAnimation() {
      return this.isCasting();
   }

   public void initiateCastSpell(AbstractSpell spell, int spellLevel) {
      this.beginCast(new IronsCombatNpcEntity.SpellChoice(spell, spellLevel, SpellSelectionManager.MAINHAND), this.getTarget());
   }

   public void cancelCast() {
      if (this.castingSpell != null) {
         this.finishCast(true);
      }
   }

   public void castComplete() {
      this.finishCast(false);
   }

   public void notifyDangerousProjectile(Projectile projectile) {
   }

   public boolean setTeleportLocationBehindTarget(int distance) {
      return false;
   }

   public void setBurningDashDirectionData() {
   }

   public boolean isDrinkingPotion() {
      return false;
   }

   public boolean getHasUsedSingleAttack() {
      return this.usedSingleAttack;
   }

   public void setHasUsedSingleAttack(boolean bool) {
      this.usedSingleAttack = bool;
   }

   public void startDrinkingPotion() {
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      ListTag cooldownList = new ListTag();
      this.npcCooldowns.forEach((id, ticks) -> {
         if (ticks > 0) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Spell", id);
            entry.putInt("Ticks", ticks);
            cooldownList.add(entry);
         }
      });
      if (!cooldownList.isEmpty()) {
         tag.put("IronsSpellCooldowns", cooldownList);
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.npcCooldowns.clear();

      for (Tag value : tag.getList("IronsSpellCooldowns", 10)) {
         CompoundTag entry = (CompoundTag)value;
         this.npcCooldowns.put(entry.getString("Spell"), Math.max(0, entry.getInt("Ticks")));
      }

      this.magicData.resetCastingState();
      this.castingSpell = null;
      this.castingTarget = null;
      this.clearRepeatCast();
      this.forcedMeleeFallbackTicks = 0;
      this.clearHandMagicAttributes();
      this.cachedMainHand = ItemStack.EMPTY;
      this.cachedOffHand = ItemStack.EMPTY;
      this.rebuildSpellPool();
   }

   private static record AppliedMagicModifier(Attribute attribute, UUID uuid) {
   }

   private static record ScoredSpell(IronsCombatNpcEntity.SpellChoice choice, @Nullable LivingEntity target, int score) {
   }

   private static record SpellChoice(AbstractSpell spell, int level, String equipmentSlot) {
   }

   private static record SpellSelection(IronsCombatNpcEntity.SpellChoice choice, @Nullable LivingEntity target) {
   }

   private final class SpellbookCastingGoal extends Goal {
      private LivingEntity enemy;

      private SpellbookCastingGoal() {
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         this.enemy = IronsCombatNpcEntity.this.getTarget();
         return !IronsCombatNpcEntity.this.spellPool.isEmpty()
            && this.enemy != null
            && this.enemy.isAlive()
            && !IronsCombatNpcEntity.this.canUseMeleeCombatGoal();
      }

      public boolean canContinueToUse() {
         this.enemy = IronsCombatNpcEntity.this.getTarget();
         return !IronsCombatNpcEntity.this.spellPool.isEmpty()
            && this.enemy != null
            && this.enemy.isAlive()
            && !IronsCombatNpcEntity.this.canUseMeleeCombatGoal();
      }

      public void tick() {
         this.enemy = IronsCombatNpcEntity.this.getTarget();
         if (this.enemy != null) {
            IronsCombatNpcEntity.this.faceTargetImmediately(this.enemy);
            double distanceSqr = IronsCombatNpcEntity.this.distanceToSqr(this.enemy);
            if (IronsCombatNpcEntity.this.isCasting()) {
               IronsCombatNpcEntity.this.getNavigation().stop();
            } else if (IronsCombatNpcEntity.this.repeatSpell != null) {
               IronsCombatNpcEntity.this.getNavigation().stop();
            } else if (!(distanceSqr > 1024.0) && IronsCombatNpcEntity.this.getSensing().hasLineOfSight(this.enemy)) {
               if (distanceSqr < 36.0) {
                  Vec3 away = DefaultRandomPos.getPosAway(IronsCombatNpcEntity.this, 10, 4, this.enemy.position());
                  if (away != null) {
                     IronsCombatNpcEntity.this.getNavigation()
                        .moveTo(away.x, away.y, away.z, IronsCombatNpcEntity.this.playerSprintNavigationSpeed());
                  }
               } else {
                  IronsCombatNpcEntity.this.getNavigation().stop();
               }

               if (IronsCombatNpcEntity.this.globalCastDelay <= 0) {
                  IronsCombatNpcEntity.SpellSelection selection = IronsCombatNpcEntity.this.chooseSpell(this.enemy);
                  if (selection != null) {
                     IronsCombatNpcEntity.this.beginCast(selection.choice(), selection.target());
                  } else if (distanceSqr > 81.0 && IronsCombatNpcEntity.this.hasReadyCloseRangeSpell()) {
                     IronsCombatNpcEntity.this.getNavigation().moveTo(this.enemy, IronsCombatNpcEntity.this.playerSprintNavigationSpeed());
                  }
               }
            } else {
               IronsCombatNpcEntity.this.getNavigation().moveTo(this.enemy, IronsCombatNpcEntity.this.playerSprintNavigationSpeed());
            }
         }
      }
   }
}
