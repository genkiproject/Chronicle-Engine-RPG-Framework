package com.chronicle.engine;

import com.chronicle.engine.data.JsonUtil;
import com.chronicle.engine.data.QuestSpec;
import com.google.gson.JsonObject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ChronicleEngineItems {
    private ChronicleEngineItems() {
    }

    public static boolean hasItem(ServerPlayer player, String itemId, int count) {
        int remaining = count;
        for (ItemStack stack : player.getInventory().items) {
            if (matches(stack, itemId)) {
                remaining -= stack.getCount();
                if (remaining <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean removeItem(ServerPlayer player, String itemId, int count) {
        if (!hasItem(player, itemId, count)) {
            return false;
        }
        int remaining = count;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.items.size() && remaining > 0; i++) {
            ItemStack stack = inventory.items.get(i);
            if (matches(stack, itemId)) {
                int removed = Math.min(remaining, stack.getCount());
                stack.shrink(removed);
                remaining -= removed;
            }
        }
        inventory.setChanged();
        return true;
    }

    public static int countItem(ServerPlayer player, String itemId) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (matches(stack, itemId)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static boolean matches(ItemStack stack, String itemId) {
        if (stack.isEmpty() || itemId == null || itemId.isBlank()) {
            return false;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key.toString().equals(itemId);
    }

    public static void giveItem(ServerPlayer player, String itemId, int count) {
        Optional<Item> optional = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(itemId));
        if (optional.isEmpty()) {
            ChronicleEngine.LOGGER.warn("Unknown item reward: {}", itemId);
            return;
        }
        giveItem(player, itemId, count, "");
    }

    public static void giveItem(ServerPlayer player, String itemId, int count, String nbt) {
        Optional<Item> optional = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(itemId));
        if (optional.isEmpty()) {
            ChronicleEngine.LOGGER.warn("Unknown item reward: {}", itemId);
            return;
        }
        ItemStack stack = new ItemStack(optional.get(), Math.max(1, count));
        if (nbt != null && !nbt.isBlank()) {
            try {
                CompoundTag tag = TagParser.parseTag(nbt);
                stack.setTag(tag);
            } catch (Exception exception) {
                ChronicleEngine.LOGGER.warn("Invalid item reward NBT for {}: {}", itemId, nbt, exception);
            }
        }
        giveStack(player, stack);
    }

    public static void giveEnchantedBook(ServerPlayer player, String enchantmentId, int level) {
        ResourceLocation id = ResourceLocation.tryParse(enchantmentId);
        Optional<Enchantment> enchantment = id == null
                ? Optional.empty()
                : BuiltInRegistries.ENCHANTMENT.getOptional(id);
        if (enchantment.isEmpty()) {
            ChronicleEngine.LOGGER.warn("Unknown enchantment reward: {}", enchantmentId);
            return;
        }
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(enchantment.get(), Math.max(1, level)));
        giveStack(player, stack);
    }

    public static void givePotion(ServerPlayer player, String itemId, String potionId, int count) {
        Optional<Item> optionalItem = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(itemId));
        if (optionalItem.isEmpty()) {
            ChronicleEngine.LOGGER.warn("Unknown potion item reward: {}", itemId);
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(potionId);
        Optional<Potion> potion = id == null ? Optional.empty() : BuiltInRegistries.POTION.getOptional(id);
        if (potion.isEmpty()) {
            ChronicleEngine.LOGGER.warn("Unknown potion reward: {}", potionId);
            return;
        }
        ItemStack stack = new ItemStack(optionalItem.get(), Math.max(1, count));
        PotionUtils.setPotion(stack, potion.get());
        giveStack(player, stack);
    }

    public static void giveStack(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public static void executeReward(ServerPlayer player, QuestSpec.Reward reward) {
        executeReward(player, reward.type(), reward.raw());
    }

    public static void executeReward(ServerPlayer player, String type, JsonObject raw) {
        if ("item".equals(type) || "give_item".equals(type)) {
            giveItem(
                    player,
                    JsonUtil.string(raw, "itemId", ""),
                    JsonUtil.integer(raw, "count", 1),
                    JsonUtil.string(raw, "nbt", "")
            );
        } else if ("enchanted_book".equals(type)) {
            giveEnchantedBook(
                    player,
                    JsonUtil.string(raw, "enchantmentId", ""),
                    JsonUtil.integer(raw, "level", 1)
            );
        } else if ("random_enchanted_book".equals(type)) {
            giveRandomEnchantedBook(player, raw);
        } else if ("random_item".equals(type)) {
            giveRandomItem(player, raw);
        } else if ("potion".equals(type)) {
            givePotion(
                    player,
                    JsonUtil.string(raw, "itemId", "minecraft:potion"),
                    JsonUtil.string(raw, "potionId", "minecraft:water"),
                    JsonUtil.integer(raw, "count", 1)
            );
        } else if ("random_potion".equals(type)) {
            giveRandomPotion(player, raw);
        } else if ("command".equals(type) || "run_command".equals(type)) {
            runCommand(player, JsonUtil.string(raw, "command", ""));
        } else if (!type.isBlank()) {
            ChronicleEngine.LOGGER.warn("Unknown reward/action type: {}", type);
        }
    }

    public static boolean canPay(ServerPlayer player, QuestSpec.Reward cost) {
        if (!"item".equals(cost.type())) {
            return false;
        }
        return ChronicleEngineWalletService.canPay(
                player,
                JsonUtil.string(cost.raw(), "itemId", ""),
                JsonUtil.integer(cost.raw(), "count", 1)
        );
    }

    public static boolean pay(ServerPlayer player, QuestSpec.Reward cost) {
        if (!"item".equals(cost.type())) {
            return false;
        }
        return ChronicleEngineWalletService.pay(
                player,
                JsonUtil.string(cost.raw(), "itemId", ""),
                JsonUtil.integer(cost.raw(), "count", 1)
        );
    }

    public static String describe(QuestSpec.Reward reward) {
        if ("item".equals(reward.type()) || "give_item".equals(reward.type())) {
            String itemId = JsonUtil.string(reward.raw(), "itemId", "");
            String nbt = JsonUtil.string(reward.raw(), "nbt", "");
            return itemId + (nbt.isBlank() ? "" : nbt) + " x" + JsonUtil.integer(reward.raw(), "count", 1);
        }
        if ("enchanted_book".equals(reward.type())) {
            return JsonUtil.string(reward.raw(), "enchantmentId", "") + " "
                    + JsonUtil.integer(reward.raw(), "level", 1);
        }
        if ("random_enchanted_book".equals(reward.type())) {
            return "\u968f\u673a\u9644\u9b54\u4e66";
        }
        if ("random_item".equals(reward.type())) {
            return "\u968f\u673a\u7269\u54c1";
        }
        if ("potion".equals(reward.type())) {
            return JsonUtil.string(reward.raw(), "potionId", "minecraft:water") + " x"
                    + JsonUtil.integer(reward.raw(), "count", 1);
        }
        if ("random_potion".equals(reward.type())) {
            return "\u968f\u673a\u836f\u6c34";
        }
        if ("command".equals(reward.type()) || "run_command".equals(reward.type())) {
            return "/" + JsonUtil.string(reward.raw(), "command", "");
        }
        return reward.type();
    }

    private static void giveRandomEnchantedBook(ServerPlayer player, JsonObject raw) {
        if (!passesChance(player, raw)) {
            return;
        }
        List<JsonObject> valid = new ArrayList<>();
        for (JsonObject entry : JsonUtil.objects(raw, "entries")) {
            ResourceLocation id = ResourceLocation.tryParse(JsonUtil.string(entry, "enchantmentId", ""));
            if (id != null && BuiltInRegistries.ENCHANTMENT.containsKey(id)) {
                valid.add(entry);
            }
        }
        JsonObject selected = selectWeighted(player, valid);
        if (selected != null) {
            giveEnchantedBook(
                    player,
                    JsonUtil.string(selected, "enchantmentId", ""),
                    JsonUtil.integer(selected, "level", 1)
            );
        }
    }

    private static void giveRandomItem(ServerPlayer player, JsonObject raw) {
        if (!passesChance(player, raw)) {
            return;
        }
        List<JsonObject> valid = new ArrayList<>();
        for (JsonObject entry : JsonUtil.objects(raw, "entries")) {
            ResourceLocation id = ResourceLocation.tryParse(JsonUtil.string(entry, "itemId", ""));
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
                valid.add(entry);
            }
        }
        JsonObject selected = selectWeighted(player, valid);
        if (selected != null) {
            giveItem(
                    player,
                    JsonUtil.string(selected, "itemId", ""),
                    JsonUtil.integer(selected, "count", 1)
            );
        }
    }

    private static void giveRandomPotion(ServerPlayer player, JsonObject raw) {
        if (!passesChance(player, raw)) {
            return;
        }
        List<JsonObject> valid = new ArrayList<>();
        for (JsonObject entry : JsonUtil.objects(raw, "entries")) {
            ResourceLocation itemId = ResourceLocation.tryParse(JsonUtil.string(entry, "itemId", "minecraft:potion"));
            ResourceLocation potionId = ResourceLocation.tryParse(JsonUtil.string(entry, "potionId", "minecraft:water"));
            if (itemId != null && potionId != null
                    && BuiltInRegistries.ITEM.containsKey(itemId)
                    && BuiltInRegistries.POTION.containsKey(potionId)) {
                valid.add(entry);
            }
        }
        JsonObject selected = selectWeighted(player, valid);
        if (selected != null) {
            givePotion(
                    player,
                    JsonUtil.string(selected, "itemId", "minecraft:potion"),
                    JsonUtil.string(selected, "potionId", "minecraft:water"),
                    JsonUtil.integer(selected, "count", 1)
            );
        }
    }

    private static boolean passesChance(ServerPlayer player, JsonObject raw) {
        double chance = raw.has("chance") ? raw.get("chance").getAsDouble() : 1.0D;
        return chance >= 1.0D || player.getRandom().nextDouble() < Math.max(0.0D, chance);
    }

    private static JsonObject selectWeighted(ServerPlayer player, List<JsonObject> entries) {
        int totalWeight = 0;
        for (JsonObject entry : entries) {
            totalWeight += Math.max(1, JsonUtil.integer(entry, "weight", 1));
        }
        if (totalWeight <= 0) {
            return null;
        }
        int roll = player.getRandom().nextInt(totalWeight);
        for (JsonObject entry : entries) {
            roll -= Math.max(1, JsonUtil.integer(entry, "weight", 1));
            if (roll < 0) {
                return entry;
            }
        }
        return entries.isEmpty() ? null : entries.get(entries.size() - 1);
    }

    public static void runCommand(ServerPlayer player, String command) {
        if (command == null || command.isBlank()) {
            return;
        }
        String resolved = command.replace("{player}", player.getGameProfile().getName());
        if (resolved.startsWith("/")) {
            resolved = resolved.substring(1);
        }
        CommandSourceStack source = player.createCommandSourceStack()
                .withPermission(4)
                .withSuppressedOutput();
        player.getServer().getCommands().performPrefixedCommand(source, resolved);
    }
}

