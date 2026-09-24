package com.deathfrog.reliableroutes.item;

/** Pure validation rules for selecting routing-zone endpoints. */
final class EndpointPairMath
{
    private EndpointPairMath() {}

    static boolean hasHorizontalSeparation(int firstX, int firstZ, int secondX, int secondZ)
    {
        return firstX != secondX || firstZ != secondZ;
    }
}
