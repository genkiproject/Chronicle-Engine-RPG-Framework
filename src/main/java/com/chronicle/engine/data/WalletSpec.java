package com.chronicle.engine.data;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public record WalletSpec(
        String currencyId,
        String itemId,
        List<String> itemIds,
        TextValue displayName,
        int sortOrder
) {
    public static WalletSpec parse(JsonObject json) {
        List<String> ids = new ArrayList<>();
        String itemId = JsonUtil.string(json, "itemId", JsonUtil.string(json, "currencyItem", ""));
        if (!itemId.isBlank()) {
            ids.add(itemId);
        }
        ids.addAll(JsonUtil.strings(json, "itemIds"));
        ids.addAll(JsonUtil.strings(json, "items"));

        List<String> uniqueIds = ids.stream()
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .distinct()
                .toList();
        String primaryItemId = uniqueIds.isEmpty() ? "" : uniqueIds.get(0);
        return new WalletSpec(
                JsonUtil.string(json, "currencyId", primaryItemId),
                primaryItemId,
                uniqueIds,
                TextValue.parse(json.get("displayName")),
                JsonUtil.integer(json, "sortOrder", 0)
        );
    }
}

