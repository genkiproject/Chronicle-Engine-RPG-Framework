package com.chronicle.engine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChronicleEngineTeamSavedData extends SavedData {
    private static final String DATA_NAME = ChronicleEngine.MOD_ID + "_teams";

    private final Map<UUID, Team> teams = new LinkedHashMap<>();
    private final Map<UUID, String> knownNames = new LinkedHashMap<>();

    public static ChronicleEngineTeamSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return new ChronicleEngineTeamSavedData();
        }
        return overworld.getDataStorage().computeIfAbsent(ChronicleEngineTeamSavedData::load, ChronicleEngineTeamSavedData::new, DATA_NAME);
    }

    public static ChronicleEngineTeamSavedData load(CompoundTag tag) {
        ChronicleEngineTeamSavedData data = new ChronicleEngineTeamSavedData();
        ListTag teamList = tag.getList("teams", Tag.TAG_COMPOUND);
        for (int i = 0; i < teamList.size(); i++) {
            Team team = Team.load(teamList.getCompound(i));
            if (team != null) {
                data.teams.put(team.id, team);
            }
        }
        ListTag nameList = tag.getList("knownNames", Tag.TAG_COMPOUND);
        for (int i = 0; i < nameList.size(); i++) {
            CompoundTag entry = nameList.getCompound(i);
            UUID id = readUuid(entry, "uuid");
            String name = entry.getString("name");
            if (id != null && !name.isBlank()) {
                data.knownNames.put(id, name);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag teamList = new ListTag();
        for (Team team : teams.values()) {
            teamList.add(team.save());
        }
        tag.put("teams", teamList);

        ListTag nameList = new ListTag();
        for (Map.Entry<UUID, String> entry : knownNames.entrySet()) {
            CompoundTag name = new CompoundTag();
            name.putString("uuid", entry.getKey().toString());
            name.putString("name", entry.getValue());
            nameList.add(name);
        }
        tag.put("knownNames", nameList);
        return tag;
    }

    public Map<UUID, Team> teams() {
        return teams;
    }

    public Map<UUID, String> knownNames() {
        return knownNames;
    }

    public void remember(UUID id, String name) {
        if (id != null && name != null && !name.isBlank()) {
            knownNames.put(id, name);
            setDirty();
        }
    }

    static UUID readUuid(CompoundTag tag, String key) {
        String value = tag.getString(key);
        if (value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    static ListTag saveUuidSet(Set<UUID> ids) {
        ListTag list = new ListTag();
        for (UUID id : ids) {
            list.add(StringTag.valueOf(id.toString()));
        }
        return list;
    }

    static Set<UUID> loadUuidSet(CompoundTag tag, String key) {
        Set<UUID> result = new LinkedHashSet<>();
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            try {
                result.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed entries from hand-edited saves.
            }
        }
        return result;
    }

    public static class Team {
        public final UUID id;
        public String name;
        public UUID owner;
        public boolean friendlyFire;
        public final Set<UUID> members = new LinkedHashSet<>();
        public final Set<UUID> admins = new LinkedHashSet<>();
        public final Set<UUID> pending = new LinkedHashSet<>();
        public final Set<UUID> blacklist = new LinkedHashSet<>();

        public Team(UUID id, String name, UUID owner) {
            this.id = id;
            this.name = name;
            this.owner = owner;
            this.friendlyFire = false;
            this.members.add(owner);
        }

        static Team load(CompoundTag tag) {
            UUID id = readUuid(tag, "id");
            UUID owner = readUuid(tag, "owner");
            if (id == null || owner == null) {
                return null;
            }
            Team team = new Team(id, tag.getString("name"), owner);
            team.friendlyFire = tag.getBoolean("friendlyFire");
            team.members.clear();
            team.members.addAll(loadUuidSet(tag, "members"));
            team.members.add(owner);
            team.admins.addAll(loadUuidSet(tag, "admins"));
            team.pending.addAll(loadUuidSet(tag, "pending"));
            team.blacklist.addAll(loadUuidSet(tag, "blacklist"));
            team.admins.remove(owner);
            team.pending.remove(owner);
            team.blacklist.remove(owner);
            return team;
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", id.toString());
            tag.putString("name", name);
            tag.putString("owner", owner.toString());
            tag.putBoolean("friendlyFire", friendlyFire);
            tag.put("members", saveUuidSet(members));
            tag.put("admins", saveUuidSet(admins));
            tag.put("pending", saveUuidSet(pending));
            tag.put("blacklist", saveUuidSet(blacklist));
            return tag;
        }
    }
}
