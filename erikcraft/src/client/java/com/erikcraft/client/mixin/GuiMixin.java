package com.erikcraft.client.mixin;

import com.erikcraft.effect.ModEffects;
import com.erikcraft.item.ModItems;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
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

import java.util.List;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Shadow @Final private Minecraft minecraft;

    // Sniper Reticle
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

        int crosshairColor = 0xDD111111;
        int redDotColor = 0xFFFF2222;
        int accentColor = 0xAAFF3333;

        int lineLen = 85;
        int gap = 5;

        // Horizontal line
        guiGraphics.fill(cx - lineLen, cy, cx - gap, cy + 1, crosshairColor);
        guiGraphics.fill(cx + gap + 1, cy, cx + lineLen + 1, cy + 1, crosshairColor);

        // Vertical line
        guiGraphics.fill(cx, cy - lineLen, cx + 1, cy - gap, crosshairColor);
        guiGraphics.fill(cx, cy + gap + 1, cx + 1, cy + lineLen + 1, crosshairColor);

        // Mil-dots
        for (int dist = 16; dist <= 72; dist += 14) {
            guiGraphics.fill(cx - dist, cy - 2, cx - dist + 1, cy + 3, crosshairColor);
            guiGraphics.fill(cx + dist, cy - 2, cx + dist + 1, cy + 3, crosshairColor);
            guiGraphics.fill(cx - 2, cy - dist, cx + 3, cy - dist + 1, crosshairColor);
            guiGraphics.fill(cx - 2, cy + dist, cx + 3, cy + dist + 1, crosshairColor);
        }

        // Center red dot
        guiGraphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, redDotColor);

        // Corner brackets
        int bSize = 20;
        int bOffset = 55;
        guiGraphics.fill(cx - bOffset, cy - bOffset, cx - bOffset + bSize, cy - bOffset + 1, accentColor);
        guiGraphics.fill(cx - bOffset, cy - bOffset, cx - bOffset + 1, cy - bOffset + bSize, accentColor);
        guiGraphics.fill(cx + bOffset - bSize, cy - bOffset, cx + bOffset, cy - bOffset + 1, accentColor);
        guiGraphics.fill(cx + bOffset - 1, cy - bOffset, cx + bOffset, cy - bOffset + bSize, accentColor);
        guiGraphics.fill(cx - bOffset, cy + bOffset - 1, cx - bOffset + bSize, cy + bOffset, accentColor);
        guiGraphics.fill(cx - bOffset, cy + bOffset - bSize, cx - bOffset + 1, cy + bOffset, accentColor);
        guiGraphics.fill(cx + bOffset - bSize, cy + bOffset - 1, cx + bOffset, cy + bOffset, accentColor);
        guiGraphics.fill(cx + bOffset - 1, cy + bOffset - bSize, cx + bOffset, cy + bOffset, accentColor);

        // Rangefinder
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

    // Drone Tactical HUD
    @Inject(method = "render", at = @At("RETURN"))
    private void erikcraft$renderDroneHUD(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        LocalPlayer player = this.minecraft.player;
        if (player == null || !player.hasEffect(ModEffects.DRONE_PILOTING)) {
            return;
        }

        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        int cx = width / 2;
        int cy = height / 2;
        Font font = this.minecraft.font;

        MobEffectInstance effect = player.getEffect(ModEffects.DRONE_PILOTING);
        int duration = effect != null ? effect.getDuration() : 300;
        float batteryPct = Math.clamp(duration / 300.0f, 0.0f, 1.0f);
        int secsRemaining = (duration + 19) / 20;

        // 1. Tactical HUD Corner Brackets
        int frameColor = 0xAA00DDFF;
        int bLen = 22;
        int m = 14;

        // Top-Left
        guiGraphics.fill(m, m, m + bLen, m + 2, frameColor);
        guiGraphics.fill(m, m, m + 2, m + bLen, frameColor);
        // Top-Right
        guiGraphics.fill(width - m - bLen, m, width - m, m + 2, frameColor);
        guiGraphics.fill(width - m - 2, m, width - m, m + bLen, frameColor);
        // Bottom-Left
        guiGraphics.fill(m, height - m - 2, m + bLen, height - m, frameColor);
        guiGraphics.fill(m, height - m - bLen, m + 2, height - m, frameColor);
        // Bottom-Right
        guiGraphics.fill(width - m - bLen, height - m - 2, width - m, height - m, frameColor);
        guiGraphics.fill(width - m - 2, height - m - bLen, width - m, height - m, frameColor);

        // 2. Top Battery & Telemetry Banner
        int topBoxW = 120;
        int topBoxH = 22;
        guiGraphics.fill(cx - topBoxW, 8, cx + topBoxW, 8 + topBoxH, 0x88001015);
        guiGraphics.fill(cx - topBoxW, 8, cx + topBoxW, 9, frameColor);
        guiGraphics.fill(cx - topBoxW, 8 + topBoxH - 1, cx + topBoxW, 8 + topBoxH, frameColor);

        String title = "§b🛸 FELDERÍTŐ DRÓN §8| §fAKKU: §e" + (int)(batteryPct * 100) + "% §8(§f" + secsRemaining + "s§8)";
        guiGraphics.drawString(font, title, cx - font.width(title) / 2, 11, 0xFFFFFF, true);

        // Battery gauge bar
        int barTotalW = 140;
        int barH = 3;
        int barX = cx - barTotalW / 2;
        int barY = 22;
        guiGraphics.fill(barX, barY, barX + barTotalW, barY + barH, 0x55222222);

        int filledW = (int) (barTotalW * batteryPct);
        int barColor = batteryPct > 0.4f ? 0xFF00FF88 : (batteryPct > 0.2f ? 0xFFFFDD00 : 0xFFFF3333);
        guiGraphics.fill(barX, barY, barX + filledW, barY + barH, barColor);

        // 3. Left Telemetry Box
        int boxW = 85;
        guiGraphics.fill(16, cy - 35, 16 + boxW, cy + 35, 0x77001015);
        guiGraphics.fill(16, cy - 35, 17, cy + 35, frameColor);
        guiGraphics.drawString(font, "§7MAGASSÁG", 22, cy - 28, 0xCCCCCC, true);
        guiGraphics.drawString(font, "§b" + player.getBlockY() + "m", 22, cy - 18, 0x00FFFF, true);
        guiGraphics.drawString(font, "§7KOORDINÁTA", 22, cy - 4, 0xCCCCCC, true);
        guiGraphics.drawString(font, "§f" + player.getBlockX() + ", " + player.getBlockZ(), 22, cy + 6, 0xFFFFFF, true);
        guiGraphics.drawString(font, "§7DŐLÉS: §f" + (int) player.getXRot() + "°", 22, cy + 20, 0xAAAAAA, true);

        // 4. Right Radar Scan Box
        AABB scanBox = player.getBoundingBox().inflate(32.0);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class, scanBox,
            e -> e != player && e.isAlive());

        guiGraphics.fill(width - 16 - boxW, cy - 35, width - 16, cy + 35, 0x77001015);
        guiGraphics.fill(width - 16 - 1, cy - 35, width - 16, cy + 35, frameColor);
        guiGraphics.drawString(font, "§e📡 RADAR", width - 16 - boxW + 6, cy - 28, 0xFFDD00, true);
        guiGraphics.drawString(font, "§a" + nearby.size() + " CÉLPONT", width - 16 - boxW + 6, cy - 18, 0x55FF55, true);
        guiGraphics.drawString(font, "§7HATÓTÁV", width - 16 - boxW + 6, cy - 4, 0xCCCCCC, true);
        guiGraphics.drawString(font, "§b32 MÉTER", width - 16 - boxW + 6, cy + 6, 0x00FFFF, true);
        guiGraphics.drawString(font, "§7SZKENNER: §aAKTÍV", width - 16 - boxW + 6, cy + 20, 0x55FF55, true);

        // 5. Center Drone Aiming Reticle
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 reach = eyePos.add(look.scale(64.0));

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            player.level(), player, eyePos, reach,
            new AABB(eyePos, reach).inflate(1.2),
            e -> e instanceof LivingEntity && e != player && e.isAlive()
        );

        int reticleColor = entityHit != null ? 0xFFFF2222 : 0xCC00FFEE;
        int rSize = 10;

        // Crosshair corner marks
        guiGraphics.fill(cx - rSize, cy - rSize, cx - rSize + 4, cy - rSize + 1, reticleColor);
        guiGraphics.fill(cx - rSize, cy - rSize, cx - rSize + 1, cy - rSize + 4, reticleColor);
        guiGraphics.fill(cx + rSize - 4, cy - rSize, cx + rSize, cy - rSize + 1, reticleColor);
        guiGraphics.fill(cx + rSize - 1, cy - rSize, cx + rSize, cy - rSize + 4, reticleColor);
        guiGraphics.fill(cx - rSize, cy + rSize - 1, cx - rSize + 4, cy + rSize, reticleColor);
        guiGraphics.fill(cx - rSize, cy + rSize - 4, cx - rSize + 1, cy + rSize, reticleColor);
        guiGraphics.fill(cx + rSize - 4, cy + rSize - 1, cx + rSize, cy + rSize, reticleColor);
        guiGraphics.fill(cx + rSize - 1, cy + rSize - 4, cx + rSize, cy + rSize, reticleColor);

        // Center dot
        guiGraphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, reticleColor);

        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
            int d = (int) player.distanceTo(target);
            String targetMsg = "§c🎯 CÉLPONT ZÁROLVA: §f" + target.getName().getString() + " §8[§e" + d + "m§8]";
            guiGraphics.drawString(font, targetMsg, cx - font.width(targetMsg) / 2, cy - 24, 0xFFFFFF, true);

            String actionMsg = "§c[JOBB KLIKK: LÉZER JELÖLÉS]";
            guiGraphics.drawString(font, actionMsg, cx - font.width(actionMsg) / 2, cy + 16, 0xFF5555, true);
        } else {
            String scanMsg = "§b[JOBB KLIKK: LÉZER / GPS JELÖLÉS]";
            guiGraphics.drawString(font, scanMsg, cx - font.width(scanMsg) / 2, cy + 16, 0x00EEEE, true);
        }

        // 6. Bottom Tactical Action Bar
        String botToolbar = "§b[JOBB KLIKK: 🎯 Lézer] §8| §c[SHIFT+KLIKK: 💥 Kamikaze] §8| §e[ÉGRE NÉZVE KLIKK: 🏠 Visszatérés]";
        int botW = font.width(botToolbar) + 20;
        guiGraphics.fill(cx - botW / 2, height - 30, cx + botW / 2, height - 12, 0x88001015);
        guiGraphics.fill(cx - botW / 2, height - 30, cx + botW / 2, height - 29, frameColor);
        guiGraphics.drawString(font, botToolbar, cx - font.width(botToolbar) / 2, height - 23, 0xFFFFFF, true);
    }
}
