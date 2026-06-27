package com.chronicle.engine;

import com.chronicle.engine.data.ChronicleEngineData;
import com.chronicle.engine.data.WalletSpec;
import com.chronicle.engine.network.ChronicleEngineNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ChronicleEngineWalletService {
    private static final String WALLET = "wallet";

    private ChronicleEngineWalletService() {
    }

    public static List<ChronicleEngineNetwork.WalletLine> snapshot(ServerPlayer player) {
        List<ChronicleEngineNetwork.WalletLine> lines = new ArrayList<>();
        for (WalletSpec currency : ChronicleEngineData.walletCurrencies()) {
            lines.add(new ChronicleEngineNetwork.WalletLine(
                    currency.currencyId(),
                    currency.itemId(),
                    currency.displayName().component(),
                    balance(player, currency.itemId())
            ));
        }
        return lines;
    }

    public static void openWallet(ServerPlayer player) {
        ChronicleEngineNetwork.openWallet(player, new ChronicleEngineNetwork.OpenWalletPacket(snapshot(player)));
    }

    public static boolean isCurrency(String itemId) {
        return ChronicleEngineData.walletCurrencyByItem(itemId) != null;
    }

    public static long balance(ServerPlayer player, String itemId) {
        WalletSpec currency = ChronicleEngineData.walletCurrencyByItem(itemId);
        return balances(player).getLong(storageKey(currency, itemId));
    }

    public static long available(ServerPlayer player, String itemId) {
        WalletSpec currency = ChronicleEngineData.walletCurrencyByItem(itemId);
        if (currency == null) {
            return ChronicleEngineItems.countItem(player, itemId);
        }
        return balance(player, currency.itemId()) + countCurrencyItems(player, currency);
    }

    public static boolean canPay(ServerPlayer player, String itemId, int count) {
        if (itemId == null || itemId.isBlank() || count <= 0) {
            return false;
        }
        if (isCurrency(itemId)) {
            return available(player, itemId) >= count;
        }
        return ChronicleEngineItems.hasItem(player, itemId, count);
    }

    public static boolean pay(ServerPlayer player, String itemId, int count) {
        if (itemId == null || itemId.isBlank() || count <= 0) {
            return false;
        }
        WalletSpec currency = ChronicleEngineData.walletCurrencyByItem(itemId);
        if (currency == null) {
            return ChronicleEngineItems.removeItem(player, itemId, count);
        }
        if (available(player, itemId) < count) {
            return false;
        }
        long fromWallet = Math.min(balance(player, currency.itemId()), count);
        if (fromWallet > 0) {
            add(player, currency.itemId(), -fromWallet);
        }
        int fromInventory = (int) (count - fromWallet);
        return fromInventory <= 0 || removeCurrencyItems(player, currency, fromInventory);
    }

    public static void depositAll(ServerPlayer player) {
        if (ChronicleEngineData.walletCurrencies().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.chronicle_engine.wallet.no_currency").withStyle(ChatFormatting.RED), true);
            return;
        }
        long total = 0L;
        for (WalletSpec currency : ChronicleEngineData.walletCurrencies()) {
            long removed = removeAllCurrency(player, currency);
            if (removed > 0) {
                add(player, currency.itemId(), removed);
                total += removed;
            }
        }
        if (total > 0) {
            player.displayClientMessage(Component.translatable("message.chronicle_engine.wallet.deposited", total).withStyle(ChatFormatting.GOLD), true);
        } else {
            player.displayClientMessage(Component.translatable("message.chronicle_engine.wallet.empty").withStyle(ChatFormatting.GRAY), true);
        }
    }

    public static void withdraw(ServerPlayer player, String itemId, long requested) {
        WalletSpec currency = ChronicleEngineData.walletCurrencyByItem(itemId);
        if (currency == null || requested <= 0) {
            openWallet(player);
            return;
        }
        long amount = Math.min(requested, balance(player, currency.itemId()));
        if (amount <= 0) {
            player.displayClientMessage(Component.translatable("message.chronicle_engine.wallet.not_enough").withStyle(ChatFormatting.RED), false);
            openWallet(player);
            return;
        }
        add(player, currency.itemId(), -amount);
        giveCurrency(player, currency.itemId(), amount);
        player.displayClientMessage(Component.translatable("message.chronicle_engine.wallet.withdrawn", amount).withStyle(ChatFormatting.GREEN), false);
        openWallet(player);
    }

    private static void giveCurrency(ServerPlayer player, String itemId, long amount) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        Optional<Item> item = id == null ? Optional.empty() : BuiltInRegistries.ITEM.getOptional(id);
        if (item.isEmpty()) {
            ChronicleEngine.LOGGER.warn("Unknown wallet currency item: {}", itemId);
            return;
        }
        int maxStackSize = Math.max(1, item.get().getDefaultInstance().getMaxStackSize());
        long remaining = amount;
        while (remaining > 0) {
            int chunk = (int) Math.min(maxStackSize, remaining);
            ChronicleEngineItems.giveStack(player, new ItemStack(item.get(), chunk));
            remaining -= chunk;
        }
    }

    private static long removeAllCurrency(ServerPlayer player, WalletSpec currency) {
        long removed = 0L;
        for (String itemId : currency.itemIds()) {
            removed += removeAllFrom(player.getInventory().items, itemId);
            removed += removeAllFrom(player.getInventory().offhand, itemId);
        }
        if (removed > 0) {
            player.getInventory().setChanged();
        }
        return removed;
    }

    private static long countCurrencyItems(ServerPlayer player, WalletSpec currency) {
        long total = 0L;
        for (String itemId : currency.itemIds()) {
            total += ChronicleEngineItems.countItem(player, itemId);
        }
        return total;
    }

    private static boolean removeCurrencyItems(ServerPlayer player, WalletSpec currency, int count) {
        int remaining = count;
        for (String itemId : currency.itemIds()) {
            int available = ChronicleEngineItems.countItem(player, itemId);
            if (available <= 0) {
                continue;
            }
            int removed = Math.min(remaining, available);
            if (!ChronicleEngineItems.removeItem(player, itemId, removed)) {
                return false;
            }
            remaining -= removed;
            if (remaining <= 0) {
                return true;
            }
        }
        return remaining <= 0;
    }

    private static long removeAllFrom(List<ItemStack> stacks, String itemId) {
        long removed = 0L;
        for (ItemStack stack : stacks) {
            if (ChronicleEngineItems.matches(stack, itemId)) {
                removed += stack.getCount();
                stack.setCount(0);
            }
        }
        return removed;
    }

    private static void add(ServerPlayer player, String itemId, long amount) {
        WalletSpec currency = ChronicleEngineData.walletCurrencyByItem(itemId);
        String key = storageKey(currency, itemId);
        long next = Math.max(0L, balance(player, itemId) + amount);
        balances(player).putLong(key, next);
    }

    private static String storageKey(WalletSpec currency, String fallback) {
        return currency != null && !currency.itemId().isBlank() ? currency.itemId() : fallback;
    }

    private static CompoundTag balances(ServerPlayer player) {
        CompoundTag root = ChronicleEnginePlayerData.root(player);
        if (!root.contains(WALLET)) {
            root.put(WALLET, new CompoundTag());
        }
        return root.getCompound(WALLET);
    }
}

