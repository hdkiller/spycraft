package com.hdkiller.spycraft.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HologramDecoyManager {
    private static class DecoyEntry {
        final java.util.UUID uuid;
        final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        int remainingTicks;

        DecoyEntry(ArmorStand stand, int ticks) {
            this.uuid = stand.getUUID();
            this.dimension = stand.level().dimension();
            this.remainingTicks = ticks;
        }
    }

    private static final List<DecoyEntry> ACTIVE_DECOYS = new CopyOnWriteArrayList<>();

    public static void registerDecoy(ArmorStand stand, int durationTicks) {
        ACTIVE_DECOYS.add(new DecoyEntry(stand, durationTicks));
    }

    public static void tick(ServerLevel level) {
        if (ACTIVE_DECOYS.isEmpty()) return;

        Iterator<DecoyEntry> it = ACTIVE_DECOYS.iterator();
        while (it.hasNext()) {
            DecoyEntry entry = it.next();
            if (!entry.dimension.equals(level.dimension())) continue;
            var entity = level.getEntity(entry.uuid);
            if (!(entity instanceof ArmorStand stand)) {
                ACTIVE_DECOYS.remove(entry);
                continue;
            }

            if (stand.isRemoved() || !stand.isAlive()) {
                ACTIVE_DECOYS.remove(entry);
                continue;
            }

            if (stand.level() != level) continue;

            entry.remainingTicks--;

            // Particle effect every 4 ticks
            if (entry.remainingTicks % 4 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        stand.getX(), stand.getY() + 1.0, stand.getZ(),
                        3, 0.3, 0.5, 0.3, 0.02);
            }

            // Mob aggro lure every 10 ticks
            if (entry.remainingTicks % 10 == 0) {
                var mobs = level.getEntitiesOfClass(Mob.class, stand.getBoundingBox().inflate(20.0),
                        mob -> mob.isAlive() && !mob.isAlliedTo(stand));
                for (Mob mob : mobs) {
                    mob.setTarget(stand);
                }
            }

            // Expiration
            if (entry.remainingTicks <= 0) {
                level.playSound(null, stand.getX(), stand.getY(), stand.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 1.2f);
                level.sendParticles(ParticleTypes.POOF,
                        stand.getX(), stand.getY() + 1.0, stand.getZ(),
                        15, 0.3, 0.5, 0.3, 0.05);
                stand.discard();
                ACTIVE_DECOYS.remove(entry);
            }
        }
    }
    public static void clearRuntime() {
        ACTIVE_DECOYS.clear();
    }
}
