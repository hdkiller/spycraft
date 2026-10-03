package com.hdkiller.spycraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Thermal Vision Goggles (Hőkamera Szemüveg)
 * - Highlights all living entities through solid walls with Glowing outlines!
 * - Provides tactical Night Vision.
 * - Displays live heat-signature count on the Action Bar HUD.
 */
public class ThermalGogglesItem extends ArmorItem {
    public ThermalGogglesItem(Properties properties) {
        super(ArmorMaterials.LEATHER, Type.HELMET, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof Player player) {
            boolean isWearing = player.getItemBySlot(EquipmentSlot.HEAD).getItem() == this;
            if (!isWearing) return;

            // 1. Tactical Night Vision
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false, false));

            // Run thermal scans every 10 ticks (0.5s)
            if (level.getGameTime() % 10 == 0) {
                AABB scanBox = player.getBoundingBox().inflate(32.0);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, scanBox,
                        target -> target != player && target.isAlive());

                double closestDist = Double.MAX_VALUE;
                for (LivingEntity target : targets) {
                    // Apply glowing outline through walls
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 35, 0, false, false, false));
                    double d = player.distanceTo(target);
                    if (d < closestDist) {
                        closestDist = d;
                    }
                }

                // Periodic sonar ping
                if (level.getGameTime() % 40 == 0 && !targets.isEmpty()) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.4f, 1.8f);
                }

                // HUD Action bar update
                String hudText = "§6🔥 HŐKAMERA §7| Hőforrások: §c" + targets.size() + " célpont";
                if (!targets.isEmpty()) {
                    hudText += " §7(Legközelebbi: §e" + String.format("%.1f", closestDist) + "m§7)";
                }
                player.displayClientMessage(Component.literal(hudText), true);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Viseld a fejeden a falakon keresztüli §6Hőlátáshoz§7!"));
        tooltip.add(Component.literal("§8↳ Minden élőlény ragyogó kontúrt kap a falak mögött (32m)."));
    }
}
