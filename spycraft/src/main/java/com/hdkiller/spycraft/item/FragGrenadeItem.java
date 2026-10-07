package com.hdkiller.spycraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Tactical Frag Grenade (Taktikai Repeszgránát)
 * - Throwable high-explosive anti-personnel fragmentation grenade.
 * - Flies along a realistic visible ballistic arc in the air.
 * - Bounces upon hitting the ground/walls with metallic clinking sounds.
 * - Features a 1.25s sizzling ground fuse delay before unleashing a 3.8f shrapnel blast.
 */
public class FragGrenadeItem extends Item {
    public FragGrenadeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            Vec3 eyePos = player.getEyePosition();
            Vec3 lookVec = player.getLookAngle();
            // Moderate velocity (0.95) with a slight upward arc for realistic grenade flight
            Vec3 initialVel = lookVec.scale(0.95).add(0, 0.12, 0);

            FragGrenadeManager.throwGrenade(serverLevel, player, eyePos, initialVel);

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
        tooltip.add(Component.translatable("tooltip.spycraft.frag_grenade.fuse"));
    }
}
