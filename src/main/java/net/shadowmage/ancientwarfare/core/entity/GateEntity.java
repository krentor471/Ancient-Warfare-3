package net.shadowmage.ancientwarfare.core.entity;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.block.GateProxyBlock;
import net.shadowmage.ancientwarfare.core.common.gate.GateAccessPolicy;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.structure.LargeIslandWorldGenerator;
import net.shadowmage.ancientwarfare.core.tile.GateProxyBlockEntity;

public class GateEntity extends Entity {
   private static final EntityDataAccessor<BlockPos> MIN_POS = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.BLOCK_POS);
   private static final EntityDataAccessor<BlockPos> MAX_POS = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.BLOCK_POS);
   private static final EntityDataAccessor<Integer> FACING = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> OPEN_PROGRESS = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> STATUS = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> MATERIAL = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> OPENING_STYLE = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> HURT_TIME = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.INT);
   private static final int CLOSED = 0;
   private static final int OPENING = 1;
   private static final int OPEN = 2;
   private static final int CLOSING = 3;
   private static final float MOVE_SPEED = 0.08F;
   private static final float DRAWBRIDGE_MOVE_SPEED = 2.0F;
   private static final int HURT_COOLDOWN_TICKS = 10;
   private static final float WOOD_MAX_HEALTH = 500.0F;
   private static final float IRON_MAX_HEALTH = 1000.0F;
   private boolean lastProxyOpen;
   private int hurtCooldown;
   private float previousOpenProgress;
   private boolean bridgeProxyActive;
   private boolean redstonePowered;
   private boolean initialProxyPassComplete;
   private UUID owner;

   public void setOwner(UUID owner) {
      this.owner = owner;
   }

   public UUID getOwnerUUID() {
      return this.owner;
   }

   public boolean canOperate(Player player) {
      return GateAccessPolicy.canOperate(this.owner, player.getUUID(), player.getAbilities().instabuild, player.isSpectator());
   }

   public ItemStack createPackedItem() {
      String id = switch (this.openingStyle()) {
         case SINGLE_SLIDING -> "gate_spawner_single_" + (this.material() == GateEntity.Material.IRON ? "iron" : "wood");
         case DOUBLE_SLIDING -> "gate_spawner_double_" + (this.material() == GateEntity.Material.IRON ? "iron" : "wood");
         case DRAWBRIDGE -> "gate_spawner_drawbridge";
         default -> this.material() == GateEntity.Material.IRON ? "gate_spawner_iron" : "gate_spawner";
      };
      ItemStack stack = new ItemStack(AWCoreRegistryAccess.item(AWCoreIds.id(id)));
      stack.getOrCreateTag().putFloat("GateHealth", this.health());
      return stack;
   }

   public void restorePackedHealth(float value) {
      if (Float.isFinite(value)) {
         this.entityData.set(HEALTH, Math.max(1.0F, Math.min(value, this.maxHealth())));
      }
   }

   public GateEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noPhysics = true;
   }

   public void configure(BlockPos first, BlockPos second, Direction facing) {
      BlockPos min = new BlockPos(
         Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ())
      );
      BlockPos max = new BlockPos(
         Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ())
      );
      this.entityData.set(MIN_POS, min);
      this.entityData.set(MAX_POS, max);
      this.entityData.set(FACING, facing.get2DDataValue());
      this.setPos((double)(min.getX() + max.getX() + 1) * 0.5, (double)min.getY(), (double)(min.getZ() + max.getZ() + 1) * 0.5);
   }

   protected void defineSynchedData() {
      this.entityData.define(MIN_POS, BlockPos.ZERO);
      this.entityData.define(MAX_POS, BlockPos.ZERO);
      this.entityData.define(FACING, Direction.NORTH.get2DDataValue());
      this.entityData.define(OPEN_PROGRESS, 0.0F);
      this.entityData.define(STATUS, 0);
      this.entityData.define(MATERIAL, GateEntity.Material.WOOD.ordinal());
      this.entityData.define(OPENING_STYLE, GateEntity.OpeningStyle.VERTICAL.ordinal());
      this.entityData.define(HEALTH, 500.0F);
      this.entityData.define(HURT_TIME, 0);
   }

   public void tick() {
      super.tick();
      if (this.level() instanceof ServerLevel serverLevel && LargeIslandWorldGenerator.isConstructingAt(serverLevel, this.blockPosition())) {
         return;
      }

      this.previousOpenProgress = this.openProgress();
      if (this.hurtCooldown > 0) {
         this.hurtCooldown--;
      }

      if (!this.level().isClientSide && this.hurtTime() > 0) {
         this.entityData.set(HURT_TIME, this.hurtTime() - 1);
      }

      this.updateOpenProgress();
      if (!this.level().isClientSide) {
         boolean proxyOpen = this.status() == 1 || this.status() == 2;
         if (proxyOpen != this.lastProxyOpen) {
            this.setProxyOpen(proxyOpen);
            this.lastProxyOpen = proxyOpen;
         }

         if (!this.initialProxyPassComplete) {
            this.ensureProxyBlocks();
            this.refreshRedstonePower();
            this.initialProxyPassComplete = true;
         }

         this.updateDrawbridgeProxyBlocks();
      }
   }

   private void updateOpenProgress() {
      int status = this.status();
      float progress = this.openProgress();
      float maxProgress = this.maxOpenProgress();
      if (status == 1) {
         progress = Math.min(maxProgress, progress + this.moveSpeed());
         this.entityData.set(OPEN_PROGRESS, progress);
         if (!this.level().isClientSide && progress >= maxProgress) {
            this.entityData.set(STATUS, 2);
         }
      } else if (status == 3) {
         progress = Math.max(0.0F, progress - this.moveSpeed());
         this.entityData.set(OPEN_PROGRESS, progress);
         if (!this.level().isClientSide && progress <= 0.0F) {
            this.entityData.set(STATUS, 0);
         }
      }
   }

   private float moveSpeed() {
      return this.openingStyle() == GateEntity.OpeningStyle.DRAWBRIDGE ? 2.0F : 0.08F;
   }

   public boolean isInWall() {
      return false;
   }

   public boolean isInvulnerableTo(DamageSource source) {
      return source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING) || super.isInvulnerableTo(source);
   }

   public boolean hurt(DamageSource source, float amount) {
      if (!this.isAlive() || !Float.isFinite(amount) || amount <= 0.0F || this.isInvulnerableTo(source)) {
         return false;
      } else if (this.level().isClientSide) {
         return true;
      } else if (this.hurtCooldown > 0) {
         return false;
      } else {
         this.hurtCooldown = 10;
         float appliedDamage = Math.max(0.0F, amount);
         if (appliedDamage <= 0.0F) {
            return false;
         } else {
            this.entityData.set(HEALTH, Math.max(0.0F, this.health() - appliedDamage));
            this.entityData.set(HURT_TIME, 10);
            this.markHurt();
            if (this.health() <= 0.0F) {
               this.discard();
            }

            return true;
         }
      }
   }

   public boolean isPickable() {
      return true;
   }

   public boolean isAttackable() {
      return true;
   }

   protected AABB makeBoundingBox() {
      BlockPos min = this.minPos();
      BlockPos max = this.maxPos();
      return min.equals(BlockPos.ZERO) && max.equals(BlockPos.ZERO)
         ? super.makeBoundingBox()
         : new AABB(
            (double)min.getX(),
            (double)min.getY(),
            (double)min.getZ(),
            (double)max.getX() + 1.0,
            (double)max.getY() + 1.0,
            (double)max.getZ() + 1.0
         );
   }

   public void toggle() {
      if (this.redstonePowered) {
         this.setOpen(true);
      } else {
         int status = this.status();
         this.entityData.set(STATUS, status != 0 && status != 3 ? 3 : 1);
      }
   }

   public void refreshRedstonePower() {
      if (!this.level().isClientSide) {
         if (this.level() instanceof ServerLevel serverLevel && LargeIslandWorldGenerator.isConstructingAt(serverLevel, this.blockPosition())) {
            return;
         }

         boolean powered = this.hasRedstonePower();
         if (powered != this.redstonePowered) {
            this.redstonePowered = powered;
            this.setOpen(powered);
         }
      }
   }

   private boolean hasRedstonePower() {
      BlockPos min = this.minPos();
      BlockPos max = this.maxPos();

      for (int x = min.getX(); x <= max.getX(); x++) {
         for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
               BlockPos pos = new BlockPos(x, y, z);
               if (this.level().getBlockState(pos).is(AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY)) && this.level().hasNeighborSignal(pos)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private void setOpen(boolean open) {
      int currentStatus = this.status();
      if (!open || currentStatus != 0 && currentStatus != 3) {
         if (!open && (currentStatus == 2 || currentStatus == 1)) {
            this.entityData.set(STATUS, 3);
         }
      } else {
         this.entityData.set(STATUS, 1);
      }
   }

   public void finishWorldgenProxySetup(boolean powered) {
      this.initialProxyPassComplete = true;
      this.redstonePowered = powered;
      this.lastProxyOpen = powered;
      this.setOpen(powered);
   }

   public void setMaterial(GateEntity.Material material) {
      this.entityData.set(MATERIAL, material.ordinal());
      this.entityData.set(HEALTH, this.maxHealth());
   }

   public void setOpeningStyle(GateEntity.OpeningStyle openingStyle) {
      this.entityData.set(OPENING_STYLE, openingStyle.ordinal());
   }

   public GateEntity.Material material() {
      int id = (Integer)this.entityData.get(MATERIAL);
      GateEntity.Material[] values = GateEntity.Material.values();
      return id >= 0 && id < values.length ? values[id] : GateEntity.Material.WOOD;
   }

   public float health() {
      return (Float)this.entityData.get(HEALTH);
   }

   public float maxHealth() {
      return this.material() == GateEntity.Material.IRON ? 1000.0F : 500.0F;
   }

   public int hurtTime() {
      return (Integer)this.entityData.get(HURT_TIME);
   }

   public GateEntity.OpeningStyle openingStyle() {
      int id = (Integer)this.entityData.get(OPENING_STYLE);
      GateEntity.OpeningStyle[] values = GateEntity.OpeningStyle.values();
      return id >= 0 && id < values.length ? values[id] : GateEntity.OpeningStyle.VERTICAL;
   }

   public InteractionResult interact(Player player, InteractionHand hand) {
      if (!this.level().isClientSide) {
         if (!this.isAlive()) {
            return InteractionResult.FAIL;
         }

         if (!this.canOperate(player)) {
            player.displayClientMessage(Component.translatable("message.ancientwarfare.gate.not_owner"), true);
            return InteractionResult.FAIL;
         }

         if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
            if (!GateAccessPolicy.canPack(this.owner, player.getUUID(), player.getAbilities().instabuild, player.isSpectator())) {
               player.displayClientMessage(Component.translatable("message.ancientwarfare.gate.unowned_pack"), true);
               return InteractionResult.FAIL;
            }

            ItemStack packed = this.createPackedItem();
            if (!player.addItem(packed)) {
               player.drop(packed, false);
            }

            this.discard();
            player.displayClientMessage(Component.translatable("message.ancientwarfare.gate.packed"), true);
            return InteractionResult.CONSUME;
         }

         this.toggle();
         player.displayClientMessage(Component.translatable(this.status() == 1 ? "message.ancientwarfare.gate.opening" : "message.ancientwarfare.gate.closing"), true);
      }

      return InteractionResult.sidedSuccess(this.level().isClientSide);
   }

   public float openProgress(float partialTick) {
      return this.previousOpenProgress + (this.openProgress() - this.previousOpenProgress) * partialTick;
   }

   public float openProgress() {
      return (Float)this.entityData.get(OPEN_PROGRESS);
   }

   public float maxOpenProgress() {
      if (this.openingStyle() == GateEntity.OpeningStyle.DOUBLE_SLIDING) {
         return (float)this.widthBlocks() * 0.5F + 0.25F;
      } else if (this.openingStyle() == GateEntity.OpeningStyle.SINGLE_SLIDING) {
         return (float)this.widthBlocks() + 0.25F;
      } else {
         return this.openingStyle() == GateEntity.OpeningStyle.DRAWBRIDGE ? 90.0F : (float)this.heightBlocks() + 0.25F;
      }
   }

   public BlockPos minPos() {
      return (BlockPos)this.entityData.get(MIN_POS);
   }

   public BlockPos maxPos() {
      return (BlockPos)this.entityData.get(MAX_POS);
   }

   public Direction facing() {
      return Direction.from2DDataValue((Integer)this.entityData.get(FACING));
   }

   public int widthBlocks() {
      BlockPos min = this.minPos();
      BlockPos max = this.maxPos();
      return Math.max(max.getX() - min.getX() + 1, max.getZ() - min.getZ() + 1);
   }

   public int heightBlocks() {
      return this.maxPos().getY() - this.minPos().getY() + 1;
   }

   public boolean isXAxisGate() {
      return this.minPos().getZ() == this.maxPos().getZ();
   }

   private int status() {
      return (Integer)this.entityData.get(STATUS);
   }

   boolean isClosedForSiege() {
      return this.status() == 0 || this.status() == 3;
   }

   public void ensureProxyBlocks() {
      if (this.level() instanceof ServerLevel) {
         BlockState proxy = (BlockState)AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY)
            .defaultBlockState()
            .setValue(GateProxyBlock.OPEN, this.status() == 1 || this.status() == 2);
         this.forEachGatePos(pos -> {
            BlockState existing = this.level().getBlockState(pos);
            if (existing.isAir() || existing.canBeReplaced() || existing.is(AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY))) {
               this.level().setBlock(pos, proxy, 3);
               if (this.level().getBlockEntity(pos) instanceof GateProxyBlockEntity blockEntity) {
                  blockEntity.setGateUuid(this.getUUID());
               }
            }
         });
      }
   }

   private void setProxyOpen(boolean open) {
      BlockState proxy = (BlockState)((BlockState)AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY).defaultBlockState().setValue(GateProxyBlock.OPEN, open))
         .setValue(GateProxyBlock.BRIDGE, false);
      this.forEachGatePos(pos -> {
         if (this.level().getBlockState(pos).is(AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY))) {
            this.level().setBlock(pos, proxy, 3);
            if (this.level().getBlockEntity(pos) instanceof GateProxyBlockEntity blockEntity) {
               blockEntity.setGateUuid(this.getUUID());
            }
         }
      });
   }

   private void updateDrawbridgeProxyBlocks() {
      if (this.openingStyle() != GateEntity.OpeningStyle.DRAWBRIDGE) {
         if (this.bridgeProxyActive) {
            this.removeOpenBridgeProxyBlocks();
            this.bridgeProxyActive = false;
         }
      } else {
         if (this.status() == 2) {
            this.ensureOpenBridgeProxyBlocks();
            this.bridgeProxyActive = true;
         } else {
            this.removeOpenBridgeProxyBlocks();
            this.bridgeProxyActive = false;
         }
      }
   }

   private void ensureOpenBridgeProxyBlocks() {
      BlockState proxy = (BlockState)((BlockState)AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY).defaultBlockState().setValue(GateProxyBlock.OPEN, false))
         .setValue(GateProxyBlock.BRIDGE, true);
      this.forEachOpenBridgePos(pos -> {
         BlockState existing = this.level().getBlockState(pos);
         if (existing.isAir() || existing.canBeReplaced() || this.isOwnedProxy(pos)) {
            this.level().setBlock(pos, proxy, 3);
            if (this.level().getBlockEntity(pos) instanceof GateProxyBlockEntity blockEntity) {
               blockEntity.setGateUuid(this.getUUID());
            }
         }
      });
   }

   private void removeOpenBridgeProxyBlocks() {
      this.forEachOpenBridgePos(pos -> {
         if (this.isOwnedProxy(pos)) {
            this.level().removeBlock(pos, false);
         }
      });
   }

   private boolean isOwnedProxy(BlockPos pos) {
      return this.level().getBlockState(pos).is(AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY))
         && this.level().getBlockEntity(pos) instanceof GateProxyBlockEntity blockEntity
         && blockEntity.isGate(this.getUUID());
   }

   public void remove(RemovalReason reason) {
      if (!this.level().isClientSide && reason.shouldDestroy()) {
         this.removeOpenBridgeProxyBlocks();
         this.forEachGatePos(pos -> {
            if (this.isOwnedProxy(pos)) {
               this.level().removeBlock(pos, false);
            }
         });
      }

      super.remove(reason);
   }

   private void forEachGatePos(Consumer<BlockPos> consumer) {
      BlockPos min = this.minPos();
      BlockPos max = this.maxPos();

      for (int x = min.getX(); x <= max.getX(); x++) {
         for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
               consumer.accept(new BlockPos(x, y, z));
            }
         }
      }
   }

   public boolean ownsProxyPosition(BlockPos pos) {
      return this.isInsideGatePlane(pos) || this.openingStyle() == GateEntity.OpeningStyle.DRAWBRIDGE && this.isInsideOpenBridge(pos);
   }

   private boolean isInsideGatePlane(BlockPos pos) {
      BlockPos min = this.minPos();
      BlockPos max = this.maxPos();
      return pos.getX() >= min.getX()
         && pos.getX() <= max.getX()
         && pos.getY() >= min.getY()
         && pos.getY() <= max.getY()
         && pos.getZ() >= min.getZ()
         && pos.getZ() <= max.getZ();
   }

   private boolean isInsideOpenBridge(BlockPos pos) {
      BlockPos min = this.minPos();
      BlockPos max = this.maxPos();
      Direction bridgeDirection = this.facing().getOpposite();
      int distance = this.isXAxisGate()
         ? (pos.getZ() - min.getZ()) * bridgeDirection.getStepZ()
         : (pos.getX() - min.getX()) * bridgeDirection.getStepX();
      if (distance < 1 || distance > this.heightBlocks() || pos.getY() != min.getY()) {
         return false;
      } else {
         return this.isXAxisGate()
            ? pos.getX() >= min.getX() && pos.getX() <= max.getX()
            : pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
      }
   }

   private void forEachOpenBridgePos(Consumer<BlockPos> consumer) {
      if (this.openingStyle() == GateEntity.OpeningStyle.DRAWBRIDGE) {
         BlockPos min = this.minPos();
         BlockPos max = this.maxPos();
         Direction bridgeDirection = this.facing().getOpposite();
         int y = min.getY();

         for (int distance = 1; distance <= this.heightBlocks(); distance++) {
            int xOffset = bridgeDirection.getStepX() * distance;
            int zOffset = bridgeDirection.getStepZ() * distance;
            if (this.isXAxisGate()) {
               int z = min.getZ() + zOffset;

               for (int x = min.getX(); x <= max.getX(); x++) {
                  consumer.accept(new BlockPos(x, y, z));
               }
            } else {
               int x = min.getX() + xOffset;

               for (int z = min.getZ(); z <= max.getZ(); z++) {
                  consumer.accept(new BlockPos(x, y, z));
               }
            }
         }
      }
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.put("Min", NbtUtils.writeBlockPos(this.minPos()));
      tag.put("Max", NbtUtils.writeBlockPos(this.maxPos()));
      tag.putInt("Facing", (Integer)this.entityData.get(FACING));
      tag.putFloat("OpenProgress", this.openProgress());
      tag.putInt("Status", this.status());
      tag.putInt("Material", this.material().ordinal());
      tag.putInt("OpeningStyle", this.openingStyle().ordinal());
      tag.putBoolean("RedstonePowered", this.redstonePowered);
      tag.putBoolean("InitialProxyPassComplete", this.initialProxyPassComplete);
      tag.putBoolean("LastProxyOpen", this.lastProxyOpen);
      tag.putFloat("Health", this.health());
      if (this.owner != null) {
         tag.putUUID("Owner", this.owner);
      }
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.entityData.set(MIN_POS, NbtUtils.readBlockPos(tag.getCompound("Min")));
      this.entityData.set(MAX_POS, NbtUtils.readBlockPos(tag.getCompound("Max")));
      this.entityData.set(FACING, tag.getInt("Facing"));
      this.entityData.set(OPEN_PROGRESS, tag.getFloat("OpenProgress"));
      this.entityData.set(STATUS, tag.getInt("Status"));
      this.entityData.set(MATERIAL, tag.contains("Material") ? tag.getInt("Material") : GateEntity.Material.WOOD.ordinal());
      this.entityData.set(OPENING_STYLE, tag.contains("OpeningStyle") ? tag.getInt("OpeningStyle") : GateEntity.OpeningStyle.VERTICAL.ordinal());
      this.entityData.set(HEALTH, tag.contains("Health") ? Math.min(tag.getFloat("Health"), this.maxHealth()) : this.maxHealth());
      this.redstonePowered = tag.getBoolean("RedstonePowered");
      this.initialProxyPassComplete = tag.getBoolean("InitialProxyPassComplete");
      this.lastProxyOpen = tag.getBoolean("LastProxyOpen");
      this.owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
   }

   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return AWCorePlatform.createEntitySpawnPacket(this);
   }

   public boolean shouldRenderAtSqrDistance(double distance) {
      return distance < 4096.0;
   }

   public AABB getBoundingBoxForCulling() {
      BlockPos min = this.minPos();
      BlockPos max = this.maxPos();
      return new AABB(
         (double)min.getX(),
         (double)min.getY(),
         (double)min.getZ(),
         (double)max.getX() + 1.0,
         (double)(max.getY() + this.heightBlocks()) + 2.0,
         (double)max.getZ() + 1.0
      );
   }

   public static enum Material {
      WOOD,
      IRON;
   }

   public static enum OpeningStyle {
      VERTICAL,
      SINGLE_SLIDING,
      DOUBLE_SLIDING,
      DRAWBRIDGE;
   }
}
