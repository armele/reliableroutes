package com.deathfrog.reliableroutes.network;

import com.deathfrog.reliableroutes.Constants;
import com.deathfrog.reliableroutes.ReliableRoutes;
import com.deathfrog.reliableroutes.navigation.RoutingZone;
import com.deathfrog.reliableroutes.navigation.WaypointPairSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Collection;

import javax.annotation.Nonnull;

/** Requests routing zones near a lens user's current position. */
public record RequestWaypointPairsPayload(BlockPos center, int radius) implements CustomPacketPayload
{
    @SuppressWarnings("null")
    public static final Type<RequestWaypointPairsPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "request_waypoint_pairs"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestWaypointPairsPayload> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public RequestWaypointPairsPayload decode(@Nonnull RegistryFriendlyByteBuf buffer)
        {
            return new RequestWaypointPairsPayload(buffer.readBlockPos(), buffer.readVarInt());
        }

        @SuppressWarnings("null")
        @Override
        public void encode(@Nonnull RegistryFriendlyByteBuf buffer, @Nonnull RequestWaypointPairsPayload payload)
        {
            buffer.writeBlockPos(payload.center);
            buffer.writeVarInt(payload.radius);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    @SuppressWarnings("null")
    public static void handle(RequestWaypointPairsPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!player.getMainHandItem().is(ReliableRoutes.PATHFINDER_LENS.get())) return;
            if (player.blockPosition().distSqr(payload.center) > 64) return;
            int radius = Math.clamp(payload.radius, 8, 96);
            Collection<RoutingZone> pairs = WaypointPairSavedData.get(player.serverLevel()).findZonesNear(payload.center, radius);
            WaypointPairSavedData data = WaypointPairSavedData.get(player.serverLevel());
            PacketDistributor.sendToPlayer(player, new ClientboundWaypointPairsPayload(pairs.stream().map(data::snapshot).toList()));
        });
    }
}
