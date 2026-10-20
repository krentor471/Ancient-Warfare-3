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
   private static final EntityDataAccessor<String> BARD_TRACKS_DATA = SynchedEntityData.m_135353_(BardNpcEntity.class, EntityDataSerializers.f_135030_);
   private static final String TAG_BARD_TRACKS = "BardTracks";
   private static final int DEFAULT_PLAY_RANGE = 20;
   private final List<BardTrack> tracks = new ArrayList<>();

   public BardNpcEntity(EntityType<? extends BardNpcEntity> entityType, Level level) {
      super(entityType, level);
      this.setNpcType("bard");
   }

   public static Builder createAttributes() {
      return m_21552_().m_22268_(Attributes.f_22276_, 20.0).m_22268_(Attributes.f_22279_, 0.25).m_22268_(Attributes.f_22277_, 24.0);
   }

   @Override
   protected void m_8097_() {
      super.m_8097_();
      this.f_19804_.m_135372_(BARD_TRACKS_DATA, "");
   }

   protected void m_8099_() {
      this.f_21345_.m_25352_(0, new FloatGoal(this));
      this.f_21345_.m_25352_(2, new BardNpcEntity.MoveToCommandGoal(this, 1.0, 2.0));
      this.f_21345_.m_25352_(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.f_21345_.m_25352_(7, new RandomLookAroundGoal(this));
   }

   protected InteractionResult m_6071_(Player player, InteractionHand hand) {
      if (this.tryAcceptFood(player, hand)) {
         return InteractionResult.m_19078_(this.m_9236_().f_46443_);
      } else if (player.m_21120_(hand).m_41720_() instanceof CommandBatonItem) {
         if (!this.m_9236_().f_46443_ && this.hasCommandPermission(player)) {
            CommandBatonItem.toggleNpcSelection(player.m_21120_(hand), player, this);
         }

         return InteractionResult.m_19078_(this.m_9236_().f_46443_);
      } else {
         if (!this.m_9236_().f_46443_ && this.hasCommandPermission(player)) {
            AWCorePlatform.openMenu(
               player,
               new SimpleMenuProvider((containerId, inventory, openedPlayer) -> new NpcInventoryMenu(containerId, inventory, this), this.m_5446_()),
               buffer -> buffer.writeInt(this.m_19879_())
            );
         }

         return InteractionResult.m_19078_(this.m_9236_().f_46443_);
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
   public Component m_5677_() {
      return Component.m_237115_("entity.ancientwarfare.bard");
   }

   @Override
   public void m_7380_(CompoundTag tag) {
      super.m_7380_(tag);
      ListTag list = new ListTag();

      for (BardTrack track : this.tracks) {
         list.add(track.save());
      }

      tag.m_128365_("BardTracks", list);
   }

   @Override
   public void m_7378_(CompoundTag tag) {
      super.m_7378_(tag);
      this.tracks.clear();
      if (tag.m_128441_("BardTracks")) {
         ListTag list = tag.m_128437_("BardTracks", 10);

         for (int i = 0; i < list.size(); i++) {
            this.tracks.add(BardTrack.load(list.m_128728_(i)));
         }
      }

      this.syncTracksToEntityData();
   }

   public List<BardTrack> getBardTracks() {
      return this.m_9236_().f_46443_ ? this.parseSyncedTracks() : List.copyOf(this.tracks);
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
      tag.m_128365_("BardTracks", list);
      this.f_19804_.m_135381_(BARD_TRACKS_DATA, tag.toString());
   }

   private List<BardTrack> parseSyncedTracks() {
      String data = (String)this.f_19804_.m_135370_(BARD_TRACKS_DATA);
      if (data.isBlank()) {
         return List.of();
      } else {
         try {
            CompoundTag tag = TagParser.m_129359_(data);
            ListTag list = tag.m_128437_("BardTracks", 10);
            List<BardTrack> parsed = new ArrayList<>(list.size());

            for (int i = 0; i < list.size(); i++) {
               parsed.add(BardTrack.load(list.m_128728_(i)));
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
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         if (this.bard.getCurrentCommand() != NpcCommandType.MOVE) {
            return false;
         } else {
            this.target = this.bard.getCommandTarget();
            return !BlockPos.f_121853_.equals(this.target) && this.distanceToTargetSq() > this.stopDistanceSq;
         }
      }

      public boolean m_8045_() {
         return this.bard.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() > this.stopDistanceSq;
      }

      public void m_8037_() {
         this.bard.m_21563_().m_24946_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
         this.bard
            .m_21573_()
            .m_26519_(
               (double)this.target.m_123341_() + 0.5,
               (double)this.target.m_123342_(),
               (double)this.target.m_123343_() + 0.5,
               this.bard.playerWalkNavigationSpeed()
            );
      }

      public void m_8041_() {
         if (this.bard.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() <= this.stopDistanceSq) {
            this.bard.clearCommand();
         }

         this.target = null;
         this.bard.m_21573_().m_26573_();
      }

      private double distanceToTargetSq() {
         return this.bard.m_20275_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
      }
   }
}
