package com.deathfrog.reliableroutes.navigation;

import net.minecraft.core.BlockPos;
import javax.annotation.Nonnull;

/** 
 * An X/Z protected column with one bidirectional solid crossing. 
 */
public record RoutingZone(
    int minX, int minZ, int maxX, int maxZ,
    @Nonnull BlockPos firstEndpoint,
    @Nonnull BlockPos secondEndpoint)
{
    public static final int VERTICAL_PADDING = 5;

    @SuppressWarnings("null")
    public RoutingZone
    {
        int normalizedMinX = Math.min(minX, maxX);
        int normalizedMaxX = Math.max(minX, maxX);
        int normalizedMinZ = Math.min(minZ, maxZ);
        int normalizedMaxZ = Math.max(minZ, maxZ);
        minX = normalizedMinX;
        maxX = normalizedMaxX;
        minZ = normalizedMinZ;
        maxZ = normalizedMaxZ;
        firstEndpoint = firstEndpoint.immutable();
        secondEndpoint = secondEndpoint.immutable();
    }

    public static RoutingZone fromCorners(BlockPos firstCorner, BlockPos secondCorner, BlockPos firstEndpoint, BlockPos secondEndpoint)
    {
        return new RoutingZone(firstCorner.getX(), firstCorner.getZ(), secondCorner.getX(), secondCorner.getZ(), firstEndpoint, secondEndpoint);
    }

    public int minY()
    {
        return Math.min(firstEndpoint.getY(), secondEndpoint.getY()) - VERTICAL_PADDING;
    }

    public int maxY()
    {
        return Math.max(firstEndpoint.getY(), secondEndpoint.getY()) + VERTICAL_PADDING;
    }

    public boolean contains(int x, int y, int z)
    {
        return x >= minX && x <= maxX && y >= minY() && y <= maxY() && z >= minZ && z <= maxZ;
    }

    public boolean contains(BlockPos pos)
    {
        return contains(pos.getX(), pos.getY(), pos.getZ());
    }

    public WaypointPair pair()
    {
        return new WaypointPair(firstEndpoint, secondEndpoint);
    }
}
