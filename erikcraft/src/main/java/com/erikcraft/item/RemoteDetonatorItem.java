package com.erikcraft.item;

import com.erikcraft.block.C4Block;
import com.erikcraft.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Remote Detonator Device
 * 1. Right-click any placed C4 block to arm and link it to this detonator.
 * 2. Walk away to safety.
 * 3. Right-click in the air to trigger the remote explosion!
 */
public class RemoteDetonatorItem extends Item {
    // Player UUID -> List of armed C4 BlockPos
    private static final Map<UUID, List<BlockPos>> ARMED_CHARGES = new ConcurrentHashMap<>();

    public RemoteDetonatorItem(Properties properties) {
        super(properties);
    }

    public static List<BlockPos> getArmedCharges(UUID playerUuid) {
        return ARMED_CHARGES.getOrDefault(playerUuid, List.of());
    }

    /**
     * Arm / Sync a C4 Block by right-clicking it with the detonator
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (level.getBlockState(pos).is(ModBlocks.C4_BLOCK)) {
            if (!level.isClientSide) {
                List<BlockPos> list = ARMED_CHARGES.computeIfAbsent(player.getUUID(), k -> new CopyOnWriteArrayList<>());
                if (!list.contains(pos)) {
                    list.add(pos);
                }

                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                        15, 0.2, 0.2, 0.2, 0.05);

                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 2.0f);
                }

                player.sendSystemMessage(Component.literal(
                    "§a📡 [C4 ARMED] §fCharge #" + list.size() +
                    " linked at [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]! Ready to detonate."
                ));

                player.getCooldowns().addCooldown(this, 10);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    /**
     * Trigger detonation by right-clicking in the air!
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            List<BlockPos> charges = ARMED_CHARGES.get(player.getUUID());

            if (charges == null || charges.isEmpty()) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.5f);
                player.sendSystemMessage(Component.literal(
                    "§c📡 [DETONATOR] §7No C4 charges armed! Right-click a placed C4 block first to link it."
                ));
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            // Click sound on detonator
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 1.2f, 1.0f);

            int detonatedCount = 0;
            List<BlockPos> toRemove = new ArrayList<>();

            for (BlockPos pos : charges) {
                if (level.getBlockState(pos).is(ModBlocks.C4_BLOCK)) {
                    C4Block.explode(level, pos);
                    detonatedCount++;
                    toRemove.add(pos);
                }
            }

            charges.removeAll(toRemove);

            if (detonatedCount > 0) {
                player.sendSystemMessage(Component.literal(
                    "§c💥 [DETONATION TRIGGERED!] §eSuccessfully detonated " + detonatedCount + " C4 charge(s)!"
                ));
            } else {
                player.sendSystemMessage(Component.literal(
                    "§e📡 [DETONATOR] §7Armed C4 charges were already destroyed or missing."
                ));
            }

            player.getCooldowns().addCooldown(this, 30); // 1.5s tactical cooldown
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
