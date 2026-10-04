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
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tactical Grappling Hook Gun - Advanced Spy Infiltration & Harpoon Tool!
 * - Hold Right-Click: Continuous high-speed winch reeling towards target anchor.
 * - Ledge Mantling: Auto-boosts player over ledges upon arrival.
 * - Slingshot Release: Releasing mid-flight preserves momentum and flings you forward.
 * - Tactical Fall Protection: 0 fall damage on grapple landing (no floaty slow-falling).
 * - Enemy Harpoon: Reel enemies towards you with a brief stun, or grapple onto giant bosses.
 */
public class GrapplingHookGunItem extends Item {
    public static final double MAX_DISTANCE = 32.0;

    private static class GrappleSession {
        Vec3 anchorPos;
        UUID targetEntityId;
        int ticksReeling;

        GrappleSession(Vec3 anchorPos, UUID targetEntityId) {
            this.anchorPos = anchorPos;
            this.targetEntityId = targetEntityId;
            this.ticksReeling = 0;
        }
    }

    private static final Map<UUID, GrappleSession> ACTIVE_GRAPPLES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> GRAPPLE_PROTECTION = new ConcurrentHashMap<>();

    public GrapplingHookGunItem(Properties properties) {
        super(properties);
    }

    public static void grantGrappleProtection(UUID uuid) {
        GRAPPLE_PROTECTION.put(uuid, System.currentTimeMillis() + 6000);
    }

    public static boolean hasRecentGrappleProtection(UUID uuid) {
        Long expiry = GRAPPLE_PROTECTION.get(uuid);
        if (expiry != null && System.currentTimeMillis() < expiry) {
            return true;
        }
        GRAPPLE_PROTECTION.remove(uuid);
        return false;
    }

    public static void clearGrappleProtection(UUID uuid) {
        GRAPPLE_PROTECTION.remove(uuid);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 reachVec = eyePos.add(lookVec.scale(MAX_DISTANCE));

        // 1. Block raycast
        BlockHitResult blockHit = level.clip(new ClipContext(
            eyePos,
            reachVec,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        ));

        double blockDist = (blockHit.getType() != HitResult.Type.MISS)
            ? eyePos.distanceTo(blockHit.getLocation())
            : MAX_DISTANCE;

        // 2. Entity raycast
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            level, player, eyePos, reachVec,
            new AABB(eyePos, reachVec).inflate(1.2),
            e -> e instanceof LivingEntity && e != player && e.isAlive() && !e.isSpectator()
        );

