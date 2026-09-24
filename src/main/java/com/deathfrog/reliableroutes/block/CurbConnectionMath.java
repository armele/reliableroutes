package com.deathfrog.reliableroutes.block;

/** Pure connection-mask rules for automatic curb corner selection. */
final class CurbConnectionMath
{
    static final int NORTH = 1;
    static final int EAST = 2;
    static final int SOUTH = 4;
    static final int WEST = 8;

    private CurbConnectionMath()
    {
    }

    static boolean isCorner(int connections)
    {
        return connections == (NORTH | EAST)
            || connections == (EAST | SOUTH)
            || connections == (SOUTH | WEST)
            || connections == (WEST | NORTH);
    }

    static int cornerRotation(int connections)
    {
        return switch (connections)
        {
            case NORTH | EAST -> 0;
            case EAST | SOUTH -> 1;
            case SOUTH | WEST -> 2;
            case WEST | NORTH -> 3;
            default -> throw new IllegalArgumentException("Connections do not form a corner: " + connections);
        };
    }
}
