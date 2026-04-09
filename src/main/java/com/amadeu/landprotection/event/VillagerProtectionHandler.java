package com.amadeu.landprotection.event;

import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public class VillagerProtectionHandler {

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(entity instanceof VillagerEntity villager)) {
                return ActionResult.PASS;
            }

            if (!ClaimManager.canInteract(player.getUuid(), villager.getBlockPos())) {
                player.sendMessage(Text.literal("Este villager está protegido nesta área privada."), true);
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }
}