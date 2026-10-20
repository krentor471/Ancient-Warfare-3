package net.shadowmage.ancientwarfare.core.tile;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.block.FlywheelControllerBlock;
import net.shadowmage.ancientwarfare.core.block.FlywheelStorageBlock;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueAccess;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueCell;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueDemandNode;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueStats;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTier;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class FlywheelControllerBlockEntity extends BlockEntity implements TorqueAccess, TorqueDemandNode, GeoBlockEntity {
   private static final String INPUT_TORQUE_TAG = "InputTorque";
   private static final String OUTPUT_TORQUE_TAG = "OutputTorque";
   private static final String VISUAL_ACTIVE_TAG = "VisualActive";
   private static final int MAX_HEIGHT = 21;
   private final TorqueCell inputCell;
   private final TorqueCell outputCell;
   private TorqueStats stats = TorqueStats.EMPTY;
   private double storageStored;
   private double storageCapacity;
   private boolean visualActive;
   private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

   public FlywheelControllerBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.id("flywheel_controller")), pos, state);
      TorqueTier tier = getTierFromState(state);
      double transferLimit = tier.getTransferLimit();
      this.inputCell = new TorqueCell(transferLimit, transferLimit, transferLimit, tier.getEfficiency());
      this.outputCell = new TorqueCell(transferLimit, transferLimit, transferLimit, tier.getEfficiency());
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, FlywheelControllerBlockEntity blockEntity) {
      blockEntity.tickServer();
   }

   public TorqueStats getStats() {
      return this.stats.withStored(this.getTorqueStored(null));
   }

   public double getStorageStored() {
      return this.storageStored;
   }

   public double getStorageCapacity() {
      return this.storageCapacity;
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(
         new AnimationController[]{
            new AnimationController(
               this,
               "flywheel_rotation",
               0,
               state -> this.visualActive
                     ? state.setAndContinue(RawAnimation.begin().thenLoop("animation.ancientwarfare.flywheel_controller.run"))
                     : PlayState.STOP
            )
         }
      );
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.animationCache;
   }

   public boolean isVisualActive() {
      return this.visualActive;
   }

   private void tickServer() {
      FlywheelControllerBlockEntity.FlywheelStorageView storage = this.findStorage();
      double beforeInput = this.inputCell.getTorque();
      double beforeOutput = this.outputCell.getTorque();
      double loss = 0.0;
      boolean outputDisabled = this.f_58857_ != null && this.f_58857_.m_276867_(this.f_58858_);
      double downstreamDemand = this.f_58857_ != null && !outputDisabled
         ? TorqueNetworkDemand.downstreamDemand(this.f_58857_, this.f_58858_, this.getFacing())
         : 0.0;
      this.balancePower(storage, downstreamDemand);
      double output = outputDisabled ? 0.0 : this.transferPowerTo(this.getFacing());
      double input = Math.max(0.0, this.inputCell.getTorque() + this.outputCell.getTorque() - beforeInput - beforeOutput);
      this.storageStored = storage == null ? 0.0 : storage.getStored();
      this.storageCapacity = storage == null ? 0.0 : storage.capacity();
      this.stats = new TorqueStats(this.getTorqueStored(null), input, output, loss);
      boolean nextVisualActive = this.inputCell.getTorque() > 0.0
         || this.outputCell.getTorque() > 0.0
         || this.storageStored > 0.0
         || beforeInput > 0.0
         || output > 0.0;
      if (this.visualActive != nextVisualActive) {
         this.visualActive = nextVisualActive;
         this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 3);
      }

      if (input > 0.0 || output > 0.0 || loss > 0.0) {
         this.m_6596_();
      }
   }

   private void balancePower(@Nullable FlywheelControllerBlockEntity.FlywheelStorageView storage, double downstreamDemand) {
      double input = this.inputCell.getTorque();
      double output = this.outputCell.getTorque();
      double desiredOutput = Math.min(this.outputCell.getMaxTorque(), Math.max(0.0, downstreamDemand));
      if (storage != null) {
         double returnToStorage = Math.min(Math.max(0.0, output - desiredOutput), storage.capacity() - storage.getStored());
         if (returnToStorage > 0.0) {
            output -= returnToStorage;
            storage.setStored(storage.getStored() + returnToStorage);
         }

         double toStorage = Math.min(input, storage.capacity() - storage.getStored());
         if (toStorage > 0.0) {
            input -= toStorage;
            storage.setStored(storage.getStored() + toStorage);
         }
      } else if (output > desiredOutput) {
         double returnToInput = Math.min(output - desiredOutput, this.inputCell.getMaxTorque() - input);
         output -= returnToInput;
         input += returnToInput;
      }

      double neededOutput = Math.max(0.0, desiredOutput - output);
      double fromInput = Math.min(input, neededOutput);
      input -= fromInput;
      output += fromInput;
      neededOutput -= fromInput;
      if (storage != null && neededOutput > 0.0) {
         double fromStorage = Math.min(storage.getStored(), neededOutput);
         storage.setStored(storage.getStored() - fromStorage);
         output += fromStorage;
      }

      this.inputCell.setTorque(input);
      this.outputCell.setTorque(output);
   }

   @Nullable
   private FlywheelControllerBlockEntity.FlywheelStorageView findStorage() {
      if (this.f_58857_ == null) {
         return null;
      } else {
         BlockPos topCenter = this.f_58858_.m_7495_();
         TorqueTier tier = this.getTier();
         if (!this.isStorage(topCenter, tier)) {
            return null;
         } else {
            int size = this.isFullLayer(topCenter, tier, 3) ? 3 : 1;
            int height = 0;

            for (int i = 0; i < 21; i++) {
               BlockPos layerCenter = topCenter.m_6625_(i);
               if (!this.isFullLayer(layerCenter, tier, size)) {
                  break;
               }

               height++;
            }

            if (height <= 0) {
               return null;
            } else {
               BlockPos controlPos = topCenter.m_6625_(height - 1);
               return this.f_58857_.m_7702_(controlPos) instanceof FlywheelStorageBlockEntity storage
                  ? new FlywheelControllerBlockEntity.FlywheelStorageView(storage, tier, size, height)
                  : null;
            }
         }
      }
   }

   private boolean isFullLayer(BlockPos center, TorqueTier tier, int size) {
      if (this.f_58857_ == null) {
         return false;
      } else {
         int radius = size == 3 ? 1 : 0;

         for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
               if (!this.isStorage(center.m_7918_(x, 0, z), tier)) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   private boolean isStorage(BlockPos pos, TorqueTier tier) {
      return this.f_58857_ != null && this.f_58857_.m_8055_(pos).m_60734_() instanceof FlywheelStorageBlock block && block.getTier() == tier;
   }

   private double transferPowerTo(Direction side) {
      if (this.f_58857_ != null && this.canOutputTorque(side)) {
         if (this.f_58857_.m_7702_(this.f_58858_.m_121945_(side)) instanceof TorqueAccess target) {
            Direction targetSide = side.m_122424_();
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
      TorqueCell cell = this.getCell(side);
      return cell == null ? 0.0 : cell.getMaxTorque();
   }

   @Override
   public double getTorqueStored(@Nullable Direction side) {
      if (side == null) {
         return this.inputCell.getTorque() + this.outputCell.getTorque() + this.storageStored;
      } else {
         TorqueCell cell = this.getCell(side);
         return cell == null ? 0.0 : cell.getTorque();
      }
   }

   @Override
   public double addTorque(@Nullable Direction side, double amount) {
      TorqueCell cell = this.getCell(side);
      return cell == this.inputCell ? cell.addTorque(Math.min(amount, this.getMaxTorqueInput(side))) : 0.0;
   }

   @Override
   public double drainTorque(@Nullable Direction side, double amount) {
      TorqueCell cell = this.getCell(side);
      return cell == this.outputCell ? cell.drainTorque(amount) : 0.0;
   }

   @Override
   public double getMaxTorqueOutput(Direction side) {
      return this.canOutputTorque(side) ? this.outputCell.getMaxTickOutput() : 0.0;
   }

   @Override
   public double getMaxTorqueInput(@Nullable Direction side) {
      if (!this.canInputTorque(side)) {
         return 0.0;
      } else {
         Set<BlockPos> visited = new HashSet<>();
         visited.add(this.f_58858_);
         return this.getInputDemand(side, visited, 0);
      }
   }

   double getInputDemand(Direction side, Set<BlockPos> visited, int depth) {
      if (this.f_58857_ != null && this.canInputTorque(side)) {
         FlywheelControllerBlockEntity.FlywheelStorageView storage = this.findStorage();
         double storageSpace = storage == null ? 0.0 : storage.capacity() - storage.getStored();
         double downstreamDemand = this.f_58857_.m_276867_(this.f_58858_)
            ? 0.0
            : TorqueNetworkDemand.downstreamDemand(this.f_58857_, this.f_58858_.m_121945_(this.getFacing()), this.getFacing().m_122424_(), visited, depth + 1);
         double passThroughNeed = Math.max(0.0, downstreamDemand - this.outputCell.getTorque());
         return Math.min(this.inputCell.getMaxTickInput(), storageSpace + passThroughNeed);
      } else {
         return 0.0;
      }
   }

   @Override
   public double getNetworkTorqueDemand(Direction inputSide, Set<BlockPos> visited, int depth) {
      return this.getInputDemand(inputSide, visited, depth);
   }

   @Override
   public boolean canOutputTorque(Direction side) {
      return side == this.getFacing();
   }

   @Override
   public boolean canInputTorque(@Nullable Direction side) {
      return side != null && side == this.getFacing().m_122424_();
   }

   @Nullable
   private TorqueCell getCell(@Nullable Direction side) {
      if (side == this.getFacing()) {
         return this.outputCell;
      } else {
         return side == this.getFacing().m_122424_() ? this.inputCell : null;
      }
   }

   private Direction getFacing() {
      return this.m_58900_().m_61138_(FlywheelControllerBlock.FACING) ? (Direction)this.m_58900_().m_61143_(FlywheelControllerBlock.FACING) : Direction.NORTH;
   }

   private TorqueTier getTier() {
      return getTierFromState(this.m_58900_());
   }

   private static TorqueTier getTierFromState(BlockState state) {
      return state.m_60734_() instanceof FlywheelControllerBlock block ? block.getTier() : TorqueTier.LIGHT;
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      tag.m_128347_("InputTorque", this.inputCell.getTorque());
      tag.m_128347_("OutputTorque", this.outputCell.getTorque());
      tag.m_128379_("VisualActive", this.visualActive);
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.inputCell.setTorque(tag.m_128459_("InputTorque"));
      this.outputCell.setTorque(tag.m_128459_("OutputTorque"));
      this.visualActive = tag.m_128471_("VisualActive");
   }

   public CompoundTag m_5995_() {
      CompoundTag tag = super.m_5995_();
      this.m_183515_(tag);
      return tag;
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.m_195640_(this);
   }

   private static record FlywheelStorageView(FlywheelStorageBlockEntity storage, TorqueTier tier, int size, int height) {
      double capacity() {
         return (double)(this.size * this.size * this.height) * this.tier.getStoragePerBlock();
      }

      double getStored() {
         return Math.min(this.storage.getStoredTorque(), this.capacity());
      }

      void setStored(double value) {
         this.storage.setStoredTorque(Math.min(value, this.capacity()));
      }
   }
}
