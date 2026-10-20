package net.shadowmage.ancientwarfare.core.compat;

import java.util.EnumSet;
import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.SlashBlade.RegistryEvents;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.capability.inputstate.CapabilityInputState;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.EntityBlisteringSwords;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.EntityHeavyRainSwords;
import mods.flammpfeil.slashblade.entity.EntitySpiralSwords;
import mods.flammpfeil.slashblade.entity.EntityStormSwords;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.slasharts.SlashArts.ArtsType;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.InputCommand;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity;

final class SlashBladeCompatImpl {
   private static final String MOD_ID = "slashblade";
   public static final int MOVE_A1 = 1;
   public static final int MOVE_A2 = 2;
   public static final int MOVE_A3 = 3;
   public static final int MOVE_A4 = 4;
   public static final int MOVE_A4_EX = 5;
   public static final int MOVE_A5_EX = 6;
   public static final int MOVE_COMBO_B = 7;
   public static final int MOVE_COMBO_C = 8;
   public static final int MOVE_RAPID_SLASH = 9;
   public static final int MOVE_UPPERSLASH = 10;
   public static final int MOVE_UPPERSLASH_JUMP = 11;
   public static final int MOVE_RISING_STAR = 12;
   public static final int MOVE_AERIAL_A1 = 13;
   public static final int MOVE_AERIAL_A2 = 14;
   public static final int MOVE_AERIAL_A3 = 15;
   public static final int MOVE_AERIAL_B3 = 16;
   public static final int MOVE_AERIAL_B4 = 17;
   public static final int MOVE_AERIAL_CLEAVE = 18;
   public static final int MOVE_CHARGE = 19;
   public static final int MOVE_SLASH_ART = 20;
   public static final int MOVE_SLASH_ART_THRUST = 21;
   public static final int MOVE_SLASH_ART_RANGED = 22;
   public static final int KNOCKBACK_NONE = 0;
   public static final int KNOCKBACK_TOSS = 1;
   public static final int KNOCKBACK_SMASH = 2;
   public static final int KNOCKBACK_METEOR = 3;
   public static final int SUMMONED_SWORD_BASIC = 1;
   public static final int SUMMONED_SWORD_SPIRAL = 2;
   public static final int SUMMONED_SWORD_STORM = 3;
   public static final int SUMMONED_SWORD_BLISTERING = 4;
   public static final int SUMMONED_SWORD_HEAVY_RAIN = 5;
   public static final int CHARGE_NORMAL = 1;
   public static final int CHARGE_JUST = 2;
   public static final int CHARGE_SUPER = 3;
   public static final int SLASH_ART_NONE = 0;
   public static final int SLASH_ART_JUDGEMENT_CUT = 1;
   public static final int SLASH_ART_SAKURA_END = 2;
   public static final int SLASH_ART_VOID_SLASH = 3;
   public static final int SLASH_ART_CIRCLE_SLASH = 4;
   public static final int SLASH_ART_DRIVE_VERTICAL = 5;
   public static final int SLASH_ART_DRIVE_HORIZONTAL = 6;
   public static final int SLASH_ART_WAVE_EDGE = 7;
   public static final int SLASH_ART_PIERCING = 8;
   public static final String NPC_SUMMONED_SWORD_TAG = "ancientwarfareNpcSummonedSword";
   public static final String NPC_SUMMONED_SWORD_TARGET_TAG = "ancientwarfareNpcSummonedSwordTarget";
   private static final int DEFAULT_STUN_TICKS = 10;
   private static final int HEAVY_STUN_TICKS = 15;

   private SlashBladeCompatImpl() {
   }

   public static boolean isSlashBlade(ItemStack stack) {
      if (stack != null && !stack.isEmpty() && AWCorePlatform.isModLoaded("slashblade")) {
         try {
            return stack.getItem() instanceof ItemSlashBlade;
         } catch (LinkageError var2) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void preserveNpcBladeDurability(LivingEntity holder) {
      if (holder instanceof CombatNpcEntity npc && npc.isSwordsman() && !holder.level().isClientSide) {
         ItemStack blade = holder.getMainHandItem();
         if (!isSlashBlade(blade)) {
            return;
         }

         try {
            blade.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
               state.setDamage(0);
               if (state.isBroken() && !state.isSealed()) {
                  state.setBroken(false);
               }
            });
            blade.setDamageValue(0);
         } catch (Throwable var4) {
            AncientWarfareCore.LOG.warn("Failed to preserve SlashBlade NPC durability", var4);
         }

         return;
      }
   }