        // Check if entity is hit before or without a blocking block
        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target
            && eyePos.distanceTo(entityHit.getLocation()) <= blockDist) {

            boolean isTitan = target instanceof EnderDragon
                || target instanceof WitherBoss
                || target instanceof Warden
                || target.getBbWidth() > 1.8
                || target.getBbHeight() > 2.5;

            if (isTitan) {
                // Titan Grapple: Reel player towards the massive boss
                Vec3 targetPos = target.getEyePosition();
                ACTIVE_GRAPPLES.put(player.getUUID(), new GrappleSession(targetPos, target.getUUID()));
                grantGrappleProtection(player.getUUID());
                player.startUsingItem(hand);

                if (!level.isClientSide) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.4f);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            } else {
                // Enemy Harpoon: Pull enemy towards player + stun
                Vec3 toPlayer = eyePos.subtract(target.position()).normalize();
                double pullDist = eyePos.distanceTo(target.position());
                double pullSpeed = Math.min(1.5, 0.6 + pullDist * 0.05);

                target.setDeltaMovement(toPlayer.x * pullSpeed, 0.45, toPlayer.z * pullSpeed);
                target.hurtMarked = true;
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 35, 4, false, false));

                if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                    int steps = Math.min((int) (pullDist * 2), 40);
                    Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);
                    for (int i = 0; i <= steps; i++) {
                        double progress = (double) i / steps;
                        Vec3 pt = eyePos.lerp(targetCenter, progress);
                        serverLevel.sendParticles(ParticleTypes.CRIT, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
                    }

                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0f, 1.3f);
                    serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.TRIPWIRE_ATTACH, SoundSource.NEUTRAL, 1.0f, 1.5f);

                    player.displayClientMessage(
                        Component.translatable("hud.spycraft.grappling_hook.harpooned", target.getName().getString()),
                        true
                    );
                }

                player.getCooldowns().addCooldown(this, 18);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }

        // 3. Block Grapple: Continuous Winch Reeling
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            Vec3 hitPos = blockHit.getLocation();
            ACTIVE_GRAPPLES.put(player.getUUID(), new GrappleSession(hitPos, null));
            grantGrappleProtection(player.getUUID());
            player.startUsingItem(hand);

            if (!level.isClientSide) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.4f);
                level.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                    SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 1.0f, 1.2f);
            }

            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        // 4. Out of Range
        if (!level.isClientSide) {
            player.sendSystemMessage(Component.translatable("message.spycraft.grappling_hook.too_far"));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.8f);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!(livingEntity instanceof Player player)) return;

        GrappleSession session = ACTIVE_GRAPPLES.get(player.getUUID());
        if (session == null) {
            player.stopUsingItem();
            return;
        }

        session.ticksReeling++;

        Vec3 targetPos = session.anchorPos;
        if (session.targetEntityId != null) {
            if (level instanceof ServerLevel serverLevel) {
                net.minecraft.world.entity.Entity e = serverLevel.getEntity(session.targetEntityId);
                if (e != null && e.isAlive()) {
                    targetPos = e.getEyePosition();
                    session.anchorPos = targetPos;
                } else {
                    ACTIVE_GRAPPLES.remove(player.getUUID());
                    player.stopUsingItem();
                    return;
                }
            }
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 toTarget = targetPos.subtract(eyePos);
        double dist = toTarget.length();

        // Ledge Mantling & Arrival Check: pop player over ledge cleanly
        if (dist < 2.2 || session.ticksReeling > 60) {
            Vec3 currentVel = player.getDeltaMovement();
            Vec3 look = player.getLookAngle();
            double popY = Math.max(currentVel.y * 0.4, 0.54);

            player.setDeltaMovement(
                currentVel.x * 0.4 + look.x * 0.28,
                popY,
                currentVel.z * 0.4 + look.z * 0.28
            );
            player.hurtMarked = true;
            player.resetFallDistance();
            grantGrappleProtection(player.getUUID());

            if (!level.isClientSide) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.9f, 1.5f);
            }

            ACTIVE_GRAPPLES.remove(player.getUUID());
            player.stopUsingItem();
            player.getCooldowns().addCooldown(this, 10);
            return;
        }

        // Smooth Continuous Reeling Acceleration
        Vec3 dir = toTarget.normalize();
        double speed = 1.15;
        Vec3 desiredVel = dir.scale(speed);
        Vec3 currentVel = player.getDeltaMovement();

        // Blend velocity for responsive, agile pulling
        Vec3 newVel = currentVel.scale(0.3).add(desiredVel.scale(0.7));
        player.setDeltaMovement(newVel);
        player.hurtMarked = true;
        player.resetFallDistance();

        // Cable Particle Trail every tick
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            int steps = Math.min((int) (dist * 2), 50);
            for (int i = 0; i <= steps; i++) {
                double progress = (double) i / steps;
                double px = eyePos.x + (targetPos.x - eyePos.x) * progress;
                double py = eyePos.y + (targetPos.y - eyePos.y) * progress;
                double pz = eyePos.z + (targetPos.z - eyePos.z) * progress;
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 1, 0, 0, 0, 0);
            }

            // Winch reeling sound every 4 ticks
            if (session.ticksReeling % 4 == 0) {
                float pitch = 1.3f + Math.min(session.ticksReeling * 0.03f, 0.5f);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.4f, pitch);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            ACTIVE_GRAPPLES.remove(player.getUUID());

            // Slingshot momentum boost upon early release
            Vec3 vel = player.getDeltaMovement();
            player.setDeltaMovement(vel.x * 1.25, vel.y * 1.1 + 0.12, vel.z * 1.25);
            player.hurtMarked = true;
            player.resetFallDistance();
            grantGrappleProtection(player.getUUID());

            if (!level.isClientSide) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.LEASH_KNOT_BREAK, SoundSource.PLAYERS, 0.8f, 1.8f);
            }

            player.getCooldowns().addCooldown(this, 12);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spycraft.grappling_hook_gun.desc"));
        tooltip.add(Component.translatable("tooltip.spycraft.grappling_hook_gun.reel"));
        tooltip.add(Component.translatable("tooltip.spycraft.grappling_hook_gun.harpoon"));
        tooltip.add(Component.translatable("tooltip.spycraft.grappling_hook_gun.nofall"));
    }
}
