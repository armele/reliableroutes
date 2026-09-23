package com.deathfrog.reliableroutes.navigation;

/** Last known validation result for one direction of a waypoint pair. */
public record WaypointPairDirectionHealth(
    WaypointPairDirectionStatus status,
    WaypointPairFailureReason reason,
    long validatedAt)
{
    public static final WaypointPairDirectionHealth UNKNOWN = new WaypointPairDirectionHealth(
        WaypointPairDirectionStatus.UNKNOWN,
        WaypointPairFailureReason.NONE,
        0);
}
