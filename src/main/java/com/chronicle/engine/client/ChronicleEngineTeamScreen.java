package com.chronicle.engine.client;

import com.chronicle.engine.network.ChronicleEngineNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class ChronicleEngineTeamScreen extends Screen {
    private static final int BUTTON_HEIGHT = 16;
    private static final int CONTENT_GAP = 10;

    private ChronicleEngineNetwork.OpenTeamsPacket packet;
    private final long openedAt = System.currentTimeMillis();
    private final List<ButtonArea> buttons = new ArrayList<>();
    private String selectedTeamId = "";
    private boolean notificationTab;
    private int listScroll;
    private int detailScroll;
    private int refreshTicks;

    public ChronicleEngineTeamScreen(ChronicleEngineNetwork.OpenTeamsPacket packet) {
        super(Component.translatable("screen.chronicle_engine.team"));
        this.packet = packet;
        selectDefaultTeam();
    }

    public void updatePacket(ChronicleEngineNetwork.OpenTeamsPacket packet) {
        this.packet = packet;
        if (selectedTeam() == null) {
            selectDefaultTeam();
        }
    }

    @Override
    public void tick() {
        refreshTicks++;
        if (refreshTicks % 40 == 0) {
            ChronicleEngineNetwork.requestTeams();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < width / 2.0D) {
            listScroll = Mth.clamp(listScroll - (int) Math.signum(delta), 0, Math.max(0, packet.teams().size() - 8));
        } else {
            detailScroll = Mth.clamp(detailScroll - (int) (delta * 14), 0, 900);
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (ButtonArea area : List.copyOf(buttons)) {
            if (!area.enabled || !area.contains(mouseX, mouseY)) {
                continue;
            }
            if ("TAB_TEAMS".equals(area.action)) {
                notificationTab = false;
                detailScroll = 0;
            } else if ("TAB_NOTIFICATIONS".equals(area.action)) {
                notificationTab = true;
                detailScroll = 0;
            } else if ("REFRESH".equals(area.action)) {
                ChronicleEngineNetwork.requestTeams();
            } else if ("SELECT".equals(area.action)) {
                selectedTeamId = area.teamId;
                notificationTab = false;
                detailScroll = 0;
            } else {
                ChronicleEngineNetwork.teamAction(area.action, area.teamId, area.targetId);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        buttons.clear();
        float progress = ease(Mth.clamp((System.currentTimeMillis() - openedAt) / 180.0F, 0.0F, 1.0F));
        graphics.fill(0, 0, width, height, argb((int) (94 * progress), 0x000000));

        int panelWidth = Math.min(900, width - 18);
        int panelHeight = Math.min(520, height - 18);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2 + (int) ((1.0F - progress) * 8.0F);
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        int headerHeight = 26;

        graphics.fill(left, top, right, bottom, argb((int) (212 * progress), 0x08090B));
        graphics.fill(left, top, right, top + headerHeight, argb((int) (192 * progress), 0x151922));
        graphics.fill(left, top + headerHeight, right, top + headerHeight + 1, argb((int) (92 * progress), 0xD8C18A));
        graphics.drawString(font, title, left + 12, top + 9, argb((int) (245 * progress), 0xFFD46A), false);
        if (packet.serverAdmin()) {
            graphics.drawString(font, Component.literal("OP"), left + 48, top + 9, argb((int) (220 * progress), 0x9ED8FF), false);
        }

        renderTopButtons(graphics, mouseX, mouseY, progress, right - 176, top + 5);

        int listWidth = Mth.clamp((int) (panelWidth * 0.40F), 150, 250);
        int contentTop = top + headerHeight + CONTENT_GAP;
        int contentBottom = bottom - CONTENT_GAP;
        graphics.fill(left + listWidth, contentTop, left + listWidth + 1, contentBottom, argb((int) (70 * progress), 0xFFFFFF));
        renderTeamList(graphics, mouseX, mouseY, progress, left + CONTENT_GAP, contentTop, listWidth - CONTENT_GAP * 2, contentBottom);
        if (notificationTab) {
            renderNotifications(graphics, mouseX, mouseY, progress, left + listWidth + CONTENT_GAP, contentTop, right - CONTENT_GAP, contentBottom);
        } else {
            renderTeamDetail(graphics, mouseX, mouseY, progress, left + listWidth + CONTENT_GAP, contentTop, right - CONTENT_GAP, contentBottom);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTopButtons(GuiGraphics graphics, int mouseX, int mouseY, float progress, int x, int y) {
        renderButton(graphics, mouseX, mouseY, progress, x, y, 58, Component.translatable("screen.chronicle_engine.team.create"), "CREATE", "", "", canCreateTeam());
        renderButton(graphics, mouseX, mouseY, progress, x + 64, y, 38, Component.translatable("screen.chronicle_engine.team.teams"), "TAB_TEAMS", "", "", true);
        renderButton(graphics, mouseX, mouseY, progress, x + 108, y, 20, Component.literal("!"), "TAB_NOTIFICATIONS", "", "", true);
        if (!packet.notifications().isEmpty()) {
            graphics.fill(x + 122, y + 2, x + 127, y + 7, argb((int) (240 * progress), 0xE85454));
        }
        renderButton(graphics, mouseX, mouseY, progress, x + 134, y, 42, Component.translatable("screen.chronicle_engine.team.refresh"), "REFRESH", "", "", true);
    }

    private void renderTeamList(GuiGraphics graphics, int mouseX, int mouseY, float progress, int left, int top, int width, int bottom) {
        graphics.drawString(font, Component.translatable("screen.chronicle_engine.team.list", packet.ownedTeams(), packet.maxOwnedTeams()), left, top, argb((int) (210 * progress), 0xD8D8D8), false);
        int y = top + 14;
        int rowHeight = 30;
        int visible = Math.max(1, (bottom - y) / rowHeight);
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, packet.teams().size() - visible));
        for (int i = 0; i < visible; i++) {
            int index = listScroll + i;
            if (index >= packet.teams().size()) {
                break;
            }
            ChronicleEngineNetwork.TeamLine team = packet.teams().get(index);
            boolean selected = team.teamId().equals(selectedTeamId) && !notificationTab;
            boolean hovered = mouseX >= left && mouseX <= left + width && mouseY >= y && mouseY <= y + rowHeight - 6;
            graphics.fill(left, y, left + width, y + rowHeight - 4, argb((int) ((selected ? 162 : hovered ? 124 : 96) * progress), selected ? 0x27303C : 0x101318));
            graphics.fill(left, y, left + 3, y + rowHeight - 4, argb((int) ((selected ? 230 : 90) * progress), team.member() ? 0xFFD46A : 0x9ED8FF));
            graphics.drawString(font, trimToWidth(team.name(), Math.max(32, width - 18)), left + 10, y + 5, argb((int) (235 * progress), 0xEFEFEF), false);
            graphics.drawString(font, trimToWidth(Component.translatable("screen.chronicle_engine.team.owner", team.ownerName()).getString(), Math.max(32, width - 18)), left + 10, y + 17, argb((int) (170 * progress), 0xB7B7B7), false);
            buttons.add(new ButtonArea("SELECT", team.teamId(), "", left, y, width, rowHeight - 6, true));
            y += rowHeight;
        }
    }

    private void renderTeamDetail(GuiGraphics graphics, int mouseX, int mouseY, float progress, int left, int top, int right, int bottom) {
        ChronicleEngineNetwork.TeamLine team = selectedTeam();
        if (team == null) {
            graphics.drawString(font, Component.translatable("screen.chronicle_engine.team.empty"), left, top, argb((int) (210 * progress), 0xD8D8D8), false);
            return;
        }
        int y = top - detailScroll;
        graphics.enableScissor(left, top, right, bottom);
        int detailWidth = right - left;
        graphics.drawString(font, trimToWidth(team.name(), Math.max(50, detailWidth - 4)), left, y, argb((int) (245 * progress), 0xFFD46A), false);
        graphics.drawString(font, trimToWidth(Component.translatable("screen.chronicle_engine.team.owner", team.ownerName()).getString(), Math.max(50, detailWidth - 4)), left, y + 12, argb((int) (190 * progress), 0xB7B7B7), false);
        graphics.drawString(font, Component.translatable("screen.chronicle_engine.team.members", team.members().size()), left, y + 24, argb((int) (190 * progress), 0xB7B7B7), false);

        int actionY = y + 38;
        int halfButtonWidth = Math.max(46, (detailWidth - 6) / 2);
        if (!team.member()) {
            String labelKey = team.blacklisted() ? "screen.chronicle_engine.team.blacklisted" : team.pendingForMe() ? "screen.chronicle_engine.team.pending" : packet.serverAdmin() ? "screen.chronicle_engine.team.force_join" : "screen.chronicle_engine.team.request_join";
            renderButton(graphics, mouseX, mouseY, progress, left, actionY, Math.min(120, detailWidth), Component.translatable(labelKey), "REQUEST_JOIN", team.teamId(), "", !team.blacklisted() || packet.serverAdmin());
            actionY += BUTTON_HEIGHT + 4;
        } else if (!team.owner()) {
            renderButton(graphics, mouseX, mouseY, progress, left, actionY, Math.min(120, detailWidth), Component.translatable("screen.chronicle_engine.team.leave"), "LEAVE", team.teamId(), "", true);
            actionY += BUTTON_HEIGHT + 4;
        }
        if (team.canManage()) {
            renderButton(graphics, mouseX, mouseY, progress, left, actionY, halfButtonWidth, Component.translatable(team.friendlyFire() ? "screen.chronicle_engine.team.ff_on" : "screen.chronicle_engine.team.ff_off"), "TOGGLE_FRIENDLY_FIRE", team.teamId(), "", true);
            renderButton(graphics, mouseX, mouseY, progress, left + halfButtonWidth + 6, actionY, Math.max(42, detailWidth - halfButtonWidth - 6), Component.translatable("screen.chronicle_engine.team.disband"), "DISBAND", team.teamId(), "", team.owner() || packet.serverAdmin());
            actionY += BUTTON_HEIGHT + 4;
        }
        y = Math.max(y + 54, actionY + 2);
        y = renderRequestSection(graphics, mouseX, mouseY, progress, team, left, y, right);
        y = renderMemberSection(graphics, mouseX, mouseY, progress, team, left, y + 8, right);
        y = renderBlacklistSection(graphics, mouseX, mouseY, progress, team, left, y + 8, right);
        graphics.disableScissor();
    }

    private int renderRequestSection(GuiGraphics graphics, int mouseX, int mouseY, float progress, ChronicleEngineNetwork.TeamLine team, int left, int y, int right) {
        if (!team.canManage() || team.requests().isEmpty()) {
            return y;
        }
        graphics.drawString(font, Component.translatable("screen.chronicle_engine.team.requests"), left, y, argb((int) (230 * progress), 0xFFD46A), false);
        y += 13;
        for (ChronicleEngineNetwork.TeamRequestLine request : team.requests()) {
            int buttonWidth = 42;
            graphics.drawString(font, trimToWidth(request.playerName(), Math.max(28, right - left - buttonWidth * 2 - 18)), left + 6, y + 4, argb((int) (220 * progress), 0xEDEDED), false);
            renderButton(graphics, mouseX, mouseY, progress, right - buttonWidth * 2 - 6, y, buttonWidth, Component.translatable("screen.chronicle_engine.team.approve"), "APPROVE", team.teamId(), request.playerId(), true);
            renderButton(graphics, mouseX, mouseY, progress, right - buttonWidth, y, buttonWidth, Component.translatable("screen.chronicle_engine.team.deny"), "DENY", team.teamId(), request.playerId(), true);
            y += BUTTON_HEIGHT + 5;
        }
        return y;
    }

    private int renderMemberSection(GuiGraphics graphics, int mouseX, int mouseY, float progress, ChronicleEngineNetwork.TeamLine team, int left, int y, int right) {
        graphics.drawString(font, Component.translatable("screen.chronicle_engine.team.member_list"), left, y, argb((int) (230 * progress), 0xFFD46A), false);
        y += 13;
        for (ChronicleEngineNetwork.TeamMemberLine member : team.members()) {
            String role = member.playerId().equals(team.ownerId()) ? "owner" : contains(team.admins(), member.playerId()) ? "admin" : "member";
            int rowHeight = team.canManage() && !"owner".equals(role) ? 42 : 20;
            graphics.fill(left, y, right, y + rowHeight, argb((int) (70 * progress), 0x11151C));
            int roleWidth = 44;
            graphics.drawString(font, trimToWidth(member.name() + (member.online() ? "" : " [offline]"), Math.max(40, right - left - roleWidth - 16)), left + 6, y + 5, argb((int) (225 * progress), member.online() ? 0xEDEDED : 0x9A9A9A), false);
            graphics.drawString(font, Component.translatable("screen.chronicle_engine.team.role." + role), right - roleWidth, y + 5, argb((int) (180 * progress), 0xB7B7B7), false);
            if (team.canManage() && !"owner".equals(role)) {
                boolean isAdmin = "admin".equals(role);
                int buttonY = y + 21;
                int detailWidth = right - left;
                int adminWidth = Math.max(48, Math.min(58, detailWidth / 4));
                int transferWidth = 38;
                int kickWidth = 34;
                int blacklistWidth = Math.max(58, detailWidth - adminWidth - transferWidth - kickWidth - 24);
                renderButton(graphics, mouseX, mouseY, progress, left + 6, buttonY, adminWidth, Component.translatable(isAdmin ? "screen.chronicle_engine.team.remove_admin" : "screen.chronicle_engine.team.add_admin"), isAdmin ? "REMOVE_ADMIN" : "ADD_ADMIN", team.teamId(), member.playerId(), true);
                renderButton(graphics, mouseX, mouseY, progress, left + 12 + adminWidth, buttonY, transferWidth, Component.translatable("screen.chronicle_engine.team.transfer"), "TRANSFER", team.teamId(), member.playerId(), team.owner() || packet.serverAdmin());
                renderButton(graphics, mouseX, mouseY, progress, left + 18 + adminWidth + transferWidth, buttonY, kickWidth, Component.translatable("screen.chronicle_engine.team.kick"), "KICK", team.teamId(), member.playerId(), true);
                renderButton(graphics, mouseX, mouseY, progress, left + 24 + adminWidth + transferWidth + kickWidth, buttonY, blacklistWidth, Component.translatable("screen.chronicle_engine.team.kick_blacklist"), "KICK_BLACKLIST", team.teamId(), member.playerId(), true);
            }
            y += rowHeight + 4;
        }
        return y;
    }

    private int renderBlacklistSection(GuiGraphics graphics, int mouseX, int mouseY, float progress, ChronicleEngineNetwork.TeamLine team, int left, int y, int right) {
        if (!team.canManage()) {
            return y;
        }
        graphics.drawString(font, Component.translatable("screen.chronicle_engine.team.blacklist"), left, y, argb((int) (230 * progress), 0xFFD46A), false);
        y += 13;
        if (team.blacklist().isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.chronicle_engine.team.blacklist_empty"), left + 8, y + 4, argb((int) (170 * progress), 0xB7B7B7), false);
            return y + 18;
        }
        for (ChronicleEngineNetwork.TeamMemberLine member : team.blacklist()) {
            int buttonWidth = 62;
            graphics.drawString(font, trimToWidth(member.name(), Math.max(30, right - left - buttonWidth - 16)), left + 6, y + 4, argb((int) (220 * progress), 0xEDEDED), false);
            renderButton(graphics, mouseX, mouseY, progress, right - buttonWidth, y, buttonWidth, Component.translatable("screen.chronicle_engine.team.unblacklist"), "UNBLACKLIST", team.teamId(), member.playerId(), true);
            y += BUTTON_HEIGHT + 5;
        }
        return y;
    }

    private void renderNotifications(GuiGraphics graphics, int mouseX, int mouseY, float progress, int left, int top, int right, int bottom) {
        int y = top - detailScroll;
        graphics.enableScissor(left, top, right, bottom);
        graphics.drawString(font, Component.translatable("screen.chronicle_engine.team.notifications"), left, y, argb((int) (245 * progress), 0xFFD46A), false);
        y += 18;
        if (packet.notifications().isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.chronicle_engine.team.no_notifications"), left, y, argb((int) (200 * progress), 0xB7B7B7), false);
        }
        for (ChronicleEngineNetwork.TeamRequestLine request : packet.notifications()) {
            int rowHeight = 32;
            int buttonWidth = 42;
            graphics.fill(left, y, right, y + rowHeight, argb((int) (82 * progress), 0x11151C));
            graphics.drawString(font, trimToWidth(request.playerName(), Math.max(28, right - left - buttonWidth * 2 - 22)), left + 7, y + 5, argb((int) (230 * progress), 0xEDEDED), false);
            graphics.drawString(font, trimToWidth(request.teamName(), Math.max(28, right - left - buttonWidth * 2 - 22)), left + 7, y + 17, argb((int) (160 * progress), 0xB7B7B7), false);
            renderButton(graphics, mouseX, mouseY, progress, right - buttonWidth * 2 - 6, y + 8, buttonWidth, Component.translatable("screen.chronicle_engine.team.approve"), "APPROVE", request.teamId(), request.playerId(), true);
            renderButton(graphics, mouseX, mouseY, progress, right - buttonWidth, y + 8, buttonWidth, Component.translatable("screen.chronicle_engine.team.deny"), "DENY", request.teamId(), request.playerId(), true);
            y += rowHeight + 5;
        }
        graphics.disableScissor();
    }

    private void renderButton(GuiGraphics graphics, int mouseX, int mouseY, float progress, int x, int y, int width, Component label, String action, String teamId, String targetId, boolean enabled) {
        boolean hovered = enabled && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + BUTTON_HEIGHT;
        int bg = enabled ? hovered ? 0x313946 : 0x171C24 : 0x111111;
        int accent = enabled ? hovered ? 0xFFD46A : 0x9ED8FF : 0x666666;
        graphics.fill(x, y, x + width, y + BUTTON_HEIGHT, argb((int) ((enabled ? hovered ? 170 : 125 : 70) * progress), bg));
        graphics.fill(x, y, x + 2, y + BUTTON_HEIGHT, argb((int) (210 * progress), accent));
        graphics.drawString(font, trimToWidth(label.getString(), Math.max(12, width - 9)), x + 6, y + 4, argb((int) (235 * progress), enabled ? 0xEDEDED : 0x888888), false);
        buttons.add(new ButtonArea(action, teamId, targetId, x, y, width, BUTTON_HEIGHT, enabled));
    }

    private ChronicleEngineNetwork.TeamLine selectedTeam() {
        for (ChronicleEngineNetwork.TeamLine team : packet.teams()) {
            if (team.teamId().equals(selectedTeamId)) {
                return team;
            }
        }
        return null;
    }

    private void selectDefaultTeam() {
        selectedTeamId = packet.teams().isEmpty() ? "" : packet.teams().get(0).teamId();
    }

    private boolean canCreateTeam() {
        return packet.maxOwnedTeams() > 0 && packet.ownedTeams() < packet.maxOwnedTeams();
    }

    private static boolean contains(List<ChronicleEngineNetwork.TeamMemberLine> members, String playerId) {
        for (ChronicleEngineNetwork.TeamMemberLine member : members) {
            if (member.playerId().equals(playerId)) {
                return true;
            }
        }
        return false;
    }

    private static String trim(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 1)) + "...";
    }

    private String trimToWidth(String value, int width) {
        if (font.width(value) <= width) {
            return value;
        }
        return font.plainSubstrByWidth(value, Math.max(0, width - font.width("..."))) + "...";
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

    private record ButtonArea(String action, String teamId, String targetId, int x, int y, int width, int height, boolean enabled) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }
    }
}
