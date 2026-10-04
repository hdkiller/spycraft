package com.hdkiller.spycraft.client.mixin;

import com.hdkiller.spycraft.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getFieldOfViewModifier", at = @At("HEAD"), cancellable = true)
    private void spycraft$getFieldOfViewModifier(CallbackInfoReturnable<Float> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        if (player.isUsingItem() && player.getUseItem().is(ModItems.BINOCULARS)) {
            if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
                // Moderate tactical binocular zoom (~3x magnification) rather than extreme 10x tunnel
                cir.setReturnValue(0.35f);
            }
        }
    }
}
