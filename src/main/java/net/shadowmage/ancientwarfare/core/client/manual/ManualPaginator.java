package net.shadowmage.ancientwarfare.core.client.manual;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ManualPaginator {
   private ManualPaginator() {
   }

   public static <T> ManualPaginator.Book<T> paginate(List<ManualPaginator.Element<T>> elements, int pageHeight) {
      if (pageHeight <= 0) {
         throw new IllegalArgumentException("Invalid page height");
      } else {
         List<List<ManualPaginator.Placed<T>>> pages = new ArrayList<>();
         List<ManualPaginator.Placed<T>> page = new ArrayList<>();
         Map<String, Integer> anchors = new LinkedHashMap<>();
         int used = 0;

         for (int i = 0; i < elements.size(); i++) {
            ManualPaginator.Element<T> element = elements.get(i);
            if (element.height() > pageHeight) {
               throw new IllegalArgumentException("Element taller than a page");
            }

            int required = element.height();
            int following = i;

            while (elements.get(following).keepWithNext() && following + 1 < elements.size()) {
               required += elements.get(following).gapAfter() + elements.get(++following).height();
            }

            required = Math.min(pageHeight, required);
            if (!page.isEmpty() && used + required > pageHeight) {
               pages.add(List.copyOf(page));
               page.clear();
               used = 0;
            }

            if (element.anchor() != null && !element.anchor().isBlank()) {
               anchors.putIfAbsent(element.anchor(), pages.size());
            }

            page.add(new ManualPaginator.Placed<>(element.content(), used));
            used += element.height() + element.gapAfter();
         }

         if (!page.isEmpty() || pages.isEmpty()) {
            pages.add(List.copyOf(page));
         }

         if (pages.size() % 2 != 0) {
            pages.add(List.of());
         }

         return new ManualPaginator.Book<>(List.copyOf(pages), Map.copyOf(anchors));
      }
   }

   public static record Book<T>(List<List<ManualPaginator.Placed<T>>> pages, Map<String, Integer> anchors) {
   }

   public static record Element<T>(T content, int height, int gapAfter, boolean keepWithNext, String anchor) {
      public Element(T content, int height, int gapAfter, boolean keepWithNext, String anchor) {
         if (height > 0 && gapAfter >= 0) {
            this.content = content;
            this.height = height;
            this.gapAfter = gapAfter;
            this.keepWithNext = keepWithNext;
            this.anchor = anchor;
         } else {
            throw new IllegalArgumentException("Invalid element dimensions");
         }
      }
   }

   public static record Placed<T>(T content, int y) {
   }
}
