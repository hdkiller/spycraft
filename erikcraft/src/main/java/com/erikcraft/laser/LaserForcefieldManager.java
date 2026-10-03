package com.erikcraft.laser;

import com.erikcraft.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active laser forcefield perimeters in the world.
 */
public class LaserForcefieldManager {
    public static class ForcefieldNetwork {
        public final UUID owner;
        public ResourceKey<Level> dimension;
        public final List<BlockPos> pylons = new ArrayList<>();
        public boolean active = false;

        public ForcefieldNetwork(UUID owner) {
            this.owner = owner;
        }
    }

    private static final Map<UUID, ForcefieldNetwork> NETWORKS = new ConcurrentHashMap<>();

    public static ForcefieldNetwork getNetwork(UUID owner) {
        return NETWORKS.computeIfAbsent(owner, ForcefieldNetwork::new);
    }

    public static void clearNetwork(UUID owner) {
        NETWORKS.remove(owner);
    }

    public static void onPylonBroken(Level level, BlockPos pos) {
        for (ForcefieldNetwork net : NETWORKS.values()) {
            if (net.dimension != null && net.dimension.equals(level.dimension())) {
                if (net.pylons.remove(pos)) {
                    if (net.active && net.pylons.size() < 3) {
                        net.active = false;
                        if (level instanceof ServerLevel slevel) {
                            ServerPlayer ownerPlayer = slevel.getServer().getPlayerList().getPlayer(net.owner);
                            if (ownerPlayer != null) {
                                ownerPlayer.sendSystemMessage(Component.literal(
                                    "§c⚠️ [LÉZER ERŐPAJZS MEGSZŰNT] §7Egy lézeroszlop le lett bontva, a pajzs leállt!"
                                ));
                            }
                            slevel.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.5f, 0.6f);
                        }
                    }
                }
            }
        }
    }

    public static void tick(ServerLevel level) {
        for (ForcefieldNetwork net : NETWORKS.values()) {
            if (!net.active || net.pylons.size() < 3) continue;
            if (net.dimension != null && !net.dimension.equals(level.dimension())) continue;

            // Check if any pylon was destroyed or missing in loaded chunks
            boolean anyPylonRemoved = false;
            Iterator<BlockPos> it = net.pylons.iterator();
            while (it.hasNext()) {
                BlockPos p = it.next();
                if (level.isLoaded(p)) {
                    if (!level.getBlockState(p).is(ModBlocks.LASER_PYLON)) {
                        it.remove();
                        anyPylonRemoved = true;
                    }
                }
            }

            if (anyPylonRemoved && net.pylons.size() < 3) {
                net.active = false;
                ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(net.owner);
                if (ownerPlayer != null) {
                    ownerPlayer.sendSystemMessage(Component.literal(
                        "§c⚠️ [LÉZER ERŐPAJZS MEGSZŰNT] §7Az egyik lézeroszlop megsemmisült, a pajzs leállt!"
                    ));
                }
                continue;
            }

            List<BlockPos> pylons = net.pylons;
            int count = pylons.size();

            // Calculate centroid for repulsion physics
            double cx = 0, cz = 0;
            double minX = Double.MAX_VALUE, minZ = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

            for (BlockPos p : pylons) {
                cx += p.getX() + 0.5;
                cz += p.getZ() + 0.5;
                minX = Math.min(minX, p.getX());
                minZ = Math.min(minZ, p.getZ());
                minY = Math.min(minY, p.getY());
                maxX = Math.max(maxX, p.getX() + 1);
                maxZ = Math.max(maxZ, p.getZ() + 1);
                maxY = Math.max(maxY, p.getY() + 5);
            }
            cx /= count;
            cz /= count;
            Vec3 centroid = new Vec3(cx, minY, cz);

            // 1. Render Laser Walls (every 2 ticks for smooth performance)
            if (level.getGameTime() % 2 == 0) {
                for (int i = 0; i < count; i++) {
                    BlockPos p1 = pylons.get(i);
                    BlockPos p2 = pylons.get((i + 1) % count);

                    double dx = (p2.getX() - p1.getX());
                    double dz = (p2.getZ() - p1.getZ());
                    double segmentLen = Math.sqrt(dx * dx + dz * dz);
                    int steps = Math.max((int) (segmentLen * 2.5), 1);

                    for (int s = 0; s <= steps; s++) {
                        double t = (double) s / steps;
                        double lx = (p1.getX() + 0.5) + dx * t;
                        double lz = (p1.getZ() + 0.5) + dz * t;
                        double baseLy = p1.getY() + (p2.getY() - p1.getY()) * t;

                        // 4 horizontal laser beams (y: +0.6, +1.6, +2.6, +3.6)
                        for (double dy : new double[]{0.6, 1.6, 2.6, 3.6}) {
                            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                lx, baseLy + dy, lz,
                                1, 0, 0, 0, 0);
                        }
                    }
                }
            }

            // 2. Collision & Impassable Repulsion Barrier
            AABB area = new AABB(minX - 2, minY - 1, minZ - 2, maxX + 2, maxY + 2, maxZ + 2);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive);

            for (LivingEntity entity : entities) {
                double ex = entity.getX();
                double ey = entity.getY();
                double ez = entity.getZ();

                for (int i = 0; i < count; i++) {
                    BlockPos p1 = pylons.get(i);
                    BlockPos p2 = pylons.get((i + 1) % count);

                    double segY = Math.min(p1.getY(), p2.getY());
                    if (ey < segY - 1.0 || ey > segY + 4.8) continue;

                    // Distance from point (ex, ez) to segment (p1 -> p2)
                    double ax = p1.getX() + 0.5;
                    double az = p1.getZ() + 0.5;
                    double bx = p2.getX() + 0.5;
                    double bz = p2.getZ() + 0.5;

                    double segDx = bx - ax;
                    double segDz = bz - az;
                    double segLenSq = segDx * segDx + segDz * segDz;

                    double t = segLenSq == 0 ? 0 : Math.max(0, Math.min(1, ((ex - ax) * segDx + (ez - az) * segDz) / segLenSq));
                    double closeX = ax + t * segDx;
                    double closeZ = az + t * segDz;

                    double distToWallSq = (ex - closeX) * (ex - closeX) + (ez - closeZ) * (ez - closeZ);

                    // If entity enters laser wall barrier (within 0.95 blocks)
                    if (distToWallSq < 0.95 * 0.95) {
                        // Repel back: calculate whether to push inside or outside
                        double distEntityToCenter = (ex - cx) * (ex - cx) + (ez - cz) * (ez - cz);
                        double distWallToCenter = (closeX - cx) * (closeX - cx) + (closeZ - cz) * (closeZ - cz);

                        Vec3 pushDir;
                        if (distEntityToCenter < distWallToCenter) {
                            // Entity was INSIDE -> push back towards center!
                            pushDir = new Vec3(cx - ex, 0, cz - ez).normalize();
                        } else {
                            // Entity was OUTSIDE -> push away from wall outwards!
                            pushDir = new Vec3(ex - closeX, 0, ez - closeZ).normalize();
                        }

                        // Apply strong bounce velocity
                        entity.setDeltaMovement(pushDir.x * 1.35, 0.28, pushDir.z * 1.35);
                        entity.hurtMarked = true;

                        // Electric shock sound & particles
                        level.playSound(null, ex, ey, ez,
                            SoundEvents.THORNS_HIT, SoundSource.BLOCKS, 1.2f, 1.8f);
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            ex, ey + 1.0, ez,
                            15, 0.2, 0.3, 0.2, 0.1);

                        // Electric zap damage (1.5 hearts)
                        entity.hurt(level.damageSources().magic(), 3.0f);
                        break;
                    }
                }
            }
        }
    }
}
