package com.erikcraft.drone;

import com.erikcraft.effect.ModEffects;
import com.erikcraft.item.MobTrackerItem;
import com.erikcraft.item.ModItems;
import com.erikcraft.item.ReconDroneItem;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
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
 * Manages active drone piloting sessions, 60s battery life, action mode routing,
 * tranquilizer sleep darts (5x), laser tagging, and kamikaze strikes.
 */
public class ReconDroneManager {
    public static final int DEFAULT_MAX_BATTERY = 1200; // 60 seconds (1 minute)
    public static final int DEFAULT_MAX_DARTS = 5;

    public record SleepingMobState(ResourceKey<Level> dimension, int ticksRemaining) {}

    private static final Map<UUID, DroneSession> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, SleepingMobState> SLEEPING_MOBS = new ConcurrentHashMap<>();

    public static class DroneSession {
        public final UUID playerUuid;
        public final Vec3 launchPos;
        public final float launchYaw;
        public final float launchPitch;
        public final ResourceKey<Level> dimension;
        public final int maxTicks;
        public int ticksRemaining;
        public int dartsRemaining;
        public final Set<UUID> scannedMobs = new HashSet<>();
        public UUID laserTaggedMob;

        public DroneSession(UUID playerUuid, Vec3 launchPos, float launchYaw, float launchPitch, ResourceKey<Level> dimension, int durationTicks, int darts) {
            this.playerUuid = playerUuid;
            this.launchPos = launchPos;
            this.launchYaw = launchYaw;
            this.launchPitch = launchPitch;
            this.dimension = dimension;
            this.maxTicks = durationTicks;
            this.ticksRemaining = durationTicks;
            this.dartsRemaining = darts;
        }
    }

    public static boolean isPiloting(UUID playerUuid) {
        return SESSIONS.containsKey(playerUuid);
    }

    public static DroneSession getSession(UUID playerUuid) {
        return SESSIONS.get(playerUuid);
    }

