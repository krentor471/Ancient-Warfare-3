package net.shadowmage.ancientwarfare.core.forge;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.items.IItemHandler;
import net.shadowmage.ancientwarfare.core.common.inventory.ItemHandlerAccess;
import net.shadowmage.ancientwarfare.core.common.inventory.PlatformItemHandlerProvider;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.FORGE
)
public final class ForgeItemHandlerCapabilityEvents {
   private static final ResourceLocation CAPABILITY_ID = new ResourceLocation("ancientwarfare", "item_handler");

   private ForgeItemHandlerCapabilityEvents() {
   }

   @SubscribeEvent
   public static void attachBlockEntityCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
      if (event.getObject() instanceof PlatformItemHandlerProvider provider) {
         ForgeItemHandlerCapabilityEvents.ItemHandlerCapabilityProvider var3 = new ForgeItemHandlerCapabilityEvents.ItemHandlerCapabilityProvider(provider);
         event.addCapability(CAPABILITY_ID, var3);
         event.addListener(var3::invalidate);
      }
   }

   private static record Adapter(ItemHandlerAccess delegate) implements IItemHandler {
      public int getSlots() {
         return this.delegate.getSlots();
      }

      public ItemStack getStackInSlot(int slot) {
         return this.delegate.getStackInSlot(slot);
      }

      public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
         return this.delegate.insertItem(slot, stack, simulate);
      }

      public ItemStack extractItem(int slot, int amount, boolean simulate) {
         return this.delegate.extractItem(slot, amount, simulate);
      }

      public int getSlotLimit(int slot) {
         return this.delegate.getSlotLimit(slot);
      }

      public boolean isItemValid(int slot, ItemStack stack) {
         return this.delegate.isItemValid(slot, stack);
      }
   }

   private static final class ItemHandlerCapabilityProvider implements ICapabilityProvider {
      private final PlatformItemHandlerProvider provider;
      private final Map<Direction, LazyOptional<IItemHandler>> sided = new EnumMap<>(Direction.class);
      private LazyOptional<IItemHandler> unsided;

      private ItemHandlerCapabilityProvider(PlatformItemHandlerProvider provider) {
         this.provider = provider;
      }

      public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
         if (capability != ForgeCapabilities.ITEM_HANDLER) {
            return LazyOptional.empty();
         } else {
            LazyOptional<IItemHandler> result;
            if (side == null) {
               if (this.unsided == null) {
                  this.unsided = this.create(null);
               }

               result = this.unsided;
            } else {
               result = this.sided.computeIfAbsent(side, this::create);
            }

            return result.cast();
         }
      }

      private LazyOptional<IItemHandler> create(@Nullable Direction side) {
         ItemHandlerAccess handler = this.provider.getItemHandler(side);
         return handler == null ? LazyOptional.empty() : LazyOptional.of(() -> new ForgeItemHandlerCapabilityEvents.Adapter(handler));
      }

      private void invalidate() {
         if (this.unsided != null) {
            this.unsided.invalidate();
         }

         this.sided.values().forEach(LazyOptional::invalidate);
         this.sided.clear();
      }
   }
}
