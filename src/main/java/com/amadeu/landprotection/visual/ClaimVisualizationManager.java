package com.amadeu.landprotection.visual;

import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ClaimVisualizationManager {

    private static final Map<UUID, ActiveVisualization> activeVisualizations = new HashMap<>();

    private static final int OUT_OF_CLAIM_TIMEOUT_TICKS = 20 * 30; // 30 segundos
    private static final int PARTICLE_INTERVAL_TICKS = 10; // redesenha a cada 10 ticks

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Map.Entry<UUID, ActiveVisualization>> iterator = activeVisualizations.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<UUID, ActiveVisualization> entry = iterator.next();
                UUID playerUuid = entry.getKey();
                ActiveVisualization active = entry.getValue();

                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);

                // remove ao deslogar
                if (player == null) {
                    iterator.remove();
                    continue;
                }

                Claim claim = ClaimManager.getClaimByPlayer(playerUuid);

                // remove se o player não tiver mais claim
                if (claim == null) {
                    iterator.remove();
                    continue;
                }

                // conta tempo fora da claim
                if (claim.contains(player.getBlockPos())) {
                    active.ticksOutsideClaim = 0;
                } else {
                    active.ticksOutsideClaim++;

                    if (active.ticksOutsideClaim > OUT_OF_CLAIM_TIMEOUT_TICKS) {
                        player.sendMessage(net.minecraft.text.Text.literal(
                                "Visualização da área protegida desativada por você ficar muito tempo fora dela."),
                                false);
                        iterator.remove();
                        continue;
                    }
                }

                active.ticksUntilNextRender--;

                if (active.ticksUntilNextRender <= 0) {
                    net.minecraft.world.World playerWorld = player.getEntityWorld();

                    if (playerWorld instanceof net.minecraft.server.world.ServerWorld serverWorld) {
                        ClaimVisualization.showClaimBounds(player, serverWorld, claim);
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
        activeVisualizations.put(playerUuid, new ActiveVisualization());
    }

    public static void hide(UUID playerUuid) {
        activeVisualizations.remove(playerUuid);
    }

    private static class ActiveVisualization {
        private int ticksOutsideClaim = 0;
        private int ticksUntilNextRender = 0;
    }
}