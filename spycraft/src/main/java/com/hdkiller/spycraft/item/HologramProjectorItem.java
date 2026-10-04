package com.hdkiller.spycraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * Holographic Decoy Projector (Hologram Projektor)
 * - Deploys a full holographic duplicate of the player.
 * - Lures and taunts all nearby hostile mobs and guards to attack it!
 * - Lasts for 25 seconds before safely vanishing in a burst of sparks.
 */
public class HologramProjectorItem extends Item {
    public HologramProjectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        var level = context.getLevel();
        BlockPos pos = context.getClickedPos().above();

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            ArmorStand decoy = new ArmorStand(serverLevel, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            decoy.setCustomName(Component.literal("§b✨ " + player.getName().getString() + " (Hologram)"));
            decoy.setCustomNameVisible(true);
            decoy.setGlowingTag(true);
            decoy.setNoGravity(true);
            decoy.setInvulnerable(false);

            // Copy player equipment
            decoy.setItemSlot(EquipmentSlot.HEAD, player.getItemBySlot(EquipmentSlot.HEAD).copy());
            decoy.setItemSlot(EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST).copy());
            decoy.setItemSlot(EquipmentSlot.MAINHAND, player.getItemBySlot(EquipmentSlot.MAINHAND).copy());

            serverLevel.addFreshEntity(decoy);

            // Register in manager for mob aggro lure & auto-cleanup
            HologramDecoyManager.registerDecoy(decoy, 500); // 25 seconds

            // Sound and cyber particles
            serverLevel.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.8f);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    25, 0.4, 0.6, 0.4, 0.08);

            player.sendSystemMessage(Component.literal("§b✨ [HOLOGRAM KIVETÍTVE!] §7A közeli szörnyek és őrök a másolatot támadják!"));
            player.getCooldowns().addCooldown(this, 200); // 10s cooldown
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spycraft.hologram_projector.line1"));
        tooltip.add(Component.translatable("tooltip.spycraft.hologram_projector.line2"));
    }
}
