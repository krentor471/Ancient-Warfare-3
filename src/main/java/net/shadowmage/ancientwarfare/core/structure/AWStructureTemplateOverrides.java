package net.shadowmage.ancientwarfare.core.structure;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;

public final class AWStructureTemplateOverrides {
   private static final Path ROOT = AWCorePlatform.configDirectory().resolve("ancientwarfare-structure-generation");
   private static final Path OVERRIDES = ROOT.resolve("overrides.json");
   private static final String AWS_INDEX = "/assets/ancientwarfare/template_index.txt";
   private static final String AWT_INDEX = "/assets/ancientwarfare/town_template_index.txt";
   private static volatile AWStructureTemplateOverrides.Snapshot active = new AWStructureTemplateOverrides.Snapshot(Map.of(), Map.of());

   private AWStructureTemplateOverrides() {
   }

   public static void initialize() {
      try {
         Files.createDirectories(ROOT);
         if (Files.notExists(OVERRIDES)) {
            Files.writeString(OVERRIDES, "{\n  \"aws\": {},\n  \"awt\": {}\n}\n", StandardCharsets.UTF_8);
         }

         writeText(ROOT.resolve("README-zh_cn.txt"), chineseInstructions());
         writeText(ROOT.resolve("README-en_us.txt"), englishInstructions());
         writeText(ROOT.resolve("examples.json"), examples());
         copyResource("/assets/ancientwarfare/template_index.txt", ROOT.resolve("aws-template-index.txt"));
         copyResource("/assets/ancientwarfare/town_template_index.txt", ROOT.resolve("awt-template-index.txt"));
         reload();
      } catch (IOException var1) {
         AncientWarfareCore.LOG.error("Unable to prepare AWS/AWT structure generation configuration at {}", ROOT, var1);
      }
   }

   public static Path overrideFile() {
      return OVERRIDES;
   }

   public static boolean reload() {
      try {
         JsonElement rootElement = (JsonElement)(Files.exists(OVERRIDES)
            ? JsonParser.parseString(Files.readString(OVERRIDES, StandardCharsets.UTF_8))
            : new JsonObject());
         if (!rootElement.isJsonObject()) {
            throw new IllegalArgumentException("The root value must be a JSON object");
         } else {
            JsonObject root = rootElement.getAsJsonObject();
            Map<String, AWStructureTemplateOverrides.Entry> aws = readSection(root, "aws");
            Map<String, AWStructureTemplateOverrides.Entry> awt = readSection(root, "awt");
            active = new AWStructureTemplateOverrides.Snapshot(aws, awt);
            AncientWarfareCore.LOG.info("Loaded {} AWS and {} AWT per-template generation overrides from {}", new Object[]{aws.size(), awt.size(), OVERRIDES});
            return true;
         }
      } catch (RuntimeException | IOException var4) {
         AncientWarfareCore.LOG.error("Unable to reload {}; keeping the last working structure overrides. Fix and save the file to retry", OVERRIDES, var4);
         return false;
      }
   }

