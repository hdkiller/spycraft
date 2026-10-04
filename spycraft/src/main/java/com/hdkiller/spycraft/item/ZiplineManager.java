package com.hdkiller.spycraft.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tactical Zipline System
 * Manages deployed taut wire ropes and high-speed cable sliding.
 */
public class ZiplineManager {

    public static class ActiveZipline {
        public final UUID id;
        public final Vec3 startPos;
        public final Vec3 endPos;
        public final double length;
        public int remainingTicks;

        ActiveZipline(Vec3 startPos, Vec3 endPos, int durationTicks) {
            this.id = UUID.randomUUID();
            this.startPos = startPos;
            this.endPos = endPos;
            this.length = startPos.distanceTo(endPos);
            this.remainingTicks = durationTicks;
        }
    }

    public static class ZiplineRider {
        public final UUID playerId;
        public final Vec3 startPos;
        public final Vec3 endPos;
        public final double totalDist;
        public final double speed;
        public double currentDist;
        public int ticksSliding;

        ZiplineRider(UUID playerId, Vec3 startPos, Vec3 endPos, double speed) {
            this.playerId = playerId;
            this.startPos = startPos;
            this.endPos = endPos;
            this.totalDist = startPos.distanceTo(endPos);
            this.speed = speed;
            this.currentDist = 0.0;
            this.ticksSliding = 0;
        }
    }

    private static final List<ActiveZipline> ACTIVE_ZIPLINES = new CopyOnWriteArrayList<>();
    private static final Map<UUID, ZiplineRider> ACTIVE_RIDERS = new ConcurrentHashMap<>();

    public static void createZiplineAndRide(ServerLevel level, ServerPlayer player, Vec3 startPos, Vec3 endPos) {
        ActiveZipline zipline = new ActiveZipline(startPos, endPos, 1800); // 90 seconds cable lifetime
        ACTIVE_ZIPLINES.add(zipline);

        startRiding(level, player, zipline.startPos, zipline.endPos);

        // Cable anchor spawn sound & fx
        level.playSound(null, startPos.x, startPos.y, startPos.z,
            SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0f, 1.2f);
        level.playSound(null, endPos.x, endPos.y, endPos.z,
            SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 1.0f, 1.4f);

        // Visual cable shoot tracer
        int steps = Math.min((int) (zipline.length * 2), 60);
        for (int i = 0; i <= steps; i++) {
            double p = (double) i / steps;
            Vec3 pt = startPos.lerp(endPos, p);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
        }
    }

