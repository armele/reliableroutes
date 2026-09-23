package com.deathfrog.reliableroutes.navigation;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaypointRouteSelectorTest
{
    @Test
    void selectsBridgeNearTripCorridorWhenCitizenStartsFarAway()
    {
        WaypointRouteSelector selector = new WaypointRouteSelector(32, 1.75, 16);
        WaypointPair bridge = new WaypointPair(new BlockPos(40, 64, 20), new BlockPos(60, 64, 20));

        var selected = selector.select(
            new BlockPos(0, 64, 0),
            new BlockPos(100, 64, 0),
            List.of(bridge));

        assertTrue(selected.isPresent());
        assertEquals(bridge.first(), selected.get().entrance());
        assertEquals(bridge.second(), selected.get().exit());
    }

    @Test
    void rejectsBridgeOutsideTripCorridor()
    {
        WaypointRouteSelector selector = new WaypointRouteSelector(32, 10, 256);
        WaypointPair bridge = new WaypointPair(new BlockPos(40, 64, 33), new BlockPos(60, 64, 33));

        var selected = selector.select(
            new BlockPos(0, 64, 0),
            new BlockPos(100, 64, 0),
            List.of(bridge));

        assertTrue(selected.isEmpty());
    }

    @Test
    void measuresDistanceToFiniteTripSegment()
    {
        assertEquals(10,
            WaypointRouteSelector.horizontalDistanceToSegment(
                new BlockPos(50, 200, 10), new BlockPos(0, 64, 0), new BlockPos(100, 64, 0)),
            0.0001);
        assertEquals(Math.sqrt(200),
            WaypointRouteSelector.horizontalDistanceToSegment(
                new BlockPos(110, 64, 10), new BlockPos(0, 64, 0), new BlockPos(100, 64, 0)),
            0.0001);
    }
}
