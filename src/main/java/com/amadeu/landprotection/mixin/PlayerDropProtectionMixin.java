package com.amadeu.landprotection.mixin;

import com.amadeu.landprotection.event.ColizeuKeepInventoryHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerDropProtectionMixin {

    @Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
    private void landprotection_cancelColizeuDrops(
            ServerLevel level,
            CallbackInfo ci) {

        Player player = (Player) (Object) this;

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (ColizeuKeepInventoryHandler.hasSavedInventory(serverPlayer)) {
            ci.cancel();
        }
    }
}
