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

/**
 * Remote Detonator Device with 3 Yield Levels:
 * - Level 1: Standard blast (power: 4.5f)
 * - Level 2: Double blast (power: 9.0f)
 * - Level 3: Mega blast (power: 18.0f)
 * Right-click a placed C4 canister repeatedly to cycle through the 3 power levels!
 */
public class RemoteDetonatorItem extends Item {
    // Player UUID -> Map of BlockPos -> Yield Level (1, 2, or 3)
    private static final Map<UUID, Map<BlockPos, Integer>> ARMED_CHARGES = new ConcurrentHashMap<>();

    public RemoteDetonatorItem(Properties properties) {
        super(properties);
    }

    public static Map<BlockPos, Integer> getArmedChargesMap(UUID playerUuid) {
        return ARMED_CHARGES.getOrDefault(playerUuid, Map.of());
    }

    /**
     * Arm or cycle yield level of a C4 block
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (level.getBlockState(pos).is(ModBlocks.C4_BLOCK)) {
            if (!level.isClientSide) {
                Map<BlockPos, Integer> playerCharges = ARMED_CHARGES.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
                int currentLevel = playerCharges.getOrDefault(pos, 0);

                // Cycle: 0 -> 1 -> 2 -> 3 -> 1
                int newLevel = currentLevel >= 3 ? 1 : currentLevel + 1;
                playerCharges.put(pos, newLevel);

                if (level instanceof ServerLevel serverLevel) {
                    float pitch;
                    if (newLevel == 1) {
                        pitch = 1.0f;
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                            15, 0.2, 0.2, 0.2, 0.05);
                        player.sendSystemMessage(Component.literal(
                            "§a📡 [C4 ÉLESÍTVE - 1. FOKOZAT] §fAlap robbanóerő (1x) §8| §7Kattints újra a növeléshez!"
                        ));
                    } else if (newLevel == 2) {
                        pitch = 1.5f;
                        serverLevel.sendParticles(ParticleTypes.FLAME,
                            pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                            25, 0.2, 0.2, 0.2, 0.05);
                        player.sendSystemMessage(Component.literal(
                            "§e⚡ [C4 ÉLESÍTVE - 2. FOKOZAT] §6DUPLA robbanóerő (2x)! §8| §7Kattints újra a MAX-hoz!"
                        ));
                    } else {
                        pitch = 2.0f;
                        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                            35, 0.2, 0.2, 0.2, 0.08);
                        serverLevel.sendParticles(ParticleTypes.SMOKE,
                            pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                            15, 0.1, 0.2, 0.1, 0.02);
                        player.sendSystemMessage(Component.literal(
                            "§c💥 [C4 ÉLESÍTVE - 3. FOKOZAT] §4🔥 MEGA ROBBANÁS (Óriási kráter)! 🔥"
                        ));
                    }

                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, pitch);
                }

                player.getCooldowns().addCooldown(this, 10);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    /**
     * Trigger remote explosion by right-clicking in the air!
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            Map<BlockPos, Integer> charges = ARMED_CHARGES.get(player.getUUID());

            if (charges == null || charges.isEmpty()) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.5f);
                player.sendSystemMessage(Component.literal(
                    "§c📡 [DETONÁTOR] §7Nincs élesített C4 töltet! Kattints egy lehelyezett C4 csőre az élesítéshez."
                ));
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            // Click trigger sound
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 1.2f, 1.0f);

            int detonatedCount = 0;
            int maxLevel = 1;
            List<BlockPos> toRemove = new ArrayList<>();

            for (Map.Entry<BlockPos, Integer> entry : charges.entrySet()) {
                BlockPos pos = entry.getKey();
                int yieldLevel = entry.getValue();
                if (yieldLevel > maxLevel) maxLevel = yieldLevel;

                if (level.getBlockState(pos).is(ModBlocks.C4_BLOCK)) {
                    float power = yieldLevel == 1 ? 4.5f : (yieldLevel == 2 ? 9.0f : 18.0f);
                    C4Block.explodeWithPower(level, pos, power);
                    detonatedCount++;
                    toRemove.add(pos);
                }
            }

            for (BlockPos pos : toRemove) {
                charges.remove(pos);
            }

            if (detonatedCount > 0) {
                if (maxLevel == 3) {
                    player.sendSystemMessage(Component.literal(
                        "§c💥 [ROBBANTÁS SIKERES!] §4🔥 MEGA ROBBANÁS! (" + detonatedCount + " C4 töltet)"
                    ));
                } else if (maxLevel == 2) {
                    player.sendSystemMessage(Component.literal(
                        "§e💥 [ROBBANTÁS SIKERES!] §6DUPLA erejű robbantás! (" + detonatedCount + " C4 töltet)"
                    ));
                } else {
                    player.sendSystemMessage(Component.literal(
                        "§a💥 [ROBBANTÁS SIKERES!] §f" + detonatedCount + " C4 töltet felrobbantva!"
                    ));
                }
            } else {
                player.sendSystemMessage(Component.literal(
                    "§e📡 [DETONÁTOR] §7Az élesített C4 töltetek már megsemmisültek vagy hiányoznak."
                ));
            }

            player.getCooldowns().addCooldown(this, 30);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
