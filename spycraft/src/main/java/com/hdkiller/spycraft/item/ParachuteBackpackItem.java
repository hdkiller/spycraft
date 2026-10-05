package com.hdkiller.spycraft.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tactical Parachute Backpack (Taktikai Ejtőernyős Hátizsák)
 * 1. Wear on chest (EquipmentSlot.CHEST):
 *    - Auto-deploys when jumping/falling from high places!
 *    - Or press Jump (Space) while falling for instant manual deployment.
 * 2. Hold in hand / inventory:
 *    - Right-click in mid-air (or hit with it) to emergency deploy from hand!
 * 3. Parachute mechanics:
 *    - Gentle, steerable gliding in the direction the player looks.
 *    - 100% negates fall damage (fallDistance = 0).
 *    - Automatically folds up and resets upon touchdown on the ground.
 */
public class ParachuteBackpackItem extends ArmorItem {

    private static final Set<UUID> ACTIVE_PARACHUTES = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ParachuteBackpackItem(Properties properties) {
        super(ArmorMaterials.LEATHER, Type.CHESTPLATE, properties);
    }

    public static boolean isParachuting(Player player) {
        return ACTIVE_PARACHUTES.contains(player.getUUID());
    }

    public static void deployParachute(Player player, Level level) {
        if (level.isClientSide) return;
        if (ACTIVE_PARACHUTES.add(player.getUUID())) {
            player.fallDistance = 0.0f;
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, false, false, false));

            if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                // Deployment audio
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_ELYTRA.value(), SoundSource.PLAYERS, 1.5f, 0.9f);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.2f, 1.4f);

                // Canopy burst particles above player
                double px = player.getX();
                double py = player.getY() + 3.0;
                double pz = player.getZ();
                serverLevel.sendParticles(ParticleTypes.CLOUD, px, py, pz, 15, 0.8, 0.3, 0.8, 0.05);

                player.sendSystemMessage(Component.translatable("message.spycraft.parachute.deployed"));
            }
        }
    }

    public static void closeParachute(Player player, Level level) {
        if (ACTIVE_PARACHUTES.remove(player.getUUID())) {
            player.removeEffect(MobEffects.SLOW_FALLING);
            player.fallDistance = 0.0f;

            if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WOOL_STEP, SoundSource.PLAYERS, 1.0f, 1.2f);
                player.sendSystemMessage(Component.translatable("message.spycraft.parachute.landed"));
            }
        }
    }

    public static void onPlayerDisconnect(UUID uuid) {
        ACTIVE_PARACHUTES.remove(uuid);
    }

    public static void tick(MinecraftServer server) {
        ACTIVE_PARACHUTES.removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player);
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        boolean isWorn = player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.PARACHUTE_BACKPACK);
        boolean isHeld = player.getMainHandItem().is(ModItems.PARACHUTE_BACKPACK)
                || player.getOffhandItem().is(ModItems.PARACHUTE_BACKPACK);
        UUID uuid = player.getUUID();
        boolean parachuting = ACTIVE_PARACHUTES.contains(uuid);

        // If unequipped or dropped mid-flight, immediately close parachute
        if (parachuting && !isWorn && !isHeld) {
            closeParachute(player, level);
            return;
        }

        // Only manage parachute if player is wearing or holding this backpack
        if (!isWorn && !isHeld) {
            return;
        }

        if (parachuting) {
            // Touchdown check: ground, water, climbing, or dead
            if (player.onGround() || player.isInWater() || player.isDeadOrDying() || player.isFallFlying()) {
                closeParachute(player, level);
                return;
            }

            // Parachute Gliding Physics
            player.fallDistance = 0.0f;
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 10, 0, false, false, false));

            Vec3 vel = player.getDeltaMovement();
            Vec3 look = player.getLookAngle();

            // Safe vertical sink rate (-0.15 is gentle and steady descent)
            double targetY = Math.max(vel.y, -0.15);

            // Steerable forward gliding in the look direction
            double steerSpeed = 0.038;
            double targetX = vel.x * 0.94 + look.x * steerSpeed;
            double targetZ = vel.z * 0.94 + look.z * steerSpeed;

            player.setDeltaMovement(targetX, targetY, targetZ);
            player.hurtMarked = true;

            // Visual Parachute Canopy particles above player
            if (level.getGameTime() % 4 == 0) {
                double px = player.getX();
                double py = player.getY() + 2.8;
                double pz = player.getZ();

                // Canopy arc particles
                for (double angle = -Math.PI / 2; angle <= Math.PI / 2; angle += Math.PI / 5) {
                    double ox = Math.cos(angle) * 1.3;
                    double oz = Math.sin(angle) * 1.3;
                    level.sendParticles(ParticleTypes.CLOUD, px + ox, py + (0.8 - Math.abs(angle) * 0.3), pz + oz,
                        1, 0, 0, 0, 0.01);
                }
                // Suspension lines
                level.sendParticles(ParticleTypes.CRIT, px, py - 0.4, pz, 2, 0.2, 0.3, 0.2, 0.02);
            }
        } else {
            // Fall Detection for Auto-Deploy (only when worn on chest)
            if (isWorn && player.isAlive() && !player.onGround() && !player.isInWater() && !player.isFallFlying()) {
                Vec3 vel = player.getDeltaMovement();
                // 1. Auto-deploy on real falls from height (>=4.0 blocks fallen with downward velocity, or >=5.5 blocks)
                // Prevents annoying triggers on normal jumping or hopping down small 1-2 block ledges.
                if ((player.fallDistance >= 4.0f && vel.y < -0.5) || player.fallDistance >= 5.5f) {
                    deployParachute(player, level);
                }
                // 2. Manual key-press deploy while falling: Pressing Sneak (Crouch) during a descent
                else if (player.isCrouching() && player.fallDistance > 2.0f && vel.y < -0.2) {
                    deployParachute(player, level);
                }
            }
        }
    }

    /**
     * Hand activation: Right-click in mid-air deploys the parachute from hand!
     * On ground: Normal armor equip behavior.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.onGround() && !player.isInWater() && !player.isFallFlying()) {
            deployParachute(player, level);
            player.getCooldowns().addCooldown(this, 15);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        // On ground: standard chestplate equip
        return super.use(level, player, hand);
    }

    /**
     * Hand activation when clicking on a block in mid-air
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && !player.onGround() && !player.isInWater()) {
            deployParachute(player, context.getLevel());
            player.getCooldowns().addCooldown(this, 15);
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }
        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spycraft.parachute_backpack.desc"));
        tooltip.add(Component.translatable("tooltip.spycraft.parachute_backpack.worn"));
        tooltip.add(Component.translatable("tooltip.spycraft.parachute_backpack.hand"));
        tooltip.add(Component.translatable("tooltip.spycraft.parachute_backpack.glide"));
    }
    public static void clearRuntime() {
        ACTIVE_PARACHUTES.clear();
    }
}
