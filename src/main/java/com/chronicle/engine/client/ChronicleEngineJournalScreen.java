package com.chronicle.engine.client;

import com.chronicle.engine.ChronicleEngineQuestService;
import com.chronicle.engine.ChronicleEngineRegistry;
import com.chronicle.engine.network.ChronicleEngineNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ChronicleEngineJournalScreen extends Screen {
    private ChronicleEngineNetwork.OpenJournalPacket packet;
    private final long openedAt = System.currentTimeMillis();
    private final List<RowArea> questRows = new ArrayList<>();
    private final List<NodeArea> nodeAreas = new ArrayList<>();
    private ButtonArea offerButton;
    private ButtonArea routeButton;
    private ButtonArea trackButton;
    private ButtonArea walletButton;
    private ButtonArea teamButton;
    private String selectedQuestId;
    private String selectedNodeId;
    private int listScroll;
    private int detailScroll;
    private int detailContentHeight;
    private int detailViewportHeight;
    private boolean routeMode;
    private long routeOpenedAt;
    private double routeOffsetX;
    private double routeOffsetY;
    private boolean draggingRoute;
    private double lastMouseX;
    private double lastMouseY;
    private int refreshTicks;

    public ChronicleEngineJournalScreen(ChronicleEngineNetwork.OpenJournalPacket packet) {
        super(Component.translatable("screen.chronicle_engine.quest_journal"));
        this.packet = packet;
        selectDefaultQuest();
    }

    public void updatePacket(ChronicleEngineNetwork.OpenJournalPacket packet) {
        this.packet = packet;
        if (selectedQuest() == null) {
            selectDefaultQuest();
        }
    }

    @Override
    public void tick() {
        refreshTicks++;
        if (refreshTicks % 20 == 0) {
            ChronicleEngineNetwork.requestJournal();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (routeMode) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        int panelWidth = Math.min(980, width - 44);
        int left = (width - panelWidth) / 2;
        int listWidth = Math.min(286, panelWidth / 3);
        if (mouseX > left + listWidth + 10) {
            int max = Math.max(0, detailContentHeight - detailViewportHeight);
            detailScroll = Mth.clamp(detailScroll - (int) (delta * 22), 0, max);
            return true;
        }
        int visible = visibleRows();
        if (packet.quests().size() > visible) {
            listScroll = Mth.clamp(listScroll - (int) Math.signum(delta), 0, packet.quests().size() - visible);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (walletButton != null && walletButton.contains(mouseX, mouseY)) {
            ChronicleEngineNetwork.requestWallet();
            return true;
        }
        if (teamButton != null && teamButton.contains(mouseX, mouseY)) {
            ChronicleEngineNetwork.requestTeams();
            return true;
        }
        if (routeButton != null && routeButton.contains(mouseX, mouseY)) {
            routeMode = !routeMode;
            routeOpenedAt = System.currentTimeMillis();
            if (routeMode) {
                selectDefaultNode();
            }
            return true;
        }
        if (routeMode) {
            for (NodeArea area : nodeAreas) {
                if (area.contains(mouseX, mouseY)) {
                    selectedNodeId = area.phaseId();
                    return true;
                }
            }
            draggingRoute = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        for (RowArea row : questRows) {
            if (row.contains(mouseX, mouseY)) {
                selectedQuestId = row.questId();
                selectedNodeId = "";
                detailScroll = 0;
                return true;
            }
        }
        if (offerButton != null && offerButton.contains(mouseX, mouseY)) {
            ChronicleEngineQuestService.JournalQuest quest = selectedQuest();
            if (quest != null) {
                ChronicleEngineNetwork.offerQuest(quest.questId());
            }
            return true;
        }
        if (trackButton != null && trackButton.contains(mouseX, mouseY)) {
            ChronicleEngineQuestService.JournalQuest quest = selectedQuest();
            if (quest != null && quest.active()) {
                ChronicleEngineNetwork.toggleTrack(quest.questId());
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (routeMode && draggingRoute) {
            routeOffsetX += mouseX - lastMouseX;
            routeOffsetY += mouseY - lastMouseY;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingRoute = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        float progress = ease(Mth.clamp((System.currentTimeMillis() - openedAt) / 180.0F, 0.0F, 1.0F));
        float viewProgress = routeMode ? ease(Mth.clamp((System.currentTimeMillis() - routeOpenedAt) / 180.0F, 0.0F, 1.0F)) : 1.0F;
        graphics.fill(0, 0, width, height, argb((int) (92 * progress), 0x000000));

        int panelWidth = Math.min(980, width - 44);
        int panelHeight = Math.min(560, height - 48);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2 + (int) ((1.0F - progress) * 14.0F);
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        graphics.fill(left, top, right, bottom, argb((int) (205 * progress), 0x08090B));
        graphics.fill(left, top, right, top + 30, argb((int) (188 * progress), 0x151922));
        graphics.fill(left, top + 30, right, top + 31, argb((int) (82 * progress), 0xD8C18A));
        graphics.drawString(font, routeMode ? Component.literal("节点路线") : title, left + 14, top + 11, argb((int) (245 * progress), 0xFFD46A), false);
        renderRouteButton(graphics, mouseX, mouseY, progress, right - 42, top + 5);
        renderTeamButton(graphics, mouseX, mouseY, progress, right - 76, top + 5);
        renderWalletButton(graphics, mouseX, mouseY, progress, left + 14, bottom - 30);

        if (routeMode) {
            renderRouteView(graphics, mouseX, mouseY, viewProgress, left + 18, top + 46, right - 18, bottom - 18);
        } else {
            int listWidth = Math.min(286, panelWidth / 3);
            int detailLeft = left + listWidth + 14;
            graphics.fill(left + listWidth, top + 42, left + listWidth + 1, bottom - 14, argb((int) (70 * progress), 0xFFFFFF));
            renderQuestList(graphics, mouseX, mouseY, progress, left, top + 44, listWidth, bottom - 16);
            renderQuestDetail(graphics, mouseX, mouseY, progress, detailLeft, top + 48, right - 18, bottom - 18);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderWalletButton(GuiGraphics graphics, int mouseX, int mouseY, float progress, int x, int y) {
        int buttonWidth = 76;
        int buttonHeight = 22;
        boolean hovered = mouseX >= x && mouseX <= x + buttonWidth && mouseY >= y && mouseY <= y + buttonHeight;
        graphics.fill(x, y, x + buttonWidth, y + buttonHeight, argb((int) ((hovered ? 175 : 128) * progress), hovered ? 0x283344 : 0x141923));
        graphics.fill(x, y, x + 2, y + buttonHeight, argb((int) (225 * progress), 0xFFD46A));
        graphics.renderItem(new ItemStack(ChronicleEngineRegistry.WALLET.get()), x + 6, y + 3);
        graphics.drawString(font, Component.translatable("screen.chronicle_engine.wallet.short"), x + 28, y + 7, argb((int) (235 * progress), hovered ? 0xFFD46A : 0xEDEDED), false);
        walletButton = new ButtonArea(x, y, buttonWidth, buttonHeight);
        if (hovered) {
            graphics.renderTooltip(font, Component.translatable("screen.chronicle_engine.wallet.open"), mouseX, mouseY);
        }
    }

    private void renderRouteButton(GuiGraphics graphics, int mouseX, int mouseY, float progress, int x, int y) {
        boolean hovered = mouseX >= x && mouseX <= x + 28 && mouseY >= y && mouseY <= y + 22;
        int alpha = (int) ((hovered ? 205 : 145) * progress);
        graphics.fill(x, y, x + 28, y + 22, argb(alpha, hovered ? 0x283344 : 0x141923));
        graphics.drawString(font, routeMode ? "×" : "◎", x + (routeMode ? 10 : 8), y + 7, argb((int) (245 * progress), 0xFFD46A), false);
        graphics.drawString(font, routeMode ? "" : "◎", x + 13, y + 7, argb((int) (155 * progress), 0x9ED8FF), false);
        routeButton = new ButtonArea(x, y, 28, 22);
    }

    private void renderTeamButton(GuiGraphics graphics, int mouseX, int mouseY, float progress, int x, int y) {
        boolean hovered = mouseX >= x && mouseX <= x + 28 && mouseY >= y && mouseY <= y + 22;
        graphics.fill(x, y, x + 28, y + 22, argb((int) ((hovered ? 205 : 145) * progress), hovered ? 0x283344 : 0x141923));
        int color = argb((int) (245 * progress), hovered ? 0xFFD46A : 0x9ED8FF);
        graphics.fill(x + 8, y + 7, x + 13, y + 12, color);
        graphics.fill(x + 15, y + 7, x + 20, y + 12, color);
        graphics.fill(x + 6, y + 14, x + 22, y + 16, color);
        graphics.fill(x + 9, y + 4, x + 19, y + 5, argb((int) (180 * progress), 0xFFD46A));
        teamButton = new ButtonArea(x, y, 28, 22);
        if (hovered) {
            graphics.renderTooltip(font, Component.translatable("screen.chronicle_engine.team.open"), mouseX, mouseY);
        }
    }

    private void renderQuestList(GuiGraphics graphics, int mouseX, int mouseY, float progress, int left, int top, int listWidth, int bottom) {
        questRows.clear();
        int rowHeight = 46;
        int visible = visibleRows();
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, packet.quests().size() - visible));
        for (int i = 0; i < visible; i++) {
            int index = listScroll + i;
            if (index >= packet.quests().size()) {
                break;
            }
            ChronicleEngineQuestService.JournalQuest quest = packet.quests().get(index);
            int y = top + i * rowHeight;
            boolean selected = quest.questId().equals(selectedQuestId);
            boolean hovered = mouseX >= left + 10 && mouseX <= left + listWidth - 10 && mouseY >= y && mouseY <= y + rowHeight - 6;
            int bg = selected ? 0x27303C : hovered ? 0x1E242D : 0x101318;
            graphics.fill(left + 10, y, left + listWidth - 10, y + rowHeight - 6, argb((int) ((selected ? 162 : 104) * progress), bg));
            graphics.fill(left + 10, y, left + 12, y + rowHeight - 6, argb((int) ((selected ? 230 : 70) * progress), selected ? 0xFFD46A : 0xFFFFFF));
            int titleColor = quest.active() ? 0xF0F0F0 : quest.completed() ? 0x9CE29C : 0xA8A8A8;
            graphics.drawString(font, trim(quest.title(), 24), left + 18, y + 8, argb((int) (238 * progress), titleColor), false);
            if (quest.tracked()) {
                graphics.fill(left + listWidth - 24, y + 8, left + listWidth - 18, y + 14, argb((int) (220 * progress), 0xFFD46A));
                graphics.fill(left + listWidth - 22, y + 10, left + listWidth - 20, y + 12, argb((int) (245 * progress), 0x101318));
            }
            String state = quest.active() ? "进行中" : quest.completed() ? "已完成" : "可接取";
            graphics.drawString(font, state, left + 18, y + 24, argb((int) (180 * progress), quest.active() ? 0x9ED8FF : 0xB0B0B0), false);
            questRows.add(new RowArea(quest.questId(), left + 10, y, listWidth - 20, rowHeight - 6));
        }
    }

    private void renderQuestDetail(GuiGraphics graphics, int mouseX, int mouseY, float progress, int left, int top, int right, int bottom) {
        offerButton = null;
        trackButton = null;
        ChronicleEngineQuestService.JournalQuest quest = selectedQuest();
        if (quest == null) {
            graphics.drawString(font, "暂无可显示任务", left, top, argb((int) (210 * progress), 0xD0D0D0), false);
            return;
        }

        int contentRight = right - 28;
        int width = contentRight - left;
        detailViewportHeight = bottom - top - 30;
        int max = Math.max(0, detailContentHeight - detailViewportHeight);
        detailScroll = Mth.clamp(detailScroll, 0, max);
        int y = top - detailScroll;
        graphics.enableScissor(left, top, contentRight, bottom - 28);
        int titleColor = quest.active() ? 0xFFFFFF : quest.completed() ? 0x9CE29C : 0xC8C8C8;
        graphics.drawString(font, font.plainSubstrByWidth(quest.title(), Math.max(80, width - 58)), left, y, argb((int) (245 * progress), titleColor), false);
        graphics.drawString(font, quest.active() ? "当前状态：进行中" : quest.completed() ? "当前状态：已完成" : "当前状态：可接取", left, y + 16, argb((int) (190 * progress), 0xB7B7B7), false);

        y += 40;
        y = drawWrapped(graphics, quest.description(), left, y, width, 99, argb((int) (210 * progress), 0xD8D8D8));

        if (!quest.phaseTitle().isBlank()) {
            y += 12;
            graphics.fill(left, y - 4, contentRight, y + 39, argb((int) (86 * progress), 0x11151C));
            graphics.drawString(font, "当前节点", left + 10, y + 2, argb((int) (205 * progress), 0xFFD46A), false);
            graphics.drawString(font, quest.phaseTitle(), left + 10, y + 16, argb((int) (240 * progress), 0xEDEDED), false);
            if (!quest.phaseId().isBlank()) {
                graphics.drawString(font, quest.phaseId(), contentRight - Math.min(font.width(quest.phaseId()), 220), y + 2, argb((int) (120 * progress), 0x9A9A9A), false);
            }
            y += 50;
        }

        if (!quest.phaseDescription().isBlank()) {
            y = drawWrapped(graphics, quest.phaseDescription(), left, y, width, 99, argb((int) (205 * progress), 0xCFCFCF));
            y += 8;
        }

        boolean hasOffer = false;
        for (ChronicleEngineQuestService.JournalObjective objective : quest.objectives()) {
            int barWidth = Math.min(300, width - 76);
            int complete = objective.required() <= 0 ? 0 : Mth.clamp((int) ((objective.progress() / (float) objective.required()) * barWidth), 0, barWidth);
            graphics.drawString(font, objective.text(), left, y, argb((int) (232 * progress), 0xEFEFEF), false);
            graphics.drawString(font, objective.progress() + " / " + objective.required(), contentRight - 58, y, argb((int) (190 * progress), 0xB7B7B7), false);
            y += 14;
            graphics.fill(left, y, left + barWidth, y + 5, argb((int) (95 * progress), 0xFFFFFF));
            graphics.fill(left, y, left + complete, y + 5, argb((int) (210 * progress), objective.progress() >= objective.required() ? 0x8EE28E : 0xFFD46A));
            y += 20;
            if ("OFFER".equalsIgnoreCase(objective.type()) && objective.progress() < objective.required()) {
                hasOffer = true;
            }
        }
        detailContentHeight = Math.max(0, y - (top - detailScroll));
        graphics.disableScissor();

        if (detailContentHeight > detailViewportHeight) {
            int trackX = right - 5;
            int trackTop = top;
            int trackBottom = bottom - 34;
            int thumbHeight = Math.max(18, (int) ((detailViewportHeight / (float) detailContentHeight) * (trackBottom - trackTop)));
            int thumbY = trackTop + (int) ((detailScroll / (float) Math.max(1, detailContentHeight - detailViewportHeight)) * (trackBottom - trackTop - thumbHeight));
            graphics.fill(trackX, trackTop, trackX + 2, trackBottom, argb((int) (80 * progress), 0xFFFFFF));
            graphics.fill(trackX - 1, thumbY, trackX + 3, thumbY + thumbHeight, argb((int) (190 * progress), 0xFFD46A));
        }

        if (quest.active()) {
            renderTrackButton(graphics, mouseX, mouseY, progress, quest, contentRight - 24, top);
        }

        if (quest.active() && hasOffer) {
            int buttonWidth = 152;
            int buttonHeight = 24;
            int x = contentRight - buttonWidth;
            int yButton = bottom - buttonHeight;
            boolean hovered = mouseX >= x && mouseX <= x + buttonWidth && mouseY >= yButton && mouseY <= yButton + buttonHeight;
            int bg = hovered ? 0x353B45 : 0x20242B;
            graphics.fill(x, yButton, x + buttonWidth, yButton + buttonHeight, argb((int) (150 * progress), bg));
            graphics.fill(x, yButton, x + 2, yButton + buttonHeight, argb((int) (220 * progress), 0xFFD46A));
            graphics.drawString(font, "提交当前节点所需物品", x + 10, yButton + 8, argb((int) (235 * progress), hovered ? 0xFFD46A : 0xEDEDED), false);
            offerButton = new ButtonArea(x, yButton, buttonWidth, buttonHeight);
        }
    }

    private void renderTrackButton(GuiGraphics graphics, int mouseX, int mouseY, float progress, ChronicleEngineQuestService.JournalQuest quest, int x, int y) {
        int size = 20;
        boolean hovered = mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
        int trackedCount = trackedCount();
        int background = quest.tracked() ? 0x3A3320 : hovered ? 0x283344 : 0x171B22;
        int accent = quest.tracked() ? 0xFFD46A : 0x9ED8FF;
        graphics.fill(x, y, x + size, y + size, argb((int) ((hovered ? 205 : 150) * progress), background));
        graphics.fill(x, y, x + 2, y + size, argb((int) (225 * progress), accent));
        graphics.drawString(font, quest.tracked() ? "-" : "+", x + 8, y + 6, argb((int) (240 * progress), accent), false);
        graphics.drawString(font, trackedCount + "/2", x - 24, y + 6, argb((int) (150 * progress), 0xB7B7B7), false);
        trackButton = new ButtonArea(x, y, size, size);
        if (hovered) {
            graphics.renderTooltip(font, Component.translatable(quest.tracked()
                    ? "screen.chronicle_engine.quest.untrack"
                    : "screen.chronicle_engine.quest.track"), mouseX, mouseY);
        }
    }

    private int trackedCount() {
        int count = 0;
        for (ChronicleEngineQuestService.JournalQuest quest : packet.quests()) {
            if (quest.tracked()) {
                count++;
            }
        }
        return count;
    }

    private void renderRouteView(GuiGraphics graphics, int mouseX, int mouseY, float progress, int left, int top, int right, int bottom) {
        ChronicleEngineQuestService.JournalQuest quest = selectedQuest();
        if (quest == null) {
            return;
        }
        if (selectedNodeId == null || selectedNodeId.isBlank()) {
            selectDefaultNode();
        }
        int detailWidth = 300;
        int graphRight = right - detailWidth - 16;
        graphics.fill(graphRight + 8, top, graphRight + 9, bottom, argb((int) (70 * progress), 0xFFFFFF));
        graphics.enableScissor(left, top, graphRight, bottom);
        nodeAreas.clear();
        int centerX = (left + graphRight) / 2 + (int) routeOffsetX;
        int startY = top + 44 + (int) routeOffsetY;
        int nodeW = 250;
        int nodeH = 42;
        for (int i = 0; i < quest.nodes().size(); i++) {
            int x = centerX - nodeW / 2;
            int y = startY + i * 78;
            if (i > 0) {
                int lineX = centerX;
                graphics.fill(lineX - 1, y - 36, lineX + 1, y, argb((int) (95 * progress), 0xFFFFFF));
            }
            ChronicleEngineQuestService.JournalNode node = quest.nodes().get(i);
            boolean selected = node.phaseId().equals(selectedNodeId);
            boolean hovered = mouseX >= x && mouseX <= x + nodeW && mouseY >= y && mouseY <= y + nodeH;
            int accent = nodeColor(node.status());
            graphics.fill(x, y, x + nodeW, y + nodeH, argb((int) ((selected ? 165 : hovered ? 128 : 102) * progress), selected ? 0x27303C : 0x101318));
            graphics.fill(x, y, x + 3, y + nodeH, argb((int) (220 * progress), accent));
            graphics.drawString(font, trim(node.title(), 28), x + 12, y + 8, argb((int) (235 * progress), selected ? 0xFFFFFF : 0xEDEDED), false);
            graphics.drawString(font, nodeStatusText(node.status()), x + 12, y + 24, argb((int) (200 * progress), accent), false);
            nodeAreas.add(new NodeArea(node.phaseId(), x, y, nodeW, nodeH));
        }
        graphics.disableScissor();
        renderNodeDetail(graphics, progress, selectedNode(quest), graphRight + 26, top + 4, right, bottom);
    }

    private void renderNodeDetail(GuiGraphics graphics, float progress, ChronicleEngineQuestService.JournalNode node, int left, int top, int right, int bottom) {
        if (node == null) {
            return;
        }
        int y = top;
        graphics.drawString(font, node.title(), left, y, argb((int) (245 * progress), 0xFFD46A), false);
        y += 14;
        graphics.drawString(font, nodeStatusText(node.status()) + "  " + node.phaseId(), left, y, argb((int) (160 * progress), 0xB7B7B7), false);
        y += 22;
        y = drawWrapped(graphics, node.description(), left, y, right - left, 8, argb((int) (215 * progress), 0xD8D8D8));
        y += 10;
        for (ChronicleEngineQuestService.JournalObjective objective : node.objectives()) {
            if (y > bottom - 28) {
                graphics.drawString(font, "...", left, y, argb((int) (180 * progress), 0xB7B7B7), false);
                break;
            }
            graphics.drawString(font, objective.text(), left, y, argb((int) (230 * progress), 0xEFEFEF), false);
            y += 12;
            graphics.drawString(font, objective.type() + "  " + objective.progress() + "/" + objective.required(), left + 8, y, argb((int) (170 * progress), 0xB7B7B7), false);
            y += 18;
        }
    }

    private int drawWrapped(GuiGraphics graphics, String text, int x, int y, int width, int maxLines, int color) {
        if (text == null || text.isBlank()) {
            return y;
        }
        List<FormattedCharSequence> lines = font.split(Component.literal(text), width);
        int count = 0;
        for (FormattedCharSequence line : lines) {
            if (count >= maxLines) {
                graphics.drawString(font, "...", x, y, color, false);
                y += 12;
                break;
            }
            graphics.drawString(font, line, x, y, color, false);
            y += 12;
            count++;
        }
        return y;
    }

    private ChronicleEngineQuestService.JournalQuest selectedQuest() {
        for (ChronicleEngineQuestService.JournalQuest quest : packet.quests()) {
            if (quest.questId().equals(selectedQuestId)) {
                return quest;
            }
        }
        return null;
    }

    private ChronicleEngineQuestService.JournalNode selectedNode(ChronicleEngineQuestService.JournalQuest quest) {
        for (ChronicleEngineQuestService.JournalNode node : quest.nodes()) {
            if (node.phaseId().equals(selectedNodeId)) {
                return node;
            }
        }
        return quest.nodes().isEmpty() ? null : quest.nodes().get(0);
    }

    private void selectDefaultQuest() {
        for (ChronicleEngineQuestService.JournalQuest quest : packet.quests()) {
            if (quest.active()) {
                selectedQuestId = quest.questId();
                return;
            }
        }
        selectedQuestId = packet.quests().isEmpty() ? "" : packet.quests().get(0).questId();
    }

    private void selectDefaultNode() {
        ChronicleEngineQuestService.JournalQuest quest = selectedQuest();
        if (quest == null || quest.nodes().isEmpty()) {
            selectedNodeId = "";
            return;
        }
        for (ChronicleEngineQuestService.JournalNode node : quest.nodes()) {
            if ("current".equals(node.status())) {
                selectedNodeId = node.phaseId();
                return;
            }
        }
        selectedNodeId = quest.nodes().get(0).phaseId();
    }

    private int visibleRows() {
        int panelHeight = Math.min(560, height - 48);
        return Math.max(1, (panelHeight - 70) / 46);
    }

    private static int nodeColor(String status) {
        return switch (status) {
            case "completed" -> 0x8EE28E;
            case "current" -> 0xFFD46A;
            case "next" -> 0x9ED8FF;
            default -> 0x8A8A8A;
        };
    }

    private static String nodeStatusText(String status) {
        return switch (status) {
            case "completed" -> "历史节点";
            case "current" -> "当前节点";
            case "next" -> "下一步";
            default -> "未抵达";
        };
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

    private record RowArea(String questId, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private record ButtonArea(int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }

    private record NodeArea(String phaseId, int x, int y, int width, int height) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}

