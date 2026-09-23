package com.deathfrog.reliableroutes.navigation;

/** Machine-readable reason why one direction of a waypoint pair is unusable. */
public enum WaypointPairFailureReason
{
    NONE,
    NO_PATH,
    EXCESSIVE_DETOUR,
    MISSING_ENDPOINT
}
