package net.shadowmage.ancientwarfare.core.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.npc.NpcEquipmentResolver;
import net.shadowmage.ancientwarfare.core.common.npc.NpcFormation;
import net.shadowmage.ancientwarfare.core.common.npc.NpcSquadData;
import net.shadowmage.ancientwarfare.core.common.npc.NpcProfessionTool;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;
import net.shadowmage.ancientwarfare.core.entity.PlayerOwnedNpcEntity;

public class CommandBatonItem extends Item implements NpcProfessionTool {
   private static final String SELECTED_NPCS_TAG = "SelectedNpcs";
   private static final String COMBAT_GROUP_NPCS_TAG = "CombatGroupNpcs";
   private static final String COMBAT_GROUP_TYPE_TAG = "CombatGroupType";
   private static final String MODE_TAG = "Mode";
   private static final int MAX_SELECTED = 128;
   private static final double AREA_SELECT_RADIUS = 24.0;
   private static final double MIN_MARCH_FOR_FACING_SQ = 16.0;
   private static final String FORMATION_TAG = "Formation";
   private final Tier tier;
   private final Multimap<Attribute, AttributeModifier> defaultModifiers;

   public CommandBatonItem(Tier tier) {
      super(new Properties().stacksTo(1).durability(tier.getUses()));
      this.tier = tier;
      Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
      builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 4.0 + (double)tier.getAttackDamageBonus(), Operation.ADDITION));
      builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.3, Operation.ADDITION));
      this.defaultModifiers = builder.build();
   }

   @Override
   public NpcProfessionTool.ToolRole npcToolRole() {
      return NpcProfessionTool.ToolRole.COMMANDER;
   }

   public int getEnchantmentValue() {
      return this.tier.getEnchantmentValue();
   }

   public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
      return this.tier.getRepairIngredient().test(repairCandidate) || super.isValidRepairItem(stack, repairCandidate);
   }

   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      stack.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
      return true;
   }

   public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
      return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
   }

   public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
      if (player.level().isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         CommandBatonItem.BatonMode mode = getMode(stack);
         if (target instanceof PlayerOwnedNpcEntity npc && npc.hasCommandPermission(player)) {
            if (player.isShiftKeyDown() && mode != CommandBatonItem.BatonMode.COMBAT_GROUP) {
               selectNearby(stack, player, npc);
            } else {
               toggleNpcSelection(stack, player, npc);
            }

            return InteractionResult.SUCCESS;
         }

         if (mode == CommandBatonItem.BatonMode.MOUNT && target != player) {
            int commanded = issueMountCommand((ServerLevel)player.level(), player, stack, target);
            player.displayClientMessage(Component.translatable("guistrings.npc.baton.mount_command", new Object[]{commanded}), true);
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
         player.displayClientMessage(Component.translatable("guistrings.npc.baton.attack_selection_invalid"), true);
      } else if (getMode(stack) == CommandBatonItem.BatonMode.DISMOUNT && !isMountedNpc(npc)) {
         player.displayClientMessage(Component.translatable("guistrings.npc.baton.dismount_invalid"), true);
      } else {
         UUID npcId = npc.getUUID();
         List<UUID> selected = getSelectedNpcs(stack);
         if (selected.remove(npcId)) {
            player.displayClientMessage(Component.translatable("guistrings.npc.baton.deselected", new Object[]{npc.getDisplayName()}), true);
         } else if (selected.size() < MAX_SELECTED) {
            selected.add(npcId);
            player.displayClientMessage(Component.translatable("guistrings.npc.baton.selected", new Object[]{npc.getDisplayName()}), true);
         } else {
            player.displayClientMessage(Component.translatable("guistrings.npc.baton.selection_full"), true);
         }

         setSelectedNpcs(stack, selected);
      }
   }

   /** Selects every NPC of the clicked kind (soldier or civilian) the player commands around the clicked one. */
   private static void selectNearby(ItemStack stack, Player player, PlayerOwnedNpcEntity clicked) {
      boolean soldiers = clicked instanceof CombatNpcEntity;
      List<UUID> selected = getSelectedNpcs(stack);
      List<PlayerOwnedNpcEntity> nearby = player.level()
         .getEntitiesOfClass(
            PlayerOwnedNpcEntity.class,
            clicked.getBoundingBox().inflate(AREA_SELECT_RADIUS),
            npc -> npc.isAlive() && npc instanceof CombatNpcEntity == soldiers && npc.hasCommandPermission(player)
         );

      for (PlayerOwnedNpcEntity npc : nearby) {
         if (selected.size() >= MAX_SELECTED) {
            break;
         }

         if (!selected.contains(npc.getUUID())) {
            selected.add(npc.getUUID());
         }
      }

      setSelectedNpcs(stack, selected);
      player.displayClientMessage(Component.translatable("guistrings.npc.baton.area_selected", new Object[]{selected.size()}), true);
   }

   /** Saves the current selection as squad {@code slot}, or loads that squad as the selection. */
   public static void squadHotkey(ItemStack stack, ServerPlayer player, int slot, boolean assign) {
      if (stack.getItem() instanceof CommandBatonItem && slot >= 1 && slot <= NpcSquadData.SLOTS) {
         NpcSquadData squads = NpcSquadData.get(player.serverLevel());
         if (assign) {
            List<UUID> selected = getSelectedNpcs(stack);
            squads.setSquad(player.getUUID(), slot, selected);
            player.displayClientMessage(Component.translatable("guistrings.npc.baton.squad_saved", new Object[]{slot, selected.size()}), true);
         } else {
            List<UUID> members = new ArrayList<>();

            for (UUID uuid : squads.getSquad(player.getUUID(), slot)) {
               // Unloaded members stay in the squad; dead or foreign ones are dropped.
               Entity entity = player.serverLevel().getEntity(uuid);
               if (entity == null || entity instanceof PlayerOwnedNpcEntity npc && npc.isAlive() && npc.hasCommandPermission(player)) {
                  members.add(uuid);
               }
            }

            squads.setSquad(player.getUUID(), slot, members);
            setSelectedNpcs(stack, members);
            player.displayClientMessage(Component.translatable("guistrings.npc.baton.squad_selected", new Object[]{slot, members.size()}), true);
         }
      }
   }

   private static int issueHoldCommand(ServerLevel level, Player player, ItemStack stack) {
      List<UUID> selected = getSelectedNpcs(stack);
      List<UUID> stillValid = new ArrayList<>();
      int commanded = 0;

      for (UUID uuid : selected) {
         if (level.getEntity(uuid) instanceof CombatNpcEntity npc && npc.hasCommandPermission(player)) {
            npc.setHoldCommand(npc.blockPosition(), npc.getYRot());
            stillValid.add(uuid);
            commanded++;
         } else if (level.getEntity(uuid) instanceof PlayerOwnedNpcEntity npc && npc.hasCommandPermission(player)) {
            stillValid.add(uuid);
         }
      }

      setSelectedNpcs(stack, stillValid);
      return commanded;
   }

   private static void toggleCombatGroupSelection(ItemStack stack, Player player, PlayerOwnedNpcEntity npc) {
      if (!npc.isCombatGroupEligible()) {
         player.displayClientMessage(Component.translatable("guistrings.npc.baton.combat_group_invalid"), true);
      } else {
         UUID npcId = npc.getUUID();
         List<UUID> selected = getCombatGroupNpcs(stack);
         if (selected.remove(npcId)) {
            player.displayClientMessage(Component.translatable("guistrings.npc.baton.combat_group_removed", new Object[]{npc.getDisplayName()}), true);
            refreshCombatGroupType(stack, player, selected);
         } else if (selected.size() < MAX_SELECTED) {
            String selectedType = getCombatGroupType(stack);
            if (selectedType == null || selectedType.isBlank()) {
               selectedType = selected.stream()
                  .map(id -> player.level() instanceof ServerLevel level ? level.getEntity(id) : null)
                  .filter(PlayerOwnedNpcEntity.class::isInstance)
                  .map(PlayerOwnedNpcEntity.class::cast)
                  .map(CommandBatonItem::combatGroupType)
                  .findFirst()
                  .orElse(null);
            }

            String npcType = combatGroupType(npc);
            if (selectedType != null && !selectedType.equals(npcType)) {
               player.displayClientMessage(
                  Component.translatable(
                     "guistrings.npc.baton.combat_group_type_mismatch",
                     new Object[]{
                        Component.translatable(combatGroupTypeTranslationKey(selectedType)), Component.translatable(combatGroupTypeTranslationKey(npcType))
                     }
                  ),
                  true
               );
               return;
            }

            setCombatGroupType(stack, npcType);
            selected.add(npcId);
            player.displayClientMessage(Component.translatable("guistrings.npc.baton.combat_group_added", new Object[]{npc.getDisplayName()}), true);
         } else {
            player.displayClientMessage(Component.translatable("guistrings.npc.baton.selection_full"), true);
         }

         setUuidList(stack, "CombatGroupNpcs", selected);
      }
   }

   private static String combatGroupType(PlayerOwnedNpcEntity npc) {
      if (npc.isSiegeEngineerProfession()) {
         return "siege_engineer";
      } else if (npc instanceof CombatNpcEntity combat) {
         String subtype = combat.getNpcSubtype();
         if (NpcEquipmentResolver.isRangedWeapon(combat.getMainHandItem())) {
            return combat.isPassenger() ? "mounted_archer" : "archer";
         } else if (combat.isPassenger()) {
            return "cavalry";
         } else if ("commander".equals(subtype)) {
            return "commander";
         } else if ("medic".equals(subtype)) {
            return "medic";
         } else {
            return "engineer".equals(subtype) ? "engineer" : "soldier";
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
      CompoundTag tag = stack.getTag();
      return tag != null && tag.contains("CombatGroupType", 8) ? tag.getString("CombatGroupType") : null;
   }

   private static void setCombatGroupType(ItemStack stack, String type) {
      if (type != null && !type.isBlank()) {
         stack.getOrCreateTag().putString("CombatGroupType", type);
      } else {
         stack.getOrCreateTag().remove("CombatGroupType");
      }
   }

   private static void refreshCombatGroupType(ItemStack stack, Player player, List<UUID> selected) {
      if (selected.isEmpty()) {
         setCombatGroupType(stack, null);
      } else {
         String existingType = getCombatGroupType(stack);
         if (existingType == null || existingType.isBlank()) {
            if (player.level() instanceof ServerLevel level) {
               selected.stream()
                  .<Entity>map(level::getEntity)
                  .filter(PlayerOwnedNpcEntity.class::isInstance)
                  .map(PlayerOwnedNpcEntity.class::cast)
                  .map(CommandBatonItem::combatGroupType)
                  .findFirst()
                  .ifPresentOrElse(type -> setCombatGroupType(stack, type), () -> setCombatGroupType(stack, null));
            }
         }
      }
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      Player player = context.getPlayer();
      if (!level.isClientSide && player != null) {
         ItemStack stack = context.getItemInHand();
         if (player.isShiftKeyDown() && getMode(stack) == CommandBatonItem.BatonMode.MOVE) {
            BlockPos target = context.getClickedPos().relative(context.getClickedFace());
            int commanded = issueQueuedMoveCommand((ServerLevel)level, player, stack, target);
            player.displayClientMessage(Component.translatable("guistrings.npc.baton.queued_move_command", new Object[]{commanded}), true);
            return InteractionResult.SUCCESS;
         } else if (player.isShiftKeyDown()) {
            changeMode(stack, player, getMode(stack).next());
            return InteractionResult.SUCCESS;
         } else {
            BlockPos target = context.getClickedPos().relative(context.getClickedFace());
            int commanded = executeBlockCommand((ServerLevel)level, player, context.getItemInHand(), target);
            player.displayClientMessage(commandMessage(getMode(context.getItemInHand()), commanded), true);
            return InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tooltip.ancientwarfare.command_baton.select").withStyle(ChatFormatting.GRAY));
      tooltip.add(
         Component.translatable("tooltip.ancientwarfare.command_baton.mode", new Object[]{Component.translatable(getMode(stack).translationKey)})
            .withStyle(ChatFormatting.GRAY)
      );
      tooltip.add(Component.translatable("tooltip.ancientwarfare.command_baton.execute").withStyle(ChatFormatting.DARK_GRAY));
      tooltip.add(Component.translatable("tooltip.ancientwarfare.command_baton.cycle").withStyle(ChatFormatting.DARK_GRAY));
      tooltip.add(Component.translatable("tooltip.ancientwarfare.command_baton.queue").withStyle(ChatFormatting.DARK_GRAY));
      tooltip.add(Component.translatable("tooltip.ancientwarfare.command_baton.squads").withStyle(ChatFormatting.DARK_GRAY));
      tooltip.add(
         Component.translatable("tooltip.ancientwarfare.command_baton.formation", new Object[]{Component.translatable(getFormation(stack).translationKey())})
            .withStyle(ChatFormatting.GRAY)
      );
      int selectedCount = getMode(stack) == CommandBatonItem.BatonMode.COMBAT_GROUP ? getCombatGroupNpcs(stack).size() : getSelectedNpcs(stack).size();
      tooltip.add(Component.translatable("tooltip.ancientwarfare.command_baton.selected", new Object[]{selectedCount}).withStyle(ChatFormatting.DARK_GRAY));
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (level.isClientSide) {
         return InteractionResultHolder.success(stack);
      } else if (player.isShiftKeyDown()) {
         changeMode(stack, player, getMode(stack).next());
         return InteractionResultHolder.success(stack);
      } else if (getMode(stack) == CommandBatonItem.BatonMode.HOLD) {
         int commanded = issueHoldCommand((ServerLevel)level, player, stack);
         player.displayClientMessage(Component.translatable("guistrings.npc.baton.hold_command", new Object[]{commanded}), true);
         return InteractionResultHolder.success(stack);
      } else if (getMode(stack) == CommandBatonItem.BatonMode.CLEAR_COMMAND) {
         int commanded = issueClearCommand((ServerLevel)level, player, stack);
         player.displayClientMessage(Component.translatable("guistrings.npc.baton.clear_command", new Object[]{commanded}), true);
         return InteractionResultHolder.success(stack);
      } else if (getMode(stack) == CommandBatonItem.BatonMode.DISMOUNT) {
         player.displayClientMessage(Component.translatable("guistrings.npc.baton.dismount_ready", new Object[]{getSelectedNpcs(stack).size()}), true);
         return InteractionResultHolder.success(stack);
      } else if (getMode(stack) == CommandBatonItem.BatonMode.COMBAT_GROUP) {
         player.displayClientMessage(Component.translatable("guistrings.npc.baton.combat_group_ready", new Object[]{getCombatGroupNpcs(stack).size()}), true);
         return InteractionResultHolder.success(stack);
      } else {
         EntityHitResult hit = getLookedAtEntity(level, player, 120.0);
         if (hit != null && hit.getEntity() instanceof LivingEntity target && getMode(stack) == CommandBatonItem.BatonMode.MOUNT) {
            int commanded = issueMountCommand((ServerLevel)level, player, stack, target);
            player.displayClientMessage(Component.translatable("guistrings.npc.baton.mount_command", new Object[]{commanded}), true);
            return InteractionResultHolder.success(stack);
         } else {
            return InteractionResultHolder.pass(stack);
         }
      }
   }

   private static int executeBlockCommand(ServerLevel level, Player player, ItemStack stack, BlockPos target) {
      return switch (getMode(stack)) {
         case CLEAR_COMMAND -> issueClearCommand(level, player, stack);
         case DISMOUNT -> issueDismountCommand(level, player, stack, target);
         case MOVE -> issueMoveCommand(level, player, stack, target);
         case HOLD -> issueHoldCommand(level, player, stack);
         case ATTACK -> 0;
         case MOUNT -> 0;
         case COMBAT_GROUP -> getCombatGroupNpcs(stack).size();
      };
   }

   private static int issueMoveCommand(ServerLevel level, Player player, ItemStack stack, BlockPos target) {
      return issueMoveCommand(level, player, stack, target, false);
   }

   private static int issueQueuedMoveCommand(ServerLevel level, Player player, ItemStack stack, BlockPos target) {
      return issueMoveCommand(level, player, stack, target, true);
   }

   private static int issueMoveCommand(ServerLevel level, Player player, ItemStack stack, BlockPos target, boolean queued) {
      List<UUID> stillValid = new ArrayList<>();
      List<PlayerOwnedNpcEntity> npcs = new ArrayList<>();

      for (UUID uuid : getSelectedNpcs(stack)) {
         if (level.getEntity(uuid) instanceof PlayerOwnedNpcEntity npc && npc.hasCommandPermission(player)) {
            npcs.add(npc);
            stillValid.add(uuid);
         }
      }

      if (!npcs.isEmpty()) {
         Direction facing = formationFacing(player, npcs, target);
         float yaw = facing.toYRot();
         List<BlockPos> spots = assignFormation(level, npcs, target, facing, getFormation(stack));

         for (int i = 0; i < npcs.size(); i++) {
            if (queued) {
               npcs.get(i).queueMoveCommand(spots.get(i), yaw);
            } else {
               npcs.get(i).setMoveCommand(spots.get(i), yaw);
            }
         }
      }

      setSelectedNpcs(stack, stillValid);
      return npcs.size();
   }

   /** The group faces the way it marches: from its centre to the target, or where the player looks for a short step. */
   private static Direction formationFacing(Player player, List<PlayerOwnedNpcEntity> npcs, BlockPos target) {
      double x = 0.0;
      double z = 0.0;

      for (PlayerOwnedNpcEntity npc : npcs) {
         x += npc.getX();
         z += npc.getZ();
      }

      double dx = target.getX() + 0.5 - x / npcs.size();
      double dz = target.getZ() + 0.5 - z / npcs.size();
      return dx * dx + dz * dz < MIN_MARCH_FOR_FACING_SQ ? player.getDirection() : Direction.getNearest(dx, 0.0, dz);
   }

   /**
    * Gives every NPC a slot: melee in the front rows and ranged behind, and inside a row NPCs keep their left-to-right
    * order so paths do not cross. Slots snap to the block grid, so the formation faces one of the four directions.
    */
   private static List<BlockPos> assignFormation(ServerLevel level, List<PlayerOwnedNpcEntity> npcs, BlockPos target, Direction facing, NpcFormation formation) {
      Direction right = facing.getClockWise();
      List<PlayerOwnedNpcEntity> ordered = new ArrayList<>(npcs);
      ordered.sort(
         Comparator.<PlayerOwnedNpcEntity>comparingInt(npc -> NpcEquipmentResolver.isRangedWeapon(npc.getMainHandItem()) ? 1 : 0)
            .thenComparingDouble(npc -> -along(npc, target, facing))
      );
      List<NpcFormation.Slot> slots = formation.slots(ordered.size());
      Map<PlayerOwnedNpcEntity, BlockPos> spots = new HashMap<>();
      int start = 0;

      while (start < slots.size()) {
         int row = slots.get(start).row();
         int end = start;
         while (end < slots.size() && slots.get(end).row() == row) {
            end++;
         }

         List<PlayerOwnedNpcEntity> rowNpcs = new ArrayList<>(ordered.subList(start, end));
         rowNpcs.sort(Comparator.comparingDouble(npc -> along(npc, target, right)));

         for (int i = 0; i < rowNpcs.size(); i++) {
            NpcFormation.Slot slot = slots.get(start + i);
            int x = target.getX() + right.getStepX() * slot.side() - facing.getStepX() * slot.depth();
            int z = target.getZ() + right.getStepZ() * slot.side() - facing.getStepZ() * slot.depth();
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            spots.put(rowNpcs.get(i), new BlockPos(x, Math.abs(surface - target.getY()) <= 3 ? surface : target.getY(), z));
         }

         start = end;
      }

      List<BlockPos> result = new ArrayList<>(npcs.size());
      npcs.forEach(npc -> result.add(spots.get(npc)));
      return result;
   }

   private static double along(PlayerOwnedNpcEntity npc, BlockPos target, Direction direction) {
      return (npc.getX() - target.getX() - 0.5) * direction.getStepX() + (npc.getZ() - target.getZ() - 0.5) * direction.getStepZ();
   }

   public static NpcFormation getFormation(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag != null && tag.contains(FORMATION_TAG) ? NpcFormation.byName(tag.getString(FORMATION_TAG)) : NpcFormation.SQUARE;
   }

   public static void cycleFormation(ItemStack stack, Player player) {
      if (stack.getItem() instanceof CommandBatonItem) {
         NpcFormation formation = getFormation(stack).next();
         stack.getOrCreateTag().putString(FORMATION_TAG, formation.serializedName());
         player.displayClientMessage(Component.translatable("guistrings.npc.baton.formation_changed", new Object[]{Component.translatable(formation.translationKey())}), true);
      }
   }

   private static int issueAttackCommand(ServerLevel level, Player player, ItemStack stack, LivingEntity target) {
      List<UUID> selected = getSelectedNpcs(stack);
      List<UUID> stillValid = new ArrayList<>();
      int commanded = 0;

      for (UUID uuid : selected) {
         Entity entity = level.getEntity(uuid);
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
      return stack != null && !stack.isEmpty() && stack.getItem() instanceof CommandBatonItem && getMode(stack) == CommandBatonItem.BatonMode.MOUNT;
   }

   private static int issueMountCommand(ServerLevel level, Player player, ItemStack stack, LivingEntity mount) {
      if (!(mount instanceof PlayerOwnedNpcEntity) && mount.isAlive()) {
         List<UUID> selected = getSelectedNpcs(stack);
         AncientWarfareCore.LOG
            .info(
               "Mount command: selected={}, target={}, targetId={}, distance={}",
               new Object[]{selected.size(), mount.getType().builtInRegistryHolder().key().location(), mount.getId(), player.distanceTo(mount)}
            );
         List<UUID> stillValid = new ArrayList<>();
         int commanded = 0;

         for (UUID uuid : selected) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof PlayerOwnedNpcEntity) {
               PlayerOwnedNpcEntity npc = (PlayerOwnedNpcEntity)entity;
               if (!npc.hasCommandPermission(player)) {
                  AncientWarfareCore.LOG.info("Mount candidate {} rejected: permission", npc.getId());
               } else {
                  stillValid.add(uuid);
                  double mountCommandRange = 16.0;
                  if (commanded == 0
                     && npc.isAlive()
                     && !npc.isPassenger()
                     && npc.distanceToSqr(mount) <= mountCommandRange * mountCommandRange) {
                     prepareMount(mount);
                     boolean mounted = npc.startRiding(mount, true);
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
         mob.setPersistenceRequired();
      }

      if (mount instanceof AbstractHorse horse) {
         horse.setTamed(true);
         horse.setTemper(100);
         horse.equipSaddle(SoundSource.NEUTRAL);
         AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
         if (speed != null) {
            speed.setBaseValue(Math.max(speed.getBaseValue(), 0.32));
         }
      }
   }

   private static int issueClearCommand(ServerLevel level, Player player, ItemStack stack) {
      List<UUID> selected = getSelectedNpcs(stack);
      List<UUID> stillValid = new ArrayList<>();
      int commanded = 0;

      for (UUID uuid : selected) {
         Entity entity = level.getEntity(uuid);
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
         Entity entity = level.getEntity(uuid);
         if (entity instanceof PlayerOwnedNpcEntity) {
            PlayerOwnedNpcEntity npc = (PlayerOwnedNpcEntity)entity;
            if (npc.hasCommandPermission(player)) {
               if (!npc.isPassenger()) {
                  stillValid.add(uuid);
               } else {
                  npc.stopRiding();
                  npc.teleportTo((double)target.getX() + 0.5, (double)target.getY() + 1.0, (double)target.getZ() + 0.5);
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
      return npc != null && npc.isPassenger();
   }

   private static Component commandMessage(CommandBatonItem.BatonMode mode, int commanded) {
      return switch (mode) {
         case CLEAR_COMMAND -> Component.translatable("guistrings.npc.baton.clear_command", new Object[]{commanded});
         case DISMOUNT -> Component.translatable("guistrings.npc.baton.dismount_command", new Object[]{commanded});
         case MOVE -> Component.translatable("guistrings.npc.baton.move_command", new Object[]{commanded});
         case HOLD -> Component.translatable("guistrings.npc.baton.hold_command", new Object[]{commanded});
         case ATTACK -> Component.translatable("guistrings.npc.baton.move_attack_command", new Object[]{commanded});
         case MOUNT -> Component.translatable("guistrings.npc.baton.mount_command", new Object[]{commanded});
         case COMBAT_GROUP -> Component.translatable("guistrings.npc.baton.combat_group_ready", new Object[]{commanded});
      };
   }

   private static EntityHitResult getLookedAtEntity(Level level, Player player, double range) {
      Vec3 start = player.getEyePosition();
      Vec3 end = start.add(player.getLookAngle().scale(range));
      AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(1.0);
      return ProjectileUtil.getEntityHitResult(level, player, start, end, box, entity -> entity instanceof LivingEntity && entity.isPickable() && entity != player);
   }

   private static List<UUID> getSelectedNpcs(ItemStack stack) {
      return getUuidList(stack, "SelectedNpcs");
   }

   public static List<UUID> getCombatGroupNpcs(ItemStack stack) {
      return getUuidList(stack, "CombatGroupNpcs");
   }

   public static boolean isCombatGroupMode(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.getItem() instanceof CommandBatonItem && getMode(stack) == CommandBatonItem.BatonMode.COMBAT_GROUP;
   }

   public static boolean containsCombatGroupNpc(ItemStack stack, UUID npcId) {
      return npcId != null && isCombatGroupMode(stack) && getCombatGroupNpcs(stack).contains(npcId);
   }

   public static boolean selectsNpc(ItemStack stack, UUID npcId) {
      return stack != null && !stack.isEmpty() && npcId != null ? getSelectedNpcs(stack).contains(npcId) || getCombatGroupNpcs(stack).contains(npcId) : false;
   }

   private static List<UUID> getUuidList(ItemStack stack, String tagName) {
      List<UUID> selected = new ArrayList<>();
      CompoundTag tag = stack.getTag();
      if (tag != null && tag.contains(tagName, 9)) {
         ListTag list = tag.getList(tagName, 11);

         for (int i = 0; i < list.size(); i++) {
            selected.add(NbtUtils.loadUUID(list.get(i)));
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
      selected.forEach(uuid -> list.add(NbtUtils.createUUID(uuid)));
      stack.getOrCreateTag().put(tagName, list);
   }

   private static CommandBatonItem.BatonMode getMode(ItemStack stack) {
      CompoundTag tag = stack.getTag();
      return tag != null && tag.contains("Mode") ? CommandBatonItem.BatonMode.byName(tag.getString("Mode")) : CommandBatonItem.BatonMode.MOVE;
   }

   private static void changeMode(ItemStack stack, Player player, CommandBatonItem.BatonMode mode) {
      stack.getOrCreateTag().putString("Mode", mode.name);
      player.displayClientMessage(Component.translatable("guistrings.npc.baton.mode_changed", new Object[]{Component.translatable(mode.translationKey)}), true);
   }

   public static boolean isAttackMode(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.getItem() instanceof CommandBatonItem && getMode(stack) == CommandBatonItem.BatonMode.ATTACK;
   }

   public static int commandAttack(ServerLevel level, Player player, ItemStack stack, LivingEntity target) {
      if (isAttackMode(stack) && target != null && target != player && target.isAlive()) {
         if (target instanceof PlayerOwnedNpcEntity owned && owned.hasCommandPermission(player)) {
            return 0;
         }

         return issueAttackCommand(level, player, stack, target);
      } else {
         return 0;
      }
   }

   public static void clearSelections(ItemStack stack, Player player) {
      if (stack != null && !stack.isEmpty() && stack.getItem() instanceof CommandBatonItem) {
         int cleared = getSelectedNpcs(stack).size() + getCombatGroupNpcs(stack).size();
         stack.getOrCreateTag().remove("SelectedNpcs");
         stack.getOrCreateTag().remove("CombatGroupNpcs");
         stack.getOrCreateTag().remove("CombatGroupType");
         player.displayClientMessage(Component.translatable("guistrings.npc.baton.selections_cleared", new Object[]{cleared}), true);
      }
   }

   private static enum BatonMode {
      CLEAR_COMMAND("clear", "guistrings.npc.baton.clear"),
      DISMOUNT("dismount", "guistrings.npc.baton.dismount"),
      ATTACK("attack", "guistrings.npc.baton.attack"),
      MOVE("move", "guistrings.npc.baton.move"),
      HOLD("hold", "guistrings.npc.baton.hold"),
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
