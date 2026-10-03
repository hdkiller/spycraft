package com.erikcraft.sound;

import com.erikcraft.block.ModBlocks;
import com.erikcraft.block.SoundTrapBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sonic Decoy Sound Trap Manager
 * Plays realistic Creeper hiss audio, lures all nearby mobs within 32 blocks,
 * and deceives other players investigating the suspicious sounds.
 */
public class SoundTrapManager {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> ACTIVE_TRAPS = new ConcurrentHashMap<>();

    public static void registerTrap(Level level, BlockPos pos) {
        ACTIVE_TRAPS.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos);
    }

    public static void removeTrap(Level level, BlockPos pos) {
        Set<BlockPos> traps = ACTIVE_TRAPS.get(level.dimension());
        if (traps != null) {
            traps.remove(pos);
        }
    }

    public static void tick(ServerLevel level) {
        Set<BlockPos> traps = ACTIVE_TRAPS.get(level.dimension());
        if (traps == null || traps.isEmpty()) return;

        long gameTime = level.getGameTime();

        for (BlockPos pos : new ArrayList<>(traps)) {
            if (!level.isLoaded(pos)) continue;

            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.SOUND_TRAP)) {
                traps.remove(pos);
                continue;
            }

            if (!state.getValue(SoundTrapBlock.ACTIVE)) {
                traps.remove(pos);
                continue;
            }

            // 1. Creeper hiss audio: plays every 60 ticks (3 seconds) with high volume
            if (gameTime % 60 == 0) {
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.CREEPER_PRIMED, SoundSource.BLOCKS, 2.5f, 1.0f);
            }

            // 2. Sonic ripple particles
            if (gameTime % 15 == 0) {
                level.sendParticles(ParticleTypes.NOTE,
                    pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5,
                    3, 0.25, 0.25, 0.25, 0.1);
                level.sendParticles(ParticleTypes.SCULK_CHARGE_POP,
                    pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5,
                    2, 0.1, 0.1, 0.1, 0.02);
            }

            // 3. Mob Attraction: every 20 ticks (1 second), lure mobs within 32 blocks
            if (gameTime % 20 == 0) {
                AABB searchBox = new AABB(pos).inflate(32.0);
                List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class, searchBox, Mob::isAlive);

                for (Mob mob : nearbyMobs) {
                    double distSqr = mob.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

                    // Force mob navigation directly towards the Sound Trap
                    mob.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.25);

                    // If close (< 4 blocks), stare directly at the speaker
                    if (distSqr < 16.0) {
                        mob.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    }
                }
            }
        }
    }
}
