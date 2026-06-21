package com.amadeu.landprotection.mixin;

import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerExplosion.class)
public abstract class ExplosionProtectionMixin {

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private Entity source;

    @Inject(method = "interactWithBlocks", at = @At("HEAD"))
    private void landprotection_filterProtectedClaimBlocks(List<BlockPos> blocks, CallbackInfo ci) {
        blocks.removeIf(this::landprotection_shouldProtectBlockFromExplosion);
    }

    private boolean landprotection_shouldProtectBlockFromExplosion(BlockPos pos) {
        String dimension = level.dimension().toString();
        Claim claim = ClaimManager.getClaimAt(pos, dimension);

        if (claim == null) {
            return false;
        }

        if (claim.isArena()) {
            return false;
        }

        /*
         * Regra de creeper:
         *
         * - Se o creeper explodir dentro de uma claim normal, a explosão só pode
         *   quebrar blocos se ele estiver perseguindo o dono ou alguém trusted.
         *
         * - Guest não libera explosão, porque guest é permissão limitada:
         *   baús, portas, alavancas e botões.
         *
         * - Jogador sem permissão também não libera explosão, evitando furto com creeper.
         */
        if (source instanceof Creeper creeper) {
            LivingEntity target = creeper.getTarget();

            if (target instanceof Player player) {
                return !canCreeperBreakClaimForPlayer(claim, player);
            }
        }

        return true;
    }

    private boolean canCreeperBreakClaimForPlayer(Claim claim, Player player) {
        return claim.isOwner(player.getUUID())
                || claim.isTrusted(player.getUUID());
    }
}
