package net.shadowmage.ancientwarfare.core.tile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.block.AutomationWorksiteBlock;
import net.shadowmage.ancientwarfare.core.common.npc.NpcWorkType;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.BoundedWorkSite;
import net.shadowmage.ancientwarfare.core.common.worksite.WorkerAccess;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteBoundsGeometry;
import net.shadowmage.ancientwarfare.core.common.worksite.WorksiteUpgradeHelper;
import net.shadowmage.ancientwarfare.core.menu.AnimalFarmBoundsMenu;
import net.shadowmage.ancientwarfare.core.menu.AnimalFarmControlMenu;
import net.shadowmage.ancientwarfare.core.menu.AnimalFarmMenu;
import org.jetbrains.annotations.Nullable;

public class AnimalFarmBlockEntity extends BasicWorksiteBlockEntity implements Container, MenuProvider, BoundedWorkSite {
   public static final int MAIN_SLOT_START = 0;
   public static final int MAIN_SLOT_COUNT = 27;
   public static final int FOOD_SLOT_START = 27;
   public static final int FOOD_SLOT_COUNT = 3;
   public static final int TOOL_SLOT_START = 30;
   public static final int TOOL_SLOT_COUNT = 3;
   public static final int UPGRADE_SLOT_START = 33;
   public static final int UPGRADE_SLOT_COUNT = 5;
   public static final int SLOT_COUNT = 38;
   public static final int DEFAULT_RADIUS = 2;
   public static final int DEFAULT_DEPTH = 5;
   public static final int MAX_BOUNDS_WIDTH = 16;
   private static final String MIN_TAG = "BoundsMin";
   private static final String MAX_TAG = "BoundsMax";
   private static final String ANIMAL_LIMITS_TAG = "AnimalLimits";
   private static final int WORK_COST = 100;
   private static final int WORKER_ENERGY_PER_VISIT = 1600;
   public static final ResourceLocation[] SUPPORTED_ANIMAL_TYPES = new ResourceLocation[]{
      entityId("cow"),
      entityId("mooshroom"),
      entityId("sheep"),
      entityId("pig"),
      entityId("chicken"),
      entityId("rabbit"),
      entityId("bee"),
      entityId("turtle"),
      entityId("panda"),
      entityId("fox"),
      entityId("goat"),
      entityId("frog"),
      entityId("camel"),
      entityId("sniffer"),
      entityId("hoglin"),
      entityId("strider"),
      entityId("horse"),
      entityId("donkey"),
      entityId("mule"),
      entityId("llama"),
      entityId("trader_llama"),
      entityId("cat"),
      entityId("ocelot"),
      entityId("wolf"),
      entityId("axolotl")
   };
   private final NonNullList<ItemStack> items = NonNullList.withSize(38, ItemStack.EMPTY);
   private final Map<ResourceLocation, Integer> animalLimits = new HashMap<>();
   private BlockPos boundsMin;
   private BlockPos boundsMax;
   private int cullLootingPickups;

   public AnimalFarmBlockEntity(BlockPos pos, BlockState state) {
      super(AWCoreRegistryAccess.blockEntityType(AWCoreIds.ANIMAL_FARM), pos, state);
      this.resetDefaultBounds(pos, state);
   }

   @Override
   public NpcWorkType getWorkType() {
      return NpcWorkType.FARMING;
   }

   @Override
   public String getDescriptionId() {
      return "block.ancientwarfare.animal_farm";
   }

   @Override
   public boolean hasWork() {
      return this.level != null && !this.level.isClientSide
         ? this.findShearTarget().isPresent()
            || this.findMilkTarget().isPresent()
            || this.findCullTarget().isPresent()
            || this.findBreedPair().isPresent()
            || this.findPickup().isPresent()
         : false;
   }

   @Override
   public void addEnergyFromWorker(WorkerAccess worker) {
      if (this.level != null && !this.level.isClientSide && worker.canWorkAt(this.getWorkType())) {
         this.storedEnergy = Math.min(
            this.getMaxEnergy(), this.storedEnergy + Math.max(1, Math.round(1600.0F * worker.getWorkEffectiveness(this.getWorkType())))
         );
         this.processStoredEnergy();
         this.setChanged();
      }
   }

