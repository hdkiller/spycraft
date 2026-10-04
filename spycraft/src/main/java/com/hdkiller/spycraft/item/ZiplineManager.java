package com.hdkiller.spycraft.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tactical Zipline System
 * Manages deployed taut steel wire ropes and high-speed cable sliding.
 * Features a two-stage mechanism:
 * 1. Deploy & anchor steel wire rope across chasm or rooftop
 * 2. Mount trolley pulley and slide at high velocity with dismount/retract options
 */
public class ZiplineManager {

    public static class ActiveZipline {
        public final UUID id;
        public final UUID ownerId;
        public final Vec3 startPos;
        public final Vec3 endPos;
        public final double length;
        public int remainingTicks;

        ActiveZipline(UUID ownerId, Vec3 startPos, Vec3 endPos, int durationTicks) {
            this.id = UUID.randomUUID();
            this.ownerId = ownerId;
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

    public static class ZiplineTarget {
        public final ActiveZipline zipline;
        public final double distance;
        public final boolean lookingAlongCable;

        public ZiplineTarget(ActiveZipline zipline, double distance, boolean lookingAlongCable) {
            this.zipline = zipline;
            this.distance = distance;
            this.lookingAlongCable = lookingAlongCable;
        }
    }

    private static final List<ActiveZipline> ACTIVE_ZIPLINES = new CopyOnWriteArrayList<>();
    private static final Map<UUID, ZiplineRider> ACTIVE_RIDERS = new ConcurrentHashMap<>();

    public static void deployZipline(ServerLevel level, ServerPlayer player, Vec3 startPos, Vec3 endPos) {
        // Limit active ziplines per player to 2
        List<ActiveZipline> owned = new ArrayList<>();
        for (ActiveZipline z : ACTIVE_ZIPLINES) {
            if (player.getUUID().equals(z.ownerId)) {
                owned.add(z);
            }
        }
        if (owned.size() >= 2) {
            ActiveZipline oldest = owned.get(0);
            ACTIVE_ZIPLINES.remove(oldest);
            level.playSound(null, oldest.startPos.x, oldest.startPos.y, oldest.startPos.z,
                SoundEvents.LEASH_KNOT_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
        }

        ActiveZipline zipline = new ActiveZipline(player.getUUID(), startPos, endPos, 3600); // 3 minutes
        ACTIVE_ZIPLINES.add(zipline);

        // Sound effects
        level.playSound(null, startPos.x, startPos.y, startPos.z,
            SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.2f);
        level.playSound(null, startPos.x, startPos.y, startPos.z,
            SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0f, 1.4f);

        level.playSound(null, endPos.x, endPos.y, endPos.z,
            SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 1.2f, 1.2f);
        level.playSound(null, endPos.x, endPos.y, endPos.z,
            SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.7f, 1.8f);

        // Visual tracer particles
        int steps = Math.min((int) (zipline.length * 2), 60);
        for (int i = 0; i <= steps; i++) {
            double p = (double) i / steps;
            Vec3 pt = startPos.lerp(endPos, p);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
        }

        player.displayClientMessage(
            Component.translatable("message.spycraft.zipline.deployed"),
            true
        );
    }

    public static boolean mountZipline(ServerLevel level, ServerPlayer player, ActiveZipline zipline) {
        Vec3 playerPos = player.getEyePosition();
        Vec3 ab = zipline.endPos.subtract(zipline.startPos);
        double lenSqr = ab.lengthSqr();
        if (lenSqr < 1e-4) return false;

        Vec3 ap = playerPos.subtract(zipline.startPos);
        double t = ap.dot(ab) / lenSqr;
        t = Math.max(0.0, Math.min(1.0, t));

        Vec3 look = player.getLookAngle();
        boolean forward;
        if (t < 0.25) {
            forward = look.dot(ab) >= -0.3;
        } else if (t > 0.75) {
            forward = look.dot(ab) > 0.3;
        } else {
            forward = look.dot(ab) >= 0.0;
        }

        Vec3 rideStart = zipline.startPos.lerp(zipline.endPos, t);
        Vec3 rideEnd = forward ? zipline.endPos : zipline.startPos;

        double dist = rideStart.distanceTo(rideEnd);
        if (dist < 1.5) {
            player.displayClientMessage(Component.translatable("message.spycraft.zipline.already_at_end"), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.6f);
            return false;
        }

        // Refresh lifetime on use
        zipline.remainingTicks = Math.max(zipline.remainingTicks, 3600);

        double speed = 0.70; // ~14 blocks per second, smooth and controllable cinematic slide
        ACTIVE_RIDERS.put(player.getUUID(), new ZiplineRider(player.getUUID(), rideStart, rideEnd, speed));
        GrapplingHookGunItem.grantGrappleProtection(player.getUUID());

        // Audio & Visual feedback
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ARMOR_EQUIP_CHAIN, SoundSource.PLAYERS, 1.0f, 1.3f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.MINECART_RIDING, SoundSource.PLAYERS, 0.7f, 1.6f);

        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
            rideStart.x, rideStart.y, rideStart.z, 10, 0.2, 0.2, 0.2, 0.05);

        return true;
    }

