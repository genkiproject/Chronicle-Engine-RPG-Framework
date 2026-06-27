package com.chronicle.engine.data;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ShopSpec(
        String shopId,
        TextValue displayName,
        TextValue description,
        JsonObject openCondition,
        List<Category> categories,
        Map<String, Entry> entries
) {
    public static ShopSpec parse(JsonObject json) {
        List<Category> categories = new ArrayList<>();
        for (JsonObject categoryJson : JsonUtil.objects(json, "categories")) {
            categories.add(Category.parse(categoryJson));
        }
        categories.sort(Comparator.comparingInt(Category::sortOrder).thenComparing(Category::categoryId));

        Map<String, Entry> entries = new LinkedHashMap<>();
        for (JsonObject entryJson : JsonUtil.objectValues(json, "entries")) {
            Entry entry = Entry.parse(entryJson);
            entries.put(entry.entryId(), entry);
        }

        return new ShopSpec(
                JsonUtil.string(json, "shopId", ""),
                TextValue.parse(json.get("displayName")),
                TextValue.parse(json.get("description")),
                JsonUtil.object(json, "openCondition"),
                categories,
                entries
        );
    }

    public List<Entry> sortedEntries() {
        return entries.values().stream()
                .sorted(Comparator.comparing(Entry::category).thenComparingInt(Entry::sortOrder).thenComparing(Entry::entryId))
                .toList();
    }

    public record Category(String categoryId, TextValue displayName, int sortOrder, String formatting) {
        public static Category parse(JsonObject json) {
            return new Category(
                    JsonUtil.string(json, "categoryId", ""),
                    TextValue.parse(json.get("displayName")),
                    JsonUtil.integer(json, "sortOrder", 0),
                    JsonUtil.string(json, "formatting", "WHITE")
            );
        }
    }

    public record Entry(
            String entryId,
            TextValue displayName,
            List<QuestSpec.Reward> costs,
            List<QuestSpec.Reward> rewards,
            String category,
            JsonObject visibleCondition,
            int sortOrder
    ) {
        public static Entry parse(JsonObject json) {
            return new Entry(
                    JsonUtil.string(json, "entryId", ""),
                    TextValue.parse(json.get("displayName")),
                    QuestSpec.Reward.parseList(json, "costs"),
                    QuestSpec.Reward.parseList(json, "rewards"),
                    JsonUtil.string(json, "category", ""),
                    JsonUtil.object(json, "visibleCondition"),
                    JsonUtil.integer(json, "sortOrder", 0)
            );
        }
    }
}

