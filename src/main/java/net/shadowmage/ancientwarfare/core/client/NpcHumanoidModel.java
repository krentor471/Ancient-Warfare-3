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
         if (!entity.isPassenger()) {
            if (entity instanceof PlayerOwnedNpcEntity owned && owned.isLogicalDragonRider()) {
               break label70;
            }

            var10001 = false;
            break label78;
         }

         var10001 = true;
      }

      this.riding = var10001;
      this.rightArmPose = ArmPose.EMPTY;
      this.leftArmPose = ArmPose.EMPTY;
      if (entity.isShielding()) {
         if (entity.getMainArm() == HumanoidArm.RIGHT) {
            this.leftArmPose = ArmPose.BLOCK;
         } else {
            this.rightArmPose = ArmPose.BLOCK;
         }
      } else if (entity.isUsingItem()) {
         ArmPose rangedPose = this.rangedArmPose(entity);
         if (entity.getUsedItemHand() == InteractionHand.MAIN_HAND ? entity.getMainArm() != HumanoidArm.RIGHT : entity.getMainArm() != HumanoidArm.LEFT) {
            this.leftArmPose = rangedPose;
         } else {
            this.rightArmPose = rangedPose;
         }
      }

      label76: {
         super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
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

      if (entity instanceof WorkerNpcEntity worker && worker.isWorkingAnimation() && entity.getAttackAnim(0.0F) <= 0.01F) {
         float phase = (float)entity.tickCount * 0.35F;
         float swing = Mth.sin(phase) * 0.65F;
         this.rightArm.xRot = -1.05F + swing;
         this.rightArm.yRot = -0.12F;
         this.rightArm.zRot = 0.08F;
      }
   }

   private void applySpellcastingPose(CombatNpcEntity entity, float ageInTicks) {
      this.applySpellcastingPose(ageInTicks);
   }

   private void applySpellcastingPose(float ageInTicks) {
      float pulse = Mth.sin(ageInTicks * 0.18F) * 0.08F;
      this.body.yRot = Mth.sin(ageInTicks * 0.08F) * 0.06F;
      this.rightArm.xRot = -1.28F + pulse;
      this.rightArm.yRot = -0.24F;
      this.rightArm.zRot = 0.16F;
      this.leftArm.xRot = -1.18F - pulse;
      this.leftArm.yRot = 0.24F;
      this.leftArm.zRot = -0.16F;
   }

   private void applyMeleeAttackPose(T entity) {
      float progress = Mth.clamp((12.0F - (float)entity.getMeleeAttackAnimationTicks()) / 11.0F, 0.0F, 1.0F);
      float strike = Mth.sin(progress * (float) Math.PI);
      boolean rightHanded = entity.getMainArm() == HumanoidArm.RIGHT;
      float side = rightHanded ? 1.0F : -1.0F;
      this.body.yRot = -side * strike * 0.32F;
      this.head.yRot = this.head.yRot - this.body.yRot * 0.45F;
      if (rightHanded) {
         this.rightArm.xRot = -0.65F - strike * 1.75F;
         this.rightArm.yRot = -0.55F + strike * 0.85F;
         this.rightArm.zRot = 0.28F - strike * 0.48F;
      } else {
         this.leftArm.xRot = -0.65F - strike * 1.75F;
         this.leftArm.yRot = 0.55F - strike * 0.85F;
         this.leftArm.zRot = -0.28F + strike * 0.48F;
      }
   }

   private void applySwordsmanComboPose(CombatNpcEntity entity) {
      float attack = entity.getAttackAnim(0.0F);
      float cut = Mth.sin(Mth.sqrt(attack) * (float) Math.PI);
      switch (entity.getSwordsmanComboStep()) {
         case 1:
            this.rightArm.xRot = -1.15F - cut * 0.95F;
            this.rightArm.yRot = -0.55F + cut * 0.35F;
            this.rightArm.zRot = 0.18F;
            this.leftArm.xRot = -0.35F;
            this.leftArm.yRot = 0.3F;
            break;
         case 2:
            this.rightArm.xRot = -1.05F - cut * 0.9F;
            this.rightArm.yRot = 0.6F - cut * 0.4F;
            this.rightArm.zRot = -0.2F;
            this.leftArm.xRot = -0.45F;
            this.leftArm.yRot = -0.25F;
            break;
         case 3:
         case 4:
         case 5:
         case 6:
            this.rightArm.xRot = -1.75F + cut * 0.55F;
            this.rightArm.yRot = 0.0F;
            this.rightArm.zRot = 0.0F;
            this.leftArm.xRot = -1.15F + cut * 0.35F;
            this.leftArm.yRot = 0.15F;
            break;
         case 7:
            float side = (entity.tickCount & 1) == 0 ? 1.0F : -1.0F;
            this.rightArm.xRot = -1.4F - cut * 0.75F;
            this.rightArm.yRot = side * (0.85F - cut * 0.35F);
            this.rightArm.zRot = -side * 0.25F;
            this.leftArm.xRot = -0.65F;
            this.leftArm.yRot = -side * 0.35F;
            break;
         case 8:
            this.rightArm.xRot = -1.6F - cut * 0.7F;
            this.rightArm.yRot = -0.85F + cut * 1.35F;
            this.rightArm.zRot = 0.3F;
            this.leftArm.xRot = -0.8F;
            this.leftArm.yRot = 0.4F;
            break;
         case 9:
            this.body.xRot = 0.3F;
            this.rightArm.xRot = -1.7F - cut * 0.45F;
            this.rightArm.yRot = 0.65F;
            this.rightArm.zRot = -0.35F;
            this.leftArm.xRot = -1.1F;
            this.leftArm.yRot = -0.45F;
            break;
         case 10:
            this.rightArm.xRot = -2.55F + cut * 1.15F;
            this.rightArm.yRot = 0.15F;
            this.rightArm.zRot = 0.35F;
            this.leftArm.xRot = -1.1F;
            this.leftArm.yRot = -0.2F;
            break;
         case 11:
         case 12:
            this.body.xRot = -0.2F;
            this.rightArm.xRot = -2.75F + cut * 0.55F;
            this.rightArm.yRot = 0.1F;
            this.leftArm.xRot = -1.65F;
            this.leftArm.yRot = -0.15F;
            break;
         case 13:
            this.body.xRot = -0.1F;
            this.rightArm.xRot = -1.75F - cut * 0.55F;
            this.rightArm.yRot = -0.55F;
            this.rightArm.zRot = 0.2F;
            this.leftArm.xRot = -0.85F;
            this.leftArm.yRot = 0.35F;
            break;
         case 14:
            this.body.xRot = -0.12F;
            this.rightArm.xRot = -1.6F - cut * 0.6F;
            this.rightArm.yRot = 0.6F;
            this.rightArm.zRot = -0.25F;
            this.leftArm.xRot = -0.9F;
            this.leftArm.yRot = -0.3F;
            break;
         case 15:
         case 17:
            this.body.xRot = 0.22F;
            this.rightArm.xRot = -2.65F + cut * 0.75F;
            this.rightArm.yRot = 0.05F;
            this.leftArm.xRot = -1.45F;
            this.leftArm.yRot = 0.1F;
            break;
         case 16:
            this.body.xRot = -0.28F;
            this.rightArm.xRot = -2.45F + cut * 0.45F;
            this.rightArm.yRot = -0.25F;
            this.rightArm.zRot = 0.2F;
            this.leftArm.xRot = -1.5F;
            this.leftArm.yRot = 0.2F;
            break;
         case 18:
            this.body.xRot = 0.55F;
            this.head.xRot += 0.2F;
            this.rightArm.xRot = -2.8F + cut * 0.3F;
            this.rightArm.yRot = 0.05F;
            this.leftArm.xRot = -2.15F;
            this.leftArm.yRot = -0.1F;
            this.rightLeg.xRot = -0.35F;
            this.leftLeg.xRot = 0.35F;
            break;
         case 19:
            this.body.yRot = -0.18F;
            this.rightArm.xRot = -1.85F;
            this.rightArm.yRot = 0.72F;
            this.rightArm.zRot = -0.38F;
            this.leftArm.xRot = -1.15F;
            this.leftArm.yRot = -0.55F;
            this.leftArm.zRot = 0.28F;
            break;
         case 20:
            this.body.yRot = cut * 0.45F;
            this.rightArm.xRot = -2.35F + cut * 1.45F;
            this.rightArm.yRot = -0.75F + cut * 1.25F;
            this.rightArm.zRot = 0.22F;
            this.leftArm.xRot = -1.25F + cut * 0.35F;
            this.leftArm.yRot = 0.35F;
            break;
         case 21:
            this.body.xRot = 0.35F;
            this.rightArm.xRot = -1.45F;
            this.rightArm.yRot = -0.08F;
            this.rightArm.zRot = -0.12F;
            this.leftArm.xRot = -1.25F;
            this.leftArm.yRot = 0.12F;
            break;
         case 22:
            this.body.xRot = -0.12F;
            this.rightArm.xRot = -2.75F + cut * 1.65F;
            this.rightArm.yRot = 0.05F;
            this.leftArm.xRot = -1.7F + cut * 0.65F;
            this.leftArm.yRot = -0.18F;
      }
   }

   private ArmPose rangedArmPose(T entity) {
      if (entity.getUseItem().getItem() instanceof BowItem) {
         return ArmPose.BOW_AND_ARROW;
      } else {
         return entity.getUseItem().getItem() instanceof CrossbowItem ? ArmPose.CROSSBOW_CHARGE : ArmPose.ITEM;
      }
   }
}
