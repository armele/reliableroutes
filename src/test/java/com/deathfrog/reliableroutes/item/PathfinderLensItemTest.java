package com.deathfrog.reliableroutes.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathfinderLensItemTest
{
    @Test void rejectsEndpointsInTheSameVerticalColumn()
    {
        assertFalse(EndpointPairMath.hasHorizontalSeparation(12, -8, 12, -8));
    }

    @Test void acceptsEndpointsWithHorizontalSeparation()
    {
        assertTrue(EndpointPairMath.hasHorizontalSeparation(12, -8, 13, -8));
        assertTrue(EndpointPairMath.hasHorizontalSeparation(12, -8, 12, -7));
    }
}
