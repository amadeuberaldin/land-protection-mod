package com.amadeu.landprotection.event;

import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;

public class FluidProtectionHandler {

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            var stack = player.getItemInHand(hand);

            if (stack.getItem() != Items.LAVA_BUCKET && stack.getItem() != Items.WATER_BUCKET) {
                return InteractionResult.PASS;
            }

            HitResult hit = player.pick(5.0, 0.0F, false);
            if (!(hit instanceof BlockHitResult blockHit)) {
                return InteractionResult.PASS;
            }

            BlockPos targetPos = blockHit.getBlockPos().relative(blockHit.getDirection());

            if (!ClaimManager.canInteract(player.getUUID(), targetPos)) {
                player.sendSystemMessage(Component.literal("Você não pode despejar líquidos nesta área protegida."));
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });
    }
}