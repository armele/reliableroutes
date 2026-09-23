package com.deathfrog.reliableroutes.network;

import com.deathfrog.reliableroutes.navigation.WaypointPairSnapshot;
import java.util.List;

/** Latest server-authoritative pair snapshot for client visualization. */
public final class ClientWaypointPairCache
{
    private static volatile List<WaypointPairSnapshot> pairs = List.of();

    private ClientWaypointPairCache() {}

    public static List<WaypointPairSnapshot> get()
    {
        return pairs;
    }

    public static void update(List<WaypointPairSnapshot> newPairs)
    {
        pairs = List.copyOf(newPairs);
    }

    public static void clear()
    {
        pairs = List.of();
    }
}
