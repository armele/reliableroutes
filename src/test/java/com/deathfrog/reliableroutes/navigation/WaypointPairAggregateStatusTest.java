package com.deathfrog.reliableroutes.navigation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaypointPairAggregateStatusTest
{
    @Test
    void summarizesDirectionalHealth()
    {
        assertEquals(WaypointPairAggregateStatus.UNKNOWN,
            WaypointPairAggregateStatus.from(WaypointPairDirectionStatus.UNKNOWN, WaypointPairDirectionStatus.UNKNOWN));
        assertEquals(WaypointPairAggregateStatus.UNKNOWN,
            WaypointPairAggregateStatus.from(WaypointPairDirectionStatus.VALID, WaypointPairDirectionStatus.UNKNOWN));
        assertEquals(WaypointPairAggregateStatus.HEALTHY,
            WaypointPairAggregateStatus.from(WaypointPairDirectionStatus.VALID, WaypointPairDirectionStatus.VALID));
        assertEquals(WaypointPairAggregateStatus.PARTIALLY_BROKEN,
            WaypointPairAggregateStatus.from(WaypointPairDirectionStatus.VALID, WaypointPairDirectionStatus.BROKEN));
        assertEquals(WaypointPairAggregateStatus.PARTIALLY_BROKEN,
            WaypointPairAggregateStatus.from(WaypointPairDirectionStatus.UNKNOWN, WaypointPairDirectionStatus.BROKEN));
        assertEquals(WaypointPairAggregateStatus.BROKEN,
            WaypointPairAggregateStatus.from(WaypointPairDirectionStatus.BROKEN, WaypointPairDirectionStatus.BROKEN));
    }
}
