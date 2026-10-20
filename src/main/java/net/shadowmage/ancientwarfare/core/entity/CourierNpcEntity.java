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
      return createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.FOLLOW_RANGE, 24.0);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(2, new CourierNpcEntity.MoveToCommandGoal(this, 1.0, 2.0));
      this.goalSelector.addGoal(4, new CourierNpcEntity.CourierRouteGoal(this, 1.0, 2.0));
      this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("RouteIndex", this.routeIndex);
      tag.putInt("CourierWorkTicks", this.workTicks);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.routeIndex = tag.getInt("RouteIndex");
      this.workTicks = tag.getInt("CourierWorkTicks");
   }

   @Override
   public void setOrdersStack(ItemStack stack) {
      super.setOrdersStack(stack);
      this.routeIndex = 0;
      this.workTicks = 0;
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
      return "courier";
   }

   @Override
   public String getNpcSubtype() {
      return "";
   }

   @Override
   public Component getTypeName() {
      return Component.translatable("entity.ancientwarfare.courier");
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
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (this.courier.getCurrentCommand() == NpcCommandType.NONE
            && AWCoreRegistryAccess.is(this.courier.getOrdersStack(), AWCoreIds.ROUTING_ORDER)
            && AWCoreRegistryAccess.is(this.courier.getMainHandItem(), AWCoreIds.BACKPACK)) {
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

      public boolean canContinueToUse() {
         return this.courier.getCurrentCommand() == NpcCommandType.NONE
            && AWCoreRegistryAccess.is(this.courier.getOrdersStack(), AWCoreIds.ROUTING_ORDER)
            && AWCoreRegistryAccess.is(this.courier.getMainHandItem(), AWCoreIds.BACKPACK)
            && !RoutingOrderData.getEntries(this.courier.getOrdersStack()).isEmpty();
      }

      public void tick() {
         if (this.target == null) {
            this.advanceRoute();
         } else if (this.courier.distanceToSqr((double)this.target.getX() + 0.5, (double)this.target.getY() + 0.5, (double)this.target.getZ() + 0.5)
            > this.stopDistanceSq) {
            this.courier.workTicks = 0;
            this.courier.getLookControl().setLookAt((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5);
            this.courier
               .getNavigation()
               .moveTo(
                  (double)this.target.getX() + 0.5,
                  (double)this.target.getY(),
                  (double)this.target.getZ() + 0.5,
                  this.courier.playerWalkNavigationSpeed()
               );
         } else {
            this.courier.getNavigation().stop();
            if (++this.courier.workTicks >= 10) {
               this.courier.swing(InteractionHand.MAIN_HAND);
               this.transferAtRoutePoint();
               this.advanceRoute();
            }
         }
      }

      public void stop() {
         this.target = null;
         this.courier.workTicks = 0;
         this.courier.getNavigation().stop();
      }

      private void transferAtRoutePoint() {
         List<RoutingOrderData.RouteEntry> entries = RoutingOrderData.getEntries(this.courier.getOrdersStack());
         if (!entries.isEmpty() && this.courier.routeIndex >= 0 && this.courier.routeIndex < entries.size()) {
            RoutingOrderData.RouteEntry entry = entries.get(this.courier.routeIndex);
            BlockEntity blockEntity = this.courier.level().getBlockEntity(entry.pos());
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
         SimpleContainer backpackInventory = BackpackInventoryAccess.load(this.courier.getMainHandItem());
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

         BackpackInventoryAccess.save(this.courier.getMainHandItem(), backpackInventory);
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
         for (int slot = 0; slot < from.getContainerSize(); slot++) {
            ItemStack stack = from.getItem(slot);
            if (!stack.isEmpty()) {
               ItemStack remaining = insertIntoHandler(to, stack);
               from.setItem(slot, remaining);
               if (remaining.getCount() != stack.getCount()) {
                  return;
               }
            }
         }
      }

      private static void moveHandlerToContainer(ItemHandlerAccess from, Container to) {
         for (int slot = 0; slot < from.getSlots(); slot++) {
            ItemStack simulated = from.extractItem(slot, 64, true);
            if (!simulated.isEmpty()) {
               ItemStack remaining = insertIntoContainer(to, simulated);
               int moved = simulated.getCount() - remaining.getCount();
               if (moved > 0) {
                  from.extractItem(slot, moved, false);
                  return;
               }
            }
         }
      }

      private static ItemStack insertIntoHandler(ItemHandlerAccess handler, ItemStack stack) {
         ItemStack remaining = stack.copy();

         for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, false);
         }

         return remaining;
      }

      private static void fillTo(Container from, ItemHandlerAccess to, RoutingOrderData.RouteEntry entry) {
         for (ItemStack filter : entry.filters()) {
            if (!filter.isEmpty()) {
               int missing = filter.getCount() - countMatching(to, filter, entry);
               if (missing > 0) {
                  moveMatchingContainerToHandler(from, to, filter, missing, entry);
               }
            }
         }
      }

      private static void fillTo(ItemHandlerAccess from, Container to, RoutingOrderData.RouteEntry entry) {
         for (ItemStack filter : entry.filters()) {
            if (!filter.isEmpty()) {
               int missing = filter.getCount() - countMatching(to, filter, entry);
               if (missing > 0) {
                  moveMatchingHandlerToContainer(from, to, filter, missing, entry);
               }
            }
         }
      }

      private static void transferMatching(Container from, ItemHandlerAccess to, RoutingOrderData.RouteEntry entry, boolean except) {
         for (int slot = 0; slot < from.getContainerSize(); slot++) {
            ItemStack source = from.getItem(slot);
            if (!source.isEmpty() && (except || matchesAny(source, entry)) && (!except || !matchesAny(source, entry))) {
               ItemStack remaining = insertIntoHandler(to, source.copy());
               int moved = source.getCount() - remaining.getCount();
               if (moved > 0) {
                  source.shrink(moved);
                  from.setItem(slot, source);
               }
            }
         }

         from.setChanged();
      }

      private static void transferMatching(ItemHandlerAccess from, Container to, RoutingOrderData.RouteEntry entry, boolean except) {
         for (int slot = 0; slot < from.getSlots(); slot++) {
            ItemStack source = from.getStackInSlot(slot);
            if (!source.isEmpty() && (except || matchesAny(source, entry)) && (!except || !matchesAny(source, entry))) {
               ItemStack extracted = from.extractItem(slot, source.getCount(), true);
               ItemStack remaining = insertIntoContainer(to, extracted);
               int moved = extracted.getCount() - remaining.getCount();
               if (moved > 0) {
                  from.extractItem(slot, moved, false);
               }
            }
         }
      }

      private static void transferRatio(Container from, ItemHandlerAccess to, RoutingOrderData.RouteEntry entry) {
         for (ItemStack filter : entry.filters()) {
            if (!filter.isEmpty() && filter.getCount() > 0) {
               int available = countMatching(from, filter, entry);
               int amount = available / filter.getCount();
               if (amount > 0) {
                  moveMatchingContainerToHandler(from, to, filter, amount, entry);
               }
            }
         }
      }

      private static void transferRatio(ItemHandlerAccess from, Container to, RoutingOrderData.RouteEntry entry) {
         for (ItemStack filter : entry.filters()) {
            if (!filter.isEmpty() && filter.getCount() > 0) {
               int available = countMatching(from, filter, entry);
               int amount = available / filter.getCount();
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

         for (int slot = 0; slot < from.getContainerSize() && remaining > 0; slot++) {
            ItemStack source = from.getItem(slot);
            if (matchesFilter(source, filter, entry)) {
               ItemStack moving = source.copyWithCount(Math.min(remaining, source.getCount()));
               ItemStack rejected = insertIntoHandler(to, moving);
               int moved = moving.getCount() - rejected.getCount();
               if (moved > 0) {
                  source.shrink(moved);
                  from.setItem(slot, source);
                  remaining -= moved;
               }
            }
         }

         from.setChanged();
      }

      private static void moveMatchingHandlerToContainer(ItemHandlerAccess from, Container to, ItemStack filter, int amount, RoutingOrderData.RouteEntry entry) {
         int remaining = amount;

         for (int slot = 0; slot < from.getSlots() && remaining > 0; slot++) {
            ItemStack source = from.getStackInSlot(slot);
            if (matchesFilter(source, filter, entry)) {
               ItemStack extracted = from.extractItem(slot, Math.min(remaining, source.getCount()), true);
               ItemStack rejected = insertIntoContainer(to, extracted);
               int moved = extracted.getCount() - rejected.getCount();
               if (moved > 0) {
                  from.extractItem(slot, moved, false);
                  remaining -= moved;
               }
            }
         }
      }

      private static ItemStack insertIntoContainer(Container container, ItemStack stack) {
         ItemStack remaining = stack.copy();

         for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack current = container.getItem(slot);
            if (current.isEmpty()) {
               int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
               ItemStack placed = remaining.copy();
               placed.setCount(moved);
               container.setItem(slot, placed);
               remaining.shrink(moved);
            } else if (ItemStack.isSameItemSameTags(current, remaining) && current.getCount() < current.getMaxStackSize()) {
               int moved = Math.min(remaining.getCount(), current.getMaxStackSize() - current.getCount());
               current.grow(moved);
               remaining.shrink(moved);
               container.setItem(slot, current);
            }
         }

         container.setChanged();
         return remaining;
      }

      private static void depositExact(Container from, ItemHandlerAccess to, RoutingOrderData.RouteEntry entry) {
         for (ItemStack filter : entry.filters()) {
            int amount = filter.getCount();
            if (!filter.isEmpty() && amount > 0) {
               ItemStack candidate = findContainerCandidate(from, filter, amount, entry);
               if (!candidate.isEmpty() && handlerInsertCapacity(to, candidate) >= amount) {
                  moveExactContainerToHandler(from, to, candidate, amount);
               }
            }
         }
      }

      private static void withdrawExact(ItemHandlerAccess from, Container to, RoutingOrderData.RouteEntry entry) {
         for (ItemStack filter : entry.filters()) {
            int amount = filter.getCount();
            if (!filter.isEmpty() && amount > 0) {
               ItemStack candidate = findHandlerCandidate(from, filter, amount, entry);
               if (!candidate.isEmpty() && containerInsertCapacity(to, candidate) >= amount) {
                  moveExactHandlerToContainer(from, to, candidate, amount);
               }
            }
         }
      }

      private static void fillTargetMinimum(Container from, ItemHandlerAccess to, RoutingOrderData.RouteEntry entry) {
         for (ItemStack filter : entry.filters()) {
            if (!filter.isEmpty()) {
               int missing = filter.getCount() - countMatching(to, filter, entry);
               if (missing > 0) {
                  ItemStack candidate = findContainerCandidate(from, filter, missing, entry);
                  if (!candidate.isEmpty() && handlerInsertCapacity(to, candidate) >= missing) {
                     moveExactContainerToHandler(from, to, candidate, missing);
                  }
               }
            }
         }
      }

      private static void fillCourierMinimum(ItemHandlerAccess from, Container to, RoutingOrderData.RouteEntry entry) {
         for (ItemStack filter : entry.filters()) {
            if (!filter.isEmpty()) {
               int missing = filter.getCount() - countMatching(to, filter, entry);
               if (missing > 0) {
                  ItemStack candidate = findHandlerCandidate(from, filter, missing, entry);
                  if (!candidate.isEmpty() && containerInsertCapacity(to, candidate) >= missing) {
                     moveExactHandlerToContainer(from, to, candidate, missing);
                  }
               }
            }
         }
      }

      private static ItemStack findContainerCandidate(Container container, ItemStack filter, int amount, RoutingOrderData.RouteEntry entry) {
         for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack candidate = container.getItem(slot);
            if (matchesFilter(candidate, filter, entry) && countExact(container, candidate) >= amount) {
               return candidate.copyWithCount(amount);
            }
         }

         return ItemStack.EMPTY;
      }

      private static ItemStack findHandlerCandidate(ItemHandlerAccess handler, ItemStack filter, int amount, RoutingOrderData.RouteEntry entry) {
         for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack candidate = handler.getStackInSlot(slot);
            if (matchesFilter(candidate, filter, entry) && countExtractableExact(handler, candidate, amount) >= amount) {
               return candidate.copyWithCount(amount);
            }
         }

         return ItemStack.EMPTY;
      }

      private static int countMatching(ItemHandlerAccess handler, ItemStack filter, RoutingOrderData.RouteEntry entry) {
         int count = 0;

         for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (matchesFilter(stack, filter, entry)) {
               count += stack.getCount();
            }
         }

         return count;
      }

      private static int countMatching(Container container, ItemStack filter, RoutingOrderData.RouteEntry entry) {
         int count = 0;

         for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (matchesFilter(stack, filter, entry)) {
               count += stack.getCount();
            }
         }

         return count;
      }

      private static int countExact(Container container, ItemStack filter) {
         int count = 0;

         for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (ItemStack.isSameItemSameTags(stack, filter)) {
               count += stack.getCount();
            }
         }

         return count;
      }

      private static int countExtractableExact(ItemHandlerAccess handler, ItemStack filter, int limit) {
         int count = 0;

         for (int slot = 0; slot < handler.getSlots() && count < limit; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (ItemStack.isSameItemSameTags(stack, filter)) {
               count += handler.extractItem(slot, limit - count, true).getCount();
            }
         }

         return count;
      }

      private static int handlerInsertCapacity(ItemHandlerAccess handler, ItemStack stack) {
         ItemStack remaining = stack.copy();

         for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, true);
         }

         return stack.getCount() - remaining.getCount();
      }

      private static int containerInsertCapacity(Container container, ItemStack stack) {
         int capacity = 0;

         for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack current = container.getItem(slot);
            if (current.isEmpty()) {
               capacity += Math.min(stack.getMaxStackSize(), container.getMaxStackSize());
            } else if (ItemStack.isSameItemSameTags(current, stack)) {
               capacity += Math.max(0, Math.min(current.getMaxStackSize(), container.getMaxStackSize()) - current.getCount());
            }

            if (capacity >= stack.getCount()) {
               return capacity;
            }
         }

         return capacity;
      }

      private static void moveExactContainerToHandler(Container from, ItemHandlerAccess to, ItemStack filter, int amount) {
         int remaining = amount;

         for (int slot = 0; slot < from.getContainerSize() && remaining > 0; slot++) {
            ItemStack source = from.getItem(slot);
            if (ItemStack.isSameItemSameTags(source, filter)) {
               int requested = Math.min(remaining, source.getCount());
               ItemStack moving = source.copyWithCount(requested);
               ItemStack rejected = insertIntoHandler(to, moving);
               int moved = requested - rejected.getCount();
               if (moved > 0) {
                  source.shrink(moved);
                  from.setItem(slot, source);
                  remaining -= moved;
               }
            }
         }

         from.setChanged();
      }

      private static void moveExactHandlerToContainer(ItemHandlerAccess from, Container to, ItemStack filter, int amount) {
         int remaining = amount;

         for (int slot = 0; slot < from.getSlots() && remaining > 0; slot++) {
            ItemStack source = from.getStackInSlot(slot);
            if (ItemStack.isSameItemSameTags(source, filter)) {
               ItemStack extracted = from.extractItem(slot, remaining, false);
               if (!extracted.isEmpty()) {
                  ItemStack rejected = insertIntoContainer(to, extracted);
                  int moved = extracted.getCount() - rejected.getCount();
                  remaining -= moved;
                  if (!rejected.isEmpty()) {
                     for (int restoreSlot = 0; restoreSlot < from.getSlots() && !rejected.isEmpty(); restoreSlot++) {
                        rejected = from.insertItem(restoreSlot, rejected, false);
                     }
                  }
               }
            }
         }
      }

      private static boolean matchesFilter(ItemStack stack, ItemStack filter, RoutingOrderData.RouteEntry entry) {
         if (stack.isEmpty() || filter.isEmpty() || stack.getItem() != filter.getItem()) {
            return false;
         } else if (!entry.ignoreDamage() && stack.getDamageValue() != filter.getDamageValue()) {
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
         CompoundTag tag = stack.getTag();
         if (tag == null) {
            return null;
         } else {
            CompoundTag copy = tag.copy();
            if (ignoreDamage) {
               copy.remove("Damage");
            }

            return copy.isEmpty() ? null : copy;
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
         this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
      }

      public boolean canUse() {
         if (this.courier.getCurrentCommand() != NpcCommandType.MOVE) {
            return false;
         } else {
            this.target = this.courier.getCommandTarget();
            return !BlockPos.ZERO.equals(this.target) && this.distanceToTargetSq() > this.stopDistanceSq;
         }
      }

      public boolean canContinueToUse() {
         return this.courier.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() > this.stopDistanceSq;
      }

      public void tick() {
         this.courier.getLookControl().setLookAt((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5);
         this.courier
            .getNavigation()
            .moveTo(
               (double)this.target.getX() + 0.5,
               (double)this.target.getY(),
               (double)this.target.getZ() + 0.5,
               this.courier.playerWalkNavigationSpeed()
            );
      }

      public void stop() {
         if (this.courier.getCurrentCommand() == NpcCommandType.MOVE && this.target != null && this.distanceToTargetSq() <= this.stopDistanceSq) {
            this.courier.clearCommand();
         }

         this.target = null;
         this.courier.getNavigation().stop();
      }

      private double distanceToTargetSq() {
         return this.courier.distanceToSqr((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5);
      }
   }
}
