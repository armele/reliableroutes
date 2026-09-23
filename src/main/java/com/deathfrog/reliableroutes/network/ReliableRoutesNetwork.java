package com.deathfrog.reliableroutes.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Registers waypoint visualization payloads. */
public final class ReliableRoutesNetwork
{
    private ReliableRoutesNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToServer(RequestWaypointPairsPayload.TYPE, RequestWaypointPairsPayload.STREAM_CODEC,
            RequestWaypointPairsPayload::handle);
        registrar.playToClient(ClientboundWaypointPairsPayload.TYPE, ClientboundWaypointPairsPayload.STREAM_CODEC,
            ClientboundWaypointPairsPayload::handle);
    }
}
