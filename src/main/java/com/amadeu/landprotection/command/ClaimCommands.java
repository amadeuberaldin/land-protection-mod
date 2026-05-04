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

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClaimCommands {

    private static final int CLAIM_RADIUS_XZ = 30;
    private static final int CLAIM_RADIUS_Y_DOWN = 15;
    private static final int CLAIM_RADIUS_Y_UP = 15;

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
                            String dimension = world.dimension().toString();

                            if (!ClaimManager.canCreateClaim(player.getUUID())) {
                                player.sendSystemMessage(Component.literal(
                                        "Você já atingiu o limite de " + ClaimManager.MAX_CLAIMS_PER_PLAYER
                                                + " claims."));
                                return 0;
                            }

                            if (
        ClaimManager.getClaimAt(centerPos, dimension) != null) {
                                player.sendSystemMessage(Component.literal(
                                        "Você já está dentro de uma área protegida. Vá para outro local."));
                                return 0;
                            }

                            if (isNearStructure(world, centerPos)) {
                                player.sendSystemMessage(Component.literal(
                                        "Você não pode proteger áreas em estruturas do jogo."));
                                return 0;
                            }

                            BlockPos pos1 = centerPos.offset(-CLAIM_RADIUS_XZ, -CLAIM_RADIUS_Y_DOWN, -CLAIM_RADIUS_XZ);
                            BlockPos pos2 = centerPos.offset(CLAIM_RADIUS_XZ, CLAIM_RADIUS_Y_UP, CLAIM_RADIUS_XZ);

                            if (ClaimManager.overlapsExistingArea(pos1, pos2, dimension)) {
                                player.sendSystemMessage(Component.literal(
                                        "Não é possível criar a claim aqui porque ela sobrepõe outra área protegida."));
                                return 0;
                            }

                            Claim claim = new Claim(player.getUUID(), pos1, pos2, centerPos, dimension);
                            ClaimManager.addClaim(claim);

                            int total = ClaimManager.playerClaimCount(player.getUUID());

                            player.sendSystemMessage(Component.literal(
                                    "Área protegida criada com sucesso. Você agora possui "
                                            + total + "/" + ClaimManager.MAX_CLAIMS_PER_PLAYER + " claims."));
                            return 1;
                        }));
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
                            String dimension = source.getLevel().dimension().toString();
                            Claim claim = ClaimManager.getClaimAtOwnedBy(player.getUUID(), pos, dimension);

                            if (claim == null) {
                                player.sendSystemMessage(Component.literal(
                                        "Você precisa estar dentro de uma claim sua para removê-la."));
                                return 0;
                            }

                            ClaimManager.removeClaim(claim);
                            ClaimVisualizationManager.hide(player.getUUID(), claim);

                            player.sendSystemMessage(Component.literal(
                                    "A claim atual foi removida com sucesso."));
                            return 1;
                        }));
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

                            List<Claim> claims = ClaimManager.getClaimsByPlayer(player.getUUID());

                            if (claims.isEmpty()) {
                                player.sendSystemMessage(Component.literal("Você não possui nenhuma área protegida."));
                                return 0;
                            }

                            player.sendSystemMessage(Component.literal(
                                    "Suas claims (" + claims.size() + "/" + ClaimManager.MAX_CLAIMS_PER_PLAYER + "):"));

                            int index = 1;
                            for (Claim claim : claims) {
                                BlockPos center = claim.getCenter();
                                player.sendSystemMessage(Component.literal(
                                        "#" + index
                                                + " - X: " + center.getX()
                                                + " Y: " + center.getY()
                                                + " Z: " + center.getZ()));
                                index++;
                            }

                            return 1;
                        }));
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

                            String dimension = source.getLevel().dimension().toString();
                            Claim claim = ClaimManager.getClaimAtOwnedBy(player.getUUID(), player.blockPosition(), dimension);

                            if (claim == null) {
                                player.sendSystemMessage(Component.literal(
                                        "Você precisa estar dentro de uma claim sua para ver a trustlist."));
                                return 0;
                            }

                            Map<UUID, String> trustedPlayers = claim.getTrustedPlayers();

                            if (trustedPlayers.isEmpty()) {
                                player.sendSystemMessage(Component.literal(
                                        "Nenhum jogador possui permissão nesta claim."));
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

                            player.sendSystemMessage(Component.literal("Jogadores com permissão nesta claim: " + list));
                            return 1;
                        }));
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
                                        source.sendFailure(
                                                Component.literal("Este comando só pode ser usado por jogadores."));
                                        return 0;
                                    }

                                    String playerName = StringArgumentType.getString(context, "jogador");
                                    String dimension = source.getLevel().dimension().toString();
                                    Claim claim = ClaimManager.getClaimAtOwnedBy(owner.getUUID(),
                                            owner.blockPosition(), dimension);

                                    if (claim == null) {
                                        owner.sendSystemMessage(Component.literal(
                                                "Você precisa estar dentro de uma claim sua para usar /trust."));
                                        return 0;
                                    }

                                    ResolvedPlayer resolved = resolvePlayer(source.getServer(), playerName);
                                    if (resolved == null) {
                                        owner.sendSystemMessage(
                                                Component.literal("Jogador não encontrado ou offline."));
                                        return 0;
                                    }

                                    if (resolved.uuid().equals(owner.getUUID())) {
                                        owner.sendSystemMessage(Component.literal("Você já é o dono desta claim."));
                                        return 0;
                                    }

                                    claim.trustPlayer(resolved.uuid(), resolved.name());
                                    owner.sendSystemMessage(Component.literal(
                                            "Permissão concedida para " + resolved.name() + " nesta claim."));

                                    ServerPlayer onlineTrusted = source.getServer().getPlayerList()
                                            .getPlayerByName(resolved.name());
                                    if (onlineTrusted != null) {
                                        onlineTrusted.sendSystemMessage(Component.literal(
                                                "Você recebeu permissão para usar uma claim de "
                                                        + owner.getName().getString() + "."));
                                    }

                                    return 1;
                                })));
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
                                        source.sendFailure(
                                                Component.literal("Este comando só pode ser usado por jogadores."));
                                        return 0;
                                    }

                                    String playerName = StringArgumentType.getString(context, "jogador");
                                    String dimension = source.getLevel().dimension().toString();
                                    Claim claim = ClaimManager.getClaimAtOwnedBy(owner.getUUID(),
                                            owner.blockPosition(), dimension);

                                    if (claim == null) {
                                        owner.sendSystemMessage(Component.literal(
                                                "Você precisa estar dentro de uma claim sua para usar /untrust."));
                                        return 0;
                                    }

                                    ResolvedPlayer resolved = resolvePlayer(source.getServer(), playerName);
                                    if (resolved == null) {
                                        owner.sendSystemMessage(
                                                Component.literal("Jogador não encontrado ou offline."));
                                        return 0;
                                    }

                                    claim.untrustPlayer(resolved.uuid());
                                    owner.sendSystemMessage(Component.literal(
                                            "Permissão removida de " + resolved.name() + " nesta claim."));

                                    ServerPlayer onlineTrusted = source.getServer().getPlayerList()
                                            .getPlayerByName(resolved.name());
                                    if (onlineTrusted != null) {
                                        onlineTrusted.sendSystemMessage(Component.literal(
                                                "Sua permissão em uma claim de "
                                                        + owner.getName().getString() + " foi removida."));
                                    }

                                    return 1;
                                })));
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

                            String dimension = source.getLevel().dimension().toString();
                            Claim claim = ClaimManager.getClaimAtOwnedBy(player.getUUID(), player.blockPosition(), dimension);

                            if (claim == null) {
                                player.sendSystemMessage(Component.literal(
                                        "Você precisa estar dentro de uma claim sua para usar /claimshow."));
                                return 0;
                            }

                            ClaimVisualizationManager.show(player.getUUID(), claim);
                            player.sendSystemMessage(Component.literal("Visualização desta claim ativada."));
                            return 1;
                        }));
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

                            String dimension = source.getLevel().dimension().toString();
                            Claim claim = ClaimManager.getClaimAtOwnedBy(player.getUUID(), player.blockPosition(), dimension);

                            if (claim == null) {
                                player.sendSystemMessage(Component.literal(
                                        "Você precisa estar dentro de uma claim sua para usar /claimhide."));
                                return 0;
                            }

                            if (!ClaimVisualizationManager.isShowing(player.getUUID(), claim)) {
                                player.sendSystemMessage(
                                        Component.literal("A visualização desta claim já está desativada."));
                                return 0;
                            }

                            ClaimVisualizationManager.hide(player.getUUID(), claim);
                            player.sendSystemMessage(Component.literal("Visualização desta claim desativada."));
                            return 1;
                        }));
    }

    private static ResolvedPlayer resolvePlayer(MinecraftServer server, String playerName) {
        ServerPlayer onlinePlayer = server.getPlayerList().getPlayerByName(playerName);

        if (onlinePlayer != null) {
            return new ResolvedPlayer(
                    onlinePlayer.getUUID(),
                    onlinePlayer.getName().getString());
        }

        return null;
    }

    private record ResolvedPlayer(UUID uuid, String name) {
    }

    private static boolean isNearStructure(Level world, BlockPos pos) {
        return false;
    }
}