package com.deathfrog.reliableroutes.network;

import com.deathfrog.reliableroutes.Constants;
import com.deathfrog.reliableroutes.navigation.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/** Synchronizes nearby routing zones for the lens overlay. */
public record ClientboundWaypointPairsPayload(List<RoutingZoneSnapshot> zones) implements CustomPacketPayload
{
    private static final int MAX_ZONES = 512;
    @SuppressWarnings("null")
    public static final Type<ClientboundWaypointPairsPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "routing_zones"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundWaypointPairsPayload> STREAM_CODEC = new StreamCodec<>()
    {
        @SuppressWarnings("null")
        @Override
        public ClientboundWaypointPairsPayload decode(@Nonnull RegistryFriendlyByteBuf buffer)
        {
            int size = Math.min(buffer.readVarInt(), MAX_ZONES);
            List<RoutingZoneSnapshot> zones = new ArrayList<>(size);
            for (int index = 0; index < size; index++)
            {
                RoutingZone zone = new RoutingZone(buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readBlockPos(),
                    buffer.readBlockPos());
                zones.add(new RoutingZoneSnapshot(zone, readHealth(buffer), readHealth(buffer)));
            }
            return new ClientboundWaypointPairsPayload(zones);
        }

        @Override
        public void encode(@Nonnull RegistryFriendlyByteBuf buffer, @Nonnull ClientboundWaypointPairsPayload payload)
        {
            int size = Math.min(payload.zones.size(), MAX_ZONES);
            buffer.writeVarInt(size);
            for (int index = 0; index < size; index++)
            {
                RoutingZoneSnapshot snapshot = payload.zones.get(index);
                RoutingZone zone = snapshot.zone();
                buffer.writeVarInt(zone.minX());
                buffer.writeVarInt(zone.minZ());
                buffer.writeVarInt(zone.maxX());
                buffer.writeVarInt(zone.maxZ());
                buffer.writeBlockPos(zone.firstEndpoint());
                buffer.writeBlockPos(zone.secondEndpoint());
                writeHealth(buffer, snapshot.firstToSecond());
                writeHealth(buffer, snapshot.secondToFirst());
            }
        }

        private WaypointPairDirectionHealth readHealth(RegistryFriendlyByteBuf buffer)
        {
            WaypointPairDirectionStatus[] statuses = WaypointPairDirectionStatus.values();
            WaypointPairFailureReason[] reasons = WaypointPairFailureReason.values();
            int status = buffer.readVarInt(), reason = buffer.readVarInt();
            return new WaypointPairDirectionHealth(status < statuses.length ? statuses[status] : WaypointPairDirectionStatus.UNKNOWN,
                reason < reasons.length ? reasons[reason] : WaypointPairFailureReason.NONE,
                buffer.readVarLong());
        }

        private void writeHealth(RegistryFriendlyByteBuf buffer, WaypointPairDirectionHealth value)
        {
            buffer.writeVarInt(value.status().ordinal());
            buffer.writeVarInt(value.reason().ordinal());
            buffer.writeVarLong(value.validatedAt());
        }
    };

    public ClientboundWaypointPairsPayload
    {
        zones = List.copyOf(zones);
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(ClientboundWaypointPairsPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() -> ClientWaypointPairCache.update(payload.zones));
    }
}
