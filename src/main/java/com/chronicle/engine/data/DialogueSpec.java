package com.chronicle.engine.data;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DialogueSpec(String id, TextValue defaultNpc, String startNodeId, Map<String, Node> nodes) {
    public static DialogueSpec parse(JsonObject json) {
        String id = JsonUtil.string(json, "id", "");
        TextValue defaultNpc = TextValue.parse(json.get("defaultNpc"));
        String startNodeId = JsonUtil.string(json, "startNodeId", "root");
        Map<String, Node> nodes = new LinkedHashMap<>();
        for (JsonObject nodeJson : JsonUtil.objects(json, "nodes")) {
            Node node = Node.parse(nodeJson);
            nodes.put(node.nodeId(), node);
        }
        return new DialogueSpec(id, defaultNpc, startNodeId, nodes);
    }

    public Node startNode() {
        return nodes.get(startNodeId);
    }

    public record Node(String nodeId, TextValue text, List<ConditionalText> conditionalTexts, List<Choice> choices) {
        public static Node parse(JsonObject json) {
            String nodeId = JsonUtil.string(json, "nodeId", "root");
            TextValue text = TextValue.parse(json.get("text"));
            List<ConditionalText> conditionalTexts = new ArrayList<>();
            for (JsonObject value : JsonUtil.objectValues(json, "conditionalTexts")) {
                conditionalTexts.add(ConditionalText.parse(value));
            }
            List<Choice> choices = new ArrayList<>();
            for (JsonObject choiceJson : JsonUtil.objects(json, "choices")) {
                choices.add(Choice.parse(choiceJson));
            }
            return new Node(nodeId, text, conditionalTexts, choices);
        }
    }

    public record ConditionalText(String sayId, TextValue text, List<JsonObject> conditions, int priority) {
        public static ConditionalText parse(JsonObject json) {
            return new ConditionalText(
                    JsonUtil.string(json, "sayId", ""),
                    TextValue.parse(json.get("text")),
                    JsonUtil.objects(json, "conditions"),
                    JsonUtil.integer(json, "priority", 0)
            );
        }
    }

    public record Choice(String choiceId, TextValue text, String nextNodeId, List<JsonObject> conditions, List<Action> actions) {
        public static Choice parse(JsonObject json) {
            List<Action> actions = new ArrayList<>();
            for (JsonObject actionJson : JsonUtil.objects(json, "actions")) {
                actions.add(Action.parse(actionJson));
            }
            return new Choice(
                    JsonUtil.string(json, "choiceId", ""),
                    TextValue.parse(json.get("text")),
                    JsonUtil.string(json, "nextNodeId", ""),
                    JsonUtil.objects(json, "conditions"),
                    actions
            );
        }
    }

    public record Action(String type, JsonObject raw) {
        public static Action parse(JsonObject json) {
            return new Action(JsonUtil.string(json, "type", ""), json);
        }
    }

    public static List<ConditionalText> sortedConditionalTexts(Node node) {
        return node.conditionalTexts().stream()
                .sorted(Comparator.comparingInt(ConditionalText::priority).reversed())
                .toList();
    }
}

