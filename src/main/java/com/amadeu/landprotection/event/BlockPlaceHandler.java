package com.amadeu.landprotection.event;

import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class BlockPlaceHandler {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

            ItemStack heldStack = player.getItemInHand(hand);

            if (!(heldStack.getItem() instanceof BlockItem)) {
                return InteractionResult.PASS;
            }

            Direction side = hitResult.getDirection();
            BlockPos placedPos = hitResult.getBlockPos().relative(side);

            if (!world.isClientSide()) {
                String dimension = world.dimension().toString();

                if (!ClaimManager.canInteract(player.getUUID(), placedPos, dimension)) {
                    return InteractionResult.FAIL;
                }
            }

            return InteractionResult.PASS;
        });
    }
}
