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
   private final NonNullList<ItemStack> items = NonNullList.m_122780_(38, ItemStack.f_41583_);
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
      return this.f_58857_ != null && !this.f_58857_.f_46443_
         ? this.findShearTarget().isPresent()
            || this.findMilkTarget().isPresent()
            || this.findCullTarget().isPresent()
            || this.findBreedPair().isPresent()
            || this.findPickup().isPresent()
         : false;
   }

   @Override
   public void addEnergyFromWorker(WorkerAccess worker) {
      if (this.f_58857_ != null && !this.f_58857_.f_46443_ && worker.canWorkAt(this.getWorkType())) {
         this.storedEnergy = Math.min(
            this.getMaxEnergy(), this.storedEnergy + Math.max(1, Math.round(1600.0F * worker.getWorkEffectiveness(this.getWorkType())))
         );
         this.processStoredEnergy();
         this.m_6596_();
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
      Optional<Integer> shearsSlot = this.findToolSlot(stack -> stack.m_150930_(Items.f_42574_));
      if (!target.isEmpty() && !shearsSlot.isEmpty()) {
         target.get().m_5851_(SoundSource.BLOCKS);
         this.m_6596_();
         return true;
      } else {
         return false;
      }
   }

   private boolean tryMilk() {
      Optional<Cow> target = this.findMilkTarget();
      Optional<Integer> bucketSlot = this.findToolSlot(stack -> stack.m_150930_(Items.f_42446_));
      if (!target.isEmpty() && !bucketSlot.isEmpty()) {
         ((ItemStack)this.items.get(bucketSlot.get())).m_41774_(1);
         this.insertOrDrop(target.get().m_20183_(), new ItemStack(Items.f_42455_));
         this.m_6596_();
         return true;
      } else {
         return false;
      }
   }

   private boolean tryBreed() {
      Optional<AnimalFarmBlockEntity.BreedPair> pair = this.findBreedPair();
      if (!pair.isEmpty() && this.f_58857_ instanceof ServerLevel serverLevel) {
         AnimalFarmBlockEntity.BreedPair target = pair.get();
         target.first.m_27595_(null);
         target.second.m_27595_(null);
         target.first.m_27563_(serverLevel, target.second);
         ((ItemStack)this.items.get(target.foodSlot)).m_41774_(2);
         this.m_6596_();
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
         ItemStack stack = item.m_32055_().m_41777_();
         this.applyCullLootingBonus(stack);
         ItemStack remaining = this.insertStack(stack);
         if (remaining.m_41619_()) {
            item.m_146870_();
         } else {
            item.m_32045_(remaining);
         }

         return true;
      }
   }

   private boolean tryCull() {
      Optional<Animal> target = this.findCullTarget();
      if (!target.isEmpty() && this.f_58857_ instanceof ServerLevel serverLevel) {
         Animal var4 = target.get();
         this.cullLootingPickups = Math.max(this.cullLootingPickups, 4);
         var4.m_6469_(serverLevel.m_269111_().m_269264_(), var4.m_21233_() + 1.0F);
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
               if (this.f_58857_.f_46441_.m_188501_() < 0.5F) {
                  bonus++;
               }
            }

            if (bonus > 0) {
               stack.m_41769_(bonus);
            }

            this.cullLootingPickups--;
         }
      }
   }

   private Optional<Sheep> findShearTarget() {
      return this.findToolSlot(stack -> stack.m_150930_(Items.f_42574_)).isEmpty()
         ? Optional.empty()
         : this.f_58857_.m_6443_(Sheep.class, this.workAabb(), Sheep::m_6220_).stream().findFirst();
   }

   private Optional<Cow> findMilkTarget() {
      return this.findToolSlot(stack -> stack.m_150930_(Items.f_42446_)).isEmpty()
         ? Optional.empty()
         : this.f_58857_.m_6443_(Cow.class, this.workAabb(), cow -> cow.m_6084_() && !cow.m_6162_()).stream().findFirst();
   }

   private Optional<AnimalFarmBlockEntity.BreedPair> findBreedPair() {
      List<Animal> animals = this.f_58857_.m_6443_(Animal.class, this.workAabb(), animal -> animal.m_6084_() && animal.m_5957_());

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
      return first != second && first.m_6095_() == second.m_6095_() && first.m_5957_() && second.m_5957_();
   }

   private Optional<ItemEntity> findPickup() {
      return this.f_58857_.m_6443_(ItemEntity.class, this.workAabb(), item -> item.m_6084_() && this.isAnimalDrop(item.m_32055_())).stream().findFirst();
   }

   private Optional<Animal> findCullTarget() {
      for (ResourceLocation animalType : SUPPORTED_ANIMAL_TYPES) {
         if (BuiltInRegistries.f_256780_.m_7804_(animalType)) {
            EntityType<?> entityType = (EntityType<?>)BuiltInRegistries.f_256780_.m_7745_(animalType);
            List<Animal> adults = this.f_58857_
               .m_142425_(
                  EntityTypeTest.m_156916_(Animal.class),
                  this.workAabb(),
                  animalx -> animalx.m_6095_() == entityType && animalx.m_6084_() && !animalx.m_6162_()
               );
            if (adults.size() > this.getAnimalLimit(animalType)) {
               for (Animal animal : adults) {
                  if (!animal.m_27593_()) {
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
         if (stack.m_41613_() >= 2 && first.m_6898_(stack) && second.m_6898_(stack)) {
            return Optional.of(slot);
         }
      }

      return Optional.empty();
   }

   private Optional<Integer> findToolSlot(AnimalFarmBlockEntity.StackPredicate predicate) {
      for (int slot = 30; slot < 33; slot++) {
         ItemStack stack = (ItemStack)this.items.get(slot);
         if (!stack.m_41619_() && predicate.test(stack)) {
            return Optional.of(slot);
         }
      }

      return Optional.empty();
   }

   private AABB workAabb() {
      BlockPos min = this.getBoundsMin();
      BlockPos max = this.getBoundsMax();
      return new AABB(
         (double)min.m_123341_(),
         (double)min.m_123342_(),
         (double)min.m_123343_(),
         (double)max.m_123341_() + 1.0,
         (double)max.m_123342_() + 3.0,
         (double)max.m_123343_() + 1.0
      );
   }

   public boolean isFoodItem(ItemStack stack) {
      return !stack.m_41619_()
         && (
            this.f_58857_ != null && this.f_58857_.m_6443_(Animal.class, this.workAabb(), animal -> animal.m_6898_(stack)).size() > 0
               || stack.m_204117_(ItemTags.f_13149_)
               || stack.m_150930_(Items.f_42405_)
               || stack.m_150930_(Items.f_42404_)
               || stack.m_150930_(Items.f_42578_)
               || stack.m_150930_(Items.f_42577_)
               || stack.m_150930_(Items.f_271133_)
               || stack.m_150930_(Items.f_42619_)
               || stack.m_150930_(Items.f_42677_)
               || stack.m_150930_(Items.f_42620_)
               || stack.m_150930_(Items.f_42732_)
               || stack.m_150930_(Items.f_42733_)
               || stack.m_150930_(Items.f_42410_)
               || stack.m_150930_(Items.f_42436_)
               || stack.m_150930_(Items.f_42437_)
               || stack.m_150930_(Items.f_41939_)
               || stack.m_150930_(Items.f_42129_)
               || stack.m_150930_(Items.f_41867_)
               || stack.m_150930_(Items.f_41911_)
               || stack.m_150930_(Items.f_41982_)
               || stack.m_150930_(Items.f_42780_)
               || stack.m_150930_(Items.f_151079_)
               || stack.m_150930_(Items.f_42518_)
               || stack.m_150930_(Items.f_42526_)
               || stack.m_150930_(Items.f_42527_)
               || stack.m_150930_(Items.f_42528_)
               || stack.m_150930_(Items.f_42459_)
               || stack.m_150930_(Items.f_41954_)
               || stack.m_150930_(Items.f_41955_)
               || stack.m_150930_(Items.f_42583_)
         );
   }

   public boolean isToolItem(ItemStack stack) {
      return stack.m_150930_(Items.f_42446_) || stack.m_150930_(Items.f_42574_);
   }

   public int getAnimalLimit(ResourceLocation animalType) {
      return Math.max(0, this.animalLimits.getOrDefault(animalType, 0));
   }

   public void adjustAnimalLimit(ResourceLocation animalType, int delta) {
      this.setAnimalLimit(animalType, this.getAnimalLimit(animalType) + delta);
   }

   public void setAnimalLimit(ResourceLocation animalType, int limit) {
      this.animalLimits.put(animalType, Math.max(0, limit));
      this.m_6596_();
      if (this.f_58857_ != null) {
         this.f_58857_.m_7260_(this.f_58858_, this.m_58900_(), this.m_58900_(), 3);
      }
   }

   private boolean isAnimalDrop(ItemStack stack) {
      return stack.m_150930_(Items.f_42521_)
         || stack.m_150930_(Items.f_42402_)
         || stack.m_150930_(Items.f_42454_)
         || stack.m_150930_(Items.f_42579_)
         || stack.m_150930_(Items.f_42485_)
         || stack.m_150930_(Items.f_42581_)
         || stack.m_150930_(Items.f_42658_)
         || stack.m_150930_(Items.f_42697_)
         || stack.m_150930_(Items.f_42649_)
         || stack.m_150930_(Items.f_42648_)
         || stack.m_150930_(Items.f_41870_)
         || stack.m_150930_(Items.f_41871_)
         || stack.m_150930_(Items.f_41872_)
         || stack.m_150930_(Items.f_41873_)
         || stack.m_150930_(Items.f_41874_)
         || stack.m_150930_(Items.f_41875_)
         || stack.m_150930_(Items.f_41876_)
         || stack.m_150930_(Items.f_41877_)
         || stack.m_150930_(Items.f_41878_)
         || stack.m_150930_(Items.f_41932_)
         || stack.m_150930_(Items.f_41933_)
         || stack.m_150930_(Items.f_41934_)
         || stack.m_150930_(Items.f_41935_)
         || stack.m_150930_(Items.f_41936_)
         || stack.m_150930_(Items.f_41937_)
         || stack.m_150930_(Items.f_41938_);
   }

   private void insertOrDrop(BlockPos pos, ItemStack stack) {
      ItemStack remaining = this.insertStack(stack);
      if (!remaining.m_41619_()) {
         Block.m_49840_(this.f_58857_, pos, remaining);
      }
   }

   private ItemStack insertStack(ItemStack stack) {
      return this.insertIntoRange(stack.m_41777_(), 0, 27);
   }

   private ItemStack insertIntoRange(ItemStack stack, int start, int count) {
      for (int slot = start; slot < start + count && !stack.m_41619_(); slot++) {
         ItemStack existing = (ItemStack)this.items.get(slot);
         if (existing.m_41619_()) {
            this.items.set(slot, stack.m_41777_());
            stack.m_41764_(0);
         } else if (ItemStack.m_150942_(existing, stack) && existing.m_41613_() < existing.m_41741_()) {
            int moved = Math.min(stack.m_41613_(), existing.m_41741_() - existing.m_41613_());
            existing.m_41769_(moved);
            stack.m_41774_(moved);
         }
      }

      this.m_6596_();
      return stack;
   }

   @Override
   public BlockPos getBoundsMin() {
      if (this.boundsMin == null) {
         this.resetDefaultBounds(this.f_58858_, this.m_58900_());
      }

      return this.boundsMin;
   }

   @Override
   public BlockPos getBoundsMax() {
      if (this.boundsMax == null) {
         this.resetDefaultBounds(this.f_58858_, this.m_58900_());
      }

      return this.boundsMax;
   }

   public int getMaxBoundsWidth() {
      return WorksiteUpgradeHelper.getBoundsMaxWidth(this, 33, 5);
   }

   public void adjustBounds(int minXDelta, int maxXDelta, int minZDelta, int maxZDelta) {
      WorksiteBoundsGeometry.Bounds adjusted = WorksiteBoundsGeometry.adjust(
         this.f_58858_, this.getBoundsMin(), this.getBoundsMax(), this.getMaxBoundsWidth(), minXDelta, maxXDelta, minZDelta, maxZDelta
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
      return pos.m_123341_() >= min.m_123341_()
         && pos.m_123341_() <= max.m_123341_()
         && pos.m_123343_() >= min.m_123343_()
         && pos.m_123343_() <= max.m_123343_()
         && pos.m_123342_() >= min.m_123342_()
         && pos.m_123342_() <= min.m_123342_() + 3;
   }

   private void resetDefaultBounds(BlockPos pos, BlockState state) {
      Direction facing = state.m_61138_(AutomationWorksiteBlock.FACING)
         ? ((Direction)state.m_61143_(AutomationWorksiteBlock.FACING)).m_122424_()
         : Direction.SOUTH;
      BlockPos near = pos.m_121945_(facing);
      BlockPos far = pos.m_5484_(facing, 5);
      if (facing.m_122434_() == Axis.Z) {
         this.boundsMin = new BlockPos(pos.m_123341_() - 2, pos.m_123342_(), Math.min(near.m_123343_(), far.m_123343_()));
         this.boundsMax = new BlockPos(pos.m_123341_() + 2, pos.m_123342_(), Math.max(near.m_123343_(), far.m_123343_()));
      } else {
         this.boundsMin = new BlockPos(Math.min(near.m_123341_(), far.m_123341_()), pos.m_123342_(), pos.m_123343_() - 2);
         this.boundsMax = new BlockPos(Math.max(near.m_123341_(), far.m_123341_()), pos.m_123342_(), pos.m_123343_() + 2);
      }
   }

   @Override
   protected void m_183515_(CompoundTag tag) {
      super.m_183515_(tag);
      ContainerHelper.m_18973_(tag, this.items);
      tag.m_128356_("BoundsMin", this.getBoundsMin().m_121878_());
      tag.m_128356_("BoundsMax", this.getBoundsMax().m_121878_());
      CompoundTag limitsTag = new CompoundTag();
      this.animalLimits.forEach((animalType, limit) -> limitsTag.m_128405_(animalType.toString(), Math.max(0, limit)));
      tag.m_128365_("AnimalLimits", limitsTag);
   }

   @Override
   public void m_142466_(CompoundTag tag) {
      super.m_142466_(tag);
      ContainerHelper.m_18980_(tag, this.items);
      if (tag.m_128441_("BoundsMin")) {
         this.boundsMin = BlockPos.m_122022_(tag.m_128454_("BoundsMin"));
      }

      if (tag.m_128441_("BoundsMax")) {
         this.boundsMax = BlockPos.m_122022_(tag.m_128454_("BoundsMax"));
      }

      this.animalLimits.clear();
      if (tag.m_128441_("AnimalLimits")) {
         CompoundTag limitsTag = tag.m_128469_("AnimalLimits");

         for (String key : limitsTag.m_128431_()) {
            this.animalLimits.put(new ResourceLocation(key), Math.max(0, limitsTag.m_128451_(key)));
         }
      }

      this.normalizeLoadedBounds();
   }

   private void normalizeLoadedBounds() {
      if (this.f_58857_ == null || !this.f_58857_.f_46443_) {
         if (this.boundsMin != null && this.boundsMax != null && !WorksiteBoundsGeometry.hasValidSize(this.boundsMin, this.boundsMax, this.getMaxBoundsWidth())
            )
          {
            this.resetDefaultBounds(this.f_58858_, this.m_58900_());
         }
      }
   }

   @Nullable
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.m_195640_(this);
   }

   public CompoundTag m_5995_() {
      CompoundTag tag = super.m_5995_();
      tag.m_128356_("BoundsMin", this.getBoundsMin().m_121878_());
      tag.m_128356_("BoundsMax", this.getBoundsMax().m_121878_());
      CompoundTag limitsTag = new CompoundTag();
      this.animalLimits.forEach((animalType, limit) -> limitsTag.m_128405_(animalType.toString(), Math.max(0, limit)));
      tag.m_128365_("AnimalLimits", limitsTag);
      return tag;
   }

   public int m_6643_() {
      return 38;
   }

   public boolean m_7983_() {
      return this.items.stream().allMatch(ItemStack::m_41619_);
   }

   public ItemStack m_8020_(int slot) {
      return this.isValidSlot(slot) ? (ItemStack)this.items.get(slot) : ItemStack.f_41583_;
   }

   public ItemStack m_7407_(int slot, int amount) {
      ItemStack removed = this.isValidSlot(slot) ? ContainerHelper.m_18969_(this.items, slot, amount) : ItemStack.f_41583_;
      if (!removed.m_41619_()) {
         this.m_6596_();
      }

      return removed;
   }

   public ItemStack m_8016_(int slot) {
      return this.isValidSlot(slot) ? ContainerHelper.m_18966_(this.items, slot) : ItemStack.f_41583_;
   }

   public void m_6836_(int slot, ItemStack stack) {
      if (this.isValidSlot(slot)) {
         this.items.set(slot, stack);
         if (!stack.m_41619_() && stack.m_41613_() > this.m_6893_()) {
            stack.m_41764_(this.m_6893_());
         }

         this.m_6596_();
         if (slot >= 33 && slot < 38) {
            this.refreshChunkLoading();
         }
      }
   }

   public boolean m_6542_(Player player) {
      return this.f_58857_ != null
         && this.f_58857_.m_7702_(this.f_58858_) == this
         && player.m_20275_((double)this.f_58858_.m_123341_() + 0.5, (double)this.f_58858_.m_123342_() + 0.5, (double)this.f_58858_.m_123343_() + 0.5) <= 64.0;
   }

   public void m_6211_() {
      this.items.clear();
      this.m_6596_();
   }

   public Component m_5446_() {
      return Component.m_237115_(this.getDescriptionId());
   }

   @Nullable
   public AbstractContainerMenu m_7208_(int containerId, Inventory playerInventory, Player player) {
      return new AnimalFarmMenu(containerId, playerInventory, this.f_58858_);
   }

   public AbstractContainerMenu createBoundsMenu(int containerId, Inventory playerInventory) {
      return new AnimalFarmBoundsMenu(containerId, playerInventory, this.f_58858_);
   }

   public AbstractContainerMenu createControlMenu(int containerId, Inventory playerInventory) {
      return new AnimalFarmControlMenu(containerId, playerInventory, this.f_58858_);
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
