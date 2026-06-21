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
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.mojang.brigadier.arguments.IntegerArgumentType;

public class ClaimCommands {

        private static final int CLAIM_RADIUS_XZ = 30;
        private static final int SPAWN_PROTECTION_RADIUS = 100;
        private static final int WORLD_MIN_Y = -64;
        private static final int WORLD_MAX_Y = 320;

        public static void register() {
                CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
                        registerClaimCommand(dispatcher);
                        registerDisclaimCommand(dispatcher);
                        registerClaimLocationCommand(dispatcher);
                        registerTrustListCommand(dispatcher);
                        registerTrustCommand(dispatcher);
                        registerUntrustCommand(dispatcher);
                        registerGuestCommand(dispatcher);
                        registerUnguestCommand(dispatcher);
                        registerGuestListCommand(dispatcher);
                        registerClaimShowCommand(dispatcher);
                        registerClaimHideCommand(dispatcher);
                        registerClaimAdminCommand(dispatcher);
                        registerColizeuCommand(dispatcher);
                        registerArenaCommand(dispatcher);
                        registerColizeuBuilderCommand(dispatcher);
                        registerColizeuUnbuilderCommand(dispatcher);
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
                                                                source.sendFailure(Component.literal(
                                                                                "Este comando só pode ser usado por jogadores."));
                                                                return 0;
                                                        }

                                                        BlockPos centerPos = player.blockPosition();
                                                        Level world = source.getLevel();
                                                        String dimension = world.dimension().toString();

                                                        if (!ClaimManager.canCreateClaim(player.getUUID())) {
                                                                player.sendSystemMessage(Component.literal(
                                                                                "Você já atingiu o limite de "
                                                                                                + ClaimManager.MAX_CLAIMS_PER_PLAYER
                                                                                                + " claims."));
                                                                return 0;
                                                        }

                                                        if (ClaimManager.getClaimAt(centerPos, dimension) != null) {
                                                                player.sendSystemMessage(Component.literal(
                                                                                "Você já está dentro de uma área protegida. Vá para outro local."));
                                                                return 0;
                                                        }

                                                        if (isNearStructure(world, centerPos)) {
                                                                player.sendSystemMessage(Component.literal(
                                                                                "Você não pode proteger áreas em estruturas do jogo."));
                                                                return 0;
                                                        }

                                                        BlockPos pos1 = new BlockPos(
                                                                        centerPos.getX() - CLAIM_RADIUS_XZ,
                                                                        WORLD_MIN_Y,
                                                                        centerPos.getZ() - CLAIM_RADIUS_XZ);

                                                        BlockPos pos2 = new BlockPos(
                                                                        centerPos.getX() + CLAIM_RADIUS_XZ,
                                                                        WORLD_MAX_Y,
                                                                        centerPos.getZ() + CLAIM_RADIUS_XZ);

                                                        if (overlapsSpawnProtection(pos1, pos2)) {
                                                                player.sendSystemMessage(Component.literal(
                                                                                "Você não pode criar claims próximas ao spawn."));
                                                                return 0;
                                                        }
                                                        if (ClaimManager.overlapsExistingArea(pos1, pos2, dimension)) {
                                                                player.sendSystemMessage(Component.literal(
                                                                                "Não é possível criar a claim aqui porque ela sobrepõe outra área protegida."));
                                                                return 0;
                                                        }

                                                        Claim claim = new Claim(player.getUUID(), pos1, pos2, centerPos,
                                                                        dimension);
                                                        ClaimManager.addClaim(claim);

                                                        int total = ClaimManager.playerClaimCount(player.getUUID());

