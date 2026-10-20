package net.shadowmage.ancientwarfare.core.compat;

import java.lang.reflect.Method;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.NpcWorkType;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkSite;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkerAccess;

public final class CreateHandCrankCompat {
   private static final String MOD_ID = "create";
   private static final ResourceLocation HAND_CRANK_ID = new ResourceLocation("create", "hand_crank");
   private static final String HAND_CRANK_BLOCK_ENTITY_CLASS = "com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity";
   private static Class<?> handCrankBlockEntityClass;
   private static Method turnMethod;
   private static boolean reflectionResolved;
   private static boolean linkageFailureLogged;

   private CreateHandCrankCompat() {
   }

   public static Optional<WorkSite> getWorkSite(Level level, BlockPos pos) {
      return !isHandCrank(level, pos) ? Optional.empty() : Optional.of(new CreateHandCrankCompat.HandCrankWorkSite(level, pos.immutable()));
   }

   private static boolean isHandCrank(Level level, BlockPos pos) {
      return level != null && pos != null && AWCorePlatform.isModLoaded("create")
         ? HAND_CRANK_ID.equals(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock())) && isCompatibleBlockEntity(level.getBlockEntity(pos))
         : false;
   }

   private static boolean isCompatibleBlockEntity(BlockEntity blockEntity) {
      resolveReflection();
      return blockEntity != null && handCrankBlockEntityClass != null && handCrankBlockEntityClass.isInstance(blockEntity) && turnMethod != null;
   }

   private static void turn(Level level, BlockPos pos) {
      if (level != null && !level.isClientSide && isHandCrank(level, pos)) {
         BlockEntity blockEntity = level.getBlockEntity(pos);

         try {
            turnMethod.invoke(blockEntity, false);
         } catch (LinkageError | ReflectiveOperationException var4) {
            if (!linkageFailureLogged) {
               linkageFailureLogged = true;
               AncientWarfareCore.LOG.error("Failed to operate Create hand crank at {}", pos, var4);
            }
         }
      }
   }

   private static void resolveReflection() {
      if (!reflectionResolved && AWCorePlatform.isModLoaded("create")) {
         reflectionResolved = true;

         try {
            handCrankBlockEntityClass = Class.forName("com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity");
            turnMethod = handCrankBlockEntityClass.getMethod("turn", boolean.class);
         } catch (LinkageError | ReflectiveOperationException var1) {
            handCrankBlockEntityClass = null;
            turnMethod = null;
            if (!linkageFailureLogged) {
               linkageFailureLogged = true;
               AncientWarfareCore.LOG.error("Create is installed, but its hand crank API could not be linked; craftsman operation is disabled", var1);
            }
         }
      }
   }

   private static record HandCrankWorkSite(Level level, BlockPos pos) implements WorkSite {
      @Override
      public boolean hasWork() {
         return CreateHandCrankCompat.isHandCrank(this.level, this.pos);
      }

      @Override
      public void onWorkerTick(WorkerAccess worker) {
         if (worker != null && worker.canWorkAt(NpcWorkType.CRAFTING)) {
            CreateHandCrankCompat.turn(this.level, this.pos);
         }
      }

      @Override
      public void addEnergyFromWorker(WorkerAccess worker) {
         this.onWorkerTick(worker);
      }

      @Override
      public NpcWorkType getWorkType() {
         return NpcWorkType.CRAFTING;
      }
   }
}
