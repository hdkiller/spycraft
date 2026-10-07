package com.hdkiller.spycraft.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tactical Frag Grenade Physics & Fuse Manager
 * - Simulates realistic ballistic flight arc with smoke & flame trail.
 * - Handles ground/wall impact bounce with metallic impact sounds.
 * - Manages delayed ground fuse timer (1.25s) with ticking fuse audio before detonation.
 */
public class FragGrenadeManager {

    public static class ActiveGrenade {
        final ResourceKey<Level> dimension;
        final UUID throwerUuid;
        Vec3 pos;
        Vec3 vel;
        int age = 0;
        int fuseTicks = -1; // -1 while in flight; counts down from 25 once landed
        boolean onGround = false;

        public ActiveGrenade(ResourceKey<Level> dimension, UUID throwerUuid, Vec3 pos, Vec3 vel) {
            this.dimension = dimension;
            this.throwerUuid = throwerUuid;
            this.pos = pos;
            this.vel = vel;
        }
    }

    private static final List<ActiveGrenade> ACTIVE_GRENADES = new CopyOnWriteArrayList<>();

    public static void throwGrenade(ServerLevel level, Player player, Vec3 eyePos, Vec3 vel) {
        // Offset starting position slightly forward so it doesn't clip the player
        Vec3 startPos = eyePos.add(vel.normalize().scale(0.5));
        ACTIVE_GRENADES.add(new ActiveGrenade(level.dimension(), player.getUUID(), startPos, vel));

        // Throw sounds
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.PLAYERS, 0.8f, 1.8f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.9f, 0.7f);
    }

    public static void tick(ServerLevel level) {
        if (ACTIVE_GRENADES.isEmpty()) return;

        Iterator<ActiveGrenade> it = ACTIVE_GRENADES.iterator();
        while (it.hasNext()) {
            ActiveGrenade g = it.next();
            if (!g.dimension.equals(level.dimension())) continue;

            g.age++;

            // Max airborne lifetime safety (4 seconds max if thrown into void or sky)
            if (g.age > 80 && g.fuseTicks < 0) {
                g.fuseTicks = 1;
            }

            // --- 1. COUNTDOWN STATE (GRENADE HAS LANDED ON GROUND) ---
            if (g.fuseTicks >= 0) {
                g.fuseTicks--;

                // Apply remaining ground roll friction
                if (g.vel.lengthSqr() > 0.001) {
                    Vec3 nextPos = g.pos.add(g.vel);
                    BlockHitResult hit = level.clip(new ClipContext(
                            g.pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
                    if (hit.getType() == HitResult.Type.MISS) {
                        g.pos = nextPos;
                    }
                    g.vel = g.vel.scale(0.6);
                }

                // Sizzling / smoking fuse particles while resting on the ground
                level.sendParticles(ParticleTypes.SMOKE,
                        g.pos.x, g.pos.y + 0.15, g.pos.z,
                        2, 0.08, 0.08, 0.08, 0.01);
                level.sendParticles(ParticleTypes.SMALL_FLAME,
                        g.pos.x, g.pos.y + 0.15, g.pos.z,
                        1, 0.04, 0.04, 0.04, 0.005);

                // Tense ticking / hissing audio as fuse counts down
                if (g.fuseTicks % 6 == 0) {
                    float pitch = 1.3f + (float) (25 - g.fuseTicks) * 0.035f;
                    level.playSound(null, g.pos.x, g.pos.y, g.pos.z,
                            SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 0.6f, pitch);
                }

                // Fuse expired -> DETONATE!
                if (g.fuseTicks <= 0) {
                    detonate(level, g);
                    ACTIVE_GRENADES.remove(g);
                }
                continue;
            }

            // --- 2. IN-FLIGHT BALLISTIC TRAJECTORY STATE ---
            // Gravity & air resistance
            g.vel = g.vel.add(0, -0.045, 0).scale(0.985);
            Vec3 nextPos = g.pos.add(g.vel);

            // Flight particles (smoke + sparks trailing the grenade in the air)
            level.sendParticles(ParticleTypes.SMOKE,
                    g.pos.x, g.pos.y + 0.08, g.pos.z,
                    1, 0.02, 0.02, 0.02, 0.002);
            level.sendParticles(ParticleTypes.SMALL_FLAME,
                    g.pos.x, g.pos.y + 0.08, g.pos.z,
                    1, 0.01, 0.01, 0.01, 0.002);
            if (g.age % 2 == 0) {
                level.sendParticles(ParticleTypes.CRIT,
                        g.pos.x, g.pos.y + 0.08, g.pos.z,
                        1, 0.02, 0.02, 0.02, 0.01);
            }

            // Check entity collision
            AABB stepBox = new AABB(g.pos, nextPos).inflate(0.5);
            Player thrower = level.getServer().getPlayerList().getPlayer(g.throwerUuid);
            List<Entity> hitEntities = level.getEntities(thrower, stepBox,
                    e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator());

            if (!hitEntities.isEmpty()) {
                Entity target = hitEntities.get(0);
                target.hurt(level.damageSources().thrown(null, thrower), 2.5f);
                target.setDeltaMovement(target.getDeltaMovement().add(g.vel.scale(0.3)));

                // Hit entity -> start fuse and bounce
                g.pos = target.position().add(0, 0.5, 0);
                g.vel = new Vec3(-g.vel.x * 0.3, 0.15, -g.vel.z * 0.3);
                g.fuseTicks = 20; // 1 second fuse on direct entity hit

                level.playSound(null, g.pos.x, g.pos.y, g.pos.z,
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4f, 1.8f);
                continue;
            }

            // Check block collision
            BlockHitResult blockHit = level.clip(new ClipContext(
                    g.pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));

            if (blockHit.getType() != HitResult.Type.MISS) {
                Vec3 hitLoc = blockHit.getLocation();
                g.pos = hitLoc;

                // Metallic clatter on ground/wall impact
                level.playSound(null, g.pos.x, g.pos.y, g.pos.z,
                        SoundEvents.COPPER_BULB_PLACE, SoundSource.PLAYERS, 1.2f, 1.5f);
                level.playSound(null, g.pos.x, g.pos.y, g.pos.z,
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.35f, 1.9f);

                // Bounce & roll physics
                switch (blockHit.getDirection().getAxis()) {
                    case Y -> {
                        // Landed on floor
                        double reboundY = Math.abs(g.vel.y) * 0.35;
                        g.vel = new Vec3(g.vel.x * 0.45, reboundY < 0.08 ? 0 : reboundY, g.vel.z * 0.45);
                    }
                    case X -> g.vel = new Vec3(-g.vel.x * 0.4, g.vel.y * 0.6, g.vel.z * 0.4);
                    case Z -> g.vel = new Vec3(g.vel.x * 0.4, g.vel.y * 0.6, -g.vel.z * 0.4);
                }

                // START GROUND FUSE COUNTDOWN (25 ticks = 1.25 seconds after landing!)
                g.fuseTicks = 25;
            } else {
                g.pos = nextPos;
            }
        }
    }

    private static void detonate(ServerLevel level, ActiveGrenade g) {
        Player thrower = level.getServer().getPlayerList().getPlayer(g.throwerUuid);

        // Lethal explosion
        level.explode(thrower, g.pos.x, g.pos.y + 0.1, g.pos.z, 3.8f, Level.ExplosionInteraction.MOB);

        // Shrapnel shockwave burst (30 high-speed crit & firework fragments)
        for (int i = 0; i < 30; i++) {
            double vx = (level.random.nextDouble() - 0.5) * 1.6;
            double vy = level.random.nextDouble() * 0.85 + 0.15;
            double vz = (level.random.nextDouble() - 0.5) * 1.6;
            level.sendParticles(ParticleTypes.CRIT, g.pos.x, g.pos.y + 0.2, g.pos.z,
                    0, vx, vy, vz, 0.9);
        }
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, g.pos.x, g.pos.y + 0.15, g.pos.z,
                14, 0.4, 0.3, 0.4, 0.06);
    }

    public static void clearRuntime() {
        ACTIVE_GRENADES.clear();
    }
}
