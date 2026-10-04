package com.hdkiller.spycraft.item;

import com.hdkiller.spycraft.block.LaserPylonBlock;
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
 * 1. Right-click Laser Pylon blocks to link them into an impenetrable perimeter.
 *    Pylons close together (within 32 blocks) automatically join the same trap.
 *    Pylons placed further away automatically form separate, independent laser traps!
 * 2. Right-click in the air near a trap to toggle that trap ON or OFF!
 * 3. Sneak + Right-click to clear the nearest trap (or all traps).
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
                var network = LaserForcefieldManager.getNetwork(player.getUUID());
                LaserForcefieldManager.LaserTrap nearTrap = network.findNearestTrap(level.dimension(), pos, 64.0);
                if (nearTrap != null) {
                    int trapNum = network.traps.indexOf(nearTrap) + 1;
                    LaserForcefieldManager.clearTrap(level, network, nearTrap);
                    player.sendSystemMessage(Component.literal("§e🗑️ [#" + trapNum + " LÉZER CSAPDA TÖRLVE] §7A közeli csapda oszlopai leválasztva."));
                } else {
                    LaserForcefieldManager.clearNetwork(player.getUUID(), level);
                    player.sendSystemMessage(Component.literal("§e🗑️ [MINDEN LÉZER CSAPDA TÖRLVE] §7Minden összekötött oszlop leválasztva."));
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.2f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (level.getBlockState(pos).is(ModBlocks.LASER_PYLON)) {
            if (!level.isClientSide) {
                var network = LaserForcefieldManager.getNetwork(player.getUUID());
                network.dimension = level.dimension();

                // 1. Check if pos is already in an existing trap -> Unlink it
                LaserForcefieldManager.LaserTrap existingTrap = null;
                for (LaserForcefieldManager.LaserTrap t : network.traps) {
                    if (level.dimension().equals(t.dimension) && t.pylons.contains(pos)) {
                        existingTrap = t;
                        break;
                    }
                }

                if (existingTrap != null) {
                    existingTrap.pylons.remove(pos);
                    boolean wasActive = existingTrap.active;
                    if (level.getBlockState(pos).is(ModBlocks.LASER_PYLON) && level.getBlockState(pos).hasProperty(LaserPylonBlock.ACTIVE)) {
                        level.setBlock(pos, level.getBlockState(pos).setValue(LaserPylonBlock.ACTIVE, false), Block.UPDATE_ALL);
                    }
                    if (wasActive) {
                        LaserForcefieldManager.setTrapPylonsActive(level, existingTrap, false);
                    }

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.SMOKE,
                            pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                            15, 0.2, 0.2, 0.2, 0.05);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0f, 0.8f);
                    }

                    int remaining = existingTrap.pylons.size();
                    int trapNum = network.traps.indexOf(existingTrap) + 1;
                    player.sendSystemMessage(Component.literal(
                        "§e✂️ [LÉZEROSZLOP LEVÁLASZTVA!] §7#" + trapNum + " Csapda [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "] " +
                        (wasActive ? "§c(A csapda kikapcsolt!) " : "") +
                        "§7Megmaradt: " + remaining + " oszlop."
                    ));

                    if (remaining == 0) {
                        network.traps.remove(existingTrap);
                    }
                } else {
                    // 2. Pos is NOT in any trap: find nearest trap within MAX_LINK_DISTANCE (32 blocks)
                    LaserForcefieldManager.LaserTrap targetTrap = null;
                    double bestDistSq = Double.MAX_VALUE;

                    for (LaserForcefieldManager.LaserTrap t : network.traps) {
                        if (t.dimension != null && !t.dimension.equals(level.dimension())) continue;
                        double d = t.getMinDistanceSqTo(pos);
                        if (d <= LaserForcefieldManager.MAX_LINK_DISTANCE_SQ && d < bestDistSq) {
                            bestDistSq = d;
                            targetTrap = t;
                        }
                    }

                    boolean isNewTrap = false;
                    if (targetTrap == null) {
                        targetTrap = new LaserForcefieldManager.LaserTrap(player.getUUID(), level.dimension());
                        network.traps.add(targetTrap);
                        isNewTrap = true;
                    }

                    targetTrap.pylons.add(pos);
                    int trapNum = network.traps.indexOf(targetTrap) + 1;
                    int count = targetTrap.pylons.size();

                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                            20, 0.2, 0.2, 0.2, 0.05);
                        float pitch = Math.min(0.8f + count * 0.25f, 2.0f);
                        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.2f, pitch);
                    }

                    if (isNewTrap) {
                        player.sendSystemMessage(Component.literal(
                            "§b✨ [ÚJ LÉZER CSAPDA #" + trapNum + " ELKEZDVE!] §f[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "] (1. oszlop)"
                        ));
                        player.sendSystemMessage(Component.literal(
                            "§7  ↳ Helyezz további oszlopokat a közelben (max 32 blokk), hogy láncot alkossanak!"
                        ));
                    } else {
                        player.sendSystemMessage(Component.literal(
                            "§a🔗 [LÉZEROSZLOP #" + count + " CSATLAKOZTATVA!] §7(#" + trapNum + " Csapda) §f[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]"
                        ));
                        if (count >= 2) {
                            player.sendSystemMessage(Component.literal(
                                "§a  ↳ ✅ Lánc kész (" + count + " oszlop)! Kattints a levegőbe az ERŐPAJZS aktiválásához!"
                            ));
                        }
                    }
                }

                LaserForcefieldManager.markDirty(level);
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
            var network = LaserForcefieldManager.getNetwork(player.getUUID());

            // Sneak + click in air clears nearest trap or all
            if (player.isShiftKeyDown()) {
                LaserForcefieldManager.LaserTrap nearTrap = network.findNearestTrap(level.dimension(), player.blockPosition(), 64.0);
                if (nearTrap != null) {
                    int trapNum = network.traps.indexOf(nearTrap) + 1;
                    LaserForcefieldManager.clearTrap(level, network, nearTrap);
                    player.sendSystemMessage(Component.literal("§e🗑️ [#" + trapNum + " LÉZER CSAPDA TÖRLVE] §7A közeli csapda oszlopai leválasztva."));
                } else {
                    LaserForcefieldManager.clearNetwork(player.getUUID(), level);
                    player.sendSystemMessage(Component.literal("§e🗑️ [MINDEN LÉZER CSAPDA TÖRLVE] §7Minden hálózat és oszlop törölve."));
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.2f);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            // Find nearest trap within 64 blocks
            LaserForcefieldManager.LaserTrap targetTrap = network.findNearestTrap(level.dimension(), player.blockPosition(), 64.0);


            if (targetTrap == null) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.5f);
                if (network.traps.isEmpty()) {
                    player.sendSystemMessage(Component.literal(
                        "§c📡 [LÉZER TÁVIRÁNYÍTÓ] §7Nincs egyetlen beállított lézer csapdád sem! Kattints előbb lézeroszlopokra a távirányítóval."
                    ));
                } else {
                    player.sendSystemMessage(Component.literal(
                        "§c📡 [LÉZER TÁVIRÁNYÍTÓ] §7Nem vagy egyetlen lézer csapdád közelében sem (64 blokk)! Menj közelebb a kívánt csapdához."
                    ));
                }
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            // Prune broken/missing pylons from target trap before toggling
            boolean prunedAny = false;
            Iterator<BlockPos> it = targetTrap.pylons.iterator();
            while (it.hasNext()) {
                BlockPos p = it.next();
                if (level.isLoaded(p) && !level.getBlockState(p).is(ModBlocks.LASER_PYLON)) {
                    it.remove();
                    prunedAny = true;
                }
            }
            if (prunedAny) {
                LaserForcefieldManager.setTrapPylonsActive(level, targetTrap, false);
            }

            int trapNum = network.traps.indexOf(targetTrap) + 1;
            int count = targetTrap.pylons.size();

            if (count < 2) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.5f);
                player.sendSystemMessage(Component.literal(
                    "§c📡 [LÉZER CSAPDA #" + trapNum + "] §7Legalább 2 épp oszlop kell az erőpajzshoz! (Jelenleg: " + count + ")"
                ));
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            // Toggle active state for this trap
            boolean newActive = !targetTrap.active;
            LaserForcefieldManager.setTrapPylonsActive(level, targetTrap, newActive);

            if (targetTrap.active) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5f, 1.2f);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.2f, 1.6f);

                player.sendSystemMessage(Component.literal(
                    "§c🚨 [LÉZER CSAPDA #" + trapNum + " AKTIVÁLVA!] §4Áthatolhatatlan erőpajzs fal élesítve " + count + " oszlop között!"
                ));
            } else {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.5f, 0.8f);

                player.sendSystemMessage(Component.literal(
                    "§a🛡️ [LÉZER CSAPDA #" + trapNum + " KIKAPCSOLVA] §7A lézerfal leállt, a terület szabadon átjárható."
                ));
            }

            player.getCooldowns().addCooldown(this, 15);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
