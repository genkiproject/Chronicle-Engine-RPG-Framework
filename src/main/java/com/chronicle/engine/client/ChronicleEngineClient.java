package com.chronicle.engine.client;

import com.chronicle.engine.ChronicleEngine;
import com.chronicle.engine.network.ChronicleEngineNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class ChronicleEngineClient {
    private static KeyMapping openJournal;
    private static List<ChronicleEngineNetwork.MarkerLine> markers = new ArrayList<>();
    private static List<ChronicleEngineNetwork.TrackerQuest> trackers = List.of();

    private ChronicleEngineClient() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(ChronicleEngineClient::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ChronicleEngineClient::onRenderOverlay);
    }

    public static void openDialogue(ChronicleEngineNetwork.OpenDialoguePacket packet) {
        Minecraft.getInstance().setScreen(new ChronicleEngineDialogueScreen(packet));
    }

    public static void openShop(ChronicleEngineNetwork.OpenShopPacket packet) {
        if (Minecraft.getInstance().screen instanceof ChronicleEngineShopScreen shopScreen) {
            shopScreen.updatePacket(packet);
        } else {
            Minecraft.getInstance().setScreen(new ChronicleEngineShopScreen(packet));
        }
    }

    public static void openJournal(ChronicleEngineNetwork.OpenJournalPacket packet) {
        if (Minecraft.getInstance().screen instanceof ChronicleEngineJournalScreen journalScreen) {
            journalScreen.updatePacket(packet);
        } else {
            Minecraft.getInstance().setScreen(new ChronicleEngineJournalScreen(packet));
        }
    }

    public static void openWallet(ChronicleEngineNetwork.OpenWalletPacket packet) {
        if (Minecraft.getInstance().screen instanceof ChronicleEngineWalletScreen walletScreen) {
            walletScreen.updatePacket(packet);
        } else {
            Minecraft.getInstance().setScreen(new ChronicleEngineWalletScreen(packet));
        }
    }

    public static void openTeams(ChronicleEngineNetwork.OpenTeamsPacket packet) {
        if (Minecraft.getInstance().screen instanceof ChronicleEngineTeamScreen teamScreen) {
            teamScreen.updatePacket(packet);
        } else {
            Minecraft.getInstance().setScreen(new ChronicleEngineTeamScreen(packet));
        }
    }

    public static void closeScreen() {
        Minecraft.getInstance().setScreen(null);
    }

    public static void setMarkers(List<ChronicleEngineNetwork.MarkerLine> newMarkers) {
        markers = List.copyOf(newMarkers);
    }

    public static void setTrackers(List<ChronicleEngineNetwork.TrackerQuest> newTrackers) {
        trackers = List.copyOf(newTrackers);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || openJournal == null) {
            return;
        }
        while (openJournal.consumeClick()) {
            ChronicleEngineNetwork.requestJournal();
        }
    }

    private static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || markers.isEmpty()) {
            if (minecraft.player != null && minecraft.level != null) {
                renderQuestTracker(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth(), 18);
            }
            return;
        }
        String dimension = minecraft.level.dimension().location().toString();
        GuiGraphics graphics = event.getGuiGraphics();
        int y = renderQuestTracker(graphics, event.getWindow().getGuiScaledWidth(), 18) + 8;
        int x = event.getWindow().getGuiScaledWidth() - 220;
        for (ChronicleEngineNetwork.MarkerLine marker : markers) {
            if (!marker.dimension().equals(dimension)) {
                continue;
            }
            double distance = Math.sqrt(minecraft.player.distanceToSqr(marker.x() + 0.5D, marker.y() + 0.5D, marker.z() + 0.5D));
            String text = "◇ " + marker.label() + "  " + (int) distance + "m";
            graphics.fill(x - 6, y - 4, x + 210, y + 12, 0x88000000);
            graphics.drawString(minecraft.font, text, x, y, 0xFFFFD46A, false);
            y += 16;
            if (y > 100) {
                break;
            }
        }
    }

    private static int renderQuestTracker(GuiGraphics graphics, int screenWidth, int top) {
        int y = top;
        for (ChronicleEngineNetwork.TrackerQuest tracker : trackers.stream().filter(ChronicleEngineNetwork.TrackerQuest::visible).limit(2).toList()) {
            y = renderSingleTracker(graphics, screenWidth, y, tracker) + 4;
        }
        return y;
    }

    private static int renderSingleTracker(GuiGraphics graphics, int screenWidth, int top, ChronicleEngineNetwork.TrackerQuest tracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        float scale = 0.78F;
        int logicalWidth = 184;
        int renderedWidth = Mth.ceil(logicalWidth * scale);
        int x = screenWidth - renderedWidth - 8;
        int lineCount = Math.min(2, tracker.objectives().size());
        int logicalHeight = 31 + lineCount * 15;
        graphics.pose().pushPose();
        graphics.pose().translate(x, top, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.fill(0, 0, logicalWidth, logicalHeight, 0xAD000000);
        graphics.fill(0, 0, logicalWidth, 1, 0x8CFFD46A);
        graphics.fill(6, 20, logicalWidth - 6, 21, 0x2AFFFFFF);
        graphics.drawString(font, trimToWidth(font, tracker.questTitle(), logicalWidth - 12), 6, 4, 0xFFFFD46A, false);
        graphics.drawString(font, trimToWidth(font, tracker.phaseTitle(), logicalWidth - 12), 6, 13, 0xFFE5E5E5, false);

        int y = 25;
        for (int i = 0; i < lineCount; i++) {
            ChronicleEngineNetwork.TrackerLine line = tracker.objectives().get(i);
            String label = trimToWidth(font, line.text(), logicalWidth - 47);
            graphics.drawString(font, label, 6, y, 0xFFD8D8D8, false);
            graphics.drawString(font, line.progress() + "/" + line.required(), logicalWidth - 37, y, 0xFFB7B7B7, false);
            int barWidth = logicalWidth - 12;
            int fill = line.required() <= 0 ? 0 : Math.min(barWidth, (int) ((line.progress() / (float) line.required()) * barWidth));
            graphics.fill(6, y + 9, 6 + barWidth, y + 11, 0x3AFFFFFF);
            graphics.fill(6, y + 9, 6 + fill, y + 11, line.progress() >= line.required() ? 0xBB8EE28E : 0xBBFFD46A);
            y += 15;
        }
        if (tracker.objectives().size() > lineCount) {
            graphics.drawString(font, "...", logicalWidth - 16, logicalHeight - 9, 0xFFB7B7B7, false);
        }
        graphics.pose().popPose();
        return top + Mth.ceil(logicalHeight * scale);
    }

    private static String trimToWidth(Font font, String value, int width) {
        if (font.width(value) <= width) {
            return value;
        }
        String result = value;
        while (!result.isEmpty() && font.width(result + "...") > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    @Mod.EventBusSubscriber(modid = ChronicleEngine.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            openJournal = new KeyMapping(
                    "key.chronicle_engine.open_journal",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_L,
                    "key.categories.chronicle_engine"
            );
            event.register(openJournal);
        }
    }
}

