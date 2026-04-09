package com.amadeu.landprotection.event;

import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class BlockPlaceHandler {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack heldStack = player.getStackInHand(hand);

            if (!(heldStack.getItem() instanceof BlockItem blockItem)) {
                return ActionResult.PASS;
            }

            Direction side = hitResult.getSide();
            BlockPos placedPos = hitResult.getBlockPos().offset(side);
            Block block = blockItem.getBlock();

            if (!world.isClient()) {
                if (!ClaimManager.canInteract(player.getUuid(), placedPos)) {
                    return ActionResult.FAIL;
                }

                if (ClaimManager.getClaimAt(placedPos) == null) {
                    if (isChestLike(block)) {
                        if (ClaimManager.playerHasClaim(player.getUuid())) {
                            player.sendMessage(Text.literal("Este baú não está numa área protegida."), true);
                        } else {
                            player.sendMessage(Text.literal("Este baú não está numa área protegida, considere criar uma área protegida."), true);
                        }
                    } else if (block instanceof BedBlock) {
                        if (ClaimManager.playerHasClaim(player.getUuid())) {
                            player.sendMessage(Text.literal("Esta cama não está numa área protegida."), true);
                        } else {
                            player.sendMessage(Text.literal("Esta cama não está numa área protegida, considere criar uma área protegida."), true);
                        }
                    } else if (isWorkstation(block)) {
                        String nome = getDisplayName(block);

                        if (ClaimManager.playerHasClaim(player.getUuid())) {
                            player.sendMessage(Text.literal("Esta " + nome + " não está numa área protegida."), true);
                        } else {
                            player.sendMessage(Text.literal("Esta " + nome + " não está numa área protegida, considere criar uma área protegida."), true);
                        }
                    }
                }
            }

            return ActionResult.PASS;
        });
    }

    private static boolean isChestLike(Block block) {
        return block == Blocks.CHEST
                || block == Blocks.TRAPPED_CHEST
                || block == Blocks.BARREL
                || block == Blocks.ENDER_CHEST;
    }

    private static boolean isWorkstation(Block block) {
        return block == Blocks.CRAFTING_TABLE
                || block == Blocks.FURNACE
                || block == Blocks.BLAST_FURNACE
                || block == Blocks.SMOKER
                || block == Blocks.CARTOGRAPHY_TABLE
                || block == Blocks.SMITHING_TABLE
                || block == Blocks.FLETCHING_TABLE
                || block == Blocks.LOOM
                || block == Blocks.STONECUTTER
                || block == Blocks.GRINDSTONE
                || block == Blocks.ENCHANTING_TABLE
                || block == Blocks.ANVIL
                || block == Blocks.CHIPPED_ANVIL
                || block == Blocks.DAMAGED_ANVIL
                || block == Blocks.BREWING_STAND
                || block == Blocks.CAULDRON;
    }

    private static String getDisplayName(Block block) {
        if (block == Blocks.CRAFTING_TABLE) return "bancada de trabalho";
        if (block == Blocks.FURNACE) return "fornalha";
        if (block == Blocks.BLAST_FURNACE) return "alto-forno";
        if (block == Blocks.SMOKER) return "defumador";
        if (block == Blocks.CARTOGRAPHY_TABLE) return "mesa de cartografia";
        if (block == Blocks.SMITHING_TABLE) return "mesa de ferraria";
        if (block == Blocks.FLETCHING_TABLE) return "mesa de flechas";
        if (block == Blocks.LOOM) return "tear";
        if (block == Blocks.STONECUTTER) return "cortador de pedra";
        if (block == Blocks.GRINDSTONE) return "mó";
        if (block == Blocks.ENCHANTING_TABLE) return "mesa de encantamentos";
        if (block == Blocks.ANVIL) return "bigorna";
        if (block == Blocks.CHIPPED_ANVIL) return "bigorna lascada";
        if (block == Blocks.DAMAGED_ANVIL) return "bigorna danificada";
        if (block == Blocks.BREWING_STAND) return "suporte de poções";
        if (block == Blocks.CAULDRON) return "caldeirão";
        return "bancada";
    }
}