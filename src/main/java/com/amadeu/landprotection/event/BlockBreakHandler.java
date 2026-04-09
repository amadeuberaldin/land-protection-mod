package com.amadeu.landprotection.event;

import com.amadeu.landprotection.claim.BaseClaim;
import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public class BlockBreakHandler {

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (ClaimManager.canInteract(player.getUUID(), pos)) {
                return true;
            }

            String ownerName = "outro jogador";

            Claim claim = ClaimManager.getClaimAt(pos);
            if (claim != null) {
                ServerPlayer owner = world.getServer().getPlayerList().getPlayer(claim.getOwner());
                if (owner != null) {
                    ownerName = owner.getName().getString();
                }

                player.sendSystemMessage(
                        Component.literal("Esta área pertence a " + ownerName + ", peça autorização para utilizar."));
                return false;
            }

            BaseClaim base = ClaimManager.getBaseAt(pos);
            if (base != null) {
                ServerPlayer leader = world.getServer().getPlayerList().getPlayer(base.getLeader());
                if (leader != null) {
                    ownerName = leader.getName().getString();
                }

                player.sendSystemMessage(
                        Component.literal("Esta base pertence ao grupo liderado por " + ownerName + "."));
                return false;
            }

            return true;
        });
    }
}