   @Override
   protected void onWorkEnergyAdded() {
      this.processStoredEnergy();
   }

   @Override
   protected void onStoredEnergyTick() {
      this.processStoredEnergy();
   }

   private void processStoredEnergy() {
      int processed = 0;

      for (int workCost = this.getEffectiveWorkCost(); this.storedEnergy >= workCost && processed < 4 && this.processNextAction(); processed++) {
         this.storedEnergy -= workCost;
      }
   }

   private int getEffectiveWorkCost() {
      return WorksiteUpgradeHelper.adjustedWorkCost(100, this, 33, 5);
   }

   private boolean processNextAction() {
      return this.tryShear() || this.tryMilk() || this.tryCull() || this.tryBreed() || this.tryPickup();
   }

   private boolean tryShear() {
      Optional<Sheep> target = this.findShearTarget();
      Optional<Integer> shearsSlot = this.findToolSlot(stack -> stack.is(Items.SHEARS));
      if (!target.isEmpty() && !shearsSlot.isEmpty()) {
         target.get().shear(SoundSource.BLOCKS);
         this.setChanged();
         return true;
      } else {
         return false;
      }
   }

   private boolean tryMilk() {
      Optional<Cow> target = this.findMilkTarget();
      Optional<Integer> bucketSlot = this.findToolSlot(stack -> stack.is(Items.BUCKET));
      if (!target.isEmpty() && !bucketSlot.isEmpty()) {
         ((ItemStack)this.items.get(bucketSlot.get())).shrink(1);
         this.insertOrDrop(target.get().blockPosition(), new ItemStack(Items.MILK_BUCKET));
         this.setChanged();
         return true;
      } else {
         return false;
      }
   }

   private boolean tryBreed() {
      Optional<AnimalFarmBlockEntity.BreedPair> pair = this.findBreedPair();
      if (!pair.isEmpty() && this.level instanceof ServerLevel serverLevel) {
         AnimalFarmBlockEntity.BreedPair target = pair.get();
         target.first.setInLove(null);
         target.second.setInLove(null);
         target.first.spawnChildFromBreeding(serverLevel, target.second);
         ((ItemStack)this.items.get(target.foodSlot)).shrink(2);
         this.setChanged();
         return true;
      } else {
         return false;
      }
   }

   private boolean tryPickup() {
      Optional<ItemEntity> target = this.findPickup();
      if (target.isEmpty()) {
         return false;
      } else {
         ItemEntity item = target.get();
         ItemStack stack = item.getItem().copy();
         this.applyCullLootingBonus(stack);
         ItemStack remaining = this.insertStack(stack);
         if (remaining.isEmpty()) {
            item.discard();
         } else {
            item.setItem(remaining);
         }

         return true;
      }
   }

   private boolean tryCull() {
      Optional<Animal> target = this.findCullTarget();
      if (!target.isEmpty() && this.level instanceof ServerLevel serverLevel) {
         Animal var4 = target.get();
         this.cullLootingPickups = Math.max(this.cullLootingPickups, 4);
         var4.hurt(serverLevel.damageSources().generic(), var4.getMaxHealth() + 1.0F);
         return true;
      } else {
         return false;
      }
   }

   private void applyCullLootingBonus(ItemStack stack) {
      if (this.cullLootingPickups > 0 && this.isAnimalDrop(stack)) {
         int looting = WorksiteUpgradeHelper.getEnchantedToolsLevel(this, 33, 5);
         if (looting > 0) {
            int bonus = 0;

            for (int i = 0; i < looting; i++) {
               if (this.level.random.nextFloat() < 0.5F) {
                  bonus++;
               }
            }

            if (bonus > 0) {
               stack.grow(bonus);
            }

            this.cullLootingPickups--;
         }
      }
   }