    public static void retractZipline(ServerLevel level, ServerPlayer player, ActiveZipline zipline) {
        ACTIVE_ZIPLINES.remove(zipline);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.LEASH_KNOT_BREAK, SoundSource.PLAYERS, 1.0f, 1.4f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);

        level.sendParticles(ParticleTypes.CRIT,
            zipline.startPos.x, zipline.startPos.y, zipline.startPos.z, 8, 0.2, 0.2, 0.2, 0.05);
        level.sendParticles(ParticleTypes.CRIT,
            zipline.endPos.x, zipline.endPos.y, zipline.endPos.z, 8, 0.2, 0.2, 0.2, 0.05);

        player.displayClientMessage(
            Component.translatable("message.spycraft.zipline.retracted"),
            true
        );
    }

    public static ZiplineTarget findBestZipline(ServerPlayer player, double maxDist) {
        Vec3 playerPos = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        ActiveZipline best = null;
        double bestDist = maxDist;
        boolean bestLooking = false;

        for (ActiveZipline z : ACTIVE_ZIPLINES) {
            Vec3 ab = z.endPos.subtract(z.startPos);
            double lenSqr = ab.lengthSqr();
            if (lenSqr < 1e-4) continue;

            Vec3 ap = playerPos.subtract(z.startPos);
            double t = ap.dot(ab) / lenSqr;
            t = Math.max(0.0, Math.min(1.0, t));
            Vec3 closest = z.startPos.add(ab.scale(t));
            double dist = playerPos.distanceTo(closest);

            if (dist <= maxDist) {
                boolean lookingAlong;
                Vec3 norm = ab.normalize();
                if (t < 0.25) {
                    lookingAlong = look.dot(norm) >= -0.2;
                } else if (t > 0.75) {
                    lookingAlong = look.dot(norm) <= 0.2;
                } else {
                    lookingAlong = Math.abs(look.dot(norm)) >= 0.2;
                }

                if (best == null || (lookingAlong && !bestLooking) || (lookingAlong == bestLooking && dist < bestDist)) {
                    best = z;
                    bestDist = dist;
                    bestLooking = lookingAlong;
                }
            }
        }

        if (best != null) {
            return new ZiplineTarget(best, bestDist, bestLooking);
        }
        return null;
    }

    public static void createZiplineAndRide(ServerLevel level, ServerPlayer player, Vec3 startPos, Vec3 endPos) {
        ActiveZipline zipline = new ActiveZipline(player.getUUID(), startPos, endPos, 3600);
        ACTIVE_ZIPLINES.add(zipline);
        mountZipline(level, player, zipline);
    }

    public static boolean isPlayerRiding(UUID playerId) {
        return ACTIVE_RIDERS.containsKey(playerId);
    }

    public static List<ActiveZipline> getActiveZiplines() {
        return ACTIVE_ZIPLINES;
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

                // Render taut cable particles every 2 ticks
                if (zipline.remainingTicks % 2 == 0) {
                    int steps = Math.min((int) (zipline.length * 1.6), 64);
                    for (int s = 0; s <= steps; s += 2) {
                        double p = (double) s / steps;
                        double sag = Math.sin(p * Math.PI) * Math.min(zipline.length * 0.015, 0.85);
                        Vec3 pt = zipline.startPos.lerp(zipline.endPos, p).subtract(0, sag, 0);
                        level.sendParticles(ParticleTypes.CRIT, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
                    }
                    // Anchors markers
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, zipline.startPos.x, zipline.startPos.y, zipline.startPos.z, 1, 0.02, 0.02, 0.02, 0);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, zipline.endPos.x, zipline.endPos.y, zipline.endPos.z, 1, 0.02, 0.02, 0.02, 0);
                }
            }
        }
    }
}
