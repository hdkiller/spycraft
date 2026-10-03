package com.erikcraft.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Tactical Sniper Rifle (Mesterlövész Puska)
 * - Hold Right-Click: Scopes in with high-magnification zoom scope!
 * - Release: Fires a supersonic piercing bullet with vapor trails up to 128 blocks!
 * - Sneak + Right-Click: Fast hip-fire shot!
 */
public class SniperRifleItem extends Item {
    private static final double MAX_RANGE = 128.0;

    public SniperRifleItem(Properties properties) {
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

        // Sneak (Shift) + Right-Click = Quick Hip-Fire
        if (player.isShiftKeyDown()) {
            fireBullet(level, player, stack, 18.0f, false);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        // Normal Right-Click = Scope in with sniper crosshair zoom!
        player.startUsingItem(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SPYGLASS_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            int chargeTicks = getUseDuration(stack, entity) - timeLeft;

            if (chargeTicks >= 10) {
                // Full Scoped Precision Shot (28.0 damage - extreme lethal range)
                fireBullet(level, player, stack, 28.0f, true);
            } else {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SPYGLASS_STOP_USING, SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }
    }

    private void fireBullet(Level level, Player player, ItemStack stack, float damage, boolean scoped) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 reach = eyePos.add(look.scale(MAX_RANGE));

        // 1. Raycast against blocks
        BlockHitResult blockHit = level.clip(new ClipContext(
            eyePos,
            reach,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        ));

        Vec3 targetHitPos = blockHit.getType() != HitResult.Type.MISS ? blockHit.getLocation() : reach;
        LivingEntity hitEntity = null;

        // 2. Raycast against entities along the bullet path
        AABB searchBox = new AABB(eyePos, targetHitPos).inflate(1.2);
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, searchBox,
            e -> e != player && e.isAlive());

        double closestDist = Double.MAX_VALUE;
        for (LivingEntity target : potentialTargets) {
            AABB targetBox = target.getBoundingBox().inflate(0.3);
            Optional<Vec3> clip = targetBox.clip(eyePos, targetHitPos);
            if (clip.isPresent()) {
                double d = eyePos.distanceToSqr(clip.get());
                if (d < closestDist) {
                    closestDist = d;
                    hitEntity = target;
                    targetHitPos = clip.get();
                }
            }
        }

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            // 3. Supersonic vapor trail particles from barrel to hit point
            double totalDist = eyePos.distanceTo(targetHitPos);
            int steps = Math.max((int) (totalDist * 2.0), 1);

            for (int i = 1; i <= steps; i++) {
                double progress = (double) i / steps;
                double px = eyePos.x + (targetHitPos.x - eyePos.x) * progress;
                double py = eyePos.y + (targetHitPos.y - eyePos.y) * progress;
                double pz = eyePos.z + (targetHitPos.z - eyePos.z) * progress;

                serverLevel.sendParticles(ParticleTypes.CRIT, px, py, pz, 1, 0, 0, 0, 0);
                if (i % 4 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE, px, py, pz, 1, 0, 0, 0, 0.01);
                }
            }

            // Gunshot acoustic crack & boom
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9f, 1.8f);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.5f, 0.5f);

            // 4. Handle entity hit
            if (hitEntity != null) {
                hitEntity.hurt(level.damageSources().playerAttack(player), damage);
                Vec3 knockback = look.scale(1.2).add(0, 0.2, 0);
                hitEntity.setDeltaMovement(knockback);
                hitEntity.hurtMarked = true;

                // Impact blood/sparks
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    targetHitPos.x, targetHitPos.y, targetHitPos.z,
                    15, 0.2, 0.2, 0.2, 0.1);

                int distMeters = (int) totalDist;
                player.sendSystemMessage(Component.literal(
                    "§c🎯 [TALÁLAT] §f" + hitEntity.getName().getString() +
                    " §8(§e" + distMeters + "m§8) | Sebzés: §c" + (int) damage
                ));
            } else if (blockHit.getType() != HitResult.Type.MISS) {
                // Ricochet sparks on block
                serverLevel.sendParticles(ParticleTypes.LAVA,
                    targetHitPos.x, targetHitPos.y, targetHitPos.z,
                    5, 0.1, 0.1, 0.1, 0.05);
            }

            // Recoil kickback on player
            player.setDeltaMovement(player.getDeltaMovement().subtract(look.x * 0.22, 0, look.z * 0.22));
            player.hurtMarked = true;
        }

        // Tactical reload cooldown (1.25s) on both client & server
        player.getCooldowns().addCooldown(this, 25);
    }
}
