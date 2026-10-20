package net.shadowmage.ancientwarfare.core.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.shadowmage.ancientwarfare.core.AncientWarfareCore;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreIds;
import net.shadowmage.ancientwarfare.core.common.registry.AWCoreRegistryAccess;
import net.shadowmage.ancientwarfare.core.common.structure.DraftingPreviewData;
import net.shadowmage.ancientwarfare.core.structure.AwsTemplatePlacer;
import net.shadowmage.ancientwarfare.core.structure.PlayerStructureTemplateData;
import net.shadowmage.ancientwarfare.core.tile.DraftingStationBlockEntity;

public class DraftingStationMenu extends AbstractContainerMenu {
   private static volatile List<DraftingStationMenu.TemplateSummary> CACHED_BUILT_IN_TEMPLATES;
   private final BlockPos pos;
   private final DraftingStationBlockEntity station;
   private final List<DraftingStationMenu.TemplateSummary> templateSummaries;
   private final List<List<Integer>> suppliedCounts;
   private String selectedTemplateId;
   private final Player menuPlayer;
   private static final AtomicInteger PREVIEW_REQUEST_IDS = new AtomicInteger();
   private static final long PREVIEW_TIMEOUT_NANOS = 10000000000L;
   private final Map<Integer, DraftingPreviewData> previewCache = new LinkedHashMap<>();
   private int pendingPreviewIndex = -1;
   private int pendingPreviewRequest;
   private long pendingPreviewDeadline;
   private long pendingPreviewSentNanos;
   private long lastPreviewRequestNanos;
   private List<DraftingPreviewData> pendingPreviewChunks;
   private long lastServerPreviewTick = Long.MIN_VALUE;
   private int activeServerPreviewRequest;
   private int activeServerPreviewIndex = -1;
   private boolean serverPreviewInFlight;
   private PlayerStructureTemplateData.PlayerTemplate cachedServerTemplate;
   private DraftingPreviewData cachedServerPreview;
   private boolean previewFailureLogged;
   private static final ThreadPoolExecutor PREVIEW_EXECUTOR = new ThreadPoolExecutor(
      1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1), runnable -> {
         Thread thread = new Thread(runnable, "AW drafting server preview");
         thread.setDaemon(true);
         return thread;
      }, new AbortPolicy()
   );

   public DraftingStationMenu(int id, Inventory inventory, BlockPos pos) {
      this(
         id,
         inventory,
         pos,
         inventory.f_35978_.m_9236_() instanceof ServerLevel level ? serverTemplateSummaries(level) : List.of(),
         inventory.f_35978_.m_9236_().m_7702_(pos) instanceof DraftingStationBlockEntity found ? found.selectedTemplate() : "",
         List.of()
      );
   }

   public DraftingStationMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
      this(id, inventory, readClientData(buffer));
   }

   private DraftingStationMenu(int id, Inventory inventory, DraftingStationMenu.ClientData data) {
      this(id, inventory, data.pos(), data.templates(), data.selectedTemplate(), data.suppliedCounts());
   }

   private DraftingStationMenu(
      int id,
      Inventory inventory,
      BlockPos pos,
      List<DraftingStationMenu.TemplateSummary> templateSummaries,
      String selectedTemplateId,
      List<List<Integer>> suppliedCounts
   ) {
      super(AWCoreRegistryAccess.menu(AWCoreIds.DRAFTING_STATION), id);
      this.pos = pos;
      this.menuPlayer = inventory.f_35978_;
      this.templateSummaries = templateSummaries;
      this.suppliedCounts = suppliedCounts;
      this.station = inventory.f_35978_.m_9236_().m_7702_(pos) instanceof DraftingStationBlockEntity found ? found : null;
      this.selectedTemplateId = !inventory.f_35978_.m_9236_().f_46443_ && this.station != null ? this.station.selectedTemplate() : selectedTemplateId;
      Container container = (Container)(this.station == null ? new SimpleContainer(28) : this.station);

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.m_38897_(new Slot(container, 0 + col + row * 9, 8 + col * 18, 94 + row * 18));
         }
      }

      this.m_38897_(new Slot(container, 27, 80, 60) {
         public boolean m_5857_(ItemStack stack) {
            return false;
         }
      });

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.m_38897_(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 156 + row * 18));
         }
      }

      for (int col = 0; col < 9; col++) {
         this.m_38897_(new Slot(inventory, col, 8 + col * 18, 214));
      }
   }

   public BlockPos pos() {
      return this.pos;
   }

   public String selectedTemplate() {
      return this.selectedTemplateId;
   }

   public int selectedTemplateIndex() {
      String selected = this.selectedTemplate();

      for (int i = 0; i < this.templateSummaries.size(); i++) {
         if (this.templateSummaries.get(i).id().equals(selected)) {
            return i;
         }
      }

      return 0;
   }

   public List<String> templateNames() {
      return this.templateSummaries.stream().map(DraftingStationMenu.TemplateSummary::label).toList();
   }

   public boolean isBuiltInTemplate(int index) {
      return index >= 0 && index < this.templateSummaries.size() && this.templateSummaries.get(index).id().startsWith("aws:");
   }

   public int templateCount() {
      return this.templateSummaries.size();
   }

   public String templateId(int index) {
      return index >= 0 && index < this.templateSummaries.size() ? this.templateSummaries.get(index).id() : "";
   }

   public void requestPreview(int index) {
      if (this.menuPlayer.m_9236_().f_46443_
         && this.menuPlayer.f_36096_ == this
         && index >= 0
         && index < this.templateCount()
         && !this.isBuiltInTemplate(index)) {
         this.expirePreviewRequest();
         long now = System.nanoTime();
         if (!this.previewCache.containsKey(index)) {
            if (this.pendingPreviewIndex == index) {
               if (this.pendingPreviewChunks == null && now - this.pendingPreviewSentNanos >= 1000000000L) {
                  this.pendingPreviewSentNanos = now;
                  AWCorePlatform.requestDraftingPreview(this.f_38840_, index, this.pendingPreviewRequest);
               }
            } else if (this.lastPreviewRequestNanos == 0L || now - this.lastPreviewRequestNanos >= 250000000L) {
               this.lastPreviewRequestNanos = now;
               this.pendingPreviewIndex = index;
               this.pendingPreviewRequest = PREVIEW_REQUEST_IDS.updateAndGet(value -> value == Integer.MAX_VALUE ? 1 : value + 1);
               this.pendingPreviewDeadline = now + 10000000000L;
               this.pendingPreviewSentNanos = now;
               this.pendingPreviewChunks = null;
               AWCorePlatform.requestDraftingPreview(this.f_38840_, index, this.pendingPreviewRequest);
            }
         }
      }
   }

   public DraftingPreviewData preview(int index) {
      this.expirePreviewRequest();
      return this.previewCache.get(index);
   }

   private void expirePreviewRequest() {
      if (this.pendingPreviewIndex >= 0 && System.nanoTime() - this.pendingPreviewDeadline >= 0L) {
         this.cachePreview(this.pendingPreviewIndex, DraftingPreviewData.empty());
         this.clearPendingPreview();
      }
   }

   private void clearPendingPreview() {
      this.pendingPreviewIndex = -1;
      this.pendingPreviewChunks = null;
   }

   private void cachePreview(int index, DraftingPreviewData preview) {
      if (!this.previewCache.containsKey(index) && this.previewCache.size() >= 8) {
         this.previewCache.remove(this.previewCache.keySet().iterator().next());
      }

      this.previewCache.put(index, preview);
   }

   public void acceptPreview(int packetContainerId, int index, int requestId, int chunkIndex, int chunkCount, DraftingPreviewData chunk) {
      this.expirePreviewRequest();
      if (this.menuPlayer.m_9236_().f_46443_
         && this.menuPlayer.f_36096_ == this
         && packetContainerId == this.f_38840_
         && index == this.pendingPreviewIndex
         && requestId == this.pendingPreviewRequest
         && chunkCount >= 1
         && chunkCount <= 32
         && chunkIndex >= 0
         && chunkIndex < chunkCount
         && chunk.cells().size() <= 1024) {
         if (this.pendingPreviewChunks == null) {
            this.pendingPreviewChunks = new ArrayList<>(Collections.nCopies(chunkCount, null));
         }

         if (this.pendingPreviewChunks.size() == chunkCount && this.pendingPreviewChunks.get(chunkIndex) == null) {
            for (DraftingPreviewData existing : this.pendingPreviewChunks) {
               if (existing != null
                  && (
                     existing.sizeX() != chunk.sizeX()
                        || existing.sizeY() != chunk.sizeY()
                        || existing.sizeZ() != chunk.sizeZ()
                        || existing.simplified() != chunk.simplified()
                  )) {
                  return;
               }
            }

            this.pendingPreviewChunks.set(chunkIndex, chunk);
            if (!this.pendingPreviewChunks.stream().anyMatch(Objects::isNull)) {
               List<DraftingPreviewData.Cell> cells = new ArrayList<>();

               for (DraftingPreviewData part : this.pendingPreviewChunks) {
                  if (cells.size() + part.cells().size() > 32768) {
                     this.clearPendingPreview();
                     return;
                  }

                  cells.addAll(part.cells());
               }

               this.cachePreview(index, new DraftingPreviewData(chunk.sizeX(), chunk.sizeY(), chunk.sizeZ(), cells, chunk.simplified()));
               this.clearPendingPreview();
            }
         }
      }
   }

   public boolean requestServerPreview(Player player, int packetContainerId, int index, int requestId, Consumer<DraftingPreviewData> completed) {
      if (!(player.m_9236_() instanceof ServerLevel level) || !this.validServerRequest(player, packetContainerId, index, requestId, true)) {
         return false;
      }

      if (this.serverPreviewInFlight) {
         return false;
      } else {
         PlayerStructureTemplateData.PlayerTemplate template = PlayerStructureTemplateData.get(level).template(this.templateId(index)).orElse(null);
         if (template == null || template.blocks().size() > 1048576) {
            completed.accept(DraftingPreviewData.empty());
            return true;
         } else if (template == this.cachedServerTemplate && this.cachedServerPreview != null) {
            completed.accept(this.cachedServerPreview);
            return true;
         } else {
            List<PlayerStructureTemplateData.TemplateBlock> snapshot = List.copyOf(template.blocks());
            this.activeServerPreviewRequest = requestId;
            this.activeServerPreviewIndex = index;
            this.serverPreviewInFlight = true;
            MinecraftServer server = level.m_7654_();

            try {
               PREVIEW_EXECUTOR.execute(
                  () -> {
                     DraftingPreviewData result;
                     try {
                        result = DraftingPreviewData.fromCells(
                           snapshot.stream()
                              .filter(block -> !block.state().m_60795_())
                              .map(block -> new DraftingPreviewData.Cell(block.relativePos(), block.state()))
                              .toList()
                        );
                     } catch (RuntimeException var11x) {
                        result = DraftingPreviewData.empty();
                        if (!this.previewFailureLogged) {
                           this.previewFailureLogged = true;
                           AncientWarfareCore.LOG.warn("Unable to prepare drafting preview {} at {}", new Object[]{template.name(), this.pos, var11x});
                        }
                     }

                     DraftingPreviewData ready = result;
                     server.execute(() -> {
                        this.serverPreviewInFlight = false;
                        if (this.validServerRequest(player, packetContainerId, index, requestId, false)) {
                           if (PlayerStructureTemplateData.get((ServerLevel)player.m_9236_()).template(this.templateId(index)).orElse(null) != template) {
                              completed.accept(DraftingPreviewData.empty());
                           } else {
                              this.cachedServerTemplate = template;
                              this.cachedServerPreview = ready;
                              completed.accept(ready);
                           }
                        }
                     });
                  }
               );
            } catch (RejectedExecutionException var11) {
               this.serverPreviewInFlight = false;
               completed.accept(DraftingPreviewData.empty());
            }

            return true;
         }
      }
   }

   public boolean validServerRequest(Player player, int packetContainerId, int index, int requestId, boolean applyThrottle) {
      if (player == this.menuPlayer
         && player.f_36096_ == this
         && packetContainerId == this.f_38840_
         && !player.m_5833_()
         && this.m_6875_(player)
         && requestId >= 1
         && index >= 0
         && index < this.templateCount()
         && !this.isBuiltInTemplate(index)
         && (this.templateId(index).equals(this.selectedTemplateId) || this.selectedTemplateId.isEmpty() && index == this.selectedTemplateIndex())
         && player.m_9236_() instanceof ServerLevel level) {
         if (applyThrottle) {
            long now = level.m_46467_();
            if (this.lastServerPreviewTick != Long.MIN_VALUE && now - this.lastServerPreviewTick < 5L) {
               return false;
            }

            this.lastServerPreviewTick = now;
         }

         return !applyThrottle ? this.activeServerPreviewRequest == requestId && this.activeServerPreviewIndex == index : true;
      } else {
         return false;
      }
   }

   public void m_6877_(Player player) {
      super.m_6877_(player);
      this.clearPendingPreview();
      this.previewCache.clear();
      this.activeServerPreviewRequest = 0;
      this.activeServerPreviewIndex = -1;
      this.cachedServerPreview = null;
      this.cachedServerTemplate = null;
   }

   public List<DraftingStationMenu.MaterialSummary> materials(int index) {
      return index >= 0 && index < this.templateSummaries.size() ? this.templateSummaries.get(index).materials() : List.of();
   }

   public int remainingMaterialCount(int templateIndex, int materialIndex) {
      if (templateIndex >= 0 && templateIndex < this.templateSummaries.size()) {
         List<DraftingStationMenu.MaterialSummary> materials = this.templateSummaries.get(templateIndex).materials();
         if (materialIndex >= 0 && materialIndex < materials.size()) {
            int supplied = 0;
            if (this.station != null && this.templateSummaries.get(templateIndex).id().equals(this.station.selectedTemplate())) {
               supplied = this.station.suppliedCount(materials.get(materialIndex).itemId());
            } else if (templateIndex < this.suppliedCounts.size() && materialIndex < this.suppliedCounts.get(templateIndex).size()) {
               supplied = this.suppliedCounts.get(templateIndex).get(materialIndex);
            }

            return Math.max(0, materials.get(materialIndex).count() - supplied);
         } else {
            return 0;
         }
      } else {
         return 0;
      }
   }

   public boolean m_6366_(Player player, int id) {
      if (id == 1000) {
         if (this.station != null) {
            this.station.start();
         }

         return this.station != null;
      } else if (id >= 0 && id < this.templateSummaries.size()) {
         this.selectedTemplateId = this.templateSummaries.get(id).id();
         if (this.station != null) {
            this.station.selectTemplate(this.selectedTemplateId);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean m_6875_(Player player) {
      return m_38889_(ContainerLevelAccess.m_39289_(player.m_9236_(), this.pos), player, AWCoreRegistryAccess.block(AWCoreIds.DRAFTING_STATION));
   }

   public ItemStack m_7648_(Player player, int index) {
      Slot slot = index >= 0 && index < this.f_38839_.size() ? (Slot)this.f_38839_.get(index) : null;
      if (slot != null && slot.m_6657_()) {
         ItemStack stack = slot.m_7993_();
         ItemStack copy = stack.m_41777_();
         if (index < 28) {
            if (!this.m_38903_(stack, 28, this.f_38839_.size(), true)) {
               return ItemStack.f_41583_;
            }
         } else if (!this.m_38903_(stack, 0, 27, false)) {
            return ItemStack.f_41583_;
         }

         if (stack.m_41619_()) {
            slot.m_5852_(ItemStack.f_41583_);
         } else {
            slot.m_6654_();
         }

         slot.m_142406_(player, stack);
         return copy;
      } else {
         return ItemStack.f_41583_;
      }
   }

   public static List<DraftingStationMenu.TemplateSummary> serverTemplateSummaries(ServerLevel level) {
      ArrayList<DraftingStationMenu.TemplateSummary> result = new ArrayList<>();

      for (PlayerStructureTemplateData.PlayerTemplate template : PlayerStructureTemplateData.get(level).templates()) {
         result.add(
            new DraftingStationMenu.TemplateSummary(
               template.name(),
               template.name(),
               template.resources().stream().map(r -> new DraftingStationMenu.MaterialSummary(r.itemId(), r.count())).toList()
            )
         );
      }

      result.addAll(builtInTemplateSummaries());
      result.sort(Comparator.comparing(DraftingStationMenu.TemplateSummary::label, String.CASE_INSENSITIVE_ORDER));
      return List.copyOf(result);
   }

   private static List<DraftingStationMenu.TemplateSummary> builtInTemplateSummaries() {
      List<DraftingStationMenu.TemplateSummary> cached = CACHED_BUILT_IN_TEMPLATES;
      if (cached != null) {
         return cached;
      } else {
         synchronized (DraftingStationMenu.class) {
            cached = CACHED_BUILT_IN_TEMPLATES;
            if (cached == null) {
               ArrayList<DraftingStationMenu.TemplateSummary> builtIns = new ArrayList<>();

               for (AwsTemplatePlacer.TemplateInfo template : AwsTemplatePlacer.survivalTemplates()) {
                  builtIns.add(
                     new DraftingStationMenu.TemplateSummary(
                        "aws:" + template.path(),
                        template.name(),
                        AwsTemplatePlacer.templateResources(template.path())
                           .stream()
                           .map(r -> new DraftingStationMenu.MaterialSummary(r.itemId(), r.count()))
                           .toList()
                     )
                  );
               }

               cached = List.copyOf(builtIns);
               CACHED_BUILT_IN_TEMPLATES = cached;
            }

            return cached;
         }
      }
   }

   private static List<DraftingStationMenu.TemplateSummary> readNames(FriendlyByteBuf buffer) {
      int count = Math.min(1024, buffer.m_130242_());
      ArrayList<DraftingStationMenu.TemplateSummary> templates = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         String id = buffer.m_130136_(256);
         String label = buffer.m_130136_(128);
         int materialCount = Math.min(4096, buffer.m_130242_());
         ArrayList<DraftingStationMenu.MaterialSummary> materials = new ArrayList<>(materialCount);

         for (int j = 0; j < materialCount; j++) {
            Item item = (Item)BuiltInRegistries.f_257033_.m_7942_(buffer.m_130242_());
            materials.add(new DraftingStationMenu.MaterialSummary(BuiltInRegistries.f_257033_.m_7981_(item), buffer.m_130242_()));
         }

         templates.add(new DraftingStationMenu.TemplateSummary(id, label, materials));
      }

      return templates;
   }

   private static DraftingStationMenu.ClientData readClientData(FriendlyByteBuf buffer) {
      BlockPos pos = buffer.m_130135_();
      List<DraftingStationMenu.TemplateSummary> templates = readNames(buffer);
      String selected = buffer.m_130136_(256);
      int templateCount = Math.min(buffer.m_130242_(), templates.size());
      ArrayList<List<Integer>> supplied = new ArrayList<>(templates.size());

      for (int i = 0; i < templateCount; i++) {
         int count = Math.min(buffer.m_130242_(), templates.get(i).materials().size());
         ArrayList<Integer> values = new ArrayList<>(count);

         for (int j = 0; j < count; j++) {
            values.add(buffer.m_130242_());
         }

         supplied.add(values);
      }

      while (supplied.size() < templates.size()) {
         supplied.add(List.of());
      }

      return new DraftingStationMenu.ClientData(pos, templates, selected, supplied);
   }

   private static record ClientData(
      BlockPos pos, List<DraftingStationMenu.TemplateSummary> templates, String selectedTemplate, List<List<Integer>> suppliedCounts
   ) {
   }

   public static record MaterialSummary(ResourceLocation itemId, int count) {
      public ItemStack stack() {
         Item item = (Item)BuiltInRegistries.f_257033_.m_7745_(this.itemId);
         return item == Items.f_41852_ ? ItemStack.f_41583_ : new ItemStack(item);
      }
   }

   public static record TemplateSummary(String id, String label, List<DraftingStationMenu.MaterialSummary> materials) {
   }
}
