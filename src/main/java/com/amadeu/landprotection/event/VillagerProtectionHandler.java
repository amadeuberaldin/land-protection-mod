package com.amadeu.landprotection.event;

import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;

public class VillagerProtectionHandler {

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(entity instanceof Villager villager)) {
                return InteractionResult.PASS;
            }

            if (!ClaimManager.canInteract(player.getUUID(), villager.blockPosition())) {
                player.sendSystemMessage(Component.literal("Este villager está protegido nesta área privada."));
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });
    }
}