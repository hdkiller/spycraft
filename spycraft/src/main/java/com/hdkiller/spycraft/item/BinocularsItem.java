package com.hdkiller.spycraft.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tactical Recon Binoculars (Taktikai Felderítő Távcső)
 * - Hold Right-Click: High-magnification optical zoom.
 * - Dwell Tagging: Hovering crosshairs over an enemy for 1.2 seconds locks on,
 *   tags them with Glowing (visible through walls), and syncs them directly
 *   to the player's Spy Radar and GPS navigation goggles!
 */
public class BinocularsItem extends Item {
    private static final double MAX_SCAN_RANGE = 96.0;
    private static final int DWELL_LOCK_TICKS = 24; // 1.2 seconds dwell time

    private static final Map<UUID, ScanSession> ACTIVE_SCANS = new ConcurrentHashMap<>();

    private static class ScanSession {
        UUID targetId;
        int dwellTicks;
        boolean locked;

        ScanSession(UUID targetId) {
            this.targetId = targetId;
            this.dwellTicks = 0;
            this.locked = false;
        }
    }

    public BinocularsItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPYGLASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);

        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player)) return;

        if (level.isClientSide) {
            return;
        }

        UUID playerUuid = player.getUUID();
        LivingEntity target = getTargetInCrosshairs(player, level, MAX_SCAN_RANGE);

        if (target == null) {
            ACTIVE_SCANS.remove(playerUuid);
            sendScan(player, -1, 0, false);
            return;
        }

        ScanSession session = ACTIVE_SCANS.compute(playerUuid, (k, v) -> {
            if (v == null || !v.targetId.equals(target.getUUID())) {
                return new ScanSession(target.getUUID());
            }
            v.dwellTicks++;
            return v;
        });

        int distance = (int) Math.round(player.position().distanceTo(target.position()));
        String targetName = target.getName().getString();

        if (!session.locked) {
            float progress = Math.min((float) session.dwellTicks / DWELL_LOCK_TICKS, 1.0f);
            int barCount = (int) (progress * 10);
            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                bar.append(i < barCount ? "█" : "░");
            }

            // Audio pitch feedback every 4 ticks
            if (session.dwellTicks % 4 == 0) {
                float pitch = 1.0f + progress * 0.8f;
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.4f, pitch);
            }

            // Action bar scanning HUD
            player.displayClientMessage(
                Component.translatable("hud.spycraft.binoculars.scanning", targetName, bar.toString(), distance),
                true
            );

            // Lock-on acquired!
            if (session.dwellTicks >= DWELL_LOCK_TICKS) {
                session.locked = true;

                // 1. Grant Glowing effect for 5 minutes (visible through solid walls)
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 60 * 5, 0, false, false, false));

                // 2. Synchronize target into player's Spy Bug & Radar network
                MobTrackerItem.setTrackedMob(playerUuid, target.getUUID());

                // 3. Audio & Particle feedback
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0f, 1.3f);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 2.0f);

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        target.getX(), target.getEyeY(), target.getZ(), 25, 0.3, 0.3, 0.3, 0.08);
                    serverLevel.sendParticles(ParticleTypes.GLOW,
                        target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.4, 0.5, 0.4, 0.05);
                }

                // Chat announcement
                player.sendSystemMessage(Component.translatable("message.spycraft.binoculars.tag_success", targetName));
            }
        } else {
            // Already locked: show steady lock indicator
            player.displayClientMessage(
                Component.translatable("hud.spycraft.binoculars.locked", targetName, distance),
                true
            );
        }
        sendScan(player, target.getId(), session.dwellTicks, session.locked);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide && entity instanceof Player player) {
            ACTIVE_SCANS.remove(player.getUUID());
            sendScan(player, -1, 0, false);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SPYGLASS_STOP_USING, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    private static LivingEntity getTargetInCrosshairs(Player player, Level level, double maxDistance) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 reach = eyePos.add(look.scale(maxDistance));

        // 1. Block raycast
        BlockHitResult blockHit = level.clip(new ClipContext(
            eyePos,
            reach,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        ));

        double maxReachDist = (blockHit.getType() != HitResult.Type.MISS)
            ? eyePos.distanceTo(blockHit.getLocation())
            : maxDistance;

        // 2. Entity search
        AABB searchBox = new AABB(eyePos, reach).inflate(2.0);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchBox,
            e -> e != player && e.isAlive() && !e.isSpectator());

        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity entity : entities) {
            AABB box = entity.getBoundingBox().inflate(0.45);
            Optional<Vec3> hit = box.clip(eyePos, reach);
            if (hit.isPresent()) {
                double dist = eyePos.distanceTo(hit.get());
                if (dist <= maxReachDist && dist < closestDist) {
                    closestDist = dist;
                    closest = entity;
                }
            }
        }

        return closest;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spycraft.binoculars.desc"));
        tooltip.add(Component.translatable("tooltip.spycraft.binoculars.zoom"));
        tooltip.add(Component.translatable("tooltip.spycraft.binoculars.tag"));
    }
    public static void clearRuntime() {
        ACTIVE_SCANS.clear();
    }
    private static void sendScan(Player player, int targetId, int ticks, boolean locked) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(serverPlayer,
                        com.hdkiller.spycraft.network.BinocularScanPayload.TYPE)) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(serverPlayer,
                    new com.hdkiller.spycraft.network.BinocularScanPayload(targetId, ticks, locked));
        }
    }

    public static void onPlayerDisconnect(UUID uuid) {
        ACTIVE_SCANS.remove(uuid);
    }
}
