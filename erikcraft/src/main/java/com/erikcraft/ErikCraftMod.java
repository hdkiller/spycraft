package com.erikcraft;

import com.erikcraft.item.ModItemGroups;
import com.erikcraft.item.ModItems;
import net.fabricmc.api.ModInitializer;
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

        // Register custom items
        ModItems.registerModItems();

        // Register custom creative tab
        ModItemGroups.registerItemGroups();

        // Also add items to standard Combat & Tools tabs for convenience
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.accept(ModItems.ERIKS_SWORD);
            entries.accept(ModItems.ERIKS_STAR);
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
