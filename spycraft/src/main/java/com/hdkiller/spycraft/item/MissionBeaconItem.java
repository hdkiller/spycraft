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
 * Consumes the item upon deployment and prevents duplicate deployments in the same world.
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
            // Check if mission base is already deployed in this world
            if (SpyBaseMissionBuilder.isMissionDeployed(serverLevel)) {
                player.sendSystemMessage(Component.translatable("message.spycraft.mission_beacon.already_active"));
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0f, 0.8f);
                return InteractionResult.FAIL;
            }

            player.sendSystemMessage(Component.translatable("message.spycraft.mission_beacon.activating"));
            serverLevel.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0f, 1.2f);

            boolean success = SpyBaseMissionBuilder.deployMission(serverLevel, pos, player);
            if (success) {
                // Consume beacon from inventory upon deployment
                context.getItemInHand().shrink(1);
                player.getCooldowns().addCooldown(this, 100);
                return InteractionResult.SUCCESS;
            } else {
                player.sendSystemMessage(Component.translatable("message.spycraft.mission_beacon.error"));
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.spycraft.mission_beacon.desc"));
        tooltip.add(Component.translatable("tooltip.spycraft.mission_beacon.single_use"));
        tooltip.add(Component.translatable("tooltip.spycraft.mission_beacon.specs"));
    }
}
