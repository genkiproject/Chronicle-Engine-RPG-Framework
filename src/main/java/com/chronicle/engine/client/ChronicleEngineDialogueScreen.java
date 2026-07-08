package com.chronicle.engine.client;

import com.chronicle.engine.network.ChronicleEngineNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class ChronicleEngineDialogueScreen extends Screen {
    private static final int MAX_VISIBLE_CHOICES = 4;

    private final ChronicleEngineNetwork.OpenDialoguePacket packet;
    private final long openedAt = System.currentTimeMillis();
    private final List<ChoiceArea> choiceAreas = new ArrayList<>();
    private int scroll;
    private boolean textRevealed;

    public ChronicleEngineDialogueScreen(ChronicleEngineNetwork.OpenDialoguePacket packet) {
        super(packet.npcName());
        this.packet = packet;
    }

    @Override
    protected void init() {
        choiceAreas.clear();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (packet.choices().size() <= MAX_VISIBLE_CHOICES) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        scroll = Mth.clamp(scroll - (int) Math.signum(delta), 0, packet.choices().size() - MAX_VISIBLE_CHOICES);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isTextComplete()) {
            textRevealed = true;
            return true;
        }
        for (ChoiceArea area : choiceAreas) {
            if (area.contains(mouseX, mouseY)) {
                ChronicleEngineNetwork.chooseDialogue(packet.dialogueId(), packet.nodeId(), area.choiceId());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        ChronicleEngineNetwork.endDialogue();
        super.onClose();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return packet.allowEscClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float progress = ease(Mth.clamp((System.currentTimeMillis() - openedAt) / 190.0F, 0.0F, 1.0F));
        renderColumns(graphics, mouseX, mouseY, progress);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderColumns(GuiGraphics graphics, int mouseX, int mouseY, float progress) {
        int totalWidth = Math.min(1440, width - 24);
        int totalLeft = (width - totalWidth) / 2;
        int gap = Math.max(12, Math.min(40, totalWidth / 30));
        int usableWidth = totalWidth - gap;
        int dialogueWidth = usableWidth * 5 / 7;
        int choiceWidth = usableWidth - dialogueWidth;
        int dialogueLeft = totalLeft;
        int dialogueRight = dialogueLeft + dialogueWidth;
        int choiceLeft = dialogueRight + gap;
        int panelHeight = Math.min(150, Math.max(126, height / 3));
        int panelTop = height - panelHeight - 18;
        int panelBottom = panelTop + panelHeight;
        int panelOffset = (int) ((1.0F - progress) * -24.0F);

        renderDialoguePanel(graphics, progress, dialogueLeft + panelOffset, dialogueRight + panelOffset, panelTop, panelBottom);
        layoutChoices(choiceLeft, panelTop, panelBottom, choiceWidth);
        renderChoices(graphics, mouseX, mouseY, progress * (isTextComplete() ? 1.0F : 0.38F), true);
    }

    private void renderDialoguePanel(GuiGraphics graphics, float progress, int left, int right, int top, int bottom) {
        graphics.fill(left, top, right, bottom, argb((int) (178 * progress), 0x050506));
        graphics.fill(left, top, right, top + 1, argb((int) (118 * progress), 0xD8C18A));
        graphics.fill(left, bottom - 1, right, bottom, argb((int) (70 * progress), 0xD8C18A));
        graphics.fill(left + 10, top + 28, right - 10, top + 29, argb((int) (42 * progress), 0xFFFFFF));

        int textColor = argb((int) (235 * progress), 0xEDEDED);
        int mutedColor = argb((int) (190 * progress), 0xB7B7B7);
        int accentColor = argb((int) (250 * progress), 0xFFD46A);
        graphics.drawString(font, packet.npcName(), left + 16, top + 10, accentColor, false);

        int textWidth = Math.max(80, right - left - 44);
        int textBottom = bottom - 14;
        List<FormattedCharSequence> lines = font.split(Component.literal(visibleDialogueText()), textWidth);
        int y = top + 39;
        for (FormattedCharSequence line : lines) {
            if (y > textBottom - 10) {
                graphics.drawString(font, "...", left + 22, y, mutedColor, false);
                break;
            }
            graphics.drawString(font, line, left + 22, y, textColor, false);
            y += 13;
        }
    }

    private void layoutChoices(int x, int panelTop, int panelBottom, int choiceWidth) {
        choiceAreas.clear();
        int visible = Math.min(MAX_VISIBLE_CHOICES, packet.choices().size());
        int maxScroll = Math.max(0, packet.choices().size() - visible);
        scroll = Mth.clamp(scroll, 0, maxScroll);
        int choiceHeight = 25;
        int gap = 7;
        int baseY = panelBottom - choiceHeight;
        for (int i = 0; i < visible; i++) {
            int index = scroll + i;
            if (index >= packet.choices().size()) {
                break;
            }
            ChronicleEngineNetwork.ChoiceLine choice = packet.choices().get(index);
            int y = baseY - i * (choiceHeight + gap);
            choiceAreas.add(new ChoiceArea(choice.choiceId(), choice.text(), x, y, choiceWidth, choiceHeight, i));
        }
    }

    private void renderChoices(GuiGraphics graphics, int mouseX, int mouseY, float progress, boolean animateFromRight) {
        int textColor = argb((int) (235 * progress), 0xEDEDED);
        int accentColor = argb((int) (250 * progress), 0xFFD46A);
        for (ChoiceArea area : choiceAreas) {
            float itemProgress = ease(Mth.clamp((System.currentTimeMillis() - openedAt - area.order() * 35L) / 180.0F, 0.0F, 1.0F));
            int offset = animateFromRight ? (int) ((1.0F - itemProgress) * 20.0F) : 0;
            int x = area.x() + offset;
            boolean hovered = mouseX >= x && mouseX <= x + area.width() && mouseY >= area.y() && mouseY <= area.y() + area.height();
            int fill = hovered ? argb((int) (132 * progress * itemProgress), 0x262A32) : argb((int) (92 * progress * itemProgress), 0x050506);
            int line = hovered ? argb((int) (220 * progress * itemProgress), 0xFFD46A) : argb((int) (88 * progress * itemProgress), 0xFFFFFF);
            graphics.fill(x, area.y(), x + area.width(), area.y() + area.height(), fill);
            graphics.fill(x, area.y(), x + 2, area.y() + area.height(), line);
            graphics.drawString(font, trimToWidth(area.text(), area.width() - 20), x + 10, area.y() + 9, hovered ? accentColor : textColor, false);
        }

        if (packet.choices().size() > MAX_VISIBLE_CHOICES && !choiceAreas.isEmpty()) {
            ChoiceArea top = choiceAreas.get(choiceAreas.size() - 1);
            Component hint = Component.literal((scroll + 1) + " / " + (packet.choices().size() - MAX_VISIBLE_CHOICES + 1));
            graphics.drawString(font, hint, top.x() + top.width() - 42, top.y() - 12, argb((int) (180 * progress), 0xB7B7B7), false);
        }
    }

    private Component trimToWidth(Component value, int maxWidth) {
        if (font.width(value) <= maxWidth) {
            return value;
        }
        String text = value.getString();
        while (!text.isEmpty() && font.width(text + "...") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return Component.literal(text + "...");
    }

    private boolean isTextComplete() {
        return textRevealed || visibleCharacterCount() >= packet.text().getString().length();
    }

    private String visibleDialogueText() {
        String text = packet.text().getString();
        if (textRevealed) {
            return text;
        }
        int visible = Mth.clamp(visibleCharacterCount(), 0, text.length());
        String result = text.substring(0, visible);
        if (visible < text.length() && (System.currentTimeMillis() / 260L) % 2L == 0L) {
            result += "|";
        }
        return result;
    }

    private int visibleCharacterCount() {
        long elapsed = System.currentTimeMillis() - openedAt;
        return (int) Math.max(0L, (elapsed - 90L) / 18L);
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

    private record ChoiceArea(String choiceId, Component text, int x, int y, int width, int height, int order) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}

