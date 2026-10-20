package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.FactionSpellcasterNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.NpcBaseEntity;
import net.shadowmage.ancientwarfare.core.entity.PlayerOwnedNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.WorkerNpcEntity;

public class NpcHumanoidModel<T extends NpcBaseEntity> extends HumanoidModel<T> {
   public NpcHumanoidModel(ModelPart root) {
      super(root);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      boolean var10001;
      label78: {
         label70:
         if (!entity.m_20159_()) {
            if (entity instanceof PlayerOwnedNpcEntity owned && owned.isLogicalDragonRider()) {
               break label70;
            }

            var10001 = false;
            break label78;
         }

         var10001 = true;
      }

      this.f_102609_ = var10001;
      this.f_102816_ = ArmPose.EMPTY;
      this.f_102815_ = ArmPose.EMPTY;
      if (entity.isShielding()) {
         if (entity.m_5737_() == HumanoidArm.RIGHT) {
            this.f_102815_ = ArmPose.BLOCK;
         } else {
            this.f_102816_ = ArmPose.BLOCK;
         }
      } else if (entity.m_6117_()) {
         ArmPose rangedPose = this.rangedArmPose(entity);
         if (entity.m_7655_() == InteractionHand.MAIN_HAND ? entity.m_5737_() != HumanoidArm.RIGHT : entity.m_5737_() != HumanoidArm.LEFT) {
            this.f_102815_ = rangedPose;
         } else {
            this.f_102816_ = rangedPose;
         }
      }

      label76: {
         super.m_6973_(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
         if (entity instanceof CombatNpcEntity combat && combat.getSwordsmanComboStep() > 0) {
            this.applySwordsmanComboPose(combat);
            break label76;
         }

         if (entity instanceof CombatNpcEntity combat && combat.isSpellcastingAnimation()) {
            this.applySpellcastingPose(combat, ageInTicks);
            break label76;
         }

         if (entity instanceof FactionSpellcasterNpcEntity spellcaster && spellcaster.isSpellcastingAnimation()) {
            this.applySpellcastingPose(ageInTicks);
            break label76;
         }

         if (entity.getMeleeAttackAnimationTicks() > 0) {
            this.applyMeleeAttackPose(entity);
         }
      }

      if (entity instanceof WorkerNpcEntity worker && worker.isWorkingAnimation() && entity.m_21324_(0.0F) <= 0.01F) {
         float phase = (float)entity.f_19797_ * 0.35F;
         float swing = Mth.m_14031_(phase) * 0.65F;
         this.f_102811_.f_104203_ = -1.05F + swing;
         this.f_102811_.f_104204_ = -0.12F;
         this.f_102811_.f_104205_ = 0.08F;
      }
   }

   private void applySpellcastingPose(CombatNpcEntity entity, float ageInTicks) {
      this.applySpellcastingPose(ageInTicks);
   }

   private void applySpellcastingPose(float ageInTicks) {
      float pulse = Mth.m_14031_(ageInTicks * 0.18F) * 0.08F;
      this.f_102810_.f_104204_ = Mth.m_14031_(ageInTicks * 0.08F) * 0.06F;
      this.f_102811_.f_104203_ = -1.28F + pulse;
      this.f_102811_.f_104204_ = -0.24F;
      this.f_102811_.f_104205_ = 0.16F;
      this.f_102812_.f_104203_ = -1.18F - pulse;
      this.f_102812_.f_104204_ = 0.24F;
      this.f_102812_.f_104205_ = -0.16F;
   }

   private void applyMeleeAttackPose(T entity) {
      float progress = Mth.m_14036_((12.0F - (float)entity.getMeleeAttackAnimationTicks()) / 11.0F, 0.0F, 1.0F);
      float strike = Mth.m_14031_(progress * (float) Math.PI);
      boolean rightHanded = entity.m_5737_() == HumanoidArm.RIGHT;
      float side = rightHanded ? 1.0F : -1.0F;
      this.f_102810_.f_104204_ = -side * strike * 0.32F;
      this.f_102808_.f_104204_ = this.f_102808_.f_104204_ - this.f_102810_.f_104204_ * 0.45F;
      if (rightHanded) {
         this.f_102811_.f_104203_ = -0.65F - strike * 1.75F;
         this.f_102811_.f_104204_ = -0.55F + strike * 0.85F;
         this.f_102811_.f_104205_ = 0.28F - strike * 0.48F;
      } else {
         this.f_102812_.f_104203_ = -0.65F - strike * 1.75F;
         this.f_102812_.f_104204_ = 0.55F - strike * 0.85F;
         this.f_102812_.f_104205_ = -0.28F + strike * 0.48F;
      }
   }

   private void applySwordsmanComboPose(CombatNpcEntity entity) {
      float attack = entity.m_21324_(0.0F);
      float cut = Mth.m_14031_(Mth.m_14116_(attack) * (float) Math.PI);
      switch (entity.getSwordsmanComboStep()) {
         case 1:
            this.f_102811_.f_104203_ = -1.15F - cut * 0.95F;
            this.f_102811_.f_104204_ = -0.55F + cut * 0.35F;
            this.f_102811_.f_104205_ = 0.18F;
            this.f_102812_.f_104203_ = -0.35F;
            this.f_102812_.f_104204_ = 0.3F;
            break;
         case 2:
            this.f_102811_.f_104203_ = -1.05F - cut * 0.9F;
            this.f_102811_.f_104204_ = 0.6F - cut * 0.4F;
            this.f_102811_.f_104205_ = -0.2F;
            this.f_102812_.f_104203_ = -0.45F;
            this.f_102812_.f_104204_ = -0.25F;
            break;
         case 3:
         case 4:
         case 5:
         case 6:
            this.f_102811_.f_104203_ = -1.75F + cut * 0.55F;
            this.f_102811_.f_104204_ = 0.0F;
            this.f_102811_.f_104205_ = 0.0F;
            this.f_102812_.f_104203_ = -1.15F + cut * 0.35F;
            this.f_102812_.f_104204_ = 0.15F;
            break;
         case 7:
            float side = (entity.f_19797_ & 1) == 0 ? 1.0F : -1.0F;
            this.f_102811_.f_104203_ = -1.4F - cut * 0.75F;
            this.f_102811_.f_104204_ = side * (0.85F - cut * 0.35F);
            this.f_102811_.f_104205_ = -side * 0.25F;
            this.f_102812_.f_104203_ = -0.65F;
            this.f_102812_.f_104204_ = -side * 0.35F;
            break;
         case 8:
            this.f_102811_.f_104203_ = -1.6F - cut * 0.7F;
            this.f_102811_.f_104204_ = -0.85F + cut * 1.35F;
            this.f_102811_.f_104205_ = 0.3F;
            this.f_102812_.f_104203_ = -0.8F;
            this.f_102812_.f_104204_ = 0.4F;
            break;
         case 9:
            this.f_102810_.f_104203_ = 0.3F;
            this.f_102811_.f_104203_ = -1.7F - cut * 0.45F;
            this.f_102811_.f_104204_ = 0.65F;
            this.f_102811_.f_104205_ = -0.35F;
            this.f_102812_.f_104203_ = -1.1F;
            this.f_102812_.f_104204_ = -0.45F;
            break;
         case 10:
            this.f_102811_.f_104203_ = -2.55F + cut * 1.15F;
            this.f_102811_.f_104204_ = 0.15F;
            this.f_102811_.f_104205_ = 0.35F;
            this.f_102812_.f_104203_ = -1.1F;
            this.f_102812_.f_104204_ = -0.2F;
            break;
         case 11:
         case 12:
            this.f_102810_.f_104203_ = -0.2F;
            this.f_102811_.f_104203_ = -2.75F + cut * 0.55F;
            this.f_102811_.f_104204_ = 0.1F;
            this.f_102812_.f_104203_ = -1.65F;
            this.f_102812_.f_104204_ = -0.15F;
            break;
         case 13:
            this.f_102810_.f_104203_ = -0.1F;
            this.f_102811_.f_104203_ = -1.75F - cut * 0.55F;
            this.f_102811_.f_104204_ = -0.55F;
            this.f_102811_.f_104205_ = 0.2F;
            this.f_102812_.f_104203_ = -0.85F;
            this.f_102812_.f_104204_ = 0.35F;
            break;
         case 14:
            this.f_102810_.f_104203_ = -0.12F;
            this.f_102811_.f_104203_ = -1.6F - cut * 0.6F;
            this.f_102811_.f_104204_ = 0.6F;
            this.f_102811_.f_104205_ = -0.25F;
            this.f_102812_.f_104203_ = -0.9F;
            this.f_102812_.f_104204_ = -0.3F;
            break;
         case 15:
         case 17:
            this.f_102810_.f_104203_ = 0.22F;
            this.f_102811_.f_104203_ = -2.65F + cut * 0.75F;
            this.f_102811_.f_104204_ = 0.05F;
            this.f_102812_.f_104203_ = -1.45F;
            this.f_102812_.f_104204_ = 0.1F;
            break;
         case 16:
            this.f_102810_.f_104203_ = -0.28F;
            this.f_102811_.f_104203_ = -2.45F + cut * 0.45F;
            this.f_102811_.f_104204_ = -0.25F;
            this.f_102811_.f_104205_ = 0.2F;
            this.f_102812_.f_104203_ = -1.5F;
            this.f_102812_.f_104204_ = 0.2F;
            break;
         case 18:
            this.f_102810_.f_104203_ = 0.55F;
            this.f_102808_.f_104203_ += 0.2F;
            this.f_102811_.f_104203_ = -2.8F + cut * 0.3F;
            this.f_102811_.f_104204_ = 0.05F;
            this.f_102812_.f_104203_ = -2.15F;
            this.f_102812_.f_104204_ = -0.1F;
            this.f_102813_.f_104203_ = -0.35F;
            this.f_102814_.f_104203_ = 0.35F;
            break;
         case 19:
            this.f_102810_.f_104204_ = -0.18F;
            this.f_102811_.f_104203_ = -1.85F;
            this.f_102811_.f_104204_ = 0.72F;
            this.f_102811_.f_104205_ = -0.38F;
            this.f_102812_.f_104203_ = -1.15F;
            this.f_102812_.f_104204_ = -0.55F;
            this.f_102812_.f_104205_ = 0.28F;
            break;
         case 20:
            this.f_102810_.f_104204_ = cut * 0.45F;
            this.f_102811_.f_104203_ = -2.35F + cut * 1.45F;
            this.f_102811_.f_104204_ = -0.75F + cut * 1.25F;
            this.f_102811_.f_104205_ = 0.22F;
            this.f_102812_.f_104203_ = -1.25F + cut * 0.35F;
            this.f_102812_.f_104204_ = 0.35F;
            break;
         case 21:
            this.f_102810_.f_104203_ = 0.35F;
            this.f_102811_.f_104203_ = -1.45F;
            this.f_102811_.f_104204_ = -0.08F;
            this.f_102811_.f_104205_ = -0.12F;
            this.f_102812_.f_104203_ = -1.25F;
            this.f_102812_.f_104204_ = 0.12F;
            break;
         case 22:
            this.f_102810_.f_104203_ = -0.12F;
            this.f_102811_.f_104203_ = -2.75F + cut * 1.65F;
            this.f_102811_.f_104204_ = 0.05F;
            this.f_102812_.f_104203_ = -1.7F + cut * 0.65F;
            this.f_102812_.f_104204_ = -0.18F;
      }
   }

   private ArmPose rangedArmPose(T entity) {
      if (entity.m_21211_().m_41720_() instanceof BowItem) {
         return ArmPose.BOW_AND_ARROW;
      } else {
         return entity.m_21211_().m_41720_() instanceof CrossbowItem ? ArmPose.CROSSBOW_CHARGE : ArmPose.ITEM;
      }
   }
}
