package com.erikcraft.client.mixin;

import com.erikcraft.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "renderSpyglassOverlay", at = @At("RETURN"))
    private void erikcraft$renderSniperReticle(GuiGraphics guiGraphics, float scopeScale, CallbackInfo ci) {
        LocalPlayer player = this.minecraft.player;
        if (player == null || !player.isUsingItem() || !player.getUseItem().is(ModItems.SNIPER_RIFLE)) {
            return;
        }

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int cx = width / 2;
        int cy = height / 2;

        // Tactical Reticle Colors
        int crosshairColor = 0xDD111111; // Solid dark reticle
        int redDotColor = 0xFFFF2222;    // Glowing red center dot
        int accentColor = 0xAAFF3333;    // Red tactical brackets

        // 1. Crosshair lines (leaving small center gap for the precision dot)
        int lineLen = 85;
        int gap = 5;

        // Horizontal line
        guiGraphics.fill(cx - lineLen, cy, cx - gap, cy + 1, crosshairColor);
        guiGraphics.fill(cx + gap + 1, cy, cx + lineLen + 1, cy + 1, crosshairColor);

        // Vertical line
        guiGraphics.fill(cx, cy - lineLen, cx + 1, cy - gap, crosshairColor);
        guiGraphics.fill(cx, cy + gap + 1, cx + 1, cy + lineLen + 1, crosshairColor);

        // 2. Mil-dot elevation and windage tick marks
        for (int dist = 16; dist <= 72; dist += 14) {
            // Horizontal ticks
            guiGraphics.fill(cx - dist, cy - 2, cx - dist + 1, cy + 3, crosshairColor);
            guiGraphics.fill(cx + dist, cy - 2, cx + dist + 1, cy + 3, crosshairColor);

            // Vertical ticks
            guiGraphics.fill(cx - 2, cy - dist, cx + 3, cy - dist + 1, crosshairColor);
            guiGraphics.fill(cx - 2, cy + dist, cx + 3, cy + dist + 1, crosshairColor);
        }

        // 3. Center precision red dot
        guiGraphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, redDotColor);

        // 4. Tactical corner brackets
        int bSize = 20;
        int bOffset = 55;
        // Top-left
        guiGraphics.fill(cx - bOffset, cy - bOffset, cx - bOffset + bSize, cy - bOffset + 1, accentColor);
        guiGraphics.fill(cx - bOffset, cy - bOffset, cx - bOffset + 1, cy - bOffset + bSize, accentColor);
        // Top-right
        guiGraphics.fill(cx + bOffset - bSize, cy - bOffset, cx + bOffset, cy - bOffset + 1, accentColor);
        guiGraphics.fill(cx + bOffset - 1, cy - bOffset, cx + bOffset, cy - bOffset + bSize, accentColor);
        // Bottom-left
        guiGraphics.fill(cx - bOffset, cy + bOffset - 1, cx - bOffset + bSize, cy + bOffset, accentColor);
        guiGraphics.fill(cx - bOffset, cy + bOffset - bSize, cx - bOffset + 1, cy + bOffset, accentColor);
        // Bottom-right
        guiGraphics.fill(cx + bOffset - bSize, cy + bOffset - 1, cx + bOffset, cy + bOffset, accentColor);
        guiGraphics.fill(cx + bOffset - 1, cy + bOffset - bSize, cx + bOffset, cy + bOffset, accentColor);

        // 5. Tactical Rangefinder Readout
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        double maxDist = 128.0;
        Vec3 reach = eyePos.add(look.scale(maxDist));

        HitResult hit = player.level().clip(new ClipContext(
            eyePos, reach,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        ));

        double distMeters = hit.getType() != HitResult.Type.MISS ? eyePos.distanceTo(hit.getLocation()) : maxDist;

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            player.level(), player, eyePos, reach,
            new AABB(eyePos, reach).inflate(1.0),
            e -> e instanceof LivingEntity && e.isAlive()
        );

        Font font = this.minecraft.font;
        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
            double targetDist = eyePos.distanceTo(entityHit.getLocation());
            String targetText = "§c🎯 CÉLPONT: §f" + target.getName().getString() + " §8[§e" + (int) targetDist + "m§8]";
            int textWidth = font.width(targetText);
            guiGraphics.drawString(font, targetText, cx - textWidth / 2, cy + 28, 0xFFFFFF, true);
        } else {
            String rangeText = "§7TÁVOLSÁG: §e" + (int) distMeters + "m";
            int textWidth = font.width(rangeText);
            guiGraphics.drawString(font, rangeText, cx - textWidth / 2, cy + 28, 0xFFFFFF, true);
        }

        // Charge indicator
        int ticksUsing = player.getTicksUsingItem();
        if (ticksUsing >= 10) {
            String readyText = "§a● LÖVÉS KÉSZ";
            int readyWidth = font.width(readyText);
            guiGraphics.drawString(font, readyText, cx - readyWidth / 2, cy - 32, 0x55FF55, true);
        } else {
            String chargingText = "§e○ CÉLZÁS...";
            int chargeWidth = font.width(chargingText);
            guiGraphics.drawString(font, chargingText, cx - chargeWidth / 2, cy - 32, 0xFFFF55, true);
        }
    }
}
