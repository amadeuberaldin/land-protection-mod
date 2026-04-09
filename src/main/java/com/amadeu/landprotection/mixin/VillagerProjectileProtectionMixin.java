package com.amadeu.landprotection.mixin;

import com.amadeu.landprotection.claim.ClaimManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class VillagerProjectileProtectionMixin {

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void protectVillagerFromArrow(EntityHitResult hitResult, CallbackInfo ci) {
        Entity target = hitResult.getEntity();

        if (!(target instanceof Villager villager)) {
            return;
        }

        Projectile projectile = (Projectile) (Object) this;
        Entity owner = projectile.getOwner();

        // Só bloqueia projétil disparado por jogador
        if (!(owner instanceof Player player)) {
            return;
        }

        if (!ClaimManager.canInteract(player.getUUID(), villager.blockPosition())) {
            player.sendSystemMessage(Component.literal("Este villager está protegido nesta área privada."));
            ci.cancel();
        }
    }
}