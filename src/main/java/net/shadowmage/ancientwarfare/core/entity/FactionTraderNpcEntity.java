package net.shadowmage.ancientwarfare.core.entity;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.shadowmage.ancientwarfare.core.common.npc.FactionNpcRole;
import net.shadowmage.ancientwarfare.core.common.npc.FactionTradeListRegistry;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.menu.FactionTradeMenu;

public class FactionTraderNpcEntity extends FactionNpcEntity {
   private static final String SAVED_TRADES_TAG = "AWFactionTrades";
   private List<FactionTradeListRegistry.FactionTradeEntry> customTrades = List.of();

   public FactionTraderNpcEntity(EntityType<? extends FactionTraderNpcEntity> entityType, Level level) {
      super(entityType, level, FactionNpcRole.TRADER);
   }

   public static Builder createAttributes() {
      return m_21552_().m_22268_(Attributes.f_22276_, 18.0).m_22268_(Attributes.f_22279_, 0.25).m_22268_(Attributes.f_22277_, 20.0);
   }

   protected void m_8099_() {
      this.addSharedGoals(false);
      this.f_21345_.m_25352_(1, new PanicGoal(this, 1.25));
   }

   @Override
   public void m_7380_(CompoundTag tag) {
      super.m_7380_(tag);
      if (!this.customTrades.isEmpty()) {
         FactionTradeListRegistry.writeSavedTrades(tag, "AWFactionTrades", this.customTrades);
      }
   }

   @Override
   public void m_7378_(CompoundTag tag) {
      super.m_7378_(tag);
      List<FactionTradeListRegistry.FactionTradeEntry> saved = FactionTradeListRegistry.readSavedTrades(tag, "AWFactionTrades");
      this.customTrades = saved.isEmpty() ? FactionTradeListRegistry.readLegacyTradeList(tag) : FactionTradeListRegistry.repairSavedGoldenAppleTrades(saved);
   }

   public List<FactionTradeListRegistry.FactionTradeEntry> getAvailableTrades() {
      return this.customTrades.isEmpty() ? FactionTradeListRegistry.tradesFor(this.getFactionName()) : FactionTradeListRegistry.copyTrades(this.customTrades);
   }

   protected InteractionResult m_6071_(Player player, InteractionHand hand) {
      if (!this.m_9236_().f_46443_) {
         List<FactionTradeListRegistry.FactionTradeEntry> trades = this.getAvailableTrades();
         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, openedPlayer) -> new FactionTradeMenu(containerId, inventory, this.m_19879_(), this.getFactionName(), trades),
               Component.m_237115_("entity.ancientwarfare.faction.trader")
            ),
            buffer -> {
               buffer.writeInt(this.m_19879_());
               buffer.m_130070_(this.getFactionName());
               FactionTradeListRegistry.writeTrades(buffer, trades);
            }
         );
      }

      return InteractionResult.m_19078_(this.m_9236_().f_46443_);
   }
}
