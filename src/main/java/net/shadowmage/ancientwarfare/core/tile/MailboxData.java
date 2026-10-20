package net.shadowmage.ancientwarfare.core.tile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

public class MailboxData extends SavedData {
   private final Map<String, Set<MailboxBlockEntity>> receivers = new HashMap<>();
   private final Map<String, Set<MailboxBlockEntity>> privateReceivers = new HashMap<>();
   private final Map<String, List<ItemStack>> pending = new HashMap<>();
   private final Set<String> publicNames = new TreeSet<>();
   private final Map<String, Set<String>> privateNames = new HashMap<>();

   public static MailboxData get(ServerLevel level) {
      return (MailboxData)level.getDataStorage().computeIfAbsent(MailboxData::load, MailboxData::new, "ancientwarfare_mailboxes");
   }

   public static MailboxData load(CompoundTag tag) {
      MailboxData data = new MailboxData();

      for (Tag t : tag.getList("PublicNames", 8)) {
         data.publicNames.add(t.getAsString());
      }

      CompoundTag priv = tag.getCompound("PrivateNames");

      for (String owner : priv.getAllKeys()) {
         Set<String> names = new TreeSet<>();

         for (Tag t : priv.getList(owner, 8)) {
            names.add(t.getAsString());
         }

         data.privateNames.put(owner, names);
      }

      CompoundTag pend = tag.getCompound("Pending");

      for (String key : pend.getAllKeys()) {
         List<ItemStack> list = new ArrayList<>();

         for (Tag x : pend.getList(key, 10)) {
            list.add(ItemStack.of((CompoundTag)x));
         }

         data.pending.put(key, list);
      }

      return data;
   }

   public CompoundTag save(CompoundTag tag) {
      ListTag pub = new ListTag();
      this.publicNames.forEach(n -> pub.add(StringTag.valueOf(n)));
      tag.put("PublicNames", pub);
      CompoundTag priv = new CompoundTag();
      this.privateNames.forEach((owner, names) -> {
         ListTag list = new ListTag();
         names.forEach(n -> list.add(StringTag.valueOf(n)));
         priv.put(owner, list);
      });
      tag.put("PrivateNames", priv);
      CompoundTag pend = new CompoundTag();
      this.pending.forEach((key, list) -> {
         ListTag l = new ListTag();

         for (ItemStack st : list) {
            if (!st.isEmpty()) {
               l.add(st.save(new CompoundTag()));
            }
         }

         pend.put(key, l);
      });
      tag.put("Pending", pend);
      return tag;
   }

   public Set<String> names(String owner, boolean isPrivate) {
      return isPrivate ? this.privateNames.getOrDefault(owner, Set.of()) : this.publicNames;
   }

   public boolean addName(String owner, boolean isPrivate, String name) {
      if (name != null && !name.isBlank()) {
         boolean c = (isPrivate ? this.privateNames.computeIfAbsent(owner, k -> new TreeSet<>()) : this.publicNames).add(name);
         if (c) {
            this.setDirty();
         }

         return c;
      } else {
         return false;
      }
   }

   public boolean removeName(String owner, boolean isPrivate, String name) {
      Set<String> s = isPrivate ? this.privateNames.get(owner) : this.publicNames;
      boolean c = s != null && s.remove(name);
      if (c) {
         this.setDirty();
      }

      return c;
   }

   public void register(MailboxBlockEntity box) {
      this.unregister(box);
      String key = box.addressKey();
      if (key != null) {
         (box.isPrivateBox() ? this.privateReceivers : this.receivers).computeIfAbsent(key, k -> new HashSet<>()).add(box);
         this.flushPending(key, box);
      }
   }

   private void flushPending(String key, MailboxBlockEntity box) {
      List<ItemStack> list = this.pending.get(key);
      if (list != null) {
         int i = 0;

         while (i < list.size()) {
            ItemStack rem = box.insertReceived(list.get(i));
            if (!rem.isEmpty()) {
               list.set(i, rem);
               break;
            }

            list.remove(i);
         }

         if (list.isEmpty()) {
            this.pending.remove(key);
         }

         this.setDirty();
      }
   }

   public List<MailboxData.MailboxInfo> knownMailboxes() {
      List<MailboxData.MailboxInfo> out = new ArrayList<>();
      this.receivers.forEach((k, set) -> {
         for (MailboxBlockEntity b : set) {
            out.add(new MailboxData.MailboxInfo(b.getMailboxName(), b.getOwnerNamePublic(), b.getBlockPos()));
         }
      });
      return out;
   }

   public void unregister(MailboxBlockEntity box) {
      this.receivers.values().forEach(s -> s.remove(box));
      this.privateReceivers.values().forEach(s -> s.remove(box));
   }

   public ItemStack deliver(MailboxBlockEntity from, String target, ItemStack stack) {
      if (target != null && !target.isBlank() && !stack.isEmpty()) {
         Set<MailboxBlockEntity> set = (from.isPrivateBox() ? this.privateReceivers : this.receivers).get(from.keyFor(target));
         if (set == null) {
            this.pending.computeIfAbsent(from.keyFor(target), k -> new ArrayList<>()).add(stack.copy());
            this.setDirty();
            return ItemStack.EMPTY;
         } else {
            for (MailboxBlockEntity box : new ArrayList<>(set)) {
               stack = box.insertReceived(stack);
               if (stack.isEmpty()) {
                  break;
               }
            }

            this.setDirty();
            return stack;
         }
      } else {
         return stack;
      }
   }

   public static record MailboxInfo(String name, String owner, BlockPos pos) {
   }
}
