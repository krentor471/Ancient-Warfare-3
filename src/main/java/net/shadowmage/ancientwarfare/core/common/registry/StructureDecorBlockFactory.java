package net.shadowmage.ancientwarfare.core.common.registry;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.shadowmage.ancientwarfare.core.block.BenchStructureDecorBlock;
import net.shadowmage.ancientwarfare.core.block.CoinStackBlock;
import net.shadowmage.ancientwarfare.core.block.FourPartStructureDecorBlock;
import net.shadowmage.ancientwarfare.core.block.HorizontalFourPartStructureDecorBlock;
import net.shadowmage.ancientwarfare.core.block.LitStructureDecorBlock;
import net.shadowmage.ancientwarfare.core.block.StructureDecorBlock;
import net.shadowmage.ancientwarfare.core.block.TableStructureDecorBlock;
import net.shadowmage.ancientwarfare.core.block.TallStructureDecorBlock;
import net.shadowmage.ancientwarfare.core.block.ThreePartStructureDecorBlock;
import net.shadowmage.ancientwarfare.core.common.npc.CoinMetal;

public final class StructureDecorBlockFactory {
   public static final List<String> IDS = List.of(
      "scissor_seat",
      "tribal_stool",
      "wooden_throne",
      "golden_idol",
      "golden_throne",
      "orc_totem_1",
      "orc_totem_2",
      "orc_totem_2_lit",
      "goblin_totem_1",
      "goblin_totem_2",
      "goblin_totem_2_lit",
      "iron_cage",
      "gibbet",
      "wooden_post",
      "stool",
      "table",
      "chair",
      "bench",
      "tribal_chair",
      "altar_lectern",
      "altar_sun",
      "fire_pit",
      "fire_pit_log",
      "fire_pit_square",
      "brazier_flame",
      "brazier_ember",
      "coin_stack_copper",
      "coin_stack_silver",
      "coin_stack_gold",
      "coin_stack_ancient",
      "stretching_rack",
      "gravestone_1",
      "gravestone_2",
      "gravestone_3",
      "gravestone_4",
      "gravestone_5",
      "gravestone_6",
      "gravestone_7",
      "gravestone_8",
      "altar_short_cloth",
      "altar_long_cloth",
      "altar_candle",
      "stake",
      "totem_part_base",
      "totem_part_mid",
      "totem_part_mid_alt",
      "totem_part_top",
      "totem_part_wings",
      "totem_part_irminsul_base",
      "totem_part_irminsul_mid",
      "totem_part_irminsul_top",
      "totem_part_irminsul_side",
      "totem_part_elephant_body",
      "totem_part_elephant_left",
      "totem_part_elephant_right",
      "xoltec_idol_body_left",
      "xoltec_idol_body_right",
      "xoltec_idol_head_left",
      "xoltec_idol_head_right"
   );
   public static final Set<String> CUTOUT_IDS = Set.of(
      "iron_cage",
      "gibbet",
      "fire_pit",
      "fire_pit_log",
      "fire_pit_square",
      "brazier_flame",
      "brazier_ember",
      "stretching_rack",
      "altar_short_cloth",
      "altar_long_cloth",
      "altar_candle",
      "stake",
      "totem_part_wings",
      "totem_part_irminsul_side",
      "totem_part_elephant_body",
      "totem_part_elephant_left",
      "totem_part_elephant_right"
   );

   private StructureDecorBlockFactory() {
   }

