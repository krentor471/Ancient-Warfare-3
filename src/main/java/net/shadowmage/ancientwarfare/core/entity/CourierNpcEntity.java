package net.shadowmage.ancientwarfare.core.entity;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.shadowmage.ancientwarfare.core.common.backpack.BackpackInventoryAccess;
import net.shadowmage.ancientwarfare.core.common.inventory.ItemHandlerAccess;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.npc.RoutingOrderData;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteInventoryAccess;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;
import net.shadowmage.ancientwarfare.core.menu.NpcInventoryMenu;
import net.shadowmage.ancientwarfare.core.tile.BasicWorksiteBlockEntity;

public class CourierNpcEntity extends PlayerOwnedNpcEntity {
   private static final String TAG_ROUTE_INDEX = "RouteIndex";
   private static final String TAG_WORK_TICKS = "CourierWorkTicks";
   private int routeIndex;
   private int workTicks;

   public CourierNpcEntity(EntityType<? extends CourierNpcEntity> entityType, Level level) {
      super(entityType, level);
      this.setNpcType("courier");
   }

   public static Builder createAttributes() {
      return m_21552_().m_22268_(Attributes.f_22276_, 20.0).m_22268_(Attributes.f_22279_, 0.27).m_22268_(Attributes.f_22277_, 24.0);
   }

