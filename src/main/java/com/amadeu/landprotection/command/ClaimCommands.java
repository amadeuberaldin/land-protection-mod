package com.amadeu.landprotection.command;

import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import com.amadeu.landprotection.visual.ClaimVisualizationManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.UUID;

public class ClaimCommands {

    private static final int CLAIM_RADIUS_XZ = 10;
    private static final int CLAIM_RADIUS_Y_DOWN = 10;
    private static final int CLAIM_RADIUS_Y_UP = 10;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerClaimCommand(dispatcher);
            registerDisclaimCommand(dispatcher);
            registerClaimLocationCommand(dispatcher);
            registerTrustListCommand(dispatcher);
            registerTrustCommand(dispatcher);
            registerUntrustCommand(dispatcher);
            registerClaimShowCommand(dispatcher);
            registerClaimHideCommand(dispatcher);
        });
    }

    private static void registerClaimCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("claim")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();

                            ServerPlayer player;
                            try {
                                player = source.getPlayerOrException();
                            } catch (Exception e) {
                                source.sendFailure(Component.literal("Este comando só pode ser usado por jogadores."));
                                return 0;
                            }

                            BlockPos centerPos = player.blockPosition();
                            Level world = source.getLevel();

                            if (ClaimManager.playerHasClaim(player.getUUID())) {
                                player.sendSystemMessage(Component.literal(
                                        "Você já possui uma área protegida. Use /disclaim antes de criar outra."));
                                return 0;
                            }

                            if (isNearStructure(world, centerPos)) {
                                player.sendSystemMessage(Component.literal(
                                        "Você não pode proteger áreas em estruturas do jogo."));
                                return 0;
                            }

                            BlockPos pos1 = centerPos.offset(-CLAIM_RADIUS_XZ, -CLAIM_RADIUS_Y_DOWN, -CLAIM_RADIUS_XZ);
                            BlockPos pos2 = centerPos.offset(CLAIM_RADIUS_XZ, CLAIM_RADIUS_Y_UP, CLAIM_RADIUS_XZ);

                            if (ClaimManager.overlapsExistingArea(pos1, pos2)) {
                                player.sendSystemMessage(Component.literal(
                                        "Não é possível criar a claim aqui porque ela sobrepõe outra área protegida."));
                                return 0;
                            }

                            Claim claim = new Claim(player.getUUID(), pos1, pos2, centerPos);
                            ClaimManager.addClaim(claim);

                            player.sendSystemMessage(Component.literal(
                                    "Área protegida criada com sucesso no local atual."));
                            return 1;
                        })
        );
    }

    private static void registerDisclaimCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("disclaim")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();

                            ServerPlayer player;
                            try {
                                player = source.getPlayerOrException();
                            } catch (Exception e) {
                                source.sendFailure(Component.literal("Este comando só pode ser usado por jogadores."));
                                return 0;
                            }

                            BlockPos pos = player.blockPosition();
                            Claim claim = ClaimManager.getClaimAt(pos);

                            if (claim == null) {
                                player.sendSystemMessage(Component.literal(
                                        "Você não está dentro de nenhuma área protegida individual."));
                                return 0;
                            }

                            if (!claim.isOwner(player.getUUID())) {
                                player.sendSystemMessage(Component.literal(
                                        "Esta área protegida não pertence a você."));
                                return 0;
                            }

                            ClaimManager.removeClaim(claim);
                            ClaimVisualizationManager.hide(player.getUUID());

                            player.sendSystemMessage(Component.literal(
                                    "Sua área protegida foi removida com sucesso."));
                            return 1;
                        })
        );
    }

    private static void registerClaimLocationCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("claimlocation")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();

                            ServerPlayer player;
                            try {
                                player = source.getPlayerOrException();
                            } catch (Exception e) {
                                source.sendFailure(Component.literal("Este comando só pode ser usado por jogadores."));
                                return 0;
                            }

                            Claim claim = ClaimManager.getClaimByPlayer(player.getUUID());

                            if (claim == null) {
                                player.sendSystemMessage(Component.literal("Você não possui uma área protegida."));
                                return 0;
                            }

                            BlockPos center = claim.getCenter();
                            player.sendSystemMessage(Component.literal(
                                    "Sua área protegida foi criada em X: "
                                            + center.getX()
                                            + " Y: "
                                            + center.getY()
                                            + " Z: "
                                            + center.getZ()
                                            + "."));
                            return 1;
                        })
        );
    }

    private static void registerTrustListCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("trustlist")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();

                            ServerPlayer player;
                            try {
                                player = source.getPlayerOrException();
                            } catch (Exception e) {
                                source.sendFailure(Component.literal("Este comando só pode ser usado por jogadores."));
                                return 0;
                            }

                            Claim claim = ClaimManager.getClaimByPlayer(player.getUUID());

                            if (claim == null) {
                                player.sendSystemMessage(Component.literal("Você não possui uma área protegida."));
                                return 0;
                            }

                            Map<UUID, String> trustedPlayers = claim.getTrustedPlayers();

                            if (trustedPlayers.isEmpty()) {
                                player.sendSystemMessage(Component.literal(
                                        "Nenhum jogador possui permissão na sua área protegida."));
                                return 1;
                            }

                            StringBuilder list = new StringBuilder();
                            boolean first = true;

                            for (String name : trustedPlayers.values()) {
                                if (!first) {
                                    list.append(", ");
                                }
                                list.append(name);
                                first = false;
                            }

                            player.sendSystemMessage(Component.literal("Jogadores com permissão: " + list));
                            return 1;
                        })
        );
    }

    private static void registerTrustCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("trust")
                        .then(Commands.argument("jogador", StringArgumentType.word())
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();

                                    ServerPlayer owner;
                                    try {
                                        owner = source.getPlayerOrException();
                                    } catch (Exception e) {
                                        source.sendFailure(Component.literal("Este comando só pode ser usado por jogadores."));
                                        return 0;
                                    }

                                    String playerName = StringArgumentType.getString(context, "jogador");
                                    Claim claim = ClaimManager.getClaimByPlayer(owner.getUUID());

                                    if (claim == null) {
                                        owner.sendSystemMessage(Component.literal("Você não possui uma área protegida."));
                                        return 0;
                                    }

                                    ResolvedPlayer resolved = resolvePlayer(source.getServer(), playerName);
                                    if (resolved == null) {
                                        owner.sendSystemMessage(Component.literal("Jogador não encontrado ou offline."));
                                        return 0;
                                    }

                                    if (resolved.uuid().equals(owner.getUUID())) {
                                        owner.sendSystemMessage(Component.literal("Você já é o dono da sua área."));
                                        return 0;
                                    }

                                    if (ClaimManager.playerHasBase(resolved.uuid())) {
                                        owner.sendSystemMessage(Component.literal(
                                                "O jogador informado participa de uma base e não pode ser trusted na claim individual."));
                                        return 0;
                                    }

                                    claim.trustPlayer(resolved.uuid(), resolved.name());
                                    owner.sendSystemMessage(Component.literal(
                                            "Permissão concedida para " + resolved.name() + "."));

                                    ServerPlayer onlineTrusted = source.getServer().getPlayerList().getPlayerByName(resolved.name());
                                    if (onlineTrusted != null) {
                                        onlineTrusted.sendSystemMessage(Component.literal(
                                                "Você recebeu permissão para usar a área protegida de "
                                                        + owner.getName().getString() + "."));
                                    }

                                    return 1;
                                }))
        );
    }

    private static void registerUntrustCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("untrust")
                        .then(Commands.argument("jogador", StringArgumentType.word())
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();

                                    ServerPlayer owner;
                                    try {
                                        owner = source.getPlayerOrException();
                                    } catch (Exception e) {
                                        source.sendFailure(Component.literal("Este comando só pode ser usado por jogadores."));
                                        return 0;
                                    }

                                    String playerName = StringArgumentType.getString(context, "jogador");
                                    Claim claim = ClaimManager.getClaimByPlayer(owner.getUUID());

                                    if (claim == null) {
                                        owner.sendSystemMessage(Component.literal("Você não possui uma área protegida."));
                                        return 0;
                                    }

                                    ResolvedPlayer resolved = resolvePlayer(source.getServer(), playerName);
                                    if (resolved == null) {
                                        owner.sendSystemMessage(Component.literal("Jogador não encontrado ou offline."));
                                        return 0;
                                    }

                                    claim.untrustPlayer(resolved.uuid());
                                    owner.sendSystemMessage(Component.literal(
                                            "Permissão removida de " + resolved.name() + "."));

                                    ServerPlayer onlineTrusted = source.getServer().getPlayerList().getPlayerByName(resolved.name());
                                    if (onlineTrusted != null) {
                                        onlineTrusted.sendSystemMessage(Component.literal(
                                                "Sua permissão na área protegida de "
                                                        + owner.getName().getString() + " foi removida."));
                                    }

                                    return 1;
                                }))
        );
    }

    private static void registerClaimShowCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("claimshow")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();

                            ServerPlayer player;
                            try {
                                player = source.getPlayerOrException();
                            } catch (Exception e) {
                                source.sendFailure(Component.literal("Este comando só pode ser usado por jogadores."));
                                return 0;
                            }

                            Claim claim = ClaimManager.getClaimByPlayer(player.getUUID());

                            if (claim == null) {
                                player.sendSystemMessage(Component.literal("Você não possui uma área protegida."));
                                return 0;
                            }

                            ClaimVisualizationManager.show(player.getUUID());
                            player.sendSystemMessage(Component.literal("Visualização da claim ativada."));
                            return 1;
                        })
        );
    }

    private static void registerClaimHideCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("claimhide")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();

                            ServerPlayer player;
                            try {
                                player = source.getPlayerOrException();
                            } catch (Exception e) {
                                source.sendFailure(Component.literal("Este comando só pode ser usado por jogadores."));
                                return 0;
                            }

                            if (!ClaimVisualizationManager.isShowing(player.getUUID())) {
                                player.sendSystemMessage(Component.literal("A visualização da claim já está desativada."));
                                return 0;
                            }

                            ClaimVisualizationManager.hide(player.getUUID());
                            player.sendSystemMessage(Component.literal("Visualização da claim desativada."));
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

    private static boolean isNearStructure(Level world, BlockPos pos) {
        return false;
    }
}
