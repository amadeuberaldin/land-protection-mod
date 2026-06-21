package com.amadeu.landprotection.dragon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class DragonEggSystem {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static EggState state = new EggState();
    private static int scanCooldownTicks = 0;

    private DragonEggSystem() {
    }

    public static void register() {

        ServerLifecycleEvents.SERVER_STARTED.register(DragonEggSystem::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(DragonEggSystem::save);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;

            if (isMarkedOldCarrier(player)) {
                int removed = removeDragonEggs(player);

                state.oldCarrierUuid = null;
                state.oldCarrierName = null;
                save(server);

                if (removed > 0) {
                    player.sendSystemMessage(Component.literal(
                            "§5O antigo Ovo do Dragão perdeu seu poder e desapareceu do seu inventário."));

                    broadcast(server,
                            "§5✦ O antigo Ovo do Dragão se desfez quando §d"
                                    + player.getName().getString()
                                    + " §5retornou ao mundo.");
                }
                return;
            }

            if (playerHasEgg(player)) {
                setCarrier(player);
                save(server);

                broadcast(server,
                        "§5✦ O Portador do Ovo do Dragão despertou! §d"
                                + player.getName().getString()
                                + " §5está online carregando o artefato mais raro do MundoZ.");
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            scanCooldownTicks++;

            if (scanCooldownTicks < 100) {
                return;
            }

            scanCooldownTicks = 0;
            scanOnlinePlayers(server);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {

            if (!(entity instanceof EnderDragon dragon)) {
                return;
            }

            MinecraftServer server = dragon.level().getServer();

            if (server == null || !state.awaitingNextDragonKill) {
                return;
            }

            ServerLevel level = (ServerLevel) dragon.level();

            ItemEntity itemEntity = new ItemEntity(
                    level,
                    dragon.getX(),
                    dragon.getY(),
                    dragon.getZ(),
                    new ItemStack(Blocks.DRAGON_EGG.asItem()));

            itemEntity.setUnlimitedLifetime();
            level.addFreshEntity(itemEntity);

            state.awaitingNextDragonKill = false;
            state.status = "DROPPED";
            state.carrierUuid = null;
            state.carrierName = null;
            state.dimension = level.dimension().identifier().toString();
            state.x = dragon.blockPosition().getX();
            state.y = dragon.blockPosition().getY();
            state.z = dragon.blockPosition().getZ();

            save(server);

            broadcast(server,
                    "§5✦ O chamado foi atendido! §dO Ovo do Dragão retornou ao mundo após a queda do dragão.");
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher);
        });
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("dragoneggloc")
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            MinecraftServer server = source.getServer();

                            scanOnlinePlayers(server);
                            sendLocation(source, server);

                            return 1;
                        }));

        dispatcher.register(
                Commands.literal("dragonegg")
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    MinecraftServer server = source.getServer();

                                    scanOnlinePlayers(server);
                                    sendStatus(source, server);

                                    return 1;
                                }))
                        .then(Commands.literal("reset")
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerPlayer player = source.getPlayerOrException();

                                    if (!source.getServer().getPlayerList()
                                            .isOp(new NameAndId(player.getGameProfile()))) {
                                        player.sendSystemMessage(
                                                Component.literal("Você não possui permissão para usar este comando."));
                                        return 0;
                                    }

                                    MinecraftServer server = source.getServer();

                                    markCurrentCarrierForRemoval(server);

                                    state.awaitingNextDragonKill = true;
                                    state.status = "WAITING_DRAGON";
                                    state.carrierUuid = null;
                                    state.carrierName = null;
                                    state.dimension = null;
                                    state.x = 0;
                                    state.y = 0;
                                    state.z = 0;

                                    save(server);

                                    broadcast(server,
                                            "§5✦ O Chamado do Dragão ecoou pelo servidor! §dO próximo dragão morto irá revelar um novo Ovo do Dragão.");

                                    return 1;
                                })));
    }

    public static void onDragonEggPlaced(MinecraftServer server, ServerLevel level, BlockPos pos) {
        state.awaitingNextDragonKill = false;
        state.status = "PLACED";
        state.carrierUuid = null;
        state.carrierName = null;
        state.dimension = level.dimension().identifier().toString();
        state.x = pos.getX();
        state.y = pos.getY();
        state.z = pos.getZ();

        save(server);
    }

    public static void onDragonEggBroken(MinecraftServer server, ServerLevel level, BlockPos pos) {
        state.status = "UNKNOWN";
        state.dimension = level.dimension().identifier().toString();
        state.x = pos.getX();
        state.y = pos.getY();
        state.z = pos.getZ();

        save(server);
    }

    private static void sendLocation(CommandSourceStack source, MinecraftServer server) {
        if ("CARRIER".equals(state.status) && state.carrierName != null) {
            ServerPlayer carrier = getOnlineCarrier(server);

            if (carrier != null) {
                source.sendSuccess(
                        () -> Component.literal("§5O Ovo do Dragão está com §d"
                                + state.carrierName
                                + "§5, que está online."),
                        false);
            } else {
                source.sendSuccess(
                        () -> Component.literal("§5O Ovo do Dragão está com §d"
                                + state.carrierName
                                + "§5, mas o portador está offline."),
                        false);
            }
            return;
        }

        if ("PLACED".equals(state.status) || "DROPPED".equals(state.status)) {
            source.sendSuccess(
                    () -> Component.literal("§5Última localização conhecida do Ovo do Dragão: §d"
                            + state.x + " " + state.y + " " + state.z
                            + " §5em §d" + state.dimension),
                    false);
            return;
        }

        if (state.awaitingNextDragonKill) {
            source.sendSuccess(
                    () -> Component.literal("§5O Ovo do Dragão ainda não voltou. §dO próximo dragão morto irá dropar o ovo."),
                    false);
            return;
        }

        source.sendSuccess(
                () -> Component.literal("§5A localização do Ovo do Dragão é desconhecida."),
                false);
    }

    private static void sendStatus(CommandSourceStack source, MinecraftServer server) {
        source.sendSuccess(
                () -> Component.literal("§5Status do Ovo do Dragão: §d" + state.status),
                false);

        source.sendSuccess(
                () -> Component.literal("§5Aguardando próximo dragão: §d" + state.awaitingNextDragonKill),
                false);

        if (state.carrierName != null) {
            ServerPlayer carrier = getOnlineCarrier(server);

            source.sendSuccess(
                    () -> Component.literal("§5Portador atual conhecido: §d"
                            + state.carrierName
                            + (carrier != null ? " §5(online)" : " §5(offline)")),
                    false);
        }

        if (state.oldCarrierName != null) {
            source.sendSuccess(
                    () -> Component.literal("§5Ovo antigo marcado para remoção quando logar: §d"
                            + state.oldCarrierName),
                    false);
        }

        if (state.dimension != null) {
            source.sendSuccess(
                    () -> Component.literal("§5Última posição conhecida: §d"
                            + state.x + " " + state.y + " " + state.z
                            + " §5em §d" + state.dimension),
                    false);
        }
    }

    private static void markCurrentCarrierForRemoval(MinecraftServer server) {
        scanOnlinePlayers(server);

        if (!"CARRIER".equals(state.status) || state.carrierUuid == null) {
            return;
        }

        ServerPlayer onlineCarrier = getOnlineCarrier(server);

        state.oldCarrierUuid = state.carrierUuid;
        state.oldCarrierName = state.carrierName;

        if (onlineCarrier != null) {
            int removed = removeDragonEggs(onlineCarrier);

            if (removed > 0) {
                onlineCarrier.sendSystemMessage(Component.literal(
                        "§5O antigo Ovo do Dragão perdeu seu poder e desapareceu do seu inventário."));
            }

            state.oldCarrierUuid = null;
            state.oldCarrierName = null;
        }
    }

    private static boolean isMarkedOldCarrier(ServerPlayer player) {
        return state.oldCarrierUuid != null
                && state.oldCarrierUuid.equals(player.getUUID().toString());
    }

    private static void scanOnlinePlayers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (playerHasEgg(player)) {
                setCarrier(player);
                save(server);
                return;
            }
        }
    }

    private static void setCarrier(ServerPlayer player) {
        state.awaitingNextDragonKill = false;
        state.status = "CARRIER";
        state.carrierUuid = player.getUUID().toString();
        state.carrierName = player.getName().getString();
        state.dimension = player.level().dimension().identifier().toString();
        state.x = player.blockPosition().getX();
        state.y = player.blockPosition().getY();
        state.z = player.blockPosition().getZ();
    }

    private static ServerPlayer getOnlineCarrier(MinecraftServer server) {
        if (state.carrierUuid == null) {
            return null;
        }

        return server.getPlayerList().getPlayer(UUID.fromString(state.carrierUuid));
    }

    private static boolean playerHasEgg(ServerPlayer player) {
        return countDragonEggs(player) > 0;
    }

    private static int removeDragonEggs(ServerPlayer player) {
        int removed = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (stack.is(Blocks.DRAGON_EGG.asItem())) {
                removed += stack.getCount();
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }

        return removed;
    }

    private static int countDragonEggs(ServerPlayer player) {
        int total = 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (stack.is(Blocks.DRAGON_EGG.asItem())) {
                total += stack.getCount();
            }
        }

        return total;
    }

    private static void broadcast(MinecraftServer server, String message) {
        server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
    }

    private static Path getFile(MinecraftServer server) {
        return server.getServerDirectory()
                .resolve("world")
                .resolve("landprotection")
                .resolve("dragon_egg.json");
    }

    private static void load(MinecraftServer server) {
        Path file = getFile(server);

        if (!Files.exists(file)) {
            state = new EggState();
            return;
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            EggState loaded = GSON.fromJson(reader, EggState.class);
            state = loaded != null ? loaded : new EggState();
        } catch (Exception e) {
            state = new EggState();
            System.err.println("[landprotection] Erro ao carregar dragon_egg.json: " + e.getMessage());
        }
    }

    private static void save(MinecraftServer server) {
        Path file = getFile(server);

        try {
            Files.createDirectories(file.getParent());

            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(state, writer);
            }
        } catch (Exception e) {
            System.err.println("[landprotection] Erro ao salvar dragon_egg.json: " + e.getMessage());
        }
    }

    private static class EggState {
        private boolean awaitingNextDragonKill = false;
        private String status = "UNKNOWN";
        private String carrierUuid;
        private String carrierName;
        private String oldCarrierUuid;
        private String oldCarrierName;
        private String dimension;
        private int x;
        private int y;
        private int z;
    }
}
