package com.hdkiller.spycraft.item;

import com.hdkiller.spycraft.SpyCraftMod;
import com.hdkiller.spycraft.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModItemGroups {
    public static final ResourceKey<CreativeModeTab> ERIK_GROUP = ResourceKey.create(
        Registries.CREATIVE_MODE_TAB,
        ResourceLocation.fromNamespaceAndPath(SpyCraftMod.MOD_ID, "spycraft_tab")
    );

    public static final CreativeModeTab ERIK_TAB = Registry.register(
        BuiltInRegistries.CREATIVE_MODE_TAB,
        ERIK_GROUP,
        FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.SNIPER_RIFLE))
            .title(Component.translatable("itemGroup.erikcraft.spycraft_tab"))
            .displayItems((displayContext, entries) -> {
                entries.accept(ModItems.MISSION_BEACON);
                entries.accept(ModItems.THERMAL_GOGGLES);
                entries.accept(ModItems.HOLOGRAM_PROJECTOR);
                entries.accept(ModItems.TRANQUILIZER_GUN);
                entries.accept(ModItems.SMOKE_GRENADE);
                entries.accept(ModItems.CLIMBING_GLOVES);
                entries.accept(ModItems.PARACHUTE_BACKPACK);
                entries.accept(ModItems.BINOCULARS);
                entries.accept(ModItems.RECON_DRONE);
                entries.accept(ModItems.SNIPER_RIFLE);
                entries.accept(ModItems.LASER_REMOTE);
                entries.accept(ModBlocks.LASER_PYLON);
                entries.accept(ModBlocks.SOUND_TRAP);
                entries.accept(ModItems.VILLAGER_DISGUISE_MASK);
                entries.accept(ModItems.VILLAGER_DISGUISE_ROBE);
                entries.accept(ModItems.SPY_GOGGLES);
                entries.accept(ModItems.GPS_NAVIGATOR_GOGGLES);
                entries.accept(ModItems.GRAPPLING_HOOK_GUN);
                entries.accept(ModItems.SPY_TRACKER);
                entries.accept(ModBlocks.C4_BLOCK);
                entries.accept(ModItems.REMOTE_DETONATOR);
                entries.accept(ModItems.ERIKS_SWORD);
                entries.accept(ModItems.ERIKS_STAR);
            })
            .build()
    );

    public static void registerItemGroups() {
        SpyCraftMod.LOGGER.info("Registering creative item groups for " + SpyCraftMod.MOD_ID);
    }
}
