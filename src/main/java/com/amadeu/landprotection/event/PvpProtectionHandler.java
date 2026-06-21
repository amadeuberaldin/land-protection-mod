package com.amadeu.landprotection.event;

import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

public class PvpProtectionHandler {

    public static void register() {

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {

            if (!(entity instanceof ServerPlayer victim)) {
                return true;
            }

            Entity attacker = source.getEntity();

            if (attacker instanceof Projectile projectile) {
                attacker = projectile.getOwner();
            }

            if (!(attacker instanceof ServerPlayer attackerPlayer)) {
                return true;
            }

            String victimDimension = victim.level().dimension().toString();
            String attackerDimension = attackerPlayer.level().dimension().toString();

            Claim victimArena = ClaimManager.getArenaAt(victim.blockPosition(), victimDimension);
            Claim attackerArena = ClaimManager.getArenaAt(attackerPlayer.blockPosition(), attackerDimension);

            if (victimArena != null && attackerArena != null) {
                return true;
            }

            Claim victimClaim = ClaimManager.getClaimAt(victim.blockPosition(), victimDimension);
            Claim attackerClaim = ClaimManager.getClaimAt(attackerPlayer.blockPosition(), attackerDimension);

            if (isNormalPlayerClaim(victimClaim) || isNormalPlayerClaim(attackerClaim)) {
                attackerPlayer.sendSystemMessage(
                        Component.literal("PVP está desativado em claims de jogadores."));

                return false;
            }

            return true;
        });
    }

    private static boolean isNormalPlayerClaim(Claim claim) {
        return claim != null
                && !claim.isServerClaim()
                && !claim.isArena();
    }
}