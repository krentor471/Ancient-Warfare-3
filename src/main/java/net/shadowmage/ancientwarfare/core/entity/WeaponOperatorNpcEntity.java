package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.siege.SiegeWeaponControl;

public abstract class WeaponOperatorNpcEntity extends PlayerOwnedNpcEntity {
   protected final SiegeEngineerBrain siegeBrain = new SiegeEngineerBrain(this);

   protected WeaponOperatorNpcEntity(EntityType<? extends WeaponOperatorNpcEntity> type, Level level) {
      super(type, level);
   }

   public static Builder createAttributes() {
      return m_21552_().m_22268_(Attributes.f_22276_, 20.0).m_22268_(Attributes.f_22279_, 0.25).m_22268_(Attributes.f_22277_, 120.0);
   }

   @Override
   public boolean isCombatGroupEligible() {
      return true;
   }

   protected void m_8099_() {
      this.f_21345_.m_25352_(0, new FloatGoal(this));
      this.f_21345_.m_25352_(2, new WeaponOperatorNpcEntity.MoveToCommandGoal(this));
      this.f_21345_.m_25352_(5, new CombatGroupFollowGoal(this));
      this.f_21345_.m_25352_(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.f_21345_.m_25352_(8, new RandomLookAroundGoal(this));
   }

   public Optional<SiegeWeaponControl> getSiegeWeapon() {
      return this.siegeBrain.getSiegeWeapon();
   }

   public void setSiegeWeapon(SiegeWeaponControl weapon) {
      this.siegeBrain.setSiegeWeapon(weapon);
   }

   public void clearSiegeWeapon() {
      this.siegeBrain.clearSiegeWeapon();
   }

   @Override
   protected boolean hasActiveWeaponDeployment() {
      return this.getSiegeWeapon().filter(weapon -> {
         if (weapon instanceof Entity entity && (!entity.m_6084_() || !this.isSiegeEngineerProfession() && this.m_20202_() != entity)) {
            return false;
         }

         return true;
      }).isPresent();
   }

   @Override
   public void m_8107_() {
      if (!this.m_9236_().f_46443_) {
         this.siegeBrain.getSiegeWeapon().filter(weapon -> {
            if (weapon instanceof Entity entity && (!entity.m_6084_() || !this.isSiegeEngineerProfession() && this.m_20202_() != entity)) {
               return true;
            }

            return false;
         }).ifPresent(weapon -> this.clearSiegeWeapon());
      }

      super.m_8107_();
      if (this.isCombatGroupCommander()) {
         CombatGroupController.tick(this);
      }
   }

   private static final class MoveToCommandGoal extends Goal {
      private final WeaponOperatorNpcEntity npc;
      private BlockPos target;

      private MoveToCommandGoal(WeaponOperatorNpcEntity npc) {
         this.npc = npc;
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         if (this.npc.getCurrentCommand() != NpcCommandType.MOVE) {
            return false;
         } else {
            this.target = this.npc.getCommandTarget();
            return !BlockPos.f_121853_.equals(this.target) && this.distanceToTargetSq() > 4.0;
         }
      }

      public boolean m_8045_() {
         return this.npc.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() > 4.0;
      }

      public void m_8037_() {
         this.npc.m_21563_().m_24946_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
         this.npc.m_21573_().m_26519_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5, 1.0);
      }

      public void m_8041_() {
         if (this.npc.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() <= 4.0) {
            this.npc.clearCommand();
         }

         this.target = null;
         this.npc.m_21573_().m_26573_();
      }

      private double distanceToTargetSq() {
         return this.npc.m_20275_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
      }
   }
}
