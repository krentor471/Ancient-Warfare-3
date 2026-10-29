package net.shadowmage.ancientwarfare.core.faction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.shadowmage.ancientwarfare.core.common.npc.FactionStandingData;

/**
 * Each ruler's household: a wife, from the nobles of his own land or a daughter of another ruler,
 * and the children she bears him. A son who comes of age is named heir. A player may marry a ruler's
 * daughter through a deal; her father's faction is then kin to the player and slow to make war on him.
 * When the ruler changes, his widow and children leave the throne's household with him.
 */
public final class FactionFamilies extends SavedData {
   private static final String DATA_NAME = "ancientwarfare_faction_families";
   /** How many wives' and daughters' names the lang files hold: family.ancientwarfare.name.0 and on. */
   public static final int FEMALE_NAMES = 24;
   private static final int MARRY_AGE = 16;
   private static final int OLDEST_GROOM = 60;
   private static final int OLDEST_MOTHER = 44;
   private static final int MAX_CHILDREN = 6;
   private static final double MARRY_CHANCE = 0.05;
   /** The chance a ruler who marries takes a daughter of another ruler rather than a noblewoman. */
   private static final double FOREIGN_CHANCE = 0.5;
   private static final double BIRTH_CHANCE = 0.12;
   private static final double CHILD_DEATH = 0.004;
   private static final int WEDDING_OPINION = 20;
   private static final int WEDDING_STANDING = 25;
   /** Kin's good will grows a little each day, up to this standing. */
   private static final int KIN_STANDING = 30;
   private static final int BROKEN_STANDING = -30;
   /** How much less often kin raise an army against the player. */
   public static final double KIN_PROTECTION = 0.1;
   private static final int BASE_PRICE = 250;
   private static final int SETTLEMENT_PRICE = 6;

   static final class Person {
      boolean female;
      int name;
      int look;
      long born;
      int character;
      /** The faction she was born to, for a wife; empty for a noblewoman of the ruler's own land. */
      String house = "";
      /** The faction a daughter was married into; empty while unwed. */
      String wed = "";

      int age(long day) {
         return FactionPeople.age(this.born, day);
      }

      Component name() {
         return this.female ? femaleName(this.name) : FactionRulers.name(this.name);
      }

      CompoundTag save() {
         CompoundTag tag = new CompoundTag();
         tag.putBoolean("Female", this.female);
         tag.putInt("Name", this.name);
         tag.putInt("Look", this.look);
         tag.putLong("Born", this.born);
         tag.putInt("Character", this.character);
         tag.putString("House", this.house);
         tag.putString("Wed", this.wed);
         return tag;
      }

      static Person load(CompoundTag tag) {
         Person person = new Person();
         person.female = tag.getBoolean("Female");
         person.name = tag.getInt("Name");
         person.look = tag.getInt("Look");
         person.born = tag.getLong("Born");
         person.character = tag.contains("Character") ? tag.getInt("Character") : FactionPeople.oldCharacter(person.look);
         person.house = tag.getString("House");
         person.wed = tag.getString("Wed");
         return person;
      }
   }

   static final class Family {
      /** The ruler this household belongs to, by name and day of birth. */
      String ruler = "";
      Person wife;
      final List<Person> children = new ArrayList<>();
      /** The ruler's brothers and sisters, when he took the throne from their father. */
      final List<Person> kin = new ArrayList<>();
   }

   /** A player's wife and the faction she came from. */
   private record Bride(String faction, Person person) {
   }

   private final Map<String, Family> families = new HashMap<>();
   private final Map<UUID, Bride> brides = new HashMap<>();
   /** Each player's children by his wife; they stay his after she is gone. */
   private final Map<UUID, List<Person>> playerChildren = new HashMap<>();
   private static final int MAX_PLAYER_CHILDREN = 4;
   private static final double PLAYER_BIRTH_CHANCE = 0.08;
   private static final int DAUGHTER_STANDING = 15;
   /** What a ruler gives for a player's daughter's hand, in silver. */
   private static final int DAUGHTER_WORTH = 300;
   private static final String PLAYER_HOUSE = "player:";
   public static final String SON_TAG = "aw_son";

