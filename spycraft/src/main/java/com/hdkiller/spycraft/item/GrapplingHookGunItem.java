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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Grappling Hook Gun - A tactical spy infiltration tool!
 * Aim at any wall or ledge up to 32 blocks away and right-click to reel in.
 */
public class GrapplingHookGunItem extends Item {
    private static final double MAX_DISTANCE = 32.0;

    public GrapplingHookGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Raycast from player eyes in the look direction
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 reachVec = eyePos.add(lookVec.scale(MAX_DISTANCE));

        BlockHitResult hit = level.clip(new ClipContext(
            eyePos,
            reachVec,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 hitPos = hit.getLocation();
            Vec3 pullVec = hitPos.subtract(player.position());
            double distance = pullVec.length();

            if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                // Draw cable particle trail from gun to target
                int steps = Math.min((int) distance * 2, 60);
                for (int i = 0; i <= steps; i++) {
                    double progress = (double) i / steps;
                    double px = eyePos.x + (hitPos.x - eyePos.x) * progress;
                    double py = eyePos.y + (hitPos.y - eyePos.y) * progress;
                    double pz = eyePos.z + (hitPos.z - eyePos.z) * progress;
                    serverLevel.sendParticles(ParticleTypes.CRIT, px, py, pz, 1, 0, 0, 0, 0);
                }

                // Play grappling sounds
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.5f);
                serverLevel.playSound(null, hitPos.x, hitPos.y, hitPos.z,
                    SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 1.0f, 1.2f);
            }

            // Pull velocity calculation
            Vec3 direction = pullVec.normalize();
            double horizontalForce = Math.min(distance * 0.15 + 0.8, 2.2);
            double verticalForce = Math.min(Math.max(pullVec.y * 0.18 + 0.45, 0.4), 1.5);

            player.setDeltaMovement(
                direction.x * horizontalForce,
                verticalForce,
                direction.z * horizontalForce
            );
            player.hurtMarked = true;

            // Spy safety: grant 3 seconds of slow falling to avoid splatting on landing!
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
            player.resetFallDistance();

            player.getCooldowns().addCooldown(this, 15); // 0.75s tactical cooldown
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        } else {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.literal("§c[Grappling Hook] §7Target too far! (Max 32 blocks)"));
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.8f);
            }
            return InteractionResultHolder.pass(stack);
        }
    }
}
