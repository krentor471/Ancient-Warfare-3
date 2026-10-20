package net.shadowmage.ancientwarfare.core.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.shadowmage.ancientwarfare.core.common.npc.NpcCommandType;
import net.shadowmage.ancientwarfare.core.common.npc.NpcTargeting;
import net.shadowmage.ancientwarfare.core.item.CommandBatonItem;

final class CombatGroupController {
   private static final int FORMATION_COLUMNS = 4;
   private static final double FORMATION_SPACING = 3.25;
   private static final double DRAGON_FORMATION_SPACING = 5.0;
   private static final double ENGAGEMENT_SEARCH_RANGE = 40.0;

   private CombatGroupController() {
   }

   static void tick(PlayerOwnedNpcEntity commander) {
      if (!commander.level().isClientSide && commander.level() instanceof ServerLevel level) {
         ItemStack baton = commander.getMainHandItem();
         if (CommandBatonItem.isCombatGroupMode(baton)) {
            List<UUID> selected = CommandBatonItem.getCombatGroupNpcs(baton);
            if (!selected.isEmpty()) {
               List<CombatGroupController.GroupMember> members = resolveMembers(level, commander, selected);
               boolean dragonGroup = commander.isDragonRiderCombatGroupCommander();
               boolean hippogryphGroup = commander.isHippogryphCombatGroupCommander();
               boolean hippocampusGroup = commander.isHippocampusCombatGroupCommander();
               assignFormation(commander, members, dragonGroup || hippogryphGroup);
               coordinateEngagement(commander, members);
            }
         }
      }
   }

   private static List<CombatGroupController.GroupMember> resolveMembers(ServerLevel level, PlayerOwnedNpcEntity commander, List<UUID> selected) {
      List<CombatGroupController.GroupMember> members = new ArrayList<>();
      boolean dragonGroup = commander.isDragonRiderCombatGroupCommander();
      boolean hippogryphGroup = commander.isHippogryphCombatGroupCommander();
      boolean hippocampusGroup = commander.isHippocampusCombatGroupCommander();

      for (int order = 0; order < selected.size(); order++) {
         Entity entity = level.getEntity(selected.get(order));
         if (entity instanceof PlayerOwnedNpcEntity) {
            PlayerOwnedNpcEntity npc = (PlayerOwnedNpcEntity)entity;
            if (npc != commander
               && npc.isAlive()
               && npc.getOwner().equals(commander.getOwner())
               && npc.isCombatGroupEligible()
               && (
                  dragonGroup
                     ? npc instanceof CombatNpcEntity && npc.isLogicalDragonRider()
                     : (
                        hippogryphGroup
                           ? npc instanceof CombatNpcEntity && npc.isHippogryphRider()
                           : (
                              hippocampusGroup
                                 ? npc instanceof CombatNpcEntity && npc.isHippocampusRider()
                                 : !npc.isLogicalDragonRider() && !npc.isHippogryphRider()
                           )
                     )
               )
               && (!commander.isSiegeEngineerProfession() || npc.isSiegeEngineerProfession())
               && !npc.isCombatGroupCommander()
               && npc.canJoinCombatGroup(commander)) {
               members.add(new CombatGroupController.GroupMember(npc, formationRank(npc), order));
            }
         }
      }

      members.sort(Comparator.comparingInt(CombatGroupController.GroupMember::rank).thenComparingInt(CombatGroupController.GroupMember::selectionOrder));
      return members;
   }

   private static void assignFormation(PlayerOwnedNpcEntity commander, List<CombatGroupController.GroupMember> members, boolean dragonGroup) {
      if (dragonGroup) {
         for (int index = 0; index < members.size(); index++) {
            int row = index / 4 + 1;
            int columnCount = Math.min(4, members.size() - (row - 1) * 4);
            int column = index % 4;
            double lateral = ((double)column - (double)(columnCount - 1) / 2.0) * 5.0;
            members.get(index).npc().assignCombatGroupFormation(commander, lateral, (double)row * 5.0);
         }
      } else {
         int rearRow = 1;

         for (int rank = 0; rank <= 2; rank++) {
            int formationRank = rank;
            List<CombatGroupController.GroupMember> roleMembers = members.stream().filter(member -> member.rank() == formationRank).toList();

            for (int start = 0; start < roleMembers.size(); start += 4) {
               int rowSize = Math.min(4, roleMembers.size() - start);

               for (int column = 0; column < rowSize; column++) {
                  double lateral = ((double)column - (double)(rowSize - 1) / 2.0) * 3.25;
                  roleMembers.get(start + column).npc().assignCombatGroupFormation(commander, lateral, (double)rearRow * 3.25);
               }

               rearRow++;
            }
         }
      }
   }

