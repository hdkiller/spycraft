package com.hdkiller.spycraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tactical Spy Tracker & GPS Beacon
 * - Right-click any MOB/PLAYER: Plants a live tracking bug (glows through walls).
 * - Right-click any BLOCK: Plants a hidden GPS spy beacon (secret base, vault, chest).
 * - Right-click AIR: Pings radar for distance, compass heading, and live location!
 */
public class MobTrackerItem extends Item {
    private static final Map<UUID, UUID> TRACKED_MOBS = new ConcurrentHashMap<>();
    private static final Map<UUID, TrackedBlock> TRACKED_BLOCKS = new ConcurrentHashMap<>();

    public record TrackedBlock(ResourceKey<Level> dimension, BlockPos pos, String name) {}

    public MobTrackerItem(Properties properties) {
        super(properties);
    }

    public static TrackedBlock getTrackedBlock(UUID playerUuid) {
        return TRACKED_BLOCKS.get(playerUuid);
    }

    public static LivingEntity getTrackedMob(UUID playerUuid, Level level) {
        UUID mobId = TRACKED_MOBS.get(playerUuid);
        if (mobId != null && level instanceof ServerLevel serverLevel) {
            var entity = serverLevel.getEntity(mobId);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                return living;
            }
        }
        return null;
    }

    public static void setTrackedMob(UUID playerUuid, UUID mobId) {
        TRACKED_MOBS.put(playerUuid, mobId);
    }

    /**
     * Plant tracking bug on a MOB or PLAYER
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Level level = player.level();

        if (!level.isClientSide) {
            TRACKED_MOBS.put(player.getUUID(), target.getUUID());

            // Target glows through walls for 10 minutes
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 60 * 10, 0, false, false));

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getEyeY(), target.getZ(),
                    20, 0.2, 0.2, 0.2, 0.05);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 2.0f);
            }

            player.sendSystemMessage(Component.literal(
                "§2[Spy Bug Planted!] §aNow tracking mob: §f" + target.getName().getString() + " §7(Glows through walls!)"
            ));

            player.getCooldowns().addCooldown(this, 10);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Plant GPS beacon on any BLOCK (Base, Chest, Door, Secret Hideout)
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        String blockName = state.getBlock().getName().getString();

        if (!level.isClientSide) {
            TRACKED_BLOCKS.put(player.getUUID(), new TrackedBlock(level.dimension(), pos, blockName));

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    30, 0.3, 0.3, 0.3, 0.5);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    15, 0.2, 0.2, 0.2, 0.05);

                serverLevel.playSound(null, pos.getX(), pos.getY(), pos.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.8f);
            }

            player.sendSystemMessage(Component.literal(
                "§6[GPS Beacon Planted!] §eTagged block: §f" + blockName +
                " §7at [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]"
            ));

            player.getCooldowns().addCooldown(this, 10);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Ping Radar (Right-click in air)
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            LivingEntity targetMob = getTrackedMob(player.getUUID(), level);
            TrackedBlock trackedBlock = getTrackedBlock(player.getUUID());

            // Radar Ping Sound
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.2f, 1.8f);

            boolean pingedAny = false;

            // 1. Report Mob Signal if active
            if (targetMob != null && targetMob.isAlive()) {
                if (targetMob.level().dimension().equals(level.dimension())) {
                    pingedAny = true;
                    int dist = (int) player.distanceTo(targetMob);
                    targetMob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 15, 0, false, false));

                    double dx = targetMob.getX() - player.getX();
                    double dz = targetMob.getZ() - player.getZ();
                    String heading = getDirectionLabel(dx, dz);
                    int elevationDiff = (int) (targetMob.getY() - player.getY());
                    String elevation = elevationDiff > 1 ? " [Above ⬆]" : (elevationDiff < -1 ? " [Below ⬇]" : " [Level ➡]");

                    player.sendSystemMessage(Component.literal(
                        "§b📡 [MOB RADAR] §f" + targetMob.getName().getString() +
                        " §8| §e" + dist + "m §8| §a" + heading + elevation +
                        " §8[X: " + targetMob.getBlockX() + ", Y: " + targetMob.getBlockY() + ", Z: " + targetMob.getBlockZ() + "]"
                    ));
                } else {
                    pingedAny = true;
                    player.sendSystemMessage(Component.literal(
                        "§b📡 [MOB RADAR] §f" + targetMob.getName().getString() +
                        " §8| §cMásik dimenzióban található! (" + targetMob.level().dimension().location().getPath() + ")"
                    ));
                }
            }

            // 2. Report Block Beacon Signal if active
            if (trackedBlock != null) {
                if (trackedBlock.dimension().equals(level.dimension())) {
                    pingedAny = true;
                    BlockPos bpos = trackedBlock.pos();
                    double dist = Math.sqrt(player.blockPosition().distSqr(bpos));

                    double dx = (bpos.getX() + 0.5) - player.getX();
                    double dz = (bpos.getZ() + 0.5) - player.getZ();
                    String heading = getDirectionLabel(dx, dz);
                    int elevationDiff = bpos.getY() - player.getBlockY();
                    String elevation = elevationDiff > 1 ? " [Above ⬆]" : (elevationDiff < -1 ? " [Below ⬇]" : " [Level ➡]");

                    if (level instanceof ServerLevel serverLevel && dist < 120) {
                        serverLevel.sendParticles(ParticleTypes.END_ROD,
                            bpos.getX() + 0.5, bpos.getY() + 1.2, bpos.getZ() + 0.5,
                            10, 0.1, 0.3, 0.1, 0.05);
                    }

                    player.sendSystemMessage(Component.literal(
                        "§6📍 [BASE BEACON] §f" + trackedBlock.name() +
                        " §8| §e" + (int) dist + "m §8| §a" + heading + elevation +
                        " §8[X: " + bpos.getX() + ", Y: " + bpos.getY() + ", Z: " + bpos.getZ() + "]"
                    ));
                } else {
                    pingedAny = true;
                    player.sendSystemMessage(Component.literal(
                        "§6📍 [BASE BEACON] §f" + trackedBlock.name() +
                        " §8| §cMásik dimenzióban található! (" + trackedBlock.dimension().location().getPath() + ")"
                    ));
                }
            }

            // 3. Fallback: if nothing tracked yet, auto-scan for nearest living mob
            if (!pingedAny) {
                AABB scanArea = player.getBoundingBox().inflate(40.0);
                List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, scanArea, e -> e != player && e.isAlive());
                LivingEntity nearest = nearby.stream().min(Comparator.comparingDouble(player::distanceTo)).orElse(null);

                if (nearest != null) {
                    TRACKED_MOBS.put(player.getUUID(), nearest.getUUID());
                    nearest.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 15, 0, false, false));
                    int dist = (int) player.distanceTo(nearest);
                    String heading = getDirectionLabel(nearest.getX() - player.getX(), nearest.getZ() - player.getZ());

                    player.sendSystemMessage(Component.literal(
                        "§3📡 [AUTO-SCAN] §7Detected nearest entity: §f" + nearest.getName().getString() +
                        " §8| §e" + dist + "m §8| §a" + heading
                    ));
                } else {
                    player.sendSystemMessage(Component.literal(
                        "§c📡 [SPY RADAR] §7No signals! Right-click a mob (bug) or a block (base beacon) to track."
                    ));
                }
            }

            player.getCooldowns().addCooldown(this, 15);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static String getDirectionLabel(double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        if (angle < 0) angle += 360;

        if (angle >= 337.5 || angle < 22.5) return "South ⬇";
        if (angle >= 22.5 && angle < 67.5) return "South-West ↙";
        if (angle >= 67.5 && angle < 112.5) return "West ⬅";
        if (angle >= 112.5 && angle < 157.5) return "North-West ↖";
        if (angle >= 157.5 && angle < 202.5) return "North ⬆";
        if (angle >= 202.5 && angle < 247.5) return "North-East ↗";
        if (angle >= 247.5 && angle < 292.5) return "East ➡";
        return "South-East ↘";
    }
    public static void clearRuntime() {
        TRACKED_MOBS.clear();
        TRACKED_BLOCKS.clear();
    }
}
