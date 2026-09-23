package com.deathfrog.reliableroutes.navigation;

import javax.annotation.Nonnull;

public record RoutingZoneSnapshot(
    @Nonnull RoutingZone zone,
    @Nonnull WaypointPairDirectionHealth firstToSecond,
    @Nonnull WaypointPairDirectionHealth secondToFirst)
{}
