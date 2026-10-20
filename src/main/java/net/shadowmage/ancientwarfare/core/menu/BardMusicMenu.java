package net.shadowmage.ancientwarfare.core.menu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.common.npc.BardTrack;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.entity.BardNpcEntity;

public class BardMusicMenu extends AbstractContainerMenu {
   private final int bardEntityId;
   private final List<BardTrack> tracks;
   private final BardNpcEntity bard;
   private boolean guiLockHeld;

   public BardMusicMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
      this(containerId, playerInventory, buffer.readVarInt(), readTracks(buffer), null);
   }

   public BardMusicMenu(int containerId, Inventory playerInventory, BardNpcEntity bard) {
      this(containerId, playerInventory, bard == null ? -1 : bard.getId(), bard == null ? List.of() : bard.getBardTracks(), bard);
   }

   private BardMusicMenu(int containerId, Inventory playerInventory, int bardEntityId, List<BardTrack> tracks, BardNpcEntity bard) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.BARD_MUSIC), containerId);
      this.bardEntityId = bardEntityId;
      this.tracks = List.copyOf(tracks);
      this.bard = bard;
      if (bard != null && !playerInventory.player.level().isClientSide) {
         bard.beginGuiInteraction(playerInventory.player);
         this.guiLockHeld = bard.isGuiInteractionPaused();
      }
   }

   public int getBardEntityId() {
      return this.bardEntityId;
   }

   public List<BardTrack> getTracks() {
      return this.tracks;
   }

   public ItemStack quickMoveStack(Player player, int index) {
      return ItemStack.EMPTY;
   }

   public boolean stillValid(Player player) {
      if (player.level().getEntity(this.bardEntityId) instanceof BardNpcEntity bard
         && bard.isAlive()
         && bard.hasCommandPermission(player)
         && bard.distanceToSqr(player) < 64.0) {
         return true;
      }

      return false;
   }

   public void removed(Player player) {
      super.removed(player);
      if (this.guiLockHeld && this.bard != null) {
         this.bard.endGuiInteraction(player);
         this.guiLockHeld = false;
      }
   }

   public static void writeTracks(FriendlyByteBuf buffer, int entityId, List<BardTrack> tracks) {
      buffer.writeVarInt(entityId);
      buffer.writeVarInt(Math.min(32, tracks.size()));

      for (BardTrack track : tracks.stream().limit(32L).toList()) {
         track.toNetwork(buffer);
      }
   }

   private static List<BardTrack> readTracks(FriendlyByteBuf buffer) {
      int count = Math.min(32, buffer.readVarInt());
      List<BardTrack> tracks = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         tracks.add(BardTrack.fromNetwork(buffer));
      }

      return tracks;
   }
}
