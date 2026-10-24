package net.shadowmage.ancientwarfare.core.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder.SortState;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.RenderShape;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.structure.DraftingPreviewData;
import net.shadowmage.ancientwarfare.core.menu.DraftingStationMenu;
import net.shadowmage.ancientwarfare.core.structure.AwsTemplatePlacer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

public final class DraftingBuildingPreview implements AutoCloseable {
   private static final int BLOCKS_PER_FRAME = 256;
   private static final long BUILD_NANOS_PER_FRAME = 4000000L;
   private static final int MAX_VERTICES = 1048576;
   private static final int MAX_RENDER_LAYERS = 8;
   private static final float DEFAULT_YAW = -35.0F;
   private static final float DEFAULT_PITCH = 24.0F;
   private static final float MIN_ZOOM = 0.35F;
   private static final float MAX_ZOOM = 2.5F;
   private static volatile int resourceGeneration;
   private static final ThreadPoolExecutor LOADER = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1), runnable -> {
      Thread thread = new Thread(runnable, "AW drafting preview");
      thread.setDaemon(true);
      return thread;
   }, new AbortPolicy());
   private final DraftingStationMenu menu;
   private final Map<String, DraftingPreviewData> localData = new LinkedHashMap<>();
   private final List<DraftingBuildingPreview.Mesh> opaque = new ArrayList<>();
   private final List<DraftingBuildingPreview.Mesh> translucent = new ArrayList<>();
   private final Map<RenderType, DraftingBuildingPreview.LayerBuilder> opaqueBuilders = new LinkedHashMap<>();
   private final Map<RenderType, DraftingBuildingPreview.LayerBuilder> transparentBuilders = new LinkedHashMap<>();
   private DraftingPreviewData data;
   private CompletableFuture<DraftingPreviewData> loading;
   private int template = -1;
   private String templateId = "";
   private int cursor;
   private int vertices;
   private int loadedGeneration;
   private long loadingDeadline;
   private int left;
   private int top;
   private int width;
   private int height;
   private float yaw;
   private float pitch;
   private float zoom;
   private boolean dragging;
   private boolean unavailable;
   private boolean closed;

   public DraftingBuildingPreview(DraftingStationMenu menu) {
      this.loadedGeneration = resourceGeneration;
      this.yaw = -35.0F;
      this.pitch = 24.0F;
      this.zoom = 1.0F;
      this.menu = menu;
   }

   public static void invalidateResources() {
      resourceGeneration++;
   }

   public void setTemplate(int index) {
      if (!this.closed && this.template != index) {
         this.cancelLoading();
         this.releaseMeshes();
         this.template = index;
         this.templateId = this.menu.templateId(index);
         this.data = null;
         this.unavailable = false;
         this.dragging = false;
         if (index >= 0 && index < this.menu.templateCount()) {
            if (this.templateId.startsWith("aws:")) {
               this.data = this.localData.get(this.templateId);
               if (this.data == null) {
                  this.startBuiltInLoad();
               }
            } else {
               this.data = this.menu.preview(index);
               if (this.data == null) {
                  this.menu.requestPreview(index);
               }
            }
         }
      }
   }

   private static DraftingPreviewData loadBuiltIn(String path) {
      AwsTemplatePlacer.PreviewData preview = AwsTemplatePlacer.preview(path, BlockPos.ZERO, Direction.NORTH);
      if (preview.blocks().size() > 1048576) {
         throw new IllegalArgumentException("Drafting preview source too large");
      } else {
         List<DraftingPreviewData.Cell> cells = new ArrayList<>(preview.blocks().size());

         for (AwsTemplatePlacer.PreviewBlock block : preview.blocks()) {
            cells.add(new DraftingPreviewData.Cell(block.pos(), block.state()));
         }

         return DraftingPreviewData.fromCells(cells);
      }
   }

   private void startBuiltInLoad() {
      try {
         String path = this.templateId.substring(4);
         this.loading = CompletableFuture.supplyAsync(() -> loadBuiltIn(path), LOADER);
         this.loadingDeadline = System.nanoTime() + 15000000000L;
      } catch (RejectedExecutionException var2) {
         this.loading = null;
      }
   }

   private void pollData() {
      if (this.loading != null && !this.loading.isDone() && System.nanoTime() - this.loadingDeadline >= 0L) {
         this.cancelLoading();
         this.unavailable = true;
      }

      if (this.loading != null && this.loading.isDone()) {
         try {
            this.data = this.loading.join();
            if (this.localData.size() >= 4) {
               this.localData.remove(this.localData.keySet().iterator().next());
            }

            this.localData.put(this.templateId, this.data);
         } catch (RuntimeException var2) {
            this.unavailable = true;
            AncientWarfareCore.LOG.warn("Unable to load drafting preview {}", this.templateId, var2);
         }

         this.loading = null;
      } else if (this.data == null && this.loading == null && !this.unavailable && this.templateId.startsWith("aws:")) {
         this.startBuiltInLoad();
      } else if (this.data == null && !this.templateId.startsWith("aws:") && this.template >= 0) {
         this.data = this.menu.preview(this.template);
         if (this.data == null && !this.unavailable) {
            this.menu.requestPreview(this.template);
         }
      }
   }

   public boolean isLoading() {
      return !this.closed
         && !this.unavailable
         && this.template >= 0
         && this.template < this.menu.templateCount()
         && (this.data == null || this.cursor < this.data.cells().size());
   }

   public boolean isUnavailable() {
      return this.unavailable;
   }

   public boolean simplifiedStatus() {
      return this.data != null && this.data.simplified();
   }

   public Component dimensions() {
      return this.data != null && !this.data.cells().isEmpty()
         ? Component.translatable("gui.ancientwarfare.drafting.preview_size", new Object[]{this.data.sizeX(), this.data.sizeY(), this.data.sizeZ()})
         : Component.empty();
   }

   public void resetView() {
      this.yaw = -35.0F;
      this.pitch = 24.0F;
      this.zoom = 1.0F;
      this.dragging = false;
   }

   public boolean mouseClicked(double x, double y, int button) {
      if (!this.closed && button == 0 && insideViewport(x, y, this.left, this.top, this.width, this.height)) {
         this.dragging = true;
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseDragged(double x, double y, int button, double dragX, double dragY) {
      if (!this.closed && this.dragging && button == 0) {
         this.yaw = normalizeYaw(this.yaw + (float)dragX * 0.8F);
         this.pitch = clampPitch(this.pitch + (float)dragY * 0.65F);
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseReleased(double x, double y, int button) {
      if (button == 0 && this.dragging) {
         this.dragging = false;
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double x, double y, double delta) {
      if (!this.closed && insideViewport(x, y, this.left, this.top, this.width, this.height)) {
         this.zoom = clampZoom(this.zoom * (float)Math.pow(1.15, Math.max(-8.0, Math.min(8.0, delta))));
         return true;
      } else {
         return false;
      }
   }

   public void render(GuiGraphics graphics, int left, int top, int width, int height, int mouseX, int mouseY, float partialTick) {
      this.left = left;
      this.top = top;
      this.width = width;
      this.height = height;
      if (!this.closed && width > 0 && height > 0) {
         if (this.loadedGeneration != resourceGeneration) {
            this.releaseMeshes();
            this.loadedGeneration = resourceGeneration;
         }

         this.pollData();
         if (this.data != null && !this.unavailable && this.cursor < this.data.cells().size()) {
            this.buildNextBatch();
         }

         if (this.opaque.isEmpty() && this.translucent.isEmpty()) {
            String key = this.unavailable ? "preview_unavailable" : (this.isLoading() ? "preview_loading" : "preview_empty");
            graphics.drawCenteredString(
               Minecraft.getInstance().font, Component.translatable("gui.ancientwarfare.drafting." + key), left + width / 2, top + height / 2 - 4, -2039584
            );
         } else {
            graphics.flush();
            DraftingBuildingPreview.GlState previous = new DraftingBuildingPreview.GlState();
            PoseStack pose = graphics.pose();
            graphics.enableScissor(left, top, left + width, top + height);
            pose.pushPose();

            try {
               RenderSystem.depthMask(true);
               RenderSystem.clearDepth(1.0);
               RenderSystem.clear(256, Minecraft.ON_OSX);
               RenderSystem.enableDepthTest();
               RenderSystem.depthFunc(515);
               RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
               Lighting.setupFor3DItems();
               DraftingBuildingPreview.ViewFit fit = fit(
                  this.data.sizeX(), this.data.sizeY(), this.data.sizeZ(), this.yaw, this.pitch, width, height, this.zoom
               );
               pose.translate((double)left + (double)width * 0.5, (double)top + (double)height * 0.5, 400.0);
               pose.scale((float)fit.scale(), (float)(-fit.scale()), (float)fit.scale());
               pose.mulPose(Axis.XP.rotationDegrees(this.pitch));
               pose.mulPose(Axis.YP.rotationDegrees(this.yaw));
               pose.translate((double)(-this.data.sizeX()) * 0.5, (double)(-this.data.sizeY()) * 0.5, (double)(-this.data.sizeZ()) * 0.5);

               for (DraftingBuildingPreview.Mesh mesh : this.opaque) {
                  mesh.draw(pose.last().pose());
               }

               Vector3f direction = cameraDirection(this.yaw, this.pitch);

               for (DraftingBuildingPreview.Mesh mesh : this.translucent) {
                  mesh.sort(direction, this.yaw, this.pitch);
                  mesh.draw(pose.last().pose());
               }
            } catch (RuntimeException var18) {
               this.failPreview(var18);
            } finally {
               VertexBuffer.unbind();
               pose.popPose();
               RenderSystem.depthMask(true);
               RenderSystem.clear(256, Minecraft.ON_OSX);
               graphics.disableScissor();
               Lighting.setupFor3DItems();
               previous.restore();
            }
         }
      }
   }

   private void buildNextBatch() {
      long deadline = System.nanoTime() + 4000000L;
      int end = Math.min(this.data.cells().size(), (this.cursor / 256 + 1) * 256);
      PoseStack blockPose = new PoseStack();
      MultiBufferSource target = requestedType -> {
         boolean transparent = isTranslucent(requestedType);
         RenderType type = transparent ? RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS) : requestedType;
         Map<RenderType, DraftingBuildingPreview.LayerBuilder> collection = transparent ? this.transparentBuilders : this.opaqueBuilders;
         if (collection.size() >= 8 && !collection.containsKey(type)) {
            throw new IllegalStateException("Too many drafting preview render layers");
         } else {
            return collection.computeIfAbsent(type, key -> new DraftingBuildingPreview.LayerBuilder(key)).consumer;
         }
      };

      try {
         while (this.cursor < end) {
            DraftingPreviewData.Cell cell = this.data.cells().get(this.cursor++);
            if (cell.state().getRenderShape() == RenderShape.MODEL) {
               blockPose.pushPose();

               try {
                  blockPose.translate((float)cell.pos().getX(), (float)cell.pos().getY(), (float)cell.pos().getZ());
                  Minecraft.getInstance().getBlockRenderer().renderSingleBlock(cell.state(), blockPose, target, 15728880, OverlayTexture.NO_OVERLAY);
               } finally {
                  blockPose.popPose();
               }
            }

            if (System.nanoTime() >= deadline) {
               break;
            }
         }

         if (this.cursor == end) {
            for (DraftingBuildingPreview.LayerBuilder layer : this.opaqueBuilders.values()) {
               DraftingBuildingPreview.Mesh mesh = layer.finish(false);
               if (mesh != null) {
                  this.opaque.add(mesh);
               }
            }

            this.opaqueBuilders.clear();
         }

         if (this.cursor == this.data.cells().size()) {
            for (DraftingBuildingPreview.LayerBuilder layerx : this.transparentBuilders.values()) {
               DraftingBuildingPreview.Mesh mesh = layerx.finish(true);
               if (mesh != null) {
                  this.translucent.add(mesh);
               }
            }

            this.transparentBuilders.clear();
            if (!this.data.cells().isEmpty() && this.opaque.isEmpty() && this.translucent.isEmpty()) {
               this.unavailable = true;
            }
         }
      } catch (RuntimeException var17) {
         this.failPreview(var17);
      } finally {
         VertexBuffer.unbind();
      }
   }

   private static boolean isTranslucent(RenderType type) {
      return type == RenderType.translucent()
         || type == RenderType.translucentMovingBlock()
         || type == RenderType.translucentNoCrumbling()
         || type == Sheets.translucentCullBlockSheet()
         || type == Sheets.translucentItemSheet()
         || type == RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS)
         || type == RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS)
         || type == RenderType.itemEntityTranslucentCull(TextureAtlas.LOCATION_BLOCKS);
   }

   private void failPreview(RuntimeException exception) {
      this.unavailable = true;
      this.releaseMeshes();
      AncientWarfareCore.LOG.warn("Unable to render drafting preview {}", this.templateId, exception);
   }

   private void cancelLoading() {
      if (this.loading != null) {
         this.loading.cancel(false);
      }

      this.loading = null;
   }

   private void releaseMeshes() {
      this.opaque.forEach(DraftingBuildingPreview.Mesh::close);
      this.translucent.forEach(DraftingBuildingPreview.Mesh::close);
      this.opaque.clear();
      this.translucent.clear();
      this.opaqueBuilders.values().forEach(DraftingBuildingPreview.LayerBuilder::discard);
      this.opaqueBuilders.clear();
      this.transparentBuilders.values().forEach(DraftingBuildingPreview.LayerBuilder::discard);
      this.transparentBuilders.clear();
      this.cursor = 0;
      this.vertices = 0;
   }

   @Override
   public void close() {
      if (!this.closed) {
         this.closed = true;
         this.dragging = false;
         this.cancelLoading();
         this.releaseMeshes();
         this.localData.clear();
         this.data = null;
      }
   }

   public static boolean insideViewport(double x, double y, int left, int top, int width, int height) {
      return width > 0 && height > 0 && x >= (double)left && x < (double)left + (double)width && y >= (double)top && y < (double)top + (double)height;
   }

   public static float clampPitch(float value) {
      return Float.isFinite(value) ? Math.max(-80.0F, Math.min(80.0F, value)) : 24.0F;
   }

   public static float clampZoom(float value) {
      return Float.isFinite(value) ? Math.max(0.35F, Math.min(2.5F, value)) : 1.0F;
   }

   public static float normalizeYaw(float value) {
      return Float.isFinite(value) ? (value % 360.0F + 360.0F) % 360.0F : -35.0F;
   }

   public static DraftingBuildingPreview.ViewFit fit(int sizeX, int sizeY, int sizeZ, float yaw, float pitch, int width, int height, float zoom) {
      yaw = normalizeYaw(yaw);
      pitch = clampPitch(pitch);
      double sy = Math.sin(Math.toRadians((double)yaw));
      double cy = Math.cos(Math.toRadians((double)yaw));
      double sp = Math.sin(Math.toRadians((double)pitch));
      double cp = Math.cos(Math.toRadians((double)pitch));
      double maxX = 0.0;
      double maxY = 0.0;
      double maxZ = 0.0;

      for (int x = 0; x < 2; x++) {
         for (int y = 0; y < 2; y++) {
            for (int z = 0; z < 2; z++) {
               double px = ((double)x - 0.5) * (double)Math.max(1, sizeX);
               double py = ((double)y - 0.5) * (double)Math.max(1, sizeY);
               double pz = ((double)z - 0.5) * (double)Math.max(1, sizeZ);
               double rx = cy * px + sy * pz;
               double rz = -sy * px + cy * pz;
               maxX = Math.max(maxX, Math.abs(rx));
               maxY = Math.max(maxY, Math.abs(cp * py - sp * rz));
               maxZ = Math.max(maxZ, Math.abs(sp * py + cp * rz));
            }
         }
      }

      double scale = 0.9
         * Math.min((double)Math.max(1, width) / Math.max(1.0, maxX * 2.0), (double)Math.max(1, height) / Math.max(1.0, maxY * 2.0))
         * (double)clampZoom(zoom);
      scale = Math.min(scale, 512.0 / Math.max(1.0, maxZ * 2.0));
      return new DraftingBuildingPreview.ViewFit(scale, maxX * 2.0 * scale, maxY * 2.0 * scale, maxZ * 2.0 * scale);
   }

   private static Vector3f cameraDirection(float yaw, float pitch) {
      double y = Math.toRadians((double)yaw);
      double p = Math.toRadians((double)pitch);
      return new Vector3f((float)(-Math.sin(y) * Math.cos(p)), (float)Math.sin(p), (float)(Math.cos(y) * Math.cos(p)));
   }

   private final class BoundedConsumer implements VertexConsumer {
      private final VertexConsumer delegate;

      private BoundedConsumer(VertexConsumer delegate) {
         this.delegate = delegate;
      }

      public VertexConsumer vertex(double x, double y, double z) {
         this.delegate.vertex(x, y, z);
         return this;
      }

      public VertexConsumer color(int r, int g, int b, int a) {
         this.delegate.color(r, g, b, a);
         return this;
      }

      public VertexConsumer uv(float u, float v) {
         this.delegate.uv(u, v);
         return this;
      }

      public VertexConsumer overlayCoords(int u, int v) {
         this.delegate.overlayCoords(u, v);
         return this;
      }

      public VertexConsumer uv2(int u, int v) {
         this.delegate.uv2(u, v);
         return this;
      }

      public VertexConsumer normal(float x, float y, float z) {
         this.delegate.normal(x, y, z);
         return this;
      }

      public void endVertex() {
         if (++DraftingBuildingPreview.this.vertices > 1048576) {
            throw new IllegalStateException("Drafting preview vertex limit exceeded");
         } else {
            this.delegate.endVertex();
         }
      }

      public void defaultColor(int r, int g, int b, int a) {
         this.delegate.defaultColor(r, g, b, a);
      }

      public void unsetDefaultColor() {
         this.delegate.unsetDefaultColor();
      }
   }

   private static final class GlState {
      private final boolean depth = GL11.glIsEnabled(2929);
      private final boolean cull = GL11.glIsEnabled(2884);
      private final boolean blend = GL11.glIsEnabled(3042);
      private final boolean depthMask = GL11.glGetBoolean(2930);
      private final double clearDepth = GL11.glGetDouble(2931);
      private final int depthFunc = GL11.glGetInteger(2932);
      private final int srcRgb = GL11.glGetInteger(32969);
      private final int dstRgb = GL11.glGetInteger(32968);
      private final int srcAlpha = GL11.glGetInteger(32971);
      private final int dstAlpha = GL11.glGetInteger(32970);
      private final float[] color = (float[])RenderSystem.getShaderColor().clone();
      private final ShaderInstance shader = RenderSystem.getShader();
      private final int texture0 = RenderSystem.getShaderTexture(0);
      private final int texture1 = RenderSystem.getShaderTexture(1);
      private final int texture2 = RenderSystem.getShaderTexture(2);

      private void restore() {
         if (this.depth) {
            RenderSystem.enableDepthTest();
         } else {
            RenderSystem.disableDepthTest();
         }

         if (this.cull) {
            RenderSystem.enableCull();
         } else {
            RenderSystem.disableCull();
         }

         if (this.blend) {
            RenderSystem.enableBlend();
         } else {
            RenderSystem.disableBlend();
         }

         RenderSystem.depthMask(this.depthMask);
         RenderSystem.clearDepth(this.clearDepth);
         RenderSystem.depthFunc(this.depthFunc);
         RenderSystem.blendFuncSeparate(this.srcRgb, this.dstRgb, this.srcAlpha, this.dstAlpha);
         RenderSystem.setShaderColor(this.color[0], this.color[1], this.color[2], this.color[3]);
         RenderSystem.setShader(() -> this.shader);
         RenderSystem.setShaderTexture(0, this.texture0);
         RenderSystem.setShaderTexture(1, this.texture1);
         RenderSystem.setShaderTexture(2, this.texture2);
      }
   }

   private final class LayerBuilder {
      private final RenderType type;
      private final BufferBuilder builder = new BufferBuilder(4096);
      private final VertexConsumer consumer;

      private LayerBuilder(RenderType type) {
         this.type = type;
         if (type.mode() != Mode.QUADS) {
            throw new IllegalStateException("Non-quad block model");
         } else {
            this.builder.begin(type.mode(), type.format());
            this.consumer = DraftingBuildingPreview.this.new BoundedConsumer(this.builder);
         }
      }

      private DraftingBuildingPreview.Mesh finish(boolean transparent) {
         if (this.builder.isCurrentBatchEmpty()) {
            this.builder.endOrDiscardIfEmpty();
            return null;
         } else {
            SortState sortState = null;
            if (transparent) {
               Vector3f direction = DraftingBuildingPreview.cameraDirection(DraftingBuildingPreview.this.yaw, DraftingBuildingPreview.this.pitch);
               this.builder.setQuadSorting(VertexSorting.byDistance(point -> -point.dot(direction)));
               sortState = this.builder.getSortState();
            }

            RenderedBuffer rendered = this.builder.end();
            VertexBuffer buffer = null;
            boolean handedToUpload = false;

            DraftingBuildingPreview.Mesh exception;
            try {
               buffer = new VertexBuffer(Usage.STATIC);
               buffer.bind();
               handedToUpload = true;
               buffer.upload(rendered);
               exception = new DraftingBuildingPreview.Mesh(this.type, buffer, sortState);
            } catch (RuntimeException var10) {
               if (buffer != null) {
                  buffer.close();
               }

               throw var10;
            } finally {
               if (!handedToUpload) {
                  rendered.release();
               }
            }

            return exception;
         }
      }

      private void discard() {
         this.builder.discard();
      }
   }

   private static final class Mesh implements AutoCloseable {
      private final RenderType type;
      private final VertexBuffer buffer;
      private final SortState sortState;
      private final BufferBuilder indices;
      private float sortedYaw = Float.NaN;
      private float sortedPitch = Float.NaN;

      private Mesh(RenderType type, VertexBuffer buffer, SortState sortState) {
         this.type = type;
         this.buffer = buffer;
         this.sortState = sortState;
         this.indices = sortState == null ? null : new BufferBuilder(4096);
      }

      private void sort(Vector3f direction, float yaw, float pitch) {
         if (this.sortState != null && (this.sortedYaw != yaw || this.sortedPitch != pitch)) {
            this.indices.begin(this.type.mode(), this.type.format());
            this.indices.restoreSortState(this.sortState);
            this.indices.setQuadSorting(VertexSorting.byDistance(point -> -point.dot(direction)));
            RenderedBuffer rendered = this.indices.end();
            boolean handedToUpload = false;

            try {
               this.buffer.bind();
               handedToUpload = true;
               this.buffer.upload(rendered);
            } finally {
               if (!handedToUpload) {
                  rendered.release();
               }
            }

            this.sortedYaw = yaw;
            this.sortedPitch = pitch;
         }
      }

      private void draw(Matrix4f pose) {
         this.type.setupRenderState();

         try {
            this.buffer.bind();
            Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose);
            this.buffer.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
         } finally {
            this.type.clearRenderState();
         }
      }

      @Override
      public void close() {
         this.buffer.close();
      }
   }

   public static record ViewFit(double scale, double projectedWidth, double projectedHeight, double projectedDepth) {
   }
}
