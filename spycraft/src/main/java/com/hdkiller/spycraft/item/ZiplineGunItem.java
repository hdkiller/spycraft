package com.hdkiller.spycraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * Tactical Zipline Gun (Taktikai Drótkötélpálya Kilövő)
 * - Aim at any wall, ledge, or distant cliff up to 64 blocks away.
 * - Deploys a taut steel wire rope between your position and the target.
 * - Automatically hooks your trolley pulley to slide across the chasm at 27 m/s!
 * - Safe dismount upon arrival or press Sneak to drop off mid-flight.
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
                if (!level.isClientSide) {
                    player.sendSystemMessage(Component.translatable("message.spycraft.zipline.too_close"));
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.8f);
                }
                return InteractionResultHolder.fail(stack);
            }

            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
                // Deploy zipline cable and start high speed slide!
                ZiplineManager.createZiplineAndRide(serverLevel, serverPlayer, startPos, hitPos);
            }

            player.getCooldowns().addCooldown(this, 25);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        } else {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatable("message.spycraft.zipline.too_far"));
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.8f);
            }
            return InteractionResultHolder.pass(stack);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spycraft.zipline_gun.desc"));
        tooltip.add(Component.translatable("tooltip.spycraft.zipline_gun.range"));
        tooltip.add(Component.translatable("tooltip.spycraft.zipline_gun.controls"));
    }
}
