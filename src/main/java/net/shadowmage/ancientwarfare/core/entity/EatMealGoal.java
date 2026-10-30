package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Townsfolk stop for a meal at breakfast, noon and supper, and now and then for a bite between:
 * they stand still with bread, an apple or a baked potato and eat it, crumbs and all.
 */
public class EatMealGoal extends Goal {
   private static final int MEAL_TICKS = 70;
   private final FactionNpcEntity npc;
   private ItemStack held = ItemStack.EMPTY;
   private long lastMeal = -1L;
   private int left;

   public EatMealGoal(FactionNpcEntity npc) {
      this.npc = npc;
      this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
   }

   private long mealTime() {
      long time = this.npc.level().getDayTime() % 24000L;
      return time < 1500L || time >= 5500L && time < 7000L || time >= 11000L && time < 12500L ? this.npc.level().getDayTime() / 6000L : -1L;
   }

   @Override
   public boolean canUse() {
      if (!(this.npc.level() instanceof ServerLevel) || this.npc.getTarget() != null || this.npc.isSleeping() || this.npc.isPassenger()
         || this.npc.getTags().contains("AWBuilder") || this.npc.getLastHurtByMob() != null) {
         return false;
      }

      long meal = this.mealTime();
      if (meal >= 0L && meal != this.lastMeal && this.npc.getRandom().nextInt(400) == 0) {
         this.lastMeal = meal;
         return true;
      }

      return this.npc.getRandom().nextInt(12000) == 0;
   }

   @Override
   public boolean canContinueToUse() {
      return this.left > 0 && this.npc.getTarget() == null && this.npc.getLastHurtByMob() == null;
   }

   @Override
   public void start() {
      this.left = MEAL_TICKS;
      this.held = this.npc.getMainHandItem().copy();
      ItemStack food = switch (this.npc.getRandom().nextInt(4)) {
         case 0 -> new ItemStack(Items.APPLE);
         case 1 -> new ItemStack(Items.BAKED_POTATO);
         case 2 -> new ItemStack(Items.COOKED_COD);
         default -> new ItemStack(Items.BREAD);
      };
      this.npc.getNavigation().stop();
      this.npc.setItemInHand(InteractionHand.MAIN_HAND, food);
      this.npc.startUsingItem(InteractionHand.MAIN_HAND);
   }

   @Override
   public void tick() {
      this.left--;
      this.npc.getNavigation().stop();
      if (this.left % 16 == 0) {
         this.npc.swing(InteractionHand.MAIN_HAND);
      }

      // A meal lasts longer than one bite: the eating starts again until the time is up.
      if (!this.npc.isUsingItem() && this.left > 10) {
         if (this.npc.getMainHandItem().isEmpty()) {
            this.npc.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BREAD));
         }

         this.npc.startUsingItem(InteractionHand.MAIN_HAND);
      }
   }

   @Override
   public void stop() {
      this.left = 0;
      this.npc.stopUsingItem();
      this.npc.setItemInHand(InteractionHand.MAIN_HAND, this.held);
      this.held = ItemStack.EMPTY;
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }
}
