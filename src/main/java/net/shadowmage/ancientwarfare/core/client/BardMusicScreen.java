package net.shadowmage.ancientwarfare.core.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.shadowmage.ancientwarfare.core.common.npc.BardTrack;
import net.shadowmage.ancientwarfare.core.common.platform.AWCorePlatform;
import net.shadowmage.ancientwarfare.core.menu.BardMusicMenu;

public class BardMusicScreen extends LegacyContainerScreen<BardMusicMenu> {
   private static final int TEXT = -1;
   private static final int MUTED = -1513240;
   private final List<BardMusicScreen.MutableTrack> tracks = new ArrayList<>();
   private final List<EditBox> fields = new ArrayList<>();
   private int scrollOffset;
   private int expandedFileRow = -1;
   private List<String> fileChoices = List.of();
   private int fileScrollOffset;

   public BardMusicScreen(BardMusicMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title, 320, 240);

      for (BardTrack track : menu.getTracks()) {
         this.tracks.add(new BardMusicScreen.MutableTrack(track));
      }
   }

   @Override
   protected void m_7856_() {
      super.m_7856_();
      this.rebuildFields();
   }

   private void rebuildFields() {
      this.m_169413_();
      this.m_7522_(null);
      this.fields.clear();

      for (int row = 0; row < 2; row++) {
         int index = this.scrollOffset + row;
         if (index >= this.tracks.size()) {
            break;
         }

         BardMusicScreen.MutableTrack track = this.tracks.get(index);
         List<BardMusicScreenLayout.Rect> positions = BardMusicScreenLayout.fields(row);
         this.addTextField(positions.get(0), "file", track.fileName, value -> track.fileName = value);
         this.addTextField(positions.get(1), "name", track.displayName, value -> track.displayName = value);
         this.addNumberField(positions.get(2), "duration", track.durationSeconds, value -> track.durationSeconds = value);
         this.addNumberField(positions.get(3), "intro", track.introSilenceSeconds, value -> track.introSilenceSeconds = value);
         this.addNumberField(positions.get(4), "outro", track.outroSilenceSeconds, value -> track.outroSilenceSeconds = value);
         this.addNumberField(positions.get(5), "loops", track.loops, value -> track.loops = value);
         this.addButton(
            BardMusicScreenLayout.remove(row), Component.m_237113_("X"), Component.m_237115_("guistrings.npc.bard.remove_track"), () -> this.removeTrack(index)
         );
         this.addButton(
            BardMusicScreenLayout.fileButton(row),
            Component.m_237113_("..."),
            Component.m_237115_("guistrings.npc.bard.select_file"),
            () -> this.toggleFiles(index)
         );
      }

      LegacyTexturedButton add = this.addButton(BardMusicScreenLayout.ADD, Component.m_237115_("guistrings.npc.bard.add_track"), null, this::addTrack);
      add.f_93623_ = this.tracks.size() < 32;
      this.addButton(BardMusicScreenLayout.SAVE, Component.m_237115_("guistrings.npc.bard.save"), null, this::save);
      this.addButton(BardMusicScreenLayout.BACK, Component.m_237115_("guistrings.npc.back"), null, this::m_7379_);
   }

   private LegacyTexturedButton addButton(BardMusicScreenLayout.Rect rect, Component label, Component tooltip, Runnable action) {
      LegacyTexturedButton button = (LegacyTexturedButton)this.m_142416_(
         new LegacyTexturedButton(this.f_97735_ + rect.x(), this.f_97736_ + rect.y(), rect.width(), rect.height(), label, ignored -> action.run())
      );
      if (tooltip != null) {
         button.m_257544_(Tooltip.m_257550_(tooltip));
      }

      return button;
   }

   private EditBox input(BardMusicScreenLayout.Rect rect, String key, int maxLength, String value) {
      EditBox box = new EditBox(
         this.f_96547_,
         this.f_97735_ + rect.x() + 2,
         this.f_97736_ + rect.y() + 2,
         rect.width() - 4,
         rect.height() - 2,
         Component.m_237115_("guistrings.npc.bard." + key)
      );
      box.m_94182_(false);
      box.m_94199_(maxLength);
      box.m_94144_(value);
      this.m_142416_(box);
      this.fields.add(box);
      return box;
   }

   private void addTextField(BardMusicScreenLayout.Rect rect, String key, String value, BardMusicScreen.StringSetter setter) {
      this.input(rect, key, 128, value).m_94151_(setter::set);
   }

   private void addNumberField(BardMusicScreenLayout.Rect rect, String key, int value, BardMusicScreen.IntSetter setter) {
      this.input(rect, key, 4, Integer.toString(value)).m_94151_(text -> {
         try {
            setter.set(Integer.parseInt(text));
         } catch (NumberFormatException var3x) {
         }
      });
   }

   public void m_181908_() {
      super.m_181908_();
      this.fields.forEach(EditBox::m_94120_);
   }

   protected void m_280003_(GuiGraphics graphics, int mouseX, int mouseY) {
   }

   @Override
   protected void renderLegacyBackground(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      this.drawFitted(graphics, this.f_96539_, 8, 8, 304, -1);
      BardMusicScreenLayout.Rect list = BardMusicScreenLayout.LIST;
      graphics.m_280262_();
      graphics.m_280588_(this.f_97735_ + list.x(), this.f_97736_ + list.y(), this.f_97735_ + list.x() + list.width(), this.f_97736_ + list.y() + list.height());
      if (this.tracks.isEmpty()) {
         this.drawFitted(graphics, Component.m_237115_("guistrings.npc.bard.no_tracks"), 12, 88, 288, -1513240);
      }

      for (int row = 0; row < 2; row++) {
         int index = this.scrollOffset + row;
         if (index >= this.tracks.size()) {
            break;
         }

         int y = BardMusicScreenLayout.trackY(row);
         this.drawFitted(graphics, Component.m_237113_("#" + (index + 1)), 16, y + 4, 160, -1);
         this.drawFieldLabel(graphics, "file", 10, y + 18, 28);
         this.drawFieldLabel(graphics, "name", 10, y + 42, 28);
         this.drawFieldLabel(graphics, "duration", 190, y + 18, 26);
         this.drawFieldLabel(graphics, "intro", 190, y + 38, 26);
         this.drawFieldLabel(graphics, "outro", 190, y + 58, 26);
         this.drawFieldLabel(graphics, "loops", 264, y + 4, 40);

         for (BardMusicScreenLayout.Rect field : BardMusicScreenLayout.fields(row)) {
            graphics.m_280509_(
               this.f_97735_ + field.x(),
               this.f_97736_ + field.y(),
               this.f_97735_ + field.x() + field.width(),
               this.f_97736_ + field.y() + field.height(),
               -1644826
            );
            graphics.m_280509_(
               this.f_97735_ + field.x() + 1,
               this.f_97736_ + field.y() + 1,
               this.f_97735_ + field.x() + field.width() - 1,
               this.f_97736_ + field.y() + field.height() - 1,
               -16777216
            );
         }

         graphics.m_280509_(this.f_97735_ + 10, this.f_97736_ + y + 77, this.f_97735_ + 304, this.f_97736_ + y + 78, -14671840);
      }

      graphics.m_280262_();
      graphics.m_280618_();
      this.drawScrollIndicator(graphics, 306, 26, 160, this.tracks.size(), 2, this.scrollOffset);
      this.drawFitted(graphics, Component.m_237113_(this.tracks.size() + " / 32"), 98, 206, 74, -1513240);
   }

   private void drawFieldLabel(GuiGraphics graphics, String key, int x, int y, int width) {
      this.drawFitted(graphics, Component.m_237115_("guistrings.npc.bard." + key), x, y, width, -1);
   }

   private void drawFitted(GuiGraphics graphics, Component text, int x, int y, int width, int color) {
      float scale = Math.min(1.0F, (float)width / (float)Math.max(1, this.f_96547_.m_92852_(text)));
      graphics.m_280168_().m_85836_();
      graphics.m_280168_().m_252880_((float)(this.f_97735_ + x), (float)(this.f_97736_ + y), 0.0F);
      graphics.m_280168_().m_85841_(scale, scale, 1.0F);
      graphics.m_280614_(this.f_96547_, text, 0, 0, color, false);
      graphics.m_280168_().m_85849_();
   }

   @Override
   protected void afterContainerRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (this.expandedFileRow >= 0) {
         BardMusicScreenLayout.Rect popup = this.popup();
         int selected = BardMusicScreenLayout.fileChoice(
            popup, (double)(mouseX - this.f_97735_), (double)(mouseY - this.f_97736_), this.fileChoices.size(), this.fileScrollOffset
         );
         graphics.m_280262_();
         graphics.m_280168_().m_85836_();
         graphics.m_280168_().m_252880_(0.0F, 0.0F, 400.0F);
         LegacyGuiStyle.background(graphics, this.f_97735_ + popup.x(), this.f_97736_ + popup.y(), popup.width(), popup.height());
         graphics.m_280262_();
         graphics.m_280588_(
            this.f_97735_ + popup.x() + 2,
            this.f_97736_ + popup.y() + 2,
            this.f_97735_ + popup.x() + popup.width() - 8,
            this.f_97736_ + popup.y() + popup.height() - 2
         );
         if (this.fileChoices.isEmpty()) {
            this.drawFitted(graphics, Component.m_237115_("guistrings.npc.bard.no_audio_files"), popup.x() + 4, popup.y() + 3, popup.width() - 14, -1);
         }

         int end = Math.min(this.fileChoices.size(), this.fileScrollOffset + 6);

         for (int choice = this.fileScrollOffset; choice < end; choice++) {
            int y = popup.y() + 2 + (choice - this.fileScrollOffset) * 12;
            if (choice == selected) {
               graphics.m_280509_(
                  this.f_97735_ + popup.x() + 2, this.f_97736_ + y, this.f_97735_ + popup.x() + popup.width() - 8, this.f_97736_ + y + 12, -8355712
               );
            }

            String text = this.f_96547_.m_92834_(this.fileChoices.get(choice), popup.width() - 16);
            this.drawFitted(graphics, Component.m_237113_(text), popup.x() + 4, y + 2, popup.width() - 16, -1);
         }

         graphics.m_280262_();
         graphics.m_280618_();
         this.drawScrollIndicator(graphics, popup.x() + popup.width() - 6, popup.y() + 2, popup.height() - 4, this.fileChoices.size(), 6, this.fileScrollOffset);
         graphics.m_280262_();
         graphics.m_280168_().m_85849_();
      }
   }

   private void drawScrollIndicator(GuiGraphics graphics, int x, int y, int height, int count, int visible, int offset) {
      if (count > visible) {
         int thumb = Math.max(8, height * visible / count);
         int top = y + (height - thumb) * offset / (count - visible);
         graphics.m_280509_(this.f_97735_ + x, this.f_97736_ + y, this.f_97735_ + x + 4, this.f_97736_ + y + height, -9408400);
         graphics.m_280509_(this.f_97735_ + x, this.f_97736_ + top, this.f_97735_ + x + 4, this.f_97736_ + top + thumb, -1644826);
      }
   }

   private BardMusicScreenLayout.Rect popup() {
      return BardMusicScreenLayout.popup(this.expandedFileRow - this.scrollOffset, this.fileChoices.size());
   }

   private void closeFiles() {
      this.expandedFileRow = -1;
      this.fileChoices = List.of();
      this.fileScrollOffset = 0;
   }

   private void toggleFiles(int index) {
      if (this.expandedFileRow == index) {
         this.closeFiles();
      } else {
         this.expandedFileRow = index;
         this.fileChoices = List.copyOf(BardMusicLibrary.availableTracks());
         this.fileScrollOffset = 0;
         this.m_7522_(null);
         this.fields.forEach(field -> field.m_93692_(false));
      }
   }

   private void chooseFile(int choice) {
      if (this.expandedFileRow >= 0 && this.expandedFileRow < this.tracks.size() && choice >= 0 && choice < this.fileChoices.size()) {
         BardMusicScreen.MutableTrack track = this.tracks.get(this.expandedFileRow);
         track.fileName = this.fileChoices.get(choice);
         if (track.displayName.isBlank()) {
            track.displayName = stripExtension(track.fileName);
         }
      }

      this.closeFiles();
      this.rebuildFields();
   }

   private void addTrack() {
      if (this.tracks.size() < 32) {
         this.closeFiles();
         this.tracks.add(new BardMusicScreen.MutableTrack(BardTrack.blank()));
         this.scrollOffset = Math.max(0, this.tracks.size() - 2);
         this.rebuildFields();
      }
   }

   private void removeTrack(int index) {
      this.closeFiles();
      if (index >= 0 && index < this.tracks.size()) {
         this.tracks.remove(index);
      }

      this.scrollOffset = Math.min(this.scrollOffset, Math.max(0, this.tracks.size() - 2));
      this.rebuildFields();
   }

   public boolean m_6375_(double mouseX, double mouseY, int button) {
      if (this.expandedFileRow >= 0) {
         BardMusicScreenLayout.Rect popup = this.popup();
         double x = mouseX - (double)this.f_97735_;
         double y = mouseY - (double)this.f_97736_;
         if (popup.contains(x, y)) {
            if (button == 0) {
               int choice = BardMusicScreenLayout.fileChoice(popup, x, y, this.fileChoices.size(), this.fileScrollOffset);
               if (choice >= 0) {
                  this.f_96541_.m_91106_().m_120367_(SimpleSoundInstance.m_263171_(SoundEvents.f_12490_, 1.0F));
                  this.chooseFile(choice);
               }
            }

            return true;
         } else {
            this.closeFiles();
            return true;
         }
      } else {
         return super.m_6375_(mouseX, mouseY, button);
      }
   }

   public boolean m_6050_(double mouseX, double mouseY, double delta) {
      if (this.expandedFileRow >= 0) {
         if (this.popup().contains(mouseX - (double)this.f_97735_, mouseY - (double)this.f_97736_)) {
            this.fileScrollOffset = Math.max(
               0, Math.min(BardMusicScreenLayout.maxFileScroll(this.fileChoices.size()), this.fileScrollOffset - (int)Math.signum(delta))
            );
         }

         return true;
      } else if (BardMusicScreenLayout.LIST.contains(mouseX - (double)this.f_97735_, mouseY - (double)this.f_97736_)) {
         int next = Math.max(0, Math.min(Math.max(0, this.tracks.size() - 2), this.scrollOffset - (int)Math.signum(delta)));
         if (next != this.scrollOffset) {
            this.scrollOffset = next;
            this.rebuildFields();
         }

         return true;
      } else {
         return super.m_6050_(mouseX, mouseY, delta);
      }
   }

   public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
      if (this.expandedFileRow < 0) {
         return super.m_7933_(keyCode, scanCode, modifiers);
      } else {
         if (keyCode == 256) {
            this.closeFiles();
         } else if (keyCode == 264 || keyCode == 267) {
            this.fileScrollOffset = Math.min(BardMusicScreenLayout.maxFileScroll(this.fileChoices.size()), this.fileScrollOffset + (keyCode == 267 ? 6 : 1));
         } else if (keyCode == 265 || keyCode == 266) {
            this.fileScrollOffset = Math.max(0, this.fileScrollOffset - (keyCode == 266 ? 6 : 1));
         }

         return true;
      }
   }

   public boolean m_5534_(char codePoint, int modifiers) {
      return this.expandedFileRow >= 0 || super.m_5534_(codePoint, modifiers);
   }

   private void save() {
      AWCorePlatform.setBardTracks(((BardMusicMenu)this.f_97732_).getBardEntityId(), this.tracks.stream().map(BardMusicScreen.MutableTrack::toTrack).toList());
      this.m_7379_();
   }

   private static String stripExtension(String name) {
      int dot = name.lastIndexOf(46);
      return dot <= 0 ? name : name.substring(0, dot);
   }

   private interface IntSetter {
      void set(int var1);
   }

   private static class MutableTrack {
      private String fileName;
      private String displayName;
      private int durationSeconds;
      private int introSilenceSeconds;
      private int outroSilenceSeconds;
      private int loops;

      private MutableTrack(BardTrack track) {
         this.fileName = track.fileName();
         this.displayName = track.displayName();
         this.durationSeconds = track.durationSeconds();
         this.introSilenceSeconds = track.introSilenceSeconds();
         this.outroSilenceSeconds = track.outroSilenceSeconds();
         this.loops = track.loops();
      }

      private BardTrack toTrack() {
         return new BardTrack(this.fileName, this.displayName, this.durationSeconds, this.introSilenceSeconds, this.outroSilenceSeconds, this.loops);
      }
   }

   private interface StringSetter {
      void set(String var1);
   }
}
