package com.erikcraft.client.mixin;

import com.erikcraft.item.VillagerDisguiseItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public class PlayerRendererMixin {
    @Unique
    private static Villager dummyVillager;

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void renderVillagerDisguise(AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (player.hasEffect(com.erikcraft.effect.ModEffects.DRONE_PILOTING)) {
            ci.cancel(); // Player is flying as the drone, hide player humanoid model
            return;
        }

        if (VillagerDisguiseItem.isDisguised(player)) {
            Minecraft mc = Minecraft.getInstance();
            if (player.level() != null) {
                if (dummyVillager == null || dummyVillager.level() != player.level()) {
                    dummyVillager = new Villager(EntityType.VILLAGER, player.level());
                }

                // Sync position, head/body rotation, and walking animation
                dummyVillager.setPos(player.getX(), player.getY(), player.getZ());
                dummyVillager.setXRot(player.getXRot());
                dummyVillager.setYRot(player.getYRot());
                dummyVillager.xRotO = player.xRotO;
                dummyVillager.yRotO = player.yRotO;
                dummyVillager.yBodyRot = player.yBodyRot;
                dummyVillager.yBodyRotO = player.yBodyRotO;
                dummyVillager.yHeadRot = player.yHeadRot;
                dummyVillager.yHeadRotO = player.yHeadRotO;
                dummyVillager.tickCount = player.tickCount;
                dummyVillager.setShiftKeyDown(player.isShiftKeyDown());
                dummyVillager.walkAnimation.setSpeed(player.walkAnimation.speed());
                dummyVillager.walkAnimation.update(player.walkAnimation.speed(), 1.0f);

                EntityRenderer<? super Villager> renderer = mc.getEntityRenderDispatcher().getRenderer(dummyVillager);
                if (renderer != null) {
                    renderer.render(dummyVillager, entityYaw, partialTicks, poseStack, buffer, packedLight);
                    ci.cancel(); // Cancel standard player model rendering!
                }
            }
        }
    }
}
