package com.chronicle.engine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class ChronicleEnginePlayerData {
    private static final String ROOT = ChronicleEngine.MOD_ID;
    private static final String FLAGS = "flags";
    private static final String QUESTS = "quests";
    private static final String TRACKED_QUESTS = "trackedQuests";

    private ChronicleEnginePlayerData() {
    }

    public static CompoundTag root(Player player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) {
            persistent.put(ROOT, new CompoundTag());
        }
        return persistent.getCompound(ROOT);
    }

    public static CompoundTag flags(Player player) {
        CompoundTag root = root(player);
        if (!root.contains(FLAGS)) {
            root.put(FLAGS, new CompoundTag());
        }
        return root.getCompound(FLAGS);
    }

    public static boolean hasFlag(Player player, String flag) {
        return flags(player).getBoolean(flag);
    }

    public static void setFlag(Player player, String flag) {
        if (flag != null && !flag.isBlank()) {
            flags(player).putBoolean(flag, true);
        }
    }

    public static CompoundTag quests(Player player) {
        CompoundTag root = root(player);
        if (!root.contains(QUESTS)) {
            root.put(QUESTS, new CompoundTag());
        }
        return root.getCompound(QUESTS);
    }

    public static CompoundTag quest(Player player, String questId) {
        CompoundTag quests = quests(player);
        if (!quests.contains(questId)) {
            quests.put(questId, new CompoundTag());
        }
        return quests.getCompound(questId);
    }

    public static boolean isQuestActive(Player player, String questId) {
        return "active".equals(quest(player, questId).getString("status"));
    }

    public static boolean isQuestCompleted(Player player, String questId) {
        return "completed".equals(quest(player, questId).getString("status"));
    }

    public static String phase(Player player, String questId) {
        return quest(player, questId).getString("phase");
    }

    public static CompoundTag progress(Player player, String questId) {
        CompoundTag quest = quest(player, questId);
        if (!quest.contains("progress")) {
            quest.put("progress", new CompoundTag());
        }
        return quest.getCompound("progress");
    }

    public static boolean hasTrackingSelection(Player player) {
        return root(player).contains(TRACKED_QUESTS, Tag.TAG_LIST);
    }

    public static List<String> trackedQuestIds(Player player) {
        ListTag tracked = root(player).getList(TRACKED_QUESTS, Tag.TAG_STRING);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < tracked.size(); i++) {
            String questId = tracked.getString(i);
            if (!questId.isBlank() && !result.contains(questId)) {
                result.add(questId);
            }
        }
        return result;
    }

    public static void setTrackedQuestIds(Player player, List<String> questIds) {
        ListTag tracked = new ListTag();
        for (String questId : questIds.stream().distinct().limit(2).toList()) {
            if (!questId.isBlank()) {
                tracked.add(StringTag.valueOf(questId));
            }
        }
        root(player).put(TRACKED_QUESTS, tracked);
    }

    public static boolean isQuestTracked(Player player, String questId) {
        return trackedQuestIds(player).contains(questId);
    }

    public static void untrackQuest(Player player, String questId) {
        List<String> tracked = new ArrayList<>(trackedQuestIds(player));
        if (tracked.remove(questId)) {
            setTrackedQuestIds(player, tracked);
        }
    }

    public static void clearAll(Player player) {
        player.getPersistentData().remove(ROOT);
    }

    public static void copyOnClone(Player original, Player clone) {
        if (original.getPersistentData().contains(ROOT)) {
            clone.getPersistentData().put(ROOT, original.getPersistentData().getCompound(ROOT).copy());
        }
    }
}

