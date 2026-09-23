package com.deathfrog.reliableroutes.navigation;

/** Player-facing summary derived from both directional validation records. */
public enum WaypointPairAggregateStatus
{
    UNKNOWN,
    HEALTHY,
    PARTIALLY_BROKEN,
    BROKEN;

    public static WaypointPairAggregateStatus from(
        WaypointPairDirectionStatus forward,
        WaypointPairDirectionStatus reverse)
    {
        if (forward == WaypointPairDirectionStatus.BROKEN && reverse == WaypointPairDirectionStatus.BROKEN)
        {
            return BROKEN;
        }
        if (forward == WaypointPairDirectionStatus.BROKEN || reverse == WaypointPairDirectionStatus.BROKEN)
        {
            return PARTIALLY_BROKEN;
        }
        if (forward == WaypointPairDirectionStatus.VALID && reverse == WaypointPairDirectionStatus.VALID)
        {
            return HEALTHY;
        }
        return UNKNOWN;
    }
}
