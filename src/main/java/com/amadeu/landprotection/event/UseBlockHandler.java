package com.amadeu.landprotection.event;

import com.amadeu.landprotection.claim.BaseClaim;
import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.level.block.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;

public class UseBlockHandler {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            Block block = world.getBlockState(hitResult.getBlockPos()).getBlock();

            if (!ClaimManager.canInteract(player.getUUID(), hitResult.getBlockPos())) {
                String ownerName = "outro jogador";

                Claim claim = ClaimManager.getClaimAt(hitResult.getBlockPos());
                if (claim != null) {
                    if (!world.isClientSide()) {
                        var server = world.getServer();
                        if (server != null) {
                            ServerPlayer owner = server.getPlayerList().getPlayer(claim.getOwner());
                            if (owner != null) {
                                ownerName = owner.getName().getString();
                            }
                        }
                    }

                    if (isDoorLike(block)) {
                        player.sendSystemMessage(
                                Component.literal("Esta porta ou mecanismo pertence à área de " + ownerName + ".")
                        );
                    } else {
                        player.sendSystemMessage(
                                Component.literal("Esta área pertence a " + ownerName + ", peça autorização para utilizar.")
                        );
                    }

                    return InteractionResult.FAIL;
                }

                BaseClaim base = ClaimManager.getBaseAt(hitResult.getBlockPos());
                if (base != null) {
                    if (!world.isClientSide()) {
                        var server = world.getServer();
                        if (server != null) {
                            ServerPlayer leader = server.getPlayerList().getPlayer(base.getLeader());
                            if (leader != null) {
                                ownerName = leader.getName().getString();
                            }
                        }
                    }

                    if (isDoorLike(block)) {
                        player.sendSystemMessage(
                                Component.literal("Esta porta ou mecanismo pertence à base liderada por " + ownerName + ".")
                        );
                    } else {
                        player.sendSystemMessage(
                                Component.literal("Esta base pertence ao grupo liderado por " + ownerName + ".")
                        );
                    }

                    return InteractionResult.FAIL;
                }
            }

            Claim claim = ClaimManager.getClaimAt(hitResult.getBlockPos());
            BaseClaim base = ClaimManager.getBaseAt(hitResult.getBlockPos());

            if (claim == null && base == null) {
                if (isChestLike(block)) {
                    if (ClaimManager.playerHasClaim(player.getUUID()) || ClaimManager.playerHasBase(player.getUUID())) {
                        player.sendSystemMessage(Component.literal("Este baú não está numa área protegida."));
                    } else {
                        player.sendSystemMessage(
                                Component.literal("Este baú não está numa área protegida, considere criar uma área protegida.")
                        );
                    }
                } else if (block instanceof BedBlock) {
                    if (ClaimManager.playerHasClaim(player.getUUID()) || ClaimManager.playerHasBase(player.getUUID())) {
                        player.sendSystemMessage(Component.literal("Esta cama não está numa área protegida."));
                    } else {
                        player.sendSystemMessage(
                                Component.literal("Esta cama não está numa área protegida, considere criar uma área protegida.")
                        );
                    }
                } else if (isWorkstation(block)) {
                    String nome = getDisplayName(block);

                    if (ClaimManager.playerHasClaim(player.getUUID()) || ClaimManager.playerHasBase(player.getUUID())) {
                        player.sendSystemMessage(Component.literal("Esta " + nome + " não está numa área protegida."));
                    } else {
                        player.sendSystemMessage(
                                Component.literal("Esta " + nome + " não está numa área protegida, considere criar uma área protegida.")
                        );
                    }
                }
            }

            return InteractionResult.PASS;
        });
    }

    private static boolean isChestLike(Block block) {
        return block == Blocks.CHEST
                || block == Blocks.TRAPPED_CHEST
                || block == Blocks.BARREL
                || block == Blocks.ENDER_CHEST;
    }

    private static boolean isDoorLike(Block block) {
        return block instanceof DoorBlock
                || block instanceof TrapDoorBlock
                || block instanceof FenceGateBlock
                || block == Blocks.LEVER
                || block instanceof ButtonBlock;
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