package com.deathfrog.reliableroutes.block;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A routing road whose physical and selection height matches its recessed model. */
public class RoadRoutingBlock extends RoutingBlock
{
    @SuppressWarnings("null")
    private static final @Nonnull VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D);

    @Override
    protected @Nonnull VoxelShape getShape(@Nonnull BlockState state,
        @Nonnull BlockGetter level,
        @Nonnull BlockPos pos,
        @Nonnull CollisionContext context)
    {
        return SHAPE;
    }

    @Override
    protected @Nonnull VoxelShape getCollisionShape(@Nonnull BlockState state,
        @Nonnull BlockGetter level,
        @Nonnull BlockPos pos,
        @Nonnull CollisionContext context)
    {
        return SHAPE;
    }
}
