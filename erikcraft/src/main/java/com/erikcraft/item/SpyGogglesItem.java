package com.erikcraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Map;

/**
 * Tactical Spy Goggles / Night Vision Glasses
 * - Wear on head for instant Night Vision!
 * - Illuminates tracked Block Beacons with a sky-high towering light pillar (96+ blocks) through terrain!
 * - Highlights tracked Mobs with a glowing outline!
 * - Highlights armed C4 canisters with level-specific particle sparks!
 * - Displays a live tactical HUD on the Action Bar!
 */
public class SpyGogglesItem extends ArmorItem {
    public SpyGogglesItem(Properties properties) {
        super(ArmorMaterials.LEATHER, Type.HELMET, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof Player player) {
            boolean isWearing = player.getItemBySlot(EquipmentSlot.HEAD).getItem() == this;
            if (!isWearing) return;

            // 1. Constant Night Vision
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false, false));

            // Only run heavier HUD updates every 10 ticks (0.5s) to stay super performant
            if (level.getGameTime() % 10 != 0) return;

            MobTrackerItem.TrackedBlock trackedBlock = MobTrackerItem.getTrackedBlock(player.getUUID());
            LivingEntity trackedMob = MobTrackerItem.getTrackedMob(player.getUUID(), level);
            Map<BlockPos, Integer> armedC4 = RemoteDetonatorItem.getArmedChargesMap(player.getUUID());

            StringBuilder hud = new StringBuilder("§b🕶️ SPY HUD ");
            boolean hasSignals = false;

            // 2. Render Sky-High Beacon Light Pillar (up to 96 blocks into the sky!)
            if (trackedBlock != null) {
                BlockPos bpos = trackedBlock.pos();
                double dist = Math.sqrt(player.blockPosition().distSqr(bpos));
                hasSignals = true;

                // Send beacon light beam particles straight up into the clouds (visible up to 256m!)
                if (level instanceof ServerLevel serverLevel && dist < 256) {
                    for (int dy = 0; dy <= 96; dy += 3) {
                        serverLevel.sendParticles(ParticleTypes.END_ROD,
                            bpos.getX() + 0.5, bpos.getY() + 1.0 + dy, bpos.getZ() + 0.5,
                            1, 0, 0, 0, 0);
                    }
                }

                String heading = MobTrackerItem.getDirectionLabel(
                    (bpos.getX() + 0.5) - player.getX(),
                    (bpos.getZ() + 0.5) - player.getZ()
                );
                hud.append("§8| §6📍 ").append(trackedBlock.name()).append(": §e")
                   .append((int) dist).append("m ").append(heading).append(" ");
            }

            // 3. Highlight Tracked Mob through walls
            if (trackedMob != null && trackedMob.isAlive()) {
                hasSignals = true;
                int dist = (int) player.distanceTo(trackedMob);
                trackedMob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));

                String heading = MobTrackerItem.getDirectionLabel(
                    trackedMob.getX() - player.getX(),
                    trackedMob.getZ() - player.getZ()
                );
                hud.append("§8| §a📡 ").append(trackedMob.getName().getString()).append(": §e")
                   .append(dist).append("m ").append(heading).append(" ");
            }

            // 4. Highlight Armed C4 Canisters with level indicator
            if (!armedC4.isEmpty()) {
                hasSignals = true;
                int maxLvl = armedC4.values().stream().max(Integer::compareTo).orElse(1);
                String lvlTag = maxLvl == 3 ? "§4[MEGA 3x]§r" : (maxLvl == 2 ? "§6[DUPLA 2x]§r" : "§a[1x]§r");
                hud.append("§8| §c💣 C4: §f").append(armedC4.size()).append(" ").append(lvlTag).append(" ");

                if (level instanceof ServerLevel serverLevel) {
                    for (Map.Entry<BlockPos, Integer> entry : armedC4.entrySet()) {
                        BlockPos c4pos = entry.getKey();
                        int lvl = entry.getValue();

                        if (player.blockPosition().distSqr(c4pos) < 100 * 100) {
                            var particle = lvl == 3 ? ParticleTypes.SOUL_FIRE_FLAME : (lvl == 2 ? ParticleTypes.FLAME : ParticleTypes.ELECTRIC_SPARK);
                            serverLevel.sendParticles(particle,
                                c4pos.getX() + 0.5, c4pos.getY() + 0.8, c4pos.getZ() + 0.5,
                                3, 0.1, 0.1, 0.1, 0.02);
                        }
                    }
                }
            }

            if (!hasSignals) {
                hud.append("§8| §7No active beacons, C4, or targets");
            }

            player.displayClientMessage(Component.literal(hud.toString()), true);
        }
    }
}
