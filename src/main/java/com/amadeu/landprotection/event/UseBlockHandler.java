package com.amadeu.landprotection.event;

import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public class UseBlockHandler {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

            String dimension = world.dimension().toString();

            if (!ClaimManager.canInteract(player.getUUID(), hitResult.getBlockPos(), dimension)) {

                String ownerName = "outro jogador";

                Claim claim = ClaimManager.getClaimAt(hitResult.getBlockPos(), dimension);
                if (claim != null && !world.isClientSide()) {
                    var server = world.getServer();
                    if (server != null) {
                        ServerPlayer owner = server.getPlayerList().getPlayer(claim.getOwner());
                        if (owner != null) {
                            ownerName = owner.getName().getString();
                        }
                    }
                }

                player.sendSystemMessage(
                        Component.literal("Esta área pertence a " + ownerName + ", peça autorização para utilizar.")
                );

                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });
    }
}
