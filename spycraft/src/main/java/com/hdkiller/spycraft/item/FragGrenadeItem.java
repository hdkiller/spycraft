package com.hdkiller.spycraft.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Tactical Frag Grenade (Taktikai Repeszgránát)
 * - Throwable high-explosive anti-personnel fragmentation grenade.
 * - Explodes upon impact with blocks or enemies with a lethal 3.8f blast radius.
 * - Releases a high-velocity shrapnel shockwave dealing heavy damage and knockback.
 * - Safe for friendly structures (ExplosionInteraction.MOB) while decimating hostiles.
 */
public class FragGrenadeItem extends Item {
    public FragGrenadeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        double throwDist = 28.0;
        Vec3 reachVec = eyePos.add(lookVec.scale(throwDist));

        // 1. Raycast for block impact
        BlockHitResult blockHit = level.clip(new ClipContext(
                eyePos, reachVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        Vec3 impactPos = blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : reachVec;

        // 2. Check for entity impact along trajectory
        AABB traceBox = new AABB(eyePos, impactPos).inflate(1.2);
        List<Entity> nearbyEntities = level.getEntities(player, traceBox,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator());

        LivingEntity directHitEntity = null;
        double closestDistSq = Double.MAX_VALUE;

        for (Entity e : nearbyEntities) {
            AABB entityBox = e.getBoundingBox().inflate(0.3);
            var hitOpt = entityBox.clip(eyePos, impactPos);
            if (hitOpt.isPresent()) {
                double distSq = eyePos.distanceToSqr(hitOpt.get());
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    directHitEntity = (LivingEntity) e;
                    impactPos = hitOpt.get();
                }
            }
        }

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            // Pin pull & throw sound
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.PLAYERS, 0.8f, 1.6f);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.9f, 0.7f);

            // Ballistic spark & smoke trajectory arc
            int steps = Math.max((int) (eyePos.distanceTo(impactPos) * 2.5), 1);
            for (int i = 0; i <= steps; i++) {
                double progress = (double) i / steps;
                Vec3 p = eyePos.lerp(impactPos, progress);
                double arc = Math.sin(progress * Math.PI) * 1.6;
                serverLevel.sendParticles(ParticleTypes.SMALL_FLAME, p.x, p.y + arc, p.z, 1, 0, 0, 0, 0.01);
                if (i % 2 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SMOKE, p.x, p.y + arc, p.z, 1, 0, 0, 0, 0.02);
                }
            }

            // Direct hit kinetic punch
            if (directHitEntity != null) {
                directHitEntity.hurt(level.damageSources().thrown(player, player), 4.0f);
            }

            // Detonation explosion
            serverLevel.explode(player, impactPos.x, impactPos.y, impactPos.z, 3.8f, Level.ExplosionInteraction.MOB);

            // Shrapnel fragment burst shockwave
            for (int i = 0; i < 28; i++) {
                double vx = (serverLevel.random.nextDouble() - 0.5) * 1.5;
                double vy = serverLevel.random.nextDouble() * 0.9 + 0.1;
                double vz = (serverLevel.random.nextDouble() - 0.5) * 1.5;
                serverLevel.sendParticles(ParticleTypes.CRIT, impactPos.x, impactPos.y + 0.3, impactPos.z,
                        0, vx, vy, vz, 0.8);
            }
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, impactPos.x, impactPos.y + 0.2, impactPos.z,
                    12, 0.4, 0.3, 0.4, 0.05);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.getCooldowns().addCooldown(this, 25);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spycraft.frag_grenade.desc"));
        tooltip.add(Component.translatable("tooltip.spycraft.frag_grenade.effect"));
        tooltip.add(Component.translatable("tooltip.spycraft.frag_grenade.range"));
    }
}
