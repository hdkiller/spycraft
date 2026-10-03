package com.erikcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Tactical C4 Explosive Canister (Tube / Pipe Shaped)
 * - Cylindrical canister shape with custom VoxelShape.
 * - Supports 3 explosive yield levels (Level 1: 4.5x, Level 2: 9.0x, Level 3: 18.0x Mega Blast).
 */
public class C4Block extends Block {
    // 8x8 wide tube, 14 pixels tall (cylindrical pipe)
    protected static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 14.0, 12.0);

    public C4Block(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                com.erikcraft.item.RemoteDetonatorItem.onC4Removed(level, pos);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    /**
     * Explodes the C4 canister with the specified yield power.
     */
    public static void explodeWithPower(Level level, BlockPos pos, float power) {
        if (!level.isClientSide) {
            level.removeBlock(pos, false);
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power, Level.ExplosionInteraction.BLOCK);

            if (power > 5.0f && level instanceof ServerLevel serverLevel) {
                // Giant cinematic explosion particles for Level 2 & 3
                int particleCount = power >= 15.0f ? 6 : 3;
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    particleCount, 1.0, 1.0, 1.0, 0.1);
            }
        }
    }
}
