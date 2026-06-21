package com.amadeu.landprotection.event;

import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class EntityProtectionHandler {

    public static void register() {

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {

            if (!isProtectedEntity(entity)) {
                return InteractionResult.PASS;
            }

            String dimension = world.dimension().toString();

            if (player instanceof ServerPlayer serverPlayer
                    && ClaimManager.canBuild(serverPlayer, entity.blockPosition(), dimension)) {
                return InteractionResult.PASS;
            }

            sendProtectedMessage(player, world, entity);
            return InteractionResult.FAIL;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {

            if (!isProtectedEntity(entity)) {
                return InteractionResult.PASS;
            }

            String dimension = world.dimension().toString();

            if (player instanceof ServerPlayer serverPlayer
                    && ClaimManager.canBuild(serverPlayer, entity.blockPosition(), dimension)) {
                return InteractionResult.PASS;
            }

            sendProtectedMessage(player, world, entity);
            return InteractionResult.FAIL;
        });
    }

    private static boolean isProtectedEntity(Entity entity) {
        return entity instanceof ItemFrame
                || entity instanceof GlowItemFrame
                || entity instanceof ArmorStand
                || entity instanceof Animal;
    }

    private static void sendProtectedMessage(Player player, Level world, Entity entity) {

        String ownerName = "outro jogador";

        String dimension = world.dimension().toString();
        Claim claim = ClaimManager.getClaimAt(entity.blockPosition(), dimension);

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
                Component.literal("Esta entidade pertence à área de " + ownerName + "."));
    }
}
