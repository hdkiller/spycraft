package com.erikcraft.item;

import com.erikcraft.drone.ReconDroneManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Reconnaissance Drone (Felderítő Drón)
 * - 60s High-Capacity Battery (1200 ticks)
 * - 5x Pneumatic Tranquilizer Sleep Darts
 * - Right-Click: Launch Drone into flight!
 * - Sneak + Right-Click: Instant Quick-Charge & Reload using Redstone!
 * - Auto trickle-recharges while resting in your inventory.
 */
public class ReconDroneItem extends Item {
    public static final int MAX_BATTERY = 1200; // 60 seconds
    public static final int MAX_DARTS = 5;

    public ReconDroneItem(Properties properties) {
        super(properties);
    }

    public static int getBattery(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return tag.contains("battery") ? tag.getInt("battery") : MAX_BATTERY;
    }

    public static void setBattery(ItemStack stack, int battery) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putInt("battery", Math.clamp(battery, 0, MAX_BATTERY));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static int getDarts(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return tag.contains("darts") ? tag.getInt("darts") : MAX_DARTS;
    }

    public static void setDarts(ItemStack stack, int darts) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        tag.putInt("darts", Math.clamp(darts, 0, MAX_DARTS));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int battery = getBattery(stack);
        return Math.round(13.0f * (float) battery / MAX_BATTERY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float pct = (float) getBattery(stack) / MAX_BATTERY;
        return pct > 0.4f ? 0x00FF88 : (pct > 0.2f ? 0xFFDD00 : 0xFFFF3333);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int battery = getBattery(stack);
        int darts = getDarts(stack);
        int secs = (battery + 19) / 20;
        int pct = (battery * 100) / MAX_BATTERY;

        tooltip.add(Component.literal("§b🔋 Akkumulátor: §e" + pct + "% §7(" + secs + "mp / 60mp)"));
        tooltip.add(Component.literal("§3💤 Altató lövedékek: §a" + darts + " / 5 db"));
        tooltip.add(Component.literal("§7[Guggolva Jobb klikk Redstone-nal az azonnali feltöltéshez]"));
        tooltip.add(Component.literal("§8Hátizsákban pihenve automatikusan töltődik"));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof Player player) {
            // Trickle-recharge while resting in inventory when NOT piloting
            if (!ReconDroneManager.isPiloting(player.getUUID())) {
                int battery = getBattery(stack);
                if (battery < MAX_BATTERY && level.getGameTime() % 2 == 0) {
                    setBattery(stack, battery + 1);
                }
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (ReconDroneManager.isPiloting(serverPlayer.getUUID())) {
                // Actions handled directly by ServerPlayerGameModeMixin
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            if (serverPlayer.isShiftKeyDown()) {
                // Sneak + Right-Click = Quick-Charge using Redstone
                boolean recharged = false;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack invStack = player.getInventory().getItem(i);
                    if (invStack.is(Items.REDSTONE) || invStack.is(Items.REDSTONE_BLOCK)) {
                        if (!player.isCreative()) {
                            invStack.shrink(1);
                        }
                        setBattery(stack, MAX_BATTERY);
                        setDarts(stack, MAX_DARTS);
                        recharged = true;
                        break;
                    }
                }

                if (recharged) {
                    ServerLevel slevel = serverPlayer.serverLevel();
                    slevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 2.0f);
                    slevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        25, 0.4, 0.4, 0.4, 0.1);

                    player.sendSystemMessage(Component.literal(
                        "§b⚡ [GYORS-TÖLTÉS] §aAkkumulátor 100%-ra töltve (60s), és 5/5 altató lövedék betöltve!"
                    ));
                } else {
                    int battery = getBattery(stack);
                    int darts = getDarts(stack);
                    player.sendSystemMessage(Component.literal(
                        "§e🔋 [DRÓN STÁTUSZ] §fAkku: §e" + (battery * 100 / MAX_BATTERY) + "% §8(" + (battery / 20) + "s) §8| §fAltató lövedék: §a" + darts + "/5 db\n" +
                        "§7💡 Tartsd nálad Redstone-t és nyomj Guggolva Jobb klikket a gyors-töltéshez!"
                    ));
                }

                player.getCooldowns().addCooldown(this, 10);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            // Normal Right-Click = Launch Drone!
            int battery = getBattery(stack);
            if (battery < 60) {
                serverPlayer.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0f, 1.2f);
                player.sendSystemMessage(Component.literal(
                    "§c⚠️ [LEMERÜLT] Az akkumulátor lemerült (" + (battery / 20) + "s)! Töltsd fel Redstone-nal (Shift + Jobb klikk) vagy várj a hátizsákban."
                ));
                return InteractionResultHolder.fail(stack);
            }

            int darts = getDarts(stack);
            ReconDroneManager.startSession(serverPlayer, battery, darts);
            player.getCooldowns().addCooldown(this, 20);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
