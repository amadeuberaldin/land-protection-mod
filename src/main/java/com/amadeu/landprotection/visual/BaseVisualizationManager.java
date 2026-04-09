package com.amadeu.landprotection.visual;

import com.amadeu.landprotection.claim.BaseClaim;
import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

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

                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);

                if (player == null) {
                    iterator.remove();
                    continue;
                }

                BaseClaim base = ClaimManager.getBaseByMember(playerUuid);

                if (base == null) {
                    iterator.remove();
                    continue;
                }

                if (base.contains(player.getBlockPos())) {
                    active.ticksOutsideBase = 0;
                } else {
                    active.ticksOutsideBase++;

                    if (active.ticksOutsideBase > OUT_OF_BASE_TIMEOUT_TICKS) {
                        player.sendMessage(
                                net.minecraft.text.Text.literal(
                                        "Visualização da base desativada por você ficar muito tempo fora dela."),
                                false);
                        iterator.remove();
                        continue;
                    }
                }

                active.ticksUntilNextRender--;

                if (active.ticksUntilNextRender <= 0) {
                    net.minecraft.world.World playerWorld = player.getEntityWorld();

                    if (playerWorld instanceof net.minecraft.server.world.ServerWorld serverWorld) {
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
            activeVisualizations.remove(handler.player.getUuid());
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