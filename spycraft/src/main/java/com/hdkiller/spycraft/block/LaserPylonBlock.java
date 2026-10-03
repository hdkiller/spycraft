package com.hdkiller.spycraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Laser Security Pylon
 * Place 3 or more pylons to form a perimeter, then activate with the Laser Remote!
 * Inactive = Blue glow & emitter (standby)
 * Active = Red glow & emitter (forcefield online)
 */
public class LaserPylonBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    protected static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0);

    public LaserPylonBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
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
    public BlockState playerWillDestroy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        if (!level.isClientSide) {
            com.hdkiller.spycraft.laser.LaserForcefieldManager.onPylonBroken(level, pos);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                com.hdkiller.spycraft.laser.LaserForcefieldManager.onPylonBroken(level, pos);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
