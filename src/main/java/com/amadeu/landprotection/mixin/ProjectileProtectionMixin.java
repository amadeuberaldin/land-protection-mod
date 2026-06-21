package com.amadeu.landprotection.mixin;

import com.amadeu.landprotection.claim.Claim;
import com.amadeu.landprotection.claim.ClaimManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileProtectionMixin {

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void landprotection_blockProjectileClaimAbuse(EntityHitResult hitResult, CallbackInfo ci) {
        Projectile projectile = (Projectile) (Object) this;

        Entity owner = projectile.getOwner();
        Entity target = hitResult.getEntity();

        if (!(owner instanceof ServerPlayer attacker)) {
            return;
        }

        String attackerDimension = attacker.level().dimension().identifier().toString();
        String targetDimension = target.level().dimension().identifier().toString();

        Claim attackerArena = ClaimManager.getArenaAt(attacker.blockPosition(), attackerDimension);
        Claim targetArena = ClaimManager.getArenaAt(target.blockPosition(), targetDimension);

        if (attackerArena != null && targetArena != null) {
            return;
        }

        Claim attackerClaim = ClaimManager.getClaimAt(attacker.blockPosition(), attackerDimension);
        Claim targetClaim = ClaimManager.getClaimAt(target.blockPosition(), targetDimension);

        if (isNormalPlayerClaim(attackerClaim) || isNormalPlayerClaim(targetClaim)) {
            attacker.sendSystemMessage(
                    Component.literal("Projéteis não podem ser usados para abusar de claims protegidas."));
            ci.cancel();
        }
    }

    private boolean isNormalPlayerClaim(Claim claim) {
        return claim != null
                && !claim.isServerClaim()
                && !claim.isArena();
    }
}