   protected void m_8099_() {
      this.f_21345_.m_25352_(0, new FloatGoal(this));
      this.f_21345_.m_25352_(2, new CourierNpcEntity.MoveToCommandGoal(this, 1.0, 2.0));
      this.f_21345_.m_25352_(4, new CourierNpcEntity.CourierRouteGoal(this, 1.0, 2.0));
      this.f_21345_.m_25352_(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.f_21345_.m_25352_(7, new RandomLookAroundGoal(this));
   }

   @Override
   public void m_7380_(CompoundTag tag) {
      super.m_7380_(tag);
      tag.m_128405_("RouteIndex", this.routeIndex);
      tag.m_128405_("CourierWorkTicks", this.workTicks);
   }

   @Override
   public void m_7378_(CompoundTag tag) {
      super.m_7378_(tag);
      this.routeIndex = tag.m_128451_("RouteIndex");
      this.workTicks = tag.m_128451_("CourierWorkTicks");
   }

   @Override
   public void setOrdersStack(ItemStack stack) {
      super.setOrdersStack(stack);
      this.routeIndex = 0;
      this.workTicks = 0;
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
      return "courier";
   }

   @Override
   public String getNpcSubtype() {
      return "";
   }

   @Override
   public Component m_5677_() {
      return Component.m_237115_("entity.ancientwarfare.courier");
   }

   private static class CourierRouteGoal extends Goal {
      private static final int WORK_DELAY_TICKS = 10;
      private final CourierNpcEntity courier;
      private final double speedModifier;
      private final double stopDistanceSq;
      private BlockPos target;

      private CourierRouteGoal(CourierNpcEntity courier, double speedModifier, double stopDistance) {
         this.courier = courier;
         this.speedModifier = speedModifier;
         this.stopDistanceSq = stopDistance * stopDistance;
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         if (this.courier.getCurrentCommand() == NpcCommandType.NONE
            && AWCoreRegistryAccess.is(this.courier.getOrdersStack(), AWCoreIds.ROUTING_ORDER)
            && AWCoreRegistryAccess.is(this.courier.m_21205_(), AWCoreIds.BACKPACK)) {
            List<RoutingOrderData.RouteEntry> entries = RoutingOrderData.getEntries(this.courier.getOrdersStack());
            if (entries.isEmpty()) {
               return false;
            } else {
               if (this.courier.routeIndex < 0 || this.courier.routeIndex >= entries.size()) {
                  this.courier.routeIndex = 0;
               }

               this.target = entries.get(this.courier.routeIndex).pos();
               return true;
            }
         } else {
            return false;
         }
      }

      public boolean m_8045_() {
         return this.courier.getCurrentCommand() == NpcCommandType.NONE
            && AWCoreRegistryAccess.is(this.courier.getOrdersStack(), AWCoreIds.ROUTING_ORDER)
            && AWCoreRegistryAccess.is(this.courier.m_21205_(), AWCoreIds.BACKPACK)
            && !RoutingOrderData.getEntries(this.courier.getOrdersStack()).isEmpty();
      }

      public void m_8037_() {
         if (this.target == null) {
            this.advanceRoute();
         } else if (this.courier.m_20275_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_() + 0.5, (double)this.target.m_123343_() + 0.5)
            > this.stopDistanceSq) {
            this.courier.workTicks = 0;
            this.courier.m_21563_().m_24946_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
            this.courier
               .m_21573_()
               .m_26519_(
                  (double)this.target.m_123341_() + 0.5,
                  (double)this.target.m_123342_(),
                  (double)this.target.m_123343_() + 0.5,
                  this.courier.playerWalkNavigationSpeed()
               );
         } else {
            this.courier.m_21573_().m_26573_();
            if (++this.courier.workTicks >= 10) {
               this.courier.m_6674_(InteractionHand.MAIN_HAND);
               this.transferAtRoutePoint();
               this.advanceRoute();
            }
         }
      }

      public void m_8041_() {
         this.target = null;
         this.courier.workTicks = 0;
         this.courier.m_21573_().m_26573_();
      }

      private void transferAtRoutePoint() {
         List<RoutingOrderData.RouteEntry> entries = RoutingOrderData.getEntries(this.courier.getOrdersStack());
         if (!entries.isEmpty() && this.courier.routeIndex >= 0 && this.courier.routeIndex < entries.size()) {
            RoutingOrderData.RouteEntry entry = entries.get(this.courier.routeIndex);
            BlockEntity blockEntity = this.courier.m_9236_().m_7702_(entry.pos());
            if (blockEntity != null) {
               if (blockEntity instanceof BasicWorksiteBlockEntity worksite) {
                  this.transferWithWorksite(entry, worksite);
               } else {
                  ItemHandlerAccess targetInventory = AWCorePlatform.findItemHandler(blockEntity, entry.side());
                  if (targetInventory != null) {
                     this.transferWithHandler(entry, targetInventory);
                  }
               }
            }
         }
      }

      private void transferWithHandler(RoutingOrderData.RouteEntry entry, ItemHandlerAccess targetInventory) {
         SimpleContainer backpackInventory = BackpackInventoryAccess.load(this.courier.m_21205_());
         switch (entry.routeType()) {
            case FILL_TARGET_TO:
               fillTo(backpackInventory, targetInventory, entry);
               break;
            case FILL_COURIER_TO:
               fillTo(targetInventory, backpackInventory, entry);
               break;
            case DEPOSIT_ALL_OF:
               transferMatching(backpackInventory, targetInventory, entry, false);
               break;
            case WITHDRAW_ALL_OF:
               transferMatching(targetInventory, backpackInventory, entry, false);
               break;
            case DEPOSIT_ALL_EXCEPT:
               transferMatching(backpackInventory, targetInventory, entry, true);
               break;
            case WITHDRAW_ALL_EXCEPT:
               transferMatching(targetInventory, backpackInventory, entry, true);
               break;
            case DEPOSIT_RATIO:
               transferRatio(backpackInventory, targetInventory, entry);
               break;
            case WITHDRAW_RATIO:
               transferRatio(targetInventory, backpackInventory, entry);
               break;
            case DEPOSIT_EXACT:
               depositExact(backpackInventory, targetInventory, entry);
               break;
            case WITHDRAW_EXACT:
               withdrawExact(targetInventory, backpackInventory, entry);
               break;
            case FILL_MINIMUM:
               fillTargetMinimum(backpackInventory, targetInventory, entry);
               break;
            case TAKE_MINIMUM:
               fillCourierMinimum(targetInventory, backpackInventory, entry);
         }

         BackpackInventoryAccess.save(this.courier.m_21205_(), backpackInventory);
      }

      private void transferWithWorksite(RoutingOrderData.RouteEntry entry, BasicWorksiteBlockEntity worksite) {
         boolean extracting = switch (entry.routeType()) {
            case FILL_COURIER_TO, WITHDRAW_ALL_OF, WITHDRAW_ALL_EXCEPT, WITHDRAW_RATIO, WITHDRAW_EXACT, TAKE_MINIMUM -> true;
            default -> false;
         };
         ItemHandlerAccess requested = worksite.getItemHandler(entry.side());
         WorksiteInventoryAccess required = extracting ? WorksiteInventoryAccess.OUTPUT : WorksiteInventoryAccess.INPUT;
         if (worksite.getSideAccess(worksite.getRelativeSide(entry.side())) == required) {
            this.transferWithHandler(entry, requested);
         } else {
            for (Direction direction : Direction.values()) {
               if (direction != entry.side() && worksite.getSideAccess(worksite.getRelativeSide(direction)) == required) {
                  ItemHandlerAccess fallback = worksite.getItemHandler(direction);
                  this.transferWithHandler(entry, fallback);
                  return;
               }
            }
         }
      }

      private void advanceRoute() {
         List<RoutingOrderData.RouteEntry> entries = RoutingOrderData.getEntries(this.courier.getOrdersStack());
         this.courier.workTicks = 0;
         if (entries.isEmpty()) {
            this.target = null;
            this.courier.routeIndex = 0;
         } else {
            this.courier.routeIndex = (this.courier.routeIndex + 1) % entries.size();
            this.target = entries.get(this.courier.routeIndex).pos();
         }
      }

      private static void moveContainerToHandler(Container from, ItemHandlerAccess to) {
         for (int slot = 0; slot < from.m_6643_(); slot++) {
            ItemStack stack = from.m_8020_(slot);
            if (!stack.m_41619_()) {
               ItemStack remaining = insertIntoHandler(to, stack);
               from.m_6836_(slot, remaining);
               if (remaining.m_41613_() != stack.m_41613_()) {
                  return;
               }
            }
         }
      }

      private static void moveHandlerToContainer(ItemHandlerAccess from, Container to) {
         for (int slot = 0; slot < from.getSlots(); slot++) {
            ItemStack simulated = from.extractItem(slot, 64, true);
            if (!simulated.m_41619_()) {
               ItemStack remaining = insertIntoContainer(to, simulated);
               int moved = simulated.m_41613_() - remaining.m_41613_();
               if (moved > 0) {
                  from.extractItem(slot, moved, false);
                  return;
               }
            }
         }
      }

      private static ItemStack insertIntoHandler(ItemHandlerAccess handler, ItemStack stack) {
         ItemStack remaining = stack.m_41777_();

         for (int slot = 0; slot < handler.getSlots() && !remaining.m_41619_(); slot++) {
            remaining = handler.insertItem(slot, remaining, false);
         }

         return remaining;
      }

      private static void fillTo(Container from, ItemHandlerAccess to, RoutingOrderData.RouteEntry entry) {
         for (ItemStack filter : entry.filters()) {
            if (!filter.m_41619_()) {
               int missing = filter.m_41613_() - countMatching(to, filter, entry);
               if (missing > 0) {
                  moveMatchingContainerToHandler(from, to, filter, missing, entry);
               }
            }
         }
      }

      private static void fillTo(ItemHandlerAccess from, Container to, RoutingOrderData.RouteEntry entry) {
         for (ItemStack filter : entry.filters()) {
            if (!filter.m_41619_()) {
               int missing = filter.m_41613_() - countMatching(to, filter, entry);
               if (missing > 0) {
                  moveMatchingHandlerToContainer(from, to, filter, missing, entry);
               }
            }
         }
      }

      private static void transferMatching(Container from, ItemHandlerAccess to, RoutingOrderData.RouteEntry entry, boolean except) {
         for (int slot = 0; slot < from.m_6643_(); slot++) {
            ItemStack source = from.m_8020_(slot);
            if (!source.m_41619_() && (except || matchesAny(source, entry)) && (!except || !matchesAny(source, entry))) {
               ItemStack remaining = insertIntoHandler(to, source.m_41777_());
               int moved = source.m_41613_() - remaining.m_41613_();
               if (moved > 0) {
                  source.m_41774_(moved);
                  from.m_6836_(slot, source);
               }
            }
         }

         from.m_6596_();
      }

      private static void transferMatching(ItemHandlerAccess from, Container to, RoutingOrderData.RouteEntry entry, boolean except) {
         for (int slot = 0; slot < from.getSlots(); slot++) {
            ItemStack source = from.getStackInSlot(slot);
            if (!source.m_41619_() && (except || matchesAny(source, entry)) && (!except || !matchesAny(source, entry))) {
               ItemStack extracted = from.extractItem(slot, source.m_41613_(), true);
               ItemStack remaining = insertIntoContainer(to, extracted);
               int moved = extracted.m_41613_() - remaining.m_41613_();
               if (moved > 0) {
                  from.extractItem(slot, moved, false);
               }
            }
         }
      }

      private static void transferRatio(Container from, ItemHandlerAccess to, RoutingOrderData.RouteEntry entry) {
         for (ItemStack filter : entry.filters()) {
            if (!filter.m_41619_() && filter.m_41613_() > 0) {
               int available = countMatching(from, filter, entry);
               int amount = available / filter.m_41613_();
               if (amount > 0) {
                  moveMatchingContainerToHandler(from, to, filter, amount, entry);
               }
            }
         }
      }

      private static void transferRatio(ItemHandlerAccess from, Container to, RoutingOrderData.RouteEntry entry) {
         for (ItemStack filter : entry.filters()) {
            if (!filter.m_41619_() && filter.m_41613_() > 0) {
               int available = countMatching(from, filter, entry);
               int amount = available / filter.m_41613_();
               if (amount > 0) {
                  moveMatchingHandlerToContainer(from, to, filter, amount, entry);
               }
            }
         }
      }

      private static boolean matchesAny(ItemStack stack, RoutingOrderData.RouteEntry entry) {
         return entry.filters().stream().anyMatch(filter -> matchesFilter(stack, filter, entry));
      }

      private static void moveMatchingContainerToHandler(Container from, ItemHandlerAccess to, ItemStack filter, int amount, RoutingOrderData.RouteEntry entry) {
         int remaining = amount;

         for (int slot = 0; slot < from.m_6643_() && remaining > 0; slot++) {
            ItemStack source = from.m_8020_(slot);
            if (matchesFilter(source, filter, entry)) {
               ItemStack moving = source.m_255036_(Math.min(remaining, source.m_41613_()));
               ItemStack rejected = insertIntoHandler(to, moving);
               int moved = moving.m_41613_() - rejected.m_41613_();
               if (moved > 0) {
                  source.m_41774_(moved);
                  from.m_6836_(slot, source);
                  remaining -= moved;
               }
            }
         }

         from.m_6596_();
      }

      private static void moveMatchingHandlerToContainer(ItemHandlerAccess from, Container to, ItemStack filter, int amount, RoutingOrderData.RouteEntry entry) {
         int remaining = amount;

         for (int slot = 0; slot < from.getSlots() && remaining > 0; slot++) {
            ItemStack source = from.getStackInSlot(slot);
            if (matchesFilter(source, filter, entry)) {
               ItemStack extracted = from.extractItem(slot, Math.min(remaining, source.m_41613_()), true);
               ItemStack rejected = insertIntoContainer(to, extracted);
               int moved = extracted.m_41613_() - rejected.m_41613_();
               if (moved > 0) {
                  from.extractItem(slot, moved, false);
                  remaining -= moved;
               }
            }
         }
      }

      private static ItemStack insertIntoContainer(Container container, ItemStack stack) {
         ItemStack remaining = stack.m_41777_();

         for (int slot = 0; slot < container.m_6643_() && !remaining.m_41619_(); slot++) {
            ItemStack current = container.m_8020_(slot);
            if (current.m_41619_()) {
               int moved = Math.min(remaining.m_41613_(), remaining.m_41741_());
               ItemStack placed = remaining.m_41777_();
               placed.m_41764_(moved);
               container.m_6836_(slot, placed);
               remaining.m_41774_(moved);
            } else if (ItemStack.m_150942_(current, remaining) && current.m_41613_() < current.m_41741_()) {
               int moved = Math.min(remaining.m_41613_(), current.m_41741_() - current.m_41613_());
               current.m_41769_(moved);
               remaining.m_41774_(moved);
               container.m_6836_(slot, current);
            }
         }

         container.m_6596_();
         return remaining;
      }

      private static void depositExact(Container from, ItemHandlerAccess to, RoutingOrderData.RouteEntry entry) {
         for (ItemStack filter : entry.filters()) {
            int amount = filter.m_41613_();
            if (!filter.m_41619_() && amount > 0) {
               ItemStack candidate = findContainerCandidate(from, filter, amount, entry);
               if (!candidate.m_41619_() && handlerInsertCapacity(to, candidate) >= amount) {
                  moveExactContainerToHandler(from, to, candidate, amount);
               }
            }
         }
      }

      private static void withdrawExact(ItemHandlerAccess from, Container to, RoutingOrderData.RouteEntry entry) {
         for (ItemStack filter : entry.filters()) {
            int amount = filter.m_41613_();
            if (!filter.m_41619_() && amount > 0) {
               ItemStack candidate = findHandlerCandidate(from, filter, amount, entry);
               if (!candidate.m_41619_() && containerInsertCapacity(to, candidate) >= amount) {
                  moveExactHandlerToContainer(from, to, candidate, amount);
               }
            }
         }
      }

      private static void fillTargetMinimum(Container from, ItemHandlerAccess to, RoutingOrderData.RouteEntry entry) {
         for (ItemStack filter : entry.filters()) {
            if (!filter.m_41619_()) {
               int missing = filter.m_41613_() - countMatching(to, filter, entry);
               if (missing > 0) {
                  ItemStack candidate = findContainerCandidate(from, filter, missing, entry);
                  if (!candidate.m_41619_() && handlerInsertCapacity(to, candidate) >= missing) {
                     moveExactContainerToHandler(from, to, candidate, missing);
                  }
               }
            }
         }
      }

      private static void fillCourierMinimum(ItemHandlerAccess from, Container to, RoutingOrderData.RouteEntry entry) {
         for (ItemStack filter : entry.filters()) {
            if (!filter.m_41619_()) {
               int missing = filter.m_41613_() - countMatching(to, filter, entry);
               if (missing > 0) {
                  ItemStack candidate = findHandlerCandidate(from, filter, missing, entry);
                  if (!candidate.m_41619_() && containerInsertCapacity(to, candidate) >= missing) {
                     moveExactHandlerToContainer(from, to, candidate, missing);
                  }
               }
            }
         }
      }

      private static ItemStack findContainerCandidate(Container container, ItemStack filter, int amount, RoutingOrderData.RouteEntry entry) {
         for (int slot = 0; slot < container.m_6643_(); slot++) {
            ItemStack candidate = container.m_8020_(slot);
            if (matchesFilter(candidate, filter, entry) && countExact(container, candidate) >= amount) {
               return candidate.m_255036_(amount);
            }
         }

         return ItemStack.f_41583_;
      }

      private static ItemStack findHandlerCandidate(ItemHandlerAccess handler, ItemStack filter, int amount, RoutingOrderData.RouteEntry entry) {
         for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack candidate = handler.getStackInSlot(slot);
            if (matchesFilter(candidate, filter, entry) && countExtractableExact(handler, candidate, amount) >= amount) {
               return candidate.m_255036_(amount);
            }
         }

         return ItemStack.f_41583_;
      }

