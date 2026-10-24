package net.shadowmage.ancientwarfare.core.tile;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.block.HandCrankedGeneratorBlock;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueAccess;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueCell;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueStats;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTier;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class HandCrankedGeneratorBlockEntity extends BlockEntity implements TorqueAccess, GeoBlockEntity {
   private static final String INPUT_TORQUE_TAG = "InputTorque";
   private static final String OUTPUT_TORQUE_TAG = "OutputTorque";
   private static final String CRANK_ACTIVE_TICKS_TAG = "CrankActiveTicks";
   private static final int LOGIC_INTERVAL_TICKS = 1;
   private static final double PLAYER_CRANK_TORQUE = 100.0;
   private static final double BALANCE_PER_TICK = 5.0;
   private static final int CRANK_ACTIVE_TICKS = 18;
   private final TorqueCell inputCell = new TorqueCell(100.0, 0.0, 100.0, TorqueTier.LIGHT.getEfficiency());
   private final TorqueCell outputCell = new TorqueCell(0.0, 100.0, 100.0, TorqueTier.LIGHT.getEfficiency());
   private double previousOutputTorque;
   private int crankActiveTicks;
   private int logicCooldown;
   private TorqueStats stats = TorqueStats.EMPTY;
   private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

   public HandCrankedGeneratorBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.id("hand_cranked_generator")), pos, state);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, HandCrankedGeneratorBlockEntity blockEntity) {
      blockEntity.tickServer();
   }

   public double crankByPlayer() {
      if (this.level != null && !this.level.isClientSide) {
         double accepted = this.inputCell.addTorque(100.0);
         if (accepted > 0.0) {
            this.crankActiveTicks = 18;
            this.setChanged();
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
         }

         return accepted;
      } else {
         return 0.0;
      }
   }

   public TorqueStats getStats() {
      return this.stats.withStored(this.getTorqueStored(null));
   }

   public int getCrankActiveTicks() {
      return this.crankActiveTicks;
   }

   public void registerControllers(ControllerRegistrar controllers) {
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.animationCache;
   }

   private void tickServer() {
      boolean wasCranking = this.crankActiveTicks > 0;
      if (this.crankActiveTicks > 0) {
         this.crankActiveTicks--;
      }

      if (this.logicCooldown++ >= 0) {
         this.logicCooldown = 0;
         double inputBefore = this.outputCell.getTorque();
         this.balancePower();
         double generated = Math.max(0.0, this.outputCell.getTorque() - inputBefore);
         double output = this.transferPowerTo(this.getFacing());
         double loss = 0.0;
         this.stats = new TorqueStats(this.getTorqueStored(null), generated, output, loss);
         this.previousOutputTorque = this.outputCell.getTorque();
         if (generated > 0.0 || output > 0.0 || this.inputCell.getTorque() > 0.0 || this.outputCell.getTorque() > 0.0) {
            this.crankActiveTicks = 18;
         }

         if (generated > 0.0 || output > 0.0 || loss > 0.0 || this.previousOutputTorque != this.outputCell.getTorque()) {
            this.setChanged();
         }

         if (wasCranking && this.crankActiveTicks <= 0 && this.level != null) {
            this.setChanged();
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
         }
      }
   }

   private void balancePower() {
      double transfer = Math.min(5.0, this.outputCell.getMaxTorque() - this.outputCell.getTorque());
      transfer = Math.min(transfer, this.inputCell.getTorque());
      if (!(transfer <= 0.0)) {
         this.inputCell.setTorque(this.inputCell.getTorque() - transfer);
         this.outputCell.setTorque(this.outputCell.getTorque() + transfer);
      }
   }

   private double transferPowerTo(Direction side) {
      if (this.level != null && this.canOutputTorque(side)) {
         if (this.level.getBlockEntity(this.worldPosition.relative(side)) instanceof TorqueAccess target) {
            Direction targetSide = side.getOpposite();
            if (!target.canInputTorque(targetSide)) {
               return 0.0;
            } else {
               double available = Math.min(this.getTorqueStored(side), this.getMaxTorqueOutput(side));
               double accepted = available > 0.0 ? target.addTorque(targetSide, available) : 0.0;
               return accepted > 0.0 ? this.drainTorque(side, accepted) : 0.0;
            }
         } else {
            return 0.0;
         }
      } else {
         return 0.0;
      }
   }

   @Override
   public double getMaxTorque(@Nullable Direction side) {
      return this.inputCell.getMaxTorque() + this.outputCell.getMaxTorque();
   }

   @Override
   public double getTorqueStored(@Nullable Direction side) {
      return this.inputCell.getTorque() + this.outputCell.getTorque();
   }

   @Override
   public double addTorque(@Nullable Direction side, double amount) {
      return 0.0;
   }

   @Override
   public double drainTorque(@Nullable Direction side, double amount) {
      return side != null && this.canOutputTorque(side) ? this.outputCell.drainTorque(amount) : 0.0;
   }

   @Override
   public double getMaxTorqueOutput(Direction side) {
      return this.canOutputTorque(side) ? this.outputCell.getMaxTickOutput() : 0.0;
   }

   @Override
   public double getMaxTorqueInput(@Nullable Direction side) {
      return 0.0;
   }

   @Override
   public boolean canOutputTorque(Direction side) {
      return side == this.getFacing();
   }

   @Override
   public boolean canInputTorque(@Nullable Direction side) {
      return false;
   }

   private Direction getFacing() {
      return this.getBlockState().hasProperty(HandCrankedGeneratorBlock.FACING)
         ? (Direction)this.getBlockState().getValue(HandCrankedGeneratorBlock.FACING)
         : Direction.NORTH;
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putDouble("InputTorque", this.inputCell.getTorque());
      tag.putDouble("OutputTorque", this.outputCell.getTorque());
      tag.putInt("CrankActiveTicks", this.crankActiveTicks);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.inputCell.setTorque(tag.getDouble("InputTorque"));
      this.outputCell.setTorque(tag.getDouble("OutputTorque"));
      this.crankActiveTicks = tag.getInt("CrankActiveTicks");
      this.previousOutputTorque = this.outputCell.getTorque();
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag() {
      CompoundTag tag = super.getUpdateTag();
      this.saveAdditional(tag);
      return tag;
   }

   public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
      CompoundTag tag = packet.getTag();
      if (tag != null) {
         this.load(tag);
      }
   }
}
