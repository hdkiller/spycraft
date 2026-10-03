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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Tactical Tranquilizer Dart Gun (Altató Nyílpuska)
 * - Ultra-quiet suppressed dart pistol for silent infiltration.
 * - Puts guards and monsters to sleep (Slowness VII + Weakness V + Agro-reset) for 15 seconds.
 */
public class TranquilizerGunItem extends Item {
    public TranquilizerGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            Vec3 eyePos = player.getEyePosition();
            Vec3 lookVec = player.getViewVector(1.0f);
            double maxRange = 36.0;
            Vec3 traceEnd = eyePos.add(lookVec.scale(maxRange));

            // Block trace
            BlockHitResult blockHit = level.clip(new ClipContext(
                    eyePos, traceEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            double hitDist = maxRange;
            if (blockHit.getType() != HitResult.Type.MISS) {
                hitDist = eyePos.distanceTo(blockHit.getLocation());
            }

            // Entity trace
            AABB traceBox = player.getBoundingBox().expandTowards(lookVec.scale(hitDist)).inflate(1.0);
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                    level, player, eyePos, eyePos.add(lookVec.scale(hitDist)), traceBox,
                    entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator());

            Vec3 actualEnd = eyePos.add(lookVec.scale(hitDist));

            if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
                actualEnd = target.getEyePosition();

                // Apply deep sleep / tranquilizer effects (15s = 300 ticks)
                target.hurt(level.damageSources().playerAttack(player), 1.0f);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 6, false, true, true));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 5, false, true, true));

                if (target instanceof Mob mob) {
                    mob.setTarget(null); // Agro reset!
                }

                // Sleep visual feedback
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        target.getX(), target.getY() + target.getEyeHeight() + 0.3, target.getZ(),
                        12, 0.3, 0.3, 0.3, 0.05);

                serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.8f, 0.6f);

                player.displayClientMessage(Component.literal("§a💤 [CÉLPONT ELALTATVA!] §7" + target.getName().getString() + " (15 mp)"), true);
            }

            // Particle tracer trail (green sleep particles)
            int steps = (int) (eyePos.distanceTo(actualEnd) * 2.5);
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                Vec3 p = eyePos.lerp(actualEnd, t);
                serverLevel.sendParticles(ParticleTypes.COMPOSTER,
                        p.x, p.y - 0.15, p.z,
                        1, 0, 0, 0, 0);
            }

            // Ultra-quiet pneumatic sound
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5f, 1.8f);

            player.getCooldowns().addCooldown(this, 18);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Csendes, sűrített levegős §aAltató Nyílpuska§7."));
        tooltip.add(Component.literal("§8↳ Eltalálva a célpont elalszik (15 mp mozgásképtelenség + agro-törlés)."));
    }
}