   public static FactionFamilies get(ServerLevel level) {
      return level.getServer().overworld().getDataStorage().computeIfAbsent(FactionFamilies::load, FactionFamilies::new, DATA_NAME);
   }

   public static Component femaleName(int name) {
      return Component.translatable("family.ancientwarfare.name." + Math.floorMod(name, FEMALE_NAMES));
   }

   private static String key(FactionWorldData.Ruler ruler) {
      return FactionRulers.key(ruler);
   }

   /** The household of the faction's present ruler; a new ruler starts with an empty one. */
   private Family family(ServerLevel level, String faction) {
      FactionWorldData.Ruler ruler = FactionRulers.ruler(FactionWorldData.get(level), faction, level.getRandom());
      Family family = this.families.computeIfAbsent(faction, ignored -> new Family());
      if (!family.ruler.equals(key(ruler))) {
         // A son who inherits keeps his brothers and sisters about him; anyone else comes to the throne alone.
         Person self = family.children.stream().filter(child -> !child.female && child.name == ruler.name() && child.born == ruler.born()).findFirst().orElse(null);
         family.kin.clear();
         if (self != null) {
            family.children.remove(self);
            family.kin.addAll(family.children);
         }

         family.ruler = key(ruler);
         family.wife = null;
         family.children.clear();
         this.setDirty();
      }

      return family;
   }

   private static Person newPerson(boolean female, RandomSource random, long day, int age) {
      Person person = new Person();
      person.female = female;
      person.name = random.nextInt(female ? FEMALE_NAMES : FactionRulers.NAMES);
      person.look = FactionPeople.newLook(random);
      person.born = FactionPeople.bornAt(day, age, random);
      person.character = FactionPeople.newCharacter(random);
      return person;
   }

   private static boolean bride(Person person, long day) {
      return person.female && person.wed.isEmpty() && person.age(day) >= MARRY_AGE;
   }

