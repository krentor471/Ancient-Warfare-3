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
      return (MailboxData)level.m_8895_().m_164861_(MailboxData::load, MailboxData::new, "ancientwarfare_mailboxes");
   }

   public static MailboxData load(CompoundTag tag) {
      MailboxData data = new MailboxData();

      for (Tag t : tag.m_128437_("PublicNames", 8)) {
         data.publicNames.add(t.m_7916_());
      }

      CompoundTag priv = tag.m_128469_("PrivateNames");

      for (String owner : priv.m_128431_()) {
         Set<String> names = new TreeSet<>();

         for (Tag t : priv.m_128437_(owner, 8)) {
            names.add(t.m_7916_());
         }

         data.privateNames.put(owner, names);
      }

      CompoundTag pend = tag.m_128469_("Pending");

      for (String key : pend.m_128431_()) {
         List<ItemStack> list = new ArrayList<>();

         for (Tag x : pend.m_128437_(key, 10)) {
            list.add(ItemStack.m_41712_((CompoundTag)x));
         }

         data.pending.put(key, list);
      }

      return data;
   }

   public CompoundTag m_7176_(CompoundTag tag) {
      ListTag pub = new ListTag();
      this.publicNames.forEach(n -> pub.add(StringTag.m_129297_(n)));
      tag.m_128365_("PublicNames", pub);
      CompoundTag priv = new CompoundTag();
      this.privateNames.forEach((owner, names) -> {
         ListTag list = new ListTag();
         names.forEach(n -> list.add(StringTag.m_129297_(n)));
         priv.m_128365_(owner, list);
      });
      tag.m_128365_("PrivateNames", priv);
      CompoundTag pend = new CompoundTag();
      this.pending.forEach((key, list) -> {
         ListTag l = new ListTag();

         for (ItemStack st : list) {
            if (!st.m_41619_()) {
               l.add(st.m_41739_(new CompoundTag()));
            }
         }

         pend.m_128365_(key, l);
      });
      tag.m_128365_("Pending", pend);
      return tag;
   }

   public Set<String> names(String owner, boolean isPrivate) {
      return isPrivate ? this.privateNames.getOrDefault(owner, Set.of()) : this.publicNames;
   }

   public boolean addName(String owner, boolean isPrivate, String name) {
      if (name != null && !name.isBlank()) {
         boolean c = (isPrivate ? this.privateNames.computeIfAbsent(owner, k -> new TreeSet<>()) : this.publicNames).add(name);
         if (c) {
            this.m_77762_();
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
         this.m_77762_();
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
            if (!rem.m_41619_()) {
               list.set(i, rem);
               break;
            }

            list.remove(i);
         }

         if (list.isEmpty()) {
            this.pending.remove(key);
         }

         this.m_77762_();
      }
   }

   public List<MailboxData.MailboxInfo> knownMailboxes() {
      List<MailboxData.MailboxInfo> out = new ArrayList<>();
      this.receivers.forEach((k, set) -> {
         for (MailboxBlockEntity b : set) {
            out.add(new MailboxData.MailboxInfo(b.getMailboxName(), b.getOwnerNamePublic(), b.m_58899_()));
         }
      });
      return out;
   }

   public void unregister(MailboxBlockEntity box) {
      this.receivers.values().forEach(s -> s.remove(box));
      this.privateReceivers.values().forEach(s -> s.remove(box));
   }

   public ItemStack deliver(MailboxBlockEntity from, String target, ItemStack stack) {
      if (target != null && !target.isBlank() && !stack.m_41619_()) {
         Set<MailboxBlockEntity> set = (from.isPrivateBox() ? this.privateReceivers : this.receivers).get(from.keyFor(target));
         if (set == null) {
            this.pending.computeIfAbsent(from.keyFor(target), k -> new ArrayList<>()).add(stack.m_41777_());
            this.m_77762_();
            return ItemStack.f_41583_;
         } else {
            for (MailboxBlockEntity box : new ArrayList<>(set)) {
               stack = box.insertReceived(stack);
               if (stack.m_41619_()) {
                  break;
               }
            }

            this.m_77762_();
            return stack;
         }
      } else {
         return stack;
      }
   }

   public static record MailboxInfo(String name, String owner, BlockPos pos) {
   }
}
