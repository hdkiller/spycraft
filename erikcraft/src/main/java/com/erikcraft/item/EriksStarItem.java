package com.erikcraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Erik's Star - A legendary artifact crafted for Erik!
 * Right-clicking summons lightning where you are looking.
 */
public class EriksStarItem extends Item {
    public EriksStarItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // Spawn lightning bolt 8 blocks in front of the player
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                Vec3 look = player.getLookAngle();
                bolt.moveTo(player.getX() + look.x * 8, player.getY(), player.getZ() + look.z * 8);
                level.addFreshEntity(bolt);
            }

            // Play celebratory sound
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);

            // Announce in chat
            player.sendSystemMessage(Component.literal("§6[ErikCraft] §e⚡ Erik's Star summoned a lightning strike!"));

            // 1.5 second cooldown (30 ticks)
            player.getCooldowns().addCooldown(this, 30);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
