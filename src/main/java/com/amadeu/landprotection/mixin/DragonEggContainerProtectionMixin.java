package com.amadeu.landprotection.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class DragonEggContainerProtectionMixin {

    @Shadow
    @Final
    public Container container;

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void landprotection_blockDragonEggInExternalSlots(
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir) {

        if (!stack.is(Blocks.DRAGON_EGG.asItem())) {
            return;
        }

        if (container instanceof Inventory) {
            return;
        }

        cir.setReturnValue(false);
    }
}
