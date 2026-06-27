package com.chronicle.engine;

import com.chronicle.engine.network.ChronicleEngineNetwork;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public final class ChronicleEngineCommands {
    private ChronicleEngineCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(root("chronicle"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root(String commandName) {
        return Commands.literal(commandName)
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            context.getSource().sendSuccess(() -> Component.translatable("command.chronicle_engine.reload"), true);
                            return 1;
                        }))
                .then(Commands.literal("dialogue")
                        .then(Commands.literal("start")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("dialogue_id", ResourceLocationArgument.id())
                                                .executes(context -> {
                                                    ResourceLocation id = ResourceLocationArgument.getId(context, "dialogue_id");
                                                    return forPlayers(EntityArgument.getPlayers(context, "targets"), player -> {
                                                        ChronicleEngineDialogueService.open(player, id.toString());
                                                        return true;
                                                    });
                                                })))))
                .then(Commands.literal("quest")
                        .then(Commands.literal("give")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("quest_id", ResourceLocationArgument.id())
                                                .executes(context -> {
                                                    ResourceLocation id = ResourceLocationArgument.getId(context, "quest_id");
                                                    return forPlayers(EntityArgument.getPlayers(context, "targets"), player -> ChronicleEngineQuestService.accept(player, id.toString()));
                                                }))))
                        .then(Commands.literal("complete")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("quest_id", ResourceLocationArgument.id())
                                                .executes(context -> {
                                                    ResourceLocation id = ResourceLocationArgument.getId(context, "quest_id");
                                                    return forPlayers(EntityArgument.getPlayers(context, "targets"), player -> {
                                                        ChronicleEngineQuestService.complete(player, id.toString());
                                                        return true;
                                                    });
                                                }))))
                        .then(Commands.literal("reset")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("quest_id", ResourceLocationArgument.id())
                                                .executes(context -> {
                                                    ResourceLocation id = ResourceLocationArgument.getId(context, "quest_id");
                                                    return forPlayers(EntityArgument.getPlayers(context, "targets"), player -> {
                                                        ChronicleEngineQuestService.reset(player, id.toString());
                                                        return true;
                                                    });
                                                }))))
                        .then(Commands.literal("reset_all")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ChronicleEngineQuestService.resetAll(player);
                                    return 1;
                                })
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .requires(source -> source.hasPermission(2))
                                        .executes(context -> forPlayers(EntityArgument.getPlayers(context, "targets"), player -> {
                                            ChronicleEngineQuestService.resetAll(player);
                                            return true;
                                        }))))
                        .then(Commands.literal("offer")
                                .then(Commands.argument("quest_id", ResourceLocationArgument.id())
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            ResourceLocation id = ResourceLocationArgument.getId(context, "quest_id");
                                            return ChronicleEngineQuestService.offer(player, id.toString()) ? 1 : 0;
                                        })))
                        .then(Commands.literal("journal")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ChronicleEngineNetwork.openJournal(player, new ChronicleEngineNetwork.OpenJournalPacket(ChronicleEngineQuestService.journal(player)));
                                    return 1;
                                })))
                .then(Commands.literal("shop")
                        .then(Commands.literal("open")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("shop_id", ResourceLocationArgument.id())
                                                .executes(context -> {
                                                    ResourceLocation id = ResourceLocationArgument.getId(context, "shop_id");
                                                    return forPlayers(EntityArgument.getPlayers(context, "targets"), player -> {
                                                        ChronicleEngineShopService.open(player, id.toString());
                                                        return true;
                                                    });
                                                })))));
    }

    private static int forPlayers(Collection<ServerPlayer> players, PlayerAction action) {
        int count = 0;
        for (ServerPlayer player : players) {
            if (action.run(player)) {
                count++;
            }
        }
        return count;
    }

    @FunctionalInterface
    private interface PlayerAction {
        boolean run(ServerPlayer player);
    }
}


