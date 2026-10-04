package com.hdkiller.spycraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Tactical Breaching Spray (Taktikai Falbontó C4 Spray)
 * - Spray wall blocks with liquid C4 breaching foam.
 * - Trigger a shaped directional wall breach via the Remote Detonator!
 * - Shift + Right-Click in the air to disarm and clear all sprayed blocks.
 */
public class BreachingSprayItem extends Item {

    public BreachingSprayItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        boolean added = BreachingSprayManager.addSprayedBlock(level, pos, player);
        if (added) {
            if (!level.isClientSide) {
                int count = BreachingSprayManager.getSprayedBlockCount(player.getUUID());
                player.displayClientMessage(
                    Component.translatable("hud.spycraft.breaching_spray.sprayed", count),
                    true
                );
                context.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
            }
            player.getCooldowns().addCooldown(this, 4); // Quick spray sweep
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Sneak + Right Click in air: Disarm and clear all sprayed blocks
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                int count = BreachingSprayManager.getSprayedBlockCount(player.getUUID());
                if (count > 0) {
                    BreachingSprayManager.clearSprayedBlocks(player.getUUID());
                    player.sendSystemMessage(Component.translatable("message.spycraft.breaching_spray.cleared", count));
                } else {
                    player.sendSystemMessage(Component.translatable("message.spycraft.breaching_spray.none"));
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // Mid-range raycast spray (up to 5 blocks)
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 reach = eyePos.add(look.scale(5.0));

        BlockHitResult hit = level.clip(new ClipContext(
            eyePos, reach,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            boolean added = BreachingSprayManager.addSprayedBlock(level, pos, player);
            if (added) {
                if (!level.isClientSide) {
                    int count = BreachingSprayManager.getSprayedBlockCount(player.getUUID());
                    player.displayClientMessage(
                        Component.translatable("hud.spycraft.breaching_spray.sprayed", count),
                        true
                    );
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                }
                player.getCooldowns().addCooldown(this, 4);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spycraft.breaching_spray.desc"));
        tooltip.add(Component.translatable("tooltip.spycraft.breaching_spray.usage"));
        tooltip.add(Component.translatable("tooltip.spycraft.breaching_spray.detonate"));
        tooltip.add(Component.translatable("tooltip.spycraft.breaching_spray.clear"));
    }
}
