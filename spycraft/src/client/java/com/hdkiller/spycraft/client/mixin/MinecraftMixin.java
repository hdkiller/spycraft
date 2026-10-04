package com.hdkiller.spycraft.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow private int rightClickDelay;
    @Inject(at = @At("HEAD"), method = "startUseItem", cancellable = true)
    private void spycraft$droneAction(CallbackInfo info) {
        Minecraft client = (Minecraft) (Object) this;
        if (client.player != null && client.player.hasEffect(com.hdkiller.spycraft.effect.ModEffects.DRONE_PILOTING)
                && net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(
                        com.hdkiller.spycraft.network.DroneActionPayload.TYPE)) {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                    new com.hdkiller.spycraft.network.DroneActionPayload(client.player.getInventory().selected % 4));
            rightClickDelay = 4;
            info.cancel();
        }
    }
}
