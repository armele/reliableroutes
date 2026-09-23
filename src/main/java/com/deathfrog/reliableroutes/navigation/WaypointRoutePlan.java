package com.deathfrog.reliableroutes.navigation;

import net.minecraft.core.BlockPos;
import javax.annotation.Nonnull;

/** A directionally resolved traversal through a waypoint pair. */
public record WaypointRoutePlan(
    @Nonnull BlockPos entrance,
    @Nonnull BlockPos exit,
    @Nonnull BlockPos destination,
    double estimatedDistance)
{
}