   private static Map<String, AWStructureTemplateOverrides.Entry> readSection(JsonObject root, String name) {
      if (!root.has(name)) {
         return Map.of();
      } else if (!root.get(name).isJsonObject()) {
         throw new IllegalArgumentException(name + " must be an object");
      } else {
         Map<String, AWStructureTemplateOverrides.Entry> result = new LinkedHashMap<>();

         for (Map.Entry<String, JsonElement> value : root.getAsJsonObject(name).entrySet()) {
            if (!value.getValue().isJsonObject()) {
               throw new IllegalArgumentException(name + "." + value.getKey() + " must be an object");
            }

            JsonObject object = value.getValue().getAsJsonObject();
            Boolean enabled = optionalBoolean(object, "enabled");
            Integer weight = optionalInteger(object, "selectionWeight");
            if (weight != null) {
               weight = Math.max(0, weight);
            }

            Boolean whitelist = optionalBoolean(object, "biomeWhiteList");
            Set<String> biomes = null;
            if (object.has("biomes")) {
               if (!object.get("biomes").isJsonArray()) {
                  throw new IllegalArgumentException("biomes must be an array");
               }

               Set<String> var13 = new LinkedHashSet();

               for (JsonElement biome : object.getAsJsonArray("biomes")) {
                  if (!biome.isJsonPrimitive() || !biome.getAsJsonPrimitive().isString()) {
                     throw new IllegalArgumentException("biomes must contain string ids");
                  }

                  String normalized = biome.getAsString().trim().toLowerCase(Locale.ROOT);
                  if (ResourceLocation.tryParse(normalized) == null) {
                     throw new IllegalArgumentException("Invalid biome id: " + normalized);
                  }

                  var13.add(normalized);
               }

               biomes = Set.copyOf(var13);
            }

            result.put(normalizePath(value.getKey()), new AWStructureTemplateOverrides.Entry(enabled, weight, whitelist, biomes));
         }

         return Map.copyOf(result);
      }
   }

