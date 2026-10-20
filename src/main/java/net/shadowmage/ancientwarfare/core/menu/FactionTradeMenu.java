package net.shadowmage.ancientwarfare.core.menu;

import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.FactionRegistry;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;
import net.shadowmage.ancientwarfare.core.common.npc.FactionTradeListRegistry;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.entity.FactionTraderNpcEntity;
import net.shadowmage.ancientwarfare.core.item.CoinItem;

public class FactionTradeMenu extends AbstractContainerMenu {
   public static final int BUTTON_TRADE_BASE = 0;
   private final Inventory playerInventory;
   private final int traderEntityId;
   private final String faction;
   private final List<FactionTradeListRegistry.FactionTradeEntry> trades;
   private boolean invalidMenuLogged;

   public FactionTradeMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
      this(containerId, playerInventory, buffer.readInt(), buffer.m_130277_(), FactionTradeListRegistry.readTrades(buffer));
   }

   public FactionTradeMenu(
      int containerId, Inventory playerInventory, int traderEntityId, String faction, List<FactionTradeListRegistry.FactionTradeEntry> trades
   ) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.FACTION_TRADE_MENU), containerId);
      this.playerInventory = playerInventory;
      this.traderEntityId = traderEntityId;
      this.faction = faction;
      this.trades = FactionTradeListRegistry.copyTrades(trades.stream().limit(128L).toList());
      this.addPlayerInventorySlots(playerInventory, 8, 160);
   }

   public List<FactionTradeListRegistry.FactionTradeEntry> getTrades() {
      return this.trades;
   }

   public boolean canTrade(int index) {
      return FactionTradeListRegistry.canTrade(this.trades, index, this.playerInventory.f_35978_);
   }

   public boolean m_6366_(Player player, int id) {
      if (player instanceof ServerPlayer serverPlayer) {
         int index = id - 0;
         if (player.f_36096_ != this || player.m_5833_()) {
            this.logTrade(serverPlayer, index, "REJECTED", player.m_5833_() ? "SPECTATOR" : "MENU_MISMATCH");
            return false;
         } else if (!this.m_6875_(player)) {
            return false;
         } else {
            FactionTradeListRegistry.TradeResult result = FactionTradeListRegistry.performTradeWithResult(this.trades, index, player);
            if (result == FactionTradeListRegistry.TradeResult.SUCCESS) {
               FactionStandingData.get(serverPlayer.m_284548_())
                  .adjustStanding(player.m_20148_(), this.faction, FactionRegistry.get(this.faction).tradeStandingChange());
            }

            this.logTrade(serverPlayer, index, result == FactionTradeListRegistry.TradeResult.SUCCESS ? "SUCCESS" : "REJECTED", result.name());
            return result == FactionTradeListRegistry.TradeResult.SUCCESS;
         }
      } else {
         return false;
      }
   }

   public boolean m_6875_(Player player) {
      Entity entity = player.m_9236_().m_6815_(this.traderEntityId);
      String reason = !(entity instanceof FactionTraderNpcEntity)
         ? "TRADER_MISSING"
         : (!entity.m_6084_() ? "TRADER_DEAD" : (player.m_20280_(entity) >= 64.0 ? "TOO_FAR" : null));
      if (reason != null && !this.invalidMenuLogged && player instanceof ServerPlayer serverPlayer) {
         this.invalidMenuLogged = true;
         this.logTrade(serverPlayer, -1, "MENU_INVALID", reason);
      }

      return reason == null;
   }

   public ItemStack m_7648_(Player player, int index) {
      return ItemStack.f_41583_;
   }

   public static int tradeButton(int index) {
      if (index >= 0 && index < 128) {
         return 0 + index;
      } else {
         throw new IllegalArgumentException("Faction trade index must fit in a signed byte: " + index);
      }
   }

   private void logTrade(ServerPlayer player, int index, String result, String reason) {
      Entity trader = player.m_9236_().m_6815_(this.traderEntityId);
      FactionTradeListRegistry.FactionTradeEntry trade = index >= 0 && index < this.trades.size() ? this.trades.get(index) : null;
      AncientWarfareCore.LOG
         .info(
            "[AW FactionTrade] result={} reason={} player={} uuid={} traderId={} traderUuid={} faction={} dimension={} traderPos={} menu={} tradeIndex={} inputs={} outputs={}",
            new Object[]{
               result,
               reason,
               player.m_36316_().getName(),
               player.m_20148_(),
               this.traderEntityId,
               trader == null ? "missing" : trader.m_20148_(),
               this.faction.replace('\n', ' ').replace('\r', ' '),
               player.m_9236_().m_46472_().m_135782_(),
               trader == null ? "missing" : trader.m_20183_().m_123344_(),
               this.f_38840_,
               index,
               trade == null ? "[]" : describeStacks(trade.inputs()),
               trade == null ? "[]" : describeStacks(trade.outputs())
            }
         );
   }

   private static String describeStacks(List<ItemStack> stacks) {
      return stacks.stream()
         .filter(stack -> !stack.m_41619_())
         .map(
            stack -> BuiltInRegistries.f_257033_.m_7981_(stack.m_41720_())
                  + "x"
                  + stack.m_41613_()
                  + (stack.m_41720_() instanceof CoinItem ? "(metal=" + CoinItem.getMetal(stack) + ")" : "")
         )
         .collect(Collectors.joining(",", "[", "]"));
   }

   private void addPlayerInventorySlots(Inventory inventory, int x, int y) {
      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.m_38897_(new Slot(inventory, col + row * 9 + 9, x + col * 18, y + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.m_38897_(new Slot(inventory, col, x + col * 18, y + 58));
      }
   }
}
