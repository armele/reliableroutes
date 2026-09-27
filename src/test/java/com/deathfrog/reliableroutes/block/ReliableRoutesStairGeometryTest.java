package com.deathfrog.reliableroutes.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReliableRoutesStairGeometryTest
{
    @Test
    void raisedTreadStopsAtPixelFifteen()
    {
        assertEquals(15, ReliableRoutesStairGeometry.BOTTOM_RAISED_TOP);
    }

    @Test
    void upsideDownTreadMirrorsTheOnePixelInset()
    {
        assertEquals(1, ReliableRoutesStairGeometry.TOP_LOWER_BOTTOM);
        assertEquals(16,
            ReliableRoutesStairGeometry.BOTTOM_RAISED_TOP + ReliableRoutesStairGeometry.TOP_LOWER_BOTTOM);
    }
}
