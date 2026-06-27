package com.chronicle.engine;

import com.chronicle.engine.data.JsonUtil;
import com.chronicle.engine.data.QuestSpec;
import com.chronicle.engine.data.ChronicleEngineData;
import com.chronicle.engine.network.ChronicleEngineNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class ChronicleEngineQuestService {
    private ChronicleEngineQuestService() {
    }

    public static boolean canAccept(ServerPlayer player, QuestSpec quest) {
        if (quest == null) {
            return false;
        }
        if (ChronicleEnginePlayerData.isQuestActive(player, quest.id())) {
            return false;
        }
        if (!quest.repeatable() && ChronicleEnginePlayerData.isQuestCompleted(player, quest.id())) {
            return false;
        }
        return ChronicleEngineConditions.all(player, quest.unlockConditions());
    }

    public static boolean accept(ServerPlayer player, String questId) {
        return accept(player, questId, true);
    }

    public static boolean acceptFromTeam(ServerPlayer player, String questId) {
        return accept(player, questId, false);
    }

    private static boolean accept(ServerPlayer player, String questId, boolean propagateTeam) {
        QuestSpec quest = ChronicleEngineData.quest(questId);
        if (!canAccept(player, quest)) {
            return false;
        }
        CompoundTag state = ChronicleEnginePlayerData.quest(player, questId);
        state.putString("status", "active");
        state.putString("phase", quest.initialPhaseId());
        state.put("progress", new CompoundTag());
        state.putBoolean("phaseRewarded", false);
        List<String> tracked = new ArrayList<>(ChronicleEnginePlayerData.trackedQuestIds(player));
        if (!tracked.contains(questId) && tracked.size() < 2) {
            tracked.add(questId);
            ChronicleEnginePlayerData.setTrackedQuestIds(player, tracked);
        }
        for (String flag : quest.flagsToSetOnAccept()) {
            ChronicleEnginePlayerData.setFlag(player, flag);
        }
        enterPhase(player, quest, quest.initialPhaseId(), false);
        player.displayClientMessage(Component.translatable("message.chronicle_engine.quest.accepted", quest.displayName().component()).withStyle(ChatFormatting.GREEN), false);
        syncMarkers(player);
        syncTracker(player);
        if (propagateTeam && quest.teamSync()) {
            ChronicleEngineTeamService.propagateQuestAccept(player, questId);
        }
        return true;
    }

    public static void complete(ServerPlayer player, String questId) {
        QuestSpec quest = ChronicleEngineData.quest(questId);
        if (quest != null) {
            completeQuest(player, quest, true);
        }
    }

    public static void reset(ServerPlayer player, String questId) {
        ChronicleEnginePlayerData.quests(player).remove(questId);
        ChronicleEnginePlayerData.untrackQuest(player, questId);
        syncMarkers(player);
        syncTracker(player);
    }

    public static void resetAll(ServerPlayer player) {
        ChronicleEnginePlayerData.clearAll(player);
        syncMarkers(player);
        syncTracker(player);
        player.displayClientMessage(Component.translatable("message.chronicle_engine.quest.reset_all").withStyle(ChatFormatting.YELLOW), false);
    }

    public static void notifyObjective(ServerPlayer player, String type, String targetId, int amount) {
        notifyObjective(player, type, targetId, amount, true, false);
    }

    public static void notifyObjectiveFromTeam(ServerPlayer player, String type, String targetId, int amount) {
        notifyObjective(player, type, targetId, amount, false, true);
    }

    private static void notifyObjective(ServerPlayer player, String type, String targetId, int amount, boolean propagateTeam, boolean teamOnly) {
        boolean changed = false;
        for (QuestSpec quest : ChronicleEngineData.quests()) {
            if (teamOnly && !quest.teamSync()) {
                continue;
            }
            if (!ChronicleEnginePlayerData.isQuestActive(player, quest.id())) {
                continue;
            }
            QuestSpec.Phase phase = quest.phases().get(ChronicleEnginePlayerData.phase(player, quest.id()));
            if (phase == null) {
                continue;
            }
            boolean questChanged = false;
            CompoundTag progress = ChronicleEnginePlayerData.progress(player, quest.id());
            for (int i = 0; i < phase.objectives().size(); i++) {
                QuestSpec.Objective objective = phase.objectives().get(i);
                if (objective.type().equalsIgnoreCase(type) && objective.targetId().equals(targetId)) {
                    String key = progressKey(i, objective);
                    int next = Math.min(objective.requiredCount(), progress.getInt(key) + Math.max(1, amount));
                    if (next != progress.getInt(key)) {
                        progress.putInt(key, next);
                        questChanged = true;
                    }
                }
            }
            if (questChanged) {
                changed = true;
                checkPhase(player, quest);
            }
        }
        if (changed) {
            syncMarkers(player);
            syncTracker(player);
            if (propagateTeam) {
                ChronicleEngineTeamService.propagateObjective(player, type, targetId, amount);
            }
        }
    }

    public static void notifyAdvancement(ServerPlayer player, String advancementId) {
        notifyAdvancement(player, advancementId, true, false);
    }

    public static void notifyAdvancementFromTeam(ServerPlayer player, String advancementId) {
        notifyAdvancement(player, advancementId, false, true);
    }

    private static void notifyAdvancement(ServerPlayer player, String advancementId, boolean propagateTeam, boolean teamOnly) {
        boolean changed = false;
        for (QuestSpec quest : ChronicleEngineData.quests()) {
            if (teamOnly && !quest.teamSync()) {
                continue;
            }
            if (!ChronicleEnginePlayerData.isQuestActive(player, quest.id())) {
                continue;
            }
            QuestSpec.Phase phase = quest.phases().get(ChronicleEnginePlayerData.phase(player, quest.id()));
            if (phase == null) {
                continue;
            }
            boolean questChanged = false;
            CompoundTag progress = ChronicleEnginePlayerData.progress(player, quest.id());
            for (int i = 0; i < phase.objectives().size(); i++) {
                QuestSpec.Objective objective = phase.objectives().get(i);
                if (!objectiveAdvancementId(objective).equals(advancementId)) {
                    continue;
                }
                String key = progressKey(i, objective);
                if (progress.getInt(key) < objective.requiredCount()) {
                    progress.putInt(key, objective.requiredCount());
                    questChanged = true;
                }
            }
            if (questChanged) {
                changed = true;
                checkPhase(player, quest);
            }
        }
        if (changed) {
            syncMarkers(player);
            syncTracker(player);
            if (propagateTeam) {
                ChronicleEngineTeamService.propagateAdvancement(player, advancementId);
            }
        }
    }

    public static boolean offer(ServerPlayer player, String questId) {
        QuestSpec quest = ChronicleEngineData.quest(questId);
        if (quest == null || !ChronicleEnginePlayerData.isQuestActive(player, quest.id())) {
            return false;
        }
        QuestSpec.Phase phase = quest.phases().get(ChronicleEnginePlayerData.phase(player, quest.id()));
        if (phase == null) {
            return false;
        }
        boolean changed = false;
        CompoundTag progress = ChronicleEnginePlayerData.progress(player, quest.id());
        for (int i = 0; i < phase.objectives().size(); i++) {
            QuestSpec.Objective objective = phase.objectives().get(i);
            if (!"OFFER".equalsIgnoreCase(objective.type())) {
                continue;
            }
            String key = progressKey(i, objective);
            int remaining = objective.requiredCount() - progress.getInt(key);
            if (remaining <= 0) {
                continue;
            }
            if (ChronicleEngineItems.removeItem(player, objective.targetId(), remaining)) {
                progress.putInt(key, objective.requiredCount());
                changed = true;
                player.displayClientMessage(Component.translatable("message.chronicle_engine.offer.completed", objective.displayText().component()).withStyle(ChatFormatting.YELLOW), false);
            } else {
                player.displayClientMessage(Component.translatable("message.chronicle_engine.offer.missing", objective.displayText().component()).withStyle(ChatFormatting.RED), false);
            }
        }
        if (changed) {
            checkPhase(player, quest);
            syncMarkers(player);
            syncTracker(player);
        }
        return changed;
    }

    public static void updateCollectedItems(ServerPlayer player) {
        boolean changed = false;
        for (QuestSpec quest : ChronicleEngineData.quests()) {
            if (!ChronicleEnginePlayerData.isQuestActive(player, quest.id())) {
                continue;
            }
            QuestSpec.Phase phase = quest.phases().get(ChronicleEnginePlayerData.phase(player, quest.id()));
            if (phase == null) {
                continue;
            }
            CompoundTag progress = ChronicleEnginePlayerData.progress(player, quest.id());
            for (int i = 0; i < phase.objectives().size(); i++) {
                QuestSpec.Objective objective = phase.objectives().get(i);
                if (!"COLLECT".equalsIgnoreCase(objective.type())) {
                    continue;
                }
                String key = progressKey(i, objective);
                int previous = progress.getInt(key);
                int next = Math.min(objective.requiredCount(), ChronicleEngineItems.countItem(player, objective.targetId()));
                if (next != previous) {
                    progress.putInt(key, next);
                    changed = true;
                    if (next > previous && quest.teamSync()) {
                        ChronicleEngineTeamService.propagateObjective(player, "COLLECT", objective.targetId(), next - previous);
                    }
                }
            }
            if (changed) {
                checkPhase(player, quest);
            }
        }
        if (changed) {
            syncMarkers(player);
            syncTracker(player);
        }
    }

    public static void updateAdvancementObjectives(ServerPlayer player) {
        boolean changed = false;
        for (QuestSpec quest : ChronicleEngineData.quests()) {
            if (!ChronicleEnginePlayerData.isQuestActive(player, quest.id())) {
                continue;
            }
            QuestSpec.Phase phase = quest.phases().get(ChronicleEnginePlayerData.phase(player, quest.id()));
            if (phase == null) {
                continue;
            }
            boolean questChanged = false;
            CompoundTag progress = ChronicleEnginePlayerData.progress(player, quest.id());
            for (int i = 0; i < phase.objectives().size(); i++) {
                QuestSpec.Objective objective = phase.objectives().get(i);
                String advancementId = objectiveAdvancementId(objective);
                if (advancementId.isBlank() || !hasAdvancement(player, advancementId)) {
                    continue;
                }
                String key = progressKey(i, objective);
                if (progress.getInt(key) < objective.requiredCount()) {
                    progress.putInt(key, objective.requiredCount());
                    questChanged = true;
                }
            }
            if (questChanged) {
                changed = true;
                checkPhase(player, quest);
            }
        }
        if (changed) {
            syncMarkers(player);
            syncTracker(player);
        }
    }

    public static List<JournalQuest> journal(ServerPlayer player) {
        List<JournalQuest> result = new ArrayList<>();
        for (QuestSpec quest : ChronicleEngineData.quests()) {
            boolean active = ChronicleEnginePlayerData.isQuestActive(player, quest.id());
            boolean completed = ChronicleEnginePlayerData.isQuestCompleted(player, quest.id());
            if (!active && !completed && !canAccept(player, quest)) {
                continue;
            }
            String phaseId = ChronicleEnginePlayerData.phase(player, quest.id());
            QuestSpec.Phase phase = quest.phases().get(phaseId);
            List<JournalObjective> objectives = new ArrayList<>();
            List<JournalNode> nodes = buildJournalNodes(player, quest, active, completed, phaseId);
            if (active && phase != null) {
                CompoundTag progress = ChronicleEnginePlayerData.progress(player, quest.id());
                for (int i = 0; i < phase.objectives().size(); i++) {
                    QuestSpec.Objective objective = phase.objectives().get(i);
                    if (!objective.hidden()) {
                        objectives.add(new JournalObjective(objective.displayText().plain(), progress.getInt(progressKey(i, objective)), objective.requiredCount(), objective.type()));
                    }
                }
            }
            result.add(new JournalQuest(
                    quest.id(),
                    quest.displayName().plain(),
                    quest.description().plain(),
                    active,
                    completed,
                    active && ChronicleEnginePlayerData.isQuestTracked(player, quest.id()),
                    phase == null ? "" : phase.phaseId(),
                    phase == null ? "" : phase.displayName().plain(),
                    phase == null ? "" : phase.description().plain(),
                    objectives,
                    nodes
            ));
        }
        return result;
    }

    private static List<JournalNode> buildJournalNodes(ServerPlayer player, QuestSpec quest, boolean active, boolean completed, String currentPhaseId) {
        List<JournalNode> nodes = new ArrayList<>();
        List<QuestSpec.Phase> phases = new ArrayList<>(quest.phases().values());
        int currentIndex = -1;
        for (int i = 0; i < phases.size(); i++) {
            if (phases.get(i).phaseId().equals(currentPhaseId)) {
                currentIndex = i;
                break;
            }
        }
        for (int i = 0; i < phases.size(); i++) {
            QuestSpec.Phase phase = phases.get(i);
            String status;
            if (completed) {
                status = "completed";
            } else if (active && i == currentIndex) {
                status = "current";
            } else if (active && currentIndex >= 0 && i < currentIndex) {
                status = "completed";
            } else if (active && i == currentIndex + 1) {
                status = "next";
            } else if (!active && i == 0) {
                status = "next";
            } else {
                status = "locked";
            }
            List<JournalObjective> nodeObjectives = new ArrayList<>();
            CompoundTag progress = active && phase.phaseId().equals(currentPhaseId) ? ChronicleEnginePlayerData.progress(player, quest.id()) : new CompoundTag();
            for (int objectiveIndex = 0; objectiveIndex < phase.objectives().size(); objectiveIndex++) {
                QuestSpec.Objective objective = phase.objectives().get(objectiveIndex);
                if (!objective.hidden()) {
                    int value = "completed".equals(status) ? objective.requiredCount() : progress.getInt(progressKey(objectiveIndex, objective));
                    nodeObjectives.add(new JournalObjective(objective.displayText().plain(), value, objective.requiredCount(), objective.type()));
                }
            }
            nodes.add(new JournalNode(phase.phaseId(), phase.displayName().plain(), phase.description().plain(), status, nodeObjectives));
        }
        return nodes;
    }

    private static void enterPhase(ServerPlayer player, QuestSpec quest, String phaseId, boolean notify) {
        QuestSpec.Phase phase = quest.phases().get(phaseId);
        if (phase == null) {
            completeQuest(player, quest, true);
            return;
        }
        CompoundTag state = ChronicleEnginePlayerData.quest(player, quest.id());
        state.putString("phase", phaseId);
        state.put("progress", new CompoundTag());
        state.putBoolean("phaseRewarded", false);
        for (String flag : phase.flagsToSetOnEnter()) {
            ChronicleEnginePlayerData.setFlag(player, flag);
        }
        if (notify) {
            player.displayClientMessage(Component.translatable("message.chronicle_engine.quest.updated", phase.displayName().component()).withStyle(ChatFormatting.AQUA), false);
        }
    }

    private static void checkPhase(ServerPlayer player, QuestSpec quest) {
        QuestSpec.Phase phase = quest.phases().get(ChronicleEnginePlayerData.phase(player, quest.id()));
        if (phase == null || !isPhaseComplete(player, quest, phase)) {
            return;
        }
        CompoundTag state = ChronicleEnginePlayerData.quest(player, quest.id());
        if (state.getBoolean("phaseRewarded")) {
            return;
        }
        state.putBoolean("phaseRewarded", true);
        for (String flag : phase.flagsToSetOnComplete()) {
            ChronicleEnginePlayerData.setFlag(player, flag);
        }
        for (QuestSpec.Reward reward : phase.phaseRewards()) {
            ChronicleEngineItems.executeReward(player, reward);
        }
        for (QuestSpec.Transition transition : phase.transitions()) {
            if (ChronicleEngineConditions.test(player, transition.condition())) {
                enterPhase(player, quest, transition.targetPhaseId(), true);
                return;
            }
        }
        completeQuest(player, quest, true);
    }

    private static boolean isPhaseComplete(ServerPlayer player, QuestSpec quest, QuestSpec.Phase phase) {
        CompoundTag progress = ChronicleEnginePlayerData.progress(player, quest.id());
        for (int i = 0; i < phase.objectives().size(); i++) {
            QuestSpec.Objective objective = phase.objectives().get(i);
            if (objective.optional()) {
                continue;
            }
            if (progress.getInt(progressKey(i, objective)) < objective.requiredCount()) {
                return false;
            }
        }
        return true;
    }

    private static void completeQuest(ServerPlayer player, QuestSpec quest, boolean propagateTeam) {
        boolean alreadyCompleted = ChronicleEnginePlayerData.isQuestCompleted(player, quest.id());
        CompoundTag state = ChronicleEnginePlayerData.quest(player, quest.id());
        state.putString("status", "completed");
        state.putString("phase", "");
        state.put("progress", new CompoundTag());
        if (!alreadyCompleted) {
            state.putInt("completedCount", state.getInt("completedCount") + 1);
        }
        ChronicleEnginePlayerData.untrackQuest(player, quest.id());
        if (!alreadyCompleted) {
            for (String flag : quest.flagsToSetOnComplete()) {
                ChronicleEnginePlayerData.setFlag(player, flag);
            }
            for (QuestSpec.Reward reward : quest.completionRewards()) {
                ChronicleEngineItems.executeReward(player, reward);
            }
        }
        player.displayClientMessage(Component.translatable("message.chronicle_engine.quest.completed", quest.displayName().component()).withStyle(ChatFormatting.GOLD), false);
        syncMarkers(player);
        syncTracker(player);
        if (propagateTeam && quest.teamSync()) {
            for (ServerPlayer member : ChronicleEngineTeamService.onlineTeamMembersForQuestSync(player)) {
                if (!ChronicleEnginePlayerData.isQuestCompleted(member, quest.id())) {
                    completeQuest(member, quest, false);
                }
            }
        }
    }

    private static String progressKey(int index, QuestSpec.Objective objective) {
        return index + "|" + objective.type() + "|" + objective.targetId();
    }

    private static String objectiveAdvancementId(QuestSpec.Objective objective) {
        if ("ADVANCEMENT".equalsIgnoreCase(objective.type()) || "ACHIEVEMENT".equalsIgnoreCase(objective.type())) {
            return objective.targetId();
        }
        return JsonUtil.string(objective.extraData(), "advancementId", "");
    }

    private static boolean hasAdvancement(ServerPlayer player, String advancementId) {
        ResourceLocation id = ResourceLocation.tryParse(advancementId);
        MinecraftServer server = player.getServer();
        if (id == null || server == null) {
            return false;
        }
        Advancement advancement = server.getAdvancements().getAdvancement(id);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    public static void syncMarkers(ServerPlayer player) {
        List<ChronicleEngineNetwork.MarkerLine> markers = new ArrayList<>();
        for (QuestSpec quest : ChronicleEngineData.quests()) {
            if (!ChronicleEnginePlayerData.isQuestActive(player, quest.id())) {
                continue;
            }
            QuestSpec.Phase phase = quest.phases().get(ChronicleEnginePlayerData.phase(player, quest.id()));
            if (phase == null) {
                continue;
            }
            for (QuestSpec.Objective objective : phase.objectives()) {
                if (!objective.extraData().has("x") || !objective.extraData().has("z")) {
                    continue;
                }
                String dimension = JsonUtil.string(objective.extraData(), "dimension", player.level().dimension().location().toString());
                markers.add(new ChronicleEngineNetwork.MarkerLine(
                        objective.displayText().plain(),
                        dimension,
                        JsonUtil.integer(objective.extraData(), "x", 0),
                        JsonUtil.integer(objective.extraData(), "y", 64),
                        JsonUtil.integer(objective.extraData(), "z", 0)
                ));
            }
        }
        ChronicleEngineNetwork.sendMarkers(player, markers);
    }

    public static void syncTracker(ServerPlayer player) {
        initializeTrackingSelection(player);
        List<String> trackedIds = new ArrayList<>(ChronicleEnginePlayerData.trackedQuestIds(player));
        trackedIds.removeIf(questId -> !ChronicleEnginePlayerData.isQuestActive(player, questId));
        ChronicleEnginePlayerData.setTrackedQuestIds(player, trackedIds);

        List<ChronicleEngineNetwork.TrackerQuest> trackers = new ArrayList<>();
        for (String questId : trackedIds) {
            QuestSpec quest = ChronicleEngineData.quest(questId);
            if (quest == null) {
                continue;
            }
            QuestSpec.Phase phase = quest.phases().get(ChronicleEnginePlayerData.phase(player, quest.id()));
            if (phase == null) {
                continue;
            }
            CompoundTag progress = ChronicleEnginePlayerData.progress(player, quest.id());
            List<ChronicleEngineNetwork.TrackerLine> objectives = new ArrayList<>();
            for (int i = 0; i < phase.objectives().size(); i++) {
                QuestSpec.Objective objective = phase.objectives().get(i);
                if (!objective.hidden()) {
                    objectives.add(new ChronicleEngineNetwork.TrackerLine(
                            objective.displayText().plain(),
                            progress.getInt(progressKey(i, objective)),
                            objective.requiredCount(),
                            objective.type()
                    ));
                }
            }
            trackers.add(new ChronicleEngineNetwork.TrackerQuest(true, quest.displayName().plain(), phase.displayName().plain(), objectives));
        }
        ChronicleEngineNetwork.sendTrackers(player, trackers);
    }

    public static void toggleTracking(ServerPlayer player, String questId) {
        if (!ChronicleEnginePlayerData.isQuestActive(player, questId)) {
            return;
        }
        List<String> tracked = new ArrayList<>(ChronicleEnginePlayerData.trackedQuestIds(player));
        if (tracked.remove(questId)) {
            ChronicleEnginePlayerData.setTrackedQuestIds(player, tracked);
            syncTracker(player);
            return;
        }
        if (tracked.size() >= 2) {
            player.displayClientMessage(Component.translatable("message.chronicle_engine.tracker.limit").withStyle(ChatFormatting.RED), false);
            return;
        }
        tracked.add(questId);
        ChronicleEnginePlayerData.setTrackedQuestIds(player, tracked);
        syncTracker(player);
    }

    private static void initializeTrackingSelection(ServerPlayer player) {
        if (ChronicleEnginePlayerData.hasTrackingSelection(player)) {
            return;
        }
        List<String> initial = new ArrayList<>();
        for (QuestSpec quest : ChronicleEngineData.quests()) {
            if (ChronicleEnginePlayerData.isQuestActive(player, quest.id())) {
                initial.add(quest.id());
                if (initial.size() == 2) {
                    break;
                }
            }
        }
        ChronicleEnginePlayerData.setTrackedQuestIds(player, initial);
    }

    public record JournalQuest(String questId, String title, String description, boolean active, boolean completed, boolean tracked, String phaseId, String phaseTitle, String phaseDescription, List<JournalObjective> objectives, List<JournalNode> nodes) {
    }

    public record JournalNode(String phaseId, String title, String description, String status, List<JournalObjective> objectives) {
    }

    public record JournalObjective(String text, int progress, int required, String type) {
    }
}