   public static Block create(String id) {
      return (Block)(switch (id) {
         case "scissor_seat" -> new StructureDecorBlock(wood(), scissorSeatShapes());
         case "tribal_stool" -> new StructureDecorBlock(wood(), tribalStoolShapes());
         case "wooden_throne" -> new TallStructureDecorBlock(wood(), woodenThroneLowerShapes(), woodenThroneUpperShapes());
         case "golden_idol" -> new TallStructureDecorBlock(metal(), fullBlockShapes(), fullBlockShapes());
         case "golden_throne" -> new TallStructureDecorBlock(metal(), goldenThroneLowerShapes(), goldenThroneUpperShapes(), true);
         case "orc_totem_1", "orc_totem_2", "goblin_totem_1", "goblin_totem_2" -> new StructureDecorBlock(stone(), fullBlockShapes());
         case "orc_totem_2_lit", "goblin_totem_2_lit" -> new StructureDecorBlock(stone().lightLevel(state -> 14), fullBlockShapes());
         case "iron_cage" -> new FourPartStructureDecorBlock(metal());
         case "gibbet", "wooden_post" -> new FourPartStructureDecorBlock(wood());
         case "stool" -> new StructureDecorBlock(wood(), stoolShapes());
         case "table" -> new TableStructureDecorBlock(wood());
         case "chair" -> new StructureDecorBlock(wood(), chairShapes());
         case "bench" -> new BenchStructureDecorBlock(wood());
         case "tribal_chair" -> new TallStructureDecorBlock(wood(), tribalChairLowerShapes(), tribalChairUpperShapes());
         case "altar_lectern" -> new StructureDecorBlock(wood(), fullBlockShapes());
         case "altar_sun" -> new StructureDecorBlock(stone().noOcclusion(), fullBlockShapes());
         case "fire_pit" -> new LitStructureDecorBlock(stone().noOcclusion().lightLevel(state -> 15), firePitShape());
         case "fire_pit_log" -> new LitStructureDecorBlock(wood().lightLevel(state -> 15), firePitShape());
         case "fire_pit_square" -> new LitStructureDecorBlock(stone().noOcclusion().lightLevel(state -> 15), firePitShape());
         case "brazier_flame" -> new LitStructureDecorBlock(stone().noOcclusion().lightLevel(state -> 15), brazierFlameShape());
         case "brazier_ember" -> new LitStructureDecorBlock(stone().noOcclusion().lightLevel(state -> 6), brazierEmberShape());
         case "coin_stack_copper" -> new CoinStackBlock(metal().strength(0.3F), CoinMetal.COPPER);
         case "coin_stack_silver" -> new CoinStackBlock(metal().strength(0.3F), CoinMetal.SILVER);
         case "coin_stack_gold" -> new CoinStackBlock(metal().strength(0.3F), CoinMetal.GOLD);
         case "coin_stack_ancient" -> new CoinStackBlock(metal().strength(0.3F), CoinMetal.ANCIENT);
         case "stretching_rack" -> new HorizontalFourPartStructureDecorBlock(wood());
         case "gravestone_1", "gravestone_2", "gravestone_3", "gravestone_4", "gravestone_5", "gravestone_6", "gravestone_7", "gravestone_8" -> new StructureDecorBlock(
         stone().noOcclusion(), gravestoneShapes()
      );
         case "altar_short_cloth", "altar_long_cloth" -> new LitStructureDecorBlock(wood(), fullBlockShape());
         case "altar_candle" -> new LitStructureDecorBlock(metal().lightLevel(state -> 12), Block.box(6.0, 0.0, 6.0, 10.0, 10.0, 10.0));
         case "stake" -> new ThreePartStructureDecorBlock(wood());
         case "totem_part_base", "totem_part_mid", "totem_part_mid_alt", "totem_part_top", "totem_part_irminsul_base", "totem_part_irminsul_mid" -> new StructureDecorBlock(
         wood(), totemShapes()
      );
         case "totem_part_wings", "totem_part_irminsul_top", "totem_part_irminsul_side", "totem_part_elephant_body", "totem_part_elephant_left", "totem_part_elephant_right" -> new StructureDecorBlock(
         wood(), fullBlockShapes()
      );
         case "xoltec_idol_body_left", "xoltec_idol_body_right", "xoltec_idol_head_left", "xoltec_idol_head_right" -> new StructureDecorBlock(
         stone().noOcclusion(), fullBlockShapes()
      );
         default -> throw new IllegalArgumentException("Unknown structure decoration block: " + id);
      });
   }