   private Optional<Sheep> findShearTarget() {
      return this.findToolSlot(stack -> stack.is(Items.SHEARS)).isEmpty()
         ? Optional.empty()
         : this.level.getEntitiesOfClass(Sheep.class, this.workAabb(), Sheep::readyForShearing).stream().findFirst();
   }

   private Optional<Cow> findMilkTarget() {
      return this.findToolSlot(stack -> stack.is(Items.BUCKET)).isEmpty()
         ? Optional.empty()
         : this.level.getEntitiesOfClass(Cow.class, this.workAabb(), cow -> cow.isAlive() && !cow.isBaby()).stream().findFirst();
   }

   private Optional<AnimalFarmBlockEntity.BreedPair> findBreedPair() {
      List<Animal> animals = this.level.getEntitiesOfClass(Animal.class, this.workAabb(), animal -> animal.isAlive() && animal.canFallInLove());

      for (int firstIndex = 0; firstIndex < animals.size(); firstIndex++) {
         Animal first = animals.get(firstIndex);

         for (int secondIndex = firstIndex + 1; secondIndex < animals.size(); secondIndex++) {
            Animal second = animals.get(secondIndex);
            if (this.canBreedTogether(first, second)) {
               Optional<Integer> foodSlot = this.findFoodSlot(first, second);
               if (foodSlot.isPresent()) {
                  return Optional.of(new AnimalFarmBlockEntity.BreedPair(first, second, foodSlot.get()));
               }
            }
         }
      }

      return Optional.empty();
   }

   private boolean canBreedTogether(Animal first, Animal second) {
      return first != second && first.getType() == second.getType() && first.canFallInLove() && second.canFallInLove();
   }

   private Optional<ItemEntity> findPickup() {
      return this.level.getEntitiesOfClass(ItemEntity.class, this.workAabb(), item -> item.isAlive() && this.isAnimalDrop(item.getItem())).stream().findFirst();
   }

   private Optional<Animal> findCullTarget() {
      for (ResourceLocation animalType : SUPPORTED_ANIMAL_TYPES) {
         if (BuiltInRegistries.ENTITY_TYPE.containsKey(animalType)) {
            EntityType<?> entityType = (EntityType<?>)BuiltInRegistries.ENTITY_TYPE.get(animalType);
            List<Animal> adults = this.level
               .getEntities(
                  EntityTypeTest.forClass(Animal.class),
                  this.workAabb(),
                  animalx -> animalx.getType() == entityType && animalx.isAlive() && !animalx.isBaby()
               );
            if (adults.size() > this.getAnimalLimit(animalType)) {
               for (Animal animal : adults) {
                  if (!animal.isInLove()) {
                     return Optional.of(animal);
                  }
               }
            }
         }
      }

      return Optional.empty();
   }

   private Optional<Integer> findFoodSlot(Animal first, Animal second) {
      for (int slot = 27; slot < 30; slot++) {
         ItemStack stack = (ItemStack)this.items.get(slot);
         if (stack.getCount() >= 2 && first.isFood(stack) && second.isFood(stack)) {
            return Optional.of(slot);
         }
      }

      return Optional.empty();
   }

   private Optional<Integer> findToolSlot(AnimalFarmBlockEntity.StackPredicate predicate) {
      for (int slot = 30; slot < 33; slot++) {
         ItemStack stack = (ItemStack)this.items.get(slot);
         if (!stack.isEmpty() && predicate.test(stack)) {
            return Optional.of(slot);
         }
      }

      return Optional.empty();
   }

   private AABB workAabb() {
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();
      return new AABB(
         (double)min.getX(),
         (double)min.getY(),
         (double)min.getZ(),
         (double)max.getX() + 1.0,
         (double)max.getY() + 3.0,
         (double)max.getZ() + 1.0
      );
   }

