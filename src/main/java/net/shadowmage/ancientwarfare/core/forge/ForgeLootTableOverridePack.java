package net.shadowmage.ancientwarfare.core.forge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.loading.FMLPaths;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;

@EventBusSubscriber(
   modid = "ancientwarfare",
   bus = Bus.MOD
)
public final class ForgeLootTableOverridePack {
   private static final String PACK_ID = "ancientwarfare_loot_overrides";
   private static final String CATALOG_RESOURCE = "/assets/ancientwarfare/loot_tables/chest_table_catalog.txt";
   private static final String TABLE_RESOURCE_ROOT = "/data/ancientwarfare/loot_tables/chests/";
   private static final Path ROOT = FMLPaths.CONFIGDIR.get().resolve("ancientwarfare-loot-tables");
   private static final Path DEFAULT_TABLES = ROOT.resolve("defaults/data/ancientwarfare/loot_tables/chests");
   private static final Path OVERRIDE_ROOT = ROOT.resolve("overrides");
   private static volatile ForgeLootOverrideSnapshot activeSnapshot = ForgeLootOverrideSnapshot.empty();

   private ForgeLootTableOverridePack() {
   }

   public static void initialize() {
      try {
         Files.createDirectories(DEFAULT_TABLES);
         Files.createDirectories(OVERRIDE_ROOT.resolve("data/ancientwarfare/loot_tables/chests"));
         writeText(
            OVERRIDE_ROOT.resolve("pack.mcmeta"),
            "{\n  \"pack\": {\n    \"pack_format\": 15,\n    \"description\": \"Ancient Warfare editable structure loot overrides\"\n  }\n}\n",
            false
         );
         writeText(ROOT.resolve("README-zh_cn.txt"), chineseInstructions(), true);
         writeText(ROOT.resolve("README-en_us.txt"), englishInstructions(), true);
         exportDefaults();
      } catch (IOException var1) {
         AncientWarfareCore.LOG.error("Unable to prepare editable Ancient Warfare loot tables at {}", ROOT, var1);
      }
   }

   @SubscribeEvent
   public static void addPackFinder(AddPackFindersEvent event) {
      if (event.getPackType() == PackType.SERVER_DATA) {
         initialize();

         try {
            activeSnapshot = ForgeLootOverrideSnapshot.read(OVERRIDE_ROOT);
         } catch (RuntimeException | IOException var2) {
            AncientWarfareCore.LOG.error("Unable to load loot override files; retaining the last working override snapshot", var2);
         }

         event.addRepositorySource(
            output -> {
               Pack pack = Pack.readMetaAndCreate(
                  "ancientwarfare_loot_overrides",
                  Component.literal("Ancient Warfare Loot Overrides"),
                  true,
                  id -> activeSnapshot.packResources(id),
                  PackType.SERVER_DATA,
                  Position.TOP,
                  PackSource.DEFAULT
               );
               if (pack != null) {
                  output.accept(pack);
               } else {
                  AncientWarfareCore.LOG.error("Unable to read Ancient Warfare loot override pack metadata at {}", OVERRIDE_ROOT);
               }
            }
         );
      }
   }

   static ForgeLootOverrideSnapshot activeSnapshot() {
      return activeSnapshot;
   }

   static void useSnapshot(ForgeLootOverrideSnapshot snapshot) {
      activeSnapshot = snapshot;
   }

   private static void exportDefaults() throws IOException {
      List<String> names = readCatalog();

      for (String name : names) {
         try (InputStream input = ForgeLootTableOverridePack.class.getResourceAsStream("/data/ancientwarfare/loot_tables/chests/" + name + ".json")) {
            if (input != null) {
               Files.copy(input, DEFAULT_TABLES.resolve(name + ".json"), StandardCopyOption.REPLACE_EXISTING);
            } else {
               AncientWarfareCore.LOG.warn("Built-in loot table listed in catalog is missing: {}", name);
            }
         }
      }

      writeText(ROOT.resolve("table-index.txt"), String.join(System.lineSeparator(), names) + System.lineSeparator(), true);
      AncientWarfareCore.LOG.info("Prepared {} editable Ancient Warfare loot-table references at {}", names.size(), ROOT);
   }

