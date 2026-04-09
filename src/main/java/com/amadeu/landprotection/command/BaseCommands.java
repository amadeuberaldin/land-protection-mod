package com.amadeu.landprotection.command;

import com.amadeu.landprotection.claim.BaseClaim;
import com.amadeu.landprotection.claim.ClaimManager;
import com.amadeu.landprotection.visual.BaseVisualizationManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
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

    private static void registerClaimBaseWithCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("claimbasewith")
                        .then(CommandManager.argument("jogador", StringArgumentType.word())
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();

                                    if (source.getPlayer() == null) {
                                        source.sendError(Text.literal("Este comando só pode ser usado por jogadores."));
                                        return 0;
                                    }

                                    ServerPlayerEntity leader = source.getPlayer();
                                    String targetName = StringArgumentType.getString(context, "jogador");

                                    if (ClaimManager.playerHasBase(leader.getUuid())) {
                                        leader.sendMessage(Text.literal("Você já participa de uma base."), false);
                                        return 0;
                                    }
                                
                                    ResolvedPlayer resolved = resolvePlayer(source.getServer(), targetName);
                                    if (resolved == null) {
                                        leader.sendMessage(Text.literal("Jogador não encontrado ou offline."), false);
                                        return 0;
                                    }

                                    if (resolved.uuid().equals(leader.getUuid())) {
                                        leader.sendMessage(Text.literal(
                                                "Use /claimbasewith com outro jogador para formar uma base em grupo."),
                                                false);
                                        return 0;
                                    }

                                    if (ClaimManager.playerHasBase(resolved.uuid())) {
                                        leader.sendMessage(
                                                Text.literal(
                                                        "O jogador informado já participa de outra área protegida."),
                                                false);
                                        return 0;
                                    }

                                    BlockPos center = leader.getBlockPos();
                                    BlockPos pos1 = center.add(-BASE_RADIUS_XZ, -BASE_RADIUS_Y_DOWN, -BASE_RADIUS_XZ);
                                    BlockPos pos2 = center.add(BASE_RADIUS_XZ, BASE_RADIUS_Y_UP, BASE_RADIUS_XZ);

                                    if (ClaimManager.overlapsExistingArea(pos1, pos2)) {
                                        leader.sendMessage(Text.literal(
                                                "Não é possível criar a base aqui porque ela sobrepõe outra área protegida."),
                                                false);
                                        return 0;
                                    }

                                    BaseClaim base = new BaseClaim(
                                            leader.getUuid(),
                                            leader.getName().getString(),
                                            pos1,
                                            pos2,
                                            center);

                                    boolean added = base.addMember(resolved.uuid(), resolved.name(),
                                            ClaimManager.MAX_BASE_MEMBERS);
                                    if (!added) {
                                        leader.sendMessage(Text.literal("Não foi possível adicionar o jogador à base."),
                                                false);
                                        return 0;
                                    }

                                    ClaimManager.addBase(base);
                                    leader.sendMessage(
                                            Text.literal("Base criada com sucesso com " + resolved.name() + "."),
                                            false);

                                    ServerPlayerEntity onlineTarget = source.getServer().getPlayerManager()
                                            .getPlayer(resolved.uuid());
                                    if (onlineTarget != null) {
                                        onlineTarget.sendMessage(
                                                Text.literal("Você foi adicionado à base de "
                                                        + leader.getName().getString() + "."),
                                                false);
                                    }

                                    return 1;
                                })));
    }

    private static void registerBaseAddCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("baseadd")
                        .then(CommandManager.argument("jogador", StringArgumentType.word())
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();

                                    if (source.getPlayer() == null) {
                                        source.sendError(Text.literal("Este comando só pode ser usado por jogadores."));
                                        return 0;
                                    }

                                    ServerPlayerEntity leader = source.getPlayer();
                                    String targetName = StringArgumentType.getString(context, "jogador");

                                    BaseClaim base = ClaimManager.getBaseByLeader(leader.getUuid());
                                    if (base == null) {
                                        leader.sendMessage(Text.literal("Você não é líder de nenhuma base."), false);
                                        return 0;
                                    }

                                    ResolvedPlayer resolved = resolvePlayer(source.getServer(), targetName);
                                    if (resolved == null) {
                                        leader.sendMessage(Text.literal("Jogador não encontrado ou offline."), false);
                                        return 0;
                                    }

                                    if (ClaimManager.playerHasBase(resolved.uuid())
                                            || ClaimManager.playerHasClaim(resolved.uuid())) {
                                        leader.sendMessage(
                                                Text.literal(
                                                        "O jogador informado já participa de outra área protegida."),
                                                false);
                                        return 0;
                                    }

                                    boolean added = base.addMember(resolved.uuid(), resolved.name(),
                                            ClaimManager.MAX_BASE_MEMBERS);
                                    if (!added) {
                                        leader.sendMessage(Text.literal(
                                                "Não foi possível adicionar o jogador. A base pode estar cheia ou ele já faz parte dela."),
                                                false);
                                        return 0;
                                    }

                                    leader.sendMessage(
                                            Text.literal("Jogador " + resolved.name() + " adicionado à base."), false);

                                    ServerPlayerEntity onlineTarget = source.getServer().getPlayerManager()
                                            .getPlayer(resolved.uuid());
                                    if (onlineTarget != null) {
                                        onlineTarget.sendMessage(
                                                Text.literal("Você foi adicionado à base de "
                                                        + leader.getName().getString() + "."),
                                                false);
                                    }

                                    return 1;
                                })));
    }

    private static void registerBaseListCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("baselist")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();

                            if (source.getPlayer() == null) {
                                source.sendError(Text.literal("Este comando só pode ser usado por jogadores."));
                                return 0;
                            }

                            ServerPlayerEntity player = source.getPlayer();
                            BaseClaim base = ClaimManager.getBaseByMember(player.getUuid());

                            if (base == null) {
                                player.sendMessage(Text.literal("Você não participa de nenhuma base."), false);
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

                            player.sendMessage(Text.literal("Membros da base: " + list), false);
                            return 1;
                        }));
    }

    private static void registerBaseBanCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("baseban")
                        .then(CommandManager.argument("jogador", StringArgumentType.word())
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();

                                    if (source.getPlayer() == null) {
                                        source.sendError(Text.literal("Este comando só pode ser usado por jogadores."));
                                        return 0;
                                    }

                                    ServerPlayerEntity leader = source.getPlayer();
                                    String targetName = StringArgumentType.getString(context, "jogador");

                                    BaseClaim base = ClaimManager.getBaseByLeader(leader.getUuid());
                                    if (base == null) {
                                        leader.sendMessage(Text.literal("Você não é líder de nenhuma base."), false);
                                        return 0;
                                    }

                                    ResolvedPlayer resolved = resolvePlayer(source.getServer(), targetName);
                                    if (resolved == null) {
                                        leader.sendMessage(Text.literal("Jogador não encontrado ou offline."), false);
                                        return 0;
                                    }

                                    if (resolved.uuid().equals(leader.getUuid())) {
                                        leader.sendMessage(
                                                Text.literal("O líder não pode ser removido da própria base."), false);
                                        return 0;
                                    }

                                    boolean removed = base.removeMember(resolved.uuid());
                                    if (!removed) {
                                        leader.sendMessage(
                                                Text.literal("O jogador informado não faz parte da sua base."), false);
                                        return 0;
                                    }

                                    leader.sendMessage(
                                            Text.literal("Jogador " + resolved.name() + " removido da base."), false);

                                    ServerPlayerEntity onlineTarget = source.getServer().getPlayerManager()
                                            .getPlayer(resolved.uuid());
                                    if (onlineTarget != null) {
                                        onlineTarget.sendMessage(
                                                Text.literal("Você foi removido da base de "
                                                        + leader.getName().getString() + "."),
                                                false);
                                    }

                                    // Se sobrou apenas o líder, a base é removida automaticamente
                                    if (base.getMembers().size() <= 1) {
                                        ClaimManager.removeBase(base);
                                        leader.sendMessage(
                                                Text.literal(
                                                        "Sua base foi desfeita automaticamente porque não restaram membros além do líder."),
                                                false);
                                    }

                                    return 1;
                                })));
    }

    private static void registerBaseShowCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("baseshow")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();

                            if (source.getPlayer() == null) {
                                source.sendError(Text.literal("Este comando só pode ser usado por jogadores."));
                                return 0;
                            }

                            ServerPlayerEntity player = source.getPlayer();
                            BaseClaim base = ClaimManager.getBaseByMember(player.getUuid());

                            if (base == null) {
                                player.sendMessage(Text.literal("Você não participa de nenhuma base."), false);
                                return 0;
                            }

                            BaseVisualizationManager.show(player.getUuid());
                            player.sendMessage(Text.literal("Visualização da base ativada."), false);
                            return 1;
                        }));
    }

    private static void registerBaseHideCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("basehide")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();

                            if (source.getPlayer() == null) {
                                source.sendError(Text.literal("Este comando só pode ser usado por jogadores."));
                                return 0;
                            }

                            ServerPlayerEntity player = source.getPlayer();

                            if (!BaseVisualizationManager.isShowing(player.getUuid())) {
                                player.sendMessage(Text.literal("A visualização da base já está desativada."), false);
                                return 0;
                            }

                            BaseVisualizationManager.hide(player.getUuid());
                            player.sendMessage(Text.literal("Visualização da base desativada."), false);
                            return 1;
                        }));
    }

    private static ResolvedPlayer resolvePlayer(MinecraftServer server, String playerName) {
        ServerPlayerEntity onlinePlayer = server.getPlayerManager().getPlayer(playerName);

        if (onlinePlayer != null) {
            return new ResolvedPlayer(
                    onlinePlayer.getUuid(),
                    onlinePlayer.getName().getString());
        }

        return null;
    }

    private record ResolvedPlayer(UUID uuid, String name) {
    }
}