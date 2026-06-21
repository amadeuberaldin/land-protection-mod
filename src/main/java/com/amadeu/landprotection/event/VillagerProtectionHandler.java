package com.amadeu.landprotection.event;

import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.Projectile;

public class VillagerProtectionHandler {

    public static void register() {

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {

            if (!(entity instanceof Villager villager)) {
                return InteractionResult.PASS;
            }

            String dimension = world.dimension().toString();
            Claim claim = ClaimManager.getClaimAt(villager.blockPosition(), dimension);

            if (!isNormalPlayerClaim(claim)) {
                return InteractionResult.PASS;
            }

            if (claim.canInteract(player.getUUID())) {
                return InteractionResult.PASS;
            }

            player.sendSystemMessage(
                    Component.literal("Este villager está protegido nesta área privada."));
            return InteractionResult.FAIL;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {

            if (!(entity instanceof Villager villager)) {
                return InteractionResult.PASS;
            }

            String dimension = world.dimension().toString();
            Claim claim = ClaimManager.getClaimAt(villager.blockPosition(), dimension);

            if (!isNormalPlayerClaim(claim)) {
                return InteractionResult.PASS;
            }

            player.sendSystemMessage(
                    Component.literal("Villagers protegidos por claim não podem receber dano."));
            return InteractionResult.FAIL;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {

            if (!(entity instanceof Villager villager)) {
                return true;
            }

            String dimension = villager.level().dimension().toString();
            Claim claim = ClaimManager.getClaimAt(villager.blockPosition(), dimension);

            if (!isNormalPlayerClaim(claim)) {
                return true;
            }

            /*
             * Regra:
             *
             * Villager dentro de claim normal não recebe dano.
             * Dono, trust e guest podem negociar, mas ninguém pode matar.
             *
             * Isso protege contra:
             * - espada
             * - soco
             * - flecha
             * - tridente
             * - poção
             * - dano indireto
             * - mobs
             * - fogo/lava/acidentes
             */
            Entity attacker = source.getEntity();

            if (attacker instanceof Projectile projectile) {
                attacker = projectile.getOwner();
            }

            return false;
        });
    }

    private static boolean isNormalPlayerClaim(Claim claim) {
        return claim != null
                && !claim.isServerClaim()
                && !claim.isArena();
    }
}
