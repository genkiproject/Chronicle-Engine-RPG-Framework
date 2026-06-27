package com.chronicle.engine.client;

import com.chronicle.engine.ChronicleEngineRegistry;
import com.chronicle.engine.network.ChronicleEngineNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.List;

public class ChronicleEngineShopScreen extends Screen {
    private static final int ROW_HEIGHT = 54;

    private ChronicleEngineNetwork.OpenShopPacket packet;
    private final long openedAt = System.currentTimeMillis();
    private final List<BuyArea> buyAreas = new ArrayList<>();
    private final List<CategoryArea> categoryAreas = new ArrayList<>();
    private final List<ItemArea> itemAreas = new ArrayList<>();
    private final List<IdArea> idAreas = new ArrayList<>();
    private String selectedCategory = "__all";
    private int scroll;
    private long categoryChangedAt = System.currentTimeMillis();

    public ChronicleEngineShopScreen(ChronicleEngineNetwork.OpenShopPacket packet) {
        super(packet.title());
        this.packet = packet;
    }

    public void updatePacket(ChronicleEngineNetwork.OpenShopPacket packet) {
        this.packet = packet;
        if (!categoryExists(selectedCategory)) {
            selectedCategory = "__all";
            categoryChangedAt = System.currentTimeMillis();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<ChronicleEngineNetwork.ShopEntryLine> entries = filteredEntries();
        int maxScroll = Math.max(0, entries.size() - visibleRows());
        scroll = Mth.clamp(scroll - (int) Math.signum(delta), 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (CategoryArea area : categoryAreas) {
            if (area.contains(mouseX, mouseY)) {
                selectedCategory = area.categoryId();
                scroll = 0;
                categoryChangedAt = System.currentTimeMillis();
                return true;
            }
        }
        for (BuyArea area : buyAreas) {
            if (area.contains(mouseX, mouseY)) {
                ChronicleEngineNetwork.buyShopEntry(packet.shopId(), area.entryId());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float progress = ease(Mth.clamp((System.currentTimeMillis() - openedAt) / 180.0F, 0.0F, 1.0F));
        graphics.fill(0, 0, width, height, argb((int) (74 * progress), 0x000000));

        int panelWidth = Math.min(820, width - 44);
        int panelHeight = Math.min(500, height - 58);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2 + (int) ((1.0F - progress) * 14.0F);
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        int categoryWidth = Math.min(142, Math.max(112, panelWidth / 5));
        int listLeft = left + categoryWidth + 12;

        graphics.fill(left, top, right, bottom, argb((int) (205 * progress), 0x08090B));
        graphics.fill(left, top, right, top + 30, argb((int) (188 * progress), 0x151922));
        graphics.fill(left, top + 30, right, top + 31, argb((int) (82 * progress), 0xD8C18A));
        graphics.fill(left + categoryWidth, top + 42, left + categoryWidth + 1, bottom - 14, argb((int) (62 * progress), 0xFFFFFF));
        graphics.drawString(font, packet.title(), left + 14, top + 11, argb((int) (245 * progress), 0xFFD46A), false);

        renderCategories(graphics, mouseX, mouseY, progress, left, top + 44, categoryWidth, bottom);
        renderEntries(graphics, mouseX, mouseY, progress, listLeft, top + 44, right, bottom);
        renderWalletSummary(graphics, progress, left + 14, bottom - 32);
        renderHoveredTooltip(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderWalletSummary(GuiGraphics graphics, float progress, int x, int y) {
        if (packet.wallet().isEmpty()) {
            return;
        }
        ChronicleEngineNetwork.WalletLine line = packet.wallet().get(0);
        int width = 132;
        int height = 24;
        graphics.fill(x, y, x + width, y + height, argb((int) (138 * progress), 0x101318));
        graphics.fill(x, y, x + 2, y + height, argb((int) (225 * progress), 0xFFD46A));
        graphics.renderItem(new ItemStack(ChronicleEngineRegistry.WALLET.get()), x + 6, y + 4);
        ItemStack currency = stack(line.itemId());
        if (!currency.isEmpty()) {
            graphics.renderItem(currency, x + 25, y + 4);
        }
        graphics.drawString(font, Long.toString(line.amount()), x + 47, y + 8, argb((int) (235 * progress), 0xFFD46A), false);
    }

    private void renderCategories(GuiGraphics graphics, int mouseX, int mouseY, float progress, int left, int top, int categoryWidth, int bottom) {
        categoryAreas.clear();
        int rowHeight = 28;
        int y = top;
        for (ChronicleEngineNetwork.ShopCategoryLine category : packet.categories()) {
            if (y + rowHeight > bottom - 18) {
                break;
            }
            boolean selected = category.categoryId().equals(selectedCategory);
            boolean hovered = mouseX >= left + 10 && mouseX <= left + categoryWidth - 10 && mouseY >= y && mouseY <= y + rowHeight - 5;
            int bg = selected ? 0x27303C : hovered ? 0x1E242D : 0x101318;
            graphics.fill(left + 10, y, left + categoryWidth - 10, y + rowHeight - 5, argb((int) ((selected ? 154 : 96) * progress), bg));
            graphics.fill(left + 10, y, left + 12, y + rowHeight - 5, argb((int) ((selected ? 220 : 70) * progress), 0xFFD46A));
            graphics.drawString(font, category.name(), left + 18, y + 8, argb((int) (selected ? 240 : 200), selected ? 0xFFD46A : 0xEDEDED), false);
            categoryAreas.add(new CategoryArea(category.categoryId(), left + 10, y, categoryWidth - 20, rowHeight - 5));
            y += rowHeight;
        }
    }

    private void renderEntries(GuiGraphics graphics, int mouseX, int mouseY, float progress, int left, int top, int right, int bottom) {
        buyAreas.clear();
        itemAreas.clear();
        idAreas.clear();
        List<ChronicleEngineNetwork.ShopEntryLine> entries = filteredEntries();
        int visibleRows = visibleRows();
        scroll = Mth.clamp(scroll, 0, Math.max(0, entries.size() - visibleRows));

        for (int i = 0; i < visibleRows; i++) {
            int index = scroll + i;
            if (index >= entries.size()) {
                break;
            }
            ChronicleEngineNetwork.ShopEntryLine entry = entries.get(index);
            float rowProgress = ease(Mth.clamp((System.currentTimeMillis() - categoryChangedAt - i * 28L) / 150.0F, 0.0F, 1.0F));
            int y = top + i * ROW_HEIGHT + (int) ((1.0F - rowProgress) * 10.0F);
            int rowAlpha = (int) (progress * rowProgress * 255.0F);
            boolean hovered = mouseX >= left && mouseX <= right - 12 && mouseY >= y && mouseY <= y + ROW_HEIGHT - 6;
            graphics.fill(left, y, right - 12, y + ROW_HEIGHT - 6, argb(Math.min(rowAlpha, hovered ? 126 : 90), hovered ? 0x1E242D : 0x101318));
            graphics.fill(left, y, left + 2, y + ROW_HEIGHT - 6, argb(Math.min(rowAlpha, hovered ? 210 : 76), 0xFFD46A));

            int iconX = left + 11;
            int iconY = y + 16;
            int shownItems = Math.min(3, entry.rewardItems().size());
            for (int itemIndex = 0; itemIndex < shownItems; itemIndex++) {
                ChronicleEngineNetwork.ShopItemLine item = entry.rewardItems().get(itemIndex);
                ItemStack stack = stack(item);
                int x = iconX + itemIndex * 22;
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, x, iconY);
                    graphics.renderItemDecorations(font, stack, x, iconY, item.count() > 1 ? Integer.toString(item.count()) : null);
                    itemAreas.add(new ItemArea(stack, x, iconY, 16, 16));
                }
                int dotX = x + 13;
                int dotY = iconY - 4;
                boolean dotHovered = mouseX >= dotX && mouseX <= dotX + 6 && mouseY >= dotY && mouseY <= dotY + 6;
                graphics.fill(dotX, dotY, dotX + 6, dotY + 6, argb(Math.min(rowAlpha, dotHovered ? 230 : 145), dotHovered ? 0xFFD46A : 0x59616C));
                graphics.fill(dotX + 2, dotY + 2, dotX + 4, dotY + 4, argb(Math.min(rowAlpha, 245), 0x0A0C0F));
                String shownId = item.enchantmentId().isBlank()
                        ? item.itemId() + (item.nbt().isBlank() ? "" : item.nbt())
                        : item.enchantmentId();
                idAreas.add(new IdArea(shownId, dotX, dotY, 6, 6));
            }

            ItemStack primary = entry.rewardItems().isEmpty() ? ItemStack.EMPTY : stack(entry.rewardItems().get(0));
            boolean nbtItem = !entry.rewardItems().isEmpty()
                    && !entry.rewardItems().get(0).enchantmentId().isBlank();
            boolean namedNbtItem = !entry.rewardItems().isEmpty()
                    && !entry.rewardItems().get(0).nbt().isBlank();
            Component itemName = primary.isEmpty() || nbtItem || namedNbtItem ? entry.name() : primary.getHoverName();
            int nameX = shownItems == 0 ? left + 12 : iconX + shownItems * 22 + 4;
            int nameWidth = Math.max(60, right - nameX - 88);
            graphics.drawString(font, trimToWidth(itemName.getString(), nameWidth), nameX, y + 8, argb(Math.min(rowAlpha, 235), 0xEFEFEF), false);
            Component cost = Component.translatable("screen.chronicle_engine.shop.cost", trim(describeItems(entry.costItems(), entry.costs()), 42));
            graphics.drawString(font, cost, nameX, y + 28, argb(Math.min(rowAlpha, 196), 0xFFD46A), false);

            int buttonWidth = 46;
            int buttonHeight = 21;
            int buttonX = right - 72;
            int buttonY = y + 14;
            boolean buttonHovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
            graphics.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, argb(Math.min(rowAlpha, 145), buttonHovered ? 0x353B45 : 0x20242B));
            graphics.drawString(font, Component.translatable("screen.chronicle_engine.shop.buy"), buttonX + 11, buttonY + 7, argb(Math.min(rowAlpha, 230), buttonHovered ? 0xFFD46A : 0xEDEDED), false);
            buyAreas.add(new BuyArea(entry.entryId(), buttonX, buttonY, buttonWidth, buttonHeight));
        }

        if (entries.size() > visibleRows) {
            graphics.drawString(font, (scroll + 1) + " / " + Math.max(1, entries.size() - visibleRows + 1), right - 58, bottom - 16, argb((int) (160 * progress), 0xB0B0B0), false);
        }
    }

    private void renderHoveredTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (IdArea area : idAreas) {
            if (area.contains(mouseX, mouseY)) {
                graphics.renderTooltip(font, Component.literal(area.itemId()).withStyle(ChatFormatting.GRAY), mouseX, mouseY);
                return;
            }
        }
        for (ItemArea area : itemAreas) {
            if (area.contains(mouseX, mouseY)) {
                graphics.renderTooltip(font, area.stack(), mouseX, mouseY);
                return;
            }
        }
    }

    private List<ChronicleEngineNetwork.ShopEntryLine> filteredEntries() {
        if ("__all".equals(selectedCategory)) {
            return packet.entries();
        }
        List<ChronicleEngineNetwork.ShopEntryLine> result = new ArrayList<>();
        for (ChronicleEngineNetwork.ShopEntryLine entry : packet.entries()) {
            if (entry.category().equals(selectedCategory)) {
                result.add(entry);
            }
        }
        return result;
    }

    private boolean categoryExists(String categoryId) {
        for (ChronicleEngineNetwork.ShopCategoryLine category : packet.categories()) {
            if (category.categoryId().equals(categoryId)) {
                return true;
            }
        }
        return false;
    }

    private int visibleRows() {
        int panelHeight = Math.min(500, height - 58);
        return Math.max(1, (panelHeight - 66) / ROW_HEIGHT);
    }

    private ItemStack stack(ChronicleEngineNetwork.ShopItemLine item) {
        ResourceLocation itemId = ResourceLocation.tryParse(item.itemId());
        if (itemId == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = BuiltInRegistries.ITEM.getOptional(itemId)
                .map(value -> new ItemStack(value, Math.max(1, item.count())))
                .orElse(ItemStack.EMPTY);
        if (!stack.isEmpty() && !item.nbt().isBlank()) {
            try {
                stack.setTag(TagParser.parseTag(item.nbt()));
            } catch (Exception ignored) {
            }
        }
        ResourceLocation enchantmentId = ResourceLocation.tryParse(item.enchantmentId());
        if (!stack.isEmpty() && enchantmentId != null
                && (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION))
                && BuiltInRegistries.POTION.containsKey(enchantmentId)) {
            PotionUtils.setPotion(stack, BuiltInRegistries.POTION.get(enchantmentId));
            return stack;
        }
        if (!stack.isEmpty() && enchantmentId != null) {
            BuiltInRegistries.ENCHANTMENT.getOptional(enchantmentId).ifPresent(enchantment ->
                    EnchantedBookItem.addEnchantment(
                            stack,
                            new EnchantmentInstance(enchantment, Math.max(1, item.enchantmentLevel()))
                    )
            );
        }
        return stack;
    }

    private ItemStack stack(String itemIdValue) {
        ResourceLocation itemId = ResourceLocation.tryParse(itemIdValue);
        if (itemId == null) {
            return ItemStack.EMPTY;
        }
        return BuiltInRegistries.ITEM.getOptional(itemId)
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    private String describeItems(List<ChronicleEngineNetwork.ShopItemLine> items, String fallback) {
        if (items.isEmpty()) {
            return fallback;
        }
        List<String> parts = new ArrayList<>();
        for (ChronicleEngineNetwork.ShopItemLine item : items) {
            ItemStack stack = stack(item);
            parts.add((stack.isEmpty() ? item.itemId() : stack.getHoverName().getString()) + " x" + item.count());
        }
        return String.join(", ", parts);
    }

    private String trimToWidth(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) {
            return value;
        }
        String result = value;
        while (!result.isEmpty() && font.width(result + "...") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    private static String trim(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 1)) + "...";
    }

    private static float ease(float value) {
        return 1.0F - (1.0F - value) * (1.0F - value);
    }

    private static int argb(int alpha, int rgb) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record BuyArea(String entryId, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private record CategoryArea(String categoryId, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private record ItemArea(ItemStack stack, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private record IdArea(String itemId, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}

