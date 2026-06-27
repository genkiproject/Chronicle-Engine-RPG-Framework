package com.chronicle.engine.network;

import com.chronicle.engine.ChronicleEngine;
import com.chronicle.engine.ChronicleEngineDialogueService;
import com.chronicle.engine.ChronicleEngineNpcFocusService;
import com.chronicle.engine.ChronicleEngineQuestService;
import com.chronicle.engine.ChronicleEngineShopService;
import com.chronicle.engine.ChronicleEngineTeamService;
import com.chronicle.engine.ChronicleEngineWalletService;
import com.chronicle.engine.client.ChronicleEngineClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ChronicleEngineNetwork {
    private static final String PROTOCOL = "6";
    private static int nextId = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ChronicleEngine.id("main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private ChronicleEngineNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(nextId++, OpenDialoguePacket.class, OpenDialoguePacket::encode, OpenDialoguePacket::decode, OpenDialoguePacket::handle);
        CHANNEL.registerMessage(nextId++, ChooseDialoguePacket.class, ChooseDialoguePacket::encode, ChooseDialoguePacket::decode, ChooseDialoguePacket::handle);
        CHANNEL.registerMessage(nextId++, OpenShopPacket.class, OpenShopPacket::encode, OpenShopPacket::decode, OpenShopPacket::handle);
        CHANNEL.registerMessage(nextId++, BuyShopPacket.class, BuyShopPacket::encode, BuyShopPacket::decode, BuyShopPacket::handle);
        CHANNEL.registerMessage(nextId++, OpenJournalPacket.class, OpenJournalPacket::encode, OpenJournalPacket::decode, OpenJournalPacket::handle);
        CHANNEL.registerMessage(nextId++, OpenJournalRequestPacket.class, OpenJournalRequestPacket::encode, OpenJournalRequestPacket::decode, OpenJournalRequestPacket::handle);
        CHANNEL.registerMessage(nextId++, OfferQuestPacket.class, OfferQuestPacket::encode, OfferQuestPacket::decode, OfferQuestPacket::handle);
        CHANNEL.registerMessage(nextId++, SyncMarkersPacket.class, SyncMarkersPacket::encode, SyncMarkersPacket::decode, SyncMarkersPacket::handle);
        CHANNEL.registerMessage(nextId++, SyncTrackerPacket.class, SyncTrackerPacket::encode, SyncTrackerPacket::decode, SyncTrackerPacket::handle);
        CHANNEL.registerMessage(nextId++, CloseScreenPacket.class, CloseScreenPacket::encode, CloseScreenPacket::decode, CloseScreenPacket::handle);
        CHANNEL.registerMessage(nextId++, EndDialoguePacket.class, EndDialoguePacket::encode, EndDialoguePacket::decode, EndDialoguePacket::handle);
        CHANNEL.registerMessage(nextId++, ToggleTrackPacket.class, ToggleTrackPacket::encode, ToggleTrackPacket::decode, ToggleTrackPacket::handle);
        CHANNEL.registerMessage(nextId++, OpenWalletPacket.class, OpenWalletPacket::encode, OpenWalletPacket::decode, OpenWalletPacket::handle);
        CHANNEL.registerMessage(nextId++, OpenWalletRequestPacket.class, OpenWalletRequestPacket::encode, OpenWalletRequestPacket::decode, OpenWalletRequestPacket::handle);
        CHANNEL.registerMessage(nextId++, WithdrawWalletPacket.class, WithdrawWalletPacket::encode, WithdrawWalletPacket::decode, WithdrawWalletPacket::handle);
        CHANNEL.registerMessage(nextId++, OpenTeamsPacket.class, OpenTeamsPacket::encode, OpenTeamsPacket::decode, OpenTeamsPacket::handle);
        CHANNEL.registerMessage(nextId++, OpenTeamsRequestPacket.class, OpenTeamsRequestPacket::encode, OpenTeamsRequestPacket::decode, OpenTeamsRequestPacket::handle);
        CHANNEL.registerMessage(nextId++, TeamActionPacket.class, TeamActionPacket::encode, TeamActionPacket::decode, TeamActionPacket::handle);
    }

    public static void openDialogue(ServerPlayer player, OpenDialoguePacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void openShop(ServerPlayer player, OpenShopPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void openJournal(ServerPlayer player, OpenJournalPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void openWallet(ServerPlayer player, OpenWalletPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void openTeams(ServerPlayer player, OpenTeamsPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendMarkers(ServerPlayer player, List<MarkerLine> markers) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncMarkersPacket(markers));
    }

    public static void sendTrackers(ServerPlayer player, List<TrackerQuest> trackers) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncTrackerPacket(trackers));
    }

    public static void closeScreen(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CloseScreenPacket());
    }

    public static void requestJournal() {
        CHANNEL.sendToServer(new OpenJournalRequestPacket());
    }

    public static void requestWallet() {
        CHANNEL.sendToServer(new OpenWalletRequestPacket());
    }

    public static void requestTeams() {
        CHANNEL.sendToServer(new OpenTeamsRequestPacket());
    }

    public static void chooseDialogue(String dialogueId, String nodeId, String choiceId) {
        CHANNEL.sendToServer(new ChooseDialoguePacket(dialogueId, nodeId, choiceId));
    }

    public static void buyShopEntry(String shopId, String entryId) {
        CHANNEL.sendToServer(new BuyShopPacket(shopId, entryId));
    }

    public static void offerQuest(String questId) {
        CHANNEL.sendToServer(new OfferQuestPacket(questId));
    }

    public static void toggleTrack(String questId) {
        CHANNEL.sendToServer(new ToggleTrackPacket(questId));
    }

    public static void withdrawWallet(String itemId, long amount) {
        CHANNEL.sendToServer(new WithdrawWalletPacket(itemId, amount));
    }

    public static void teamAction(String action, String teamId, String targetId) {
        CHANNEL.sendToServer(new TeamActionPacket(action, teamId == null ? "" : teamId, targetId == null ? "" : targetId));
    }

    public static void endDialogue() {
        CHANNEL.sendToServer(new EndDialoguePacket());
    }

    private static void writeChoice(FriendlyByteBuf buffer, ChoiceLine choice) {
        buffer.writeUtf(choice.choiceId());
        buffer.writeComponent(choice.text());
    }

    private static ChoiceLine readChoice(FriendlyByteBuf buffer) {
        return new ChoiceLine(buffer.readUtf(), buffer.readComponent());
    }

    private static void writeShopEntry(FriendlyByteBuf buffer, ShopEntryLine entry) {
        buffer.writeUtf(entry.entryId());
        buffer.writeUtf(entry.category());
        buffer.writeComponent(entry.name());
        buffer.writeUtf(entry.costs());
        buffer.writeUtf(entry.rewards());
        writeShopItems(buffer, entry.costItems());
        writeShopItems(buffer, entry.rewardItems());
    }

    private static ShopEntryLine readShopEntry(FriendlyByteBuf buffer) {
        return new ShopEntryLine(buffer.readUtf(), buffer.readUtf(), buffer.readComponent(), buffer.readUtf(), buffer.readUtf(), readShopItems(buffer), readShopItems(buffer));
    }

    private static void writeShopItems(FriendlyByteBuf buffer, List<ShopItemLine> items) {
        buffer.writeVarInt(items.size());
        for (ShopItemLine item : items) {
            buffer.writeUtf(item.itemId());
            buffer.writeVarInt(item.count());
            buffer.writeUtf(item.enchantmentId());
            buffer.writeVarInt(item.enchantmentLevel());
            buffer.writeUtf(item.nbt());
        }
    }

    private static List<ShopItemLine> readShopItems(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<ShopItemLine> items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            items.add(new ShopItemLine(buffer.readUtf(), buffer.readVarInt(), buffer.readUtf(), buffer.readVarInt(), buffer.readUtf()));
        }
        return items;
    }

    private static void writeShopCategory(FriendlyByteBuf buffer, ShopCategoryLine category) {
        buffer.writeUtf(category.categoryId());
        buffer.writeComponent(category.name());
    }

    private static ShopCategoryLine readShopCategory(FriendlyByteBuf buffer) {
        return new ShopCategoryLine(buffer.readUtf(), buffer.readComponent());
    }

    private static void writeWalletLine(FriendlyByteBuf buffer, WalletLine line) {
        buffer.writeUtf(line.currencyId());
        buffer.writeUtf(line.itemId());
        buffer.writeComponent(line.name());
        buffer.writeVarLong(line.amount());
    }

    private static WalletLine readWalletLine(FriendlyByteBuf buffer) {
        return new WalletLine(buffer.readUtf(), buffer.readUtf(), buffer.readComponent(), buffer.readVarLong());
    }

    private static void writeTeamMember(FriendlyByteBuf buffer, TeamMemberLine member) {
        buffer.writeUtf(member.playerId());
        buffer.writeUtf(member.name());
        buffer.writeBoolean(member.online());
    }

    private static TeamMemberLine readTeamMember(FriendlyByteBuf buffer) {
        return new TeamMemberLine(buffer.readUtf(), buffer.readUtf(), buffer.readBoolean());
    }

    private static void writeTeamRequest(FriendlyByteBuf buffer, TeamRequestLine request) {
        buffer.writeUtf(request.teamId());
        buffer.writeUtf(request.teamName());
        buffer.writeUtf(request.playerId());
        buffer.writeUtf(request.playerName());
    }

    private static TeamRequestLine readTeamRequest(FriendlyByteBuf buffer) {
        return new TeamRequestLine(buffer.readUtf(), buffer.readUtf(), buffer.readUtf(), buffer.readUtf());
    }

    private static void writeTeamMembers(FriendlyByteBuf buffer, List<TeamMemberLine> members) {
        buffer.writeVarInt(members.size());
        for (TeamMemberLine member : members) {
            writeTeamMember(buffer, member);
        }
    }

    private static List<TeamMemberLine> readTeamMembers(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<TeamMemberLine> members = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            members.add(readTeamMember(buffer));
        }
        return members;
    }

    private static void writeTeamRequests(FriendlyByteBuf buffer, List<TeamRequestLine> requests) {
        buffer.writeVarInt(requests.size());
        for (TeamRequestLine request : requests) {
            writeTeamRequest(buffer, request);
        }
    }

    private static List<TeamRequestLine> readTeamRequests(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<TeamRequestLine> requests = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            requests.add(readTeamRequest(buffer));
        }
        return requests;
    }

    private static void writeTeamLine(FriendlyByteBuf buffer, TeamLine team) {
        buffer.writeUtf(team.teamId());
        buffer.writeUtf(team.name());
        buffer.writeUtf(team.ownerName());
        buffer.writeUtf(team.ownerId());
        buffer.writeBoolean(team.member());
        buffer.writeBoolean(team.owner());
        buffer.writeBoolean(team.admin());
        buffer.writeBoolean(team.canManage());
        buffer.writeBoolean(team.friendlyFire());
        buffer.writeBoolean(team.pendingForMe());
        buffer.writeBoolean(team.blacklisted());
        writeTeamMembers(buffer, team.members());
        writeTeamMembers(buffer, team.admins());
        writeTeamMembers(buffer, team.blacklist());
        writeTeamRequests(buffer, team.requests());
    }

    private static TeamLine readTeamLine(FriendlyByteBuf buffer) {
        return new TeamLine(
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                readTeamMembers(buffer),
                readTeamMembers(buffer),
                readTeamMembers(buffer),
                readTeamRequests(buffer)
        );
    }

    private static void writeJournalQuest(FriendlyByteBuf buffer, ChronicleEngineQuestService.JournalQuest quest) {
        buffer.writeUtf(quest.questId());
        buffer.writeUtf(quest.title());
        buffer.writeUtf(quest.description());
        buffer.writeBoolean(quest.active());
        buffer.writeBoolean(quest.completed());
        buffer.writeBoolean(quest.tracked());
        buffer.writeUtf(quest.phaseId());
        buffer.writeUtf(quest.phaseTitle());
        buffer.writeUtf(quest.phaseDescription());
        buffer.writeVarInt(quest.objectives().size());
        for (ChronicleEngineQuestService.JournalObjective objective : quest.objectives()) {
            buffer.writeUtf(objective.text());
            buffer.writeVarInt(objective.progress());
            buffer.writeVarInt(objective.required());
            buffer.writeUtf(objective.type());
        }
        buffer.writeVarInt(quest.nodes().size());
        for (ChronicleEngineQuestService.JournalNode node : quest.nodes()) {
            writeJournalNode(buffer, node);
        }
    }

    private static ChronicleEngineQuestService.JournalQuest readJournalQuest(FriendlyByteBuf buffer) {
        String questId = buffer.readUtf();
        String title = buffer.readUtf();
        String description = buffer.readUtf();
        boolean active = buffer.readBoolean();
        boolean completed = buffer.readBoolean();
        boolean tracked = buffer.readBoolean();
        String phaseId = buffer.readUtf();
        String phaseTitle = buffer.readUtf();
        String phaseDescription = buffer.readUtf();
        int size = buffer.readVarInt();
        List<ChronicleEngineQuestService.JournalObjective> objectives = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            objectives.add(new ChronicleEngineQuestService.JournalObjective(buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf()));
        }
        int nodeSize = buffer.readVarInt();
        List<ChronicleEngineQuestService.JournalNode> nodes = new ArrayList<>();
        for (int i = 0; i < nodeSize; i++) {
            nodes.add(readJournalNode(buffer));
        }
        return new ChronicleEngineQuestService.JournalQuest(questId, title, description, active, completed, tracked, phaseId, phaseTitle, phaseDescription, objectives, nodes);
    }

    private static void writeJournalNode(FriendlyByteBuf buffer, ChronicleEngineQuestService.JournalNode node) {
        buffer.writeUtf(node.phaseId());
        buffer.writeUtf(node.title());
        buffer.writeUtf(node.description());
        buffer.writeUtf(node.status());
        buffer.writeVarInt(node.objectives().size());
        for (ChronicleEngineQuestService.JournalObjective objective : node.objectives()) {
            buffer.writeUtf(objective.text());
            buffer.writeVarInt(objective.progress());
            buffer.writeVarInt(objective.required());
            buffer.writeUtf(objective.type());
        }
    }

    private static ChronicleEngineQuestService.JournalNode readJournalNode(FriendlyByteBuf buffer) {
        String phaseId = buffer.readUtf();
        String title = buffer.readUtf();
        String description = buffer.readUtf();
        String status = buffer.readUtf();
        int size = buffer.readVarInt();
        List<ChronicleEngineQuestService.JournalObjective> objectives = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            objectives.add(new ChronicleEngineQuestService.JournalObjective(buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf()));
        }
        return new ChronicleEngineQuestService.JournalNode(phaseId, title, description, status, objectives);
    }

    private static void writeMarker(FriendlyByteBuf buffer, MarkerLine marker) {
        buffer.writeUtf(marker.label());
        buffer.writeUtf(marker.dimension());
        buffer.writeVarInt(marker.x());
        buffer.writeVarInt(marker.y());
        buffer.writeVarInt(marker.z());
    }

    private static MarkerLine readMarker(FriendlyByteBuf buffer) {
        return new MarkerLine(buffer.readUtf(), buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    private static void writeTrackerLine(FriendlyByteBuf buffer, TrackerLine line) {
        buffer.writeUtf(line.text());
        buffer.writeVarInt(line.progress());
        buffer.writeVarInt(line.required());
        buffer.writeUtf(line.type());
    }

    private static TrackerLine readTrackerLine(FriendlyByteBuf buffer) {
        return new TrackerLine(buffer.readUtf(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf());
    }

    public record ChoiceLine(String choiceId, Component text) {
    }

    public record ShopItemLine(String itemId, int count, String enchantmentId, int enchantmentLevel, String nbt) {
    }

    public record ShopEntryLine(String entryId, String category, Component name, String costs, String rewards, List<ShopItemLine> costItems, List<ShopItemLine> rewardItems) {
    }

    public record ShopCategoryLine(String categoryId, Component name) {
    }

    public record WalletLine(String currencyId, String itemId, Component name, long amount) {
    }

    public record TeamMemberLine(String playerId, String name, boolean online) {
    }

    public record TeamRequestLine(String teamId, String teamName, String playerId, String playerName) {
    }

    public record TeamLine(String teamId, String name, String ownerName, String ownerId, boolean member, boolean owner, boolean admin, boolean canManage, boolean friendlyFire, boolean pendingForMe, boolean blacklisted, List<TeamMemberLine> members, List<TeamMemberLine> admins, List<TeamMemberLine> blacklist, List<TeamRequestLine> requests) {
    }

    public record MarkerLine(String label, String dimension, int x, int y, int z) {
    }

    public record TrackerLine(String text, int progress, int required, String type) {
    }

    public record TrackerQuest(boolean visible, String questTitle, String phaseTitle, List<TrackerLine> objectives) {
        public static TrackerQuest hidden() {
            return new TrackerQuest(false, "", "", List.of());
        }
    }

    public record OpenTeamsPacket(List<TeamLine> teams, List<TeamMemberLine> onlinePlayers, List<TeamRequestLine> notifications, boolean serverAdmin, int ownedTeams, int maxOwnedTeams) {
        public static void encode(OpenTeamsPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.teams.size());
            for (TeamLine team : packet.teams) {
                writeTeamLine(buffer, team);
            }
            writeTeamMembers(buffer, packet.onlinePlayers);
            writeTeamRequests(buffer, packet.notifications);
            buffer.writeBoolean(packet.serverAdmin);
            buffer.writeVarInt(packet.ownedTeams);
            buffer.writeVarInt(packet.maxOwnedTeams);
        }

        public static OpenTeamsPacket decode(FriendlyByteBuf buffer) {
            int teamSize = buffer.readVarInt();
            List<TeamLine> teams = new ArrayList<>();
            for (int i = 0; i < teamSize; i++) {
                teams.add(readTeamLine(buffer));
            }
            return new OpenTeamsPacket(
                    teams,
                    readTeamMembers(buffer),
                    readTeamRequests(buffer),
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readVarInt()
            );
        }

        public static void handle(OpenTeamsPacket packet, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ChronicleEngineClient.openTeams(packet)));
            context.get().setPacketHandled(true);
        }
    }

    public record OpenDialoguePacket(String dialogueId, String nodeId, Component npcName, Component text, List<ChoiceLine> choices) {
        public static void encode(OpenDialoguePacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.dialogueId);
            buffer.writeUtf(packet.nodeId);
            buffer.writeComponent(packet.npcName);
            buffer.writeComponent(packet.text);
            buffer.writeVarInt(packet.choices.size());
            for (ChoiceLine choice : packet.choices) {
                writeChoice(buffer, choice);
            }
        }

        public static OpenDialoguePacket decode(FriendlyByteBuf buffer) {
            String dialogueId = buffer.readUtf();
            String nodeId = buffer.readUtf();
            Component npcName = buffer.readComponent();
            Component text = buffer.readComponent();
            int size = buffer.readVarInt();
            List<ChoiceLine> choices = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                choices.add(readChoice(buffer));
            }
            return new OpenDialoguePacket(dialogueId, nodeId, npcName, text, choices);
        }

        public static void handle(OpenDialoguePacket packet, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ChronicleEngineClient.openDialogue(packet)));
            context.get().setPacketHandled(true);
        }
    }

    public record ChooseDialoguePacket(String dialogueId, String nodeId, String choiceId) {
        public static void encode(ChooseDialoguePacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.dialogueId);
            buffer.writeUtf(packet.nodeId);
            buffer.writeUtf(packet.choiceId);
        }

        public static ChooseDialoguePacket decode(FriendlyByteBuf buffer) {
            return new ChooseDialoguePacket(buffer.readUtf(), buffer.readUtf(), buffer.readUtf());
        }

        public static void handle(ChooseDialoguePacket packet, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            context.get().enqueueWork(() -> {
                if (player != null) {
                    ChronicleEngineDialogueService.choose(player, packet.dialogueId, packet.nodeId, packet.choiceId);
                }
            });
            context.get().setPacketHandled(true);
        }
    }

    public record OpenShopPacket(String shopId, Component title, List<ShopCategoryLine> categories, List<ShopEntryLine> entries, List<WalletLine> wallet) {
        public static void encode(OpenShopPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.shopId);
            buffer.writeComponent(packet.title);
            buffer.writeVarInt(packet.categories.size());
            for (ShopCategoryLine category : packet.categories) {
                writeShopCategory(buffer, category);
            }
            buffer.writeVarInt(packet.entries.size());
            for (ShopEntryLine entry : packet.entries) {
                writeShopEntry(buffer, entry);
            }
            buffer.writeVarInt(packet.wallet.size());
            for (WalletLine line : packet.wallet) {
                writeWalletLine(buffer, line);
            }
        }

        public static OpenShopPacket decode(FriendlyByteBuf buffer) {
            String shopId = buffer.readUtf();
            Component title = buffer.readComponent();
            int categorySize = buffer.readVarInt();
            List<ShopCategoryLine> categories = new ArrayList<>();
            for (int i = 0; i < categorySize; i++) {
                categories.add(readShopCategory(buffer));
            }
            int size = buffer.readVarInt();
            List<ShopEntryLine> entries = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                entries.add(readShopEntry(buffer));
            }
            int walletSize = buffer.readVarInt();
            List<WalletLine> wallet = new ArrayList<>();
            for (int i = 0; i < walletSize; i++) {
                wallet.add(readWalletLine(buffer));
            }
            return new OpenShopPacket(shopId, title, categories, entries, wallet);
        }

        public static void handle(OpenShopPacket packet, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ChronicleEngineClient.openShop(packet)));
            context.get().setPacketHandled(true);
        }
    }

    public record BuyShopPacket(String shopId, String entryId) {
        public static void encode(BuyShopPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.shopId);
            buffer.writeUtf(packet.entryId);
        }

        public static BuyShopPacket decode(FriendlyByteBuf buffer) {
            return new BuyShopPacket(buffer.readUtf(), buffer.readUtf());
        }

        public static void handle(BuyShopPacket packet, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            context.get().enqueueWork(() -> {
                if (player != null) {
                    ChronicleEngineShopService.buy(player, packet.shopId, packet.entryId);
                }
            });
            context.get().setPacketHandled(true);
        }
    }

    public record OpenJournalPacket(List<ChronicleEngineQuestService.JournalQuest> quests) {
        public static void encode(OpenJournalPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.quests.size());
            for (ChronicleEngineQuestService.JournalQuest quest : packet.quests) {
                writeJournalQuest(buffer, quest);
            }
        }

        public static OpenJournalPacket decode(FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<ChronicleEngineQuestService.JournalQuest> quests = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                quests.add(readJournalQuest(buffer));
            }
            return new OpenJournalPacket(quests);
        }

        public static void handle(OpenJournalPacket packet, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ChronicleEngineClient.openJournal(packet)));
            context.get().setPacketHandled(true);
        }
    }

    public record OpenJournalRequestPacket() {
        public static void encode(OpenJournalRequestPacket packet, FriendlyByteBuf buffer) {
        }

        public static OpenJournalRequestPacket decode(FriendlyByteBuf buffer) {
            return new OpenJournalRequestPacket();
        }

        public static void handle(OpenJournalRequestPacket packet, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            context.get().enqueueWork(() -> {
                if (player != null) {
                    ChronicleEngineNetwork.openJournal(player, new OpenJournalPacket(ChronicleEngineQuestService.journal(player)));
                }
            });
            context.get().setPacketHandled(true);
        }
    }

    public record OpenWalletPacket(List<WalletLine> wallet) {
        public static void encode(OpenWalletPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.wallet.size());
            for (WalletLine line : packet.wallet) {
                writeWalletLine(buffer, line);
            }
        }

        public static OpenWalletPacket decode(FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<WalletLine> wallet = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                wallet.add(readWalletLine(buffer));
            }
            return new OpenWalletPacket(wallet);
        }

        public static void handle(OpenWalletPacket packet, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ChronicleEngineClient.openWallet(packet)));
            context.get().setPacketHandled(true);
        }
    }

    public record OpenWalletRequestPacket() {
        public static void encode(OpenWalletRequestPacket packet, FriendlyByteBuf buffer) {
        }

        public static OpenWalletRequestPacket decode(FriendlyByteBuf buffer) {
            return new OpenWalletRequestPacket();
        }

        public static void handle(OpenWalletRequestPacket packet, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            context.get().enqueueWork(() -> {
                if (player != null) {
                    ChronicleEngineWalletService.openWallet(player);
                }
            });
            context.get().setPacketHandled(true);
        }
    }

    public record WithdrawWalletPacket(String itemId, long amount) {
        public static void encode(WithdrawWalletPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.itemId);
            buffer.writeVarLong(packet.amount);
        }

        public static WithdrawWalletPacket decode(FriendlyByteBuf buffer) {
            return new WithdrawWalletPacket(buffer.readUtf(), buffer.readVarLong());
        }

        public static void handle(WithdrawWalletPacket packet, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            context.get().enqueueWork(() -> {
                if (player != null) {
                    ChronicleEngineWalletService.withdraw(player, packet.itemId, packet.amount);
                }
            });
            context.get().setPacketHandled(true);
        }
    }

    public record OpenTeamsRequestPacket() {
        public static void encode(OpenTeamsRequestPacket packet, FriendlyByteBuf buffer) {
        }

        public static OpenTeamsRequestPacket decode(FriendlyByteBuf buffer) {
            return new OpenTeamsRequestPacket();
        }

        public static void handle(OpenTeamsRequestPacket packet, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            context.get().enqueueWork(() -> {
                if (player != null) {
                    ChronicleEngineTeamService.open(player);
                }
            });
            context.get().setPacketHandled(true);
        }
    }

    public record TeamActionPacket(String action, String teamId, String targetId) {
        public static void encode(TeamActionPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.action);
            buffer.writeUtf(packet.teamId);
            buffer.writeUtf(packet.targetId);
        }

        public static TeamActionPacket decode(FriendlyByteBuf buffer) {
            return new TeamActionPacket(buffer.readUtf(), buffer.readUtf(), buffer.readUtf());
        }

        public static void handle(TeamActionPacket packet, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            context.get().enqueueWork(() -> {
                if (player != null) {
                    ChronicleEngineTeamService.handleAction(player, packet.action, packet.teamId, packet.targetId);
                }
            });
            context.get().setPacketHandled(true);
        }
    }

    public record OfferQuestPacket(String questId) {
        public static void encode(OfferQuestPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.questId);
        }

        public static OfferQuestPacket decode(FriendlyByteBuf buffer) {
            return new OfferQuestPacket(buffer.readUtf());
        }

        public static void handle(OfferQuestPacket packet, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            context.get().enqueueWork(() -> {
                if (player != null) {
                    ChronicleEngineQuestService.offer(player, packet.questId);
                    ChronicleEngineNetwork.openJournal(player, new OpenJournalPacket(ChronicleEngineQuestService.journal(player)));
                }
            });
            context.get().setPacketHandled(true);
        }
    }

    public record SyncMarkersPacket(List<MarkerLine> markers) {
        public static void encode(SyncMarkersPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.markers.size());
            for (MarkerLine marker : packet.markers) {
                writeMarker(buffer, marker);
            }
        }

        public static SyncMarkersPacket decode(FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<MarkerLine> markers = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                markers.add(readMarker(buffer));
            }
            return new SyncMarkersPacket(markers);
        }

        public static void handle(SyncMarkersPacket packet, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ChronicleEngineClient.setMarkers(packet.markers)));
            context.get().setPacketHandled(true);
        }
    }

    public record SyncTrackerPacket(List<TrackerQuest> trackers) {
        public static void encode(SyncTrackerPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.trackers.size());
            for (TrackerQuest tracker : packet.trackers) {
                buffer.writeBoolean(tracker.visible());
                buffer.writeUtf(tracker.questTitle());
                buffer.writeUtf(tracker.phaseTitle());
                buffer.writeVarInt(tracker.objectives().size());
                for (TrackerLine line : tracker.objectives()) {
                    writeTrackerLine(buffer, line);
                }
            }
        }

        public static SyncTrackerPacket decode(FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            List<TrackerQuest> trackers = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                boolean visible = buffer.readBoolean();
                String questTitle = buffer.readUtf();
                String phaseTitle = buffer.readUtf();
                int objectiveSize = buffer.readVarInt();
                List<TrackerLine> objectives = new ArrayList<>();
                for (int objectiveIndex = 0; objectiveIndex < objectiveSize; objectiveIndex++) {
                    objectives.add(readTrackerLine(buffer));
                }
                trackers.add(new TrackerQuest(visible, questTitle, phaseTitle, objectives));
            }
            return new SyncTrackerPacket(trackers);
        }

        public static void handle(SyncTrackerPacket packet, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ChronicleEngineClient.setTrackers(packet.trackers)));
            context.get().setPacketHandled(true);
        }
    }

    public record ToggleTrackPacket(String questId) {
        public static void encode(ToggleTrackPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.questId);
        }

        public static ToggleTrackPacket decode(FriendlyByteBuf buffer) {
            return new ToggleTrackPacket(buffer.readUtf());
        }

        public static void handle(ToggleTrackPacket packet, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            context.get().enqueueWork(() -> {
                if (player != null) {
                    ChronicleEngineQuestService.toggleTracking(player, packet.questId);
                    ChronicleEngineNetwork.openJournal(player, new OpenJournalPacket(ChronicleEngineQuestService.journal(player)));
                }
            });
            context.get().setPacketHandled(true);
        }
    }

    public record CloseScreenPacket() {
        public static void encode(CloseScreenPacket packet, FriendlyByteBuf buffer) {
        }

        public static CloseScreenPacket decode(FriendlyByteBuf buffer) {
            return new CloseScreenPacket();
        }

        public static void handle(CloseScreenPacket packet, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ChronicleEngineClient::closeScreen));
            context.get().setPacketHandled(true);
        }
    }

    public record EndDialoguePacket() {
        public static void encode(EndDialoguePacket packet, FriendlyByteBuf buffer) {
        }

        public static EndDialoguePacket decode(FriendlyByteBuf buffer) {
            return new EndDialoguePacket();
        }

        public static void handle(EndDialoguePacket packet, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            context.get().enqueueWork(() -> {
                if (player != null) {
                    ChronicleEngineNpcFocusService.release(player);
                }
            });
            context.get().setPacketHandled(true);
        }
    }
}

