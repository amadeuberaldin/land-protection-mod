package com.amadeu.landprotection.mixin;

import com.amadeu.landprotection.claim.ClaimManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class VillagerProjectileProtectionMixin {

    @Inject(method = "onEntityHit", at = @At("HEAD"), cancellable = true)
    private void protectVillagerFromArrow(EntityHitResult hitResult, CallbackInfo ci) {
        Entity target = hitResult.getEntity();

        if (!(target instanceof VillagerEntity villager)) {
            return;
        }

        PersistentProjectileEntity projectile = (PersistentProjectileEntity) (Object) this;
        Entity owner = projectile.getOwner();

        // Só bloqueia projétil disparado por jogador.
        // Dano de zumbi e outros mobs continua permitido.
        if (!(owner instanceof PlayerEntity player)) {
            return;
        }

        if (!ClaimManager.canInteract(player.getUuid(), villager.getBlockPos())) {
            player.sendMessage(Text.literal("Este villager está protegido nesta área privada."), true);
            ci.cancel();
        }
    }
}
