package com.hdkiller.spycraft.item;

import com.hdkiller.spycraft.block.ModBlocks;
import com.hdkiller.spycraft.laser.LaserForcefieldManager;
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
import net.minecraft.world.level.block.Block;

import java.util.Iterator;

/**
 * Laser Forcefield Remote Controller
 * 1. Right-click 3 or more Laser Pylon blocks to link them into a closed perimeter.
 * 2. Right-click in the air to toggle the impenetrable Laser Forcefield Wall ON or OFF!
 * 3. Sneak + Right-click to clear the linked network.
 */
public class LaserRemoteItem extends Item {
    public LaserRemoteItem(Properties properties) {
        super(properties);
    }

    /**
     * Link Laser Pylons by right-clicking them
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        // Sneak + click to reset network
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                LaserForcefieldManager.clearNetwork(player.getUUID(), level);
                player.sendSystemMessage(Component.literal("§e🗑️ [LÉZER HÁLÓZAT TÖRLVE] §7Minden összekötött oszlop leválasztva."));
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.2f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (level.getBlockState(pos).is(ModBlocks.LASER_PYLON)) {
            if (!level.isClientSide) {
                var network = LaserForcefieldManager.getNetwork(player.getUUID());
                if (network.dimension != null && !network.dimension.equals(level.dimension())) {
                    LaserForcefieldManager.clearNetwork(player.getUUID(), level);
                    network = LaserForcefieldManager.getNetwork(player.getUUID());
                    player.sendSystemMessage(Component.literal("§e⚠️ Dimenzióváltás: a korábbi hálózat törölve lett. Új hálózat indult."));
                }
                network.dimension = level.dimension();

                if (!network.pylons.contains(pos)) {
                    network.pylons.add(pos);

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                            20, 0.2, 0.2, 0.2, 0.05);

                        float pitch = Math.min(0.8f + network.pylons.size() * 0.25f, 2.0f);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.2f, pitch);
                    }

                    int count = network.pylons.size();
                    player.sendSystemMessage(Component.literal(
                        "§a🔗 [LÉZEROSZLOP #" + count + " CSATLAKOZTATVA!] §f[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]"
                    ));

                    if (count >= 3) {
                        player.sendSystemMessage(Component.literal(
                            "§a  ↳ ✅ Körlet kész (" + count + " pont)! Kattints a levegőbe az ERŐPAJZS aktiválásához!"
                        ));
                    } else {
                        player.sendSystemMessage(Component.literal(
                            "§7  ↳ Még legalább " + (3 - count) + " oszlop szükséges a zárt körlethez."
                        ));
                    }
                } else {
                    // Right clicking an already connected pylon unlinks it!
                    network.pylons.remove(pos);
                    boolean wasActive = network.active;
                    if (level.getBlockState(pos).is(ModBlocks.LASER_PYLON) && level.getBlockState(pos).hasProperty(com.hdkiller.spycraft.block.LaserPylonBlock.ACTIVE)) {
                        level.setBlock(pos, level.getBlockState(pos).setValue(com.hdkiller.spycraft.block.LaserPylonBlock.ACTIVE, false), Block.UPDATE_ALL);
                    }
                    if (wasActive) {
                        LaserForcefieldManager.setNetworkPylonsActive(level, network, false);
                    }

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SMOKE,
                            pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                            15, 0.2, 0.2, 0.2, 0.05);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0f, 0.8f);
                        if (wasActive) {
                            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.2f, 0.8f);
                        }
                    }

                    int remaining = network.pylons.size();
                    player.sendSystemMessage(Component.literal(
                        "§e✂️ [LÉZEROSZLOP LEVÁLASZTVA!] §7[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "] " +
                        (wasActive ? "§c(Az erőpajzs kikapcsolt!) " : "") +
                        "§7Megmaradt: " + remaining + " oszlop."
                    ));
                }

                player.getCooldowns().addCooldown(this, 10);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    /**
     * Toggle Forcefield ON / OFF by right-clicking in the air
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // Sneak + click in air clears network
            if (player.isShiftKeyDown()) {
                LaserForcefieldManager.clearNetwork(player.getUUID(), level);
                player.sendSystemMessage(Component.literal("§e🗑️ [LÉZER HÁLÓZAT TÖRLVE] §7Minden összekötött oszlop leválasztva."));
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.2f);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            var network = LaserForcefieldManager.getNetwork(player.getUUID());
            if (network.dimension != null && !network.dimension.equals(level.dimension())) {
                player.sendSystemMessage(Component.literal(
                    "§c📡 [LÉZER CSAPDA] §7A csatlakoztatott oszlopok egy másik dimenzióban találhatók!"
                ));
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            // Prune broken/missing pylons from network before toggling
            boolean prunedAny = false;
            Iterator<BlockPos> it = network.pylons.iterator();
            while (it.hasNext()) {
                BlockPos p = it.next();
                if (level.isLoaded(p) && !level.getBlockState(p).is(ModBlocks.LASER_PYLON)) {
                    it.remove();
                    prunedAny = true;
                }
            }
            if (prunedAny) {
                LaserForcefieldManager.setNetworkPylonsActive(level, network, false);
            }

            int count = network.pylons.size();

            if (count < 3) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.5f);
                player.sendSystemMessage(Component.literal(
                    "§c📡 [LÉZER CSAPDA] §7Legalább 3 épp lézeroszlop kell az erőpajzshoz! (Jelenleg: " + count + ")"
                ));
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            // Toggle active state
            boolean newActive = !network.active;
            LaserForcefieldManager.setNetworkPylonsActive(level, network, newActive);

            if (network.active) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5f, 1.2f);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.2f, 1.6f);

                player.sendSystemMessage(Component.literal(
                    "§c🚨 [LÉZER ERŐPAJZS AKTIVÁLVA!] §4Áthatolhatatlan erőpajzs fal élesítve " + count + " oszlop között!"
                ));
            } else {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.5f, 0.8f);

                player.sendSystemMessage(Component.literal(
                    "§a🛡️ [LÉZER ERŐPAJZS KIKAPCSOLVA] §7A lézerfal leállt, a terület szabadon átjárható."
                ));
            }

            player.getCooldowns().addCooldown(this, 15);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
