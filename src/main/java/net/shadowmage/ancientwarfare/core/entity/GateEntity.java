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
   private static final EntityDataAccessor<BlockPos> MIN_POS = SynchedEntityData.m_135353_(GateEntity.class, EntityDataSerializers.f_135038_);
   private static final EntityDataAccessor<BlockPos> MAX_POS = SynchedEntityData.m_135353_(GateEntity.class, EntityDataSerializers.f_135038_);
   private static final EntityDataAccessor<Integer> FACING = SynchedEntityData.m_135353_(GateEntity.class, EntityDataSerializers.f_135028_);
   private static final EntityDataAccessor<Float> OPEN_PROGRESS = SynchedEntityData.m_135353_(GateEntity.class, EntityDataSerializers.f_135029_);
   private static final EntityDataAccessor<Integer> STATUS = SynchedEntityData.m_135353_(GateEntity.class, EntityDataSerializers.f_135028_);
   private static final EntityDataAccessor<Integer> MATERIAL = SynchedEntityData.m_135353_(GateEntity.class, EntityDataSerializers.f_135028_);
   private static final EntityDataAccessor<Integer> OPENING_STYLE = SynchedEntityData.m_135353_(GateEntity.class, EntityDataSerializers.f_135028_);
   private static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.m_135353_(GateEntity.class, EntityDataSerializers.f_135029_);
   private static final EntityDataAccessor<Integer> HURT_TIME = SynchedEntityData.m_135353_(GateEntity.class, EntityDataSerializers.f_135028_);
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
      return GateAccessPolicy.canOperate(this.owner, player.m_20148_(), player.m_150110_().f_35937_, player.m_5833_());
   }

   public ItemStack createPackedItem() {
      String id = switch (this.openingStyle()) {
         case SINGLE_SLIDING -> "gate_spawner_single_" + (this.material() == GateEntity.Material.IRON ? "iron" : "wood");
         case DOUBLE_SLIDING -> "gate_spawner_double_" + (this.material() == GateEntity.Material.IRON ? "iron" : "wood");
         case DRAWBRIDGE -> "gate_spawner_drawbridge";
         default -> this.material() == GateEntity.Material.IRON ? "gate_spawner_iron" : "gate_spawner";
      };
      ItemStack stack = new ItemStack(AWCoreRegistryAccess.item(AWCoreIds.id(id)));
      stack.m_41784_().m_128350_("GateHealth", this.health());
      return stack;
   }

   public void restorePackedHealth(float value) {
      if (Float.isFinite(value)) {
         this.f_19804_.m_135381_(HEALTH, Math.max(1.0F, Math.min(value, this.maxHealth())));
      }
   }

   public GateEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.f_19794_ = true;
   }

   public void configure(BlockPos first, BlockPos second, Direction facing) {
      BlockPos min = new BlockPos(
         Math.min(first.m_123341_(), second.m_123341_()), Math.min(first.m_123342_(), second.m_123342_()), Math.min(first.m_123343_(), second.m_123343_())
      );
      BlockPos max = new BlockPos(
         Math.max(first.m_123341_(), second.m_123341_()), Math.max(first.m_123342_(), second.m_123342_()), Math.max(first.m_123343_(), second.m_123343_())
      );
      this.f_19804_.m_135381_(MIN_POS, min);
      this.f_19804_.m_135381_(MAX_POS, max);
      this.f_19804_.m_135381_(FACING, facing.m_122416_());
      this.m_6034_((double)(min.m_123341_() + max.m_123341_() + 1) * 0.5, (double)min.m_123342_(), (double)(min.m_123343_() + max.m_123343_() + 1) * 0.5);
   }

   protected void m_8097_() {
      this.f_19804_.m_135372_(MIN_POS, BlockPos.f_121853_);
      this.f_19804_.m_135372_(MAX_POS, BlockPos.f_121853_);
      this.f_19804_.m_135372_(FACING, Direction.NORTH.m_122416_());
      this.f_19804_.m_135372_(OPEN_PROGRESS, 0.0F);
      this.f_19804_.m_135372_(STATUS, 0);
      this.f_19804_.m_135372_(MATERIAL, GateEntity.Material.WOOD.ordinal());
      this.f_19804_.m_135372_(OPENING_STYLE, GateEntity.OpeningStyle.VERTICAL.ordinal());
      this.f_19804_.m_135372_(HEALTH, 500.0F);
      this.f_19804_.m_135372_(HURT_TIME, 0);
   }

   public void m_8119_() {
      super.m_8119_();
      if (this.m_9236_() instanceof ServerLevel serverLevel && LargeIslandWorldGenerator.isConstructingAt(serverLevel, this.m_20183_())) {
         return;
      }

      this.previousOpenProgress = this.openProgress();
      if (this.hurtCooldown > 0) {
         this.hurtCooldown--;
      }

      if (!this.m_9236_().f_46443_ && this.hurtTime() > 0) {
         this.f_19804_.m_135381_(HURT_TIME, this.hurtTime() - 1);
      }

      this.updateOpenProgress();
      if (!this.m_9236_().f_46443_) {
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
         this.f_19804_.m_135381_(OPEN_PROGRESS, progress);
         if (!this.m_9236_().f_46443_ && progress >= maxProgress) {
            this.f_19804_.m_135381_(STATUS, 2);
         }
      } else if (status == 3) {
         progress = Math.max(0.0F, progress - this.moveSpeed());
         this.f_19804_.m_135381_(OPEN_PROGRESS, progress);
         if (!this.m_9236_().f_46443_ && progress <= 0.0F) {
            this.f_19804_.m_135381_(STATUS, 0);
         }
      }
   }

   private float moveSpeed() {
      return this.openingStyle() == GateEntity.OpeningStyle.DRAWBRIDGE ? 2.0F : 0.08F;
   }

   public boolean m_5830_() {
      return false;
   }

   public boolean m_6673_(DamageSource source) {
      return source.m_276093_(DamageTypes.f_268612_) || source.m_276093_(DamageTypes.f_268613_) || super.m_6673_(source);
   }

   public boolean m_6469_(DamageSource source, float amount) {
      if (!this.m_6084_() || !Float.isFinite(amount) || amount <= 0.0F || this.m_6673_(source)) {
         return false;
      } else if (this.m_9236_().f_46443_) {
         return true;
      } else if (this.hurtCooldown > 0) {
         return false;
      } else {
         this.hurtCooldown = 10;
         float appliedDamage = Math.max(0.0F, amount);
         if (appliedDamage <= 0.0F) {
            return false;
         } else {
            this.f_19804_.m_135381_(HEALTH, Math.max(0.0F, this.health() - appliedDamage));
            this.f_19804_.m_135381_(HURT_TIME, 10);
            this.m_5834_();
            if (this.health() <= 0.0F) {
               this.m_146870_();
            }

            return true;
         }
      }
   }

   public boolean m_6087_() {
      return true;
   }

   public boolean m_6097_() {
      return true;
   }

   protected AABB m_142242_() {
      BlockPos min = this.minPos();
      BlockPos max = this.maxPos();
      return min.equals(BlockPos.f_121853_) && max.equals(BlockPos.f_121853_)
         ? super.m_142242_()
         : new AABB(
            (double)min.m_123341_(),
            (double)min.m_123342_(),
            (double)min.m_123343_(),
            (double)max.m_123341_() + 1.0,
            (double)max.m_123342_() + 1.0,
            (double)max.m_123343_() + 1.0
         );
   }

   public void toggle() {
      if (this.redstonePowered) {
         this.setOpen(true);
      } else {
         int status = this.status();
         this.f_19804_.m_135381_(STATUS, status != 0 && status != 3 ? 3 : 1);
      }
   }

   public void refreshRedstonePower() {
      if (!this.m_9236_().f_46443_) {
         if (this.m_9236_() instanceof ServerLevel serverLevel && LargeIslandWorldGenerator.isConstructingAt(serverLevel, this.m_20183_())) {
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

      for (int x = min.m_123341_(); x <= max.m_123341_(); x++) {
         for (int y = min.m_123342_(); y <= max.m_123342_(); y++) {
            for (int z = min.m_123343_(); z <= max.m_123343_(); z++) {
               BlockPos pos = new BlockPos(x, y, z);
               if (this.m_9236_().m_8055_(pos).m_60713_(AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY)) && this.m_9236_().m_276867_(pos)) {
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
            this.f_19804_.m_135381_(STATUS, 3);
         }
      } else {
         this.f_19804_.m_135381_(STATUS, 1);
      }
   }

   public void finishWorldgenProxySetup(boolean powered) {
      this.initialProxyPassComplete = true;
      this.redstonePowered = powered;
      this.lastProxyOpen = powered;
      this.setOpen(powered);
   }

   public void setMaterial(GateEntity.Material material) {
      this.f_19804_.m_135381_(MATERIAL, material.ordinal());
      this.f_19804_.m_135381_(HEALTH, this.maxHealth());
   }

   public void setOpeningStyle(GateEntity.OpeningStyle openingStyle) {
      this.f_19804_.m_135381_(OPENING_STYLE, openingStyle.ordinal());
   }

   public GateEntity.Material material() {
      int id = (Integer)this.f_19804_.m_135370_(MATERIAL);
      GateEntity.Material[] values = GateEntity.Material.values();
      return id >= 0 && id < values.length ? values[id] : GateEntity.Material.WOOD;
   }

   public float health() {
      return (Float)this.f_19804_.m_135370_(HEALTH);
   }

   public float maxHealth() {
      return this.material() == GateEntity.Material.IRON ? 1000.0F : 500.0F;
   }

   public int hurtTime() {
      return (Integer)this.f_19804_.m_135370_(HURT_TIME);
   }

   public GateEntity.OpeningStyle openingStyle() {
      int id = (Integer)this.f_19804_.m_135370_(OPENING_STYLE);
      GateEntity.OpeningStyle[] values = GateEntity.OpeningStyle.values();
      return id >= 0 && id < values.length ? values[id] : GateEntity.OpeningStyle.VERTICAL;
   }

   public InteractionResult m_6096_(Player player, InteractionHand hand) {
      if (!this.m_9236_().f_46443_) {
         if (!this.m_6084_()) {
            return InteractionResult.FAIL;
         }

         if (!this.canOperate(player)) {
            player.m_5661_(Component.m_237115_("message.ancientwarfare.gate.not_owner"), true);
            return InteractionResult.FAIL;
         }

         if (player.m_6144_() && player.m_21120_(hand).m_41619_()) {
            if (!GateAccessPolicy.canPack(this.owner, player.m_20148_(), player.m_150110_().f_35937_, player.m_5833_())) {
               player.m_5661_(Component.m_237115_("message.ancientwarfare.gate.unowned_pack"), true);
               return InteractionResult.FAIL;
            }

            ItemStack packed = this.createPackedItem();
            if (!player.m_36356_(packed)) {
               player.m_36176_(packed, false);
            }

            this.m_146870_();
            player.m_5661_(Component.m_237115_("message.ancientwarfare.gate.packed"), true);
            return InteractionResult.CONSUME;
         }

         this.toggle();
         player.m_5661_(Component.m_237115_(this.status() == 1 ? "message.ancientwarfare.gate.opening" : "message.ancientwarfare.gate.closing"), true);
      }

      return InteractionResult.m_19078_(this.m_9236_().f_46443_);
   }

   public float openProgress(float partialTick) {
      return this.previousOpenProgress + (this.openProgress() - this.previousOpenProgress) * partialTick;
   }

   public float openProgress() {
      return (Float)this.f_19804_.m_135370_(OPEN_PROGRESS);
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
      return (BlockPos)this.f_19804_.m_135370_(MIN_POS);
   }

   public BlockPos maxPos() {
      return (BlockPos)this.f_19804_.m_135370_(MAX_POS);
   }

   public Direction facing() {
      return Direction.m_122407_((Integer)this.f_19804_.m_135370_(FACING));
   }

   public int widthBlocks() {
      BlockPos min = this.minPos();
      BlockPos max = this.maxPos();
      return Math.max(max.m_123341_() - min.m_123341_() + 1, max.m_123343_() - min.m_123343_() + 1);
   }

   public int heightBlocks() {
      return this.maxPos().m_123342_() - this.minPos().m_123342_() + 1;
   }

   public boolean isXAxisGate() {
      return this.minPos().m_123343_() == this.maxPos().m_123343_();
   }

   private int status() {
      return (Integer)this.f_19804_.m_135370_(STATUS);
   }

   boolean isClosedForSiege() {
      return this.status() == 0 || this.status() == 3;
   }

   public void ensureProxyBlocks() {
      if (this.m_9236_() instanceof ServerLevel) {
         BlockState proxy = (BlockState)AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY)
            .m_49966_()
            .m_61124_(GateProxyBlock.OPEN, this.status() == 1 || this.status() == 2);
         this.forEachGatePos(pos -> {
            BlockState existing = this.m_9236_().m_8055_(pos);
            if (existing.m_60795_() || existing.m_247087_() || existing.m_60713_(AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY))) {
               this.m_9236_().m_7731_(pos, proxy, 3);
               if (this.m_9236_().m_7702_(pos) instanceof GateProxyBlockEntity blockEntity) {
                  blockEntity.setGateUuid(this.m_20148_());
               }
            }
         });
      }
   }

   private void setProxyOpen(boolean open) {
      BlockState proxy = (BlockState)((BlockState)AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY).m_49966_().m_61124_(GateProxyBlock.OPEN, open))
         .m_61124_(GateProxyBlock.BRIDGE, false);
      this.forEachGatePos(pos -> {
         if (this.m_9236_().m_8055_(pos).m_60713_(AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY))) {
            this.m_9236_().m_7731_(pos, proxy, 3);
            if (this.m_9236_().m_7702_(pos) instanceof GateProxyBlockEntity blockEntity) {
               blockEntity.setGateUuid(this.m_20148_());
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
      BlockState proxy = (BlockState)((BlockState)AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY).m_49966_().m_61124_(GateProxyBlock.OPEN, false))
         .m_61124_(GateProxyBlock.BRIDGE, true);
      this.forEachOpenBridgePos(pos -> {
         BlockState existing = this.m_9236_().m_8055_(pos);
         if (existing.m_60795_() || existing.m_247087_() || this.isOwnedProxy(pos)) {
            this.m_9236_().m_7731_(pos, proxy, 3);
            if (this.m_9236_().m_7702_(pos) instanceof GateProxyBlockEntity blockEntity) {
               blockEntity.setGateUuid(this.m_20148_());
            }
         }
      });
   }

   private void removeOpenBridgeProxyBlocks() {
      this.forEachOpenBridgePos(pos -> {
         if (this.isOwnedProxy(pos)) {
            this.m_9236_().m_7471_(pos, false);
         }
      });
   }

   private boolean isOwnedProxy(BlockPos pos) {
      return this.m_9236_().m_8055_(pos).m_60713_(AWCoreRegistryAccess.block(AWCoreIds.GATE_PROXY))
         && this.m_9236_().m_7702_(pos) instanceof GateProxyBlockEntity blockEntity
         && blockEntity.isGate(this.m_20148_());
   }

   public void m_142687_(RemovalReason reason) {
      if (!this.m_9236_().f_46443_ && reason.m_146965_()) {
         this.removeOpenBridgeProxyBlocks();
         this.forEachGatePos(pos -> {
            if (this.isOwnedProxy(pos)) {
               this.m_9236_().m_7471_(pos, false);
            }
         });
      }

      super.m_142687_(reason);
   }

   private void forEachGatePos(Consumer<BlockPos> consumer) {
      BlockPos min = this.minPos();
      BlockPos max = this.maxPos();

      for (int x = min.m_123341_(); x <= max.m_123341_(); x++) {
         for (int y = min.m_123342_(); y <= max.m_123342_(); y++) {
            for (int z = min.m_123343_(); z <= max.m_123343_(); z++) {
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
      return pos.m_123341_() >= min.m_123341_()
         && pos.m_123341_() <= max.m_123341_()
         && pos.m_123342_() >= min.m_123342_()
         && pos.m_123342_() <= max.m_123342_()
         && pos.m_123343_() >= min.m_123343_()
         && pos.m_123343_() <= max.m_123343_();
   }

   private boolean isInsideOpenBridge(BlockPos pos) {
      BlockPos min = this.minPos();
      BlockPos max = this.maxPos();
      Direction bridgeDirection = this.facing().m_122424_();
      int distance = this.isXAxisGate()
         ? (pos.m_123343_() - min.m_123343_()) * bridgeDirection.m_122431_()
         : (pos.m_123341_() - min.m_123341_()) * bridgeDirection.m_122429_();
      if (distance < 1 || distance > this.heightBlocks() || pos.m_123342_() != min.m_123342_()) {
         return false;
      } else {
         return this.isXAxisGate()
            ? pos.m_123341_() >= min.m_123341_() && pos.m_123341_() <= max.m_123341_()
            : pos.m_123343_() >= min.m_123343_() && pos.m_123343_() <= max.m_123343_();
      }
   }

   private void forEachOpenBridgePos(Consumer<BlockPos> consumer) {
      if (this.openingStyle() == GateEntity.OpeningStyle.DRAWBRIDGE) {
         BlockPos min = this.minPos();
         BlockPos max = this.maxPos();
         Direction bridgeDirection = this.facing().m_122424_();
         int y = min.m_123342_();

         for (int distance = 1; distance <= this.heightBlocks(); distance++) {
            int xOffset = bridgeDirection.m_122429_() * distance;
            int zOffset = bridgeDirection.m_122431_() * distance;
            if (this.isXAxisGate()) {
               int z = min.m_123343_() + zOffset;

               for (int x = min.m_123341_(); x <= max.m_123341_(); x++) {
                  consumer.accept(new BlockPos(x, y, z));
               }
            } else {
               int x = min.m_123341_() + xOffset;

               for (int z = min.m_123343_(); z <= max.m_123343_(); z++) {
                  consumer.accept(new BlockPos(x, y, z));
               }
            }
         }
      }
   }

   protected void m_7380_(CompoundTag tag) {
      tag.m_128365_("Min", NbtUtils.m_129224_(this.minPos()));
      tag.m_128365_("Max", NbtUtils.m_129224_(this.maxPos()));
      tag.m_128405_("Facing", (Integer)this.f_19804_.m_135370_(FACING));
      tag.m_128350_("OpenProgress", this.openProgress());
      tag.m_128405_("Status", this.status());
      tag.m_128405_("Material", this.material().ordinal());
      tag.m_128405_("OpeningStyle", this.openingStyle().ordinal());
      tag.m_128379_("RedstonePowered", this.redstonePowered);
      tag.m_128379_("InitialProxyPassComplete", this.initialProxyPassComplete);
      tag.m_128379_("LastProxyOpen", this.lastProxyOpen);
      tag.m_128350_("Health", this.health());
      if (this.owner != null) {
         tag.m_128362_("Owner", this.owner);
      }
   }

   protected void m_7378_(CompoundTag tag) {
      this.f_19804_.m_135381_(MIN_POS, NbtUtils.m_129239_(tag.m_128469_("Min")));
      this.f_19804_.m_135381_(MAX_POS, NbtUtils.m_129239_(tag.m_128469_("Max")));
      this.f_19804_.m_135381_(FACING, tag.m_128451_("Facing"));
      this.f_19804_.m_135381_(OPEN_PROGRESS, tag.m_128457_("OpenProgress"));
      this.f_19804_.m_135381_(STATUS, tag.m_128451_("Status"));
      this.f_19804_.m_135381_(MATERIAL, tag.m_128441_("Material") ? tag.m_128451_("Material") : GateEntity.Material.WOOD.ordinal());
      this.f_19804_.m_135381_(OPENING_STYLE, tag.m_128441_("OpeningStyle") ? tag.m_128451_("OpeningStyle") : GateEntity.OpeningStyle.VERTICAL.ordinal());
      this.f_19804_.m_135381_(HEALTH, tag.m_128441_("Health") ? Math.min(tag.m_128457_("Health"), this.maxHealth()) : this.maxHealth());
      this.redstonePowered = tag.m_128471_("RedstonePowered");
      this.initialProxyPassComplete = tag.m_128471_("InitialProxyPassComplete");
      this.lastProxyOpen = tag.m_128471_("LastProxyOpen");
      this.owner = tag.m_128403_("Owner") ? tag.m_128342_("Owner") : null;
   }

   public Packet<ClientGamePacketListener> m_5654_() {
      return AWCorePlatform.createEntitySpawnPacket(this);
   }

   public boolean m_6783_(double distance) {
      return distance < 4096.0;
   }

   public AABB m_6921_() {
      BlockPos min = this.minPos();
      BlockPos max = this.maxPos();
      return new AABB(
         (double)min.m_123341_(),
         (double)min.m_123342_(),
         (double)min.m_123343_(),
         (double)max.m_123341_() + 1.0,
         (double)(max.m_123342_() + this.heightBlocks()) + 2.0,
         (double)max.m_123343_() + 1.0
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
