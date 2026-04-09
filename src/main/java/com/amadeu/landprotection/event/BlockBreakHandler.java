package com.amadeu.landprotection.event;

import com.amadeu.landprotection.claim.BaseClaim;
import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class BlockBreakHandler {

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (ClaimManager.canInteract(player.getUuid(), pos)) {
                return true;
            }

            String ownerName = "outro jogador";

            Claim claim = ClaimManager.getClaimAt(pos);
            if (claim != null) {
                ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(claim.getOwner());
                if (owner != null) {
                    ownerName = owner.getName().getString();
                }

                player.sendMessage(
                        Text.literal("Esta área pertence a " + ownerName + ", peça autorização para utilizar."),
                        true
                );
                return false;
            }

            BaseClaim base = ClaimManager.getBaseAt(pos);
            if (base != null) {
                ServerPlayerEntity leader = world.getServer().getPlayerManager().getPlayer(base.getLeader());
                if (leader != null) {
                    ownerName = leader.getName().getString();
                }

                player.sendMessage(
                        Text.literal("Esta base pertence ao grupo liderado por " + ownerName + "."),
                        true
                );
                return false;
            }

            return true;
        });
    }
}