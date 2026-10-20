package net.shadowmage.ancientwarfare.core.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.NpcEquipmentResolver;
import net.shadowmage.ancientwarfare.core.common.npc.NpcProfessionTool;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireDragonCompat;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireHippocampusCompat;
import net.shadowmage.ancientwarfare.core.compat.IceAndFireHippogryphCompat;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.DragonRiderController;
import net.shadowmage.ancientwarfare.core.entity.HippocampusRiderController;
import net.shadowmage.ancientwarfare.core.entity.HippogryphRiderController;
import net.shadowmage.ancientwarfare.core.entity.ModernSoldierNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.PlayerOwnedNpcEntity;

public class CommandBatonItem extends Item implements NpcProfessionTool {
   private static final String SELECTED_NPCS_TAG = "SelectedNpcs";
   private static final String COMBAT_GROUP_NPCS_TAG = "CombatGroupNpcs";
   private static final String COMBAT_GROUP_TYPE_TAG = "CombatGroupType";
   private static final String MODE_TAG = "Mode";
   private static final int MAX_SELECTED = 16;
   private final Tier tier;
   private final Multimap<Attribute, AttributeModifier> defaultModifiers;

   public CommandBatonItem(Tier tier) {
      super(new Properties().m_41487_(1).m_41503_(tier.m_6609_()));
      this.tier = tier;
      Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
      builder.put(Attributes.f_22281_, new AttributeModifier(f_41374_, "Weapon modifier", 4.0 + (double)tier.m_6631_(), Operation.ADDITION));
      builder.put(Attributes.f_22283_, new AttributeModifier(f_41375_, "Weapon modifier", -2.3, Operation.ADDITION));
      this.defaultModifiers = builder.build();
   }

   @Override
   public NpcProfessionTool.ToolRole npcToolRole() {
      return NpcProfessionTool.ToolRole.COMMANDER;
   }

   public int m_6473_() {
      return this.tier.m_6601_();
   }

   public boolean m_6832_(ItemStack stack, ItemStack repairCandidate) {
      return this.tier.m_6282_().test(repairCandidate) || super.m_6832_(stack, repairCandidate);
   }

   public boolean m_7579_(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      stack.m_41622_(1, attacker, entity -> entity.m_21166_(EquipmentSlot.MAINHAND));
      return true;
   }

   public Multimap<Attribute, AttributeModifier> m_7167_(EquipmentSlot slot) {
      return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.m_7167_(slot);
   }