   private static Properties wood() {
      return Properties.of().mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion();
   }

   private static Properties metal() {
      return Properties.of().mapColor(MapColor.GOLD).strength(2.0F).sound(SoundType.METAL).noOcclusion();
   }

   private static Properties stone() {
      return Properties.of().mapColor(MapColor.STONE).strength(2.0F).sound(SoundType.STONE);
   }

   private static Map<Direction, VoxelShape> fullBlockShapes() {
      return allDirections(fullBlockShape());
   }

   private static VoxelShape fullBlockShape() {
      return Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
   }

   private static Map<Direction, VoxelShape> allDirections(VoxelShape shape) {
      return Map.of(Direction.NORTH, shape, Direction.SOUTH, shape, Direction.EAST, shape, Direction.WEST, shape);
   }

   private static Map<Direction, VoxelShape> totemShapes() {
      return allDirections(Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0));
   }

   private static Map<Direction, VoxelShape> tribalStoolShapes() {
      VoxelShape zAxis = Block.box(0.0, 0.0, 1.0, 16.0, 10.0, 15.0);
      VoxelShape xAxis = Block.box(1.0, 0.0, 0.0, 15.0, 10.0, 16.0);
      return Map.of(Direction.NORTH, zAxis, Direction.SOUTH, zAxis, Direction.EAST, xAxis, Direction.WEST, xAxis);
   }

   private static Map<Direction, VoxelShape> scissorSeatShapes() {
      return Map.of(
         Direction.NORTH,
         StructureDecorBlock.shape(
            Block.box(3.0, 0.0, 3.0, 13.0, 8.0, 12.0),
            Block.box(1.5, 11.0, 12.5, 14.5, 15.5, 13.5),
            Block.box(2.0, 8.0, 2.0, 3.0, 11.0, 13.0),
            Block.box(13.0, 8.0, 2.0, 14.0, 11.0, 13.0)
         ),
         Direction.SOUTH,
         StructureDecorBlock.shape(
            Block.box(3.0, 0.0, 4.0, 13.0, 8.0, 13.0),
            Block.box(1.5, 11.0, 2.5, 14.5, 15.5, 3.5),
            Block.box(2.0, 8.0, 3.0, 3.0, 11.0, 14.0),
            Block.box(13.0, 8.0, 3.0, 14.0, 11.0, 14.0)
         ),
         Direction.EAST,
         StructureDecorBlock.shape(
            Block.box(4.0, 0.0, 3.0, 13.0, 8.0, 13.0),
            Block.box(2.5, 11.0, 1.5, 3.5, 15.5, 14.5),
            Block.box(3.0, 8.0, 2.0, 14.0, 11.0, 3.0),
            Block.box(3.0, 8.0, 13.0, 14.0, 11.0, 14.0)
         ),
         Direction.WEST,
         StructureDecorBlock.shape(
            Block.box(3.0, 0.0, 3.0, 12.0, 8.0, 13.0),
            Block.box(12.5, 11.0, 1.5, 13.5, 15.5, 14.5),
            Block.box(2.0, 8.0, 2.0, 13.0, 11.0, 3.0),
            Block.box(2.0, 8.0, 13.0, 13.0, 11.0, 14.0)
         )
      );
   }

   private static Map<Direction, VoxelShape> stoolShapes() {
      return allDirections(Block.box(3.0, 0.0, 3.0, 13.0, 9.0, 13.0));
   }

   private static VoxelShape firePitShape() {
      return Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);
   }

   private static VoxelShape brazierFlameShape() {
      return Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0);
   }

   private static VoxelShape brazierEmberShape() {
      return Block.box(0.0, 0.0, 0.0, 16.0, 14.0, 16.0);
   }

   private static Map<Direction, VoxelShape> chairShapes() {
      return Map.of(
         Direction.NORTH,
         Block.box(1.0, 0.0, 0.0, 15.0, 16.0, 15.0),
         Direction.SOUTH,
         Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 16.0),
         Direction.EAST,
         Block.box(1.0, 0.0, 1.0, 16.0, 16.0, 15.0),
         Direction.WEST,
         Block.box(0.0, 0.0, 1.0, 15.0, 16.0, 15.0)
      );
   }

   private static Map<Direction, VoxelShape> gravestoneShapes() {
      VoxelShape zAxis = Block.box(0.0, 0.0, 6.0, 16.0, 19.0, 10.0);
      VoxelShape xAxis = Block.box(6.0, 0.0, 0.0, 10.0, 19.0, 16.0);
      return Map.of(Direction.NORTH, zAxis, Direction.SOUTH, zAxis, Direction.EAST, xAxis, Direction.WEST, xAxis);
   }

   private static Map<Direction, VoxelShape> woodenThroneLowerShapes() {
      return Map.of(
         Direction.NORTH,
         Block.box(0.0, 0.0, 1.0, 16.0, 16.0, 16.0),
         Direction.SOUTH,
         Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 15.0),
         Direction.EAST,
         Block.box(0.0, 0.0, 0.0, 15.0, 16.0, 16.0),
         Direction.WEST,
         Block.box(1.0, 0.0, 0.0, 16.0, 16.0, 16.0)
      );
   }

   private static Map<Direction, VoxelShape> woodenThroneUpperShapes() {
      return Map.of(
         Direction.NORTH,
         Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 16.0),
         Direction.SOUTH,
         Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 3.0),
         Direction.EAST,
         Block.box(0.0, 0.0, 0.0, 3.0, 16.0, 16.0),
         Direction.WEST,
         Block.box(13.0, 0.0, 0.0, 16.0, 16.0, 16.0)
      );
   }

   private static Map<Direction, VoxelShape> goldenThroneLowerShapes() {
      return Map.of(
         Direction.NORTH,
         Block.box(0.0, 0.0, 1.0, 16.0, 16.0, 15.0),
         Direction.SOUTH,
         Block.box(0.0, 0.0, 1.0, 16.0, 16.0, 15.0),
         Direction.EAST,
         Block.box(1.0, 0.0, 0.0, 15.0, 16.0, 16.0),
         Direction.WEST,
         Block.box(1.0, 0.0, 0.0, 15.0, 16.0, 16.0)
      );
   }

   private static Map<Direction, VoxelShape> goldenThroneUpperShapes() {
      return Map.of(
         Direction.NORTH,
         Block.box(0.0, 0.0, 13.0, 16.0, 16.0, 15.0),
         Direction.SOUTH,
         Block.box(0.0, 0.0, 1.0, 16.0, 16.0, 3.0),
         Direction.EAST,
         Block.box(1.0, 0.0, 0.0, 3.0, 16.0, 16.0),
         Direction.WEST,
         Block.box(13.0, 0.0, 0.0, 15.0, 16.0, 16.0)
      );
   }

   private static Map<Direction, VoxelShape> tribalChairLowerShapes() {
      return Map.of(
         Direction.NORTH,
         Block.box(1.0, 0.0, 0.0, 15.0, 16.0, 15.0),
         Direction.SOUTH,
         Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 16.0),
         Direction.EAST,
         Block.box(1.0, 0.0, 1.0, 16.0, 16.0, 15.0),
         Direction.WEST,
         Block.box(0.0, 0.0, 1.0, 15.0, 16.0, 15.0)
      );
   }

   private static Map<Direction, VoxelShape> tribalChairUpperShapes() {
      return Map.of(
         Direction.NORTH,
         Block.box(1.0, 0.0, 12.0, 15.0, 14.0, 16.0),
         Direction.SOUTH,
         Block.box(1.0, 0.0, 0.0, 15.0, 14.0, 4.0),
         Direction.EAST,
         Block.box(0.0, 0.0, 1.0, 4.0, 14.0, 15.0),
         Direction.WEST,
         Block.box(12.0, 0.0, 1.0, 16.0, 14.0, 15.0)
      );
   }
}
