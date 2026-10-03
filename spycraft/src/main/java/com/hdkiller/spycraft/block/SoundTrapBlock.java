package com.hdkiller.spycraft.block;

import com.hdkiller.spycraft.sound.SoundTrapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Sonic Decoy Sound Trap (Hangcsapda)
 * - Emits authentic, terrifying Creeper hiss audio (SoundEvents.CREEPER_PRIMED).
 * - Lures all nearby mobs within 32 blocks towards it.
 * - Stops completely when destroyed / broken.
 * - Can be toggled on/off by right-clicking.
 */
public class SoundTrapBlock extends Block {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    protected static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 13.0, 13.0);

    public SoundTrapBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, true));
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && state.getValue(ACTIVE)) {
            SoundTrapManager.registerTrap(level, pos);
            level.playSound(null, pos, SoundEvents.CREEPER_PRIMED, SoundSource.BLOCKS, 1.5f, 1.0f);
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE)) {
            // Re-register in SoundTrapManager in case of world reload/chunk load
            SoundTrapManager.registerTrap(level, pos);
            level.scheduleTick(pos, this, 40);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide) {
                SoundTrapManager.removeTrap(level, pos);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            boolean active = !state.getValue(ACTIVE);
            level.setBlock(pos, state.setValue(ACTIVE, active), 3);

            if (active) {
                SoundTrapManager.registerTrap(level, pos);
                level.playSound(null, pos, SoundEvents.CREEPER_PRIMED, SoundSource.BLOCKS, 1.5f, 1.0f);
                level.scheduleTick(pos, this, 20);
                player.displayClientMessage(Component.literal("§e🔊 [HANGCSAPDA] §aBEKAPCSOLVA §8| Még több mobot vonz (Creeper sziszegés)!"), true);
            } else {
                SoundTrapManager.removeTrap(level, pos);
                level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 1.0f, 1.0f);
                player.displayClientMessage(Component.literal("§e🔊 [HANGCSAPDA] §cKIKAPCSOLVA §8(Készenléti csend)"), true);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
