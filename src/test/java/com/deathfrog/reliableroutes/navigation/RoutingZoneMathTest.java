package com.deathfrog.reliableroutes.navigation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoutingZoneMathTest
{
    @Test void detectsCrossingSegment()
    {
        assertTrue(RoutingZoneMath.segmentIntersectsBox(0, 64, 5, 20, 64, 5, 8, 60, 0, 12, 68, 10));
    }

    @Test void rejectsSegmentThatMissesRectangle()
    {
        assertFalse(RoutingZoneMath.segmentIntersectsBox(0, 64, 20, 20, 64, 20, 8, 60, 0, 12, 68, 10));
    }

    @Test void detectsParallelSegmentInsideOneAxis()
    {
        assertTrue(RoutingZoneMath.segmentIntersectsBox(10, 64, -5, 10, 64, 15, 8, 60, 0, 12, 68, 10));
    }

    @Test void ordersNearestIntersectionFirst()
    {
        assertEquals(0.25, RoutingZoneMath.intersectionOrder(0, 64, 5, 40, 64, 5,
            10, 60, 0, 15, 68, 10), 0.0001);
    }

    @Test void rejectsTripAboveZone()
    {
        assertFalse(RoutingZoneMath.segmentIntersectsBox(0, 80, 5, 20, 80, 5,
            8, 60, 0, 12, 68, 10));
    }

    @Test void detectsTripDescendingThroughZone()
    {
        assertTrue(RoutingZoneMath.segmentIntersectsBox(0, 75, 5, 20, 55, 5,
            8, 60, 0, 12, 68, 10));
    }
}
