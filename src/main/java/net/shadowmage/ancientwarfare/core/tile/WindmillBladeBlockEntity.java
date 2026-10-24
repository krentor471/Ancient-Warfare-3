package net.shadowmage.ancientwarfare.core.tile;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.block.WindmillBladeBlock;
import net.shadowmage.ancientwarfare.core.block.WindmillBladeRunningBlock;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.util.GeckoLibUtil;

public class WindmillBladeBlockEntity extends BlockEntity implements GeoBlockEntity {
   private static final int MIN_WINDMILL_SIZE = 3;
   private static final int MAX_WINDMILL_SIZE = 7;
   private static final String CONTROL_POS = "ControlPos";
   private static final String CONTROL = "Control";
   private static final String SIZE = "Size";
   private static final String DIRECTION = "Direction";
   private BlockPos controlPos;
   private boolean control;
   private int size;
   private Direction windmillDirection = Direction.NORTH;
   private float rotation;
   private float previousRotation;
   private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

   public WindmillBladeBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.id("windmill_blade")), pos, state);
   }

   public void onPlaced() {
   }

   public WindmillBladeBlockEntity.ToggleResult toggleAssembly() {
      return this.toggleAssembly(null);
   }

   public WindmillBladeBlockEntity.ToggleResult toggleAssembly(@Nullable Direction preferredHorizontal) {
      if (this.level == null || this.level.isClientSide) {
         return WindmillBladeBlockEntity.ToggleResult.INVALID;
      } else if (this.control && this.controlPos != null) {
         this.clearSquare();
         return WindmillBladeBlockEntity.ToggleResult.STOPPED;
      } else {
         WindmillBladeBlockEntity.Structure structure = preferredHorizontal == null ? this.findStructure() : this.findStructure(preferredHorizontal);
         if (structure == null) {
            return WindmillBladeBlockEntity.ToggleResult.INVALID;
         } else {
            this.formSquare(structure.center(), structure.horizontal(), structure.size());
            return WindmillBladeBlockEntity.ToggleResult.STARTED;
         }
      }
   }

   public static WindmillBladeBlockEntity.ToggleResult toggleAt(Level level, BlockPos bladePos) {
      return toggleAt(level, bladePos, null);
   }

   public static WindmillBladeBlockEntity.ToggleResult toggleAt(Level level, BlockPos bladePos, @Nullable Direction preferredHorizontal) {
      if (level.isClientSide) {
         return WindmillBladeBlockEntity.ToggleResult.INVALID;
      } else if (level.getBlockState(bladePos).is(AWCoreRegistryAccess.block(AWCoreIds.WINDMILL_BLADE_RUNNING))
         && level.getBlockEntity(bladePos) instanceof WindmillBladeBlockEntity running) {
         return running.toggleAssembly(preferredHorizontal);
      } else if (!level.getBlockState(bladePos).is(AWCoreRegistryAccess.block(AWCoreIds.WINDMILL_BLADE))) {
         return WindmillBladeBlockEntity.ToggleResult.INVALID;
      } else {
         Direction horizontal = preferredHorizontal != null
            ? preferredHorizontal
            : getBladePlane((Direction)level.getBlockState(bladePos).getValue(WindmillBladeBlock.FACING));
         WindmillBladeBlockEntity.Structure structure = findStructure(level, bladePos, horizontal);
         if (structure == null) {
            return WindmillBladeBlockEntity.ToggleResult.INVALID;
         } else {
            formSquare(level, structure.center(), horizontal, structure.size());
            return WindmillBladeBlockEntity.ToggleResult.STARTED;
         }
      }
   }

   public void onBroken() {
      if (this.level != null && !this.level.isClientSide) {
         if (this.control) {
            this.clearSquare();
         } else if (this.controlPos != null && this.level.getBlockEntity(this.controlPos) instanceof WindmillBladeBlockEntity controller) {
            controller.clearSquare();
         }
      }
   }

   public void clientTick() {
      this.previousRotation = this.rotation;
      if (this.control && this.isFormed()) {
         this.rotation = (this.rotation + 20.0F) % 360.0F;
      }
   }

   public float getRotation(float partialTick) {
      return this.previousRotation + (this.rotation - this.previousRotation) * partialTick;
   }

   public boolean isFormed() {
      return this.controlPos != null;
   }

   public boolean isControl() {
      return this.control;
   }

   @Nullable
   public BlockPos getControlPos() {
      return this.controlPos;
   }

   public int getWindmillSize() {
      return this.size;
   }

   public Direction getWindmillDirection() {
      return this.windmillDirection;
   }

   @Nullable
   private WindmillBladeBlockEntity.Structure findStructure() {
      if (this.level == null) {
         return null;
      } else {
         Direction normal = (Direction)this.getBlockState().getValue(WindmillBladeRunningBlock.FACING);
         return this.findStructure(getBladePlane(normal));
      }
   }

   @Nullable
   private WindmillBladeBlockEntity.Structure findStructure(Direction horizontal) {
      if (this.level == null) {
         return null;
      } else {
         WindmillBladeBlockEntity.Structure direct = this.findStructureOnPlane(horizontal);
         if (direct != null) {
            return direct;
         } else {
            Direction alternate = horizontal.getAxis() == Axis.X ? Direction.SOUTH : Direction.EAST;
            return alternate != horizontal ? this.findStructureOnPlane(alternate) : null;
         }
      }
   }

   @Nullable
   private WindmillBladeBlockEntity.Structure findStructureOnPlane(Direction horizontal) {
      for (int candidateSize = 7; candidateSize >= 3; candidateSize -= 2) {
         int radius = (candidateSize - 1) / 2;

         for (int h = -radius; h <= radius; h++) {
            for (int v = -radius; v <= radius; v++) {
               BlockPos candidate = this.worldPosition.relative(horizontal, h).above(v);
               if (this.isCompleteSquare(candidate, horizontal, candidateSize)) {
                  return new WindmillBladeBlockEntity.Structure(candidate, horizontal, candidateSize);
               }
            }
         }
      }

      return null;
   }

   private static Direction getBladePlane(Direction normal) {
      return normal.getAxis() == Axis.X ? Direction.SOUTH : Direction.EAST;
   }

   @Nullable
   private static WindmillBladeBlockEntity.Structure findStructure(Level level, BlockPos origin, Direction horizontal) {
      WindmillBladeBlockEntity.Structure direct = findStructureOnPlane(level, origin, horizontal);
      if (direct != null) {
         return direct;
      } else {
         Direction alternate = horizontal.getAxis() == Axis.X ? Direction.SOUTH : Direction.EAST;
         return alternate != horizontal ? findStructureOnPlane(level, origin, alternate) : null;
      }
   }

   @Nullable
   private static WindmillBladeBlockEntity.Structure findStructureOnPlane(Level level, BlockPos origin, Direction horizontal) {
      for (int candidateSize = 7; candidateSize >= 3; candidateSize -= 2) {
         int radius = (candidateSize - 1) / 2;

         for (int h = -radius; h <= radius; h++) {
            for (int v = -radius; v <= radius; v++) {
               BlockPos candidate = origin.relative(horizontal, h).above(v);
               if (isCompleteSquare(level, candidate, horizontal, candidateSize)) {
                  return new WindmillBladeBlockEntity.Structure(candidate, horizontal, candidateSize);
               }
            }
         }
      }

      return null;
   }

   private boolean isCompleteSquare(BlockPos center, Direction horizontal, int candidateSize) {
      return isCompleteSquare(this.level, center, horizontal, candidateSize);
   }

   private static boolean isCompleteSquare(Level level, BlockPos center, Direction horizontal, int candidateSize) {
      int radius = (candidateSize - 1) / 2;

      for (int h = -radius; h <= radius; h++) {
         for (int v = -radius; v <= radius; v++) {
            BlockPos pos = center.relative(horizontal, h).above(v);
            if (!level.getBlockState(pos).is(AWCoreRegistryAccess.block(AWCoreIds.WINDMILL_BLADE))) {
               return false;
            }
         }
      }

      return true;
   }

   private void formSquare(BlockPos center, Direction horizontal, int candidateSize) {
      formSquare(this.level, center, horizontal, candidateSize);
   }

   private static void formSquare(Level level, BlockPos center, Direction horizontal, int candidateSize) {
      int radius = (candidateSize - 1) / 2;
      Direction renderDirection = horizontal.getAxis() == Axis.X ? Direction.WEST : Direction.NORTH;
      List<BlockPos> positions = new ArrayList<>();

      for (int h = -radius; h <= radius; h++) {
         for (int v = -radius; v <= radius; v++) {
            positions.add(center.relative(horizontal, h).above(v));
         }
      }

      for (BlockPos pos : positions) {
         BlockState oldState = level.getBlockState(pos);
         Direction facing = oldState.hasProperty(WindmillBladeBlock.FACING) ? (Direction)oldState.getValue(WindmillBladeBlock.FACING) : Direction.NORTH;
         level.setBlock(
            pos, (BlockState)AWCoreRegistryAccess.block(AWCoreIds.WINDMILL_BLADE_RUNNING).defaultBlockState().setValue(WindmillBladeRunningBlock.FACING, facing), 11
         );
         if (level.getBlockEntity(pos) instanceof WindmillBladeBlockEntity blade) {
            blade.controlPos = center;
            blade.control = pos.equals(center);
            blade.size = blade.control ? candidateSize : 0;
            blade.windmillDirection = renderDirection;
            blade.setChanged();
            level.sendBlockUpdated(pos, blade.getBlockState(), blade.getBlockState(), 2);
         }
      }
   }

   private void clearSquare() {
      int radius = Math.max(0, (this.size - 1) / 2);
      if (this.controlPos != null && this.size > 0) {
         Direction[] planes = new Direction[]{Direction.EAST, Direction.SOUTH};
         List<BlockPos> positions = new ArrayList<>();

         for (Direction horizontal : planes) {
            for (int h = -radius; h <= radius; h++) {
               for (int v = -radius; v <= radius; v++) {
                  BlockPos pos = this.controlPos.relative(horizontal, h).above(v);
                  if (!positions.contains(pos)) {
                     positions.add(pos);
                  }
               }
            }
         }

         for (BlockPos pos : positions) {
            if (this.level.getBlockState(pos).is(AWCoreRegistryAccess.block(AWCoreIds.WINDMILL_BLADE_RUNNING))) {
               BlockEntity var14 = this.level.getBlockEntity(pos);
               if (var14 instanceof WindmillBladeBlockEntity) {
                  WindmillBladeBlockEntity blade = (WindmillBladeBlockEntity)var14;
                  if (this.controlPos.equals(blade.controlPos)) {
                     blade.clearState();
                  }
               }
            }
         }
      }
   }

   @Nullable
   public static WindmillBladeBlockEntity.StructureInfo findStructureForGenerator(Level level, BlockPos bladeOrigin, Direction preferredHorizontal) {
      WindmillBladeBlockEntity.Structure structure = findStructure(level, bladeOrigin, preferredHorizontal);
      if (structure == null) {
         return null;
      } else {
         int radius = (structure.size() - 1) / 2;
         List<BlockPos> positions = new ArrayList<>();

         for (int h = -radius; h <= radius; h++) {
            for (int v = -radius; v <= radius; v++) {
               positions.add(structure.center().relative(structure.horizontal(), h).above(v));
            }
         }

         return new WindmillBladeBlockEntity.StructureInfo(structure.center(), structure.horizontal(), structure.size(), positions);
      }
   }

   private void clearConnected() {
      List<BlockPos> connected = new ArrayList<>();
      List<BlockPos> frontier = new ArrayList<>();
      connected.add(this.worldPosition);
      frontier.add(this.worldPosition);

      while (!frontier.isEmpty() && connected.size() < 289) {
         BlockPos current = frontier.remove(frontier.size() - 1);

         for (Direction direction : Direction.values()) {
            BlockPos neighbor = current.relative(direction);
            if (!connected.contains(neighbor)
               && !(neighbor.distSqr(this.worldPosition) > 400.0)
               && this.level.getBlockState(neighbor).is(AWCoreRegistryAccess.block(AWCoreIds.WINDMILL_BLADE_RUNNING))) {
               connected.add(neighbor);
               frontier.add(neighbor);
            }
         }
      }

      for (BlockPos pos : connected) {
         if (this.level.getBlockEntity(pos) instanceof WindmillBladeBlockEntity blade) {
            blade.clearState();
         }
      }
   }

   private void clearState() {
      if (this.level != null) {
         BlockState oldState = this.getBlockState();
         Direction facing = oldState.hasProperty(WindmillBladeRunningBlock.FACING)
            ? (Direction)oldState.getValue(WindmillBladeRunningBlock.FACING)
            : Direction.NORTH;
         this.controlPos = null;
         this.control = false;
         this.size = 0;
         this.setChanged();
         BlockState newState = (BlockState)AWCoreRegistryAccess.block(AWCoreIds.WINDMILL_BLADE).defaultBlockState().setValue(WindmillBladeBlock.FACING, facing);
         this.level.setBlock(this.worldPosition, newState, 11);
      }
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      if (this.controlPos != null) {
         tag.putLong("ControlPos", this.controlPos.asLong());
      }

      tag.putBoolean("Control", this.control);
      tag.putInt("Size", this.size);
      tag.putByte("Direction", (byte)this.windmillDirection.get2DDataValue());
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.controlPos = tag.contains("ControlPos") ? BlockPos.of(tag.getLong("ControlPos")) : null;
      this.control = tag.getBoolean("Control");
      this.size = tag.getInt("Size");
      if (tag.contains("Direction")) {
         this.windmillDirection = Direction.from2DDataValue(tag.getByte("Direction"));
      }
   }

   public CompoundTag getUpdateTag() {
      CompoundTag tag = new CompoundTag();
      this.saveAdditional(tag);
      return tag;
   }

   public void handleUpdateTag(CompoundTag tag) {
      this.load(tag);
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public AABB getRenderBoundingBox() {
      int radius = Math.max(1, (this.size - 1) / 2);
      int renderRadius = Math.max(2, radius * 2 + 2);
      return new AABB(
         this.worldPosition.offset(-renderRadius, -renderRadius, -renderRadius), this.worldPosition.offset(renderRadius + 1, renderRadius + 1, renderRadius + 1)
      );
   }

   public void registerControllers(ControllerRegistrar controllers) {
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.animationCache;
   }

   private static record Structure(BlockPos center, Direction horizontal, int size) {
   }

   public static record StructureInfo(BlockPos center, Direction horizontal, int size, List<BlockPos> positions) {
   }

   public static enum ToggleResult {
      STARTED,
      STOPPED,
      INVALID;
   }
}
