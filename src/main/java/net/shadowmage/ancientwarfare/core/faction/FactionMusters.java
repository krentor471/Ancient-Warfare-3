package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

/**
 * A faction going to war against another faction musters a host first. Over a few days soldiers
 * leave the garrisons and gather at the town nearest the enemy; then the host sets out under the
 * marshal. What it is made of comes from the stores: wood makes bows and arrows, leather and grain
 * put men on horses, iron, coal and copper put them in mail, and grain feeds them on the road. A
 * host whose granaries run dry goes hungry and melts away.
 *
 * The marshal leads it by his character: a bold one musters quickly and never turns back, a
 * contented one takes his time, avoids a stronger enemy and leaves strong walls alone, a cruel one
 * sacks what he takes, a kind one spares it, a drunkard dawdles, and a suspicious one keeps
 * soldiers at home. A marshal on the losing side of a battle may fall.
 *
 * This is between factions only; a player's wars are fought by the other armies.
 */
public final class FactionMusters extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_faction_musters";
   private static final int BASE_DAYS = 2;
   /** Grain each soldier takes along when the host sets out, and eats a day on the road (per four). */
   private static final int PROVISIONS = 2;
   private static final int RATIONS = 4;
   /** Stores each archer and each rider ties up; a quarter of it is used up on the campaign. */
   private static final int BOW_WOOD = 20;
   private static final int HARNESS = 10;
   private static final int FODDER = 20;
   private static final double MARSHAL_FALLS = 0.15;
   private static final int SACK_GOLD = 150;

   private final List<Muster> musters = new ArrayList<>();

   static final class Muster {
      final String faction;
      final String defender;
      final int target;
      final int rally;
      final int want;
      int gathered;
      final long ready;

      Muster(String faction, String defender, int target, int rally, int want, int gathered, long ready) {
         this.faction = faction;
         this.defender = defender;
         this.target = target;
         this.rally = rally;
         this.want = want;
         this.gathered = gathered;
         this.ready = ready;
      }
   }

   public static FactionMusters get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(FactionMusters::load, FactionMusters::new, DATA_NAME);
   }

   private static boolean has(ServerLevel level, String faction, FactionPeople.Character character) {
      return FactionPeople.has(FactionCourt.marshalCharacter(level, faction), character);
   }

   static boolean mustering(ServerLevel level, String faction, String defender) {
      return get(level).musters.stream().anyMatch(muster -> muster.faction.equals(faction) && muster.defender.equals(defender));
   }

   /**
    * The faction calls up a host of about force soldiers against the target; they gather over the
    * next days at its town nearest the target. Returns false when it has no town to gather at.
    */
   static boolean muster(ServerLevel level, FactionWorldData data, String faction, FactionWorldData.Settlement target, int force) {
      FactionWorldData.Settlement rally = null;
      double best = Double.MAX_VALUE;
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         double distance = settlement.horizontalDistanceSquared(target.center().getX(), target.center().getZ());
         if (settlement.faction().equals(faction) && distance < best) {
            best = distance;
            rally = settlement;
         }
      }

      if (rally == null) {
         return false;
      }

      FactionRulers.Trait trait = FactionCourt.marshalTrait(level, faction);
      int days = BASE_DAYS;
      days += has(level, faction, FactionPeople.Character.AMBITIOUS) || trait == FactionRulers.Trait.WARLIKE ? -1 : 0;
      days += has(level, faction, FactionPeople.Character.CONTENT) || trait == FactionRulers.Trait.CAUTIOUS ? 1 : 0;
      days += has(level, faction, FactionPeople.Character.DRUNKARD) ? 1 : 0;
      days = Mth.clamp(days, 1, 4);
      double size = force;
      // A careful marshal wants more men before he goes; a suspicious one leaves some at home.
      size *= trait == FactionRulers.Trait.CAUTIOUS ? 1.25 : 1.0;
      size *= has(level, faction, FactionPeople.Character.PARANOID) ? 0.7 : 1.0;
      int want = Mth.clamp((int)Math.round(size), 1, FactionAI.MAX_ARMY);
      long day = Math.max(0L, data.lastSimulatedDay());
      FactionMusters musters = get(level);
      musters.musters.add(new Muster(faction, target.faction(), target.id(), rally.id(), want, 0, day + days));
      musters.setDirty();
      AncientWarfareCore.LOG.info("Faction {} musters {} soldiers at #{} against {} #{} for {} days", faction, want, rally.id(), target.faction(), target.id(), days);
      FactionNews.tell(
         level,
         Component.translatable(
            "news.ancientwarfare.muster",
            FactionNews.factionName(faction),
            FactionNews.named(rally),
            FactionNews.factionName(target.faction()),
            FactionCourt.courtierName(level, faction, FactionCourt.Role.MARSHAL)
         )
      );
      return true;
   }

   static void simulateDay(ServerLevel level, FactionWorldData data) {
      FactionMusters musters = get(level);
      long day = Math.max(0L, data.lastSimulatedDay());
      for (Muster muster : new ArrayList<>(musters.musters)) {
         FactionWorldData.Settlement target = data.settlement(muster.target);
         FactionWorldData.Settlement rally = data.settlement(muster.rally);
         boolean over = target == null
            || rally == null
            || !rally.faction().equals(muster.faction)
            || !target.faction().equals(muster.defender)
            || FactionDiplomacy.relation(level, muster.faction, muster.defender).state() != RelationState.WAR;
         if (over) {
            // Peace, or the town lost: the host is sent home.
            musters.musters.remove(muster);
            FactionAI.returnSoldiers(data, muster.faction, muster.gathered);
            continue;
         }

         long left = Math.max(1L, muster.ready - day + 1);
         int share = (int)Math.ceil((muster.want - muster.gathered) / (double)left);
         muster.gathered += FactionAI.takeLosses(data, muster.faction, Math.max(0, share));
         if (day >= muster.ready) {
            musters.musters.remove(muster);
            setOut(level, data, muster, rally, target);
         }
      }

      for (FactionWorldData.Campaign campaign : new ArrayList<>(data.campaigns())) {
         if (campaign.led) {
            feed(level, data, campaign);
         }
      }

      musters.setDirty();
   }

   /** The host sets out, fitted out from the stores. */
   private static void setOut(ServerLevel level, FactionWorldData data, Muster muster, FactionWorldData.Settlement rally, FactionWorldData.Settlement target) {
      if (muster.gathered <= 0) {
         return;
      }

      int size = muster.gathered;
      int[] stores = data.stores(muster.faction);
      int arms = FactionArmament.tier(stores);
      // Each archer's bow and arrows cost wood; each rider's horse wants leather for its harness and grain.
      int bowmen = Mth.clamp(stores[FactionResources.WOOD] / BOW_WOOD, size / 10, size * 45 / 100);
      int riders = Mth.clamp(Math.min(stores[FactionResources.LEATHER] / HARNESS, stores[FactionResources.GRAIN] / FODDER), 0, size * 30 / 100);
      int archers = bowmen * 100 / size;
      int horse = riders * 100 / size;
      stores[FactionResources.WOOD] = Math.max(0, stores[FactionResources.WOOD] - bowmen * BOW_WOOD / 4);
      stores[FactionResources.LEATHER] = Math.max(0, stores[FactionResources.LEATHER] - riders * HARNESS / 4);
      stores[FactionResources.GRAIN] = Math.max(0, stores[FactionResources.GRAIN] - riders * FODDER / 4);
      boolean hungry = stores[FactionResources.GRAIN] < size * PROVISIONS;
      stores[FactionResources.GRAIN] = Math.max(0, stores[FactionResources.GRAIN] - size * PROVISIONS);
      FactionWorldData.Campaign campaign = FactionCampaigns.launchMustered(level, data, rally, target, size);
      campaign.led = true;
      campaign.archers = archers;
      campaign.horse = horse;
      campaign.arms = arms;
      campaign.hungry = hungry;
      data.setDirty();
      FactionNews.tell(
         level,
         Component.translatable(
            "news.ancientwarfare.host_set_out",
            FactionNews.factionName(muster.faction),
            size,
            FactionNews.named(target),
            FactionCourt.courtierName(level, muster.faction, FactionCourt.Role.MARSHAL),
            makeup(campaign)
         )
      );
   }

   /** "a third archers, a tenth on horse, in mail" */
   static Component makeup(FactionWorldData.Campaign campaign) {
      return Component.translatable(
         "host.ancientwarfare.makeup", campaign.archers, campaign.horse, Component.translatable("host.ancientwarfare.arms." + campaign.arms)
      );
   }

   /** The host on the road eats from the granaries at home; with none left it starves and soldiers desert. */
   private static void feed(ServerLevel level, FactionWorldData data, FactionWorldData.Campaign campaign) {
      int[] stores = data.stores(campaign.faction());
      int need = Math.max(1, campaign.size / RATIONS);
      if (stores[FactionResources.GRAIN] >= need) {
         stores[FactionResources.GRAIN] -= need;
         campaign.hungry = false;
         return;
      }

      stores[FactionResources.GRAIN] = 0;
      if (!campaign.hungry) {
         FactionNews.tell(level, Component.translatable("news.ancientwarfare.host_hungry", FactionNews.factionName(campaign.faction())));
      }

      campaign.hungry = true;
      campaign.size -= Math.max(1, campaign.size / 15);
      data.setDirty();
   }

   /**
    * How hard the army hits, times its numbers: armour, archers before walls, riders in the field,
    * hunger, and the marshal's hand. Armies not mustered fight as they are.
    */
   static double might(ServerLevel level, FactionWorldData.Campaign campaign, boolean field) {
      if (!campaign.led) {
         return 1.0;
      }

      double might = 1.0 + 0.12 * campaign.arms + (field ? campaign.horse * 0.008 : campaign.archers * 0.006);
      might *= campaign.hungry ? 0.75 : 1.0;
      might *= has(level, campaign.faction(), FactionPeople.Character.AMBITIOUS) ? 1.1 : 1.0;
      might *= has(level, campaign.faction(), FactionPeople.Character.DRUNKARD) ? 0.85 : 1.0;
      might *= !field && has(level, campaign.faction(), FactionPeople.Character.CRUEL) ? 1.1 : 1.0;
      return might;
   }

   /** Times the usual marching pace. */
   static double pace(ServerLevel level, FactionWorldData.Campaign campaign) {
      if (!campaign.led) {
         return 1.0;
      }

      double pace = FactionCourt.marshalTrait(level, campaign.faction()) == FactionRulers.Trait.WARLIKE ? 1.2 : 1.0;
      pace *= has(level, campaign.faction(), FactionPeople.Character.DRUNKARD) ? 0.8 : 1.0;
      pace *= campaign.horse >= 20 ? 1.1 : 1.0;
      return pace;
   }

   /** Whether the marshal is one to turn away from a fight he thinks he would lose. */
   private static boolean careful(ServerLevel level, FactionWorldData.Campaign campaign) {
      return campaign.led
         && !has(level, campaign.faction(), FactionPeople.Character.AMBITIOUS)
         && (has(level, campaign.faction(), FactionPeople.Character.CONTENT) || FactionCourt.marshalTrait(level, campaign.faction()) == FactionRulers.Trait.CAUTIOUS);
   }

   /** A careful marshal facing a much stronger army falls back without a battle. */
   static boolean avoidsBattle(ServerLevel level, FactionWorldData.Campaign campaign, FactionWorldData.Campaign enemy) {
      if (!careful(level, campaign) || campaign.size * might(level, campaign, true) >= enemy.size * might(level, enemy, true) * 0.75) {
         return false;
      }

      FactionNews.tell(
         level,
         Component.translatable(
            "news.ancientwarfare.host_avoids", FactionCourt.courtierName(level, campaign.faction(), FactionCourt.Role.MARSHAL), FactionNews.factionName(campaign.faction()), FactionNews.factionName(enemy.faction())
         )
      );
      return true;
   }

   /** A careful marshal before walls he cannot hope to take turns for home. */
   static boolean shunsWalls(ServerLevel level, FactionWorldData.Campaign campaign, FactionWorldData.Settlement target, double walls) {
      if (!careful(level, campaign) || target.garrison * walls <= campaign.size * might(level, campaign, false) / 1.3) {
         return false;
      }

      FactionNews.tell(
         level,
         Component.translatable(
            "news.ancientwarfare.host_shuns", FactionCourt.courtierName(level, campaign.faction(), FactionCourt.Role.MARSHAL), FactionNews.factionName(campaign.faction()), FactionNews.named(target)
         )
      );
      return true;
   }

   /** The host lost a battle: its marshal may fall with it. */
   static void defeated(ServerLevel level, FactionWorldData data, FactionWorldData.Campaign campaign) {
      if (!campaign.led || level.getRandom().nextDouble() >= MARSHAL_FALLS) {
         return;
      }

      FactionNews.tell(
         level,
         Component.translatable("news.ancientwarfare.marshal_fell", FactionCourt.courtierName(level, campaign.faction(), FactionCourt.Role.MARSHAL), FactionNews.factionName(campaign.faction()))
      );
      FactionCourt.replace(level, campaign.faction(), FactionCourt.Role.MARSHAL, Math.max(0L, data.lastSimulatedDay()));
   }

   /** The host took the town: a cruel marshal sacks it, a kind one spares it. */
   static void took(ServerLevel level, FactionWorldData data, FactionWorldData.Campaign campaign, FactionWorldData.Settlement town) {
      if (!campaign.led) {
         return;
      }

      Component marshal = FactionCourt.courtierName(level, campaign.faction(), FactionCourt.Role.MARSHAL);
      if (has(level, campaign.faction(), FactionPeople.Character.CRUEL)) {
         int loot = Math.min(SACK_GOLD, Math.max(0, data.treasury(campaign.defender())));
         data.setTreasury(campaign.defender(), data.treasury(campaign.defender()) - loot);
         data.setTreasury(campaign.faction(), data.treasury(campaign.faction()) + loot);
         town.loyalty = 0;
         town.order = 10;
         FactionDiplomacy.adjustOpinion(level, campaign.defender(), campaign.faction(), -5);
         FactionNews.tell(level, Component.translatable("news.ancientwarfare.town_sacked", marshal, FactionNews.named(town)));
      } else if (has(level, campaign.faction(), FactionPeople.Character.KIND)) {
         town.loyalty = 60;
         town.order = 60;
         FactionNews.tell(level, Component.translatable("news.ancientwarfare.town_spared", marshal, FactionNews.named(town)));
      }

      data.setDirty();
   }

   /** The overview's lines: a host gathering, a host on the road. */
   static List<Component> lines(ServerLevel level, String faction) {
      List<Component> lines = new ArrayList<>();
      FactionWorldData data = FactionWorldData.get(level);
      for (Muster muster : get(level).musters) {
         if (muster.faction.equals(faction)) {
            lines.add(Component.translatable("host.ancientwarfare.line.muster", FactionNews.factionName(muster.defender), muster.gathered, muster.want).withStyle(ChatFormatting.GOLD));
         }
      }

      for (FactionWorldData.Campaign campaign : data.campaigns()) {
         if (campaign.led && campaign.faction().equals(faction)) {
            FactionWorldData.Settlement target = data.settlement(campaign.targetSettlement());
            lines.add(
               Component.translatable(
                     campaign.hungry ? "host.ancientwarfare.line.hungry" : "host.ancientwarfare.line.march",
                     target == null ? FactionNews.factionName(campaign.defender()) : FactionNews.named(target),
                     campaign.size,
                     makeup(campaign)
                  )
                  .withStyle(campaign.hungry ? ChatFormatting.RED : ChatFormatting.GOLD)
            );
         }
      }

      if (lines.size() <= 1) {
         return lines;
      }

      // Several at once would crowd the overview: one line sums them up.
      int gathering = 0;
      int marching = 0;
      int soldiers = 0;
      for (Muster muster : get(level).musters) {
         gathering += muster.faction.equals(faction) ? 1 : 0;
      }

      for (FactionWorldData.Campaign campaign : data.campaigns()) {
         if (campaign.led && campaign.faction().equals(faction)) {
            marching++;
            soldiers += campaign.size;
         }
      }

      return List.of(Component.translatable("host.ancientwarfare.line.many", marching, soldiers, gathering).withStyle(ChatFormatting.GOLD));
   }

   /** Admin: "<defender>" musters against it, "go" sends the faction's musters out now, "list" shows them. */
   public static String debug(ServerLevel level, String faction, String what) {
      FactionWorldData data = FactionWorldData.get(level);
      FactionMusters musters = get(level);
      if (what.equals("go")) {
         int sent = 0;
         for (Muster muster : new ArrayList<>(musters.musters)) {
            if (muster.faction.equals(faction)) {
               musters.musters.remove(muster);
               muster.gathered += FactionAI.takeLosses(data, faction, muster.want - muster.gathered);
               FactionWorldData.Settlement rally = data.settlement(muster.rally);
               FactionWorldData.Settlement target = data.settlement(muster.target);
               if (rally != null && target != null) {
                  setOut(level, data, muster, rally, target);
                  sent++;
               }
            }
         }

         musters.setDirty();
         return "Sent " + sent;
      }

      if (what.equals("meet") || what.equals("arrive")) {
         // Moves the faction's first host next to an enemy host, or to its target's gate.
         for (FactionWorldData.Campaign campaign : data.campaigns()) {
            if (!campaign.led || !campaign.faction().equals(faction)) {
               continue;
            }

            if (what.equals("arrive")) {
               FactionWorldData.Settlement target = data.settlement(campaign.targetSettlement());
               campaign.x = target.center().getX() + 10;
               campaign.z = target.center().getZ() + 10;
               return "Host #" + campaign.id() + " " + campaign.size + " at #" + target.id() + " garrison " + target.garrison + " walls " + FactionAI.walls(target);
            }

            for (FactionWorldData.Campaign enemy : data.campaigns()) {
               if (!enemy.faction().equals(faction) && FactionDiplomacy.relation(level, faction, enemy.faction()).state() == RelationState.WAR) {
                  campaign.x = enemy.x + 10;
                  campaign.z = enemy.z;
                  return "Host #" + campaign.id() + " " + campaign.size + " meets #" + enemy.id() + " " + enemy.faction() + " " + enemy.size;
               }
            }
         }

         return "Nothing";
      }

      if (what.equals("list")) {
         StringBuilder out = new StringBuilder("Musters:");
         for (Muster muster : musters.musters) {
            out.append(' ').append(muster.faction).append("->").append(muster.defender).append(' ').append(muster.gathered).append('/').append(muster.want).append(" ready ").append(muster.ready);
         }

         out.append(" Hosts:");
         for (FactionWorldData.Campaign campaign : data.campaigns()) {
            if (campaign.led) {
               out.append(" #").append(campaign.id()).append(' ').append(campaign.faction()).append("->").append(campaign.defender()).append(' ').append(campaign.size)
                  .append(" a").append(campaign.archers).append(" h").append(campaign.horse).append(" m").append(campaign.arms).append(campaign.hungry ? " hungry" : "")
                  .append(" @").append(Mth.floor(campaign.x())).append(',').append(Mth.floor(campaign.z()));
            }
         }

         return out.toString();
      }

      if (FactionDiplomacy.relation(level, faction, what).state() != RelationState.WAR) {
         FactionDiplomacy.setState(level, faction, what, RelationState.WAR);
      }

      FactionWorldData.Settlement target = FactionAI.borderSettlement(data, faction, what, data.lastSimulatedDay());
      if (target == null) {
         return "No target";
      }

      return muster(level, data, faction, target, 30) ? "Mustering against #" + target.id() : "No town to muster at";
   }

   private static FactionMusters load(CompoundTag tag) {
      FactionMusters data = new FactionMusters();
      for (Tag element : tag.getList("Musters", Tag.TAG_COMPOUND)) {
         CompoundTag entry = (CompoundTag)element;
         data.musters.add(
            new Muster(
               entry.getString("Faction"),
               entry.getString("Defender"),
               entry.getInt("Target"),
               entry.getInt("Rally"),
               entry.getInt("Want"),
               entry.getInt("Gathered"),
               entry.getLong("Ready")
            )
         );
      }

      return data;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      ListTag list = new ListTag();
      for (Muster muster : this.musters) {
         CompoundTag entry = new CompoundTag();
         entry.putString("Faction", muster.faction);
         entry.putString("Defender", muster.defender);
         entry.putInt("Target", muster.target);
         entry.putInt("Rally", muster.rally);
         entry.putInt("Want", muster.want);
         entry.putInt("Gathered", muster.gathered);
         entry.putLong("Ready", muster.ready);
         list.add(entry);
      }

      tag.put("Musters", list);
      return tag;
   }
}