   private static void coordinateEngagement(PlayerOwnedNpcEntity commander, List<CombatGroupController.GroupMember> members) {
      LivingEntity commanderTarget = commander.getTarget();
      if (NpcTargeting.canHarm(commander, commanderTarget)) {
         AABB searchBox = commander.getBoundingBox().inflate(40.0);
         List<LivingEntity> nearbyEnemies = new ArrayList<>(
            commander.level()
               .getEntitiesOfClass(LivingEntity.class, searchBox, target -> target instanceof FactionNpcEntity || NpcTargeting.isAutomaticHostile(commander, target))
         );
         if (!nearbyEnemies.contains(commanderTarget)) {
            nearbyEnemies.add(commanderTarget);
         }

         Map<UUID, Integer> assignments = new HashMap<>();
         assignments.put(commanderTarget.getUUID(), 1);

         for (CombatGroupController.GroupMember groupMember : members) {
            PlayerOwnedNpcEntity member = groupMember.npc();
            LivingEntity currentTarget = member.getTarget();
            if (member.getCurrentCommand() != NpcCommandType.NONE || currentTarget != null && currentTarget.isAlive()) {
               if (currentTarget != null && currentTarget.isAlive()) {
                  assignments.merge(currentTarget.getUUID(), 1, Integer::sum);
               }
            } else {
               double range = member.isSiegeEngineerProfession() ? 96.0 : Math.max(24.0, member.getAttributeValue(Attributes.FOLLOW_RANGE));
               if (commander.isDragonRiderCombatGroupCommander()) {
                  if (validTargetFor(member, commanderTarget, true)) {
                     member.setTarget(commanderTarget);
                     assignments.merge(commanderTarget.getUUID(), 1, Integer::sum);
                  }
               } else if (commander.isHippogryphCombatGroupCommander()) {
                  if (validTargetFor(member, commanderTarget, true)) {
                     member.setTarget(commanderTarget);
                     assignments.merge(commanderTarget.getUUID(), 1, Integer::sum);
                  }
               } else if (commander.isHippocampusCombatGroupCommander()) {
                  if (validTargetFor(member, commanderTarget, true)) {
                     member.setTarget(commanderTarget);
                     assignments.merge(commanderTarget.getUUID(), 1, Integer::sum);
                  }
               } else {
                  LivingEntity chosen = nearbyEnemies.stream()
                     .filter(target -> validTargetFor(member, target, target == commanderTarget))
                     .filter(target -> member.distanceToSqr(target) <= range * range)
                     .min(
                        Comparator.comparingDouble(
                           target -> member.distanceToSqr(target) + (double)assignments.getOrDefault(target.getUUID(), 0).intValue() * 144.0
                        )
                     )
                     .orElse(null);
                  if (chosen != null) {
                     member.setTarget(chosen);
                     assignments.merge(chosen.getUUID(), 1, Integer::sum);
                  }
               }
            }
         }
      }
   }

   private static boolean validTargetFor(PlayerOwnedNpcEntity member, LivingEntity target, boolean commandedTarget) {
      if (!NpcTargeting.canHarm(member, target)) {
         return false;
      } else if (commandedTarget) {
         return true;
      } else {
         return target instanceof FactionNpcEntity factionNpc ? factionNpc.isHostileTowards(member) : NpcTargeting.isAutomaticHostile(member, target);
      }
   }

   private static int formationRank(PlayerOwnedNpcEntity npc) {
      if (npc.isSiegeEngineerProfession()) {
         return 2;
      } else {
         return !(npc.getMainHandItem().getItem() instanceof BowItem) && !(npc.getMainHandItem().getItem() instanceof CrossbowItem) ? 0 : 1;
      }
   }

   private static record GroupMember(PlayerOwnedNpcEntity npc, int rank, int selectionOrder) {
   }
}
