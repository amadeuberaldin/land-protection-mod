package com.amadeu.landprotection.event;

import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class FluidProtectionHandler {

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            var stack = player.getStackInHand(hand);

            if (stack.getItem() != Items.LAVA_BUCKET && stack.getItem() != Items.WATER_BUCKET) {
                return ActionResult.PASS;
            }

            HitResult hit = player.raycast(5.0, 0.0F, false);
            if (!(hit instanceof BlockHitResult blockHit)) {
                return ActionResult.PASS;
            }

            BlockPos targetPos = blockHit.getBlockPos().offset(blockHit.getSide());

            if (!ClaimManager.canInteract(player.getUuid(), targetPos)) {
                player.sendMessage(Text.literal("Você não pode despejar líquidos nesta área protegida."), true);
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }
}