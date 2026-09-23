package com.deathfrog.reliableroutes.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;

/** Resolves navigator candidates from the persistent waypoint pair index. */
public final class SavedWaypointPairProvider implements WaypointPairProvider
{
    @Override
    public @Nonnull Collection<WaypointPair> findRelevantPairs(
        @Nonnull Level level,
        @Nonnull BlockPos start,
        @Nonnull BlockPos destination,
        int searchRadius)
    {
        if (!(level instanceof ServerLevel serverLevel)) return List.of();
        return WaypointPairSavedData.get(serverLevel).findPairsNearSegment(start, destination, searchRadius);
    }
}
