package com.erikcraft.item;

import com.erikcraft.ErikCraftMod;
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
            .icon(() -> new ItemStack(ModItems.ERIKS_STAR))
            .title(Component.translatable("itemGroup.erikcraft.erikcraft_tab"))
            .displayItems((displayContext, entries) -> {
                entries.accept(ModItems.ERIKS_STAR);
                entries.accept(ModItems.ERIKS_SWORD);
            })
            .build()
    );

    public static void registerItemGroups() {
        ErikCraftMod.LOGGER.info("Registering creative item groups for " + ErikCraftMod.MOD_ID);
    }
}
