package com.amadeu.landprotection.visual;

import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ClaimVisualizationManager {

    private static final Map<UUID, ActiveVisualization> activeVisualizations = new HashMap<>();

    private static final int OUT_OF_CLAIM_TIMEOUT_TICKS = 20 * 30;
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

                Claim claim = ClaimManager.getClaimByPlayer(playerUuid);

                if (claim == null) {
                    iterator.remove();
                    continue;
                }

                if (claim.contains(player.blockPosition())) {
                    active.ticksOutsideClaim = 0;
                } else {
                    active.ticksOutsideClaim++;

                    if (active.ticksOutsideClaim > OUT_OF_CLAIM_TIMEOUT_TICKS) {
                        player.sendSystemMessage(Component.literal(
                                "Visualização da área protegida desativada por você ficar muito tempo fora dela."));
                        iterator.remove();
                        continue;
                    }
                }

                active.ticksUntilNextRender--;

                if (active.ticksUntilNextRender <= 0) {
                    Level playerWorld = player.level();

                    if (playerWorld instanceof ServerLevel serverWorld) {
                        ClaimVisualization.showClaimBounds(player, serverWorld, claim);
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
