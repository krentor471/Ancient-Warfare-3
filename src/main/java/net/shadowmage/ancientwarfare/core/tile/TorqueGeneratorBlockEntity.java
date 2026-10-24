package net.shadowmage.ancientwarfare.core.tile;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.block.TorqueGeneratorBlock;
import net.shadowmage.ancientwarfare.core.block.WindmillBladeBlock;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueAccess;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueCell;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueGeneratorKind;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueStats;
import net.shadowmage.ancientwarfare.core.common.torque.TorqueTier;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class TorqueGeneratorBlockEntity extends BlockEntity implements TorqueAccess, GeoBlockEntity {
   private static final String TORQUE_TAG = "Torque";
   private static final String BURN_TIME_TAG = "BurnTime";
   private static final String BURN_TIME_BASE_TAG = "BurnTimeBase";
   private static final String FUEL_TAG = "Fuel";
   private static final String VALID_SETUP_TAG = "ValidSetup";
   private static final String BLADE_COUNT_TAG = "BladeCount";
   private static final String WINDMILL_CENTER_TAG = "WindmillCenter";
   private static final String WINDMILL_PLANE_TAG = "WindmillPlane";
   private static final int LOGIC_INTERVAL_TICKS = 1;
   private static final double STIRLING_OUTPUT = 1000.0;
   private static final double WATERWHEEL_OUTPUT = 10.0;
   private static final double WINDMILL_SMALL_OUTPUT = 20.0;
   private static final double WINDMILL_MEDIUM_OUTPUT = 30.0;
   private static final double WINDMILL_LARGE_OUTPUT = 40.0;
   private final TorqueCell torqueCell;
   private ItemStack fuel = ItemStack.EMPTY;
   private int burnTime;
   private int burnTimeBase;
   private int validationCooldown;
   private boolean validSetup;
   private int bladeCount;
   @Nullable
   private BlockPos windmillCenter;
   private Direction windmillPlane = Direction.EAST;
   private int tickCount;
   private int logicCooldown;
   private TorqueStats stats = TorqueStats.EMPTY;
   private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

   public TorqueGeneratorBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.id("torque_generator")), pos, state);
      boolean windmill = this.getKind() == TorqueGeneratorKind.WINDMILL;
      double outputLimit = windmill ? 160.0 : (this.getKind() == TorqueGeneratorKind.WATERWHEEL ? 10.0 : 1000.0);
      this.torqueCell = new TorqueCell(0.0, outputLimit, outputLimit, windmill ? 1.0 : TorqueTier.LIGHT.getEfficiency());
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, TorqueGeneratorBlockEntity blockEntity) {
      blockEntity.tickServer();
   }

   public boolean tryInsertFuel(Player player, InteractionHand hand, ItemStack held) {
      if (!held.isEmpty() && this.getFuelBurnTime(held) > 0 && this.fuel.isEmpty()) {
         ItemStack inserted = held.copyWithCount(1);
         this.fuel = inserted;
         if (!player.getAbilities().instabuild) {
            held.shrink(1);
         }

         this.tryStartBurning();
         this.setChanged();
         return true;
      } else {
         return false;
      }
   }

   public TorqueStats getStats() {
      return this.stats.withStored(this.getTorqueStored(null));
   }

   public String getStatusTranslationKey() {
      return switch (this.getKind()) {
         case WATERWHEEL -> "guistrings.automation.torque.waterwheel_status";
         case WINDMILL -> "guistrings.automation.torque.windmill_status";
         case STIRLING -> "guistrings.automation.torque.stirling_status";
      };
   }

   public String getStatusDetail() {
      if (this.getKind() == TorqueGeneratorKind.WATERWHEEL) {
         return (this.validSetup ? "valid" : "invalid") + " seconds=" + this.tickCount;
      } else if (this.getKind() == TorqueGeneratorKind.WINDMILL) {
         return "blades=" + this.bladeCount + " " + (this.validSetup ? "valid" : "invalid") + " seconds=" + this.tickCount;
      } else {
         return this.burnTimeBase <= 0
            ? (this.fuel.isEmpty() ? "no fuel" : "ready") + " seconds=" + this.tickCount
            : this.burnTime + "/" + this.burnTimeBase + " fuelTicks, seconds=" + this.tickCount;
      }
   }

   public AABB getRenderBoundingBox() {
      if (this.getKind() == TorqueGeneratorKind.WINDMILL) {
         int radius = Math.max(2, this.bladeCount + 2);
         return new AABB(this.worldPosition.offset(-radius, -radius, -radius), this.worldPosition.offset(radius + 1, radius + 1, radius + 1));
      } else {
         return this.getKind() == TorqueGeneratorKind.WATERWHEEL
            ? new AABB(this.worldPosition.offset(-3, -3, -3), this.worldPosition.offset(4, 4, 4))
            : new AABB(this.worldPosition);
      }
   }

   private void tickServer() {
      if (this.getKind() == TorqueGeneratorKind.STIRLING) {
         this.tickStirlingPerTick();
         double output = this.transferPowerTo(this.getFacing()) + this.transferPowerTo(this.getFacing().getOpposite());
         this.stats = new TorqueStats(this.getTorqueStored(null), this.burnTime > 0 ? 1000.0 : 0.0, output, 0.0);
      } else if (this.logicCooldown++ >= 0) {
         this.logicCooldown = 0;
         this.tickCount++;
         if (this.getKind() == TorqueGeneratorKind.WINDMILL) {
            double before = this.torqueCell.getTorque();
            this.tickWindmill();
            double generated = Math.max(0.0, this.torqueCell.getTorque() - before);
            double output = this.transferPowerTo(this.getFacing()) + this.transferPowerTo(this.getFacing().getOpposite());
            this.stats = new TorqueStats(this.getTorqueStored(null), generated, output, 0.0);
            this.setChanged();
         } else {
            double before = this.torqueCell.getTorque();
            switch (this.getKind()) {
               case WATERWHEEL:
                  this.tickWaterwheel();
               case STIRLING:
            }

            double generated = Math.max(0.0, this.torqueCell.getTorque() - before);
            double output = this.transferPowerTo(this.getFacing()) + this.transferPowerTo(this.getFacing().getOpposite());
            double loss = 0.0;
            this.stats = new TorqueStats(this.getTorqueStored(null), generated, output, loss);
            if (generated > 0.0 || output > 0.0 || loss > 0.0) {
               this.setChanged();
            }
         }
      }
   }

   private void tickStirlingPerTick() {
      if (this.burnTime <= 0) {
         this.tryStartBurning();
      }

      if (this.burnTime > 0) {
         if (this.torqueCell.getTorque() < this.torqueCell.getMaxTorque()) {
            this.generateTorque(1000.0);
         }

         this.burnTime--;
         if (this.burnTime == 0) {
            this.syncStirlingState();
         }
      }
   }

   private boolean tryStartBurning() {
      int ticks = this.getFuelBurnTime(this.fuel);
      if (ticks <= 0) {
         return false;
      } else {
         this.fuel.shrink(1);
         if (this.fuel.isEmpty()) {
            this.fuel = ItemStack.EMPTY;
         }

         this.burnTime = ticks;
         this.burnTimeBase = ticks;
         this.syncStirlingState();
         return true;
      }
   }

   private void syncStirlingState() {
      this.setChanged();
      if (this.level != null && !this.level.isClientSide) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public boolean isStirlingRunningForRender() {
      return this.getKind() == TorqueGeneratorKind.STIRLING && this.burnTime > 0;
   }

   private void tickWaterwheel() {
      this.validSetup = this.validateWaterwheel();
      if (this.validSetup && this.torqueCell.getTorque() < this.torqueCell.getMaxTorque()) {
         this.generateTorque(10.0);
      }
   }

   private void generateTorque(double amount) {
      this.torqueCell.setTorque(Math.min(this.torqueCell.getMaxTorque(), this.torqueCell.getTorque() + amount));
   }

   private void tickWindmill() {
      this.validSetup = this.windmillCenter != null && (this.bladeCount == 3 || this.bladeCount == 5 || this.bladeCount == 7);
      if (this.validSetup && this.torqueCell.getTorque() < this.torqueCell.getMaxTorque()) {
         this.generateTorque(this.getWindmillOutput());
      }
   }

   private double getWindmillOutput() {
      double base = switch (this.bladeCount) {
         case 3 -> 20.0;
         default -> 0.0;
         case 5 -> 30.0;
         case 7 -> 40.0;
      };
      if (this.level != null && !(base <= 0.0)) {
         int y = this.worldPosition.getY();
         double heightMultiplier = y <= 100 ? 1.0 : (y <= 150 ? 1.5 : (y <= 200 ? 2.0 : (y <= 250 ? 3.0 : 4.0)));
         return base * heightMultiplier;
      } else {
         return base;
      }
   }

   public int countWindmillBladesForRender() {
      return this.validSetup ? this.bladeCount : 0;
   }

   public boolean hasValidWindmillStructureForRender() {
      return this.validSetup && this.windmillCenter != null;
   }

   public boolean isValidSetup() {
      return this.validSetup;
   }

   public WindmillBladeBlockEntity.ToggleResult toggleWindmillAssembly() {
      if (this.level != null && this.getKind() == TorqueGeneratorKind.WINDMILL) {
         if (this.validSetup && this.windmillCenter != null) {
            this.restoreWindmillBlades();
            this.validSetup = false;
            this.bladeCount = 0;
            this.windmillCenter = null;
            this.torqueCell.setTorque(0.0);
            this.syncWindmillState();
            return WindmillBladeBlockEntity.ToggleResult.STOPPED;
         } else {
            Direction preferredPlane = this.getFacing().getAxis() == Axis.X ? Direction.SOUTH : Direction.EAST;
            Direction facing = this.getFacing();

            for (Direction side : new Direction[]{facing.getOpposite(), facing}) {
               WindmillBladeBlockEntity.StructureInfo structure = WindmillBladeBlockEntity.findStructureForGenerator(
                  this.level, this.worldPosition.relative(side), preferredPlane
               );
               if (structure != null) {
                  for (BlockPos bladePos : structure.positions()) {
                     this.level.setBlock(bladePos, Blocks.AIR.defaultBlockState(), 11);
                  }

                  this.windmillCenter = structure.center();
                  this.windmillPlane = structure.horizontal();
                  this.bladeCount = structure.size();
                  this.validSetup = true;
                  this.syncWindmillState();
                  return WindmillBladeBlockEntity.ToggleResult.STARTED;
               }
            }

            return WindmillBladeBlockEntity.ToggleResult.INVALID;
         }
      } else {
         return WindmillBladeBlockEntity.ToggleResult.INVALID;
      }
   }

   private void restoreWindmillBlades() {
      if (this.level != null && this.windmillCenter != null && this.bladeCount > 0) {
         int radius = (this.bladeCount - 1) / 2;
         Direction bladeFacing = this.getFacing().getOpposite();

         for (int h = -radius; h <= radius; h++) {
            for (int v = -radius; v <= radius; v++) {
               BlockPos pos = this.windmillCenter.relative(this.windmillPlane, h).above(v);
               this.level
                  .setBlock(
                     pos, (BlockState)AWCoreRegistryAccess.block(AWCoreIds.WINDMILL_BLADE).defaultBlockState().setValue(WindmillBladeBlock.FACING, bladeFacing), 11
                  );
            }
         }
      }
   }

   private void syncWindmillState() {
      this.setChanged();
      if (this.level != null) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(
         new AnimationController[]{
            new AnimationController(
               this,
               "generator_rotation",
               0,
               state -> {
                  if (this.getKind() == TorqueGeneratorKind.WATERWHEEL && this.hasValidWaterwheelStructureForRender()) {
                     return state.setAndContinue(RawAnimation.begin().thenLoop("animation.ancientwarfare.waterwheel_generator.spin"));
                  } else {
                     return this.isStirlingRunningForRender()
                        ? state.setAndContinue(RawAnimation.begin().thenLoop("animation.ancientwarfare.stirling_generator.run"))
                        : PlayState.STOP;
                  }
               }
            )
         }
      );
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.animationCache;
   }

   public boolean hasValidWaterwheelStructureForRender() {
      return this.getKind() == TorqueGeneratorKind.WATERWHEEL && this.validateWaterwheel();
   }

   private boolean validateWaterwheel() {
      for (BlockPos pos : BlockPos.betweenClosed(this.worldPosition.offset(-2, -2, -2), this.worldPosition.offset(2, 2, 2))) {
         if (this.isWater(pos)) {
            return true;
         }
      }

      Direction forward = this.getFacing().getOpposite();
      BlockPos inner = this.worldPosition.relative(forward);
      if (this.isAir(inner) && this.isAir(inner.above())) {
         Direction rightDirection = forward.getClockWise();
         Direction leftDirection = forward.getCounterClockWise();
         BlockPos left = inner.relative(leftDirection);
         BlockPos right = inner.relative(rightDirection);
         boolean leftDownFlow = this.isWater(left) && this.isWater(left.below());
         boolean rightDownFlow = this.isWater(right) && this.isWater(right.below());
         if (leftDownFlow && this.isAir(right) && this.isAir(right.above())) {
            return true;
         } else {
            return rightDownFlow && this.isAir(left) && this.isAir(left.above())
               ? true
               : this.isWater(left.below()) && this.isWater(right.below()) && this.isWater(inner.below());
         }
      } else {
         return false;
      }
   }

   private boolean isAir(BlockPos pos) {
      return this.level != null && this.level.getBlockState(pos).isAir();
   }

   private boolean isWater(BlockPos pos) {
      if (this.level == null) {
         return false;
      } else {
         FluidState fluid = this.level.getFluidState(pos);
         return fluid.is(Fluids.WATER) || this.level.getBlockState(pos).is(Blocks.WATER);
      }
   }

   private int getFuelBurnTime(ItemStack stack) {
      return AWCorePlatform.fuelBurnTime(stack);
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
      return this.torqueCell.getMaxTorque();
   }

   @Override
   public double getTorqueStored(@Nullable Direction side) {
      return this.torqueCell.getTorque();
   }

   @Override
   public double addTorque(@Nullable Direction side, double amount) {
      return 0.0;
   }

   @Override
   public double drainTorque(@Nullable Direction side, double amount) {
      return side != null && this.canOutputTorque(side) ? this.torqueCell.drainTorque(amount) : 0.0;
   }

   @Override
   public double getMaxTorqueOutput(Direction side) {
      return this.canOutputTorque(side) ? this.torqueCell.getMaxTickOutput() : 0.0;
   }

   @Override
   public double getMaxTorqueInput(@Nullable Direction side) {
      return 0.0;
   }

   @Override
   public boolean canOutputTorque(Direction side) {
      return side == this.getFacing() || side == this.getFacing().getOpposite();
   }

   @Override
   public boolean canInputTorque(@Nullable Direction side) {
      return false;
   }

   private Direction getFacing() {
      return this.getBlockState().hasProperty(TorqueGeneratorBlock.FACING) ? (Direction)this.getBlockState().getValue(TorqueGeneratorBlock.FACING) : Direction.NORTH;
   }

   private TorqueGeneratorKind getKind() {
      return this.getBlockState().getBlock() instanceof TorqueGeneratorBlock block ? block.getKind() : TorqueGeneratorKind.STIRLING;
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putDouble("Torque", this.torqueCell.getTorque());
      tag.putInt("BurnTime", this.burnTime);
      tag.putInt("BurnTimeBase", this.burnTimeBase);
      tag.putBoolean("ValidSetup", this.validSetup);
      tag.putInt("BladeCount", this.bladeCount);
      if (this.windmillCenter != null) {
         tag.putLong("WindmillCenter", this.windmillCenter.asLong());
      }

      tag.putByte("WindmillPlane", (byte)this.windmillPlane.get2DDataValue());
      if (!this.fuel.isEmpty()) {
         tag.put("Fuel", this.fuel.save(new CompoundTag()));
      }
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.torqueCell.setTorque(tag.getDouble("Torque"));
      this.burnTime = tag.getInt("BurnTime");
      this.burnTimeBase = tag.getInt("BurnTimeBase");
      this.validSetup = tag.getBoolean("ValidSetup");
      this.bladeCount = tag.getInt("BladeCount");
      this.windmillCenter = tag.contains("WindmillCenter") ? BlockPos.of(tag.getLong("WindmillCenter")) : null;
      if (tag.contains("WindmillPlane")) {
         this.windmillPlane = Direction.from2DDataValue(tag.getByte("WindmillPlane"));
      }

      this.fuel = tag.contains("Fuel") ? ItemStack.of(tag.getCompound("Fuel")) : ItemStack.EMPTY;
   }

   public CompoundTag getUpdateTag() {
      CompoundTag tag = super.getUpdateTag();
      this.saveAdditional(tag);
      return tag;
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }
}