                                                        player.sendSystemMessage(Component.literal(
                                                                        "Área protegida criada com sucesso. Você agora possui "
                                                                                        + total + "/"
                                                                                        + ClaimManager.MAX_CLAIMS_PER_PLAYER
                                                                                        + " claims."));
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
                                                                source.sendFailure(Component.literal(
                                                                                "Este comando só pode ser usado por jogadores."));
                                                                return 0;
                                                        }

                                                        BlockPos pos = player.blockPosition();
                                                        String dimension = source.getLevel().dimension().toString();
                                                        Claim claim = ClaimManager.getClaimAtOwnedBy(player.getUUID(),
                                                                        pos, dimension);

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
                                                                source.sendFailure(Component.literal(
                                                                                "Este comando só pode ser usado por jogadores."));
                                                                return 0;
                                                        }

                                                        List<Claim> claims = ClaimManager
                                                                        .getClaimsByPlayer(player.getUUID());

                                                        if (claims.isEmpty()) {
                                                                player.sendSystemMessage(Component.literal(
                                                                                "Você não possui nenhuma área protegida."));
                                                                return 0;
                                                        }

                                                        player.sendSystemMessage(Component.literal(
                                                                        "Suas claims (" + claims.size() + "/"
                                                                                        + ClaimManager.MAX_CLAIMS_PER_PLAYER
                                                                                        + "):"));

                                                        int index = 1;
                                                        for (Claim claim : claims) {
                                                                BlockPos center = claim.getCenter();
                                                                player.sendSystemMessage(Component.literal(
                                                                                "#" + index
                                                                                                + " - X: "
                                                                                                + center.getX()
                                                                                                + " Y: " + center.getY()
                                                                                                + " Z: "
                                                                                                + center.getZ()));
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
                                                                source.sendFailure(Component.literal(
                                                                                "Este comando só pode ser usado por jogadores."));
                                                                return 0;
                                                        }

                                                        String dimension = source.getLevel().dimension().toString();
                                                        Claim claim = ClaimManager.getClaimAtOwnedBy(player.getUUID(),
                                                                        player.blockPosition(),
                                                                        dimension);

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

                                                        player.sendSystemMessage(Component
                                                                        .literal("Jogadores com permissão nesta claim: "
                                                                                        + list));
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
                                                                                                Component.literal(
                                                                                                                "Este comando só pode ser usado por jogadores."));
                                                                                return 0;
                                                                        }

                                                                        String playerName = StringArgumentType
                                                                                        .getString(context, "jogador");
                                                                        String dimension = source.getLevel().dimension()
                                                                                        .toString();
                                                                        Claim claim = ClaimManager.getClaimAtOwnedBy(
                                                                                        owner.getUUID(),
                                                                                        owner.blockPosition(),
                                                                                        dimension);

                                                                        if (claim == null) {
                                                                                owner.sendSystemMessage(
                                                                                                Component.literal(
                                                                                                                "Você precisa estar dentro de uma claim sua para usar /trust."));
                                                                                return 0;
                                                                        }

                                                                        ResolvedPlayer resolved = resolvePlayer(
                                                                                        source.getServer(), playerName);
                                                                        if (resolved == null) {
                                                                                owner.sendSystemMessage(
                                                                                                Component.literal(
                                                                                                                "Jogador não encontrado ou offline."));
                                                                                return 0;
                                                                        }

                                                                        if (resolved.uuid().equals(owner.getUUID())) {
                                                                                owner.sendSystemMessage(Component
                                                                                                .literal("Você já é o dono desta claim."));
                                                                                return 0;
                                                                        }

                                                                        claim.trustPlayer(resolved.uuid(),
                                                                                        resolved.name());
                                                                        owner.sendSystemMessage(Component.literal(
                                                                                        "Permissão concedida para "
                                                                                                        + resolved.name()
                                                                                                        + " nesta claim."));

                                                                        ServerPlayer onlineTrusted = source.getServer()
                                                                                        .getPlayerList()
                                                                                        .getPlayerByName(resolved
                                                                                                        .name());
                                                                        if (onlineTrusted != null) {
                                                                                onlineTrusted.sendSystemMessage(
                                                                                                Component.literal(
                                                                                                                "Você recebeu permissão para usar uma claim de "
                                                                                                                                + owner.getName()
                                                                                                                                                .getString()
                                                                                                                                + "."));
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
                                                                                                Component.literal(
                                                                                                                "Este comando só pode ser usado por jogadores."));
                                                                                return 0;
                                                                        }

                                                                        String playerName = StringArgumentType
                                                                                        .getString(context, "jogador");
                                                                        String dimension = source.getLevel().dimension()
                                                                                        .toString();
                                                                        Claim claim = ClaimManager.getClaimAtOwnedBy(
                                                                                        owner.getUUID(),
                                                                                        owner.blockPosition(),
                                                                                        dimension);

                                                                        if (claim == null) {
                                                                                owner.sendSystemMessage(
                                                                                                Component.literal(
                                                                                                                "Você precisa estar dentro de uma claim sua para usar /untrust."));
                                                                                return 0;
                                                                        }

                                                                        ResolvedPlayer resolved = resolvePlayer(
                                                                                        source.getServer(), playerName);
                                                                        if (resolved == null) {
                                                                                owner.sendSystemMessage(
                                                                                                Component.literal(
                                                                                                                "Jogador não encontrado ou offline."));
                                                                                return 0;
                                                                        }

                                                                        claim.untrustPlayer(resolved.uuid());
                                                                        owner.sendSystemMessage(Component.literal(
                                                                                        "Permissão removida de "
                                                                                                        + resolved.name()
                                                                                                        + " nesta claim."));

                                                                        ServerPlayer onlineTrusted = source.getServer()
                                                                                        .getPlayerList()
                                                                                        .getPlayerByName(resolved
                                                                                                        .name());
                                                                        if (onlineTrusted != null) {
                                                                                onlineTrusted.sendSystemMessage(
                                                                                                Component.literal(
                                                                                                                "Sua permissão em uma claim de "
                                                                                                                                + owner.getName()
                                                                                                                                                .getString()
                                                                                                                                + " foi removida."));
                                                                        }

                                                                        return 1;
                                                                })));
        }

        private static void registerGuestCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
                dispatcher.register(
                                Commands.literal("guest")
                                                .then(Commands.argument("jogador", StringArgumentType.word())
                                                                .executes(context -> {

                                                                        CommandSourceStack source = context.getSource();

                                                                        ServerPlayer owner;

                                                                        try {
                                                                                owner = source.getPlayerOrException();
                                                                        } catch (Exception e) {
                                                                                source.sendFailure(Component.literal(
                                                                                                "Este comando só pode ser usado por jogadores."));
                                                                                return 0;
                                                                        }

                                                                        String playerName = StringArgumentType
                                                                                        .getString(context, "jogador");

                                                                        String dimension = source.getLevel().dimension()
                                                                                        .toString();

                                                                        Claim claim = ClaimManager.getClaimAtOwnedBy(
                                                                                        owner.getUUID(),
                                                                                        owner.blockPosition(),
                                                                                        dimension);

                                                                        if (claim == null) {
                                                                                owner.sendSystemMessage(
                                                                                                Component.literal(
                                                                                                                "Você precisa estar dentro de uma claim sua para usar /guest."));
                                                                                return 0;
                                                                        }

                                                                        ResolvedPlayer resolved = resolvePlayer(
                                                                                        source.getServer(),
                                                                                        playerName);

                                                                        if (resolved == null) {
                                                                                owner.sendSystemMessage(
                                                                                                Component.literal(
                                                                                                                "Jogador não encontrado ou offline."));
                                                                                return 0;
                                                                        }

                                                                        if (resolved.uuid().equals(owner.getUUID())) {
                                                                                owner.sendSystemMessage(
                                                                                                Component.literal(
                                                                                                                "Você já é o dono desta claim."));
                                                                                return 0;
                                                                        }

                                                                        claim.guestPlayer(
                                                                                        resolved.uuid(),
                                                                                        resolved.name());

                                                                        owner.sendSystemMessage(Component.literal(
                                                                                        resolved.name()
                                                                                                        + " agora possui acesso limitado nesta claim."));

                                                                        return 1;
                                                                })));
        }

        private static void registerUnguestCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
                dispatcher.register(
                                Commands.literal("unguest")
                                                .then(Commands.argument("jogador", StringArgumentType.word())
                                                                .executes(context -> {

                                                                        CommandSourceStack source = context.getSource();

                                                                        ServerPlayer owner;

                                                                        try {
                                                                                owner = source.getPlayerOrException();
                                                                        } catch (Exception e) {
                                                                                source.sendFailure(Component.literal(
                                                                                                "Este comando só pode ser usado por jogadores."));
                                                                                return 0;
                                                                        }

                                                                        String playerName = StringArgumentType
                                                                                        .getString(context, "jogador");

                                                                        String dimension = source.getLevel().dimension()
                                                                                        .toString();

                                                                        Claim claim = ClaimManager.getClaimAtOwnedBy(
                                                                                        owner.getUUID(),
                                                                                        owner.blockPosition(),
                                                                                        dimension);

                                                                        if (claim == null) {
                                                                                owner.sendSystemMessage(
                                                                                                Component.literal(
                                                                                                                "Você precisa estar dentro de uma claim sua para usar /unguest."));
                                                                                return 0;
                                                                        }

                                                                        ResolvedPlayer resolved = resolvePlayer(
                                                                                        source.getServer(),
                                                                                        playerName);

                                                                        if (resolved == null) {
                                                                                owner.sendSystemMessage(
                                                                                                Component.literal(
                                                                                                                "Jogador não encontrado ou offline."));
                                                                                return 0;
                                                                        }

                                                                        claim.unguestPlayer(resolved.uuid());

                                                                        owner.sendSystemMessage(Component.literal(
                                                                                        resolved.name()
                                                                                                        + " não possui mais acesso guest nesta claim."));

                                                                        return 1;
                                                                })));
        }

        private static void registerGuestListCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
                dispatcher.register(
                                Commands.literal("guestlist")
                                                .executes(context -> {

                                                        CommandSourceStack source = context.getSource();

                                                        ServerPlayer player;

                                                        try {
                                                                player = source.getPlayerOrException();
                                                        } catch (Exception e) {
                                                                source.sendFailure(Component.literal(
                                                                                "Este comando só pode ser usado por jogadores."));
                                                                return 0;
                                                        }

                                                        String dimension = source.getLevel().dimension().toString();

                                                        Claim claim = ClaimManager.getClaimAtOwnedBy(
                                                                        player.getUUID(),
                                                                        player.blockPosition(),
                                                                        dimension);

                                                        if (claim == null) {
                                                                player.sendSystemMessage(Component.literal(
                                                                                "Você precisa estar dentro de uma claim sua para usar /guestlist."));
                                                                return 0;
                                                        }

                                                        Map<UUID, String> guests = claim.getGuestPlayers();

                                                        if (guests.isEmpty()) {
                                                                player.sendSystemMessage(Component.literal(
                                                                                "Nenhum jogador possui acesso guest nesta claim."));
                                                                return 1;
                                                        }

                                                        StringBuilder list = new StringBuilder();
                                                        boolean first = true;

                                                        for (String name : guests.values()) {

                                                                if (!first) {
                                                                        list.append(", ");
                                                                }

                                                                list.append(name);
                                                                first = false;
                                                        }

                                                        player.sendSystemMessage(Component.literal(
                                                                        "Guests desta claim: " + list));

                                                        return 1;
                                                }));
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
                                                                source.sendFailure(Component.literal(
                                                                                "Este comando só pode ser usado por jogadores."));
                                                                return 0;
                                                        }

                                                        String dimension = source.getLevel().dimension().toString();
                                                        Claim claim = ClaimManager.getClaimAtOwnedBy(player.getUUID(),
                                                                        player.blockPosition(),
                                                                        dimension);

                                                        if (claim == null) {
                                                                player.sendSystemMessage(Component.literal(
                                                                                "Você precisa estar dentro de uma claim sua para usar /claimshow."));
                                                                return 0;
                                                        }

                                                        ClaimVisualizationManager.show(player.getUUID(), claim);
                                                        player.sendSystemMessage(Component
                                                                        .literal("Visualização desta claim ativada."));
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
                                                                source.sendFailure(Component.literal(
                                                                                "Este comando só pode ser usado por jogadores."));
                                                                return 0;
                                                        }

                                                        String dimension = source.getLevel().dimension().toString();
                                                        Claim claim = ClaimManager.getClaimAtOwnedBy(player.getUUID(),
                                                                        player.blockPosition(),
                                                                        dimension);

                                                        if (claim == null) {
                                                                player.sendSystemMessage(Component.literal(
                                                                                "Você precisa estar dentro de uma claim sua para usar /claimhide."));
                                                                return 0;
                                                        }

                                                        if (!ClaimVisualizationManager.isShowing(player.getUUID(),
                                                                        claim)) {
                                                                player.sendSystemMessage(
                                                                                Component.literal(
                                                                                                "A visualização desta claim já está desativada."));
                                                                return 0;
                                                        }

                                                        ClaimVisualizationManager.hide(player.getUUID(), claim);
                                                        player.sendSystemMessage(Component.literal(
                                                                        "Visualização desta claim desativada."));
                                                        return 1;
                                                }));
        }

        private static void registerClaimAdminCommand(CommandDispatcher<CommandSourceStack> dispatcher) {

                dispatcher.register(
                                Commands.literal("claimadmin")
                                                .then(Commands.literal("here")
                                                                .executes(context -> {
                                                                        CommandSourceStack source = context.getSource();
                                                                        ServerPlayer player = source
                                                                                        .getPlayerOrException();

                                                                        if (!source.getServer().getPlayerList()
                                                                                        .isOp(new NameAndId(player
                                                                                                        .getGameProfile()))) {
                                                                                player.sendSystemMessage(
                                                                                                Component.literal(
                                                                                                                "Você não possui permissão para usar este comando."));
                                                                                return 0;
                                                                        }

                                                                        String dimension = source.getLevel().dimension()
                                                                                        .toString();

                                                                        Claim claim = ClaimManager.getArenaAt(
                                                                                        player.blockPosition(),
                                                                                        dimension);

                                                                        if (claim == null) {
                                                                                claim = ClaimManager.getServerClaimAt(
                                                                                                player.blockPosition(),
                                                                                                dimension);
                                                                        }

                                                                        if (claim == null) {
                                                                                claim = ClaimManager.getClaimAt(
                                                                                                player.blockPosition(),
                                                                                                dimension);
                                                                        }

                                                                        if (claim == null) {
                                                                                player.sendSystemMessage(
                                                                                                Component.literal(
                                                                                                                "Não existe nenhuma claim nesta posição."));
                                                                                return 0;
                                                                        }

                                                                        BlockPos center = claim.getCenter();

                                                                        player.sendSystemMessage(Component.literal(
                                                                                        "Informações da claim atual:"));
                                                                        player.sendSystemMessage(Component
                                                                                        .literal("Tipo: Claim normal"));
                                                                        player.sendSystemMessage(Component.literal(
                                                                                        "Server Claim: " + claim
                                                                                                        .isServerClaim()));
                                                                        player.sendSystemMessage(Component.literal(
                                                                                        "Arena: " + claim.isArena()));
                                                                        player.sendSystemMessage(Component
                                                                                        .literal("Dono UUID: " + claim
                                                                                                        .getOwner()));
                                                                        player.sendSystemMessage(Component
                                                                                        .literal("Dimensão: " + claim
                                                                                                        .getDimension()));
                                                                        player.sendSystemMessage(Component.literal(
                                                                                        "Centro: X " + center.getX()
                                                                                                        + " Y "
                                                                                                        + center.getY()
                                                                                                        + " Z "
                                                                                                        + center.getZ()));
                                                                        player.sendSystemMessage(Component.literal(
                                                                                        "Pos1: " + claim.getPos1()
                                                                                                        .getX() + " "
                                                                                                        + claim.getPos1()
                                                                                                                        .getY()
                                                                                                        + " "
                                                                                                        + claim.getPos1()
                                                                                                                        .getZ()));
                                                                        player.sendSystemMessage(Component.literal(
                                                                                        "Pos2: " + claim.getPos2()
                                                                                                        .getX() + " "
                                                                                                        + claim.getPos2()
                                                                                                                        .getY()
                                                                                                        + " "
                                                                                                        + claim.getPos2()
                                                                                                                        .getZ()));
                                                                        player.sendSystemMessage(
                                                                                        Component.literal("Trustlist: "
                                                                                                        + claim.getTrustedPlayers()
                                                                                                                        .values()));
                                                                        player.sendSystemMessage(
                                                                                        Component.literal("Builders: "
                                                                                                        + claim.getBuilders()
                                                                                                                        .values()));

                                                                        return 1;
                                                                }))

                                                .then(Commands.literal("listall")
                                                                .executes(context -> {
                                                                        CommandSourceStack source = context.getSource();
                                                                        ServerPlayer player = source
                                                                                        .getPlayerOrException();

                                                                        if (!source.getServer().getPlayerList()
                                                                                        .isOp(new NameAndId(player
                                                                                                        .getGameProfile()))) {
                                                                                player.sendSystemMessage(
                                                                                                Component.literal(
                                                                                                                "Você não possui permissão para usar este comando."));
                                                                                return 0;
                                                                        }

                                                                        List<Claim> claims = ClaimManager.getClaims();

                                                                        if (claims.isEmpty()) {
                                                                                player.sendSystemMessage(Component
                                                                                                .literal("Não existe nenhuma claim registrada."));
                                                                                return 0;
                                                                        }

                                                                        player.sendSystemMessage(Component.literal(
                                                                                        "Claims registradas: " + claims
                                                                                                        .size()));

                                                                        int index = 1;

                                                                        for (Claim claim : claims) {
                                                                                BlockPos center = claim.getCenter();

                                                                                ServerPlayer ownerPlayer = source
                                                                                                .getServer()
                                                                                                .getPlayerList()
                                                                                                .getPlayer(claim.getOwner());

                                                                                String ownerName = ownerPlayer != null
                                                                                                ? ownerPlayer.getName()
                                                                                                                .getString()
                                                                                                : claim.getOwner()
                                                                                                                .toString();

                                                                                player.sendSystemMessage(
                                                                                                Component.literal(
                                                                                                                "#" + index
                                                                                                                                + " | "
                                                                                                                                + "Normal"
                                                                                                                                + " | Dono: "
                                                                                                                                + ownerName
                                                                                                                                + " | Dim: "
                                                                                                                                + claim.getDimension()
                                                                                                                                + " | Centro: X "
                                                                                                                                + center.getX()
                                                                                                                                + " Y "
                                                                                                                                + center.getY()
                                                                                                                                + " Z "
                                                                                                                                + center.getZ()));

                                                                                index++;
                                                                        }

                                                                        return 1;
                                                                }))
                                                .then(Commands.literal("remove")
                                                                .then(Commands.argument("numero",
                                                                                IntegerArgumentType.integer(1))
                                                                                .executes(context -> {
                                                                                        CommandSourceStack source = context
                                                                                                        .getSource();
                                                                                        ServerPlayer player = source
                                                                                                        .getPlayerOrException();

                                                                                        if (!source.getServer()
                                                                                                        .getPlayerList()
                                                                                                        .isOp(new NameAndId(
                                                                                                                        player.getGameProfile()))) {
                                                                                                player.sendSystemMessage(
                                                                                                                Component.literal(
                                                                                                                                "Você não possui permissão para usar este comando."));
                                                                                                return 0;
                                                                                        }

                                                                                        int number = IntegerArgumentType
                                                                                                        .getInteger(context,
                                                                                                                        "numero");
                                                                                        List<Claim> claims = ClaimManager
                                                                                                        .getClaims();

                                                                                        if (number > claims.size()) {
                                                                                                player.sendSystemMessage(
                                                                                                                Component.literal(
                                                                                                                                "Claim #" + number
                                                                                                                                                + " não existe."));
                                                                                                return 0;
                                                                                        }

                                                                                        Claim claim = claims.get(
                                                                                                        number - 1);

                                                                                        ClaimManager.removeClaim(claim);

                                                                                        player.sendSystemMessage(
                                                                                                        Component.literal(
                                                                                                                        "Claim #" + number
                                                                                                                                        + " removida com sucesso."));

                                                                                        return 1;
                                                                                })))
                                                .then(Commands.literal("tp")
                                                                .then(Commands.argument("numero",
                                                                                IntegerArgumentType.integer(1))
                                                                                .executes(context -> {
                                                                                        CommandSourceStack source = context
                                                                                                        .getSource();
                                                                                        ServerPlayer player = source
                                                                                                        .getPlayerOrException();

                                                                                        if (!source.getServer()
                                                                                                        .getPlayerList()
                                                                                                        .isOp(new NameAndId(
                                                                                                                        player.getGameProfile()))) {
                                                                                                player.sendSystemMessage(
                                                                                                                Component.literal(
                                                                                                                                "Você não possui permissão para usar este comando."));
                                                                                                return 0;
                                                                                        }

                                                                                        int number = IntegerArgumentType
                                                                                                        .getInteger(context,
                                                                                                                        "numero");
                                                                                        List<Claim> claims = ClaimManager
                                                                                                        .getClaims();

                                                                                        if (number > claims.size()) {
                                                                                                player.sendSystemMessage(
                                                                                                                Component.literal(
                                                                                                                                "Claim #" + number
                                                                                                                                                + " não existe."));
                                                                                                return 0;
                                                                                        }

                                                                                        Claim claim = claims.get(
                                                                                                        number - 1);
                                                                                        BlockPos center = claim
                                                                                                        .getCenter();

                                                                                        if (!claim.getDimension()
                                                                                                        .equals(source.getLevel()
                                                                                                                        .dimension()
                                                                                                                        .toString())) {
                                                                                                player.sendSystemMessage(
                                                                                                                Component.literal(
                                                                                                                                "Esta claim está em outra dimensão: "
                                                                                                                                                + claim.getDimension()));
                                                                                                return 0;
                                                                                        }

                                                                                        player.teleportTo(
                                                                                                        center.getX() + 0.5,
                                                                                                        center.getY(),
                                                                                                        center.getZ() + 0.5);

                                                                                        player.sendSystemMessage(
                                                                                                        Component.literal(
                                                                                                                        "Teleportado para a claim #"
                                                                                                                                        + number
                                                                                                                                        + "."));

                                                                                        return 1;
                                                                                })))

                                                .then(Commands.literal("removehere")
                                                                .executes(context -> {
                                                                        CommandSourceStack source = context.getSource();
                                                                        ServerPlayer player = source
                                                                                        .getPlayerOrException();
                                                                        if (!source.getServer().getPlayerList()
                                                                                        .isOp(new NameAndId(player
                                                                                                        .getGameProfile()))) {
                                                                                player.sendSystemMessage(
                                                                                                Component.literal(
                                                                                                                "Você não possui permissão para usar este comando."));
                                                                                return 0;
                                                                        }
                                                                        String dimension = context.getSource()
                                                                                        .getLevel().dimension()
                                                                                        .toString();

                                                                        Claim claim = ClaimManager.getClaimAt(
                                                                                        player.blockPosition(),
                                                                                        dimension);

                                                                        if (claim == null) {
                                                                                player.sendSystemMessage(
                                                                                                Component.literal(
                                                                                                                "Não existe nenhuma claim nesta posição."));
                                                                                return 0;
                                                                        }

                                                                        ClaimManager.removeClaim(claim);
                                                                        ClaimVisualizationManager.hide(player.getUUID(),
                                                                                        claim);

                                                                        player.sendSystemMessage(
                                                                                        Component.literal(
                                                                                                        "Claim removida pelo admin com sucesso."));
                                                                        return 1;
                                                                })));
        }

        private static void registerColizeuCommand(CommandDispatcher<CommandSourceStack> dispatcher) {

                dispatcher.register(
                                Commands.literal("colizeu")
                                                .executes(context -> {
                                                        CommandSourceStack source = context.getSource();
                                                        ServerPlayer player = source.getPlayerOrException();

                                                        if (!source.getServer().getPlayerList()
                                                                        .isOp(new NameAndId(player.getGameProfile()))) {
                                                                player.sendSystemMessage(
                                                                                Component.literal(
                                                                                                "Você não possui permissão para usar este comando."));
                                                                return 0;
                                                        }

                                                        BlockPos centerPos = player.blockPosition();
                                                        String dimension = source.getLevel().dimension().toString();

                                                        BlockPos pos1 = new BlockPos(
                                                                        centerPos.getX() - 50,
                                                                        centerPos.getY() - 50,
                                                                        centerPos.getZ() - 50);

                                                        BlockPos pos2 = new BlockPos(
                                                                        centerPos.getX() + 50,
                                                                        centerPos.getY() + 50,
                                                                        centerPos.getZ() + 50);

                                                        Claim claim = new Claim(player.getUUID(), pos1, pos2, centerPos,
                                                                        dimension);
                                                        claim.setServerClaim(true);
                                                        claim.setArena(false);

                                                        ClaimManager.addClaim(claim);

                                                        player.sendSystemMessage(
                                                                        Component.literal(
                                                                                        "Colizeu criado com sucesso. Área protegida do servidor: 50 blocos para cada lado, 50 para cima e 50 para baixo."));

                                                        return 1;
                                                }));
        }

        private static void registerArenaCommand(CommandDispatcher<CommandSourceStack> dispatcher) {

                dispatcher.register(
                                Commands.literal("arena")
                                                .executes(context -> {
                                                        CommandSourceStack source = context.getSource();
                                                        ServerPlayer player = source.getPlayerOrException();

                                                        if (!source.getServer().getPlayerList()
                                                                        .isOp(new NameAndId(player.getGameProfile()))) {
                                                                player.sendSystemMessage(
                                                                                Component.literal(
                                                                                                "Você não possui permissão para usar este comando."));
                                                                return 0;
                                                        }

                                                        BlockPos centerPos = player.blockPosition();
                                                        String dimension = source.getLevel().dimension().toString();

                                                        BlockPos pos1 = new BlockPos(
                                                                        centerPos.getX() - 10,
                                                                        centerPos.getY() - 10,
                                                                        centerPos.getZ() - 10);

                                                        BlockPos pos2 = new BlockPos(
                                                                        centerPos.getX() + 10,
                                                                        centerPos.getY() + 10,
                                                                        centerPos.getZ() + 10);

                                                        Claim claim = new Claim(player.getUUID(), pos1, pos2, centerPos,
                                                                        dimension);
                                                        claim.setServerClaim(true);
                                                        claim.setArena(true);

                                                        ClaimManager.addClaim(claim);

                                                        player.sendSystemMessage(
                                                                        Component.literal(
                                                                                        "Arena criada com sucesso. Área de batalha: 10 blocos para cada lado, 10 para cima e 10 para baixo."));

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

        private static void registerColizeuBuilderCommand(CommandDispatcher<CommandSourceStack> dispatcher) {

                dispatcher.register(
                                Commands.literal("colizeubuilder")
                                                .then(Commands.argument("jogador", StringArgumentType.word())
                                                                .executes(context -> {
                                                                        CommandSourceStack source = context.getSource();
                                                                        ServerPlayer player = source
                                                                                        .getPlayerOrException();

                                                                        if (!source.getServer().getPlayerList()
                                                                                        .isOp(new NameAndId(player
                                                                                                        .getGameProfile()))) {
                                                                                player.sendSystemMessage(Component
                                                                                                .literal("Você não possui permissão para usar este comando."));
                                                                                return 0;
                                                                        }

                                                                        String dimension = source.getLevel().dimension()
                                                                                        .toString();
                                                                        Claim claim = ClaimManager.getServerClaimAt(
                                                                                        player.blockPosition(),
                                                                                        dimension);

                                                                        if (claim == null || claim.isArena()) {
                                                                                player.sendSystemMessage(Component
                                                                                                .literal("Você precisa estar dentro de um colizeu."));
                                                                                return 0;
                                                                        }

                                                                        String playerName = StringArgumentType
                                                                                        .getString(context, "jogador");
                                                                        ResolvedPlayer resolved = resolvePlayer(
                                                                                        source.getServer(), playerName);

                                                                        if (resolved == null) {
                                                                                player.sendSystemMessage(Component
                                                                                                .literal("Jogador não encontrado ou offline."));
                                                                                return 0;
                                                                        }

                                                                        claim.addBuilder(resolved.uuid(),
                                                                                        resolved.name());

                                                                        player.sendSystemMessage(Component.literal(
                                                                                        resolved.name() + " agora pode construir neste colizeu."));
                                                                        return 1;
                                                                })));
        }

        private static void registerColizeuUnbuilderCommand(CommandDispatcher<CommandSourceStack> dispatcher) {

                dispatcher.register(
                                Commands.literal("colizeuunbuilder")
                                                .then(Commands.argument("jogador", StringArgumentType.word())
                                                                .executes(context -> {
                                                                        CommandSourceStack source = context.getSource();
                                                                        ServerPlayer player = source
                                                                                        .getPlayerOrException();

                                                                        if (!source.getServer().getPlayerList()
                                                                                        .isOp(new NameAndId(player
                                                                                                        .getGameProfile()))) {
                                                                                player.sendSystemMessage(Component
                                                                                                .literal("Você não possui permissão para usar este comando."));
                                                                                return 0;
                                                                        }

                                                                        String dimension = source.getLevel().dimension()
                                                                                        .toString();
                                                                        Claim claim = ClaimManager.getServerClaimAt(
                                                                                        player.blockPosition(),
                                                                                        dimension);

                                                                        if (claim == null || claim.isArena()) {
                                                                                player.sendSystemMessage(Component
                                                                                                .literal("Você precisa estar dentro de um colizeu."));
                                                                                return 0;
                                                                        }

                                                                        String playerName = StringArgumentType
                                                                                        .getString(context, "jogador");
                                                                        ResolvedPlayer resolved = resolvePlayer(
                                                                                        source.getServer(), playerName);

                                                                        if (resolved == null) {
                                                                                player.sendSystemMessage(Component
                                                                                                .literal("Jogador não encontrado ou offline."));
                                                                                return 0;
                                                                        }

                                                                        claim.removeBuilder(resolved.uuid());

                                                                        player.sendSystemMessage(Component.literal(
                                                                                        resolved.name() + " não pode mais construir neste colizeu."));
                                                                        return 1;
                                                                })));
        }

        private record ResolvedPlayer(UUID uuid, String name) {
        }

        private static boolean overlapsSpawnProtection(BlockPos pos1, BlockPos pos2) {

                int minX = Math.min(pos1.getX(), pos2.getX());
                int maxX = Math.max(pos1.getX(), pos2.getX());

                int minZ = Math.min(pos1.getZ(), pos2.getZ());
                int maxZ = Math.max(pos1.getZ(), pos2.getZ());

                return maxX >= -SPAWN_PROTECTION_RADIUS
                                && minX <= SPAWN_PROTECTION_RADIUS
                                && maxZ >= -SPAWN_PROTECTION_RADIUS
                                && minZ <= SPAWN_PROTECTION_RADIUS;
        }

        private static boolean isNearStructure(Level world, BlockPos pos) {
                return false;
        }
}