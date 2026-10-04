package com.hdkiller.spycraft.item;

import com.hdkiller.spycraft.block.C4Block;
import com.hdkiller.spycraft.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remote Detonator Device with 3 Yield Levels:
 * - Level 1: Standard blast (power: 4.5f)
 * - Level 2: Double blast (power: 9.0f)
 * - Level 3: Mega blast (power: 18.0f)
 * Right-click a placed C4 canister repeatedly to cycle through the 3 power levels!
 */
public class RemoteDetonatorItem extends Item {
    public record ChargeLocation(ResourceKey<Level> dimension, BlockPos pos) {}

    // Player UUID -> Map of ChargeLocation -> Yield Level (1, 2, or 3)
    private static final Map<UUID, Map<ChargeLocation, Integer>> ARMED_CHARGES = new ConcurrentHashMap<>();

    public RemoteDetonatorItem(Properties properties) {
        super(properties);
    }

    public static Map<BlockPos, Integer> getArmedChargesMap(UUID playerUuid) {
        Map<ChargeLocation, Integer> map = ARMED_CHARGES.get(playerUuid);
        if (map == null || map.isEmpty()) return Map.of();
        Map<BlockPos, Integer> result = new HashMap<>();
        for (Map.Entry<ChargeLocation, Integer> entry : map.entrySet()) {
            result.put(entry.getKey().pos(), entry.getValue());
        }
        return result;
    }

    public static Map<BlockPos, Integer> getArmedChargesMap(UUID playerUuid, ResourceKey<Level> dimension) {
        Map<ChargeLocation, Integer> map = ARMED_CHARGES.get(playerUuid);
        if (map == null || map.isEmpty()) return Map.of();
        Map<BlockPos, Integer> result = new HashMap<>();
        for (Map.Entry<ChargeLocation, Integer> entry : map.entrySet()) {
            if (entry.getKey().dimension().equals(dimension)) {
                result.put(entry.getKey().pos(), entry.getValue());
            }
        }
        return result;
    }

    public static void onC4Removed(Level level, BlockPos pos) {
        ChargeLocation loc = new ChargeLocation(level.dimension(), pos);
        for (Map<ChargeLocation, Integer> map : ARMED_CHARGES.values()) {
            map.remove(loc);
        }
        ARMED_CHARGES.entrySet().removeIf(e -> e.getValue().isEmpty());
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
                ChargeLocation loc = new ChargeLocation(level.dimension(), pos);
                Map<ChargeLocation, Integer> playerCharges = ARMED_CHARGES.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
                int currentLevel = playerCharges.getOrDefault(loc, 0);

                // Cycle: 0 -> 1 -> 2 -> 3 -> 1
                int newLevel = currentLevel >= 3 ? 1 : currentLevel + 1;
                playerCharges.put(loc, newLevel);

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
            Map<ChargeLocation, Integer> charges = ARMED_CHARGES.get(player.getUUID());
            int sprayedCount = BreachingSprayManager.getSprayedBlockCount(player.getUUID());

            boolean hasC4 = charges != null && !charges.isEmpty();
            boolean hasSpray = sprayedCount > 0;

            if (!hasC4 && !hasSpray) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.5f);
                player.sendSystemMessage(Component.literal(
                    "§c📡 [DETONÁTOR] §7Nincs élesített C4 töltet vagy befújt fal! Használj C4-et vagy Falbontó Spray-t."
                ));
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            // Click trigger sound
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 1.2f, 1.0f);

            int detonatedC4Count = 0;
            int maxLevel = 1;

            if (hasC4) {
                List<ChargeLocation> toRemove = new ArrayList<>();
                for (Map.Entry<ChargeLocation, Integer> entry : charges.entrySet()) {
                    ChargeLocation loc = entry.getKey();
                    int yieldLevel = entry.getValue();
                    if (yieldLevel > maxLevel) maxLevel = yieldLevel;

                    ServerLevel chargeLevel = level.getServer() != null ? level.getServer().getLevel(loc.dimension()) : null;
                    if (chargeLevel == null) {
                        toRemove.add(loc);
                        continue;
                    }

                    if (chargeLevel.isLoaded(loc.pos())) {
                        if (chargeLevel.getBlockState(loc.pos()).is(ModBlocks.C4_BLOCK)) {
                            float power = yieldLevel == 1 ? 4.5f : (yieldLevel == 2 ? 9.0f : 18.0f);
                            C4Block.explodeWithPower(chargeLevel, loc.pos(), power);
                            detonatedC4Count++;
                        }
                        toRemove.add(loc);
                    }
                }
                for (ChargeLocation loc : toRemove) {
                    charges.remove(loc);
                }
                if (charges.isEmpty()) {
                    ARMED_CHARGES.remove(player.getUUID());
                }
            }

            // Detonate sprayed wall breaches!
            int breachedBlockCount = 0;
            if (hasSpray && level instanceof ServerLevel serverLevel) {
                breachedBlockCount = BreachingSprayManager.detonate(serverLevel, player.getUUID());
            }

            if (detonatedC4Count > 0 || breachedBlockCount > 0) {
                StringBuilder msg = new StringBuilder("§a💥 [ROBBANTÁS SIKERES!] ");
                if (breachedBlockCount > 0 && detonatedC4Count > 0) {
                    msg.append("§f").append(detonatedC4Count).append(" C4 töltet és §6").append(breachedBlockCount).append(" falblokk §felrobbantva!");
                } else if (breachedBlockCount > 0) {
                    msg.append("§6FAL ÁTTÖRVE: §f").append(breachedBlockCount).append(" befújt falblokk kirobbantva!");
                } else {
                    if (maxLevel == 3) {
                        msg.append("§4🔥 MEGA ROBBANÁS! (").append(detonatedC4Count).append(" C4 töltet)");
                    } else if (maxLevel == 2) {
                        msg.append("§6DUPLA erejű robbantás! (").append(detonatedC4Count).append(" C4 töltet)");
                    } else {
                        msg.append("§f").append(detonatedC4Count).append(" C4 töltet felrobbantva!");
                    }
                }
                player.sendSystemMessage(Component.literal(msg.toString()));
            } else {
                player.sendSystemMessage(Component.literal(
                    "§e📡 [DETONÁTOR] §7Az élesített töltetek már megsemmisültek vagy hiányoznak."
                ));
            }

            player.getCooldowns().addCooldown(this, 30);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
    public static void clearRuntime() {
        ARMED_CHARGES.clear();
    }
}
