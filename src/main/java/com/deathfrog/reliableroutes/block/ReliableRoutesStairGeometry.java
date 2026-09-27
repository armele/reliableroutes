package com.deathfrog.reliableroutes.block;

/** Pixel coordinates shared by the stair model and its mirrored collision geometry. */
public final class ReliableRoutesStairGeometry
{
    public static final int MIDPOINT = 8;
    public static final int BOTTOM_RAISED_TOP = 15;
    public static final int TOP_LOWER_BOTTOM = 16 - BOTTOM_RAISED_TOP;

    private ReliableRoutesStairGeometry()
    {
        throw new IllegalStateException("ReliableRoutesStairGeometry cannot be instantiated");
    }
}
