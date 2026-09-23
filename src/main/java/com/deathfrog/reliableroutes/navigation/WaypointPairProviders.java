package com.deathfrog.reliableroutes.navigation;

import java.util.Objects;

/** Runtime seam through which the eventual waypoint system supplies pairs to existing citizen navigators. */
public final class WaypointPairProviders
{
    private static volatile WaypointPairProvider provider = WaypointPairProvider.EMPTY;

    private WaypointPairProviders()
    {
        throw new IllegalStateException("WaypointPairProviders cannot be instantiated");
    }

    public static WaypointPairProvider get()
    {
        return provider;
    }

    public static void install(WaypointPairProvider newProvider)
    {
        provider = Objects.requireNonNull(newProvider);
    }
}
