package com.erikcraft.drone;

import com.erikcraft.effect.ModEffects;
import com.erikcraft.item.MobTrackerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reconnaissance Drone Fleet Manager
 * Manages active drone piloting sessions, real-time scanning, laser tagging, and kamikaze strikes.
 */
public class ReconDroneManager {
    private static final Map<UUID, DroneSession> SESSIONS = new ConcurrentHashMap<>();

    public static class DroneSession {
        public final UUID playerUuid;
        public final Vec3 launchPos;
        public final float launchYaw;
        public final float launchPitch;
        public final ResourceKey<Level> dimension;
        public final int maxTicks;
        public int ticksRemaining;
        public final Set<UUID> scannedMobs = new HashSet<>();
        public UUID laserTaggedMob;

        public DroneSession(UUID playerUuid, Vec3 launchPos, float launchYaw, float launchPitch, ResourceKey<Level> dimension, int durationTicks) {
            this.playerUuid = playerUuid;
            this.launchPos = launchPos;
            this.launchYaw = launchYaw;
            this.launchPitch = launchPitch;
            this.dimension = dimension;
            this.maxTicks = durationTicks;
            this.ticksRemaining = durationTicks;
        }
    }

    public static boolean isPiloting(UUID playerUuid) {
        return SESSIONS.containsKey(playerUuid);
    }

    public static DroneSession getSession(UUID playerUuid) {
        return SESSIONS.get(playerUuid);
    }

