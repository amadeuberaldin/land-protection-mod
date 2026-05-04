package com.amadeu.landprotection.event;

import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class BlockBreakHandler {

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {

            String dimension = world.dimension().toString();

            if (ClaimManager.canInteract(player.getUUID(), pos, dimension)) {
                return true;
            }

            String ownerName = "outro jogador";

            Claim claim = ClaimManager.getClaimAt(pos, dimension);
            if (claim != null) {
                ServerPlayer owner = world.getServer().getPlayerList().getPlayer(claim.getOwner());
                if (owner != null) {
                    ownerName = owner.getName().getString();
                }

                player.sendSystemMessage(
                        Component.literal("Esta área pertence a " + ownerName + ", peça autorização para utilizar.")
                );
                return false;
            }

            return true;
        });
    }
}
