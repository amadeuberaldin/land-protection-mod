package com.amadeu.landprotection.command;

import com.amadeu.landprotection.claim.BaseClaim;
import com.amadeu.landprotection.claim.ClaimManager;
import com.amadeu.landprotection.visual.BaseVisualizationManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class BaseCommands {

    private static final int BASE_RADIUS_XZ = 50;
    private static final int BASE_RADIUS_Y_DOWN = 50;
    private static final int BASE_RADIUS_Y_UP = 50;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerClaimBaseWithCommand(dispatcher);
            registerBaseAddCommand(dispatcher);
            registerBaseListCommand(dispatcher);
            registerBaseBanCommand(dispatcher);
            registerBaseShowCommand(dispatcher);
            registerBaseHideCommand(dispatcher);
        });
    }

    private static void registerClaimBaseWithCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("claimbasewith")
                        .then(Commands.argument("jogador", StringArgumentType.word())
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();

                                    ServerPlayer leader;
                                    try {
                                        leader = source.getPlayerOrException();
                                    } catch (Exception e) {
                                        source.sendFailure(Component.literal("Este comando só pode ser usado por jogadores."));
                                        return 0;
                                    }

                                    String targetName = StringArgumentType.getString(context, "jogador");

                                    if (ClaimManager.playerHasBase(leader.getUUID())) {
                                        leader.sendSystemMessage(Component.literal("Você já participa de uma base."));
                                        return 0;
                                    }

                                    ResolvedPlayer resolved = resolvePlayer(source.getServer(), targetName);
                                    if (resolved == null) {
                                        leader.sendSystemMessage(Component.literal("Jogador não encontrado ou offline."));
                                        return 0;
                                    }

                                    if (resolved.uuid().equals(leader.getUUID())) {
                                        leader.sendSystemMessage(Component.literal(
                                                "Use /claimbasewith com outro jogador para formar uma base em grupo."));
                                        return 0;
                                    }

                                    if (ClaimManager.playerHasBase(resolved.uuid())
                                            || ClaimManager.playerHasClaim(resolved.uuid())) {
                                        leader.sendSystemMessage(Component.literal(
                                                "O jogador informado já participa de outra área protegida."));
                                        return 0;
                                    }

                                    BlockPos center = leader.blockPosition();
                                    BlockPos pos1 = center.offset(-BASE_RADIUS_XZ, -BASE_RADIUS_Y_DOWN, -BASE_RADIUS_XZ);
                                    BlockPos pos2 = center.offset(BASE_RADIUS_XZ, BASE_RADIUS_Y_UP, BASE_RADIUS_XZ);

                                    if (ClaimManager.overlapsExistingArea(pos1, pos2)) {
                                        leader.sendSystemMessage(Component.literal(
                                                "Não é possível criar a base aqui porque ela sobrepõe outra área protegida."));
                                        return 0;
                                    }

                                    BaseClaim base = new BaseClaim(
                                            leader.getUUID(),
                                            leader.getName().getString(),
                                            pos1,
                                            pos2,
                                            center
                                    );

                                    boolean added = base.addMember(
                                            resolved.uuid(),
                                            resolved.name(),
                                            ClaimManager.MAX_BASE_MEMBERS
                                    );

                                    if (!added) {
                                        leader.sendSystemMessage(Component.literal("Não foi possível adicionar o jogador à base."));
                                        return 0;
                                    }

                                    ClaimManager.addBase(base);

                                    leader.sendSystemMessage(Component.literal(
                                            "Base criada com sucesso com " + resolved.name() + "."
                                    ));

                                    ServerPlayer onlineTarget = source.getServer().getPlayerList().getPlayerByName(resolved.name());
                                    if (onlineTarget != null) {
                                        onlineTarget.sendSystemMessage(Component.literal(
                                                "Você foi adicionado à base de " + leader.getName().getString() + "."
                                        ));
                                    }

                                    return 1;
                                })));
    }

    private static void registerBaseAddCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("baseadd")
                        .then(Commands.argument("jogador", StringArgumentType.word())
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();

                                    ServerPlayer leader;
                                    try {
                                        leader = source.getPlayerOrException();
                                    } catch (Exception e) {
                                        source.sendFailure(Component.literal("Este comando só pode ser usado por jogadores."));
                                        return 0;
                                    }

                                    String targetName = StringArgumentType.getString(context, "jogador");
                                    BaseClaim base = ClaimManager.getBaseByLeader(leader.getUUID());

                                    if (base == null) {
                                        leader.sendSystemMessage(Component.literal("Você não é líder de nenhuma base."));
                                        return 0;
                                    }

                                    ResolvedPlayer resolved = resolvePlayer(source.getServer(), targetName);
                                    if (resolved == null) {
                                        leader.sendSystemMessage(Component.literal("Jogador não encontrado ou offline."));
                                        return 0;
                                    }

                                    if (ClaimManager.playerHasBase(resolved.uuid())
                                            || ClaimManager.playerHasClaim(resolved.uuid())) {
                                        leader.sendSystemMessage(Component.literal(
                                                "O jogador informado já participa de outra área protegida."));
                                        return 0;
                                    }

                                    boolean added = base.addMember(
                                            resolved.uuid(),
                                            resolved.name(),
                                            ClaimManager.MAX_BASE_MEMBERS
                                    );

                                    if (!added) {
                                        leader.sendSystemMessage(Component.literal(
                                                "Não foi possível adicionar o jogador. A base pode estar cheia ou ele já faz parte dela."
                                        ));
                                        return 0;
                                    }

                                    leader.sendSystemMessage(Component.literal(
                                            "Jogador " + resolved.name() + " adicionado à base."
                                    ));

                                    ServerPlayer onlineTarget = source.getServer().getPlayerList().getPlayerByName(resolved.name());
                                    if (onlineTarget != null) {
                                        onlineTarget.sendSystemMessage(Component.literal(
                                                "Você foi adicionado à base de " + leader.getName().getString() + "."
                                        ));
                                    }

                                    return 1;
                                })));
    }

    private static void registerBaseListCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("baselist")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();

                            ServerPlayer player;
                            try {
                                player = source.getPlayerOrException();
                            } catch (Exception e) {
                                source.sendFailure(Component.literal("Este comando só pode ser usado por jogadores."));
                                return 0;
                            }

                            BaseClaim base = ClaimManager.getBaseByMember(player.getUUID());

                            if (base == null) {
                                player.sendSystemMessage(Component.literal("Você não participa de nenhuma base."));
                                return 0;
                            }

                            StringBuilder list = new StringBuilder();
                            boolean first = true;

                            for (String name : base.getMembers().values()) {
                                if (!first) {
                                    list.append(", ");
                                }
                                list.append(name);
                                first = false;
                            }

                            player.sendSystemMessage(Component.literal("Membros da base: " + list));
                            return 1;
                        })
        );
    }

    private static void registerBaseBanCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("baseban")
                        .then(Commands.argument("jogador", StringArgumentType.word())
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();

                                    ServerPlayer leader;
                                    try {
                                        leader = source.getPlayerOrException();
                                    } catch (Exception e) {
                                        source.sendFailure(Component.literal("Este comando só pode ser usado por jogadores."));
                                        return 0;
                                    }

                                    String targetName = StringArgumentType.getString(context, "jogador");
                                    BaseClaim base = ClaimManager.getBaseByLeader(leader.getUUID());

                                    if (base == null) {
                                        leader.sendSystemMessage(Component.literal("Você não é líder de nenhuma base."));
                                        return 0;
                                    }

                                    ResolvedPlayer resolved = resolvePlayer(source.getServer(), targetName);
                                    if (resolved == null) {
                                        leader.sendSystemMessage(Component.literal("Jogador não encontrado ou offline."));
                                        return 0;
                                    }

                                    if (resolved.uuid().equals(leader.getUUID())) {
                                        leader.sendSystemMessage(Component.literal(
                                                "O líder não pode ser removido da própria base."
                                        ));
                                        return 0;
                                    }

                                    boolean removed = base.removeMember(resolved.uuid());
                                    if (!removed) {
                                        leader.sendSystemMessage(Component.literal(
                                                "O jogador informado não faz parte da sua base."
                                        ));
                                        return 0;
                                    }

                                    leader.sendSystemMessage(Component.literal(
                                            "Jogador " + resolved.name() + " removido da base."
                                    ));

                                    ServerPlayer onlineTarget = source.getServer().getPlayerList().getPlayerByName(resolved.name());
                                    if (onlineTarget != null) {
                                        onlineTarget.sendSystemMessage(Component.literal(
                                                "Você foi removido da base de " + leader.getName().getString() + "."
                                        ));
                                    }

                                    if (base.getMembers().size() <= 1) {
                                        ClaimManager.removeBase(base);
                                        BaseVisualizationManager.hide(leader.getUUID());
                                        leader.sendSystemMessage(Component.literal(
                                                "Sua base foi desfeita automaticamente porque não restaram membros além do líder."
                                        ));
                                    }

                                    return 1;
                                })));
    }

    private static void registerBaseShowCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("baseshow")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();

                            ServerPlayer player;
                            try {
                                player = source.getPlayerOrException();
                            } catch (Exception e) {
                                source.sendFailure(Component.literal("Este comando só pode ser usado por jogadores."));
                                return 0;
                            }

                            BaseClaim base = ClaimManager.getBaseByMember(player.getUUID());

                            if (base == null) {
                                player.sendSystemMessage(Component.literal("Você não participa de nenhuma base."));
                                return 0;
                            }

                            BaseVisualizationManager.show(player.getUUID());
                            player.sendSystemMessage(Component.literal("Visualização da base ativada."));
                            return 1;
                        })
        );
    }

    private static void registerBaseHideCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("basehide")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();

                            ServerPlayer player;
                            try {
                                player = source.getPlayerOrException();
                            } catch (Exception e) {
                                source.sendFailure(Component.literal("Este comando só pode ser usado por jogadores."));
                                return 0;
                            }

                            if (!BaseVisualizationManager.isShowing(player.getUUID())) {
                                player.sendSystemMessage(Component.literal("A visualização da base já está desativada."));
                                return 0;
                            }

                            BaseVisualizationManager.hide(player.getUUID());
                            player.sendSystemMessage(Component.literal("Visualização da base desativada."));
                            return 1;
                        })
        );
    }

    private static ResolvedPlayer resolvePlayer(MinecraftServer server, String playerName) {
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayerByName(playerName);

        if (onlinePlayer != null) {
            return new ResolvedPlayer(
                    onlinePlayer.getUUID(),
                    onlinePlayer.getName().getString()
            );
        }

        return null;
    }

    private record ResolvedPlayer(UUID uuid, String name) {
    }
}
