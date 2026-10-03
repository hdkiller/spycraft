package com.hdkiller.spycraft.laser;

import com.hdkiller.spycraft.block.ModBlocks;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages active laser forcefield perimeters and independent traps in the world.
 * Each trap links ONLY nearby pylons (within MAX_LINK_DISTANCE = 32 blocks).
 * Distant pylons automatically form separate, independent laser traps.
 */
public class LaserForcefieldManager {

    public static final double MAX_LINK_DISTANCE = 32.0;
    public static final double MAX_LINK_DISTANCE_SQ = MAX_LINK_DISTANCE * MAX_LINK_DISTANCE;

    public static class LaserTrap {
        public final UUID id = UUID.randomUUID();
        public final UUID owner;
        public ResourceKey<Level> dimension;
        public final List<BlockPos> pylons = new ArrayList<>();
        public boolean active = false;

        public LaserTrap(UUID owner, ResourceKey<Level> dimension) {
            this.owner = owner;
            this.dimension = dimension;
        }

        public double getMinDistanceSqTo(BlockPos pos) {
            double minD = Double.MAX_VALUE;
            for (BlockPos p : pylons) {
                double d = p.distSqr(pos);
                if (d < minD) minD = d;
            }
            return minD;
        }

        public BlockPos getCenterPos() {
            if (pylons.isEmpty()) return BlockPos.ZERO;
            long x = 0, y = 0, z = 0;
            for (BlockPos p : pylons) {
                x += p.getX();
                y += p.getY();
                z += p.getZ();
            }
            return new BlockPos((int)(x / pylons.size()), (int)(y / pylons.size()), (int)(z / pylons.size()));
        }
    }

    public static class ForcefieldNetwork {
        public final UUID owner;
        public ResourceKey<Level> dimension;
        public final List<LaserTrap> traps = new CopyOnWriteArrayList<>();

        // Backward compatibility fields
        public final List<BlockPos> pylons = new ArrayList<>();
        public boolean active = false;

        public ForcefieldNetwork(UUID owner) {
            this.owner = owner;
        }

        public List<LaserTrap> getAllTraps() {
            if (traps.isEmpty() && !pylons.isEmpty()) {
                LaserTrap legacy = new LaserTrap(owner, dimension);
                legacy.pylons.addAll(pylons);
                legacy.active = active;
                return List.of(legacy);
            }
            return traps;
        }

        public LaserTrap findNearestTrap(BlockPos pos, double maxDist) {
            double maxDistSq = maxDist * maxDist;
            LaserTrap best = null;
            double bestDistSq = Double.MAX_VALUE;

            for (LaserTrap trap : getAllTraps()) {
                double d = trap.getMinDistanceSqTo(pos);
                if (d <= maxDistSq && d < bestDistSq) {
                    bestDistSq = d;
                    best = trap;
                }
            }
            return best;
        }
    }

    private static final Map<UUID, ForcefieldNetwork> NETWORKS = new ConcurrentHashMap<>();

    public static ForcefieldNetwork getNetwork(UUID owner) {
        return NETWORKS.computeIfAbsent(owner, ForcefieldNetwork::new);
    }

    public static void setTrapPylonsActive(Level level, LaserTrap trap, boolean active) {
        trap.active = active;
        if (level != null && !level.isClientSide) {
            for (BlockPos p : trap.pylons) {
                if (level.isLoaded(p)) {
                    var state = level.getBlockState(p);
                    if (state.is(ModBlocks.LASER_PYLON) && state.hasProperty(com.hdkiller.spycraft.block.LaserPylonBlock.ACTIVE)) {
                        if (state.getValue(com.hdkiller.spycraft.block.LaserPylonBlock.ACTIVE) != active) {
                            level.setBlock(p, state.setValue(com.hdkiller.spycraft.block.LaserPylonBlock.ACTIVE, active), Block.UPDATE_ALL);
                        }
                    }
                }
            }
        }
    }

    public static void setNetworkPylonsActive(Level level, ForcefieldNetwork network, boolean active) {
        network.active = active;
        for (LaserTrap trap : network.getAllTraps()) {
            setTrapPylonsActive(level, trap, active);
        }
    }