   private static List<String> readCatalog() throws IOException {
      Object var9;
      try (InputStream input = ForgeLootTableOverridePack.class.getResourceAsStream("/assets/ancientwarfare/loot_tables/chest_table_catalog.txt")) {
         if (input == null) {
            throw new IOException("Missing loot table catalog resource /assets/ancientwarfare/loot_tables/chest_table_catalog.txt");
         }

         List<String> names = new ArrayList<>();

         String line;
         try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            while ((line = reader.readLine()) != null) {
               String name = line.trim();
               if (!name.isEmpty() && !name.startsWith("#")) {
                  names.add(name);
               }
            }
         }

         var9 = names;
      }

      return (List<String>)var9;
   }

   private static void writeText(Path path, String contents, boolean replace) throws IOException {
      Files.createDirectories(path.getParent());
      if (replace || Files.notExists(path)) {
         Files.writeString(path, contents, StandardCharsets.UTF_8);
      }
   }

   private static String chineseInstructions() {
      return "============================================================\n古代战争2 重制版：建筑战利品配置详细说明\n适用于 Minecraft 1.20.1 / Ancient Warfare Remastered\n============================================================\n\n一、这套配置能修改什么？\n------------------------------------------------------------\n这套配置专门管理古代战争建筑中的箱子、战利品篮、骨灰瓮、棺材等容器。\n\n你可以进行两种程度的修改：\n\n1. 简单修改（推荐普通玩家使用）\n   使用 ancientwarfare-loot.toml 调整：\n   - 所有建筑战利品整体变多或变少；\n   - 每组物品的数量整体变多或变少；\n   - 完全关闭某一张战利品表；\n   - 单独提高或降低某个阵营、某类建筑的奖励；\n   - 是否避免同一个箱子刷出重复研究笔记；\n   - 是否允许冰与火、拔刀剑的额外对接奖励。\n\n2. 完整修改（适合整合包作者和服务器管理员）\n   使用 overrides 目录中的标准 Minecraft 战利品表 JSON 修改：\n   - 增加或删除物品；\n   - 修改物品权重和出现概率；\n   - 修改数量范围；\n   - 修改奖池抽取次数；\n   - 增加附魔、药水、钱币材质、研究笔记等特殊数据；\n   - 完全重写某个阵营或建筑类型的战利品。\n\n\n二、文件和目录在哪里？\n------------------------------------------------------------\n首次使用带有此功能的版本启动游戏或服务器后，会生成：\n\nconfig/\n├─ ancientwarfare-loot.toml\n└─ ancientwarfare-loot-tables/\n   ├─ README-zh_cn.txt          当前中文说明\n   ├─ README-en_us.txt          英文说明\n   ├─ table-index.txt           全部可覆盖战利品表名称\n   ├─ defaults/\n   │  └─ data/ancientwarfare/loot_tables/chests/\n   │     └─ 123 张内置战利品表参考副本\n   └─ overrides/\n      ├─ pack.mcmeta\n      └─ data/ancientwarfare/loot_tables/chests/\n         └─ 玩家实际启用的覆盖表\n\ndefaults 是“参考目录”。模组每次启动时都会重新导出内置版本，\n所以不要直接修改 defaults 中的文件，否则下次启动会被覆盖。\n\noverrides 是“实际修改目录”。只有放进这里的 JSON 才会覆盖模组内置表。\n不需要把123张表全部复制过去，只复制你确实想修改的表即可。\n\n\n三、简单配置 ancientwarfare-loot.toml\n------------------------------------------------------------\n直接修改原 TOML 文件并保存，Forge 会自动应用，无须重启或替换模组。\nTOML 通常位于游戏目录的 config/ancientwarfare-loot.toml。\n\n1. globalRollMultiplier\n\n   控制一只容器要完整执行战利品表多少次。\n\n   例子：\n   globalRollMultiplier = 1.0   保持原版数量\n   globalRollMultiplier = 2.0   每只箱子的整张表执行次数翻倍\n   globalRollMultiplier = 0.5   大约一半的执行次数，结果按箱子种子稳定决定\n   globalRollMultiplier = 0.0   关闭所有随机建筑战利品\n\n   注意：它不是简单把每组物品数量乘倍数，而是让整张表多抽或少抽。\n   因此提高它通常会增加物品“种类”和“组数”。\n\n2. globalItemCountMultiplier\n\n   控制已经抽中的每一组物品数量。\n\n   例子：\n   globalItemCountMultiplier = 1.0   保持原数量\n   globalItemCountMultiplier = 2.0   例如原本3个铁锭变成6个\n   globalItemCountMultiplier = 0.5   例如原本3个物品可能变成1或2个\n   globalItemCountMultiplier = 0.0   抽取过程仍进行，但最终物品数量变为0\n\n   小数使用稳定的概率取整。例如1个物品乘0.5后，不会永远取成1，\n   而是约有一半机会保留1个。相同结构种子的结果保持稳定。\n\n   此选项只影响随机生成的战利品，不会放大建筑模板中写死的装饰物或固定物品。\n\n3. preventDuplicateResearchNotes\n\n   true：同一只容器会尽量避免生成内容相同的研究笔记。\n   false：完全按照战利品表抽取，允许重复研究笔记。\n\n   默认值为 true，保持重制版当前行为。\n\n4. enableIceAndFireBonusLoot\n\n   true：安装冰与火模组时，允许古代战争建筑额外生成冰与火奖励。\n   false：只生成古代战争自身战利品，不生成这部分额外奖励。\n\n   如果没有安装冰与火，此项不会产生任何效果。\n\n5. enableSlashBladeBonusLoot\n\n   true：安装拔刀剑时，允许建筑箱额外生成对接奖励。\n   false：关闭这部分额外奖励。\n\n   如果没有安装拔刀剑，此项不会产生任何效果。\n\n6. fillUnconfiguredStructureContainers\n\n   true（默认）：自然生成的 AWS/AWT 建筑中，如果一个容器既没有可用战利品表，\n   最终也没有任何有效固定物品，模组会根据建筑用途、附近工作方块和阵营为它\n   自动选择合适的表。例如帝国磨坊优先使用 village_mill，铁匠铺使用\n   village_blacksmith，矿场使用 village_mine，找不到用途时再使用阵营表。\n\n   false：保留这些未完成的旧模板空箱，不为它们自动补充战利品。\n\n   此功能只处理自然生成的建筑和 AWT 城镇组成建筑。玩家使用模板手动建造的\n   生存建筑不会因此凭空获得战利品。旧模板已有专属表或有效固定物品时也不会覆盖。\n\n7. disabledTables\n\n   用于完全关闭指定表。必须填写完整资源ID，并使用英文半角引号。\n\n   示例：\n   disabledTables = [\n     \"ancientwarfare:chests/empire\",\n     \"ancientwarfare:chests/dwarf\"\n   ]\n\n   上例会让 empire 和 dwarf 两张表不再产生随机物品。\n   可用名称请查看 table-index.txt。\n\n8. tableRollMultipliers\n\n   单独设置某张表的完整执行倍率。格式必须是“资源ID=倍率”。\n\n   示例：\n   tableRollMultipliers = [\n     \"ancientwarfare:chests/empire=2.0\",\n     \"ancientwarfare:chests/dwarf=0.5\",\n     \"ancientwarfare:chests/castle_treasury_high_value=0.25\"\n   ]\n\n   最终倍率 = globalRollMultiplier × 对应单表倍率。\n\n   例如全局倍率为1.5，empire单表倍率为2.0，\n   empire最终按3.0倍完整执行次数计算。\n\n   如果同一张表重复填写，程序使用列表中最先匹配到的一项。\n\n\n四、如何完整修改某一张战利品表？\n------------------------------------------------------------\n以下以 empire.json 为例：\n\n1. 找到参考文件：\n   defaults/data/ancientwarfare/loot_tables/chests/empire.json\n\n2. 将它复制到：\n   overrides/data/ancientwarfare/loot_tables/chests/empire.json\n\n3. 只编辑 overrides 中的 empire.json。\n\n4. 保存后：\n   - 已经进入世界：约0.75秒后自动校验并重载数据资源，完成结果写入日志；\n   - 尚未启动服务器：正常启动即可；\n   - TOML 也会在保存后自动应用，无须执行 /reload 或重启。\n\n5. 必须新生成建筑或打开尚未生成物品的箱子进行测试。\n   已经打开并生成过内容的箱子不会因为配置更新自动重抽。\n\n想恢复内置版本时，只需删除 overrides 中对应的 JSON，运行中的世界会自动重载。\ndefaults 中的参考文件不需要删除。\n\n\n五、战利品表 JSON 最基础的结构\n------------------------------------------------------------\n一个简单例子：\n\n{\n  \"type\": \"minecraft:chest\",\n  \"pools\": [\n    {\n      \"name\": \"main\",\n      \"rolls\": 3,\n      \"entries\": [\n        {\n          \"type\": \"minecraft:item\",\n          \"name\": \"minecraft:iron_ingot\",\n          \"weight\": 10,\n          \"functions\": [\n            {\n              \"function\": \"minecraft:set_count\",\n              \"count\": {\n                \"type\": \"minecraft:uniform\",\n                \"min\": 1,\n                \"max\": 4\n              }\n            }\n          ]\n        },\n        {\n          \"type\": \"minecraft:item\",\n          \"name\": \"minecraft:diamond\",\n          \"weight\": 1\n        },\n        {\n          \"type\": \"minecraft:empty\",\n          \"weight\": 5\n        }\n      ]\n    }\n  ]\n}\n\n字段说明：\n\ntype\n  顶层通常使用 minecraft:chest。\n\npools\n  奖池列表。一张表可以有多个奖池，每个奖池分别抽取。\n\nrolls\n  这个奖池抽几次。3表示抽3次。\n  也可以写成随机范围：\n\n  \"rolls\": {\n    \"type\": \"minecraft:uniform\",\n    \"min\": 3,\n    \"max\": 5\n  }\n\nentries\n  这个奖池中所有候选结果。\n\nweight\n  相对权重，不是直接百分比。\n  如果铁锭权重10、钻石权重1、空结果权重5，总权重为16：\n  - 铁锭约为10/16；\n  - 钻石约为1/16；\n  - 空结果约为5/16。\n\nminecraft:empty\n  表示这一次抽取什么也不给，可用于降低总体奖励概率。\n\nfunctions / minecraft:set_count\n  设置抽中后物品数量。uniform 表示在 min 和 max 之间随机。\n\n\n六、常见修改示例\n------------------------------------------------------------\n1. 增加一种物品\n\n   在 entries 数组中加入：\n\n   {\n     \"type\": \"minecraft:item\",\n     \"name\": \"minecraft:emerald\",\n     \"weight\": 3,\n     \"functions\": [\n       {\n         \"function\": \"minecraft:set_count\",\n         \"count\": {\n           \"type\": \"minecraft:uniform\",\n           \"min\": 1,\n           \"max\": 3\n         }\n       }\n     ]\n   }\n\n   注意前后条目之间必须有英文逗号。\n\n2. 让某件物品更常见\n\n   将它的 weight 从2改成10。\n   权重只表示它相对于同一奖池其他条目的比例。\n\n3. 让一组物品变多\n\n   修改 set_count 中的 min 和 max，例如从1～3改成4～10。\n\n4. 让整个奖池抽更多次\n\n   修改奖池的 rolls，例如从3改成6。\n   这和 TOML 的 tableRollMultipliers 类似，但只影响这张 JSON 中的这个奖池。\n\n5. 引用另一张战利品表\n\n   {\n     \"type\": \"minecraft:loot_table\",\n     \"name\": \"ancientwarfare:chests/research_note\",\n     \"weight\": 2\n   }\n\n   这样抽中时会继续从 research_note 表中生成物品。\n\n6. 随机附魔物品\n\n   {\n     \"type\": \"minecraft:item\",\n     \"name\": \"minecraft:iron_sword\",\n     \"weight\": 3,\n     \"functions\": [\n       {\n         \"function\": \"minecraft:enchant_with_levels\",\n         \"levels\": {\n           \"type\": \"minecraft:uniform\",\n           \"min\": 10,\n           \"max\": 25\n         }\n       }\n     ]\n   }\n\n7. 古代战争钱币\n\n   铜币：\n   {\n     \"type\": \"minecraft:item\",\n     \"name\": \"ancientwarfare:coin\",\n     \"weight\": 10,\n     \"functions\": [\n       {\n         \"function\": \"minecraft:set_nbt\",\n         \"tag\": \"{metal:\"copper\"}\"\n       }\n     ]\n   }\n\n   metal 可使用 copper、silver、gold；不要翻译这些英文值。\n\n8. 研究笔记\n\n   {\n     \"type\": \"minecraft:item\",\n     \"name\": \"ancientwarfare:research_note\",\n     \"weight\": 5,\n     \"functions\": [\n       {\n         \"function\": \"minecraft:set_nbt\",\n         \"tag\": \"{researchName:\"engineering\"}\"\n       }\n     ]\n   }\n\n   researchName 必须是模组中实际存在的研究ID。\n   可直接参考 defaults 中的 research_note.json，不建议凭空猜测名称。\n\n9. 药水\n\n   {\n     \"type\": \"minecraft:item\",\n     \"name\": \"minecraft:potion\",\n     \"weight\": 3,\n     \"functions\": [\n       {\n         \"function\": \"minecraft:set_nbt\",\n         \"tag\": \"{Potion:\"minecraft:healing\"}\"\n       }\n     ]\n   }\n\n   可参考 potion_beneficial.json 和 potion_harmful.json。\n\n\n七、表名大致如何分类？\n------------------------------------------------------------\ntable-index.txt 中列出了全部123张表。常见命名包括：\n\nempire、dwarf、norska、vyncan 等\n  各阵营或种族的通用战利品。\n\ncastle_treasury_low_value / medium_value / high_value\n  城堡宝库的低、中、高价值奖励。\n\ncastle_armoury_*\n  城堡军械库，按装备材质分类。\n\ncastle_pantry_*\n  城堡食品储藏室。\n\nvillage_*\n  村庄职业、农场、矿场、图书馆等专用表。\n\ngravestone_*\n  坟墓或墓地容器。\n\npotion_beneficial / potion_harmful\n  正面与负面药水子表。\n\nenchanted_book、research_note\n  附魔书和研究笔记子表。\n\nammo_*\n  各类攻城武器弹药。\n\n如果不知道某座建筑使用哪张表，可以先查看箱子生成后的日志，\n或搜索 AWS 模板中的 lootTableName。\n\n\n八、普通 lootLevel 箱与专属表的区别\n------------------------------------------------------------\n部分旧建筑使用 lootLevel，而不是 ancientwarfare:chests/... 专属表：\n\nlootLevel 0：不生成随机物品；模板为空时就是空箱。\nlootLevel 1：完整抽取一次 minecraft:chests/simple_dungeon。\nlootLevel 2：完整抽取两次 minecraft:chests/simple_dungeon。\nlootLevel 3：完整抽取三次 minecraft:chests/simple_dungeon。\n\nTOML 的全局抽取倍率也会影响这些箱子。\n\n如果要覆盖它们使用的原版简单地牢表，需要制作完整数据包并覆盖\ndata/minecraft/loot_tables/chests/simple_dungeon.json。\n本目录默认只导出古代战争自己的123张表，不会导出所有原版表。\n\n\n九、多人服务器如何使用？\n------------------------------------------------------------\n- 所有战利品配置由服务端决定。\n- 只需要修改服务器的 config 目录。\n- 普通玩家客户端不需要复制 TOML 或 overrides。\n- 修改原 JSON 或 TOML 并保存，即可自动生效，无须手动 /reload 或重启。\n- JSON 错误时保留上一份有效资源；修正后再次保存即可重试。\n- 战利品更新会重载完整数据资源，大型整合包可能短暂增加负载。\n- 如果服务器同时有世界数据包，优先级更高的数据包可能再次覆盖这些表。\n\n\n十、常见错误与排查方法\n------------------------------------------------------------\n1. 修改后完全没变化\n   - 是否误改了 defaults，而不是 overrides？\n   - 文件路径和文件名是否与 defaults 完全一致？\n   - logs/latest.log 是否记录了成功应用？是否有 JSON 校验错误？\n   - 测试箱是否早已打开并生成过物品？请重新生成建筑测试。\n\n2. 整张表不出物品\n   - JSON 可能存在语法错误；\n   - 物品ID可能不存在；\n   - disabledTables 中可能禁用了它；\n   - 全局或单表倍率可能设置成0；\n   - 箱子本身可能是 lootLevel 0 的设计空箱。\n\n3. JSON 常见语法错误\n   - 使用了中文逗号、中文引号或中文冒号；\n   - 最后一个数组条目后仍有多余逗号；\n   - 少了右方括号 ] 或右花括号 }；\n   - 把说明文字直接写进 JSON。标准 JSON 不支持注释；\n   - 物品ID没有命名空间，例如只写 iron_ingot，应写 minecraft:iron_ingot。\n\n4. 去哪里看错误？\n   查看游戏或服务器目录：\n   logs/latest.log\n\n   搜索关键词：\n   loot table\n   Couldn't parse\n   Unknown item\n   Parsing error\n   ancientwarfare\n\n5. 保存后报告 JSON 错误\n   自动加载前会校验文件，错误时保留上一份有效资源。\n   根据日志修正刚修改的 JSON，再保存即可；也可删除该覆盖表恢复内置版本。\n\n\n十一、重要注意事项\n------------------------------------------------------------\n- 永远不要直接修改模组 JAR 中的 JSON。\n- 永远不要把 defaults 当成实际配置目录。\n- 修改前建议备份 overrides。\n- JSON 文件必须保存为 UTF-8。\n- 旧箱子不会自动刷新；用新生成的结构或尚未开启的战利品箱测试。\n- 权重不是百分比，必须结合同一奖池的总权重计算。\n- globalRollMultiplier 增加“抽取组数”，globalItemCountMultiplier 增加“每组数量”。\n- 外部模组奖励是额外奖励，可用 TOML 独立关闭。\n- 如果多个数据包覆盖同一路径，最终结果由数据包优先级决定。\n";
   }

   private static String englishInstructions() {
      return "============================================================\nAncient Warfare 2 Remastered: Structure Loot Configuration\nMinecraft 1.20.1\n============================================================\n\n1. What can be configured?\n------------------------------------------------------------\nThese settings control loot generated in Ancient Warfare structure chests,\nloot baskets, urns, coffins and other supported containers.\n\nThere are two configuration layers:\n\nA. Simple TOML settings\n   Use config/ancientwarfare-loot.toml to change global table executions,\n   item stack sizes, individual table multipliers, disabled tables,\n   duplicate research notes and optional-mod bonus loot.\n\nB. Full JSON loot-table overrides\n   Use standard Minecraft loot-table JSON to add or remove items,\n   change weights and pool rolls, alter count ranges, add enchantments,\n   or completely replace one of the 123 built-in Ancient Warfare tables.\n\n\n2. Generated files and directories\n------------------------------------------------------------\nconfig/\n├─ ancientwarfare-loot.toml\n└─ ancientwarfare-loot-tables/\n   ├─ README-zh_cn.txt\n   ├─ README-en_us.txt\n   ├─ table-index.txt\n   ├─ defaults/data/ancientwarfare/loot_tables/chests/\n   └─ overrides/data/ancientwarfare/loot_tables/chests/\n\nThe defaults directory contains reference copies of every built-in table.\nIt is refreshed when the mod starts. Do not edit it.\n\nThe overrides directory is the active editable data pack. Copy only the tables\nyou want to change into overrides, preserving the same file name and path.\nTables absent from overrides continue using the version inside the mod.\n\n\n3. TOML settings\n------------------------------------------------------------\nEdit the existing TOML and save. Forge applies it automatically without restarting.\n\nglobalRollMultiplier\n  Multiplies the number of complete loot-table executions requested by a container.\n\n  1.0 = unchanged\n  2.0 = execute the complete table twice as often\n  0.5 = approximately half as many executions, using stable seeded rounding\n  0.0 = disable all random Ancient Warfare structure loot\n\n  This usually changes how many groups and varieties of items appear.\n\nglobalItemCountMultiplier\n  Multiplies the size of every generated item stack after the table is rolled.\n\n  1.0 = unchanged\n  2.0 = a stack of 3 becomes 6\n  0.5 = a stack of 3 becomes 1 or 2 using seeded random rounding\n  0.0 = generated stacks are removed\n\n  It does not multiply fixed items stored directly in an AWS template.\n\npreventDuplicateResearchNotes\n  true prevents duplicate research-note subjects in one container where possible.\n  false follows the loot table exactly and permits duplicates.\n\nenableIceAndFireBonusLoot\n  Enables the extra Ice and Fire integration rewards when Ice and Fire is installed.\n\nenableSlashBladeBonusLoot\n  Enables the extra SlashBlade integration rewards when SlashBlade is installed.\n\nfillUnconfiguredStructureContainers\n  true (default) assigns an appropriate existing loot table to an otherwise empty container in a\n  naturally generated AWS/AWT structure. Building purpose is preferred, then nearby workstations,\n  faction, structure value and finally structure_basic. Existing configured loot and valid fixed\n  contents are preserved. Player-built template structures are not filled this way.\n\n  false preserves unfinished legacy empty containers exactly as empty.\n\ndisabledTables\n  A list of full table resource locations that should generate nothing.\n\n  Example:\n  disabledTables = [\n    \"ancientwarfare:chests/empire\",\n    \"ancientwarfare:chests/dwarf\"\n  ]\n\ntableRollMultipliers\n  Optional per-table execution multipliers in resource-location=number format.\n\n  Example:\n  tableRollMultipliers = [\n    \"ancientwarfare:chests/empire=2.0\",\n    \"ancientwarfare:chests/dwarf=0.5\",\n    \"ancientwarfare:chests/castle_treasury_high_value=0.25\"\n  ]\n\n  Effective multiplier = globalRollMultiplier × per-table multiplier.\n  If the same table is listed more than once, the first matching entry is used.\n\n\n4. Editing a complete table\n------------------------------------------------------------\nExample for empire.json:\n\n1. Find:\n   defaults/data/ancientwarfare/loot_tables/chests/empire.json\n\n2. Copy it to:\n   overrides/data/ancientwarfare/loot_tables/chests/empire.json\n\n3. Edit only the copy under overrides.\n\n4. Save it. Changes are validated after about 0.75 seconds and data resources reload automatically.\n   TOML saves also apply automatically. No manual /reload or restart is needed.\n\n5. Test with a newly generated structure or a loot container that has never\n   generated its contents. Previously opened containers are not rerolled.\n\nTo restore the built-in table, delete its JSON from overrides. The running world reloads automatically.\n\n\n5. Basic loot-table JSON\n------------------------------------------------------------\n{\n  \"type\": \"minecraft:chest\",\n  \"pools\": [\n    {\n      \"name\": \"main\",\n      \"rolls\": 3,\n      \"entries\": [\n        {\n          \"type\": \"minecraft:item\",\n          \"name\": \"minecraft:iron_ingot\",\n          \"weight\": 10,\n          \"functions\": [\n            {\n              \"function\": \"minecraft:set_count\",\n              \"count\": {\n                \"type\": \"minecraft:uniform\",\n                \"min\": 1,\n                \"max\": 4\n              }\n            }\n          ]\n        },\n        {\n          \"type\": \"minecraft:item\",\n          \"name\": \"minecraft:diamond\",\n          \"weight\": 1\n        },\n        {\n          \"type\": \"minecraft:empty\",\n          \"weight\": 5\n        }\n      ]\n    }\n  ]\n}\n\npools are independent groups of rolls.\nrolls controls how many selections the pool makes.\nentries contains the possible results.\nweight is relative, not a direct percentage.\n\nIn the example the total weight is 16: iron is about 10/16,\ndiamond is about 1/16 and the empty result is about 5/16 per roll.\n\nA random roll range can be written as:\n\n\"rolls\": {\n  \"type\": \"minecraft:uniform\",\n  \"min\": 3,\n  \"max\": 5\n}\n\n\n6. Common entries\n------------------------------------------------------------\nAdd emeralds with a random count:\n\n{\n  \"type\": \"minecraft:item\",\n  \"name\": \"minecraft:emerald\",\n  \"weight\": 3,\n  \"functions\": [\n    {\n      \"function\": \"minecraft:set_count\",\n      \"count\": {\n        \"type\": \"minecraft:uniform\",\n        \"min\": 1,\n        \"max\": 3\n      }\n    }\n  ]\n}\n\nReference another table:\n\n{\n  \"type\": \"minecraft:loot_table\",\n  \"name\": \"ancientwarfare:chests/research_note\",\n  \"weight\": 2\n}\n\nRandomly enchant a sword:\n\n{\n  \"type\": \"minecraft:item\",\n  \"name\": \"minecraft:iron_sword\",\n  \"weight\": 3,\n  \"functions\": [\n    {\n      \"function\": \"minecraft:enchant_with_levels\",\n      \"levels\": {\n        \"type\": \"minecraft:uniform\",\n        \"min\": 10,\n        \"max\": 25\n      }\n    }\n  ]\n}\n\nAncient Warfare copper coin:\n\n{\n  \"type\": \"minecraft:item\",\n  \"name\": \"ancientwarfare:coin\",\n  \"weight\": 10,\n  \"functions\": [\n    {\n      \"function\": \"minecraft:set_nbt\",\n      \"tag\": \"{metal:\"copper\"}\"\n    }\n  ]\n}\n\nValid coin metal values include copper, silver and gold.\n\nResearch note:\n\n{\n  \"type\": \"minecraft:item\",\n  \"name\": \"ancientwarfare:research_note\",\n  \"weight\": 5,\n  \"functions\": [\n    {\n      \"function\": \"minecraft:set_nbt\",\n      \"tag\": \"{researchName:\"engineering\"}\"\n    }\n  ]\n}\n\nUse an existing research id. See defaults/.../research_note.json for valid examples.\n\nPotion:\n\n{\n  \"type\": \"minecraft:item\",\n  \"name\": \"minecraft:potion\",\n  \"weight\": 3,\n  \"functions\": [\n    {\n      \"function\": \"minecraft:set_nbt\",\n      \"tag\": \"{Potion:\"minecraft:healing\"}\"\n    }\n  ]\n}\n\nSee potion_beneficial.json and potion_harmful.json for more potion examples.\n\n\n7. Table-name groups\n------------------------------------------------------------\nempire, dwarf, norska, vyncan, etc. are faction or race tables.\ncastle_treasury_* contains low, medium and high-value treasury rewards.\ncastle_armoury_* contains equipment grouped by material.\ncastle_pantry_* contains food storage rewards.\nvillage_* contains profession and village-building rewards.\ngravestone_* contains grave and cemetery rewards.\npotion_beneficial and potion_harmful are potion sub-tables.\nenchanted_book and research_note are reusable sub-tables.\nammo_* contains siege-ammunition rewards.\n\nSee table-index.txt for all 123 names.\n\n\n8. Legacy lootLevel containers\n------------------------------------------------------------\nSome old AWS templates use lootLevel instead of an Ancient Warfare table:\n\nlootLevel 0 = no random loot\nlootLevel 1 = execute minecraft:chests/simple_dungeon once\nlootLevel 2 = execute it twice\nlootLevel 3 = execute it three times\n\nThe TOML execution multiplier affects these containers too.\nThis folder exports Ancient Warfare's 123 tables only. To replace the vanilla\nsimple-dungeon table, use a separate data pack overriding:\ndata/minecraft/loot_tables/chests/simple_dungeon.json\n\n\n9. Multiplayer servers\n------------------------------------------------------------\nLoot is server-authoritative. Edit only the server config directory.\nClients do not need the TOML or override JSON files.\nSave the existing JSON or TOML to apply changes automatically; no /reload or restart is needed.\nInvalid JSON retains the last working resources. Fix it and save again to retry.\nLoot edits reload all data resources; large modpacks may experience a brief increase in load.\nA world data pack with higher priority may override these same table paths again.\n\n\n10. Troubleshooting\n------------------------------------------------------------\nIf a change has no effect:\n- Make sure you edited overrides, not defaults.\n- Preserve the exact path and file name.\n- Check logs/latest.log for successful application or JSON validation errors.\n- Test a newly generated or unopened loot container.\n\nIf a table becomes empty:\n- Check for JSON syntax errors or unknown item ids.\n- Check disabledTables and both multiplier settings.\n- The original template may deliberately use lootLevel 0.\n\nJSON does not support comments or trailing commas. Use straight ASCII quotes,\ncommas and colons. Every item id needs a namespace such as minecraft:iron_ingot.\n\nCheck logs/latest.log and search for:\nloot table\nCouldn't parse\nUnknown item\nParsing error\nancientwarfare\n\nInvalid saves retain the last working resources. Fix the reported JSON and save again,\nor delete its override to restore the built-in table.\n\n\n11. Important reminders\n------------------------------------------------------------\n- Never edit loot JSON inside the mod jar.\n- Never use defaults as the active configuration directory.\n- Back up overrides before large changes.\n- Save JSON as UTF-8.\n- Existing opened containers do not refresh automatically.\n- Weight is relative, not a percentage.\n- Roll multiplier changes how many groups are selected.\n- Item count multiplier changes the size of selected stacks.\n- Optional-mod rewards are extra and can be disabled independently.\n";
   }
}
