package com.erikcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tactical C4 Explosive Block
 * Place on the ground or walls, sync with the Remote Detonator, and detonate from anywhere!
 */
public class C4Block extends Block {
    public C4Block(Properties properties) {
        super(properties);
    }

    public static void explode(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            level.removeBlock(pos, false);
            // Powerful explosion with block destruction (similar to TNT, power 4.5)
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 4.5f, Level.ExplosionInteraction.BLOCK);
        }
    }
}
