package net.shadowmage.ancientwarfare.core.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.shadowmage.ancientwarfare.core.entity.NpcBaseEntity;
import net.shadowmage.ancientwarfare.core.entity.WorkerNpcEntity;

public class NpcHumanoidModel<T extends NpcBaseEntity> extends HumanoidModel<T> {
   public NpcHumanoidModel(ModelPart root) {
      super(root);
   }

   public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
      this.riding = entity.isPassenger();
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

      super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
      if (entity.getMeleeAttackAnimationTicks() > 0) {
         this.applyMeleeAttackPose(entity);
      }

      if (entity instanceof WorkerNpcEntity worker && worker.isWorkingAnimation() && entity.getAttackAnim(0.0F) <= 0.01F) {
         float phase = (float)entity.tickCount * 0.35F;
         float swing = Mth.sin(phase) * 0.65F;
         this.rightArm.xRot = -1.05F + swing;
         this.rightArm.yRot = -0.12F;
         this.rightArm.zRot = 0.08F;
      }
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

   private ArmPose rangedArmPose(T entity) {
      if (entity.getUseItem().getItem() instanceof BowItem) {
         return ArmPose.BOW_AND_ARROW;
      } else {
         return entity.getUseItem().getItem() instanceof CrossbowItem ? ArmPose.CROSSBOW_CHARGE : ArmPose.ITEM;
      }
   }
}