    /**
     * Start Drone Piloting Session with specific initial battery and darts
     */
    public static void startSession(ServerPlayer player, int durationTicks, int darts) {
        UUID uuid = player.getUUID();
        Vec3 launchPos = player.position();
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        DroneSession session = new DroneSession(uuid, launchPos, yaw, pitch, player.level().dimension(), durationTicks, darts);
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
            "§b🛸 [FELDERÍTŐ DRÓN] §fRendszerek bekapcsolva! Irányítás átvéve. §8(§e" + (durationTicks / 20) + "s akku | " + darts + "/5 altató lövedék§8)"
        ));
    }

    /**
     * Dispatch Drone Action depending on selected hotbar slot (1-4)
     */
    public static void handleDroneAction(ServerPlayer player) {
        DroneSession session = SESSIONS.get(player.getUUID());
        if (session == null) return;

        int slot = player.getInventory().selected;
        int mode = slot % 4;

        switch (mode) {
            case 0 -> triggerLaserTag(player);
            case 1 -> fireTranquilizerDart(player);
            case 2 -> triggerKamikaze(player);
            case 3 -> recallPlayer(player, session, "§b🛸 [DRÓN] §fKézi visszatérés parancs végrehajtva. Pilóta a bázison.");
        }
    }

    /**
     * Server tick handler for all active drone sessions & sleeping mobs
     */
    public static void tick(ServerLevel level) {
        // 1. Tick sleeping mobs and render Zzz note particles (dimension-isolated)
        if (!SLEEPING_MOBS.isEmpty()) {
            for (Map.Entry<UUID, SleepingMobState> entry : new HashMap<>(SLEEPING_MOBS).entrySet()) {
                UUID mobUuid = entry.getKey();
                SleepingMobState state = entry.getValue();

                if (!state.dimension().equals(level.dimension())) continue;

                var entity = level.getEntity(mobUuid);
                int time = state.ticksRemaining() - 1;

                if (entity == null || !entity.isAlive() || !(entity instanceof LivingEntity mob) || time <= 0) {
                    SLEEPING_MOBS.remove(mobUuid);
                } else {
                    SLEEPING_MOBS.put(mobUuid, new SleepingMobState(state.dimension(), time));
                    if (time % 10 == 0) {
                        level.sendParticles(ParticleTypes.NOTE,
                            mob.getX(), mob.getEyeY() + 0.4, mob.getZ(),
                            1, 0.15, 0.15, 0.15, 0.05);
                    }
                }
            }
        }

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

            // Void safety check: if player drops below world min height or enters void
            if (player.getY() < level.getMinBuildHeight() - 5) {
                recallPlayer(player, session, "§c⚠️ [VÉSZLEÁLLÍTÁS] A drón zuhanni kezdett a mélységbe! Vészhelyzeti visszatérés aktiválva.");
                continue;
            }

            // Dimension safety check
            if (!player.level().dimension().equals(session.dimension)) {
                recallPlayer(player, session, "§c⚠️ [VÉSZLEÁLLÍTÁS] Dimenzióváltás észlelve! A drón visszatért a kiindulópontra.");
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

            // Rotor buzzing sound (every 14 ticks)
            if (session.ticksRemaining % 14 == 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEE_LOOP, SoundSource.PLAYERS, 0.8f, 1.9f);
            }

            // Rotor Wing LED Particles
            Vec3 pos = player.position();
            Vec3 look = player.getLookAngle();
            Vec3 right = new Vec3(-look.z, 0, look.x).normalize();

            Vec3 leftWing = pos.subtract(right.scale(0.6)).add(0, 0.4, 0);
            Vec3 rightWing = pos.add(right.scale(0.6)).add(0, 0.4, 0);

            level.sendParticles(redLed, leftWing.x, leftWing.y, leftWing.z, 1, 0, 0, 0, 0);
            level.sendParticles(greenLed, rightWing.x, rightWing.y, rightWing.z, 1, 0, 0, 0, 0);

            // Holographic marker at launch position
            if (session.ticksRemaining % 10 == 0) {
                level.sendParticles(ParticleTypes.GLOW,
                    session.launchPos.x, session.launchPos.y + 0.1, session.launchPos.z,
                    3, 0.2, 0.1, 0.2, 0.01);
            }

            // Area Scanning for Mobs (every 4 ticks, 32m radius)
            AABB scanBox = player.getBoundingBox().inflate(32.0);
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, scanBox,
                e -> e != player && e.isAlive());

            for (LivingEntity target : nearby) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 35, 0, false, false, false));

                if (session.scannedMobs.add(target.getUUID())) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0f, 2.0f);
                    MobTrackerItem.setTrackedMob(player.getUUID(), target.getUUID());
                }
            }

            // Action bar telemetry
            int pct = Math.max(0, (int) (((float) session.ticksRemaining / session.maxTicks) * 100));
            int secs = (session.ticksRemaining + 19) / 20;
            player.displayClientMessage(Component.literal(
                "§b🛸 DRÓN §8| §f🔋 AKKU: §e" + pct + "% (" + secs + "s) §8| §3💤 LŐSZER: §b" + session.dartsRemaining + "/5 §8| §e📡 CÉLOK: §a" + nearby.size() + " db"
            ), true);

            // Battery Depleted -> Automatic Safe Return
            if (session.ticksRemaining <= 0) {
                recallPlayer(player, session, "§b🛸 [DRÓN] §eAkkumulátor lemerült! Sikeres visszatérés a kiindulópontra.");
            }
        }
    }

    /**
     * Tranquilizer Dart: Fires pneumatic sleeping dart (max 5x). Freezes mob asleep for 15 seconds!
     */
    public static void fireTranquilizerDart(ServerPlayer player) {
        DroneSession session = SESSIONS.get(player.getUUID());
        if (session == null) return;

        if (session.dartsRemaining <= 0) {
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0f, 1.5f);
            player.displayClientMessage(Component.literal("§c⚠️ [KILŐVE] Nincs több altató lövedék a drónban! (0/5)"), true);
            return;
        }

        session.dartsRemaining--;
        ServerLevel level = player.serverLevel();
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double maxDist = 48.0;
        Vec3 reach = eyePos.add(look.scale(maxDist));

        // Pneumatic dart sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.2f, 1.8f);

        BlockHitResult blockHit = level.clip(new ClipContext(
            eyePos, reach,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        ));

        Vec3 hitPos = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : reach;

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

        // Dart trail particles
        double dist = eyePos.distanceTo(hitPos);
        int steps = Math.max((int) (dist * 2.5), 1);
        DustParticleOptions dartVapor = new DustParticleOptions(new Vector3f(0.1f, 0.9f, 0.9f), 0.9f);

        for (int i = 1; i <= steps; i++) {
            double p = (double) i / steps;
            double x = eyePos.x + (hitPos.x - eyePos.x) * p;
            double y = eyePos.y + (hitPos.y - eyePos.y) * p;
            double z = eyePos.z + (hitPos.z - eyePos.z) * p;
            level.sendParticles(dartVapor, x, y, z, 1, 0, 0, 0, 0);
        }

        if (hitEntity != null) {
            // Apply 15-second deep sleep / tranquilizer paralysis
            hitEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 255, false, false));
            hitEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 255, false, false));
            hitEntity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 300, 0, false, false));

            if (hitEntity instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }

            SLEEPING_MOBS.put(hitEntity.getUUID(), new SleepingMobState(level.dimension(), 300));

            // Hit sound
            level.playSound(null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(),
                SoundEvents.FOX_SLEEP, SoundSource.PLAYERS, 1.5f, 1.2f);
            level.playSound(null, hitEntity.getX(), hitEntity.getY(), hitEntity.getZ(),
                SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.2f, 1.5f);

            // Zzz note particles
            level.sendParticles(ParticleTypes.NOTE,
                hitEntity.getX(), hitEntity.getEyeY() + 0.5, hitEntity.getZ(),
                8, 0.3, 0.3, 0.3, 0.1);

            player.sendSystemMessage(Component.literal(
                "§3💤 [ALTATÓ LÖVEDÉK] §fCélpont elaltatva: §e" + hitEntity.getName().getString() +
                " §8(15mp alvás!) §7[Lőszer: §b" + session.dartsRemaining + "/5§7]"
            ));
        } else {
            // Missed / hit block
            level.sendParticles(ParticleTypes.CRIT, hitPos.x, hitPos.y, hitPos.z, 6, 0.1, 0.1, 0.1, 0.05);
            player.sendSystemMessage(Component.literal(
                "§7[Altató lövedék mellément] §8| Hátralévő lőszer: §b" + session.dartsRemaining + "/5"
            ));
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

        BlockHitResult blockHit = level.clip(new ClipContext(
            eyePos, reach,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        ));

        Vec3 hitPos = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : reach;

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

        if (hitEntity != null) {
            session.laserTaggedMob = hitEntity.getUUID();
            MobTrackerItem.setTrackedMob(player.getUUID(), hitEntity.getUUID());

            hitEntity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 60, 0, false, false));

            for (int dy = 0; dy <= 96; dy += 3) {
                level.sendParticles(ParticleTypes.END_ROD,
                    hitEntity.getX(), hitEntity.getY() + 1.0 + dy, hitEntity.getZ(),
                    1, 0, 0, 0, 0);
            }

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

        level.explode(null, boomPos.x, boomPos.y, boomPos.z, 4.0f, Level.ExplosionInteraction.MOB);

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, boomPos.x, boomPos.y, boomPos.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, boomPos.x, boomPos.y, boomPos.z, 30, 0.6, 0.6, 0.6, 0.05);
        level.sendParticles(ParticleTypes.FLAME, boomPos.x, boomPos.y, boomPos.z, 25, 0.5, 0.5, 0.5, 0.08);

        level.playSound(null, boomPos.x, boomPos.y, boomPos.z,
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 1.1f);
        level.playSound(null, boomPos.x, boomPos.y, boomPos.z,
            SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 2.0f, 0.8f);

        // When kamikaze is executed, battery is set to 0
        session.ticksRemaining = 0;

        recallPlayer(player, session,
            "§c💥 [KAMIKAZE DETONÁCIÓ] §fA drón felrobbantotta a célterületet! A pilóta biztonságban visszatért a bázisra."
        );
    }

    /**
     * Recall player safely back to launch coordinates & persist battery/ammo on drone item
     */
    public static void recallPlayer(ServerPlayer player, DroneSession session, String reason) {
        SESSIONS.remove(player.getUUID());

        // Update battery and darts on the drone item in player's inventory
        for (ItemStack item : player.getInventory().items) {
            if (item.is(ModItems.RECON_DRONE)) {
                ReconDroneItem.setBattery(item, session.ticksRemaining);
                ReconDroneItem.setDarts(item, session.dartsRemaining);
                break;
            }
        }

        // Clear drone effects
        player.removeEffect(ModEffects.DRONE_PILOTING);
        player.removeEffect(MobEffects.INVISIBILITY);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);

        // Safe landing buffs (no fall damage)
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4, false, false, false));

        // Teleport back to exact launchpad (dimension-safe)
        ServerLevel launchLevel = player.getServer() != null ? player.getServer().getLevel(session.dimension) : null;
        if (launchLevel != null && player.serverLevel() != launchLevel) {
            player.teleportTo(launchLevel, session.launchPos.x, session.launchPos.y, session.launchPos.z, session.launchYaw, session.launchPitch);
        } else {
            player.teleportTo(session.launchPos.x, session.launchPos.y, session.launchPos.z);
            player.setYRot(session.launchYaw);
            player.setXRot(session.launchPitch);
        }

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

    /**
     * Handle player disconnect: clean up active session and restore abilities
     */
    public static void onPlayerDisconnect(ServerPlayer player) {
        DroneSession session = SESSIONS.get(player.getUUID());
        if (session != null) {
            recallPlayer(player, session, "§b🛸 [DRÓN] §eA pilóta bontotta a kapcsolatot. Drón visszatért a bázisra.");
        }
    }
}