   private static Boolean optionalBoolean(JsonObject object, String key) {
      if (!object.has(key)) {
         return null;
      } else {
         JsonElement value = object.get(key);
         if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
            return value.getAsBoolean();
         } else {
            throw new IllegalArgumentException(key + " must be true or false");
         }
      }
   }

   private static Integer optionalInteger(JsonObject object, String key) {
      if (!object.has(key)) {
         return null;
      } else {
         JsonElement value = object.get(key);
         if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            return value.getAsBigDecimal().intValueExact();
         } else {
            throw new IllegalArgumentException(key + " must be an integer");
         }
      }
   }

   public static boolean awsEnabled(String path, boolean fallback) {
      AWStructureTemplateOverrides.Entry entry = active.aws.get(normalizePath(path));
      return entry != null && entry.enabled != null ? entry.enabled : fallback;
   }

   public static int awsWeight(String path, int fallback) {
      AWStructureTemplateOverrides.Entry entry = active.aws.get(normalizePath(path));
      return entry != null && entry.selectionWeight != null ? entry.selectionWeight : fallback;
   }

   public static boolean awsBiomeWhiteList(String path, boolean fallback) {
      AWStructureTemplateOverrides.Entry entry = active.aws.get(normalizePath(path));
      return entry != null && entry.biomeWhiteList != null ? entry.biomeWhiteList : fallback;
   }

   public static Set<String> awsBiomes(String path, Set<String> fallback) {
      AWStructureTemplateOverrides.Entry entry = active.aws.get(normalizePath(path));
      return entry != null && entry.biomes != null ? entry.biomes : fallback;
   }

   public static boolean awtEnabled(String path) {
      AWStructureTemplateOverrides.Entry entry = active.awt.get(normalizePath(path));
      return entry == null || entry.enabled == null || entry.enabled;
   }

   public static int awtWeight(String path, int fallback) {
      AWStructureTemplateOverrides.Entry entry = active.awt.get(normalizePath(path));
      return entry != null && entry.selectionWeight != null ? entry.selectionWeight : fallback;
   }

   public static boolean awtBiomeWhiteList(String path, boolean fallback) {
      AWStructureTemplateOverrides.Entry entry = active.awt.get(normalizePath(path));
      return entry != null && entry.biomeWhiteList != null ? entry.biomeWhiteList : fallback;
   }

   public static Set<String> awtBiomes(String path, Set<String> fallback) {
      AWStructureTemplateOverrides.Entry entry = active.awt.get(normalizePath(path));
      return entry != null && entry.biomes != null ? entry.biomes : fallback;
   }

   private static String normalizePath(String path) {
      return path == null ? "" : path.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
   }

   private static void copyResource(String resource, Path target) throws IOException {
      try (InputStream input = AWStructureTemplateOverrides.class.getResourceAsStream(resource)) {
         if (input == null) {
            throw new IOException("Missing resource " + resource);
         }

         Files.write(target, input.readAllBytes());
      }
   }

   private static void writeText(Path target, String contents) throws IOException {
      Files.writeString(target, contents, StandardCharsets.UTF_8);
   }

   private static String examples() {
      return "{\n  \"aws\": {\n    \"MohawkyPack1.12.2V72/normal/AWNationEmpireCoastal/EmpireColonyCityLarge.aws\": {\n      \"enabled\": false,\n      \"selectionWeight\": 5,\n      \"biomeWhiteList\": true,\n      \"biomes\": [\"minecraft:plains\", \"minecraft:forest\"]\n    }\n  },\n  \"awt\": {\n    \"MohawkyPack1.12.2V72/towns/town_templates/EmpireWalledCity.awt\": {\n      \"enabled\": true,\n      \"selectionWeight\": 3,\n      \"biomeWhiteList\": true,\n      \"biomes\": [\"minecraft:plains\"]\n    }\n  }\n}\n";
   }

   private static String chineseInstructions() {
      return "============================================================\n古代战争2 重制版：AWS/AWT 自然生成配置说明\n============================================================\n\n本目录用于控制哪些古代战争建筑会自然生成、生成频率、权重和生物群系。\n修改原配置文件并保存即可自动生效，无须替换文件、执行 /reload 或重启。\nJSON 保存约0.75秒后校验并刷新；错误时保留上一份有效规则，修正后再次保存即可。\n更新只影响后续生成选择，不删除已有建筑，不取消已经开始的施工。\n\n一、两个配置层\n\n1. config/ancientwarfare-structures.toml\n   用于调整整体规则：\n   - 是否允许 AWS 独立建筑自然生成；\n   - 是否允许大型水上/岛屿 AWS 自然生成；\n   - AWS 候选区域大小和建筑间距；\n   - 是否允许 AWT 城镇自然生成；\n   - AWT 候选区域大小、出生点保护距离和城镇间距。\n\n   candidateRegionSizeChunks 越小，候选点越密，建筑越常见。\n   AWS 默认12，即每个12×12区块区域最多选择一个候选区块。\n   AWT 默认96，即每个96×96区块区域最多选择一个候选区块。\n   候选点仍需通过地形、生物群系、模板尺寸和间距检查，因此不是每次都能生成。\n\n2. overrides.json\n   用于单独修改某一张 AWS 或 AWT 模板。默认文件是空的：\n\n   {\n     \"aws\": {},\n     \"awt\": {}\n   }\n\n   可从 examples.json 复制示例，再把路径替换成索引中的真实路径。\n   aws-template-index.txt 列出全部 AWS；awt-template-index.txt 列出全部 AWT。\n\n二、每个模板支持的字段\n\nenabled\n  true：允许自然生成。\n  false：禁止自然生成。\n  它不会删除创造物品栏里的模板，也不会禁止命令或玩家手动放置。\n\nselectionWeight\n  同一地点有多张合格模板时的相对权重，不是百分比。\n  例如三张表权重为10、5、1，则它们每次被选中的相对机会约为10:5:1。\n  设为0相当于不参与自然生成。建议用 enabled=false 表达禁用，意思更清楚。\n\nbiomeWhiteList\n  true：biomes 是允许生成的生物群系列表。\n  false：biomes 是禁止生成的生物群系列表，其他生物群系允许。\n\nbiomes\n  生物群系ID数组，例如：\n  [\"minecraft:plains\", \"minecraft:forest\"]\n\n  模组也会进行保守的同类生物群系兼容。例如旧模板写平原时，部分带有\n  Forge平原标签的模组生物群系也可能匹配。\n\n字段可以只写需要修改的部分。例如只禁用一座建筑：\n\n{\n  \"aws\": {\n    \"从aws-template-index.txt复制的完整路径.aws\": {\n      \"enabled\": false\n    }\n  },\n  \"awt\": {}\n}\n\n三、AWS 与 AWT 的区别\n\nAWS 是单座建筑模板，也可能是水上建筑或岛屿。\nAWT 是城镇布局模板，里面会引用许多 AWS 作为城墙、房屋和装饰。\n禁止某张 AWS 自然生成，只会阻止它作为独立野外建筑被抽中；如果某张 AWT\n明确引用这张 AWS，它仍可能作为城镇组成部分出现。要修改整座城镇的选择，\n请在 awt 区域修改对应 AWT。\n\n四、重要注意事项\n\n- 配置只影响尚未生成的新区域，不会删除或改变世界中已有建筑。\n- overrides.json 必须使用UTF-8、英文双引号和英文逗号；标准JSON不支持注释。\n- 模板路径虽忽略大小写和斜杠方向，但建议直接从索引完整复制。\n- 不需要也不建议导出约140MB的全部AWS建筑正文；索引和轻量覆盖足以调整生成。\n- JSON写错时保留上一份有效覆盖，并在 logs/latest.log 记录错误；修正后再次保存即可。\n- 删除 overrides.json 会恢复内置规则，已开始的施工仍继续完成。\n- examples.json 每次启动会恢复成示例；不要直接修改它，实际配置只写 overrides.json。\n- 提高生成密度会增加新区块生成时的计算和磁盘压力，服务器请逐步调整。\n- AWS支持无人区域的新区块预生成。candidateIntervalTicks控制候选处理间隔，默认20 tick。\n- 预生成工具完成地形后，AWS队列可能仍在施工；可用 /awworldgen status 查询。\n- 待处理候选和已尝试记录随存档保存。已有预生成区域不会批量补扫，AWT仍由玩家触发。\n";
   }

   private static String englishInstructions() {
      return "============================================================\nAncient Warfare 2 Remastered: AWS/AWT Generation Settings\n============================================================\n\nEdit the existing files and save to apply changes automatically. No /reload or restart is needed.\nJSON saves are debounced for about 0.75 seconds. Invalid saves retain the last working rules;\nfix the file and save again. Changes affect future selection, not existing or active construction.\n\nconfig/ancientwarfare-structures.toml controls global AWS and AWT generation density,\nspawn protection and minimum distances. A smaller candidateRegionSizeChunks creates denser\ncandidate sites. Terrain, biome, world build height and distance checks can still reject a candidate.\nAWS new-chunk generation supports remote pregeneration without players. candidateIntervalTicks\nlimits queued attempts (default 20 ticks). Use /awworldgen status to check pending candidates.\nA pregenerator finishing terrain does not mean the AWS queue has drained. Pending/attempted\ncandidates survive world saves. Existing areas are not bulk rescanned; AWT remains player-triggered.\n\noverrides.json controls individual templates. Copy paths from aws-template-index.txt or\nawt-template-index.txt. See examples.json for the complete format.\n\nSupported fields:\nenabled: true allows and false prevents natural selection. Manual/command placement remains available.\nselectionWeight: relative selection weight, not a percentage. Zero removes it from selection.\nbiomeWhiteList: true makes biomes an allow-list; false makes it a deny-list.\nbiomes: an array of biome ids such as [\"minecraft:plains\", \"minecraft:forest\"].\n\nAWS files are individual buildings. AWT files are town layouts which reference many AWS buildings.\nDisabling an AWS as a standalone natural structure does not remove it from a town that explicitly\nreferences it. Configure the corresponding AWT to control selection of the complete town.\n\nChanges affect only newly generated terrain. Existing structures are not changed or removed.\nUse UTF-8 and valid JSON; comments and trailing commas are not supported. If parsing fails,\nthe last working overrides remain active and the error is written to logs/latest.log.\nDelete overrides.json to restore built-in rules. Active construction still completes.\nexamples.json is refreshed on startup; edit overrides.json instead.\n";
   }

   private static record Entry(Boolean enabled, Integer selectionWeight, Boolean biomeWhiteList, Set<String> biomes) {
   }

   private static record Snapshot(Map<String, AWStructureTemplateOverrides.Entry> aws, Map<String, AWStructureTemplateOverrides.Entry> awt) {
   }
}