   public boolean isFoodItem(ItemStack stack) {
      return !stack.isEmpty()
         && (
            this.level != null && this.level.getEntitiesOfClass(Animal.class, this.workAabb(), animal -> animal.isFood(stack)).size() > 0
               || stack.is(ItemTags.FLOWERS)
               || stack.is(Items.WHEAT)
               || stack.is(Items.WHEAT_SEEDS)
               || stack.is(Items.MELON_SEEDS)
               || stack.is(Items.PUMPKIN_SEEDS)
               || stack.is(Items.TORCHFLOWER_SEEDS)
               || stack.is(Items.CARROT)
               || stack.is(Items.GOLDEN_CARROT)
               || stack.is(Items.POTATO)
               || stack.is(Items.BEETROOT)
               || stack.is(Items.BEETROOT_SEEDS)
               || stack.is(Items.APPLE)
               || stack.is(Items.GOLDEN_APPLE)
               || stack.is(Items.ENCHANTED_GOLDEN_APPLE)
               || stack.is(Items.DANDELION)
               || stack.is(Items.HAY_BLOCK)
               || stack.is(Items.SEAGRASS)
               || stack.is(Items.BAMBOO)
               || stack.is(Items.CACTUS)
               || stack.is(Items.SWEET_BERRIES)
               || stack.is(Items.GLOW_BERRIES)
               || stack.is(Items.SLIME_BALL)
               || stack.is(Items.COD)
               || stack.is(Items.SALMON)
               || stack.is(Items.TROPICAL_FISH)
               || stack.is(Items.TROPICAL_FISH_BUCKET)
               || stack.is(Items.CRIMSON_FUNGUS)
               || stack.is(Items.WARPED_FUNGUS)
               || stack.is(Items.ROTTEN_FLESH)
         );
   }

   public boolean isToolItem(ItemStack stack) {
      return stack.is(Items.BUCKET) || stack.is(Items.SHEARS);
   }

   public int getAnimalLimit(ResourceLocation animalType) {
      return Math.max(0, this.animalLimits.getOrDefault(animalType, 0));
   }

   public void adjustAnimalLimit(ResourceLocation animalType, int delta) {
      this.setAnimalLimit(animalType, this.getAnimalLimit(animalType) + delta);
   }

