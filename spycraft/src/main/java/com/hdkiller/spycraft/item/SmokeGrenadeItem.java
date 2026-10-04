package com.hdkiller.spycraft.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Tactical Smoke Grenade (Taktikai Füstgránát)
 * - Throws a smoke grenade that bursts into a dense 11m smoke screen for 12 seconds.
 * - Blinds and resets aggro for all hostile guards and monsters inside.
 * - Grants tactical Invisibility to players within the smoke cloud.
 */
public class SmokeGrenadeItem extends Item {
    public SmokeGrenadeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        double throwDist = 26.0;
        Vec3 reachVec = eyePos.add(lookVec.scale(throwDist));

        BlockHitResult hit = level.clip(new ClipContext(
                eyePos, reachVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        Vec3 impactPos = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : reachVec;

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            // Throw sound
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.9f, 0.8f);

            // Ballistic arc particles
            int steps = Math.max((int) (eyePos.distanceTo(impactPos) * 2.0), 1);
            for (int i = 0; i <= steps; i++) {
                double progress = (double) i / steps;
                Vec3 p = eyePos.lerp(impactPos, progress);
                double arc = Math.sin(progress * Math.PI) * 1.5;
                serverLevel.sendParticles(ParticleTypes.SMOKE, p.x, p.y + arc, p.z, 1, 0, 0, 0, 0);
            }

            // Spawn smoke cloud
            SmokeCloudManager.spawnCloud(serverLevel, impactPos, 5.5, 240); // 12 seconds

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.getCooldowns().addCooldown(this, 25);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spycraft.smoke_grenade.desc"));
        tooltip.add(Component.translatable("tooltip.spycraft.smoke_grenade.effect"));
    }
}