    public static void clearTrap(Level level, ForcefieldNetwork network, LaserTrap trap) {
        setTrapPylonsActive(level, trap, false);
        network.traps.remove(trap);
    }

    public static void clearNetwork(UUID owner, Level level) {
        ForcefieldNetwork net = NETWORKS.remove(owner);
        if (net != null) {
            for (LaserTrap trap : net.getAllTraps()) {
                setTrapPylonsActive(level, trap, false);
            }
            net.traps.clear();
            net.pylons.clear();
            net.active = false;
        }
    }

    public static void clearNetwork(UUID owner) {
        clearNetwork(owner, null);
    }

    public static void onPylonBroken(Level level, BlockPos pos) {
        for (ForcefieldNetwork net : NETWORKS.values()) {
            // Check legacy list
            if (net.pylons.remove(pos)) {
                net.active = false;
            }

            for (LaserTrap trap : net.traps) {
                if (trap.dimension != null && !trap.dimension.equals(level.dimension())) continue;
                if (trap.pylons.remove(pos)) {
                    boolean wasActive = trap.active;
                    setTrapPylonsActive(level, trap, false);

                    if (level instanceof ServerLevel slevel) {
                        slevel.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.5f, 0.6f);
                        slevel.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.2f, 1.2f);
                        slevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            15, 0.3, 0.3, 0.3, 0.05);

                        ServerPlayer ownerPlayer = slevel.getServer().getPlayerList().getPlayer(net.owner);
                        if (ownerPlayer != null) {
                            int trapIndex = net.traps.indexOf(trap) + 1;
                            if (wasActive) {
                                ownerPlayer.sendSystemMessage(Component.literal(
                                    "§c⚠️ [LÉZER ERŐPAJZS MEGSZŰNT] §7A(z) #" + trapIndex + " csapda egyik oszlopa le lett bontva, a lézerfal leállt!"
                                ));
                            } else {
                                ownerPlayer.sendSystemMessage(Component.literal(
                                    "§eℹ️ [LÉZEROSZLOP LEBONTVA] §7Oszlop kikerült a(z) #" + trapIndex + " csapdából. (Megmaradt: " + trap.pylons.size() + ")"
                                ));
                            }
                        }
                    }

                    if (trap.pylons.isEmpty()) {
                        net.traps.remove(trap);
                    }
                }
            }
        }
    }

    public static void tick(ServerLevel level) {
        for (ForcefieldNetwork net : NETWORKS.values()) {
            for (LaserTrap trap : net.getAllTraps()) {
                if (!trap.active || trap.pylons.size() < 2) continue;
                if (trap.dimension != null && !trap.dimension.equals(level.dimension())) continue;

                // Check if any pylon was destroyed or missing in loaded chunks
                boolean anyPylonRemoved = false;
                Iterator<BlockPos> it = trap.pylons.iterator();
                while (it.hasNext()) {
                    BlockPos p = it.next();
                    if (level.isLoaded(p)) {
                        if (!level.getBlockState(p).is(ModBlocks.LASER_PYLON)) {
                            it.remove();
                            anyPylonRemoved = true;
                        }
                    }
                }

                if (anyPylonRemoved) {
                    setTrapPylonsActive(level, trap, false);
                    ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(net.owner);
                    if (ownerPlayer != null) {
                        int trapIdx = net.traps.indexOf(trap) + 1;
                        ownerPlayer.sendSystemMessage(Component.literal(
                            "§c⚠️ [LÉZER ERŐPAJZS MEGSZŰNT] §7A(z) #" + trapIdx + " lézeroszlop megsemmisült, a lézerfal leállt!"
                        ));
                    }
                    continue;
                }

                List<BlockPos> pylons = trap.pylons;
                int count = pylons.size();
                if (count < 2) continue;

                // Periodic check to guarantee all loaded pylons stay visually red
                if (level.getGameTime() % 20 == 0) {
                    setTrapPylonsActive(level, trap, true);
                }

                // Emitter top energy sparks
                if (level.getGameTime() % 10 == 0) {
                    for (BlockPos p : pylons) {
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5,
                            2, 0.05, 0.05, 0.05, 0.02);
                    }
                }

                int segmentCount = (count == 2) ? 1 : count;
                boolean renderBeams = (level.getGameTime() % 2 == 0);

                double minX = Double.MAX_VALUE, minZ = Double.MAX_VALUE, minY = Double.MAX_VALUE;
                double maxX = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
                double cx = 0, cz = 0;
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

                // 1. Render Laser Walls
                for (int i = 0; i < segmentCount; i++) {
                    BlockPos p1 = pylons.get(i);
                    BlockPos p2 = pylons.get((i + 1) % count);

                    double dx = (p2.getX() - p1.getX());
                    double dy = (p2.getY() - p1.getY());
                    double dz = (p2.getZ() - p1.getZ());
                    double segmentLenSq = dx * dx + dz * dz;

                    // Distance constraint: only link nearby pylons
                    if (segmentLenSq > MAX_LINK_DISTANCE_SQ) {
                        continue;
                    }

                    if (renderBeams) {
                        double segmentLen = Math.sqrt(segmentLenSq);
                        int steps = Math.max((int) (segmentLen * 2.5), 1);

                        for (int s = 0; s <= steps; s++) {
                            double t = (double) s / steps;
                            double lx = (p1.getX() + 0.5) + dx * t;
                            double lz = (p1.getZ() + 0.5) + dz * t;
                            double baseLy = p1.getY() + dy * t;

                            for (double dY : new double[]{0.6, 1.6, 2.6, 3.6}) {
                                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                    lx, baseLy + dY, lz,
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

                    for (int i = 0; i < segmentCount; i++) {
                        BlockPos p1 = pylons.get(i);
                        BlockPos p2 = pylons.get((i + 1) % count);

                        double dx = (p2.getX() - p1.getX());
                        double dz = (p2.getZ() - p1.getZ());
                        double segLenSq = dx * dx + dz * dz;

                        if (segLenSq > MAX_LINK_DISTANCE_SQ) continue;

                        double segY = Math.min(p1.getY(), p2.getY());
                        if (ey < segY - 1.0 || ey > segY + 4.8) continue;

                        double ax = p1.getX() + 0.5;
                        double az = p1.getZ() + 0.5;
                        double bx = p2.getX() + 0.5;
                        double bz = p2.getZ() + 0.5;

                        double segDx = bx - ax;
                        double segDz = bz - az;

                        double t = segLenSq == 0 ? 0 : Math.max(0, Math.min(1, ((ex - ax) * segDx + (ez - az) * segDz) / segLenSq));
                        double closeX = ax + t * segDx;
                        double closeZ = az + t * segDz;

                        double distToWallSq = (ex - closeX) * (ex - closeX) + (ez - closeZ) * (ez - closeZ);

                        if (distToWallSq < 0.95 * 0.95) {
                            Vec3 pushDir;
                            if (count >= 3) {
                                double distEntityToCenter = (ex - cx) * (ex - cx) + (ez - cz) * (ez - cz);
                                double distWallToCenter = (closeX - cx) * (closeX - cx) + (closeZ - cz) * (closeZ - cz);
                                if (distEntityToCenter < distWallToCenter) {
                                    pushDir = new Vec3(cx - ex, 0, cz - ez).normalize();
                                } else {
                                    pushDir = new Vec3(ex - closeX, 0, ez - closeZ).normalize();
                                }
                            } else {
                                pushDir = new Vec3(ex - closeX, 0, ez - closeZ).normalize();
                            }

                            if (pushDir.lengthSqr() < 0.001) {
                                pushDir = new Vec3(0, 0, 1);
                            }

                            entity.setDeltaMovement(pushDir.x * 1.35, 0.28, pushDir.z * 1.35);
                            entity.hurtMarked = true;

                            level.playSound(null, ex, ey, ez,
                                SoundEvents.THORNS_HIT, SoundSource.BLOCKS, 1.2f, 1.8f);
                            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                ex, ey + 1.0, ez,
                                15, 0.2, 0.3, 0.2, 0.1);

                            entity.hurt(level.damageSources().magic(), 3.0f);
                            break;
                        }
                    }
                }
            }
        }
    }
}
