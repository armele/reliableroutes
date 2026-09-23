package com.deathfrog.reliableroutes.navigation;

import javax.annotation.Nonnull;

/** Server-authoritative pair and directional health snapshot for client diagnostics. */
public record WaypointPairSnapshot(
    @Nonnull WaypointPair pair,
    @Nonnull WaypointPairDirectionHealth firstToSecond,
    @Nonnull WaypointPairDirectionHealth secondToFirst)
{
}
