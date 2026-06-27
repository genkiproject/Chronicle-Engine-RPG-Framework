package com.chronicle.engine;

import com.chronicle.engine.data.DialogueSpec;
import com.chronicle.engine.data.JsonUtil;
import com.chronicle.engine.data.ChronicleEngineData;
import com.chronicle.engine.network.ChronicleEngineNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class ChronicleEngineDialogueService {
    private ChronicleEngineDialogueService() {
    }

    public static void open(ServerPlayer player, String dialogueId) {
        DialogueSpec dialogue = ChronicleEngineData.dialogue(dialogueId);
        if (dialogue != null) {
            openNode(player, dialogue, dialogue.startNodeId(), dialogue.defaultNpc().component());
        }
    }

    public static void open(ServerPlayer player, String dialogueId, Component npcName) {
        DialogueSpec dialogue = ChronicleEngineData.dialogue(dialogueId);
        if (dialogue != null) {
            openNode(player, dialogue, dialogue.startNodeId(), npcName == null ? dialogue.defaultNpc().component() : npcName);
        }
    }

    public static void choose(ServerPlayer player, String dialogueId, String nodeId, String choiceId) {
        if ("__close".equals(choiceId)) {
            ChronicleEngineNpcFocusService.release(player);
            ChronicleEngineNetwork.closeScreen(player);
            return;
        }
        DialogueSpec dialogue = ChronicleEngineData.dialogue(dialogueId);
        if (dialogue == null) {
            return;
        }
        DialogueSpec.Node node = dialogue.nodes().get(nodeId);
        if (node == null) {
            return;
        }
        DialogueSpec.Choice choice = node.choices().stream()
                .filter(candidate -> candidate.choiceId().equals(choiceId))
                .findFirst()
                .orElse(null);
        if (choice == null || !ChronicleEngineConditions.all(player, choice.conditions())) {
            return;
        }

        boolean close = false;
        boolean openedExternalScreen = false;
        for (DialogueSpec.Action action : choice.actions()) {
            String type = action.type();
            if ("close".equals(type)) {
                close = true;
                continue;
            }
            if ("start_quest".equals(type)) {
                ChronicleEngineQuestService.accept(player, JsonUtil.string(action.raw(), "questId", ""));
            } else if ("notify_interact".equals(type)) {
                ChronicleEngineQuestService.notifyObjective(player, "INTERACT", JsonUtil.string(action.raw(), "targetId", ""), 1);
            } else if ("set_flag".equals(type)) {
                String flag = JsonUtil.string(action.raw(), "flagName", JsonUtil.string(action.raw(), "flag", ""));
                ChronicleEnginePlayerData.setFlag(player, flag);
            } else if ("open_trade".equals(type) || "open_shop".equals(type)) {
                ChronicleEngineNpcFocusService.release(player);
                ChronicleEngineShopService.open(player, JsonUtil.string(action.raw(), "shopId", ""));
                openedExternalScreen = true;
            } else {
                ChronicleEngineItems.executeReward(player, type, action.raw());
            }
        }

        if (!choice.nextNodeId().isBlank() && !close && !openedExternalScreen) {
            openNode(player, dialogue, choice.nextNodeId(), dialogue.defaultNpc().component());
        } else if (close && !openedExternalScreen) {
            ChronicleEngineNpcFocusService.release(player);
            ChronicleEngineNetwork.closeScreen(player);
        }
    }

    private static void openNode(ServerPlayer player, DialogueSpec dialogue, String nodeId, Component npcName) {
        DialogueSpec.Node node = dialogue.nodes().get(nodeId);
        if (node == null) {
            return;
        }

        Component text = node.text().component();
        for (DialogueSpec.ConditionalText conditionalText : DialogueSpec.sortedConditionalTexts(node)) {
            if (ChronicleEngineConditions.all(player, conditionalText.conditions())) {
                text = conditionalText.text().component();
                break;
            }
        }

        List<ChronicleEngineNetwork.ChoiceLine> choices = new ArrayList<>();
        for (DialogueSpec.Choice choice : node.choices()) {
            if (ChronicleEngineConditions.all(player, choice.conditions())) {
                choices.add(new ChronicleEngineNetwork.ChoiceLine(choice.choiceId(), choice.text().component()));
            }
        }
        if (choices.isEmpty()) {
            choices.add(new ChronicleEngineNetwork.ChoiceLine("__close", Component.translatable("screen.chronicle_engine.dialogue.close")));
        }
        ChronicleEngineNetwork.openDialogue(player, new ChronicleEngineNetwork.OpenDialoguePacket(dialogue.id(), node.nodeId(), npcName, text, choices));
    }
}