   public void setAnimalLimit(ResourceLocation animalType, int limit) {
      this.animalLimits.put(animalType, Math.max(0, limit));
      this.setChanged();
      if (this.level != null) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   private boolean isAnimalDrop(ItemStack stack) {
      return stack.is(Items.EGG)
         || stack.is(Items.FEATHER)
         || stack.is(Items.LEATHER)
         || stack.is(Items.BEEF)
         || stack.is(Items.PORKCHOP)
         || stack.is(Items.CHICKEN)
         || stack.is(Items.MUTTON)
         || stack.is(Items.RABBIT)
         || stack.is(Items.RABBIT_HIDE)
         || stack.is(Items.RABBIT_FOOT)
         || stack.is(Items.WHITE_WOOL)
         || stack.is(Items.ORANGE_WOOL)
         || stack.is(Items.MAGENTA_WOOL)
         || stack.is(Items.LIGHT_BLUE_WOOL)
         || stack.is(Items.YELLOW_WOOL)
         || stack.is(Items.LIME_WOOL)
         || stack.is(Items.PINK_WOOL)
         || stack.is(Items.GRAY_WOOL)
         || stack.is(Items.LIGHT_GRAY_WOOL)
         || stack.is(Items.CYAN_WOOL)
         || stack.is(Items.PURPLE_WOOL)
         || stack.is(Items.BLUE_WOOL)
         || stack.is(Items.BROWN_WOOL)
         || stack.is(Items.GREEN_WOOL)
         || stack.is(Items.RED_WOOL)
         || stack.is(Items.BLACK_WOOL);
   }

   private void insertOrDrop(BlockPos pos, ItemStack stack) {
      ItemStack remaining = this.insertStack(stack);
      if (!remaining.isEmpty()) {
         Block.popResource(this.level, pos, remaining);
      }
   }

   private ItemStack insertStack(ItemStack stack) {
      return this.insertIntoRange(stack.copy(), 0, 27);
   }

   private ItemStack insertIntoRange(ItemStack stack, int start, int count) {
      for (int slot = start; slot < start + count && !stack.isEmpty(); slot++) {
         ItemStack existing = (ItemStack)this.items.get(slot);
         if (existing.isEmpty()) {
            this.items.set(slot, stack.copy());
            stack.setCount(0);
         } else if (ItemStack.isSameItemSameTags(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
            int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
            existing.grow(moved);
            stack.shrink(moved);
         }
      }

      this.setChanged();
      return stack;
   }

   @Override
   public BlockPos getBoundsMin() {
      if (this.boundsMin == null) {
         this.resetDefaultBounds(this.worldPosition, this.getBlockState());
      }

      return this.boundsMin;
   }

   @Override
   public BlockPos getBoundsMax() {
      if (this.boundsMax == null) {
         this.resetDefaultBounds(this.worldPosition, this.getBlockState());
      }

      return this.boundsMax;
   }

   public int getMaxBoundsWidth() {
      return WorksiteUpgradeHelper.getBoundsMaxWidth(this, 33, 5);
   }

   public void adjustBounds(int minXDelta, int maxXDelta, int minZDelta, int maxZDelta) {
      WorksiteBoundsGeometry.Bounds adjusted = WorksiteBoundsGeometry.adjust(
         this.worldPosition, this.getBoundsMin(), this.getBoundsMax(), this.getMaxBoundsWidth(), minXDelta, maxXDelta, minZDelta, maxZDelta
      );
      if (adjusted != null) {
         this.boundsMin = adjusted.min();
         this.boundsMax = adjusted.max();
         this.saveAndSyncWorksiteState();
      }
   }

   @Override
   public boolean containsWorkTarget(BlockPos pos) {
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();
      return pos.getX() >= min.getX()
         && pos.getX() <= max.getX()
         && pos.getZ() >= min.getZ()
         && pos.getZ() <= max.getZ()
         && pos.getY() >= min.getY()
         && pos.getY() <= min.getY() + 3;
   }

   private void resetDefaultBounds(BlockPos pos, BlockState state) {
      Direction facing = state.hasProperty(AutomationWorksiteBlock.FACING)
         ? ((Direction)state.getValue(AutomationWorksiteBlock.FACING)).getOpposite()
         : Direction.SOUTH;
      BlockPos near = pos.relative(facing);
      BlockPos far = pos.relative(facing, 5);
      if (facing.getAxis() == Axis.Z) {
         this.boundsMin = new BlockPos(pos.getX() - 2, pos.getY(), Math.min(near.getZ(), far.getZ()));
         this.boundsMax = new BlockPos(pos.getX() + 2, pos.getY(), Math.max(near.getZ(), far.getZ()));
      } else {
         this.boundsMin = new BlockPos(Math.min(near.getX(), far.getX()), pos.getY(), pos.getZ() - 2);
         this.boundsMax = new BlockPos(Math.max(near.getX(), far.getX()), pos.getY(), pos.getZ() + 2);
      }
   }

   @Override
   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      ContainerHelper.saveAllItems(tag, this.items);
      tag.putLong("BoundsMin", this.getBoundsMin().asLong());
      tag.putLong("BoundsMax", this.getBoundsMax().asLong());
      CompoundTag limitsTag = new CompoundTag();
      this.animalLimits.forEach((animalType, limit) -> limitsTag.putInt(animalType.toString(), Math.max(0, limit)));
      tag.put("AnimalLimits", limitsTag);
   }

   @Override
   public void load(CompoundTag tag) {
      super.load(tag);
      ContainerHelper.loadAllItems(tag, this.items);
      if (tag.contains("BoundsMin")) {
         this.boundsMin = BlockPos.of(tag.getLong("BoundsMin"));
      }

      if (tag.contains("BoundsMax")) {
         this.boundsMax = BlockPos.of(tag.getLong("BoundsMax"));
      }

      this.animalLimits.clear();
      if (tag.contains("AnimalLimits")) {
         CompoundTag limitsTag = tag.getCompound("AnimalLimits");

         for (String key : limitsTag.getAllKeys()) {
            this.animalLimits.put(new ResourceLocation(key), Math.max(0, limitsTag.getInt(key)));
         }
      }

      this.normalizeLoadedBounds();
   }

   private void normalizeLoadedBounds() {
      if (this.level == null || !this.level.isClientSide) {
         if (this.boundsMin != null && this.boundsMax != null && !WorksiteBoundsGeometry.hasValidSize(this.boundsMin, this.boundsMax, this.getMaxBoundsWidth())
            )
          {
            this.resetDefaultBounds(this.worldPosition, this.getBlockState());
         }
      }
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag() {
      CompoundTag tag = super.getUpdateTag();
      tag.putLong("BoundsMin", this.getBoundsMin().asLong());
      tag.putLong("BoundsMax", this.getBoundsMax().asLong());
      CompoundTag limitsTag = new CompoundTag();
      this.animalLimits.forEach((animalType, limit) -> limitsTag.putInt(animalType.toString(), Math.max(0, limit)));
      tag.put("AnimalLimits", limitsTag);
      return tag;
   }

   public int getContainerSize() {
      return 38;
   }

   public boolean isEmpty() {
      return this.items.stream().allMatch(ItemStack::isEmpty);
   }

   public ItemStack getItem(int slot) {
      return this.isValidSlot(slot) ? (ItemStack)this.items.get(slot) : ItemStack.EMPTY;
   }

   public ItemStack removeItem(int slot, int amount) {
      ItemStack removed = this.isValidSlot(slot) ? ContainerHelper.removeItem(this.items, slot, amount) : ItemStack.EMPTY;
      if (!removed.isEmpty()) {
         this.setChanged();
      }

      return removed;
   }

   public ItemStack removeItemNoUpdate(int slot) {
      return this.isValidSlot(slot) ? ContainerHelper.takeItem(this.items, slot) : ItemStack.EMPTY;
   }

   public void setItem(int slot, ItemStack stack) {
      if (this.isValidSlot(slot)) {
         this.items.set(slot, stack);
         if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
         }

         this.setChanged();
         if (slot >= 33 && slot < 38) {
            this.refreshChunkLoading();
         }
      }
   }

   public boolean stillValid(Player player) {
      return this.level != null
         && this.level.getBlockEntity(this.worldPosition) == this
         && player.distanceToSqr((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5) <= 64.0;
   }

   public void clearContent() {
      this.items.clear();
      this.setChanged();
   }

   public Component getDisplayName() {
      return Component.translatable(this.getDescriptionId());
   }

   @Nullable
   public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
      return new AnimalFarmMenu(containerId, playerInventory, this.worldPosition);
   }

   public AbstractContainerMenu createBoundsMenu(int containerId, Inventory playerInventory) {
      return new AnimalFarmBoundsMenu(containerId, playerInventory, this.worldPosition);
   }

   public AbstractContainerMenu createControlMenu(int containerId, Inventory playerInventory) {
      return new AnimalFarmControlMenu(containerId, playerInventory, this.worldPosition);
   }

   @Override
   protected BasicWorksiteBlockEntity.UpgradeSlots getUpgradeSlots() {
      return new BasicWorksiteBlockEntity.UpgradeSlots(33, 5);
   }

   @Override
   protected boolean isOutputSlot(int slot) {
      return slot >= 0 && slot < 27;
   }

   @Override
   protected boolean isInputSlot(int slot) {
      return slot >= 27 && slot < 30;
   }

   @Override
   protected boolean isSpecialSlot(int slot) {
      return slot >= 30 && slot < 33;
   }

   private boolean isValidSlot(int slot) {
      return slot >= 0 && slot < 38;
   }

   private static ResourceLocation entityId(String path) {
      return new ResourceLocation("minecraft", path);
   }

   private static record BreedPair(Animal first, Animal second, int foodSlot) {
   }

   private interface StackPredicate {
      boolean test(ItemStack var1);
   }
}
