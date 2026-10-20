package net.shadowmage.ancientwarfare.core.entity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.BardTrack;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;
import net.shadowmage.ancientwarfare.core.menu.NpcInventoryMenu;

public class BardNpcEntity extends PlayerOwnedNpcEntity {
   private static final EntityDataAccessor<String> BARD_TRACKS_DATA = SynchedEntityData.defineId(BardNpcEntity.class, EntityDataSerializers.STRING);
   private static final String TAG_BARD_TRACKS = "BardTracks";
   private static final int DEFAULT_PLAY_RANGE = 20;
   private final List<BardTrack> tracks = new ArrayList<>();

   public BardNpcEntity(EntityType<? extends BardNpcEntity> entityType, Level level) {
      super(entityType, level);
      this.setNpcType("bard");
   }

   public static Builder createAttributes() {
      return createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 24.0);
   }

   @Override
   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(BARD_TRACKS_DATA, "");
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(2, new BardNpcEntity.MoveToCommandGoal(this, 1.0, 2.0));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
   }

   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (this.tryAcceptFood(player, hand)) {
         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else if (player.getItemInHand(hand).getItem() instanceof CommandBatonItem) {
         if (!this.level().isClientSide && this.hasCommandPermission(player)) {
            CommandBatonItem.toggleNpcSelection(player.getItemInHand(hand), player, this);
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else {
         if (!this.level().isClientSide && this.hasCommandPermission(player)) {
            AWCorePlatform.openMenu(
               player,
               new SimpleMenuProvider((containerId, inventory, openedPlayer) -> new NpcInventoryMenu(containerId, inventory, this), this.getDisplayName()),
               buffer -> buffer.writeInt(this.getId())
            );
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
      }
   }

   @Override
   public String getNpcType() {
      return "bard";
   }

   @Override
   public String getNpcSubtype() {
      return "";
   }

   @Override
   public Component getTypeName() {
      return Component.translatable("entity.ancientwarfare.bard");
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      ListTag list = new ListTag();

      for (BardTrack track : this.tracks) {
         list.add(track.save());
      }

      tag.put("BardTracks", list);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.tracks.clear();
      if (tag.contains("BardTracks")) {
         ListTag list = tag.getList("BardTracks", 10);

         for (int i = 0; i < list.size(); i++) {
            this.tracks.add(BardTrack.load(list.getCompound(i)));
         }
      }

      this.syncTracksToEntityData();
   }

   public List<BardTrack> getBardTracks() {
      return this.level().isClientSide ? this.parseSyncedTracks() : List.copyOf(this.tracks);
   }

   public void setBardTracks(List<BardTrack> tracks) {
      this.tracks.clear();
      if (tracks != null) {
         this.tracks.addAll(tracks.stream().limit(32L).toList());
      }

      this.syncTracksToEntityData();
   }

   public int getBardPlayRange() {
      return 20;
   }

   private void syncTracksToEntityData() {
      ListTag list = new ListTag();

      for (BardTrack track : this.tracks) {
         list.add(track.save());
      }

      CompoundTag tag = new CompoundTag();
      tag.put("BardTracks", list);
      this.entityData.set(BARD_TRACKS_DATA, tag.toString());
   }

   private List<BardTrack> parseSyncedTracks() {
      String data = (String)this.entityData.get(BARD_TRACKS_DATA);
      if (data.isBlank()) {
         return List.of();
      } else {
         try {
            CompoundTag tag = TagParser.parseTag(data);
            ListTag list = tag.getList("BardTracks", 10);
            List<BardTrack> parsed = new ArrayList<>(list.size());

            for (int i = 0; i < list.size(); i++) {
               parsed.add(BardTrack.load(list.getCompound(i)));
            }

            return parsed;
         } catch (Exception var6) {
            return List.of();
         }
      }
   }

   private static class MoveToCommandGoal extends Goal {
      private final BardNpcEntity bard;
      private final double speedModifier;
      private final double stopDistanceSq;
      private BlockPos target;

      private MoveToCommandGoal(BardNpcEntity bard, double speedModifier, double stopDistance) {
         this.bard = bard;
         this.speedModifier = speedModifier;
         this.stopDistanceSq = stopDistance * stopDistance;
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (this.bard.getCurrentCommand() != NpcCommandType.MOVE) {
            return false;
         } else {
            this.target = this.bard.getCommandTarget();
            return !BlockPos.ZERO.equals(this.target) && this.distanceToTargetSq() > this.stopDistanceSq;
         }
      }

      public boolean canContinueToUse() {
         return this.bard.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() > this.stopDistanceSq;
      }

      public void tick() {
         this.bard.getLookControl().setLookAt((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5);
         this.bard
            .getNavigation()
            .moveTo(
               (double)this.target.getX() + 0.5,
               (double)this.target.getY(),
               (double)this.target.getZ() + 0.5,
               this.bard.playerWalkNavigationSpeed()
            );
      }

      public void stop() {
         if (this.bard.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() <= this.stopDistanceSq) {
            this.bard.clearCommand();
         }

         this.target = null;
         this.bard.getNavigation().stop();
      }

      private double distanceToTargetSq() {
         return this.bard.distanceToSqr((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5);
      }
   }
}
