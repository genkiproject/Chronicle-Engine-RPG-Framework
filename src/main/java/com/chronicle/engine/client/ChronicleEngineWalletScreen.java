package com.chronicle.engine.client;

import com.chronicle.engine.ChronicleEngineRegistry;
import com.chronicle.engine.network.ChronicleEngineNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ChronicleEngineWalletScreen extends Screen {
    private ChronicleEngineNetwork.OpenWalletPacket packet;
    private final long openedAt = System.currentTimeMillis();
    private final List<CurrencyArea> currencyAreas = new ArrayList<>();
    private final List<ButtonArea> amountButtons = new ArrayList<>();
    private ButtonArea withdrawButton;
    private String selectedItemId = "";
    private EditBox amountBox;

    public ChronicleEngineWalletScreen(ChronicleEngineNetwork.OpenWalletPacket packet) {
        super(Component.translatable("screen.chronicle_engine.wallet"));
        this.packet = packet;
        selectDefaultCurrency();
    }

    public void updatePacket(ChronicleEngineNetwork.OpenWalletPacket packet) {
        this.packet = packet;
        if (selectedLine() == null) {
            selectDefaultCurrency();
        }
        clampAmount();
    }

    @Override
    protected void init() {
        amountBox = new EditBox(font, 0, 0, 84, 18, Component.translatable("screen.chronicle_engine.wallet.amount"));
        amountBox.setFilter(value -> value.isEmpty() || value.matches("\\d{1,12}"));
        amountBox.setValue(defaultAmount());
        addRenderableWidget(amountBox);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (CurrencyArea area : currencyAreas) {
            if (area.contains(mouseX, mouseY)) {
                selectedItemId = area.itemId();
                amountBox.setValue(defaultAmount());
                return true;
            }
        }
        for (ButtonArea area : amountButtons) {
            if (area.contains(mouseX, mouseY)) {
                applyAmountButton(area.action());
                return true;
            }
        }
        if (withdrawButton != null && withdrawButton.contains(mouseX, mouseY)) {
            ChronicleEngineNetwork.withdrawWallet(selectedItemId, requestedAmount());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float progress = ease(Mth.clamp((System.currentTimeMillis() - openedAt) / 180.0F, 0.0F, 1.0F));
        graphics.fill(0, 0, width, height, argb((int) (88 * progress), 0x000000));

        int panelWidth = Math.min(460, width - 38);
        int panelHeight = Math.min(270, height - 44);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2 + (int) ((1.0F - progress) * 12.0F);
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        graphics.fill(left, top, right, bottom, argb((int) (212 * progress), 0x08090B));
        graphics.fill(left, top, right, top + 32, argb((int) (190 * progress), 0x151922));
        graphics.fill(left, top + 32, right, top + 33, argb((int) (96 * progress), 0xD8C18A));
        graphics.drawString(font, title, left + 38, top + 12, argb((int) (245 * progress), 0xFFD46A), false);
        graphics.renderItem(new ItemStack(ChronicleEngineRegistry.WALLET.get()), left + 14, top + 8);

        renderCurrencyList(graphics, mouseX, mouseY, progress, left + 14, top + 48, left + 188, bottom - 18);
        renderControls(graphics, mouseX, mouseY, progress, left + 208, top + 50, right - 18, bottom - 18);
        renderHoveredTooltip(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderCurrencyList(GuiGraphics graphics, int mouseX, int mouseY, float progress, int left, int top, int right, int bottom) {
        currencyAreas.clear();
        int y = top;
        for (ChronicleEngineNetwork.WalletLine line : packet.wallet()) {
            if (y + 38 > bottom) {
                break;
            }
            boolean selected = line.itemId().equals(selectedItemId);
            boolean hovered = mouseX >= left && mouseX <= right && mouseY >= y && mouseY <= y + 32;
            graphics.fill(left, y, right, y + 32, argb((int) ((selected ? 154 : hovered ? 116 : 86) * progress), selected ? 0x27303C : 0x101318));
            graphics.fill(left, y, left + 2, y + 32, argb((int) ((selected ? 220 : 75) * progress), 0xFFD46A));
            ItemStack stack = stack(line.itemId());
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, left + 8, y + 8);
            }
            graphics.drawString(font, trim(line.name().getString(), 15), left + 30, y + 6, argb((int) (235 * progress), 0xEDEDED), false);
            graphics.drawString(font, Long.toString(line.amount()), left + 30, y + 19, argb((int) (205 * progress), 0xFFD46A), false);
            currencyAreas.add(new CurrencyArea(line.itemId(), left, y, right - left, 32));
            y += 38;
        }
    }

    private void renderControls(GuiGraphics graphics, int mouseX, int mouseY, float progress, int left, int top, int right, int bottom) {
        amountButtons.clear();
        withdrawButton = null;
        ChronicleEngineNetwork.WalletLine line = selectedLine();
        if (line == null) {
            graphics.drawString(font, Component.translatable("screen.chronicle_engine.wallet.no_currency"), left, top, argb((int) (210 * progress), 0xCFCFCF), false);
            return;
        }
        graphics.drawString(font, line.name(), left, top, argb((int) (245 * progress), 0xFFD46A), false);
        graphics.drawString(font, Component.translatable("screen.chronicle_engine.wallet.balance", line.amount()), left, top + 16, argb((int) (210 * progress), 0xD8D8D8), false);
        graphics.drawString(font, Component.translatable("screen.chronicle_engine.wallet.amount"), left, top + 43, argb((int) (190 * progress), 0xB7B7B7), false);
        if (amountBox != null) {
            amountBox.setX(left);
            amountBox.setY(top + 56);
            amountBox.setWidth(Math.min(96, right - left));
        }

        int y = top + 84;
        renderAmountButton(graphics, mouseX, mouseY, progress, left, y, 40, "-64");
        renderAmountButton(graphics, mouseX, mouseY, progress, left + 45, y, 34, "-1");
        renderAmountButton(graphics, mouseX, mouseY, progress, left + 84, y, 34, "+1");
        renderAmountButton(graphics, mouseX, mouseY, progress, left + 123, y, 40, "+64");
        renderAmountButton(graphics, mouseX, mouseY, progress, left, y + 28, 60, "all");
        renderAmountButton(graphics, mouseX, mouseY, progress, left + 65, y + 28, 60, "half");

        int buttonWidth = Math.min(138, right - left);
        int buttonY = bottom - 28;
        boolean hovered = mouseX >= left && mouseX <= left + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + 24;
        graphics.fill(left, buttonY, left + buttonWidth, buttonY + 24, argb((int) ((hovered ? 170 : 135) * progress), hovered ? 0x353B45 : 0x20242B));
        graphics.fill(left, buttonY, left + 2, buttonY + 24, argb((int) (225 * progress), 0xFFD46A));
        graphics.drawString(font, Component.translatable("screen.chronicle_engine.wallet.withdraw"), left + 12, buttonY + 8, argb((int) (235 * progress), hovered ? 0xFFD46A : 0xEDEDED), false);
        withdrawButton = new ButtonArea(left, buttonY, buttonWidth, 24, "withdraw");
    }

    private void renderAmountButton(GuiGraphics graphics, int mouseX, int mouseY, float progress, int x, int y, int width, String action) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 20;
        graphics.fill(x, y, x + width, y + 20, argb((int) ((hovered ? 160 : 110) * progress), hovered ? 0x283344 : 0x141923));
        Component label = switch (action) {
            case "all" -> Component.translatable("screen.chronicle_engine.wallet.all");
            case "half" -> Component.translatable("screen.chronicle_engine.wallet.half");
            default -> Component.literal(action);
        };
        graphics.drawString(font, label, x + 7, y + 6, argb((int) (225 * progress), hovered ? 0xFFD46A : 0xEDEDED), false);
        amountButtons.add(new ButtonArea(x, y, width, 20, action));
    }

    private void renderHoveredTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (CurrencyArea area : currencyAreas) {
            if (area.contains(mouseX, mouseY)) {
                graphics.renderTooltip(font, Component.literal(area.itemId()).withStyle(ChatFormatting.GRAY), mouseX, mouseY);
                return;
            }
        }
    }

    private void applyAmountButton(String action) {
        ChronicleEngineNetwork.WalletLine line = selectedLine();
        if (line == null) {
            return;
        }
        long current = requestedAmount();
        long next = switch (action) {
            case "-64" -> current - 64;
            case "-1" -> current - 1;
            case "+1" -> current + 1;
            case "+64" -> current + 64;
            case "half" -> Math.max(1L, line.amount() / 2L);
            case "all" -> line.amount();
            default -> current;
        };
        amountBox.setValue(Long.toString(clampLong(next, 1L, Math.max(1L, line.amount()))));
    }

    private void clampAmount() {
        ChronicleEngineNetwork.WalletLine line = selectedLine();
        if (amountBox == null || line == null) {
            return;
        }
        amountBox.setValue(Long.toString(clampLong(requestedAmount(), 1L, Math.max(1L, line.amount()))));
    }

    private long requestedAmount() {
        if (amountBox == null || amountBox.getValue().isBlank()) {
            return 1L;
        }
        try {
            return Math.max(1L, Long.parseLong(amountBox.getValue()));
        } catch (NumberFormatException exception) {
            return 1L;
        }
    }

    private String defaultAmount() {
        ChronicleEngineNetwork.WalletLine line = selectedLine();
        if (line == null || line.amount() <= 0) {
            return "1";
        }
        return Long.toString(Math.min(64L, line.amount()));
    }

    private ChronicleEngineNetwork.WalletLine selectedLine() {
        for (ChronicleEngineNetwork.WalletLine line : packet.wallet()) {
            if (line.itemId().equals(selectedItemId)) {
                return line;
            }
        }
        return packet.wallet().isEmpty() ? null : packet.wallet().get(0);
    }

    private void selectDefaultCurrency() {
        selectedItemId = packet.wallet().isEmpty() ? "" : packet.wallet().get(0).itemId();
    }

    private ItemStack stack(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        return BuiltInRegistries.ITEM.getOptional(id)
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    private static String trim(String value, int max) {
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 1)) + "...";
    }

    private static float ease(float value) {
        return 1.0F - (1.0F - value) * (1.0F - value);
    }

    private static long clampLong(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int argb(int alpha, int rgb) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record CurrencyArea(String itemId, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private record ButtonArea(int x, int y, int width, int height, String action) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}

