package com.deathfrog.reliableroutes.navigation;

import com.minecolonies.core.entity.pathfinding.MNode;
import com.minecolonies.core.entity.pathfinding.PathfindingUtils;
import com.minecolonies.core.entity.pathfinding.pathjobs.PathJobMoveToLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;
import java.util.List;

/** Move job that prohibits swimming inside routing zones while allowing solid crossings. */
final class PathJobMoveToLocationInZones extends PathJobMoveToLocation
{
    private final List<RoutingZone> zones;
    private final BlockPos escapeTarget;

    PathJobMoveToLocationInZones(Level world, BlockPos start, BlockPos end, int range, Mob entity,
        Collection<RoutingZone> zones, BlockPos escapeTarget)
    {
        super(world, start, end, range, entity);
        this.zones = List.copyOf(zones);
        this.escapeTarget = escapeTarget;
    }

    @Override
    protected int getGroundHeight(MNode parent, int x, int y, int z)
    {
        int height = super.getGroundHeight(parent, x, y, z);
        if (height == Integer.MIN_VALUE || !isInsideZone(x, height, z)) return height;

        BlockState below = cachedBlockLookup.getBlockState(x, height - 1, z);
        boolean water = PathfindingUtils.isWater(cachedBlockLookup, null, below, null)
            || PathfindingUtils.isWater(cachedBlockLookup, null, cachedBlockLookup.getBlockState(x, height, z), null)
            || PathfindingUtils.isWater(cachedBlockLookup, null, cachedBlockLookup.getBlockState(x, height + 1, z), null);
        if (water)
        {
            if (escapeTarget != null && parent != null
                && horizontalDistanceSquared(x, z, escapeTarget) < horizontalDistanceSquared(parent.x, parent.z, escapeTarget))
            {
                return height;
            }
            return Integer.MIN_VALUE;
        }

        // Non-water nodes retain MineColonies' result from super.getGroundHeight.
        return height;
    }

    private boolean isInsideZone(int x, int y, int z)
    {
        for (RoutingZone zone : zones)
        {
            if (zone.contains(x, y, z)) return true;
        }
        return false;
    }

    private static long horizontalDistanceSquared(int x, int z, BlockPos target)
    {
        long dx = x - target.getX();
        long dz = z - target.getZ();
        return dx * dx + dz * dz;
    }
}
