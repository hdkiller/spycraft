package com.hdkiller.spycraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
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
 * Tactical Zipline Gun (Taktikai Drótkötélpálya Kilövő)
 * - Stage 1: Aim and shoot a taut steel wire rope cable up to 64 blocks away.
 * - Stage 2: Walk up to the cable and Right-Click to mount your trolley pulley and slide across at 27 m/s!
 * - Multi-direction: Mounts towards whichever end you are looking along.
 * - Controls:
 *   - Right-Click: Shoots & anchors cable (or mounts when looking along cable)
 *   - Sneak + Right-Click: Retracts deployed cable (or press Sneak mid-flight to drop off)
 */
public class ZiplineGunItem extends Item {
    public static final double MAX_DISTANCE = 64.0;
    public static final double MIN_DISTANCE = 4.0;

    public ZiplineGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (ZiplineManager.isPlayerRiding(player.getUUID())) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            ZiplineManager.ZiplineTarget target = ZiplineManager.findBestZipline(serverPlayer, 4.0);

            // Handle Sneak + Right-Click
            if (serverPlayer.isShiftKeyDown()) {
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

                // If not aiming at distant block, retract nearest cable
                if (target != null && (hit.getType() != HitResult.Type.BLOCK || hit.getLocation().distanceTo(player.position()) < MIN_DISTANCE)) {
                    ZiplineManager.retractZipline(serverLevel, serverPlayer, target.zipline);
                    serverPlayer.getCooldowns().addCooldown(this, 10);
                    return InteractionResultHolder.sidedSuccess(stack, false);
                }
                // If aiming at distant block while sneaking, fall through to force deploy new zipline
            } else if (target != null && target.lookingAlongCable) {
                // Normal Right-Click looking along deployed cable -> Mount & Ride!
                boolean mounted = ZiplineManager.mountZipline(serverLevel, serverPlayer, target.zipline);
                if (mounted) {
                    serverPlayer.getCooldowns().addCooldown(this, 15);
                    return InteractionResultHolder.sidedSuccess(stack, false);
                } else {
                    return InteractionResultHolder.pass(stack);
                }
            }

            // Deploy new zipline cable
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
                Vec3 startPos = player.position().add(0, 1.2, 0);
                double distance = startPos.distanceTo(hitPos);

                if (distance < MIN_DISTANCE) {
                    serverPlayer.displayClientMessage(Component.translatable("message.spycraft.zipline.too_close"), true);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.8f);
                    return InteractionResultHolder.fail(stack);
                }

                // Deploy taut wire rope cable!
                ZiplineManager.deployZipline(serverLevel, serverPlayer, startPos, hitPos);
                serverPlayer.getCooldowns().addCooldown(this, 15);
                return InteractionResultHolder.sidedSuccess(stack, false);
            } else {
                serverPlayer.displayClientMessage(Component.translatable("message.spycraft.zipline.too_far"), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.8f);
                return InteractionResultHolder.pass(stack);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (selected && !level.isClientSide && entity instanceof ServerPlayer serverPlayer) {
            if (!ZiplineManager.isPlayerRiding(serverPlayer.getUUID()) && serverPlayer.tickCount % 10 == 0) {
                ZiplineManager.ZiplineTarget target = ZiplineManager.findBestZipline(serverPlayer, 4.0);
                if (target != null && target.lookingAlongCable) {
                    serverPlayer.displayClientMessage(
                        Component.translatable("hud.spycraft.zipline.prompt_mount"),
                        true
                    );
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spycraft.zipline_gun.desc"));
        tooltip.add(Component.translatable("tooltip.spycraft.zipline_gun.range"));
        tooltip.add(Component.translatable("tooltip.spycraft.zipline_gun.controls"));
        tooltip.add(Component.translatable("tooltip.spycraft.zipline_gun.retract"));
    }
}