   public static boolean performGroundCombo(LivingEntity attacker, Entity target, int comboStep) {
      int move = switch (comboStep) {
         case 2 -> 2;
         case 3 -> 3;
         default -> 1;
      };
      return performTechniqueHit(attacker, target, move, 0.44F, 0);
   }

   public static boolean performTechniqueHit(LivingEntity attacker, Entity target, int move, float damageRatio, int knockback) {
      if (attacker != null && target != null && !attacker.level().isClientSide && isSlashBlade(attacker.getMainHandItem())) {
         try {
            ItemStack blade = attacker.getMainHandItem();
            ResourceLocation combo = comboForMove(move);
            boolean hasState = blade.getCapability(ItemSlashBlade.BLADESTATE).map(state -> {
               state.setComboSeq(combo);
               state.setLastActionTime(attacker.level().getGameTime());
               return true;
            }).orElse(false);
            if (!hasState) {
               return false;
            } else {
               float healthBefore = target instanceof LivingEntity livingTarget ? livingTarget.getHealth() : 0.0F;
               float absorptionBefore = target instanceof LivingEntity livingTargetx ? livingTargetx.getAbsorptionAmount() : 0.0F;
               int hurtTimeBefore = target instanceof LivingEntity livingTargetxx ? livingTargetxx.hurtTime : 0;
               Vec3 motionBefore = target.getDeltaMovement();
               AttackManager.doMeleeAttack(attacker, target, true, false, damageRatio);
               if (target instanceof LivingEntity livingTargetxxx) {
                  boolean hitSucceeded = livingTargetxxx.getHealth() < healthBefore
                     || livingTargetxxx.getAbsorptionAmount() < absorptionBefore
                     || livingTargetxxx.hurtTime > hurtTimeBefore;
                  if (hitSucceeded) {
                     applyTechniqueKnockback(livingTargetxxx, motionBefore, knockback);
                     StunManager.setStun(livingTargetxxx, (long)stunDurationForMove(move));
                  }
               }

               return true;
            }
         } catch (Throwable var14) {
            AncientWarfareCore.LOG.warn("Failed to perform SlashBlade NPC combo", var14);
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean setTechniqueState(LivingEntity attacker, int move) {
      if (attacker != null && !attacker.level().isClientSide && isSlashBlade(attacker.getMainHandItem())) {
         try {
            if (move >= 19) {
               return true;
            } else {
               ResourceLocation combo = comboForMove(move);
               return attacker.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).map(state -> {
                  state.setComboSeq(combo);
                  state.setLastActionTime(attacker.level().getGameTime());
                  return true;
               }).orElse(false);
            }
         } catch (Throwable var3) {
            AncientWarfareCore.LOG.warn("Failed to update SlashBlade NPC technique state", var3);
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean isPowered(LivingEntity attacker) {
      if (attacker != null && isSlashBlade(attacker.getMainHandItem())) {
         try {
            return AttackManager.isPowered(attacker);
         } catch (Throwable var2) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean canUseSummonedSwordArts(LivingEntity attacker) {
      if (attacker != null && isSlashBlade(attacker.getMainHandItem())) {
         try {
            ItemStack blade = attacker.getMainHandItem();
            return blade.getEnchantmentLevel(Enchantments.POWER_ARROWS) > 0 && SwordType.from(blade).contains(SwordType.BEWITCHED)
               ? blade.getCapability(ItemSlashBlade.BLADESTATE).map(state -> !state.isBroken() && !state.isSealed()).orElse(false)
               : false;
         } catch (Throwable var2) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static boolean canUseThornsGuard(LivingEntity attacker) {
      if (attacker != null && attacker.onGround() && !attacker.isPassenger() && isSlashBlade(attacker.getMainHandItem())) {
         try {
            ItemStack blade = attacker.getMainHandItem();
            return blade.getEnchantmentLevel(Enchantments.THORNS) <= 0
               ? false
               : blade.getCapability(ItemSlashBlade.BLADESTATE).map(state -> !state.isBroken() && !state.isSealed()).orElse(false);
         } catch (Throwable var2) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void setThornsGuardInput(LivingEntity attacker, boolean guarding) {
      if (attacker != null && AWCorePlatform.isModLoaded("slashblade")) {
         try {
            attacker.getCapability(CapabilityInputState.INPUT_STATE).ifPresent(input -> {
               if (guarding) {
                  if (input.getCommands().add(InputCommand.SNEAK)) {
                     input.getLastPressTimes().put(InputCommand.SNEAK, attacker.level().getGameTime());
                  }

                  input.getCommands().removeAll(InputCommand.move);
               } else {
                  input.getCommands().remove(InputCommand.SNEAK);
               }
            });
            attacker.setShiftKeyDown(guarding);
         } catch (Throwable var3) {
            AncientWarfareCore.LOG.warn("Failed to update SlashBlade NPC guard input", var3);
         }
      }
   }

   public static int getHeldSlashArt(LivingEntity attacker) {
      if (attacker != null && isSlashBlade(attacker.getMainHandItem())) {
         try {
            return attacker.getMainHandItem()
               .getCapability(ItemSlashBlade.BLADESTATE)
               .map(state -> slashArtFromId(SlashArts.getRegistryKey(state.getSlashArts())))
               .orElse(0);
         } catch (Throwable var2) {
            return 0;
         }
      } else {
         return 0;
      }
   }

   public static boolean canUseChargedSlashArt(LivingEntity attacker, int chargeType) {
      if (attacker != null && !attacker.isPassenger() && isSlashBlade(attacker.getMainHandItem())) {
         try {
            ItemStack blade = attacker.getMainHandItem();
            return blade.getCapability(ItemSlashBlade.BLADESTATE)
               .map(
                  state -> {
                     EnumSet<SwordType> swordTypes = SwordType.from(blade);
                     if (state.isBroken() || state.isSealed() || !swordTypes.contains(SwordType.ENCHANTED) || getHeldSlashArt(attacker) == 0) {
                        return false;
                     } else {
                        return chargeType != 3
                           ? true
                           : attacker.onGround()
                              && state.getDamage() <= 0
                              && swordTypes.contains(SwordType.BEWITCHED)
                              && swordTypes.contains(SwordType.FIERCEREDGE);
                     }
                  }
               )
               .orElse(false);
         } catch (Throwable var3) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static int beginChargedSlashArt(LivingEntity attacker, LivingEntity target, int chargeType) {
      if (attacker != null && target != null && !attacker.level().isClientSide && target.isAlive() && canUseChargedSlashArt(attacker, chargeType)) {
         try {
            ItemStack blade = attacker.getMainHandItem();
            return blade.getCapability(ItemSlashBlade.BLADESTATE).map(state -> {
               ArtsType nativeType = chargeType == 3 ? ArtsType.Super : (chargeType == 2 ? ArtsType.Jackpot : ArtsType.Success);
               ResourceLocation nativeCombo = state.getSlashArts().doArts(nativeType, attacker);
               if (nativeCombo != null && !nativeCombo.equals(ComboStateRegistry.NONE.getId())) {
                  int art = chargeType == 3 ? 1 : slashArtFromId(SlashArts.getRegistryKey(state.getSlashArts()));
                  if (art == 0) {
                     return 0;
                  } else {
                     state.setTargetEntityId(target);
                     if (chargeType != 3) {
                        int cost = state.getSlashArts().getProudSoulCost();
                        if (state.getProudSoulCount() >= cost) {
                           state.setProudSoulCount(state.getProudSoulCount() - cost);
                        }
                     }

                     attacker.swing(InteractionHand.MAIN_HAND, true);
                     attacker.level().playSound(null, attacker.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.8F, chargeType == 3 ? 0.65F : 0.9F);
                     return art;
                  }
               } else {
                  return 0;
               }
            }).orElse(0);
         } catch (Throwable var4) {
            AncientWarfareCore.LOG.warn("Failed to begin SlashBlade NPC charged art", var4);
            return 0;
         }
      } else {
         return 0;
      }
   }

   public static boolean tickChargedSlashArt(LivingEntity attacker, LivingEntity target, int art, int chargeType, int executionTick) {
      if (attacker != null && target != null && !attacker.level().isClientSide && target.isAlive() && isSlashBlade(attacker.getMainHandItem())) {
         float tierScale = chargeType == 3 ? 1.55F : (chargeType == 2 ? 1.18F : 1.0F);

         try {
            return switch (art) {
               case 1 -> {
                  int interval = chargeType == 3 ? 3 : 4;
                  int strikes = chargeType == 3 ? 5 : (chargeType == 2 ? 3 : 2);
                  if (executionTick >= 2 && executionTick < 2 + interval * strikes && (executionTick - 2) % interval == 0) {
                     spawnTargetBurst(attacker, target, chargeType == 3 ? 18 : 10);
                     yield performTechniqueHit(attacker, target, 20, 0.48F * tierScale, 0);
                  } else {
                     yield true;
                  }
               }
               case 2 -> {
                  if (executionTick != 2 && executionTick != 8) {
                     yield true;
                  } else {
                     spawnTargetBurst(attacker, target, 8);
                     yield performTechniqueHit(attacker, target, 20, (executionTick == 8 ? 0.76F : 0.5F) * tierScale, 0);
                  }
               }
               case 3 -> {
                  if (executionTick != 13 && executionTick != 17 && executionTick != 21) {
                     yield true;
                  } else {
                     spawnTargetBurst(attacker, target, 12);
                     yield performTechniqueHit(attacker, target, 20, 0.62F * tierScale, 0);
                  }
               }
               case 4 -> {
                  if (executionTick >= 4 && executionTick <= 10 && executionTick % 2 == 0) {
                     spawnTargetBurst(attacker, target, 6);
                     yield performTechniqueHit(attacker, target, 20, 0.325F * tierScale, 0);
                  } else {
                     yield true;
                  }
               }
               case 5 -> {
                  if (executionTick == 3) {
                     spawnDrive(attacker, target, -90.0F, 1.5 * (double)tierScale, 2.0F, 14, true);
                  }

                  yield true;
               }
               case 6 -> {
                  if (executionTick == 3) {
                     spawnDrive(attacker, target, 0.0F, 1.5 * (double)tierScale, 2.0F, 14, true);
                  }

                  yield true;
               }
               case 7 -> {
                  if (executionTick == 3) {
                     for (int i = 0; i < 5; i++) {
                        spawnDrive(attacker, target, 90.0F, 0.4 * (double)tierScale, 0.65F + (float)i * 0.12F, 24, false);
                     }
                  }

                  yield true;
               }
               case 8 -> executionTick >= 3 && executionTick <= 7 && executionTick % 2 == 1 && attacker.distanceToSqr(target) <= 30.25
               ? performTechniqueHit(attacker, target, 21, 0.52F * tierScale, executionTick == 7 ? 1 : 0)
               : true;
               default -> false;
            };
         } catch (Throwable var8) {
            AncientWarfareCore.LOG.warn("Failed to tick SlashBlade NPC charged art", var8);
            return false;
         }
      } else {
         return false;
      }
   }

   public static int getChargedSlashArtExecutionTicks(int art, int chargeType) {
      if (art == 3) {
         return 28;
      } else if (art == 1 && chargeType == 3) {
         return 24;
      } else if (art == 1) {
         return 16;
      } else {
         return art != 2 && art != 4 ? 12 : 16;
      }
   }

   public static int getSlashArtPose(int art) {
      return switch (art) {
         case 5, 6, 7 -> 22;
         case 8 -> 21;
         default -> 20;
      };
   }

   public static boolean isPiercingSlashArt(int art) {
      return art == 8;
   }

   public static void emitChargedSlashArtParticles(LivingEntity attacker, int chargeType) {
      if (attacker.level() instanceof ServerLevel serverLevel) {
         int count = chargeType == 3 ? 8 : 4;
         serverLevel.sendParticles(
            chargeType == 3 ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.ENCHANT,
            attacker.getX(),
            attacker.getY() + (double)attacker.getEyeHeight() * 0.65,
            attacker.getZ(),
            count,
            0.45,
            0.65,
            0.45,
            0.03
         );
      }
   }

   private static int slashArtFromId(ResourceLocation id) {
      if (id != null && "slashblade".equals(id.getNamespace())) {
         String var1 = id.getPath();

         return switch (var1) {
            case "judgement_cut" -> 1;
            case "sakura_end" -> 2;
            case "void_slash" -> 3;
            case "circle_slash" -> 4;
            case "drive_vertical" -> 5;
            case "drive_horizontal" -> 6;
            case "wave_edge" -> 7;
            case "piercing" -> 8;
            default -> 0;
         };
      } else {
         return 0;
      }
   }

   private static void spawnTargetBurst(LivingEntity attacker, LivingEntity target, int count) {
      if (attacker.level() instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(
            ParticleTypes.SWEEP_ATTACK,
            target.getX(),
            target.getY() + (double)target.getBbHeight() * 0.55,
            target.getZ(),
            count,
            Math.max(0.2, (double)target.getBbWidth() * 0.45),
            (double)target.getBbHeight() * 0.35,
            Math.max(0.2, (double)target.getBbWidth() * 0.45),
            0.02
         );
      }
   }

   private static void spawnDrive(LivingEntity attacker, LivingEntity target, float roll, double damage, float speed, int lifetime, boolean critical) {
      EntityDrive drive = new EntityDrive(RegistryEvents.Drive, attacker.level());
      Vec3 origin = attacker.getEyePosition().add(attacker.getLookAngle().scale(0.35));
      Vec3 aim = targetPoint(target).add(target.getDeltaMovement().scale(0.35));
      Vec3 direction = aim.subtract(origin);
      if (direction.lengthSqr() < 1.0E-6) {
         direction = attacker.getLookAngle();
      }

      drive.setPos(origin.x, origin.y, origin.z);
      drive.setOwner(attacker);
      drive.setDamage(damage);
      drive.setSpeed(speed);
      drive.setRotationRoll(roll);
      drive.setLifetime((float)lifetime);
      drive.setIsCritical(critical);
      drive.setKnockBack(KnockBacks.cancel);
      attacker.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> drive.setColor(state.getColorCode()));
      drive.getPersistentData().putBoolean("ancientwarfareNpcSummonedSword", true);
      drive.getPersistentData().putInt("ancientwarfareNpcSummonedSwordTarget", target.getId());
      drive.shoot(direction.x, direction.y, direction.z, speed, 0.0F);
      attacker.level().addFreshEntity(drive);
   }

   public static boolean performSummonedSwordArt(LivingEntity attacker, LivingEntity target, int art) {
      if (attacker != null && target != null && !attacker.level().isClientSide && target.isAlive() && canUseSummonedSwordArts(attacker)) {
         try {
            ItemStack blade = attacker.getMainHandItem();
            int powerLevel = blade.getEnchantmentLevel(Enchantments.POWER_ARROWS);
            int cost = art == 1 ? (Integer)SlashBladeConfig.SUMMON_SWORD_COST.get() : (Integer)SlashBladeConfig.SUMMON_SWORD_ART_COST.get();
            return blade.getCapability(ItemSlashBlade.BLADESTATE)
               .map(
                  state -> {
                     if (state.getProudSoulCount() < cost) {
                        return false;
                     } else {
                        state.setProudSoulCount(state.getProudSoulCount() - cost);
                        state.setTargetEntityId(target);
                        int color = state.getColorCode();
                        switch (art) {
                           case 2:
                              spawnSpiralSwords(attacker, target, powerLevel, color);
                              break;
                           case 3:
                              spawnStormSwords(attacker, target, powerLevel, color);
                              break;
                           case 4:
                              spawnBlisteringSwords(attacker, target, powerLevel, color);
                              break;
                           case 5:
                              spawnHeavyRainSwords(attacker, target, powerLevel, color);
                              break;
                           default:
                              spawnBasicSummonedSword(attacker, target, powerLevel, color);
                        }

                        attacker.level()
                           .playSound(
                              null, attacker.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.35F, 1.35F + attacker.getRandom().nextFloat() * 0.2F
                           );
                        return true;
                     }
                  }
               )
               .orElse(false);
         } catch (Throwable var6) {
            AncientWarfareCore.LOG.warn("Failed to perform SlashBlade NPC summoned-sword art", var6);
            return false;
         }
      } else {
         return false;
      }
   }

   private static void spawnBasicSummonedSword(LivingEntity attacker, LivingEntity target, int power, int color) {
      EntityAbstractSummonedSword sword = new EntityAbstractSummonedSword(RegistryEvents.SummonedSword, attacker.level());
      Vec3 side = horizontalSide(attacker);
      Vec3 pos = attacker.getEyePosition().add(side.scale(attacker.tickCount % 2 == 0 ? 0.85 : -0.85));
      configureAndShoot(sword, attacker, target, power, color, pos, targetPoint(target), 3.1F, 0.15F);
   }

   private static void spawnSpiralSwords(LivingEntity attacker, LivingEntity target, int power, int color) {
      int count = 6;
      Vec3 center = attacker.position().add(0.0, (double)attacker.getEyeHeight() * 0.55, 0.0);

      for (int i = 0; i < count; i++) {
         double angle = (Math.PI * 2) * (double)i / (double)count;
         Vec3 pos = center.add(Math.cos(angle) * 1.8, (double)(i % 2) * 0.35, Math.sin(angle) * 1.8);
         EntitySpiralSwords sword = new EntitySpiralSwords(RegistryEvents.SpiralSwords, attacker.level());
         sword.doFire();
         sword.setDelay(360 / count * i);
         configureAndShoot(sword, attacker, target, power, color, pos, targetPoint(target), 3.0F, 0.35F);
      }
   }

   private static void spawnStormSwords(LivingEntity attacker, LivingEntity target, int power, int color) {
      int count = 6;
      Vec3 center = target.position().add(0.0, (double)target.getBbHeight() * 0.55, 0.0);

      for (int i = 0; i < count; i++) {
         double angle = (Math.PI * 2) * (double)i / (double)count;
         Vec3 pos = center.add(Math.cos(angle) * 3.6, 0.5 + (double)(i % 2) * 0.6, Math.sin(angle) * 3.6);
         EntityStormSwords sword = new EntityStormSwords(RegistryEvents.StormSwords, attacker.level());
         sword.doFire();
         sword.setDelay(360 / count * i);
         configureAndShoot(sword, attacker, target, power, color, pos, targetPoint(target), 2.8F, 0.2F);
      }
   }

   private static void spawnBlisteringSwords(LivingEntity attacker, LivingEntity target, int power, int color) {
      int count = 6;
      Vec3 center = attacker.getEyePosition();
      Vec3 side = horizontalSide(attacker);

      for (int i = 0; i < count; i++) {
         double lateral = ((double)i - (double)(count - 1) * 0.5) * 0.38;
         Vec3 pos = center.add(side.scale(lateral)).add(0.0, (double)(i % 2) * 0.28 - 0.14, 0.0);
         EntityBlisteringSwords sword = new EntityBlisteringSwords(RegistryEvents.BlisteringSwords, attacker.level());
         sword.doFire();
         sword.setDelay(i);
         configureAndShoot(sword, attacker, target, power, color, pos, targetPoint(target), 3.25F, 0.3F);
      }
   }

   private static void spawnHeavyRainSwords(LivingEntity attacker, LivingEntity target, int power, int color) {
      int count = 9;
      Vec3 predicted = targetPoint(target).add(target.getDeltaMovement().scale(0.45));

      for (int i = 0; i < count; i++) {
         double angle = (Math.PI * 2) * (double)i / (double)count;
         double radius = i == 0 ? 0.0 : 1.3 + (double)(i % 3) * 0.55;
         Vec3 pos = predicted.add(Math.cos(angle) * radius, 7.0 + (double)(i % 2) * 0.7, Math.sin(angle) * radius);
         EntityHeavyRainSwords sword = new EntityHeavyRainSwords(RegistryEvents.HeavyRainSwords, attacker.level());
         sword.doFire();
         sword.setDelay(i);
         configureAndShoot(sword, attacker, target, power, color, pos, predicted, 4.0F, 0.35F);
      }
   }

   private static void configureAndShoot(
      EntityAbstractSummonedSword sword,
      LivingEntity attacker,
      LivingEntity target,
      int power,
      int color,
      Vec3 position,
      Vec3 aim,
      float velocity,
      float inaccuracy
   ) {
      sword.setPos(position);
      sword.setOwner(attacker);
      sword.setColor(color);
      sword.setRoll(attacker.getRandom().nextFloat() * 360.0F);
      sword.setDamage((double)power);
      sword.getPersistentData().putBoolean("ancientwarfareNpcSummonedSword", true);
      sword.getPersistentData().putInt("ancientwarfareNpcSummonedSwordTarget", target.getId());
      Vec3 direction = aim.subtract(position);
      if (direction.lengthSqr() < 1.0E-6) {
         direction = attacker.getLookAngle();
      }

      sword.shoot(direction.x, direction.y, direction.z, velocity, inaccuracy);
      attacker.level().addFreshEntity(sword);
   }

   private static Vec3 targetPoint(LivingEntity target) {
      return target.position().add(0.0, (double)target.getBbHeight() * 0.55, 0.0).add(target.getDeltaMovement().scale(0.18));
   }

   private static Vec3 horizontalSide(LivingEntity attacker) {
      Vec3 look = attacker.getLookAngle();
      Vec3 side = new Vec3(-look.z, 0.0, look.x);
      return side.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
   }

   private static ResourceLocation comboForMove(int move) {
      return switch (move) {
         case 2 -> ComboStateRegistry.COMBO_A2.getId();
         case 3 -> ComboStateRegistry.COMBO_A3.getId();
         case 4 -> ComboStateRegistry.COMBO_A4.getId();
         case 5 -> ComboStateRegistry.COMBO_A4_EX.getId();
         case 6 -> ComboStateRegistry.COMBO_A5.getId();
         case 7 -> ComboStateRegistry.COMBO_B1.getId();
         case 8 -> ComboStateRegistry.COMBO_C.getId();
         case 9 -> ComboStateRegistry.RAPID_SLASH.getId();
         case 10 -> ComboStateRegistry.UPPERSLASH.getId();
         case 11 -> ComboStateRegistry.UPPERSLASH_JUMP.getId();
         case 12 -> ComboStateRegistry.RISING_STAR.getId();
         case 13 -> ComboStateRegistry.AERIAL_RAVE_A1.getId();
         case 14 -> ComboStateRegistry.AERIAL_RAVE_A2.getId();
         case 15 -> ComboStateRegistry.AERIAL_RAVE_A3.getId();
         case 16 -> ComboStateRegistry.AERIAL_RAVE_B3.getId();
         case 17 -> ComboStateRegistry.AERIAL_RAVE_B4.getId();
         case 18 -> ComboStateRegistry.AERIAL_CLEAVE.getId();
         default -> ComboStateRegistry.COMBO_A1.getId();
      };
   }

   private static int stunDurationForMove(int move) {
      return switch (move) {
         case 10, 18 -> 15;
         default -> 10;
      };
   }

   private static void applyTechniqueKnockback(LivingEntity target, Vec3 motionBefore, int knockback) {
      Vec3 motionAfter = target.getDeltaMovement();
      if (knockback == 1) {
         target.setDeltaMovement(motionBefore.x, Math.max(motionBefore.y, 0.75), motionBefore.z);
         target.setOnGround(false);
      } else if (knockback == 2) {
         double x = motionBefore.x + (motionAfter.x - motionBefore.x) * 0.25;
         double z = motionBefore.z + (motionAfter.z - motionBefore.z) * 0.25;
         target.setDeltaMovement(x, motionBefore.y, z);
      } else if (knockback == 3) {
         target.setDeltaMovement(motionBefore.x, Math.min(motionBefore.y, -1.25), motionBefore.z);
         target.setOnGround(false);
      } else {
         target.setDeltaMovement(motionBefore);
      }

      target.hasImpulse = true;
   }
}
