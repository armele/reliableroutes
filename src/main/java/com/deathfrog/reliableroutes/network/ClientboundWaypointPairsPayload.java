package com.deathfrog.reliableroutes.network;

import com.deathfrog.reliableroutes.Constants;
import com.deathfrog.reliableroutes.navigation.WaypointPair;
import com.deathfrog.reliableroutes.navigation.WaypointPairDirectionHealth;
import com.deathfrog.reliableroutes.navigation.WaypointPairDirectionStatus;
import com.deathfrog.reliableroutes.navigation.WaypointPairFailureReason;
import com.deathfrog.reliableroutes.navigation.WaypointPairSnapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/** Synchronizes the nearby pair snapshot used by the lens overlay. */
public record ClientboundWaypointPairsPayload(List<WaypointPairSnapshot> pairs) implements CustomPacketPayload
{
    private static final int MAX_PAIRS = 512;
    public static final Type<ClientboundWaypointPairsPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "waypoint_pairs"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundWaypointPairsPayload> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public ClientboundWaypointPairsPayload decode(@Nonnull RegistryFriendlyByteBuf buffer)
        {
            int size = Math.min(buffer.readVarInt(), MAX_PAIRS);
            List<WaypointPairSnapshot> pairs = new ArrayList<>(size);
            for (int index = 0; index < size; index++)
            {
                WaypointPair pair = new WaypointPair(buffer.readBlockPos(), buffer.readBlockPos());
                pairs.add(new WaypointPairSnapshot(pair, readHealth(buffer), readHealth(buffer)));
            }
            return new ClientboundWaypointPairsPayload(List.copyOf(pairs));
        }

        @Override
        public void encode(@Nonnull RegistryFriendlyByteBuf buffer, @Nonnull ClientboundWaypointPairsPayload payload)
        {
            int size = Math.min(payload.pairs.size(), MAX_PAIRS);
            buffer.writeVarInt(size);
            for (int index = 0; index < size; index++)
            {
                WaypointPairSnapshot snapshot = payload.pairs.get(index);
                WaypointPair pair = snapshot.pair();
                buffer.writeBlockPos(pair.first());
                buffer.writeBlockPos(pair.second());
                writeHealth(buffer, snapshot.firstToSecond());
                writeHealth(buffer, snapshot.secondToFirst());
            }
        }

        private WaypointPairDirectionHealth readHealth(RegistryFriendlyByteBuf buffer)
        {
            WaypointPairDirectionStatus status = readEnum(buffer, WaypointPairDirectionStatus.values(), WaypointPairDirectionStatus.UNKNOWN);
            WaypointPairFailureReason reason = readEnum(buffer, WaypointPairFailureReason.values(), WaypointPairFailureReason.NONE);
            return new WaypointPairDirectionHealth(status, reason, buffer.readVarLong());
        }

        private void writeHealth(RegistryFriendlyByteBuf buffer, WaypointPairDirectionHealth health)
        {
            buffer.writeVarInt(health.status().ordinal());
            buffer.writeVarInt(health.reason().ordinal());
            buffer.writeVarLong(health.validatedAt());
        }

        private <T> T readEnum(RegistryFriendlyByteBuf buffer, T[] values, T fallback)
        {
            int ordinal = buffer.readVarInt();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
        }
    };

    public ClientboundWaypointPairsPayload
    {
        pairs = List.copyOf(pairs);
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(ClientboundWaypointPairsPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() -> ClientWaypointPairCache.update(payload.pairs));
    }
}
