package com.amadeu.landprotection.event;

import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import com.amadeu.landprotection.dragon.DragonEggSystem;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;

public class BlockPlaceHandler {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

            ItemStack heldStack = player.getItemInHand(hand);

            if (!(heldStack.getItem() instanceof BlockItem)) {
                return InteractionResult.PASS;
            }

            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            Direction side = hitResult.getDirection();
            BlockPos placedPos = hitResult.getBlockPos().relative(side);

            if (!world.isClientSide()) {
                String dimension = world.dimension().toString();

                BlockPos clickedPos = hitResult.getBlockPos();
                var clickedBlock = world.getBlockState(clickedPos).getBlock();

                Claim serverClaim = ClaimManager.getServerClaimAt(clickedPos, dimension);

                if (serverClaim != null && (clickedBlock == Blocks.ENDER_CHEST || clickedBlock instanceof BedBlock)) {
                    return InteractionResult.PASS;
                }

                Claim arena = ClaimManager.getArenaAt(placedPos, dimension);

                if (arena != null) {
                    return InteractionResult.PASS;
                }

                Claim claimAtPlace = ClaimManager.getClaimAt(placedPos, dimension);

                if (heldStack.is(Blocks.DRAGON_EGG.asItem())
                        && isNormalPlayerClaim(claimAtPlace)) {
                    serverPlayer.sendSystemMessage(
                            Component.literal("O Ovo do Dragão não pode ser colocado dentro de claims."));
                    return InteractionResult.FAIL;
                }

                if (!ClaimManager.canBuild(serverPlayer, placedPos, dimension)) {
                    return InteractionResult.FAIL;
                }

                if (heldStack.is(Blocks.DRAGON_EGG.asItem())
                        && world instanceof ServerLevel serverLevel) {
                    DragonEggSystem.onDragonEggPlaced(
                            serverPlayer.level().getServer(),
                            serverLevel,
                            placedPos);
                }
            }

            return InteractionResult.PASS;
        });
    }

    private static boolean isNormalPlayerClaim(Claim claim) {
        return claim != null
                && !claim.isServerClaim()
                && !claim.isArena();
    }
}
