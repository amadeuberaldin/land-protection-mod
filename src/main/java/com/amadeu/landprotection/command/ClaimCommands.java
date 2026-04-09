package com.amadeu.landprotection.command;

import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import com.amadeu.landprotection.visual.ClaimVisualizationManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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

    private static void registerClaimCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("claim")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();

                            if (source.getPlayer() == null) {
                                source.sendError(Text.literal("Este comando só pode ser usado por jogadores."));
                                return 0;
                            }

                            ServerPlayerEntity player = source.getPlayer();
                            BlockPos centerPos = player.getBlockPos();
                            World world = source.getWorld();

                            if (ClaimManager.playerHasClaim(player.getUuid())) {
                                player.sendMessage(Text.literal("Você já possui uma área protegida. Use /disclaim antes de criar outra."), false);
                                return 0;
                            }
                            
                            if (isNearStructure(world, centerPos)) {
                                player.sendMessage(Text.literal("Você não pode proteger áreas em estruturas do jogo."), false);
                                return 0;
                            }

                            BlockPos pos1 = centerPos.add(-CLAIM_RADIUS_XZ, -CLAIM_RADIUS_Y_DOWN, -CLAIM_RADIUS_XZ);
                            BlockPos pos2 = centerPos.add(CLAIM_RADIUS_XZ, CLAIM_RADIUS_Y_UP, CLAIM_RADIUS_XZ);

                            if (ClaimManager.overlapsExistingArea(pos1, pos2)) {
                                player.sendMessage(Text.literal("Não é possível criar a claim aqui porque ela sobrepõe outra área protegida."), false);
                                return 0;
                            }

                            Claim claim = new Claim(
                                    player.getUuid(),
                                    pos1,
                                    pos2,
                                    centerPos
                            );

                            ClaimManager.addClaim(claim);
                            player.sendMessage(Text.literal("Área protegida criada com sucesso no local atual."), false);
                            return 1;
                        })
        );
    }

    private static void registerDisclaimCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("disclaim")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();

                            if (source.getPlayer() == null) {
                                source.sendError(Text.literal("Este comando só pode ser usado por jogadores."));
                                return 0;
                            }

                            ServerPlayerEntity player = source.getPlayer();
                            BlockPos pos = player.getBlockPos();

                            Claim claim = ClaimManager.getClaimAt(pos);

                            if (claim == null) {
                                player.sendMessage(Text.literal("Você não está dentro de nenhuma área protegida individual."), false);
                                return 0;
                            }

                            if (!claim.isOwner(player.getUuid())) {
                                player.sendMessage(Text.literal("Esta área protegida não pertence a você."), false);
                                return 0;
                            }

                            ClaimManager.removeClaim(claim);
                            player.sendMessage(Text.literal("Sua área protegida foi removida com sucesso."), false);
                            return 1;
                        })
        );
    }

    private static void registerClaimLocationCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("claimlocation")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();

                            if (source.getPlayer() == null) {
                                source.sendError(Text.literal("Este comando só pode ser usado por jogadores."));
                                return 0;
                            }

                            ServerPlayerEntity player = source.getPlayer();
                            Claim claim = ClaimManager.getClaimByPlayer(player.getUuid());

                            if (claim == null) {
                                player.sendMessage(Text.literal("Você não possui uma área protegida."), false);
                                return 0;
                            }

                            BlockPos center = claim.getCenter();
                            player.sendMessage(
                                    Text.literal("Sua área protegida foi criada em X: "
                                            + center.getX()
                                            + " Y: "
                                            + center.getY()
                                            + " Z: "
                                            + center.getZ()
                                            + "."),
                                    false
                            );
                            return 1;
                        })
        );
    }

    private static void registerTrustListCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("trustlist")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();

                            if (source.getPlayer() == null) {
                                source.sendError(Text.literal("Este comando só pode ser usado por jogadores."));
                                return 0;
                            }

                            ServerPlayerEntity player = source.getPlayer();
                            Claim claim = ClaimManager.getClaimByPlayer(player.getUuid());

                            if (claim == null) {
                                player.sendMessage(Text.literal("Você não possui uma área protegida."), false);
                                return 0;
                            }

                            Map<UUID, String> trustedPlayers = claim.getTrustedPlayers();

                            if (trustedPlayers.isEmpty()) {
                                player.sendMessage(Text.literal("Nenhum jogador possui permissão na sua área protegida."), false);
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

                            player.sendMessage(Text.literal("Jogadores com permissão: " + list), false);
                            return 1;
                        })
        );
    }

    private static void registerTrustCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("trust")
                        .then(CommandManager.argument("jogador", StringArgumentType.word())
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();

                                    if (source.getPlayer() == null) {
                                        source.sendError(Text.literal("Este comando só pode ser usado por jogadores."));
                                        return 0;
                                    }

                                    ServerPlayerEntity owner = source.getPlayer();
                                    String playerName = StringArgumentType.getString(context, "jogador");
                                    Claim claim = ClaimManager.getClaimByPlayer(owner.getUuid());

                                    if (claim == null) {
                                        owner.sendMessage(Text.literal("Você não possui uma área protegida."), false);
                                        return 0;
                                    }

                                    ResolvedPlayer resolved = resolvePlayer(source.getServer(), playerName);
                                    if (resolved == null) {
                                        owner.sendMessage(Text.literal("Jogador não encontrado ou offline."), false);
                                        return 0;
                                    }

                                    if (resolved.uuid().equals(owner.getUuid())) {
                                        owner.sendMessage(Text.literal("Você já é o dono da sua área."), false);
                                        return 0;
                                    }

                                    if (ClaimManager.playerHasBase(resolved.uuid())) {
                                        owner.sendMessage(Text.literal("O jogador informado participa de uma base e não pode ser trusted na claim individual."), false);
                                        return 0;
                                    }

                                    claim.trustPlayer(resolved.uuid(), resolved.name());
                                    owner.sendMessage(Text.literal("Permissão concedida para " + resolved.name() + "."), false);

                                    ServerPlayerEntity onlineTrusted = source.getServer().getPlayerManager().getPlayer(resolved.uuid());
                                    if (onlineTrusted != null) {
                                        onlineTrusted.sendMessage(
                                                Text.literal("Você recebeu permissão para usar a área protegida de " + owner.getName().getString() + "."),
                                                false
                                        );
                                    }

                                    return 1;
                                }))
        );
    }

    private static void registerUntrustCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("untrust")
                        .then(CommandManager.argument("jogador", StringArgumentType.word())
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();

                                    if (source.getPlayer() == null) {
                                        source.sendError(Text.literal("Este comando só pode ser usado por jogadores."));
                                        return 0;
                                    }

                                    ServerPlayerEntity owner = source.getPlayer();
                                    String playerName = StringArgumentType.getString(context, "jogador");
                                    Claim claim = ClaimManager.getClaimByPlayer(owner.getUuid());

                                    if (claim == null) {
                                        owner.sendMessage(Text.literal("Você não possui uma área protegida."), false);
                                        return 0;
                                    }

                                    ResolvedPlayer resolved = resolvePlayer(source.getServer(), playerName);
                                    if (resolved == null) {
                                        owner.sendMessage(Text.literal("Jogador não encontrado ou offline."), false);
                                        return 0;
                                    }

                                    claim.untrustPlayer(resolved.uuid());
                                    owner.sendMessage(Text.literal("Permissão removida de " + resolved.name() + "."), false);

                                    ServerPlayerEntity onlineTrusted = source.getServer().getPlayerManager().getPlayer(resolved.uuid());
                                    if (onlineTrusted != null) {
                                        onlineTrusted.sendMessage(
                                                Text.literal("Sua permissão na área protegida de "
                                                        + owner.getName().getString() + " foi removida."),
                                                false
                                        );
                                    }

                                    return 1;
                                }))
        );
    }

    private static void registerClaimShowCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("claimshow")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();

                            if (source.getPlayer() == null) {
                                source.sendError(Text.literal("Este comando só pode ser usado por jogadores."));
                                return 0;
                            }

                            ServerPlayerEntity player = source.getPlayer();
                            Claim claim = ClaimManager.getClaimByPlayer(player.getUuid());

                            if (claim == null) {
                                player.sendMessage(Text.literal("Você não possui uma área protegida."), false);
                                return 0;
                            }

                            ClaimVisualizationManager.show(player.getUuid());
                            player.sendMessage(Text.literal("Visualização da sua área protegida ativada."), false);
                            return 1;
                        })
        );
    }

    private static void registerClaimHideCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("claimhide")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();

                            if (source.getPlayer() == null) {
                                source.sendError(Text.literal("Este comando só pode ser usado por jogadores."));
                                return 0;
                            }

                            ServerPlayerEntity player = source.getPlayer();

                            if (!ClaimVisualizationManager.isShowing(player.getUuid())) {
                                player.sendMessage(Text.literal("A visualização da sua área protegida já está desativada."), false);
                                return 0;
                            }

                            ClaimVisualizationManager.hide(player.getUuid());
                            player.sendMessage(Text.literal("Visualização da sua área protegida desativada."), false);
                            return 1;
                        })
        );
    }

    private static ResolvedPlayer resolvePlayer(MinecraftServer server, String playerName) {
        ServerPlayerEntity onlinePlayer = server.getPlayerManager().getPlayer(playerName);

        if (onlinePlayer != null) {
            return new ResolvedPlayer(
                    onlinePlayer.getUuid(),
                    onlinePlayer.getName().getString()
            );
        }

        return null;
    }

    private record ResolvedPlayer(UUID uuid, String name) {
    }

    private static boolean isNearStructure(World world, BlockPos pos) {
        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos checkPos = pos.add(x, y, z);
                    Block block = world.getBlockState(checkPos).getBlock();

                    if (block == Blocks.NETHER_BRICKS
                            || block == Blocks.CRACKED_NETHER_BRICKS
                            || block == Blocks.NETHER_BRICK_FENCE
                            || block == Blocks.SOUL_SAND
                            || block == Blocks.MOSSY_COBBLESTONE
                            || block == Blocks.SPAWNER
                            || block == Blocks.COPPER_BULB
                            || block == Blocks.WAXED_COPPER_BULB
                            || block == Blocks.COPPER_DOOR
                            || block == Blocks.WAXED_COPPER_DOOR
                            || block == Blocks.COPPER_TRAPDOOR
                            || block == Blocks.WAXED_COPPER_TRAPDOOR
                            || block == Blocks.CHISELED_TUFF
                            || block == Blocks.POLISHED_TUFF
                            || block == Blocks.TUFF_BRICKS
                            || block == Blocks.TRIAL_SPAWNER
                            || block == Blocks.VAULT) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}