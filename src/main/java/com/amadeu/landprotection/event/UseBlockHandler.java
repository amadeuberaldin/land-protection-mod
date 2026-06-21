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

            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            String dimension = world.dimension().toString();

            Claim serverClaim = ClaimManager.getServerClaimAt(
                    hitResult.getBlockPos(),
                    dimension);

            if (serverClaim != null) {
                return InteractionResult.PASS;
            }

            Claim claim = ClaimManager.getClaimAt(
                    hitResult.getBlockPos(),
                    dimension);

            if (claim != null
                    && !claim.canInteract(serverPlayer.getUUID())) {

                String ownerName = "outro jogador";

                if (!world.isClientSide()) {
                    var server = world.getServer();

                    if (server != null) {
                        ServerPlayer owner = server.getPlayerList()
                                .getPlayer(claim.getOwner());

                        if (owner != null) {
                            ownerName = owner.getName().getString();
                        }
                    }
                }

                player.sendSystemMessage(
                        Component.literal(
                                "Esta área pertence a "
                                        + ownerName
                                        + ", peça autorização para utilizar."));

                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });
    }
}
