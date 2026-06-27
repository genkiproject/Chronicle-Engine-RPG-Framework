package com.chronicle.engine;

import com.chronicle.engine.network.ChronicleEngineNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ChronicleEngineTeamService {
    private ChronicleEngineTeamService() {
    }

    public static void open(ServerPlayer player) {
        ChronicleEngineNetwork.openTeams(player, snapshot(player));
    }

    public static ChronicleEngineNetwork.OpenTeamsPacket snapshot(ServerPlayer viewer) {
        MinecraftServer server = viewer.getServer();
        ChronicleEngineTeamSavedData data = ChronicleEngineTeamSavedData.get(server);
        rememberOnlinePlayers(data, server);

        boolean serverAdmin = isServerAdmin(viewer);
        UUID viewerId = viewer.getUUID();
        List<ChronicleEngineNetwork.TeamLine> teams = new ArrayList<>();
        List<ChronicleEngineNetwork.TeamRequestLine> notifications = new ArrayList<>();
        for (ChronicleEngineTeamSavedData.Team team : sortedTeams(data)) {
            boolean member = team.members.contains(viewerId);
            boolean owner = team.owner.equals(viewerId);
            boolean teamAdmin = team.admins.contains(viewerId);
            boolean canManage = owner || teamAdmin || serverAdmin;
            if (canManage) {
                for (UUID pending : team.pending) {
                    notifications.add(new ChronicleEngineNetwork.TeamRequestLine(
                            team.id.toString(),
                            team.name,
                            pending.toString(),
                            name(data, pending)
                    ));
                }
            }
            teams.add(new ChronicleEngineNetwork.TeamLine(
                    team.id.toString(),
                    team.name,
                    name(data, team.owner),
                    team.owner.toString(),
                    member,
                    owner,
                    teamAdmin,
                    canManage,
                    team.friendlyFire,
                    team.pending.contains(viewerId),
                    team.blacklist.contains(viewerId),
                    members(data, server, team.members),
                    members(data, server, team.admins),
                    members(data, server, team.blacklist),
                    requests(data, team)
            ));
        }
        return new ChronicleEngineNetwork.OpenTeamsPacket(
                teams,
                onlinePlayers(data, server),
                notifications,
                serverAdmin,
                ownedTeams(data, viewerId),
                ChronicleEngineConfig.maxOwnedTeams()
        );
    }

    public static void handleAction(ServerPlayer player, String action, String teamIdRaw, String targetIdRaw) {
        MinecraftServer server = player.getServer();
        ChronicleEngineTeamSavedData data = ChronicleEngineTeamSavedData.get(server);
        rememberOnlinePlayers(data, server);
        UUID teamId = parseUuid(teamIdRaw);
        UUID targetId = parseUuid(targetIdRaw);
        UUID playerId = player.getUUID();
        boolean serverAdmin = isServerAdmin(player);
        ChronicleEngineTeamSavedData.Team team = teamId == null ? null : data.teams().get(teamId);

        switch (action) {
            case "CREATE" -> createTeam(player, data);
            case "REQUEST_JOIN" -> {
                if (team != null) {
                    requestJoin(player, data, team, serverAdmin);
                }
            }
            case "APPROVE" -> {
                if (team != null && targetId != null && canManage(team, playerId, serverAdmin)) {
                    team.pending.remove(targetId);
                    team.blacklist.remove(targetId);
                    team.members.add(targetId);
                    data.setDirty();
                }
            }
            case "DENY" -> {
                if (team != null && targetId != null && canManage(team, playerId, serverAdmin)) {
                    team.pending.remove(targetId);
                    data.setDirty();
                }
            }
            case "LEAVE" -> {
                if (team != null && team.members.contains(playerId) && !team.owner.equals(playerId)) {
                    team.members.remove(playerId);
                    team.admins.remove(playerId);
                    team.pending.remove(playerId);
                    data.setDirty();
                }
            }
            case "DISBAND" -> {
                if (team != null && (team.owner.equals(playerId) || serverAdmin)) {
                    data.teams().remove(team.id);
                    data.setDirty();
                }
            }
            case "TOGGLE_FRIENDLY_FIRE" -> {
                if (team != null && canManage(team, playerId, serverAdmin)) {
                    team.friendlyFire = !team.friendlyFire;
                    data.setDirty();
                }
            }
            case "ADD_ADMIN" -> {
                if (team != null && targetId != null && canManage(team, playerId, serverAdmin) && team.members.contains(targetId) && !team.owner.equals(targetId)) {
                    team.admins.add(targetId);
                    data.setDirty();
                }
            }
            case "REMOVE_ADMIN" -> {
                if (team != null && targetId != null && canManage(team, playerId, serverAdmin)) {
                    team.admins.remove(targetId);
                    data.setDirty();
                }
            }
            case "TRANSFER" -> {
                if (team != null && targetId != null && (team.owner.equals(playerId) || serverAdmin) && team.members.contains(targetId)) {
                    team.admins.remove(targetId);
                    team.admins.add(team.owner);
                    team.owner = targetId;
                    team.members.add(targetId);
                    data.setDirty();
                }
            }
            case "KICK", "KICK_BLACKLIST" -> {
                if (team != null && targetId != null && canManage(team, playerId, serverAdmin) && !team.owner.equals(targetId)) {
                    team.members.remove(targetId);
                    team.admins.remove(targetId);
                    team.pending.remove(targetId);
                    if ("KICK_BLACKLIST".equals(action)) {
                        team.blacklist.add(targetId);
                    }
                    data.setDirty();
                }
            }
            case "UNBLACKLIST" -> {
                if (team != null && targetId != null && canManage(team, playerId, serverAdmin)) {
                    team.blacklist.remove(targetId);
                    data.setDirty();
                }
            }
            default -> {
                // Ignore unknown client actions from older/newer builds.
            }
        }
        open(player);
    }

    public static boolean shouldBlockFriendlyFire(ServerPlayer attacker, Player target) {
        MinecraftServer server = attacker.getServer();
        if (!(target instanceof ServerPlayer victim) || server == null) {
            return false;
        }
        ChronicleEngineTeamSavedData data = ChronicleEngineTeamSavedData.get(server);
        UUID attackerId = attacker.getUUID();
        UUID victimId = victim.getUUID();
        for (ChronicleEngineTeamSavedData.Team team : data.teams().values()) {
            if (!team.friendlyFire && team.members.contains(attackerId) && team.members.contains(victimId)) {
                return true;
            }
        }
        return false;
    }

    public static void propagateQuestAccept(ServerPlayer source, String questId) {
        for (ServerPlayer member : onlineTeamMembersForQuestSync(source)) {
            ChronicleEngineQuestService.acceptFromTeam(member, questId);
        }
    }

    public static void propagateObjective(ServerPlayer source, String type, String targetId, int amount) {
        for (ServerPlayer member : onlineTeamMembersForQuestSync(source)) {
            ChronicleEngineQuestService.notifyObjectiveFromTeam(member, type, targetId, amount);
        }
    }

    public static void propagateAdvancement(ServerPlayer source, String advancementId) {
        for (ServerPlayer member : onlineTeamMembersForQuestSync(source)) {
            ChronicleEngineQuestService.notifyAdvancementFromTeam(member, advancementId);
        }
    }

    private static void createTeam(ServerPlayer player, ChronicleEngineTeamSavedData data) {
        int max = ChronicleEngineConfig.maxOwnedTeams();
        if (max <= 0 || ownedTeams(data, player.getUUID()) >= max) {
            player.displayClientMessage(Component.translatable("message.chronicle_engine.team.create_limit", max).withStyle(ChatFormatting.RED), false);
            return;
        }
        int index = ownedTeams(data, player.getUUID()) + 1;
        String name = player.getGameProfile().getName() + "'s Team " + index;
        ChronicleEngineTeamSavedData.Team team = new ChronicleEngineTeamSavedData.Team(UUID.randomUUID(), name, player.getUUID());
        data.teams().put(team.id, team);
        data.remember(player.getUUID(), player.getGameProfile().getName());
        data.setDirty();
    }

    private static void requestJoin(ServerPlayer player, ChronicleEngineTeamSavedData data, ChronicleEngineTeamSavedData.Team team, boolean serverAdmin) {
        UUID playerId = player.getUUID();
        if (team.blacklist.contains(playerId) && !serverAdmin) {
            player.displayClientMessage(Component.translatable("message.chronicle_engine.team.blacklisted").withStyle(ChatFormatting.RED), false);
            return;
        }
        if (serverAdmin) {
            team.members.add(playerId);
            team.pending.remove(playerId);
        } else if (!team.members.contains(playerId)) {
            team.pending.add(playerId);
        }
        data.remember(playerId, player.getGameProfile().getName());
        data.setDirty();
    }

    public static List<ServerPlayer> onlineTeamMembersForQuestSync(ServerPlayer source) {
        MinecraftServer server = source.getServer();
        ChronicleEngineTeamSavedData data = ChronicleEngineTeamSavedData.get(server);
        UUID sourceId = source.getUUID();
        Set<UUID> ids = new LinkedHashSet<>();
        for (ChronicleEngineTeamSavedData.Team team : data.teams().values()) {
            if (team.members.contains(sourceId)) {
                ids.addAll(team.members);
            }
        }
        ids.remove(sourceId);
        List<ServerPlayer> players = new ArrayList<>();
        for (UUID id : ids) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    private static boolean canManage(ChronicleEngineTeamSavedData.Team team, UUID player, boolean serverAdmin) {
        return serverAdmin || team.owner.equals(player) || team.admins.contains(player);
    }

    private static int ownedTeams(ChronicleEngineTeamSavedData data, UUID owner) {
        int count = 0;
        for (ChronicleEngineTeamSavedData.Team team : data.teams().values()) {
            if (team.owner.equals(owner)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isServerAdmin(ServerPlayer player) {
        return player.hasPermissions(2);
    }

    private static void rememberOnlinePlayers(ChronicleEngineTeamSavedData data, MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            data.remember(player.getUUID(), player.getGameProfile().getName());
        }
    }

    private static List<ChronicleEngineTeamSavedData.Team> sortedTeams(ChronicleEngineTeamSavedData data) {
        return data.teams().values().stream()
                .sorted(Comparator.comparing(team -> team.name.toLowerCase()))
                .toList();
    }

    private static List<ChronicleEngineNetwork.TeamMemberLine> onlinePlayers(ChronicleEngineTeamSavedData data, MinecraftServer server) {
        List<ChronicleEngineNetwork.TeamMemberLine> result = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            result.add(new ChronicleEngineNetwork.TeamMemberLine(player.getUUID().toString(), player.getGameProfile().getName(), true));
            data.remember(player.getUUID(), player.getGameProfile().getName());
        }
        result.sort(Comparator.comparing(ChronicleEngineNetwork.TeamMemberLine::name, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static List<ChronicleEngineNetwork.TeamMemberLine> members(ChronicleEngineTeamSavedData data, MinecraftServer server, Set<UUID> ids) {
        List<ChronicleEngineNetwork.TeamMemberLine> result = new ArrayList<>();
        for (UUID id : ids) {
            result.add(new ChronicleEngineNetwork.TeamMemberLine(id.toString(), name(data, id), server.getPlayerList().getPlayer(id) != null));
        }
        result.sort(Comparator.comparing(ChronicleEngineNetwork.TeamMemberLine::name, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static List<ChronicleEngineNetwork.TeamRequestLine> requests(ChronicleEngineTeamSavedData data, ChronicleEngineTeamSavedData.Team team) {
        List<ChronicleEngineNetwork.TeamRequestLine> result = new ArrayList<>();
        for (UUID id : team.pending) {
            result.add(new ChronicleEngineNetwork.TeamRequestLine(team.id.toString(), team.name, id.toString(), name(data, id)));
        }
        result.sort(Comparator.comparing(ChronicleEngineNetwork.TeamRequestLine::playerName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static String name(ChronicleEngineTeamSavedData data, UUID id) {
        return data.knownNames().getOrDefault(id, id.toString().substring(0, 8));
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
