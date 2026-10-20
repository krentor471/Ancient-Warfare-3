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

   public int m_43211_(int tintIndex) {
      return 16777215;
   }

   public InteractionResult m_6225_(UseOnContext context) {
      if (!(context.m_43725_() instanceof ServerLevel level)) {
         return InteractionResult.SUCCESS;
      } else {
         BlockPos var9 = context.m_8083_();
         BlockState state = level.m_8055_(var9);
         if (state.m_60713_(Blocks.f_50085_)) {
            return super.m_6225_(context);
         } else {
            ItemStack stack = context.m_43722_();
            Direction face = context.m_43719_();
            BlockPos position = state.m_60812_(level, var9).m_83281_() ? var9 : var9.m_121945_(face);
            Entity spawned = spawnOwned(
               this.m_43228_(stack.m_41783_()), level, stack, context.m_43723_(), position, true, !var9.equals(position) && face == Direction.UP
            );
            if (spawned != null) {
               stack.m_41774_(1);
               level.m_142346_(context.m_43723_(), GameEvent.f_157810_, var9);
            }

            return InteractionResult.CONSUME;
         }
      }
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.m_21120_(hand);
      BlockHitResult hit = m_41435_(level, player, Fluid.SOURCE_ONLY);
      if (hit.m_6662_() != Type.BLOCK) {
         return InteractionResultHolder.m_19098_(stack);
      } else if (level instanceof ServerLevel server) {
         BlockPos position = hit.m_82425_();
         if (!(level.m_8055_(position).m_60734_() instanceof LiquidBlock)) {
            return InteractionResultHolder.m_19098_(stack);
         } else if (level.m_7966_(player, position) && player.m_36204_(position, hit.m_82434_(), stack)) {
            Entity spawned = spawnOwned(this.m_43228_(stack.m_41783_()), server, stack, player, position, false, false);
            if (spawned == null) {
               return InteractionResultHolder.m_19098_(stack);
            } else {
               if (!player.m_150110_().f_35937_) {
                  stack.m_41774_(1);
               }

               player.m_36246_(Stats.f_12982_.m_12902_(this));
               level.m_220400_(player, GameEvent.f_157810_, spawned.m_20182_());
               return InteractionResultHolder.m_19096_(stack);
            }
         } else {
            return InteractionResultHolder.m_19100_(stack);
         }
      } else {
         return InteractionResultHolder.m_19090_(stack);
      }
   }

   public Optional<Mob> m_43215_(Player player, Mob parent, EntityType<? extends Mob> type, ServerLevel level, Vec3 position, ItemStack stack) {
      Optional<Mob> offspring = super.m_43215_(player, parent, type, level, position, stack);
      offspring.ifPresent(entity -> assignOwner(entity, player));
      return offspring;
   }

   private static <T extends Entity> T spawnOwned(
      EntityType<T> type, ServerLevel level, ItemStack stack, Player player, BlockPos position, boolean alignToSurface, boolean invertYOffset
   ) {
      Consumer<T> configure = EntityType.m_263562_(level, stack, player).andThen(entity -> assignOwner(entity, player));
      if (type.m_20615_(level) instanceof ModernSoldierNpcEntity owned) {
         Entity extended = NpcSpawnerItem.createNpcFromStack(level, NpcSpawnerItem.create(owned.getNpcType(), owned.getNpcSubtype()));
         if (extended instanceof Mob) {
            type = extended.m_6095_();
         }
      }

      return (T)type.m_262455_(level, stack.m_41783_(), configure, position, MobSpawnType.SPAWN_EGG, alignToSurface, invertYOffset);
   }

   private static void assignOwner(Entity entity, Player player) {
      if (player != null && !entity.m_9236_().f_46443_ && entity instanceof PlayerOwnedNpcEntity npc) {
         npc.setOwner(player.m_20148_());
         npc.m_21530_();
      }
   }
}
