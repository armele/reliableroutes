package com.deathfrog.reliableroutes.network;

import com.deathfrog.reliableroutes.navigation.RoutingZoneSnapshot;
import java.util.List;

/** Latest server-authoritative pair snapshot for client visualization. */
public final class ClientWaypointPairCache
{
    private static volatile List<RoutingZoneSnapshot> pairs = List.of();

    private ClientWaypointPairCache() {}

    public static List<RoutingZoneSnapshot> get()
    {
        return pairs;
    }

    public static void update(List<RoutingZoneSnapshot> newPairs)
    {
        pairs = List.copyOf(newPairs);
    }

    public static void clear()
    {
        pairs = List.of();
    }
}
