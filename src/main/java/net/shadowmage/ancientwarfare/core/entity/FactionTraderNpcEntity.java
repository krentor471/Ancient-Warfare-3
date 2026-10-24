package net.shadowmage.ancientwarfare.core.entity;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.shadowmage.ancientwarfare.core.faction.PlayerDiplomacy;
import net.shadowmage.ancientwarfare.core.menu.FactionTradeMenu;
import net.shadowmage.ancientwarfare.core.faction.FactionRulers;
import net.shadowmage.ancientwarfare.core.faction.FactionWorldData;
import net.shadowmage.ancientwarfare.core.faction.TownMarkets;
import net.minecraft.server.level.ServerLevel;

public class FactionTraderNpcEntity extends FactionNpcEntity {
   private static final String SAVED_TRADES_TAG = "AWFactionTrades";
   private List<FactionTradeListRegistry.FactionTradeEntry> customTrades = List.of();

   public FactionTraderNpcEntity(EntityType<? extends FactionTraderNpcEntity> entityType, Level level) {
      super(entityType, level, FactionNpcRole.TRADER);
   }

   public static Builder createAttributes() {
      return createMobAttributes().add(Attributes.MAX_HEALTH, 18.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 20.0);
   }

   protected void registerGoals() {
      this.addSharedGoals(false);
      this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      if (!this.customTrades.isEmpty()) {
         FactionTradeListRegistry.writeSavedTrades(tag, "AWFactionTrades", this.customTrades);
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      List<FactionTradeListRegistry.FactionTradeEntry> saved = FactionTradeListRegistry.readSavedTrades(tag, "AWFactionTrades");
      this.customTrades = saved.isEmpty() ? FactionTradeListRegistry.readLegacyTradeList(tag) : FactionTradeListRegistry.repairSavedGoldenAppleTrades(saved);
   }

   public List<FactionTradeListRegistry.FactionTradeEntry> getAvailableTrades() {
      return this.customTrades.isEmpty() ? FactionTradeListRegistry.tradesFor(this.getFactionName()) : FactionTradeListRegistry.copyTrades(this.customTrades);
   }

   protected InteractionResult mobInteract(Player player, InteractionHand hand) {
      if (player instanceof ServerPlayer serverPlayer) {
         PlayerDiplomacy.discover(serverPlayer, this.getFactionName());
      }

      if (!this.level().isClientSide) {
         ServerLevel server = (ServerLevel)this.level();
         List<FactionTradeListRegistry.FactionTradeEntry> trades = TownMarkets.price(
            server,
            this.getX(),
            this.getZ(),
            FactionRulers.price(
               server,
               this.getFactionName(),
               this.getAvailableTrades(),
               net.shadowmage.ancientwarfare.core.faction.FactionTownLife.priceFactor(server, this.getFactionName(), this.getX(), this.getZ())
            )
         );
         // The trader says what is dear and what is cheap in his town today.
         FactionWorldData.Settlement town = TownMarkets.townAt(server, this.getX(), this.getZ());
         Component prices = town == null ? null : TownMarkets.prices(server, town);
         if (prices != null) {
            player.sendSystemMessage(
               Component.translatable("chatter.ancientwarfare.say", this.getDisplayName().copy().withStyle(net.minecraft.ChatFormatting.GOLD), prices)
                  .withStyle(net.minecraft.ChatFormatting.GRAY)
            );
         }

         AWCorePlatform.openMenu(
            player,
            new SimpleMenuProvider(
               (containerId, inventory, openedPlayer) -> new FactionTradeMenu(containerId, inventory, this.getId(), this.getFactionName(), trades),
               Component.translatable("entity.ancientwarfare.faction.trader")
            ),
            buffer -> {
               buffer.writeInt(this.getId());
               buffer.writeUtf(this.getFactionName());
               FactionTradeListRegistry.writeTrades(buffer, trades);
            }
         );
      }

      return InteractionResult.sidedSuccess(this.level().isClientSide);
   }
}
