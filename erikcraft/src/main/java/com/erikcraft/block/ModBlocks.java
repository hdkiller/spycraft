package com.erikcraft.block;

import com.erikcraft.ErikCraftMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {
    // Tactical C4 Explosive Canister
    public static final Block C4_BLOCK = registerBlock(
        "c4_block",
        new C4Block(BlockBehaviour.Properties.of()
            .strength(0.5f)
            .sound(SoundType.WOOD)
            .noOcclusion())
    );

    // Laser Security Pylon
    public static final Block LASER_PYLON = registerBlock(
        "laser_pylon",
        new LaserPylonBlock(BlockBehaviour.Properties.of()
            .strength(1.5f)
            .sound(SoundType.METAL)
            .noOcclusion()
            .lightLevel(state -> 8))
    );

    private static Block registerBlock(String name, Block block) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ErikCraftMod.MOD_ID, name);
        Registry.register(
            BuiltInRegistries.ITEM,
            id,
            new BlockItem(block, new Item.Properties().rarity(Rarity.RARE))
        );
        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }

    public static void registerModBlocks() {
        ErikCraftMod.LOGGER.info("Registering custom blocks for " + ErikCraftMod.MOD_ID);
    }
}
