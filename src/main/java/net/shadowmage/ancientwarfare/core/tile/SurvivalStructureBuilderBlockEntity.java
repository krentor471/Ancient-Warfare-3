package net.shadowmage.ancientwarfare.core.tile;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.block.SurvivalStructureBuilderBlock;
import net.shadowmage.ancientwarfare.core.common.npc.NpcWorkType;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkSite;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkerAccess;
import net.shadowmage.ancientwarfare.core.item.SurvivalStructureBuilderItem;
import net.shadowmage.ancientwarfare.core.structure.AwsTemplatePlacer;
import net.shadowmage.ancientwarfare.core.structure.PlayerStructureTemplateData;
import org.jetbrains.annotations.Nullable;

public class SurvivalStructureBuilderBlockEntity extends BlockEntity implements WorkSite {
   private static final int MAX_STORED_WORK_ENERGY = 256;
   private static final int WORKER_ENERGY_PER_VISIT = 16;
   private static final int WORKER_ENERGY_PER_TICK = 2;
   private static final int PLAYER_ENERGY_PER_USE = 8;
   private static final int MAX_OPERATIONS_PER_TICK = 16;
   private String templateName = "";
   private int phase;
   private int cursor;
   private int storedWorkEnergy;
   private BlockPos origin;
   private Direction facing = Direction.NORTH;

   public SurvivalStructureBuilderBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.STRUCTURE_BUILDER_TICKED), pos, state);
      this.origin = pos;
      if (state.hasProperty(SurvivalStructureBuilderBlock.FACING)) {
         this.facing = (Direction)state.getValue(SurvivalStructureBuilderBlock.FACING);
      }
   }

   public void configure(ItemStack stack, Direction placedFacing) {
      this.templateName = SurvivalStructureBuilderItem.templateName(stack);
      this.origin = this.worldPosition;
      this.facing = placedFacing != null && placedFacing.getAxis().isHorizontal() ? placedFacing : Direction.NORTH;
      this.phase = SurvivalStructureBuilderItem.phase(stack);
      this.cursor = SurvivalStructureBuilderItem.cursor(stack);
      this.storedWorkEnergy = SurvivalStructureBuilderItem.storedWorkEnergy(stack);
      this.clampProgress();
      this.markChangedAndSync();
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, SurvivalStructureBuilderBlockEntity builder) {
      if (level instanceof ServerLevel serverLevel) {
         builder.consumeWorkEnergy(serverLevel);
      }
   }

   private void consumeWorkEnergy(ServerLevel level) {
      if (!this.templateName.isBlank() && this.phase <= 2 && this.storedWorkEnergy > 0) {
         int budget = Math.min(16, this.storedWorkEnergy);
         int consumed;
         if (this.templateName.startsWith("aws:")) {
            consumed = this.advanceAws(level, budget);
         } else {
            consumed = this.advancePlayerTemplate(level, budget);
         }

         this.storedWorkEnergy = Math.max(0, this.storedWorkEnergy - consumed);
         if (level.getBlockEntity(this.worldPosition) == this) {
            this.markChangedAndSync();
         }
      }
   }

   private int advanceAws(ServerLevel level, int budget) {
      String path = this.templateName.substring(4);
      int consumed = 0;

      try {
         int volume = AwsTemplatePlacer.incrementalVolume(path);

         while (consumed < budget && this.phase <= 2) {
            if (this.phase == 0) {
               AwsTemplatePlacer.IncrementalStep step = AwsTemplatePlacer.clearPlayerBuiltStep(
                  level, this.origin, path, this.facing, this.cursor, budget - consumed, this.worldPosition
               );
               this.cursor = step.cursor();
               consumed += step.workUsed();
               if (this.cursor < volume) {
                  break;
               }

               this.phase = 1;
               this.cursor = 0;
            } else if (this.phase == 1) {
               AwsTemplatePlacer.IncrementalStep step = AwsTemplatePlacer.placePlayerBuiltStep(
                  level, this.origin, path, this.facing, this.cursor, budget - consumed, this.worldPosition
               );
               this.cursor = step.cursor();
               consumed += step.workUsed();
               if (this.cursor < volume) {
                  break;
               }

               this.phase = 2;
               this.cursor = 0;
            } else {
               consumed++;
               AwsTemplatePlacer.finishPlayerBuiltIncremental(level, this.origin, path, this.facing);
               this.completeAfterAnchorReplacement();
            }
         }
      } catch (RuntimeException var7) {
         AncientWarfareCore.LOG.warn("Unable to advance survival AWS builder {} at {}", new Object[]{path, this.worldPosition, var7});
         this.storedWorkEnergy = 0;
      }

      return consumed;
   }

   private int advancePlayerTemplate(ServerLevel level, int budget) {
      PlayerStructureTemplateData.PlayerTemplate template = PlayerStructureTemplateData.get(level).template(this.templateName).orElse(null);
      if (template != null && !template.blocks().isEmpty()) {
         int consumed = 0;
         Rotation rotation = rotationFor(this.facing);

         while (consumed < budget && this.phase <= 1) {
            int inspected = 0;
            int inspectionLimit = Math.max(256, budget * 16);

            while (consumed < budget && this.cursor < template.blocks().size() && inspected++ < inspectionLimit) {
               PlayerStructureTemplateData.TemplateBlock entry = template.blocks().get(this.cursor++);
               BlockPos target = this.origin.offset(rotateRelative(entry.relativePos(), this.facing));
               if (this.phase == 0) {
                  if (!entry.state().isAir() && entry.state().getBlock() != Blocks.STRUCTURE_VOID) {
                     BlockState existing = level.getBlockState(target);
                     if (!target.equals(this.worldPosition)
                        && level.getBlockEntity(target) == null
                        && AwsTemplatePlacer.isBasicNaturalTerrain(existing)
                        && level.setBlock(target, Blocks.AIR.defaultBlockState(), 18)) {
                        consumed++;
                     }
                  }
               } else if (!target.equals(this.worldPosition) && !entry.state().isAir() && entry.state().getBlock() != Blocks.STRUCTURE_VOID) {
                  BlockState intended = entry.state().rotate(rotation);
                  BlockState existing = level.getBlockState(target);
                  if (!existing.equals(intended)
                     && (existing.isAir() || level.getBlockEntity(target) == null && AwsTemplatePlacer.isBasicNaturalTerrain(existing))
                     && level.setBlock(target, intended, 18)) {
                     consumed++;
                     BlockEntity placed = level.getBlockEntity(target);
                     if (entry.blockEntityTag() != null && placed != null) {
                        CompoundTag tag = entry.blockEntityTag().copy();
                        tag.putInt("x", target.getX());
                        tag.putInt("y", target.getY());
                        tag.putInt("z", target.getZ());
                        placed.load(tag);
                        placed.setChanged();
                     }
                  }
               }
            }

            if (this.cursor >= template.blocks().size()) {
               this.cursor = 0;
               this.phase++;
               if (this.phase > 1) {
                  this.restorePlayerTemplateAnchor(level, template, rotation);
                  Set<Long> positions = new HashSet<>();
                  template.blocks().forEach(entry -> positions.add(this.origin.offset(rotateRelative(entry.relativePos(), this.facing)).asLong()));
                  AwsTemplatePlacer.refreshHorizontalConnections(level, positions);
                  this.completeAfterAnchorReplacement();
                  break;
               }
            }
         }

         return consumed;
      } else {
         this.storedWorkEnergy = 0;
         return 0;
      }
   }

   private void restorePlayerTemplateAnchor(ServerLevel level, PlayerStructureTemplateData.PlayerTemplate template, Rotation rotation) {
      level.removeBlock(this.worldPosition, false);

      for (PlayerStructureTemplateData.TemplateBlock entry : template.blocks()) {
         BlockPos target = this.origin.offset(rotateRelative(entry.relativePos(), this.facing));
         if (target.equals(this.worldPosition) && !entry.state().isAir() && entry.state().getBlock() != Blocks.STRUCTURE_VOID) {
            level.setBlock(target, entry.state().rotate(rotation), 18);
            BlockEntity placed = level.getBlockEntity(target);
            if (entry.blockEntityTag() != null && placed != null) {
               CompoundTag tag = entry.blockEntityTag().copy();
               tag.putInt("x", target.getX());
               tag.putInt("y", target.getY());
               tag.putInt("z", target.getZ());
               placed.load(tag);
               placed.setChanged();
            }

            return;
         }
      }
   }

   private void completeAfterAnchorReplacement() {
      this.phase = 3;
      this.storedWorkEnergy = 0;
   }

   public boolean addPlayerWork() {
      if (!this.hasWork()) {
         return false;
      } else {
         this.storedWorkEnergy = Math.min(256, this.storedWorkEnergy + 8);
         this.markChangedAndSync();
         return true;
      }
   }

   public Component progressMessage() {
      int volume = this.progressVolume();
      int percent = volume <= 0 ? 0 : Mth.clamp(Math.round((float)this.cursor * 100.0F / (float)volume), 0, 100);
      String stageKey = this.phase == 0 ? "message.ancientwarfare.structure_builder.clearing" : "message.ancientwarfare.structure_builder.building";
      return Component.translatable(
         "message.ancientwarfare.structure_builder.progress", new Object[]{Component.translatable(stageKey), percent, this.storedWorkEnergy}
      );
   }

   private int progressVolume() {
      if (this.templateName.startsWith("aws:")) {
         try {
            return AwsTemplatePlacer.incrementalVolume(this.templateName.substring(4));
         } catch (RuntimeException var3) {
            return 0;
         }
      } else {
         return this.level instanceof ServerLevel serverLevel
            ? PlayerStructureTemplateData.get(serverLevel).template(this.templateName).map(template -> template.blocks().size()).orElse(0)
            : 0;
      }
   }

   public ItemStack createProgressStack() {
      ItemStack stack;
      if (this.templateName.startsWith("aws:")) {
         String path = this.templateName.substring(4);
         String displayName = AwsTemplatePlacer.survivalTemplates()
            .stream()
            .filter(template -> template.path().equals(path))
            .map(AwsTemplatePlacer.TemplateInfo::name)
            .findFirst()
            .orElse(path);
         stack = SurvivalStructureBuilderItem.forAwsTemplate(path, displayName);
      } else {
         stack = new ItemStack(AWCoreRegistryAccess.block(AWCoreIds.STRUCTURE_BUILDER_TICKED));
         if (!this.templateName.isBlank()) {
            stack.setHoverName(Component.translatable("block.ancientwarfare.structure_builder_ticked").append(": ").append(Component.literal(this.templateName)));
         }
      }

      SurvivalStructureBuilderItem.writeProgress(stack, this.templateName, this.phase, this.cursor, this.storedWorkEnergy);
      return stack;
   }

   @Override
   public boolean hasWork() {
      return this.level != null
            && !this.level.isClientSide
            && !this.templateName.isBlank()
            && this.phase <= 2
            && this.level.getBlockEntity(this.worldPosition) == this
         ? this.progressVolume() > 0
         : false;
   }

   @Override
   public void addEnergyFromWorker(WorkerAccess worker) {
      if (this.hasWork() && worker.canWorkAt(this.getWorkType())) {
         int amount = Math.max(1, Math.round(16.0F * worker.getWorkEffectiveness(this.getWorkType())));
         this.storedWorkEnergy = Math.min(256, this.storedWorkEnergy + amount);
         this.markChangedAndSync();
      }
   }

   @Override
   public void onWorkerTick(WorkerAccess worker) {
      if (this.hasWork() && worker.canWorkAt(this.getWorkType())) {
         int amount = Math.max(1, Math.round(2.0F * worker.getWorkEffectiveness(this.getWorkType())));
         this.storedWorkEnergy = Math.min(256, this.storedWorkEnergy + amount);
         this.setChanged();
      }
   }

   @Override
   public NpcWorkType getWorkType() {
      return NpcWorkType.CRAFTING;
   }

   private void clampProgress() {
      this.phase = Mth.clamp(this.phase, 0, 2);
      this.cursor = Math.max(0, this.cursor);
      this.storedWorkEnergy = Mth.clamp(this.storedWorkEnergy, 0, 256);
   }

   private static Rotation rotationFor(Direction facing) {
      return switch (facing) {
         case EAST -> Rotation.CLOCKWISE_90;
         case SOUTH -> Rotation.CLOCKWISE_180;
         case WEST -> Rotation.COUNTERCLOCKWISE_90;
         default -> Rotation.NONE;
      };
   }

   private static BlockPos rotateRelative(BlockPos pos, Direction facing) {
      return switch (facing) {
         case EAST -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX());
         case SOUTH -> new BlockPos(-pos.getX(), pos.getY(), -pos.getZ());
         case WEST -> new BlockPos(pos.getZ(), pos.getY(), -pos.getX());
         default -> pos;
      };
   }

   public String templateName() {
      return this.templateName;
   }

   public BlockPos buildOrigin() {
      return this.origin;
   }

   public Direction buildFacing() {
      return this.facing;
   }

   public int buildPhase() {
      return this.phase;
   }

   private void markChangedAndSync() {
      this.setChanged();
      if (this.level != null && !this.level.isClientSide) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
      }
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

   public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
      CompoundTag tag = packet.getTag();
      if (tag != null) {
         this.load(tag);
      }
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putString("TemplateName", this.templateName);
      tag.putInt("Phase", this.phase);
      tag.putInt("Cursor", this.cursor);
      tag.putInt("StoredWorkEnergy", this.storedWorkEnergy);
      tag.putLong("Origin", this.origin.asLong());
      tag.putByte("Facing", (byte)this.facing.get2DDataValue());
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.templateName = tag.getString("TemplateName");
      this.phase = tag.getInt("Phase");
      this.cursor = tag.getInt("Cursor");
      this.storedWorkEnergy = tag.getInt("StoredWorkEnergy");
      this.origin = tag.contains("Origin") ? BlockPos.of(tag.getLong("Origin")) : this.worldPosition;
      this.facing = tag.contains("Facing") ? Direction.from2DDataValue(tag.getByte("Facing")) : Direction.NORTH;
      this.clampProgress();
   }
}
