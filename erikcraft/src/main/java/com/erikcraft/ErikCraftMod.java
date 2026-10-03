package com.erikcraft;

import com.erikcraft.block.ModBlocks;
import com.erikcraft.item.ModItemGroups;
import com.erikcraft.item.ModItems;
import com.erikcraft.laser.LaserForcefieldManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErikCraftMod implements ModInitializer {
    public static final String MOD_ID = "erikcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("==========================================");
        LOGGER.info("  ErikCraft Mod initialized! Welcome Erik!  ");
        LOGGER.info("==========================================");

        // Register custom blocks (C4 Canister, Laser Pylon)
        ModBlocks.registerModBlocks();

        // Register custom items
        ModItems.registerModItems();

        // Register custom creative tab
        ModItemGroups.registerItemGroups();

        // Register server tick event for active laser forcefields
        ServerTickEvents.END_WORLD_TICK.register(LaserForcefieldManager::tick);

        // Add items to standard Combat & Tools tabs
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.accept(ModItems.VILLAGER_DISGUISE_MASK);
            entries.accept(ModItems.VILLAGER_DISGUISE_ROBE);
            entries.accept(ModItems.SPY_GOGGLES);
            entries.accept(ModItems.GPS_NAVIGATOR_GOGGLES);
            entries.accept(ModItems.GRAPPLING_HOOK_GUN);
            entries.accept(ModItems.LASER_REMOTE);
            entries.accept(ModBlocks.LASER_PYLON);
            entries.accept(ModItems.REMOTE_DETONATOR);
            entries.accept(ModBlocks.C4_BLOCK);
            entries.accept(ModItems.ERIKS_SWORD);
            entries.accept(ModItems.ERIKS_STAR);
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
