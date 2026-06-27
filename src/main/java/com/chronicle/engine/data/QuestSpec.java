package com.chronicle.engine.data;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record QuestSpec(
        String id,
        String category,
        TextValue displayName,
        TextValue description,
        int sortOrder,
        boolean repeatable,
        boolean teamSync,
        String mode,
        String initialPhaseId,
        List<JsonObject> unlockConditions,
        List<String> flagsToSetOnAccept,
        List<String> flagsToSetOnComplete,
        List<Reward> completionRewards,
        Map<String, Phase> phases
) {
    public static QuestSpec parse(JsonObject json) {
        Map<String, Phase> phases = new LinkedHashMap<>();
        for (JsonObject phaseJson : JsonUtil.objects(json, "phases")) {
            Phase phase = Phase.parse(phaseJson);
            phases.put(phase.phaseId(), phase);
        }
        return new QuestSpec(
                JsonUtil.string(json, "id", ""),
                JsonUtil.string(json, "category", ""),
                TextValue.parse(json.get("displayName")),
                TextValue.parse(json.get("description")),
                JsonUtil.integer(json, "sortOrder", 0),
                JsonUtil.bool(json, "repeatable", false),
                JsonUtil.bool(json, "teamSync", false),
                JsonUtil.string(json, "mode", "PROGRESSION"),
                JsonUtil.string(json, "initialPhaseId", ""),
                JsonUtil.objects(json, "unlockConditions"),
                JsonUtil.strings(json, "flagsToSetOnAccept"),
                JsonUtil.strings(json, "flagsToSetOnComplete"),
                Reward.parseList(json, "completionRewards"),
                phases
        );
    }

    public Phase initialPhase() {
        return phases.get(initialPhaseId);
    }

    public record Phase(
            String phaseId,
            TextValue displayName,
            TextValue description,
            TextValue story,
            List<String> flagsToSetOnEnter,
            List<String> flagsToSetOnComplete,
            List<Objective> objectives,
            List<Transition> transitions,
            List<Reward> phaseRewards
    ) {
        public static Phase parse(JsonObject json) {
            List<Objective> objectives = new ArrayList<>();
            for (JsonObject objectiveJson : JsonUtil.objects(json, "objectives")) {
                objectives.add(Objective.parse(objectiveJson));
            }
            List<Transition> transitions = new ArrayList<>();
            for (JsonObject transitionJson : JsonUtil.objects(json, "transitions")) {
                transitions.add(Transition.parse(transitionJson));
            }
            return new Phase(
                    JsonUtil.string(json, "phaseId", ""),
                    TextValue.parse(json.get("displayName")),
                    TextValue.parse(json.get("description")),
                    TextValue.parse(json.get("story")),
                    JsonUtil.strings(json, "flagsToSetOnEnter"),
                    JsonUtil.strings(json, "flagsToSetOnComplete"),
                    objectives,
                    transitions,
                    Reward.parseList(json, "phaseRewards")
            );
        }
    }

    public record Objective(
            String type,
            String targetId,
            int requiredCount,
            TextValue displayText,
            boolean hidden,
            boolean optional,
            JsonObject extraData
    ) {
        public static Objective parse(JsonObject json) {
            return new Objective(
                    JsonUtil.string(json, "type", ""),
                    JsonUtil.string(json, "targetId", ""),
                    JsonUtil.integer(json, "requiredCount", 1),
                    TextValue.parse(json.get("displayText")),
                    JsonUtil.bool(json, "hidden", false),
                    JsonUtil.bool(json, "optional", false),
                    JsonUtil.object(json, "extraData")
            );
        }

        public String key() {
            return type + "|" + targetId;
        }
    }

    public record Transition(String targetPhaseId, JsonObject condition) {
        public static Transition parse(JsonObject json) {
            return new Transition(
                    JsonUtil.string(json, "targetPhaseId", ""),
                    JsonUtil.object(json, "condition")
            );
        }
    }

    public record Reward(String type, JsonObject raw) {
        public static Reward parse(JsonObject json) {
            return new Reward(JsonUtil.string(json, "type", ""), json);
        }

        public static List<Reward> parseList(JsonObject owner, String key) {
            List<Reward> rewards = new ArrayList<>();
            for (JsonObject rewardJson : JsonUtil.objects(owner, key)) {
                rewards.add(parse(rewardJson));
            }
            return rewards;
        }
    }
}

