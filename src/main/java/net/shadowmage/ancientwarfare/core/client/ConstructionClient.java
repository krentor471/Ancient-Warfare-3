package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.shadowmage.ancientwarfare.core.item.ConstructionWandItem;

/** The builder's wand on the client: the corner marked first, the player's jobs as the server last sent them, and their boxes in the world. */
public final class ConstructionClient {
   /** A job as the client sees it; kind is ConstructionJobs.Kind's ordinal. */
   public static record JobView(int id, int kind, BlockPos min, BlockPos max, int done, int total, Component block) {
   }

   private static final float[][] COLORS = {{1.0F, 0.35F, 0.25F}, {1.0F, 0.85F, 0.2F}, {0.35F, 0.6F, 1.0F}, {0.4F, 1.0F, 0.45F}, {0.85F, 0.5F, 1.0F}};
   private static BlockPos corner;
   /** A box shown while it is being chosen: dragged in the building view or set in the work window. */
   private static BlockPos previewFrom;
   private static BlockPos previewTo;
   private static List<JobView> jobs = List.of();

   private ConstructionClient() {
   }

   public static List<JobView> jobs() {
      return jobs;
   }

   public static void setJobs(List<JobView> received) {
      if (received.equals(jobs)) {
         return;
      }

      jobs = received;
      if (Minecraft.getInstance().screen instanceof ConstructionScreen screen) {
         screen.jobsChanged();
      }
   }

   public static void setPreview(BlockPos from, BlockPos to) {
      previewFrom = from;
      previewTo = to;
   }

   public static void click(BlockPos pos, boolean shift) {
      Minecraft minecraft = Minecraft.getInstance();
      if (shift) {
         corner = null;
         minecraft.player.displayClientMessage(Component.translatable("construction.ancientwarfare.cleared"), true);
      } else if (corner == null) {
         corner = pos.immutable();
         minecraft.player.displayClientMessage(Component.translatable("construction.ancientwarfare.corner", pos.getX(), pos.getY(), pos.getZ()), true);
      } else {
         BlockPos first = corner;
         corner = null;
         minecraft.setScreen(new ConstructionScreen(first, pos.immutable()));
      }
   }

   public static void clickAir(boolean shift) {
      if (shift || corner != null) {
         corner = null;
         Minecraft.getInstance().player.displayClientMessage(Component.translatable("construction.ancientwarfare.cleared"), true);
      } else {
         Minecraft.getInstance().setScreen(new ConstructionScreen(null, null));
      }
   }

   private static boolean holdingWand(Minecraft minecraft) {
      return minecraft.player.getMainHandItem().getItem() instanceof ConstructionWandItem
         || minecraft.player.getOffhandItem().getItem() instanceof ConstructionWandItem;
   }

   /** The box being marked, from the first corner to the block under the cursor, and the jobs under way. */
   public static void render(PoseStack poseStack, VertexConsumer consumer, Minecraft minecraft) {
      if (!holdingWand(minecraft)) {
         return;
      }

      if (previewFrom != null && previewTo != null) {
         AABB box = new AABB(previewFrom).minmax(new AABB(previewTo)).inflate(0.03);
         LevelRenderer.renderLineBox(poseStack, consumer, box, 1.0F, 1.0F, 1.0F, 1.0F);
      } else if (corner != null && minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
         BlockPos other = hit.getBlockPos();
         AABB box = new AABB(corner).minmax(new AABB(other)).inflate(0.02);
         LevelRenderer.renderLineBox(poseStack, consumer, box, 1.0F, 1.0F, 1.0F, 1.0F);
      } else if (corner != null) {
         LevelRenderer.renderLineBox(poseStack, consumer, new AABB(corner).inflate(0.02), 1.0F, 1.0F, 1.0F, 1.0F);
      }

      for (JobView job : jobs) {
         float[] color = COLORS[Math.floorMod(job.kind(), COLORS.length)];
         AABB box = new AABB(job.min()).minmax(new AABB(job.max())).inflate(0.01);
         LevelRenderer.renderLineBox(poseStack, consumer, box, color[0], color[1], color[2], 0.9F);
      }
   }
}
