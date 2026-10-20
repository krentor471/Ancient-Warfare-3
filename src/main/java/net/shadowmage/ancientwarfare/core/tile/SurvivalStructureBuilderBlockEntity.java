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
      if (state.m_61138_(SurvivalStructureBuilderBlock.FACING)) {
         this.facing = (Direction)state.m_61143_(SurvivalStructureBuilderBlock.FACING);
      }
   }

   public void configure(ItemStack stack, Direction placedFacing) {
      this.templateName = SurvivalStructureBuilderItem.templateName(stack);
      this.origin = this.f_58858_;
      this.facing = placedFacing != null && placedFacing.m_122434_().m_122479_() ? placedFacing : Direction.NORTH;
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
         if (level.m_7702_(this.f_58858_) == this) {
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
                  level, this.origin, path, this.facing, this.cursor, budget - consumed, this.f_58858_
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
                  level, this.origin, path, this.facing, this.cursor, budget - consumed, this.f_58858_
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
         AncientWarfareCore.LOG.warn("Unable to advance survival AWS builder {} at {}", new Object[]{path, this.f_58858_, var7});
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
               BlockPos target = this.origin.m_121955_(rotateRelative(entry.relativePos(), this.facing));
               if (this.phase == 0) {
                  if (!entry.state().m_60795_() && entry.state().m_60734_() != Blocks.f_50454_) {
                     BlockState existing = level.m_8055_(target);
                     if (!target.equals(this.f_58858_)
                        && level.m_7702_(target) == null
                        && AwsTemplatePlacer.isBasicNaturalTerrain(existing)
                        && level.m_7731_(target, Blocks.f_50016_.m_49966_(), 18)) {
                        consumed++;
                     }
                  }
               } else if (!target.equals(this.f_58858_) && !entry.state().m_60795_() && entry.state().m_60734_() != Blocks.f_50454_) {
                  BlockState intended = entry.state().m_60717_(rotation);
                  BlockState existing = level.m_8055_(target);
                  if (!existing.equals(intended)
                     && (existing.m_60795_() || level.m_7702_(target) == null && AwsTemplatePlacer.isBasicNaturalTerrain(existing))
                     && level.m_7731_(target, intended, 18)) {
                     consumed++;
                     BlockEntity placed = level.m_7702_(target);
                     if (entry.blockEntityTag() != null && placed != null) {
                        CompoundTag tag = entry.blockEntityTag().m_6426_();
                        tag.m_128405_("x", target.m_123341_());
                        tag.m_128405_("y", target.m_123342_());
                        tag.m_128405_("z", target.m_123343_());
                        placed.m_142466_(tag);
                        placed.m_6596_();
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
                  template.blocks().forEach(entry -> positions.add(this.origin.m_121955_(rotateRelative(entry.relativePos(), this.facing)).m_121878_()));
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
      level.m_7471_(this.f_58858_, false);

      for (PlayerStructureTemplateData.TemplateBlock entry : template.blocks()) {
         BlockPos target = this.origin.m_121955_(rotateRelative(entry.relativePos(), this.facing));
         if (target.equals(this.f_58858_) && !entry.state().m_60795_() && entry.state().m_60734_() != Blocks.f_50454_) {
            level.m_7731_(target, entry.state().m_60717_(rotation), 18);
            BlockEntity placed = level.m_7702_(target);
            if (entry.blockEntityTag() != null && placed != null) {
               CompoundTag tag = entry.blockEntityTag().m_6426_();
               tag.m_128405_("x", target.m_123341_());
               tag.m_128405_("y", target.m_123342_());
               tag.m_128405_("z", target.m_123343_());
               placed.m_142466_(tag);
               placed.m_6596_();
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
      int percent = volume <= 0 ? 0 : Mth.m_14045_(Math.round((float)this.cursor * 100.0F / (float)volume), 0, 100);
      String stageKey = this.phase == 0 ? "message.ancientwarfare.structure_builder.clearing" : "message.ancientwarfare.structure_builder.building";
      return Component.m_237110_(
         "message.ancientwarfare.structure_builder.progress", new Object[]{Component.m_237115_(stageKey), percent, this.storedWorkEnergy}
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
         return this.f_58857_ instanceof ServerLevel serverLevel
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
            stack.m_41714_(Component.m_237115_("block.ancientwarfare.structure_builder_ticked").m_130946_(": ").m_7220_(Component.m_237113_(this.templateName)));
         }
      }

      SurvivalStructureBuilderItem.writeProgress(stack, this.templateName, this.phase, this.cursor, this.storedWorkEnergy);
      return stack;
   }

   @Override
   public boolean hasWork() {
      return this.f_58857_ != null
            && !this.f_58857_.f_46443_
            && !this.templateName.isBlank()
            && this.phase <= 2
            && this.f_58857_.m_7702_(this.f_58858_) == this
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
         this.m_6596_();
      }
   }

   @Override
   public NpcWorkType getWorkType() {
      return NpcWorkType.CRAFTING;
   }

   private void clampProgress() {
      this.phase = Mth.m_14045_(this.phase, 0, 2);
      this.cursor = Math.max(0, this.cursor);
      this.storedWorkEnergy = Mth.m_14045_(this.storedWorkEnergy, 0, 256);
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
         case EAST -> new BlockPos(-pos.m_123343_(), pos.m_123342_(), pos.m_123341_());
         case SOUTH -> new BlockPos(-pos.m_123341_(), pos.m_123342_(), -pos.m_123343_());
         case WEST -> new BlockPos(pos.m_123343_(), pos.m_123342_(), -pos.m_123341_());
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
      this.m_6596_();
      if (this.f_58857_ != null && !this.f_58857_.f_46443_) {
         this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 2);
      }
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

   public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
      CompoundTag tag = packet.m_131708_();
      if (tag != null) {
         this.m_142466_(tag);
      }
   }

   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      tag.m_128359_("TemplateName", this.templateName);
      tag.m_128405_("Phase", this.phase);
      tag.m_128405_("Cursor", this.cursor);
      tag.m_128405_("StoredWorkEnergy", this.storedWorkEnergy);
      tag.m_128356_("Origin", this.origin.m_121878_());
      tag.m_128344_("Facing", (byte)this.facing.m_122416_());
   }

   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      this.templateName = tag.m_128461_("TemplateName");
      this.phase = tag.m_128451_("Phase");
      this.cursor = tag.m_128451_("Cursor");
      this.storedWorkEnergy = tag.m_128451_("StoredWorkEnergy");
      this.origin = tag.m_128441_("Origin") ? BlockPos.m_122022_(tag.m_128454_("Origin")) : this.f_58858_;
      this.facing = tag.m_128441_("Facing") ? Direction.m_122407_(tag.m_128445_("Facing")) : Direction.NORTH;
      this.clampProgress();
   }
}