   public InteractionResult m_6880_(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
      if (player.m_9236_().f_46443_) {
         return InteractionResult.SUCCESS;
      } else {
         CommandBatonItem.BatonMode mode = getMode(stack);
         if (target instanceof PlayerOwnedNpcEntity npc && npc.hasCommandPermission(player)) {
            toggleNpcSelection(stack, player, npc);
            return InteractionResult.SUCCESS;
         }

         if (mode == CommandBatonItem.BatonMode.MOUNT && target != player) {
            int commanded = issueMountCommand((ServerLevel)player.m_9236_(), player, stack, target);
            player.m_5661_(Component.m_237110_("guistrings.npc.baton.mount_command", new Object[]{commanded}), true);
            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   public static void toggleNpcSelection(ItemStack stack, Player player, PlayerOwnedNpcEntity npc) {
      if (getMode(stack) == CommandBatonItem.BatonMode.COMBAT_GROUP) {
         toggleCombatGroupSelection(stack, player, npc);
      } else if (getMode(stack) == CommandBatonItem.BatonMode.ATTACK && !npc.canReceiveAttackCommand()) {
         player.m_5661_(Component.m_237115_("guistrings.npc.baton.attack_selection_invalid"), true);
      } else if (getMode(stack) == CommandBatonItem.BatonMode.DISMOUNT && !isMountedNpc(npc)) {
         player.m_5661_(Component.m_237115_("guistrings.npc.baton.dismount_invalid"), true);
      } else {
         UUID npcId = npc.m_20148_();
         List<UUID> selected = getSelectedNpcs(stack);
         if (selected.remove(npcId)) {
            player.m_5661_(Component.m_237110_("guistrings.npc.baton.deselected", new Object[]{npc.m_5446_()}), true);
         } else if (selected.size() < 16) {
            selected.add(npcId);
            player.m_5661_(Component.m_237110_("guistrings.npc.baton.selected", new Object[]{npc.m_5446_()}), true);
         } else {
            player.m_5661_(Component.m_237115_("guistrings.npc.baton.selection_full"), true);
         }

         setSelectedNpcs(stack, selected);
      }
   }

   private static void toggleCombatGroupSelection(ItemStack stack, Player player, PlayerOwnedNpcEntity npc) {
      if (!npc.isCombatGroupEligible()) {
         player.m_5661_(Component.m_237115_("guistrings.npc.baton.combat_group_invalid"), true);
      } else {
         UUID npcId = npc.m_20148_();
         List<UUID> selected = getCombatGroupNpcs(stack);
         if (selected.remove(npcId)) {
            player.m_5661_(Component.m_237110_("guistrings.npc.baton.combat_group_removed", new Object[]{npc.m_5446_()}), true);
            refreshCombatGroupType(stack, player, selected);
         } else if (selected.size() < 16) {
            String selectedType = getCombatGroupType(stack);
            if (selectedType == null || selectedType.isBlank()) {
               selectedType = selected.stream()
                  .map(id -> player.m_9236_() instanceof ServerLevel level ? level.m_8791_(id) : null)
                  .filter(PlayerOwnedNpcEntity.class::isInstance)
                  .map(PlayerOwnedNpcEntity.class::cast)
                  .map(CommandBatonItem::combatGroupType)
                  .findFirst()
                  .orElse(null);
            }

            String npcType = combatGroupType(npc);
            if (selectedType != null && !selectedType.equals(npcType)) {
               player.m_5661_(
                  Component.m_237110_(
                     "guistrings.npc.baton.combat_group_type_mismatch",
                     new Object[]{
                        Component.m_237115_(combatGroupTypeTranslationKey(selectedType)), Component.m_237115_(combatGroupTypeTranslationKey(npcType))
                     }
                  ),
                  true
               );
               return;
            }

            setCombatGroupType(stack, npcType);
            selected.add(npcId);
            player.m_5661_(Component.m_237110_("guistrings.npc.baton.combat_group_added", new Object[]{npc.m_5446_()}), true);
         } else {
            player.m_5661_(Component.m_237115_("guistrings.npc.baton.selection_full"), true);
         }

         setUuidList(stack, "CombatGroupNpcs", selected);
      }
   }

   private static String combatGroupType(PlayerOwnedNpcEntity npc) {
      if (npc.isSiegeEngineerProfession()) {
         return "siege_engineer";
      } else if (npc instanceof ModernSoldierNpcEntity) {
         return npc.getNpcType();
      } else if (npc instanceof CombatNpcEntity combat) {
         if (combat.isLogicalDragonRider()) {
            return "dragon_rider";
         } else if (combat.isHippogryphRider()) {
            return "hippogryph_rider";
         } else if (combat.isHippocampusRider()) {
            return "hippocampus_rider";
         } else {
            String subtype = combat.getNpcSubtype();
            if (NpcEquipmentResolver.isRangedWeapon(combat.m_21205_())) {
               return combat.m_20159_() ? "mounted_archer" : "archer";
            } else if (combat.m_20159_()) {
               return "cavalry";
            } else if ("swordsman".equals(subtype)) {
               return "swordsman";
            } else if ("commander".equals(subtype)) {
               return "commander";
            } else if ("medic".equals(subtype)) {
               return "medic";
            } else {
               return "engineer".equals(subtype) ? "engineer" : "soldier";
            }
         }
      } else {
         return "combat";
      }
   }

   private static String combatGroupTypeTranslationKey(String type) {
      return "guistrings.npc.baton.combat_group_type." + type;
   }

   @Nullable
   private static String getCombatGroupType(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      return tag != null && tag.m_128425_("CombatGroupType", 8) ? tag.m_128461_("CombatGroupType") : null;
   }

   private static void setCombatGroupType(ItemStack stack, String type) {
      if (type != null && !type.isBlank()) {
         stack.m_41784_().m_128359_("CombatGroupType", type);
      } else {
         stack.m_41784_().m_128473_("CombatGroupType");
      }
   }

   private static void refreshCombatGroupType(ItemStack stack, Player player, List<UUID> selected) {
      if (selected.isEmpty()) {
         setCombatGroupType(stack, null);
      } else {
         String existingType = getCombatGroupType(stack);
         if (existingType == null || existingType.isBlank()) {
            if (player.m_9236_() instanceof ServerLevel level) {
               selected.stream()
                  .<Entity>map(level::m_8791_)
                  .filter(PlayerOwnedNpcEntity.class::isInstance)
                  .map(PlayerOwnedNpcEntity.class::cast)
                  .map(CommandBatonItem::combatGroupType)
                  .findFirst()
                  .ifPresentOrElse(type -> setCombatGroupType(stack, type), () -> setCombatGroupType(stack, null));
            }
         }
      }
   }

   public InteractionResult m_6225_(UseOnContext context) {
      Level level = context.m_43725_();
      Player player = context.m_43723_();
      if (!level.f_46443_ && player != null) {
         ItemStack stack = context.m_43722_();
         if (player.m_6144_()) {
            changeMode(stack, player, getMode(stack).next());
            return InteractionResult.SUCCESS;
         } else {
            BlockPos target = context.m_8083_().m_121945_(context.m_43719_());
            int commanded = executeBlockCommand((ServerLevel)level, player, context.m_43722_(), target);
            player.m_5661_(commandMessage(getMode(context.m_43722_()), commanded), true);
            return InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   public void m_7373_(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.m_237115_("tooltip.ancientwarfare.command_baton.select").m_130940_(ChatFormatting.GRAY));
      tooltip.add(
         Component.m_237110_("tooltip.ancientwarfare.command_baton.mode", new Object[]{Component.m_237115_(getMode(stack).translationKey)})
            .m_130940_(ChatFormatting.GRAY)
      );
      tooltip.add(Component.m_237115_("tooltip.ancientwarfare.command_baton.execute").m_130940_(ChatFormatting.DARK_GRAY));
      tooltip.add(Component.m_237115_("tooltip.ancientwarfare.command_baton.cycle").m_130940_(ChatFormatting.DARK_GRAY));
      int selectedCount = getMode(stack) == CommandBatonItem.BatonMode.COMBAT_GROUP ? getCombatGroupNpcs(stack).size() : getSelectedNpcs(stack).size();
      tooltip.add(Component.m_237110_("tooltip.ancientwarfare.command_baton.selected", new Object[]{selectedCount}).m_130940_(ChatFormatting.DARK_GRAY));
   }

   public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.m_21120_(hand);
      if (level.f_46443_) {
         return InteractionResultHolder.m_19090_(stack);
      } else if (player.m_6144_()) {
         changeMode(stack, player, getMode(stack).next());
         return InteractionResultHolder.m_19090_(stack);
      } else if (getMode(stack) == CommandBatonItem.BatonMode.CLEAR_COMMAND) {
         int commanded = issueClearCommand((ServerLevel)level, player, stack);
         player.m_5661_(Component.m_237110_("guistrings.npc.baton.clear_command", new Object[]{commanded}), true);
         return InteractionResultHolder.m_19090_(stack);
      } else if (getMode(stack) == CommandBatonItem.BatonMode.DISMOUNT) {
         player.m_5661_(Component.m_237110_("guistrings.npc.baton.dismount_ready", new Object[]{getSelectedNpcs(stack).size()}), true);
         return InteractionResultHolder.m_19090_(stack);
      } else if (getMode(stack) == CommandBatonItem.BatonMode.COMBAT_GROUP) {
         player.m_5661_(Component.m_237110_("guistrings.npc.baton.combat_group_ready", new Object[]{getCombatGroupNpcs(stack).size()}), true);
         return InteractionResultHolder.m_19090_(stack);
      } else {
         EntityHitResult hit = getLookedAtEntity(level, player, 120.0);
         if (hit != null && hit.m_82443_() instanceof LivingEntity target && getMode(stack) == CommandBatonItem.BatonMode.MOUNT) {
            int commanded = issueMountCommand((ServerLevel)level, player, stack, target);
            player.m_5661_(Component.m_237110_("guistrings.npc.baton.mount_command", new Object[]{commanded}), true);
            return InteractionResultHolder.m_19090_(stack);
         } else {
            return InteractionResultHolder.m_19098_(stack);
         }
      }
   }

   private static int executeBlockCommand(ServerLevel level, Player player, ItemStack stack, BlockPos target) {
      return switch (getMode(stack)) {
         case CLEAR_COMMAND -> issueClearCommand(level, player, stack);
         case DISMOUNT -> issueDismountCommand(level, player, stack, target);
         case MOVE -> issueMoveCommand(level, player, stack, target);
         case ATTACK -> 0;
         case MOUNT -> 0;
         case COMBAT_GROUP -> getCombatGroupNpcs(stack).size();
      };
   }

   private static int issueMoveCommand(ServerLevel level, Player player, ItemStack stack, BlockPos target) {
      List<UUID> selected = getSelectedNpcs(stack);
      List<UUID> stillValid = new ArrayList<>();
      int commanded = 0;

      for (UUID uuid : selected) {
         Entity entity = level.m_8791_(uuid);
         if (entity instanceof PlayerOwnedNpcEntity) {
            PlayerOwnedNpcEntity npc = (PlayerOwnedNpcEntity)entity;
            if (npc.hasCommandPermission(player)) {
               npc.setMoveCommand(target);
               stillValid.add(uuid);
               commanded++;
            }
         }
      }

      setSelectedNpcs(stack, stillValid);
      return commanded;
   }

   private static int issueAttackCommand(ServerLevel level, Player player, ItemStack stack, LivingEntity target) {
      List<UUID> selected = getSelectedNpcs(stack);
      List<UUID> stillValid = new ArrayList<>();
      int commanded = 0;

      for (UUID uuid : selected) {
         Entity entity = level.m_8791_(uuid);
         if (entity instanceof PlayerOwnedNpcEntity) {
            PlayerOwnedNpcEntity npc = (PlayerOwnedNpcEntity)entity;
            if (npc.canReceiveAttackCommand() && npc.hasCommandPermission(player)) {
               npc.setAttackCommand(target);
               stillValid.add(uuid);
               commanded++;
            }
         }
      }

      setSelectedNpcs(stack, stillValid);
      return commanded;
   }

   public static int commandMount(ServerLevel level, Player player, ItemStack stack, LivingEntity mount) {
      return issueMountCommand(level, player, stack, mount);
   }

   public static boolean isMountMode(ItemStack stack) {
      return stack != null && !stack.m_41619_() && stack.m_41720_() instanceof CommandBatonItem && getMode(stack) == CommandBatonItem.BatonMode.MOUNT;
   }

   private static int issueMountCommand(ServerLevel level, Player player, ItemStack stack, LivingEntity mount) {
      if (!(mount instanceof PlayerOwnedNpcEntity) && mount.m_6084_()) {
         List<UUID> selected = getSelectedNpcs(stack);
         AncientWarfareCore.LOG
            .info(
               "Mount command: selected={}, target={}, targetId={}, distance={}",
               new Object[]{selected.size(), mount.m_6095_().m_204041_().m_205785_().m_135782_(), mount.m_19879_(), player.m_20270_(mount)}
            );
         List<UUID> stillValid = new ArrayList<>();
         int commanded = 0;

         for (UUID uuid : selected) {
            Entity entity = level.m_8791_(uuid);
            if (entity instanceof PlayerOwnedNpcEntity) {
               PlayerOwnedNpcEntity npc = (PlayerOwnedNpcEntity)entity;
               if (!npc.hasCommandPermission(player)) {
                  AncientWarfareCore.LOG.info("Mount candidate {} rejected: permission", npc.m_19879_());
               } else {
                  stillValid.add(uuid);
                  boolean melee = npc instanceof CombatNpcEntity && "soldier".equals(npc.getNpcSubtype());
                  boolean iceDragon = IceAndFireDragonCompat.isDragon(mount);
                  boolean hippogryph = IceAndFireHippogryphCompat.isHippogryph(mount);
                  boolean hippocampus = IceAndFireHippocampusCompat.isHippocampus(mount);
                  boolean validDragon = !iceDragon || melee && IceAndFireDragonCompat.canBeMountedBy(mount, player.m_20148_());
                  boolean ranged = npc.m_21205_().m_41720_() instanceof BowItem || npc.m_21205_().m_41720_() instanceof CrossbowItem;
                  boolean validHippogryph = !hippogryph
                     || npc instanceof CombatNpcEntity
                        && ("soldier".equals(npc.getNpcSubtype()) || ranged)
                        && IceAndFireHippogryphCompat.canBeMountedBy(mount, player.m_20148_());
                  boolean validHippocampus = !hippocampus
                     || npc instanceof CombatNpcEntity
                        && ("soldier".equals(npc.getNpcSubtype()) || ranged)
                        && IceAndFireHippocampusCompat.canBeMountedBy(mount, player.m_20148_());
                  boolean alreadyLogicalDragonRider = npc.isLogicalDragonRider() || IceAndFireDragonCompat.isDragon(npc.m_20202_());
                  AncientWarfareCore.LOG
                     .info(
                        "Mount candidate {} subtype={}, iceDragon={}, validDragon={}, alive={}, passenger={}, logical={}, distance={}",
                        new Object[]{
                           npc.m_19879_(),
                           npc.getNpcSubtype(),
                           iceDragon,
                           validDragon,
                           npc.m_6084_(),
                           npc.m_20159_(),
                           alreadyLogicalDragonRider,
                           npc.m_20270_(mount)
                        }
                     );
                  double mountCommandRange = 16.0;
                  if (commanded == 0
                     && validDragon
                     && validHippogryph
                     && validHippocampus
                     && npc.m_6084_()
                     && (!npc.m_20159_() || alreadyLogicalDragonRider)
                     && npc.m_20280_(mount) <= mountCommandRange * mountCommandRange) {
                     prepareMount(mount);
                     boolean mounted = iceDragon ? DragonRiderController.attach(npc, mount) : npc.m_7998_(mount, true);
                     if (mounted) {
                        npc.clearCommand();
                        commanded++;
                     }
                  }
               }
            } else {
               AncientWarfareCore.LOG.info("Mount candidate {} not found on server", uuid);
            }
         }

         setSelectedNpcs(stack, stillValid);
         return commanded;
      } else {
         return 0;
      }
   }

   private static void prepareMount(LivingEntity mount) {
      if (mount instanceof Mob mob) {
         mob.m_21530_();
      }

      if (mount instanceof AbstractHorse horse) {
         horse.m_30651_(true);
         horse.m_30649_(100);
         horse.m_5853_(SoundSource.NEUTRAL);
         AttributeInstance speed = horse.m_21051_(Attributes.f_22279_);
         if (speed != null) {
            speed.m_22100_(Math.max(speed.m_22115_(), 0.32));
         }
      }
   }

   private static int issueClearCommand(ServerLevel level, Player player, ItemStack stack) {
      List<UUID> selected = getSelectedNpcs(stack);
      List<UUID> stillValid = new ArrayList<>();
      int commanded = 0;

      for (UUID uuid : selected) {
         Entity entity = level.m_8791_(uuid);
         if (entity instanceof PlayerOwnedNpcEntity) {
            PlayerOwnedNpcEntity npc = (PlayerOwnedNpcEntity)entity;
            if (npc.hasCommandPermission(player)) {
               npc.clearCommand();
               stillValid.add(uuid);
               commanded++;
            }
         }
      }

      setSelectedNpcs(stack, stillValid);
      return commanded;
   }

   private static int issueDismountCommand(ServerLevel level, Player player, ItemStack stack, BlockPos target) {
      List<UUID> selected = getSelectedNpcs(stack);
      List<UUID> stillValid = new ArrayList<>();
      int commanded = 0;

      for (UUID uuid : selected) {
         Entity entity = level.m_8791_(uuid);
         if (entity instanceof PlayerOwnedNpcEntity) {
            PlayerOwnedNpcEntity npc = (PlayerOwnedNpcEntity)entity;
            if (npc.hasCommandPermission(player)) {
               boolean mounted = npc.isLogicalDragonRider() || npc.m_20159_();
               if (!mounted) {
                  stillValid.add(uuid);
               } else {
                  label57: {
                     if (npc instanceof CombatNpcEntity combat && npc.isLogicalDragonRider()) {
                        DragonRiderController.detach(combat);
                        break label57;
                     }

                     if (npc instanceof CombatNpcEntity combat && IceAndFireHippogryphCompat.isHippogryph(npc.m_20202_())) {
                        HippogryphRiderController.detach(combat);
                        break label57;
                     }

                     if (npc instanceof CombatNpcEntity combat && IceAndFireHippocampusCompat.isHippocampus(npc.m_20202_())) {
                        HippocampusRiderController.detach(combat);
                        break label57;
                     }

                     if (npc.m_20159_()) {
                        npc.m_8127_();
                     }
                  }

                  npc.m_6021_((double)target.m_123341_() + 0.5, (double)target.m_123342_() + 1.0, (double)target.m_123343_() + 0.5);
                  npc.clearCommand();
                  stillValid.add(uuid);
                  commanded++;
               }
            }
         }
      }

      setSelectedNpcs(stack, stillValid);
      return commanded;
   }

   private static boolean isMountedNpc(PlayerOwnedNpcEntity npc) {
      return npc != null && (npc.m_20159_() || npc.isLogicalDragonRider());
   }

   private static Component commandMessage(CommandBatonItem.BatonMode mode, int commanded) {
      return switch (mode) {
         case CLEAR_COMMAND -> Component.m_237110_("guistrings.npc.baton.clear_command", new Object[]{commanded});
         case DISMOUNT -> Component.m_237110_("guistrings.npc.baton.dismount_command", new Object[]{commanded});
         case MOVE -> Component.m_237110_("guistrings.npc.baton.move_command", new Object[]{commanded});
         case ATTACK -> Component.m_237110_("guistrings.npc.baton.move_attack_command", new Object[]{commanded});
         case MOUNT -> Component.m_237110_("guistrings.npc.baton.mount_command", new Object[]{commanded});
         case COMBAT_GROUP -> Component.m_237110_("guistrings.npc.baton.combat_group_ready", new Object[]{commanded});
      };
   }

   private static EntityHitResult getLookedAtEntity(Level level, Player player, double range) {
      Vec3 start = player.m_146892_();
      Vec3 end = start.m_82549_(player.m_20154_().m_82490_(range));
      AABB box = player.m_20191_().m_82369_(player.m_20154_().m_82490_(range)).m_82400_(1.0);
      return ProjectileUtil.m_37304_(level, player, start, end, box, entity -> entity instanceof LivingEntity && entity.m_6087_() && entity != player);
   }

   private static List<UUID> getSelectedNpcs(ItemStack stack) {
      return getUuidList(stack, "SelectedNpcs");
   }

   public static List<UUID> getCombatGroupNpcs(ItemStack stack) {
      return getUuidList(stack, "CombatGroupNpcs");
   }

   public static boolean isCombatGroupMode(ItemStack stack) {
      return stack != null && !stack.m_41619_() && stack.m_41720_() instanceof CommandBatonItem && getMode(stack) == CommandBatonItem.BatonMode.COMBAT_GROUP;
   }

   public static boolean containsCombatGroupNpc(ItemStack stack, UUID npcId) {
      return npcId != null && isCombatGroupMode(stack) && getCombatGroupNpcs(stack).contains(npcId);
   }

   public static boolean selectsNpc(ItemStack stack, UUID npcId) {
      return stack != null && !stack.m_41619_() && npcId != null ? getSelectedNpcs(stack).contains(npcId) || getCombatGroupNpcs(stack).contains(npcId) : false;
   }

   private static List<UUID> getUuidList(ItemStack stack, String tagName) {
      List<UUID> selected = new ArrayList<>();
      CompoundTag tag = stack.m_41783_();
      if (tag != null && tag.m_128425_(tagName, 9)) {
         ListTag list = tag.m_128437_(tagName, 11);

         for (int i = 0; i < list.size(); i++) {
            selected.add(NbtUtils.m_129233_(list.get(i)));
         }

         return selected;
      } else {
         return selected;
      }
   }

   private static void setSelectedNpcs(ItemStack stack, List<UUID> selected) {
      setUuidList(stack, "SelectedNpcs", selected);
   }

   private static void setUuidList(ItemStack stack, String tagName, List<UUID> selected) {
      ListTag list = new ListTag();
      selected.forEach(uuid -> list.add(NbtUtils.m_129226_(uuid)));
      stack.m_41784_().m_128365_(tagName, list);
   }

   private static CommandBatonItem.BatonMode getMode(ItemStack stack) {
      CompoundTag tag = stack.m_41783_();
      return tag != null && tag.m_128441_("Mode") ? CommandBatonItem.BatonMode.byName(tag.m_128461_("Mode")) : CommandBatonItem.BatonMode.MOVE;
   }

   private static void changeMode(ItemStack stack, Player player, CommandBatonItem.BatonMode mode) {
      stack.m_41784_().m_128359_("Mode", mode.name);
      player.m_5661_(Component.m_237110_("guistrings.npc.baton.mode_changed", new Object[]{Component.m_237115_(mode.translationKey)}), true);
   }

   public static boolean isAttackMode(ItemStack stack) {
      return stack != null && !stack.m_41619_() && stack.m_41720_() instanceof CommandBatonItem && getMode(stack) == CommandBatonItem.BatonMode.ATTACK;
   }

   public static int commandAttack(ServerLevel level, Player player, ItemStack stack, LivingEntity target) {
      if (isAttackMode(stack) && target != null && target != player && target.m_6084_()) {
         if (target instanceof PlayerOwnedNpcEntity owned && owned.hasCommandPermission(player)) {
            return 0;
         }

         return issueAttackCommand(level, player, stack, target);
      } else {
         return 0;
      }
   }

   public static void clearSelections(ItemStack stack, Player player) {
      if (stack != null && !stack.m_41619_() && stack.m_41720_() instanceof CommandBatonItem) {
         int cleared = getSelectedNpcs(stack).size() + getCombatGroupNpcs(stack).size();
         stack.m_41784_().m_128473_("SelectedNpcs");
         stack.m_41784_().m_128473_("CombatGroupNpcs");
         stack.m_41784_().m_128473_("CombatGroupType");
         player.m_5661_(Component.m_237110_("guistrings.npc.baton.selections_cleared", new Object[]{cleared}), true);
      }
   }

   private static enum BatonMode {
      CLEAR_COMMAND("clear", "guistrings.npc.baton.clear"),
      DISMOUNT("dismount", "guistrings.npc.baton.dismount"),
      ATTACK("attack", "guistrings.npc.baton.attack"),
      MOVE("move", "guistrings.npc.baton.move"),
      MOUNT("mount", "guistrings.npc.baton.mount"),
      COMBAT_GROUP("combatGroup", "guistrings.npc.baton.combat_group");

      private final String name;
      private final String translationKey;

      private BatonMode(String name, String translationKey) {
         this.name = name;
         this.translationKey = translationKey;
      }

      private CommandBatonItem.BatonMode next() {
         CommandBatonItem.BatonMode[] modes = values();
         return modes[(this.ordinal() + 1) % modes.length];
      }

      private static CommandBatonItem.BatonMode byName(String name) {
         for (CommandBatonItem.BatonMode mode : values()) {
            if (mode.name.equals(name)) {
               return mode;
            }
         }

         return MOVE;
      }
   }
}
