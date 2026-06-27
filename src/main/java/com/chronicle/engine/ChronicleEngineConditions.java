package com.chronicle.engine;

import com.chronicle.engine.data.JsonUtil;
import com.google.gson.JsonObject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public final class ChronicleEngineConditions {
    private ChronicleEngineConditions() {
    }

    public static boolean all(Player player, List<JsonObject> conditions) {
        for (JsonObject condition : conditions) {
            if (!test(player, null, condition)) {
                return false;
            }
        }
        return true;
    }

    public static boolean test(Player player, JsonObject condition) {
        return test(player, null, condition);
    }

    public static boolean test(Player player, Entity entity, JsonObject condition) {
        if (condition == null || condition.entrySet().isEmpty()) {
            return true;
        }
        String type = JsonUtil.string(condition, "condition", "chronicle_engine:always");
        return switch (type) {
            case "chronicle_engine:always" -> true;
            case "chronicle_engine:has_flag" ->
                    ChronicleEnginePlayerData.hasFlag(player, JsonUtil.string(condition, "flag", ""));
            case "chronicle_engine:quest_accepted" ->
                    ChronicleEnginePlayerData.isQuestActive(player, JsonUtil.string(condition, "questId", ""));
            case "chronicle_engine:quest_not_started" -> {
                String questId = JsonUtil.string(condition, "questId", "");
                yield !ChronicleEnginePlayerData.isQuestActive(player, questId) && !ChronicleEnginePlayerData.isQuestCompleted(player, questId);
            }
            case "chronicle_engine:quest_phase" -> {
                String questId = JsonUtil.string(condition, "questId", "");
                String phaseId = JsonUtil.string(condition, "phaseId", "");
                yield ChronicleEnginePlayerData.isQuestActive(player, questId) && phaseId.equals(ChronicleEnginePlayerData.phase(player, questId));
            }
            case "chronicle_engine:and" -> {
                for (JsonObject child : JsonUtil.objects(condition, "conditions")) {
                    if (!test(player, entity, child)) {
                        yield false;
                    }
                }
                yield true;
            }
            case "chronicle_engine:or" -> {
                for (JsonObject child : JsonUtil.objects(condition, "conditions")) {
                    if (test(player, entity, child)) {
                        yield true;
                    }
                }
                yield false;
            }
            case "chronicle_engine:not" -> !test(player, entity, JsonUtil.object(condition, "inner"));
            case "chronicle_engine:entity_name" -> entityNameMatches(entity, JsonUtil.string(condition, "namePattern", ""));
            case "chronicle_engine:villager_profession" -> villagerProfessionMatches(entity, JsonUtil.string(condition, "profession", ""));
            default -> {
                ChronicleEngine.LOGGER.warn("Unknown ChronicleEngine condition type: {}", type);
                yield false;
            }
        };
    }

    private static boolean entityNameMatches(Entity entity, String pattern) {
        if (entity == null || pattern.isBlank()) {
            return false;
        }
        String name = entity.getName().getString();
        return name.equals(pattern) || name.contains(pattern) || name.matches(pattern);
    }

    private static boolean villagerProfessionMatches(Entity entity, String profession) {
        if (!(entity instanceof Villager villager) || profession.isBlank()) {
            return false;
        }
        String id = villager.getVillagerData().getProfession().toString();
        return id.equals(profession) || id.endsWith(":" + profession);
    }
}


