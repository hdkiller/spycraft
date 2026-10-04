package com.hdkiller.spycraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tactical Breaching Spray System (Liquid C4 Wall Foam)
 * Tracks sprayed wall blocks and triggers directional shaped breaches via Remote Detonator.
 */
public class BreachingSprayManager {
    public record BlockLocation(ResourceKey<Level> dimension, BlockPos pos) {}

    public static final int MAX_BLOCKS_PER_PLAYER = 24;
    private static final long EXPIRY_MS = 5 * 60 * 1000; // 5 minutes lifetime

    private static final Map<UUID, Set<BlockLocation>> SPRAYED_BLOCKS = new ConcurrentHashMap<>();
    private static final Map<BlockLocation, Long> BLOCK_TIMESTAMPS = new ConcurrentHashMap<>();

    public static boolean addSprayedBlock(Level level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0) {
            return false; // Bedrock or indestructible
        }

        UUID playerUuid = player.getUUID();
        Set<BlockLocation> blocks = SPRAYED_BLOCKS.computeIfAbsent(playerUuid, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()));

        if (blocks.size() >= MAX_BLOCKS_PER_PLAYER) {
            return false;
        }

        BlockLocation loc = new BlockLocation(level.dimension(), pos.immutable());
        boolean isNew = blocks.add(loc);
        BLOCK_TIMESTAMPS.put(loc, System.currentTimeMillis() + EXPIRY_MS);

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            // Sizzling foam application sound & particles
            serverLevel.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 1.8f);
            serverLevel.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.SPONGE_ABSORB, SoundSource.BLOCKS, 0.5f, 1.4f);

            serverLevel.sendParticles(ParticleTypes.SMALL_FLAME,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.35, 0.35, 0.35, 0.02);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.04);
        }

        return isNew;
    }

    public static int getSprayedBlockCount(UUID playerUuid) {
        Set<BlockLocation> set = SPRAYED_BLOCKS.get(playerUuid);
        return set != null ? set.size() : 0;
    }

    public static void clearSprayedBlocks(UUID playerUuid) {
        Set<BlockLocation> set = SPRAYED_BLOCKS.remove(playerUuid);
        if (set != null) {
            for (BlockLocation loc : set) {
                BLOCK_TIMESTAMPS.remove(loc);
            }
        }
    }

    public static int detonate(ServerLevel serverLevel, UUID playerUuid) {
        Set<BlockLocation> set = SPRAYED_BLOCKS.get(playerUuid);
        if (set == null || set.isEmpty()) {
            return 0;
        }

        ResourceKey<Level> dim = serverLevel.dimension();
        List<BlockPos> toDetonate = new ArrayList<>();

        for (BlockLocation loc : set) {
            if (loc.dimension().equals(dim)) {
                toDetonate.add(loc.pos());
            }
        }

        if (toDetonate.isEmpty()) {
            return 0;
        }

        Vec3 center = Vec3.ZERO;
        for (BlockPos p : toDetonate) {
            center = center.add(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
        }
        center = center.scale(1.0 / toDetonate.size());

        int count = 0;
        for (BlockPos p : toDetonate) {
            BlockLocation loc = new BlockLocation(dim, p);
            set.remove(loc);
            BLOCK_TIMESTAMPS.remove(loc);

            BlockState state = serverLevel.getBlockState(p);
            if (!state.isAir() && state.getDestroySpeed(serverLevel, p) >= 0) {
                // Destroy block with drops (clean breach hole!)
                serverLevel.destroyBlock(p, true);

                // Heavy breaching blast fx
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.25, 0.25, 0.25, 0.05);
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.35, 0.35, 0.35, 0.04);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.08);

                // Minor breach shockwave damage to nearby hostiles on the other side
                AABB shockwave = new AABB(p).inflate(1.5);
                List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, shockwave,
                    e -> e.isAlive() && !e.getUUID().equals(playerUuid));
                for (LivingEntity victim : nearby) {
                    victim.hurt(serverLevel.damageSources().explosion(null, null), 6.0f);
                }

                count++;
            }
        }

        // Heavy bass breach blast explosion audio
        serverLevel.playSound(null, center.x, center.y, center.z,
            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2.5f, 0.65f);
        serverLevel.playSound(null, center.x, center.y, center.z,
            SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.BLOCKS, 2.0f, 0.8f);

        if (set.isEmpty()) {
            SPRAYED_BLOCKS.remove(playerUuid);
        }

        return count;
    }

    public static void tick(ServerLevel level) {
        if (BLOCK_TIMESTAMPS.isEmpty()) return;

        long now = System.currentTimeMillis();
        ResourceKey<Level> dim = level.dimension();

        // Every 12 ticks, emit sizzling explosive foam indicator particles on primed blocks
        boolean emitParticles = (level.getGameTime() % 12 == 0);

        Iterator<Map.Entry<BlockLocation, Long>> it = BLOCK_TIMESTAMPS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockLocation, Long> entry = it.next();
            BlockLocation loc = entry.getKey();

            if (now > entry.getValue()) {
                it.remove();
                for (Set<BlockLocation> playerSet : SPRAYED_BLOCKS.values()) {
                    playerSet.remove(loc);
                }
                continue;
            }

            if (loc.dimension().equals(dim) && emitParticles) {
                BlockPos p = loc.pos();
                if (level.isLoaded(p) && !level.getBlockState(p).isAir()) {
                    level.sendParticles(ParticleTypes.SMALL_FLAME,
                        p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0.3, 0.3, 0.3, 0.01);
                }
            }
        }
    }
}
