package com.hdkiller.spycraft.item;

import com.hdkiller.spycraft.mission.SpyBaseMissionBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * Mission Deployer Beacon Item
 * Right-click on the ground to deploy the massive 96-story Spy Base Skyscraper!
 * Automatically populates with guards, active laser defense grids, and rooftop boss.
 */
public class MissionBeaconItem extends Item {
    public MissionBeaconItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        var level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            player.sendSystemMessage(Component.literal("§b🛰️ [KÜLDETÉS JELADÓ AKTIVÁLVA!] §eKémbázis építése folyamatban..."));
            serverLevel.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0f, 1.2f);

            boolean success = SpyBaseMissionBuilder.deployMission(serverLevel, pos, player);
            if (success) {
                player.getCooldowns().addCooldown(this, 100);
                return InteractionResult.SUCCESS;
            } else {
                player.sendSystemMessage(Component.literal("§c❌ [HIBA] A kémbázis adatfájl nem tölthető be!"));
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Jobb-klikk a földre a §bKémbázis Felhőkarcoló §7lehelyezéséhez!"));
        tooltip.add(Component.literal("§8↳ 96 emelet, 67.477 blokk, automata őrök & lézercsapdák."));
    }
}
