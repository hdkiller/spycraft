package com.erikcraft.item;

import com.erikcraft.ErikCraftMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

public class ModItems {
    // Erik's Star - Right click to strike lightning!
    public static final Item ERIKS_STAR = registerItem(
        "eriks_star",
        new EriksStarItem(new Item.Properties().rarity(Rarity.EPIC))
    );

    // Erik's Lightning Sword - An ultra-powerful diamond-tier sword!
    public static final Item ERIKS_SWORD = registerItem(
        "eriks_sword",
        new SwordItem(Tiers.NETHERITE, new Item.Properties().rarity(Rarity.RARE).attributes(
            SwordItem.createAttributes(Tiers.NETHERITE, 7, -2.2f)
        ))
    );

    private static Item registerItem(String name, Item item) {
        return Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ErikCraftMod.MOD_ID, name),
            item
        );
    }

    public static void registerModItems() {
        ErikCraftMod.LOGGER.info("Registering custom items for " + ErikCraftMod.MOD_ID);
    }
}