      private static int countMatching(ItemHandlerAccess handler, ItemStack filter, RoutingOrderData.RouteEntry entry) {
         int count = 0;

         for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (matchesFilter(stack, filter, entry)) {
               count += stack.m_41613_();
            }
         }

         return count;
      }

      private static int countMatching(Container container, ItemStack filter, RoutingOrderData.RouteEntry entry) {
         int count = 0;

         for (int slot = 0; slot < container.m_6643_(); slot++) {
            ItemStack stack = container.m_8020_(slot);
            if (matchesFilter(stack, filter, entry)) {
               count += stack.m_41613_();
            }
         }

         return count;
      }

      private static int countExact(Container container, ItemStack filter) {
         int count = 0;

         for (int slot = 0; slot < container.m_6643_(); slot++) {
            ItemStack stack = container.m_8020_(slot);
            if (ItemStack.m_150942_(stack, filter)) {
               count += stack.m_41613_();
            }
         }

         return count;
      }

      private static int countExtractableExact(ItemHandlerAccess handler, ItemStack filter, int limit) {
         int count = 0;

         for (int slot = 0; slot < handler.getSlots() && count < limit; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (ItemStack.m_150942_(stack, filter)) {
               count += handler.extractItem(slot, limit - count, true).m_41613_();
            }
         }

         return count;
      }

      private static int handlerInsertCapacity(ItemHandlerAccess handler, ItemStack stack) {
         ItemStack remaining = stack.m_41777_();

         for (int slot = 0; slot < handler.getSlots() && !remaining.m_41619_(); slot++) {
            remaining = handler.insertItem(slot, remaining, true);
         }

         return stack.m_41613_() - remaining.m_41613_();
      }

      private static int containerInsertCapacity(Container container, ItemStack stack) {
         int capacity = 0;

         for (int slot = 0; slot < container.m_6643_(); slot++) {
            ItemStack current = container.m_8020_(slot);
            if (current.m_41619_()) {
               capacity += Math.min(stack.m_41741_(), container.m_6893_());
            } else if (ItemStack.m_150942_(current, stack)) {
               capacity += Math.max(0, Math.min(current.m_41741_(), container.m_6893_()) - current.m_41613_());
            }

            if (capacity >= stack.m_41613_()) {
               return capacity;
            }
         }

         return capacity;
      }

      private static void moveExactContainerToHandler(Container from, ItemHandlerAccess to, ItemStack filter, int amount) {
         int remaining = amount;

         for (int slot = 0; slot < from.m_6643_() && remaining > 0; slot++) {
            ItemStack source = from.m_8020_(slot);
            if (ItemStack.m_150942_(source, filter)) {
               int requested = Math.min(remaining, source.m_41613_());
               ItemStack moving = source.m_255036_(requested);
               ItemStack rejected = insertIntoHandler(to, moving);
               int moved = requested - rejected.m_41613_();
               if (moved > 0) {
                  source.m_41774_(moved);
                  from.m_6836_(slot, source);
                  remaining -= moved;
               }
            }
         }

         from.m_6596_();
      }

      private static void moveExactHandlerToContainer(ItemHandlerAccess from, Container to, ItemStack filter, int amount) {
         int remaining = amount;

         for (int slot = 0; slot < from.getSlots() && remaining > 0; slot++) {
            ItemStack source = from.getStackInSlot(slot);
            if (ItemStack.m_150942_(source, filter)) {
               ItemStack extracted = from.extractItem(slot, remaining, false);
               if (!extracted.m_41619_()) {
                  ItemStack rejected = insertIntoContainer(to, extracted);
                  int moved = extracted.m_41613_() - rejected.m_41613_();
                  remaining -= moved;
                  if (!rejected.m_41619_()) {
                     for (int restoreSlot = 0; restoreSlot < from.getSlots() && !rejected.m_41619_(); restoreSlot++) {
                        rejected = from.insertItem(restoreSlot, rejected, false);
                     }
                  }
               }
            }
         }
      }

      private static boolean matchesFilter(ItemStack stack, ItemStack filter, RoutingOrderData.RouteEntry entry) {
         if (stack.m_41619_() || filter.m_41619_() || stack.m_41720_() != filter.m_41720_()) {
            return false;
         } else if (!entry.ignoreDamage() && stack.m_41773_() != filter.m_41773_()) {
            return false;
         } else if (entry.ignoreTag()) {
            return true;
         } else {
            CompoundTag stackTag = comparableTag(stack, entry.ignoreDamage());
            CompoundTag filterTag = comparableTag(filter, entry.ignoreDamage());
            return stackTag == null ? filterTag == null : stackTag.equals(filterTag);
         }
      }

      private static CompoundTag comparableTag(ItemStack stack, boolean ignoreDamage) {
         CompoundTag tag = stack.m_41783_();
         if (tag == null) {
            return null;
         } else {
            CompoundTag copy = tag.m_6426_();
            if (ignoreDamage) {
               copy.m_128473_("Damage");
            }

            return copy.m_128456_() ? null : copy;
         }
      }
   }

   private static class MoveToCommandGoal extends Goal {
      private final CourierNpcEntity courier;
      private final double speedModifier;
      private final double stopDistanceSq;
      private BlockPos target;

      private MoveToCommandGoal(CourierNpcEntity courier, double speedModifier, double stopDistance) {
         this.courier = courier;
         this.speedModifier = speedModifier;
         this.stopDistanceSq = stopDistance * stopDistance;
         this.m_7021_(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean m_8036_() {
         if (this.courier.getCurrentCommand() != NpcCommandType.MOVE) {
            return false;
         } else {
            this.target = this.courier.getCommandTarget();
            return !BlockPos.f_121853_.equals(this.target) && this.distanceToTargetSq() > this.stopDistanceSq;
         }
      }

      public boolean m_8045_() {
         return this.courier.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() > this.stopDistanceSq;
      }

      public void m_8037_() {
         this.courier.m_21563_().m_24946_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
         this.courier
            .m_21573_()
            .m_26519_(
               (double)this.target.m_123341_() + 0.5,
               (double)this.target.m_123342_(),
               (double)this.target.m_123343_() + 0.5,
               this.courier.playerWalkNavigationSpeed()
            );
      }

      public void m_8041_() {
         if (this.courier.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() <= this.stopDistanceSq) {
            this.courier.clearCommand();
         }

         this.target = null;
         this.courier.m_21573_().m_26573_();
      }

      private double distanceToTargetSq() {
         return this.courier.m_20275_((double)this.target.m_123341_() + 0.5, (double)this.target.m_123342_(), (double)this.target.m_123343_() + 0.5);
      }
   }
}
