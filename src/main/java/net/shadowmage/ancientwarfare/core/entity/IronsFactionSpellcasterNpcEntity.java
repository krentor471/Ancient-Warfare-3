package net.shadowmage.ancientwarfare.core.entity;

import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainerMutable;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.api.spells.SpellSlot;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class IronsFactionSpellcasterNpcEntity extends FactionSpellcasterNpcEntity implements IMagicEntity {
   private static final String TAG_COOLDOWNS = "IronsFactionSpellCooldowns";
   private static final double CAST_RANGE = 28.0;
   private static final int GLOBAL_CAST_DELAY = 14;
   private static final Set<String> BASIC_ATTACK_SPELLS = Set.of(
      "magic_missile",
      "magic_arrow",
      "firebolt",
      "fire_arrow",
      "snowball",
      "icicle",
      "poison_arrow",
      "acid_orb",
      "guiding_bolt",
      "blood_needles",
      "eldritch_blast",
      "lightning_bolt"
   );
   private final MagicData magicData = new MagicData(true);
   private final List<SpellData> spellPool = new ArrayList<>();
   private final Map<String, Integer> cooldowns = new HashMap<>();
   @Nullable
   private SpellData castingSpell;
   @Nullable
   private LivingEntity castingTarget;
   private int globalCastDelay;
   private String lastSpell = "";
   private ItemStack cachedBook = ItemStack.f_41583_;
   private boolean usedSingleAttack;

   public IronsFactionSpellcasterNpcEntity(EntityType<? extends FactionSpellcasterNpcEntity> entityType, Level level) {
      super(entityType, level);
      this.magicData.setSyncedData(new SyncedSpellData(this));
   }

   @Override
   protected void m_8099_() {
      this.addSharedGoals();
      this.f_21345_.m_25352_(2, new IronsFactionSpellcasterNpcEntity.FactionSpellCastingGoal());
      this.addFactionCombatTargets();
   }

   protected void m_8024_() {
      super.m_8024_();
      this.ensureRandomSpellbook();
      this.cooldowns.replaceAll((id, ticks) -> Math.max(0, ticks - 1));
      this.cooldowns.values().removeIf(ticks -> ticks <= 0);
      if (this.globalCastDelay > 0) {
         this.globalCastDelay--;
      }

      this.refreshSpellPool();
      this.tickActiveCast();
   }

   @Override
   public void m_8107_() {
      super.m_8107_();
      if (this.m_9236_().f_46443_) {
         this.refreshSpellPool();
      }
   }

   private void ensureRandomSpellbook() {
      ItemStack held = this.m_21205_();
      if (!ISpellContainer.isSpellContainer(held)) {
         ItemStack book = new ItemStack((ItemLike)ItemRegistry.COPPER_SPELL_BOOK.get());
         List<AbstractSpell> candidates = new ArrayList<>();

         for (String id : BASIC_ATTACK_SPELLS) {
            AbstractSpell spell = SpellRegistry.getSpell("irons_spellbooks:" + id);
            if (spell != null && spell.isEnabled()) {
               candidates.add(spell);
            }
         }

         for (int i = candidates.size() - 1; i > 0; i--) {
            int swap = this.f_19796_.m_188503_(i + 1);
            AbstractSpell value = candidates.get(i);
            candidates.set(i, candidates.get(swap));
            candidates.set(swap, value);
         }

         int spellCount = Math.min(candidates.size(), 3 + this.f_19796_.m_188503_(3));
         ISpellContainerMutable mutable = ISpellContainer.create(5, true, true).mutableCopy();

         for (int i = 0; i < spellCount; i++) {
            AbstractSpell spell = candidates.get(i);
            int maxLevel = Math.max(1, Math.min(3, spell.getMaxLevel()));
            int level = 1 + this.f_19796_.m_188503_(maxLevel);
            mutable.addSpell(spell, level, true);
         }

         ISpellContainer.set(book, mutable.toImmutable());
         this.m_21008_(InteractionHand.MAIN_HAND, book);
         this.cachedBook = ItemStack.f_41583_;
      }
   }

   private void refreshSpellPool() {
      ItemStack held = this.m_21205_();
      if (!ItemStack.m_150942_(held, this.cachedBook)) {
         this.cachedBook = held.m_41777_();
         this.spellPool.clear();
         if (ISpellContainer.isSpellContainer(held)) {
            ISpellContainer container = ISpellContainer.get(held);
            if (container != null) {
               for (SpellSlot slot : container.getActiveSpells()) {
                  AbstractSpell spell = slot.getSpell();
                  if (spell.isEnabled() && BASIC_ATTACK_SPELLS.contains(spell.getSpellName().toLowerCase(Locale.ROOT))) {
                     this.spellPool.add(new SpellData(spell, Math.max(1, slot.getLevel())));
                  }
               }
            }
         }
      }
   }

   private void tickActiveCast() {
      if (this.castingSpell != null) {
         AbstractSpell spell = this.castingSpell.getSpell();
         LivingEntity target = this.castingTarget;
         if (target != null && target.m_6084_() && !spell.shouldAIStopCasting(this.castingSpell.getLevel(), this, target)) {
            this.faceTarget(target);
            this.magicData.handleCastDuration();
            if (this.magicData.isCasting()) {
               spell.onServerCastTick(this.m_9236_(), this.castingSpell.getLevel(), this, this.magicData);
            }

            if (this.magicData.getCastDurationRemaining() <= 0) {
               if (spell.getCastType() == CastType.LONG || spell.getCastType() == CastType.INSTANT) {
                  spell.onCast(this.m_9236_(), this.castingSpell.getLevel(), this, CastSource.MOB, this.magicData);
               }

               this.finishCast(false);
            } else if (spell.getCastType() == CastType.CONTINUOUS && (this.magicData.getCastDurationRemaining() + 1) % 10 == 0) {
               spell.onCast(this.m_9236_(), this.castingSpell.getLevel(), this, CastSource.MOB, this.magicData);
            }
         } else {
            this.cancelCast();
         }
      }
   }

   private void beginCast(SpellData selected, LivingEntity target) {
      if (selected != null && !this.isCasting() && !this.m_9236_().f_46443_) {
         AbstractSpell spell = selected.getSpell();
         this.faceTarget(target);
         if (!spell.checkPreCastConditions(this.m_9236_(), selected.getLevel(), this, this.magicData)) {
            this.globalCastDelay = 6;
         } else {
            this.castingSpell = selected;
            this.castingTarget = target;
            this.magicData
               .initiateCast(spell, selected.getLevel(), spell.getEffectiveCastTime(selected.getLevel(), this), CastSource.MOB, SpellSelectionManager.MAINHAND);
            spell.onServerPreCast(this.m_9236_(), selected.getLevel(), this, this.magicData);
            this.m_21011_(InteractionHand.MAIN_HAND, true);
         }
      }
   }

   private void finishCast(boolean cancelled) {
      if (this.castingSpell == null) {
         this.magicData.resetCastingState();
      } else {
         AbstractSpell spell = this.castingSpell.getSpell();
         int spellLevel = this.castingSpell.getLevel();
         spell.onServerCastComplete(this.m_9236_(), spellLevel, this, this.magicData, cancelled);
         if (!cancelled) {
            this.cooldowns.put(spell.getSpellName(), Math.max(20, Utils.applyCooldownReduction(spell.getSpellCooldown(), this)));
            this.lastSpell = spell.getSpellName();
            this.globalCastDelay = 14;
         } else {
            this.globalCastDelay = 5;
         }

         this.castingSpell = null;
         this.castingTarget = null;
      }
   }

   @Nullable
   private SpellData chooseReadySpell() {
      List<SpellData> ready = this.spellPool.stream().filter(data -> !this.cooldowns.containsKey(data.getSpell().getSpellName())).toList();
      if (ready.isEmpty()) {
         return null;
      } else {
         List<SpellData> alternatives = ready.stream().filter(data -> !data.getSpell().getSpellName().equals(this.lastSpell)).toList();
         List<SpellData> choices = alternatives.isEmpty() ? ready : alternatives;
         return choices.get(this.f_19796_.m_188503_(choices.size()));
      }
   }

   private void faceTarget(LivingEntity target) {
      Vec3 delta = target.m_146892_().m_82546_(this.m_146892_());
      double horizontal = Math.sqrt(delta.f_82479_ * delta.f_82479_ + delta.f_82481_ * delta.f_82481_);
      float yaw = (float)(Mth.m_14136_(delta.f_82481_, delta.f_82479_) * 180.0F / (float)Math.PI) - 90.0F;
      float pitch = (float)(-(Mth.m_14136_(delta.f_82480_, horizontal) * 180.0F / (float)Math.PI));
      this.m_146922_(yaw);
      this.f_20885_ = yaw;
      this.f_20883_ = yaw;
      this.m_146926_(pitch);
      this.m_21563_().m_24960_(target, 360.0F, 360.0F);
   }

   public MagicData getMagicData() {
      return this.magicData;
   }

   public void setSyncedSpellData(SyncedSpellData syncedSpellData) {
      if (this.m_9236_().f_46443_) {
         this.magicData.setSyncedData(syncedSpellData);
         this.castingSpell = syncedSpellData.isCasting()
            ? new SpellData(SpellRegistry.getSpell(syncedSpellData.getCastingSpellId()), syncedSpellData.getCastingSpellLevel())
            : null;
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
      LivingEntity target = this.m_5448_();
      if (target != null) {
         this.beginCast(new SpellData(spell, spellLevel), target);
      }
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

   public void setHasUsedSingleAttack(boolean used) {
      this.usedSingleAttack = used;
   }

   public void startDrinkingPotion() {
   }

   @Override
   public void m_7380_(CompoundTag tag) {
      super.m_7380_(tag);
      ListTag list = new ListTag();
      this.cooldowns.forEach((id, ticks) -> {
         if (ticks > 0) {
            CompoundTag entry = new CompoundTag();
            entry.m_128359_("Spell", id);
            entry.m_128405_("Ticks", ticks);
            list.add(entry);
         }
      });
      if (!list.isEmpty()) {
         tag.m_128365_("IronsFactionSpellCooldowns", list);
      }
   }

   @Override
   public void m_7378_(CompoundTag tag) {
      super.m_7378_(tag);
      this.cooldowns.clear();

      for (Tag value : tag.m_128437_("IronsFactionSpellCooldowns", 10)) {
         CompoundTag entry = (CompoundTag)value;
         this.cooldowns.put(entry.m_128461_("Spell"), Math.max(0, entry.m_128451_("Ticks")));
      }

      this.magicData.resetCastingState();
      this.castingSpell = null;
      this.castingTarget = null;
      this.cachedBook = ItemStack.f_41583_;
   }

   private final class FactionSpellCastingGoal extends Goal {
      private FactionSpellCastingGoal() {
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         LivingEntity target = IronsFactionSpellcasterNpcEntity.this.m_5448_();
         return !IronsFactionSpellcasterNpcEntity.this.spellPool.isEmpty() && target != null && target.m_6084_();
      }

      public boolean m_8045_() {
         return this.m_8036_();
      }

      public void m_8037_() {
         LivingEntity target = IronsFactionSpellcasterNpcEntity.this.m_5448_();
         if (target != null) {
            IronsFactionSpellcasterNpcEntity.this.faceTarget(target);
            double distanceSqr = IronsFactionSpellcasterNpcEntity.this.m_20280_(target);
            if (IronsFactionSpellcasterNpcEntity.this.isCasting()) {
               IronsFactionSpellcasterNpcEntity.this.m_21573_().m_26573_();
            } else if (!(distanceSqr > 784.0) && IronsFactionSpellcasterNpcEntity.this.m_21574_().m_148306_(target)) {
               if (distanceSqr < 25.0) {
                  Vec3 away = DefaultRandomPos.m_148407_(IronsFactionSpellcasterNpcEntity.this, 10, 4, target.m_20182_());
                  if (away != null) {
                     IronsFactionSpellcasterNpcEntity.this.m_21573_().m_26519_(away.f_82479_, away.f_82480_, away.f_82481_, 1.0);
                  }
               } else {
                  IronsFactionSpellcasterNpcEntity.this.m_21573_().m_26573_();
               }

               if (IronsFactionSpellcasterNpcEntity.this.globalCastDelay <= 0) {
                  SpellData selected = IronsFactionSpellcasterNpcEntity.this.chooseReadySpell();
                  if (selected != null) {
                     IronsFactionSpellcasterNpcEntity.this.beginCast(selected, target);
                  }
               }
            } else {
               IronsFactionSpellcasterNpcEntity.this.m_21573_().m_5624_(target, 1.0);
            }
         }
      }
   }
}
