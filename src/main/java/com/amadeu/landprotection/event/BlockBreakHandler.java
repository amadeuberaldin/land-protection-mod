package com.amadeu.landprotection.event;

import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import com.amadeu.landprotection.dragon.DragonEggSystem;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

public class BlockBreakHandler {

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {

            if (!(player instanceof ServerPlayer serverPlayer)) {
                return true;
            }

            String dimension = world.dimension().toString();

            Claim arena = ClaimManager.getArenaAt(pos, dimension);

            if (arena != null) {
                return true;
            }

            if (ClaimManager.canBuild(serverPlayer, pos, dimension)) {
                if (state.is(Blocks.DRAGON_EGG)
                        && world instanceof ServerLevel serverLevel) {
                    DragonEggSystem.onDragonEggBroken(
                            serverPlayer.level().getServer(),
                            serverLevel,
                            pos);
                }

                return true;
            }

            String ownerName = "outro jogador";

            Claim claim = ClaimManager.getClaimAt(pos, dimension);
            if (claim != null) {
                ServerPlayer owner = world.getServer().getPlayerList().getPlayer(claim.getOwner());
                if (owner != null) {
                    ownerName = owner.getName().getString();
                }

                player.sendSystemMessage(
                        Component.literal("Esta área pertence a " + ownerName + ", peça autorização para construir."));
                return false;
            }

            return true;
        });
    }
}
