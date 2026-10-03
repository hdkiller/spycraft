package com.erikcraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * GPS Waypoint Navigator Visor (GPS Navigációs Szemüveg)
 * - Worn on head.
 * - Draws floating 3D waypoint direction arrows directly in the air in front of the player!
 * - Calculates relative turn directions (e.g. ⬆ Straight ahead, ➡ Turn right, ⬇ Turn around).
 * - Ideal for navigating across mountains and long distances to find your base or tracked mobs!
 */
public class GpsNavigatorGogglesItem extends ArmorItem {
    public GpsNavigatorGogglesItem(Properties properties) {
        super(ArmorMaterials.LEATHER, Type.HELMET, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof Player player) {
            boolean isWearing = player.getItemBySlot(EquipmentSlot.HEAD).getItem() == this;
            if (!isWearing) return;

            // Only update guidance every 4 ticks (0.2s) for smooth floating arrows
            if (level.getGameTime() % 4 != 0) return;

            MobTrackerItem.TrackedBlock trackedBlock = MobTrackerItem.getTrackedBlock(player.getUUID());
            LivingEntity trackedMob = MobTrackerItem.getTrackedMob(player.getUUID(), level);

            Vec3 targetPos = null;
            String targetName = null;

            // Prefer tracked block beacon, otherwise tracked mob (in same dimension)
            if (trackedBlock != null && trackedBlock.dimension().equals(level.dimension())) {
                BlockPos bpos = trackedBlock.pos();
                targetPos = new Vec3(bpos.getX() + 0.5, bpos.getY() + 0.5, bpos.getZ() + 0.5);
                targetName = "📍 " + trackedBlock.name();
            } else if (trackedMob != null && trackedMob.isAlive() && trackedMob.level().dimension().equals(level.dimension())) {
                targetPos = trackedMob.position();
                targetName = "📡 " + trackedMob.getName().getString();
            }

            if (targetPos != null && level instanceof ServerLevel serverLevel) {
                Vec3 eyePos = player.getEyePosition();
                Vec3 toTarget = targetPos.subtract(eyePos);
                double distance = toTarget.length();
                Vec3 dir = toTarget.normalize();

                // 1. Draw floating 3D guidance arrows in the air 2.5m - 4.5m ahead of the player
                Vec3 look = player.getLookAngle();
                Vec3 anchor = eyePos.add(look.scale(2.2));

                // Spawn 4 trail points along the target direction
                for (double step = 0.4; step <= 2.2; step += 0.45) {
                    Vec3 arrowPoint = anchor.add(dir.scale(step));
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        arrowPoint.x, arrowPoint.y, arrowPoint.z,
                        1, 0, 0, 0, 0);
                }

                // Arrowhead wings pointing towards target
                Vec3 tip = anchor.add(dir.scale(2.2));
                Vec3 perp = new Vec3(-dir.z, 0, dir.x).normalize().scale(0.35); // Perpendicular horizontal vector
                Vec3 wingBase = tip.subtract(dir.scale(0.5));
                Vec3 wingLeft = wingBase.add(perp);
                Vec3 wingRight = wingBase.subtract(perp);

                serverLevel.sendParticles(ParticleTypes.GLOW, tip.x, tip.y, tip.z, 1, 0, 0, 0, 0);
                serverLevel.sendParticles(ParticleTypes.GLOW, wingLeft.x, wingLeft.y, wingLeft.z, 1, 0, 0, 0, 0);
                serverLevel.sendParticles(ParticleTypes.GLOW, wingRight.x, wingRight.y, wingRight.z, 1, 0, 0, 0, 0);

                // 2. Relative turn calculation (relative to where player is currently looking)
                double playerYaw = Math.toDegrees(Math.atan2(-look.x, look.z));
                double targetYaw = Math.toDegrees(Math.atan2(-dir.x, dir.z));
                double diff = targetYaw - playerYaw;

                while (diff > 180) diff -= 360;
                while (diff < -180) diff += 360;

                String arrowSymbol;
                String instruction;

                if (Math.abs(diff) < 22.5) {
                    arrowSymbol = "⬆";
                    instruction = "§aEGYENESEN ELŐRE";
                } else if (diff >= 22.5 && diff < 67.5) {
                    arrowSymbol = "↗";
                    instruction = "§eENYHÉN JOBBRA";
                } else if (diff >= 67.5 && diff < 112.5) {
                    arrowSymbol = "➡";
                    instruction = "§6FORDULJ JOBBRA";
                } else if (diff >= 112.5 && diff < 157.5) {
                    arrowSymbol = "↘";
                    instruction = "§cJOBBRA HÁTRA";
                } else if (Math.abs(diff) >= 157.5) {
                    arrowSymbol = "⬇";
                    instruction = "§4FORDULJ MEG!";
                } else if (diff <= -112.5 && diff > -157.5) {
                    arrowSymbol = "↙";
                    instruction = "§cBALRA HÁTRA";
                } else if (diff <= -67.5 && diff > -112.5) {
                    arrowSymbol = "⬅";
                    instruction = "§6FORDULJ BALRA";
                } else {
                    arrowSymbol = "↖";
                    instruction = "§eENYHÉN BALRA";
                }

                // Elevation difference
                int elevDiff = (int) (targetPos.y - eyePos.y);
                String elev = elevDiff > 4 ? " §b[FENT 🔼]" : (elevDiff < -4 ? " §8[LENT 🔽]" : "");

                player.displayClientMessage(Component.literal(
                    "§e🧭 GPS: §f" + targetName + " §8| §e" + (int) distance + "m §8| " + arrowSymbol + " " + instruction + elev
                ), true);
            } else {
                player.displayClientMessage(Component.literal(
                    "§e🧭 GPS NAVIGÁCIÓ §8| §7Nincs beállított beacon vagy célpont!"
                ), true);
            }
        }
    }
}
