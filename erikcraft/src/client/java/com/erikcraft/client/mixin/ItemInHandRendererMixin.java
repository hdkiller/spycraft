package com.erikcraft.client.mixin;

import com.erikcraft.effect.ModEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void erikcraft$hideHandsInDrone(float partialTicks, PoseStack poseStack, MultiBufferSource.BufferSource buffer, LocalPlayer player, int packedLight, CallbackInfo ci) {
        if (player.hasEffect(ModEffects.DRONE_PILOTING)) {
            ci.cancel(); // First person drone camera view is completely unobstructed
        }
    }
}
