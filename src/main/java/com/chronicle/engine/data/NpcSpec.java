package com.chronicle.engine.data;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record NpcSpec(
        String id,
        String entityType,
        List<Binding> bindings,
        boolean cancelVanillaInteract,
        double dialogueDistance,
        boolean shouldLookAtPlayer,
        boolean shouldStopMoving
) {
    public static NpcSpec parse(String path, JsonObject json) {
        List<Binding> bindings = new ArrayList<>();
        for (JsonObject bindingJson : JsonUtil.objects(json, "bindings")) {
            bindings.add(Binding.parse(bindingJson));
        }
        bindings.sort(Comparator.comparingInt(Binding::priority).reversed());
        return new NpcSpec(
                path,
                JsonUtil.string(json, "entityType", ""),
                bindings,
                JsonUtil.bool(json, "cancelVanillaInteract", true),
                JsonUtil.integer(json, "dialogueDistance", 8),
                JsonUtil.bool(json, "shouldLookAtPlayer", true),
                JsonUtil.bool(json, "shouldStopMoving", true)
        );
    }

    public record Binding(String bindingId, String dialogueId, String dialogueIdFromNbt, JsonObject condition, int priority) {
        public static Binding parse(JsonObject json) {
            return new Binding(
                    JsonUtil.string(json, "bindingId", ""),
                    JsonUtil.string(json, "dialogueId", ""),
                    JsonUtil.string(json, "dialogueIdFromNbt", ""),
                    JsonUtil.object(json, "condition"),
                    JsonUtil.integer(json, "priority", 0)
            );
        }
    }
}