   /** Daily: rulers wed, children are born, grow up and die, and the old pass away. */
   static void simulateDay(ServerLevel level, FactionWorldData data) {
      FactionFamilies families = get(level);
      long day = Math.max(0L, data.lastSimulatedDay());
      RandomSource random = level.getRandom();
      Set<String> factions = new TreeSet<>();
      for (FactionWorldData.Settlement settlement : data.settlements()) {
         if (!FactionDiplomacy.isOutlaw(settlement.faction())) {
            factions.add(settlement.faction());
         }
      }

      families.families.keySet().retainAll(factions);
      for (String faction : factions) {
         Family family = families.family(level, faction);
         FactionWorldData.Ruler ruler = FactionRulers.ruler(data, faction, random);
         int age = FactionPeople.age(ruler.born(), day);
         if (family.wife != null && random.nextDouble() < FactionPeople.oldAgeDeath(family.wife.age(day))) {
            FactionNews.tell(level, Component.translatable("news.ancientwarfare.wife_died", FactionNews.factionName(faction), family.wife.name(), FactionRulers.name(ruler.name())));
            family.wife = null;
         }

         if (family.wife == null && age >= MARRY_AGE && age <= OLDEST_GROOM && random.nextDouble() < MARRY_CHANCE) {
            families.wed(level, faction, family, ruler, day);
         }

         if (family.wife != null && family.wife.age(day) <= OLDEST_MOTHER && family.children.size() < MAX_CHILDREN && random.nextDouble() < BIRTH_CHANCE) {
            Person child = newPerson(random.nextBoolean(), random, day, 0);
            child.born = day;
            family.children.add(child);
            FactionNews.tell(
               level,
               Component.translatable(child.female ? "news.ancientwarfare.daughter_born" : "news.ancientwarfare.son_born", FactionNews.factionName(faction), FactionRulers.name(ruler.name()), child.name())
            );
         }

         boolean[] heirLost = new boolean[1];
         family.children.removeIf(child -> {
            int childAge = child.age(day);
            if (random.nextDouble() < (childAge < 5 ? CHILD_DEATH : FactionPeople.oldAgeDeath(childAge))) {
               heirLost[0] |= FactionCourt.isHeir(level, faction, child.name, child.born);
               FactionNews.tell(level, Component.translatable("news.ancientwarfare.child_died", FactionNews.factionName(faction), child.name(), childAge));
               return true;
            }

            return false;
         });
         family.kin.removeIf(sibling -> {
            int siblingAge = sibling.age(day);
            if (random.nextDouble() < FactionPeople.oldAgeDeath(siblingAge)) {
               heirLost[0] |= FactionCourt.isHeir(level, faction, sibling.name, sibling.born);
               FactionNews.tell(
                  level,
                  Component.translatable(sibling.female ? "news.ancientwarfare.sister_died" : "news.ancientwarfare.brother_died", FactionNews.factionName(faction), sibling.name(), FactionRulers.name(ruler.name()), siblingAge)
               );
               return true;
            }

            return false;
         });
         families.chooseHeir(level, faction, family, ruler, day, heirLost[0]);
      }

      families.brides.entrySet().removeIf(entry -> {
         Bride bride = entry.getValue();
         if (random.nextDouble() < FactionPeople.oldAgeDeath(bride.person.age(day))) {
            FactionNews.tellPlayer(level, entry.getKey(), Component.translatable("family.ancientwarfare.bride_died", bride.person.name(), FactionNews.factionName(bride.faction)).withStyle(ChatFormatting.GRAY));
            return true;
         }

         FactionStandingData standings = FactionStandingData.get(level);
         if (standings.getStanding(entry.getKey(), bride.faction) < KIN_STANDING) {
            standings.adjustStanding(entry.getKey(), bride.faction, 1);
         }

         List<Person> children = families.playerChildren.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>());
         if (bride.person.age(day) <= OLDEST_MOTHER && children.size() < MAX_PLAYER_CHILDREN && random.nextDouble() < PLAYER_BIRTH_CHANCE) {
            Person child = newPerson(random.nextBoolean(), random, day, 0);
            child.born = day;
            children.add(child);
            FactionNews.tellPlayer(
               level, entry.getKey(), Component.translatable(child.female ? "family.ancientwarfare.your_daughter" : "family.ancientwarfare.your_son", bride.person.name(), child.name()).withStyle(ChatFormatting.LIGHT_PURPLE)
            );
         }

         return false;
      });
      families.growChildren(level, day);
      families.setDirty();
   }

   /**
    * The ruler's eldest son is his heir, however young; with no son, his eldest brother. Only a ruler
    * with neither names someone from court, and a dead heir is followed by the next in line.
    */
   private void chooseHeir(ServerLevel level, String faction, Family family, FactionWorldData.Ruler ruler, long day, boolean heirLost) {
      Comparator<Person> eldest = Comparator.comparingLong(person -> person.born);
      Person son = family.children.stream().filter(child -> !child.female).min(eldest).orElse(null);
      Person next = son != null ? son : family.kin.stream().filter(sibling -> !sibling.female).min(eldest).orElse(null);
      if (next == null) {
         if (heirLost) {
            FactionCourt.replace(level, faction, FactionCourt.Role.HEIR, day);
         }

         return;
      }

      if (FactionCourt.isHeir(level, faction, next.name, next.born)) {
         return;
      }

      RandomSource random = level.getRandom();
      // The heir's disposition is one his character allows.
      FactionRulers.Trait[] dispositions = FactionRulers.Trait.values();
      int disposition = random.nextInt(dispositions.length);
      for (int i = 0; i < dispositions.length && FactionPeople.consistent(next.character, dispositions[disposition]) != next.character; i++) {
         disposition = (disposition + 1) % dispositions.length;
      }

      FactionCourt.nameHeir(level, faction, next.name, disposition, next.look, next.born, FactionPeople.consistent(next.character, dispositions[disposition]));
      if (next.born != day) {
         // A son born today is heir from his first breath; the birth is news enough.
         FactionNews.tell(
            level,
            Component.translatable(next == son ? "news.ancientwarfare.son_heir" : "news.ancientwarfare.brother_heir", FactionNews.factionName(faction), next.name(), FactionRulers.name(ruler.name()))
         );
      }
   }

   /** The player's children grow up: a son of age comes to serve at his father's side. */
   private void growChildren(ServerLevel level, long day) {
      RandomSource random = level.getRandom();
      this.playerChildren.forEach((player, children) -> children.removeIf(child -> {
         int age = child.age(day);
         if (random.nextDouble() < (age < 5 ? CHILD_DEATH : FactionPeople.oldAgeDeath(age))) {
            FactionNews.tellPlayer(level, player, Component.translatable("family.ancientwarfare.your_child_died", child.name(), age).withStyle(ChatFormatting.GRAY));
            return true;
         }

         ServerPlayer online = level.getServer().getPlayerList().getPlayer(player);
         if (!child.female && age >= MARRY_AGE && online != null && online.serverLevel() == level) {
            net.shadowmage.ancientwarfare.core.entity.CombatNpcEntity son = net.shadowmage.ancientwarfare.core.forge.init.AWCoreEntityTypes.COMBAT_NPC.get().create(level);
            if (son == null) {
               return false;
            }

            son.moveTo(online.getX(), online.getY(), online.getZ(), online.getYRot(), 0.0F);
            son.setOwner(player);
            son.setCustomName(Component.translatable("family.ancientwarfare.son_name", child.name(), online.getDisplayName()));
            son.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
            son.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHAINMAIL_CHESTPLATE));
            net.minecraft.world.entity.ai.attributes.AttributeInstance health = son.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            if (health != null) {
               health.setBaseValue(health.getBaseValue() * 2.0);
               son.setHealth(son.getMaxHealth());
            }

            son.addTag(SON_TAG);
            son.setPersistenceRequired();
            level.addFreshEntity(son);
            online.sendSystemMessage(Component.translatable("family.ancientwarfare.son_arrived", child.name()).withStyle(ChatFormatting.LIGHT_PURPLE));
            return true;
         }

         return false;
      }));
   }

   /** The ruler takes a wife: a daughter of a friendly ruler when one is free, else a noblewoman. */
   private void wed(ServerLevel level, String faction, Family family, FactionWorldData.Ruler ruler, long day) {
      RandomSource random = level.getRandom();
      if (random.nextDouble() < FOREIGN_CHANCE) {
         List<String> others = new ArrayList<>(this.families.keySet());
         others.remove(faction);
         java.util.Collections.shuffle(others, new java.util.Random(random.nextLong()));
         for (String other : others) {
            if (FactionDiplomacy.relation(level, faction, other).state() == RelationState.WAR) {
               continue;
            }

            Family theirs = this.family(level, other);
            Person daughter = theirs.children.stream().filter(child -> bride(child, day)).findFirst().orElse(null);
            if (daughter != null) {
               theirs.children.remove(daughter);
               daughter.house = other;
               family.wife = daughter;
               FactionDiplomacy.adjustOpinion(level, faction, other, WEDDING_OPINION);
               FactionDiplomacy.adjustOpinion(level, other, faction, WEDDING_OPINION);
               FactionNews.tell(
                  level,
                  Component.translatable(
                     "news.ancientwarfare.royal_wedding", FactionNews.factionName(faction), FactionRulers.name(ruler.name()), daughter.name(), FactionNews.factionName(other)
                  )
               );
               return;
            }
         }
      }

      family.wife = newPerson(true, random, day, Math.max(MARRY_AGE, FactionPeople.age(ruler.born(), day) - 3 - random.nextInt(10)));
      FactionNews.tell(level, Component.translatable("news.ancientwarfare.wedding", FactionNews.factionName(faction), FactionRulers.name(ruler.name()), family.wife.name()));
   }

   // ---------------------------------------------------------------- players

   /** The faction whose daughter the player married, or null. */
   public static String kin(ServerLevel level, UUID player) {
      Bride bride = get(level).brides.get(player);
      return bride == null ? null : bride.faction;
   }

   /** Kin: the faction of the player's wife, or one whose ruler took the player's daughter to wife. */
   public static boolean isKin(ServerLevel level, UUID player, String faction) {
      if (faction.equals(kin(level, player))) {
         return true;
      }

      Family family = get(level).families.get(faction);
      return family != null && family.wife != null && family.wife.house.startsWith(PLAYER_HOUSE + player);
   }

   /** Why the ruler will not take the player's daughter to wife, or null when he will. */
   static String cannotGiveDaughter(ServerLevel level, UUID player, String faction) {
      FactionFamilies families = get(level);
      long day = Math.max(0L, FactionWorldData.get(level).lastSimulatedDay());
      if (families.playerChildren.getOrDefault(player, List.of()).stream().noneMatch(child -> bride(child, day))) {
         return "deal.ancientwarfare.no_daughter_yours";
      }

      FactionWorldData.Ruler ruler = FactionRulers.ruler(FactionWorldData.get(level), faction, level.getRandom());
      return families.family(level, faction).wife != null || FactionPeople.age(ruler.born(), day) < MARRY_AGE ? "deal.ancientwarfare.ruler_married" : null;
   }

   static double daughterWorth() {
      return DAUGHTER_WORTH;
   }

   /** The deal is struck: the player's eldest unwed daughter is the ruler's wife. */
   static void giveDaughter(ServerLevel level, ServerPlayer player, String faction) {
      FactionFamilies families = get(level);
      long day = Math.max(0L, FactionWorldData.get(level).lastSimulatedDay());
      List<Person> children = families.playerChildren.getOrDefault(player.getUUID(), new ArrayList<>());
      Person daughter = children.stream().filter(child -> bride(child, day)).min(Comparator.comparingLong(child -> child.born)).orElse(null);
      if (daughter == null) {
         return;
      }

      children.remove(daughter);
      daughter.house = PLAYER_HOUSE + player.getUUID() + ":" + player.getGameProfile().getName();
      families.family(level, faction).wife = daughter;
      FactionStandingData.get(level).adjustStanding(player.getUUID(), faction, DAUGHTER_STANDING);
      FactionNews.tell(
         level,
         Component.translatable(
            "news.ancientwarfare.daughter_wedding", player.getDisplayName(), daughter.name(), FactionRulers.name(FactionRulers.ruler(FactionWorldData.get(level), faction, level.getRandom()).name()), FactionNews.factionName(faction)
         )
      );
      families.setDirty();
   }

   /** The player's own household for the chat: wife, children, and daughters given away. */
   public static List<Component> describe(ServerLevel level, UUID player) {
      FactionFamilies families = get(level);
      long day = Math.max(0L, FactionWorldData.get(level).lastSimulatedDay());
      List<Component> lines = new ArrayList<>();
      Bride bride = families.brides.get(player);
      lines.add(
         bride == null
            ? Component.translatable("family.ancientwarfare.no_wife")
            : Component.translatable("family.ancientwarfare.your_wife", bride.person.name(), bride.person.age(day), FactionNews.factionName(bride.faction))
      );
      for (Person child : families.playerChildren.getOrDefault(player, List.of())) {
         lines.add(Component.translatable(child.female ? "family.ancientwarfare.tip.daughter" : "family.ancientwarfare.tip.son", child.name(), child.age(day)));
      }

      families.families.forEach((faction, family) -> {
         if (family.wife != null && family.wife.house.startsWith(PLAYER_HOUSE + player)) {
            lines.add(Component.translatable("family.ancientwarfare.daughter_wed", family.wife.name(), FactionNews.factionName(faction)));
         }
      });
      return lines;
   }

   /** Why the player cannot marry into the faction, or null when he can. */
   static String cannotMarry(ServerLevel level, UUID player, String faction) {
      FactionFamilies families = get(level);
      if (families.brides.containsKey(player)) {
         return "deal.ancientwarfare.already_married";
      }

      long day = Math.max(0L, FactionWorldData.get(level).lastSimulatedDay());
      return families.family(level, faction).children.stream().anyMatch(child -> bride(child, day)) ? null : "deal.ancientwarfare.no_daughter";
   }

   /** What the ruler asks for his daughter's hand, in silver. */
   static double price(FactionWorldData data, String faction, int standing) {
      long settlements = data.settlements().stream().filter(settlement -> settlement.faction().equals(faction)).count();
      return BASE_PRICE + settlements * SETTLEMENT_PRICE - Math.max(0, standing) * 3;
   }

   /** The deal is struck: the eldest unwed daughter is the player's wife. */
   static void marry(ServerLevel level, ServerPlayer player, String faction) {
      FactionFamilies families = get(level);
      long day = Math.max(0L, FactionWorldData.get(level).lastSimulatedDay());
      Family family = families.family(level, faction);
      Person daughter = family.children.stream().filter(child -> bride(child, day)).min(Comparator.comparingLong(child -> child.born)).orElse(null);
      if (daughter == null) {
         return;
      }

      family.children.remove(daughter);
      daughter.house = faction;
      families.brides.put(player.getUUID(), new Bride(faction, daughter));
      FactionStandingData.get(level).adjustStanding(player.getUUID(), faction, WEDDING_STANDING);
      FactionNews.tell(
         level,
         Component.translatable(
            "news.ancientwarfare.player_wedding", player.getDisplayName(), daughter.name(), FactionRulers.name(FactionRulers.ruler(FactionWorldData.get(level), faction, level.getRandom()).name()), FactionNews.factionName(faction)
         )
      );
      families.setDirty();
   }

   /** The player made war on his wife's kin: she goes home, and they will not forget it. */
   public static void warDeclared(ServerLevel level, UUID player, String faction) {
      FactionFamilies families = get(level);
      Bride bride = families.brides.get(player);
      if (bride == null || !bride.faction.equals(faction)) {
         return;
      }

      families.brides.remove(player);
      FactionStandingData.get(level).adjustStanding(player, faction, BROKEN_STANDING);
      FactionNews.tellPlayer(level, player, Component.translatable("family.ancientwarfare.bride_left", bride.person.name(), FactionNews.factionName(faction)).withStyle(ChatFormatting.RED));
      families.setDirty();
   }

   /** The line in the overview: the player's wife from this faction. */
   static Component line(ServerLevel level, UUID player, String faction) {
      Bride bride = get(level).brides.get(player);
      if (bride == null || !bride.faction.equals(faction)) {
         return null;
      }

      long day = Math.max(0L, FactionWorldData.get(level).lastSimulatedDay());
      return Component.translatable("family.ancientwarfare.line", bride.person.name(), bride.person.age(day)).withStyle(ChatFormatting.LIGHT_PURPLE);
   }

   /** For the diplomacy screen: for the wife and then each child, whether female, looks and age. */
   static int[] view(ServerLevel level, String faction) {
      Family family = get(level).family(level, faction);
      long day = Math.max(0L, FactionWorldData.get(level).lastSimulatedDay());
      List<Person> people = new ArrayList<>();
      if (family.wife != null) {
         people.add(family.wife);
      }

      people.addAll(family.children);
      int[] view = new int[1 + people.size() * 3];
      view[0] = family.wife != null ? 1 : 0;
      for (int i = 0; i < people.size(); i++) {
         Person person = people.get(i);
         view[1 + i * 3] = person.female ? 1 : 0;
         view[2 + i * 3] = person.look;
         view[3 + i * 3] = person.age(day);
      }

      return view;
   }

   /** The names that go with view, in the same order, for the portraits' tooltips. */
   static List<Component> tips(ServerLevel level, String faction) {
      Family family = get(level).family(level, faction);
      long day = Math.max(0L, FactionWorldData.get(level).lastSimulatedDay());
      List<Component> tips = new ArrayList<>();
      if (family.wife != null) {
         tips.add(
            family.wife.house.isEmpty()
               ? Component.translatable("family.ancientwarfare.tip.wife", family.wife.name(), family.wife.age(day))
               : family.wife.house.startsWith(PLAYER_HOUSE)
                  ? Component.translatable("family.ancientwarfare.tip.wife_player", family.wife.name(), family.wife.age(day), family.wife.house.substring(family.wife.house.lastIndexOf(':') + 1))
                  : Component.translatable("family.ancientwarfare.tip.wife_from", family.wife.name(), family.wife.age(day), FactionNews.factionName(family.wife.house))
         );
      }

      for (Person child : family.children) {
         String kind = child.female ? "daughter" : FactionCourt.isHeir(level, faction, child.name, child.born) ? "son_heir" : "son";
         tips.add(Component.translatable("family.ancientwarfare.tip." + kind, child.name(), child.age(day)));
      }

      return tips;
   }

   /** For testing: wed the ruler at once, or give him a child of the given age. */
   public static String debug(ServerLevel level, String faction, String what, int value) {
      FactionFamilies families = get(level);
      long day = Math.max(0L, FactionWorldData.get(level).lastSimulatedDay());
      Family family = families.family(level, faction);
      FactionWorldData.Ruler ruler = FactionRulers.ruler(FactionWorldData.get(level), faction, level.getRandom());
      switch (what) {
         case "wed" -> families.wed(level, faction, family, ruler, day);
         case "son", "daughter" -> family.children.add(newPerson(what.equals("daughter"), level.getRandom(), day, value));
         case "myson", "mydaughter" -> level.players().forEach(player -> families.playerChildren.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>())
            .add(newPerson(what.equals("mydaughter"), level.getRandom(), day, value)));
         case "day" -> {
            for (int i = 0; i < Math.max(1, value); i++) {
               simulateDay(level, FactionWorldData.get(level));
            }
         }
         default -> {
            return "wed|son N|daughter N|myson N|mydaughter N|day N";
         }
      }

      families.setDirty();
      return "wife " + (family.wife != null) + ", children " + family.children.size();
   }

   private static FactionFamilies load(CompoundTag tag) {
      FactionFamilies data = new FactionFamilies();
      CompoundTag families = tag.getCompound("Families");
      for (String faction : families.getAllKeys()) {
         CompoundTag entry = families.getCompound(faction);
         Family family = new Family();
         family.ruler = entry.getString("Ruler");
         if (entry.contains("Wife")) {
            family.wife = Person.load(entry.getCompound("Wife"));
         }

         for (Tag child : entry.getList("Children", Tag.TAG_COMPOUND)) {
            family.children.add(Person.load((CompoundTag)child));
         }

         for (Tag sibling : entry.getList("Kin", Tag.TAG_COMPOUND)) {
            family.kin.add(Person.load((CompoundTag)sibling));
         }

         data.families.put(faction, family);
      }

      CompoundTag brides = tag.getCompound("Brides");
      for (String player : brides.getAllKeys()) {
         try {
            CompoundTag entry = brides.getCompound(player);
            data.brides.put(UUID.fromString(player), new Bride(entry.getString("Faction"), Person.load(entry.getCompound("Person"))));
         } catch (IllegalArgumentException ignored) {
         }
      }

      CompoundTag children = tag.getCompound("PlayerChildren");
      for (String player : children.getAllKeys()) {
         try {
            List<Person> list = new ArrayList<>();
            for (Tag child : children.getList(player, Tag.TAG_COMPOUND)) {
               list.add(Person.load((CompoundTag)child));
            }

            data.playerChildren.put(UUID.fromString(player), list);
         } catch (IllegalArgumentException ignored) {
         }
      }

      return data;
   }

   @Override
   public CompoundTag save(CompoundTag tag) {
      CompoundTag families = new CompoundTag();
      this.families.forEach((faction, family) -> {
         CompoundTag entry = new CompoundTag();
         entry.putString("Ruler", family.ruler);
         if (family.wife != null) {
            entry.put("Wife", family.wife.save());
         }

         ListTag children = new ListTag();
         family.children.forEach(child -> children.add(child.save()));
         entry.put("Children", children);
         ListTag kin = new ListTag();
         family.kin.forEach(sibling -> kin.add(sibling.save()));
         entry.put("Kin", kin);
         families.put(faction, entry);
      });
      tag.put("Families", families);
      CompoundTag brides = new CompoundTag();
      this.brides.forEach((player, bride) -> {
         CompoundTag entry = new CompoundTag();
         entry.putString("Faction", bride.faction);
         entry.put("Person", bride.person.save());
         brides.put(player.toString(), entry);
      });
      tag.put("Brides", brides);
      CompoundTag children = new CompoundTag();
      this.playerChildren.forEach((player, list) -> {
         ListTag entry = new ListTag();
         list.forEach(child -> entry.add(child.save()));
         children.put(player.toString(), entry);
      });
      tag.put("PlayerChildren", children);
      return tag;
   }
}
