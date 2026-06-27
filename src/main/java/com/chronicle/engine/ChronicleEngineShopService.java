package com.chronicle.engine;

import com.chronicle.engine.data.QuestSpec;
import com.chronicle.engine.data.JsonUtil;
import com.chronicle.engine.data.ShopSpec;
import com.chronicle.engine.data.ChronicleEngineData;
import com.chronicle.engine.network.ChronicleEngineNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class ChronicleEngineShopService {
    private ChronicleEngineShopService() {
    }

    public static void open(ServerPlayer player, String shopId) {
        ShopSpec shop = ChronicleEngineData.shop(shopId);
        if (shop == null || !ChronicleEngineConditions.test(player, shop.openCondition())) {
            return;
        }
        List<ChronicleEngineNetwork.ShopCategoryLine> categories = new ArrayList<>();
        categories.add(new ChronicleEngineNetwork.ShopCategoryLine("__all", Component.translatable("screen.chronicle_engine.shop.all")));
        for (ShopSpec.Category category : shop.categories()) {
            categories.add(new ChronicleEngineNetwork.ShopCategoryLine(category.categoryId(), category.displayName().component()));
        }
        List<ChronicleEngineNetwork.ShopEntryLine> entries = new ArrayList<>();
        for (ShopSpec.Entry entry : shop.sortedEntries()) {
            if (!ChronicleEngineConditions.test(player, entry.visibleCondition())) {
                continue;
            }
            entries.add(new ChronicleEngineNetwork.ShopEntryLine(
                    entry.entryId(),
                    entry.category(),
                    entry.displayName().component(),
                    describe(entry.costs()),
                    describe(entry.rewards()),
                    itemLines(entry.costs()),
                    itemLines(entry.rewards())
            ));
        }
        ChronicleEngineNetwork.openShop(player, new ChronicleEngineNetwork.OpenShopPacket(
                shop.shopId(),
                shop.displayName().component(),
                categories,
                entries,
                ChronicleEngineWalletService.snapshot(player)
        ));
    }

    public static void buy(ServerPlayer player, String shopId, String entryId) {
        ShopSpec shop = ChronicleEngineData.shop(shopId);
        if (shop == null || !ChronicleEngineConditions.test(player, shop.openCondition())) {
            return;
        }
        ShopSpec.Entry entry = shop.entries().get(entryId);
        if (entry == null || !ChronicleEngineConditions.test(player, entry.visibleCondition())) {
            return;
        }
        for (QuestSpec.Reward cost : entry.costs()) {
            if (!ChronicleEngineItems.canPay(player, cost)) {
                player.displayClientMessage(Component.translatable("message.chronicle_engine.shop.no_money").withStyle(ChatFormatting.RED), false);
                return;
            }
        }
        for (QuestSpec.Reward cost : entry.costs()) {
            ChronicleEngineItems.pay(player, cost);
        }
        for (QuestSpec.Reward reward : entry.rewards()) {
            ChronicleEngineItems.executeReward(player, reward);
        }
        player.displayClientMessage(Component.translatable("message.chronicle_engine.shop.bought").withStyle(ChatFormatting.GREEN), false);
        open(player, shopId);
    }

    private static String describe(List<QuestSpec.Reward> rewards) {
        List<String> parts = new ArrayList<>();
        for (QuestSpec.Reward reward : rewards) {
            parts.add(ChronicleEngineItems.describe(reward));
        }
        return String.join(", ", parts);
    }

    private static List<ChronicleEngineNetwork.ShopItemLine> itemLines(List<QuestSpec.Reward> rewards) {
        List<ChronicleEngineNetwork.ShopItemLine> items = new ArrayList<>();
        for (QuestSpec.Reward reward : rewards) {
            if ("item".equals(reward.type()) || "give_item".equals(reward.type())) {
                String itemId = JsonUtil.string(reward.raw(), "itemId", "");
                if (!itemId.isBlank()) {
                    items.add(new ChronicleEngineNetwork.ShopItemLine(
                            itemId,
                            JsonUtil.integer(reward.raw(), "count", 1),
                            "",
                            0,
                            JsonUtil.string(reward.raw(), "nbt", "")
                    ));
                }
            } else if ("enchanted_book".equals(reward.type())) {
                items.add(new ChronicleEngineNetwork.ShopItemLine(
                        "minecraft:enchanted_book",
                        1,
                        JsonUtil.string(reward.raw(), "enchantmentId", ""),
                        JsonUtil.integer(reward.raw(), "level", 1),
                        ""
                ));
            } else if ("potion".equals(reward.type())) {
                items.add(new ChronicleEngineNetwork.ShopItemLine(
                        JsonUtil.string(reward.raw(), "itemId", "minecraft:potion"),
                        JsonUtil.integer(reward.raw(), "count", 1),
                        JsonUtil.string(reward.raw(), "potionId", "minecraft:water"),
                        0,
                        ""
                ));
            }
        }
        return items;
    }
}

