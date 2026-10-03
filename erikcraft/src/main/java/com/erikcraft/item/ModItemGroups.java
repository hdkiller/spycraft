package com.erikcraft.item;

import com.erikcraft.ErikCraftMod;
import com.erikcraft.block.ModBlocks;
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
        ResourceLocation.fromNamespaceAndPath(ErikCraftMod.MOD_ID, "erikcraft_tab")
    );

    public static final CreativeModeTab ERIK_TAB = Registry.register(
        BuiltInRegistries.CREATIVE_MODE_TAB,
        ERIK_GROUP,
        FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.VILLAGER_DISGUISE_MASK))
            .title(Component.translatable("itemGroup.erikcraft.erikcraft_tab"))
            .displayItems((displayContext, entries) -> {
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
        ErikCraftMod.LOGGER.info("Registering creative item groups for " + ErikCraftMod.MOD_ID);
    }
}
