package com.amadeu.landprotection.mixin;

import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxSlot.class)
public abstract class DragonEggShulkerSlotProtectionMixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void landprotection_blockDragonEggInShulkerSlot(
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir) {

        if (stack.is(Blocks.DRAGON_EGG.asItem())) {
            cir.setReturnValue(false);
        }
    }
}
