package com.hdkiller.spycraft.item;

import com.hdkiller.spycraft.block.C4Block;
import com.hdkiller.spycraft.block.LaserPylonBlock;
import com.hdkiller.spycraft.block.ModBlocks;
import com.hdkiller.spycraft.block.SoundTrapBlock;
import com.hdkiller.spycraft.laser.LaserForcefieldManager;
import com.hdkiller.spycraft.sound.SoundTrapManager;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tactical Universal Remote Control (Intelligent / Context-Aware)
 *
 * 1. Direct Block Interaction (useOn):
 *    - C4 Canister: Cycle explosive yield (1x -> 2x -> 3x Mega).
 *    - Laser Pylon: Link / unlink pylon to form forcefield perimeter (Shift-click to clear).
 *    - Sound Trap: Toggle audio decoy lure on/off.
 *
 * 2. Distance Aiming / Raycast (use):
 *    - Pointing at Laser Pylon (up to 48m): Remotely toggle laser forcefield.
 *    - Pointing at Sound Trap (up to 48m): Remotely toggle sound decoy.
 *
 * 3. Air Activation (use):
 *    - Regular Click:
 *      * If C4 or Breaching Foam is primed -> DETONATE ALL CHARGES!
 *      * If no C4 is primed -> Toggle nearest Laser Forcefield ON/OFF!
 *      * Standby HUD status if no devices are active.
 *    - Sneak + Click in Air:
 *      * Dedicated safe Laser Forcefield toggle (never detonates C4).
 *      * Pitch > 70 deg (looking down): Reset / clear laser network.
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

    public static void clearRuntime() {
        ARMED_CHARGES.clear();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        // --- CONTEXT 1: C4 Explosive Canister ---
        if (state.is(ModBlocks.C4_BLOCK)) {
            return handleC4Interaction(level, pos, player);
        }

        // --- CONTEXT 2: Laser Pylon Grid ---
        if (state.is(ModBlocks.LASER_PYLON)) {
            return handleLaserPylonInteraction(level, pos, player);
        }

        // --- CONTEXT 3: Sound Trap Decoy ---
        if (state.is(ModBlocks.SOUND_TRAP)) {
            return handleSoundTrapInteraction(level, pos, player, state);
        }

        return InteractionResult.PASS;
    }

    private InteractionResult handleC4Interaction(Level level, BlockPos pos, Player player) {
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

    private InteractionResult handleLaserPylonInteraction(Level level, BlockPos pos, Player player) {
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

        if (!level.isClientSide) {
            var network = LaserForcefieldManager.getNetwork(player.getUUID());
            network.dimension = level.dimension();

            // Check if pos is already in an existing trap -> Unlink it
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

    private InteractionResult handleSoundTrapInteraction(Level level, BlockPos pos, Player player, BlockState state) {
        if (!level.isClientSide) {
            boolean active = !state.getValue(SoundTrapBlock.ACTIVE);
            level.setBlock(pos, state.setValue(SoundTrapBlock.ACTIVE, active), Block.UPDATE_ALL);

            if (active) {
                SoundTrapManager.registerTrap(level, pos);
                level.playSound(null, pos, SoundEvents.CREEPER_PRIMED, SoundSource.BLOCKS, 1.5f, 1.0f);
                level.scheduleTick(pos, ModBlocks.SOUND_TRAP, 20);
                player.displayClientMessage(Component.literal("§e🔊 [HANGCSAPDA AKTIVÁLVA] §aCsalogató Creeper sziszegés bekapcsolva!"), true);
            } else {
                SoundTrapManager.removeTrap(level, pos);
                level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 1.0f, 1.0f);
                player.displayClientMessage(Component.literal("§e🔊 [HANGCSAPDA KIKAPCSOLVA] §7Készenléti mód."), true);
            }

            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.1);
            }
            player.getCooldowns().addCooldown(this, 10);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        // --- CONTEXT A: Raycast Aiming at Distance (up to 48 blocks) ---
        Vec3 eyePos = player.getEyePosition();
        Vec3 viewVec = player.getViewVector(1.0f);
        Vec3 reachVec = eyePos.add(viewVec.scale(48.0));
        BlockHitResult hit = level.clip(new ClipContext(
            eyePos,
            reachVec,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos targetPos = hit.getBlockPos();
            BlockState targetState = level.getBlockState(targetPos);

            // 1. Aiming directly at a Laser Pylon from range -> Toggle its trap
            if (targetState.is(ModBlocks.LASER_PYLON)) {
                var network = LaserForcefieldManager.getNetwork(player.getUUID());
                LaserForcefieldManager.LaserTrap targetTrap = null;
                for (LaserForcefieldManager.LaserTrap t : network.traps) {
                    if (level.dimension().equals(t.dimension) && t.pylons.contains(targetPos)) {
                        targetTrap = t;
                        break;
                    }
                }
                if (targetTrap == null) {
                    targetTrap = network.findNearestTrap(level.dimension(), targetPos, 16.0);
                }
                if (targetTrap != null) {
                    return toggleLaserTrap(level, player, stack, network, targetTrap);
                }
            }

            // 2. Aiming directly at a Sound Trap from range -> Toggle it
            if (targetState.is(ModBlocks.SOUND_TRAP)) {
                boolean active = !targetState.getValue(SoundTrapBlock.ACTIVE);
                level.setBlock(targetPos, targetState.setValue(SoundTrapBlock.ACTIVE, active), Block.UPDATE_ALL);

                if (active) {
                    SoundTrapManager.registerTrap(level, targetPos);
                    level.playSound(null, targetPos, SoundEvents.CREEPER_PRIMED, SoundSource.BLOCKS, 2.0f, 1.0f);
                    level.scheduleTick(targetPos, ModBlocks.SOUND_TRAP, 20);
                    int dist = (int) Math.sqrt(player.blockPosition().distSqr(targetPos));
                    player.displayClientMessage(Component.literal("§e🔊 [TÁVOLI HANGCSAPDA] §aAktiválva (" + dist + "m távolságból)!"), true);
                } else {
                    SoundTrapManager.removeTrap(level, targetPos);
                    level.playSound(null, targetPos, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 1.2f, 1.0f);
                    player.displayClientMessage(Component.literal("§e🔊 [TÁVOLI HANGCSAPDA] §cKikapcsolva!"), true);
                }

                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.NOTE, targetPos.getX() + 0.5, targetPos.getY() + 0.9, targetPos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.1);
                }
                player.getCooldowns().addCooldown(this, 15);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }
        }

        // --- CONTEXT B: Sneak + Right-Click in Air (Dedicated Laser Control) ---
        if (player.isShiftKeyDown()) {
            var network = LaserForcefieldManager.getNetwork(player.getUUID());

            // Pitch > 70 deg (looking down while crouching) = Reset / Clear Network
            if (player.getXRot() > 70.0f) {
                LaserForcefieldManager.LaserTrap nearTrap = network.findNearestTrap(level.dimension(), player.blockPosition(), 64.0);
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
                player.getCooldowns().addCooldown(this, 15);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            // Normal sneak + click in air = Toggle laser forcefield safely (NEVER detonates C4)
            LaserForcefieldManager.LaserTrap targetTrap = network.findNearestTrap(level.dimension(), player.blockPosition(), 64.0);
            if (targetTrap != null) {
                return toggleLaserTrap(level, player, stack, network, targetTrap);
            } else {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.5f);
                if (network.traps.isEmpty()) {
                    player.displayClientMessage(Component.literal("§c📡 [LÉZER HÁLÓZAT] §7Nincs beállított lézer csapda!"), true);
                } else {
                    player.displayClientMessage(Component.literal("§c📡 [LÉZER HÁLÓZAT] §7Nincs lézer csapda a közeledben (64 blokk)!"), true);
                }
                player.getCooldowns().addCooldown(this, 10);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }
        }

        // --- CONTEXT C: Normal Right-Click in Air ---
        Map<ChargeLocation, Integer> charges = ARMED_CHARGES.get(player.getUUID());
        int sprayedCount = BreachingSprayManager.getSprayedBlockCount(player.getUUID());
        boolean hasC4 = charges != null && !charges.isEmpty();
        boolean hasSpray = sprayedCount > 0;

        // Priority 1: Detonate C4 and Breaching Foam
        if (hasC4 || hasSpray) {
            return triggerExplosives(level, player, stack, charges, hasC4, hasSpray);
        }

        // Priority 2: Toggle nearest Laser Forcefield if present
        var network = LaserForcefieldManager.getNetwork(player.getUUID());
        LaserForcefieldManager.LaserTrap targetTrap = network.findNearestTrap(level.dimension(), player.blockPosition(), 64.0);
        if (targetTrap != null && targetTrap.pylons.size() >= 2) {
            return toggleLaserTrap(level, player, stack, network, targetTrap);
        }

        // Priority 3: Standby HUD Status
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6f, 1.6f);
        player.displayClientMessage(Component.literal(
            "§7📡 [UNIVERZÁLIS TÁVIRÁNYÍTÓ] §8Készenlétben §8| §f0 élesített C4 §8| §7Nincs lézerfal a közelben (64m)"
        ), true);
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private InteractionResultHolder<ItemStack> toggleLaserTrap(
            Level level, Player player, ItemStack stack,
            LaserForcefieldManager.ForcefieldNetwork network,
            LaserForcefieldManager.LaserTrap targetTrap) {

        // Prune broken/missing pylons
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
                "§c📡 [LÉZER CSAPDA #" + trapNum + "] §7Legalább 2 ép oszlop kell az erőpajzshoz! (Jelenleg: " + count + ")"
            ));
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

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
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private InteractionResultHolder<ItemStack> triggerExplosives(
            Level level, Player player, ItemStack stack,
            Map<ChargeLocation, Integer> charges,
            boolean hasC4, boolean hasSpray) {

        // Trigger click sound
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
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spycraft.remote_detonator.desc"));
        tooltip.add(Component.translatable("tooltip.spycraft.remote_detonator.c4"));
        tooltip.add(Component.translatable("tooltip.spycraft.remote_detonator.laser"));
        tooltip.add(Component.translatable("tooltip.spycraft.remote_detonator.sound"));
    }
}
