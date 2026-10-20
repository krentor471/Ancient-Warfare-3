package net.shadowmage.ancientwarfare.core.tile;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.block.TorqueTransportBlock;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.torque.SidedTorqueCell;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueAccess;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueCell;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueDemandNode;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueStats;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTier;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTransportKind;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TorqueTransportBlockEntity extends BlockEntity implements TorqueAccess, TorqueDemandNode, GeoBlockEntity {
   private static final String SINGLE_TORQUE_TAG = "Torque";
   private static final String SIDED_TORQUE_TAG = "SidedTorque";
   private static final String SIDE_TAG = "Side";
   private static final String TORQUE_TAG = "Torque";
   private static final String VISUAL_SPEED_TAG = "VisualSpeed";
   private static final String ACTIVE_SIDES_TAG = "ActiveSides";
   private static final int SIDE_ACTIVE_TICKS = 8;
   private final TorqueCell singleCell;
   private final SidedTorqueCell[] sidedCells = new SidedTorqueCell[Direction.values().length];
   private final int[] sideActiveTicks = new int[Direction.values().length];
   private double previousTotal;
   private TorqueStats stats = TorqueStats.EMPTY;
   private float visualSpeed;
   private int visualActiveSides;
   private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

   public TorqueTransportBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.id("torque_transport")), pos, state);
      TorqueTier tier = getTierFromState(state);
      this.singleCell = tier.createCell();

      for (Direction direction : Direction.values()) {
         this.sidedCells[direction.ordinal()] = new SidedTorqueCell(
            tier.getTransferLimit(), tier.getTransferLimit(), tier.getTransferLimit(), tier.getEfficiency(), direction, this
         );
      }
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, TorqueTransportBlockEntity blockEntity) {
      blockEntity.tickServer();
   }

   public TorqueStats getStats() {
      return this.stats.withStored(this.getTotalTorque());
   }

   public float getVisualSpeed() {
      return this.visualSpeed;
   }

   public boolean isSideVisuallyActive(Direction direction) {
      return (this.visualActiveSides & 1 << direction.ordinal()) != 0;
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(
         new AnimationController[]{
            new AnimationController(
               this,
               "shaft_rotation",
               0,
               state -> {
                  if (this.getVisualSpeed() > 0.001F) {
                     String animation = this.getKind() == TorqueTransportKind.JUNCTION
                        ? "animation.torque_junction.spin"
                        : (this.getKind() == TorqueTransportKind.DISTRIBUTOR ? "animation.torque_distributor.spin" : "animation.torque_shaft.spin");
                     return state.setAndContinue(RawAnimation.begin().thenLoop(animation));
                  } else {
                     return PlayState.STOP;
                  }
               }
            )
         }
      );
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.animationCache;
   }

   private void tickServer() {
      this.tickSideActivity();
      TorqueTransportKind kind = this.getKind();
      double before = this.getTotalTorque();
      double input = Math.max(0.0, before - this.previousTotal);
      double loss = 0.0;
      double output;
      if (kind == TorqueTransportKind.SHAFT) {
         output = this.transferPowerTo(this.getFacing());
      } else if (kind == TorqueTransportKind.DISTRIBUTOR) {
         this.balanceDistributorStorage();
         output = this.transferPowerFromDistributor();
      } else {
         this.balanceJunctionStorage();
         output = this.transferPowerTo(this.getFacing());
      }

      this.stats = new TorqueStats(this.getTotalTorque(), input, output, loss);
      this.previousTotal = this.getTotalTorque();
      this.updateVisualSpeed(input, output);
      if (input > 0.0 || output > 0.0 || loss > 0.0) {
         this.m_6596_();
      }
   }

   private void tickSideActivity() {
      int activeSides = 0;

      for (Direction direction : Direction.values()) {
         int index = direction.ordinal();
         if (this.sideActiveTicks[index] > 0) {
            this.sideActiveTicks[index]--;
            activeSides |= 1 << index;
         }
      }

      this.updateVisualActiveSides(activeSides);
   }

   private void updateVisualSpeed(double input, double output) {
      double activity = Math.max(Math.max(input, output), this.getTotalTorque() * 0.05);
      float nextSpeed = activity > 0.001 ? (float)Math.min(1.0, 0.18 + activity / Math.max(1.0, getTierFromState(this.m_58900_()).getTransferLimit())) : 0.0F;
      if (!(Math.abs(nextSpeed - this.visualSpeed) < 0.05F)) {
         this.visualSpeed = nextSpeed;
         if (this.f_58857_ != null) {
            this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 3);
         }
      }
   }

   private double transferPowerFromDistributor() {
      Direction inputSide = this.getFacing().m_122424_();
      double output = 0.0;

      for (Direction direction : Direction.values()) {
         if (direction != inputSide && this.isConnected(direction)) {
            output += this.transferPowerTo(direction);
         }
      }

      return output;
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

   private void balanceJunctionStorage() {
      Direction outputSide = this.getFacing();
      TorqueCell output = this.sidedCells[outputSide.ordinal()];
      double totalInput = 0.0;

      for (Direction direction : Direction.values()) {
         if (direction != outputSide) {
            totalInput += this.sidedCells[direction.ordinal()].getTorque();
         }
      }

      if (!(totalInput <= 0.0)) {
         double transfer = Math.min(totalInput, output.getMaxTorque() - output.getTorque());
         if (!(transfer <= 0.0)) {
            double percent = transfer / totalInput;
            double moved = 0.0;

            for (Direction directionx : Direction.values()) {
               if (directionx != outputSide) {
                  TorqueCell input = this.sidedCells[directionx.ordinal()];
                  double fromSide = input.getTorque() * percent;
                  input.setTorque(input.getTorque() - fromSide);
                  this.markSideActive(directionx);
                  moved += fromSide;
               }
            }

            output.setTorque(output.getTorque() + moved);
            this.markSideActive(outputSide);
         }
      }
   }

   private void balanceDistributorStorage() {
      Direction inputSide = this.getFacing().m_122424_();
      TorqueCell input = this.sidedCells[inputSide.ordinal()];
      if (!(input.getTorque() <= 0.0)) {
         double totalRequested = 0.0;

         for (Direction direction : Direction.values()) {
            if (direction != inputSide && this.isConnected(direction) && this.hasTorqueConnection(direction)) {
               TorqueCell output = this.sidedCells[direction.ordinal()];
               double downstreamDemand = this.getDemandBeyond(direction);
               totalRequested += Math.min(output.getMaxTorque() - output.getTorque(), Math.max(0.0, downstreamDemand - output.getTorque()));
            }
         }

         if (!(totalRequested <= 0.0)) {
            double transfer = Math.min(totalRequested, input.getTorque());
            double percent = transfer / totalRequested;
            double moved = 0.0;

            for (Direction directionx : Direction.values()) {
               if (directionx != inputSide && this.isConnected(directionx) && this.hasTorqueConnection(directionx)) {
                  TorqueCell output = this.sidedCells[directionx.ordinal()];
                  double downstreamDemand = this.getDemandBeyond(directionx);
                  double requested = Math.min(output.getMaxTorque() - output.getTorque(), Math.max(0.0, downstreamDemand - output.getTorque()));
                  double toSide = requested * percent;
                  output.setTorque(output.getTorque() + toSide);
                  this.markSideActive(directionx);
                  moved += toSide;
               }
            }

            input.setTorque(input.getTorque() - moved);
            this.markSideActive(inputSide);
         }
      }
   }

   private boolean hasTorqueConnection(Direction side) {
      if (this.f_58857_ == null) {
         return false;
      } else if (!(this.f_58857_.m_7702_(this.f_58858_.m_121945_(side)) instanceof TorqueAccess access)) {
         return false;
      } else {
         Direction neighborSide = side.m_122424_();
         return access.canInputTorque(neighborSide) || access.canOutputTorque(neighborSide);
      }
   }

   @Override
   public double getMaxTorque(@Nullable Direction side) {
      if (this.getKind() == TorqueTransportKind.SHAFT) {
         return this.singleCell.getMaxTorque();
      } else {
         return side == null ? 0.0 : this.sidedCells[side.ordinal()].getMaxTorque();
      }
   }

   @Override
   public double getTorqueStored(@Nullable Direction side) {
      if (this.getKind() == TorqueTransportKind.SHAFT) {
         return this.singleCell.getTorque();
      } else {
         return side == null ? 0.0 : this.sidedCells[side.ordinal()].getTorque();
      }
   }

   @Override
   public double addTorque(@Nullable Direction side, double amount) {
      double requested = Math.min(amount, this.getMaxTorqueInput(side));
      if (requested <= 0.0) {
         return 0.0;
      } else if (this.getKind() == TorqueTransportKind.SHAFT) {
         return this.canInputTorque(side) ? this.singleCell.addTorque(requested) : 0.0;
      } else if (side == null) {
         return 0.0;
      } else {
         double accepted = this.sidedCells[side.ordinal()].addTorque(requested);
         if (accepted > 0.0) {
            this.markSideActive(side);
         }

         return accepted;
      }
   }

   @Override
   public double drainTorque(@Nullable Direction side, double amount) {
      if (this.getKind() != TorqueTransportKind.SHAFT) {
         if (side == null) {
            return 0.0;
         } else {
            double drained = this.sidedCells[side.ordinal()].drainTorque(amount);
            if (drained > 0.0) {
               this.markSideActive(side);
            }

            return drained;
         }
      } else {
         return side != null && this.canOutputTorque(side) ? this.singleCell.drainTorque(amount) : 0.0;
      }
   }

   private void markSideActive(Direction direction) {
      this.sideActiveTicks[direction.ordinal()] = 8;
      this.updateVisualActiveSides(this.visualActiveSides | 1 << direction.ordinal());
   }

   private void updateVisualActiveSides(int activeSides) {
      if (this.visualActiveSides != activeSides) {
         this.visualActiveSides = activeSides;
         if (this.f_58857_ != null) {
            this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 3);
         }
      }
   }

   @Override
   public double getMaxTorqueOutput(Direction side) {
      if (this.getKind() == TorqueTransportKind.SHAFT) {
         return this.canOutputTorque(side) ? this.singleCell.getMaxTickOutput() : 0.0;
      } else {
         return this.sidedCells[side.ordinal()].getMaxTickOutput();
      }
   }

   @Override
   public double getMaxTorqueInput(@Nullable Direction side) {
      if (side != null && this.canInputTorque(side)) {
         Set<BlockPos> visited = new HashSet<>();
         visited.add(this.f_58858_);
         return this.getDownstreamDemand(side, visited, 0);
      } else {
         return 0.0;
      }
   }

   double getDownstreamDemand(Direction inputSide, Set<BlockPos> visited, int depth) {
      if (this.f_58857_ != null && this.canInputTorque(inputSide)) {
         double downstreamDemand;
         if (this.getKind() == TorqueTransportKind.DISTRIBUTOR) {
            Direction distributorInput = this.getFacing().m_122424_();
            downstreamDemand = 0.0;

            for (Direction direction : Direction.values()) {
               if (direction != distributorInput && this.isConnected(direction)) {
                  downstreamDemand += TorqueNetworkDemand.downstreamDemand(
                     this.f_58857_, this.f_58858_.m_121945_(direction), direction.m_122424_(), new HashSet<>(visited), depth + 1
                  );
               }
            }
         } else {
            Direction outputSide = this.getFacing();
            downstreamDemand = TorqueNetworkDemand.downstreamDemand(
               this.f_58857_, this.f_58858_.m_121945_(outputSide), outputSide.m_122424_(), visited, depth + 1
            );
         }

         double unmetDemand = Math.max(0.0, downstreamDemand - this.getTotalTorque());
         return this.getKind() == TorqueTransportKind.SHAFT
            ? Math.min(this.singleCell.getMaxTickInput(), unmetDemand)
            : Math.min(this.sidedCells[inputSide.ordinal()].getMaxTickInput(), unmetDemand);
      } else {
         return 0.0;
      }
   }

   @Override
   public double getNetworkTorqueDemand(Direction inputSide, Set<BlockPos> visited, int depth) {
      return this.getDownstreamDemand(inputSide, visited, depth);
   }

   private double getDemandBeyond(Direction outputSide) {
      return this.f_58857_ == null ? 0.0 : TorqueNetworkDemand.downstreamDemand(this.f_58857_, this.f_58858_, outputSide);
   }

   @Override
   public boolean canOutputTorque(Direction side) {
      TorqueTransportKind kind = this.getKind();
      return kind == TorqueTransportKind.DISTRIBUTOR
         ? side != this.getFacing().m_122424_() && this.isConnected(side)
         : side == this.getFacing() && this.isConnected(side);
   }

   @Override
   public boolean canInputTorque(@Nullable Direction side) {
      if (side == null) {
         return false;
      } else {
         TorqueTransportKind kind = this.getKind();
         return kind != TorqueTransportKind.SHAFT && kind != TorqueTransportKind.DISTRIBUTOR
            ? side != this.getFacing() && this.isConnected(side)
            : side == this.getFacing().m_122424_() && this.isConnected(side);
      }
   }

   private double getTotalTorque() {
      if (this.getKind() == TorqueTransportKind.SHAFT) {
         return this.singleCell.getTorque();
      } else {
         double total = 0.0;

         for (Direction direction : Direction.values()) {
            if (this.canInputTorque(direction) || this.canOutputTorque(direction)) {
               total += this.sidedCells[direction.ordinal()].getTorque();
            }
         }

         return total;
      }
   }

   private Direction getFacing() {
      return this.m_58900_().m_61138_(TorqueTransportBlock.FACING) ? (Direction)this.m_58900_().m_61143_(TorqueTransportBlock.FACING) : Direction.NORTH;
   }

   private TorqueTransportKind getKind() {
      return this.m_58900_().m_60734_() instanceof TorqueTransportBlock block ? block.getKind() : TorqueTransportKind.SHAFT;
   }

   private boolean isConnected(Direction direction) {
      return this.m_58900_().m_60734_() instanceof TorqueTransportBlock block ? block.isConnected(this.m_58900_(), direction) : true;
   }

   private static TorqueTier getTierFromState(BlockState state) {
      return state.m_60734_() instanceof TorqueTransportBlock block ? block.getTier() : TorqueTier.LIGHT;
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      tag.m_128347_("Torque", this.singleCell.getTorque());
      tag.m_128350_("VisualSpeed", this.visualSpeed);
      tag.m_128405_("ActiveSides", this.visualActiveSides);
      ListTag list = new ListTag();

      for (Direction direction : Direction.values()) {
         CompoundTag sideTag = new CompoundTag();
         sideTag.m_128359_("Side", direction.m_7912_());
         sideTag.m_128347_("Torque", this.sidedCells[direction.ordinal()].getTorque());
         list.add(sideTag);
      }

      tag.m_128365_("SidedTorque", list);
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.singleCell.setTorque(tag.m_128459_("Torque"));
      this.visualSpeed = tag.m_128457_("VisualSpeed");
      this.visualActiveSides = tag.m_128451_("ActiveSides");
      if (tag.m_128441_("SidedTorque")) {
         ListTag list = tag.m_128437_("SidedTorque", 10);

         for (int i = 0; i < list.size(); i++) {
            CompoundTag sideTag = list.m_128728_(i);
            Direction direction = Direction.m_122402_(sideTag.m_128461_("Side"));
            if (direction != null) {
               this.sidedCells[direction.ordinal()].setTorque(sideTag.m_128459_("Torque"));
            }
         }
      }

      this.previousTotal = this.getTotalTorque();
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.m_195640_(this);
   }

   public CompoundTag m_5995_() {
      CompoundTag tag = super.m_5995_();
      this.m_183515_(tag);
      return tag;
   }

   public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
      CompoundTag tag = packet.m_131708_();
      if (tag != null) {
         this.m_142466_(tag);
      }
   }
}
