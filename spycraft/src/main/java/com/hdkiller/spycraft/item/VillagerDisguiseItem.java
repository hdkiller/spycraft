package com.hdkiller.spycraft.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Villager Spy Disguise Suit (Falusi Álcaruha)
 * - Equipping this replaces the player's 3D model with an authentic animated Villager!
 * - Right-clicking produces the classic Villager "HRMMM!" sound and emerald sparkles.
 */
public class VillagerDisguiseItem extends ArmorItem {
    public VillagerDisguiseItem(Type type, Properties properties) {
        super(ArmorMaterials.LEATHER, type, properties);
    }

    public static boolean isDisguised(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof VillagerDisguiseItem
            || player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof VillagerDisguiseItem;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // If player is already wearing disguise, right-clicking plays villager "HRMMM!"
        if (isDisguised(player)) {
            if (!level.isClientSide) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_AMBIENT, SoundSource.PLAYERS, 1.2f, 1.0f);

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        player.getX(), player.getY() + 1.8, player.getZ(),
                        5, 0.2, 0.2, 0.2, 0.05);
                }

                player.sendSystemMessage(Component.literal("§a🥸 Falusi Erik: §f\"Hrrrrmmm!\""));
                player.getCooldowns().addCooldown(this, 20);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        // Standard armor equip behavior if not equipped yet
        return super.use(level, player, hand);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof Player player) {
            boolean isWearing = player.getItemBySlot(EquipmentSlot.CHEST).getItem() == this
                             || player.getItemBySlot(EquipmentSlot.HEAD).getItem() == this;

            if (isWearing && level.getGameTime() % 100 == 0) {
                // Occasional subtle green sparkle to show disguise is active
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        1, 0.2, 0.3, 0.2, 0.01);
                }
            }
        }
    }
}
