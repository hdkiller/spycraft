package com.hdkiller.spycraft.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Magnetic Wall-Climbing Gloves (Mágneses Mászókesztyű)
 * - Hold in either hand to scale any vertical wall or skyscraper!
 * - Press Jump or Forward against a wall to climb upwards.
 * - Sneak (Shift) to cling to the wall and hang suspended in mid-air.
 * - Eliminates all fall damage while holding.
 */
public class ClimbingGlovesItem extends Item {
    public ClimbingGlovesItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof Player player) {
            boolean isHeld = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
            if (!isHeld) return;

            // Zero out fall damage when gloves are equipped in hand
            player.fallDistance = 0.0f;

            // Wall climbing physics
            if (player.horizontalCollision) {
                Vec3 velocity = player.getDeltaMovement();

                if (player.isCrouching()) {
                    // Stick/hang suspended on wall
                    player.setDeltaMovement(0, 0.0, 0);
                    player.hurtMarked = true;
                } else if (player.zza > 0 || player.getXRot() < -10.0f || !player.onGround()) {
                    // Climb smoothly upward
                    player.setDeltaMovement(velocity.x * 0.5, 0.24, velocity.z * 0.5);
                    player.hurtMarked = true;

                    // Audio and particle feedback
                    if (!level.isClientSide && level.getGameTime() % 8 == 0 && level instanceof ServerLevel serverLevel) {
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.COPPER_STEP, SoundSource.PLAYERS, 0.8f, 1.8f);
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                player.getX(), player.getY() + 0.8, player.getZ(),
                                2, 0.2, 0.2, 0.2, 0.02);
                    }
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spycraft.climbing_gloves.desc"));
        tooltip.add(Component.translatable("tooltip.spycraft.climbing_gloves.effect"));
    }
}
