package com.hdkiller.spycraft;

import com.hdkiller.spycraft.block.ModBlocks;
import com.hdkiller.spycraft.drone.ReconDroneManager;
import com.hdkiller.spycraft.effect.ModEffects;
import com.hdkiller.spycraft.item.ModItemGroups;
import com.hdkiller.spycraft.item.ModItems;
import com.hdkiller.spycraft.laser.LaserForcefieldManager;
import com.hdkiller.spycraft.sound.SoundTrapManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpyCraftMod implements ModInitializer {
    public static final String MOD_ID = "spycraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("==========================================");
        LOGGER.info("  SpyCraft Mod initialized! Welcome Erik!  ");
        LOGGER.info("==========================================");

        // Register custom effects (Drone Piloting)
        ModEffects.registerModEffects();

        // Register custom blocks (C4 Canister, Laser Pylon)
        ModBlocks.registerModBlocks();

        // Register custom items
        ModItems.registerModItems();

        // Register custom creative tab
        ModItemGroups.registerItemGroups();

        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(
                com.hdkiller.spycraft.network.DroneActionPayload.TYPE,
                com.hdkiller.spycraft.network.DroneActionPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(
                com.hdkiller.spycraft.network.BinocularScanPayload.TYPE,
                com.hdkiller.spycraft.network.BinocularScanPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
                com.hdkiller.spycraft.network.DroneActionPayload.TYPE,
                (payload, context) -> ReconDroneManager.handleDroneAction(context.player(), payload.mode()));

        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server -> clearRuntime());
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(LaserForcefieldManager::load);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) ReconDroneManager.onPlayerDisconnect(player);
        });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearRuntime());

        // Register server tick events
        ServerTickEvents.END_WORLD_TICK.register(LaserForcefieldManager::tick);
        ServerTickEvents.END_WORLD_TICK.register(ReconDroneManager::tick);
        ServerTickEvents.END_WORLD_TICK.register(SoundTrapManager::tick);
        ServerTickEvents.END_WORLD_TICK.register(com.hdkiller.spycraft.item.HologramDecoyManager::tick);
        ServerTickEvents.END_WORLD_TICK.register(com.hdkiller.spycraft.item.SmokeCloudManager::tick);
        ServerTickEvents.END_WORLD_TICK.register(com.hdkiller.spycraft.item.ZiplineManager::tick);
        ServerTickEvents.END_WORLD_TICK.register(com.hdkiller.spycraft.item.BreachingSprayManager::tick);
        ServerTickEvents.END_SERVER_TICK.register(com.hdkiller.spycraft.item.ParachuteBackpackItem::tick);

        // Register disconnect event for safe cleanup
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            var player = handler.getPlayer();
            if (player != null) {
                ReconDroneManager.onPlayerDisconnect(player);
                com.hdkiller.spycraft.item.GrapplingHookGunItem.onPlayerDisconnect(player.getUUID());
                com.hdkiller.spycraft.item.ZiplineManager.onPlayerDisconnect(player.getUUID());
                com.hdkiller.spycraft.item.ParachuteBackpackItem.onPlayerDisconnect(player.getUUID());
                com.hdkiller.spycraft.item.BinocularsItem.onPlayerDisconnect(player.getUUID());
            }
        });

        // Register /spycraft spawn_base command
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(net.minecraft.commands.Commands.literal("spycraft")
                .then(net.minecraft.commands.Commands.literal("spawn_base")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> {
                        var source = context.getSource();
                        var player = source.getPlayer();
                        var level = source.getLevel();
                        var pos = player != null ? player.blockPosition() : BlockPos.containing(source.getPosition());
                        boolean success = com.hdkiller.spycraft.mission.SpyBaseMissionBuilder.deployMission(level, pos, player);
                        return success ? 1 : 0;
                    })
                )
                .then(net.minecraft.commands.Commands.literal("reset_mission")
                    .requires(source -> source.hasPermission(2))
                    .executes(context -> {
                        var source = context.getSource();
                        var level = source.getLevel();
                        boolean reset = com.hdkiller.spycraft.mission.SpyBaseMissionBuilder.resetMission(level);
                        if (reset) {
                            source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("§a✅ Kémküldetés bázis zárolása feloldva! A jeladó újra használható."), true);
                        } else {
                            source.sendFailure(net.minecraft.network.chat.Component.literal("§eNem volt aktív küldetés zárolás."));
                        }
                        return 1;
                    })
                )
            );
        });

        // Add items to standard Combat & Tools tabs
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.accept(ModItems.MISSION_BEACON);
            entries.accept(ModItems.THERMAL_GOGGLES);
            entries.accept(ModItems.HOLOGRAM_PROJECTOR);
            entries.accept(ModItems.TRANQUILIZER_GUN);
            entries.accept(ModItems.SMOKE_GRENADE);
            entries.accept(ModItems.CLIMBING_GLOVES);
            entries.accept(ModItems.PARACHUTE_BACKPACK);
            entries.accept(ModItems.BINOCULARS);
            entries.accept(ModItems.ZIPLINE_GUN);
            entries.accept(ModBlocks.SOUND_TRAP);
            entries.accept(ModItems.RECON_DRONE);
            entries.accept(ModItems.SNIPER_RIFLE);
            entries.accept(ModItems.VILLAGER_DISGUISE_MASK);
            entries.accept(ModItems.VILLAGER_DISGUISE_ROBE);
            entries.accept(ModItems.SPY_GOGGLES);
            entries.accept(ModItems.GPS_NAVIGATOR_GOGGLES);
            entries.accept(ModItems.GRAPPLING_HOOK_GUN);
            entries.accept(ModItems.LASER_REMOTE);
            entries.accept(ModBlocks.LASER_PYLON);
            entries.accept(ModItems.REMOTE_DETONATOR);
            entries.accept(ModItems.BREACHING_SPRAY);
            entries.accept(ModBlocks.C4_BLOCK);
            entries.accept(ModItems.ERIKS_SWORD);
            entries.accept(ModItems.ERIKS_STAR);
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
    private static void clearRuntime() {
        LaserForcefieldManager.clearRuntime();
        ReconDroneManager.clearRuntime();
        SoundTrapManager.clearRuntime();
        com.hdkiller.spycraft.item.BreachingSprayManager.clearRuntime();
        com.hdkiller.spycraft.item.SmokeCloudManager.clearRuntime();
        com.hdkiller.spycraft.item.GrapplingHookGunItem.clearRuntime();
        com.hdkiller.spycraft.item.ZiplineManager.clearRuntime();
        com.hdkiller.spycraft.item.HologramDecoyManager.clearRuntime();
        com.hdkiller.spycraft.item.BinocularsItem.clearRuntime();
        com.hdkiller.spycraft.item.MobTrackerItem.clearRuntime();
        com.hdkiller.spycraft.item.RemoteDetonatorItem.clearRuntime();
        com.hdkiller.spycraft.item.ParachuteBackpackItem.clearRuntime();
    }
}
