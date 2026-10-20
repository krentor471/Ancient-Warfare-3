package net.shadowmage.ancientwarfare.core.entity;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.item.ChestPushcartItem;
import net.shadowmage.ancientwarfare.core.item.VehicleArmorItem;
import net.shadowmage.ancientwarfare.core.menu.ChestPushcartMenu;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ChestPushcartEntity extends Entity implements GeoEntity, Container {
   public static final int VARIANT_COUNT = 5;
   public static final int STORAGE_SLOT_COUNT = 216;
   public static final int UPGRADE_SLOT_COUNT = 6;
   private static final EntityDataAccessor<Integer> MATERIAL_LEVEL = SynchedEntityData.m_135353_(ChestPushcartEntity.class, EntityDataSerializers.f_135028_);
   private static final EntityDataAccessor<Float> VEHICLE_HEALTH = SynchedEntityData.m_135353_(ChestPushcartEntity.class, EntityDataSerializers.f_135029_);
   private static final EntityDataAccessor<Integer> HURT_FLASH_TICKS = SynchedEntityData.m_135353_(ChestPushcartEntity.class, EntityDataSerializers.f_135028_);
   private static final Component MENU_TITLE = Component.m_237115_("container.ancientwarfare.chest_pushcart");
   private static final RawAnimation WHEEL_ROLL = RawAnimation.begin().thenLoop("animation.ancientwarfare.chest_pushcart.wheel_roll");
   private static final RawAnimation HITCH_RAISE = RawAnimation.begin().thenPlayAndHold("animation.ancientwarfare.chest_pushcart.hitch_raise");
   private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
   private final NonNullList<ItemStack> storageItems = NonNullList.m_122780_(216, ItemStack.f_41583_);
   private final NonNullList<ItemStack> upgradeItems = NonNullList.m_122780_(6, ItemStack.f_41583_);
   private boolean packing;
   protected float inputForward;
   protected float inputTurn;
   public float wheelRotation;
   public float wheelRotationPrev;

   public ChestPushcartEntity(EntityType<? extends ChestPushcartEntity> type, Level level) {
      super(type, level);
      this.f_19850_ = true;
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController[]{new AnimationController(this, "wheel", 0, this::wheelAnimation)});
      controllers.add(new AnimationController[]{new AnimationController(this, "hitch", 0, this::hitchAnimation)});
   }

   private PlayState wheelAnimation(AnimationState<ChestPushcartEntity> state) {
      if (this.m_20184_().m_165925_() > 4.0E-4) {
         state.setAnimation(WHEEL_ROLL);
         return PlayState.CONTINUE;
      } else {
         return PlayState.STOP;
      }
   }

   private PlayState hitchAnimation(AnimationState<ChestPushcartEntity> state) {
      if (this.m_20184_().m_165925_() <= 4.0E-4) {
         state.setAnimation(HITCH_RAISE);
         return PlayState.CONTINUE;
      } else {
         return PlayState.STOP;
      }
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.animationCache;
   }

   protected void m_8097_() {
      this.f_19804_.m_135372_(MATERIAL_LEVEL, 0);
      this.f_19804_.m_135372_(VEHICLE_HEALTH, this.maxHealthForLevel(0));
      this.f_19804_.m_135372_(HURT_FLASH_TICKS, 0);
   }

   public int getMaterialLevel() {
      return Math.max(0, Math.min(4, (Integer)this.f_19804_.m_135370_(MATERIAL_LEVEL)));
   }

   public void setMaterialLevel(int level) {
      int oldLevel = this.getMaterialLevel();
      float oldMaxHealth = this.maxHealthForLevel(oldLevel);
      float oldHealth = this.getHealth();
      this.f_19804_.m_135381_(MATERIAL_LEVEL, Math.max(0, Math.min(4, level)));
      if (oldHealth >= oldMaxHealth) {
         this.setHealth(this.getMaxHealth());
      } else {
         this.setHealth(oldHealth);
      }
   }

   public float getHealth() {
      return (Float)this.f_19804_.m_135370_(VEHICLE_HEALTH);
   }

   public float getMaxHealth() {
      return this.maxHealthForLevel(this.getMaterialLevel());
   }

   public void setHealth(float health) {
      this.f_19804_.m_135381_(VEHICLE_HEALTH, Math.max(0.0F, Math.min(this.getMaxHealth(), health)));
   }

   public float getArmor() {
      return this.armorForLevel(this.getMaterialLevel());
   }

   public int getHurtFlashTicks() {
      return (Integer)this.f_19804_.m_135370_(HURT_FLASH_TICKS);
   }

   protected void setHurtFlashTicks(int ticks) {
      this.f_19804_.m_135381_(HURT_FLASH_TICKS, Math.max(0, ticks));
   }

   public void m_8119_() {
      super.m_8119_();
      if (this.getHurtFlashTicks() > 0) {
         this.setHurtFlashTicks(this.getHurtFlashTicks() - 1);
      }

      this.wheelRotationPrev = this.wheelRotation;
      this.updateVehicleMotion();
   }

   protected void updateVehicleMotion() {
      LivingEntity rider = this.m_6688_();
      if (this.m_20160_() && rider != null) {
         float forward = this.inputForward;
         float turn = this.inputTurn;
         if (Math.abs(turn) > 1.0E-4F) {
            this.m_146922_(this.m_146908_() - turn * 4.0F);
            this.m_5616_(this.m_146908_());
            this.m_5618_(this.m_146908_());
         }

         this.f_19859_ = this.m_146908_();
         if (Math.abs(forward) > 1.0E-4F) {
            double speed = forward > 0.0F ? 0.185 : 0.08;
            double yaw = Math.toRadians((double)this.m_146908_());
            double motion = (double)forward * speed;
            this.m_20334_(-Math.sin(yaw) * motion, this.m_20184_().f_82480_, Math.cos(yaw) * motion);
            this.f_19812_ = true;
         } else {
            this.m_20256_(this.m_20184_().m_82542_(0.65, 1.0, 0.65));
         }
      } else {
         this.m_20256_(this.m_20184_().m_82542_(0.85, 1.0, 0.85));
      }

      if (!this.m_20068_()) {
         this.m_20256_(this.m_20184_().m_82520_(0.0, -0.08, 0.0));
      }

      double startX = this.m_20185_();
      double startZ = this.m_20189_();
      this.m_6478_(MoverType.SELF, this.m_20184_());
      this.m_20256_(this.m_20184_().m_82542_(0.91, this.m_20096_() ? 0.0 : 0.98, 0.91));
      double yaw = Math.toRadians((double)this.m_146908_());
      double forwardDistance = (this.m_20185_() - startX) * -Math.sin(yaw) + (this.m_20189_() - startZ) * Math.cos(yaw);
      this.wheelRotation += (float)(forwardDistance * 110.0);
   }

   protected void m_19956_(Entity passenger, MoveFunction moveFunction) {
      if (this.m_20363_(passenger)) {
         Vec3 offset = this.getPassengerOffset().m_82524_(-this.m_146908_() * (float) (Math.PI / 180.0));
         moveFunction.m_20372_(passenger, this.m_20185_() + offset.f_82479_, this.m_20186_() + offset.f_82480_, this.m_20189_() + offset.f_82481_);
      }
   }

   public void setVehicleInput(float forward, float turn) {
      this.inputForward = Math.max(-1.0F, Math.min(1.0F, forward));
      this.inputTurn = Math.max(-1.0F, Math.min(1.0F, turn));
   }

   public LivingEntity m_6688_() {
      return this.m_146895_() instanceof LivingEntity living ? living : null;
   }

   public boolean m_6469_(DamageSource source, float amount) {
      if (this.m_6673_(source) || this.m_213877_() || this.packing) {
         return false;
      } else if (this.m_9236_().f_46443_) {
         return true;
      } else {
         float adjustedDamage = source.m_269533_(DamageTypeTags.f_268490_)
            ? amount
            : this.applyVehicleArmorReductions(source, getDamageAfterArmor(amount, this.getArmor(), 0.0F));
         this.setHealth(this.getHealth() - adjustedDamage);
         this.setHurtFlashTicks(8);
         this.m_5834_();
         if (this.getHealth() <= 0.0F) {
            this.m_20153_();
            this.m_142687_(RemovalReason.KILLED);
         }

         return true;
      }
   }

   private float applyVehicleArmorReductions(DamageSource source, float damage) {
      float reduction = 0.0F;

      for (ItemStack stack : this.upgradeItems) {
         VehicleArmorItem.Type armor = VehicleArmorItem.fromStack(stack);
         if (armor != null) {
            float armorReduction = armor.generalReduction();
            if (source.m_269533_(DamageTypeTags.f_268745_)) {
               armorReduction = Math.max(armorReduction, armor.fireReduction());
            }

            if (source.m_269533_(DamageTypeTags.f_268415_)) {
               armorReduction = Math.max(armorReduction, armor.explosiveReduction());
            }

            reduction += armorReduction;
         }
      }

      return damage * (1.0F - Math.min(reduction, 80.0F) / 100.0F);
   }

   public InteractionResult m_6096_(Player player, InteractionHand hand) {
      if (!this.m_9236_().f_46443_) {
         if (player.m_6144_()) {
            AWCorePlatform.openMenu(
               player,
               new SimpleMenuProvider(
                  (containerId, playerInventory, openingPlayer) -> new ChestPushcartMenu(containerId, playerInventory, this), this.getMenuTitle()
               ),
               buffer -> buffer.writeInt(this.m_19879_())
            );
         } else {
            player.m_20329_(this);
         }
      }

      return InteractionResult.m_19078_(this.m_9236_().f_46443_);
   }

   public Container upgrades() {
      return new ChestPushcartEntity.ListBackedContainer(this.upgradeItems);
   }

   protected Component getMenuTitle() {
      return MENU_TITLE;
   }

   protected Vec3 getPassengerOffset() {
      return new Vec3(0.0, 0.35, 1.75);
   }

   protected ItemStack createPackedItem() {
      ItemStack stack = new ItemStack(ChestPushcartItem.itemForMaterialLevel(this.getMaterialLevel()));
      CompoundTag tag = new CompoundTag();
      this.saveCartData(tag);
      stack.m_41784_().m_128365_("ChestPushcartData", tag);
      return stack;
   }

   public boolean packIntoItem(Player player) {
      if (!this.m_9236_().f_46443_ && this.m_6084_()) {
         ItemStack stack = this.createPackedItem();
         boolean accepted = player.m_36356_(stack);
         if (!accepted) {
            this.m_19983_(stack);
         }

         if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.f_36096_.m_38946_();
            serverPlayer.f_36095_.m_38946_();
            serverPlayer.m_6915_();
         }

         this.packing = true;
         this.m_146870_();
         return true;
      } else {
         return false;
      }
   }

   public void loadCartData(CompoundTag tag) {
      this.setMaterialLevel(tag.m_128451_("MaterialLevel"));
      this.setHealth(tag.m_128441_("Health") ? tag.m_128457_("Health") : this.getMaxHealth());
      this.storageItems.clear();
      this.upgradeItems.clear();
      if (tag.m_128441_("Storage")) {
         ContainerHelper.m_18980_(tag.m_128469_("Storage"), this.storageItems);
      } else {
         ContainerHelper.m_18980_(tag, this.storageItems);
      }

      if (tag.m_128441_("Upgrades")) {
         ContainerHelper.m_18980_(tag.m_128469_("Upgrades"), this.upgradeItems);
      }
   }

   public void saveCartData(CompoundTag tag) {
      tag.m_128405_("MaterialLevel", this.getMaterialLevel());
      tag.m_128350_("Health", this.getHealth());
      CompoundTag storageTag = new CompoundTag();
      ContainerHelper.m_18973_(storageTag, this.storageItems);
      tag.m_128365_("Storage", storageTag);
      CompoundTag upgradeTag = new CompoundTag();
      ContainerHelper.m_18973_(upgradeTag, this.upgradeItems);
      tag.m_128365_("Upgrades", upgradeTag);
   }

   protected void m_7378_(CompoundTag tag) {
      this.loadCartData(tag);
   }

   protected void m_7380_(CompoundTag tag) {
      this.saveCartData(tag);
   }

   public void m_142687_(RemovalReason reason) {
      if (!this.packing && !this.m_9236_().f_46443_ && reason.m_146965_()) {
         Containers.m_18998_(this.m_9236_(), this, this);
         Containers.m_18998_(this.m_9236_(), this, this.upgrades());
      }

      super.m_142687_(reason);
   }

   public boolean m_6087_() {
      return true;
   }

   public int m_6643_() {
      return this.storageItems.size();
   }

   public boolean m_7983_() {
      for (ItemStack stack : this.storageItems) {
         if (!stack.m_41619_()) {
            return false;
         }
      }

      return true;
   }

   public ItemStack m_8020_(int slot) {
      return this.isValidSlot(slot) ? (ItemStack)this.storageItems.get(slot) : ItemStack.f_41583_;
   }

   public ItemStack m_7407_(int slot, int amount) {
      ItemStack removed = this.isValidSlot(slot) ? ContainerHelper.m_18969_(this.storageItems, slot, amount) : ItemStack.f_41583_;
      if (!removed.m_41619_()) {
         this.m_6596_();
      }

      return removed;
   }

   public ItemStack m_8016_(int slot) {
      return this.isValidSlot(slot) ? ContainerHelper.m_18966_(this.storageItems, slot) : ItemStack.f_41583_;
   }

   public void m_6836_(int slot, ItemStack stack) {
      if (this.isValidSlot(slot)) {
         this.storageItems.set(slot, stack);
         if (stack.m_41613_() > this.m_6893_()) {
            stack.m_41764_(this.m_6893_());
         }

         this.m_6596_();
      }
   }

   public void m_6596_() {
   }

   public boolean m_6542_(Player player) {
      return this.m_6084_() && player.m_20280_(this) < 64.0;
   }

   public void m_6211_() {
      this.storageItems.clear();
      this.m_6596_();
   }

   private boolean isValidSlot(int slot) {
      return slot >= 0 && slot < this.storageItems.size();
   }

   protected float maxHealthForLevel(int level) {
      return VehicleDurability.cargoHealth(level);
   }

   protected float armorForLevel(int level) {
      return VehicleDurability.cargoArmor(level);
   }

   private static float getDamageAfterArmor(float damage, float armor, float toughness) {
      float armorReduction = Math.min(20.0F, Math.max(armor / 5.0F, armor - damage / (2.0F + toughness / 4.0F)));
      return damage * (1.0F - armorReduction / 25.0F);
   }

   public Packet<ClientGamePacketListener> m_5654_() {
      return AWCorePlatform.createEntitySpawnPacket(this);
   }

   private static final class ListBackedContainer implements Container {
      private final NonNullList<ItemStack> items;

      private ListBackedContainer(NonNullList<ItemStack> items) {
         this.items = items;
      }

      public int m_6643_() {
         return this.items.size();
      }

      public boolean m_7983_() {
         for (ItemStack stack : this.items) {
            if (!stack.m_41619_()) {
               return false;
            }
         }

         return true;
      }

      public ItemStack m_8020_(int slot) {
         return slot >= 0 && slot < this.items.size() ? (ItemStack)this.items.get(slot) : ItemStack.f_41583_;
      }

      public ItemStack m_7407_(int slot, int amount) {
         return slot >= 0 && slot < this.items.size() ? ContainerHelper.m_18969_(this.items, slot, amount) : ItemStack.f_41583_;
      }

      public ItemStack m_8016_(int slot) {
         return slot >= 0 && slot < this.items.size() ? ContainerHelper.m_18966_(this.items, slot) : ItemStack.f_41583_;
      }

      public void m_6836_(int slot, ItemStack stack) {
         if (slot >= 0 && slot < this.items.size()) {
            this.items.set(slot, stack);
            if (stack.m_41613_() > this.m_6893_()) {
               stack.m_41764_(this.m_6893_());
            }
         }
      }

      public void m_6596_() {
      }

      public boolean m_6542_(Player player) {
         return true;
      }

      public void m_6211_() {
         this.items.clear();
      }
   }
}
