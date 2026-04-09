package com.amadeu.landprotection.visual;

import com.amadeu.landprotection.claim.BaseClaim;
import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class BaseVisualizationManager {

    private static final Map<UUID, ActiveVisualization> activeVisualizations = new HashMap<>();

    private static final int OUT_OF_BASE_TIMEOUT_TICKS = 20 * 30;
    private static final int PARTICLE_INTERVAL_TICKS = 10;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Map.Entry<UUID, ActiveVisualization>> iterator = activeVisualizations.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<UUID, ActiveVisualization> entry = iterator.next();
                UUID playerUuid = entry.getKey();
                ActiveVisualization active = entry.getValue();

                ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);

                if (player == null) {
                    iterator.remove();
                    continue;
                }

                BaseClaim base = ClaimManager.getBaseByMember(playerUuid);

                if (base == null) {
                    iterator.remove();
                    continue;
                }

                if (base.contains(player.blockPosition())) {
                    active.ticksOutsideBase = 0;
                } else {
                    active.ticksOutsideBase++;

                    if (active.ticksOutsideBase > OUT_OF_BASE_TIMEOUT_TICKS) {
                        player.sendSystemMessage(
                                net.minecraft.network.chat.Component.literal(
                                        "Visualização da base desativada por você ficar muito tempo fora dela."),
                                false);
                        iterator.remove();
                        continue;
                    }
                }

                active.ticksUntilNextRender--;

                if (active.ticksUntilNextRender <= 0) {player.level();
                    net.minecraft.world.level.Level playerWorld = player.level();

                    if (playerWorld instanceof net.minecraft.server.level.ServerLevel serverWorld) {
                        // Reaproveita o mesmo visualizador usando os limites da base
                        com.amadeu.landprotection.claim.Claim fakeClaim = new com.amadeu.landprotection.claim.Claim(
                                base.getLeader(),
                                base.getPos1(),
                                base.getPos2(),
                                base.getCenter());

                        ClaimVisualization.showClaimBounds(player, serverWorld, fakeClaim);
                    }

                    active.ticksUntilNextRender = PARTICLE_INTERVAL_TICKS;
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            activeVisualizations.remove(handler.player.getUUID());
        });
    }

    public static boolean isShowing(UUID playerUuid) {
        return activeVisualizations.containsKey(playerUuid);
    }

    public static void show(UUID playerUuid) {
        ActiveVisualization active = new ActiveVisualization();
        active.ticksUntilNextRender = 1; // força render imediato
        activeVisualizations.put(playerUuid, active);
    }

    public static void hide(UUID playerUuid) {
        activeVisualizations.remove(playerUuid);
    }

    private static class ActiveVisualization {
        private int ticksOutsideBase = 0;
        private int ticksUntilNextRender = 0;
    }
}