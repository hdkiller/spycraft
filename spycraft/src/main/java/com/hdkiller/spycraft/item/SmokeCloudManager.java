package com.hdkiller.spycraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SmokeCloudManager {
    public static class SmokeCloud {
        final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        final Vec3 pos;
        final double radius;
        int remainingTicks;

        SmokeCloud(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, Vec3 pos, double radius, int ticks) {
            this.dimension = dimension;
            this.pos = pos;
            this.radius = radius;
            this.remainingTicks = ticks;
        }
    }

    private static final List<SmokeCloud> ACTIVE_CLOUDS = new CopyOnWriteArrayList<>();

    public static void spawnCloud(ServerLevel level, Vec3 pos, double radius, int durationTicks) {
        ACTIVE_CLOUDS.add(new SmokeCloud(level.dimension(), pos, radius, durationTicks));

        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.2f, 0.6f);
        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.5f, 0.8f);

        // Initial burst
        level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                pos.x, pos.y + 0.5, pos.z,
                40, radius * 0.4, 0.8, radius * 0.4, 0.05);
    }

    public static void tick(ServerLevel level) {
        if (ACTIVE_CLOUDS.isEmpty()) return;

        Iterator<SmokeCloud> it = ACTIVE_CLOUDS.iterator();
        while (it.hasNext()) {
            SmokeCloud cloud = it.next();

            if (!cloud.dimension.equals(level.dimension())) {
                continue;
            }

            cloud.remainingTicks--;

            // Generate smoke billows every 3 ticks
            if (cloud.remainingTicks % 3 == 0) {
                level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                        cloud.pos.x, cloud.pos.y + 0.5, cloud.pos.z,
                        12, cloud.radius * 0.5, 0.7, cloud.radius * 0.5, 0.02);
                level.sendParticles(ParticleTypes.POOF,
                        cloud.pos.x, cloud.pos.y + 0.2, cloud.pos.z,
                        6, cloud.radius * 0.4, 0.4, cloud.radius * 0.4, 0.03);
            }

            // Apply blindness & agro reset to mobs, and invisibility to players every 6 ticks
            if (cloud.remainingTicks % 6 == 0) {
                AABB box = new AABB(
                        cloud.pos.x - cloud.radius, cloud.pos.y - 1.0, cloud.pos.z - cloud.radius,
                        cloud.pos.x + cloud.radius, cloud.pos.y + 3.0, cloud.pos.z + cloud.radius);

                // Mobs: Blindness + Slowness + Agro Break
                List<Mob> mobs = level.getEntitiesOfClass(Mob.class, box, Mob::isAlive);
                for (Mob mob : mobs) {
                    mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0, false, false, false));
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false, false));
                    mob.setTarget(null); // Lose sight of player!
                }

                // Players: Concealed Invisibility
                List<Player> players = level.getEntitiesOfClass(Player.class, box, Player::isAlive);
                for (Player player : players) {
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, false));
                }
            }

            if (cloud.remainingTicks <= 0) {
                ACTIVE_CLOUDS.remove(cloud);
            }
        }
    }
    public static void clearRuntime() {
        ACTIVE_CLOUDS.clear();
    }
}
