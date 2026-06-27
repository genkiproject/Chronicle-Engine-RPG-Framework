package com.chronicle.engine.data;

import com.chronicle.engine.ChronicleEngine;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ChronicleEngineData {
    private static final Map<String, DialogueSpec> DIALOGUES = new LinkedHashMap<>();
    private static final Map<String, QuestSpec> QUESTS = new LinkedHashMap<>();
    private static final Map<String, NpcSpec> NPCS = new LinkedHashMap<>();
    private static final Map<String, ShopSpec> SHOPS = new LinkedHashMap<>();
    private static final Map<String, WalletSpec> WALLET_CURRENCIES = new LinkedHashMap<>();

    private ChronicleEngineData() {
    }

    public static void apply(Map<ResourceLocation, JsonElement> objects) {
        Map<ResourceLocation, JsonElement> combined = new LinkedHashMap<>(objects);
        combined.putAll(ChronicleEngineConfigDataLoader.load());
        Map<String, DialogueSpec> dialogues = new LinkedHashMap<>();
        Map<String, QuestSpec> quests = new LinkedHashMap<>();
        Map<String, NpcSpec> npcs = new LinkedHashMap<>();
        Map<String, ShopSpec> shops = new LinkedHashMap<>();
        Map<String, WalletSpec> walletCurrencies = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : combined.entrySet()) {
            String path = entry.getKey().getPath();
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject json = entry.getValue().getAsJsonObject();
            try {
                if (path.startsWith("dialogues/")) {
                    DialogueSpec spec = DialogueSpec.parse(json);
                    dialogues.put(spec.id(), spec);
                } else if (path.startsWith("quests/")) {
                    QuestSpec spec = QuestSpec.parse(json);
                    quests.put(spec.id(), spec);
                } else if (path.startsWith("npc/")) {
                    NpcSpec spec = NpcSpec.parse(path, json);
                    npcs.put(spec.id(), spec);
                } else if (path.startsWith("trades/") || path.startsWith("shops/")) {
                    ShopSpec spec = ShopSpec.parse(json);
                    shops.put(spec.shopId(), spec);
                } else if (path.startsWith("wallet/")) {
                    WalletSpec spec = WalletSpec.parse(json);
                    if (!spec.itemIds().isEmpty()) {
                        for (String itemId : spec.itemIds()) {
                            walletCurrencies.put(itemId, spec);
                        }
                    }
                }
            } catch (RuntimeException exception) {
                ChronicleEngine.LOGGER.error("Failed to parse ChronicleEngine resource {}: {}", entry.getKey(), exception.getMessage(), exception);
            }
        }

        DIALOGUES.clear();
        DIALOGUES.putAll(dialogues);
        QUESTS.clear();
        QUESTS.putAll(quests);
        NPCS.clear();
        NPCS.putAll(npcs);
        SHOPS.clear();
        SHOPS.putAll(shops);
        WALLET_CURRENCIES.clear();
        WALLET_CURRENCIES.putAll(walletCurrencies);

        ChronicleEngine.LOGGER.info("Loaded ChronicleEngine data: {} dialogues, {} quests, {} npc bindings, {} shops, {} wallet currencies",
                DIALOGUES.size(), QUESTS.size(), NPCS.size(), SHOPS.size(), WALLET_CURRENCIES.size());
    }

    public static DialogueSpec dialogue(String id) {
        return DIALOGUES.get(id);
    }

    public static QuestSpec quest(String id) {
        return QUESTS.get(id);
    }

    public static ShopSpec shop(String id) {
        return SHOPS.get(id);
    }

    public static List<QuestSpec> quests() {
        return QUESTS.values().stream()
                .sorted(Comparator.comparingInt(QuestSpec::sortOrder).thenComparing(QuestSpec::id))
                .toList();
    }

    public static List<NpcSpec> npcsForEntityType(String entityType) {
        List<NpcSpec> result = new ArrayList<>();
        for (NpcSpec spec : NPCS.values()) {
            if (spec.entityType().equals(entityType)) {
                result.add(spec);
            }
        }
        return result;
    }

    public static List<ShopSpec> shops() {
        return List.copyOf(SHOPS.values());
    }

    public static List<WalletSpec> walletCurrencies() {
        Map<String, WalletSpec> unique = new LinkedHashMap<>();
        for (WalletSpec spec : WALLET_CURRENCIES.values()) {
            unique.putIfAbsent(spec.currencyId(), spec);
        }
        return unique.values().stream()
                .sorted(Comparator.comparingInt(WalletSpec::sortOrder).thenComparing(WalletSpec::itemId))
                .toList();
    }

    public static WalletSpec walletCurrencyByItem(String itemId) {
        return WALLET_CURRENCIES.get(itemId);
    }
}

