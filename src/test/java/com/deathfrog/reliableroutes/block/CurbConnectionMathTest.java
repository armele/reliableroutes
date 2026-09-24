package com.deathfrog.reliableroutes.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurbConnectionMathTest
{
    @Test void recognizesOnlyPerpendicularPairsAsCorners()
    {
        assertTrue(CurbConnectionMath.isCorner(CurbConnectionMath.NORTH | CurbConnectionMath.EAST));
        assertTrue(CurbConnectionMath.isCorner(CurbConnectionMath.SOUTH | CurbConnectionMath.WEST));
        assertFalse(CurbConnectionMath.isCorner(CurbConnectionMath.NORTH | CurbConnectionMath.SOUTH));
        assertFalse(CurbConnectionMath.isCorner(CurbConnectionMath.NORTH));
        assertFalse(CurbConnectionMath.isCorner(
            CurbConnectionMath.NORTH | CurbConnectionMath.EAST | CurbConnectionMath.SOUTH));
    }

    @Test void mapsCornersToClockwiseQuarterTurns()
    {
        assertEquals(0, CurbConnectionMath.cornerRotation(CurbConnectionMath.NORTH | CurbConnectionMath.EAST));
        assertEquals(1, CurbConnectionMath.cornerRotation(CurbConnectionMath.EAST | CurbConnectionMath.SOUTH));
        assertEquals(2, CurbConnectionMath.cornerRotation(CurbConnectionMath.SOUTH | CurbConnectionMath.WEST));
        assertEquals(3, CurbConnectionMath.cornerRotation(CurbConnectionMath.WEST | CurbConnectionMath.NORTH));
    }
}
