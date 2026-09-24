package com.deathfrog.reliableroutes.block;

import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A low, orientable visual boundary which MineColonies treats as forbidden ground. */
public class ForbiddenGroundCurbBlock extends RoutingBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<CurbShape> SHAPE = EnumProperty.create("shape", CurbShape.class);

    @SuppressWarnings("null")
    private static final VoxelShape STRAIGHT_EAST_WEST = Shapes.or(
        Block.box(0.0D, 0.0D, 6.0D, 16.0D, 2.0D, 10.0D),
        Block.box(0.0D, 2.0D, 7.0D, 16.0D, 4.0D, 9.0D));
    @SuppressWarnings("null")
    private static final VoxelShape STRAIGHT_NORTH_SOUTH = Shapes.or(
        Block.box(6.0D, 0.0D, 0.0D, 10.0D, 2.0D, 16.0D),
        Block.box(7.0D, 2.0D, 0.0D, 9.0D, 4.0D, 16.0D));

    private static final VoxelShape CORNER_NORTH_EAST = cornerShape(6, 0, 16, 10, 7, 0, 16, 9);

    private static final VoxelShape CORNER_EAST_SOUTH = cornerShape(6, 6, 16, 16, 7, 7, 16, 16);

    private static final VoxelShape CORNER_SOUTH_WEST = cornerShape(0, 6, 10, 16, 0, 7, 9, 16);

    private static final VoxelShape CORNER_WEST_NORTH = cornerShape(0, 0, 10, 10, 0, 0, 9, 9);

    @SuppressWarnings("null")
    public ForbiddenGroundCurbBlock()
    {
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(SHAPE, CurbShape.STRAIGHT));
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING, SHAPE);
    }

    @SuppressWarnings("null")
    @Override
    public BlockState getStateForPlacement(@Nonnull BlockPlaceContext context)
    {
        BlockState placed = defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        return resolveConnections(placed, context.getLevel(), context.getClickedPos());
    }

    @SuppressWarnings("null")
    @Override
    protected @Nonnull BlockState updateShape(@Nonnull BlockState state,
        @Nonnull Direction direction,
        @Nonnull BlockState neighborState,
        @Nonnull LevelAccessor level,
        @Nonnull BlockPos pos,
        @Nonnull BlockPos neighborPos)
    {
        return direction.getAxis().isHorizontal() ? resolveConnections(state, level, pos) : state;
    }

    @SuppressWarnings("null")
    @Override
    protected @Nonnull BlockState rotate(@Nonnull BlockState state, @Nonnull Rotation rotation)
    {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @SuppressWarnings("null")
    @Override
    protected @Nonnull BlockState mirror(@Nonnull BlockState state, @Nonnull Mirror mirror)
    {
        Direction facing = state.getValue(FACING);
        if (state.getValue(SHAPE) == CurbShape.CORNER)
        {
            Set<Direction> mirrored = EnumSet.noneOf(Direction.class);
            for (Direction connection : connections(state))
            {
                mirrored.add(mirror.mirror(connection));
            }
            return state.setValue(FACING, cornerFacing(mirrored));
        }
        return rotate(state, mirror.getRotation(facing));
    }

    @SuppressWarnings("null")
    @Override
    protected @Nonnull VoxelShape getShape(@Nonnull BlockState state,
        @Nonnull BlockGetter level,
        @Nonnull BlockPos pos,
        @Nonnull CollisionContext context)
    {
        Direction facing = state.getValue(FACING);
        if (state.getValue(SHAPE) == CurbShape.STRAIGHT)
        {
            return facing.getAxis() == Direction.Axis.Z ? STRAIGHT_EAST_WEST : STRAIGHT_NORTH_SOUTH;
        }
        return switch (facing)
        {
            case NORTH -> CORNER_NORTH_EAST;
            case EAST -> CORNER_EAST_SOUTH;
            case SOUTH -> CORNER_SOUTH_WEST;
            case WEST -> CORNER_WEST_NORTH;
            default -> CORNER_NORTH_EAST;
        };
    }

    @SuppressWarnings("null")
    @Override
    protected @Nonnull VoxelShape getCollisionShape(@Nonnull BlockState state,
        @Nonnull BlockGetter level,
        @Nonnull BlockPos pos,
        @Nonnull CollisionContext context)
    {
        return Shapes.empty();
    }

    @SuppressWarnings("null")
    private BlockState resolveConnections(BlockState state, BlockGetter level, BlockPos pos)
    {
        Set<Direction> connected = EnumSet.noneOf(Direction.class);
        int connectionMask = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL)
        {
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (neighbor.is(this) && connections(neighbor).contains(direction.getOpposite()))
            {
                connected.add(direction);
                connectionMask |= connectionBit(direction);
            }
        }

        if (CurbConnectionMath.isCorner(connectionMask))
        {
            return state.setValue(SHAPE, CurbShape.CORNER)
                .setValue(FACING, cornerFacing(CurbConnectionMath.cornerRotation(connectionMask)));
        }
        if (state.getValue(SHAPE) == CurbShape.CORNER)
        {
            Direction facing = state.getValue(FACING);
            if (connected.size() == 1)
            {
                facing = straightFacing(connected.iterator().next());
            }
            else if (connected.size() == 2)
            {
                facing = straightFacing(connected.iterator().next());
            }
            return state.setValue(SHAPE, CurbShape.STRAIGHT).setValue(FACING, facing);
        }
        return state;
    }

    private static Direction cornerFacing(Set<Direction> directions)
    {
        if (directions.contains(Direction.NORTH) && directions.contains(Direction.EAST)) return Direction.NORTH;
        if (directions.contains(Direction.EAST) && directions.contains(Direction.SOUTH)) return Direction.EAST;
        if (directions.contains(Direction.SOUTH) && directions.contains(Direction.WEST)) return Direction.SOUTH;
        return Direction.WEST;
    }

    private static Direction cornerFacing(int quarterTurns)
    {
        return switch (quarterTurns)
        {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }

    private static Direction straightFacing(Direction connection)
    {
        return connection.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.NORTH;
    }

    private static int connectionBit(Direction direction)
    {
        return switch (direction)
        {
            case NORTH -> CurbConnectionMath.NORTH;
            case EAST -> CurbConnectionMath.EAST;
            case SOUTH -> CurbConnectionMath.SOUTH;
            case WEST -> CurbConnectionMath.WEST;
            default -> 0;
        };
    }

    @SuppressWarnings("null")
    private static Set<Direction> connections(BlockState state)
    {
        Direction facing = state.getValue(FACING);
        if (state.getValue(SHAPE) == CurbShape.CORNER)
        {
            return EnumSet.of(facing, facing.getClockWise());
        }
        return facing.getAxis() == Direction.Axis.Z
            ? EnumSet.of(Direction.EAST, Direction.WEST)
            : EnumSet.of(Direction.NORTH, Direction.SOUTH);
    }

    @SuppressWarnings("null")
    private static VoxelShape cornerShape(int baseMinX,
        int baseMinZ,
        int baseMaxX,
        int baseMaxZ,
        int topMinX,
        int topMinZ,
        int topMaxX,
        int topMaxZ)
    {
        return Shapes.or(
            Block.box(baseMinX, 0.0D, 6.0D, baseMaxX, 2.0D, 10.0D),
            Block.box(6.0D, 0.0D, baseMinZ, 10.0D, 2.0D, baseMaxZ),
            Block.box(topMinX, 2.0D, 7.0D, topMaxX, 4.0D, 9.0D),
            Block.box(7.0D, 2.0D, topMinZ, 9.0D, 4.0D, topMaxZ));
    }

    public enum CurbShape implements StringRepresentable
    {
        STRAIGHT("straight"),
        CORNER("corner");

        private final String serializedName;

        CurbShape(String serializedName)
        {
            this.serializedName = serializedName;
        }

        @SuppressWarnings("null")
        @Override
        public @Nonnull String getSerializedName()
        {
            return serializedName;
        }
    }
}
