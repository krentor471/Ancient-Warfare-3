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
   private static final EntityDataAccessor<Integer> MATERIAL_LEVEL = SynchedEntityData.defineId(ChestPushcartEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> VEHICLE_HEALTH = SynchedEntityData.defineId(ChestPushcartEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> HURT_FLASH_TICKS = SynchedEntityData.defineId(ChestPushcartEntity.class, EntityDataSerializers.INT);
   private static final Component MENU_TITLE = Component.translatable("container.ancientwarfare.chest_pushcart");
   private static final RawAnimation WHEEL_ROLL = RawAnimation.begin().thenLoop("animation.ancientwarfare.chest_pushcart.wheel_roll");
   private static final RawAnimation HITCH_RAISE = RawAnimation.begin().thenPlayAndHold("animation.ancientwarfare.chest_pushcart.hitch_raise");
   private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
   private final NonNullList<ItemStack> storageItems = NonNullList.withSize(216, ItemStack.EMPTY);
   private final NonNullList<ItemStack> upgradeItems = NonNullList.withSize(6, ItemStack.EMPTY);
   private boolean packing;
   protected float inputForward;
   protected float inputTurn;
   public float wheelRotation;
   public float wheelRotationPrev;

   public ChestPushcartEntity(EntityType<? extends ChestPushcartEntity> type, Level level) {
      super(type, level);
      this.blocksBuilding = true;
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(new AnimationController[]{new AnimationController(this, "wheel", 0, this::wheelAnimation)});
      controllers.add(new AnimationController[]{new AnimationController(this, "hitch", 0, this::hitchAnimation)});
   }

   private PlayState wheelAnimation(AnimationState<ChestPushcartEntity> state) {
      if (this.getDeltaMovement().horizontalDistanceSqr() > 4.0E-4) {
         state.setAnimation(WHEEL_ROLL);
         return PlayState.CONTINUE;
      } else {
         return PlayState.STOP;
      }
   }

   private PlayState hitchAnimation(AnimationState<ChestPushcartEntity> state) {
      if (this.getDeltaMovement().horizontalDistanceSqr() <= 4.0E-4) {
         state.setAnimation(HITCH_RAISE);
         return PlayState.CONTINUE;
      } else {
         return PlayState.STOP;
      }
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.animationCache;
   }

   protected void defineSynchedData() {
      this.entityData.define(MATERIAL_LEVEL, 0);
      this.entityData.define(VEHICLE_HEALTH, this.maxHealthForLevel(0));
      this.entityData.define(HURT_FLASH_TICKS, 0);
   }

   public int getMaterialLevel() {
      return Math.max(0, Math.min(4, (Integer)this.entityData.get(MATERIAL_LEVEL)));
   }

   public void setMaterialLevel(int level) {
      int oldLevel = this.getMaterialLevel();
      float oldMaxHealth = this.maxHealthForLevel(oldLevel);
      float oldHealth = this.getHealth();
      this.entityData.set(MATERIAL_LEVEL, Math.max(0, Math.min(4, level)));
      if (oldHealth >= oldMaxHealth) {
         this.setHealth(this.getMaxHealth());
      } else {
         this.setHealth(oldHealth);
      }
   }

   public float getHealth() {
      return (Float)this.entityData.get(VEHICLE_HEALTH);
   }

   public float getMaxHealth() {
      return this.maxHealthForLevel(this.getMaterialLevel());
   }

   public void setHealth(float health) {
      this.entityData.set(VEHICLE_HEALTH, Math.max(0.0F, Math.min(this.getMaxHealth(), health)));
   }

   public float getArmor() {
      return this.armorForLevel(this.getMaterialLevel());
   }

   public int getHurtFlashTicks() {
      return (Integer)this.entityData.get(HURT_FLASH_TICKS);
   }

   protected void setHurtFlashTicks(int ticks) {
      this.entityData.set(HURT_FLASH_TICKS, Math.max(0, ticks));
   }

   public void tick() {
      super.tick();
      if (this.getHurtFlashTicks() > 0) {
         this.setHurtFlashTicks(this.getHurtFlashTicks() - 1);
      }

      this.wheelRotationPrev = this.wheelRotation;
      this.updateVehicleMotion();
   }

   protected void updateVehicleMotion() {
      LivingEntity rider = this.getControllingPassenger();
      if (this.isVehicle() && rider != null) {
         float forward = this.inputForward;
         float turn = this.inputTurn;
         if (Math.abs(turn) > 1.0E-4F) {
            this.setYRot(this.getYRot() - turn * 4.0F);
            this.setYHeadRot(this.getYRot());
            this.setYBodyRot(this.getYRot());
         }

         this.yRotO = this.getYRot();
         if (Math.abs(forward) > 1.0E-4F) {
            double speed = forward > 0.0F ? 0.185 : 0.08;
            double yaw = Math.toRadians((double)this.getYRot());
            double motion = (double)forward * speed;
            this.setDeltaMovement(-Math.sin(yaw) * motion, this.getDeltaMovement().y, Math.cos(yaw) * motion);
            this.hasImpulse = true;
         } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.65, 1.0, 0.65));
         }
      } else {
         this.setDeltaMovement(this.getDeltaMovement().multiply(0.85, 1.0, 0.85));
      }

      if (!this.isNoGravity()) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.08, 0.0));
      }

      double startX = this.getX();
      double startZ = this.getZ();
      this.move(MoverType.SELF, this.getDeltaMovement());
      this.setDeltaMovement(this.getDeltaMovement().multiply(0.91, this.onGround() ? 0.0 : 0.98, 0.91));
      double yaw = Math.toRadians((double)this.getYRot());
      double forwardDistance = (this.getX() - startX) * -Math.sin(yaw) + (this.getZ() - startZ) * Math.cos(yaw);
      this.wheelRotation += (float)(forwardDistance * 110.0);
   }

   protected void positionRider(Entity passenger, MoveFunction moveFunction) {
      if (this.hasPassenger(passenger)) {
         Vec3 offset = this.getPassengerOffset().yRot(-this.getYRot() * (float) (Math.PI / 180.0));
         moveFunction.accept(passenger, this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);
      }
   }

   public void setVehicleInput(float forward, float turn) {
      this.inputForward = Math.max(-1.0F, Math.min(1.0F, forward));
      this.inputTurn = Math.max(-1.0F, Math.min(1.0F, turn));
   }

   public LivingEntity getControllingPassenger() {
      return this.getFirstPassenger() instanceof LivingEntity living ? living : null;
   }

   public boolean hurt(DamageSource source, float amount) {
      if (this.isInvulnerableTo(source) || this.isRemoved() || this.packing) {
         return false;
      } else if (this.level().isClientSide) {
         return true;
      } else {
         float adjustedDamage = source.is(DamageTypeTags.BYPASSES_ARMOR)
            ? amount
            : this.applyVehicleArmorReductions(source, getDamageAfterArmor(amount, this.getArmor(), 0.0F));
         this.setHealth(this.getHealth() - adjustedDamage);
         this.setHurtFlashTicks(8);
         this.markHurt();
         if (this.getHealth() <= 0.0F) {
            this.ejectPassengers();
            this.remove(RemovalReason.KILLED);
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
            if (source.is(DamageTypeTags.IS_FIRE)) {
               armorReduction = Math.max(armorReduction, armor.fireReduction());
            }

            if (source.is(DamageTypeTags.IS_EXPLOSION)) {
               armorReduction = Math.max(armorReduction, armor.explosiveReduction());
            }

            reduction += armorReduction;
         }
      }

      return damage * (1.0F - Math.min(reduction, 80.0F) / 100.0F);
   }

   public InteractionResult interact(Player player, InteractionHand hand) {
      if (!this.level().isClientSide) {
         if (player.isShiftKeyDown()) {
            AWCorePlatform.openMenu(
               player,
               new SimpleMenuProvider(
                  (containerId, playerInventory, openingPlayer) -> new ChestPushcartMenu(containerId, playerInventory, this), this.getMenuTitle()
               ),
               buffer -> buffer.writeInt(this.getId())
            );
         } else {
            player.startRiding(this);
         }
      }

      return InteractionResult.sidedSuccess(this.level().isClientSide);
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
      stack.getOrCreateTag().put("ChestPushcartData", tag);
      return stack;
   }

   public boolean packIntoItem(Player player) {
      if (!this.level().isClientSide && this.isAlive()) {
         ItemStack stack = this.createPackedItem();
         boolean accepted = player.addItem(stack);
         if (!accepted) {
            this.spawnAtLocation(stack);
         }

         if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.containerMenu.broadcastChanges();
            serverPlayer.inventoryMenu.broadcastChanges();
            serverPlayer.closeContainer();
         }

         this.packing = true;
         this.discard();
         return true;
      } else {
         return false;
      }
   }

   public void loadCartData(CompoundTag tag) {
      this.setMaterialLevel(tag.getInt("MaterialLevel"));
      this.setHealth(tag.contains("Health") ? tag.getFloat("Health") : this.getMaxHealth());
      this.storageItems.clear();
      this.upgradeItems.clear();
      if (tag.contains("Storage")) {
         ContainerHelper.loadAllItems(tag.getCompound("Storage"), this.storageItems);
      } else {
         ContainerHelper.loadAllItems(tag, this.storageItems);
      }

      if (tag.contains("Upgrades")) {
         ContainerHelper.loadAllItems(tag.getCompound("Upgrades"), this.upgradeItems);
      }
   }

   public void saveCartData(CompoundTag tag) {
      tag.putInt("MaterialLevel", this.getMaterialLevel());
      tag.putFloat("Health", this.getHealth());
      CompoundTag storageTag = new CompoundTag();
      ContainerHelper.saveAllItems(storageTag, this.storageItems);
      tag.put("Storage", storageTag);
      CompoundTag upgradeTag = new CompoundTag();
      ContainerHelper.saveAllItems(upgradeTag, this.upgradeItems);
      tag.put("Upgrades", upgradeTag);
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.loadCartData(tag);
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      this.saveCartData(tag);
   }

   public void remove(RemovalReason reason) {
      if (!this.packing && !this.level().isClientSide && reason.shouldDestroy()) {
         Containers.dropContents(this.level(), this, this);
         Containers.dropContents(this.level(), this, this.upgrades());
      }

      super.remove(reason);
   }

   public boolean isPickable() {
      return true;
   }

   public int getContainerSize() {
      return this.storageItems.size();
   }

   public boolean isEmpty() {
      for (ItemStack stack : this.storageItems) {
         if (!stack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public ItemStack getItem(int slot) {
      return this.isValidSlot(slot) ? (ItemStack)this.storageItems.get(slot) : ItemStack.EMPTY;
   }

   public ItemStack removeItem(int slot, int amount) {
      ItemStack removed = this.isValidSlot(slot) ? ContainerHelper.removeItem(this.storageItems, slot, amount) : ItemStack.EMPTY;
      if (!removed.isEmpty()) {
         this.setChanged();
      }

      return removed;
   }

   public ItemStack removeItemNoUpdate(int slot) {
      return this.isValidSlot(slot) ? ContainerHelper.takeItem(this.storageItems, slot) : ItemStack.EMPTY;
   }

   public void setItem(int slot, ItemStack stack) {
      if (this.isValidSlot(slot)) {
         this.storageItems.set(slot, stack);
         if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
         }

         this.setChanged();
      }
   }

   public void setChanged() {
   }

   public boolean stillValid(Player player) {
      return this.isAlive() && player.distanceToSqr(this) < 64.0;
   }

   public void clearContent() {
      this.storageItems.clear();
      this.setChanged();
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

   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return AWCorePlatform.createEntitySpawnPacket(this);
   }

   private static final class ListBackedContainer implements Container {
      private final NonNullList<ItemStack> items;

      private ListBackedContainer(NonNullList<ItemStack> items) {
         this.items = items;
      }

      public int getContainerSize() {
         return this.items.size();
      }

      public boolean isEmpty() {
         for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
               return false;
            }
         }

         return true;
      }

      public ItemStack getItem(int slot) {
         return slot >= 0 && slot < this.items.size() ? (ItemStack)this.items.get(slot) : ItemStack.EMPTY;
      }

      public ItemStack removeItem(int slot, int amount) {
         return slot >= 0 && slot < this.items.size() ? ContainerHelper.removeItem(this.items, slot, amount) : ItemStack.EMPTY;
      }

      public ItemStack removeItemNoUpdate(int slot) {
         return slot >= 0 && slot < this.items.size() ? ContainerHelper.takeItem(this.items, slot) : ItemStack.EMPTY;
      }

      public void setItem(int slot, ItemStack stack) {
         if (slot >= 0 && slot < this.items.size()) {
            this.items.set(slot, stack);
            if (stack.getCount() > this.getMaxStackSize()) {
               stack.setCount(this.getMaxStackSize());
            }
         }
      }

      public void setChanged() {
      }

      public boolean stillValid(Player player) {
         return true;
      }

      public void clearContent() {
         this.items.clear();
      }
   }
}