    public static void startRiding(ServerLevel level, ServerPlayer player, Vec3 startPos, Vec3 endPos) {
        double speed = 1.35; // ~27 blocks per second, exhilarating tactical slide
        ACTIVE_RIDERS.put(player.getUUID(), new ZiplineRider(player.getUUID(), startPos, endPos, speed));
        GrapplingHookGunItem.grantGrappleProtection(player.getUUID());

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.6f);
    }

    public static boolean isPlayerRiding(UUID playerId) {
        return ACTIVE_RIDERS.containsKey(playerId);
    }

    public static void tick(ServerLevel level) {
        // 1. Process active zipline riders
        if (!ACTIVE_RIDERS.isEmpty()) {
            Iterator<Map.Entry<UUID, ZiplineRider>> riderIt = ACTIVE_RIDERS.entrySet().iterator();
            while (riderIt.hasNext()) {
                Map.Entry<UUID, ZiplineRider> entry = riderIt.next();
                ZiplineRider rider = entry.getValue();

                ServerPlayer player = level.getServer().getPlayerList().getPlayer(rider.playerId);
                if (player == null || !player.isAlive()) {
                    riderIt.remove();
                    continue;
                }

                rider.ticksSliding++;

                // Early detach on sneak / crouch
                if (player.isShiftKeyDown() && rider.ticksSliding > 5) {
                    Vec3 dir = rider.endPos.subtract(rider.startPos).normalize();
                    player.setDeltaMovement(dir.x * rider.speed * 0.75, 0.2, dir.z * rider.speed * 0.75);
                    player.hurtMarked = true;
                    player.resetFallDistance();
                    GrapplingHookGunItem.grantGrappleProtection(player.getUUID());

                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.LEASH_KNOT_BREAK, SoundSource.PLAYERS, 0.9f, 1.6f);

                    riderIt.remove();
                    continue;
                }

                rider.currentDist += rider.speed;
                double progress = rider.currentDist / rider.totalDist;

                if (progress >= 1.0) {
                    // Arrived at destination anchor!
                    Vec3 dir = rider.endPos.subtract(rider.startPos).normalize();
                    Vec3 arrivalPos = rider.endPos.add(0, 0.35, 0);

                    player.teleportTo(arrivalPos.x, arrivalPos.y, arrivalPos.z);
                    player.setDeltaMovement(dir.x * 0.35, 0.48, dir.z * 0.35); // Ledge hop
                    player.hurtMarked = true;
                    player.resetFallDistance();
                    GrapplingHookGunItem.grantGrappleProtection(player.getUUID());

                    level.playSound(null, arrivalPos.x, arrivalPos.y, arrivalPos.z,
                        SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0f, 1.6f);
                    level.playSound(null, arrivalPos.x, arrivalPos.y, arrivalPos.z,
                        SoundEvents.ARMOR_EQUIP_CHAIN, SoundSource.PLAYERS, 1.0f, 1.2f);

                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        arrivalPos.x, arrivalPos.y + 0.5, arrivalPos.z, 15, 0.3, 0.3, 0.3, 0.05);

                    player.displayClientMessage(
                        Component.translatable("hud.spycraft.zipline.dismount"),
                        true
                    );

                    riderIt.remove();
                } else {
                    // Interpolate position along cable with subtle catenary sag
                    double sag = Math.sin(progress * Math.PI) * Math.min(rider.totalDist * 0.015, 1.0);
                    Vec3 cablePt = rider.startPos.lerp(rider.endPos, progress);
                    Vec3 riderPt = cablePt.subtract(0, sag + 1.25, 0);

                    player.teleportTo(riderPt.x, riderPt.y, riderPt.z);
                    Vec3 dir = rider.endPos.subtract(rider.startPos).normalize();
                    player.setDeltaMovement(dir.scale(rider.speed));
                    player.hurtMarked = true;
                    player.resetFallDistance();
                    GrapplingHookGunItem.grantGrappleProtection(player.getUUID());

                    // Friction sparks at the pulley handle
                    level.sendParticles(ParticleTypes.CRIT,
                        cablePt.x, cablePt.y - sag, cablePt.z, 2, 0.05, 0.05, 0.05, 0.02);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        cablePt.x, cablePt.y - sag, cablePt.z, 1, 0, 0, 0, 0);

                    // Pulley zip sound
                    if (rider.ticksSliding % 3 == 0) {
                        float pitch = 1.6f + (float) (Math.sin(progress * Math.PI) * 0.3);
                        level.playSound(null, riderPt.x, riderPt.y, riderPt.z,
                            SoundEvents.MINECART_RIDING, SoundSource.PLAYERS, 0.5f, pitch);
                    }

                    // Progress HUD in actionbar
                    int pct = (int) (progress * 100);
                    player.displayClientMessage(
                        Component.translatable("hud.spycraft.zipline.riding", pct),
                        true
                    );
                }
            }
        }

        // 2. Process active deployed zipline ropes (render cable particles across chasm)
        if (!ACTIVE_ZIPLINES.isEmpty()) {
            Iterator<ActiveZipline> it = ACTIVE_ZIPLINES.iterator();
            while (it.hasNext()) {
                ActiveZipline zipline = it.next();
                zipline.remainingTicks--;

                if (zipline.remainingTicks <= 0) {
                    ACTIVE_ZIPLINES.remove(zipline);
                    continue;
                }

                // Periodic cable line particle rendering every 6 ticks
                if (zipline.remainingTicks % 6 == 0) {
                    int steps = Math.min((int) (zipline.length * 1.5), 50);
                    for (int s = 0; s <= steps; s += 2) {
                        double p = (double) s / steps;
                        double sag = Math.sin(p * Math.PI) * Math.min(zipline.length * 0.015, 1.0);
                        Vec3 pt = zipline.startPos.lerp(zipline.endPos, p).subtract(0, sag, 0);
                        level.sendParticles(ParticleTypes.CRIT, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }
}