    /**
     * Start Drone Piloting Session
     */
    public static void startSession(ServerPlayer player, int durationTicks) {
        UUID uuid = player.getUUID();
        Vec3 launchPos = player.position();
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        DroneSession session = new DroneSession(uuid, launchPos, yaw, pitch, player.level().dimension(), durationTicks);
        SESSIONS.put(uuid, session);

        // Apply drone effect, invisibility, and full damage resistance
        player.addEffect(new MobEffectInstance(ModEffects.DRONE_PILOTING, durationTicks, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, durationTicks + 40, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks + 40, 4, false, false, false));

        // Enable nimble 3D flight
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.getAbilities().setFlyingSpeed(0.08f);
        player.onUpdateAbilities();

        // Launch audio & visual effects
        ServerLevel level = player.serverLevel();
        level.playSound(null, launchPos.x, launchPos.y, launchPos.z,
            SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.2f, 1.2f);
        level.playSound(null, launchPos.x, launchPos.y, launchPos.z,
            SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5f, 2.0f);

        // Holographic pilot beacon particles at launch point
        for (int i = 0; i < 20; i++) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                launchPos.x, launchPos.y + 0.1, launchPos.z,
                1, 0.4, 0.1, 0.4, 0.02);
        }

        player.sendSystemMessage(Component.literal(
            "§b🛸 [FELDERÍTŐ DRÓN] §fRendszerek bekapcsolva! Irányítás átvéve. §8(§e15s akkumulátor§8)"
        ));
    }

    /**
     * Server tick handler for all active drone sessions
     */
    public static void tick(ServerLevel level) {
        if (SESSIONS.isEmpty()) return;

        DustParticleOptions redLed = new DustParticleOptions(new Vector3f(1.0f, 0.1f, 0.1f), 0.7f);
        DustParticleOptions greenLed = new DustParticleOptions(new Vector3f(0.1f, 1.0f, 0.2f), 0.7f);

        for (Map.Entry<UUID, DroneSession> entry : new HashMap<>(SESSIONS).entrySet()) {
            UUID uuid = entry.getKey();
            DroneSession session = entry.getValue();

            if (!session.dimension.equals(level.dimension())) continue;

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player == null || !player.isAlive()) {
                SESSIONS.remove(uuid);
                continue;
            }

            // If drone effect ended or cleared, recall immediately
            if (!player.hasEffect(ModEffects.DRONE_PILOTING)) {
                recallPlayer(player, session, "§b🛸 [DRÓN] §eIrányítás véget ért. Pilóta visszatért a bázisra.");
                continue;
            }

            session.ticksRemaining--;

            // Keep flight enabled
            if (!player.getAbilities().flying) {
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
            }

            // 1. Drone Rotor Buzzing sound (every 12 ticks)
            if (session.ticksRemaining % 12 == 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEE_LOOP, SoundSource.PLAYERS, 0.8f, 1.9f);
            }

            // 2. Drone Rotor / Wing LED Particles
            Vec3 pos = player.position();
            Vec3 look = player.getLookAngle();
            Vec3 right = new Vec3(-look.z, 0, look.x).normalize();

            Vec3 leftWing = pos.subtract(right.scale(0.6)).add(0, 0.4, 0);
            Vec3 rightWing = pos.add(right.scale(0.6)).add(0, 0.4, 0);

            level.sendParticles(redLed, leftWing.x, leftWing.y, leftWing.z, 1, 0, 0, 0, 0);
            level.sendParticles(greenLed, rightWing.x, rightWing.y, rightWing.z, 1, 0, 0, 0, 0);

            // 3. Holographic marker at launch position (so you see where your body is stationed)
            if (session.ticksRemaining % 10 == 0) {
                level.sendParticles(ParticleTypes.GLOW,
                    session.launchPos.x, session.launchPos.y + 0.1, session.launchPos.z,
                    3, 0.2, 0.1, 0.2, 0.01);
            }

            // 4. Area Scanning for Mobs (every 4 ticks, 32m radius)
            AABB scanBox = player.getBoundingBox().inflate(32.0);
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, scanBox,
                e -> e != player && e.isAlive());

            for (LivingEntity target : nearby) {
                // Keep glowing while inside drone scan field
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 35, 0, false, false, false));

                if (session.scannedMobs.add(target.getUUID())) {
                    // New target detected! Radar ping!
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0f, 2.0f);

                    // Update MobTracker with newly spotted target
                    MobTrackerItem.setTrackedMob(player.getUUID(), target.getUUID());
                }
            }

            // 5. Action bar telemetry
            int pct = Math.max(0, (int) (((float) session.ticksRemaining / session.maxTicks) * 100));
            int secs = (session.ticksRemaining + 19) / 20;
            player.displayClientMessage(Component.literal(
                "§b🛸 DRÓN §8| §f🔋 AKKU: §e" + pct + "% (" + secs + "s) §8| §7MAGASSÁG: §b" +
                player.getBlockY() + "m §8| §e📡 CÉLPONTOK: §a" + nearby.size() + " db"
            ), true);

            // 6. Battery Depleted -> Automatic Safe Return
            if (session.ticksRemaining <= 0) {
                recallPlayer(player, session, "§b🛸 [DRÓN] §eAkkumulátor lemerült! Sikeres visszatérés a kiindulópontra.");
            }
        }
    }

    /**
     * Laser Tag: Fires precision targeting laser beam, marks target with glowing & towering sky-beacon pillar!
     */
    public static void triggerLaserTag(ServerPlayer player) {
        DroneSession session = SESSIONS.get(player.getUUID());
        if (session == null) return;

        ServerLevel level = player.serverLevel();
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double maxDist = 64.0;
        Vec3 reach = eyePos.add(look.scale(maxDist));

        // 1. Raycast against blocks
        BlockHitResult blockHit = level.clip(new ClipContext(
            eyePos, reach,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        ));

        Vec3 hitPos = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : reach;

        // 2. Raycast against entities
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            level, player, eyePos, reach,
            new AABB(eyePos, reach).inflate(1.2),
            e -> e instanceof LivingEntity && e != player && e.isAlive()
        );

        LivingEntity hitEntity = null;
        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity living) {
            hitEntity = living;
            hitPos = entityHit.getLocation();
        }

        // 3. Render High-Density Red Laser Beam
        double dist = eyePos.distanceTo(hitPos);
        int steps = Math.max((int) (dist * 3.0), 1);
        DustParticleOptions laserDot = new DustParticleOptions(new Vector3f(1.0f, 0.05f, 0.05f), 1.2f);

        for (int i = 1; i <= steps; i++) {
            double p = (double) i / steps;
            double x = eyePos.x + (hitPos.x - eyePos.x) * p;
            double y = eyePos.y + (hitPos.y - eyePos.y) * p;
            double z = eyePos.z + (hitPos.z - eyePos.z) * p;

            level.sendParticles(laserDot, x, y, z, 1, 0, 0, 0, 0);
            if (i % 6 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0, 0, 0, 0);
            }
        }

        // 4. Handle Hit
        if (hitEntity != null) {
            session.laserTaggedMob = hitEntity.getUUID();
            MobTrackerItem.setTrackedMob(player.getUUID(), hitEntity.getUUID());

            // Target glows for 60 seconds
            hitEntity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 60, 0, false, false));

            // Towering Sky-Beacon Light Beam (96 blocks straight into sky!)
            for (int dy = 0; dy <= 96; dy += 3) {
                level.sendParticles(ParticleTypes.END_ROD,
                    hitEntity.getX(), hitEntity.getY() + 1.0 + dy, hitEntity.getZ(),
                    1, 0, 0, 0, 0);
            }

            // Sound
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 2.0f);
            level.playSound(null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(),
                SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.5f, 1.2f);

            int distMeters = (int) player.distanceTo(hitEntity);
            player.sendSystemMessage(Component.literal(
                "§c🎯 [LÉZER CÉLMEGJELÖLŐ] §fZárolva: §e" + hitEntity.getName().getString() +
                " §8(§a" + distMeters + "m§8) | Beacon fénysugár aktív!"
            ));
        } else if (blockHit.getType() != HitResult.Type.MISS) {
            BlockPos bpos = blockHit.getBlockPos();
            String bname = level.getBlockState(bpos).getBlock().getName().getString();

            // Tag as GPS block waypoint
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.8f);

            for (int dy = 0; dy <= 48; dy += 3) {
                level.sendParticles(ParticleTypes.END_ROD,
                    bpos.getX() + 0.5, bpos.getY() + 1.0 + dy, bpos.getZ() + 0.5,
                    1, 0, 0, 0, 0);
            }

            player.sendSystemMessage(Component.literal(
                "§6📍 [LÉZER GPS JELÖLŐ] §eMegjelölt pont: §f" + bname + " [" + bpos.toShortString() + "]"
            ));
        }
    }

    /**
     * Kamikaze Self-Destruct: Tactical dive and massive explosion, then safe pilot return!
     */
    public static void triggerKamikaze(ServerPlayer player) {
        DroneSession session = SESSIONS.get(player.getUUID());
        if (session == null) return;

        ServerLevel level = player.serverLevel();
        Vec3 boomPos = player.position();

        // 1. Massive Tactical Detonation
        level.explode(null, boomPos.x, boomPos.y, boomPos.z, 4.0f, Level.ExplosionInteraction.MOB);

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, boomPos.x, boomPos.y, boomPos.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, boomPos.x, boomPos.y, boomPos.z, 30, 0.6, 0.6, 0.6, 0.05);
        level.sendParticles(ParticleTypes.FLAME, boomPos.x, boomPos.y, boomPos.z, 25, 0.5, 0.5, 0.5, 0.08);

        level.playSound(null, boomPos.x, boomPos.y, boomPos.z,
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 1.1f);
        level.playSound(null, boomPos.x, boomPos.y, boomPos.z,
            SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 2.0f, 0.8f);

        // 2. Safe Pilot Return to Launchpad
        recallPlayer(player, session,
            "§c💥 [KAMIKAZE DETONÁCIÓ] §fA drón felrobbantotta a célterületet! A pilóta biztonságban visszatért a bázisra."
        );
    }

    /**
     * Recall player safely back to launch coordinates
     */
    public static void recallPlayer(ServerPlayer player, DroneSession session, String reason) {
        SESSIONS.remove(player.getUUID());

        // Clear drone effects
        player.removeEffect(ModEffects.DRONE_PILOTING);
        player.removeEffect(MobEffects.INVISIBILITY);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);

        // Safe landing buffs (no fall damage)
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4, false, false, false));

        // Teleport back to exact launchpad
        player.teleportTo(session.launchPos.x, session.launchPos.y, session.launchPos.z);
        player.setYRot(session.launchYaw);
        player.setXRot(session.launchPitch);

        // Reset abilities
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
        }
        player.getAbilities().setFlyingSpeed(0.05f);
        player.onUpdateAbilities();

        // Sound effects
        ServerLevel level = player.serverLevel();
        level.playSound(null, session.launchPos.x, session.launchPos.y, session.launchPos.z,
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.2f, 1.5f);
        level.playSound(null, session.launchPos.x, session.launchPos.y, session.launchPos.z,
            SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0f, 1.5f);

        player.sendSystemMessage(Component.literal(reason));
    }
}
