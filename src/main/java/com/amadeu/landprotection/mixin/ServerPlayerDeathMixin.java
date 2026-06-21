package com.amadeu.landprotection.mixin;

import com.amadeu.landprotection.event.ColizeuKeepInventoryHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerDeathMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void landprotection_saveColizeuInventory(
            DamageSource damageSource,
            CallbackInfo ci) {

        ServerPlayer player = (ServerPlayer) (Object) this;

        ColizeuKeepInventoryHandler.captureInventory(player);
    }
}
