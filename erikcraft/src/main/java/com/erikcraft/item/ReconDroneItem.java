package com.erikcraft.item;

import com.erikcraft.drone.ReconDroneManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Reconnaissance Drone (Felderítő Drón)
 * - Right-Click: Launch flying Recon Drone for 15 seconds!
 * - While Piloting:
 *     - Right-Click on Target/Ground: Laser Tag (Célmegjelölő Lézer) - 96m towering beacon & tracking bug!
 *     - Right-Click towards Sky: Safe Recall (Visszahívás a kiindulópontra)!
 *     - Sneak (Shift) + Right-Click: Kamikaze Dive & Detonation (Önmegsemmisítő robbanás)!
 */
public class ReconDroneItem extends Item {
    public ReconDroneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (ReconDroneManager.isPiloting(serverPlayer.getUUID())) {
                ReconDroneManager.DroneSession session = ReconDroneManager.getSession(serverPlayer.getUUID());

                if (serverPlayer.isShiftKeyDown()) {
                    // Shift + Right-Click = Kamikaze Self-Destruct
                    ReconDroneManager.triggerKamikaze(serverPlayer);
                } else if (serverPlayer.getXRot() < -50.0f) {
                    // Look high up into the sky + Right-Click = Safe Recall
                    if (session != null) {
                        ReconDroneManager.recallPlayer(serverPlayer, session,
                            "§b🛸 [DRÓN] §fKézi visszahívás parancs végrehajtva. Pilóta biztonságban visszatért.");
                    }
                } else {
                    // Normal Right-Click = Precision Laser Tag
                    ReconDroneManager.triggerLaserTag(serverPlayer);
                    player.getCooldowns().addCooldown(this, 10);
                }
            } else {
                // Launch Drone! (15 seconds = 300 ticks)
                ReconDroneManager.startSession(serverPlayer, 300);
                player.getCooldowns().addCooldown(this, 20);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
