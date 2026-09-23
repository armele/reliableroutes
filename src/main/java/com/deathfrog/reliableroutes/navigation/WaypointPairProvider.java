package com.deathfrog.reliableroutes.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;

/** Supplies nearby waypoint pairs. Pair discovery and persistence are intentionally kept outside the navigator. */
@FunctionalInterface
public interface WaypointPairProvider
{
    WaypointPairProvider EMPTY = (level, start, destination, searchRadius) -> List.of();

    @Nonnull
    Collection<WaypointPair> findRelevantPairs(
        @Nonnull Level level,
        @Nonnull BlockPos start,
        @Nonnull BlockPos destination,
        int searchRadius);
}
