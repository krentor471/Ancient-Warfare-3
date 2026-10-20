package net.shadowmage.ancientwarfare.core.item;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.shadowmage.ancientwarfare.core.entity.ModernSoldierNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.PlayerOwnedNpcEntity;

public final class OwnedNpcSpawnEggItem extends ForgeSpawnEggItem {
   public OwnedNpcSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type, int background, int highlight, Properties properties) {
      super(type, background, highlight, properties);
   }

   public int getColor(int tintIndex) {
      return 16777215;
   }

   public InteractionResult useOn(UseOnContext context) {
      if (!(context.getLevel() instanceof ServerLevel level)) {
         return InteractionResult.SUCCESS;
      } else {
         BlockPos var9 = context.getClickedPos();
         BlockState state = level.getBlockState(var9);
         if (state.is(Blocks.SPAWNER)) {
            return super.useOn(context);
         } else {
            ItemStack stack = context.getItemInHand();
            Direction face = context.getClickedFace();
            BlockPos position = state.getCollisionShape(level, var9).isEmpty() ? var9 : var9.relative(face);
            Entity spawned = spawnOwned(
               this.getType(stack.getTag()), level, stack, context.getPlayer(), position, true, !var9.equals(position) && face == Direction.UP
            );
            if (spawned != null) {
               stack.shrink(1);
               level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, var9);
            }

            return InteractionResult.CONSUME;
         }
      }
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      BlockHitResult hit = getPlayerPOVHitResult(level, player, Fluid.SOURCE_ONLY);
      if (hit.getType() != Type.BLOCK) {
         return InteractionResultHolder.pass(stack);
      } else if (level instanceof ServerLevel server) {
         BlockPos position = hit.getBlockPos();
         if (!(level.getBlockState(position).getBlock() instanceof LiquidBlock)) {
            return InteractionResultHolder.pass(stack);
         } else if (level.mayInteract(player, position) && player.mayUseItemAt(position, hit.getDirection(), stack)) {
            Entity spawned = spawnOwned(this.getType(stack.getTag()), server, stack, player, position, false, false);
            if (spawned == null) {
               return InteractionResultHolder.pass(stack);
            } else {
               if (!player.getAbilities().instabuild) {
                  stack.shrink(1);
               }

               player.awardStat(Stats.ITEM_USED.get(this));
               level.gameEvent(player, GameEvent.ENTITY_PLACE, spawned.position());
               return InteractionResultHolder.consume(stack);
            }
         } else {
            return InteractionResultHolder.fail(stack);
         }
      } else {
         return InteractionResultHolder.success(stack);
      }
   }

   public Optional<Mob> spawnOffspringFromSpawnEgg(Player player, Mob parent, EntityType<? extends Mob> type, ServerLevel level, Vec3 position, ItemStack stack) {
      Optional<Mob> offspring = super.spawnOffspringFromSpawnEgg(player, parent, type, level, position, stack);
      offspring.ifPresent(entity -> assignOwner(entity, player));
      return offspring;
   }

   private static <T extends Entity> T spawnOwned(
      EntityType<T> type, ServerLevel level, ItemStack stack, Player player, BlockPos position, boolean alignToSurface, boolean invertYOffset
   ) {
      Consumer<T> configure = EntityType.createDefaultStackConfig(level, stack, player).andThen(entity -> assignOwner(entity, player));
      if (type.create(level) instanceof ModernSoldierNpcEntity owned) {
         Entity extended = NpcSpawnerItem.createNpcFromStack(level, NpcSpawnerItem.create(owned.getNpcType(), owned.getNpcSubtype()));
         if (extended instanceof Mob) {
            type = extended.getType();
         }
      }

      return (T)type.spawn(level, stack.getTag(), configure, position, MobSpawnType.SPAWN_EGG, alignToSurface, invertYOffset);
   }

   private static void assignOwner(Entity entity, Player player) {
      if (player != null && !entity.level().isClientSide && entity instanceof PlayerOwnedNpcEntity npc) {
         npc.setOwner(player.getUUID());
         npc.setPersistenceRequired();
      }
   }
}
